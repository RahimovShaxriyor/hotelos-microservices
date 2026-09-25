package com.hotelos.roomservice.service;

import com.hotelos.roomservice.domain.OrderItem;
import com.hotelos.roomservice.domain.OrderStatus;
import com.hotelos.roomservice.domain.RoomOrder;
import com.hotelos.roomservice.dto.CreateOrderRequest;
import com.hotelos.roomservice.event.LocalOrderDeliveredEvent;
import com.hotelos.roomservice.event.LocalOrderUpdatedEvent;
import com.hotelos.roomservice.exception.HotelValidationException;
import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.EventTypes;
import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import com.hotelos.common.event.payload.RoomServiceChargePayload;
import com.hotelos.common.event.payload.RoomServiceOrderUpdatedPayload;
import com.hotelos.roomservice.outbox.RoomServiceOutboxService;
import com.hotelos.roomservice.persistence.entity.OrderItemEntity;
import com.hotelos.roomservice.persistence.entity.RoomServiceOrderEntity;
import com.hotelos.roomservice.persistence.repository.OrderItemRepository;
import com.hotelos.roomservice.persistence.repository.OrderRepository;
import com.hotelos.roomservice.persistence.repository.RoomServiceOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class OrderWorkflowService {
    private static final Logger log = LoggerFactory.getLogger(OrderWorkflowService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RoomServiceOutboxService outboxService;
    private final RoomServiceOutboxRepository outboxRepository;

    public OrderWorkflowService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            RoomServiceOutboxService outboxService,
            RoomServiceOutboxRepository outboxRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.outboxService = outboxService;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public RoomOrder createOrder(CreateOrderRequest request) {
        validate(request);
        UUID orderId = UUID.randomUUID();
        Instant now = Instant.now();

        RoomServiceOrderEntity orderEntity = new RoomServiceOrderEntity(
                orderId,
                request.getRoomNumber().trim(),
                OrderStatus.RECEIVED,
                now,
                now
        );

        int lineNumber = 1;
        for (OrderItem item : request.getItems()) {
            OrderItemEntity itemEntity = new OrderItemEntity(
                    UUID.randomUUID(),
                    orderEntity,
                    lineNumber++,
                    item.getName().trim(),
                    item.getQuantity(),
                    item.getUnitPrice()
            );
            orderEntity.addItem(itemEntity);
        }

        orderRepository.save(orderEntity);
        log.info("Created order {} for room {}", orderId, request.getRoomNumber());

        RoomOrder domainOrder = toDomain(orderEntity);
        String correlationId = UUID.randomUUID().toString();
        outboxService.enqueue(
                EventEnvelope.create(
                        EventTypes.ROOM_SERVICE_ORDER_UPDATED,
                        "room-service",
                        domainOrder.getOrderId(),
                        correlationId,
                        new RoomServiceOrderUpdatedPayload(
                                domainOrder.getOrderId(),
                                domainOrder.getRoomNumber(),
                                domainOrder.getStatus().name(),
                                domainOrder.total(),
                                orderEntity.getStatusChangedAt()
                        )
                ),
                MessagingConstants.HOTEL_EXCHANGE,
                RoutingKeys.ROOM_SERVICE_ORDER_UPDATED
        );

        return domainOrder;
    }

    @Transactional(readOnly = true)
    public RoomOrder getOrder(String orderId) {
        UUID id = parseUuid(orderId);
        RoomServiceOrderEntity order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new HotelValidationException("Unknown order ID: " + orderId));
        return toDomain(order);
    }

    @Transactional
    public RoomOrder nextStatus(String orderIdStr) {
        UUID orderId = parseUuid(orderIdStr);
        RoomServiceOrderEntity order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new HotelValidationException("Unknown order ID: " + orderIdStr));

        OrderStatus oldStatus = order.getStatus();
        if (oldStatus == OrderStatus.DELIVERED) {
            log.warn("Illegal advance rejected for terminal order {}", orderIdStr);
            throw new HotelValidationException("Delivered orders cannot be advanced");
        }
        if (oldStatus == OrderStatus.CANCELLED) {
            log.warn("Illegal advance rejected for cancelled order {}", orderIdStr);
            throw new HotelValidationException("Cancelled orders cannot be advanced");
        }

        OrderStatus newStatus = switch (oldStatus) {
            case RECEIVED -> OrderStatus.PREPARING;
            case PREPARING -> OrderStatus.DELIVERING;
            case DELIVERING -> OrderStatus.DELIVERED;
            default -> throw new HotelValidationException("Unsupported order transition from " + oldStatus);
        };

        Instant now = Instant.now();
        order.setStatus(newStatus);
        order.setStatusChangedAt(now);

        boolean justDelivered = (newStatus == OrderStatus.DELIVERED);
        if (justDelivered) {
            order.setDeliveredAt(now);
        }

        orderRepository.save(order);
        log.info("Order {} transitioned from {} to {}", orderIdStr, oldStatus, newStatus);

        RoomOrder domainOrder = toDomain(order);
        String correlationId = UUID.randomUUID().toString();

        outboxService.enqueue(
                EventEnvelope.create(
                        EventTypes.ROOM_SERVICE_ORDER_UPDATED,
                        "room-service",
                        domainOrder.getOrderId(),
                        correlationId,
                        new RoomServiceOrderUpdatedPayload(
                                domainOrder.getOrderId(),
                                domainOrder.getRoomNumber(),
                                domainOrder.getStatus().name(),
                                domainOrder.total(),
                                order.getStatusChangedAt()
                        )
                ),
                MessagingConstants.HOTEL_EXCHANGE,
                RoutingKeys.ROOM_SERVICE_ORDER_UPDATED
        );

        if (justDelivered) {
            log.info("Order {} transitioned to DELIVERED. Scheduling room service charge.", domainOrder.getOrderId());
            outboxService.enqueue(
                    EventEnvelope.create(
                            EventTypes.ROOM_SERVICE_CHARGE,
                            "room-service",
                            domainOrder.getOrderId(),
                            correlationId,
                            new RoomServiceChargePayload(
                                    domainOrder.getOrderId(),
                                    domainOrder.getRoomNumber(),
                                    domainOrder.total(),
                                    order.getDeliveredAt()
                            )
                    ),
                    MessagingConstants.HOTEL_EXCHANGE,
                    RoutingKeys.ROOM_SERVICE_CHARGE
            );
        }

        return domainOrder;
    }

    @Transactional
    public RoomOrder cancel(String orderIdStr) {
        UUID orderId = parseUuid(orderIdStr);
        RoomServiceOrderEntity order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new HotelValidationException("Unknown order ID: " + orderIdStr));

        OrderStatus currentStatus = order.getStatus();
        if (currentStatus == OrderStatus.DELIVERED) {
            log.warn("Illegal cancel rejected for delivered order {}", orderIdStr);
            throw new HotelValidationException("Delivered orders cannot be cancelled");
        }
        if (currentStatus == OrderStatus.CANCELLED) {
            log.warn("Illegal cancel rejected for already cancelled order {}", orderIdStr);
            throw new HotelValidationException("Order is already cancelled");
        }

        Instant now = Instant.now();
        order.setStatus(OrderStatus.CANCELLED);
        order.setStatusChangedAt(now);
        order.setCancelledAt(now);

        orderRepository.save(order);
        log.info("Order {} cancelled", orderIdStr);

        RoomOrder domainOrder = toDomain(order);
        String correlationId = UUID.randomUUID().toString();

        outboxService.enqueue(
                EventEnvelope.create(
                        EventTypes.ROOM_SERVICE_ORDER_UPDATED,
                        "room-service",
                        domainOrder.getOrderId(),
                        correlationId,
                        new RoomServiceOrderUpdatedPayload(
                                domainOrder.getOrderId(),
                                domainOrder.getRoomNumber(),
                                domainOrder.getStatus().name(),
                                domainOrder.total(),
                                order.getStatusChangedAt()
                        )
                ),
                MessagingConstants.HOTEL_EXCHANGE,
                RoutingKeys.ROOM_SERVICE_ORDER_UPDATED
        );

        return domainOrder;
    }

    @Transactional(readOnly = true)
    public List<RoomOrder> getOrders() {
        return orderRepository.findAllWithItems().stream()
                .map(this::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getChargesByRoom(String roomNumber) {
        if (roomNumber == null || roomNumber.isBlank()) {
            throw new HotelValidationException("Room number is required");
        }
        if (roomNumber.trim().length() > 16) {
            throw new HotelValidationException("Room number cannot exceed 16 characters");
        }
        List<RoomServiceOrderEntity> entities = orderRepository.findByRoomNumberWithItems(roomNumber.trim());
        List<RoomOrder> domainOrders = entities.stream().map(this::toDomain).toList();

        BigDecimal deliveredCharges = domainOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .map(RoomOrder::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomNumber", roomNumber);
        result.put("deliveredRoomServiceCharges", deliveredCharges);
        result.put("orders", domainOrders);
        return result;
    }

    @Transactional
    public Map<String, Object> reset() {
        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        outboxRepository.deleteAllInBatch();
        log.info("Room service orders, items, and outbox cleared via reset");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Room service orders have been cleared");
        result.put("orders", 0);
        return result;
    }

    private RoomOrder toDomain(RoomServiceOrderEntity entity) {
        List<OrderItem> items = entity.getItems() != null
                ? entity.getItems().stream()
                .map(item -> new OrderItem(item.getItemName(), item.getQuantity(), item.getUnitPrice()))
                .toList()
                : List.of();

        return new RoomOrder(
                entity.getId().toString(),
                entity.getRoomNumber(),
                items,
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getStatusChangedAt()
        );
    }

    private UUID parseUuid(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new HotelValidationException("Unknown order ID: " + orderId);
        }
        try {
            return UUID.fromString(orderId);
        } catch (IllegalArgumentException ex) {
            throw new HotelValidationException("Unknown order ID: " + orderId);
        }
    }

    private void validate(CreateOrderRequest request) {
        if (request == null || request.getRoomNumber() == null || request.getRoomNumber().isBlank()) {
            throw new HotelValidationException("Room number is required");
        }
        if (request.getRoomNumber().trim().length() > 16) {
            throw new HotelValidationException("Room number cannot exceed 16 characters");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new HotelValidationException("At least one order item is required");
        }
        for (OrderItem item : request.getItems()) {
            if (item.getName() == null || item.getName().isBlank()) {
                throw new HotelValidationException("Order item name is required");
            }
            if (item.getName().trim().length() > 128) {
                throw new HotelValidationException("Order item name cannot exceed 128 characters");
            }
            if (item.getQuantity() <= 0) {
                throw new HotelValidationException("Item quantity must be greater than zero");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new HotelValidationException("Item unit price must be greater than zero");
            }
        }
    }
}
