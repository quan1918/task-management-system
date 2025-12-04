package com.taskmanagement.repository;

import com.taskmanagement.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
/**
 * ProjectRepository - Tầng truy cập dữ liệu cho thực thể Project
 * 
 * Mục đích (đối với tính năng Create Task):
 * - Xác thực rằng project tồn tại trước khi tạo task
 * - Lấy thực thể Project để gán cho task
 * 
 * Các phương thức cần cho Create Task:
 * - findById(Long id) – Kế thừa từ JpaRepository 
 * - existsById(Long id) – Kế thừa từ JpaRepository
 * 
 * Custom methods for Create Task:
 * - findByIdAndActiveTrue(Long id) - Chỉ lấy nếu dự án đang hoạt động
 * 
 * Các future method (tạm bỏ qua):
 * - findByName(), findByStatus(), v.v.
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
public interface ProjectRepository extends JpaRepository<Project, Long> {

// ==================== CÁC PHƯƠNG THỨC KẾ THỪA ====================
/**
* Tìm project theo ID chỉ khi project đang ở trạng thái active
*
* Truy vấn được tự động tạo:
* SELECT * FROM projects WHERE id = ? AND active = true
*
* Mục đích: Quy tắc nghiệp vụ cho Create Task
* - Không được tạo task trong project đã inactive/archived
* - Kiểm tra project tồn tại VÀ active chỉ trong một truy vấn
*
* Ví dụ sử dụng trong TaskService.createTask():
* Project project = projectRepository.findByIdAndActiveTrue(projectId)
*     .orElseThrow(() -> new ProjectNotFoundException("Project not found or inactive"));
*
* @param id ID của Project
* @return Optional<Project> (rỗng nếu không tìm thấy hoặc project không active)
*/
    Optional<Project> findByIdAndActiveTrue(Long id);
    
// Các phương thức khác có thể được thêm trong tương lai:
// List<Project> findByOwnerId(Long ownerId);
// List<Project> findByActiveTrue();
// List<Project> findByNameContainingIgnoreCase(String name);

} 
