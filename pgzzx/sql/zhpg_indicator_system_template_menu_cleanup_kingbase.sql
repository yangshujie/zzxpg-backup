-- =============================================================================
-- 指标体系模板菜单清理（在 ruoyi-system 库执行）
-- 「指标体系模板管理」已合并到「指标体系管理」页面，单独菜单项不再需要。
-- 配套：zhpg_indicator_system_merge_template_kingbase.sql（在 zhpg 库执行）
-- =============================================================================

BEGIN;

DELETE FROM sys_menu WHERE perms = 'zhpg:indicatorSystemTemplate:list';

COMMIT;
