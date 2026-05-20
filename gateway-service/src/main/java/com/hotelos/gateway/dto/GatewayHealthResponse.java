package com.hotelos.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Gateway health response")
public record GatewayHealthResponse(
        String status,
        String gateway,
        String swagger
) {
}
