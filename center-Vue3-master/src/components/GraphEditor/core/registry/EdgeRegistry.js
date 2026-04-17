// src/components/GraphEditor/core/registry/EdgeRegistry.js

/**
 * 连线注册中心 - 管理所有连线类型
 */
class EdgeRegistry {
  constructor() {
    this.edges = new Map();
    this.x6Edges = new Map();
    this.g6Edges = new Map();
  }

  /**
   * 注册连线类型
   * @param {String} type - 连线类型
   * @param {Object} config - 连线配置
   */
  register(type, config) {
    this.edges.set(type, config);
    
    if (config.x6) {
      this.x6Edges.set(type, config.x6);
    }
    if (config.g6) {
      this.g6Edges.set(type, config.g6);
    }
  }

  /**
   * 批量注册连线
   */
  registerBatch(edges) {
    edges.forEach(edge => {
      this.register(edge.type, edge.config);
    });
  }

  /**
   * 获取连线配置
   */
  getEdgeConfig(type, engine = 'x6') {
    const edgeConfig = this.edges.get(type);
    if (!edgeConfig) return null;
    
    return engine === 'x6' ? edgeConfig.x6 : edgeConfig.g6;
  }

  /**
   * 获取所有连线类型
   */
  getAllTypes() {
    return Array.from(this.edges.keys());
  }
}

export const edgeRegistry = new EdgeRegistry();