package hu.gerab.payment.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import hu.gerab.payment.domain.Currency;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementation for the {@link PaymentService}. This is split in 2 main parts. This class mainly
 * handles some threading/task queuing things, and forwards the actual calls to {@link
 * PaymentServiceHelper}. This is needed as the @{@link
 * org.springframework.transaction.annotation.Transactional} annotation is not respected for calls
 * within a class ( due to how proxies work )
 *
 * <p>For further explanation why a simple @Async was not used please see: {@link
 * PaymentServiceImpl::getExecutorForUser}
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

  private PaymentServiceHelper paymentServiceHelper;
  private ConcurrentHashMap<Long, ExecutorService> idGroupToExecutors;
  private int idGroupThreadLimit;

  public PaymentServiceImpl(
      PaymentServiceHelper paymentServiceHelper,
      @Value("${service.payment.idGroupThreadLimit}") int idGroupThreadLimit) {
    this.paymentServiceHelper = paymentServiceHelper;
    this.idGroupThreadLimit = idGroupThreadLimit;
    this.idGroupToExecutors = new ConcurrentHashMap<>();
  }

  /**
   * Using simply @Async on the {@link this::processTransaction} can lead to racing conditions,
   * as @Async does not ensure that a task that was submitted before, will be completely processed
   * before the next message. This means that transactions belonging to the same user but processed
   * by different threads can be essentially re-ordered. Even DB level locking would not protect
   * against this. Thus we must ensure that transactions belonging to the same user are processed
   * sequentially with respect to the user (this is achieved here by assigning Executors base on the
   * userId). This still may not be enough if transactions for the same user are processed on
   * different nodes ( if we would scale to multiple nodes this application ). To protect against
   * that we either need to make sure requests for the same user are routed to the same node. in
   * which case this solution would be sufficient. Or we would need to create some intermediate,
   * distributed queues to order the transactions. Kafka may be appropriate for this however, for
   * now on a single node, this solution should suffice.
   *
   * <p>This also means I do not need to use locks as the messages belonging to the same user are
   * always processed essentially sequentially
   *
   * @param userId
   * @return
   */
  public ExecutorService getExecutorForUser(long userId) {
    final long group = userId % idGroupThreadLimit;
    ExecutorService executorService =
        idGroupToExecutors.computeIfAbsent(
            group,
            groupId -> {
              ThreadFactory factory =
                  new ThreadFactoryBuilder()
                      .setNameFormat("payment-" + groupId)
                      .setUncaughtExceptionHandler(
                          (thread, ex) -> LOGGER.error("Unhandled Exception task!", ex))
                      .setDaemon(false)
                      .build();
              return new ThreadPoolExecutor(
                  1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), factory);
            });
    return executorService;
  }

  @Override
  public Future<Boolean> processTransaction(
      String requestId, long userId, BigDecimal amount, Currency currency) {
    final ExecutorService executor = getExecutorForUser(userId);
    return CompletableFuture.supplyAsync(
        () -> paymentServiceHelper.processTransaction(requestId, userId, amount, currency),
        executor);
  }

  @PreDestroy
  public void shutdown() {
    idGroupToExecutors.values().forEach(ExecutorService::shutdown);
  }
}
