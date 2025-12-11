package com.taskmanagement.service;

import com.taskmanagement.dto.request.CreateTaskRequest;
import com.taskmanagement.dto.request.UpdateTaskRequest;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.entity.Project;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.TaskStatus;
import com.taskmanagement.entity.User;
import com.taskmanagement.exception.ProjectNotFoundException;
import com.taskmanagement.exception.TaskNotFoundException;
import com.taskmanagement.exception.UserNotFoundException;
import com.taskmanagement.repository.ProjectRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

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
            request.getTitle(), request.getAssigneeId(), request.getProjectId());
        
    // ========== STEP 1: Validate Assignee Exists ==========
    
        User assignee = userRepository.findById(request.getAssigneeId())
            .orElseThrow(() -> {
                log.error("Assignee not found: id={}", request.getAssigneeId());
                return new UserNotFoundException(request.getAssigneeId());
            });
    
    // ========== STEP 2: Validate Project Exists & Active ==========

        Project project = projectRepository.findByIdAndActiveTrue(request.getProjectId())
            .orElseThrow(() -> {
                log.error("Active project not found: id={}", request.getProjectId());
                return new ProjectNotFoundException(request.getProjectId());
            });
        
        log.debug("Project found: projectId={}, active={}", project.getId(), project.getName(), project.getActive());
    
    // ========== STEP 3: Business Validations (Optional) ==========

    // Tương lai: thêm các business rule khác tại đây
    // Ví dụ: Kiểm tra workload của assignee quá cao
    // Ví dụ: Kiểm tra project đã đạt giới hạn số lượng task
    // Ví dụ: Validate due date nằm trong phạm vi timeline của project}
    
    // ========== STEP 4: Create Task Entity ==========

        Task task = Task.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .priority(request.getPriority())
            .dueDate(request.getDueDate())
            .estimatedHours(request.getEstimatedHours())
            .notes(request.getNotes())
            .assignee(assignee)
            .project(project)
            .status(TaskStatus.PENDING) // Mặc định trạng thái là PENDING
            .build();

        log.debug("Task entity created: {}", task);
    
    // ========== STEP 5: Save to Database ==========

        Task savedTask = taskRepository.save(task);

        log.info("Task saved succesfully: taskId={}, title={}", savedTask.getId(), savedTask.getTitle());

    // ========== STEP 6: Convert to Response DTO ==========
        TaskResponse response = TaskResponse.from(savedTask);
    
        log.debug("TaskResponse created: taskId={}, assignee={}, project={}",
            response.getId(),
            response.getAssignee().getUsername(),
            response.getProject().getName());

    // ========== STEP 7: Return Response ==========
        
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
     * Business Rules:
     * - Task phải tồn tại trong database
     * - Trả về đầy đủ thông tin task kèm assignee và project
     * - Lazy loading được xử lý trong transaction
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

    // ========== STEP 1: Find Task in Database ==========
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Task not found: id={}", id);
                return new TaskNotFoundException(id);
            });
        
        log.debug("Task found: task_id={}, title={}, status={}", task.getId(), task.getTitle(), task.getStatus());

    // ========== STEP 2: Trigger Lazy Loading ==========
        String assigneeName = task.getAssignee().getFullName();
        String projectName = task.getProject().getName();

        log.debug("Loaded relationships - Assignee: {}, Project: {}", assigneeName, projectName);

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
        
        if (request.getAssigneeId() !=null) {
            // Chỉ query database nếu assignee thực sự thay đổi
            if(!task.getAssignee().getId().equals(request.getAssigneeId())) {
                log.debug("Changing assignee from {} to {}", task.getAssignee().getId(), request.getAssigneeId());
            
            User newAssignee = userRepository.findById(request.getAssigneeId())
                .orElseThrow(() -> {
                    log.error("New assignee not found: id={}",request.getAssigneeId());
                    return new UserNotFoundException(request.getAssigneeId());
                });

            task.setAssignee(newAssignee);
            log.info("Task assignee updated: taskId= {}, oldAssigneeId= {}, neAssignee={}", id, task.getAssignee().getUsername(), newAssignee.getUsername());
            } else {
                log.debug("Assignee unchanged: id={}, skipping update");
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
        updatedTask.getAssignee().getUsername();
        updatedTask.getProject().getName();

        TaskResponse response = TaskResponse.from(updatedTask);

        log.debug("TaskResponse created for updated task: id={}", response.getId());

        return response;
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
