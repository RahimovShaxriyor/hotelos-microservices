-- ==============================================================================
-- HotelOS Identity Service — V3 Staff Invitations and Owner Governance Schema
--
-- Tables & Changes:
--   1. Seed OWNER role in identity.roles
--   2. Widen identity.users.username to VARCHAR(254)
--   3. Create identity.staff_invitations table with explicit status and timestamp constraints
--   4. Create identity.staff_invitation_roles table with CHECK (role_name <> 'OWNER')
--   5. Partial unique active index on identity.staff_invitations (normalized_email) WHERE status = 'ACTIVE'
--   6. Supporting indexes
--   7. Additive governance audit columns to identity.auth_audit_log
-- ==============================================================================

-- 1. Seed OWNER role
INSERT INTO identity.roles (role_name, description)
VALUES ('OWNER', 'Hotel Owner with administrative sovereignty and governance oversight')
ON CONFLICT (role_name) DO NOTHING;

-- 2. Widen identity.users.username
ALTER TABLE identity.users ALTER COLUMN username TYPE VARCHAR(254);

-- 3. Create identity.staff_invitations
CREATE TABLE identity.staff_invitations (
    id UUID PRIMARY KEY,
    normalized_email VARCHAR(254) NOT NULL,
    full_name VARCHAR(128) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    invited_by UUID NOT NULL REFERENCES identity.users(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ NULL,
    invalidated_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_staff_invitations_email_not_empty CHECK (length(trim(normalized_email)) > 0),
    CONSTRAINT chk_staff_invitations_email_lowercase CHECK (normalized_email = lower(normalized_email)),
    CONSTRAINT chk_staff_invitations_status CHECK (status IN ('ACTIVE', 'ACCEPTED', 'EXPIRED', 'INVALIDATED')),
    CONSTRAINT chk_staff_invitations_status_timestamps CHECK (
        (status = 'ACTIVE' AND accepted_at IS NULL AND invalidated_at IS NULL) OR
        (status = 'EXPIRED' AND accepted_at IS NULL AND invalidated_at IS NULL) OR
        (status = 'ACCEPTED' AND accepted_at IS NOT NULL AND invalidated_at IS NULL) OR
        (status = 'INVALIDATED' AND invalidated_at IS NOT NULL AND accepted_at IS NULL)
    )
);

-- 4. Create identity.staff_invitation_roles
CREATE TABLE identity.staff_invitation_roles (
    invitation_id UUID NOT NULL REFERENCES identity.staff_invitations(id) ON DELETE CASCADE,
    role_name VARCHAR(32) NOT NULL REFERENCES identity.roles(role_name) ON DELETE RESTRICT,
    PRIMARY KEY (invitation_id, role_name),
    CONSTRAINT chk_staff_invitation_roles_no_owner CHECK (role_name <> 'OWNER')
);

-- 5. Partial unique active invitation index (ensures only one active invitation per email)
CREATE UNIQUE INDEX uq_staff_invitations_active_email
ON identity.staff_invitations (normalized_email)
WHERE status = 'ACTIVE';

-- 6. Supporting indexes
CREATE INDEX idx_staff_invitations_status ON identity.staff_invitations(status);
CREATE INDEX idx_staff_invitations_invited_by ON identity.staff_invitations(invited_by);
CREATE INDEX idx_staff_invitations_expires_at ON identity.staff_invitations(expires_at);

-- 7. Extend identity.auth_audit_log minimally
ALTER TABLE identity.auth_audit_log ADD COLUMN actor_user_id UUID NULL;
ALTER TABLE identity.auth_audit_log ADD COLUMN target_user_id UUID NULL;
ALTER TABLE identity.auth_audit_log ADD COLUMN target_email VARCHAR(254) NULL;
ALTER TABLE identity.auth_audit_log ADD COLUMN correlation_id VARCHAR(64) NULL;
ALTER TABLE identity.auth_audit_log ALTER COLUMN username TYPE VARCHAR(254);
ALTER TABLE identity.auth_audit_log ALTER COLUMN username DROP NOT NULL;

CREATE INDEX idx_auth_audit_log_actor_user_id ON identity.auth_audit_log(actor_user_id);
CREATE INDEX idx_auth_audit_log_target_user_id ON identity.auth_audit_log(target_user_id);
CREATE INDEX idx_auth_audit_log_target_email ON identity.auth_audit_log(target_email);
