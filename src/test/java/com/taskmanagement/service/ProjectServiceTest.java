package com.taskmanagement.service;

import com.taskmanagement.dto.request.CreateProjectRequest;
import com.taskmanagement.dto.request.UpdateProjectRequest;
import com.taskmanagement.dto.response.ProjectResponse;
import com.taskmanagement.entity.Project;
import com.taskmanagement.entity.User;
import com.taskmanagement.exception.ProjectNotFoundException;
import com.taskmanagement.exception.UserNotFoundException;
import com.taskmanagement.repository.ProjectRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.util.TestConstants;
import com.taskmanagement.util.TestDataBuilder;

import net.bytebuddy.asm.Advice.OffsetMapping.Factory.Illegal;

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
@DisplayName("ProjectService Unit Tests")
public class ProjectServiceTest {
    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private ProjectService projectService;

    private Project testProject;
    private User testOwner;
    private CreateProjectRequest createProjectRequest;
    private UpdateProjectRequest updateProjectRequest;

    @BeforeEach
    void setUp() {
        testOwner = TestDataBuilder.buildOwnerUser();
        testOwner.setActive(true);

        testProject = TestDataBuilder.buildValidProject();
        testProject.setActive(true);
        testProject.setOwner(testOwner);

        createProjectRequest = new CreateProjectRequest();
        createProjectRequest.setName(TestConstants.TEST_PROJECT_NAME);
        createProjectRequest.setDescription(TestConstants.TEST_PROJECT_DESCRIPTION);
        createProjectRequest.setOwnerId(TestConstants.TEST_OWNER_ID);

        updateProjectRequest = new UpdateProjectRequest();
        updateProjectRequest.setName("Updated Project Name");
        updateProjectRequest.setDescription("Updated Project Description");
    }

    // ==================== CREATE PROJECT TESTS ====================
    @Test
    @DisplayName("Create Project - Valid data - Should return ProjectResponse")
    void createProject_ValidData_ReturnsProjectResponse() {
        // Arrange
        when(userRepository.findById(TestConstants.TEST_OWNER_ID)).thenReturn(Optional.of(testOwner));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        // Act
        ProjectResponse result = projectService.createProject(createProjectRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testProject.getId(), result.getId());
        assertEquals(testProject.getName(), result.getName());
        assertEquals(testProject.getDescription(), result.getDescription());
        verify(userRepository, times(1)).findById(TestConstants.TEST_OWNER_ID);
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    @DisplayName("Create Project - Non-existent owner - Should throw UserNotFoundException")
    void createProject_NonExistingOwner_ThrowsException() {
        // Arrage
        when(userRepository.findById(TestConstants.TEST_OWNER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            projectService.createProject(createProjectRequest);
        });

        verify(userRepository, times(1)).findById(TestConstants.TEST_OWNER_ID);
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("Create Project - Empty name - Should throw IllegalArgumentException")
    void createProject_EmptyName_ThrowsException() {
        // Arrange
        createProjectRequest.setName("");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            projectService.createProject(createProjectRequest);
        });

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("Create Project - Null owner ID - Should throw IllegalArgumentException")
    void createProject_NullOwnerId_ThrowsException() {
        // Arrange
        createProjectRequest.setOwnerId(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            projectService.createProject(createProjectRequest);
        });

        verify(userRepository, never()).findById(anyLong());
        verify(projectRepository, never()).save(any(Project.class));
    }

    // ==================== GET PROJECT BY ID TESTS ====================
    @Test
    @DisplayName("Get Project By ID - Existing project - Should return ProjectResponse")
    void getProjectById_ExistingId_ReturnsProjectResponse() {
        // Arrange
        when(projectRepository.findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID))
            .thenReturn(Optional.of(testProject));
        when(taskRepository.findAllByProjectId(TestConstants.TEST_PROJECT_ID))
            .thenReturn(List.of());

        // Act
        ProjectResponse result = projectService.getProjectById(TestConstants.TEST_PROJECT_ID);

        // Assert
        assertNotNull(result);
        assertEquals(testProject.getId(), result.getId());
        assertEquals(testProject.getName(), result.getName());
        verify(projectRepository, times(1)).findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID);
        verify(taskRepository, times(1)).findAllByProjectId(TestConstants.TEST_PROJECT_ID);
    }

    @Test
    @DisplayName("Get Project By ID - Non-existent project - Should throw ProjectNotFoundException")
    void getProjectById_NonExistingId_ThrowsException() {
        // Arrange
        when(projectRepository.findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProjectNotFoundException.class, () -> {
            projectService.getProjectById(TestConstants.TEST_PROJECT_ID);
        });
        verify(projectRepository, times(1)).findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID);
        verify(taskRepository, never()).findAllByProjectId(anyLong());
    }

    // ==================== GET ALL PROJECT TESTS ====================
    @Test
    @DisplayName("Get All Projects - Project exist - Should return list of ProjectResponse")
    void getAllProjects_ProjectsExist_ReturnsProjectResponseList() {
        // Arrange
        Project project1 = TestDataBuilder.buildValidProject();
        project1.setActive(true);  
        Project project2 = TestDataBuilder.buildProjectWithId(2L);
        project2.setActive(true);  
        
        List<Project> projects = List.of(project1, project2);
        
        when(projectRepository.findAllByActiveTrue()).thenReturn(projects); 

        // Act
        List<ProjectResponse> result = projectService.getAllProjects();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(projectRepository, times(1)).findAllByActiveTrue();
    }

    @Test
    @DisplayName("Get All Projects - No projects - Should return empty list")
    void getAllProjects_NoProejects_ReturnsEmptyList() {
        // Arrange
        when(projectRepository.findAllByActiveTrue()).thenReturn(List.of());

        // Act
        List<ProjectResponse> result = projectService.getAllProjects();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(projectRepository, times(1)).findAllByActiveTrue();
    }

    // ==================== UPDATE PROJECT TESTS ====================
    @Test
    @DisplayName("Update Project - Valid data - Should return updated ProjectResponse")
    void updateProject_ValidData_ReturnsUpdatedProjectResponse() {
        // Arrange
        Project updatedProject = TestDataBuilder.buildValidProject();
        updatedProject.setActive(true);
        updatedProject.setOwner(testOwner);
        updatedProject.setName("Updated Project Name");
        updatedProject.setDescription("Updated Project Description");

        when(projectRepository.findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID))
            .thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(updatedProject);

        // Act
        ProjectResponse result = projectService.updateProject(
            TestConstants.TEST_PROJECT_ID, updateProjectRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Project Name", result.getName());
        assertEquals("Updated Project Description", result.getDescription());
        verify(projectRepository, times(1)).findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID);
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    @DisplayName("Update Project - Non-existent project - Should throw ProjectNotFoundException")
    void updateProject_NonExistingId_ThrowsException() {
        // Arrange
        when(projectRepository.findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProjectNotFoundException.class, () -> {
            projectService.updateProject(TestConstants.TEST_PROJECT_ID, updateProjectRequest);
        });

        verify(projectRepository, times(1)).findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID);
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("Update Project - Empty name - Should throw IllegalArgumentException")
    void updateProject_EmptyName_ThrowsException() {
        // Arrange
        updateProjectRequest.setName("");

        when(projectRepository.findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID))
            .thenReturn(Optional.of(testProject));
         
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            projectService.updateProject(TestConstants.TEST_PROJECT_ID, updateProjectRequest);
        });

        verify(projectRepository, times(1)).findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID);
        verify(projectRepository, never()).save(any(Project.class));
    }

    // ==================== PARTIAL UPDATE TESTS ====================
    @Test
    @DisplayName("updateProject - Only name updated - Should preserve other fields")
    void updateProject_OnlyName_PreservesOtherFields() {
        // Arrange
        UpdateProjectRequest partialUpdate = new UpdateProjectRequest();
        partialUpdate.setName("New Project Name");
        // description = null

        when(projectRepository.findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID))
            .thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        // Act
        ProjectResponse result = projectService.updateProject(
            TestConstants.TEST_PROJECT_ID, 
            partialUpdate
        );

        // Assert
        assertNotNull(result);
        verify(projectRepository, times(1)).findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID);
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    @DisplayName("updateProject - Only description updated - Should preserve other fields")
    void updateProject_OnlyDescription_PreservesOtherFields() {
        // Arrange
        UpdateProjectRequest partialUpdate = new UpdateProjectRequest();
        partialUpdate.setDescription("New Description");
        // name = null

        when(projectRepository.findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID))
            .thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        // Act
        ProjectResponse result = projectService.updateProject(
            TestConstants.TEST_PROJECT_ID, 
            partialUpdate
        );

        // Assert
        assertNotNull(result);
        verify(projectRepository, times(1)).findByIdAndActiveTrue(TestConstants.TEST_PROJECT_ID);
        verify(projectRepository, times(1)).save(any(Project.class));
    }

}
