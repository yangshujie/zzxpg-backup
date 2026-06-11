import request from './request'

// 分页查询任务模板列表
export function listEvalTaskTemplate(query) {
  return request({ url: '/zhpg/evalTaskTemplate/list', method: 'get', params: query })
}

// 获取任务模板详情
export function getEvalTaskTemplate(id) {
  return request({ url: '/zhpg/evalTaskTemplate/' + id, method: 'get' })
}

// 新增任务模板
export function addEvalTaskTemplate(data) {
  return request({ url: '/zhpg/evalTaskTemplate', method: 'post', data })
}

// 修改任务模板
export function updateEvalTaskTemplate(data) {
  return request({ url: '/zhpg/evalTaskTemplate', method: 'put', data })
}

// 删除任务模板（支持批量，逗号分隔）
export function delEvalTaskTemplate(ids) {
  return request({ url: '/zhpg/evalTaskTemplate/' + ids, method: 'delete' })
}

// 停用模板（PUBLISHED -> DISABLED）
export function disableEvalTaskTemplate(id) {
  return request({ url: '/zhpg/evalTaskTemplate/' + id + '/disable', method: 'put' })
}

// 启用模板（DISABLED/TESTING -> PUBLISHED）
export function enableEvalTaskTemplate(id) {
  return request({ url: '/zhpg/evalTaskTemplate/' + id + '/enable', method: 'put' })
}
