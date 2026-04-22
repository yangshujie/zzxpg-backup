-- =============================================
-- 综合分析计算流程模板 - 移除模板配置中的权重计算阶段
-- 在业务库（pgzc_* 表所在库）中执行
-- 数据库：KingbaseES / PostgreSQL
-- 背景：流程模板已与指标体系解耦，设计态模板不再存 config_json.stages.weightCalc。
-- =============================================

-- 执行前可先查看受影响模板
SELECT id, template_code, template_name, status
FROM pgzc_calc_flow_template
WHERE config_json IS NOT NULL
  AND config_json LIKE '%"weightCalc"%';

-- 清理历史模板 JSON 中的 stages.weightCalc，并把剩余阶段顺序收敛为三阶段
UPDATE pgzc_calc_flow_template
SET config_json = jsonb_set(
        jsonb_set(
            config_json::jsonb #- '{stages,weightCalc}',
            '{stages,comprehensiveCalc,stageOrder}',
            '2'::jsonb,
            false
        ),
        '{stages,reportOutput,stageOrder}',
        '3'::jsonb,
        false
    )::text,
    update_by = 'migration',
    update_time = NOW()
WHERE config_json IS NOT NULL
  AND config_json LIKE '%"weightCalc"%';

COMMENT ON COLUMN pgzc_calc_flow_template.config_json
    IS '模板配置JSON(三阶段) - 调度策略、综合分析计算、报告输出；权重与节点计算规则由执行时传入的指标体系维护';
