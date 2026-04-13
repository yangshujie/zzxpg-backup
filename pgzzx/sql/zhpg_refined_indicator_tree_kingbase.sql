-- 主分协同：回传细化指标树独立存储（不覆盖 indicator_tree 原始粗建）
-- 字段语义顺序：indicator_tree（原始）→ refined_indicator_tree（回传细化）→ indicator_tree_weight（带权重）

BEGIN;

ALTER TABLE public.pgzc_indicator_system ADD COLUMN IF NOT EXISTS refined_indicator_tree TEXT;
COMMENT ON COLUMN public.pgzc_indicator_system.refined_indicator_tree IS
  '主分协同下分系统回传的细化指标树 JSON；权重计算以此为准（非空时）。不覆盖 indicator_tree。';

COMMIT;
