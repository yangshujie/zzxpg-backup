// src/components/GraphEditor/utils/eventBus.js

/**
 * 事件总线 - 组件间通信
 */
class EventBus {
  constructor() {
    this.events = new Map();
  }

  /**
   * 订阅事件
   * @param {String} event - 事件名称
   * @param {Function} callback - 回调函数
   * @param {Object} context - 上下文
   */
  on(event, callback, context = null) {
    if (!this.events.has(event)) {
      this.events.set(event, []);
    }
    
    this.events.get(event).push({
      callback,
      context
    });
    
    // 返回取消订阅函数
    return () => this.off(event, callback);
  }

  /**
   * 订阅一次事件
   */
  once(event, callback, context = null) {
    const wrapper = (...args) => {
      callback.apply(context, args);
      this.off(event, wrapper);
    };
    
    return this.on(event, wrapper, context);
  }

  /**
   * 触发事件
   * @param {String} event - 事件名称
   * @param {...any} args - 参数
   */
  emit(event, ...args) {
    const handlers = this.events.get(event);
    if (handlers) {
      handlers.forEach(handler => {
        handler.callback.apply(handler.context, args);
      });
    }
  }

  /**
   * 取消订阅
   * @param {String} event - 事件名称
   * @param {Function} callback - 回调函数
   */
  off(event, callback) {
    const handlers = this.events.get(event);
    if (handlers) {
      if (callback) {
        const index = handlers.findIndex(h => h.callback === callback);
        if (index !== -1) {
          handlers.splice(index, 1);
        }
      } else {
        this.events.delete(event);
      }
    }
  }

  /**
   * 清除所有事件
   */
  clear() {
    this.events.clear();
  }

  /**
   * 获取所有事件名称
   */
  getEventNames() {
    return Array.from(this.events.keys());
  }
}

// 单例模式
export const eventBus = new EventBus();