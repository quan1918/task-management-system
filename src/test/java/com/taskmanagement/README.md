# Test Suite Documentation

## Overview

This document describes the test architecture for the Task Management System. The test suite follows Spring Boot best practices with JUnit 5 and Mockito for unit testing.

---

## Test Structure

```
src/test/java/com/taskmanagement/
├── README.md                              # This file
├── TaskManagementApplicationTests.java    # Application context smoke test
│
├── api/                                   # Controller Layer Tests
│   ├── ProjectControllerTest.java
│   ├── TaskControllerTest.java
│   └── UserControllerTest.java
│
├── service/                               # Service Layer Tests (Unit)
│   ├── ProjectServiceTest.java
│   ├── TaskServiceTest.java
│   └── UserServiceTest.java
│
├── repository/                            # Repository Layer Tests (Optional)
│   ├── ProjectRepositoryTest.java
│   ├── TaskRepositoryTest.java
│   └── UserRepositoryTest.java
│
├── exception/                             # Exception Handler Tests
│   └── GlobalExceptionHandlerTest.java
│
└── util/                                  # Test Utilities
    ├── TestDataBuilder.java              # Builder for test entities
    └── TestConstants.java                # Shared test constants
```

---

## Running Tests

### Run all tests
```bash
mvn test
```

### Run specific test class
```bash
mvn test -Dtest=ProjectServiceTest
```

### Run tests with coverage report
```bash
mvn test jacoco:report
```

### Run tests in specific package
```bash
mvn test -Dtest=com.taskmanagement.service.*
```

---

## Test Types

### 1. Controller Tests (`api/` package)

**Purpose:** Test HTTP endpoints, request/response mapping, and validation

**Annotations:**
```java
@WebMvcTest(ProjectController.class)
@MockBean(ProjectService.class)
```

**What to test:**
- HTTP status codes (200, 201, 400, 404, etc.)
- Request body validation
- Path variables and query parameters
- Response body structure
- Exception handling

**Mocking strategy:**
- Mock: Service layer using `@MockBean`
- Don't mock: Controller, MockMvc, ObjectMapper

---

### 2. Service Tests (`service/` package)

**Purpose:** Test business logic and transaction handling

**Annotations:**
```java
@ExtendWith(MockitoExtension.class)
@Mock private ProjectRepository projectRepository;
@InjectMocks private ProjectService projectService;
```

**What to test:**
- Business rules and validation
- Entity creation/update logic
- Exception throwing
- Method return values
- Edge cases

**Mocking strategy:**
- Mock: Repository layer using `@Mock`
- Mock: Other services (if inter-service calls exist)
- Don't mock: Service under test, DTOs

---

### 3. Repository Tests (`repository/` package) - OPTIONAL

**Purpose:** Test custom JPA queries (only if `@Query` annotations exist)

**Annotations:**
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
```

**When to create:**
- Custom JPQL or native queries
- Complex joins or filters
- Derived query methods with complex logic

**When to SKIP:**
- Spring Data JPA's built-in methods (`findById`, `save`, `deleteById`)
- No custom `@Query` annotations

---

### 4. Exception Handler Tests (`exception/` package)

**Purpose:** Test `@ControllerAdvice` exception mappings

**What to test:**
- Exception → HTTP status code mapping
- Error response body structure
- Different exception types (404, 400, 500, etc.)

---

## Naming Conventions

### Test Classes
- Pattern: `<ClassName>Test`
- Examples:
  - `ProjectController` → `ProjectControllerTest`
  - `ProjectService` → `ProjectServiceTest`
  - `ProjectRepository` → `ProjectRepositoryTest`

### Test Methods
- Pattern: `methodName_scenario_expectedBehavior()`
- Examples:
  ```java
  createProject_ValidData_ReturnsProject()
  getProjectById_ProjectNotFound_ThrowsException()
  updateTask_MissingRequiredField_Returns400BadRequest()
  deleteUser_UserHasActiveTasks_ThrowsBusinessRuleException()
  ```

---

## Test Utilities

### TestDataBuilder (`util/TestDataBuilder.java`)

**Purpose:** Centralized factory for creating test entities

**Example usage:**
```java
Project project = TestDataBuilder.buildValidProject();
Task task = TestDataBuilder.buildValidTask();
User user = TestDataBuilder.buildValidUser();
```

**Benefits:**
- Avoid duplicate test data setup
- Easy to maintain when entity structure changes
- Readable test code

---

### TestConstants (`util/TestConstants.java`)

**Purpose:** Shared test constants

**Example constants:**
```java
public static final Long TEST_PROJECT_ID = 1L;
public static final String TEST_PROJECT_NAME = "Test Project";
public static final String TEST_USER_EMAIL = "test@example.com";
```

---

## What NOT to Test

❌ **Skip these in unit tests:**
- Getters/setters in entity classes
- Spring Boot auto-configuration
- Third-party library internals (Hibernate, Jackson)
- Simple DTO mappers (unless complex transformation logic)
- Enum constants
- `equals()` / `hashCode()` (unless custom logic)
- `toString()` methods
- Constructors without validation logic

---

## Mocking Strategy

| Test Type | Mock This | Don't Mock This |
|-----------|-----------|-----------------|
| **Controller** | Services | Controller, ObjectMapper, MockMvc |
| **Service** | Repositories, other Services | Service under test, DTOs |
| **Repository** | Nothing (use real DB) | Repository, EntityManager |
| **Exception Handler** | Controllers throwing exceptions | GlobalExceptionHandler |

---

## Coverage Goals

| Layer | Priority | Target Coverage | Rationale |
|-------|----------|-----------------|-----------|
| **Service** | HIGH | 80-90% | Contains all business logic |
| **Controller** | MEDIUM | 70-80% | Focus on error cases + validation |
| **Exception Handler** | HIGH | 90%+ | Critical for API error responses |
| **Repository** | LOW | 0-50% | Only if custom queries exist |
| **Entity** | SKIP | 0% | No business logic |

---

## Test Configuration

### Test Resources

```
src/test/resources/
├── application-test.yml    # Test-specific config
└── logback-test.xml        # Logging config (optional)
```

### Example `application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  security:
    user:
      name: testuser
      password: testpass

logging:
  level:
    com.taskmanagement: DEBUG
```

---

## Best Practices

### ✅ DO:
- Test behavior, not implementation
- Use descriptive test method names
- Follow AAA pattern (Arrange, Act, Assert)
- Mock external dependencies
- Test edge cases and error scenarios
- Keep tests independent and isolated
- Use `@BeforeEach` for common setup

### ❌ DON'T:
- Test framework code
- Mock the class under test
- Create interdependent tests
- Test multiple behaviors in one method
- Ignore test failures
- Write tests that depend on execution order

---

## Troubleshooting

### Tests fail with "No qualifying bean" error
- Ensure you're using `@WebMvcTest` for controllers (not `@SpringBootTest`)
- Add `@MockBean` for all service dependencies

### Tests fail with database errors
- Check `application-test.yml` configuration
- Ensure H2 dependency is in `test` scope

### Mocks not working as expected
- Verify `@ExtendWith(MockitoExtension.class)` is present
- Check mock setup with `when().thenReturn()`
- Use `verify()` to check method calls

---

## Dependencies

### Required (already in `spring-boot-starter-test`):
- JUnit 5
- Mockito
- AssertJ
- Hamcrest
- Spring Test

### Optional:
- H2 Database (for repository tests)
- Testcontainers (for integration tests)

---

## Next Steps

1. **Start with Service Tests:** Core business logic testing
2. **Add Controller Tests:** API endpoint validation
3. **Implement Exception Handler Tests:** Error response validation
4. **Create Repository Tests:** Only if custom queries exist
5. **Build Test Utilities:** TestDataBuilder and TestConstants

---

## References

- [Spring Boot Testing Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
