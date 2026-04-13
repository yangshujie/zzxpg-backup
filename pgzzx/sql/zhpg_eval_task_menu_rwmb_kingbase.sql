-- 1) 下线前端已移除的「任务模板管理」相关菜单（若库中曾手工配置 component 含 rwmb）
UPDATE sys_menu
SET visible = '1',
    status  = '1',
    remark  = COALESCE(remark, '') || ' [已移除：任务模板管理将重做]'
WHERE menu_type = 'C'
  AND (component LIKE '%zhpg/rwmb%' OR path LIKE '%rwmb%');

-- 2) 「评估任务管理」菜单
--    parent_id：若存在顶级目录「综合分析计算」则挂其下；否则挂顶层 parent_id=0（无该目录时也可用）
--    若你要挂到其它目录：把下面 COALESCE 第二段改成固定 ID，例如 (SELECT 123456::bigint) 或先查 sys_menu 填 menu_id
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '评估任务管理',
       COALESCE(
         (SELECT menu_id FROM sys_menu WHERE menu_name = '综合分析计算' AND parent_id = 0 LIMIT 1),
         0
       ),
       2, 'evalTaskManage', 'zhpg/zhfx/evalTaskManage/index', 1, 0, 'C', '0', '0',
       'zhpg:calcTask:list', 'guide', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '选择指标体系与流程模板发起评估计算（测试）'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:calcTask:list');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '计算任务查询',
       (SELECT menu_id FROM sys_menu WHERE perms = 'zhpg:calcTask:list' LIMIT 1),
       1, '#', '', 1, 0, 'F', '0', '0', 'zhpg:calcTask:query', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:calcTask:query');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '发起评估计算',
       (SELECT menu_id FROM sys_menu WHERE perms = 'zhpg:calcTask:list' LIMIT 1),
       2, '#', '', 1, 0, 'F', '0', '0', 'zhpg:calcTask:run', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:calcTask:run');
