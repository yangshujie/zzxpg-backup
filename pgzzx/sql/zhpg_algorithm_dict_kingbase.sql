-- =============================================
-- 算法模型配置子系统(SFMX) - 字典数据
-- 请在 RuoYi 系统库（sys_dict_type 所在库）中执行
--
-- Kingbase/PostgreSQL：sys_dict_type.dict_id、sys_dict_data.dict_code 无默认值，
-- 省略主键会得到 0，导致第二条字典数据报 duplicate key。本脚本用 MAX+1 动态分配主键。
-- =============================================

-- ----------------------------
-- 算法类型字典
-- ----------------------------
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_id) FROM sys_dict_type), 0) + 1,
       '算法类型', 'pgzc_algorithm_type', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '算法模型配置-算法类型'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'pgzc_algorithm_type');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       1, '预处理算法', '预处理算法', 'pgzc_algorithm_type', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'pgzc_algorithm_type' AND dict_value = '预处理算法');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       2, '指标量化算法', '指标量化算法', 'pgzc_algorithm_type', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'pgzc_algorithm_type' AND dict_value = '指标量化算法');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       3, '属性值计算方法', '属性值计算方法', 'pgzc_algorithm_type', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'pgzc_algorithm_type' AND dict_value = '属性值计算方法');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       4, '权重分配', '权重分配', 'pgzc_algorithm_type', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'pgzc_algorithm_type' AND dict_value = '权重分配');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       5, '权重传导', '权重传导', 'pgzc_algorithm_type', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'pgzc_algorithm_type' AND dict_value = '权重传导');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       6, '方案评价', '方案评价', 'pgzc_algorithm_type', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'pgzc_algorithm_type' AND dict_value = '方案评价');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       7, '其它算法', '其它算法', 'pgzc_algorithm_type', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'pgzc_algorithm_type' AND dict_value = '其它算法');

-- 装备类型与指标体系一致，使用前端常量 ZHPG_EQUIPMENT_TYPE_OPTIONS（库表字段存编码如 SPACE_RECON），不再单独维护 pgzc_equipment_type 字典。

-- ----------------------------
-- 算法参数类别字典
-- ----------------------------
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_id) FROM sys_dict_type), 0) + 1,
       '算法参数类别', 'pgzc_param_category', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), '算法参数类别'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'pgzc_param_category');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       1, '输入参数', 'INPUT', 'pgzc_param_category', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'pgzc_param_category' AND dict_value = 'INPUT');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       2, '输出参数', 'OUTPUT', 'pgzc_param_category', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'pgzc_param_category' AND dict_value = 'OUTPUT');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(dict_code) FROM sys_dict_data), 0) + 1,
       3, '配置参数', 'CONFIG', 'pgzc_param_category', '', '', 'N', '0', 'admin', NOW(), '', CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR)
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'pgzc_param_category' AND dict_value = 'CONFIG');
