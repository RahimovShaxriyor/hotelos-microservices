package com.hotelos.identity.dto;

import com.hotelos.identity.domain.AccountType;
import com.hotelos.identity.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class StaffUserDetailResponse {

    @Schema(description = "User unique identifier")
    private UUID id;

    @Schema(description = "Staff username (email)")
    private String username;

    @Schema(description = "Staff full name")
    private String fullName;

    @Schema(description = "Assigned roles")
    private Set<String> roles;

    @Schema(description = "Account status")
    private UserStatus status;

    @Schema(description = "Account type")
    private AccountType accountType;

    @Schema(description = "Failed login attempts counter")
    private int failedLoginAttempts;

    @Schema(description = "Account lock expiration timestamp, if currently locked")
    private Instant lockedUntil;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;

    public StaffUserDetailResponse() {
    }

    public StaffUserDetailResponse(
            UUID id,
            String username,
            String fullName,
            Set<String> roles,
            UserStatus status,
            AccountType accountType,
            int failedLoginAttempts,
            Instant lockedUntil,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.roles = roles;
        this.status = status;
        this.accountType = accountType;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
