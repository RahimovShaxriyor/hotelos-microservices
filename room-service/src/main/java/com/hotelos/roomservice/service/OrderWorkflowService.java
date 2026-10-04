package com.hotelos.roomservice.service;

import com.hotelos.roomservice.domain.OrderItem;
import com.hotelos.roomservice.domain.OrderStatus;
import com.hotelos.roomservice.domain.RoomOrder;
import com.hotelos.roomservice.dto.CreateOrderRequest;
import com.hotelos.roomservice.event.LocalOrderDeliveredEvent;
import com.hotelos.roomservice.event.LocalOrderUpdatedEvent;
import com.hotelos.roomservice.exception.HotelValidationException;
import com.hotelos.common.currency.CurrencyValidator;
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
import com.hotelos.common.event.payload.RoomServiceChargePayloadV2;
import com.hotelos.roomservice.client.ReceptionClient;
import com.hotelos.roomservice.domain.OrderingSessionStatus;
import com.hotelos.roomservice.dto.ActiveStayResolutionResponse;
import com.hotelos.roomservice.exception.ConflictException;
import com.hotelos.roomservice.exception.NoActiveStayException;
import com.hotelos.roomservice.persistence.entity.StayOrderingSessionEntity;
import com.hotelos.roomservice.persistence.repository.StayOrderingSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final StayOrderingSessionRepository sessionRepository;
    private final ReceptionClient receptionClient;
    private final TransactionTemplate transactionTemplate;

    public OrderWorkflowService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            RoomServiceOutboxService outboxService,
            RoomServiceOutboxRepository outboxRepository,
            StayOrderingSessionRepository sessionRepository,
            ReceptionClient receptionClient,
            TransactionTemplate transactionTemplate
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.outboxService = outboxService;
        this.outboxRepository = outboxRepository;
        this.sessionRepository = sessionRepository;
        this.receptionClient = receptionClient;
        this.transactionTemplate = transactionTemplate;
    }

    public RoomOrder createOrder(CreateOrderRequest request) {
        validate(request);
        String roomNumber = request.getRoomNumber().trim();

        // 1. Outside DB transaction: call Reception to resolve active stay
        ActiveStayResolutionResponse activeStay = receptionClient.resolveActiveStay(roomNumber);
        if (activeStay == null || activeStay.stayId() == null) {
            throw new NoActiveStayException("No active stay found for room: " + roomNumber);
        }

        // Validate client-supplied currency against authoritative stay currency
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            String clientCurrency = request.getCurrency().trim().toUpperCase(Locale.ROOT);
            if (!clientCurrency.equalsIgnoreCase(activeStay.currency())) {
                throw new HotelValidationException("Supplied currency " + clientCurrency + " does not match stay currency " + activeStay.currency());
            }
        }

        return transactionTemplate.execute(status -> createOrderWithSession(request, activeStay));
    }

    @Transactional
    public RoomOrder createOrderWithSession(CreateOrderRequest request, ActiveStayResolutionResponse activeStay) {
        UUID stayId = activeStay.stayId();

        // 1. Ensure ordering session row atomically
        sessionRepository.ensureSessionRow(stayId);

        // 2. Lock session FOR UPDATE
        StayOrderingSessionEntity session = sessionRepository.findByStayIdForUpdate(stayId)
                .orElseThrow(() -> new IllegalStateException("Failed to lock stay ordering session: " + stayId));

        if (session.getStatus() == OrderingSessionStatus.FROZEN) {
            throw new ConflictException("STAY_ORDERING_FROZEN", "Cannot create order: stay ordering session is frozen");
        }
        if (session.getStatus() == OrderingSessionStatus.CLOSED) {
            throw new ConflictException("STAY_ORDERING_CLOSED", "Cannot create order: stay ordering session is closed");
        }

        UUID orderId = UUID.randomUUID();
        Instant now = Instant.now();
        String currency = activeStay.currency();

        RoomServiceOrderEntity orderEntity = new RoomServiceOrderEntity(
                orderId,
                stayId,
                request.getRoomNumber().trim(),
                OrderStatus.RECEIVED,
                currency,
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
        log.info("Created order {} for stay {} room {}", orderId, stayId, request.getRoomNumber());

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

        // Read order first to determine stayId
        RoomServiceOrderEntity existing = orderRepository.findById(orderId)
                .orElseThrow(() -> new HotelValidationException("Unknown order ID: " + orderIdStr));

        UUID stayId = existing.getStayId();
        if (stayId != null) {
            // Lock session first according to lock hierarchy: sessions -> orders
            StayOrderingSessionEntity session = sessionRepository.findByStayIdForUpdate(stayId)
                    .orElseThrow(() -> new IllegalStateException("Stay ordering session missing for stay: " + stayId));
            if (session.getStatus() == OrderingSessionStatus.FROZEN) {
                throw new ConflictException("STAY_ORDERING_FROZEN", "Cannot advance order: stay ordering session is frozen");
            }
            if (session.getStatus() == OrderingSessionStatus.CLOSED) {
                throw new ConflictException("STAY_ORDERING_CLOSED", "Cannot advance order: stay ordering session is closed");
            }
        }

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
            BigDecimal chargeAmount = OrderCalculationHelper.calculateOrderTotal(order);
            log.info("Order {} transitioned to DELIVERED. Scheduling room service charge of {}", domainOrder.getOrderId(), chargeAmount);

            if (order.getStayId() != null) {
                // V2 event for orders bound to a stay
                outboxService.enqueue(
                        EventEnvelope.create(
                                EventTypes.ROOM_SERVICE_CHARGE,
                                "room-service",
                                domainOrder.getOrderId(),
                                correlationId,
                                new RoomServiceChargePayloadV2(
                                        domainOrder.getOrderId(),
                                        order.getStayId(),
                                        domainOrder.getRoomNumber(),
                                        chargeAmount,
                                        domainOrder.getCurrency(),
                                        order.getDeliveredAt()
                                ),
                                2
                        ),
                        MessagingConstants.HOTEL_EXCHANGE,
                        RoutingKeys.ROOM_SERVICE_CHARGE
                );
            } else {
                // Legacy V1 event for historical/unbound orders
                outboxService.enqueue(
                        EventEnvelope.create(
                                EventTypes.ROOM_SERVICE_CHARGE,
                                "room-service",
                                domainOrder.getOrderId(),
                                correlationId,
                                new RoomServiceChargePayload(
                                        domainOrder.getOrderId(),
                                        domainOrder.getRoomNumber(),
                                        chargeAmount,
                                        domainOrder.getCurrency(),
                                        order.getDeliveredAt()
                                )
                        ),
                        MessagingConstants.HOTEL_EXCHANGE,
                        RoutingKeys.ROOM_SERVICE_CHARGE
                );
            }
        }

        return domainOrder;
    }

    @Transactional
    public RoomOrder cancel(String orderIdStr) {
        UUID orderId = parseUuid(orderIdStr);

        RoomServiceOrderEntity existing = orderRepository.findById(orderId)
                .orElseThrow(() -> new HotelValidationException("Unknown order ID: " + orderIdStr));

        UUID stayId = existing.getStayId();
        if (stayId != null) {
            StayOrderingSessionEntity session = sessionRepository.findByStayIdForUpdate(stayId)
                    .orElseThrow(() -> new IllegalStateException("Stay ordering session missing for stay: " + stayId));
            if (session.getStatus() == OrderingSessionStatus.FROZEN) {
                throw new ConflictException("STAY_ORDERING_FROZEN", "Cannot cancel order: stay ordering session is frozen");
            }
            if (session.getStatus() == OrderingSessionStatus.CLOSED) {
                throw new ConflictException("STAY_ORDERING_CLOSED", "Cannot cancel order: stay ordering session is closed");
            }
        }

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
        sessionRepository.deleteAllInBatch();
        outboxRepository.deleteAllInBatch();
        log.info("Room service orders, items, sessions, and outbox cleared via reset");

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
                entity.getCurrency(),
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
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            CurrencyValidator.validate(request.getCurrency());
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
