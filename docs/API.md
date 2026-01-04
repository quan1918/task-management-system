# Task Management System - API Documentation

> **Purpose:** Complete REST API reference with all endpoints, request/response examples, validation rules, and authentication requirements. Use this as the definitive guide for API integration and testing.

---

## Table of Contents
1. [API Overview](#api-overview)
2. [Authentication](#authentication)
3. [Task Management APIs](#task-management-apis)
4. [User Management APIs](#user-management-apis)
5. [Project Management APIs](#project-management-apis)
6. [Health & Monitoring](#health--monitoring)
7. [Error Handling](#error-handling)
8. [Testing with cURL](#testing-with-curl)
9. [Testing with Postman](#testing-with-postman)

---

## API Overview

**Base URLs:**
- **Local Development:** `http://localhost:8080`
- **Production:** `https://task-management-system-0c0p.onrender.com`

**Content Type:** `application/json`

**Authentication:** Basic Authentication (except `/actuator/health`)

**API Version:** v1 (implicit, no version prefix)

---

## Authentication

### Current Authentication (v0.7.0)

**Type:** HTTP Basic Authentication

**Credentials:**
- **Admin:** `admin:admin`
- **User:** `user:user123`

**Usage:**
```bash
# cURL
curl -u admin:admin http://localhost:8080/api/tasks

# Base64-encoded header
Authorization: Basic YWRtaW46YWRtaW4=
```

### Future Authentication (v1.0.0)

**Type:** JWT Bearer Token

**Planned Flow:**
1. `POST /api/auth/login` - Get JWT token
2. Include token in all requests: `Authorization: Bearer <token>`
3. Token expires after 1 hour
4. Refresh token: `POST /api/auth/refresh`

See [docs/ARCHITECTURE.md](ARCHITECTURE.md#security-basic-auth--jwt) for migration plan.

---

## Task Management APIs

### Create Task

**Endpoint:** `POST /api/tasks`

**Authentication:** Required

**Request Body:**
```json
{
  "title": "Fix login bug",
  "description": "Users cannot login with special characters in password",
  "priority": "HIGH",
  "dueDate": "2025-12-31T17:00:00",
  "estimatedHours": 8,
  "assigneeIds": [3, 7, 8],
  "projectId": 1
}
```

**Validation Rules:**
- `title`: Required, max 200 characters
- `description`: Optional, max 2000 characters
- `priority`: Required, must be `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL`
- `dueDate`: Optional, ISO 8601 format
- `estimatedHours`: Optional, positive integer
- `assigneeIds`: Required, at least one assignee, all must exist and be active
- `projectId`: Required, must exist and be active

**Response (201 Created):**
```json
{
  "id": 7,
  "title": "Fix login bug",
  "description": "Users cannot login with special characters in password",
  "status": "PENDING",
  "priority": "HIGH",
  "dueDate": "2025-12-31T17:00:00",
  "estimatedHours": 8,
  "assignees": [
    {"id": 3, "username": "alice", "email": "alice@example.com", "fullName": "Alice Johnson"},
    {"id": 7, "username": "admin", "email": "admin@example.com", "fullName": "Admin User"},
    {"id": 8, "username": "anna", "email": "anna@example.com", "fullName": "Anna Smith"}
  ],
  "project": {
    "id": 1,
    "name": "Website Redesign",
    "description": "Redesign company website",
    "active": true
  },
  "createdAt": "2026-01-04T12:30:45",
  "updatedAt": "2026-01-04T12:30:45"
}
```

**Error Responses:**

| Status Code | Error Code | Description |
|-------------|-----------|-------------|
| 400 | `VALIDATION_ERROR` | Invalid request body (missing required fields, invalid format) |
| 404 | `ENTITY_NOT_FOUND` | One or more assignees not found or project not found |
| 404 | `BUSINESS_RULE_VIOLATION` | Assignee is deleted/inactive or project is archived |
| 401 | `UNAUTHORIZED` | Missing or invalid authentication credentials |

**Example Error:**
```json
{
  "code": "ENTITY_NOT_FOUND",
  "message": "User with id=10 not found",
  "timestamp": 1704369045000,
  "path": "/api/tasks"
}
```

---

### Get Task by ID

**Endpoint:** `GET /api/tasks/{id}`

**Authentication:** Required

**Path Parameters:**
- `id` (Long) - Task ID

**Response (200 OK):**
```json
{
  "id": 7,
  "title": "Fix login bug",
  "description": "Users cannot login with special characters in password",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "dueDate": "2025-12-31T17:00:00",
  "estimatedHours": 8,
  "assignees": [
    {"id": 3, "username": "alice", "email": "alice@example.com", "fullName": "Alice Johnson"}
  ],
  "project": {
    "id": 1,
    "name": "Website Redesign",
    "description": "Redesign company website",
    "active": true
  },
  "createdAt": "2026-01-04T12:30:45",
  "updatedAt": "2026-01-04T14:15:20"
}
```

**Error Responses:**

| Status Code | Error Code | Description |
|-------------|-----------|-------------|
| 404 | `ENTITY_NOT_FOUND` | Task with specified ID not found |
| 401 | `UNAUTHORIZED` | Missing or invalid authentication credentials |

**Implementation Note:**

Due to Hibernate `@Where` filter issues, this endpoint uses a **3-step workaround**:
1. Load Task entity with native SQL
2. Load assignee IDs separately with native SQL
3. Manually populate `task.assignees` collection

See [KNOWN_ISSUES.md](KNOWN_ISSUES.md#hibernate-where-manytomany-issue) for details.

---

### Update Task

**Endpoint:** `PUT /api/tasks/{id}`

**Authentication:** Required

**Path Parameters:**
- `id` (Long) - Task ID

**Request Body (partial update supported):**
```json
{
  "title": "Fix login bug - Updated",
  "description": "Updated description",
  "status": "IN_PROGRESS",
  "priority": "CRITICAL",
  "dueDate": "2026-01-15T17:00:00",
  "estimatedHours": 12,
  "assigneeIds": [3, 7]
}
```

**Validation Rules:**
- All fields are optional (partial update)
- `title`: Max 200 characters if provided
- `description`: Max 2000 characters if provided
- `status`: Must be valid `TaskStatus` enum value
- `priority`: Must be valid `TaskPriority` enum value
- `assigneeIds`: If provided, at least one assignee required, all must exist and be active

**Response (200 OK):**
```json
{
  "id": 7,
  "title": "Fix login bug - Updated",
  "description": "Updated description",
  "status": "IN_PROGRESS",
  "priority": "CRITICAL",
  "dueDate": "2026-01-15T17:00:00",
  "estimatedHours": 12,
  "assignees": [
    {"id": 3, "username": "alice", "email": "alice@example.com", "fullName": "Alice Johnson"},
    {"id": 7, "username": "admin", "email": "admin@example.com", "fullName": "Admin User"}
  ],
  "project": {
    "id": 1,
    "name": "Website Redesign",
    "active": true
  },
  "createdAt": "2026-01-04T12:30:45",
  "updatedAt": "2026-01-04T15:20:10"
}
```

**Error Responses:**

| Status Code | Error Code | Description |
|-------------|-----------|-------------|
| 404 | `ENTITY_NOT_FOUND` | Task not found or assignee not found |
| 400 | `VALIDATION_ERROR` | Invalid field values |
| 401 | `UNAUTHORIZED` | Missing or invalid authentication credentials |

---

### Delete Task

**Endpoint:** `DELETE /api/tasks/{id}`

**Authentication:** Required

**Path Parameters:**
- `id` (Long) - Task ID

**Response (204 No Content):**
- Empty body
- HTTP status 204

**Behavior:**
- **Hard delete** (permanent deletion)
- Cascade deletes all associated comments
- Cascade deletes all associated attachments
- Removes entries from `task_assignees` junction table

**Error Responses:**

| Status Code | Error Code | Description |
|-------------|-----------|-------------|
| 404 | `ENTITY_NOT_FOUND` | Task with specified ID not found |
| 401 | `UNAUTHORIZED` | Missing or invalid authentication credentials |

---

### List Tasks with Filters (Planned)

**Endpoint:** `GET /api/tasks` ⚠️ **Not Implemented Yet**

**Authentication:** Required

**Query Parameters:**
- `assigneeId` (Long, optional) - Filter by assignee
- `projectId` (Long, optional) - Filter by project
- `status` (String, optional) - Filter by status
- `priority` (String, optional) - Filter by priority
- `page` (int, default=0) - Page number
- `size` (int, default=20) - Page size
- `sort` (String, default=createdAt,desc) - Sort field and direction

**Planned Response (200 OK):**
```json
{
  "content": [
    { "id": 1, "title": "Task 1", ... },
    { "id": 2, "title": "Task 2", ... }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 45,
    "totalPages": 3
  }
}
```

**Status:** Planned for v0.8.0

---

## User Management APIs

### Create User

**Endpoint:** `POST /api/users`

**Authentication:** Required

**Request Body:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "fullName": "John Doe"
}
```

**Validation Rules:**
- `username`: Required, 3-50 characters, alphanumeric + underscore
- `email`: Required, valid email format, unique
- `password`: Required, min 8 characters
- `fullName`: Required, max 100 characters

**Response (201 Created):**
```json
{
  "id": 15,
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "active": true,
  "deleted": false,
  "createdAt": "2026-01-04T16:30:00"
}
```

**Error Responses:**

| Status Code | Error Code | Description |
|-------------|-----------|-------------|
| 400 | `VALIDATION_ERROR` | Invalid request body |
| 409 | `DUPLICATE_RESOURCE` | Username or email already exists |
| 401 | `UNAUTHORIZED` | Missing or invalid authentication credentials |

---

### Get All Users

**Endpoint:** `GET /api/users`

**Authentication:** Required

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "fullName": "Admin User",
    "active": true,
    "deleted": false,
    "createdAt": "2025-12-01T10:00:00"
  },
  {
    "id": 2,
    "username": "alice",
    "email": "alice@example.com",
    "fullName": "Alice Johnson",
    "active": true,
    "deleted": false,
    "createdAt": "2025-12-05T14:20:00"
  }
]
```

**Behavior:**
- Returns only active users (soft-deleted users excluded by default)
- Hibernate `@Where(clause = "deleted = false")` filter applied automatically
- No pagination (planned for future version)

---

### Get User by ID

**Endpoint:** `GET /api/users/{id}`

**Authentication:** Required

**Path Parameters:**
- `id` (Long) - User ID

**Response (200 OK):**
```json
{
  "id": 3,
  "username": "alice",
  "email": "alice@example.com",
  "fullName": "Alice Johnson",
  "active": true,
  "deleted": false,
  "createdAt": "2025-12-05T14:20:00",
  "updatedAt": "2025-12-20T09:15:00"
}
```

**Error Responses:**

| Status Code | Error Code | Description |
|-------------|-----------|-------------|
| 404 | `ENTITY_NOT_FOUND` | User with specified ID not found or deleted |
| 401 | `UNAUTHORIZED` | Missing or invalid authentication credentials |

---

### Update User

**Endpoint:** `PUT /api/users/{id}`

**Authentication:** Required

**Path Parameters:**
- `id` (Long) - User ID

**Request Body (partial update supported):**
```json
{
  "email": "alice.new@example.com",
  "fullName": "Alice Johnson Smith",
  "active": true
}
```

**Validation Rules:**
- All fields are optional
- `email`: Must be valid email format, unique
- `fullName`: Max 100 characters
- `active`: Boolean (true/false)

**Response (200 OK):**
```json
{
  "id": 3,
  "username": "alice",
  "email": "alice.new@example.com",
  "fullName": "Alice Johnson Smith",
  "active": true,
  "deleted": false,
  "updatedAt": "2026-01-04T16:45:00"
}
```

**Error Responses:**

| Status Code | Error Code | Description |
|-------------|-----------|-------------|
| 404 | `ENTITY_NOT_FOUND` | User with specified ID not found |
| 409 | `DUPLICATE_RESOURCE` | Email already in use by another user |
| 401 | `UNAUTHORIZED` | Missing or invalid authentication credentials |

---

### Delete User (Soft Delete)

**Endpoint:** `DELETE /api/users/{id}`

**Authentication:** Required

**Path Parameters:**
- `id` (Long) - User ID

**Response (204 No Content):**
- Empty body
- HTTP status 204

**Behavior:**
- **Soft delete** (user remains in database with `deleted=true`)
- User is automatically excluded from all queries (Hibernate `@Where` filter)
- User is removed from all task assignments (junction table cleanup)
- User can be restored with `POST /api/users/{id}/restore`

**Error Responses:**

| Status Code | Error Code | Description |
|-------------|-----------|-------------|
| 404 | `ENTITY_NOT_FOUND` | User with specified ID not found |
| 401 | `UNAUTHORIZED` | Missing or invalid authentication credentials |

---

### Restore Deleted User

**Endpoint:** `POST /api/users/{id}/restore`

**Authentication:** Required

**Path Parameters:**
- `id` (Long) - User ID

**Response (200 OK):**
```json
{
  "id": 3,
  "username": "alice",
  "email": "alice@example.com",
  "fullName": "Alice Johnson",
  "active": true,
  "deleted": false,
  "updatedAt": "2026-01-04T17:00:00"
}
```

**Behavior:**
- Sets `deleted=false`
- User becomes visible in all queries again
- Does **not** restore previous task assignments (must be reassigned manually)

**Error Responses:**

| Status Code | Error Code | Description |
|-------------|-----------|-------------|
| 404 | `ENTITY_NOT_FOUND` | User with specified ID not found |
| 401 | `UNAUTHORIZED` | Missing or invalid authentication credentials |

---

## Project Management APIs

### Create Project

**Endpoint:** `POST /api/projects`

**Authentication:** Required

**Request Body:**
```json
{
  "name": "Website Redesign",
  "description": "Redesign company website with modern UI",
  "ownerId": 5,
  "startDate": "2026-01-10",
  "endDate": "2026-03-31"
}
```

**Validation Rules:**
- `name`: Required, max 100 characters
- `description`: Optional, max 2000 characters
- `ownerId`: Required, user must exist and be active
- `startDate`: Optional, ISO 8601 date format
- `endDate`: Optional, must be after startDate

**Response (201 Created):**
```json
{
  "id": 10,
  "name": "Website Redesign",
  "description": "Redesign company website with modern UI",
  "owner": {
    "id": 5,
    "username": "project_manager",
    "email": "pm@example.com",
    "fullName": "Project Manager"
  },
  "active": true,
  "startDate": "2026-01-10",
  "endDate": "2026-03-31",
  "createdAt": "2026-01-04T18:00:00"
}
```

**Error Responses:**

| Status Code | Error Code | Description |
|-------------|-----------|-------------|
| 400 | `VALIDATION_ERROR` | Invalid request body |
| 404 | `ENTITY_NOT_FOUND` | Owner user not found |
| 401 | `UNAUTHORIZED` | Missing or invalid authentication credentials |

---

### Get All Projects

**Endpoint:** `GET /api/projects`

**Authentication:** Required

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Website Redesign",
    "description": "Redesign company website",
    "active": true,
    "createdAt": "2025-12-01T10:00:00"
  },
  {
    "id": 2,
    "name": "Mobile App",
    "description": "Develop mobile application",
    "active": true,
    "createdAt": "2025-12-15T14:00:00"
  }
]
```

**Behavior:**
- Returns only active projects (archived projects excluded)
- No pagination (planned for future version)

---

### Get Project by ID

**Endpoint:** `GET /api/projects/{id}`

**Authentication:** Required

**Path Parameters:**
- `id` (Long) - Project ID

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "Website Redesign",
  "description": "Redesign company website with modern UI",
  "owner": {
    "id": 5,
    "username": "project_manager",
    "fullName": "Project Manager"
  },
  "active": true,
  "startDate": "2026-01-10",
  "endDate": "2026-03-31",
  "createdAt": "2025-12-01T10:00:00"
}
```

**Error Responses:**

| Status Code | Error Code | Description |
|-------------|-----------|-------------|
| 404 | `ENTITY_NOT_FOUND` | Project with specified ID not found or archived |
| 401 | `UNAUTHORIZED` | Missing or invalid authentication credentials |

---

### Update Project

**Endpoint:** `PUT /api/projects/{id}`

**Authentication:** Required

**Path Parameters:**
- `id` (Long) - Project ID

**Request Body (partial update supported):**
```json
{
  "name": "Website Redesign v2",
  "description": "Updated project description",
  "endDate": "2026-04-30"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "Website Redesign v2",
  "description": "Updated project description",
  "active": true,
  "endDate": "2026-04-30",
  "updatedAt": "2026-01-04T18:30:00"
}
```

---

### Archive Project

**Endpoint:** `DELETE /api/projects/{id}`

**Authentication:** Required

**Path Parameters:**
- `id` (Long) - Project ID

**Response (204 No Content):**
- Empty body
- HTTP status 204

**Behavior:**
- **Soft archive** (project remains in database with `active=false`)
- Project is excluded from default queries
- Associated tasks are **not deleted** but can no longer be created/updated
- Project can be reactivated with `POST /api/projects/{id}/reactivate`

---

### Reactivate Project

**Endpoint:** `POST /api/projects/{id}/reactivate`

**Authentication:** Required

**Path Parameters:**
- `id` (Long) - Project ID

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "Website Redesign",
  "active": true,
  "updatedAt": "2026-01-04T19:00:00"
}
```

---

### Get Project Tasks

**Endpoint:** `GET /api/projects/{id}/tasks`

**Authentication:** Required

**Path Parameters:**
- `id` (Long) - Project ID

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "title": "Design homepage mockup",
    "status": "COMPLETED",
    "priority": "HIGH",
    "assignees": [
      {"id": 3, "username": "alice", "fullName": "Alice Johnson"}
    ]
  },
  {
    "id": 2,
    "title": "Implement responsive layout",
    "status": "IN_PROGRESS",
    "priority": "MEDIUM",
    "assignees": [
      {"id": 7, "username": "admin", "fullName": "Admin User"}
    ]
  }
]
```

---

## Health & Monitoring

### Health Check

**Endpoint:** `GET /actuator/health`

**Authentication:** Not Required ⚠️

**Response (200 OK):**
```json
{
  "status": "UP"
}
```

**Detailed Health Check (requires authentication):**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760
      }
    }
  }
}
```

---

### Application Metrics

**Endpoint:** `GET /actuator/metrics`

**Authentication:** Required

**Response (200 OK):**
```json
{
  "names": [
    "jvm.memory.used",
    "jvm.threads.live",
    "http.server.requests",
    "jdbc.connections.active",
    "system.cpu.usage"
  ]
}
```

**Get Specific Metric:**
```bash
GET /actuator/metrics/http.server.requests
```

---

### Prometheus Metrics

**Endpoint:** `GET /actuator/prometheus`

**Authentication:** Required

**Response (200 OK):**
```
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="PS Eden Space",} 1.234567E8
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{exception="None",method="GET",outcome="SUCCESS",status="200",uri="/api/tasks",} 42.0
http_server_requests_seconds_sum{exception="None",method="GET",outcome="SUCCESS",status="200",uri="/api/tasks",} 1.234
```

---

## Error Handling

### Standard Error Response Format

All errors return a consistent JSON structure:

```json
{
  "code": "ENTITY_NOT_FOUND",
  "message": "Task with id=123 not found",
  "timestamp": 1704369045000,
  "path": "/api/tasks/123"
}
```

### Error Codes

| Error Code | HTTP Status | Description | Example |
|-----------|-------------|-------------|---------|
| `ENTITY_NOT_FOUND` | 404 | Resource not found | Task, User, or Project doesn't exist |
| `VALIDATION_ERROR` | 400 | Invalid request body | Missing required fields, invalid format |
| `BUSINESS_RULE_VIOLATION` | 400 | Business rule failed | Assignee is deleted, project is archived |
| `DUPLICATE_RESOURCE` | 409 | Resource already exists | Username or email already taken |
| `UNAUTHORIZED` | 401 | Missing/invalid auth | No credentials or wrong password |
| `FORBIDDEN` | 403 | Insufficient permissions | User doesn't have required role (future) |
| `INTERNAL_SERVER_ERROR` | 500 | Server error | Unexpected error, check logs |

### Example Error Scenarios

**Missing Required Field:**
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed: title is required",
  "timestamp": 1704369045000,
  "path": "/api/tasks"
}
```

**Invalid Assignee:**
```json
{
  "code": "ENTITY_NOT_FOUND",
  "message": "User with id=99 not found",
  "timestamp": 1704369045000,
  "path": "/api/tasks"
}
```

**Business Rule Violation:**
```json
{
  "code": "BUSINESS_RULE_VIOLATION",
  "message": "Cannot assign deleted user to task",
  "timestamp": 1704369045000,
  "path": "/api/tasks"
}
```

---

## Testing with cURL

### Local Development Examples

**Create Task:**
```bash
curl -X POST http://localhost:8080/api/tasks \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Task",
    "description": "Testing API",
    "priority": "HIGH",
    "dueDate": "2026-01-31T17:00:00",
    "assigneeIds": [3],
    "projectId": 1
  }'
```

**Get Task:**
```bash
curl -X GET http://localhost:8080/api/tasks/1 \
  -u admin:admin
```

**Update Task:**
```bash
curl -X PUT http://localhost:8080/api/tasks/1 \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_PROGRESS",
    "priority": "CRITICAL"
  }'
```

**Delete Task:**
```bash
curl -X DELETE http://localhost:8080/api/tasks/1 \
  -u admin:admin
```

**Get All Users:**
```bash
curl -X GET http://localhost:8080/api/users \
  -u admin:admin
```

**Create User:**
```bash
curl -X POST http://localhost:8080/api/users \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "fullName": "John Doe"
  }'
```

**Health Check (No Auth):**
```bash
curl -X GET http://localhost:8080/actuator/health
```

---

### Production Examples

Replace `http://localhost:8080` with `https://task-management-system-0c0p.onrender.com`

**Create Task:**
```bash
curl -X POST https://task-management-system-0c0p.onrender.com/api/tasks \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Production Task",
    "priority": "HIGH",
    "assigneeIds": [1],
    "projectId": 1
  }'
```

---

## Testing with Postman

### Postman Collection

[![Run in Postman](https://run.pstmn.io/button.svg)](https://www.postman.com/api-team-5375/workspace/api-workspace/request/37783257-eb670533-dc90-408b-ad08-732c7d8390e1?action=share&creator=37783257)

**Collection includes:**
- ✅ Task Management API (CRUD operations)
- ✅ User Management API (Create, Get, Delete, Restore)
- ✅ Project Management API (CRUD, Archive, Reactivate)
- ✅ Pre-configured environment variables
- ✅ Sample requests with test data
- ✅ Authentication examples (Basic Auth)

### Setup Instructions

1. **Import Collection:**
   - Click "Run in Postman" button above
   - Or manually import from workspace

2. **Configure Environment:**
   ```
   LOCAL_BASE_URL = http://localhost:8080
   PROD_BASE_URL = https://task-management-system-0c0p.onrender.com
   USERNAME = admin
   PASSWORD = admin
   ```

3. **Set Authorization:**
   - Type: Basic Auth
   - Username: `{{USERNAME}}`
   - Password: `{{PASSWORD}}`

4. **Run Requests:**
   - Select environment (Local or Production)
   - Send requests
   - View responses

---

## Planned API Enhancements

### v0.8.0 - Task Filtering & Pagination

**Endpoint:** `GET /api/tasks`

**Query Parameters:**
- `assigneeId` - Filter by assignee
- `projectId` - Filter by project
- `status` - Filter by status
- `priority` - Filter by priority
- `page`, `size`, `sort` - Pagination

---

### v0.9.0 - Task Comments

**Endpoints:**
- `POST /api/tasks/{id}/comments` - Add comment
- `GET /api/tasks/{id}/comments` - List comments
- `PUT /api/comments/{id}` - Update comment
- `DELETE /api/comments/{id}` - Delete comment

---

### v1.0.0 - JWT Authentication

**Endpoints:**
- `POST /api/auth/login` - Get JWT token
- `POST /api/auth/register` - Register new user
- `POST /api/auth/refresh` - Refresh token
- `POST /api/auth/logout` - Invalidate token

---

## Related Documentation

- [docs/ARCHITECTURE.md](ARCHITECTURE.md) - Clean Architecture layers and design patterns
- [docs/DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) - Entity relationships and schema
- [docs/KNOWN_ISSUES.md](KNOWN_ISSUES.md) - Current bugs and workarounds
- [docs/FRONTEND_ARCHITECTURE.md](FRONTEND_ARCHITECTURE.md) - React component architecture

---

**Last Updated:** January 4, 2026  
**Version:** v0.7.0  
**Author:** Task Management Team
