package com.taskmanagement.service;

import com.taskmanagement.dto.response.UserResponse;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;
import com.taskmanagement.entity.TaskStatus;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.CommentRepository;
import com.taskmanagement.exception.UserNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UserService - Service xử lý logic nghiệp vụ liên quan đến User
 * 
 * Mục đích:
 * - Cung cấp các phương thức để quản lý user (tạo, đọc, cập nhật, xóa)
 * - Xử lý các quy tắc nghiệp vụ liên quan đến user
 * - Tương tác với UserRepository để truy xuất dữ liệu
 * 
 * Được sử dụng bởi:
 * - UserController để phục vụ các yêu cầu từ client
 * 
 * Quyết định thiết kế:
 * - Sử dụng DTO UserResponse để trả về dữ liệu an toàn cho client
 * - Quản lý transaction để đảm bảo tính nhất quán dữ liệu
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;

    /**
     * Lấy thông tin user theo ID
     * 
     * @param id ID của user
     * @return UserResponse DTO chứa thông tin user
     * @throws UserNotFoundException nếu không tìm thấy user
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.info("Fetching user by ID: {}", id);

        User user = userRepository.findById(id)     // @Where clause tự động filter deleted
            .orElseThrow(() -> {log.error("User not found with ID: {}",id);
                return new UserNotFoundException(id);
            });

        log.debug("User found: username={}, email={}", user.getUsername(), user.getEmail());

        UserResponse response = UserResponse.from(user);

        log.info("User retrieval successful: userId = {}", response.getId());
        
        return response;
    }

    /**
     * Lấy danh sách tất cả user (chỉ active, không bị deleted)
     * 
     * Business Flow:
     * 1. Query tất cả users từ database
     * 2. Convert từng User entity sang UserResponse
     * 3. Return danh sách UserResponse
     * 
     * @return List<UserResponse> - Danh sách users
     * 
     * Example:
     * GET /api/users
     * Response: [{"id":1,...},{"id":2,...}]
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");

        List<User> users = userRepository.findAll();    // @Where tự động filter

        log.debug("Found {} users", users.size());

        List<UserResponse> responses = users.stream()
            .map(UserResponse::from)
            .collect(Collectors.toList());

        log.info("Retrieval {} users successful", responses.size());

        return responses;
    }

    /**
     * Xóa user và xử lý các tasks của user đó
     * 
     * Business Flow:
     * 1. Tìm user (bao gồm cả deleted)
     * 2. Kiểm tra đã deleted chưa
     * 3. Unassign tất cả tasks (bulk update)
     * 4. Set deleted = true (Hibernate sẽ chạy @SQLDelete)
     * 5. Comments GIỮ NGUYÊN author_id (audit trail)
     * @param id User ID cần xóa
     * @throws UserNotFoundException nếu user không tồn tại
     */
    public void deleteUser(Long id) {
        log.info("Soft deleting user: userId={}", id);

        // STEP 1: Tìm user (bypass @Where để tìm cả deleted users)
        User user = userRepository.findByIdIncludingDeleted(id)
            .orElseThrow(() -> {
                log.error("User not found: userId={}", id);
                return new UserNotFoundException(id);
            });

        // STEP 2: Kiểm tra đã deleted chưa
        if (user.getDeleted()) {
            log.warn("User already deleted: userId={}, username={}", 
                id, user.getUsername());
            throw new IllegalStateException(
                "User '" + user.getUsername() + "' has already been deleted"
            );
        }

        log.debug("User found: username={}, email={}", 
            user.getUsername(), user.getEmail());

        // STEP 3: Count resources
        long taskCount = taskRepository.countByAssigneeId(id);
        long commentCount = commentRepository.countByAuthorId(id);
        long projectCount = userRepository.countOwnedProjects(id);

        log.info("User statistics: tasks={}, comments={}, projects={}", 
            taskCount, commentCount, projectCount);

        // STEP 4: Unassign tasks (bulk update - không load entities)
        if (taskCount > 0) {
            int unassigned = taskRepository.unassignTasksByUserId(id);
            log.info("Unassigned {} tasks", unassigned);
        }

        // STEP 5: Soft delete user
        // Comments GIỮ NGUYÊN author_id (audit trail)
        // Projects GIỮ NGUYÊN owner_id (business continuity)
        
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        // user.setDeletedBy(getCurrentAdminUserId());  // TODO: Get from SecurityContext

        userRepository.save(user);  // Trigger @SQLDelete

        log.info("User soft deleted successfully: " +
                "userId={}, username={}, " +
                "unassigned_tasks={}, " +
                "preserved_comments={}, " +
                "preserved_projects={}", 
            id, user.getUsername(), taskCount, commentCount, projectCount);
    }

    /**
     * Restore user đã bị soft delete
     * 
     * @param id User ID
     */
    public void restoreUser(Long id) {
        log.info("Restoring user: userId={}", id);

        User user = userRepository.findByIdIncludingDeleted(id)
            .orElseThrow(() -> new UserNotFoundException(id));

        if (!user.getDeleted()) {
            throw new IllegalStateException(
                "User '" + user.getUsername() + "' is not deleted"
            );
        }

        user.setDeleted(false);
        user.setDeletedAt(null);
        user.setDeletedBy(null);

        userRepository.save(user);

        log.info("User restored successfully: userId={}, username={}", 
            id, user.getUsername());
    }

}
