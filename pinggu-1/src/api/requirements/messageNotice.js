import request from './request'

/**
 * 分页查询消息通知列表
 * @param {Object} query 查询参数
 * @param {number} [query.pageNum] 页码
 * @param {number} [query.pageSize] 条数
 * @param {string} [query.title] 消息标题
 * @returns {Promise} { code, msg, data: { rows: [], total } }
 */
export function listReminderMessage(query) {
  return request({
    url: '/reminderMessage/list',
    method: 'get',
    params: query
  })
}

/**
 * 获取消息详情
 * @param {number|string} id 消息ID
 * @returns {Promise} { code, msg, data: { title, content, sendTime, ... } }
 */
export function getReminderMessage(id) {
  return request({
    url: '/reminderMessage/get/' + id,
    method: 'get'
  })
}

/**
 * 新增消息通知
 * @param {Object} data 消息数据
 * @param {string} data.title 标题
 * @param {string} data.content 内容
 * @param {string} [data.receiver] 接收人ID/标识
 * @returns {Promise} { code, msg, data }
 */
export function addReminderMessage(data) {
  return request({
    url: '/reminderMessage/add',
    method: 'post',
    data: data
  })
}

/**
 * 修改消息通知内容
 * @param {Object} data 消息数据
 * @param {number|string} data.id 消息ID
 * @param {string} [data.title] 标题
 * @param {string} [data.content] 内容
 * @returns {Promise} { code, msg, data }
 */
export function updateReminderMessage(data) {
  return request({
    url: '/reminderMessage/update',
    method: 'put',
    data: data
  })
}

/**
 * 批量删除消息记录
 * @param {string} ids 逗号分隔的ID字符串
 * @returns {Promise} { code, msg, data }
 */
export function delReminderMessage(ids) {
  return request({
    url: '/reminderMessage/remove/' + ids,
    method: 'delete'
  })
}
