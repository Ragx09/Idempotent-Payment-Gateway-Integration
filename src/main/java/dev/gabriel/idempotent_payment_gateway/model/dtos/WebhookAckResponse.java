package dev.gabriel.idempotent_payment_gateway.model.dtos;

import java.util.UUID;

/**
 * Acknowledgement payload returned to payment providers after webhook ingestion.
 */
public record WebhookAckResponse(
        String eventId,
        String status,
        UUID transactionId,
        String detail
) {}
