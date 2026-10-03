package com.hotelos.reception.service;

import com.hotelos.reception.domain.FolioEntryType;
import com.hotelos.reception.dto.CreateAdjustmentRequest;
import com.hotelos.reception.dto.FolioEntryResponse;
import com.hotelos.reception.dto.FolioResponse;
import com.hotelos.reception.exception.ResourceNotFoundException;
import com.hotelos.reception.persistence.entity.FolioEntity;
import com.hotelos.reception.persistence.repository.FolioEntryRepository;
import com.hotelos.reception.persistence.repository.FolioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class FolioService {

    private final FolioRepository folioRepository;
    private final FolioEntryRepository folioEntryRepository;
    private final FolioTransactionService folioTransactionService;

    public FolioService(
            FolioRepository folioRepository,
            FolioEntryRepository folioEntryRepository,
            FolioTransactionService folioTransactionService
    ) {
        this.folioRepository = folioRepository;
        this.folioEntryRepository = folioEntryRepository;
        this.folioTransactionService = folioTransactionService;
    }

    public FolioResponse getFolioByStayId(UUID stayId) {
        FolioEntity folio = folioRepository.findByStayId(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Folio not found for stay: " + stayId));
        BigDecimal balance = folioEntryRepository.calculateBalanceByFolioId(folio.getId());
        return FolioResponse.from(folio, balance);
    }

    public FolioResponse getFolioById(UUID folioId) {
        FolioEntity folio = folioRepository.findById(folioId)
                .orElseThrow(() -> new ResourceNotFoundException("Folio not found: " + folioId));
        BigDecimal balance = folioEntryRepository.calculateBalanceByFolioId(folio.getId());
        return FolioResponse.from(folio, balance);
    }

    public Page<FolioEntryResponse> getFolioEntries(UUID folioId, FolioEntryType entryType, Pageable pageable) {
        // Verify folio exists
        if (!folioRepository.existsById(folioId)) {
            throw new ResourceNotFoundException("Folio not found: " + folioId);
        }

        if (entryType != null) {
            return folioEntryRepository.findByFolioIdAndEntryType(folioId, entryType, pageable)
                    .map(FolioEntryResponse::from);
        }
        return folioEntryRepository.findByFolioId(folioId, pageable)
                .map(FolioEntryResponse::from);
    }

    public FolioEntryResponse createAdjustment(
            UUID folioId,
            CreateAdjustmentRequest request,
            String idempotencyKey,
            String authenticatedUser
    ) {
        return folioTransactionService.createManualAdjustment(folioId, request, idempotencyKey, authenticatedUser);
    }

    public BigDecimal getBalance(UUID folioId) {
        return folioEntryRepository.calculateBalanceByFolioId(folioId);
    }
}
