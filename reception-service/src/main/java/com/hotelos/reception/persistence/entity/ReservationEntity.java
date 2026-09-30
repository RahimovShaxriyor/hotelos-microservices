package com.hotelos.reception.persistence.entity;

import com.hotelos.reception.domain.ReservationStatus;
import com.hotelos.reception.domain.RoomType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reservations", schema = "reception")
public class ReservationEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "guest_id", nullable = false)
    private UUID guestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", length = 32, nullable = false)
    private RoomType roomType;

    @Column(name = "assigned_room_number", length = 16)
    private String assignedRoomNumber;

    @Column(name = "arrival_date", nullable = false)
    private LocalDate arrivalDate;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private ReservationStatus status;

    @Column(name = "nightly_rate_snapshot", precision = 19, scale = 2, nullable = false)
    private BigDecimal nightlyRateSnapshot;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "idempotency_key", length = 128, nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", length = 64, nullable = false, updatable = false, columnDefinition = "CHAR(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String requestFingerprint;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "no_show_at")
    private Instant noShowAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReservationEntity() { }

    public ReservationEntity(UUID id, UUID guestId, RoomType roomType, String assignedRoomNumber,
                             LocalDate arrivalDate, LocalDate departureDate, BigDecimal nightlyRateSnapshot,
                             String currency, String idempotencyKey, String requestFingerprint) {
        this.id = id;
        this.guestId = guestId;
        this.roomType = roomType;
        this.assignedRoomNumber = assignedRoomNumber;
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
        this.status = ReservationStatus.CONFIRMED;
        this.nightlyRateSnapshot = nightlyRateSnapshot;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = ReservationStatus.CONFIRMED;
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getGuestId() { return guestId; }
    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }
    public String getAssignedRoomNumber() { return assignedRoomNumber; }
    public void setAssignedRoomNumber(String assignedRoomNumber) { this.assignedRoomNumber = assignedRoomNumber; }
    public LocalDate getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(LocalDate arrivalDate) { this.arrivalDate = arrivalDate; }
    public LocalDate getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }
    public ReservationStatus getStatus() { return status; }
    public BigDecimal getNightlyRateSnapshot() { return nightlyRateSnapshot; }
    public String getCurrency() { return currency; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getNoShowAt() { return noShowAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void cancel(Instant at) {
        status = ReservationStatus.CANCELLED;
        cancelledAt = at;
        noShowAt = null;
    }

    public void markNoShow(Instant at) {
        status = ReservationStatus.NO_SHOW;
        noShowAt = at;
        cancelledAt = null;
    }
}
