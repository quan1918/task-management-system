package com.taskmanagement.exception;

/**
 * ProjectNotFoundException - Exception khi không tìm thấy project
 * 
 * Ném ra khi:
 * - Project ID không tồn tại
 * - Project không active (archived)
 * 
 * HTTP Status: 404 Not Found
 */
public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(String message) {
        super(message);
    }

    public ProjectNotFoundException(Long projectId) {
        super("Active project not found with ID:" + projectId);
    }
} 
    

