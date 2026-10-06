package com.hotelos.dashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hotelos.dashboard.model.ProjectionOutcome;
import com.hotelos.dashboard.persistence.entity.EventFeedEntity;
import com.hotelos.dashboard.persistence.entity.RoomProjectionEntity;
import com.hotelos.dashboard.persistence.repository.DashboardInboxRepository;
import com.hotelos.dashboard.persistence.repository.EventFeedRepository;
import com.hotelos.dashboard.persistence.repository.RoomProjectionRepository;
import com.hotelos.dashboard.websocket.DashboardSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class DashboardProjectionService {

    private static final Logger log = LoggerFactory.getLogger(DashboardProjectionService.class);

    private final DashboardInboxRepository inboxRepository;
    private final RoomProjectionRepository roomProjectionRepository;
    private final EventFeedRepository eventFeedRepository;
    private final DashboardSocketHandler socketHandler;
    private final ObjectMapper objectMapper;

    public DashboardProjectionService(
            DashboardInboxRepository inboxRepository,
            RoomProjectionRepository roomProjectionRepository,
            EventFeedRepository eventFeedRepository,
            DashboardSocketHandler socketHandler,
            ObjectMapper objectMapper
    ) {
        this.inboxRepository = inboxRepository;
        this.roomProjectionRepository = roomProjectionRepository;
        this.eventFeedRepository = eventFeedRepository;
        this.socketHandler = socketHandler;
        this.objectMapper = objectMapper;
    }

    public static String computeEnvelopeHash(String routingKey, byte[] bodyBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(routingKey != null ? routingKey.getBytes(StandardCharsets.UTF_8) : new byte[0]);
            md.update((byte) 0x00);
            md.update(bodyBytes != null ? bodyBytes : new byte[0]);
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    @Transactional
    public ProjectionOutcome processMessage(String routingKey, byte[] rawBytes) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBytes);
            if (root == null || root.isNull() || !root.isObject()) {
                log.warn("MALFORMED_AMQP_MESSAGE: Root is null or not JSON object");
                return ProjectionOutcome.INTEGRITY_CONFLICT;
            }
        } catch (Exception ex) {
            log.warn("MALFORMED_AMQP_MESSAGE: Failed to parse JSON: {}", ex.getMessage());
            return ProjectionOutcome.INTEGRITY_CONFLICT;
        }

        JsonNode eventIdNode = root.get("eventId");
        JsonNode eventTypeNode = root.get("eventType");
        JsonNode occurredAtNode = root.get("occurredAt");

        if (eventIdNode == null || eventIdNode.isNull() || eventTypeNode == null || occurredAtNode == null) {
            log.warn("MALFORMED_EVENT_ENVELOPE: Missing mandatory envelope fields");
            return ProjectionOutcome.INTEGRITY_CONFLICT;
        }

        UUID eventId;
        try {
            eventId = UUID.fromString(eventIdNode.asText());
        } catch (Exception ex) {
            log.warn("MALFORMED_EVENT_ID: Invalid UUID: {}", eventIdNode.asText());
            return ProjectionOutcome.INTEGRITY_CONFLICT;
        }

        String eventType = eventTypeNode.asText();
        int eventVersion = root.has("eventVersion") && !root.get("eventVersion").isNull() ? root.get("eventVersion").asInt(1) : 1;
        String source = root.has("source") && !root.get("source").isNull() ? root.get("source").asText() : "unknown";
        String aggregateId = root.has("aggregateId") && !root.get("aggregateId").isNull() ? root.get("aggregateId").asText() : null;
        String correlationId = root.has("correlationId") && !root.get("correlationId").isNull() ? root.get("correlationId").asText() : null;

        Instant occurredAt;
        try {
            occurredAt = Instant.parse(occurredAtNode.asText());
        } catch (Exception ex) {
            occurredAt = Instant.now();
        }

        JsonNode payload = root.get("payload");
        if (payload == null || payload.isNull()) {
            log.warn("MALFORMED_EVENT_PAYLOAD: Null payload for eventId: {}", eventId);
            return ProjectionOutcome.INTEGRITY_CONFLICT;
        }

        String envelopeHash = computeEnvelopeHash(routingKey, rawBytes);
        Instant now = Instant.now();

        // 1. Atomic Inbox Registration
        int inserted = inboxRepository.insertIfAbsent(
                eventId, eventType, source, aggregateId, correlationId, envelopeHash, now, now
        );

        if (inserted == 0) {
            Optional<String> storedHash = inboxRepository.findEnvelopeHashByEventId(eventId);
            if (storedHash.isPresent() && storedHash.get().equals(envelopeHash)) {
                log.info("TRANSPORT_DUPLICATE: eventId {} already processed with matching hash", eventId);
                return ProjectionOutcome.TRANSPORT_DUPLICATE;
            } else {
                log.error("EVENT_ID_REUSE_ANOMALY: eventId {} already exists with different envelope hash", eventId);
                return ProjectionOutcome.EVENT_ID_REUSE_ANOMALY;
            }
        }

        // 2. Dispatch by (eventType, eventVersion)
        ProjectionOutcome outcome = dispatchEvent(eventType, eventVersion, payload, correlationId, occurredAt);

        // 3. Sanitized Feed Persistence
        String sanitizedJson = buildSanitizedPayloadJson(eventType, eventVersion, payload);
        String summary = buildSummary(eventType, eventVersion, payload);

        if (outcome == ProjectionOutcome.APPLIED || outcome == ProjectionOutcome.FEED_ONLY
                || outcome == ProjectionOutcome.STALE || outcome == ProjectionOutcome.LOGICAL_DUPLICATE) {
            EventFeedEntity feedEntity = new EventFeedEntity(
                    eventId, eventType, eventVersion, source, routingKey, aggregateId, correlationId,
                    occurredAt, now, summary, sanitizedJson
            );
            eventFeedRepository.save(feedEntity);
        }

        // 4. WebSocket Broadcast after-commit (strictly APPLIED or FEED_ONLY)
        if (outcome == ProjectionOutcome.APPLIED || outcome == ProjectionOutcome.FEED_ONLY) {
            String broadcastPayload = buildBroadcastMessage(routingKey, now, sanitizedJson);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        socketHandler.broadcast(broadcastPayload);
                    } catch (Exception ex) {
                        log.warn("Non-fatal: WebSocket broadcast exception after commit for event {}: {}", eventId, ex.getMessage());
                    }
                }
            });
        }

        return outcome;
    }

    private ProjectionOutcome dispatchEvent(String eventType, int eventVersion, JsonNode payload, String correlationId, Instant occurredAt) {
        return switch (eventType) {
            case "reception.room.occupancy.changed" -> {
                if (eventVersion != 1) yield ProjectionOutcome.INTEGRITY_CONFLICT;
                yield handleOccupancyChanged(payload, occurredAt);
            }
            case "reception.room.vacated", "room.vacated" -> {
                if (eventVersion != 1) yield ProjectionOutcome.INTEGRITY_CONFLICT;
                yield handleRoomVacated(payload, correlationId, occurredAt);
            }
            case "housekeeping.room.status.changed" -> {
                if (eventVersion != 1) yield ProjectionOutcome.INTEGRITY_CONFLICT;
                yield handleHousekeepingChanged(payload, correlationId, occurredAt);
            }
            case "maintenance.room.status.changed" -> {
                if (eventVersion != 1) yield ProjectionOutcome.INTEGRITY_CONFLICT;
                yield handleEngineeringChanged(payload, occurredAt);
            }
            case "room.service.charge" -> {
                if (eventVersion == 1 || eventVersion == 2) {
                    yield ProjectionOutcome.FEED_ONLY;
                }
                log.warn("UNSUPPORTED_EVENT_VERSION: Received unsupported version {} for room.service.charge", eventVersion);
                yield ProjectionOutcome.INTEGRITY_CONFLICT;
            }
            case "room.service.order.updated", "maintenance.issue.updated",
                 "maintenance.work.completed", "housekeeping.maintenance.cleanup.completed",
                 "reception.stay.checked_out" -> {
                if (eventVersion == 1) {
                    yield ProjectionOutcome.FEED_ONLY;
                }
                log.warn("UNSUPPORTED_EVENT_VERSION: Received unsupported version {} for {}", eventVersion, eventType);
                yield ProjectionOutcome.INTEGRITY_CONFLICT;
            }
            default -> {
                log.info("UNHANDLED_EVENT_TYPE: Skipping projection for event type {}", eventType);
                yield ProjectionOutcome.FEED_ONLY;
            }
        };
    }

    private ProjectionOutcome handleOccupancyChanged(JsonNode payload, Instant occurredAt) {
        String roomNumber = payload.has("roomNumber") ? payload.get("roomNumber").asText() : null;
        String status = payload.has("occupancyStatus") ? payload.get("occupancyStatus").asText() : null;
        Instant changedAt = parseInstant(payload.get("changedAt"), occurredAt);

        if (roomNumber == null || status == null) {
            return ProjectionOutcome.INTEGRITY_CONFLICT;
        }

        roomProjectionRepository.insertRoomIfAbsent(roomNumber);
        int updated = roomProjectionRepository.updateOccupancyIfNewer(roomNumber, status, changedAt);

        if ("OCCUPIED".equals(status)) {
            roomProjectionRepository.resetTurnoverOnOccupiedIfNewer(roomNumber, changedAt);
        }

        if (updated > 0) {
            return ProjectionOutcome.APPLIED;
        }

        Optional<RoomProjectionEntity> storedOpt = roomProjectionRepository.findByRoomNumber(roomNumber);
        if (storedOpt.isEmpty()) return ProjectionOutcome.APPLIED;
        RoomProjectionEntity stored = storedOpt.get();

        Instant storedTS = stored.getOccupancyChangedAt();
        String storedStatus = stored.getOccupancyStatus();

        if (storedTS != null && storedTS.isAfter(changedAt)) {
            return ProjectionOutcome.STALE;
        }
        if (storedTS != null && storedTS.equals(changedAt)) {
            return Objects.equals(storedStatus, status) ? ProjectionOutcome.LOGICAL_DUPLICATE : ProjectionOutcome.INTEGRITY_CONFLICT;
        }
        return ProjectionOutcome.APPLIED;
    }

    private ProjectionOutcome handleRoomVacated(JsonNode payload, String correlationId, Instant occurredAt) {
        String roomNumber = payload.has("roomNumber") ? payload.get("roomNumber").asText() : null;
        Instant vacatedAt = parseInstant(payload.get("vacatedAt"), occurredAt);
        if ((correlationId == null || correlationId.isBlank()) && payload.has("turnoverCorrelationId") && !payload.get("turnoverCorrelationId").isNull()) {
            correlationId = payload.get("turnoverCorrelationId").asText();
        }

        if (roomNumber == null || correlationId == null || correlationId.isBlank()) {
            log.warn("INVALID_ROOM_VACATED: Missing roomNumber or turnover correlationId for room {}", roomNumber);
            return ProjectionOutcome.INTEGRITY_CONFLICT;
        }

        roomProjectionRepository.insertRoomIfAbsent(roomNumber);
        int updated = roomProjectionRepository.setTurnoverPendingIfNewer(roomNumber, correlationId, vacatedAt);

        if (updated > 0) {
            return ProjectionOutcome.APPLIED;
        }

        Optional<RoomProjectionEntity> storedOpt = roomProjectionRepository.findByRoomNumber(roomNumber);
        if (storedOpt.isEmpty()) return ProjectionOutcome.APPLIED;
        RoomProjectionEntity stored = storedOpt.get();

        Instant storedTS = stored.getTurnoverChangedAt();
        Boolean storedPending = stored.getTurnoverPending();
        String storedCorr = stored.getTurnoverCorrelationId();

        if (storedTS != null && storedTS.isAfter(vacatedAt)) {
            return ProjectionOutcome.STALE;
        }
        if (storedTS != null && storedTS.equals(vacatedAt)) {
            boolean same = Boolean.TRUE.equals(storedPending) && Objects.equals(storedCorr, correlationId);
            return same ? ProjectionOutcome.LOGICAL_DUPLICATE : ProjectionOutcome.INTEGRITY_CONFLICT;
        }
        return ProjectionOutcome.APPLIED;
    }

    private ProjectionOutcome handleHousekeepingChanged(JsonNode payload, String correlationId, Instant occurredAt) {
        String roomNumber = payload.has("roomNumber") ? payload.get("roomNumber").asText() : null;
        String status = payload.has("housekeepingStatus") ? payload.get("housekeepingStatus").asText() : null;
        Instant changedAt = parseInstant(payload.get("changedAt"), occurredAt);
        if ((correlationId == null || correlationId.isBlank()) && payload.has("turnoverCorrelationId") && !payload.get("turnoverCorrelationId").isNull()) {
            correlationId = payload.get("turnoverCorrelationId").asText();
        }

        if (roomNumber == null || status == null) {
            return ProjectionOutcome.INTEGRITY_CONFLICT;
        }

        roomProjectionRepository.insertRoomIfAbsent(roomNumber);
        int updated = roomProjectionRepository.updateHousekeepingIfNewer(roomNumber, status, changedAt);

        if ("CLEAN".equals(status) && correlationId != null && !correlationId.isBlank()) {
            roomProjectionRepository.clearTurnoverPendingIfMatchingAndNewer(roomNumber, correlationId, changedAt);
        }

        if (updated > 0) {
            return ProjectionOutcome.APPLIED;
        }

        Optional<RoomProjectionEntity> storedOpt = roomProjectionRepository.findByRoomNumber(roomNumber);
        if (storedOpt.isEmpty()) return ProjectionOutcome.APPLIED;
        RoomProjectionEntity stored = storedOpt.get();

        Instant storedTS = stored.getHousekeepingChangedAt();
        String storedStatus = stored.getHousekeepingStatus();

        if (storedTS != null && storedTS.isAfter(changedAt)) {
            return ProjectionOutcome.STALE;
        }
        if (storedTS != null && storedTS.equals(changedAt)) {
            return Objects.equals(storedStatus, status) ? ProjectionOutcome.LOGICAL_DUPLICATE : ProjectionOutcome.INTEGRITY_CONFLICT;
        }
        return ProjectionOutcome.APPLIED;
    }

    private ProjectionOutcome handleEngineeringChanged(JsonNode payload, Instant occurredAt) {
        String roomNumber = payload.has("roomNumber") ? payload.get("roomNumber").asText() : null;
        String status = payload.has("engineeringStatus") ? payload.get("engineeringStatus").asText() : null;
        Instant changedAt = parseInstant(payload.get("changedAt"), occurredAt);

        if (roomNumber == null || status == null) {
            return ProjectionOutcome.INTEGRITY_CONFLICT;
        }

        roomProjectionRepository.insertRoomIfAbsent(roomNumber);
        int updated = roomProjectionRepository.updateEngineeringIfNewer(roomNumber, status, changedAt);

        if (updated > 0) {
            return ProjectionOutcome.APPLIED;
        }

        Optional<RoomProjectionEntity> storedOpt = roomProjectionRepository.findByRoomNumber(roomNumber);
        if (storedOpt.isEmpty()) return ProjectionOutcome.APPLIED;
        RoomProjectionEntity stored = storedOpt.get();

        Instant storedTS = stored.getEngineeringChangedAt();
        String storedStatus = stored.getEngineeringStatus();

        if (storedTS != null && storedTS.isAfter(changedAt)) {
            return ProjectionOutcome.STALE;
        }
        if (storedTS != null && storedTS.equals(changedAt)) {
            return Objects.equals(storedStatus, status) ? ProjectionOutcome.LOGICAL_DUPLICATE : ProjectionOutcome.INTEGRITY_CONFLICT;
        }
        return ProjectionOutcome.APPLIED;
    }

    private Instant parseInstant(JsonNode node, Instant fallback) {
        if (node == null || node.isNull() || node.asText().isBlank()) {
            return fallback != null ? fallback : Instant.now();
        }
        try {
            return Instant.parse(node.asText());
        } catch (Exception ex) {
            return fallback != null ? fallback : Instant.now();
        }
    }

    private String buildSanitizedPayloadJson(String eventType, int eventVersion, JsonNode payload) {
        ObjectNode sanitized = objectMapper.createObjectNode();
        switch (eventType) {
            case "reception.room.occupancy.changed" -> {
                if (payload.has("roomNumber")) sanitized.set("roomNumber", payload.get("roomNumber"));
                if (payload.has("occupancyStatus")) sanitized.set("occupancyStatus", payload.get("occupancyStatus"));
                if (payload.has("changedAt")) sanitized.set("changedAt", payload.get("changedAt"));
            }
            case "reception.room.vacated", "room.vacated" -> {
                if (payload.has("roomNumber")) sanitized.set("roomNumber", payload.get("roomNumber"));
                if (payload.has("vacatedAt")) sanitized.set("vacatedAt", payload.get("vacatedAt"));
            }
            case "housekeeping.room.status.changed" -> {
                if (payload.has("roomNumber")) sanitized.set("roomNumber", payload.get("roomNumber"));
                if (payload.has("housekeepingStatus")) sanitized.set("housekeepingStatus", payload.get("housekeepingStatus"));
                if (payload.has("changedAt")) sanitized.set("changedAt", payload.get("changedAt"));
            }
            case "maintenance.room.status.changed" -> {
                if (payload.has("roomNumber")) sanitized.set("roomNumber", payload.get("roomNumber"));
                if (payload.has("engineeringStatus")) sanitized.set("engineeringStatus", payload.get("engineeringStatus"));
                if (payload.has("changedAt")) sanitized.set("changedAt", payload.get("changedAt"));
            }
            case "room.service.charge" -> {
                if (payload.has("orderId")) sanitized.set("orderId", payload.get("orderId"));
                if (eventVersion == 2 && payload.has("stayId")) sanitized.set("stayId", payload.get("stayId"));
                if (payload.has("roomNumber")) sanitized.set("roomNumber", payload.get("roomNumber"));
                if (payload.has("amount")) sanitized.set("amount", payload.get("amount"));
                if (payload.has("currency")) sanitized.set("currency", payload.get("currency"));
                if (payload.has("chargedAt")) sanitized.set("chargedAt", payload.get("chargedAt"));
            }
            case "room.service.order.updated" -> {
                if (payload.has("orderId")) sanitized.set("orderId", payload.get("orderId"));
                if (payload.has("roomNumber")) sanitized.set("roomNumber", payload.get("roomNumber"));
                if (payload.has("status")) sanitized.set("status", payload.get("status"));
                if (payload.has("total")) sanitized.set("total", payload.get("total"));
                if (payload.has("changedAt")) sanitized.set("changedAt", payload.get("changedAt"));
            }
            case "maintenance.issue.updated" -> {
                if (payload.has("issueId")) sanitized.set("issueId", payload.get("issueId"));
                if (payload.has("roomNumber")) sanitized.set("roomNumber", payload.get("roomNumber"));
                if (payload.has("priority")) sanitized.set("priority", payload.get("priority"));
                if (payload.has("status")) sanitized.set("status", payload.get("status"));
                if (payload.has("assignedTechnician")) sanitized.set("assignedTechnician", payload.get("assignedTechnician"));
                if (payload.has("changedAt")) sanitized.set("changedAt", payload.get("changedAt"));
            }
            case "maintenance.work.completed" -> {
                if (payload.has("issueId")) sanitized.set("issueId", payload.get("issueId"));
                if (payload.has("roomNumber")) sanitized.set("roomNumber", payload.get("roomNumber"));
                if (payload.has("requiresCleaning")) sanitized.set("requiresCleaning", payload.get("requiresCleaning"));
                if (payload.has("completedAt")) sanitized.set("completedAt", payload.get("completedAt"));
            }
            case "housekeeping.maintenance.cleanup.completed" -> {
                if (payload.has("issueId")) sanitized.set("issueId", payload.get("issueId"));
                if (payload.has("roomNumber")) sanitized.set("roomNumber", payload.get("roomNumber"));
                if (payload.has("cleanedAt")) sanitized.set("cleanedAt", payload.get("cleanedAt"));
            }
            case "reception.stay.checked_out" -> {
                if (payload.has("stayId")) sanitized.set("stayId", payload.get("stayId"));
                if (payload.has("folioId")) sanitized.set("folioId", payload.get("folioId"));
                if (payload.has("checkedOutAt")) sanitized.set("checkedOutAt", payload.get("checkedOutAt"));
            }
            default -> {
                return payload.toString();
            }
        }
        return sanitized.toString();
    }

    private String buildSummary(String eventType, int eventVersion, JsonNode payload) {
        String room = payload.has("roomNumber") ? payload.get("roomNumber").asText() : "";
        return switch (eventType) {
            case "reception.room.occupancy.changed" -> "Room " + room + " occupancy changed to " +
                    (payload.has("occupancyStatus") ? payload.get("occupancyStatus").asText() : "");
            case "reception.room.vacated", "room.vacated" -> "Room " + room + " vacated (turnover pending)";
            case "housekeeping.room.status.changed" -> "Room " + room + " housekeeping status changed to " +
                    (payload.has("housekeepingStatus") ? payload.get("housekeepingStatus").asText() : "");
            case "maintenance.room.status.changed" -> "Room " + room + " engineering status changed to " +
                    (payload.has("engineeringStatus") ? payload.get("engineeringStatus").asText() : "");
            case "room.service.charge" -> "Room Service Charge: " +
                    (payload.has("amount") ? payload.get("amount").asText() : "") + " " +
                    (payload.has("currency") ? payload.get("currency").asText() : "") + " (order " +
                    (payload.has("orderId") ? payload.get("orderId").asText() : "") + ")";
            case "room.service.order.updated" -> "Room Service order " +
                    (payload.has("orderId") ? payload.get("orderId").asText() : "") + " updated: " +
                    (payload.has("status") ? payload.get("status").asText() : "");
            case "maintenance.issue.updated" -> "Maintenance issue " +
                    (payload.has("issueId") ? payload.get("issueId").asText() : "") + " in room " + room + ": " +
                    (payload.has("status") ? payload.get("status").asText() : "");
            case "maintenance.work.completed" -> "Maintenance work completed for issue " +
                    (payload.has("issueId") ? payload.get("issueId").asText() : "") + " in room " + room;
            case "housekeeping.maintenance.cleanup.completed" -> "Maintenance cleanup completed for issue " +
                    (payload.has("issueId") ? payload.get("issueId").asText() : "") + " in room " + room;
            case "reception.stay.checked_out" -> "Stay " +
                    (payload.has("stayId") ? payload.get("stayId").asText() : "") + " checked out";
            default -> "Event: " + eventType;
        };
    }

    private String buildBroadcastMessage(String routingKey, Instant receivedAt, String sanitizedPayload) {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("routingKey", routingKey);
        envelope.put("receivedAt", receivedAt.toString());
        envelope.put("payload", sanitizedPayload);
        return envelope.toString();
    }
}
