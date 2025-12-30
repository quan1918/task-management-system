package com.taskmanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
/**
 * User entity đại diện cho một người dùng trong ứng dụng.
 * 
 * Mục đích:
 * - Xác thực người dùng (thông tin đăng nhập)
 * - Lưu trữ thông tin hồ sơ người dùng
 * - Theo dõi hoạt động và trạng thái tài khoản
 * - Là điểm tham chiếu cho việc gán task, bình luận, và thành viên dự án
 * 
 * Quan hệ (sẽ được thêm sau):
 * - One-to-Many với Task (các task được giao)
 * - One-to-Many với Comment (các bình luận đã viết)
 * - Many-to-Many với Project (tham gia nhiều dự án)
 * 
 * Các quyết định thiết kế:
 * - Chưa dùng BaseEntity (giữ đơn giản ở giai đoạn đầu)
 * - Mật khẩu lưu dưới dạng hash (không bao giờ lưu plaintext)
 * - Email và username phải là duy nhất
 * - Soft delete thông qua trường 'active'
 * - Trường audit để phục vụ kiểm tra và theo dõi
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_email", columnList = "email", unique = true),
        @Index(name = "idx_username", columnList = "username", unique =true)
    }
)
@SQLDelete(sql = "UPDATE users SET deleted = true, deleted_at = NOW() WHERE id = ?")
// REMOVED @Where annotation - it was blocking assignees JOIN FETCH
// Filter deleted users in repository queries instead
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"passwordHash", "assignedTasks", "ownedProjects", "comments"})
public class User {

// ==================== PRIMARY KEY ====================
/**
 * Primary key - ID tự động sinh
 * 
 * @GeneratedValue(IDENTITY): Sử dụng auto-increment của database
 * Lý do: PostgreSQL dùng kiểu SERIAL nên phù hợp cho tăng ID tự động
 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

// ==================== CORE FIELDS ====================
/**
 * Username - định danh duy nhất để đăng nhập
 * 
 * @NotBlank: Không được null hoặc chuỗi rỗng
 * @Size: Giới hạn từ 3–50 ký tự
 * @Column(unique=true): Database đảm bảo tính duy nhất
 * 
 * Lý do: Username là thông tin đăng nhập chính
 */
    @NotBlank
    @Size(min =3, max =50)
    @Column(nullable = false, unique = true, length = 50)
    private String username;

/**
 * Email - dùng để liên hệ hoặc đăng nhập thay thế
 * 
 * @Email: Kiểm tra đúng định dạng email (theo RFC 5322)
 * @Column(unique=true): Mỗi email chỉ gán cho một người dùng
 * 
 * Lý do: Email cần thiết cho thông báo và khôi phục mật khẩu
 */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be vaild")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

/**
 * Họ tên đầy đủ - hiển thị trong giao diện
 * 
 * Lý do: Phân biệt tên hiển thị với tên đăng nhập
 * Ghi chú: Không bắt buộc duy nhất (nhiều người có thể trùng tên)
 */
    @NotBlank(message = "Full name is required")
    @Size(min =2, max = 100, message ="Full name must be between 2 and 100 characters")
    @Column(nullable = false, length = 100)
    private String fullName;

/**
 * Password hash - mật khẩu đã mã hóa (KHÔNG lưu plaintext)
 * 
 * Lý do length=255: BCrypt ~60 ký tự nhưng để phòng trường hợp dùng thuật toán khác
 * 
 * Ghi chú bảo mật:
 * - Lưu bằng BCrypt (ví dụ: List<User>a...)
 * - Tuyệt đối không lưu hoặc log mật khẩu gốc
 * - Việc validate mật khẩu nằm trong AuthService
 */
    @NotBlank(message = "Password is required")
    @Column(nullable = false, length = 255)
    private String passwordHash;

// ==================== STATUS & METADATA ====================
/**
 * Active Flag - dùng cho soft delete
 * 
 * @Builder.Default: Gán giá trị mặc định khi dùng builder pattern
 * 
 * Lý do:
 * - Giữ dữ liệu lịch sử ngay cả khi user bị “xoá”
 * - active=true: tài khoản vẫn hoạt động
 * - active=false: tài khoản bị vô hiệu hóa (soft delete)
 */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

/**
 * Thời điểm đăng nhập lần cuối - theo dõi hoạt động người dùng
 * 
 * Lý do: Phục vụ audit bảo mật, kiểm tra người dùng không hoạt động
 * Ghi chú: Cập nhật trong AuthService khi đăng nhập thành công
 */
    @Column
    private LocalDateTime LastLoginAt;

// ==================== AUDIT FIELDS ====================
/**
 * Thời điểm tạo tài khoản
 * 
 * @CreationTimestamp: Hibernate tự set khi INSERT
 * @Column(updatable=false): Không được sửa sau khi tạo
 * 
 * Lý do: Phục vụ log, kiểm tra, tracking vòng đời người dùng
 */
    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;    

/**
 * Thời điểm cập nhật cuối cùng
 * 
 * @UpdateTimestamp: Hibernate tự cập nhật khi UPDATE
 * 
 * Lý do: Theo dõi hồ sơ người dùng thay đổi lúc nào
 */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

// ==================== BUSINESS LOGIC METHODS ====================
/**
 * Kiểm tra tài khoản đang hoạt động hay không
 * 
 * Lý do: Tạo phương thức tiện dụng, dễ đọc trong code
 * Ví dụ: if (user.isActive()) { ... }
 */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }

/**
 * Vô hiệu hóa tài khoản (soft delete)
 * 
 * Lý do: Giữ lại dữ liệu để phục vụ log/audit
 * Ghi chú: Tài khoản bị deactivate sẽ không thể đăng nhập
 */
    public void deactivate() {
        this.active = false;
    }

/**
 * Kích hoạt lại tài khoản
 * 
 * Lý do: Cho phép khôi phục tài khoản đã vô hiệu hóa
 */
    public void activate() {
        this.active = true;
    }

/**
 * Cập nhật thời điểm đăng nhập gần nhất
 * 
 * Lý do: Được gọi bởi AuthService sau khi đăng nhập thành công
 */
    public void updateLastLogin() {
        this.LastLoginAt = LocalDateTime.now();
    }

// ========== SOFT DELETE FIELDS ==========
    
    /**
     * Soft delete flag
     * - true: User đã bị xóa (logical delete)
     * - false: User vẫn active
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    /**
     * Thời điểm xóa user
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Admin user đã xóa (audit trail)
     */
    @Column(name = "deleted_by")
    private Long deletedBy;

    // ========== RELATIONSHIPS ==========

    @ManyToMany(mappedBy = "assignees", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private Set<Task> assignedTasks = new HashSet<>();

    @OneToMany(mappedBy = "owner")
    @Builder.Default
    @JsonIgnore
    private List<Project> ownedProjects = new ArrayList<>();

    @OneToMany(mappedBy = "author")
    @Builder.Default
    @JsonIgnore
    private List<Comment> comments = new ArrayList<>();

    // ========== LIFECYCLE CALLBACKS ==========

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

      