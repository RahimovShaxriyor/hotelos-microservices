package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RefreshResponse", description = "New JWT access token and rotated refresh token")
public record RefreshResponse(
        @Schema(description = "Newly issued RS256 JWT access token", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.placeholder")
        String accessToken,
        @Schema(description = "Rotated opaque refresh token", example = "new-refresh-tok-placeholder")
        String refreshToken,
        @Schema(description = "Token type prefix", example = "Bearer")
        String tokenType,
        @Schema(description = "Access token lifespan in seconds", example = "900")
        long expiresIn,
        @Schema(description = "Remaining refresh session lifespan in seconds", example = "28800")
        long refreshExpiresIn
) {}
