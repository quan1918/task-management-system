package com.taskmanagement.security;

import com.taskmanagement.entity.User;
import com.taskmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CustomUserDetailsService - Spring Security UserDetailsService implementation
 * 
 * Responsibilities:
 * 1. Load user từ database by username
 * 2. Convert User entity → Spring Security UserDetails
 * 3. Map user roles → GrantedAuthority objects
 * 4. Handle deleted/inactive users
 * 
 * Flow:
 * 1. Spring Security call loadUserByUsername(username)
 * 2. Query database via UserRepository
 * 3. Check if user exists and active
 * 4. Convert User → UserDetails (với roles)
 * 5. Return UserDetails to Spring Security
 * 6. Spring Security compare password và authenticate
 * 
 * Security Notes:
 * - Only load ACTIVE users (deleted = false)
 * - Only load ENABLED users (active = true)
 * - Throw UsernameNotFoundException nếu user không tồn tại
 * - Roles được map từ User.roles (comma-separated string)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;

    /**
     * loadUserByUsername - Load user từ database by username
     * 
     * Called by Spring Security khi:
     * - User login (authenticate)
     * - Validate JWT token (load user từ token)
     * Example:
     * UserDetails user = userDetailsService.loadUserByUsername("john_doe");
     * // user.getUsername() → "john_doe"
     * // user.getAuthorities() → [ROLE_ADMIN, ROLE_USER]
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        // 1. Query database để tìm user by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        // 2.  Kiểm tra nếu user is active
        if (!user.isActive()) {
            log.error("User is not active: {}", username);
            throw new UsernameNotFoundException("User is not active: " + username);
        }

        return UserPrincipal.from(user);
    }
}
