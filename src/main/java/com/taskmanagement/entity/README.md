# Entity Layer (Domain Model)

##  Overview

The **Entity layer** defines the core domain model of the application. Entities represent persistent domain concepts (tasks, users, projects) that are stored in the database using JPA and Hibernate ORM.

**Location:** `src/main/java/com/taskmanagement/entity/`

**Responsibility:** Define domain entities with proper JPA mappings, relationships, validation, and auditing

---

##  Current Implementation Status

### ✅ Fully Implemented Entities
- **Task.java** - Core task entity with full relationships
- **User.java** - User entity (referenced by Task)
- **Project.java** - Project entity (referenced by Task)
- **Comment.java** - Comment entity (One-to-Many with Task)
- **Attachment.java** - Attachment entity (One-to-Many with Task)
- **TaskStatus.java** - Task status enumeration
- **TaskPriority.java** - Task priority enumeration

### 🔧 Implementation Notes
- Task entity **NOW ALLOWS nullable assignee** (optional = true) - supports UNASSIGNED status
- Project remains **REQUIRED** (NOT NULL constraint)
- **User soft delete fully implemented** with @SQLDelete and @Where annotations
- Task assignee can be NULL (when user deleted or unassigned)
- Comment and Attachment entities defined but **no API endpoints** yet
- Project entity defined but **no management API** yet

---

##  Core Responsibilities

### 1. Domain Model Definition
- Define classes representing persistent entities (Task, User, Project)
- Represent business concepts and domain logic
- Encapsulate data and behavior related to domain objects
- Enforce domain constraints at the entity level

### 2. Database Mapping
- Map Java classes to database tables using JPA annotations
- Define column properties (type, length, nullable, unique)
- Implement primary and foreign keys
- Handle data type conversions

### 3. Relationships Management
- Define associations between entities (one-to-many, many-to-one, many-to-many)
- Configure cascading behavior (PERSIST, REMOVE, MERGE, REFRESH, DETACH)
- Specify fetch strategies (LAZY, EAGER)
- Handle bidirectional relationships correctly

### 4. Validation
- Apply Bean Validation constraints at entity level
- Enforce business rules on entity state
- Support validation at both entity and transaction boundaries
- Provide meaningful validation error messages

### 5. Auditing
- Track entity creation and modification metadata
- Record creator and modifier information
- Maintain audit timestamps automatically
- Support audit trail for compliance

---

##  Entity Files

```
entity/
├── Task.java              ✅ Main task domain entity
├── User.java              ✅ User entity (assignee)
├── Project.java           ✅ Project entity
├── Comment.java           ✅ Comment entity (cascade delete with Task)
├── Attachment.java        ✅ File attachment entity (cascade delete)
├── TaskStatus.java        ✅ Task status enumeration (PENDING, IN_PROGRESS, etc.)
├── TaskPriority.java      ✅ Task priority enumeration (LOW, MEDIUM, HIGH, CRITICAL)
└── README.md              📄 This file
```

**Note:** No BaseEntity or AuditListener - entities use direct `@CreationTimestamp` and `@UpdateTimestamp`

---

##  Key Concepts

### Base Entity Class

All entities inherit from a base class that provides common auditable fields.

\\\java
/**
 * Abstract base class for all entities
 * Provides common auditable fields and JPA callbacks
 */
@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    protected LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    protected LocalDateTime updatedAt;
    
    @CreatedBy
    @Column(nullable = false, updatable = false, length = 100)
    protected String createdBy;
    
    @LastModifiedBy
    @Column(nullable = false, length = 100)
    protected String updatedBy;
    
    @Version
    @Column(nullable = false)
    protected Long version;
}
\\\

### Task Entity - Complete Structure

```java
@Entity
@Table(
    name = "tasks",
    indexes = {
        @Index(name = "idx_task_status", columnList = "status"),
        @Index(name = "idx_task_priority", columnList = "priority"),
        @Index(name = "idx_task_assignee", columnList = "assignee_id"),
        @Index(name = "idx_task_project", columnList = "project_id"),
        @Index(name = "idx_task_due_date", columnList = "due_date"),
        @Index(name = "idx_task_assignee_status", columnList = "assignee_id, status"),
        @Index(name = "idx_task_deleted", columnList = "deleted"),
        @Index(name = "idx_task_deleted_at", columnList = "deleted_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ========== CORE FIELDS ==========
    
    @NotBlank(message = "Task title is required")
    @Size(min = 3, max = 255)
    @Column(nullable = false, length = 255)
    private String title;
    
    @NotBlank(message = "Task description is required")
    @Size(min = 10, max = 2000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @NotNull(message = "Task status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;
    
    @NotNull(message = "Task priority is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;
    
    @NotNull(message = "Due date is required")
    @FutureOrPresent
    @Column(nullable = false)
    private LocalDateTime dueDate;
    
    @Column
    private LocalDateTime startDate;
    
    @Column
    private LocalDateTime completedAt;
    
    @Min(0)
    @Max(999)
    @Column
    private Integer estimatedHours;
    
    @Size(max = 1000)
    @Column(length = 1000)
    private String notes;
    
    // ========== RELATIONSHIPS ==========
    
    /**
     * Many-to-One: Task → User (assignee)
     * ✅ NOW OPTIONAL (optional=true, nullable=true)
     * Allows UNASSIGNED tasks and user deletion with auto-unassignment
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "assignee_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)  // DB: ON DELETE SET NULL
    private User assignee;
    
    /**
     * Many-to-One: Task → Project
     * REQUIRED - Task must belong to a project
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    @NotNull(message = "Task project is required")
    private Project project;
    
    /**
     * One-to-Many: Task → Comments
     * Cascade ALL + orphanRemoval - deleting task deletes comments
     */
    @OneToMany(
        mappedBy = "task",
        cascade = CascadeType.ALL,
        fetch = FetchType.LAZY,
        orphanRemoval = true
    )
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
    
    /**
     * One-to-Many: Task → Attachments
     * Cascade ALL + orphanRemoval - deleting task deletes attachments
     * 
     * Cascade: ALL - Comments are deleted when task is deleted
     * Fetch: LAZY - Load comments only when explicitly accessed
     * OrphanRemoval: true - Remove comments when removed from collection
     */
    @OneToMany(
        mappedBy = "task",
        cascade = CascadeType.ALL,
        fetch = FetchType.LAZY,
        orphanRemoval = true
    )
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
    
    /**
     * One-to-Many relationship with Attachment
     * Task can have multiple attachments
     * 
     * Cascade: ALL - Attachments are deleted when task is deleted
     * Fetch: LAZY - Load attachments only when explicitly accessed
     * OrphanRemoval: true - Remove attachments when removed from collection
     */
    @OneToMany(
        mappedBy = "task",
        cascade = CascadeType.ALL,
        fetch = FetchType.LAZY,
        orphanRemoval = true
    )
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();
    
    // ==================== BUSINESS LOGIC ====================
    
    /**
     * Verify task can be assigned to user
     * Business rule: Only certain users can be assigned to certain projects
     */
    public void assignTo(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        this.assignee = user;
    }
    
    /**
     * Mark task as completed
     * Business rule: Can only mark non-deleted tasks as complete
     */
    public void markComplete() {
        if (this.status == TaskStatus.DELETED) {
            throw new IllegalStateException("Cannot complete a deleted task");
        }
        this.status = TaskStatus.COMPLETED;
    }
    
    /**
     * Add comment to task
     * Business rule: Comments cannot be added to deleted tasks
     */
    public void addComment(Comment comment) {
        if (this.status == TaskStatus.DELETED) {
            throw new IllegalStateException("Cannot add comment to deleted task");
        }
        comment.setTask(this);
        this.comments.add(comment);
    }
}
\\\

---

##  JPA Annotations Reference

### Class-Level Annotations

\\\java
@Entity                     // Marks class as JPA entity
@Table(name = "tasks")      // Maps to database table
@Table(name = "tasks", 
  uniqueConstraints = {...})  // Unique constraints
@Table(name = "tasks",
  indexes = {...})          // Database indexes
@MappedSuperclass           // Abstract base class (not an entity)
@EntityListeners(...)       // JPA listeners for callbacks
\\\

### Field-Level Annotations

\\\java
@Id                         // Primary key
@GeneratedValue(
  strategy = GenerationType.IDENTITY)  // Auto-increment
@Column(nullable = false)   // NOT NULL constraint
@Column(unique = true)      // UNIQUE constraint
@Column(length = 255)       // VARCHAR(255)
@Column(columnDefinition = "TEXT")  // Custom SQL type
@Transient                  // Not persisted
@Enumerated(EnumType.STRING)  // Enum handling
@Temporal(TemporalType.TIMESTAMP)  // Date/time mapping
@Version                    // Optimistic locking
\\\

---

##  Relationships in Detail

### One-to-Many Relationship

\\\java
/**
 * One-to-Many: One Task has many Comments
 * (Owner side - defines the relationship)
 */
@OneToMany(
    mappedBy = "task",              // Property name in Comment entity
    cascade = CascadeType.ALL,      // Delete comments when task deleted
    fetch = FetchType.LAZY,         // Load on demand
    orphanRemoval = true            // Remove comments when removed from list
)
private List<Comment> comments = new ArrayList<>();

/**
 * One-to-Many: One User has many Tasks
 */
@OneToMany(
    mappedBy = "assignee",
    cascade = CascadeType.NONE,     // Don't cascade - User is independent
    fetch = FetchType.LAZY
)
private List<Task> assignedTasks = new ArrayList<>();
\\\

### Many-to-One Relationship

\\\java
/**
 * Many-to-One: Many Tasks belong to One Project
 * (Inverse side - owns the foreign key)
 */
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(
    name = "project_id",           // Foreign key column
    nullable = false               // Required relationship
)
private Project project;

/**
 * Many-to-One: Many Tasks are assigned to One User
 */
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "assignee_id")
private User assignee;
\\\

### Many-to-Many Relationship

\\\java
/**
 * Many-to-Many: Many Users can be assigned to Many Projects
 * (Owner side)
 */
@ManyToMany
@JoinTable(
    name = "project_users",                    // Join table
    joinColumns = @JoinColumn(name = "project_id"),     // Owner FK
    inverseJoinColumns = @JoinColumn(name = "user_id")  // Inverse FK
)
private List<User> members = new ArrayList<>();

/**
 * Many-to-Many: Many Projects have many Users
 * (Inverse side)
 */
@ManyToMany(mappedBy = "members")
private List<Project> projects = new ArrayList<>();
\\\

---

##  Cascade Types Explained

\\\java
// CascadeType.PERSIST - Cascade save/insert
@OneToMany(cascade = CascadeType.PERSIST)
private List<Comment> comments;

// CascadeType.MERGE - Cascade merge/update
@OneToMany(cascade = CascadeType.MERGE)
private List<Comment> comments;

// CascadeType.REMOVE - Cascade delete
@OneToMany(cascade = CascadeType.REMOVE)
private List<Comment> comments;

// CascadeType.REFRESH - Cascade refresh from DB
@OneToMany(cascade = CascadeType.REFRESH)
private List<Comment> comments;

// CascadeType.DETACH - Cascade detach
@OneToMany(cascade = CascadeType.DETACH)
private List<Comment> comments;

// CascadeType.ALL - All cascade operations
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
private List<Comment> comments;
\\\

---

##  Fetch Strategies

### LAZY Loading (Recommended)

\\\java
/**
 * LAZY - Load related data only when accessed
 * Advantages: Better performance, smaller initial queries
 * Disadvantages: Potential LazyInitializationException if accessed outside session
 */
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "assignee_id")
private User assignee;

@OneToMany(fetch = FetchType.LAZY, mappedBy = "task")
private List<Comment> comments;
\\\

### EAGER Loading

\\\java
/**
 * EAGER - Load related data immediately
 * Advantages: Avoid LazyInitializationException
 * Disadvantages: Can load unnecessary data, potential N+1 queries
 * 
 * Use sparingly! Consider using JOIN FETCH in queries instead.
 */
@ManyToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "assignee_id")
private User assignee;
\\\

---

##  Complete Entity Examples

### User Entity

\\\java
/**
 * User entity representing an application user
 * 
 * Relationships:
 * - One-to-Many with Task (assigned tasks)
 * - One-to-Many with Comment (created comments)
 * - Many-to-Many with Project (project membership)
 * - Many-to-Many with UserRole (roles for authorization)
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_email", columnList = "email", unique = true),
        @Index(name = "idx_username", columnList = "username", unique = true)
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String fullName;
    
    @NotBlank(message = "Password is required")
    @Column(nullable = false, length = 255)
    private String passwordHash;
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column
    private LocalDateTime lastLoginAt;
    
    // ==================== RELATIONSHIPS ====================
    
    /**
     * One-to-Many: One User has many assigned Tasks
     */
    @OneToMany(
        mappedBy = "assignee",
        fetch = FetchType.LAZY,
        cascade = CascadeType.NONE
    )
    @Builder.Default
    private List<Task> assignedTasks = new ArrayList<>();
    
    /**
     * One-to-Many: One User creates many Comments
     */
    @OneToMany(
        mappedBy = "author",
        fetch = FetchType.LAZY,
        cascade = CascadeType.NONE
    )
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
    
    /**
     * Many-to-Many: User has multiple Roles
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<UserRole> roles = new HashSet<>();
    
    /**
     * Many-to-Many: User is member of multiple Projects
     */
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "members")
    @Builder.Default
    private List<Project> projects = new ArrayList<>();
    
    // ==================== BUSINESS LOGIC ====================
    
    /**
     * Add role to user
     */
    public void addRole(UserRole role) {
        this.roles.add(role);
    }
    
    /**
     * Check if user has specific role
     */
    public boolean hasRole(String roleName) {
        return roles.stream()
            .anyMatch(r -> r.getName().equals(roleName));
    }
}
\\\

### Project Entity

\\\java
/**
 * Project entity representing a project/workspace
 * 
 * Relationships:
 * - One-to-Many with Task (tasks in project)
 * - Many-to-Many with User (project members)
 */
@Entity
@Table(
    name = "projects",
    indexes = {
        @Index(name = "idx_name", columnList = "name")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project extends BaseEntity {
    
    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 100)
    @Column(nullable = false, length = 100)
    private String name;
    
    @Size(max = 500)
    @Column(length = 500)
    private String description;
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
    
    // ==================== RELATIONSHIPS ====================
    
    /**
     * One-to-Many: One Project has many Tasks
     */
    @OneToMany(
        mappedBy = "project",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @Builder.Default
    private List<Task> tasks = new ArrayList<>();
    
    /**
     * Many-to-Many: Project has multiple member Users
     */
    @ManyToMany
    @JoinTable(
        name = "project_users",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private List<User> members = new ArrayList<>();
}
\\\

### Comment Entity

\\\java
/**
 * Comment entity representing a comment on a task
 * 
 * Relationships:
 * - Many-to-One with Task (belongs to task)
 * - Many-to-One with User (created by user)
 */
@Entity
@Table(
    name = "comments",
    indexes = {
        @Index(name = "idx_task_id", columnList = "task_id"),
        @Index(name = "idx_author_id", columnList = "author_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity {
    
    @NotBlank(message = "Comment text is required")
    @Size(min = 1, max = 1000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;
    
    // ==================== RELATIONSHIPS ====================
    
    /**
     * Many-to-One: Many Comments belong to One Task
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    @NotNull(message = "Task is required")
    private Task task;
    
    /**
     * Many-to-One: Many Comments are created by One User
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    @NotNull(message = "Author is required")
    private User author;
}
\\\

---

##  Auditing

### Auditable Fields

All entities extend \BaseEntity\ which provides auditing fields:

\\\java
@CreatedDate
@Column(nullable = false, updatable = false)
private LocalDateTime createdAt;

@LastModifiedDate
@Column(nullable = false)
private LocalDateTime updatedAt;

@CreatedBy
@Column(nullable = false, updatable = false, length = 100)
private String createdBy;

@LastModifiedBy
@Column(nullable = false, length = 100)
private String updatedBy;

@Version
@Column(nullable = false)
private Long version;
\\\

### Optimistic Locking

The \@Version\ annotation enables optimistic locking to prevent concurrent update conflicts:

\\\java
@Version
@Column(nullable = false)
private Long version;

// When updating entity, Hibernate increments version
// If another transaction modified the entity, update fails with OptimisticLockException
\\\

### AuditorAware Implementation

\\\java
/**
 * Provides the current auditor (user) for @CreatedBy and @LastModifiedBy
 * Gets current user from Spring Security context
 */
@Component
public class AuditorAwareImpl implements AuditorAware<String> {
    
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = 
            SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("SYSTEM");
        }
        
        return Optional.of(authentication.getName());
    }
}
\\\

---

##  Enumerations

### Task Status

\\\java
public enum TaskStatus {
    UNASSIGNED("Unassigned"),      // ✅ NEW: Task has no assignee
    PENDING("Pending"),             // Not started yet (has assignee)
    IN_PROGRESS("In Progress"),     // Currently being worked on
    COMPLETED("Completed"),         // Finished
    BLOCKED("Blocked"),             // Waiting on dependency
    CANCELLED("Cancelled"),         // No longer needed
    DELETED("Deleted");             // Soft deleted
    
    private final String displayName;
    
    TaskStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
\\\

**UNASSIGNED Status - NEW Feature:**
- **Purpose:** Mark tasks without assignee
- **Set when:** User deleted → tasks auto-unassigned
- **Difference from PENDING:** 
  - PENDING = has assignee, not started
  - UNASSIGNED = no assignee, needs assignment
- **Status Flow:**
  ```
  User deleted → Tasks: assignee=NULL, status=UNASSIGNED
  Admin reassigns → status: UNASSIGNED → PENDING
  Normal flow → PENDING → IN_PROGRESS → COMPLETED
  ```

### Task Priority

\\\java
public enum TaskPriority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);
    
    private final int level;
    
    TaskPriority(int level) {
        this.level = level;
    }
    
    public int getLevel() {
        return level;
    }
}
\\\

### User Role

\\\java
/**
 * User role for role-based access control
 * Mapped to database for flexible role management
 */
@Entity
@Table(name = "user_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String name;  // ADMIN, MANAGER, USER
    
    @Column(length = 255)
    private String description;
}
\\\

---

##  Validation at Entity Level

### Constraint Annotations

\\\java
@Entity
public class Task extends BaseEntity {
    
    @NotBlank(message = "Title cannot be blank")
    @Size(min = 5, max = 255)
    @Column(nullable = false, length = 255)
    private String title;
    
    @NotNull(message = "Due date is required")
    @FutureOrPresent(message = "Due date must be in future")
    @Column(nullable = false)
    private LocalDateTime dueDate;
    
    @Email(message = "Invalid email format")
    @Column(unique = true)
    private String notificationEmail;
    
    @Min(value = 0, message = "Estimated hours cannot be negative")
    @Max(value = 999, message = "Estimated hours cannot exceed 999")
    @Column
    private Integer estimatedHours;
}
\\\

### Custom Validation Annotation

\\\java
/**
 * Custom validator to ensure due date is after start date
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidTaskDatesValidator.class)
public @interface ValidTaskDates {
    String message() default "Due date must be after start date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

/**
 * Validator implementation
 */
public class ValidTaskDatesValidator 
    implements ConstraintValidator<ValidTaskDates, Task> {
    
    @Override
    public boolean isValid(Task task, ConstraintValidatorContext context) {
        if (task == null) return true;
        
        return task.getDueDate().isAfter(task.getStartDate());
    }
}

/**
 * Apply to entity
 */
@Entity
@ValidTaskDates
public class Task extends BaseEntity {
    private LocalDateTime startDate;
    private LocalDateTime dueDate;
}
\\\

---

##  Database Design Patterns

### Indexes for Performance

\\\java
@Entity
@Table(
    name = "tasks",
    indexes = {
        // Single column index
        @Index(name = "idx_status", columnList = "status"),
        
        // Composite index (multiple columns)
        @Index(name = "idx_user_status", columnList = "assignee_id, status"),
        
        // Foreign key index
        @Index(name = "idx_project_id", columnList = "project_id"),
        
        // Timestamp index for range queries
        @Index(name = "idx_created_at", columnList = "created_at")
    }
)
public class Task extends BaseEntity {
    // Fields...
}
\\\

### Unique Constraints

\\\java
@Entity
@Table(
    name = "tasks",
    uniqueConstraints = {
        // Single column unique constraint
        @UniqueConstraint(
            name = "uq_external_id",
            columnNames = "external_id"
        ),
        
        // Composite unique constraint
        @UniqueConstraint(
            name = "uq_project_task_name",
            columnNames = {"project_id", "title"}
        )
    }
)
public class Task extends BaseEntity {
    // Fields...
}
\\\

---

##  Best Practices

### 1. Use Proper Fetch Strategies

\\\java
// GOOD: LAZY for collections, LAZY for relationships
@ManyToOne(fetch = FetchType.LAZY)
private User assignee;

@OneToMany(fetch = FetchType.LAZY, mappedBy = "task")
private List<Comment> comments;

// BAD: EAGER loading by default
@ManyToOne(fetch = FetchType.EAGER)
private User assignee;

@OneToMany(fetch = FetchType.EAGER, mappedBy = "task")
private List<Comment> comments;
\\\

### 2. Initialize Collections

\\\java
// GOOD: Initialize to prevent NullPointerException
@OneToMany(mappedBy = "task")
@Builder.Default
private List<Comment> comments = new ArrayList<>();

// BAD: Can cause null checks throughout code
@OneToMany(mappedBy = "task")
private List<Comment> comments;  // May be null
\\\

### 3. Avoid Bidirectional Relationships When Possible

\\\java
// GOOD: Unidirectional (simpler)
@Entity
public class Task extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    private User assignee;
}

// COMPLEX: Bidirectional (requires careful management)
@Entity
public class Task extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    private User assignee;
}

@Entity
public class User extends BaseEntity {
    @OneToMany(mappedBy = "assignee")
    private List<Task> tasks;
}
\\\

### 4. Use Immutable Value Objects

\\\java
/**
 * Value Object: Address is immutable
 * Embedded in Entity, not mapped separately
 */
@Embeddable
@Value
public class Address {
    @Column(length = 100)
    private final String street;
    
    @Column(length = 50)
    private final String city;
    
    @Column(length = 50)
    private final String state;
    
    @Column(length = 10)
    private final String zipCode;
}

/**
 * Embed in Entity
 */
@Entity
public class User extends BaseEntity {
    @Embedded
    private Address address;
}
\\\

### 5. Entity Should Represent Domain Concept

\\\java
// GOOD: Entity has business logic
@Entity
public class Task extends BaseEntity {
    
    public void markComplete() {
        if (this.status == TaskStatus.DELETED) {
            throw new IllegalStateException("Cannot complete deleted task");
        }
        this.status = TaskStatus.COMPLETED;
    }
    
    public void assignTo(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        this.assignee = user;
    }
}

// BAD: Entity is just a data container
@Entity
public class Task {
    private Long id;
    private String title;
    // Getters/setters only
}
\\\

### 6. Use @NotNull for Required Relationships

\\\java
// GOOD: Clear that relationship is required
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(nullable = false)
@NotNull(message = "Assignee is required")
private User assignee;

// BAD: Ambiguous if relationship is optional
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "assignee_id")
private User assignee;
\\\

### 7. Document Complex Relationships

\\\java
/**
 * One-to-Many relationship with Comment
 * 
 * Purpose: Track all comments on this task for audit and discussion
 * 
 * Fetch Strategy: LAZY - Comments loaded only when explicitly accessed
 * Cascade: ALL - Comments are automatically deleted when task is deleted
 * OrphanRemoval: true - Comments removed from list are deleted from DB
 * 
 * Related Entity: Comment
 * Inverse Property: Comment.task
 * 
 * Example:
 *   task.addComment(newComment);  // Adds comment to task
 *   task.getComments().clear();    // Would delete all comments
 * 
 * Business Rules:
 * - Cannot add comments to deleted tasks
 * - Comments cannot be null
 * - Comments are ordered by creation time
 */
@OneToMany(
    mappedBy = "task",
    cascade = CascadeType.ALL,
    fetch = FetchType.LAZY,
    orphanRemoval = true
)
@OrderBy("createdAt DESC")
@Builder.Default
private List<Comment> comments = new ArrayList<>();
\\\

---

##  Common Patterns

### Soft Delete Pattern

\\\java
@Entity
public class Task extends BaseEntity {
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
    
    /**
     * Soft delete: Mark as deleted without removing from DB
     */
    public void softDelete() {
        this.deleted = true;
        this.status = TaskStatus.DELETED;
    }
    
    /**
     * Check if entity is not deleted
     */
    public boolean isActive() {
        return !deleted;
    }
}

// Query active records only
// SELECT * FROM tasks WHERE deleted = false
\\\

### Temporal Entity Pattern

\\\java
@Entity
public class Task extends BaseEntity {
    
    @NotNull
    private LocalDateTime startDate;
    
    @NotNull
    private LocalDateTime endDate;
    
    /**
     * Check if task is currently active (within date range)
     */
    public boolean isActive(LocalDateTime now) {
        return !now.isBefore(startDate) && !now.isAfter(endDate);
    }
}
\\\

### Status Enum Pattern

\\\java
@Entity
public class Task extends BaseEntity {
    
    @Enumerated(EnumType.STRING)
    private TaskStatus status;
    
    /**
     * Check if can transition to new status
     */
    public boolean canTransitionTo(TaskStatus newStatus) {
        return switch(this.status) {
            case PENDING -> newStatus == TaskStatus.IN_PROGRESS;
            case IN_PROGRESS -> newStatus == TaskStatus.COMPLETED;
            case COMPLETED -> false;
            case BLOCKED -> newStatus == TaskStatus.IN_PROGRESS;
            default -> false;
        };
    }
}
\\\

---

##  Entity Lifecycle Hooks

### JPA Callbacks

\\\java
@Entity
public class Task extends BaseEntity {
    
    /**
     * Called before entity is persisted
     */
    @PrePersist
    protected void onPrePersist() {
        if (this.status == null) {
            this.status = TaskStatus.PENDING;
        }
    }
    
    /**
     * Called after entity is persisted
     */
    @PostPersist
    protected void onPostPersist() {
        // Log creation, send event, etc.
    }
    
    /**
     * Called before entity is updated
     */
    @PreUpdate
    protected void onPreUpdate() {
        // Validate state before update
    }
    
    /**
     * Called after entity is updated
     */
    @PostUpdate
    protected void onPostUpdate() {
        // Log update, publish event, etc.
    }
    
    /**
     * Called before entity is deleted
     */
    @PreRemove
    protected void onPreRemove() {
        // Validate deletion is allowed
    }
}
\\\

---

##  Testing Entities

### Unit Test Example

\\\java
@SpringBootTest
class TaskEntityTest {
    
    @Test
    void markComplete_WithPendingTask_ChangesStatus() {
        Task task = new Task();
        task.setStatus(TaskStatus.PENDING);
        
        task.markComplete();
        
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
    }
    
    @Test
    void markComplete_WithDeletedTask_ThrowsException() {
        Task task = new Task();
        task.setStatus(TaskStatus.DELETED);
        
        assertThrows(IllegalStateException.class, () -> task.markComplete());
    }
    
    @Test
    void addComment_WithValidComment_AddsToList() {
        Task task = new Task();
        Comment comment = new Comment();
        
        task.addComment(comment);
        
        assertTrue(task.getComments().contains(comment));
        assertEquals(task, comment.getTask());
    }
}
\\\

### JPA Validation Test

\\\java
@SpringBootTest
class TaskValidationTest {
    
    @Autowired
    private Validator validator;
    
    @Test
    void validTask_PassesValidation() {
        Task task = Task.builder()
            .title("Valid Title")
            .description("Valid description at least 10 chars")
            .status(TaskStatus.PENDING)
            .priority(TaskPriority.MEDIUM)
            .dueDate(LocalDateTime.now().plusDays(1))
            .build();
        
        Set<ConstraintViolation<Task>> violations = validator.validate(task);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void missingTitle_FailsValidation() {
        Task task = Task.builder()
            .title(null)
            .status(TaskStatus.PENDING)
            .build();
        
        Set<ConstraintViolation<Task>> violations = validator.validate(task);
        assertFalse(violations.isEmpty());
    }
}
\\\

---

##  Common Issues and Solutions

### LazyInitializationException

\\\java
// PROBLEM: Accessing lazy collection outside session
Task task = taskRepository.findById(1L).get();
task.getComments().size();  // LazyInitializationException

// SOLUTION 1: Use JOIN FETCH in query
@Query("SELECT t FROM Task t JOIN FETCH t.comments WHERE t.id = :id")
Task findTaskWithComments(@Param("id") Long id);

// SOLUTION 2: Use @Transactional
@Transactional
public Task getTaskWithComments(Long id) {
    return taskRepository.findById(id)
        .map(task -> {
            task.getComments().size();  // OK - still in session
            return task;
        })
        .orElseThrow();
}
\\\

### N+1 Query Problem

\\\java
// PROBLEM: N+1 queries
List<Task> tasks = taskRepository.findAll();  // 1 query
for (Task task : tasks) {
    User assignee = task.getAssignee();  // N queries (1 per task)
}

// SOLUTION: Use JOIN FETCH
@Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.assignee")
List<Task> findAllWithAssignee();

// Usage: No N+1 problem
List<Task> tasks = taskRepository.findAllWithAssignee();
\\\

### OptimisticLockException

\\\java
// PROBLEM: Concurrent update conflict
// Transaction 1 and 2 both read version 1
// Transaction 1 commits, increments to version 2
// Transaction 2 tries to commit, fails - version changed

// SOLUTION: Retry logic
@Retryable(
    retryFor = OptimisticLockException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 100)
)
public Task updateTask(Long id, String newTitle) {
    Task task = taskRepository.findById(id).get();
    task.setTitle(newTitle);
    return taskRepository.save(task);
}
\\\

---

##  Entity Checklist

When creating new entities:

- [ ] Extend \BaseEntity\ class
- [ ] Apply \@Entity\ and \@Table\ annotations
- [ ] Use \@NotBlank\ for required string fields
- [ ] Use \@NotNull\ for required object fields
- [ ] Apply appropriate validation annotations
- [ ] Use \@Column\ for column constraints
- [ ] Define indexes for frequently queried fields
- [ ] Use \FetchType.LAZY\ for relationships
- [ ] Initialize collections with \ArrayList\
- [ ] Document complex relationships
- [ ] Implement entity business logic
- [ ] Add JPA callbacks (@PrePersist, @PreUpdate) if needed
- [ ] Avoid bidirectional relationships if possible
- [ ] Write unit tests for business logic
- [ ] Write JPA validation tests

---

##  Related Documentation

- **ARCHITECTURE.md** - Overall system architecture
- **README.md** - Main project overview
- **Repository Layer** - Data access patterns
- **Service Layer** - Business logic that uses entities
- **Database Design** - Entity-relationship diagram
- **Flyway Migrations** - Database schema creation

---

##  Quick Reference

### Common Field Annotations

\\\
@Id                           Primary key
@GeneratedValue(IDENTITY)     Auto-increment
@Column(nullable=false)       NOT NULL
@Column(unique=true)          UNIQUE constraint
@Column(length=100)           VARCHAR(100)
@Enumerated(STRING)           Store enum as string
@ManyToOne(LAZY)              Many-to-One relationship
@OneToMany(mappedBy="x")      One-to-Many inverse
@CreatedDate                  Audit timestamp
@CreatedBy                    Audit creator
\\\

### Common Validations

\\\
@NotNull                      Required (not null)
@NotBlank                     Required (not blank string)
@Size(min=3, max=255)         String length
@Positive                     Must be > 0
@FutureOrPresent              Date constraint
@Email                        Email format
\\\

    @OneToMany(
        mappedBy = "task",
        cascade = CascadeType.ALL,
        fetch = FetchType.LAZY,
        orphanRemoval = true
    )
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();
    
    // ========== SOFT DELETE ==========
    
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    // ========== AUDIT FIELDS ==========
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // ========== BUSINESS METHODS ==========
    
    public void assignTo(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        this.assignee = user;
    }
    
    public boolean isAssignedTo(User user) {
        return user != null && this.assignee != null 
            && this.assignee.getId().equals(user.getId());
    }
    
    public void complete() {
        this.status = TaskStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
    
    public boolean isOverdue() {
        return LocalDateTime.now().isAfter(this.dueDate) 
            && this.status != TaskStatus.COMPLETED;
    }
    
    // Soft delete methods
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
    
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
    }
}
```

---

## Task Entity Key Details

### Database Table: `tasks`

**Indexes for Performance:**
- `idx_task_assignee_status` - Query tasks by assignee and status
- `idx_task_project` - Query tasks by project
- `idx_task_due_date` - Query overdue tasks
- `idx_task_deleted` - Filter out soft-deleted tasks

### Relationships Explained

#### 1. Task → User (Assignee) - Many-to-One

**Current Implementation:**
```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "assignee_id", nullable = false)
@NotNull
private User assignee;
```

**What this means:**
- ✅ Every task MUST have an assignee (NOT NULL constraint)
- ✅ Lazy loading - assignee loaded only when accessed
- ❌ **Cannot create unassigned tasks**
- ❌ **Cannot remove assignee from task**
- ❌ **Cannot delete user if they have tasks** (foreign key violation)

**Database:**
```sql
CREATE TABLE tasks (
    assignee_id BIGINT NOT NULL,
    FOREIGN KEY (assignee_id) REFERENCES users(id)
    -- No ON DELETE action = RESTRICT (blocks user deletion)
);
```

#### 2. Task → Project - Many-to-One

**Implementation:**
```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "project_id", nullable = false)
@NotNull
private Project project;
```

**What this means:**
- ✅ Every task MUST belong to a project
- ✅ Lazy loading for performance
- ❌ Cannot delete project if it has tasks

#### 3. Task → Comments - One-to-Many

**Implementation:**
```java
@OneToMany(
    mappedBy = "task",
    cascade = CascadeType.ALL,
    orphanRemoval = true
)
private List<Comment> comments;
```

**What this means:**
- ✅ Task can have multiple comments
- ✅ **Deleting task deletes all comments** (CASCADE ALL)
- ✅ Removing comment from list deletes it (orphanRemoval)
- ✅ Comments belong to task (mappedBy = "task")

#### 4. Task → Attachments - One-to-Many

Same behavior as comments - cascade delete.

---

## TaskStatus Enum

```java
public enum TaskStatus {
    PENDING,      // Task created, waiting to start
    IN_PROGRESS,  // Actively being worked on
    BLOCKED,      // Cannot proceed due to dependency
    IN_REVIEW,    // Waiting for code review/approval
    COMPLETED,    // Finished successfully
    CANCELLED     // Task cancelled/obsolete
}
```

**Default:** New tasks are `PENDING`

---

## TaskPriority Enum

```java
public enum TaskPriority {
    LOW,       // Nice to have
    MEDIUM,    // Normal priority
    HIGH,      // Important
    CRITICAL   // Urgent, highest priority
}
```

**Default:** New tasks are `MEDIUM`

---

## Soft Delete Implementation

### User Entity - Soft Delete ✅ FULLY IMPLEMENTED

**Fields:**
```java
@Column(nullable = false)
@Builder.Default
private Boolean deleted = false;        // Soft delete flag

@Column(name = "deleted_at")
private LocalDateTime deletedAt;        // Deletion timestamp

@Column(name = "deleted_by")
private Long deletedBy;                 // Admin who deleted (audit trail)
```

**Hibernate Annotations:**
```java
@SQLDelete(sql = "UPDATE users SET deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
public class User { ... }
```

**How it works:**
1. **@SQLDelete** - Override DELETE command to UPDATE instead
   - Physical DELETE becomes logical UPDATE
   - Data preserved in database

2. **@Where** - Auto-filter deleted users in all queries
   - `userRepository.findById(1)` only returns active users
   - `userRepository.findAll()` excludes deleted users
   - Bypass with custom query: `@Query("SELECT u FROM User u WHERE u.id = :id")`

**Business Rules:**
- DELETE user → soft delete only (deleted = true)
- Tasks → unassigned automatically (assignee = NULL, status = UNASSIGNED)
- Comments → preserved (audit trail, author_id retained)
- Projects → preserved (business continuity, owner_id retained)

**Restore Capability:**
```java
// UserService.restoreUser()
user.setDeleted(false);
user.setDeletedAt(null);
user.setDeletedBy(null);
userRepository.save(user);
```

---

### Task Entity - Soft Delete (Partial)

**Fields:**
```java
private boolean deleted = false;        // Flag: is task deleted?
private LocalDateTime deletedAt = null; // When was it deleted?
```

**⚠️ Known Issue:**
Soft delete fields are **defined but not fully used**:
- ✅ Fields exist in entity
- ❌ No @SQLDelete annotation
- ❌ Repositories don't filter `deleted = false` automatically
- ❌ APIs use hard delete, not soft delete

**Recommended Fix:**
Add Hibernate annotations like User entity:
```java
@SQLDelete(sql = "UPDATE tasks SET deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
public class Task { ... }
```

---

## Known Issues & Limitations

### ✅ RESOLVED Issues

**1. Cannot remove assignee from task** ✅ RESOLVED
```java
// NOW POSSIBLE:
task.setAssignee(null); // ✅ Works! Assignee is optional
```
**Resolution:** 
- ✅ Changed Task.assignee to `optional = true, nullable = true`
- ✅ Added @OnDelete(SET_NULL) annotation
- ✅ Added UNASSIGNED enum to TaskStatus
- ✅ Tasks can be unassigned
- ✅ Users can be deleted (tasks auto-unassigned)

**Implementation:**
```java
@ManyToOne(fetch = FetchType.LAZY, optional = true)  // ✅ Changed
@JoinColumn(name = "assignee_id", nullable = true)   // ✅ Changed
@OnDelete(action = OnDeleteAction.SET_NULL)          // ✅ Added
private User assignee;
```

---

**2. Foreign key blocks user deletion** ✅ RESOLVED
```java
// NOW WORKS:
userService.deleteUser(1L); // ✅ Soft delete + unassign tasks
```

**Resolution:**
- ✅ Implemented User soft delete (@SQLDelete, @Where)
- ✅ Added ON DELETE SET NULL to Task.assignee
- ✅ Bulk unassign tasks when user deleted (1 query, not N)
- ✅ Tasks status → UNASSIGNED automatically
- ✅ Comments preserved (audit trail)
- ✅ Projects preserved (business continuity)

**Business Flow:**
```java
// UserService.deleteUser()
1. Find user (including deleted)
2. Check if already deleted
3. Count resources (tasks, comments, projects)
4. Bulk unassign tasks: assignee=NULL, status=UNASSIGNED
5. Preserve comments (keep author_id)
6. Preserve projects (keep owner_id)
7. Set deleted=true, deletedAt=NOW()
```

---

### ❌ Remaining Issues

**3. Task soft delete not enforced**
```java
// This returns deleted tasks too:
taskRepository.findAll(); // ❌ No filter
```

**Status:** NOT RESOLVED
- Task has soft delete fields but no @SQLDelete/@Where
- APIs use hard delete (physical DELETE)
- Queries don't filter deleted=false

**Solution:** Add Hibernate annotations like User entity:
```java
@SQLDelete(sql = "UPDATE tasks SET deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
public class Task { ... }
```

---

## Business Methods

### Task Completion
```java
task.complete();
// Sets status = COMPLETED, completedAt = now()
```

### Check Overdue
```java
if (task.isOverdue()) {
    // Send notification
}
```

### Assign Task
```java
task.assignTo(user);
// Validates user not null
```

---

## Related Documentation

- [TaskService](../service/README.md) - Business logic for tasks
- [TaskController](../api/README.md) - REST API endpoints
- [TaskRepository](../repository/README.md) - Data access
- [Main README](../../../README.md) - Project overview

---

**Last Updated:** December 14, 2025  
**Version:** 0.5.0 - MVP Phase  
**Status:** Core entity complete, soft delete partially implemented
