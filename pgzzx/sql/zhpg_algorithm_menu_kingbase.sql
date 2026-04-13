-- =============================================
-- 算法模型配置子系统(SFMX) - 菜单数据
-- 请在 RuoYi 系统库（sys_menu 所在库）中执行
--
-- 菜单结构:
--   算法模型配置(目录)          ← 一级目录
--     └─ 算法管理(菜单页)      ← 二级菜单页面
--         ├─ 算法查询(按钮)
--         ├─ 算法新增(按钮)
--         ├─ 算法修改(按钮)
--         ├─ 算法删除(按钮)
--         ├─ 算法发布(按钮)
--         └─ 算法导出(按钮)
-- =============================================

-- 1. 一级目录：算法模型配置
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '算法模型配置', 0, 4, 'sfmx', NULL, '', '1', '0', 'M', '0', '0', '', 'cpu', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '算法模型配置目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '算法模型配置' AND parent_id = 0);

-- 2. 二级菜单：算法管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '算法管理',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '算法模型配置' AND parent_id = 0 LIMIT 1),
       1, 'algorithm', 'zhpg/sfmx/algorithm/index', '', '1', '0', 'C', '0', '0', 'zhpg:algorithm:list', 'example', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '算法管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '算法管理' AND perms = 'zhpg:algorithm:list');

-- 3. 按钮权限：算法查询
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '算法查询',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '算法管理' AND perms = 'zhpg:algorithm:list' LIMIT 1),
       1, '', '', '', '1', '0', 'F', '0', '0', 'zhpg:algorithm:query', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:algorithm:query');

-- 4. 按钮权限：算法新增
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '算法新增',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '算法管理' AND perms = 'zhpg:algorithm:list' LIMIT 1),
       2, '', '', '', '1', '0', 'F', '0', '0', 'zhpg:algorithm:add', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:algorithm:add');

-- 5. 按钮权限：算法修改
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '算法修改',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '算法管理' AND perms = 'zhpg:algorithm:list' LIMIT 1),
       3, '', '', '', '1', '0', 'F', '0', '0', 'zhpg:algorithm:edit', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:algorithm:edit');

-- 6. 按钮权限：算法删除
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '算法删除',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '算法管理' AND perms = 'zhpg:algorithm:list' LIMIT 1),
       4, '', '', '', '1', '0', 'F', '0', '0', 'zhpg:algorithm:remove', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:algorithm:remove');

-- 7. 按钮权限：算法发布
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '算法发布',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '算法管理' AND perms = 'zhpg:algorithm:list' LIMIT 1),
       5, '', '', '', '1', '0', 'F', '0', '0', 'zhpg:algorithm:publish', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:algorithm:publish');

-- 8. 按钮权限：算法导出
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '算法导出',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '算法管理' AND perms = 'zhpg:algorithm:list' LIMIT 1),
       6, '', '', '', '1', '0', 'F', '0', '0', 'zhpg:algorithm:export', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:algorithm:export');
