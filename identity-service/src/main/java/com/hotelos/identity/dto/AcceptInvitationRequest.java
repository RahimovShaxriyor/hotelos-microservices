package com.hotelos.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class AcceptInvitationRequest {

    @Schema(description = "Raw invitation token", example = "4uR-...")
    private String token;

    @Schema(description = "Desired password for the new staff account", example = "P@ssword123!")
    private String password;

    public AcceptInvitationRequest() {
    }

    public AcceptInvitationRequest(String token, String password) {
        this.token = token;
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
