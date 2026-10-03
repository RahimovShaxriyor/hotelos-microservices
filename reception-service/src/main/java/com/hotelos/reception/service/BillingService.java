package com.hotelos.reception.service;

import com.hotelos.reception.persistence.entity.GuestStayEntity;
import com.hotelos.reception.persistence.repository.FolioEntryRepository;
import com.hotelos.reception.persistence.repository.FolioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class BillingService {

    private final FolioRepository folioRepository;
    private final FolioEntryRepository folioEntryRepository;

    public BillingService(FolioRepository folioRepository, FolioEntryRepository folioEntryRepository) {
        this.folioRepository = folioRepository;
        this.folioEntryRepository = folioEntryRepository;
    }

    public BigDecimal calculateBill(GuestStayEntity stay, BigDecimal roomServiceCharges) {
        if (stay == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        var folioOpt = folioRepository.findByStayId(stay.getId());
        if (folioOpt.isPresent()) {
            BigDecimal balance = folioEntryRepository.calculateBalanceByFolioId(folioOpt.get().getId());
            return balance != null ? balance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        int chargedNights = Math.max(1, stay.getBookedNights());
        BigDecimal roomCost = stay.getNightlyRateAtCheckin().multiply(BigDecimal.valueOf(chargedNights));
        BigDecimal charges = roomServiceCharges != null ? roomServiceCharges : BigDecimal.ZERO;
        BigDecimal total = roomCost
                .add(charges)
                .add(stay.getMinibarCharge() != null ? stay.getMinibarCharge() : BigDecimal.ZERO)
                .add(stay.getLateCheckoutFee() != null ? stay.getLateCheckoutFee() : BigDecimal.ZERO)
                .subtract(stay.getDiscount() != null ? stay.getDiscount() : BigDecimal.ZERO);
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
