package com.taskmanagement.dto.response;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.taskmanagement.entity.TaskStatus;
import com.taskmanagement.entity.Project;
import com.taskmanagement.entity.Task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ProjectResponse - DTO trả về thông tin project
 * 
 * Chứa:
 * - Thông tin cơ bản project
 * - Owner summary (id, username, fullName)
 * - Task statistics (total, completed, pending, in_progress)
 * - Timestamps
 * 
 * Example Response:
 * {
 *   "id": 3,
 *   "name": "Website Redesign",
 *   "description": "Redesign company website",
 *   "active": true,
 *   "owner": {
 *     "id": 5,
 *     "username": "john_doe",
 *     "fullName": "John Doe",
 *     "email": "john@example.com"
 *   },
 *   "startDate": "2025-12-20",
 *   "endDate": "2026-03-31",
 *   "taskStatistics": {
 *     "total": 25,
 *     "completed": 10,
 *     "inProgress": 8,
 *     "pending": 7
 *   },
 *   "createdAt": "2025-12-17T10:00:00",
 *   "updatedAt": "2025-12-17T10:00:00"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {
    
    private Long id;
    private String name;
    private String description;
    private Boolean active;
    
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private UserSummary owner;

    private LocalDate startDate;
    private LocalDate endDate;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private TaskStatistics taskStatistics;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * UserSummary - Thông tin tóm tắt user
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserSummary {
        private Long id;
        private String username;
        private String fullName;
        private String email;
    }

    /**
     * TaskStatistics - Thống kê task trong project
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaskStatistics {
        private long total;
        private long completed;
        private long inProgress;
        private long pending;
    }

    /**
     * Chuyển đổi từ entity Project sang DTO ProjectResponse
     * 
     * @param project Entity Project
     * @return DTO ProjectResponse
     */
    public static ProjectResponse from(Project project, List<Task> tasks) {
        if(project == null) {
            return null;
        }

        // Chuyển đổi owner sang UserSummary
        UserSummary ownerSummary = null;
        if(project.getOwner() != null) {
            ownerSummary = UserSummary.builder()
                .id(project.getOwner().getId())
                .username(project.getOwner().getUsername())
                .fullName(project.getOwner().getFullName())
                .email(project.getOwner().getEmail())
                .build();
        }

        // Build task statistics
        TaskStatistics stats = TaskStatistics.builder()
            .total(tasks != null ? tasks.size() : 0)
            .completed(tasks != null ?
                tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.COMPLETED).count() : 0)
            .inProgress(tasks != null ?
                tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count() : 0)
            .pending(tasks != null ?
                tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.PENDING).count() : 0)
            .build();

        return ProjectResponse.builder()
            .id(project.getId())
            .name(project.getName())
            .description(project.getDescription())
            .active(project.getActive())
            .owner(ownerSummary)
            .startDate(project.getStartDate())
            .endDate(project.getEndDate())
            .taskStatistics(stats)
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt())
            .build();
    }

}
