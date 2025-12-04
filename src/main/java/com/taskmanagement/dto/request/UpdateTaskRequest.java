package com.taskmanagement.dto.request;

import com.taskmanagement.entity.TaskPriority;
import com.taskmanagement.entity.TaskStatus;
import lombok.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/**
 * UpdateTaskRequest - DTO dùng để cập nhật một task hiện có
 * 
 * Mục đích:
 * - Định nghĩa cấu trúc dữ liệu mà API sẽ nhận khi cập nhật task
 * - Cho phép cập nhật một phần (tất cả trường đều tùy chọn trừ khi có rule validation)
 * - Kiểm tra dữ liệu đầu vào sau khi cập nhật
 * 
 * Được sử dụng bởi:
 * - PUT /api/tasks/{id}
 * - PATCH /api/tasks/{id}
 * 
 * Quyết định thiết kế:
 * - Mọi trường đều tùy chọn (nullable)
 * - Chỉ cập nhật các trường được truyền từ client
 * - Chỉ kiểm tra validation nếu trường đó được gửi lên
 * - Không cho phép cập nhật id, timestamps (vì hệ thống quản lý)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTaskRequest {

/**
 * Tiêu đề mới của task (tùy chọn)
 * Nếu được gửi lên, phải thỏa mãn các luật validation
 */
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;

/**
 * Mô tả mới cho task (tùy chọn)
 */
    @Size(min = 10, max = 2000, message = "Description must be berween 10 and 2000 characters")
    private String description;
    
/**
 * Trạng thái mới của task (tùy chọn)
 * Giá trị: PENDING, IN_PROGRESS, COMPLETED, BLOCKED, CANCELLED
 * 
 * Lưu ý: Việc chuyển đổi trạng thái cần được kiểm tra ở service layer
 * Ví dụ: Không thể chuyển từ COMPLETED quay lại PENDING
 */
    private TaskStatus status;

    private TaskPriority priority;

/**
 * Hạn hoàn thành mới (tùy chọn)
 */
    @FutureOrPresent(message = "Due date must be in the present or future")
    private LocalDateTime dueDate;

/**
 * Số giờ ước lượng mới (tùy chọn)
 */
    @Min(value = 0, message = "Estimated hours cannot be negative")
    @Max(value = 999, message = "Estimated hours cannot exceed 999")
    private Integer estimatedHours;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

/**
 * ID người được giao task mới (tùy chọn)
 * Cho phép gán lại task cho user khác
 */
    @Positive(message = "Assignee ID must be positive")
    private Long assigneeId;

/**
 * ID project mới (tùy chọn)
 * Cho phép chuyển task sang project khác
 */
    @Positive(message = "Project ID must be positive")
    private Long projectId;
}
