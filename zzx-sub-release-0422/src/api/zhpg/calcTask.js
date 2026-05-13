import request from './request'

export function listCalcTask(query) {
  return request({ url: '/zhpg/calc/task/list', method: 'get', params: query })
}

export function getCalcTask(id) {
  return request({ url: '/zhpg/calc/task/' + id, method: 'get' })
}

export function runCalcTask(data) {
  return request({
    url: '/zhpg/calc/run',
    method: 'post',
    data,
    timeout: 300000
  })
}
