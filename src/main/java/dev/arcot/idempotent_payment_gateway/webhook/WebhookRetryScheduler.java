package dev.gabriel.idempotent_payment_gateway.webhook;

import dev.gabriel.idempotent_payment_gateway.config.PaymentWebhookProperties;
import dev.gabriel.idempotent_payment_gateway.model.entities.WebhookEvent;
import dev.gabriel.idempotent_payment_gateway.model.enums.WebhookEventStatus;
import dev.gabriel.idempotent_payment_gateway.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Webhook retry handling: periodically re-drives FAILED {@link dev.gabriel.idempotent_payment_gateway.model.entities.WebhookEvent}
 * rows until {@code payment.webhook.max-attempts}, complementing provider-side retries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookRetryScheduler {

    private final WebhookEventRepository webhookEventRepository;
    private final PaymentWebhookService paymentWebhookService;
    private final PaymentWebhookProperties properties;

    @Scheduled(fixedDelayString = "${payment.webhook.retry-interval-ms:5000}")
    public void retryFailedWebhooks() {
        List<WebhookEvent> batch = webhookEventRepository.findTop50ByStatusOrderByUpdatedAtAsc(WebhookEventStatus.FAILED);
        for (WebhookEvent event : batch) {
            if (event.getAttemptCount() >= properties.getMaxAttempts()) {
                continue;
            }
            try {
                paymentWebhookService.retryFailed(event);
            } catch (Exception ex) {
                log.warn("scheduled webhook retry failed for eventId={}", event.getEventId(), ex);
            }
        }
    }
}
