-- ============================================================
-- 评估指标表结构优化：增加来源体系追踪
-- ============================================================

-- 添加关联指标体系ID和名称字段
ALTER TABLE pgzc_indicator ADD COLUMN source_system_id BIGINT;
ALTER TABLE pgzc_indicator ADD COLUMN source_system_name VARCHAR(255);

-- 添加字段注释（针对 Kingbase/PostgreSQL 风格）
COMMENT ON COLUMN pgzc_indicator.source_system_id IS '关联指标体系ID';
COMMENT ON COLUMN pgzc_indicator.source_system_name IS '关联指标体系名称';
