package com.taskmanagement.service;

import com.taskmanagement.dto.auth.*;
import com.taskmanagement.entity.User;
import com.taskmanagement.exception.*;
import com.taskmanagement.entity.RoleType;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.JwtTokenProvider;
import com.taskmanagement.security.TokenBlacklistService;
import com.taskmanagement.entity.AuditAction;
import com.taskmanagement.entity.AuditEntityType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService - Service xử lý authentication operations
 * 
 * Responsibilities:
 * 1. Login: Authenticate user và generate JWT tokens
 * 2. Register: Tạo user mới và auto-login
 * 3. Refresh Token: Generate new access token từ refresh token
 * 
 * Security:
 * - Password hashing với BCrypt (strength 12)
 * - JWT token generation với secret key
 * - Input validation (username format, email format, password strength)
 * - Duplicate check (username, email)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserSessionService userSessionService;
    private final LoginAttemptService loginAttemptService;
    private final AuditService auditService;

    /**
     * login - Authenticate user và return JWT tokens
     * 
     * Flow:
     * 1. Authenticate với Spring Security
     * 2. Generate access token (24h)
     * 3. Generate refresh token (7d)
     * 4. Return AuthResponse với tokens
     * Example:
     * LoginRequest request = new LoginRequest("john_doe", "password123");
     * AuthResponse response = authService.login(request);
     * // response.getAccessToken() → "eyJhbGci..."
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        if (loginAttemptService.isLocked(request.getUsername())) {
            long remainingSeconds = loginAttemptService.getRemainingLockSeconds(request.getUsername());
            log.warn("Blocked locked account login attempt: {}", request.getUsername());
            throw new UnauthorizedException(String.format(
                "Account is locked due to multiple failed login attempts. Please try again in %d minutes %d seconds.",
                remainingSeconds / 60, remainingSeconds % 60
            ));
        }

        try {
            // Authenticate user credentials
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );

            // Reset counter khi login thành công
            loginAttemptService.recordSuccess(request.getUsername());
            
            // Generate JWT tokens
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            // Load user details
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

            user.setRefreshToken(refreshToken);
            user.updateLastLogin();
            userRepository.save(user);

            auditService.logAction(
                user.getId(),
                AuditAction.LOGIN,
                AuditEntityType.AUTH,
                user.getId(),
                "User logged in: '" + user.getUsername() + "'"
            );

            log.info("User logged in successfully: {}", request.getUsername());

            return buildAuthResponse(user, accessToken, refreshToken);

        } catch (BadCredentialsException ex) {
            // Wrong username or password
            log.warn("Login failed for user: {} - Invalid credentials", request.getUsername());
            loginAttemptService.recordFailure(request.getUsername());
            throw new UnauthorizedException("Invalid username or password");

        } catch (AuthenticationException ex) {
            // Other authentication errors (locked account, disabled, etc.)
            log.warn("Login failed for user: {} - {}", request.getUsername(), ex.getMessage());
            loginAttemptService.recordFailure(request.getUsername());
            throw new UnauthorizedException("Authentication failed: " + ex.getMessage());
            
        } catch (Exception ex) {
            // Unexpected errors
            log.error("Unexpected error during login for user: {}", request.getUsername(), ex);
            throw new RuntimeException("An unexpected error occurred during login");
        }
    }

    
    /**
     * register - Tạo user mới và auto-login
     * 
     * Flow:
     * 1. Validate input (username format, email format, password strength)
     * 2. Check duplicates (username, email)
     * 3. Hash password với BCrypt
     * 4. Create user entity với default role (ROLE_USER)
     * 5. Save to database
     * 6. Auto-login (generate tokens)
     * 7. Return AuthResponse
     * 
     * Example:
     * RegisterRequest request = RegisterRequest.builder()
     *     .username("john_doe")
     *     .email("john@example.com")
     *     .password("password123")
     *     .fullName("John Doe")
     *     .build();
     * AuthResponse response = authService.register(request);
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        
        log.info("Register attempt for username: {}", request.getUsername());

        // 1. Check username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.error("Username already taken: {}", request.getUsername());
            throw new DuplicateResourceException("Username is already taken");
        }

        // 2. Check email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.error("Email already registered: {}", request.getEmail());
            throw new DuplicateResourceException("Email is already registered");
        }

        // 3. Hash password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        log.debug("Password hashed for user: {}", request.getUsername());

        // 4. Create user entity
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(hashedPassword)
            .fullName(request.getFullName())
            .roles("ROLE_USER")
            .active(true)
            .deleted(false)
            .build();

        // 5. Save user to database
        user = userRepository.save(user);

        auditService.logAction(
            user.getId(),
            AuditAction.REGISTER,
            AuditEntityType.USER,
            user.getId(),
            "New user registered: '" + user.getUsername() + "'"
        );

        log.info("User registered successfully: {}", request.getUsername());

        // 6. Auto-login: Generate tokens
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getUsername(),
            request.getPassword(),
            user.getRoleSet().stream()
                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.name()))
                .toList()
        );

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        // 7. Build và return AuthResponse
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    /**
     * refreshToken - Generate new access token từ refresh token
     * 
     * Flow:
     * 1. Validate refresh token (signature, expiration)
     * 2. Extract username từ refresh token
     * 3. Load user từ database
     * 4. Generate new access token
     * 5. Return AuthResponse (reuse refresh token)
     * 
     * Example:
     * RefreshTokenRequest request = new RefreshTokenRequest("eyJhbGci...");
     * AuthResponse response = authService.refreshToken(request);
     * // response.getAccessToken() → new token
     * // response.getRefreshToken() → same token
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        String refreshToken = request.getRefreshToken();
        log.info("Refresh token attempt");

        try {
            // 1. Validate refresh token
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                log.error("Invalid refresh token");
                throw new UnauthorizedException("Invalid or expired refresh token");
            }

            // 2. Extract username từ refresh token
            String username = jwtTokenProvider.extractUsername(refreshToken);

            if (username == null) {
                log.error("Cannot extract username from refresh token");
                throw new UnauthorizedException("Invalid refresh token");
            }

            log.debug("Refresh token valid for user: {}", username);

            // 3. Load user entity
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found for the provided refresh token"));  // ✅ FIX

            // 4. Check if user is active
            if (!user.isActive()) {
                log.error("User account is inactive: {}", username);
                throw new UnauthorizedException("User account is inactive");
            }

            if (!refreshToken.equals(user.getRefreshToken())) {
                // The token is structurally valid (JWT signature OK) but doesn't match
                // what we have in the DB. This means either:
                //   (a) An old, already-rotated token is being replayed (theft scenario), or
                //   (b) The token was already used and rotated.
                // In either case, clear the stored token to invalidate all sessions.
                log.warn("SECURITY ALERT — refresh token reuse detected for user: {}. " +
                        "Clearing stored token to terminate all sessions.", username);
                userSessionService.clearRefreshToken(user);
                throw new UnauthorizedException("Refresh token has already been used. Please log in again.");
            }
            // 5. Generate new access token
            String roles = user.getRoleSet().stream()
                .map(RoleType::name)
                .collect(Collectors.joining(","));

            String newAccessToken = jwtTokenProvider.generateAccessToken(username, roles);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

            user.setRefreshToken(newRefreshToken);
            userRepository.save(user);

            log.info("Token refreshed successfully for user: {}", username);

            // 6. Build và return AuthResponse
            return buildAuthResponse(user, newAccessToken, newRefreshToken);

        } catch (UnauthorizedException ex) {
            // Re-throw custom exception
            throw ex;
            
        } catch (Exception ex) {
            // Log unexpected errors
            log.error("Unexpected error during token refresh", ex);
            throw new UnauthorizedException("Failed to refresh token: " + ex.getMessage());
        }
    }

    // Logout
    @Transactional
    public void logout(String rawAccessToken) {
        // Step 1: Extract jti and TTL from the access token
        String jti = jwtTokenProvider.extractJti(rawAccessToken);
        if (jti == null) {
            log.warn("Logout failed: Unable to extract jti from token");
            return; // Cannot blacklist without jti
        }

        long remainingTTL = jwtTokenProvider.getTokenExpirationInMillis(rawAccessToken);
        String username = jwtTokenProvider.extractUsername(rawAccessToken);

        // Step 2: Blacklist the token's jti in Redis
        if (jti != null) {
            tokenBlacklistService.blacklist(jti, remainingTTL);
        }

        // Step 3: Clear the refresh token from DB
        if (username != null) {
            userRepository.findByUsername(username).ifPresent(user -> {
                userSessionService.clearRefreshToken(user);
                auditService.logAction(
                    user.getId(),
                    AuditAction.LOGOUT,
                    AuditEntityType.AUTH,
                    user.getId(),
                    "User logged out: '" + user.getUsername() + "'"
                );
                log.info("User {} logged out successfully", username);
            });
        }

        log.info("Logout completed for token jti={}, username={}", jti, username);
    }

    /**
     * buildAuthResponse - Helper method để build AuthResponse DTO
     */
    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {

        // Convert roles Set<RoleType> -> String[]
        String[] roles = user.getRoleSet().stream()
            .map(RoleType::name)
            .toArray(String[]::new);

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .username(user.getUsername())
            .email(user.getEmail())
            .roles(roles)
            .build();
    }
}
