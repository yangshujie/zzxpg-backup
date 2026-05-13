-- ============================================================
-- 评估指标及算法装备类型标签转编码迁移脚本 (Kingbase/MySQL)
-- 将 pgzc_indicator 和 pgzc_algorithm 表中的汉字标签统一转换为字典编码
-- ============================================================

-- 1. 更新 pgzc_indicator 表
UPDATE pgzc_indicator SET indicator_type = 'space_recon' WHERE indicator_type = '航天侦察';
UPDATE pgzc_indicator SET indicator_type = 'space_domain_awareness' WHERE indicator_type = '太空态势感知';
UPDATE pgzc_indicator SET indicator_type = 'space_defense' WHERE indicator_type = '太空攻防';
UPDATE pgzc_indicator SET indicator_type = 'space_track_control' WHERE indicator_type = '航天测运控';
UPDATE pgzc_indicator SET indicator_type = 'space_launch' WHERE indicator_type = '航天发射';
UPDATE pgzc_indicator SET indicator_type = 'sea_based_space' WHERE indicator_type = '海基航天';
UPDATE pgzc_indicator SET indicator_type = '无' WHERE indicator_type = '体系聚合';

-- 2. 处理可能的大写情况
UPDATE pgzc_indicator SET indicator_type = 'space_recon' WHERE UPPER(indicator_type) = 'SPACE_RECON';
UPDATE pgzc_indicator SET indicator_type = 'space_domain_awareness' WHERE UPPER(indicator_type) = 'SPACE_DOMAIN_AWARENESS' OR UPPER(indicator_type) = 'SPACE_SITUATIONAL_AWARENESS' OR UPPER(indicator_type) = 'SPACE_AWARENESS' OR UPPER(indicator_type) = 'SPACE_SA';
UPDATE pgzc_indicator SET indicator_type = 'space_defense' WHERE UPPER(indicator_type) = 'SPACE_DEFENSE' OR UPPER(indicator_type) = 'SPACE_OFFENSE_DEFENSE' OR UPPER(indicator_type) = 'SPACE_AD' OR UPPER(indicator_type) = 'SPACE_COMBAT';
UPDATE pgzc_indicator SET indicator_type = 'space_track_control' WHERE UPPER(indicator_type) = 'SPACE_TRACK_CONTROL' OR UPPER(indicator_type) = 'SPACE_TTC' OR UPPER(indicator_type) = 'COMM_COUNTER';
UPDATE pgzc_indicator SET indicator_type = 'space_launch' WHERE UPPER(indicator_type) = 'SPACE_LAUNCH';
UPDATE pgzc_indicator SET indicator_type = 'sea_based_space' WHERE UPPER(indicator_type) = 'SEA_BASED_SPACE' OR UPPER(indicator_type) = 'SEA_BASED' OR UPPER(indicator_type) = 'NAV_POSITION';
UPDATE pgzc_indicator SET indicator_type = '无' WHERE UPPER(indicator_type) = 'SYSTEM_AGGREGATION';

