import request from "@/utils/equRequest";

const baseURL = "/oodaPath";
const nodeBaseURL = "/oodaPathNode";
const networkBaseURL = "/oodaNetwork"; // 新增OODA网络基础路径
const apis = {
  // OODA链路管理接口
  deleteOodaPath: baseURL + "/delete/{ids}", // 按ID删除OODA链路
  deleteAllOodaPath: baseURL + "/deleteAll", // 清空OODA链路
  getOodaPath: baseURL + "/get/{id}", // 按ID查询OODA链路
  insertOodaPath: baseURL + "/insert", // 新增OODA链路
  listOodaPath: baseURL + "/list", // 分页查询OODA链路列表
  updateOodaPath: baseURL + "/update", // 更新OODA链路
  
  // OODA链路节点管理接口
  deleteOodaPathNode: nodeBaseURL + "/delete/{ids}", // 按ID删除OODA链路节点
  deleteAllOodaPathNode: nodeBaseURL + "/deleteAll", // 清空OODA链路节点
  getOodaPathNode: nodeBaseURL + "/get/{id}", // 按ID查询OODA链路节点
  insertOodaPathNode: nodeBaseURL + "/insert", // 新增OODA链路节点
  listOodaPathNode: nodeBaseURL + "/list", // 分页查询OODA链路节点列表
  updateOodaPathNode: nodeBaseURL + "/update", // 更新OODA链路节点
  
  // OODA网络管理接口
  deleteOodaNetwork: networkBaseURL + "/delete/{ids}", // 按ID删除OODA网络
  deleteAllOodaNetwork: networkBaseURL + "/deleteAll", // 清空OODA网络
  getOodaNetwork: networkBaseURL + "/get/{id}", // 按ID查询OODA网络
  insertOodaNetwork: networkBaseURL + "/insert", // 新增OODA网络
  listOodaNetwork: networkBaseURL + "/list", // 分页查询OODA网络列表
  updateOodaNetwork: networkBaseURL + "/update", // 更新OODA网络
};

/**
 * 按ID删除OODA链路
 * @param {string|number} ids - 要删除的OODA链路ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteOodaPath(ids) {
  return request({
    url: apis.deleteOodaPath.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空OODA链路
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllOodaPath() {
  return request({
    url: apis.deleteAllOodaPath,
    method: "delete",
  });
}

/**
 * 按ID查询OODA链路
 * @param {string|number} id - 要查询的OODA链路ID
 * @return {Promise} - 返回包含OODA链路详情的Promise对象
 */
export function getOodaPath(id) {
  return request({
    url: apis.getOodaPath.replace('{id}', id),
    method: "get",
  });
}

/**
 * 新增OODA链路
 * @param {Object} data - OODA链路数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertOodaPath(data) {
  return request({
    url: apis.insertOodaPath,
    method: "post",
    data: data,
  });
}

/**
 * 分页查询OODA链路列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含OODA链路列表的Promise对象
 */
export function listOodaPath(query) {
  return request({
    url: apis.listOodaPath,
    method: "get",
    params: query,
  });
}

/**
 * 更新OODA链路
 * @param {Object} data - OODA链路数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateOodaPath(data) {
  return request({
    url: apis.updateOodaPath,
    method: "put",
    data: data,
  });
}

/**
 * 按ID删除OODA链路节点
 * @param {string|number} ids - 要删除的OODA链路节点ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteOodaPathNode(ids) {
  return request({
    url: apis.deleteOodaPathNode.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空OODA链路节点
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllOodaPathNode() {
  return request({
    url: apis.deleteAllOodaPathNode,
    method: "delete",
  });
}

/**
 * 按ID查询OODA链路节点
 * @param {string|number} id - 要查询的OODA链路节点ID
 * @return {Promise} - 返回包含OODA链路节点详情的Promise对象
 */
export function getOodaPathNode(id) {
  return request({
    url: apis.getOodaPathNode.replace('{id}', id),
    method: "get",
  });
}

/**
 * 新增OODA链路节点
 * @param {Object} data - OODA链路节点数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertOodaPathNode(data) {
  return request({
    url: apis.insertOodaPathNode,
    method: "post",
    data: data,
  });
}

/**
 * 分页查询OODA链路节点列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含OODA链路节点列表的Promise对象
 */
export function listOodaPathNode(query) {
  return request({
    url: apis.listOodaPathNode,
    method: "get",
    params: query,
    timeout: 60000,
  });
}

/**
 * 更新OODA链路节点
 * @param {Object} data - OODA链路节点数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateOodaPathNode(data) {
  return request({
    url: apis.updateOodaPathNode,
    method: "put",
    data: data,
  });
}

// ==================== OODA网络管理接口 ====================

/**
 * 按ID删除OODA网络
 * @param {string|number} ids - 要删除的OODA网络ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteOodaNetwork(ids) {
  return request({
    url: apis.deleteOodaNetwork.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空OODA网络
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllOodaNetwork() {
  return request({
    url: apis.deleteAllOodaNetwork,
    method: "delete",
  });
}

/**
 * 按ID查询OODA网络
 * @param {string|number} id - 要查询的OODA网络ID
 * @return {Promise} - 返回包含OODA网络详情的Promise对象
 */
export function getOodaNetwork(id) {
  return request({
    url: apis.getOodaNetwork.replace('{id}', id),
    method: "get",
  });
}

/**
 * 新增OODA网络
 * @param {Object} data - OODA网络数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertOodaNetwork(data) {
  return request({
    url: apis.insertOodaNetwork,
    method: "post",
    data: data,
  });
}

/**
 * 分页查询OODA网络列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含OODA网络列表的Promise对象
 */
export function listOodaNetwork(query) {
  return request({
    url: apis.listOodaNetwork,
    method: "get",
    params: query,
    timeout: 60000,
   });
}

/**
 * 更新OODA网络
 * @param {Object} data - OODA网络数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateOodaNetwork(data) {
  return request({
    url: apis.updateOodaNetwork,
    method: "put",
    data: data,
  });
}