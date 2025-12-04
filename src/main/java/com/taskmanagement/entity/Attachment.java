package com.taskmanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity Attachment đại diện cho một tệp được đính kèm vào một task
 * 
 * Mục đích:
 * - Lưu metadata của tệp dùng trong tài liệu liên quan đến task
 * - Theo dõi các tệp được tải lên (ảnh chụp màn hình, tài liệu, thiết kế, v.v.)
 * - Cung cấp thông tin tệp để tải xuống/hiển thị
 * - Duy trì dấu vết kiểm toán (audit trail) của các tệp đính kèm
 * 
 * Quan hệ:
 * - Many-to-One với Task (cha) - Attachment thuộc về một task
 * 
 * Các quyết định thiết kế:
 * - Chỉ lưu metadata (không lưu nội dung file trong DB)
 * - File thực tế được lưu trong file system hoặc cloud storage (S3, Azure Blob)
 * - filePath lưu đường dẫn đến tệp thực tế
 * - Mỗi attachment phải thuộc về một task (không được phép orphan)
 * - Xóa theo cascade: Bị xóa khi task bị xóa
 * - Không có updatedAt: File là bất biến (không thể chỉnh sửa file đã upload)
 * - Giới hạn kích thước tệp được áp dụng ở mức ứng dụng
 * - Lưu content type để xử lý đúng MIME type
 * 
 * Chiến lược lưu trữ file:
 * - Môi trường dev: Lưu local (./uploads/attachments/)
 * - Môi trường production: Lưu cloud storage (AWS S3, Azure Blob Storage)
 * - Ví dụ filePath: "attachments/2025/12/task-5/screenshot-123.png"
 * 
 * Vòng đời:
 * - Được tạo khi người dùng upload file vào task
 * - Được đọc khi người dùng xem/tải về file
 * - Bị xóa khi task bị xóa (cascade)
 * - Bị xóa khi người dùng chủ động xóa attachment
 */
@Entity
@Table(
    name = "attachments",
    indexes = {
        @Index(name = "idx_attachment_task", columnList = "task_id"),
        @Index(name = "idx_attachment_created", columnList = "created_at"),
        @Index(name = "idx_attachment_content_type", columnList = "content_type")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Attachment {

// ==================== PRIMARY KEY ====================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;    

// ==================== CORE FIELDS ====================

/**
 * Tên tệp gốc — tên tệp khi người dùng tải lên
 * 
 * @NotBlank: Tên file bắt buộc phải có
 * @Size: 1–255 ký tự (giới hạn của file system)
 * 
 * Lý do: Giữ nguyên tên tệp gốc để người dùng dễ nhận diện
 * Ví dụ:
 * - "design-mockup.png"
 * - "requirements-document.pdf"
 * - "error-screenshot-2025-12-03.jpg"
 * 
 * Ghi chú: Tên tệp thực tế có thể khác (thêm UUID) để tránh trùng lặp
 */
    @NotBlank(message = "Filename is required")
    @Size(min = 1, max = 255, message = "Filename must be between 1 and 255 characters")
    @Column(nullable = false, length = 255)
    private String originalFilename;

/**
 * Tên tệp đã lưu — tên tệp thực tế trong hệ thống lưu trữ
 * 
 * Tại sao phải tách riêng với originalFilename?
 * - Tránh trùng tên file (nhiều file "screenshot.png")
 * - Ngăn chặn lỗ hổng bảo mật (directory traversal)
 * - Thêm UUID để đảm bảo duy nhất
 * 
 * Ví dụ:
 * - originalFilename: "design.png"
 * - storedFilename: "a3f5c9d1-2b4e-4a1c-9f3e-8d7a6c5b4a3f-design.png"
 */
    @NotBlank(message = "Stored filename is required")
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String storedFilename;

/**
 * Đường dẫn file — nơi file được lưu trữ
 * 
 * Ví dụ:
 * - Local: "uploads/attachments/2025/12/task-5/file.png"
 * - S3: "s3://bucket-name/attachments/2025/12/task-5/file.png"
 * - Azure: "https://storageaccount.blob.core.windows.net/attachments/file.png"
 * 
 * Lý do: Cung cấp đường dẫn đầy đủ để lấy file
 * - Dễ dàng truy xuất và tải xuống
 * - Hỗ trợ đa dạng backend lưu trữ
 * - Có thể di chuyển file giữa các hệ thống lưu trữ
 */
    @NotBlank(message = "File path is required")
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String filePath;

/**
 * Content type (MIME type) — loại tệp
 * 
 * @NotBlank: Bắt buộc phải có để xử lý file đúng cách
 * 
 * Ví dụ thường gặp:
 * - Ảnh: "image/png", "image/jpeg", "image/gif"
 * - Tài liệu: "application/pdf", "application/msword"
 * - Text: "text/plain", "text/csv"
 * - Nén: "application/zip", "application/x-rar"
 * 
 * Lý do quan trọng:
 * - Trình duyệt cần biết để hiển thị hoặc tải về đúng cách
 * - Xác định được icon phù hợp trên frontend
 * - Hỗ trợ validate loại file được phép tải lên
 */
    @NotBlank(message = "Content type is required")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String contentType;

/**
 * Kích thước tệp tính bằng byte
 * 
 * @NotNull: Bắt buộc phải biết kích thước tệp
 * @Min(1): Không cho phép tệp rỗng (0 bytes)
 * @Max(52428800): Giới hạn 50 MB
 * 
 * Tại sao giới hạn kích thước?
 * - Tránh abuse tài nguyên lưu trữ
 * - Kiểm soát chi phí lưu trữ
 * - Kích thước hợp lý cho file đi kèm task
 * - File lớn nên dùng dịch vụ chuyên dụng
 */
    @NotNull(message = "File size is required")
    @Min(value = 1, message = "File size must be at least 1 byte")
    @Max(value = 52428800, message = "File size cannot exceed 50 MB")
    @Column(nullable = false)
    private Long fileSize;

/**
 * Mô tả file — ghi chú tùy chọn về file
 * 
 * Lý do là optional: Việc upload nhanh không cần mô tả
 * Ví dụ:
 * - "Bản thiết kế cập nhật theo feedback khách hàng"
 * - "Ảnh lỗi từ môi trường production"
 * - "Tài liệu yêu cầu phiên bản 2.1"
 */
    @Size(max = 500)
    @Column(length = 500)
    private String description;

/**
 * ID người tải file
 * 
 * Tại sao lưu ID người upload?
 * - Theo dõi hành động upload (trách nhiệm)
 * - Dấu vết kiểm toán (audit)
 * - Kiểm tra quyền (user có được phép xóa không?)
 * 
 * Ghi chú: Không dùng quan hệ khóa ngoại để giữ đơn giản
 * - Không cần tải User entity khi tải attachments
 * - Tăng hiệu năng
 * - Có thể enrich ở service layer nếu cần
 */
    @NotNull(message = "Uploader ID is required")
    @Column(nullable = false)
    private Long uploadedBy;

// ==================== RELATIONSHIPS ====================
/**
 * Quan hệ Many-to-One với Task (cha)
 * 
 * Quan hệ: Nhiều attachments → Một task
 * Ví dụ: Task "Thiết kế homepage" có file1.png, file2.pdf
 * 
 * @ManyToOne: Nhiều file thuộc cùng một task
 * @JoinColumn(name="task_id"): Cột khóa ngoại
 * @NotNull: Mỗi file phải thuộc về một task
 * 
 * Fetch: LAZY
 * Lý do:
 * - Không cần load task đầy đủ khi load attachment
 * - Danh sách attachment thường lấy từ task đã load sẵn
 * 
 * Cascade: NONE
 * Lý do: Attachment không sở hữu task
 * - Xóa attachment KHÔNG xóa task
 * - Task mới là cha và chịu trách nhiệm cascade xóa
 * 
 * NOTE trong Task:
 * @OneToMany(mappedBy="task", cascade=CascadeType.ALL, orphanRemoval=true)
 * Điều này nghĩa là: Khi task bị xóa, tất cả attachment bị xóa theo
 * (cả metadata lẫn file thực tế trong storage)
 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    @NotNull(message = "Attachment muset belong to a task")
    private Task task;

// ==================== AUDIT FIELDS ====================

/**
 * Thời điểm upload — khi file được tải lên
 * 
 * @CreationTimestamp: Hibernate tự set khi INSERT
 * @Column(updatable=false): Không thể thay đổi sau khi tạo
 * 
 * Lý do:
 * - Sắp xếp theo thời điểm upload
 * - Hiển thị "uploaded 2 days ago"
 * - Thu thập audit history
 * 
 * Ghi chú: Không có updatedAt
 * Vì sao? File là bất biến — muốn cập nhật thì phải xóa và upload file mới
 */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

// ==================== BUSINESS LOGIC METHODS ====================

/**
 * Lấy phần mở rộng của file từ tên gốc
 * 
 * @return extension (ví dụ: "png", "pdf", "docx")
 * Trả chuỗi rỗng nếu không có extension
 * 
 * Dùng để: hiển thị icon file phù hợp trong UI
 */
    public String getFileExtension() {
        if (this.originalFilename != null || !this.originalFilename.contains(".")) {
            return "";
        }
        int lastDotIndex = this.originalFilename.lastIndexOf('.');
        return this.originalFilename.substring(lastDotIndex + 1).toLowerCase();
    }

/**
 * Lấy kích thước file dạng dễ đọc
 * 
 * @return ví dụ: "1.5 MB", "320 KB", "45 bytes"
 * 
 * Dùng để: hiển thị size trên UI
 */
    public String getFormattedFileSize() {
        if (this.fileSize == null) {
            return "0 bytes";
        }
        long size = this.fileSize;

        if (size < 1024) {
            return size + " bytes";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }

/**
 * Kiểm tra file có phải là ảnh không
 * 
 * @return true nếu content type bắt đầu với image/*
 * 
 * Dùng để: quyết định xem file có thể xem trước (preview) hay không
 */
    public boolean isImage() {
        return this.contentType != null && this.contentType.startsWith("image/");
    }

/**
 * Kiểm tra file có phải tài liệu không
 * 
 * @return true nếu là PDF, Word, Excel, v.v.
 */
    public boolean isDocument() {
        if (this.contentType == null) {
            return false;
        }
        return this.contentType.equals("application/pdf") ||
               this.contentType.equals("application/msword") ||
               this.contentType.equals("application/vnd.openxmlformarts-officedocument.wordprocessingml.document") ||
               this.contentType.equals("application/vnd.ms-excel")||
               this.contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

/**
 * Kiểm tra file có thuộc về một task cụ thể không
 * 
 * @param taskToCheck task cần kiểm tra
 * @return true nếu attachment thuộc task đó
 * 
 * Dùng khi: validate trước khi tải file
 */
    public boolean belongsToTask(Task taskToCheck) {
        return taskToCheck != null && this.task != null && this.task.getId().equals(taskToCheck.getId());
    }

/**
 * Kiểm tra file có được upload bởi user cụ thể không
 * 
 * @param userId ID user cần kiểm tra
 * @return true nếu user đó là người upload
 * 
 * Dùng cho: kiểm tra quyền xóa file
 */
    public boolean wasUpLoadedBy(Long userId) {
        return userId != null && this.uploadedBy != null && this.uploadedBy.equals(userId);
    }

/**
 * Kiểm tra file có vượt quá giới hạn kích thước không
 * 
 * @param maxSizeInMB kích thước tối đa tính theo MB
 * @return true nếu file vượt quá giới hạn
 * 
 * Dùng để: validate trước khi upload
 */
    public boolean exceedsSizeLimit(int maxSizeInMB) {
        long maxSizeInBytes = maxSizeInMB * 1024L * 1024L;
        return this.fileSize != null && this.fileSize > maxSizeInBytes;
    }

/**
 * Lấy tên file an toàn để download
 * 
 * @return tên file đã được làm sạch ký tự nguy hiểm
 * 
 * Dùng cho: Content-Disposition header
 */
    public String getDownloadFilename() {
        if (this.originalFilename == null) {
            return "attachment";
        }
        // Xoá ký tự nguy hiểm
        return this.originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

/**
 * Kiểm tra content type có nằm trong danh sách được phép hay không
 * 
 * @param allowedTypes tập các MIME types được cho phép
 * @return true nếu contentType hợp lệ
 * 
 * Dùng cho: validate loại file khi upload
 */
    public boolean isContentTypeAllowed(java.util.Set<String> allowedTypes) {
        return this.contentType != null && allowedTypes.contains(this.contentType);
    }
}
