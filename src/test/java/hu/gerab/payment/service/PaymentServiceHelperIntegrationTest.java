package hu.gerab.payment.service;

import static hu.gerab.payment.service.MessagingService.*;
import static java.math.BigDecimal.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import hu.gerab.payment.config.TestDatabaseConfig;
import hu.gerab.payment.domain.User;
import hu.gerab.payment.repository.TransactionRepository;
import hu.gerab.payment.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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

  @Autowired private JdbcTemplate jdbcTemplate;
  private MessagingService messagingService;

  @BeforeEach
  void setup() {
    messagingService = spy(new MessagingService(mock(KafkaTemplate.class)));
    paymentServiceHelper =
        new PaymentServiceHelper(messagingService, userRepository, transactionRepository);
  }

  @AfterEach
  void cleanup() {
    jdbcTemplate.execute("TRUNCATE TABLE users");
    jdbcTemplate.execute("TRUNCATE TABLE transactions");
  }

  @Test
  public void givenNoUser_whenTransactionComes_FailsWithoutMessage() {
    assertEquals(0, userRepository.count());
    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);

    paymentServiceHelper.processTransaction("1", 1L, new BigDecimal("-10"), "USD");

    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);
  }

  @Test
  public void givenUserWithBalance_whenSmallerAmountTransactionComes_processedAsExpected() {
    Long userId =
        userRepository
            .save(User.builder().balance(new BigDecimal("1000")).currency("USD").build())
            .getId();
    userRepository.findAll().forEach(System.out::println);
    assertTrue(userRepository.findById(userId).isPresent());
    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);
    var notificationCaptor = ArgumentCaptor.forClass(TransactionNotification.class);

    paymentServiceHelper.processTransaction("1", userId, new BigDecimal("-10"), "USD");

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
    Long userId = userRepository.save(User.builder().balance(ZERO).currency("USD").build()).getId();
    assertEquals(0, transactionRepository.count());
    verifyNoInteractions(messagingService);
    var notificationCaptor = ArgumentCaptor.forClass(TransactionNotification.class);

    paymentServiceHelper.processTransaction("1", userId, new BigDecimal("-10"), "USD");

    assertEquals(0, ZERO.compareTo(userRepository.findById(userId).get().getBalance()));
    assertEquals(0, transactionRepository.count());
    verify(messagingService).sendTransactionNotification(notificationCaptor.capture());
    assertFalse(notificationCaptor.getValue().isSuccessful());
  }
}
