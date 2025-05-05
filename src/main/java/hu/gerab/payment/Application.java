package hu.gerab.payment;

import java.util.Collection;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

@SpringBootApplication
@Slf4j
public class Application {

  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SpringApplication.run(Application.class, args);
  }

  @EventListener
  public void handleContextRefreshed(ContextRefreshedEvent event) {
    ConfigurableEnvironment env =
        (ConfigurableEnvironment) event.getApplicationContext().getEnvironment();

    env.getPropertySources().stream()
        .filter(ps -> ps instanceof MapPropertySource)
        .map(ps -> ((MapPropertySource) ps).getSource().keySet())
        .flatMap(Collection::stream)
        .distinct()
        .sorted()
        .forEach(key -> LOGGER.info("Properties {}={}", key, env.getProperty(key)));
  }
}
