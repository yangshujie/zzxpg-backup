import { serviceNoAuth as request } from '@/utils/request'

// 分页查询算法需求列表
export function listAlgorithmRequirement(query) {
  return request({ url: '/zhpg/algorithmRequirement/list', method: 'get', params: query })
}

// 查询全部算法需求（不分页）
export function listAllAlgorithmRequirement(query) {
  return request({ url: '/zhpg/algorithmRequirement/listAll', method: 'get', params: query })
}

// 获取算法需求详情（含参数）
export function getAlgorithmRequirement(id) {
  return request({ url: '/zhpg/algorithmRequirement/' + id, method: 'get' })
}

// 新增算法需求
export function addAlgorithmRequirement(data) {
  return request({ url: '/zhpg/algorithmRequirement', method: 'post', data })
}

// 修改算法需求
export function updateAlgorithmRequirement(data) {
  return request({ url: '/zhpg/algorithmRequirement', method: 'put', data })
}

// 删除算法需求
export function delAlgorithmRequirement(id) {
  return request({ url: '/zhpg/algorithmRequirement/' + id, method: 'delete' })
}

// 批量删除算法需求
export function batchDelAlgorithmRequirement(ids) {
  return request({ url: '/zhpg/algorithmRequirement/batch/' + ids, method: 'delete' })
}

// 导出算法需求
export function exportAlgorithmRequirement(query) {
  return request({ url: '/zhpg/algorithmRequirement/export', method: 'post', params: query, responseType: 'blob' })
}

// 更新构建状态
export function updateBuildStatus(id, algorithmName, algorithmId) {
  return request({ url: '/zhpg/algorithmRequirement/build/' + id, method: 'put', params: { algorithmName, algorithmId } })
}

// 更新通知状态
export function updateNotifyStatus(id, notifyStatus) {
  return request({ url: '/zhpg/algorithmRequirement/notify/' + id, method: 'put', params: { notifyStatus } })
}

// 查询需求状态（供分系统调用）
export function getRequirementStatus(id) {
  return request({ url: '/zhpg/algorithmRequirement/status/' + id, method: 'get' })
}

// ================== 分系统对接接口 ==================

// 接收分系统算法需求新增请求
export function receiveRequirementFromSubsystem(data) {
  return request({ url: '/zhpg/algorithmRequirement/receive', method: 'post', data })
}

// 更新分系统算法需求
export function updateRequirementFromSubsystem(data) {
  return request({ url: '/zhpg/algorithmRequirement/receive', method: 'put', data })
}
