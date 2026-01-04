package com.taskmanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.dto.request.CreateProjectRequest;
import com.taskmanagement.dto.request.UpdateProjectRequest;
import com.taskmanagement.dto.response.ProjectResponse;
import com.taskmanagement.entity.Project;
import com.taskmanagement.exception.GlobalExceptionHandler;
import com.taskmanagement.exception.ProjectNotFoundException;
import com.taskmanagement.service.ProjectService;
import com.taskmanagement.util.TestConstants;
import com.taskmanagement.util.TestDataBuilder;

import org.aspectj.lang.annotation.Before;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectController Tests")
public class ProjectControllerTest {
    
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectController projectController;

    private ProjectResponse projectResponse;
    private CreateProjectRequest createProjectRequest;
    private UpdateProjectRequest updateProjectRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        Project project = TestDataBuilder.buildValidProject();

        projectResponse = ProjectResponse.from(project, List.of());

        createProjectRequest = new CreateProjectRequest();
        createProjectRequest.setName(TestConstants.TEST_PROJECT_NAME);
        createProjectRequest.setDescription(TestConstants.TEST_PROJECT_DESCRIPTION);
        createProjectRequest.setOwnerId(TestConstants.TEST_OWNER_ID);
        createProjectRequest.setStartDate(TestConstants.TEST_PROJECT_START_DATE);
        createProjectRequest.setEndDate(TestConstants.TEST_PROJECT_END_DATE);
        
        // Build UpdateProjectRequest
        updateProjectRequest = new UpdateProjectRequest();
        updateProjectRequest.setName("Updated Project");
        updateProjectRequest.setDescription("Updated Description");
        updateProjectRequest.setEndDate(LocalDate.now().plusMonths(12));
    }

    // ==================== CREATE PROJECT TESTS ====================
    @Test
    @DisplayName("Create project with valid data returns 201 Created")
    void createProject_ValidData_ReturnsCreated() throws Exception {
        when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenReturn(projectResponse);

        mockMvc.perform(post("/api/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createProjectRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TestConstants.TEST_PROJECT_ID))
                .andExpect(jsonPath("$.name").value(TestConstants.TEST_PROJECT_NAME))
                .andExpect(jsonPath("$.description").value(TestConstants.TEST_PROJECT_DESCRIPTION));

        verify(projectService, times(1)).createProject(any(CreateProjectRequest.class));
    }

    @Test
    @DisplayName("Create project with empty name returns 400 Bad Request")
    void createProject_EmptyName_ReturnsBadRequest() throws Exception {
        createProjectRequest.setName("");

        mockMvc.perform(post("/api/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createProjectRequest)))
                .andExpect(status().isBadRequest());

        verify(projectService, never()).createProject(any(CreateProjectRequest.class));
    }

    @Test
    @DisplayName("Create project with short name returns 404 Not Found")
    void createProject_ShortName_ReturnsNotFound() throws Exception {
        createProjectRequest.setName("AB");

        mockMvc.perform(post("/api/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createProjectRequest)))
                .andExpect(status().isBadRequest());

        verify(projectService, never()).createProject(any(CreateProjectRequest.class));
    }

    @Test
    @DisplayName("Create project with null ownerId returns 400 Bad Request")
    void createProject_NullOwnerId_ReturnsBadRequest() throws Exception {
        createProjectRequest.setOwnerId(null);

        mockMvc.perform(post("/api/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createProjectRequest)))
                .andExpect(status().isBadRequest());

        verify(projectService, never()).createProject(any(CreateProjectRequest.class));
    }

    // ==================== GET PROJECT BY ID TESTS ====================
    @Test
    @DisplayName("Get project by id when project exists returns 200 OK")
    void getProjectById_ExistingProject_ReturnsOk() throws Exception {
        when(projectService.getProjectById(TestConstants.TEST_PROJECT_ID))
                .thenReturn(projectResponse);

        mockMvc.perform(get("/api/projects/{id}", TestConstants.TEST_PROJECT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TestConstants.TEST_PROJECT_ID))
                .andExpect(jsonPath("$.name").value(TestConstants.TEST_PROJECT_NAME))
                .andExpect(jsonPath("$.description").value(TestConstants.TEST_PROJECT_DESCRIPTION));

        verify(projectService, times(1)).getProjectById(TestConstants.TEST_PROJECT_ID);
    }

    @Test
    @DisplayName("Get project by id when project does not exist returns 404 Not Found")
    void getProjectById_NonExistingProject_ReturnsNotFound() throws Exception {
        when(projectService.getProjectById(TestConstants.INVALID_ID))
                .thenThrow(new ProjectNotFoundException(TestConstants.INVALID_ID));

        mockMvc.perform(get("/api/projects/{id}", TestConstants.INVALID_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
 
        verify(projectService, times(1)).getProjectById(TestConstants.INVALID_ID);
    }

    // ==================== GET ALL PROJECTS TESTS ====================
    @Test
    @DisplayName("Get all projects returns 200 OK with list of projects")
    void getAllProjects_ProjectsExist_ReturnsOk() throws Exception {
        when(projectService.getAllProjects())
                .thenReturn(List.of(projectResponse));

        mockMvc.perform(get("/api/projects")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(TestConstants.TEST_PROJECT_ID))
                .andExpect(jsonPath("$[0].name").value(TestConstants.TEST_PROJECT_NAME));

        verify(projectService, times(1)).getAllProjects();
    }

    @Test
    @DisplayName("Get all projects when no projects exist returns 200 OK with empty list")
    void getAllProejct_NoProjects_ReturnsOkEmptyList() throws Exception {
        when(projectService.getAllProjects())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/projects")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(projectService, times(1)).getAllProjects();
    }

    // =================== UPDATE PROJECT TESTS ====================
    @Test
    @DisplayName("Update project with valid request returns 200 OK")
    void updateProject_ValidRequest_ReturnsOk() throws Exception {
        when(projectService.updateProject(eq(TestConstants.TEST_PROJECT_ID), any(UpdateProjectRequest.class)))
                .thenReturn(projectResponse);

        mockMvc.perform(put("/api/projects/{id}", TestConstants.TEST_PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateProjectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TestConstants.TEST_PROJECT_ID));

        verify(projectService, times(1)).updateProject(eq(TestConstants.TEST_PROJECT_ID), any(UpdateProjectRequest.class));
    }

    @Test
    @DisplayName("Update project when project does not exist returns 404 Not Found")
    void updateProject_NonExistingProject_ReturnsNotFound() throws Exception {
        when(projectService.updateProject(eq(TestConstants.TEST_PROJECT_ID), any(UpdateProjectRequest.class)))
                .thenThrow(new ProjectNotFoundException(TestConstants.TEST_PROJECT_ID));

        mockMvc.perform(put("/api/projects/{id}", TestConstants.TEST_PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateProjectRequest)))
                .andExpect(status().isNotFound());

        verify(projectService, times(1)).updateProject(eq(TestConstants.TEST_PROJECT_ID), any(UpdateProjectRequest.class));
    }

    @Test
    @DisplayName("Update project with short name returns 400 Bad Request")
    void updateProject_ShortName_ReturnsBadRequest() throws Exception {
        updateProjectRequest.setName("AB"); // Less than 3 characters

        mockMvc.perform(put("/api/projects/{id}", TestConstants.TEST_PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateProjectRequest)))
                .andExpect(status().isBadRequest());

        verify(projectService, never()).updateProject(eq(TestConstants.TEST_PROJECT_ID), any(UpdateProjectRequest.class));
    }

    // ==================== DELETE PROJECT TESTS ====================

    @Test
    @DisplayName("Delete project when project exists returns 204 No Content")
    void deleteProject_ExistingProject_ReturnsNoContent() throws Exception {
        doNothing().when(projectService).archiveProject(TestConstants.TEST_PROJECT_ID);

        mockMvc.perform(delete("/api/projects/{id}", TestConstants.TEST_PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(projectService, times(1)).archiveProject(TestConstants.TEST_PROJECT_ID);
    }

    @Test
    @DisplayName("Delete project when project does not exist returns 404 Not Found")
    void deleteProject_NonExistingProject_ReturnsNotFound() throws Exception {
        doThrow(new ProjectNotFoundException(TestConstants.TEST_PROJECT_ID))
                .when(projectService).archiveProject(TestConstants.TEST_PROJECT_ID);

        mockMvc.perform(delete("/api/projects/{id}", TestConstants.TEST_PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(projectService, times(1)).archiveProject(TestConstants.TEST_PROJECT_ID);
    }
}
