package com.hotelos.dashboard.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Repository
public class WsTicketConsumptionRepository {

    private static final Logger log = LoggerFactory.getLogger(WsTicketConsumptionRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public WsTicketConsumptionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Atomically attempts to consume a ticket by its jti.
     * Uses PostgreSQL INSERT ... ON CONFLICT (jti) DO NOTHING.
     *
     * @return true if the ticket was successfully consumed (first time), false if it was already consumed (replay)
     */
    @Transactional
    public boolean consumeTicket(UUID jti, String subject, String username, Instant expiresAt) {
        String sql = """
            INSERT INTO dashboard_read.ws_ticket_consumptions (jti, subject, username, expires_at, consumed_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (jti) DO NOTHING
            """;
        Instant now = Instant.now();
        int affectedRows = jdbcTemplate.update(
                sql,
                jti,
                subject,
                username,
                java.sql.Timestamp.from(expiresAt),
                java.sql.Timestamp.from(now)
        );
        if (affectedRows == 1) {
            log.debug("WebSocket ticket jti={} successfully consumed", jti);
            return true;
        } else {
            log.warn("Replay detected for WebSocket ticket jti={}", jti);
            return false;
        }
    }

    /**
     * Cleans up expired consumed tickets older than retention threshold.
     * Does NOT delete unexpired entries.
     */
    @Transactional
    public int deleteExpiredBefore(Instant threshold) {
        String sql = "DELETE FROM dashboard_read.ws_ticket_consumptions WHERE expires_at < ?";
        int deleted = jdbcTemplate.update(sql, java.sql.Timestamp.from(threshold));
        if (deleted > 0) {
            log.info("Cleaned up {} expired WebSocket ticket consumption records", deleted);
        }
        return deleted;
    }
}
