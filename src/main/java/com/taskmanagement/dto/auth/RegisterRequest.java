package com.taskmanagement.dto.auth;

import com.taskmanagement.annotation.StrongPassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * RegisterRequest - DTO cho user registration
 * 
 * Request body format:
 * {
 *   "username": "john_doe",
 *   "email": "john@example.com",
 *   "password": "SecurePass123!",
 *   "fullName": "John Doe"
 * }
 * 
 * Validation:
 * - username: 3-50 characters, alphanumeric + underscore
 * - email: valid email format
 * - password: 8-100 characters
 * - fullName: 2-100 characters
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @StrongPassword
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;
}
