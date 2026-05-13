import { serviceNoAuth as request } from '@/utils/request'

// 分页查询评估任务需求列表
export function listEvalTaskRequirement(query) {
  return request({ url: '/zhpg/evalTaskRequirement/list', method: 'get', params: query })
}

// 查询全部评估任务需求（不分页）
export function listAllEvalTaskRequirement(query) {
  return request({ url: '/zhpg/evalTaskRequirement/listAll', method: 'get', params: query })
}

// 获取评估任务需求详情
export function getEvalTaskRequirement(id) {
  return request({ url: '/zhpg/evalTaskRequirement/' + id, method: 'get' })
}

// 新增评估任务需求
export function addEvalTaskRequirement(data) {
  return request({ url: '/zhpg/evalTaskRequirement', method: 'post', data })
}

// 修改评估任务需求
export function updateEvalTaskRequirement(data) {
  return request({ url: '/zhpg/evalTaskRequirement', method: 'put', data })
}

// 删除评估任务需求
export function delEvalTaskRequirement(id) {
  return request({ url: '/zhpg/evalTaskRequirement/' + id, method: 'delete' })
}

// 批量删除评估任务需求
export function batchDelEvalTaskRequirement(ids) {
  return request({ url: '/zhpg/evalTaskRequirement/batch/' + ids, method: 'delete' })
}

// 导出评估任务需求
export function exportEvalTaskRequirement(query) {
  return request({ url: '/zhpg/evalTaskRequirement/export', method: 'get', params: query, responseType: 'blob' })
}
