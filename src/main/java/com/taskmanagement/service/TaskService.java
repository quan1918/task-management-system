package com.taskmanagement.service;

import com.taskmanagement.dto.request.CreateTaskRequest;
import com.taskmanagement.dto.request.UpdateTaskRequest;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.dto.response.PagedResponse;
import com.taskmanagement.entity.Project;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.TaskStatus;
import com.taskmanagement.entity.User;
import com.taskmanagement.entity.AuditAction;
import com.taskmanagement.entity.AuditEntityType;
import com.taskmanagement.exception.BusinessRuleException;
import com.taskmanagement.exception.ProjectNotFoundException;
import com.taskmanagement.exception.TaskNotFoundException;
import com.taskmanagement.exception.UserNotFoundException;
import com.taskmanagement.repository.ProjectRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.SecurityUtils;
import com.taskmanagement.event.TaskEvent;
import com.taskmanagement.event.TaskEventType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;

/**
 * TaskService - Lớp chứa tầng business logic cho quản lý Task
 * 
 * Chịu trách nhiệm:
 * - Triển khai các use case liên quan đến Task
 * - Xác thực các business rule
 * - Điều phối các repository
 * - Chuyển đổi giữa DTOs và Entities
 * - Quản lý transactions
 * 
 * Design patterns:
 * - Service layer pattern
 * - Quản lý transaction (@Transactional)
 * - Constructor injection (thông qua @RequiredArgsConstructor)
 * 
 * @author Task Management System
 * @version 1.0 (Tính năng Create Task)
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    // ==================== DEPENDENCIES ====================
    /**
     * Các dependency của Repository (được inject thông qua constructor)
     * 
     * @RequiredArgsConstructor (Lombok):
     * - Tự động tạo constructor với tất cả các trường 'final'
     * - Spring sẽ tự động inject dependency
     * - Tốt hơn @Autowired (bất biến, dễ kiểm thử)
     */
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    // ==================== CREATE TASK ====================
    /**
     * Tạo task mới
     * 
     * Business Flow:
     * 1. Kiểm tra assignee có tồn tại
     * 2. Kiểm tra project có tồn tại và đang active
     * 3. Tạo entity Task
     * 4. Lưu vào database
     * 5. Trả về DTO phản hồi
     * 
     * Business Rules:
     * - Assignee phải tồn tại trong database
     * - Project phải tồn tại và đang active (không bị archive)
     * - Task status mặc định là PENDING
     * - Due date phải nằm trong tương lai (được validate trong DTO)
     * 
     * @param request CreateTaskRequest DTO từ controller
     * @return TaskResponse DTO chứa dữ liệu task vừa tạo
     * @throws UserNotFoundException nếu không tìm thấy assignee
     * @throws ProjectNotFoundException nếu project không tồn tại hoặc không active
     */
    public TaskResponse createTask(CreateTaskRequest request) {
        log.info("Creating new task: title={}, assigneeId={}, projectId={}",
            request.getTitle(), request.getAssigneeIds(), request.getProjectId());
        
        if (request.getAssigneeIds() != null && request.getAssigneeIds().size() > 10) {
            log.error("Too many assignees: {}", request.getAssigneeIds().size());
            throw new BusinessRuleException("Max 10 assignees allowed");
        }
    // ========== STEP 1: Validate Assignee Exists ==========
    
        // Lấy assignee từ database
        Set<User> assignees = new HashSet<>(
            userRepository.findAllActiveByIds(request.getAssigneeIds())
        );

        // Kiểm tra tất cả assignees có được tìm thấy không
        if (assignees.size() != new HashSet<>(request.getAssigneeIds()).size()) {
            Set<Long> resolvedIds = assignees.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

            List<Long> invalidIds = request.getAssigneeIds().stream()
                .distinct()
                .filter(id -> !resolvedIds.contains(id))
                .toList();

            log.error("Some assignees not found: invalidIds={}", invalidIds);
            throw new UserNotFoundException("Assignees not found with IDs: " + invalidIds);
        }

        // Kiểm tra tất cả assignees đều active
        List<User> inactiveUsers = assignees.stream()
            .filter(user -> !user.isActive())
            .collect(Collectors.toList());

        if (!inactiveUsers.isEmpty()) {
            String inactiveUsernames = inactiveUsers.stream()
                .map(User::getUsername)
                .collect(Collectors.joining(", "));
            log.error("Cannot assign task to inactive users: {}", inactiveUsernames);
            throw new BusinessRuleException("Cannot assign task to inactive users: " + inactiveUsernames);
        }

        log.debug("All {} assignees validated successfully", assignees.size());
        
    // ========== STEP 2: Validate Project Exists & Active ==========

        Project project = projectRepository.findByIdAndActiveTrue(request.getProjectId())
            .orElseThrow(() -> {
                log.error("Active project not found: id={}", request.getProjectId());
                return new ProjectNotFoundException(request.getProjectId());
            });
        
        log.debug("Project found: projectId={}, active={}", project.getId(), project.isActive());
       
    // ========== STEP 3: Create Task Entity ==========

        Task task = Task.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .priority(request.getPriority())
            .dueDate(request.getDueDate())
            .estimatedHours(request.getEstimatedHours())
            .notes(request.getNotes())
            .assignees(assignees)
            .project(project)
            .status(TaskStatus.PENDING)
            .build();

        log.debug("Task entity created: {}", task);
    
    // ========== STEP 4: Save to Database ==========

        Task savedTask = taskRepository.save(task);

        log.info("Task saved succesfully: taskId={}, title={}", savedTask.getId(), savedTask.getTitle());

        TaskResponse response = getTaskById(savedTask.getId());
        eventPublisher.publishEvent(
            new TaskEvent(this, TaskEventType.CREATED, response.getProject().getId(), response)
        );
        auditService.logAction(
            SecurityUtils.getCurrentUserId().orElse(null),
            AuditAction.CREATE,
            AuditEntityType.TASK,
            savedTask.getId(),
            "Task created: '" + savedTask.getTitle() + "' in project ID " + savedTask.getProject().getId()
        );
        return response;
    }    

    // ==================== GET TASK BY ID ====================
    
    /**
     * Lấy task theo ID
     * 
     * Business Flow:
     * 1. Tìm task trong database theo ID
     * 2. Nếu không tìm thấy → throw TaskNotFoundException
     * 3. Convert Task entity → TaskResponse DTO
     * 4. Return response
     * 
     * @param id Task ID
     * @return TaskResponse DTO với đầy đủ thông tin
     * @throws TaskNotFoundException nếu task không tồn tại
     * 
     * Example Usage:
     * TaskResponse task = taskService.getTaskById(123L);
     * 
     * Example Response:
     * {
     *   "id": 123,
     *   "title": "Fix login bug",
     *   "status": "PENDING",
     *   "assignee": { "id": 5, "username": "johndoe", ... },
     *   "project": { "id": 3, "name": "Website Redesign", ... }
     * }
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        log.info("Fetching task by ID: {}", id);

        Task task = taskRepository.findByIdWithAssignees(id)   
            .orElseThrow(() -> {
                log.error("Task not found: id={}", id);
                return new TaskNotFoundException(id);
            });

        log.info("Task found: id={}, title={}, assigneesSize={}", 
            task.getId(), task.getTitle(), task.getAssignees().size());

        return TaskResponse.from(task);
    }

    // ==================== UPDATE TASK ====================

    /**
     * Cập nhật task hiện có
     * 
     * Business Flow:
     * 1. Tìm task theo ID (throw exception nếu không tồn tại)
     * 2. Validate assignee mới (nếu có)
     * 3. Validate project mới (nếu có)
     * 4. Apply business rules (status transition, completedAt)
     * 5. Cập nhật các trường được gửi lên (partial update)
     * 6. Lưu vào database
     * 7. Return TaskResponse
     * 
     * Business Rules:
     * - Task phải tồn tại trong database
     * - Chỉ update các field không null trong request
     * - Assignee mới phải tồn tại (nếu thay đổi)
     * - Project mới phải active (nếu thay đổi)
     * - Khi status chuyển sang COMPLETED, tự động set completedAt
     * - Khi status chuyển từ COMPLETED sang khác, xóa completedAt
     * 
     */
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        log.info("Updateing task: id={}", id);

    // ========== STEP 1: Find Existing Task ==========

        Task task = taskRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Task not found for update: id={}", id);
                return new TaskNotFoundException(id);
            });

        log.debug("Found task for update: {}, title={}, status={}", task.getId(), task.getTitle(), task.getStatus());
     
    // ========== STEP 2: Validate & Update Assignee ==========
        
        if (request.getAssigneeIds() !=null) {
            log.debug("Updating assignees: old count={}, new IDs={}",
                task.getAssignees().size(), request.getAssigneeIds());
            
            // Clear old assignees
            task.getAssignees().clear();

            // Add new assignees (if not empty list)
            if (!request.getAssigneeIds().isEmpty()) {

                if (request.getAssigneeIds().size() > 10) {
                    throw new BusinessRuleException(
                        "Task cannot have more than 10 assignees. Provided: " + request.getAssigneeIds().size());
                }

                Set<User> newAssignees = new HashSet<>(
                    userRepository.findAllActiveByIds(request.getAssigneeIds())
                );

                // Kiểm tra tất cả assignees có được tìm thấy không
                if (newAssignees.size() != new HashSet<>(request.getAssigneeIds()).size()) {
                    Set<Long> resolvedIds = newAssignees.stream()
                        .map(User::getId)
                        .collect(Collectors.toSet());

                    List<Long> invalidIds = request.getAssigneeIds().stream()
                        .distinct()
                        .filter(assigneeId -> !resolvedIds.contains(assigneeId))
                        .collect(Collectors.toList());

                    log.error("Some assignees not found: invalidIds={}", invalidIds);
                    throw new UserNotFoundException("Assignees not found with IDs: " + invalidIds);
                }

                // Kiểm tra tất cả assignees đều active
                List<User> inactiveUsers = newAssignees.stream()
                    .filter(user -> !user.isActive())
                    .collect(Collectors.toList());

                if (!inactiveUsers.isEmpty()) {
                    String inactiveUsernames = inactiveUsers.stream()
                        .map(User::getUsername)
                        .collect(Collectors.joining(", "));
                    throw new BusinessRuleException(
                        "Cannot assign task to inactive users: " + inactiveUsernames
                    );
                }

                task.getAssignees().addAll(newAssignees);
                log.debug("Assignees updated: new count={}", task.getAssignees().size());
                } else {
                    log.warn("Task {} set to UNASSIGNED (empty assignee list)", id);
                }
            }

    // ========== STEP 3: Validate & Update Project (if provided) ==========
        
        if (request.getProjectId() != null) {
           if (!task.getProject().getId().equals(request.getProjectId())) {
                log.debug("Changing project from {} to {}", task.getProject().getId(), request.getProjectId());
                
                Project newProject = projectRepository.findByIdAndActiveTrue(request.getProjectId())
                    .orElseThrow(() -> {
                        log.error("Active project not found: id={}", request.getProjectId());
                        return new ProjectNotFoundException(request.getProjectId());
                    });
                task.setProject(newProject);
                log.info("Task project updated: taskId ={}, oldProjectId={}, newProjectId={}", id, task.getProject().getName(), newProject.getName());
            } else {
                log.debug("Project unchanged: id={}, skipping update");
            }
        }


        if (request.getTitle() != null) {
            log.debug("Updating title: old='{}', new='{}'", task.getTitle(), request.getTitle());
            task.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            log.debug("Updating description (length: {} -> {})", 
                task.getDescription().length(), request.getDescription().length());
            task.setDescription(request.getDescription());
        }

        if (request.getPriority() != null) {
            log.debug("Updating priority: {} -> {}", task.getPriority(), request.getPriority());
            task.setPriority(request.getPriority());
        }

        if (request.getDueDate() != null) {
            if (request.getDueDate().isBefore(LocalDateTime.now())) {
                throw new BusinessRuleException(
                    "Due date must be in the present or future");
            }
            log.debug("Updating due date: {} -> {}", task.getDueDate(), request.getDueDate());
            task.setDueDate(request.getDueDate());
        }

        if (request.getEstimatedHours() != null) {
            log.debug("Updating estimated hours: {} -> {}", task.getEstimatedHours(), request.getEstimatedHours());
            task.setEstimatedHours(request.getEstimatedHours());
        }

        if (request.getNotes() != null) {
            log.debug("Updating notes");
            task.setNotes(request.getNotes());
        }


        if (request.getStatus() != null) {
            TaskStatus oldStatus = task.getStatus();
            TaskStatus newStatus = request.getStatus();

            log.debug("Status transititon: {} -> {}", oldStatus, newStatus);

        // auto-set startDate on first transition to IN_PROGRESS
            if (newStatus == TaskStatus.IN_PROGRESS && task.getStartDate() == null) {
                LocalDateTime startDate = LocalDateTime.now();
                // Guard: startDate must be before dueDate
                if (!startDate.isBefore(task.getDueDate())) {
                    throw new BusinessRuleException(
                        "Cannot start task: current time is at or past the due date ("
                        + task.getDueDate() + ")");
                }
                task.setStartDate(startDate);
                log.info("Task started, set startDate: {}", task.getStartDate());
            }
        
        // Business Rule: Đặt completedAt khi chuyển sang trạng thái COMPLETED
            if (newStatus == TaskStatus.COMPLETED && oldStatus != TaskStatus.COMPLETED) {
                task.setCompletedAt(LocalDateTime.now());
                log.info("Task marked as COMPLETED, set completedAt: {}", task.getCompletedAt());
            }
        
        // Business Rule: Xóa completedAt khi chuyển từ COMPLETED sang trạng thái khác
            if (oldStatus == TaskStatus.COMPLETED && newStatus != TaskStatus.COMPLETED) {
                task.setCompletedAt(null);
                log.info("Task status changed from COMPLETED, cleared completedAt");
            }
            task.setStatus(newStatus);
        }
        
        // JPA Dirty Checking: Hibernate tự động detect changes và generate UPDATE query
        // Không cần gọi repository.save() nếu đang trong @Transactional
        // Nhưng vẫn nên gọi để explicit và dễ hiểu        
        Task updatedTask = taskRepository.save(task);

        log.info("Task updated sucessfully: id={}, updatedAt={}", updatedTask.getId(), updatedTask.getUpdatedAt());

        // Trigger lazy loading trước khi transaction close
        if (!updatedTask.getAssignees().isEmpty()) {
            updatedTask.getAssignees().forEach(user -> user.getUsername());
        }
        updatedTask.getProject().getName();

        TaskResponse response = TaskResponse.from(updatedTask);

        log.debug("TaskResponse created for updated task: id={}", response.getId());

        eventPublisher.publishEvent(
            new TaskEvent(this, TaskEventType.UPDATED, response.getProject().getId(), response)
        );
        auditService.logAction(
            SecurityUtils.getCurrentUserId().orElse(null),
            AuditAction.UPDATE,
            AuditEntityType.TASK,
            updatedTask.getId(),
            "Task updated: '" + updatedTask.getTitle() + "'"
        );
        return response;
    }

    // ==================== DELETE TASK ====================

    /**
     * Xóa task theo ID
     * 
     * Business Flow:
     * 1. Tìm task theo ID (throw exception nếu không tồn tại)
     * 2. Xóa task khỏi database
     * 3. JPA tự động cascade delete comments và attachments
     * 
     * Business Rules:
     * - Task phải tồn tại trong database
     * - Cascade delete: Comments và Attachments sẽ bị xóa theo
     * - Transaction rollback nếu có lỗi
     * 
     * Cascade Delete:
     * - Task entity có @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
     * - Khi xóa Task, Hibernate tự động:
     *   1. DELETE FROM comments WHERE task_id = ?
     *   2. DELETE FROM attachments WHERE task_id = ?
     *   3. DELETE FROM tasks WHERE id = ?
     * 
     * @param id Task ID cần xóa
     * @throws TaskNotFoundException nếu task không tồn tại
     * 
     * HTTP Response:
     * - Success: 204 No Content (không có body)
     * - Task not found: 404 Not Found
     */
    public void deleteTask(Long id) {
        log.info("Deleting task: id={}", id);

        // ========== STEP 1: Validate Task Exists ==========
    
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Task not found for deletion: id={}", id);
                return new TaskNotFoundException(id);
            });

        log.debug("Found task to delete: id={}, title={}, ",
            task.getId(),
            task.getTitle()
        );

        // ========== STEP 2: Delete Task ==========
        Long projectId = task.getProject().getId();
        taskRepository.delete(task);

        log.info("Task deleted successfully: id={}, title={}", id, task.getTitle());
        eventPublisher.publishEvent(
            new TaskEvent(this, projectId, id));
        auditService.logAction(
            SecurityUtils.getCurrentUserId().orElse(null),
            AuditAction.DELETE,
            AuditEntityType.TASK,
            id,
            "Task deleted: '" + task.getTitle() + "' from project ID " + projectId
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<TaskResponse> getAllTasksPaged(Pageable pageable) {
        log.info("Fetching all tasks paged: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<TaskResponse> page = taskRepository.findAll(pageable).map(TaskResponse::from);
        return PagedResponse.from(page);
    }
    // ==================== FUTURE METHODS ====================

    /**
     * Soft delete: Đánh dấu task đã xóa thay vì xóa thật
    public void softDeleteTask(Long id) {
        log.info("Soft deleting task: id={}", id);

        Task task = taskRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));

        // Save thay vi delete
        task.softDelete(); // Đánh dấu deleted = true + set deletedAt

        // Lưu lại
        taskRepository.save(task);
    }

    /**
     * Khôi phục task đã soft delete

    public void restoreTask(Long id) {
        log.info("Restoring soft deleted task: id={}", id);

        // Cần custom query để tìm cả task đã deleted
        Task task = taskRepository.findByIdIncludingDeleted(id)
            .orElseThrow(() -> new TaskNotFoundException(id));
        
        // Khôi phục
        task.restore(); // Đặt deleted = false + xóa deletedAt
    
        taskRepository.save(task);
    }

    
    // Future methods for other features:

    // public List<TaskResponse> getAllTasks() { }
    // public List<TaskResponse> getTasksByProjectId(Long projectId) { }
    // public List<TaskResponse> getTasksByAssigneeId(Long assigneeId) { }
    */
}
