import request from '@/utils/request'

// 分页查询流程模板列表
export function listCalcFlow(query) {
  return request({ url: '/zhpg/calcFlow/list', method: 'get', params: query })
}

// 获取流程模板详情
export function getCalcFlow(id) {
  return request({ url: '/zhpg/calcFlow/' + id, method: 'get' })
}

// 新增流程模板
export function addCalcFlow(data) {
  return request({ url: '/zhpg/calcFlow', method: 'post', data })
}

// 修改流程模板
export function updateCalcFlow(data) {
  return request({ url: '/zhpg/calcFlow', method: 'put', data })
}

// 删除流程模板
export function delCalcFlow(id) {
  return request({ url: '/zhpg/calcFlow/' + id, method: 'delete' })
}

// 提交测试（DRAFT -> TESTING）
export function submitTestCalcFlow(id) {
  return request({ url: '/zhpg/calcFlow/' + id + '/test', method: 'put' })
}

// 发布模板（TESTING -> PUBLISHED）
export function publishCalcFlow(id) {
  return request({ url: '/zhpg/calcFlow/' + id + '/publish', method: 'put' })
}

// 停用模板（PUBLISHED -> DISABLED）
export function disableCalcFlow(id) {
  return request({ url: '/zhpg/calcFlow/' + id + '/disable', method: 'put' })
}

// 启用模板（DISABLED/TESTING -> PUBLISHED）
export function enableCalcFlow(id) {
  return request({ url: '/zhpg/calcFlow/' + id + '/enable', method: 'put' })
}

// 复制新版本
export function copyVersionCalcFlow(id) {
  return request({ url: '/zhpg/calcFlow/' + id + '/copyVersion', method: 'post' })
}
