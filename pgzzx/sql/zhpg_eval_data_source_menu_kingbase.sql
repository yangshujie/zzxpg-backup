BEGIN;

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '评估数据匹配', 0, 5, 'pgsj', NULL, '', '1', '0',
       'M', '0', '0', '', 'database', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '评估数据匹配目录'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE menu_name = '评估数据匹配' AND parent_id = 0
);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '评估数据源管理',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '评估数据匹配' AND parent_id = 0 LIMIT 1),
       1, 'dataSource', 'zhpg/pgsj/dataSource/index', '', '1', '0',
       'C', '0', '0', 'zhpg:dataSource:list', 'list', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '评估数据源管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:dataSource:list');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '评估数据源新增',
       (SELECT menu_id FROM sys_menu WHERE perms = 'zhpg:dataSource:list' LIMIT 1),
       1, '#', '', '', '1', '0',
       'F', '0', '0', 'zhpg:dataSource:add', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '评估数据源新增按钮'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:dataSource:add');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '评估数据源修改',
       (SELECT menu_id FROM sys_menu WHERE perms = 'zhpg:dataSource:list' LIMIT 1),
       2, '#', '', '', '1', '0',
       'F', '0', '0', 'zhpg:dataSource:edit', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '评估数据源修改按钮'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:dataSource:edit');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '评估数据源删除',
       (SELECT menu_id FROM sys_menu WHERE perms = 'zhpg:dataSource:list' LIMIT 1),
       3, '#', '', '', '1', '0',
       'F', '0', '0', 'zhpg:dataSource:remove', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '评估数据源删除按钮'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:dataSource:remove');

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '评估数据源测试',
       (SELECT menu_id FROM sys_menu WHERE perms = 'zhpg:dataSource:list' LIMIT 1),
       4, '#', '', '', '1', '0',
       'F', '0', '0', 'zhpg:dataSource:test', '#', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '评估数据源测试按钮'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:dataSource:test');

COMMIT;
