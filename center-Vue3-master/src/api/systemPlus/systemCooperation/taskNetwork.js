import request from '@/utils/request'

const baseUrl = '/systemPlus/systemCooperation/taskNetwork'

export function listTaskNetwork(query) {
  return request({
    url: `${baseUrl}/list`,
    method: 'get',
    params: query
  })
}

export function getTaskNetwork(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'get'
  })
}

export function addTaskNetwork(data) {
  return request({
    url: baseUrl,
    method: 'post',
    data
  })
}

export function updateTaskNetwork(data) {
  return request({
    url: baseUrl,
    method: 'put',
    data
  })
}

export function delTaskNetwork(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'delete'
  })
}
