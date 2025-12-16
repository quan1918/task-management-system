# Exception Handling Layer

## 📋 Overview

**Purpose:** Centralized exception handling with consistent error responses across all API endpoints.

**Location:** `src/main/java/com/taskmanagement/exception/`

**Pattern:** @RestControllerAdvice for global exception handling

**Current Status:**  
✅ **MVP Phase** - Core exception handling implemented  
🔲 **Future** - Custom business exceptions, validation groups

---

## 📁 Current Structure

```
exception/
├── GlobalExceptionHandler.java      # ✅ Centralized exception handler
├── ErrorResponse.java               # ✅ Standardized error response DTO
├── TaskNotFoundException.java       # ✅ 404 when task not found
├── UserNotFoundException.java       # ✅ 404 when user not found
├── ProjectNotFoundException.java    # ✅ 404 when project not found
└── README.md                        # This file
```

---

## 🎯 Exception Handlers Summary

| Exception Type | HTTP Status | Handler Method | Triggered By |
|----------------|-------------|----------------|--------------|
| `MethodArgumentNotValidException` | 400 | handleValidationErrors() | @Valid constraint violations |
| `TaskNotFoundException` | 404 | handleTaskNotFound() | Task ID not found |
| `UserNotFoundException` | 404 | handleUserNotFound() | Assignee ID not found |
| `ProjectNotFoundException` | 404 | handleProjectNotFound() | Project ID not found |
| `Exception` (generic) | 500 | handleGenericError() | Unexpected errors |

---

## 1. GlobalExceptionHandler

**Location:** [GlobalExceptionHandler.java](GlobalExceptionHandler.java)

**Purpose:** Catch and handle all exceptions thrown by controllers

```java
@RestControllerAdvice  // Applies to all @RestController
@Slf4j
public class GlobalExceptionHandler {
    // Exception handler methods...
}
```

### Validation Errors (400)

**Handles:** Bean Validation failures from `@Valid` annotation

**Example scenarios:**
- Missing required fields (@NotBlank, @NotNull)
- Invalid field formats (@Size, @Email, @Pattern)
- Business constraints (@FutureOrPresent, @Positive)

**Response:**
```json
{
  "timestamp": "2025-12-15T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed. Check 'errors' field for details.",
  "path": "/api/tasks",
  "errors": {
    "title": "Task title is required",
    "assigneeId": "Assignee ID must be positive"
  }
}
```

### Task Not Found (404)

**Handles:** `TaskNotFoundException`

**Thrown by:**
- `TaskService.getTaskById()`
- `TaskService.updateTask()`
- `TaskService.deleteTask()`

**Response:**
```json
{
  "timestamp": "2025-12-15T10:30:00",
  "status": 404,
  "error": "Task Not Found",
  "message": "Task not found with ID: 999",
  "path": "/api/tasks/999"
}
```

### User Not Found (404)

**Handles:** `UserNotFoundException`

**Thrown by:**
- `TaskService.createTask()` - assignee doesn't exist
- `TaskService.updateTask()` - new assignee doesn't exist

**Response:**
```json
{
  "timestamp": "2025-12-15T10:30:00",
  "status": 404,
  "error": "User Not Found",
  "message": "User not found with ID: 5",
  "path": "/api/tasks"
}
```

### Project Not Found (404)

**Handles:** `ProjectNotFoundException`

**Thrown by:**
- `TaskService.createTask()` - project doesn't exist

**Response:**
```json
{
  "timestamp": "2025-12-15T10:30:00",
  "status": 404,
  "error": "Project Not Found",
  "message": "Project not found with ID: 10",
  "path": "/api/tasks"
}
```

### Generic Errors (500)

**Handles:** All unhandled exceptions

**Triggered by:**
- Database connection failures
- Unexpected runtime errors
- Any exception not explicitly handled

**Response:**
```json
{
  "timestamp": "2025-12-15T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred. Please try again later.",
  "path": "/api/tasks"
}
```

---

## 2. ErrorResponse

**Location:** [ErrorResponse.java](ErrorResponse.java)

**Purpose:** Standardized error response DTO

```java
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> errors;  // Optional: validation errors
}
```

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `timestamp` | LocalDateTime | ✅ | When error occurred (ISO-8601) |
| `status` | int | ✅ | HTTP status code (400, 404, 500) |
| `error` | String | ✅ | Error type ("Validation Failed") |
| `message` | String | ✅ | Human-readable message |
| `path` | String | ✅ | Request path that failed |
| `errors` | Map | ❌ | Field-level validation errors |

---

## 3. Custom Exceptions

### TaskNotFoundException

```java
public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long taskId) {
        super("Task not found with ID: " + taskId);
    }
}
```

**Usage:**
```java
Task task = taskRepository.findById(id)
    .orElseThrow(() -> new TaskNotFoundException(id));
```

### UserNotFoundException

```java
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("User not found with ID: " + userId);
    }
}
```

**Usage:**
```java
User user = userRepository.findById(assigneeId)
    .orElseThrow(() -> new UserNotFoundException(assigneeId));
```

### ProjectNotFoundException

```java
public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(Long projectId) {
        super("Project not found with ID: " + projectId);
    }
}
```

**Usage:**
```java
Project project = projectRepository.findById(projectId)
    .orElseThrow(() -> new ProjectNotFoundException(projectId));
```

---

## 🔄 Exception Flow

### Success Path
```
Client → Controller → Service → Repository → Database
                                              ↓
Client ← 200 OK + Data ← ← ← ← ← ← ← ← ← ← ← ←
```

### Validation Error Path
```
Client → Controller
         ↓
         @Valid fails → MethodArgumentNotValidException
         ↓
         GlobalExceptionHandler.handleValidationErrors()
         ↓
Client ← 400 Bad Request + ErrorResponse (with field errors)
```

### Not Found Error Path
```
Client → Controller → Service → Repository
                      ↓
                      findById().orElseThrow()
                      ↓
                      TaskNotFoundException
                      ↓
                      GlobalExceptionHandler.handleTaskNotFound()
                      ↓
Client ← 404 Not Found + ErrorResponse
```

---

## ⚠️ Known Limitations

### 1. No Business Logic Exceptions

**Missing:**
```java
InvalidStatusTransitionException  // Can't go COMPLETED → PENDING
AssigneeNotInProjectException     // Assignee not project member
DuplicateTaskException            // Task with same title exists
TaskAlreadyCompletedException     // Can't modify completed task
```

### 2. No Authorization Exceptions

**Missing:**
```java
UnauthorizedException    // 401 - Not authenticated
ForbiddenException       // 403 - No permission to access resource
```

**Current:** Only Basic Auth, no role-based access control

### 3. No Conflict Handling

**Missing:**
```java
ResourceConflictException      // 409 - Resource already exists
OptimisticLockException        // 409 - Concurrent modification
```

### 4. Generic Error Messages

**Current behavior:**
- 500 errors show generic message for security
- Root cause not revealed to client
- Makes debugging harder

**Future:** Add error codes for better client-side handling

---

## 📚 Best Practices

### ✅ Do's

```java
// Use specific exceptions
throw new TaskNotFoundException(id);

// Log appropriately
log.error("Task not found: {}", ex.getMessage());  // 404
log.warn("Validation error: {}", ex.getMessage()); // 400
log.error("Unexpected error", ex);                 // 500 (with stack trace)

// Return consistent format
return ResponseEntity.status(status).body(errorResponse);
```

### ❌ Don'ts

```java
// Don't expose sensitive info
"Database connection failed: jdbc://localhost:5432/..."  // ❌

// Don't swallow exceptions
catch (Exception e) { }  // ❌

// Don't return inconsistent formats
return "Error: Task not found";  // ❌
```

---

## 🔮 Planned Enhancements

### Phase 1: Business Exceptions
```java
InvalidStatusTransitionException
TaskAlreadyCompletedException
AssigneeNotInProjectException
DuplicateResourceException
```

### Phase 2: Authorization
```java
UnauthorizedException (401)
ForbiddenException (403)
ResourceConflictException (409)
```

### Phase 3: Error Codes
```java
{
  "errorCode": "TASK_NOT_FOUND",
  "timestamp": "...",
  "status": 404,
  "message": "..."
}
```

### Phase 4: Localization
```java
// i18n support for error messages
messageSource.getMessage("error.task.notFound", locale);
```

---

## 📖 Related Documentation

- [TaskController](../api/README.md) - API endpoints throwing exceptions
- [TaskService](../service/README.md) - Business logic throwing exceptions
- [DTOs](../dto/README.md) - Validation rules

---

**Last Updated:** December 15, 2025  
**Version:** 0.5.0 - MVP Phase  
**Status:** Core exception handling complete, business exceptions pending
