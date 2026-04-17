-- ============================================================
-- ZHPG 指标体系库迁移脚本 (Kingbase/PostgreSQL)
-- 任务：将 pgzc_indicator_system 表中 JSON 字段内的中文装备/分中心标签替换为字典键
-- 目标字段：indicator_tree, refined_indicator_tree
-- ============================================================

-- 建议开启事务
BEGIN;

-- 1. 迁移 indicator_tree 字段中的 indicatorType 和 source
UPDATE public.pgzc_indicator_system
SET indicator_tree = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        indicator_tree::TEXT,
        '"indicatorType":"航天侦察"', '"indicatorType":"space_recon"'),
        '"indicatorType":"太空态势感知"', '"indicatorType":"space_domain_awareness"'),
        '"indicatorType":"太空攻防"', '"indicatorType":"space_defense"'),
        '"indicatorType":"航天测运控"', '"indicatorType":"space_track_control"'),
        '"indicatorType":"航天发射"', '"indicatorType":"space_launch"'),
        '"indicatorType":"海基航天"', '"indicatorType":"sea_based_space"'),
        '"source":"航天侦察"', '"source":"space_recon"'),
        '"source":"太空态势感知"', '"source":"space_domain_awareness"'),
        '"source":"太空攻防"', '"source":"space_defense"'),
        '"source":"航天测运控"', '"source":"space_track_control"'),
        '"source":"航天发射"', '"source":"space_launch"'),
        '"source":"海基航天"', '"source":"sea_based_space"')
WHERE indicator_tree IS NOT NULL AND indicator_tree <> '';

-- 2. 迁移 refined_indicator_tree 字段中的 indicatorType 和 source (回传细化树)
UPDATE public.pgzc_indicator_system
SET refined_indicator_tree = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        refined_indicator_tree::TEXT,
        '"indicatorType":"航天侦察"', '"indicatorType":"space_recon"'),
        '"indicatorType":"太空态势感知"', '"indicatorType":"space_domain_awareness"'),
        '"indicatorType":"太空攻防"', '"indicatorType":"space_defense"'),
        '"indicatorType":"航天测运控"', '"indicatorType":"space_track_control"'),
        '"indicatorType":"航天发射"', '"indicatorType":"space_launch"'),
        '"indicatorType":"海基航天"', '"indicatorType":"sea_based_space"'),
        '"source":"航天侦察"', '"source":"space_recon"'),
        '"source":"太空态势感知"', '"source":"space_domain_awareness"'),
        '"source":"太空攻防"', '"source":"space_defense"'),
        '"source":"航天测运控"', '"source":"space_track_control"'),
        '"source":"航天发射"', '"source":"space_launch"'),
        '"source":"海基航天"', '"source":"sea_based_space"')
WHERE refined_indicator_tree IS NOT NULL AND refined_indicator_tree <> '';

-- 提交事务
COMMIT;

-- ============================================================
-- 核对脚本：
-- SELECT id, indicator_tree FROM pgzc_indicator_system WHERE indicator_tree LIKE '%航天侦察%';
-- 预期结果：0行
-- ============================================================
