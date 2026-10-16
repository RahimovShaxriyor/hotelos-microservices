package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "GuestStay", description = "Active or historical stay of a guest in a room")
public record GuestStayResponse(
        @Schema(description = "Stay unique identifier", example = "00000000-0000-0000-0000-000000000010")
        String stayId,
        @Schema(description = "Guest profile ID", example = "00000000-0000-0000-0000-000000000020")
        String guestId,
        @Schema(description = "Primary guest full name", example = "Alice Smith")
        String guestName,
        @Schema(description = "Assigned room number", example = "204")
        String roomNumber,
        @Schema(description = "Billing currency code", example = "UZS")
        String currency,
        @Schema(description = "Check-in calendar date", example = "2026-10-10")
        LocalDate checkInDate,
        @Schema(description = "Total reserved nights", example = "2")
        int bookedNights,
        @Schema(description = "Accumulated room service charge total", example = "25000.00")
        BigDecimal roomServiceCharges,
        @Schema(description = "Minibar consumption charges", example = "0.00")
        BigDecimal minibarCharge,
        @Schema(description = "Late checkout fee applied", example = "0.00")
        BigDecimal lateCheckoutFee,
        @Schema(description = "Discounts applied", example = "0.00")
        BigDecimal discount,
        @Schema(description = "Whether guest has checked out", example = "false")
        boolean checkedOut,
        @Schema(description = "Whether record has been archived", example = "false")
        boolean archived
) {}
