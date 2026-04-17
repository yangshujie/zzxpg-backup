<!-- src/components/GraphEditor/Toolbar.vue -->
<template>
  <div class="graph-toolbar">
    <div class="toolbar-left">
      <!-- 文件操作 -->
      <el-dropdown @command="handleFileCommand">
        <el-button size="small">
          文件 <i class="el-icon-arrow-down el-icon--right"></i>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="new">新建</el-dropdown-item>
            <el-dropdown-item command="open">打开</el-dropdown-item>
            <el-dropdown-item command="save" divided>保存</el-dropdown-item>
            <el-dropdown-item command="saveAs">另存为</el-dropdown-item>
            <el-dropdown-item command="export" divided>导出图片</el-dropdown-item>
            <el-dropdown-item command="import">导入数据</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      
      <!-- 编辑操作 -->
      <template v-if="isEditable">
        <el-divider direction="vertical"></el-divider>
        <el-button
          size="small"
          :disabled="!canUndo"
          icon="el-icon-refresh-left"
          @click="handleUndo"
        >
          撤销
        </el-button>
        <el-button
          size="small"
          :disabled="!canRedo"
          icon="el-icon-refresh-right"
          @click="handleRedo"
        >
          重做
        </el-button>
      </template>
      
      <!-- 视图操作 -->
      <el-divider direction="vertical"></el-divider>
      <el-button
        size="small"
        icon="el-icon-zoom-in"
        @click="handleZoomIn"
      >
        放大
      </el-button>
      <el-button
        size="small"
        icon="el-icon-zoom-out"
        @click="handleZoomOut"
      >
        缩小
      </el-button>
      <el-button
        size="small"
        icon="el-icon-full-screen"
        @click="handleFitView"
      >
        适应视图
      </el-button>
      
      <!-- 工具选择 -->
      <el-divider direction="vertical"></el-divider>
      <el-button-group>
        <el-button
          size="small"
          :type="currentTool === 'select' ? 'primary' : 'default'"
          icon="el-icon-mouse"
          @click="setTool('select')"
        >
          选择
        </el-button>
        <el-button
          v-if="isEditable"
          size="small"
          :type="currentTool === 'drag' ? 'primary' : 'default'"
          icon="el-icon-rank"
          @click="setTool('drag')"
        >
          拖拽
        </el-button>
        <el-button
          v-if="isEditable"
          size="small"
          :type="currentTool === 'connect' ? 'primary' : 'default'"
          icon="el-icon-share"
          @click="setTool('connect')"
        >
          连线
        </el-button>
      </el-button-group>
    </div>
    
    <div class="toolbar-right">
      <!-- 缩放比例显示 -->
      <span class="zoom-info">{{ Math.round(toolStates.zoom * 100) }}%</span>
      
      <!-- 保存状态 -->
      <el-tag v-if="isModified" size="small" type="warning">未保存</el-tag>
      <el-tag v-else size="small" type="success">已保存</el-tag>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useGraphStore } from '@/store/modules/graph';
import { ElMessage, ElMessageBox } from 'element-plus';

const props = defineProps({
  scene: {
    type: Object,
    required: true
  },
  tools: {
    type: Array,
    default: () => []
  },
  isEditable: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits(['tool-click']);

const graphStore = useGraphStore();

// 从store获取状态
const canUndo = computed(() => graphStore.canUndo);
const canRedo = computed(() => graphStore.canRedo);
const isModified = computed(() => graphStore.hasUnsavedChanges);
const currentTool = computed(() => graphStore.currentTool || 'select');
const toolStates = computed(() => graphStore.toolStates || { zoom: 1 });

// 文件操作
const handleFileCommand = async (command) => {
  switch (command) {
    case 'new':
      ElMessageBox.confirm('确定新建吗？未保存的数据将会丢失', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        graphStore.updateGraphData({ nodes: [], edges: [] });
        ElMessage.success('新建成功');
      });
      break;
      
    case 'open':
      // 打开文件
      break;
      
    case 'save':
      await graphStore.saveGraphData();
      ElMessage.success('保存成功');
      break;
      
    case 'saveAs':
      // 另存为
      break;
      
    case 'export':
      emit('tool-click', 'export');
      break;
      
    case 'import':
      // 导入数据
      break;
  }
};

// 编辑操作
const handleUndo = () => {
  graphStore.undo();
  emit('tool-click', 'undo');
};

const handleRedo = () => {
  graphStore.redo();
  emit('tool-click', 'redo');
};

// 视图操作
const handleZoomIn = () => {
  emit('tool-click', 'zoomIn');
};

const handleZoomOut = () => {
  emit('tool-click', 'zoomOut');
};

const handleFitView = () => {
  emit('tool-click', 'fitView');
};

// 设置工具
const setTool = (tool) => {
  graphStore.setCurrentTool(tool);
  emit('tool-click', tool);
};
</script>

<style scoped>
.graph-toolbar {
  position: absolute;
  top: 12px;
  left: 12px;
  right: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 4px;
  padding: 8px 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.zoom-info {
  font-size: 12px;
  color: #606266;
  min-width: 50px;
  text-align: center;
}

.el-divider--vertical {
  height: 24px;
  margin: 0 4px;
}
</style>