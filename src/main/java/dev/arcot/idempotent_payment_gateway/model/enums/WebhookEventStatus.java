package dev.gabriel.idempotent_payment_gateway.model.enums;

/**
 * Lifecycle of an inbound payment webhook stored for retries and idempotent replay semantics.
 */
public enum WebhookEventStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
