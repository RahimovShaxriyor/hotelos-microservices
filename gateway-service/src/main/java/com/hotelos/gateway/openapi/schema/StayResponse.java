package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(name = "StayResponse", description = "Complete projection of a guest stay including room history and billing")
public record StayResponse(
        @Schema(description = "Stay ID", example = "00000000-0000-0000-0000-000000000010")
        UUID id,
        @Schema(description = "Guest profile ID", example = "00000000-0000-0000-0000-000000000020")
        UUID guestId,
        @Schema(description = "Associated reservation ID if created from reservation", example = "00000000-0000-0000-0000-000000000030")
        UUID reservationId,
        @Schema(description = "Currently assigned room number", example = "204")
        String roomNumber,
        @Schema(description = "Guest full name", example = "Alice Smith")
        String guestName,
        @Schema(description = "Currency code", example = "UZS")
        String currency,
        @Schema(description = "Check-in date", example = "2026-10-10")
        LocalDate checkInDate,
        @Schema(description = "Scheduled departure date", example = "2026-10-12")
        LocalDate departureDate,
        @Schema(description = "Booked nights count", example = "2")
        int bookedNights,
        @Schema(description = "Nightly rate fixed at check-in", example = "150000.00")
        BigDecimal nightlyRateAtCheckin,
        @Schema(description = "Room service orders total", example = "25000.00")
        BigDecimal roomServiceCharges,
        @Schema(description = "Total bill balance", example = "325000.00")
        BigDecimal totalBill,
        @Schema(description = "Actual check-in timestamp", example = "2026-10-10T14:30:00Z")
        Instant checkedInAt,
        @Schema(description = "Actual checkout timestamp if checked out", example = "null")
        Instant checkedOutAt,
        @Schema(description = "Whether stay is actively ongoing", example = "true")
        boolean active,
        @Schema(description = "History of room reassignment segments")
        List<RoomHistorySegmentResponse> roomHistory
) {}
