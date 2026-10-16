package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "OrderItem", description = "Single menu item line in a room service order")
public record RoomServiceOrderItemResponse(
        @Schema(description = "Item name", example = "Espresso")
        String name,
        @Schema(description = "Quantity ordered", example = "2")
        int quantity,
        @Schema(description = "Unit price", example = "4.50")
        BigDecimal unitPrice,
        @Schema(description = "Line total", example = "9.00")
        BigDecimal lineTotal
) {}
