-- 若库表由旧版 ry-job-kingbase.sql 初始化，列名被误写为 glue_upTIMESTAMP，与 xxl-job-admin Mapper 不一致。
-- 在目标 schema（如 ryjob）下执行一次即可。
ALTER TABLE "xxl_job_info" RENAME COLUMN "glue_upTIMESTAMP" TO "glue_updatetime";
