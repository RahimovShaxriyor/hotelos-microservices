package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WebSocketTicketResponse", description = "One-time ticket for establishing authenticated WebSocket stream")
public record WebSocketTicketResponse(
        @Schema(description = "Single-use signed JWT ticket", example = "ws-ticket-jwt-placeholder")
        String ticket,
        @Schema(description = "Validity duration in seconds", example = "30")
        long expiresIn
) {}
