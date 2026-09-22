package com.hotelos.reception.persistence.repository;

import com.hotelos.reception.persistence.entity.RoomServiceChargeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomServiceChargeRepository extends JpaRepository<RoomServiceChargeEntity, UUID> {

    Optional<RoomServiceChargeEntity> findByOrderId(String orderId);

    List<RoomServiceChargeEntity> findByStayId(UUID stayId);

    @Modifying
    @Query(value = """
        INSERT INTO reception.room_service_charges (id, order_id, stay_id, amount, source_event_id, charged_at, created_at)
        VALUES (:id, :orderId, :stayId, :amount, :sourceEventId, :chargedAt, CURRENT_TIMESTAMP)
        ON CONFLICT (order_id) DO NOTHING
        """, nativeQuery = true)
    int insertChargeOnConflictDoNothing(
            @Param("id") UUID id,
            @Param("orderId") String orderId,
            @Param("stayId") UUID stayId,
            @Param("amount") BigDecimal amount,
            @Param("sourceEventId") UUID sourceEventId,
            @Param("chargedAt") Instant chargedAt
    );

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM RoomServiceChargeEntity c WHERE c.stayId = :stayId")
    BigDecimal sumChargesByStayId(@Param("stayId") UUID stayId);
}
