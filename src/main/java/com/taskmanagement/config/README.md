# Configuration Layer

##  Overview

The **configuration layer** contains all Spring Boot configuration classes that set up the application infrastructure, security, database connectivity, API documentation, and cross-cutting concerns.

**Location:** \src/main/java/com/taskmanagement/config/\

**Responsibility:** Initialize and configure application components, beans, and framework integrations

---

##  Core Responsibilities

### 1. Framework Integration
- Configure Spring Data JPA and Hibernate
- Set up Spring Security with JWT authentication
- Register custom beans in the Spring container
- Configure Bean Validation

### 2. Security Setup
- JWT token provider and validation
- Password encryption (BCrypt)
- CORS (Cross-Origin Resource Sharing)
- Security filter chains

### 3. Database Configuration
- Repository component scanning
- Entity auditing setup
- Connection pooling
- Transaction management

### 4. API Documentation
- Swagger/OpenAPI configuration
- Interactive API documentation
- Security scheme definitions for JWT

### 5. Error Handling
- Global exception handler
- Standardized error response format
- HTTP status code mapping

### 6. Application Properties
- Environment-specific settings
- Profiles (dev, prod, test)
- Spring Boot configuration

---

##  Folder Structure

\\\
config/
 JpaConfig.java              # JPA/Hibernate & entity auditing
 OpenApiConfig.java          # Swagger/OpenAPI setup
 SecurityConfig.java         # Spring Security & JWT
 WebConfig.java              # CORS, content negotiation
 Jackson.Config.java         # JSON serialization settings
 README.md                   # This file
\\\

---

##  Key Concepts

### Configuration Classes

Configuration classes are Spring beans marked with \@Configuration\ that define how the application should be set up.

**Naming Convention:** \{Domain}Config\
- JpaConfig
- SecurityConfig
- OpenApiConfig
- WebConfig

**Anatomy of a Configuration Class:**

\\\java
@Configuration
public class JpaConfig {
    
    /**
     * Enable JPA repository component scanning
     * Scans com.taskmanagement.repository package for repository interfaces
     */
    @Bean
    public SomeFeature someFeature() {
        return new SomeFeature();
    }
}
\\\

---

##  JpaConfig - Database Configuration

**Purpose:** Configure JPA/Hibernate, repository scanning, and entity auditing

**Responsibilities:**
- Enable \@Repository\ component scanning
- Configure entity auditing (\@CreatedDate\, \@LastModifiedDate\)
- Set up JPA repository base packages
- Register custom repository implementations

**Example:**

\\\java
@Configuration
@EnableJpaRepositories(basePackages = "com.taskmanagement.repository")
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaConfig {
    
    /**
     * Provides audit information (who created/modified records)
     * Used by JPA auditing to populate @CreatedBy, @LastModifiedBy
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> SecurityContextHolder.getContext()
            .getAuthentication()
            .map(auth -> auth.getName())
            .stream()
            .findFirst();
    }
}
\\\

**Key Features:**
- \@EnableJpaRepositories\ - Scans for repository interfaces
- \@EnableJpaAuditing\ - Enables audit field population
- \AuditorAware\ - Tracks who created/modified records
- Entity lifecycle management

---

##  SecurityConfig - Spring Security Setup

**Purpose:** Configure JWT authentication, password encoding, and authorization

**Responsibilities:**
- Configure Spring Security filter chain
- Set session management (stateless)
- Define password encoder (BCrypt)
- Register authentication manager
- Configure authorization rules
- Set up CORS

**Example:**

\\\java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    
    /**
     * Configure HTTP security: stateless sessions, JWT filter, authorization
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()  // Stateless API (CSRF not needed)
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
            .authorizeRequests()
                .antMatchers("/api/auth/**").permitAll()  // Public endpoints
                .antMatchers("/api/swagger-ui/**", "/api/v3/api-docs/**").permitAll()  // Docs
                .anyRequest().authenticated()  // All others require auth
                .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling()
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                .accessDeniedHandler(new JwtAccessDeniedHandler());
        
        return http.build();
    }
    
    /**
     * Password encoder: BCrypt with strength 10
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
    
    /**
     * Authentication manager for user login
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * CORS configuration
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:3000", "http://localhost:4200")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}
\\\

**Key Components:**
- \@EnableWebSecurity\ - Activates Spring Security
- \@EnableMethodSecurity\ - Enables \@PreAuthorize\ on methods
- \SecurityFilterChain\ - HTTP security configuration
- \PasswordEncoder\ - BCrypt password hashing
- \AuthenticationManager\ - Handles login authentication
- \CORS\ - Cross-origin requests configuration

---

##  OpenApiConfig - API Documentation Setup

**Purpose:** Configure Swagger/OpenAPI 3.0 interactive documentation

**Responsibilities:**
- Define OpenAPI 3.0 schema
- Set up JWT Bearer authentication
- Configure API metadata (title, version, description)
- Define security schemes

**Example:**

\\\java
@Configuration
public class OpenApiConfig {
    
    /**
     * Define OpenAPI 3.0 specification
     */
    @Bean
    public OpenAPI taskManagementOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Task Management System API")
                .description("REST API for task management and team collaboration")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Development Team")
                    .email("dev@example.com")))
            .addServersItem(new Server()
                .url("http://localhost:8080")
                .description("Development Server"))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", 
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token")))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
\\\

**Key Features:**
- \OpenAPI\ - Main API specification
- \Info\ - API title, description, version
- \SecurityScheme\ - JWT Bearer definition
- \Server\ - Base URL configuration
- Interactive docs at \http://localhost:8080/api/swagger-ui.html\

---

##  WebConfig - Web & CORS Configuration

**Purpose:** Configure web layer, CORS, content negotiation, and request/response handling

**Responsibilities:**
- CORS (Cross-Origin Resource Sharing)
- Content type negotiation
- View resolution
- Interceptor registration
- Message converter configuration

**Example:**

\\\java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    /**
     * Configure CORS for API endpoints
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000", "http://localhost:4200")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("X-Total-Count", "X-Page-Number")
            .allowCredentials(true)
            .maxAge(3600);  // 1 hour
    }
    
    /**
     * Configure HTTP message converters
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter());
    }
    
    /**
     * Register interceptors for request/response processing
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoggingInterceptor())
            .addPathPatterns("/api/**");
    }
}
\\\

**Key Features:**
- CORS configuration for frontend access
- Allowed HTTP methods and headers
- Credentials support
- Cache duration settings

---

##  JacksonConfig - JSON Serialization

**Purpose:** Configure JSON serialization/deserialization behavior

**Responsibilities:**
- Date/time formatting
- Null value handling
- Case conversion
- Custom serializers

**Example:**

\\\java
@Configuration
public class JacksonConfig {
    
    /**
     * Customize ObjectMapper for JSON serialization
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Java 8+ date/time support
        mapper.registerModule(new JavaTimeModule());
        
        // Ignore unknown properties during deserialization
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Include non-null values only
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        // Format dates as ISO 8601
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }
}
\\\

**Key Features:**
- ISO 8601 date formatting
- Null value exclusion
- Unknown property handling
- Custom serialization rules

---

##  Exception Handling Configuration

**Purpose:** Centralized exception handling with standardized error responses

**Responsibilities:**
- Catch exceptions globally
- Map exceptions to HTTP status codes
- Format error responses
- Log errors for debugging

**Example:**

\\\java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Handle task not found (404)
     */
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFound(
            TaskNotFoundException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
            .code("TASK_NOT_FOUND")
            .message(ex.getMessage())
            .timestamp(System.currentTimeMillis())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(error);
    }
    
    /**
     * Handle validation errors (400)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        String message = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        ErrorResponse error = ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message(message)
            .timestamp(System.currentTimeMillis())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }
    
    /**
     * Handle generic exceptions (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
            .code("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred")
            .timestamp(System.currentTimeMillis())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }
}
\\\

**Key Features:**
- \@RestControllerAdvice\ - Global exception handler
- \@ExceptionHandler\ - Maps exceptions to responses
- Standardized error response format
- HTTP status code mapping

---

##  Application Configuration Files

### application.yml

The main configuration file with environment-specific profiles.

\\\yaml
spring:
  application:
    name: task-management-system
    version: 1.0.0
  
  jpa:
    hibernate:
      ddl-auto: validate  # Don't auto-create schema
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL15Dialect
        format_sql: true
  
  datasource:
    url: jdbc:postgresql://localhost:5432/task_management
    username: postgres
    password: postgres
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  
  security:
    jwt:
      secret: your-super-secret-key-change-in-production
      expiration: 86400000  # 24 hours
      refresh-expiration: 604800000  # 7 days

---
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: \
    username: \
    password: \
  security:
    jwt:
      secret: \
\\\

---

##  Profiles

### Development Profile (application-dev.yml)

\\\yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        use_sql_comments: true
  h2:
    console:
      enabled: true

logging:
  level:
    com.taskmanagement: DEBUG
    org.hibernate.SQL: DEBUG
\\\

### Production Profile (application-prod.yml)

\\\yaml
server:
  compression:
    enabled: true
    min-response-size: 1024
  
  ssl:
    enabled: true
    key-store: \
    key-store-password: \

logging:
  level:
    root: INFO
    com.taskmanagement: INFO
\\\

### Running with Profiles

\\\ash
# Development
java -jar app.jar --spring.profiles.active=dev

# Production
java -jar app.jar --spring.profiles.active=prod
\\\

---

##  Bean Lifecycle

Beans are created in this order:

1. **Application starts**  Spring Boot initializes
2. **Configuration classes loaded**  \@Bean\ methods executed
3. **Beans registered**  Available for injection
4. **Components autowired**  Dependencies injected
5. **Application ready**  Accept requests

**Example Flow:**

\\\
Spring Boot starts
  
Loads SecurityConfig
  
Creates PasswordEncoder bean
  
Creates AuthenticationManager bean
  
Creates JwtAuthenticationFilter bean
  
Loads JpaConfig
  
Creates AuditorAware bean
  
Application ready
  
Controllers receive requests
\\\

---

##  Testing Configurations

### Test Configuration Class

\\\java
@TestConfiguration
public class TestSecurityConfig {
    
    /**
     * Mock security configuration for tests
     */
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests().anyRequest().permitAll();
        return http.build();
    }
    
    @Bean
    public PasswordEncoder testPasswordEncoder() {
        return NoOpPasswordEncoder.getInstance();  // For testing only!
    }
}
\\\

### Using Test Configuration

\\\java
@SpringBootTest
@Import(TestSecurityConfig.class)
class TaskControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void test() {
        // Test without security filters
    }
}
\\\

---

##  Best Practices

### 1. Separate Concerns
Each configuration class handles one domain:

\\\java
// JpaConfig - Database only
@Configuration
@EnableJpaRepositories
public class JpaConfig { }

// SecurityConfig - Security only
@Configuration
@EnableWebSecurity
public class SecurityConfig { }

// OpenApiConfig - API docs only
@Configuration
public class OpenApiConfig { }
\\\

### 2. Use Environment Variables

\\\java
@Value("\")
private String jwtSecret;

@Value("\")
private String databaseUrl;
\\\

### 3. Document Configuration Purposes

\\\java
/**
 * JpaConfig configures:
 * - Repository component scanning
 * - Entity auditing (@CreatedDate, @LastModifiedDate)
 * - JPA lifecycle callbacks
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.taskmanagement.repository")
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaConfig {
    // Implementation
}
\\\

### 4. Order Bean Creation (Phase 2+)

\\\java
@Configuration
@Order(1)  // Load first
public class SecurityConfig { }

@Configuration
@Order(2)  // Load second
public class JpaConfig { }
\\\

### 5. Conditional Configuration (Phase 2+)

\\\java
@Configuration
@ConditionalOnProperty(name = "feature.caching.enabled", havingValue = "true")
public class CachingConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("users", "tasks");
    }
}
\\\

---

##  Debugging Configurations

### Log Bean Creation

Add to \pplication.yml\:

\\\yaml
logging:
  level:
    org.springframework: DEBUG
\\\

### List All Beans

\\\java
@Component
public class BeanLister implements ApplicationContextAware {
    
    @Override
    public void setApplicationContext(ApplicationContext context) {
        String[] beanNames = context.getBeanDefinitionNames();
        System.out.println("Registered Beans:");
        for (String name : beanNames) {
            System.out.println("  - " + name);
        }
    }
}
\\\

---

##  Configuration Checklist

When creating a new configuration class:

- [ ] Class marked with \@Configuration\
- [ ] Named \{Feature}Config\ following convention
- [ ] Extensive JavaDoc explaining purpose
- [ ] \@Bean\ methods return specific types
- [ ] Environment variables for sensitive values
- [ ] \@ConditionalOn*\ if conditionally loaded
- [ ] Profile-specific configurations in separate files
- [ ] Test configuration class for testing
- [ ] Error handling for initialization failures
- [ ] Logging for debugging

---

##  Related Documentation

- **ARCHITECTURE.md** - Overall system architecture
- **README.md** - Main project overview
- **application.yml** - Configuration properties explanation
- **Security Layer** - JWT and authentication implementation
- **Exception Handling** - Global error handling strategy

---

##  Configuration File Locations

\\\
src/main/resources/
 application.yml              # Main configuration
 application-dev.yml          # Development profile
 application-prod.yml         # Production profile
 application-test.yml         # Testing profile
 db/
    migration/               # Flyway migrations
        V1__initial_schema.sql
        V2__add_indexes.sql
 templates/                   # Email templates, etc.
\\\

---

##  Quick Reference

| Configuration | Purpose | File |
|--------------|---------|------|
| Database | JPA, Hibernate, Flyway | JpaConfig.java |
| Security | JWT, Auth, CORS | SecurityConfig.java |
| API Docs | Swagger/OpenAPI | OpenApiConfig.java |
| JSON | Serialization | JacksonConfig.java |
| Web | CORS, interceptors | WebConfig.java |
| Errors | Exception handling | GlobalExceptionHandler.java |

---

**Last Updated:** December 1, 2025  
**Version:** 1.0.0  
**Status:** Complete
