package com.hotelos.roomservice.service;

import com.hotelos.roomservice.persistence.entity.OrderItemEntity;
import com.hotelos.roomservice.persistence.entity.RoomServiceOrderEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class OrderCalculationHelper {
    private OrderCalculationHelper() {}

    public static BigDecimal calculateOrderTotal(RoomServiceOrderEntity order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
