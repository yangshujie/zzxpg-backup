import request from '@/utils/request'

// 查询指标列表
export function listIndicator(query) {
  return request({
    url: '/system/indicator/list',
    method: 'get',
    params: query
  })
}

// 查询指标详细
export function getIndicator(id) {
  return request({
    url: '/system/indicator/' + id,
    method: 'get'
  })
}

// 新增指标
export function addIndicator(data) {
  return request({
    url: '/system/indicator',
    method: 'post',
    data: data
  })
}

// 修改指标
export function updateIndicator(data) {
  return request({
    url: '/system/indicator',
    method: 'put',
    data: data
  })
}

// 删除指标
export function delIndicator(id) {
  return request({
    url: '/system/indicator/' + id,
    method: 'delete'
  })
}

// 导出指标
export function exportIndicator(query) {
  return request({
    url: '/system/indicator/export',
    method: 'get',
    params: query
  })
}