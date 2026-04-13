-- ============================================================
-- 可选：若曾执行过含 system_scope 的旧版脚本，执行本脚本删除残留列与索引
-- 人大金仓 Kingbase / PostgreSQL 兼容，整段执行即可
-- ============================================================

DROP INDEX IF EXISTS public.idx_pgzc_is_system_scope;
DROP INDEX IF EXISTS public.idx_pgzc_ist_system_scope;

ALTER TABLE public.pgzc_indicator_system DROP COLUMN IF EXISTS system_scope;
ALTER TABLE public.pgzc_indicator_system_template DROP COLUMN IF EXISTS system_scope;
