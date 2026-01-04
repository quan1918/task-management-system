/**
 * Security infrastructure for authentication and authorization in the Task Management System.
 * 
 * <h2>Current State (v0.7.0)</h2>
 * Security configuration exists in {@link com.taskmanagement.config.SecurityConfig} with Basic Authentication.
 * 
 * <h2>Planned Implementation (v1.0.0)</h2>
 * This package will contain JWT-based authentication and role-based authorization:
 * 
 * <h3>Authentication Components:</h3>
 * <ul>
 *   <li><b>JwtTokenProvider</b> - Generates and validates JWT tokens
 *     <ul>
 *       <li>createToken(username, roles) - Creates JWT with claims</li>
 *       <li>validateToken(token) - Validates signature and expiration</li>
 *       <li>getUsernameFromToken(token) - Extracts username from JWT</li>
 *     </ul>
 *   </li>
 *   <li><b>JwtAuthenticationFilter</b> - Intercepts requests to validate tokens
 *     <ul>
 *       <li>Extracts JWT from Authorization header</li>
 *       <li>Validates token and sets SecurityContext</li>
 *       <li>Handles expired or invalid tokens</li>
 *     </ul>
 *   </li>
 *   <li><b>CustomUserDetailsService</b> - Loads user from database
 *     <ul>
 *       <li>Implements UserDetailsService</li>
 *       <li>Loads user by username/email</li>
 *       <li>Maps User entity to Spring Security UserDetails</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h3>Authorization Components:</h3>
 * <ul>
 *   <li><b>RoleBasedAuthorizationManager</b> - Custom authorization logic
 *     <ul>
 *       <li>PROJECT_MANAGER - Can create/delete projects and tasks</li>
 *       <li>TEAM_LEAD - Can manage tasks in assigned projects</li>
 *       <li>DEVELOPER - Can update task status and add comments</li>
 *       <li>VIEWER - Read-only access to assigned projects</li>
 *     </ul>
 *   </li>
 *   <li><b>@HasProjectAccess</b> - Custom annotation for project-level security</li>
 *   <li><b>@HasTaskAccess</b> - Custom annotation for task-level security</li>
 * </ul>
 * 
 * <h3>Security Utilities:</h3>
 * <ul>
 *   <li><b>SecurityUtils</b> - Helper methods for security context
 *     <ul>
 *       <li>getCurrentUser() - Gets authenticated user</li>
 *       <li>hasRole(role) - Checks if user has role</li>
 *       <li>hasProjectAccess(projectId) - Checks project access</li>
 *     </ul>
 *   </li>
 *   <li><b>PasswordEncoderConfig</b> - BCrypt password encoder configuration</li>
 *   <li><b>CorsConfig</b> - CORS configuration for frontend</li>
 * </ul>
 * 
 * <h3>Authentication Flow:</h3>
 * <pre>
 * POST /api/auth/login
 * {
 *   "username": "john.doe",
 *   "password": "password123"
 * }
 * 
 * Response:
 * {
 *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "type": "Bearer",
 *   "expiresIn": 3600
 * }
 * 
 * Subsequent requests:
 * GET /api/tasks
 * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * </pre>
 * 
 * <h3>Security Features:</h3>
 * <ul>
 *   <li>JWT tokens with HMAC-SHA256 signature</li>
 *   <li>Token expiration (1 hour) and refresh mechanism</li>
 *   <li>Role-based access control (RBAC)</li>
 *   <li>Project-level permission checks</li>
 *   <li>Password encryption with BCrypt</li>
 *   <li>CSRF protection for non-API endpoints</li>
 *   <li>Rate limiting for authentication endpoints</li>
 * </ul>
 * 
 * <h3>Migration from Basic Auth:</h3>
 * <ol>
 *   <li>Add JWT dependencies to pom.xml (jjwt-api, jjwt-impl)</li>
 *   <li>Create JwtTokenProvider and JwtAuthenticationFilter</li>
 *   <li>Update SecurityConfig to use JWT filter chain</li>
 *   <li>Create AuthController with /login and /refresh endpoints</li>
 *   <li>Update frontend to store and send JWT tokens</li>
 *   <li>Add role fields to User entity and database</li>
 * </ol>
 * 
 * <h3>Dependencies:</h3>
 * <ul>
 *   <li>Spring Security 6.x</li>
 *   <li>io.jsonwebtoken:jjwt-api:0.12.x (JWT library)</li>
 *   <li>Spring Security Test (for testing)</li>
 * </ul>
 * 
 * @see com.taskmanagement.config.SecurityConfig
 * @see org.springframework.security.core.Authentication
 * @see org.springframework.security.core.userdetails.UserDetailsService
 * @since v0.7.0
 * @author Task Management Team
 * @version v1.0.0 (Planned)
 */
@com.taskmanagement.annotation.Planned(
    version = "v1.0.0",
    description = "JWT authentication and role-based authorization",
    ticket = "TM-200",
    priority = "HIGH"
)
package com.taskmanagement.security;
