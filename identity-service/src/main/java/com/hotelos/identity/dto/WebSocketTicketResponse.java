package com.hotelos.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Short-lived single-use WebSocket authentication ticket")
public record WebSocketTicketResponse(
        @Schema(description = "One-time signed RS256 ticket JWT")
        String ticket,
        @Schema(description = "Validity in seconds", example = "30")
        long expiresIn
) {
}
