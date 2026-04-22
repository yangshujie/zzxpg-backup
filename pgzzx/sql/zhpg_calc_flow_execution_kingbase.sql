-- =============================================
-- Calc flow execution runtime config table
-- Database: KingbaseES / PostgreSQL
-- =============================================

CREATE TABLE IF NOT EXISTS pgzc_calc_flow_execution (
    id                     BIGSERIAL       PRIMARY KEY,
    execution_code         VARCHAR(100)    NOT NULL,
    template_id            BIGINT          NOT NULL,
    indicator_system_id    BIGINT          NOT NULL,
    execution_name         VARCHAR(200)    DEFAULT NULL,
    runtime_config_json    TEXT            DEFAULT NULL,
    step_state_json        TEXT            DEFAULT NULL,
    template_snapshot_json TEXT            DEFAULT NULL,
    current_step           VARCHAR(64)     DEFAULT NULL,
    calc_task_id           BIGINT          DEFAULT NULL,
    eval_result_id         BIGINT          DEFAULT NULL,
    latest_report_id       BIGINT          DEFAULT NULL,
    status                 VARCHAR(32)     NOT NULL DEFAULT 'DRAFT',
    create_by              VARCHAR(64)     DEFAULT '',
    create_time            TIMESTAMP       DEFAULT NULL,
    update_by              VARCHAR(64)     DEFAULT '',
    update_time            TIMESTAMP       DEFAULT NULL,
    remark                 VARCHAR(500)    DEFAULT NULL,
    del_flag               VARCHAR(1)      NOT NULL DEFAULT '0'
);

COMMENT ON TABLE pgzc_calc_flow_execution IS 'Calc flow execution runtime config';
COMMENT ON COLUMN pgzc_calc_flow_execution.runtime_config_json IS 'Runtime config copied from template and adjusted for this execution';
COMMENT ON COLUMN pgzc_calc_flow_execution.step_state_json IS 'Frontend wizard step state';
COMMENT ON COLUMN pgzc_calc_flow_execution.template_snapshot_json IS 'Template config snapshot when execution was initialized';

CREATE UNIQUE INDEX IF NOT EXISTS uk_calc_flow_execution_code ON pgzc_calc_flow_execution(execution_code);
CREATE INDEX IF NOT EXISTS idx_calc_flow_execution_context ON pgzc_calc_flow_execution(template_id, indicator_system_id, del_flag);
CREATE INDEX IF NOT EXISTS idx_calc_flow_execution_task ON pgzc_calc_flow_execution(calc_task_id);
CREATE INDEX IF NOT EXISTS idx_calc_flow_execution_result ON pgzc_calc_flow_execution(eval_result_id);
