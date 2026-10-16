package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginResponse", description = "Authentication tokens issued after successful staff login")
public record LoginResponse(
        @Schema(description = "Signed RS256 JWT access token", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.placeholder")
        String accessToken,
        @Schema(description = "Opaque refresh token for session rotation", example = "refresh-tok-placeholder")
        String refreshToken,
        @Schema(description = "Token type prefix", example = "Bearer")
        String tokenType,
        @Schema(description = "Access token lifespan in seconds", example = "900")
        long expiresIn,
        @Schema(description = "Refresh session lifespan in seconds", example = "28800")
        long refreshExpiresIn
) {}
