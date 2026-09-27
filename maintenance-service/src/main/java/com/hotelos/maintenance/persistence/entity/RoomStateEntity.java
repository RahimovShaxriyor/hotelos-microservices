package com.hotelos.maintenance.persistence.entity;

import com.hotelos.maintenance.domain.EngineeringStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "room_states", schema = "maintenance")
public class RoomStateEntity {

    @Id
    @Column(name = "room_number", length = 16, nullable = false)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "engineering_status", length = 32, nullable = false)
    private EngineeringStatus engineeringStatus;

    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RoomStateEntity() {
    }

    public RoomStateEntity(String roomNumber, EngineeringStatus engineeringStatus, Instant statusChangedAt, Instant updatedAt) {
        this.roomNumber = roomNumber;
        this.engineeringStatus = engineeringStatus;
        this.statusChangedAt = statusChangedAt != null ? statusChangedAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.statusChangedAt;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public EngineeringStatus getEngineeringStatus() {
        return engineeringStatus;
    }

    public void setEngineeringStatus(EngineeringStatus engineeringStatus) {
        this.engineeringStatus = engineeringStatus;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    public void setStatusChangedAt(Instant statusChangedAt) {
        this.statusChangedAt = statusChangedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
