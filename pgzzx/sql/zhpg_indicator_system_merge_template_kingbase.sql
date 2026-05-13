-- =============================================================================
-- 指标体系模板合并到指标体系（人大金仓 / PostgreSQL 兼容写法）
-- 1) pgzc_indicator_system 增加 is_template 列（0=实例 1=模板）
-- 2) 旧 pgzc_indicator_system_template 表整表丢弃（不迁移历史模板数据）
-- 请将 schema 名 public 按需改为实际 schema。
-- 注：菜单清理见 zhpg_indicator_system_template_menu_cleanup_kingbase.sql（在 ruoyi-system 库执行）
-- =============================================================================

BEGIN;

ALTER TABLE public.pgzc_indicator_system
    ADD COLUMN IF NOT EXISTS is_template SMALLINT NOT NULL DEFAULT 0;
COMMENT ON COLUMN public.pgzc_indicator_system.is_template IS
  '是否模板：0=指标体系实例 1=指标体系模板';
CREATE INDEX IF NOT EXISTS idx_pgzc_is_is_template ON public.pgzc_indicator_system (is_template);

-- 旧模板表整表删除（用户已确认无需保留历史模板数据）
DROP TABLE IF EXISTS public.pgzc_indicator_system_template;

COMMIT;
