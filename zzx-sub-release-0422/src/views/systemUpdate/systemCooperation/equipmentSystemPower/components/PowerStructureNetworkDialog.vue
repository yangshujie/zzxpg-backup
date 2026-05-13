<template>
  <el-dialog :title="title" :model-value="open" width="1400px" append-to-body @close="handleClose" @update:model-value="$emit('update:open', $event)">
    <div class="network-editor-container">
      <!-- 左侧树形结构 -->
      <div class="left-panel">
        <div class="tree-section">
          <div class="tree-header">
            <el-input
              v-model="rootNodeName"
              placeholder="请输入根节点名称"
              size="small"
              style="width: 200px"
            />
            <el-button 
              type="primary" 
              size="small" 
              icon="Plus"
              @click="handleAddEquipmentGroup"
            >
              新增编组
            </el-button>
          </div>
          <el-tree
            ref="treeRef"
            :data="treeData"
            :props="treeProps"
            node-key="id"
            default-expand-all
            class="structure-tree"
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="tree-node">
                <span>{{ node.label }}</span>
                <el-button 
                  v-if="data.type === 'group'"
                  type="danger" 
                  size="small" 
                  link 
                  icon="Delete"
                  @click.stop="handleDeleteGroup(data)"
                >
                  删除编组
                </el-button>
              </div>
            </template>
          </el-tree>
        </div>
        
        <div class="edge-section">
          <h3>边信息</h3>
          <el-tree
            :data="edgeTreeData"
            :props="edgeTreeProps"
            node-key="id"
            default-expand-all
            class="structure-tree"
          >
            <template #default="{ data }">
              <div class="edge-tree-node">
                <span>{{ data.label }}</span>
                <el-button 
                  v-if="data.type === 'edge'"
                  type="danger" 
                  size="small" 
                  link 
                  @click.stop="deleteEdge(data)"
                >
                  删除
                </el-button>
              </div>
            </template>
          </el-tree>
        </div>
      </div>
      
      <!-- 右侧画布 -->
      <div class="right-panel">
        <div ref="graphContainer" class="graph-container"></div>
      </div>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="cancel">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </div>
    </template>
  </el-dialog>
  
  <!-- 装备编组选择弹窗（多选） -->
  <el-dialog v-model="groupSelectVisible" title="选择装备" width="600px" append-to-body>
    <el-form label-width="100px">
      <el-form-item label="加入类型">
        <el-select v-model="joinType" placeholder="请选择加入类型" style="width: 100%" @change="onJoinTypeChange">
          <el-option label="装备编组" value="group" />
          <el-option label="单装" value="single" />
        </el-select>
      </el-form-item>
      
      <!-- 装备编组选择 -->
      <el-form-item v-if="joinType === 'group'" label="选择编组">
        <el-select
          v-model="selectedGroups"
          multiple
          placeholder="请选择装备编组"
          style="width: 100%"
          filterable
        >
          <el-option
            v-for="item in groupOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      
      <!-- 单装选择 -->
      <el-form-item v-if="joinType === 'single'" label="选择单装">
        <el-select
          v-model="selectedEquipments"
          multiple
          placeholder="请选择单装"
          style="width: 100%"
          filterable
          @focus="loadEquipmentList"
        >
          <el-option
            v-for="item in equipmentOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="groupSelectVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGroupConfirm">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, nextTick, watch, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listEquipmentGroup, insertEquipmentNetwork, updateEquipmentNetwork, listEquipment } from '@/api/systemPlus/systemCooperation/equipment'
import * as G6 from '@antv/g6'

const props = defineProps({
  open: Boolean,
  title: String,
  formData: Object
})

const emit = defineEmits(['update:open', 'submit', 'cancel'])

// 使用 computed 或直接使用 props.open，不要定义本地 open 状态
const rootNodeName = ref('装备网络根节点')
const treeData = ref([
  {
    id: 'root',
    label: '装备网络根节点',
    type: 'root',
    children: []
  }
])
const edgeTreeData = ref([])
const groupSelectVisible = ref(false)
const joinType = ref('group') // 加入类型：group-装备编组，single-单装
const selectedGroups = ref([])
const groupOptions = ref([])
const selectedEquipments = ref([])
const equipmentOptions = ref([])
const graphContainer = ref(null)
let graph = null
let isCreatingEdge = false
let selectedSourceNode = null

const addedNodeIds = new Set()
const fullGraphData = ref({
  nodes: [],
  edges: [],
  combos: []
})

const getCanvasSize = () => {
  const container = graphContainer.value
  return {
    width: container?.clientWidth || 1000,
    height: container?.clientHeight || 700
  }
}

const nodeStylePool = [
  { shape: 'circle', color: '#409EFF' },
  { shape: 'rect', color: '#67C23A', radius: 4 },
  { shape: 'diamond', color: '#E6A23C' },
  { shape: 'triangle', color: '#F56C6C' },
  { shape: 'star', color: '#909399' }
]

const getEquipmentNodeStyle = (regionIndex) => {
  const style = nodeStylePool[regionIndex % nodeStylePool.length]
  const base = {
    type: style.shape,
    size: 40,
    style: {
      fill: style.color,
      stroke: '#fff',
      lineWidth: 2,
      cursor: 'pointer'
    },
    labelCfg: {
      style: {
        fill: '#fff',
        fontSize: 10
      },
      position: 'bottom'
    }
  }
  if (style.radius) base.style.radius = style.radius
  return base
}

const treeProps = { children: 'children', label: 'label' }
const edgeTreeProps = { children: 'children', label: 'label' }

// 加入类型变化处理
function onJoinTypeChange() {
  // 清空已选择的内容
  selectedGroups.value = []
  selectedEquipments.value = []
  
  if (joinType.value === 'group') {
    // 加载装备编组列表
    loadGroupOptions()
  }
}

// 加载装备列表
async function loadEquipmentList() {
  try {
    const response = await listEquipment({ pageNum: 1, pageSize: 1000 })
    equipmentOptions.value = response.rows || []
    console.log('加载装备列表:', equipmentOptions.value)
  } catch (error) {
    console.error('加载装备列表失败:', error)
    equipmentOptions.value = []
  }
}

// 加载装备编组列表
async function loadGroupOptions() {
  try {
    const response = await listEquipmentGroup({ pageNum: 1, pageSize: 1000 })
    groupOptions.value = response.rows || []
  } catch (error) {
    console.error('加载装备编组列表失败:', error)
    groupOptions.value = []
  }
}

// ---------- 通用名称获取（支持节点和编组）----------
const getNodeNameById = (id) => {
  // 先从节点中查找
  const node = fullGraphData.value.nodes.find(n => n.id === id)
  if (node) return node.label || node.name || id

  // 再从编组中查找
  const combo = fullGraphData.value.combos.find(c => c.id === id)
  if (combo) return combo.label || combo.name || id

  // 回退到树形结构中的装备或编组
  const findInTree = (nodes) => {
    for (const n of nodes) {
      if (n.id === id) return n.label
      if (n.children) {
        const found = findInTree(n.children)
        if (found) return found
      }
    }
    return null
  }
  return findInTree(treeData.value) || id
}

// ---------- 统一处理节点/编组点击（连线逻辑）----------
const handleItemClick = (item) => {
  if (!item) return
  const model = item.getModel()
  const currentId = model.id

  // 连线模式下的处理
  if (isCreatingEdge) {
    if (selectedSourceNode === currentId) {
      // 点击同一个节点/编组 -> 取消连线
      isCreatingEdge = false
      selectedSourceNode = null
      graph.get('canvas').setCursor('default')
      ElMessage.info('已取消创建连线')
    } else if (selectedSourceNode) {
      // 创建连线
      createEdge(selectedSourceNode, currentId)
      isCreatingEdge = false
      selectedSourceNode = null
      graph.get('canvas').setCursor('default')
    } else {
      // 异常情况：重置
      isCreatingEdge = false
      selectedSourceNode = null
      graph.get('canvas').setCursor('default')
    }
  } else {
    // 未处于连线模式 -> 开启连线模式
    isCreatingEdge = true
    selectedSourceNode = currentId
    graph.get('canvas').setCursor('crosshair')
    ElMessage.info('请点击目标节点/编组创建连线')
  }
}

// ---------- 创建连线（支持节点/编组混合）----------
const createEdge = (sourceId, targetId) => {
  if (sourceId === targetId) {
    ElMessage.warning('不能连接同一个节点')
    return
  }

  // 检查是否已存在双向连线
  const exists = fullGraphData.value.edges.some(e =>
    (e.source === sourceId && e.target === targetId) ||
    (e.source === targetId && e.target === sourceId)
  )
  if (exists) {
    ElMessage.warning('连线已存在')
    return
  }

  const sourceName = getNodeNameById(sourceId)
  const targetName = getNodeNameById(targetId)
  const edgeId = `edge_${Date.now()}_${Math.random()}`
  const edgeLabel = `${sourceName} → ${targetName}`

  fullGraphData.value.edges.push({
    id: edgeId,
    source: sourceId,
    target: targetId,
    label: edgeLabel,
    type: 'polyline'
  })

  edgeTreeData.value.push({
    id: edgeId,
    label: edgeLabel,
    source: sourceId,
    target: targetId,
    type: 'edge'
  })

  refreshGraph()
  ElMessage.success('连线创建成功')
}

// ---------- 删除连线 ----------
const deleteEdge = (edgeData) => {
  ElMessageBox.confirm('确定要删除这条连线吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const edgeIndex = fullGraphData.value.edges.findIndex(e => e.id === edgeData.id)
    if (edgeIndex !== -1) fullGraphData.value.edges.splice(edgeIndex, 1)

    const index = edgeTreeData.value.findIndex(e => e.id === edgeData.id)
    if (index !== -1) edgeTreeData.value.splice(index, 1)

    refreshGraph()
    ElMessage.success('连线删除成功')
  }).catch(() => {})
}

// ---------- 初始化 G6 画布 (v4) ----------
const initGraph = () => {
  if (!graphContainer.value) return
  const { width, height } = getCanvasSize()
  if (width === 0 || height === 0) {
    setTimeout(initGraph, 100)
    return
  }

  if (graph) {
    graph.destroy()
    graph = null
  }

  graph = new G6.Graph({
    container: graphContainer.value,
    width,
    height,
    modes: {
      default: ['drag-canvas', 'zoom-canvas', 'drag-node', 'drag-combo', 'click-select'] // 增加 drag-combo
    },
    defaultNode: {
      type: 'circle',
      size: 40,
      style: {
        fill: '#8b5cf6',
        stroke: '#f8fafc',
        lineWidth: 2,
        cursor: 'pointer',
        shadowColor: '#8b5cf6',
        shadowBlur: 8,
      },
      labelCfg: {
        position: 'bottom',
        offset: 0,
        style: {
          fill: '#e66465',
          fontSize: 12,
          fontStyle: 'italic',
          fontWeight: 'normal',
          background: {
            fill: 'linear-gradient(#e66465, #9198e5)',
            stroke: '#9ec9ff',
            radius: 2,
            padding: [2, 4, 2, 4],
          },
        },
      },
    },
    defaultEdge: {
      type: 'polyline',
      style: {
        stroke: '#94a3b8',
        lineWidth: 2,
        endArrow: {
          path: 'M 0,0 L 8,4 L 8,-4 Z',
          fill: '#94a3b8',
          stroke: '#94a3b8'
        },
        shadowColor: '#64748b',
        shadowBlur: 5,
      },
      labelCfg: {
        autoRotate: true,
        style: {
          fill: '#1890ff',
          fontSize: 12,
          fontWeight: 'bold',
          fontStyle: 'normal',
          background: {
            fill: 'linear-gradient(336deg, rgba(0,0,255,.8), rgba(0,0,255,0) 70.71%)',
            stroke: '#9ec9ff',
            radius: 2,
            padding: [2, 4, 2, 4],
          },
        },
      },
    },
    combo: {
      type: 'ellipse',
      style: {
        fill: 'rgba(56, 189, 248, 0.15)',
        stroke: '#38bdf8',
        lineWidth: 2,
        lineDash: [8, 8],
        shadowColor: '#38bdf8',
        shadowBlur: 10,
        cursor: 'pointer' // 增加指针样式，提示可点击
      },
      labelCfg: {
        position: 'top',
        offsetY: -10,
        style: {
          fill: '#f8fafc',
          fontSize: 14,
          fontWeight: 'bold',
          background: {
            fill: 'rgba(0,0,0,0.6)',
            stroke: '#38bdf8',
            radius: 4,
            padding: [4, 10, 4, 10],
          },
        },
      },
    },
  })

  // 节点点击
  graph.on('node:click', (evt) => {
    handleItemClick(evt.item)
  })

  // 编组点击（新增）
  graph.on('combo:click', (evt) => {
    handleItemClick(evt.item)
  })

  // 画布空白点击取消连线
  graph.on('canvas:click', () => {
    if (isCreatingEdge) {
      isCreatingEdge = false
      selectedSourceNode = null
      graph.get('canvas').setCursor('default')
      ElMessage.info('已取消创建连线')
    }
  })

  // 节点拖拽结束事件（节点拖入群组）
  graph.on('node:dragend', (evt) => {
    const node = evt.item
    const nodeModel = node.getModel()
    const nodeId = nodeModel.id
    
    console.log('节点拖拽结束:', nodeId, nodeModel)
    
    // 基于位置检测群组成员关系
    const nodeX = nodeModel.x
    const nodeY = nodeModel.y
    
    console.log('节点位置:', { x: nodeX, y: nodeY })
    
    // 检查节点是否拖入了某个群组
    const targetCombo = findComboAtPosition(nodeX, nodeY)
    console.log('检测到的目标群组:', targetCombo)
    
    if (targetCombo && targetCombo.id !== nodeModel.comboId) {
      // 节点被拖入了新的群组
      console.log('节点被拖入新群组:', targetCombo.id)
      handleNodeDropIntoCombo(nodeId, targetCombo.id)
    } else if (!targetCombo && nodeModel.comboId) {
      // 节点被拖出了群组
      console.log('节点被拖出群组')
      handleNodeDropOutOfCombo(nodeId)
    } else {
      console.log('群组关系未发生变化')
    }
  })

  graph.data(fullGraphData.value)
  graph.render()
}

// ---------- 刷新画布 ----------
const refreshGraph = () => {
  if (!graph) return
  graph.data(fullGraphData.value)
  graph.render()
  setTimeout(() => {
    if (graph) graph.fitView(20)
  }, 100)
}

// ---------- 从画布数据更新树形结构（边标签自动补全）----------
const updateTreeFromGraphData = (graphData) => {
  treeData.value[0].children = []
  edgeTreeData.value = []

  // 处理组合
  if (graphData.combos?.length) {
    graphData.combos.forEach(combo => {
      const groupNode = {
        id: combo.id,
        label: combo.name || combo.label || '未命名编组',
        type: 'group',
        children: []
      }
      treeData.value[0].children.push(groupNode)

      if (graphData.nodes?.length) {
        graphData.nodes
          .filter(node => node.comboId === combo.id)
          .forEach(child => {
            groupNode.children.push({
              id: child.id,
              label: child.name || child.label || '未命名装备',
              type: 'equipment',
              equipmentType: child.equipmentType
            })
          })
      }
    })
  }

  // 处理边：确保每条边都有可读标签
  if (graphData.edges?.length) {
    edgeTreeData.value = graphData.edges.map(edge => {
      // 如果已有标签但不清晰，重新生成
      let label = edge.label
      if (!label || label === 'undefined → undefined') {
        const sourceName = getNodeNameById(edge.source)
        const targetName = getNodeNameById(edge.target)
        label = `${sourceName} → ${targetName}`
      }
      return {
        id: edge.id,
        source: edge.source,
        target: edge.target,
        label: label,
        type: 'edge'
      }
    })
  }
}

// ---------- 加载网络结构（降级方案）----------
const loadNetworkStructureData = () => {
  try {
    const structure = JSON.parse(props.formData.networkStructure)
    console.log('加载networkStructure数据:', structure)

    treeData.value[0].label = props.formData.name || rootNodeName.value
    rootNodeName.value = props.formData.name || rootNodeName.value
    treeData.value[0].children = []

    structure.nodes.forEach(node => {
      if (node.type === 'group') {
        const groupNode = {
          id: node.id,
          label: node.name,
          type: 'group',
          children: []
        }
        treeData.value[0].children.push(groupNode)

        structure.nodes
          .filter(n => n.parentId === node.id)
          .forEach(child => {
            groupNode.children.push({
              id: child.id,
              label: child.name,
              type: 'equipment',
              equipmentType: child.equipmentType
            })
          })
      }
    })

    edgeTreeData.value = structure.edges?.map(edge => ({ ...edge, type: 'edge' })) || []

    const graphData = { nodes: [], edges: [], combos: [] }
    treeData.value[0].children.forEach(group => {
      graphData.combos.push({ id: group.id, label: group.label, x: 0, y: 0 })
      group.children.forEach(child => {
        graphData.nodes.push({
          id: child.id,
          label: child.label,
          nodeType: 'equipment',
          comboId: group.id
        })
      })
    })
    graphData.edges = edgeTreeData.value.map(edge => ({ ...edge }))
    fullGraphData.value = graphData
    refreshGraph()
  } catch (error) {
    console.error('解析网络结构数据失败:', error)
    ElMessage.error('加载网络数据失败')
  }
}

// ---------- 删除编组（同时删除相关边）----------
const handleDeleteGroup = (groupData) => {
  ElMessageBox.confirm(`确定要删除编组 "${groupData.label}" 吗？其内部所有装备节点及关联连线也会被删除。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const groupId = groupData.id
    const equipmentIds = groupData.children.map(child => child.id)
    const allRelatedIds = [...equipmentIds, groupId]

    // 删除与这些 id 相关的所有边（包括以 group 为端点的边）
    const relatedEdges = fullGraphData.value.edges.filter(edge =>
      allRelatedIds.includes(edge.source) || allRelatedIds.includes(edge.target)
    )
    relatedEdges.forEach(edge => {
      const idx = fullGraphData.value.edges.findIndex(e => e.id === edge.id)
      if (idx !== -1) fullGraphData.value.edges.splice(idx, 1)
      const edgeIdx = edgeTreeData.value.findIndex(e => e.id === edge.id)
      if (edgeIdx !== -1) edgeTreeData.value.splice(edgeIdx, 1)
    })

    // 删除装备节点
    equipmentIds.forEach(id => {
      const nodeIdx = fullGraphData.value.nodes.findIndex(n => n.id === id)
      if (nodeIdx !== -1) fullGraphData.value.nodes.splice(nodeIdx, 1)
      addedNodeIds.delete(id)
    })

    // 删除组合
    const comboIdx = fullGraphData.value.combos.findIndex(c => c.id === groupId)
    if (comboIdx !== -1) fullGraphData.value.combos.splice(comboIdx, 1)

    // 从树中删除
    const parentNode = treeData.value[0]
    const index = parentNode.children.findIndex(c => c.id === groupId)
    if (index !== -1) parentNode.children.splice(index, 1)

    repositionAllCombosInData()
    refreshGraph()
    ElMessage.success('编组删除成功')
  }).catch(() => {})
}

// ---------- 其余辅助函数（addGroupToTree, addGroupToGraphData, repositionAllCombosInData, calculateRegionPositions 等保持不变）----------
// 为节省篇幅，此处保留原有实现（未修改逻辑的函数无需改动，但请确保它们与修改后的数据兼容）
// 以下为原有函数，已确认可正常工作，无需修改
const addGroupToTree = (group) => {
  const groupNode = {
    id: group.id,
    label: group.name,
    type: 'group',
    children: (group.formationDetail || []).map(child => ({
      id: child.id,
      label: child.name,
      type: 'equipment',
      equipmentType: child.equipmentType
    }))
  }
  treeData.value[0].children.push(groupNode)
}

const addEquipmentToTree = (equipment) => {
  const equipmentNode = {
    id: equipment.id,
    label: equipment.name,
    type: 'equipment',
    equipmentType: equipment.type
  }
  treeData.value[0].children.push(equipmentNode)
}

const addGroupToGraphData = (group, regionIndex) => {
  const { width, height } = getCanvasSize()
  const regionCount = treeData.value[0].children.length
  const positions = calculateRegionPositions(regionCount, width, height)
  const position = positions[regionCount - 1]

  const comboId = group.id
  const equipmentCount = group.formationDetail?.length || 0
  if (equipmentCount === 0) return

  if (fullGraphData.value.combos.some(c => c.id === comboId)) {
    ElMessage.warning(`区域 "${group.name}" 已存在，跳过创建`)
    return
  }

  const nodeSize = 40
  const nodeMargin = 20
  const cellSize = nodeSize + nodeMargin
  const cols = Math.ceil(Math.sqrt(equipmentCount))
  const rows = Math.ceil(equipmentCount / cols)

  const paddingX = 60
  const paddingY = 50
  const regionWidth = cols * cellSize + paddingX * 2
  const regionHeight = rows * cellSize + paddingY * 2

  fullGraphData.value.combos.push({
    id: comboId,
    label: group.name,
    x: position.x,
    y: position.y
  })

  const nodeStyle = getEquipmentNodeStyle(regionIndex)
  const startX = position.x - regionWidth / 2 + paddingX
  const startY = position.y - regionHeight / 2 + paddingY
  const newNodes = []
  const skippedNodes = []

  for (let idx = 0; idx < group.formationDetail.length; idx++) {
    const child = group.formationDetail[idx]
    if (addedNodeIds.has(child.id)) {
      skippedNodes.push(child.name)
      continue
    }
    const row = Math.floor(idx / cols)
    const col = idx % cols
    const x = startX + col * cellSize + nodeSize / 2
    const y = startY + row * cellSize + nodeSize / 2
    newNodes.push({
      id: child.id,
      label: child.name,
      nodeType: 'equipment',
      comboId: comboId,
      x: x,
      y: y,
      ...nodeStyle,
    })
    addedNodeIds.add(child.id)
  }

  if (skippedNodes.length) {
    ElMessage.warning(`编组 "${group.name}" 中以下装备节点已存在，已跳过：${skippedNodes.join(', ')}`)
  }

  if (newNodes.length === 0) {
    ElMessage.warning(`编组 "${group.name}" 的所有装备节点都已存在，未创建新区域`)
    const index = treeData.value[0].children.findIndex(c => c.id === group.id)
    if (index !== -1) treeData.value[0].children.splice(index, 1)
    const comboIdx = fullGraphData.value.combos.findIndex(c => c.id === comboId)
    if (comboIdx !== -1) fullGraphData.value.combos.splice(comboIdx, 1)
    return
  }

  fullGraphData.value.nodes.push(...newNodes)
}

const addEquipmentToGraphData = (equipment, regionIndex) => {
  const { width, height } = getCanvasSize()
  const regionCount = treeData.value[0].children.length
  const positions = calculateRegionPositions(regionCount, width, height)
  const position = positions[regionCount - 1]

  if (addedNodeIds.has(equipment.id)) {
    ElMessage.warning(`装备 "${equipment.name}" 已存在，跳过创建`)
    return
  }

  const nodeStyle = getEquipmentNodeStyle(regionIndex)
  
  fullGraphData.value.nodes.push({
    id: equipment.id,
    label: equipment.name,
    nodeType: 'equipment',
    x: position.x,
    y: position.y,
    ...nodeStyle,
  })
  addedNodeIds.add(equipment.id)
}

// 根据位置查找群组
const findComboAtPosition = (x, y) => {
  const combos = fullGraphData.value.combos
  
  for (const combo of combos) {
    // 简化的碰撞检测：检查点是否在群组区域内
    // 这里假设群组有固定大小，实际应根据群组大小调整
    const comboWidth = 200 // 群组宽度
    const comboHeight = 150 // 群组高度
    
    const left = combo.x - comboWidth / 2
    const right = combo.x + comboWidth / 2
    const top = combo.y - comboHeight / 2
    const bottom = combo.y + comboHeight / 2
    
    if (x >= left && x <= right && y >= top && y <= bottom) {
      return combo
    }
  }
  
  return null
}

// 处理节点拖入群组
const handleNodeDropIntoCombo = (nodeId, comboId) => {
  console.log(`节点 ${nodeId} 被拖入群组 ${comboId}`)
  
  // 更新画布数据
  const nodeIndex = fullGraphData.value.nodes.findIndex(n => n.id === nodeId)
  if (nodeIndex !== -1) {
    fullGraphData.value.nodes[nodeIndex].comboId = comboId
  }
  
  // 更新树结构
  updateTreeStructureAfterDrag(nodeId, comboId)
  
  // 刷新图表
  refreshGraph()
  
  ElMessage.success('节点已成功加入群组')
}

// 处理节点拖出群组
const handleNodeDropOutOfCombo = (nodeId) => {
  console.log(`节点 ${nodeId} 被拖出群组`)
  
  // 更新画布数据
  const nodeIndex = fullGraphData.value.nodes.findIndex(n => n.id === nodeId)
  if (nodeIndex !== -1) {
    fullGraphData.value.nodes[nodeIndex].comboId = undefined
  }
  
  // 更新树结构
  updateTreeStructureAfterDrag(nodeId, null)
  
  // 刷新图表
  refreshGraph()
  
  ElMessage.success('节点已从群组中移除')
}

// 更新树结构（拖拽后）
const updateTreeStructureAfterDrag = (nodeId, comboId) => {
  console.log('开始更新树结构:', { nodeId, comboId })
  
  // 在树结构中查找节点
  const findNodeInTree = (nodes, targetId) => {
    for (const node of nodes) {
      if (node.id === targetId) {
        return { node, parent: nodes }
      }
      if (node.children) {
        const result = findNodeInTree(node.children, targetId)
        if (result) return result
      }
    }
    return null
  }
  
  const result = findNodeInTree(treeData.value, nodeId)
  if (!result) {
    console.log('未找到节点:', nodeId)
    return
  }
  
  const { node, parent } = result
  console.log('找到节点:', { node, parent })
  
  if (comboId) {
    // 节点被拖入群组：将节点移动到群组的children中
    const groupNode = findNodeInTree(treeData.value, comboId)
    console.log('查找群组节点:', { comboId, groupNode })
    
    if (groupNode && groupNode.node.type === 'group') {
      // 从原位置移除
      const index = parent.indexOf(node)
      if (index !== -1) {
        parent.splice(index, 1)
        console.log('从原位置移除节点')
      }
      
      // 添加到群组的children中
      if (!groupNode.node.children) {
        groupNode.node.children = []
      }
      groupNode.node.children.push(node)
      console.log('添加到群组children中:', groupNode.node.children)
    }
  } else {
    // 节点被拖出群组：将节点移动到根节点的children中
    const rootNode = treeData.value[0]
    
    // 从原位置移除
    const index = parent.indexOf(node)
    if (index !== -1) {
      parent.splice(index, 1)
      console.log('从群组中移除节点')
    }
    
    // 添加到根节点的children中
    if (!rootNode.children) {
      rootNode.children = []
    }
    rootNode.children.push(node)
    console.log('添加到根节点children中:', rootNode.children)
  }
  
  // 强制更新树结构显示 - 使用深度拷贝确保Vue响应式更新
  treeData.value = JSON.parse(JSON.stringify(treeData.value))
  console.log('树结构更新完成:', treeData.value)
}

const repositionAllCombosInData = () => {
  const groups = treeData.value[0].children.filter(child => child.type === 'group')
  if (groups.length === 0) return
  const { width, height } = getCanvasSize()
  const positions = calculateRegionPositions(groups.length, width, height)

  groups.forEach((group, idx) => {
    const comboId = group.id
    const position = positions[idx]
    if (!position) return

    const comboIndex = fullGraphData.value.combos.findIndex(c => c.id === comboId)
    if (comboIndex !== -1) {
      fullGraphData.value.combos[comboIndex].x = position.x
      fullGraphData.value.combos[comboIndex].y = position.y
    }

    const equipmentCount = group.children?.length || 0
    if (equipmentCount === 0) return

    const nodeSize = 40
    const nodeMargin = 20
    const cellSize = nodeSize + nodeMargin
    const cols = Math.ceil(Math.sqrt(equipmentCount))
    const rows = Math.ceil(equipmentCount / cols)
    const paddingX = 60
    const paddingY = 50
    const regionWidth = cols * cellSize + paddingX * 2
    const regionHeight = rows * cellSize + paddingY * 2
    const startX = position.x - regionWidth / 2 + paddingX
    const startY = position.y - regionHeight / 2 + paddingY

    group.children.forEach((child, childIdx) => {
      const row = Math.floor(childIdx / cols)
      const col = childIdx % cols
      const x = startX + col * cellSize + nodeSize / 2
      const y = startY + row * cellSize + nodeSize / 2
      const nodeIndex = fullGraphData.value.nodes.findIndex(n => n.id === child.id)
      if (nodeIndex !== -1) {
        fullGraphData.value.nodes[nodeIndex].x = x
        fullGraphData.value.nodes[nodeIndex].y = y
      }
    })
  })
}

const calculateRegionPositions = (regionCount, width, height) => {
  const positions = []
  const centerX = width / 2
  const centerY = height / 2
  const radius = Math.min(width, height) * 0.45

  if (regionCount === 1) {
    positions.push({ x: centerX, y: centerY })
  } else if (regionCount === 2) {
    const offsetX = radius * 1.2
    positions.push({ x: centerX - offsetX, y: centerY })
    positions.push({ x: centerX + offsetX, y: centerY })
  } else {
    for (let i = 0; i < regionCount; i++) {
      const angle = (i * 2 * Math.PI) / regionCount - Math.PI / 2
      positions.push({ x: centerX + radius * Math.cos(angle), y: centerY + radius * Math.sin(angle) })
    }
  }
  return positions
}

// ---------- 监听弹窗打开/关闭 ----------
watch(() => props.open, (newVal) => {
  open.value = newVal
  if (newVal) {
    nextTick(() => {
      initGraph()
      loadEquipmentGroups()

      if (props.formData && props.formData.graphJSONEquipmentNetwork) {
        try {
          const graphData = JSON.parse(props.formData.graphJSONEquipmentNetwork)
          fullGraphData.value = graphData
          if (fullGraphData.value.nodes) {
            fullGraphData.value.nodes.forEach(node => {
              if (!node.nodeType && node.type !== 'combo') node.nodeType = 'equipment'
            })
          }
          treeData.value[0].label = props.formData.name || rootNodeName.value
          rootNodeName.value = props.formData.name || rootNodeName.value
          if (graph) {
            graph.data(graphData)
            graph.render()
            graph.fitView(20)
            updateTreeFromGraphData(graphData)
          }
        } catch (error) {
          console.error('解析graphJSONEquipmentNetwork失败:', error)
          loadNetworkStructureData()
        }
      } else if (props.formData && props.formData.networkStructure) {
        loadNetworkStructureData()
      } else {
        if (treeData.value[0]?.children?.length) {
          treeData.value[0].children.forEach((group, index) => {
            if (group.type === 'group') {
              addGroupToGraphData({
                id: group.id,
                name: group.label,
                formationDetail: group.children?.map(child => ({
                  id: child.id,
                  name: child.label,
                  equipmentType: child.equipmentType
                })) || []
              }, index)
            }
          })
          repositionAllCombosInData()
          refreshGraph()
        }
      }
    })
  } else {
    if (graph) {
      graph.destroy()
      graph = null
    }
    resetData()
  }
})

watch(rootNodeName, (newVal) => {
  if (treeData.value[0]?.id === 'root') {
    treeData.value[0].label = newVal
  }
})

const resetData = () => {
  treeData.value = [{
    id: 'root',
    label: rootNodeName.value,
    type: 'root',
    children: []
  }]
  edgeTreeData.value = []
  addedNodeIds.clear()
  isCreatingEdge = false
  selectedSourceNode = null
  fullGraphData.value = { nodes: [], edges: [], combos: [] }
  if (graph) {
    graph.data(fullGraphData.value)
    graph.render()
  }
}

const loadEquipmentGroups = async () => {
  try {
    const res = await listEquipmentGroup({ pageNum: 1, pageSize: 1000 })
    groupOptions.value = res.rows || res.data || []
  } catch (error) {
    console.error('加载装备编组失败:', error)
    ElMessage.error('加载装备编组失败')
  }
}

const handleAddEquipmentGroup = () => {
  groupSelectVisible.value = true
}

const handleGroupConfirm = async () => {
  if (joinType.value === 'group') {
    // 处理装备编组
    if (!selectedGroups.value.length) {
      ElMessage.warning('请至少选择一个装备编组')
      return
    }
    if (!graph) {
      ElMessage.warning('画布尚未初始化，请稍后再试')
      return
    }

    const existingGroupIds = new Set(treeData.value[0].children.map(child => child.id))
    const newGroups = groupOptions.value.filter(g =>
      selectedGroups.value.includes(g.id) && !existingGroupIds.has(g.id)
    )

    if (newGroups.length === 0) {
      ElMessage.warning('所选编组均已存在')
      groupSelectVisible.value = false
      selectedGroups.value = []
      return
    }

    for (const group of newGroups) {
      addGroupToTree(group)
      const regionIndex = treeData.value[0].children.length - 1
      addGroupToGraphData(group, regionIndex)
    }
  } else if (joinType.value === 'single') {
    // 处理单装
    if (!selectedEquipments.value.length) {
      ElMessage.warning('请至少选择一个单装')
      return
    }
    if (!graph) {
      ElMessage.warning('画布尚未初始化，请稍后再试')
      return
    }

    const existingEquipmentIds = new Set(treeData.value[0].children.map(child => child.id))
    const newEquipments = equipmentOptions.value.filter(e =>
      selectedEquipments.value.includes(e.id) && !existingEquipmentIds.has(e.id)
    )

    if (newEquipments.length === 0) {
      ElMessage.warning('所选单装均已存在')
      groupSelectVisible.value = false
      selectedEquipments.value = []
      return
    }

    for (const equipment of newEquipments) {
      addEquipmentToTree(equipment)
      const regionIndex = treeData.value[0].children.length - 1
      addEquipmentToGraphData(equipment, regionIndex)
    }
  }

  repositionAllCombosInData()
  refreshGraph()

  groupSelectVisible.value = false
  selectedGroups.value = []
  selectedEquipments.value = []
}

const handleNodeClick = (data, node) => {
  console.log('节点点击:', data, node)
}

const buildNetworkStructure = () => {
  const nodes = []
  const edges = []
  const collect = (treeNodes, parentId = null) => {
    for (const node of treeNodes) {
      nodes.push({
        id: node.id,
        name: node.label,
        type: node.type,
        equipmentType: node.equipmentType,
        parentId: parentId
      })
      if (node.children) collect(node.children, node.id)
    }
  }
  collect(treeData.value)
  for (const edge of edgeTreeData.value) {
    edges.push({
      id: edge.id,
      source: edge.source,
      target: edge.target,
      label: edge.label
    })
  }
  return { nodes, edges, rootName: rootNodeName.value }
}

const submitForm = () => {
  const structure = buildNetworkStructure()

  let graphJSONEquipmentNetwork = ''
  if (graph && typeof graph.save === 'function') {
    const graphData = graph.save()
    graphJSONEquipmentNetwork = JSON.stringify(graphData)
  } else {
    console.warn('graph 无效或没有 save 方法，将保存空字符串')
  }

  const networkStructure = structure.nodes.map(node => ({
    equipmentFormationId: node.id,
    subSystemType: 0,
    networkRelations: null
  }))

  const submitData = {
    graphJSONEquipmentNetwork,
    name: rootNodeName.value,
    description: '这是一个装备编队网络示例',
    pgTaskId: 'TASK_001',
    networkStructure
  }

  if (props.formData && props.formData.id) {
    submitData.id = props.formData.id
    updateEquipmentNetwork(submitData).then(() => {
      ElMessage.success('修改成功')
      emit('update:open', false)
      emit('submit', submitData)
    }).catch(() => {
      ElMessage.error('修改失败')
    })
  } else {
    insertEquipmentNetwork(submitData).then(() => {
      ElMessage.success('新增成功')
      emit('update:open', false)
      emit('submit', submitData)
    }).catch(() => {
      ElMessage.error('新增失败')
    })
  }
}

const cancel = () => {
  emit('update:open', false)
  emit('cancel')
}

const handleClose = () => {
  if (graph) {
    graph.destroy()
    graph = null
  }
  resetData()
}

onUnmounted(() => {
  if (graph) {
    graph.destroy()
    graph = null
  }
})
</script>
<style scoped>
.network-editor-container {
  display: flex;
  height: 600px;
}

.left-panel {
  width: 320px;
  border-right: 1px solid #e4e7ed;
  padding: 10px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.right-panel {
  flex: 1;
  padding: 10px;
}

.tree-section {
  flex: 1;
  margin-bottom: 20px;
  min-height: 300px;
}

.edge-section {
  flex: 1;
  min-height: 200px;
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.structure-tree {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px;
  height: calc(100% - 40px);
  overflow-y: auto;
}

.graph-container {
  width: 100%;
  height: 100%;
  border: 1px solid #2d3748;
  border-radius: 8px;
  background: 
    linear-gradient(45deg, #0f172a 25%, transparent 25%), 
    linear-gradient(-45deg, #0f172a 25%, transparent 25%), 
    linear-gradient(45deg, transparent 75%, #0f172a 75%), 
    linear-gradient(-45deg, transparent 75%, #0f172a 75%);
  background-size: 20px 20px;
  background-position: 0 0, 0 10px, 10px -10px, -10px 0px;
  background-color: #1a202c;
  position: relative;
  overflow: hidden;
}

.graph-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 80%, rgba(56, 189, 248, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(139, 92, 246, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 40% 40%, rgba(16, 185, 129, 0.05) 0%, transparent 50%);
  pointer-events: none;
}

.dialog-footer {
  text-align: center;
}

.dialog-footer .el-button {
  margin: 0 10px;
}

.edge-tree-node,
.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

h3 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 14px;
}
</style>