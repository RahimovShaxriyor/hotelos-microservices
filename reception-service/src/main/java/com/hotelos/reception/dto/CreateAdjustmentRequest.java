package com.hotelos.reception.dto;

import com.hotelos.reception.domain.FolioEntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAdjustmentRequest(
        @NotNull(message = "entryType is required")
        FolioEntryType entryType,

        @NotNull(message = "amount is required")
        BigDecimal amount,

        @NotBlank(message = "description is required")
        String description
) {}
