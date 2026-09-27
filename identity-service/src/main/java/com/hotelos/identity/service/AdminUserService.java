package com.hotelos.identity.service;

import com.hotelos.identity.domain.UserStatus;
import com.hotelos.identity.persistence.entity.UserEntity;
import com.hotelos.identity.persistence.repository.RefreshSessionRepository;
import com.hotelos.identity.persistence.repository.RoleRepository;
import com.hotelos.identity.persistence.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final AuthAuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshSessionRepository refreshSessionRepository,
            AuthAuditLogService auditLogService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserEntity createUser(String rawUsername, String rawPassword, String fullName, Set<String> roles) {
        if (rawUsername == null || rawUsername.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        String normalizedUsername = rawUsername.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalStateException("User already exists with username: " + normalizedUsername);
        }

        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        String passwordHash = passwordEncoder.encode(rawPassword);

        UserEntity user = new UserEntity(userId, normalizedUsername, passwordHash, fullName, UserStatus.ACTIVE, now);
        if (roles != null) {
            for (String role : roles) {
                if (roleRepository.existsById(role)) {
                    user.addRole(role);
                } else {
                    throw new IllegalArgumentException("Unknown role: " + role);
                }
            }
        }

        UserEntity saved = userRepository.save(user);
        auditLogService.recordEvent("USER_CREATED", userId, normalizedUsername, null, "Staff account created with roles: " + user.getRoles());
        return saved;
    }

    @Transactional
    public UserEntity updateUserStatus(UUID userId, UserStatus newStatus) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));

        user.setStatus(newStatus);
        user.setUpdatedAt(Instant.now());

        if (newStatus == UserStatus.DISABLED) {
            refreshSessionRepository.revokeAllForUser(userId, Instant.now());
            auditLogService.recordEvent("USER_DISABLED", userId, user.getUsername(), null, "Account disabled; all active refresh sessions revoked");
        } else {
            auditLogService.recordEvent("USER_ENABLED", userId, user.getUsername(), null, "Account set to active");
        }

        return userRepository.save(user);
    }

    @Transactional
    public UserEntity assignRoles(UUID userId, Set<String> roles) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));

        user.getRoles().clear();
        if (roles != null) {
            for (String role : roles) {
                if (roleRepository.existsById(role)) {
                    user.addRole(role);
                } else {
                    throw new IllegalArgumentException("Unknown role: " + role);
                }
            }
        }
        user.setUpdatedAt(Instant.now());
        UserEntity saved = userRepository.save(user);
        auditLogService.recordEvent("ROLES_MODIFIED", userId, user.getUsername(), null, "Roles updated to: " + user.getRoles());
        return saved;
    }

    @Transactional
    public UserEntity resetPassword(UUID userId, String newRawPassword) {
        if (newRawPassword == null || newRawPassword.isBlank()) {
            throw new IllegalArgumentException("New password cannot be empty");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));

        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setUpdatedAt(Instant.now());

        // Revoke existing sessions on password change
        refreshSessionRepository.revokeAllForUser(userId, Instant.now());

        UserEntity saved = userRepository.save(user);
        auditLogService.recordEvent("PASSWORD_RESET", userId, user.getUsername(), null, "Staff password reset; sessions revoked");
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> getUser(UUID userId) {
        return userRepository.findById(userId);
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> getUserByUsername(String rawUsername) {
        if (rawUsername == null) return Optional.empty();
        return userRepository.findByUsername(rawUsername.trim().toLowerCase(Locale.ROOT));
    }
}
