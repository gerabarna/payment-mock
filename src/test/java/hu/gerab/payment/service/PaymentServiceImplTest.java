package hu.gerab.payment.service;

import static hu.gerab.payment.TestUtils.*;
import static hu.gerab.payment.domain.Currency.*;
import static org.junit.jupiter.api.Assertions.*;

import hu.gerab.payment.config.TestDatabaseConfig;
import hu.gerab.payment.domain.Transaction;
import hu.gerab.payment.domain.User;
import hu.gerab.payment.repository.TransactionRepository;
import hu.gerab.payment.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;
import org.junit.jupiter.api.AfterEach;
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

  private PaymentService paymentService;
  private MessagingServiceStub messagingService;
  @Autowired private UserRepository userRepository;
  @Autowired private TransactionRepository transactionRepository;

  @BeforeEach
  void setup() {
    messagingService = new MessagingServiceStub();
    paymentService =
        new PaymentServiceImpl(messagingService, userRepository, transactionRepository, 4, 100, 3);
  }

  @AfterEach
  void cleanup() {
    userRepository.deleteAll();
    transactionRepository.deleteAll();
  }

  private class MessagingServiceStub implements MessagingService {

    private Map<Long, CountDownLatch> latches;

    @Override
    public void sendTransactionNotification(TransactionNotification notification) {
      Long senderId = notification.getSenderId();
      if (senderId != null) {
        sendTransactionNotification(senderId, notification);
      }
    }

    @Override
    public void sendTransactionNotification(long userId, TransactionNotification notification) {
      CountDownLatch latch = latches.get(userId);
      if (latch != null) {
        latch.countDown();
      }
    }

    public void initLatches(Map<Long, CountDownLatch> latches) {
      this.latches = new ConcurrentHashMap<>(latches);
    }
  }

  @Test
  public void
      givenThreeUsers_whenMultipleFastTransactionRequestsComeConcurrently_ThenAllProcessedCorrectly()
          throws InterruptedException {
    assertEquals(0, userRepository.count());
    assertEquals(0, transactionRepository.count());
    Long userId1 =
        userRepository.save(User.builder().balance(THOUSAND).currency(USD).build()).getId();
    Long userId2 =
        userRepository.save(User.builder().balance(THOUSAND).currency(USD).build()).getId();
    Long userId3 =
        userRepository.save(User.builder().balance(THOUSAND).currency(USD).build()).getId();
    Map<Long, CountDownLatch> latchMap = new HashMap<>();
    latchMap.put(userId1, new CountDownLatch(10));
    latchMap.put(userId2, new CountDownLatch(10));
    latchMap.put(userId3, new CountDownLatch(10));
    messagingService.initLatches(latchMap);
    // Three users placing transaction requests in quick succession
    Instant start = Instant.now();
    Map<Long, Long> senderIdToReceiverId =
        Map.of(userId1, userId2, userId2, userId3, userId3, userId1);
    LongStream.of(userId1, userId2, userId3)
        .mapToObj(
            senderId ->
                new Runnable() {
                  @Override
                  public void run() {
                    for (int i = 0; i < 10; i++) {
                      paymentService.processTransfer(
                          String.format("%d-%d", senderId, i + 1),
                          senderId,
                          senderIdToReceiverId.get(senderId),
                          new BigDecimal(i + 1),
                          USD);
                    }
                  }
                })
        .map(Thread::new)
        .forEach(Thread::start);
    for (Entry<Long, CountDownLatch> entry : latchMap.entrySet()) {
      final boolean await = entry.getValue().await(10, TimeUnit.SECONDS);
      assertTrue(await, "Incomplete message set for userId=" + entry.getKey());
    }

    List<Long> userIds = LongStream.of(userId1, userId2, userId3).boxed().toList();
    for (Long userId : userIds) {
      List<Transaction> user1Transactions = transactionRepository.findBySenderId(userId);
      assertEquals(10, user1Transactions.size());
      for (int i = 0; i < 10; i++) {
        final Transaction transaction = user1Transactions.get(i);
        assertEquals(userId, transaction.getSenderId());
        assertEquals(
            0,
            transaction.getAmount().compareTo(new BigDecimal(i + 1)),
            String.format("Expected Amount=%d but actual=%s", i + 1, transaction.getAmount()));
        assertEquals(USD, transaction.getCurrency());
        assertEquals(String.format("%d-%d", userId, i + 1), transaction.getRequestId());
      }
    }
    assertEquals(0, userRepository.findById(userId1).get().getBalance().compareTo(THOUSAND));
    assertEquals(0, userRepository.findById(userId2).get().getBalance().compareTo(THOUSAND));
    assertEquals(0, userRepository.findById(userId3).get().getBalance().compareTo(THOUSAND));
  }
}
