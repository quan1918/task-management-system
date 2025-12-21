package com.taskmanagement.dto.request;

import lombok.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * CreateProjectRequest - DTO cho việc tạo project mới
 * 
 * Validation Rules:
 * - name: required, 3-100 chars, unique
 * - description: optional, max 500 chars
 * - ownerId: required, phải là user tồn tại
 * - startDate: optional
 * - endDate: optional, phải sau startDate
 * 
 * Business Rules:
 * - Owner phải là user active
 * - Project mặc định active = true
 * - Nếu không có startDate → set = hôm nay
 * 
 * Example Request Body:
 * {
 *   "name": "Website Redesign",
 *   "description": "Redesign company website with modern UI/UX",
 *   "ownerId": 5,
 *   "startDate": "2025-12-20",
 *   "endDate": "2026-03-31"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(min =3, max = 100, message = "Project name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Description cannot be blank")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Owner ID is required")
    private Long ownerId;

    private LocalDate startDate;

    private LocalDate endDate;

    /**
     * Custom validation: endDate phải sau startDate
     */
    public boolean isEndDateValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }
    
}
