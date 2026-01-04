# Task Management System

> **Enterprise-grade task management application** built with Spring Boot 3.2 + Clean Architecture and React 19.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptopenjdk.net)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19.2-blue.svg)](https://react.dev)

---

## 🚀 Quick Links

**Live Applications:**
- 🌐 **Frontend:** [https://task-management-frontend-8brf.onrender.com/](https://task-management-frontend-8brf.onrender.com/)

**API Testing:**
- [![Run in Postman](https://run.pstmn.io/button.svg)](https://www.postman.com/api-team-5375/workspace/api-workspace/request/37783257-eb670533-dc90-408b-ad08-732c7d8390e1?action=share&creator=37783257)

---

## 🎯 What is This?

A full-stack task management system for teams with:
- **Clean Architecture** - 4-layer backend (Controllers → Services → Repositories → Entities)
- **RESTful API** - Comprehensive CRUD operations for tasks, users, and projects
- **React Frontend** - Modern component-based architecture
- **PostgreSQL** - Relational database with JPA/Hibernate ORM
- **Production-Ready** - Deployed on Render with health monitoring

**Use Cases:**
- Team task tracking and assignment
- Project management with multiple projects
- User management with soft delete support
- Real-time task status updates
- Responsive UI for mobile/desktop

---

## ✨ Core Features

### ✅ Implemented (v0.7.0)

**Backend:**
- ✅ Task CRUD with Many-to-Many assignees
- ✅ User management with soft delete
- ✅ Project management with archive/reactivate
- ✅ RESTful API with validation and error handling
- ✅ Basic Authentication (JWT planned for v1.0.0)
- ✅ Native SQL workarounds for Hibernate @Where filter issues

**Frontend:**
- ✅ Component-based React 19 architecture
- ✅ Real-time search and status filtering
- ✅ Inline task status updates
- ✅ Responsive grid layout
- ✅ Member ID badges with gradient styling

### 🔲 Planned

- 🔲 Backend task filtering API (v0.8.0)
- 🔲 JWT authentication & RBAC (v1.0.0)
- 🔲 Event-driven notifications (v0.9.0)
- 🔲 File attachments & comments
- 🔲 WebSocket for real-time updates
- 🔲 Priority filter in frontend UI

---

## 🏗️ Architecture Overview

**High-Level Architecture:**

```
┌─────────────────────────────────────────────────────────┐
│                    React Frontend                        │
│         (React 19 + Vite + Axios)                        │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP/JSON (Basic Auth)
                     │
┌────────────────────▼────────────────────────────────────┐
│                    REST API Layer                        │
│         (Controllers - TaskController, etc.)             │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                  Business Logic Layer                    │
│          (Services - TaskService, etc.)                  │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                  Data Access Layer                       │
│        (Repositories - TaskRepository, etc.)             │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                  PostgreSQL Database                     │
│   (tasks, users, projects, task_assignees, etc.)        │
└──────────────────────────────────────────────────────────┘
```

**Dependency Flow:** Controllers → Services → Repositories → Entities (Clean Architecture)

📖 **For detailed architecture explanation, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**

---

### Currently Implemented Features
✅ **Task Management:**
  - Create tasks with multiple assignees (Many-to-Many relationship)
  - Get task by ID with full details (assignees, project)
  - Update tasks (title, description, status, priority, assignees)
  - Delete tasks (hard delete with cascade to comments/attachments)

✅ **User Management:**
  - Create new users (POST /api/users)
  - Get all users (GET /api/users)
  - Get user by ID (GET /api/users/{id})
  - Update user details (PUT /api/users/{id})
  - Soft delete users (DELETE /api/users/{id})
  - Restore deleted users (POST /api/users/{id}/restore)

✅ **Project Management:**
  - Create new projects (POST /api/projects)
  - Get all active projects (GET /api/projects)
  - Get project by ID (GET /api/projects/{id})
  - Update project details (PUT /api/projects/{id})
  - Archive projects (DELETE /api/projects/{id})
  - Reactivate projects (POST /api/projects/{id}/reactivate)
  - Get project tasks (GET /api/projects/{id}/tasks)
  
✅ **Relationships:**
  - Task → User (Many-to-Many via task_assignees junction table)
  - Task → Project (Many-to-One, project required)
  - Task → Comments (One-to-Many with cascade delete)
  - Task → Attachments (One-to-Many with cascade delete)

✅ **Database:**
  - PostgreSQL with proper indexes
  - JPA entities with validation
  - Lazy loading for performance

✅ **Frontend (v0.7.0):**
  - React 19 with Vite build system
  - Component-based architecture
  - Search & filter functionality within task panel
  - Real-time task status updates
  - Responsive design for mobile/desktop

### Planned Features (Not Yet Implemented)
🔲 Backend task filtering API (GET /api/tasks?assigneeId=1&status=PENDING)
🔲 JWT Authentication & Authorization (currently using Basic Auth)
🔲 Task comments CRUD API
🔲 File attachments upload/download API
🔲 Event-driven notifications system
🔲 WebSocket for real-time updates
🔲 Priority filter in frontend UI
🔲 Email notification integration
🔲 Task activity history/audit log
🔲 Advanced search with Elasticsearch
🔲 API rate limiting
🔲 Caching layer with Redis

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Clean Architecture layers, design patterns, dependency rules, architectural decision records |
| [API.md](docs/API.md) | Complete REST API reference with endpoints, request/response examples, authentication, error handling |
| [DATABASE_SCHEMA.md](docs/DATABASE_SCHEMA.md) | Entity relationships, table schemas, indexing strategy, JPA mappings, migration notes |
| [KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md) | Critical bugs (Hibernate @Where filter issues), workarounds, root cause analysis, migration plans |
| [FRONTEND_ARCHITECTURE.md](docs/FRONTEND_ARCHITECTURE.md) | React component hierarchy, performance optimization patterns, state management, rendering strategy |

---

## 🚀 Quick Start

### Prerequisites

- **Java 17+** (JDK)
- **Maven 3.x**
- **PostgreSQL 15+**
- **Node.js 18+** & **npm/yarn** (for frontend)

### Backend Setup

1. **Clone repository:**
   ```bash
   git clone https://github.com/your-repo/task-management-system.git
   cd task-management-system
   ```

2. **Configure database** (`src/main/resources/application.yml`):
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/task_db
       username: your_username
       password: your_password
   ```

3. **Build and run:**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

4. **Verify:** http://localhost:8080/actuator/health

### Frontend Setup

1. **Navigate to frontend:**
   ```bash
   cd frontend
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Configure API** (`src/api.js`):
   ```javascript
   const API_BASE_URL = 'http://localhost:8080/api';
   ```

4. **Start development server:**
   ```bash
   npm run dev
   ```

5. **Access:** http://localhost:5173

📖 **For detailed setup instructions, see [docs/ARCHITECTURE.md#getting-started](docs/ARCHITECTURE.md#getting-started)**

---

## 🛠️ Technology Stack

### Backend
- **Java 17** - Modern Java features (Records, Pattern Matching)
- **Spring Boot 3.2** - Application framework
- **Spring Data JPA** - Database abstraction (Hibernate 6.x)
- **Spring Security 6.x** - Basic Auth (JWT planned v1.0.0)
- **PostgreSQL 15+** - Relational database
- **Maven 3.x** - Build automation
- **Lombok** - Boilerplate reduction
- **JUnit 5 + Mockito** - Testing framework

### Frontend
- **React 19.2** - UI library with concurrent features
- **Vite 7.2** - Build tool and dev server
- **Axios 1.13** - HTTP client
- **CSS Modules** - Component-scoped styling

### DevOps
- **Render** - Cloud hosting platform
- **Docker** - Containerization (Dockerfile included)
- **Spring Boot Actuator** - Health monitoring

---

## 📂 Project Structure

```
java_project/                                 # Project root
│
├── frontend/                                  # ✅ React Frontend Application
│   ├── src/
│   │   ├── components/                        # Shared Components
│   │   │   └── Modal.jsx                      # Modal dialog component
│   │   ├── pages/                             # Page Components
│   │   │   ├── DashboardPage.jsx              # Main dashboard container
│   │   ├── styles/                            # CSS Stylesheets
│   │   │   ├── DashboardPage.css              # Dashboard styling
│   │   │   └── Modal.css                      # Modal styling
│   │   ├── assets/                            # Static assets
│   │   ├── api.js                             # API service layer
│   │   ├── App.jsx                            # Root component
│   │   ├── App.css                            # App styling
│   │   ├── index.css                          # Global styles
│   │   └── main.jsx                           # React entry point
│   ├── public/                                # Static assets
│   ├── index.html                             # HTML entry point
│   ├── package.json                           # NPM dependencies
│   ├── vite.config.js                         # Vite configuration
│   └── eslint.config.js                       # ESLint configuration
│
├── src/
│   ├── main/
│   │   ├── java/com/taskmanagement/          # Source code (Clean Architecture)
│   │   │   │
│   │   │   ├── annotation/                   # ✅ Custom Annotations
│   │   │   │   └── Planned.java              # @Planned annotation for future features
│   │   │   │
│   │   │   ├── api/                          # ✅ Layer 1: REST Controllers
│   │   │   │   ├── TaskController.java       # Task CRUD endpoints
│   │   │   │   ├── UserController.java       # User CRUD endpoints
│   │   │   │   ├── ProjectController.java    # Project CRUD endpoints
│   │   │   │   └── README.md                 # API documentation
│   │   │   │
│   │   │   ├── service/                      # ✅ Layer 2: Business Logic
│   │   │   │   ├── TaskService.java          # Task business logic
│   │   │   │   ├── UserService.java          # User management logic
│   │   │   │   ├── ProjectService.java       # Project management logic
│   │   │   │   └── README.md                 # Service layer documentation
│   │   │   │
│   │   │   ├── repository/                   # ✅ Layer 3: Data Access (JPA)
│   │   │   │   ├── TaskRepository.java       # Task queries with native SQL
│   │   │   │   ├── UserRepository.java       # User validation queries
│   │   │   │   ├── ProjectRepository.java    # Project validation queries
│   │   │   │   ├── CommentRepository.java    # Comment queries (defined)
│   │   │   │   └── README.md                 # Repository documentation
│   │   │   │
│   │   │   ├── entity/                       # ✅ Layer 4: Domain Models
│   │   │   │   ├── Task.java                 # Task entity with relationships
│   │   │   │   ├── User.java                 # User entity with soft delete
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
│   │   │   │   │   ├── UpdateTaskRequest.java  # PUT /api/tasks/{id}
│   │   │   │   │   ├── CreateUserRequest.java  # POST /api/users
│   │   │   │   │   ├── UpdateUserRequest.java  # PUT /api/users/{id}
│   │   │   │   │   ├── CreateProjectRequest.java # POST /api/projects
│   │   │   │   │   └── UpdateProjectRequest.java # PUT /api/projects/{id}
│   │   │   │   ├── response/
│   │   │   │   │   ├── TaskResponse.java       # Task response DTO
│   │   │   │   │   ├── UserResponse.java       # User response DTO
│   │   │   │   │   └── ProjectResponse.java    # Project response DTO
│   │   │   │   └── README.md                   # DTO documentation
│   │   │   │
│   │   │   ├── exception/                    # ✅ Error Handling
│   │   │   │   ├── GlobalExceptionHandler.java  # Centralized exception handling
│   │   │   │   ├── ErrorResponse.java           # Standard error format
│   │   │   │   ├── TaskNotFoundException.java   # 404 for tasks
│   │   │   │   ├── UserNotFoundException.java   # 404 for users
│   │   │   │   ├── ProjectNotFoundException.java # 404 for projects
│   │   │   │   ├── BusinessRuleException.java   # Business rule violations
│   │   │   │   ├── DuplicateResourceException.java # Duplicate resource errors
│   │   │   │   └── README.md                    # Exception documentation
│   │   │   │
│   │   │   ├── config/                       # ✅ Configuration
│   │   │   │   ├── SecurityConfig.java       # Basic Auth configuration
│   │   │   │   └── README.md                 # Security configuration docs
│   │   │   │
│   │   │   ├── security/                     # 📋 Planned (v1.0.0)
│   │   │   │   └── package-info.java         # JWT authentication documentation
│   │   │   │
│   │   │   ├── util/                         # 📋 Planned (v0.8.0)
│   │   │   │   └── package-info.java         # Utility classes documentation
│   │   │   │
│   │   │   ├── event/                        # 📋 Planned (v0.9.0)
│   │   │   │   └── package-info.java         # Event-driven architecture documentation
│   │   │   │
│   │   │   └── TaskManagementApplication.java  # Main Spring Boot entry point
│   │   │
│   │   └── resources/
│   │       ├── application.yml               # Spring Boot configuration
│   │       └── APPLICATION_YML_CONFIGURATION.md  # Configuration guide
│   │
│   └── test/                                 # ✅ Tests Implemented
│       └── java/com/taskmanagement/
│           ├── api/                          # Controller Tests
│           │   ├── TaskControllerTest.java
│           │   ├── UserControllerTest.java
│           │   └── ProjectControllerTest.java
│           ├── service/                      # Service Tests
│           │   ├── TaskServiceTest.java
│           │   ├── UserServiceTest.java
│           │   └── ProjectServiceTest.java
│           ├── util/                         # Test Utilities
│           │   ├── TestDataBuilder.java     # Test data factory
│           │   └── TestConstants.java       # Test constants
│           └── TaskManagementApplicationTests.java # Application context test
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
- � **Planned** - Package exists with package-info.java documentation for future implementation
- ⭐ **Documentation** - README or configuration files

---

## 📝 Version History

| Version | Date | Features |
|---------|------|----------|
| v0.7.0 | 2026-01-04 | Frontend UI improvements and component refactoring |
| v0.6.0 | 2026-01-03 | Project archive/reactivate functionality |
| v0.5.0 | 2026-01-02 | User soft delete with restore capability |
| v0.4.0 | 2026-01-01 | Many-to-Many task assignment with native SQL workaround |
| v0.3.0 | 2025-12-30 | Project CRUD operations |
| v0.2.0 | 2025-12-29 | User management and Basic Auth |
| v0.1.0 | 2025-12-28 | Initial task CRUD implementation |

---

**Documentation last updated:** January 4, 2026  
**Project Status:** Active Development (MVP Phase Complete)

---

## 💡 Key Highlights

- ✅ **Clean 4-Layer Architecture** - Controllers → Services → Repositories → Entities
- ✅ **Production-Ready Backend** - Spring Boot 3.2 + PostgreSQL with health monitoring
- ✅ **Modern React Frontend** - Component-based architecture with React 19
- ✅ **Comprehensive Testing** - 42 unit tests with JaCoCo coverage reports
- ✅ **Real-World Problem Solving** - Documented Hibernate issues with working solutions
- ✅ **Complete API Documentation** - REST endpoints with request/response examples
- ✅ **Interview-Ready Docs** - Architecture decisions and trade-offs explained

---

## 🎬 Next Steps

**For Users:**
1. Follow [Quick Start](#-quick-start) to run locally
2. Explore [API.md](docs/API.md) for API endpoints
3. Check [KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md) for current limitations

**For Developers:**
1. Read [ARCHITECTURE.md](docs/ARCHITECTURE.md) for design principles
2. Review [DATABASE_SCHEMA.md](docs/DATABASE_SCHEMA.md) for data model
3. Study [FRONTEND_ARCHITECTURE.md](docs/FRONTEND_ARCHITECTURE.md) for React patterns

**For Interviewers:**
- Review [KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md) for debugging process
- Check [ARCHITECTURE.md](docs/ARCHITECTURE.md) for design decisions

---

## Contact & Support

For questions, issues, or suggestions:
- Create an issue in the repository
- Review documentation files in [docs/](docs/) folder
- Check [BUSINESS_OVERVIEW.md](BUSINESS_OVERVIEW.md) for business requirements
- See [POM_CONFIGURATION.md](POM_CONFIGURATION.md) for Maven setup details

---

**Last Updated:** January 4, 2026  
**Version:** v0.7.0 - Frontend UI Improvements  
**Status:** MVP Phase - Core Task Management + Optimized UI  
**Next Milestone:** Backend Task Filtering API & Priority Filter in Frontend (v0.8.0)
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
├── annotation.*        # Custom annotations (@Planned)
├── api.*               # REST Controllers
├── service.*           # Business logic services
├── repository.*        # Data access interfaces
├── entity.*            # JPA domain models
├── dto.*               # Request/Response DTOs
├── config.*            # Configuration classes
├── security.*          # JWT and security logic (planned)
├── exception.*         # Exception handling
├── event.*             # Event-driven components (planned)
└── util.*              # Utilities and constants (planned)
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

### Frontend Stack
- **React 19.2.0** - Modern UI library with concurrent features
- **Vite 7.2.4** - Lightning-fast build tool and dev server
- **Axios 1.13.2** - Promise-based HTTP client
- **ESLint 9.39.1** - Code quality and style checking

### Frontend Architecture Patterns
- **Component Composition** - Reusable UI components
- **useCallback** - Memoize functions for stable references
- **useMemo** - Memoize expensive computations
- **Controlled Components** - Form state management
- **Separation of Concerns** - Components, Pages, Styles, API layer

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
   
   **Local Development:**
   - Health Check: http://localhost:8080/actuator/health
   - Expected response: `{"status":"UP"}`
   
   **Production:**
   - Health Check: https://task-management-system-0c0p.onrender.com/actuator/health
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


### Test API Endpoints (Manual)

#### 1. Create a Task

**Local Development:**
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

**Production:**
```bash
POST https://task-management-system-0c0p.onrender.com/api/tasks
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

**Local Development:**
```bash
GET http://localhost:8080/api/tasks/1
Authorization: Basic YWRtaW46YWRtaW4xMjM=
```

**Production:**
```bash
GET https://task-management-system-0c0p.onrender.com/api/tasks/1
Authorization: Basic YWRtaW46YWRtaW4xMjM=
```

#### 3. Update Task

**Local Development:**
```bash
PUT http://localhost:8080/api/tasks/1
Content-Type: application/json
Authorization: Basic YWRtaW46YWRtaW4xMjM=

{
  "status": "IN_PROGRESS",
  "assigneeId": 2
}
```

**Production:**
```bash
PUT https://task-management-system-0c0p.onrender.com/api/tasks/1
Content-Type: application/json
Authorization: Basic YWRtaW46YWRtaW4xMjM=

{
  "status": "IN_PROGRESS",
  "assigneeId": 2
}
```

#### 4. Delete Task

**Local Development:**
```bash
DELETE http://localhost:8080/api/tasks/1
Authorization: Basic YWRtaW46YWRtaW4xMjM=
```

**Production:**
```bash
DELETE https://task-management-system-0c0p.onrender.com/api/tasks/1
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

All endpoints require **Basic Authentication** (except `/actuator/health`).


### Task Management APIs

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| **POST** | `/api/tasks` | Create new task with multiple assignees | ✅ Yes |
| **GET** | `/api/tasks/{id}` | Get task by ID (includes assignees, project) | ✅ Yes |
| **PUT** | `/api/tasks/{id}` | Update task details and assignees | ✅ Yes |
| **DELETE** | `/api/tasks/{id}` | Delete task (hard delete with cascade) | ✅ Yes |

**Request Body Example (POST /api/tasks):**
```json
{
  "title": "Fix login bug",
  "description": "Users cannot login with special characters",
  "priority": "HIGH",
  "dueDate": "2025-12-31T17:00:00",
  "estimatedHours": 8,
  "assigneeIds": [3, 7, 8],
  "projectId": 1
}
```

**Response Example (200 OK):**
```json
{
  "id": 7,
  "title": "Fix login bug",
  "status": "PENDING",
  "priority": "HIGH",
  "assignees": [
    {"id": 3, "username": "alice", "email": "alice@example.com"},
    {"id": 7, "username": "admin", "email": "admin@example.com"},
    {"id": 8, "username": "anna", "email": "anna@example.com"}
  ],
  "project": {"id": 1, "name": "Website Redesign", "active": true},
  "createdAt": "2025-12-20T23:25:38",
  "updatedAt": "2025-12-20T23:25:38"
}
```

---

### User Management APIs

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| **POST** | `/api/users` | Create new user | ✅ Yes |
| **GET** | `/api/users` | Get all active users | ✅ Yes |
| **GET** | `/api/users/{id}` | Get user by ID | ✅ Yes |
| **PUT** | `/api/users/{id}` | Update user details | ✅ Yes |
| **DELETE** | `/api/users/{id}` | Soft delete user (set deleted=true) | ✅ Yes |
| **POST** | `/api/users/{id}/restore` | Restore deleted user | ✅ Yes |

**Request Body Example (POST /api/users):**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "fullName": "John Doe"
}
```

**Business Rules:**
- DELETE: Soft delete (user remains in DB with `deleted=true`)
- Deleted users are automatically removed from task assignments via junction table cleanup

---

### Project Management APIs

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| **POST** | `/api/projects` | Create new project | ✅ Yes |
| **GET** | `/api/projects` | Get all active projects | ✅ Yes |
| **GET** | `/api/projects/{id}` | Get project by ID | ✅ Yes |
| **PUT** | `/api/projects/{id}` | Update project details | ✅ Yes |
| **DELETE** | `/api/projects/{id}` | Archive project (set active=false) | ✅ Yes |
| **POST** | `/api/projects/{id}/reactivate` | Reactivate archived project | ✅ Yes |
| **GET** | `/api/projects/{id}/tasks` | Get all tasks for a project | ✅ Yes |

**Request Body Example (POST /api/projects):**
```json
{
  "name": "Website Redesign",
  "description": "Redesign company website",
  "ownerId": 5,
  "startDate": "2025-12-20",
  "endDate": "2026-03-31"
}
```

---

### Health & Monitoring

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| **GET** | `/actuator/health` | Application health check | ❌ No |
| **GET** | `/actuator/metrics` | Application metrics | ✅ Yes |
| **GET** | `/actuator/prometheus` | Prometheus metrics | ✅ Yes |

---

### Testing APIs with cURL

#### Local Development

**Create Task:**
```bash
curl -X POST http://localhost:8080/api/tasks \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Task",
    "description": "Testing API",
    "priority": "HIGH",
    "dueDate": "2025-12-31T17:00:00",
    "assigneeIds": [3],
    "projectId": 1
  }'
```

**Get Task:**
```bash
curl -X GET http://localhost:8080/api/tasks/7 \
  -u admin:admin
```

**Get All Users:**
```bash
curl -X GET http://localhost:8080/api/users \
  -u admin:admin
```

**Health Check (No Auth):**
```bash
curl -X GET http://localhost:8080/actuator/health
```

#### Production

**Create Task:**
```bash
curl -X POST https://task-management-system-0c0p.onrender.com/api/tasks \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Task",
    "description": "Testing API",
    "priority": "HIGH",
    "dueDate": "2025-12-31T17:00:00",
    "assigneeIds": [3],
    "projectId": 1
  }'
```

**Get Task:**
```bash
curl -X GET https://task-management-system-0c0p.onrender.com/api/tasks/7 \
  -u admin:admin
```

**Get All Users:**
```bash
curl -X GET https://task-management-system-0c0p.onrender.com/api/users \
  -u admin:admin
```

**Health Check (No Auth):**
```bash
curl -X GET https://task-management-system-0c0p.onrender.com/actuator/health
```

---

### Not Yet Implemented

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | /api/tasks | List tasks with filters (status, assignee, project) | 🔲 Planned |
| POST | /api/tasks/{id}/comments | Add comment to task | 🔲 Planned |
| POST | /api/tasks/{id}/attachments | Upload file attachment | 🔲 Planned |
| POST | /api/auth/login | JWT authentication | 🔲 Planned |
| POST | /api/auth/register | User registration | 🔲 Planned |

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

**v0.7.0 (2026-01-04)** ⭐ Current Version
- ✅ **Frontend UI Improvements:**
  - Separated components: ProjectList, TaskList, TaskFilters
  - Component-based architecture
- ✅ **Frontend Features:**
  - Real-time search within tasks (title/description)
  - Status filter dropdown
  - Inline task status updates
  - Member ID badges with gradient styling
  - Responsive grid layout
- ✅ **Code Quality:**
  - Clear separation of concerns
  - Well-documented with comments
  - CSS improvements for better readability

**v0.6.0 (2025-12-20)**
- ✅ Fixed empty assignees issue in GET /api/tasks/{id}
- ✅ Implemented 3-step workaround for Hibernate @Where filter
- ✅ Added native query methods to bypass Hibernate filtering
- ✅ Enhanced SQL logging for debugging
- ✅ Many-to-Many assignees fully functional

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
- [ ] Implement backend task filtering API (GET /api/tasks?assigneeId=1&projectId=2&status=PENDING)
- [ ] Add Priority filter in frontend UI
- [ ] Add WebSocket for real-time task updates
- [ ] Add Project management API (fully implemented)
- [ ] Support removing assignee from tasks
- [ ] Implement soft delete for tasks
- [ ] Add pagination and sorting
- [ ] Implement JWT authentication
- [ ] Add role-based access control (RBAC)
- [ ] Event-driven notifications
- [ ] File attachment upload API
- [ ] Task comments API
- [ ] Unit and integration tests
- [ ] API rate limiting
- [ ] Caching layer (Redis)
- [ ] Migration from @Where to @FilterDef

---

## Frontend Development Guide

### Running Frontend Locally

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies (first time only)
npm install

# Run development server
npm run dev

# Open browser at http://localhost:5173
```

### Building for Production

```bash
# Build optimized production bundle
npm run build

# Preview production build
npm run preview
```

### Frontend Architecture Explained

**Component Structure:**
```
App.jsx (Root)
└── DashboardPage (Parent Container in pages/)
    ├── ProjectList (in pages/)
    │   └── Handles project selection
    ├── TaskList (in pages/)
    │   ├── TaskFilters (in pages/)
    │   │   ├── Search Input
    │   │   └── Status Dropdown
    │   └── Task Cards
    ├── Modal (Shared in components/)
    └── Team Members Section
```

**State Management:**
- Filter state lives inside TaskList component
- Filters don't reset when switching projects
- Parent (DashboardPage) manages global state (projects, tasks, users)
- Children receive props via props drilling

---

## Performance Optimization Tips

### Backend
1. **Use indexes** on frequently queried columns
2. **Lazy loading** for relationships (avoid N+1 queries)
3. **Native queries** when Hibernate filters cause issues
4. **Connection pooling** with HikariCP (already configured)
5. **@Transactional** for database consistency

### Frontend
1. **React.memo** for expensive components
2. **useCallback** for event handlers passed as props
3. **useMemo** for expensive calculations (filtering, sorting)
4. **Debounce** search input (wait 300ms before filtering)
5. **Virtualization** for long lists (100+ items)
6. **Code splitting** with React.lazy() for routes
useCallback** for event handlers passed as props
2. **useMemo** for expensive calculations (filtering, sorting)
3. **Debounce** search input (wait 300ms before filtering)
4. **Virtualization** for long lists (100+ items)
5
For questions, issues, or suggestions:
- Create an issue in the repository
- Review documentation in package README files
- Check [BUSINESS_OVERVIEW.md](BUSINESS_OVERVIEW.md) for requirements

---

**Last Updated:** January 4, 2026  
**Version:** v0.7.0 - Frontend Performance Optimizations with React.memo  
**Status:** MVP Phase - Core Task Management + Optimized UI  
**Next Milestone:** Backend Task Filtering API & Priority Filter in Frontend
