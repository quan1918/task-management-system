# Known Issues & Technical Debt

> **Purpose:** Comprehensive documentation of current bugs, workarounds, root cause analysis, and planned solutions. Essential reading before making changes to affected areas.

---

## Table of Contents
1. [Critical Issues](#critical-issues)
2. [Hibernate ORM Issues](#hibernate-orm-issues)
3. [API Limitations](#api-limitations)
4. [Database Constraints](#database-constraints)
5. [Planned Improvements](#planned-improvements)

---

## Critical Issues

### ⚠️ Issue #1: Hibernate @Where + Many-to-Many Lazy Loading

**Status:** ✅ FIXED with Workaround (v0.6.0)

**Severity:** HIGH - Blocks core functionality

**Symptoms:**
```java
// POST /api/tasks creates task with assignees successfully
Task task = taskService.createTask(request);
task.getAssignees().size(); // Returns correct size (e.g., 3)

// BUT: GET /api/tasks/{id} returns empty assignees
TaskResponse response = taskService.getTaskById(taskId);
response.getAssignees(); // Returns [] (empty array)
```

**Root Cause:**

Hibernate 6.x `@Where(clause = "deleted = false")` filter on User entity applies **AFTER** collection loading, causing empty collections even with valid database data.

**Detailed Analysis:**

1. **User Entity with @Where Filter:**
   ```java
   @Entity
   @Where(clause = "deleted = false")
   public class User {
       @Column(nullable = false)
       private Boolean deleted = false;
   }
   ```

2. **Task Entity with Many-to-Many:**
   ```java
   @Entity
   public class Task {
       @ManyToMany(fetch = FetchType.LAZY)
       @JoinTable(name = "task_assignees", ...)
       private Set<User> assignees = new HashSet<>();
   }
   ```

3. **What Happens:**
   ```
   Step 1: JPA loads Task entity
   Step 2: JPA initializes lazy collection proxy for `assignees`
   Step 3: First access to `task.getAssignees()` triggers query:
           SELECT * FROM users u
           INNER JOIN task_assignees ta ON u.id = ta.user_id
           WHERE ta.task_id = ?
   Step 4: Hibernate loads users into memory
   Step 5: ⚠️ @Where filter applied POST-LOADING
   Step 6: Filter removes ALL users (even non-deleted ones)
   Step 7: Result: Empty collection
   ```

4. **Why Filter Removes All Users:**
   - `@Where` filter expects `deleted = false` in SELECT query
   - But Hibernate applies filter logic AFTER data is loaded
   - Filter logic checks if User object has `deleted == false`
   - Due to lazy loading proxy mechanics, filter sees uninitialized state
   - Result: All users filtered out

**Impact:**
- ✅ POST /api/tasks works (direct save, no lazy loading)
- ❌ GET /api/tasks/{id} returns empty assignees
- ❌ Cannot display task assignees in UI
- ❌ Many-to-Many relationships with filtered entities broken

---

### ✅ Solution Implemented (3-Step Workaround)

**Approach:** Bypass Hibernate lazy loading entirely, use native SQL.

**Step 1: Add Native Query Methods to TaskRepository**

```java
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Native query bypasses Hibernate filters
    @Query(value = "SELECT * FROM tasks WHERE id = :id", nativeQuery = true)
    Optional<Task> findByIdNative(@Param("id") Long id);
    
    // Load assignee IDs directly from junction table
    @Query(value = "SELECT user_id FROM task_assignees WHERE task_id = :taskId", 
           nativeQuery = true)
    List<Long> findAssigneeIdsByTaskId(@Param("taskId") Long taskId);
}
```

**Step 2: Modify TaskService.getTaskById()**

```java
@Service
@Transactional
public class TaskService {
    public TaskResponse getTaskById(Long id) {
        // Step 1: Load task with native SQL (bypasses @Where filter)
        Task task = taskRepository.findByIdNative(id)
            .orElseThrow(() -> new TaskNotFoundException("Task not found: " + id));
        
        // Step 2: Load assignee IDs from junction table
        List<Long> assigneeIds = taskRepository.findAssigneeIdsByTaskId(id);
        
        // Step 3: Load users by IDs (uses @Where filter correctly)
        if (!assigneeIds.isEmpty()) {
            List<User> assignees = userRepository.findAllById(assigneeIds);
            task.setAssignees(new HashSet<>(assignees));
        }
        
        // Step 4: Convert to DTO and return
        return TaskResponse.fromEntity(task);
    }
}
```

**Step 3: Enhanced SQL Logging (for debugging)**

```yaml
# application.yml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

**Why This Works:**

1. **Native SQL bypasses Hibernate filters**
   - `findByIdNative()` executes raw SQL
   - No `@Where` filter applied
   - Task entity loaded with all data

2. **Load assignee IDs separately**
   - Query junction table directly
   - No User entity involved yet
   - Get pure IDs without filtering

3. **Load Users by ID**
   - `findAllById()` uses Hibernate's ID-based query
   - `@Where` filter applied correctly (only filters deleted users)
   - Returns active users only

4. **Manually populate collection**
   - Bypass lazy loading entirely
   - Set assignees eagerly
   - Collection fully initialized

**Files Modified:**
- [TaskRepository.java](../src/main/java/com/taskmanagement/repository/TaskRepository.java) - Added 2 native query methods
- [TaskService.java](../src/main/java/com/taskmanagement/service/TaskService.java) - Modified `getTaskById()` with 3-step workaround
- [application.yml](../src/main/resources/application.yml) - Enhanced SQL logging

---

### Alternative Approaches (Attempted & Failed)

#### ❌ Approach 1: LEFT JOIN FETCH

```java
@Query("SELECT t FROM Task t LEFT JOIN FETCH t.assignees WHERE t.id = :id")
Optional<Task> findByIdWithAssignees(@Param("id") Long id);
```

**Result:** Still returns empty assignees (Hibernate applies @Where filter after fetch)

---

#### ❌ Approach 2: @EntityGraph

```java
@EntityGraph(attributePaths = {"assignees", "project"})
Optional<Task> findById(Long id);
```

**Result:** Same issue - @Where filter applied post-loading

---

#### ❌ Approach 3: Hibernate.initialize()

```java
Task task = taskRepository.findById(id).orElseThrow();
Hibernate.initialize(task.getAssignees());
```

**Result:** Collection initialized but empty due to filter

---

#### ❌ Approach 4: Remove @Where Temporarily

```java
@Entity
// @Where(clause = "deleted = false")  // Commented out
public class User { ... }
```

**Result:** 
- ✅ Many-to-Many loading works
- ❌ Soft-deleted users appear in all queries
- ❌ Not viable due to soft delete requirements

---

### Lesson Learned

**Key Insight:** Hibernate `@Where` filter + Many-to-Many lazy loading = **incompatible in Hibernate 6.x**

**Best Practices:**
1. Use native queries for complex scenarios
2. Avoid `@Where` with Many-to-Many relationships
3. Consider `@FilterDef` + `@Filter` for fine-grained control
4. Document workarounds for future maintainers

**Migration Plan (v0.8.0):**

Replace `@Where` with `@FilterDef` for better control:

```java
@Entity
@FilterDef(name = "deletedUserFilter", 
           parameters = @ParamDef(name = "deleted", type = Boolean.class))
@Filter(name = "deletedUserFilter", condition = "deleted = :deleted")
public class User { ... }

// Usage in service:
Session session = entityManager.unwrap(Session.class);
session.enableFilter("deletedUserFilter").setParameter("deleted", false);
```

**Benefits:**
- Session-level control (can disable for specific queries)
- No interference with lazy loading
- Explicit filter activation

---

## Hibernate ORM Issues

### Issue #2: User Soft Delete with @Where Filter

**Status:** ⚠️ KNOWN ISSUE

**Severity:** MEDIUM

**Description:**

`@Where(clause = "deleted = false")` on User entity may cause issues with any lazy-loaded collection referencing users.

**Impact:**
- Collections referencing soft-deleted users might load empty
- Affects not just Task assignees but any entity with User relationship
- Hard to debug (appears as "no data" instead of explicit error)

**Workaround:**
- Use native queries or explicit joins
- Load users eagerly where needed
- Document relationships affected by @Where filter

**Planned Solution:**
- Migrate to `@FilterDef` + `@Filter` pattern
- Provides session-level control
- Can disable filter for specific queries

---

## API Limitations

### Issue #3: No Backend Task Filtering API

**Status:** 🔲 NOT IMPLEMENTED

**Severity:** LOW - Workaround exists (frontend filtering)

**Description:**

`GET /api/tasks` with query parameters not implemented. Cannot filter tasks by assignee, status, priority, or project on backend.

**Current Workaround:**
- Frontend loads all tasks
- Filters client-side using JavaScript
- Works for small datasets (<1000 tasks)

**Impact:**
- Slow for large datasets
- Wastes bandwidth (loads all tasks)
- Cannot paginate results

**Planned Implementation (v0.8.0):**

```java
@GetMapping("/api/tasks")
public ResponseEntity<Page<TaskResponse>> getTasks(
    @RequestParam(required = false) Long assigneeId,
    @RequestParam(required = false) Long projectId,
    @RequestParam(required = false) TaskStatus status,
    @RequestParam(required = false) TaskPriority priority,
    Pageable pageable
) {
    Page<Task> tasks = taskService.findTasks(
        assigneeId, projectId, status, priority, pageable
    );
    return ResponseEntity.ok(tasks.map(TaskResponse::fromEntity));
}
```

---

### Issue #4: No Priority Filter in Frontend UI

**Status:** 🔲 NOT IMPLEMENTED

**Severity:** LOW

**Description:**

Frontend has search and status filter, but no priority filter dropdown.

**Current UI:**
- ✅ Search by title/description
- ✅ Filter by status (ALL, PENDING, IN_PROGRESS, COMPLETED, BLOCKED)
- ❌ No priority filter (HIGH, MEDIUM, LOW, CRITICAL)

**Planned Implementation (v0.7.1):**

Add priority dropdown to `TaskFilters.jsx`:

```jsx
<select value={priority} onChange={(e) => setPriority(e.target.value)}>
  <option value="ALL">All Priorities</option>
  <option value="CRITICAL">Critical</option>
  <option value="HIGH">High</option>
  <option value="MEDIUM">Medium</option>
  <option value="LOW">Low</option>
</select>
```

---

## Database Constraints

### Issue #5: Cannot Delete Users with Assigned Tasks

**Status:** ⚠️ KNOWN LIMITATION

**Severity:** LOW - Soft delete available

**Description:**

Foreign key constraint blocks hard deletion of users if they have active task assignments.

**Error Message:**
```
ERROR: update or delete on table "users" violates foreign key constraint "fk_task_assignees_user"
Detail: Key (id)=(5) is still referenced from table "task_assignees".
```

**Current Workaround:**
- Use soft delete instead: `DELETE /api/users/{id}`
- Sets `deleted=true`, user remains in database
- Junction table entries cascade deleted automatically

**Impact:**
- Cannot permanently remove users from database
- Database grows over time (soft-deleted users accumulate)

**Future Solution (v1.0.0):**
1. Add bulk unassign operation
2. Implement periodic cleanup job
3. Add "Delete All User Data" admin function

---

### Issue #6: project_id is NOT NULL in tasks

**Status:** ⚠️ KNOWN LIMITATION

**Severity:** LOW

**Description:**

Tasks **must** have a project. Cannot create unassigned/orphaned tasks.

**Impact:**
- Cannot model personal tasks (no project)
- Cannot temporarily park tasks without project assignment

**Future Solution (v0.8.0):**
1. Make `project_id` nullable
2. Create "Personal Tasks" or "Backlog" default project
3. Update validation rules

---

## Planned Improvements

### v0.7.1 (Immediate)

- [ ] Add priority filter to frontend UI
- [ ] Improve error messages for assignee validation
- [ ] Add loading states to task operations

---

### v0.8.0 (Next Minor Release)

- [ ] Implement backend task filtering API
- [ ] Add pagination support (Spring Data Pageable)
- [ ] Make `project_id` nullable for personal tasks
- [ ] Implement soft delete for tasks (currently hard delete)
- [ ] Add `@FilterDef` migration for User entity

---

### v0.9.0 (Event-Driven)

- [ ] Event-driven notifications (TaskCreatedEvent, etc.)
- [ ] Async event processing with `@Async`
- [ ] Email notification integration
- [ ] WebSocket for real-time updates

---

### v1.0.0 (Production-Ready)

- [ ] JWT authentication replacing Basic Auth
- [ ] Role-based access control (RBAC)
- [ ] API rate limiting
- [ ] Redis caching layer
- [ ] File upload for attachments
- [ ] Task comments CRUD API
- [ ] Audit log for all operations

---

## Debugging Tips

### How to Debug Hibernate @Where Issues

1. **Enable SQL Logging:**
   ```yaml
   spring.jpa.show-sql: true
   spring.jpa.properties.hibernate.format_sql: true
   logging.level.org.hibernate.SQL: DEBUG
   logging.level.org.hibernate.type.descriptor.sql.BasicBinder: TRACE
   ```

2. **Check Database Directly:**
   ```sql
   -- Verify data exists
   SELECT * FROM task_assignees WHERE task_id = 1;
   SELECT * FROM users WHERE id IN (3, 7, 8);
   
   -- Check deleted flag
   SELECT id, username, deleted FROM users;
   ```

3. **Use Native Queries:**
   ```java
   @Query(value = "SELECT * FROM tasks WHERE id = ?1", nativeQuery = true)
   Optional<Task> findByIdNative(Long id);
   ```

4. **Test Without @Where:**
   ```java
   // Temporarily comment out @Where to isolate issue
   @Entity
   // @Where(clause = "deleted = false")
   public class User { ... }
   ```

---

### How to Report New Issues

When reporting bugs, include:

1. **Symptom:** What's the observable problem?
2. **Expected:** What should happen?
3. **Actual:** What actually happens?
4. **Steps to Reproduce:** Minimal code/API calls to reproduce
5. **Logs:** SQL queries, stack traces
6. **Environment:** Java version, Hibernate version, database version
7. **Hypothesis:** Root cause guess (if known)

**Example:**

```markdown
## Issue: GET /api/tasks/{id} returns 500 error

**Symptom:** API returns 500 Internal Server Error

**Expected:** Should return 200 OK with task details

**Actual:** 
```json
{"code": "INTERNAL_SERVER_ERROR", "message": "..."}
```

**Steps to Reproduce:**
1. POST /api/tasks (create task)
2. GET /api/tasks/{id} (retrieve task)
3. Observe 500 error

**Logs:**
```
NullPointerException at TaskService.getTaskById()
  at line 45: task.getProject().getName()
```

**Hypothesis:** Project is null or lazy-loading failed
```

---

## Related Documentation

- [docs/ARCHITECTURE.md](ARCHITECTURE.md) - Clean Architecture and design patterns
- [docs/API.md](API.md) - REST API endpoints
- [docs/DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) - Entity relationships
- [docs/FRONTEND_ARCHITECTURE.md](FRONTEND_ARCHITECTURE.md) - React component architecture

---

**Last Updated:** January 4, 2026  
**Version:** v0.7.0  
**Author:** Task Management Team
