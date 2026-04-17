<!-- src/components/GraphEditor/PropertyPanel.vue -->
<template>
  <div class="property-panel">
    <div class="panel-header">
      <h3>属性面板</h3>
      <el-button
        v-if="selectedNode"
        size="small"
        type="danger"
        icon="el-icon-delete"
        @click="handleDelete"
      >
        删除
      </el-button>
    </div>
    
    <div v-if="selectedNode" class="panel-content">
      <el-form :model="formData" label-width="80px" size="small">
        <el-form-item label="节点ID">
          <el-input v-model="formData.id" disabled />
        </el-form-item>
        
        <el-form-item label="节点类型">
          <el-tag size="small">{{ formData.type }}</el-tag>
        </el-form-item>
        
        <el-form-item label="标签">
          <el-input v-model="formData.label" @change="handleUpdate" />
        </el-form-item>
        
        <el-form-item label="位置X">
          <el-input-number
            v-model="formData.x"
            :step="10"
            @change="handleUpdate"
          />
        </el-form-item>
        
        <el-form-item label="位置Y">
          <el-input-number
            v-model="formData.y"
            :step="10"
            @change="handleUpdate"
          />
        </el-form-item>
        
        <!-- 动态属性 -->
        <template v-if="customProperties.length">
          <el-divider>自定义属性</el-divider>
          <el-form-item
            v-for="prop in customProperties"
            :key="prop.key"
            :label="prop.label"
          >
            <el-input
              v-model="formData[prop.key]"
              @change="handleUpdate"
            />
          </el-form-item>
        </template>
      </el-form>
    </div>
    
    <div v-else class="panel-empty">
      <el-empty description="请选择一个节点" :image-size="80" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue';

const props = defineProps({
  selectedNode: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['update', 'delete']);

const formData = ref({});

// 自定义属性配置
const customProperties = computed(() => {
  if (!formData.value.type) return [];
  
  // 根据节点类型返回不同的自定义属性
  const propertyMap = {
    process: [
      { key: 'duration', label: '处理时长(ms)' },
      { key: 'handler', label: '处理器' }
    ],
    decision: [
      { key: 'condition', label: '判断条件' },
      { key: 'trueBranch', label: '真分支' },
      { key: 'falseBranch', label: '假分支' }
    ],
    entity: [
      { key: 'description', label: '描述' },
      { key: 'properties', label: '属性列表' }
    ]
  };
  
  return propertyMap[formData.value.type] || [];
});

// 监听选中节点变化
watch(() => props.selectedNode, (newNode) => {
  if (newNode) {
    formData.value = {
      id: newNode.id,
      type: newNode.type,
      label: newNode.label,
      x: newNode.x,
      y: newNode.y,
      ...newNode.data
    };
  } else {
    formData.value = {};
  }
}, { immediate: true, deep: true });

// 更新节点
const handleUpdate = () => {
  emit('update', {
    id: formData.value.id,
    label: formData.value.label,
    position: { x: formData.value.x, y: formData.value.y },
    data: customProperties.value.reduce((acc, prop) => {
      acc[prop.key] = formData.value[prop.key];
      return acc;
    }, {})
  });
};

// 删除节点
const handleDelete = () => {
  emit('delete', formData.value.id);
};
</script>

<style scoped>
.property-panel {
  width: 320px;
  background: #fff;
  border-left: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.panel-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.panel-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.panel-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>