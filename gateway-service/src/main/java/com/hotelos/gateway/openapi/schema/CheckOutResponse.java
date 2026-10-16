package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "CheckOutResponse", description = "Final settlement summary returned after checking out a guest")
public record CheckOutResponse(
        @Schema(description = "Vacated room number", example = "204")
        String roomNumber,
        @Schema(description = "Guest full name", example = "Alice Smith")
        String guestName,
        @Schema(description = "Final billing balance total", example = "325000.00")
        BigDecimal total,
        @Schema(description = "Settlement currency code", example = "UZS")
        String currency,
        @Schema(description = "Confirmation message", example = "Check-out completed")
        String message
) {}
