package com.taskmanagement.security;

import com.taskmanagement.entity.User;
import com.taskmanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_Success() {

        User user = User.builder()
                .id(1L)
                .username("john_doe")
                .passwordHash("$2a$10$hashedPassword")
                .email("john.doe@example.com")
                .roles("ROLE_USER")
                .active(true)
                .build();
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john_doe");

        assertNotNull(userDetails);
        assertEquals("john_doe", userDetails.getUsername());
        assertEquals("$2a$10$hashedPassword", userDetails.getPassword());
        assertEquals(1, userDetails.getAuthorities().size());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonLocked());

        verify(userRepository, times(1)).findByUsername("john_doe");
    }

    @Test
    void loadUserByUsername_UserNotFound() {

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("nonexistent");
        });
    }

    @Test
    void loadUserByUsername_UserDisabled() {
        
        User user = User.builder()
                .username("disabled_user")
                .passwordHash("$2a$10$hashedPassword")
                .active(false)
                .build();

        when(userRepository.findByUsername("disabled_user")).thenReturn(Optional.of(user));

        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("disabled_user");
        });
    }

}
