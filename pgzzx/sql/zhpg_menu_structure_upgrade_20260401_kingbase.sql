-- ============================================================
-- ZHPG 菜单结构调整脚本（Kingbase / PostgreSQL）
-- 目标：
-- 1) 新增一级目录「模板库管理」
--    子菜单：任务模板库管理（未实现）、指标模板管理、指标体系模板管理、
--           综合分析计算流程模板管理、评估报告模板管理
-- 2) 不调整「能力资源底层」目录及其子菜单（避免影响关联系统）
-- 说明：
-- - 脚本幂等，可重复执行。
-- - 通过 perms 定位已有菜单并迁移 parent_id，避免重复插入。
-- - 「能力资源底层」目录及其子菜单不做任何改动。
--
-- 界面手工修改指引（不执行 SQL 时可用）：
-- 1) 系统管理 -> 菜单管理 -> 新增目录
--    - 菜单名称：模板库管理
--    - 上级菜单：主目录（parent_id=0）
--    - 路由地址(path)：templateLib
--    - 菜单类型：目录(M)
-- 2) 在「模板库管理」下新增/调整 5 个子菜单
--    a) 任务模板库管理（未实现，占位目录）
--       - 类型：目录(M)；path：taskTemplateLib；component 留空
--    b) 指标模板管理
--       - 类型：菜单(C)；path：indicatorTemplate；component：zhpg/zbgj/indicatorTemplate/index
--       - 权限标识(perms)：zhpg:indicatorTemplate:list
--    c) 指标体系模板管理
--       - 类型：菜单(C)；path：indicatorSystemTemplate；component：zhpg/zbgj/indicatorSystemTemplate/index
--       - 权限标识(perms)：zhpg:indicatorSystemTemplate:list
--    d) 综合分析计算流程模板管理
--       - 类型：菜单(C)；path：calcFlow；component：zhpg/zhfx/calcFlow/index
--       - 权限标识(perms)：zhpg:calcFlow:list
--    e) 评估报告模板管理
--       - 类型：菜单(C)；path：reportTemplate；component：zhpg/report/manage
--       - 权限标识(perms)：zhpg:report:manage
-- 3) 若菜单已存在，直接在界面修改「上级菜单」为「模板库管理」并调整显示排序为 1~5。
-- 4) 不要修改「能力资源底层」目录及其子菜单（指标管理/指标体系管理/评估报告管理等）。
-- 5) 若要求“除模板以外的菜单页面”统一挂到「能力资源底座/底层」：
--    可按下方 SQL（已内置）通过 perms 迁移：指标管理、指标体系管理、评估报告管理（及会签管理兜底）。
--
-- 非模板菜单的界面手工修改指引：
-- 1) 系统管理 -> 菜单管理，先确认一级目录名称（能力资源底座 或 能力资源底层）。
-- 2) 依次修改以下菜单的「上级菜单」为该一级目录：
--    - 指标管理（建议 perms：zhpg:indicator:list，排序 1）
--    - 指标体系管理（建议 perms：zhpg:indicatorSystem:list，排序 2）
--    - 评估报告管理（建议 perms：zhpg:report:generate，排序 3）
--    - 会签管理（若存在，排序 4）
-- 3) 若菜单名不一致，可通过路径/组件定位后再改上级：
--    - 指标管理：path=indicator 或 component=zhpg/zbgj/indicator/index
--    - 指标体系管理：path=indicatorSystem 或 component=zhpg/zbgj/indicatorSystem/index
--    - 评估报告管理：path=reportManage 或 component=zhpg/report/generate
--    - 会签管理：path=cosign 或 component 包含 /cosign/
-- 4) 保存后刷新菜单缓存（或重新登录）再验证左侧菜单树。
-- ============================================================

BEGIN;

-- 新增一级目录：模板库管理（若不存在）
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '模板库管理', 0, 4, 'templateLib', NULL, '', '1', '0',
       'M', '0', '0', '', 'clipboard', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '模板库管理目录'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE menu_name = '模板库管理' AND parent_id = 0
);

-- 任务模板库管理（未实现）：目录型占位
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '任务模板库管理',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '模板库管理' AND parent_id = 0 LIMIT 1),
       1, 'taskTemplateLib', NULL, '', '1', '0',
       'M', '0', '0', '', 'form', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '任务模板库管理（未实现）'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE menu_name = '任务模板库管理'
      AND parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '模板库管理' AND parent_id = 0 LIMIT 1)
);

-- 指标模板管理：按 perms 迁移到「模板库管理」
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '指标模板管理',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '模板库管理' AND parent_id = 0 LIMIT 1),
       2, 'indicatorTemplate', 'zhpg/zbgj/indicatorTemplate/index', '', '1', '0',
       'C', '0', '0', 'zhpg:indicatorTemplate:list', 'list', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '指标模板管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:indicatorTemplate:list');

UPDATE sys_menu
SET parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '模板库管理' AND parent_id = 0 LIMIT 1),
    menu_name = '指标模板管理',
    order_num = 2
WHERE perms = 'zhpg:indicatorTemplate:list';

-- 指标体系模板管理：按 perms 迁移到「模板库管理」
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '指标体系模板管理',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '模板库管理' AND parent_id = 0 LIMIT 1),
       3, 'indicatorSystemTemplate', 'zhpg/zbgj/indicatorSystemTemplate/index', '', '1', '0',
       'C', '0', '0', 'zhpg:indicatorSystemTemplate:list', 'list', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '指标体系模板管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:indicatorSystemTemplate:list');

UPDATE sys_menu
SET parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '模板库管理' AND parent_id = 0 LIMIT 1),
    menu_name = '指标体系模板管理',
    order_num = 3
WHERE perms = 'zhpg:indicatorSystemTemplate:list';

-- 综合分析计算流程模板管理：按 perms 迁移到「模板库管理」
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '综合分析计算流程模板管理',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '模板库管理' AND parent_id = 0 LIMIT 1),
       4, 'calcFlow', 'zhpg/zhfx/calcFlow/index', '', '1', '0',
       'C', '0', '0', 'zhpg:calcFlow:list', 'list', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '综合分析计算流程模板管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:calcFlow:list');

UPDATE sys_menu
SET parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '模板库管理' AND parent_id = 0 LIMIT 1),
    menu_name = '综合分析计算流程模板管理',
    order_num = 4
WHERE perms = 'zhpg:calcFlow:list';

-- 评估报告模板管理：复用报告模板管理菜单 perms
UPDATE sys_menu
SET parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '模板库管理' AND parent_id = 0 LIMIT 1),
    menu_name = '评估报告模板管理',
    order_num = 5
WHERE perms = 'zhpg:report:manage';

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT COALESCE((SELECT MAX(menu_id) FROM sys_menu), 0) + 1,
       '评估报告模板管理',
       (SELECT menu_id FROM sys_menu WHERE menu_name = '模板库管理' AND parent_id = 0 LIMIT 1),
       5, 'reportTemplate', 'zhpg/report/manage', '', '1', '0',
       'C', '0', '0', 'zhpg:report:manage', 'list', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '评估报告模板管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'zhpg:report:manage');

-- ============================================================
-- 非模板菜单统一挂载到「能力资源底座/能力资源底层」
-- 仅迁移子菜单，不修改底座目录自身配置
-- ============================================================
WITH resource_parent AS (
    SELECT menu_id
    FROM sys_menu
    WHERE parent_id = 0
      AND menu_type = 'M'
      AND menu_name IN ('能力资源底座', '能力资源底层')
    ORDER BY CASE WHEN menu_name = '能力资源底座' THEN 0 ELSE 1 END, menu_id
    LIMIT 1
)
UPDATE sys_menu m
SET parent_id = (SELECT menu_id FROM resource_parent),
    order_num = CASE m.perms
        WHEN 'zhpg:indicator:list' THEN 1
        WHEN 'zhpg:indicatorSystem:list' THEN 2
        WHEN 'zhpg:report:generate' THEN 3
        ELSE m.order_num
    END
WHERE EXISTS (SELECT 1 FROM resource_parent)
  AND m.perms IN ('zhpg:indicator:list', 'zhpg:indicatorSystem:list', 'zhpg:report:generate');

-- 会签管理兜底迁移（若存在且未配置 perms，则按菜单名/路径特征迁移）
WITH resource_parent AS (
    SELECT menu_id
    FROM sys_menu
    WHERE parent_id = 0
      AND menu_type = 'M'
      AND menu_name IN ('能力资源底座', '能力资源底层')
    ORDER BY CASE WHEN menu_name = '能力资源底座' THEN 0 ELSE 1 END, menu_id
    LIMIT 1
)
UPDATE sys_menu m
SET parent_id = (SELECT menu_id FROM resource_parent),
    order_num = 4
WHERE EXISTS (SELECT 1 FROM resource_parent)
  AND m.menu_type = 'C'
  AND (
      m.menu_name = '会签管理'
      OR m.path = 'cosign'
      OR m.component LIKE '%/cosign/%'
  );

COMMIT;
