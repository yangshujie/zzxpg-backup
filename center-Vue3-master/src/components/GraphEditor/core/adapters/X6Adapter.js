// src/components/GraphEditor/core/adapters/X6Adapter.js

import { Graph, Shape } from '@antv/x6';
import { BaseAdapter } from './BaseAdapter';

export class X6Adapter extends BaseAdapter {
  init(container, config) {
    super.init(container, config);
    
    this.graph = new Graph({
      container: this.container,
      width: config.width || 800,
      height: config.height || 600,
      grid: config.grid !== false,  // X6特有：网格
      panning: {
        enabled: true,
        modifiers: 'shift',  // X6特有：拖动画布修饰键
      },
      mousewheel: {
        enabled: true,
        modifiers: 'ctrl',   // X6特有：缩放修饰键
      },
      connecting: {
        router: 'manhattan',  // X6特有：连线路由
        connector: 'rounded',
        allowBlank: false,
        allowLoop: false,
        highlight: true,
      },
      keyboard: {
        enabled: true,        // X6特有：键盘快捷键
        global: true,
      },
      history: {
        enabled: true,        // X6特有：撤销/重做
      },
      ...config,
    });

    // 注册自定义节点
    this._registerNodes();
    
    // 绑定事件
    this._bindAllEvents();
  }

  destroy() {
    if (this.graph) {
      this.graph.dispose();
      this.graph = null;
    }
    super.destroy();
  }

  addNode(node) {
    const x6Node = {
      id: node.id,
      shape: node.type || 'rect',
      x: node.x || 100,
      y: node.y || 100,
      width: node.width || 100,
      height: node.height || 40,
      label: node.label,
      attrs: {
        body: {
          fill: node.fill || '#fff',
          stroke: node.stroke || '#5F95FF',
          strokeWidth: 1,
        },
        label: {
          text: node.label,
          fill: '#333',
          fontSize: 12,
        },
      },
      ...node,
    };
    
    this.graph.addNode(x6Node);
  }

  addEdge(edge) {
    const x6Edge = {
      id: edge.id,
      source: edge.source,
      target: edge.target,
      shape: 'edge',
      attrs: {
        line: {
          stroke: edge.stroke || '#5F95FF',
          strokeWidth: 2,
          targetMarker: {  // X6特有：箭头标记
            name: 'block',
            width: 8,
            height: 8,
          },
        },
      },
      ...edge,
    };
    
    this.graph.addEdge(x6Edge);
  }

  removeNode(id) {
    const node = this.graph.getCellById(id);
    if (node) {
      node.remove();
    }
  }

  removeEdge(id) {
    const edge = this.graph.getCellById(id);
    if (edge) {
      edge.remove();
    }
  }

  updateNode(id, config) {
    const node = this.graph.getCellById(id);
    if (node) {
      node.setAttrs(config.attrs);
      if (config.position) {
        node.setPosition(config.position);
      }
      if (config.label) {
        node.setAttrs({ label: { text: config.label } });
      }
    }
  }

  updateEdge(id, config) {
    const edge = this.graph.getCellById(id);
    if (edge) {
      if (config.attrs) {
        edge.setAttrs(config.attrs);
      }
      if (config.source) {
        edge.setSource(config.source);
      }
      if (config.target) {
        edge.setTarget(config.target);
      }
    }
  }

  importData(data) {
    this.graph.fromJSON(data);
  }

  exportData() {
    return this.graph.toJSON();
  }

  zoomTo(scale) {
    this.graph.zoomTo(scale);
  }

  centerContent() {
    this.graph.centerContent();
  }

  fitView() {
    this.graph.fitView();
  }

  _registerNodes() {
    // X6特有：注册自定义节点（支持HTML/React/Vue组件）
    Graph.registerNode('custom-rect', {
      inherit: 'rect',
      width: 120,
      height: 48,
      attrs: {
        body: {
          fill: '#fff',
          stroke: '#5F95FF',
          strokeWidth: 2,
          rx: 6,
          ry: 6,
        },
        label: {
          fontSize: 12,
          fill: '#333',
        },
      },
    });
    
    Graph.registerNode('custom-circle', {
      inherit: 'circle',
      width: 60,
      height: 60,
      attrs: {
        body: {
          fill: '#fff',
          stroke: '#5F95FF',
          strokeWidth: 2,
        },
        label: {
          fontSize: 12,
          fill: '#333',
        },
      },
    });
  }

  _bindAllEvents() {
    // X6特有：节点点击事件
    this.graph.on('node:click', ({ node }) => {
      this._emit('node:click', { id: node.id, data: node.getData() });
    });
    
    // X6特有：节点拖拽移动事件
    this.graph.on('node:moved', ({ node }) => {
      this._emit('node:moved', {
        id: node.id,
        position: node.getPosition(),
      });
    });
    
    // X6特有：连线事件
    this.graph.on('edge:connected', ({ edge }) => {
      this._emit('edge:connected', {
        id: edge.id,
        source: edge.getSource(),
        target: edge.getTarget(),
      });
    });
    
    // X6特有：撤销/重做事件
    this.graph.on('history:change', ({ cmd }) => {
      this._emit('history:change', { canUndo: this.graph.canUndo(), canRedo: this.graph.canRedo() });
    });
  }

  _bindEvent(event, handler) {
    // 实际事件绑定由 _bindAllEvents 统一处理
    // 这里只是存储handler，实际触发时调用
  }
  
  _emit(event, data) {
    const handlers = this.eventHandlers.get(event);
    if (handlers) {
      handlers.forEach(handler => handler(data));
    }
  }
}