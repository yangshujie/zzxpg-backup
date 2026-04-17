import request from "@/utils/equRequest";

const algorithmBaseURL = "/algorithmManagement";

const apis = {
  // 算法管理接口
  insertAlgorithm: algorithmBaseURL + "/insert", // 新增算法
  deleteAlgorithm: algorithmBaseURL + "/delete/{ids}", // 按ID删除算法
  deleteAllAlgorithm: algorithmBaseURL + "/deleteAll", // 清空所有算法
  updateAlgorithm: algorithmBaseURL + "/update", // 更新算法
  getAlgorithm: algorithmBaseURL + "/get/{id}", // 按ID查询算法
  listAlgorithm: algorithmBaseURL + "/list", // 分页查询算法列表
};

// ==================== 算法管理接口 ====================

/**
 * 新增算法
 * @param {Object} data - 算法数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertAlgorithm(data) {
  return request({
    url: apis.insertAlgorithm,
    method: "post",
    data: data,
  });
}

/**
 * 按ID删除算法
 * @param {string|number} ids - 要删除的算法ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAlgorithm(ids) {
  return request({
    url: apis.deleteAlgorithm.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空所有算法
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllAlgorithm() {
  return request({
    url: apis.deleteAllAlgorithm,
    method: "delete",
  });
}

/**
 * 更新算法
 * @param {Object} data - 算法数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateAlgorithm(data) {
  return request({
    url: apis.updateAlgorithm,
    method: "put",
    data: data,
  });
}

/**
 * 按ID查询算法
 * @param {string|number} id - 要查询的算法ID
 * @return {Promise} - 返回包含算法详情的Promise对象
 */
export function getAlgorithm(id) {
  return request({
    url: apis.getAlgorithm.replace('{id}', id),
    method: "get",
  });
}

/**
 * 分页查询算法列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含算法列表的Promise对象
 */
export function listAlgorithm(query) {
  return request({
    url: apis.listAlgorithm,
    method: "get",
    params: query,
  });
}