import request from "@/utils/equRequest";

const indicatorBaseURL = "/indicator";
const indicatorSystemBaseURL = "/indicatorSystem";

const apis = {
  // 指标管理接口
  deleteIndicator: indicatorBaseURL + "/delete/{ids}", // 按ID删除指标
  deleteAllIndicator: indicatorBaseURL + "/deleteAll", // 清空所有指标
  getIndicator: indicatorBaseURL + "/get/{id}", // 按ID查询指标
  insertIndicator: indicatorBaseURL + "/insert", // 新增指标
  listIndicator: indicatorBaseURL + "/list", // 分页查询指标列表
  updateIndicator: indicatorBaseURL + "/update", // 更新指标
  
  // 指标体系管理接口
  deleteIndicatorSystem: indicatorSystemBaseURL + "/delete/{ids}", // 按ID删除指标体系节点
  deleteAllIndicatorSystem: indicatorSystemBaseURL + "/deleteAll", // 清空所有指标体系节点
  updateIndicatorSystem: indicatorSystemBaseURL + "/update", // 更新指标体系节点
  getIndicatorSystem: indicatorSystemBaseURL + "/get/{id}", // 按ID查询指标体系节点
  treeIndicatorSystem: indicatorSystemBaseURL + "/tree", // 分页查询完整指标体系树
  insertTreeIndicatorSystem: indicatorSystemBaseURL + "/insertTree", // 新增完整指标体系树
};

// ==================== 指标管理接口 ====================

/**
 * 按ID删除指标
 * @param {string|number} ids - 要删除的指标ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteIndicator(ids) {
  return request({
    url: apis.deleteIndicator.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空所有指标
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllIndicator() {
  return request({
    url: apis.deleteAllIndicator,
    method: "delete",
  });
}

/**
 * 按ID查询指标
 * @param {string|number} id - 要查询的指标ID
 * @return {Promise} - 返回包含指标详情的Promise对象
 */
export function getIndicator(id) {
  return request({
    url: apis.getIndicator.replace('{id}', id),
    method: "get",
  });
}

/**
 * 新增指标
 * @param {Object} data - 指标数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertIndicator(data) {
  return request({
    url: apis.insertIndicator,
    method: "post",
    data: data,
  });
}

/**
 * 分页查询指标列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含指标列表的Promise对象
 */
export function listIndicator(query) {
  return request({
    url: apis.listIndicator,
    method: "get",
    params: query,
  });
}

/**
 * 更新指标
 * @param {Object} data - 指标数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateIndicator(data) {
  return request({
    url: apis.updateIndicator,
    method: "put",
    data: data,
  });
}

// ==================== 指标体系管理接口 ====================

/**
 * 按ID删除指标体系节点
 * @param {string|number} ids - 要删除的指标体系节点ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteIndicatorSystem(ids) {
  return request({
    url: apis.deleteIndicatorSystem.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空所有指标体系节点
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllIndicatorSystem() {
  return request({
    url: apis.deleteAllIndicatorSystem,
    method: "delete",
  });
}

/**
 * 更新指标体系节点
 * @param {Object} data - 指标体系节点数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateIndicatorSystem(data) {
  return request({
    url: apis.updateIndicatorSystem,
    method: "put",
    data: data,
  });
}

/**
 * 按ID查询指标体系节点
 * @param {string|number} id - 要查询的指标体系节点ID
 * @return {Promise} - 返回包含指标体系节点详情的Promise对象
 */
export function getIndicatorSystem(id) {
  return request({
    url: apis.getIndicatorSystem.replace('{id}', id),
    method: "get",
  });
}

/**
 * 分页查询完整指标体系树
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含完整指标体系树的Promise对象
 */
export function treeIndicatorSystem(query) {
  return request({
    url: apis.treeIndicatorSystem,
    method: "get",
    params: query,
  });
}

/**
 * 新增完整指标体系树
 * @param {Object} data - 完整指标体系树数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertTreeIndicatorSystem(data) {
  return request({
    url: apis.insertTreeIndicatorSystem,
    method: "post",
    data: data,
  });
}