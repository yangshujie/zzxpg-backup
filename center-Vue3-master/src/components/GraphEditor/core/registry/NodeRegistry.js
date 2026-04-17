// src/components/GraphEditor/core/registry/NodeRegistry.js

class NodeRegistry {
  constructor() {
    this.nodes = new Map();
    this.x6Nodes = new Map();
    this.g6Nodes = new Map();
    // 新增：样式模式映射
    this.styleModes = new Map();  // 存储不同模式下的样式配置
  }

  /**
   * 注册节点类型（支持多模式）
   * @param {String} type - 节点类型
   * @param {Object} config - 节点配置
   */
  register(type, config) {
    this.nodes.set(type, config);
    
    // 存储 X6 配置（编辑模式）
    if (config.x6) {
      this.x6Nodes.set(type, {
        ...config.x6,
        mode: 'edit'  // 标记为编辑模式
      });
    }
    
    // 存储 G6 配置（展示模式）
    if (config.g6) {
      this.g6Nodes.set(type, {
        ...config.g6,
        mode: 'view'  // 标记为展示模式
      });
    }
    
    // 存储不同展示模式的样式配置
    if (config.styles) {
      this.styleModes.set(type, config.styles);
    }
  }

  /**
   * 获取节点配置（支持模式切换）
   * @param {String} type - 节点类型
   * @param {String} engine - 引擎类型 ('x6' | 'g6')
   * @param {String} mode - 模式 ('edit' | 'view' | 'highlight' | 'selected')
   */
  getNodeConfig(type, engine, mode = 'edit') {
    let nodeConfig = null;
    
    if (engine === 'x6') {
      nodeConfig = this.x6Nodes.get(type);
    } else {
      nodeConfig = this.g6Nodes.get(type);
    }
    
    if (!nodeConfig) return null;
    
    // 如果有特殊模式的样式配置，进行覆盖
    if (this.styleModes.has(type)) {
      const styleConfig = this.styleModes.get(type);
      if (styleConfig[mode]) {
        return {
          ...nodeConfig,
          ...styleConfig[mode]
        };
      }
    }
    
    return nodeConfig;
  }

  /**
   * 获取节点的展示样式（根据状态）
   * @param {String} type - 节点类型
   * @param {Object} state - 节点状态（selected, hover, highlight等）
   */
  getNodeStyleByState(type, state) {
    const styles = this.styleModes.get(type);
    if (!styles) return null;
    
    // 优先级：selected > highlight > hover > default
    if (state.selected && styles.selected) return styles.selected;
    if (state.highlight && styles.highlight) return styles.highlight;
    if (state.hover && styles.hover) return styles.hover;
    return styles.default || null;
  }
}

export const nodeRegistry = new NodeRegistry();