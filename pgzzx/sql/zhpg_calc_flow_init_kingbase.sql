-- =============================================
-- 综合分析计算流程模板(ZHFX) - 业务表初始化脚本
-- 在业务库（pgzc_* 表所在库）中执行
-- 数据库：KingbaseES / PostgreSQL
-- =============================================

-- ----------------------------
-- 1. 评估计算流程模板表
-- ----------------------------
CREATE TABLE IF NOT EXISTS pgzc_calc_flow_template (
    id                      BIGSERIAL       PRIMARY KEY,
    template_code           VARCHAR(50)     NOT NULL,
    template_name           VARCHAR(200)    NOT NULL,
    task_type               VARCHAR(50)     NOT NULL,
    equipment_type          VARCHAR(50)     NOT NULL,
    calc_granularity        VARCHAR(50)     NOT NULL,
    indicator_system_id     BIGINT          DEFAULT NULL,
    indicator_system_name   VARCHAR(200)    DEFAULT NULL,
    data_plan_id            BIGINT          DEFAULT NULL,
    data_plan_name          VARCHAR(200)    DEFAULT NULL,
    config_json             TEXT            NOT NULL,
    version_no              VARCHAR(20)     NOT NULL DEFAULT 'V1.0',
    status                  VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    description             VARCHAR(500)    DEFAULT NULL,
    create_by               VARCHAR(64)     DEFAULT '',
    create_time             TIMESTAMP       DEFAULT NULL,
    update_by               VARCHAR(64)     DEFAULT '',
    update_time             TIMESTAMP       DEFAULT NULL,
    remark                  VARCHAR(500)    DEFAULT NULL,
    del_flag                CHAR(1)         NOT NULL DEFAULT '0'
);
COMMENT ON TABLE  pgzc_calc_flow_template IS '评估计算流程模板表';
COMMENT ON COLUMN pgzc_calc_flow_template.id IS '模板ID';
COMMENT ON COLUMN pgzc_calc_flow_template.template_code IS '模板编号';
COMMENT ON COLUMN pgzc_calc_flow_template.template_name IS '模板名称';
COMMENT ON COLUMN pgzc_calc_flow_template.task_type IS '评估任务类型(COMBAT_TEST/SYSTEM_TASK/INSERVICE_ASSESS/PERFORMANCE_TEST)';
COMMENT ON COLUMN pgzc_calc_flow_template.equipment_type IS '装备类型';
COMMENT ON COLUMN pgzc_calc_flow_template.calc_granularity IS '评估粒度(EQUIP_EFFECTIVENESS/SYSTEM_TASK/PERFORMANCE)';
COMMENT ON COLUMN pgzc_calc_flow_template.indicator_system_id IS '关联指标体系ID（可选；未绑定时由计算执行请求传入 indicatorSystemId）';
COMMENT ON COLUMN pgzc_calc_flow_template.indicator_system_name IS '关联指标体系名称';
COMMENT ON COLUMN pgzc_calc_flow_template.data_plan_id IS '关联数据方案ID';
COMMENT ON COLUMN pgzc_calc_flow_template.data_plan_name IS '关联数据方案名称';
COMMENT ON COLUMN pgzc_calc_flow_template.config_json IS '模板配置JSON(四阶段) - 权重计算阶段:运行时执行策略(快照/校验/缺失处理/精度); 综合分析阶段:计算编排(聚合/合并/空数据处理); 算法定义和权重值由指标体系/SFMX维护';
COMMENT ON COLUMN pgzc_calc_flow_template.version_no IS '版本号';
COMMENT ON COLUMN pgzc_calc_flow_template.status IS '状态(DRAFT草稿/TESTING测试中/PUBLISHED已启用/DISABLED已停用)';
COMMENT ON COLUMN pgzc_calc_flow_template.description IS '模板说明';
COMMENT ON COLUMN pgzc_calc_flow_template.del_flag IS '删除标志(0存在 2删除)';

-- 唯一索引
CREATE UNIQUE INDEX IF NOT EXISTS uk_calc_flow_template_code ON pgzc_calc_flow_template(template_code);
-- 查询索引
CREATE INDEX IF NOT EXISTS idx_calc_flow_task_type ON pgzc_calc_flow_template(task_type);
CREATE INDEX IF NOT EXISTS idx_calc_flow_equipment_type ON pgzc_calc_flow_template(equipment_type);
CREATE INDEX IF NOT EXISTS idx_calc_flow_status ON pgzc_calc_flow_template(status);

-- ----------------------------
-- 2. 评估计算任务表
-- ----------------------------
CREATE TABLE IF NOT EXISTS pgzc_calc_task (
    id                      BIGSERIAL       PRIMARY KEY,
    task_name               VARCHAR(200)    NOT NULL,
    assess_task_id          BIGINT          DEFAULT NULL,
    calc_flow_template_id   BIGINT          NOT NULL,
    template_snapshot_json  TEXT            DEFAULT NULL,
    run_status              VARCHAR(20)     NOT NULL DEFAULT 'WAITING',
    current_stage           VARCHAR(50)     DEFAULT NULL,
    progress_percent        INT             DEFAULT 0,
    result_summary_json     TEXT            DEFAULT NULL,
    log_trace_id            VARCHAR(64)     DEFAULT NULL,
    start_time              TIMESTAMP       DEFAULT NULL,
    end_time                TIMESTAMP       DEFAULT NULL,
    create_by               VARCHAR(64)     DEFAULT '',
    create_time             TIMESTAMP       DEFAULT NULL,
    update_by               VARCHAR(64)     DEFAULT '',
    update_time             TIMESTAMP       DEFAULT NULL,
    remark                  VARCHAR(500)    DEFAULT NULL,
    del_flag                CHAR(1)         NOT NULL DEFAULT '0'
);
COMMENT ON TABLE  pgzc_calc_task IS '评估计算任务表';
COMMENT ON COLUMN pgzc_calc_task.id IS '计算任务ID';
COMMENT ON COLUMN pgzc_calc_task.task_name IS '计算任务名称';
COMMENT ON COLUMN pgzc_calc_task.assess_task_id IS '关联评估任务ID';
COMMENT ON COLUMN pgzc_calc_task.calc_flow_template_id IS '关联流程模板ID';
COMMENT ON COLUMN pgzc_calc_task.template_snapshot_json IS '执行时模板快照';
COMMENT ON COLUMN pgzc_calc_task.run_status IS '运行状态(WAITING/RUNNING/PAUSED/SUCCESS/FAILED/TERMINATED)';
COMMENT ON COLUMN pgzc_calc_task.current_stage IS '当前执行阶段编码';
COMMENT ON COLUMN pgzc_calc_task.progress_percent IS '执行进度(0-100)';
COMMENT ON COLUMN pgzc_calc_task.result_summary_json IS '结果摘要JSON';
COMMENT ON COLUMN pgzc_calc_task.log_trace_id IS '日志跟踪号';
COMMENT ON COLUMN pgzc_calc_task.start_time IS '开始时间';
COMMENT ON COLUMN pgzc_calc_task.end_time IS '结束时间';
COMMENT ON COLUMN pgzc_calc_task.del_flag IS '删除标志(0存在 2删除)';

CREATE INDEX IF NOT EXISTS idx_calc_task_template_id ON pgzc_calc_task(calc_flow_template_id);
CREATE INDEX IF NOT EXISTS idx_calc_task_run_status ON pgzc_calc_task(run_status);

-- ----------------------------
-- 3. 计算任务阶段执行日志表
-- ----------------------------
CREATE TABLE IF NOT EXISTS pgzc_calc_task_stage_log (
    id                BIGSERIAL       PRIMARY KEY,
    calc_task_id      BIGINT          NOT NULL,
    stage_code        VARCHAR(50)     NOT NULL,
    stage_name        VARCHAR(100)    NOT NULL,
    stage_order       INT             NOT NULL,
    execute_status    VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    input_summary     TEXT            DEFAULT NULL,
    output_summary    TEXT            DEFAULT NULL,
    error_message     TEXT            DEFAULT NULL,
    begin_time        TIMESTAMP       DEFAULT NULL,
    finish_time       TIMESTAMP       DEFAULT NULL
);
COMMENT ON TABLE  pgzc_calc_task_stage_log IS '计算任务阶段执行日志表';
COMMENT ON COLUMN pgzc_calc_task_stage_log.id IS '日志ID';
COMMENT ON COLUMN pgzc_calc_task_stage_log.calc_task_id IS '关联计算任务ID';
COMMENT ON COLUMN pgzc_calc_task_stage_log.stage_code IS '阶段编码(SCHEDULE_CONFIG/WEIGHT_CALC/COMPREHENSIVE_CALC/REPORT_OUTPUT)';
COMMENT ON COLUMN pgzc_calc_task_stage_log.stage_name IS '阶段名称';
COMMENT ON COLUMN pgzc_calc_task_stage_log.stage_order IS '执行顺序(1-4)';
COMMENT ON COLUMN pgzc_calc_task_stage_log.execute_status IS '执行状态(PENDING/RUNNING/SUCCESS/FAILED/SKIPPED)';
COMMENT ON COLUMN pgzc_calc_task_stage_log.input_summary IS '输入摘要';
COMMENT ON COLUMN pgzc_calc_task_stage_log.output_summary IS '输出摘要';
COMMENT ON COLUMN pgzc_calc_task_stage_log.error_message IS '异常信息';
COMMENT ON COLUMN pgzc_calc_task_stage_log.begin_time IS '开始时间';
COMMENT ON COLUMN pgzc_calc_task_stage_log.finish_time IS '结束时间';

CREATE INDEX IF NOT EXISTS idx_stage_log_calc_task_id ON pgzc_calc_task_stage_log(calc_task_id);
