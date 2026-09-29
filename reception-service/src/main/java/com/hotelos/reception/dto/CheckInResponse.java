package com.hotelos.reception.dto;

import com.hotelos.reception.domain.OccupancyStatus;

public record CheckInResponse(
        String stayId,
        String guestId,
        String guestName,
        String roomNumber,
        String roomType,
        OccupancyStatus occupancyStatus,
        String currency,
        String message
) {
    public CheckInResponse(String stayId, String guestName, String roomNumber, String roomType, OccupancyStatus occupancyStatus, String message) {
        this(stayId, null, guestName, roomNumber, roomType, occupancyStatus, "UZS", message);
    }
}
