package com.taskmanagement.service;

import com.taskmanagement.dto.request.CreateTaskRequest;
import com.taskmanagement.dto.request.UpdateTaskRequest;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;
import com.taskmanagement.entity.Project;
import com.taskmanagement.entity.TaskStatus;
import com.taskmanagement.entity.TaskPriority;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.repository.ProjectRepository;
import com.taskmanagement.exception.TaskNotFoundException;
import com.taskmanagement.exception.UserNotFoundException;
import com.taskmanagement.exception.ProjectNotFoundException;
import com.taskmanagement.util.TestConstants;
import com.taskmanagement.util.TestDataBuilder;

import ch.qos.logback.core.joran.action.Action;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Positive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
public class TaskServiceTest {
    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TaskService taskService;

    private Task testTask;
    private User testAssignee;
    private Project testProject;
    private CreateTaskRequest createTaskRequest;
    private UpdateTaskRequest updateTaskRequest;

    @BeforeEach
    void setUp() {
        testProject = TestDataBuilder.buildValidProject();
        testProject.setActive(true);
        testProject.setOwner(TestDataBuilder.buildValidUser());

        testAssignee = TestDataBuilder.buildValidUser();
        testAssignee.setActive(true);

        testTask = TestDataBuilder.buildValidTask();
        testTask.setProject(testProject);
        testTask.setAssignees(new HashSet<>(Set.of(testAssignee)));
        testTask.setStatus(TaskStatus.PENDING);

        createTaskRequest = new CreateTaskRequest();
        createTaskRequest.setTitle(TestConstants.TEST_TASK_TITLE);
        createTaskRequest.setDescription(TestConstants.TEST_TASK_DESCRIPTION);
        createTaskRequest.setProjectId(TestConstants.TEST_PROJECT_ID);
        createTaskRequest.setPriority(TaskPriority.MEDIUM);
        createTaskRequest.setAssigneeIds(List.of(TestConstants.TEST_USER_ID));

        updateTaskRequest = new UpdateTaskRequest();
        updateTaskRequest.setTitle("Update Task Title");
        updateTaskRequest.setDescription("Update Task Description");
        updateTaskRequest.setStatus(TaskStatus.IN_PROGRESS);
        updateTaskRequest.setPriority(TaskPriority.HIGH);
    }

    // ==================== CREATE TASK TESTS ====================
    @Test
    @DisplayName("CreateTask - Valid data - Should return TaskResponse")
    void createTask_ValidData_ReturnTaskResponse() {
        // Arrange
        when(userRepository.findAllById(createTaskRequest.getAssigneeIds()))
            .thenReturn(List.of(testAssignee));
        when(projectRepository.findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID))
            .thenReturn(Optional.of(testProject));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        when(taskRepository.findByIdNative(testTask.getId()))
            .thenReturn(Optional.of(testTask));
        when(taskRepository.findAssigneeIdsByTaskId(testTask.getId()))
            .thenReturn(List.of(testAssignee.getId()));

        // Act
        TaskResponse result = taskService.createTask(createTaskRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testTask.getId(), result.getId());
        assertEquals(testTask.getTitle(), result.getTitle());
        verify(userRepository, times(2)).findAllById(any());
        verify(projectRepository, times(1)).findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("CreateTask - Non-existent project - Should throw ProjectNotFoundException")
    void createTask_NonExistingProject_ThrowsException() {
        // Arrange
        when(userRepository.findAllById(createTaskRequest.getAssigneeIds()))
            .thenReturn(List.of(testAssignee));
        when(projectRepository.findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ProjectNotFoundException.class, () -> {
            taskService.createTask(createTaskRequest);
        });

        verify(userRepository, times(1)).findAllById(createTaskRequest.getAssigneeIds());
        verify(projectRepository, times(1)).findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("CreateTask - Empty title - Should throw UserNotFoundException")
    void createTask_EmptyTitle_ThrowsException() {
        // Arrange
        createTaskRequest.setTitle("");

        when(userRepository.findAllById(createTaskRequest.getAssigneeIds()))
            .thenReturn(List.of()); 

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            taskService.createTask(createTaskRequest);
        });

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("CreateTask - Null project Id - Should throw UserNotFoundException")
    void createTask_NullProjectId_ThrowsException() {
        // Arrange
        createTaskRequest.setProjectId(null);

        when(userRepository.findAllById(createTaskRequest.getAssigneeIds()))
            .thenReturn(List.of());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            taskService.createTask(createTaskRequest);
        });

        verify(projectRepository, never()).findByIdAndActiveTrue(anyLong());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("CreateTask - Non-existent assignee - Should throw UserNotFoundException")
    void createTask_NonExistingAssignee_ThrowsException() {
        // Arrange
        when(userRepository.findAllById(createTaskRequest.getAssigneeIds()))
            .thenReturn(List.of()); 

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            taskService.createTask(createTaskRequest);
        });

        verify(userRepository, times(1)).findAllById(createTaskRequest.getAssigneeIds());
        verify(projectRepository, never()).findByIdAndActiveTrue(anyLong());
        verify(taskRepository, never()).save(any(Task.class));
    }

    // ==================== GET TASK BY ID TESTS ====================
    @Test
    @DisplayName("GetTaskById - Existing task - Should return TaskResponse")
    void getTaskById_ExistingTask_ReturnTaskResponse() {
        // Arrange
        when(taskRepository.findByIdNative(TestConstants.TEST_TASK_ID))
            .thenReturn(Optional.of(testTask));
        when(taskRepository.findAssigneeIdsByTaskId(TestConstants.TEST_TASK_ID))
            .thenReturn(List.of(testAssignee.getId()));
        when(userRepository.findAllById(List.of(testAssignee.getId())))
            .thenReturn(List.of(testAssignee));

        // Act
        TaskResponse result = taskService.getTaskById(TestConstants.TEST_TASK_ID);

        // Assert
        assertNotNull(result);
        assertEquals(testTask.getId(), result.getId());
        assertEquals(testTask.getTitle(), result.getTitle());
        verify(taskRepository, times(1)).findByIdNative(TestConstants.TEST_TASK_ID);
    }

    @Test
    @DisplayName("GetTaskById - Non-existent task - Should throw TaskNotFoundException")
    void getTaskById_NonExistingTask_ThrowsException() {
        // Arrange
        when(taskRepository.findByIdNative(TestConstants.INVALID_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> {
            taskService.getTaskById(TestConstants.INVALID_ID);
        });

        verify(taskRepository, times(1)).findByIdNative(TestConstants.INVALID_ID);
    }

    // ==================== UPDATE TASK TESTS ====================
    @Test
    @DisplayName("UpdateTask - Valid data - Should return updated TaskResponse")    
    void updateTask_ValidData_ReturnsUpdatedTaskResponse() {
        // Arrange
        Task updatedTask = TestDataBuilder.buildValidTask();
        updatedTask.setTitle("Update Task Title");
        updatedTask.setDescription("Update Task Description");
        updatedTask.setStatus(TaskStatus.IN_PROGRESS);
        updatedTask.setProject(testProject);
        updatedTask.setAssignees(new HashSet<>(Set.of(testAssignee)));

        when(taskRepository.findById(TestConstants.TEST_TASK_ID))
            .thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class)))
            .thenReturn(updatedTask);

        // Act
        TaskResponse result = taskService.updateTask(TestConstants.TEST_TASK_ID, updateTaskRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Update Task Title", result.getTitle());
        assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());
        verify(taskRepository, times(1)).findById(TestConstants.TEST_TASK_ID);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("UpdateTask - Non-existent task - Should throw TaskNotFoundException")
    void updateTask_NonExistingId_ThrowsException() {
        // Arrange
        when(taskRepository.findById(TestConstants.TEST_TASK_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> {
            taskService.updateTask(TestConstants.TEST_TASK_ID, updateTaskRequest);
        });

        verify(taskRepository, times(1)).findById(TestConstants.TEST_TASK_ID);
        verify(taskRepository, never()).save(any(Task.class));
    }

    // ==================== DELETE TASK TESTS ====================
    @Test
    @DisplayName("DeleteTask - Existing task - Should delete successfully")
    void deleteTask_ExistingId_DeletesSuccessfully() {
        // Arrange
        when(taskRepository.findById(TestConstants.TEST_TASK_ID))
            .thenReturn(Optional.of(testTask));

        // Act
        taskService.deleteTask(TestConstants.TEST_TASK_ID);

        // Assert
        verify(taskRepository, times(1)).findById(TestConstants.TEST_TASK_ID);
        verify(taskRepository, times(1)).delete(testTask);
    }

    @Test
    @DisplayName("DeleteTask - Non-existent task - Should throw TaskNotFoundException")
    void deleteTask_NonExistingId_ThrowsException() {
        // Arrange
        when(taskRepository.findById(TestConstants.TEST_TASK_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> {
            taskService.deleteTask(TestConstants.TEST_TASK_ID);
        });

        verify(taskRepository, times(1)).findById(TestConstants.TEST_TASK_ID);
        verify(taskRepository, never()).delete(any(Task.class));
    }

}
