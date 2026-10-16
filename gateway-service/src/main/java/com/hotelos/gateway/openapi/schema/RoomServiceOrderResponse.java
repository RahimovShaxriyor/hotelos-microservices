package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(name = "RoomOrder", description = "Room service meal or amenity order")
public record RoomServiceOrderResponse(
        @Schema(description = "Order unique ID", example = "00000000-0000-0000-0000-000000000060")
        String orderId,
        @Schema(description = "Room number destination", example = "301")
        String roomNumber,
        @Schema(description = "List of ordered items")
        List<RoomServiceOrderItemResponse> items,
        @Schema(description = "Order delivery status", example = "RECEIVED", allowableValues = {"RECEIVED", "PREPARING", "DELIVERING", "DELIVERED", "CANCELLED"})
        String status,
        @Schema(description = "Currency code", example = "UZS")
        String currency,
        @Schema(description = "Order total monetary amount", example = "25000.00")
        BigDecimal total,
        @Schema(description = "Order creation timestamp", example = "2026-10-10T12:00:00Z")
        Instant createdAt,
        @Schema(description = "Last status update timestamp", example = "2026-10-10T12:00:00Z")
        Instant updatedAt
) {}
