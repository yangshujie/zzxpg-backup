-- 指标体系：默认传导方法配置（JSON，与原项目 conductionAlgorithm 结构兼容）
ALTER TABLE pgzc_indicator_system ADD COLUMN IF NOT EXISTS conduction_config TEXT;
COMMENT ON COLUMN pgzc_indicator_system.conduction_config IS '默认/全局传导算法配置JSON（name/k/mainUnit等）';
