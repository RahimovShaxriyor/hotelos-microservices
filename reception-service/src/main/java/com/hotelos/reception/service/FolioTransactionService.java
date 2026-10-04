package com.hotelos.reception.service;

import com.hotelos.common.event.payload.RoomServiceChargePayload;
import com.hotelos.reception.domain.FolioEntryType;
import com.hotelos.reception.domain.FolioSourceType;
import com.hotelos.reception.domain.FolioStatus;
import com.hotelos.reception.dto.CreateAdjustmentRequest;
import com.hotelos.reception.dto.FolioEntryResponse;
import com.hotelos.reception.exception.ConflictException;
import com.hotelos.reception.exception.HotelValidationException;
import com.hotelos.reception.exception.ResourceNotFoundException;
import com.hotelos.reception.persistence.entity.FolioEntity;
import com.hotelos.reception.persistence.entity.FolioEntryEntity;
import com.hotelos.reception.persistence.entity.GuestStayEntity;
import com.hotelos.reception.persistence.repository.FolioEntryRepository;
import com.hotelos.reception.persistence.repository.FolioRepository;
import com.hotelos.reception.persistence.repository.GuestStayRepository;
import com.hotelos.reception.persistence.repository.RoomServiceChargeRepository;
import com.hotelos.reception.persistence.repository.StayRoomHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.hotelos.common.event.payload.RoomServiceChargePayloadV2;
import com.hotelos.reception.dto.RoomServiceChargeItem;
import org.springframework.beans.factory.annotation.Value;

@Service
public class FolioTransactionService {
    private static final Logger log = LoggerFactory.getLogger(FolioTransactionService.class);

    private final FolioRepository folioRepository;
    private final FolioEntryRepository folioEntryRepository;
    private final GuestStayRepository guestStayRepository;
    private final StayRoomHistoryRepository stayRoomHistoryRepository;
    private final RoomServiceChargeRepository roomServiceChargeRepository;
    private final boolean strictBarrier;

    public FolioTransactionService(
            FolioRepository folioRepository,
            FolioEntryRepository folioEntryRepository,
            GuestStayRepository guestStayRepository,
            StayRoomHistoryRepository stayRoomHistoryRepository,
            RoomServiceChargeRepository roomServiceChargeRepository,
            @Value("${hotelos.reception.strict-room-service-barrier:${HOTELOS_RECEPTION_STRICT_ROOM_SERVICE_BARRIER:true}}") boolean strictBarrier
    ) {
        this.folioRepository = folioRepository;
        this.folioEntryRepository = folioEntryRepository;
        this.guestStayRepository = guestStayRepository;
        this.stayRoomHistoryRepository = stayRoomHistoryRepository;
        this.roomServiceChargeRepository = roomServiceChargeRepository;
        this.strictBarrier = strictBarrier;
    }

    public enum ChargeResult {
        APPLIED,
        DUPLICATE,
        UNRESOLVED,
        CURRENCY_MISMATCH,
        REJECTED_POST_CHECKOUT,
        AMBIGUOUS_STAY_MATCH
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public FolioEntity createInitialFolio(GuestStayEntity stay) {
        Optional<FolioEntity> existing = folioRepository.findByStayId(stay.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        FolioEntity folio = new FolioEntity(
                UUID.randomUUID(),
                stay.getId(),
                stay.getCurrency(),
                FolioStatus.OPEN,
                null,
                stay.getCheckedInAt(),
                stay.getCheckedInAt()
        );
        folioRepository.save(folio);

        int nights = Math.max(1, stay.getBookedNights());
        BigDecimal totalRoomCharge = stay.getNightlyRateAtCheckin()
                .multiply(BigDecimal.valueOf(nights))
                .setScale(2, RoundingMode.HALF_UP);

        if (totalRoomCharge.compareTo(BigDecimal.ZERO) > 0) {
            String sourceId = "INITIAL_ROOM_CHARGE_" + stay.getId();
            Optional<FolioEntryEntity> existingEntry = folioEntryRepository.findByFolioIdAndSourceTypeAndSourceId(
                    folio.getId(), FolioSourceType.SYSTEM, sourceId);
            if (existingEntry.isEmpty()) {
                FolioEntryEntity roomCharge = new FolioEntryEntity(
                        UUID.randomUUID(),
                        folio.getId(),
                        folio.getCurrency(),
                        FolioEntryType.ROOM_CHARGE,
                        FolioSourceType.SYSTEM,
                        sourceId,
                        totalRoomCharge,
                        "Initial room charge for stay " + stay.getId(),
                        null,
                        "SYSTEM",
                        stay.getCheckedInAt(),
                        stay.getCheckedInAt()
                );
                folioEntryRepository.save(roomCharge);
            }
        }

        return folio;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordExtensionCharge(UUID stayId, LocalDate newDepartureDate, int extraNights, BigDecimal nightlyRate, Instant now) {
        if (extraNights <= 0) {
            return;
        }

        FolioEntity folio = folioRepository.findByStayIdForUpdate(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Folio not found for stay: " + stayId));

        BigDecimal chargeAmount = nightlyRate
                .multiply(BigDecimal.valueOf(extraNights))
                .setScale(2, RoundingMode.HALF_UP);

        if (chargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String sourceId = "EXTENSION_" + stayId + "_" + newDepartureDate;
        Optional<FolioEntryEntity> existing = folioEntryRepository.findByFolioIdAndSourceTypeAndSourceId(
                folio.getId(), FolioSourceType.SYSTEM, sourceId);
        if (existing.isPresent()) {
            log.info("Extension charge {} already exists for folio {}", sourceId, folio.getId());
            return;
        }

        FolioEntryEntity extensionCharge = new FolioEntryEntity(
                UUID.randomUUID(),
                folio.getId(),
                folio.getCurrency(),
                FolioEntryType.ROOM_CHARGE,
                FolioSourceType.SYSTEM,
                sourceId,
                chargeAmount,
                "Extension charge for " + extraNights + " night(s) until " + newDepartureDate,
                null,
                "SYSTEM",
                now,
                now
        );
        folioEntryRepository.save(extensionCharge);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEarlyCheckoutCredit(UUID stayId, int unusedNights, BigDecimal nightlyRate, Instant checkoutTime) {
        if (unusedNights <= 0) {
            return;
        }

        FolioEntity folio = folioRepository.findByStayIdForUpdate(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Folio not found for stay: " + stayId));

        BigDecimal creditAmount = nightlyRate
                .multiply(BigDecimal.valueOf(unusedNights))
                .setScale(2, RoundingMode.HALF_UP)
                .negate();

        String sourceId = "EARLY_CHECKOUT_" + stayId;
        Optional<FolioEntryEntity> existing = folioEntryRepository.findByFolioIdAndSourceTypeAndSourceId(
                folio.getId(), FolioSourceType.SYSTEM, sourceId);
        if (existing.isPresent()) {
            log.info("Early checkout credit {} already exists for folio {}", sourceId, folio.getId());
            return;
        }

        FolioEntryEntity earlyCheckoutCredit = new FolioEntryEntity(
                UUID.randomUUID(),
                folio.getId(),
                folio.getCurrency(),
                FolioEntryType.ADJUSTMENT,
                FolioSourceType.SYSTEM,
                sourceId,
                creditAmount,
                "Early checkout credit for " + unusedNights + " unused night(s)",
                null,
                "SYSTEM",
                checkoutTime,
                checkoutTime
        );
        folioEntryRepository.save(earlyCheckoutCredit);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reconcileRoomServiceCharges(UUID stayId, List<RoomServiceChargeItem> charges) {
        if (charges == null || charges.isEmpty()) {
            return;
        }

        FolioEntity folio = folioRepository.findByStayIdForUpdate(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Folio not found for stay: " + stayId));

        GuestStayEntity stay = guestStayRepository.findById(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Stay not found: " + stayId));

        for (RoomServiceChargeItem charge : charges) {
            String orderId = charge.orderId();
            if (orderId == null || orderId.isBlank()) {
                continue;
            }

            if (folioEntryRepository.findBySourceTypeAndSourceId(FolioSourceType.ROOM_SERVICE_ORDER, orderId).isPresent()) {
                continue;
            }

            String chargeCurrency = (charge.currency() != null && !charge.currency().isBlank())
                    ? charge.currency().trim().toUpperCase()
                    : stay.getCurrency();

            if (!folio.getCurrency().equalsIgnoreCase(chargeCurrency)) {
                log.warn("FOLIO_CURRENCY_MISMATCH during reconciliation for stay {}, order {}: folio {}, charge {}",
                        stayId, orderId, folio.getCurrency(), chargeCurrency);
                throw new ConflictException("FOLIO_CURRENCY_MISMATCH",
                        "Charge currency " + chargeCurrency + " does not match folio currency " + folio.getCurrency());
            }

            Instant chargeTime = charge.chargedAt() != null ? charge.chargedAt() : Instant.now();
            BigDecimal amount = charge.amount().setScale(2, RoundingMode.HALF_UP);

            FolioEntryEntity entry = new FolioEntryEntity(
                    UUID.randomUUID(),
                    folio.getId(),
                    folio.getCurrency(),
                    FolioEntryType.ROOM_SERVICE,
                    FolioSourceType.ROOM_SERVICE_ORDER,
                    orderId,
                    amount,
                    "Room service order " + orderId,
                    null,
                    "SYSTEM",
                    chargeTime,
                    Instant.now()
            );

            try {
                folioEntryRepository.saveAndFlush(entry);
            } catch (DataIntegrityViolationException ex) {
                log.info("Concurrent insert caught during reconciliation for orderId: {}", orderId);
            }

            try {
                roomServiceChargeRepository.insertChargeOnConflictDoNothing(
                        UUID.randomUUID(),
                        orderId,
                        stay.getId(),
                        amount,
                        chargeCurrency,
                        UUID.randomUUID(),
                        chargeTime
                );
            } catch (Exception ex) {
                log.warn("Compatibility insert into room_service_charges skipped/failed during reconciliation: {}", ex.getMessage());
            }
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public FolioEntity closeFolio(UUID stayId, Instant checkoutTime) {
        FolioEntity folio = folioRepository.findByStayIdForUpdate(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Folio not found for stay: " + stayId));

        if (folio.isClosed()) {
            return folio;
        }

        folio.close(checkoutTime);
        return folioRepository.save(folio);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public ChargeResult processRoomServiceCharge(RoomServiceChargePayload payload, Instant eventOccurredAt, UUID eventId) {
        Instant chargeTime = payload.chargedAt() != null ? payload.chargedAt() : eventOccurredAt;
        if (chargeTime == null) {
            chargeTime = Instant.now();
        }

        String orderId = payload.orderId();
        // Check global deduplication for room service order
        Optional<FolioEntryEntity> existingRsEntry = folioEntryRepository.findBySourceTypeAndSourceId(
                FolioSourceType.ROOM_SERVICE_ORDER, orderId);
        if (existingRsEntry.isPresent()) {
            log.info("Room service order {} already ingested, skipping duplicate", orderId);
            return ChargeResult.DUPLICATE;
        }

        // Half-open interval resolution on stay_room_history: [started_at, ended_at)
        List<UUID> candidateStayIds = stayRoomHistoryRepository.findStayIdsForRoomAtTime(payload.roomNumber(), chargeTime);
        if (candidateStayIds.isEmpty()) {
            log.warn("UNRESOLVED_STAY_AT_TIMESTAMP: No stay found for room {} at charge timestamp {}",
                    payload.roomNumber(), chargeTime);
            return ChargeResult.UNRESOLVED;
        }
        if (candidateStayIds.size() > 1) {
            log.warn("AMBIGUOUS_STAY_MATCH: Multiple stays ({}) matched for room {} at charge timestamp {}",
                    candidateStayIds.size(), payload.roomNumber(), chargeTime);
            return ChargeResult.AMBIGUOUS_STAY_MATCH;
        }

        UUID stayId = candidateStayIds.get(0);

        // Level 4: Lock and fresh re-read of Stay
        GuestStayEntity stay = guestStayRepository.findByIdForUpdate(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Stay missing: " + stayId));

        // Level 6: Lock Folio
        FolioEntity folio = folioRepository.findByStayIdForUpdate(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Folio missing for stay: " + stayId));

        String chargeCurrency = (payload.currency() != null && !payload.currency().isBlank())
                ? payload.currency().trim().toUpperCase()
                : stay.getCurrency();

        // Single currency check
        if (!folio.getCurrency().equalsIgnoreCase(chargeCurrency)) {
            log.warn("FOLIO_CURRENCY_MISMATCH: Folio currency {} does not match charge currency {}",
                    folio.getCurrency(), chargeCurrency);
            return ChargeResult.CURRENCY_MISMATCH;
        }

        // Check closed folio semantics & late charge boundaries
        if (folio.isClosed()) {
            if (folioEntryRepository.findBySourceTypeAndSourceId(FolioSourceType.ROOM_SERVICE_ORDER, orderId).isPresent()) {
                return ChargeResult.DUPLICATE;
            }
            if (strictBarrier) {
                log.warn("REJECT_FINALIZED_FOLIO: CLOSED folio {} rejected unexpected V1 charge for order {}", folio.getId(), orderId);
                return ChargeResult.REJECTED_POST_CHECKOUT;
            }
            Instant checkoutTime = stay.getCheckedOutAt();
            if (checkoutTime != null && !chargeTime.isBefore(checkoutTime)) {
                // chargedAt >= checkedOutAt: including exact boundary chargedAt == checkedOutAt
                log.warn("REJECTED_POST_CHECKOUT: Order {} charged at {} is on/after checkout time {}",
                        orderId, chargeTime, checkoutTime);
                return ChargeResult.REJECTED_POST_CHECKOUT;
            }
            log.info("Accepting legitimate delayed pre-checkout charge for order {} into CLOSED folio {}",
                    orderId, folio.getId());
        }

        // Check duplicate again under lock
        if (folioEntryRepository.findBySourceTypeAndSourceId(FolioSourceType.ROOM_SERVICE_ORDER, orderId).isPresent()) {
            return ChargeResult.DUPLICATE;
        }

        // Ingest into folio_entries
        FolioEntryEntity entry = new FolioEntryEntity(
                UUID.randomUUID(),
                folio.getId(),
                folio.getCurrency(),
                FolioEntryType.ROOM_SERVICE,
                FolioSourceType.ROOM_SERVICE_ORDER,
                orderId,
                payload.amount().setScale(2, RoundingMode.HALF_UP),
                "Room service order " + orderId,
                null,
                "SYSTEM",
                chargeTime,
                Instant.now()
        );
        try {
            folioEntryRepository.saveAndFlush(entry);
        } catch (DataIntegrityViolationException ex) {
            log.info("Concurrent insert caught by uq_folio_entries_room_service_order for orderId: {}", orderId);
            return ChargeResult.DUPLICATE;
        }

        // Maintain room_service_charges table for compatibility
        try {
            roomServiceChargeRepository.insertChargeOnConflictDoNothing(
                    UUID.randomUUID(),
                    orderId,
                    stay.getId(),
                    payload.amount().setScale(2, RoundingMode.HALF_UP),
                    chargeCurrency,
                    eventId,
                    chargeTime
            );
        } catch (Exception ex) {
            log.warn("Compatibility insert into room_service_charges skipped/failed: {}", ex.getMessage());
        }

        log.info("Applied room service charge of {} {} for order {} to folio {}",
                payload.amount(), chargeCurrency, orderId, folio.getId());
        return ChargeResult.APPLIED;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public ChargeResult processRoomServiceChargeV2(RoomServiceChargePayloadV2 payload, Instant eventOccurredAt, UUID eventId) {
        String orderId = payload.orderId();
        if (orderId == null || orderId.isBlank() || payload.stayId() == null) {
            log.warn("Received room service charge V2 missing orderId or stayId: {}", payload);
            return ChargeResult.UNRESOLVED;
        }

        Instant chargeTime = payload.chargedAt() != null ? payload.chargedAt() : eventOccurredAt;
        if (chargeTime == null) {
            chargeTime = Instant.now();
        }

        // Global deduplication check for room service order
        if (folioEntryRepository.findBySourceTypeAndSourceId(FolioSourceType.ROOM_SERVICE_ORDER, orderId).isPresent()) {
            log.info("Room service order {} already ingested, skipping duplicate V2", orderId);
            return ChargeResult.DUPLICATE;
        }

        UUID stayId = payload.stayId();
        Optional<GuestStayEntity> stayOpt = guestStayRepository.findByIdForUpdate(stayId);
        if (stayOpt.isEmpty()) {
            log.warn("UNRESOLVED_STAY: Stay {} not found for V2 charge order {}", stayId, orderId);
            return ChargeResult.UNRESOLVED;
        }
        GuestStayEntity stay = stayOpt.get();

        if (payload.roomNumber() != null && !payload.roomNumber().equalsIgnoreCase(stay.getRoomNumber())) {
            log.info("HISTORICAL_ROOM_MISMATCH: Order {} was created for historical room {} but stay {} is currently in room {}",
                    orderId, payload.roomNumber(), stayId, stay.getRoomNumber());
        }

        Optional<FolioEntity> folioOpt = folioRepository.findByStayIdForUpdate(stayId);
        if (folioOpt.isEmpty()) {
            log.warn("UNRESOLVED_FOLIO: Folio not found for stay {} V2 charge order {}", stayId, orderId);
            return ChargeResult.UNRESOLVED;
        }
        FolioEntity folio = folioOpt.get();

        String chargeCurrency = (payload.currency() != null && !payload.currency().isBlank())
                ? payload.currency().trim().toUpperCase()
                : stay.getCurrency();

        if (!folio.getCurrency().equalsIgnoreCase(chargeCurrency)) {
            log.warn("FOLIO_CURRENCY_MISMATCH: Folio currency {} does not match V2 charge currency {}",
                    folio.getCurrency(), chargeCurrency);
            return ChargeResult.CURRENCY_MISMATCH;
        }

        // Closed folio semantics:
        if (folio.isClosed()) {
            if (folioEntryRepository.findBySourceTypeAndSourceId(FolioSourceType.ROOM_SERVICE_ORDER, orderId).isPresent()) {
                return ChargeResult.DUPLICATE;
            }
            if (strictBarrier) {
                log.warn("REJECT_FINALIZED_FOLIO: CLOSED folio {} rejected V2 charge for order {}", folio.getId(), orderId);
                return ChargeResult.REJECTED_POST_CHECKOUT;
            } else {
                Instant checkoutTime = stay.getCheckedOutAt();
                if (checkoutTime != null && !chargeTime.isBefore(checkoutTime)) {
                    log.warn("REJECTED_POST_CHECKOUT: Order {} charged at {} is on/after checkout time {}",
                            orderId, chargeTime, checkoutTime);
                    return ChargeResult.REJECTED_POST_CHECKOUT;
                }
            }
        }

        // Check duplicate again under lock
        if (folioEntryRepository.findBySourceTypeAndSourceId(FolioSourceType.ROOM_SERVICE_ORDER, orderId).isPresent()) {
            return ChargeResult.DUPLICATE;
        }

        BigDecimal amount = payload.amount().setScale(2, RoundingMode.HALF_UP);
        FolioEntryEntity entry = new FolioEntryEntity(
                UUID.randomUUID(),
                folio.getId(),
                folio.getCurrency(),
                FolioEntryType.ROOM_SERVICE,
                FolioSourceType.ROOM_SERVICE_ORDER,
                orderId,
                amount,
                "Room service order " + orderId,
                null,
                "SYSTEM",
                chargeTime,
                Instant.now()
        );

        try {
            folioEntryRepository.saveAndFlush(entry);
        } catch (DataIntegrityViolationException ex) {
            log.info("Concurrent insert caught by uq_folio_entries_room_service_order for orderId: {}", orderId);
            return ChargeResult.DUPLICATE;
        }

        try {
            roomServiceChargeRepository.insertChargeOnConflictDoNothing(
                    UUID.randomUUID(),
                    orderId,
                    stay.getId(),
                    amount,
                    chargeCurrency,
                    eventId,
                    chargeTime
            );
        } catch (Exception ex) {
            log.warn("Compatibility insert into room_service_charges skipped/failed: {}", ex.getMessage());
        }

        log.info("Applied room service charge V2 of {} {} for order {} to folio {}",
                amount, chargeCurrency, orderId, folio.getId());
        return ChargeResult.APPLIED;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public FolioEntryResponse createManualAdjustment(
            UUID folioId,
            CreateAdjustmentRequest request,
            String idempotencyKey,
            String authenticatedUser
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new HotelValidationException("Idempotency-Key header is required for adjustments");
        }

        // Lock Folio (Level 6)
        FolioEntity folio = folioRepository.findByIdForUpdate(folioId)
                .orElseThrow(() -> new ResourceNotFoundException("Folio not found: " + folioId));

        if (folio.isClosed()) {
            throw new ConflictException("FOLIO_CLOSED", "Manual adjustments are forbidden on closed folios");
        }

        FolioEntryType entryType = request.entryType();
        if (entryType == FolioEntryType.ROOM_CHARGE || entryType == FolioEntryType.ROOM_SERVICE) {
            throw new HotelValidationException("Manual entry cannot have type " + entryType);
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        validateAmountSign(entryType, amount);

        String normalizedDescription = request.description().trim();
        String fingerprint = calculateFingerprint(folioId, entryType, amount, normalizedDescription);

        Optional<FolioEntryEntity> existing = folioEntryRepository.findByFolioIdAndSourceTypeAndSourceId(
                folioId, FolioSourceType.MANUAL, idempotencyKey);

        if (existing.isPresent()) {
            FolioEntryEntity entry = existing.get();
            if (fingerprint.equals(entry.getIdempotencyFingerprint())) {
                log.info("Idempotent replay for manual adjustment on folio {} with key {}", folioId, idempotencyKey);
                return FolioEntryResponse.from(entry);
            } else {
                log.warn("Idempotency conflict for key {} on folio {}: stored fingerprint={}, request fingerprint={}",
                        idempotencyKey, folioId, entry.getIdempotencyFingerprint(), fingerprint);
                throw new ConflictException("IDEMPOTENCY_CONFLICT",
                        "An adjustment with this Idempotency-Key already exists with different request parameters");
            }
        }

        String actor = (authenticatedUser != null && !authenticatedUser.isBlank()) ? authenticatedUser : "SYSTEM";

        FolioEntryEntity newEntry = new FolioEntryEntity(
                UUID.randomUUID(),
                folio.getId(),
                folio.getCurrency(),
                entryType,
                FolioSourceType.MANUAL,
                idempotencyKey,
                amount,
                normalizedDescription,
                fingerprint,
                actor,
                Instant.now(),
                Instant.now()
        );

        try {
            FolioEntryEntity saved = folioEntryRepository.saveAndFlush(newEntry);
            log.info("Created manual adjustment {} of {} {} on folio {} by {}",
                    saved.getId(), amount, folio.getCurrency(), folioId, actor);
            return FolioEntryResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            Optional<FolioEntryEntity> raceEntry = folioEntryRepository.findByFolioIdAndSourceTypeAndSourceId(
                    folioId, FolioSourceType.MANUAL, idempotencyKey);
            if (raceEntry.isPresent()) {
                if (fingerprint.equals(raceEntry.get().getIdempotencyFingerprint())) {
                    log.info("Idempotent replay on unique index race for folio {} key {}", folioId, idempotencyKey);
                    return FolioEntryResponse.from(raceEntry.get());
                } else {
                    log.warn("Idempotency conflict on unique index race for key {} on folio {}", idempotencyKey, folioId);
                    throw new ConflictException("IDEMPOTENCY_CONFLICT",
                            "An adjustment with this Idempotency-Key already exists with different request parameters");
                }
            }
            throw ex;
        }
    }

    private void validateAmountSign(FolioEntryType type, BigDecimal amount) {
        switch (type) {
            case MINIBAR, LATE_CHECKOUT -> {
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new HotelValidationException(type.name() + " amount must be strictly positive");
                }
            }
            case DISCOUNT -> {
                if (amount.compareTo(BigDecimal.ZERO) >= 0) {
                    throw new HotelValidationException("DISCOUNT amount must be strictly negative");
                }
            }
            case ADJUSTMENT -> {
                if (amount.compareTo(BigDecimal.ZERO) == 0) {
                    throw new HotelValidationException("ADJUSTMENT amount cannot be zero");
                }
            }
            default -> throw new HotelValidationException("Unsupported entry type for manual adjustment: " + type);
        }
    }

    private String calculateFingerprint(UUID folioId, FolioEntryType entryType, BigDecimal amount, String description) {
        String normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String normalizedDesc = description.trim();
        String canonical = "v1:" + folioId + ":" + entryType.name() + ":" + normalizedAmount + ":" + normalizedDesc;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm missing", e);
        }
    }
}
