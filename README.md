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

The **Task Management System** is an enterprise-grade application built using **Spring Boot 3.2** and **Clean Architecture** principles. It provides centralized task management with role-based access control, real-time collaboration, and comprehensive reporting.

### Core Characteristics
- **Architecture:** Clean Architecture with clear separation of concerns
- **Framework:** Spring Boot 3.2 with Java 17
- **Database:** PostgreSQL with JPA/Hibernate ORM
- **Security:** JWT-based stateless authentication + Spring Security RBAC
- **API:** RESTful with OpenAPI/Swagger documentation
- **Scalability:** Stateless design, connection pooling, optimized queries
- **Monitoring:** Actuator endpoints for health checks and metrics

---

## Project Structure

```
task-management-system/
│
├── src/
│   ├── main/
│   │   ├── java/com/taskmanagement/          # Source code (Clean Architecture)
│   │   │   ├── api/                          # Layer 1: Controllers (HTTP)
│   │   │   │   └── [README.md]               # REST endpoint documentation
│   │   │   │
│   │   │   ├── service/                      # Layer 2: Business Logic
│   │   │   │   └── [README.md]               # Service layer documentation
│   │   │   │
│   │   │   ├── repository/                   # Layer 3: Data Access (JPA)
│   │   │   │   └── [README.md]               # Repository interfaces
│   │   │   │
│   │   │   ├── entity/                       # Layer 4: Domain Models (Database)
│   │   │   │   └── [README.md]               # JPA entity definitions
│   │   │   │
│   │   │   ├── dto/                          # Data Transfer Objects
│   │   │   │   └── [README.md]               # Request/Response DTOs
│   │   │   │
│   │   │   ├── config/                       # Configuration Classes
│   │   │   │   ├── SecurityConfig.java       # Spring Security & JWT setup
│   │   │   │   ├── JpaConfig.java            # JPA/Hibernate configuration
│   │   │   │   └── OpenApiConfig.java        # Swagger/OpenAPI documentation
│   │   │   │
│   │   │   ├── security/                     # Security & JWT
│   │   │   │   ├── JwtTokenProvider.java     # JWT token generation/validation
│   │   │   │   └── JwtAuthenticationFilter.java  # JWT request filter
│   │   │   │
│   │   │   ├── exception/                    # Error Handling
│   │   │   │   ├── GlobalExceptionHandler.java  # Centralized exception handling
│   │   │   │   └── ErrorResponse.java        # Standard error response DTO
│   │   │   │
│   │   │   ├── event/                        # Event-Driven Architecture
│   │   │   │   └── [README.md]               # Domain events and listeners
│   │   │   │
│   │   │   ├── util/                         # Utility Classes
│   │   │   │   └── Constants.java            # Application-wide constants
│   │   │   │
│   │   │   └── TaskManagementApplication.java  # Main Spring Boot Entry Point
│   │   │
│   │   └── resources/
│   │       ├── application.yml               # Spring Boot configuration (step-by-step)
│   │       ├── db/
│   │       │   └── migration/
│   │       │       └── V1__initial_schema.sql  # Flyway database migrations
│   │       └── [static/]                     # (Future) Frontend assets
│   │
│   └── test/
│       ├── java/com/taskmanagement/          # Unit & integration tests
│       │   ├── api/
│       │   ├── service/
│       │   ├── repository/
│       │   └── security/
│       │
│       └── resources/                        # Test configuration
│           └── application-test.yml
│
├── pom.xml                                   # Maven build configuration
├── .gitignore                                # Git ignore rules
├── README.md                                 # This file
├── POM_CONFIGURATION.md                      # Detailed pom.xml explanation
├── APPLICATION_YML_CONFIGURATION.md          # Detailed application.yml explanation
├── BUSINESS_OVERVIEW.md                      # Business requirements & features
├── ARCHITECTURE.md                           # Architecture deep dive
│
└── logs/                                     # Application logs (created at runtime)
    └── task-management.log
```

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
- **Java 17+** - Download from [adoptopenjdk.com](https://adoptopenjdk.net)
- **Maven 3.8+** - Download from [maven.apache.org](https://maven.apache.org)
- **PostgreSQL 15+** - Download from [postgresql.org](https://www.postgresql.org)
- **Git** - Download from [git-scm.com](https://git-scm.com)

### Quick Start

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd task-management-system
   ```

2. **Setup PostgreSQL:**
   ```bash
   psql -U postgres
   postgres=# CREATE DATABASE task_management_dev;
   postgres=# \q
   ```

3. **Build the project:**
   ```bash
   mvn clean compile
   ```

4. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

5. **Access the application:**
   - API Base: `http://localhost:8080/api`
   - Swagger UI: `http://localhost:8080/api/swagger-ui.html`
   - Health Check: `http://localhost:8080/api/actuator/health`

### Configuration
- **Development:** Edit `application.yml` - STEP 1 is enabled
- **Production:** Set environment variables (DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD)

### Next Steps
1. Read `POM_CONFIGURATION.md` to understand dependencies
2. Read `APPLICATION_YML_CONFIGURATION.md` to understand configuration
3. Review `BUSINESS_OVERVIEW.md` to understand features
4. Start implementing entities in Step 1 of development workflow

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

## Version History

**v1.0.0 (2025-12-01)**
- Initial project scaffold
- Clean Architecture setup
- Spring Boot 3.2 configuration
- JWT authentication placeholders
- API documentation setup

---

## License

[Specify your license here]

---

## Contact & Support

For questions, issues, or suggestions:
- Create an issue in the repository
- Contact the development team at [email]

---

**Last Updated:** December 1, 2025  
**Status:** Project Skeleton Complete - Ready for Feature Implementation
