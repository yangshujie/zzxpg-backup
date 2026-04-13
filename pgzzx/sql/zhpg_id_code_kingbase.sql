-- 指标体系 / 指标库：全局 ID 编码（与指标树节点 id 对齐）
-- 人大金仓 / PostgreSQL 兼容

BEGIN;

ALTER TABLE public.pgzc_indicator_system ADD COLUMN IF NOT EXISTS id_code VARCHAR(128);
COMMENT ON COLUMN public.pgzc_indicator_system.id_code IS
  '全局唯一 ID 编码，与同套指标树根节点 JSON 的 id（或 uid）一致';

ALTER TABLE public.pgzc_indicator ADD COLUMN IF NOT EXISTS id_code VARCHAR(128);
COMMENT ON COLUMN public.pgzc_indicator.id_code IS
  '全局唯一 ID 编码；导入指标体系时树节点 id 与此一致';

CREATE UNIQUE INDEX IF NOT EXISTS uq_pgzc_indicator_system_id_code
  ON public.pgzc_indicator_system (id_code) WHERE id_code IS NOT NULL AND id_code <> '';

CREATE UNIQUE INDEX IF NOT EXISTS uq_pgzc_indicator_id_code
  ON public.pgzc_indicator (id_code) WHERE id_code IS NOT NULL AND id_code <> '';

COMMIT;
