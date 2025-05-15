package hu.gerab.payment.service;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

public interface MessagingService {
  void sendTransactionNotification(TransactionNotification notification);

  void sendTransactionNotification(long userId, TransactionNotification notification);

  @Data
  @Builder
  class TransactionNotification {
    private Long senderId;
    private Long receiverId;
    private String requestId;
    private BigDecimal amount;
    private String currency;
    private boolean successful;
    private String error;
  }
}
