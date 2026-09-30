package com.hotelos.reception.dto;

import com.hotelos.reception.domain.RoomType;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(
        RoomType roomType,
        LocalDate arrivalDate,
        LocalDate departureDate,
        int availableInventory,
        List<DailyAvailability> daily
) {
    public record DailyAvailability(
            LocalDate date,
            int totalPhysicalInventory,
            int permanentlyUnsellableInventory,
            int currentStayRooms,
            int reservationsConsumingCapacity,
            int blockedPhysicalRooms,
            int unassignedReservations,
            int availableInventory
    ) { }
}
