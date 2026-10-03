-- ============================================================================
-- V5__add_stay_lifecycle.sql
-- Reception Service: Stay Lifecycle, Check-in Integration, Room History
-- ============================================================================

-- 1. Reservation terminal timestamp for check-in
ALTER TABLE reception.reservations
    ADD COLUMN checked_in_at TIMESTAMPTZ NULL;

-- 2. Update status constraint to include CHECKED_IN
ALTER TABLE reception.reservations
    DROP CONSTRAINT chk_reservations_status;

ALTER TABLE reception.reservations
    ADD CONSTRAINT chk_reservations_status
    CHECK (status IN ('CONFIRMED', 'CANCELLED', 'CHECKED_IN', 'NO_SHOW'));

-- 3. Update terminal timestamps constraint
ALTER TABLE reception.reservations
    DROP CONSTRAINT chk_reservations_terminal_timestamps;

ALTER TABLE reception.reservations
    ADD CONSTRAINT chk_reservations_terminal_timestamps
    CHECK (
        (status = 'CONFIRMED'  AND cancelled_at IS NULL AND checked_in_at IS NULL AND no_show_at IS NULL) OR
        (status = 'CANCELLED'  AND cancelled_at IS NOT NULL AND checked_in_at IS NULL AND no_show_at IS NULL) OR
        (status = 'CHECKED_IN' AND checked_in_at IS NOT NULL AND cancelled_at IS NULL AND no_show_at IS NULL) OR
        (status = 'NO_SHOW'    AND no_show_at IS NOT NULL AND cancelled_at IS NULL AND checked_in_at IS NULL)
    );

-- 4. Link GuestStay to Reservation (1-to-1 natural idempotency)
ALTER TABLE reception.guest_stays
    ADD COLUMN reservation_id UUID NULL;

ALTER TABLE reception.guest_stays
    ADD CONSTRAINT fk_guest_stays_reservation
    FOREIGN KEY (reservation_id)
    REFERENCES reception.reservations (id)
    ON DELETE RESTRICT;

CREATE UNIQUE INDEX uq_guest_stays_reservation_id
    ON reception.guest_stays (reservation_id)
    WHERE reservation_id IS NOT NULL;

-- 5. Stay Room History Table (Variant B)
CREATE TABLE reception.stay_room_history (
    id           UUID PRIMARY KEY,
    stay_id      UUID NOT NULL,
    room_number  VARCHAR(16) NOT NULL,
    started_at   TIMESTAMPTZ NOT NULL,
    ended_at     TIMESTAMPTZ NULL,
    reason       VARCHAR(255) NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_stay_room_history_stay
        FOREIGN KEY (stay_id)
        REFERENCES reception.guest_stays (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_stay_room_history_room
        FOREIGN KEY (room_number)
        REFERENCES reception.rooms (room_number)
        ON DELETE RESTRICT,

    CONSTRAINT chk_stay_room_history_interval
        CHECK (ended_at IS NULL OR ended_at >= started_at)
);

-- Invariant: Maximum one open room segment per stay
CREATE UNIQUE INDEX uq_stay_room_history_active_segment
    ON reception.stay_room_history (stay_id)
    WHERE ended_at IS NULL;

-- Query optimization indexes
CREATE INDEX idx_stay_room_history_stay_started
    ON reception.stay_room_history (stay_id, started_at);

CREATE INDEX idx_stay_room_history_room
    ON reception.stay_room_history (room_number);

CREATE INDEX idx_reservations_guest_status
    ON reception.reservations (guest_id, status);

CREATE INDEX idx_guest_stays_guest_status
    ON reception.guest_stays (guest_id, checked_out_at);

-- 6. Backfill historical stays into stay_room_history
INSERT INTO reception.stay_room_history (
    id,
    stay_id,
    room_number,
    started_at,
    ended_at,
    reason,
    created_at
)
SELECT
    gen_random_uuid(),
    s.id,
    s.room_number,
    s.checked_in_at,
    s.checked_out_at,
    'INITIAL_CHECKIN',
    s.checked_in_at
FROM reception.guest_stays s
WHERE NOT EXISTS (
    SELECT 1 FROM reception.stay_room_history h WHERE h.stay_id = s.id
);
