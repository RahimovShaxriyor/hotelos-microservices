package com.hotelos.reception.persistence.repository;

import com.hotelos.reception.domain.EngineeringStatus;
import com.hotelos.reception.domain.HousekeepingStatus;
import com.hotelos.reception.persistence.entity.RoomEntity;
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

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, String> {

    List<RoomEntity> findAllByOrderByRoomNumberAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RoomEntity r WHERE r.roomNumber = :roomNumber")
    Optional<RoomEntity> findByRoomNumberForUpdate(@Param("roomNumber") String roomNumber);

    @Query(value = """
        SELECT * FROM reception.rooms
        WHERE occupancy_status = 'VACANT'
          AND housekeeping_status = 'CLEAN'
          AND engineering_status = 'OPERATIONAL'
          AND turnover_pending = FALSE
          AND room_type = :roomType
        ORDER BY
          CASE WHEN :preferredFloor IS NOT NULL AND floor = :preferredFloor THEN 0 ELSE 1 END ASC,
          clean_since ASC,
          CASE WHEN :proximityPreference IS NOT NULL AND UPPER(proximity) = UPPER(:proximityPreference) THEN 0 ELSE 1 END ASC,
          room_number ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<RoomEntity> findAvailableRoomForUpdate(
            @Param("roomType") String roomType,
            @Param("preferredFloor") Integer preferredFloor,
            @Param("proximityPreference") String proximityPreference
    );

    @Modifying
    @Query("UPDATE RoomEntity r SET r.housekeepingStatus = :status, r.cleanSince = :cleanSince WHERE r.roomNumber = :roomNumber")
    int updateHousekeepingStatus(
            @Param("roomNumber") String roomNumber,
            @Param("status") HousekeepingStatus status,
            @Param("cleanSince") Instant cleanSince
    );

    @Modifying
    @Query("UPDATE RoomEntity r SET r.housekeepingStatus = :status, r.cleanSince = :cleanSince, r.turnoverPending = false, r.turnoverCorrelationId = null WHERE r.roomNumber = :roomNumber AND r.turnoverPending = true AND r.turnoverCorrelationId = :correlationId")
    int clearTurnoverPendingIfMatching(
            @Param("roomNumber") String roomNumber,
            @Param("status") HousekeepingStatus status,
            @Param("cleanSince") Instant cleanSince,
            @Param("correlationId") String correlationId
    );

    @Modifying
    @Query("UPDATE RoomEntity r SET r.engineeringStatus = :status WHERE r.roomNumber = :roomNumber")
    int updateEngineeringStatus(
            @Param("roomNumber") String roomNumber,
            @Param("status") EngineeringStatus status
    );
}
