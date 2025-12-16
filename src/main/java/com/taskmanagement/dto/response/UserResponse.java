package com.taskmanagement.dto.response;

import com.taskmanagement.entity.User;
import lombok.*;

import java.time.LocalDateTime;

/**
 * UserResponse - DTO dùng để trả dữ liệu user về cho client
 * 
 * Mục đích:
 * - Định nghĩa cấu trúc dữ liệu trả về của API cho các user
 * - Kiểm soát dữ liệu nào được phép gửi cho client
 * - Tránh lỗi lazy loading
 * 
 * Được sử dụng bởi:
 * - GET /api/users/{id}
 * - GET /api/users (danh sách)
 * - POST /api/users (sau khi tạo)
 * - PUT /api/users/{id} (sau khi cập nhật)
 * 
 * Quyết định thiết kế:
 * - Bao gồm thông tin cơ bản của user
 * - Không chứa dữ liệu nhạy cảm (như password hash, roles)
 * - Các timestamp dùng định dạng ISO-8601
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private boolean active;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Chuyển đổi từ entity User sang DTO UserResponse
     * 
     * @param user Entity User
     * @return DTO UserResponse
     */
    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .active(user.getActive())
            .lastLoginAt(user.getLastLoginAt())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
}
