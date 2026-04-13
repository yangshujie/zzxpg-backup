CREATE TABLE IF NOT EXISTS pgzc_eval_data_source (
    id                BIGSERIAL PRIMARY KEY,
    source_code       VARCHAR(64)   NOT NULL,
    source_name       VARCHAR(200)  NOT NULL,
    source_type       VARCHAR(64)   NOT NULL,
    host              VARCHAR(255),
    port              INTEGER,
    database_name     VARCHAR(200),
    schema_name       VARCHAR(100),
    table_name        VARCHAR(200),
    api_url           VARCHAR(500),
    request_method    VARCHAR(16),
    file_path         VARCHAR(500),
    username          VARCHAR(100),
    password          VARCHAR(255),
    field_names       VARCHAR(1000),
    request_params    TEXT,
    description       VARCHAR(500),
    status            VARCHAR(32) DEFAULT 'ENABLED',
    last_test_status  VARCHAR(32),
    last_test_message VARCHAR(1000),
    last_test_time    TIMESTAMP,
    del_flag          CHAR(1)     DEFAULT '0',
    create_by         VARCHAR(64),
    create_time       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_by         VARCHAR(64),
    update_time       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    remark            VARCHAR(500)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_pgzc_eval_data_source_code ON pgzc_eval_data_source (source_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_pgzc_eval_data_source_name ON pgzc_eval_data_source (source_name);
CREATE INDEX IF NOT EXISTS idx_pgzc_eval_data_source_type ON pgzc_eval_data_source (source_type);
CREATE INDEX IF NOT EXISTS idx_pgzc_eval_data_source_status ON pgzc_eval_data_source (status);

COMMENT ON TABLE pgzc_eval_data_source IS '评估数据源配置表';
COMMENT ON COLUMN pgzc_eval_data_source.id IS '主键';
COMMENT ON COLUMN pgzc_eval_data_source.source_code IS '数据源编号';
COMMENT ON COLUMN pgzc_eval_data_source.source_name IS '数据源名称';
COMMENT ON COLUMN pgzc_eval_data_source.source_type IS '数据源类型(MYSQL/POSTGRESQL/ORACLE/KINGBASE/HIVE/REST_API/CSV/EXCEL/TXT/WORD/WPS/PDF/DAT/JSON)';
COMMENT ON COLUMN pgzc_eval_data_source.host IS '主机地址';
COMMENT ON COLUMN pgzc_eval_data_source.port IS '端口';
COMMENT ON COLUMN pgzc_eval_data_source.database_name IS '数据库/服务名称';
COMMENT ON COLUMN pgzc_eval_data_source.schema_name IS 'Schema名称';
COMMENT ON COLUMN pgzc_eval_data_source.table_name IS '表名/视图名';
COMMENT ON COLUMN pgzc_eval_data_source.api_url IS 'REST接口地址';
COMMENT ON COLUMN pgzc_eval_data_source.request_method IS '请求方法';
COMMENT ON COLUMN pgzc_eval_data_source.file_path IS '文件路径';
COMMENT ON COLUMN pgzc_eval_data_source.username IS '用户名';
COMMENT ON COLUMN pgzc_eval_data_source.password IS '密码';
COMMENT ON COLUMN pgzc_eval_data_source.field_names IS '关联字段，逗号分隔';
COMMENT ON COLUMN pgzc_eval_data_source.request_params IS '请求参数(JSON或文本)';
COMMENT ON COLUMN pgzc_eval_data_source.description IS '描述';
COMMENT ON COLUMN pgzc_eval_data_source.status IS '状态(ENABLED/DISABLED)';
COMMENT ON COLUMN pgzc_eval_data_source.last_test_status IS '最近测试状态(SUCCESS/FAILED)';
COMMENT ON COLUMN pgzc_eval_data_source.last_test_message IS '最近测试结果';
COMMENT ON COLUMN pgzc_eval_data_source.last_test_time IS '最近测试时间';
COMMENT ON COLUMN pgzc_eval_data_source.del_flag IS '删除标记(0存在 2删除)';
