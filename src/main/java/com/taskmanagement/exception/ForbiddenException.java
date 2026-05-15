package com.taskmanagement.exception;

/**
 * Exception thrown when a user tries to access a resource they are not authorized to access.
 * 
 * Use cases:
 * - User tries to access an admin-only endpoint
 * - User tries to modify a resource they do not own
 * 
 * HTTP Status: 403 Forbidden
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }

}
