package hu.gerab.payment.service;

import static hu.gerab.payment.domain.Currency.*;
import static java.math.BigDecimal.*;
import static org.junit.jupiter.api.Assertions.*;

import hu.gerab.payment.config.TestDatabaseConfig;
import hu.gerab.payment.domain.Transaction;
import hu.gerab.payment.domain.User;
import hu.gerab.payment.repository.TransactionRepository;
import hu.gerab.payment.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.LongStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = TestDatabaseConfig.class)
class PaymentServiceImplTest {

  private PaymentServiceImpl paymentService;
  @Autowired private PaymentServiceHelper paymentServiceHelper;
  @Autowired private UserRepository userRepository;
  @Autowired private TransactionRepository transactionRepository;

  @BeforeEach
  void setup() {
    paymentService = new PaymentServiceImpl(paymentServiceHelper, 10);
  }

  @AfterEach
  void cleanup() {
    userRepository.deleteAll();
    transactionRepository.deleteAll();
  }

  @Test
  public void
      givenThreeUsers_whenMultipleFastTransactionRequestsCome_ThenDBTransactionsAreInSameOrder()
          throws InterruptedException {
    assertEquals(0, userRepository.count());
    assertEquals(0, transactionRepository.count());
    Long userId1 = userRepository.save(User.builder().balance(ZERO).currency(USD).build()).getId();
    Long userId2 = userRepository.save(User.builder().balance(ZERO).currency(USD).build()).getId();
    Long userId3 = userRepository.save(User.builder().balance(ZERO).currency(USD).build()).getId();
    // Three users placing transaction requests in quick succession
    CountDownLatch latch = new CountDownLatch(3);
    Instant start = Instant.now();
    LongStream.of(userId1, userId2, userId3)
        .mapToObj(
            userId ->
                new Runnable() {
                  @Override
                  public void run() {
                    List<Future<Boolean>> futures = new ArrayList<>();
                    for (int i = 0; i < 10; i++) {
                      Future<Boolean> future =
                          paymentService.processTransaction(
                              String.format("%d-%d", userId, i + 1),
                              userId,
                              new BigDecimal(i + 1),
                              USD);
                      futures.add(future);
                    }
                    for (Future<Boolean> future : futures) {
                      try {
                        future.get(10, TimeUnit.SECONDS);
                      } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        Assertions.fail(e);
                      }
                    }
                    latch.countDown();
                  }
                })
        .map(Thread::new)
        .forEach(Thread::start);
    final boolean await = latch.await(10, TimeUnit.SECONDS);
    assertTrue(await);

    List<Long> userIds = LongStream.of(userId1, userId2, userId3).boxed().toList();
    for (Long userId : userIds) {
      List<Transaction> user1Transactions = transactionRepository.findByUserId(userId);
      assertEquals(10, user1Transactions.size());
      Instant lastInserted = start;
      for (int i = 0; i < 10; i++) {
        final Transaction transaction = user1Transactions.get(i);
        assertEquals(userId, transaction.getUserId());
        assertEquals(
            0,
            transaction.getAmount().compareTo(new BigDecimal(i + 1)),
            String.format("Expected Amount=%d but actual=%s", i + 1, transaction.getAmount()));
        assertEquals(USD, transaction.getCurrency());
        assertEquals(String.format("%d-%d", userId, i + 1), transaction.getRequestId());
        // assertEquals(-1, lastInserted.compareTo(transaction.getInserted()));
        lastInserted = transaction.getInserted();
      }
    }
  }
}
