/**
 * Utility classes and helper functions for the Task Management System.
 * 
 * <h2>Planned Implementation (v0.8.0)</h2>
 * This package will contain reusable utilities and helper classes:
 * 
 * <h3>Date/Time Utilities:</h3>
 * <ul>
 *   <li><b>DateTimeUtils</b> - Helper methods for date/time operations
 *     <ul>
 *       <li>isOverdue(LocalDateTime dueDate) - Checks if task is overdue</li>
 *       <li>formatDuration(LocalDateTime start, LocalDateTime end) - Human-readable duration</li>
 *       <li>getBusinessDaysBetween(LocalDate start, LocalDate end) - Excludes weekends</li>
 *       <li>toUserTimezone(LocalDateTime utc, String timezone) - Timezone conversion</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h3>Validation Utilities:</h3>
 * <ul>
 *   <li><b>ValidationUtils</b> - Custom validation helpers
 *     <ul>
 *       <li>isValidEmail(String email) - Email format validation</li>
 *       <li>isStrongPassword(String password) - Password strength check</li>
 *       <li>sanitizeInput(String input) - XSS prevention</li>
 *       <li>validateProjectCode(String code) - Project code format</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h3>Collection Utilities:</h3>
 * <ul>
 *   <li><b>CollectionUtils</b> - Helper methods for collections
 *     <ul>
 *       <li>isNullOrEmpty(Collection&lt;?&gt; collection) - Null-safe check</li>
 *       <li>partition(List&lt;T&gt; list, int size) - Split list into chunks</li>
 *       <li>distinct(List&lt;T&gt; list, Function&lt;T, ?&gt; keyExtractor) - Deduplicate by key</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h3>String Utilities:</h3>
 * <ul>
 *   <li><b>StringUtils</b> - String manipulation helpers
 *     <ul>
 *       <li>truncate(String text, int maxLength) - Truncate with ellipsis</li>
 *       <li>slugify(String text) - Generate URL-friendly slug</li>
 *       <li>generateRandomCode(int length) - Random alphanumeric code</li>
 *       <li>maskSensitiveData(String data) - Mask PII for logging</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h3>JSON Utilities:</h3>
 * <ul>
 *   <li><b>JsonUtils</b> - JSON serialization/deserialization helpers
 *     <ul>
 *       <li>toJson(Object obj) - Convert to JSON string</li>
 *       <li>fromJson(String json, Class&lt;T&gt; clazz) - Parse JSON</li>
 *       <li>prettyPrint(Object obj) - Pretty-printed JSON</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h3>Exception Utilities:</h3>
 * <ul>
 *   <li><b>ExceptionUtils</b> - Exception handling helpers
 *     <ul>
 *       <li>getRootCause(Throwable throwable) - Find root cause</li>
 *       <li>getStackTraceAsString(Throwable throwable) - Stack trace string</li>
 *       <li>isCausedBy(Throwable throwable, Class&lt;? extends Throwable&gt; type)</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h3>File Utilities:</h3>
 * <ul>
 *   <li><b>FileUtils</b> - File operation helpers
 *     <ul>
 *       <li>getFileExtension(String filename) - Extract extension</li>
 *       <li>validateFileType(String filename, Set&lt;String&gt; allowedTypes)</li>
 *       <li>generateUniqueFilename(String originalName) - Avoid collisions</li>
 *       <li>formatFileSize(long bytes) - Human-readable size (e.g., "2.5 MB")</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h3>Constants:</h3>
 * <ul>
 *   <li><b>AppConstants</b> - Application-wide constants
 *     <ul>
 *       <li>Default pagination sizes</li>
 *       <li>Date/time formats</li>
 *       <li>API response codes</li>
 *       <li>Regex patterns</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>
 * // DateTimeUtils
 * if (DateTimeUtils.isOverdue(task.getDueDate())) {
 *     task.markAsOverdue();
 * }
 * 
 * // StringUtils
 * String slug = StringUtils.slugify(project.getName());
 * // "My New Project" → "my-new-project"
 * 
 * // ValidationUtils
 * if (!ValidationUtils.isStrongPassword(password)) {
 *     throw new WeakPasswordException();
 * }
 * 
 * // JsonUtils
 * String json = JsonUtils.toJson(taskDTO);
 * logger.info("Created task: {}", json);
 * </pre>
 * 
 * <h3>Best Practices:</h3>
 * <ul>
 *   <li>All utility classes should be final with private constructors</li>
 *   <li>All methods should be static</li>
 *   <li>Prefer null-safe operations (return empty instead of null)</li>
 *   <li>Use Apache Commons Lang3 where applicable</li>
 *   <li>Document edge cases and performance characteristics</li>
 *   <li>Write comprehensive unit tests for all utilities</li>
 * </ul>
 * 
 * <h3>Dependencies:</h3>
 * <ul>
 *   <li>Apache Commons Lang3 (org.apache.commons:commons-lang3)</li>
 *   <li>Apache Commons Collections4 (optional)</li>
 *   <li>Jackson for JSON operations (already in Spring Boot)</li>
 * </ul>
 * 
 * @see org.apache.commons.lang3.StringUtils
 * @see org.apache.commons.collections4.CollectionUtils
 * @since v0.7.0
 * @author Task Management Team
 * @version v0.8.0 (Planned)
 */
@com.taskmanagement.annotation.Planned(
    version = "v0.8.0",
    description = "Utility classes and helper functions",
    ticket = "TM-175",
    priority = "MEDIUM"
)
package com.taskmanagement.util;
