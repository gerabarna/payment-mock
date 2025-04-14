package hu.gerab.payment.service;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessagingService {

  private final KafkaTemplate<Long, Object> kafkaTemplate;

  @Value("${kafka.transaction.topic}")
  private String topic;

  public MessagingService(KafkaTemplate<Long, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  /**
   * Sends notifications about the transactions to the users.
   * The implementation uses kafka for message delivery
   * @param notification
   */
  public void sendTransactionNotification(TransactionNotification notification) {
    /*
    Since we are using the userId as key ordering is ensured, and the clients only need to read their specific
    partitions.
    However this approach will only work well for a limited number of users for two reasons:
      - clients still need to filter the messages as multiple keys can be mapped to the same partition
      - With large number of users a large number of partitions would be created. This leads to a large number of open
      files on the kafka node, which can degrade performance
     */
    kafkaTemplate.send(topic, notification.getUserId(), notification);
  }

  @Data
  @Builder
  public static class TransactionNotification {
    private Long userId;
    private String requestId;
    private BigDecimal amount;
    private String currency;
    private boolean successful;
    private String error;
  }
}
