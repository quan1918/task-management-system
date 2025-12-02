package com.taskmanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Task entity – Thực thể cốt lõi trong hệ thống quản lý công việc (Task Management System)
 *
 * Mục đích:
 * - Đại diện cho một công việc cần được thực hiện
 * - Theo dõi vòng đời của task từ lúc tạo đến lúc hoàn thành
 * - Gán công việc cho thành viên trong nhóm
 * - Tổ chức các task trong từng project
 * - Theo dõi tiến độ và deadline
 *
 * Quan hệ:
 * - Many-to-One với User (assignee) – người được giao task
 * - Many-to-One với Project – task thuộc về project nào
 * - One-to-Many với Comment – bình luận trao đổi (thêm sau)
 * - One-to-Many với Attachment – tệp đính kèm (thêm sau)
 *
 * Các quyết định thiết kế:
 * - Mỗi task bắt buộc phải có người được giao (tính trách nhiệm)
 * - Mỗi task bắt buộc thuộc về một project (tổ chức công việc)
 * - Status và priority là bắt buộc (tránh mơ hồ)
 * - Due date là bắt buộc (công việc phải có thời hạn)
 * - Thời điểm hoàn thành được lưu riêng khỏi status
 */
@Entity
@Table(
    name = "tasks",
    indexes = {
        @Index(name = "idx_task_status", columnList = "status"),
        @Index(name = "idx_task_priority", columnList = "priority"),
        @Index(name = "idx_task_assignee", columnList = "assignee_id"),
        @Index(name = "idx_task_project", columnList = "project_id"),
        @Index(name = "idx_task_due_date", columnList = "due_date"),
        @Index(name = "idx_task_assignee_status", columnList = "assignee_id, status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {
    
// ==================== PRIMARY KEY ====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

// ==================== CORE FIELDS ====================
/**
 * Task title – Tiêu đề mô tả ngắn gọn công việc
 *
 * @NotBlank: Bắt buộc, không được để trống
 * @Size: Tối thiểu 3, tối đa 255 ký tự
 *
 * Lý do: Mỗi task cần có tên rõ ràng, dễ tìm kiếm
 * Ví dụ: "Fix login bug", "Design homepage", "Write API docs"
 */
    @NotBlank(message = "Task title is required")
    @Size(min = 3, max= 255, message = "Task title must be between 3 and 255 characters")
    @Column(nullable = false, length =255)
    private String title;

/**
 * Task description – Mô tả chi tiết công việc
 *
 * @NotBlank: Bắt buộc để rõ ràng yêu cầu
 * @Size: 10–2000 ký tự
 * @Column(TEXT): Cho phép mô tả dài
 *
 * Lý do: Cung cấp ngữ cảnh, yêu cầu, tiêu chí nghiệm thu
 */
    @NotBlank(message = "Task description is required")
    @Size(min = 10, max = 2000, message = "Task description must be between 10 and 2000 characters")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

/**
 * Task status – Trạng thái hiện tại trong vòng đời task
 *
 * @Enumerated(STRING): Lưu dạng chuỗi (“PENDING”) thay vì số (0)
 * @NotNull: Luôn phải biết trạng thái
 * @Builder.Default: Task mới mặc định là PENDING
 *
 * Vì sao dùng STRING thay vì ORDINAL?
 * - STRING: dễ đọc, an toàn khi thay đổi thứ tự enum
 * - ORDINAL: nhỏ nhưng dễ lỗi khi đổi thứ tự enum
 */
    @NotNull(message = "Task status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

/**
 * Task priority – Mức độ ưu tiên & tầm quan trọng
 *
 * @Builder.Default: Mặc định là MEDIUM
 *
 * Lý do: Dùng để sắp xếp và phân bổ nguồn lực
 */
    @NotNull(message = "Task priority is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

/**
 * Due date – Thời hạn phải hoàn thành task
 *
 * @NotNull: Mọi task đều cần deadline
 * @FutureOrPresent: Không được đặt deadline trong quá khứ
 *
 * Lý do: Công việc có thời hạn sẽ dễ hoàn thành hơn
 * Lưu ý: Chỉ kiểm tra lúc tạo, không ngăn việc bị quá hạn sau đó
 */
    @NotNull(message = "Due date is required")
    @FutureOrPresent(message = "Due date must be in the present or future")
    @Column(nullable = false)
    private LocalDateTime dueDate;

/**
 * Start date – Thời điểm task thực sự bắt đầu
 *
 * Lý do tùy chọn: Được set khi trạng thái chuyển sang IN_PROGRESS
 * Mục đích: Tính thời gian thực tế làm task
 */
    @Column
    private LocalDateTime startDate;

/**
 * Completion date – Thời điểm task hoàn thành
 *
 * Lý do tùy chọn: Set khi trạng thái chuyển COMPLETED
 * Mục đích: Theo dõi thời gian giao việc, tính velocity
 */
    @Column
    private LocalDateTime completeAt;

/**
 * Estimated hours – Thời gian dự kiến cần để hoàn thành task
 *
 * @Min(0): Không được âm
 * @Max(999): Giới hạn hợp lý (tối đa 41 ngày)
 *
 * Lý do tùy chọn: Không phải đội nào cũng estimate theo giờ
 * Mục đích: Planning & phân bổ workload
 */
    @Min(value = 0, message = "Estimated hours cannot be negative")
    @Max(value = 999, message = "Estimated hours cannot exceed 999")
    @Column
    private Integer estimatedHours;

/**
 * Notes – Ghi chú bổ sung
 *
 * Lý do: Trường linh hoạt để thêm thông tin khó đặt ở nơi khác
 * Ví dụ: Link, tài liệu tham khảo, hướng dẫn đặc biệt
 */
    @Size(max = 1000)
    @Column(length = 1000)
    private String notes;

// ==================== RELATIONSHIPS ====================
/**
 * Many-to-One với User (Assignee)
 *
 * Quan hệ: Nhiều task → Một user (được giao)
 * Ví dụ: User "John" được giao Task 1, 2, 3
 *
 * @NotNull: Mỗi task bắt buộc phải có người chịu trách nhiệm
 *
 * Fetch = LAZY: Không load user khi lấy danh sách task
 * Cascade = NONE: Xóa task không xóa user
 *
 * optional=false: Ràng buộc NOT NULL trong DB
 *
 * Lý do: Không tồn tại task mà không có ai phụ trách
 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignee_id", nullable = false)
    @NotNull(message = "Task assignee is required")
    private User assignee;

/**
 * Many-to-One với Project
 *
 * Quan hệ: Nhiều task → Một project
 *
 * @NotNull: Task phải thuộc về một project
 *
 * Fetch = LAZY: Không tự động load project
 * Cascade = NONE: Xóa task không ảnh hưởng project
 *
 * Lý do: Task cần được tổ chức theo project
 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    @NotNull(message = "Task project i required")
    private Project project;
    
// ==================== AUDIT FIELDS ====================

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

// ==================== BUSINESS LOGIC METHODS ====================
/**
 * assignTo – Giao task cho user
 *
 * Quy tắc nghiệp vụ:
 * - User không được null
 * - Có thể đổi người giao (reassign)
 * - Không thay đổi trạng thái task
 */
    public void assignTo(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        this.assignee = user;
    }

/**
 * start – Bắt đầu thực hiện task
 *
 * Quy tắc nghiệp vụ:
 * - Chỉ bắt đầu được task PENDING hoặc BLOCKED
 * - Set trạng thái thành IN_PROGRESS
 * - Ghi lại thời điểm start
 */
    public void start() {
        if (this.status != TaskStatus.PENDING && this.status != TaskStatus.BLOCKED){
            throw new IllegalStateException("Can only start a task that is PENDING or BLOCKED. Current status: " + this.status);
        }
        this.status = TaskStatus.IN_PROGRESS;
        this.startDate = LocalDateTime.now();
    }

/**
 * complete – Đánh dấu task hoàn thành
 *
 * Quy tắc nghiệp vụ:
 * - Chỉ hoàn thành được khi đang IN_PROGRESS
 * - Set trạng thái COMPLETED
 * - Lưu thời điểm completedAt
 */
    public void complete() {
        if (this.status != TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Can only complete a task that in IN_PROGRESS. Current status: " + this.status
            );
        }
        this.status = TaskStatus.COMPLETED;
        this.completeAt = LocalDateTime.now();
    }

/**
 * block – Chặn task (đang chờ phụ thuộc bên ngoài)
 *
 * Quy tắc:
 * - Không được block task đã COMPLETED hoặc CANCELLED
 * - Set trạng thái BLOCKED
 * - Lưu lại lý do vào notes
 */
    public void block(String reason) {
        if (this.status.isTerminal()) {
            throw new IllegalStateException("Cannot block completed or cancelled task");
        }
        this.status = TaskStatus.BLOCKED;
        this.notes = (this.notes != null ? this.notes + "\n" : "") + "[BLOCKED] " + LocalDateTime.now() + ": " + reason;
    }

/**
 * cancel – Hủy task
 *
 * Quy tắc:
 * - Không được hủy task đã completed
 * - Set trạng thái CANCELLED
 */
    public void cancel() {
        if (this.status == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed task");
        }
        this.status = TaskStatus.CANCELLED;
    }
/**
 * isOverdue – Kiểm tra task có quá hạn không
 *
 * Logic: Due date đã qua nhưng task chưa hoàn thành
 */
    public boolean isOverdue() {
        return !this.status.isTerminal() && LocalDateTime.now().isAfter(this.dueDate);
    }

/**
 * isAssignedTo – Kiểm tra task có được giao cho user cụ thể không
 */
    public boolean isAssignedTo(User user) {
        return user != null && this.assignee != null &&this.assignee.getId().equals(user.getId());
    }

/**
 * belongsToProject – Kiểm tra task có thuộc project cụ thể không
 */
    public boolean belongsToProject(Project project) {
        return project != null && this.project != null && this.project.getId().equals(project.getId());
    }

/**
 * hoursUntilDue – Tính số giờ còn lại trước deadline
 *
 * Trả về số âm nếu đã quá hạn
 */
    public long hoursUntilDue() {
        return java.time.Duration.between(LocalDateTime.now(), this.dueDate).toHours();
    }
}

