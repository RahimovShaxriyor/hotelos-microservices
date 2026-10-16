package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(name = "RoomServiceChargesResponse", description = "Summary of delivered room service orders and total charges for a room")
public record RoomServiceChargesResponse(
        @Schema(description = "Room number", example = "301")
        String roomNumber,
        @Schema(description = "Sum of all delivered order charges", example = "25000.00")
        BigDecimal deliveredRoomServiceCharges,
        @Schema(description = "Historical room service orders for room")
        List<RoomServiceOrderResponse> orders
) {}
