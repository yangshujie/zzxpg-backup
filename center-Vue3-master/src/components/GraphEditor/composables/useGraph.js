// src/components/GraphEditor/composables/useGraph.js

import { ref, onMounted, onUnmounted } from 'vue';
import { graphEngine } from '../core/GraphEngine';

export function useGraph(containerRef, options) {
  const adapter = ref(null);
  const loading = ref(false);
  const error = ref(null);
  
  const init = async (type, config) => {
    try {
      loading.value = true;
      
      // 获取容器DOM
      const container = containerRef.value;
      if (!container) {
        throw new Error('Container not found');
      }
      
      // 初始化引擎
      adapter.value = graphEngine.init(type, container, {
        ...options,
        ...config,
      });
      
      loading.value = false;
      
      // 触发初始化完成事件
      options.onInit?.(adapter.value);
      
    } catch (err) {
      error.value = err;
      loading.value = false;
      options.onError?.(err);
    }
  };
  
  const destroy = () => {
    graphEngine.destroy();
    adapter.value = null;
  };
  
  const importData = (data) => {
    if (adapter.value) {
      adapter.value.importData(data);
    }
  };
  
  const exportData = () => {
    if (adapter.value) {
      return adapter.value.exportData();
    }
    return null;
  };
  
  const addNode = (node) => {
    adapter.value?.addNode(node);
  };
  
  const addEdge = (edge) => {
    adapter.value?.addEdge(edge);
  };
  
  const on = (event, handler) => {
    adapter.value?.on(event, handler);
  };
  
  // 生命周期
  onMounted(() => {
    if (options.autoInit !== false) {
      init(options.type, options.config);
    }
  });
  
  onUnmounted(() => {
    destroy();
  });
  
  return {
    adapter,
    loading,
    error,
    init,
    destroy,
    importData,
    exportData,
    addNode,
    addEdge,
    on,
  };
}