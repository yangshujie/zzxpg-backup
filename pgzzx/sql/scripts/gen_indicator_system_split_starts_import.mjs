/**
 * 从 indicator_system_with_source_filters_split_starts.json 生成 pgzc_indicator_system 导入 SQL
 * 用法: node gen_indicator_system_split_starts_import.mjs
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const jsonPath = path.resolve(
  __dirname,
  '../../../通信对抗场景设计包_20260331/indicator_system_with_source_filters_split_starts.json'
)
const outPath = path.resolve(__dirname, '../zhpg_import_indicator_system_split_starts_kingbase.sql')

function escSql(str) {
  return String(str).replace(/'/g, "''")
}

let jsonText = fs.readFileSync(jsonPath, 'utf8')
if (jsonText.charCodeAt(0) === 0xfeff) jsonText = jsonText.slice(1)
const raw = JSON.parse(jsonText)
const root = raw.treeData
if (!root || !root.label) {
  throw new Error('JSON 缺少 treeData 或 label')
}

const indicatorTreeJson = JSON.stringify(raw)
const systemName = `${root.label}（source_filters·split_starts）`

const sql = `-- =============================================================================
-- 指标体系导入：indicator_system_with_source_filters_split_starts.json
-- 对齐当前设计：无会签；主分协同；build_phase=REFINED（已回传细化，整树含 computeRule）
-- 执行前请确认已执行 zhpg_indicator_system_schema_latest_kingbase.sql（或表结构已含扩展列）
-- schema 可按需将 public 改为实际 schema
-- =============================================================================

DELETE FROM public.pgzc_indicator_system WHERE system_name = '${escSql(systemName)}';

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
    '${escSql(systemName)}',
    '${escSql(root.workMode || '主分协同')}',
    '${escSql(indicatorTreeJson)}',
    NULL,
    'EQUAL',
    NULL,
    '{"method":"SERIES","params":{}}',
    NULL,
    '由 indicator_system_with_source_filters_split_starts.json 生成：多 start 节点 + source_filters；入库后可在「指标体系管理」编辑、权重计算与启用。',
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
`

fs.writeFileSync(outPath, sql, 'utf8')
console.log('written:', outPath, 'tree chars:', indicatorTreeJson.length)
