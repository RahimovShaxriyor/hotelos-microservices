package com.hotelos.reception.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stay_room_history", schema = "reception")
public class StayRoomHistoryEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "stay_id", nullable = false)
    private UUID stayId;

    @Column(name = "room_number", length = 16, nullable = false)
    private String roomNumber;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StayRoomHistoryEntity() {
    }

    public StayRoomHistoryEntity(UUID id, UUID stayId, String roomNumber, Instant startedAt,
                                 Instant endedAt, String reason, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.stayId = stayId;
        this.roomNumber = roomNumber;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.reason = reason;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getStayId() {
        return stayId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
