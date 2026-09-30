-- ==============================================================================
-- HotelOS Reception Service — V4 Reservations Engine
-- ==============================================================================

CREATE TABLE reception.room_type_inventory_locks (
    room_type VARCHAR(32) PRIMARY KEY,

    CONSTRAINT chk_room_type_inventory_locks_type
        CHECK (room_type IN ('SINGLE', 'DOUBLE', 'SUITE', 'ACCESSIBLE'))
);

INSERT INTO reception.room_type_inventory_locks (room_type)
VALUES ('SINGLE'), ('DOUBLE'), ('SUITE'), ('ACCESSIBLE');

ALTER TABLE reception.rooms
    ADD CONSTRAINT uq_rooms_room_number_type UNIQUE (room_number, room_type);

CREATE TABLE reception.reservations (
    id                      UUID PRIMARY KEY,
    guest_id                UUID NOT NULL,
    room_type               VARCHAR(32) NOT NULL,
    assigned_room_number    VARCHAR(16),
    arrival_date            DATE NOT NULL,
    departure_date          DATE NOT NULL,
    status                  VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED',
    nightly_rate_snapshot   NUMERIC(19,2) NOT NULL,
    currency                VARCHAR(3) NOT NULL,
    idempotency_key         VARCHAR(128) NOT NULL,
    request_fingerprint     CHAR(64) NOT NULL,
    cancelled_at            TIMESTAMPTZ,
    no_show_at              TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reservations_guest
        FOREIGN KEY (guest_id) REFERENCES reception.guests(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reservations_inventory_lock
        FOREIGN KEY (room_type) REFERENCES reception.room_type_inventory_locks(room_type) ON DELETE RESTRICT,
    CONSTRAINT fk_reservations_assigned_room_type
        FOREIGN KEY (assigned_room_number, room_type)
        REFERENCES reception.rooms(room_number, room_type) ON DELETE RESTRICT,
    CONSTRAINT uq_reservations_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT chk_reservations_room_type
        CHECK (room_type IN ('SINGLE', 'DOUBLE', 'SUITE', 'ACCESSIBLE')),
    CONSTRAINT chk_reservations_dates CHECK (arrival_date < departure_date),
    CONSTRAINT chk_reservations_status
        CHECK (status IN ('CONFIRMED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT chk_reservations_rate CHECK (nightly_rate_snapshot > 0),
    CONSTRAINT chk_reservations_currency
        CHECK (length(currency) = 3 AND currency = upper(currency)),
    CONSTRAINT chk_reservations_idempotency_key
        CHECK (length(trim(idempotency_key)) > 0 AND idempotency_key = trim(idempotency_key)),
    CONSTRAINT chk_reservations_request_fingerprint
        CHECK (request_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_reservations_terminal_timestamps
        CHECK (
            (status = 'CONFIRMED' AND cancelled_at IS NULL AND no_show_at IS NULL)
            OR (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND no_show_at IS NULL)
            OR (status = 'NO_SHOW' AND cancelled_at IS NULL AND no_show_at IS NOT NULL)
        )
);

CREATE INDEX idx_reservations_active_type_dates
    ON reception.reservations (room_type, arrival_date, departure_date)
    WHERE status = 'CONFIRMED';

CREATE INDEX idx_reservations_active_assigned_room_dates
    ON reception.reservations (assigned_room_number, arrival_date, departure_date)
    WHERE status = 'CONFIRMED' AND assigned_room_number IS NOT NULL;

CREATE INDEX idx_reservations_guest_created
    ON reception.reservations (guest_id, created_at DESC);

CREATE INDEX idx_reservations_status_arrival
    ON reception.reservations (status, arrival_date);
