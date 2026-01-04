# Database Schema & Entity Relationships

> **Purpose:** Comprehensive guide to the database structure, entity relationships, JPA mappings, and data model design decisions. Essential for understanding data flow and database operations.

---

## Table of Contents
1. [Database Overview](#database-overview)
2. [Entity Relationship Diagram](#entity-relationship-diagram)
3. [Core Tables](#core-tables)
4. [Entity Relationships](#entity-relationships)
5. [Soft Delete Strategy](#soft-delete-strategy)
6. [Indexing Strategy](#indexing-strategy)
7. [Known Limitations](#known-limitations)

---

## Database Overview

**Database:** PostgreSQL 15+  
**ORM:** JPA/Hibernate 6.x  
**Connection Pooling:** HikariCP  
**Schema Management:** Hibernate DDL Auto (development), Flyway (production planned)

**Current Configuration:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/task_db
    username: postgres
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update  # Auto-generates tables
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
```

---

## Entity Relationship Diagram

```
┌─────────────┐                    ┌─────────────┐
│   Project   │                    │    User     │
│─────────────│                    │─────────────│
│ id (PK)     │                    │ id (PK)     │
│ name        │                    │ username    │
│ description │                    │ email       │
│ active      │                    │ fullName    │
│ created_at  │                    │ active      │
│ updated_at  │                    │ deleted     │
└─────────────┘                    └─────────────┘
       │ 1                                │ N
       │                                  │
       │                                  │
       │ N                                │
┌─────▼─────────────────────────────────▼─────┐
│                   Task                       │
│──────────────────────────────────────────────│
│ id (PK)                                      │
│ title                                        │
│ description                                  │
│ status (ENUM)                                │
│ priority (ENUM)                              │
│ project_id (FK → projects.id) NOT NULL       │
│ due_date                                     │
│ start_date                                   │
│ completed_at                                 │
│ estimated_hours                              │
│ notes                                        │
│ created_at                                   │
│ updated_at                                   │
└──────────────────────────────────────────────┘
       │ N                    │ 1
       │                      │
       ├──────────────────────┴──────────┐
       │                                  │
┌──────▼───────────┐          ┌──────────▼─────┐
│ task_assignees   │          │   Comment      │
│ (Junction Table) │          │────────────────│
│──────────────────│          │ id (PK)        │
│ task_id (FK)     │          │ task_id (FK)   │
│ user_id (FK)     │          │ user_id (FK)   │
└──────────────────┘          │ content        │
                              │ created_at     │
                              │ updated_at     │
                              └────────────────┘
                                     │ 1
                                     │
                              ┌──────▼─────────┐
                              │  Attachment    │
                              │────────────────│
                              │ id (PK)        │
                              │ task_id (FK)   │
                              │ filename       │
                              │ file_path      │
                              │ file_size      │
                              │ mime_type      │
                              │ uploaded_by FK │
                              │ created_at     │
                              └────────────────┘
```

---

## Core Tables

### tasks

**Purpose:** Core entity representing a unit of work.

**Columns:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PRIMARY KEY | Auto-incrementing ID |
| `title` | VARCHAR(200) | NOT NULL | Task title |
| `description` | TEXT | NULL | Detailed description |
| `status` | VARCHAR(50) | NOT NULL | Task status (enum) |
| `priority` | VARCHAR(50) | NOT NULL | Priority level (enum) |
| `project_id` | BIGINT | NOT NULL, FK → projects(id) | Associated project |
| `due_date` | TIMESTAMP | NULL | Deadline |
| `start_date` | TIMESTAMP | NULL | Start date |
| `completed_at` | TIMESTAMP | NULL | Completion timestamp |
| `estimated_hours` | INTEGER | NULL | Estimated effort |
| `notes` | TEXT | NULL | Additional notes |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Creation timestamp |
| `updated_at` | TIMESTAMP | NOT NULL | Last update timestamp |

**Indexes:**
```sql
CREATE INDEX idx_tasks_project_id ON tasks(project_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_created_at ON tasks(created_at DESC);
```

**Status Values:**
- `PENDING` - Task created, not started
- `IN_PROGRESS` - Work in progress
- `BLOCKED` - Waiting on dependencies
- `IN_REVIEW` - Pending review
- `COMPLETED` - Finished successfully
- `CANCELLED` - Cancelled/abandoned

**Priority Values:**
- `LOW` - Nice to have
- `MEDIUM` - Normal priority
- `HIGH` - Important
- `CRITICAL` - Urgent, blocking

---

### users

**Purpose:** System users with authentication credentials.

**Columns:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PRIMARY KEY | Auto-incrementing ID |
| `username` | VARCHAR(50) | NOT NULL, UNIQUE | Login username |
| `email` | VARCHAR(100) | NOT NULL, UNIQUE | Email address |
| `full_name` | VARCHAR(100) | NOT NULL | Display name |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt hashed password |
| `active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Account active status |
| `deleted` | BOOLEAN | NOT NULL, DEFAULT FALSE | Soft delete flag |
| `last_login_at` | TIMESTAMP | NULL | Last login timestamp |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Registration date |
| `updated_at` | TIMESTAMP | NOT NULL | Last update timestamp |

**Indexes:**
```sql
CREATE UNIQUE INDEX idx_users_username ON users(username);
CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_deleted ON users(deleted);
```

**Soft Delete Filter:**
```java
@Where(clause = "deleted = false")
```

This Hibernate annotation automatically excludes soft-deleted users from all queries. See [Soft Delete Strategy](#soft-delete-strategy) for details.

---

### projects

**Purpose:** Grouping/organization of tasks.

**Columns:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PRIMARY KEY | Auto-incrementing ID |
| `name` | VARCHAR(100) | NOT NULL | Project name |
| `description` | TEXT | NULL | Project description |
| `active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Active/archived status |
| `owner_id` | BIGINT | NULL, FK → users(id) | Project owner |
| `start_date` | DATE | NULL | Project start date |
| `end_date` | DATE | NULL | Project end date |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Creation timestamp |
| `updated_at` | TIMESTAMP | NOT NULL | Last update timestamp |

**Indexes:**
```sql
CREATE INDEX idx_projects_active ON projects(active);
CREATE INDEX idx_projects_owner_id ON projects(owner_id);
```

---

### task_assignees (Junction Table)

**Purpose:** Many-to-Many relationship between tasks and users.

**Columns:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `task_id` | BIGINT | NOT NULL, FK → tasks(id) | Task reference |
| `user_id` | BIGINT | NOT NULL, FK → users(id) | User reference |

**Primary Key:** `(task_id, user_id)`

**Foreign Keys:**
```sql
ALTER TABLE task_assignees 
  ADD CONSTRAINT fk_task_assignees_task 
  FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE;

ALTER TABLE task_assignees 
  ADD CONSTRAINT fk_task_assignees_user 
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
```

**Indexes:**
```sql
CREATE INDEX idx_task_assignees_task_id ON task_assignees(task_id);
CREATE INDEX idx_task_assignees_user_id ON task_assignees(user_id);
```

**Behavior:**
- When a task is deleted, all entries in this table are cascade deleted
- When a user is deleted, all entries in this table are cascade deleted
- This prevents orphaned relationships

---

### comments

**Purpose:** Task discussions and notes. (Defined but not exposed via API yet)

**Columns:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PRIMARY KEY | Auto-incrementing ID |
| `task_id` | BIGINT | NOT NULL, FK → tasks(id) ON DELETE CASCADE | Associated task |
| `user_id` | BIGINT | NOT NULL, FK → users(id) | Comment author |
| `content` | TEXT | NOT NULL | Comment text |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Creation timestamp |
| `updated_at` | TIMESTAMP | NOT NULL | Last update timestamp |

**Cascade Behavior:**
- When a task is deleted, all its comments are cascade deleted
- When a user is deleted, comments remain but `user_id` becomes NULL (future enhancement)

---

### attachments

**Purpose:** File attachments for tasks. (Defined but not exposed via API yet)

**Columns:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PRIMARY KEY | Auto-incrementing ID |
| `task_id` | BIGINT | NOT NULL, FK → tasks(id) ON DELETE CASCADE | Associated task |
| `filename` | VARCHAR(255) | NOT NULL | Original filename |
| `file_path` | VARCHAR(500) | NOT NULL | Storage path/URL |
| `file_size` | BIGINT | NOT NULL | File size in bytes |
| `mime_type` | VARCHAR(100) | NOT NULL | MIME type |
| `uploaded_by` | BIGINT | NOT NULL, FK → users(id) | Uploader |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Upload timestamp |

**Cascade Behavior:**
- When a task is deleted, all its attachments are cascade deleted
- Physical files should be deleted via background job (not implemented yet)

---

## Entity Relationships

### Task ↔ Project (Many-to-One)

**JPA Mapping:**
```java
@Entity
public class Task {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
}
```

**Behavior:**
- Each task belongs to exactly one project
- A project can have many tasks
- `project_id` is NOT NULL (required relationship)
- Lazy loading: Project is loaded only when accessed
- Deleting a project does **not** cascade delete tasks (must be handled manually)

---

### Task ↔ User (Many-to-Many via task_assignees)

**JPA Mapping:**
```java
@Entity
public class Task {
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "task_assignees",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> assignees = new HashSet<>();
}
```

**Behavior:**
- A task can have multiple assignees
- A user can be assigned to multiple tasks
- Junction table `task_assignees` stores the relationships
- Lazy loading: Assignees are loaded only when accessed
- Deleting a task cascade deletes junction table entries
- Deleting a user cascade deletes junction table entries

**⚠️ Known Issue:**

Hibernate `@Where(clause = "deleted = false")` on User entity breaks lazy loading of Many-to-Many relationships. The filter applies AFTER collection loading, resulting in empty collections even with valid data.

**Workaround:** Use native queries to load assignees separately.

See [docs/KNOWN_ISSUES.md](KNOWN_ISSUES.md#hibernate-where-manytomany-issue) for detailed explanation.

---

### Task ↔ Comment (One-to-Many)

**JPA Mapping:**
```java
@Entity
public class Task {
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();
}

@Entity
public class Comment {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;
}
```

**Behavior:**
- A task can have many comments
- Each comment belongs to exactly one task
- `cascade = CascadeType.ALL` - Saving task saves comments
- `orphanRemoval = true` - Removing comment from list deletes it from DB
- Deleting task cascade deletes all its comments

---

### Task ↔ Attachment (One-to-Many)

**JPA Mapping:**
```java
@Entity
public class Task {
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();
}

@Entity
public class Attachment {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;
}
```

**Behavior:**
- A task can have many attachments
- Each attachment belongs to exactly one task
- Deleting task cascade deletes all its attachments
- Physical files should be deleted separately (not implemented yet)

---

## Soft Delete Strategy

### User Soft Delete

**Implementation:**
```java
@Entity
@Where(clause = "deleted = false")
public class User {
    @Column(nullable = false)
    private Boolean deleted = false;
    
    // Soft delete method
    public void markAsDeleted() {
        this.deleted = true;
    }
}
```

**Behavior:**
- Deleting a user sets `deleted=true`
- Hibernate `@Where` filter excludes deleted users from all queries automatically
- User remains in database (recoverable)
- Cascade deletes entries in `task_assignees` junction table
- User can be restored by setting `deleted=false`

**Advantages:**
- ✅ Data preservation for audit/recovery
- ✅ Maintain referential integrity
- ✅ Can restore accidentally deleted users

**Disadvantages:**
- ⚠️ Breaks Many-to-Many lazy loading (Hibernate 6.x bug)
- ⚠️ Requires native queries workaround
- ⚠️ Unique constraints still apply (username, email)

---

### Project Archive (Similar to Soft Delete)

**Implementation:**
```java
@Entity
public class Project {
    @Column(nullable = false)
    private Boolean active = true;
    
    public void archive() {
        this.active = false;
    }
    
    public void reactivate() {
        this.active = true;
    }
}
```

**Behavior:**
- Archiving sets `active=false`
- Archived projects excluded from default queries
- Tasks remain associated (not cascade deleted)
- Can reactivate project to resume work

---

### Task Hard Delete

**Current Behavior:**
- Tasks are **permanently deleted** (hard delete)
- Cascade deletes all comments
- Cascade deletes all attachments
- Removes entries from `task_assignees` junction table

**Future Enhancement (v0.8.0):**
- Implement soft delete for tasks
- Add `deleted` column to tasks table
- Add `@Where` filter similar to users
- Prevent accidental permanent data loss

---

## Indexing Strategy

### Primary Indexes (Auto-created)

- `tasks.id` (PRIMARY KEY)
- `users.id` (PRIMARY KEY)
- `projects.id` (PRIMARY KEY)
- `comments.id` (PRIMARY KEY)
- `attachments.id` (PRIMARY KEY)

### Foreign Key Indexes (Auto-created by JPA)

- `tasks.project_id`
- `task_assignees.task_id`
- `task_assignees.user_id`
- `comments.task_id`
- `comments.user_id`
- `attachments.task_id`
- `attachments.uploaded_by`

### Custom Indexes for Query Performance

**tasks table:**
```sql
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_created_at ON tasks(created_at DESC);
```

**users table:**
```sql
CREATE UNIQUE INDEX idx_users_username ON users(username);
CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_deleted ON users(deleted);
```

**projects table:**
```sql
CREATE INDEX idx_projects_active ON projects(active);
```

### Query Optimization Examples

**Find overdue tasks:**
```sql
SELECT * FROM tasks 
WHERE status != 'COMPLETED' 
  AND due_date < NOW()
ORDER BY due_date ASC;
-- Uses: idx_tasks_due_date, idx_tasks_status
```

**Find user's tasks:**
```sql
SELECT t.* FROM tasks t
INNER JOIN task_assignees ta ON t.id = ta.task_id
WHERE ta.user_id = 5;
-- Uses: idx_task_assignees_user_id
```

**Find active projects:**
```sql
SELECT * FROM projects WHERE active = true;
-- Uses: idx_projects_active
```

---

## Known Limitations

### 1. Assignee Cannot Be Removed

**Issue:** `project_id` is NOT NULL, so tasks must always have a project. Cannot create unassigned tasks or orphan tasks.

**Impact:** 
- Cannot temporarily park tasks without project assignment
- Cannot model personal tasks (no project)

**Future Solution:**
- Make `project_id` nullable
- Add "Personal Tasks" project for orphaned tasks

---

### 2. No Cascade Delete for User FK

**Issue:** No `ON DELETE` action for `tasks.assignee_id` (if it existed as single assignee).

**Impact:**
- Cannot delete user if they have assigned tasks
- Foreign key constraint violation

**Current Workaround:**
- Soft delete users instead
- Junction table has `ON DELETE CASCADE`

---

### 3. Hard Delete for Tasks

**Issue:** Tasks are permanently deleted, no recovery.

**Impact:**
- Accidental deletion loses all data
- No audit trail for deleted tasks

**Future Solution:**
- Implement soft delete for tasks
- Add `deleted` column
- Add `@Where` filter

---

### 4. Hibernate @Where + Many-to-Many Issue

**Issue:** Hibernate 6.x applies `@Where` filter AFTER lazy loading, causing empty collections.

**Impact:**
- `task.getAssignees()` returns empty list even with valid data
- Many-to-Many relationships with filtered entities fail

**Current Workaround:**
- Use native SQL queries
- Manually populate collections
- See [docs/KNOWN_ISSUES.md](KNOWN_ISSUES.md) for details

**Future Solution:**
- Migrate from `@Where` to `@FilterDef` for fine-grained control
- Use `@Filter` with session-level activation

---

### 5. No Unique Constraint on (task_id, user_id)

**Issue:** Theoretically possible to assign same user to task multiple times (though JPA Set prevents this).

**Impact:**
- Database doesn't enforce uniqueness
- Relies on application-level validation

**Solution:**
```sql
ALTER TABLE task_assignees 
ADD CONSTRAINT uk_task_user UNIQUE (task_id, user_id);
```

---

## Related Documentation

- [docs/ARCHITECTURE.md](ARCHITECTURE.md) - Clean Architecture and design patterns
- [docs/API.md](API.md) - REST API endpoints
- [docs/KNOWN_ISSUES.md](KNOWN_ISSUES.md) - Detailed analysis of Hibernate issues
- [docs/FRONTEND_ARCHITECTURE.md](FRONTEND_ARCHITECTURE.md) - React component architecture

---

**Last Updated:** January 4, 2026  
**Version:** v0.7.0  
**Author:** Task Management Team
