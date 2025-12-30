package com.taskmanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.taskmanagement.entity.TaskPriority;
import com.taskmanagement.entity.TaskStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * TaskResponse - DTO dùng để trả dữ liệu task về cho client
 * 
 * Mục đích:
 * - Định nghĩa cấu trúc dữ liệu trả về của API cho các task
 * - Kiểm soát dữ liệu nào được phép gửi cho client
 * - Tránh lỗi lazy loading
 * - Sử dụng DTO lồng nhau cho các entity liên quan
 * 
 * Được sử dụng bởi:
 * - GET /api/tasks/{id}
 * - GET /api/tasks (danh sách)
 * - POST /api/tasks (sau khi tạo)
 * - PUT /api/tasks/{id} (sau khi cập nhật)
 * 
 * Quyết định thiết kế:
 * - Bao gồm đầy đủ thông tin quan trọng của task
 * - Dùng UserSummary cho assignee (không trả toàn bộ User)
 * - Dùng ProjectSummary cho project
 * - Các timestamp dùng định dạng ISO-8601
 * - Không chứa dữ liệu nhạy cảm (như password hash)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDateTime dueDate;
    private LocalDateTime startDate;
    private LocalDateTime completedAt;
    private Integer estimatedHours;
    private String notes;

/**
* List of users assigned to this task
* Empty list = UNASSIGNED task
*/
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private List<UserSummary> assignees;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private ProjectSummary project;
    private Integer commentCount;

/**
 * Số lượng file đính kèm
 */
    private Integer attachmentCount;

    private Boolean overdue;

/**
 * Số giờ còn lại đến hạn (âm nếu đã quá hạn)
 * Field tính toán
 */
    private Long hoursUntilDue;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

// ==================== NESTED DTOs ====================

    /**
     * UserSummary - 
     * Tránh hiển thị toàn bộ thực thể Người dùng với dữ liệu nhạy cảm
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
     * ProjectSummary - Thông tin dự án tối thiểu
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectSummary {
        private Long id;
        private String name;
        private Boolean active;
    }

// ==================== FACTORY METHODS ====================

/**
 * Tạo đối tượng TaskResponse từ entity Task
 * 
 * Phương thức static giúp việc chuyển đổi dễ dàng
 * 
 * @param task Task entity
 * @return TaskResponse DTO
 */
    public static TaskResponse from(com.taskmanagement.entity.Task task) {
        if (task == null) {
            return null;
        }
        
        List<UserSummary> assigneeSummaries = new ArrayList<>();
        if (task.getAssignees() != null && !task.getAssignees().isEmpty()) {
            assigneeSummaries = task.getAssignees().stream().map(user -> UserSummary.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build())
            .collect(Collectors.toList());
        }
        
        ProjectSummary projectSummary = null;
        if (task.getProject() != null) {
            projectSummary = ProjectSummary.builder()
                .id(task.getProject().getId())
                .name(task.getProject().getName())
                .active(task.getProject().getActive())
                .build();
        }

        return TaskResponse.builder()
            .id(task.getId())
            .title(task.getTitle())
            .description(task.getDescription())
            .status(task.getStatus())
            .priority(task.getPriority())
            .dueDate(task.getDueDate())
            .startDate(task.getStartDate())
            .completedAt(task.getCompletedAt())
            .estimatedHours(task.getEstimatedHours())
            .notes(task.getNotes())
            .assignees(assigneeSummaries)
            .project(projectSummary)
            .commentCount(0)
            .attachmentCount(0)
            .overdue(task.isOverdue())
            .hoursUntilDue(task.hoursUntilDue())
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .build();
    }
}
