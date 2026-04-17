// src/components/GraphEditor/config/scenes.js

// 场景配置
export const scenes = {
  // 流程图场景（使用X6 - 编辑功能）
  flowChart: {
    type: 'x6',
    name: '流程图',
    description: '适用于业务流程设计、审批流配置等',
    defaultConfig: {
      grid: true,
      history: true,
      keyboard: true,
      width: '100%',
      height: '100%',
    },
    nodeTypes: ['start', 'process', 'decision', 'end'],
    edgeTypes: ['flow'],
    toolbar: ['undo', 'redo', 'save', 'zoomIn', 'zoomOut', 'fitView'],
  },
  
  // 知识图谱场景（使用G6 - 可视化分析）
  knowledgeGraph: {
    type: 'g6',
    name: '知识图谱',
    description: '适用于数据关系分析、知识发现等',
    defaultConfig: {
      layout: {
        type: 'force',
        preventOverlap: true,
        nodeSize: 60,
        linkDistance: 150,
      },
      modes: {
        default: ['drag-canvas', 'zoom-canvas', 'click-select'],
      },
      width: '100%',
      height: '100%',
    },
    nodeTypes: ['entity', 'concept', 'attribute'],
    edgeTypes: ['relation'],
    toolbar: ['zoomIn', 'zoomOut', 'fitView', 'export'],
  },
  
  // UML图场景（使用X6 - 精确绘制）
  umlDiagram: {
    type: 'x6',
    name: 'UML类图',
    description: '适用于软件设计、系统建模等',
    defaultConfig: {
      grid: true,
      snapline: true,
      connecting: {
        router: 'orth',
        connector: 'rounded',
      },
      width: '100%',
      height: '100%',
    },
    nodeTypes: ['class', 'interface', 'abstract'],
    edgeTypes: ['inheritance', 'association', 'dependency'],
    toolbar: ['undo', 'redo', 'save', 'zoomIn', 'zoomOut', 'fitView'],
  },
  
  // 社交网络图场景（使用G6 - 大规模数据）
  socialNetwork: {
    type: 'g6',
    name: '社交网络',
    description: '适用于社交关系分析、影响力传播等',
    defaultConfig: {
      layout: {
        type: 'force',
        preventOverlap: true,
        nodeSize: 50,
        linkDistance: 100,
        nodeStrength: -200,
        edgeStrength: 0.5,
      },
      modes: {
        default: ['drag-canvas', 'zoom-canvas', 'brush-select'],
      },
      width: '100%',
      height: '100%',
    },
    nodeTypes: ['person', 'group', 'organization'],
    edgeTypes: ['follow', 'friend', 'work'],
    toolbar: ['zoomIn', 'zoomOut', 'fitView', 'search', 'filter'],
  },
};