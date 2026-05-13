-- ============================================================
-- ZHPG 指标管理 · 表结构基线（人大金仓 Kingbase）
--
-- 适用：指标管理（前端「能力资源 → 指标管理」、后端 /zhpg/indicator）
-- 单表统一存放「指标」与「指标模板」，由 is_template 区分。
--
-- 幂等性：未建表则建，已建表通过 ADD COLUMN IF NOT EXISTS 补齐缺失列。
-- 已废弃列（work_mode / status / weight / system_type）保证一并删除。
-- ============================================================

BEGIN;

-- =================================================================
-- 1) pgzc_indicator —— 指标 + 指标模板共表
-- =================================================================
CREATE TABLE IF NOT EXISTS public.pgzc_indicator (
    id              BIGSERIAL    PRIMARY KEY,
    id_code         VARCHAR(64),
    parent_id       BIGINT       DEFAULT 0,
    indicator_name  VARCHAR(255) NOT NULL,
    indicator_type  VARCHAR(64),
    unit            VARCHAR(64),
    value_min       DECIMAL(20,4),
    value_max       DECIMAL(20,4),
    value_category  VARCHAR(32),
    calc_method     TEXT,
    algorithm_id    BIGINT,
    indicator_level INT          DEFAULT 1,
    order_num       INT          DEFAULT 0,
    description     TEXT,
    is_bottom_node  SMALLINT     DEFAULT 1,
    is_template     SMALLINT     DEFAULT 0,
    is_applied      SMALLINT     DEFAULT 0,
    create_by       VARCHAR(64),
    create_time     TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     TIMESTAMP,
    remark          VARCHAR(500),
    del_flag        VARCHAR(2)   DEFAULT '0'
);

-- 已存在表时，按需补齐列（迁移已生效环境）
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS id_code         VARCHAR(64);
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS indicator_type  VARCHAR(64);
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS unit            VARCHAR(64);
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS value_min       DECIMAL(20,4);
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS value_max       DECIMAL(20,4);
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS value_category  VARCHAR(32);
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS calc_method     TEXT;
ALTER TABLE public.pgzc_indicator ALTER COLUMN calc_method TYPE TEXT USING calc_method::TEXT;
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS algorithm_id    BIGINT;
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS indicator_level INT          DEFAULT 1;
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS order_num       INT          DEFAULT 0;
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS description     TEXT;
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS is_bottom_node  SMALLINT     DEFAULT 1;
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS is_template     SMALLINT     DEFAULT 0;
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS is_applied      SMALLINT     DEFAULT 0;
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS create_by       VARCHAR(64);
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS create_time     TIMESTAMP;
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS update_by       VARCHAR(64);
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS update_time     TIMESTAMP;
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS remark          VARCHAR(500);
ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS del_flag        VARCHAR(2)   DEFAULT '0';

-- 移除已废弃列
ALTER TABLE public.pgzc_indicator DROP COLUMN IF EXISTS work_mode;
ALTER TABLE public.pgzc_indicator DROP COLUMN IF EXISTS status;
ALTER TABLE public.pgzc_indicator DROP COLUMN IF EXISTS weight;
ALTER TABLE public.pgzc_indicator DROP COLUMN IF EXISTS system_type;

COMMENT ON TABLE  public.pgzc_indicator                  IS '评估指标（指标 + 指标模板共表，is_template 区分）';
COMMENT ON COLUMN public.pgzc_indicator.id_code          IS '全局ID编码（指标体系导入时与树节点 uid/id 一致）';
COMMENT ON COLUMN public.pgzc_indicator.parent_id        IS '父指标ID，0 为根';
COMMENT ON COLUMN public.pgzc_indicator.indicator_type   IS '装备类型：航天侦察/太空态势感知/太空攻防/航天测运控/航天发射/海基航天/无';
COMMENT ON COLUMN public.pgzc_indicator.value_category   IS '指标值类型：成本型 / 效益型 / 区间效益型';
COMMENT ON COLUMN public.pgzc_indicator.calc_method      IS '计算方法 JSON（仅底层指标持有）';
COMMENT ON COLUMN public.pgzc_indicator.indicator_level  IS '层级（根=1，子级递增）';
COMMENT ON COLUMN public.pgzc_indicator.is_bottom_node   IS '是否底层节点（1=底层，0=非底层）';
COMMENT ON COLUMN public.pgzc_indicator.is_template      IS '是否模板：0=指标实例 1=指标模板';
COMMENT ON COLUMN public.pgzc_indicator.is_applied       IS '是否启用：0=未启用 1=已启用';

CREATE INDEX IF NOT EXISTS idx_pgzc_indicator_parent_id      ON public.pgzc_indicator (parent_id);
CREATE INDEX IF NOT EXISTS idx_pgzc_indicator_indicator_type ON public.pgzc_indicator (indicator_type);
CREATE INDEX IF NOT EXISTS idx_pgzc_indicator_is_bottom_node ON public.pgzc_indicator (is_bottom_node);
CREATE INDEX IF NOT EXISTS idx_pgzc_indicator_is_template    ON public.pgzc_indicator (is_template);
CREATE INDEX IF NOT EXISTS idx_pgzc_indicator_is_applied     ON public.pgzc_indicator (is_applied);

-- =================================================================
-- 2) 数据规范化：parent_id 空值统一为 0；indicator_level 回填
-- =================================================================
UPDATE public.pgzc_indicator SET parent_id      = 0  WHERE parent_id      IS NULL;
UPDATE public.pgzc_indicator SET is_bottom_node = 1  WHERE is_bottom_node IS NULL;
UPDATE public.pgzc_indicator SET is_template    = 0  WHERE is_template    IS NULL;
UPDATE public.pgzc_indicator SET is_applied     = 0  WHERE is_applied     IS NULL;
UPDATE public.pgzc_indicator SET del_flag       = '0' WHERE del_flag      IS NULL;

-- 父子关系一致性：有子节点的回填为非底层
UPDATE public.pgzc_indicator p
SET is_bottom_node = 0
WHERE EXISTS (
    SELECT 1 FROM public.pgzc_indicator c
    WHERE c.parent_id = p.id AND COALESCE(c.del_flag, '0') = '0'
);

-- 重新回填 indicator_level
WITH RECURSIVE indicator_tree AS (
    SELECT id, parent_id, 1::BIGINT AS lvl
    FROM public.pgzc_indicator
    WHERE COALESCE(parent_id, 0) = 0
      AND COALESCE(del_flag, '0') = '0'
    UNION ALL
    SELECT c.id, c.parent_id, t.lvl + 1
    FROM public.pgzc_indicator c
    JOIN indicator_tree t ON c.parent_id = t.id
    WHERE COALESCE(c.del_flag, '0') = '0'
)
UPDATE public.pgzc_indicator p
SET indicator_level = t.lvl::INT
FROM indicator_tree t
WHERE p.id = t.id;

UPDATE public.pgzc_indicator SET indicator_level = 1 WHERE indicator_level IS NULL;

-- =================================================================
-- 3) 旧 pgzc_indicator_template：仅在「确认空表」时清理
--    若仍有数据，请先执行 zhpg_indicator_merge_template_kingbase.sql
--    把模板搬到 pgzc_indicator(is_template=1) 后再回到本脚本
-- =================================================================
DO $$
DECLARE
    legacy_count BIGINT;
BEGIN
    IF to_regclass('public.pgzc_indicator_template') IS NOT NULL THEN
        EXECUTE 'SELECT COUNT(*) FROM public.pgzc_indicator_template' INTO legacy_count;
        IF legacy_count = 0 THEN
            EXECUTE 'DROP TABLE public.pgzc_indicator_template';
        ELSE
            RAISE NOTICE '检测到 pgzc_indicator_template 仍有 % 条数据，未自动 DROP；请先跑 zhpg_indicator_merge_template_kingbase.sql', legacy_count;
        END IF;
    END IF;
END $$;

COMMIT;

-- ============================================================
-- 校验：
--   SELECT is_template, COUNT(*) FROM pgzc_indicator GROUP BY is_template;
--   SELECT is_applied,  COUNT(*) FROM pgzc_indicator GROUP BY is_applied;
--   SELECT column_name FROM information_schema.columns
--     WHERE table_schema='public' AND table_name='pgzc_indicator' ORDER BY ordinal_position;
--   SELECT to_regclass('public.pgzc_indicator_template'); -- 期望 NULL
-- ============================================================
