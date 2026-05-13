-- =============================================================================
-- 移除指标体系 ID 编码的唯一约束，支持多次回传细化生成多条记录
-- =============================================================================

BEGIN;

-- 1. 移除旧的唯一索引
DROP INDEX IF EXISTS public.uq_pgzc_indicator_system_id_code;

-- 2. 创建普通索引以保证查询效率
CREATE INDEX IF NOT EXISTS idx_pgzc_indicator_system_id_code
  ON public.pgzc_indicator_system (id_code) WHERE id_code IS NOT NULL AND id_code <> '';

-- 3. 增加 requirement_id 唯一性约束（同一个需求只保留一个体系）
CREATE UNIQUE INDEX IF NOT EXISTS uq_pgzc_is_requirement_id 
  ON public.pgzc_indicator_system (requirement_id) 
  WHERE requirement_id IS NOT NULL;

-- 4. 移除行级冗余的权重分配算法字段（已迁移至指标树节点内部管理）
ALTER TABLE public.pgzc_indicator_system DROP COLUMN IF EXISTS weight_assign_algorithm;
ALTER TABLE public.pgzc_indicator_system DROP COLUMN IF EXISTS weight_assign_params;

COMMIT;
