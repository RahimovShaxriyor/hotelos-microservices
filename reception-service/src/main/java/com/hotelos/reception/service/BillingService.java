package com.hotelos.reception.service;

import com.hotelos.reception.domain.GuestStay;
import com.hotelos.reception.domain.Room;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BillingService {
    public BigDecimal calculateBill(Room room, GuestStay stay) {
        int chargedNights = Math.max(1, stay.getBookedNights());
        BigDecimal roomCost = room.getNightlyRate().multiply(BigDecimal.valueOf(chargedNights));
        BigDecimal total = roomCost
                .add(stay.getRoomServiceCharges())
                .add(stay.getMinibarCharge())
                .add(stay.getLateCheckoutFee())
                .subtract(stay.getDiscount());
        return total.max(BigDecimal.ZERO);
    }
}
