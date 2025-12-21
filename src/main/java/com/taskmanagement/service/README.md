# Service Layer (Business Logic)

##  Overview

The **Service layer** implements the core business logic of the application. Services orchestrate operations between controllers (API requests) and repositories (data access), managing transactions, validation, and complex workflows.

**Location:** `src/main/java/com/taskmanagement/service/`

**Responsibility:** Implement business rules, coordinate data operations, manage transactions, and provide the API for application use cases

---

##  Current Implementation Status

### ✅ Implemented Services
- **TaskService.java** - Complete CRUD operations for tasks with Many-to-Many assignees
  - `createTask()` - Create task with multiple assignees (N:N relationship)
  - `getTaskById()` - Retrieve task with 3-step workaround for @Where filter issues
    - Uses native SQL to load Task
    - Queries assignee IDs separately to bypass Hibernate filter
    - Manually populates assignees collection
  - `updateTask()` - Update task fields and assignees
  - `deleteTask()` - Hard delete with cascade to comments/attachments

- **UserService.java** - User management with soft delete
  - `getUserById()` - Retrieve user by ID (active users only)
  - `getAllUsers()` - Get all active users
  - `deleteUser()` - Soft delete with bulk unassign tasks
  - `restoreUser()` - Restore deleted user

- **ProjectService.java** - Project management operations
  - `createProject()` - Create new project
  - `getProjectById()` - Retrieve project by ID
  - `updateProject()` - Update project details
  - `deleteProject()` - Delete project (hard delete)

### 🔲 Not Yet Implemented
- CommentService.java - Comment operations
- NotificationService.java - Event-driven notifications
- Event publishing system
- UserService CREATE/UPDATE operations (only GET/DELETE/RESTORE)
- Migration from @Where to @FilterDef for better lazy loading support

---

##  Core Responsibilities

### 1. Business Logic Implementation
- Implement domain business rules and workflows
- Validate data according to business constraints
- Enforce state transitions and lifecycle management
- Coordinate operations across multiple entities

### 2. Transaction Management
- Define transaction boundaries with @Transactional
- Manage read-only transactions for performance
- Handle rollback on errors
- Support distributed transactions with sagas

### 3. Orchestration & Coordination
- Coordinate between repositories
- Combine multiple operations into workflows
- Call external services and APIs
- Handle dependencies between entities

### 4. Event Publishing
- Publish domain events from business operations
- Track state changes and important events
- Enable event-driven workflows
- Support audit trails and event sourcing

### 5. Exception Handling
- Validate business constraints
- Throw domain-specific exceptions
- Handle and translate repository exceptions
- Provide meaningful error context

---

##  Service Files

```
service/
├── TaskService.java       ✅ Task CRUD with Many-to-Many assignees + @Where workaround
├── UserService.java       ✅ User management with soft delete (GET/DELETE/RESTORE)
├── ProjectService.java    ✅ Project CRUD operations (fully implemented)
└── README.md              📄 This file
```

**Note:** No base service class or service utilities - simple direct implementation

---

##  TaskService Implementation

### Complete Service Code Structure

```java
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    
    // ========== DEPENDENCIES ==========
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    
    // ========== PUBLIC METHODS ==========
    
    /**
     * Create new task
     * Validates: assignee exists, project exists
     * Returns: TaskResponse DTO
     */
    public TaskResponse createTask(CreateTaskRequest request) {
        // Business logic implementation
    }
    
    /**
     * Get task by ID (read-only transaction)
     * Handles: lazy loading of relationships
     * Throws: TaskNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        // Fetch and convert to DTO
    }
    
    /**
     * Update task (partial update)
     * Handles: assignee change, status transitions
     * Validates: new assignee/project exists
     */
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        // Update logic with validation
    }
    
    /**
     * Delete task (hard delete)
     * Cascade: deletes comments and attachments
     * Throws: TaskNotFoundException if not found
     */
    public void deleteTask(Long id) {
        // Delete with cascade
    }
    
    /**
     * Get all entities with pagination
     */
    public Page<T> findAll(Pageable pageable) {
        return getRepository().findAll(pageable);
    }
    
    /**
     * Count total entities
     */
    public long count() {
        return getRepository().count();
    }
    
    /**
     * Publish domain event
     */
    protected void publishEvent(Object event) {
        log.debug("Publishing event: {}", event.getClass().getSimpleName());
        getEventPublisher().publishEvent(event);
    }
    
    /**
     * Log service operation
     */
    protected void logOperation(String operation, ID id) {
        log.info("{} - ID: {}", operation, id);
    }
}
\\\

---

##  Method Details

### 1. createTask() - Create New Task

**Business Flow:**
1. Validate assignee exists (required)
2. Validate project exists (required)
3. Create Task entity with default status = PENDING
4. Save to database
5. Convert to TaskResponse DTO
6. Return response

**Code:**
```java
public TaskResponse createTask(CreateTaskRequest request) {
    log.info("Creating task: title={}, assigneeId={}, projectId={}", 
        request.getTitle(), request.getAssigneeId(), request.getProjectId());
    
    // STEP 1: Validate Assignee
    User assignee = userRepository.findById(request.getAssigneeId())
        .orElseThrow(() -> {
            log.error("Assignee not found: id={}", request.getAssigneeId());
            return new UserNotFoundException(request.getAssigneeId());
        });
    
    // STEP 2: Validate Project
    Project project = projectRepository.findById(request.getProjectId())
        .orElseThrow(() -> {
            log.error("Project not found: id={}", request.getProjectId());
            return new ProjectNotFoundException(request.getProjectId());
        });
    
    // STEP 3: Create Task Entity
    Task task = Task.builder()
        .title(request.getTitle())
        .description(request.getDescription())
        .assignee(assignee)
        .project(project)
        .priority(request.getPriority())
        .dueDate(request.getDueDate())
        .estimatedHours(request.getEstimatedHours())
        .status(TaskStatus.PENDING)  // Default status
        .build();
    
    // STEP 4: Save to Database
    Task savedTask = taskRepository.save(task);
    
    log.info("Task saved successfully: taskId={}", savedTask.getId());
    
    // STEP 5: Convert to DTO
    TaskResponse response = TaskResponse.from(savedTask);
    
    return response;
}
        return savedTask;
    }
    
    /**
     * Get task by ID
     * 
     * @param id Task ID
     * @return Task
     * @throws TaskNotFoundException if task doesn't exist
     */
    @Transactional(readOnly = true)
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));
    }
    
    /**
     * Update task with optimistic locking
     * 
     * @param id Task ID
     * @param request Update request
     * @return Updated task
     * @throws TaskNotFoundException if task doesn't exist
     * @throws TaskAlreadyCompletedException if task is completed
     * @throws OptimisticLockException if task was modified concurrently
     */
    public Task updateTask(Long id, UpdateTaskRequest request) {
        log.info("Updating task: {}", id);
        
        Task task = getTaskById(id);
        
        // Prevent updates to completed tasks
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new TaskAlreadyCompletedException(id);
        }
        
        // Update allowed fields
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        
        // Update assignee if provided
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                .orElseThrow(() -> new UserNotFoundException(request.getAssigneeId()));
            task.setAssignee(assignee);
        }
        
        // Save with optimistic locking
        try {
            Task updatedTask = taskRepository.save(task);
            
            // Publish event
            eventPublisher.publishEvent(
                TaskUpdatedEvent.from(updatedTask, getCurrentUserId())
            );
            
            log.info("Task updated successfully: {}", id);
            return updatedTask;
        } catch (OptimisticLockingFailureException e) {
            log.warn("Concurrent modification detected for task: {}", id);
            throw new ConflictException("Task was modified by another user. Please refresh and try again.");
        }
    }
    
    /**
     * Change task status
     * 
     * @param id Task ID
     * @param newStatus New status
     * @return Updated task
     * @throws TaskNotFoundException if task doesn't exist
     * @throws InvalidTaskStatusException if transition is invalid
     * @throws TaskAlreadyCompletedException if task is already completed
     */
    public Task changeStatus(Long id, TaskStatus newStatus) {
        log.info("Changing task {} status to: {}", id, newStatus);
        
        Task task = getTaskById(id);
        TaskStatus currentStatus = task.getStatus();
        
        // Validate status transition
        if (!task.canTransitionTo(newStatus)) {
            throw new InvalidTaskStatusException(
                currentStatus.name(),
                newStatus.name(),
                "Status transition not allowed"
            );
        }
        
        // Prevent status changes on completed tasks
        if (currentStatus == TaskStatus.COMPLETED) {
            throw new TaskAlreadyCompletedException(id);
        }
        
        // Update status
        task.setStatus(newStatus);
        Task updatedTask = taskRepository.save(task);
        
        // Publish event based on status
        if (newStatus == TaskStatus.COMPLETED) {
            eventPublisher.publishEvent(
                TaskCompletedEvent.from(updatedTask, getCurrentUserId())
            );
        } else if (newStatus == TaskStatus.IN_PROGRESS) {
            eventPublisher.publishEvent(
                TaskStartedEvent.from(updatedTask, getCurrentUserId())
            );
        }
        
        log.info("Task status changed successfully: {} -> {}", id, newStatus);
        return updatedTask;
    }
    
    /**
     * Assign task to user
     * 
     * @param taskId Task ID
     * @param assigneeId User ID to assign to
     * @return Updated task
     */
    public Task assignTask(Long taskId, Long assigneeId) {
        log.info("Assigning task {} to user {}", taskId, assigneeId);
        
        Task task = getTaskById(taskId);
        User assignee = userRepository.findById(assigneeId)
            .orElseThrow(() -> new UserNotFoundException(assigneeId));
        
        // Validate assignee is active
        if (!assignee.isActive()) {
            throw new BusinessException("Cannot assign task to inactive user");
        }
        
        // Validate assignee is project member
        if (!task.getProject().getMembers().contains(assignee)) {
            throw new UserNotInProjectException(assigneeId, task.getProject().getId());
        }
        
        task.setAssignee(assignee);
        Task updatedTask = taskRepository.save(task);
        
        // Publish event
        eventPublisher.publishEvent(
            TaskAssignedEvent.from(updatedTask, assigneeId, getCurrentUserId())
        );
        
        log.info("Task assigned successfully: {} -> {}", taskId, assigneeId);
        return updatedTask;
    }
    
    /**
     * Delete task
     * 
     * @param id Task ID
     * @throws TaskNotFoundException if task doesn't exist
     * @throws TaskAlreadyCompletedException if task is completed
     */
    public void deleteTask(Long id) {
        log.info("Deleting task: {}", id);
        
        Task task = getTaskById(id);
        
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new TaskAlreadyCompletedException(id);
        }
        
        taskRepository.delete(task);
        
        // Publish event
        eventPublisher.publishEvent(
            TaskDeletedEvent.from(task, getCurrentUserId())
        );
        
        log.info("Task deleted successfully: {}", id);
    }
    
    /**
     * Find tasks by status with pagination
     * 
     * @param status Task status
     * @param pageable Pagination
     * @return Page of tasks
     */
    @Transactional(readOnly = true)
    public Page<Task> findByStatus(TaskStatus status, Pageable pageable) {
        return taskRepository.findByStatus(status, pageable);
    }
    
    /**
     * Find tasks assigned to user with pagination
     * 
     * @param userId User ID
     * @param pageable Pagination
     * @return Page of tasks
     */
    @Transactional(readOnly = true)
    public Page<Task> findByAssignee(Long userId, Pageable pageable) {
        User assignee = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        return taskRepository.findByAssignee(assignee, pageable);
    }
    
    /**
     * Find overdue tasks
     * 
     * @param pageable Pagination
     * @return Page of overdue tasks
     */
    @Transactional(readOnly = true)
    public Page<Task> findOverdueTasks(Pageable pageable) {
        return taskRepository.findOverdueTasks(pageable);
    }
    
    /**
     * Find tasks by project with eager loading to prevent N+1 queries
     * 
     * @param projectId Project ID
     * @return List of tasks with relationships loaded
     */
    @Transactional(readOnly = true)
    public List<Task> findByProjectIdWithRelations(Long projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));
        
        return taskRepository.findByProjectIdWithRelations(projectId);
    }
    
    /**
     * Search tasks with complex criteria
     * 
     * @param criteria Search criteria
     * @param pageable Pagination
     * @return Page of matching tasks
     */
    @Transactional(readOnly = true)
    public Page<Task> searchTasks(TaskSearchCriteria criteria, Pageable pageable) {
        Specification<Task> spec = Specification
            .where(TaskSpecification.inProject(criteria.getProjectId()))
            .and(TaskSpecification.hasStatus(criteria.getStatus()))
            .and(TaskSpecification.hasPriority(criteria.getPriority()));
        
        if (criteria.getAssigneeId() != null) {
            spec = spec.and(TaskSpecification.assignedTo(criteria.getAssigneeId()));
        }
        
        return taskRepository.findAll(spec, pageable);
    }
    
    /**
     * Get current authenticated user ID
     */
    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }
}
\\\

---

##  User Service Implementation

\\\java
/**
 * User Service
 * Implements user management and registration logic
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * Register a new user
     * 
     * @param request Registration request
     * @return Registered user
     * @throws DuplicateEmailException if email already exists
     * @throws UserAlreadyExistsException if username already exists
     */
    public User registerUser(CreateUserRequest request) {
        log.info("Registering new user: {}", request.getUsername());
        
        // Check username uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException(request.getUsername());
        }
        
        // Check email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }
        
        // Create user
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .fullName(request.getFullName())
            .password(passwordEncoder.encode(request.getPassword()))
            .active(true)
            .locked(false)
            .build();
        
        // Save user
        User savedUser = userRepository.save(user);
        
        // Publish event
        eventPublisher.publishEvent(UserRegisteredEvent.from(savedUser));
        
        log.info("User registered successfully: {}", savedUser.getId());
        return savedUser;
    }
    
    /**
     * Update user profile
     * 
     * @param userId User ID
     * @param request Update request
     * @return Updated user
     */
    public User updateProfile(Long userId, UpdateUserRequest request) {
        log.info("Updating user profile: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        // Update allowed fields
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null && !user.getEmail().equals(request.getEmail())) {
            // Check email uniqueness
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateEmailException(request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        
        User updatedUser = userRepository.save(user);
        
        // Publish event
        eventPublisher.publishEvent(UserUpdatedEvent.from(updatedUser));
        
        log.info("User profile updated successfully: {}", userId);
        return updatedUser;
    }
    
    /**
     * Change user password
     * 
     * @param userId User ID
     * @param oldPassword Current password
     * @param newPassword New password
     * @throws InvalidCredentialsException if old password is incorrect
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("Changing password for user: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("Password changed successfully for user: {}", userId);
    }
    
    /**
     * Get user by ID
     * 
     * @param userId User ID
     * @return User
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }
    
    /**
     * Get user by username
     * 
     * @param username Username
     * @return User
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException(username));
    }
    
    /**
     * Activate user account
     * 
     * @param userId User ID
     */
    public void activateUser(Long userId) {
        log.info("Activating user: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        user.setActive(true);
        userRepository.save(user);
        
        log.info("User activated successfully: {}", userId);
    }
    
    /**
     * Deactivate user account
     * 
     * @param userId User ID
     */
    public void deactivateUser(Long userId) {
        log.info("Deactivating user: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        user.setActive(false);
        userRepository.save(user);
        
        log.info("User deactivated successfully: {}", userId);
    }
    
    /**
     * Find all users with pagination
     * 
     * @param pageable Pagination
     * @return Page of users
     */
    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    /**
     * Find active users with pagination
     * 
     * @param pageable Pagination
     * @return Page of active users
     */
    @Transactional(readOnly = true)
    public Page<User> findActiveUsers(Pageable pageable) {
        return userRepository.findByActiveTrue(pageable);
    }
}
\\\

---

##  Project Service Implementation

\\\java
/**
 * Project Service
 * Implements project management logic
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProjectService {
    
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * Create a new project
     * 
     * @param request Project creation request
     * @return Created project
     */
    public Project createProject(CreateProjectRequest request) {
        log.info("Creating project: {}", request.getName());
        
        // Create project
        Project project = Project.builder()
            .name(request.getName())
            .description(request.getDescription())
            .active(true)
            .build();
        
        // Add creator as member
        User creator = userRepository.findById(request.getCreatorId())
            .orElseThrow(() -> new UserNotFoundException(request.getCreatorId()));
        project.addMember(creator);
        
        // Save project
        Project savedProject = projectRepository.save(project);
        
        // Publish event
        eventPublisher.publishEvent(ProjectCreatedEvent.from(savedProject));
        
        log.info("Project created successfully: {}", savedProject.getId());
        return savedProject;
    }
    
    /**
     * Update project
     * 
     * @param id Project ID
     * @param request Update request
     * @return Updated project
     */
    public Project updateProject(Long id, UpdateProjectRequest request) {
        log.info("Updating project: {}", id);
        
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new ProjectNotFoundException(id));
        
        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        
        Project updatedProject = projectRepository.save(project);
        
        // Publish event
        eventPublisher.publishEvent(ProjectUpdatedEvent.from(updatedProject));
        
        log.info("Project updated successfully: {}", id);
        return updatedProject;
    }
    
    /**
     * Add member to project
     * 
     * @param projectId Project ID
     * @param userId User ID to add
     * @return Updated project
     */
    public Project addMember(Long projectId, Long userId) {
        log.info("Adding member {} to project {}", userId, projectId);
        
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        if (!user.isActive()) {
            throw new BusinessException("Cannot add inactive user to project");
        }
        
        project.addMember(user);
        Project updatedProject = projectRepository.save(project);
        
        // Publish event
        eventPublisher.publishEvent(
            MemberAddedEvent.from(updatedProject, user.getId())
        );
        
        log.info("Member added successfully to project: {}", projectId);
        return updatedProject;
    }
    
    /**
     * Remove member from project
     * 
     * @param projectId Project ID
     * @param userId User ID to remove
     * @return Updated project
     */
    public Project removeMember(Long projectId, Long userId) {
        log.info("Removing member {} from project {}", userId, projectId);
        
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        project.removeMember(user);
        Project updatedProject = projectRepository.save(project);
        
        // Unassign all tasks from user
        taskRepository.findByAssignee(user)
            .forEach(task -> {
                if (task.getProject().equals(project)) {
                    task.setAssignee(null);
                    taskRepository.save(task);
                }
            });
        
        // Publish event
        eventPublisher.publishEvent(
            MemberRemovedEvent.from(updatedProject, user.getId())
        );
        
        log.info("Member removed successfully from project: {}", projectId);
        return updatedProject;
    }
    
    /**
     * Get project by ID
     * 
     * @param id Project ID
     * @return Project
     */
    @Transactional(readOnly = true)
    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
            .orElseThrow(() -> new ProjectNotFoundException(id));
    }
    
    /**
     * Find projects for user
     * 
     * @param userId User ID
     * @param pageable Pagination
     * @return Page of projects
     */
    @Transactional(readOnly = true)
    public Page<Project> findByMember(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        return projectRepository.findByMembersContaining(user, pageable);
    }
    
    /**
     * Delete project
     * 
     * @param id Project ID
     */
    public void deleteProject(Long id) {
        log.info("Deleting project: {}", id);
        
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new ProjectNotFoundException(id));
        
        // Check if project has active tasks
        long activeTasks = taskRepository.countByProjectAndStatusNot(project, TaskStatus.COMPLETED);
        if (activeTasks > 0) {
            throw new BusinessException("Cannot delete project with active tasks");
        }
        
        projectRepository.delete(project);
        
        // Publish event
        eventPublisher.publishEvent(ProjectDeletedEvent.from(project));
        
        log.info("Project deleted successfully: {}", id);
    }
}
\\\

---

##  Service Transaction Management

\\\java
/**
 * Transaction management patterns and best practices
 */

// Example 1: Read-only service method
@Transactional(readOnly = true)
public Page<Task> searchTasks(TaskSearchCriteria criteria, Pageable pageable) {
    // read-only transactions are optimized by the database
    return taskRepository.findAll(spec, pageable);
}

// Example 2: Write transaction with rollback
@Transactional(rollbackFor = Exception.class)
public Task updateTask(Long id, UpdateTaskRequest request) {
    Task task = getTaskById(id);
    task.update(request);
    return taskRepository.save(task);
    // Rolls back automatically if any exception occurs
}

// Example 3: Nested transaction with isolation level
@Transactional(isolation = Isolation.SERIALIZABLE)
public void criticalOperation(Long id) {
    // SERIALIZABLE provides maximum isolation
    Task task = getTaskById(id);
    task.updateCriticalField();
    taskRepository.save(task);
}

// Example 4: Transaction with timeout
@Transactional(timeout = 30)  // 30 second timeout
public void longRunningOperation() {
    // Will rollback if takes longer than 30 seconds
}

// Example 5: Propagation patterns
@Service
public class CompositeService {
    
    private final TaskService taskService;
    private final ProjectService projectService;
    
    @Transactional
    public void complexWorkflow() {
        // Both service calls participate in same transaction
        taskService.createTask(...);
        projectService.updateProject(...);
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void independentOperation() {
        // Creates new transaction, independent of caller
    }
}
\\\

---

##  Service Best Practices

### 1. Keep Services Focused

\\\java
// GOOD: Single responsibility
@Service
public class TaskService {
    public Task createTask(CreateTaskRequest request) { }
    public Task updateTask(Long id, UpdateTaskRequest request) { }
    public void deleteTask(Long id) { }
}

// BAD: Multiple responsibilities
@Service
public class TaskService {
    public Task createTask(CreateTaskRequest request) { }
    public void sendNotifications() { }  // Belongs in NotificationService
    public void generateReport() { }     // Belongs in ReportService
    public void processPayment() { }     // Doesn't belong here at all
}
\\\

### 2. Use Read-Only Transactions for Queries

\\\java
// GOOD: Read-only for performance
@Transactional(readOnly = true)
public Page<Task> findTasks(Pageable pageable) {
    return taskRepository.findAll(pageable);
}

// BAD: Write transaction for query
@Transactional
public Page<Task> findTasks(Pageable pageable) {
    return taskRepository.findAll(pageable);
}
\\\

### 3. Publish Events from Services

\\\java
// GOOD: Publish domain events
@Transactional
public Task createTask(CreateTaskRequest request) {
    Task task = new Task(...);
    Task savedTask = taskRepository.save(task);
    
    eventPublisher.publishEvent(TaskCreatedEvent.from(savedTask));
    return savedTask;
}

// BAD: Direct coupling to notification
@Transactional
public Task createTask(CreateTaskRequest request) {
    Task task = new Task(...);
    Task savedTask = taskRepository.save(task);
    
    notificationService.notifyTaskCreated(savedTask);  // Direct coupling
    return savedTask;
}
\\\

### 4. Validate Business Rules in Services

\\\java
// GOOD: Validate business constraints
@Transactional
public Task changeStatus(Long id, TaskStatus newStatus) {
    Task task = getTaskById(id);
    
    if (!task.canTransitionTo(newStatus)) {
        throw new InvalidTaskStatusException(...);
    }
    
    if (task.getStatus() == TaskStatus.COMPLETED) {
        throw new TaskAlreadyCompletedException(id);
    }
    
    task.setStatus(newStatus);
    return taskRepository.save(task);
}

// BAD: No business validation
@Transactional
public Task changeStatus(Long id, TaskStatus newStatus) {
    Task task = getTaskById(id);
    task.setStatus(newStatus);
    return taskRepository.save(task);
}
\\\

### 5. Handle Exceptions Appropriately

\\\java
// GOOD: Translate repository exceptions to domain exceptions
@Transactional
public Task updateTask(Long id, UpdateTaskRequest request) {
    try {
        Task task = getTaskById(id);
        task.update(request);
        return taskRepository.save(task);
    } catch (OptimisticLockingFailureException e) {
        throw new ConflictException("Task was modified by another user");
    }
}

// BAD: Let low-level exceptions propagate
@Transactional
public Task updateTask(Long id, UpdateTaskRequest request) {
    Task task = getTaskById(id);
    task.update(request);
    return taskRepository.save(task);  // OptimisticLockingFailureException bubbles up
}
\\\

### 6. Use Pagination for Large Result Sets

\\\java
// GOOD: Paginate results
@Transactional(readOnly = true)
public Page<Task> findTasks(Pageable pageable) {
    return taskRepository.findAll(pageable);
}

// BAD: Load all records
@Transactional(readOnly = true)
public List<Task> findTasks() {
    return taskRepository.findAll();  // Could cause OutOfMemoryException
}
\\\

### 7. Prevent N+1 Queries

\\\java
// GOOD: Use JOIN FETCH to eager load
@Transactional(readOnly = true)
public List<Task> getProjectTasks(Long projectId) {
    // Query includes relationships in single query
    return taskRepository.findByProjectIdWithRelations(projectId);
}

// BAD: Causes N+1 queries
@Transactional(readOnly = true)
public List<Task> getProjectTasks(Long projectId) {
    List<Task> tasks = taskRepository.findByProjectId(projectId);
    // Each task.getAssignee() triggers additional query
    return tasks;
}
\\\

---

##  Service DTOs

\\\java
/**
 * Task creation request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskRequest {
    
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotNull(message = "Priority is required")
    private TaskPriority priority;
    
    @NotNull(message = "Project ID is required")
    private Long projectId;
    
    @Future(message = "Due date must be in the future")
    private LocalDateTime dueDate;
    
    private Long assigneeId;
}

/**
 * Task update request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequest {
    private String title;
    private String description;
    private TaskPriority priority;
    private LocalDateTime dueDate;
    private Long assigneeId;
}

/**
 * Task search criteria
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskSearchCriteria {
    private Long projectId;
    private TaskStatus status;
    private TaskPriority priority;
    private Long assigneeId;
    private String titleContains;
}
\\\

---

##  Service Testing

\\\java
/**
 * Service layer unit tests
 */
@SpringBootTest
class TaskServiceTest {
    
    @Autowired
    private TaskService taskService;
    
    @MockBean
    private TaskRepository taskRepository;
    
    @MockBean
    private UserRepository userRepository;
    
    @MockBean
    private ProjectRepository projectRepository;
    
    @MockBean
    private ApplicationEventPublisher eventPublisher;
    
    @Test
    void createTask_WithValidRequest_CreatesTask() {
        // Arrange
        CreateTaskRequest request = CreateTaskRequest.builder()
            .title("Test Task")
            .description("Test Description")
            .priority(TaskPriority.MEDIUM)
            .projectId(1L)
            .build();
        
        Project project = new Project();
        project.setId(1L);
        
        Task expectedTask = Task.builder()
            .title("Test Task")
            .status(TaskStatus.PENDING)
            .build();
        
        when(projectRepository.findById(1L))
            .thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class)))
            .thenReturn(expectedTask);
        
        // Act
        Task result = taskService.createTask(request);
        
        // Assert
        assertNotNull(result);
        assertEquals("Test Task", result.getTitle());
        assertEquals(TaskStatus.PENDING, result.getStatus());
        verify(eventPublisher).publishEvent(any(TaskCreatedEvent.class));
    }
    
    @Test
    void changeStatus_WithInvalidTransition_ThrowsException() {
        // Arrange
        Task task = Task.builder()
            .id(1L)
            .status(TaskStatus.COMPLETED)
            .build();
        
        when(taskRepository.findById(1L))
            .thenReturn(Optional.of(task));
        
        // Act & Assert
        assertThrows(TaskAlreadyCompletedException.class, () -> {
            taskService.changeStatus(1L, TaskStatus.PENDING);
        });
    }
}
\\\

---

##  Service Checklist

When implementing services:

- [ ] Define clear service boundaries and responsibilities
- [ ] Use @Transactional with appropriate settings (readOnly, propagation, timeout)
- [ ] Validate business rules and constraints
- [ ] Publish domain events from operations
- [ ] Handle repository exceptions and translate to domain exceptions
- [ ] Use pagination for large result sets
- [ ] Prevent N+1 queries with eager loading
- [ ] Keep service methods focused and cohesive
- [ ] Document business logic in method comments
- [ ] Test services with unit tests
- [ ] Use dependency injection for repositories and services
- [ ] Avoid direct calls between services when possible
- [ ] Use specifications for complex queries
- [ ] Implement proper error handling
- [ ] Log important business operations

---

##  Related Documentation

- **ARCHITECTURE.md** - Service layer architecture
- **README.md** - Main project overview
- **Repository Layer** - Data access patterns
- **Event Layer** - Event publishing patterns
- **Exception Layer** - Exception handling

---

##  Quick Reference

### Transaction Annotations

\\\
@Transactional                                  - Start transaction (default read-write)
@Transactional(readOnly = true)                - Optimize for reads only
@Transactional(propagation = Propagation.REQUIRES_NEW)  - New transaction
@Transactional(isolation = Isolation.SERIALIZABLE)      - Maximum isolation
@Transactional(timeout = 30)                   - 30 second timeout
@Transactional(rollbackFor = Exception.class) - Rollback on any exception
\\\

### Service Scope

\\\
Controller Layer      HTTP requests, routing, validation
    
Service Layer        Business logic, transactions, orchestration
    
Repository Layer     Data access, queries
    
Database            Persistent storage
\\\

### Service Responsibilities

\\\
 Business logic implementation
 Transaction management
 Input validation and constraints
 Domain event publishing
 Orchestration of multiple repositories
 Exception translation
 HTTP request/response handling (Controller)
 Direct data queries (Repository)
 Database schema definition (Entity)
\\\

```

**Exceptions Thrown:**
- `UserNotFoundException` (404) - Assignee ID not found
- `ProjectNotFoundException` (404) - Project ID not found

**Validations:**
- DTO validations (@Valid) - title, description, priority, dueDate
- Assignee must exist in database
- Project must exist in database

---

### 2. getTaskById() - Retrieve Task

**Business Flow:**
1. Find task by ID
2. Trigger lazy loading of assignee and project
3. Convert to TaskResponse DTO
4. Return response

**Code:**
```java
@Transactional(readOnly = true)
public TaskResponse getTaskById(Long id) {
    log.info("Fetching task by ID: {}", id);
    
    // STEP 1: Find Task
    Task task = taskRepository.findById(id)
        .orElseThrow(() -> {
            log.error("Task not found: id={}", id);
            return new TaskNotFoundException(id);
        });
    
    // STEP 2: Trigger Lazy Loading
    String assigneeName = task.getAssignee().getFullName();
    String projectName = task.getProject().getName();
    
    // STEP 3: Convert to DTO
    TaskResponse response = TaskResponse.from(task);
    
    return response;
}
```

**Key Points:**
- `@Transactional(readOnly = true)` - Optimized for read operations
- Lazy loading triggered within transaction
- Returns full task details with assignee and project

---

### 3. updateTask() - Update Task

**Business Flow:**
1. Find existing task
2. Validate new assignee if changed
3. Update only provided fields (partial update)
4. Handle status transitions (set completedAt if COMPLETED)
5. Save changes
6. Return updated TaskResponse

**Code:**
```java
public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
    log.info("Updating task: id={}", id);
    
    // STEP 1: Find Task
    Task task = taskRepository.findById(id)
        .orElseThrow(() -> new TaskNotFoundException(id));
    
    // STEP 2: Update Assignee (if changed)
    if (request.getAssigneeId() != null) {
        if (!task.getAssignee().getId().equals(request.getAssigneeId())) {
            User newAssignee = userRepository.findById(request.getAssigneeId())
                .orElseThrow(() -> new UserNotFoundException(request.getAssigneeId()));
            task.setAssignee(newAssignee);
        }
    }
    
    // STEP 3: Update Other Fields
    if (request.getTitle() != null) task.setTitle(request.getTitle());
    if (request.getDescription() != null) task.setDescription(request.getDescription());
    if (request.getStatus() != null) task.setStatus(request.getStatus());
    if (request.getPriority() != null) task.setPriority(request.getPriority());
    if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
    
    // STEP 4: Handle Status Transitions
    if (request.getStatus() == TaskStatus.COMPLETED && task.getCompletedAt() == null) {
        task.setCompletedAt(LocalDateTime.now());
    }
    
    // STEP 5: Save
    Task updatedTask = taskRepository.save(task);
    
    return TaskResponse.from(updatedTask);
}
```

**Key Features:**
- Partial update - only provided fields are changed
- Optimized assignee update - only queries if ID actually changed
- Automatic completedAt timestamp when status → COMPLETED

---

### 4. deleteTask() - Delete Task

**Business Flow:**
1. Validate task exists
2. Delete task (cascade to comments and attachments)
3. No return value (void)

**Code:**
```java
public void deleteTask(Long id) {
    log.info("Deleting task: id={}", id);
    
    // STEP 1: Validate Exists
    Task task = taskRepository.findById(id)
        .orElseThrow(() -> new TaskNotFoundException(id));
    
    // STEP 2: Delete (cascade to related entities)
    taskRepository.delete(task);
    
    log.info("Task deleted successfully: id={}", id);
}
```

**Cascade Behavior:**
- ✅ Comments deleted automatically (orphanRemoval = true)
- ✅ Attachments deleted automatically (orphanRemoval = true)
- ❌ User NOT deleted (no cascade)
- ❌ Project NOT deleted (no cascade)

---

## Transaction Management

### @Transactional Annotation

**Class Level:**
```java
@Service
@Transactional  // All methods are transactional by default
public class TaskService {
```

**Benefits:**
- Automatic transaction commit on success
- Automatic rollback on exception
- Database consistency guaranteed

**Read-Only Optimization:**
```java
@Transactional(readOnly = true)
public TaskResponse getTaskById(Long id) {
    // Optimized for SELECT queries
    // No flush, no dirty checking
}
```

---

## Exception Handling

**Service Layer Exceptions:**
```java
// Thrown by service, caught by GlobalExceptionHandler
throw new TaskNotFoundException(id);        // → 404 Not Found
throw new UserNotFoundException(id);        // → 404 Not Found  
throw new ProjectNotFoundException(id);     // → 404 Not Found
```

**Controller receives:**
```
HTTP 404 Not Found
{
  "timestamp": "2025-12-14T10:30:00",
  "status": 404,
  "error": "Task Not Found",
  "message": "Task not found with ID: 123"
}
```

---

## Known Limitations

### ❌ Missing Features

**1. No task filtering**
```java
// This doesn't exist yet:
List<TaskResponse> getTasksByAssignee(Long assigneeId);
List<TaskResponse> getTasksByProject(Long projectId);
List<TaskResponse> filterTasks(Long assigneeId, Long projectId, String status);
```

**2. No pagination**
```java
// This doesn't exist yet:
Page<TaskResponse> getAllTasks(Pageable pageable);
```

**3. No soft delete support**
```java
// Defined in entity but not implemented:
void softDeleteTask(Long id);
void restoreTask(Long id);
```

**4. No event publishing**
```java
// No events published after operations:
// - TaskCreatedEvent
// - TaskUpdatedEvent
// - TaskDeletedEvent
```

**5. No assignee removal**
```java
// Cannot do this currently:
updateTask(id, UpdateTaskRequest.builder()
    .assigneeId(null)  // ❌ Assignee is required
    .build());
```

---

## Best Practices Implemented

### ✅ 1. Constructor Injection
```java
@RequiredArgsConstructor  // Lombok generates constructor
private final TaskRepository taskRepository;
private final UserRepository userRepository;
```

### ✅ 2. Comprehensive Logging
```java
log.info("Creating task: title={}", request.getTitle());
log.error("Assignee not found: id={}", id);
log.debug("Task found: title={}", task.getTitle());
```

### ✅ 3. Transaction Boundaries
```java
@Transactional              // Write operations
@Transactional(readOnly = true)  // Read operations
```

### ✅ 4. DTO Conversion
```java
// Never return entities directly
return TaskResponse.from(task);  // Convert to DTO
```

### ✅ 5. Validation Before Action
```java
// Always validate before processing
User assignee = userRepository.findById(id)
    .orElseThrow(() -> new UserNotFoundException(id));
```

---

##  UserService Implementation

### Complete Service Code Structure

```java
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    // ========== DEPENDENCIES ==========
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    
    // ========== PUBLIC METHODS ==========
    
    /**
     * Get user by ID (active users only)
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id);
    
    /**
     * Get all active users
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers();
    
    /**
     * Soft delete user with bulk task unassignment
     */
    public void deleteUser(Long id);
    
    /**
     * Restore deleted user
     */
    public UserResponse restoreUser(Long id);
}
```

---

### Method 1: getUserById() - Read User

**Purpose:** Retrieve user by ID (active users only)

**Transaction:** Read-only

**Business Flow:**
1. Find user by ID
2. Throw UserNotFoundException if not found
3. @Where clause auto-filters deleted users
4. Convert to UserResponse DTO
5. Return response

**Code:**
```java
@Transactional(readOnly = true)
public UserResponse getUserById(Long id) {
    log.info("Getting user by ID: {}", id);
    
    User user = userRepository.findById(id)
        .orElseThrow(() -> {
            log.error("User not found: id={}", id);
            return new UserNotFoundException(id);
        });
    
    log.info("User found: id={}, username={}", user.getId(), user.getUsername());
    return UserResponse.from(user);
}
```

**Key Points:**
- @Where clause filters deleted users automatically
- findById() only returns active users (deleted = false)
- To find deleted users, use custom query: findByIdIncludingDeleted()

---

### Method 2: getAllUsers() - List Users

**Purpose:** Get all active users

**Transaction:** Read-only

**Business Flow:**
1. Find all users (auto-filtered by @Where)
2. Stream to UserResponse DTOs
3. Return list

**Code:**
```java
@Transactional(readOnly = true)
public List<UserResponse> getAllUsers() {
    log.info("Getting all users");
    
    List<UserResponse> users = userRepository.findAll().stream()
        .map(UserResponse::from)
        .toList();
    
    log.info("Found {} users", users.size());
    return users;
}
```

**Key Points:**
- findAll() excludes deleted users (@Where clause)
- Returns empty list if no users
- Stream API for clean DTO conversion

---

### Method 3: deleteUser() - Soft Delete ⭐ CRITICAL

**Purpose:** Soft delete user with complex business logic

**Transaction:** Read-write (default)

**Business Flow:**
1. Find user (including deleted) - bypass @Where filter
2. Validate user exists
3. Check if already deleted (throw IllegalStateException)
4. Count resources: tasks, comments, projects
5. **Bulk unassign tasks** (1 SQL UPDATE, not N queries)
   - Set assignee = NULL
   - Set status = UNASSIGNED
6. **Preserve comments** (audit trail, keep author_id)
7. **Preserve projects** (business continuity, keep owner_id)
8. Set deleted = true, deletedAt = NOW()
9. Save user (triggers @SQLDelete)
10. Log completion with statistics

**Code:**
```java
public void deleteUser(Long id) {
    log.info("Attempting to delete user: id={}", id);
    
    // STEP 1: Find user (including deleted)
    User user = userRepository.findByIdIncludingDeleted(id)
        .orElseThrow(() -> {
            log.error("User not found for deletion: id={}", id);
            return new UserNotFoundException(id);
        });
    
    // STEP 2: Check if already deleted
    if (user.getDeleted()) {
        log.warn("User is already deleted: id={}", id);
        throw new IllegalStateException("User is already deleted");
    }
    
    // STEP 3: Count resources
    long taskCount = taskRepository.countByAssigneeId(id);
    long commentCount = commentRepository.countByAuthorId(id);
    long projectCount = userRepository.countOwnedProjects(id);
    
    log.info("User {} has {} tasks, {} comments, {} projects", 
        id, taskCount, commentCount, projectCount);
    
    // STEP 4: Bulk unassign tasks (PERFORMANCE OPTIMIZATION)
    if (taskCount > 0) {
        int unassignedCount = taskRepository.unassignTasksByUserId(id);
        log.info("Unassigned {} tasks from user {}", unassignedCount, id);
    }
    
    // STEP 5: Comments and Projects are PRESERVED (audit trail)
    // No action needed - foreign keys remain intact
    
    // STEP 6: Soft delete user
    user.setDeleted(true);
    user.setDeletedAt(LocalDateTime.now());
    // user.setDeletedBy() - TODO: Set current admin user ID
    userRepository.save(user);
    
    log.info("User deleted successfully: id={}, username={}", 
        user.getId(), user.getUsername());
}
```

**Performance Optimization - Bulk Update:**

```sql
-- ONE query instead of N queries:
UPDATE tasks 
SET assignee_id = NULL, status = 'UNASSIGNED' 
WHERE assignee_id = ?

-- Avoids N+1 problem:
-- 1. Load N tasks into memory
-- 2. Loop: N x UPDATE queries
-- 3. N x validation triggers
```

**Why Bulk Update?**
- ✅ Single SQL UPDATE (1 query vs N queries)
- ✅ No entity loading into memory
- ✅ No validation triggers (@FutureOrPresent)
- ✅ No Hibernate flush/dirty checking
- ✅ Performance: O(1) vs O(N)

**Business Rules:**
- Soft delete only (data preserved)
- Tasks → unassigned (assignee = NULL, status = UNASSIGNED)
- Comments → kept (audit trail, author_id NOT NULL)
- Projects → kept (business continuity, owner_id NOT NULL)
- Cannot delete already-deleted user (409 Conflict)

---

### Method 4: restoreUser() - Restore Deleted User

**Purpose:** Restore soft-deleted user

**Transaction:** Read-write

**Business Flow:**
1. Find user (including deleted)
2. Validate user exists
3. Check if actually deleted (throw if not)
4. Reset deleted flags
5. Save user
6. Return UserResponse

**Code:**
```java
public UserResponse restoreUser(Long id) {
    log.info("Attempting to restore user: id={}", id);
    
    // STEP 1: Find user (including deleted)
    User user = userRepository.findByIdIncludingDeleted(id)
        .orElseThrow(() -> {
            log.error("User not found for restore: id={}", id);
            return new UserNotFoundException(id);
        });
    
    // STEP 2: Check if actually deleted
    if (!user.getDeleted()) {
        log.warn("User is not deleted: id={}", id);
        throw new IllegalStateException("User is not deleted");
    }
    
    // STEP 3: Restore user
    user.setDeleted(false);
    user.setDeletedAt(null);
    user.setDeletedBy(null);
    userRepository.save(user);
    
    log.info("User restored successfully: id={}, username={}", 
        user.getId(), user.getUsername());
    
    return UserResponse.from(user);
}
```

**Business Rules:**
- Only restore deleted users
- Cannot restore active users (409 Conflict)
- Tasks remain UNASSIGNED (manual reassignment needed)
- Comments and Projects already intact

---

### Exception Handling

**Thrown Exceptions:**

1. **UserNotFoundException (404)**
   ```java
   throw new UserNotFoundException(id);
   // "User not found with id: {id}"
   ```

2. **IllegalStateException (409)**
   ```java
   throw new IllegalStateException("User is already deleted");
   throw new IllegalStateException("User is not deleted");
   ```

**Caught by GlobalExceptionHandler:**
- UserNotFoundException → 404 Not Found
- IllegalStateException → 409 Conflict (should create custom exception)

---

### Transaction Management

**Read-only transactions:**
```java
@Transactional(readOnly = true)
public UserResponse getUserById(Long id) { ... }
```
**Benefits:**
- Optimization: Hibernate skips dirty checking
- No flush needed
- Database can optimize (read replicas)

**Read-write transactions:**
```java
@Transactional  // Default: propagation=REQUIRED, isolation=DEFAULT
public void deleteUser(Long id) { ... }
```
**Guarantees:**
- Atomic: All or nothing
- Rollback on unchecked exceptions
- Flush changes at transaction end

---

### Dependencies

**UserService dependencies:**

1. **UserRepository**
   - findById() - Find active user
   - findByIdIncludingDeleted() - Bypass @Where filter
   - findAll() - List active users
   - countOwnedProjects() - Count user's projects
   - save() - Persist changes

2. **TaskRepository**
   - countByAssigneeId() - Count user's tasks
   - unassignTasksByUserId() - Bulk unassign (CRITICAL)

3. **CommentRepository**
   - countByAuthorId() - Count user's comments

---

### Logging Strategy

**Log levels:**
- **INFO:** Normal operations, business flow
- **WARN:** Already deleted, validation issues
- **ERROR:** Not found errors

**Log messages:**
```java
log.info("Getting user by ID: {}", id);
log.info("User found: id={}, username={}", user.getId(), user.getUsername());
log.warn("User is already deleted: id={}", id);
log.error("User not found: id={}", id);
```

---

## Related Documentation

- [TaskController](../api/README.md) - REST API endpoints
- [Task Entity](../entity/README.md) - Domain model
- [TaskRepository](../repository/README.md) - Data access
- [DTOs](../dto/README.md) - Request/Response objects
- [Exception Handling](../exception/README.md) - Error handling

---

**Last Updated:** December 14, 2025  
**Version:** 0.5.0 - MVP Phase  
**Status:** Core CRUD operations complete, filtering/pagination pending
