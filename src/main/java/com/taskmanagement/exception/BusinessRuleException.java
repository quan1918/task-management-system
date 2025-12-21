package com.taskmanagement.exception;

/**
 * BusinessRuleException - Exception khi vi phạm business rules
 * 
 * Use Cases:
 * - Owner không active
 * - EndDate trước startDate
 * - Update project đã archived
 * - Assign task cho user deleted
 * 
 * HTTP Status: 400 Bad Request
 */
public class BusinessRuleException extends RuntimeException {
    
    public BusinessRuleException(String message) {
        super(message);
    }
}
