package com.hotelos.maintenance.persistence.repository;

import com.hotelos.maintenance.persistence.entity.RoomStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RoomStateRepository extends JpaRepository<RoomStateEntity, String> {

    @Modifying
    @Query(value = "INSERT INTO maintenance.room_states (room_number, engineering_status, status_changed_at, updated_at) " +
            "VALUES (:roomNumber, 'OPERATIONAL', :now, :now) ON CONFLICT (room_number) DO NOTHING", nativeQuery = true)
    void insertIfAbsent(@Param("roomNumber") String roomNumber, @Param("now") Instant now);

    @Query(value = "SELECT * FROM maintenance.room_states WHERE room_number = :roomNumber FOR UPDATE", nativeQuery = true)
    Optional<RoomStateEntity> lockRoomState(@Param("roomNumber") String roomNumber);
}
