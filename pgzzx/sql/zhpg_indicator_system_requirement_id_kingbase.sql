-- 评估指标体系：关联需求分析侧需求 ID（receiveRefinedFromRequirement 等写入）
ALTER TABLE public.pgzc_indicator_system ADD COLUMN IF NOT EXISTS requirement_id BIGINT DEFAULT NULL;
COMMENT ON COLUMN public.pgzc_indicator_system.requirement_id IS '关联需求ID（外部需求分析等系统下发时传入）';
CREATE INDEX IF NOT EXISTS idx_pgzc_is_requirement_id ON public.pgzc_indicator_system (requirement_id);
