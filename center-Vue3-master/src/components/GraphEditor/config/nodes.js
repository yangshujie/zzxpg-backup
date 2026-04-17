// src/components/GraphEditor/config/nodes.js

import { nodeRegistry } from '../core/registry/NodeRegistry';

// 注册通用节点类型
const commonNodes = [
  {
    type: 'start',
    label: '开始节点',
    icon: 'el-icon-video-play',
    preview: { shape: 'circle', size: 40, fill: '#67C23A' },
    x6: {
      shape: 'circle',
      width: 40,
      height: 40,
      attrs: {
        body: {
          fill: '#67C23A',
          stroke: '#529b2e',
          strokeWidth: 2,
        },
        label: {
          text: '开始',
          fill: '#fff',
          fontSize: 12,
        }
      }
    },
    g6: {
      type: 'circle',
      size: 40,
      style: {
        fill: '#67C23A',
        stroke: '#fff',
        lineWidth: 2,
      },
      labelCfg: {
        style: {
          fill: '#fff',
          fontSize: 12,
        }
      }
    }
  },
  {
    type: 'process',
    label: '处理节点',
    icon: 'el-icon-s-operation',
    preview: { shape: 'rect', size: [100, 40], fill: '#409EFF' },
    x6: {
      shape: 'rect',
      width: 100,
      height: 40,
      attrs: {
        body: {
          fill: '#409EFF',
          stroke: '#3069c4',
          strokeWidth: 2,
          rx: 6,
          ry: 6,
        },
        label: {
          text: '处理',
          fill: '#fff',
          fontSize: 12,
        }
      }
    },
    g6: {
      type: 'rect',
      size: [100, 40],
      style: {
        fill: '#409EFF',
        stroke: '#fff',
        lineWidth: 2,
        radius: 6,
      },
      labelCfg: {
        style: {
          fill: '#fff',
          fontSize: 12,
        }
      }
    }
  },
  {
    type: 'decision',
    label: '判断节点',
    icon: 'el-icon-question',
    preview: { shape: 'diamond', size: 60, fill: '#E6A23C' },
    x6: {
      shape: 'diamond',
      width: 60,
      height: 60,
      attrs: {
        body: {
          fill: '#E6A23C',
          stroke: '#b88230',
          strokeWidth: 2,
        },
        label: {
          text: '判断',
          fill: '#fff',
          fontSize: 12,
        }
      }
    },
    g6: {
      type: 'diamond',
      size: [60, 60],
      style: {
        fill: '#E6A23C',
        stroke: '#fff',
        lineWidth: 2,
      },
      labelCfg: {
        style: {
          fill: '#fff',
          fontSize: 12,
        }
      }
    }
  },
  {
    type: 'end',
    label: '结束节点',
    icon: 'el-icon-circle-close',
    preview: { shape: 'circle', size: 40, fill: '#F56C6C' },
    x6: {
      shape: 'circle',
      width: 40,
      height: 40,
      attrs: {
        body: {
          fill: '#F56C6C',
          stroke: '#c45656',
          strokeWidth: 2,
        },
        label: {
          text: '结束',
          fill: '#fff',
          fontSize: 12,
        }
      }
    },
    g6: {
      type: 'circle',
      size: 40,
      style: {
        fill: '#F56C6C',
        stroke: '#fff',
        lineWidth: 2,
      },
      labelCfg: {
        style: {
          fill: '#fff',
          fontSize: 12,
        }
      }
    }
  },
  {
    type: 'entity',
    label: '实体节点',
    icon: 'el-icon-user',
    preview: { shape: 'circle', size: 50, fill: '#409EFF' },
    x6: {
      shape: 'circle',
      width: 50,
      height: 50,
      attrs: {
        body: {
          fill: '#409EFF',
          stroke: '#3069c4',
          strokeWidth: 2,
        },
        label: {
          text: '实体',
          fill: '#fff',
          fontSize: 12,
        }
      }
    },
    g6: {
      type: 'circle',
      size: 50,
      style: {
        fill: '#409EFF',
        stroke: '#fff',
        lineWidth: 2,
      },
      labelCfg: {
        style: {
          fill: '#fff',
          fontSize: 12,
        }
      }
    }
  },
  {
    type: 'class',
    label: '类节点',
    icon: 'el-icon-s-grid',
    preview: { shape: 'rect', size: [120, 80], fill: '#E6A23C' },
    x6: {
      shape: 'rect',
      width: 120,
      height: 80,
      attrs: {
        body: {
          fill: '#E6A23C',
          stroke: '#b88230',
          strokeWidth: 2,
          rx: 4,
          ry: 4,
        },
        label: {
          text: '类',
          fill: '#fff',
          fontSize: 12,
        }
      }
    },
    g6: {
      type: 'rect',
      size: [120, 80],
      style: {
        fill: '#E6A23C',
        stroke: '#fff',
        lineWidth: 2,
        radius: 4,
      },
      labelCfg: {
        style: {
          fill: '#fff',
          fontSize: 12,
        }
      }
    }
  }
];

// 注册所有节点
commonNodes.forEach(node => {
  nodeRegistry.register(node.type, node);
});

// 导出节点配置
export const nodeConfig = commonNodes;

// 导出注册中心实例
export { nodeRegistry };