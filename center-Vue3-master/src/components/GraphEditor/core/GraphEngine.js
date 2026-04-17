// src/components/GraphEditor/core/GraphEngine.js

import { X6Adapter } from './adapters/X6Adapter';
import { G6Adapter } from './adapters/G6Adapter';

export class GraphEngine {
  constructor() {
    this.adapter = null;
    this.currentScene = null;
  }

  /**
   * 初始化图引擎
   * @param {String} type - 引擎类型：'x6' | 'g6'
   * @param {HTMLElement} container - 容器DOM
   * @param {Object} config - 配置
   */
  init(type, container, config) {
    // 根据类型选择适配器
    switch (type) {
      case 'x6':
        this.adapter = new X6Adapter();
        break;
      case 'g6':
        this.adapter = new G6Adapter();
        break;
      default:
        throw new Error(`Unsupported graph type: ${type}`);
    }
    
    this.adapter.init(container, config);
    this.currentScene = config.scene;
    
    return this.adapter;
  }

  /**
   * 获取适配器实例
   */
  getAdapter() {
    return this.adapter;
  }

  /**
   * 销毁引擎
   */
  destroy() {
    if (this.adapter) {
      this.adapter.destroy();
      this.adapter = null;
    }
  }

  /**
   * 切换场景
   */
  switchScene(type, container, config) {
    this.destroy();
    return this.init(type, container, config);
  }
}

// 单例模式
export const graphEngine = new GraphEngine();