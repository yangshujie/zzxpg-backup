-- =============================================
-- 评估报告辅助生成分系统 - 菜单数据 (三菜单版)
-- 请在 RuoYi 系统库（sys_menu 所在库）中执行
-- =============================================

-- 1. 一级目录：评估报告辅助生成
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '评估报告辅助生成', 0, 5, 'report', NULL, '', '1', '0', 'M', '0', '0', '', 'documentation', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '评估报告辅助生成分系统'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '评估报告辅助生成' AND parent_id = 0);

-- 2. 二级菜单：模板构建
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '模板构建',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '评估报告辅助生成' AND parent_id = 0 LIMIT 1),
       1, 'builder', 'zhpg/report/builder', '', '1', '0', 'C', '0', '0', 'zhpg:report:builder', 'edit', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '设计报告模板'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '模板构建' AND parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '评估报告辅助生成' AND parent_id = 0 LIMIT 1));

-- 3. 二级菜单：模板管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '模板管理',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '评估报告辅助生成' AND parent_id = 0 LIMIT 1),
       2, 'manage', 'zhpg/report/manage', '', '1', '0', 'C', '0', '0', 'zhpg:report:manage', 'list', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '查看模板与版本'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '模板管理' AND parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '评估报告辅助生成' AND parent_id = 0 LIMIT 1));

-- 4. 二级菜单：报告生成
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '报告生成',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '评估报告辅助生成' AND parent_id = 0 LIMIT 1),
       3, 'generate', 'zhpg/report/generate', '', '1', '0', 'C', '0', '0', 'zhpg:report:generate', 'download', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '生成并下载报告'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '报告生成' AND parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '评估报告辅助生成' AND parent_id = 0 LIMIT 1));
