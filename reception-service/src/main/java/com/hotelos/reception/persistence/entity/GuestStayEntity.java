package com.hotelos.reception.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "guest_stays", schema = "reception")
public class GuestStayEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "room_number", length = 16, nullable = false)
    private String roomNumber;

    @Column(name = "guest_name", length = 128, nullable = false)
    private String guestName;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "booked_nights", nullable = false)
    private int bookedNights;

    @Column(name = "nightly_rate_at_checkin", precision = 19, scale = 2, nullable = false)
    private BigDecimal nightlyRateAtCheckin;

    @Column(name = "minibar_charge", precision = 19, scale = 2, nullable = false)
    private BigDecimal minibarCharge = BigDecimal.ZERO;

    @Column(name = "late_checkout_fee", precision = 19, scale = 2, nullable = false)
    private BigDecimal lateCheckoutFee = BigDecimal.ZERO;

    @Column(name = "discount", precision = 19, scale = 2, nullable = false)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "checked_in_at", nullable = false)
    private Instant checkedInAt;

    @Column(name = "checked_out_at")
    private Instant checkedOutAt;

    @Column(name = "archived", nullable = false)
    private boolean archived = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public GuestStayEntity() {
    }

    public GuestStayEntity(UUID id, String roomNumber, String guestName, LocalDate checkInDate, int bookedNights,
                           BigDecimal nightlyRateAtCheckin, Instant checkedInAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.checkInDate = checkInDate != null ? checkInDate : LocalDate.now();
        this.bookedNights = bookedNights;
        this.nightlyRateAtCheckin = nightlyRateAtCheckin;
        this.checkedInAt = checkedInAt != null ? checkedInAt : Instant.now();
        this.minibarCharge = BigDecimal.ZERO;
        this.lateCheckoutFee = BigDecimal.ZERO;
        this.discount = BigDecimal.ZERO;
        this.archived = false;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.minibarCharge == null) this.minibarCharge = BigDecimal.ZERO;
        if (this.lateCheckoutFee == null) this.lateCheckoutFee = BigDecimal.ZERO;
        if (this.discount == null) this.discount = BigDecimal.ZERO;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }

    public int getBookedNights() { return bookedNights; }
    public void setBookedNights(int bookedNights) { this.bookedNights = bookedNights; }

    public BigDecimal getNightlyRateAtCheckin() { return nightlyRateAtCheckin; }
    public void setNightlyRateAtCheckin(BigDecimal nightlyRateAtCheckin) { this.nightlyRateAtCheckin = nightlyRateAtCheckin; }

    public BigDecimal getMinibarCharge() { return minibarCharge; }
    public void setMinibarCharge(BigDecimal minibarCharge) { this.minibarCharge = minibarCharge; }

    public BigDecimal getLateCheckoutFee() { return lateCheckoutFee; }
    public void setLateCheckoutFee(BigDecimal lateCheckoutFee) { this.lateCheckoutFee = lateCheckoutFee; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public Instant getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(Instant checkedInAt) { this.checkedInAt = checkedInAt; }

    public Instant getCheckedOutAt() { return checkedOutAt; }
    public void setCheckedOutAt(Instant checkedOutAt) { this.checkedOutAt = checkedOutAt; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public Instant getCreatedAt() { return createdAt; }

    public boolean isCheckedOut() {
        return checkedOutAt != null;
    }
}
