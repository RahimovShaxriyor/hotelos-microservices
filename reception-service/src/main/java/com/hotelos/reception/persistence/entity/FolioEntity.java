package com.hotelos.reception.persistence.entity;

import com.hotelos.reception.domain.FolioStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "folios", schema = "reception")
public class FolioEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "stay_id", nullable = false, unique = true)
    private UUID stayId;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private FolioStatus status = FolioStatus.OPEN;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public FolioEntity() {}

    public FolioEntity(UUID id, UUID stayId, String currency, FolioStatus status, Instant closedAt, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.stayId = stayId;
        this.currency = currency;
        this.status = status != null ? status : FolioStatus.OPEN;
        this.closedAt = closedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getStayId() { return stayId; }
    public void setStayId(UUID stayId) { this.stayId = stayId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public FolioStatus getStatus() { return status; }
    public void setStatus(FolioStatus status) { this.status = status; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isOpen() {
        return status == FolioStatus.OPEN;
    }

    public boolean isClosed() {
        return status == FolioStatus.CLOSED;
    }

    public void close(Instant closedAt) {
        this.status = FolioStatus.CLOSED;
        this.closedAt = closedAt;
        this.updatedAt = closedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FolioEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
