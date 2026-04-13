-- =============================================================================
-- 修复 XXL-JOB 在 Kingbase/PostgreSQL 下无法获取自增 ID 的问题
-- =============================================================================
-- 问题：useGeneratedKeys 在 Kingbase 下不生效，导致 logId=0
-- 解决：创建序列并修改表结构使用序列
-- =============================================================================

-- 1. 创建序列（如果不存在）
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'xxl_job_log_id_seq') THEN
        CREATE SEQUENCE xxl_job_log_id_seq START 1;
    END IF;
END $$;

-- 2. 修改表结构使用序列（如果当前是 IDENTITY 或 SERIAL）
-- 先检查当前列的默认值
SELECT column_name, column_default, is_identity 
FROM information_schema.columns 
WHERE table_name = 'xxl_job_log' AND column_name = 'id';

-- 3. 如果上面查询显示没有使用序列，执行以下命令设置默认值
-- ALTER TABLE xxl_job_log ALTER COLUMN id SET DEFAULT nextval('xxl_job_log_id_seq');

-- 4. 同步序列当前值到最大ID（避免主键冲突）
SELECT setval('xxl_job_log_id_seq', COALESCE((SELECT MAX(id) FROM xxl_job_log), 0) + 1);
