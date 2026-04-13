-- =============================================================================
-- 删除已废弃列 initial_tree（对比基准改由 indicator_tree 承担）
-- 人大金仓 / PostgreSQL：DROP COLUMN IF EXISTS
-- =============================================================================

ALTER TABLE public.pgzc_indicator_system DROP COLUMN IF EXISTS initial_tree;
