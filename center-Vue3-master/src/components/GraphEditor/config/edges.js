// src/components/GraphEditor/config/edges.js

import { edgeRegistry } from '../core/registry/EdgeRegistry';

// 注册通用连线类型
const commonEdges = [
  {
    type: 'flow',
    label: '流程连线',
    x6: {
      shape: 'edge',
      attrs: {
        line: {
          stroke: '#5F95FF',
          strokeWidth: 2,
          targetMarker: {
            name: 'block',
            width: 8,
            height: 8,
          }
        }
      }
    },
    g6: {
      type: 'line',
      style: {
        stroke: '#5F95FF',
        lineWidth: 2,
        endArrow: {
          // G6 v4 箭头配置
          path: 'M 0,0 L 8,4 L 8,-4 Z',  // 三角形路径
          fill: '#5F95FF',
          d: 8  // 箭头长度
        }
      }
    }
  },
  {
    type: 'relation',
    label: '关系连线',
    x6: {
      shape: 'edge',
      attrs: {
        line: {
          stroke: '#909399',
          strokeWidth: 2,
          strokeDasharray: '5 5',
          targetMarker: {
            name: 'circle',
            width: 6,
            height: 6,
          }
        }
      }
    },
    g6: {
      type: 'line',
      style: {
        stroke: '#909399',
        lineWidth: 2,
        lineDash: [5, 5],
        endArrow: {
          // G6 v4 圆形箭头
          path: 'M 0,0 A 4,4 0 1,0 8,0 A 4,4 0 1,0 0,0 Z',
          fill: '#909399',
          d: 8
        }
      }
    }
  },
  {
    type: 'inheritance',
    label: '继承关系',
    x6: {
      shape: 'edge',
      attrs: {
        line: {
          stroke: '#67C23A',
          strokeWidth: 2,
          targetMarker: {
            name: 'triangle',
            width: 10,
            height: 10,
          }
        }
      }
    },
    g6: {
      type: 'line',
      style: {
        stroke: '#67C23A',
        lineWidth: 2,
        endArrow: {
          path: 'M 0,0 L 10,5 L 10,-5 Z',  // 三角形
          fill: '#67C23A',
          d: 10
        }
      }
    }
  },
  {
    type: 'dependency',
    label: '依赖关系',
    x6: {
      shape: 'edge',
      attrs: {
        line: {
          stroke: '#E6A23C',
          strokeWidth: 2,
          strokeDasharray: '5 5',
          targetMarker: {
            name: 'vee',
            width: 8,
            height: 8,
          }
        }
      }
    },
    g6: {
      type: 'line',
      style: {
        stroke: '#E6A23C',
        lineWidth: 2,
        lineDash: [5, 5],
        endArrow: {
          path: 'M 0,0 L 8,4 L 0,8 L 4,4 Z',  // V 形箭头
          fill: '#E6A23C',
          d: 8
        }
      }
    }
  }
];

// 注册所有连线
commonEdges.forEach(edge => {
  edgeRegistry.register(edge.type, edge);
});

export const edgeConfig = commonEdges;