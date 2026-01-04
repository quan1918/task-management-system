# Task Management System - Architecture Documentation

> **Purpose:** This document explains the Clean Architecture implementation, design patterns, and structural decisions in the Task Management System. Essential reading for understanding the system's architectural intent and dependency flow.

---

## Table of Contents
1. [Clean Architecture Overview](#clean-architecture-overview)
2. [Layer-by-Layer Breakdown](#layer-by-layer-breakdown)
3. [Design Patterns](#design-patterns)
4. [Package Organization](#package-organization)
5. [Dependency Rules](#dependency-rules)
6. [Cross-Cutting Concerns](#cross-cutting-concerns)
7. [Future Architecture Plans](#future-architecture-plans)

---

## Clean Architecture Overview

Clean Architecture organizes code into **concentric layers** with business logic at the center and external dependencies at the edges. This architecture ensures:

- **Independence from frameworks** - Business logic doesn't depend on Spring Boot
- **Testability** - Each layer can be tested in isolation
- **Independence from UI** - Business rules work with any UI (REST API, CLI, GraphQL)
- **Independence from database** - Switch from PostgreSQL to MongoDB without changing business logic

### Architectural Layers (Outside → Inside)

```
┌─────────────────────────────────────────────────────┐
│   Layer 1: Controllers (API / Presentation)         │  ← HTTP, JSON, Validation
│   - Accept requests, return responses               │
│   - Map DTOs to/from domain models                  │
└─────────────────────────────────────────────────────┘
                      ↓ depends on
┌─────────────────────────────────────────────────────┐
│   Layer 2: Services (Business Logic)                │  ← Core business rules
│   - Implement use cases                             │
│   - Orchestrate repositories                        │
│   - Publish domain events                           │
└─────────────────────────────────────────────────────┘
                      ↓ depends on
┌─────────────────────────────────────────────────────┐
│   Layer 3: Repositories (Data Access)               │  ← Database abstraction
│   - Abstract CRUD operations                        │
│   - Define query interfaces                         │
│   - Hide ORM implementation                         │
└─────────────────────────────────────────────────────┘
                      ↓ depends on
┌─────────────────────────────────────────────────────┐
│   Layer 4: Entities (Domain Models)                 │  ← Pure business objects
│   - Represent business concepts                     │
│   - Define relationships                            │
│   - Enforce invariants                              │
└─────────────────────────────────────────────────────┘
```

**Key Principle:** Dependencies flow **inward**. Outer layers depend on inner layers, never the reverse.

---

## Layer-by-Layer Breakdown

### Layer 1: Controllers (API / Presentation Layer)

**Location:** `com.taskmanagement.api`

**Responsibility:**
- Accept HTTP requests from clients
- Validate request parameters using `@Valid`
- Call appropriate service methods
- Return HTTP responses with proper status codes (200, 201, 404, 400, 500)
- Handle request/response serialization (JSON via Jackson)

**Key Components:**
- `TaskController` - Task CRUD operations (`/api/tasks`)
- `UserController` - User management (`/api/users`)
- `ProjectController` - Project/Team management (`/api/projects`)

**Dependencies:** 
- ✅ Services (business logic)
- ✅ DTOs (request/response contracts)
- ❌ Independent of database (doesn't import JPA or Hibernate)

**Example Request Flow:**
```
HTTP POST /api/tasks
    ↓
TaskController.createTask(@Valid CreateTaskRequest request)
    ↓
taskService.createTask(request)  ← Calls business logic
    ↓
HTTP 201 Created + TaskResponse
```

**Code Example:**
```java
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;
    
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
        @Valid @RequestBody CreateTaskRequest request
    ) {
        TaskResponse response = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

---

### Layer 2: Services (Business Logic Layer)

**Location:** `com.taskmanagement.service`

**Responsibility:**
- Implement core business logic and use cases
- Orchestrate repositories (call multiple repositories if needed)
- Handle transactions with `@Transactional`
- Validate business rules (e.g., user must be active before assignment)
- Publish domain events (e.g., `TaskCreatedEvent`)
- Calculate derived data (e.g., task overdue status)

**Key Components:**
- `TaskService` - Task creation, assignment, status updates
- `UserService` - User management, RBAC enforcement
- `ProjectService` - Project/Team operations

**Dependencies:** 
- ✅ Repositories (data access)
- ✅ Events (domain events)
- ✅ Utilities (helper methods)
- ❌ Independent of HTTP (doesn't import Spring Web annotations)

**Example Business Logic:**
```java
@Service
@Transactional
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    
    public TaskResponse createTask(CreateTaskRequest request) {
        // 1. Validate assignees exist and are active
        List<User> assignees = userRepository.findAllById(request.getAssigneeIds());
        if (assignees.size() != request.getAssigneeIds().size()) {
            throw new UserNotFoundException("One or more assignees not found");
        }
        
        // 2. Validate project exists and is active
        Project project = projectRepository.findByIdAndActiveTrue(request.getProjectId())
            .orElseThrow(() -> new ProjectNotFoundException("Project not found"));
        
        // 3. Create task entity
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setProject(project);
        task.setAssignees(new HashSet<>(assignees));
        task.setStatus(TaskStatus.PENDING);
        
        // 4. Save task
        Task savedTask = taskRepository.save(task);
        
        // 5. Publish domain event (future feature)
        // eventPublisher.publishEvent(new TaskCreatedEvent(savedTask));
        
        // 6. Return response DTO
        return TaskResponse.fromEntity(savedTask);
    }
}
```

---

### Layer 3: Repositories (Data Access Layer)

**Location:** `com.taskmanagement.repository`

**Responsibility:**
- Abstract database operations (CRUD)
- Define custom query methods using Spring Data JPA conventions
- Hide implementation details (PostgreSQL, SQL, Hibernate)
- Provide interface contracts (no concrete implementations needed)
- Enable dependency injection of data sources

**Key Components:**
- `TaskRepository extends JpaRepository<Task, Long>` - Task queries
- `UserRepository extends JpaRepository<User, Long>` - User queries
- `ProjectRepository extends JpaRepository<Project, Long>` - Project queries
- `CommentRepository` - Comment queries (defined but not exposed via API yet)

**Dependencies:** 
- ✅ Entities (domain models)
- ❌ Independent of business logic (services are decoupled from repository implementation)

**Example Repository Interface:**
```java
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Spring Data JPA auto-generates implementation
    List<Task> findByProjectId(Long projectId);
    
    List<Task> findByStatusAndDueDateBefore(
        TaskStatus status, 
        LocalDateTime dueDate
    );
    
    // Custom native query (workaround for Hibernate @Where filter issue)
    @Query(value = "SELECT * FROM tasks WHERE id = :id", nativeQuery = true)
    Optional<Task> findByIdNative(@Param("id") Long id);
    
    @Query(value = "SELECT user_id FROM task_assignees WHERE task_id = :taskId", 
           nativeQuery = true)
    List<Long> findAssigneeIdsByTaskId(@Param("taskId") Long taskId);
}
```

**Why Native Queries?**
- Hibernate 6.x `@Where(clause = "deleted = false")` on User entity breaks Many-to-Many lazy loading
- Native queries bypass Hibernate filters and load data directly from database
- See [KNOWN_ISSUES.md](KNOWN_ISSUES.md) for detailed explanation

---

### Layer 4: Entities (Domain / Data Model Layer)

**Location:** `com.taskmanagement.entity`

**Responsibility:**
- Define database table structure via JPA annotations
- Represent core business domain concepts (Task, User, Project)
- Enforce constraints and validation (`@NotNull`, `@Size`)
- Define relationships between entities (`@ManyToOne`, `@ManyToMany`)
- Implement lifecycle hooks (`@PrePersist`, `@PreUpdate`)

**Key Components:**
- `Task` - Core task entity with N:N assignees, N:1 project, 1:N comments/attachments
- `User` - System users with soft delete support (`@Where(clause = "deleted = false")`)
- `Project` - Project/Team grouping
- `Comment` - Task comments (cascade delete)
- `Attachment` - File attachments (cascade delete)
- `TaskStatus` - Status enum (PENDING, IN_PROGRESS, COMPLETED, BLOCKED, IN_REVIEW, CANCELLED)
- `TaskPriority` - Priority enum (LOW, MEDIUM, HIGH, CRITICAL)

**Dependencies:** 
- ❌ None (entities are at the center of Clean Architecture)

**Example Entity:**
```java
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    private TaskPriority priority = TaskPriority.MEDIUM;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "task_assignees",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> assignees = new HashSet<>();
    
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();
    
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();
    
    // Audit fields
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

---

## Design Patterns

### 1. Dependency Injection (Constructor Injection)

All dependencies are injected via Spring's constructor injection (preferred over `@Autowired` field injection).

**Benefits:**
- Immutable dependencies (final fields)
- Easy to mock for testing
- Explicit dependencies (clear what a class needs)
- Spring automatically detects constructor and injects dependencies

**Example:**
```java
@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    
    // Spring automatically injects dependencies
    public TaskService(TaskRepository taskRepository, 
                       UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }
}
```

---

### 2. Repository Pattern

Data access is abstracted behind repository interfaces. Services interact with repositories, not directly with databases.

**Benefits:**
- Switch database implementations without changing business logic
- Centralize query logic in one place
- Testable with mock repositories (Mockito)
- Hide ORM complexity from services

**Example:**
```java
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Spring Data JPA generates implementation at runtime
    List<Task> findByAssigneeId(Long assigneeId);
    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);
}
```

---

### 3. Service Layer Pattern

Business logic is isolated in service classes, separate from HTTP concerns (controllers) and database concerns (repositories).

**Benefits:**
- Reusable logic (can be called from REST API, CLI, scheduled jobs, GraphQL)
- Easier to test (mock repositories, no HTTP context needed)
- Clear separation of concerns (HTTP → Service → Database)
- Transaction management with `@Transactional`

**Example:**
```java
@Service
@Transactional
public class TaskService {
    public TaskResponse createTask(CreateTaskRequest request) {
        // Business logic here (validation, orchestration)
        // Calls repositories, publishes events
        // Returns DTO, not entity
    }
}
```

---

### 4. DTO (Data Transfer Object) Pattern

APIs communicate via DTOs, not entities. This decouples API contracts from database schema.

**Benefits:**
- API can change without affecting database schema (versioning)
- Hide internal entity structure from clients (security)
- Validate input before processing (`@Valid`, `@NotBlank`, `@Size`)
- Support multiple representations (e.g., TaskSummaryResponse vs TaskDetailResponse)

**Example:**
```java
// Request DTO with validation
@Data
public class CreateTaskRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;
    
    @Size(max = 2000)
    private String description;
    
    @NotNull
    private TaskPriority priority;
    
    @NotNull
    @Min(1)
    private Long projectId;
    
    @NotEmpty(message = "At least one assignee is required")
    private List<Long> assigneeIds;
}

// Response DTO
@Data
public class TaskResponse {
    private Long id;
    private String title;
    private TaskStatus status;
    private TaskPriority priority;
    private ProjectResponse project;
    private List<UserResponse> assignees;
    private LocalDateTime createdAt;
    
    public static TaskResponse fromEntity(Task task) {
        // Map entity → DTO
    }
}
```

---

### 5. Event-Driven Architecture (Planned)

Services publish domain events that trigger side effects asynchronously. This decouples services and enables scalable event processing.

**Benefits:**
- Loose coupling between services (services don't directly call each other)
- Asynchronous operations (notifications, metrics updates)
- Scalable event processing (can later integrate RabbitMQ, Kafka)
- Audit trail (all events logged)

**Example (Planned for v0.9.0):**
```java
@Service
public class TaskService {
    private final ApplicationEventPublisher eventPublisher;
    
    public TaskResponse createTask(CreateTaskRequest request) {
        Task task = // create task
        taskRepository.save(task);
        
        // Publish domain event
        eventPublisher.publishEvent(new TaskCreatedEvent(task));
        
        return TaskResponse.fromEntity(task);
    }
}

@Component
public class NotificationEventListener {
    @EventListener
    @Async
    public void onTaskCreated(TaskCreatedEvent event) {
        // Send notification to assignees
        notificationService.sendTaskAssignedEmail(event.getTask());
    }
}
```

See [event/package-info.java](../src/main/java/com/taskmanagement/event/package-info.java) for detailed plan.

---

### 6. Layered Architecture with Dependency Inversion

Dependencies flow **inward**. Outer layers depend on inner layers, never the reverse. This is achieved via **interfaces**.

```
Controllers (HTTP)
    ↓ depends on
Services (Business Logic)
    ↓ depends on
Repositories (Interfaces)  ← Services depend on interfaces, not implementations
    ↓ implements
JPA Repositories (Spring Data generated)
    ↓ depends on
Entities (Domain Models)
```

**Benefits:**
- Testable in isolation (mock interfaces)
- Easy to replace implementations (PostgreSQL → MongoDB)
- Clear data flow (no circular dependencies)

---

## Package Organization

```
com.taskmanagement
│
├── annotation/              # Custom annotations
│   └── Planned.java         # @Planned(version="v0.9.0", description="...")
│
├── api/                     # Layer 1: REST Controllers
│   ├── TaskController.java
│   ├── UserController.java
│   └── ProjectController.java
│
├── service/                 # Layer 2: Business Logic
│   ├── TaskService.java
│   ├── UserService.java
│   └── ProjectService.java
│
├── repository/              # Layer 3: Data Access Interfaces
│   ├── TaskRepository.java
│   ├── UserRepository.java
│   ├── ProjectRepository.java
│   └── CommentRepository.java
│
├── entity/                  # Layer 4: Domain Models
│   ├── Task.java
│   ├── User.java
│   ├── Project.java
│   ├── Comment.java
│   ├── Attachment.java
│   ├── TaskStatus.java      # Enum
│   └── TaskPriority.java    # Enum
│
├── dto/                     # Data Transfer Objects
│   ├── request/
│   │   ├── CreateTaskRequest.java
│   │   ├── UpdateTaskRequest.java
│   │   ├── CreateUserRequest.java
│   │   ├── UpdateUserRequest.java
│   │   ├── CreateProjectRequest.java
│   │   └── UpdateProjectRequest.java
│   └── response/
│       ├── TaskResponse.java
│       ├── UserResponse.java
│       └── ProjectResponse.java
│
├── exception/               # Error Handling
│   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   ├── ErrorResponse.java
│   ├── TaskNotFoundException.java
│   ├── UserNotFoundException.java
│   ├── ProjectNotFoundException.java
│   ├── BusinessRuleException.java
│   └── DuplicateResourceException.java
│
├── config/                  # Configuration
│   └── SecurityConfig.java  # Spring Security configuration
│
├── security/                # 📋 Planned (v1.0.0)
│   └── package-info.java    # JWT authentication plan
│
├── util/                    # 📋 Planned (v0.8.0)
│   └── package-info.java    # Utilities plan
│
├── event/                   # 📋 Planned (v0.9.0)
│   └── package-info.java    # Event-driven architecture plan
│
└── TaskManagementApplication.java  # Main entry point
```

### Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Controllers | `XyzController` | `TaskController` |
| Services | `XyzService` | `TaskService` |
| Repositories | `XyzRepository` | `TaskRepository` |
| Entities | `Xyz` | `Task`, `User`, `Project` |
| Request DTOs | `CreateXyzRequest`, `UpdateXyzRequest` | `CreateTaskRequest` |
| Response DTOs | `XyzResponse` | `TaskResponse` |
| Exceptions | `XyzException` | `TaskNotFoundException` |
| Events (planned) | `XyzEvent` | `TaskCreatedEvent` |
| Listeners (planned) | `XyzListener` | `NotificationListener` |
| Config | `XyzConfig` | `SecurityConfig` |

---

## Dependency Rules

### ✅ Allowed Dependencies (Inward Flow)

```
Controllers → Services → Repositories → Entities
    ↓            ↓            ↓
  DTOs       DTOs, Events   (none)
```

- ✅ Controllers can depend on Services and DTOs
- ✅ Services can depend on Repositories, DTOs, Events, Utilities
- ✅ Repositories can depend on Entities only
- ✅ Entities depend on nothing (pure POJOs with JPA annotations)

### ❌ Forbidden Dependencies (Outward Flow)

- ❌ Entities cannot depend on Repositories
- ❌ Repositories cannot depend on Services
- ❌ Services cannot depend on Controllers
- ❌ Services cannot import Spring Web annotations (`@RestController`, `@RequestMapping`)
- ❌ Controllers cannot import JPA/Hibernate classes (`EntityManager`, `Session`)

### Enforcing Dependency Rules

Use **ArchUnit** (optional) to enforce architecture rules in tests:

```java
@Test
public void servicesShouldNotDependOnControllers() {
    classes()
        .that().resideInAPackage("..service..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage("..service..", "..repository..", "..entity..", "..dto..")
        .check(importedClasses);
}
```

---

## Cross-Cutting Concerns

### Exception Handling

**Location:** `com.taskmanagement.exception`

**Strategy:** Centralized exception handling with `@RestControllerAdvice`.

**Components:**
- `GlobalExceptionHandler` - Catches all exceptions, returns consistent `ErrorResponse`
- Custom exceptions inherit from `RuntimeException`

**Error Response Format:**
```json
{
  "code": "ENTITY_NOT_FOUND",
  "message": "Task with id=123 not found",
  "timestamp": 1701389400000,
  "path": "/api/tasks/123"
}
```

**Example:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFound(TaskNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            "ENTITY_NOT_FOUND",
            ex.getMessage(),
            System.currentTimeMillis(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

See [API.md](API.md#error-handling) for all error codes.

---

### Security (Basic Auth → JWT)

**Current (v0.7.0):** Basic Authentication with hardcoded users (`admin:admin`, `user:user123`)

**Planned (v1.0.0):** JWT-based authentication with role-based authorization

**Location:** 
- Current: `com.taskmanagement.config.SecurityConfig`
- Planned: `com.taskmanagement.security.*`

**Migration Plan:**
1. Add JWT dependencies (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
2. Create `JwtTokenProvider` for token generation/validation
3. Create `JwtAuthenticationFilter` to intercept requests
4. Add `AuthController` with `/api/auth/login` and `/api/auth/refresh` endpoints
5. Update `SecurityConfig` to use JWT filter chain
6. Add `roles` field to User entity
7. Implement RBAC with `@PreAuthorize("hasRole('ADMIN')")`

See [security/package-info.java](../src/main/java/com/taskmanagement/security/package-info.java) for detailed plan.

---

### Configuration Management

**Location:** `src/main/resources/application.yml`

**Profiles:**
- `default` - Local development with H2/PostgreSQL
- `test` - Test environment with TestContainers
- `prod` - Production with external PostgreSQL

**Example:**
```yaml
spring:
  profiles:
    active: default
  datasource:
    url: jdbc:postgresql://localhost:5432/task_db
    username: postgres
    password: ${DB_PASSWORD}  # Environment variable
  jpa:
    hibernate:
      ddl-auto: update  # validate in production
    show-sql: true
```

See [APPLICATION_YML_CONFIGURATION.md](../src/main/resources/APPLICATION_YML_CONFIGURATION.md) for detailed configuration options.

---

## Future Architecture Plans

### v0.8.0 - Utility Classes

**Package:** `com.taskmanagement.util`

**Planned Components:**
- `DateTimeUtils` - Date/time operations (isOverdue, formatDuration)
- `ValidationUtils` - Custom validation helpers
- `StringUtils` - String manipulation (truncate, slugify)
- `JsonUtils` - JSON serialization helpers
- `CollectionUtils` - Collection operations

See [util/package-info.java](../src/main/java/com/taskmanagement/util/package-info.java).

---

### v0.9.0 - Event-Driven Architecture

**Package:** `com.taskmanagement.event`

**Planned Components:**
- **Domain Events:**
  - `TaskCreatedEvent` - Published when task is created
  - `TaskStatusChangedEvent` - Published when status changes
  - `TaskAssignedEvent` - Published when assignees change
  - `ProjectCreatedEvent` - Published when project is created

- **Event Handlers:**
  - `NotificationEventHandler` - Sends email/push notifications
  - `MetricsEventHandler` - Updates real-time metrics
  - `AuditEventHandler` - Logs all system events

**Benefits:**
- Decouple notification logic from task creation
- Async processing for better performance
- Prepare for microservices migration

See [event/package-info.java](../src/main/java/com/taskmanagement/event/package-info.java).

---

### v1.0.0 - JWT Authentication & RBAC

**Package:** `com.taskmanagement.security`

**Planned Components:**
- `JwtTokenProvider` - Token generation/validation
- `JwtAuthenticationFilter` - Request interceptor
- `CustomUserDetailsService` - Load users from database
- `RoleBasedAuthorizationManager` - Custom authorization logic

**Roles:**
- `PROJECT_MANAGER` - Create/delete projects and tasks
- `TEAM_LEAD` - Manage tasks in assigned projects
- `DEVELOPER` - Update task status, add comments
- `VIEWER` - Read-only access

See [security/package-info.java](../src/main/java/com/taskmanagement/security/package-info.java).

---

## Architectural Decision Records (ADRs)

### ADR-001: Why Clean Architecture?

**Decision:** Use Clean Architecture with 4 layers (Controllers → Services → Repositories → Entities)

**Reasoning:**
- **Testability** - Each layer can be tested independently
- **Maintainability** - Clear separation of concerns
- **Scalability** - Easy to add new features without breaking existing code
- **Technology Independence** - Can swap Spring Boot for Quarkus, PostgreSQL for MongoDB

**Trade-offs:**
- More boilerplate code (DTOs, mappers)
- Learning curve for junior developers
- Slower initial development (faster in long run)

---

### ADR-002: Why DTOs instead of exposing Entities?

**Decision:** Use separate DTOs for request/response instead of exposing JPA entities directly

**Reasoning:**
- **Security** - Hide sensitive fields (passwordHash, internal IDs)
- **Versioning** - API can evolve independently of database schema
- **Validation** - Enforce input validation with `@Valid` annotations
- **Flexibility** - Different representations (summary vs detail)

**Trade-offs:**
- More classes to maintain (entity + DTO)
- Mapping overhead (entity ↔ DTO conversion)

---

### ADR-003: Why Native Queries for Task Assignees?

**Decision:** Use native SQL queries to load Task assignees instead of Hibernate's lazy loading

**Reasoning:**
- Hibernate 6.x `@Where(clause = "deleted = false")` breaks Many-to-Many lazy loading
- Native queries bypass Hibernate filters and work reliably
- Workaround until migration to `@FilterDef`

**Trade-offs:**
- Less portable (SQL dialect specific)
- More manual mapping code
- Loses some Hibernate magic

See [KNOWN_ISSUES.md](KNOWN_ISSUES.md#hibernate-where-manytomany-issue) for detailed analysis.

---

## Related Documentation

- [API.md](API.md) - REST API endpoints and examples
- [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) - Entity relationships and schema
- [KNOWN_ISSUES.md](KNOWN_ISSUES.md) - Current bugs and workarounds
- [FRONTEND_ARCHITECTURE.md](FRONTEND_ARCHITECTURE.md) - React component architecture

---

**Last Updated:** January 4, 2026  
**Version:** v0.7.0  
**Author:** Task Management Team
