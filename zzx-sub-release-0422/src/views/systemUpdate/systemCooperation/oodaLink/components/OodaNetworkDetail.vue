<template>
  <el-dialog :title="title" :model-value="open" width="1200px" append-to-body @close="closeDetail" @update:model-value="$emit('update:open', $event)">
    <div class="ooda-detail-container">
      <!-- 左中右布局 -->
      <div class="detail-layout">
        <!-- 左侧：树结构 -->
        <div class="detail-left">
          <div class="tree-title">任务网结构</div>
          <el-tree
            :data="detailTreeData"
            :props="detailTreeProps"
            node-key="id"
            default-expand-all
            :expand-on-click-node="false"
            class="detail-tree"
          >
            <template #default="{ node, data }">
              <span class="custom-tree-node">
                <span>{{ node.label }}</span>
                <span v-if="data.isRoot" class="tree-actions">
                  <el-button link type="primary" size="small" @click="handleEditRoot">修改</el-button>
                </span>
              </span>
            </template>
          </el-tree>
        </div>

        <!-- 中间：OODA网络画布 -->
        <div class="detail-center">
          <div class="canvas-title">OODA网络拓扑</div>
          <div class="ooda-canvas-container">
            <div ref="oodaCanvas" class="ooda-canvas"></div>
          </div>
        </div>

        <!-- 右侧：节点参数信息画布 -->
        <div class="detail-right">
          <div class="canvas-title">节点参数信息</div>
          <div class="param-canvas-container">
            <div ref="paramCanvas" class="param-canvas"></div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 修改根节点弹窗 -->
    <el-dialog v-model="editRootDialogVisible" title="修改任务网结构" width="500px" append-to-body>
      <el-form :model="editRootForm" label-width="80px">
        <el-form-item label="根节点名称">
          <el-input v-model="editRootForm.name" placeholder="请输入根节点名称" />
        </el-form-item>
        <el-form-item label="选择任务">
          <el-select 
            v-model="editRootForm.selectedTasks" 
            multiple 
            placeholder="请选择OODA链路" 
            style="width: 100%"
            @change="handleTaskSelectionChange"
          >
            <el-option
              v-for="task in taskList"
              :key="task.id"
              :label="task.name"
              :value="task.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editRootDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditRootConfirm">确定</el-button>
      </template>
    </el-dialog>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="handleSubmit" v-if="!isViewOnly">确 定</el-button>
        <el-button @click="closeDetail">{{ isViewOnly ? '关 闭' : '取 消' }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, nextTick, watch } from 'vue'
import { Graph, Shape } from '@antv/x6'
import { listOodaPath, getOodaPath } from '@/api/systemPlus/systemCooperation/ooda'

const props = defineProps({
  open: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  formData: {
    type: Object,
    default: () => ({})
  },
  isViewOnly: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:open', 'submit', 'cancel'])

// 详情弹窗相关变量
const detailTreeData = ref([])
const detailTreeProps = {
  children: 'children',
  label: 'label'
}
const oodaCanvas = ref(null)
const oodaGraph = ref(null)
const paramCanvas = ref(null)
const paramGraph = ref(null)

// 任务列表和编辑相关
const taskList = ref([])
const editRootDialogVisible = ref(false)
const editRootForm = reactive({
  name: '默认根节点',
  selectedTasks: []
})

// OODA网络数据结构
const networkData = reactive({
  name: '',
  description: '',
  pgTaskId: '',
  networkStructure: [],
  graphJSONOODANetwork: ''
})

/** 加载任务列表 */
async function loadTaskList() {
  try {
    const response = await listOodaPath({ pageNum: 1, pageSize: 100 })
    taskList.value = response.rows || []
  } catch (error) {
    console.error('加载任务列表失败:', error)
    taskList.value = []
  }
}

/** 初始化详情树结构数据 */
function initDetailTreeData() {
  console.log('初始化树结构数据，formData:', props.formData)
  
  // 如果有编辑数据，从networkStructure中加载子节点
  let children = []
  
  if (props.formData.networkStructure && props.formData.networkStructure.length > 0) {
    console.log('从networkStructure加载子节点:', props.formData.networkStructure)
    
    // 从任务列表中获取对应的任务名称
    children = props.formData.networkStructure.map(item => {
      const oodaPathId = item.oodaPathId || item.OODAPathId // 兼容大小写
      const task = taskList.value.find(t => t.id === oodaPathId)
      return {
        id: `task_${oodaPathId}`,
        label: task ? task.name : oodaPathId,
        taskId: oodaPathId
      }
    })
    
    console.log('构建的子节点:', children)
  }
  
  detailTreeData.value = [
    {
      id: 'root',
      label: props.formData.name || editRootForm.name,
      isRoot: true,
      children: children
    }
  ]
  
  console.log('最终树结构数据:', detailTreeData.value)
}

/** 初始化OODA网络画布 */
function initOodaGraph() {
  if (oodaGraph.value) {
    oodaGraph.value.dispose()
  }
  
  oodaGraph.value = new Graph({
    container: oodaCanvas.value,
    width: '100%',
    height: '100%',
    grid: true,
    mousewheel: {
      enabled: true,
      zoomAtMousePosition: true,
      modifiers: 'ctrl',
      minScale: 0.5,
      maxScale: 3,
    },
    connecting: {
      router: 'manhattan',
      connector: 'rounded',
      anchor: 'center',
      connectionPoint: 'anchor',
      allowBlank: false,
      allowMulti: true,  // 允许多重连线
      allowLoop: true,   // 允许自环
      allowEdge: false,  // 不允许边连接到边
      highlight: true,   // 高亮可连接的连接桩
      snap: { radius: 20 },
      validateMagnet: () => true,
      createEdge() {
        return new Shape.Edge({
          attrs: {
            line: {
              stroke: '#5F95FF',
              strokeWidth: 2,
              targetMarker: { name: 'block', size: 8 }
            }
          }
        })
      }
    }
  })
  
  // 启用节点拖拽
  oodaGraph.value.enableRubberband()
  
  // 启用键盘事件
  oodaGraph.value.enableKeyboard()
  
  // 添加节点点击事件
  oodaGraph.value.on('node:click', ({ node }) => {
    const nodeData = node.store.data
    if (nodeData.taskId) {
      loadTaskDetail(nodeData.taskId)
    }
  })
  
  // 监听连线开始事件，确保连接桩状态正确
  oodaGraph.value.on('edge:connected', ({ edge, isNew }) => {
    console.log('连线创建成功:', edge, '是否新连线:', isNew)
    
    // 可选：连线完成后自动刷新画布，确保连接桩状态重置
    // oodaGraph.value.refresh()
  })
  
  // 监听连线取消事件
  oodaGraph.value.on('edge:connected:cancel', () => {
    console.log('连线创建取消')
  })
  
  // 添加连线时的鼠标样式变化
  oodaGraph.value.on('edge:mouseenter', ({ edge }) => {
    edge.addTools([
      {
        name: 'vertices',
        args: { snapRadius: 20 }
      },
      {
        name: 'segments',
        args: { snapRadius: 20 }
      }
    ])
  })
  
  oodaGraph.value.on('edge:mouseleave', ({ edge }) => {
    edge.removeTools()
  })
  
  // 监听节点添加事件，确保连接桩始终可用
  oodaGraph.value.on('node:added', ({ node }) => {
    setTimeout(() => {
      node.updatePortsLayout()
      const ports = node.getPorts()
      ports.forEach(port => {
        node.setPortProp(port.id, 'attrs/circle/magnet', true)
      })
    }, 50)
  })
  
  // 如果有保存的画布数据，加载它
  if (networkData.graphJSONOODANetwork) {
    try {
      const graphData = JSON.parse(networkData.graphJSONOODANetwork)
      oodaGraph.value.fromJSON(graphData)
    } catch (error) {
      console.error('解析graphJSON失败:', error)
    }
  }
}

/** 加载任务详情到右侧画布 */
async function loadTaskDetail(taskId) {
  try {
    console.log('开始加载任务详情，任务ID:', taskId)
    const response = await getOodaPath(taskId)
    const taskData = response.data
    console.log('获取到的任务数据:', taskData)
    
    if (taskData.graphJSON) {
      console.log('开始初始化右侧画布，graphJSON:', taskData.graphJSON)
      initParamGraph(taskData.graphJSON)
    } else {
      console.log('任务数据中没有graphJSON，显示默认内容')
      initParamGraph() // 显示默认内容
    }
  } catch (error) {
    console.error('加载任务详情失败:', error)
    // 出错时也显示默认内容
    initParamGraph()
  }
}

/** 在中间画布添加任务节点 - 添加防重复检查 */
function addTaskToCanvas(task) {
  if (!oodaGraph.value) return null
  
  // 检查是否已存在相同任务ID的节点
  const existingNodes = oodaGraph.value.getNodes()
  const existingTaskNode = existingNodes.find(node => node.store.data?.taskId === task.id)
  
  if (existingTaskNode) {
    console.log('任务节点已存在，跳过添加:', task.name)
    return existingTaskNode
  }
  
  // 计算新节点的位置
  const nodeCount = existingNodes.length
  const x = 100 + (nodeCount % 3) * 200
  const y = 100 + Math.floor(nodeCount / 3) * 150
  
  const node = oodaGraph.value.addNode({
    shape: 'rect',
    x: x,
    y: y,
    width: 120,
    height: 60,
    attrs: {
      body: {
        fill: '#409EFF',
        stroke: '#337ecc',
        rx: 8,
        ry: 8
      },
      label: {
        text: task.name,
        fill: '#fff',
        fontSize: 12,
        fontWeight: 'bold'
      }
    },
    // 修改连接桩配置，确保可以多次连线
    ports: {
      groups: {
        top: {
          position: 'top',
          attrs: {
            circle: {
              r: 6,
              magnet: true,  // 关键：确保 magnet 属性为 true
              stroke: '#5F95FF',
              strokeWidth: 2,
              fill: '#fff',
              cursor: 'crosshair'  // 添加光标样式提示
            }
          },
          label: {
            position: 'top'
          }
        },
        right: {
          position: 'right',
          attrs: {
            circle: {
              r: 6,
              magnet: true,
              stroke: '#5F95FF',
              strokeWidth: 2,
              fill: '#fff',
              cursor: 'crosshair'
            }
          }
        },
        bottom: {
          position: 'bottom',
          attrs: {
            circle: {
              r: 6,
              magnet: true,
              stroke: '#5F95FF',
              strokeWidth: 2,
              fill: '#fff',
              cursor: 'crosshair'
            }
          }
        },
        left: {
          position: 'left',
          attrs: {
            circle: {
              r: 6,
              magnet: true,
              stroke: '#5F95FF',
              strokeWidth: 2,
              fill: '#fff',
              cursor: 'crosshair'
            }
          }
        }
      },
      items: [
        { 
          id: 'top', 
          group: 'top',
          // 确保每个连接桩都可以独立触发连线
          attrs: {
            circle: {
              magnet: true
            }
          }
        },
        { 
          id: 'right', 
          group: 'right',
          attrs: {
            circle: {
              magnet: true
            }
          }
        },
        { 
          id: 'bottom', 
          group: 'bottom',
          attrs: {
            circle: {
              magnet: true
            }
          }
        },
        { 
          id: 'left', 
          group: 'left',
          attrs: {
            circle: {
              magnet: true
            }
          }
        }
      ]
    },
    taskId: task.id,
    taskName: task.name
  })
  
  console.log('成功添加任务节点:', task.name, '节点ID:', node.id)
  return node
}

/** 初始化参数信息画布 */
function initParamGraph(graphJSON = null) {
  // 确保画布容器存在
  if (!paramCanvas.value) {
    console.error('参数画布容器不存在')
    return
  }
  
  // 如果画布已存在，先清理
  if (paramGraph.value) {
    paramGraph.value.dispose()
  }
  
  // 创建新的画布实例
  paramGraph.value = new Graph({
    container: paramCanvas.value,
    width: '100%',
    height: '100%',
    grid: false,
    background: {
      color: 'rgba(64, 158, 255, 0.1)' // 透明蓝色背景
    }
  })
  
  console.log('参数画布初始化完成，graphJSON:', graphJSON)
  
  // 如果有graphJSON数据，加载它
  if (graphJSON) {
    try {
      console.log('开始解析graphJSON数据')
      const graphData = JSON.parse(graphJSON)
      console.log('解析成功，开始加载到画布')
      paramGraph.value.fromJSON(graphData)
      console.log('graphJSON数据加载到画布成功')
    } catch (error) {
      console.error('解析参数画布graphJSON失败:', error)
      console.log('解析失败，显示默认内容')
      // 如果解析失败，显示默认内容
      createDefaultParamNodes()
    }
  } else {
    console.log('没有graphJSON数据，显示默认内容')
    // 显示默认内容
    createDefaultParamNodes()
  }
}

/** 创建默认参数信息节点 */
function createDefaultParamNodes() {
  // 创建默认提示节点
  const defaultNode = paramGraph.value.addNode({
    shape: 'rect',
    x: 50,
    y: 30,
    width: 200,
    height: 40,
    attrs: {
      body: {
        fill: '#909399',
        stroke: '#909399',
        rx: 6,
        ry: 6
      },
      label: {
        text: '请选择左侧任务节点查看详情',
        fill: '#fff',
        fontSize: 12
      }
    }
  })
}

/** 处理修改根节点 - 修改为自动勾选已有节点 */
function handleEditRoot() {
  // 设置根节点名称
  editRootForm.name = detailTreeData.value[0]?.label || '默认根节点'
  
  // 清空当前选中的任务列表
  editRootForm.selectedTasks = []
  
  // 获取当前画布上已有的任务节点
  if (oodaGraph.value) {
    const existingNodes = oodaGraph.value.getNodes()
    console.log('当前画布上的节点:', existingNodes)
    
    // 提取已存在的任务ID
    const existingTaskIds = existingNodes
      .map(node => node.store.data?.taskId)
      .filter(taskId => taskId !== undefined && taskId !== null)
    
    console.log('已存在的任务ID:', existingTaskIds)
    
    // 设置选中状态
    editRootForm.selectedTasks = existingTaskIds
  }
  
  // 如果画布上没有节点，但从树结构获取（作为备选）
  if (editRootForm.selectedTasks.length === 0 && detailTreeData.value[0]?.children) {
    const treeTaskIds = detailTreeData.value[0].children
      .map(child => child.taskId)
      .filter(taskId => taskId !== undefined && taskId !== null)
    
    editRootForm.selectedTasks = treeTaskIds
  }
  
  console.log('弹窗打开时选中的任务:', editRootForm.selectedTasks)
  editRootDialogVisible.value = true
}

/** 处理任务选择变化 - 实时同步画布显示 */
function handleTaskSelectionChange(selectedTaskIds) {
  console.log('任务选择变化，选中的任务ID:', selectedTaskIds)
  
  if (!oodaGraph.value) return
  
  // 获取当前画布上的节点
  const existingNodes = oodaGraph.value.getNodes()
  const existingTaskIds = existingNodes
    .map(node => node.store.data?.taskId)
    .filter(taskId => taskId !== undefined && taskId !== null)
  
  // 找出新添加的任务
  const addedTaskIds = selectedTaskIds.filter(id => !existingTaskIds.includes(id))
  
  // 找出需要删除的任务
  const removedTaskIds = existingTaskIds.filter(id => !selectedTaskIds.includes(id))
  
  // 添加新任务节点
  addedTaskIds.forEach(taskId => {
    const task = taskList.value.find(t => t.id === taskId)
    if (task) {
      console.log('添加任务节点:', task.name)
      addTaskToCanvas(task)
    }
  })
  
  // 删除移除的任务节点
  removedTaskIds.forEach(taskId => {
    const nodeToRemove = existingNodes.find(node => node.store.data?.taskId === taskId)
    if (nodeToRemove) {
      console.log('删除任务节点:', nodeToRemove.store.data?.taskName)
      oodaGraph.value.removeNode(nodeToRemove.id)
    }
  })
  
  // 同步更新树结构
  if (detailTreeData.value.length > 0) {
    const selectedTasks = taskList.value.filter(task => 
      selectedTaskIds.includes(task.id)
    )
    
    detailTreeData.value[0].children = selectedTasks.map(task => ({
      id: `task_${task.id}`,
      label: task.name,
      taskId: task.id
    }))
    
    console.log('树结构已同步更新，子节点数量:', detailTreeData.value[0].children.length)
  }
  
  // 可选：重新布局画布上的节点
  setTimeout(() => {
    relayoutCanvas()
  }, 100)
}

/** 重新布局画布节点 - 让节点排列更整齐 */
function relayoutCanvas() {
  if (!oodaGraph.value) return
  
  const nodes = oodaGraph.value.getNodes()
  const nodeCount = nodes.length
  
  if (nodeCount === 0) return
  
  // 重新排列节点位置
  nodes.forEach((node, index) => {
    const x = 100 + (index % 3) * 200
    const y = 100 + Math.floor(index / 3) * 150
    node.setPosition(x, y)
  })
  
  // 刷新画布
  oodaGraph.value.refresh()
}

/** 确认修改根节点 - 简化逻辑，因为选择时已经实时更新画布 */
function handleEditRootConfirm() {
  console.log('确认修改根节点，最终选中的任务:', editRootForm.selectedTasks)
  
  // 更新根节点名称
  if (detailTreeData.value.length > 0) {
    detailTreeData.value[0].label = editRootForm.name
    console.log('根节点名称更新为:', editRootForm.name)
  }
  
  // 注意：画布和树结构已经在 handleTaskSelectionChange 中实时更新了
  // 这里只需要关闭弹窗即可
  editRootDialogVisible.value = false
  
  // 可选：触发一个事件通知父组件数据已更改
  // emit('network-updated', getCurrentNetworkData())
}

/** 获取当前网络数据（用于通知父组件） */
function getCurrentNetworkData() {
  if (!oodaGraph.value) return null
  
  const nodes = oodaGraph.value.getNodes()
  const edges = oodaGraph.value.getEdges()
  
  const networkStructure = []
  
  nodes.forEach(node => {
    const nodeData = node.store.data
    if (nodeData.taskId) {
      const networkNode = {
        OODAPathId: nodeData.taskId,
        networkRelations: []
      }
      
      edges.forEach(edge => {
        const sourceId = edge.getSourceCellId()
        const targetId = edge.getTargetCellId()
        
        if (sourceId === node.id) {
          const targetNode = nodes.find(n => n.id === targetId)
          if (targetNode && targetNode.store.data.taskId) {
            networkNode.networkRelations.push({
              targetOODAPathId: targetNode.store.data.taskId,
              relationType: 0
            })
          }
        }
      })
      
      networkStructure.push(networkNode)
    }
  })
  
  return {
    name: detailTreeData.value[0]?.label || '默认根节点',
    networkStructure: networkStructure,
    graphJSONOODANetwork: JSON.stringify(oodaGraph.value.toJSON())
  }
}

/** 提交表单 */
function handleSubmit() {
  // 构建网络结构数据
  const networkStructure = []
  
  if (oodaGraph.value) {
    const nodes = oodaGraph.value.getNodes()
    const edges = oodaGraph.value.getEdges()
    
    nodes.forEach(node => {
      const nodeData = node.store.data
      if (nodeData.taskId) {
        const networkNode = {
          OODAPathId: nodeData.taskId,
          networkRelations: []
        }
        
        // 处理连线关系
        edges.forEach(edge => {
          const sourceId = edge.getSourceCellId()
          const targetId = edge.getTargetCellId()
          
          if (sourceId === node.id) {
            const targetNode = nodes.find(n => n.id === targetId)
            if (targetNode && targetNode.store.data.taskId) {
              networkNode.networkRelations.push({
                targetOODAPathId: targetNode.store.data.taskId,
                relationType: 0 // 默认关系类型
              })
            }
          }
        })
        
        networkStructure.push(networkNode)
      }
    })
  }
  
  // 保存画布状态
  const graphJSONOODANetwork = oodaGraph.value ? JSON.stringify(oodaGraph.value.toJSON()) : ''
  
  // 获取根节点名称（放到name字段中）
  const rootNodeName = detailTreeData.value[0]?.label || '默认根节点'
  
  // 构建提交数据
  const submitData = {
    id: props.formData.id || null,
    ...networkData,
    name: rootNodeName, // 使用根节点名称作为name
    description: props.formData.description || '这是一个OODA网络示例',
    pgTaskId: props.formData.pgTaskId || 'TASK_001',
    networkStructure: networkStructure,
    graphJSONOODANetwork: graphJSONOODANetwork
  }
  
  console.log('提交的数据:', submitData)
  emit('submit', submitData)
}

/** 关闭详情弹窗 */
function closeDetail() {
  emit('update:open', false)
  emit('cancel')
  
  if (oodaGraph.value) {
    oodaGraph.value.dispose()
  }
  if (paramGraph.value) {
    paramGraph.value.dispose()
  }
}

// 监听open变化，初始化画布
watch(() => props.open, async (newVal) => {
  if (newVal) {
    console.log('打开详情弹窗，开始初始化数据')
    console.log('当前formData:', props.formData)
    
    // 加载任务列表
    await loadTaskList()
    
    // 初始化树结构数据
    initDetailTreeData()
    
    // 初始化画布
    nextTick(() => {
      initOodaGraph()
      initParamGraph()
      
      // 如果有编辑数据，加载到画布
      if (props.formData.graphJSONOODANetwork) {
        try {
          const graphData = JSON.parse(props.formData.graphJSONOODANetwork)
          oodaGraph.value.fromJSON(graphData)
        } catch (error) {
          console.error('加载OODA网络画布数据失败:', error)
        }
      }
      
      if (props.formData.graphJSONParamNetwork) {
        try {
          const graphData = JSON.parse(props.formData.graphJSONParamNetwork)
          paramGraph.value.fromJSON(graphData)
        } catch (error) {
          console.error('加载参数网络画布数据失败:', error)
        }
      }
    })
  } else {
    // 弹窗关闭时清理画布
    if (oodaGraph.value) {
      oodaGraph.value.dispose()
    }
    if (paramGraph.value) {
      paramGraph.value.dispose()
    }
  }
})

// 监听编辑弹窗打开，确保每次打开时都能正确显示已选中的任务
watch(() => editRootDialogVisible.value, (newVal) => {
  if (newVal) {
    // 弹窗打开时，刷新选中状态
    nextTick(() => {
      if (oodaGraph.value) {
        const existingNodes = oodaGraph.value.getNodes()
        const existingTaskIds = existingNodes
          .map(node => node.store.data?.taskId)
          .filter(taskId => taskId !== undefined && taskId !== null)
        
        editRootForm.selectedTasks = existingTaskIds
        console.log('刷新选中状态:', editRootForm.selectedTasks)
      }
    })
  }
})

// 监听formData变化，确保数据更新时树结构也更新
watch(() => props.formData, (newVal) => {
  console.log('formData发生变化:', newVal)
  if (props.open) {
    // 弹窗打开时，formData变化需要重新初始化树结构
    initDetailTreeData()
  }
}, { deep: true })
</script>

<style scoped>
.ooda-detail-container {
  height: 600px;
}

.detail-layout {
  display: flex;
  height: 100%;
  gap: 15px;
}

.detail-left {
  width: 250px;
  background: #f8f9fa;
  border-radius: 4px;
  padding: 15px;
  border: 1px solid #e4e7ed;
}

.tree-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.detail-tree {
  background: transparent;
}

.detail-tree :deep(.el-tree-node__content) {
  height: 32px;
}

.detail-tree :deep(.el-tree-node__label) {
  font-size: 13px;
}

.detail-center {
  flex: 1;
  background: white;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.detail-right {
  width: 300px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.canvas-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  padding: 15px;
  border-bottom: 1px solid #e4e7ed;
  background: #f8f9fa;
}

.ooda-canvas-container {
  flex: 1;
  padding: 10px;
  overflow: hidden;
}

.ooda-canvas {
  width: 100%;
  height: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.param-canvas-container {
  flex: 1;
  padding: 10px;
  overflow: hidden;
}

.param-canvas {
  width: 100%;
  height: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  width: 100%;
}

.tree-actions {
  margin-left: auto;
}

/* 确保连接桩始终可点击 */
:deep(.x6-port-body) {
  pointer-events: visiblePainted !important;
  cursor: crosshair !important;
}

:deep(.x6-port-body circle) {
  pointer-events: visiblePainted !important;
}

/* 弹窗宽度调整 */
:deep(.el-dialog) {
  max-width: 90vw;
}

:deep(.el-dialog__body) {
  padding: 20px;
}
</style>