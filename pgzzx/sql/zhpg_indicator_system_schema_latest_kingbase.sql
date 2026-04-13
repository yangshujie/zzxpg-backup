-- =============================================================================
-- pgzc_indicator_system 最新库表结构对齐（人大金仓 / PostgreSQL 兼容写法）
-- 仅 DDL：删会签字段、补全业务列、注释与索引。不修改业务数据。
-- 请将 schema 名 public 按需改为实际 schema。
-- =============================================================================

BEGIN;

-- 指标体系行级类型列已废弃（由指标树 JSON 承载）
DROP INDEX IF EXISTS public.idx_pgzc_is_system_type;
ALTER TABLE public.pgzc_indicator_system DROP COLUMN IF EXISTS system_type;
ALTER TABLE public.pgzc_indicator_system DROP COLUMN IF EXISTS equipment_type;

-- 会签已下线：列不存在时跳过
ALTER TABLE public.pgzc_indicator_system DROP COLUMN IF EXISTS cosign_status;

-- 传导与主分协同扩展列（已存在则跳过）
ALTER TABLE public.pgzc_indicator_system ADD COLUMN IF NOT EXISTS conduction_config TEXT;
COMMENT ON COLUMN public.pgzc_indicator_system.conduction_config IS
  '默认/全局传导算法配置 JSON（name/k/mainUnit 等）';

ALTER TABLE public.pgzc_indicator_system ADD COLUMN IF NOT EXISTS build_phase VARCHAR(32) DEFAULT NULL;
COMMENT ON COLUMN public.pgzc_indicator_system.build_phase IS
  '构建阶段，仅主分协同：INITIAL_DRAFT=初始粗建，REFINED=已回传细化；内部流转等应为 NULL';

ALTER TABLE public.pgzc_indicator_system ADD COLUMN IF NOT EXISTS source_subsystem VARCHAR(128) DEFAULT NULL;
COMMENT ON COLUMN public.pgzc_indicator_system.source_subsystem IS
  '回传来源分系统标识';

ALTER TABLE public.pgzc_indicator_system ADD COLUMN IF NOT EXISTS refined_time TIMESTAMP DEFAULT NULL;
COMMENT ON COLUMN public.pgzc_indicator_system.refined_time IS
  '分系统回传细化时间';

-- 对比基准列已废弃：对比使用 indicator_tree 与 refined_indicator_tree
ALTER TABLE public.pgzc_indicator_system DROP COLUMN IF EXISTS initial_tree;

-- 核心列注释（与当前应用一致）
COMMENT ON COLUMN public.pgzc_indicator_system.is_applied IS
  '是否启用：0=停用 1=启用';

COMMENT ON COLUMN public.pgzc_indicator_system.status IS
  '发布状态：DRAFT=草稿 PUBLISHED=已发布（与 is_applied 独立）';

COMMENT ON COLUMN public.pgzc_indicator_system.work_mode IS
  '工作模式：主分协同 / 内部流转（或历史英文 MAIN_BRANCH_COLLAB / INTERNAL_CIRCULATION）';

COMMENT ON TABLE public.pgzc_indicator_system IS '评估指标体系表';

CREATE INDEX IF NOT EXISTS idx_pgzc_is_build_phase ON public.pgzc_indicator_system (build_phase);

ALTER TABLE public.pgzc_indicator_system ADD COLUMN IF NOT EXISTS requirement_id BIGINT DEFAULT NULL;
COMMENT ON COLUMN public.pgzc_indicator_system.requirement_id IS
  '关联需求ID（外部需求分析等系统下发时传入）';
CREATE INDEX IF NOT EXISTS idx_pgzc_is_requirement_id ON public.pgzc_indicator_system (requirement_id);

COMMIT;
