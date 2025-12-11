package com.taskmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Configuration
 * Định nghĩa authentication và authorization cho API
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configure HTTP security
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {});
        
        return http.build();
    }

    /**
     * Định nghĩa users trong memory
     * ============================================
     * THAY ĐỔI USERNAME/PASSWORD TẠI ĐÂY
     * ============================================
     */
    @Bean
    public UserDetailsService userDetailsService() {
        // User 1: Admin (full access)
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
        
        // User 3: Test user (matches database)
        UserDetails john = User.builder()
            .username("john")         
            .password(passwordEncoder().encode("john"))  
            .roles("USER")
            .build();

        return new InMemoryUserDetailsManager(admin, user, john);
    }

    /**
     * Password encoder (BCrypt)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
