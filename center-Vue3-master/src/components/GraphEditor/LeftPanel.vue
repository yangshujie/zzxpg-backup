<!-- src/components/GraphEditor/LeftPanel.vue -->
<template>
  <div class="left-panel">
    <el-tabs v-model="activeTab">
      <!-- 树形结构面板 -->
      <el-tab-pane label="节点树" name="tree">
        <div class="tree-panel">
          <el-tree
            :data="treeData"
            node-key="id"
            :default-expanded-keys="['0']"
            @node-click="handleTreeClick"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <i :class="data.icon"></i>
                <span>{{ node.label }}</span>
              </span>
            </template>
          </el-tree>
        </div>
      </el-tab-pane>
      
      <!-- 拖拽面板 -->
      <el-tab-pane label="节点库" name="drag">
        <div class="drag-panel">
          <div
            v-for="nodeType in nodeTypes"
            :key="nodeType.type"
            class="drag-node"
            draggable="true"
            @dragstart="handleDragStart($event, nodeType)"
          >
            <div class="node-preview" :style="nodeType.style">
              {{ nodeType.label }}
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  scene: {
    type: Object,
    required: true
  },
  nodeTypes: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['node-drag', 'node-select']);

const activeTab = ref('tree');

// 树形数据（根据场景动态生成）
const treeData = computed(() => {
  return [
    {
      id: '0',
      label: props.scene.name,
      icon: 'el-icon-folder-opened',
      children: props.nodeTypes.map(type => ({
        id: type,
        label: `${type}节点`,
        icon: 'el-icon-document',
        type: type
      }))
    }
  ];
});

// 节点类型配置
const nodeTypeConfig = {
  start: { label: '开始节点', style: { background: '#67C23A', color: '#fff' } },
  process: { label: '处理节点', style: { background: '#409EFF', color: '#fff' } },
  decision: { label: '判断节点', style: { background: '#E6A23C', color: '#fff' } },
  end: { label: '结束节点', style: { background: '#F56C6C', color: '#fff' } },
  entity: { label: '实体', style: { background: '#409EFF', color: '#fff' } },
  concept: { label: '概念', style: { background: '#67C23A', color: '#fff' } },
  class: { label: '类', style: { background: '#E6A23C', color: '#fff' } }
};

// 处理树节点点击
const handleTreeClick = (data) => {
  if (data.type) {
    emit('node-select', {
      type: data.type,
      label: data.label
    });
  }
};

// 处理拖拽开始
const handleDragStart = (event, nodeType) => {
  const config = nodeTypeConfig[nodeType.type] || {
    label: nodeType.label,
    style: { background: '#909399', color: '#fff' }
  };
  
  event.dataTransfer.setData('text/plain', JSON.stringify({
    type: nodeType.type,
    label: config.label
  }));
  
  emit('node-drag', nodeType.type, event);
};
</script>

<style scoped>
.left-panel {
  width: 280px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  padding: 16px;
}

.tree-panel {
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.drag-panel {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.drag-node {
  cursor: move;
  text-align: center;
  padding: 8px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  transition: all 0.3s;
}

.drag-node:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.node-preview {
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
}
</style>