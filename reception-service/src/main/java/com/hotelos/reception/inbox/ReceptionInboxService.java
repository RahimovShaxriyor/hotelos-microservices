package com.hotelos.reception.inbox;

import com.hotelos.common.event.EnvelopeFingerprint;
import com.hotelos.common.event.EventEnvelope;
import com.hotelos.reception.persistence.entity.ReceptionInboxEventEntity;
import com.hotelos.reception.persistence.repository.ReceptionInboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ReceptionInboxService {
    private static final Logger log = LoggerFactory.getLogger(ReceptionInboxService.class);

    public enum InboxResult {
        PROCEED,
        DUPLICATE,
        CONFLICT
    }

    private final ReceptionInboxRepository inboxRepository;

    public ReceptionInboxService(ReceptionInboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public InboxResult registerEvent(EventEnvelope<?> envelope) {
        if (envelope == null || envelope.eventId() == null) {
            log.warn("Received null envelope or null eventId in ReceptionInboxService");
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

        Optional<ReceptionInboxEventEntity> existingOpt = inboxRepository.findById(envelope.eventId());
        if (existingOpt.isPresent()) {
            ReceptionInboxEventEntity existing = existingOpt.get();
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
}
