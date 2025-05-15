package hu.gerab.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode(of = {"id", "userId", "requestId", "amount"})
public class Transaction {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_sequence")
  @SequenceGenerator(
      name = "transaction_sequence",
      sequenceName = "transaction_sequence",
      allocationSize = 1)
  private Long id;

  /**
   * Some form of balance topup should exist in the system (even if the current implementation does
   * not really support it). In that case this field should be null.
   */
  @Column(name = "sender_id")
  private Long senderId;

  /**
   * Some form of cash withdrawal should exist in the system (even if the current implementation
   * does not really support it). In that case this field should be null.
   */
  @Column(name = "receiver_id")
  private Long receiverId;

  @Column(name = "request_id", nullable = false)
  private String requestId;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "currency", nullable = false)
  private Currency currency;

  @Column(name = "inserted")
  private Instant inserted;
}
