package com.hotelos.roomservice.persistence.repository;

import com.hotelos.roomservice.persistence.entity.RoomServiceOrderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<RoomServiceOrderEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM RoomServiceOrderEntity o WHERE o.id = :id")
    Optional<RoomServiceOrderEntity> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT o FROM RoomServiceOrderEntity o WHERE o.id = :id")
    Optional<RoomServiceOrderEntity> findByIdWithItems(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT o FROM RoomServiceOrderEntity o ORDER BY o.createdAt ASC, o.id ASC")
    List<RoomServiceOrderEntity> findAllWithItems();

    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT o FROM RoomServiceOrderEntity o WHERE o.roomNumber = :roomNumber ORDER BY o.createdAt ASC, o.id ASC")
    List<RoomServiceOrderEntity> findByRoomNumberWithItems(@Param("roomNumber") String roomNumber);

    @Query("SELECT o FROM RoomServiceOrderEntity o WHERE o.stayId = :stayId AND o.status IN (com.hotelos.roomservice.domain.OrderStatus.RECEIVED, com.hotelos.roomservice.domain.OrderStatus.PREPARING, com.hotelos.roomservice.domain.OrderStatus.DELIVERING)")
    List<RoomServiceOrderEntity> findPendingOrdersByStayId(@Param("stayId") UUID stayId);

    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT o FROM RoomServiceOrderEntity o WHERE o.stayId = :stayId AND o.status = com.hotelos.roomservice.domain.OrderStatus.DELIVERED ORDER BY o.deliveredAt ASC, o.id ASC")
    List<RoomServiceOrderEntity> findDeliveredOrdersByStayIdWithItems(@Param("stayId") UUID stayId);
}
