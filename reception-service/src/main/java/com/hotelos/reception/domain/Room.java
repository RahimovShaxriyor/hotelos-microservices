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
    private final String currency;
    private OccupancyStatus occupancyStatus;
    private HousekeepingStatus housekeepingStatus;
    private EngineeringStatus engineeringStatus;
    private Instant cleanSince;
    private boolean turnoverPending;
    private String turnoverCorrelationId;
    private Instant occupancyChangedAt;
    private Instant turnoverChangedAt;
    private boolean active = true;

    public Room(String roomNumber, int floor, RoomType type, String proximity, BigDecimal nightlyRate,
                OccupancyStatus occupancyStatus, HousekeepingStatus housekeepingStatus, EngineeringStatus engineeringStatus,
                Instant cleanSince, boolean turnoverPending) {
        this(roomNumber, floor, type, proximity, nightlyRate, "UZS", occupancyStatus, housekeepingStatus, engineeringStatus, cleanSince, turnoverPending, null);
    }

    public Room(String roomNumber, int floor, RoomType type, String proximity, BigDecimal nightlyRate, String currency,
                OccupancyStatus occupancyStatus, HousekeepingStatus housekeepingStatus, EngineeringStatus engineeringStatus,
                Instant cleanSince, boolean turnoverPending, String turnoverCorrelationId) {
        this(roomNumber, floor, type, proximity, nightlyRate, currency, occupancyStatus, housekeepingStatus, engineeringStatus,
                cleanSince, turnoverPending, turnoverCorrelationId,
                cleanSince != null ? cleanSince : Instant.now(),
                cleanSince != null ? cleanSince : Instant.now(),
                true);
    }

    public Room(String roomNumber, int floor, RoomType type, String proximity, BigDecimal nightlyRate, String currency,
                OccupancyStatus occupancyStatus, HousekeepingStatus housekeepingStatus, EngineeringStatus engineeringStatus,
                Instant cleanSince, boolean turnoverPending, String turnoverCorrelationId,
                Instant occupancyChangedAt, Instant turnoverChangedAt) {
        this(roomNumber, floor, type, proximity, nightlyRate, currency, occupancyStatus, housekeepingStatus, engineeringStatus,
                cleanSince, turnoverPending, turnoverCorrelationId, occupancyChangedAt, turnoverChangedAt, true);
    }

    public Room(String roomNumber, int floor, RoomType type, String proximity, BigDecimal nightlyRate, String currency,
                OccupancyStatus occupancyStatus, HousekeepingStatus housekeepingStatus, EngineeringStatus engineeringStatus,
                Instant cleanSince, boolean turnoverPending, String turnoverCorrelationId,
                Instant occupancyChangedAt, Instant turnoverChangedAt, boolean active) {
        this.roomNumber = roomNumber;
        this.floor = floor;
        this.type = type;
        this.proximity = proximity;
        this.nightlyRate = nightlyRate;
        this.currency = currency != null ? currency : "UZS";
        this.occupancyStatus = occupancyStatus;
        this.housekeepingStatus = housekeepingStatus;
        this.engineeringStatus = engineeringStatus;
        this.cleanSince = cleanSince;
        this.turnoverPending = turnoverPending;
        this.turnoverCorrelationId = turnoverCorrelationId;
        this.occupancyChangedAt = occupancyChangedAt != null ? occupancyChangedAt : Instant.now();
        this.turnoverChangedAt = turnoverChangedAt != null ? turnoverChangedAt : Instant.now();
        this.active = active;
    }

    public static Room fromEntity(RoomEntity entity) {
        if (entity == null) return null;
        return new Room(
                entity.getRoomNumber(),
                entity.getFloor(),
                entity.getType(),
                entity.getProximity(),
                entity.getNightlyRate(),
                entity.getCurrency(),
                entity.getOccupancyStatus(),
                entity.getHousekeepingStatus(),
                entity.getEngineeringStatus(),
                entity.getCleanSince(),
                entity.isTurnoverPending(),
                entity.getTurnoverCorrelationId(),
                entity.getOccupancyChangedAt(),
                entity.getTurnoverChangedAt(),
                entity.isActive()
        );
    }

    public String getRoomNumber() { return roomNumber; }
    public int getFloor() { return floor; }
    public RoomType getType() { return type; }
    public String getProximity() { return proximity; }
    public BigDecimal getNightlyRate() { return nightlyRate; }
    public String getCurrency() { return currency; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public OccupancyStatus getOccupancyStatus() { return occupancyStatus; }
    public HousekeepingStatus getHousekeepingStatus() { return housekeepingStatus; }
    public EngineeringStatus getEngineeringStatus() { return engineeringStatus; }
    public Instant getCleanSince() { return cleanSince; }
    public boolean isTurnoverPending() { return turnoverPending; }
    public String getTurnoverCorrelationId() { return turnoverCorrelationId; }
    public Instant getOccupancyChangedAt() { return occupancyChangedAt; }
    public Instant getTurnoverChangedAt() { return turnoverChangedAt; }

    public boolean isSellable() {
        return active
                && occupancyStatus == OccupancyStatus.VACANT
                && housekeepingStatus == HousekeepingStatus.CLEAN
                && engineeringStatus == EngineeringStatus.OPERATIONAL
                && !turnoverPending;
    }

    public void occupy(Instant changedAt) {
        Instant effectiveTime = changedAt != null ? changedAt : Instant.now();
        this.occupancyStatus = OccupancyStatus.OCCUPIED;
        this.occupancyChangedAt = effectiveTime;
        this.turnoverPending = false;
        this.turnoverCorrelationId = null;
        this.turnoverChangedAt = effectiveTime;
    }

    public void occupy() {
        occupy(Instant.now());
    }

    public void vacate(String correlationId, Instant changedAt) {
        Instant effectiveTime = changedAt != null ? changedAt : Instant.now();
        this.occupancyStatus = OccupancyStatus.VACANT;
        this.occupancyChangedAt = effectiveTime;
        this.turnoverPending = true;
        this.turnoverCorrelationId = correlationId;
        this.turnoverChangedAt = effectiveTime;
    }

    public void vacate(String correlationId) {
        vacate(correlationId, Instant.now());
    }

    public void applyHousekeepingStatus(HousekeepingStatus newStatus, Instant changedAt) {
        applyHousekeepingStatus(newStatus, changedAt, null);
    }

    public void applyHousekeepingStatus(HousekeepingStatus newStatus, Instant changedAt, String eventCorrelationId) {
        Instant effectiveTime = changedAt != null ? changedAt : Instant.now();
        this.housekeepingStatus = newStatus;
        if (newStatus == HousekeepingStatus.CLEAN) {
            this.cleanSince = effectiveTime;
            // turnoverPending is cleared ONLY by a confirmed CLEAN event from the current turnover cycle
            if (this.turnoverPending && this.turnoverCorrelationId != null && Objects.equals(this.turnoverCorrelationId, eventCorrelationId)) {
                this.turnoverPending = false;
                this.turnoverCorrelationId = null;
                this.turnoverChangedAt = effectiveTime;
            }
        }
    }

    public void applyEngineeringStatus(EngineeringStatus newStatus) {
        this.engineeringStatus = newStatus;
        // Maintenance never alters occupancy, housekeeping, or turnoverPending
    }
}
