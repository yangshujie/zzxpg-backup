-- ============================================================
-- ZHPG 指标管理 · 合并指标 / 指标模板表（人大金仓 Kingbase）
--
-- 目标：
--   1) 在 pgzc_indicator 上加 is_template / is_applied 两列
--   2) 把 pgzc_indicator_template 中的模板数据迁回 pgzc_indicator（is_template=1）
--   3) 用 is_applied 替代旧的 status（DRAFT=0、PUBLISHED=1）
--   4) 删除已无用列：status、weight、system_type（一能三性）
--   5) DROP TABLE pgzc_indicator_template
--
-- 幂等性：DDL 全部带 IF EXISTS / IF NOT EXISTS；INSERT 自带 NOT EXISTS 守门
-- 执行前请停掉 ZHPG 服务（防止旧版本写入 status/work_mode 等已删字段）
--
-- 执行前 / 出错排查：
--   先看 pgzc_indicator 是否有脏 id_code（如 'true'、空串等历史残留）：
--     SELECT id, indicator_name, id_code FROM pgzc_indicator
--       WHERE id_code IS NULL OR id_code IN ('','true','false','t','f');
--   有则手工清理（UPDATE 给个唯一编码 / DELETE）后再跑本脚本
-- ============================================================

BEGIN;

DO $$
BEGIN
    IF to_regclass('public.pgzc_indicator') IS NULL THEN
        RAISE EXCEPTION '表 public.pgzc_indicator 不存在';
    END IF;
END $$;

-- =================================================================
-- 1) 加列：is_template / is_applied
-- =================================================================
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS is_template SMALLINT DEFAULT 0;
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS is_applied  SMALLINT DEFAULT 0;

COMMENT ON COLUMN public.pgzc_indicator.is_template IS '是否模板：0=指标实例 1=指标模板';
COMMENT ON COLUMN public.pgzc_indicator.is_applied  IS '是否启用：0=未启用 1=已启用';

UPDATE public.pgzc_indicator SET is_template = 0 WHERE is_template IS NULL;
UPDATE public.pgzc_indicator SET is_applied  = 0 WHERE is_applied  IS NULL;

-- 旧 status 迁移到 is_applied（PUBLISHED → 1，其他 → 0）
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='pgzc_indicator' AND column_name='status'
    ) THEN
        UPDATE public.pgzc_indicator
        SET is_applied = 1
        WHERE status = 'PUBLISHED' AND COALESCE(is_applied, 0) = 0;
    END IF;
END $$;

-- =================================================================
-- 2) 迁移 pgzc_indicator_template → pgzc_indicator (is_template=1)
--    id_code 统一为 'tpl_id_<原模板id>'，绝对唯一可追溯
--    INSERT 自带 NOT EXISTS 守门：
--      a) 同名模板已迁过 → 跳过
--      b) 新生成的 id_code 已存在（如重跑、脏数据撞库）→ 跳过
-- =================================================================
DO $$
BEGIN
    IF to_regclass('public.pgzc_indicator_template') IS NOT NULL THEN
        INSERT INTO public.pgzc_indicator (
            id_code, parent_id, indicator_name, indicator_type,
            unit, value_min, value_max, value_category, calc_method,
            algorithm_id, indicator_level, order_num, description,
            is_bottom_node, is_template, is_applied,
            create_by, create_time, update_by, update_time, remark, del_flag
        )
        SELECT
            'tpl_id_' || t.id::text                                        AS id_code,
            0                                                              AS parent_id,
            t.template_name                                                AS indicator_name,
            t.equipment_type                                               AS indicator_type,
            NULL, NULL, NULL, NULL,
            CASE
                WHEN t.config_json IS NULL OR t.config_json = '' THEN NULL
                ELSE t.config_json
            END                                                            AS calc_method,
            NULL,
            1, 0,
            t.description,
            CASE
                WHEN t.config_json IS NOT NULL
                     AND t.config_json LIKE '%"nodeRole":"LEAF"%'
                    THEN 1::smallint
                ELSE 0::smallint
            END                                                            AS is_bottom_node,
            1                                                              AS is_template,
            CASE WHEN t.status = 'PUBLISHED' THEN 1 ELSE 0 END             AS is_applied,
            t.create_by, t.create_time, t.update_by, t.update_time, t.remark,
            COALESCE(t.del_flag, '0')                                      AS del_flag
        FROM public.pgzc_indicator_template t
        WHERE COALESCE(t.del_flag, '0') = '0'
          -- 同名模板已落到 pgzc_indicator（前次迁移）则跳过
          AND NOT EXISTS (
              SELECT 1 FROM public.pgzc_indicator i
              WHERE i.is_template = 1 AND i.indicator_name = t.template_name
          )
          -- id_code 撞已有数据则跳过（防御历史脏数据 / 重跑）
          AND NOT EXISTS (
              SELECT 1 FROM public.pgzc_indicator i2
              WHERE i2.id_code = 'tpl_id_' || t.id::text
          );
    END IF;
END $$;

-- =================================================================
-- 3) 删除已无用列
-- =================================================================
ALTER TABLE public.pgzc_indicator DROP COLUMN IF EXISTS status;
ALTER TABLE public.pgzc_indicator DROP COLUMN IF EXISTS weight;
ALTER TABLE public.pgzc_indicator DROP COLUMN IF EXISTS system_type;

-- 4) 索引
CREATE INDEX IF NOT EXISTS idx_pgzc_indicator_is_template ON public.pgzc_indicator (is_template);
CREATE INDEX IF NOT EXISTS idx_pgzc_indicator_is_applied  ON public.pgzc_indicator (is_applied);

-- =================================================================
-- 5) 干掉旧的 pgzc_indicator_template
-- =================================================================
DROP TABLE IF EXISTS public.pgzc_indicator_template;

COMMIT;

-- ============================================================
-- 校验：
--   SELECT is_template, COUNT(*) FROM pgzc_indicator GROUP BY is_template;
--   SELECT is_applied,  COUNT(*) FROM pgzc_indicator GROUP BY is_applied;
--   SELECT id, indicator_name, id_code FROM pgzc_indicator WHERE is_template = 1 ORDER BY id;
--   SELECT to_regclass('public.pgzc_indicator_template'); -- 期望 NULL
-- ============================================================
