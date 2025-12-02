package com.taskmanagement.entity;

/**
 * Enum trạng thái của Task
 *
 * Đại diện cho toàn bộ vòng đời của một task từ lúc tạo đến khi hoàn thành
 *
 * Chuyển đổi trạng thái:
 * PENDING → IN_PROGRESS → COMPLETED
 *    ↓           ↓
 * CANCELLED   BLOCKED
 *
 * Quy tắc nghiệp vụ (Business Rules):
 * - Task mới luôn bắt đầu ở trạng thái PENDING
 * - Không thể hoàn thành (COMPLETED) một task đã bị CANCELLED
 * - Task BLOCKED phải được gỡ vướng mắc trước khi tiếp tục
 * - COMPLETED là trạng thái kết thúc (không thể quay lại trạng thái trước)
 */
public enum TaskStatus {
    
    /**
     * Task đã được tạo nhưng chưa bắt đầu
     * Trạng thái khởi đầu của mọi task mới
     */
    PENDING("Pending", "Task is created but not yet started"),

    /**
     * Task đang được thực hiện
     * Được set khi người được giao bắt đầu làm việc
     */
    IN_PROGRESS("In Progress", "Task is currently being worked on"),

    /**
     * Task đã hoàn thành và bàn giao
     * Trạng thái kết thúc – vòng đời task kết thúc
     */    
    COMPLETED("Completed", "Task is finished and delivered"),

    /**
     * Task đang chờ phụ thuộc bên ngoài
     * Ví dụ: chờ phản hồi khách hàng, bị chặn bởi task khác
     */
    BLOCKED("Blocked", "Task is blocked by external factors"),

    /**
     * Task không còn cần thiết nữa
     * Ví dụ: thay đổi yêu cầu, trùng công việc
     */
    CANCELLED("Cancelled", "Task is no longer needed");    

    private final String displayName;
    private final String description;

    TaskStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Kiểm tra task có ở trạng thái kết thúc (không thể chỉnh sửa) hay không
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * Kiểm tra task có thể tiếp tục xử lý hay không
     */
    public boolean isActionable() {
        return this == PENDING || this == IN_PROGRESS;
    }
}
