import request from "@/utils/equRequest";

const equipmentBaseURL = "/equipmentInfo";
const groupBaseURL = "/equipmentGroup";
const tacticsBaseURL = "/equipmentSynergyTactics";
const networkBaseURL = "/equipmentNetwork";
const apis = {
  // 装备信息管理接口
  deleteEquipment: equipmentBaseURL + "/delete/{ids}", // 按ID删除设备信息聚合数据
  deleteAllEquipment: equipmentBaseURL + "/deleteAll", // 清空设备信息聚合数据
  getEquipment: equipmentBaseURL + "/get/{id}", // 按ID查询设备信息聚合数据
  insertEquipment: equipmentBaseURL + "/insert", // 新增设备信息聚合数据
  listEquipment: equipmentBaseURL + "/list", // 分页查询设备信息聚合列表
  listEquipmentBySide: equipmentBaseURL + "/listBySide", // 按阵营查询设备信息聚合数据
  updateEquipment: equipmentBaseURL + "/update", // 更新设备信息聚合数据
  
  // 装备编组管理接口
  deleteEquipmentGroup: groupBaseURL + "/delete/{ids}", // 按ID删除装备编组
  deleteAllEquipmentGroup: groupBaseURL + "/deleteAll", // 清空装备编组
  getEquipmentGroup: groupBaseURL + "/get/{id}", // 按ID查询装备编组
  insertEquipmentGroup: groupBaseURL + "/insert", // 新增装备编组
  listEquipmentGroup: groupBaseURL + "/list", // 分页查询装备编组列表
  listEquipmentGroupBySide: groupBaseURL + "/listBySide", // 按阵营查询装备编组
  updateEquipmentGroup: groupBaseURL + "/update", // 更新装备编组
  
  // 装备协同策略管理接口
  deleteEquipmentSynergyTactics: tacticsBaseURL + "/delete/{ids}", // 按ID删除装备协同战术
  deleteAllEquipmentSynergyTactics: tacticsBaseURL + "/deleteAll", // 清空装备协同策略
  getEquipmentSynergyTactics: tacticsBaseURL + "/get/{id}", // 按ID查询装备协同策略
  insertEquipmentSynergyTactics: tacticsBaseURL + "/insert", // 新增装备协同策略
  listEquipmentSynergyTactics: tacticsBaseURL + "/list", // 分页查询装备协同策略列表
  updateEquipmentSynergyTactics: tacticsBaseURL + "/update", // 更新装备协同策略
  
  // 装备网络管理接口
  deleteEquipmentNetwork: networkBaseURL + "/delete/{ids}", // 按ID删除装备网络
  deleteAllEquipmentNetwork: networkBaseURL + "/deleteAll", // 清空装备网络
  getEquipmentNetwork: networkBaseURL + "/get/{id}", // 按ID查询装备网络
  insertEquipmentNetwork: networkBaseURL + "/insert", // 新增装备网络
  listEquipmentNetwork: networkBaseURL + "/list", // 分页查询装备网络列表
  updateEquipmentNetwork: networkBaseURL + "/update", // 更新装备网络
};

/**
 * 按ID删除设备信息聚合数据
 * @param {string|number} ids - 要删除的设备信息ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteEquipment(ids) {
  return request({
    url: apis.deleteEquipment.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空设备信息聚合数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllEquipment() {
  return request({
    url: apis.deleteAllEquipment,
    method: "delete",
  });
}

/**
 * 按ID查询设备信息聚合数据
 * @param {string|number} id - 要查询的设备信息ID
 * @return {Promise} - 返回包含设备信息详情的Promise对象
 */
export function getEquipment(id) {
  return request({
    url: apis.getEquipment.replace('{id}', id),
    method: "get",
  });
}

/**
 * 新增设备信息聚合数据
 * @param {Object} data - 设备信息数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertEquipment(data) {
  return request({
    url: apis.insertEquipment,
    method: "post",
    data: data,
  });
}

/**
 * 分页查询设备信息聚合列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含设备信息列表的Promise对象
 */
export function listEquipment(query) {
  return request({
    url: apis.listEquipment,
    method: "get",
    params: query,
    timeout: 60000
  });
}

/**
 * 按阵营查询设备信息聚合数据
 * @param {Object} query - 查询参数，包含side参数
 * @return {Promise} - 返回包含设备信息列表的Promise对象
 */
export function listEquipmentBySide(query) {
  return request({
    url: apis.listEquipmentBySide,
    method: "get",
    params: query,
  });
}

/**
 * 更新设备信息聚合数据
 * @param {Object} data - 设备信息数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateEquipment(data) {
  return request({
    url: apis.updateEquipment,
    method: "put",
    data: data,
  });
}

/**
 * 按ID删除装备编组
 * @param {string|number} ids - 要删除的装备编组ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteEquipmentGroup(ids) {
  return request({
    url: apis.deleteEquipmentGroup.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空装备编组
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllEquipmentGroup() {
  return request({
    url: apis.deleteAllEquipmentGroup,
    method: "delete",
  });
}

/**
 * 按ID查询装备编组
 * @param {string|number} id - 要查询的装备编组ID
 * @return {Promise} - 返回包含装备编组详情的Promise对象
 */
export function getEquipmentGroup(id) {
  return request({
    url: apis.getEquipmentGroup.replace('{id}', id),
    method: "get",
  });
}

/**
 * 新增装备编组
 * @param {Object} data - 装备编组数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertEquipmentGroup(data) {
  return request({
    url: apis.insertEquipmentGroup,
    method: "post",
    data: data,
  });
}

/**
 * 分页查询装备编组列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含装备编组列表的Promise对象
 */
export function listEquipmentGroup(query) {
  return request({
    url: apis.listEquipmentGroup,
    method: "get",
    params: query,
    timeout: 60000
  });
}

/**
 * 按阵营查询装备编组
 * @param {Object} query - 查询参数，包含side参数
 * @return {Promise} - 返回包含装备编组列表的Promise对象
 */
export function listEquipmentGroupBySide(query) {
  return request({
    url: apis.listEquipmentGroupBySide,
    method: "get",
    params: query,
  });
}

/**
 * 更新装备编组
 * @param {Object} data - 装备编组数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateEquipmentGroup(data) {
  return request({
    url: apis.updateEquipmentGroup,
    method: "put",
    data: data,
  });
}

/**
 * 按ID删除装备协同战术
 * @param {string|number} ids - 要删除的装备协同战术ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteEquipmentSynergyTactics(ids) {
  return request({
    url: apis.deleteEquipmentSynergyTactics.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空装备协同策略
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllEquipmentSynergyTactics() {
  return request({
    url: apis.deleteAllEquipmentSynergyTactics,
    method: "delete",
  });
}

/**
 * 按ID查询装备协同策略
 * @param {string|number} id - 要查询的装备协同策略ID
 * @return {Promise} - 返回包含装备协同策略详情的Promise对象
 */
export function getEquipmentSynergyTactics(id) {
  return request({
    url: apis.getEquipmentSynergyTactics.replace('{id}', id),
    method: "get",
  });
}

/**
 * 新增装备协同策略
 * @param {Object} data - 装备协同策略数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertEquipmentSynergyTactics(data) {
  return request({
    url: apis.insertEquipmentSynergyTactics,
    method: "post",
    data: data,
  });
}

/**
 * 分页查询装备协同策略列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含装备协同策略列表的Promise对象
 */
export function listEquipmentSynergyTactics(query) {
  return request({
    url: apis.listEquipmentSynergyTactics,
    method: "get",
    params: query,
  });
}

/**
 * 更新装备协同策略
 * @param {Object} data - 装备协同策略数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateEquipmentSynergyTactics(data) {
  return request({
    url: apis.updateEquipmentSynergyTactics,
    method: "put",
    data: data,
  });
}

/**
 * 按ID删除装备网络
 * @param {string|number} ids - 要删除的装备网络ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteEquipmentNetwork(ids) {
  return request({
    url: apis.deleteEquipmentNetwork.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空装备网络
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllEquipmentNetwork() {
  return request({
    url: apis.deleteAllEquipmentNetwork,
    method: "delete",
  });
}

/**
 * 按ID查询装备网络
 * @param {string|number} id - 要查询的装备网络ID
 * @return {Promise} - 返回包含装备网络详情的Promise对象
 */
export function getEquipmentNetwork(id) {
  return request({
    url: apis.getEquipmentNetwork.replace('{id}', id),
    method: "get",
  });
}

/**
 * 新增装备网络
 * @param {Object} data - 装备网络数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertEquipmentNetwork(data) {
  return request({
    url: apis.insertEquipmentNetwork,
    method: "post",
    data: data,
  });
}

/**
 * 分页查询装备网络列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含装备网络列表的Promise对象
 */
export function listEquipmentNetwork(query) {
  return request({
    url: apis.listEquipmentNetwork,
    method: "get",
    params: query,
    timeout: 60000
  });
}

/**
 * 更新装备网络
 * @param {Object} data - 装备网络数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateEquipmentNetwork(data) {
  return request({
    url: apis.updateEquipmentNetwork,
    method: "put",
    data: data,
  });
}