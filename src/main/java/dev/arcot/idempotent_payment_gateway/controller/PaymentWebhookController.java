package dev.gabriel.idempotent_payment_gateway.controller;

import dev.gabriel.idempotent_payment_gateway.model.dtos.WebhookAckResponse;
import dev.gabriel.idempotent_payment_gateway.webhook.PaymentWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Inbound payment provider webhooks: HMAC signature validation + idempotent processing + retry handling")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping(value = "/payments", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Payment webhook", description = "Accepts signed JSON callbacks; validates X-Webhook-Signature; credits accounts idempotently per eventId with persisted status and scheduled retries on failure.")
    public ResponseEntity<WebhookAckResponse> receivePaymentWebhook(
            @Parameter(description = "HMAC-SHA256 signature: sha256=<hex>", required = true)
            @RequestHeader("X-Webhook-Signature") String signature,
            @RequestBody String rawBody
    ) {
        return paymentWebhookService.ingest(rawBody, signature);
    }
}
