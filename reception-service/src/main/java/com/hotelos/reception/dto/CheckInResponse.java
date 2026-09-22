package com.hotelos.reception.dto;

import com.hotelos.reception.domain.OccupancyStatus;

public record CheckInResponse(String stayId, String guestName, String roomNumber, String roomType, OccupancyStatus occupancyStatus, String message) { }
