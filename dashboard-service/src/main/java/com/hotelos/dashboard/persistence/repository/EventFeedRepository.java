package com.hotelos.dashboard.persistence.repository;

import com.hotelos.dashboard.persistence.entity.EventFeedEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class EventFeedRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<EventFeedEntity> rowMapper = new RowMapper<>() {
        @Override
        public EventFeedEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            EventFeedEntity entity = new EventFeedEntity();
            entity.setEventId(UUID.fromString(rs.getString("event_id")));
            entity.setEventType(rs.getString("event_type"));
            entity.setEventVersion(rs.getInt("event_version"));
            entity.setSource(rs.getString("source"));
            entity.setRoutingKey(rs.getString("routing_key"));
            entity.setAggregateId(rs.getString("aggregate_id"));
            entity.setCorrelationId(rs.getString("correlation_id"));

            Timestamp occTs = rs.getTimestamp("occurred_at");
            entity.setOccurredAt(occTs != null ? occTs.toInstant() : null);

            Timestamp recTs = rs.getTimestamp("received_at");
            entity.setReceivedAt(recTs != null ? recTs.toInstant() : null);

            entity.setSummary(rs.getString("summary"));
            entity.setSanitizedPayload(rs.getString("sanitized_payload"));
            return entity;
        }
    };

    public EventFeedRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(EventFeedEntity entity) {
        String sql = """
            INSERT INTO dashboard_read.event_feed (event_id, event_type, event_version, source, routing_key, aggregate_id, correlation_id, occurred_at, received_at, summary, sanitized_payload)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (event_id) DO NOTHING
            """;
        return jdbcTemplate.update(
                sql,
                entity.getEventId(),
                entity.getEventType(),
                entity.getEventVersion(),
                entity.getSource(),
                entity.getRoutingKey(),
                entity.getAggregateId(),
                entity.getCorrelationId(),
                Timestamp.from(entity.getOccurredAt()),
                Timestamp.from(entity.getReceivedAt()),
                entity.getSummary(),
                entity.getSanitizedPayload()
        );
    }

    public List<EventFeedEntity> findRecentEvents(int limit) {
        String sql = """
            SELECT event_id, event_type, event_version, source, routing_key, aggregate_id, correlation_id, occurred_at, received_at, summary, sanitized_payload
            FROM dashboard_read.event_feed
            ORDER BY occurred_at DESC, received_at DESC, event_id DESC
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, rowMapper, limit);
    }

    public int deleteOldEventsExceedingRetention() {
        String sql = """
            DELETE FROM dashboard_read.event_feed
            WHERE event_id IN (
                SELECT event_id FROM dashboard_read.event_feed
                ORDER BY occurred_at DESC, received_at DESC, event_id DESC
                OFFSET 5000
            )
            """;
        return jdbcTemplate.update(sql);
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM dashboard_read.event_feed", Long.class);
        return count != null ? count : 0L;
    }

    public int deleteAll() {
        return jdbcTemplate.update("DELETE FROM dashboard_read.event_feed");
    }
}
