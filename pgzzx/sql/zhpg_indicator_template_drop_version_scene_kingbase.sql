-- ============================================================
-- ZHPG 指标模板表：删除 version、scene_type、template_type（人大金仓 Kingbase）
--
-- 若报错：relation "public.pgzc_indicator_template" does not exist (42P01)
--   说明当前连接下没有「public + 该表名」这张表，请先做完下面「第 0 步」再改 ALTER 里的 schema。
-- ============================================================

-- ========== 第 0 步：先单独执行下面查询，看表在哪个 schema ==========
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_name = 'pgzc_indicator_template';

-- 若无结果，再试模糊匹配：
-- SELECT schemaname, tablename FROM pg_tables WHERE tablename ILIKE '%indicator_template%';

-- 把下面两条 ALTER 里的 public 换成上一步查到的 table_schema（常见不是 public 而是业务库名）
-- ============================================================

ALTER TABLE public.pgzc_indicator_template DROP COLUMN IF EXISTS scene_type;

ALTER TABLE public.pgzc_indicator_template DROP COLUMN IF EXISTS version;

-- 模板类型已下线（与指标类型 equipment_type 合并为业务侧单一维度），实体与查询不再映射该列
ALTER TABLE public.pgzc_indicator_template DROP COLUMN IF EXISTS template_type;

-- ------------------------------------------------------------
-- 执行说明：
--   1) 一次只执行一条 ALTER；若 schema 不是 public，请先改表名前缀。
--   2) 若第 0 步查不到任何表：说明库未建过 ZHPG 表，需先执行项目里的建表脚本创建 pgzc_indicator_template，
--      再决定是否还需要本脚本（新库若建表语句已不含这两列，则无需执行本脚本）。
-- ------------------------------------------------------------
-- 不支持 IF EXISTS 时可改用（列已删会报错，可忽略）：
-- ALTER TABLE public.pgzc_indicator_template DROP COLUMN scene_type;
-- ALTER TABLE public.pgzc_indicator_template DROP COLUMN version;
-- ALTER TABLE public.pgzc_indicator_template DROP COLUMN template_type;
-- ------------------------------------------------------------
-- 可选回滚：
-- ALTER TABLE public.pgzc_indicator_template ADD COLUMN scene_type VARCHAR(32);
-- ALTER TABLE public.pgzc_indicator_template ADD COLUMN version VARCHAR(32) DEFAULT '1.0';
-- ALTER TABLE public.pgzc_indicator_template ADD COLUMN template_type VARCHAR(32);
