package com.taskmanagement.util;

import com.taskmanagement.entity.Project;
import com.taskmanagement.entity.Task;
import com.taskmanagement.entity.User;
import com.taskmanagement.entity.TaskStatus;
import com.taskmanagement.entity.TaskPriority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Factory class for creating test data objects.
 * Provides default valid entities for testing.
 */
public class TestDataBuilder {

    // ==================== USER BUILDERS ====================

    /**
     * Creates a valid User with default test data.
     */
    public static User buildValidUser() {
        User user = new User();
        user.setId(TestConstants.TEST_USER_ID);
        user.setUsername(TestConstants.TEST_USERNAME);
        user.setEmail(TestConstants.TEST_USER_EMAIL);
        user.setPasswordHash(TestConstants.TEST_USER_PASSWORD_HASH);
        user.setFullName(TestConstants.TEST_USER_FULLNAME);
        return user;
    }

    /**
     * Creates a User with custom ID.
     */
    public static User buildUserWithId(Long id) {
        User user = buildValidUser();
        user.setId(id);
        return user;
    }

    /**
     * Creates a User with custom email.
     */
    public static User buildUserWithEmail(String email) {
        User user = buildValidUser();
        user.setEmail(email);
        return user;
    }

    /**
     * Creates an owner User (typically used as project owner).
     */
    public static User buildOwnerUser() {
        User user = new User();
        user.setId(TestConstants.TEST_OWNER_ID);
        user.setUsername(TestConstants.TEST_OWNER_USERNAME);
        user.setEmail(TestConstants.TEST_OWNER_EMAIL);
        user.setPasswordHash(TestConstants.TEST_USER_PASSWORD_HASH);
        user.setFullName("Owner User");
        return user;
    }

    // ==================== PROJECT BUILDERS ====================

    /**
     * Creates a valid Project with default test data.
     */
    public static Project buildValidProject() {
        Project project = new Project();
        project.setId(TestConstants.TEST_PROJECT_ID);
        project.setName(TestConstants.TEST_PROJECT_NAME);
        project.setDescription(TestConstants.TEST_PROJECT_DESCRIPTION);
        project.setStartDate(TestConstants.TEST_PROJECT_START_DATE);
        project.setEndDate(TestConstants.TEST_PROJECT_END_DATE);
        project.setOwner(buildOwnerUser());
        project.setTasks(new ArrayList<>());
        return project;
    }

    /**
     * Creates a Project with custom ID.
     */
    public static Project buildProjectWithId(Long id) {
        Project project = buildValidProject();
        project.setId(id);
        return project;
    }

    /**
     * Creates a Project with custom name and owner.
     */
    public static Project buildProjectWithNameAndOwner(String name, User owner) {
        Project project = buildValidProject();
        project.setName(name);
        project.setOwner(owner);
        return project;
    }

    /**
     * Creates a Project without tasks.
     */
    public static Project buildProjectWithoutTasks() {
        Project project = buildValidProject();
        project.setTasks(new ArrayList<>());
        return project;
    }

    // ==================== TASK BUILDERS ====================

    /**
     * Creates a valid Task with default test data.
     */
    public static Task buildValidTask() {
        Task task = new Task();
        task.setId(TestConstants.TEST_TASK_ID);
        task.setTitle(TestConstants.TEST_TASK_TITLE);
        task.setDescription(TestConstants.TEST_TASK_DESCRIPTION);
        task.setStatus(TaskStatus.valueOf(TestConstants.STATUS_PENDING));
        task.setPriority(TaskPriority.valueOf(TestConstants.PRIORITY_MEDIUM));
        task.setDueDate(TestConstants.getTestTaskDueDate());
        task.setProject(buildValidProject());
        task.setAssignees(new HashSet<>());
        return task;
    }

    /**
     * Creates a Task with custom ID.
     */
    public static Task buildTaskWithId(Long id) {
        Task task = buildValidTask();
        task.setId(id);
        return task;
    }

    /**
     * Creates a Task with custom status.
     */
    public static Task buildTaskWithStatus(String status) {
        Task task = buildValidTask();
        task.setStatus(TaskStatus.valueOf(status));
        return task;
    }

    /**
     * Creates a Task with custom priority.
     */
    public static Task buildTaskWithPriority(String priority) {
        Task task = buildValidTask();
        task.setPriority(TaskPriority.valueOf(priority));
        return task;
    }

    /**
     * Creates a Task with assignees.
     */
    public static Task buildTaskWithAssignees(User... assignees) {
        Task task = buildValidTask();
        task.setAssignees(new HashSet<>(Arrays.asList(assignees)));
        return task;
    }

    /**
     * Creates a Task assigned to a specific project.
     */
    public static Task buildTaskForProject(Project project) {
        Task task = buildValidTask();
        task.setProject(project);
        return task;
    }

    // ==================== LIST BUILDERS ====================

    /**
     * Creates a list of Users.
     */
    public static List<User> buildUserList(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = buildValidUser();
            user.setId((long) (TestConstants.TEST_USER_ID + i));
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            users.add(user);
        }
        return users;
    }

    /**
     * Creates a list of Projects.
     */
    public static List<Project> buildProjectList(int count) {
        List<Project> projects = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Project project = buildValidProject();
            project.setId((long) (TestConstants.TEST_PROJECT_ID + i));
            project.setName("Project " + i);
            projects.add(project);
        }
        return projects;
    }

    /**
     * Creates a list of Tasks.
     */
    public static List<Task> buildTaskList(int count) {
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Task task = buildValidTask();
            task.setId((long) (TestConstants.TEST_TASK_ID + i));
            task.setTitle("Task " + i);
            tasks.add(task);
        }
        return tasks;
    }

    // Private constructor to prevent instantiation
    private TestDataBuilder() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}