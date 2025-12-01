# Task Management System - Architecture Deep Dive

##  Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Clean Architecture Principles](#clean-architecture-principles)
3. [Layered Architecture](#layered-architecture)
4. [Data Flow & Request Lifecycle](#data-flow--request-lifecycle)
5. [Domain-Driven Design](#domain-driven-design)
6. [Security Architecture](#security-architecture)
7. [Event-Driven Design](#event-driven-design)
8. [Scalability & Performance](#scalability--performance)
9. [Database Design](#database-design)
10. [API Design](#api-design)
11. [Testing Strategy](#testing-strategy)
12. [Deployment Architecture](#deployment-architecture)

---

## Architecture Overview

The Task Management System follows **Clean Architecture** principles combined with **Domain-Driven Design (DDD)** and **Event-Driven Architecture** patterns.

### Core Architectural Goals
 **Separation of Concerns** - Each layer has a single responsibility  
 **Testability** - Components can be tested in isolation  
 **Maintainability** - Clear code organization, easy to understand and modify  
 **Scalability** - Stateless design, horizontal scaling, event-driven operations  
 **Security** - JWT-based authentication, RBAC, data validation  
 **Performance** - Connection pooling, query optimization, caching-ready  

### Architecture Diagram

```

                    PRESENTATION LAYER (HTTP)                   
                   Controllers & Error Handling                  
  (TaskController, UserController, AuthController, etc.)        

                              

                   BUSINESS LOGIC LAYER                          
              Services & Domain Logic Implementation             
  (TaskService, UserService, NotificationService, etc.)         

                              

                  DATA ACCESS LAYER (Persistence)                
                   Repository Interfaces (JPA)                   
  (TaskRepository, UserRepository, ProjectRepository, etc.)      

                              

                  DOMAIN LAYER (Core Business Models)            
                    JPA Entity Classes                            
  (Task, User, Project, Comment, Attachment, AuditLog, etc.)    

                              

            EXTERNAL LAYER (Database, Message Broker)            
          PostgreSQL | RabbitMQ | Redis | Kafka                  

```

---

## Clean Architecture Principles

Clean Architecture organizes code into concentric circles, with **business logic at the center** and **infrastructure/external dependencies at the edges**.

### The Dependency Rule
> **Dependencies always flow inward, never outward**

```

         External Frameworks             
   (Spring, JPA, REST, Database)         

                   

      Interface Adapters Layer           
  (Controllers, Presenters, Gateways)    

                   

    Application Business Logic Layer     
     (Use Cases / Services)              

                   

        Domain / Enterprise Logic        
  (Entities, Value Objects, Aggregates)  

```

### Key Principles

#### 1. Entities at the Center
- **Location:** `entity/` package
- **Responsibility:** Represent core business concepts
- **Independence:** No dependencies on frameworks or layers
- **Examples:** Task, User, Project, Comment

```java
// Entity: Pure business concept, no framework dependencies
public class Task {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDateTime dueDate;
    // Methods represent business operations
    public void assign(User user) { ... }
    public void complete() { ... }
    public boolean isOverdue() { ... }
}
```

#### 2. Repositories Abstract Data Access
- **Location:** `repository/` package
- **Responsibility:** Hide database implementation details
- **Benefit:** Swap databases without changing services
- **Pattern:** Repository pattern with Spring Data JPA

```java
// Repository: Abstracts data persistence
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByAssigneeAndDueDateBefore(User assignee, LocalDate date);
}
```

#### 3. Services Implement Use Cases
- **Location:** `service/` package
- **Responsibility:** Coordinate entities and repositories to implement business logic
- **Independence:** Services don't know about HTTP, they're testable without controllers

```java
// Service: Business logic orchestration
@Service
public class TaskService {
    public Task createTask(CreateTaskRequest request) {
        // Validate business rules
        // Create entity
        // Persist via repository
        // Publish domain events
        // Return result
    }
}
```

#### 4. Controllers Adapt External Input
- **Location:** `api/` package
- **Responsibility:** Convert HTTP requests to service calls, return HTTP responses
- **Independence:** Thin layer, business logic in services

```java
// Controller: HTTP adapter
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        Task task = taskService.createTask(request);
        return ResponseEntity.status(201).body(TaskResponse.from(task));
    }
}
```

#### 5. DTOs Bridge API & Domain
- **Location:** `dto/` package
- **Responsibility:** Define API contracts separate from domain entities
- **Benefit:** API can change without affecting database schema

```java
// DTO: API contract
@Data
public class CreateTaskRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String description;
    @NotNull
    @FutureOrPresent
    private LocalDateTime dueDate;
}

// DTO: Response format
@Data
public class TaskResponse {
    private Long id;
    private String title;
    private TaskStatus status;
}
```

---

## Layered Architecture

### Layer 1: Presentation Layer (Controllers)

**Purpose:** Handle HTTP requests and responses

**Responsibilities:**
- Parse HTTP requests
- Validate path/query parameters
- Call appropriate services
- Convert domain objects to DTOs
- Return HTTP responses with proper status codes
- Handle CORS, content negotiation

**Technology Stack:**
- Spring Web (`@RestController`, `@RequestMapping`, etc.)
- Jackson (JSON serialization)
- Bean Validation (`@Valid`, `@Validated`)

**Example:**
```java
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request) {
        Task task = taskService.createTask(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(TaskResponse.from(task));
    }
}
```

**Key Patterns:**
- Dependency injection of services
- Use DTOs for request/response
- Use `@PreAuthorize` for RBAC
- Return appropriate HTTP status codes
- Let services handle business validation

---

### Layer 2: Application/Business Logic Layer (Services)

**Purpose:** Implement use cases and business logic

**Responsibilities:**
- Orchestrate repositories and entities
- Implement business rules and validation
- Coordinate with other services
- Manage transactions
- Publish domain events
- Handle cross-cutting concerns

**Technology Stack:**
- Spring Framework (`@Service`, `@Transactional`)
- Domain entities
- Event publishing

**Example:**
```java
@Service
@Transactional
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    public Task createTask(CreateTaskRequest request) {
        // 1. Validate business rules
        User assignee = userRepository.findById(request.getAssigneeId())
            .orElseThrow(() -> new UserNotFoundException());
        
        if (assignee.getTaskCount() >= MAX_TASKS_PER_USER) {
            throw new TaskAssignmentLimitExceededException();
        }
        
        // 2. Create entity
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setAssignee(assignee);
        task.setStatus(TaskStatus.PENDING);
        
        // 3. Persist
        Task savedTask = taskRepository.save(task);
        
        // 4. Publish event (side effects happen asynchronously)
        eventPublisher.publishEvent(new TaskCreatedEvent(savedTask));
        
        // 5. Return result
        return savedTask;
    }
}
```

**Key Patterns:**
- Constructor injection for dependencies
- `@Transactional` for data consistency
- Business validation before operations
- Event publishing for loose coupling
- Throw domain-specific exceptions

---

### Layer 3: Data Access Layer (Repositories)

**Purpose:** Abstract database operations

**Responsibilities:**
- Provide CRUD operations
- Implement custom queries
- Hide SQL/database details
- Enable transaction management
- Support pagination and sorting

**Technology Stack:**
- Spring Data JPA
- JPA Query Language (JPQL)
- Native SQL (when needed)

**Example:**
```java
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // JPQL-like queries auto-generated from method names
    List<Task> findByStatusAndAssignee(TaskStatus status, User assignee);
    
    // Custom query
    @Query("""
        SELECT t FROM Task t 
        WHERE t.dueDate < CURRENT_DATE 
        AND t.status != 'COMPLETED'
    """)
    List<Task> findOverdueTasks();
    
    // Native SQL (when needed)
    @Query(value = """
        SELECT * FROM tasks 
        WHERE project_id = ? AND status = ?
        ORDER BY due_date ASC
    """, nativeQuery = true)
    List<Task> findTasksByProjectAndStatus(Long projectId, String status);
}
```

**Key Patterns:**
- Extend `JpaRepository<Entity, ID>`
- Declarative queries from method names
- `@Query` for complex queries
- Use native SQL sparingly
- Return entities, not DTOs

---

### Layer 4: Domain Layer (Entities)

**Purpose:** Represent core business concepts

**Responsibilities:**
- Define database table structure
- Enforce business constraints
- Implement business logic
- Define relationships
- Track changes (auditing)

**Technology Stack:**
- JPA/Hibernate annotations
- Lombok (reduce boilerplate)
- Jakarta Bean Validation

**Example:**
```java
@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(length = 255)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.PENDING;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;
    
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();
    
    // Audit fields
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Business logic methods
    public void markAsCompleted() {
        this.status = TaskStatus.COMPLETED;
    }
    
    public boolean isOverdue() {
        return status != TaskStatus.COMPLETED 
            && LocalDateTime.now().isAfter(dueDate);
    }
}
```

**Key Patterns:**
- JPA annotations for mapping
- Lombok for boilerplate reduction
- Validation annotations
- Business logic methods
- Proper fetch strategies (LAZY by default)
- Cascade and orphan removal for relationships

---

## Data Flow & Request Lifecycle

### Request Flow Diagram

```
1. HTTP Request
        
2. JwtAuthenticationFilter
   - Extract token from Authorization header
   - Validate token signature and expiration
   - Extract userId and roles
   - Set SecurityContext
        
3. DispatcherServlet
   - Route to appropriate controller
        
4. Controller
   - Validate @Valid request parameters
   - Call service method
   - Convert response to DTO
   - Return HTTP response
        
5. Service
   - Validate business rules
   - Load entities from repository
   - Modify entities
   - Call repository.save()
   - Publish domain events
   - Return domain entity
        
6. Repository
   - Save/update/delete via JPA
   - Hibernate generates SQL
   - Execute against PostgreSQL
   - Return persisted entity
        
7. Event Listener (Async)
   - Listen for published events
   - Execute side effects (notifications, etc.)
   - Don't block main request
        
8. HTTP Response
   - 201 Created (for POST)
   - 200 OK (for GET/PUT)
   - JSON response body
```

### Example Request: Create Task

```
POST /api/tasks HTTP/1.1
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "title": "Implement user authentication",
  "description": "Add JWT-based authentication",
  "projectId": 1,
  "assigneeId": 5,
  "dueDate": "2025-12-15T17:00:00"
}
```

**Step-by-Step Processing:**

1. **JwtAuthenticationFilter** extracts token
   - Calls `JwtTokenProvider.validateToken()`
   - Extracts `userId` and `roles` claims
   - Creates `UsernamePasswordAuthenticationToken`
   - Sets in `SecurityContextHolder`

2. **TaskController.createTask()** receives request
   - `@Valid` triggers validation on `CreateTaskRequest`
   - If validation fails: 400 Bad Request with error details
   - Calls `taskService.createTask(request)`

3. **TaskService.createTask()** executes
   - Validates `assigneeId` exists
   - Validates assignee doesn't exceed task limit
   - Creates new `Task` entity
   - Sets properties: title, description, project, assignee, status=PENDING
   - Calls `taskRepository.save(task)`

4. **TaskRepository.save()** persists to database
   - Hibernate generates INSERT SQL
   - Executes against PostgreSQL
   - Database triggers set `createdAt` timestamp
   - Generates task `id` (auto-increment)
   - Returns saved entity

5. **TaskService** publishes event
   - `applicationEventPublisher.publishEvent(new TaskCreatedEvent(task))`
   - Event is published asynchronously (non-blocking)

6. **TaskEventListener** listens for event (async)
   - Sends notification to project members
   - Updates activity feed
   - Records audit log entry
   - Doesn't block the HTTP response

7. **TaskController** converts to DTO
   - `TaskResponse.from(task)`
   - Returns `ResponseEntity.status(201).body(response)`

8. **HTTP Response**
   ```json
   HTTP/1.1 201 Created
   Content-Type: application/json

   {
     "id": 123,
     "title": "Implement user authentication",
     "status": "PENDING",
     "assignee": {
       "id": 5,
       "name": "John Doe"
     },
     "createdAt": "2025-12-01T10:30:00"
   }
   ```

---

## Domain-Driven Design

### Domain Model Organization

**Entities:** Objects with identity
- `User` - Identified by userId
- `Task` - Identified by taskId
- `Project` - Identified by projectId

**Value Objects:** Objects without identity, immutable
```java
public record TaskPriority(String level) {
    public TaskPriority {
        if (!List.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(level)) {
            throw new IllegalArgumentException("Invalid priority");
        }
    }
}
```

**Aggregates:** Clusters of objects treated as units
```
Aggregate Root: Task
   Comments (children)
   Attachments (children)

When Task is deleted:
   All Comments deleted (cascade)
   All Attachments deleted (cascade)
```

**Repositories:** Persist aggregates
```java
// Repositories manage aggregates, not individual objects
TaskRepository.save(task);  // Saves task + comments + attachments
```

**Domain Events:** Represent something that happened
```java
public record TaskCreatedEvent(Long taskId, String title, LocalDateTime createdAt) 
    implements ApplicationEvent { }
```

### Bounded Contexts (Future)

As the system grows, divide into bounded contexts:

```

     Task Management Bounded Context     
  (Task, TaskAssignment, Comment, etc.)  



      User Management Bounded Context    
   (User, Role, Permission, etc.)        



    Notification Bounded Context         
  (Notification, Template, Delivery)     

```

---

## Security Architecture

### Authentication Flow

```
1. User Login
   POST /api/auth/login
   { "username": "user@example.com", "password": "secret" }
        
2. AuthController.login()
   - Call AuthService.authenticate(username, password)
        
3. AuthService.authenticate()
   - Load User from UserRepository
   - Compare password using PasswordEncoder
   - If match: create JWT token
   - Return token and expiration
        
4. JwtTokenProvider.generateToken()
   - Create token claims: userId, roles
   - Sign with secret key
   - Return compact token string
        
5. HTTP Response
   {
     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "expiresIn": 86400000  // 24 hours in milliseconds
   }
        
6. Client stores token (localStorage, sessionStorage, etc.)
```

### Authorization Flow

```
1. Client sends request with token
   GET /api/tasks
   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
        
2. JwtAuthenticationFilter
   - Extract token from Authorization header
   - Call JwtTokenProvider.validateToken(token)
   - If valid: extract userId, roles
   - Create Authentication object
   - Set in SecurityContextHolder
        
3. DispatcherServlet routes to controller
        
4. Controller has @PreAuthorize("hasRole('MANAGER')")
   - Spring checks Authentication.getAuthorities()
   - If role matches: execute controller method
   - If role doesn't match: return 403 Forbidden
        
5. Method execution proceeds
```

### Password Security

```
User Registration:
1. Plain password received: "MySecurePass123"
2. BCryptPasswordEncoder.encode() hash it
   - Random salt generated
   - Hash with 10 rounds (configurable)
   - Result: .K7R9VfB2XtZvVmQhQR8J9ZnV0Rm1G0a
3. Store hash in database
4. Never store plain password

Authentication:
1. User provides: "MySecurePass123"
2. BCryptPasswordEncoder.matches(plain, hash)
   - Verify hash matches plain text
   - Return true if match, false if not
3. Never compare plain passwords
```

### JWT Token Structure

```
Header.Payload.Signature

Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "userId": "user123",
  "roles": "MANAGER,TEAM_LEAD",
  "iat": 1701389400,     // issued at
  "exp": 1701475800      // expires at (24 hours later)
}

Signature: HMACSHA256(Header.Payload, secret_key)
```

### RBAC (Role-Based Access Control)

```
Roles:
 ADMIN
    Permissions: All (manage users, change system settings)
 MANAGER
    Permissions: Create/assign tasks, view team performance
 TEAM_LEAD
    Permissions: Monitor team progress, delegate tasks
 TEAM_MEMBER
    Permissions: View/update assigned tasks, add comments
 VIEWER
     Permissions: Read-only access to tasks and reports

Implementation via @PreAuthorize:
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long userId) { }

@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
public Task createTask(CreateTaskRequest request) { }

@PreAuthorize("hasAuthority('TASK_EDIT')")
public Task updateTask(Long taskId, UpdateTaskRequest request) { }
```

---

## Event-Driven Design

### Publish-Subscribe Pattern

```
Service publishes event
        
ApplicationEventPublisher.publishEvent(event)
        
Spring invokes all @EventListener methods asynchronously
        
Multiple listeners can react without blocking
```

### Example: Task Creation Event

```java
@Service
public class TaskService {
    private final ApplicationEventPublisher eventPublisher;
    
    public Task createTask(CreateTaskRequest request) {
        // Create and save task
        Task task = taskRepository.save(newTask);
        
        // Publish event
        eventPublisher.publishEvent(new TaskCreatedEvent(task));
        
        // Return immediately (events processed async)
        return task;
    }
}

// Multiple independent listeners
@Component
public class NotificationListener {
    @EventListener(TaskCreatedEvent.class)
    public void onTaskCreated(TaskCreatedEvent event) {
        // Send notifications to project members
    }
}

@Component
public class AuditListener {
    @EventListener(TaskCreatedEvent.class)
    public void onTaskCreated(TaskCreatedEvent event) {
        // Log audit entry
    }
}

@Component
public class MetricsListener {
    @EventListener(TaskCreatedEvent.class)
    public void onTaskCreated(TaskCreatedEvent event) {
        // Update metrics/analytics
    }
}
```

### Benefits

 **Loose Coupling** - Services don't know about side effects  
 **Scalability** - Can add new listeners without modifying services  
 **Async Processing** - Side effects don't block main request  
 **Testability** - Can test service without listeners  
 **Future Message Queue** - Can easily switch to RabbitMQ/Kafka  

---

## Scalability & Performance

### Stateless Design

```
Request 1  Server A 
Request 2  Server B   (doesn't need Session from Server A)
Request 3  Server C   (completely independent)

No session affinity needed  Can scale horizontally
Load balancer can route to any server
```

### Connection Pooling

```
HikariCP Configuration:
- maximum-pool-size: 20 (max concurrent connections)
- minimum-idle: 5 (keep ready)
- connection-timeout: 30s (wait max)
- idle-timeout: 10min (close if unused)
- max-lifetime: 30min (connection max life)

Benefits:
- Reuse connections (expensive to create)
- Limit total connections to database
- Prevent resource exhaustion
```

### Query Optimization

```
N+1 Query Problem (BAD):
1. SELECT * FROM tasks (1 query)
2. For each task: SELECT * FROM users WHERE id = ? (N queries)
Total: 1 + N = 1001 queries for 1000 tasks!

Solution - Eager Loading (GOOD):
SELECT t FROM Task t JOIN FETCH t.assignee
1 query, all data loaded

Or use @EntityGraph:
@EntityGraph(attributePaths = {"assignee", "project"})
List<Task> findAll();
```

### Lazy Loading vs Eager Loading

```
Lazy Loading (Default):
@ManyToOne(fetch = FetchType.LAZY)
private User assignee;

When task is loaded, assignee is NOT loaded
When task.getAssignee() is called, separate query executes
Pro: Don't load unnecessary data
Con: Potential N+1 problem

Eager Loading:
@ManyToOne(fetch = FetchType.EAGER)
private User assignee;

When task is loaded, assignee is also loaded
Prevents N+1 but loads data that might not be needed

Best Practice:
- Default: LAZY
- Use JOIN FETCH in queries when needed
- Explicitly fetch relationships
```

### Caching Strategy (Phase 2+)

```
Application Cache (Redis):
@Cacheable("users")
public User getUserById(Long id) { }

Database Cache:
Query results cached by PostgreSQL

Browser Cache:
HTTP Cache-Control headers

Layered Caching:
Browser  CDN  App Cache  Database Cache  Database
```

### Rate Limiting (Phase 2+)

```
@RateLimit(requests = 100, per = "MINUTE")
@GetMapping("/api/tasks")
public List<TaskResponse> getTasks() { }

Benefits:
- Prevent abuse
- Fair resource allocation
- Graceful degradation
```

---

## Database Design

### Entity Relationship Diagram

```
Users  Tasks
  1          *
            
        Assignees
            
        Comments (1:*)

Users  Projects
  1         (*:*)          *
  
Projects  Tasks
  1                        *

Tasks  Comments
  1                        *

Tasks  Attachments
  1                        *
```

### Key Database Features

**Indexes:**
```sql
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_assignee_id ON tasks(assignee_id);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_users_email ON users(email);
```

**Constraints:**
```sql
ALTER TABLE tasks ADD CONSTRAINT fk_assignee FOREIGN KEY (assignee_id) REFERENCES users(id);
ALTER TABLE tasks ADD CONSTRAINT check_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'ON_HOLD', 'CANCELLED'));
ALTER TABLE users ADD CONSTRAINT unique_email UNIQUE (email);
```

**Migrations (Flyway):**
```
src/main/resources/db/migration/
 V1__initial_schema.sql       # Create initial tables
 V2__add_audit_columns.sql    # Add audit fields
 V3__create_indexes.sql       # Performance indexes
 V4__add_constraints.sql      # Add constraints
```

---

## API Design

### RESTful Endpoints

```
GET     /api/tasks              # List all tasks (paginated)
POST    /api/tasks              # Create new task
GET     /api/tasks/{id}         # Get task details
PUT     /api/tasks/{id}         # Update task
DELETE  /api/tasks/{id}         # Delete task
PATCH   /api/tasks/{id}/status  # Update task status only

GET     /api/users              # List users
POST    /api/users              # Create user (admin only)
GET     /api/users/{id}         # Get user profile
PUT     /api/users/{id}         # Update user

POST    /api/auth/login         # Authenticate
POST    /api/auth/register      # Register new user
POST    /api/auth/refresh       # Refresh token
POST    /api/auth/logout        # Logout (optional)

GET     /api/projects           # List projects
POST    /api/projects           # Create project
GET     /api/projects/{id}      # Get project details
PUT     /api/projects/{id}      # Update project
```

### Response Format

```json
// Success Response (200)
{
  "id": 123,
  "title": "Implement authentication",
  "status": "IN_PROGRESS",
  "assignee": {
    "id": 5,
    "name": "John Doe"
  },
  "createdAt": "2025-12-01T10:30:00"
}

// List Response (with pagination)
{
  "content": [
    { "id": 1, "title": "Task 1" },
    { "id": 2, "title": "Task 2" }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}

// Error Response (400, 401, 403, 404, 500)
{
  "code": "VALIDATION_ERROR",
  "message": "Task title is required",
  "timestamp": 1701389400000,
  "path": "/api/tasks"
}
```

---

## Testing Strategy

### Unit Tests

```java
@SpringBootTest
class TaskServiceTest {
    @MockBean
    private TaskRepository taskRepository;
    
    @InjectMocks
    private TaskService taskService;
    
    @Test
    void createTask_WithValidData_ReturnsTask() {
        // Arrange
        CreateTaskRequest request = new CreateTaskRequest(...);
        Task expectedTask = new Task(...);
        
        // Act
        Task result = taskService.createTask(request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Testcontainers
class TaskRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Test
    void findByStatus_WithValidStatus_ReturnsTasks() {
        // Uses real PostgreSQL container
        List<Task> tasks = taskRepository.findByStatus(TaskStatus.PENDING);
        assertThat(tasks).isNotEmpty();
    }
}
```

### API Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void createTask_WithValidRequest_Returns201() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .contentType("application/json")
            .content("{\"title\": \"Test Task\"}")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());
    }
}
```

### Test Coverage Goals

- **Services:** 80%+ coverage
- **Controllers:** 70%+ coverage
- **Repositories:** 60%+ coverage
- **Entities:** 40%+ coverage (mostly getters/setters)

---

## Deployment Architecture

### Development Environment

```
Developer Workstation
 Java 17
 Maven
 PostgreSQL (local)
 IDE (IntelliJ, VS Code, etc.)
 Git

Build & Run:
mvn clean install
mvn spring-boot:run

Access:
http://localhost:8080/api
```

### Production Environment

```

           Load Balancer (ALB)           
      (Routes traffic to servers)        

                               
    
 App Server  App Srv   App Server 
 Instance1      2       Instance3 
    
                               
         
                     
         
           PostgreSQL Database  
            (Managed Service)   
         
         
         
           Redis Cache (opt.)   
         
         
         
           Message Broker (opt.)
           (RabbitMQ/Kafka)     
         
```

### Docker Containerization

```dockerfile
# Dockerfile
FROM openjdk:17-jdk-slim

COPY target/task-management-system-1.0.0-SNAPSHOT.jar app.jar

ENTRYPOINT ["java","-jar","/app.jar"]
```

```yaml
# docker-compose.yml
version: '3.9'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/task_management
      DATABASE_USER: postgres
      DATABASE_PASSWORD: postgres
    depends_on:
      - postgres
  
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: task_management
      POSTGRES_PASSWORD: postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

### Kubernetes Deployment (Optional Phase 2+)

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: task-management-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: task-management
  template:
    metadata:
      labels:
        app: task-management
    spec:
      containers:
      - name: app
        image: task-management-system:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: DATABASE_URL
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: database-url
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: database-password
        livenessProbe:
          httpGet:
            path: /api/actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

---

## Summary

The Task Management System architecture provides:

 **Clean Architecture** - Clear separation of concerns, easy to test and maintain  
 **Domain-Driven Design** - Business logic centered, ubiquitous language  
 **Layered Design** - Controllers  Services  Repositories  Entities  
 **Event-Driven** - Loose coupling, scalable event processing  
 **Secure** - JWT authentication, RBAC, password encryption  
 **Scalable** - Stateless design, connection pooling, query optimization  
 **Testable** - Unit, integration, API tests with clear patterns  
 **Production-Ready** - Docker, Kubernetes, monitoring, health checks  

This foundation supports growth from MVP to enterprise-scale application.

---

**Last Updated:** December 1, 2025  
**Version:** 1.0.0  
**Status:** Architecture Complete
