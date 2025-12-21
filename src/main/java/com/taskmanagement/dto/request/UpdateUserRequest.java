package com.taskmanagement.dto.request;

import lombok.*;
import jakarta.validation.constraints.*;

/**
 * UpdateUserRequest - DTO cho việc cập nhật user
 * 
 * Validation Rules:
 * - email: optional, valid email format nếu có, unique
 * - fullName: optional, max 100 chars
 * - active: optional, boolean
 * - username: KHÔNG cho phép update (immutable)
 * - password: Update qua endpoint riêng PUT /api/users/{id}/password
 * 
 * Partial Update:
 * - Chỉ update các field != null
 * - Field null = không thay đổi
 * 
 * Example Request Body:
 * {
 *   "email": "newemail@example.com",
 *   "fullName": "John Smith",
 *   "active": true
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @Email(message = "Email should be valid format")
    private String email;

    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    private Boolean active;

}
