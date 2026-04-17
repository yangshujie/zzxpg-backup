import request from '@/utils/request'

export function listIndicatorSystemTemplate(query) {
  return request({ url: '/zhpg/indicatorSystemTemplate/list', method: 'get', params: query })
}

export function getIndicatorSystemTemplate(id) {
  return request({ url: '/zhpg/indicatorSystemTemplate/' + id, method: 'get' })
}

export function addIndicatorSystemTemplate(data) {
  return request({ url: '/zhpg/indicatorSystemTemplate', method: 'post', data })
}

export function updateIndicatorSystemTemplate(data) {
  return request({ url: '/zhpg/indicatorSystemTemplate', method: 'put', data })
}

export function delIndicatorSystemTemplate(id) {
  return request({ url: '/zhpg/indicatorSystemTemplate/' + id, method: 'delete' })
}

export function applyIndicatorSystemTemplate(id) {
  return request({ url: '/zhpg/indicatorSystemTemplate/' + id + '/apply', method: 'get' })
}

export function publishIndicatorSystemTemplate(id) {
  return request({ url: '/zhpg/indicatorSystemTemplate/' + id + '/publish', method: 'post' })
}

export function exportIndicatorSystemTemplateJson(id) {
  return request({ url: '/zhpg/indicatorSystemTemplate/' + id + '/exportJson', method: 'get' })
}

export function importIndicatorSystemTemplateJson(data) {
  return request({ url: '/zhpg/indicatorSystemTemplate/importJson', method: 'post', data })
}

export function listIndicatorSystemTemplateVersions(id) {
  return request({ url: '/zhpg/indicatorSystemTemplate/' + id + '/versions', method: 'get' })
}

export function compareIndicatorSystemTemplate(leftId, rightId) {
  return request({ url: '/zhpg/indicatorSystemTemplate/compare', method: 'get', params: { leftId, rightId } })
}

export function listIndicatorSystemTemplateLogs(id) {
  return request({ url: '/zhpg/indicatorSystemTemplate/' + id + '/logs', method: 'get' })
}
