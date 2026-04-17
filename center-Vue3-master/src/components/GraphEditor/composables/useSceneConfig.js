// src/components/GraphEditor/composables/useSceneConfig.js

import { ref, computed } from 'vue';
import { scenes } from '../config/scenes';

/**
 * 场景配置Hook
 */
export function useSceneConfig(sceneType) {
  const currentScene = ref(sceneType);
  
  // 当前场景配置
  const sceneConfig = computed(() => {
    return scenes[currentScene.value] || scenes.flowChart;
  });
  
  // 场景类型（x6/g6）
  const engineType = computed(() => sceneConfig.value.type);
  
  // 是否支持编辑
  const isEditable = computed(() => {
    return engineType.value === 'x6';
  });
  
  // 是否支持布局
  const hasLayout = computed(() => {
    return engineType.value === 'g6';
  });
  
  // 工具栏配置
  const toolbarConfig = computed(() => {
    return sceneConfig.value.toolbar || [];
  });
  
  // 节点类型列表
  const nodeTypes = computed(() => {
    return sceneConfig.value.nodeTypes || [];
  });
  
  // 连线类型列表
  const edgeTypes = computed(() => {
    return sceneConfig.value.edgeTypes || [];
  });
  
  /**
   * 切换场景
   */
  const switchScene = (scene) => {
    currentScene.value = scene;
  };
  
  /**
   * 获取场景名称
   */
  const getSceneName = () => {
    return sceneConfig.value.name;
  };
  
  /**
   * 获取场景描述
   */
  const getSceneDescription = () => {
    return sceneConfig.value.description;
  };
  
  return {
    currentScene,
    sceneConfig,
    engineType,
    isEditable,
    hasLayout,
    toolbarConfig,
    nodeTypes,
    edgeTypes,
    switchScene,
    getSceneName,
    getSceneDescription
  };
}