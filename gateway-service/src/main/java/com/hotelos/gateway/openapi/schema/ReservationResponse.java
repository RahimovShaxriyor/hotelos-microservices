package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "ReservationResponse", description = "Hotel room reservation details")
public record ReservationResponse(
        @Schema(description = "Reservation ID", example = "00000000-0000-0000-0000-000000000030")
        UUID id,
        @Schema(description = "Guest profile ID", example = "00000000-0000-0000-0000-000000000020")
        UUID guestId,
        @Schema(description = "Reserved room category", example = "DOUBLE", allowableValues = {"SINGLE", "DOUBLE", "SUITE", "DELUXE"})
        String roomType,
        @Schema(description = "Pre-assigned room number if any", example = "204")
        String assignedRoomNumber,
        @Schema(description = "Arrival date", example = "2026-10-15")
        LocalDate arrivalDate,
        @Schema(description = "Departure date", example = "2026-10-18")
        LocalDate departureDate,
        @Schema(description = "Reservation lifecycle status", example = "CONFIRMED", allowableValues = {"CONFIRMED", "CHECKED_IN", "CANCELLED", "NO_SHOW"})
        String status,
        @Schema(description = "Captured nightly rate snapshot", example = "150000.00")
        BigDecimal nightlyRateSnapshot,
        @Schema(description = "Currency", example = "UZS")
        String currency,
        @Schema(description = "Cancellation timestamp if cancelled", example = "null")
        Instant cancelledAt,
        @Schema(description = "No-show timestamp if marked no-show", example = "null")
        Instant noShowAt,
        @Schema(description = "Check-in timestamp if checked in", example = "null")
        Instant checkedInAt,
        @Schema(description = "Creation timestamp", example = "2026-10-10T09:00:00Z")
        Instant createdAt,
        @Schema(description = "Last update timestamp", example = "2026-10-10T09:00:00Z")
        Instant updatedAt
) {}
