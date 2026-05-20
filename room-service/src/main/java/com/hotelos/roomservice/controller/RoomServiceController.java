package com.hotelos.roomservice.controller;

import com.hotelos.roomservice.domain.RoomOrder;
import com.hotelos.roomservice.dto.CreateOrderRequest;
import com.hotelos.roomservice.service.OrderWorkflowService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/room-service")
public class RoomServiceController {
    private final OrderWorkflowService orderWorkflowService;

    public RoomServiceController(OrderWorkflowService orderWorkflowService) {
        this.orderWorkflowService = orderWorkflowService;
    }

    @GetMapping("/orders")
    public List<RoomOrder> all() {
        return orderWorkflowService.getOrders();
    }

    @GetMapping("/orders/{orderId}")
    public RoomOrder byId(@PathVariable String orderId) {
        return orderWorkflowService.getOrder(orderId);
    }

    @PostMapping("/orders")
    public RoomOrder create(@RequestBody CreateOrderRequest request) {
        return orderWorkflowService.createOrder(request);
    }

    @PatchMapping("/orders/{orderId}/next")
    public RoomOrder nextPatch(@PathVariable String orderId) {
        return orderWorkflowService.nextStatus(orderId);
    }

    @PostMapping("/orders/{orderId}/next")
    public RoomOrder nextPost(@PathVariable String orderId) {
        return orderWorkflowService.nextStatus(orderId);
    }

    @PatchMapping("/orders/{orderId}/cancel")
    public RoomOrder cancel(@PathVariable String orderId) {
        return orderWorkflowService.cancel(orderId);
    }

    @GetMapping("/charges/{roomNumber}")
    public Map<String, Object> charges(@PathVariable String roomNumber) {
        return orderWorkflowService.getChargesByRoom(roomNumber);
    }

    @PostMapping("/dev/reset")
    public Map<String, Object> reset() {
        return orderWorkflowService.reset();
    }

    @PostMapping("/dev/seed")
    public Map<String, Object> seed() {
        return orderWorkflowService.reset();
    }
}
