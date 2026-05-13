import request from './request'

/**
 * 分页查询评估工程列表
 * @param {Object} query 查询参数
 * @param {number} [query.pageNum] 页码
 * @param {number} [query.pageSize] 条数
 * @param {string} [query.projectName] 工程名称
 * @param {number} [query.projectType] 工程类型 (1: 测控, 2: 通信, 3: 装备)
 * @returns {Promise} { code, msg, data: { rows: [], total } }
 */
export function listProjectEvaluation(query) {
  return request({
    url: '/projectEvaluation/list',
    method: 'get',
    params: query
  })
}

/**
 * 获取评估工程详细信息
 * @param {number|string} id 工程ID
 * @returns {Promise} { code, msg, data: { projectName, projectType, priority, deadline, remark, ... } }
 */
export function getProjectEvaluation(id) {
  return request({
    url: '/projectEvaluation/get/' + id,
    method: 'get'
  })
}

/**
 * 根据评估工程 ID 查询关联需求 ID 列表
 * @param {number|string} projectId 工程ID
 * @returns {Promise} { code, msg, data: number[] }
 */
export function getRequirementIdsByProjectId(projectId) {
  return request({
    url: '/projectEvaluation/getRequirementIdByProjectId/' + projectId,
    method: 'get'
  })
}

/**
 * 新增评估工程
 * @param {Object} data 工程数据
 * @param {string} data.projectName 工程名称
 * @param {number} data.projectType 工程类型
 * @param {number} data.priority 优先级 (1-5)
 * @param {string} data.deadline 截止日期 (YYYY-MM-DD)
 * @param {string} [data.remark] 备注
 * @returns {Promise} { code, msg, data }
 */
export function addProjectEvaluation(data) {
  return request({
    url: '/projectEvaluation/add',
    method: 'post',
    data: data
  })
}

/**
 * 修改评估工程
 * @param {Object} data 工程数据
 * @param {number|string} data.id 工程ID
 * @param {string} [data.projectName] 工程名称
 * @param {number} [data.projectType] 工程类型
 * @param {number} [data.priority] 优先级
 * @param {string} [data.deadline] 截止日期
 * @param {string} [data.remark] 备注
 * @returns {Promise} { code, msg, data }
 */
export function updateProjectEvaluation(data) {
  return request({
    url: '/projectEvaluation/update',
    method: 'put',
    data: data
  })
}

/**
 * 批量删除评估工程
 * @param {string} ids 逗号分隔的ID字符串
 * @returns {Promise} { code, msg, data }
 */
export function delProjectEvaluation(ids) {
  return request({
    url: '/projectEvaluation/batchDelete/' + ids,
    method: 'delete'
  })
}
