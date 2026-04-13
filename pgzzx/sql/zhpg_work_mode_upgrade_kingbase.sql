-- ============================================================
-- ZHPG 工作模式字段升级脚本（Kingbase）
-- 目标：
-- 1) 为指标表新增 work_mode 字段
-- 2) 为指标体系表新增 work_mode 字段
-- 3) 为指标模板表新增 work_mode 字段
-- 4) 为指标体系模板表新增 work_mode 字段
-- 说明：
-- - 历史数据不强制回填，避免误覆盖已有 calcMethod / indicatorTree 中的真实工作模式
-- - 新增/编辑后的数据由前后端共同写入该字段
-- ============================================================

BEGIN;

ALTER TABLE public.pgzc_indicator
    ADD COLUMN IF NOT EXISTS work_mode VARCHAR(64);

COMMENT ON COLUMN public.pgzc_indicator.work_mode IS
'工作模式：MAIN_BRANCH_COLLAB / INTERNAL_CIRCULATION';

ALTER TABLE public.pgzc_indicator_system
    ADD COLUMN IF NOT EXISTS work_mode VARCHAR(64);

COMMENT ON COLUMN public.pgzc_indicator_system.work_mode IS
'工作模式：MAIN_BRANCH_COLLAB / INTERNAL_CIRCULATION';

ALTER TABLE public.pgzc_indicator_template
    ADD COLUMN IF NOT EXISTS work_mode VARCHAR(64);

COMMENT ON COLUMN public.pgzc_indicator_template.work_mode IS
'工作模式：MAIN_BRANCH_COLLAB / INTERNAL_CIRCULATION';

ALTER TABLE public.pgzc_indicator_system_template
    ADD COLUMN IF NOT EXISTS work_mode VARCHAR(64);

COMMENT ON COLUMN public.pgzc_indicator_system_template.work_mode IS
'工作模式：MAIN_BRANCH_COLLAB / INTERNAL_CIRCULATION';

COMMIT;
