package dev.gabriel.idempotent_payment_gateway;

import dev.gabriel.idempotent_payment_gateway.config.PaymentWebhookProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Idempotent Payment Gateway Integration microservice entrypoint: REST idempotency, signed webhooks
 * with retry scheduling, Redis distributed locks, and Postgres-backed ledger processing.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(PaymentWebhookProperties.class)
public class IdempotentPaymentGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdempotentPaymentGatewayApplication.class, args);
	}

}
