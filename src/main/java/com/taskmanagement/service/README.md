# Service Layer (Business Logic)

##  Overview

The **Service layer** implements the core business logic of the application. Services orchestrate operations between controllers (API requests) and repositories (data access), managing transactions, validation, domain events, and complex workflows.

**Location:** \src/main/java/com/taskmanagement/service/\

**Responsibility:** Implement business rules, coordinate data operations, manage transactions, publish events, and provide the API for application use cases

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

##  Folder Structure

\\\
service/
 TaskService.java                      # Task business logic
 ProjectService.java                   # Project business logic
 UserService.java                      # User business logic
 CommentService.java                   # Comment business logic
 ProjectStatisticsService.java         # Statistics and reporting
 NotificationService.java              # Notification business logic

 base/
    BaseService.java                   # Abstract base service class
    ServiceTemplate.java               # Service template for common patterns

 util/
    ValidationUtil.java                # Business validation utilities
    ServiceHelper.java                 # Service helper methods

 dto/
    TaskServiceRequest.java            # Service request DTOs
    TaskServiceResponse.java           # Service response DTOs

 README.md                             # This file
\\\

---

##  Base Service Class

\\\java
/**
 * Abstract base service class
 * Provides common functionality for all services
 */
@Slf4j
public abstract class BaseService<T extends BaseEntity, ID> {
    
    protected abstract JpaRepository<T, ID> getRepository();
    protected abstract ApplicationEventPublisher getEventPublisher();
    
    /**
     * Find entity by ID or throw exception
     */
    public T findByIdOrThrow(ID id, String entityName) {
        return getRepository().findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(entityName, id.toString()));
    }
    
    /**
     * Check if entity exists
     */
    public boolean exists(ID id) {
        return getRepository().existsById(id);
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

##  Task Service Implementation

\\\java
/**
 * Task Service
 * Implements task management business logic
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * Create a new task
     * 
     * @param request Task creation request
     * @return Created task
     * @throws ProjectNotFoundException if project doesn't exist
     * @throws UserNotFoundException if assigned user doesn't exist
     * @throws DuplicateTaskException if task title already exists
     */
    public Task createTask(CreateTaskRequest request) {
        log.info("Creating task: {} in project: {}", request.getTitle(), request.getProjectId());
        
        // Validate project exists
        Project project = projectRepository.findById(request.getProjectId())
            .orElseThrow(() -> new ProjectNotFoundException(request.getProjectId()));
        
        // Check for duplicate task name
        if (taskRepository.existsByTitleAndProjectId(request.getTitle(), request.getProjectId())) {
            throw new DuplicateTaskException(request.getTitle(), request.getProjectId());
        }
        
        // Create task entity
        Task task = Task.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .status(TaskStatus.PENDING)
            .priority(request.getPriority())
            .project(project)
            .dueDate(request.getDueDate())
            .build();
        
        // Assign user if provided
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                .orElseThrow(() -> new UserNotFoundException(request.getAssigneeId()));
            task.setAssignee(assignee);
        }
        
        // Save task
        Task savedTask = taskRepository.save(task);
        
        // Publish event
        eventPublisher.publishEvent(
            TaskCreatedEvent.from(savedTask, getCurrentUserId())
        );
        
        log.info("Task created successfully: {}", savedTask.getId());
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

---

**Last Updated:** December 1, 2025  
**Version:** 1.0.0  
**Status:** Complete
