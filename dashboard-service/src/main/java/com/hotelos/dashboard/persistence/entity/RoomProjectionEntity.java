package com.hotelos.dashboard.persistence.entity;

import java.time.Instant;

public class RoomProjectionEntity {

    private String roomNumber;
    private String occupancyStatus;
    private String housekeepingStatus;
    private String engineeringStatus;
    private Boolean turnoverPending;
    private String turnoverCorrelationId;
    private Instant occupancyChangedAt;
    private Instant housekeepingChangedAt;
    private Instant engineeringChangedAt;
    private Instant turnoverChangedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public RoomProjectionEntity() {}

    public RoomProjectionEntity(String roomNumber, Instant now) {
        this.roomNumber = roomNumber;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getOccupancyStatus() { return occupancyStatus; }
    public void setOccupancyStatus(String occupancyStatus) { this.occupancyStatus = occupancyStatus; }

    public String getHousekeepingStatus() { return housekeepingStatus; }
    public void setHousekeepingStatus(String housekeepingStatus) { this.housekeepingStatus = housekeepingStatus; }

    public String getEngineeringStatus() { return engineeringStatus; }
    public void setEngineeringStatus(String engineeringStatus) { this.engineeringStatus = engineeringStatus; }

    public Boolean getTurnoverPending() { return turnoverPending; }
    public void setTurnoverPending(Boolean turnoverPending) { this.turnoverPending = turnoverPending; }

    public String getTurnoverCorrelationId() { return turnoverCorrelationId; }
    public void setTurnoverCorrelationId(String turnoverCorrelationId) { this.turnoverCorrelationId = turnoverCorrelationId; }

    public Instant getOccupancyChangedAt() { return occupancyChangedAt; }
    public void setOccupancyChangedAt(Instant occupancyChangedAt) { this.occupancyChangedAt = occupancyChangedAt; }

    public Instant getHousekeepingChangedAt() { return housekeepingChangedAt; }
    public void setHousekeepingChangedAt(Instant housekeepingChangedAt) { this.housekeepingChangedAt = housekeepingChangedAt; }

    public Instant getEngineeringChangedAt() { return engineeringChangedAt; }
    public void setEngineeringChangedAt(Instant engineeringChangedAt) { this.engineeringChangedAt = engineeringChangedAt; }

    public Instant getTurnoverChangedAt() { return turnoverChangedAt; }
    public void setTurnoverChangedAt(Instant turnoverChangedAt) { this.turnoverChangedAt = turnoverChangedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isSellable() {
        return "VACANT".equals(occupancyStatus)
                && "CLEAN".equals(housekeepingStatus)
                && "OPERATIONAL".equals(engineeringStatus)
                && Boolean.FALSE.equals(turnoverPending);
    }
}
