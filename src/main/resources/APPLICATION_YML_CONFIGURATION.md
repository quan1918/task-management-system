# application.yml Configuration Guide

## Overview

The `application.yml` file configures Spring Boot application settings, database connections, logging, server properties, and monitoring endpoints. It supports multiple profiles (dev, prod) for environment-specific configurations.

**Location:** `src/main/resources/application.yml`

---

## File Structure

The file is divided into three sections separated by `---`:

1. **Base Configuration** - Shared across all profiles
2. **Development Profile** - Development environment settings
3. **Production Profile** - Production environment settings

**Profile Activation:**
```bash
# Run with development profile (default)
mvn spring-boot:run

# Run with production profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"

# Or set environment variable
export SPRING_PROFILES_ACTIVE=prod
java -jar task-management-system-1.0.0-SNAPSHOT.jar
```

---

## Base Configuration (Shared)

### Application Identity

```yaml
spring:
  application:
    name: task-management-system
  
  profiles:
    active: dev
```

| Property | Value | Purpose |
|----------|-------|---------|
| `spring.application.name` | `task-management-system` | Application name (used in logs, actuator) |
| `spring.profiles.active` | `dev` | Default profile if not overridden |

**Usage:** Application name appears in:
- Log entries: `[task-management-system]`
- Health check endpoint: `/actuator/health`
- Monitoring dashboards

---

### JPA & Hibernate Configuration

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        use_sql_comments: true
```

#### Hibernate DDL Auto Strategy

| Value | Behavior | When to Use |
|-------|----------|------------|
| `validate` | Validates schema against entities, fails if mismatch | **PRODUCTION & DEV** (recommended) |
| `create` | Drops and recreates schema on startup | Testing only |
| `create-drop` | Drops schema on shutdown | Integration tests |
| `update` | Modifies schema incrementally | Legacy systems (risky) |

**Why `validate` is used:**
- Database schema changes managed by **Flyway** (not Hibernate)
- Prevents accidental schema modifications
- Ensures code and database stay in sync
- Safer for production deployments

#### Hibernate Properties

| Property | Value | Purpose |
|----------|-------|---------|
| `dialect` | `PostgreSQLDialect` | Database-specific SQL generation for PostgreSQL |
| `format_sql` | `true` | Pretty-print SQL queries (easier to read) |
| `use_sql_comments` | `true` | Add HQL source comments to SQL (helps debugging) |
| `show_sql` | `false` | Don't log SQL to console (use `logging.level` instead) |

**Example of formatted SQL:**
```sql
-- Without formatting
select u1_0.id,u1_0.email,u1_0.name from users u1_0 where u1_0.id=?

-- With formatting and comments
/* select
        u1_0.id,
        u1_0.email,
        u1_0.name 
    from
        users u1_0 
    where
        u1_0.id=? */
select u1_0.id,u1_0.email,u1_0.name from users u1_0 where u1_0.id=?
```

---

### Jackson (JSON Processing)

```yaml
spring:
  jackson:
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false
      indent-output: false
```

#### JSON Serialization Settings

| Property | Value | Purpose |
|----------|-------|---------|
| `default-property-inclusion` | `non_null` | Exclude null fields from JSON response |
| `write-dates-as-timestamps` | `false` | Use ISO-8601 date format (not Unix timestamps) |
| `indent-output` | `false` | Compact JSON (no pretty-printing) |

**Example: Non-null Inclusion**

```java
// Entity with null field
User user = new User();
user.setId(1L);
user.setEmail("user@example.com");
user.setMiddleName(null);  // null field

// Response without non_null filter
{
  "id": 1,
  "email": "user@example.com",
  "middleName": null,          // Unnecessary
  "phoneNumber": null
}

// Response with default-property-inclusion: non_null
{
  "id": 1,
  "email": "user@example.com"  // null fields omitted
}
```

**Example: Date Formatting**

```java
LocalDateTime createdAt = LocalDateTime.of(2025, 12, 1, 10, 30, 0);

// write-dates-as-timestamps: true
"createdAt": 1733041800000  // Unix timestamp in milliseconds

// write-dates-as-timestamps: false
"createdAt": "2025-12-01T10:30:00"  // ISO-8601 (human-readable)
```

---

### Servlet Configuration

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

| Property | Value | Purpose |
|----------|-------|---------|
| `max-file-size` | `10MB` | Maximum size for individual file upload |
| `max-request-size` | `10MB` | Maximum size for entire multipart request |

**Use Case:** Task attachments upload
- User uploads task attachment (PDF, image, etc.)
- If file exceeds 10MB, request is rejected with HTTP 413 (Payload Too Large)

**Validation Chain:**
```
Client → Upload File → Servlet Container → Check Size → Database
  ↓
  size > 10MB? → Reject (413)
```

---

### Server Configuration

```yaml
server:
  servlet:
    context-path: /api
  error:
    include-message: always
    include-binding-errors: always
```

#### Context Path

| Setting | Value | Effect |
|---------|-------|--------|
| `context-path` | `/api` | All endpoints prefixed with `/api` |

**Example:**
```
Without context-path:
GET http://localhost:8080/tasks

With context-path: /api
GET http://localhost:8080/api/tasks
```

**Reason for `/api` prefix:**
- Professional API versioning (can be `/api/v1` later)
- Separates API from static resources
- Clear distinction: `/api/*` (API), `/static/*` (assets), `/` (web)

#### Error Handling

| Property | Value | Purpose |
|----------|-------|---------|
| `include-message` | `always` | Include error message in response |
| `include-binding-errors` | `always` | Include validation errors (field-level) |

**Example Error Response:**
```json
{
  "timestamp": "2025-12-01T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid input",
  "path": "/api/tasks",
  "errors": [
    {
      "field": "email",
      "defaultMessage": "must be a valid email"
    },
    {
      "field": "taskName",
      "defaultMessage": "must not be blank"
    }
  ]
}
```

---

### Actuator (Monitoring & Metrics)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
```

#### Available Endpoints

| Endpoint | Path | Purpose | Requires Auth |
|----------|------|---------|---------------|
| Health | `/actuator/health` | Application status (UP/DOWN) | No |
| Metrics | `/actuator/metrics` | Performance metrics (CPU, memory, requests) | Yes |
| Prometheus | `/actuator/prometheus` | Prometheus-format metrics | Yes |

**Health Check Example:**
```bash
curl http://localhost:8080/api/actuator/health
```

**Response:**
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
        "total": 250000000000,
        "free": 125000000000,
        "threshold": 10000000
      }
    }
  }
}
```

**Metrics Example:**
```bash
curl http://localhost:8080/api/actuator/metrics
```

**Available Metrics:**
- `jvm.memory.used` - JVM heap memory usage
- `system.cpu.usage` - CPU utilization
- `http.server.requests` - HTTP request counts and latency
- `jdbc.connections.active` - Active database connections
- `logback.events` - Log events by level

**Prometheus Integration:**
```bash
curl http://localhost:8080/api/actuator/prometheus
```

Returns metrics in Prometheus format for scraping:
```
# HELP jvm_memory_used_bytes ...
jvm_memory_used_bytes{area="heap",id="PS Survivor Space",} 1.048576E7
```

#### Health Details Authorization

```yaml
show-details: when-authorized
```

- **Unauthenticated users:** See only `"status": "UP"`
- **Authenticated users:** See full component details (database, disk, JVM)

**Reason:** Prevents information leakage about system internals to unauthorized users.

---

### Logging Configuration

```yaml
logging:
  level:
    root: INFO
    com.taskmanagement: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/task-management.log
```

#### Logging Levels

| Level | Use Case | Examples |
|-------|----------|----------|
| **TRACE** | Lowest granularity | Variable values at each line |
| **DEBUG** | Development debugging | Method entry/exit, variable states |
| **INFO** | Important events | Application startup, user actions |
| **WARN** | Potentially harmful | Deprecated API usage, missing config |
| **ERROR** | Error conditions | Database connection failed |
| **FATAL** | Highest severity | Unrecoverable errors |

#### Logger Configuration

```yaml
root: INFO                                    # All loggers default to INFO
com.taskmanagement: DEBUG                     # Our code: DEBUG (detailed)
org.springframework.security: DEBUG           # Spring Security: DEBUG
org.hibernate.SQL: DEBUG                      # SQL queries: DEBUG
```

**Log Output Examples:**

```
[INFO ] 10:30:00 - Starting TaskManagementApplication
[DEBUG] 10:30:01 - Initializing database connection pool
[DEBUG] 10:30:02 - SELECT u.* FROM users u WHERE u.id = ?
[INFO ] 10:30:03 - Application started successfully
[WARN ] 10:30:05 - DatabaseConnection timeout after 30s
[ERROR] 10:30:06 - Failed to save task: NullPointerException
```

#### Log Patterns

**Console Pattern:**
```yaml
pattern:
  console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

```
2025-12-01 10:30:00 - Application started successfully
2025-12-01 10:30:01 - User logged in: user@example.com
```

**File Pattern:**
```yaml
pattern:
  file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

```
2025-12-01 10:30:00 [http-nio-8080-exec-1] INFO  c.t.controller.TaskController - GET /api/tasks
2025-12-01 10:30:01 [http-nio-8080-exec-2] DEBUG c.t.service.TaskService - Task retrieved: id=123
2025-12-01 10:30:02 [scheduling-1] ERROR c.t.event.NotificationListener - Email send failed: SMTP timeout
```

#### Log File Output

```yaml
file:
  name: logs/task-management.log
```

- Log file location: `logs/task-management.log`
- Created relative to application directory
- Contains full log pattern with thread and logger names

**Log File Growth:**
- No rotation by default in base config
- Dev profile: single file
- Prod profile: rotated every 10MB, keep 30 days (see below)

---

## Development Profile

Activated when `spring.profiles.active=dev` (the default).

```yaml
spring:
  config:
    activate:
      on-profile: dev
  
  datasource:
    url: jdbc:postgresql://localhost:5432/task_management_dev
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true

server:
  port: 8080

logging:
  level:
    root: INFO
    com.taskmanagement: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
```

### Development Database

```yaml
datasource:
  url: jdbc:postgresql://localhost:5432/task_management_dev
  username: postgres
  password: postgres
```

**Connection Details:**
- **Host:** `localhost` (local machine)
- **Port:** `5432` (PostgreSQL default)
- **Database:** `task_management_dev`
- **Credentials:** postgres/postgres (default local setup)

**Setup Instructions:**

1. **Install PostgreSQL:**
   ```bash
   # Windows
   choco install postgresql
   
   # macOS
   brew install postgresql
   
   # Linux (Ubuntu)
   sudo apt-get install postgresql postgresql-contrib
   ```

2. **Create development database:**
   ```bash
   psql -U postgres
   
   postgres=# CREATE DATABASE task_management_dev;
   postgres=# \q
   ```

3. **Verify connection:**
   ```bash
   psql -U postgres -d task_management_dev
   ```

### Development Logging

```yaml
logging:
  level:
    root: INFO
    com.taskmanagement: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
```

**Enhanced Debugging:**
- All application code: DEBUG (verbose output)
- Spring Web: DEBUG (HTTP requests, response statuses)
- Spring Security: DEBUG (authentication, authorization decisions)

**Example Development Log Output:**
```
[DEBUG] 10:30:00 [http-nio-8080-exec-1] o.s.w.s.DispatcherServlet - GET /api/tasks, parameters={}
[DEBUG] 10:30:00 [http-nio-8080-exec-1] o.s.security.web.FilterChainProxy - Securing GET /api/tasks
[DEBUG] 10:30:00 [http-nio-8080-exec-1] c.t.security.JwtAuthenticationFilter - Validating token...
[DEBUG] 10:30:01 [http-nio-8080-exec-1] c.t.service.TaskService - Fetching tasks for user: user123
[DEBUG] 10:30:01 [http-nio-8080-exec-1] o.h.SQL - SELECT t.* FROM tasks t WHERE t.user_id = ?
[INFO ] 10:30:01 [http-nio-8080-exec-1] o.s.w.s.DispatcherServlet - Completed 200 OK
```

### Development Server

```yaml
server:
  port: 8080
```

**Access Points:**
- Application: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- Health Check: `http://localhost:8080/api/actuator/health`
- API Docs: `http://localhost:8080/api/v3/api-docs`

---

## Production Profile

Activated with `--spring.profiles.active=prod`.

```yaml
spring:
  config:
    activate:
      on-profile: prod
  
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/task_management_prod}
    username: ${DATABASE_USER:postgres}
    password: ${DATABASE_PASSWORD:}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: false

server:
  port: ${SERVER_PORT:8080}
  compression:
    enabled: true
    min-response-size: 1024

logging:
  level:
    root: WARN
    com.taskmanagement: INFO
  file:
    name: /var/log/task-management/task-management.log
    max-size: 10MB
    max-history: 30
```

### Production Database

```yaml
datasource:
  url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/task_management_prod}
  username: ${DATABASE_USER:postgres}
  password: ${DATABASE_PASSWORD:}
```

**Environment-based Configuration:**
- Uses environment variables for sensitive data
- Fallback defaults if env vars not set

**Setting Environment Variables:**

```bash
# Linux/macOS
export DATABASE_URL=jdbc:postgresql://prod-db.example.com:5432/task_management
export DATABASE_USER=prod_user
export DATABASE_PASSWORD=secure_password_here
export SERVER_PORT=8443

java -jar task-management-system-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod

# Docker
docker run -e DATABASE_URL=jdbc:postgresql://db-host:5432/db \
           -e DATABASE_USER=user \
           -e DATABASE_PASSWORD=pass \
           -e SERVER_PORT=8080 \
           task-management-system:latest

# Kubernetes
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  DATABASE_URL: jdbc:postgresql://postgres-service:5432/task_management
  DATABASE_USER: prod_user
  # PASSWORD stored in Secret (not ConfigMap)
---
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
type: Opaque
stringData:
  DATABASE_PASSWORD: secure_password_here
```

### Connection Pooling (HikariCP)

```yaml
hikari:
  maximum-pool-size: 20
  minimum-idle: 5
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
```

**HikariCP is the default connection pool in Spring Boot 2+**

| Property | Value | Purpose |
|----------|-------|---------|
| `maximum-pool-size` | 20 | Maximum concurrent database connections |
| `minimum-idle` | 5 | Minimum idle connections kept ready |
| `connection-timeout` | 30000ms | Max wait time for connection (30 seconds) |
| `idle-timeout` | 600000ms | Close idle connections after 10 minutes |
| `max-lifetime` | 1800000ms | Connection lifetime limit (30 minutes) |

**How It Works:**

```
Initial State: 5 connections ready
        ↓
Request 1-5: Use existing connections (no wait)
        ↓
Request 6-20: New connections created on-demand
        ↓
Request 21+: Wait up to 30s for a connection to become available
        ↓
After 10 minutes idle: Unused connections closed
```

**Why These Settings:**

- **maximum-pool-size: 20** - Supports ~2000 requests/second at 100ms avg response time
- **minimum-idle: 5** - Quick response for sudden traffic spikes
- **connection-timeout: 30s** - Graceful degradation if database is slow
- **idle-timeout: 10min** - Prevents connection resource waste
- **max-lifetime: 30min** - Ensures connections don't become stale

### Production Logging

```yaml
logging:
  level:
    root: WARN
    com.taskmanagement: INFO
  file:
    name: /var/log/task-management/task-management.log
    max-size: 10MB
    max-history: 30
```

**Minimal Logging:**
- Root logger: WARN only (errors and warnings)
- Application code: INFO only (important events)
- No DEBUG or TRACE spam

**Example Production Log Output:**
```
2025-12-01 10:30:00 [app] INFO TaskManagementApplication - Application started in 3.5s
2025-12-01 10:30:05 [app] INFO UserService - User logged in: user@example.com
2025-12-01 10:30:10 [app] WARN TaskService - Task assignment failed: user not found
2025-12-01 10:30:15 [app] ERROR NotificationService - Email service unavailable, retrying...
```

**Log Rotation:**

```yaml
file:
  name: /var/log/task-management/task-management.log
  max-size: 10MB        # Rotate when file reaches 10MB
  max-history: 30       # Keep 30 days of history
```

**Log File Examples:**
```
/var/log/task-management/
  ├── task-management.log              # Current log file (< 10MB)
  ├── task-management.log.1.gz         # 2024-11-30
  ├── task-management.log.2.gz         # 2024-11-29
  ├── task-management.log.3.gz         # 2024-11-28
  └── ... (up to 30 days)
```

### Production Server & Compression

```yaml
server:
  port: ${SERVER_PORT:8080}
  compression:
    enabled: true
    min-response-size: 1024
```

**Gzip Compression:**

| Setting | Value | Purpose |
|---------|-------|---------|
| `enabled` | `true` | Enable gzip compression |
| `min-response-size` | 1024 | Only compress responses > 1KB |

**Effect on Network Traffic:**

```
Uncompressed JSON Response:
{
  "id": 1,
  "title": "Implement user authentication",
  "description": "Add JWT token-based authentication...",
  "status": "IN_PROGRESS",
  ...
}
Size: 2.5 KB

Compressed (gzip):
Size: 0.8 KB (68% reduction)

Transfer Time (1 Mbps):
- Uncompressed: 20ms
- Compressed: 6.4ms (3x faster)
```

**Browser Support:**
```
Request Header:
Accept-Encoding: gzip, deflate, br

Response Header:
Content-Encoding: gzip
Content-Length: 800  (smaller than original 2500)

Browser: Automatically decompresses
```

---

## Profile-Specific Comparison Table

| Setting | Dev | Prod |
|---------|-----|------|
| **Database** | localhost:5432 | Environment variable |
| **SQL Logging** | Enabled (`show-sql: true`) | Disabled |
| **SQL Formatting** | Pretty-printed | Compact |
| **Log Level** | DEBUG | WARN/INFO |
| **Log Output** | Console + File | File only with rotation |
| **Server Port** | 8080 | Environment variable |
| **Compression** | None | Gzip enabled |
| **Connection Pool** | Basic | Optimized (20 connections) |
| **Credentials** | Hardcoded | Environment variables |
| **Metrics Exposure** | All | Restricted |

---

## Security Considerations

### 1. Never Commit Passwords

❌ **WRONG:**
```yaml
# DO NOT COMMIT THIS!
datasource:
  password: super_secret_password_123
```

✅ **CORRECT:**
```yaml
datasource:
  password: ${DATABASE_PASSWORD:}  # Loaded from environment
```

### 2. Use Environment Variables for Production

```bash
# .env file (NOT committed to git)
DATABASE_PASSWORD=production_secret_here

# Load in shell
export $(cat .env | xargs)

# Or in Docker
docker run -e DATABASE_PASSWORD=$DATABASE_PASSWORD ...
```

### 3. Restrict Actuator Endpoints

Current setup includes `health`, `metrics`, `prometheus` - consider restricting in production:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health              # Only health check publicly
  endpoint:
    health:
      show-details: when-authorized  # Hide details from unauthenticated users
```

### 4. Use HTTPS in Production

Configure SSL/TLS:

```yaml
server:
  port: 8443
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

---

## Troubleshooting

### Issue: Database Connection Refused

**Error:** `Connection refused: connect`

**Solution:**
1. Verify PostgreSQL is running: `psql -U postgres`
2. Check dev database exists: `psql -U postgres -d task_management_dev`
3. Verify credentials match in `application.yml`
4. Check firewall: PostgreSQL default port is 5432

### Issue: SQL Queries Not Visible in Logs

**Problem:** `show-sql: true` but no SQL in logs

**Solution:** Set `logging.level.org.hibernate.SQL` to DEBUG in application.yml (already configured)

### Issue: Multipart Upload Fails

**Error:** `The field exceeds its maximum permitted size of 10485760 bytes`

**Solution:** Increase limits in application.yml:
```yaml
servlet:
  multipart:
    max-file-size: 50MB
    max-request-size: 50MB
```

### Issue: Port 8080 Already in Use

**Error:** `Address already in use: bind`

**Solution 1:** Kill process on port 8080
```bash
# macOS/Linux
lsof -ti:8080 | xargs kill -9

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Solution 2:** Change port in dev profile:
```yaml
server:
  port: 8081
```

### Issue: Logs Not Appearing in File

**Error:** No file created at `logs/task-management.log`

**Solution:** Ensure `logs/` directory exists:
```bash
mkdir -p logs
```

Or configure absolute path:
```yaml
logging:
  file:
    name: /var/log/task-management/task-management.log
```

---

## Summary

The `application.yml` provides:

✅ **Base Configuration** - Shared Spring Boot, JPA, Jackson, Server settings  
✅ **Development Profile** - Local PostgreSQL, verbose logging, quick debugging  
✅ **Production Profile** - Environment-based config, optimized pooling, minimal logging  
✅ **Security** - Environment variables for sensitive data, HTTPS-ready  
✅ **Monitoring** - Actuator endpoints for health, metrics, Prometheus integration  
✅ **Performance** - Gzip compression, connection pooling, log rotation  

Choose the right profile for your environment:
- **Development:** `mvn spring-boot:run` (uses `dev` profile by default)
- **Production:** `java -jar app.jar --spring.profiles.active=prod`

This configuration supports the complete application lifecycle from development through enterprise production deployment.
