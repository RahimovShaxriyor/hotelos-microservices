-- ==============================================================================
-- HotelOS Room Service — V4 Migration
-- Schema: room_service
-- Adds Stay Ordering Sessions and Stay Binding to Orders
-- ==============================================================================

CREATE TABLE room_service.stay_ordering_sessions (
    stay_id UUID PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    freeze_id UUID NULL,
    frozen_at TIMESTAMPTZ NULL,
    closed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_stay_ordering_session_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    CONSTRAINT chk_stay_ordering_session_state CHECK (
        (status = 'ACTIVE' AND freeze_id IS NULL AND frozen_at IS NULL AND closed_at IS NULL) OR
        (status = 'FROZEN' AND freeze_id IS NOT NULL AND frozen_at IS NOT NULL AND closed_at IS NULL) OR
        (status = 'CLOSED' AND freeze_id IS NOT NULL AND frozen_at IS NOT NULL AND closed_at IS NOT NULL AND closed_at >= frozen_at)
    )
);

CREATE UNIQUE INDEX uq_stay_ordering_sessions_freeze_id
    ON room_service.stay_ordering_sessions (freeze_id)
    WHERE freeze_id IS NOT NULL;

ALTER TABLE room_service.orders
    ADD COLUMN stay_id UUID NULL;

CREATE INDEX idx_orders_stay_id_status
    ON room_service.orders (stay_id, status);
