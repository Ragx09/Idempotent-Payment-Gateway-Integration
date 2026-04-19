package dev.gabriel.idempotent_payment_gateway.webhook;

import dev.gabriel.idempotent_payment_gateway.config.PaymentWebhookProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Payment webhook security: validates {@code X-Webhook-Signature: sha256=<hex>} using
 * HMAC-SHA256(secret, rawBody) with constant-time comparison for the gateway integration service.
 */
@Component
@RequiredArgsConstructor
public class WebhookSignatureValidator {

    private static final String HEADER_PREFIX = "sha256=";

    private final PaymentWebhookProperties properties;

    public void validate(String rawBody, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new WebhookSignatureException("missing X-Webhook-Signature");
        }
        String trimmed = signatureHeader.trim();
        if (!trimmed.regionMatches(true, 0, HEADER_PREFIX, 0, HEADER_PREFIX.length())) {
            throw new WebhookSignatureException("invalid signature format");
        }
        String expectedHex = trimmed.substring(HEADER_PREFIX.length());
        byte[] expected;
        try {
            expected = HexFormat.of().parseHex(expectedHex);
        } catch (IllegalArgumentException e) {
            throw new WebhookSignatureException("invalid signature hex");
        }

        byte[] computed = hmacSha256(properties.getSecret().getBytes(StandardCharsets.UTF_8),
                rawBody.getBytes(StandardCharsets.UTF_8));
        if (!constantTimeEquals(expected, computed)) {
            throw new WebhookSignatureException("signature mismatch");
        }
    }

    private static byte[] hmacSha256(byte[] secret, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(message);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 not available", e);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
