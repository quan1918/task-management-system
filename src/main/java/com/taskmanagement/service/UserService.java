package com.taskmanagement.service;

import com.taskmanagement.dto.request.CreateUserRequest;
import com.taskmanagement.dto.request.UpdateUserRequest;
import com.taskmanagement.dto.response.UserResponse;
import com.taskmanagement.entity.User;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.exception.DuplicateResourceException;
import com.taskmanagement.exception.UserNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ==================== CREATE USER ====================
    
    /**
     * Tạo user mới
     * 
     * Business Flow:
     * 1. Validate username và email chưa tồn tại
     * 2. Hash password bằng BCrypt
     * 3. Set default values (active=true, createdAt)
     * 4. Lưu vào database
     * 5. Return UserResponse
     * 
     * Business Rules:
     * - Username phải unique
     * - Email phải unique
     * - Password được hash bằng BCrypt (không lưu plain text)
     * - User mới mặc định active = true
     * - lastLoginAt = null (chưa login lần nào)
     * 
     */

    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating new user: username={}, email={}", request.getUsername(), request.getEmail());
        
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        if (!User.isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        // STEP 1: Validate username chưa tồn tại
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.error("Username already exists: {}", request.getUsername());
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }

        // STEP 2: Validate email chưa tồn tại
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.error("Email already exists: {}", request.getEmail());
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        // STEP 3: Hash password bằng BCrypt
        String hasedPassword = passwordEncoder.encode(request.getPassword());
        log.debug("Password hashed successfully for user: {}", request.getUsername());

        // STEP 4: Tạo User entity và set default values
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(hasedPassword)
            .fullName(request.getFullName())
            .active(true)  
            .createdAt(LocalDateTime.now())
            .build();

        // STEP 5: Lưu vào database
        User savedUser = userRepository.save(user);

        log.info("User created successfully: userId={}, username={}", savedUser.getId(), savedUser.getUsername());

        // STEP 6: Convert sang UserResponse và return
        return mapToResponse(savedUser);
    }

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

        User user = userRepository.findById(id)     
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

        List<User> users = userRepository.findAll();    

        log.debug("Found {} users", users.size());

        List<UserResponse> responses = users.stream()
            .map(UserResponse::from)
            .collect(Collectors.toList());

        log.info("Retrieval {} users successful", responses.size());

        return responses;
    }

    /** ==================== UPDATE USER ====================
    
    /**
     * Cập nhật thông tin user
     * 
     * Business Flow:
     * 1. Tìm user theo ID
     * 2. Validate email mới (nếu thay đổi) phải unique
     * 3. Update các field không null trong request
     * 4. Set updatedAt = now()
     * 5. Lưu vào database
     * 6. Return UserResponse
     * 
     * Business Rules:
     * - User phải tồn tại
     * - Username KHÔNG thể thay đổi (immutable)
     * - Email mới phải unique (nếu thay đổi)
     * - Chỉ update field != null (partial update)
     * - Password update qua endpoint riêng (security)
     * 
     * @param id User ID
     * @param request UpdateUserRequest với các field cần update
     * @return UserResponse DTO sau khi update
     * @throws UserNotFoundException nếu user không tồn tại
     * @throws DuplicateResourceException nếu email mới đã tồn tại
     * 
     * Example:
     * PUT /api/users/10
     * {
     *   "email": "newemail@example.com",
     *   "fullName": "John Smith"
     * }
     * 
     * Response 200 OK:
     * {
     *   "id": 10,
     *   "username": "john_doe",  // KHÔNG đổi
     *   "email": "newemail@example.com",  // ĐÃ đổi
     *   "fullName": "John Smith",  // ĐÃ đổi
     *   "active": true,
     *   "updatedAt": "2025-12-17T11:00:00"
     * }
     */
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        log.info("Updating user: userId={}", id);

        if (request == null) {
        throw new IllegalArgumentException("UpdateUserRequest cannot be null");
        }

        // STEP 1: Tìm user theo ID
        User user = userRepository.findById(id)
            .orElseThrow(() -> {
                log.error("User not found: userId={}", id);
                return new UserNotFoundException(id);
            });

        log.debug("User found: userId={}, username={}", user.getId(), user.getUsername());

        // STEP 2: Validate email mới (nếu thay đổi)
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Email đã thay đổi, kiểm tra trùng lặp
            if(userRepository.findByEmail(request.getEmail()).isPresent()) {
                log.error("Email already exists: {}", request.getEmail());
                throw new DuplicateResourceException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
            log.debug("Email updated: {}", request.getEmail());
        }

        // STEP 3: Update full name
        if (request.getFullName() != null) {
            if (request.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be empty");
            }
            user.setFullName(request.getFullName());
            log.debug("Full name updated: {}", request.getFullName());
        }

        // STEP 4: Update active status
        if (request.getActive() != null) {
            user.setActive(request.getActive());
            log.debug("Active status updated: {}", request.getActive());
        }

        // STEP 5: Set Updated Timestamp
        user.setUpdatedAt(LocalDateTime.now());

        // STEP 6: Lưu vào database
        User updatedUser = userRepository.save(user);

        log.info("User updated successfully: userId={}", updatedUser.getId());

        return mapToResponse(updatedUser);

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

        // STEP 3: Unassign tasks
        int removedCount = taskRepository.unassignTasksByUserId(id);

        // STEP 4: Soft delete user
        
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
  
        log.info("User soft deleted : userId={}, username={}, removed_tasks={}", id, user.getUsername(), removedCount);
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

    // ==================== PRIVATE METHODS ====================
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .active(user.getActive())
            .lastLoginAt(user.getLastLoginAt())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }

}
