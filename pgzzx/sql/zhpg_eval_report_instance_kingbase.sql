-- =============================================
-- 评估报告生成实例表
-- 在业务库（pgzc_* 表所在库）中执行
-- 数据库：KingbaseES / PostgreSQL
-- =============================================

CREATE TABLE IF NOT EXISTS pgzc_eval_report_instance (
    id                    BIGSERIAL       PRIMARY KEY,
    eval_result_id        BIGINT          NOT NULL,
    calc_task_id          BIGINT          DEFAULT NULL,
    report_code           VARCHAR(100)    NOT NULL,
    generation_no         INTEGER         NOT NULL DEFAULT 1,
    report_template_id    BIGINT          NOT NULL,
    report_template_name  VARCHAR(200)    DEFAULT NULL,
    mapping_json          TEXT            DEFAULT NULL,
    render_status         VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    report_url            VARCHAR(1000)   DEFAULT NULL,
    word_url              VARCHAR(1000)   DEFAULT NULL,
    pdf_url               VARCHAR(1000)   DEFAULT NULL,
    file_format           VARCHAR(20)     DEFAULT NULL,
    error_message         VARCHAR(1000)   DEFAULT NULL,
    create_by             VARCHAR(64)     DEFAULT '',
    create_time           TIMESTAMP       DEFAULT NULL,
    update_by             VARCHAR(64)     DEFAULT '',
    update_time           TIMESTAMP       DEFAULT NULL,
    remark                VARCHAR(500)    DEFAULT NULL,
    del_flag              INTEGER         NOT NULL DEFAULT 0
);

COMMENT ON TABLE pgzc_eval_report_instance IS '评估报告生成实例表';
COMMENT ON COLUMN pgzc_eval_report_instance.eval_result_id IS '关联评估结果ID';
COMMENT ON COLUMN pgzc_eval_report_instance.calc_task_id IS '关联计算任务ID';
COMMENT ON COLUMN pgzc_eval_report_instance.report_code IS '报告生成编号';
COMMENT ON COLUMN pgzc_eval_report_instance.generation_no IS '同一评估结果下的生成序号';
COMMENT ON COLUMN pgzc_eval_report_instance.mapping_json IS '报告匹配规则与渲染字段JSON';
COMMENT ON COLUMN pgzc_eval_report_instance.render_status IS '渲染状态(PENDING/SUCCESS/FAILED)';

CREATE UNIQUE INDEX IF NOT EXISTS uk_eval_report_code ON pgzc_eval_report_instance(report_code);
CREATE INDEX IF NOT EXISTS idx_eval_report_result_id ON pgzc_eval_report_instance(eval_result_id);
CREATE INDEX IF NOT EXISTS idx_eval_report_task_id ON pgzc_eval_report_instance(calc_task_id);
