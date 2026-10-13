package com.hotelos.identity.persistence.repository;

import com.hotelos.identity.domain.StaffInvitationStatus;
import com.hotelos.identity.persistence.entity.StaffInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffInvitationRepository extends JpaRepository<StaffInvitationEntity, UUID> {

    Optional<StaffInvitationEntity> findByTokenHash(String tokenHash);

    @Query(value = "SELECT normalized_email FROM identity.staff_invitations WHERE id = :id", nativeQuery = true)
    Optional<String> findNormalizedEmailById(@Param("id") UUID id);

    @Query(value = "SELECT normalized_email FROM identity.staff_invitations WHERE token_hash = :tokenHash", nativeQuery = true)
    Optional<String> findNormalizedEmailByTokenHash(@Param("tokenHash") String tokenHash);

    Optional<StaffInvitationEntity> findByNormalizedEmailAndStatus(String normalizedEmail, StaffInvitationStatus status);

    @Query(value = "SELECT pg_advisory_xact_lock(8086100, hashtext(:normalizedEmail))", nativeQuery = true)
    void acquireEmailAdvisoryLock(@Param("normalizedEmail") String normalizedEmail);

    @Query(value = "SELECT * FROM identity.staff_invitations WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<StaffInvitationEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query(value = "SELECT * FROM identity.staff_invitations WHERE token_hash = :tokenHash FOR UPDATE", nativeQuery = true)
    Optional<StaffInvitationEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query(value = """
        UPDATE identity.staff_invitations
        SET status = 'EXPIRED'
        WHERE normalized_email = :normalizedEmail
          AND status = 'ACTIVE'
          AND expires_at <= :now
    """, nativeQuery = true)
    int expireStaleActiveInvitations(@Param("normalizedEmail") String normalizedEmail, @Param("now") Instant now);

    @Modifying
    @Query(value = """
        UPDATE identity.staff_invitations
        SET status = 'INVALIDATED',
            invalidated_at = :now
        WHERE normalized_email = :normalizedEmail
          AND status = 'ACTIVE'
          AND id <> :currentId
    """, nativeQuery = true)
    int invalidateOtherPendingInvitations(
            @Param("normalizedEmail") String normalizedEmail,
            @Param("currentId") UUID currentId,
            @Param("now") Instant now
    );
}
