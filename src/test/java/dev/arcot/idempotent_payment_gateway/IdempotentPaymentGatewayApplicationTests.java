package dev.gabriel.idempotent_payment_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Boots the full payment gateway integration context (Postgres + Redis + Flyway) for smoke coverage.
 */
@SpringBootTest
class IdempotentPaymentGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
