package dev.gabriel.idempotent_payment_gateway.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Webhook integration settings: HMAC secret, retry ceilings, and scheduler interval for the
 * idempotent payment gateway microservice.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "payment.webhook")
public class PaymentWebhookProperties {

    /**
     * Shared secret used to validate {@code X-Webhook-Signature} (HMAC-SHA256 over raw body).
     */
    @NotBlank
    private String secret = "change-me-in-production";

    @Positive
    private int maxAttempts = 8;

    @Positive
    private long retryIntervalMs = 5000L;
}
