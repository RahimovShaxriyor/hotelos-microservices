-- ==============================================================================
-- V3__add_maintenance_task_source_and_cleanup_requirements.sql
-- HotelOS Housekeeping Service: Support MAINTENANCE Task Source & Requirements Table
-- ==============================================================================

-- 1. Add audit column source_reference_id to cleaning_tasks
ALTER TABLE housekeeping.cleaning_tasks
    ADD COLUMN source_reference_id VARCHAR(64);

-- 2. Update task source check constraint
ALTER TABLE housekeeping.cleaning_tasks
    DROP CONSTRAINT chk_cleaning_tasks_source;

ALTER TABLE housekeeping.cleaning_tasks
    ADD CONSTRAINT chk_cleaning_tasks_source
        CHECK (task_source IN ('TURNOVER', 'MANUAL', 'MAINTENANCE'));

-- 3. Update task source correlation check constraint
ALTER TABLE housekeeping.cleaning_tasks
    DROP CONSTRAINT chk_cleaning_tasks_source_correlation;

ALTER TABLE housekeeping.cleaning_tasks
    ADD CONSTRAINT chk_cleaning_tasks_source_correlation
        CHECK (
            (task_source = 'TURNOVER' AND turnover_correlation_id IS NOT NULL AND btrim(turnover_correlation_id) <> '' AND length(turnover_correlation_id) <= 64) OR
            (task_source = 'MAINTENANCE' AND turnover_correlation_id IS NULL) OR
            (task_source = 'MANUAL' AND turnover_correlation_id IS NULL)
        );

-- 4. Cancellation constraint (only MANUAL may become CANCELLED)
ALTER TABLE housekeeping.cleaning_tasks
    DROP CONSTRAINT chk_cleaning_tasks_cancellation;

ALTER TABLE housekeeping.cleaning_tasks
    ADD CONSTRAINT chk_cleaning_tasks_cancellation
        CHECK (task_status != 'CANCELLED' OR task_source = 'MANUAL');

-- 5. Table of maintenance cleanup requirements
CREATE TABLE housekeeping.maintenance_cleanup_requirements (
    issue_id UUID PRIMARY KEY,
    room_number VARCHAR(16) NOT NULL,
    maintenance_completed_at TIMESTAMPTZ NOT NULL,
    satisfied_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_hk_cleanup_req_room_len CHECK (length(trim(room_number)) > 0 AND length(room_number) <= 16),
    CONSTRAINT chk_hk_cleanup_req_timestamps CHECK (satisfied_at IS NULL OR satisfied_at >= maintenance_completed_at)
);

CREATE INDEX idx_hk_cleanup_req_room_pending
    ON housekeeping.maintenance_cleanup_requirements (room_number)
    WHERE satisfied_at IS NULL;
