// 生成指标体系假数据
const generateMockData = (query) => {
  const { pageNum = 1, pageSize = 10, name, type, equipmentType } = query || {}
  
  const baseSystems = [
    {
      id: 1,
      systemName: '侦察装备作战效能指标体系',
      systemObject: '1',
      isEnabled: true,
      weightAlgorithm: '层次分析法',
      approvalStatus: '2',
      updateTime: '2024-01-15 10:30:00',
      creator: '张三'
    },
    {
      id: 2,
      systemName: '太空态势感知适用性指标体系',
      systemObject: '2',
      isEnabled: true,
      weightAlgorithm: '熵权法',
      approvalStatus: '1',
      updateTime: '2024-01-16 14:20:00',
      creator: '李四'
    },
    {
      id: 3,
      systemName: '航天发射装备体系适用性指标',
      systemObject: '3',
      isEnabled: false,
      weightAlgorithm: '主成分分析法',
      approvalStatus: '3',
      updateTime: '2024-01-17 09:15:00',
      creator: '王五'
    }
  ]
  
  let filteredData = baseSystems
  if (name) filteredData = filteredData.filter(item => item.systemName.includes(name))
  if (equipmentType) filteredData = filteredData.filter(item => item.systemName.includes(equipmentType))
  
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

export function listIndicatorSystem(query) {
  return new Promise((resolve) => {
    setTimeout(() => {
      const result = generateMockData(query)
      resolve(result)
    }, 300)
  })
}

export function getIndicatorSystem(id) {
  return new Promise((resolve) => {
    setTimeout(() => {
      const mockData = generateMockData()
      const system = mockData.rows.find(item => item.id === parseInt(id))
      resolve({ data: system || {} })
    }, 200)
  })
}

export function addIndicatorSystem(data) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ code: 200, msg: '新增成功' })
    }, 200)
  })
}

export function updateIndicatorSystem(data) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ code: 200, msg: '修改成功' })
    }, 200)
  })
}

export function delIndicatorSystem(id) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ code: 200, msg: '删除成功' })
    }, 200)
  })
}

export function downloadApproval(id) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ code: 200, msg: '下载成功', data: { url: '/download/approval_' + id + '.pdf' } })
    }, 500)
  })
}

export function autoOptimizeWeight(id) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ code: 200, msg: '权重自优化成功' })
    }, 800)
  })
}