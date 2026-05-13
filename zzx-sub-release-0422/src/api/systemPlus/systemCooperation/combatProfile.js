
import request from "@/utils/taskRequest";

const baseURL ="/operationProfile";
const apis = {
  getConfig: baseURL + "/getConfig", // 获取剖面配置信息
  selectList: baseURL + "/selectList", // 查询全部作战剖面
  deleteWithSub: baseURL + "/deleteWithSub", // 删除作战剖面（包含子项）
  saveOrUpdate: baseURL + "/addProfile", // 新增作战剖面
  updateProfile: baseURL + "/updateProfile", // 编辑作战剖面
  profileDetail: baseURL + "/profileDetail", // 查询单个剖面
};

/**
 * 获取剖面配置信息
 * @return {Promise} - 返回包含配置信息的Promise对象
 */
export function getConfig() {
  return request({
    url: apis.getConfig,
    method: "get",
  });
}

/**
 * 查询全部作战剖面
 * @param {Object} query - 查询参数
 * @return {Promise} - 返回包含作战剖面列表的Promise对象
 */
export function selectList(query) {
  return request({
    url: apis.selectList,
    method: "post",
    data: query,
  });
}

/**
 * 删除作战剖面（包含子项）
 * @param {Number} profileId - 作战剖面ID
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function deleteWithSub(profileId) {
  return request({
    url: apis.deleteWithSub,
    method: "delete",
    params: {
      profileId: profileId
    }
  });
}


/**
 * 新增作战剖面
 * @param {Object} data - 作战剖面数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function saveOrUpdateCombatProfile(data) {
  return request({
    url: apis.saveOrUpdate,
    method: "post",
    data: data,
  });
}

/**
 * 编辑作战剖面
 * @param {Object} data - 作战剖面数据
 * @return {Promise} - 返回操作结果的Promise对象
 */
export function updateProfile(data) {
  return request({
    url: apis.updateProfile,
    method: "post",
    data: data,
  });
}

/**
 * 查询单个剖面
 * @param {Number} profileId - 作战剖面ID
 * @return {Promise} - 返回包含剖面详情的Promise对象
 */
export function profileDetail(profileId) {
  return request({
    url: apis.profileDetail,
    method: "get",
    params: {
      profileId: profileId
    }
  });
}