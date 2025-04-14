package hu.gerab.payment.domain;

import hu.gerab.payment.Comparables;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "transactions")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class Transaction {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_sequence")
  @SequenceGenerator(
      name = "transaction_sequence",
      sequenceName = "transaction_sequence",
      allocationSize = 1)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "request_id", nullable = false)
  private String requestId;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "currency", nullable = false)
  private String currency;

  @Column(name = "inserted", insertable = false, updatable = false)
  private Instant inserted;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Transaction that)) {
      return false;
    }
    return Objects.equals(id, that.id)
        && Objects.equals(userId, that.userId)
        && Comparables.compareEquals(amount, that.amount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, userId, amount);
  }
}
