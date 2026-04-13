/**
 * 将 zhpg_indicator_system_testdata_comm_countermeasure.sql 中指标树
 * 叶子 calcMethod(v4 字符串) 转为 computeRule(对象)，并更新行级字段与根 workMode。
 * 用法：node migrate_testdata_compute_rule.mjs
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sqlPath = path.join(__dirname, '..', 'zhpg_indicator_system_testdata_comm_countermeasure.sql')

function extractJsonObjectFromSql(sql, startBraceIdx) {
  let depth = 0
  let i = startBraceIdx
  let inStr = false
  let esc = false
  for (; i < sql.length; i++) {
    const c = sql[i]
    if (inStr) {
      if (esc) esc = false
      else if (c === '\\') esc = true
      else if (c === '"') inStr = false
      continue
    }
    if (c === '"') {
      inStr = true
      continue
    }
    if (c === '{') depth++
    if (c === '}') {
      depth--
      if (depth === 0) return sql.slice(startBraceIdx, i + 1)
    }
  }
  throw new Error('unbalanced JSON braces')
}

function findTreeJsonStarts(sql) {
  const needle = "'{\"treeData\":"
  const out = []
  let from = 0
  while (true) {
    const a = sql.indexOf(needle, from)
    if (a < 0) break
    const brace = sql.indexOf('{', a)
    if (brace < 0) break
    out.push(brace)
    from = a + needle.length
  }
  return out
}

let idSeq = 0
function mkNodeId() {
  idSeq++
  return `${idSeq}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function v4ToComputeRule(node) {
  const raw = node.calcMethod
  if (raw == null || raw === '') return
  const str = typeof raw === 'string' ? raw.trim() : ''
  if (!str) return
  let ws
  try {
    ws = JSON.parse(str)
  } catch {
    return
  }
  if (!ws || (ws.schemaVersion !== 2 && ws.schemaVersion !== 3 && ws.schemaVersion !== 4)) return
  if (!Array.isArray(ws.dataSources) || !Array.isArray(ws.algorithmSteps)) return

  const uid = node.uid || 'leaf'
  const params = ws.algorithmSteps[0]?.params && typeof ws.algorithmSteps[0].params === 'object' ? ws.algorithmSteps[0].params : {}
  const ds = ws.dataSources

  const methodNodes = []
  const lineList = []
  const startIds = []

  for (const d of ds) {
    const sid = mkNodeId()
    startIds.push(sid)
    methodNodes.push({
      type: 'start',
      value: d.directory != null ? String(d.directory) : '',
      id: sid,
      name: d.name != null ? String(d.name) : '',
      fields: Array.isArray(d.fields) ? d.fields.map(f => String(f)) : []
    })
  }

  const algo306Id = mkNodeId()
  const algo227Id = mkNodeId()
  const resultId = mkNodeId()

  const config306 = Object.keys(params).map(k => ({
    name: k,
    field: k,
    type: '',
    description: '',
    defaultValue: params[k] == null ? '' : String(params[k])
  }))
  if (config306.length === 0) {
    config306.push({
      name: 'config',
      field: '',
      type: '',
      description: '',
      defaultValue: ''
    })
  }

  methodNodes.push({
    type: 'algo',
    value: 306,
    id: algo306Id,
    config: config306,
    url: 'algs/character/calMean.zip',
    baseFlag: 0,
    algoType: '属性值计算方法',
    subtype: ''
  })
  methodNodes.push({
    type: 'algo',
    value: 227,
    id: algo227Id,
    config: [
      {
        name: 'config',
        field: '',
        type: '',
        description: '',
        defaultValue: ''
      }
    ],
    url: 'algs/norm/sigmoid.zip',
    baseFlag: 1,
    algoType: '指标量化算法',
    subtype: ''
  })
  methodNodes.push({
    type: 'result',
    value: '',
    id: resultId
  })

  for (const sid of startIds) {
    lineList.push({ sourceId: sid, targetId: algo306Id })
  }
  lineList.push({ sourceId: algo306Id, targetId: algo227Id })
  lineList.push({ sourceId: algo227Id, targetId: resultId })

  delete node.calcMethod
  node.computeRule = {
    data: uid,
    method: {
      node: methodNodes,
      lineList
    }
  }
}

function walkTransform(nodes) {
  if (!Array.isArray(nodes)) return
  for (const n of nodes) {
    if (n.children && n.children.length) {
      walkTransform(n.children)
    } else if (n.calcMethod != null && String(n.calcMethod).trim()) {
      v4ToComputeRule(n)
    }
  }
}

function transformTreeRoot(treeWrapper) {
  idSeq = 0
  if (!treeWrapper.treeData) return
  const root = treeWrapper.treeData
  root.workMode = 'MAIN_BRANCH_COLLAB'
  walkTransform(root.children || [])
}

function sqlStringLiteral(obj) {
  const json = JSON.stringify(obj)
  return "'" + json.replace(/'/g, "''") + "'"
}

function replaceTreeLiterals(sql) {
  const starts = findTreeJsonStarts(sql)
  if (starts.length < 1) throw new Error('no treeData literals found')

  let out = sql
  for (let t = starts.length - 1; t >= 0; t--) {
    const braceIdx = starts[t]
    const jsonStr = extractJsonObjectFromSql(out, braceIdx)
    const tree = JSON.parse(jsonStr)
    transformTreeRoot(tree)
    const newJson = JSON.stringify(tree)
    const before = out.slice(0, braceIdx)
    const after = out.slice(braceIdx + jsonStr.length)
    out = before + newJson + after
  }
  return out
}

function patchInsertRow(sql) {
  let s = sql
  s = s.replace(
    /'COMBAT_EFFECTIVENESS',\s*\n\s*'SPACE_OFFENSE_DEFENSE',\s*\n\s*'INTERNAL_CIRCULATION'/,
    "'EQUIPMENT_COMBAT_EFFECTIVENESS',\n    'SPACE_OFFENSE_DEFENSE',\n    'MAIN_BRANCH_COLLAB'"
  )
  s = s.replace(
    /-- 工作模式：内部流转（INTERNAL_CIRCULATION）[\s\S]*?-- =============================================/,
    `-- 工作模式：主分协同（MAIN_BRANCH_COLLAB），构建阶段已回传细化（REFINED）
-- 叶子节点：computeRule（method.node + lineList），属性值算法 306 → sigmoid 227
-- indicator_tree_weight：同结构含层内归一 weight，可由 sql/scripts/gen_indicator_tree_weight.mjs 再生
-- =============================================`
  )
  s = s.replace(
    /所有叶子节点均配置了数据源（表\+字段）与算法步骤（属性值算法 algorithmId=306）/,
    '所有叶子节点均配置了 computeRule（多数据源 start 汇入 306→227→result）'
  )
  if (!s.includes('build_phase')) {
    s = s.replace(
      /(\s+indicator_tree,\s+indicator_tree_weight,\s+weight_assign_algorithm,\s+conduction_config,\s*\n\s*description,\s+status,\s+is_applied,)\s*\n(?:\s*cosign_status,\s*\n)?/,
      '$1\n    build_phase,\n'
    )
    s = s.replace(
      /(\s+'PUBLISHED',\s*\n\s*1,\s*\n\s*)'APPROVED',\s*\n\s*'FINALIZED',/,
      "$1'REFINED',"
    )
    s = s.replace(
      /(\s+'PUBLISHED',\s*\n\s*1,\s*\n\s*)'APPROVED',\s*\n\s*('0',)/,
      "$1'REFINED',\n    $2"
    )
  }
  return s
}

let sql = fs.readFileSync(sqlPath, 'utf8')
sql = patchInsertRow(sql)
sql = replaceTreeLiterals(sql)
fs.writeFileSync(sqlPath, sql, 'utf8')
console.log('Migrated', sqlPath)
