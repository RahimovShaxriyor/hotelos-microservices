package com.hotelos.roomservice.domain;

import com.hotelos.roomservice.exception.HotelValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RoomOrder {
    private final String orderId;
    private final String roomNumber;
    private final List<OrderItem> items;
    private OrderStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public RoomOrder(String roomNumber, List<OrderItem> items) {
        this(UUID.randomUUID().toString(), roomNumber, items, OrderStatus.RECEIVED, Instant.now(), Instant.now());
    }

    public RoomOrder(String orderId, String roomNumber, List<OrderItem> items, OrderStatus status, Instant createdAt, Instant updatedAt) {
        this.orderId = orderId;
        this.roomNumber = roomNumber;
        this.items = items;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getOrderId() { return orderId; }
    public String getRoomNumber() { return roomNumber; }
    public List<OrderItem> getItems() { return items; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public BigDecimal total() {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void advance() {
        if (status == OrderStatus.DELIVERED) {
            throw new HotelValidationException("Delivered orders cannot be advanced");
        }
        if (status == OrderStatus.CANCELLED) {
            throw new HotelValidationException("Cancelled orders cannot be advanced");
        }
        this.status = switch (status) {
            case RECEIVED -> OrderStatus.PREPARING;
            case PREPARING -> OrderStatus.DELIVERING;
            case DELIVERING -> OrderStatus.DELIVERED;
            default -> throw new HotelValidationException("Unsupported order transition from " + status);
        };
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (status == OrderStatus.DELIVERED) {
            throw new HotelValidationException("Delivered orders cannot be cancelled");
        }
        if (status == OrderStatus.CANCELLED) {
            throw new HotelValidationException("Order is already cancelled");
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
}
