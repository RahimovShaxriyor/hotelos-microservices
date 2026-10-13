package com.hotelos.identity.persistence.repository;

import com.hotelos.identity.persistence.entity.StaffInvitationRoleEntity;
import com.hotelos.identity.persistence.entity.StaffInvitationRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface StaffInvitationRoleRepository extends JpaRepository<StaffInvitationRoleEntity, StaffInvitationRoleId> {

    List<StaffInvitationRoleEntity> findByInvitationId(UUID invitationId);

    @Query("SELECT r.roleName FROM StaffInvitationRoleEntity r WHERE r.invitationId = :invitationId")
    Set<String> findRoleNamesByInvitationId(@Param("invitationId") UUID invitationId);

    @Modifying
    @Query("DELETE FROM StaffInvitationRoleEntity r WHERE r.invitationId = :invitationId")
    void deleteByInvitationId(@Param("invitationId") UUID invitationId);
}
