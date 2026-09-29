package com.hotelos.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Room service order request sent through the API Gateway")
public record CreateOrderRequest(
        @Schema(example = "301") String roomNumber,
        @Schema(example = "UZS") String currency,
        List<OrderItemRequest> items
) {
    public CreateOrderRequest(String roomNumber, List<OrderItemRequest> items) {
        this(roomNumber, "UZS", items);
    }
}
