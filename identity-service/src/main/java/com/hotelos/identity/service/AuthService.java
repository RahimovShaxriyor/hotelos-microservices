package com.hotelos.identity.service;

import com.hotelos.identity.domain.UserStatus;
import com.hotelos.identity.dto.LoginRequest;
import com.hotelos.identity.dto.LoginResponse;
import com.hotelos.identity.exception.BadCredentialsException;
import com.hotelos.identity.persistence.entity.UserEntity;
import com.hotelos.identity.persistence.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final AuthAuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            TokenService tokenService,
            AuthAuditLogService auditLogService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public LoginResponse login(LoginRequest request, String ipAddress) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank() ||
                request.getPassword() == null || request.getPassword().isBlank()) {
            String candidateUser = (request != null && request.getUsername() != null) ? request.getUsername().trim().toLowerCase(Locale.ROOT) : "unknown";
            auditLogService.recordEventIndependent("LOGIN_FAILURE", null, candidateUser, ipAddress, "Login failed: missing credentials");
            throw new BadCredentialsException("Invalid username or password");
        }

        String normalizedUsername = request.getUsername().trim().toLowerCase(Locale.ROOT);
        Optional<UserEntity> userOpt = userRepository.findByUsername(normalizedUsername);

        if (userOpt.isEmpty()) {
            auditLogService.recordEventIndependent("LOGIN_FAILURE", null, normalizedUsername, ipAddress, "Login failed: user not found");
            throw new BadCredentialsException("Invalid username or password");
        }

        UserEntity user = userOpt.get();

        // 1. Check account status
        if (user.getStatus() == UserStatus.DISABLED) {
            auditLogService.recordEventIndependent("LOGIN_FAILURE", user.getId(), normalizedUsername, ipAddress, "Login failed: account is disabled");
            throw new BadCredentialsException("Invalid username or password");
        }

        // 2. Check temporary lockout
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            auditLogService.recordEventIndependent("LOGIN_FAILURE", user.getId(), normalizedUsername, ipAddress,
                    "Login failed: account is temporarily locked until " + user.getLockedUntil());
            throw new BadCredentialsException("Invalid username or password");
        }

        // 3. Verify password
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        if (!passwordMatches) {
            userRepository.recordFailedLoginAttempt(normalizedUsername);
            int attempts = user.getFailedLoginAttempts() + 1;
            auditLogService.recordEventIndependent("LOGIN_FAILURE", user.getId(), normalizedUsername, ipAddress,
                    "Login failed: invalid password (attempt " + attempts + ")");
            throw new BadCredentialsException("Invalid username or password");
        }

        // 4. Success: reset failed attempts and unlock
        userRepository.resetFailedLoginAttempts(user.getId());
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        auditLogService.recordEventIndependent("LOGIN_SUCCESS", user.getId(), normalizedUsername, ipAddress, "Staff login successful");

        // 5. Generate RS256 access token
        String accessToken = tokenService.createAccessToken(user);
        return new LoginResponse(accessToken, "Bearer", TokenService.ACCESS_TOKEN_VALIDITY_SECONDS);
    }
}
