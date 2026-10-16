package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(name = "DailyAvailabilityResponse", description = "Detailed capacity metrics for a specific calendar date")
public record DailyAvailabilityResponse(
        @Schema(description = "Date of availability metric", example = "2026-10-15")
        LocalDate date,
        @Schema(description = "Total physical rooms of the category", example = "10")
        int totalPhysicalInventory,
        @Schema(description = "Out-of-order or unsellable rooms", example = "1")
        int permanentlyUnsellableInventory,
        @Schema(description = "Rooms occupied by active stays", example = "4")
        int currentStayRooms,
        @Schema(description = "Confirmed reservations consuming capacity", example = "3")
        int reservationsConsumingCapacity,
        @Schema(description = "Rooms blocked by pre-assignment", example = "2")
        int blockedPhysicalRooms,
        @Schema(description = "Unassigned reservations", example = "1")
        int unassignedReservations,
        @Schema(description = "Net available inventory sellable", example = "2")
        int availableInventory
) {}
