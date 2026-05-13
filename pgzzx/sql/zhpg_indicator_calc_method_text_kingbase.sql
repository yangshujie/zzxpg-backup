-- ============================================================
-- ZHPG 指标管理 · calc_method 字段长度修复（人大金仓 Kingbase）
--
-- 背景：
--   底层指标的图形化计算规则会保存为完整 JSON，单个规则常超过 1000 字符。
--   如果历史库中的 pgzc_indicator.calc_method 仍是 VARCHAR/短文本，
--   可能出现保存后 JSON 被截断、指标管理无法回显算法画布的问题。
-- ============================================================

BEGIN;

ALTER TABLE public.pgzc_indicator
  ALTER COLUMN calc_method TYPE TEXT USING calc_method::TEXT;

COMMENT ON COLUMN public.pgzc_indicator.calc_method
  IS '计算方法 JSON（仅底层指标持有，TEXT，避免图形化算法配置被截断）';

COMMIT;

-- 校验：
-- SELECT data_type, character_maximum_length
-- FROM information_schema.columns
-- WHERE table_schema = 'public'
--   AND table_name = 'pgzc_indicator'
--   AND column_name = 'calc_method';
