package com.hotelos.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class InvitationCreatedResponse {

    @Schema(description = "Invitation unique identifier")
    private UUID id;

    @Schema(description = "Normalized recipient email")
    private String email;

    @Schema(description = "Staff recipient full name")
    private String fullName;

    @Schema(description = "Assigned staff roles")
    private Set<String> roles;

    @Schema(description = "Invitation expiration timestamp")
    private Instant expiresAt;

    @Schema(description = "Single-exposure raw invitation token (must be communicated to recipient out of band)")
    private String token;

    public InvitationCreatedResponse() {
    }

    public InvitationCreatedResponse(UUID id, String email, String fullName, Set<String> roles, Instant expiresAt, String token) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.roles = roles;
        this.expiresAt = expiresAt;
        this.token = token;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
