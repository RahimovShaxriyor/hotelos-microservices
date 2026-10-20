-- ==============================================================================
-- HotelOS — Reception Service (P4.1.2)
-- Schema: reception
-- Flyway Version: V8
-- Description: Add active lifecycle column to reception.rooms
-- ==============================================================================

ALTER TABLE reception.rooms
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
