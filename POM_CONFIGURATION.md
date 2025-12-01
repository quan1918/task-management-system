# pom.xml Configuration Guide

## Overview

This `pom.xml` file configures the Maven build system for the **Task Management System** Spring Boot application. It defines project metadata, dependencies, build plugins, and compilation settings for Java 17.

---

## Project Identification

```xml
<groupId>com.taskmanagement</groupId>
<artifactId>task-management-system</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>jar</packaging>
```

| Element | Value | Purpose |
|---------|-------|---------|
| `groupId` | `com.taskmanagement` | Reverse domain notation for package organization |
| `artifactId` | `task-management-system` | Unique project identifier |
| `version` | `1.0.0-SNAPSHOT` | Semantic versioning; SNAPSHOT indicates development version |
| `packaging` | `jar` | Builds as executable JAR file |

---

## Parent Configuration

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
    <relativePath/>
</parent>
```

**Purpose:** Inherits Spring Boot 3.2.0 parent POM, which:
- Provides pre-configured dependency versions
- Sets up common plugins (Spring Boot Maven Plugin, Surefire)
- Defines default configuration for Java compilation, JAR building, etc.

---

## Properties

```xml
<properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <jjwt.version>0.12.3</jjwt.version>
    <lombok.version>1.18.30</lombok.version>
</properties>
```

| Property | Value | Purpose |
|----------|-------|---------|
| `java.version` | 17 | Java language version used (Spring Boot convention) |
| `maven.compiler.source` | 17 | Source code Java version |
| `maven.compiler.target` | 17 | Compiled bytecode target Java version |
| `project.build.sourceEncoding` | UTF-8 | Character encoding for source files |
| `jjwt.version` | 0.12.3 | Version of JWT library (JJWT) for token handling |
| `lombok.version` | 1.18.30 | Version of Lombok for code generation |

---

## Dependencies

### Core Spring Boot Dependencies

#### Spring Boot Web
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
**Purpose:** Enables REST API development with embedded Tomcat server.
**Includes:** Spring Web, Spring MVC, Jackson, Tomcat, Validation.

#### Spring Data JPA
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```
**Purpose:** ORM (Object-Relational Mapping) framework for database operations.
**Includes:** Hibernate, JPA, Spring Data repositories.
**Usage:** Simplifies database queries and entity management.

#### Spring Security
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```
**Purpose:** Authentication and authorization framework.
**Features:** User login, role-based access control (RBAC), password encryption.
**Implements:** JWT token support (via JJWT library).

---

### JWT Authentication (JJWT)

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
```

**Purpose:** JSON Web Token (JWT) library for stateless authentication.

| Dependency | Purpose |
|------------|---------|
| `jjwt-api` | JWT API contracts and interfaces |
| `jjwt-impl` | JWT implementation (runtime only, not needed at compile time) |
| `jjwt-jackson` | JSON serialization support for JWT claims (runtime only) |

**Usage:**
- Generate JWT tokens on successful login
- Validate tokens on API requests
- Extract user claims (userId, roles) from tokens

---

### Validation

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**Purpose:** Input validation for API requests using Jakarta Bean Validation.
**Annotations:** `@NotNull`, `@NotBlank`, `@Email`, `@Min`, `@Max`, etc.
**Usage:** Validates request DTOs before processing.

---

### Database Driver (PostgreSQL)

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.6.0</version>
    <scope>runtime</scope>
</dependency>
```

**Purpose:** JDBC driver for PostgreSQL database connections.
**Scope:** `runtime` (not needed during compilation, only at runtime).
**Configuration:** Connection URL specified in `application.yml`.

---

### Lombok

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>${lombok.version}</version>
    <optional>true</optional>
</dependency>
```

**Purpose:** Code generation library to reduce boilerplate.

**Annotations:**
- `@Getter` / `@Setter` - Auto-generate getters/setters
- `@NoArgsConstructor` - Generate default constructor
- `@AllArgsConstructor` - Generate constructor with all fields
- `@Data` - Combines @Getter, @Setter, @ToString, @EqualsAndHashCode
- `@Builder` - Fluent builder pattern

**Example:**
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String email;
}
// Automatically generates: getters, setters, equals, hashCode, toString, constructors
```

---

### Database Migrations (Flyway)

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>9.22.3</version>
</dependency>
```

**Purpose:** Version control for database schema changes.
**Workflow:**
1. Create SQL migration files (e.g., `V1__initial_schema.sql`)
2. Flyway automatically applies migrations on application startup
3. Tracks applied migrations in `flyway_schema_history` table
4. Ensures database schema stays in sync with code

**Benefits:**
- Reproducible database setup across environments
- Track schema evolution over time
- Prevent manual SQL script errors

---

### Jackson (JSON Processing)

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

**Purpose:** JSON serialization/deserialization for Java 8+ date/time types.
**Handles:** `LocalDateTime`, `LocalDate`, `Instant`, etc.
**Usage:** Automatically converts Java time objects to/from JSON.

**Example:**
```json
// Before: "2025-12-01T10:30:00" (string)
// After: 2025-12-01T10:30:00 (ISO-8601 format)
```

---

### Spring Boot Actuator

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Purpose:** Monitoring and metrics endpoints for production monitoring.

**Available Endpoints:**
- `/actuator/health` - Application health status
- `/actuator/metrics` - Performance metrics (memory, CPU, HTTP requests)
- `/actuator/prometheus` - Prometheus-compatible metrics

**Configuration (in `application.yml`):**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

---

### Springdoc OpenAPI (Swagger/OpenAPI)

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.0.2</version>
</dependency>
```

**Purpose:** Auto-generates interactive API documentation.

**Features:**
- Interactive Swagger UI for testing endpoints
- OpenAPI 3.0 schema generation
- Automatic endpoint discovery from Spring annotations

**Access:**
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

---

### Optional Dependencies (Phase 2+)

These are commented out and can be enabled for advanced features:

#### RabbitMQ Support
```xml
<!-- <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency> -->
```
**Purpose:** Message queuing for asynchronous notifications and event processing.
**Use Case:** Send task notifications without blocking API responses.

#### Redis Support
```xml
<!-- <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency> -->
```
**Purpose:** In-memory caching for high-performance data access.
**Use Case:** Cache frequently accessed tasks, user profiles, session data.

#### Kafka Support
```xml
<!-- <dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency> -->
```
**Purpose:** High-throughput event streaming and log aggregation.
**Use Case:** Stream task events to analytics and monitoring systems.

---

## Testing Dependencies

### Spring Boot Test
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Includes:** JUnit 5, Mockito, AssertJ, Hamcrest.
**Purpose:** Unit and integration testing framework.

### Spring Security Test
```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Purpose:** Testing utilities for Spring Security (authentication, authorization).
**Annotations:** `@WithMockUser`, `@WithAnonymousUser`.

### JUnit 5
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

**Purpose:** Modern testing framework with parameterized tests, nested tests, extensions.

### Mockito
```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

**Purpose:** Mocking framework for isolating code under test.
**Usage:** Mock repositories, services, external APIs in unit tests.

### TestContainers
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

**Purpose:** Docker containers for integration testing with real PostgreSQL instance.
**Benefits:**
- Test against actual database (not mocks)
- Spin up PostgreSQL container automatically for tests
- Tear down after tests complete

**Example:**
```java
@Testcontainers
class TaskRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    // Tests run against real PostgreSQL instance
}
```

---

## Build Plugins

### Spring Boot Maven Plugin

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </exclude>
        </excludes>
    </configuration>
</plugin>
```

**Purpose:** Packages application as executable JAR with embedded Tomcat.

**Features:**
- Creates "fat JAR" containing all dependencies
- Generates `BOOT-INF` directory structure
- Main-Class automatically configured

**Usage:**
```bash
mvn clean package
java -jar task-management-system-1.0.0-SNAPSHOT.jar
```

**Configuration:** Excludes Lombok from JAR (compile-time only).

---

### Maven Compiler Plugin

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>17</source>
        <target>17</target>
    </configuration>
</plugin>
```

**Purpose:** Explicitly configures Java 17 compilation.
**Ensures:** Consistent Java version across all build machines.

---

### Surefire Plugin

```xml
<plugin>
    <groupId>org.apache.maven.surefire</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.0.0</version>
</plugin>
```

**Purpose:** Runs unit tests during build (`mvn test`).
**Features:** Discovers and executes all `*Test.java` and `*Tests.java` classes.

---

### JaCoCo Plugin (Code Coverage)

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Purpose:** Measures code coverage (what % of code is tested).

**Execution Flow:**
1. `prepare-agent` - Instruments bytecode for coverage tracking
2. Tests run and coverage data is collected
3. `report` - Generates coverage report in `target/site/jacoco/index.html`

**Usage:**
```bash
mvn clean test
# Open target/site/jacoco/index.html in browser to view coverage report
```

---

## Build Lifecycle Commands

### Clean and Build
```bash
mvn clean compile
```
Removes old build artifacts and compiles source code.

### Run Tests
```bash
mvn test
```
Executes all unit tests.

### Package Application
```bash
mvn clean package
```
Compiles, tests, and creates executable JAR file in `target/` directory.

### Run Application (During Development)
```bash
mvn spring-boot:run
```
Starts the application directly (useful for development).

### Run with Specific Profile
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```
Starts with dev profile (dev database, verbose logging).

---

## Dependency Management Best Practices

### Version Control
- **Direct versions:** Used only for JJWT, Lombok, Flyway, TestContainers, JaCoCo
- **Spring Boot managed:** All other Spring dependencies inherit versions from parent POM
- **Properties:** Centralized version definitions using `<properties>` for easy updates

### Scope Definitions

| Scope | Usage | Examples |
|-------|-------|----------|
| **compile** (default) | Available in all classpaths (source, test, runtime) | Spring Web, JPA, Security |
| **runtime** | Needed only at runtime, not for compilation | PostgreSQL driver, Jackson datatype |
| **test** | Only in test classpath | JUnit, Mockito, Spring Security Test |
| **optional** | Dependency is optional; dependents must explicitly include | Lombok |

### Why Scope Matters
- **runtime scope:** Reduces JAR size, doesn't need them during compilation
- **test scope:** Excludes testing libraries from production JAR
- **optional:** Consumers of your library must explicitly request Lombok

---

## Future Configuration for Phase 2+

### Adding RabbitMQ
1. Uncomment `spring-boot-starter-amqp` in pom.xml
2. Configure in `application.yml`:
   ```yaml
   spring:
     rabbitmq:
       host: localhost
       port: 5672
   ```
3. Create message producers and listeners

### Adding Redis
1. Uncomment `spring-boot-starter-data-redis` in pom.xml
2. Configure in `application.yml`:
   ```yaml
   spring:
     redis:
       host: localhost
       port: 6379
   ```
3. Implement caching annotations

### Adding Kafka
1. Uncomment `spring-kafka` in pom.xml
2. Configure producers and consumers
3. Integrate with event system

---

## Troubleshooting

### Issue: Compilation Error with Java Version
**Solution:** Ensure Java 17 is installed and JAVA_HOME points to Java 17.
```bash
java -version
# Output should show openjdk version 17.x.x
```

### Issue: PostgreSQL Driver Not Found
**Solution:** PostgreSQL dependency has `runtime` scope. It's included in JAR but not during compilation.
```bash
mvn clean package  # Ensures driver is downloaded
```

### Issue: Lombok Annotations Not Working
**Solution:** Enable annotation processing in IDE:
- **IntelliJ:** Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable
- **VS Code:** Install "Project Manager for Java" and "Extension Pack for Java"

### Issue: Tests Not Running
**Solution:** Ensure test files follow naming convention (`*Test.java` or `*Tests.java`).
```bash
mvn test -X  # Verbose output to debug
```

---

## Summary

This `pom.xml` provides a complete, production-ready Spring Boot 3.2 Maven configuration with:

✅ **Core Framework:** Spring Boot Web, JPA/Hibernate, Security  
✅ **Authentication:** JWT tokens via JJWT library  
✅ **Database:** PostgreSQL with Flyway migrations  
✅ **API Documentation:** Swagger/OpenAPI via Springdoc  
✅ **Monitoring:** Actuator endpoints for health and metrics  
✅ **Testing:** JUnit 5, Mockito, TestContainers, Spring Security Test  
✅ **Code Quality:** JaCoCo for code coverage analysis  
✅ **Extensibility:** Optional dependencies for Phase 2+ features (RabbitMQ, Redis, Kafka)  

The configuration follows Maven best practices and Spring Boot conventions, making it easy to maintain, extend, and deploy.
