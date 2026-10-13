package com.hotelos.identity.service;

import com.hotelos.identity.domain.AccountType;
import com.hotelos.identity.domain.GovernanceAuthority;
import com.hotelos.identity.domain.StaffInvitationStatus;
import com.hotelos.identity.domain.UserStatus;
import com.hotelos.identity.dto.AcceptInvitationRequest;
import com.hotelos.identity.dto.CreateInvitationRequest;
import com.hotelos.identity.dto.InvitationCreatedResponse;
import com.hotelos.identity.dto.StaffUserSummaryResponse;
import com.hotelos.identity.exception.IdentityBusinessException;
import com.hotelos.identity.persistence.entity.StaffInvitationEntity;
import com.hotelos.identity.persistence.entity.StaffInvitationRoleEntity;
import com.hotelos.identity.persistence.entity.UserEntity;
import com.hotelos.identity.persistence.repository.RoleRepository;
import com.hotelos.identity.persistence.repository.StaffInvitationRepository;
import com.hotelos.identity.persistence.repository.StaffInvitationRoleRepository;
import com.hotelos.identity.persistence.repository.UserRepository;
import com.hotelos.identity.security.TrustedGatewayIpResolver;
import com.hotelos.security.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class StaffInvitationService {

    private static final Logger log = LoggerFactory.getLogger(StaffInvitationService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final StaffInvitationRepository staffInvitationRepository;
    private final StaffInvitationRoleRepository staffInvitationRoleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InvitationTokenService invitationTokenService;
    private final AuthAuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final TrustedGatewayIpResolver trustedGatewayIpResolver;

    public StaffInvitationService(
            StaffInvitationRepository staffInvitationRepository,
            StaffInvitationRoleRepository staffInvitationRoleRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            InvitationTokenService invitationTokenService,
            AuthAuditLogService auditLogService,
            PasswordEncoder passwordEncoder,
            TrustedGatewayIpResolver trustedGatewayIpResolver
    ) {
        this.staffInvitationRepository = staffInvitationRepository;
        this.staffInvitationRoleRepository = staffInvitationRoleRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.invitationTokenService = invitationTokenService;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
        this.trustedGatewayIpResolver = trustedGatewayIpResolver;
    }

    @Transactional
    public InvitationCreatedResponse createInvitation(
            CreateInvitationRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        GovernanceAuthority authority = GovernanceAuthority.fromAuthentication(authentication);
        if (authority == GovernanceAuthority.NONE) {
            throw IdentityBusinessException.accessDenied("Caller lacks governance authority to create invitations");
        }

        if (request == null) {
            throw IdentityBusinessException.roleNotAssignable("Request body is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw IdentityBusinessException.roleNotAssignable("Email address is required");
        }
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw IdentityBusinessException.roleNotAssignable("Full name is required");
        }
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            throw IdentityBusinessException.roleNotAssignable("At least one staff role must be specified");
        }

        String rawEmail = request.getEmail().trim();
        if (rawEmail.length() > 254 || !EMAIL_PATTERN.matcher(rawEmail).matches()) {
            throw IdentityBusinessException.roleNotAssignable("Invalid email address format or length exceeds 254 characters");
        }
        String normalizedEmail = rawEmail.toLowerCase(Locale.ROOT);

        Set<String> desiredRoles = new HashSet<>();
        for (String r : request.getRoles()) {
            if (r == null || r.isBlank()) {
                throw IdentityBusinessException.roleNotAssignable("Role cannot be empty");
            }
            String trimmedRole = r.trim().toUpperCase(Locale.ROOT);
            if ("CUSTOMER".equals(trimmedRole)) {
                throw IdentityBusinessException.roleNotAssignable("CUSTOMER is not a valid staff role");
            }
            if (!roleRepository.existsById(trimmedRole)) {
                throw IdentityBusinessException.roleNotAssignable("Unknown staff role: " + trimmedRole);
            }
            desiredRoles.add(trimmedRole);
        }

        // Evaluate governance assignment policy
        if (desiredRoles.contains("OWNER")) {
            if (authority == GovernanceAuthority.ADMIN) {
                throw IdentityBusinessException.accessDenied("ADMIN cannot invite OWNER role");
            }
            // OWNER cannot mint new OWNER via invitation
            throw IdentityBusinessException.roleNotAssignable("OWNER role cannot be granted via invitation; use operator runner");
        }
        if (desiredRoles.contains("ADMIN")) {
            if (authority == GovernanceAuthority.ADMIN) {
                throw IdentityBusinessException.accessDenied("ADMIN cannot invite ADMIN role");
            }
        }

        // Concurrency lock ordering: email advisory lock first
        staffInvitationRepository.acquireEmailAdvisoryLock(normalizedEmail);

        Instant now = Instant.now();
        // Invalidate/expire stale expired invitations
        staffInvitationRepository.expireStaleActiveInvitations(normalizedEmail, now);

        // Check if active invitation already exists
        Optional<StaffInvitationEntity> existingActive = staffInvitationRepository.findByNormalizedEmailAndStatus(
                normalizedEmail, StaffInvitationStatus.ACTIVE
        );
        if (existingActive.isPresent()) {
            StaffInvitationEntity existing = existingActive.get();
            if (existing.getExpiresAt().isBefore(now)) {
                existing.setStatus(StaffInvitationStatus.EXPIRED);
                staffInvitationRepository.save(existing);
            } else {
                throw IdentityBusinessException.invitationAlreadyActive("An active invitation already exists for email: " + normalizedEmail);
            }
        }

        // Check if user already exists
        if (userRepository.existsByUsername(normalizedEmail)) {
            throw IdentityBusinessException.userAlreadyExists("A user already exists with username: " + normalizedEmail);
        }

        UUID actorUserId = resolveActorUserId(authentication);
        UUID invitationId = UUID.randomUUID();
        String rawToken = invitationTokenService.generateRawToken();
        String tokenHash = invitationTokenService.hashToken(rawToken);
        Instant expiresAt = now.plus(48, ChronoUnit.HOURS);

        StaffInvitationEntity invitation = new StaffInvitationEntity(
                invitationId,
                normalizedEmail,
                request.getFullName().trim(),
                tokenHash,
                actorUserId,
                StaffInvitationStatus.ACTIVE,
                expiresAt,
                now
        );
        staffInvitationRepository.save(invitation);

        for (String roleName : desiredRoles) {
            staffInvitationRoleRepository.save(new StaffInvitationRoleEntity(invitationId, roleName));
        }

        String correlationId = extractCorrelationId(httpRequest);
        String clientIp = trustedGatewayIpResolver.resolveClientIp(httpRequest);

        auditLogService.recordGovernanceEvent(
                "STAFF_INVITATION_CREATED",
                actorUserId,
                null,
                normalizedEmail,
                correlationId,
                authentication.getName(),
                clientIp,
                "Staff invitation created for " + normalizedEmail + " with roles: " + desiredRoles
        );

        return new InvitationCreatedResponse(
                invitationId,
                normalizedEmail,
                request.getFullName().trim(),
                desiredRoles,
                expiresAt,
                rawToken
        );
    }

    @Transactional
    public InvitationCreatedResponse resendInvitation(
            UUID invitationId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        GovernanceAuthority authority = GovernanceAuthority.fromAuthentication(authentication);
        if (authority == GovernanceAuthority.NONE) {
            throw IdentityBusinessException.accessDenied("Caller lacks governance authority to resend invitations");
        }

        // Initial read WITHOUT row lock to resolve normalized email (scalar query to prevent Hibernate 1st-level cache pollution before lock)
        String normalizedEmail = staffInvitationRepository.findNormalizedEmailById(invitationId)
                .orElseThrow(() -> IdentityBusinessException.invitationNotFound("Invitation not found with id: " + invitationId));

        // Lock ordering: email advisory lock first
        staffInvitationRepository.acquireEmailAdvisoryLock(normalizedEmail);

        // Re-read same invitation FOR UPDATE
        StaffInvitationEntity invitation = staffInvitationRepository.findByIdForUpdate(invitationId)
                .orElseThrow(() -> IdentityBusinessException.invitationNotFound("Invitation not found with id: " + invitationId));

        // Revalidate state
        if (invitation.getStatus() == StaffInvitationStatus.ACCEPTED || invitation.getAcceptedAt() != null) {
            throw IdentityBusinessException.invitationAlreadyAccepted("Invitation has already been accepted");
        }
        if (invitation.getStatus() == StaffInvitationStatus.INVALIDATED || invitation.getInvalidatedAt() != null) {
            throw IdentityBusinessException.invitationNotActive("Invitation is no longer active");
        }

        // Authorize stored roles against current caller
        Set<String> storedRoles = staffInvitationRoleRepository.findRoleNamesByInvitationId(invitationId);
        if (storedRoles.contains("OWNER")) {
            throw IdentityBusinessException.accessDenied("Stored invitation contains OWNER role which cannot be resent");
        }
        if (storedRoles.contains("ADMIN") && authority == GovernanceAuthority.ADMIN) {
            throw IdentityBusinessException.accessDenied("ADMIN cannot resend invitation containing ADMIN role");
        }

        Instant now = Instant.now();
        // Invalidate or mark expired if currently active
        if (invitation.getStatus() == StaffInvitationStatus.ACTIVE) {
            if (invitation.getExpiresAt().isBefore(now)) {
                invitation.setStatus(StaffInvitationStatus.EXPIRED);
            } else {
                invitation.setStatus(StaffInvitationStatus.INVALIDATED);
                invitation.setInvalidatedAt(now);
            }
            staffInvitationRepository.save(invitation);
        }
        // If already EXPIRED, it remains EXPIRED

        // Expire any other stale active invitations
        staffInvitationRepository.expireStaleActiveInvitations(normalizedEmail, now);

        UUID actorUserId = resolveActorUserId(authentication);
        UUID newInvitationId = UUID.randomUUID();
        String rawToken = invitationTokenService.generateRawToken();
        String tokenHash = invitationTokenService.hashToken(rawToken);
        Instant expiresAt = now.plus(48, ChronoUnit.HOURS);

        StaffInvitationEntity replacement = new StaffInvitationEntity(
                newInvitationId,
                normalizedEmail,
                invitation.getFullName(),
                tokenHash,
                actorUserId,
                StaffInvitationStatus.ACTIVE,
                expiresAt,
                now
        );
        staffInvitationRepository.save(replacement);

        for (String roleName : storedRoles) {
            staffInvitationRoleRepository.save(new StaffInvitationRoleEntity(newInvitationId, roleName));
        }

        String correlationId = extractCorrelationId(httpRequest);
        String clientIp = trustedGatewayIpResolver.resolveClientIp(httpRequest);

        auditLogService.recordGovernanceEvent(
                "STAFF_INVITATION_RESENT",
                actorUserId,
                null,
                normalizedEmail,
                correlationId,
                authentication.getName(),
                clientIp,
                "Invitation resent; old id " + invitationId + " invalidated, replacement " + newInvitationId + " created"
        );

        return new InvitationCreatedResponse(
                newInvitationId,
                normalizedEmail,
                invitation.getFullName(),
                storedRoles,
                expiresAt,
                rawToken
        );
    }

    @Transactional
    public StaffUserSummaryResponse acceptInvitation(
            AcceptInvitationRequest request,
            HttpServletRequest httpRequest
    ) {
        if (request == null) {
            throw IdentityBusinessException.invalidOrExpiredInvitation("Request body is required");
        }
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw IdentityBusinessException.invalidOrExpiredInvitation("Invitation token is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw IdentityBusinessException.invalidOrExpiredInvitation("Password is required");
        }
        if (request.getPassword().length() < 8) {
            throw IdentityBusinessException.invalidOrExpiredInvitation("Password must be at least 8 characters");
        }

        String tokenHash = invitationTokenService.hashToken(request.getToken());

        // Initial read WITHOUT row lock to find email (scalar query to prevent Hibernate 1st-level cache pollution before lock)
        String normalizedEmail = staffInvitationRepository.findNormalizedEmailByTokenHash(tokenHash)
                .orElseThrow(() -> IdentityBusinessException.invalidOrExpiredInvitation("Invalid or expired invitation token"));

        // Lock ordering: acquire email advisory lock
        staffInvitationRepository.acquireEmailAdvisoryLock(normalizedEmail);

        // Re-read invitation FOR UPDATE
        StaffInvitationEntity invitation = staffInvitationRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> IdentityBusinessException.invalidOrExpiredInvitation("Invalid or expired invitation token"));

        if (invitation.getStatus() == StaffInvitationStatus.ACCEPTED || invitation.getAcceptedAt() != null) {
            throw IdentityBusinessException.invitationAlreadyAccepted("Invitation has already been accepted");
        }
        if (invitation.getStatus() == StaffInvitationStatus.INVALIDATED || invitation.getInvalidatedAt() != null) {
            throw IdentityBusinessException.invalidOrExpiredInvitation("Invitation is no longer active");
        }
        Instant now = Instant.now();
        if (invitation.getStatus() == StaffInvitationStatus.EXPIRED || invitation.getExpiresAt().isBefore(now)) {
            if (invitation.getStatus() == StaffInvitationStatus.ACTIVE) {
                invitation.setStatus(StaffInvitationStatus.EXPIRED);
                staffInvitationRepository.save(invitation);
            }
            throw IdentityBusinessException.invalidOrExpiredInvitation("Invitation has expired");
        }

        Set<String> roles = staffInvitationRoleRepository.findRoleNamesByInvitationId(invitation.getId());
        if (roles == null || roles.isEmpty()) {
            throw IdentityBusinessException.invalidOrExpiredInvitation("Invitation has no roles assigned");
        }
        if (roles.contains("OWNER")) {
            throw IdentityBusinessException.invalidOrExpiredInvitation("OWNER role cannot be accepted via invitation");
        }

        if (userRepository.existsByUsername(normalizedEmail)) {
            throw IdentityBusinessException.userAlreadyExists("A user already exists with username: " + normalizedEmail);
        }

        UUID userId = UUID.randomUUID();
        String passwordHash = passwordEncoder.encode(request.getPassword());
        UserEntity newUser = new UserEntity(
                userId,
                normalizedEmail,
                passwordHash,
                invitation.getFullName(),
                UserStatus.ACTIVE,
                AccountType.STAFF,
                now
        );
        for (String r : roles) {
            newUser.addRole(r);
        }
        UserEntity savedUser = userRepository.save(newUser);

        // Mark accepted
        invitation.setStatus(StaffInvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(now);
        staffInvitationRepository.save(invitation);

        // Invalidate any other pending invitations for this email
        staffInvitationRepository.invalidateOtherPendingInvitations(normalizedEmail, invitation.getId(), now);

        String correlationId = extractCorrelationId(httpRequest);
        String clientIp = trustedGatewayIpResolver.resolveClientIp(httpRequest);

        auditLogService.recordGovernanceEvent(
                "STAFF_INVITATION_ACCEPTED",
                userId,
                userId,
                normalizedEmail,
                correlationId,
                normalizedEmail,
                clientIp,
                "Staff invitation accepted; account created with roles: " + roles
        );

        return new StaffUserSummaryResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getFullName(),
                savedUser.getRoles(),
                savedUser.getStatus(),
                savedUser.getAccountType(),
                savedUser.getCreatedAt(),
                savedUser.getUpdatedAt()
        );
    }

    private UUID resolveActorUserId(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username.toLowerCase(Locale.ROOT))
                .map(UserEntity::getId)
                .orElse(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    private String extractCorrelationId(HttpServletRequest request) {
        if (request == null) return null;
        String cid = (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        if (cid == null) {
            cid = request.getHeader(CorrelationIdFilter.HEADER_NAME);
        }
        return cid;
    }
}
