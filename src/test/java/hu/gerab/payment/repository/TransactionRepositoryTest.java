package hu.gerab.payment.repository;

import static hu.gerab.payment.domain.Currency.USD;
import static java.math.BigDecimal.TEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hu.gerab.payment.config.TestDatabaseConfig;
import hu.gerab.payment.domain.Transaction;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = TestDatabaseConfig.class)
class TransactionRepositoryTest {

  @Autowired private TransactionRepository transactionRepository;

  @AfterEach
  void cleanup() {
    transactionRepository.deleteAll();
  }

  @Test
  public void whenNewTransactionPersistedAndRead_thenGivenFieldsEqualAndGeneratedFieldsAreFilled() {
    final Transaction transaction =
        Transaction.builder().userId(1L).amount(TEN).currency(USD).requestId("request").build();
    transactionRepository.save(transaction);
    List<Transaction> transactions = transactionRepository.findByUserId(1L);
    assertEquals(1, transactions.size());
    Transaction persisted = transactions.get(0);
    assertEquals(transaction.getUserId(), persisted.getUserId());
    assertEquals(transaction.getRequestId(), persisted.getRequestId());
    assertEquals(0, persisted.getAmount().compareTo(transaction.getAmount()));
    assertEquals(transaction.getCurrency(), persisted.getCurrency());
    assertNotNull(persisted.getId());
    // assertNotNull(persisted.getInserted());
  }
}
