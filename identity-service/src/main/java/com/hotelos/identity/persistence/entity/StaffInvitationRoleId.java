package com.hotelos.identity.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class StaffInvitationRoleId implements Serializable {

    private UUID invitationId;
    private String roleName;

    public StaffInvitationRoleId() {
    }

    public StaffInvitationRoleId(UUID invitationId, String roleName) {
        this.invitationId = invitationId;
        this.roleName = roleName;
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
        StaffInvitationRoleId that = (StaffInvitationRoleId) o;
        return Objects.equals(invitationId, that.invitationId) && Objects.equals(roleName, that.roleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invitationId, roleName);
    }
}
