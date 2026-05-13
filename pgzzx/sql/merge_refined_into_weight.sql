-- 1. 将回传细化树的数据迁移到结果/权重树字段（仅在权重树为空时迁移，避免覆盖已有权重结果）
UPDATE pgzc_indicator_system 
SET indicator_tree_weight = refined_indicator_tree 
WHERE refined_indicator_tree IS NOT NULL 
  AND (indicator_tree_weight IS NULL OR TRIM(indicator_tree_weight) = '');

-- 2. 删除旧的回传细化树字段
ALTER TABLE pgzc_indicator_system DROP COLUMN refined_indicator_tree;
