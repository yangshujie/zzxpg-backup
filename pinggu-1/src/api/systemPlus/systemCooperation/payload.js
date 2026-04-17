import request from "@/utils/equRequest";

const opticalPayloadBaseURL = "/satPayloadOptical";
const sarPayloadBaseURL = "/satPayloadSAR";
const electronicPayloadBaseURL = "/satPayloadElectronic";
const relayPayloadBaseURL = "/satPayloadRelay";
const apis = {
  // 卫星光学载荷管理接口
  deleteSatPayloadOptical: opticalPayloadBaseURL + "/delete/{ids}", // 按ID删除卫星光学载荷
  deleteAllSatPayloadOptical: opticalPayloadBaseURL + "/deleteAll", // 清空卫星光学载荷
  getSatPayloadOptical: opticalPayloadBaseURL + "/get/{payloadId}", // 按ID查询卫星光学载荷
  insertSatPayloadOptical: opticalPayloadBaseURL + "/insert", // 新增卫星光学载荷
  listSatPayloadOptical: opticalPayloadBaseURL + "/list", // 分页查询卫星光学载荷列表
  updateSatPayloadOptical: opticalPayloadBaseURL + "/update", // 更新卫星光学载荷
  
  // 卫星SAR载荷管理接口
  deleteSatPayloadSAR: sarPayloadBaseURL + "/delete/{ids}", // 按ID删除卫星SAR载荷
  deleteAllSatPayloadSAR: sarPayloadBaseURL + "/deleteAll", // 清空卫星SAR载荷
  getSatPayloadSAR: sarPayloadBaseURL + "/get/{payloadId}", // 按ID查询卫星SAR载荷
  insertSatPayloadSAR: sarPayloadBaseURL + "/insert", // 新增卫星SAR载荷
  listSatPayloadSAR: sarPayloadBaseURL + "/list", // 分页查询卫星SAR载荷列表
  updateSatPayloadSAR: sarPayloadBaseURL + "/update", // 更新卫星SAR载荷
  
  // 卫星电子侦察载荷管理接口
  deleteSatPayloadElectronic: electronicPayloadBaseURL + "/delete/{ids}", // 按ID删除卫星电子侦察载荷
  deleteAllSatPayloadElectronic: electronicPayloadBaseURL + "/deleteAll", // 清空卫星电子侦察载荷
  getSatPayloadElectronic: electronicPayloadBaseURL + "/get/{payloadId}", // 按ID查询卫星电子侦察载荷
  insertSatPayloadElectronic: electronicPayloadBaseURL + "/insert", // 新增卫星电子侦察载荷
  listSatPayloadElectronic: electronicPayloadBaseURL + "/list", // 分页查询卫星电子侦察载荷列表
  updateSatPayloadElectronic: electronicPayloadBaseURL + "/update", // 更新卫星电子侦察载荷
  
  // 卫星中继载荷管理接口
  deleteSatPayloadRelay: relayPayloadBaseURL + "/delete/{ids}", // 按ID删除卫星中继载荷
  deleteAllSatPayloadRelay: relayPayloadBaseURL + "/deleteAll", // 清空卫星中继载荷
  getSatPayloadRelay: relayPayloadBaseURL + "/get/{payloadId}", // 按ID查询卫星中继载荷
  insertSatPayloadRelay: relayPayloadBaseURL + "/insert", // 新增卫星中继载荷
  listSatPayloadRelay: relayPayloadBaseURL + "/list", // 分页查询卫星中继载荷列表
  updateSatPayloadRelay: relayPayloadBaseURL + "/update", // 更新卫星中继载荷
};

/**
 * 按ID删除卫星光学载荷
 * @param {string|number} ids - 要删除的卫星光学载荷ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteSatPayloadOptical(ids) {
  return request({
    url: apis.deleteSatPayloadOptical.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空卫星光学载荷
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllSatPayloadOptical() {
  return request({
    url: apis.deleteAllSatPayloadOptical,
    method: "delete",
  });
}

/**
 * 按ID查询卫星光学载荷
 * @param {string|number} payloadId - 要查询的卫星光学载荷ID
 * @return {Promise} - 返回包含卫星光学载荷详情的Promise对象
 */
export function getSatPayloadOptical(payloadId) {
  return request({
    url: apis.getSatPayloadOptical.replace('{payloadId}', payloadId),
    method: "get",
  });
}

/**
 * 新增卫星光学载荷
 * @param {Object} data - 卫星光学载荷数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertSatPayloadOptical(data) {
  return request({
    url: apis.insertSatPayloadOptical,
    method: "post",
    data: data,
  });
}

/**
 * 分页查询卫星光学载荷列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含卫星光学载荷列表的Promise对象
 */
export function listSatPayloadOptical(query) {
  return request({
    url: apis.listSatPayloadOptical,
    method: "get",
    params: query,
  });
}

/**
 * 更新卫星光学载荷
 * @param {Object} data - 卫星光学载荷数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateSatPayloadOptical(data) {
  return request({
    url: apis.updateSatPayloadOptical,
    method: "put",
    data: data,
  });
}

/**
 * 按ID删除卫星SAR载荷
 * @param {string|number} ids - 要删除的卫星SAR载荷ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteSatPayloadSAR(ids) {
  return request({
    url: apis.deleteSatPayloadSAR.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空卫星SAR载荷
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllSatPayloadSAR() {
  return request({
    url: apis.deleteAllSatPayloadSAR,
    method: "delete",
  });
}

/**
 * 按ID查询卫星SAR载荷
 * @param {string|number} payloadId - 要查询的卫星SAR载荷ID
 * @return {Promise} - 返回包含卫星SAR载荷详情的Promise对象
 */
export function getSatPayloadSAR(payloadId) {
  return request({
    url: apis.getSatPayloadSAR.replace('{payloadId}', payloadId),
    method: "get",
  });
}

/**
 * 新增卫星SAR载荷
 * @param {Object} data - 卫星SAR载荷数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertSatPayloadSAR(data) {
  return request({
    url: apis.insertSatPayloadSAR,
    method: "post",
    data: data,
  });
}

/**
 * 分页查询卫星SAR载荷列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含卫星SAR载荷列表的Promise对象
 */
export function listSatPayloadSAR(query) {
  return request({
    url: apis.listSatPayloadSAR,
    method: "get",
    params: query,
  });
}

/**
 * 更新卫星SAR载荷
 * @param {Object} data - 卫星SAR载荷数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateSatPayloadSAR(data) {
  return request({
    url: apis.updateSatPayloadSAR,
    method: "put",
    data: data,
  });
}

/**
 * 按ID删除卫星电子侦察载荷
 * @param {string|number} ids - 要删除的卫星电子侦察载荷ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteSatPayloadElectronic(ids) {
  return request({
    url: apis.deleteSatPayloadElectronic.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空卫星电子侦察载荷
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllSatPayloadElectronic() {
  return request({
    url: apis.deleteAllSatPayloadElectronic,
    method: "delete",
  });
}

/**
 * 按ID查询卫星电子侦察载荷
 * @param {string|number} payloadId - 要查询的卫星电子侦察载荷ID
 * @return {Promise} - 返回包含卫星电子侦察载荷详情的Promise对象
 */
export function getSatPayloadElectronic(payloadId) {
  return request({
    url: apis.getSatPayloadElectronic.replace('{payloadId}', payloadId),
    method: "get",
  });
}

/**
 * 新增卫星电子侦察载荷
 * @param {Object} data - 卫星电子侦察载荷数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertSatPayloadElectronic(data) {
  return request({
    url: apis.insertSatPayloadElectronic,
    method: "post",
    data: data,
  });
}

/**
 * 分页查询卫星电子侦察载荷列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含卫星电子侦察载荷列表的Promise对象
 */
export function listSatPayloadElectronic(query) {
  return request({
    url: apis.listSatPayloadElectronic,
    method: "get",
    params: query,
  });
}

/**
 * 更新卫星电子侦察载荷
 * @param {Object} data - 卫星电子侦察载荷数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateSatPayloadElectronic(data) {
  return request({
    url: apis.updateSatPayloadElectronic,
    method: "put",
    data: data,
  });
}

/**
 * 按ID删除卫星中继载荷
 * @param {string|number} ids - 要删除的卫星中继载荷ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteSatPayloadRelay(ids) {
  return request({
    url: apis.deleteSatPayloadRelay.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空卫星中继载荷
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllSatPayloadRelay() {
  return request({
    url: apis.deleteAllSatPayloadRelay,
    method: "delete",
  });
}

/**
 * 按ID查询卫星中继载荷
 * @param {string|number} payloadId - 要查询的卫星中继载荷ID
 * @return {Promise} - 返回包含卫星中继载荷详情的Promise对象
 */
export function getSatPayloadRelay(payloadId) {
  return request({
    url: apis.getSatPayloadRelay.replace('{payloadId}', payloadId),
    method: "get",
  });
}

/**
 * 新增卫星中继载荷
 * @param {Object} data - 卫星中继载荷数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertSatPayloadRelay(data) {
  return request({
    url: apis.insertSatPayloadRelay,
    method: "post",
    data: data,
  });
}

/**
 * 分页查询卫星中继载荷列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含卫星中继载荷列表的Promise对象
 */
export function listSatPayloadRelay(query) {
  return request({
    url: apis.listSatPayloadRelay,
    method: "get",
    params: query,
  });
}

/**
 * 更新卫星中继载荷
 * @param {Object} data - 卫星中继载荷数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateSatPayloadRelay(data) {
  return request({
    url: apis.updateSatPayloadRelay,
    method: "put",
    data: data,
  });
}