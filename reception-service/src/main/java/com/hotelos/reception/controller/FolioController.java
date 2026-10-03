package com.hotelos.reception.controller;

import com.hotelos.reception.domain.FolioEntryType;
import com.hotelos.reception.dto.CreateAdjustmentRequest;
import com.hotelos.reception.dto.FolioEntryResponse;
import com.hotelos.reception.dto.FolioResponse;
import com.hotelos.reception.service.FolioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reception")
public class FolioController {

    private final FolioService folioService;

    public FolioController(FolioService folioService) {
        this.folioService = folioService;
    }

    @GetMapping("/stays/{stayId}/folio")
    public FolioResponse getFolioByStay(@PathVariable UUID stayId) {
        return folioService.getFolioByStayId(stayId);
    }

    @GetMapping("/folios/{folioId}")
    public FolioResponse getFolio(@PathVariable UUID folioId) {
        return folioService.getFolioById(folioId);
    }

    @GetMapping("/folios/{folioId}/entries")
    public Page<FolioEntryResponse> getFolioEntries(
            @PathVariable UUID folioId,
            @RequestParam(required = false) FolioEntryType entryType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt,asc") String sort
    ) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        return folioService.getFolioEntries(folioId, entryType, pageable);
    }

    @PostMapping("/folios/{folioId}/adjustments")
    public FolioEntryResponse createAdjustment(
            @PathVariable UUID folioId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateAdjustmentRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "ANONYMOUS";
        return folioService.createAdjustment(folioId, request, idempotencyKey, actor);
    }

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "occurredAt");
        }
        String[] parts = sortParam.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = (parts.length > 1 && parts[1].equalsIgnoreCase("desc"))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }
}
