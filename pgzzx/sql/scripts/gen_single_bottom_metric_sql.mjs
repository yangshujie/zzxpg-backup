import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const jsonPath = path.resolve(__dirname, '../../../通信对抗场景设计包_20260331/indicator_system_single_bottom_metric.json')
const outPath = path.resolve(__dirname, '../zhpg_import_indicator_system_single_bottom_metric_kingbase.sql')

const j = JSON.parse(fs.readFileSync(jsonPath, 'utf8'))
const compact = JSON.stringify(j)
const esc = (s) => String(s).replace(/'/g, "''")

const systemName = '单底层指标测试体系（单叶示例）'

const sql = `-- =============================================================================
-- 指标体系导入：indicator_system_single_bottom_metric.json
-- 仅 1 个底层叶指标（时间占空比）；根下直接挂叶
-- 执行前请确认已执行 zhpg_indicator_system_schema_latest_kingbase.sql
-- schema 可按需将 public 改为实际 schema
-- =============================================================================

DELETE FROM public.pgzc_indicator_system WHERE system_name = '${esc(systemName)}';

INSERT INTO public.pgzc_indicator_system (
    system_name,
    work_mode,
    indicator_tree,
    indicator_tree_weight,
    weight_assign_algorithm,
    weight_assign_params,
    conduction_config,
    template_id,
    description,
    status,
    is_applied,
    build_phase,
    source_subsystem,
    refined_time,
    del_flag,
    create_by,
    create_time,
    update_by,
    update_time,
    remark
) VALUES (
    '${esc(systemName)}',
    '主分协同',
    '${esc(compact)}',
    NULL,
    'EQUAL',
    NULL,
    '{"method":"SERIES","params":{}}',
    NULL,
    '由 indicator_system_single_bottom_metric.json 生成；单底层指标 SQL 入库示例。',
    'DRAFT',
    0,
    'REFINED',
    NULL,
    NULL,
    '0',
    'admin',
    NOW(),
    'admin',
    NOW(),
    NULL
);

-- 若库表已含 id_code 且需与树根 id 一致，可执行：
-- UPDATE public.pgzc_indicator_system SET id_code = '0_1775999000001' WHERE system_name = '${esc(systemName)}';
`

fs.writeFileSync(outPath, sql, 'utf8')
console.log('Wrote', outPath, 'tree len', compact.length)
