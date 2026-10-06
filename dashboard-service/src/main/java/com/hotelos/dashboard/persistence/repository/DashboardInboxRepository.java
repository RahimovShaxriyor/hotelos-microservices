package com.hotelos.dashboard.persistence.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DashboardInboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardInboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insertIfAbsent(
            UUID eventId,
            String eventType,
            String source,
            String aggregateId,
            String correlationId,
            String envelopeHash,
            Instant receivedAt,
            Instant processedAt
    ) {
        String sql = """
            INSERT INTO dashboard_read.inbox_events (event_id, event_type, source, aggregate_id, correlation_id, envelope_hash, received_at, processed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (event_id) DO NOTHING
            """;
        return jdbcTemplate.update(
                sql,
                eventId,
                eventType,
                source,
                aggregateId,
                correlationId,
                envelopeHash,
                Timestamp.from(receivedAt),
                Timestamp.from(processedAt)
        );
    }

    public Optional<String> findEnvelopeHashByEventId(UUID eventId) {
        String sql = "SELECT envelope_hash FROM dashboard_read.inbox_events WHERE event_id = ?";
        try {
            String hash = jdbcTemplate.queryForObject(sql, String.class, eventId);
            return Optional.ofNullable(hash);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
