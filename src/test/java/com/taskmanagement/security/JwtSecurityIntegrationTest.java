package com.taskmanagement.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.dto.auth.*;
import com.taskmanagement.entity.User;
import com.taskmanagement.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Simple JWT Security Integration Test
 * 
 * This test verifies the basic JWT authentication flow:
 * 1. Login with correct credentials
 * 2. Login with wrong credentials
 * 3. Access protected endpoint with token
 * 4. Access protected endpoint without token
 * 5. Refresh token
 */

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("JWT Security Integration Test")
class JwtSecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired 
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired 
    private PasswordEncoder passwordEncoder;

    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "testpassword";
    private static final String EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        // 1. Clear database
        userRepository.deleteAll();

        // 2. Create test user
        User user = User.builder()
            .username(USERNAME)
            .email(EMAIL)
            .passwordHash(passwordEncoder.encode(PASSWORD))
            .fullName("Test User")
            .roles("ROLE_USER")
            .active(true)
            .deleted(false)
            .build();
        userRepository.save(user);
    }

    @Test
    @DisplayName("Test 1: Login with correct credentials should return JWT token")
    void testLoginSuccess() throws Exception {
        // Prepare login request
        LoginRequest request = LoginRequest.builder()
            .username(USERNAME)
            .password(PASSWORD)
            .build();

        // Send login request
        MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.username").value(USERNAME))
            .andReturn();

        // Verify tokens are not empty
        String responseBody = result.getResponse().getContentAsString();
        AuthResponse response = objectMapper.readValue(responseBody, AuthResponse.class);

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertFalse(response.getAccessToken().isEmpty());
        assertFalse(response.getRefreshToken().isEmpty());
    }

    @Test
    @DisplayName("Test 2: Login with wrong credentials should return 401")
    void testLoginWithWrongPassword() throws Exception {
        // Prepare login request with wrong password
        LoginRequest request = LoginRequest.builder()
            .username(USERNAME)
            .password("WrongPassword123!")
            .build();

        // Send login request
        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Test 3: Access protected endpoint with valid token should succeed")
    void testAccessProtectedEndpointWithToken() throws Exception {
        // 1. Login to get token
        LoginRequest loginRequest = LoginRequest.builder()
            .username(USERNAME)
            .password(PASSWORD)
            .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        // Extract token from response
        String responseBody = loginResult.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        String accessToken = authResponse.getAccessToken();

        // 2. Access protected endpoint with token
        mockMvc.perform(get("/api/users")
            .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Test 4: Access protected endpoint without token should return 401")
    void testAccessProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Test 5: Refresh token should return new access token")
    void testRefreshToken() throws Exception {
        // 1. Login to get refresh token
        LoginRequest loginRequest = LoginRequest.builder()
            .username(USERNAME)
            .password(PASSWORD)
            .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        // Extract refresh token from response
        String loginResponseBody = loginResult.getResponse().getContentAsString();
        AuthResponse loginResponse = objectMapper.readValue(loginResponseBody, AuthResponse.class);
        String refreshToken = loginResponse.getRefreshToken();

        // 2. Send refresh token request
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
            .refreshToken(refreshToken)
            .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(refreshRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andReturn();

        // Verify new tokens are not empty
        String refreshResponseBody = refreshResult.getResponse().getContentAsString();
        AuthResponse refreshResponse = objectMapper.readValue(refreshResponseBody, AuthResponse.class);

        assertNotNull(refreshResponse.getAccessToken());
        assertNotNull(refreshResponse.getRefreshToken());

        // Step 3: Verify new access token works
        mockMvc.perform(get("/api/users")
            .header("Authorization", "Bearer " + refreshResponse.getAccessToken()))
            .andExpect(status().isOk());
    }

}
