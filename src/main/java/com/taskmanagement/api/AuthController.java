package com.taskmanagement.api;

import com.taskmanagement.dto.auth.*;
import com.taskmanagement.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController - REST API endpoints cho authentication
 * 
 * Endpoints:
 * - POST /api/auth/login      - Login với username/password
 * - POST /api/auth/register   - Register user mới
 * - POST /api/auth/refresh    - Refresh access token
 * 
 * All endpoints are public (không cần JWT token)
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

        log.info("Login request received for username: {}", request.getUsername());

        AuthResponse response = authService.login(request);

        log.info("Login successful for username: {}", request.getUsername());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {

        log.info("Register request received for username: {}", request.getUsername());

        AuthResponse response = authService.register(request);

        log.info("Registration successful for username: {}", request.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {

        log.info("Refresh token request received");

        AuthResponse response = authService.refreshToken(request);

        log.info("Refresh token successful for username: {}", response.getUsername());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String rawToken = authHeader.substring(7);

        authService.logout(rawToken);
        return ResponseEntity.noContent().build();
    }
}
