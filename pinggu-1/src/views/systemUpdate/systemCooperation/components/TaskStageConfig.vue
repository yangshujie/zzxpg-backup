<template>
  <el-dialog
    v-model="dialogVisible"
    title="任务阶段配置"
    width="1200px"
    :close-on-click-modal="false"
    @close="handleClose"
    append-to-body
  >
    <div class="task-stage-config">
      <!-- 左侧任务列表 -->
      <div class="task-list">
        <div class="task-list-header">
          <span>任务列表</span>
          <el-button type="primary" size="small" @click="addTask">新增任务</el-button>
        </div>
        <div class="task-list-content">
          <draggable
            v-model="taskList"
            group="tasks"
            :sort="true"
            item-key="id"
            class="task-items"
            ghost-class="task-item-ghost"
            drag-class="task-item-drag"
            :disabled="false"
          >
            <template #item="{ element, index }">
              <div
                class="task-item"
                :class="{ active: selectedTaskId === element.id }"
                @click="selectTask(element.id)"
              >
                <div class="task-item-content">
                  <el-icon class="drag-icon"><Rank /></el-icon>
                  <span class="task-name">{{ element.name }}</span>
                  <el-icon class="edit-icon" @click.stop="editTask(element)">
                    <Edit />
                  </el-icon>
                  <el-icon class="delete-icon" @click.stop="deleteTask(element.id)">
                    <Delete />
                  </el-icon>
                </div>
              </div>
            </template>
          </draggable>
        </div>
      </div>

      <!-- 右侧画布区域 -->
      <div class="canvas-area">
        <div class="canvas-header">
          <span>流程配置（拖拽节点调整顺序）</span>
          <el-button size="small" @click="resetLayout">重置布局</el-button>
        </div>
        <div class="canvas-container" ref="canvasContainer">
          <svg class="connection-lines" ref="svgCanvas">
            <!-- 连接线会动态绘制 -->
          </svg>
          <div class="nodes-container">
            <draggable
              v-model="stageList"
              group="stages"
              :sort="true"
              item-key="id"
              class="stage-nodes"
              ghost-class="stage-node-ghost"
              drag-class="stage-node-drag"
              @end="onDragEnd"
              :animation="300"
            >
              <template #item="{ element, index }">
                <StageNode
                  :stage="element"
                  :index="index"
                  :is-first="index === 0"
                  :is-last="index === stageList.length - 1"
                  @click="handleNodeClick(element)"
                />
              </template>
            </draggable>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSave">保存配置</el-button>
      </span>
    </template>

    <!-- 编辑任务对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="editMode === 'add' ? '新增任务' : '编辑任务'"
      width="400px"
      append-to-body
    >
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="任务名称">
          <el-input v-model="editForm.name" placeholder="请输入任务名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTask">确定</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Rank, Edit, Delete } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import StageNode from './StageNode.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  // 初始任务列表
  initialTasks: {
    type: Array,
    default: () => [
      { id: '1', name: '侦察阶段' },
      { id: '2', name: '指挥决策' },
      { id: '3', name: '火力打击' },
      { id: '4', name: '效果评估' }
    ]
  },
  // 初始阶段顺序
  initialStages: {
    type: Array,
    default: () => [
      { id: '1', name: '侦察阶段' },
      { id: '2', name: '指挥决策' },
      { id: '3', name: '火力打击' },
      { id: '4', name: '效果评估' }
    ]
  },
  // 外部传入的任务数据
  externalTaskData: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'save'])

// 对话框显示状态
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 数据
const taskList = ref([...props.initialTasks])
const stageList = ref([...props.initialStages])
const selectedTaskId = ref(null)
const editDialogVisible = ref(false)
const editMode = ref('add')
const editForm = ref({ id: null, name: '' })
const canvasContainer = ref(null)
const svgCanvas = ref(null)

// 监听外部数据变化
watch(() => props.externalTaskData, (newData) => {
  if (newData && newData.length > 0) {
    // 转换API数据格式为组件需要的格式
    const convertedTasks = newData.map(item => ({
      id: item.taskStage,
      name: item.taskName
    }))
    
    taskList.value = convertedTasks
    stageList.value = convertedTasks
    
    nextTick(() => {
      drawConnections()
    })
  }
}, { immediate: true })

// 选择任务
const selectTask = (taskId) => {
  selectedTaskId.value = taskId
  // 可以在这里添加高亮画布中对应节点的逻辑
  highlightNode(taskId)
}

// 高亮节点
const highlightNode = (taskId) => {
  // 通过事件通知子组件高亮
  // 这里可以通过ref或者事件总线实现
}

// 新增任务
const addTask = () => {
  editMode.value = 'add'
  editForm.value = { id: null, name: '' }
  editDialogVisible.value = true
}

// 编辑任务
const editTask = (task) => {
  editMode.value = 'edit'
  editForm.value = { id: task.id, name: task.name }
  editDialogVisible.value = true
}

// 保存任务
const saveTask = () => {
  if (!editForm.value.name.trim()) {
    ElMessage.warning('请输入任务名称')
    return
  }

  if (editMode.value === 'add') {
    const newId = Date.now().toString()
    taskList.value.push({
      id: newId,
      name: editForm.value.name
    })
    ElMessage.success('新增成功')
  } else {
    const index = taskList.value.findIndex(t => t.id === editForm.value.id)
    if (index !== -1) {
      taskList.value[index].name = editForm.value.name
      // 同时更新画布中对应节点的名称
      const stageIndex = stageList.value.findIndex(s => s.id === editForm.value.id)
      if (stageIndex !== -1) {
        stageList.value[stageIndex].name = editForm.value.name
      }
    }
    ElMessage.success('编辑成功')
  }
  editDialogVisible.value = false
}

// 删除任务
const deleteTask = async (taskId) => {
  try {
    await ElMessageBox.confirm('确定要删除该任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 从任务列表中删除
    taskList.value = taskList.value.filter(t => t.id !== taskId)
    
    // 从阶段列表中删除
    const index = stageList.value.findIndex(s => s.id === taskId)
    if (index !== -1) {
      stageList.value.splice(index, 1)
    }
    
    ElMessage.success('删除成功')
  } catch (error) {
    // 取消删除
  }
}

// 拖拽结束后的处理
const onDragEnd = () => {
  nextTick(() => {
    drawConnections()
  })
}

// 绘制连接线
const drawConnections = () => {
  if (!svgCanvas.value || !canvasContainer.value) return
  
  const svg = svgCanvas.value
  const container = canvasContainer.value
  const nodes = document.querySelectorAll('.stage-node')
  
  // 清空现有线条
  svg.innerHTML = ''
  
  if (nodes.length < 2) return
  
  // 获取容器位置
  const containerRect = container.getBoundingClientRect()
  
  for (let i = 0; i < nodes.length - 1; i++) {
    const fromNode = nodes[i]
    const toNode = nodes[i + 1]
    
    if (fromNode && toNode) {
      // 获取节点的位置
      const fromRect = fromNode.getBoundingClientRect()
      const toRect = toNode.getBoundingClientRect()
      
      // 计算连接点位置
      const fromPoint = {
        x: fromRect.right - containerRect.left,
        y: fromRect.top + fromRect.height / 2 - containerRect.top
      }
      
      const toPoint = {
        x: toRect.left - containerRect.left,
        y: toRect.top + toRect.height / 2 - containerRect.top
      }
      
      // 创建贝塞尔曲线
      const startX = fromPoint.x
      const startY = fromPoint.y
      const endX = toPoint.x
      const endY = toPoint.y
      
      // 控制点
      const cp1x = startX + (endX - startX) * 0.5
      const cp1y = startY
      const cp2x = endX - (endX - startX) * 0.5
      const cp2y = endY
      
      // 创建路径
      const path = document.createElementNS('http://www.w3.org/2000/svg', 'path')
      path.setAttribute('d', `M ${startX} ${startY} C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${endX} ${endY}`)
      path.setAttribute('stroke', '#409EFF')
      path.setAttribute('stroke-width', '2')
      path.setAttribute('fill', 'none')
      path.setAttribute('stroke-dasharray', '5,5')
      
      // 添加箭头
      const angle = Math.atan2(endY - startY, endX - startX)
      const arrowSize = 8
      const arrowX = endX - arrowSize * Math.cos(angle)
      const arrowY = endY - arrowSize * Math.sin(angle)
      
      const arrow = document.createElementNS('http://www.w3.org/2000/svg', 'polygon')
      const points = [
        [endX, endY],
        [arrowX - arrowSize * Math.sin(angle), arrowY + arrowSize * Math.cos(angle)],
        [arrowX + arrowSize * Math.sin(angle), arrowY - arrowSize * Math.cos(angle)]
      ]
      arrow.setAttribute('points', points.map(p => p.join(',')).join(' '))
      arrow.setAttribute('fill', '#409EFF')
      
      svg.appendChild(path)
      svg.appendChild(arrow)
    }
  }
}

// 节点点击处理
const handleNodeClick = (stage) => {
  selectedTaskId.value = stage.id
  // 滚动左侧列表到对应项
  const taskElement = document.querySelector(`.task-item[data-id="${stage.id}"]`)
  if (taskElement) {
    taskElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

// 重置布局
const resetLayout = () => {
  stageList.value = [...props.initialStages]
  nextTick(() => {
    drawConnections()
  })
}

// 保存配置
const handleSave = () => {
  const config = {
    taskList: stageList.value.map((stage, index) => ({
      taskName: stage.name,
      taskStage: stage.id,
    }))
  }
  emit('save', config)
  ElMessage.success('配置保存成功')
  handleClose()
}

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false
}

// 监听对话框打开
watch(dialogVisible, (newVal) => {
  if (newVal) {
    nextTick(() => {
      // 确保画布中有四个默认节点
      if (stageList.value.length === 0) {
        stageList.value = [...props.initialStages]
      }
      drawConnections()
      
      // 监听窗口大小变化
      window.addEventListener('resize', drawConnections)
    })
  } else {
    window.removeEventListener('resize', drawConnections)
  }
})

// 监听画布内容变化，重新绘制连接线
watch([stageList, canvasContainer], () => {
  nextTick(() => {
    drawConnections()
  })
})

defineExpose({
  resetLayout,
  drawConnections
})
</script>

<style lang="scss" scoped>
.task-stage-config {
  display: flex;
  height: 500px;
  gap: 20px;
}

/* 左侧任务列表样式 */
.task-list {
  width: 280px;
  border-right: 1px solid var(--el-border-color);
  display: flex;
  flex-direction: column;
  background-color: var(--el-bg-color-page);
  border-radius: 8px;
  overflow: hidden;
}

.task-list-header {
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  background-color: var(--el-bg-color);
}

.task-list-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.task-items {
  min-height: 100%;
}

.task-item {
  padding: 12px;
  margin-bottom: 8px;
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    border-color: var(--el-color-primary);
    transform: translateX(2px);
  }

  &.active {
    border-color: var(--el-color-primary);
    background-color: var(--el-color-primary-light-9);
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
  }
}

.task-item-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.drag-icon {
  cursor: move;
  color: var(--el-text-color-secondary);
  font-size: 16px;
}

.task-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
}

.edit-icon,
.delete-icon {
  cursor: pointer;
  font-size: 16px;
  color: var(--el-text-color-secondary);
  transition: color 0.3s;

  &:hover {
    color: var(--el-color-primary);
  }
}

.delete-icon:hover {
  color: var(--el-color-danger);
}

/* 右侧画布样式 */
.canvas-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--el-bg-color-page);
  border-radius: 8px;
  overflow: hidden;
}

.canvas-header {
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--el-bg-color);
}

.canvas-container {
  flex: 1;
  position: relative;
  overflow: auto;
  background: 
    linear-gradient(90deg, rgba(0, 0, 0, 0.02) 1px, transparent 1px),
    linear-gradient(0deg, rgba(0, 0, 0, 0.02) 1px, transparent 1px);
  background-size: 20px 20px;
  min-height: 400px;
}

.connection-lines {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.nodes-container {
  position: relative;
  width: 100%;
  height: 100%;
  z-index: 2;
}

.stage-nodes {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 40px;
  padding: 60px 20px;
}

/* 拖拽动画 */
.stage-node-ghost {
  opacity: 0.5;
  background: var(--el-color-primary-light-8);
  border: 2px dashed var(--el-color-primary);
}

.stage-node-drag {
  cursor: grabbing;
  opacity: 0.8;
  transform: scale(1.02);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

.task-item-ghost {
  opacity: 0.5;
  background: var(--el-color-primary-light-8);
}

.task-item-drag {
  cursor: grabbing;
}
</style>