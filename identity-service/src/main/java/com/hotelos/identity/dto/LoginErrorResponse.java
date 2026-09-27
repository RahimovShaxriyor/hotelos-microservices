package com.hotelos.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class LoginErrorResponse {

    @Schema(description = "HTTP error status", example = "Unauthorized")
    private String error;

    @Schema(description = "Error description", example = "Invalid username or password")
    private String message;

    public LoginErrorResponse() {
    }

    public LoginErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
