// src/components/GraphEditor/utils/dataConverter.js

export class DataConverter {
  /**
   * 转换 X6 数据到统一格式（包含组合信息）
   */
  static fromX6(x6Data) {
    const nodes = [];
    const edges = [];
    const combos = [];
    
    x6Data.cells.forEach(cell => {
      // 识别组合节点
      if (cell.shape === 'group' || cell.data?.isCombo) {
        combos.push({
          id: cell.id,
          label: cell.attrs?.label?.text || '组合',
          type: 'group',
          children: cell.children || [],
          position: cell.position,
          size: cell.size,
          collapsed: cell.data?.collapsed || false,
        });
      } 
      // 普通节点
      else if (cell.shape && !cell.source) {
        nodes.push({
          id: cell.id,
          type: cell.shape,
          label: cell.attrs?.label?.text || '',
          x: cell.position?.x || 0,
          y: cell.position?.y || 0,
          width: cell.size?.width || 100,
          height: cell.size?.height || 40,
          comboId: cell.data?.comboId,  // 所属组合
          attrs: cell.attrs,
          data: cell.data,
        });
      }
      // 边
      else if (cell.source && cell.target) {
        edges.push({
          id: cell.id,
          source: cell.source.cell,
          target: cell.target.cell,
          label: cell.attrs?.label?.text || '',
          type: cell.shape || 'edge',
          weight: cell.data?.weight,  // 边权重
          attrs: cell.attrs,
        });
      }
    });
    
    return { nodes, edges, combos };
  }

  /**
   * 转换 G6 数据到统一格式
   */
  static fromG6(g6Data) {
    return {
      nodes: g6Data.nodes.map(node => ({
        id: node.id,
        type: node.type,
        label: node.label,
        x: node.x,
        y: node.y,
        comboId: node.comboId,
        style: node.style,
        data: node.data,
      })),
      edges: g6Data.edges.map(edge => ({
        id: edge.id,
        source: edge.source,
        target: edge.target,
        label: edge.label,
        type: edge.type,
        weight: edge.weight,
        style: edge.style,
      })),
      combos: g6Data.combos || [],
    };
  }

  /**
   * 转换统一格式到 G6 格式（用于展示）
   */
  static toG6ForDisplay(data, options = {}) {
    const { layout = 'force', highlightNodes = [], highlightEdges = [] } = options;
    
    // 处理节点样式
    const nodes = data.nodes.map(node => {
      // 获取展示样式
      const nodeConfig = nodeRegistry.getNodeConfig(node.type, 'g6', 'view');
      const isHighlight = highlightNodes.includes(node.id);
      
      return {
        id: node.id,
        type: node.type,
        label: node.label,
        comboId: node.comboId,
        style: {
          ...nodeConfig?.style,
          ...(isHighlight ? { fill: '#F6BD16', stroke: '#F6BD16', lineWidth: 3 } : {}),
          ...node.style,
        },
        labelCfg: nodeConfig?.labelCfg,
      };
    });
    
    // 处理边样式
    const edges = data.edges.map(edge => {
      const edgeConfig = edgeRegistry.getEdgeConfig(edge.type, 'g6');
      const isHighlight = highlightEdges.includes(edge.id);
      
      return {
        id: edge.id,
        source: edge.source,
        target: edge.target,
        label: edge.label,
        style: {
          ...edgeConfig?.style,
          ...(isHighlight ? { stroke: '#F6BD16', lineWidth: 4 } : {}),
          ...(edge.weight ? { stroke: this._getWeightColor(edge.weight) } : {}),
          ...edge.style,
        },
      };
    });
    
    return {
      nodes,
      edges,
      combos: data.combos || [],
      layout: layout === 'force' ? {
        type: 'force',
        preventOverlap: true,
        nodeSize: 60,
        linkDistance: 150,
      } : {
        type: 'dagre',
        rankdir: 'TB',
        nodesep: 50,
        ranksep: 80,
      },
    };
  }

  /**
   * 根据权重获取颜色
   */
  static _getWeightColor(weight) {
    if (weight > 0.8) return '#F56C6C';
    if (weight > 0.5) return '#E6A23C';
    if (weight > 0.3) return '#67C23A';
    return '#5B8FF9';
  }
}