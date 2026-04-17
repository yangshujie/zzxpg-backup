// src/components/GraphEditor/composables/useGraphTools.js

import { ref, reactive } from 'vue';

/**
 * 图工具Hook - 管理工具栏状态
 */
export function useGraphTools(adapter) {
  const currentTool = ref(null);
  const toolStates = reactive({
    zoom: 1,
    canUndo: false,
    canRedo: false,
    isDragging: false,
    selectedNodes: []
  });
  
  /**
   * 设置当前工具
   */
  const setTool = (tool) => {
    currentTool.value = tool;
    
    // 根据工具类型设置图模式
    if (adapter.value) {
      switch (tool) {
        case 'select':
          adapter.value.graph?.setMode?.('default');
          break;
        case 'drag':
          adapter.value.graph?.setMode?.('drag');
          break;
        case 'connect':
          adapter.value.graph?.setMode?.('connect');
          break;
      }
    }
  };
  
  /**
   * 放大
   */
  const zoomIn = () => {
    if (adapter.value) {
      const newZoom = toolStates.zoom + 0.1;
      adapter.value.zoomTo(newZoom);
      toolStates.zoom = newZoom;
    }
  };
  
  /**
   * 缩小
   */
  const zoomOut = () => {
    if (adapter.value) {
      const newZoom = Math.max(0.1, toolStates.zoom - 0.1);
      adapter.value.zoomTo(newZoom);
      toolStates.zoom = newZoom;
    }
  };
  
  /**
   * 重置缩放
   */
  const resetZoom = () => {
    if (adapter.value) {
      adapter.value.zoomTo(1);
      toolStates.zoom = 1;
    }
  };
  
  /**
   * 适应视图
   */
  const fitView = () => {
    if (adapter.value) {
      adapter.value.fitView();
    }
  };
  
  /**
   * 撤销
   */
  const undo = () => {
    if (adapter.value && toolStates.canUndo) {
      adapter.value.graph?.undo?.();
    }
  };
  
  /**
   * 重做
   */
  const redo = () => {
    if (adapter.value && toolStates.canRedo) {
      adapter.value.graph?.redo?.();
    }
  };
  
  /**
   * 导出
   */
  const exportData = () => {
    if (adapter.value) {
      return adapter.value.exportData();
    }
    return null;
  };
  
  /**
   * 导入
   */
  const importData = (data) => {
    if (adapter.value) {
      adapter.value.importData(data);
    }
  };
  
  /**
   * 更新工具状态
   */
  const updateToolState = (state) => {
    Object.assign(toolStates, state);
  };
  
  return {
    currentTool,
    toolStates,
    setTool,
    zoomIn,
    zoomOut,
    resetZoom,
    fitView,
    undo,
    redo,
    exportData,
    importData,
    updateToolState
  };
}