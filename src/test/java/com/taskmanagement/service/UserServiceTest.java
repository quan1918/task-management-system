package com.taskmanagement.service;

import com.taskmanagement.dto.request.CreateUserRequest;
import com.taskmanagement.dto.request.UpdateUserRequest;
import com.taskmanagement.dto.response.UserResponse;
import com.taskmanagement.entity.User;
import com.taskmanagement.exception.UserNotFoundException;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.util.TestConstants;
import com.taskmanagement.util.TestDataBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.DisplayName;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.buildValidUser();

        createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername(TestConstants.TEST_USERNAME);
        createUserRequest.setEmail(TestConstants.TEST_USER_EMAIL);
        createUserRequest.setPassword(TestConstants.TEST_USER_PASSWORD_HASH);
        createUserRequest.setFullName(TestConstants.TEST_USER_FULLNAME);

        updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setEmail(TestConstants.TEST_USER_EMAIL);
        updateUserRequest.setFullName("Updated Name");
        updateUserRequest.setActive(true);
    }

    // CREATE USER TESTS

    @Test
    @DisplayName("createUser - Valid data - Should return saved user")
    void createUser_ValidData_ReturnsUserResponse() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserResponse result = userService.createUser(createUserRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getUsername(), result.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ==================== GET USER BY ID TESTS ====================

    @Test
    @DisplayName("getUserById - Existing ID - Should return userResponse")
    void getUserById_ExistingId_ReturnsUserResponse() {
        // Arrange
        when(userRepository.findById(TestConstants.TEST_USER_ID))
            .thenReturn(Optional.of(testUser));

        // Act
        UserResponse result = userService.getUserById(TestConstants.TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getUsername(), result.getUsername());
        verify(userRepository, times(1)).findById(TestConstants.TEST_USER_ID);
    }

    @Test
    @DisplayName("getUserById - Non-existing ID - Should throw ResourceNotFoundException")
    void getUserById_NonExistingId_ThrowsException() {
        // Arrange
        when(userRepository.findById(TestConstants.INVALID_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.getUserById(TestConstants.INVALID_ID);
        });

        verify(userRepository, times(1)).findById(TestConstants.INVALID_ID);
    }

    // ==================== GET ALL USERS TESTS ====================

    @Test
    @DisplayName("getAllUsers - Users exist - Should return list of users")
    void getAllUsers_UsersExist_ReturnsUserList() {
        // Arrange
        List<User> users = TestDataBuilder.buildUserList(3);
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<UserResponse> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllUsers - No users - Should return empty list")
    void getAllUsers_NoUsers_ReturnsEmptyList() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of());

        // Act
        List<UserResponse> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findAll();
    }

    // ==================== UPDATE USER TESTS ====================

    @Test
    @DisplayName("updateUser - Valid data - Should return updated userResponse")
    void updateUser_ValidData_ReturnsUpdatedUserResponse() {
        // Arrange
        User updatedUser = TestDataBuilder.buildValidUser();
        updatedUser.setFullName("Updated Name");

        when(userRepository.findById(TestConstants.TEST_USER_ID))
            .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserResponse result = userService.updateUser(TestConstants.TEST_USER_ID, updateUserRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", result.getFullName());
        verify(userRepository, times(1)).findById(TestConstants.TEST_USER_ID);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser - Non-existing ID - Should throw ResourceNotFoundException")
    void updateUser_NonExistingId_ThrowsException() {
        // Arrange
        when(userRepository.findById(TestConstants.INVALID_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.updateUser(TestConstants.INVALID_ID, updateUserRequest);
        });

        verify(userRepository, times(1)).findById(TestConstants.INVALID_ID);
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== DELETE USER TESTS ====================

    @Test
    @DisplayName("deleteUser - Existing ID - Should delete successfully")
    void deleteUser_ExistingId_DeletesSuccessfully() {
        // Arrange
        testUser.setDeleted(false);
        testUser.setDeletedAt(null);

        when(userRepository.findByIdIncludingDeleted(TestConstants.TEST_USER_ID))
            .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        when(taskRepository.unassignTasksByUserId(TestConstants.TEST_USER_ID)).thenReturn(0);
      
        // Act
        userService.deleteUser(TestConstants.TEST_USER_ID);

        // Assert
        verify(userRepository, times(1)).findByIdIncludingDeleted(TestConstants.TEST_USER_ID);
        verify(userRepository, times(1)).save(any(User.class));
        verify(taskRepository, times(1)).unassignTasksByUserId(TestConstants.TEST_USER_ID);
    }

    @Test
    @DisplayName("deleteUser - Non-existing ID - Should throw ResourceNotFoundException")
    void deleteUser_NonExistingId_ThrowsException() {
        // Arrange
        when(userRepository.findByIdIncludingDeleted(TestConstants.INVALID_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.deleteUser(TestConstants.INVALID_ID);
        });

        verify(userRepository, times(1)).findByIdIncludingDeleted(TestConstants.INVALID_ID);
        verify(userRepository, never()).save(any(User.class));
        verify(taskRepository, never()).unassignTasksByUserId(anyLong());
    }

    // ==================== CREATE USER - INVALID EMAIL ====================

    @Test
    @DisplayName("createUser - Invalid email format - Should throw IllegalArgumentException")
    void createUser_InvalidEmail_ThrowsException() {
        // Arrange
        createUserRequest.setEmail(TestConstants.INVALID_EMAIL);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(createUserRequest);
        });
    }
}
