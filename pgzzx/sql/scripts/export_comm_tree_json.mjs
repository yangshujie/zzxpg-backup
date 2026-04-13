/**
 * 从 zhpg_indicator_system_testdata_comm_countermeasure.sql 抽出首段 indicator_tree JSON，
 * 写入 通信对抗场景设计包_20260331/当前系统的指标体系.json
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sqlPath = path.join(__dirname, '..', 'zhpg_indicator_system_testdata_comm_countermeasure.sql')
const outPath = path.join(
  __dirname,
  '..',
  '..',
  '..',
  '通信对抗场景设计包_20260331',
  '当前系统的指标体系.json'
)

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
  throw new Error('unbalanced')
}

const sql = fs.readFileSync(sqlPath, 'utf8')
const needle = "'{\"treeData\":"
const a = sql.indexOf(needle)
if (a < 0) throw new Error('tree marker not found')
const brace = sql.indexOf('{', a)
const jsonStr = extractJsonObjectFromSql(sql, brace)
const tree = JSON.parse(jsonStr)
fs.writeFileSync(outPath, JSON.stringify(tree, null, 2), 'utf8')
console.log('Wrote', outPath)
