package com.taskmanagement.service;

import com.taskmanagement.dto.request.CreateProjectRequest;
import com.taskmanagement.dto.request.UpdateProjectRequest;
import com.taskmanagement.dto.response.ProjectResponse;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.Project;
import com.taskmanagement.entity.User;
import com.taskmanagement.exception.ProjectNotFoundException;
import com.taskmanagement.exception.BusinessRuleException;
import com.taskmanagement.exception.UserNotFoundException;
import com.taskmanagement.repository.ProjectRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.repository.TaskRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ProjectService - Business logic cho Project management
 * 
 * Chịu trách nhiệm:
 * - CRUD operations cho Project
 * - Validate business rules
 * - Archive/Reactivate project
 * - Quản lý relationship với Task và User
 * 
 * Business Rules:
 * - Owner phải là user active
 * - Project name phải unique
 * - EndDate phải sau startDate
 * - Không thể update project đã archived
 * - Archive project → không thể tạo task mới
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    // ==================== CREATE PROJECT ====================
    
    /**
     * Tạo project mới
     * 
     * Business Flow:
     * 1. Validate owner tồn tại và active
     * 2. Validate endDate sau startDate
     * 3. Set default values (active=true, startDate=today nếu null)
     * 4. Save project
     * 5. Return ProjectResponse
     * 
     * @param request CreateProjectRequest
     * @return ProjectResponse
     * @throws UserNotFoundException nếu owner không tồn tại
     * @throws BusinessRuleException nếu vi phạm business rules
     */
    public ProjectResponse createProject(CreateProjectRequest request) {
        log.info("Creating new project: name={}, owenerId={}", request.getName(), request.getOwnerId());

        if (request.getOwnerId() == null) {
            throw new IllegalArgumentException("Owner ID cannot be null");
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be null or empty");
        }

        // STEP 1: Validate owner tồn tại và active
        User owner = userRepository.findById(request.getOwnerId())
            .orElseThrow(() -> {log.error("Owner not found: userId={}", request.getOwnerId());
                return new UserNotFoundException(request.getOwnerId());
            });
        
        if (!owner.getActive()) {
            log.error("Owner is not active: userId={}", request.getOwnerId());
            throw new BusinessRuleException("Cannot create project: Owner '" + owner.getUsername() + "' is not active.");
        }

        // STEP 2: Validate dates
        if (!request.isEndDateValid()) {
            log.error("Invalid date range: startDate={}, endDate={}", request.getStartDate(), request.getEndDate());
            throw new BusinessRuleException("End date must be after start date");
        }

        // STEP 3: Build project entity
        Project project = Project.builder()
            .name(request.getName())
            .description(request.getDescription())
            .owner(owner)
            .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now())
            .endDate(request.getEndDate())
            .active(true) // Mặc định project mới là active
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // STEP 4: Save database
        Project savedProject = projectRepository.save(project);

        log.info("Project created seccessfully: projectId={}", savedProject.getId());

        // STEP 5: Query task for statistics
        List<Task> tasks = taskRepository.findAllByProjectId(savedProject.getId());
        return ProjectResponse.from(savedProject, tasks);
    }

    // ==================== GET ALL PROJECTS ====================

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        log.info("Fetching all active projects");

        List<Project> projects = projectRepository.findAllByActiveTrue();

        log.info("Retrieved {} active projects", projects.size());

        return projects.stream().map(project -> { // Query tasks for each project
            List<Task> tasks = taskRepository.findAllByProjectId(project.getId());
            return ProjectResponse.from(project, tasks);
        })
        .collect(Collectors.toList());
    }
    
    // ==================== GET PROJECT BY ID ====================

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        log.info("Fetching project by ID: {}", id);

        Project project = projectRepository.findByIdAndActiveTrue(id)
            .orElseThrow(() -> {
                log.error("Project not found or inactive: projectId={}", id);
                return new ProjectNotFoundException(id);
            });

        // Query tasks for project
        List<Task> tasks = taskRepository.findAllByProjectId(id);

        return ProjectResponse.from(project, tasks);
    }

    // ==================== UPDATE PROJECT ====================

    public ProjectResponse updateProject(Long id, UpdateProjectRequest request) {
        log.info("Updating project: projectId={}", id);

        // STEP 1: Find project
        Project project = projectRepository.findByIdAndActiveTrue(id)
            .orElseThrow(() -> {
                log.error("Project not found or inactive: projectId={}", id);
                return new ProjectNotFoundException(id);
            });
        
        // STEP 2: Update fields
        if (request.getName() != null) {
            if (request.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Project name cannot be empty");
            }
            project.setName(request.getName());
        }

        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
            log.debug("Project description updated: {}", request.getDescription());
        }

        if (request.getOwnerId() != null && 
            !request.getOwnerId().equals(project.getOwner().getId())) {

            User newOwner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new UserNotFoundException(request.getOwnerId()));

            if (!newOwner.getActive()) {
                throw new BusinessRuleException("New owner must be active");
            }

            project.setOwner(newOwner);
            log.debug("Project owner updated: userId={}", request.getOwnerId());
        }
        
        if (request.getStartDate() != null) {
            project.setStartDate(request.getStartDate());
            log.debug("Project startDate updated: {}", request.getStartDate());
        }

        if (request.getEndDate() != null) {
            if(project.getStartDate() != null &&
                request.getEndDate().isBefore(project.getStartDate())) {
                throw new BusinessRuleException("End date must be after start date");
            }
            project.setEndDate(request.getEndDate());
            log.debug("End date updated: {}", request.getEndDate());
        }

        project.setUpdatedAt(LocalDateTime.now());

        // STEP 3: Save updated project
        Project updatedProject = projectRepository.save(project);

        log.info("Project updated successfully: projectId={}", id);

        List<Task> tasks = taskRepository.findAllByProjectId(id);

        return ProjectResponse.from(updatedProject, tasks);
    }

    // ==================== ARCHIVE PROJECT ====================

    public void archiveProject(Long id) {
        log.info("Archiving project: projectId={}", id);

        Project project = projectRepository.findByIdAndActiveTrue(id)
            .orElseThrow(() -> {
                log.error("Project not found or already archived: projectId={}", id);
                return new ProjectNotFoundException(id);
            });

        project.setActive(false);
        project.setUpdatedAt(LocalDateTime.now());

        projectRepository.save(project);

        log.info("Project archived successfully: projectId={}", id);
    }

    // ==================== REACTIVATE PROJECT ====================

    public ProjectResponse reactivateProject(Long id) {
        log.info("Reactivating project: projectId={}", id);

        // Find project (including archived)
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Project not found: projectId={}", id);
                return new ProjectNotFoundException(id);
            });

        if (project.getActive()) {
            log.warn("Project already active: project");
            throw new BusinessRuleException("Project is already active");
        }

        project.setActive(true);
        project.setUpdatedAt(LocalDateTime.now());

        Project reactivatedProject = projectRepository.save(project);

        log.info("Project reactivated successfully: projectId={}", id);

        // Query tasks for response
        List<Task> tasks = taskRepository.findAllByProjectId(id);

        return ProjectResponse.from(reactivatedProject, tasks);
    }

    // ==================== GET PROJECT TASKS ====================

    @Transactional(readOnly = true)
    public List<TaskResponse> getProjectTasks(Long projectId) {
        log.info("Fetching tasks for project: projectId={}", projectId);

        // Validate project exists
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> {
                log.error("Project not found: projectId={}", projectId);
                return new ProjectNotFoundException(projectId);
            });
            
        List<Task> tasks = taskRepository.findAllByProjectIdWithAssignees(projectId);

        log.info("Retrieved {} tasks for projectId={}", tasks.size(), projectId);

        return tasks.stream()
            .map(TaskResponse::from)
            .collect(Collectors.toList());
    }
}
