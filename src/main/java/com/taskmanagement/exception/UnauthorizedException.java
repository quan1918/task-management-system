package com.taskmanagement.exception;

/**
 * Exception thrown when authentication fails
 * 
 * Use cases:
 * - Invalid username or password
 * - Invalid JWT token
 * - Expired token
 * - Missing authentication
 * 
 * HTTP Status: 401 Unauthorized
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

}
    

