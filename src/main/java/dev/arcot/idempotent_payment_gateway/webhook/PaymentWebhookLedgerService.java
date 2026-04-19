package dev.gabriel.idempotent_payment_gateway.webhook;

import dev.gabriel.idempotent_payment_gateway.config.PaymentWebhookProperties;
import dev.gabriel.idempotent_payment_gateway.model.dtos.TransactionRequestDto;
import dev.gabriel.idempotent_payment_gateway.model.dtos.TransactionResponseDto;
import dev.gabriel.idempotent_payment_gateway.model.dtos.WebhookAckResponse;
import dev.gabriel.idempotent_payment_gateway.model.dtos.WebhookPayloadDto;
import dev.gabriel.idempotent_payment_gateway.model.entities.WebhookEvent;
import dev.gabriel.idempotent_payment_gateway.model.enums.WebhookEventStatus;
import dev.gabriel.idempotent_payment_gateway.repository.WebhookEventRepository;
import dev.gabriel.idempotent_payment_gateway.service.IdempotentPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists webhook receipts and applies ledger credits idempotently (Postgres event row +
 * Redis webhook idempotency scope + {@code PESSIMISTIC_WRITE} account locking).
 */
@Service
@RequiredArgsConstructor
public class PaymentWebhookLedgerService {

    private final WebhookEventRepository webhookEventRepository;
    private final IdempotentPaymentService idempotentPaymentService;
    private final PaymentWebhookProperties properties;

    @Transactional
    public WebhookAckResponse apply(WebhookPayloadDto dto, String rawBody) {
        WebhookEvent event = webhookEventRepository.findByEventId(dto.eventId()).orElse(null);
        if (event != null && event.getStatus() == WebhookEventStatus.COMPLETED) {
            return new WebhookAckResponse(event.getEventId(), "duplicate", event.getTransactionId(), "already processed");
        }

        if (event != null && event.getStatus() == WebhookEventStatus.FAILED
                && event.getAttemptCount() >= properties.getMaxAttempts()) {
            return new WebhookAckResponse(
                    event.getEventId(),
                    "failed",
                    event.getTransactionId(),
                    event.getLastError());
        }

        if (event == null) {
            event = WebhookEvent.builder()
                    .eventId(dto.eventId())
                    .payload(rawBody)
                    .status(WebhookEventStatus.PROCESSING)
                    .attemptCount(1)
                    .build();
            event = webhookEventRepository.save(event);
        } else {
            event.setStatus(WebhookEventStatus.PROCESSING);
            event.setAttemptCount(event.getAttemptCount() + 1);
            webhookEventRepository.save(event);
        }

        try {
            TransactionRequestDto tr = new TransactionRequestDto(dto.accountId(), dto.amount(), "CREDIT");
            TransactionResponseDto tx = idempotentPaymentService.processWebhookPayment(dto.eventId(), tr);
            if (!"COMPLETED".equals(tx.status())) {
                throw new IllegalStateException(tx.message() != null ? tx.message() : "transaction not completed");
            }
            event.setStatus(WebhookEventStatus.COMPLETED);
            event.setTransactionId(tx.transactionId());
            event.setLastError(null);
            webhookEventRepository.save(event);
            return new WebhookAckResponse(dto.eventId(), "completed", tx.transactionId(), null);
        } catch (Exception ex) {
            event.setStatus(WebhookEventStatus.FAILED);
            event.setLastError(ex.getMessage());
            webhookEventRepository.save(event);
            return new WebhookAckResponse(dto.eventId(), "failed", null, ex.getMessage());
        }
    }
}
