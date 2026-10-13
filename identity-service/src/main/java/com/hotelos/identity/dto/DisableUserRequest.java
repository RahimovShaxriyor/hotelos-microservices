package com.hotelos.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class DisableUserRequest {

    @Schema(description = "Optional reason for disabling the account", example = "Staff member offboarding")
    private String reason;

    public DisableUserRequest() {
    }

    public DisableUserRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
