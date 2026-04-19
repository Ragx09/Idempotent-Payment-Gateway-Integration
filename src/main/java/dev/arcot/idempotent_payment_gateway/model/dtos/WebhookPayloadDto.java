package dev.gabriel.idempotent_payment_gateway.model.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Minimal provider-agnostic payload for inbound payment webhooks handled by the gateway microservice.
 */
public record WebhookPayloadDto(
        @NotBlank String eventId,
        @NotBlank String type,
        @NotNull UUID accountId,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {}
