<template>
  <div class="simple-mindmap" ref="containerRef">
    <div class="mindmap-canvas" ref="canvasRef">
      <!-- 连接线 -->
      <svg class="connectors-layer" :width="canvasWidth" :height="canvasHeight">
        <path 
          v-for="connector in connectors" 
          :key="connector.id"
          :d="connector.path" 
          :stroke="connector.color" 
          stroke-width="2" 
          fill="none"
          class="connector-line"
        />
      </svg>
      
      <!-- 节点 -->
      <div 
        v-for="node in nodes" 
        :key="node.id"
        :class="['mindmap-node', { selected: selectedNode === node, dragging: node.dragging }]"
        :style="{
          left: node.x + 'px',
          top: node.y + 'px',
          width: node.width + 'px',
          height: node.height + 'px'
        }"
        @mousedown="startDrag(node, $event)"
        @click="selectNode(node)"
        @dblclick="editNode(node)"
      >
        <div class="node-content">
          <div class="node-header">
            <div class="node-text">{{ node.text }}</div>
            <div class="node-actions">
              <button class="action-btn" @click.stop="deleteNode(node)" title="删除">
                <i class="fas fa-trash"></i>
              </button>
              <button class="action-btn" @click.stop="configureNode(node)" title="配置">
                <i class="fas fa-cog"></i>
              </button>
            </div>
          </div>
          <div v-if="node.description" class="node-description">{{ node.description }}</div>
          <div v-if="node.dataSource" class="node-info">
            <span class="info-tag"><i class="fas fa-database"></i> {{ node.dataSource }}</span>
          </div>
          <div v-if="node.algorithm" class="node-info">
            <span class="info-tag"><i class="fas fa-calculator"></i> {{ node.algorithm }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 节点配置模态框 -->
    <div v-if="configuringNode" class="node-config-modal">
      <div class="modal-overlay" @click="configuringNode = null">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <h4>节点配置 - {{ configuringNode.text }}</h4>
            <button class="modal-close" @click="configuringNode = null">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="modal-body">
            <div class="config-tabs">
              <div class="tab-header">
                <button :class="{ active: activeTab === 'basic' }" @click="activeTab = 'basic'">基本信息</button>
                <button :class="{ active: activeTab === 'data' }" @click="activeTab = 'data'">数据源</button>
                <button :class="{ active: activeTab === 'algorithm' }" @click="activeTab = 'algorithm'">算法</button>
              </div>
              
              <div class="tab-content">
                <!-- 基本信息 -->
                <div v-if="activeTab === 'basic'" class="config-section">
                  <div class="form-group">
                    <label>节点名称</label>
                    <input type="text" v-model="configuringNode.text" />
                  </div>
                  <div class="form-group">
                    <label>节点描述</label>
                    <textarea v-model="configuringNode.description"></textarea>
                  </div>
                </div>
                
                <!-- 数据源配置 -->
                <div v-if="activeTab === 'data'" class="config-section">
                  <div class="form-group">
                    <label>数据源类型</label>
                    <select v-model="configuringNode.dataSource">
                      <option value="">请选择数据源</option>
                      <option value="船只考核履历">船只考核履历</option>
                      <option value="实时传感器数据">实时传感器数据</option>
                      <option value="历史评估数据">历史评估数据</option>
                      <option value="外部API接口">外部API接口</option>
                      <option value="本地数据库">本地数据库</option>
                    </select>
                  </div>
                  <div class="form-group" v-if="configuringNode.dataSource">
                    <label>数据表/接口</label>
                    <input type="text" v-model="configuringNode.dataTable" placeholder="请输入数据表名或接口地址" />
                  </div>
                </div>
                
                <!-- 算法配置 -->
                <div v-if="activeTab === 'algorithm'" class="config-section">
                  <div class="form-group">
                    <label>计算算法</label>
                    <select v-model="configuringNode.algorithm">
                      <option value="">请选择算法</option>
                      <option value="加权平均法">加权平均法</option>
                      <option value="模糊综合评价">模糊综合评价</option>
                      <option value="神经网络算法">神经网络算法</option>
                      <option value="决策树算法">决策树算法</option>
                      <option value="自定义脚本">自定义脚本</option>
                    </select>
                  </div>
                  <div class="form-group" v-if="configuringNode.algorithm === '加权平均法'">
                    <label>权重设置</label>
                    <input type="number" v-model="configuringNode.weight" min="0" max="100" step="1" />
                    <span>%</span>
                  </div>
                  <div class="form-group" v-if="configuringNode.algorithm === '自定义脚本'">
                    <label>脚本代码</label>
                    <textarea v-model="configuringNode.customScript" placeholder="请输入自定义脚本代码"></textarea>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="modal-actions">
            <button class="btn-primary" @click="saveNodeConfig">保存配置</button>
            <button class="btn-outline" @click="configuringNode = null">取消</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({ text: '根节点', children: [] })
  },
  editable: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['node-click', 'node-update', 'node-add', 'node-delete'])

// Refs
const containerRef = ref(null)
const canvasRef = ref(null)
const canvasWidth = ref(1200)
const canvasHeight = ref(800)
const selectedNode = ref(null)
const configuringNode = ref(null)
const activeTab = ref('basic')

// 拖拽状态
const isDragging = ref(false)
const dragStartX = ref(0)
const dragStartY = ref(0)

// 节点数据
const nodes = ref([])

// 生成唯一ID
function generateId() {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

// 树形布局算法
function calculateLayout() {
  const levelSpacing = 200
  const siblingSpacing = 150
  
  // 简单直接的布局算法
  const rootNodes = nodes.value.filter(node => node.level === 0)
  
  // 第一层节点（根节点）
  const rootY = 100
  const rootTotalWidth = rootNodes.length * 120 + (rootNodes.length - 1) * siblingSpacing
  const rootStartX = (canvasWidth.value - rootTotalWidth) / 2
  
  rootNodes.forEach((node, index) => {
    node.x = rootStartX + index * (120 + siblingSpacing)
    node.y = rootY
  })
  
  // 第二层节点（子节点）
  const childNodes = nodes.value.filter(node => node.level === 1)
  const childY = rootY + levelSpacing
  
  if (childNodes.length > 0) {
    const childTotalWidth = childNodes.length * 120 + (childNodes.length - 1) * siblingSpacing
    const childStartX = (canvasWidth.value - childTotalWidth) / 2
    
    childNodes.forEach((node, index) => {
      node.x = childStartX + index * (120 + siblingSpacing)
      node.y = childY
    })
  }
  
  // 第三层节点（孙子节点）
  const grandChildNodes = nodes.value.filter(node => node.level === 2)
  const grandChildY = childY + levelSpacing
  
  if (grandChildNodes.length > 0) {
    const grandChildTotalWidth = grandChildNodes.length * 120 + (grandChildNodes.length - 1) * siblingSpacing
    const grandChildStartX = (canvasWidth.value - grandChildTotalWidth) / 2
    
    grandChildNodes.forEach((node, index) => {
      node.x = grandChildStartX + index * (120 + siblingSpacing)
      node.y = grandChildY
    })
  }
  
  console.log('布局计算完成')
  console.log('根节点数量:', rootNodes.length)
  console.log('子节点数量:', childNodes.length)
  console.log('孙子节点数量:', grandChildNodes.length)
  
  // 强制更新视图
  nodes.value = [...nodes.value]
}

// 初始化节点数据
function initNodes(data, parentId = null, level = 0) {
  const node = {
    id: data.id || generateId(),
    text: data.text || '节点',
    description: data.description || '',
    x: 0,
    y: 0,
    width: 120,
    height: 80,
    level: level,
    parentId: parentId,
    childrenIds: [],
    dragging: false,
    dataSource: data.dataSource || '',
    algorithm: data.algorithm || '',
    dataTable: data.dataTable || '',
    weight: data.weight || 0,
    customScript: data.customScript || ''
  }
  
  nodes.value.push(node)
  console.log(`创建节点: ${node.text}, 层级: ${level}, 父节点: ${parentId}`)
  
  if (data.children && data.children.length > 0) {
    console.log(`节点 ${node.text} 有 ${data.children.length} 个子节点`)
    data.children.forEach(childData => {
      const childNode = initNodes(childData, node.id, level + 1)
      node.childrenIds.push(childNode.id)
    })
  }
  
  return node
}

// 计算连接线
const connectors = computed(() => {
  const connectors = []
  
  nodes.value.forEach(node => {
    if (node.childrenIds && node.childrenIds.length > 0) {
      node.childrenIds.forEach(childId => {
        const childNode = nodes.value.find(n => n.id === childId)
        if (childNode) {
          const parentX = node.x + node.width / 2
          const parentY = node.y + node.height
          const childX = childNode.x + childNode.width / 2
          const childY = childNode.y
          
          // 贝塞尔曲线
          const path = `M ${parentX} ${parentY} C ${parentX} ${parentY + 50}, ${childX} ${childY - 50}, ${childX} ${childY}`
          
          connectors.push({
            id: `${node.id}-${childId}`,
            path: path,
            color: '#3b82f6'
          })
        }
      })
    }
  })
  
  return connectors
})

// 拖拽功能
function startDrag(node, event) {
  if (!props.editable) return
  
  isDragging.value = true
  node.dragging = true
  dragStartX.value = event.clientX
  dragStartY.value = event.clientY
  
  document.addEventListener('mousemove', handleDrag)
  document.addEventListener('mouseup', stopDrag)
  event.preventDefault()
}

function handleDrag(event) {
  if (!isDragging.value) return
  
  const dx = event.clientX - dragStartX.value
  const dy = event.clientY - dragStartY.value
  
  const draggedNode = nodes.value.find(n => n.dragging)
  if (draggedNode) {
    draggedNode.x += dx
    draggedNode.y += dy
    
    dragStartX.value = event.clientX
    dragStartY.value = event.clientY
  }
}

function stopDrag() {
  if (!isDragging.value) return
  
  isDragging.value = false
  const draggedNode = nodes.value.find(n => n.dragging)
  if (draggedNode) {
    draggedNode.dragging = false
  }
  
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', stopDrag)
}

// 节点操作
function selectNode(node) {
  selectedNode.value = node
  emit('node-click', node)
}

function editNode(node) {
  if (props.editable) {
    const newText = prompt('编辑节点文本:', node.text)
    if (newText !== null) {
      node.text = newText
      emit('node-update', node)
    }
  }
}

function deleteNode(node) {
  if (props.editable) {
    if (confirm(`确定要删除节点"${node.text}"吗？`)) {
      // 删除子节点
      if (node.childrenIds && node.childrenIds.length > 0) {
        node.childrenIds.forEach(childId => {
          const childNode = nodes.value.find(n => n.id === childId)
          if (childNode) deleteNode(childNode)
        })
      }
      
      // 从父节点中移除
      if (node.parentId) {
        const parentNode = nodes.value.find(n => n.id === node.parentId)
        if (parentNode) {
          parentNode.childrenIds = parentNode.childrenIds.filter(id => id !== node.id)
        }
      }
      
      nodes.value = nodes.value.filter(n => n.id !== node.id)
      emit('node-delete', node)
    }
  }
}

function configureNode(node) {
  if (props.editable) {
    configuringNode.value = { ...node }
    activeTab.value = 'basic'
  }
}

function saveNodeConfig() {
  if (configuringNode.value) {
    const originalNode = nodes.value.find(n => n.id === configuringNode.value.id)
    if (originalNode) {
      Object.assign(originalNode, configuringNode.value)
      emit('node-update', originalNode)
    }
    configuringNode.value = null
  }
}

// 添加节点方法
defineExpose({
  addRootNode: (nodeData = {}) => {
    const newNode = {
      id: generateId(),
      text: nodeData.text || '新节点',
      description: nodeData.description || '',
      x: 0,
      y: 0,
      width: 120,
      height: 80,
      level: 0,
      parentId: null,
      childrenIds: [],
      dataSource: nodeData.dataSource || '',
      algorithm: nodeData.algorithm || ''
    }
    
    nodes.value.push(newNode)
    calculateLayout() // 重新计算布局
    emit('node-add', newNode)
    return newNode
  },
  
  addChildNode: (nodeData = {}) => {
    if (!selectedNode.value) {
      alert('请先选择一个节点作为父节点')
      return null
    }
    
    const newNode = {
      id: generateId(),
      text: nodeData.text || '子节点',
      description: nodeData.description || '',
      x: 0,
      y: 0,
      width: 120,
      height: 80,
      level: selectedNode.value.level + 1,
      parentId: selectedNode.value.id,
      childrenIds: [],
      dataSource: nodeData.dataSource || '',
      algorithm: nodeData.algorithm || ''
    }
    
    selectedNode.value.childrenIds.push(newNode.id)
    nodes.value.push(newNode)
    calculateLayout() // 重新计算布局
    emit('node-add', newNode)
    return newNode
  },
  
  deleteSelectedNode: () => {
    if (selectedNode.value) {
      deleteNode(selectedNode.value)
      calculateLayout() // 重新计算布局
    }
  },
  
  recalculateLayout: () => {
    calculateLayout()
  }
})

// 初始化
onMounted(() => {
  if (props.data) {
    initNodes(props.data)
    
    // 强制设置canvas尺寸并立即计算布局
    nextTick(() => {
      if (canvasRef.value) {
        // 强制设置较大的canvas尺寸
        canvasWidth.value = 2000
        canvasHeight.value = 1200
        
        console.log('Canvas尺寸:', canvasWidth.value, canvasHeight.value)
        console.log('节点数量:', nodes.value.length)
        
        // 立即计算布局
        calculateLayout()
      }
    })
  }
})

// 监听数据变化
watch(() => props.data, (newData) => {
  if (newData) {
    nodes.value = []
    initNodes(newData)
    nextTick(() => {
      calculateLayout()
    })
  }
}, { deep: true })
</script>

<style scoped>
.simple-mindmap {
  width: 100%;
  height: 100%;
  background: #f8fafc;
  overflow: auto;
}

.mindmap-canvas {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 600px;
  min-width: 800px;
}

.connectors-layer {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
  z-index: 1;
}

.connector-line {
  stroke: #3b82f6;
  stroke-width: 2;
  fill: none;
}

.mindmap-node {
  position: absolute;
  background: white;
  border: 2px solid #3b82f6;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
  z-index: 2;
  min-width: 120px;
  min-height: 80px;
}

.mindmap-node:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.mindmap-node.selected {
  border-color: #ef4444;
  background: #fef2f2;
  box-shadow: 0 4px 16px rgba(239, 68, 68, 0.3);
}

.mindmap-node.dragging {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  transform: scale(1.05);
  z-index: 1000;
}

.node-content {
  text-align: center;
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.node-text {
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 0;
  flex: 1;
}

.node-actions {
  display: flex;
  gap: 4px;
}

.action-btn {
  background: none;
  border: none;
  padding: 4px 6px;
  border-radius: 4px;
  cursor: pointer;
  color: #6b7280;
  font-size: 12px;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.node-description {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.3;
  margin-bottom: 6px;
}

.node-info {
  margin-top: 4px;
}

.info-tag {
  display: inline-block;
  background: #f3f4f6;
  color: #374151;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  margin-right: 4px;
}

.info-tag i {
  margin-right: 2px;
}

/* 模态框样式 */
.node-config-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
}

.modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 500px;
  max-width: 90vw;
  max-height: 80vh;
  overflow: auto;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 20px 0;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 20px;
}

.modal-close {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #6b7280;
}

.modal-body {
  padding: 20px;
}

.config-tabs {
  margin-top: 0;
}

.tab-header {
  display: flex;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 16px;
}

.tab-header button {
  background: none;
  border: none;
  padding: 8px 16px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.tab-header button.active {
  border-bottom-color: #3b82f6;
  color: #3b82f6;
}

.config-section {
  margin-bottom: 0;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 4px;
  font-weight: 500;
  color: #374151;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group textarea {
  height: 80px;
  resize: vertical;
}

.modal-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding: 20px;
  border-top: 1px solid #e5e7eb;
}

.btn-primary,
.btn-outline {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover {
  background: #2563eb;
}

.btn-outline {
  background: #f3f4f6;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-outline:hover {
  background: #e5e7eb;
}
</style>