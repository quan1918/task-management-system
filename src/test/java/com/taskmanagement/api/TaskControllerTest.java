package com.taskmanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.dto.request.CreateTaskRequest;
import com.taskmanagement.dto.request.UpdateTaskRequest;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.TaskStatus;
import com.taskmanagement.entity.TaskPriority;
import com.taskmanagement.exception.GlobalExceptionHandler;
import com.taskmanagement.exception.TaskNotFoundException;
import com.taskmanagement.service.TaskService;
import com.taskmanagement.util.TestConstants;
import com.taskmanagement.util.TestDataBuilder;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskController Tests")
public class TaskControllerTest {
    private MockMvc mockMvc;
    
    private ObjectMapper objectMapper;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private TaskResponse taskResponse;
    private CreateTaskRequest createTaskRequest;
    private UpdateTaskRequest updateTaskRequest;

    @BeforeEach
    void setUp() {
        // Initialize MockMvc with standalone setup
        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        
        // Initialize ObjectMapper
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // For LocalDate serialization
        
        // Build task entity first
        Task task = TestDataBuilder.buildValidTask();
        
        // Convert entity to DTO using from() method
        taskResponse = TaskResponse.from(task);
        
        // Build CreateTaskRequest
        createTaskRequest = new CreateTaskRequest();
        createTaskRequest.setTitle(TestConstants.TEST_TASK_TITLE);
        createTaskRequest.setDescription(TestConstants.TEST_TASK_DESCRIPTION);
        createTaskRequest.setProjectId(TestConstants.TEST_PROJECT_ID);
        createTaskRequest.setAssigneeIds(List.of(TestConstants.TEST_USER_ID));
        createTaskRequest.setPriority(TaskPriority.MEDIUM);
        createTaskRequest.setDueDate(TestConstants.getTestTaskDueDate());
        
        // Build UpdateTaskRequest
        updateTaskRequest = new UpdateTaskRequest();
        updateTaskRequest.setTitle("Updated Task");
        updateTaskRequest.setDescription("Updated Description");
        updateTaskRequest.setStatus(TaskStatus.IN_PROGRESS);
        updateTaskRequest.setPriority(TaskPriority.HIGH);
        updateTaskRequest.setDueDate(LocalDateTime.now().plusDays(14));
    }

    // ==================== CREATE TASK TESTS ====================

    @Test
    @DisplayName("Create task with valid request returns 201 Created")
    void createTask_ValidRequest_ReturnsCreated() throws Exception {
        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(taskResponse);

    mockMvc.perform(post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createTaskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TestConstants.TEST_TASK_ID))
                .andExpect(jsonPath("$.title").value(TestConstants.TEST_TASK_TITLE))
                .andExpect(jsonPath("$.description").value(TestConstants.TEST_TASK_DESCRIPTION))
                .andExpect(jsonPath("$.project.id").value(TestConstants.TEST_PROJECT_ID));

        verify(taskService, times(1)).createTask(any(CreateTaskRequest.class));
    }

    @Test
    @DisplayName("Create task with empty title returns 400 Bad Request")
    void createTask_EmptyTitle_ReturnsBadRequest() throws Exception {
        createTaskRequest.setTitle("");

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTaskRequest)))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any(CreateTaskRequest.class));
    }

    @Test
    @DisplayName("Create task with short title returns 400 Bad Request")
    void createTask_ShortTitle_ReturnsBadRequest() throws Exception {
        createTaskRequest.setTitle("AB"); // Less than 3 characters

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTaskRequest)))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any(CreateTaskRequest.class));
    }

    @Test
    @DisplayName("Create task with null projectId returns 400 Bad Request")
    void createTask_NullProjectId_ReturnsBadRequest() throws Exception {
        createTaskRequest.setProjectId(null);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTaskRequest)))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any(CreateTaskRequest.class));
    }

    @Test
    @DisplayName("Create task with past due date returns 400 Bad Request")
    void createTask_PastDueDate_ReturnsBadRequest() throws Exception {
        createTaskRequest.setDueDate(LocalDateTime.now().minusDays(1));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTaskRequest)))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any(CreateTaskRequest.class));
    }

    // ==================== GET TASK BY ID TESTS ====================

    @Test
    @DisplayName("Get task by id when task exists returns 200 OK")
    void getTaskById_ExistingTask_ReturnsOk() throws Exception {
        when(taskService.getTaskById(TestConstants.TEST_TASK_ID)).thenReturn(taskResponse);

        mockMvc.perform(get("/api/tasks/{id}", TestConstants.TEST_TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TestConstants.TEST_TASK_ID))
                .andExpect(jsonPath("$.title").value(TestConstants.TEST_TASK_TITLE))
                .andExpect(jsonPath("$.description").value(TestConstants.TEST_TASK_DESCRIPTION))
                .andExpect(jsonPath("$.project.id").value(TestConstants.TEST_PROJECT_ID));

        verify(taskService, times(1)).getTaskById(TestConstants.TEST_TASK_ID);
    }

    @Test
    @DisplayName("Get task by id when task does not exist returns 404 Not Found")
    void getTaskById_NonExistingTask_ReturnsNotFound() throws Exception {
        when(taskService.getTaskById(TestConstants.TEST_TASK_ID))
                .thenThrow(new TaskNotFoundException(TestConstants.TEST_TASK_ID));

        mockMvc.perform(get("/api/tasks/{id}", TestConstants.TEST_TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(taskService, times(1)).getTaskById(TestConstants.TEST_TASK_ID);
    }

    // ==================== UPDATE TASK TESTS ====================

    @Test
    @DisplayName("Update task with valid request returns 200 OK")
    void updateTask_ValidRequest_ReturnsOk() throws Exception {
        when(taskService.updateTask(eq(TestConstants.TEST_TASK_ID), any(UpdateTaskRequest.class)))
                .thenReturn(taskResponse);

        mockMvc.perform(put("/api/tasks/{id}", TestConstants.TEST_TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateTaskRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TestConstants.TEST_TASK_ID));

        verify(taskService, times(1)).updateTask(eq(TestConstants.TEST_TASK_ID), any(UpdateTaskRequest.class));
    }

    @Test
    @DisplayName("Update task when task does not exist returns 404 Not Found")
    void updateTask_NonExistingTask_ReturnsNotFound() throws Exception {
        when(taskService.updateTask(eq(TestConstants.TEST_TASK_ID), any(UpdateTaskRequest.class)))
                .thenThrow(new TaskNotFoundException(TestConstants.TEST_TASK_ID));

        mockMvc.perform(put("/api/tasks/{id}", TestConstants.TEST_TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateTaskRequest)))
                .andExpect(status().isNotFound());

        verify(taskService, times(1)).updateTask(eq(TestConstants.TEST_TASK_ID), any(UpdateTaskRequest.class));
    }

    @Test
    @DisplayName("Update task with short title returns 400 Bad Request")
    void updateTask_ShortTitle_ReturnsBadRequest() throws Exception {
        updateTaskRequest.setTitle("AB"); // Less than 3 characters

        mockMvc.perform(put("/api/tasks/{id}", TestConstants.TEST_TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateTaskRequest)))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).updateTask(eq(TestConstants.TEST_TASK_ID), any(UpdateTaskRequest.class));
    }

    // ==================== DELETE TASK TESTS ====================

    @Test
    @DisplayName("Delete task when task exists returns 204 No Content")
    void deleteTask_ExistingTask_ReturnsNoContent() throws Exception {
        doNothing().when(taskService).deleteTask(TestConstants.TEST_TASK_ID);

        mockMvc.perform(delete("/api/tasks/{id}", TestConstants.TEST_TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).deleteTask(TestConstants.TEST_TASK_ID);
    }

    @Test
    @DisplayName("Delete task when task does not exist returns 404 Not Found")
    void deleteTask_NonExistingTask_ReturnsNotFound() throws Exception {
        doThrow(new TaskNotFoundException(TestConstants.TEST_TASK_ID))
                .when(taskService).deleteTask(TestConstants.TEST_TASK_ID);

        mockMvc.perform(delete("/api/tasks/{id}", TestConstants.TEST_TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(taskService, times(1)).deleteTask(TestConstants.TEST_TASK_ID);
    }

}
