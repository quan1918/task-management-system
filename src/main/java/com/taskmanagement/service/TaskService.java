package com.taskmanagement.service;

import com.taskmanagement.dto.request.CreateTaskRequest;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.entity.Project;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.TaskStatus;
import com.taskmanagement.entity.User;
import com.taskmanagement.exception.ProjectNotFoundException;
import com.taskmanagement.exception.UserNotFoundException;
import com.taskmanagement.repository.ProjectRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    // ==================== FUTURE METHODS ====================
    
    // Future methods for other features:
    
    // public TaskResponse updateTask(Long id, UpdateTaskRequest request) { }
    // public void deleteTask(Long id) { }
    // public TaskResponse getTaskById(Long id) { }
    // public List<TaskResponse> getAllTasks() { }
    // public List<TaskResponse> getTasksByProjectId(Long projectId) { }
    // public List<TaskResponse> getTasksByAssigneeId(Long assigneeId) { }
}
