// src/api/graph.js
import request from './request';

// 获取图数据
export function getGraphData(params) {
  return request({
    url: '/graph/data',
    method: 'get',
    params
  });
}

// 保存图数据
export function saveGraphData(data) {
  return request({
    url: '/graph/save',
    method: 'post',
    data
  });
}

// 导出图数据
export function exportGraphData(params) {
  return request({
    url: '/graph/export',
    method: 'get',
    params,
    responseType: 'blob'
  });
}

// 导入图数据
export function importGraphData(data) {
  return request({
    url: '/graph/import',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}