import request from "@/utils/taskRequest";

const baseURL = "/evaluation";
const apis = {
  // 评估方案生成
  generateEvaluationScheme: "/sampling/sample",           // 评估方案生成
  
  // 评估方案管理
  addEvaluationScheme: baseURL + "/scheme/evaluation/add",        // 新增评估方案
  getEvaluationSchemeList: baseURL + "/evaluation/schemeList",    // 查询评估方案列表
  getEvaluationScheme: baseURL + "/evaluation/getScheme",          // 查询单个评估方案详情
  updateEvaluationScheme: baseURL + "/evaluation/editWithSub",     // 修改评估方案
  deleteEvaluationScheme: baseURL + "/evaluation/deleteWithSub",  // 删除评估方案
};

// ==================== 评估方案生成 ====================

/**
 * 评估方案生成
 * @param {Object} data - 评估方案生成参数
 */
export function generateEvaluationScheme(data) {
  return request({
    url: apis.generateEvaluationScheme,
    method: "post",
    data: data,
  });
}

// ==================== 评估方案管理 ====================

/**
 * 新增评估方案
 * @param {Object} data - 评估方案数据
 */
export function addEvaluationScheme(data) {
  return request({
    url: apis.addEvaluationScheme,
    method: "post",
    data: data,
  });
}

/**
 * 查询评估方案列表
 * @param {Object} data - 查询参数 { pageNum, pageSize, schemeName, status, createTimeStart, createTimeEnd }
 */
export function getEvaluationSchemeList(data) {
  return request({
    url: apis.getEvaluationSchemeList,
    method: "post",
    data: data,
  });
}

/**
 * 查询单个评估方案详情
 * @param {Object} params - { schemeId }
 */
export function getEvaluationScheme(params) {
  return request({
    url: apis.getEvaluationScheme,
    method: "get",
    params: params,
  });
}

/**
 * 修改评估方案
 * @param {Object} data - 评估方案数据
 */
export function updateEvaluationScheme(data) {
  return request({
    url: apis.updateEvaluationScheme,
    method: "post",
    data: data,
  });
}

/**
 * 删除评估方案
 * @param {Object} data - { schemeId }
 */
export function deleteEvaluationScheme(data) {
  return request({
    url: apis.deleteEvaluationScheme,
    method: "delete",
    data: data,
  });
}