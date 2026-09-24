-- ==============================================================================
-- HotelOS — Room Service Persistence Foundation (P1.2)
-- Schema: room_service
-- Flyway Version: V1
-- ==============================================================================

-- 1. room_service.orders
CREATE TABLE room_service.orders (
    id                  UUID PRIMARY KEY,
    room_number         VARCHAR(16) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_changed_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at        TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,

    CONSTRAINT chk_orders_status
        CHECK (status IN ('RECEIVED', 'PREPARING', 'DELIVERING', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT chk_orders_terminal_timestamps
        CHECK (
            (status = 'DELIVERED' AND delivered_at IS NOT NULL AND cancelled_at IS NULL) OR
            (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND delivered_at IS NULL) OR
            (status IN ('RECEIVED', 'PREPARING', 'DELIVERING') AND delivered_at IS NULL AND cancelled_at IS NULL)
        ),
    CONSTRAINT chk_orders_status_changed_order
        CHECK (status_changed_at >= created_at),
    CONSTRAINT chk_orders_room_number
        CHECK (length(trim(room_number)) > 0)
);

-- 2. room_service.order_items
CREATE TABLE room_service.order_items (
    id                  UUID PRIMARY KEY,
    order_id            UUID NOT NULL REFERENCES room_service.orders(id) ON DELETE RESTRICT,
    line_number         INT NOT NULL,
    item_name           VARCHAR(128) NOT NULL,
    quantity            INT NOT NULL,
    unit_price          NUMERIC(19,2) NOT NULL,

    CONSTRAINT chk_order_items_line_number
        CHECK (line_number > 0),
    CONSTRAINT chk_order_items_quantity
        CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price
        CHECK (unit_price > 0),
    CONSTRAINT chk_order_items_name
        CHECK (length(trim(item_name)) > 0),
    CONSTRAINT uq_order_items_line
        UNIQUE (order_id, line_number)
);

-- Explicit indexes aligned with ORDER BY and query lookups
CREATE INDEX idx_orders_room_number ON room_service.orders (room_number);
CREATE INDEX idx_orders_created_at ON room_service.orders (created_at, id);
