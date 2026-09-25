package com.hotelos.housekeeping.inbox;

import com.hotelos.common.event.EnvelopeFingerprint;
import com.hotelos.common.event.EventEnvelope;
import com.hotelos.housekeeping.persistence.entity.HousekeepingInboxEventEntity;
import com.hotelos.housekeeping.persistence.repository.HousekeepingInboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class HousekeepingInboxService {
    private static final Logger log = LoggerFactory.getLogger(HousekeepingInboxService.class);

    public enum InboxResult {
        PROCEED,
        DUPLICATE,
        CONFLICT
    }

    private final HousekeepingInboxRepository inboxRepository;

    public HousekeepingInboxService(HousekeepingInboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public InboxResult registerEvent(EventEnvelope<?> envelope) {
        if (envelope == null || envelope.eventId() == null) {
            log.warn("Received null envelope or null eventId in HousekeepingInboxService");
            return InboxResult.CONFLICT;
        }

        String incomingHash = EnvelopeFingerprint.computeSha256(envelope);

        int inserted = inboxRepository.insertOnConflictDoNothing(
                envelope.eventId(),
                envelope.eventType(),
                envelope.source(),
                envelope.aggregateId(),
                envelope.correlationId(),
                incomingHash
        );

        if (inserted == 1) {
            return InboxResult.PROCEED;
        }

        Optional<HousekeepingInboxEventEntity> existingOpt = inboxRepository.findById(envelope.eventId());
        if (existingOpt.isPresent()) {
            HousekeepingInboxEventEntity existing = existingOpt.get();
            if (existing.getEnvelopeHash().equals(incomingHash)) {
                log.info("Durable inbox duplicate detected for eventId {} ({}) from {}. Skipping business mutation.",
                        envelope.eventId(), envelope.eventType(), envelope.source());
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
        inboxRepository.insertOnConflictDoNothing(
                envelope.eventId(),
                envelope.eventType(),
                envelope.source(),
                envelope.aggregateId(),
                envelope.correlationId(),
                incomingHash
        );
        log.info("Recorded consumed eventId {} in inbox during handled conflict resolution", envelope.eventId());
    }
}
