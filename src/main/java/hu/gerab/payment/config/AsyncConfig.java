package hu.gerab.payment.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Configuration
@EnableAsync
@Component
public class AsyncConfig {

  public static final String PAYMENT_SERVICE_EXECUTOR = "paymentServiceExecutor";

  @Value("${service.payment.pool.size}")
  private int threadPoolSize;

  private ThreadPoolTaskExecutor executor;

  @Bean(name = PAYMENT_SERVICE_EXECUTOR)
  public Executor taskExecutor() {
    executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(threadPoolSize);
    executor.setMaxPoolSize(threadPoolSize);
    executor.setThreadNamePrefix("A-PSE-");
    executor.initialize();
    /* This pool deals with the queued transactions. If the application is shut down
    prematurely some queued, but unprocessed transactions may be lost. The following
    settings should mitigate this to some degree, but not completely.

    To prevent this, either the application would need to rely on some distributed
    structure to store the unprocessed transaction requests or the queued transactions
    could be 'logged' in kafka or some other system, so they can be checked during
    startup for completion.
    */
    executor.setQueueCapacity(300);
    executor.setAwaitTerminationSeconds(300);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    return executor;
  }
}
