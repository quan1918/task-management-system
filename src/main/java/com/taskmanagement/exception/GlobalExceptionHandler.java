package com.taskmanagement.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.taskmanagement.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler - Xử lý exception tập trung
 * 
 * Chịu trách nhiệm:
 * - Bắt các exception xuất phát từ controllers
 * - Chuyển đổi exception thành HTTP response
 * - Format thông điệp lỗi một cách thống nhất
 * - Ghi log lỗi phục vụ debugging
 * 
 * @RestControllerAdvice:
 * - Áp dụng cho tất cả các @RestController
 * - Xử lý lỗi tập trung
 * - Tự động trả về JSON response
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

// ==================== VALIDATION ERRORS (400 Bad Request) ====================

/**
 * Xử lý lỗi validation từ annotation @Valid
 * 
 * Được kích hoạt khi:
 * - @NotBlank không đạt (title rỗng)
 * - @Size không đạt (title quá ngắn/dài)
 * - @FutureOrPresent không đạt (dueDate trong quá khứ)
 * - Bất kỳ constraint nào của Bean Validation bị vi phạm
 * 
 * Trả về: 400 Bad Request với danh sách field errors
 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        log.warn("Validation error: {}", ex.getMessage());

        // Trích xuat lỗi theo field
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String errorMessage = error.getDefaultMessage();

            if (error instanceof FieldError fieldError) {
                fieldErrors.put(fieldError.getField(), errorMessage);
            } else {
                // Object-level error
                fieldErrors.put("_object", errorMessage);
            }
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Input validation failed. Check 'errors' field for details.")
            .path(request.getDescription(false).replace("uri=", ""))
            .errors(fieldErrors)
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleException ex,
            WebRequest request) {

        log.warn("Business rule violation: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Business Rule Violation")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
// ==================== UNAUTHORIZED (401 Unauthorized) ====================

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex,
            WebRequest request) {

        log.warn("Unauthorized access: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Unauthorized")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

// ==================== FORBIDDEN (403 Forbidden) ====================

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex,
            WebRequest request) {

        log.warn("Forbidden access: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Forbidden")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

// ==================== RESOURCE NOT FOUND (404 Not Found) ====================

    @ExceptionHandler({
        UserNotFoundException.class,
        ProjectNotFoundException.class,
        TaskNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(
            RuntimeException ex,
            WebRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Resource Not Found")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

// ==================== DUPLICATE RESOURCE (409 Conflict) ====================

/**
 * Xử lý DuplicateResourceException
 * 
 * Được kích hoạt khi:
 * - Username đã tồn tại khi tạo user
 * - Email đã tồn tại khi tạo/update user
 * 
 * Trả về: 409 Conflict
 * 
 * Example Response:
 * {
 *   "timestamp": "2025-12-20T10:30:00",
 *   "status": 409,
 *   "error": "Duplicate Resource",
 *   "message": "Username already exists: admin",
 *   "path": "/api/users"
 * }
 */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex,
            WebRequest request) {

        log.warn("Duplicate resource: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Duplicate Resource")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
// ==================== GENERIC ERRORS (500 Internal Server Error) ====================

/**
 * Xử lý tất cả các lỗi chung khác
 * 
 * Được kích hoạt khi:
 * - Các lỗi ngoài dự kiến xảy ra
 * - Kết nối database gặp sự cố
 * - Bất kỳ exception nào không được xử lý riêng
 * 
 * Trả về: 500 Internal Server Error
 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(
            Exception ex,
            WebRequest request) {

        log.error("Unexpected error occurred", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred. Please try again later.")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
