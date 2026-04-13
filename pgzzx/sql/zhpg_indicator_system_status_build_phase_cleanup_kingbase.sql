-- =============================================================================
-- 评估指标体系表 pgzc_indicator_system：与会签下线、构建阶段两态、工作模式对齐
-- 适用：人大金仓 / PostgreSQL 风格（COMMENT ON COLUMN、DROP COLUMN IF EXISTS）
-- 执行前请备份；建议在业务低峰执行。
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1) 构建阶段数据归一（业务仅保留：初始粗建 INITIAL_DRAFT、已回传细化 REFINED）
-- -----------------------------------------------------------------------------
-- 历史「已定稿」并入「已回传细化」
UPDATE public.pgzc_indicator_system
SET build_phase = 'REFINED'
WHERE build_phase = 'FINALIZED';

-- 历史 SUBMITTED 并入「初始粗建」
UPDATE public.pgzc_indicator_system
SET build_phase = 'INITIAL_DRAFT'
WHERE build_phase = 'SUBMITTED';

-- 仅主分协同保留 build_phase；其余工作模式一律清空
UPDATE public.pgzc_indicator_system
SET build_phase = NULL
WHERE build_phase IS NOT NULL
  AND (
    work_mode IS NULL
    OR TRIM(work_mode) = ''
    OR work_mode NOT IN ('主分协同', 'MAIN_BRANCH_COLLAB')
  );

-- -----------------------------------------------------------------------------
-- 2) 会签字段：直接删除列（无则跳过）
-- -----------------------------------------------------------------------------
ALTER TABLE public.pgzc_indicator_system
  DROP COLUMN IF EXISTS cosign_status;

-- -----------------------------------------------------------------------------
-- 3) 列注释与当前语义对齐（is_applied / status / build_phase）
-- -----------------------------------------------------------------------------
COMMENT ON COLUMN public.pgzc_indicator_system.is_applied IS
  '是否启用：0=停用 1=启用';

COMMENT ON COLUMN public.pgzc_indicator_system.status IS
  '发布状态：DRAFT=草稿 PUBLISHED=已发布（与是否启用 is_applied 独立；启用流程依赖已发布）';

COMMENT ON COLUMN public.pgzc_indicator_system.work_mode IS
  '工作模式：主分协同 / 内部流转（或历史英文 MAIN_BRANCH_COLLAB / INTERNAL_CIRCULATION）';

COMMENT ON COLUMN public.pgzc_indicator_system.build_phase IS
  '构建阶段，仅主分协同有意义：INITIAL_DRAFT=初始粗建，REFINED=已回传细化；内部流转等应为 NULL';

COMMIT;

-- =============================================================================
-- 验证用查询（可选执行）
-- =============================================================================
-- SELECT work_mode, build_phase, COUNT(*) FROM public.pgzc_indicator_system WHERE del_flag = '0' GROUP BY 1, 2 ORDER BY 1, 2;
-- SELECT COUNT(*) FROM public.pgzc_indicator_system WHERE build_phase IS NOT NULL AND work_mode NOT IN ('主分协同', 'MAIN_BRANCH_COLLAB');
