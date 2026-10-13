package com.hotelos.identity.service;

import com.hotelos.identity.domain.AccountType;
import com.hotelos.identity.domain.GovernanceAuthority;
import com.hotelos.identity.domain.UserStatus;
import com.hotelos.identity.dto.*;
import com.hotelos.identity.exception.IdentityBusinessException;
import com.hotelos.identity.persistence.entity.UserEntity;
import com.hotelos.identity.persistence.repository.RefreshSessionRepository;
import com.hotelos.identity.persistence.repository.RoleRepository;
import com.hotelos.identity.persistence.repository.UserRepository;
import com.hotelos.identity.security.TrustedGatewayIpResolver;
import com.hotelos.security.filter.CorrelationIdFilter;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final AuthAuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final TrustedGatewayIpResolver trustedGatewayIpResolver;

    public AdminUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshSessionRepository refreshSessionRepository,
            AuthAuditLogService auditLogService,
            PasswordEncoder passwordEncoder,
            TrustedGatewayIpResolver trustedGatewayIpResolver
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
        this.trustedGatewayIpResolver = trustedGatewayIpResolver;
    }

    @Transactional(readOnly = true)
    public StaffUserPageResponse listUsers(
            int page,
            int size,
            UserStatus status,
            String roleFilter,
            String search,
            Authentication authentication
    ) {
        GovernanceAuthority authority = GovernanceAuthority.fromAuthentication(authentication);
        if (authority == GovernanceAuthority.NONE) {
            throw IdentityBusinessException.accessDenied("Caller lacks governance authority to list staff accounts");
        }

        int pageIndex = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), 100);
        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.ASC, "username"));

        Specification<UserEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("accountType"), AccountType.STAFF));

            // ADMIN can only view normal staff accounts (hide ADMIN and OWNER accounts)
            if (authority == GovernanceAuthority.ADMIN) {
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<UserEntity> subRoot = subquery.from(UserEntity.class);
                Join<UserEntity, String> subRoles = subRoot.join("roles");
                subquery.select(subRoot.get("id"))
                        .where(subRoles.in("ADMIN", "OWNER"));
                predicates.add(cb.not(root.get("id").in(subquery)));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (roleFilter != null && !roleFilter.isBlank()) {
                Join<UserEntity, String> roleJoin = root.join("roles");
                predicates.add(cb.equal(roleJoin, roleFilter.trim().toUpperCase(Locale.ROOT)));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("fullName")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<UserEntity> userPage = userRepository.findAll(spec, pageRequest);
        List<StaffUserSummaryResponse> items = userPage.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new StaffUserPageResponse(
                items,
                userPage.getTotalElements(),
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public StaffUserDetailResponse getUserDetail(UUID userId, Authentication authentication) {
        GovernanceAuthority authority = GovernanceAuthority.fromAuthentication(authentication);
        if (authority == GovernanceAuthority.NONE) {
            throw IdentityBusinessException.accessDenied("Caller lacks governance authority");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> IdentityBusinessException.userNotFound("User not found with id: " + userId));

        if (user.getAccountType() != AccountType.STAFF) {
            throw IdentityBusinessException.userNotFound("User not found with id: " + userId);
        }

        // Prevent governance enumeration: ADMIN cannot view ADMIN or OWNER accounts
        if (authority == GovernanceAuthority.ADMIN) {
            if (user.getRoles().contains("ADMIN") || user.getRoles().contains("OWNER")) {
                throw IdentityBusinessException.userNotFound("User not found with id: " + userId);
            }
        }

        return toDetail(user);
    }

    @Transactional
    public StaffUserDetailResponse updateRoles(
            UUID targetUserId,
            UpdateRolesRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        GovernanceAuthority authority = GovernanceAuthority.fromAuthentication(authentication);
        if (authority == GovernanceAuthority.NONE) {
            throw IdentityBusinessException.accessDenied("Caller lacks governance authority to modify roles");
        }

        if (request == null || request.getRoles() == null || request.getRoles().isEmpty()) {
            throw IdentityBusinessException.roleNotAssignable("Role set cannot be empty");
        }

        Set<String> desiredRoles = new HashSet<>();
        for (String r : request.getRoles()) {
            if (r == null || r.isBlank()) {
                throw IdentityBusinessException.roleNotAssignable("Role name cannot be empty");
            }
            String trimmed = r.trim().toUpperCase(Locale.ROOT);
            if ("CUSTOMER".equals(trimmed)) {
                throw IdentityBusinessException.roleNotAssignable("CUSTOMER is not a staff role");
            }
            if (!roleRepository.existsById(trimmed)) {
                throw IdentityBusinessException.roleNotAssignable("Unknown role: " + trimmed);
            }
            desiredRoles.add(trimmed);
        }

        UserEntity caller = resolveCaller(authentication);

        // Lock ordering: acquire governance advisory lock 8086002
        userRepository.acquireGovernanceAdvisoryLock();

        // Lock target user FOR UPDATE
        UserEntity target = userRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> IdentityBusinessException.userNotFound("User not found with id: " + targetUserId));

        if (target.getAccountType() != AccountType.STAFF) {
            throw IdentityBusinessException.roleNotAssignable("Target account is not a staff account");
        }

        boolean targetHasAdmin = target.getRoles().contains("ADMIN");
        boolean targetHasOwner = target.getRoles().contains("OWNER");

        // Governance authority checks
        if (authority == GovernanceAuthority.ADMIN) {
            if (targetHasAdmin || targetHasOwner) {
                throw IdentityBusinessException.accessDenied("ADMIN cannot modify roles of ADMIN or OWNER accounts");
            }
            if (desiredRoles.contains("ADMIN") || desiredRoles.contains("OWNER")) {
                throw IdentityBusinessException.accessDenied("ADMIN cannot assign ADMIN or OWNER roles");
            }
        } else if (authority == GovernanceAuthority.OWNER) {
            // REST cannot mint a new OWNER into a non-OWNER account
            if (!targetHasOwner && desiredRoles.contains("OWNER")) {
                throw IdentityBusinessException.roleNotAssignable("OWNER role cannot be granted via REST; operator runner required");
            }

            // Check demotion from OWNER
            if (targetHasOwner && !desiredRoles.contains("OWNER")) {
                if (target.getId().equals(caller.getId())) {
                    throw IdentityBusinessException.cannotDemoteSelf("Cannot remove OWNER role from yourself");
                }
                long remainingOwners = userRepository.countActiveUsersWithRoleExcluding("OWNER", target.getId());
                if (remainingOwners < 1) {
                    throw IdentityBusinessException.cannotRemoveLastOwner("Cannot remove OWNER role: at least one active OWNER must remain");
                }
            }

            // Check demotion from ADMIN
            if (targetHasAdmin && !desiredRoles.contains("ADMIN")) {
                if (target.getId().equals(caller.getId())) {
                    throw IdentityBusinessException.cannotDemoteSelf("Cannot remove ADMIN role from yourself");
                }
                long remainingAdmins = userRepository.countActiveUsersWithRoleExcluding("ADMIN", target.getId());
                if (remainingAdmins < 1) {
                    throw IdentityBusinessException.cannotRemoveLastAdmin("Cannot remove ADMIN role: at least one active ADMIN must remain");
                }
            }
        }

        Instant now = Instant.now();
        target.getRoles().clear();
        target.getRoles().addAll(desiredRoles);
        target.setUpdatedAt(now);
        UserEntity saved = userRepository.saveAndFlush(target);

        // Revoke all refresh sessions for target user in same transaction
        refreshSessionRepository.revokeAllForUser(targetUserId, now);

        String correlationId = extractCorrelationId(httpRequest);
        String clientIp = trustedGatewayIpResolver.resolveClientIp(httpRequest);

        auditLogService.recordGovernanceEvent(
                "USER_ROLES_CHANGED",
                caller.getId(),
                targetUserId,
                target.getUsername(),
                correlationId,
                caller.getUsername(),
                clientIp,
                "Roles updated to: " + desiredRoles
        );

        return toDetail(saved);
    }

    @Transactional
    public StaffUserSummaryResponse disableUser(
            UUID targetUserId,
            DisableUserRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        GovernanceAuthority authority = GovernanceAuthority.fromAuthentication(authentication);
        if (authority == GovernanceAuthority.NONE) {
            throw IdentityBusinessException.accessDenied("Caller lacks governance authority to disable accounts");
        }

        UserEntity caller = resolveCaller(authentication);
        if (targetUserId.equals(caller.getId())) {
            throw IdentityBusinessException.cannotDisableSelf("Cannot disable your own account");
        }

        // Lock ordering: governance advisory lock 8086002
        userRepository.acquireGovernanceAdvisoryLock();

        UserEntity target = userRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> IdentityBusinessException.userNotFound("User not found with id: " + targetUserId));

        if (target.getStatus() == UserStatus.DISABLED) {
            throw IdentityBusinessException.accountAlreadyDisabled("Account is already disabled");
        }

        boolean targetHasAdmin = target.getRoles().contains("ADMIN");
        boolean targetHasOwner = target.getRoles().contains("OWNER");

        if (authority == GovernanceAuthority.ADMIN) {
            if (targetHasAdmin || targetHasOwner) {
                throw IdentityBusinessException.accessDenied("ADMIN cannot disable ADMIN or OWNER accounts");
            }
        } else if (authority == GovernanceAuthority.OWNER) {
            if (targetHasOwner) {
                long remainingOwners = userRepository.countActiveUsersWithRoleExcluding("OWNER", target.getId());
                if (remainingOwners < 1) {
                    throw IdentityBusinessException.cannotDisableLastOwner("Cannot disable the last active OWNER account");
                }
            }
            if (targetHasAdmin) {
                long remainingAdmins = userRepository.countActiveUsersWithRoleExcluding("ADMIN", target.getId());
                if (remainingAdmins < 1) {
                    throw IdentityBusinessException.cannotDisableLastAdmin("Cannot disable the last active ADMIN account");
                }
            }
        }

        Instant now = Instant.now();
        target.setStatus(UserStatus.DISABLED);
        target.setUpdatedAt(now);
        UserEntity saved = userRepository.saveAndFlush(target);

        // Revoke all refresh sessions
        refreshSessionRepository.revokeAllForUser(targetUserId, now);

        String correlationId = extractCorrelationId(httpRequest);
        String clientIp = trustedGatewayIpResolver.resolveClientIp(httpRequest);
        String reason = (request != null && request.getReason() != null) ? request.getReason().trim() : "Account disabled by administrator";

        auditLogService.recordGovernanceEvent(
                "USER_DISABLED",
                caller.getId(),
                targetUserId,
                target.getUsername(),
                correlationId,
                caller.getUsername(),
                clientIp,
                "Account disabled: " + reason
        );

        return toSummary(saved);
    }

    @Transactional
    public StaffUserSummaryResponse enableUser(
            UUID targetUserId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        GovernanceAuthority authority = GovernanceAuthority.fromAuthentication(authentication);
        if (authority == GovernanceAuthority.NONE) {
            throw IdentityBusinessException.accessDenied("Caller lacks governance authority to enable accounts");
        }

        UserEntity caller = resolveCaller(authentication);

        // Lock ordering: governance advisory lock 8086002
        userRepository.acquireGovernanceAdvisoryLock();

        UserEntity target = userRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> IdentityBusinessException.userNotFound("User not found with id: " + targetUserId));

        if (target.getStatus() == UserStatus.ACTIVE) {
            throw IdentityBusinessException.accountAlreadyActive("Account is already active");
        }

        boolean targetHasAdmin = target.getRoles().contains("ADMIN");
        boolean targetHasOwner = target.getRoles().contains("OWNER");

        if (authority == GovernanceAuthority.ADMIN) {
            if (targetHasAdmin || targetHasOwner) {
                throw IdentityBusinessException.accessDenied("ADMIN cannot enable ADMIN or OWNER accounts");
            }
        }

        Instant now = Instant.now();
        target.setStatus(UserStatus.ACTIVE);
        target.setFailedLoginAttempts(0);
        target.setLockedUntil(null);
        target.setUpdatedAt(now);
        UserEntity saved = userRepository.saveAndFlush(target);

        String correlationId = extractCorrelationId(httpRequest);
        String clientIp = trustedGatewayIpResolver.resolveClientIp(httpRequest);

        auditLogService.recordGovernanceEvent(
                "USER_ENABLED",
                caller.getId(),
                targetUserId,
                target.getUsername(),
                correlationId,
                caller.getUsername(),
                clientIp,
                "Account enabled by administrator"
        );

        return toSummary(saved);
    }

    @Transactional
    public void revokeSessions(
            UUID targetUserId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        GovernanceAuthority authority = GovernanceAuthority.fromAuthentication(authentication);
        if (authority == GovernanceAuthority.NONE) {
            throw IdentityBusinessException.accessDenied("Caller lacks governance authority to revoke sessions");
        }

        UserEntity caller = resolveCaller(authentication);

        UserEntity target = userRepository.findById(targetUserId)
                .orElseThrow(() -> IdentityBusinessException.userNotFound("User not found with id: " + targetUserId));

        // Self revocation is always allowed
        if (!targetUserId.equals(caller.getId())) {
            boolean targetHasAdmin = target.getRoles().contains("ADMIN");
            boolean targetHasOwner = target.getRoles().contains("OWNER");

            if (authority == GovernanceAuthority.ADMIN) {
                if (targetHasAdmin || targetHasOwner) {
                    throw IdentityBusinessException.accessDenied("ADMIN cannot revoke sessions of ADMIN or OWNER accounts");
                }
            }
        }

        Instant now = Instant.now();
        refreshSessionRepository.revokeAllForUser(targetUserId, now);

        String correlationId = extractCorrelationId(httpRequest);
        String clientIp = trustedGatewayIpResolver.resolveClientIp(httpRequest);

        auditLogService.recordGovernanceEvent(
                "USER_SESSIONS_REVOKED",
                caller.getId(),
                targetUserId,
                target.getUsername(),
                correlationId,
                caller.getUsername(),
                clientIp,
                "Refresh sessions revoked for user: " + target.getUsername()
        );
    }

    private UserEntity resolveCaller(Authentication authentication) {
        if (authentication == null) {
            throw IdentityBusinessException.accessDenied("Authentication required");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> IdentityBusinessException.userNotFound("Caller user not found: " + username));
    }

    private String extractCorrelationId(HttpServletRequest request) {
        if (request == null) return null;
        String cid = (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        if (cid == null) {
            cid = request.getHeader(CorrelationIdFilter.HEADER_NAME);
        }
        return cid;
    }

    private StaffUserSummaryResponse toSummary(UserEntity user) {
        return new StaffUserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRoles(),
                user.getStatus(),
                user.getAccountType(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private StaffUserDetailResponse toDetail(UserEntity user) {
        return new StaffUserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRoles(),
                user.getStatus(),
                user.getAccountType(),
                user.getFailedLoginAttempts(),
                user.getLockedUntil(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
