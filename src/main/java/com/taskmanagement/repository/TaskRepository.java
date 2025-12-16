package com.taskmanagement.repository;

import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;
import com.taskmanagement.entity.TaskStatus;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Tìm task theo ID, BỎ QUA deleted flag
     * 
     * Dùng cho:
     * - Restore task đã soft delete
     * - Admin view (xem cả task đã xóa)
     * - Audit/Report
     * 
     * @param id Task ID
     * @return Optional<Task> (kể cả task đã deleted)
     */
    @Query("SELECT t FROM Task t WHERE t.id = :id")
    Optional<Task> findByIdIncludingDeleted(Long id);

     /**
     * Tìm tất cả tasks được gán cho một user cụ thể
     * 
     * @param assigneeId User ID
     * @return List các tasks của user đó
     */
    List<Task> findByAssigneeId(Long assigneeId);

    /**
     * Tìm tất cả tasks chưa được gán (assignee = NULL)
     * 
     * @return List các UNASSIGNED tasks
     */
    List<Task> findByAssigneeIsNull();

    /**
     * Đếm số tasks đang assigned cho một user
     * 
     * @param assigneeId User ID
     * @return Số lượng tasks
     */
    long countByAssigneeId(Long assigneeId);   

    /**
     * Bulk update: Unassign tất cả tasks của một user
     * 
     * Business Logic:
     * - Set assignee = NULL
     * - Set status = UNASSIGNED
     * 
     * ✅ KHÔNG load entities vào memory
     * ✅ KHÔNG trigger validation
     * ✅ Execute trực tiếp 1 SQL UPDATE
     * 
     * Performance:
     * - Thay vì N queries (loop save từng task)
     * - Chỉ cần 1 query duy nhất
     * - Tránh N+1 problem
     * 
     * @param userId User ID cần unassign tasks
     * @return Số tasks đã được update
     */
    @Modifying
    @Query("UPDATE Task t " +
           "SET t.assignee = NULL, " +
           "    t.status = com.taskmanagement.entity.TaskStatus.UNASSIGNED " +
           "WHERE t.assignee.id = :userId")
    int unassignTasksByUserId(@Param("userId") Long userId);

    /**
     * Tìm tất cả tasks có status = UNASSIGNED
     * 
     * @return List các UNASSIGNED tasks
     */
    List<Task> findByStatus(TaskStatus status);

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
