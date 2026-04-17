import request from './request'

/**
 * 分页查询评估需求列表
 * @param {Object} query 查询参数
 * @param {number} [query.pageNum] 页码
 * @param {number} [query.pageSize] 条数
 * @param {string} [query.requirementName] 需求名称
 * @returns {Promise} { code, msg, data: { records: [], total, size, current } }
 */
export function listEvaluationRequirement(query) {
  return request({
    url: '/evaluationRequirement/list',
    method: 'get',
    params: query
  })
}

/**
 * 获取评估需求基础信息
 * @param {number|string} id 需求ID
 * @returns {Promise} { code, msg, data: { requirementName, ...基础信息 } }
 */
export function getEvaluationRequirement(id) {
  return request({
    url: '/evaluationRequirement/get/' + id,
    method: 'get'
  })
}

/**
 * 获取评估需求详细信息 (含指标树结构)
 * @param {number|string} id 需求ID
 * @returns {Promise} { code, msg, data: { requirementName, treeData: [], ...详细信息 } }
 */
export function getDetailedRequirement(id) {
  return request({
    url: '/evaluationRequirement/getDetailed/' + id,
    method: 'get'
  })
}

/**
 * 新增评估需求
 * @param {Object} data 需求数据
 * @param {number} data.projectId 关联评估工程ID
 * @param {number} [data.designType] 设计类型 1独立设计 2协同设计
 * @param {number} [data.status] 状态 1草稿 2已发布
 * @param {string[]} [data.equipList] 待评装备列表
 * @param {string[]} [data.supportEquip] 陪试装备列表
 * @param {string[]} [data.evaluationGoal] 评估目标列表
 * @param {number} [data.evaluationType] 评估类型 1效能 2贡献率 3成熟度 4适用性
 * @param {number} [data.evaluationLevel] 评估级别 1装备 2系统 3体系 4任务
 * @param {string} [data.evaluationPurpose] 评估目的说明
 * @param {string[]} [data.testType] 试验类型列表
 * @param {Object} [data.treeData] 指标树数据 (JSON对象)
 * @returns {Promise} { code, msg, data }
 */
export function addEvaluationRequirement(data) {
  return request({
    url: '/evaluationRequirement/add',
    method: 'post',
    data: data
  })
}

/**
 * 修改评估需求
 * @param {Object} data 需求数据
 * @param {number|string} data.requirementId 需求ID
 * @param {number} [data.projectId] 关联评估工程ID
 * @param {number} [data.designType] 1独立设计 2协同设计
 * @param {number} [data.status] 1草稿 2已发布
 * @param {string[]} [data.equipList] 待评装备列表
 * @param {string[]} [data.supportEquip] 陪试装备列表
 * @param {string[]} [data.evaluationGoal] 评估目标列表
 * @param {number} [data.evaluationType] 评估类型
 * @param {number} [data.evaluationLevel] 评估级别
 * @param {string} [data.evaluationPurpose] 评估目的
 * @param {string[]} [data.testType] 试验类型
 * @param {Object} [data.treeData] 更新后的指标树数据
 * @returns {Promise} { code, msg, data }
 */
export function updateEvaluationRequirement(data) {
  return request({
    url: '/evaluationRequirement/update',
    method: 'put',
    data: data
  })
}

/**
 * 批量删除评估需求
 * @param {string} ids 逗号分隔的ID字符串
 * @returns {Promise} { code, msg, data }
 */
export function delEvaluationRequirement(ids) {
  return request({
    url: '/evaluationRequirement/batchDelete/' + ids,
    method: 'delete'
  })
}

/**
 * 需求分析下发指标体系
 * @param {number|string} id 需求ID
 * @returns {Promise}
 */
export function analysisSendTree(id) {
  return request({
    url: '/evaluationRequirement/analysisSendTree/' + id,
    method: 'post'
  })
}

/**
 * 会签管理新增接口 (分中心调用填写指标体系)
 * @param {Object} data 会签管理数据
 * @param {number} data.requirementId 对应需求ID
 * @param {string} [data.subCenter] 分中心标识
 * @param {number} [data.status] 会签状态 1拒绝 2填写中 3已填写上报
 * @param {Object} [data.issueData] 下发数据
 * @param {Object} [data.reportData] 上报数据
 * @param {string} [data.remark] 备注(描述)
 * @returns {Promise}
 */
export function addRequirementCountersign(data) {
  return request({
    url: '/requirementCountersign/add',
    method: 'post',
    data: data
  })
}

/**
 * 汇集分中心上报的指标体系数据
 * @param {number|string} requirementId 需求ID
 * @returns {Promise}
 */
export function gatherCountersignData(requirementId) {
  return request({
    url: '/requirementCountersign/gatherData/' + requirementId,
    method: 'get'
  })
}

/**
 * 需求对应会签详情接口
 * @param {number|string} requirementId 需求ID
 * @returns {Promise}
 */
export function getRequirementCountersign(requirementId) {
  return request({
    url: '/requirementCountersign/get/' + requirementId,
    method: 'get'
  })
}