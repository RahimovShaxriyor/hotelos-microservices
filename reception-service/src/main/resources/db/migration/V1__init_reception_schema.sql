-- ==============================================================================
-- HotelOS — Reception Persistence Foundation (P1.1)
-- Schema: reception
-- Flyway Version: V1
-- ==============================================================================

-- 1. reception.rooms
CREATE TABLE reception.rooms (
    room_number             VARCHAR(16) PRIMARY KEY,
    floor                   INT NOT NULL,
    room_type               VARCHAR(32) NOT NULL,
    proximity               VARCHAR(32) NOT NULL,
    nightly_rate            NUMERIC(19,2) NOT NULL,
    occupancy_status        VARCHAR(32) NOT NULL DEFAULT 'VACANT',
    housekeeping_status     VARCHAR(32) NOT NULL DEFAULT 'CLEAN',
    engineering_status      VARCHAR(32) NOT NULL DEFAULT 'OPERATIONAL',
    clean_since             TIMESTAMPTZ,
    turnover_pending        BOOLEAN NOT NULL DEFAULT FALSE,
    turnover_correlation_id VARCHAR(64),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_rooms_occupancy_status
        CHECK (occupancy_status IN ('VACANT', 'OCCUPIED')),
    CONSTRAINT chk_rooms_housekeeping_status
        CHECK (housekeeping_status IN ('CLEAN', 'DIRTY', 'CLEANING')),
    CONSTRAINT chk_rooms_engineering_status
        CHECK (engineering_status IN ('OPERATIONAL', 'OUT_OF_SERVICE', 'OUT_OF_ORDER')),
    CONSTRAINT chk_rooms_room_type
        CHECK (room_type IN ('SINGLE', 'DOUBLE', 'SUITE', 'ACCESSIBLE')),
    CONSTRAINT chk_rooms_nightly_rate
        CHECK (nightly_rate > 0),
    CONSTRAINT chk_rooms_floor
        CHECK (floor > 0),
    CONSTRAINT chk_rooms_clean_since
        CHECK ((housekeeping_status = 'CLEAN' AND clean_since IS NOT NULL) OR (housekeeping_status IN ('DIRTY', 'CLEANING') AND clean_since IS NULL)),
    CONSTRAINT chk_rooms_turnover_correlation
        CHECK ((turnover_pending = TRUE AND turnover_correlation_id IS NOT NULL) OR (turnover_pending = FALSE AND turnover_correlation_id IS NULL)),
    CONSTRAINT chk_rooms_occupancy_turnover
        CHECK (NOT (occupancy_status = 'OCCUPIED' AND turnover_pending = TRUE))
);

-- 2. reception.guest_stays
CREATE TABLE reception.guest_stays (
    id                      UUID PRIMARY KEY,
    room_number             VARCHAR(16) NOT NULL REFERENCES reception.rooms(room_number),
    guest_name              VARCHAR(128) NOT NULL,
    check_in_date           DATE NOT NULL,
    booked_nights           INT NOT NULL,
    nightly_rate_at_checkin NUMERIC(19,2) NOT NULL,
    minibar_charge          NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    late_checkout_fee       NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    discount                NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    checked_in_at           TIMESTAMPTZ NOT NULL,
    checked_out_at          TIMESTAMPTZ,
    archived                BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_guest_stays_booked_nights
        CHECK (booked_nights > 0),
    CONSTRAINT chk_guest_stays_nightly_rate
        CHECK (nightly_rate_at_checkin > 0),
    CONSTRAINT chk_guest_stays_minibar_charge
        CHECK (minibar_charge >= 0),
    CONSTRAINT chk_guest_stays_late_checkout_fee
        CHECK (late_checkout_fee >= 0),
    CONSTRAINT chk_guest_stays_discount
        CHECK (discount >= 0),
    CONSTRAINT chk_guest_stays_checkout_order
        CHECK (checked_out_at IS NULL OR checked_out_at >= checked_in_at)
);

-- Partial unique index ensuring at most one active stay per room
CREATE UNIQUE INDEX uq_active_stay_per_room
    ON reception.guest_stays (room_number)
    WHERE checked_out_at IS NULL;

-- Index for historical stay time matching
CREATE INDEX idx_guest_stays_room_lookup
    ON reception.guest_stays (room_number, checked_in_at, checked_out_at);

-- 3. reception.room_service_charges (immutable financial ledger)
CREATE TABLE reception.room_service_charges (
    id                      UUID PRIMARY KEY,
    order_id                VARCHAR(64) NOT NULL,
    stay_id                 UUID NOT NULL REFERENCES reception.guest_stays(id) ON DELETE RESTRICT,
    amount                  NUMERIC(19,2) NOT NULL,
    source_event_id         UUID,
    charged_at              TIMESTAMPTZ NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_room_service_charges_amount
        CHECK (amount > 0)
);

-- Durable business idempotency key
CREATE UNIQUE INDEX uq_room_service_charges_order_id
    ON reception.room_service_charges (order_id);

-- Index for charge aggregation by stay
CREATE INDEX idx_room_service_charges_stay_id
    ON reception.room_service_charges (stay_id);
