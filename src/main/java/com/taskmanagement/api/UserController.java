package com.taskmanagement.api;

import com.taskmanagement.dto.request.CreateUserRequest;
import com.taskmanagement.dto.request.UpdateUserRequest;
import com.taskmanagement.dto.response.UserResponse;
import com.taskmanagement.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UserController - Controller xử lý các yêu cầu HTTP liên quan đến User
 * 
 * Mục đích:
 * - Cung cấp các endpoint API để quản lý user (tạo, đọc, cập nhật, xóa)
 * - Nhận và trả về dữ liệu dưới dạng JSON
 * - Tương tác với UserService để xử lý logic nghiệp vụ
 * 
 * Quyết định thiết kế:
 * - Sử dụng RESTful API conventions
 * - Trả về dữ liệu qua DTO UserResponse để đảm bảo an toàn
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;

    // ==================== CREATE USER ====================
    
    /**
     * Tạo user mới
     * 
     * Endpoint: POST /api/users
     * 
     * Request:
     * POST /api/users
     * Content-Type: application/json
     * Authorization: Bearer <token>
     * 
     * Body:
     * {
     *   "username": "john_doe",
     *   "email": "john@example.com",
     *   "password": "SecurePass123!",
     *   "fullName": "John Doe"
     * }
     * 
     * 
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Chỉ admin mới có quyền tạo user mới
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("POST /users - Creating new user: {}", request.getUsername());

        //STEP 1: Gọi Service (Business Logic)
        UserResponse response = userService.createUser(request);
        
        log.info("User created successfully: userId ={}, username={}", response.getId(), response.getUsername());

        //STEP 2: Trả về Response
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lấy danh sách tất cả users
     * 
     * Endpoint: GET /api/users
     * 
     * Request:
     * - Method: GET
     * - URL: /api/users
     * - Authentication: Required (Bearer Token)
     * 
     * 
     * Example Request:
     * GET /api/users
     * Authorization: Bearer <token>
     * 
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUser() {
        log.info("GET /users - Fetching all users");

        //STEP 1: Gọi Service
        List<UserResponse> responses = userService.getAllUsers();

        log.info("Retrieved {} users successfully", responses.size());

        //STEP 2: Trả về Response
        return ResponseEntity.ok(responses);
    }

/**
     * Lấy thông tin user theo ID
     * 
     * Endpoint: GET /api/users/{id}
     * 
     * Request:
     * - Method: GET
     * - URL: /api/users/{id}
     * - Path Variable: id (Long) - User ID
     * - Authentication: Required (Bearer Token)
     * 
     * 
     * Example Request:
     * GET /api/users/1
     * Authorization: Bearer <token>
     * 
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("GET /users/{} - Fetching user by ID", id);
    
        //STEP 1: Gọi Service (Business Logic)
        UserResponse response = userService.getUserById(id);

        log.info("User retrieval successful: userId = {}, username ={}", response.getId(), response.getUsername());

        //STEP 2: Trả về Response
        return ResponseEntity.ok(response);
    }

    // ==================== UPDATE USER ====================
    
    /**
     * Cập nhật user
     * 
     * Endpoint: PATCH /api/users/{id}
     * 
     * Request:
     * PATCH /api/users/10
     * Content-Type: application/json
     * Authorization: Bearer <token>
     * 
     * Body (Partial Update):
     * {
     *   "email": "newemail@example.com",
     *   "fullName": "John Smith"
     * }
     * 
     */
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        log.info("PATCH /users/{} - Updating user: {}", id, request);

        //STEP 1: Gọi Service
        UserResponse response = userService.updateUser(id, request);

        log.info("User updated successfully: userId={}, username={}", response.getId(), response.getUsername());

        //STEP 2: Trả về Response
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa user
     * 
     * Endpoint: DELETE /api/users/{id}
     * 
     * Business Rules:
     * - Tất cả tasks của user sẽ được unassigned
     * - Task status → UNASSIGNED
     * - Admin cần reassign tasks sau đó
     * 
     * Request:
     * DELETE /api/users/2
     * Authorization: Bearer <token>
     * 
     */
    @DeleteMapping("/{id}")    
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("DELETE /users/{} - Deleting user", id);

        userService.deleteUser(id);

        log.info("User deleted successfully: userId={}", id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Restore deleted user
     * 
     * POST /api/users/{id}/restore
     * 
     * Response: 200 OK
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<UserResponse> restoreUser(@PathVariable Long id) {
        log.info("POST /users/{}/restore - Restoring user", id);

        userService.restoreUser(id);
        UserResponse response = userService.getUserById(id);

        log.info("User restored successfully: userId={}", id);

        return ResponseEntity.ok(response);
    }
}
