import request from '@/utils/request'

// 查询指标体系列表
export function listIndicatorSystem(query) {
  return request({
    url: '/indicatorSystem/list',
    method: 'get',
    params: query
  })
}

// 获取指标体系详情
export function getIndicatorSystem(id) {
  return request({
    url: `/indicatorSystem/${id}`,
    method: 'get'
  })
}

// 新增指标体系
export function addIndicatorSystem(data) {
  return request({
    url: '/indicatorSystem',
    method: 'post',
    data: data
  })
}

// 修改指标体系
export function updateIndicatorSystem(data) {
  return request({
    url: '/indicatorSystem',
    method: 'put',
    data: data
  })
}

// 删除指标体系
export function delIndicatorSystem(id) {
  return request({
    url: `/indicatorSystem/${id}`,
    method: 'delete'
  })
}

// 下载审批
export function downloadApproval(id) {
  return request({
    url: `/indicatorSystem/downloadApproval/${id}`,
    method: 'get',
    responseType: 'blob'
  })
}

// 自动优化权重
export function autoOptimizeWeight(data) {
  return request({
    url: '/indicatorSystem/autoOptimizeWeight',
    method: 'post',
    data: data
  })
}