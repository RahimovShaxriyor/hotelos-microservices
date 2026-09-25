CREATE TABLE housekeeping.outbox_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id UUID NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    event_version INT NOT NULL DEFAULT 1,
    source VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(64),
    exchange_name VARCHAR(128) NOT NULL,
    routing_key VARCHAR(128) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    message_body TEXT NOT NULL,
    message_headers TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error TEXT,
    CONSTRAINT uq_housekeeping_outbox_event_id UNIQUE (event_id)
);

CREATE INDEX idx_housekeeping_outbox_pending_dispatch 
ON housekeeping.outbox_events (aggregate_id, id) 
WHERE published_at IS NULL;

CREATE TABLE housekeeping.inbox_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    source VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(64),
    envelope_hash VARCHAR(64) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_housekeeping_inbox_processed_at ON housekeeping.inbox_events (processed_at);
