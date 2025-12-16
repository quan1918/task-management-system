# API Layer (REST Controllers)

##  Overview

The **API layer** is the presentation/interface adapter layer in our Clean Architecture design. It handles all HTTP requests and responses, serving as the bridge between external clients and the application's business logic.

**Location:** `src/main/java/com/taskmanagement/api/`

**Responsibility:** Convert HTTP requests to service calls and return HTTP responses

---

##  Current Implementation Status

### ✅ Implemented Controllers
- **TaskController.java** - Task CRUD operations (POST, GET, PUT, DELETE)
- **UserController.java** - User management (GET all, GET by ID, DELETE with soft delete, POST restore)

### 🔲 Not Yet Implemented
- AuthController.java - Authentication & authorization  
- ProjectController.java - Project management
- CommentController.java - Task comments
- AttachmentController.java - File attachments
- ReportController.java - Reports & analytics
- UserController - CREATE and UPDATE operations (only GET/DELETE/RESTORE implemented)

---

##  Core Responsibilities

### 1. Request Handling
- Parse incoming HTTP requests
- Extract path variables, query parameters, and request bodies
- Deserialize JSON payloads into DTOs (Data Transfer Objects)
- Validate request format using Bean Validation (@Valid)

### 2. Authorization & Security
- ✅ Basic Authentication with Spring Security (username/password)
- 🔲 JWT tokens (planned for Phase 2)
- 🔲 Role-based access control @PreAuthorize (planned)
- ✅ Secured all endpoints with HTTP Basic Auth

### 3. Service Orchestration
- Call appropriate service methods based on the request
- Delegate business logic to service layer
- Return DTOs instead of entities

### 4. Response Formatting
- Convert domain entities to DTOs
- Build proper HTTP response structures
- Return appropriate HTTP status codes (200, 201, 400, 401, 403, 404, 500, etc.)
- Include Location header for created resources (201)

### 5. Error Handling
- Exceptions caught by GlobalExceptionHandler
- Translate business exceptions to HTTP status codes
- Return standardized ErrorResponse

---

##  Implemented API Endpoints

### TaskController

Base URL: `/api/tasks`

| Method | Endpoint | Description | Request Body | Response | Auth |
|--------|----------|-------------|--------------|----------|------|
| POST | `/api/tasks` | Create new task | CreateTaskRequest | 201 + TaskResponse | Basic Auth |
| GET | `/api/tasks/{id}` | Get task by ID | - | 200 + TaskResponse | Basic Auth |
| PUT | `/api/tasks/{id}` | Update task | UpdateTaskRequest | 200 + TaskResponse | Basic Auth |
| DELETE | `/api/tasks/{id}` | Delete task | - | 204 No Content | Basic Auth |

---

### UserController

Base URL: `/api/users`

| Method | Endpoint | Description | Request Body | Response | Auth |
|--------|----------|-------------|--------------|----------|------|
| GET | `/api/users` | Get all active users | - | 200 + List<UserResponse> | Basic Auth |
| GET | `/api/users/{id}` | Get user by ID | - | 200 + UserResponse | Basic Auth |
| DELETE | `/api/users/{id}` | Soft delete user | - | 204 No Content | Basic Auth |
| POST | `/api/users/{id}/restore` | Restore deleted user | - | 200 + UserResponse | Basic Auth |

#### Business Rules
- **DELETE user** → Soft delete (deleted = true, not physical delete)
- **Tasks** → Unassigned automatically (assignee = NULL, status = UNASSIGNED)
- **Comments** → Preserved (audit trail, author_id retained)
- **Projects** → Preserved (business continuity, owner_id retained)
- **Cannot delete** user already deleted (409 Conflict)
- **Cannot restore** user not deleted (409 Conflict)

#### Example Responses

**GET /api/users - Success (200)**
```json
[
  {
    "id": 1,
    "username": "john_doe",
    "fullName": "John Doe",
    "email": "john@example.com",
    "active": true,
    "lastLoginAt": "2025-12-16T10:30:00",
    "createdAt": "2025-12-01T08:00:00",
    "updatedAt": "2025-12-16T10:30:00"
  }
]
```

**GET /api/users/1 - Success (200)**
```json
{
  "id": 1,
  "username": "john_doe",
  "fullName": "John Doe",
  "email": "john@example.com",
  "active": true,
  "lastLoginAt": "2025-12-16T10:30:00",
  "createdAt": "2025-12-01T08:00:00",
  "updatedAt": "2025-12-16T10:30:00"
}
```

**GET /api/users/999 - Not Found (404)**
```json
{
  "timestamp": "2025-12-16T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: 999",
  "path": "/api/users/999"
}
```

**DELETE /api/users/1 - Success (204)**
- No response body
- Tasks auto-unassigned (bulk update)
- Comments preserved
- Projects preserved

**DELETE /api/users/1 - Already Deleted (409)**
```json
{
  "timestamp": "2025-12-16T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "User is already deleted",
  "path": "/api/users/1"
}
```

**POST /api/users/1/restore - Success (200)**
```json
{
  "id": 1,
  "username": "john_doe",
  "fullName": "John Doe",
  "email": "john@example.com",
  "active": true,
  "lastLoginAt": "2025-12-16T10:30:00",
  "createdAt": "2025-12-01T08:00:00",
  "updatedAt": "2025-12-16T11:00:00"
}
```

**POST /api/users/1/restore - User Not Deleted (409)**
```json
{
  "timestamp": "2025-12-16T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "User is not deleted",
  "path": "/api/users/1/restore"
}
```

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

##  TaskController Implementation

### Complete Controller Code (Current)

```java
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {
    
    private final TaskService taskService;
    
    /**
     * POST /api/tasks - Create new task
     * Returns: 201 Created with Location header
     */
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request) {
        
        log.info("POST /api/tasks - Creating task: title={}", request.getTitle());
        
        TaskResponse response = taskService.createTask(request);
        
        // Build Location header: /api/tasks/{id}
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getId())
            .toUri();
        
        return ResponseEntity.created(location).body(response);
    }
    
    /**
     * GET /api/tasks/{id} - Get task by ID
     * Returns: 200 OK with TaskResponse
     * Throws: TaskNotFoundException (404) if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        log.info("GET /api/tasks/{} - Fetching task by ID", id);
        
        TaskResponse response = taskService.getTaskById(id);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * PUT /api/tasks/{id} - Update task (partial update)
     * Returns: 200 OK with updated TaskResponse
     * Throws: TaskNotFoundException (404) if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        
        log.info("PUT /api/tasks/{} - Updating task", id);
        
        TaskResponse response = taskService.updateTask(id, request);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * DELETE /api/tasks/{id} - Delete task
     * Returns: 204 No Content
     * Throws: TaskNotFoundException (404) if not found
     * Note: Cascade deletes comments and attachments
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        log.info("DELETE /api/tasks/{} - Deleting task", id);
        
        taskService.deleteTask(id);
        
        return ResponseEntity.noContent().build();
    }
}
```

### API Examples with Full Request/Response

#### 1. Create Task

**Request:**
```http
POST http://localhost:8080/api/tasks
Content-Type: application/json
Authorization: Basic YWRtaW46YWRtaW4xMjM=

{
  "title": "Fix login bug",
  "description": "Users cannot login with special characters",
  "priority": "HIGH",
  "dueDate": "2025-12-20T17:00:00",
  "estimatedHours": 8,
  "assigneeId": 1,
  "projectId": 1
}
```

**Response (201 Created):**
```json
{
  "id": 123,
  "title": "Fix login bug",
  "status": "PENDING",
  "priority": "HIGH",
  "assignee": {
    "id": 1,
    "username": "johndoe",
    "fullName": "John Doe",
    "email": "john@example.com"
  },
  "project": {
    "id": 1,
    "name": "Website Redesign"
  },
  "createdAt": "2025-12-14T10:30:00",
  "updatedAt": "2025-12-14T10:30:00"
}
```

**Headers:**
```
Location: http://localhost:8080/api/tasks/123
```

#### 2. Get Task

**Request:**
```http
GET http://localhost:8080/api/tasks/123
Authorization: Basic YWRtaW46YWRtaW4xMjM=
```

**Response (200 OK):**
```json
{
  "id": 123,
  "title": "Fix login bug",
  "status": "IN_PROGRESS",
  "assignee": {
    "id": 1,
    "username": "johndoe"
  }
}
```

#### 3. Update Task

**Request:**
```http
PUT http://localhost:8080/api/tasks/123
Content-Type: application/json

{
  "status": "IN_PROGRESS",
  "assigneeId": 2
}
```

#### 4. Delete Task  

**Request:**
```http
DELETE http://localhost:8080/api/tasks/123
```

**Response:** 204 No Content

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

## Related Documentation

- [Main README](../../../README.md) - Project overview  
- [Service Layer](../service/README.md) - Business logic
- [Entity Layer](../entity/README.md) - Domain model
- [DTO Layer](../dto/README.md) - Request/Response DTOs
- [Exception Handling](../exception/README.md) - Error handling

---

**Last Updated:** December 14, 2025  
**Version:** 0.5.0 - MVP Phase  
**Status:** TaskController fully implemented, other controllers pending
