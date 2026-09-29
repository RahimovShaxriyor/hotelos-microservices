package com.hotelos.reception.domain;

import com.hotelos.reception.persistence.entity.GuestStayEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GuestStay {
    private final String stayId;
    private final String guestId;
    private final String guestName;
    private final String roomNumber;
    private final String currency;
    private final LocalDate checkInDate;
    private final int bookedNights;
    private final BigDecimal roomServiceCharges;
    private final BigDecimal minibarCharge;
    private final BigDecimal lateCheckoutFee;
    private final BigDecimal discount;
    private final boolean checkedOut;
    private final boolean archived;

    public GuestStay(String stayId, String guestId, String guestName, String roomNumber, String currency, LocalDate checkInDate, int bookedNights,
                     BigDecimal roomServiceCharges, BigDecimal minibarCharge, BigDecimal lateCheckoutFee,
                     BigDecimal discount, boolean checkedOut, boolean archived) {
        this.stayId = stayId;
        this.guestId = guestId;
        this.guestName = guestName;
        this.roomNumber = roomNumber;
        this.currency = currency != null ? currency : "UZS";
        this.checkInDate = checkInDate;
        this.bookedNights = bookedNights;
        this.roomServiceCharges = roomServiceCharges != null ? roomServiceCharges : BigDecimal.ZERO;
        this.minibarCharge = minibarCharge != null ? minibarCharge : BigDecimal.ZERO;
        this.lateCheckoutFee = lateCheckoutFee != null ? lateCheckoutFee : BigDecimal.ZERO;
        this.discount = discount != null ? discount : BigDecimal.ZERO;
        this.checkedOut = checkedOut;
        this.archived = archived;
    }

    public static GuestStay fromEntity(GuestStayEntity entity, BigDecimal roomServiceCharges) {
        if (entity == null) return null;
        return new GuestStay(
                entity.getId().toString(),
                entity.getGuestId() != null ? entity.getGuestId().toString() : null,
                entity.getGuestName(),
                entity.getRoomNumber(),
                entity.getCurrency(),
                entity.getCheckInDate(),
                entity.getBookedNights(),
                roomServiceCharges != null ? roomServiceCharges : BigDecimal.ZERO,
                entity.getMinibarCharge(),
                entity.getLateCheckoutFee(),
                entity.getDiscount(),
                entity.isCheckedOut(),
                entity.isArchived()
        );
    }

    public String getStayId() { return stayId; }
    public String getGuestId() { return guestId; }
    public String getGuestName() { return guestName; }
    public String getRoomNumber() { return roomNumber; }
    public String getCurrency() { return currency; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public int getBookedNights() { return bookedNights; }
    public BigDecimal getRoomServiceCharges() { return roomServiceCharges; }
    public BigDecimal getMinibarCharge() { return minibarCharge; }
    public BigDecimal getLateCheckoutFee() { return lateCheckoutFee; }
    public BigDecimal getDiscount() { return discount; }
    public boolean isCheckedOut() { return checkedOut; }
    public boolean isArchived() { return archived; }
}
