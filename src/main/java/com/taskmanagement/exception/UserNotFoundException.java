package com.taskmanagement.exception;

/**
 * UserNotFoundException - Exception khi không tìm thấy user
 * 
 * Ném ra khi:
 * - User ID không tồn tại trong database
 * - Assignee không hợp lệ khi tạo task
 * 
 * HTTP Status: 404 Not Found
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(Long userId) {
        super("User not found with ID: " + userId);
    }
    
}
