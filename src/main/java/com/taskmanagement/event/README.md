# Event Layer (Domain-Driven Design Events)

##  Overview

The **Event layer** implements domain-driven design (DDD) event patterns for asynchronous communication and event-driven architecture. Domain events represent significant occurrences in the business domain that other parts of the system may need to react to.

**Location:** \src/main/java/com/taskmanagement/event/\

**Responsibility:** Define domain events, publish events from aggregates, and handle event listeners for decoupled communication

---

##  Core Responsibilities

### 1. Domain Event Definition
- Define immutable event objects representing business occurrences
- Capture the state at the time the event occurred
- Include all information needed by event handlers
- Follow domain language and terminology

### 2. Event Publishing
- Publish events from entities/aggregates when important changes occur
- Use Spring's ApplicationEventPublisher for async publication
- Maintain order and consistency of events
- Support transactional event publishing

### 3. Event Handling
- Implement event listeners that react to published events
- Decouple business logic from side effects
- Support async processing of time-consuming tasks
- Enable flexible business workflow implementation

### 4. Event Logging and Auditing
- Track all domain events for audit trail
- Support event sourcing patterns (future phase)
- Enable replay capabilities
- Provide event history for compliance

### 5. Integration Points
- Trigger notifications (email, SMS, notifications)
- Update read models and caches
- Sync with external systems
- Trigger workflows and background jobs

---

##  Folder Structure

\\\
event/
 domain/
    DomainEvent.java                    # Base event interface
    TaskCreatedEvent.java               # Task created event
    TaskUpdatedEvent.java               # Task updated event
    TaskStatusChangedEvent.java         # Task status changed
    TaskAssignedEvent.java              # Task assigned event
    TaskCompletedEvent.java             # Task completed event
    UserRegisteredEvent.java            # User registered event
    ProjectCreatedEvent.java            # Project created event
    CommentAddedEvent.java              # Comment added event
    README.md                           # Domain events documentation

 listener/
    TaskEventListener.java              # Task event handlers
    NotificationEventListener.java      # Notification handlers
    AuditEventListener.java             # Audit trail handlers
    ProjectEventListener.java           # Project event handlers
    UserEventListener.java              # User event handlers
    README.md                           # Event listeners documentation

 publisher/
    DomainEventPublisher.java           # Event publishing utilities
    TransactionalEventPublisher.java    # Transactional publishing
    README.md                           # Publisher documentation

 exception/
    EventPublishingException.java       # Event publishing errors
    EventHandlingException.java         # Event handling errors

 README.md                              # This file
\\\

---

##  Key Concepts

### Domain Event Base Interface

\\\java
/**
 * Base interface for all domain events
 * Every domain event should implement this interface
 */
public interface DomainEvent {
    
    /**
     * Unique identifier for this event instance
     */
    String getEventId();
    
    /**
     * Type of event (e.g., "TaskCreated", "TaskCompleted")
     */
    String getEventType();
    
    /**
     * Aggregate ID that triggered this event
     */
    Long getAggregateId();
    
    /**
     * Type of aggregate that triggered this event
     */
    String getAggregateType();
    
    /**
     * When this event occurred
     */
    LocalDateTime getOccurredAt();
    
    /**
     * User who triggered this event
     */
    String getTriggeredBy();
    
    /**
     * Version of aggregate when event occurred
     */
    Long getAggregateVersion();
}
\\\

### Abstract Base Event Class

\\\java
/**
 * Abstract base class for domain events
 * Provides common functionality for all events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class AbstractDomainEvent implements DomainEvent {
    
    @NonNull
    private String eventId = UUID.randomUUID().toString();
    
    @NonNull
    private String eventType;
    
    @NonNull
    private Long aggregateId;
    
    @NonNull
    private String aggregateType;
    
    @NonNull
    private LocalDateTime occurredAt = LocalDateTime.now();
    
    @NonNull
    private String triggeredBy;
    
    @NonNull
    private Long aggregateVersion;
    
    /**
     * Optional correlation ID for tracking related events
     */
    private String correlationId;
    
    /**
     * Optional metadata for extension
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
\\\

---

##  Domain Event Examples

### Task Created Event

\\\java
/**
 * Event published when a new task is created
 * 
 * Triggered by: TaskService.createTask()
 * Listeners: NotificationListener, AuditListener, ProjectStatisticsListener
 * 
 * Important fields:
 * - taskId: ID of the newly created task
 * - projectId: Project the task belongs to
 * - assigneeId: User assigned to the task
 * - priority: Task priority level
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TaskCreatedEvent extends AbstractDomainEvent {
    
    private String title;
    private String description;
    private Long projectId;
    private Long assigneeId;
    private String priority;
    private LocalDateTime dueDate;
    
    /**
     * Create event from task entity
     */
    public static TaskCreatedEvent from(Task task, String triggeredBy) {
        return TaskCreatedEvent.builder()
            .eventType("TaskCreated")
            .aggregateId(task.getId())
            .aggregateType("Task")
            .triggeredBy(triggeredBy)
            .aggregateVersion(task.getVersion())
            .title(task.getTitle())
            .description(task.getDescription())
            .projectId(task.getProject().getId())
            .assigneeId(task.getAssignee().getId())
            .priority(task.getPriority().name())
            .dueDate(task.getDueDate())
            .build();
    }
}
\\\

### Task Status Changed Event

\\\java
/**
 * Event published when task status changes
 * 
 * Triggered by: TaskService.updateTaskStatus()
 * Listeners: NotificationListener (notify assignee), AuditListener, WorkflowListener
 * 
 * Important fields:
 * - previousStatus: Status before change
 * - newStatus: New status after change
 * - reason: Reason for status change
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TaskStatusChangedEvent extends AbstractDomainEvent {
    
    private String previousStatus;
    private String newStatus;
    private String reason;
    private Long assigneeId;
    
    /**
     * Create event from task status change
     */
    public static TaskStatusChangedEvent from(
        Task task,
        TaskStatus previousStatus,
        String reason,
        String triggeredBy
    ) {
        return TaskStatusChangedEvent.builder()
            .eventType("TaskStatusChanged")
            .aggregateId(task.getId())
            .aggregateType("Task")
            .triggeredBy(triggeredBy)
            .aggregateVersion(task.getVersion())
            .previousStatus(previousStatus.name())
            .newStatus(task.getStatus().name())
            .reason(reason)
            .assigneeId(task.getAssignee().getId())
            .build();
    }
}
\\\

### Task Completed Event

\\\java
/**
 * Event published when a task is completed
 * 
 * Triggered by: TaskService.completeTask()
 * Listeners: NotificationListener, MetricsListener, ProjectProgressListener
 * 
 * Important fields:
 * - completionTime: When task was completed
 * - daysOverdue: Number of days task was overdue (if applicable)
 * - completedBy: User who completed the task
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TaskCompletedEvent extends AbstractDomainEvent {
    
    private LocalDateTime completionTime;
    private Integer daysOverdue;
    private Long completedBy;
    private String completionNotes;
    
    /**
     * Create event from task completion
     */
    public static TaskCompletedEvent from(Task task, String triggeredBy) {
        LocalDateTime now = LocalDateTime.now();
        int daysOverdue = 0;
        
        if (now.isAfter(task.getDueDate())) {
            daysOverdue = (int) ChronoUnit.DAYS.between(task.getDueDate(), now);
        }
        
        return TaskCompletedEvent.builder()
            .eventType("TaskCompleted")
            .aggregateId(task.getId())
            .aggregateType("Task")
            .triggeredBy(triggeredBy)
            .aggregateVersion(task.getVersion())
            .completionTime(now)
            .daysOverdue(daysOverdue)
            .build();
    }
}
\\\

### Task Assigned Event

\\\java
/**
 * Event published when a task is assigned to a user
 * 
 * Triggered by: TaskService.assignTask()
 * Listeners: NotificationListener, AuditListener, UserWorkloadListener
 * 
 * Important fields:
 * - previousAssigneeId: User task was assigned to before
 * - newAssigneeId: User task is now assigned to
 * - reason: Reason for reassignment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TaskAssignedEvent extends AbstractDomainEvent {
    
    private Long previousAssigneeId;
    private Long newAssigneeId;
    private String reason;
    private LocalDateTime assignedAt;
    
    /**
     * Create event from task assignment
     */
    public static TaskAssignedEvent from(
        Task task,
        Long previousAssigneeId,
        String reason,
        String triggeredBy
    ) {
        return TaskAssignedEvent.builder()
            .eventType("TaskAssigned")
            .aggregateId(task.getId())
            .aggregateType("Task")
            .triggeredBy(triggeredBy)
            .aggregateVersion(task.getVersion())
            .previousAssigneeId(previousAssigneeId)
            .newAssigneeId(task.getAssignee().getId())
            .reason(reason)
            .assignedAt(LocalDateTime.now())
            .build();
    }
}
\\\

### User Registered Event

\\\java
/**
 * Event published when a new user registers
 * 
 * Triggered by: UserService.registerUser()
 * Listeners: NotificationListener (welcome email), AuditListener, MetricsListener
 * 
 * Important fields:
 * - userId: ID of newly registered user
 * - email: User email address
 * - username: User username
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class UserRegisteredEvent extends AbstractDomainEvent {
    
    private String email;
    private String username;
    private String fullName;
    private LocalDateTime registeredAt;
    
    /**
     * Create event from user registration
     */
    public static UserRegisteredEvent from(User user, String triggeredBy) {
        return UserRegisteredEvent.builder()
            .eventType("UserRegistered")
            .aggregateId(user.getId())
            .aggregateType("User")
            .triggeredBy(triggeredBy)
            .aggregateVersion(user.getVersion())
            .email(user.getEmail())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .registeredAt(LocalDateTime.now())
            .build();
    }
}
\\\

### Comment Added Event

\\\java
/**
 * Event published when a comment is added to a task
 * 
 * Triggered by: TaskService.addComment()
 * Listeners: NotificationListener (notify task owner), AuditListener
 * 
 * Important fields:
 * - taskId: Task the comment is on
 * - commentId: ID of the new comment
 * - authorId: User who added the comment
 * - text: Comment content
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class CommentAddedEvent extends AbstractDomainEvent {
    
    private Long taskId;
    private Long commentId;
    private Long authorId;
    private String text;
    private LocalDateTime addedAt;
    
    /**
     * Create event from comment addition
     */
    public static CommentAddedEvent from(Comment comment, String triggeredBy) {
        return CommentAddedEvent.builder()
            .eventType("CommentAdded")
            .aggregateId(comment.getId())
            .aggregateType("Comment")
            .triggeredBy(triggeredBy)
            .aggregateVersion(1L)  // Comments are not versioned
            .taskId(comment.getTask().getId())
            .commentId(comment.getId())
            .authorId(comment.getAuthor().getId())
            .text(comment.getText())
            .addedAt(LocalDateTime.now())
            .build();
    }
}
\\\

---

##  Event Publishing

### Publishing Events from Entities

\\\java
/**
 * Entities can publish events using ApplicationEventPublisher
 * Injected into service, called after entity state change
 */
@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public Task createTask(CreateTaskRequest request, String currentUser) {
        // Create entity
        Task task = Task.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .project(/* ... */)
            .assignee(/* ... */)
            .status(TaskStatus.PENDING)
            .priority(TaskPriority.MEDIUM)
            .dueDate(request.getDueDate())
            .build();
        
        // Save to database
        Task savedTask = taskRepository.save(task);
        
        // Publish event (after transaction commits)
        TaskCreatedEvent event = TaskCreatedEvent.from(savedTask, currentUser);
        eventPublisher.publishEvent(event);
        
        return savedTask;
    }
    
    @Transactional
    public Task updateTaskStatus(Long taskId, TaskStatus newStatus, String reason, String currentUser) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
        
        TaskStatus previousStatus = task.getStatus();
        task.setStatus(newStatus);
        Task updated = taskRepository.save(task);
        
        // Publish event
        TaskStatusChangedEvent event = TaskStatusChangedEvent.from(
            updated,
            previousStatus,
            reason,
            currentUser
        );
        eventPublisher.publishEvent(event);
        
        return updated;
    }
    
    @Transactional
    public Task assignTask(Long taskId, Long assigneeId, String reason, String currentUser) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
        
        Long previousAssigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;
        User newAssignee = userRepository.findById(assigneeId)
            .orElseThrow(() -> new UserNotFoundException(assigneeId));
        
        task.setAssignee(newAssignee);
        Task updated = taskRepository.save(task);
        
        // Publish event
        TaskAssignedEvent event = TaskAssignedEvent.from(
            updated,
            previousAssigneeId,
            reason,
            currentUser
        );
        eventPublisher.publishEvent(event);
        
        return updated;
    }
}
\\\

### Transactional Event Publishing

Ensure events are published only if transaction commits:

\\\java
/**
 * Use @TransactionalEventListener to publish events
 * only when transaction successfully commits
 */
@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    private static final ThreadLocal<List<DomainEvent>> events = 
        ThreadLocal.withInitial(ArrayList::new);
    
    /**
     * Register event to be published when transaction commits
     */
    public void registerEvent(DomainEvent event) {
        events.get().add(event);
    }
    
    /**
     * Get all registered events for current transaction
     */
    public List<DomainEvent> getRegisteredEvents() {
        return events.get();
    }
    
    /**
     * Clear events after processing
     */
    public void clearEvents() {
        events.remove();
    }
    
    @Transactional
    public Task createTask(CreateTaskRequest request, String currentUser) {
        Task task = Task.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .build();
        
        Task saved = taskRepository.save(task);
        
        // Register event (not published yet)
        TaskCreatedEvent event = TaskCreatedEvent.from(saved, currentUser);
        registerEvent(event);
        
        return saved;
    }
    
    /**
     * Publish all registered events after transaction commits
     */
    @TransactionalEventListener
    public void publishRegisteredEvents(TransactionPhase phase) {
        if (phase == TransactionPhase.AFTER_COMMIT) {
            List<DomainEvent> registeredEvents = getRegisteredEvents();
            registeredEvents.forEach(eventPublisher::publishEvent);
            clearEvents();
        }
    }
}
\\\

---

##  Event Listeners

### Notification Event Listener

\\\java
/**
 * Event listener that sends notifications based on domain events
 * Handles email, SMS, and in-app notifications
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    
    /**
     * Send welcome email when user registers
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            log.info("Processing UserRegisteredEvent for user: {}", event.getEmail());
            
            User user = userRepository.findById(event.getAggregateId())
                .orElseThrow(() -> new UserNotFoundException(event.getAggregateId()));
            
            notificationService.sendWelcomeEmail(
                user.getEmail(),
                user.getFullName()
            );
            
            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error processing UserRegisteredEvent", e);
        }
    }
    
    /**
     * Notify assignee when task is assigned
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskAssigned(TaskAssignedEvent event) {
        try {
            log.info("Processing TaskAssignedEvent for task: {}", event.getAggregateId());
            
            User assignee = userRepository.findById(event.getNewAssigneeId())
                .orElseThrow();
            
            notificationService.sendAssignmentNotification(
                assignee.getEmail(),
                event.getAggregateId(),
                event.getReason()
            );
            
            log.info("Assignment notification sent to: {}", assignee.getEmail());
        } catch (Exception e) {
            log.error("Error processing TaskAssignedEvent", e);
        }
    }
    
    /**
     * Notify task owner when comment is added
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentAdded(CommentAddedEvent event) {
        try {
            log.info("Processing CommentAddedEvent for task: {}", event.getTaskId());
            
            // Send notification to task owner/assignee
            notificationService.sendCommentNotification(
                event.getTaskId(),
                event.getAuthorId(),
                event.getText()
            );
            
        } catch (Exception e) {
            log.error("Error processing CommentAddedEvent", e);
        }
    }
    
    /**
     * Notify manager when task is completed overdue
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCompleted(TaskCompletedEvent event) {
        try {
            if (event.getDaysOverdue() > 0) {
                log.info("Task completed {} days overdue", event.getDaysOverdue());
                
                notificationService.sendOverdueTaskNotification(
                    event.getAggregateId(),
                    event.getDaysOverdue()
                );
            }
        } catch (Exception e) {
            log.error("Error processing TaskCompletedEvent", e);
        }
    }
}
\\\

### Audit Event Listener

\\\java
/**
 * Event listener that logs all domain events for audit trail
 * Maintains complete history of all business changes
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {
    
    private final AuditLogRepository auditLogRepository;
    
    /**
     * Log all domain events to audit trail
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEvent event) {
        try {
            log.debug("Logging audit event: {}", event.getEventType());
            
            AuditLog auditLog = AuditLog.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .aggregateId(event.getAggregateId())
                .aggregateType(event.getAggregateType())
                .triggeredBy(event.getTriggeredBy())
                .occurredAt(event.getOccurredAt())
                .aggregateVersion(event.getAggregateVersion())
                .eventData(serializeEvent(event))
                .metadata(event.getMetadata())
                .build();
            
            auditLogRepository.save(auditLog);
            
            log.debug("Audit log created for event: {}", event.getEventType());
        } catch (Exception e) {
            log.error("Error logging audit event", e);
            // Don't throw - audit logging shouldn't break application
        }
    }
    
    private String serializeEvent(DomainEvent event) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Error serializing event", e);
            return "{}";
        }
    }
}
\\\

### Project Statistics Event Listener

\\\java
/**
 * Event listener that updates project statistics based on task events
 * Maintains read models for dashboard and reporting
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectEventListener {
    
    private final ProjectStatisticsRepository statsRepository;
    private final ProjectRepository projectRepository;
    
    /**
     * Update statistics when task is created
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCreated(TaskCreatedEvent event) {
        try {
            log.info("Updating project statistics for task created in project: {}",
                event.getProjectId());
            
            ProjectStatistics stats = statsRepository.findByProjectId(event.getProjectId())
                .orElseGet(() -> ProjectStatistics.builder()
                    .projectId(event.getProjectId())
                    .totalTasks(0L)
                    .completedTasks(0L)
                    .overdueTasks(0L)
                    .build());
            
            stats.setTotalTasks(stats.getTotalTasks() + 1);
            statsRepository.save(stats);
            
        } catch (Exception e) {
            log.error("Error updating project statistics", e);
        }
    }
    
    /**
     * Update statistics when task is completed
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCompleted(TaskCompletedEvent event) {
        try {
            log.info("Updating project statistics for task completion");
            
            // Update project completion count
            // Decrement overdue count if applicable
            // Update metrics
            
        } catch (Exception e) {
            log.error("Error updating completion statistics", e);
        }
    }
}
\\\

---

##  Async Event Processing

### Processing Events Asynchronously

\\\java
/**
 * Use @Async for event listeners that should not block
 * Long-running operations like sending emails, API calls
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AsyncEventListener {
    
    private final EmailService emailService;
    private final ExternalApiService externalApi;
    
    /**
     * Send email asynchronously
     * Won't block the request
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCreatedSendEmail(TaskCreatedEvent event) {
        try {
            log.info("Async: Sending task creation email");
            emailService.sendTaskCreationEmail(event.getAggregateId());
        } catch (Exception e) {
            log.error("Error sending task creation email", e);
            // Handle error appropriately
        }
    }
    
    /**
     * Call external API asynchronously
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCompletedNotifyExternal(TaskCompletedEvent event) {
        try {
            log.info("Async: Notifying external system of task completion");
            externalApi.notifyTaskCompletion(event.getAggregateId());
        } catch (Exception e) {
            log.error("Error notifying external system", e);
        }
    }
}
\\\

### Async Configuration

\\\java
/**
 * Configure async processing for event listeners
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("TaskMgmt-Async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
    
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }
    
    /**
     * Handle exceptions in async tasks
     */
    @Component
    public static class AsyncExceptionHandler 
        implements AsyncUncaughtExceptionHandler {
        
        private static final Logger log = LoggerFactory.getLogger(AsyncExceptionHandler.class);
        
        @Override
        public void handleUncaughtException(Throwable throwable, Method method, Object... params) {
            log.error("Async task error in method: {}", method.getName(), throwable);
        }
    }
}
\\\

---

##  Event Sourcing Pattern

### Event Store Entity

\\\java
/**
 * Entity representing an event in the event store
 * Enables event sourcing for complete audit trail
 */
@Entity
@Table(
    name = "event_store",
    indexes = {
        @Index(name = "idx_aggregate_id", columnList = "aggregate_id"),
        @Index(name = "idx_aggregate_type", columnList = "aggregate_type"),
        @Index(name = "idx_occurred_at", columnList = "occurred_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 36)
    private String eventId;
    
    @Column(nullable = false, length = 100)
    private String eventType;
    
    @Column(nullable = false)
    private Long aggregateId;
    
    @Column(nullable = false, length = 50)
    private String aggregateType;
    
    @Column(nullable = false)
    private Long aggregateVersion;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String eventData;
    
    @Column(nullable = false, length = 100)
    private String triggeredBy;
    
    @Column(nullable = false)
    private LocalDateTime occurredAt;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime storedAt = LocalDateTime.now();
    
    /**
     * Create from domain event
     */
    public static StoredEvent from(DomainEvent event, String eventData) {
        return StoredEvent.builder()
            .eventId(event.getEventId())
            .eventType(event.getEventType())
            .aggregateId(event.getAggregateId())
            .aggregateType(event.getAggregateType())
            .aggregateVersion(event.getAggregateVersion())
            .eventData(eventData)
            .triggeredBy(event.getTriggeredBy())
            .occurredAt(event.getOccurredAt())
            .build();
    }
}
\\\

### Event Store Repository

\\\java
/**
 * Repository for event sourcing - store and retrieve domain events
 */
public interface EventStoreRepository extends JpaRepository<StoredEvent, Long> {
    
    /**
     * Get all events for an aggregate in order
     */
    List<StoredEvent> findByAggregateIdOrderByAggregateVersionAsc(Long aggregateId);
    
    /**
     * Get events of specific type for an aggregate
     */
    List<StoredEvent> findByAggregateIdAndEventType(Long aggregateId, String eventType);
    
    /**
     * Get all events since a specific time
     */
    List<StoredEvent> findByOccurredAtAfterOrderByOccurredAtAsc(LocalDateTime since);
    
    /**
     * Get events for projection/read model updates
     */
    List<StoredEvent> findByStoredAtAfterOrderByStoredAtAsc(LocalDateTime since);
}
\\\

### Event Sourcing Listener

\\\java
/**
 * Listener that stores all domain events for event sourcing
 * Maintains complete event log for replay capability
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventSourcingListener {
    
    private final EventStoreRepository eventStore;
    private final ObjectMapper objectMapper;
    
    /**
     * Store every domain event in event store
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEvent event) {
        try {
            log.debug("Storing event in event store: {}", event.getEventType());
            
            String eventData = objectMapper.writeValueAsString(event);
            StoredEvent storedEvent = StoredEvent.from(event, eventData);
            
            eventStore.save(storedEvent);
            
            log.debug("Event stored with ID: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Error storing event in event store", e);
            // Don't throw - event sourcing shouldn't break application
        }
    }
}
\\\

---

##  Best Practices

### 1. Events Should Be Immutable

\\\java
// GOOD: Immutable event
@Data
@Builder
public class TaskCreatedEvent {
    private final String eventId;
    private final Long aggregateId;
    private final String title;
    private final LocalDateTime occurredAt;
    
    // No setters, only builder
}

// BAD: Mutable event
@Data
public class TaskCreatedEvent {
    private String eventId;
    private Long aggregateId;
    private String title;
    
    // Setters allow mutation
}
\\\

### 2. Include All Necessary Context

\\\java
// GOOD: Event includes everything needed for handlers
@Data
public class TaskAssignedEvent extends AbstractDomainEvent {
    private Long newAssigneeId;
    private Long previousAssigneeId;
    private String reason;
    private LocalDateTime assignedAt;
}

// BAD: Missing context, handlers need to query database
@Data
public class TaskAssignedEvent extends AbstractDomainEvent {
    private Long newAssigneeId;
    // Handlers need to query for previous assignee
}
\\\

### 3. Handle Errors in Listeners

\\\java
// GOOD: Proper error handling
@TransactionalEventListener
public void onTaskCreated(TaskCreatedEvent event) {
    try {
        // Process event
        notificationService.sendEmail(event.getAssigneeId());
    } catch (Exception e) {
        log.error("Error sending email for task creation", e);
        // Don't throw - don't interrupt main flow
        // Implement retry logic if critical
    }
}

// BAD: No error handling
@TransactionalEventListener
public void onTaskCreated(TaskCreatedEvent event) {
    notificationService.sendEmail(event.getAssigneeId());  // Can throw!
}
\\\

### 4. Use Async for Long-Running Operations

\\\java
// GOOD: Async processing for slow operations
@Async
@TransactionalEventListener
public void onTaskCreated(TaskCreatedEvent event) {
    // This won't block the API response
    emailService.sendComplexEmailWithAttachments(event);
}

// BAD: Blocking listener
@TransactionalEventListener
public void onTaskCreated(TaskCreatedEvent event) {
    // This blocks API response while sending email
    emailService.sendComplexEmailWithAttachments(event);
}
\\\

### 5. Order Listeners by Priority

\\\java
// GOOD: Explicit ordering
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Order(1)  // First priority
public void onTaskCreated(TaskCreatedEvent event) {
    // Core business logic
}

@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Order(2)  // Second priority
public void sendNotification(TaskCreatedEvent event) {
    // Side effect: send email
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Order(3)  // Last: logging
public void auditEvent(TaskCreatedEvent event) {
    // Audit logging
}
\\\

### 6. Document Event Impact

\\\java
/**
 * Event published when a new task is created
 * 
 * Event Type: TaskCreated
 * Triggered By: TaskService.createTask()
 * 
 * Listeners:
 * 1. NotificationEventListener - Sends assignment notification
 * 2. AuditEventListener - Logs to audit trail
 * 3. ProjectStatisticsListener - Updates project metrics
 * 4. ActivityStreamListener - Updates activity feed
 * 
 * Async Processing:
 * - Email notification (async, non-critical)
 * - External API sync (async, best-effort)
 * 
 * Event Fields:
 * - title: Task title
 * - description: Task description
 * - projectId: Associated project
 * - assigneeId: Assigned user
 * - priority: Task priority level
 * - dueDate: Task due date
 * 
 * Side Effects:
 * - Assignee receives notification
 * - Project task count increases
 * - Activity feed updated
 * - Email sent (async)
 * 
 * Compensation/Rollback:
 * - If task creation fails, event is NOT published
 * - Listeners never see events from failed transactions
 */
@Data
public class TaskCreatedEvent extends AbstractDomainEvent {
    // Fields...
}
\\\

### 7. Use Correlation IDs for Tracing

\\\java
/**
 * Correlation ID allows tracing related events across system
 * Useful for debugging and understanding workflows
 */
@Service
public class TaskService {
    
    public Task createTask(CreateTaskRequest request, String currentUser) {
        String correlationId = UUID.randomUUID().toString();
        
        Task task = taskRepository.save(/* ... */);
        
        TaskCreatedEvent event = TaskCreatedEvent.from(task, currentUser);
        event.setCorrelationId(correlationId);
        
        eventPublisher.publishEvent(event);
        
        return task;
    }
}

/**
 * Listener can use correlation ID to link related events
 */
@Component
public class WorkflowListener {
    
    @TransactionalEventListener
    public void onTaskCreated(TaskCreatedEvent event) {
        log.info("Task created with correlation ID: {}", event.getCorrelationId());
        // All subsequent events in this workflow can use same correlation ID
    }
}
\\\

---

##  Common Patterns

### Saga Pattern for Distributed Transactions

\\\java
/**
 * Saga: Manage distributed transaction across multiple aggregates
 * Example: Order creation involves both Order and Payment aggregates
 */
@Service
@RequiredArgsConstructor
public class TaskWorkflowSaga {
    
    private final TaskService taskService;
    private final NotificationService notificationService;
    private final ProjectService projectService;
    
    /**
     * Orchestrate complex workflow using events
     */
    @TransactionalEventListener
    public void onTaskCreated(TaskCreatedEvent event) {
        try {
            // Step 1: Task created
            log.info("Step 1: Task created");
            
            // Step 2: Update project
            projectService.incrementTaskCount(event.getProjectId());
            log.info("Step 2: Project updated");
            
            // Step 3: Send notification
            notificationService.sendAssignmentNotification(event.getNewAssigneeId());
            log.info("Step 3: Notification sent");
            
        } catch (Exception e) {
            log.error("Saga failed, initiating compensating actions");
            // Compensate: Undo changes in reverse order
            undoProjectUpdate(event.getProjectId());
        }
    }
    
    private void undoProjectUpdate(Long projectId) {
        // Implement compensation logic
    }
}
\\\

### Event Projection (Read Model)

\\\java
/**
 * Build read models (projections) from domain events
 * Optimized views for querying without joining entities
 */
@Entity
@Table(name = "task_summary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskSummary {
    @Id
    private Long id;
    private String title;
    private String status;
    private Long assigneeId;
    private String assigneeName;
    private Long projectId;
    private String projectName;
    private LocalDateTime dueDate;
    private Integer commentCount;
}

/**
 * Update projection when events occur
 */
@Component
@RequiredArgsConstructor
public class TaskSummaryProjection {
    
    private final TaskSummaryRepository summaryRepository;
    
    @TransactionalEventListener
    public void onTaskCreated(TaskCreatedEvent event) {
        TaskSummary summary = TaskSummary.builder()
            .id(event.getAggregateId())
            .title(event.getTitle())
            .status("PENDING")
            .assigneeId(event.getAssigneeId())
            .projectId(event.getProjectId())
            .dueDate(event.getDueDate())
            .commentCount(0)
            .build();
        
        summaryRepository.save(summary);
    }
    
    @TransactionalEventListener
    public void onTaskStatusChanged(TaskStatusChangedEvent event) {
        TaskSummary summary = summaryRepository.findById(event.getAggregateId())
            .orElseThrow();
        
        summary.setStatus(event.getNewStatus());
        summaryRepository.save(summary);
    }
}
\\\

---

##  Testing Events

### Unit Testing Events

\\\java
@SpringBootTest
class TaskCreatedEventTest {
    
    @Test
    void from_WithValidTask_CreatesEvent() {
        User user = User.builder().id(1L).username("john").build();
        Task task = Task.builder()
            .id(1L)
            .title("Test Task")
            .description("Test Description")
            .assignee(user)
            .priority(TaskPriority.HIGH)
            .build();
        
        TaskCreatedEvent event = TaskCreatedEvent.from(task, "admin");
        
        assertEquals("TaskCreated", event.getEventType());
        assertEquals(1L, event.getAggregateId());
        assertEquals("Test Task", event.getTitle());
        assertNotNull(event.getEventId());
    }
}
\\\

### Testing Event Listeners

\\\java
@SpringBootTest
class NotificationEventListenerTest {
    
    @MockBean
    private NotificationService notificationService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Test
    void onTaskAssigned_SendsNotification() throws InterruptedException {
        User assignee = User.builder().id(1L).email("user@test.com").build();
        TaskAssignedEvent event = TaskAssignedEvent.builder()
            .aggregateId(1L)
            .aggregateType("Task")
            .newAssigneeId(1L)
            .eventType("TaskAssigned")
            .build();
        
        eventPublisher.publishEvent(event);
        
        // Wait for async processing
        Thread.sleep(1000);
        
        verify(notificationService).sendAssignmentNotification(anyString(), anyLong(), anyString());
    }
}
\\\

---

##  Event Layer Checklist

When working with domain events:

- [ ] Create domain event class extending AbstractDomainEvent
- [ ] Include all necessary context in event fields
- [ ] Make events immutable
- [ ] Create static factory method from() in event class
- [ ] Document event purpose and impact
- [ ] Publish event from service after entity change
- [ ] Use @TransactionalEventListener for event handlers
- [ ] Implement error handling in listeners
- [ ] Use @Async for long-running operations
- [ ] Order listeners by priority
- [ ] Use correlation IDs for tracing
- [ ] Don't throw exceptions in listeners
- [ ] Test event publishing and listeners
- [ ] Document all listeners for an event
- [ ] Consider event sourcing for audit trail

---

##  Related Documentation

- **ARCHITECTURE.md** - Event-driven architecture overview
- **README.md** - Main project overview
- **Service Layer** - Where events are published
- **Entity Layer** - Aggregates that publish events

---

##  Quick Reference

### Event Publishing

\\\
1. Entity/aggregate changes state
2. Service publishes DomainEvent
3. Transaction commits
4. ApplicationEventPublisher notifies listeners
5. Listeners process event (sync or async)
\\\

### Event Listener Lifecycle

\\\
TransactionPhase.BEFORE_COMMIT   During transaction
TransactionPhase.AFTER_COMMIT    After transaction commits (default)
TransactionPhase.AFTER_ROLLBACK  After transaction fails
TransactionPhase.AFTER_COMPLETION  After transaction finishes
\\\

### Common Event Types

\\\
Created    New entity created (User, Task, Project)
Updated    Entity field changed (details, properties)
StatusChanged  Status field changed (state machine)
Assigned   Entity assigned to user
Completed  Entity completed/finished
Deleted    Entity soft-deleted
\\\

---

**Last Updated:** December 1, 2025  
**Version:** 1.0.0  
**Status:** Complete
