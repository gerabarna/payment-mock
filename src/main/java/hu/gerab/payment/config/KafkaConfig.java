package hu.gerab.payment.config;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@Slf4j
public class KafkaConfig {

  @Value("${kafka.bootstrap.host}")
  private String bootstrapServers;

  @Bean
  public ProducerFactory<Long, Object> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    LOGGER.info(
        "Creating Kafka connection configuration with bootstrap host(s)={}", bootstrapServers);
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.LongSerializer");
    configProps.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        "org.springframework.kafka.support.serializer.JsonSerializer");
    return new DefaultKafkaProducerFactory<>(configProps);
  }

  @Bean
  public KafkaTemplate<Long, Object> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }
}
