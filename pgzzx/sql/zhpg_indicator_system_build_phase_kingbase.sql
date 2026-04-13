-- =============================================
-- 指标体系表：主分协同构建阶段管理字段
-- 适用场景：主分协同模式下，分系统主动拉取当前指标树 → 回传完整指标体系（已回传细化）
-- =============================================

-- 构建阶段：INITIAL_DRAFT=初始粗建，REFINED=已回传细化（仅主分协同）；历史 SUBMITTED/FINALIZED 由迁移脚本归一
ALTER TABLE pgzc_indicator_system ADD COLUMN IF NOT EXISTS build_phase VARCHAR(32) DEFAULT NULL
    COMMENT '构建阶段：INITIAL_DRAFT/REFINED（仅主分协同；内部流转应为 NULL）';

-- 回传来源分系统标识
ALTER TABLE pgzc_indicator_system ADD COLUMN IF NOT EXISTS source_subsystem VARCHAR(128) DEFAULT NULL
    COMMENT '回传来源分系统标识（如：需求分析分系统）';

-- 细化回传时间
ALTER TABLE pgzc_indicator_system ADD COLUMN IF NOT EXISTS refined_time TIMESTAMP DEFAULT NULL
    COMMENT '分系统回传细化指标体系的时间';

-- initial_tree 对比基准列已废弃（对比使用 indicator_tree / refined_indicator_tree）
ALTER TABLE pgzc_indicator_system DROP COLUMN IF EXISTS initial_tree;

-- 索引
CREATE INDEX IF NOT EXISTS idx_pgzc_is_build_phase ON pgzc_indicator_system(build_phase);

-- 将历史「已提交分系统」阶段归一为初始粗建（分系统改为主动拉取后不再使用 SUBMITTED）
UPDATE pgzc_indicator_system SET build_phase = 'INITIAL_DRAFT' WHERE build_phase = 'SUBMITTED';
