package com.taskmanagement.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

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
    public ResponseEntity<Object> handleValidationErrors(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        log.warn("Validation error: {}", ex.getMessage());

        // Trích xuat lỗi theo field
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
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
// ==================== RESOURCE NOT FOUND (404 Not Found) ====================

/**
 * Xử lý UserNotFoundException
 * 
 * Được kích hoạt khi:
 * - Assignee ID không tồn tại
 * 
 * Trả về: 404 Not Found
 */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handeUserNotFound(
            UserNotFoundException ex,
            WebRequest request) {
        
        log.error("User not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("User Not Found")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
/**
 * Xử lý ProjectNotFoundException
 * 
 * Được kích hoạt khi:
 * - Project ID không tồn tại
 * - Project không active (đã archived)
 * 
 * Trả về: 404 Not Found
 */
    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectNotFound(
            ProjectNotFoundException ex,
            WebRequest request) {

        log.error("Project not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Project Not Found")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
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
