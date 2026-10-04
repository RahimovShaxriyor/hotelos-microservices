package com.hotelos.roomservice.persistence.repository;

import com.hotelos.roomservice.persistence.entity.StayOrderingSessionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface StayOrderingSessionRepository extends JpaRepository<StayOrderingSessionEntity, UUID> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO room_service.stay_ordering_sessions (stay_id, status, created_at, updated_at)
            VALUES (:stayId, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (stay_id) DO NOTHING
            """, nativeQuery = true)
    void ensureSessionRow(@Param("stayId") UUID stayId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StayOrderingSessionEntity s WHERE s.stayId = :stayId")
    Optional<StayOrderingSessionEntity> findByStayIdForUpdate(@Param("stayId") UUID stayId);

    Optional<StayOrderingSessionEntity> findByFreezeId(UUID freezeId);
}
