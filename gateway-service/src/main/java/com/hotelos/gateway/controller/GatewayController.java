package com.hotelos.gateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hotelos.gateway.config.ServiceUrls;
import com.hotelos.gateway.dto.CheckInRequest;
import com.hotelos.gateway.dto.CreateIssueRequest;
import com.hotelos.gateway.dto.CreateOrderRequest;
import com.hotelos.gateway.dto.GatewayHealthResponse;
import com.hotelos.gateway.dto.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@Tag(name = "HotelOS Gateway", description = "Unified gateway endpoints for all HotelOS microservices")
public class GatewayController {
    private final RestClient receptionClient;
    private final RestClient housekeepingClient;
    private final RestClient roomServiceClient;
    private final RestClient maintenanceClient;
    private final RestClient dashboardClient;
    private final ServiceUrls serviceUrls;
    private final ObjectMapper objectMapper;
    private final String authToken;

    public GatewayController(
            @Qualifier("receptionClient") RestClient receptionClient,
            @Qualifier("housekeepingClient") RestClient housekeepingClient,
            @Qualifier("roomServiceClient") RestClient roomServiceClient,
            @Qualifier("maintenanceClient") RestClient maintenanceClient,
            @Qualifier("dashboardClient") RestClient dashboardClient,
            ServiceUrls serviceUrls,
            ObjectMapper objectMapper,
            @Value("${hotelos.auth-token}") String authToken
    ) {
        this.receptionClient = receptionClient;
        this.housekeepingClient = housekeepingClient;
        this.roomServiceClient = roomServiceClient;
        this.maintenanceClient = maintenanceClient;
        this.dashboardClient = dashboardClient;
        this.serviceUrls = serviceUrls;
        this.objectMapper = objectMapper;
        this.authToken = authToken;
    }

    // -------------------------------------------------------------------------
    // Gateway
    // -------------------------------------------------------------------------
    @Operation(summary = "Gateway health check")
    @GetMapping("/api/gateway/health")
    public GatewayHealthResponse health() {
        return new GatewayHealthResponse("UP", "gateway-service", "/swagger-ui.html");
    }

    @Operation(summary = "Gateway information")
    @GetMapping("/api/gateway/info")
    public Map<String, Object> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("project", "HotelOS");
        body.put("description", "Real-time hotel operations system with microservices, RabbitMQ, WebSocket and Swagger Gateway");
        body.put("version", "2.0.0");
        body.put("swagger", "/swagger-ui.html");
        body.put("dashboard", "http://localhost:8085");
        body.put("token", authToken);
        return body;
    }

    @Operation(summary = "List gateway routes and downstream services")
    @GetMapping("/api/gateway/routes")
    public Map<String, Object> routes() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("gateway", "/api/gateway/**");
        body.put("auth", "/api/auth/**");
        body.put("reception", serviceUrls.receptionUrl() + "/api/reception/**");
        body.put("housekeeping", serviceUrls.housekeepingUrl() + "/api/housekeeping/**");
        body.put("roomService", serviceUrls.roomServiceUrl() + "/api/room-service/**");
        body.put("maintenance", serviceUrls.maintenanceUrl() + "/api/maintenance/**");
        body.put("dashboard", serviceUrls.dashboardUrl() + "/api/dashboard/**");
        body.put("webSocket", "ws://localhost:8085/ws/dashboard?token=" + authToken);
        return body;
    }

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------
    @Operation(summary = "Demo login", description = "Use username admin and password admin123. Returns the dashboard/API demo token.")
    @PostMapping(value = "/api/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> login(@RequestBody LoginRequest request) {
        if (request == null || !"admin".equals(request.getUsername()) || !"admin123".equals(request.getPassword())) {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("error", "Invalid credentials");
            body.put("message", "Use username admin and password admin123 for the demo.");
            return ResponseEntity.status(401).body(body);
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("token", authToken);
        body.put("tokenType", "DemoToken");
        body.put("dashboardWebSocket", "ws://localhost:8085/ws/dashboard?token=" + authToken);
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Validate demo token")
    @GetMapping("/api/auth/validate")
    public ResponseEntity<JsonNode> validateToken(@RequestParam(required = false) String token) {
        ObjectNode body = objectMapper.createObjectNode();
        boolean valid = authToken.equals(token);
        body.put("valid", valid);
        body.put("message", valid ? "Token is valid" : "Token is missing or invalid");
        return ResponseEntity.status(valid ? 200 : 401).body(body);
    }

    // -------------------------------------------------------------------------
    // Reception
    // -------------------------------------------------------------------------
    @Operation(summary = "Get all rooms")
    @GetMapping("/api/reception/rooms")
    public ResponseEntity<JsonNode> getRooms() {
        return get(receptionClient, "/api/reception/rooms");
    }

    @Operation(summary = "Get room by room number")
    @GetMapping("/api/reception/rooms/{roomNumber}")
    public ResponseEntity<JsonNode> getRoom(@PathVariable String roomNumber) {
        return get(receptionClient, "/api/reception/rooms/" + roomNumber);
    }

    @Operation(summary = "Get available rooms", description = "Optional query params: roomType and floor.")
    @GetMapping("/api/reception/rooms/available")
    public ResponseEntity<JsonNode> getAvailableRooms(@RequestParam(required = false) String roomType,
                                                      @RequestParam(required = false) Integer floor) {
        StringBuilder uri = new StringBuilder("/api/reception/rooms/available");
        if (roomType != null || floor != null) {
            uri.append("?");
            if (roomType != null) {
                uri.append("roomType=").append(roomType);
            }
            if (floor != null) {
                if (roomType != null) uri.append("&");
                uri.append("floor=").append(floor);
            }
        }
        return get(receptionClient, uri.toString());
    }

    @Operation(summary = "Check in guest")
    @PostMapping(value = "/api/reception/check-in", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> checkIn(@RequestBody CheckInRequest request) {
        return post(receptionClient, "/api/reception/check-in", request);
    }

    @Operation(summary = "Check out guest")
    @PostMapping("/api/reception/check-out/{roomNumber}")
    public ResponseEntity<JsonNode> checkOut(@PathVariable String roomNumber) {
        return post(receptionClient, "/api/reception/check-out/" + roomNumber, null);
    }

    @Operation(summary = "Get active guests")
    @GetMapping("/api/reception/guests")
    public ResponseEntity<JsonNode> getGuests() {
        return get(receptionClient, "/api/reception/guests");
    }

    @Operation(summary = "Get guest by room")
    @GetMapping("/api/reception/guests/by-room/{roomNumber}")
    public ResponseEntity<JsonNode> getGuestByRoom(@PathVariable String roomNumber) {
        return get(receptionClient, "/api/reception/guests/by-room/" + roomNumber);
    }

    @Operation(summary = "Archive a guest stay")
    @PatchMapping("/api/reception/guests/{guestId}/archive")
    public ResponseEntity<JsonNode> archiveGuest(@PathVariable String guestId) {
        return patch(receptionClient, "/api/reception/guests/" + guestId + "/archive", null);
    }

    @Operation(summary = "Calculate bill for room")
    @PostMapping("/api/reception/bills/{roomNumber}/calculate")
    public ResponseEntity<JsonNode> calculateBill(@PathVariable String roomNumber) {
        return post(receptionClient, "/api/reception/bills/" + roomNumber + "/calculate", null);
    }

    // -------------------------------------------------------------------------
    // Housekeeping
    // -------------------------------------------------------------------------
    @Operation(summary = "Get housekeeping queue")
    @GetMapping("/api/housekeeping/queue")
    public ResponseEntity<JsonNode> getHousekeepingQueue() {
        return get(housekeepingClient, "/api/housekeeping/queue");
    }

    @Operation(summary = "Start room cleaning")
    @PostMapping("/api/housekeeping/rooms/{roomNumber}/start")
    public ResponseEntity<JsonNode> startCleaning(@PathVariable String roomNumber) {
        return post(housekeepingClient, "/api/housekeeping/rooms/" + roomNumber + "/start", null);
    }

    @Operation(summary = "Mark room clean")
    @PostMapping("/api/housekeeping/rooms/{roomNumber}/clean")
    public ResponseEntity<JsonNode> markRoomClean(@PathVariable String roomNumber) {
        return post(housekeepingClient, "/api/housekeeping/rooms/" + roomNumber + "/clean", null);
    }

    @Operation(summary = "Cancel housekeeping queue item")
    @PatchMapping("/api/housekeeping/queue/{roomNumber}/cancel")
    public ResponseEntity<JsonNode> cancelCleaning(@PathVariable String roomNumber) {
        return patch(housekeepingClient, "/api/housekeeping/queue/" + roomNumber + "/cancel", null);
    }

    @Operation(summary = "Get cleaners")
    @GetMapping("/api/housekeeping/cleaners")
    public ResponseEntity<JsonNode> getCleaners() {
        return get(housekeepingClient, "/api/housekeeping/cleaners");
    }

    // -------------------------------------------------------------------------
    // Room Service
    // -------------------------------------------------------------------------
    @Operation(summary = "Get room service orders")
    @GetMapping("/api/room-service/orders")
    public ResponseEntity<JsonNode> getRoomServiceOrders() {
        return get(roomServiceClient, "/api/room-service/orders");
    }

    @Operation(summary = "Get room service order by ID")
    @GetMapping("/api/room-service/orders/{orderId}")
    public ResponseEntity<JsonNode> getRoomServiceOrder(@PathVariable String orderId) {
        return get(roomServiceClient, "/api/room-service/orders/" + orderId);
    }

    @Operation(summary = "Create room service order")
    @PostMapping(value = "/api/room-service/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> createRoomServiceOrder(@RequestBody CreateOrderRequest request) {
        return post(roomServiceClient, "/api/room-service/orders", request);
    }

    @Operation(summary = "Move room service order to next status")
    @PatchMapping("/api/room-service/orders/{orderId}/next")
    public ResponseEntity<JsonNode> nextOrderStatus(@PathVariable String orderId) {
        return patch(roomServiceClient, "/api/room-service/orders/" + orderId + "/next", null);
    }

    @Operation(summary = "Cancel room service order")
    @PatchMapping("/api/room-service/orders/{orderId}/cancel")
    public ResponseEntity<JsonNode> cancelOrder(@PathVariable String orderId) {
        return patch(roomServiceClient, "/api/room-service/orders/" + orderId + "/cancel", null);
    }

    @Operation(summary = "Get room service charges by room")
    @GetMapping("/api/room-service/charges/{roomNumber}")
    public ResponseEntity<JsonNode> getCharges(@PathVariable String roomNumber) {
        return get(roomServiceClient, "/api/room-service/charges/" + roomNumber);
    }

    // -------------------------------------------------------------------------
    // Maintenance
    // -------------------------------------------------------------------------
    @Operation(summary = "Get maintenance issues")
    @GetMapping("/api/maintenance/issues")
    public ResponseEntity<JsonNode> getMaintenanceIssues() {
        return get(maintenanceClient, "/api/maintenance/issues");
    }

    @Operation(summary = "Get maintenance issue by ID")
    @GetMapping("/api/maintenance/issues/{issueId}")
    public ResponseEntity<JsonNode> getMaintenanceIssue(@PathVariable String issueId) {
        return get(maintenanceClient, "/api/maintenance/issues/" + issueId);
    }

    @Operation(summary = "Report maintenance issue")
    @PostMapping(value = "/api/maintenance/issues", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> reportMaintenanceIssue(@RequestBody CreateIssueRequest request) {
        return post(maintenanceClient, "/api/maintenance/issues", request);
    }

    @Operation(summary = "Get maintenance priority queue")
    @GetMapping("/api/maintenance/queue")
    public ResponseEntity<JsonNode> getMaintenanceQueue() {
        return get(maintenanceClient, "/api/maintenance/queue");
    }

    @Operation(summary = "Process next maintenance queue item")
    @PostMapping("/api/maintenance/queue/process-next")
    public ResponseEntity<JsonNode> processNextMaintenance() {
        return post(maintenanceClient, "/api/maintenance/queue/process-next", null);
    }

    @Operation(summary = "Resolve maintenance issue")
    @PatchMapping("/api/maintenance/issues/{issueId}/resolve")
    public ResponseEntity<JsonNode> resolveMaintenanceIssue(@PathVariable String issueId) {
        return patch(maintenanceClient, "/api/maintenance/issues/" + issueId + "/resolve", null);
    }

    @Operation(summary = "Cancel maintenance issue")
    @PatchMapping("/api/maintenance/issues/{issueId}/cancel")
    public ResponseEntity<JsonNode> cancelMaintenanceIssue(@PathVariable String issueId) {
        return patch(maintenanceClient, "/api/maintenance/issues/" + issueId + "/cancel", null);
    }

    @Operation(summary = "Get technicians")
    @GetMapping("/api/maintenance/technicians")
    public ResponseEntity<JsonNode> getTechnicians() {
        return get(maintenanceClient, "/api/maintenance/technicians");
    }

    // -------------------------------------------------------------------------
    // Dashboard
    // -------------------------------------------------------------------------
    @Operation(summary = "Aggregate full operational snapshot")
    @GetMapping("/api/dashboard/snapshot")
    public ResponseEntity<JsonNode> dashboardSnapshot() {
        ObjectNode body = objectMapper.createObjectNode();
        body.set("rooms", getBody(receptionClient, "/api/reception/rooms"));
        body.set("guests", getBody(receptionClient, "/api/reception/guests"));
        body.set("roomServiceOrders", getBody(roomServiceClient, "/api/room-service/orders"));
        body.set("maintenanceIssues", getBody(maintenanceClient, "/api/maintenance/issues"));
        body.set("housekeepingQueue", getBody(housekeepingClient, "/api/housekeeping/queue"));
        body.set("events", getBody(dashboardClient, "/api/dashboard/events"));
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Get dashboard event history")
    @GetMapping("/api/dashboard/events")
    public ResponseEntity<JsonNode> getDashboardEvents() {
        return get(dashboardClient, "/api/dashboard/events");
    }

    @Operation(summary = "Clear dashboard event history")
    @DeleteMapping("/api/dashboard/events")
    public ResponseEntity<JsonNode> clearDashboardEvents() {
        return delete(dashboardClient, "/api/dashboard/events");
    }

    // -------------------------------------------------------------------------
    // Demo endpoints
    // -------------------------------------------------------------------------
    @Operation(summary = "Reset all demo service state")
    @PostMapping("/api/demo/reset")
    public ResponseEntity<JsonNode> demoReset() {
        ObjectNode body = objectMapper.createObjectNode();
        body.set("reception", safePostBody(receptionClient, "/api/reception/dev/reset", null));
        body.set("housekeeping", safePostBody(housekeepingClient, "/api/housekeeping/dev/reset", null));
        body.set("roomService", safePostBody(roomServiceClient, "/api/room-service/dev/reset", null));
        body.set("maintenance", safePostBody(maintenanceClient, "/api/maintenance/dev/reset", null));
        body.set("dashboard", safePostBody(dashboardClient, "/api/dashboard/dev/reset", null));
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Seed demo data")
    @PostMapping("/api/demo/seed")
    public ResponseEntity<JsonNode> demoSeed() {
        ObjectNode body = objectMapper.createObjectNode();
        body.set("reception", safePostBody(receptionClient, "/api/reception/dev/seed", null));
        body.set("housekeeping", safePostBody(housekeepingClient, "/api/housekeeping/dev/seed", null));
        body.set("roomService", safePostBody(roomServiceClient, "/api/room-service/dev/seed", null));
        body.set("maintenance", safePostBody(maintenanceClient, "/api/maintenance/dev/seed", null));
        body.set("dashboard", safePostBody(dashboardClient, "/api/dashboard/dev/seed", null));
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Run TS-01: double room check-in with 3rd floor preference")
    @PostMapping("/api/demo/run/ts-01")
    public ResponseEntity<JsonNode> runTs01() {
        return post(receptionClient, "/api/reception/check-in", checkInRequest("Diana Otayeva", "DOUBLE", 2, 3, "LIFT"));
    }

    @Operation(summary = "Run TS-02: check out room 204")
    @PostMapping("/api/demo/run/ts-02")
    public ResponseEntity<JsonNode> runTs02() {
        return post(receptionClient, "/api/reception/check-out/204", null);
    }

    @Operation(summary = "Run TS-03: clean room 204 after check-out")
    @PostMapping("/api/demo/run/ts-03")
    public ResponseEntity<JsonNode> runTs03() {
        ObjectNode body = objectMapper.createObjectNode();
        body.set("checkout204", safePostBody(receptionClient, "/api/reception/check-out/204", null));
        body.set("ensureQueue", safePostBody(housekeepingClient, "/api/housekeeping/queue/204", null));
        body.set("startCleaning", safePostBody(housekeepingClient, "/api/housekeeping/rooms/204/start", null));
        body.set("markClean", safePostBody(housekeepingClient, "/api/housekeeping/rooms/204/clean", null));
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Run TS-04: room 301 orders coffee and sandwich")
    @PostMapping("/api/demo/run/ts-04")
    public ResponseEntity<JsonNode> runTs04() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("roomNumber", "301");
        ArrayNode items = objectMapper.createArrayNode();
        items.add(item("Coffee", 2, "4.50"));
        items.add(item("Sandwich", 1, "8.00"));
        request.set("items", items);
        JsonNode order = postBody(roomServiceClient, "/api/room-service/orders", request);
        String orderId = order.get("orderId").asText();
        ObjectNode body = objectMapper.createObjectNode();
        body.set("created", order);
        body.set("preparing", postBody(roomServiceClient, "/api/room-service/orders/" + orderId + "/next", null));
        body.set("delivering", postBody(roomServiceClient, "/api/room-service/orders/" + orderId + "/next", null));
        body.set("delivered", postBody(roomServiceClient, "/api/room-service/orders/" + orderId + "/next", null));
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Run TS-05: report critical broken shower in room 115")
    @PostMapping("/api/demo/run/ts-05")
    public ResponseEntity<JsonNode> runTs05() {
        CreateIssueRequest request = new CreateIssueRequest("115", "Broken shower", "CRITICAL");
        return post(maintenanceClient, "/api/maintenance/issues", request);
    }

    @Operation(summary = "Run TS-06: concurrent check-in requests")
    @PostMapping("/api/demo/run/ts-06")
    public ResponseEntity<JsonNode> runTs06() {
        safePostBody(receptionClient, "/api/reception/dev/reset", null);
        CompletableFuture<JsonNode> first = CompletableFuture.supplyAsync(() -> safePostBody(receptionClient,
                "/api/reception/check-in", checkInRequest("Concurrent Guest A", "DOUBLE", 1, null, null)));
        CompletableFuture<JsonNode> second = CompletableFuture.supplyAsync(() -> safePostBody(receptionClient,
                "/api/reception/check-in", checkInRequest("Concurrent Guest B", "DOUBLE", 1, null, null)));
        CompletableFuture.allOf(first, second).join();
        ObjectNode body = objectMapper.createObjectNode();
        body.set("firstResult", first.join());
        body.set("secondResult", second.join());
        body.set("roomsAfter", getBody(receptionClient, "/api/reception/rooms"));
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Run TS-07: no rooms available for requested type")
    @PostMapping("/api/demo/run/ts-07")
    public ResponseEntity<JsonNode> runTs07() {
        safePostBody(receptionClient, "/api/reception/dev/reset", null);
        ObjectNode body = objectMapper.createObjectNode();
        body.set("firstSuiteCheckIn", safePostBody(receptionClient, "/api/reception/check-in", checkInRequest("Suite Guest A", "SUITE", 1, null, null)));
        body.set("secondSuiteCheckInExpectedError", safePostBody(receptionClient, "/api/reception/check-in", checkInRequest("Suite Guest B", "SUITE", 1, null, null)));
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Run TS-08: invalid room number validation")
    @PostMapping("/api/demo/run/ts-08")
    public ResponseEntity<JsonNode> runTs08() {
        ObjectNode body = objectMapper.createObjectNode();
        body.set("invalidRoomCheckOutExpectedError", safePostBody(receptionClient, "/api/reception/check-out/999", null));
        return ResponseEntity.ok(body);
    }

    private CheckInRequest checkInRequest(String guestName, String roomType, int nights, Integer preferredFloor, String proximity) {
        return new CheckInRequest(guestName, roomType, nights, preferredFloor, proximity);
    }

    private ObjectNode item(String name, int quantity, String unitPrice) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("name", name);
        item.put("quantity", quantity);
        item.put("unitPrice", unitPrice);
        return item;
    }

    private JsonNode getBody(RestClient client, String uri) {
        return get(client, uri).getBody();
    }

    private JsonNode postBody(RestClient client, String uri, Object body) {
        return post(client, uri, body).getBody();
    }

    private JsonNode safePostBody(RestClient client, String uri, Object body) {
        try {
            return postBody(client, uri, body);
        } catch (RestClientResponseException ex) {
            return safeError(ex);
        } catch (Exception ex) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Request failed");
            error.put("message", "The demo step could not be completed safely.");
            return error;
        }
    }

    private JsonNode safeError(RestClientResponseException ex) {
        try {
            if (ex.getResponseBodyAsString() != null && !ex.getResponseBodyAsString().isBlank()) {
                return objectMapper.readTree(ex.getResponseBodyAsString());
            }
        } catch (Exception ignored) {
            // Fall through to safe generic body.
        }
        ObjectNode error = objectMapper.createObjectNode();
        error.put("status", ex.getStatusCode().value());
        error.put("error", "Downstream validation error");
        error.put("message", ex.getMessage());
        return error;
    }

    private ResponseEntity<JsonNode> get(RestClient client, String uri) {
        ResponseEntity<JsonNode> downstream = client.get()
                .uri(uri)
                .retrieve()
                .toEntity(JsonNode.class);
        return cleanGatewayResponse(downstream);
    }

    private ResponseEntity<JsonNode> post(RestClient client, String uri, Object body) {
        RestClient.RequestBodySpec request = client.post().uri(uri);
        ResponseEntity<JsonNode> downstream;
        if (body == null) {
            downstream = request.retrieve().toEntity(JsonNode.class);
        } else {
            downstream = request
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(JsonNode.class);
        }
        return cleanGatewayResponse(downstream);
    }

    private ResponseEntity<JsonNode> patch(RestClient client, String uri, Object body) {
        RestClient.RequestBodySpec request = client.patch().uri(uri);
        ResponseEntity<JsonNode> downstream;
        if (body == null) {
            downstream = request.retrieve().toEntity(JsonNode.class);
        } else {
            downstream = request
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(JsonNode.class);
        }
        return cleanGatewayResponse(downstream);
    }

    private ResponseEntity<JsonNode> delete(RestClient client, String uri) {
        ResponseEntity<JsonNode> downstream = client.delete()
                .uri(uri)
                .retrieve()
                .toEntity(JsonNode.class);
        return cleanGatewayResponse(downstream);
    }

    private ResponseEntity<JsonNode> cleanGatewayResponse(ResponseEntity<JsonNode> downstream) {
        return ResponseEntity
                .status(downstream.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(downstream.getBody());
    }
}
