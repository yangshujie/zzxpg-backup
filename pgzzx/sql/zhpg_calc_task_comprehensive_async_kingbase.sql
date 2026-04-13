-- 综合计算经 XXL-JOB 异步回写时使用（ZhpgCalcExecutor 写回，ZHPG 轮询）
ALTER TABLE pgzc_calc_task ADD COLUMN IF NOT EXISTS comprehensive_async_state VARCHAR(32);
ALTER TABLE pgzc_calc_task ADD COLUMN IF NOT EXISTS comprehensive_async_json TEXT;

COMMENT ON COLUMN pgzc_calc_task.comprehensive_async_state IS '综合计算异步状态 PENDING/SUCCESS/FAIL，空表示未走XXL异步';
COMMENT ON COLUMN pgzc_calc_task.comprehensive_async_json IS 'SUCCESS时为综合计算结果JSON；FAIL时为错误信息';
