package com.hotelos.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Room service order request sent through the API Gateway")
public record CreateOrderRequest(
        @Schema(example = "301") String roomNumber,
        List<OrderItemRequest> items
) {
}
