import request from '@/utils/request'

export function listIndicator(query) {
  return request({ url: '/zhpg/indicator/list', method: 'get', params: query })
}

export function listAllIndicator(query) {
  return request({ url: '/zhpg/indicator/listAll', method: 'get', params: query })
}

export function listParentIndicatorCandidates(query) {
  return request({ url: '/zhpg/indicator/parentCandidates', method: 'get', params: query })
}

export function getIndicatorTree(query) {
  return request({ url: '/zhpg/indicator/tree', method: 'get', params: query })
}

export function getIndicator(id) {
  return request({ url: '/zhpg/indicator/' + id, method: 'get' })
}

export function addIndicator(data) {
  return request({ url: '/zhpg/indicator', method: 'post', data })
}

export function updateIndicator(data) {
  return request({ url: '/zhpg/indicator', method: 'put', data })
}

export function delIndicator(id) {
  return request({ url: '/zhpg/indicator/' + id, method: 'delete' })
}

export function publishIndicator(id) {
  return request({ url: '/zhpg/indicator/' + id + '/publish', method: 'post' })
}

export function validatePublishIndicator(id) {
  return request({ url: '/zhpg/indicator/' + id + '/validatePublish', method: 'get' })
}

export function batchPublishIndicator(ids) {
  return request({ url: '/zhpg/indicator/batchPublish', method: 'post', data: ids })
}

export function archiveIndicator(id) {
  return request({ url: '/zhpg/indicator/' + id + '/archive', method: 'post' })
}

export function instantiateIndicatorFromTemplate(data) {
  return request({ url: '/zhpg/indicator/from-template', method: 'post', data })
}

export function reorderIndicator(data) {
  return request({ url: '/zhpg/indicator/reorder', method: 'post', data })
}

// ========== 新增接口 ==========

/**
 * 多条件指标查询（支持模糊/精确切换）
 */
export function queryIndicators(queryDTO) {
  return request({ url: '/zhpg/indicator/query', method: 'post', data: queryDTO })
}

/**
 * 批量更新指标
 */
export function batchUpdateIndicators(indicators) {
  return request({ url: '/zhpg/indicator/batchUpdate', method: 'put', data: indicators })
}

/**
 * 获取指标变更历史
 */
export function getIndicatorChangeLogs(id) {
  return request({ url: '/zhpg/indicator/' + id + '/changeLogs', method: 'get' })
}

/**
 * 向导式模板实例化
 */
export function instantiateByWizard(id, dto) {
  return request({ url: '/zhpg/indicator/' + id + '/instantiateTemplate', method: 'post', data: dto })
}
