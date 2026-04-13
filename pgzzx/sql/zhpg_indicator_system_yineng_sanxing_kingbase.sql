-- ============================================================
-- ZHPG 指标体系 / 模板：一能三性体系类型编码迁移
-- 人大金仓 Kingbase / PostgreSQL 兼容
-- 请整段执行；执行后重启应用或清缓存
-- ============================================================

BEGIN;

-- 历史体系类型编码映射到「一能三性」标准码
UPDATE public.pgzc_indicator_system
SET system_type = 'EQUIPMENT_COMBAT_EFFECTIVENESS'
WHERE system_type = 'COMBAT_EFFECTIVENESS';

UPDATE public.pgzc_indicator_system
SET system_type = 'SYSTEM_APPLICABILITY'
WHERE system_type = 'SYSTEM_CONTRIBUTION';

UPDATE public.pgzc_indicator_system
SET system_type = 'SERVICE_APPLICABILITY'
WHERE system_type = 'CAPABILITY_MATURITY';

UPDATE public.pgzc_indicator_system_template
SET system_type = 'EQUIPMENT_COMBAT_EFFECTIVENESS'
WHERE system_type = 'COMBAT_EFFECTIVENESS';

UPDATE public.pgzc_indicator_system_template
SET system_type = 'SYSTEM_APPLICABILITY'
WHERE system_type = 'SYSTEM_CONTRIBUTION';

UPDATE public.pgzc_indicator_system_template
SET system_type = 'SERVICE_APPLICABILITY'
WHERE system_type = 'CAPABILITY_MATURITY';

COMMIT;

COMMENT ON COLUMN public.pgzc_indicator_system.system_type IS
'指标集类型/一能三性：EQUIPMENT_COMBAT_EFFECTIVENESS装备作战效能、COMBAT_APPLICABILITY作战适用性、SYSTEM_APPLICABILITY体系适用性、SERVICE_APPLICABILITY在役适用性';

COMMENT ON COLUMN public.pgzc_indicator_system_template.system_type IS
'指标集类型/一能三性：EQUIPMENT_COMBAT_EFFECTIVENESS装备作战效能、COMBAT_APPLICABILITY作战适用性、SYSTEM_APPLICABILITY体系适用性、SERVICE_APPLICABILITY在役适用性';
