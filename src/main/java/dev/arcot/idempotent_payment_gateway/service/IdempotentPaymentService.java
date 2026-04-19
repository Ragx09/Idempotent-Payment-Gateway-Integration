package dev.gabriel.idempotent_payment_gateway.service;

import dev.gabriel.idempotent_payment_gateway.model.dtos.TransactionRequestDto;
import dev.gabriel.idempotent_payment_gateway.model.dtos.TransactionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

/**
 * Idempotent payment orchestration for the gateway integration service: Redis response cache plus
 * distributed lock fencing so concurrent retries (same Idempotency-Key or webhook {@code eventId})
 * cannot double-post; complements DB pessimistic locking inside {@link TransactionService}.
 */
@Service
@RequiredArgsConstructor
public class IdempotentPaymentService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(45);
    private static final Duration WAIT_FOR_PEER = Duration.ofSeconds(35);
    private static final long POLL_MS = 50L;

    private final IdempotencyService idempotencyService;
    private final TransactionService transactionService;
    private final DistributedLockService distributedLockService;

    public TransactionResponseDto processPayment(String idempotencyKey, TransactionRequestDto request) {
        return execute(IdempotencyScope.PAYMENT_API, idempotencyKey, request);
    }

    public TransactionResponseDto processWebhookPayment(String webhookEventId, TransactionRequestDto request) {
        return execute(IdempotencyScope.PAYMENT_WEBHOOK, webhookEventId, request);
    }

    private TransactionResponseDto execute(IdempotencyScope scope, String idempotencyKey, TransactionRequestDto request) {
        TransactionResponseDto cached = idempotencyService.get(scope, idempotencyKey);
        if (cached != null) {
            return cached;
        }

        String lockKey = scope.getLockPrefix() + idempotencyKey;
        String token = distributedLockService.newToken();
        long deadline = System.currentTimeMillis() + WAIT_FOR_PEER.toMillis();

        while (System.currentTimeMillis() < deadline) {
            if (distributedLockService.tryLock(lockKey, token, LOCK_TTL)) {
                try {
                    return processAndCache(scope, idempotencyKey, request);
                } finally {
                    distributedLockService.unlock(lockKey, token);
                }
            }
            sleepQuietly(POLL_MS);
            cached = idempotencyService.get(scope, idempotencyKey);
            if (cached != null) {
                return cached;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "idempotency key is still being processed; retry with the same key");
    }

    private TransactionResponseDto processAndCache(
            IdempotencyScope scope,
            String idempotencyKey,
            TransactionRequestDto request) {
        TransactionResponseDto again = idempotencyService.get(scope, idempotencyKey);
        if (again != null) {
            return again;
        }
        TransactionResponseDto result = transactionService.processTransaction(request);
        idempotencyService.save(scope, idempotencyKey, result);
        return result;
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "interrupted while waiting for lock");
        }
    }
}
