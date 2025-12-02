package com.taskmanagement.entity;

/**
 * Enum mức độ ưu tiên của Task
 *
 * Định nghĩa mức độ khẩn cấp và tầm quan trọng của task để phục vụ việc sắp xếp ưu tiên
 *
 * Các mức ưu tiên:
 * - LOW: Mức thấp, không gấp
 * - MEDIUM: Ưu tiên bình thường, deadline tiêu chuẩn
 * - HIGH: Quan trọng, cần xử lý sớm
 * - CRITICAL: Khẩn cấp, có thể chặn các công việc khác
 *
 * Ứng dụng trong sắp xếp:
 *   Tasks sẽ được sort theo priority.level giảm dần (CRITICAL đứng đầu)
 */
public enum TaskPriority {
    
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High"),
    CRITICAL(4, "Critical");
    
    private final int level;
    private final String displayName;

    TaskPriority(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Kiểm tra mức độ ưu tiên hiện tại có cao hơn mức khác hay không
     *
     * @param other Mức ưu tiên để so sánh
     * @return true nếu mức hiện tại cao hơn mức còn lại
     */
    public boolean isHigherThan(TaskPriority other) {
        return this.level > other.level;
    }
}
