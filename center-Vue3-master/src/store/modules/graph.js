// src/store/modules/graph.js
import { defineStore } from 'pinia';
import { getGraphData, saveGraphData } from '@/api/graph';

export const useGraphStore = defineStore('graph', {
  state: () => ({
    // 当前场景
    currentScene: 'flowChart',
    // 图数据
    graphData: {
      nodes: [],
      edges: []
    },
    // 选中的节点/边
    selected: {
      type: null,
      id: null,
      data: null
    },
    // 历史记录
    history: {
      past: [],
      future: []
    },
    // 加载状态
    loading: false,
    // 是否已修改
    isModified: false,
    // 当前工具
    currentTool: 'select',
    // 工具状态
    toolStates: {
      zoom: 1
    }
  }),

  getters: {
    // 当前场景配置
    currentSceneConfig: (state) => {
      const scenes = {
        flowChart: { type: 'x6', name: '流程图' },
        knowledgeGraph: { type: 'g6', name: '知识图谱' },
        umlDiagram: { type: 'x6', name: 'UML图' },
        socialNetwork: { type: 'g6', name: '社交网络' }
      };
      return scenes[state.currentScene] || scenes.flowChart;
    },
    
    // 节点数量
    nodeCount: (state) => state.graphData.nodes.length,
    
    // 边数量
    edgeCount: (state) => state.graphData.edges.length,
    
    // 是否有未保存的修改
    hasUnsavedChanges: (state) => state.isModified,
    
    // 是否可以撤销
    canUndo: (state) => state.history.past.length > 0,
    
    // 是否可以重做
    canRedo: (state) => state.history.future.length > 0
  },

  actions: {
    // 设置当前场景
    setCurrentScene(scene) {
      this.currentScene = scene;
    },
    
    // 设置图数据
    setGraphData(data) {
      this.graphData = data;
      this.isModified = false;
    },
    
    // 更新图数据
    updateGraphData(data) {
      this.graphData = data;
      this.isModified = true;
    },
    
    // 设置选中项
    setSelected({ type, id, data }) {
      this.selected = { type, id, data };
    },
    
    // 清除选中项
    clearSelected() {
      this.selected = { type: null, id: null, data: null };
    },
    
    // 添加历史记录
    pushHistory(data) {
      this.history.past.push(JSON.parse(JSON.stringify(data)));
      if (this.history.past.length > 50) {
        this.history.past.shift();
      }
      this.history.future = [];
    },
    
    // 撤销
    undo() {
      if (this.history.past.length > 0) {
        const previous = this.history.past.pop();
        this.history.future.push(JSON.parse(JSON.stringify(this.graphData)));
        this.graphData = previous;
        this.isModified = true;
      }
    },
    
    // 重做
    redo() {
      if (this.history.future.length > 0) {
        const next = this.history.future.pop();
        this.history.past.push(JSON.parse(JSON.stringify(this.graphData)));
        this.graphData = next;
        this.isModified = true;
      }
    },
    
    // 设置加载状态
    setLoading(loading) {
      this.loading = loading;
    },
    
    // 设置修改状态
    setModified(modified) {
      this.isModified = modified;
    },
    
    // 设置当前工具
    setCurrentTool(tool) {
      this.currentTool = tool;
    },
    
    // 设置工具状态
    setToolStates(states) {
      this.toolStates = { ...this.toolStates, ...states };
    },
    
    // 加载图数据
    async loadGraphData() {
      this.setLoading(true);
      try {
        const res = await getGraphData({ scene: this.currentScene });
        this.setGraphData(res.data);
        this.pushHistory(res.data);
        return res.data;
      } catch (error) {
        console.error('加载图数据失败:', error);
        throw error;
      } finally {
        this.setLoading(false);
      }
    },
    
    // 保存图数据
    async saveGraphData() {
      this.setLoading(true);
      try {
        await saveGraphData({
          scene: this.currentScene,
          data: this.graphData
        });
        this.setModified(false);
      } catch (error) {
        console.error('保存图数据失败:', error);
        throw error;
      } finally {
        this.setLoading(false);
      }
    },
    
    // 切换场景
    async switchScene(scene) {
      this.setCurrentScene(scene);
      await this.loadGraphData();
    }
  }
});