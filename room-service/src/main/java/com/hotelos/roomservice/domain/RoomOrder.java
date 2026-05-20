package com.hotelos.roomservice.domain;

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
        this.orderId = UUID.randomUUID().toString();
        this.roomNumber = roomNumber;
        this.items = items;
        this.status = OrderStatus.RECEIVED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getOrderId() { return orderId; }
    public String getRoomNumber() { return roomNumber; }
    public List<OrderItem> getItems() { return items; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public BigDecimal total() {
        return items.stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void advance() {
        this.status = switch (status) {
            case RECEIVED -> OrderStatus.PREPARING;
            case PREPARING -> OrderStatus.DELIVERING;
            case DELIVERING, DELIVERED -> OrderStatus.DELIVERED;
            case CANCELLED -> OrderStatus.CANCELLED;
        };
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
}
