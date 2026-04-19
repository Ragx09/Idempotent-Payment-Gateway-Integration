package dev.gabriel.idempotent_payment_gateway.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Redis idempotency namespaces for the payment gateway microservice so API {@code Idempotency-Key}
 * values never collide with webhook {@code eventId} values.
 */
@Getter
@RequiredArgsConstructor
public enum IdempotencyScope {
    PAYMENT_API("idempotency:payment:", "lock:idempotency:payment:"),
    PAYMENT_WEBHOOK("idempotency:webhook:", "lock:idempotency:webhook:");

    private final String cachePrefix;
    private final String lockPrefix;
}
