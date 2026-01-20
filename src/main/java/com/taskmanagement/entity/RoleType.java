package com.taskmanagement.entity;

public enum RoleType {
    /**
     * ADMIN
     * Create/Delete/Update any project, task, user
     * Full access to all system features
     */
    ROLE_ADMIN,

    /**
     * USER
     * Create/Update tasks in assigned projects
     * Not delete projects or users
     */
    ROLE_USER;

    /**
     * getAuthority - Returns string of the role
     * use in Spring Security authorities
     * @return "ROLE_ADMIN" or "ROLE_USER"
     */
    public String getAuthority() {
        return this.name();
    }
}
