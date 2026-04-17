import baseRequest from '@/utils/request'

/**
 * 数据采集模块专用请求包装器
 * 自动将 baseURL 设置为 /huage-api 以匹配 Vite 代理配置
 */
const request = (config) => {
  return baseRequest({
    ...config,
    baseURL: '/huage-api',
    headers: {
      ...config.headers,
      isToken: false // 开发阶段先不加 token 校验
    }
  })
}

export default request
