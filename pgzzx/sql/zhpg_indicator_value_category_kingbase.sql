-- 指标：指标值类型（门限范围沿用 value_min / value_max）
ALTER TABLE pgzc_indicator ADD COLUMN IF NOT EXISTS value_category VARCHAR(32);
COMMENT ON COLUMN pgzc_indicator.value_category IS '指标值类型：COST成本型 BENEFIT效益型 INTERVAL_BENEFIT区间效益型';
