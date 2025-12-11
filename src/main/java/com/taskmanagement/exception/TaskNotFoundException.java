package com.taskmanagement.exception;

/**
 * TaskNotFoundException - Exception khi không tìm thấy task
 * 
 * Ném ra khi:
 * - Task ID không tồn tại trong database
 * - User request task không có quyền truy cập
 * 
 * HTTP Status: 404 Not Found
 * 
 * @author Task Management System
 * @version 1.0
 */
public class TaskNotFoundException extends RuntimeException{

    public TaskNotFoundException(String message) {
        super(message);
    }

    public TaskNotFoundException(Long taskId){
        super("Task not found with ID: " + taskId);
    }

    public TaskNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
