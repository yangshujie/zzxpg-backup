import { normalizeZhpgEquipmentType } from '@/constants/zhpgIndicatorSystem'
import { ZHPG_WORK_MODE, normalizeZhpgWorkMode } from '@/constants/zhpgWorkMode'

/**
 * 指标“计算方法”图形配置：
 * v4: { schemaVersion: 4, workMode, dataSources: [{ name?, source?, directory?, fields?, taskStage?, taskTimeStart?, taskTimeEnd?, experimentalTask?, description? }], algorithmSteps: [{ algorithmId, params? }] }
 * v3: { schemaVersion: 3, dataSources: [{ name }], algorithmSteps: [{ algorithmId, params? }] }
 * v2: 同 v3，但 algorithmSteps 可能缺少 params
 * 兼容旧版线性节点数组：[{ type: 'data'|'algorithm'|'result', ... }]
 */

export const CALC_METHOD_SCHEMA_VERSION = 4

export function emptyCalcWorkspace() {
  return {
    workMode: ZHPG_WORK_MODE.INTERNAL_CIRCULATION,
    dataSources: [],
    algorithmSteps: [],
    resultFlowNodeId: undefined,
    resultPosition: undefined,
    flowEdges: []
  }
}

function normalizeParams(p) {
  if (!p || typeof p !== 'object' || Array.isArray(p)) return {}
  const o = {}
  for (const [k, v] of Object.entries(p)) {
    if (v == null) continue
    const s = String(v).trim()
    if (s !== '') o[k] = s
  }
  return o
}

function normalizeFields(fields) {
  // 兼容 field 单值字符串 或 fields 数组；同一目录下可绑定多列
  if (fields != null && !Array.isArray(fields)) {
    const s = String(fields).trim()
    return s ? [s] : []
  }
  if (!Array.isArray(fields)) return []
  return fields.map(item => {
    if (item == null) return ''
    // 如果是对象格式 {"fieldName": "fieldComment"}，直接保留
    if (typeof item === 'object' && !Array.isArray(item)) return item
    return String(item).trim()
  }).filter(Boolean)
}

function normalizeWorkMode(workMode) {
  return normalizeZhpgWorkMode(workMode, ZHPG_WORK_MODE.INTERNAL_CIRCULATION)
}

function normalizeDataSource(d) {
  const row = {
    name: d && d.name != null ? String(d.name) : '',
    source: normalizeZhpgEquipmentType(d && d.source != null ? String(d.source) : '', '') || '',
    sourceName: d && d.sourceName != null ? String(d.sourceName) : '',
    directory: d && d.directory != null ? String(d.directory) : '',
    directoryName: d && d.directoryName != null ? String(d.directoryName) : '',
    fields: normalizeFields(d && d.fields),
    fieldTypes: d && d.fieldTypes && typeof d.fieldTypes === 'object' && !Array.isArray(d.fieldTypes) ? { ...d.fieldTypes } : {},
    taskStage: d && d.taskStage != null ? String(d.taskStage) : '',
    taskTimeStart: d && d.taskTimeStart != null ? String(d.taskTimeStart) : '',
    taskTimeEnd: d && d.taskTimeEnd != null ? String(d.taskTimeEnd) : '',
    experimentalTask: d && d.experimentalTask != null ? String(d.experimentalTask) : '',
    description: d && d.description != null ? String(d.description) : ''
  }
  const fid = d && d.flowNodeId != null ? String(d.flowNodeId).trim() : ''
  if (fid) {
    row.flowNodeId = fid
  }
  const pos = normalizePosition(d)
  if (pos) Object.assign(row, pos)
  return row
}

function ensureDataSourcesByWorkMode(workMode, dataSources) {
  void workMode
  return (dataSources || []).map(normalizeDataSource)
}

/** 解析入 workspace：按节点真实配置还原，不再按工作模式注入占位数据源 */
function dataSourcesForParsedWorkspace(_workMode, dataSources) {
  return (dataSources || []).map(normalizeDataSource)
}

function normalizeStep(s) {
  const step = {
    algorithmId: s && s.algorithmId != null ? String(s.algorithmId) : '',
    params: normalizeParams(s && s.params)
  }
  const fid = s && s.flowNodeId != null ? String(s.flowNodeId).trim() : ''
  if (fid) {
    step.flowNodeId = fid
  }
  const pos = normalizePosition(s)
  if (pos) Object.assign(step, pos)
  return step
}

function normalizePosition(item) {
  if (!item || typeof item !== 'object') return null
  const x = Number(item.x)
  const y = Number(item.y)
  if (!Number.isFinite(x) || !Number.isFinite(y)) return null
  return { x, y }
}

function normalizeFlowEdges(edges) {
  if (!Array.isArray(edges)) return []
  const out = []
  const seen = new Set()
  for (const edge of edges) {
    const sourceId = edge && edge.sourceId != null ? String(edge.sourceId).trim() : ''
    const targetId = edge && edge.targetId != null ? String(edge.targetId).trim() : ''
    if (!sourceId || !targetId || sourceId === targetId) continue
    const key = `${sourceId}->${targetId}`
    if (seen.has(key)) continue
    seen.add(key)
    out.push({ sourceId, targetId })
  }
  return out
}

export function legacyFlatNodesToWorkspace(nodes) {
  const dataSources = []
  const algorithmSteps = []
  if (!Array.isArray(nodes)) return emptyCalcWorkspace()
  for (const n of nodes) {
    if (!n || n.type === 'result') continue
    if (n.type === 'data') dataSources.push(normalizeDataSource(n))
    else if (n.type === 'algorithm') algorithmSteps.push(normalizeStep(n))
  }
  return {
    workMode: ZHPG_WORK_MODE.INTERNAL_CIRCULATION,
    dataSources,
    algorithmSteps,
    resultFlowNodeId: undefined
  }
}

function parseV4LikeObject(o, defaultWorkMode) {
  if (!o || typeof o !== 'object') return null
  const ver = Number(o.schemaVersion)
  if (
    (ver === 2 || ver === 3 || ver === 4) &&
    Array.isArray(o.dataSources) &&
    Array.isArray(o.algorithmSteps)
  ) {
    const workMode = normalizeWorkMode(o.workMode != null ? o.workMode : defaultWorkMode)
    return {
      workMode,
      dataSources: dataSourcesForParsedWorkspace(workMode, o.dataSources),
      algorithmSteps: o.algorithmSteps.map(normalizeStep),
      resultFlowNodeId: o.resultFlowNodeId != null ? String(o.resultFlowNodeId).trim() || undefined : undefined,
      resultPosition: normalizePosition(o.resultPosition),
      flowEdges: normalizeFlowEdges(o.flowEdges)
    }
  }
  return null
}

/** 自 computeRule.method 按 lineList 顺序收集 algo 节点 */
function orderedAlgoNodesFromMethod(method) {
  const nodes = method?.node
  const lines = method?.lineList
  if (!Array.isArray(nodes) || !Array.isArray(lines)) return []
  const byId = {}
  for (const n of nodes) {
    if (n && n.id != null) byId[String(n.id)] = n
  }
  const outgoing = {}
  for (const l of lines) {
    if (!l) continue
    const s = l.sourceId != null ? String(l.sourceId) : ''
    const t = l.targetId != null ? String(l.targetId) : ''
    if (!s || !t) continue
    if (!outgoing[s]) outgoing[s] = []
    outgoing[s].push(t)
  }
  const starts = nodes.filter(n => n && n.type === 'start').map(n => String(n.id))
  const seen = new Set()
  const algoOrder = []
  function visit(id) {
    if (!id || seen.has(id)) return
    seen.add(id)
    const n = byId[id]
    if (n && n.type === 'algo') algoOrder.push(n)
    for (const t of outgoing[id] || []) visit(t)
  }
  for (const s of starts) visit(s)
  return algoOrder
}

function configArrayToParams(config) {
  if (!Array.isArray(config)) return {}
  const o = {}
  for (const c of config) {
    if (!c || typeof c !== 'object') continue
    const k = c.field != null && String(c.field).trim() !== '' ? String(c.field) : c.name
    if (!k) continue
    const dv = c.defaultValue
    if (dv == null) continue
    const s = String(dv).trim()
    if (s !== '') o[k] = s
  }
  return o
}

function parseComputeRuleObject(computeRule, defaultWorkMode) {
  if (!computeRule || typeof computeRule !== 'object') return null
  const method = computeRule.method
  if (!method || !Array.isArray(method.node)) return null
  const workMode = normalizeWorkMode(defaultWorkMode)
  const startNodes = method.node.filter(n => n && n.type === 'start')
  const dataSources = startNodes.map(n =>
    normalizeDataSource({
      name: n.name,
      source: n.source,
      directory: n.value,
      // 兼容 field 单值字符串（新版 split_starts 格式）
      fields: n.fields !== undefined ? n.fields : (n.field !== undefined ? n.field : undefined),
      taskStage: n.taskStage,
      taskTimeStart: n.taskTimeStart,
      taskTimeEnd: n.taskTimeEnd,
      experimentalTask: n.experimentalTask,
      description: n.description,
      flowNodeId: n.id != null ? String(n.id) : undefined,
      x: n.x,
      y: n.y
    })
  )
  const orderedAlgos = orderedAlgoNodesFromMethod(method)
  const algorithmSteps = orderedAlgos.map(n =>
    normalizeStep({
      algorithmId: n.value != null ? String(n.value) : '',
      params: configArrayToParams(n.config),
      flowNodeId: n.id != null ? String(n.id) : undefined,
      x: n.x,
      y: n.y
    })
  )
  const resultNode = (method.node || []).find(n => n && n.type === 'result')
  const resultFlowNodeId = resultNode && resultNode.id != null ? String(resultNode.id).trim() : undefined
  const resultPosition = normalizePosition(resultNode)
  return {
    workMode,
    dataSources: dataSourcesForParsedWorkspace(workMode, dataSources),
    algorithmSteps,
    resultFlowNodeId: resultFlowNodeId || undefined,
    resultPosition,
    flowEdges: normalizeFlowEdges(method.lineList)
  }
}

/**
 * 统一解析：v4 JSON 字符串 / v4 对象 / computeRule 对象 / 含 computeRule 的叶子片段
 * @param {string|object|null|undefined} input
 * @param {{ defaultWorkMode?: string }} [options]
 */
export function parseLeafCalcToWorkspace(input, options = {}) {
  const defaultWorkMode = options.defaultWorkMode || ZHPG_WORK_MODE.INTERNAL_CIRCULATION
  if (input == null) return emptyCalcWorkspace()
  if (Array.isArray(input)) {
    return legacyFlatNodesToWorkspace(input)
  }
  if (typeof input === 'string') {
    const t = String(input).trim()
    if (!t) return emptyCalcWorkspace()
    try {
      const o = JSON.parse(t)
      return parseLeafCalcToWorkspace(o, options)
    } catch {
      return emptyCalcWorkspace()
    }
  }
  if (typeof input !== 'object' || Array.isArray(input)) {
    return emptyCalcWorkspace()
  }
  if (input.computeRule != null && typeof input.computeRule === 'object') {
    const ws = parseComputeRuleObject(input.computeRule, defaultWorkMode)
    if (ws) return ws
  }
  // 叶子节点直接存 computeRule：{ method: { node, lineList } }，常无顶层 data
  if (input.method != null && typeof input.method === 'object' && Array.isArray(input.method.node)) {
    const ws = parseComputeRuleObject(input, defaultWorkMode)
    if (ws) return ws
  }
  const v4 = parseV4LikeObject(input, defaultWorkMode)
  if (v4) return v4
  return emptyCalcWorkspace()
}

function mkFlowNodeId(seq) {
  return `${seq}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

/** 新建数据源/算法步骤时分配 id，序号区间与解析出的旧 id（通常为小序号）错开，降低歧义 */
let newFlowNodeSeq = 100000

/**
 * 为界面新建的流节点分配稳定 id（首次分配后由 flowNodeId 字段带回，后续序列化不再改）
 */
export function allocFlowNodeIdForNewNode() {
  return mkFlowNodeId(++newFlowNodeSeq)
}

function pickOrAllocFlowNodeId(preferred, seqRef) {
  const s = preferred != null ? String(preferred).trim() : ''
  if (s) {
    return s
  }
  return mkFlowNodeId(++seqRef.v)
}

/**
 * 将工作区序列化为原项目风格 computeRule（嵌套对象，非字符串）
 */
export function serializeWorkspaceToComputeRule(ws, leafUid) {
  const dataSources = (ws.dataSources || []).map(normalizeDataSource)

  const steps = (ws.algorithmSteps || []).map(normalizeStep)
  const methodNodes = []
  const lineList = []
  const seqRef = { v: 0 }
  const startIds = []
  for (const d of dataSources) {
    const id = pickOrAllocFlowNodeId(d.flowNodeId, seqRef)
    startIds.push(id)
    const startNode = {
      type: 'start',
      value: d.directory != null ? String(d.directory) : '',
      id,
      name: d.name != null ? String(d.name) : '',
      fields: normalizeFields(d.fields)
    }
    const pos = normalizePosition(d)
    if (pos) Object.assign(startNode, pos)
    const src = d.source != null ? String(d.source).trim() : ''
    if (src) startNode.source = src
    const ts = d.taskStage != null ? String(d.taskStage).trim() : ''
    const t0 = d.taskTimeStart != null ? String(d.taskTimeStart).trim() : ''
    const t1 = d.taskTimeEnd != null ? String(d.taskTimeEnd).trim() : ''
    const et = d.experimentalTask != null ? String(d.experimentalTask).trim() : ''
    const desc = d.description != null ? String(d.description).trim() : ''
    if (ts) startNode.taskStage = ts
    if (t0) startNode.taskTimeStart = t0
    if (t1) startNode.taskTimeEnd = t1
    if (et) startNode.experimentalTask = et
    if (desc) startNode.description = desc
    methodNodes.push(startNode)
  }
  let prevAlgoId = null
  for (const s of steps) {
    const aid = String(s.algorithmId || '').trim()
    if (!aid) continue
    const id = pickOrAllocFlowNodeId(s.flowNodeId, seqRef)
    const numId = /^\d+$/.test(aid) ? Number(aid) : aid
    const params = normalizeParams(s.params)
    const configKeys = Object.keys(params)
    const config =
      configKeys.length > 0
        ? configKeys.map(k => ({
            name: k,
            field: k,
            type: '',
            description: '',
            defaultValue: params[k]
          }))
        : [
            {
              name: 'config',
              field: '',
              type: '',
              description: '',
              defaultValue: ''
            }
          ]
    const isNorm = numId === 227 || aid === '227'
    methodNodes.push({
      type: 'algo',
      value: numId,
      id,
      config,
      url: isNorm ? 'algs/norm/sigmoid.zip' : 'algs/character/calMean.zip',
      baseFlag: isNorm ? 1 : 0,
      algoType: isNorm ? '指标量化算法' : '属性值计算方法',
      subtype: ''
    })
    const pos = normalizePosition(s)
    if (pos) Object.assign(methodNodes[methodNodes.length - 1], pos)
    if (prevAlgoId == null) {
      for (const sid of startIds) {
        lineList.push({ sourceId: sid, targetId: id })
      }
    } else {
      lineList.push({ sourceId: prevAlgoId, targetId: id })
    }
    prevAlgoId = id
  }
  const resultId = pickOrAllocFlowNodeId(ws.resultFlowNodeId, seqRef)
  methodNodes.push({
    type: 'result',
    value: '',
    id: resultId
  })
  const resultPosition = normalizePosition(ws.resultPosition)
  if (resultPosition) Object.assign(methodNodes[methodNodes.length - 1], resultPosition)
  const customEdges = normalizeFlowEdges(ws.flowEdges)
  if (customEdges.length > 0) {
    lineList.splice(0, lineList.length, ...customEdges)
  } else {
    if (prevAlgoId) {
      lineList.push({ sourceId: prevAlgoId, targetId: resultId })
    } else {
      for (const sid of startIds) {
        lineList.push({ sourceId: sid, targetId: resultId })
      }
    }
  }
  return {
    data: leafUid != null && String(leafUid).trim() !== '' ? String(leafUid) : 'leaf',
    method: {
      node: methodNodes,
      lineList
    }
  }
}

export function parseCalcMethodToWorkspace(str) {
  return parseLeafCalcToWorkspace(str, {})
}

export function serializeCalcWorkspace(ws) {
  const workMode = normalizeWorkMode(ws.workMode)
  const dataSources = ensureDataSourcesByWorkMode(
    workMode,
    (ws.dataSources || []).map(normalizeDataSource)
  )
  return {
    schemaVersion: CALC_METHOD_SCHEMA_VERSION,
    workMode,
    dataSources: dataSources.map(d => {
      const row = {
        name: d.name || '',
        directory: d.directory || undefined,
        fields: d.fields && d.fields.length ? d.fields : undefined
      }
      const sc = String(d.source || '').trim()
      if (sc) row.source = sc
      const ts = String(d.taskStage || '').trim()
      const t0 = String(d.taskTimeStart || '').trim()
      const t1 = String(d.taskTimeEnd || '').trim()
      const et = String(d.experimentalTask || '').trim()
      const desc = String(d.description || '').trim()
      if (ts) row.taskStage = ts
      if (t0) row.taskTimeStart = t0
      if (t1) row.taskTimeEnd = t1
      if (et) row.experimentalTask = et
      if (desc) row.description = desc
      return row
    }),
    algorithmSteps: (ws.algorithmSteps || []).map(s => {
      const algorithmId = s.algorithmId || ''
      const params = normalizeParams(s.params)
      const step = { algorithmId }
      if (Object.keys(params).length > 0) {
        step.params = params
      }
      return step
    })
  }
}

export function calcMethodIsNonJsonText(input) {
  if (input != null && typeof input === 'object') return false
  const str = input == null ? '' : String(input)
  if (!str.trim()) return false
  try {
    JSON.parse(str)
    return false
  } catch {
    return true
  }
}

export function hasStructuredCalcFlow(input) {
  const ws = parseLeafCalcToWorkspace(input, {})
  return ws.dataSources.length > 0 || ws.algorithmSteps.length > 0
}

export function getCalcMethodWorkMode(input, rootFallback) {
  const def = rootFallback || ZHPG_WORK_MODE.INTERNAL_CIRCULATION
  if (input == null || input === '') return def
  if (typeof input === 'string' && !String(input).trim()) return def
  const ws = parseLeafCalcToWorkspace(input, { defaultWorkMode: def })
  if (ws.dataSources.length === 0 && ws.algorithmSteps.length === 0) return def
  return ws.workMode
}

/**
 * 更新工作模式：字符串 calcMethod 仍输出 v4 字符串；computeRule 对象则输出新 computeRule 对象（工作模式在根节点维护，此处仅影响解析默认数据源策略）。
 */
export function setCalcMethodWorkMode(input, workMode) {
  if (input != null && typeof input === 'object') {
    const ws = parseLeafCalcToWorkspace(input, { defaultWorkMode: workMode })
    ws.workMode = normalizeWorkMode(workMode)
    const leafUid =
      input.data != null && typeof input.data === 'string'
        ? input.data
        : input.computeRule?.data != null
          ? String(input.computeRule.data)
          : 'leaf'
    return serializeWorkspaceToComputeRule(ws, leafUid)
  }
  if (calcMethodIsNonJsonText(input)) return input
  const ws = parseLeafCalcToWorkspace(input, {})
  ws.workMode = normalizeWorkMode(workMode)
  return JSON.stringify(serializeCalcWorkspace(ws))
}

/**
 * 图形配置完整性校验：
 * 1. 仅当配置了算法链或数据源时才校验；
 * 2. 有算法链时须至少一个数据源，且每个数据源须配置目录与至少一个字段（与工作模式无关）。
 */
export function validateCalcWorkspace(ws) {
  const ds = ensureDataSourcesByWorkMode(ws.workMode, ws.dataSources || [])
  const steps = ws.algorithmSteps || []
  if (ds.length === 0 && steps.length === 0) return null
  if (steps.length === 0) {
    return '已添加数据源，请在算法链中至少添加一个算法步骤并选择算法'
  }
  if (ds.length === 0) {
    return '已配置算法链，请至少添加一个数据源并配置数据目录和字段'
  }
  for (let i = 0; i < ds.length; i++) {
    if (!String(ds[i].directory || '').trim()) {
      return '数据源必须配置数据目录'
    }
    if (!Array.isArray(ds[i].fields) || ds[i].fields.length === 0) {
      return '每个数据源（start）必须配置至少一个数据字段（多个字段用逗号分隔）'
    }
  }
  for (let i = 0; i < steps.length; i++) {
    if (!String(steps[i].algorithmId || '').trim()) {
      return '算法链中存在未选择算法的步骤，请补全或删除该项'
    }
  }
  return null
}

/**
 * 对存库 calcMethod 字符串做完整性校验；纯文本说明不拦截。
 */
/** 叶子节点是否配置了计算方法（computeRule 或 calcMethod） */
export function leafHasCalcConfig(node) {
  if (!node) return false
  if (node.computeRule != null && typeof node.computeRule === 'object') return true
  return String(node.calcMethod || '').trim() !== ''
}

export function validateCalcMethodString(input) {
  if (input == null) return null
  if (typeof input === 'object') {
    const ws = parseLeafCalcToWorkspace(input, {})
    if (ws.dataSources.length === 0 && ws.algorithmSteps.length === 0) return null
    return validateCalcWorkspace(ws)
  }
  const str = String(input)
  if (!str.trim()) return null
  if (calcMethodIsNonJsonText(str)) return null
  let o
  try {
    o = JSON.parse(str.trim())
  } catch {
    return '计算方法 JSON 格式无效，请重新打开“配置算法”修正，或改为纯文本说明'
  }
  const ws = parseLeafCalcToWorkspace(o, {})
  if (ws.dataSources.length === 0 && ws.algorithmSteps.length === 0) return null
  return validateCalcWorkspace(ws)
}
