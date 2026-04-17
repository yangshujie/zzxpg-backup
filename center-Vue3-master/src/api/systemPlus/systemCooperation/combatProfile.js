import request from '@/utils/request'

const baseUrl = '/systemPlus/systemCooperation/combatProfile'

export function listCombatProfile(query) {
  return request({
    url: `${baseUrl}/list`,
    method: 'get',
    params: query
  })
}

export function getCombatProfile(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'get'
  })
}

export function addCombatProfile(data) {
  return request({
    url: baseUrl,
    method: 'post',
    data
  })
}

export function updateCombatProfile(data) {
  return request({
    url: baseUrl,
    method: 'put',
    data
  })
}

export function saveOrUpdateCombatProfile(data) {
  return request({
    url: `${baseUrl}/saveOrUpdate`,
    method: 'post',
    data
  })
}

export function delCombatProfile(id) {
  return request({
    url: `${baseUrl}/${id}`,
    method: 'delete'
  })
}
