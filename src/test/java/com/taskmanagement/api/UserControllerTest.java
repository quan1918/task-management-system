package com.taskmanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.dto.request.CreateUserRequest;
import com.taskmanagement.dto.request.UpdateUserRequest;
import com.taskmanagement.dto.response.UserResponse;
import com.taskmanagement.exception.GlobalExceptionHandler;
import com.taskmanagement.exception.UserNotFoundException;
import com.taskmanagement.service.UserService;
import com.taskmanagement.util.TestConstants;
import com.taskmanagement.util.TestDataBuilder;
import com.taskmanagement.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Test")
class UserControllerTest {

    private MockMvc mockMvc;
    
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserResponse userResponse;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        
        objectMapper = new ObjectMapper();
        
        User user = TestDataBuilder.buildValidUser();
        
        userResponse = UserResponse.from(user);
        
        createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername(TestConstants.TEST_USERNAME);
        createUserRequest.setEmail(TestConstants.TEST_USER_EMAIL);
        createUserRequest.setPassword(TestConstants.TEST_USER_PASSWORD_HASH);
        createUserRequest.setFullName(TestConstants.TEST_USER_FULLNAME);
        
        updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setEmail("updated@example.com");
        updateUserRequest.setFullName("Updated Full Name");
    }

    @Test
    @DisplayName("Create user with valid request returns 201 Created")
    void createUser_ValidRequest_ReturnsCreated() throws Exception {
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TestConstants.TEST_USER_ID))
                .andExpect(jsonPath("$.username").value(TestConstants.TEST_USERNAME))
                .andExpect(jsonPath("$.email").value(TestConstants.TEST_USER_EMAIL));

        verify(userService, times(1)).createUser(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("Create user with empty username returns 400 Bad Request")
    void createUser_EmptyUsername_ReturnsBadRequest() throws Exception {
        createUserRequest.setUsername("");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("Create user with invalid email returns 400 Bad Request")
    void createUser_InvalidEmail_ReturnsBadRequest() throws Exception {
        createUserRequest.setEmail("invalid-email");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("Create user with short password returns 400 Bad Request")
    void createUser_ShortPassword_ReturnsBadRequest() throws Exception {
        createUserRequest.setPassword("123");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("Get user by id when user exists returns 200 OK")
    void getUserById_ExistingUser_ReturnsOk() throws Exception {
        when(userService.getUserById(TestConstants.TEST_USER_ID)).thenReturn(userResponse);

        mockMvc.perform(get("/api/users/{id}", TestConstants.TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TestConstants.TEST_USER_ID))
                .andExpect(jsonPath("$.username").value(TestConstants.TEST_USERNAME))
                .andExpect(jsonPath("$.email").value(TestConstants.TEST_USER_EMAIL));

        verify(userService, times(1)).getUserById(TestConstants.TEST_USER_ID);
    }

    @Test
    @DisplayName("Get user by id when user does not exist returns 404 Not Found")
    void getUserById_NonExistingUser_ReturnsNotFound() throws Exception {
        when(userService.getUserById(TestConstants.TEST_USER_ID))
                .thenThrow(new UserNotFoundException(TestConstants.TEST_USER_ID));

        mockMvc.perform(get("/api/users/{id}", TestConstants.TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).getUserById(TestConstants.TEST_USER_ID);
    }

    @Test
    @DisplayName("Get all users when users exist returns 200 OK")
    void getAllUsers_UsersExist_ReturnsOk() throws Exception {
        List<UserResponse> users = List.of(userResponse);
        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(TestConstants.TEST_USER_ID))
                .andExpect(jsonPath("$[0].username").value(TestConstants.TEST_USERNAME));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("Get all users when no users exist returns empty list")
    void getAllUsers_NoUsers_ReturnsEmptyList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("Update user with valid request returns 200 OK")
    void updateUser_ValidRequest_ReturnsOk() throws Exception {
        when(userService.updateUser(eq(TestConstants.TEST_USER_ID), any(UpdateUserRequest.class)))
                .thenReturn(userResponse);

        mockMvc.perform(put("/api/users/{id}", TestConstants.TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TestConstants.TEST_USER_ID))
                .andExpect(jsonPath("$.username").value(TestConstants.TEST_USERNAME));

        verify(userService, times(1)).updateUser(eq(TestConstants.TEST_USER_ID), any(UpdateUserRequest.class));
    }

    @Test
    @DisplayName("Update user with invalid email returns 400 Bad Request")
    void updateUser_InvalidEmail_ReturnsBadRequest() throws Exception {
        updateUserRequest.setEmail("invalid-email");

        mockMvc.perform(put("/api/users/{id}", TestConstants.TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUser(eq(TestConstants.TEST_USER_ID), any(UpdateUserRequest.class));
    }

    @Test
    @DisplayName("Update user when user does not exist returns 404 Not Found")
    void updatedUser_NonExistingUser_ReturnsNotFound() throws Exception {
        when(userService.updateUser(eq(TestConstants.TEST_USER_ID), any(UpdateUserRequest.class)))
                .thenThrow(new UserNotFoundException(TestConstants.TEST_USER_ID));

        mockMvc.perform(put("/api/users/{id}", TestConstants.TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).updateUser(eq(TestConstants.TEST_USER_ID), any(UpdateUserRequest.class));
    }

    @Test
    @DisplayName("Delete user when user exists returns 204 No Content")
    void deleteUser_ExistingUser_ReturnsNoContent() throws Exception {
        doNothing().when(userService).deleteUser(TestConstants.TEST_USER_ID);

        mockMvc.perform(delete("/api/users/{id}", TestConstants.TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(TestConstants.TEST_USER_ID);
    }

    @Test
    @DisplayName("Delete user when user does not exist returns 404 Not Found")
    void deleteUser_NonExistingUser_ReturnsNotFound() throws Exception {
        doThrow(new UserNotFoundException(TestConstants.TEST_USER_ID))
                .when(userService).deleteUser(TestConstants.TEST_USER_ID);

        mockMvc.perform(delete("/api/users/{id}", TestConstants.TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).deleteUser(TestConstants.TEST_USER_ID);
    }
}
