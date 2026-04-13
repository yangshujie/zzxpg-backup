-- ============================================================
-- ZHPG 指标分类规则调整 - 数据库升级脚本（人大金仓 Kingbase）
-- 1) 为指标表和指标模板表新增 system_type 字段（对应一能三性功能维度）
-- 2) 扩展 indicator_type 以支持多装备聚合类型
-- ============================================================

BEGIN;

-- 1) pgzc_indicator 表扩展
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS system_type VARCHAR(64);
COMMENT ON COLUMN public.pgzc_indicator.system_type IS '指标集类型/一能三性：EQUIPMENT_COMBAT_EFFECTIVENESS, COMBAT_APPLICABILITY, SYSTEM_APPLICABILITY, SERVICE_APPLICABILITY';

-- 2) pgzc_indicator_template 表扩展
ALTER TABLE public.pgzc_indicator_template ADD COLUMN IF NOT EXISTS system_type VARCHAR(64);
COMMENT ON COLUMN public.pgzc_indicator_template.system_type IS '指标集类型/一能三性维度';

-- 3) 插入新的“体系聚合”字典项
-- 先获取字典类型的 ID
DO $$
DECLARE
    dict_id_val BIGINT;
    next_code BIGINT;
BEGIN
    SELECT dict_id INTO dict_id_val FROM ry.sys_dict_type WHERE dict_type = 'zhpg_indicator_type';
    
    IF dict_id_val IS NOT NULL THEN
        -- 检查是否已存在 SYSTEM_AGGREGATION
        IF NOT EXISTS (SELECT 1 FROM ry.sys_dict_data WHERE dict_type = 'zhpg_indicator_type' AND dict_value = 'SYSTEM_AGGREGATION') THEN
            SELECT COALESCE(MAX(dict_code), 0) + 1 INTO next_code FROM ry.sys_dict_data;
            
            INSERT INTO ry.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
            VALUES (next_code, 7, '体系聚合', 'SYSTEM_AGGREGATION', 'zhpg_indicator_type', '', 'primary', 'N', '0', 'admin', CURRENT_TIMESTAMP, '用于跨装备体系评估的聚合节点');
        END IF;
    END IF;
END $$;

COMMIT;
