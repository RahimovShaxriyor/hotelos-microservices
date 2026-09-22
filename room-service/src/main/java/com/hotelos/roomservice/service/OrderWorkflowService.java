package com.hotelos.roomservice.service;

import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.EventTypes;
import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import com.hotelos.common.event.payload.RoomServiceChargePayload;
import com.hotelos.common.event.payload.RoomServiceOrderUpdatedPayload;
import com.hotelos.roomservice.config.RabbitConfig;
import com.hotelos.roomservice.domain.OrderItem;
import com.hotelos.roomservice.domain.OrderStatus;
import com.hotelos.roomservice.domain.RoomOrder;
import com.hotelos.roomservice.dto.CreateOrderRequest;
import com.hotelos.roomservice.exception.HotelValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class OrderWorkflowService {
    private static final Logger log = LoggerFactory.getLogger(OrderWorkflowService.class);

    private final Map<String, RoomOrder> orders = new ConcurrentHashMap<>();
    private final Queue<RoomOrder> orderQueue = new ConcurrentLinkedQueue<>();
    private final RabbitTemplate rabbitTemplate;

    public OrderWorkflowService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public RoomOrder createOrder(CreateOrderRequest request) {
        validate(request);
        RoomOrder order = new RoomOrder(request.getRoomNumber(), request.getItems());
        orders.put(order.getOrderId(), order);
        orderQueue.add(order);
        publishOrder(order);
        return order;
    }

    public RoomOrder getOrder(String orderId) {
        RoomOrder order = orders.get(orderId);
        if (order == null) {
            throw new HotelValidationException("Unknown order ID: " + orderId);
        }
        return order;
    }

    public RoomOrder nextStatus(String orderId) {
        RoomOrder order = getOrder(orderId);
        boolean justDelivered = false;
        synchronized (order) {
            OrderStatus oldStatus = order.getStatus();
            if (oldStatus == OrderStatus.DELIVERED) {
                log.warn("Illegal advance rejected for terminal order {}", orderId);
                throw new HotelValidationException("Delivered orders cannot be advanced");
            }
            if (oldStatus == OrderStatus.CANCELLED) {
                log.warn("Illegal advance rejected for cancelled order {}", orderId);
                throw new HotelValidationException("Cancelled orders cannot be advanced");
            }
            order.advance();
            OrderStatus newStatus = order.getStatus();
            if (oldStatus == OrderStatus.DELIVERING && newStatus == OrderStatus.DELIVERED) {
                justDelivered = true;
                orderQueue.remove(order);
            }
        }

        publishOrder(order);

        if (justDelivered) {
            log.info("Order {} transitioned to DELIVERED. Emitting room service charge.", order.getOrderId());
            RoomServiceChargePayload payload = new RoomServiceChargePayload(
                    order.getOrderId(),
                    order.getRoomNumber(),
                    order.total(),
                    Instant.now()
            );
            EventEnvelope<RoomServiceChargePayload> envelope = EventEnvelope.create(
                    EventTypes.ROOM_SERVICE_CHARGE,
                    "room-service",
                    order.getOrderId(),
                    null,
                    payload
            );
            rabbitTemplate.convertAndSend(MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_SERVICE_CHARGE, envelope);
        }
        return order;
    }

    public RoomOrder cancel(String orderId) {
        RoomOrder order = getOrder(orderId);
        synchronized (order) {
            order.cancel();
            orderQueue.remove(order);
        }
        publishOrder(order);
        return order;
    }

    public List<RoomOrder> getOrders() {
        return orders.values().stream().sorted(Comparator.comparing(RoomOrder::getCreatedAt)).toList();
    }

    public Map<String, Object> getChargesByRoom(String roomNumber) {
        if (roomNumber == null || roomNumber.isBlank()) {
            throw new HotelValidationException("Room number is required");
        }
        BigDecimal deliveredCharges = orders.values().stream()
                .filter(order -> order.getRoomNumber().equals(roomNumber))
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .map(RoomOrder::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomNumber", roomNumber);
        result.put("deliveredRoomServiceCharges", deliveredCharges);
        result.put("orders", orders.values().stream().filter(order -> order.getRoomNumber().equals(roomNumber)).toList());
        return result;
    }

    public synchronized Map<String, Object> reset() {
        orders.clear();
        orderQueue.clear();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Room service orders have been cleared");
        result.put("orders", orders.size());
        return result;
    }

    private void publishOrder(RoomOrder order) {
        RoomServiceOrderUpdatedPayload payload = new RoomServiceOrderUpdatedPayload(
                order.getOrderId(),
                order.getRoomNumber(),
                order.getStatus().name(),
                order.total(),
                Instant.now()
        );
        EventEnvelope<RoomServiceOrderUpdatedPayload> envelope = EventEnvelope.create(
                EventTypes.ROOM_SERVICE_ORDER_UPDATED,
                "room-service",
                order.getOrderId(),
                null,
                payload
        );
        rabbitTemplate.convertAndSend(MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_SERVICE_ORDER_UPDATED, envelope);
    }

    private void validate(CreateOrderRequest request) {
        if (request == null || request.getRoomNumber() == null || request.getRoomNumber().isBlank()) {
            throw new HotelValidationException("Room number is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new HotelValidationException("At least one order item is required");
        }
        for (OrderItem item : request.getItems()) {
            if (item.getName() == null || item.getName().isBlank()) {
                throw new HotelValidationException("Order item name is required");
            }
            if (item.getQuantity() <= 0) {
                throw new HotelValidationException("Item quantity must be greater than zero");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new HotelValidationException("Item unit price cannot be negative");
            }
        }
    }
}
