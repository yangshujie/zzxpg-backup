import request from '@/utils/request'

export function listDataSource(query) {
  return request({ url: '/zhpg/dataSource/list', method: 'get', params: query })
}

export function listAllDataSource(query) {
  return request({ url: '/zhpg/dataSource/listAll', method: 'get', params: query })
}

export function getDataSource(id) {
  return request({ url: '/zhpg/dataSource/' + id, method: 'get' })
}

export function addDataSource(data) {
  return request({ url: '/zhpg/dataSource', method: 'post', data })
}

export function updateDataSource(data) {
  return request({ url: '/zhpg/dataSource', method: 'put', data })
}

export function delDataSource(id) {
  return request({ url: '/zhpg/dataSource/' + id, method: 'delete' })
}

export function testDataSource(data) {
  return request({ url: '/zhpg/dataSource/test', method: 'post', data })
}

export function uploadDataSourceFile(data) {
  return request({
    url: '/zhpg/dataSource/uploadFile',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
