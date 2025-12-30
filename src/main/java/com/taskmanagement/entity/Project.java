package com.taskmanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Project entity đại diện cho một workspace hoặc một “container” chứa các task.
 *
 * Mục đích:
 * - Gom nhóm các task liên quan vào cùng một dự án
 * - Xác định phạm vi (scope) và giới hạn của dự án
 * - Theo dõi vòng đời dự án (đang hoạt động / đã lưu trữ)
 * - Gán chủ sở hữu và các thành viên trong dự án
 *
 * Quan hệ:
 * - Many-to-One với User (owner/creator)
 * - One-to-Many với Task (dự án chứa nhiều task) – sẽ thêm sau
 * - Many-to-Many với User (các thành viên dự án) – Phase 2
 *
 * Quyết định thiết kế:
 * - Mọi project bắt buộc phải có một owner (người tạo)
 * - Project có thể được archive (mô hình soft delete)
 * - Mô tả là tùy chọn (cho phép tạo project nhanh)
 * - Owner không thể thay đổi sau khi tạo (immutable)
 */
@Entity
@Table(
    name = "projects",
    indexes = {
        @Index(name = "idx_project_name", columnList = "name"),
        @Index(name = "idx_project_owner", columnList = "owner_id"),
        @Index(name = "idx_project_active", columnList = "active")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"owner", "tasks"})
public class Project {

// ==================== PRIMARY KEY ====================
/**
 * Khóa chính – ID tự tăng
 *
 * Lý do dùng IDENTITY: PostgreSQL sử dụng SERIAL (auto-increment) để sinh ID
 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

// ==================== CORE FIELDS ====================
/**
 * Tên project – định danh duy nhất trong tổ chức
 *
 * @NotBlank: Bắt buộc nhập
 * @Size: 3–100 ký tự để đảm bảo tên hợp lý
 *
 * Lý do: Project cần tên rõ ràng, dễ tìm kiếm
 * Ví dụ: "Website Redesign", "Q4 Marketing Campaign"
 */
    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 100, message = "Project name must be between 3 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

/**
 * Mô tả dự án – thông tin chi tiết về mục tiêu dự án
 *
 * @Size(max=1000): Cho phép mô tả dài nhưng có giới hạn
 * @Column(TEXT): Lưu dạng TEXT (không giới hạn như VARCHAR)
 *
 * Lý do: Trường tùy chọn để linh hoạt
 * Ghi chú: TEXT cho phép nhiều đoạn văn
 */
    @Size(max = 1000, message = "Description can be up to 1000 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

/**
 * Trạng thái hoạt động của project
 *
 * @Builder.Default: Mặc định là active khi tạo mới
 *
 * Lý do: Mô hình soft delete
 * - active=true: dự án đang hoạt động
 * - active=false: dự án đã lưu trữ / hoàn thành
 *
 * Lợi ích:
 * - Giữ lại dữ liệu lịch sử
 * - Có thể khôi phục project đã archive
 * - Các task vẫn truy cập được khi project archive
 */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

/**
 * startDate - Thời điểm bắt đầu dự án
 *
 * Lý do tùy chọn: Một số dự án không có ngày bắt đầu chính thức
 * Trường hợp: Phân biệt dự án lên kế hoạch và thực thi
 */
    @Column
    private LocalDate startDate;

/**
 * endDate - Ngày hoàn thành dự án
 *
 * Lý do tùy chọn: Dự án ongoing có thể chưa có ngày kết thúc
 * Trường hợp: Theo dõi timeline và hạn chót
 */
    @Column
    private LocalDate endDate;

// ==================== RELATIONSHIPS ====================
/**
 * Quan hệ Many-to-One với User (owner)
 *
 * Quan hệ:
 * - Một user có thể sở hữu nhiều project
 * 
 * @ManyToOne: Nhiều project → 1 user
 * @JoinColumn(name="owner_id"): Tên cột khóa ngoại
 * @NotNull: Bắt buộc phải có owner
 *
 * Fetch = LAZY:
 * - Không tự động tải owner khi tải project
 * - Tối ưu truy vấn
 *
 * Cascade = NONE (mặc định):
 * - Xóa project KHÔNG được xóa user
 *
 * optional=false:
 * - Database bắt buộc phải có owner
 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    @NotNull(message = "Project owner is required")
    private User owner;

// ==================== AUDIT FIELDS ====================
/**
 * createdAt – thời điểm tạo project
 *
 * @CreationTimestamp: Hibernate tự set khi INSERT
 * @Column(updatable=false): Không bao giờ thay đổi
 *
 * Lý do: Theo dõi ngày tạo phục vụ thống kê
 */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

/**
 * updatedAt – thời điểm chỉnh sửa gần nhất
 *
 * @UpdateTimestamp: Hibernate tự cập nhật khi UPDATE
 *
 * Lý do: Biết khi nào project được chỉnh sửa
 */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Relationship với Task
     * 
     * - OneToMany: Một project có nhiều tasks
     * - mappedBy = "project": Bên Task có field "project" (owner của relationship)
     * - fetch = LAZY: Chỉ load tasks khi cần (performance)
     * - cascade = CascadeType.ALL: Khi delete project → cascade sang tasks
     * - orphanRemoval = false: Giữ task khi remove khỏi collection
     * 
     * NOTE: Dùng @Builder.Default để tránh null khi build Project
     */
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<Task> tasks = new ArrayList<>();

// ==================== BUSINESS LOGIC METHODS ====================
/**
 * Kiểm tra dự án có đang active hay không
 *
 * Lý do: Cách đọc dễ hiểu hơn getActive()
 */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }

/**
 * Lưu trữ project (soft delete)
 *
 * Quy tắc nghiệp vụ:
 * - Project đã archive không được thêm task mới
 * - Task cũ vẫn xem được (read-only)
 * - Có thể khôi phục lại sau
 *
 * Lý do: Giữ lịch sử và kết quả công việc
 */
    public void archive() {
        this.active = false;
    }

/**
 * Khôi phục project đã archive
 *
 * Trường hợp: Muốn tiếp tục làm dự án cũ
 *
 * Lý do: Linh hoạt trong quản lý project
 */
    public void reactivate() {
        this.active = true;
    }

/**
 * Kiểm tra dự án đã bắt đầu hay chưa
 *
 * Logic:
 * - Nếu chưa có startDate → đang ở giai đoạn planning
 *
 * Lý do: Phân biệt dự án lên kế hoạch vs đang hoạt động
 */
    public boolean hasStarted() {
        return startDate != null && LocalDate.now().isAfter(startDate);
    }

/**
 * Kiểm tra dự án có bị quá hạn hay không
 *
 * Logic:
 * - Dự án active + endDate < hiện tại → overdue
 *
 * Lý do: Cảnh báo dự án cần ưu tiên
 */
    public boolean isOverdue() {
        return active && endDate != null && LocalDate.now().isAfter(endDate);
    }

/**
 * Kiểm tra user có phải là owner của project hay không
 *
 * Dùng cho logic phân quyền (Authorization)
 * Ví dụ: chỉ owner mới được sửa/xóa project
 */
    public boolean isOwnedBy(User user) {
        return user != null && this.owner.getId().equals(user.getId());
    }

    /**
     * Helper: Thêm task vào project
     */
    public void addTask(Task task) {
        tasks.add(task);
        task.setProject(this);
    }
    
    /**
     * Helper: Xóa task khỏi project
     */
    public void removeTask(Task task) {
        tasks.remove(task);
        task.setProject(null);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

