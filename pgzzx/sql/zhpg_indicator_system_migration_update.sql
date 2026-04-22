-- ============================================================
-- ZHPG 指标体系 JSON 字段装备类型/数据源编码迁移
-- 目标：将 pgzc_indicator_system 中 indicatorType/source 的中文值替换为字典键值
-- 涉及字段：indicator_tree / refined_indicator_tree / indicator_tree_weight
-- 适用数据库：Kingbase / PostgreSQL
-- ============================================================

BEGIN;

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
WHERE indicator_tree IS NOT NULL
  AND indicator_tree <> '';

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
WHERE refined_indicator_tree IS NOT NULL
  AND refined_indicator_tree <> '';

UPDATE public.pgzc_indicator_system
SET indicator_tree_weight = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        indicator_tree_weight::TEXT,
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
WHERE indicator_tree_weight IS NOT NULL
  AND indicator_tree_weight <> '';

COMMIT;

-- 核对示例：
-- SELECT id
-- FROM public.pgzc_indicator_system
-- WHERE indicator_tree LIKE '%太空攻防%'
--    OR refined_indicator_tree LIKE '%太空攻防%'
--    OR indicator_tree_weight LIKE '%太空攻防%';
