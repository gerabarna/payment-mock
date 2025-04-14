package hu.gerab.payment.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataSourceConfig {

  @Bean
  @Primary
  @Profile("!test") // This bean is active for all profiles except 'test', we use H2 there
  public DataSource dataSource(
      @Value("${db.postgres.url}") String url,
      @Value("${db.postgres.username}") String user,
      @Value("${db.postgres.password}") String pass) {
    return DataSourceBuilder.create()
        .driverClassName("org.postgresql.Driver")
        .url(url)
        .username(user)
        .password(pass)
        .build();
  }
}
