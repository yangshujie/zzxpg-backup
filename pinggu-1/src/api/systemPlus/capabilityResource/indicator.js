import request from '@/utils/request'

// 生成假数据
const generateMockData = (query) => {
  const { pageNum = 1, pageSize = 10, name, type } = query || {}
  
  // 基础指标数据
  const baseIndicators = [
    { id: 1, name: '侦察覆盖率', type: '1', isBase: true, algorithm: '覆盖率计算算法', creator: '张三', createTime: '2024-01-15 10:30:00' },
    { id: 2, name: '目标识别准确率', type: '1', isBase: true, algorithm: '准确率评估算法', creator: '李四', createTime: '2024-01-16 14:20:00' },
    { id: 3, name: '系统响应时间', type: '3', isBase: false, algorithm: '响应时间分析算法', creator: '王五', createTime: '2024-01-17 09:15:00' },
    { id: 4, name: '装备配置合理性', type: '2', isBase: false, algorithm: '配置优化算法', creator: '赵六', createTime: '2024-01-18 16:45:00' },
    { id: 5, name: '系统稳定性指数', type: '3', isBase: true, algorithm: '稳定性评估算法', creator: '钱七', createTime: '2024-01-19 11:20:00' },
    { id: 6, name: '作战效能评估', type: '1', isBase: false, algorithm: '效能综合算法', creator: '孙八', createTime: '2024-01-20 13:30:00' },
    { id: 7, name: '装备增长潜力', type: '5', isBase: true, algorithm: '潜力预测算法', creator: '周九', createTime: '2024-01-21 15:10:00' },
    { id: 8, name: '反应敏捷性指标', type: '4', isBase: false, algorithm: '敏捷性分析算法', creator: '吴十', createTime: '2024-01-22 08:50:00' },
    { id: 9, name: '体系配置优化度', type: '2', isBase: true, algorithm: '优化度评估算法', creator: '郑十一', createTime: '2024-01-23 12:25:00' },
    { id: 10, name: '运行稳定性监测', type: '3', isBase: false, algorithm: '稳定性监测算法', creator: '王十二', createTime: '2024-01-24 17:35:00' },
    { id: 11, name: '侦察装备效能', type: '1', isBase: true, algorithm: '装备效能算法', creator: '李十三', createTime: '2024-01-25 10:40:00' },
    { id: 12, name: '攻防体系合理性', type: '2', isBase: false, algorithm: '攻防评估算法', creator: '张十四', createTime: '2024-01-26 14:55:00' },
    { id: 13, name: '系统运行效率', type: '3', isBase: true, algorithm: '效率分析算法', creator: '刘十五', createTime: '2024-01-27 09:30:00' },
    { id: 14, name: '反应时间优化', type: '4', isBase: false, algorithm: '时间优化算法', creator: '陈十六', createTime: '2024-01-28 16:20:00' },
    { id: 15, name: '能力增长预测', type: '5', isBase: true, algorithm: '增长预测算法', creator: '杨十七', createTime: '2024-01-29 11:45:00' }
  ]
  
  // 过滤数据
  let filteredData = baseIndicators
  
  if (name) {
    filteredData = filteredData.filter(item => item.name.includes(name))
  }
  
  if (type) {
    filteredData = filteredData.filter(item => item.type === type)
  }
  
  // 分页处理
  const startIndex = (pageNum - 1) * pageSize
  const endIndex = startIndex + pageSize
  const paginatedData = filteredData.slice(startIndex, endIndex)
  
  return {
    rows: paginatedData,
    total: filteredData.length,
    pageNum: parseInt(pageNum),
    pageSize: parseInt(pageSize)
  }
}

// 查询指标列表
export function listIndicator(query) {
  // 模拟网络延迟
  return new Promise((resolve) => {
    setTimeout(() => {
      const result = generateMockData(query)
      resolve(result)
    }, 300)
  })
}

// 查询指标详细
export function getIndicator(id) {
  return new Promise((resolve) => {
    setTimeout(() => {
      const baseIndicators = generateMockData().rows
      const indicator = baseIndicators.find(item => item.id === parseInt(id))
      resolve({ data: indicator || null })
    }, 200)
  })
}

// 新增指标
export function addIndicator(data) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ code: 200, msg: '新增成功' })
    }, 300)
  })
}

// 修改指标
export function updateIndicator(data) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ code: 200, msg: '修改成功' })
    }, 300)
  })
}

// 删除指标
export function delIndicator(id) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ code: 200, msg: '删除成功' })
    }, 300)
  })
}

// 导出指标
export function exportIndicator(query) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ code: 200, msg: '导出成功' })
    }, 500)
  })
}