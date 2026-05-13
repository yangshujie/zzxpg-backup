import request from "@/utils/taskRequest";
import { time } from "echarts";

const baseURL = "/operationtask";
const apis = {
  // 任务管理
  addTask: baseURL + "/addTask",               // 新增作战任务（含子任务）
  updateTask: baseURL + "/updateTask",         // 编辑作战任务（含子任务）
  deleteTask: baseURL + "/deleteTask",         // 删除作战任务（级联删除子任务）
  getTaskList: baseURL + "/getTaskList", // 查询剖面作战任务列表
  queryTask: baseURL + "/queryTask",           // 查询单个任务详情（含子任务）
  getSubTasks: baseURL + "/getSubTasks", // 查询子任务列表
  
  // 判据管理
  addCriterion: baseURL + "/addCriterion",
  updateCriterion: baseURL + "/updateCriterion",
  deleteCriterion: baseURL + "/deleteCriterion",
  queryCriterionList: baseURL + "/queryCriterionList",
  queryCriterionById: baseURL + "/queryCriterionById",
};

// ==================== 任务管理 ====================

/**
 * 新增作战任务（含子任务）
 * @param {Object} data - 任务数据，包含子任务列表 subTaskList
 */
export function addTask(data) {
  return request({
    url: apis.addTask,
    method: "post",
    data: data,
  });
}

/**
 * 编辑作战任务（含子任务）
 * @param {Object} data - 任务数据，包含子任务列表 subTaskList
 */
export function updateTask(data) {
  return request({
    url: apis.updateTask,
    method: "post",
    data: data,
  });
}

/**
 * 删除作战任务（级联删除子任务）
 * @param {Object} params - { taskId }
 */
export function deleteTask(params) {
  return request({
    url: apis.deleteTask,
    method: "delete",
    params: params,
  });
}

/**
 * 查询剖面作战任务列表（不含子任务详情）
 * @param {Object} params - { profileId, taskName, taskStage, pageNum, pageSize }
 */
export function getTaskList(params) {
  return request({
    url: apis.getTaskList,
    method: "post",
    data: params,
  });
}

/**
 * 查询单个任务详情（含子任务列表）
 * @param {Object} data - { taskId }
 */
export function queryTask(data) {
  return request({
    url: apis.queryTask,
    method: "post",
    data: data,
    timeout: 600000,
  });
}


/**
 * 查询子任务列表
 * @param {Object} data - { taskId }
 */
export function getSubTasks(data) {
  return request({
    url: apis.getSubTasks,
    method: "get",
    params: data,
  });
}


// ==================== 判据管理 ====================

/**
 * 新增作战判据
 * @param {Object} data
 */
export function addCriterion(data) {
  return request({
    url: apis.addCriterion,
    method: "post",
    data: data,
  });
}

/**
 * 更新作战判据
 * @param {Object} data
 */
export function updateCriterion(data) {
  return request({
    url: apis.updateCriterion,
    method: "post",
    data: data,
  });
}

/**
 * 删除作战判据
 * @param {Object} data - { criterionId }
 */
export function deleteCriterion(data) {
  return request({
    url: apis.deleteCriterion,
    method: "delete",
    data: data,
  });
}

/**
 * 查询判据列表
 * @param {Object} params
 */
export function queryCriterionList(params) {
  return request({
    url: apis.queryCriterionList,
    method: "post",
    data: params,
  });
}

/**
 * 查询单个判据详情
 * @param {Object} params - { criterionId }
 */
export function queryCriterionById(params) {
  return request({
    url: apis.queryCriterionById,
    method: "get",
    params: params,
  });
}