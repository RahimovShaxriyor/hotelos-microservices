package com.hotelos.roomservice.persistence.repository;

import com.hotelos.roomservice.persistence.entity.RoomServiceOutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomServiceOutboxRepository extends JpaRepository<RoomServiceOutboxEventEntity, Long> {

    @Query(value = """
            SELECT * FROM room_service.outbox_events o
            WHERE o.published_at IS NULL
              AND (o.next_attempt_at IS NULL OR o.next_attempt_at <= CURRENT_TIMESTAMP)
              AND NOT EXISTS (
                  SELECT 1 FROM room_service.outbox_events prior
                  WHERE prior.aggregate_id = o.aggregate_id
                    AND prior.published_at IS NULL
                    AND prior.id < o.id
              )
            ORDER BY o.id ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<RoomServiceOutboxEventEntity> findNextEligibleEventForUpdate();

    Optional<RoomServiceOutboxEventEntity> findByEventId(UUID eventId);
}
