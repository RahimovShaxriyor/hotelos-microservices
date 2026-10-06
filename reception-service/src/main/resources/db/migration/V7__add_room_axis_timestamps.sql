-- ==============================================================================
-- HotelOS — Reception Service (P3.7)
-- Schema: reception
-- Flyway Version: V7
-- Description: Add authoritative room axis timestamps for occupancy and turnover
-- ==============================================================================

-- 1. Add authoritative timestamp columns
ALTER TABLE reception.rooms
    ADD COLUMN occupancy_changed_at TIMESTAMPTZ,
    ADD COLUMN turnover_changed_at TIMESTAMPTZ;

-- 2. One-time deterministic cutover baseline for existing rows
UPDATE reception.rooms
SET occupancy_changed_at = CURRENT_TIMESTAMP,
    turnover_changed_at = CURRENT_TIMESTAMP;

-- 3. Set NOT NULL on both columns
ALTER TABLE reception.rooms
    ALTER COLUMN occupancy_changed_at SET NOT NULL,
    ALTER COLUMN turnover_changed_at SET NOT NULL;

-- 4. Replace turnover correlation check constraint with hardened nonblank check
ALTER TABLE reception.rooms
    DROP CONSTRAINT IF EXISTS chk_rooms_turnover_correlation;

ALTER TABLE reception.rooms
    ADD CONSTRAINT chk_rooms_turnover_correlation_state
    CHECK (
        (turnover_pending = TRUE AND turnover_correlation_id IS NOT NULL AND length(trim(turnover_correlation_id)) > 0) OR
        (turnover_pending = FALSE AND turnover_correlation_id IS NULL)
    );
