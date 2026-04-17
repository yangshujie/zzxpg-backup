import request from '@/utils/request'

const baseUrl = '/systemPlus/systemCooperation/taskManagement'

export function listTaskManagement(query) {
  return request({
    url: `${baseUrl}/list`,
    method: 'get',
    params: query
  })
}

export function getTaskManagement(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'get'
  })
}

export function addTaskManagement(data) {
  return request({
    url: baseUrl,
    method: 'post',
    data
  })
}

export function updateTaskManagement(data) {
  return request({
    url: baseUrl,
    method: 'put',
    data
  })
}

export function delTaskManagement(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'delete'
  })
}
