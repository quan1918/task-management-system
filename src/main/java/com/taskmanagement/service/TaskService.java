package com.taskmanagement.service;

import com.taskmanagement.dto.request.CreateTaskRequest;
import com.taskmanagement.dto.request.UpdateTaskRequest;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.entity.Project;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.TaskStatus;
import com.taskmanagement.entity.User;
import com.taskmanagement.exception.BusinessRuleException;
import com.taskmanagement.exception.ProjectNotFoundException;
import com.taskmanagement.exception.TaskNotFoundException;
import com.taskmanagement.exception.UserNotFoundException;
import com.taskmanagement.repository.ProjectRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        
    // ========== STEP 1: Validate Assignee Exists ==========
    
        // Lấy assignee từ database
        Set<User> assignees = new HashSet<>(
            userRepository.findAllById(request.getAssigneeIds())
        );

        // Kiểm tra tất cả assignees có được tìm thấy không
        if (assignees.size() != request.getAssigneeIds().size()) {
            Set<Long> foundIds = assignees.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

            List<Long> missingIds = request.getAssigneeIds().stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toList());

            log.error("Some assignees not found: missingIds={}", missingIds);
            throw new UserNotFoundException("Assignees not found with IDs: " + missingIds);
        }

        // Kiểm tra tất cả assignees đều active
        List<User> inactiveUsers = assignees.stream()
            .filter(user -> !user.getActive())
            .collect(Collectors.toList());

        if (!inactiveUsers.isEmpty()) {
            String inactiveUsernames = inactiveUsers.stream()
                .map(User::getUsername)
                .collect(Collectors.joining(", "));
            log.error("Cannot assign task to inactive users: " + inactiveUsernames);
            throw new BusinessRuleException("Cannot assign task to inactive users: " + inactiveUsernames);
        }

        log.debug("All {} assignees validated successfully", assignees.size());
        
    // ========== STEP 2: Validate Project Exists & Active ==========

        Project project = projectRepository.findByIdAndActiveTrue(request.getProjectId())
            .orElseThrow(() -> {
                log.error("Active project not found: id={}", request.getProjectId());
                return new ProjectNotFoundException(request.getProjectId());
            });
        
        log.debug("Project found: projectId={}, active={}", project.getId(), project.getActive());
       
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
            .status(TaskStatus.PENDING) // Mặc định trạng thái là PENDING
            .build();

        log.debug("Task entity created: {}", task);
    
    // ========== STEP 4: Save to Database ==========

        Task savedTask = taskRepository.save(task);

        log.info("Task saved succesfully: taskId={}, title={}", savedTask.getId(), savedTask.getTitle());

    // ========== STEP 5: Convert to Response DTO ==========


    // ========== STEP 7: Return Response ==========
        
        return getTaskById(savedTask.getId());
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
        log.info("Fetching task by ID: {} with separate assignees query", id);

    // ========== STEP 1: Find Task in Database ==========
        Task task = taskRepository.findByIdNative(id)
            .orElseThrow(() -> {
                log.error("Task not found: id={}", id);
                return new TaskNotFoundException(id);
            });
        
        // WORKAROUND: Load assignees separately to bypass @Where filter issue
        List<Long> assigneeIds = taskRepository.findAssigneeIdsByTaskId(id);
        List<User> assignees = userRepository.findAllById(assigneeIds);
        task.getAssignees().clear();
        task.getAssignees().addAll(assignees);
        
        log.info("Task found: id={}, title={}, assigneesSize={}", 
            task.getId(), task.getTitle(), task.getAssignees().size());

    // ========== STEP 3: Convert Entity → DTO ==========
        TaskResponse response = TaskResponse.from(task);

        log.info("Task retrieved successfully: taskId={}", id);
    
    // ========== STEP 4: Return Response ==========
        return response;
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
     * @param id Task ID cần update
     * @param request UpdateTaskRequest với các field cần thay đổi
     * @return TaskResponse DTO với dữ liệu sau khi update
     * @throws TaskNotFoundException nếu task không tồn tại
     * @throws UserNotFoundException nếu assignee mới không tồn tại
     * @throws ProjectNotFoundException nếu project mới không tồn tại/inactive
     * 
     * Example Usage:
     * UpdateTaskRequest request = UpdateTaskRequest.builder()
     *     .status(TaskStatus.IN_PROGRESS)
     *     .priority(TaskPriority.HIGH)
     *     .build();
     * TaskResponse updated = taskService.updateTask(123L, request);
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
                Set<User> newAssignees = new HashSet<>(
                    userRepository.findAllById(request.getAssigneeIds())
                );

                // Kiểm tra tất cả assignees có được tìm thấy không
                if (newAssignees.size() != request.getAssigneeIds().size()) {
                    Set<Long> foundIds = newAssignees.stream()
                        .map(User::getId)
                        .collect(Collectors.toSet());

                    List<Long> missingIds = request.getAssigneeIds().stream()
                        .filter(assigneeId -> !foundIds.contains(assigneeId))
                        .collect(Collectors.toList());

                    log.error("Some assignees not found: missingIds={}", missingIds);
                    throw new UserNotFoundException("Assignees not found with IDs: " + missingIds);
                }

                // Kiểm tra tất cả assignees đều active
                List<User> inactiveUsers = newAssignees.stream()
                    .filter(user -> !user.getActive())
                    .collect(Collectors.toList());

                if (!inactiveUsers.isEmpty()) {
                    String inactiveUsernames = inactiveUsers.stream()
                        .map(User::getUsername)
                        .collect(Collectors.joining(", "));
                    log.error("Cannot assign to inactive users: {}", inactiveUsernames);
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

    // ========== STEP 4: Update Basic Fields (partial update) ==========

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

    // ========== STEP 5: Update Status with Business Logic ==========

        if (request.getStatus() != null) {
            TaskStatus oldStatus = task.getStatus();
            TaskStatus newStatus = request.getStatus();

            log.debug("Status transititon: {} -> {}", oldStatus, newStatus);

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

    // ========== STEP 6: Save to Database ==========
        
        // JPA Dirty Checking: Hibernate tự động detect changes và generate UPDATE query
        // Không cần gọi repository.save() nếu đang trong @Transactional
        // Nhưng vẫn nên gọi để explicit và dễ hiểu        
        Task updatedTask = taskRepository.save(task);

        log.info("Task updated sucessfully: id={}, updatedAt={}", updatedTask.getId(), updatedTask.getUpdatedAt());

    // ========== STEP 7: Convert to Response DTO ==========
    
        // Trigger lazy loading trước khi transaction close
        if (updatedTask.getAssignees() != null && !updatedTask.getAssignees().isEmpty()) {
            updatedTask.getAssignees().forEach(user -> user.getUsername());
        }
        updatedTask.getProject().getName();

        TaskResponse response = TaskResponse.from(updatedTask);

        log.debug("TaskResponse created for updated task: id={}", response.getId());

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
     * Example Usage:
     * taskService.deleteTask(123L);
     * // Task #123, comments, và attachments đều bị xóa
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

        log.debug("Found task to delete: id={}, title={}, commentCount={}, attachmentCount={}",
            task.getId(),
            task.getTitle(),
            task.getComments() !=null ? task.getComments().size() : 0,
            task.getAttachments() !=null ? task.getAttachments().size() : 0
        );

        // ========== STEP 2: Log Related Data (for audit) ==========

        // Log để audit trail (biết task nào bị xóa)

        if (task.getComments() != null && !task.getComments().isEmpty()) {
            log.info("Task has {} comments that will be cascade deleted", task.getComments().size());
        }

        if (task.getAttachments() != null && !task.getAttachments().isEmpty()) {
            log.info("Task has {} attachment that will be cascade deleted", task.getAttachments().size());
        }

        // ========== STEP 3: Delete Task ==========
    
        // JPA/Hibernate sẽ tự động:
        // 1. DELETE FROM comments WHERE task_id = ?
        // 2. DELETE FROM attachments WHERE task_id = ?
        // 3. DELETE FROM tasks WHERE id = ?

        taskRepository.delete(task);

        log.info("Task deleted successfully: id={}, title={}", id, task.getTitle());

        // Transaction commit tự động khi method kết thúc (@Transactional)
        // Nếu có exception, transaction sẽ rollback
    }

    // ==================== SOFT DELETE ====================

    /**
     * Soft delete: Đánh dấu task đã xóa thay vì xóa thật
     * 
     * Ưu điểm:
     * - Có thể khôi phục task đã xóa
     * - Giữ lại lịch sử cho audit
     * - An toàn hơn (tránh xóa nhầm)
     * 
     * Cần thêm:
     * - Task entity: boolean deleted + LocalDateTime deletedAt
     * - Repository: @Query filter deleted = false
     * 
     * @param id Task ID
     */
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
     * 
     * @param id Task ID
     */

    public void restoreTask(Long id) {
        log.info("Restoring soft deleted task: id={}", id);

        // Cần custom query để tìm cả task đã deleted
        Task task = taskRepository.findByIdIncludingDeleted(id)
            .orElseThrow(() -> new TaskNotFoundException(id));
        
        // Khôi phục
        task.restore(); // Đặt deleted = false + xóa deletedAt
    
        taskRepository.save(task);
    }

    // ==================== HELPER METHOD (OPTIONAL) ====================
    
    /**
     * Kiểm tra task có tồn tại không
     * 
     * Helper method cho các service khác
     * 
     * @param id Task ID
     * @return true nếu task tồn tại
     */
    public boolean taskExists(Long id) {
        return taskRepository.existsById(id);
    }
    
    /**
     * Lấy task entity (internal use)
     * 
     * Dùng cho internal service logic, không expose ra controller
     * 
     * @param id Task ID
     * @return Task entity
     * @throws TaskNotFoundException nếu không tìm thấy
     */
    protected Task getTaskEntityById(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));
    }

    /**
     * Validate status transition rules
     * 
     * Optional business logic để prevent invalid status transitions
     * Ví dụ:
     * - Không cho phép CANCELLED → IN_PROGRESS
     * - Không cho phép COMPLETED → PENDING (reopen task)
     * 
     * @param oldStatus Current status
     * @param newStatus Desired new status
     * @throws IllegalStateException nếu transition không hợp lệ
     */
    private void validateStatusTransition(TaskStatus oldStatus, TaskStatus newStatus) {
        // Example: Ngăn chặn việc mở lại các tác vụ đã hoàn thành
        if (oldStatus == TaskStatus.COMPLETED && newStatus == TaskStatus.PENDING) {
            log.error("Invalid status transition: Cannot reopen completed task");
            throw new IllegalStateException(
                "Cannot change status from COMPLETED to PENDING. Task cannot be reopened."
            );
        }
        
        // Example: Ngăn chặn việc bắt đầu các tác vụ đã bị hủy
        if (oldStatus == TaskStatus.CANCELLED && newStatus == TaskStatus.IN_PROGRESS) {
            log.error("Invalid status transition: Cannot start cancelled task");
            throw new IllegalStateException(
                "Cannot change status from CANCELLED to IN_PROGRESS. Create a new task instead."
            );
        }
        
        // Thêm các quy tắc khác khi cần thiết.
        log.debug("Status transition validated: {} -> {}", oldStatus, newStatus);
    }
    // ==================== FUTURE METHODS ====================
    
    // Future methods for other features:
    
    // public void deleteTask(Long id) { }
    // public TaskResponse getTaskById(Long id) { }
    // public List<TaskResponse> getAllTasks() { }
    // public List<TaskResponse> getTasksByProjectId(Long projectId) { }
    // public List<TaskResponse> getTasksByAssigneeId(Long assigneeId) { }
}
