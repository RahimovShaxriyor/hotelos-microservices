package com.hotelos.reception.persistence.repository;

import com.hotelos.reception.persistence.entity.StayRoomHistoryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StayRoomHistoryRepository extends JpaRepository<StayRoomHistoryEntity, UUID> {

    List<StayRoomHistoryEntity> findByStayIdOrderByStartedAtAsc(UUID stayId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM StayRoomHistoryEntity h WHERE h.stayId = :stayId AND h.endedAt IS NULL")
    Optional<StayRoomHistoryEntity> findActiveSegmentForUpdate(@Param("stayId") UUID stayId);

    Optional<StayRoomHistoryEntity> findByStayIdAndEndedAtIsNull(UUID stayId);

    @Query("SELECT h.stayId FROM StayRoomHistoryEntity h WHERE h.roomNumber = :roomNumber AND h.startedAt <= :chargeTime AND (h.endedAt IS NULL OR :chargeTime < h.endedAt)")
    List<UUID> findStayIdsForRoomAtTime(
            @Param("roomNumber") String roomNumber,
            @Param("chargeTime") java.time.Instant chargeTime
    );
}
