-- ==============================================================================
-- HotelOS Reception Service — V3 Migration
-- Schema: reception
-- Adds Guest Profiles and Currency Foundation
-- ==============================================================================

-- 1. Create table reception.guests
CREATE TABLE reception.guests (
    id          UUID PRIMARY KEY,
    first_name  VARCHAR(64) NOT NULL,
    last_name   VARCHAR(64) NOT NULL,
    email       VARCHAR(128),
    phone       VARCHAR(32),
    vip_status  BOOLEAN NOT NULL DEFAULT FALSE,
    notes       VARCHAR(512),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_guests_first_name_not_empty CHECK (length(trim(first_name)) > 0),
    CONSTRAINT chk_guests_last_name_not_empty  CHECK (length(trim(last_name)) > 0),
    CONSTRAINT chk_guests_email_lowercase      CHECK (email IS NULL OR email = lower(trim(email)))
);

CREATE INDEX idx_guests_email ON reception.guests (lower(email)) WHERE email IS NOT NULL;
CREATE INDEX idx_guests_search_name ON reception.guests (lower(last_name), lower(first_name));
CREATE INDEX idx_guests_phone ON reception.guests (phone) WHERE phone IS NOT NULL;

-- 2. Add currency and guest_id columns
-- LEGACY_DEFAULT_CURRENCY — 'UZS'
ALTER TABLE reception.rooms 
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'UZS',
    ADD CONSTRAINT chk_rooms_currency CHECK (length(currency) = 3 AND currency = upper(currency));

ALTER TABLE reception.guest_stays 
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'UZS',
    ADD CONSTRAINT chk_guest_stays_currency CHECK (length(currency) = 3 AND currency = upper(currency)),
    ADD COLUMN guest_id UUID;

ALTER TABLE reception.room_service_charges 
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'UZS',
    ADD CONSTRAINT chk_rs_charges_currency CHECK (length(currency) = 3 AND currency = upper(currency));

-- 3. Safe Backfill: 1 existing Stay -> 1 new Guest Profile (No merging by same name)
DO $$
DECLARE
    rec RECORD;
    new_guest_id UUID;
    parsed_first_name VARCHAR(64);
    parsed_last_name VARCHAR(64);
BEGIN
    FOR rec IN SELECT id, guest_name, created_at FROM reception.guest_stays WHERE guest_id IS NULL LOOP
        new_guest_id := gen_random_uuid();
        
        parsed_first_name := split_part(trim(rec.guest_name), ' ', 1);
        IF parsed_first_name = '' THEN
            parsed_first_name := 'Guest';
        END IF;
        
        parsed_last_name := COALESCE(NULLIF(substring(trim(rec.guest_name) from ' (.*)'), ''), 'Legacy');
        
        INSERT INTO reception.guests (id, first_name, last_name, created_at, updated_at)
        VALUES (new_guest_id, parsed_first_name, parsed_last_name, rec.created_at, rec.created_at);
        
        UPDATE reception.guest_stays
        SET guest_id = new_guest_id
        WHERE id = rec.id;
    END LOOP;
END $$;

-- 4. Enforce NOT NULL and Foreign Key constraints on guest_stays
ALTER TABLE reception.guest_stays 
    ALTER COLUMN guest_id SET NOT NULL,
    ADD CONSTRAINT fk_guest_stays_guest FOREIGN KEY (guest_id) 
        REFERENCES reception.guests(id) ON DELETE RESTRICT;

CREATE INDEX idx_guest_stays_guest_id ON reception.guest_stays (guest_id);
