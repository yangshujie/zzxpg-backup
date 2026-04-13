-- 删除评估指标体系表行级「指标集类型 / 装备类型」（类型信息由指标树 JSON 节点等承载）
-- 执行顺序建议：在应用升级到无这两列的实体之后执行；新建库请直接用 zhpg_indicator_system_init_kingbase.sql 最新版。

DROP INDEX IF EXISTS public.idx_pgzc_is_system_type;

ALTER TABLE public.pgzc_indicator_system DROP COLUMN IF EXISTS system_type;
ALTER TABLE public.pgzc_indicator_system DROP COLUMN IF EXISTS equipment_type;
