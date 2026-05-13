import request from '@/utils/request'

// 查询模板列表
export function listTemplates() {
  return request({
    url: '/zhpg/report/templates/list',
    method: 'get'
  })
}

// 查询模板详情
export function getTemplate(id) {
  return request({
    url: '/zhpg/report/templates/' + id,
    method: 'get'
  })
}

// 新增模板
export function createTemplate(data) {
  return request({
    url: '/zhpg/report/templates',
    method: 'post',
    data: data
  })
}

// 修改模板元信息
export function updateTemplate(id, data) {
  return request({
    url: '/zhpg/report/templates/' + id,
    method: 'put',
    data: data
  })
}

// 更新模板内容
export function updateTemplateContent(id, htmlContent) {
  return request({
    url: '/zhpg/report/templates/' + id + '/content',
    method: 'put',
    data: { htmlContent }
  })
}

// 删除模板
export function deleteTemplate(id) {
  return request({
    url: '/zhpg/report/templates/' + id,
    method: 'delete'
  })
}

// 查询版本列表
export function getVersions(id) {
  return request({
    url: '/zhpg/report/templates/' + id + '/versions',
    method: 'get'
  })
}

// 模板校验
export function validateTemplate(data) {
  return request({
    url: '/zhpg/report/templates/validate',
    method: 'post',
    data: data
  })
}

// 预览模板
export function previewTemplate(id, data) {
  return request({
    url: '/zhpg/report/templates/' + id + '/preview',
    method: 'post',
    data: data
  })
}

// 下载报告
export function downloadTemplateFile(id, type, data) {
  return request({
    url: '/zhpg/report/templates/' + id + '/render/' + type,
    method: 'post',
    data: data,
    responseType: 'blob'
  })
}
