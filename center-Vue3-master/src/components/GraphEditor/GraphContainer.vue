<!-- src/components/GraphEditor/GraphContainer.vue -->
<template>
  <div class="graph-container">
    <div ref="canvasRef" class="canvas-wrapper"></div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue';
import { graphEngine } from './core/GraphEngine';
import { DataConverter } from './utils/dataConverter';

const props = defineProps({
  scene: String,
  mode: {
    type: String,
    default: 'edit'  // 'edit' 或 'view'
  },
  data: Object
});

const emit = defineEmits(['data-change']);

const canvasRef = ref(null);
let currentAdapter = null;

// 初始化
const init = () => {
  const sceneConfig = scenes[props.scene];
  const engineType = props.mode === 'edit' ? 'x6' : 'g6';
  
  currentAdapter = graphEngine.init(engineType, canvasRef.value, {
    ...sceneConfig.defaultConfig,
    mode: props.mode
  });
  
  // 导入数据
  if (props.data) {
    if (props.mode === 'view') {
      const displayData = DataConverter.toG6ForDisplay(props.data);
      currentAdapter.importData(displayData);
    } else {
      currentAdapter.importData(props.data);
    }
  }
  
  // 监听事件
  currentAdapter.on('data-change', (newData) => {
    emit('data-change', newData);
  });
};

// 切换到展示模式
const switchToView = (displayData) => {
  graphEngine.destroy();
  currentAdapter = graphEngine.init('g6', canvasRef.value, {
    ...scenes[props.scene].defaultConfig,
    mode: 'view'
  });
  currentAdapter.importData(displayData);
};

// 切换到编辑模式
const switchToEdit = (editData) => {
  graphEngine.destroy();
  currentAdapter = graphEngine.init('x6', canvasRef.value, {
    ...scenes[props.scene].defaultConfig,
    mode: 'edit'
  });
  currentAdapter.importData(editData);
};

// 高亮节点（展示模式）
const highlightNodes = (nodeIds) => {
  if (props.mode === 'view' && currentAdapter) {
    currentAdapter.highlightNodes?.(nodeIds);
  }
};

// 高亮路径（展示模式）
const highlightPath = (startId, endId) => {
  if (props.mode === 'view' && currentAdapter) {
    currentAdapter.highlightPath?.(startId, endId);
  }
};

// 添加组合（编辑模式）
const addCombo = (combo) => {
  if (props.mode === 'edit' && currentAdapter) {
    currentAdapter.addCombo?.(combo);
  }
};

// 自动布局
const autoLayout = () => {
  if (currentAdapter) {
    currentAdapter.autoLayout?.();
  }
};

onMounted(init);
onUnmounted(() => {
  graphEngine.destroy();
});

defineExpose({
  switchToView,
  switchToEdit,
  highlightNodes,
  highlightPath,
  addCombo,
  autoLayout
});
</script>