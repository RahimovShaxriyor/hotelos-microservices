package com.hotelos.reception.persistence.repository;

import com.hotelos.reception.persistence.entity.ReservationEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<ReservationEntity, UUID>,
        JpaSpecificationExecutor<ReservationEntity> {

    Optional<ReservationEntity> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReservationEntity r WHERE r.id = :id")
    Optional<ReservationEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
        SELECT COUNT(r) > 0 FROM ReservationEntity r
        WHERE r.status = com.hotelos.reception.domain.ReservationStatus.CONFIRMED
          AND r.assignedRoomNumber = :roomNumber
          AND r.departureDate >= :hotelToday
    """)
    boolean hasFutureConfirmedReservation(
            @Param("roomNumber") String roomNumber,
            @Param("hotelToday") java.time.LocalDate hotelToday
    );
}
