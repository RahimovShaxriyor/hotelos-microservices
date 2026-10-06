package com.hotelos.dashboard.service;

import com.hotelos.dashboard.dto.EventFeedDto;
import com.hotelos.dashboard.dto.RoomProjectionDto;
import com.hotelos.dashboard.persistence.entity.EventFeedEntity;
import com.hotelos.dashboard.persistence.entity.RoomProjectionEntity;
import com.hotelos.dashboard.persistence.repository.EventFeedRepository;
import com.hotelos.dashboard.persistence.repository.RoomProjectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DashboardQueryService {

    private final RoomProjectionRepository roomProjectionRepository;
    private final EventFeedRepository eventFeedRepository;

    public DashboardQueryService(
            RoomProjectionRepository roomProjectionRepository,
            EventFeedRepository eventFeedRepository
    ) {
        this.roomProjectionRepository = roomProjectionRepository;
        this.eventFeedRepository = eventFeedRepository;
    }

    public Map<String, Object> getSnapshot() {
        List<RoomProjectionEntity> entities = roomProjectionRepository.findAll();
        List<RoomProjectionDto> rooms = entities.stream()
                .map(e -> RoomProjectionDto.from(
                        e.getRoomNumber(),
                        e.getOccupancyStatus(),
                        e.getHousekeepingStatus(),
                        e.getEngineeringStatus(),
                        e.getTurnoverPending(),
                        e.getOccupancyChangedAt(),
                        e.getHousekeepingChangedAt(),
                        e.getEngineeringChangedAt(),
                        e.getTurnoverChangedAt(),
                        e.getUpdatedAt()
                ))
                .toList();

        List<EventFeedEntity> feedEntities = eventFeedRepository.findRecentEvents(200);
        List<Map<String, Object>> events = feedEntities.stream()
                .map(e -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("eventId", e.getEventId() != null ? e.getEventId().toString() : "");
                    map.put("eventType", e.getEventType());
                    map.put("routingKey", e.getRoutingKey());
                    map.put("source", e.getSource());
                    map.put("occurredAt", e.getOccurredAt() != null ? e.getOccurredAt().toString() : "");
                    map.put("receivedAt", e.getReceivedAt() != null ? e.getReceivedAt().toString() : "");
                    map.put("payload", e.getSanitizedPayload());
                    return map;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", "dashboard-service");
        result.put("events", events);
        result.put("eventCount", events.size());
        result.put("totalStoredEvents", eventFeedRepository.count());
        result.put("rooms", rooms);
        result.put("projectionTimestamp", Instant.now().toString());
        result.put("message", "This service keeps live event history and durable CQRS room projections.");
        return result;
    }

    public List<EventFeedDto> getRecentEvents() {
        return eventFeedRepository.findRecentEvents(200).stream()
                .map(e -> new EventFeedDto(
                        e.getEventId(),
                        e.getEventType(),
                        e.getEventVersion(),
                        e.getSource(),
                        e.getRoutingKey(),
                        e.getAggregateId(),
                        e.getCorrelationId(),
                        e.getOccurredAt(),
                        e.getReceivedAt(),
                        e.getSummary(),
                        e.getSanitizedPayload()
                ))
                .toList();
    }

    @Transactional
    public Map<String, Object> clearEvents() {
        long count = eventFeedRepository.count();
        eventFeedRepository.deleteAll();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Dashboard event history cleared");
        result.put("removed", count);
        return result;
    }
}
