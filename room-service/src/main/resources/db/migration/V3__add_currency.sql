-- ==============================================================================
-- HotelOS Room Service — V3 Migration
-- Schema: room_service
-- Adds Currency to Orders
-- ==============================================================================

-- LEGACY_DEFAULT_CURRENCY — 'UZS'
ALTER TABLE room_service.orders 
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'UZS',
    ADD CONSTRAINT chk_orders_currency CHECK (length(currency) = 3 AND currency = upper(currency));
