// src/components/GraphEditor/core/adapters/BaseAdapter.js

export class BaseAdapter {
  constructor() {
    this.graph = null;
    this.container = null;
    this.config = null;
    this.eventHandlers = new Map();
  }

  /**
   * 初始化图实例
   * @param {HTMLElement} container - 容器DOM
   * @param {Object} config - 配置
   */
  init(container, config) {
    this.container = container;
    this.config = config;
  }

  /**
   * 销毁图实例
   */
  destroy() {
    if (this.graph) {
      this.graph.dispose?.();
      this.graph = null;
    }
  }

  /**
   * 添加节点
   */
  addNode(node) {
    throw new Error('子类必须实现 addNode 方法');
  }

  /**
   * 添加边
   */
  addEdge(edge) {
    throw new Error('子类必须实现 addEdge 方法');
  }

  /**
   * 删除节点
   */
  removeNode(id) {
    throw new Error('子类必须实现 removeNode 方法');
  }

  /**
   * 删除边
   */
  removeEdge(id) {
    throw new Error('子类必须实现 removeEdge 方法');
  }

  /**
   * 更新节点
   */
  updateNode(id, config) {
    throw new Error('子类必须实现 updateNode 方法');
  }

  /**
   * 更新边
   */
  updateEdge(id, config) {
    throw new Error('子类必须实现 updateEdge 方法');
  }

  /**
   * 导入数据
   */
  importData(data) {
    throw new Error('子类必须实现 importData 方法');
  }

  /**
   * 导出数据
   */
  exportData() {
    throw new Error('子类必须实现 exportData 方法');
  }

  /**
   * 缩放
   */
  zoomTo(scale) {
    throw new Error('子类必须实现 zoomTo 方法');
  }

  /**
   * 居中显示
   */
  centerContent() {
    throw new Error('子类必须实现 centerContent 方法');
  }

  /**
   * 适应视图
   */
  fitView() {
    throw new Error('子类必须实现 fitView 方法');
  }

  /**
   * 注册事件
   */
  on(event, handler) {
    if (!this.eventHandlers.has(event)) {
      this.eventHandlers.set(event, []);
    }
    this.eventHandlers.get(event).push(handler);
    
    if (this.graph) {
      this._bindEvent(event, handler);
    }
  }

  /**
   * 移除事件
   */
  off(event, handler) {
    const handlers = this.eventHandlers.get(event);
    if (handlers) {
      const index = handlers.indexOf(handler);
      if (index > -1) {
        handlers.splice(index, 1);
      }
    }
  }

  /**
   * 绑定事件（子类实现）
   */
  _bindEvent(event, handler) {
    throw new Error('子类必须实现 _bindEvent 方法');
  }
}