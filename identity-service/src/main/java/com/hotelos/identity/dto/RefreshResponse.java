package com.hotelos.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class RefreshResponse {

    @Schema(description = "Signed RS256 access JWT", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessToken;

    @Schema(description = "Opaque refresh token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;

    @Schema(description = "Authentication token type", example = "Bearer", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tokenType;

    @Schema(description = "Access token validity in seconds", example = "900", requiredMode = Schema.RequiredMode.REQUIRED)
    private long expiresIn;

    @Schema(description = "Remaining refresh session validity in seconds", example = "28800", requiredMode = Schema.RequiredMode.REQUIRED)
    private long refreshExpiresIn;

    public RefreshResponse() {
    }

    public RefreshResponse(String accessToken, String refreshToken, String tokenType, long expiresIn, long refreshExpiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public long getRefreshExpiresIn() {
        return refreshExpiresIn;
    }

    public void setRefreshExpiresIn(long refreshExpiresIn) {
        this.refreshExpiresIn = refreshExpiresIn;
    }
}
