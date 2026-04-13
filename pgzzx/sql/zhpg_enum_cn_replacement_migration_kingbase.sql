-- ============================================================
-- ZHPG 枚举值中文化总迁移（Kingbase/PostgreSQL）
-- 目标：将“底层枚举值”从英文编码统一替换为中文值
-- 范围：结构化字段 + JSON 文本字段 + 相关字典值
-- 执行建议：整段一次执行（脚本模式）
-- ============================================================

BEGIN;

-- ------------------------------------------------------------
-- 1) 结构化字段替换（直接列值）
-- ------------------------------------------------------------

-- pgzc_indicator_system
UPDATE pgzc_indicator_system
SET system_type = CASE system_type
    WHEN 'EQUIPMENT_COMBAT_EFFECTIVENESS' THEN '装备作战效能'
    WHEN 'COMBAT_APPLICABILITY' THEN '作战适用性'
    WHEN 'SYSTEM_APPLICABILITY' THEN '体系适用性'
    WHEN 'SERVICE_APPLICABILITY' THEN '在役适用性'
    WHEN 'COMBAT_EFFECTIVENESS' THEN '装备作战效能'
    WHEN 'SYSTEM_CONTRIBUTION' THEN '体系适用性'
    WHEN 'CAPABILITY_MATURITY' THEN '在役适用性'
    ELSE system_type
END
WHERE system_type IN (
    'EQUIPMENT_COMBAT_EFFECTIVENESS', 'COMBAT_APPLICABILITY', 'SYSTEM_APPLICABILITY', 'SERVICE_APPLICABILITY',
    'COMBAT_EFFECTIVENESS', 'SYSTEM_CONTRIBUTION', 'CAPABILITY_MATURITY'
);

UPDATE pgzc_indicator_system
SET equipment_type = CASE equipment_type
    WHEN 'SPACE_RECON' THEN '航天侦察'
    WHEN 'SPACE_SITUATIONAL_AWARENESS' THEN '太空态势感知'
    WHEN 'SPACE_OFFENSE_DEFENSE' THEN '太空攻防'
    WHEN 'SPACE_TTC' THEN '航天测运控'
    WHEN 'SPACE_LAUNCH' THEN '航天发射'
    WHEN 'SEA_BASED_SPACE' THEN '海基航天'
    WHEN 'SYSTEM_AGGREGATION' THEN '聚合'
    WHEN 'SPACE_AWARENESS' THEN '太空态势感知'
    WHEN 'SPACE_AD' THEN '太空攻防'
    WHEN 'SPACE_SA' THEN '太空态势感知'
    WHEN 'SEA_BASED' THEN '海基航天'
    WHEN 'SPACE_COMBAT' THEN '太空攻防'
    WHEN 'COMM_COUNTER' THEN '航天测运控'
    WHEN 'NAV_POSITION' THEN '海基航天'
    ELSE equipment_type
END
WHERE equipment_type IN (
    'SPACE_RECON', 'SPACE_SITUATIONAL_AWARENESS', 'SPACE_OFFENSE_DEFENSE', 'SPACE_TTC', 'SPACE_LAUNCH',
    'SEA_BASED_SPACE', 'SYSTEM_AGGREGATION', 'SPACE_AWARENESS', 'SPACE_AD', 'SPACE_SA', 'SEA_BASED',
    'SPACE_COMBAT', 'COMM_COUNTER', 'NAV_POSITION'
);

UPDATE pgzc_indicator_system
SET work_mode = CASE work_mode
    WHEN 'MAIN_BRANCH_COLLAB' THEN '主分协同'
    WHEN 'INTERNAL_CIRCULATION' THEN '内部流转'
    ELSE work_mode
END
WHERE work_mode IN ('MAIN_BRANCH_COLLAB', 'INTERNAL_CIRCULATION');


-- pgzc_indicator_system_template
UPDATE pgzc_indicator_system_template
SET system_type = CASE system_type
    WHEN 'EQUIPMENT_COMBAT_EFFECTIVENESS' THEN '装备作战效能'
    WHEN 'COMBAT_APPLICABILITY' THEN '作战适用性'
    WHEN 'SYSTEM_APPLICABILITY' THEN '体系适用性'
    WHEN 'SERVICE_APPLICABILITY' THEN '在役适用性'
    WHEN 'COMBAT_EFFECTIVENESS' THEN '装备作战效能'
    WHEN 'SYSTEM_CONTRIBUTION' THEN '体系适用性'
    WHEN 'CAPABILITY_MATURITY' THEN '在役适用性'
    ELSE system_type
END
WHERE system_type IN (
    'EQUIPMENT_COMBAT_EFFECTIVENESS', 'COMBAT_APPLICABILITY', 'SYSTEM_APPLICABILITY', 'SERVICE_APPLICABILITY',
    'COMBAT_EFFECTIVENESS', 'SYSTEM_CONTRIBUTION', 'CAPABILITY_MATURITY'
);

UPDATE pgzc_indicator_system_template
SET equipment_type = CASE equipment_type
    WHEN 'SPACE_RECON' THEN '航天侦察'
    WHEN 'SPACE_SITUATIONAL_AWARENESS' THEN '太空态势感知'
    WHEN 'SPACE_OFFENSE_DEFENSE' THEN '太空攻防'
    WHEN 'SPACE_TTC' THEN '航天测运控'
    WHEN 'SPACE_LAUNCH' THEN '航天发射'
    WHEN 'SEA_BASED_SPACE' THEN '海基航天'
    WHEN 'SYSTEM_AGGREGATION' THEN '聚合'
    WHEN 'SPACE_AWARENESS' THEN '太空态势感知'
    WHEN 'SPACE_AD' THEN '太空攻防'
    WHEN 'SPACE_SA' THEN '太空态势感知'
    WHEN 'SEA_BASED' THEN '海基航天'
    WHEN 'SPACE_COMBAT' THEN '太空攻防'
    WHEN 'COMM_COUNTER' THEN '航天测运控'
    WHEN 'NAV_POSITION' THEN '海基航天'
    ELSE equipment_type
END
WHERE equipment_type IN (
    'SPACE_RECON', 'SPACE_SITUATIONAL_AWARENESS', 'SPACE_OFFENSE_DEFENSE', 'SPACE_TTC', 'SPACE_LAUNCH',
    'SEA_BASED_SPACE', 'SYSTEM_AGGREGATION', 'SPACE_AWARENESS', 'SPACE_AD', 'SPACE_SA', 'SEA_BASED',
    'SPACE_COMBAT', 'COMM_COUNTER', 'NAV_POSITION'
);

UPDATE pgzc_indicator_system_template
SET work_mode = CASE work_mode
    WHEN 'MAIN_BRANCH_COLLAB' THEN '主分协同'
    WHEN 'INTERNAL_CIRCULATION' THEN '内部流转'
    ELSE work_mode
END
WHERE work_mode IN ('MAIN_BRANCH_COLLAB', 'INTERNAL_CIRCULATION');


-- pgzc_indicator
UPDATE pgzc_indicator
SET system_type = CASE system_type
    WHEN 'EQUIPMENT_COMBAT_EFFECTIVENESS' THEN '装备作战效能'
    WHEN 'COMBAT_APPLICABILITY' THEN '作战适用性'
    WHEN 'SYSTEM_APPLICABILITY' THEN '体系适用性'
    WHEN 'SERVICE_APPLICABILITY' THEN '在役适用性'
    WHEN 'COMBAT_EFFECTIVENESS' THEN '装备作战效能'
    WHEN 'SYSTEM_CONTRIBUTION' THEN '体系适用性'
    WHEN 'CAPABILITY_MATURITY' THEN '在役适用性'
    ELSE system_type
END
WHERE system_type IN (
    'EQUIPMENT_COMBAT_EFFECTIVENESS', 'COMBAT_APPLICABILITY', 'SYSTEM_APPLICABILITY', 'SERVICE_APPLICABILITY',
    'COMBAT_EFFECTIVENESS', 'SYSTEM_CONTRIBUTION', 'CAPABILITY_MATURITY'
);

UPDATE pgzc_indicator
SET indicator_type = CASE indicator_type
    WHEN 'SPACE_RECON' THEN '航天侦察'
    WHEN 'SPACE_SITUATIONAL_AWARENESS' THEN '太空态势感知'
    WHEN 'SPACE_OFFENSE_DEFENSE' THEN '太空攻防'
    WHEN 'SPACE_TTC' THEN '航天测运控'
    WHEN 'SPACE_LAUNCH' THEN '航天发射'
    WHEN 'SEA_BASED_SPACE' THEN '海基航天'
    WHEN 'SYSTEM_AGGREGATION' THEN '聚合'
    WHEN 'SPACE_AWARENESS' THEN '太空态势感知'
    WHEN 'SPACE_AD' THEN '太空攻防'
    WHEN 'SPACE_SA' THEN '太空态势感知'
    WHEN 'SEA_BASED' THEN '海基航天'
    WHEN 'SPACE_COMBAT' THEN '太空攻防'
    WHEN 'COMM_COUNTER' THEN '航天测运控'
    WHEN 'NAV_POSITION' THEN '海基航天'
    ELSE indicator_type
END
WHERE indicator_type IN (
    'SPACE_RECON', 'SPACE_SITUATIONAL_AWARENESS', 'SPACE_OFFENSE_DEFENSE', 'SPACE_TTC', 'SPACE_LAUNCH',
    'SEA_BASED_SPACE', 'SYSTEM_AGGREGATION', 'SPACE_AWARENESS', 'SPACE_AD', 'SPACE_SA', 'SEA_BASED',
    'SPACE_COMBAT', 'COMM_COUNTER', 'NAV_POSITION'
);

UPDATE pgzc_indicator
SET work_mode = CASE work_mode
    WHEN 'MAIN_BRANCH_COLLAB' THEN '主分协同'
    WHEN 'INTERNAL_CIRCULATION' THEN '内部流转'
    ELSE work_mode
END
WHERE work_mode IN ('MAIN_BRANCH_COLLAB', 'INTERNAL_CIRCULATION');

UPDATE pgzc_indicator
SET value_category = CASE value_category
    WHEN 'COST' THEN '成本型'
    WHEN 'BENEFIT' THEN '效益型'
    WHEN 'INTERVAL_BENEFIT' THEN '区间效益型'
    ELSE value_category
END
WHERE value_category IN ('COST', 'BENEFIT', 'INTERVAL_BENEFIT');


-- pgzc_indicator_template
UPDATE pgzc_indicator_template
SET system_type = CASE system_type
    WHEN 'EQUIPMENT_COMBAT_EFFECTIVENESS' THEN '装备作战效能'
    WHEN 'COMBAT_APPLICABILITY' THEN '作战适用性'
    WHEN 'SYSTEM_APPLICABILITY' THEN '体系适用性'
    WHEN 'SERVICE_APPLICABILITY' THEN '在役适用性'
    WHEN 'COMBAT_EFFECTIVENESS' THEN '装备作战效能'
    WHEN 'SYSTEM_CONTRIBUTION' THEN '体系适用性'
    WHEN 'CAPABILITY_MATURITY' THEN '在役适用性'
    ELSE system_type
END
WHERE system_type IN (
    'EQUIPMENT_COMBAT_EFFECTIVENESS', 'COMBAT_APPLICABILITY', 'SYSTEM_APPLICABILITY', 'SERVICE_APPLICABILITY',
    'COMBAT_EFFECTIVENESS', 'SYSTEM_CONTRIBUTION', 'CAPABILITY_MATURITY'
);

UPDATE pgzc_indicator_template
SET equipment_type = CASE equipment_type
    WHEN 'SPACE_RECON' THEN '航天侦察'
    WHEN 'SPACE_SITUATIONAL_AWARENESS' THEN '太空态势感知'
    WHEN 'SPACE_OFFENSE_DEFENSE' THEN '太空攻防'
    WHEN 'SPACE_TTC' THEN '航天测运控'
    WHEN 'SPACE_LAUNCH' THEN '航天发射'
    WHEN 'SEA_BASED_SPACE' THEN '海基航天'
    WHEN 'SYSTEM_AGGREGATION' THEN '聚合'
    WHEN 'SPACE_AWARENESS' THEN '太空态势感知'
    WHEN 'SPACE_AD' THEN '太空攻防'
    WHEN 'SPACE_SA' THEN '太空态势感知'
    WHEN 'SEA_BASED' THEN '海基航天'
    WHEN 'SPACE_COMBAT' THEN '太空攻防'
    WHEN 'COMM_COUNTER' THEN '航天测运控'
    WHEN 'NAV_POSITION' THEN '海基航天'
    ELSE equipment_type
END
WHERE equipment_type IN (
    'SPACE_RECON', 'SPACE_SITUATIONAL_AWARENESS', 'SPACE_OFFENSE_DEFENSE', 'SPACE_TTC', 'SPACE_LAUNCH',
    'SEA_BASED_SPACE', 'SYSTEM_AGGREGATION', 'SPACE_AWARENESS', 'SPACE_AD', 'SPACE_SA', 'SEA_BASED',
    'SPACE_COMBAT', 'COMM_COUNTER', 'NAV_POSITION'
);

UPDATE pgzc_indicator_template
SET work_mode = CASE work_mode
    WHEN 'MAIN_BRANCH_COLLAB' THEN '主分协同'
    WHEN 'INTERNAL_CIRCULATION' THEN '内部流转'
    ELSE work_mode
END
WHERE work_mode IN ('MAIN_BRANCH_COLLAB', 'INTERNAL_CIRCULATION');


-- pgzc_calc_flow_template / pgzc_algorithm（如存在装备类型编码）
UPDATE pgzc_calc_flow_template
SET equipment_type = CASE equipment_type
    WHEN 'SPACE_RECON' THEN '航天侦察'
    WHEN 'SPACE_SITUATIONAL_AWARENESS' THEN '太空态势感知'
    WHEN 'SPACE_OFFENSE_DEFENSE' THEN '太空攻防'
    WHEN 'SPACE_TTC' THEN '航天测运控'
    WHEN 'SPACE_LAUNCH' THEN '航天发射'
    WHEN 'SEA_BASED_SPACE' THEN '海基航天'
    WHEN 'SYSTEM_AGGREGATION' THEN '聚合'
    WHEN 'SPACE_AWARENESS' THEN '太空态势感知'
    WHEN 'SPACE_AD' THEN '太空攻防'
    WHEN 'SPACE_SA' THEN '太空态势感知'
    WHEN 'SEA_BASED' THEN '海基航天'
    WHEN 'SPACE_COMBAT' THEN '太空攻防'
    WHEN 'COMM_COUNTER' THEN '航天测运控'
    WHEN 'NAV_POSITION' THEN '海基航天'
    ELSE equipment_type
END
WHERE equipment_type IN (
    'SPACE_RECON', 'SPACE_SITUATIONAL_AWARENESS', 'SPACE_OFFENSE_DEFENSE', 'SPACE_TTC', 'SPACE_LAUNCH',
    'SEA_BASED_SPACE', 'SYSTEM_AGGREGATION', 'SPACE_AWARENESS', 'SPACE_AD', 'SPACE_SA', 'SEA_BASED',
    'SPACE_COMBAT', 'COMM_COUNTER', 'NAV_POSITION'
);

UPDATE pgzc_algorithm
SET equipment_type = CASE equipment_type
    WHEN 'SPACE_RECON' THEN '航天侦察'
    WHEN 'SPACE_SITUATIONAL_AWARENESS' THEN '太空态势感知'
    WHEN 'SPACE_OFFENSE_DEFENSE' THEN '太空攻防'
    WHEN 'SPACE_TTC' THEN '航天测运控'
    WHEN 'SPACE_LAUNCH' THEN '航天发射'
    WHEN 'SEA_BASED_SPACE' THEN '海基航天'
    WHEN 'SYSTEM_AGGREGATION' THEN '聚合'
    WHEN 'SPACE_AWARENESS' THEN '太空态势感知'
    WHEN 'SPACE_AD' THEN '太空攻防'
    WHEN 'SPACE_SA' THEN '太空态势感知'
    WHEN 'SEA_BASED' THEN '海基航天'
    WHEN 'SPACE_COMBAT' THEN '太空攻防'
    WHEN 'COMM_COUNTER' THEN '航天测运控'
    WHEN 'NAV_POSITION' THEN '海基航天'
    ELSE equipment_type
END
WHERE equipment_type IN (
    'SPACE_RECON', 'SPACE_SITUATIONAL_AWARENESS', 'SPACE_OFFENSE_DEFENSE', 'SPACE_TTC', 'SPACE_LAUNCH',
    'SEA_BASED_SPACE', 'SYSTEM_AGGREGATION', 'SPACE_AWARENESS', 'SPACE_AD', 'SPACE_SA', 'SEA_BASED',
    'SPACE_COMBAT', 'COMM_COUNTER', 'NAV_POSITION'
);


-- ------------------------------------------------------------
-- 2) JSON / 文本字段替换（indicator_tree / config_json / calc_method 等）
-- ------------------------------------------------------------

-- 通用替换链：只替换值，不改字段名
-- 为保持 SQL 可读性，直接在每个 UPDATE 中内联 REPLACE。

UPDATE pgzc_indicator_system
SET indicator_tree = NULLIF(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(indicator_tree, ''),
    'EQUIPMENT_COMBAT_EFFECTIVENESS', '装备作战效能'),
    'COMBAT_APPLICABILITY', '作战适用性'),
    'SYSTEM_APPLICABILITY', '体系适用性'),
    'SERVICE_APPLICABILITY', '在役适用性'),
    'COMBAT_EFFECTIVENESS', '装备作战效能'),
    'SYSTEM_CONTRIBUTION', '体系适用性'),
    'CAPABILITY_MATURITY', '在役适用性'),
    'MAIN_BRANCH_COLLAB', '主分协同'),
    'INTERNAL_CIRCULATION', '内部流转'),
    'SPACE_SITUATIONAL_AWARENESS', '太空态势感知'),
    'SPACE_OFFENSE_DEFENSE', '太空攻防'),
    'SPACE_RECON', '航天侦察'),
    'SPACE_TTC', '航天测运控'),
    'SPACE_LAUNCH', '航天发射'),
    'SEA_BASED_SPACE', '海基航天'),
    'SYSTEM_AGGREGATION', '聚合'),
    'SPACE_AWARENESS', '太空态势感知'),
    'SPACE_AD', '太空攻防'),
    'SPACE_SA', '太空态势感知'),
    'SEA_BASED', '海基航天'),
    'SPACE_COMBAT', '太空攻防'),
    'COMM_COUNTER', '航天测运控'),
    'NAV_POSITION', '海基航天'),
    'COST', '成本型'),
    'INTERVAL_BENEFIT', '区间效益型'),
    'BENEFIT', '效益型'),
    '体系聚合', '聚合'
), '')
WHERE indicator_tree IS NOT NULL
  AND (
      indicator_tree LIKE '%MAIN_BRANCH_COLLAB%' OR indicator_tree LIKE '%INTERNAL_CIRCULATION%' OR
      indicator_tree LIKE '%SPACE_%' OR indicator_tree LIKE '%SEA_BASED%' OR
      indicator_tree LIKE '%SYSTEM_AGGREGATION%' OR indicator_tree LIKE '%EQUIPMENT_COMBAT_EFFECTIVENESS%' OR
      indicator_tree LIKE '%COMBAT_APPLICABILITY%' OR indicator_tree LIKE '%SYSTEM_APPLICABILITY%' OR
      indicator_tree LIKE '%SERVICE_APPLICABILITY%' OR indicator_tree LIKE '%COST%' OR
      indicator_tree LIKE '%BENEFIT%' OR indicator_tree LIKE '%INTERVAL_BENEFIT%' OR
      indicator_tree LIKE '%体系聚合%'
  );

UPDATE pgzc_indicator_system
SET indicator_tree_weight = NULLIF(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(indicator_tree_weight, ''),
    'EQUIPMENT_COMBAT_EFFECTIVENESS', '装备作战效能'),
    'COMBAT_APPLICABILITY', '作战适用性'),
    'SYSTEM_APPLICABILITY', '体系适用性'),
    'SERVICE_APPLICABILITY', '在役适用性'),
    'COMBAT_EFFECTIVENESS', '装备作战效能'),
    'SYSTEM_CONTRIBUTION', '体系适用性'),
    'CAPABILITY_MATURITY', '在役适用性'),
    'MAIN_BRANCH_COLLAB', '主分协同'),
    'INTERNAL_CIRCULATION', '内部流转'),
    'SPACE_SITUATIONAL_AWARENESS', '太空态势感知'),
    'SPACE_OFFENSE_DEFENSE', '太空攻防'),
    'SPACE_RECON', '航天侦察'),
    'SPACE_TTC', '航天测运控'),
    'SPACE_LAUNCH', '航天发射'),
    'SEA_BASED_SPACE', '海基航天'),
    'SYSTEM_AGGREGATION', '聚合'),
    'SPACE_AWARENESS', '太空态势感知'),
    'SPACE_AD', '太空攻防'),
    'SPACE_SA', '太空态势感知'),
    'SEA_BASED', '海基航天'),
    'SPACE_COMBAT', '太空攻防'),
    'COMM_COUNTER', '航天测运控'),
    'NAV_POSITION', '海基航天'),
    'COST', '成本型'),
    'INTERVAL_BENEFIT', '区间效益型'),
    'BENEFIT', '效益型'),
    '体系聚合', '聚合'
), '')
WHERE indicator_tree_weight IS NOT NULL
  AND (
      indicator_tree_weight LIKE '%MAIN_BRANCH_COLLAB%' OR indicator_tree_weight LIKE '%INTERNAL_CIRCULATION%' OR
      indicator_tree_weight LIKE '%SPACE_%' OR indicator_tree_weight LIKE '%SEA_BASED%' OR
      indicator_tree_weight LIKE '%SYSTEM_AGGREGATION%' OR indicator_tree_weight LIKE '%EQUIPMENT_COMBAT_EFFECTIVENESS%' OR
      indicator_tree_weight LIKE '%COMBAT_APPLICABILITY%' OR indicator_tree_weight LIKE '%SYSTEM_APPLICABILITY%' OR
      indicator_tree_weight LIKE '%SERVICE_APPLICABILITY%' OR indicator_tree_weight LIKE '%COST%' OR
      indicator_tree_weight LIKE '%BENEFIT%' OR indicator_tree_weight LIKE '%INTERVAL_BENEFIT%' OR
      indicator_tree_weight LIKE '%体系聚合%'
  );

UPDATE pgzc_indicator_system
SET initial_tree = NULLIF(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(initial_tree, ''),
    'EQUIPMENT_COMBAT_EFFECTIVENESS', '装备作战效能'),
    'COMBAT_APPLICABILITY', '作战适用性'),
    'SYSTEM_APPLICABILITY', '体系适用性'),
    'SERVICE_APPLICABILITY', '在役适用性'),
    'COMBAT_EFFECTIVENESS', '装备作战效能'),
    'SYSTEM_CONTRIBUTION', '体系适用性'),
    'CAPABILITY_MATURITY', '在役适用性'),
    'MAIN_BRANCH_COLLAB', '主分协同'),
    'INTERNAL_CIRCULATION', '内部流转'),
    'SPACE_SITUATIONAL_AWARENESS', '太空态势感知'),
    'SPACE_OFFENSE_DEFENSE', '太空攻防'),
    'SPACE_RECON', '航天侦察'),
    'SPACE_TTC', '航天测运控'),
    'SPACE_LAUNCH', '航天发射'),
    'SEA_BASED_SPACE', '海基航天'),
    'SYSTEM_AGGREGATION', '聚合'),
    'SPACE_AWARENESS', '太空态势感知'),
    'SPACE_AD', '太空攻防'),
    'SPACE_SA', '太空态势感知'),
    'SEA_BASED', '海基航天'),
    'SPACE_COMBAT', '太空攻防'),
    'COMM_COUNTER', '航天测运控'),
    'NAV_POSITION', '海基航天'),
    'COST', '成本型'),
    'INTERVAL_BENEFIT', '区间效益型'),
    'BENEFIT', '效益型'),
    '体系聚合', '聚合'
), '')
WHERE initial_tree IS NOT NULL
  AND (
      initial_tree LIKE '%MAIN_BRANCH_COLLAB%' OR initial_tree LIKE '%INTERNAL_CIRCULATION%' OR
      initial_tree LIKE '%SPACE_%' OR initial_tree LIKE '%SEA_BASED%' OR
      initial_tree LIKE '%SYSTEM_AGGREGATION%' OR initial_tree LIKE '%EQUIPMENT_COMBAT_EFFECTIVENESS%' OR
      initial_tree LIKE '%COMBAT_APPLICABILITY%' OR initial_tree LIKE '%SYSTEM_APPLICABILITY%' OR
      initial_tree LIKE '%SERVICE_APPLICABILITY%' OR initial_tree LIKE '%COST%' OR
      initial_tree LIKE '%BENEFIT%' OR initial_tree LIKE '%INTERVAL_BENEFIT%' OR
      initial_tree LIKE '%体系聚合%'
  );

UPDATE pgzc_indicator
SET calc_method = NULLIF(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(calc_method, ''),
    'EQUIPMENT_COMBAT_EFFECTIVENESS', '装备作战效能'),
    'COMBAT_APPLICABILITY', '作战适用性'),
    'SYSTEM_APPLICABILITY', '体系适用性'),
    'SERVICE_APPLICABILITY', '在役适用性'),
    'COMBAT_EFFECTIVENESS', '装备作战效能'),
    'SYSTEM_CONTRIBUTION', '体系适用性'),
    'CAPABILITY_MATURITY', '在役适用性'),
    'MAIN_BRANCH_COLLAB', '主分协同'),
    'INTERNAL_CIRCULATION', '内部流转'),
    'SPACE_SITUATIONAL_AWARENESS', '太空态势感知'),
    'SPACE_OFFENSE_DEFENSE', '太空攻防'),
    'SPACE_RECON', '航天侦察'),
    'SPACE_TTC', '航天测运控'),
    'SPACE_LAUNCH', '航天发射'),
    'SEA_BASED_SPACE', '海基航天'),
    'SYSTEM_AGGREGATION', '聚合'),
    'SPACE_AWARENESS', '太空态势感知'),
    'SPACE_AD', '太空攻防'),
    'SPACE_SA', '太空态势感知'),
    'SEA_BASED', '海基航天'),
    'SPACE_COMBAT', '太空攻防'),
    'COMM_COUNTER', '航天测运控'),
    'NAV_POSITION', '海基航天'),
    'COST', '成本型'),
    'INTERVAL_BENEFIT', '区间效益型'),
    'BENEFIT', '效益型'),
    '体系聚合', '聚合'
), '')
WHERE calc_method IS NOT NULL
  AND (
      calc_method LIKE '%MAIN_BRANCH_COLLAB%' OR calc_method LIKE '%INTERNAL_CIRCULATION%' OR
      calc_method LIKE '%SPACE_%' OR calc_method LIKE '%SEA_BASED%' OR
      calc_method LIKE '%SYSTEM_AGGREGATION%' OR calc_method LIKE '%EQUIPMENT_COMBAT_EFFECTIVENESS%' OR
      calc_method LIKE '%COMBAT_APPLICABILITY%' OR calc_method LIKE '%SYSTEM_APPLICABILITY%' OR
      calc_method LIKE '%SERVICE_APPLICABILITY%' OR calc_method LIKE '%COST%' OR
      calc_method LIKE '%BENEFIT%' OR calc_method LIKE '%INTERVAL_BENEFIT%' OR
      calc_method LIKE '%体系聚合%'
  );

UPDATE pgzc_indicator_system_template
SET indicator_tree = NULLIF(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(indicator_tree, ''),
    'EQUIPMENT_COMBAT_EFFECTIVENESS', '装备作战效能'),
    'COMBAT_APPLICABILITY', '作战适用性'),
    'SYSTEM_APPLICABILITY', '体系适用性'),
    'SERVICE_APPLICABILITY', '在役适用性'),
    'COMBAT_EFFECTIVENESS', '装备作战效能'),
    'SYSTEM_CONTRIBUTION', '体系适用性'),
    'CAPABILITY_MATURITY', '在役适用性'),
    'MAIN_BRANCH_COLLAB', '主分协同'),
    'INTERNAL_CIRCULATION', '内部流转'),
    'SPACE_SITUATIONAL_AWARENESS', '太空态势感知'),
    'SPACE_OFFENSE_DEFENSE', '太空攻防'),
    'SPACE_RECON', '航天侦察'),
    'SPACE_TTC', '航天测运控'),
    'SPACE_LAUNCH', '航天发射'),
    'SEA_BASED_SPACE', '海基航天'),
    'SYSTEM_AGGREGATION', '聚合'),
    'SPACE_AWARENESS', '太空态势感知'),
    'SPACE_AD', '太空攻防'),
    'SPACE_SA', '太空态势感知'),
    'SEA_BASED', '海基航天'),
    'SPACE_COMBAT', '太空攻防'),
    'COMM_COUNTER', '航天测运控'),
    'NAV_POSITION', '海基航天'),
    'COST', '成本型'),
    'INTERVAL_BENEFIT', '区间效益型'),
    'BENEFIT', '效益型'),
    '体系聚合', '聚合'
), '')
WHERE indicator_tree IS NOT NULL
  AND (
      indicator_tree LIKE '%MAIN_BRANCH_COLLAB%' OR indicator_tree LIKE '%INTERNAL_CIRCULATION%' OR
      indicator_tree LIKE '%SPACE_%' OR indicator_tree LIKE '%SEA_BASED%' OR
      indicator_tree LIKE '%SYSTEM_AGGREGATION%' OR indicator_tree LIKE '%EQUIPMENT_COMBAT_EFFECTIVENESS%' OR
      indicator_tree LIKE '%COMBAT_APPLICABILITY%' OR indicator_tree LIKE '%SYSTEM_APPLICABILITY%' OR
      indicator_tree LIKE '%SERVICE_APPLICABILITY%' OR indicator_tree LIKE '%COST%' OR
      indicator_tree LIKE '%BENEFIT%' OR indicator_tree LIKE '%INTERVAL_BENEFIT%' OR
      indicator_tree LIKE '%体系聚合%'
  );

UPDATE pgzc_indicator_system_template
SET config_json = NULLIF(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(config_json, ''),
    'EQUIPMENT_COMBAT_EFFECTIVENESS', '装备作战效能'),
    'COMBAT_APPLICABILITY', '作战适用性'),
    'SYSTEM_APPLICABILITY', '体系适用性'),
    'SERVICE_APPLICABILITY', '在役适用性'),
    'COMBAT_EFFECTIVENESS', '装备作战效能'),
    'SYSTEM_CONTRIBUTION', '体系适用性'),
    'CAPABILITY_MATURITY', '在役适用性'),
    'MAIN_BRANCH_COLLAB', '主分协同'),
    'INTERNAL_CIRCULATION', '内部流转'),
    'SPACE_SITUATIONAL_AWARENESS', '太空态势感知'),
    'SPACE_OFFENSE_DEFENSE', '太空攻防'),
    'SPACE_RECON', '航天侦察'),
    'SPACE_TTC', '航天测运控'),
    'SPACE_LAUNCH', '航天发射'),
    'SEA_BASED_SPACE', '海基航天'),
    'SYSTEM_AGGREGATION', '聚合'),
    'SPACE_AWARENESS', '太空态势感知'),
    'SPACE_AD', '太空攻防'),
    'SPACE_SA', '太空态势感知'),
    'SEA_BASED', '海基航天'),
    'SPACE_COMBAT', '太空攻防'),
    'COMM_COUNTER', '航天测运控'),
    'NAV_POSITION', '海基航天'),
    'COST', '成本型'),
    'INTERVAL_BENEFIT', '区间效益型'),
    'BENEFIT', '效益型'),
    '体系聚合', '聚合'
), '')
WHERE config_json IS NOT NULL
  AND (
      config_json LIKE '%MAIN_BRANCH_COLLAB%' OR config_json LIKE '%INTERNAL_CIRCULATION%' OR
      config_json LIKE '%SPACE_%' OR config_json LIKE '%SEA_BASED%' OR
      config_json LIKE '%SYSTEM_AGGREGATION%' OR config_json LIKE '%EQUIPMENT_COMBAT_EFFECTIVENESS%' OR
      config_json LIKE '%COMBAT_APPLICABILITY%' OR config_json LIKE '%SYSTEM_APPLICABILITY%' OR
      config_json LIKE '%SERVICE_APPLICABILITY%' OR config_json LIKE '%COST%' OR
      config_json LIKE '%BENEFIT%' OR config_json LIKE '%INTERVAL_BENEFIT%' OR
      config_json LIKE '%体系聚合%'
  );

UPDATE pgzc_indicator_template
SET config_json = NULLIF(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(config_json, ''),
    'EQUIPMENT_COMBAT_EFFECTIVENESS', '装备作战效能'),
    'COMBAT_APPLICABILITY', '作战适用性'),
    'SYSTEM_APPLICABILITY', '体系适用性'),
    'SERVICE_APPLICABILITY', '在役适用性'),
    'COMBAT_EFFECTIVENESS', '装备作战效能'),
    'SYSTEM_CONTRIBUTION', '体系适用性'),
    'CAPABILITY_MATURITY', '在役适用性'),
    'MAIN_BRANCH_COLLAB', '主分协同'),
    'INTERNAL_CIRCULATION', '内部流转'),
    'SPACE_SITUATIONAL_AWARENESS', '太空态势感知'),
    'SPACE_OFFENSE_DEFENSE', '太空攻防'),
    'SPACE_RECON', '航天侦察'),
    'SPACE_TTC', '航天测运控'),
    'SPACE_LAUNCH', '航天发射'),
    'SEA_BASED_SPACE', '海基航天'),
    'SYSTEM_AGGREGATION', '聚合'),
    'SPACE_AWARENESS', '太空态势感知'),
    'SPACE_AD', '太空攻防'),
    'SPACE_SA', '太空态势感知'),
    'SEA_BASED', '海基航天'),
    'SPACE_COMBAT', '太空攻防'),
    'COMM_COUNTER', '航天测运控'),
    'NAV_POSITION', '海基航天'),
    'COST', '成本型'),
    'INTERVAL_BENEFIT', '区间效益型'),
    'BENEFIT', '效益型'),
    '体系聚合', '聚合'
), '')
WHERE config_json IS NOT NULL
  AND (
      config_json LIKE '%MAIN_BRANCH_COLLAB%' OR config_json LIKE '%INTERNAL_CIRCULATION%' OR
      config_json LIKE '%SPACE_%' OR config_json LIKE '%SEA_BASED%' OR
      config_json LIKE '%SYSTEM_AGGREGATION%' OR config_json LIKE '%EQUIPMENT_COMBAT_EFFECTIVENESS%' OR
      config_json LIKE '%COMBAT_APPLICABILITY%' OR config_json LIKE '%SYSTEM_APPLICABILITY%' OR
      config_json LIKE '%SERVICE_APPLICABILITY%' OR config_json LIKE '%COST%' OR
      config_json LIKE '%BENEFIT%' OR config_json LIKE '%INTERVAL_BENEFIT%' OR
      config_json LIKE '%体系聚合%'
  );

UPDATE pgzc_calc_flow_template
SET config_json = NULLIF(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(config_json, ''),
    'EQUIPMENT_COMBAT_EFFECTIVENESS', '装备作战效能'),
    'COMBAT_APPLICABILITY', '作战适用性'),
    'SYSTEM_APPLICABILITY', '体系适用性'),
    'SERVICE_APPLICABILITY', '在役适用性'),
    'COMBAT_EFFECTIVENESS', '装备作战效能'),
    'SYSTEM_CONTRIBUTION', '体系适用性'),
    'CAPABILITY_MATURITY', '在役适用性'),
    'MAIN_BRANCH_COLLAB', '主分协同'),
    'INTERNAL_CIRCULATION', '内部流转'),
    'SPACE_SITUATIONAL_AWARENESS', '太空态势感知'),
    'SPACE_OFFENSE_DEFENSE', '太空攻防'),
    'SPACE_RECON', '航天侦察'),
    'SPACE_TTC', '航天测运控'),
    'SPACE_LAUNCH', '航天发射'),
    'SEA_BASED_SPACE', '海基航天'),
    'SYSTEM_AGGREGATION', '聚合'),
    'SPACE_AWARENESS', '太空态势感知'),
    'SPACE_AD', '太空攻防'),
    'SPACE_SA', '太空态势感知'),
    'SEA_BASED', '海基航天'),
    'SPACE_COMBAT', '太空攻防'),
    'COMM_COUNTER', '航天测运控'),
    'NAV_POSITION', '海基航天'),
    'COST', '成本型'),
    'INTERVAL_BENEFIT', '区间效益型'),
    'BENEFIT', '效益型'),
    '体系聚合', '聚合'
), '')
WHERE config_json IS NOT NULL
  AND (
      config_json LIKE '%MAIN_BRANCH_COLLAB%' OR config_json LIKE '%INTERNAL_CIRCULATION%' OR
      config_json LIKE '%SPACE_%' OR config_json LIKE '%SEA_BASED%' OR
      config_json LIKE '%SYSTEM_AGGREGATION%' OR config_json LIKE '%EQUIPMENT_COMBAT_EFFECTIVENESS%' OR
      config_json LIKE '%COMBAT_APPLICABILITY%' OR config_json LIKE '%SYSTEM_APPLICABILITY%' OR
      config_json LIKE '%SERVICE_APPLICABILITY%' OR config_json LIKE '%COST%' OR
      config_json LIKE '%BENEFIT%' OR config_json LIKE '%INTERVAL_BENEFIT%' OR
      config_json LIKE '%体系聚合%'
  );


-- ------------------------------------------------------------
-- 3) 列注释（可选，统一中文说明）
-- ------------------------------------------------------------
COMMENT ON COLUMN public.pgzc_indicator.work_mode IS '工作模式：主分协同 / 内部流转';
COMMENT ON COLUMN public.pgzc_indicator_system.work_mode IS '工作模式：主分协同 / 内部流转';
COMMENT ON COLUMN public.pgzc_indicator_template.work_mode IS '工作模式：主分协同 / 内部流转';
COMMENT ON COLUMN public.pgzc_indicator_system_template.work_mode IS '工作模式：主分协同 / 内部流转';
COMMENT ON COLUMN public.pgzc_indicator.value_category IS '指标值类型：成本型、效益型、区间效益型';
COMMENT ON COLUMN public.pgzc_indicator.system_type IS '指标集类型/一能三性：装备作战效能、作战适用性、体系适用性、在役适用性';
COMMENT ON COLUMN public.pgzc_indicator_system.system_type IS '指标集类型/一能三性：装备作战效能、作战适用性、体系适用性、在役适用性';
COMMENT ON COLUMN public.pgzc_indicator_system_template.system_type IS '指标集类型/一能三性：装备作战效能、作战适用性、体系适用性、在役适用性';

COMMIT;

-- ------------------------------------------------------------
-- 4) 执行后核对（手工执行）
-- ------------------------------------------------------------
-- SELECT system_type, COUNT(*) FROM pgzc_indicator_system GROUP BY system_type ORDER BY 1;
-- SELECT equipment_type, COUNT(*) FROM pgzc_indicator_system GROUP BY equipment_type ORDER BY 1;
-- SELECT work_mode, COUNT(*) FROM pgzc_indicator_system GROUP BY work_mode ORDER BY 1;
-- SELECT indicator_type, COUNT(*) FROM pgzc_indicator GROUP BY indicator_type ORDER BY 1;
-- SELECT value_category, COUNT(*) FROM pgzc_indicator GROUP BY value_category ORDER BY 1;

