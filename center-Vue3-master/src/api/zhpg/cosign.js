import request from '@/utils/request'

export function listCosignTask(query) {
  return request({ url: '/zhpg/cosign/list', method: 'get', params: query })
}

export function getCosignTask(id) {
  return request({ url: '/zhpg/cosign/' + id, method: 'get' })
}

export function addCosignTask(data) {
  return request({ url: '/zhpg/cosign', method: 'post', data })
}

export function updateCosignTask(data) {
  return request({ url: '/zhpg/cosign', method: 'put', data })
}

export function delCosignTask(id) {
  return request({ url: '/zhpg/cosign/' + id, method: 'delete' })
}

export function approveCosignTask(id, data) {
  return request({ url: '/zhpg/cosign/' + id + '/approve', method: 'post', data })
}

export function rejectCosignTask(id, data) {
  return request({ url: '/zhpg/cosign/' + id + '/reject', method: 'post', data })
}
