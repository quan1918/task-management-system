package com.taskmanagement.dto.auth;

import lombok.*;

/**
 * AuthResponse - DTO cho authentication response
 * 
 * Response body format:
 * {
 *   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "tokenType": "Bearer",
 *   "expiresIn": 86400000,
 *   "username": "john_doe",
 *   "email": "john@example.com",
 *   "roles": ["ROLE_USER"]
 * }
 * 
 * Use case:
 * - Login response
 * - Register response (auto-login after register)
 * - Refresh token response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    
    /**
     * JWT access token
     * 
     * Used for API authentication
     * Client phải gửi token này trong header:
     *   Authorization: Bearer <accessToken>
     * 
     * Expiration: 24 hours (configurable)
     * 
     * Example:
     * "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzA1NzQ2MDAwLCJleHAiOjE3MDU4MzI0MDB9.signature"
     */
    private String accessToken;

    /**
     * JWT refresh token
     * 
     * Used to obtain new access token khi access token hết hạn
     * 
     * Expiration: 7 days (configurable)
     * 
     * Flow:
     * 1. Access token expired
     * 2. Client send refresh token to /api/auth/refresh
     * 3. Server validate refresh token
     * 4. Return new access token
     */
    private String refreshToken;

    /**
     * Token type
     * 
     * Always "Bearer" for JWT
     * 
     * Used in Authorization header:
     *   Authorization: Bearer <token>
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Token expiration time (milliseconds)
     * 
     * Example: 86400000 (24 hours)
     * 
     * Client có thể dùng để:
     * - Hiển thị "Token expires in X minutes"
     * - Auto-refresh token trước khi hết hạn
     */
    private long expiresIn;

    private String username;

    private String email;

    /**
     * User roles
     * 
     * Array of role strings
     * Example: ["ROLE_USER", "ROLE_ADMIN"]
     * 
     * Client có thể dùng để:
     * - Show/hide UI elements based on roles
     * - Conditional routing
     */
    private String[] roles;
}
