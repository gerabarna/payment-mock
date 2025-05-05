package hu.gerab.payment.service;

import static hu.gerab.payment.domain.Currency.*;
import static java.math.BigDecimal.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import hu.gerab.payment.config.TestDatabaseConfig;
import hu.gerab.payment.domain.User;
import hu.gerab.payment.repository.TransactionRepository;
import hu.gerab.payment.repository.UserRepository;
import hu.gerab.payment.service.MessagingService.TransactionNotification;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = TestDatabaseConfig.class)
class PaymentServiceHelperIntegrationTest {
  private PaymentServiceHelper paymentServiceHelper;
  @Autowired private UserRepository userRepository;
  @Autowired private TransactionRepository transactionRepository;
  private MessagingService messagingService;

  @BeforeEach
  void setup() {
    messagingService = spy(new MessagingService(mock(KafkaTemplate.class)));
    paymentServiceHelper =
        new PaymentServiceHelper(messagingService, userRepository, transactionRepository);
  }

  @AfterEach
  void cleanup() {
    userRepository.deleteAll();
    transactionRepository.deleteAll();
  }

  @Test
  public void givenNoUser_whenTransactionComes_FailsWithoutMessage() {
    assertEquals(0, userRepository.count());
    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);

    paymentServiceHelper.processTransaction("1", 1L, new BigDecimal("-10"), USD);

    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);
  }

  @Test
  public void givenUserWithBalance_whenSmallerAmountTransactionComes_processedAsExpected() {
    Long userId =
        userRepository
            .save(User.builder().balance(new BigDecimal("1000")).currency(USD).build())
            .getId();
    userRepository.findAll().forEach(System.out::println);
    assertTrue(userRepository.findById(userId).isPresent());
    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);
    var notificationCaptor = ArgumentCaptor.forClass(TransactionNotification.class);

    paymentServiceHelper.processTransaction("1", userId, new BigDecimal("-10"), USD);

    BigDecimal balance = userRepository.findById(userId).get().getBalance();
    assertEquals(0, new BigDecimal("990").compareTo(balance));
    assertEquals(1, transactionRepository.count());
    assertEquals(
        0,
        transactionRepository
            .findByUserId(userId)
            .get(0)
            .getAmount()
            .compareTo(new BigDecimal("-10")));
    verify(messagingService).sendTransactionNotification(notificationCaptor.capture());
    assertTrue(notificationCaptor.getValue().isSuccessful());
  }

  @Test
  public void givenUserWithNoBalance_whenNegativeTransactionComes_Fails() {
    Long userId = userRepository.save(User.builder().balance(ZERO).currency(USD).build()).getId();
    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);
    var notificationCaptor = ArgumentCaptor.forClass(TransactionNotification.class);

    paymentServiceHelper.processTransaction("1", userId, new BigDecimal("-10"), USD);

    assertEquals(0, ZERO.compareTo(userRepository.findById(userId).get().getBalance()));
    assertEquals(0, transactionRepository.count());
    verify(messagingService).sendTransactionNotification(notificationCaptor.capture());
    assertFalse(notificationCaptor.getValue().isSuccessful());
  }

  @Test
  public void givenUserWithNegativeBalance_whenPositiveTransactionComes_SucceedsWitMessage() {
    Long userId =
        userRepository
            .save(User.builder().balance(new BigDecimal("-100")).currency(USD).build())
            .getId();
    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);
    var notificationCaptor = ArgumentCaptor.forClass(TransactionNotification.class);

    paymentServiceHelper.processTransaction("1", userId, new BigDecimal("10"), USD);

    assertEquals(
        0, new BigDecimal("-90").compareTo(userRepository.findById(userId).get().getBalance()));
    assertEquals(1, transactionRepository.count());
    verify(messagingService).sendTransactionNotification(notificationCaptor.capture());
    assertTrue(notificationCaptor.getValue().isSuccessful());
  }

  @Test
  public void whenTransactionComesWithZeroAmount_FailsWithMessage() {
    Long userId = userRepository.save(User.builder().balance(TEN).currency(USD).build()).getId();
    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);
    var notificationCaptor = ArgumentCaptor.forClass(TransactionNotification.class);

    paymentServiceHelper.processTransaction("1", userId, ZERO, USD);

    assertEquals(0, TEN.compareTo(userRepository.findById(userId).get().getBalance()));
    assertEquals(0, transactionRepository.count());
    verify(messagingService).sendTransactionNotification(notificationCaptor.capture());
    assertFalse(notificationCaptor.getValue().isSuccessful());
  }
}
