package hu.gerab.payment.repository;

import hu.gerab.payment.domain.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query(nativeQuery = true, value = "select * from transactions "
            + "where user_id = :userId "
            + "order by inserted desc")
    List<Transaction> findByUserId(@Param("userId") Long userId);
}
