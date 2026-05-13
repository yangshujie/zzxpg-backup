import request from './request'

export function initCalcFlowExecution(data) {
  return request({
    url: '/zhpg/calcFlowExecution/init',
    method: 'post',
    data
  })
}

export function getCalcFlowExecution(id) {
  return request({
    url: '/zhpg/calcFlowExecution/' + id,
    method: 'get'
  })
}

export function getLatestCalcFlowExecution(query) {
  return request({
    url: '/zhpg/calcFlowExecution/latest',
    method: 'get',
    params: query
  })
}

export function saveCalcFlowExecutionConfig(id, data) {
  return request({
    url: '/zhpg/calcFlowExecution/' + id + '/runtime-config',
    method: 'put',
    data
  })
}

export function runCalcFlowExecution(id, data) {
  return request({
    url: '/zhpg/calcFlowExecution/' + id + '/run',
    method: 'post',
    data,
    timeout: 300000
  })
}
