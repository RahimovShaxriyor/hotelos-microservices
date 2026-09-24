package com.hotelos.housekeeping.persistence.entity;

import com.hotelos.housekeeping.domain.HousekeepingStatus;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

@Entity
@Table(name = "room_states", schema = "housekeeping")
public class RoomStateEntity implements Persistable<String> {

    @Id
    @Column(name = "room_number", length = 16, nullable = false)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "housekeeping_status", length = 16, nullable = false)
    private HousekeepingStatus housekeepingStatus;

    @Column(name = "active_turnover_correlation_id", length = 64)
    private String activeTurnoverCorrelationId;

    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt;

    @Column(name = "clean_since")
    private Instant cleanSince;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    protected RoomStateEntity() {
    }

    public RoomStateEntity(String roomNumber, HousekeepingStatus housekeepingStatus, Instant now) {
        this.roomNumber = roomNumber;
        this.housekeepingStatus = housekeepingStatus;
        this.statusChangedAt = now;
        this.cleanSince = housekeepingStatus == HousekeepingStatus.CLEAN ? now : null;
        this.createdAt = now;
        this.isNew = true;
    }

    @Override
    public String getId() {
        return roomNumber;
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

    public String getRoomNumber() {
        return roomNumber;
    }

    public HousekeepingStatus getHousekeepingStatus() {
        return housekeepingStatus;
    }

    public void setHousekeepingStatus(HousekeepingStatus housekeepingStatus) {
        this.housekeepingStatus = housekeepingStatus;
    }

    public String getActiveTurnoverCorrelationId() {
        return activeTurnoverCorrelationId;
    }

    public void setActiveTurnoverCorrelationId(String activeTurnoverCorrelationId) {
        this.activeTurnoverCorrelationId = activeTurnoverCorrelationId;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    public void setStatusChangedAt(Instant statusChangedAt) {
        this.statusChangedAt = statusChangedAt;
    }

    public Instant getCleanSince() {
        return cleanSince;
    }

    public void setCleanSince(Instant cleanSince) {
        this.cleanSince = cleanSince;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
