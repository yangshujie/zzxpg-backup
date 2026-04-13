-- =============================================================================
-- 这段脚本在做什么（为什么跑完应用就不报「主键重复」了）
-- =============================================================================
-- 初始化 SQL 里用 INSERT … VALUES (固定 id) 往 xxl_* 表里灌了数据（例如 id 1～136）。
-- Kingbase/PostgreSQL 的 IDENTITY（自增）背后有一个「发号器」。灌数据时虽然写了 id，
-- 发号器常常**不会**自动跳到 max(id)，仍会从较小的数接着发。
-- 应用里 INSERT 不写 id，由库发号；若发号器仍发到「表里已经占用的 id」（例如 295），
-- 就会：duplicate key violates unique constraint "xxl_job_log_report_pkey"。
-- 本脚本把每张表的发号器拨到：下一条新行的 id = 当前表里 MAX(id) + 1。
--
-- 请先改下面 sch 为你的 schema（与 JDBC 里 SET search_path 一致，一般是 ryjob）。
-- =============================================================================

DO $$
DECLARE
  sch text := 'ryjob';  -- <<<<<<<<<< 按实际 schema 修改
  tbl text;
  m   bigint;
  tbls text[] := ARRAY[
    'xxl_job_group',
    'xxl_job_info',
    'xxl_job_log',
    'xxl_job_logglue',
    'xxl_job_log_report',
    'xxl_job_registry',
    'xxl_job_user'
  ];
BEGIN
  FOREACH tbl IN ARRAY tbls
  LOOP
    EXECUTE format('SELECT COALESCE(MAX(id), 0::bigint) FROM %I.%I', sch, tbl) INTO m;
    EXECUTE format('ALTER TABLE %I.%I ALTER COLUMN id RESTART WITH %s', sch, tbl, m + 1);
  END LOOP;
END $$;
