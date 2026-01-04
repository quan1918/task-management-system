package com.taskmanagement.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark packages, classes, or methods as planned for future implementation.
 * This helps track technical debt and upcoming features in the codebase.
 *
 * @author Task Management Team
 * @since v0.7.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD})
public @interface Planned {
    
    /**
     * Target version for implementation.
     * @return version string (e.g., "v0.9.0", "v1.0.0")
     */
    String version() default "";
    
    /**
     * Detailed description of planned functionality.
     * @return description text
     */
    String description() default "";
    
    /**
     * Related JIRA ticket or issue tracker ID.
     * @return ticket ID (e.g., "TM-123")
     */
    String ticket() default "";
    
    /**
     * Priority level for implementation.
     * @return priority (HIGH, MEDIUM, LOW)
     */
    String priority() default "MEDIUM";
}
