/**
 * 从 zhpg_indicator_system_testdata_comm_countermeasure.sql 解析 indicator_tree，
 * 按 Java IndicatorTreeWeightServiceImpl RENORMALIZE 写入子节点 weight，
 * 并在同一 INSERT 中增加 indicator_tree_weight 列（就地写回 SQL 文件）。
 * 用法：node gen_indicator_tree_weight.mjs
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sqlPath = path.join(__dirname, '..', 'zhpg_indicator_system_testdata_comm_countermeasure.sql')

function round8(v) {
  return Math.round(v * 1e8) / 1e8
}

function applyRenormDepthFirst(nodes) {
  if (!nodes || !nodes.length) return
  for (const n of nodes) {
    const ch = n.children
    if (ch && ch.length) {
      let sum = 0
      for (const c of ch) {
        const w = c.weight
        if (w != null && w > 0 && Number.isFinite(Number(w))) {
          sum += Number(w)
        }
      }
      if (sum <= 0) {
        const w = 1 / ch.length
        for (const c of ch) {
          c.weight = round8(w)
        }
      } else {
        for (const c of ch) {
          const w = c.weight
          if (w != null && w > 0 && Number.isFinite(Number(w))) {
            c.weight = round8(Number(w) / sum)
          } else {
            c.weight = 0
          }
        }
      }
      applyRenormDepthFirst(ch)
    }
  }
}

function extractTreeJsonFromSql(s) {
  const marker = "'{\"treeData\":"
  const a = s.indexOf(marker)
  if (a < 0) throw new Error('tree JSON start not found')
  const endBetweenTrees = "  }',\n    '{\"treeData\":"
  const b = s.indexOf(endBetweenTrees, a)
  if (b >= 0) {
    return s.slice(a + 1, b + 3)
  }
  const endEqual = "  }',\n    'EQUAL'"
  const b2 = s.indexOf(endEqual, a)
  if (b2 < 0) throw new Error('tree JSON end not found')
  return s.slice(a + 1, b2 + 3)
}

const sql = fs.readFileSync(sqlPath, 'utf8')
const treeDataLiterals = sql.match(/'\{"treeData":/g)
if (sql.includes('indicator_tree_weight') && treeDataLiterals && treeDataLiterals.length >= 2) {
  console.log('Skip: indicator_tree_weight already populated in', sqlPath)
  process.exit(0)
}
const raw = extractTreeJsonFromSql(sql)
const tree = JSON.parse(raw)
const roots = tree.treeData ? [tree.treeData] : []
applyRenormDepthFirst(roots)

const weightedJson = JSON.stringify(tree)
const sqlLiteral = "'" + weightedJson.replace(/'/g, "''") + "'"

let out = sql
if (!out.includes('indicator_tree_weight')) {
  out = out.replace(
    /indicator_tree,\s*weight_assign_algorithm/,
    'indicator_tree, indicator_tree_weight, weight_assign_algorithm'
  )
}

out = out.replace(
  /(\n  \}\',\n)    \'EQUAL\'/,
  `$1    ${sqlLiteral},\n    'EQUAL'`
)

fs.writeFileSync(sqlPath, out, 'utf8')
console.log('Updated', sqlPath, 'indicator_tree_weight length', weightedJson.length)
