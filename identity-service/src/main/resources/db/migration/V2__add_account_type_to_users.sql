-- ==============================================================================
-- HotelOS Identity Service — V2 Add Account Type to Users
--
-- P4.0 Foundation:
--   - Adds account_type column to identity.users
--   - Backfills all existing accounts to 'STAFF'
--   - Enforces NOT NULL and CHECK (account_type IN ('STAFF', 'CUSTOMER'))
-- ==============================================================================

ALTER TABLE identity.users ADD COLUMN account_type VARCHAR(20);

UPDATE identity.users
SET account_type = 'STAFF'
WHERE account_type IS NULL;

ALTER TABLE identity.users ALTER COLUMN account_type SET NOT NULL;

ALTER TABLE identity.users
    ADD CONSTRAINT chk_identity_users_account_type
    CHECK (account_type IN ('STAFF', 'CUSTOMER'));
