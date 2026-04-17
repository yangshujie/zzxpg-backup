// src/components/GraphEditor/core/adapters/G6Adapter.js

import * as G6 from '@antv/g6';
import { BaseAdapter } from './BaseAdapter';
import { nodeRegistry } from '../registry/NodeRegistry';
import { edgeRegistry } from '../registry/EdgeRegistry';

export class G6Adapter extends BaseAdapter {
  constructor() {
    super();
    this.engineType = 'g6';
    this.combos = new Map();  // 存储组合关系
    this.nodeStates = new Map();  // 存储节点状态
  }

  init(container, config) {
    super.init(container, config);
    
    // 注册自定义行为
    this._registerBehaviors();
    
    this.graph = new Graph({
      container: this.container,
      width: config.width || 800,
      height: config.height || 600,
      modes: {
        default: [
          'drag-canvas',
          'zoom-canvas',
          'drag-node',
          'click-select',
          // 新增：组合相关行为
          'collapse-expand-combo',
          'drag-combo',
        ],
      },
      layout: config.layout || {
        type: 'dagre',  // 使用层次布局
        rankdir: 'TB',
        align: 'UL',
        nodesep: 50,
        ranksep: 80,
      },
      // 启用组合功能
      groupByTypes: false,
      defaultNode: this._getDefaultNodeConfig(),
      defaultEdge: this._getDefaultEdgeConfig(),
      defaultCombo: this._getDefaultComboConfig(),
      ...config,
    });
    
    this.graph.data({ nodes: [], edges: [], combos: [] });
    this.graph.render();
    
    this._bindAllEvents();
  }

  /**
   * 注册自定义行为
   */
  _registerBehaviors() {
    // 注册组合折叠/展开行为
    G6.registerBehavior('collapse-expand-combo', {
      getEvents() {
        return {
          'combo:click': 'onComboClick',
        };
      },
      onComboClick(evt) {
        const combo = evt.item;
        const model = combo.getModel();
        const isCollapsed = model.collapsed;
        
        // 折叠或展开组合
        this.graph.collapseExpandCombo(combo, !isCollapsed);
        
        // 触发事件
        this.graph.emit('combo:collapse-expand', {
          comboId: model.id,
          collapsed: !isCollapsed,
        });
      },
    });
  }

  /**
   * 获取默认组合配置
   */
  _getDefaultComboConfig() {
    return {
      type: 'rect',
      style: {
        fill: '#F5F7FA',
        stroke: '#DCDFE6',
        lineWidth: 2,
        radius: 8,
        lineDash: [5, 5],
      },
      labelCfg: {
        style: {
          fill: '#606266',
          fontSize: 12,
        },
        position: 'top',
        offsetY: -5,
      },
    };
  }

  /**
   * 添加组合
   * @param {Object} combo - 组合配置
   */
  addCombo(combo) {
    const g6Combo = {
      id: combo.id,
      label: combo.label,
      type: combo.type || 'group',
      parentId: combo.parentId,
      children: combo.children || [],
      collapsed: combo.collapsed || false,
      style: combo.style,
      ...combo,
    };
    
    this.graph.addItem('combo', g6Combo);
    this.combos.set(combo.id, g6Combo);
    
    return g6Combo;
  }

  /**
   * 添加节点（支持组合）
   */
  _doAddNode(node) {
    // 获取节点配置（展示模式）
    const nodeConfig = nodeRegistry.getNodeConfig(node.type, 'g6', 'view');
    
    const g6Node = {
      id: node.id,
      type: node.type,
      label: node.label,
      x: node.x || Math.random() * 500 + 100,
      y: node.y || Math.random() * 300 + 100,
      style: nodeConfig?.style || {},
      labelCfg: nodeConfig?.labelCfg || {},
      comboId: node.comboId,  // 所属组合
      data: node.data,
      ...node,
    };
    
    this.graph.addItem('node', g6Node);
    
    // 如果节点属于某个组合，自动布局
    if (node.comboId) {
      this.graph.updateComboTree(node.comboId);
    }
    
    return g6Node;
  }

  /**
   * 设置节点状态（用于动态样式）
   * @param {String} nodeId - 节点ID
   * @param {String} state - 状态类型
   * @param {Boolean} value - 状态值
   */
  setNodeState(nodeId, state, value = true) {
    const node = this.graph.findById(nodeId);
    if (!node) return;
    
    // 保存状态
    if (!this.nodeStates.has(nodeId)) {
      this.nodeStates.set(nodeId, {});
    }
    const states = this.nodeStates.get(nodeId);
    states[state] = value;
    
    // 应用样式
    const nodeModel = node.getModel();
    const styleConfig = nodeRegistry.getNodeStyleByState(nodeModel.type, states);
    
    if (styleConfig) {
      this.graph.updateItem(node, {
        style: styleConfig.style,
        labelCfg: styleConfig.labelCfg,
      });
    }
    
    // 添加视觉反馈
    if (state === 'highlight' && value) {
      this.graph.setItemState(node, 'highlight', true);
    } else if (state === 'selected' && value) {
      this.graph.setItemState(node, 'selected', true);
    } else {
      this.graph.setItemState(node, state, value);
    }
  }

  /**
   * 高亮路径（展示关系）
   * @param {String} startNodeId - 起始节点
   * @param {String} endNodeId - 结束节点
   */
  highlightPath(startNodeId, endNodeId) {
    // 清除所有高亮
    this.clearHighlight();
    
    // 计算最短路径
    const path = this.graph.findAllPath(startNodeId, endNodeId);
    
    if (path && path.length > 0) {
      // 高亮路径上的节点和边
      path[0].forEach(nodeId => {
        this.setNodeState(nodeId, 'highlight', true);
      });
      
      // 高亮路径上的边
      for (let i = 0; i < path[0].length - 1; i++) {
        const sourceId = path[0][i];
        const targetId = path[0][i + 1];
        const edges = this.graph.findAll('edge', edge => {
          return (edge.getSource() === sourceId && edge.getTarget() === targetId) ||
                 (edge.getSource() === targetId && edge.getTarget() === sourceId);
        });
        
        edges.forEach(edge => {
          this.graph.setItemState(edge, 'highlight', true);
          this.graph.updateItem(edge, {
            style: {
              stroke: '#F6BD16',
              lineWidth: 3,
              shadowBlur: 10,
              shadowColor: '#F6BD16',
            }
          });
        });
      }
    }
  }

  /**
   * 清除所有高亮
   */
  clearHighlight() {
    // 清除节点高亮
    this.graph.getNodes().forEach(node => {
      const nodeId = node.getID();
      const states = this.nodeStates.get(nodeId);
      if (states) {
        states.highlight = false;
        this.setNodeState(nodeId, 'highlight', false);
      }
      this.graph.setItemState(node, 'highlight', false);
    });
    
    // 清除边高亮
    this.graph.getEdges().forEach(edge => {
      this.graph.setItemState(edge, 'highlight', false);
      this.graph.updateItem(edge, {
        style: {
          stroke: '#5B8FF9',
          lineWidth: 2,
          shadowBlur: 0,
        }
      });
    });
  }

  /**
   * 展开组合
   */
  expandCombo(comboId) {
    const combo = this.graph.findById(comboId);
    if (combo) {
      this.graph.collapseExpandCombo(combo, false);
      this.graph.layout();
    }
  }

  /**
   * 折叠组合
   */
  collapseCombo(comboId) {
    const combo = this.graph.findById(comboId);
    if (combo) {
      this.graph.collapseExpandCombo(combo, true);
      this.graph.layout();
    }
  }

  /**
   * 更新边的样式（支持动态样式）
   */
  _doAddEdge(edge) {
    const edgeConfig = edgeRegistry.getEdgeConfig(edge.type, 'g6');
    
    const g6Edge = {
      id: edge.id,
      source: edge.source,
      target: edge.target,
      type: edge.type,
      label: edge.label,
      style: {
        stroke: edge.weight ? this._getWeightColor(edge.weight) : (edgeConfig?.style?.stroke || '#5B8FF9'),
        lineWidth: edge.weight ? 2 + edge.weight / 2 : (edgeConfig?.style?.lineWidth || 2),
        endArrow: edgeConfig?.style?.endArrow,
        ...edge.style,
      },
      ...edge,
    };
    
    this.graph.addItem('edge', g6Edge);
    return g6Edge;
  }

  /**
   * 根据权重获取颜色
   */
  _getWeightColor(weight) {
    if (weight > 0.8) return '#F56C6C';
    if (weight > 0.5) return '#E6A23C';
    if (weight > 0.3) return '#67C23A';
    return '#5B8FF9';
  }

  // ... 其他方法
}