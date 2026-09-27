package com.hotelos.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class RefreshRequest {

    @Schema(description = "Opaque refresh token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;

    public RefreshRequest() {
    }

    public RefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
