package com.hotelos.reception.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "room_service_charges", schema = "reception")
public class RoomServiceChargeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "order_id", length = 64, nullable = false, unique = true)
    private String orderId;

    @Column(name = "stay_id", nullable = false)
    private UUID stayId;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "source_event_id")
    private UUID sourceEventId;

    @Column(name = "charged_at", nullable = false)
    private Instant chargedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RoomServiceChargeEntity() {
    }

    public RoomServiceChargeEntity(UUID id, String orderId, UUID stayId, BigDecimal amount, UUID sourceEventId, Instant chargedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.orderId = orderId;
        this.stayId = stayId;
        this.amount = amount;
        this.sourceEventId = sourceEventId;
        this.chargedAt = chargedAt != null ? chargedAt : Instant.now();
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public UUID getStayId() { return stayId; }
    public void setStayId(UUID stayId) { this.stayId = stayId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public UUID getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(UUID sourceEventId) { this.sourceEventId = sourceEventId; }

    public Instant getChargedAt() { return chargedAt; }
    public void setChargedAt(Instant chargedAt) { this.chargedAt = chargedAt; }

    public Instant getCreatedAt() { return createdAt; }
}
