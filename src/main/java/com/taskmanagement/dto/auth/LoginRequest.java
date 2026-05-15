package com.taskmanagement.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * LoginRequest - DTO cho login request
 * 
 * Request body format:
 * {
 *   "username": "john_doe",
 *   "password": "password123"
 * }
 * 
 * Validation:
 * - username: required, not blank
 * - password: required, not blank
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
