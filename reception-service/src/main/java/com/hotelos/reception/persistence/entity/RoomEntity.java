package com.hotelos.reception.persistence.entity;

import com.hotelos.reception.domain.EngineeringStatus;
import com.hotelos.reception.domain.HousekeepingStatus;
import com.hotelos.reception.domain.OccupancyStatus;
import com.hotelos.reception.domain.RoomType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "rooms", schema = "reception")
public class RoomEntity {

    @Id
    @Column(name = "room_number", length = 16, nullable = false)
    private String roomNumber;

    @Column(name = "floor", nullable = false)
    private int floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", length = 32, nullable = false)
    private RoomType type;

    @Column(name = "proximity", length = 32, nullable = false)
    private String proximity;

    @Column(name = "nightly_rate", precision = 19, scale = 2, nullable = false)
    private BigDecimal nightlyRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "occupancy_status", length = 32, nullable = false)
    private OccupancyStatus occupancyStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "housekeeping_status", length = 32, nullable = false)
    private HousekeepingStatus housekeepingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "engineering_status", length = 32, nullable = false)
    private EngineeringStatus engineeringStatus;

    @Column(name = "clean_since")
    private Instant cleanSince;

    @Column(name = "turnover_pending", nullable = false)
    private boolean turnoverPending;

    @Column(name = "turnover_correlation_id", length = 64)
    private String turnoverCorrelationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RoomEntity() {
    }

    public RoomEntity(String roomNumber, int floor, RoomType type, String proximity, BigDecimal nightlyRate,
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
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }

    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }

    public String getProximity() { return proximity; }
    public void setProximity(String proximity) { this.proximity = proximity; }

    public BigDecimal getNightlyRate() { return nightlyRate; }
    public void setNightlyRate(BigDecimal nightlyRate) { this.nightlyRate = nightlyRate; }

    public OccupancyStatus getOccupancyStatus() { return occupancyStatus; }
    public void setOccupancyStatus(OccupancyStatus occupancyStatus) { this.occupancyStatus = occupancyStatus; }

    public HousekeepingStatus getHousekeepingStatus() { return housekeepingStatus; }
    public void setHousekeepingStatus(HousekeepingStatus housekeepingStatus) { this.housekeepingStatus = housekeepingStatus; }

    public EngineeringStatus getEngineeringStatus() { return engineeringStatus; }
    public void setEngineeringStatus(EngineeringStatus engineeringStatus) { this.engineeringStatus = engineeringStatus; }

    public Instant getCleanSince() { return cleanSince; }
    public void setCleanSince(Instant cleanSince) { this.cleanSince = cleanSince; }

    public boolean isTurnoverPending() { return turnoverPending; }
    public void setTurnoverPending(boolean turnoverPending) { this.turnoverPending = turnoverPending; }

    public String getTurnoverCorrelationId() { return turnoverCorrelationId; }
    public void setTurnoverCorrelationId(String turnoverCorrelationId) { this.turnoverCorrelationId = turnoverCorrelationId; }

    public Instant getCreatedAt() { return createdAt; }

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

    public void applyHousekeepingStatus(HousekeepingStatus newStatus, Instant changedAt, String eventCorrelationId) {
        if (this.turnoverPending && this.turnoverCorrelationId != null) {
            // Under turnover: ONLY matching correlationId can transition to CLEAN and clear turnoverPending
            if (newStatus == HousekeepingStatus.CLEAN) {
                if (Objects.equals(this.turnoverCorrelationId, eventCorrelationId)) {
                    this.housekeepingStatus = HousekeepingStatus.CLEAN;
                    this.cleanSince = changedAt;
                    this.turnoverPending = false;
                    this.turnoverCorrelationId = null;
                }
                // Stale CLEAN with mismatched correlationId is completely ignored!
            } else {
                // DIRTY or CLEANING during turnover updates housekeeping status and nulls cleanSince
                this.housekeepingStatus = newStatus;
                this.cleanSince = null;
            }
        } else {
            // Normal housekeeping update
            this.housekeepingStatus = newStatus;
            if (newStatus == HousekeepingStatus.CLEAN) {
                this.cleanSince = changedAt;
            } else {
                this.cleanSince = null;
            }
        }
    }

    public void applyEngineeringStatus(EngineeringStatus newStatus) {
        this.engineeringStatus = newStatus;
    }
}
