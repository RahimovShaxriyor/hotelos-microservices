-- ==============================================================================
-- HotelOS Identity Service — V1 Initial Schema Migration
--
-- Tables:
--   1. identity.users
--   2. identity.roles
--   3. identity.user_roles
--   4. identity.refresh_sessions
--   5. identity.auth_audit_log
--
-- Notes:
--   - Schema 'identity' is pre-created by postgres init script (P1.0 isolation).
--   - Zero CREATE SCHEMA / CREATE ROLE / ALTER ROLE here.
-- ==============================================================================

-- 1. Users table
CREATE TABLE identity.users (
    id UUID PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_identity_users_username UNIQUE (username),
    CONSTRAINT chk_identity_users_username_not_empty CHECK (length(trim(username)) > 0),
    CONSTRAINT chk_identity_users_username_lowercase CHECK (username = lower(username)),
    CONSTRAINT chk_identity_users_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_identity_users_failed_attempts CHECK (failed_login_attempts >= 0)
);

-- 2. Roles table
CREATE TABLE identity.roles (
    role_name VARCHAR(32) PRIMARY KEY,
    description VARCHAR(255) NOT NULL
);

-- Deterministic role seeds
INSERT INTO identity.roles (role_name, description) VALUES
    ('ADMIN', 'System Administrator with full technical and management access'),
    ('MANAGER', 'Hotel Operations Manager with supervisory oversight'),
    ('RECEPTIONIST', 'Front desk staff handling reservations and guest check-in/out'),
    ('HOUSEKEEPING', 'Cleaning and room inspection staff'),
    ('ROOM_SERVICE', 'Kitchen and dining delivery staff'),
    ('MAINTENANCE', 'Engineering and facility maintenance staff');

-- 3. User Roles mapping table
CREATE TABLE identity.user_roles (
    user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    role_name VARCHAR(32) NOT NULL REFERENCES identity.roles(role_name) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_name)
);

-- 4. Refresh Sessions table (persistence model for P2.3)
CREATE TABLE identity.refresh_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    family_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL,
    CONSTRAINT uq_refresh_sessions_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_sessions_family_id ON identity.refresh_sessions(family_id);
CREATE INDEX idx_refresh_sessions_user_id ON identity.refresh_sessions(user_id);

-- 5. Authentication Audit Log table
CREATE TABLE identity.auth_audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(40) NOT NULL,
    user_id UUID NULL,
    username VARCHAR(64) NOT NULL,
    ip_address VARCHAR(45) NULL,
    details TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_auth_audit_log_created_at ON identity.auth_audit_log(created_at);
CREATE INDEX idx_auth_audit_log_username ON identity.auth_audit_log(username);
