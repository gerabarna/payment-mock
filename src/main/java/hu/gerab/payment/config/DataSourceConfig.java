package hu.gerab.payment.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

  @Bean
  @Primary
  @Profile("!test") // This bean is active for all profiles except 'test'
  public DataSource dataSource() {
    // Your production data source configuration
    return DataSourceBuilder.create()
        .driverClassName("org.postgresql.Driver")
        .url("jdbc:postgresql://localhost:5432/kibit")
        .username("kibit")
        .password("kibit")
        .build();
  }
}
