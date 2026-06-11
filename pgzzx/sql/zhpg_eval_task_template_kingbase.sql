-- ============================================================
-- 评估任务构建分系统(RWGJ) - 业务表建表脚本（人大金仓 / Kingbase）
-- 含：任务模板、评估任务实例、评估准则、模板操作日志(预留)
-- 设计文档：pgzzx/docs/评估任务构建分系统设计.md
-- 脚本幂等，可重复执行。
-- ============================================================

-- ========== 任务模板 ==========
CREATE TABLE IF NOT EXISTS pgzc_eval_task_template (
  id                    BIGSERIAL    PRIMARY KEY,
  template_code         VARCHAR(64)  NOT NULL,
  template_name         VARCHAR(200) NOT NULL,
  template_type         VARCHAR(50)  NOT NULL,
  classification        VARCHAR(20)  NOT NULL DEFAULT 'SPECIFIC',
  equipment_type        VARCHAR(50)  DEFAULT NULL,
  calc_granularity      VARCHAR(50)  DEFAULT NULL,
  indicator_system_id   BIGINT       DEFAULT NULL,
  calc_flow_template_id BIGINT       DEFAULT NULL,
  criterion_set_id      BIGINT       DEFAULT NULL,
  report_template_id    BIGINT       DEFAULT NULL,
  type_config_json      TEXT         DEFAULT NULL,
  version_no            VARCHAR(20)  NOT NULL DEFAULT 'V1.0',
  status                VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
  description           VARCHAR(500) DEFAULT NULL,
  deleted               INT          NOT NULL DEFAULT 0,
  create_by             VARCHAR(64)  DEFAULT '',
  update_by             VARCHAR(64)  DEFAULT '',
  create_time           TIMESTAMP    DEFAULT NULL,
  update_time           TIMESTAMP    DEFAULT NULL,
  remark                VARCHAR(500) DEFAULT NULL,
  CONSTRAINT uk_eval_task_template_code UNIQUE (template_code)
);
COMMENT ON TABLE  pgzc_eval_task_template IS '评估任务模板表';
COMMENT ON COLUMN pgzc_eval_task_template.id IS '主键';
COMMENT ON COLUMN pgzc_eval_task_template.template_code IS '模板编号(系统生成,唯一)';
COMMENT ON COLUMN pgzc_eval_task_template.template_name IS '模板名称';
COMMENT ON COLUMN pgzc_eval_task_template.template_type IS '评估方法类型(EQUIP_EFFECTIVENESS作战效能/SYSTEM_CONTRIBUTION体系贡献率/TASK_SATISFACTION作战任务满足度/SPECIAL_CAPABILITY专项能力预留/KEY_FACTOR关键因素预留)';
COMMENT ON COLUMN pgzc_eval_task_template.classification IS '模板分类(GENERAL通用/SPECIFIC专用)';
COMMENT ON COLUMN pgzc_eval_task_template.equipment_type IS '装备类型(space_recon/space_domain_awareness/space_defense/space_track_control/space_launch/sea_based_space)';
COMMENT ON COLUMN pgzc_eval_task_template.calc_granularity IS '评估粒度';
COMMENT ON COLUMN pgzc_eval_task_template.indicator_system_id IS '引用指标体系ID';
COMMENT ON COLUMN pgzc_eval_task_template.calc_flow_template_id IS '引用计算流程模板ID';
COMMENT ON COLUMN pgzc_eval_task_template.criterion_set_id IS '引用评估准则集ID';
COMMENT ON COLUMN pgzc_eval_task_template.report_template_id IS '引用报告模板ID';
COMMENT ON COLUMN pgzc_eval_task_template.type_config_json IS '类型差异化配置JSON';
COMMENT ON COLUMN pgzc_eval_task_template.version_no IS '版本号';
COMMENT ON COLUMN pgzc_eval_task_template.status IS '状态(DRAFT/TESTING/PUBLISHED/DISABLED)';
COMMENT ON COLUMN pgzc_eval_task_template.description IS '模板说明';
COMMENT ON COLUMN pgzc_eval_task_template.deleted IS '逻辑删除(0否1是)';

-- ========== 评估任务实例 ==========
CREATE TABLE IF NOT EXISTS pgzc_eval_task (
  id                    BIGSERIAL    PRIMARY KEY,
  task_no               VARCHAR(64)  NOT NULL,
  task_name             VARCHAR(200) NOT NULL,
  source_template_id    BIGINT       DEFAULT NULL,
  template_type         VARCHAR(50)  DEFAULT NULL,
  task_type             VARCHAR(50)  DEFAULT NULL,
  requirement_id        BIGINT       DEFAULT NULL,
  build_mode            VARCHAR(20)  DEFAULT 'NEW',
  indicator_system_id   BIGINT       DEFAULT NULL,
  data_source_id        BIGINT       DEFAULT NULL,
  calc_flow_template_id BIGINT       DEFAULT NULL,
  criterion_set_id      BIGINT       DEFAULT NULL,
  report_template_id    BIGINT       DEFAULT NULL,
  config_snapshot_json  TEXT         DEFAULT NULL,
  task_status           VARCHAR(20)  NOT NULL DEFAULT 'CREATED',
  dispatch_status       VARCHAR(20)  NOT NULL DEFAULT 'NONE',
  target_center_id      BIGINT       DEFAULT NULL,
  target_center_name    VARCHAR(200) DEFAULT NULL,
  deleted               INT          NOT NULL DEFAULT 0,
  create_by             VARCHAR(64)  DEFAULT '',
  update_by             VARCHAR(64)  DEFAULT '',
  create_time           TIMESTAMP    DEFAULT NULL,
  update_time           TIMESTAMP    DEFAULT NULL,
  CONSTRAINT uk_eval_task_no UNIQUE (task_no)
);
COMMENT ON TABLE  pgzc_eval_task IS '评估任务实例表(id 即 CalcTask.assess_task_id 的指向)';
COMMENT ON COLUMN pgzc_eval_task.id IS '主键(= pgzc_calc_task.assess_task_id)';
COMMENT ON COLUMN pgzc_eval_task.task_no IS '任务编号(唯一)';
COMMENT ON COLUMN pgzc_eval_task.task_name IS '任务名称';
COMMENT ON COLUMN pgzc_eval_task.source_template_id IS '来源任务模板ID(空=空白新建)';
COMMENT ON COLUMN pgzc_eval_task.template_type IS '模板类型(决定编辑跳转的向导)';
COMMENT ON COLUMN pgzc_eval_task.task_type IS '试验阶段(INITIAL_COMBAT装备初期作战/PERFORMANCE_TEST性能鉴定/COMBAT_TEST作战试验/INSERVICE_ASSESS在役考核/FACTOR_ANALYSIS影响因子分析)';
COMMENT ON COLUMN pgzc_eval_task.requirement_id IS '来源需求ID';
COMMENT ON COLUMN pgzc_eval_task.build_mode IS '构建模式(NEW新建/FROM_REQUIREMENT需求生成)';
COMMENT ON COLUMN pgzc_eval_task.config_snapshot_json IS '任务实例配置副本(权重/参数微调结果)';
COMMENT ON COLUMN pgzc_eval_task.task_status IS '任务状态(CREATED/CONFIGURING/READY/CALCULATING/COMPLETED/PAUSED)';
COMMENT ON COLUMN pgzc_eval_task.dispatch_status IS '下发状态(NONE/SENT/RECEIVED/CONFIRMED/RETURNED)';
COMMENT ON COLUMN pgzc_eval_task.deleted IS '逻辑删除(0否1是)';
CREATE INDEX IF NOT EXISTS idx_eval_task_template ON pgzc_eval_task (source_template_id);

-- ========== 评估准则 ==========
CREATE TABLE IF NOT EXISTS pgzc_eval_criterion (
  id                    BIGSERIAL     PRIMARY KEY,
  criterion_code        VARCHAR(64)   NOT NULL,
  criterion_name        VARCHAR(200)  NOT NULL,
  set_id                BIGINT        DEFAULT NULL,
  equipment_type        VARCHAR(50)   DEFAULT NULL,
  indicator_id          BIGINT        DEFAULT NULL,
  indicator_level       INT           DEFAULT NULL,
  indicator_system_id   BIGINT        DEFAULT NULL,
  build_mode            VARCHAR(20)   NOT NULL DEFAULT 'CUSTOM',
  rule_type             VARCHAR(30)   NOT NULL,
  rule_json             TEXT          DEFAULT NULL,
  value_category        VARCHAR(20)   DEFAULT NULL,
  best_value            NUMERIC(20,6) DEFAULT NULL,
  worst_value           NUMERIC(20,6) DEFAULT NULL,
  conclusion_template   VARCHAR(500)  DEFAULT NULL,
  priority              INT           NOT NULL DEFAULT 0,
  status                VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
  deleted               INT           NOT NULL DEFAULT 0,
  create_by             VARCHAR(64)   DEFAULT '',
  update_by             VARCHAR(64)   DEFAULT '',
  create_time           TIMESTAMP     DEFAULT NULL,
  update_time           TIMESTAMP     DEFAULT NULL,
  CONSTRAINT uk_eval_criterion_code UNIQUE (criterion_code)
);
COMMENT ON TABLE  pgzc_eval_criterion IS '评估准则表';
COMMENT ON COLUMN pgzc_eval_criterion.set_id IS '准则集ID(被模板/任务以criterion_set_id引用)';
COMMENT ON COLUMN pgzc_eval_criterion.equipment_type IS '装备类型(按6类分类管理)';
COMMENT ON COLUMN pgzc_eval_criterion.indicator_id IS '绑定指标ID(空=作用于准则集/任务级)';
COMMENT ON COLUMN pgzc_eval_criterion.indicator_level IS '指标层级(1/2/3...)';
COMMENT ON COLUMN pgzc_eval_criterion.build_mode IS '构建方式(CUSTOM自定义/SCRIPT脚本预留)';
COMMENT ON COLUMN pgzc_eval_criterion.rule_type IS '规则类型(THRESHOLD/LEVEL_MAP/CONDITION/FORMULA/SCRIPT)';
COMMENT ON COLUMN pgzc_eval_criterion.rule_json IS '规则定义JSON';
COMMENT ON COLUMN pgzc_eval_criterion.value_category IS '指标值类型(成本型/效益型/区间效益型),用于归一化';
COMMENT ON COLUMN pgzc_eval_criterion.best_value IS '最佳值,调用时注入';
COMMENT ON COLUMN pgzc_eval_criterion.worst_value IS '最差值,调用时注入';
COMMENT ON COLUMN pgzc_eval_criterion.conclusion_template IS '结论文案(如:有效压制/部分压制/未形成压制)';
COMMENT ON COLUMN pgzc_eval_criterion.priority IS '优先级(多准则命中时执行序)';
COMMENT ON COLUMN pgzc_eval_criterion.deleted IS '逻辑删除(0否1是)';
CREATE INDEX IF NOT EXISTS idx_eval_criterion_set ON pgzc_eval_criterion (set_id);
CREATE INDEX IF NOT EXISTS idx_eval_criterion_indicator ON pgzc_eval_criterion (indicator_id);

-- ========== 模板操作日志（预留，本期仅建表） ==========
CREATE TABLE IF NOT EXISTS pgzc_eval_task_template_log (
  id              BIGSERIAL    PRIMARY KEY,
  template_id     BIGINT       NOT NULL,
  operation_type  VARCHAR(20)  NOT NULL,
  operator        VARCHAR(64)  DEFAULT '',
  operation_time  TIMESTAMP    DEFAULT NULL,
  content_diff    TEXT         DEFAULT NULL,
  remark          VARCHAR(500) DEFAULT NULL
);
COMMENT ON TABLE  pgzc_eval_task_template_log IS '评估任务模板操作日志表(预留)';
COMMENT ON COLUMN pgzc_eval_task_template_log.operation_type IS '操作类型(CREATE/UPDATE/DELETE/PUBLISH/DISABLE)';
COMMENT ON COLUMN pgzc_eval_task_template_log.content_diff IS '变更摘要(JSON diff)';
CREATE INDEX IF NOT EXISTS idx_eval_tpl_log_template ON pgzc_eval_task_template_log (template_id);
