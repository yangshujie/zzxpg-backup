import request from '@/utils/request'

// 查询评估结果列表（分页）
export function listEvalResult(query) {
  return request({
    url: '/zhpg/evalResult/list',
    method: 'get',
    params: query
  })
}

// 获取评估结果统计
export function getEvalResultStats() {
  return request({
    url: '/zhpg/evalResult/stats',
    method: 'get'
  })
}

// 获取评估结果详情
export function getEvalResult(id) {
  return request({
    url: '/zhpg/evalResult/' + id,
    method: 'get'
  })
}

/** 报告文件在 MinIO，经文件服务换取可预览/下载的 URL */
export function getEvalResultReportPreviewUrl(id) {
  return request({
    url: '/zhpg/evalResult/' + id + '/reportPreviewUrl',
    method: 'get'
  })
}

export function getEvalResultReportLinks(id) {
  return request({
    url: '/zhpg/evalResult/' + id + '/reportLinks',
    method: 'get'
  })
}

// 新增评估结果
export function addEvalResult(data) {
  return request({
    url: '/zhpg/evalResult',
    method: 'post',
    data: data
  })
}

// 修改评估结果
export function updateEvalResult(data) {
  return request({
    url: '/zhpg/evalResult',
    method: 'put',
    data: data
  })
}

// 删除评估结果
export function delEvalResult(ids) {
  return request({
    url: '/zhpg/evalResult/' + ids,
    method: 'delete'
  })
}

// 发布评估结果
export function publishEvalResult(id) {
  return request({
    url: '/zhpg/evalResult/' + id + '/publish',
    method: 'post'
  })
}

// 导出评估结果
export function exportEvalResult(query) {
  return request({
    url: '/zhpg/evalResult/export',
    method: 'post',
    params: query,
    responseType: 'blob'
  })
}
