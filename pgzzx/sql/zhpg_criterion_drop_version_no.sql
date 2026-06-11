-- ============================================================
-- 评估准则管理 - 移除 version_no 字段
-- ============================================================

-- 准则集主表
ALTER TABLE pgzc_eval_criterion_set DROP COLUMN IF EXISTS version_no;

-- 准则明细表
ALTER TABLE pgzc_eval_criterion DROP COLUMN IF EXISTS version_no;
