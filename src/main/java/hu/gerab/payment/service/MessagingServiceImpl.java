package hu.gerab.payment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/** Simplistic messaging service to forward messages to kafka. */
@Service
public class MessagingServiceImpl implements MessagingService {

  private final KafkaTemplate<Long, Object> kafkaTemplate;

  @Value("${kafka.transaction.topic}")
  private String topic;

  @Autowired
  public MessagingServiceImpl(KafkaTemplate<Long, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  /**
   * Sends notifications about the transactions to the users. The implementation uses kafka for
   * message delivery
   *
   * @param notification the actual message body
   */
  @Override
  public void sendTransactionNotification(TransactionNotification notification) {
    final Long senderId = notification.getSenderId();
    if (senderId != null) {
      sendTransactionNotification(senderId, notification);
    }
    final Long receiverId = notification.getReceiverId();
    if (receiverId != null) {
      sendTransactionNotification(receiverId, notification);
    }
  }

  public void sendTransactionNotification(long userId, TransactionNotification notification) {
    /*
    Since we are using the userId as key ordering is ensured, and the clients only need to read
    their specific partitions. We duplicate the messages for sender and receiver to ensure that
    both users would get notifications just by reading their own partition.
    However this approach will only work well for a limited number of users for two reasons:
      - clients still need to filter the messages as multiple keys can be mapped to the same
      partition
      - With large number of users a large number of partitions would be created. This leads
      to a large number of open files on the kafka node, which can degrade performance
     */
    kafkaTemplate.send(topic, userId, notification);
  }
}
