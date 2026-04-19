-- Webhook integration: persisted inbound events for signature-validated callbacks and retry handling.
CREATE TABLE webhook_event (
    id UUID PRIMARY KEY,
    event_id VARCHAR(128) NOT NULL UNIQUE,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    transaction_id UUID REFERENCES transaction(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_webhook_event_status ON webhook_event(status);
