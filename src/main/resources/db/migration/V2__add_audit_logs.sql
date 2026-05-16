-- =====================================================================
-- V2: Audit Logs Table
-- =====================================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     BIGINT,
    action      VARCHAR(50)     NOT NULL,
    entity_type VARCHAR(50)     NOT NULL,
    entity_id   BIGINT,
    description VARCHAR(1000),
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_user_id   ON audit_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_entity    ON audit_logs (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs (created_at);