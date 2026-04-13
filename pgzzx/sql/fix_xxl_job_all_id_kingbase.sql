-- =============================================================================
-- 修复 XXL-JOB 所有表在 Kingbase/PostgreSQL 下无法获取自增 ID 的问题
-- =============================================================================

-- 为所有需要自增ID的表创建序列并同步
DO $$
DECLARE
    tbl TEXT;
    seq TEXT;
    max_id BIGINT;
BEGIN
    FOREACH tbl IN ARRAY ARRAY['xxl_job_group', 'xxl_job_info', 'xxl_job_log', 
                                'xxl_job_logglue', 'xxl_job_log_report', 
                                'xxl_job_registry', 'xxl_job_user']
    LOOP
        seq := tbl || '_id_seq';
        
        -- 检查序列是否存在，不存在则创建
        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = seq) THEN
            EXECUTE format('CREATE SEQUENCE %I START 1', seq);
            RAISE NOTICE '创建序列: %', seq;
        END IF;
        
        -- 同步序列值到最大ID
        EXECUTE format('SELECT COALESCE(MAX(id), 0) FROM %I', tbl) INTO max_id;
        IF max_id > 0 THEN
            EXECUTE format('SELECT setval(%L, %s)', seq, max_id + 1);
            RAISE NOTICE '同步序列 % 到 %', seq, max_id + 1;
        END IF;
    END LOOP;
END $$;

-- 查看当前序列状态
SELECT 
    c.relname as sequence_name,
    pg_get_serial_sequence(t.tablename, 'id') as table_sequence,
    (SELECT last_value FROM pg_class pc JOIN pg_sequence ps ON pc.oid = ps.seqrelid 
     WHERE pc.relname = c.relname) as current_value
FROM pg_class c
JOIN pg_sequence s ON c.oid = s.seqrelid
WHERE c.relname LIKE 'xxl_job%'
ORDER BY c.relname;
