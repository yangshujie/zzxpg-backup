import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const workspaceRoot = path.join(__dirname, '..', '..', '..')
const sourceJsonPath = path.join(workspaceRoot, 'indicator_system_with_source_filters.json')
const outA = path.join(__dirname, '..', 'zhpg_indicator_system_testdata_comm_countermeasure.sql')
const outB = path.join(__dirname, '..', 'zhpg_indicator_system_testdata_comm_countermeasure_with_source_filters.sql')

function normalizeWorkMode(v) {
  const s = String(v || '').trim()
  if (s === '主分协同' || s === 'MAIN_BRANCH_COLLAB') return 'MAIN_BRANCH_COLLAB'
  if (s === '内部流转' || s === 'INTERNAL_CIRCULATION') return 'INTERNAL_CIRCULATION'
  return 'INTERNAL_CIRCULATION'
}

function normalizeIndicatorType(v) {
  const s = String(v || '').trim()
  const map = {
    '航天侦察': 'SPACE_RECON',
    '太空态势感知': 'SPACE_SITUATIONAL_AWARENESS',
    '太空攻防': 'SPACE_OFFENSE_DEFENSE',
    '航天测运控': 'SPACE_TTC',
    '航天发射': 'SPACE_LAUNCH',
    '海基航天': 'SEA_BASED_SPACE',
    '聚合': 'SYSTEM_AGGREGATION',
    SPACE_RECON: 'SPACE_RECON',
    SPACE_SITUATIONAL_AWARENESS: 'SPACE_SITUATIONAL_AWARENESS',
    SPACE_OFFENSE_DEFENSE: 'SPACE_OFFENSE_DEFENSE',
    SPACE_TTC: 'SPACE_TTC',
    SPACE_LAUNCH: 'SPACE_LAUNCH',
    SEA_BASED_SPACE: 'SEA_BASED_SPACE',
    SYSTEM_AGGREGATION: 'SYSTEM_AGGREGATION'
  }
  return map[s] || s
}

function normalizeValueCategory(v) {
  const s = String(v || '').trim()
  const map = {
    成本型: 'COST',
    效益型: 'BENEFIT',
    区间效益型: 'INTERVAL_BENEFIT',
    COST: 'COST',
    BENEFIT: 'BENEFIT',
    INTERVAL_BENEFIT: 'INTERVAL_BENEFIT'
  }
  return map[s] || undefined
}

function normalizeStartFields(computeRule) {
  const methodNodes = computeRule?.method?.node
  if (!Array.isArray(methodNodes)) return
  for (const n of methodNodes) {
    if (!n || n.type !== 'start') continue
    const fields = Array.isArray(n.fields)
      ? n.fields.map(item => String(item || '').trim()).filter(Boolean)
      : []
    n.fields = fields.length > 0 ? [fields[0]] : []
  }
}

const FLOW_TYPES = new Set(['start', 'algo', 'result', ''])

function walk(node) {
  if (!node || typeof node !== 'object') return
  if (!node.uid) {
    node.uid = node.id != null ? String(node.id) : `node_${Math.random().toString(36).slice(2, 10)}`
  }

  if (node.indicatorType != null) {
    node.indicatorType = normalizeIndicatorType(node.indicatorType)
  }
  if (node.workMode != null) {
    node.workMode = normalizeWorkMode(node.workMode)
  }

  const vc = node.valueCategory != null
    ? normalizeValueCategory(node.valueCategory)
    : normalizeValueCategory(node.type)
  if (vc) {
    node.valueCategory = vc
  }

  if (node.type != null) {
    const t = String(node.type)
    if (!FLOW_TYPES.has(t)) {
      delete node.type
    }
  }

  if (node.computeRule && typeof node.computeRule === 'object') {
    normalizeStartFields(node.computeRule)
  }
  if (Array.isArray(node.children)) {
    node.children.forEach(walk)
  }
}

function round8(v) {
  return Math.round(v * 1e8) / 1e8
}

function applyRenormDepthFirst(nodes) {
  if (!Array.isArray(nodes)) return
  for (const n of nodes) {
    const children = n.children
    if (Array.isArray(children) && children.length > 0) {
      let sum = 0
      for (const c of children) {
        const w = Number(c.weight)
        if (Number.isFinite(w) && w > 0) sum += w
      }
      if (sum <= 0) {
        const w = round8(1 / children.length)
        for (const c of children) c.weight = w
      } else {
        for (const c of children) {
          const w = Number(c.weight)
          c.weight = Number.isFinite(w) && w > 0 ? round8(w / sum) : 0
        }
      }
      applyRenormDepthFirst(children)
    }
  }
}

function toChineseWorkMode(v) {
  if (v === 'MAIN_BRANCH_COLLAB') return '主分协同'
  if (v === 'INTERNAL_CIRCULATION') return '内部流转'
  return v
}

function toChineseIndicatorType(v) {
  const map = {
    SPACE_RECON: '航天侦察',
    SPACE_SITUATIONAL_AWARENESS: '太空态势感知',
    SPACE_OFFENSE_DEFENSE: '太空攻防',
    SPACE_TTC: '航天测运控',
    SPACE_LAUNCH: '航天发射',
    SEA_BASED_SPACE: '海基航天',
    SYSTEM_AGGREGATION: '聚合'
  }
  return map[v] || v
}

function walkToChinese(node) {
  if (!node || typeof node !== 'object') return
  if (node.workMode != null) {
    node.workMode = toChineseWorkMode(node.workMode)
  }
  if (node.indicatorType != null) {
    node.indicatorType = toChineseIndicatorType(node.indicatorType)
  }
  if (Array.isArray(node.children)) {
    node.children.forEach(walkToChinese)
  }
}

function escSql(s) {
  return String(s).replace(/'/g, "''")
}

const sourceObj = JSON.parse(fs.readFileSync(sourceJsonPath, 'utf8'))
const tree = JSON.parse(JSON.stringify(sourceObj))
if (!tree.treeData || typeof tree.treeData !== 'object') {
  throw new Error('source json missing treeData object')
}

walk(tree.treeData)
tree.treeData.workMode = normalizeWorkMode(tree.treeData.workMode)

const weighted = JSON.parse(JSON.stringify(tree))
applyRenormDepthFirst([weighted.treeData])

// SQL 测试数据按需求输出中文值（仅 workMode / indicatorType）
walkToChinese(tree.treeData)
walkToChinese(weighted.treeData)

const systemName = '通信对抗试验评估指标体系（单字段start）'
const description = '基于 indicator_system_with_source_filters.json 生成：每个 computeRule.start 的 fields 仅保留1个字段，并统一 workMode/indicatorType/valueCategory 编码。'

const indicatorTree = escSql(JSON.stringify(tree))
const indicatorTreeWeight = escSql(JSON.stringify(weighted))

const sql = `-- =============================================
-- 通信对抗指标体系测试数据（单字段 start）
-- 生成脚本：sql/scripts/gen_single_field_testdata_sql.mjs
-- 规则：每个 computeRule.start 节点 fields 仅保留 1 个字段
-- =============================================

DELETE FROM pgzc_indicator_system WHERE system_name = '${systemName}';

INSERT INTO pgzc_indicator_system (
    system_name, work_mode,
    indicator_tree, indicator_tree_weight, weight_assign_algorithm, conduction_config,
    description, status, is_applied,
    build_phase,
    del_flag, create_by, create_time
) VALUES (
    '${systemName}',
    '主分协同',
    '${indicatorTree}',
    '${indicatorTreeWeight}',
    'EQUAL',
    NULL,
    '${escSql(description)}',
    'PUBLISHED',
    1,
    'REFINED',
    '0',
    'admin',
    NOW()
);
`

fs.writeFileSync(outA, sql, 'utf8')
fs.writeFileSync(outB, sql, 'utf8')
console.log('written:', outA)
console.log('written:', outB)
