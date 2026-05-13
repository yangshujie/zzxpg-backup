import request from "@/utils/equRequest";

const equipmentActivityBaseURL = "/equipmentActivity";
const srActivityDetailBaseURL = "/srActivityDetail";
const ttcActivityDetailBaseURL = "/ttcActivityDetail";
const apis = {
  // 装备活动管理接口
  deleteEquipmentActivity: equipmentActivityBaseURL + "/delete/{ids}", // 按ID批量删除装备活动
  deleteAllEquipmentActivity: equipmentActivityBaseURL + "/deleteAll", // 清空装备活动
  getEquipmentActivity: equipmentActivityBaseURL + "/get/{id}", // 按ID查询装备活动
  insertEquipmentActivity: equipmentActivityBaseURL + "/insert", // 新增装备活动
  listEquipmentActivity: equipmentActivityBaseURL + "/list", // 分页查询装备活动列表
  updateEquipmentActivity: equipmentActivityBaseURL + "/update", // 更新装备活动
  
  // 航天侦察活动细节管理接口
  deleteSrActivityDetail: srActivityDetailBaseURL + "/delete/{ids}", // 按ID删除航天侦察活动细节
  deleteAllSrActivityDetail: srActivityDetailBaseURL + "/deleteAll", // 清空航天侦察活动细节
  getSrActivityDetail: srActivityDetailBaseURL + "/get/{id}", // 按ID查询航天侦察活动细节
  insertSrActivityDetail: srActivityDetailBaseURL + "/insert", // 新增航天侦察活动细节
  listSrActivityDetail: srActivityDetailBaseURL + "/list", // 分页查询航天侦察活动细节列表
  updateSrActivityDetail: srActivityDetailBaseURL + "/update", // 更新航天侦察活动细节
  
  // 航天测运控活动细节管理接口
  deleteTtcActivityDetail: ttcActivityDetailBaseURL + "/delete/{ids}", // 按ID删除航天测运控活动细节
  deleteAllTtcActivityDetail: ttcActivityDetailBaseURL + "/deleteAll", // 清空航天测运控活动细节
  getTtcActivityDetail: ttcActivityDetailBaseURL + "/get/{id}", // 按ID查询航天测运控活动细节
  insertTtcActivityDetail: ttcActivityDetailBaseURL + "/insert", // 新增航天测运控活动细节
  listTtcActivityDetail: ttcActivityDetailBaseURL + "/list", // 分页查询航天测运控活动细节列表
  updateTtcActivityDetail: ttcActivityDetailBaseURL + "/update", // 更新航天测运控活动细节
};

/**
 * 按ID批量删除装备活动
 * @param {string|number} ids - 要删除的装备活动ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteEquipmentActivity(ids) {
  return request({
    url: apis.deleteEquipmentActivity.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空装备活动
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllEquipmentActivity() {
  return request({
    url: apis.deleteAllEquipmentActivity,
    method: "delete",
  });
}

/**
 * 按ID查询装备活动
 * @param {string|number} id - 要查询的装备活动ID
 * @return {Promise} - 返回包含装备活动详情的Promise对象
 */
export function getEquipmentActivity(id) {
  return request({
    url: apis.getEquipmentActivity.replace('{id}', id),
    method: "get",
  });
}

/**
 * 新增装备活动
 * @param {Object} data - 装备活动数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertEquipmentActivity(data) {
  return request({
    url: apis.insertEquipmentActivity,
    method: "post",
    data: data,
  });
}

/**
 * 分页查询装备活动列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含装备活动列表的Promise对象
 */
export function listEquipmentActivity(query) {
  return request({
    url: apis.listEquipmentActivity,
    method: "get",
    params: query,
  });
}

/**
 * 更新装备活动
 * @param {Object} data - 装备活动数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateEquipmentActivity(data) {
  return request({
    url: apis.updateEquipmentActivity,
    method: "put",
    data: data,
  });
}

/**
 * 按ID删除航天侦察活动细节
 * @param {string|number} ids - 要删除的航天侦察活动细节ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteSrActivityDetail(ids) {
  return request({
    url: apis.deleteSrActivityDetail.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空航天侦察活动细节
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllSrActivityDetail() {
  return request({
    url: apis.deleteAllSrActivityDetail,
    method: "delete",
  });
}

/**
 * 按ID查询航天侦察活动细节
 * @param {string|number} id - 要查询的航天侦察活动细节ID
 * @return {Promise} - 返回包含航天侦察活动细节详情的Promise对象
 */
export function getSrActivityDetail(id) {
  return request({
    url: apis.getSrActivityDetail.replace('{id}', id),
    method: "get",
  });
}

/**
 * 新增航天侦察活动细节
 * @param {Object} data - 航天侦察活动细节数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertSrActivityDetail(data) {
  return request({
    url: apis.insertSrActivityDetail,
    method: "post",
    data: data,
  });
}

/**
 * 分页查询航天侦察活动细节列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含航天侦察活动细节列表的Promise对象
 */
export function listSrActivityDetail(query) {
  return request({
    url: apis.listSrActivityDetail,
    method: "get",
    params: query,
  });
}

/**
 * 更新航天侦察活动细节
 * @param {Object} data - 航天侦察活动细节数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateSrActivityDetail(data) {
  return request({
    url: apis.updateSrActivityDetail,
    method: "put",
    data: data,
  });
}

/**
 * 按ID删除航天测运控活动细节
 * @param {string|number} ids - 要删除的航天测运控活动细节ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteTtcActivityDetail(ids) {
  return request({
    url: apis.deleteTtcActivityDetail.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空航天测运控活动细节
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllTtcActivityDetail() {
  return request({
    url: apis.deleteAllTtcActivityDetail,
    method: "delete",
  });
}

/**
 * 按ID查询航天测运控活动细节
 * @param {string|number} id - 要查询的航天测运控活动细节ID
 * @return {Promise} - 返回包含航天测运控活动细节详情的Promise对象
 */
export function getTtcActivityDetail(id) {
  return request({
    url: apis.getTtcActivityDetail.replace('{id}', id),
    method: "get",
  });
}

/**
 * 新增航天测运控活动细节
 * @param {Object} data - 航天测运控活动细节数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertTtcActivityDetail(data) {
  return request({
    url: apis.insertTtcActivityDetail,
    method: "post",
    data: data,
  });
}

/**
 * 分页查询航天测运控活动细节列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含航天测运控活动细节列表的Promise对象
 */
export function listTtcActivityDetail(query) {
  return request({
    url: apis.listTtcActivityDetail,
    method: "get",
    params: query,
  });
}

/**
 * 更新航天测运控活动细节
 * @param {Object} data - 航天测运控活动细节数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateTtcActivityDetail(data) {
  return request({
    url: apis.updateTtcActivityDetail,
    method: "put",
    data: data,
  });
}