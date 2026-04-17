import request from '@/utils/request'

const baseUrl = '/systemPlus/systemCooperation/taskCriterion'

export function listTaskCriterion(query) {
  return request({
    url: `${baseUrl}/list`,
    method: 'get',
    params: query
  })
}

export function getTaskCriterion(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'get'
  })
}

export function addTaskCriterion(data) {
  return request({
    url: baseUrl,
    method: 'post',
    data
  })
}

export function updateTaskCriterion(data) {
  return request({
    url: baseUrl,
    method: 'put',
    data
  })
}

export function delTaskCriterion(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'delete'
  })
}
