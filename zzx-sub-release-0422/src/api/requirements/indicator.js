import request from './request'

/**
 * 保存/更新指标体系树结构
 * @param {Object} data 指标树数据
 * @param {number|string} data.requirementId 需求ID
 * @param {Array} data.treeData 指标树层级结构
 * @returns {Promise} { code, msg, data }
 */
export function saveIndicatorTree(data) {
  return request({
    url: '/indicator/saveTree',
    method: 'post',
    data: data
  })
}

/**
 * 广播指标体系树结构
 * @param {number|string} requirementId 需求ID
 * @returns {Promise} { code, msg, data: [] }
 */
export function broadcastIndicatorTree(requirementId) {
  return request({
    url: `/indicator/broadcast/${requirementId}`,
    method: 'post',
  })
}

/**
 * 获取指定需求的指标树结构
 * @param {number|string} requirementId 需求ID
 * @returns {Promise} { code, msg, data: [] }
 */
export function getIndicatorTree(requirementId) {
  return request({
    url: '/indicator/tree',
    method: 'get',
    params: { requirementId }
  })
}
