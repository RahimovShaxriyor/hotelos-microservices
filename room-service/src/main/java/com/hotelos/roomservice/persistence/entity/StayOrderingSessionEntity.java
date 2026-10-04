package com.hotelos.roomservice.persistence.entity;

import com.hotelos.roomservice.domain.OrderingSessionStatus;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stay_ordering_sessions", schema = "room_service")
public class StayOrderingSessionEntity implements Persistable<UUID> {

    @Id
    @Column(name = "stay_id", nullable = false, updatable = false)
    private UUID stayId;

    @Transient
    private boolean isNew = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderingSessionStatus status = OrderingSessionStatus.ACTIVE;

    @Column(name = "freeze_id")
    private UUID freezeId;

    @Column(name = "frozen_at")
    private Instant frozenAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public StayOrderingSessionEntity() {
    }

    public StayOrderingSessionEntity(UUID stayId) {
        this.stayId = stayId;
        this.status = OrderingSessionStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.isNew = true;
    }

    @Override
    public UUID getId() {
        return stayId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public UUID getStayId() {
        return stayId;
    }

    public void setStayId(UUID stayId) {
        this.stayId = stayId;
    }

    public OrderingSessionStatus getStatus() {
        return status;
    }

    public void setStatus(OrderingSessionStatus status) {
        this.status = status;
    }

    public UUID getFreezeId() {
        return freezeId;
    }

    public void setFreezeId(UUID freezeId) {
        this.freezeId = freezeId;
    }

    public Instant getFrozenAt() {
        return frozenAt;
    }

    public void setFrozenAt(Instant frozenAt) {
        this.frozenAt = frozenAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void freeze(UUID freezeId, Instant now) {
        this.status = OrderingSessionStatus.FROZEN;
        this.freezeId = freezeId;
        this.frozenAt = now;
        this.updatedAt = now;
    }

    public void close(Instant now) {
        this.status = OrderingSessionStatus.CLOSED;
        this.closedAt = now;
        this.updatedAt = now;
    }

    public void unfreeze(Instant now) {
        this.status = OrderingSessionStatus.ACTIVE;
        this.freezeId = null;
        this.frozenAt = null;
        this.updatedAt = now;
    }
}
