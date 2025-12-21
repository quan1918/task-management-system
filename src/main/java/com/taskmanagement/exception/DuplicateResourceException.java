package com.taskmanagement.exception;

/**
 * DuplicateResourceException - Exception khi tài nguyên đã tồn tại
 * 
 * Use Cases:
 * - Username đã tồn tại khi tạo user
 * - Email đã tồn tại khi tạo/update user
 * - Project name đã tồn tại (nếu có unique constraint)
 * 
 * HTTP Status: 409 Conflict
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
