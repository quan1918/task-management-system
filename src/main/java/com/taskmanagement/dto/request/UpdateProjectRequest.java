package com.taskmanagement.dto.request;

import lombok.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * UpdateProjectRequest - DTO cho việc cập nhật project
 * 
 * Validation Rules:
 * - name: optional, 3-100 chars nếu có
 * - description: optional, max 500 chars
 * - ownerId: optional, phải là user tồn tại nếu có
 * - startDate: optional
 * - endDate: optional, phải sau startDate
 * - active: optional, boolean
 * 
 * Partial Update:
 * - Chỉ update các field != null
 * - Field null = không thay đổi
 * 
 * Business Rules:
 * - Không thể update project đã archived (active = false)
 * - Nếu đổi owner → owner mới phải active
 * - Nếu đổi endDate → phải sau startDate hiện tại
 * 
 * Example Request Body:
 * {
 *   "name": "Website Redesign v2",
 *   "description": "Updated description",
 *   "endDate": "2026-06-30"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProjectRequest {
    
    @Size(min = 3, max = 100, message = "Project name must be between 3 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Long ownerId;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean active;
}
