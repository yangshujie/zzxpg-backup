import request from './request'

export function listIndicatorSystem(query) {
  return request({ url: '/zhpg/indicatorSystem/list', method: 'get', params: query })
}

/** 下拉：外部系统/评估任务等选择指标体系 */
export function selectIndicatorSystem(query) {
  const params = {}
  if (typeof query === 'string') {
    if (query) params.keyword = query
  } else if (query && typeof query === 'object') {
    if (query.keyword) params.keyword = query.keyword
    if (query.requirementId !== undefined && query.requirementId !== null && query.requirementId !== '') {
      params.requirementId = query.requirementId
    }
  }
  return request({
    url: '/zhpg/indicatorSystem/select',
    method: 'get',
    params
  })
}

export function getIndicatorSystem(id) {
  return request({ url: '/zhpg/indicatorSystem/' + id, method: 'get' })
}

export function addIndicatorSystem(data) {
  return request({ url: '/zhpg/indicatorSystem', method: 'post', data })
}

export function updateIndicatorSystem(data) {
  return request({ url: '/zhpg/indicatorSystem', method: 'put', data })
}

export function delIndicatorSystem(id) {
  return request({ url: '/zhpg/indicatorSystem/' + id, method: 'delete' })
}

export function distributeIndicatorSystem(id) {
  return request({ url: '/zhpg/indicatorSystem/' + id + '/distribute', method: 'post' })
}

export function publishIndicatorSystem(id) {
  return request({ url: '/zhpg/indicatorSystem/' + id + '/publish', method: 'post' })
}

/** 取消启用（isApplied=0，保留发布状态） */
export function disableIndicatorSystem(id) {
  return request({ url: '/zhpg/indicatorSystem/' + id + '/disableApplied', method: 'post' })
}


export function startCosign(id) {
  return request({ url: '/zhpg/indicatorSystem/' + id + '/cosign', method: 'post' })
}

export function approveCosign(id, approved) {
  return request({ url: '/zhpg/indicatorSystem/' + id + '/approveCosign', method: 'post', params: { approved } })
}

export function exportIndicatorSystemJson(id) {
  return request({ url: '/zhpg/indicatorSystem/' + id + '/exportJson', method: 'get' })
}

export function importIndicatorSystemJson(data) {
  return request({ url: '/zhpg/indicatorSystem/importJson', method: 'post', data })
}

export function createFromTemplate(data) {
  return request({ url: '/zhpg/indicatorSystem/createFromTemplate', method: 'post', data })
}

/** 保存用户手动编辑后的指标树权重；body: { systemId, indicatorTree } */
export function applyIndicatorTreeWeights(data) {
  return request({ url: '/zhpg/indicatorSystem/applyTreeWeights', method: 'post', data })
}

/** AHP 一致性校验：传入 n×n 比值矩阵（数字或 "a/b" 字符串），返回 { ok, cr, threshold } */
export function checkAhpConsistency(matrix) {
  return request({ url: '/zhpg/indicatorSystem/checkAHP', method: 'post', data: matrix })
}

/** 主观赋权：按各父节点保存的 6 种算法（不校验/相似度法/校验/理论证据法/连环比率法/层次分析）计算并写回 */
export function subjectiveWeightIndicatorSystem(id, data) {
  return request({
    url: '/zhpg/indicatorSystem/' + id + '/subjectiveWeight',
    method: 'post',
    data: data || {},
    timeout: 120000
  })
}

// ==================== 主分协同：构建阶段管理 ====================

/** 主分协同：提交指标体系给需求分析分系统 */
export function submitToSubsystem(id) {
  return request({ url: '/zhpg/indicatorSystem/' + id + '/submitToSubsystem', method: 'post' })
}

/** 主分协同：接收需求分析分系统回传的完整指标体系 */
export function receiveRefined(id, data) {
  return request({ url: '/zhpg/indicatorSystem/' + id + '/receiveRefined', method: 'post', data })
}

/** 客观赋权：模拟样本 + zgpg_algs；可选 persist、mockSampleRows、mockSeed（算法+Celery 可能较慢） */
export function objectiveWeightIndicatorSystem(id, data) {
  return request({
    url: '/zhpg/indicatorSystem/' + id + '/objectiveWeight',
    method: 'post',
    data: data || {},
    timeout: 360000
  })
}

/** 智能综合赋权：根据各节点配置自动分发客观/主观逻辑；可选 persist、mockSampleRows、mockSeed */
export function computeWeightsSmart(id, data) {
  return request({
    url: '/zhpg/indicatorSystem/' + id + '/computeWeightsSmart',
    method: 'post',
    data: data || {},
    timeout: 360000
  })
}

/** 客观赋权：模拟样本 + zgpg_algs；可选 persist、mockSampleRows、mockSeed（算法+Celery 可能较慢） */
export function finalizeRefined(id) {
  return request({ url: '/zhpg/indicatorSystem/' + id + '/finalizeRefined', method: 'post' })
}

/** 主分协同：获取初始粗建指标树快照 */
export function getInitialTree(id) {
  return request({ url: '/zhpg/indicatorSystem/' + id + '/initialTree', method: 'get' })
}

/** 批量检测指标体系是否被其他模块引用 */
export function checkSystemReferences(ids) {
  return request({ url: '/zhpg/indicatorSystem/checkReferences/' + ids, method: 'get' })
}
