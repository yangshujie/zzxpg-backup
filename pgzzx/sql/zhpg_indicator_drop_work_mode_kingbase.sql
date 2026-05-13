-- ============================================================
-- ZHPG 指标管理 · 移除「工作模式」字段（人大金仓 Kingbase）
--
-- 背景：
--   指标管理（pgzc_indicator / pgzc_indicator_template）作为指标体系的
--   「单条指标」管理面，按 0429 评审决议不再区分工作模式。工作模式仅在
--   指标体系（pgzc_indicator_system）层面保留。
--
-- 说明：
--   1) 删列采用 IF EXISTS，重复执行幂等
--   2) 不破坏 pgzc_indicator_system.work_mode；指标体系页继续使用
--   3) 执行前请确认 ZHPG 服务已停机或已发布到不依赖该列的版本
-- ============================================================

BEGIN;

DO $$
BEGIN
    IF to_regclass('public.pgzc_indicator') IS NULL THEN
        RAISE EXCEPTION '表 public.pgzc_indicator 不存在';
    END IF;
    IF to_regclass('public.pgzc_indicator_template') IS NULL THEN
        RAISE EXCEPTION '表 public.pgzc_indicator_template 不存在';
    END IF;
END $$;

-- 1) pgzc_indicator 移除 work_mode
ALTER TABLE public.pgzc_indicator DROP COLUMN IF EXISTS work_mode;

-- 2) pgzc_indicator_template 移除 work_mode
ALTER TABLE public.pgzc_indicator_template DROP COLUMN IF EXISTS work_mode;

COMMIT;

-- 校验：
-- SELECT column_name FROM information_schema.columns
--   WHERE table_schema='public' AND table_name='pgzc_indicator' ORDER BY ordinal_position;
-- SELECT column_name FROM information_schema.columns
--   WHERE table_schema='public' AND table_name='pgzc_indicator_template' ORDER BY ordinal_position;
