package com.hotelos.identity.persistence.entity;

import com.hotelos.identity.domain.StaffInvitationStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "staff_invitations", schema = "identity")
public class StaffInvitationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "normalized_email", length = 254, nullable = false)
    private String normalizedEmail;

    @Column(name = "full_name", length = 128, nullable = false)
    private String fullName;

    @Column(name = "token_hash", length = 64, nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private StaffInvitationStatus status = StaffInvitationStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public StaffInvitationEntity() {
    }

    public StaffInvitationEntity(
            UUID id,
            String normalizedEmail,
            String fullName,
            String tokenHash,
            UUID invitedBy,
            Instant expiresAt,
            Instant createdAt
    ) {
        this(id, normalizedEmail, fullName, tokenHash, invitedBy, StaffInvitationStatus.ACTIVE, expiresAt, createdAt);
    }

    public StaffInvitationEntity(
            UUID id,
            String normalizedEmail,
            String fullName,
            String tokenHash,
            UUID invitedBy,
            StaffInvitationStatus status,
            Instant expiresAt,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.normalizedEmail = Objects.requireNonNull(normalizedEmail, "normalizedEmail must not be null");
        this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        this.invitedBy = Objects.requireNonNull(invitedBy, "invitedBy must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNormalizedEmail() {
        return normalizedEmail;
    }

    public void setNormalizedEmail(String normalizedEmail) {
        this.normalizedEmail = normalizedEmail;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(UUID invitedBy) {
        this.invitedBy = invitedBy;
    }

    public StaffInvitationStatus getStatus() {
        return status;
    }

    public void setStatus(StaffInvitationStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Instant getInvalidatedAt() {
        return invalidatedAt;
    }

    public void setInvalidatedAt(Instant invalidatedAt) {
        this.invalidatedAt = invalidatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive(Instant now) {
        return status == StaffInvitationStatus.ACTIVE && acceptedAt == null && invalidatedAt == null && expiresAt.isAfter(now);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaffInvitationEntity that = (StaffInvitationEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
