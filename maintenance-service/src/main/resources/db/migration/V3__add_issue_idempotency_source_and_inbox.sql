-- ==============================================================================
-- V3__add_issue_idempotency_source_and_inbox.sql
-- HotelOS Maintenance Service: Idempotency, Impact, Cleanup Tracking & Inbox
-- ==============================================================================

-- 1. Add columns to maintenance_issues
ALTER TABLE maintenance.maintenance_issues
    ADD COLUMN idempotency_key VARCHAR(128),
    ADD COLUMN request_fingerprint VARCHAR(64),
    ADD COLUMN source VARCHAR(32) NOT NULL DEFAULT 'DIRECT',
    ADD COLUMN source_reference_id VARCHAR(64),
    ADD COLUMN requires_cleaning BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN engineering_impact VARCHAR(32) NOT NULL DEFAULT 'OUT_OF_ORDER',
    ADD COLUMN cleanup_pending BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN cleanup_acknowledged_at TIMESTAMPTZ;

-- 2. Constraints for maintenance_issues
ALTER TABLE maintenance.maintenance_issues
    ADD CONSTRAINT chk_maintenance_issues_source
        CHECK (source IN ('DIRECT', 'HOUSEKEEPING')),
    ADD CONSTRAINT chk_maintenance_issues_engineering_impact
        CHECK (engineering_impact IN ('OUT_OF_ORDER', 'OUT_OF_SERVICE', 'OPERATIONAL')),
    ADD CONSTRAINT chk_maintenance_issues_idempotency_pair
        CHECK (
            (idempotency_key IS NULL AND request_fingerprint IS NULL) OR
            (idempotency_key IS NOT NULL AND request_fingerprint IS NOT NULL)
        ),
    ADD CONSTRAINT chk_maintenance_issues_hk_idempotency_required
        CHECK (
            source != 'HOUSEKEEPING' OR (idempotency_key IS NOT NULL AND request_fingerprint IS NOT NULL)
        ),
    ADD CONSTRAINT chk_maintenance_issues_cleanup_consistency
        CHECK (
            (requires_cleaning = FALSE AND cleanup_pending = FALSE AND cleanup_acknowledged_at IS NULL) OR
            (requires_cleaning = TRUE AND cleanup_pending = TRUE AND status = 'RESOLVED' AND cleanup_acknowledged_at IS NULL) OR
            (requires_cleaning = TRUE AND cleanup_pending = FALSE AND status = 'RESOLVED' AND cleanup_acknowledged_at IS NOT NULL) OR
            (requires_cleaning = TRUE AND cleanup_pending = FALSE AND status IN ('OPEN', 'ASSIGNED', 'CANCELLED') AND cleanup_acknowledged_at IS NULL)
        );

-- 3. Partial unique index for idempotency
CREATE UNIQUE INDEX uq_maintenance_issues_source_idempotency
    ON maintenance.maintenance_issues (source, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- 4. Create maintenance.inbox_events
CREATE TABLE maintenance.inbox_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    source VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    correlation_id VARCHAR(64),
    envelope_hash VARCHAR(64) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_maintenance_inbox_processed_at ON maintenance.inbox_events (processed_at);
