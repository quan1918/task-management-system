# Utility Layer (Helper Classes & Functions)

##  Overview

The **Utility layer** provides common helper classes, extension methods, and utility functions used across the application. These utilities reduce code duplication, promote consistency, and provide reusable functionality for logging, validation, date/time handling, string manipulation, and security operations.

**Location:** \src/main/java/com/taskmanagement/util/\

**Responsibility:** Provide reusable helper functions, extension methods, and utility classes that support other layers without business logic

---

##  Core Responsibilities

### 1. Security Utilities
- Extract current user information from security context
- Check user roles and permissions
- Validate authorization for resources
- Handle JWT token manipulation

### 2. Date & Time Utilities
- Format and parse dates consistently
- Convert between time zones
- Calculate time differences
- Provide common date ranges

### 3. String & Text Utilities
- Validate and sanitize strings
- Format text for display
- Generate slugs from titles
- Truncate and ellipsize text

### 4. Validation Utilities
- Validate email addresses
- Verify phone numbers
- Check password strength
- Validate custom business rules

### 5. Collection & Stream Utilities
- Filter and transform collections
- Batch processing helpers
- Null-safe operations
- Collection conversions

---

##  Folder Structure

\\\
util/
 security/
    SecurityUtils.java                 # Current user and authorization
    JwtUtils.java                      # JWT token utilities
    PasswordUtils.java                 # Password validation and hashing

 date/
    DateTimeUtils.java                 # Date and time helpers
    TimeZoneUtils.java                 # Timezone conversion
    DateFormatUtils.java               # Date formatting

 string/
    StringUtils.java                   # String manipulation
    SlugUtils.java                     # URL slug generation
    TextUtils.java                     # Text formatting

 validation/
    ValidationUtils.java               # Common validation methods
    EmailValidator.java                # Email validation
    PhoneValidator.java                # Phone validation

 collection/
    CollectionUtils.java               # Collection operations
    StreamUtils.java                   # Stream API helpers
    PageUtils.java                     # Pagination utilities

 constant/
    Constants.java                     # Application constants
    RegexPatterns.java                 # Regex patterns

 README.md                             # This file
\\\

---

##  Security Utilities

\\\java
/**
 * Security Utility Functions
 * Provides methods for accessing user information and permissions
 */
@UtilityClass
@Slf4j
public class SecurityUtils {
    
    /**
     * Get current authenticated user ID
     * Returns null if user is not authenticated
     * 
     * @return User ID or null
     */
    public static Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            
            Object principal = authentication.getPrincipal();
            if (principal instanceof SecurityUserDetails) {
                return ((SecurityUserDetails) principal).getId();
            }
            return null;
        } catch (Exception e) {
            log.error("Error getting current user ID", e);
            return null;
        }
    }
    
    /**
     * Get current authenticated username
     * 
     * @return Username or null
     */
    public static String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            return authentication.getName();
        } catch (Exception e) {
            log.error("Error getting current username", e);
            return null;
        }
    }
    
    /**
     * Get current authenticated user principal
     * 
     * @return SecurityUserDetails or null
     */
    public static SecurityUserDetails getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof SecurityUserDetails) {
                return (SecurityUserDetails) authentication.getPrincipal();
            }
            return null;
        } catch (Exception e) {
            log.error("Error getting current user", e);
            return null;
        }
    }
    
    /**
     * Check if user is authenticated
     * 
     * @return true if user is authenticated
     */
    public static boolean isAuthenticated() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null && authentication.isAuthenticated();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if current user has role
     * 
     * @param role Role name (without ROLE_ prefix)
     * @return true if user has role
     */
    public static boolean hasRole(String role) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) return false;
            
            return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role.toUpperCase()));
        } catch (Exception e) {
            log.error("Error checking role", e);
            return false;
        }
    }
    
    /**
     * Check if current user has any role
     * 
     * @param roles Role names (without ROLE_ prefix)
     * @return true if user has any of the roles
     */
    public static boolean hasAnyRole(String... roles) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) return false;
            
            List<String> roleList = Arrays.stream(roles)
                .map(r -> "ROLE_" + r.toUpperCase())
                .collect(Collectors.toList());
            
            return authentication.getAuthorities().stream()
                .anyMatch(auth -> roleList.contains(auth.getAuthority()));
        } catch (Exception e) {
            log.error("Error checking roles", e);
            return false;
        }
    }
    
    /**
     * Check if current user is admin
     * 
     * @return true if user has ADMIN role
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
    
    /**
     * Check if user owns resource (for authorization)
     * 
     * @param userId User ID who owns resource
     * @return true if current user owns resource or is admin
     */
    public static boolean ownsResource(Long userId) {
        Long currentId = getCurrentUserId();
        if (currentId == null) return false;
        return currentId.equals(userId) || isAdmin();
    }
}
\\\

---

##  Date & Time Utilities

\\\java
/**
 * Date and Time Utility Functions
 * Provides consistent date/time handling
 */
@UtilityClass
@Slf4j
public class DateTimeUtils {
    
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    
    /**
     * Get current date in system timezone
     * 
     * @return LocalDate
     */
    public static LocalDate getCurrentDate() {
        return LocalDate.now();
    }
    
    /**
     * Get current date and time in system timezone
     * 
     * @return LocalDateTime
     */
    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }
    
    /**
     * Get current date and time in UTC
     * 
     * @return ZonedDateTime in UTC
     */
    public static ZonedDateTime getCurrentDateTimeUTC() {
        return ZonedDateTime.now(ZoneId.of("UTC"));
    }
    
    /**
     * Format LocalDate to string
     * 
     * @param date LocalDate
     * @return Formatted date string
     */
    public static String formatDate(LocalDate date) {
        if (date == null) return null;
        return date.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
    }
    
    /**
     * Format LocalDateTime to string
     * 
     * @param dateTime LocalDateTime
     * @return Formatted datetime string
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ofPattern(DATETIME_FORMAT));
    }
    
    /**
     * Format LocalDateTime to ISO format
     * 
     * @param dateTime LocalDateTime
     * @return ISO formatted datetime string
     */
    public static String formatDateTimeISO(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ofPattern(ISO_DATETIME_FORMAT));
    }
    
    /**
     * Parse date string to LocalDate
     * 
     * @param dateStr Date string in yyyy-MM-dd format
     * @return LocalDate
     */
    public static LocalDate parseDate(String dateStr) {
        if (StringUtils.isBlank(dateStr)) return null;
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(DATE_FORMAT));
        } catch (DateTimeParseException e) {
            log.error("Error parsing date: {}", dateStr, e);
            return null;
        }
    }
    
    /**
     * Parse datetime string to LocalDateTime
     * 
     * @param dateTimeStr Datetime string in yyyy-MM-dd HH:mm:ss format
     * @return LocalDateTime
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (StringUtils.isBlank(dateTimeStr)) return null;
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(DATETIME_FORMAT));
        } catch (DateTimeParseException e) {
            log.error("Error parsing datetime: {}", dateTimeStr, e);
            return null;
        }
    }
    
    /**
     * Calculate days between two dates
     * 
     * @param from Start date
     * @param to End date
     * @return Number of days (negative if from > to)
     */
    public static long daysBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) return 0;
        return ChronoUnit.DAYS.between(from, to);
    }
    
    /**
     * Calculate minutes between two datetimes
     * 
     * @param from Start datetime
     * @param to End datetime
     * @return Number of minutes
     */
    public static long minutesBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) return 0;
        return ChronoUnit.MINUTES.between(from, to);
    }
    
    /**
     * Check if date is in past
     * 
     * @param date Date to check
     * @return true if date is before today
     */
    public static boolean isPast(LocalDate date) {
        if (date == null) return false;
        return date.isBefore(getCurrentDate());
    }
    
    /**
     * Check if date is in future
     * 
     * @param date Date to check
     * @return true if date is after today
     */
    public static boolean isFuture(LocalDate date) {
        if (date == null) return false;
        return date.isAfter(getCurrentDate());
    }
    
    /**
     * Check if datetime is overdue
     * 
     * @param dateTime Datetime to check
     * @return true if datetime is before now
     */
    public static boolean isOverdue(LocalDateTime dateTime) {
        if (dateTime == null) return false;
        return dateTime.isBefore(getCurrentDateTime());
    }
    
    /**
     * Get start of day (00:00:00)
     * 
     * @param date Date
     * @return LocalDateTime at start of day
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        if (date == null) return null;
        return date.atStartOfDay();
    }
    
    /**
     * Get end of day (23:59:59)
     * 
     * @param date Date
     * @return LocalDateTime at end of day
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        if (date == null) return null;
        return date.atTime(LocalTime.MAX);
    }
    
    /**
     * Add days to date
     * 
     * @param date Start date
     * @param days Days to add
     * @return New date
     */
    public static LocalDate addDays(LocalDate date, long days) {
        if (date == null) return null;
        return date.plusDays(days);
    }
    
    /**
     * Add days to datetime
     * 
     * @param dateTime Start datetime
     * @param days Days to add
     * @return New datetime
     */
    public static LocalDateTime addDays(LocalDateTime dateTime, long days) {
        if (dateTime == null) return null;
        return dateTime.plusDays(days);
    }
}
\\\

---

##  String Utilities

\\\java
/**
 * String Utility Functions
 * Provides common string operations
 */
@UtilityClass
@Slf4j
public class StringUtils {
    
    /**
     * Check if string is blank (null, empty, or whitespace)
     * 
     * @param value String to check
     * @return true if blank
     */
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    /**
     * Check if string is not blank
     * 
     * @param value String to check
     * @return true if not blank
     */
    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }
    
    /**
     * Safely get string length
     * 
     * @param value String
     * @return Length or 0 if null
     */
    public static int safeLength(String value) {
        return value == null ? 0 : value.length();
    }
    
    /**
     * Truncate string to max length
     * 
     * @param value String to truncate
     * @param maxLength Maximum length
     * @return Truncated string
     */
    public static String truncate(String value, int maxLength) {
        if (isBlank(value)) return value;
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
    
    /**
     * Truncate string and add ellipsis
     * 
     * @param value String to truncate
     * @param maxLength Maximum length (including ellipsis)
     * @return Truncated string with ellipsis
     */
    public static String ellipsize(String value, int maxLength) {
        if (isBlank(value)) return value;
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Convert to slug format (lowercase, spaces to hyphens)
     * 
     * @param value String to convert
     * @return Slug format string
     */
    public static String toSlug(String value) {
        if (isBlank(value)) return "";
        return value.toLowerCase()
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+\$", "");
    }
    
    /**
     * Capitalize first letter
     * 
     * @param value String
     * @return Capitalized string
     */
    public static String capitalize(String value) {
        if (isBlank(value)) return value;
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
    
    /**
     * Check if string contains any of given substrings
     * 
     * @param value String to check
     * @param substrings Substrings to find
     * @return true if contains any substring
     */
    public static boolean containsAny(String value, String... substrings) {
        if (isBlank(value)) return false;
        return Arrays.stream(substrings)
            .anyMatch(value::contains);
    }
    
    /**
     * Remove whitespace from string
     * 
     * @param value String
     * @return String without whitespace
     */
    public static String removeWhitespace(String value) {
        if (isBlank(value)) return value;
        return value.replaceAll("\\s+", "");
    }
    
    /**
     * Mask sensitive data (keep first and last char)
     * 
     * @param value String to mask
     * @return Masked string
     */
    public static String maskSensitive(String value) {
        if (isBlank(value) || value.length() < 3) return "***";
        return value.charAt(0) + "*".repeat(value.length() - 2) + value.charAt(value.length() - 1);
    }
}
\\\

---

##  Validation Utilities

\\\java
/**
 * Validation Utility Functions
 * Provides common validation checks
 */
@UtilityClass
@Slf4j
public class ValidationUtils {
    
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_REGEX = "^[+]?[0-9]{10,}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);
    
    /**
     * Validate email address format
     * 
     * @param email Email to validate
     * @return true if valid email format
     */
    public static boolean isValidEmail(String email) {
        if (StringUtils.isBlank(email)) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validate phone number format
     * 
     * @param phone Phone to validate
     * @return true if valid phone format
     */
    public static boolean isValidPhone(String phone) {
        if (StringUtils.isBlank(phone)) return false;
        return PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * Validate URL format
     * 
     * @param url URL to validate
     * @return true if valid URL
     */
    public static boolean isValidUrl(String url) {
        if (StringUtils.isBlank(url)) return false;
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
    
    /**
     * Validate password strength
     * Requirements: min 8 chars, uppercase, lowercase, digit, special char
     * 
     * @param password Password to validate
     * @return true if password is strong
     */
    public static boolean isStrongPassword(String password) {
        if (StringUtils.isBlank(password) || password.length() < 8) return false;
        
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+-=\\[\\]{};:'\",.<>?/\\\\|~].*");
        
        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar;
    }
    
    /**
     * Validate UUID format
     * 
     * @param uuid UUID string
     * @return true if valid UUID
     */
    public static boolean isValidUUID(String uuid) {
        if (StringUtils.isBlank(uuid)) return false;
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Validate that value is within range
     * 
     * @param value Value to check
     * @param min Minimum (inclusive)
     * @param max Maximum (inclusive)
     * @return true if within range
     */
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }
    
    /**
     * Validate that value is not null
     * 
     * @param value Value to check
     * @param fieldName Field name for error message
     * @throws IllegalArgumentException if value is null
     */
    public static void notNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }
    
    /**
     * Validate that string is not blank
     * 
     * @param value String to check
     * @param fieldName Field name for error message
     * @throws IllegalArgumentException if blank
     */
    public static void notBlank(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
    }
}
\\\

---

##  Collection Utilities

\\\java
/**
 * Collection Utility Functions
 * Provides common collection operations
 */
@UtilityClass
@Slf4j
public class CollectionUtils {
    
    /**
     * Check if collection is empty
     * 
     * @param collection Collection to check
     * @return true if null or empty
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
    
    /**
     * Check if collection is not empty
     * 
     * @param collection Collection to check
     * @return true if not null and not empty
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }
    
    /**
     * Get safe size of collection
     * 
     * @param collection Collection
     * @return Size or 0 if null
     */
    public static int safeSize(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }
    
    /**
     * Filter collection by predicate
     * 
     * @param collection Collection to filter
     * @param predicate Filter predicate
     * @return Filtered list
     */
    public static <T> List<T> filter(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection)) return Collections.emptyList();
        return collection.stream()
            .filter(predicate)
            .collect(Collectors.toList());
    }
    
    /**
     * Map collection to different type
     * 
     * @param collection Collection to map
     * @param mapper Mapping function
     * @return Mapped list
     */
    public static <T, R> List<R> map(Collection<T> collection, Function<T, R> mapper) {
        if (isEmpty(collection)) return Collections.emptyList();
        return collection.stream()
            .map(mapper)
            .collect(Collectors.toList());
    }
    
    /**
     * Partition collection into batches
     * 
     * @param collection Collection to partition
     * @param batchSize Size of each batch
     * @return List of batches
     */
    public static <T> List<List<T>> partition(Collection<T> collection, int batchSize) {
        if (isEmpty(collection) || batchSize <= 0) return Collections.emptyList();
        
        return collection.stream()
            .collect(Collectors.groupingBy(e -> collection.stream()
                .takeWhile(x -> !x.equals(e)).count() / batchSize))
            .values()
            .stream()
            .collect(Collectors.toList());
    }
    
    /**
     * Flatten nested collections
     * 
     * @param collection Collection of collections
     * @return Flattened list
     */
    public static <T> List<T> flatten(Collection<Collection<T>> collection) {
        if (isEmpty(collection)) return Collections.emptyList();
        return collection.stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }
    
    /**
     * Get distinct items from collection
     * 
     * @param collection Collection
     * @return List of distinct items
     */
    public static <T> List<T> distinct(Collection<T> collection) {
        if (isEmpty(collection)) return Collections.emptyList();
        return collection.stream()
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Find first item matching predicate
     * 
     * @param collection Collection to search
     * @param predicate Filter predicate
     * @return Optional containing found item
     */
    public static <T> Optional<T> findFirst(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection)) return Optional.empty();
        return collection.stream()
            .filter(predicate)
            .findFirst();
    }
    
    /**
     * Check if any item matches predicate
     * 
     * @param collection Collection to check
     * @param predicate Filter predicate
     * @return true if any item matches
     */
    public static <T> boolean anyMatch(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection)) return false;
        return collection.stream().anyMatch(predicate);
    }
}
\\\

---

##  Validation Utilities Example

\\\java
/**
 * Business validation helper methods
 */
@UtilityClass
@Slf4j
public class BusinessValidationUtils {
    
    /**
     * Validate task creation request
     * 
     * @param request Task creation request
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateCreateTaskRequest(CreateTaskRequest request) {
        ValidationUtils.notBlank(request.getTitle(), "Task title");
        
        if (request.getTitle().length() < 3) {
            throw new IllegalArgumentException("Task title must be at least 3 characters");
        }
        
        if (request.getTitle().length() > 255) {
            throw new IllegalArgumentException("Task title cannot exceed 255 characters");
        }
        
        ValidationUtils.notNull(request.getProjectId(), "Project ID");
        
        if (request.getDueDate() != null && DateTimeUtils.isPast(request.getDueDate().toLocalDate())) {
            throw new IllegalArgumentException("Due date cannot be in the past");
        }
    }
    
    /**
     * Validate user registration request
     * 
     * @param request User registration request
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateRegisterUserRequest(CreateUserRequest request) {
        ValidationUtils.notBlank(request.getUsername(), "Username");
        ValidationUtils.notBlank(request.getEmail(), "Email");
        ValidationUtils.notBlank(request.getPassword(), "Password");
        
        if (!ValidationUtils.isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        if (!ValidationUtils.isStrongPassword(request.getPassword())) {
            throw new IllegalArgumentException("Password does not meet strength requirements");
        }
    }
}
\\\

---

##  Constants

\\\java
/**
 * Application-wide constants
 */
public class Constants {
    
    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    
    // Task
    public static final int TASK_TITLE_MIN_LENGTH = 3;
    public static final int TASK_TITLE_MAX_LENGTH = 255;
    public static final int TASK_DESCRIPTION_MAX_LENGTH = 5000;
    
    // User
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 50;
    public static final int PASSWORD_MIN_LENGTH = 8;
    
    // Validation
    public static final int EMAIL_MAX_LENGTH = 255;
    public static final int PHONE_MIN_LENGTH = 10;
    public static final int PHONE_MAX_LENGTH = 20;
    
    // Cache
    public static final long CACHE_DURATION_MINUTES = 30;
    public static final long CACHE_DURATION_HOURS = 1;
    
    // API
    public static final String API_VERSION_V1 = "/api/v1";
    public static final String API_VERSION_V2 = "/api/v2";
    
    // Roles
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
    
    private Constants() {
        throw new AssertionError("Utility class cannot be instantiated");
    }
}
\\\

---

##  Utility Best Practices

### 1. Keep Utilities Stateless

\\\java
// GOOD: Stateless utility with static methods
@UtilityClass
public class DateTimeUtils {
    public static LocalDate getCurrentDate() {
        return LocalDate.now();
    }
}

// BAD: Utility class with state
@Component
public class DateTimeService {
    private LocalDate lastDate;  // State makes it harder to test
}
\\\

### 2. Use Null-Safe Operations

\\\java
// GOOD: Handles null safely
public static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
}

// BAD: Will throw NullPointerException
public static boolean isBlank(String value) {
    return value.trim().isEmpty();
}
\\\

### 3. Use Optional for Return Values

\\\java
// GOOD: Returns Optional
public static Optional<LocalDate> parseDate(String dateStr) {
    try {
        return Optional.of(LocalDate.parse(dateStr));
    } catch (DateTimeParseException e) {
        return Optional.empty();
    }
}

// BAD: Returns null
public static LocalDate parseDate(String dateStr) {
    try {
        return LocalDate.parse(dateStr);
    } catch (DateTimeParseException e) {
        return null;
    }
}
\\\

### 4. Document Edge Cases

\\\java
// GOOD: Documents behavior for null/empty
/**
 * Get safe string length
 * 
 * @param value String
 * @return Length or 0 if null
 */
public static int safeLength(String value) {
    return value == null ? 0 : value.length();
}

// BAD: No documentation of edge cases
public static int getLength(String value) {
    return value.length();
}
\\\

### 5. Use Lombok @UtilityClass

\\\java
// GOOD: Uses @UtilityClass from Lombok
@UtilityClass
public class StringUtils {
    public static boolean isBlank(String value) {
        // Implementation
    }
}

// BAD: Manual constructor hiding
public final class StringUtils {
    private StringUtils() {
        throw new AssertionError("Cannot instantiate");
    }
    
    public static boolean isBlank(String value) {
        // Implementation
    }
}
\\\

### 6. Keep Methods Pure Functions

\\\java
// GOOD: Pure function (no side effects)
public static String capitalize(String value) {
    if (isBlank(value)) return value;
    return value.substring(0, 1).toUpperCase() + value.substring(1);
}

// BAD: Has side effects (logging, external calls)
public static String capitalize(String value) {
    log.info("Capitalizing: {}", value);  // Side effect
    emailService.sendNotification();      // Side effect
    return value.substring(0, 1).toUpperCase() + value.substring(1);
}
\\\

---

##  Testing Utilities

\\\java
/**
 * Unit tests for utilities
 */
class DateTimeUtilsTest {
    
    @Test
    void getCurrentDate_ReturnsToday() {
        LocalDate result = DateTimeUtils.getCurrentDate();
        assertEquals(LocalDate.now(), result);
    }
    
    @Test
    void isPast_WithPastDate_ReturnsTrue() {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        assertTrue(DateTimeUtils.isPast(pastDate));
    }
    
    @Test
    void isPast_WithNullDate_ReturnsFalse() {
        assertFalse(DateTimeUtils.isPast(null));
    }
}

class StringUtilsTest {
    
    @Test
    void isBlank_WithNullValue_ReturnsTrue() {
        assertTrue(StringUtils.isBlank(null));
    }
    
    @Test
    void isBlank_WithWhitespace_ReturnsTrue() {
        assertTrue(StringUtils.isBlank("   "));
    }
    
    @Test
    void toSlug_ConvertsTitleToSlug() {
        assertEquals("hello-world", StringUtils.toSlug("Hello World"));
    }
}

class ValidationUtilsTest {
    
    @Test
    void isValidEmail_WithValidEmail_ReturnsTrue() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"));
    }
    
    @Test
    void isStrongPassword_WithWeakPassword_ReturnsFalse() {
        assertFalse(ValidationUtils.isStrongPassword("password"));
    }
}
\\\

---

##  Utility Checklist

When creating utilities:

- [ ] Keep utilities stateless (use static methods)
- [ ] Handle null inputs safely
- [ ] Use Optional for optional return values
- [ ] Document edge cases and special behavior
- [ ] Make methods pure functions (no side effects)
- [ ] Write comprehensive unit tests
- [ ] Use meaningful names for utility methods
- [ ] Group related utilities in classes
- [ ] Avoid creating utility classes for single-use methods
- [ ] Consider if functionality belongs in domain objects
- [ ] Use @UtilityClass annotation from Lombok
- [ ] Log errors appropriately
- [ ] Provide consistent error messages
- [ ] Reuse existing utilities (don't reinvent the wheel)
- [ ] Document assumptions and limitations

---

##  Related Documentation

- **ARCHITECTURE.md** - Application architecture
- **README.md** - Main project overview
- **Service Layer** - Business logic using utilities
- **Configuration Layer** - Utilities configuration

---

##  Quick Reference

### Common Utility Patterns

\\\java
// Date/Time
LocalDate date = DateTimeUtils.getCurrentDate();
LocalDateTime dateTime = DateTimeUtils.getCurrentDateTime();
boolean overdue = DateTimeUtils.isOverdue(dueDate);

// String
boolean blank = StringUtils.isBlank(value);
String slug = StringUtils.toSlug(title);
String masked = StringUtils.maskSensitive(email);

// Validation
boolean validEmail = ValidationUtils.isValidEmail(email);
boolean strongPassword = ValidationUtils.isStrongPassword(password);

// Collections
List<T> filtered = CollectionUtils.filter(items, predicate);
List<R> mapped = CollectionUtils.map(items, mapper);

// Security
Long userId = SecurityUtils.getCurrentUserId();
boolean admin = SecurityUtils.isAdmin();
boolean owns = SecurityUtils.ownsResource(userId);
\\\

### Utility Class Organization

\\\
security/
  SecurityUtils, JwtUtils, PasswordUtils

date/
  DateTimeUtils, TimeZoneUtils, DateFormatUtils

string/
  StringUtils, SlugUtils, TextUtils

validation/
  ValidationUtils, EmailValidator, PhoneValidator

collection/
  CollectionUtils, StreamUtils, PageUtils

constant/
  Constants, RegexPatterns
\\\

---

**Last Updated:** December 1, 2025  
**Version:** 1.0.0  
**Status:** Complete
