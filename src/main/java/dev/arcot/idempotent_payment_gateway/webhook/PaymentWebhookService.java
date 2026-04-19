package dev.gabriel.idempotent_payment_gateway.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.gabriel.idempotent_payment_gateway.config.PaymentWebhookProperties;
import dev.gabriel.idempotent_payment_gateway.model.dtos.WebhookAckResponse;
import dev.gabriel.idempotent_payment_gateway.model.dtos.WebhookPayloadDto;
import dev.gabriel.idempotent_payment_gateway.model.entities.WebhookEvent;
import dev.gabriel.idempotent_payment_gateway.model.enums.WebhookEventStatus;
import dev.gabriel.idempotent_payment_gateway.repository.WebhookEventRepository;
import dev.gabriel.idempotent_payment_gateway.service.DistributedLockService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Set;

/**
 * Inbound webhook ingress for payment gateway integration: HMAC verification, Redis ingress lock,
 * transactional ledger apply, and compatibility with provider retries and concurrent deliveries.
 */
@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private static final Duration INGRESS_LOCK_TTL = Duration.ofSeconds(45);
    private static final Duration INGRESS_WAIT = Duration.ofSeconds(35);
    private static final long POLL_MS = 50L;

    private final WebhookSignatureValidator signatureValidator;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final PaymentWebhookLedgerService ledgerService;
    private final DistributedLockService distributedLockService;
    private final WebhookEventRepository webhookEventRepository;
    private final PaymentWebhookProperties properties;

    public ResponseEntity<WebhookAckResponse> ingest(String rawBody, String signatureHeader) {
        signatureValidator.validate(rawBody, signatureHeader);
        WebhookPayloadDto dto = parsePayload(rawBody);
        if (!"payment.succeeded".equalsIgnoreCase(dto.type())) {
            return ResponseEntity.ok(new WebhookAckResponse(dto.eventId(), "ignored", null, "event type not handled"));
        }

        WebhookAckResponse body = withIngressLock(dto, rawBody);
        return ResponseEntity.ok(body);
    }

    public void retryFailed(WebhookEvent event) {
        WebhookEvent latest = webhookEventRepository.findById(event.getId()).orElse(null);
        if (latest == null) {
            return;
        }
        if (latest.getStatus() != WebhookEventStatus.FAILED) {
            return;
        }
        if (latest.getAttemptCount() >= properties.getMaxAttempts()) {
            return;
        }
        WebhookPayloadDto dto;
        try {
            dto = parsePayload(event.getPayload());
        } catch (ResponseStatusException ex) {
            return;
        }
        if (!"payment.succeeded".equalsIgnoreCase(dto.type())) {
            return;
        }
        withIngressLock(dto, event.getPayload());
    }

    private WebhookAckResponse withIngressLock(WebhookPayloadDto dto, String rawBody) {
        String lockKey = "lock:webhook:ingress:" + dto.eventId();
        String token = distributedLockService.newToken();
        long deadline = System.currentTimeMillis() + INGRESS_WAIT.toMillis();

        while (System.currentTimeMillis() < deadline) {
            if (distributedLockService.tryLock(lockKey, token, INGRESS_LOCK_TTL)) {
                try {
                    return ledgerService.apply(dto, rawBody);
                } finally {
                    distributedLockService.unlock(lockKey, token);
                }
            }
            sleepQuietly(POLL_MS);
            WebhookEvent done = webhookEventRepository.findByEventId(dto.eventId()).orElse(null);
            if (done != null && done.getStatus() == WebhookEventStatus.COMPLETED) {
                return new WebhookAckResponse(done.getEventId(), "duplicate", done.getTransactionId(), "already processed");
            }
        }

        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "webhook event is still being processed; provider should retry");
    }

    private WebhookPayloadDto parsePayload(String rawBody) {
        try {
            WebhookPayloadDto dto = objectMapper.readValue(rawBody, WebhookPayloadDto.class);
            Set<ConstraintViolation<WebhookPayloadDto>> violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, violations.iterator().next().getMessage());
            }
            return dto;
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid json payload");
        }
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "interrupted while waiting for webhook lock");
        }
    }
}
