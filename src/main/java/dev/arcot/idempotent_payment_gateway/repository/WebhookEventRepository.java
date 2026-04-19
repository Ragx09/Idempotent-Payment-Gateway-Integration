package dev.gabriel.idempotent_payment_gateway.repository;

import dev.gabriel.idempotent_payment_gateway.model.entities.WebhookEvent;
import dev.gabriel.idempotent_payment_gateway.model.enums.WebhookEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for inbound payment webhook events (idempotency by {@code eventId}, status for retries).
 */
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    Optional<WebhookEvent> findByEventId(String eventId);

    List<WebhookEvent> findTop50ByStatusOrderByUpdatedAtAsc(WebhookEventStatus status);
}
