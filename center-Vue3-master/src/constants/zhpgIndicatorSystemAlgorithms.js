/**
 * 指标体系 / 模板：权重分配算法、传导方法（传导与赋权一致，下拉选一项即可）
 */

export const ZHPG_WEIGHT_ASSIGN_OPTIONS = [
  { value: 'AHP', label: '层次分析法（AHP）' },
  { value: 'ENTROPY', label: '熵权法' },
  { value: 'FAHP', label: '基于模糊层次分析与相似度算法' },
  { value: 'RELATIVE', label: '相对比较法' },
  { value: 'FUZZY_ENTROPY', label: '多维度模糊理论与熵分析' },
  { value: 'MIN_SQUARE', label: '基于对数最小二阶的模糊层次分析法' },
  { value: 'DELPHI', label: '德尔菲法' },
  { value: 'SCORING', label: '德分制、专家调查法' }
]

export function getZhpgWeightAssignLabel(val) {
  if (val == null || val === '') return '—'
  const item = ZHPG_WEIGHT_ASSIGN_OPTIONS.find(o => o.value === val)
  return item ? item.label : String(val)
}

/** 传导聚合方式（下拉一项，对应原 AggregateUtil 分支名） */
export const ZHPG_CONDUCTION_METHOD_OPTIONS = [
  { value: '串联', label: '串联' },
  { value: '并联', label: '并联' },
  { value: '热备', label: '热备' },
  { value: '冷备', label: '冷备' },
  { value: '表决', label: '表决' },
  { value: '其他', label: '其他' }
]

/**
 * @param {string} val 存库的传导名（算法名称或历史项如「串联」）
 * @param {Array<{value:string,label:string}>} [dynamicOptions] 下拉选项（与 fetchZhpgConductionMethodSelectOptions 一致），优先匹配展示
 */
export function getZhpgConductionMethodLabel(val, dynamicOptions) {
  if (val == null || val === '') return '—'
  if (Array.isArray(dynamicOptions)) {
    const d = dynamicOptions.find(o => o.value === val)
    if (d) return d.label
  }
  const item = ZHPG_CONDUCTION_METHOD_OPTIONS.find(o => o.value === val)
  return item ? item.label : String(val)
}

/**
 * 节点或历史数据：字符串，或旧版 { name, k, ... }
 */
export function coerceConductionMethod(raw) {
  if (raw == null || raw === '') return ''
  if (typeof raw === 'string') return raw
  if (typeof raw === 'object' && raw.name != null && raw.name !== '') return String(raw.name)
  return ''
}

/** 存库 / 接口：{"name":"并联"}，便于与旧引擎读取 conductionAlgorithm.name 对齐 */
export function serializeConductionConfig(method) {
  const s = coerceConductionMethod(method)
  return s ? JSON.stringify({ name: s }) : ''
}

export function parseConductionConfig(jsonStr) {
  if (!jsonStr || !String(jsonStr).trim()) return ''
  try {
    const o = JSON.parse(jsonStr)
    if (typeof o === 'string') return o
    return coerceConductionMethod(o.name ?? o.algorithm ?? o)
  } catch {
    return ''
  }
}
