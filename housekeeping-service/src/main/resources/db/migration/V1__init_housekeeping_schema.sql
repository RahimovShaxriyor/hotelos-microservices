-- ==============================================================================
-- HotelOS — Housekeeping Persistence (P1.3)
-- Schema: housekeeping
-- Flyway Version: V1
-- ==============================================================================

-- 1. housekeeping.room_states
CREATE TABLE housekeeping.room_states (
    room_number                     VARCHAR(16) PRIMARY KEY,
    housekeeping_status             VARCHAR(16) NOT NULL DEFAULT 'CLEAN',
    active_turnover_correlation_id  VARCHAR(64),
    status_changed_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    clean_since                     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_room_states_room_number
        CHECK (length(trim(room_number)) > 0 AND length(room_number) <= 16),
    CONSTRAINT chk_room_states_status
        CHECK (housekeeping_status IN ('CLEAN', 'DIRTY', 'CLEANING')),
    CONSTRAINT chk_room_states_clean_since
        CHECK (
            (housekeeping_status = 'CLEAN' AND clean_since IS NOT NULL) OR
            (housekeeping_status IN ('DIRTY', 'CLEANING') AND clean_since IS NULL)
        ),
    CONSTRAINT chk_room_states_active_correlation
        CHECK (housekeeping_status != 'CLEAN' OR active_turnover_correlation_id IS NULL)
);

-- 2. housekeeping.cleaning_tasks
CREATE TABLE housekeeping.cleaning_tasks (
    id                          UUID PRIMARY KEY,
    room_number                 VARCHAR(16) NOT NULL REFERENCES housekeeping.room_states(room_number),
    task_source                 VARCHAR(16) NOT NULL,
    turnover_correlation_id     VARCHAR(64),
    task_status                 VARCHAR(16) NOT NULL DEFAULT 'WAITING',
    assigned_cleaner            VARCHAR(64) NOT NULL,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at                  TIMESTAMPTZ,
    completed_at                TIMESTAMPTZ,
    cancelled_at                TIMESTAMPTZ,

    CONSTRAINT chk_cleaning_tasks_room_len
        CHECK (length(trim(room_number)) > 0 AND length(room_number) <= 16),
    CONSTRAINT chk_cleaning_tasks_cleaner_len
        CHECK (length(trim(assigned_cleaner)) > 0 AND length(assigned_cleaner) <= 64),
    CONSTRAINT chk_cleaning_tasks_source
        CHECK (task_source IN ('TURNOVER', 'MANUAL')),
    CONSTRAINT chk_cleaning_tasks_source_correlation
        CHECK (
            (task_source = 'TURNOVER' AND turnover_correlation_id IS NOT NULL AND btrim(turnover_correlation_id) <> '' AND length(turnover_correlation_id) <= 64) OR
            (task_source = 'MANUAL' AND turnover_correlation_id IS NULL)
        ),
    CONSTRAINT chk_cleaning_tasks_status
        CHECK (task_status IN ('WAITING', 'CLEANING', 'CLEAN', 'CANCELLED')),
    CONSTRAINT chk_cleaning_tasks_cancellation
        CHECK (task_status != 'CANCELLED' OR task_source = 'MANUAL'),
    CONSTRAINT chk_cleaning_tasks_timestamps
        CHECK (
            (task_status = 'WAITING' AND started_at IS NULL AND completed_at IS NULL AND cancelled_at IS NULL) OR
            (task_status = 'CLEANING' AND started_at IS NOT NULL AND started_at >= created_at AND completed_at IS NULL AND cancelled_at IS NULL) OR
            (task_status = 'CLEAN' AND started_at IS NOT NULL AND started_at >= created_at AND completed_at IS NOT NULL AND completed_at >= started_at AND cancelled_at IS NULL) OR
            (task_status = 'CANCELLED' AND cancelled_at IS NOT NULL AND cancelled_at >= created_at AND completed_at IS NULL AND (started_at IS NULL OR cancelled_at >= started_at))
        )
);

-- Unique index on turnover_correlation_id for TURNOVER tasks
CREATE UNIQUE INDEX uq_cleaning_tasks_correlation
    ON housekeeping.cleaning_tasks (turnover_correlation_id)
    WHERE task_source = 'TURNOVER' AND turnover_correlation_id IS NOT NULL;

-- At most one active task per room (WAITING or CLEANING)
CREATE UNIQUE INDEX uq_cleaning_tasks_active_room
    ON housekeeping.cleaning_tasks (room_number)
    WHERE task_status IN ('WAITING', 'CLEANING');

-- Deterministic queue ordering (tie-broken by UUID primary key)
CREATE INDEX idx_cleaning_tasks_queue
    ON housekeeping.cleaning_tasks (created_at ASC, id ASC)
    WHERE task_status IN ('WAITING', 'CLEANING');

-- Foreign key lookup index
CREATE INDEX idx_cleaning_tasks_room
    ON housekeeping.cleaning_tasks (room_number);
