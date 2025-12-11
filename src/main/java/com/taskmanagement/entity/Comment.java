package com.taskmanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Comment entity đại diện cho một cuộc thảo luận hoặc ghi chú trên một task
 *
 * Mục đích:
 * - Cho phép cộng tác trong team thông qua phần thảo luận của task
 * - Theo dõi lịch sử trao đổi liên quan đến task
 * - Ghi lại câu hỏi, cập nhật và quyết định
 * - Cung cấp audit trail cho các cuộc trò chuyện liên quan đến task
 *
 * Các mối quan hệ:
 * - Many-to-One với Task (cha) — Comment thuộc về một task
 * - Many-to-One với User (tác giả) — Comment được viết bởi một user
 *
 * Các quyết định thiết kế:
 * - Comments thuộc quyền sở hữu của tasks (xóa task sẽ xóa comment)
 * - Mỗi comment phải có tác giả (đảm bảo trách nhiệm)
 * - Mỗi comment phải thuộc về một task (không có comment mồ côi)
 * - Comments có thể được chỉnh sửa (updatedAt theo dõi thay đổi)
 * - Không dùng soft delete (comment bị xóa sẽ xóa thật)
 * - Giới hạn text 2000 ký tự (không cho phép không giới hạn)
 *
 * Vòng đời:
 * - Được tạo khi người dùng thêm comment vào task
 * - Được cập nhật khi người dùng chỉnh sửa comment
 * - Được xóa khi task bị xóa (cascade)
 * - Được xóa khi người dùng chủ động xóa comment
 */
@Entity
@Table(
    name = "comments",
    indexes = {
        @Index(name = "idx_comment_task_id", columnList = "task_id"),
        @Index(name = "idx_comment_author_id", columnList = "author_id"),
        @Index(name = "idx_comment_created_at", columnList = "created_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
// ==================== PRIMARY KEY ====================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

// ==================== CORE FIELDS ====================
/**
 * Comment text — Nội dung comment
 *
 * @NotBlank: Comment không được để trống
 * @Size: Từ 1–2000 ký tự
 * @Column(TEXT): Cho phép comment dài nhưng không phải không giới hạn
 *
 * Vì sao giới hạn 2000 ký tự?
 * - Ngăn việc lạm dụng (viết bài dài)
 * - Khuyến khích giao tiếp ngắn gọn
 * - Tối ưu UI/UX
 * - Nếu cần nội dung dài hơn → dùng mô tả task hoặc tệp đính kèm
 *
 * Ví dụ:
 * - "Task completed ahead of schedule!"
 * - "Blocked by issue #123, waiting for API team"
 * - "@john can you review the design mockups?"
 */
    @NotBlank(message = "Comment text cannot be blank")
    @Size(min = 1, max = 2000, message = "Comment must be between 1 and 2000 characters")
    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

/**
 * Edited flag — Theo dõi comment có bị chỉnh sửa sau khi tạo hay không
 *
 * @Builder.Default: Comment mới mặc định là chưa được chỉnh sửa
 *
 * Lý do — tính minh bạch trong thảo luận:
 * - Người dùng có thể biết comment đã bị thay đổi
 * - Kết hợp với updatedAt để theo dõi lịch sử chỉnh sửa
 * - UI có thể hiển thị nhãn "(edited)"
 */
    @Column(nullable = false)
    @Builder.Default
    private Boolean edited = false;

// ==================== RELATIONSHIPS ====================
/**
 * Quan hệ Many-to-One với Task (Parent)
 *
 * Quan hệ:
 * - Nhiều comment → Một task
 *
 * Ví dụ:
 * - Task "Fix login bug" có Comment 1, Comment 2, Comment 3
 *
 * Ghi chú annotation:
 * @ManyToOne: Nhiều comment cùng thuộc một task
 * @JoinColumn(name="task_id"): Cột foreign key
 * @NotNull: Comment phải thuộc về một task
 *
 * Fetch = LAZY — vì:
 * - Không cần load toàn bộ task khi load comment
 * - Danh sách comment thường được hiển thị khi task đã load trước
 *
 * Cascade = NONE — vì:
 * - Comment không sở hữu task
 * - Xóa comment không được xóa task
 * - Khi xóa task → Task (chủ sở hữu) sẽ cascade để xóa comment
 *
 *
 * Ghi chú thêm:
 * Task entity sẽ có:
 * @OneToMany(mappedBy="task", cascade=CascadeType.ALL, orphanRemoval=true)
 * → Khi task bị xóa, tất cả comments cũng bị xóa theo
 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    @NotNull(message = "Comment must belong to a task")
    private Task task;

/**
 * Quan hệ Many-to-One với User (Author)
 *
 * Quan hệ:
 * - Nhiều comment → Một user (tác giả)
 *
 * Ví dụ:
 * - User "John" viết Comment 1, Comment 5, Comment 8
 *
 * Ghi chú annotation:
 * @ManyToOne: Một user có thể viết nhiều comment
 * @JoinColumn(name="author_id"): Cột foreign key
 * @NotNull: Comment phải có tác giả
 *
 * Fetch = LAZY — vì:
 * - Không phải lúc nào cũng cần thông tin tác giả khi load comment
 * - Chỉ tải khi cần hiển thị tên tác giả
 *
 * Cascade = NONE — vì:
 * - Xóa comment không được xóa user
 * - User tồn tại độc lập và có thể có dữ liệu khác
 *
 * optional=false:
 * - Đảm bảo mỗi comment có người chịu trách nhiệm
 * - Không có comment ẩn danh
 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable =false)
    @NotNull(message = "Comment must have an author")
    private User author;

// ==================== AUDIT FIELDS ====================

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

// ==================== BUSINESS LOGIC METHODS ====================

/**
 * Cập nhật nội dung comment
 *
 * Quy tắc nghiệp vụ:
 * - Text không được để trống
 * - Không vượt quá 2000 ký tự
 * - Đặt edited = true
 * - updatedAt sẽ tự được cập nhật bởi @UpdateTimestamp
 *
 * @param newText nội dung mới của comment
 * @throws IllegalArgumentException nếu text không hợp lệ
 */
    public void updatedText(String newText) {
        if (newText == null || newText.trim().isEmpty()) { 
            throw new IllegalArgumentException("Comment text cannot be blank");
        }
        if (newText.length() > 2000) {
            throw new IllegalArgumentException("Comment cannot exceed 2000 characters");
        }
        this.text = newText;
        this.edited = true;
    }

/**
 * Kiểm tra comment có bị chỉnh sửa hay không
 *
 * Logic:
 * - So sánh updatedAt với createdAt
 * - Nếu updatedAt > createdAt + 1 giây → đã chỉnh sửa
 *
 * @return true nếu comment bị chỉnh sửa sau khi tạo
 *
 * Đây là cách kiểm tra chính xác hơn so với dùng edited flag
 */
    public boolean wasEdited() {
        if (this.createdAt == null || this.updatedAt == null) {
            return false;
        }
        // Xem như đã chỉnh sửa nếu được cập nhật sau thời điểm tạo hơn 1 giây
        return this.updatedAt.isAfter(this.createdAt.plusSeconds(1));
    }

/**
 * Kiểm tra comment có được viết bởi user chỉ định hay không
 *
 * @param user user cần kiểm tra
 * @return true nếu user là tác giả comment
 *
 * Use case:
 * - Kiểm tra quyền chỉnh sửa/xóa comment
 * - UI chỉ hiển thị nút Edit/Delete cho tác giả
 */
    public boolean isAuthoredBy(User user) {
        return user != null && this.author != null && this.author.getId().equals(user.getId());
    }

/**
 * Kiểm tra comment có thuộc về task cụ thể hay không
 *
 * @param taskToCheck task cần kiểm tra
 * @return true nếu comment thuộc task đó
 *
 * Use case:
 * - Đảm bảo comment liên quan đúng task
 * - Tránh hiển thị nhầm comment từ task khác
 */
    public boolean belongsToTask(Task taskToCheck) {
        return taskToCheck != null && this.task != null && this.task.getId().equals(taskToCheck.getId());
    }

/**
 * Lấy thời gian tương đối kể từ khi comment được đăng
 *
 * @return chuỗi mô tả thời gian — ví dụ "2 hours ago"
 *
 * Ghi chú:
 * - Đây là cách hiện thực đơn giản
 * - Trong production có thể dùng PrettyTime hoặc xử lý ở frontend
 */
    public  String getTimeAgo() {
        if (this.createdAt == null) {
            return "unknown";
        }

        long minutesAgo = java.time.Duration.between(this.createdAt, LocalDateTime.now()).toMinutes();
    
        if (minutesAgo < 1) return "just now";
        if (minutesAgo < 60) return minutesAgo + " minutes ago";

        long hoursAgo = minutesAgo / 60;
        if (hoursAgo < 24) return hoursAgo + " hours ago";

        long daysAgo = hoursAgo / 24;
        if (daysAgo < 7) return daysAgo + " days ago";

        long monthsAgo = daysAgo / 30;
        return monthsAgo + " months ago";
    }

/**
 * Lấy đoạn preview của text (rút gọn)
 *
 * @param maxLength độ dài tối đa của preview
 * @return text bị cắt và thêm "..." nếu dài hơn maxLength
 *
 * Use case:
 * - Hiển thị trong danh sách thông báo
 * - Activity feed
 */
    public String getTextPreview(int maxLength) {
        if (this.text == null) {
            return "";
        }
        if (this.text.length() <= maxLength) {
            return this.text;
        }
        return this.text.substring(0, maxLength) + "...";
    }
}
