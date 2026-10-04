package com.hotelos.roomservice.service;

import com.hotelos.roomservice.domain.OrderingSessionStatus;
import com.hotelos.roomservice.dto.FreezeStayResponse;
import com.hotelos.roomservice.dto.RoomServiceChargeItem;
import com.hotelos.roomservice.exception.ConflictException;
import com.hotelos.roomservice.exception.ResourceNotFoundException;
import com.hotelos.roomservice.persistence.entity.RoomServiceOrderEntity;
import com.hotelos.roomservice.persistence.entity.StayOrderingSessionEntity;
import com.hotelos.roomservice.persistence.repository.OrderRepository;
import com.hotelos.roomservice.persistence.repository.StayOrderingSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class StayOrderingSessionService {

    private static final Logger log = LoggerFactory.getLogger(StayOrderingSessionService.class);

    private final StayOrderingSessionRepository sessionRepository;
    private final OrderRepository orderRepository;

    public StayOrderingSessionService(
            StayOrderingSessionRepository sessionRepository,
            OrderRepository orderRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public FreezeStayResponse freezeStay(UUID stayId) {
        if (stayId == null) {
            throw new IllegalArgumentException("stayId is required");
        }

        // Canonical ensure pattern
        sessionRepository.ensureSessionRow(stayId);

        StayOrderingSessionEntity session = sessionRepository.findByStayIdForUpdate(stayId)
                .orElseThrow(() -> new IllegalStateException("Failed to lock stay ordering session: " + stayId));

        if (session.getStatus() == OrderingSessionStatus.CLOSED) {
            log.warn("Attempt to freeze already finalized stay: {}", stayId);
            throw new ConflictException("STAY_ALREADY_FINALIZED", "Stay " + stayId + " ordering session is already closed");
        }

        if (session.getStatus() == OrderingSessionStatus.FROZEN) {
            log.info("Stay {} is already FROZEN with freezeId {}, returning idempotent snapshot", stayId, session.getFreezeId());
            return buildSnapshot(session);
        }

        // Status is ACTIVE: check for pending orders
        List<RoomServiceOrderEntity> pendingOrders = orderRepository.findPendingOrdersByStayId(stayId);
        if (!pendingOrders.isEmpty()) {
            log.warn("Cannot freeze stay {}: found {} pending orders", stayId, pendingOrders.size());
            throw new ConflictException("PENDING_ROOM_SERVICE_ORDERS",
                    "Cannot freeze stay ordering session with pending room service orders");
        }

        UUID freezeId = UUID.randomUUID();
        Instant now = Instant.now();
        session.freeze(freezeId, now);
        sessionRepository.save(session);

        log.info("Successfully froze stay ordering session {} with freezeId {}", stayId, freezeId);
        return buildSnapshot(session);
    }

    @Transactional
    public void finalizeStay(UUID stayId, UUID freezeId) {
        if (stayId == null) {
            throw new IllegalArgumentException("stayId is required");
        }
        if (freezeId == null) {
            throw new IllegalArgumentException("freezeId is required");
        }

        StayOrderingSessionEntity session = sessionRepository.findByStayIdForUpdate(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("STAY_ORDERING_SESSION_NOT_FOUND",
                        "Stay ordering session not found: " + stayId));

        if (session.getStatus() == OrderingSessionStatus.ACTIVE) {
            log.warn("Cannot finalize ACTIVE session for stay {}", stayId);
            throw new ConflictException("INVALID_STAY_LIFECYCLE", "Cannot finalize stay in ACTIVE status");
        }

        if (session.getStatus() == OrderingSessionStatus.FROZEN) {
            if (!freezeId.equals(session.getFreezeId())) {
                log.warn("FreezeId mismatch for stay {}: expected {}, received {}", stayId, session.getFreezeId(), freezeId);
                throw new ConflictException("FREEZE_ID_MISMATCH", "Supplied freezeId does not match session freezeId");
            }
            session.close(Instant.now());
            sessionRepository.save(session);
            log.info("Finalized stay {} ordering session with freezeId {}", stayId, freezeId);
            return;
        }

        if (session.getStatus() == OrderingSessionStatus.CLOSED) {
            if (!freezeId.equals(session.getFreezeId())) {
                log.warn("FreezeId mismatch for closed stay {}: expected {}, received {}", stayId, session.getFreezeId(), freezeId);
                throw new ConflictException("FREEZE_ID_MISMATCH", "Supplied freezeId does not match session freezeId");
            }
            log.info("Idempotent finalize for already closed stay {} with freezeId {}", stayId, freezeId);
        }
    }

    @Transactional
    public void unfreezeStay(UUID stayId, UUID freezeId) {
        if (stayId == null) {
            throw new IllegalArgumentException("stayId is required");
        }
        if (freezeId == null) {
            throw new IllegalArgumentException("freezeId is required");
        }

        StayOrderingSessionEntity session = sessionRepository.findByStayIdForUpdate(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("STAY_ORDERING_SESSION_NOT_FOUND",
                        "Stay ordering session not found: " + stayId));

        if (session.getStatus() == OrderingSessionStatus.CLOSED) {
            log.warn("Cannot unfreeze closed stay {}", stayId);
            throw new ConflictException("CANNOT_UNFREEZE_CLOSED_STAY", "Cannot unfreeze stay in CLOSED status");
        }

        if (session.getStatus() == OrderingSessionStatus.ACTIVE) {
            log.info("Idempotent unfreeze for already active stay {}", stayId);
            return;
        }

        if (session.getStatus() == OrderingSessionStatus.FROZEN) {
            if (!freezeId.equals(session.getFreezeId())) {
                log.warn("FreezeId mismatch during unfreeze for stay {}: expected {}, received {}",
                        stayId, session.getFreezeId(), freezeId);
                throw new ConflictException("FREEZE_ID_MISMATCH", "Supplied freezeId does not match session freezeId");
            }
            session.unfreeze(Instant.now());
            sessionRepository.save(session);
            log.info("Successfully unfroze stay ordering session {} with freezeId {}", stayId, freezeId);
        }
    }

    private FreezeStayResponse buildSnapshot(StayOrderingSessionEntity session) {
        List<RoomServiceOrderEntity> deliveredOrders = orderRepository.findDeliveredOrdersByStayIdWithItems(session.getStayId());

        List<RoomServiceChargeItem> charges = deliveredOrders.stream()
                .sorted(Comparator.comparing(RoomServiceOrderEntity::getDeliveredAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RoomServiceOrderEntity::getId))
                .map(o -> new RoomServiceChargeItem(
                        o.getId().toString(),
                        o.getRoomNumber(),
                        OrderCalculationHelper.calculateOrderTotal(o),
                        o.getCurrency(),
                        o.getDeliveredAt()
                ))
                .toList();

        BigDecimal total = charges.stream()
                .map(RoomServiceChargeItem::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new FreezeStayResponse(
                session.getStayId(),
                session.getStatus(),
                session.getFreezeId(),
                session.getFrozenAt(),
                charges,
                total
        );
    }
}
