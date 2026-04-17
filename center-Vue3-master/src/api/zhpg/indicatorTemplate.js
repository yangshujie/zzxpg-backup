import request from '@/utils/request'

export function listIndicatorTemplate(query) {
  return request({ url: '/zhpg/indicatorTemplate/list', method: 'get', params: query })
}

export function getIndicatorTemplate(id) {
  return request({ url: '/zhpg/indicatorTemplate/' + id, method: 'get' })
}

export function addIndicatorTemplate(data) {
  return request({ url: '/zhpg/indicatorTemplate', method: 'post', data })
}

export function updateIndicatorTemplate(data) {
  return request({ url: '/zhpg/indicatorTemplate', method: 'put', data })
}

export function delIndicatorTemplate(id) {
  return request({ url: '/zhpg/indicatorTemplate/' + id, method: 'delete' })
}

export function applyIndicatorTemplate(id) {
  return request({ url: '/zhpg/indicatorTemplate/' + id + '/apply', method: 'get' })
}

export function publishIndicatorTemplate(id) {
  return request({ url: '/zhpg/indicatorTemplate/' + id + '/publish', method: 'post' })
}

export function exportIndicatorTemplateJson(id) {
  return request({ url: '/zhpg/indicatorTemplate/' + id + '/exportJson', method: 'get' })
}

export function importIndicatorTemplateJson(data, conflictStrategy = 'rename') {
  return request({ url: '/zhpg/indicatorTemplate/importJson', method: 'post', data, params: { conflictStrategy } })
}

export function previewImportIndicatorTemplate(data) {
  return request({ url: '/zhpg/indicatorTemplate/previewImport', method: 'post', data })
}

export function listIndicatorTemplateVersions(id) {
  return request({ url: '/zhpg/indicatorTemplate/' + id + '/versions', method: 'get' })
}

export function compareIndicatorTemplate(leftId, rightId) {
  return request({ url: '/zhpg/indicatorTemplate/compare', method: 'get', params: { leftId, rightId } })
}

export function listIndicatorTemplateLogs(id) {
  return request({ url: '/zhpg/indicatorTemplate/' + id + '/logs', method: 'get' })
}

export function copyIndicatorTemplateVersion(id, version) {
  return request({ url: '/zhpg/indicatorTemplate/' + id + '/copyVersion', method: 'post', params: { version } })
}

// ========== 新增接口 ==========

/**
 * 模板构建向导
 */
export function buildTemplate(buildDTO) {
  return request({ url: '/zhpg/indicatorTemplate/build', method: 'post', data: buildDTO })
}

/**
 * 获取场景预设指标
 */
export function getScenePresets(sceneType) {
  return request({ url: '/zhpg/indicatorTemplate/scenePresets/' + sceneType, method: 'get' })
}

/**
 * 模板预览（含指标树）
 */
export function previewTemplate(id) {
  return request({ url: '/zhpg/indicatorTemplate/' + id + '/preview', method: 'get' })
}

/**
 * 批量删除模板
 */
export function batchDeleteTemplate(ids) {
  return request({ url: '/zhpg/indicatorTemplate/batch', method: 'delete', data: ids })
}

/**
 * 按业务场景查询模板
 */
export function listTemplateByBizScene(bizScene) {
  return request({ url: '/zhpg/indicatorTemplate/listByBizScene/' + bizScene, method: 'get' })
}
