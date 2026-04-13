-- ===============================================================
-- 算法需求管理表 - 人大金仓数据库版本
-- ===============================================================

-- 1. 算法需求主表
CREATE TABLE pgzc_algorithm_requirement (
    id              BIGSERIAL PRIMARY KEY COMMENT '需求ID',
    code            VARCHAR(50) NOT NULL COMMENT '需求编号',
    title           VARCHAR(200) NOT NULL COMMENT '需求名称',
    type            VARCHAR(20) COMMENT '需求类型：新增算法、算法改造、参数调优',
    source          VARCHAR(100) COMMENT '需求来源',
    template        VARCHAR(200) COMMENT '关联模板',
    input           VARCHAR(1000) COMMENT '关键输入',
    output          VARCHAR(1000) COMMENT '目标输出',
    summary         TEXT COMMENT '需求说明',
    acceptance      VARCHAR(500) COMMENT '验收方式',
    special_config  VARCHAR(500) COMMENT '特殊配置要求',
    status          VARCHAR(20) DEFAULT '待构建' COMMENT '状态：待构建、构建中、已构建',
    algorithm_id    BIGINT COMMENT '关联算法ID',
    algorithm_name  VARCHAR(200) COMMENT '关联算法名称',
    notify_status   VARCHAR(20) DEFAULT '待通知' COMMENT '通知状态：待通知、已通知、通知失败',
    notify_time     VARCHAR(50) COMMENT '通知时间',
    notify_type     VARCHAR(20) COMMENT '通知方式：HTTP、MQ、WEBHOOK',
    notify_target   VARCHAR(500) COMMENT '通知目标地址',
    data_status     VARCHAR(20) DEFAULT 'ENABLED' COMMENT '数据状态：ENABLED、DISABLED',
    del_flag        CHAR(1) DEFAULT '0' COMMENT '删除标志：0正常、2删除',
    create_by       VARCHAR(64) DEFAULT ''::VARCHAR COMMENT '创建者',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       VARCHAR(64) DEFAULT ''::VARCHAR COMMENT '更新者',
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    remark          VARCHAR(500) COMMENT '备注',
    CONSTRAINT uk_code UNIQUE (code)
);

COMMENT ON TABLE pgzc_algorithm_requirement IS '算法需求表';

-- 2. 算法需求参数要求表
CREATE TABLE pgzc_algorithm_requirement_param (
    id              BIGSERIAL PRIMARY KEY COMMENT '参数ID',
    requirement_id  BIGINT NOT NULL COMMENT '需求ID',
    param_field    VARCHAR(100) COMMENT '参数名称',
    param_type     VARCHAR(50) COMMENT '参数类型：string、number、array、dictionary、date',
    default_value  VARCHAR(200) COMMENT '默认值',
    required_flag  SMALLINT DEFAULT 0 COMMENT '是否必填：0否、1是',
    param_desc     VARCHAR(500) COMMENT '参数说明',
    sort_order     INT DEFAULT 0 COMMENT '参数顺序',
    create_by      VARCHAR(64) DEFAULT ''::VARCHAR COMMENT '创建者',
    create_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by      VARCHAR(64) DEFAULT ''::VARCHAR COMMENT '更新者',
    update_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_requirement FOREIGN KEY (requirement_id)
        REFERENCES pgzc_algorithm_requirement(id) ON DELETE CASCADE
);

COMMENT ON TABLE pgzc_algorithm_requirement_param IS '算法需求参数要求表';

-- 3. 创建索引
CREATE INDEX idx_requirement_code ON pgzc_algorithm_requirement(code);
CREATE INDEX idx_requirement_status ON pgzc_algorithm_requirement(status);
CREATE INDEX idx_requirement_type ON pgzc_algorithm_requirement(type);
CREATE INDEX idx_requirement_algorithm ON pgzc_algorithm_requirement(algorithm_id);
CREATE INDEX idx_param_requirement ON pgzc_algorithm_requirement_param(requirement_id);

-- ===============================================================
-- 初始数据
-- ===============================================================

INSERT INTO pgzc_algorithm_requirement (code, title, type, source, template, input, output, summary, acceptance, special_config, status, notify_status, create_by) VALUES
('ALG-REQ-001', '体系贡献率边际贡献算法', '新增算法', '总体设计会审', '体系贡献率评估任务模板', '体系关系网（JSON格式）、任务网络拓扑、节点状态列表', '节点贡献率（数组）、贡献排序（列表）', '需要按节点剔除与替代策略计算体系贡献率，并输出关键节点贡献排序。', '完成算法入库、参数配置和2个场景样例验证。', '需支持大规模节点网络（100+节点）的高效计算', '待构建', '待通知', 'admin'),
('ALG-REQ-002', '作战效能阶段波动识别算法', '算法改造', '分系统对接', '作战效能评估任务模板', '阶段评分时序数据、事件序列、扰动标记', '波动识别结果、异常阶段说明', '需要增强时序识别能力，支持阶段波动解释。', '算法准确率达到历史样本基线以上，并具备可解释输出。', '需输出波动原因分析', '已构建', '已通知', 'admin'),
('ALG-REQ-003', '任务满足度缺口分级规则', '参数调优', '试验阶段复盘', '作战任务满足度评估任务模板', '需求满足度分数、历史缺口样本', '缺口等级（A/B/C/D）、整改建议标签', '面向缺口分级与整改建议联动，细化规则阈值。', '完成参数配置，形成可复用的分级规则模板。', '分级阈值可配置', '已构建', '待通知', 'admin'),
('ALG-REQ-004', '关键因素敏感性分析算法', '新增算法', '技术评审', '关键因素影响分析任务模板', '因素列表、扰动范围[min,max]、基准值', '敏感性系数、关键因素排序', '需要实现单因素和多因素敏感性分析算法。', '完成算法实现及3个以上场景验证。', '需支持Sobol和Morris两种敏感性分析方法', '待构建', '待通知', 'admin'),
('ALG-REQ-005', '协同指数计算优化算法', '算法改造', '分系统对接', '体系贡献率评估任务模板', '协同矩阵、节点权重向量', '协同指数（标量）、协同趋势图数据', '优化协同指数计算精度和性能。', '计算效率提升30%以上，精度满足要求。', '需优化大规模矩阵计算性能', '待构建', '待通知', 'admin');

-- 插入参数数据
INSERT INTO pgzc_algorithm_requirement_param (requirement_id, param_field, param_type, default_value, required_flag, param_desc, sort_order) VALUES
(1, 'networkData', 'dictionary', '', 1, '体系关系网络数据', 1),
(1, 'missionData', 'dictionary', '', 1, '任务网络数据', 2),
(1, 'nodeStatus', 'array', '', 1, '节点状态列表', 3),
(1, 'threshold', 'number', '0.05', 0, '贡献率阈值', 4),
(4, 'factorList', 'array', '', 1, '待分析因素列表', 1),
(4, 'perturbRange', 'array', '', 1, '扰动范围[min,max]', 2),
(4, 'baselineValue', 'number', '', 1, '基准值', 3),
(4, 'method', 'string', 'Sobol', 0, '分析方法(Sobol/Morris)', 4);

-- ===============================================================
-- 序列重置（确保ID从正确位置开始）
-- ===============================================================
SELECT setval('pgzc_algorithm_requirement_id_seq', (SELECT COALESCE(MAX(id), 0) FROM pgzc_algorithm_requirement));
SELECT setval('pgzc_algorithm_requirement_param_id_seq', (SELECT COALESCE(MAX(id), 0) FROM pgzc_algorithm_requirement_param));
