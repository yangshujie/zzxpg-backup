-- =============================================================================
-- 指标体系导入：indicator_system_single_bottom_metric.json
-- 仅 1 个底层叶指标（时间占空比）；根下直接挂叶
-- 执行前请确认已执行 zhpg_indicator_system_schema_latest_kingbase.sql
-- schema 可按需将 public 改为实际 schema
-- =============================================================================

DELETE FROM public.pgzc_indicator_system WHERE system_name = '单底层指标测试体系（单叶示例）';

INSERT INTO public.pgzc_indicator_system (
    system_name,
    work_mode,
    indicator_tree,
    indicator_tree_weight,
    weight_assign_algorithm,
    weight_assign_params,
    conduction_config,
    template_id,
    description,
    status,
    is_applied,
    build_phase,
    source_subsystem,
    refined_time,
    del_flag,
    create_by,
    create_time,
    update_by,
    update_time,
    remark
) VALUES (
    '单底层指标测试体系（单叶示例）',
    '主分协同',
    '{"treeData":{"id":"0_1775999000001","label":"单底层指标测试体系","indicatorType":"太空攻防","workMode":"主分协同","children":[{"id":"1_1775999000001","label":"时间占空比","indicatorType":"太空攻防","unit":"%","type":"效益型","valueMin":0,"valueMax":100,"description":"干扰期间中断时长占总干扰持续时长的百分比（单指标导入示例）","computeRule":{"method":{"node":[{"type":"start","source":"太空攻防","value":"jammer_actions","id":"1_1775999000001_a1","name":"干扰动作时间戳","description":"干扰装备执行干扰动作的时间戳（干扰开始/停止记录），用于确定总干扰持续时段","taskStage":"干扰执行阶段","taskTimeStart":"2026-03-20 09:00:00","taskTimeEnd":"2026-03-20 10:40:00","experimentalTask":"通信对抗试验","fields":["timestamp"]},{"type":"start","source":"太空攻防","value":"link_truth_series","id":"2_1775999000001_a2","name":"链路在线状态 / 链路状态时间戳","description":"靶星地面系统通信链路在线/中断状态序列（1=在线，0=中断），用于计算干扰期间中断时长","taskStage":"干扰执行阶段","taskTimeStart":"2026-03-20 09:00:00","taskTimeEnd":"2026-03-20 10:40:00","experimentalTask":"通信对抗试验","fields":["link_online","timestamp"]},{"type":"algo","value":306,"id":"3_1775999000001_a3","config":[{"name":"metric_id","field":"metric_id","type":"","description":"","defaultValue":"time_occupancy_ratio"},{"name":"formula","field":"formula","type":"","description":"","defaultValue":"干扰期间中断时长 / 总干扰持续时长 × 100%"}],"url":"algs/character/calMean.zip","baseFlag":0,"algoType":"属性值计算方法","subtype":""},{"type":"algo","value":227,"id":"4_1775999000001_a4","config":[{"name":"config","field":"","type":"","description":"","defaultValue":""}],"url":"algs/norm/sigmoid.zip","baseFlag":1,"algoType":"指标量化算法","subtype":""},{"type":"result","value":"","id":"5_1775999000001_a5"}],"lineList":[{"sourceId":"2_1775999000001_a2","targetId":"3_1775999000001_a3"},{"sourceId":"1_1775999000001_a1","targetId":"3_1775999000001_a3"},{"sourceId":"3_1775999000001_a3","targetId":"4_1775999000001_a4"},{"sourceId":"4_1775999000001_a4","targetId":"5_1775999000001_a5"}]}}}]}}',
    NULL,
    'EQUAL',
    NULL,
    '{"method":"SERIES","params":{}}',
    NULL,
    '由 indicator_system_single_bottom_metric.json 生成；单底层指标 SQL 入库示例。',
    'DRAFT',
    0,
    'REFINED',
    NULL,
    NULL,
    '0',
    'admin',
    NOW(),
    'admin',
    NOW(),
    NULL
);

-- 若库表已含 id_code 且需与树根 id 一致，可执行：
-- UPDATE public.pgzc_indicator_system SET id_code = '0_1775999000001' WHERE system_name = '单底层指标测试体系（单叶示例）';
