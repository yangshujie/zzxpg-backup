-- 已有库补齐：算法参数表增加 remark（与 BaseEntity / MyBatis-Plus 字段一致）
ALTER TABLE pgzc_algorithm_param ADD COLUMN IF NOT EXISTS remark VARCHAR(500) DEFAULT NULL;
COMMENT ON COLUMN pgzc_algorithm_param.remark IS '备注';
