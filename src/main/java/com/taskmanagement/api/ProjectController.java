package com.taskmanagement.api;

import com.taskmanagement.dto.request.CreateProjectRequest;
import com.taskmanagement.dto.request.UpdateProjectRequest;
import com.taskmanagement.dto.response.ProjectResponse;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.service.ProjectService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ProjectController - REST API endpoints cho Project management
 * 
 * Base URL: /api/projects
 * 
 * Endpoints:
 * - POST   /api/projects              → Create project
 * - GET    /api/projects              → Get all active projects
 * - GET    /api/projects/{id}         → Get project by ID
 * - PUT    /api/projects/{id}         → Update project
 * - DELETE /api/projects/{id}         → Archive project
 * - POST   /api/projects/{id}/reactivate → Reactivate project
 * - GET    /api/projects/{id}/tasks   → Get project tasks
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {
    
    private final ProjectService projectService;

    // ==================== CREATE PROJECT ====================
    
    /**
     * Tạo project mới
     * 
     * POST /api/projects
     * 
     * Request Body:
     * {
     *   "name": "Website Redesign",
     *   "description": "Redesign company website",
     *   "ownerId": 5,
     *   "startDate": "2025-12-20",
     *   "endDate": "2026-03-31"
     * }
     * 
     * Response 201 Created:
     * {
     *   "id": 3,
     *   "name": "Website Redesign",
     *   "active": true,
     *   "owner": { "id": 5, "username": "john_doe", ... },
     *   "taskStatistics": { "total": 0, ... }
     * }
     * 
     * @param request CreateProjectRequest
     * @return ResponseEntity 201 Created với ProjectResponse
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
        @Valid @RequestBody CreateProjectRequest request
    ) {
        log.info("POST /api/projects - Tạo project mới: {}", request.getName());
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== GET ALL PROJECTS ====================
    
    /**
     * Lấy danh sách tất cả active projects
     * 
     * GET /api/projects
     * 
     * Response 200 OK:
     * [
     *   {
     *     "id": 1,
     *     "name": "Project A",
     *     "active": true,
     *     "taskStatistics": { "total": 15, "completed": 8, ... }
     *   },
     *   ...
     * ]
     * 
     * @return ResponseEntity 200 OK với List<ProjectResponse>
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        log.info("GET /api/projects - Lấy tất cả active projects");

        List<ProjectResponse> response = projectService.getAllProjects();

        log.info("Retrieved {} active projects", response.size());

        return ResponseEntity.ok(response);
    }

    // ==================== GET PROJECT BY ID ====================
    
    /**
     * Lấy project theo ID
     * 
     * GET /api/projects/{id}
     * 
     * Response 200 OK:
     * {
     *   "id": 3,
     *   "name": "Website Redesign",
     *   "description": "...",
     *   "active": true,
     *   "owner": { ... },
     *   "taskStatistics": { ... }
     * }
     * 
     * Response 404 Not Found:
     * {
     *   "status": 404,
     *   "error": "Project Not Found",
     *   "message": "Project not found with ID: 999"
     * }
     * 
     * @param id Project ID
     * @return ResponseEntity 200 OK với ProjectResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(
        @PathVariable Long id
    ) {
        log.info("GET /api/projects/{} - Fetching project by ID", id);

        ProjectResponse response = projectService.getProjectById(id);

        log.info("Project found: projectId={}", response.getId());
        return ResponseEntity.ok(response);
    }

    // ==================== UPDATE PROJECT ====================
    
    /**
     * Cập nhật project
     * 
     * PUT /api/projects/{id}
     * 
     * Request Body (Partial Update):
     * {
     *   "name": "Website Redesign v2",
     *   "endDate": "2026-06-30"
     * }
     * 
     * Response 200 OK:
     * {
     *   "id": 3,
     *   "name": "Website Redesign v2",
     *   "endDate": "2026-06-30",
     *   "updatedAt": "2025-12-17T12:00:00"
     * }
     * 
     * @param id Project ID
     * @param request UpdateProjectRequest
     * @return ResponseEntity 200 OK với ProjectResponse
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updatedProject(
        @PathVariable Long id,
        @Valid @RequestBody UpdateProjectRequest request
    ) {
        log.info("PUT /api/projects/{} - Updating project", id);

        ProjectResponse response = projectService.updateProject(id, request);

        log.info("Project updated: projectId={}", id);
        return ResponseEntity.ok(response);
    }

    // ==================== ARCHIVE PROJECT ====================
    
    /**
     * Archive project (soft delete)
     * 
     * DELETE /api/projects/{id}
     * 
     * Response 204 No Content
     * 
     * Business Effect:
     * - Set active = false
     * - Không thể tạo task mới
     * - Tasks hiện tại vẫn giữ nguyên
     * 
     * @param id Project ID
     * @return ResponseEntity 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiveProject(
        @PathVariable Long id
    ) {
        log.info("DELETE /api/projects/{} - Archiving project", id);

        projectService.archiveProject(id);

        log.info("Project archived: projectId={}", id);
        return ResponseEntity.noContent().build();
    }

    // ==================== REACTIVATE PROJECT ====================
    
    /**
     * Reactivate archived project
     * 
     * POST /api/projects/{id}/reactivate
     * 
     * Response 200 OK:
     * {
     *   "id": 3,
     *   "name": "Website Redesign",
     *   "active": true,
     *   "updatedAt": "2025-12-17T13:00:00"
     * }
     * 
     * @param id Project ID
     * @return ResponseEntity 200 OK với ProjectResponse
     */
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ProjectResponse> reactivateProject(
        @PathVariable Long id
    ) {
        log.info("POST /api/projects/{}/reactivate - Reactivating project", id);

        ProjectResponse response = projectService.reactivateProject(id);

        return ResponseEntity.ok(response);
    }

    // ==================== GET PROJECT TASKS ====================
    
    /**
     * Lấy tất cả tasks của project
     * 
     * GET /api/projects/{id}/tasks
     * 
     * Response 200 OK:
     * [
     *   {
     *     "id": 10,
     *     "title": "Design homepage",
     *     "status": "IN_PROGRESS",
     *     "assignee": { ... }
     *   },
     *   ...
     * ]
     * 
     * @param id Project ID
     * @return ResponseEntity 200 OK với List<TaskResponse>
     */
    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponse>> getProjectTasks(@PathVariable Long id) {
        log.info("GET /api/projects/{}/tasks - Fetching project tasks", id);
        
        List<TaskResponse> responses = projectService.getProjectTasks(id);
        
        log.info("Retrieved {} tasks for project: projectId={}", 
            responses.size(), id);
        
        return ResponseEntity.ok(responses);
    }
}
