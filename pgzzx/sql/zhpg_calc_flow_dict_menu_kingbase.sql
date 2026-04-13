-- =============================================
-- 综合分析计算流程模板(ZHFX) - 字典 + 菜单数据
-- 请在 RuoYi 系统库（sys_dict_type / sys_menu 所在库）中执行
-- 数据库：KingbaseES / PostgreSQL
-- =============================================

-- =============================================
-- 一、字典类型
-- =============================================

-- 评估任务类型
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_id) FROM sys_dict_type), 0) + 1,
       '评估任务类型', 'zhpg_task_type', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '综合分析计算-评估任务类型'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'zhpg_task_type');

-- 评估粒度
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_id) FROM sys_dict_type), 0) + 1,
       '评估粒度', 'zhpg_calc_granularity', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '综合分析计算-评估粒度'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'zhpg_calc_granularity');

-- 路由策略
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_id) FROM sys_dict_type), 0) + 1,
       '路由策略', 'zhpg_route_strategy', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '综合分析计算-路由策略'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'zhpg_route_strategy');

-- 执行模式
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_id) FROM sys_dict_type), 0) + 1,
       '执行模式', 'zhpg_exec_mode', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '综合分析计算-执行模式'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'zhpg_exec_mode');

-- 流程模板状态
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_id) FROM sys_dict_type), 0) + 1,
       '流程模板状态', 'zhpg_calc_flow_status', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '综合分析计算-流程模板状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'zhpg_calc_flow_status');

-- 计算任务状态
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_id) FROM sys_dict_type), 0) + 1,
       '计算任务状态', 'zhpg_calc_task_status', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '综合分析计算-计算任务状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'zhpg_calc_task_status');

-- =============================================
-- 二、字典数据
-- =============================================

-- ---------- 评估任务类型 ----------
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       1, '性能指标评估', 'PERFORMANCE', 'zhpg_task_type', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_task_type' AND dict_value = 'PERFORMANCE');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       2, '装备作战效能评估', 'EQUIP_EFFECTIVENESS', 'zhpg_task_type', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_task_type' AND dict_value = 'EQUIP_EFFECTIVENESS');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       3, '体系贡献率评估', 'SYSTEM_CONTRIBUTION', 'zhpg_task_type', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_task_type' AND dict_value = 'SYSTEM_CONTRIBUTION');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       4, '作战任务满足度评估', 'TASK_SATISFACTION', 'zhpg_task_type', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_task_type' AND dict_value = 'TASK_SATISFACTION');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       5, '体系级任务评估', 'SYSTEM_TASK', 'zhpg_task_type', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_task_type' AND dict_value = 'SYSTEM_TASK');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       6, '演习演训任务评估', 'EXERCISE_TRAINING', 'zhpg_task_type', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_task_type' AND dict_value = 'EXERCISE_TRAINING');

-- ---------- 评估粒度 ----------
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       1, '装备效能指标评估', 'EQUIP_EFFECTIVENESS', 'zhpg_calc_granularity', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_calc_granularity' AND dict_value = 'EQUIP_EFFECTIVENESS');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       2, '体系级任务评估', 'SYSTEM_TASK', 'zhpg_calc_granularity', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_calc_granularity' AND dict_value = 'SYSTEM_TASK');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       3, '性能指标评估', 'PERFORMANCE', 'zhpg_calc_granularity', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_calc_granularity' AND dict_value = 'PERFORMANCE');

-- ---------- 路由策略 ----------
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       1, '第一个', 'FIRST', 'zhpg_route_strategy', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_route_strategy' AND dict_value = 'FIRST');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       2, '负载均衡', 'LOAD_BALANCE', 'zhpg_route_strategy', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_route_strategy' AND dict_value = 'LOAD_BALANCE');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       3, '最后一个', 'LAST', 'zhpg_route_strategy', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_route_strategy' AND dict_value = 'LAST');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       4, '随机', 'RANDOM', 'zhpg_route_strategy', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_route_strategy' AND dict_value = 'RANDOM');

-- ---------- 执行模式 ----------
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       1, '自动', 'AUTO', 'zhpg_exec_mode', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_exec_mode' AND dict_value = 'AUTO');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       2, '手动确认', 'MANUAL', 'zhpg_exec_mode', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_exec_mode' AND dict_value = 'MANUAL');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       3, '逐步执行', 'STEP_BY_STEP', 'zhpg_exec_mode', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_exec_mode' AND dict_value = 'STEP_BY_STEP');

-- ---------- 流程模板状态 ----------
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       1, '草稿', 'DRAFT', 'zhpg_calc_flow_status', '', 'info', 'Y', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_calc_flow_status' AND dict_value = 'DRAFT');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       2, '测试中', 'TESTING', 'zhpg_calc_flow_status', '', 'warning', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_calc_flow_status' AND dict_value = 'TESTING');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       3, '已启用', 'PUBLISHED', 'zhpg_calc_flow_status', '', 'success', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_calc_flow_status' AND dict_value = 'PUBLISHED');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       4, '已停用', 'DISABLED', 'zhpg_calc_flow_status', '', 'danger', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_calc_flow_status' AND dict_value = 'DISABLED');

-- ---------- 计算任务状态 ----------
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       1, '待执行', 'WAITING', 'zhpg_calc_task_status', '', 'info', 'Y', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_calc_task_status' AND dict_value = 'WAITING');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       2, '运行中', 'RUNNING', 'zhpg_calc_task_status', '', 'primary', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_calc_task_status' AND dict_value = 'RUNNING');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       3, '已暂停', 'PAUSED', 'zhpg_calc_task_status', '', 'warning', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_calc_task_status' AND dict_value = 'PAUSED');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       4, '成功', 'SUCCESS', 'zhpg_calc_task_status', '', 'success', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_calc_task_status' AND dict_value = 'SUCCESS');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       5, '失败', 'FAILED', 'zhpg_calc_task_status', '', 'danger', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_calc_task_status' AND dict_value = 'FAILED');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       6, '已终止', 'TERMINATED', 'zhpg_calc_task_status', '', 'danger', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'zhpg_calc_task_status' AND dict_value = 'TERMINATED');

-- =============================================
-- 三、菜单数据
-- 注意：parent_id 需根据实际系统菜单 ID 调整
-- =============================================

-- 「综合分析计算」一级目录
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '综合分析计算', 0, 6, 'zhfx', NULL, 1, 0, 'M', '0', '0', '', 'monitor', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '综合分析计算目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '综合分析计算' AND parent_id = 0);

-- 「流程模板管理」菜单页
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '流程模板管理',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '综合分析计算' AND parent_id = 0 LIMIT 1),
       1, 'calcFlow', 'zhpg/zhfx/calcFlow/index', 1, 0, 'C', '0', '0', 'zhpg:calcFlow:list', 'list', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '流程模板管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '流程模板管理' AND perms = 'zhpg:calcFlow:list');

-- 按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '流程模板查询',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '流程模板管理' AND perms = 'zhpg:calcFlow:list' LIMIT 1),
       1, '#', '', 1, 0, 'F', '0', '0', 'zhpg:calcFlow:query', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:calcFlow:query');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '流程模板新增',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '流程模板管理' AND perms = 'zhpg:calcFlow:list' LIMIT 1),
       2, '#', '', 1, 0, 'F', '0', '0', 'zhpg:calcFlow:add', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:calcFlow:add');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '流程模板修改',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '流程模板管理' AND perms = 'zhpg:calcFlow:list' LIMIT 1),
       3, '#', '', 1, 0, 'F', '0', '0', 'zhpg:calcFlow:edit', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:calcFlow:edit');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '流程模板删除',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '流程模板管理' AND perms = 'zhpg:calcFlow:list' LIMIT 1),
       4, '#', '', 1, 0, 'F', '0', '0', 'zhpg:calcFlow:remove', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:calcFlow:remove');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '流程模板发布',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '流程模板管理' AND perms = 'zhpg:calcFlow:list' LIMIT 1),
       5, '#', '', 1, 0, 'F', '0', '0', 'zhpg:calcFlow:publish', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:calcFlow:publish');
