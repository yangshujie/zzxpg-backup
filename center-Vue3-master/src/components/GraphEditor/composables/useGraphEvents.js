// src/components/GraphEditor/composables/useGraphEvents.js

import { ref, onMounted, onUnmounted } from 'vue';
import { eventBus } from '../utils/eventBus';

/**
 * 图事件管理Hook
 */
export function useGraphEvents(adapter) {
  const events = ref(new Map());
  
  /**
   * 注册事件监听
   * @param {String} event - 事件名称
   * @param {Function} handler - 处理函数
   */
  const on = (event, handler) => {
    if (adapter.value) {
      adapter.value.on(event, handler);
      events.value.set(event, handler);
    }
  };
  
  /**
   * 移除事件监听
   */
  const off = (event) => {
    if (adapter.value && events.value.has(event)) {
      adapter.value.off(event, events.value.get(event));
      events.value.delete(event);
    }
  };
  
  /**
   * 清除所有事件
   */
  const clear = () => {
    for (const [event, handler] of events.value.entries()) {
      if (adapter.value) {
        adapter.value.off(event, handler);
      }
    }
    events.value.clear();
  };
  
  // 组件卸载时自动清理
  onUnmounted(() => {
    clear();
  });
  
  return {
    on,
    off,
    clear
  };
}