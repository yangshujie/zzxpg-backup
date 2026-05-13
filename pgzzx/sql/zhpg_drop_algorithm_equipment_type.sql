-- ============================================================
-- 评估算法表结构调整：移除装备类型分类
-- ============================================================

-- 移除 pgzc_algorithm 表中的 equipment_type 列
ALTER TABLE pgzc_algorithm DROP COLUMN equipment_type;
