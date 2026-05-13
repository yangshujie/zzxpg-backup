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

export function generateEvalReport(evalResultId, data) {
  return request({
    url: '/zhpg/evalReport/result/' + evalResultId,
    method: 'post',
    data,
    timeout: 300000
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
