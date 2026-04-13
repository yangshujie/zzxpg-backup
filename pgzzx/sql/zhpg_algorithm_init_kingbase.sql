-- =============================================
-- 算法模型配置子系统(SFMX) - 数据库初始化脚本
-- =============================================

-- ----------------------------
-- 算法信息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS pgzc_algorithm (
    id                  BIGSERIAL       PRIMARY KEY,
    algorithm_name      VARCHAR(200)    NOT NULL,
    algorithm_type      VARCHAR(64)     DEFAULT '',
    equipment_type      VARCHAR(64)     DEFAULT '',
    algorithm_desc      TEXT            DEFAULT NULL,
    algorithm_version   VARCHAR(20)     DEFAULT 'V1',
    algorithm_code_url  VARCHAR(500)    DEFAULT '',
    base_flag           SMALLINT        DEFAULT 0,
    publish_status      VARCHAR(20)     DEFAULT 'DRAFT',
    status              VARCHAR(20)     DEFAULT 'ENABLED',
    del_flag            CHAR(1)         DEFAULT '0',
    create_by           VARCHAR(64)     DEFAULT '',
    create_time         TIMESTAMP       DEFAULT NULL,
    update_by           VARCHAR(64)     DEFAULT '',
    update_time         TIMESTAMP       DEFAULT NULL,
    remark              VARCHAR(500)    DEFAULT NULL
);
COMMENT ON TABLE pgzc_algorithm IS '算法信息表';
COMMENT ON COLUMN pgzc_algorithm.id IS '算法ID';
COMMENT ON COLUMN pgzc_algorithm.algorithm_name IS '算法名称';
COMMENT ON COLUMN pgzc_algorithm.algorithm_type IS '算法类型(预处理/指标量化/属性值计算/权重分配/权重传导/方案评价/其它)';
COMMENT ON COLUMN pgzc_algorithm.equipment_type IS '装备类型';
COMMENT ON COLUMN pgzc_algorithm.algorithm_desc IS '算法描述';
COMMENT ON COLUMN pgzc_algorithm.algorithm_version IS '算法版本';
COMMENT ON COLUMN pgzc_algorithm.algorithm_code_url IS '算法代码文件路径';
COMMENT ON COLUMN pgzc_algorithm.base_flag IS '是否需要基准数据(0否 1是)';
COMMENT ON COLUMN pgzc_algorithm.publish_status IS '发布状态(DRAFT草稿 PUBLISHED已发布)';
COMMENT ON COLUMN pgzc_algorithm.status IS '状态(ENABLED启用 DISABLED停用)';
COMMENT ON COLUMN pgzc_algorithm.del_flag IS '删除标志(0存在 2删除)';

-- ----------------------------
-- 算法参数表 (优化：独立参数表替代JSON存储)
-- ----------------------------
CREATE TABLE IF NOT EXISTS pgzc_algorithm_param (
    id                  BIGSERIAL       PRIMARY KEY,
    algorithm_id        BIGINT          NOT NULL,
    param_category      VARCHAR(20)     NOT NULL DEFAULT 'CONFIG',
    param_name          VARCHAR(100)    DEFAULT '',
    param_field         VARCHAR(100)    DEFAULT '',
    param_type          VARCHAR(64)     DEFAULT 'string',
    default_value       VARCHAR(500)    DEFAULT '',
    param_desc          VARCHAR(500)    DEFAULT '',
    required_flag       SMALLINT        DEFAULT 0,
    sort_order          INT             DEFAULT 0,
    del_flag            CHAR(1)         DEFAULT '0',
    create_by           VARCHAR(64)     DEFAULT '',
    create_time         TIMESTAMP       DEFAULT NULL,
    update_by           VARCHAR(64)     DEFAULT '',
    update_time         TIMESTAMP       DEFAULT NULL,
    remark              VARCHAR(500)    DEFAULT NULL
);
COMMENT ON TABLE pgzc_algorithm_param IS '算法参数表';
COMMENT ON COLUMN pgzc_algorithm_param.id IS '参数ID';
COMMENT ON COLUMN pgzc_algorithm_param.algorithm_id IS '所属算法ID';
COMMENT ON COLUMN pgzc_algorithm_param.param_category IS '参数类别(INPUT输入参数 OUTPUT输出参数 CONFIG配置参数)';
COMMENT ON COLUMN pgzc_algorithm_param.param_name IS '参数显示名称';
COMMENT ON COLUMN pgzc_algorithm_param.param_field IS '参数字段名(英文标识)';
COMMENT ON COLUMN pgzc_algorithm_param.param_type IS '参数类型(string/number/array/dictionary/date/tel/template)';
COMMENT ON COLUMN pgzc_algorithm_param.default_value IS '默认值';
COMMENT ON COLUMN pgzc_algorithm_param.param_desc IS '参数描述';
COMMENT ON COLUMN pgzc_algorithm_param.required_flag IS '是否必填(0否 1是)';
COMMENT ON COLUMN pgzc_algorithm_param.sort_order IS '排序号';
COMMENT ON COLUMN pgzc_algorithm_param.del_flag IS '删除标志(0存在 2删除)';
COMMENT ON COLUMN pgzc_algorithm_param.remark IS '备注';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_algorithm_param_alg_id ON pgzc_algorithm_param(algorithm_id);
CREATE INDEX IF NOT EXISTS idx_algorithm_type ON pgzc_algorithm(algorithm_type);
CREATE INDEX IF NOT EXISTS idx_algorithm_equipment_type ON pgzc_algorithm(equipment_type);
CREATE INDEX IF NOT EXISTS idx_algorithm_publish_status ON pgzc_algorithm(publish_status);

-- 字典数据请在系统库中执行: zhpg_algorithm_dict_kingbase.sql
