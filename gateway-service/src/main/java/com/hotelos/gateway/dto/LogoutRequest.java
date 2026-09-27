package com.hotelos.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class LogoutRequest {

    @Schema(description = "Opaque refresh token to revoke", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;

    public LogoutRequest() {
    }

    public LogoutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
