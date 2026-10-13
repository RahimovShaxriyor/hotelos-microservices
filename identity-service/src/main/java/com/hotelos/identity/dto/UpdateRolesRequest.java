package com.hotelos.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

public class UpdateRolesRequest {

    @Schema(description = "Replacement set of roles for the staff account", example = "[\"MANAGER\", \"RECEPTIONIST\"]")
    private Set<String> roles;

    public UpdateRolesRequest() {
    }

    public UpdateRolesRequest(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
