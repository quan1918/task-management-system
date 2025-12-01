package com.taskmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main entry point for the Task Management System Spring Boot application.
 * 
 * This class bootstraps the Spring Boot application and enables component scanning
 * for dependency injection across the task-management package.
 * 
 * Features enabled by this application:
 * - RESTful API for task operations
 * - Role-based access control (RBAC) via Spring Security
 * - JWT authentication and authorization
 * - PostgreSQL database persistence via JPA/Hibernate
 * - Real-time notifications and audit logging
 * - API documentation via Swagger/OpenAPI
 * 
 * Profile Support:
 * - dev: Development environment with verbose logging (default)
 * - prod: Production environment with optimized settings and env variables
 * 
 * To run the application:
 * 
 * Development:
 *   mvn spring-boot:run
 *   
 * Production:
 *   mvn clean package
 *   java -jar target/task-management-system-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
 * 
 * Application will start on:
 * - Dev: http://localhost:8080/api
 * - Prod: http://localhost:8080/api (or configured port)
 * 
 * API Documentation:
 * - Swagger UI: http://localhost:8080/api/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/api/v3/api-docs
 * 
 * Health Check:
 * - http://localhost:8080/api/actuator/health
 * 
 * @author Development Team
 * @version 1.0.0
 * @since 2025-12-01
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.taskmanagement")
public class TaskManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskManagementApplication.class, args);
    }
}
