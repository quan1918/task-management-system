-- =====================================================================
-- V3: Ensure created_by / updated_by exist in tasks
-- ADD COLUMN IF NOT EXISTS — safe to run even if columns already exist
-- =====================================================================

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;