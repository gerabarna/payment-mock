package hu.gerab.payment.service;

import static hu.gerab.payment.config.AsyncConfig.PAYMENT_SERVICE_EXECUTOR;
import static java.math.BigDecimal.ZERO;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.util.concurrent.Striped;
import hu.gerab.payment.domain.Currency;
import hu.gerab.payment.domain.Transaction;
import hu.gerab.payment.domain.User;
import hu.gerab.payment.repository.TransactionRepository;
import hu.gerab.payment.repository.UserRepository;
import hu.gerab.payment.service.MessagingService.TransactionNotification;
import hu.gerab.payment.service.MessagingService.TransactionNotification.TransactionNotificationBuilder;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

  public static final int LOCK_TIMEOUT_MILLIS = 3000;
  private MessagingService messagingService;
  private UserRepository userRepository;
  private TransactionRepository transactionRepository;
  private Striped<Lock> multiLock;
  private final int reattemptLimit;

  public PaymentServiceImpl(
      MessagingService messagingService,
      UserRepository userRepository,
      TransactionRepository transactionRepository,
      @Value("${service.payment.pool.size}") int threadPoolSize,
      @Value("${service.payment.lockstripe.multiplier}") int lockStripeMultiplier,
      @Value("${service.payment.reattempt.limit}") int reattemptLimit) {
    this.messagingService = messagingService;
    this.userRepository = userRepository;
    this.transactionRepository = transactionRepository;
    this.multiLock = Striped.lazyWeakLock(threadPoolSize * lockStripeMultiplier);
    this.reattemptLimit = reattemptLimit;
  }

  @Async(PAYMENT_SERVICE_EXECUTOR)
  @Transactional
  public void processTransfer(
      String requestId, Long senderId, Long receiverId, BigDecimal amount, Currency currency) {
    TransactionNotificationBuilder notificationBuilder =
        TransactionNotification.builder()
            .requestId(requestId)
            .senderId(senderId)
            .receiverId(receiverId)
            .amount(amount)
            .currency(currency.getHumanFriendlyName());
    if (senderId == receiverId) {
      sendFailMessage(notificationBuilder, "Sender and receiver account cannot be the same");
      return;
    }
    if (senderId == null || receiverId == null) {
      // TODO handle topups and withdrawals separately - not in scope
      sendFailMessage(notificationBuilder, "Withdrawals and topups are not yet supported");
      throw new UnsupportedOperationException("Withdrawals and topups are not yet supported");
    }
    if (ZERO.compareTo(amount) >= 0) {
      sendFailMessage(notificationBuilder, "Transfer requires a positive amount");
      return;
    }

    try {
      /* We lock before querying the db. This strategy increases lock time, but we only need a
      single db query to retrieve the data. This strategy should be faster do to less I/O,
      and works well if we expect low lock contention and most transactions to be valid, thus
      the validation without the locks would not gain us much.
      If we would expect lots of validation issues and have high lock contention (which cannot
      be eliminated even by extending the stripes), a double-checked locking pattern could be
      used, so we would perform the validations without the locks, lock, re-query and then redo
      the balance check and complete the transaction.
       */
      List<Lock> locks = Stream.of(senderId, receiverId).sorted().map(multiLock::get).toList();
      /* As we are using multiple locks, we have a potential for a deadlock if we are not careful
      about lock ordering. To avoid it, we will always lock in userId ascending order. This
      strategy should always ensure consistent lock ordering without knowing anything else about
      any other transaction.
      */
      Lock lowerLock = locks.get(0);
      Lock higherLock = locks.get(1);
      boolean lowerLocked = false;
      boolean higherLocked = false;
      for (int reattemptCount = 0; reattemptCount < reattemptLimit; reattemptCount++) {
        try {
          lowerLocked = lowerLock.tryLock(LOCK_TIMEOUT_MILLIS, MILLISECONDS);
          if (!lowerLocked) {
            continue;
          }
          higherLocked = higherLock.tryLock(LOCK_TIMEOUT_MILLIS, MILLISECONDS);
          if (!higherLocked) {
            continue;
          }

          Map<Long, User> userIdToUser =
              getUsers(requestId, senderId, receiverId, notificationBuilder);
          if (userIdToUser == null) {
            return;
          }

          // validate sender balance
          User sender = userIdToUser.get(senderId);
          BigDecimal newBalance = sender.getBalance().subtract(amount);
          if (ZERO.compareTo(newBalance) > 0) {
            sendFailMessage(notificationBuilder, "Insufficient user balance.");
            return;
          }

          Instant now = Instant.now();
          sender.setBalance(newBalance);
          sender.setUpdated(now);
          User receiver = userIdToUser.get(receiverId);
          receiver.setBalance(receiver.getBalance().add(amount));
          receiver.setUpdated(now);

          Transaction transaction =
              Transaction.builder()
                  .amount(amount)
                  .currency(currency)
                  .senderId(senderId)
                  .receiverId(receiverId)
                  .requestId(requestId)
                  .inserted(now)
                  .build();
          transactionRepository.save(transaction);
          userRepository.saveAll(userIdToUser.values());
          messagingService.sendTransactionNotification(
              notificationBuilder.successful(true).build());
          return;
        } catch (InterruptedException e) {
          // probably a shutdown, just log and quit
          LOGGER.warn(
              "Prematurely ended transfer request processing due to shutdown signal for={}",
              notificationBuilder.build());
          return;
        } finally {
          if (higherLocked) {
            higherLock.unlock();
          }
          if (lowerLocked) {
            lowerLock.unlock();
          }
        }
      }
      LOGGER.error("Failed to acquire locks for transfer={}", notificationBuilder.build());
      sendFailMessage(
          notificationBuilder, "Could not allocate resources to process your transfer.");
    } catch (EntityNotFoundException e) {
      LOGGER.error(
          "No user(s) exists for id(s)={} for requestId={}",
          asList(senderId, receiverId),
          requestId);
    } catch (Exception e) { // we are in an executor, we don't really want to throw anything
      LOGGER.error(
          "Unexpected error occurred during transfer processing for requestId=" + requestId, e);
      messagingService.sendTransactionNotification(
          notificationBuilder
              .successful(false)
              .error("Could not allocate resources for transfer processing.")
              .build());
    }
  }

  private Map<Long, User> getUsers(
      String requestId,
      Long senderId,
      Long receiverId,
      TransactionNotificationBuilder notificationBuilder) {
    Set<Long> nonNullIds =
        Stream.of(senderId, receiverId).filter(Objects::nonNull).collect(Collectors.toSet());
    List<User> users = userRepository.findAllById(nonNullIds);
    if (users.size() != nonNullIds.size()) {
      users.stream().map(User::getId).forEach(nonNullIds::remove);
      LOGGER.error("No user(s) exists for id(s)={} for requestId={}", nonNullIds, requestId);
      if (!users.isEmpty() && Objects.equals(users.get(0).getId(), senderId)) {
        // Transfer failed because there is no such receiver -> notify the sender
        sendFailMessage(notificationBuilder, "No user exists for receiver id=" + receiverId);
      }
      return null;
    }
    return users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
  }

  private void sendFailMessage(TransactionNotificationBuilder notificationBuilder, String error) {
    TransactionNotification notification =
        notificationBuilder.successful(false).error(error).build();
    messagingService.sendTransactionNotification(notification.getSenderId(), notification);
  }
}
