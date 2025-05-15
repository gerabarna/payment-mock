package hu.gerab.payment.repository;

import static hu.gerab.payment.domain.Currency.USD;
import static java.math.BigDecimal.TEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hu.gerab.payment.config.TestDatabaseConfig;
import hu.gerab.payment.domain.Transaction;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@ActiveProfiles("test")
@ContextConfiguration(classes = TestDatabaseConfig.class)
@DataJpaTest
class TransactionRepositoryTest {

  @Autowired private TransactionRepository transactionRepository;
  @Autowired private EntityManager entityManager;

  @AfterEach
  void cleanup() {
    transactionRepository.deleteAll();
  }

  @Test
  public void whenNewTransactionPersistedAndRead_thenGivenFieldsEqualAndGeneratedFieldsAreFilled() {
    assertEquals(0, transactionRepository.count());
    final Transaction transaction =
        Transaction.builder()
            .senderId(1L)
            .receiverId(2L)
            .amount(TEN)
            .currency(USD)
            .requestId("request")
            .build();
    transactionRepository.save(transaction);
    transactionRepository.flush();
    entityManager.refresh(transaction);
    List<Transaction> transactions = transactionRepository.findAll();
    assertEquals(1, transactions.size());
    Transaction persisted = entityManager.find(Transaction.class, 1L);
    assertEquals(transaction.getSenderId(), persisted.getSenderId());
    assertEquals(transaction.getReceiverId(), persisted.getReceiverId());
    assertEquals(transaction.getRequestId(), persisted.getRequestId());
    assertEquals(0, persisted.getAmount().compareTo(transaction.getAmount()));
    assertEquals(transaction.getCurrency(), persisted.getCurrency());
    assertNotNull(persisted.getId());
  }
}
