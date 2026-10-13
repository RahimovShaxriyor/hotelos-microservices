package com.hotelos.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

public class CreateInvitationRequest {

    @Schema(description = "Staff email address", example = "alice@example.com")
    private String email;

    @Schema(description = "Staff full name", example = "Alice Smith")
    private String fullName;

    @Schema(description = "Initial roles to assign upon acceptance", example = "[\"MANAGER\"]")
    private Set<String> roles;

    public CreateInvitationRequest() {
    }

    public CreateInvitationRequest(String email, String fullName, Set<String> roles) {
        this.email = email;
        this.fullName = fullName;
        this.roles = roles;
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
}
