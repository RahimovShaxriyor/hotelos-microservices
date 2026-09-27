-- ==============================================================================
-- V1__init_maintenance_schema.sql
-- HotelOS Maintenance Service Database Schema
-- ==============================================================================

-- 1. Persistent FIFO availability sequence for technicians
CREATE SEQUENCE IF NOT EXISTS maintenance.technician_availability_seq START WITH 1 INCREMENT BY 1;

-- 2. Technicians table with strict state, FIFO sequence, and defense-in-depth unique constraint
CREATE TABLE maintenance.technicians (
    name VARCHAR(64) PRIMARY KEY,
    state VARCHAR(32) NOT NULL,
    availability_sequence BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_technicians_state CHECK (state IN ('AVAILABLE', 'BUSY')),
    CONSTRAINT chk_technicians_sequence CHECK (
        (state = 'AVAILABLE' AND availability_sequence IS NOT NULL) OR
        (state = 'BUSY' AND availability_sequence IS NULL)
    ),
    CONSTRAINT uq_technicians_availability_sequence UNIQUE (availability_sequence)
);

-- Seed initial technicians
INSERT INTO maintenance.technicians (name, state, availability_sequence, created_at, updated_at)
VALUES
    ('Tech-1', 'AVAILABLE', nextval('maintenance.technician_availability_seq'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Tech-2', 'AVAILABLE', nextval('maintenance.technician_availability_seq'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3. Room engineering states table
CREATE TABLE maintenance.room_states (
    room_number VARCHAR(16) PRIMARY KEY,
    engineering_status VARCHAR(32) NOT NULL,
    status_changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_room_states_status CHECK (engineering_status IN ('OPERATIONAL', 'OUT_OF_SERVICE', 'OUT_OF_ORDER'))
);

-- 4. Maintenance issues table
CREATE TABLE maintenance.maintenance_issues (
    id UUID PRIMARY KEY,
    room_number VARCHAR(16) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(16) NOT NULL,
    priority_rank INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    assigned_technician VARCHAR(64),
    assigned_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_maintenance_issues_priority_mapping CHECK (
           (priority = 'CRITICAL' AND priority_rank = 1)
        OR (priority = 'HIGH'     AND priority_rank = 2)
        OR (priority = 'NORMAL'   AND priority_rank = 3)
        OR (priority = 'LOW'      AND priority_rank = 4)
    ),
    CONSTRAINT chk_issues_status CHECK (status IN ('OPEN', 'ASSIGNED', 'RESOLVED', 'CANCELLED')),
    CONSTRAINT chk_issues_assigned_tech CHECK (status != 'ASSIGNED' OR (assigned_technician IS NOT NULL AND assigned_at IS NOT NULL)),
    CONSTRAINT chk_issues_resolved_at CHECK (status != 'RESOLVED' OR resolved_at IS NOT NULL),
    CONSTRAINT chk_issues_cancelled_at CHECK (status != 'CANCELLED' OR cancelled_at IS NOT NULL),
    CONSTRAINT chk_issues_timestamps CHECK (
        (assigned_at IS NULL OR assigned_at >= created_at) AND
        (resolved_at IS NULL OR resolved_at >= created_at) AND
        (cancelled_at IS NULL OR cancelled_at >= created_at)
    )
);

-- 5. Indexes
-- Single-active-issue per technician defense-in-depth invariant
CREATE UNIQUE INDEX uq_maintenance_issues_active_tech
    ON maintenance.maintenance_issues (assigned_technician)
    WHERE status = 'ASSIGNED' AND assigned_technician IS NOT NULL;

-- High-performance queue ordering index (matching claim query)
CREATE INDEX idx_maintenance_issues_open_queue
    ON maintenance.maintenance_issues (priority_rank ASC, created_at ASC, id ASC)
    WHERE status = 'OPEN';

-- Active issues per room index (for countActiveIssuesForRoom queries)
CREATE INDEX idx_maintenance_issues_room_active
    ON maintenance.maintenance_issues (room_number)
    WHERE status IN ('OPEN', 'ASSIGNED');

-- Available technicians FIFO index
CREATE INDEX idx_technicians_available
    ON maintenance.technicians (availability_sequence ASC)
    WHERE state = 'AVAILABLE';
