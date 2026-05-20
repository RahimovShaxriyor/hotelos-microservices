package com.hotelos.reception.dto;

public record CheckInResponse(String stayId, String guestName, String roomNumber, String roomType, String status, String message) { }
