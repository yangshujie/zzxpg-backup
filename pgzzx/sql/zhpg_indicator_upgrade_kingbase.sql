-- ============================================================
-- ZHPG 指标管理增量升级脚本（人大金仓 Kingbase）
-- 目标：
-- 1) 指标类型切换为 6 类装备分类（字段长度扩展 + 历史值兼容映射）
-- 2) 父级/底层指标关系重构（新增 is_bottom_node 并回填）
-- 3) 层级 indicator_level 数据回填
-- 说明：indicator_code 列已废弃时可从表中删除，本脚本不再引用该列
-- ============================================================

BEGIN;

DO $$
BEGIN
    IF to_regclass('public.pgzc_indicator') IS NULL THEN
        RAISE EXCEPTION '表 public.pgzc_indicator 不存在，无法执行升级脚本';
    END IF;
END $$;

-- 1) 扩展指标类型字段长度（兼容新类型编码）
ALTER TABLE public.pgzc_indicator
    ALTER COLUMN indicator_type TYPE VARCHAR(64);

COMMENT ON COLUMN public.pgzc_indicator.indicator_type IS
'指标类型（SPACE_RECON/SPACE_SITUATIONAL_AWARENESS/SPACE_OFFENSE_DEFENSE/SPACE_TTC/SPACE_LAUNCH/SEA_BASED_SPACE）';

-- 2) 新增底层节点标记（Kingbase/PG 使用 SMALLINT 兼容布尔语义）
ALTER TABLE public.pgzc_indicator
    ADD COLUMN IF NOT EXISTS is_bottom_node SMALLINT DEFAULT 1;

COMMENT ON COLUMN public.pgzc_indicator.is_bottom_node IS
'是否底层指标（1=底层叶子节点，0=中间/根节点）';

-- 防御性修复：parent_id 空值统一为 0（根节点）
UPDATE public.pgzc_indicator
SET parent_id = 0
WHERE parent_id IS NULL;

-- 3) 回填底层节点标记：有子节点=0，无子节点=1
UPDATE public.pgzc_indicator p
SET is_bottom_node = CASE
    WHEN EXISTS (
        SELECT 1
        FROM public.pgzc_indicator c
        WHERE c.parent_id = p.id
          AND COALESCE(c.del_flag, '0') = '0'
    ) THEN 0
    ELSE 1
END;

-- 4) 历史指标类型映射到新的 6 类（可按业务再调）
UPDATE public.pgzc_indicator
SET indicator_type = CASE indicator_type
    WHEN 'COMBAT_EFFECTIVENESS' THEN 'SPACE_RECON'
    WHEN 'COMBAT_SUITABILITY' THEN 'SPACE_SITUATIONAL_AWARENESS'
    WHEN 'SYSTEM_SUITABILITY' THEN 'SPACE_OFFENSE_DEFENSE'
    WHEN 'SERVICE_SUITABILITY' THEN 'SPACE_TTC'
    WHEN 'CAPABILITY_GROWTH' THEN 'SPACE_LAUNCH'
    ELSE indicator_type
END
WHERE indicator_type IN (
    'COMBAT_EFFECTIVENESS',
    'COMBAT_SUITABILITY',
    'SYSTEM_SUITABILITY',
    'SERVICE_SUITABILITY',
    'CAPABILITY_GROWTH'
);

-- 5) 回填层级 indicator_level（根=1，子级递增）
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

UPDATE public.pgzc_indicator
SET indicator_level = 1
WHERE indicator_level IS NULL;

-- 6) 查询性能辅助索引
CREATE INDEX IF NOT EXISTS idx_pgzc_indicator_is_bottom_node
    ON public.pgzc_indicator (is_bottom_node);

COMMIT;

-- ============================================================
-- 执行后可人工核验：
-- SELECT indicator_type, COUNT(*) FROM public.pgzc_indicator GROUP BY indicator_type;
-- SELECT is_bottom_node, COUNT(*) FROM public.pgzc_indicator GROUP BY is_bottom_node;
-- ============================================================
