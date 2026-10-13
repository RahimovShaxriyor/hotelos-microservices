package com.hotelos.identity.web;

import com.hotelos.identity.domain.UserStatus;
import com.hotelos.identity.dto.*;
import com.hotelos.identity.service.AdminUserService;
import com.hotelos.identity.service.StaffInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Staff Administration", description = "Staff identity governance, invitations, lifecycle, and role assignment")
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final StaffInvitationService staffInvitationService;

    public AdminUserController(AdminUserService adminUserService, StaffInvitationService staffInvitationService) {
        this.adminUserService = adminUserService;
        this.staffInvitationService = staffInvitationService;
    }

    @Operation(summary = "List staff accounts", description = "Query paged staff users with optional filters")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StaffUserPageResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            Authentication authentication
    ) {
        StaffUserPageResponse response = adminUserService.listUsers(page, size, status, role, search, authentication);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get staff account detail", description = "Retrieve full staff user detail by id")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StaffUserDetailResponse> getUserDetail(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        StaffUserDetailResponse response = adminUserService.getUserDetail(id, authentication);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create staff invitation", description = "Issue an invitation to invite a new staff member")
    @PostMapping(value = "/invitations", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InvitationCreatedResponse> createInvitation(
            @RequestBody CreateInvitationRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        InvitationCreatedResponse response = staffInvitationService.createInvitation(request, authentication, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Resend staff invitation", description = "Invalidate existing invitation and issue a replacement invitation")
    @PostMapping(value = "/invitations/{id}/resend", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InvitationCreatedResponse> resendInvitation(
            @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        InvitationCreatedResponse response = staffInvitationService.resendInvitation(id, authentication, httpRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Disable staff account", description = "Disable staff user account and revoke all active sessions")
    @PostMapping(value = "/{id}/disable", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StaffUserSummaryResponse> disableUser(
            @PathVariable UUID id,
            @RequestBody(required = false) DisableUserRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        StaffUserSummaryResponse response = adminUserService.disableUser(id, request, authentication, httpRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Enable staff account", description = "Re-enable a previously disabled staff user account")
    @PostMapping(value = "/{id}/enable", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StaffUserSummaryResponse> enableUser(
            @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        StaffUserSummaryResponse response = adminUserService.enableUser(id, authentication, httpRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update staff roles", description = "Replace roles for target staff user")
    @PutMapping(value = "/{id}/roles", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StaffUserDetailResponse> updateRoles(
            @PathVariable UUID id,
            @RequestBody UpdateRolesRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        StaffUserDetailResponse response = adminUserService.updateRoles(id, request, authentication, httpRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Revoke staff sessions", description = "Revoke all refresh sessions for target staff user")
    @PostMapping(value = "/{id}/revoke-sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> revokeSessions(
            @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        adminUserService.revokeSessions(id, authentication, httpRequest);
        return ResponseEntity.noContent().build();
    }
}
