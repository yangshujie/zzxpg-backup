// src/components/GraphEditor/core/registry/PluginRegistry.js

/**
 * 插件注册中心 - 管理插件扩展
 */
class PluginRegistry {
  constructor() {
    this.plugins = new Map();
    this.x6Plugins = new Map();
    this.g6Plugins = new Map();
  }

  /**
   * 注册插件
   * @param {String} name - 插件名称
   * @param {Object} plugin - 插件实例
   */
  register(name, plugin) {
    this.plugins.set(name, plugin);
    
    if (plugin.x6) {
      this.x6Plugins.set(name, plugin.x6);
    }
    if (plugin.g6) {
      this.g6Plugins.set(name, plugin.g6);
    }
  }

  /**
   * 获取插件
   */
  getPlugin(name, engine = 'x6') {
    const plugin = this.plugins.get(name);
    if (!plugin) return null;
    
    return engine === 'x6' ? plugin.x6 : plugin.g6;
  }

  /**
   * 应用插件到图实例
   * @param {Object} graph - 图实例
   * @param {String} engine - 引擎类型
   */
  applyPlugins(graph, engine = 'x6') {
    for (const [name, plugin] of this.plugins.entries()) {
      const pluginImpl = engine === 'x6' ? plugin.x6 : plugin.g6;
      if (pluginImpl && typeof pluginImpl.apply === 'function') {
        pluginImpl.apply(graph);
      }
    }
  }
}

export const pluginRegistry = new PluginRegistry();