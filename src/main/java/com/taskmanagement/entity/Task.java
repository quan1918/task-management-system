package com.taskmanagement.entity;

// JPA/Hibernate annotations
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

// Lombok
import lombok.*;

// Hibernate timestamps
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.annotations.OnDeleteAction;

// Java built-in
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

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
        @Index(name = "idx_task_assignee_status", columnList = "assignee_id, status"),
        @Index(name = "idx_task_deleted", columnList = "deleted"), 
        @Index(name = "idx_task_deleted_at", columnList = "deleted_at")  
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"assignees", "project", "comments", "attachments"})
public class Task {
    
// ==================== PRIMARY KEY ====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
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
    //@FutureOrPresent(message = "Due date must be in the present or future")
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
    private LocalDateTime completedAt;

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
 * Multi assignees per task - Nhiều người được giao cho một task
 * 
 * Business Rules:
 * - Minimum 1 assignee (validated in service layer)
 * - Maximum 10 assignees (avoid diffusion of responsibility)
 * - Use Set to prevent duplicates
 */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "task_assignees",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id"),
        indexes = {
            @Index(name = "idx_task_assignee_user", columnList = "user_id"),
            @Index(name = "idx_task_assignee_task", columnList = "task_id"),
        }
    )
    @Builder.Default
    private Set<User> assignees = new HashSet<>();


/**
 * Many-to-One với Project
 * Fetch = LAZY: Không tự động load project
 * Cascade = NONE: Xóa task không ảnh hưởng project
 *
 * Lý do: Task cần được tổ chức theo project
 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true)
    private Project project;

/**
 * One-to-Many với Comment
 * Task có thể có nhiều comment
 */
    @OneToMany(
        mappedBy = "task",
        cascade = CascadeType.ALL,
        fetch = FetchType.LAZY,
        orphanRemoval = true
    )
    @Builder.Default
    @JsonIgnore
    private List<Comment> comments = new ArrayList<>();

/**
 * Quan hệ One-to-Many với Attachment
 * Một Task có thể có nhiều file đính kèm
 *
 * Cascade: ALL - Attachment bị xóa khi Task bị xóa
 * Fetch: LAZY - Chỉ tải attachments khi được truy cập
 * OrphanRemoval: true - Xóa attachment khi bị loại khỏi danh sách
 */
    @OneToMany(
        mappedBy = "task",
        cascade = CascadeType.ALL,
        fetch = FetchType.LAZY,
        orphanRemoval = true
    )
    @Builder.Default
    @JsonIgnore
    private List<Attachment> attachments = new ArrayList<>();

    // ==================== SOFT DELETE FIELDS ====================
    // Soft delete flag
    /**
     * Giá trị:
     * - false (default): Task đang active, hiển thị bình thường
     * - true: Task đã bị "xóa", ẩn khỏi UI chính
     * 
     * Cách dùng:
     * - task.softDelete() → Set deleted = true, deletedAt = now()
     * - task.restore() → Set deleted = false, deletedAt = null
     * - task.isDeleted() → Check xem task có bị xóa không
     * 
     * Queries cần filter:
     * - findAll() → WHERE deleted = false (chỉ lấy active tasks)
     * - findById() → WHERE id = ? AND deleted = false
     * - findByProject() → WHERE project_id = ? AND deleted = false
     * 
     * @Column(nullable = false): Bắt buộc phải có giá trị (không được NULL)
     * @Builder.Default: Task mới tạo mặc định deleted = false (active)
     */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    // Thời điểm task bị soft delete
    /**
     * Giá trị:
     * - null (default): Task chưa bị xóa
     * - timestamp: Task bị xóa lúc nào (ví dụ: 2025-12-11 10:30:00)
     * 
     * Dùng cho:
     * - Audit trail: Biết task bị xóa lúc nào, bởi ai (thêm deletedBy sau)
     * - "Recently deleted" feature: Hiển thị tasks xóa trong 7 ngày gần nhất
     * - Auto cleanup: Xóa vĩnh viễn tasks deleted > 90 ngày
     * - Restore deadline: "Bạn có 30 ngày để khôi phục task"
     * 
     * Business rules:
     * - Khi deleted = false → deletedAt PHẢI NULL
     * - Khi deleted = true → deletedAt PHẢI có giá trị
     * - Restore task → Set deletedAt = null
     * 
     * Ví dụ queries:
     * - Tasks xóa trong 7 ngày: WHERE deleted = true AND deletedAt > NOW() - INTERVAL '7 days'
     * - Tasks cần cleanup: WHERE deleted = true AND deletedAt < NOW() - INTERVAL '90 days'
     * 
     * @Column(nullable = true): Cho phép NULL (task active thì deletedAt = null)
     * No @Builder.Default: Mặc định là null
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
    * isOverdue – Kiểm tra task có quá hạn không
    *
    * Logic: Due date đã qua nhưng task chưa hoàn thành
    */
    public boolean isOverdue() {
        return !this.status.isTerminal() && LocalDateTime.now().isAfter(this.dueDate);
    }

    /**
    * hoursUntilDue – Tính số giờ còn lại trước deadline
    *
    * Trả về số âm nếu đã quá hạn
    */
    public long hoursUntilDue() {
        return java.time.Duration.between(LocalDateTime.now(), this.dueDate).toHours();
    }
    
// ==================== AUDIT FIELDS ====================

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

}

