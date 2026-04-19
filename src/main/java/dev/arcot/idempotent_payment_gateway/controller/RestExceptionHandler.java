package dev.gabriel.idempotent_payment_gateway.controller;

import dev.gabriel.idempotent_payment_gateway.webhook.WebhookSignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps webhook signature failures to HTTP 401 for clear provider integration semantics.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(WebhookSignatureException.class)
    public ResponseEntity<Map<String, String>> handleSignature(WebhookSignatureException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }
}
