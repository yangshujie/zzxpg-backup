-- =============================================================================
-- 评估结果表扩展（人大金仓 / PostgreSQL 兼容）
-- 当前代码 EvalResult.insert 依赖下列列；未执行本脚本会出现：
--   column "indicator_system_id" of relation "pgzc_eval_result" does not exist
-- 请在 ZHPG 实际连接的同一业务库中执行后重启服务（或无需重启仅重试即可）。
-- 若 Kingbase 版本不支持 ADD COLUMN IF NOT EXISTS，请改为去掉 IF NOT EXISTS 逐条执行（已存在会报错可忽略）。
-- =============================================================================

ALTER TABLE pgzc_eval_result ADD COLUMN IF NOT EXISTS indicator_system_id BIGINT DEFAULT NULL;
COMMENT ON COLUMN pgzc_eval_result.indicator_system_id IS '关联指标体系主键';

ALTER TABLE pgzc_eval_result ADD COLUMN IF NOT EXISTS indicator_system_name VARCHAR(256) DEFAULT NULL;
COMMENT ON COLUMN pgzc_eval_result.indicator_system_name IS '指标体系名称快照';

ALTER TABLE pgzc_eval_result ADD COLUMN IF NOT EXISTS report_payload_json TEXT DEFAULT NULL;
COMMENT ON COLUMN pgzc_eval_result.report_payload_json IS '报告与流水线 JSON：reportDataContext、outputTargets、executionChain 等';
