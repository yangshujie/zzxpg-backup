<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="120px">
      <el-form-item label="作战剖面" prop="profileId">
        <el-select 
          v-model="selectedProfile" 
          placeholder="请选择作战剖面" 
          clearable 
          style="width: 200px"
          @change="handleProfileChange"
        >
          <el-option
            v-for="item in profileOptions"
            :key="item.id"
            :label="item.profileName"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="任务名称" prop="taskName">
        <el-input
          v-model="queryParams.taskName"
          placeholder="请输入任务名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
       <el-form-item label="任务类型" prop="taskType">
              <el-select v-model="queryParams.taskType" placeholder="请选择任务类型" style="width: 100%">
                <el-option
                  v-for="item in taskTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="success" icon="Plus" @click="handleAdd">新增任务</el-button>
      </el-form-item>
    </el-form>

    <!-- 动态表格 -->
    <DynamicTable
      :table-data="taskList"
      :field-config="fieldConfig"
      :show-checkbox="true"
      :show-index="true"
      :loading="loading"
      empty-text="暂无作战任务数据"
      @selection-change="handleSelectionChange"
    >
      <template #operationSlot="{ row }">
        <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">修改</el-button>
        <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
      </template>
    </DynamicTable>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新增或修改对话框 -->
    <el-dialog :title="title" v-model="open" width="1200px" append-to-body class="task-management-dialog" @opened="onDialogOpened">
      <el-form ref="taskRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="任务名称" prop="taskName">
              <el-input v-model="form.taskName" placeholder="请输入任务名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="任务类型" prop="taskType">
              <el-select v-model="form.taskType" placeholder="请选择任务类型" style="width: 100%">
                <el-option
                  v-for="item in taskTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="任务阶段" prop="taskStage">
              <el-select v-model="form.taskStage" placeholder="请选择任务阶段" style="width: 100%">
                <el-option
                  v-for="item in taskStageOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="关联任务判据" prop="relationCriterion">
              <el-select 
                v-model="form.relationCriterion" 
                multiple 
                placeholder="请选择关联的任务判据" 
                style="width: 100%"
              >
                <el-option
                  v-for="item in criterionOptions"
                  :key="item.criterionId"
                  :label="item.criterionName"
                  :value="item.criterionId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- 任务编排区域 -->
      <div class="task-orchestration-section">
        <h4>任务编排</h4>
        <div class="orchestration-container">
          <!-- 左侧：子任务树 -->
          <div class="left-panel">
            <div class="subtask-tree-header">
              <span>子任务列表</span>
              <el-button type="primary" link icon="Plus" @click="addRootSubtask">添加根节点</el-button>
            </div>
            <div class="subtask-tree-container">
              <el-tree
                ref="subtaskTreeRef"
                :data="subtaskTreeData"
                :props="treeProps"
                node-key="id"
                highlight-current
                default-expand-all
                :expand-on-click-node="false"
              >
                <template #default="{ node, data }">
                  <div class="tree-node-content">
                    <el-input
                      v-if="data.editing"
                      v-model="data.taskName"
                      size="small"
                      style="width: 120px"
                      @blur="finishEdit(data)"
                      @keyup.enter="finishEdit(data)"
                      autofocus
                    />
                    <span v-else class="node-label" @dblclick="startEdit(data)">{{ data.taskName }}</span>
                    <span class="node-meta">
                      <el-tag size="small" type="info">
                        分组: {{ data.execSort || '?' }}
                      </el-tag>
                    </span>
                    <div class="node-actions">
                      <el-button link type="primary" size="small" @click.stop="addChildSubtask(data)">添加子节点</el-button>
                      <el-button link type="success" size="small" @click.stop="locateNodeInCanvas(data)">定位</el-button>
                      <el-button link type="danger" size="small" @click.stop="deleteSubtask(data)">删除</el-button>
                    </div>
                  </div>
                </template>
              </el-tree>
            </div>
            <div class="subtask-tips">
              <el-text size="small" type="info">提示：双击节点可编辑名称；右键点击连线可修改逻辑关系（与/或/连接）</el-text>
            </div>
          </div>

          <!-- 右侧：画布 -->
          <div class="right-panel">
            <div id="task-flow-canvas" class="task-flow-canvas"></div>
            <!-- 图例 -->
            <div class="edge-legend">
              <div class="legend-item">
                <svg width="20" height="20" viewBox="0 0 20 20">
                  <rect x="2" y="2" width="16" height="16" rx="2" fill="#E6F7FF" stroke="#1890FF" stroke-width="1.5" />
                </svg>
                <span>子任务</span>
              </div>
              <div class="legend-item">
                <svg width="30" height="20" viewBox="0 0 30 20">
                  <line x1="2" y1="10" x2="28" y2="10" stroke="#52C41A" stroke-width="2" stroke-dasharray="0"/>
                  <text x="15" y="8" font-size="8" fill="#52C41A" text-anchor="middle">与</text>
                </svg>
                <span>逻辑与 (实线)</span>
              </div>
              <div class="legend-item">
                <svg width="30" height="20" viewBox="0 0 30 20">
                  <line x1="2" y1="10" x2="28" y2="10" stroke="#FA8C16" stroke-width="2" stroke-dasharray="0"/>
                  <text x="15" y="8" font-size="8" fill="#FA8C16" text-anchor="middle">或</text>
                </svg>
                <span>逻辑或 (实线)</span>
              </div>
              <div class="legend-item">
                <svg width="30" height="20" viewBox="0 0 30 20">
                  <line x1="2" y1="10" x2="28" y2="10" stroke="#1890FF" stroke-width="2" stroke-dasharray="4,4"/>
                  <text x="15" y="8" font-size="8" fill="#1890FF" text-anchor="middle">连接</text>
                </svg>
                <span>普通连接 (虚线)</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="TaskManagement">
import { getTaskList, addTask, updateTask, deleteTask, queryTask, queryCriterionList } from '@/api/systemPlus/systemCooperation/task'
import { selectList } from '@/api/systemPlus/systemCooperation/combatProfile'
import DynamicTable from '@/components/DynamicTable/index.vue'
import { Graph, Shape } from '@antv/x6'
import { ref, reactive, toRefs, nextTick, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { v4 as uuidv4 } from 'uuid'

const { proxy } = getCurrentInstance()

const taskList = ref([])
const open = ref(false)
const loading = ref(true)
const total = ref(0)
const title = ref("")

// 子任务树数据（与画布同步）
const subtaskTreeData = ref([])
const subtaskTreeRef = ref(null)
const treeProps = { children: 'children', label: 'taskName' }

const profileOptions = ref([])
const selectedProfile = ref('')

const taskStageOptions = ref([
  { value: '1', label: '1' },
  { value: '2', label: '2' },
  { value: '3', label: '3' },
  { value: '4', label: '4' },
  { value: '5', label: '5' }
])

const taskTypeOptions = ref([
  { value: '航天侦察装备子体系', label: '航天侦察装备子体系' },
  { value: '太空态势感知装备子体系', label: '太空态势感知装备子体系' },
  { value: '太空攻防装备子体系', label: '太空攻防装备子体系' },
  { value: '航天发射装备子体系', label: '航天发射装备子体系' },
  { value: '航天测运控装备子体系', label: '航天测运控装备子体系' },  
  { value: '航天装备联合作战体系', label: '航天装备联合作战体系' }
])

const criterionOptions = ref([])
let graph = null
let isInternalSync = false

// 表格字段配置
const fieldConfig = ref([
  { key: 'taskName', label: '任务名称', width: 180, showOverflowTooltip: true },
  { key: 'taskType', label: '任务类型', width: 180, showOverflowTooltip: true },
  { 
    key: 'taskStage', 
    label: '任务阶段', 
    width: 120,
    formatter: (value) => {
      const stage = taskStageOptions.value.find(item => item.value === value)
      return stage ? stage.label : value
    }
  },
  { 
    key: 'relationCriterion', 
    label: '关联任务判据', 
    width: 200, 
    showOverflowTooltip: true,
    formatter: (value) => {
      if (!value) return ''
      if (Array.isArray(value)) {
        return value.map(id => {
          const criterion = criterionOptions.value.find(item => item.criterionId === id)
          return criterion ? criterion.criterionName : id
        }).join(', ')
      }
      return value || ''
    }
  },
  { key: 'createTime', label: '创建时间', width: 120 },
  {
    key: 'operation',
    label: '操作',
    width: 160,
    customColumn: true,
    slotName: 'operationSlot'
  }
])

const data = reactive({
  form: {
    taskId: null,
    taskName: '',
    taskType: '',
    taskStage: '',
    relationCriterion: [],
    desc: ''
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    profileId: undefined,
    taskName: undefined,
    taskType: undefined
  },
  rules: {
    taskName: [{ required: true, message: "任务名称不能为空", trigger: "blur" }],
    taskType: [{ required: true, message: "任务类型不能为空", trigger: "blur" }],
    taskStage: [{ required: true, message: "任务阶段不能为空", trigger: "change" }],
    relationCriterion: [{ required: true, message: "关联任务判据不能为空", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

// ========== 基础数据操作 ==========
const getProfileList = async () => {
  try {
    const response = await selectList({})
    profileOptions.value = response.data || []
    if (profileOptions.value.length > 0) {
      selectedProfile.value = profileOptions.value[0].profileId
      handleProfileChange()
    }
  } catch (error) {
    console.error('获取作战剖面列表失败:', error)
    profileOptions.value = []
  }
}

const handleProfileChange = () => {
  if (selectedProfile.value) {
    queryParams.value.profileId = selectedProfile.value
    getList()
  }
}

const loadCriterionOptions = async () => {
  try {
    const response = await queryCriterionList({})
    criterionOptions.value = response.data || []
  } catch (error) {
    console.error('获取判据列表失败:', error)
    criterionOptions.value = []
  }
}

function getList() {
  loading.value = true
  getTaskList(queryParams.value).then(response => {
    taskList.value = response.data || []
    total.value = taskList.value.length
    loading.value = false
  }).catch(error => {
    console.error('查询任务列表失败:', error)
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleSelectionChange(selection) {}

// ========== 分组计算算法（基于有向图的最长路径） ==========
const computeExecSorts = (nodes, edges) => {
  const adj = new Map()
  const inDegree = new Map()
  nodes.forEach(node => {
    adj.set(node.id, [])
    inDegree.set(node.id, 0)
  })
  
  edges.forEach(edge => {
    adj.get(edge.source).push(edge.target)
    inDegree.set(edge.target, inDegree.get(edge.target) + 1)
  })
  
  const execSort = new Map()
  const queue = []
  nodes.forEach(node => {
    if (inDegree.get(node.id) === 0) {
      queue.push(node.id)
      execSort.set(node.id, 1)
    }
  })
  
  while (queue.length) {
    const u = queue.shift()
    const currentSort = execSort.get(u) || 1
    for (const v of adj.get(u) || []) {
      const newSort = currentSort + 1
      if (!execSort.has(v) || execSort.get(v) < newSort) {
        execSort.set(v, newSort)
      }
      inDegree.set(v, inDegree.get(v) - 1)
      if (inDegree.get(v) === 0) {
        queue.push(v)
      }
    }
  }
  
  nodes.forEach(node => {
    if (!execSort.has(node.id)) {
      execSort.set(node.id, 1)
    }
  })
  
  return execSort
}

// 从画布同步数据到内部模型和左侧树
const syncFromGraph = () => {
  if (!graph || isInternalSync) return
  isInternalSync = true
  
  try {
    const nodes = graph.getNodes()
    const edges = graph.getEdges()
    
    const newTreeData = []
    nodes.forEach(node => {
      const nodeData = node.getData()
      if (nodeData && nodeData.type === 'subtask') {
        const treeNode = {
          id: nodeData.subTaskId,
          taskName: nodeData.taskName,
          execSort: nodeData.execSort || 1,
          editing: false,
          children: []
        }
        newTreeData.push(treeNode)
      }
    })
    
    // 只收集连接关系 (relationType === 2) 的连线用于分组计算
    const edgeList = []
    edges.forEach(edge => {
      const sourceNode = graph.getCellById(edge.getSourceCellId())
      const targetNode = graph.getCellById(edge.getTargetCellId())
      if (sourceNode && targetNode && sourceNode.getData()?.type === 'subtask' && targetNode.getData()?.type === 'subtask') {
        const relationType = edge.getData()?.relationType ?? 2
        if (relationType === 2) { // 连接关系影响分组
          edgeList.push({
            source: sourceNode.getData().subTaskId,
            target: targetNode.getData().subTaskId,
            relationType: relationType
          })
        }
      }
    })
    
    // 计算分组（基于连接关系连线的有向图最长路径）
    const execSortMap = computeExecSorts(newTreeData, edgeList)
    
    // 更新节点分组并同步到画布节点
    newTreeData.forEach(node => {
      node.execSort = execSortMap.get(node.id) || 1
      const canvasNode = nodes.find(n => n.getData()?.subTaskId === node.id)
      if (canvasNode) {
        canvasNode.setData({ ...canvasNode.getData(), execSort: node.execSort })
      }
    })
    
    subtaskTreeData.value = newTreeData
  } finally {
    isInternalSync = false
  }
}

// 根据 relationType 获取边的样式
const getEdgeAttrsByType = (relationType) => {
  switch (relationType) {
    case 0: // 与
      return {
        line: {
          stroke: '#52C41A',
          strokeWidth: 2,
          targetMarker: { name: 'block', width: 12, height: 8, fill: '#52C41A' },
        },
        label: {
          text: '与',
          fill: '#52C41A',
          fontSize: 10,
          fontWeight: 'bold',
        }
      }
    case 1: // 或
      return {
        line: {
          stroke: '#FA8C16',
          strokeWidth: 2,
          targetMarker: { name: 'block', width: 12, height: 8, fill: '#FA8C16' },
        },
        label: {
          text: '或',
          fill: '#FA8C16',
          fontSize: 10,
          fontWeight: 'bold',
        }
      }
    default: // 连接 (2)
      return {
        line: {
          stroke: '#1890FF',
          strokeWidth: 2,
          strokeDasharray: '5,5',
          targetMarker: { name: 'block', width: 12, height: 8, fill: '#1890FF' },
        },
        label: {
          text: '连接',
          fill: '#1890FF',
          fontSize: 10,
        }
      }
  }
}

// 更新单条边的样式
const updateEdgeStyle = (edge) => {
  const relationType = edge.getData()?.relationType ?? 2
  const attrs = getEdgeAttrsByType(relationType)
  edge.setAttrs({
    line: attrs.line,
    label: attrs.label
  })
}

// 右键菜单逻辑
const showEdgeContextMenu = (edge) => {
  const currentType = edge.getData()?.relationType ?? 2
  ElMessageBox.confirm(
    '请选择连线逻辑关系',
    '设置连线类型',
    {
      confirmButtonText: '与',
      cancelButtonText: '或',
      distinguishCancelAndClose: true,
      type: 'info',
      callback: (action) => {
        let newType
        if (action === 'confirm') {
          newType = 0
        } else if (action === 'cancel') {
          newType = 1
        } else {
          newType = 2
        }
        if (action !== 'close') {
          edge.setData({ ...edge.getData(), relationType: newType })
          updateEdgeStyle(edge)
          syncFromGraph()
        }
      }
    }
  ).catch(() => {})
}

// 双击循环切换逻辑
const cycleEdgeRelation = (edge) => {
  const currentType = edge.getData()?.relationType ?? 2
  let newType = (currentType + 1) % 3
  edge.setData({ ...edge.getData(), relationType: newType })
  updateEdgeStyle(edge)
  syncFromGraph()
  const typeName = newType === 0 ? '与' : (newType === 1 ? '或' : '连接')
  ElMessage.success(`连线类型已切换为：${typeName}`)
}

// ========== 端口配置 ==========
const getPortsConfig = () => ({
  groups: {
    top: {
      position: 'top',
      attrs: {
        circle: { r: 4, magnet: true, stroke: '#31d0c6', strokeWidth: 2, fill: '#fff' }
      }
    },
    right: {
      position: 'right',
      attrs: {
        circle: { r: 4, magnet: true, stroke: '#31d0c6', strokeWidth: 2, fill: '#fff' }
      }
    },
    bottom: {
      position: 'bottom',
      attrs: {
        circle: { r: 4, magnet: true, stroke: '#31d0c6', strokeWidth: 2, fill: '#fff' }
      }
    },
    left: {
      position: 'left',
      attrs: {
        circle: { r: 4, magnet: true, stroke: '#31d0c6', strokeWidth: 2, fill: '#fff' }
      }
    }
  },
  items: [
    { id: 'top', group: 'top' },
    { id: 'right', group: 'right' },
    { id: 'bottom', group: 'bottom' },
    { id: 'left', group: 'left' }
  ]
})

// ========== 节点操作 ==========
const addSubtaskNodeToCanvas = (subtask, x = null, y = null) => {
  if (!graph) return
  const existing = graph.getNodes().some(node => node.getData()?.subTaskId === subtask.id)
  if (existing) return
  
  const posX = x !== null ? x : Math.random() * 400 + 100
  const posY = y !== null ? y : Math.random() * 200 + 100
  
  const node = graph.addNode({
    shape: 'rect',
    x: posX,
    y: posY,
    width: 120,
    height: 60,
    attrs: {
      rect: {
        fill: '#E6F7FF',
        stroke: '#1890FF',
        strokeWidth: 2,
        rx: 6,
        ry: 6,
      },
      text: {
        text: subtask.taskName,
        fontSize: 12,
        fill: '#1890FF',
      },
    },
    ports: getPortsConfig(),
    data: {
      type: 'subtask',
      subTaskId: subtask.id,
      taskName: subtask.taskName,
      execSort: subtask.execSort || 1
    }
  })
  
  // 节点右键删除
  node.on('contextmenu', ({ e }) => {
    e.preventDefault()
    ElMessageBox.confirm('确认删除该节点及其所有连线？', '提示', { type: 'warning' })
      .then(() => {
        const edges = graph.getEdges().filter(edge => 
          edge.getSourceCellId() === node.id || edge.getTargetCellId() === node.id
        )
        edges.forEach(edge => graph.removeEdge(edge))
        graph.removeNode(node)
        syncFromGraph()
      })
      .catch(() => {})
  })
  
  // 双击编辑名称
  node.on('dblclick', () => startInlineEdit(node))
  
  return node
}

const startInlineEdit = (node) => {
  const nodeData = node.getData()
  if (nodeData.type !== 'subtask') return
  
  const bbox = node.getBBox()
  const container = graph.container
  const parentRect = container.getBoundingClientRect()
  const scale = graph.zoom()
  const left = parentRect.left + bbox.x * scale
  const top = parentRect.top + bbox.y * scale
  const width = bbox.width * scale
  const height = bbox.height * scale
  
  const input = document.createElement('input')
  input.value = nodeData.taskName
  input.style.cssText = `
    position: fixed; left: ${left}px; top: ${top}px; width: ${width}px; height: ${height}px;
    font-size: 12px; text-align: center; color: #333; border: 2px solid #1890FF;
    border-radius: 6px; background: #fff; outline: none; padding: 0; box-sizing: border-box;
    z-index: 10000;
  `
  document.body.appendChild(input)
  input.focus()
  input.select()
  
  const save = () => {
    const newName = input.value.trim()
    if (newName && newName !== nodeData.taskName) {
      nodeData.taskName = newName
      node.setData(nodeData)
      node.attr('text/text', newName)
      const treeNode = subtaskTreeData.value.find(t => t.id === nodeData.subTaskId)
      if (treeNode) treeNode.taskName = newName
      ElMessage.success('名称已更新')
    }
    input.remove()
  }
  input.addEventListener('blur', save)
  input.addEventListener('keypress', (e) => { if (e.key === 'Enter') save() })
}

const locateNodeInCanvas = (subtask) => {
  if (!graph) return
  const node = graph.getNodes().find(n => n.getData()?.subTaskId === subtask.id)
  if (node) {
    graph.centerCell(node)
    node.attr('rect/stroke', '#FF4D4F')
    setTimeout(() => {
      node.attr('rect/stroke', '#1890FF')
    }, 1000)
  } else {
    addSubtaskNodeToCanvas(subtask)
  }
}

// ========== 子任务树操作 ==========
const addRootSubtask = () => {
  const newId = uuidv4()
  const newSubtask = {
    id: newId,
    taskName: '新子任务',
    execSort: 1,
    editing: true,
    children: []
  }
  subtaskTreeData.value.push(newSubtask)
  addSubtaskNodeToCanvas(newSubtask)
  nextTick(() => {
    const nodeEl = document.querySelector(`.subtask-tree-container .el-tree-node[data-key="${newId}"]`)
    if (nodeEl) nodeEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
  syncFromGraph()
}

const addChildSubtask = (parentData) => {
  const newId = uuidv4()
  const newSubtask = {
    id: newId,
    taskName: '新子任务',
    execSort: 1,
    editing: true,
    children: []
  }
  if (!parentData.children) parentData.children = []
  parentData.children.push(newSubtask)
  addSubtaskNodeToCanvas(newSubtask)
  syncFromGraph()
}

const deleteSubtask = async (data) => {
  try {
    await ElMessageBox.confirm(`确认删除子任务“${data.taskName}”吗？`, '提示', { type: 'warning' })
    const deleteNodeRecursive = (node, parentArray) => {
      const index = parentArray.findIndex(item => item.id === node.id)
      if (index !== -1) {
        parentArray.splice(index, 1)
      }
      if (node.children) {
        node.children.forEach(child => deleteNodeRecursive(child, node.children))
      }
    }
    deleteNodeRecursive(data, subtaskTreeData.value)
    
    if (graph) {
      const canvasNode = graph.getNodes().find(n => n.getData()?.subTaskId === data.id)
      if (canvasNode) {
        const edges = graph.getEdges().filter(edge => 
          edge.getSourceCellId() === canvasNode.id || edge.getTargetCellId() === canvasNode.id
        )
        edges.forEach(edge => graph.removeEdge(edge))
        graph.removeNode(canvasNode)
      }
    }
    ElMessage.success('删除成功')
    syncFromGraph()
  } catch (error) {
    if (error !== 'cancel') console.error(error)
  }
}

const startEdit = (data) => {
  data.editing = true
  data.originalName = data.taskName
}

const finishEdit = (data) => {
  data.editing = false
  if (!data.taskName || data.taskName.trim() === '') {
    data.taskName = data.originalName || '未命名'
  }
  if (graph) {
    const node = graph.getNodes().find(n => n.getData()?.subTaskId === data.id)
    if (node) {
      node.setData({ ...node.getData(), taskName: data.taskName })
      node.attr('text/text', data.taskName)
    }
  }
}

// ========== 初始化画布 ==========
const initCanvas = () => {
  let container = document.getElementById('task-flow-canvas')
  if (!container) return

  if (graph) {
    graph.dispose()
    graph = null
  }
  
  const newContainer = container.cloneNode(false)
  container.parentNode.replaceChild(newContainer, container)
  container = newContainer

  graph = new Graph({
    container,
    width: '100%',
    height: '100%',
    grid: true,
    background: { color: '#fafafa' },
    mousewheel: { enabled: true, zoomAtMousePosition: true, minScale: 0.5, maxScale: 3 },
    panning: { enabled: true },
    preventDefaultContextMenu: false, // 重要：允许右键菜单事件
    connecting: {
      router: 'manhattan',
      connector: { name: 'rounded', args: { radius: 8 } },
      createEdge() {
        const edge = new Shape.Edge({
          attrs: getEdgeAttrsByType(2),
          data: { relationType: 2 },
          zIndex: 0,
        })
        return edge
      },
      validateMagnet: () => true,
      validateConnection: ({ sourceView, targetView }) => {
        if (sourceView === targetView) return false
        const sourceData = sourceView.cell.getData()
        const targetData = targetView.cell.getData()
        if (!sourceData || !targetData) return false
        if (sourceData.type !== 'subtask' || targetData.type !== 'subtask') return false
        const edges = graph.getEdges()
        return !edges.some(edge => 
          edge.getSourceCellId() === sourceView.cell.id && 
          edge.getTargetCellId() === targetView.cell.id
        )
      }
    }
  })

  graph.enableRubberband()
  graph.enableKeyboard()

  // 全局监听边的事件（最可靠）
  graph.on('edge:contextmenu', ({ edge, e }) => {
    e.preventDefault()
    e.stopPropagation()
    console.log('edge contextmenu triggered', edge.id) // 调试日志
    showEdgeContextMenu(edge)
  })

  graph.on('edge:dblclick', ({ edge, e }) => {
    e.preventDefault()
    e.stopPropagation()
    console.log('edge dblclick triggered', edge.id)
    cycleEdgeRelation(edge)
  })

  // 监听画布变化，同步数据
  graph.on('node:added', () => syncFromGraph())
  graph.on('node:removed', () => syncFromGraph())
  graph.on('edge:connected', () => syncFromGraph())
  graph.on('edge:removed', () => syncFromGraph())
  
  loadCanvasFromTreeData()
}

const loadCanvasFromTreeData = () => {
  if (!graph) return
  graph.clearCells()
  
  const addNodesRecursive = (nodes) => {
    nodes.forEach(node => {
      addSubtaskNodeToCanvas(node)
      if (node.children && node.children.length) {
        addNodesRecursive(node.children)
      }
    })
  }
  addNodesRecursive(subtaskTreeData.value)
}

const restoreEdgesFromRelationList = (subTaskList) => {
  if (!graph) return
  const nodeMap = new Map()
  graph.getNodes().forEach(node => {
    const data = node.getData()
    if (data && data.subTaskId) {
      nodeMap.set(data.subTaskId, node)
    }
  })
  
  subTaskList.forEach(task => {
    const targetId = task.subTaskId
    const targetNode = nodeMap.get(targetId)
    if (targetNode && task.relationList) {
      task.relationList.forEach(rel => {
        const sourceNode = nodeMap.get(rel.subTaskId)
        if (sourceNode) {
          const existingEdge = graph.getEdges().find(edge => 
            edge.getSourceCellId() === sourceNode.id && edge.getTargetCellId() === targetNode.id
          )
          if (!existingEdge) {
            const edge = graph.addEdge({
              source: { cell: sourceNode.id },
              target: { cell: targetNode.id },
              data: { relationType: rel.relationType ?? 2 }
            })
            updateEdgeStyle(edge)
          } else {
            existingEdge.setData({ relationType: rel.relationType ?? 2 })
            updateEdgeStyle(existingEdge)
          }
        }
      })
    }
  })
  syncFromGraph()
}

// ========== 表单提交与关闭 ==========
const generateSubTaskList = () => {
  if (!graph) return []
  
  const nodes = graph.getNodes()
  const edges = graph.getEdges()
  
  const nodeMap = new Map()
  nodes.forEach(node => {
    const data = node.getData()
    if (data && data.type === 'subtask') {
      nodeMap.set(data.subTaskId, {
        id: data.subTaskId,
        subTaskName: data.taskName,
        execSort: data.execSort || 1,
        relationList: []
      })
    }
  })
  
  edges.forEach(edge => {
    const sourceId = edge.getSourceCellId()
    const targetId = edge.getTargetCellId()
    const sourceNode = graph.getCellById(sourceId)
    const targetNode = graph.getCellById(targetId)
    if (sourceNode && targetNode && sourceNode.getData()?.type === 'subtask' && targetNode.getData()?.type === 'subtask') {
      const sourceData = sourceNode.getData()
      const targetData = targetNode.getData()
      const relationType = edge.getData()?.relationType ?? 2
      const targetItem = nodeMap.get(targetData.subTaskId)
      if (targetItem) {
        targetItem.relationList.push({
          relationType: relationType,
          id: sourceData.subTaskId,
          subTaskName: sourceData.taskName
        })
      }
    }
  })
  
  return Array.from(nodeMap.values())
}

const submitForm = () => {
  proxy.$refs["taskRef"].validate(async (valid) => {
    if (valid) {
      const subTaskList = generateSubTaskList()
      const submitData = {
        profileId: selectedProfile.value,
        taskName: form.value.taskName,
        taskType: form.value.taskType,
        taskStage: form.value.taskStage,
        relationCriterion: form.value.relationCriterion,
        subtaskDesc: JSON.stringify(graph ? graph.toJSON() : {}),
        subTaskList: subTaskList
      }
      if (form.value.taskId) submitData.taskId = form.value.taskId
      try {
        if (form.value.taskId) {
          await updateTask(submitData)
          ElMessage.success("修改成功")
        } else {
          await addTask(submitData)
          ElMessage.success("新增成功")
        }
        open.value = false
        getList()
      } catch (error) {
        console.error(error)
        ElMessage.error("操作失败")
      }
    }
  })
}

const handleAdd = () => {
  reset()
  open.value = true
  title.value = "新增任务"
  subtaskTreeData.value = []
  nextTick(() => {
    setTimeout(() => {
      initCanvas()
    }, 100)
  })
}

const handleUpdate = async (row) => {
  reset()
  const profileId = selectedProfile.value
  if (!profileId) {
    ElMessage.warning('请先选择作战剖面')
    return
  }
  try {
    const res = await queryTask({ taskId: row.taskId })
    const detail = Array.isArray(res.data) ? res.data[0] : res.data
    if (!detail) {
      ElMessage.error('未获取到任务详情')
      return
    }
    form.value = {
      taskId: detail.taskId,
      taskName: detail.taskName,
      taskType: detail.taskType,
      taskStage: detail.taskStage,
      relationCriterion: detail.relationCriterion ? [...detail.relationCriterion] : [],
      desc: detail.subtaskDesc || ''
    }
    
    const subTaskList = detail.subTaskList || []
    const treeNodes = subTaskList.map(item => ({
      id: item.subTaskId,
      taskName: item.subTaskName,
      execSort: item.execSort || 1,
      editing: false,
      children: []
    }))
    subtaskTreeData.value = treeNodes
    
    open.value = true
    title.value = "修改任务"
    nextTick(() => {
      setTimeout(() => {
        initCanvas()
        
        // 优先使用subtaskDesc中的完整画布数据
        if (detail.subtaskDesc && detail.subtaskDesc.trim()) {
          try {
            const graphData = JSON.parse(detail.subtaskDesc)
            console.log('从subtaskDesc加载画布数据:', graphData)
            
            if (graphData.cells && graphData.cells.length > 0) {
              // 清空现有画布
              graph.clearCells()
              
              // 加载完整的画布数据
              graph.fromJSON(graphData)
              console.log('画布数据加载完成')
              
              // 同步更新左侧el-tree结构
              syncFromGraph()
            } else {
              // 如果没有完整的画布数据，使用子任务列表恢复边关系
              restoreEdgesFromRelationList(subTaskList)
            }
          } catch (error) {
            console.error('解析subtaskDesc失败:', error)
            // 解析失败时使用子任务列表恢复边关系
            restoreEdgesFromRelationList(subTaskList)
          }
        } else {
          // 没有subtaskDesc时使用子任务列表恢复边关系
          restoreEdgesFromRelationList(subTaskList)
        }
      }, 100)
    })
  } catch (error) {
    console.error('加载任务详情失败', error)
    ElMessage.error('加载任务详情失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该任务？', '提示', { type: 'warning' }).then(async () => {
    await deleteTask({ taskId: String(row.taskId) })
    ElMessage.success('删除成功')
    getList()
  }).catch(() => {})
}

const onDialogOpened = () => {
  nextTick(() => {
    setTimeout(() => {
      if (!graph) initCanvas()
    }, 100)
  })
}

const cancel = () => { open.value = false; reset() }
const reset = () => {
  form.value = { taskId: null, taskName: '', taskType: '', taskStage: '', relationCriterion: [], desc: '' }
  subtaskTreeData.value = []
  if (graph) { graph.dispose(); graph = null }
  proxy.resetForm("taskRef")
}

onMounted(() => { getProfileList(); loadCriterionOptions() })
onUnmounted(() => { if (graph) graph.dispose() })
</script>

<style scoped>
.app-container { padding: 20px; }
.mb8 { margin-bottom: 20px; }
.dialog-footer { text-align: center; }
.dialog-footer .el-button { margin: 0 10px; }
:deep(.task-management-dialog .el-dialog) { max-height: 90vh; min-height: 700px; }
.task-orchestration-section { margin-top: 20px; border: 1px solid var(--el-border-color-light); border-radius: 6px; padding: 15px; min-height: 400px; }
.orchestration-container { display: flex; height: 550px; gap: 20px; }
.left-panel { width: 360px; display: flex; flex-direction: column; gap: 10px; border: 1px solid var(--el-border-color-light); border-radius: 4px; background: var(--el-bg-color); overflow: hidden; }
.subtask-tree-header { padding: 10px 12px; border-bottom: 1px solid var(--el-border-color-light); display: flex; justify-content: space-between; align-items: center; font-weight: 500; background: var(--el-fill-color-light); }
.subtask-tree-container { flex: 1; overflow: auto; padding: 8px; }
.tree-node-content { display: flex; align-items: center; justify-content: space-between; width: 100%; padding: 4px 0; gap: 8px; }
.node-label { flex: 1; font-size: 13px; font-weight: 500; cursor: pointer; }
.node-label:hover { background: var(--el-fill-color-light); }
.node-meta { display: flex; align-items: center; gap: 6px; font-size: 12px; }
.node-actions { display: none; gap: 4px; }
.tree-node-content:hover .node-actions { display: flex; }
.subtask-tips { padding: 8px 12px; border-top: 1px solid var(--el-border-color-light); background: var(--el-fill-color-lighter); }

.right-panel { flex: 1; border: 1px solid var(--el-border-color-light); border-radius: 4px; overflow: hidden; position: relative; }
.task-flow-canvas { width: 100%; height: 100%; min-height: 400px; background: var(--el-bg-color); }
:deep(.x6-graph) { width: 100% !important; height: 100% !important; }

/* 连线图例样式 */
.edge-legend {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 12px;
  color: #333;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 6px;
  backdrop-filter: blur(4px);
  border: 1px solid var(--el-border-color-light);
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.legend-item svg {
  flex-shrink: 0;
}
</style>