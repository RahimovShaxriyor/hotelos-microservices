package com.hotelos.reception.persistence.repository;

import com.hotelos.reception.persistence.entity.GuestStayEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuestStayRepository extends JpaRepository<GuestStayEntity, UUID> {

    Optional<GuestStayEntity> findByRoomNumberAndCheckedOutAtIsNull(String roomNumber);

    List<GuestStayEntity> findByCheckedOutAtIsNullOrderByRoomNumberAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM GuestStayEntity s WHERE s.id = :stayId")
    Optional<GuestStayEntity> findByIdForUpdate(@Param("stayId") UUID stayId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM GuestStayEntity s WHERE s.roomNumber = :roomNumber AND s.checkedOutAt IS NULL")
    Optional<GuestStayEntity> findActiveStayForUpdate(@Param("roomNumber") String roomNumber);

    @Query("SELECT s FROM GuestStayEntity s WHERE s.roomNumber = :roomNumber AND s.checkedInAt <= :chargeTime AND (s.checkedOutAt IS NULL OR s.checkedOutAt > :chargeTime)")
    List<GuestStayEntity> findStaysForRoomAtTime(
            @Param("roomNumber") String roomNumber,
            @Param("chargeTime") Instant chargeTime
    );
}
