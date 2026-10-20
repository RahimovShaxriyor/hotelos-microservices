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

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "UZS";

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

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "occupancy_changed_at", nullable = false)
    private Instant occupancyChangedAt;

    @Column(name = "turnover_changed_at", nullable = false)
    private Instant turnoverChangedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RoomEntity() {
    }

    public RoomEntity(String roomNumber, int floor, RoomType type, String proximity, BigDecimal nightlyRate,
                      OccupancyStatus occupancyStatus, HousekeepingStatus housekeepingStatus, EngineeringStatus engineeringStatus,
                      Instant cleanSince, boolean turnoverPending, String turnoverCorrelationId) {
        this(roomNumber, floor, type, proximity, nightlyRate, "UZS", occupancyStatus, housekeepingStatus, engineeringStatus,
                cleanSince, turnoverPending, turnoverCorrelationId,
                cleanSince != null ? cleanSince : Instant.now(),
                cleanSince != null ? cleanSince : Instant.now(),
                true);
    }

    public RoomEntity(String roomNumber, int floor, RoomType type, String proximity, BigDecimal nightlyRate, String currency,
                      OccupancyStatus occupancyStatus, HousekeepingStatus housekeepingStatus, EngineeringStatus engineeringStatus,
                      Instant cleanSince, boolean turnoverPending, String turnoverCorrelationId,
                      Instant occupancyChangedAt, Instant turnoverChangedAt) {
        this(roomNumber, floor, type, proximity, nightlyRate, currency, occupancyStatus, housekeepingStatus, engineeringStatus,
                cleanSince, turnoverPending, turnoverCorrelationId, occupancyChangedAt, turnoverChangedAt, true);
    }

    public RoomEntity(String roomNumber, int floor, RoomType type, String proximity, BigDecimal nightlyRate, String currency,
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
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.occupancyChangedAt == null) {
            this.occupancyChangedAt = this.createdAt;
        }
        if (this.turnoverChangedAt == null) {
            this.turnoverChangedAt = this.createdAt;
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

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

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

    public Instant getOccupancyChangedAt() { return occupancyChangedAt; }
    public void setOccupancyChangedAt(Instant occupancyChangedAt) { this.occupancyChangedAt = occupancyChangedAt; }

    public Instant getTurnoverChangedAt() { return turnoverChangedAt; }
    public void setTurnoverChangedAt(Instant turnoverChangedAt) { this.turnoverChangedAt = turnoverChangedAt; }

    public Instant getCreatedAt() { return createdAt; }

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

    public void applyHousekeepingStatus(HousekeepingStatus newStatus, Instant changedAt, String eventCorrelationId) {
        Instant effectiveTime = changedAt != null ? changedAt : Instant.now();
        if (this.turnoverPending && this.turnoverCorrelationId != null) {
            // Under turnover: ONLY matching correlationId can transition to CLEAN and clear turnoverPending
            if (newStatus == HousekeepingStatus.CLEAN) {
                if (Objects.equals(this.turnoverCorrelationId, eventCorrelationId)) {
                    this.housekeepingStatus = HousekeepingStatus.CLEAN;
                    this.cleanSince = effectiveTime;
                    this.turnoverPending = false;
                    this.turnoverCorrelationId = null;
                    this.turnoverChangedAt = effectiveTime;
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
                this.cleanSince = effectiveTime;
            } else {
                this.cleanSince = null;
            }
        }
    }

    public void applyEngineeringStatus(EngineeringStatus newStatus) {
        this.engineeringStatus = newStatus;
    }
}
