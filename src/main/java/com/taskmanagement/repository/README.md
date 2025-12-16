# Repository Layer (Data Access)

## 📋 Overview

**Purpose:** Data access layer using Spring Data JPA for entity persistence and queries.

**Location:** `src/main/java/com/taskmanagement/repository/`

**Pattern:** Repository Pattern with Spring Data JPA interfaces

**Current Status:**  
✅ **MVP Phase** - Basic CRUD repositories implemented  
🔲 **Future** - Custom queries, pagination, specifications

---

## 📁 Current Structure

```
repository/
├── TaskRepository.java          # ✅ Task data access (CRUD)
├── UserRepository.java          # ✅ User data access (validation only)
├── ProjectRepository.java       # ✅ Project data access (validation only)
└── README.md                    # This file
```

**Note:** Only TaskRepository has active CRUD operations. User and Project repositories are currently used only for validation (checking existence).

---

## 🎯 Core Responsibilities

### ✅ Currently Implemented

1. **Entity Persistence**
   - Save new tasks
   - Update existing tasks
   - Delete tasks (hard delete)
   
2. **Basic Query Operations**
   - Find by ID (findById)
   - Check existence (existsById)
   - Standard JpaRepository methods

3. **Validation Support**
   - Verify assignee exists before task creation
   - Verify project exists before task creation

### 🔲 Not Yet Implemented

- Custom query methods (findByAssignee, findByProject, findByStatus)
- Pagination and sorting
- Soft delete filtering (queries ignore deleted flag)
- Full-text search
- Complex criteria queries
- Batch operations
- Query result caching

---

## 1. TaskRepository

**Location:** [TaskRepository.java](TaskRepository.java)

**Purpose:** Primary repository for Task CRUD operations

### Interface Definition

```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    /**
     * Find task by ID including soft-deleted tasks
     * (for admin/restore operations)
     */
    @Query("SELECT t FROM Task t WHERE t.id = :id")
    Optional<Task> findByIdIncludingDeleted(Long id);
    
    /**
     * Find tasks by assignee ID
     */
    List<Task> findByAssigneeId(Long assigneeId);
    
    /**
     * Find tasks without assignee
     */
    List<Task> findByAssigneeIsNull();
    
    /**
     * Count tasks by assignee
     */
    long countByAssigneeId(Long assigneeId);
    
    /**
     * Find tasks by status
     */
    List<Task> findByStatus(TaskStatus status);
    
    /**
     * Bulk unassign tasks - PERFORMANCE CRITICAL
     * Sets assignee = NULL and status = UNASSIGNED
     * Used when deleting user
     */
    @Modifying
    @Query("UPDATE Task t " +
           "SET t.assignee = NULL, " +
           "    t.status = com.taskmanagement.entity.TaskStatus.UNASSIGNED " +
           "WHERE t.assignee.id = :userId")
    int unassignTasksByUserId(@Param("userId") Long userId);
}
```

**New Methods Explained:**

1. **findByAssigneeId(Long assigneeId)**
   - Find all tasks assigned to a user
   - Used for: User dashboard, task listing

2. **findByAssigneeIsNull()**
   - Find unassigned tasks (UNASSIGNED status)
   - Used for: Admin view, task assignment UI

3. **countByAssigneeId(Long assigneeId)**
   - Count user's tasks
   - Used for: Pre-delete validation, statistics

4. **findByStatus(TaskStatus status)**
   - Find tasks by status (PENDING, IN_PROGRESS, UNASSIGNED, etc.)
   - Used for: Filtering, reporting

5. **unassignTasksByUserId() ⭐ CRITICAL**
   - **Bulk update:** Unassign ALL tasks in 1 query
   - Set assignee = NULL, status = UNASSIGNED
   - Returns: number of updated tasks
   - **Performance:** O(1) instead of O(N)
   - **No entity loading:** Bypasses Hibernate cache
   - **No validation triggers:** Avoids @FutureOrPresent issues

### Inherited Methods (from JpaRepository)

Spring Data JPA provides these methods automatically:

```java
// CREATE
Task save(Task task);                     // Insert or update
List<Task> saveAll(Iterable<Task> tasks); // Batch save

// READ
Optional<Task> findById(Long id);         // Find by primary key
List<Task> findAll();                     // Find all tasks
boolean existsById(Long id);              // Check existence
long count();                             // Count total tasks

// UPDATE
// (Use save() method - if ID exists, it updates)

// DELETE
void delete(Task task);                   // Delete entity
void deleteById(Long id);                 // Delete by ID
void deleteAll();                         // Delete all tasks
```

### Usage Examples

**1. Create Task**
```java
Task task = Task.builder()
    .title("New Task")
    .assignee(user)
    .project(project)
    .build();

Task saved = taskRepository.save(task);  // Returns task with ID
```

**2. Find Task**
```java
Optional<Task> taskOpt = taskRepository.findById(123L);
Task task = taskOpt.orElseThrow(() -> new TaskNotFoundException(123L));
```

**3. Update Task**
```java
Task task = taskRepository.findById(123L).orElseThrow();
task.setTitle("Updated Title");
taskRepository.save(task);  // JPA detects ID exists → UPDATE
```

**4. Delete Task**
```java
Task task = taskRepository.findById(123L).orElseThrow();
taskRepository.delete(task);  // Hard delete (cascade to comments/attachments)
```

### Current Limitations

✅ **Implemented Features:**
- ✅ Custom query methods (findByAssigneeId, findByStatus, findByAssigneeIsNull)
- ✅ Bulk operations (unassignTasksByUserId)
- ✅ Count queries (countByAssigneeId)

❌ **Missing Features:**
- No pagination support
- Soft delete defined but not filtered in queries (Task doesn't use @Where)
- No full-text search
- No complex criteria queries

🔲 **Planned Custom Methods:**
```java
// Will be added in future:
Page<Task> findByAssigneeId(Long assigneeId, Pageable pageable);
List<Task> findByProjectId(Long projectId);
List<Task> findByStatus(TaskStatus status);
Page<Task> findAll(Pageable pageable);
List<Task> findOverdueTasks();
```

---

## 2. UserRepository

**Location:** [UserRepository.java](UserRepository.java)

**Purpose:** User data access with soft delete support

### Interface Definition

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by ID including soft-deleted users
     * Bypasses @Where(clause = "deleted = false") filter
     */
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdIncludingDeleted(@Param("id") Long id);
    
    /**
     * Find all deleted users
     */
    @Query("SELECT u FROM User u WHERE u.deleted = true")
    List<User> findAllDeleted();
    
    /**
     * Count projects owned by user
     */
    @Query("SELECT COUNT(p) FROM Project p WHERE p.owner.id = :userId")
    long countOwnedProjects(@Param("userId") Long userId);
    
    /**
     * Hard delete user from database
     * ⚠️ DANGEROUS - Bypasses @SQLDelete annotation
     * Use only for cleanup jobs
     */
    @Modifying
    @Query(value = "DELETE FROM users WHERE id = :#{#user.id}", nativeQuery = true)
    void hardDelete(@Param("user") User user);
}
```

**Methods Explained:**

1. **findByIdIncludingDeleted(Long id)**
   - Bypass @Where clause
   - Find user even if deleted = true
   - Used for: DELETE and RESTORE operations
   - Why needed: Standard findById() filters deleted users

2. **findAllDeleted()**
   - Get all soft-deleted users
   - Used for: Admin view, cleanup jobs, reporting

3. **countOwnedProjects(Long userId)**
   - Count projects owned by user
   - Used for: Pre-delete logging, statistics
   - Business rule: Projects preserved when user deleted

4. **hardDelete(User user) ⚠️ DANGEROUS**
   - Physical DELETE from database
   - Bypasses @SQLDelete annotation
   - **WARNING:** Breaks data integrity
   - Use case: ONLY for cleanup jobs

### Current Usage

**UserService Operations:**
```java
// Get active user
User user = userRepository.findById(id)
    .orElseThrow(() -> new UserNotFoundException(id));

// Get user for delete/restore (including deleted)
User user = userRepository.findByIdIncludingDeleted(id)
    .orElseThrow(() -> new UserNotFoundException(id));

// Count resources before delete
long projectCount = userRepository.countOwnedProjects(userId);
```

**@Where Clause Behavior:**
```java
// User entity has: @Where(clause = "deleted = false")

// This ONLY returns active users:
userRepository.findById(1L);       // Returns empty if deleted
userRepository.findAll();          // Excludes deleted users

// This returns ALL users (bypasses @Where):
userRepository.findByIdIncludingDeleted(1L);  // Returns even if deleted
```

### Current Limitations

✅ **Implemented Features:**
- ✅ User CRUD operations (GET, DELETE, RESTORE)
- ✅ Soft delete support with @Where filtering
- ✅ Resource counting queries

❌ **Missing Features:**
- No User CREATE operation (POST /api/users)
- No User UPDATE operation (PUT /api/users/{id})
- No username/email lookup
- No search/filter methods

🔲 **Planned Features:**
```java
// Will be added in future:
Optional<User> findByUsername(String username);
Optional<User> findByEmail(String email);
List<User> findByFullNameContaining(String name);
Page<User> findAll(Pageable pageable);
```

---

## 3. ProjectRepository

**Location:** [ProjectRepository.java](ProjectRepository.java)

**Purpose:** Validation only - verifies projects exist before task creation

### Interface Definition

```java
@Repository  
public interface ProjectRepository extends JpaRepository<Project, Long> {
    // No custom methods yet
    // Uses inherited methods only
}
```

### Current Usage

**Validation Before Task Creation:**
```java
// In TaskService.createTask()
Project project = projectRepository.findById(request.getProjectId())
    .orElseThrow(() -> new ProjectNotFoundException(request.getProjectId()));
```

**Methods Used:**
- `findById(Long id)` - Verify project exists
- `existsById(Long id)` - Quick existence check

### Current Limitations

❌ **No Project Management:**
- No Project CRUD API
- No ProjectController exists
- Projects must exist in database for testing

🔲 **Planned Features:**
```java
// Will be added when Project management is implemented:
Optional<Project> findByName(String name);
List<Project> findByOwnerId(Long ownerId);
List<Project> findActiveProjects();
```

---

## 4. CommentRepository ✅ NEW

**Location:** [CommentRepository.java](CommentRepository.java)

**Purpose:** Data access for Comment entity

### Interface Definition

```java
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    /**
     * Count comments by author
     */
    long countByAuthorId(Long authorId);
    
    /**
     * Find comments by author
     */
    List<Comment> findByAuthorId(Long authorId);
    
    /**
     * Find comments by task
     */
    List<Comment> findByTaskId(Long taskId);
    
    /**
     * Count comments by task
     */
    long countByTaskId(Long taskId);
}
```

**Methods Explained:**

1. **countByAuthorId(Long authorId)**
   - Count comments written by user
   - Used for: UserService.deleteUser() logging
   - Business rule: Comments preserved when user deleted (audit trail)

2. **findByAuthorId(Long authorId)**
   - Get all comments by author
   - Used for: User activity view

3. **findByTaskId(Long taskId)**
   - Get all comments for a task
   - Used for: Task detail view

4. **countByTaskId(Long taskId)**
   - Count comments on a task
   - Used for: Task statistics

### Current Usage

**UserService - Count Comments:**
```java
// Before deleting user, count their comments
long commentCount = commentRepository.countByAuthorId(userId);
log.info("User {} has {} comments", userId, commentCount);

// Comments are PRESERVED (not deleted/anonymized)
// Reason: Audit trail, historical record
```

**Business Rule - Comments Preserved:**
- When user deleted → comments kept
- author_id remains (NOT NULL constraint)
- Allows historical tracking
- Audit trail compliance

### Current Limitations

✅ **Implemented Features:**
- ✅ Basic query methods (count, find by author/task)

❌ **Missing Features:**
- No Comment CRUD API
- No CommentController exists
- No CommentService exists
- Cannot create/update/delete comments via API
- No pagination support

🔲 **Planned Features:**
```java
// Will be added when Comment management is implemented:
Page<Comment> findByTaskId(Long taskId, Pageable pageable);
Page<Comment> findByAuthorId(Long authorId, Pageable pageable);
List<Comment> findRecentComments(LocalDateTime since);
```

---

## ⚠️ Known Issues & Limitations

### 1. Soft Delete Not Enforced

**Problem:**
- Task entity has `deleted` and `deletedAt` fields
- Standard queries don't filter deleted tasks

**Current Behavior:**
```java
List<Task> tasks = taskRepository.findAll();  // ❌ Returns deleted tasks too
```

**Impact:**
- Deleted tasks appear in results
- Need manual filtering in service layer
- Or use custom `findByIdIncludingDeleted()` method

**Solution (Future):**
```java
// Add to all query methods:
@Query("SELECT t FROM Task t WHERE t.deleted = false")
List<Task> findAllActive();

// Or use @Where annotation on entity:
@Where(clause = "deleted = false")
@Entity
public class Task { ... }
```

### 2. No Custom Query Methods

**Missing Features:**
```java
// These don't exist yet:
List<Task> findByAssigneeId(Long assigneeId);
List<Task> findByProjectId(Long projectId);
List<Task> findByStatus(TaskStatus status);
Page<Task> findAll(Pageable pageable);
```

**Workaround:**
- Use TaskService to filter in memory (not efficient)
- Or add custom queries when needed

### 3. No Pagination Support

**Current:**
```java
List<Task> findAll();  // Returns ALL tasks (performance issue for large datasets)
```

**Needed:**
```java
Page<Task> findAll(Pageable pageable);
Page<Task> findByProjectId(Long projectId, Pageable pageable);
```

---

## 📚 Spring Data JPA Reference

### Standard JpaRepository Methods

Every repository extending `JpaRepository<Entity, ID>` inherits:

```java
// CREATE/UPDATE
<S extends T> S save(S entity);
<S extends T> List<S> saveAll(Iterable<S> entities);

// READ
Optional<T> findById(ID id);
boolean existsById(ID id);
List<T> findAll();
List<T> findAllById(Iterable<ID> ids);
long count();

// DELETE
void delete(T entity);
void deleteById(ID id);
void deleteAll();
void deleteAll(Iterable<? extends T> entities);
void deleteAllById(Iterable<? extends ID> ids);

// FLUSH
void flush();
<S extends T> S saveAndFlush(S entity);
void deleteAllInBatch();
```

### How save() Works

```java
Task task = new Task();
task.setTitle("New Task");
taskRepository.save(task);  // ✅ INSERT (id is null)

Task existing = taskRepository.findById(1L).get();
existing.setTitle("Updated");
taskRepository.save(existing);  // ✅ UPDATE (id exists)
```

### Transaction Management

```java
@Transactional  // Required for write operations
public void updateTask() {
    Task task = taskRepository.findById(1L).get();
    task.setTitle("New Title");
    // Auto-saved at transaction commit (no need to call save())
}

@Transactional(readOnly = true)  // Optimized for reads
public Task getTask(Long id) {
    return taskRepository.findById(id).orElseThrow();
}
```

---

## 🔮 Planned Enhancements

### Phase 1: Custom Query Methods
```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssigneeId(Long assigneeId);
    List<Task> findByProjectId(Long projectId);
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);
}
```

### Phase 2: Pagination & Sorting
```java
Page<Task> findAll(Pageable pageable);
Page<Task> findByProjectId(Long projectId, Pageable pageable);
Page<Task> findByAssigneeId(Long assigneeId, Pageable pageable);

// Usage:
Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
Page<Task> page = taskRepository.findAll(pageable);
```

### Phase 3: Complex Queries
```java
@Query("SELECT t FROM Task t WHERE t.dueDate < CURRENT_TIMESTAMP AND t.status != 'COMPLETED'")
List<Task> findOverdueTasks();

@Query("SELECT t FROM Task t WHERE t.deleted = false AND " +
       "(LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
       "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
List<Task> searchTasks(@Param("keyword") String keyword);
```

### Phase 4: Specifications (Dynamic Queries)
```java
// For complex filtering scenarios
public interface TaskRepository extends JpaRepository<Task, Long>, 
                                       JpaSpecificationExecutor<Task> {
}

// Usage:
Specification<Task> spec = TaskSpecifications.withAssignee(userId)
    .and(TaskSpecifications.withStatus(TaskStatus.IN_PROGRESS))
    .and(TaskSpecifications.dueBefore(LocalDateTime.now()));
    
Page<Task> results = taskRepository.findAll(spec, pageable);
```

---

## 📖 Related Documentation

- [Task Entity](../entity/README.md) - Domain model definition
- [TaskService](../service/README.md) - Business logic layer
- [TaskController](../api/README.md) - REST API endpoints

---

**Last Updated:** December 14, 2025  
**Version:** 0.5.0 - MVP Phase  
**Status:** Basic repositories complete, custom queries pending
