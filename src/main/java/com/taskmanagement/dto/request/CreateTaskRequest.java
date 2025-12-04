package com.taskmanagement.dto.request;

import com.taskmanagement.entity.TaskPriority;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * CreateTaskRequest - DTO dùng để tạo một task mới
 *
 * Mục đích:
 * - Xác định cấu trúc dữ liệu (API contract) cho việc tạo task
 * - Kiểm tra và hợp lệ hóa dữ liệu đầu vào trước khi xử lý
 * - Tách biệt cấu trúc API khỏi schema trong cơ sở dữ liệu
 *
 * Được sử dụng bởi:
 * - Endpoint POST /api/tasks
 * - TaskController.createTask()
 * - TaskService.createTask()
 *
 * Quyết định thiết kế:
 * - Chỉ bao gồm các trường được phép set khi tạo mới
 * - Không có id (tự động sinh)
 * - Không có timestamps (tự động sinh)
 * - Không có status (mặc định là PENDING)
 * - Các quan hệ chỉ dùng ID thay vì entity đầy đủ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskRequest {
    /**
 * Mô tả task — thông tin chi tiết
 *
 * Ràng buộc:
 * - Bắt buộc (không được để trống)
 * - 10–2000 ký tự
 *
 * Ví dụ: "Users cannot login when using special characters in password"
 */
    @NotBlank(message = "Task title is required")
    @Size(min = 10, max = 2000, message = "Title must be between 3 and 255 characters")
    private String title;

/**
 * Mô tả task — thông tin chi tiết
 *
 * Ràng buộc:
 * - Bắt buộc (không được để trống)
 * - 10–2000 ký tự
 *
 * Ví dụ: "Users cannot login when using special characters in password"
 */
    @NotBlank(message = "Task description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

/**
 * Mức độ ưu tiên của task
 *
 * Ràng buộc:
 * - Bắt buộc
 *
 * Giá trị hợp lệ: LOW, MEDIUM, HIGH, CRITICAL
 */
    @NotBlank(message = "Priority is required")
    private String  priority;

/**
 * Ngày hết hạn — deadline hoàn thành task
 *
 * Ràng buộc:
 * - Bắt buộc
 * - Phải là thời điểm hiện tại hoặc tương lai
 *
 * Ví dụ: "2025-12-15T17:00:00"
 */
    @NotNull(message = "Due date is required")
    @FutureOrPresent(message = "Due date must be in the present or future")
    private LocalDateTime dueDate;

/**
 * Số giờ dự kiến cần để hoàn thành
 *
 * Ràng buộc:
 * - Không bắt buộc
 * - Nếu cung cấp: giá trị 0–999 giờ
 *
 * Ví dụ: 8 (giờ)
 */
    @Min(value = 0, message = "Estimated hours cannot be negative")
    @Max(value = 999, message = "Estimated hours cannot exceed 999")
    private Integer estimatedHours;

/**
 * Ghi chú thêm
 *
 * Ràng buộc:
 * - Không bắt buộc
 * - Tối đa 1000 ký tự
 *
 * Ví dụ: "Related to issue #123"
 */
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

/**
 * ID người được giao task — người sẽ thực hiện task
 *
 * Ràng buộc:
 * - Bắt buộc
 * - Phải là ID hợp lệ
 *
 * Lưu ý: Sử dụng ID thay vì entity User
 * Service layer sẽ kiểm tra user có tồn tại hay không
 */
    @NotNull(message = "Assignee ID is required")
    @Positive(message = "Assignee ID must be positive")
    private Long assigneeId;

/**
 * Project ID — task thuộc về project nào
 *
 * Ràng buộc:
 * - Bắt buộc
 * - Phải là ID hợp lệ
 *
 * Lưu ý: Service layer sẽ kiểm tra project có tồn tại hay không
 */
    @NotNull(message = "Project ID is required")
    @Positive(message = "Project ID must be positive")
    private Long projectId;
}
