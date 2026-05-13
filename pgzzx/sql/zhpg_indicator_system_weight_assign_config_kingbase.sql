-- 增加权重分配算法全局配置字段
ALTER TABLE pgzc_indicator_system ADD COLUMN IF NOT EXISTS weight_assign_config TEXT;
COMMENT ON COLUMN pgzc_indicator_system.weight_assign_config IS '默认权重分配算法配置JSON';
