package com.hotelos.housekeeping.persistence.repository;

import com.hotelos.housekeeping.persistence.entity.RoomStateEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RoomStateRepository extends JpaRepository<RoomStateEntity, String> {

    Optional<RoomStateEntity> findByRoomNumber(String roomNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RoomStateEntity r WHERE r.roomNumber = :roomNumber")
    Optional<RoomStateEntity> findByRoomNumberForUpdate(@Param("roomNumber") String roomNumber);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "INSERT INTO housekeeping.room_states (room_number, housekeeping_status, status_changed_at, clean_since, created_at) " +
            "VALUES (:roomNumber, 'CLEAN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (room_number) DO NOTHING", nativeQuery = true)
    void insertIfAbsent(@Param("roomNumber") String roomNumber);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE RoomStateEntity r SET r.housekeepingStatus = com.hotelos.housekeeping.domain.HousekeepingStatus.CLEAN, " +
            "r.cleanSince = :now, r.statusChangedAt = :now, r.activeTurnoverCorrelationId = null")
    void resetAllToClean(@Param("now") Instant now);
}
