package com.hotelos.housekeeping.persistence.repository;

import com.hotelos.housekeeping.persistence.entity.HousekeepingInboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HousekeepingInboxRepository extends JpaRepository<HousekeepingInboxEventEntity, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO housekeeping.inbox_events (event_id, event_type, source, aggregate_id, correlation_id, envelope_hash, received_at, processed_at)
            VALUES (:eventId, :eventType, :source, :aggregateId, :correlationId, :envelopeHash, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertOnConflictDoNothing(
            @Param("eventId") UUID eventId,
            @Param("eventType") String eventType,
            @Param("source") String source,
            @Param("aggregateId") String aggregateId,
            @Param("correlationId") String correlationId,
            @Param("envelopeHash") String envelopeHash
    );
}
