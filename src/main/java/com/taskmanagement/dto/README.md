# DTO Layer (Data Transfer Objects)

##  Overview

The **DTO layer** defines the contracts between the API and external clients. DTOs (Data Transfer Objects) decouple the API interface from the domain model, allowing the database schema and API contracts to evolve independently.

**Location:** \src/main/java/com/taskmanagement/dto/\

**Responsibility:** Define request/response structures for API endpoints without exposing internal domain logic

---

##  Core Responsibilities

### 1. Request Validation
- Validate incoming data from clients using Bean Validation
- Define constraints (\@NotBlank\, \@NotNull\, \@Email\, etc.)
- Provide meaningful error messages
- Prevent invalid data from reaching services

### 2. Response Formatting
- Convert domain entities to safe response objects
- Hide internal details and implementation
- Include only necessary fields in responses
- Ensure consistent response structure

### 3. API Contract Definition
- Define the exact shape of API requests/responses
- Separate from domain entity definitions
- Support API versioning and evolution
- Document expected data types

### 4. Data Mapping
- Convert between DTOs and domain entities
- Handle nested object conversion
- Manage optional and required fields
- Support partial updates

### 5. Type Safety
- Use specific types instead of generic objects
- Provide compile-time safety
- Enable IDE code completion
- Improve code readability

---

##  Folder Structure

\\\
dto/
 request/
    CreateTaskRequest.java
    UpdateTaskRequest.java
    UpdateTaskStatusRequest.java
    CreateUserRequest.java
    UpdateUserRequest.java
    LoginRequest.java
    README.md
 response/
    TaskResponse.java
    UserResponse.java
    ProjectResponse.java
    CommentResponse.java
    LoginResponse.java
    README.md
 mapper/
    TaskMapper.java
    UserMapper.java
    README.md
 README.md                  # This file
\\\

---

##  Key Concepts

### Request DTOs

Request DTOs represent data sent by clients to the server. They include validation constraints.

**Naming Convention:** \{Action}{Domain}Request\
- CreateTaskRequest
- UpdateTaskRequest
- LoginRequest
- CreateUserRequest

**Anatomy:**

\\\java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {
    
    // Required fields
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;
    
    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;
    
    // Optional fields
    @Positive(message = "Project ID must be positive")
    private Long projectId;
    
    @NotNull(message = "Assignee is required")
    @Positive(message = "Assignee ID must be positive")
    private Long assigneeId;
    
    // Date constraints
    @NotNull(message = "Due date is required")
    @FutureOrPresent(message = "Due date must be in the future or today")
    private LocalDateTime dueDate;
    
    // Priority field
    @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL", message = "Priority must be one of: LOW, MEDIUM, HIGH, CRITICAL")
    private String priority;
}
\\\

### Response DTOs

Response DTOs represent data sent by the server to clients. They transform domain entities into safe API responses.

**Naming Convention:** \{Domain}Response\
- TaskResponse
- UserResponse
- ProjectResponse
- LoginResponse

**Anatomy:**

\\\java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    
    // Primary fields
    private Long id;
    private String title;
    private String description;
    private String status;
    private String priority;
    
    // Nested objects
    private UserResponse assignee;
    private ProjectResponse project;
    
    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    
    // Count fields
    private Integer commentCount;
    private Integer attachmentCount;
    
    /**
     * Convert from domain entity
     * Handles null checks and nested conversions
     */
    public static TaskResponse from(Task task) {
        if (task == null) return null;
        
        return TaskResponse.builder()
            .id(task.getId())
            .title(task.getTitle())
            .description(task.getDescription())
            .status(task.getStatus().toString())
            .priority(task.getPriority().toString())
            .assignee(UserResponse.from(task.getAssignee()))
            .project(ProjectResponse.from(task.getProject()))
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .createdBy(task.getCreatedBy())
            .commentCount(task.getComments().size())
            .attachmentCount(task.getAttachments().size())
            .build();
    }
    
    /**
     * Convert list of entities
     */
    public static List<TaskResponse> from(List<Task> tasks) {
        return tasks.stream()
            .map(TaskResponse::from)
            .collect(Collectors.toList());
    }
}
\\\

---

##  Common Validation Annotations

### String Validations

\\\java
@NotBlank              // Required, not blank
@NotNull               // Required, can be blank
@NotEmpty              // Required, size > 0
@Size(min=3, max=255)  // Length constraints
@Pattern(regexp="...")  // Regex pattern
@Email                 // Valid email format
@URL                   // Valid URL format
\\\

### Numeric Validations

\\\java
@NotNull               // Required
@Positive              // > 0
@PositiveOrZero        // >= 0
@Negative              // < 0
@Min(value=18)         // Minimum value
@Max(value=100)        // Maximum value
@DecimalMin("10.5")    // Decimal minimum
@DecimalMax("99.99")   // Decimal maximum
\\\

### Date/Time Validations

\\\java
@Past                  // Must be in the past
@PastOrPresent         // Must be past or today
@Future                // Must be in the future
@FutureOrPresent       // Must be future or today
\\\

### Collection Validations

\\\java
@NotEmpty              // List/Set not empty
@Size(min=1, max=10)   // List size constraints
@Valid                 // Validate nested objects
\\\

---

##  DTO Mapping Patterns

### Pattern 1: Static Factory Methods

\\\java
public class TaskResponse {
    
    public static TaskResponse from(Task task) {
        return TaskResponse.builder()
            .id(task.getId())
            .title(task.getTitle())
            // ... more mappings
            .build();
    }
    
    public static List<TaskResponse> from(List<Task> tasks) {
        return tasks.stream()
            .map(TaskResponse::from)
            .collect(Collectors.toList());
    }
}

// Usage in controller
TaskResponse response = TaskResponse.from(task);
\\\

### Pattern 2: Constructor with Entity Parameter

\\\java
@Data
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    
    // Constructor that converts from entity
    public UserResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
    }
}

// Usage
UserResponse response = new UserResponse(user);
\\\

### Pattern 3: Builder Pattern

\\\java
@Data
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    
    public static ProjectResponse from(Project project) {
        return ProjectResponse.builder()
            .id(project.getId())
            .name(project.getName())
            .description(project.getDescription())
            .build();
    }
}

// Usage
ProjectResponse response = ProjectResponse.builder()
    .id(1L)
    .name("My Project")
    .description("Description")
    .build();
\\\

### Pattern 4: Mapper Service (Phase 2+)

\\\java
@Service
public class TaskMapper {
    
    public TaskResponse toResponse(Task task) {
        if (task == null) return null;
        
        return TaskResponse.builder()
            .id(task.getId())
            .title(task.getTitle())
            // ... detailed mapping logic
            .build();
    }
    
    public Task toEntity(CreateTaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        return task;
    }
    
    public List<TaskResponse> toResponses(List<Task> tasks) {
        return tasks.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
}
\\\

---

##  Complete Example: Task DTOs

### CreateTaskRequest

\\\java
/**
 * Request DTO for creating a new task
 * Contains validation rules for input data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {
    
    @NotBlank(message = "Task title is required")
    @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
    private String title;
    
    @NotBlank(message = "Task description is required")
    @Size(min = 20, max = 2000, message = "Description must be between 20 and 2000 characters")
    private String description;
    
    @NotNull(message = "Project ID is required")
    @Positive(message = "Project ID must be a positive number")
    private Long projectId;
    
    @NotNull(message = "Assignee is required")
    @Positive(message = "Assignee ID must be a positive number")
    private Long assigneeId;
    
    @NotNull(message = "Due date is required")
    @FutureOrPresent(message = "Due date must be in the future or today")
    private LocalDateTime dueDate;
    
    @NotNull(message = "Priority is required")
    @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL", message = "Priority must be LOW, MEDIUM, HIGH, or CRITICAL")
    private String priority;
    
    // Optional field
    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
\\\

### UpdateTaskRequest

\\\java
/**
 * Request DTO for updating an existing task
 * All fields are optional for partial updates
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequest {
    
    @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
    private String title;
    
    @Size(min = 20, max = 2000, message = "Description must be between 20 and 2000 characters")
    private String description;
    
    @Positive(message = "Assignee ID must be a positive number")
    private Long assigneeId;
    
    @FutureOrPresent(message = "Due date must be in the future or today")
    private LocalDateTime dueDate;
    
    @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL", message = "Priority must be LOW, MEDIUM, HIGH, or CRITICAL")
    private String priority;
    
    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
\\\

### TaskResponse

\\\java
/**
 * Response DTO for task data
 * Converts domain entity to API-safe format
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    
    private Long id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private LocalDateTime dueDate;
    private String notes;
    
    // Related objects
    private UserResponse assignee;
    private ProjectResponse project;
    
    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Counts
    private Integer commentCount;
    private Integer attachmentCount;
    
    /**
     * Convert from Task entity
     * Handles null safety and nested conversions
     */
    public static TaskResponse from(Task task) {
        if (task == null) return null;
        
        return TaskResponse.builder()
            .id(task.getId())
            .title(task.getTitle())
            .description(task.getDescription())
            .status(task.getStatus().name())
            .priority(task.getPriority().name())
            .dueDate(task.getDueDate())
            .notes(task.getNotes())
            .assignee(UserResponse.from(task.getAssignee()))
            .project(ProjectResponse.from(task.getProject()))
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .createdBy(task.getCreatedBy())
            .updatedBy(task.getUpdatedBy())
            .commentCount(task.getComments() != null ? task.getComments().size() : 0)
            .attachmentCount(task.getAttachments() != null ? task.getAttachments().size() : 0)
            .build();
    }
    
    /**
     * Convert from Page of tasks
     */
    public static Page<TaskResponse> from(Page<Task> tasks) {
        return tasks.map(TaskResponse::from);
    }
    
    /**
     * Convert from List of tasks
     */
    public static List<TaskResponse> from(List<Task> tasks) {
        return tasks.stream()
            .map(TaskResponse::from)
            .collect(Collectors.toList());
    }
}
\\\

---

##  Security Considerations

### 1. Never Expose Sensitive Data

\\\java
// BAD: Exposes password hash
@Data
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String passwordHash;  // NEVER!
}

// GOOD: Only public data
@Data
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
}
\\\

### 2. Validate All Input

\\\java
// BAD: No validation
@Data
public class LoginRequest {
    private String username;
    private String password;
}

// GOOD: With validation
@Data
public class LoginRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be at least 8 characters")
    private String password;
}
\\\

### 3. Filter Sensitive Fields

\\\java
@Data
@JsonIgnoreProperties(ignoreUnknown = true)  // Ignore unknown fields
public class TaskResponse {
    
    private Long id;
    private String title;
    
    @JsonIgnore  // Never expose internal ID
    private String internalProcessId;
    
    @JsonProperty("createdBy")  // Rename for API
    private String internalCreatorName;
}
\\\

---

##  Testing DTOs

### Validation Testing

\\\java
@SpringBootTest
class CreateTaskRequestValidationTest {
    
    @Autowired
    private Validator validator;
    
    @Test
    void validRequest_PassesValidation() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Valid Title");
        request.setDescription("Valid description with at least 20 characters");
        request.setProjectId(1L);
        request.setAssigneeId(2L);
        request.setDueDate(LocalDateTime.now().plusDays(1));
        request.setPriority("HIGH");
        
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void missingTitle_FailsValidation() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle(null);
        request.setDescription("Valid description");
        
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("title is required")));
    }
    
    @Test
    void invalidPriority_FailsValidation() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Title");
        request.setDescription("Valid description with at least 20 characters");
        request.setPriority("INVALID");
        
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }
}
\\\

### Mapping Testing

\\\java
@SpringBootTest
class TaskResponseMappingTest {
    
    @Test
    void from_WithValidTask_ReturnsResponse() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Test Task");
        task.setDescription("Test Description");
        task.setStatus(TaskStatus.PENDING);
        
        TaskResponse response = TaskResponse.from(task);
        
        assertEquals(1L, response.getId());
        assertEquals("Test Task", response.getTitle());
        assertEquals("PENDING", response.getStatus());
    }
    
    @Test
    void from_WithNullTask_ReturnsNull() {
        TaskResponse response = TaskResponse.from(null);
        assertNull(response);
    }
    
    @Test
    void from_WithNestedObjects_MapsCorrectly() {
        User assignee = new User();
        assignee.setId(5L);
        assignee.setName("John Doe");
        
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Task");
        task.setAssignee(assignee);
        
        TaskResponse response = TaskResponse.from(task);
        
        assertNotNull(response.getAssignee());
        assertEquals("John Doe", response.getAssignee().getName());
    }
}
\\\

---

##  Best Practices

### 1. Separation of Concerns

\\\java
// BAD: Mixed request and response
@Data
public class TaskDTO {
    private Long id;  // Not needed in request
    private String title;
    private String description;
    private LocalDateTime createdAt;  // Not needed in request
}

// GOOD: Separate request and response
@Data
public class CreateTaskRequest {
    private String title;
    private String description;
}

@Data
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime createdAt;
}
\\\

### 2. Null-Safe Conversions

\\\java
// BAD: Can throw NullPointerException
public static TaskResponse from(Task task) {
    return TaskResponse.builder()
        .id(task.getId())
        .assignee(UserResponse.from(task.getAssignee()))  // Dangerous if null
        .build();
}

// GOOD: Handle nulls safely
public static TaskResponse from(Task task) {
    if (task == null) return null;
    
    return TaskResponse.builder()
        .id(task.getId())
        .assignee(task.getAssignee() != null ? UserResponse.from(task.getAssignee()) : null)
        .build();
}
\\\

### 3. Consistent Naming

\\\java
// Request DTOs
CreateTaskRequest
UpdateTaskRequest
UpdateTaskStatusRequest

// Response DTOs
TaskResponse
UserResponse
ProjectResponse

// Mapper classes
TaskMapper
UserMapper
\\\

### 4. Use Immutable DTOs

\\\java
// For response DTOs, consider immutability
@Value  // Lombok: immutable
public class TaskResponse {
    Long id;
    String title;
    String description;
    UserResponse assignee;
}
\\\

### 5. Document Complex Mappings

\\\java
/**
 * Converts Task entity to TaskResponse DTO
 * 
 * Mapping rules:
 * - status enum is converted to string
 * - priority enum is converted to string
 * - nested assignee and project are recursively converted
 * - comment and attachment counts are calculated
 * - null assignee/project result in null in response
 * 
 * @param task the source entity
 * @return TaskResponse with all mappings applied, or null if task is null
 */
public static TaskResponse from(Task task) {
    // Implementation
}
\\\

---

##  Common DTO Patterns

### List Response with Pagination

\\\java
@Data
@Builder
public class PaginatedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    
    public static <T> PaginatedResponse<T> from(Page<T> page) {
        return PaginatedResponse.<T>builder()
            .content(page.getContent())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .build();
    }
}

// Usage in controller
Page<Task> tasks = taskService.listTasks(pageable);
return ResponseEntity.ok(PaginatedResponse.from(tasks.map(TaskResponse::from)));
\\\

### Success Response Wrapper

\\\java
@Data
@Builder
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private long timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
\\\

### Error Response

\\\java
@Data
@Builder
public class ErrorResponse {
    private String code;
    private String message;
    private long timestamp;
    private String path;
    private List<FieldError> errors;
    
    @Data
    @Builder
    public static class FieldError {
        private String field;
        private String message;
    }
}
\\\

---

##  DTO Checklist

When creating new DTOs:

- [ ] Request DTOs in \dto/request/\ folder
- [ ] Response DTOs in \dto/response/\ folder
- [ ] Validation annotations on request fields
- [ ] \@NotBlank\/\@NotNull\ for required fields
- [ ] \@Size\/\@Pattern\ for format constraints
- [ ] \@Valid\ for nested object validation
- [ ] Static \rom()\ methods for conversion
- [ ] Null-safe conversion logic
- [ ] Builder pattern for response DTOs
- [ ] Documentation for complex mappings
- [ ] No sensitive data exposed
- [ ] Unit tests for validation
- [ ] Unit tests for mapping

---

##  Related Documentation

- **ARCHITECTURE.md** - Overall system architecture
- **README.md** - Main project overview
- **API Layer** - Controller implementation
- **Domain Entities** - Entity definitions
- **Service Layer** - Business logic

---

##  Quick Examples

### Creating a Task

**Request:**
\\\ash
POST /api/tasks
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "Implement user dashboard",
  "description": "Create a responsive dashboard for users to view their tasks",
  "projectId": 1,
  "assigneeId": 5,
  "dueDate": "2025-12-15T17:00:00",
  "priority": "HIGH"
}
\\\

**Response (201 Created):**
\\\json
{
  "id": 123,
  "title": "Implement user dashboard",
  "description": "Create a responsive dashboard for users to view their tasks",
  "status": "PENDING",
  "priority": "HIGH",
  "dueDate": "2025-12-15T17:00:00",
  "assignee": {
    "id": 5,
    "name": "John Doe",
    "email": "john@example.com"
  },
  "project": {
    "id": 1,
    "name": "Web Platform"
  },
  "createdAt": "2025-12-01T10:30:00",
  "updatedAt": "2025-12-01T10:30:00",
  "createdBy": "admin",
  "commentCount": 0,
  "attachmentCount": 0
}
\\\

---

**Last Updated:** December 1, 2025  
**Version:** 1.0.0  
**Status:** Complete
