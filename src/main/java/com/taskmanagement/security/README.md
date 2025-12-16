# Security Layer

## 📋 Status: NOT IMPLEMENTED

**Location:** `src/main/java/com/taskmanagement/security/`

**Current State:** Empty folder - no implementation files

---

## 🔲 Planned Features

This folder is reserved for future JWT authentication implementation:

### Phase 1: JWT Components
- `JwtTokenProvider.java` - Generate and validate JWT tokens
- `JwtAuthenticationFilter.java` - Filter to validate tokens on each request
- `JwtAuthenticationEntryPoint.java` - Handle authentication errors

### Phase 2: Custom Authentication
- `CustomUserDetailsService.java` - Load users from database
- `AuthenticationService.java` - Login/logout business logic
- `TokenRefreshService.java` - Refresh token handling

### Phase 3: Authorization
- `SecurityExpressionRoot.java` - Custom authorization expressions
- `PermissionEvaluator.java` - Complex permission checks
- `RoleHierarchy.java` - Role inheritance

---

## 📝 Current Security Implementation

Security is currently configured in:
- [SecurityConfig.java](../config/SecurityConfig.java) - HTTP Basic Auth
- [config/README.md](../config/README.md) - Security documentation

**Authentication:** HTTP Basic Auth with hardcoded users  
**Authorization:** Not implemented (all authenticated users have same access)

---

## 🔮 Future Implementation

When JWT is implemented, this folder will contain:

```
security/
├── JwtTokenProvider.java
├── JwtAuthenticationFilter.java
├── JwtAuthenticationEntryPoint.java
├── CustomUserDetailsService.java
└── README.md
```

See [SecurityConfig README](../config/README.md) for current security setup and future plans.

---

**Last Updated:** December 15, 2025  
**Version:** 0.5.0 - MVP Phase  
**Status:** Placeholder - awaiting JWT implementation
