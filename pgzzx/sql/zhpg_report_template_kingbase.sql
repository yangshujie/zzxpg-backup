-- 报告模板主表
CREATE TABLE IF NOT EXISTS pgzc_report_template (
  id BIGSERIAL PRIMARY KEY,
  template_code VARCHAR(128) NOT NULL,
  template_name VARCHAR(255) NOT NULL,
  template_description VARCHAR(500) DEFAULT NULL,
  template_type VARCHAR(128) DEFAULT NULL,
  evaluation_type VARCHAR(128) DEFAULT NULL,
  target_object VARCHAR(128) DEFAULT NULL,
  status VARCHAR(64) DEFAULT 'DRAFT',
  document_type VARCHAR(64) DEFAULT NULL,
  cover_config VARCHAR(1000) DEFAULT NULL,
  upload_url VARCHAR(255) DEFAULT NULL,
  current_version_id BIGINT DEFAULT NULL,
  deleted INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) DEFAULT '',
  update_by VARCHAR(64) DEFAULT '',
  create_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_pgzc_report_template_code ON pgzc_report_template (template_code);

-- 模板版本表
CREATE TABLE IF NOT EXISTS pgzc_template_version (
  id BIGSERIAL PRIMARY KEY,
  template_id BIGINT NOT NULL,
  version_no VARCHAR(64) NOT NULL,
  html_content TEXT NOT NULL,
  dsl_json TEXT DEFAULT NULL,
  change_log VARCHAR(500) DEFAULT NULL,
  create_by VARCHAR(64) DEFAULT '',
  create_time TIMESTAMP DEFAULT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_pgzc_template_version ON pgzc_template_version (template_id, version_no);

COMMENT ON TABLE pgzc_report_template IS '报告模板主表';
COMMENT ON COLUMN pgzc_report_template.id IS '主键';
COMMENT ON COLUMN pgzc_report_template.template_code IS '模板代码（唯一）';
COMMENT ON COLUMN pgzc_report_template.template_name IS '模板名称';
COMMENT ON COLUMN pgzc_report_template.template_description IS '模板描述';
COMMENT ON COLUMN pgzc_report_template.template_type IS '模板类型';
COMMENT ON COLUMN pgzc_report_template.evaluation_type IS '评估类型';
COMMENT ON COLUMN pgzc_report_template.target_object IS '评估对象';
COMMENT ON COLUMN pgzc_report_template.status IS '状态（DRAFT/PUBLISHED）';
COMMENT ON COLUMN pgzc_report_template.document_type IS '文档类型';
COMMENT ON COLUMN pgzc_report_template.cover_config IS '封面配置';
COMMENT ON COLUMN pgzc_report_template.upload_url IS '上传地址';
COMMENT ON COLUMN pgzc_report_template.current_version_id IS '当前版本ID';
COMMENT ON COLUMN pgzc_report_template.deleted IS '逻辑删除';
COMMENT ON COLUMN pgzc_report_template.create_by IS '创建人';
COMMENT ON COLUMN pgzc_report_template.update_by IS '更新人';
COMMENT ON COLUMN pgzc_report_template.create_time IS '创建时间';
COMMENT ON COLUMN pgzc_report_template.update_time IS '更新时间';

COMMENT ON TABLE pgzc_template_version IS '模板版本表';
COMMENT ON COLUMN pgzc_template_version.id IS '主键';
COMMENT ON COLUMN pgzc_template_version.template_id IS '模板ID';
COMMENT ON COLUMN pgzc_template_version.version_no IS '版本号（v1,v2...）';
COMMENT ON COLUMN pgzc_template_version.html_content IS '模板HTML内容';
COMMENT ON COLUMN pgzc_template_version.dsl_json IS '可选DSL内容';
COMMENT ON COLUMN pgzc_template_version.change_log IS '变更说明';
COMMENT ON COLUMN pgzc_template_version.create_by IS '创建人';
COMMENT ON COLUMN pgzc_template_version.create_time IS '创建时间';
