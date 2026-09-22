package com.hotelos.reception.service;

import com.hotelos.reception.persistence.entity.GuestStayEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BillingService {

    public BigDecimal calculateBill(GuestStayEntity stay, BigDecimal roomServiceCharges) {
        int chargedNights = Math.max(1, stay.getBookedNights());
        BigDecimal roomCost = stay.getNightlyRateAtCheckin().multiply(BigDecimal.valueOf(chargedNights));
        BigDecimal charges = roomServiceCharges != null ? roomServiceCharges : BigDecimal.ZERO;
        BigDecimal total = roomCost
                .add(charges)
                .add(stay.getMinibarCharge() != null ? stay.getMinibarCharge() : BigDecimal.ZERO)
                .add(stay.getLateCheckoutFee() != null ? stay.getLateCheckoutFee() : BigDecimal.ZERO)
                .subtract(stay.getDiscount() != null ? stay.getDiscount() : BigDecimal.ZERO);
        return total.max(BigDecimal.ZERO);
    }
}
