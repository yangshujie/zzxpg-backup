import request from './request'

// ==================== 1. 准则集 (CriterionSet) CRUD ====================

/** 分页查询准则集列表 */
export function listSets(query) {
  return request({
    url: '/zhpg/criterionSet/list',
    method: 'get',
    params: query
  })
}

/** 查询准则集详情 */
export function getSetInfo(id) {
  return request({
    url: '/zhpg/criterionSet/' + id,
    method: 'get'
  })
}

/** 新增准则集 */
export function addSet(data) {
  return request({
    url: '/zhpg/criterionSet',
    method: 'post',
    data: data
  })
}

/** 修改准则集 */
export function editSet(data) {
  return request({
    url: '/zhpg/criterionSet',
    method: 'put',
    data: data
  })
}

/** 删除准则集 */
export function removeSets(ids) {
  return request({
    url: '/zhpg/criterionSet/' + ids,
    method: 'delete'
  })
}

// ==================== 2. 准则明细 (EvalCriterion) ====================

/** 根据准则集ID查询准则明细 */
export function getCriteriaBySet(setId) {
  return request({
    url: '/zhpg/evalCriterion/bySet/' + setId,
    method: 'get'
  })
}

/** 根据评估任务ID查询准则明细 */
export function getCriteriaByTask(taskId) {
  return request({
    url: '/zhpg/evalCriterion/byTask/' + taskId,
    method: 'get'
  })
}

/** 批量覆盖保存指标准则 */
export function batchSaveCriteria(setId, criterionList) {
  return request({
    url: '/zhpg/evalCriterion/batchSave/' + setId,
    method: 'post',
    data: criterionList
  })
}
