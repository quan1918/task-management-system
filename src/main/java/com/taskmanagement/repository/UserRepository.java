package com.taskmanagement.repository;

import com.taskmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository - Tầng truy cập dữ liệu cho thực thể User
 * 
 * Mục đích (đối với tính năng Create Task):
 * - Xác thực rằng assignee (người được giao việc) tồn tại trước khi tạo task
 * - Lấy thực thể User để gán làm assignee của task
 * 
 * Các phương thức cần cho Create Task:
 * - findById(Long id) – Kế thừa từ JpaRepository 
 * - existsById(Long id) – Kế thừa từ JpaRepository
 * 
 * Các future method (tạm bỏ qua):
 * - findByEmail(), findByUsername(), searchUsers(), v.v.
 * 
 * Tại sao giữ mức tối thiểu?
 * - Áp dụng nguyên tắc YAGNI: Chỉ tạo những gì thực sự cần bây giờ
 * - Dễ dàng bổ sung thêm phương thức khi cần ở các tính năng sau
 * - Giữ mọi thứ đơn giản cho giai đoạn hiện tại
 * 
 * @author Task Management System
 * @version 1.0 (Tối giản cho Create Task)
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>{
    // ==================== CÁC PHƯƠNG THỨC KẾ THỪA ====================
//
// Từ JpaRepository<User, Long>:
// - Optional<User> findById(Long id)
// - boolean existsById(Long id)
// - User save(User user)
// - void delete(User user)
// - List<User> findAll()
// - long count()
//
//
// ==================== CHƯA CẦN CUSTOM METHOD ====================
//
// Đối với chức năng Create Task, chúng ta chỉ cần:
// 1. findById(Long id) - để lấy thực thể User
// 2. existsById(Long id) - để kiểm tra User có tồn tại hay không
//
// Cả hai phương thức đều đã được JpaRepository cung cấp!
// Không cần viết thêm phương thức nào tại đây.
//
// Các phương thức bổ sung có thể thêm sau khi phát triển các tính năng khác:
// Optional<User> findByEmail(String email);
// Optional<User> findByUsername(String username);
// boolean existsByEmail(String email);
// List<User> findByActiveTrue();
}
