package com.hotelos.gateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hotelos.common.dto.guest.CreateGuestRequest;
import com.hotelos.common.dto.guest.UpdateGuestRequest;
import com.hotelos.gateway.config.ServiceUrls;
import com.hotelos.gateway.dto.CheckInRequest;
import com.hotelos.gateway.dto.CreateIssueRequest;
import com.hotelos.gateway.dto.CreateOrderRequest;
import com.hotelos.gateway.dto.GatewayHealthResponse;
import com.hotelos.gateway.dto.LoginRequest;
import com.hotelos.gateway.dto.LogoutRequest;
import com.hotelos.gateway.dto.RefreshRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public class GatewayController {
    private final RestClient identityClient;
    private final RestClient receptionClient;
    private final RestClient housekeepingClient;
    private final RestClient roomServiceClient;
    private final RestClient maintenanceClient;
    private final RestClient dashboardClient;
    private final ServiceUrls serviceUrls;
    private final ObjectMapper objectMapper;

    public GatewayController(
            @Qualifier("identityClient") RestClient identityClient,
            @Qualifier("receptionClient") RestClient receptionClient,
            @Qualifier("housekeepingClient") RestClient housekeepingClient,
            @Qualifier("roomServiceClient") RestClient roomServiceClient,
            @Qualifier("maintenanceClient") RestClient maintenanceClient,
            @Qualifier("dashboardClient") RestClient dashboardClient,
            ServiceUrls serviceUrls,
            ObjectMapper objectMapper
    ) {
        this.identityClient = identityClient;
        this.receptionClient = receptionClient;
        this.housekeepingClient = housekeepingClient;
        this.roomServiceClient = roomServiceClient;
        this.maintenanceClient = maintenanceClient;
        this.dashboardClient = dashboardClient;
        this.serviceUrls = serviceUrls;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Gateway
    // -------------------------------------------------------------------------
    @Operation(summary = "Gateway health check", tags = {"Gateway"}, security = {})
    @GetMapping("/api/gateway/health")
    public GatewayHealthResponse health() {
        return new GatewayHealthResponse("UP", "gateway-service", "/swagger-ui.html");
    }

    @Operation(summary = "Gateway information", tags = {"Gateway"}, security = {})
    @GetMapping("/api/gateway/info")
    public Map<String, Object> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("project", "HotelOS");
        body.put("description", "Real-time hotel operations system with microservices, RabbitMQ, WebSocket and Swagger Gateway");
        body.put("version", "2.0.0");
        body.put("swagger", "/swagger-ui.html");
        body.put("dashboard", "http://localhost:8085");
        return body;
    }

    @Operation(summary = "List gateway routes", tags = {"Gateway"})
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
        return body;
    }

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------
    @Operation(summary = "Staff login", description = "Authenticate staff credentials and issue access token with refresh token", tags = {"Authentication"}, security = {})
    @PostMapping(value = "/api/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> login(@RequestBody LoginRequest request) {
        return post(identityClient, "/api/auth/login", request);
    }

    @Operation(summary = "Rotate refresh token and issue new access token", description = "Rotate refresh token and issue new access token", tags = {"Authentication"}, security = {})
    @PostMapping(value = "/api/auth/refresh", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> refresh(@RequestBody RefreshRequest request) {
        return post(identityClient, "/api/auth/refresh", request);
    }

    @Operation(summary = "Revoke current login session", description = "Revoke current login session", tags = {"Authentication"}, security = {})
    @PostMapping(value = "/api/auth/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) {
        identityClient.post()
                .uri("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Issue WebSocket ticket for Dashboard", description = "Issue a short-lived one-time WebSocket ticket for ADMIN and MANAGER", tags = {"Authentication"})
    @PostMapping(value = "/api/ws/tickets/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> issueDashboardWsTicket(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        ResponseEntity<JsonNode> response = postWithAuth(identityClient, "/api/ws/tickets/dashboard", null, authHeader);
        return ResponseEntity
                .status(response.getStatusCode())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("Pragma", "no-cache")
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }

    // -------------------------------------------------------------------------
    // Reception
    // -------------------------------------------------------------------------
    @Operation(summary = "Get all rooms", tags = {"Reception"})
    @GetMapping("/api/reception/rooms")
    public ResponseEntity<JsonNode> getRooms() {
        return get(receptionClient, "/api/reception/rooms");
    }

    @Operation(summary = "Get room by room number", tags = {"Reception"})
    @GetMapping("/api/reception/rooms/{roomNumber}")
    public ResponseEntity<JsonNode> getRoom(@PathVariable String roomNumber) {
        return get(receptionClient, "/api/reception/rooms/" + roomNumber);
    }

    @Operation(summary = "Get available rooms", description = "Optional query params: roomType and floor.", tags = {"Reception"})
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

    @Operation(summary = "Check in guest", tags = {"Reception"})
    @PostMapping(value = "/api/reception/check-in", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> checkIn(@RequestBody CheckInRequest request) {
        return post(receptionClient, "/api/reception/check-in", request);
    }

    @Operation(summary = "Check out guest", tags = {"Reception"})
    @PostMapping("/api/reception/check-out/{roomNumber}")
    public ResponseEntity<JsonNode> checkOut(@PathVariable String roomNumber) {
        return post(receptionClient, "/api/reception/check-out/" + roomNumber, null);
    }

    @Operation(summary = "Get active guests", tags = {"Reception"})
    @GetMapping("/api/reception/guests")
    public ResponseEntity<JsonNode> getGuests() {
        return get(receptionClient, "/api/reception/guests");
    }

    @Operation(summary = "Get guest by room", tags = {"Reception"})
    @GetMapping("/api/reception/guests/by-room/{roomNumber}")
    public ResponseEntity<JsonNode> getGuestByRoom(@PathVariable String roomNumber) {
        return get(receptionClient, "/api/reception/guests/by-room/" + roomNumber);
    }

    @Operation(summary = "Archive a guest stay", tags = {"Reception"})
    @PatchMapping("/api/reception/guests/{guestId}/archive")
    public ResponseEntity<JsonNode> archiveGuest(@PathVariable String guestId) {
        return patch(receptionClient, "/api/reception/guests/" + guestId + "/archive", null);
    }

    @Operation(summary = "Calculate bill for room", tags = {"Reception"})
    @PostMapping("/api/reception/bills/{roomNumber}/calculate")
    public ResponseEntity<JsonNode> calculateBill(@PathVariable String roomNumber) {
        return post(receptionClient, "/api/reception/bills/" + roomNumber + "/calculate", null);
    }

    @Operation(summary = "Create guest profile", tags = {"Reception"})
    @PostMapping(value = "/api/reception/guest-profiles", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> createGuestProfile(@RequestBody CreateGuestRequest request) {
        return post(receptionClient, "/api/reception/guest-profiles", request);
    }

    @Operation(summary = "Search guest profiles with pagination and sorting", tags = {"Reception"})
    @GetMapping("/api/reception/guest-profiles")
    public ResponseEntity<JsonNode> getGuestProfiles(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName,asc") String sort
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/reception/guest-profiles")
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sort", sort);
        if (search != null && !search.isBlank()) {
            builder.queryParam("search", search);
        }
        return get(receptionClient, builder.toUriString());
    }

    @Operation(summary = "Get guest profile by ID", tags = {"Reception"})
    @GetMapping("/api/reception/guest-profiles/{id}")
    public ResponseEntity<JsonNode> getGuestProfileById(@PathVariable UUID id) {
        return get(receptionClient, "/api/reception/guest-profiles/" + id);
    }

    @Operation(summary = "Update guest profile", tags = {"Reception"})
    @PutMapping(value = "/api/reception/guest-profiles/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> updateGuestProfile(@PathVariable UUID id, @RequestBody UpdateGuestRequest request) {
        return put(receptionClient, "/api/reception/guest-profiles/" + id, request);
    }

    @Operation(summary = "Create confirmed reservation", tags = {"Reception"})
    @PostMapping(value = "/api/reception/reservations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> createReservation(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody JsonNode request
    ) {
        return postWithHeader(receptionClient, "/api/reception/reservations", request, "Idempotency-Key", idempotencyKey);
    }

    @Operation(summary = "List reservations", tags = {"Reception"})
    @GetMapping("/api/reception/reservations")
    public ResponseEntity<JsonNode> listReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID guestId,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String arrivalFrom,
            @RequestParam(required = false) String arrivalTo,
            @RequestParam(required = false) String departureFrom,
            @RequestParam(required = false) String departureTo,
            @RequestParam(required = false) String assignedRoomNumber
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/reception/reservations")
                .queryParam("page", page)
                .queryParam("size", size);
        addQueryParam(builder, "sort", sort);
        addQueryParam(builder, "status", status);
        if (guestId != null) builder.queryParam("guestId", guestId);
        addQueryParam(builder, "roomType", roomType);
        addQueryParam(builder, "arrivalFrom", arrivalFrom);
        addQueryParam(builder, "arrivalTo", arrivalTo);
        addQueryParam(builder, "departureFrom", departureFrom);
        addQueryParam(builder, "departureTo", departureTo);
        addQueryParam(builder, "assignedRoomNumber", assignedRoomNumber);
        return get(receptionClient, builder.toUriString());
    }

    @Operation(summary = "Get reservation availability", tags = {"Reception"})
    @GetMapping("/api/reception/reservations/availability")
    public ResponseEntity<JsonNode> reservationAvailability(
            @RequestParam String roomType,
            @RequestParam String arrivalDate,
            @RequestParam String departureDate
    ) {
        String uri = UriComponentsBuilder.fromPath("/api/reception/reservations/availability")
                .queryParam("roomType", roomType)
                .queryParam("arrivalDate", arrivalDate)
                .queryParam("departureDate", departureDate)
                .toUriString();
        return get(receptionClient, uri);
    }

    @Operation(summary = "Get reservation by ID", tags = {"Reception"})
    @GetMapping("/api/reception/reservations/{id}")
    public ResponseEntity<JsonNode> getReservation(@PathVariable UUID id) {
        return get(receptionClient, "/api/reception/reservations/" + id);
    }

    @Operation(summary = "Update mutable reservation fields", tags = {"Reception"})
    @PatchMapping(value = "/api/reception/reservations/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> patchReservation(@PathVariable UUID id, @RequestBody JsonNode request) {
        return patch(receptionClient, "/api/reception/reservations/" + id, request);
    }

    @Operation(summary = "Cancel reservation", tags = {"Reception"})
    @PostMapping("/api/reception/reservations/{id}/cancel")
    public ResponseEntity<JsonNode> cancelReservation(@PathVariable UUID id) {
        return post(receptionClient, "/api/reception/reservations/" + id + "/cancel", null);
    }

    @Operation(summary = "Mark reservation no-show", tags = {"Reception"})
    @PostMapping("/api/reception/reservations/{id}/no-show")
    public ResponseEntity<JsonNode> noShowReservation(@PathVariable UUID id) {
        return post(receptionClient, "/api/reception/reservations/" + id + "/no-show", null);
    }

    @Operation(summary = "Assign or change reservation room", tags = {"Reception"})
    @PostMapping(value = "/api/reception/reservations/{id}/assign-room", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> assignReservationRoom(@PathVariable UUID id, @RequestBody JsonNode request) {
        return post(receptionClient, "/api/reception/reservations/" + id + "/assign-room", request);
    }

    @Operation(summary = "Check in confirmed reservation", tags = {"Reception"})
    @PostMapping("/api/reception/reservations/{id}/check-in")
    public ResponseEntity<JsonNode> checkInReservation(@PathVariable UUID id) {
        return post(receptionClient, "/api/reception/reservations/" + id + "/check-in", null);
    }

    @Operation(summary = "List guest stays", tags = {"Reception"})
    @GetMapping("/api/reception/stays")
    public ResponseEntity<JsonNode> listStays(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) UUID guestId,
            @RequestParam(required = false) String roomNumber
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/reception/stays")
                .queryParam("page", page)
                .queryParam("size", size);
        if (sort != null) builder.queryParam("sort", sort);
        if (activeOnly != null) builder.queryParam("activeOnly", activeOnly);
        if (guestId != null) builder.queryParam("guestId", guestId);
        if (roomNumber != null) builder.queryParam("roomNumber", roomNumber);
        return get(receptionClient, builder.build().toUriString());
    }

    @Operation(summary = "Get guest stay details", tags = {"Reception"})
    @GetMapping("/api/reception/stays/{stayId}")
    public ResponseEntity<JsonNode> getStay(@PathVariable UUID stayId) {
        return get(receptionClient, "/api/reception/stays/" + stayId);
    }

    @Operation(summary = "Extend guest stay", tags = {"Reception"})
    @PostMapping(value = "/api/reception/stays/{stayId}/extend", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> extendStay(@PathVariable UUID stayId, @RequestBody JsonNode request) {
        return post(receptionClient, "/api/reception/stays/" + stayId + "/extend", request);
    }

    @Operation(summary = "Change room for guest stay", tags = {"Reception"})
    @PostMapping(value = "/api/reception/stays/{stayId}/change-room", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> changeRoom(@PathVariable UUID stayId, @RequestBody JsonNode request) {
        return post(receptionClient, "/api/reception/stays/" + stayId + "/change-room", request);
    }

    @Operation(summary = "Check out guest stay", tags = {"Reception"})
    @PostMapping("/api/reception/stays/{stayId}/check-out")
    public ResponseEntity<JsonNode> checkOutStay(@PathVariable UUID stayId) {
        return post(receptionClient, "/api/reception/stays/" + stayId + "/check-out", null);
    }

    @Operation(summary = "Get folio for guest stay", tags = {"Reception"})
    @GetMapping("/api/reception/stays/{stayId}/folio")
    public ResponseEntity<JsonNode> getFolioByStay(@PathVariable UUID stayId) {
        return get(receptionClient, "/api/reception/stays/" + stayId + "/folio");
    }

    @Operation(summary = "Get folio by ID", tags = {"Reception"})
    @GetMapping("/api/reception/folios/{folioId}")
    public ResponseEntity<JsonNode> getFolio(@PathVariable UUID folioId) {
        return get(receptionClient, "/api/reception/folios/" + folioId);
    }

    @Operation(summary = "List folio ledger entries", tags = {"Reception"})
    @GetMapping("/api/reception/folios/{folioId}/entries")
    public ResponseEntity<JsonNode> getFolioEntries(
            @PathVariable UUID folioId,
            @RequestParam(required = false) String entryType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/reception/folios/" + folioId + "/entries")
                .queryParam("page", page)
                .queryParam("size", size);
        if (entryType != null) builder.queryParam("entryType", entryType);
        if (sort != null) builder.queryParam("sort", sort);
        return get(receptionClient, builder.build().toUriString());
    }

    @Operation(summary = "Create manual adjustment on folio", tags = {"Reception"})
    @PostMapping(value = "/api/reception/folios/{folioId}/adjustments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> createAdjustment(
            @PathVariable UUID folioId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody JsonNode request
    ) {
        return postWithHeader(receptionClient, "/api/reception/folios/" + folioId + "/adjustments", request,
                "Idempotency-Key", idempotencyKey);
    }

    // -------------------------------------------------------------------------
    // Housekeeping
    // -------------------------------------------------------------------------
    @Operation(summary = "Get housekeeping queue", tags = {"Housekeeping"})
    @GetMapping("/api/housekeeping/queue")
    public ResponseEntity<JsonNode> getHousekeepingQueue() {
        return get(housekeepingClient, "/api/housekeeping/queue");
    }

    @Operation(summary = "Start room cleaning", tags = {"Housekeeping"})
    @PostMapping("/api/housekeeping/rooms/{roomNumber}/start")
    public ResponseEntity<JsonNode> startCleaning(@PathVariable String roomNumber) {
        return post(housekeepingClient, "/api/housekeeping/rooms/" + roomNumber + "/start", null);
    }

    @Operation(summary = "Mark room clean", tags = {"Housekeeping"})
    @PostMapping("/api/housekeeping/rooms/{roomNumber}/clean")
    public ResponseEntity<JsonNode> markRoomClean(@PathVariable String roomNumber) {
        return post(housekeepingClient, "/api/housekeeping/rooms/" + roomNumber + "/clean", null);
    }

    @Operation(summary = "Cancel housekeeping queue item", tags = {"Housekeeping"})
    @PatchMapping("/api/housekeeping/queue/{roomNumber}/cancel")
    public ResponseEntity<JsonNode> cancelCleaning(@PathVariable String roomNumber) {
        return patch(housekeepingClient, "/api/housekeeping/queue/" + roomNumber + "/cancel", null);
    }

    @Operation(summary = "Get cleaners", tags = {"Housekeeping"})
    @GetMapping("/api/housekeeping/cleaners")
    public ResponseEntity<JsonNode> getCleaners() {
        return get(housekeepingClient, "/api/housekeeping/cleaners");
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    // Room Service
    // -------------------------------------------------------------------------
    @Operation(summary = "Get room service orders", tags = {"Room Service"})
    @GetMapping("/api/room-service/orders")
    public ResponseEntity<JsonNode> getRoomServiceOrders() {
        return get(roomServiceClient, "/api/room-service/orders");
    }

    @Operation(summary = "Get room service order by ID", tags = {"Room Service"})
    @GetMapping("/api/room-service/orders/{orderId}")
    public ResponseEntity<JsonNode> getRoomServiceOrder(@PathVariable String orderId) {
        return get(roomServiceClient, "/api/room-service/orders/" + orderId);
    }

    @Operation(summary = "Create room service order", tags = {"Room Service"})
    @PostMapping(value = "/api/room-service/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> createRoomServiceOrder(@RequestBody CreateOrderRequest request) {
        return post(roomServiceClient, "/api/room-service/orders", request);
    }

    @Operation(summary = "Move room service order to next status", tags = {"Room Service"})
    @PatchMapping("/api/room-service/orders/{orderId}/next")
    public ResponseEntity<JsonNode> nextOrderStatus(@PathVariable String orderId) {
        return patch(roomServiceClient, "/api/room-service/orders/" + orderId + "/next", null);
    }

    @Operation(summary = "Cancel room service order", tags = {"Room Service"})
    @PatchMapping("/api/room-service/orders/{orderId}/cancel")
    public ResponseEntity<JsonNode> cancelOrder(@PathVariable String orderId) {
        return patch(roomServiceClient, "/api/room-service/orders/" + orderId + "/cancel", null);
    }

    @Operation(summary = "Get room service charges by room", tags = {"Room Service"})
    @GetMapping("/api/room-service/charges/{roomNumber}")
    public ResponseEntity<JsonNode> getCharges(@PathVariable String roomNumber) {
        return get(roomServiceClient, "/api/room-service/charges/" + roomNumber);
    }

    // -------------------------------------------------------------------------
    // Maintenance
    // -------------------------------------------------------------------------
    @Operation(summary = "Get maintenance issues", tags = {"Maintenance"})
    @GetMapping("/api/maintenance/issues")
    public ResponseEntity<JsonNode> getMaintenanceIssues() {
        return get(maintenanceClient, "/api/maintenance/issues");
    }

    @Operation(summary = "Get maintenance issue by ID", tags = {"Maintenance"})
    @GetMapping("/api/maintenance/issues/{issueId}")
    public ResponseEntity<JsonNode> getMaintenanceIssue(@PathVariable String issueId) {
        return get(maintenanceClient, "/api/maintenance/issues/" + issueId);
    }

    @Operation(summary = "Report maintenance issue", tags = {"Maintenance"})
    @PostMapping(value = "/api/maintenance/issues", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> reportMaintenanceIssue(@RequestBody CreateIssueRequest request) {
        return post(maintenanceClient, "/api/maintenance/issues", request);
    }

    @Operation(summary = "Get maintenance priority queue", tags = {"Maintenance"})
    @GetMapping("/api/maintenance/queue")
    public ResponseEntity<JsonNode> getMaintenanceQueue() {
        return get(maintenanceClient, "/api/maintenance/queue");
    }

    @Operation(summary = "Process next maintenance queue item", tags = {"Maintenance"})
    @PostMapping("/api/maintenance/queue/process-next")
    public ResponseEntity<JsonNode> processNextMaintenance() {
        return post(maintenanceClient, "/api/maintenance/queue/process-next", null);
    }

    @Operation(summary = "Resolve maintenance issue", tags = {"Maintenance"})
    @PatchMapping("/api/maintenance/issues/{issueId}/resolve")
    public ResponseEntity<JsonNode> resolveMaintenanceIssue(@PathVariable String issueId) {
        return patch(maintenanceClient, "/api/maintenance/issues/" + issueId + "/resolve", null);
    }

    @Operation(summary = "Cancel maintenance issue", tags = {"Maintenance"})
    @PatchMapping("/api/maintenance/issues/{issueId}/cancel")
    public ResponseEntity<JsonNode> cancelMaintenanceIssue(@PathVariable String issueId) {
        return patch(maintenanceClient, "/api/maintenance/issues/" + issueId + "/cancel", null);
    }

    @Operation(summary = "Get technicians", tags = {"Maintenance"})
    @GetMapping("/api/maintenance/technicians")
    public ResponseEntity<JsonNode> getTechnicians() {
        return get(maintenanceClient, "/api/maintenance/technicians");
    }

    // -------------------------------------------------------------------------
    // Dashboard
    // -------------------------------------------------------------------------
    @Operation(summary = "Aggregate full operational snapshot", tags = {"Dashboard"})
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

    @Operation(summary = "Get dashboard event history", tags = {"Dashboard"})
    @GetMapping("/api/dashboard/events")
    public ResponseEntity<JsonNode> getDashboardEvents() {
        return get(dashboardClient, "/api/dashboard/events");
    }

    @Operation(summary = "Clear dashboard event history", tags = {"Dashboard"})
    @DeleteMapping("/api/dashboard/events")
    public ResponseEntity<JsonNode> clearDashboardEvents() {
        return delete(dashboardClient, "/api/dashboard/events");
    }

    // -------------------------------------------------------------------------
    // Demo endpoints
    // -------------------------------------------------------------------------
    @Operation(summary = "Development reset", tags = {"Gateway"})
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

    @Operation(summary = "Development seed", tags = {"Gateway"})
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

    @Operation(summary = "Run TS-01 scenario", tags = {"Gateway"})
    @PostMapping("/api/demo/run/ts-01")
    public ResponseEntity<JsonNode> runTs01() {
        return post(receptionClient, "/api/reception/check-in", checkInRequest("Alice Smith", "DOUBLE", 2, 3, "LIFT"));
    }

    @Operation(summary = "Run TS-02 scenario", tags = {"Gateway"})
    @PostMapping("/api/demo/run/ts-02")
    public ResponseEntity<JsonNode> runTs02() {
        return post(receptionClient, "/api/reception/check-out/204", null);
    }

    @Operation(summary = "Run TS-03 scenario", tags = {"Gateway"})
    @PostMapping("/api/demo/run/ts-03")
    public ResponseEntity<JsonNode> runTs03() {
        ObjectNode body = objectMapper.createObjectNode();
        body.set("checkout204", safePostBody(receptionClient, "/api/reception/check-out/204", null));
        body.set("housekeepingQueue", getBody(housekeepingClient, "/api/housekeeping/queue"));
        body.set("startCleaning", safePostBody(housekeepingClient, "/api/housekeeping/rooms/204/start", null));
        body.set("markClean", safePostBody(housekeepingClient, "/api/housekeeping/rooms/204/clean", null));
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Run TS-04 scenario", tags = {"Gateway"})
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
        body.set("preparing", patchBody(roomServiceClient, "/api/room-service/orders/" + orderId + "/next", null));
        body.set("delivering", patchBody(roomServiceClient, "/api/room-service/orders/" + orderId + "/next", null));
        body.set("delivered", patchBody(roomServiceClient, "/api/room-service/orders/" + orderId + "/next", null));
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Run TS-05 scenario", tags = {"Gateway"})
    @PostMapping("/api/demo/run/ts-05")
    public ResponseEntity<JsonNode> runTs05() {
        CreateIssueRequest request = new CreateIssueRequest("115", "Broken shower", "CRITICAL");
        return post(maintenanceClient, "/api/maintenance/issues", request);
    }

    @Operation(summary = "Run TS-06 scenario", tags = {"Gateway"})
    @PostMapping("/api/demo/run/ts-06")
    public ResponseEntity<JsonNode> runTs06(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        final String explicitAuth = authHeader;
        safePostBodyWithAuth(receptionClient, "/api/reception/dev/reset", null, explicitAuth);
        CompletableFuture<JsonNode> first = CompletableFuture.supplyAsync(() -> safePostBodyWithAuth(receptionClient,
                "/api/reception/check-in", checkInRequest("Concurrent Guest A", "DOUBLE", 1, null, null), explicitAuth));
        CompletableFuture<JsonNode> second = CompletableFuture.supplyAsync(() -> safePostBodyWithAuth(receptionClient,
                "/api/reception/check-in", checkInRequest("Concurrent Guest B", "DOUBLE", 1, null, null), explicitAuth));
        CompletableFuture.allOf(first, second).join();
        ObjectNode body = objectMapper.createObjectNode();
        body.set("firstResult", first.join());
        body.set("secondResult", second.join());
        body.set("roomsAfter", getBodyWithAuth(receptionClient, "/api/reception/rooms", explicitAuth));
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Run TS-07 scenario", tags = {"Gateway"})
    @PostMapping("/api/demo/run/ts-07")
    public ResponseEntity<JsonNode> runTs07() {
        safePostBody(receptionClient, "/api/reception/dev/reset", null);
        ObjectNode body = objectMapper.createObjectNode();
        body.set("firstSuiteCheckIn", safePostBody(receptionClient, "/api/reception/check-in", checkInRequest("Suite Guest A", "SUITE", 1, null, null)));
        body.set("secondSuiteCheckInExpectedError", safePostBody(receptionClient, "/api/reception/check-in", checkInRequest("Suite Guest B", "SUITE", 1, null, null)));
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Run TS-08 scenario", tags = {"Gateway"})
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

    private JsonNode patchBody(RestClient client, String uri, Object body) {
        return patch(client, uri, body).getBody();
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

    private ResponseEntity<JsonNode> postWithHeader(RestClient client, String uri, Object body,
                                                    String headerName, String headerValue) {
        RestClient.RequestBodySpec request = client.post().uri(uri);
        if (headerValue != null) {
            request.header(headerName, headerValue);
        }
        ResponseEntity<JsonNode> downstream = request
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(JsonNode.class);
        return cleanGatewayResponse(downstream);
    }

    private void addQueryParam(UriComponentsBuilder builder, String name, String value) {
        if (value != null && !value.isBlank()) {
            builder.queryParam(name, value);
        }
    }

    private ResponseEntity<JsonNode> put(RestClient client, String uri, Object body) {
        RestClient.RequestBodySpec request = client.put().uri(uri);
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
        ResponseEntity.BodyBuilder builder = ResponseEntity
                .status(downstream.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON);
        URI location = downstream.getHeaders().getLocation();
        if (location != null) {
            builder.location(sanitizeLocation(location));
        }
        return builder.body(downstream.getBody());
    }

    private URI sanitizeLocation(URI location) {
        if (location == null) {
            return null;
        }
        String path = location.getRawPath();
        if (path == null || path.isBlank()) {
            return location;
        }
        if (location.getRawQuery() != null && !location.getRawQuery().isBlank()) {
            path += "?" + location.getRawQuery();
        }
        return URI.create(path);
    }

    private JsonNode getBodyWithAuth(RestClient client, String uri, String authHeader) {
        return getWithAuth(client, uri, authHeader).getBody();
    }

    private ResponseEntity<JsonNode> getWithAuth(RestClient client, String uri, String authHeader) {
        RestClient.RequestHeadersSpec<?> request = client.get().uri(uri);
        if (authHeader != null && !authHeader.isBlank()) {
            request.header(HttpHeaders.AUTHORIZATION, authHeader);
        }
        ResponseEntity<JsonNode> downstream = request
                .retrieve()
                .toEntity(JsonNode.class);
        return cleanGatewayResponse(downstream);
    }

    private JsonNode safePostBodyWithAuth(RestClient client, String uri, Object body, String authHeader) {
        try {
            return postBodyWithAuth(client, uri, body, authHeader);
        } catch (RestClientResponseException ex) {
            return safeError(ex);
        } catch (Exception ex) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Request failed");
            error.put("message", "The demo step could not be completed safely.");
            return error;
        }
    }

    private JsonNode postBodyWithAuth(RestClient client, String uri, Object body, String authHeader) {
        return postWithAuth(client, uri, body, authHeader).getBody();
    }

    private ResponseEntity<JsonNode> postWithAuth(RestClient client, String uri, Object body, String authHeader) {
        RestClient.RequestBodySpec request = client.post().uri(uri);
        if (authHeader != null && !authHeader.isBlank()) {
            request.header(HttpHeaders.AUTHORIZATION, authHeader);
        }
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
}
