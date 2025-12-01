# Security Layer (Authentication & Authorization)

##  Overview

The **Security layer** implements authentication and authorization for the application using Spring Security 6.x and JWT (JSON Web Tokens). It provides a secure mechanism for user authentication, token generation, validation, and role-based access control.

**Location:** \src/main/java/com/taskmanagement/security/\

**Responsibility:** Implement JWT token handling, authenticate users, validate credentials, and enforce access control policies

---

##  Core Responsibilities

### 1. User Authentication
- Validate user credentials against stored data
- Support username/password authentication
- Prevent unauthorized access to protected endpoints
- Track authentication state and sessions

### 2. JWT Token Management
- Generate secure JWT tokens upon successful login
- Validate JWT tokens on each request
- Extract user information from tokens
- Implement token expiration and refresh mechanisms

### 3. Authorization & Access Control
- Define role-based access control (RBAC)
- Implement permission-based authorization
- Protect endpoints with security rules
- Support method-level security annotations

### 4. Security Configuration
- Configure Spring Security filters and chains
- Setup password encoding and hashing
- Configure CORS for cross-origin requests
- Handle security exceptions and errors

### 5. Token Security
- Implement secure token signing (HMAC SHA-256)
- Add claims and metadata to tokens
- Handle token refresh and revocation
- Prevent token tampering and forgery

---

##  Folder Structure

\\\
security/
 jwt/
    JwtTokenProvider.java              # JWT token generation and validation
    JwtTokenRequest.java               # Login credentials request
    JwtTokenResponse.java              # Token response DTO
    JwtException.java                  # JWT-specific exceptions
    JwtProperties.java                 # JWT configuration properties

 filter/
    JwtAuthenticationFilter.java       # JWT extraction and validation filter
    CorsConfigurationFilter.java       # CORS configuration filter
    SecurityContextFilter.java         # Security context setup filter

 config/
    SecurityConfig.java                # Spring Security configuration
    JwtSecurityConfig.java             # JWT-specific configuration
    CorsConfig.java                    # CORS configuration
    PasswordEncoderConfig.java         # Password encoder setup

 service/
    AuthenticationService.java         # Authentication business logic
    UserDetailsServiceImpl.java         # Custom UserDetailsService
    SecurityUserDetailsImpl.java        # User details implementation

 controller/
    AuthenticationController.java      # Login and token endpoints
    AuthenticationRequest.java         # Login request DTO
    AuthenticationResponse.java        # Login response DTO

 util/
    SecurityUtils.java                 # Security utility methods
    TokenUtils.java                    # Token utility methods

 README.md                             # This file
\\\

---

##  JWT Token Provider

\\\java
/**
 * JWT Token Provider
 * Handles token generation, validation, and extraction of user information
 */
@Component
@Slf4j
public class JwtTokenProvider {
    
    @Value("\")
    private String jwtSecret;
    
    @Value("\")  // Default: 24 hours
    private int jwtExpirationMs;
    
    @Value("\")  // Default: 7 days
    private int jwtRefreshExpirationMs;
    
    /**
     * Generate JWT token from user details
     * 
     * @param authentication Spring Security Authentication object
     * @return JWT token string
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .claim("authorities", getAuthoritiesAsString(userDetails))
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    /**
     * Generate JWT token from username
     */
    public String generateTokenFromUsername(String username, Collection<String> authorities) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", authorities);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    /**
     * Generate refresh token (longer expiration)
     */
    public String generateRefreshToken(String username) {
        return Jwts.builder()
            .setSubject(username)
            .claim("tokenType", "REFRESH")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtRefreshExpirationMs))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    /**
     * Validate JWT token signature and expiration
     * 
     * @param token JWT token string
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Extract username from JWT token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        
        return claims.getSubject();
    }
    
    /**
     * Extract all claims from JWT token
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
    }
    
    /**
     * Extract authorities/roles from token
     */
    @SuppressWarnings("unchecked")
    public Collection<String> getAuthoritiesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object authorities = claims.get("authorities");
        
        if (authorities instanceof Collection) {
            return (Collection<String>) authorities;
        }
        return Collections.emptyList();
    }
    
    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
    
    /**
     * Get token expiration time
     */
    public Date getTokenExpiration(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }
    
    /**
     * Get remaining time until token expiration (in milliseconds)
     */
    public long getTokenTimeToLive(String token) {
        Date expiration = getTokenExpiration(token);
        return expiration.getTime() - System.currentTimeMillis();
    }
    
    /**
     * Extract authorities from UserDetails as string representation
     */
    private String getAuthoritiesAsString(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));
    }
}
\\\

---

##  JWT Authentication Filter

\\\java
/**
 * JWT Authentication Filter
 * Intercepts requests, extracts JWT token, and validates it
 * Sets authentication in Spring Security context
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // Extract JWT token from Authorization header
            String jwt = getJwtFromRequest(request);
            
            if (jwt != null && tokenProvider.validateToken(jwt)) {
                // Token is valid, set authentication in security context
                String username = tokenProvider.getUsernameFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // Build authentication token
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                
                // Set request attribute for correlation ID tracking
                Collection<String> authorities = tokenProvider.getAuthoritiesFromToken(jwt);
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // Store authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("JWT authenticated user: {}", username);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication", e);
            // Continue filter chain - endpoint will handle unauthorized request
        }
        
        // Continue filter chain
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from Authorization header
     * Expected format: "Bearer <token>"
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
\\\

---

##  Spring Security Configuration

\\\java
/**
 * Spring Security Configuration
 * Configures security filters, authentication, and authorization rules
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * Configure password encoder
     * Uses BCrypt for secure password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * Configure authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) 
            throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    /**
     * Configure DaoAuthenticationProvider
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
    
    /**
     * Configure security filter chain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless JWT authentication
            .csrf().disable()
            
            // Disable form login, use JWT instead
            .formLogin().disable()
            
            // Disable HTTP Basic authentication
            .httpBasic().disable()
            
            // Set exception handling
            .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint())
                .accessDeniedHandler(jwtAccessDeniedHandler())
            .and()
            
            // Set session management to stateless (no session cookies)
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            
            // Configure endpoint security
            .authorizeRequests()
                // Public endpoints
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/public/**").permitAll()
                .antMatchers("/actuator/health").permitAll()
                
                // Swagger/API documentation
                .antMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                
                // Protected endpoints
                .antMatchers("/api/tasks/**").authenticated()
                .antMatchers("/api/projects/**").authenticated()
                .antMatchers("/api/users/**").authenticated()
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            .and()
            
            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    /**
     * JWT authentication entry point for 401 responses
     */
    @Bean
    public AuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            log.error("Unauthorized access: {}", authException.getMessage());
            
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            
            ErrorResponse errorResponse = ErrorResponse.builder()
                .code("UNAUTHORIZED")
                .message("Authentication required")
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
                .path(request.getRequestURI())
                .build();
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getOutputStream(), errorResponse);
        };
    }
    
    /**
     * JWT access denied handler for 403 responses
     */
    @Bean
    public AccessDeniedHandler jwtAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.error("Access denied: {}", accessDeniedException.getMessage());
            
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            
            ErrorResponse errorResponse = ErrorResponse.builder()
                .code("FORBIDDEN")
                .message("Insufficient permissions")
                .status(HttpServletResponse.SC_FORBIDDEN)
                .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
                .path(request.getRequestURI())
                .build();
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getOutputStream(), errorResponse);
        };
    }
    
    /**
     * Configure CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "https://yourdomain.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
\\\

---

##  Authentication Service

\\\java
/**
 * Authentication Service
 * Handles user login and token generation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    
    /**
     * Authenticate user with credentials and return JWT token
     * 
     * @param request Login credentials
     * @return Token response with JWT and refresh token
     * @throws InvalidCredentialsException if credentials are invalid
     */
    @Transactional(readOnly = true)
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            // Authenticate using Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );
            
            // Generate JWT token
            String token = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(request.getUsername());
            
            // Get user details
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException(request.getUsername()));
            
            // Log successful authentication
            log.info("User authenticated successfully: {}", request.getUsername());
            
            return AuthenticationResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .type("Bearer")
                .expiresIn(86400)  // 24 hours in seconds
                .user(UserResponse.from(user))
                .build();
            
        } catch (BadCredentialsException e) {
            log.warn("Failed authentication attempt for user: {}", request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        } catch (UsernameNotFoundException e) {
            log.warn("Authentication failed: user not found: {}", request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }
    
    /**
     * Refresh JWT token using refresh token
     */
    @Transactional(readOnly = true)
    public AuthenticationResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        
        String username = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException(username));
        
        // Generate new token
        Collection<String> authorities = user.getRoles().stream()
            .map(role -> "ROLE_" + role.getName().toUpperCase())
            .collect(Collectors.toList());
        
        String newToken = tokenProvider.generateTokenFromUsername(username, authorities);
        
        return AuthenticationResponse.builder()
            .token(newToken)
            .refreshToken(refreshToken)
            .type("Bearer")
            .expiresIn(86400)
            .user(UserResponse.from(user))
            .build();
    }
}
\\\

---

##  Custom UserDetailsService

\\\java
/**
 * Custom UserDetailsService implementation
 * Loads user details from database for authentication
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    /**
     * Load user by username
     * Spring Security calls this during authentication
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.error("User not found: {}", username);
                return new UsernameNotFoundException("User not found: " + username);
            });
        
        return SecurityUserDetails.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .password(user.getPassword())
            .authorities(buildAuthorities(user))
            .accountNonExpired(user.isActive())
            .accountNonLocked(!user.isLocked())
            .credentialsNonExpired(true)
            .enabled(user.isActive())
            .build();
    }
    
    /**
     * Build granted authorities from user roles
     */
    private Collection<? extends GrantedAuthority> buildAuthorities(User user) {
        return user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()))
            .collect(Collectors.toList());
    }
}

/**
 * Custom UserDetails implementation
 * Extends User with additional fields
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SecurityUserDetails implements UserDetails {
    
    private Long id;
    private String username;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean enabled;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
\\\

---

##  Authentication Controller

\\\java
/**
 * Authentication Controller
 * Handles login and token endpoints
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {
    
    private final AuthenticationService authenticationService;
    
    /**
     * User login endpoint
     * POST /api/auth/login
     * 
     * @param request Login credentials (username and password)
     * @return JWT token and user information
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
        @Valid @RequestBody AuthenticationRequest request
    ) {
        log.info("Login attempt for user: {}", request.getUsername());
        
        AuthenticationResponse response = authenticationService.authenticate(request);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Refresh token endpoint
     * POST /api/auth/refresh
     * 
     * @param refreshToken The refresh token
     * @return New JWT token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(
        @RequestHeader("X-Refresh-Token") String refreshToken
    ) {
        AuthenticationResponse response = authenticationService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get current user information
     * GET /api/auth/me
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser(
        @AuthenticationPrincipal SecurityUserDetails userDetails
    ) {
        return ResponseEntity.ok(UserResponse.builder()
            .id(userDetails.getId())
            .username(userDetails.getUsername())
            .email(userDetails.getEmail())
            .build());
    }
    
    /**
     * Logout endpoint (optional - for client-side cleanup)
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout() {
        SecurityContextHolder.clearContext();
        log.info("User logged out");
        return ResponseEntity.ok().build();
    }
}
\\\

---

##  DTOs for Authentication

\\\java
/**
 * Authentication Request DTO
 * Used for login endpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
}

/**
 * Authentication Response DTO
 * Returned after successful login
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationResponse {
    
    private String token;
    private String refreshToken;
    private String type;
    private long expiresIn;
    private UserResponse user;
}

/**
 * Token Refresh Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequest {
    
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
\\\

---

##  Security Configuration in application.yml

\\\yaml
# JWT Configuration
app:
  jwtSecret: \
  jwtExpirationMs: 86400000       # 24 hours
  jwtRefreshExpirationMs: 604800000  # 7 days

# Security settings
spring:
  security:
    user:
      name: admin
      password: changeme
  
  # CORS configuration
  web:
    cors:
      allowed-origins: http://localhost:3000,https://yourdomain.com
      allowed-methods: GET,POST,PUT,DELETE,OPTIONS
      allowed-headers: '*'
      max-age: 3600
\\\

---

##  Method-Level Security

\\\java
/**
 * Examples of method-level security annotations
 */
@Service
@Slf4j
public class TaskService {
    
    /**
     * Requires ADMIN role
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteAllTasks() {
        // Implementation
    }
    
    /**
     * Requires authentication
     */
    @PreAuthorize("isAuthenticated()")
    public Task getMyTask(Long taskId) {
        // Implementation
    }
    
    /**
     * Check if user is the task owner or has ADMIN role
     */
    @PreAuthorize("@taskService.isTaskOwner(#taskId, authentication.principal.id) or hasRole('ADMIN')")
    public Task updateTask(Long taskId, UpdateTaskRequest request) {
        // Implementation
    }
    
    /**
     * Custom permission evaluation
     */
    @PreAuthorize("@taskService.canEditTask(#taskId, authentication.principal)")
    public void editTask(Long taskId, UpdateTaskRequest request) {
        // Implementation
    }
}
\\\

---

##  Security Best Practices

### 1. Store JWT Secret Securely

\\\yaml
# GOOD: Use environment variable
app:
  jwtSecret: \

# BAD: Hardcoding secret in code
app:
  jwtSecret: "super-secret-key-in-source"
\\\

### 2. Use HTTPS in Production

\\\java
// GOOD: Enforce HTTPS in production
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .requiresChannel()
                .anyRequest()
                .requiresSecure();  // Force HTTPS
        return http.build();
    }
}

// BAD: Not enforcing HTTPS
\\\

### 3. Set Secure Cookie Flags

\\\java
// GOOD: Configure secure session cookies
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .sessionManagement()
            .sessionFixationProtection(SessionFixationProtectionStrategy.MIGRATE_SESSION)
        .and()
        .rememberMe()
            .tokenValiditySeconds(86400)
            .useSecureCookie(true);
    
    return http.build();
}
\\\

### 4. Implement Rate Limiting

\\\java
/**
 * Rate limiting for login attempts
 */
@Component
@Slf4j
public class LoginAttemptService {
    
    private final int MAX_ATTEMPTS = 5;
    private final int ATTEMPT_INCREMENT = 1;
    private final long LOCK_TIME_DURATION = 24 * 60 * 60 * 1000; // 24 hours
    
    private LoadingCache<String, AttemptCounter> attemptsCache;
    
    @PostConstruct
    public void init() {
        attemptsCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<String, AttemptCounter>() {
                @Override
                public AttemptCounter load(String key) {
                    return new AttemptCounter();
                }
            });
    }
    
    public void loginSucceeded(String username) {
        attemptsCache.invalidate(username);
    }
    
    public void loginFailed(String username) {
        AttemptCounter counter = attemptsCache.getUnchecked(username);
        counter.increment();
    }
    
    public boolean isBlocked(String username) {
        return attemptsCache.getUnchecked(username).getAttempts() >= MAX_ATTEMPTS;
    }
    
    @Data
    private static class AttemptCounter {
        private int attempts = 0;
        private LocalDateTime lastAttempt;
        
        void increment() {
            this.attempts++;
            this.lastAttempt = LocalDateTime.now();
        }
    }
}
\\\

### 5. Validate Input Strictly

\\\java
// GOOD: Strict validation
@PostMapping("/login")
public ResponseEntity<AuthenticationResponse> login(
    @Valid @RequestBody AuthenticationRequest request
) {
    // Validation happens automatically
    return authenticationService.authenticate(request);
}

// BAD: No validation
@PostMapping("/login")
public ResponseEntity<AuthenticationResponse> login(
    @RequestBody AuthenticationRequest request
) {
    // Could receive null or invalid data
    return authenticationService.authenticate(request);
}
\\\

### 6. Use BCrypt for Password Hashing

\\\java
// GOOD: Use BCryptPasswordEncoder
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // 12 is the strength
}

// BAD: Storing passwords in plain text or weak hashing
public PasswordEncoder passwordEncoder() {
    return NoOpPasswordEncoder.getInstance();
}
\\\

### 7. Implement Token Expiration

\\\java
// GOOD: Set appropriate expiration times
jwtExpirationMs: 86400000       // 24 hours for access token
jwtRefreshExpirationMs: 604800000  // 7 days for refresh token

// BAD: Tokens that never expire
jwtExpirationMs: 999999999999   // Effectively never expires
\\\

### 8. Log Security Events

\\\java
// GOOD: Log all authentication events
@Service
@Slf4j
public class AuthenticationService {
    
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        
        try {
            Authentication authentication = authenticationManager.authenticate(...);
            log.info("User authenticated successfully: {}", request.getUsername());
            return response;
        } catch (Exception e) {
            log.warn("Failed authentication attempt for user: {}", request.getUsername());
            throw e;
        }
    }
}

// BAD: No security logging
\\\

---

##  Testing Security

### Controller Security Tests

\\\java
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AuthenticationService authenticationService;
    
    @Test
    void login_WithValidCredentials_ReturnsToken() throws Exception {
        AuthenticationRequest request = AuthenticationRequest.builder()
            .username("user@example.com")
            .password("password123")
            .build();
        
        AuthenticationResponse response = AuthenticationResponse.builder()
            .token("jwt-token")
            .refreshToken("refresh-token")
            .type("Bearer")
            .build();
        
        when(authenticationService.authenticate(request))
            .thenReturn(response);
        
        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-token"));
    }
    
    @Test
    void login_WithInvalidCredentials_Returns401() throws Exception {
        AuthenticationRequest request = AuthenticationRequest.builder()
            .username("user@example.com")
            .password("wrongpassword")
            .build();
        
        when(authenticationService.authenticate(request))
            .thenThrow(new InvalidCredentialsException("Invalid credentials"));
        
        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }
}
\\\

### Endpoint Security Tests

\\\java
@SpringBootTest
@AutoConfigureMockMvc
class EndpointSecurityTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void getProtectedEndpoint_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void getProtectedEndpoint_WithInvalidToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/tasks")
            .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @WithMockUser(username = "user", roles = "USER")
    void getProtectedEndpoint_WithValidToken_Returns200() throws Exception {
        mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(username = "user", roles = "USER")
    void getAdminEndpoint_WithUserRole_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isForbidden());
    }
}
\\\

---

##  Security Checklist

When implementing security:

- [ ] Configure Spring Security with appropriate filter chain
- [ ] Implement JWT token generation and validation
- [ ] Use BCrypt or scrypt for password hashing
- [ ] Set token expiration times (short for access, longer for refresh)
- [ ] Implement rate limiting for login attempts
- [ ] Configure CORS for allowed origins
- [ ] Use HTTPS in production
- [ ] Set secure cookie flags (httpOnly, secure, sameSite)
- [ ] Implement method-level security with @PreAuthorize
- [ ] Log all authentication and authorization events
- [ ] Store JWT secret in environment variables
- [ ] Validate all input strictly
- [ ] Implement token refresh mechanism
- [ ] Test security with @WithMockUser and MockMvc
- [ ] Document API security requirements

---

##  Related Documentation

- **ARCHITECTURE.md** - Security architecture
- **README.md** - Main project overview
- **Configuration Layer** - Security configuration
- **Exception Layer** - Error handling for security exceptions

---

##  Quick Reference

### Common Spring Security Annotations

\\\
@PreAuthorize("isAuthenticated()")              - Requires authentication
@PreAuthorize("hasRole('ADMIN')")               - Requires ADMIN role
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")   - Requires one of the roles
@PreAuthorize("hasAuthority('READ_TASKS')")    - Requires specific permission
@PostAuthorize("returnObject.owner == authentication.principal.id")  - Post-execution check
@Secured("ROLE_ADMIN")                         - Legacy secured annotation
@RolesAllowed("ADMIN")                         - JSR-250 annotation
\\\

### JWT Token Structure

\\\
Header.Payload.Signature

Header:   { "alg": "HS512", "typ": "JWT" }
Payload:  { "sub": "username", "authorities": [...], "iat": ..., "exp": ... }
Signature: HMACSHA512(base64(header).base64(payload), secret)
\\\

### HTTP Status Codes for Security

\\\
200 OK                 - Authenticated and authorized
401 Unauthorized       - Missing or invalid credentials
403 Forbidden          - Insufficient permissions
405 Method Not Allowed - HTTP method not allowed
422 Unprocessable      - Invalid request format
\\\

---

**Last Updated:** December 1, 2025  
**Version:** 1.0.0  
**Status:** Complete
