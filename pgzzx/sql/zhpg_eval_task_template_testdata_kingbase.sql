-- ============================================================
-- 任务模板管理(RWGJ) - 测试数据（人大金仓 / Kingbase，PostgreSQL 兼容）
-- 依赖表：pgzc_indicator_system / pgzc_calc_flow_template / pgzc_eval_task_template
-- 顺序：先清理废弃列 → 补下拉数据源 → 插入 6 条任务模板。
-- 幂等：按唯一标识 WHERE NOT EXISTS 跳过已存在记录，可重复执行。
-- 注意：指标体系下拉接口 /zhpg/indicatorSystem/select 仅返回
--       requirement_id IS NOT NULL 且 is_template=0 的记录，故此处必须给 requirement_id。
-- ============================================================

-- ---------- 0. 清理废弃列 + 补遗漏列 ----------
ALTER TABLE pgzc_calc_flow_template  DROP COLUMN IF EXISTS del_flag;
ALTER TABLE pgzc_eval_task_template DROP COLUMN IF EXISTS deleted;
ALTER TABLE pgzc_eval_task_template DROP COLUMN IF EXISTS version_no;
ALTER TABLE pgzc_eval_task_template ADD COLUMN IF NOT EXISTS remark VARCHAR(500);

-- ---------- 1. 指标体系（下拉数据源） ----------
INSERT INTO pgzc_indicator_system
  (system_name, work_mode, requirement_id, is_applied, is_template, status, description, create_by, create_time, update_time)
SELECT '航天侦察体系效能指标体系', 'INTERNAL_CIRCULATION', 90001, 1, 0, 'PUBLISHED', '测试数据·航天侦察方向', 'admin', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM pgzc_indicator_system WHERE system_name = '航天侦察体系效能指标体系');

INSERT INTO pgzc_indicator_system
  (system_name, work_mode, requirement_id, is_applied, is_template, status, description, create_by, create_time, update_time)
SELECT '太空态势感知贡献率指标体系', 'INTERNAL_CIRCULATION', 90002, 1, 0, 'PUBLISHED', '测试数据·态势感知方向', 'admin', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM pgzc_indicator_system WHERE system_name = '太空态势感知贡献率指标体系');

-- ---------- 2. 计算流程模板（下拉数据源） ----------
INSERT INTO pgzc_calc_flow_template
  (template_code, template_name, task_type, equipment_type, calc_granularity, config_json, version_no, status, description, create_by, create_time, update_time)
SELECT 'CF-TEST-001', '作战效能标准计算流程', 'PERFORMANCE_TEST', 'space_recon', 'EQUIP_EFFECTIVENESS', '{}', 'V1.0', 'PUBLISHED', '测试数据·作战效能', 'admin', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM pgzc_calc_flow_template WHERE template_code = 'CF-TEST-001');

INSERT INTO pgzc_calc_flow_template
  (template_code, template_name, task_type, equipment_type, calc_granularity, config_json, version_no, status, description, create_by, create_time, update_time)
SELECT 'CF-TEST-002', '体系贡献率标准计算流程', 'SYSTEM_TASK', 'space_domain_awareness', 'SYSTEM_TASK', '{}', 'V1.0', 'PUBLISHED', '测试数据·体系贡献率', 'admin', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM pgzc_calc_flow_template WHERE template_code = 'CF-TEST-002');

-- ---------- 3. 任务模板（6 条，覆盖三类评估方法 / 通用·专用 / 各状态） ----------
INSERT INTO pgzc_eval_task_template
  (template_code, template_name, template_type, classification, equipment_type, calc_granularity, indicator_system_id, calc_flow_template_id, status, description, create_by, create_time, update_time)
SELECT 'TPL-TEST-001', '航天侦察作战效能评估模板', 'EQUIP_EFFECTIVENESS', 'SPECIFIC', 'space_recon', 'EQUIP_EFFECTIVENESS',
       (SELECT id FROM pgzc_indicator_system   WHERE system_name = '航天侦察体系效能指标体系' ORDER BY id LIMIT 1),
       (SELECT id FROM pgzc_calc_flow_template WHERE template_code = 'CF-TEST-001'         ORDER BY id LIMIT 1),
       'PUBLISHED', '测试数据·专用模板（已启用）', 'admin', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM pgzc_eval_task_template WHERE template_code = 'TPL-TEST-001');

INSERT INTO pgzc_eval_task_template
  (template_code, template_name, template_type, classification, equipment_type, calc_granularity, indicator_system_id, calc_flow_template_id, status, description, create_by, create_time, update_time)
SELECT 'TPL-TEST-002', '太空态势感知体系贡献率评估模板', 'SYSTEM_CONTRIBUTION', 'SPECIFIC', 'space_domain_awareness', 'SYSTEM_TASK',
       (SELECT id FROM pgzc_indicator_system   WHERE system_name = '太空态势感知贡献率指标体系' ORDER BY id LIMIT 1),
       (SELECT id FROM pgzc_calc_flow_template WHERE template_code = 'CF-TEST-002'           ORDER BY id LIMIT 1),
       'TESTING', '测试数据·专用模板（测试中）', 'admin', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM pgzc_eval_task_template WHERE template_code = 'TPL-TEST-002');

INSERT INTO pgzc_eval_task_template
  (template_code, template_name, template_type, classification, equipment_type, calc_granularity, indicator_system_id, calc_flow_template_id, status, description, create_by, create_time, update_time)
SELECT 'TPL-TEST-003', '通用作战任务满足度评估模板', 'TASK_SATISFACTION', 'GENERAL', NULL, 'EXERCISE_TRAINING',
       NULL, NULL,
       'DRAFT', '测试数据·通用模板（草稿，未引用）', 'admin', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM pgzc_eval_task_template WHERE template_code = 'TPL-TEST-003');

INSERT INTO pgzc_eval_task_template
  (template_code, template_name, template_type, classification, equipment_type, calc_granularity, indicator_system_id, calc_flow_template_id, status, description, create_by, create_time, update_time)
SELECT 'TPL-TEST-004', '航天测运控作战效能评估模板', 'EQUIP_EFFECTIVENESS', 'SPECIFIC', 'space_track_control', 'PERFORMANCE',
       (SELECT id FROM pgzc_indicator_system   WHERE system_name = '航天侦察体系效能指标体系' ORDER BY id LIMIT 1),
       (SELECT id FROM pgzc_calc_flow_template WHERE template_code = 'CF-TEST-001'         ORDER BY id LIMIT 1),
       'PUBLISHED', '测试数据·专用模板（已启用）', 'admin', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM pgzc_eval_task_template WHERE template_code = 'TPL-TEST-004');

INSERT INTO pgzc_eval_task_template
  (template_code, template_name, template_type, classification, equipment_type, calc_granularity, indicator_system_id, calc_flow_template_id, status, description, create_by, create_time, update_time)
SELECT 'TPL-TEST-005', '太空攻防体系贡献率评估模板', 'SYSTEM_CONTRIBUTION', 'SPECIFIC', 'space_defense', 'SYSTEM_TASK',
       (SELECT id FROM pgzc_indicator_system WHERE system_name = '太空态势感知贡献率指标体系' ORDER BY id LIMIT 1),
       NULL,
       'DISABLED', '测试数据·专用模板（已停用）', 'admin', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM pgzc_eval_task_template WHERE template_code = 'TPL-TEST-005');

INSERT INTO pgzc_eval_task_template
  (template_code, template_name, template_type, classification, equipment_type, calc_granularity, indicator_system_id, calc_flow_template_id, status, description, create_by, create_time, update_time)
SELECT 'TPL-TEST-006', '通用作战效能评估模板', 'EQUIP_EFFECTIVENESS', 'GENERAL', NULL, 'SYSTEM_TASK',
       NULL, NULL,
       'PUBLISHED', '测试数据·通用模板（已启用，未引用）', 'admin', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM pgzc_eval_task_template WHERE template_code = 'TPL-TEST-006');

-- 校验：SELECT template_code, template_name, template_type, classification, status,
--              indicator_system_id, calc_flow_template_id
--       FROM pgzc_eval_task_template WHERE template_code LIKE 'TPL-TEST-%' ORDER BY template_code;
