package hu.gerab.payment.service;

import static hu.gerab.payment.service.MessagingService.*;
import static java.math.BigDecimal.ZERO;

import hu.gerab.payment.Comparables;
import hu.gerab.payment.domain.Transaction;
import hu.gerab.payment.domain.User;
import hu.gerab.payment.repository.TransactionRepository;
import hu.gerab.payment.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PessimisticLockException;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class MultiNodePaymentServiceImpl implements PaymentService {

  private MessagingService messagingService;

  private Integer lockAttemptLimit;
  private UserRepository userRepository;
  private TransactionRepository transactionRepository;

  public MultiNodePaymentServiceImpl(
      MessagingService messagingService,
      @Value("${service.payment.db.lockAttemptLimit}") Integer lockAttemptLimit,
      UserRepository userRepository,
      TransactionRepository transactionRepository) {
    this.messagingService = messagingService;
    this.lockAttemptLimit = lockAttemptLimit;
    this.userRepository = userRepository;
    this.transactionRepository = transactionRepository;
  }

  @Override
  @Transactional
  public void processTransaction(
      String requestId, long userId, BigDecimal amount, String currency) {
    if (Comparables.compareEquals(amount, ZERO)) {
      throw new IllegalArgumentException("Transactions should have a non-zero amount.");
    }
    try {
      /*
      Here we are basically doing a double checked locking with the DB to reduce lock time on the DB.
      Since the DB ensures the values are up-to-date this should work correctly even in a concurrent setting.
      This may not be a good idea if communication time with a db is 'long'
       */
      User user = userRepository.getReferenceById(userId);
      if (!validateUserBalance(user, requestId, amount, currency)) {
        return;
      }
      // Everything looks ok, so we can actually start locking
      // We use a user level lock, as we want to ensure the balance stays consistent
      int attemptCounter = 1;
      while (attemptCounter <= lockAttemptLimit) {
        try {
          user = userRepository.lockById(userId);
          // we need to check things again as the user may have been updated while we didn't lock
          if (!validateUserBalance(user, requestId, amount, currency)) {
            return;
          }
          user.setBalance(user.getBalance().add(amount));
          Transaction transaction =
              Transaction.builder()
                  .amount(amount)
                  .currency(currency)
                  .userId(userId)
                  .requestId(requestId)
                  .build();
          transactionRepository.save(transaction);
          userRepository.save(user);
          messagingService.sendTransactionNotification(
              TransactionNotification.builder()
                  .userId(userId)
                  .requestId(requestId)
                  .amount(amount)
                  .currency(currency)
                  .successful(true)
                  .build());
          return;
        } catch (PessimisticLockException e) {
          LOGGER.debug(
              "lock contention for balance transaction processing on userid={}, attempt={}, requestId={}",
              userId,
              attemptCounter,
              requestId);
          attemptCounter++;
        }
      }
    } catch (EntityNotFoundException e) {
      LOGGER.error("No user exists for id={}, requestId={}", userId, requestId);
    }
    LOGGER.error("Failed to acquire lock for transaction processing for requestId={}", requestId);
    messagingService.sendTransactionNotification(
        TransactionNotification.builder()
            .userId(userId)
            .requestId(requestId)
            .amount(amount)
            .currency(currency)
            .successful(false)
            .error("Could not allocate resources for transaction processing.")
            .build());
  }

  private boolean validateUserBalance(
      User user, String requestId, BigDecimal amount, String currency) {
    final BigDecimal newBalance = user.getBalance().add(amount);
    if (ZERO.compareTo(newBalance) > 0) {
      TransactionNotification notification =
          TransactionNotification.builder()
              .userId(user.getId())
              .requestId(requestId)
              .amount(amount)
              .currency(currency)
              .successful(false)
              .error("Insufficient user balance.")
              .build();
      messagingService.sendTransactionNotification(notification);
      return false;
    }
    return true;
  }
}
