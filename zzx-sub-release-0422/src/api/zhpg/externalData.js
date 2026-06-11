import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建专门用于外部数据的请求实例，避免使用默认的 /dev-api 前缀
const externalService = axios.create({
  baseURL: '/huage-api',
  timeout: 10000
})

// 简单的响应拦截器
externalService.interceptors.response.use(
  res => res.data,
  error => {
    ElMessage.error('外部系统接口请求失败: ' + (error.message || '未知错误'))
    return Promise.reject(error)
  }
)

/**
 * 获取外部试验数据列表 (端口 9501)
 * @param {object} query 
 * @returns 
 */
export function listExternalData(query) {
  return externalService({
    url: '/tableCreationRecord/list',
    method: 'get',
    params: query
  })
}

/**
 * 获取预处理批次列表
 * @param {object} query
 */
export function listPreprocessBatch(query) {
  return externalService({
    url: '/preprocess/batch/list',
    method: 'get',
    params: query
  })
}
