package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "BillCalculationResponse", description = "Current calculated invoice total for an active room stay")
public record BillCalculationResponse(
        @Schema(description = "Room number", example = "204")
        String roomNumber,
        @Schema(description = "Guest full name", example = "Alice Smith")
        String guestName,
        @Schema(description = "Total reserved room nights", example = "2")
        int bookedNights,
        @Schema(description = "Total room service order charges", example = "25000.00")
        BigDecimal roomServiceCharges,
        @Schema(description = "Total calculated bill amount", example = "325000.00")
        BigDecimal total,
        @Schema(description = "Invoice currency", example = "UZS")
        String currency
) {}
