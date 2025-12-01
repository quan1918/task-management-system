# Exception Layer (Error Handling)

##  Overview

The **Exception layer** defines custom exceptions and implements centralized error handling for the application. It provides a consistent, predictable way to communicate errors to clients while maintaining security and proper HTTP status code mapping.

**Location:** \src/main/java/com/taskmanagement/exception/\

**Responsibility:** Define domain-specific exceptions, implement global exception handling, and provide meaningful error responses

---

##  Core Responsibilities

### 1. Domain Exception Definition
- Define custom exceptions for domain-specific errors
- Create exception hierarchy with meaningful names
- Include error codes for programmatic handling
- Provide detailed error messages for debugging

### 2. Global Exception Handling
- Catch exceptions at application level using @RestControllerAdvice
- Convert exceptions to appropriate HTTP status codes
- Provide consistent error response format
- Handle both checked and unchecked exceptions

### 3. Error Response Formatting
- Structure error responses consistently across API
- Include error code, message, and timestamp
- Support validation error details
- Provide request path and correlation IDs

### 4. HTTP Status Mapping
- Map domain exceptions to correct HTTP status codes
- Use standard HTTP semantics (400, 404, 409, 500)
- Distinguish between client and server errors
- Support custom status codes when needed

### 5. Error Logging and Monitoring
- Log exceptions with appropriate levels (WARN, ERROR)
- Capture stack traces for debugging
- Support error tracking and monitoring systems
- Hide sensitive information from client responses

---

##  Folder Structure

\\\
exception/
 base/
    ApplicationException.java           # Base exception class
    BusinessException.java              # Business logic errors
    BadRequestException.java            # Invalid input errors
    ResourceNotFoundException.java      # Resource not found errors
    ConflictException.java              # Conflict/duplicate errors
    UnauthorizedException.java          # Authentication errors
    ForbiddenException.java             # Authorization errors
    InternalServerException.java        # System errors

 task/
    TaskNotFoundException.java          # Task not found
    InvalidTaskStatusException.java     # Invalid task status
    TaskAlreadyCompletedException.java # Completed task operations
    DuplicateTaskException.java         # Duplicate task

 user/
    UserNotFoundException.java          # User not found
    InvalidCredentialsException.java    # Login failed
    DuplicateEmailException.java        # Duplicate email
    UserAlreadyExistsException.java     # User already exists

 project/
    ProjectNotFoundException.java       # Project not found
    InvalidProjectStateException.java   # Invalid project state
    UserNotInProjectException.java      # User not in project

 validation/
    ValidationException.java            # Input validation errors
    ConstraintViolationException.java   # Bean validation violations
    InvalidInputException.java          # Invalid input format

 handler/
    GlobalExceptionHandler.java         # Central exception handler
    ValidationExceptionHandler.java     # Validation error handler
    SecurityExceptionHandler.java       # Security error handler

 response/
    ErrorResponse.java                  # Standard error response
    FieldError.java                     # Field-level error details
    ValidationErrorResponse.java        # Validation error response
    ErrorCode.java                      # Error code enumeration

 README.md                              # This file
\\\

---

##  Exception Hierarchy

\\\
Throwable
 Exception
    ApplicationException (custom base)
       BusinessException
          TaskNotFoundException
          UserNotFoundException
          ProjectNotFoundException
          ...
       BadRequestException
          InvalidInputException
          ValidationException
       ConflictException
          DuplicateEmailException
          DuplicateTaskException
       UnauthorizedException
          InvalidCredentialsException
       ForbiddenException
    Spring exceptions (handled by GlobalExceptionHandler)
\\\

---

##  Base Exception Classes

### ApplicationException Base Class

\\\java
/**
 * Base exception class for all application exceptions
 * Provides common functionality for all custom exceptions
 */
@Getter
public abstract class ApplicationException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Map<String, Object> details;
    
    /**
     * Constructor with error code and HTTP status
     */
    public ApplicationException(
        String message,
        String errorCode,
        HttpStatus httpStatus
    ) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = new HashMap<>();
    }
    
    /**
     * Constructor with error code, HTTP status, and cause
     */
    public ApplicationException(
        String message,
        String errorCode,
        HttpStatus httpStatus,
        Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = new HashMap<>();
    }
    
    /**
     * Constructor with error code, HTTP status, and additional details
     */
    public ApplicationException(
        String message,
        String errorCode,
        HttpStatus httpStatus,
        Map<String, Object> details
    ) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = details != null ? details : new HashMap<>();
    }
    
    /**
     * Add detail information to exception
     */
    public ApplicationException withDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
}
\\\

### BusinessException

\\\java
/**
 * Exception for business logic violations
 * Represents errors in domain rules or business constraints
 * 
 * HTTP Status: 400 Bad Request or 409 Conflict (depending on scenario)
 * 
 * Examples:
 * - Task cannot be assigned to inactive user
 * - Task cannot be completed if already cancelled
 * - Project cannot be deleted if it has active tasks
 */
@Getter
public class BusinessException extends ApplicationException {
    
    public BusinessException(String message) {
        super(message, "BUSINESS_ERROR", HttpStatus.BAD_REQUEST);
    }
    
    public BusinessException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.BAD_REQUEST);
    }
    
    public BusinessException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, HttpStatus.BAD_REQUEST, cause);
    }
}
\\\

### ResourceNotFoundException

\\\java
/**
 * Exception thrown when a requested resource is not found
 * 
 * HTTP Status: 404 Not Found
 * 
 * Examples:
 * - GET /api/tasks/999 (task doesn't exist)
 * - GET /api/users/999 (user doesn't exist)
 * - GET /api/projects/999 (project doesn't exist)
 */
@Getter
public class ResourceNotFoundException extends ApplicationException {
    
    private final String resourceType;
    private final String resourceId;
    
    public ResourceNotFoundException(String resourceType, Long id) {
        super(
            String.format("%s with ID %d not found", resourceType, id),
            "RESOURCE_NOT_FOUND",
            HttpStatus.NOT_FOUND
        );
        this.resourceType = resourceType;
        this.resourceId = id.toString();
    }
    
    public ResourceNotFoundException(String resourceType, String id) {
        super(
            String.format("%s with ID %s not found", resourceType, id),
            "RESOURCE_NOT_FOUND",
            HttpStatus.NOT_FOUND
        );
        this.resourceType = resourceType;
        this.resourceId = id;
    }
    
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
        this.resourceType = null;
        this.resourceId = null;
    }
}
\\\

### ConflictException

\\\java
/**
 * Exception thrown when request conflicts with current state
 * 
 * HTTP Status: 409 Conflict
 * 
 * Examples:
 * - Duplicate email already exists
 * - Task is already completed
 * - Project name already exists in organization
 * - Concurrent modification detected (optimistic locking)
 */
@Getter
public class ConflictException extends ApplicationException {
    
    private final String conflictField;
    private final Object conflictValue;
    
    public ConflictException(String message) {
        super(message, "CONFLICT", HttpStatus.CONFLICT);
        this.conflictField = null;
        this.conflictValue = null;
    }
    
    public ConflictException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.CONFLICT);
        this.conflictField = null;
        this.conflictValue = null;
    }
    
    public ConflictException(
        String message,
        String conflictField,
        Object conflictValue
    ) {
        super(message, "CONFLICT", HttpStatus.CONFLICT);
        this.conflictField = conflictField;
        this.conflictValue = conflictValue;
    }
}
\\\

### BadRequestException

\\\java
/**
 * Exception thrown when request is malformed or invalid
 * 
 * HTTP Status: 400 Bad Request
 * 
 * Examples:
 * - Missing required fields
 * - Invalid field format
 * - Invalid query parameters
 * - Invalid request body
 */
@Getter
public class BadRequestException extends ApplicationException {
    
    public BadRequestException(String message) {
        super(message, "BAD_REQUEST", HttpStatus.BAD_REQUEST);
    }
    
    public BadRequestException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.BAD_REQUEST);
    }
    
    public BadRequestException(String message, Throwable cause) {
        super(message, "BAD_REQUEST", HttpStatus.BAD_REQUEST, cause);
    }
}
\\\

### UnauthorizedException

\\\java
/**
 * Exception thrown when authentication fails
 * 
 * HTTP Status: 401 Unauthorized
 * 
 * Examples:
 * - Invalid credentials
 * - Token expired
 * - Token invalid
 * - Missing authentication
 */
@Getter
public class UnauthorizedException extends ApplicationException {
    
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }
    
    public UnauthorizedException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.UNAUTHORIZED);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED, cause);
    }
}
\\\

### ForbiddenException

\\\java
/**
 * Exception thrown when user lacks permission
 * 
 * HTTP Status: 403 Forbidden
 * 
 * Examples:
 * - User doesn't have required role
 * - User cannot access this project
 * - User cannot edit this task
 * - Admin-only operation
 */
@Getter
public class ForbiddenException extends ApplicationException {
    
    private final String requiredPermission;
    
    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
        this.requiredPermission = null;
    }
    
    public ForbiddenException(String message, String requiredPermission) {
        super(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
        this.requiredPermission = requiredPermission;
    }
}
\\\

---

##  Domain-Specific Exceptions

### Task Exceptions

\\\java
/**
 * Thrown when task is not found by ID
 */
public class TaskNotFoundException extends ResourceNotFoundException {
    
    public TaskNotFoundException(Long taskId) {
        super("Task", taskId);
    }
    
    public TaskNotFoundException(Long taskId, String message) {
        super(message);
    }
}

/**
 * Thrown when task status change is invalid
 * Example: Cannot complete a cancelled task
 */
public class InvalidTaskStatusException extends BusinessException {
    
    private final String currentStatus;
    private final String attemptedStatus;
    
    public InvalidTaskStatusException(
        String currentStatus,
        String attemptedStatus,
        String reason
    ) {
        super(
            String.format(
                "Cannot change task status from %s to %s: %s",
                currentStatus,
                attemptedStatus,
                reason
            ),
            "INVALID_TASK_STATUS"
        );
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
    }
}

/**
 * Thrown when attempting operation on completed task
 */
public class TaskAlreadyCompletedException extends ConflictException {
    
    public TaskAlreadyCompletedException(Long taskId) {
        super(
            String.format("Task %d is already completed and cannot be modified", taskId),
            "TASK_ALREADY_COMPLETED"
        );
    }
}

/**
 * Thrown when trying to create duplicate task
 */
public class DuplicateTaskException extends ConflictException {
    
    public DuplicateTaskException(String title, Long projectId) {
        super(
            String.format("Task with title '%s' already exists in project %d", title, projectId),
            "title",
            title
        );
    }
}
\\\

### User Exceptions

\\\java
/**
 * Thrown when user is not found by ID
 */
public class UserNotFoundException extends ResourceNotFoundException {
    
    public UserNotFoundException(Long userId) {
        super("User", userId);
    }
}

/**
 * Thrown when user already exists (duplicate)
 */
public class UserAlreadyExistsException extends ConflictException {
    
    public UserAlreadyExistsException(String username) {
        super(
            String.format("User with username '%s' already exists", username),
            "username",
            username
        );
    }
}

/**
 * Thrown when email is already registered
 */
public class DuplicateEmailException extends ConflictException {
    
    public DuplicateEmailException(String email) {
        super(
            String.format("Email '%s' is already registered", email),
            "email",
            email
        );
    }
}

/**
 * Thrown when login credentials are invalid
 */
public class InvalidCredentialsException extends UnauthorizedException {
    
    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
    
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
\\\

### Project Exceptions

\\\java
/**
 * Thrown when project is not found by ID
 */
public class ProjectNotFoundException extends ResourceNotFoundException {
    
    public ProjectNotFoundException(Long projectId) {
        super("Project", projectId);
    }
}

/**
 * Thrown when project is in invalid state for operation
 */
public class InvalidProjectStateException extends BusinessException {
    
    public InvalidProjectStateException(String message) {
        super(message, "INVALID_PROJECT_STATE");
    }
}

/**
 * Thrown when user is not a member of project
 */
public class UserNotInProjectException extends ForbiddenException {
    
    public UserNotInProjectException(Long userId, Long projectId) {
        super(
            String.format("User %d is not a member of project %d", userId, projectId),
            "PROJECT_MEMBERSHIP"
        );
    }
}
\\\

### Validation Exceptions

\\\java
/**
 * Thrown when input validation fails
 */
public class ValidationException extends BadRequestException {
    
    private final List<FieldError> fieldErrors;
    
    public ValidationException(String message, List<FieldError> fieldErrors) {
        super(message, "VALIDATION_ERROR");
        this.fieldErrors = fieldErrors;
    }
    
    public ValidationException(List<FieldError> fieldErrors) {
        super("Validation failed", "VALIDATION_ERROR");
        this.fieldErrors = fieldErrors;
    }
}

/**
 * Field-level error details
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FieldError {
    private String field;
    private String message;
    private Object rejectedValue;
    private String code;
}
\\\

---

##  Error Response Classes

### Standard Error Response

\\\java
/**
 * Standard error response format for all API errors
 * Returned for 4xx and 5xx status codes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * Error code for programmatic handling
     * Example: "TASK_NOT_FOUND", "INVALID_INPUT", "UNAUTHORIZED"
     */
    private String code;
    
    /**
     * User-friendly error message
     */
    private String message;
    
    /**
     * HTTP status code
     */
    private int status;
    
    /**
     * Timestamp when error occurred
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime timestamp;
    
    /**
     * Request path that caused error
     */
    private String path;
    
    /**
     * Unique correlation ID for tracking
     */
    private String correlationId;
    
    /**
     * Additional error details (optional)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> details;
    
    /**
     * Field-level validation errors (optional)
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<FieldError> fieldErrors;
    
    /**
     * Stack trace (only in development)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String stackTrace;
    
    /**
     * Create error response from exception
     */
    public static ErrorResponse of(
        ApplicationException ex,
        int status,
        String path,
        String correlationId
    ) {
        return ErrorResponse.builder()
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .status(status)
            .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
            .path(path)
            .correlationId(correlationId)
            .details(ex.getDetails().isEmpty() ? null : ex.getDetails())
            .build();
    }
}
\\\

### Validation Error Response

\\\java
/**
 * Error response with field-level validation errors
 * Extends standard error response with field details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationErrorResponse {
    
    private String code;
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private String path;
    private String correlationId;
    private List<FieldError> fieldErrors;
    
    /**
     * Create from validation exception
     */
    public static ValidationErrorResponse of(
        ValidationException ex,
        int status,
        String path,
        String correlationId
    ) {
        return ValidationErrorResponse.builder()
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .status(status)
            .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
            .path(path)
            .correlationId(correlationId)
            .fieldErrors(ex.getFieldErrors())
            .build();
    }
}
\\\

---

##  Global Exception Handler

### RestControllerAdvice

\\\java
/**
 * Global exception handler for all REST endpoints
 * Catches exceptions and converts them to proper HTTP responses
 * 
 * Handles:
 * - Custom ApplicationException and subclasses
 * - Spring validation exceptions
 * - Spring security exceptions
 * - Unhandled runtime exceptions
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Extract correlation ID from request
     */
    private String getCorrelationId(HttpServletRequest request) {
        return request.getHeader("X-Correlation-ID") != null
            ? request.getHeader("X-Correlation-ID")
            : UUID.randomUUID().toString();
    }
    
    /**
     * Handle ApplicationException and all subclasses
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(
        ApplicationException ex,
        HttpServletRequest request
    ) {
        log.warn("Application exception: {}", ex.getErrorCode(), ex);
        
        String correlationId = getCorrelationId(request);
        ErrorResponse response = ErrorResponse.of(
            ex,
            ex.getHttpStatus().value(),
            request.getRequestURI(),
            correlationId
        );
        
        return new ResponseEntity<>(response, ex.getHttpStatus());
    }
    
    /**
     * Handle Spring's MethodArgumentNotValidException (validation errors)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        log.warn("Validation exception: {}", request.getRequestURI());
        
        List<FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> FieldError.builder()
                .field(error.getField())
                .message(error.getDefaultMessage())
                .rejectedValue(error.getRejectedValue())
                .code(error.getCode())
                .build())
            .collect(Collectors.toList());
        
        String correlationId = getCorrelationId(request);
        ValidationErrorResponse response = ValidationErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("Validation failed")
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
            .path(request.getRequestURI())
            .correlationId(correlationId)
            .fieldErrors(fieldErrors)
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle Spring's ConstraintViolationException (JPA validation)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
        ConstraintViolationException ex,
        HttpServletRequest request
    ) {
        log.warn("Constraint violation: {}", request.getRequestURI());
        
        List<FieldError> fieldErrors = ex.getConstraintViolations()
            .stream()
            .map(violation -> FieldError.builder()
                .field(violation.getPropertyPath().toString())
                .message(violation.getMessage())
                .rejectedValue(violation.getInvalidValue())
                .build())
            .collect(Collectors.toList());
        
        String correlationId = getCorrelationId(request);
        ValidationErrorResponse response = ValidationErrorResponse.builder()
            .code("CONSTRAINT_VIOLATION")
            .message("Data validation failed")
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
            .path(request.getRequestURI())
            .correlationId(correlationId)
            .fieldErrors(fieldErrors)
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle HTTP request method not allowed
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
        HttpRequestMethodNotSupportedException ex,
        HttpServletRequest request
    ) {
        log.warn("Method not allowed: {} {}", ex.getMethod(), request.getRequestURI());
        
        String correlationId = getCorrelationId(request);
        ErrorResponse response = ErrorResponse.builder()
            .code("METHOD_NOT_ALLOWED")
            .message("HTTP method not supported for this endpoint")
            .status(HttpStatus.METHOD_NOT_ALLOWED.value())
            .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
            .path(request.getRequestURI())
            .correlationId(correlationId)
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }
    
    /**
     * Handle unhandled RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
        RuntimeException ex,
        HttpServletRequest request
    ) {
        log.error("Unhandled runtime exception", ex);
        
        String correlationId = getCorrelationId(request);
        ErrorResponse response = ErrorResponse.builder()
            .code("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred")
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
            .path(request.getRequestURI())
            .correlationId(correlationId)
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error("Unhandled exception", ex);
        
        String correlationId = getCorrelationId(request);
        ErrorResponse response = ErrorResponse.builder()
            .code("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred")
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
            .path(request.getRequestURI())
            .correlationId(correlationId)
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
\\\

---

##  Using Custom Exceptions

### In Services

\\\java
/**
 * Examples of throwing and handling custom exceptions in service layer
 */
@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    
    public Task getTaskById(Long taskId) {
        return taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
    }
    
    @Transactional
    public Task updateTaskStatus(Long taskId, TaskStatus newStatus) {
        Task task = getTaskById(taskId);
        
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new TaskAlreadyCompletedException(taskId);
        }
        
        if (!task.canTransitionTo(newStatus)) {
            throw new InvalidTaskStatusException(
                task.getStatus().name(),
                newStatus.name(),
                "Invalid status transition"
            );
        }
        
        task.setStatus(newStatus);
        return taskRepository.save(task);
    }
    
    @Transactional
    public Task assignTask(Long taskId, Long assigneeId) {
        Task task = getTaskById(taskId);
        User assignee = userRepository.findById(assigneeId)
            .orElseThrow(() -> new UserNotFoundException(assigneeId));
        
        if (!assignee.isActive()) {
            throw new BusinessException(
                "Cannot assign task to inactive user",
                "INACTIVE_USER"
            );
        }
        
        task.setAssignee(assignee);
        return taskRepository.save(task);
    }
}
\\\

### In Controllers

\\\java
/**
 * Examples of exception handling patterns in controllers
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    
    private final TaskService taskService;
    
    /**
     * GET endpoint - may throw TaskNotFoundException (404)
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable Long id) {
        Task task = taskService.getTaskById(id);  // Throws TaskNotFoundException if not found
        return ResponseEntity.ok(TaskResponse.from(task));
    }
    
    /**
     * POST endpoint - may throw ValidationException (400) or DuplicateTaskException (409)
     */
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        // @Valid annotation handles validation, throws MethodArgumentNotValidException if invalid
        
        Task task = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(TaskResponse.from(task));
    }
    
    /**
     * PUT endpoint - may throw multiple exceptions
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        try {
            Task task = taskService.updateTaskStatus(id, TaskStatus.valueOf(request.getStatus()));
            return ResponseEntity.ok(TaskResponse.from(task));
        } catch (IllegalArgumentException e) {
            // Thrown by TaskStatus.valueOf() for invalid enum value
            throw new BadRequestException("Invalid status value: " + request.getStatus());
        }
    }
}
\\\

---

##  Exception Best Practices

### 1. Use Specific Exception Types

\\\java
// GOOD: Specific exception with clear meaning
@GetMapping("/{id}")
public TaskResponse getTask(@PathVariable Long id) {
    return TaskResponse.from(
        taskRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id))
    );
}

// BAD: Generic exception
@GetMapping("/{id}")
public TaskResponse getTask(@PathVariable Long id) {
    Task task = taskRepository.findById(id).orElse(null);
    if (task == null) {
        throw new RuntimeException("Task not found");  // Too generic
    }
    return TaskResponse.from(task);
}
\\\

### 2. Include Context in Exception Messages

\\\java
// GOOD: Message includes details
throw new ConflictException(
    String.format("Email '%s' is already registered", email),
    "email",
    email
);

// BAD: Vague message
throw new ConflictException("Email already exists");
\\\

### 3. Don't Expose Sensitive Information

\\\java
// GOOD: Generic error message to client
public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    log.error("Database error details: {}", ex.getMessage());  // Log with details
    
    return ResponseEntity.status(500)
        .body(ErrorResponse.builder()
            .message("An unexpected error occurred")  // Generic for client
            .code("INTERNAL_SERVER_ERROR")
            .build());
}

// BAD: Exposing internal details
public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    return ResponseEntity.status(500)
        .body(ErrorResponse.builder()
            .message(ex.getMessage())  // Exposes database errors, stack traces
            .build());
}
\\\

### 4. Use Appropriate HTTP Status Codes

\\\java
// GOOD: Correct status codes
404 - Task not found (ResourceNotFoundException)
400 - Invalid input (BadRequestException)
409 - Email already exists (ConflictException)
401 - Invalid credentials (UnauthorizedException)
403 - User lacks permission (ForbiddenException)
500 - Internal server error (unhandled exceptions)

// BAD: Wrong status codes
200 - Error response (should be 4xx or 5xx)
500 - Validation error (should be 400)
400 - Authentication failure (should be 401)
\\\

### 5. Handle and Log Exceptions Properly

\\\java
// GOOD: Appropriate logging
@ExceptionHandler(ApplicationException.class)
public ResponseEntity<ErrorResponse> handle(ApplicationException ex) {
    log.warn("Application exception: {}", ex.getErrorCode(), ex);  // WARN for expected errors
    // Return error response
}

@ExceptionHandler(RuntimeException.class)
public ResponseEntity<ErrorResponse> handle(RuntimeException ex) {
    log.error("Unexpected runtime exception", ex);  // ERROR for unexpected errors
    // Return error response
}

// BAD: No logging
@ExceptionHandler(ApplicationException.class)
public ResponseEntity<ErrorResponse> handle(ApplicationException ex) {
    return ResponseEntity.status(ex.getHttpStatus()).body(response);  // Lost exception info
}
\\\

### 6. Document Exception Behavior in Controllers

\\\java
/**
 * Get task by ID
 * 
 * @param id Task ID
 * @return Task details
 * @throws TaskNotFoundException if task with given ID doesn't exist
 * 
 * @response 200 OK - Task found and returned
 * @response 404 NOT_FOUND - Task with given ID doesn't exist
 */
@GetMapping("/{id}")
@ApiResponse(responseCode = "200", description = "Task found")
@ApiResponse(responseCode = "404", description = "Task not found")
public ResponseEntity<TaskResponse> getTask(@PathVariable Long id) {
    return ResponseEntity.ok(TaskResponse.from(taskService.getTaskById(id)));
}
\\\

### 7. Use Try-Catch Only When Necessary

\\\java
// GOOD: Let exceptions propagate to handler
@PostMapping
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    // ValidationException propagates to GlobalExceptionHandler
    // UserAlreadyExistsException propagates to GlobalExceptionHandler
    User user = userService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(UserResponse.from(user));
}

// BAD: Unnecessary try-catch
@PostMapping
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    try {
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(UserResponse.from(user));
    } catch (UserAlreadyExistsException e) {
        return ResponseEntity.status(409).body(null);  // Bad practice
    }
}
\\\

---

##  Testing Exception Handling

### Unit Testing Custom Exceptions

\\\java
@SpringBootTest
class TaskServiceExceptionTest {
    
    @Autowired
    private TaskService taskService;
    
    @MockBean
    private TaskRepository taskRepository;
    
    @Test
    void getTaskById_WithInvalidId_ThrowsTaskNotFoundException() {
        Long invalidId = 999L;
        when(taskRepository.findById(invalidId)).thenReturn(Optional.empty());
        
        assertThrows(TaskNotFoundException.class, () -> {
            taskService.getTaskById(invalidId);
        });
    }
    
    @Test
    void getTaskById_WithInvalidId_ExceptionHasCorrectCode() {
        Long invalidId = 999L;
        when(taskRepository.findById(invalidId)).thenReturn(Optional.empty());
        
        TaskNotFoundException ex = assertThrows(TaskNotFoundException.class, () -> {
            taskService.getTaskById(invalidId);
        });
        
        assertEquals("RESOURCE_NOT_FOUND", ex.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }
}
\\\

### Testing Exception Handler

\\\java
@SpringBootTest
class GlobalExceptionHandlerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void handleTaskNotFound_ReturnsProperErrorResponse() throws Exception {
        mockMvc.perform(get("/api/tasks/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }
    
    @Test
    void handleValidationError_ReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors").isArray())
            .andExpect(jsonPath("$.fieldErrors[0].field").exists());
    }
}
\\\

---

##  Exception Layer Checklist

When implementing exception handling:

- [ ] Create custom exception class for each domain error
- [ ] Extend appropriate base exception (ResourceNotFoundException, ConflictException, etc.)
- [ ] Provide meaningful error code (string constant)
- [ ] Set correct HTTP status code
- [ ] Include detailed error message
- [ ] Add error code to ErrorResponse
- [ ] Implement GlobalExceptionHandler
- [ ] Handle Spring validation exceptions
- [ ] Handle Spring security exceptions
- [ ] Log exceptions appropriately
- [ ] Don't expose sensitive information
- [ ] Test exception handling in unit tests
- [ ] Document exception behavior in Javadoc
- [ ] Document API responses in Swagger/OpenAPI
- [ ] Use correlation IDs for tracing

---

##  Related Documentation

- **ARCHITECTURE.md** - Error handling strategy
- **README.md** - Main project overview
- **API Layer** - Controllers that throw exceptions
- **Service Layer** - Business logic exception handling

---

##  Quick Reference

### HTTP Status Code Mapping

\\\
200 OK                      - Successful request
201 Created                 - Resource created successfully
204 No Content              - Success with no response body
400 Bad Request             - Invalid input (BadRequestException)
401 Unauthorized            - Authentication failed (UnauthorizedException)
403 Forbidden               - Authorization failed (ForbiddenException)
404 Not Found               - Resource not found (ResourceNotFoundException)
409 Conflict                - State conflict (ConflictException)
422 Unprocessable Entity    - Validation error (ValidationException)
500 Internal Server Error   - Unhandled exception
\\\

### Exception Selection Guide

\\\
ResourceNotFoundException    Resource doesn't exist  404
BadRequestException          Invalid input format  400
ValidationException          Constraint violation  400/422
BusinessException            Business rule violated  400
ConflictException            State conflict/duplicate  409
UnauthorizedException        Authentication failed  401
ForbiddenException           Permission denied  403
InternalServerException      Unexpected error  500
\\\

### Error Code Naming Convention

\\\
RESOURCE_NOT_FOUND
DUPLICATE_EMAIL
INVALID_TASK_STATUS
UNAUTHORIZED
FORBIDDEN
VALIDATION_ERROR
BUSINESS_ERROR
INTERNAL_SERVER_ERROR
\\\

---

**Last Updated:** December 1, 2025  
**Version:** 1.0.0  
**Status:** Complete
