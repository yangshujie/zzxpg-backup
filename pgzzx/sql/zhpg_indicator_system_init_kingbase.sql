-- =============================================
-- 指标体系模板表 pgzc_indicator_system_template
-- =============================================
CREATE TABLE IF NOT EXISTS pgzc_indicator_system_template (
    id              BIGSERIAL       PRIMARY KEY,
    template_code   VARCHAR(64)     DEFAULT ''      COMMENT '模板编号',
    template_name   VARCHAR(200)    NOT NULL        COMMENT '模板名称',
    system_type     VARCHAR(64)     DEFAULT ''      COMMENT '指标集类型/一能三性：装备作战效能、作战适用性、体系适用性、在役适用性',
    equipment_type  VARCHAR(64)     DEFAULT ''      COMMENT '装备类型（不少于6类单装域）',
    work_mode       VARCHAR(64)     DEFAULT NULL    COMMENT '工作模式：MAIN_BRANCH_COLLAB / INTERNAL_CIRCULATION',
    indicator_tree  TEXT            DEFAULT NULL    COMMENT '指标树结构JSON',
    config_json     TEXT            DEFAULT NULL    COMMENT '模板配置JSON（权重分配算法、传导关系等）',
    description     VARCHAR(500)    DEFAULT ''      COMMENT '模板描述',
    status          VARCHAR(20)     DEFAULT 'DRAFT' COMMENT '状态（DRAFT/PUBLISHED）',
    del_flag        CHAR(1)         DEFAULT '0'     COMMENT '删除标志（0正常 1删除）',
    create_by       VARCHAR(64)     DEFAULT ''      COMMENT '创建者',
    create_time     TIMESTAMP       DEFAULT NULL    COMMENT '创建时间',
    update_by       VARCHAR(64)     DEFAULT ''      COMMENT '更新者',
    update_time     TIMESTAMP       DEFAULT NULL    COMMENT '更新时间',
    remark          VARCHAR(500)    DEFAULT NULL    COMMENT '备注'
);

COMMENT ON TABLE pgzc_indicator_system_template IS '指标体系模板表';

-- =============================================
-- 评估指标体系表 pgzc_indicator_system
-- =============================================
CREATE TABLE IF NOT EXISTS pgzc_indicator_system (
    id                          BIGSERIAL       PRIMARY KEY,
    system_name                 VARCHAR(500)    NOT NULL        COMMENT '指标体系名称',
    work_mode                   VARCHAR(64)     DEFAULT NULL    COMMENT '工作模式：MAIN_BRANCH_COLLAB / INTERNAL_CIRCULATION',
    indicator_tree              TEXT            DEFAULT NULL    COMMENT '指标树结构JSON',
    indicator_tree_weight       TEXT            DEFAULT NULL    COMMENT '含权重的指标树JSON',
    weight_assign_algorithm     VARCHAR(64)     DEFAULT ''      COMMENT '权重分配算法（层次分析法/熵权法等）',
    weight_assign_params        TEXT            DEFAULT NULL    COMMENT '权重分配算法参数JSON',
    is_applied                  SMALLINT        DEFAULT 0       COMMENT '是否启用（0=停用 1=启用）',
    template_id                 BIGINT          DEFAULT NULL    COMMENT '来源模板ID',
    description                 VARCHAR(500)    DEFAULT ''      COMMENT '描述',
    status                      VARCHAR(20)     DEFAULT 'DRAFT' COMMENT '状态（DRAFT/PUBLISHED）',
    del_flag                    CHAR(1)         DEFAULT '0'     COMMENT '删除标志（0正常 1删除）',
    create_by                   VARCHAR(64)     DEFAULT ''      COMMENT '创建者',
    create_time                 TIMESTAMP       DEFAULT NULL    COMMENT '创建时间',
    update_by                   VARCHAR(64)     DEFAULT ''      COMMENT '更新者',
    update_time                 TIMESTAMP       DEFAULT NULL    COMMENT '更新时间',
    remark                      VARCHAR(500)    DEFAULT NULL    COMMENT '备注'
);

COMMENT ON TABLE pgzc_indicator_system IS '评估指标体系表';

-- 索引
CREATE INDEX idx_pgzc_is_status ON pgzc_indicator_system(status);
CREATE INDEX idx_pgzc_ist_system_type ON pgzc_indicator_system_template(system_type);
