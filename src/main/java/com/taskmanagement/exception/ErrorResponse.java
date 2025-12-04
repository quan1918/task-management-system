package com.taskmanagement.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ErrorResponse - Định dạng lỗi chuẩn hóa
 * 
 * Mục đích:
 * - Tạo cấu trúc lỗi thống nhất cho tất cả API
 * - Cung cấp thông tin lỗi chi tiết
 * - Giúp client xử lý lỗi đúng cách
 * 
 * Ví dụ JSON:
 * {
 *   "timestamp": "2025-12-04T10:30:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "User not found with ID: 5",
 *   "path": "/api/tasks"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Chỉ bao gồm các trường không null trong JSON
public class ErrorResponse {
    
/**
 * Thời điểm lỗi xảy ra
 */
    private LocalDateTime timestamp;

/**
 * Mã trạng thái HTTP (ví dụ: 404, 500)
 */
    private int status;

/**
 * Mô tả ngắn về lỗi (ví dụ: "Not Found")
 */
    private String error;

/**
 * Thông điệp lỗi chi tiết
 */
    private String message;

/**
 * Đường dẫn của request gây ra lỗi
 * Ví dụ: "/api/tasks"
 */
    private String path;

/** 
 * Các lỗi theo từng field (dùng cho validation)
 * Ví dụ: { "title": "must not be blank", "dueDate": "must be in future" }
*/
    private Map<String, String> errors; 
}
