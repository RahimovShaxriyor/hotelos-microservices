package com.hotelos.identity.persistence.entity;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "staff_invitation_roles", schema = "identity")
@IdClass(StaffInvitationRoleId.class)
public class StaffInvitationRoleEntity {

    @Id
    @Column(name = "invitation_id", nullable = false)
    private UUID invitationId;

    @Id
    @Column(name = "role_name", length = 32, nullable = false)
    private String roleName;

    public StaffInvitationRoleEntity() {
    }

    public StaffInvitationRoleEntity(UUID invitationId, String roleName) {
        this.invitationId = Objects.requireNonNull(invitationId, "invitationId must not be null");
        this.roleName = Objects.requireNonNull(roleName, "roleName must not be null");
    }

    public UUID getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(UUID invitationId) {
        this.invitationId = invitationId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaffInvitationRoleEntity that = (StaffInvitationRoleEntity) o;
        return Objects.equals(invitationId, that.invitationId) && Objects.equals(roleName, that.roleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invitationId, roleName);
    }
}
