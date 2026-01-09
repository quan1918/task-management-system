package com.taskmanagement.repository;

import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;
import com.taskmanagement.entity.TaskStatus;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
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
     * Find task by ID with assignees and project eagerly loaded
     * TEST: Using native SQL to bypass Hibernate @Where filtering
     */
    @Query(value = "SELECT t.* FROM tasks t WHERE t.id = :id", nativeQuery = true)
    Optional<Task> findByIdNative(@Param("id") Long id);
    
    /**
     * Get assignees for a task - bypasses @Where filter issues
     * Returns User IDs, then load entities separately
     */
    @Query(value = "SELECT u.id FROM users u " +
           "JOIN task_assignees ta ON u.id = ta.user_id " +
           "WHERE ta.task_id = :taskId AND u.deleted = false", 
           nativeQuery = true)
    List<Long> findAssigneeIdsByTaskId(@Param("taskId") Long taskId);
    
    /**
     * Find tasks assigned to a specific user
     * 
     * Business Logic:
     * - Returns tasks where user is ONE OF the assignees
     * - Uses JOIN on task_assignees junction table
     * - DISTINCT prevents duplicate rows (if task has multiple assignees)
     * 
     * Example:
     * Task #10 has assignees: [user5, user7]
     * findTasksAssignedToUser(5) → Returns Task #10
     * findTasksAssignedToUser(7) → Returns Task #10
     */
    @Query("SELECT DISTINCT t FROM Task t JOIN t.assignees a WHERE a.id = :userId")
    List<Task> findTasksAssignedToUser(@Param("userId") Long userId); 

    /**
     * Bulk update: Remove user from all tasks
     * 
     * Business Logic:
     * - Remove user from task_assignees junction table
     * - Tasks become unassigned if this was the only assignee
     * 
     * ✅ KHÔNG load entities vào memory
     * ✅ KHÔNG trigger validation
     * ✅ Execute trực tiếp 1 SQL DELETE
     * 
     */
    @Modifying
    @Query(value = "DELETE FROM task_assignees WHERE user_id = :userId", nativeQuery = true)
    int unassignTasksByUserId(@Param("userId") Long userId);

    /**
     * Tìm tất cả tasks có status = UNASSIGNED
     * 
     * @return List các UNASSIGNED tasks
     */
    List<Task> findByStatus(TaskStatus status);
    
    /**
     * Find UNASSIGNED tasks (no assignees)
     */
    @Query("SELECT t FROM Task t WHERE t.assignees IS EMPTY")
    List<Task> findUnassignedTasks();
    
    /**
     * Find tasks by status and assignee
     *
    @Query("SELECT DISTINCT t FROM Task t JOIN t.assignees a " +
           "WHERE t.status = :status AND a.id = :userId")
    List<Task> findByStatusAndAssigneeId(
        @Param("status") TaskStatus status,
        @Param("userId") Long userId
    );
    */
    
    List<Task> findAllByProjectId(Long id);

    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.assignees " +
           "LEFT JOIN FETCH t.project " +
           "WHERE t.project.id = :projectId " +
           "ORDER BY t.createdAt DESC")
    List<Task> findAllByProjectIdWithAssignees(@Param("projectId") Long projectId);

    // ==================== CÁC PHƯƠNG THỨC KẾ THỪA ====================
    //
    // Từ JpaRepository<Task, Long>:
    // - Task save(Task task)
    // - Optional<Task> findById(Long id)
    // - void delete(Task task)
    // - List<Task> findAll()
    // - long count()
    // ==================== CHƯA CẦN CUSTOM METHOD ====================

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
