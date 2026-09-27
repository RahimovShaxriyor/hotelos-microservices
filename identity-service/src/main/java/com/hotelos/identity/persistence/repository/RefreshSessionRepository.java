package com.hotelos.identity.persistence.repository;

import com.hotelos.identity.persistence.entity.RefreshSessionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshSessionRepository extends JpaRepository<RefreshSessionEntity, UUID> {

    Optional<RefreshSessionEntity> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM RefreshSessionEntity s WHERE s.tokenHash = :tokenHash")
    Optional<RefreshSessionEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    List<RefreshSessionEntity> findByFamilyId(UUID familyId);

    List<RefreshSessionEntity> findByUserId(UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshSessionEntity s SET s.revokedAt = :now WHERE s.familyId = :familyId AND s.revokedAt IS NULL")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshSessionEntity s SET s.revokedAt = :now WHERE s.userId = :userId AND s.revokedAt IS NULL")
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}

