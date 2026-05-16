-- =====================================================================
-- V1: Initial Schema
-- Dùng IF NOT EXISTS để an toàn khi DB đã có dữ liệu từ ddl-auto:update
-- =====================================================================

CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    email           VARCHAR(100)    NOT NULL UNIQUE,
    full_name       VARCHAR(100)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    refresh_token   VARCHAR(2000),
    roles           VARCHAR(100)    NOT NULL DEFAULT 'ROLE_USER',
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP,
    deleted_by      BIGINT
);

CREATE TABLE IF NOT EXISTS projects (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL,
    description TEXT,
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    start_date  DATE,
    end_date    DATE,
    owner_id    BIGINT          NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tasks (
    id               BIGSERIAL       PRIMARY KEY,
    title            VARCHAR(255)    NOT NULL,
    description      TEXT            NOT NULL,
    status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    priority         VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM',
    due_date         TIMESTAMP       NOT NULL,
    start_date       TIMESTAMP,
    completed_at     TIMESTAMP,
    estimated_hours  INTEGER,
    notes            VARCHAR(1000),
    project_id       BIGINT          REFERENCES projects(id),
    deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMP,
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by       BIGINT,
    updated_by       BIGINT
);

CREATE TABLE IF NOT EXISTS task_assignees (
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, user_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_project_name    ON projects (name);
CREATE INDEX IF NOT EXISTS idx_project_owner   ON projects (owner_id);
CREATE INDEX IF NOT EXISTS idx_project_active  ON projects (active);

CREATE INDEX IF NOT EXISTS idx_task_status     ON tasks (status);
CREATE INDEX IF NOT EXISTS idx_task_priority   ON tasks (priority);
CREATE INDEX IF NOT EXISTS idx_task_project    ON tasks (project_id);
CREATE INDEX IF NOT EXISTS idx_task_due_date   ON tasks (due_date);
CREATE INDEX IF NOT EXISTS idx_task_deleted    ON tasks (deleted);
CREATE INDEX IF NOT EXISTS idx_task_deleted_at ON tasks (deleted_at);

CREATE INDEX IF NOT EXISTS idx_task_assignee_task ON task_assignees (task_id);
CREATE INDEX IF NOT EXISTS idx_task_assignee_user ON task_assignees (user_id);
