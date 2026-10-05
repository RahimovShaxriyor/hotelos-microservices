package com.hotelos.maintenance.inbox;

import com.hotelos.common.event.EnvelopeFingerprint;
import com.hotelos.common.event.EventEnvelope;
import com.hotelos.maintenance.persistence.entity.MaintenanceInboxEventEntity;
import com.hotelos.maintenance.persistence.repository.MaintenanceInboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class MaintenanceInboxService {
    private static final Logger log = LoggerFactory.getLogger(MaintenanceInboxService.class);

    public enum InboxResult {
        PROCEED,
        DUPLICATE,
        CONFLICT
    }

    private final MaintenanceInboxRepository inboxRepository;

    public MaintenanceInboxService(MaintenanceInboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public InboxResult registerEvent(EventEnvelope<?> envelope) {
        if (envelope == null || envelope.eventId() == null) {
            log.warn("Received null envelope or null eventId in MaintenanceInboxService");
            return InboxResult.CONFLICT;
        }

        String incomingHash = EnvelopeFingerprint.computeSha256(envelope);
        String source = (envelope.source() != null && !envelope.source().isBlank())
                ? envelope.source().trim()
                : "UNKNOWN";

        int inserted = inboxRepository.insertOnConflictDoNothing(
                envelope.eventId(),
                envelope.eventType(),
                source,
                envelope.aggregateId(),
                envelope.correlationId(),
                incomingHash
        );

        if (inserted == 1) {
            return InboxResult.PROCEED;
        }

        Optional<MaintenanceInboxEventEntity> existingOpt = inboxRepository.findById(envelope.eventId());
        if (existingOpt.isPresent()) {
            MaintenanceInboxEventEntity existing = existingOpt.get();
            if (existing.getEnvelopeHash().equals(incomingHash)) {
                log.info("Durable inbox duplicate detected for eventId {} ({}) from {}. Skipping business mutation.",
                        envelope.eventId(), envelope.eventType(), source);
                return InboxResult.DUPLICATE;
            } else {
                log.error("DATA INCONSISTENCY: Conflicting payload received for existing eventId {}! Existing hash={}, incoming hash={}. Rejecting without mutation.",
                        envelope.eventId(), existing.getEnvelopeHash(), incomingHash);
                return InboxResult.CONFLICT;
            }
        }

        return InboxResult.DUPLICATE;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordConsumedEvent(EventEnvelope<?> envelope) {
        if (envelope == null || envelope.eventId() == null) {
            return;
        }
        String incomingHash = EnvelopeFingerprint.computeSha256(envelope);
        String source = (envelope.source() != null && !envelope.source().isBlank())
                ? envelope.source().trim()
                : "UNKNOWN";
        inboxRepository.insertOnConflictDoNothing(
                envelope.eventId(),
                envelope.eventType(),
                source,
                envelope.aggregateId(),
                envelope.correlationId(),
                incomingHash
        );
        log.info("Recorded consumed eventId {} in inbox during handled conflict resolution", envelope.eventId());
    }
}
