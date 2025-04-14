package hu.gerab.payment;

import hu.gerab.payment.config.TestDatabaseConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@ActiveProfiles("test")
@ContextConfiguration(classes = TestDatabaseConfig.class)
@SpringBootTest
class ApplicationTests {

	@Test
	void contextLoads() {
	}

}
