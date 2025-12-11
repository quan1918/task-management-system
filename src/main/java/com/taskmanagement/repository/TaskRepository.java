package com.taskmanagement.repository;

import com.taskmanagement.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * TaskRepository - Tầng truy cập dữ liệu cho entity Task
 *
 * Mục đích (phục vụ tính năng Create Task):
 * - Lưu task mới vào cơ sở dữ liệu
 * - Persist entity task sau khi tạo
 *
 * Các phương thức cần cho Create Task:
 * - save(Task task) - Kế thừa từ JpaRepository
 *
 * Các phương thức tương lai (tạm bỏ qua):
 * - findByAssigneeId(), findByProjectId(), findByStatus()
 * - findOverdueTasks(), searchTasks(), v.v.
 *
 * Vì sao để tối thiểu?
 * - Đối với Create Task, chúng ta chỉ cần save()
 * - JpaRepository cung cấp sẵn save()
 * - Sẽ thêm các phương thức truy vấn khi xây dựng các tính năng List/Search sau
 *
 * Ghi chú về save():
 * - Nếu task.id là null → INSERT (tạo mới)
 * - Nếu task.id tồn tại → UPDATE (cập nhật)
 * - Đối với Create Task, luôn là INSERT (id = null)
 *
 * @author Task Management System
 * @version 1.0 (Tối thiểu cho Create Task)
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long>  {

    // ==================== CÁC PHƯƠNG THỨC KẾ THỪA ====================
    //
    // Từ JpaRepository<Task, Long>:
    // - Task save(Task task)
    // - Optional<Task> findById(Long id)
    // - void delete(Task task)
    // - List<Task> findAll()
    // - long count()
    //
    //
    // ==================== CHƯA CẦN CUSTOM METHOD ====================
    //
    // Đối với Create Task, chỉ cần:
    // - save(Task task) để insert task mới
    //
    // JpaRepository đã cung cấp sẵn phương thức này!
    // Chưa cần viết thêm phương thức tùy chỉnh.

    // Các phương thức truy vấn trong tương lai (thêm khi xây tính năng khác):

    // Cho GET /api/tasks (liệt kê tất cả):
    // List<Task> findByProjectId(Long projectId);
    // List<Task> findByAssigneeId(Long assigneeId);

    // Cho GET /api/tasks?status=PENDING:
    // List<Task> findByStatus(TaskStatus status);
    // List<Task> findByStatusAndAssigneeId(TaskStatus status, Long assigneeId);

    // Cho task quá hạn:
    // @Query("SELECT t FROM Task t WHERE t.dueDate < CURRENT_TIMESTAMP AND t.status != 'COMPLETED'")
    // List<Task> findOverdueTasks();

    // Cho tìm kiếm:
    // List<Task> findByTitleContainingIgnoreCase(String keyword);

    // Cho phân trang:
    // Page<Task> findByProjectId(Long projectId, Pageable pageable);
    
}
