-- 已有库补齐：评估数据源表增加 remark（与 BaseEntity / MyBatis-Plus 字段一致）
ALTER TABLE pgzc_eval_data_source ADD COLUMN IF NOT EXISTS remark VARCHAR(500) DEFAULT NULL;
COMMENT ON COLUMN pgzc_eval_data_source.remark IS '备注';
