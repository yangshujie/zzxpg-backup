// src/components/GraphEditor/index.js

import GraphContainer from './GraphContainer.vue';
import LeftPanel from './LeftPanel.vue';
import Toolbar from './Toolbar.vue';
import PropertyPanel from './PropertyPanel.vue';

// 导出核心模块
export { GraphContainer, LeftPanel, Toolbar, PropertyPanel };

// 导出组合式函数
export { useGraph } from './composables/useGraph';
export { useGraphEvents } from './composables/useGraphEvents';
export { useGraphTools } from './composables/useGraphTools';
export { useSceneConfig } from './composables/useSceneConfig';

// 导出配置
export { scenes } from './config/scenes';
export { nodeConfig } from './config/nodes';
export { edgeConfig } from './config/edges';

// 导出工具类
export { DataConverter } from './utils/dataConverter';
export { eventBus } from './utils/eventBus';

// 默认导出组件
export default {
  install(app) {
    app.component('GraphContainer', GraphContainer);
    app.component('LeftPanel', LeftPanel);
    app.component('Toolbar', Toolbar);
    app.component('PropertyPanel', PropertyPanel);
  }
};