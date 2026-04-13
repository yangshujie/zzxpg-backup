-- 计算流程模板绑定 XXL-JOB 任务ID
-- 发布模板时由 ZHPG 自动注册到 xxl-job-admin，回写 xxl_job_id

ALTER TABLE pgzc_calc_flow_template
    ADD COLUMN IF NOT EXISTS xxl_job_id INT DEFAULT NULL;

COMMENT ON COLUMN pgzc_calc_flow_template.xxl_job_id
    IS 'XXL-JOB admin 中对应的 jobInfo 主键；发布时由 ZHPG 自动注册写入';

CREATE INDEX IF NOT EXISTS idx_calc_flow_template_xxl_job_id
    ON pgzc_calc_flow_template(xxl_job_id);
