-- 移除 ZHPG 模块中的逻辑删除字段（切换为物理删除）
-- 适用于 KingBase / MySQL

ALTER TABLE pgzc_algorithm DROP COLUMN IF EXISTS del_flag;
ALTER TABLE pgzc_algorithm_param DROP COLUMN IF EXISTS del_flag;
ALTER TABLE pgzc_algorithm_requirement DROP COLUMN IF EXISTS del_flag;
ALTER TABLE pgzc_calc_flow_execution DROP COLUMN IF EXISTS del_flag;
ALTER TABLE pgzc_calc_flow_template DROP COLUMN IF EXISTS del_flag;
ALTER TABLE pgzc_calc_task DROP COLUMN IF EXISTS del_flag;
ALTER TABLE pgzc_eval_data_source DROP COLUMN IF EXISTS del_flag;
ALTER TABLE pgzc_indicator DROP COLUMN IF EXISTS del_flag;
ALTER TABLE pgzc_indicator_system DROP COLUMN IF EXISTS del_flag;
ALTER TABLE pgzc_eval_result DROP COLUMN IF EXISTS del_flag;
ALTER TABLE pgzc_eval_report_instance DROP COLUMN IF EXISTS del_flag;
ALTER TABLE pgzc_report_template DROP COLUMN IF EXISTS deleted;
