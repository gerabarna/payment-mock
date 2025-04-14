package hu.gerab.payment.service;

import static java.math.BigDecimal.ZERO;

import hu.gerab.payment.Comparables;
import hu.gerab.payment.domain.Transaction;
import hu.gerab.payment.domain.User;
import hu.gerab.payment.repository.TransactionRepository;
import hu.gerab.payment.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A Helper class for {@link PaymentServiceImpl} This is mainly needed as the @{@link Transactional} annotation is not
 * processed from within a class, and using manually the TransactionManage would be even more messy.
 */
@Component
@Slf4j
public class PaymentServiceHelper {
  private MessagingService messagingService;
  private UserRepository userRepository;
  private TransactionRepository transactionRepository;

  public PaymentServiceHelper(
      MessagingService messagingService,
      UserRepository userRepository,
      TransactionRepository transactionRepository) {
    this.messagingService = messagingService;
    this.userRepository = userRepository;
    this.transactionRepository = transactionRepository;
  }

  @Transactional
  public Boolean processTransaction(
      String requestId, long userId, BigDecimal amount, String currency) {
    if (Comparables.compareEquals(amount, ZERO)) {
      throw new IllegalArgumentException("Transactions should have a non-zero amount.");
    }
    try {
      // validate balance
      User user = userRepository.getReferenceById(userId);
      final BigDecimal newBalance = user.getBalance().add(amount);
      if (ZERO.compareTo(newBalance) > 0) {
        MessagingService.TransactionNotification notification =
            MessagingService.TransactionNotification.builder()
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
      user.setBalance(newBalance);
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
          MessagingService.TransactionNotification.builder()
              .userId(userId)
              .requestId(requestId)
              .amount(amount)
              .currency(currency)
              .successful(true)
              .build());
      return true;
    } catch (EntityNotFoundException e) {
      LOGGER.error("No user exists for id={}, requestId={}", userId, requestId);
    } catch (Exception e) { // we are in an executor, we don't really want to throw anything
      LOGGER.error(
          "Unexpected error occurred during transaction processing for requestId=" + requestId, e);
      messagingService.sendTransactionNotification(
          MessagingService.TransactionNotification.builder()
              .userId(userId)
              .requestId(requestId)
              .amount(amount)
              .currency(currency)
              .successful(false)
              .error("Could not allocate resources for transaction processing.")
              .build());
    }
    return false;
  }
}
