import request from './request'

export function listEvalReports(query) {
  return request({
    url: '/zhpg/evalReport/list',
    method: 'get',
    params: query
  })
}

export function listEvalReportsByResult(evalResultId) {
  return request({
    url: '/zhpg/evalReport/result/' + evalResultId + '/list',
    method: 'get'
  })
}

/**
 * 异步生成报告（返回 PENDING 实例，实际生成在后台执行）
 */
export function generateEvalReport(evalResultId, data) {
  return request({
    url: '/zhpg/evalReport/result/' + evalResultId,
    method: 'post',
    data,
    timeout: 10000
  })
}

/**
 * 快速渲染 HTML 预览（不生成 DOCX/PDF）
 */
export function previewEvalReportHtml(evalResultId, data) {
  return request({
    url: '/zhpg/evalReport/result/' + evalResultId + '/preview',
    method: 'post',
    data,
    timeout: 15000
  })
}

/**
 * 轮询报告生成进度
 */
export function getEvalReportProgress(reportId) {
  return request({
    url: '/zhpg/evalReport/' + reportId + '/progress',
    method: 'get',
    timeout: 5000
  })
}

export function getEvalReportLinks(reportId) {
  return request({
    url: '/zhpg/evalReport/' + reportId + '/links',
    method: 'get'
  })
}

export function delEvalReport(ids) {
  return request({
    url: '/zhpg/evalReport/' + ids,
    method: 'delete'
  })
}
