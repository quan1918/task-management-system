package com.taskmanagement.api;

import com.taskmanagement.dto.request.CreateTaskRequest;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * TaskController - REST API controller cho quản lý Task
 * 
 * Chịu trách nhiệm:
 * - Cung cấp các REST endpoint cho các thao tác với task
 * - Parse HTTP request và validate dữ liệu đầu vào
 * - Giao phần xử lý business logic cho TaskService
 * - Định dạng HTTP response với status code phù hợp
 * - Xử lý API versioning và routing
 * 
 * Thiết kế API:
 * - Base path: /api/tasks
 * - Tuân theo chuẩn RESTful
 * - Sử dụng HTTP status codes (201 Created, 400 Bad Request, ...)
 * - Giao tiếp qua JSON theo chuẩn request/response
 * 
 * @author Task Management System
 * @version 1.0 (Tính năng Create Task)
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    // ==================== DEPENDENCIES ====================

    /**
     * Dependency TaskService (được inject qua constructor)
     * 
     * @RequiredArgsConstructor tạo constructor tự động
     * Spring sẽ inject TaskService vào controller
     */
    private final TaskService taskService;

    // ==================== CREATE TASK ENDPOINT ====================
    
    /**
     * Tạo task mới
     * 
     * Endpoint: POST /api/tasks
     * 
     * Request:
     * - Method: POST
     * - Content-Type: application/json
     * - Body: CreateTaskRequest (JSON)
     * 
     * Response:
     * - Thành công: 201 Created
     *   - Location header: /api/tasks/{id}
     *   - Body: TaskResponse (JSON)
     * - Lỗi validation: 400 Bad Request
     * - Không tìm thấy User: 404 Not Found
     * - Không tìm thấy Project: 404 Not Found
     * - Lỗi server: 500 Internal Server Error
     * 
     * Ví dụ Request:
     * POST /api/tasks
     * Content-Type: application/json
     * 
     * {
     *   "title": "Fix login bug",
     *   "description": "Users cannot login with special characters in password",
     *   "priority": "HIGH",
     *   "dueDate": "2025-12-15T17:00:00",
     *   "estimatedHours": 8,
     *   "notes": "Check password validation regex",
     *   "assigneeId": 5,
     *   "projectId": 3
     * }
     * 
     * Ví dụ Response:
     * HTTP/1.1 201 Created
     * Location: /api/tasks/123
     * Content-Type: application/json
     * 
     * {
     *   "id": 123,
     *   "title": "Fix login bug",
     *   "description": "Users cannot login...",
     *   "status": "PENDING",
     *   "priority": "HIGH",
     *   "dueDate": "2025-12-15T17:00:00",
     *   "assignee": {
     *     "id": 5,
     *     "username": "johndoe",
     *     "fullName": "John Doe",
     *     "email": "john@example.com"
     *   },
     *   "project": {
     *     "id": 3,
     *     "name": "Website Redesign",
     *     "active": true
     *   },
     *   "createdAt": "2025-12-04T10:30:00",
     *   "updatedAt": "2025-12-04T10:30:00"
     * }
     * 
     * @param request CreateTaskRequest DTO (validated tự động)
     * @return ResponseEntity với 201 Created và TaskResponse
     */
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request) {
        
        log.info("POST /api/tasks - Creating task: title={}", request.getTitle());

    // ========== STEP 1: Gọi Service (Business Logic) ==========

        TaskResponse response = taskService.createTask(request);

        log.info("Task created successfully: id={}", response.getId());

    // ========== STEP 2: Tạo Location Header ==========

    // Best practice REST: Trả về Location header với URI của resource mới tạo
    // VD: Location: http://localhost:8080/api/tasks/123

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest() // Lấy URI hiện tại: /api/tasks
            .path("/{id}")        // Thêm /{id}
            .buildAndExpand(response.getId()) // Thay {id} bằng ID thực tế
            .toUri();            // Build URI
        
        log.debug("Location header: {}", location);

    // ========== STEP 3: Trả Response 201 Created ==========

    // ResponseEntity.created():
    // - Set status: 201 Created
    // - Thêm Location header
    // - Set body response
        return ResponseEntity
                .created(location)
                .body(response);
    }

    // ==================== FUTURE ENDPOINTS ====================

    // Các endpoint tương lai cho các tính năng khác:

    /**
     * Lấy task theo ID
     * GET /api/tasks/{id}
     */
    // @GetMapping("/{id}")
    // public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
    //     TaskResponse response = taskService.getTaskById(id);
    //     return ResponseEntity.ok(response);
    // }

    /**
     * Lấy danh sách task (kèm filter)
     * GET /api/tasks?status=PENDING&assigneeId=5
     */
    // @GetMapping
    // public ResponseEntity<List<TaskResponse>> getAllTasks(
    //         @RequestParam(required = false) String status,
    //         @RequestParam(required = false) Long assigneeId) {
    //     List<TaskResponse> tasks = taskService.getAllTasks(status, assigneeId);
    //     return ResponseEntity.ok(tasks);
    // }

    /**
     * Update task
     * PUT /api/tasks/{id}
     */
    // @PutMapping("/{id}")
    // public ResponseEntity<TaskResponse> updateTask(
    //         @PathVariable Long id,
    //         @Valid @RequestBody UpdateTaskRequest request) {
    //     TaskResponse response = taskService.updateTask(id, request);
    //     return ResponseEntity.ok(response);
    // }

    /**
     * Xóa task
     * DELETE /api/tasks/{id}
     */
    // @DeleteMapping("/{id}")
    // public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
    //     taskService.deleteTask(id);
    //     return ResponseEntity.noContent().build();
    // }
}
    

