package com.taskmanagement.dto.request;

import com.taskmanagement.annotation.StrongPassword;
import com.taskmanagement.entity.RoleType;
import lombok.*;
import jakarta.validation.constraints.*;

import java.util.Set;

/**
 * CreateUserRequest - DTO cho việc tạo user mới
 * 
 * Validation Rules:
 * - username: required, 3-50 chars, unique
 * - email: required, valid email format, unique
 * - password: required, min 8 chars
 * - fullName: required, max 100 chars
 * 
 * Example Request Body:
 * {
 *   "username": "john_doe",
 *   "email": "john@example.com",
 *   "password": "SecurePass123!",
 *   "fullName": "John Doe"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid format")
    private String email;

    @NotBlank(message = "Password is required")
    @StrongPassword
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    private Set<RoleType> roles;
}
