import request from './request'

// 分页查询算法列表
export function listAlgorithm(query) {
  return request({ url: '/zhpg/algorithm/list', method: 'get', params: query })
}

// 查询全部算法（不分页）
export function listAllAlgorithm(query) {
  return request({ url: '/zhpg/algorithm/listAll', method: 'get', params: query })
}

// 获取算法详情（含参数）
export function getAlgorithm(id) {
  return request({ url: '/zhpg/algorithm/' + id, method: 'get' })
}

// 获取算法参数列表
export function getAlgorithmParams(id, paramCategory) {
  return request({ url: '/zhpg/algorithm/' + id + '/params', method: 'get', params: { paramCategory } })
}

// 新增算法
export function addAlgorithm(data) {
  return request({ url: '/zhpg/algorithm', method: 'post', data })
}

// 修改算法
export function updateAlgorithm(data) {
  return request({ url: '/zhpg/algorithm', method: 'put', data })
}

// 删除算法
export function delAlgorithm(id) {
  return request({ url: '/zhpg/algorithm/' + id, method: 'delete' })
}

// 发布算法
export function publishAlgorithm(id) {
  return request({ url: '/zhpg/algorithm/publish/' + id, method: 'put' })
}

// 撤回发布
export function unpublishAlgorithm(id) {
  return request({ url: '/zhpg/algorithm/unpublish/' + id, method: 'put' })
}

// 上传算法 zip（路径由后端按算法类型 + 算法名称生成）
export function uploadAlgorithmCode(algorithmType, algorithmName, file) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('algorithmType', algorithmType)
  formData.append('algorithmName', algorithmName)
  return request({
    url: '/zhpg/algorithm/uploadCode',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false }
  })
}

// 预览主 .py 源码
export function previewAlgorithmCode(id) {
  return request({ url: '/zhpg/algorithm/' + id + '/codePreview', method: 'get' })
}
