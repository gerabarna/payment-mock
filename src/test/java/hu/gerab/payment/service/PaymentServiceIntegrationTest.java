package hu.gerab.payment.service;

import static hu.gerab.payment.TestUtils.*;
import static hu.gerab.payment.domain.Currency.*;
import static java.math.BigDecimal.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = TestDatabaseConfig.class)
class PaymentServiceIntegrationTest {
  private PaymentService paymentService;
  @Autowired private UserRepository userRepository;
  @Autowired private TransactionRepository transactionRepository;
  private MessagingService messagingService;

  @BeforeEach
  void setup() {
    messagingService = spy(MessagingService.class);
    paymentService =
        new PaymentServiceImpl(messagingService, userRepository, transactionRepository, 4, 100, 3);
  }

  @AfterEach
  void cleanup() {
    userRepository.deleteAll();
    transactionRepository.deleteAll();
  }

  @Test
  public void givenNoSender_whenTransactionComes_FailsWithoutMessage() {
    Long userId =
        userRepository.save(User.builder().balance(THOUSAND).currency(USD).build()).getId();
    assertEquals(1, userRepository.count());
    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);

    paymentService.processTransfer("1", 1L, userId, TEN, USD);

    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);
  }

  @Test
  public void givenNoReceiver_whenTransactionComes_FailsWithoutMessage() {
    Long userId =
        userRepository.save(User.builder().balance(THOUSAND).currency(USD).build()).getId();
    assertEquals(1, userRepository.count());
    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);
    var notificationCaptor = ArgumentCaptor.forClass(TransactionNotification.class);

    paymentService.processTransfer("1", userId, 0L, TEN, USD);

    assertEquals(0, transactionRepository.count());
    verify(messagingService).sendTransactionNotification(eq(userId), notificationCaptor.capture());
    assertFalse(notificationCaptor.getValue().isSuccessful());
  }

  @Test
  public void givenUserWithBalance_whenSmallerAmountTransactionComes_processedAsExpected() {
    Long senderId =
        userRepository.save(User.builder().balance(THOUSAND).currency(USD).build()).getId();
    Long receiverId =
        userRepository.save(User.builder().balance(ZERO).currency(USD).build()).getId();

    assertEquals(2, userRepository.count());
    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);
    var notificationCaptor = ArgumentCaptor.forClass(TransactionNotification.class);

    paymentService.processTransfer("1", senderId, receiverId, TEN, USD);

    User sender = userRepository.findById(senderId).get();
    assertEquals(0, new BigDecimal("990").compareTo(sender.getBalance()));

    User receiver = userRepository.findById(receiverId).get();
    assertEquals(0, TEN.compareTo(receiver.getBalance()));

    assertEquals(1, transactionRepository.count());
    assertEquals(
        0, transactionRepository.findBySenderId(senderId).get(0).getAmount().compareTo(TEN));
    verify(messagingService).sendTransactionNotification(notificationCaptor.capture());
    assertTrue(notificationCaptor.getValue().isSuccessful());
  }

  @Test
  public void givenUserWithBalance_whenFullBalanceTransactionComes_Succeeds() {
    Long senderId = userRepository.save(User.builder().balance(TEN).currency(USD).build()).getId();
    Long receiverId =
        userRepository.save(User.builder().balance(ZERO).currency(USD).build()).getId();

    assertEquals(2, userRepository.count());
    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);
    var notificationCaptor = ArgumentCaptor.forClass(TransactionNotification.class);

    paymentService.processTransfer("1", senderId, receiverId, TEN, USD);

    User sender = userRepository.findById(senderId).get();
    assertEquals(0, ZERO.compareTo(sender.getBalance()));

    User receiver = userRepository.findById(receiverId).get();
    assertEquals(0, TEN.compareTo(receiver.getBalance()));

    assertEquals(1, transactionRepository.count());
    assertEquals(
        0, transactionRepository.findBySenderId(senderId).get(0).getAmount().compareTo(TEN));
    verify(messagingService).sendTransactionNotification(notificationCaptor.capture());
    assertTrue(notificationCaptor.getValue().isSuccessful());
  }

  @Test
  public void givenUserWithNoBalance_whenSendingTransactionComes_Fails() {
    Long senderId = userRepository.save(User.builder().balance(ZERO).currency(USD).build()).getId();
    Long receiverId =
        userRepository.save(User.builder().balance(TEN).currency(USD).build()).getId();
    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);
    var notificationCaptor = ArgumentCaptor.forClass(TransactionNotification.class);

    paymentService.processTransfer("1", senderId, receiverId, TEN, USD);

    assertEquals(0, ZERO.compareTo(userRepository.findById(senderId).get().getBalance()));
    assertEquals(0, transactionRepository.count());
    verify(messagingService)
        .sendTransactionNotification(eq(senderId), notificationCaptor.capture());
    assertFalse(notificationCaptor.getValue().isSuccessful());
  }

  @Test
  public void whenTransactionComesWithZeroAmount_FailsWithMessage() {
    Long userId = userRepository.save(User.builder().balance(TEN).currency(USD).build()).getId();
    Long userId2 = userRepository.save(User.builder().balance(TEN).currency(USD).build()).getId();
    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);
    var notificationCaptor = ArgumentCaptor.forClass(TransactionNotification.class);

    paymentService.processTransfer("1", userId, userId2, ZERO, USD);

    assertEquals(0, TEN.compareTo(userRepository.findById(userId).get().getBalance()));
    assertEquals(0, transactionRepository.count());
    verify(messagingService).sendTransactionNotification(eq(userId), notificationCaptor.capture());
    assertFalse(notificationCaptor.getValue().isSuccessful());
  }
}
