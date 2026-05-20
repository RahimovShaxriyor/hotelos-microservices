package com.hotelos.reception.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class GuestStay {
    private final String stayId;
    private final String guestName;
    private final String roomNumber;
    private final LocalDate checkInDate;
    private final int bookedNights;
    private BigDecimal roomServiceCharges = BigDecimal.ZERO;
    private BigDecimal minibarCharge = BigDecimal.ZERO;
    private BigDecimal lateCheckoutFee = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private boolean checkedOut;
    private boolean archived;

    public GuestStay(String guestName, String roomNumber, int bookedNights) {
        this.stayId = UUID.randomUUID().toString();
        this.guestName = guestName;
        this.roomNumber = roomNumber;
        this.bookedNights = bookedNights;
        this.checkInDate = LocalDate.now();
    }

    public String getStayId() { return stayId; }
    public String getGuestName() { return guestName; }
    public String getRoomNumber() { return roomNumber; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public int getBookedNights() { return bookedNights; }
    public BigDecimal getRoomServiceCharges() { return roomServiceCharges; }
    public BigDecimal getMinibarCharge() { return minibarCharge; }
    public BigDecimal getLateCheckoutFee() { return lateCheckoutFee; }
    public BigDecimal getDiscount() { return discount; }
    public boolean isCheckedOut() { return checkedOut; }
    public boolean isArchived() { return archived; }

    public void addRoomServiceCharge(BigDecimal amount) {
        this.roomServiceCharges = this.roomServiceCharges.add(amount);
    }

    public void checkOut() {
        this.checkedOut = true;
    }

    public void archive() {
        this.archived = true;
    }
}
