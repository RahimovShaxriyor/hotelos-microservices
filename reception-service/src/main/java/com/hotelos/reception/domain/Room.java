package com.hotelos.reception.domain;

import com.hotelos.reception.persistence.entity.RoomEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class Room {
    private final String roomNumber;
    private final int floor;
    private final RoomType type;
    private final String proximity;
    private final BigDecimal nightlyRate;
    private OccupancyStatus occupancyStatus;
    private HousekeepingStatus housekeepingStatus;
    private EngineeringStatus engineeringStatus;
    private Instant cleanSince;
    private boolean turnoverPending;
    private String turnoverCorrelationId;

    public Room(String roomNumber, int floor, RoomType type, String proximity, BigDecimal nightlyRate,
                OccupancyStatus occupancyStatus, HousekeepingStatus housekeepingStatus, EngineeringStatus engineeringStatus,
                Instant cleanSince, boolean turnoverPending) {
        this(roomNumber, floor, type, proximity, nightlyRate, occupancyStatus, housekeepingStatus, engineeringStatus, cleanSince, turnoverPending, null);
    }

    public Room(String roomNumber, int floor, RoomType type, String proximity, BigDecimal nightlyRate,
                OccupancyStatus occupancyStatus, HousekeepingStatus housekeepingStatus, EngineeringStatus engineeringStatus,
                Instant cleanSince, boolean turnoverPending, String turnoverCorrelationId) {
        this.roomNumber = roomNumber;
        this.floor = floor;
        this.type = type;
        this.proximity = proximity;
        this.nightlyRate = nightlyRate;
        this.occupancyStatus = occupancyStatus;
        this.housekeepingStatus = housekeepingStatus;
        this.engineeringStatus = engineeringStatus;
        this.cleanSince = cleanSince;
        this.turnoverPending = turnoverPending;
        this.turnoverCorrelationId = turnoverCorrelationId;
    }

    public static Room fromEntity(RoomEntity entity) {
        if (entity == null) return null;
        return new Room(
                entity.getRoomNumber(),
                entity.getFloor(),
                entity.getType(),
                entity.getProximity(),
                entity.getNightlyRate(),
                entity.getOccupancyStatus(),
                entity.getHousekeepingStatus(),
                entity.getEngineeringStatus(),
                entity.getCleanSince(),
                entity.isTurnoverPending(),
                entity.getTurnoverCorrelationId()
        );
    }

    public String getRoomNumber() { return roomNumber; }
    public int getFloor() { return floor; }
    public RoomType getType() { return type; }
    public String getProximity() { return proximity; }
    public BigDecimal getNightlyRate() { return nightlyRate; }
    public OccupancyStatus getOccupancyStatus() { return occupancyStatus; }
    public HousekeepingStatus getHousekeepingStatus() { return housekeepingStatus; }
    public EngineeringStatus getEngineeringStatus() { return engineeringStatus; }
    public Instant getCleanSince() { return cleanSince; }
    public boolean isTurnoverPending() { return turnoverPending; }
    public String getTurnoverCorrelationId() { return turnoverCorrelationId; }

    public boolean isSellable() {
        return occupancyStatus == OccupancyStatus.VACANT
                && housekeepingStatus == HousekeepingStatus.CLEAN
                && engineeringStatus == EngineeringStatus.OPERATIONAL
                && !turnoverPending;
    }

    public void occupy() {
        this.occupancyStatus = OccupancyStatus.OCCUPIED;
        this.turnoverPending = false;
        this.turnoverCorrelationId = null;
    }

    public void vacate(String correlationId) {
        this.occupancyStatus = OccupancyStatus.VACANT;
        this.turnoverPending = true;
        this.turnoverCorrelationId = correlationId;
    }

    public void applyHousekeepingStatus(HousekeepingStatus newStatus, Instant changedAt) {
        applyHousekeepingStatus(newStatus, changedAt, null);
    }

    public void applyHousekeepingStatus(HousekeepingStatus newStatus, Instant changedAt, String eventCorrelationId) {
        this.housekeepingStatus = newStatus;
        if (newStatus == HousekeepingStatus.CLEAN) {
            this.cleanSince = changedAt;
            // turnoverPending is cleared ONLY by a confirmed CLEAN event from the current turnover cycle
            if (this.turnoverPending && this.turnoverCorrelationId != null && Objects.equals(this.turnoverCorrelationId, eventCorrelationId)) {
                this.turnoverPending = false;
                this.turnoverCorrelationId = null;
            }
        }
    }

    public void applyEngineeringStatus(EngineeringStatus newStatus) {
        this.engineeringStatus = newStatus;
        // Maintenance never alters occupancy, housekeeping, or turnoverPending
    }
}
