import request from "@/utils/equRequest";

const mappingRelationBaseURL = "/mappingRelation";
const apis = {
  // 超网络映射关系管理接口
  deleteMappingRelation: mappingRelationBaseURL + "/delete/{ids}", // 按ID批量删除映射关系
  deleteAllMappingRelation: mappingRelationBaseURL + "/deleteAll", // 清空映射关系
  getMappingRelation: mappingRelationBaseURL + "/get/{id}", // 按ID查询映射关系
  insertMappingRelation: mappingRelationBaseURL + "/insert", // 新增映射关系
  listMappingRelation: mappingRelationBaseURL + "/list", // 分页查询映射关系列表
  updateMappingRelation: mappingRelationBaseURL + "/update", // 更新映射关系
};

/**
 * 按ID批量删除映射关系
 * @param {string|number} ids - 要删除的映射关系ID，多个ID用逗号分隔
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteMappingRelation(ids) {
  return request({
    url: apis.deleteMappingRelation.replace('{ids}', ids),
    method: "delete",
  });
}

/**
 * 清空映射关系
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteAllMappingRelation() {
  return request({
    url: apis.deleteAllMappingRelation,
    method: "delete",
  });
}

/**
 * 按ID查询映射关系
 * @param {string|number} id - 要查询的映射关系ID
 * @return {Promise} - 返回包含映射关系详情的Promise对象
 */
export function getMappingRelation(id) {
  return request({
    url: apis.getMappingRelation.replace('{id}', id),
    method: "get",
  });
}

/**
 * 新增映射关系
 * @param {Object} data - 映射关系数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function insertMappingRelation(data) {
  return request({
    url: apis.insertMappingRelation,
    method: "post",
    data: data,
  });
}

/**
 * 分页查询映射关系列表
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含映射关系列表的Promise对象
 */
export function listMappingRelation(query) {
  return request({
    url: apis.listMappingRelation,
    method: "get",
    params: query,
  });
}

/**
 * 更新映射关系
 * @param {Object} data - 映射关系数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateMappingRelation(data) {
  return request({
    url: apis.updateMappingRelation,
    method: "put",
    data: data,
  });
}

// 导出所有接口
export default {
  deleteMappingRelation,
  deleteAllMappingRelation,
  getMappingRelation,
  insertMappingRelation,
  listMappingRelation,
  updateMappingRelation,
};