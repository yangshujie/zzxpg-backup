-- ============================================================
-- 评估任务构建分系统(RWGJ) - 评估准则与准则集升级脚本（人大金仓 / Kingbase）
-- ============================================================

-- ========== 1. 评估准则集主表 ==========
CREATE TABLE IF NOT EXISTS pgzc_eval_criterion_set (
  id                    BIGSERIAL     PRIMARY KEY,
  set_code              VARCHAR(64)   NOT NULL,
  set_name              VARCHAR(200)  NOT NULL,
  indicator_system_id   BIGINT        NOT NULL,
  equipment_type        VARCHAR(50)   DEFAULT NULL,
  status                VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
  description           VARCHAR(500)  DEFAULT NULL,
  deleted               INT           NOT NULL DEFAULT 0,
  create_by             VARCHAR(64)   DEFAULT '',
  update_by             VARCHAR(64)   DEFAULT '',
  create_time           TIMESTAMP     DEFAULT NULL,
  update_time           TIMESTAMP     DEFAULT NULL,
  CONSTRAINT uk_eval_criterion_set_code UNIQUE (set_code)
);

COMMENT ON TABLE  pgzc_eval_criterion_set IS '评估准则集主表';
COMMENT ON COLUMN pgzc_eval_criterion_set.id IS '主键';
COMMENT ON COLUMN pgzc_eval_criterion_set.set_code IS '准则集编号(系统生成,唯一)';
COMMENT ON COLUMN pgzc_eval_criterion_set.set_name IS '准则集名称';
COMMENT ON COLUMN pgzc_eval_criterion_set.indicator_system_id IS '绑定的指标体系ID';
COMMENT ON COLUMN pgzc_eval_criterion_set.equipment_type IS '装备类型';
COMMENT ON COLUMN pgzc_eval_criterion_set.status IS '状态(DRAFT草稿/PUBLISHED发布/DISABLED禁用)';
COMMENT ON COLUMN pgzc_eval_criterion_set.deleted IS '逻辑删除(0否1是)';

-- ========== 2. 评估准则明细表升级 ==========
-- 增加一票否决相关字段
ALTER TABLE pgzc_eval_criterion ADD COLUMN IF NOT EXISTS is_mandatory INT DEFAULT 0;
ALTER TABLE pgzc_eval_criterion ADD COLUMN IF NOT EXISTS veto_action VARCHAR(50) DEFAULT NULL;

COMMENT ON COLUMN pgzc_eval_criterion.is_mandatory IS '是否是一票否决/强制约束指标（0-否，1-是）';
COMMENT ON COLUMN pgzc_eval_criterion.veto_action IS '否决动作（DIRECT_ZERO子项清零/DIRECT_FAIL强行不合格/DOWNGRADE降级）';

-- 建立索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_eval_criterion_set ON pgzc_eval_criterion (set_id);
CREATE INDEX IF NOT EXISTS idx_eval_criterion_indicator ON pgzc_eval_criterion (indicator_id);
