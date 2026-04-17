-- ============================================================
-- ZHPG 装备类型字典（人大金仓 Kingbase，字典表 schema：ry）
--   zhpg_equipment_type              标准 6 类装备类型 + 体系聚合
--
-- 重要（避免「没作用」/ COMMIT 报错）：
--   1) 不要用单独 a 的 BEGIN/COMMIT：客户端若逐条执行会断开事务，COMMIT 会报
--      there is no transaction in progress。
--   2) 请「整段选中后一次执行」（DBeaver：Execute SQL Script / 执行 SQL 脚本）。
--   3) dict_code 全表唯一，本脚本用「当前 MAX(dict_code)+序号」动态分配，避免主键冲突。
-- 执行后：若依「系统管理 → 字典管理」→ 刷新缓存。
-- ============================================================

-- 字典类型（已存在则跳过）
INSERT INTO ry.sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_id) FROM ry.sys_dict_type), 0) + 1,
       '装备类型', 'zhpg_equipment_type', '0', 'admin', CURRENT_TIMESTAMP, '', NULL,
       '评估指标管理-装备类型'
WHERE NOT EXISTS (SELECT 1 FROM ry.sys_dict_type WHERE dict_type = 'zhpg_equipment_type');

-- 仅删除本业务字典数据，避免固定 dict_code 与现网冲突
DELETE FROM ry.sys_dict_data WHERE dict_type = 'zhpg_equipment_type';

INSERT INTO ry.sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT base.b + ROW_NUMBER() OVER (ORDER BY v.sort),
       v.sort,
       v.dict_label,
       v.dict_value,
       'zhpg_equipment_type',
       '',
       '',
       'N',
       '0',
       'admin',
       CURRENT_TIMESTAMP,
       '',
       CAST(NULL AS TIMESTAMP),
       CAST(NULL AS VARCHAR)
FROM (
         VALUES
             (1, '航天侦察', 'space_recon'),
             (2, '太空态势感知', 'space_domain_awareness'),
             (3, '太空攻防', 'space_defense'),
             (4, '航天测运控', 'space_track_control'),
             (5, '航天发射', 'space_launch'),
             (6, '海基航天', 'sea_based_space'),
             (7, '体系聚合', '无')
     ) AS v (sort, dict_label, dict_value)
         CROSS JOIN (SELECT COALESCE(MAX(dict_code), 0) AS b FROM ry.sys_dict_data) AS base;

-- 建议执行后核对：
-- SELECT dict_type, COUNT(*) FROM ry.sys_dict_data WHERE dict_type LIKE 'zhpg_%' GROUP BY dict_type;
-- 期望：zhpg_equipment_type = 7 行。
