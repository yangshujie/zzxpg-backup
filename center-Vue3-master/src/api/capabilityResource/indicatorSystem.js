import request from '@/utils/request'

const baseUrl = '/capabilityResource/indicatorSystem'

export function listIndicatorSystem(query) {
  return request({
    url: `${baseUrl}/list`,
    method: 'get',
    params: query
  })
}

export function getIndicatorSystem(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'get'
  })
}

export function addIndicatorSystem(data) {
  return request({
    url: baseUrl,
    method: 'post',
    data
  })
}

export function updateIndicatorSystem(data) {
  return request({
    url: baseUrl,
    method: 'put',
    data
  })
}

export function delIndicatorSystem(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'delete'
  })
}

export function autoOptimizeWeight(id) {
  return request({
    url: `${baseUrl}/${id}/autoOptimize`,
    method: 'post'
  })
}
