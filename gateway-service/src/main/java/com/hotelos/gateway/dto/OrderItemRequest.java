package com.hotelos.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record OrderItemRequest(
        @Schema(example = "Coffee") String name,
        @Schema(example = "2") Integer quantity,
        @Schema(example = "4.50") BigDecimal unitPrice
) {
}
