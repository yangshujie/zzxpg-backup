import request from './request'

/**
 * =============================================================================
 * 采集表单管理 (Collection Form Management)
 * =============================================================================
 */

/**
 * 分页查询采集表单
 * @param {Object} query 查询参数
 * @param {string} [query.collectStatus] 采集状态
 * @param {string} [query.formCode] 采集表单识别码
 * @param {string} [query.formName] 采集表单名称
 * @param {string} [query.invokeType] 引接类型
 * @param {string} [query.requireCode] 需求标识
 * @param {string} [query.requireName] 需求名称
 */
export function listCollectForms(query) {
  return request({
    url: '/collectForm/list',
    method: 'get',
    params: query
  })
}

/**
 * 获取采集表单详情（含数据项列表）
 * @param {number|string} id 主键ID
 */
export function getCollectForm(id) {
  return request({
    url: '/collectForm/' + id,
    method: 'get'
  })
}

/**
 * 新增采集表单
 * @param {Object} data 表单数据
 */
export function addCollectForm(data) {
  return request({
    url: '/collectForm',
    method: 'post',
    data: data
  })
}

/**
 * 修改采集表单
 * @param {Object} data 表单数据
 */
export function updateCollectForm(data) {
  return request({
    url: '/collectForm',
    method: 'put',
    data: data
  })
}

/**
 * 删除采集表单
 * @param {string} ids 主键ID，多个用逗号分隔
 */
export function delCollectForms(ids) {
  return request({
    url: '/collectForm/' + ids,
    method: 'delete'
  })
}

/**
 * 下发采集表单到分中心
 * @param {number|string} id 采集表单主键ID
 */
export function issueForm(id) {
  return request({
    url: '/collectForm/issue/' + id,
    method: 'post'
  })
}

/**
 * 批量下发采集表单
 * @param {Object} data { ids: [] }
 */
export function batchIssueForms(data) {
  return request({
    url: '/collectForm/issue/batch',
    method: 'post',
    data: data
  })
}

/**
 * 变更采集状态
 * @param {number|string} id 主键ID
 * @param {string} status 状态：NOT_ISSUED/COLLECTING/COMPLETED
 */
export function changeFormStatus(id, status) {
  return request({
    url: '/collectForm/changeStatus/' + id,
    method: 'put',
    params: { status }
  })
}

/**
 * 接收评估需求并按分中心生成采集表单
 * @param {Object} data 评估需求表对象
 */
export function generateFormItems(data) {
  return request({
    url: '/collectForm/generateItems',
    method: 'post',
    data: data
  })
}

/**
 * =============================================================================
 * 评估数据管理 (Evaluation Data Management)
 * =============================================================================
 */

/**
 * 查询评估数据列表
 * @param {Object} query { evaluationTaskId, indicatorCodes }
 */
export function listEvaluationData(query) {
  return request({
    url: '/evaluationData/list',
    method: 'get',
    params: query
  })
}

/**
 * 获取评估数据详情
 * @param {number|string} id 主键ID
 */
export function getEvaluationData(id) {
  return request({
    url: '/evaluationData/' + id,
    method: 'get'
  })
}

/**
 * 新增评估数据
 * @param {Object} data
 */
export function addEvaluationData(data) {
  return request({
    url: '/evaluationData',
    method: 'post',
    data: data
  })
}

/**
 * 修改评估数据
 * @param {Object} data
 */
export function updateEvaluationData(data) {
  return request({
    url: '/evaluationData',
    method: 'put',
    data: data
  })
}

/**
 * 删除评估数据
 * @param {string} ids 主键ID，多个用逗号分隔
 */
export function delEvaluationData(ids) {
  return request({
    url: '/evaluationData/' + ids,
    method: 'delete'
  })
}

/**
 * =============================================================================
 * 汇总整编任务管理 (Integration Task Management)
 * =============================================================================
 */

/**
 * 分页查询汇总整编任务
 * @param {Object} query { pageNum, pageSize, status, taskName }
 */
export function listIntegrationTasks(query) {
  return request({
    url: '/integration/task/list',
    method: 'get',
    params: query
  })
}

/**
 * 获取整编任务详细信息
 * @param {number|string} taskId
 */
export function getIntegrationTask(taskId) {
  return request({
    url: '/integration/task/' + taskId,
    method: 'get'
  })
}

/**
 * 新增汇总整编任务
 * @param {Object} data
 */
export function addIntegrationTask(data) {
  return request({
    url: '/integration/task',
    method: 'post',
    data: data
  })
}

/**
 * 修改汇总整编任务
 * @param {Object} data
 */
export function updateIntegrationTask(data) {
  return request({
    url: '/integration/task',
    method: 'put',
    data: data
  })
}

/**
 * 删除汇总整编任务
 * @param {string} taskIds
 */
export function delIntegrationTasks(taskIds) {
  return request({
    url: '/integration/task/' + taskIds,
    method: 'delete'
  })
}

/**
 * 手动触发整编任务执行
 * @param {number|string} taskId
 */
export function triggerIntegrationTask(taskId) {
  return request({
    url: '/integration/task/trigger/' + taskId,
    method: 'post'
  })
}

/**
 * =============================================================================
 * 接口信息管理 (Interface Info Management)
 * =============================================================================
 */

/**
 * 分页查询接口信息
 * @param {Object} query { pageNum, pageSize, ... }
 */
export function listInterfaceInfosPage(query) {
  return request({
    url: '/pgzzx/sjcl/interface/info/page',
    method: 'get',
    params: query
  })
}

/**
 * 查询接口详情
 * @param {number|string} id
 */
export function getInterfaceInfo(id) {
  return request({
    url: '/pgzzx/sjcl/interface/info/' + id,
    method: 'get'
  })
}

/**
 * 新增接口
 * @param {Object} data
 */
export function addInterfaceInfo(data) {
  return request({
    url: '/pgzzx/sjcl/interface/info',
    method: 'post',
    data: data
  })
}

/**
 * 更新接口
 * @param {Object} data
 */
export function updateInterfaceInfo(data) {
  return request({
    url: '/pgzzx/sjcl/interface/info',
    method: 'put',
    data: data
  })
}

/**
 * 批量删除接口
 * @param {Array} ids
 */
export function batchDelInterfaceInfos(ids) {
  return request({
    url: '/pgzzx/sjcl/interface/info/batch',
    method: 'delete',
    params: { ids }
  })
}

/**
 * 更新引接统计
 * @param {number|string} id
 * @param {number} count
 */
export function updateInterfaceImportCount(id, count) {
  return request({
    url: `/pgzzx/sjcl/interface/info/import/${id}/${count}`,
    method: 'put'
  })
}

/**
 * 更新接口状态
 * @param {number|string} id
 * @param {number} status
 */
export function updateInterfaceStatus(id, status) {
  return request({
    url: `/pgzzx/sjcl/interface/info/status/${id}/${status}`,
    method: 'put'
  })
}

/**
 * =============================================================================
 * 接口监控管理 (Interface Monitor Management)
 * =============================================================================
 */

/**
 * 分页查询监控记录
 * @param {Object} query
 */
export function getMonitorRecordsPage(query) {
  return request({
    url: '/api/interface/monitor/page',
    method: 'get',
    params: query
  })
}

/**
 * 查询最新监控记录
 * @param {number|string} interfaceId
 * @param {number} limit
 */
export function getRecentMonitorRecords(interfaceId, limit = 10) {
  return request({
    url: `/api/interface/monitor/recent/${interfaceId}/${limit}`,
    method: 'get'
  })
}

/**
 * 统计接口监控数据
 */
export function getMonitorStatistics(interfaceId, query) {
  return request({
    url: '/api/interface/monitor/statistics/' + interfaceId,
    method: 'get',
    params: query
  })
}

/**
 * 查询异常记录
 */
export function getAbnormalRecords(threshold = 0) {
  return request({
    url: '/api/interface/monitor/abnormal',
    method: 'get',
    params: { threshold }
  })
}

/**
 * 添加监控记录
 */
export function addMonitorRecord(data) {
  return request({
    url: '/api/interface/monitor',
    method: 'post',
    data: data
  })
}

/**
 * 批量添加监控记录
 */
export function batchAddMonitorRecords(data) {
  return request({
    url: '/api/interface/monitor/batch',
    method: 'post',
    data: data
  })
}
