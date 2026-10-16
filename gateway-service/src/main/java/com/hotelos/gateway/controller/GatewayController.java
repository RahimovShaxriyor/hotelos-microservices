package com.hotelos.gateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hotelos.common.dto.guest.CreateGuestRequest;
import com.hotelos.common.dto.guest.GuestProfileResponse;
import com.hotelos.common.dto.guest.UpdateGuestRequest;
import com.hotelos.gateway.config.ServiceUrls;
import com.hotelos.gateway.dto.CheckInRequest;
import com.hotelos.gateway.dto.CreateIssueRequest;
import com.hotelos.gateway.dto.CreateOrderRequest;
import com.hotelos.gateway.dto.GatewayHealthResponse;
import com.hotelos.gateway.dto.LoginRequest;
import com.hotelos.gateway.dto.LogoutRequest;
import com.hotelos.gateway.dto.RefreshRequest;
import com.hotelos.gateway.openapi.schema.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
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
    // System / Gateway
    // -------------------------------------------------------------------------
    @Operation(
            operationId = "getGatewayHealth",
            summary = "Gateway health check",
            tags = {"System"},
            description = "Public"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "UP", content = @Content(schema = @Schema(implementation = GatewayHealthResponse.class)))
    })
    @GetMapping("/api/gateway/health")
    public GatewayHealthResponse health() {
        return new GatewayHealthResponse("UP", "gateway-service", "/swagger-ui.html");
    }

    @Operation(
            operationId = "getGatewayInfo",
            summary = "Gateway information",
            tags = {"System"},
            description = "Public"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gateway metadata")
    })
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

    @Operation(
            operationId = "listGatewayRoutes",
            summary = "List gateway routes",
            tags = {"System"},
            description = "Roles: ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gateway route table")
    })
    @GetMapping("/api/gateway/routes")
    public Map<String, Object> routes() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("gateway", "/api/gateway/**");
        body.put("auth", "/api/auth/**");
        body.put("admin", "/api/admin/**");
        body.put("reception", serviceUrls.receptionUrl() + "/api/reception/**");
        body.put("housekeeping", serviceUrls.housekeepingUrl() + "/api/housekeeping/**");
        body.put("roomService", serviceUrls.roomServiceUrl() + "/api/room-service/**");
        body.put("maintenance", serviceUrls.maintenanceUrl() + "/api/maintenance/**");
        body.put("dashboard", serviceUrls.dashboardUrl() + "/api/dashboard/**");
        return body;
    }

    // -------------------------------------------------------------------------
    // Authentication
    // -------------------------------------------------------------------------
    @Operation(
            operationId = "login",
            summary = "Staff login and token issuance",
            tags = {"Authentication"},
            description = "Public"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS | ACCOUNT_DISABLED", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> login(@RequestBody LoginRequest request) {
        return post(identityClient, "/api/auth/login", request);
    }

    @Operation(
            operationId = "refreshToken",
            summary = "Refresh staff access token",
            tags = {"Authentication"},
            description = "Public"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed", content = @Content(schema = @Schema(implementation = RefreshResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "401", description = "INVALID_TOKEN | TOKEN_EXPIRED | TOKEN_REVOKED", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/auth/refresh", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> refresh(@RequestBody RefreshRequest request) {
        return post(identityClient, "/api/auth/refresh", request);
    }

    @Operation(
            operationId = "logout",
            summary = "Revoke refresh session",
            tags = {"Authentication"},
            description = "Public"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
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

    @Operation(
            operationId = "acceptStaffInvitation",
            summary = "Accept staff invitation and activate account",
            tags = {"Authentication"},
            description = "Public"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Invitation acceptance payload",
            required = true,
            content = @Content(schema = @Schema(implementation = AcceptStaffInvitationRequest.class))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invitation accepted and account activated", content = @Content(schema = @Schema(implementation = StaffUserSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR | INVALID_OR_EXPIRED_INVITATION | PASSWORD_TOO_WEAK", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "INVITATION_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "INVITATION_ALREADY_ACCEPTED | USER_ALREADY_EXISTS", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/auth/invitations/accept", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> acceptInvitation(@RequestBody JsonNode request) {
        return post(identityClient, "/api/auth/invitations/accept", request);
    }

    // -------------------------------------------------------------------------
    // Staff Administration & Governance
    // -------------------------------------------------------------------------
    @Operation(
            operationId = "listStaffUsers",
            summary = "List staff accounts",
            tags = {"Staff Administration"},
            description = "Roles: ADMIN, OWNER"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged staff users list", content = @Content(schema = @Schema(implementation = StaffUserPageResponse.class)))
    })
    @GetMapping(value = "/api/admin/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> listStaffUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/admin/users")
                .queryParam("page", page)
                .queryParam("size", size);
        if (status != null && !status.isBlank()) builder.queryParam("status", status.trim());
        if (role != null && !role.isBlank()) builder.queryParam("role", role.trim());
        if (search != null && !search.isBlank()) builder.queryParam("search", search.trim());
        return getWithAuth(identityClient, builder.toUriString(), authHeader);
    }

    @Operation(
            operationId = "getStaffUser",
            summary = "Get staff account detail",
            tags = {"Staff Administration"},
            description = "Roles: ADMIN, OWNER"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff user detail", content = @Content(schema = @Schema(implementation = StaffUserDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping(value = "/api/admin/users/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getStaffUserDetail(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        return getWithAuth(identityClient, "/api/admin/users/" + id, authHeader);
    }

    @Operation(
            operationId = "createStaffInvitation",
            summary = "Create staff invitation",
            tags = {"Staff Administration"},
            description = "Roles: ADMIN, OWNER"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Staff invitation payload",
            required = true,
            content = @Content(schema = @Schema(implementation = CreateStaffInvitationRequest.class))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invitation created", content = @Content(schema = @Schema(implementation = InvitationCreatedResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR | ROLE_NOT_ASSIGNABLE | CANNOT_INVITE_ROLE", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "USER_ALREADY_EXISTS | INVITATION_ALREADY_ACTIVE", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/admin/users/invitations", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> createStaffInvitation(
            @RequestBody JsonNode request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        return postWithAuth(identityClient, "/api/admin/users/invitations", request, authHeader);
    }

    @Operation(
            operationId = "resendStaffInvitation",
            summary = "Resend staff invitation",
            tags = {"Staff Administration"},
            description = "Roles: ADMIN, OWNER"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitation resent", content = @Content(schema = @Schema(implementation = InvitationCreatedResponse.class))),
            @ApiResponse(responseCode = "404", description = "INVITATION_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "INVITATION_NOT_ACTIVE | INVITATION_ALREADY_ACCEPTED", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/admin/users/invitations/{id}/resend", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> resendStaffInvitation(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        return postWithAuth(identityClient, "/api/admin/users/invitations/" + id + "/resend", null, authHeader);
    }

    @Operation(
            operationId = "disableStaffUser",
            summary = "Disable staff user",
            tags = {"Staff Administration"},
            description = "Roles: ADMIN, OWNER"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Optional disable reason payload",
            required = false,
            content = @Content(schema = @Schema(implementation = DisableStaffUserRequest.class))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff user disabled", content = @Content(schema = @Schema(implementation = StaffUserSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "CANNOT_DISABLE_SELF", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "ACCOUNT_ALREADY_DISABLED | CANNOT_DISABLE_LAST_ADMIN | CANNOT_DISABLE_LAST_OWNER", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/admin/users/{id}/disable", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> disableStaffUser(
            @PathVariable UUID id,
            @RequestBody(required = false) JsonNode request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        return postWithAuth(identityClient, "/api/admin/users/" + id + "/disable", request, authHeader);
    }

    @Operation(
            operationId = "enableStaffUser",
            summary = "Enable staff user",
            tags = {"Staff Administration"},
            description = "Roles: ADMIN, OWNER"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff user enabled", content = @Content(schema = @Schema(implementation = StaffUserSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "ACCOUNT_ALREADY_ACTIVE", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/admin/users/{id}/enable", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> enableStaffUser(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        return postWithAuth(identityClient, "/api/admin/users/" + id + "/enable", null, authHeader);
    }

    @Operation(
            operationId = "updateStaffRoles",
            summary = "Update staff roles",
            tags = {"Staff Administration"},
            description = "Roles: ADMIN, OWNER"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "New roles replacement payload",
            required = true,
            content = @Content(schema = @Schema(implementation = UpdateStaffRolesRequest.class))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff roles updated", content = @Content(schema = @Schema(implementation = StaffUserDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR | CANNOT_DEMOTE_SELF | ROLE_NOT_ASSIGNABLE", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "CANNOT_REMOVE_LAST_ADMIN | CANNOT_REMOVE_LAST_OWNER", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PutMapping(value = "/api/admin/users/{id}/roles", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> updateStaffRoles(
            @PathVariable UUID id,
            @RequestBody JsonNode request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        return putWithAuth(identityClient, "/api/admin/users/" + id + "/roles", request, authHeader);
    }

    @Operation(
            operationId = "revokeStaffSessions",
            summary = "Revoke staff sessions",
            tags = {"Staff Administration"},
            description = "Roles: ADMIN, OWNER"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/admin/users/{id}/revoke-sessions")
    public ResponseEntity<Void> revokeStaffSessions(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        RestClient.RequestBodySpec request = identityClient.post().uri("/api/admin/users/" + id + "/revoke-sessions");
        if (authHeader != null && !authHeader.isBlank()) {
            request.header(HttpHeaders.AUTHORIZATION, authHeader);
        }
        request.retrieve().toBodilessEntity();
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Dashboard
    // -------------------------------------------------------------------------
    @Operation(
            operationId = "issueDashboardWsTicket",
            summary = "Issue dashboard WebSocket ticket",
            tags = {"Dashboard"},
            description = "Roles: ADMIN, MANAGER"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "WebSocket ticket issued", content = @Content(schema = @Schema(implementation = WebSocketTicketResponse.class)))
    })
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

    @Operation(
            operationId = "getDashboardSnapshot",
            summary = "Aggregate full operational snapshot",
            tags = {"Dashboard"},
            description = "Roles: ADMIN, MANAGER"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operational snapshot", content = @Content(schema = @Schema(implementation = DashboardSnapshotResponse.class)))
    })
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

    @Operation(
            operationId = "listDashboardEvents",
            summary = "Get dashboard event history",
            tags = {"Dashboard"},
            description = "Roles: ADMIN, MANAGER"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event feed", content = @Content(schema = @Schema(implementation = EventFeedResponse.class)))
    })
    @GetMapping("/api/dashboard/events")
    public ResponseEntity<JsonNode> getDashboardEvents() {
        return get(dashboardClient, "/api/dashboard/events");
    }

    @Operation(
            operationId = "clearDashboardEvents",
            summary = "Clear dashboard event history",
            tags = {"Dashboard"},
            description = "Roles: ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cleared event feed", content = @Content(schema = @Schema(implementation = EventFeedResponse.class)))
    })
    @DeleteMapping("/api/dashboard/events")
    public ResponseEntity<JsonNode> clearDashboardEvents() {
        return delete(dashboardClient, "/api/dashboard/events");
    }

    @Operation(
            operationId = "triggerDashboardBootstrap",
            summary = "Trigger dashboard persistent read-model bootstrap",
            tags = {"Dashboard"},
            description = "Roles: ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bootstrap result", content = @Content(schema = @Schema(implementation = BootstrapResultResponse.class)))
    })
    @PostMapping("/api/dashboard/bootstrap")
    public ResponseEntity<JsonNode> triggerDashboardBootstrap() {
        return post(dashboardClient, "/api/dashboard/bootstrap", null);
    }

    // -------------------------------------------------------------------------
    // Reception — Rooms
    // -------------------------------------------------------------------------
    @Operation(
            operationId = "listRooms",
            summary = "Get all rooms",
            tags = {"Reception — Rooms"},
            description = "Roles: ALL_STAFF"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoomResponse.class))))
    })
    @GetMapping("/api/reception/rooms")
    public ResponseEntity<JsonNode> getRooms() {
        return get(receptionClient, "/api/reception/rooms");
    }

    @Operation(
            operationId = "getRoom",
            summary = "Get room by room number",
            tags = {"Reception — Rooms"},
            description = "Roles: ALL_STAFF"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room detail", content = @Content(schema = @Schema(implementation = RoomResponse.class))),
            @ApiResponse(responseCode = "404", description = "ROOM_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/api/reception/rooms/{roomNumber}")
    public ResponseEntity<JsonNode> getRoom(@PathVariable String roomNumber) {
        return get(receptionClient, "/api/reception/rooms/" + roomNumber);
    }

    @Operation(
            operationId = "listAvailableRooms",
            summary = "Get available rooms",
            tags = {"Reception — Rooms"},
            description = "Roles: ALL_STAFF"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Available rooms list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoomResponse.class))))
    })
    @GetMapping("/api/reception/rooms/available")
    public ResponseEntity<JsonNode> getAvailableRooms(
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) Integer floor
    ) {
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

    // -------------------------------------------------------------------------
    // Reception — Stays
    // -------------------------------------------------------------------------
    @Operation(
            operationId = "checkInGuest",
            summary = "Check in guest (direct walk-in)",
            tags = {"Reception — Stays"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guest checked in", content = @Content(schema = @Schema(implementation = CheckInResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "GUEST_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "NO_INVENTORY | NO_ROOM_READY | CONCURRENT_STATE_CHANGE", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/reception/check-in", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> checkIn(@RequestBody CheckInRequest request) {
        return post(receptionClient, "/api/reception/check-in", request);
    }

    @Operation(
            operationId = "checkOutGuest",
            summary = "Check out guest by room number",
            tags = {"Reception — Stays"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guest checked out", content = @Content(schema = @Schema(implementation = CheckOutResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "NO_ACTIVE_STAY", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "PENDING_ROOM_SERVICE_ORDERS | STAY_ALREADY_CHECKED_OUT | FREEZE_CONFLICT", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping("/api/reception/check-out/{roomNumber}")
    public ResponseEntity<JsonNode> checkOut(@PathVariable String roomNumber) {
        return post(receptionClient, "/api/reception/check-out/" + roomNumber, null);
    }

    @Operation(
            operationId = "listStays",
            summary = "List guest stays",
            tags = {"Reception — Stays"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged stays list", content = @Content(schema = @Schema(implementation = StayPageResponse.class)))
    })
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

    @Operation(
            operationId = "getStay",
            summary = "Get guest stay details",
            tags = {"Reception — Stays"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stay detail", content = @Content(schema = @Schema(implementation = StayResponse.class))),
            @ApiResponse(responseCode = "404", description = "STAY_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/api/reception/stays/{stayId}")
    public ResponseEntity<JsonNode> getStay(@PathVariable UUID stayId) {
        return get(receptionClient, "/api/reception/stays/" + stayId);
    }

    @Operation(
            operationId = "extendStay",
            summary = "Extend guest stay",
            tags = {"Reception — Stays"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Stay extension request payload",
            required = true,
            content = @Content(schema = @Schema(implementation = ExtendStayRequest.class))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stay extended", content = @Content(schema = @Schema(implementation = StayResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR | INVALID_EXTENSION_DATE", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "STAY_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "NO_INVENTORY | STAY_NOT_ACTIVE | CONCURRENT_STATE_CHANGE", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/reception/stays/{stayId}/extend", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> extendStay(@PathVariable UUID stayId, @RequestBody JsonNode request) {
        return post(receptionClient, "/api/reception/stays/" + stayId + "/extend", request);
    }

    @Operation(
            operationId = "changeStayRoom",
            summary = "Change room for guest stay",
            tags = {"Reception — Stays"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Room transfer request payload",
            required = true,
            content = @Content(schema = @Schema(implementation = ChangeRoomRequest.class))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room changed", content = @Content(schema = @Schema(implementation = StayResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR | SAME_ROOM_TRANSFER", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "STAY_NOT_FOUND | ROOM_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "ROOM_NOT_READY | ROOM_OCCUPIED | STAY_NOT_ACTIVE | CONCURRENT_STATE_CHANGE", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/reception/stays/{stayId}/change-room", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> changeRoom(@PathVariable UUID stayId, @RequestBody JsonNode request) {
        return post(receptionClient, "/api/reception/stays/" + stayId + "/change-room", request);
    }

    @Operation(
            operationId = "checkOutStayById",
            summary = "Check out guest stay by stay ID",
            tags = {"Reception — Stays"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stay checked out", content = @Content(schema = @Schema(implementation = CheckOutResponse.class))),
            @ApiResponse(responseCode = "404", description = "STAY_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "PENDING_ROOM_SERVICE_ORDERS | STAY_ALREADY_CHECKED_OUT | FREEZE_CONFLICT", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping("/api/reception/stays/{stayId}/check-out")
    public ResponseEntity<JsonNode> checkOutStay(@PathVariable UUID stayId) {
        return post(receptionClient, "/api/reception/stays/" + stayId + "/check-out", null);
    }

    // -------------------------------------------------------------------------
    // Reception — Guests
    // -------------------------------------------------------------------------
    @Operation(
            operationId = "listGuests",
            summary = "Get active guests",
            tags = {"Reception — Guests"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active guest stays list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = GuestStayResponse.class))))
    })
    @GetMapping("/api/reception/guests")
    public ResponseEntity<JsonNode> getGuests() {
        return get(receptionClient, "/api/reception/guests");
    }

    @Operation(
            operationId = "getGuestByRoom",
            summary = "Get guest by room",
            tags = {"Reception — Guests"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guest stay detail", content = @Content(schema = @Schema(implementation = GuestStayResponse.class))),
            @ApiResponse(responseCode = "404", description = "NO_ACTIVE_STAY", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/api/reception/guests/by-room/{roomNumber}")
    public ResponseEntity<JsonNode> getGuestByRoom(@PathVariable String roomNumber) {
        return get(receptionClient, "/api/reception/guests/by-room/" + roomNumber);
    }

    @Operation(
            operationId = "archiveGuestStay",
            summary = "Archive a guest stay",
            tags = {"Reception — Guests"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guest stay archived", content = @Content(schema = @Schema(implementation = GuestStayResponse.class))),
            @ApiResponse(responseCode = "404", description = "GUEST_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PatchMapping("/api/reception/guests/{guestId}/archive")
    public ResponseEntity<JsonNode> archiveGuest(@PathVariable String guestId) {
        return patch(receptionClient, "/api/reception/guests/" + guestId + "/archive", null);
    }

    @Operation(
            operationId = "createGuestProfile",
            summary = "Create guest profile",
            tags = {"Reception — Guests"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Guest profile created", content = @Content(schema = @Schema(implementation = GuestProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "GUEST_ALREADY_EXISTS", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/reception/guest-profiles", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> createGuestProfile(@RequestBody CreateGuestRequest request) {
        return post(receptionClient, "/api/reception/guest-profiles", request);
    }

    @Operation(
            operationId = "listGuestProfiles",
            summary = "Search guest profiles with pagination and sorting",
            tags = {"Reception — Guests"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged guest profiles list", content = @Content(schema = @Schema(implementation = GuestProfilePageResponse.class)))
    })
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

    @Operation(
            operationId = "getGuestProfile",
            summary = "Get guest profile by ID",
            tags = {"Reception — Guests"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guest profile detail", content = @Content(schema = @Schema(implementation = GuestProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "GUEST_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/api/reception/guest-profiles/{id}")
    public ResponseEntity<JsonNode> getGuestProfileById(@PathVariable UUID id) {
        return get(receptionClient, "/api/reception/guest-profiles/" + id);
    }

    @Operation(
            operationId = "updateGuestProfile",
            summary = "Update guest profile",
            tags = {"Reception — Guests"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guest profile updated", content = @Content(schema = @Schema(implementation = GuestProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "GUEST_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "GUEST_ALREADY_EXISTS", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PutMapping(value = "/api/reception/guest-profiles/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> updateGuestProfile(@PathVariable UUID id, @RequestBody UpdateGuestRequest request) {
        return put(receptionClient, "/api/reception/guest-profiles/" + id, request);
    }

    // -------------------------------------------------------------------------
    // Reception — Reservations
    // -------------------------------------------------------------------------
    @Operation(
            operationId = "createReservation",
            summary = "Create confirmed reservation",
            tags = {"Reception — Reservations"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Reservation creation request payload",
            required = true,
            content = @Content(schema = @Schema(implementation = CreateReservationRequest.class))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reservation created", content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "GUEST_NOT_FOUND | ROOM_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "NO_INVENTORY | ROOM_TYPE_MISMATCH | ROOM_OUT_OF_ORDER | ROOM_ALREADY_ALLOCATED | IDEMPOTENCY_CONFLICT", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/reception/reservations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> createReservation(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody JsonNode request
    ) {
        return postWithHeader(receptionClient, "/api/reception/reservations", request, "Idempotency-Key", idempotencyKey);
    }

    @Operation(
            operationId = "listReservations",
            summary = "List reservations",
            tags = {"Reception — Reservations"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged reservations list", content = @Content(schema = @Schema(implementation = ReservationPageResponse.class)))
    })
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

    @Operation(
            operationId = "checkReservationAvailability",
            summary = "Get reservation availability",
            tags = {"Reception — Reservations"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability search results", content = @Content(schema = @Schema(implementation = AvailabilityResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
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

    @Operation(
            operationId = "getReservation",
            summary = "Get reservation by ID",
            tags = {"Reception — Reservations"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation detail", content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "RESERVATION_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/api/reception/reservations/{id}")
    public ResponseEntity<JsonNode> getReservation(@PathVariable UUID id) {
        return get(receptionClient, "/api/reception/reservations/" + id);
    }

    @Operation(
            operationId = "patchReservation",
            summary = "Update mutable reservation fields",
            tags = {"Reception — Reservations"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Reservation patch payload",
            required = true,
            content = @Content(schema = @Schema(implementation = PatchReservationRequest.class))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation updated", content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "RESERVATION_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "RESERVATION_NOT_MUTABLE | NO_INVENTORY | ROOM_ALREADY_ALLOCATED | IDEMPOTENCY_CONFLICT | CONCURRENT_STATE_CHANGE", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PatchMapping(value = "/api/reception/reservations/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> patchReservation(@PathVariable UUID id, @RequestBody JsonNode request) {
        return patch(receptionClient, "/api/reception/reservations/" + id, request);
    }

    @Operation(
            operationId = "cancelReservation",
            summary = "Cancel reservation",
            tags = {"Reception — Reservations"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation cancelled", content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "RESERVATION_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "INVALID_RESERVATION_TRANSITION | CONCURRENT_STATE_CHANGE", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping("/api/reception/reservations/{id}/cancel")
    public ResponseEntity<JsonNode> cancelReservation(@PathVariable UUID id) {
        return post(receptionClient, "/api/reception/reservations/" + id + "/cancel", null);
    }

    @Operation(
            operationId = "noShowReservation",
            summary = "Mark reservation no-show",
            tags = {"Reception — Reservations"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation marked no-show", content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "RESERVATION_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "INVALID_RESERVATION_TRANSITION | NO_SHOW_TOO_EARLY | CONCURRENT_STATE_CHANGE", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping("/api/reception/reservations/{id}/no-show")
    public ResponseEntity<JsonNode> noShowReservation(@PathVariable UUID id) {
        return post(receptionClient, "/api/reception/reservations/" + id + "/no-show", null);
    }

    @Operation(
            operationId = "assignReservationRoom",
            summary = "Assign or change reservation room",
            tags = {"Reception — Reservations"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Room assignment request payload",
            required = true,
            content = @Content(schema = @Schema(implementation = AssignRoomRequest.class))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room assigned to reservation", content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "RESERVATION_NOT_FOUND | ROOM_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "ROOM_TYPE_MISMATCH | ROOM_OUT_OF_ORDER | ROOM_ALREADY_ALLOCATED | RESERVATION_NOT_MUTABLE", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/reception/reservations/{id}/assign-room", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> assignReservationRoom(@PathVariable UUID id, @RequestBody JsonNode request) {
        return post(receptionClient, "/api/reception/reservations/" + id + "/assign-room", request);
    }

    @Operation(
            operationId = "checkInReservation",
            summary = "Check in confirmed reservation",
            tags = {"Reception — Reservations"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Stay created from reservation", content = @Content(schema = @Schema(implementation = StayResponse.class))),
            @ApiResponse(responseCode = "404", description = "RESERVATION_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "INVALID_RESERVATION_TRANSITION | EARLY_CHECKIN_NOT_ALLOWED | PAST_DEPARTURE_CHECKIN | ASSIGNED_ROOM_NOT_READY | NO_ROOM_READY | CONCURRENT_STATE_CHANGE", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping("/api/reception/reservations/{id}/check-in")
    public ResponseEntity<JsonNode> checkInReservation(@PathVariable UUID id) {
        return post(receptionClient, "/api/reception/reservations/" + id + "/check-in", null);
    }

    // -------------------------------------------------------------------------
    // Reception — Folio
    // -------------------------------------------------------------------------
    @Operation(
            operationId = "calculateRoomBill",
            summary = "Calculate bill for room",
            tags = {"Reception — Folio"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Calculated bill summary", content = @Content(schema = @Schema(implementation = BillCalculationResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "ROOM_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping("/api/reception/bills/{roomNumber}/calculate")
    public ResponseEntity<JsonNode> calculateBill(@PathVariable String roomNumber) {
        return post(receptionClient, "/api/reception/bills/" + roomNumber + "/calculate", null);
    }

    @Operation(
            operationId = "getFolioByStay",
            summary = "Get folio for guest stay",
            tags = {"Reception — Folio"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Folio summary", content = @Content(schema = @Schema(implementation = FolioResponse.class))),
            @ApiResponse(responseCode = "404", description = "STAY_NOT_FOUND | FOLIO_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/api/reception/stays/{stayId}/folio")
    public ResponseEntity<JsonNode> getFolioByStay(@PathVariable UUID stayId) {
        return get(receptionClient, "/api/reception/stays/" + stayId + "/folio");
    }

    @Operation(
            operationId = "getFolio",
            summary = "Get folio by ID",
            tags = {"Reception — Folio"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Folio summary", content = @Content(schema = @Schema(implementation = FolioResponse.class))),
            @ApiResponse(responseCode = "404", description = "FOLIO_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/api/reception/folios/{folioId}")
    public ResponseEntity<JsonNode> getFolio(@PathVariable UUID folioId) {
        return get(receptionClient, "/api/reception/folios/" + folioId);
    }

    @Operation(
            operationId = "listFolioEntries",
            summary = "List folio ledger entries",
            tags = {"Reception — Folio"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged folio ledger entries", content = @Content(schema = @Schema(implementation = FolioEntryPageResponse.class))),
            @ApiResponse(responseCode = "404", description = "FOLIO_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
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

    @Operation(
            operationId = "createFolioAdjustment",
            summary = "Create manual adjustment on folio",
            tags = {"Reception — Folio"},
            description = "Roles: RECEPTIONIST, MANAGER, ADMIN"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Folio adjustment request payload",
            required = true,
            content = @Content(schema = @Schema(implementation = CreateAdjustmentRequest.class))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Adjustment posted", content = @Content(schema = @Schema(implementation = FolioEntryResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR | INVALID_ADJUSTMENT_AMOUNT", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "FOLIO_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "FOLIO_CLOSED | IDEMPOTENCY_CONFLICT", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
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
    @Operation(
            operationId = "getHousekeepingQueue",
            summary = "Get housekeeping queue",
            tags = {"Housekeeping"},
            description = "Roles: HOUSEKEEPING, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Turnover cleaning queue", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CleaningTaskResponse.class))))
    })
    @GetMapping("/api/housekeeping/queue")
    public ResponseEntity<JsonNode> getHousekeepingQueue() {
        return get(housekeepingClient, "/api/housekeeping/queue");
    }

    @Operation(
            operationId = "enqueueHousekeepingTask",
            summary = "Add room to housekeeping queue",
            tags = {"Housekeeping"},
            description = "Roles: HOUSEKEEPING, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task added to queue", content = @Content(schema = @Schema(implementation = CleaningTaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "ROOM_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "TASK_ALREADY_QUEUED", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping("/api/housekeeping/queue/{roomNumber}")
    public ResponseEntity<JsonNode> addToHousekeepingQueue(@PathVariable String roomNumber) {
        return post(housekeepingClient, "/api/housekeeping/queue/" + roomNumber, null);
    }

    @Operation(
            operationId = "startHousekeepingCleaning",
            summary = "Start room cleaning",
            tags = {"Housekeeping"},
            description = "Roles: HOUSEKEEPING, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cleaning started", content = @Content(schema = @Schema(implementation = CleaningTaskResponse.class))),
            @ApiResponse(responseCode = "404", description = "ROOM_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "ROOM_NOT_IN_DIRTY_STATE", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping("/api/housekeeping/rooms/{roomNumber}/start")
    public ResponseEntity<JsonNode> startCleaning(@PathVariable String roomNumber) {
        return post(housekeepingClient, "/api/housekeeping/rooms/" + roomNumber + "/start", null);
    }

    @Operation(
            operationId = "completeHousekeepingCleaning",
            summary = "Mark room clean",
            tags = {"Housekeeping"},
            description = "Roles: HOUSEKEEPING, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room marked clean", content = @Content(schema = @Schema(implementation = CleaningTaskResponse.class))),
            @ApiResponse(responseCode = "404", description = "ROOM_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "ROOM_NOT_IN_CLEANING_STATE", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping("/api/housekeeping/rooms/{roomNumber}/clean")
    public ResponseEntity<JsonNode> markRoomClean(@PathVariable String roomNumber) {
        return post(housekeepingClient, "/api/housekeeping/rooms/" + roomNumber + "/clean", null);
    }

    @Operation(
            operationId = "reportHousekeepingDefect",
            summary = "Report defect from housekeeping",
            tags = {"Housekeeping"},
            description = "Roles: HOUSEKEEPING, MANAGER, ADMIN"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Defect reporting payload",
            required = true,
            content = @Content(schema = @Schema(implementation = ReportDefectRequest.class))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Defect logged and maintenance issue created", content = @Content(schema = @Schema(implementation = MaintenanceIssueResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "ROOM_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "DEFECT_ALREADY_REPORTED", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/housekeeping/rooms/{roomNumber}/report-defect", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> reportDefect(
            @PathVariable String roomNumber,
            @RequestBody JsonNode request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return postWithHeader(housekeepingClient, "/api/housekeeping/rooms/" + roomNumber + "/report-defect", request, "Idempotency-Key", idempotencyKey);
    }

    @Operation(
            operationId = "cancelHousekeepingCleaning",
            summary = "Cancel housekeeping queue item",
            tags = {"Housekeeping"},
            description = "Roles: HOUSEKEEPING, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task removed from queue", content = @Content(schema = @Schema(implementation = CleaningTaskResponse.class))),
            @ApiResponse(responseCode = "404", description = "ROOM_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "TASK_CANNOT_BE_CANCELLED", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PatchMapping("/api/housekeeping/queue/{roomNumber}/cancel")
    public ResponseEntity<JsonNode> cancelCleaning(@PathVariable String roomNumber) {
        return patch(housekeepingClient, "/api/housekeeping/queue/" + roomNumber + "/cancel", null);
    }

    @Operation(
            operationId = "listHousekeepingCleaners",
            summary = "Get cleaners",
            tags = {"Housekeeping"},
            description = "Roles: HOUSEKEEPING, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cleaners staff list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))
    })
    @GetMapping("/api/housekeeping/cleaners")
    public ResponseEntity<JsonNode> getCleaners() {
        return get(housekeepingClient, "/api/housekeeping/cleaners");
    }

    // -------------------------------------------------------------------------
    // Room Service
    // -------------------------------------------------------------------------
    @Operation(
            operationId = "listRoomServiceOrders",
            summary = "Get room service orders",
            tags = {"Room Service"},
            description = "Roles: ROOM_SERVICE, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room service orders list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoomServiceOrderResponse.class))))
    })
    @GetMapping("/api/room-service/orders")
    public ResponseEntity<JsonNode> getRoomServiceOrders() {
        return get(roomServiceClient, "/api/room-service/orders");
    }

    @Operation(
            operationId = "getRoomServiceOrder",
            summary = "Get room service order by ID",
            tags = {"Room Service"},
            description = "Roles: ROOM_SERVICE, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room service order detail", content = @Content(schema = @Schema(implementation = RoomServiceOrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/api/room-service/orders/{orderId}")
    public ResponseEntity<JsonNode> getRoomServiceOrder(@PathVariable String orderId) {
        return get(roomServiceClient, "/api/room-service/orders/" + orderId);
    }

    @Operation(
            operationId = "createRoomServiceOrder",
            summary = "Create room service order",
            tags = {"Room Service"},
            description = "Roles: ROOM_SERVICE, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order created", content = @Content(schema = @Schema(implementation = RoomServiceOrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR | INVALID_ORDER_ITEMS", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "ROOM_NOT_FOUND | NO_ACTIVE_STAY", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "ROOM_NOT_OCCUPIED", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/room-service/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> createRoomServiceOrder(@RequestBody CreateOrderRequest request) {
        return post(roomServiceClient, "/api/room-service/orders", request);
    }

    @Operation(
            operationId = "advanceRoomServiceOrder",
            summary = "Move room service order to next status",
            tags = {"Room Service"},
            description = "Roles: ROOM_SERVICE, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status advanced", content = @Content(schema = @Schema(implementation = RoomServiceOrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "INVALID_STATUS_TRANSITION | ORDER_ALREADY_DELIVERED", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PatchMapping("/api/room-service/orders/{orderId}/next")
    public ResponseEntity<JsonNode> nextOrderStatus(@PathVariable String orderId) {
        return patch(roomServiceClient, "/api/room-service/orders/" + orderId + "/next", null);
    }

    @Operation(
            operationId = "cancelRoomServiceOrder",
            summary = "Cancel room service order",
            tags = {"Room Service"},
            description = "Roles: ROOM_SERVICE, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled", content = @Content(schema = @Schema(implementation = RoomServiceOrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "ORDER_CANNOT_BE_CANCELLED", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PatchMapping("/api/room-service/orders/{orderId}/cancel")
    public ResponseEntity<JsonNode> cancelOrder(@PathVariable String orderId) {
        return patch(roomServiceClient, "/api/room-service/orders/" + orderId + "/cancel", null);
    }

    @Operation(
            operationId = "getRoomServiceCharges",
            summary = "Get room service charges by room",
            tags = {"Room Service"},
            description = "Roles: ROOM_SERVICE, RECEPTIONIST, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accumulated room service charges", content = @Content(schema = @Schema(implementation = RoomServiceChargesResponse.class))),
            @ApiResponse(responseCode = "404", description = "ROOM_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/api/room-service/charges/{roomNumber}")
    public ResponseEntity<JsonNode> getCharges(@PathVariable String roomNumber) {
        return get(roomServiceClient, "/api/room-service/charges/" + roomNumber);
    }

    // -------------------------------------------------------------------------
    // Maintenance
    // -------------------------------------------------------------------------
    @Operation(
            operationId = "listMaintenanceIssues",
            summary = "Get maintenance issues",
            tags = {"Maintenance"},
            description = "Roles: MAINTENANCE, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Maintenance issues list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MaintenanceIssueResponse.class))))
    })
    @GetMapping("/api/maintenance/issues")
    public ResponseEntity<JsonNode> getMaintenanceIssues() {
        return get(maintenanceClient, "/api/maintenance/issues");
    }

    @Operation(
            operationId = "getMaintenanceIssue",
            summary = "Get maintenance issue by ID",
            tags = {"Maintenance"},
            description = "Roles: MAINTENANCE, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Maintenance issue detail", content = @Content(schema = @Schema(implementation = MaintenanceIssueResponse.class))),
            @ApiResponse(responseCode = "404", description = "ISSUE_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/api/maintenance/issues/{issueId}")
    public ResponseEntity<JsonNode> getMaintenanceIssue(@PathVariable String issueId) {
        return get(maintenanceClient, "/api/maintenance/issues/" + issueId);
    }

    @Operation(
            operationId = "createMaintenanceIssue",
            summary = "Report maintenance issue",
            tags = {"Maintenance"},
            description = "Roles: MAINTENANCE, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Maintenance issue reported", content = @Content(schema = @Schema(implementation = MaintenanceIssueResponse.class))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "ROOM_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/api/maintenance/issues", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> reportMaintenanceIssue(@RequestBody CreateIssueRequest request) {
        return post(maintenanceClient, "/api/maintenance/issues", request);
    }

    @Operation(
            operationId = "getMaintenanceQueue",
            summary = "Get maintenance priority queue",
            tags = {"Maintenance"},
            description = "Roles: MAINTENANCE, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Maintenance priority queue", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MaintenanceIssueResponse.class))))
    })
    @GetMapping("/api/maintenance/queue")
    public ResponseEntity<JsonNode> getMaintenanceQueue() {
        return get(maintenanceClient, "/api/maintenance/queue");
    }

    @Operation(
            operationId = "processNextMaintenanceQueueItem",
            summary = "Process next maintenance queue item",
            tags = {"Maintenance"},
            description = "Roles: MAINTENANCE, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Next queue items processed", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MaintenanceIssueResponse.class)))),
            @ApiResponse(responseCode = "409", description = "QUEUE_EMPTY", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping("/api/maintenance/queue/process-next")
    public ResponseEntity<JsonNode> processNextMaintenance() {
        return post(maintenanceClient, "/api/maintenance/queue/process-next", null);
    }

    @Operation(
            operationId = "resolveMaintenanceIssue",
            summary = "Resolve maintenance issue",
            tags = {"Maintenance"},
            description = "Roles: MAINTENANCE, MANAGER, ADMIN"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Resolution note payload",
            required = false,
            content = @Content(schema = @Schema(implementation = ResolveIssueRequest.class))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issue resolved", content = @Content(schema = @Schema(implementation = MaintenanceIssueResponse.class))),
            @ApiResponse(responseCode = "404", description = "ISSUE_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "ISSUE_ALREADY_RESOLVED", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PatchMapping("/api/maintenance/issues/{issueId}/resolve")
    public ResponseEntity<JsonNode> resolveMaintenanceIssue(
            @PathVariable String issueId,
            @RequestBody(required = false) JsonNode request
    ) {
        return patch(maintenanceClient, "/api/maintenance/issues/" + issueId + "/resolve", request);
    }

    @Operation(
            operationId = "resolveMaintenanceIssuePost",
            summary = "Resolve maintenance issue (POST fallback)",
            tags = {"Maintenance"},
            description = "Roles: MAINTENANCE, MANAGER, ADMIN"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Resolution note payload",
            required = false,
            content = @Content(schema = @Schema(implementation = ResolveIssueRequest.class))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issue resolved", content = @Content(schema = @Schema(implementation = MaintenanceIssueResponse.class))),
            @ApiResponse(responseCode = "404", description = "ISSUE_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "ISSUE_ALREADY_RESOLVED", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping("/api/maintenance/issues/{issueId}/resolve")
    public ResponseEntity<JsonNode> resolveMaintenanceIssuePost(
            @PathVariable String issueId,
            @RequestBody(required = false) JsonNode request
    ) {
        return post(maintenanceClient, "/api/maintenance/issues/" + issueId + "/resolve", request);
    }

    @Operation(
            operationId = "cancelMaintenanceIssue",
            summary = "Cancel maintenance issue",
            tags = {"Maintenance"},
            description = "Roles: MAINTENANCE, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issue cancelled", content = @Content(schema = @Schema(implementation = MaintenanceIssueResponse.class))),
            @ApiResponse(responseCode = "404", description = "ISSUE_NOT_FOUND", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "ISSUE_ALREADY_RESOLVED", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PatchMapping("/api/maintenance/issues/{issueId}/cancel")
    public ResponseEntity<JsonNode> cancelMaintenanceIssue(@PathVariable String issueId) {
        return patch(maintenanceClient, "/api/maintenance/issues/" + issueId + "/cancel", null);
    }

    @Operation(
            operationId = "listMaintenanceTechnicians",
            summary = "Get technicians",
            tags = {"Maintenance"},
            description = "Roles: MAINTENANCE, MANAGER, ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Technicians staff list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))
    })
    @GetMapping("/api/maintenance/technicians")
    public ResponseEntity<JsonNode> getTechnicians() {
        return get(maintenanceClient, "/api/maintenance/technicians");
    }

    // -------------------------------------------------------------------------
    // Development & Demo Workflows
    // -------------------------------------------------------------------------
    @Operation(
            operationId = "resetDemoData",
            summary = "Development reset",
            tags = {"Development / Demo"},
            description = "Development only — blocked by production edge. Roles: ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demo reset results across all services")
    })
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

    @Operation(
            operationId = "seedDemoData",
            summary = "Development seed",
            tags = {"Development / Demo"},
            description = "Development only — blocked by production edge. Roles: ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demo seed results across all services")
    })
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

    @Operation(
            operationId = "runDemoTs01",
            summary = "Run scenario TS-01 (Check-in)",
            tags = {"Development / Demo"},
            description = "Development only — blocked by production edge. Roles: ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Scenario execution result", content = @Content(schema = @Schema(implementation = CheckInResponse.class)))
    })
    @PostMapping("/api/demo/run/ts-01")
    public ResponseEntity<JsonNode> runTs01() {
        return post(receptionClient, "/api/reception/check-in", checkInRequest("Alice Smith", "DOUBLE", 2, 3, "LIFT"));
    }

    @Operation(
            operationId = "runDemoTs02",
            summary = "Run scenario TS-02 (Check-out)",
            tags = {"Development / Demo"},
            description = "Development only — blocked by production edge. Roles: ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Scenario execution result", content = @Content(schema = @Schema(implementation = CheckOutResponse.class)))
    })
    @PostMapping("/api/demo/run/ts-02")
    public ResponseEntity<JsonNode> runTs02() {
        return post(receptionClient, "/api/reception/check-out/204", null);
    }

    @Operation(
            operationId = "runDemoTs03",
            summary = "Run scenario TS-03 (Turnover flow)",
            tags = {"Development / Demo"},
            description = "Development only — blocked by production edge. Roles: ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Turnover workflow results")
    })
    @PostMapping("/api/demo/run/ts-03")
    public ResponseEntity<JsonNode> runTs03() {
        ObjectNode body = objectMapper.createObjectNode();
        body.set("checkout204", safePostBody(receptionClient, "/api/reception/check-out/204", null));
        body.set("housekeepingQueue", getBody(housekeepingClient, "/api/housekeeping/queue"));
        body.set("startCleaning", safePostBody(housekeepingClient, "/api/housekeeping/rooms/204/start", null));
        body.set("markClean", safePostBody(housekeepingClient, "/api/housekeeping/rooms/204/clean", null));
        return ResponseEntity.ok(body);
    }

    @Operation(
            operationId = "runDemoTs04",
            summary = "Run scenario TS-04 (Room service lifecycle)",
            tags = {"Development / Demo"},
            description = "Development only — blocked by production edge. Roles: ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Room service lifecycle results")
    })
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

    @Operation(
            operationId = "runDemoTs05",
            summary = "Run scenario TS-05 (Defect ticket)",
            tags = {"Development / Demo"},
            description = "Development only — blocked by production edge. Roles: ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Maintenance defect issue", content = @Content(schema = @Schema(implementation = MaintenanceIssueResponse.class)))
    })
    @PostMapping("/api/demo/run/ts-05")
    public ResponseEntity<JsonNode> runTs05() {
        CreateIssueRequest request = new CreateIssueRequest("115", "Broken shower", "CRITICAL");
        return post(maintenanceClient, "/api/maintenance/issues", request);
    }

    @Operation(
            operationId = "runDemoTs06",
            summary = "Run scenario TS-06 (Concurrent check-in)",
            tags = {"Development / Demo"},
            description = "Development only — blocked by production edge. Roles: ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Concurrent check-in execution results")
    })
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

    @Operation(
            operationId = "runDemoTs07",
            summary = "Run scenario TS-07 (Overbooking validation)",
            tags = {"Development / Demo"},
            description = "Development only — blocked by production edge. Roles: ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Overbooking validation execution results")
    })
    @PostMapping("/api/demo/run/ts-07")
    public ResponseEntity<JsonNode> runTs07() {
        safePostBody(receptionClient, "/api/reception/dev/reset", null);
        ObjectNode body = objectMapper.createObjectNode();
        body.set("firstSuiteCheckIn", safePostBody(receptionClient, "/api/reception/check-in", checkInRequest("Suite Guest A", "SUITE", 1, null, null)));
        body.set("secondSuiteCheckInExpectedError", safePostBody(receptionClient, "/api/reception/check-in", checkInRequest("Suite Guest B", "SUITE", 1, null, null)));
        return ResponseEntity.ok(body);
    }

    @Operation(
            operationId = "runDemoTs08",
            summary = "Run scenario TS-08 (Checkout invalid room)",
            tags = {"Development / Demo"},
            description = "Development only — blocked by production edge. Roles: ADMIN"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invalid checkout execution results")
    })
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

    private ResponseEntity<JsonNode> putWithAuth(RestClient client, String uri, Object body, String authHeader) {
        RestClient.RequestBodySpec request = client.put().uri(uri);
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
