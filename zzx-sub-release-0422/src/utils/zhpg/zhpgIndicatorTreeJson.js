/**
 * 指标体系指标树 JSON：与原先评估项目一致，持久化为 { treeData: 根节点对象 }。
 * 兼容历史数据：顶层为 JSON 数组、或裸对象。
 * 多根场景：使用合成根节点（uid=__zhpg_forest_root__），解析时展开为根数组供工作台使用。
 */

export const ZHPG_FOREST_ROOT_UID = '__zhpg_forest_root__'

const WORK_MODE_ALIASES = {
  MAIN_BRANCH_COLLAB: '主分协同',
  INTERNAL_CIRCULATION: '内部流转',
  主分协同: '主分协同',
  内部流转: '内部流转'
}

const EQUIPMENT_TYPE_ALIASES = {
  space_recon: 'space_recon',
  space_domain_awareness: 'space_domain_awareness',
  space_defense: 'space_defense',
  space_track_control: 'space_track_control',
  space_launch: 'space_launch',
  sea_based_space: 'sea_based_space',
  SPACE_RECON: 'space_recon',
  SPACE_SITUATIONAL_AWARENESS: 'space_domain_awareness',
  SPACE_OFFENSE_DEFENSE: 'space_defense',
  SPACE_TTC: 'space_track_control',
  SPACE_LAUNCH: 'space_launch',
  SEA_BASED_SPACE: 'sea_based_space',
  SYSTEM_AGGREGATION: '无',
  SPACE_AWARENESS: 'space_domain_awareness',
  SPACE_AD: 'space_defense',
  SPACE_SA: 'space_domain_awareness',
  SEA_BASED: 'sea_based_space',
  航天侦察: 'space_recon',
  太空态势感知: 'space_domain_awareness',
  太空攻防: 'space_defense',
  航天测运控: 'space_track_control',
  航天发射: 'space_launch',
  海基航天: 'sea_based_space',
  无: '无'
}

const VALUE_CATEGORY_ALIASES = {
  COST: '成本型',
  BENEFIT: '效益型',
  INTERVAL_BENEFIT: '区间效益型',
  成本型: '成本型',
  效益型: '效益型',
  区间效益型: '区间效益型'
}

function normalizeWorkMode(v) {
  if (v == null || v === '') return undefined
  const s = String(v).trim()
  return WORK_MODE_ALIASES[s] || s
}

function normalizeEquipmentType(v) {
  if (v == null || v === '') return undefined
  const s = String(v).trim()
  return EQUIPMENT_TYPE_ALIASES[s] || s
}

function normalizeValueCategory(v) {
  if (v == null || v === '') return undefined
  const s = String(v).trim()
  return VALUE_CATEGORY_ALIASES[s] || s
}

function normalizeTreeNode(node) {
  if (!node || typeof node !== 'object' || Array.isArray(node)) return node

  const next = { ...node }

  if (next.id != null && next.uid == null) {
    next.uid = next.id
    delete next.id
  }

  const wm = normalizeWorkMode(next.workMode)
  if (wm) next.workMode = wm

  const it = normalizeEquipmentType(next.indicatorType)
  if (it) next.indicatorType = it

  const vc = normalizeValueCategory(next.valueCategory != null ? next.valueCategory : next.type)
  if (vc) next.valueCategory = vc
  if (next.type) delete next.type

  if (next.indicatorName == null && next.label != null) {
    next.indicatorName = next.label
  }

  if (Array.isArray(next.children)) {
    next.children = next.children.map(normalizeTreeNode)
  }

  return next
}

function normalizeForest(forest) {
  if (!Array.isArray(forest)) return []
  return forest.map(normalizeTreeNode)
}

/**
 * 持久化到库的节点：使用与下发 JSON 一致的 `id` 字段（工作台运行时用 uid，解析时 id→uid）
 */
function toPersistNode(node) {
  if (!node || typeof node !== 'object' || Array.isArray(node)) return node
  const idVal = node.id != null && node.id !== '' ? node.id : node.uid
  const { uid, id, children, ...rest } = node
  const out = { ...rest }
  if (idVal != null && idVal !== '') {
    out.id = String(idVal)
  }
  // Ensure indicatorName exists for backend alignment
  if (!out.indicatorName && out.label) {
    out.indicatorName = out.label
  }
  // Remove frontend temporary fields if any
  delete out.style
  delete out.layoutWidth
  delete out.layoutHeight

  const ch = Array.isArray(children) ? children.map(toPersistNode) : []
  out.children = ch
  return out
}

/**
 * 将库中 indicator_tree 字符串解析为 el-tree 使用的根节点数组
 * @param {string|undefined|null} json
 * @returns {Array}
 */
export function parseIndicatorTreeToForest(json) {
  if (json == null || json === '') return []
  try {
    const parsed = typeof json === 'string' ? JSON.parse(json) : json
    if (Array.isArray(parsed)) {
      return normalizeForest(parsed)
    }
    if (parsed && typeof parsed === 'object' && parsed.treeData != null) {
      const td = parsed.treeData
      if (Array.isArray(td)) {
        return td.length ? normalizeForest(td) : []
      }
      if (typeof td === 'object' && td.uid === ZHPG_FOREST_ROOT_UID && Array.isArray(td.children)) {
        return normalizeForest(td.children)
      }
      return normalizeForest([td])
    }
    if (parsed && typeof parsed === 'object') {
      return normalizeForest([parsed])
    }
    return []
  } catch {
    return []
  }
}

/**
 * 将工作台根数组序列化为入库 JSON（老项目 { treeData } 形态）
 * @param {Array} forest
 * @param {{ systemName?: string, workMode?: string }} opts
 */
export function serializeForestToIndicatorTree(forest, opts = {}) {
  const roots = normalizeForest(Array.isArray(forest) ? forest : [])
  const systemName = (opts.systemName && String(opts.systemName).trim()) || '指标体系'
  const workMode = normalizeWorkMode(opts.workMode)

  const algoFields = {}
  if (opts.weightAssignAlgorithm) {
    algoFields.weightAssignAlgorithm = /^\d+$/.test(opts.weightAssignAlgorithm) ? Number(opts.weightAssignAlgorithm) : opts.weightAssignAlgorithm
    algoFields.weightAssignAlgorithmParams = opts.weightAssignAlgorithmParams || {}
  }
  if (opts.conductionAlgorithm) {
    algoFields.conductionAlgorithm = /^\d+$/.test(opts.conductionAlgorithm) ? Number(opts.conductionAlgorithm) : opts.conductionAlgorithm
    algoFields.conductionAlgorithmParams = opts.conductionAlgorithmParams || {}
  }
  let treeData
  if (roots.length === 0) {
    treeData = {
      id: 'root_0',
      label: systemName,
      indicatorType: '无',
      children: [],
      ...(workMode ? { workMode } : {}),
      ...algoFields
    }
  } else if (roots.length === 1) {
    treeData = { ...toPersistNode(roots[0]), ...algoFields }
  } else {
    treeData = {
      id: ZHPG_FOREST_ROOT_UID,
      uid: ZHPG_FOREST_ROOT_UID,
      label: systemName,
      indicatorType: '无',
      children: roots.map(toPersistNode),
      ...(workMode ? { workMode } : {}),
      ...algoFields
    }
  }
  return JSON.stringify({ treeData })
}
