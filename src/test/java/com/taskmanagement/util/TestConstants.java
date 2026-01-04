package com.taskmanagement.util;

import java.time.LocalDateTime;
import java.time.LocalDate;

public class TestConstants {
    
    // PROJECT
    public static final Long TEST_PROJECT_ID = 1L;
    public static final Long TEST_PROJECT_ID_2 = 2L;
    public static final String TEST_PROJECT_NAME = "Test Project";
    public static final String TEST_PROJECT_DESCRIPTION = "This is a test project.";
    public static final LocalDate TEST_PROJECT_START_DATE = LocalDate.of(2024, 1, 1);
    public static final LocalDate TEST_PROJECT_END_DATE = LocalDate.of(2024, 12, 31);

    // TASK
    public static final Long TEST_TASK_ID = 100L;
    public static final Long TEST_TASK_ID_2 = 101L;
    public static final String TEST_TASK_TITLE = "Test Task Title 1234";
    public static final String TEST_TASK_DESCRIPTION = "Test task description for validation 1234";
    public static LocalDateTime getTestTaskDueDate() {
        return LocalDateTime.now().plusDays(7);
    }

    // USER
    public static final Long TEST_USER_ID = 10L;
    public static final Long TEST_USER_ID_2 = 11L;
    public static final String TEST_USERNAME = "testuser";
    public static final String TEST_USER_EMAIL = "test@example.com";
    public static final String TEST_USER_PASSWORD_HASH = "password123";
    public static final String TEST_USER_FULLNAME = "Test User";

    // OWNER
    public static final Long TEST_OWNER_ID = 20L;
    public static final String TEST_OWNER_USERNAME = "owneruser";
    public static final String TEST_OWNER_EMAIL = "owner@example.com";

    // STATUS
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_BLOCKED = "BLOCKED";

    // PRIORITY
    public static final String PRIORITY_LOW = "LOW";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_HIGH = "HIGH";

    // ERRORS
    public static final String ERROR_TASK_NOT_FOUND = "Task not found";
    public static final String ERROR_PROJECT_NOT_FOUND = "Project not found";
    public static final String ERROR_USER_NOT_FOUND = "User not found";

    // INVALID DATA
    public static final Long INVALID_ID = 9999L;
    public static final String INVALID_EMAIL = "invalid-email";
    public static final String EMPTY_STRING = "";
    public static final String BLANK_STRING = "   ";

    private TestConstants() {
        throw new UnsupportedOperationException("Utility  class - do not instantiate");
    }
}
