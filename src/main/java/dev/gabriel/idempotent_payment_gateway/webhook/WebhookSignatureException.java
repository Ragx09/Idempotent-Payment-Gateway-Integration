package dev.gabriel.idempotent_payment_gateway.webhook;

/**
 * Raised when inbound webhook HMAC validation fails (invalid or missing signature).
 */
public class WebhookSignatureException extends RuntimeException {

    public WebhookSignatureException(String message) {
        super(message);
    }
}
