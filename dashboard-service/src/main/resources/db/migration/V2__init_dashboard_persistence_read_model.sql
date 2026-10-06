-- =============================================================================
-- HotelOS — Dashboard Service (P3.7)
-- Schema: dashboard_read
-- Flyway Version: V2
-- Description: Init Dashboard Persistence Read Model (CQRS Projections & Feed)
-- =============================================================================

-- 1. Dashboard Inbox
CREATE TABLE dashboard_read.inbox_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    source VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64),
    correlation_id VARCHAR(64),
    envelope_hash VARCHAR(64) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dashboard_inbox_processed_at
    ON dashboard_read.inbox_events (processed_at);

-- 2. Room Read Projection (Strictly Unknown/Null Defaults)
CREATE TABLE dashboard_read.room_projection (
    room_number VARCHAR(16) PRIMARY KEY,
    occupancy_status VARCHAR(32),
    housekeeping_status VARCHAR(32),
    engineering_status VARCHAR(32),
    turnover_pending BOOLEAN,
    turnover_correlation_id VARCHAR(64),
    occupancy_changed_at TIMESTAMPTZ,
    housekeeping_changed_at TIMESTAMPTZ,
    engineering_changed_at TIMESTAMPTZ,
    turnover_changed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_dashboard_room_number_len
        CHECK (length(trim(room_number)) > 0 AND length(room_number) <= 16),
    CONSTRAINT chk_dashboard_occupancy_status
        CHECK (occupancy_status IS NULL OR occupancy_status IN ('VACANT', 'OCCUPIED')),
    CONSTRAINT chk_dashboard_housekeeping_status
        CHECK (housekeeping_status IS NULL OR housekeeping_status IN ('CLEAN', 'DIRTY', 'CLEANING')),
    CONSTRAINT chk_dashboard_engineering_status
        CHECK (engineering_status IS NULL OR engineering_status IN ('OPERATIONAL', 'OUT_OF_SERVICE', 'OUT_OF_ORDER')),

    CONSTRAINT chk_dashboard_occupancy_ts_pair CHECK (
        (occupancy_status IS NULL AND occupancy_changed_at IS NULL) OR
        (occupancy_status IS NOT NULL AND occupancy_changed_at IS NOT NULL)
    ),
    CONSTRAINT chk_dashboard_housekeeping_ts_pair CHECK (
        (housekeeping_status IS NULL AND housekeeping_changed_at IS NULL) OR
        (housekeeping_status IS NOT NULL AND housekeeping_changed_at IS NOT NULL)
    ),
    CONSTRAINT chk_dashboard_engineering_ts_pair CHECK (
        (engineering_status IS NULL AND engineering_changed_at IS NULL) OR
        (engineering_status IS NOT NULL AND engineering_changed_at IS NOT NULL)
    ),
    CONSTRAINT chk_dashboard_turnover_ts_pair CHECK (
        (turnover_pending IS NULL AND turnover_changed_at IS NULL) OR
        (turnover_pending IS NOT NULL AND turnover_changed_at IS NOT NULL)
    ),

    CONSTRAINT chk_dashboard_turnover_correlation_state CHECK (
        (turnover_pending IS NULL AND turnover_correlation_id IS NULL) OR
        (turnover_pending = FALSE AND turnover_correlation_id IS NULL) OR
        (turnover_pending = TRUE AND turnover_correlation_id IS NOT NULL AND length(trim(turnover_correlation_id)) > 0)
    )
);

-- 3. Event Feed (Sanitized, PII-Free)
CREATE TABLE dashboard_read.event_feed (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    event_version INTEGER NOT NULL DEFAULT 1,
    source VARCHAR(64) NOT NULL,
    routing_key VARCHAR(128) NOT NULL,
    aggregate_id VARCHAR(64),
    correlation_id VARCHAR(64),
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    summary VARCHAR(256) NOT NULL,
    sanitized_payload TEXT NOT NULL
);

CREATE INDEX idx_dashboard_event_feed_canonical
    ON dashboard_read.event_feed (occurred_at DESC, received_at DESC, event_id DESC);
