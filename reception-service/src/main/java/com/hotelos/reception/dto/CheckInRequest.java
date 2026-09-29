package com.hotelos.reception.dto;

import java.util.UUID;

public class CheckInRequest {
    private UUID guestId;
    private String guestName;
    private String roomType;
    private int nights;
    private Integer preferredFloor;
    private String proximityPreference;

    public UUID getGuestId() { return guestId; }
    public void setGuestId(UUID guestId) { this.guestId = guestId; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public int getNights() { return nights; }
    public void setNights(int nights) { this.nights = nights; }
    public Integer getPreferredFloor() { return preferredFloor; }
    public void setPreferredFloor(Integer preferredFloor) { this.preferredFloor = preferredFloor; }
    public String getProximityPreference() { return proximityPreference; }
    public void setProximityPreference(String proximityPreference) { this.proximityPreference = proximityPreference; }
}
