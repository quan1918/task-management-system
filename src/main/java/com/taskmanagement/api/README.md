# API Layer (REST Controllers)

##  Overview

The **API layer** is the presentation/interface adapter layer in our Clean Architecture design. It handles all HTTP requests and responses, serving as the bridge between external clients and the application's business logic.

**Location:** \src/main/java/com/taskmanagement/api/\

**Responsibility:** Convert HTTP requests to service calls and return HTTP responses

---

##  Core Responsibilities

### 1. Request Handling
- Parse incoming HTTP requests
- Extract path variables, query parameters, and request bodies
- Deserialize JSON payloads into DTOs (Data Transfer Objects)
- Validate request format using Bean Validation (@Valid)

### 2. Authorization & Security
- Check user authentication via JWT tokens
- Enforce role-based access control (@PreAuthorize)
- Validate permissions before allowing operations

### 3. Service Orchestration
- Call appropriate service methods based on the request
- Orchestrate multi-service workflows for complex operations
- Handle transaction boundaries

### 4. Response Formatting
- Convert domain entities to DTOs
- Build proper HTTP response structures
- Return appropriate HTTP status codes (200, 201, 400, 401, 403, 404, 500, etc.)
- Include proper response headers

### 5. Error Handling
- Catch exceptions from services
- Translate business exceptions to HTTP status codes
- Return standardized error responses

---

##  Folder Structure

\\\
api/
 TaskController.java           # Task CRUD operations
 UserController.java           # User management
 AuthController.java           # Authentication & authorization
 ProjectController.java        # Project management
 CommentController.java        # Task comments
 AttachmentController.java     # File attachments
 ReportController.java         # Reports & analytics
 README.md                     # This file
\\\

---

##  Key Concepts

### Controllers

Controllers are REST endpoint handlers that define the API surface. Each controller handles one domain aggregate (Task, User, Project, etc.).

**Naming Convention:** \{Domain}Controller\
- TaskController
- UserController
- ProjectController
- CommentController

**Anatomy of a Controller:**

\\\java
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Task management endpoints")
public class TaskController {
    
    private final TaskService taskService;
    
    // Constructor injection
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }
    
    // Endpoints defined here
}
\\\

### Endpoints (HTTP Methods)

**Standard RESTful operations:**

| Method | Purpose | Example |
|--------|---------|---------|
| GET | Retrieve resource(s) | GET /api/tasks |
| POST | Create new resource | POST /api/tasks |
| PUT | Replace entire resource | PUT /api/tasks/{id} |
| PATCH | Partially update resource | PATCH /api/tasks/{id}/status |
| DELETE | Remove resource | DELETE /api/tasks/{id} |

### DTOs (Data Transfer Objects)

DTOs define the API contract separate from domain entities. They allow API changes without affecting the database schema.

**Request DTOs** - What clients send to the server:
\\\java
@Data
@NoArgsConstructor
public class CreateTaskRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotNull(message = "Project ID is required")
    private Long projectId;
    
    @NotNull(message = "Assignee ID is required")
    private Long assigneeId;
    
    @NotNull(message = "Due date is required")
    @FutureOrPresent(message = "Due date must be in the future")
    private LocalDateTime dueDate;
}
\\\

**Response DTOs** - What the server sends back:
\\\java
@Data
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private UserResponse assignee;
    private ProjectResponse project;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Convert from domain entity
    public static TaskResponse from(Task task) {
        return TaskResponse.builder()
            .id(task.getId())
            .title(task.getTitle())
            .description(task.getDescription())
            .status(task.getStatus())
            .assignee(UserResponse.from(task.getAssignee()))
            .project(ProjectResponse.from(task.getProject()))
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .build();
    }
}
\\\

---

##  Architecture Pattern

### Request-Response Flow

\\\
HTTP Request
    
TaskController.createTask()
    
@Valid validates CreateTaskRequest
    
taskService.createTask(request)
    
Service processes business logic
    
TaskResponse.from(task)
    
ResponseEntity.status(201).body(response)
    
HTTP Response (201 Created)
\\\

### Dependency Injection

Controllers use **constructor injection** to receive service dependencies:

\\\java
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    
    private final TaskService taskService;
    private final CommentService commentService;
    
    // Constructor injection (preferred)
    public TaskController(TaskService taskService, CommentService commentService) {
        this.taskService = taskService;
        this.commentService = commentService;
    }
}
\\\

**Benefits:**
- Dependencies are immutable (final)
- Easy to test (pass mocks in constructor)
- Clear dependency declarations
- Required dependencies are obvious

### Authorization

Use \@PreAuthorize\ for role-based access control:

\\\java
// Only ADMIN role
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteTask(@PathVariable Long id) { }

// MANAGER or ADMIN
@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
@PostMapping
public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) { }

// Has specific permission
@PreAuthorize("hasAuthority('TASK_EDIT')")
@PutMapping("/{id}")
public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) { }
\\\

### Error Handling

The \GlobalExceptionHandler\ catches exceptions and returns standardized error responses. Controllers throw domain-specific exceptions:

\\\java
@GetMapping("/{id}")
public ResponseEntity<TaskResponse> getTask(@PathVariable Long id) {
    // Service throws TaskNotFoundException if not found
    Task task = taskService.getTaskById(id);
    return ResponseEntity.ok(TaskResponse.from(task));
}
\\\

The exception is caught by the global handler:

\\\java
@ExceptionHandler(TaskNotFoundException.class)
public ResponseEntity<ErrorResponse> handleTaskNotFound(TaskNotFoundException ex) {
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.builder()
            .code("TASK_NOT_FOUND")
            .message(ex.getMessage())
            .timestamp(System.currentTimeMillis())
            .build());
}
\\\

---

##  Example: TaskController

\\\java
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Task management endpoints")
public class TaskController {
    
    private final TaskService taskService;
    
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }
    
    // Create new task
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Create a new task")
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request) {
        Task task = taskService.createTask(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(TaskResponse.from(task));
    }
    
    // List all tasks (paginated)
    @GetMapping
    @Operation(summary = "List all tasks")
    public ResponseEntity<Page<TaskResponse>> listTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Task> tasks = taskService.listTasks(page, size);
        return ResponseEntity.ok(tasks.map(TaskResponse::from));
    }
    
    // Get single task
    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<TaskResponse> getTask(@PathVariable Long id) {
        Task task = taskService.getTaskById(id);
        return ResponseEntity.ok(TaskResponse.from(task));
    }
    
    // Update task
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or @taskSecurityService.isAssignee(#id)")
    @Operation(summary = "Update a task")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        Task task = taskService.updateTask(id, request);
        return ResponseEntity.ok(TaskResponse.from(task));
    }
    
    // Update task status only
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update task status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        Task task = taskService.updateTaskStatus(id, request.getStatus());
        return ResponseEntity.ok(TaskResponse.from(task));
    }
    
    // Delete task
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
\\\

---

##  Security Best Practices

### 1. Validate Input
\\\java
@PostMapping
public ResponseEntity<TaskResponse> createTask(
        @Valid @RequestBody CreateTaskRequest request) {  // @Valid triggers validation
    // ...
}
\\\

### 2. Check Authorization
\\\java
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
    // Only admins can execute
}
\\\

### 3. Avoid Exposing Internal Details
\\\java
// BAD: Exposes Hibernate proxy or internal details
public ResponseEntity<Task> getTask(Long id) {
    return ResponseEntity.ok(taskRepository.findById(id).orElseThrow());
}

// GOOD: Convert to DTO which hides implementation
public ResponseEntity<TaskResponse> getTask(Long id) {
    Task task = taskService.getTaskById(id);
    return ResponseEntity.ok(TaskResponse.from(task));
}
\\\

### 4. Use Appropriate HTTP Status Codes
\\\java
// 201 Created for new resources
ResponseEntity.status(HttpStatus.CREATED).body(response);

// 204 No Content for successful deletions
ResponseEntity.noContent().build();

// 400 Bad Request for validation errors (handled by GlobalExceptionHandler)
// 401 Unauthorized for missing authentication
// 403 Forbidden for insufficient permissions
// 404 Not Found for missing resources
// 500 Internal Server Error for unexpected errors
\\\

---

##  Pagination

Handle large result sets efficiently:

\\\java
@GetMapping
public ResponseEntity<Page<TaskResponse>> listTasks(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status) {
    
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    Page<Task> tasks = taskService.listTasks(pageable, status);
    
    return ResponseEntity.ok(tasks.map(TaskResponse::from));
}
\\\

**Response:**
\\\json
{
  "content": [
    { "id": 1, "title": "Task 1" },
    { "id": 2, "title": "Task 2" }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "sorted": true, "unsorted": false }
  },
  "totalElements": 150,
  "totalPages": 8,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false
}
\\\

---

##  Testing Controllers

Test controllers using MockMvc:

\\\java
@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private TaskService taskService;
    
    @Test
    void createTask_WithValidRequest_Returns201() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("New Task");
        request.setDescription("Task description");
        request.setProjectId(1L);
        request.setAssigneeId(2L);
        request.setDueDate(LocalDateTime.now().plusDays(7));
        
        Task savedTask = new Task();
        savedTask.setId(1L);
        savedTask.setTitle("New Task");
        
        when(taskService.createTask(any())).thenReturn(savedTask);
        
        mockMvc.perform(post("/api/tasks")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request))
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.title").value("New Task"));
    }
    
    @Test
    void getTask_WithInvalidId_Returns404() throws Exception {
        when(taskService.getTaskById(999L))
            .thenThrow(new TaskNotFoundException("Task not found"));
        
        mockMvc.perform(get("/api/tasks/999")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }
}
\\\

---

##  API Documentation

Use OpenAPI annotations for Swagger/SpringDoc:

\\\java
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Task management endpoints")
public class TaskController {
    
    @PostMapping
    @Operation(
        summary = "Create a new task",
        description = "Creates a new task and assigns it to a team member"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Task created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request) {
        // ...
    }
}
\\\

**Access documentation at:** \http://localhost:8080/api/swagger-ui.html\

---

##  Common Response Patterns

### Success Response
\\\java
// Single resource
return ResponseEntity.ok(TaskResponse.from(task));

// Created resource
return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task));

// No content
return ResponseEntity.noContent().build();
\\\

### Error Response
\\\java
// Handled by GlobalExceptionHandler
throw new TaskNotFoundException("Task with ID " + id + " not found");
throw new UnauthorizedAccessException("You don't have permission to update this task");
throw new ValidationException("Title cannot be empty");
\\\

---

##  Best Practices

### 1. Thin Controllers
Controllers should be thin - let services handle business logic.

\\\java
// BAD: Business logic in controller
@PostMapping
public ResponseEntity<TaskResponse> createTask(@RequestBody CreateTaskRequest request) {
    if (request.getTitle() == null || request.getTitle().isEmpty()) {
        throw new ValidationException("Title required");
    }
    if (request.getDueDate().isBefore(LocalDateTime.now())) {
        throw new ValidationException("Due date must be in the future");
    }
    // ... more validation
    Task task = new Task();
    task.setTitle(request.getTitle());
    // ... more setup
    taskRepository.save(task);
    return ResponseEntity.status(201).body(TaskResponse.from(task));
}

// GOOD: Business logic delegated to service
@PostMapping
public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
    Task task = taskService.createTask(request);  // Service handles all logic
    return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task));
}
\\\

### 2. Use Appropriate HTTP Status Codes
\\\java
@PostMapping
public ResponseEntity<TaskResponse> createTask(...) {
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

@GetMapping("/{id}")
public ResponseEntity<TaskResponse> getTask(...) {
    return ResponseEntity.ok(response);  // 200 OK
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteTask(...) {
    return ResponseEntity.noContent().build();  // 204 No Content
}
\\\

### 3. Consistent Request/Response Formats
\\\java
// Request
@Data
public class CreateTaskRequest {
    @NotBlank
    private String title;
}

// Response
@Data
@Builder
public class TaskResponse {
    private Long id;
    private String title;
}
\\\

### 4. Version Your API (Phase 2+)
\\\java
@RestController
@RequestMapping("/api/v1/tasks")  // Version in URL
public class TaskControllerV1 { }

@RestController
@RequestMapping("/api/v2/tasks")  // Future version with changes
public class TaskControllerV2 { }
\\\

---

##  Related Documentation

- **README.md** - Main project overview
- **ARCHITECTURE.md** - System architecture and design patterns
- **Service Layer** - Business logic implementation
- **DTO Layer** - Data transfer object definitions
- **Exception Handling** - Global error handling

---

##  Checklist for New Controllers

When creating a new controller, ensure:

- [ ] Class annotated with \@RestController\
- [ ] \@RequestMapping\ specifies base path
- [ ] \@Tag\ annotation for OpenAPI documentation
- [ ] Constructor injection for dependencies
- [ ] Request DTOs with \@Valid\ validation
- [ ] Response DTOs converting from domain entities
- [ ] \@PreAuthorize\ for authorization checks
- [ ] Appropriate HTTP status codes
- [ ] \@Operation\ annotations for each endpoint
- [ ] Unit tests with MockMvc
- [ ] Null checks and exception handling

---

**Last Updated:** December 1, 2025  
**Version:** 1.0.0  
**Status:** Complete
