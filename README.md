# Task Management System - Project Structure & Clean Architecture

## 📋 Table of Contents
1. [Overview](#overview)
2. [Project Structure](#project-structure)
3. [Clean Architecture Layers](#clean-architecture-layers)
4. [Package Organization](#package-organization)
5. [Technology Stack](#technology-stack)
6. [Key Design Patterns](#key-design-patterns)
7. [Development Workflow](#development-workflow)
8. [Getting Started](#getting-started)

---

## Overview

The **Task Management System** is an enterprise-grade application built using **Spring Boot 3.2** and **Clean Architecture** principles. It provides centralized task management for teams with support for task assignment, project organization, and comprehensive API endpoints.

### Core Characteristics
- **Architecture:** Clean Architecture with clear separation of concerns
- **Framework:** Spring Boot 3.2 with Java 17
- **Database:** PostgreSQL with JPA/Hibernate ORM
- **Security:** Spring Security with Basic Authentication (JWT implementation ready for Phase 2)
- **API:** RESTful API with comprehensive CRUD operations
- **Current Features:** Task CRUD, Multi-User Assignment (N:N), Project-Task relationships
- **Status:** v0.6.0 - MVP Phase with Hibernate 6.x workarounds implemented
- **Monitoring:** Spring Boot Actuator for health checks

### Currently Implemented Features
✅ **Task Management:**
  - Create tasks with multiple assignees (Many-to-Many relationship)
  - Get task by ID with full details (assignees, project)
  - Update tasks (title, description, status, priority, assignees)
  - Delete tasks (hard delete with cascade to comments/attachments)
  
✅ **Relationships:**
  - Task → User (Many-to-Many via task_assignees junction table)
  - Task → Project (Many-to-One, project required)
  - Task → Comments (One-to-Many with cascade delete)
  - Task → Attachments (One-to-Many with cascade delete)

✅ **Database:**
  - PostgreSQL with proper indexes
  - JPA entities with validation
  - Lazy loading for performance

### Planned Features (Not Yet Implemented)
🔲 User Management API (GET /api/users)
🔲 Project Management API
🔲 Task filtering by assignee/project (GET /api/tasks?assigneeId=1)
🔲 JWT Authentication & Authorization
🔲 Remove assignee from task (allow null assignee)
🔲 Soft delete support
🔲 Task comments API
🔲 File attachments API
🔲 Event-driven notifications

---

## Project Structure

```
java_project/                                 # Project root
│
├── src/
│   ├── main/
│   │   ├── java/com/taskmanagement/          # Source code (Clean Architecture)
│   │   │   │
│   │   │   ├── api/                          # ✅ Layer 1: REST Controllers
│   │   │   │   ├── TaskController.java       # Task CRUD endpoints
│   │   │   │   └── README.md                 # API documentation
│   │   │   │
│   │   │   ├── service/                      # ✅ Layer 2: Business Logic
│   │   │   │   ├── TaskService.java          # Task business logic
│   │   │   │   └── README.md                 # Service layer documentation
│   │   │   │
│   │   │   ├── repository/                   # ✅ Layer 3: Data Access (JPA)
│   │   │   │   ├── TaskRepository.java       # Task queries
│   │   │   │   ├── UserRepository.java       # User validation
│   │   │   │   ├── ProjectRepository.java    # Project validation
│   │   │   │   └── README.md                 # Repository documentation
│   │   │   │
│   │   │   ├── entity/                       # ✅ Layer 4: Domain Models
│   │   │   │   ├── Task.java                 # Task entity with relationships
│   │   │   │   ├── User.java                 # User entity
│   │   │   │   ├── Project.java              # Project entity
│   │   │   │   ├── Comment.java              # Comment entity (cascade delete)
│   │   │   │   ├── Attachment.java           # Attachment entity (cascade delete)
│   │   │   │   ├── TaskStatus.java           # Status enum
│   │   │   │   ├── TaskPriority.java         # Priority enum
│   │   │   │   └── README.md                 # Entity documentation
│   │   │   │
│   │   │   ├── dto/                          # ✅ Data Transfer Objects
│   │   │   │   ├── request/
│   │   │   │   │   ├── CreateTaskRequest.java  # POST /api/tasks
│   │   │   │   │   └── UpdateTaskRequest.java  # PUT /api/tasks/{id}
│   │   │   │   ├── response/
│   │   │   │   │   └── TaskResponse.java       # Task response DTO
│   │   │   │   └── README.md                   # DTO documentation
│   │   │   │
│   │   │   ├── exception/                    # ✅ Error Handling
│   │   │   │   ├── GlobalExceptionHandler.java  # Centralized exception handling
│   │   │   │   ├── ErrorResponse.java           # Standard error format
│   │   │   │   ├── TaskNotFoundException.java   # 404 for tasks
│   │   │   │   ├── UserNotFoundException.java   # 404 for users
│   │   │   │   ├── ProjectNotFoundException.java # 404 for projects
│   │   │   │   └── README.md                    # Exception documentation
│   │   │   │
│   │   │   ├── config/                       # ✅ Configuration
│   │   │   │   ├── SecurityConfig.java       # Basic Auth (hardcoded users)
│   │   │   │   └── README.md                 # Security configuration docs
│   │   │   │
│   │   │   ├── security/                     # 🔲 Placeholder (JWT - not implemented)
│   │   │   │   └── README.md                 # JWT implementation plan
│   │   │   │
│   │   │   ├── util/                         # 🔲 Placeholder (no utilities yet)
│   │   │   │   └── README.md                 # Utility plan
│   │   │   │
│   │   │   ├── event/                        # 🔲 Placeholder (no events yet)
│   │   │   │   └── README.md                 # Event-driven architecture plan
│   │   │   │
│   │   │   └── TaskManagementApplication.java  # Main Spring Boot entry point
│   │   │
│   │   └── resources/
│   │       ├── application.yml               # Spring Boot configuration
│   │       └── APPLICATION_YML_CONFIGURATION.md  # Configuration guide
│   │
│   └── test/                                 # 🔲 Tests not yet implemented
│       └── java/com/taskmanagement/
│
├── target/                                   # Maven build output (generated)
│   └── classes/                              # Compiled .class files
│
├── logs/                                     # Application logs (runtime)
│   └── task-management.log
│
├── pom.xml                                   # Maven dependencies & build config
├── README.md                                 # ⭐ This file - project overview
├── POM_CONFIGURATION.md                      # Detailed pom.xml explanation
├── BUSINESS_OVERVIEW.md                      # Business requirements
├── ARCHITECTURE.md                           # Architecture decisions
└── .gitignore                                # Git ignore rules
```

### Legend
- ✅ **Implemented** - Code exists and functional
- 🔲 **Placeholder** - Folder/file exists but no implementation yet
- ⭐ **Documentation** - README or configuration files

### What's Actually Implemented (v0.6.0)

**Backend Code:**
- Task CRUD operations with Many-to-Many assignees
- Native SQL workarounds for Hibernate @Where filter issues
- Entity relationships (Task ↔ Users N:N, Task ↔ Project N:1, Comments/Attachments 1:N)
- Request validation with Bean Validation
- Exception handling with consistent error responses
- Basic Authentication with hardcoded users
- Enhanced SQL logging for debugging

**Recent Bug Fixes:**
- Fixed empty assignees issue in GET /api/tasks/{id}
- Implemented 3-step workaround for @Where filter + lazy loading
- Added native query methods to bypass Hibernate filtering

**Not Yet Implemented:**
- User/Project management APIs
- JWT authentication
- Task filtering and pagination
- Utility classes
- Event system
- Unit/integration tests
- Migration from @Where to @FilterDef

---

## Clean Architecture Layers

Clean Architecture organizes code into concentric layers, with **business logic at the center** and **external dependencies at the edges**.

### Layer 1: Controllers (API / Presentation Layer)
**Location:** `api/`

**Responsibility:**
- Accept HTTP requests from clients
- Validate request parameters
- Call appropriate services
- Return HTTP responses with proper status codes
- Handle request/response serialization (JSON)

**Key Components:**
- `TaskController` - Task CRUD operations
- `UserController` - User management
- `AuthController` - Authentication (login, register, refresh token)
- `ProjectController` - Project/Team management
- `CommentController` - Task comments
- `NotificationController` - User notifications

**Dependencies:** Services, DTOs
**Independent Of:** Database, external APIs (loosely coupled)

**Example Request Flow:**
```
HTTP Request
    ↓
@PostMapping("/tasks")
    ↓
validateRequest()
    ↓
taskService.createTask()
    ↓
HTTP Response (201 Created)
```

---

### Layer 2: Services (Business Logic Layer)
**Location:** `service/`

**Responsibility:**
- Implement core business logic
- Orchestrate repositories
- Handle transactions (@Transactional)
- Publish domain events
- Implement validation rules
- Calculate derived data

**Key Components:**
- `TaskService` - Task creation, assignment, status updates
- `UserService` - User management, RBAC enforcement
- `ProjectService` - Project/Team operations
- `CommentService` - Comment management
- `NotificationService` - Send notifications
- `AuditService` - Audit trail logging
- `AuthService` - Authentication logic

**Dependencies:** Repositories, Events, Utilities
**Independent Of:** HTTP (controllers are independent of this layer structure)

**Example Business Logic:**
```
createTask(CreateTaskRequest) {
  1. Validate request (business rules)
  2. Create Task entity
  3. Assign to user (check user permissions)
  4. Save via repository
  5. Publish TaskCreatedEvent
  6. Return success response
}
```

---

### Layer 3: Repositories (Data Access Layer)
**Location:** `repository/`

**Responsibility:**
- Abstract database operations (CRUD)
- Define data access queries
- Hide implementation details (PostgreSQL, SQL)
- Provide interface contracts
- Enable dependency injection of data sources

**Key Components:**
- `UserRepository extends JpaRepository<User, Long>` - User queries
- `TaskRepository extends JpaRepository<Task, Long>` - Task queries
- `ProjectRepository` - Project queries
- `CommentRepository` - Comment queries
- `AuditLogRepository` - Audit log queries
- `NotificationRepository` - Notification queries

**Dependencies:** Entities (domain models)
**Independent Of:** Business logic (services are independent of repository implementation)

**Example Repository Methods:**
```
interface TaskRepository extends JpaRepository<Task, Long> {
  List<Task> findByAssigneeAndStatus(User assignee, TaskStatus status);
  List<Task> findByProjectAndDueDateBefore(Project project, LocalDate dueDate);
  List<Task> findOverdueTasks();
}
```

---

### Layer 4: Entities (Domain / Data Model Layer)
**Location:** `entity/`

**Responsibility:**
- Define database table structure via JPA annotations
- Represent core business domain concepts
- Enforce constraints and validation
- Define relationships between entities
- Implement lifecycle hooks (pre-persist, pre-update)

**Key Components:**
- `User` - System users with authentication
- `Task` - Core task entity
- `Project` - Project/Team grouping
- `TaskAssignment` - Task-to-User mapping
- `Comment` - Task comments and discussions
- `Attachment` - File attachments
- `Notification` - User alerts
- `AuditLog` - System activity tracking

**Dependencies:** None (independent entities)
**Independent Of:** Everything else (entities are at the center of Clean Architecture)

**Example Entity Structure:**
```
@Entity
@Table(name = "tasks")
public class Task {
  @Id
  @GeneratedValue
  private Long id;
  
  @ManyToOne
  private Project project;
  
  @ManyToMany
  private Set<User> assignees;
  
  @OneToMany(mappedBy = "task")
  private List<Comment> comments;
  
  // Audit fields
  @CreationTimestamp
  private LocalDateTime createdAt;
  
  @UpdateTimestamp
  private LocalDateTime updatedAt;
}
```

---

## Additional Layers & Cross-Cutting Concerns

### DTOs (Data Transfer Objects)
**Location:** `dto/`

**Responsibility:**
- Define request/response contracts for APIs
- Validate input data (@Valid annotations)
- Serialize/deserialize JSON
- Decouple API contracts from entities
- Support API versioning

**Examples:**
- `CreateTaskRequest` - Input validation for task creation
- `TaskResponse` - Task serialization format
- `UpdateTaskRequest` - Task update payload
- `LoginRequest` / `LoginResponse` - Authentication

**Why DTOs Matter:**
- API can change without affecting database schema
- Hide internal entity structure from clients
- Enforce type-safe request validation
- Support multiple API versions simultaneously

---

### Security & JWT
**Location:** `security/`

**Responsibility:**
- Generate JWT tokens upon successful login
- Validate JWT tokens on each request
- Extract user claims from tokens (userId, roles)
- Filter requests via `JwtAuthenticationFilter`
- Integrate with Spring Security

**Components:**
- `JwtTokenProvider` - Token creation/validation
- `JwtAuthenticationFilter` - OncePerRequestFilter for token extraction
- `SecurityConfig` - Spring Security configuration

**Security Flow:**
```
1. POST /api/auth/login
   → UserService.authenticate()
   → JwtTokenProvider.generateToken()
   → Return JWT token

2. GET /api/tasks (with Authorization: Bearer <token>)
   → JwtAuthenticationFilter.doFilterInternal()
   → JwtTokenProvider.validateToken()
   → Extract userId, roles
   → Set SecurityContext
   → Pass to controller

3. Controller receives request with authenticated Principal
   → @PreAuthorize("hasRole('ADMIN')")
   → Grant/deny based on roles
```

---

### Configuration Classes
**Location:** `config/`

**Responsibility:**
- Define Spring beans and auto-configuration
- Configure security, JPA, OpenAPI
- Wire dependencies together
- Setup third-party integrations

**Components:**
- `SecurityConfig` - Spring Security configuration, JWT filter setup
- `JpaConfig` - JPA repository scanning, auditing configuration
- `OpenApiConfig` - Swagger/OpenAPI documentation setup

---

### Exception Handling
**Location:** `exception/`

**Responsibility:**
- Centralize exception handling (@RestControllerAdvice)
- Provide consistent error response format
- Map exceptions to HTTP status codes
- Include helpful error messages

**Components:**
- `GlobalExceptionHandler` - Catches all exceptions, returns ErrorResponse
- `ErrorResponse` - Standard error response DTO

**Example Error Response:**
```json
{
  "code": "ENTITY_NOT_FOUND",
  "message": "Task with id=123 not found",
  "timestamp": 1701389400000,
  "path": "/api/tasks/123"
}
```

---

### Event-Driven Architecture
**Location:** `event/`

**Responsibility:**
- Define domain events (TaskCreatedEvent, TaskCompletedEvent)
- Publish events from services
- Listen and react to events asynchronously
- Decouple services via event-driven patterns

**Components:**
- `TaskCreatedEvent` - Published when task is created
- `TaskCompletedEvent` - Published when task is completed
- `TaskEventListener` - Listens for task events
- `NotificationEventListener` - Sends notifications on events

**Example Event Flow:**
```
TaskService.createTask()
  ↓
Publish TaskCreatedEvent
  ↓
NotificationEventListener.onTaskCreated()
  ↓
Send notification to project members
```

---

### Utilities
**Location:** `util/`

**Responsibility:**
- Centralize constants (roles, statuses, priorities)
- Provide utility methods
- Define application-wide conventions

**Components:**
- `Constants.java` - Role constants, task statuses, priorities, HTTP status messages

---

## Package Organization

```
com.taskmanagement
├── api.*               # REST Controllers
├── service.*           # Business logic services
├── repository.*        # Data access interfaces
├── entity.*            # JPA domain models
├── dto.*               # Request/Response DTOs
├── config.*            # Configuration classes
├── security.*          # JWT and security logic
├── exception.*         # Exception handling
├── event.*             # Event-driven components
└── util.*              # Utilities and constants
```

**Naming Conventions:**
- **Controllers:** `XyzController` (e.g., `TaskController`)
- **Services:** `XyzService` (e.g., `TaskService`)
- **Repositories:** `XyzRepository` (e.g., `TaskRepository`)
- **Entities:** `Xyz` (e.g., `Task`, `User`)
- **DTOs:** `XyzRequest`, `XyzResponse` (e.g., `CreateTaskRequest`, `TaskResponse`)
- **Events:** `XyzEvent` (e.g., `TaskCreatedEvent`)
- **Listeners:** `XyzListener` (e.g., `NotificationListener`)
- **Config:** `XyzConfig` (e.g., `SecurityConfig`)

---

## Technology Stack

### Backend Framework
- **Spring Boot 3.2.0** - Java application framework
- **Java 17** - Programming language
- **Maven 3.x** - Build and dependency management

### Database & ORM
- **PostgreSQL 15+** - Relational database
- **JPA/Hibernate 6.x** - Object-Relational Mapping
- **Flyway 9.x** - Database schema migrations
- **HikariCP** - Connection pooling

### Security & Authentication
- **Spring Security 6.x** - Authentication and authorization
- **JJWT 0.12.3** - JSON Web Token library
- **BCrypt** - Password encryption

### API Documentation
- **Springdoc OpenAPI 2.x** - Swagger/OpenAPI documentation
- **Swagger UI** - Interactive API documentation

### Testing
- **JUnit 5** - Unit testing framework
- **Mockito** - Mocking framework
- **Spring Boot Test** - Integration testing
- **TestContainers** - Docker containers for testing

### Development Tools
- **Lombok 1.18.30** - Code generation (getters, setters, builders)
- **Jackson 2.x** - JSON serialization/deserialization

### Monitoring & Observability
- **Spring Boot Actuator** - Health checks and metrics
- **Micrometer** - Metrics collection
- **Prometheus** - Metrics scraping format

### Optional (Phase 2+)
- **RabbitMQ** - Message queuing (commented in pom.xml)
- **Redis** - In-memory caching (commented in pom.xml)
- **Kafka** - Event streaming (commented in pom.xml)

---

## Key Design Patterns

### 1. Dependency Injection
All dependencies are injected via Spring's `@Autowired` or constructor injection.

```java
@Service
public class TaskService {
  private final TaskRepository repository;
  
  public TaskService(TaskRepository repository) {
    this.repository = repository;
  }
}
```

**Benefits:**
- Loose coupling between classes
- Easy to mock for testing
- Spring manages lifecycle

---

### 2. Repository Pattern
Data access is abstracted behind repository interfaces.

```java
public interface TaskRepository extends JpaRepository<Task, Long> {
  List<Task> findByAssignee(User assignee);
}
```

**Benefits:**
- Switch database implementations without changing services
- Centralize query logic
- Testable with mock repositories

---

### 3. Service Layer
Business logic is isolated in services, separate from HTTP concerns.

```java
@Service
@Transactional
public class TaskService {
  public Task createTask(CreateTaskRequest request) {
    // Validation, business rules
    // Repository operations
    // Event publishing
  }
}
```

**Benefits:**
- Reusable logic (APIs, CLI, scheduled jobs)
- Easier to test
- Clear separation of concerns

---

### 4. DTO Pattern
APIs communicate via DTOs, not entities.

```java
// Request DTO with validation
@Data
public class CreateTaskRequest {
  @NotBlank
  private String title;
  
  @Min(1)
  private Long projectId;
}

// Response DTO
@Data
public class TaskResponse {
  private Long id;
  private String title;
  private TaskStatus status;
}
```

**Benefits:**
- Decouple API from database schema
- Validate input before processing
- Hide internal entity structure

---

### 5. Event-Driven Architecture
Services publish events that trigger side effects.

```java
@Service
public class TaskService {
  private final ApplicationEventPublisher eventPublisher;
  
  public Task createTask(CreateTaskRequest request) {
    Task task = new Task(...);
    repository.save(task);
    eventPublisher.publishEvent(new TaskCreatedEvent(task));
    return task;
  }
}

@EventListener
public void onTaskCreated(TaskCreatedEvent event) {
  // Send notifications, update metrics, etc.
}
```

**Benefits:**
- Loose coupling between services
- Asynchronous operations
- Scalable event processing

---

### 6. Layered Architecture with Clear Dependencies
Each layer depends only on lower layers.

```
Controllers (HTTP)
    ↓ depends on
Services (Business Logic)
    ↓ depends on
Repositories (Data Access)
    ↓ depends on
Entities (Domain Models)
```

**Benefits:**
- Testable in isolation
- Easy to replace implementations
- Clear data flow

---

## Development Workflow

### Step 1: Understand the Requirements
Review `BUSINESS_OVERVIEW.md` to understand:
- User roles and permissions
- Task workflows
- Core features
- Future expansions

### Step 2: Design the Domain Model
Create JPA entities in `entity/` package:
- Define properties and relationships
- Add validation annotations
- Implement audit fields (createdAt, updatedAt)

### Step 3: Implement Data Access
Create repository interfaces in `repository/` package:
- Extend JpaRepository
- Define custom query methods
- Test with TestContainers

### Step 4: Implement Business Logic
Create services in `service/` package:
- Implement use cases
- Add validation logic
- Publish domain events
- Use @Transactional for consistency

### Step 5: Create DTOs
Create request/response objects in `dto/` package:
- Add @Valid validation annotations
- Define contracts for APIs
- Handle serialization/deserialization

### Step 6: Build REST APIs
Create controllers in `api/` package:
- Map HTTP endpoints to services
- Return appropriate status codes
- Document with Swagger annotations

### Step 7: Secure the Application
Implement in `security/` and `config/`:
- JWT token generation/validation
- RBAC with @PreAuthorize
- Secure sensitive endpoints

### Step 8: Add Error Handling
Implement in `exception/`:
- Global exception handler
- Consistent error responses
- Proper HTTP status codes

### Step 9: Test Thoroughly
Create tests in `src/test/`:
- Unit tests for services
- Integration tests with TestContainers
- API endpoint tests
- Security tests

### Step 10: Document and Deploy
- Document APIs with Swagger
- Create migration scripts with Flyway
- Package as JAR: `mvn clean package`
- Deploy with configuration profiles

---

## Getting Started

### Prerequisites
- **Java 17+** - Download from [adoptopenjdk.net](https://adoptopenjdk.net)
- **Maven 3.8+** - Download from [maven.apache.org](https://maven.apache.org)
- **PostgreSQL 15+** - Download from [postgresql.org](https://www.postgresql.org)
- **Git** - Download from [git-scm.com](https://git-scm.com)

### Quick Start

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd java_project
   ```

2. **Setup PostgreSQL Database:**
   ```bash
   # Đăng nhập PostgreSQL
   psql -U postgres
   
   # Tạo database
   postgres=# CREATE DATABASE task_db;
   
   # Tạo user (tùy chọn)
   postgres=# CREATE USER task_user WITH PASSWORD 'task_password';
   postgres=# GRANT ALL PRIVILEGES ON DATABASE task_db TO task_user;
   postgres=# \q
   ```

3. **Configure application.yml:**
   ```yaml
   # File: src/main/resources/application.yml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/task_db
       username: postgres  # Hoặc task_user
       password: your_password
     jpa:
       hibernate:
         ddl-auto: update  # Tự động tạo tables
   ```

4. **Build the project:**
   ```bash
   mvn clean compile
   ```

5. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

6. **Verify application is running:**
   - Health Check: http://localhost:8080/actuator/health
   - Expected response: `{"status":"UP"}`

---

## 🧪 API Testing with Postman

A complete Postman collection is available for testing all API endpoints.

### Quick Access

[![Run in Postman](https://run.pstmn.io/button.svg)](https://www.postman.com/api-team-5375/workspace/api-workspace/request/37783257-eb670533-dc90-408b-ad08-732c7d8390e1?action=share&creator=37783257)

**Collection includes:**
- ✅ User Management API (GET, DELETE, RESTORE)
- ✅ Task Management API (CRUD operations)
- ✅ Pre-configured environment variables
- ✅ Sample requests with test data
- ✅ Authentication examples (Basic Auth)

### Getting Started with Postman

1. **Import Collection**
   - Click the "Run in Postman" button above
   - Or manually import from: [Postman Collection Link](https://www.postman.com/api-team-5375/workspace/api-workspace/request/37783257-eb670533-dc90-408b-ad08-732c7d8390e1?action=share&creator=37783257)

2. **Configure Environment**
   ```
   BASE_URL: http://localhost:8080
   USERNAME: admin
   PASSWORD: admin
   ```

3. **Authentication**
   - Type: Basic Auth
   - Default credentials: `admin:admin`
   - Credentials are pre-configured in collection

📖 **Detailed testing guide:** See [docs/api-testing.md](docs/api-testing.md) (coming soon)

---

### Test API Endpoints (Manual)

#### 1. Create a Task
```bash
POST http://localhost:8080/api/tasks
Content-Type: application/json
Authorization: Basic YWRtaW46YWRtaW4xMjM=

{
  "title": "Fix login bug",
  "description": "Users cannot login with special characters in password",
  "priority": "HIGH",
  "dueDate": "2025-12-20T17:00:00",
  "estimatedHours": 8,
  "assigneeId": 1,
  "projectId": 1
}
```

**Note:** Bạn cần tạo User và Project trước, hoặc dùng mock data có sẵn.

#### 2. Get Task by ID
```bash
GET http://localhost:8080/api/tasks/1
Authorization: Basic YWRtaW46YWRtaW4xMjM=
```

#### 3. Update Task
```bash
PUT http://localhost:8080/api/tasks/1
Content-Type: application/json
Authorization: Basic YWRtaW46YWRtaW4xMjM=

{
  "status": "IN_PROGRESS",
  "assigneeId": 2
}
```

#### 4. Delete Task
```bash
DELETE http://localhost:8080/api/tasks/1
Authorization: Basic YWRtaW46YWRtaW4xMjM=
```

### Configuration Files
- **application.yml:** Database, JPA, Security configuration
- **pom.xml:** Maven dependencies and build configuration
- **POM_CONFIGURATION.md:** Detailed explanation of dependencies
- **APPLICATION_YML_CONFIGURATION.md:** Configuration options explained

### Troubleshooting

**Issue:** Application fails to start with database connection error
```
Solution: Verify PostgreSQL is running and credentials in application.yml are correct
psql -U postgres -c "SELECT version();"
```

**Issue:** Cannot create task - Foreign key violation
```
Solution: Ensure User and Project with the specified IDs exist in database
INSERT INTO users (id, username, email, full_name) VALUES (1, 'john', 'john@example.com', 'John Doe');
INSERT INTO projects (id, name, description) VALUES (1, 'Project Alpha', 'First project');
```

**Issue:** Port 8080 already in use
```
Solution: Change port in application.yml
server:
  port: 8081
```

### Next Steps
1. Review implemented features in [src/main/java/com/taskmanagement](src/main/java/com/taskmanagement "src/main/java/com/taskmanagement")
2. Check API documentation in [api/README.md](src/main/java/com/taskmanagement/api/README.md "src/main/java/com/taskmanagement/api/README.md")
3. Read entity relationships in [entity/README.md](src/main/java/com/taskmanagement/entity/README.md "src/main/java/com/taskmanagement/entity/README.md")
4. Understand business logic in [service/README.md](src/main/java/com/taskmanagement/service/README.md "src/main/java/com/taskmanagement/service/README.md")

---

## Project Governance

### Code Organization Principles
✅ **Separation of Concerns** - Each class has a single responsibility  
✅ **Dependency Injection** - Spring manages all dependencies  
✅ **Immutability** - Use `@Data` with `final` fields where possible  
✅ **Testing** - Every service should have corresponding tests  
✅ **Documentation** - Use Javadoc and inline comments  
✅ **Transactions** - Use `@Transactional` for data consistency  

### File Naming Conventions
- **Controllers:** Suffix with `Controller` (e.g., `TaskController`)
- **Services:** Suffix with `Service` (e.g., `TaskService`)
- **Repositories:** Suffix with `Repository` (e.g., `TaskRepository`)
- **Entities:** Use domain name (e.g., `Task`, `User`)
- **DTOs:** Use `Request`/`Response` suffix (e.g., `CreateTaskRequest`)
- **Test Classes:** Prefix with `Test` or suffix with `Test` (e.g., `TaskServiceTest`)

### Testing Strategy
- **Unit Tests:** Test services in isolation with mock repositories
- **Integration Tests:** Use TestContainers with real PostgreSQL
- **API Tests:** Test controllers with `@WebMvcTest`
- **Security Tests:** Test with `@WithMockUser` and `@WithAnonymousUser`
- **Target Coverage:** Aim for 80%+ line coverage in critical paths

### Commit Message Convention
```
[FEATURE|BUGFIX|CHORE|DOCS] Package - Brief description

Details about the change...
```

Examples:
- `[FEATURE] api - Add task creation endpoint`
- `[BUGFIX] service - Fix null pointer in assignment logic`
- `[DOCS] config - Update security configuration comments`

---

## Resources & Documentation

### Project Documentation
- `BUSINESS_OVERVIEW.md` - Business requirements and features
- `POM_CONFIGURATION.md` - Detailed pom.xml explanation
- `APPLICATION_YML_CONFIGURATION.md` - Configuration profiles
- `ARCHITECTURE.md` - Deep dive into architecture decisions

### Spring Boot Documentation
- [Spring Boot Reference](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring Security](https://spring.io/projects/spring-security)

### External Resources
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design](https://domainlanguage.com/ddd/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Hibernate Documentation](https://hibernate.org/orm/documentation/)

---

## Contributing

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Commit with conventional messages
3. Push and create a Pull Request
4. Code review before merging
5. Ensure tests pass: `mvn clean test`
6. Build package: `mvn clean package`

---

## API Endpoints Summary

### Currently Implemented

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | /api/tasks | Create new task | ✅ Implemented |
| GET | /api/tasks/{id} | Get task by ID | ✅ Implemented |
| PUT | /api/tasks/{id} | Update task | ✅ Implemented |
| DELETE | /api/tasks/{id} | Delete task | ✅ Implemented |

### Planned for Next Phase

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | /api/tasks | List all tasks with filters | 🔲 Planned |
| GET | /api/tasks?assigneeId=1 | Filter tasks by assignee | 🔲 Planned |
| GET | /api/tasks?projectId=1 | Filter tasks by project | 🔲 Planned |
| GET | /api/users/{id} | Get user by ID | 🔲 Planned |
| GET | /api/users | List all users | 🔲 Planned |
| POST | /api/users | Create user | 🔲 Planned |
| GET | /api/projects/{id} | Get project details | 🔲 Planned |
| POST | /api/projects | Create project | 🔲 Planned |

---

## Database Schema

### Core Tables

**tasks**
- id (PK)
- title
- description
- status (PENDING, IN_PROGRESS, BLOCKED, IN_REVIEW, COMPLETED, CANCELLED)
- priority (LOW, MEDIUM, HIGH, CRITICAL)
- assignee_id (FK → users, **NOT NULL currently**)
- project_id (FK → projects, NOT NULL)
- due_date
- start_date
- completed_at
- estimated_hours
- notes
- created_at, updated_at

**users**
- id (PK)
- username (unique)
- email (unique)
- full_name
- password_hash
- active
- last_login_at
- created_at, updated_at

**projects**
- id (PK)
- name
- description
- active
- created_at, updated_at

**comments** (defined but not API exposed yet)
- id (PK)
- task_id (FK → tasks, CASCADE DELETE)
- user_id (FK → users)
- content
- created_at, updated_at

**attachments** (defined but not API exposed yet)
- id (PK)
- task_id (FK → tasks, CASCADE DELETE)
- filename
- file_path
- file_size
- mime_type
- uploaded_by (FK → users)
- created_at

### Known Limitations
⚠️ **assignee_id is currently NOT NULL** - Cannot create unassigned tasks or remove assignee
⚠️ **No ON DELETE action for user FK** - Cannot delete user if they have assigned tasks
⚠️ **No soft delete** - Deletes are permanent

---

## Version History

**v0.5.0 (2025-12-14)**
- ✅ Task CRUD operations implemented
- ✅ Task-User-Project relationships working
- ✅ Basic authentication with Spring Security
- ✅ Exception handling with GlobalExceptionHandler
- ✅ Database integration with PostgreSQL
- ✅ Comprehensive logging
- ⚠️ Known issue: Cannot remove assignee from task

**v0.1.0 (2025-12-01)**
- Initial project scaffold
- Clean Architecture setup
- Spring Boot 3.2 configuration
- Entity definitions
- Repository interfaces

---

## Known Issues & TODOs

### ⚠️ Critical Bug Fixed (v0.6.0)

**Issue: GET /api/tasks/{id} returns empty assignees array**
- **Symptoms:** POST creates task with assignees successfully, but GET returns `"assignees": []`
- **Root Cause:** Hibernate 6.x `@Where(clause = "deleted = false")` filter on User entity applies AFTER collection loading, causing empty collections even with valid data
- **Impact:** Many-to-Many relationships with @Where filtered entities fail to load
- **Solution Implemented:** 
  - Created `findByIdNative()` to load Task with native SQL
  - Created `findAssigneeIdsByTaskId()` to load assignee IDs separately
  - Manually populate `task.assignees` collection in `TaskService.getTaskById()`
  - This workaround bypasses Hibernate's @Where filter issues

**Files Modified:**
- `TaskRepository.java` - Added 2 new native query methods
- `TaskService.java` - Modified `getTaskById()` with 3-step workaround
- `application.yml` - Enhanced SQL logging for debugging

**Alternative Approaches Attempted (Failed):**
- ❌ `LEFT JOIN FETCH` in HQL - Still affected by @Where filter
- ❌ `@EntityGraph(attributePaths = {"assignees"})` - Same issue
- ❌ `Hibernate.initialize()` - Collection initialized but empty
- ❌ Removing @Where temporarily - Not viable due to soft delete requirements

**Lesson Learned:** Hibernate @Where filter + Many-to-Many lazy loading = incompatible in Hibernate 6.x. Use native queries or @FilterDef for complex scenarios.

---

### Other Known Issues
1. **User soft delete with @Where filter** - May cause issues with lazy-loaded collections
   - Impact: Collections referencing soft-deleted users might load empty
   - Workaround: Use native queries or @FilterDef for fine-grained control
   - Consider: Migrating from @Where to @Filter + @FilterDef for better control

2. **Cannot delete users with assigned tasks** - Foreign key constraint blocks user deletion
   - Impact: Users cannot be deactivated/removed if they have active task assignments
   - Solution: Implement proper cascade rules or bulk unassign before deletion

### Planned Improvements
- [ ] Implement task filtering API (GET /api/tasks?assigneeId=1&projectId=2)
- [ ] Add User management API (GET/POST /api/users)
- [ ] Add Project management API
- [ ] Support removing assignee from tasks
- [ ] Implement soft delete for tasks
- [ ] Add pagination and sorting
- [ ] Implement JWT authentication
- [ ] Add role-based access control (RBAC)
- [ ] Event-driven notifications
- [ ] File attachment upload API

---

## License

[Specify your license here]

---

## Contact & Support

For questions, issues, or suggestions:
- Create an issue in the repository
- Review documentation in package README files
- Check [BUSINESS_OVERVIEW.md](BUSINESS_OVERVIEW.md "BUSINESS_OVERVIEW.md") for requirements

---

**Last Updated:** December 21, 2025  
**Version:** v0.6.0 - Many-to-Many Assignees with @Where Filter Workaround  
**Status:** MVP Phase - Core Task Management Operational + Bug Fixes  
**Next Milestone:** Task Filtering & User Management APIs
