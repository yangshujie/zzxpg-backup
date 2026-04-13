-- ============================================================
-- ZHPG 枚举值中文化（RY 字典库专用）
-- 说明：本脚本仅在 ry 所在数据库执行
-- ============================================================

BEGIN;

-- ------------------------------------------------------------
-- 1) 字典值同步（若依字典）
-- ------------------------------------------------------------

-- 装备类型字典（zhpg_equipment_type）
UPDATE ry.sys_dict_data
SET dict_value = CASE dict_value
    WHEN 'SPACE_RECON' THEN '航天侦察'
    WHEN 'SPACE_SITUATIONAL_AWARENESS' THEN '太空态势感知'
    WHEN 'SPACE_OFFENSE_DEFENSE' THEN '太空攻防'
    WHEN 'SPACE_TTC' THEN '航天测运控'
    WHEN 'SPACE_LAUNCH' THEN '航天发射'
    WHEN 'SEA_BASED_SPACE' THEN '海基航天'
    WHEN 'SYSTEM_AGGREGATION' THEN '聚合'
    WHEN 'SPACE_AWARENESS' THEN '太空态势感知'
    WHEN 'SPACE_AD' THEN '太空攻防'
    WHEN 'SPACE_SA' THEN '太空态势感知'
    WHEN 'SEA_BASED' THEN '海基航天'
    WHEN 'SPACE_COMBAT' THEN '太空攻防'
    WHEN 'COMM_COUNTER' THEN '航天测运控'
    WHEN 'NAV_POSITION' THEN '海基航天'
    ELSE dict_value
END
WHERE dict_type = 'zhpg_equipment_type'
  AND dict_value IN (
    'SPACE_RECON', 'SPACE_SITUATIONAL_AWARENESS', 'SPACE_OFFENSE_DEFENSE', 'SPACE_TTC', 'SPACE_LAUNCH',
    'SEA_BASED_SPACE', 'SYSTEM_AGGREGATION', 'SPACE_AWARENESS', 'SPACE_AD', 'SPACE_SA', 'SEA_BASED',
    'SPACE_COMBAT', 'COMM_COUNTER', 'NAV_POSITION'
  );

UPDATE ry.sys_dict_data
SET dict_label = '聚合'
WHERE dict_type = 'zhpg_equipment_type'
  AND dict_label IN ('体系聚合', 'SYSTEM_AGGREGATION');

-- 指标类型字典（历史兼容）
UPDATE ry.sys_dict_data
SET dict_value = '聚合',
    dict_label = '聚合'
WHERE dict_type = 'zhpg_indicator_type'
  AND dict_value = 'SYSTEM_AGGREGATION';

COMMIT;

-- ------------------------------------------------------------
-- 2) 执行后核对（手工执行）
-- ------------------------------------------------------------
-- SELECT dict_type, dict_label, dict_value
-- FROM ry.sys_dict_data
-- WHERE dict_type IN ('zhpg_equipment_type','zhpg_indicator_type')
-- ORDER BY dict_type, dict_sort;

