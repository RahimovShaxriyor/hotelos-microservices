package com.hotelos.dashboard.persistence.repository;

import com.hotelos.dashboard.persistence.entity.RoomProjectionEntity;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class RoomProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<RoomProjectionEntity> rowMapper = new RowMapper<>() {
        @Override
        public RoomProjectionEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            RoomProjectionEntity entity = new RoomProjectionEntity();
            entity.setRoomNumber(rs.getString("room_number"));
            entity.setOccupancyStatus(rs.getString("occupancy_status"));
            entity.setHousekeepingStatus(rs.getString("housekeeping_status"));
            entity.setEngineeringStatus(rs.getString("engineering_status"));

            Object turnoverPendingObj = rs.getObject("turnover_pending");
            entity.setTurnoverPending(turnoverPendingObj != null ? rs.getBoolean("turnover_pending") : null);

            entity.setTurnoverCorrelationId(rs.getString("turnover_correlation_id"));

            Timestamp occTs = rs.getTimestamp("occupancy_changed_at");
            entity.setOccupancyChangedAt(occTs != null ? occTs.toInstant() : null);

            Timestamp hkTs = rs.getTimestamp("housekeeping_changed_at");
            entity.setHousekeepingChangedAt(hkTs != null ? hkTs.toInstant() : null);

            Timestamp engTs = rs.getTimestamp("engineering_changed_at");
            entity.setEngineeringChangedAt(engTs != null ? engTs.toInstant() : null);

            Timestamp toTs = rs.getTimestamp("turnover_changed_at");
            entity.setTurnoverChangedAt(toTs != null ? toTs.toInstant() : null);

            Timestamp createdTs = rs.getTimestamp("created_at");
            entity.setCreatedAt(createdTs != null ? createdTs.toInstant() : null);

            Timestamp updatedTs = rs.getTimestamp("updated_at");
            entity.setUpdatedAt(updatedTs != null ? updatedTs.toInstant() : null);

            return entity;
        }
    };

    public RoomProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<RoomProjectionEntity> findByRoomNumber(String roomNumber) {
        String sql = "SELECT * FROM dashboard_read.room_projection WHERE room_number = ?";
        try {
            RoomProjectionEntity entity = jdbcTemplate.queryForObject(sql, rowMapper, roomNumber);
            return Optional.ofNullable(entity);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<RoomProjectionEntity> findAll() {
        String sql = "SELECT * FROM dashboard_read.room_projection ORDER BY room_number ASC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM dashboard_read.room_projection", Long.class);
        return count != null ? count : 0L;
    }

    public int insertRoomIfAbsent(String roomNumber) {
        String sql = """
            INSERT INTO dashboard_read.room_projection (room_number, created_at, updated_at)
            VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (room_number) DO NOTHING
            """;
        return jdbcTemplate.update(sql, roomNumber);
    }

    public int updateOccupancyIfNewer(String roomNumber, String status, Instant changedAt) {
        String sql = """
            UPDATE dashboard_read.room_projection
            SET occupancy_status = ?,
                occupancy_changed_at = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE room_number = ?
              AND (occupancy_changed_at IS NULL OR occupancy_changed_at < ?)
            """;
        return jdbcTemplate.update(sql, status, Timestamp.from(changedAt), roomNumber, Timestamp.from(changedAt));
    }

    public int updateHousekeepingIfNewer(String roomNumber, String status, Instant changedAt) {
        String sql = """
            UPDATE dashboard_read.room_projection
            SET housekeeping_status = ?,
                housekeeping_changed_at = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE room_number = ?
              AND (housekeeping_changed_at IS NULL OR housekeeping_changed_at < ?)
            """;
        return jdbcTemplate.update(sql, status, Timestamp.from(changedAt), roomNumber, Timestamp.from(changedAt));
    }

    public int updateEngineeringIfNewer(String roomNumber, String status, Instant changedAt) {
        String sql = """
            UPDATE dashboard_read.room_projection
            SET engineering_status = ?,
                engineering_changed_at = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE room_number = ?
              AND (engineering_changed_at IS NULL OR engineering_changed_at < ?)
            """;
        return jdbcTemplate.update(sql, status, Timestamp.from(changedAt), roomNumber, Timestamp.from(changedAt));
    }

    public int setTurnoverPendingIfNewer(String roomNumber, String correlationId, Instant changedAt) {
        String sql = """
            UPDATE dashboard_read.room_projection
            SET turnover_pending = TRUE,
                turnover_correlation_id = ?,
                turnover_changed_at = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE room_number = ?
              AND (turnover_changed_at IS NULL OR turnover_changed_at < ?)
            """;
        return jdbcTemplate.update(sql, correlationId, Timestamp.from(changedAt), roomNumber, Timestamp.from(changedAt));
    }

    public int clearTurnoverPendingIfMatchingAndNewer(String roomNumber, String correlationId, Instant changedAt) {
        String sql = """
            UPDATE dashboard_read.room_projection
            SET turnover_pending = FALSE,
                turnover_correlation_id = NULL,
                turnover_changed_at = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE room_number = ?
              AND turnover_pending = TRUE
              AND turnover_correlation_id = ?
              AND (turnover_changed_at IS NULL OR turnover_changed_at < ?)
            """;
        return jdbcTemplate.update(sql, Timestamp.from(changedAt), roomNumber, correlationId, Timestamp.from(changedAt));
    }

    public int resetTurnoverOnOccupiedIfNewer(String roomNumber, Instant changedAt) {
        String sql = """
            UPDATE dashboard_read.room_projection
            SET turnover_pending = FALSE,
                turnover_correlation_id = NULL,
                turnover_changed_at = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE room_number = ?
              AND (turnover_changed_at IS NULL OR turnover_changed_at < ?)
            """;
        return jdbcTemplate.update(sql, Timestamp.from(changedAt), roomNumber, Timestamp.from(changedAt));
    }

    public int updateTurnoverBootstrapIfNewer(String roomNumber, boolean pending, String correlationId, Instant changedAt) {
        String sql = """
            UPDATE dashboard_read.room_projection
            SET turnover_pending = ?,
                turnover_correlation_id = ?,
                turnover_changed_at = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE room_number = ?
              AND (turnover_changed_at IS NULL OR turnover_changed_at < ?)
            """;
        return jdbcTemplate.update(sql, pending, correlationId, Timestamp.from(changedAt), roomNumber, Timestamp.from(changedAt));
    }
}
