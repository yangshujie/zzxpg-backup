import request from './request';

//文件提取 知识图谱
export const fileUpload = (data) => {
  return request({
    url: '/mes/extract/uploadFiles',
    method: 'post',
    data
  })
}
export const downLoadFile = (data) => {
  return request({
    url: '/mes/extract/downLoadFile',
    method: 'get',
    params: data
  })
}
export const listFile = (data) => {
  return request({
    url: '/mes/extract/list',
    method: 'get',
    params: data
  })
}
export const getFile = (id) => {
  return request({
    url: '/mes/extract/' + id,
    method: 'get',
  })
}
// export const addRule = (data) => {
//     return request({
//         url: '/hadoop/rule/addRule',
//         method: 'post',
//         data
//     })
// }

export const anaXMLData = (data) => {
  return request({
    url: '/api/v1/data/anaXMLData',
    method: 'post',
    data
  })
}

export const listXMLCollect = (data) => {
  return request({
    url: '/api/v1/data/listXMLCollect',
    method: 'get',
    params: data
  })
}

export const collect = (data) => {
  return request({
    url: '/api/v1/data/collect',
    method: 'post',
    data
  })
}
export const addRealTimeRule = (data) => {
  return request({
    url: '/mes/rTRule',
    method: 'post',
    data
  })
}
export const updateRealTimeRule = (data) => {
  return request({
    url: '/mes/rTRule',
    method: 'put',
    data
  })
}
export const listRealTimeRule = (data) => {
  return request({
    url: '/mes/rTRule/list',
    method: 'get',
    params: data
  })
}

export const listAllRealTimeRule = (data) => {
  return request({
    url: '/mes/rTRule/listAll',
    method: 'get',
    params: data
  })
}

export const deleteRealTimeRule = (ids) => {
  return request({
    url: '/mes/rTRule/' + ids,
    method: 'delete',
  })
}

export const getRealTimeRule = (id) => {
  return request({
    url: '/mes/rTRule/' + id,
    method: 'get',
  })
}

export const loadOfflineData = (data) => {
  return request({
    url: '/api/v1/data/loadOfflineData',
    method: 'post',
    data
  })
}

export const addConfig = (data) => {
  return request({
    url: '/mes/config',
    method: 'post',
    data
  })
}
// 全部
export const listAllConfig = (data) => {
  return request({
    url: '/mes/config/listAll',
    method: 'get',
    params: data
  })
}
export const listSignals = (data) => {
  return request({
    url: '/mes/config/listSignals',
    method: 'get',
    params: data
  })
}
// 分页
export const listConfig = (data) => {
  return request({
    url: '/mes/config/list',
    method: 'get',
    params: data
  })
}
// 启用禁用
export const enableConfig = (data) => {
  return request({
    url: '/mes/config/enable',
    method: 'get',
    params: data
  })
}

// 新增流水线
// export const addLineConfig = (data) => {
//   return request({
//     url: '/hadoop/line_config',
//     method:
//   })
// }