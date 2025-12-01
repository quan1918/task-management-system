# Repository Layer (Data Access)

##  Overview

The **Repository layer** defines the data access abstraction for the application. Using Spring Data JPA, repositories provide a clean interface for persisting and querying domain entities without exposing implementation details.

**Location:** \src/main/java/com/taskmanagement/repository/\

**Responsibility:** Implement data access operations, provide query methods, and manage entity persistence through Spring Data repositories

---

##  Core Responsibilities

### 1. Entity Persistence
- Save new entities to database
- Update existing entities
- Delete entities (hard and soft delete)
- Batch operations for performance

### 2. Query Operations
- Find entities by ID
- Find all entities with pagination and sorting
- Find by specific fields (findByUsername, findByEmail, etc.)
- Complex queries using @Query annotation

### 3. Custom Query Methods
- Derived query methods from method names
- JPQL queries with @Query
- Native SQL queries when needed
- Query DSL for complex criteria

### 4. Pagination and Sorting
- Support Page and Slice for large datasets
- Dynamic sorting with Sort objects
- Pageable parameter handling
- Count operations for totals

### 5. Performance Optimization
- Index usage for frequently queried fields
- Lazy vs eager loading strategies
- Query result caching
- Batch fetching and query optimization

---

##  Folder Structure

\\\
repository/
 TaskRepository.java                    # Task data access
 UserRepository.java                    # User data access
 ProjectRepository.java                 # Project data access
 CommentRepository.java                 # Comment data access
 ProjectStatisticsRepository.java       # Read model repository
 EventStoreRepository.java              # Event sourcing repository

 custom/
    TaskRepositoryCustom.java           # Custom task query interface
    TaskRepositoryImpl.java              # Custom task query implementation
    UserRepositoryCustom.java           # Custom user query interface
    UserRepositoryImpl.java              # Custom user query implementation
    BaseRepositoryCustom.java           # Base custom operations

 specification/
    TaskSpecification.java              # JPA Criteria/Specifications
    UserSpecification.java              # User specifications
    ProjectSpecification.java           # Project specifications

 README.md                              # This file
\\\

---

##  Basic Repository Interface

### Standard JpaRepository

\\\java
/**
 * Spring Data JPA repository for Task entity
 * Provides standard CRUD and query operations
 * 
 * Extends JpaRepository which provides:
 * - save(Task)
 * - saveAll(Iterable<Task>)
 * - findById(Long)
 * - findAll()
 * - findAll(Pageable)
 * - delete(Task)
 * - deleteById(Long)
 * - count()
 * - exists(Long)
 */
public interface TaskRepository extends JpaRepository<Task, Long> {
    
}
\\\

---

##  Derived Query Methods

Automatically generate queries from method names:

\\\java
/**
 * Derived query methods
 * Spring Data JPA generates the SQL based on method name
 */
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    /**
     * Find tasks by status
     * Generates: SELECT * FROM tasks WHERE status = ?1
     */
    List<Task> findByStatus(TaskStatus status);
    
    /**
     * Find tasks by assignee
     * Generates: SELECT * FROM tasks WHERE assignee_id = ?1
     */
    List<Task> findByAssignee(User assignee);
    
    /**
     * Find tasks by project
     * Generates: SELECT * FROM tasks WHERE project_id = ?1
     */
    List<Task> findByProject(Project project);
    
    /**
     * Find tasks with pagination
     * Returns Page object with pagination info
     */
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
    
    /**
     * Find and sort tasks by due date
     */
    List<Task> findByStatusOrderByDueDateAsc(TaskStatus status);
    
    /**
     * Find with multiple conditions (AND)
     * Generates: SELECT * FROM tasks WHERE status = ?1 AND priority = ?2
     */
    List<Task> findByStatusAndPriority(TaskStatus status, TaskPriority priority);
    
    /**
     * Find with OR condition
     * Generates: SELECT * FROM tasks WHERE status = ?1 OR status = ?2
     */
    List<Task> findByStatusOrStatus(TaskStatus status1, TaskStatus status2);
    
    /**
     * Find with NOT condition
     * Generates: SELECT * FROM tasks WHERE status != ?1
     */
    List<Task> findByStatusNot(TaskStatus status);
    
    /**
     * Find with LIKE (case-insensitive search)
     * Generates: SELECT * FROM tasks WHERE LOWER(title) LIKE LOWER(CONCAT('%', ?1, '%'))
     */
    List<Task> findByTitleContainingIgnoreCase(String titlePart);
    
    /**
     * Find with comparison operators
     * Generates: SELECT * FROM tasks WHERE due_date >= ?1
     */
    List<Task> findByDueDateGreaterThanEqual(LocalDateTime dueDate);
    
    /**
     * Find with BETWEEN
     * Generates: SELECT * FROM tasks WHERE created_at BETWEEN ?1 AND ?2
     */
    List<Task> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find with IN
     * Generates: SELECT * FROM tasks WHERE status IN (?1)
     */
    List<Task> findByStatusIn(List<TaskStatus> statuses);
    
    /**
     * Check existence
     * Generates: SELECT COUNT(*) > 0 FROM tasks WHERE id = ?1
     */
    boolean existsByIdAndStatus(Long id, TaskStatus status);
    
    /**
     * Count by condition
     * Generates: SELECT COUNT(*) FROM tasks WHERE status = ?1
     */
    long countByStatus(TaskStatus status);
    
    /**
     * Delete by condition
     * Generates: DELETE FROM tasks WHERE status = ?1
     */
    void deleteByStatus(TaskStatus status);
}
\\\

---

##  Custom @Query Methods

For complex queries, use @Query annotation:

\\\java
/**
 * Custom queries using @Query annotation
 * JPQL (Java Persistence Query Language)
 */
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    /**
     * Find tasks by project with sorting
     */
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId ORDER BY t.createdAt DESC")
    List<Task> findByProjectIdOrdered(@Param("projectId") Long projectId);
    
    /**
     * Find overdue tasks
     * Uses LocalDateTime.now() for comparison
     */
    @Query("SELECT t FROM Task t WHERE t.dueDate < CURRENT_TIMESTAMP AND t.status != 'COMPLETED'")
    List<Task> findOverdueTasks();
    
    /**
     * Count tasks by status for a project
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status = :status")
    long countByProjectAndStatus(@Param("projectId") Long projectId, @Param("status") TaskStatus status);
    
    /**
     * Find tasks with related data (JOIN FETCH for eager loading)
     * Prevents N+1 queries
     */
    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.assignee LEFT JOIN FETCH t.comments WHERE t.project.id = :projectId")
    List<Task> findByProjectIdWithRelations(@Param("projectId") Long projectId);
    
    /**
     * Find tasks assigned to user with pagination
     */
    @Query("SELECT t FROM Task t WHERE t.assignee.id = :assigneeId AND t.status IN (:statuses)")
    Page<Task> findUserTasksByStatus(
        @Param("assigneeId") Long assigneeId,
        @Param("statuses") List<TaskStatus> statuses,
        Pageable pageable
    );
    
    /**
     * Complex query with subquery
     */
    @Query("SELECT t FROM Task t WHERE t.id IN (SELECT c.task.id FROM Comment c WHERE c.author.id = :userId)")
    List<Task> findTasksWithCommentsByUser(@Param("userId") Long userId);
    
    /**
     * Projection - select specific columns only
     */
    @Query("SELECT new com.taskmanagement.dto.response.TaskSummary(t.id, t.title, t.status) FROM Task t WHERE t.project.id = :projectId")
    List<TaskSummary> findTaskSummaries(@Param("projectId") Long projectId);
    
    /**
     * Native SQL query for performance-critical operations
     */
    @Query(value = "SELECT t.* FROM tasks t JOIN projects p ON t.project_id = p.id WHERE p.active = true ORDER BY t.due_date ASC LIMIT :limit", nativeQuery = true)
    List<Task> findActiveProjectTasksNative(@Param("limit") int limit);
    
    /**
     * Update query using @Modifying
     */
    @Modifying
    @Transactional
    @Query("UPDATE Task t SET t.status = :newStatus WHERE t.id = :taskId")
    void updateTaskStatus(@Param("taskId") Long taskId, @Param("newStatus") TaskStatus newStatus);
    
    /**
     * Delete query using @Modifying
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Task t WHERE t.project.id = :projectId AND t.status = :status")
    void deleteProjectTasksByStatus(@Param("projectId") Long projectId, @Param("status") TaskStatus status);
}
\\\

---

##  Example Repository Implementations

### User Repository

\\\java
/**
 * Repository for User entity
 * Provides authentication and user lookup operations
 */
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by username for login
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if username already exists
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if email already exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Find all active users with pagination
     */
    Page<User> findByActiveTrue(Pageable pageable);
    
    /**
     * Find users by role
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRole(@Param("roleName") String roleName);
    
    /**
     * Find users in a project
     */
    @Query("SELECT u FROM User u JOIN u.projects p WHERE p.id = :projectId")
    List<User> findByProjectId(@Param("projectId") Long projectId);
    
    /**
     * Search users by name (case-insensitive)
     */
    List<User> findByFullNameContainingIgnoreCase(String namePart);
    
    /**
     * Find user by username with roles eager loaded
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);
    
    /**
     * Update last login timestamp
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLoginAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId);
}
\\\

### Project Repository

\\\java
/**
 * Repository for Project entity
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    /**
     * Find active projects with pagination
     */
    Page<Project> findByActiveTrue(Pageable pageable);
    
    /**
     * Find projects by name
     */
    Optional<Project> findByName(String name);
    
    /**
     * Find all projects for a user
     */
    @Query("SELECT p FROM Project p JOIN p.members u WHERE u.id = :userId")
    List<Project> findByMemberId(@Param("userId") Long userId);
    
    /**
     * Count active projects
     */
    long countByActiveTrue();
    
    /**
     * Find project with statistics
     */
    @Query("SELECT p FROM Project p WHERE p.id = :projectId")
    Optional<Project> findByIdWithStats(@Param("projectId") Long projectId);
}
\\\

---

##  Pagination and Sorting

### Using Pageable

\\\java
/**
 * Pageable parameter provides pagination and sorting
 */
@Service
public class TaskService {
    
    private final TaskRepository taskRepository;
    
    /**
     * Find all tasks with pagination
     * Example request: GET /api/tasks?page=0&size=10&sort=createdAt,desc
     */
    public Page<TaskResponse> listTasks(Pageable pageable) {
        return taskRepository.findAll(pageable)
            .map(TaskResponse::from);
    }
    
    /**
     * Find tasks by status with pagination
     * Example: Pageable pageable = PageRequest.of(0, 10, Sort.by("dueDate").ascending());
     */
    public Page<TaskResponse> listTasksByStatus(TaskStatus status, Pageable pageable) {
        return taskRepository.findByStatus(status, pageable)
            .map(TaskResponse::from);
    }
    
    /**
     * Custom pagination in controller
     */
    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getTasks(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Task> tasks = taskRepository.findAll(pageable);
        
        return ResponseEntity.ok(tasks.map(TaskResponse::from));
    }
}
\\\

### Sort Specification

\\\java
/**
 * Sort specification for ordering results
 */
// Sort by single field
Sort sort = Sort.by("createdAt").descending();

// Sort by multiple fields
Sort sort = Sort.by()
    .ascending().by("status")
    .descending().by("dueDate");

// Using PageRequest with sort
Pageable pageable = PageRequest.of(0, 10, sort);
Page<Task> tasks = taskRepository.findAll(pageable);

// Dynamic sorting
Sort.Direction direction = Sort.Direction.fromString("DESC");
Pageable pageable = PageRequest.of(0, 10, Sort.by(direction, "createdAt"));
\\\

---

##  Custom Repository Implementation

### For Complex Queries

\\\java
/**
 * Custom repository interface for additional operations
 */
public interface TaskRepositoryCustom {
    
    /**
     * Complex search with multiple criteria
     */
    List<Task> findByComplexCriteria(TaskSearchCriteria criteria);
    
    /**
     * Dynamic query builder
     */
    List<Task> findTasksDynamically(Map<String, Object> filters);
}

/**
 * Implementation using EntityManager and Criteria API
 */
@Repository
public class TaskRepositoryImpl implements TaskRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public List<Task> findByComplexCriteria(TaskSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Task> query = cb.createQuery(Task.class);
        Root<Task> root = query.from(Task.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        // Build predicates based on criteria
        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
        }
        
        if (criteria.getPriority() != null) {
            predicates.add(cb.equal(root.get("priority"), criteria.getPriority()));
        }
        
        if (criteria.getAssigneeId() != null) {
            Join<Task, User> assigneeJoin = root.join("assignee");
            predicates.add(cb.equal(assigneeJoin.get("id"), criteria.getAssigneeId()));
        }
        
        if (criteria.getFromDate() != null && criteria.getToDate() != null) {
            predicates.add(cb.between(
                root.get("dueDate"),
                criteria.getFromDate(),
                criteria.getToDate()
            ));
        }
        
        // Combine predicates with AND
        query.where(cb.and(predicates.toArray(new Predicate[0])));
        
        return entityManager.createQuery(query).getResultList();
    }
    
    @Override
    public List<Task> findTasksDynamically(Map<String, Object> filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Task> query = cb.createQuery(Task.class);
        Root<Task> root = query.from(Task.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        // Dynamically add predicates based on provided filters
        filters.forEach((key, value) -> {
            if (value != null) {
                switch (key) {
                    case "status":
                        predicates.add(cb.equal(root.get("status"), value));
                        break;
                    case "priority":
                        predicates.add(cb.equal(root.get("priority"), value));
                        break;
                    case "titleContains":
                        predicates.add(cb.like(root.get("title"), "%" + value + "%"));
                        break;
                    case "assigneeId":
                        predicates.add(cb.equal(root.get("assignee").get("id"), value));
                        break;
                }
            }
        });
        
        query.where(cb.and(predicates.toArray(new Predicate[0])));
        
        return entityManager.createQuery(query).getResultList();
    }
}

/**
 * Extend both JpaRepository and custom interface
 */
public interface TaskRepository extends JpaRepository<Task, Long>, TaskRepositoryCustom {
    
}
\\\

---

##  Specifications Pattern (JPA Criteria)

\\\java
/**
 * Specification for Task entity
 * Provides reusable predicates for complex queries
 */
public class TaskSpecification {
    
    /**
     * Specification for task status
     */
    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
    
    /**
     * Specification for priority
     */
    public static Specification<Task> hasPriority(TaskPriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }
    
    /**
     * Specification for assignee
     */
    public static Specification<Task> assignedTo(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), userId);
    }
    
    /**
     * Specification for project
     */
    public static Specification<Task> inProject(Long projectId) {
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }
    
    /**
     * Specification for title search
     */
    public static Specification<Task> titleContains(String titlePart) {
        return (root, query, cb) -> cb.like(
            cb.lower(root.get("title")),
            "%" + titlePart.toLowerCase() + "%"
        );
    }
    
    /**
     * Specification for due date range
     */
    public static Specification<Task> dueDateBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> cb.between(root.get("dueDate"), from, to);
    }
    
    /**
     * Specification for overdue tasks
     */
    public static Specification<Task> isOverdue() {
        return (root, query, cb) -> cb.and(
            cb.lessThan(root.get("dueDate"), LocalDateTime.now()),
            cb.notEqual(root.get("status"), TaskStatus.COMPLETED)
        );
    }
}

/**
 * Repository extending JpaSpecificationExecutor
 */
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    
}

/**
 * Usage of specifications
 */
@Service
public class TaskService {
    
    private final TaskRepository taskRepository;
    
    /**
     * Combine multiple specifications with AND
     */
    public List<Task> searchTasks(TaskSearchCriteria criteria) {
        Specification<Task> spec = Specification
            .where(TaskSpecification.inProject(criteria.getProjectId()))
            .and(TaskSpecification.hasStatus(criteria.getStatus()))
            .and(TaskSpecification.hasPriority(criteria.getPriority()));
        
        return taskRepository.findAll(spec);
    }
    
    /**
     * Search with pagination
     */
    public Page<Task> searchTasksWithPagination(TaskSearchCriteria criteria, Pageable pageable) {
        Specification<Task> spec = Specification
            .where(TaskSpecification.inProject(criteria.getProjectId()))
            .and(TaskSpecification.hasStatus(criteria.getStatus()));
        
        return taskRepository.findAll(spec, pageable);
    }
    
    /**
     * Find overdue tasks for a project
     */
    public List<Task> findOverdueTasks(Long projectId) {
        Specification<Task> spec = Specification
            .where(TaskSpecification.inProject(projectId))
            .and(TaskSpecification.isOverdue());
        
        return taskRepository.findAll(spec);
    }
}
\\\

---

##  Best Practices

### 1. Use Derived Queries First

\\\java
// GOOD: Simple derived query
List<Task> findByStatus(TaskStatus status);

// GOOD: Paginated derived query
Page<Task> findByStatus(TaskStatus status, Pageable pageable);

// BAD: Unnecessary @Query for simple case
@Query("SELECT t FROM Task t WHERE t.status = :status")
List<Task> findByStatus(@Param("status") TaskStatus status);
\\\

### 2. Use JOIN FETCH to Prevent N+1 Queries

\\\java
// GOOD: Eager load related data
@Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.assignee LEFT JOIN FETCH t.comments WHERE t.project.id = :projectId")
List<Task> findByProjectId(@Param("projectId") Long projectId);

// BAD: Causes N+1 queries
List<Task> findByProjectId(Long projectId);
// Then accessing t.getAssignee() triggers additional queries
\\\

### 3. Use Projections for Performance

\\\java
// GOOD: Only select needed columns
@Query("SELECT new com.taskmanagement.dto.TaskSummary(t.id, t.title, t.status) FROM Task t WHERE t.project.id = :projectId")
List<TaskSummary> findTaskSummaries(@Param("projectId") Long projectId);

// BAD: Load entire entity when only some fields needed
List<Task> findByProjectId(Long projectId);
\\\

### 4. Always Use @Transactional for Write Operations

\\\java
// GOOD: @Transactional on update/delete operations
@Modifying
@Transactional
@Query("UPDATE Task t SET t.status = :status WHERE t.id = :id")
void updateStatus(@Param("id") Long id, @Param("status") TaskStatus status);

// BAD: Missing @Transactional
@Modifying
@Query("UPDATE Task t SET t.status = :status WHERE t.id = :id")
void updateStatus(@Param("id") Long id, @Param("status") TaskStatus status);
\\\

### 5. Use Named Parameters

\\\java
// GOOD: Named parameters are clearer
@Query("SELECT t FROM Task t WHERE t.status = :status AND t.priority = :priority")
List<Task> findByStatusAndPriority(
    @Param("status") TaskStatus status,
    @Param("priority") TaskPriority priority
);

// BAD: Positional parameters are error-prone
@Query("SELECT t FROM Task t WHERE t.status = ?1 AND t.priority = ?2")
List<Task> findByStatusAndPriority(TaskStatus status, TaskPriority priority);
\\\

### 6. Use Pagination for Large Result Sets

\\\java
// GOOD: Pagination for large datasets
Page<Task> findByStatus(TaskStatus status, Pageable pageable);

// BAD: Loading all records
List<Task> findByStatus(TaskStatus status);
\\\

### 7. Create Indexes for Frequently Queried Fields

\\\java
@Entity
@Table(
    name = "tasks",
    indexes = {
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_assignee_id", columnList = "assignee_id"),
        @Index(name = "idx_project_id", columnList = "project_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
    }
)
public class Task extends BaseEntity {
    // Fields...
}
\\\

### 8. Use Specifications for Complex Filtering

\\\java
// GOOD: Composable specifications
Specification<Task> spec = Specification
    .where(TaskSpecification.hasStatus(criteria.getStatus()))
    .and(TaskSpecification.inProject(criteria.getProjectId()));

Page<Task> result = taskRepository.findAll(spec, pageable);

// BAD: Multiple custom query methods
List<Task> findByStatusAndProjectId(TaskStatus status, Long projectId);
List<Task> findByStatusAndProjectIdAndPriority(TaskStatus status, Long projectId, TaskPriority priority);
// Combinatorial explosion of methods
\\\

---

##  Common Query Patterns

### Search with Filters

\\\java
/**
 * Complex search with multiple optional filters
 */
@Query("SELECT t FROM Task t WHERE "
    + "(:status IS NULL OR t.status = :status) "
    + "AND (:priority IS NULL OR t.priority = :priority) "
    + "AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId) "
    + "AND (:projectId IS NULL OR t.project.id = :projectId) "
    + "AND (:titleContains IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :titleContains, '%')))")
List<Task> searchTasks(
    @Param("status") TaskStatus status,
    @Param("priority") TaskPriority priority,
    @Param("assigneeId") Long assigneeId,
    @Param("projectId") Long projectId,
    @Param("titleContains") String titleContains
);
\\\

### Aggregation Queries

\\\java
/**
 * Count and statistics queries
 */
@Query("SELECT COUNT(t) FROM Task t WHERE t.status = :status AND t.project.id = :projectId")
long countByStatusAndProject(@Param("status") TaskStatus status, @Param("projectId") Long projectId);

@Query("SELECT new map(t.status AS status, COUNT(t) AS count) FROM Task t WHERE t.project.id = :projectId GROUP BY t.status")
List<Map<String, Object>> countTasksByStatus(@Param("projectId") Long projectId);
\\\

### Batch Operations

\\\java
/**
 * Batch insert for performance
 */
@Repository
@Transactional
public class TaskBatchRepository {
    
    private final TaskRepository taskRepository;
    private final EntityManager entityManager;
    
    public void batchInsertTasks(List<Task> tasks, int batchSize) {
        for (int i = 0; i < tasks.size(); i++) {
            taskRepository.save(tasks.get(i));
            
            if ((i + 1) % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }
}
\\\

---

##  Testing Repositories

### Unit Testing Repositories

\\\java
@DataJpaTest  // Only loads JPA components
class TaskRepositoryTest {
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    void findByStatus_WithPendingTasks_ReturnsPendingTasks() {
        // Arrange
        Task task1 = Task.builder()
            .title("Task 1")
            .status(TaskStatus.PENDING)
            .build();
        Task task2 = Task.builder()
            .title("Task 2")
            .status(TaskStatus.COMPLETED)
            .build();
        
        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.flush();
        
        // Act
        List<Task> result = taskRepository.findByStatus(TaskStatus.PENDING);
        
        // Assert
        assertEquals(1, result.size());
        assertEquals("Task 1", result.get(0).getTitle());
    }
    
    @Test
    void findByStatusWithPagination_WithMultipleTasks_ReturnsPage() {
        // Create 25 tasks
        for (int i = 1; i <= 25; i++) {
            Task task = Task.builder()
                .title("Task " + i)
                .status(TaskStatus.PENDING)
                .build();
            entityManager.persist(task);
        }
        entityManager.flush();
        
        // Request page 0 with size 10
        Page<Task> result = taskRepository.findByStatus(
            TaskStatus.PENDING,
            PageRequest.of(0, 10)
        );
        
        assertEquals(10, result.getContent().size());
        assertEquals(3, result.getTotalPages());
        assertEquals(25, result.getTotalElements());
        assertTrue(result.isFirst());
        assertFalse(result.isLast());
    }
}
\\\

---

##  Repository Checklist

When creating repositories:

- [ ] Extend JpaRepository or appropriate interface
- [ ] Use derived query methods for simple cases
- [ ] Use @Query for complex queries
- [ ] Use JOIN FETCH to prevent N+1 queries
- [ ] Include indexes on frequently queried fields
- [ ] Use Specifications for complex filtering
- [ ] Implement pagination for large datasets
- [ ] Use named parameters in queries
- [ ] Add @Transactional to write operations
- [ ] Use projections for performance
- [ ] Document complex query methods
- [ ] Write repository tests
- [ ] Consider custom repository implementations
- [ ] Use batch operations for bulk inserts
- [ ] Profile queries for performance

---

##  Related Documentation

- **ARCHITECTURE.md** - Data access architecture
- **README.md** - Main project overview
- **Entity Layer** - Domain entities and relationships
- **Service Layer** - Business logic using repositories

---

##  Quick Reference

### Derived Query Keywords

\\\
And, Or                   AND, OR
Between                   BETWEEN
GreaterThan, LessThan     >, <
GreaterThanEqual, LessThanEqual  >=, <=
IsNull, IsNotNull         IS NULL, IS NOT NULL
Like                      LIKE
In, NotIn                 IN, NOT IN
Distinct                  DISTINCT
OrderBy                   ORDER BY
Containing, StartsWith, EndsWith  LIKE patterns
IgnoreCase                LOWER()
\\\

### @Query Example Patterns

\\\
SELECT - Query data
UPDATE - Modify data (@Modifying required)
DELETE - Remove data (@Modifying required)
COUNT  - Count results
LEFT JOIN FETCH - Eager load relationships
GROUP BY - Aggregate results
ORDER BY - Sort results
WHERE - Filter results
DISTINCT - Remove duplicates
\\\

### Pageable Usage

\\\
PageRequest.of(pageNumber, pageSize)
PageRequest.of(pageNumber, pageSize, sort)
Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending())
Page<T> - Get page with total count
Slice<T> - Get slice without total count
List<T> - Get all results (avoid for large datasets)
\\\

---

**Last Updated:** December 1, 2025  
**Version:** 1.0.0  
**Status:** Complete
