# Configuration Layer

## 📋 Overview

**Purpose:** Application infrastructure configuration including security, database, and framework setup.

**Location:** `src/main/java/com/taskmanagement/config/`

**Current Status:**  
✅ **MVP Phase** - Basic Security with HTTP Basic Auth  
🔲 **Future** - JWT authentication, CORS, Swagger/OpenAPI

---

## 📁 Current Structure

```
config/
├── SecurityConfig.java      # ✅ Spring Security with Basic Auth
└── README.md               # This file
```

---

## 🎯 Core Responsibilities

### ✅ Currently Implemented

1. **Basic Authentication**
   - HTTP Basic Auth (username/password)
   - BCrypt password encoding
   - In-memory user store (3 hardcoded users)

2. **Security Rules**
   - All endpoints require authentication (except /actuator/health)
   - CSRF disabled (for REST API)

### 🔲 Not Yet Implemented

- JWT token-based authentication
- Role-based access control (authorization)
- Database-backed user authentication
- CORS configuration
- API documentation (Swagger/OpenAPI)
- Custom authentication filters
- Session management

---

## SecurityConfig

**Location:** [SecurityConfig.java](SecurityConfig.java)

**Purpose:** Configure Spring Security for API authentication

### Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())  // Disable CSRF for REST API
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()  // Health check public
                .anyRequest().authenticated()  // All other endpoints require auth
            )
            .httpBasic(basic -> {});  // Enable HTTP Basic Auth
        
        return http.build();
    }
}
```

### Security Rules

| Endpoint | Authentication | Notes |
|----------|---------------|-------|
| `GET /actuator/health` | ❌ Not required | Public health check |
| `POST /api/tasks` | ✅ Required | Create task |
| `GET /api/tasks/{id}` | ✅ Required | Get task |
| `PUT /api/tasks/{id}` | ✅ Required | Update task |
| `DELETE /api/tasks/{id}` | ✅ Required | Delete task |

### Authentication Type

**HTTP Basic Authentication:**
```
Authorization: Basic base64(username:password)
```

**Example:**
```bash
# Username: admin, Password: admin
curl -u admin:admin http://localhost:8080/api/tasks/1

# Or with header:
curl -H "Authorization: Basic YWRtaW46YWRtaW4=" http://localhost:8080/api/tasks/1
```

### User Store

**In-Memory Users (Hardcoded):**

```java
@Bean
public UserDetailsService userDetailsService() {
    // User 1: Admin
    UserDetails admin = User.builder()
        .username("admin")
        .password(passwordEncoder().encode("admin"))
        .roles("ADMIN", "USER")
        .build();
    
    // User 2: Regular user
    UserDetails user = User.builder()
        .username("user")
        .password(passwordEncoder().encode("user"))
        .roles("USER")
        .build();
    
    // User 3: Test user
    UserDetails john = User.builder()
        .username("john")
        .password(passwordEncoder().encode("john"))
        .roles("USER")
        .build();
    
    return new InMemoryUserDetailsManager(admin, user, john);
}
```

**Available Test Users:**

| Username | Password | Roles | Notes |
|----------|----------|-------|-------|
| `admin` | `admin` | ADMIN, USER | Full access |
| `user` | `user` | USER | Standard access |
| `john` | `john` | USER | Test user |

**⚠️ Important:** These are hardcoded for testing only. Change before production!

### Password Encoding

**BCrypt:**
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**How it works:**
- Passwords stored hashed (not plaintext)
- BCrypt uses salt for each password
- Computationally expensive (prevents brute force)
- Industry standard for password hashing

---

## 🔒 Security Features

### 1. CSRF Disabled

**Why disabled:**
- REST API with stateless authentication
- No session cookies used
- JWT (future) will be stateless

**Code:**
```java
.csrf(csrf -> csrf.disable())
```

**⚠️ Warning:** Don't disable CSRF for browser-based apps with cookies!

### 2. HTTP Basic Auth

**How it works:**
1. Client sends request with `Authorization` header
2. Spring Security decodes base64 credentials
3. Checks against UserDetailsService
4. Validates password with BCrypt
5. Returns 401 if invalid, 200 if valid

**Request flow:**
```
Client → Authorization: Basic YWRtaW46YWRtaW4=
         ↓
Spring Security Filter
         ↓
BCryptPasswordEncoder.matches()
         ↓
Authentication successful → Controller
         ↓
Response
```

### 3. Password Hashing

**Example:**
```java
String plainPassword = "admin";
String hashedPassword = passwordEncoder.encode(plainPassword);
// $2a$10$N8LGr...  (60 chars)

// Validation:
boolean matches = passwordEncoder.matches("admin", hashedPassword);
// true
```

---

## 🧪 Testing Authentication

### Using cURL

```bash
# With -u flag (recommended)
curl -u admin:admin http://localhost:8080/api/tasks/1

# With Authorization header
curl -H "Authorization: Basic YWRtaW46YWRtaW4=" \
     http://localhost:8080/api/tasks/1

# Wrong credentials (401 Unauthorized)
curl -u admin:wrong http://localhost:8080/api/tasks/1
```

### Using Postman

1. **Authorization tab**
   - Type: Basic Auth
   - Username: `admin`
   - Password: `admin`

2. **Or Headers tab**
   - Key: `Authorization`
   - Value: `Basic YWRtaW46YWRtaW4=`

### Using HTTPie

```bash
# Simple syntax
http -a admin:admin GET localhost:8080/api/tasks/1

# Or explicit header
http GET localhost:8080/api/tasks/1 \
     "Authorization: Basic YWRtaW46YWRtaW4="
```

---

## ⚠️ Known Limitations

### 1. No JWT Authentication

**Current:** HTTP Basic Auth (username/password in every request)

**Issues:**
- Credentials sent with every request
- No token expiration
- No refresh tokens
- Less secure for production

**Future:** JWT tokens
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 2. No Role-Based Access Control

**Current:** All authenticated users have same permissions

**Missing:**
```java
// Can't do this yet:
@PreAuthorize("hasRole('ADMIN')")
public void deleteTask(Long id) { ... }

// Or:
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.requestMatchers("/api/tasks/**").hasAnyRole("USER", "ADMIN")
```

**Impact:** Any authenticated user can perform any operation

### 3. Hardcoded Users

**Current:** 3 users defined in code

**Issues:**
- Can't add users at runtime
- No user registration
- No user management
- Not production-ready

**Future:** Database-backed authentication
```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            user.getAuthorities()
        );
    }
}
```

### 4. No CORS Configuration

**Current:** CORS not configured

**Issue:** Frontend apps from different origin will be blocked

**Future:**
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### 5. No API Documentation

**Missing:** Swagger/OpenAPI configuration

**Future:**
```java
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Task Management API")
                .version("1.0"));
    }
}
```

---

## 🔐 Security Best Practices

### ✅ Do's

```java
// Use password encoder
passwordEncoder().encode("password")

// Disable CSRF for stateless REST API
.csrf(csrf -> csrf.disable())

// Require authentication by default
.anyRequest().authenticated()

// Use HTTPS in production
server.ssl.enabled=true
```

### ❌ Don'ts

```java
// Don't hardcode passwords in production
.password("admin")  // ❌

// Don't disable security entirely
http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())  // ❌

// Don't store plaintext passwords
user.setPassword("plaintext")  // ❌

// Don't use HTTP Basic Auth in production without HTTPS
// Credentials sent in base64 (easily decoded)
```

---

## 🔮 Planned Enhancements

### Phase 1: JWT Authentication

```java
@Configuration
public class JwtSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        return http.build();
    }
}

// Login endpoint returns JWT
POST /api/auth/login
{
  "username": "admin",
  "password": "admin"
}
→ 
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600
}

// Subsequent requests use token
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Phase 2: Role-Based Access Control

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers(POST, "/api/tasks").hasAnyRole("USER", "ADMIN")
            .requestMatchers(GET, "/api/tasks/**").authenticated()
            .requestMatchers(PUT, "/api/tasks/**").hasAnyRole("USER", "ADMIN")
            .requestMatchers(DELETE, "/api/tasks/**").hasRole("ADMIN")
        );
        return http.build();
    }
}

// In controllers:
@PreAuthorize("hasRole('ADMIN')")
public void deleteTask(Long id) { ... }

@PreAuthorize("hasRole('ADMIN') or @taskSecurity.isOwner(#id)")
public void updateTask(Long id) { ... }
```

### Phase 3: Database Authentication

```java
@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));
        
        return org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword())
            .authorities(user.getRoles())
            .build();
    }
}
```

### Phase 4: CORS Configuration

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "https://app.example.com"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

---

## 📖 Related Documentation

- [TaskController](../api/README.md) - Endpoints requiring authentication
- [Exception Handling](../exception/README.md) - 401/403 error responses
- [Application Properties](../../resources/APPLICATION_YML_CONFIGURATION.md) - Security settings

---

**Last Updated:** December 15, 2025  
**Version:** 0.5.0 - MVP Phase  
**Status:** Basic Auth implemented, JWT and RBAC pending
