import request from './request'

/** 分页查询指标 */
export function listIndicator(query) {
  return request({ url: '/zhpg/indicator/list', method: 'get', params: query })
}

/** 不分页查询全部指标（按 indicatorType / parentId 过滤） */
export function listAllIndicator(query) {
  return request({ url: '/zhpg/indicator/listAll', method: 'get', params: query })
}

/** 父节点候选（按指标类型/关键字筛选；excludeId 排除自己 + 子树） */
export function listParentIndicatorCandidates(query) {
  return request({ url: '/zhpg/indicator/parentCandidates', method: 'get', params: query })
}

/** 以 rootId 为根加载子树（指标体系页「从指标库导入」使用） */
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

export function delIndicator(ids) {
  return request({ url: '/zhpg/indicator/' + ids, method: 'delete' })
}

/** 从指标模板生成指标草稿 */
export function instantiateIndicatorFromTemplate(data) {
  return request({ url: '/zhpg/indicator/from-template', method: 'post', data })
}
