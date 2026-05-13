import { serviceNoAuth } from '@/utils/request'

/**
 * zhpg 模块专用请求包装器
 * 直连 zhpg 业务服务（127.0.0.1:9303），绕过需要 token 的 API 网关
 */
const request = (config) => {
  return serviceNoAuth({
    ...config,
    baseURL: '/zhpg-api'
  })
}

export default request
