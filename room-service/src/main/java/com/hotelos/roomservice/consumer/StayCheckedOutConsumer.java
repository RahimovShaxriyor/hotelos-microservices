package com.hotelos.roomservice.consumer;

import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.payload.StayCheckedOutPayload;
import com.hotelos.roomservice.config.RabbitConfig;
import com.hotelos.roomservice.domain.OrderingSessionStatus;
import com.hotelos.roomservice.persistence.entity.StayOrderingSessionEntity;
import com.hotelos.roomservice.persistence.repository.StayOrderingSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class StayCheckedOutConsumer {

    private static final Logger log = LoggerFactory.getLogger(StayCheckedOutConsumer.class);

    private final StayOrderingSessionRepository sessionRepository;
    private final boolean strictBarrier;

    public StayCheckedOutConsumer(
            StayOrderingSessionRepository sessionRepository,
            @Value("${hotelos.room-service.strict-barrier:${HOTELOS_ROOM_SERVICE_STRICT_BARRIER:true}}") boolean strictBarrier
    ) {
        this.sessionRepository = sessionRepository;
        this.strictBarrier = strictBarrier;
    }

    @RabbitListener(queues = RabbitConfig.ROOM_SERVICE_STAY_CHECKED_OUT_QUEUE)
    @Transactional
    public void onStayCheckedOut(EventEnvelope<StayCheckedOutPayload> event) {
        if (event == null || event.payload() == null) {
            log.error("MALFORMED_EVENT_PAYLOAD: Received empty StayCheckedOut event");
            return;
        }

        StayCheckedOutPayload payload = event.payload();
        UUID stayId = payload.stayId();
        if (stayId == null) {
            log.error("MALFORMED_EVENT_PAYLOAD: StayCheckedOut payload missing stayId");
            return;
        }

        UUID freezeId = payload.freezeId();
        if (freezeId == null) {
            if (strictBarrier) {
                log.error("PROTOCOL_INTEGRITY_ERROR: StayCheckedOutPayload.freezeId is null in strict mode for stay {}", stayId);
                return;
            } else {
                log.info("COMPATIBILITY_P34_CHECKOUT: StayCheckedOutPayload with null freezeId accepted in compatibility mode for stay {}", stayId);
                return;
            }
        }

        // Canonical ensure pattern
        sessionRepository.ensureSessionRow(stayId);

        StayOrderingSessionEntity session = sessionRepository.findByStayIdForUpdate(stayId)
                .orElseThrow(() -> new IllegalStateException("Failed to lock stay ordering session: " + stayId));

        Instant closedAt = payload.checkedOutAt() != null ? payload.checkedOutAt() : Instant.now();

        if (session.getStatus() == OrderingSessionStatus.FROZEN) {
            if (!freezeId.equals(session.getFreezeId())) {
                log.error("CRITICAL_INTEGRITY_MISMATCH: FreezeId mismatch for stay {}: expected {}, received {}",
                        stayId, session.getFreezeId(), freezeId);
                return;
            }
            session.close(closedAt);
            sessionRepository.save(session);
            log.info("Async finalize completed for stay {} with freezeId {}", stayId, freezeId);
            return;
        }

        if (session.getStatus() == OrderingSessionStatus.CLOSED) {
            if (!freezeId.equals(session.getFreezeId())) {
                log.error("CRITICAL_INTEGRITY_MISMATCH: FreezeId mismatch for closed stay {}: expected {}, received {}",
                        stayId, session.getFreezeId(), freezeId);
                return;
            }
            log.info("Async finalize duplicate no-op for already closed stay {}", stayId);
            return;
        }

        if (session.getStatus() == OrderingSessionStatus.ACTIVE) {
            log.error("CRITICAL_INTEGRITY_MISMATCH: Authoritative StayCheckedOut received with non-null freezeId {} but local session is ACTIVE for stay {}",
                    freezeId, stayId);
            // Fail-safe transition to prevent further ordering anomalies
            session.freeze(freezeId, Instant.now());
            session.close(closedAt);
            sessionRepository.save(session);
        }
    }
}
