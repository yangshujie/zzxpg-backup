<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <div class="super-network-build">
    <!-- 顶部操作栏 - 简约高级风格 -->
    <div class="header">
      <div class="header-left">
        <el-button-group class="mode-group">
          <el-button :type="isBuildMode ? 'primary' : ''" @click="handleBuildMode">构建</el-button>
          <el-button :type="!isBuildMode ? 'primary' : ''" @click="handlePreviewMode">预览</el-button>
        </el-button-group>

        <el-divider direction="vertical" />

        <el-button-group class="action-group">
          <el-button @click="handleSave" type="success" plain>保存</el-button>
          <el-button @click="handleClear" type="warning" plain>清空</el-button>
        </el-button-group>

        <el-divider direction="vertical" />

        <el-button-group class="tool-group">
          <el-button :type="currentEdgeTool === 'free' ? 'primary' : ''" @click="setEdgeMode">
            <el-icon>
              <Connection />
            </el-icon>
            {{ currentEdgeTool === 'free' ? '连线中' : '连线' }}
          </el-button>
          <el-button :type="lassoEnabled ? 'primary' : ''" @click="enableLassoSelect">
            <el-icon><Select /></el-icon>
            框选
          </el-button>
          <el-button :type="currentTool === 'select' ? 'primary' : ''" @click="setTool('select')">
            <el-icon>
              <Pointer />
            </el-icon>
          </el-button>
          <el-button :type="currentTool === 'drag' ? 'primary' : ''" @click="setTool('drag')">
            <el-icon>
              <Rank />
            </el-icon>
          </el-button>
        </el-button-group>
      </div>

      <div class="header-right">
        <div class="mode-badge">
          <span class="dot" :class="{ active: isBuildMode }"></span>
          <span>{{ isBuildMode ? '构建模式' : '预览模式' }}</span>
        </div>
        <el-badge v-if="currentEdgeTool !== 'none'" :value="'连线中'" type="primary" class="edge-badge" />
        <el-badge v-if="lassoEnabled" :value="'框选'" type="info" class="lasso-badge" />
      </div>
    </div>

    <div class="main-content">
      <div class="left-panel">
        <NetworkTree :equipment-tree-data="equipmentTreeData" :ooda-tree-data="oodaTreeData"
          :task-tree-data="taskTreeData" @node-click="handleNodeClick" />
      </div>

      <div class="right-panel">
        <div class="canvas-container" ref="canvasContainer"></div>

        <div class="debug-panel glass-panel" v-if="showDebug">
          <div class="debug-header">
            <span>调试信息</span>
            <el-button size="small" text @click="showDebug = false">关闭</el-button>
          </div>
          <div class="debug-content">
            <div>组合: {{ graphData.combos.length }} | 节点: {{ graphData.nodes.length }} | 连线: {{ graphData.edges.length }}
            </div>
            <div>任务: {{ getCombosByCategory('task').length }} | OODA: {{ getCombosByCategory('ooda').length }} | 装备: {{
              getCombosByCategory('equipment').length }}</div>
          </div>
        </div>
        <el-button class="debug-btn" size="small" text @click="showDebug = !showDebug">
          <el-icon>
            <InfoFilled />
          </el-icon>
        </el-button>
      </div>
    </div>

    <!-- 对话框组件 -->
    <el-drawer v-model="propertyDrawerVisible" title="节点属性" direction="rtl" size="350px">
      <div v-if="selectedNode" class="property-panel">
        <el-form label-width="100px">
          <el-form-item label="节点ID">
            <el-input v-model="selectedNode.id" disabled />
          </el-form-item>
          <el-form-item label="节点名称">
            <el-input v-model="selectedNode.label" />
          </el-form-item>
          <el-form-item label="节点类型">
            <el-input :value="selectedNode.isCombo ? '组合节点' : '内部节点'" disabled />
          </el-form-item>
          <el-form-item label="分类">
            <el-input v-model="selectedNode.category" disabled />
          </el-form-item>
        </el-form>
      </div>
    </el-drawer>

    <!-- 右键菜单 -->
    <Teleport to="body">
      <div v-show="edgeContextMenuVisible" class="context-menu"
        :style="{ left: edgeContextMenuX + 'px', top: edgeContextMenuY + 'px' }" @click.stop>
        <div class="context-menu-item" @click="openEdgeEditDialog(contextMenuEdge)">
          <el-icon>
            <Edit />
          </el-icon>
          <span>编辑连线</span>
        </div>
        <div class="context-menu-item" @click="deleteEdge">
          <el-icon>
            <Delete />
          </el-icon>
          <span>删除连线</span>
        </div>
      </div>

      <div v-show="contextMenuVisible" class="context-menu"
        :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }" @click.stop>
        <div class="context-menu-item" @click="handleViewProperty">
          <el-icon>
            <InfoFilled />
          </el-icon>
          <span>编辑属性</span>
        </div>
        <div class="context-menu-item" @click="contextMenuTarget?.type === 'combo' ? deleteCombo() : deleteNode()">
          <el-icon>
            <Delete />
          </el-icon>
          <span>删除</span>
        </div>
      </div>
    </Teleport>

    <!-- 编辑对话框 -->
    <el-dialog v-model="edgeEditDialogVisible" title="编辑连线" width="400px">
      <el-form :model="edgeEditForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="edgeEditForm.label" placeholder="请输入连线名称" />
        </el-form-item>
        <el-form-item label="颜色">
          <div class="color-selector">
            <div v-for="color in colorOptions" :key="color.value" class="color-option"
              :style="{ backgroundColor: color.value }" :class="{ active: edgeEditForm.stroke === color.value }"
              @click="edgeEditForm.stroke = color.value">
            </div>
          </div>
        </el-form-item>
        <el-form-item label="粗细">
          <el-slider v-model="edgeEditForm.lineWidth" :min="1" :max="5" />
        </el-form-item>
        <el-form-item label="样式">
          <el-select v-model="edgeEditForm.lineDash" placeholder="请选择样式">
            <el-option :value="[]" label="实线" />
            <el-option :value="[5, 5]" label="虚线" />
            <el-option :value="[10, 5]" label="点划线" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="edgeEditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdgeEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="nodeEditDialogVisible" title="编辑节点" width="450px">
      <el-form :model="nodeEditForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="nodeEditForm.label" placeholder="请输入节点名称" />
        </el-form-item>
        <el-form-item label="形状">
          <div class="shape-selector">
            <div v-for="shape in nodeShapeOptions" :key="shape.value" class="shape-option"
              :class="{ active: nodeEditForm.shape === shape.value }" @click="nodeEditForm.shape = shape.value">
              <div :class="['shape-preview', shape.value]"></div>
              <span>{{ shape.label }}</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="填充色">
          <div class="color-selector">
            <div v-for="color in colorOptions" :key="color.value" class="color-option"
              :style="{ backgroundColor: color.value }" :class="{ active: nodeEditForm.fill === color.value }"
              @click="nodeEditForm.fill = color.value">
            </div>
          </div>
        </el-form-item>
        <el-form-item label="大小">
          <el-slider v-model="nodeEditForm.size[0]" :min="60" :max="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nodeEditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNodeEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, getCurrentInstance } from 'vue'
import { insertMappingRelation } from '@/api/systemPlus/projectManagement/superNetwork.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Select, Rank, Connection, InfoFilled, Edit, Delete, Pointer } from '@element-plus/icons-vue'
import { Graph } from '@antv/g6'
import {
  getSubTasks,
  getTaskList
} from '@/api/systemPlus/systemCooperation/task'
import NetworkTree from './NetworkTree.vue'

const { proxy } = getCurrentInstance()

// 状态管理
const isBuildMode = ref(true)
const propertyDrawerVisible = ref(false)
const selectedNode = ref(null)
const currentTool = ref('select')
const showDebug = ref(false)
const lassoEnabled = ref(false)
const currentEdgeTool = ref('none')
let edgeSourceItem = null
let edgeSourceType = null
const edgeSourceName = ref('')

// 右键菜单相关
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
let contextMenuTarget = null

// 边右键菜单相关
const edgeContextMenuVisible = ref(false)
const edgeContextMenuX = ref(0)
const edgeContextMenuY = ref(0)
let contextMenuEdge = null
let currentZoom = 1
let currentMatrix = null
// 连线编辑对话框
const edgeEditDialogVisible = ref(false)
const editingEdge = ref(null)
const edgeEditForm = ref({
  label: '',
  stroke: '#5F95FF',
  lineWidth: 2,
  lineDash: []
})

// 节点编辑对话框
const nodeEditDialogVisible = ref(false)
const editingNode = ref(null)
const nodeEditForm = ref({
  label: '',
  fill: '#409EFF',
  stroke: '#337ecc',
  size: [100, 50],
  shape: 'rect'
})

// 节点形状选项
const nodeShapeOptions = [
  { value: 'rect', label: '矩形' },
  { value: 'circle', label: '圆形' },
  { value: 'diamond', label: '菱形' },
  { value: 'triangle', label: '三角形' }
]

// 颜色选项
const colorOptions = [
  { value: '#409EFF', label: '蓝色' },
  { value: '#F08F56', label: '橙色' },
  { value: '#67C23A', label: '绿色' },
  { value: '#F56C6C', label: '红色' },
  { value: '#E6A23C', label: '黄色' },
  { value: '#909399', label: '灰色' },
  { value: '#FF69B4', label: '粉色' },
  { value: '#9B59B6', label: '紫色' }
]

// 树形数据
const equipmentTreeData = ref([])
const oodaTreeData = ref([])
const taskTreeData = ref([])

// G6相关
const canvasContainer = ref(null)
let graph = null

// 图数据
const graphData = ref({
  nodes: [],
  edges: [],
  combos: []
})

// 存储每个 combo 的内部数据（节点和边）
const comboInnerData = new Map()
// 存储每个combo的折叠状态
const comboCollapsedState = new Map()

// ==================== 辅助函数 ====================

// 获取连线工具名称
const getEdgeToolName = () => {
  if (currentEdgeTool.value === 'node-node') return '节点-节点'
  if (currentEdgeTool.value === 'combo-node') return 'Combo-节点'
  if (currentEdgeTool.value === 'combo-combo') return 'Combo-Combo'
  return ''
}

// 关闭右键菜单
const closeContextMenu = () => {
  contextMenuVisible.value = false
  edgeContextMenuVisible.value = false
  contextMenuTarget = null
  contextMenuEdge = null
}

// 查看属性菜单项点击
const handleViewProperty = () => {
  if (contextMenuTarget) {
    if (contextMenuTarget.type === 'node' || contextMenuTarget.shape) {
      openNodeEditDialog(contextMenuTarget)
    } else if (contextMenuTarget.type === 'combo') {
      selectedNode.value = contextMenuTarget
      propertyDrawerVisible.value = true
    }
  }
  closeContextMenu()
}

// 全局点击关闭菜单
const onDocumentClick = () => {
  if (contextMenuVisible.value || edgeContextMenuVisible.value) {
    closeContextMenu()
  }
}

// 清除所有选中状态
const clearAllSelected = () => {
  if (!graph) return
  const allNodes = graph.getNodes()
  const allCombos = graph.getCombos()
    ;[...allNodes, ...allCombos].forEach(item => {
      item.setState('selected', false)
    })
}

// 获取指定分类的 Combos
const getCombosByCategory = (category) => {
  return graphData.value.combos.filter(c => c.category === category)
}

// 根据 combo 类型获取内部节点的形状
const getNodeShapeForCombo = (comboData) => {
  const name = comboData.name || ''
  const id = comboData.id || ''

  if (name.includes('侦察') || name.includes('编组') || id.includes('AC635F5C')) {
    return 'circle'
  }
  if (name.includes('实施') || name.includes('部门') || id.includes('2986317E')) {
    return 'rect'
  }
  if (name.includes('用测') || name.includes('测试') || id.includes('2BF4A34D')) {
    return 'diamond'
  }
  if (name.includes('创智') || name.includes('系统') || id.includes('FB8C3C2F')) {
    return 'triangle'
  }
  return 'rect'
}

// 根据分类获取节点颜色
const getNodeColorForCategory = (category) => {
  if (category === 'task') return '#F08F56'
  if (category === 'ooda') return '#1783FF'
  if (category === 'equipment') return '#67C23A'
  return '#409EFF'
}

// 设置连线模式（简化版，不限制类型）
const setEdgeMode = () => {
  if (currentEdgeTool.value === 'free') {
    currentEdgeTool.value = 'none'
    if (edgeSourceItem) {
      edgeSourceItem.setState('selected', false)
      edgeSourceItem = null
      edgeSourceType = null
      edgeSourceName.value = ''
    }
    clearAllSelected()
    ElMessage.info('连线模式已关闭')
  } else {
    if (lassoEnabled.value) {
      enableLassoSelect()
    }
    currentEdgeTool.value = 'free'
    if (edgeSourceItem) {
      edgeSourceItem.setState('selected', false)
      edgeSourceItem = null
      edgeSourceType = null
      edgeSourceName.value = ''
    }
    clearAllSelected()
    ElMessage.success('连线模式已开启，点击任意节点/Combo开始连线（可任意连接）')
  }
}

// 转换任务网络 X6 数据
const convertTaskX6ToInnerData = (x6Data, parentComboId, baseX, baseY) => {
  if (!x6Data?.cells) return { nodes: [], edges: [] }

  const nodeMap = new Map()
  const nodes = []
  const edges = []

  let minX = Infinity, minY = Infinity
  x6Data.cells.forEach(cell => {
    if (cell.shape === 'rect' || cell.shape === 'circle') {
      if (cell.position) {
        minX = Math.min(minX, cell.position.x || 0)
        minY = Math.min(minY, cell.position.y || 0)
      }
    }
  })

  minX = minX === Infinity ? 0 : minX
  minY = minY === Infinity ? 0 : minY

  x6Data.cells.forEach((cell, idx) => {
    if (cell.shape === 'rect' || cell.shape === 'circle') {
      const originalId = cell.id
      const nodeId = originalId || `${parentComboId}_inner_${idx}_${Date.now()}_${Math.random()}`

      let category = 'task'
      let color = '#F08F56'

      let nodeLabel = ''
      if (cell.attrs?.label?.text) {
        nodeLabel = cell.attrs.label.text
      } else if (cell.attrs?.text?.text) {
        nodeLabel = cell.attrs.text.text
      } else if (cell.data?.taskName) {
        nodeLabel = cell.data.taskName
      } else {
        nodeLabel = `节点${idx + 1}`
      }

      const offsetX = (cell.position?.x || 0) - minX + 60
      const offsetY = (cell.position?.y || 0) - minY + 60

      let nodeStyle = {}
      let nodeFill = color
      let nodeStroke = '#1890FF'
      let nodeRadius = 6

      if (cell.attrs?.rect) {
        nodeFill = cell.attrs.rect.fill || color
        nodeStroke = cell.attrs.rect.stroke || '#1890FF'
        nodeRadius = cell.attrs.rect.rx || 6
      }

      nodeStyle = {
        fill: nodeFill,
        stroke: nodeStroke,
        lineWidth: cell.attrs?.rect?.['stroke-width'] || 2,
        radius: nodeRadius,
        cursor: 'pointer'
      }

      const nodeWidth = cell.size?.width || 120
      const nodeHeight = cell.size?.height || 60

      let labelFill = '#1890FF'
      let labelFontSize = 12
      if (cell.attrs?.text) {
        labelFill = cell.attrs.text.fill || '#1890FF'
        labelFontSize = cell.attrs.text.fontSize || 12
      }

      nodes.push({
        id: nodeId,
        originalId: originalId,
        label: nodeLabel,
        type: 'node',
        category: category,
        comboId: parentComboId,
        x: baseX + offsetX,
        y: baseY + offsetY,
        size: [nodeWidth, nodeHeight],
        shape: 'rect',
        style: nodeStyle,
        labelCfg: {
          style: {
            fill: labelFill,
            fontSize: labelFontSize,
            fontWeight: 'normal',
            textAlign: 'center',
            textBaseline: 'middle'
          },
          position: 'center',
          offset: 0
        },
        originalData: cell
      })

      nodeMap.set(originalId, nodeId)
    }
  })

  x6Data.cells.forEach((cell, idx) => {
    if (cell.shape === 'edge' && cell.source?.cell && cell.target?.cell) {
      const sourceOriginalId = cell.source.cell
      const targetOriginalId = cell.target.cell
      const sourceId = nodeMap.get(sourceOriginalId)
      const targetId = nodeMap.get(targetOriginalId)

      if (sourceId && targetId) {
        let edgeStroke = '#1890FF'
        let edgeLineWidth = 2
        let edgeLineDash = []
        let edgeLabel = cell.attrs?.label?.text || ''
        let labelFill = '#1890FF'
        let labelFontSize = 10

        if (cell.attrs?.line) {
          edgeStroke = cell.attrs.line.stroke || '#1890FF'
          edgeLineWidth = cell.attrs.line['stroke-width'] || 2
          if (cell.attrs.line.strokeDasharray) {
            edgeLineDash = [5, 5]
          }
        }

        if (cell.attrs?.label) {
          edgeLabel = cell.attrs.label.text || ''
          labelFill = cell.attrs.label.fill || '#1890FF'
          labelFontSize = cell.attrs.label.fontSize || 10
        }

        let endArrow = {
          path: 'M 0,0 L 8,4 L 8,-4 Z',
          fill: edgeStroke
        }

        if (cell.attrs?.line?.targetMarker) {
          const marker = cell.attrs.line.targetMarker
          if (marker.name === 'block') {
            endArrow = {
              path: 'M 0,0 L 8,4 L 8,-4 Z',
              fill: marker.fill || edgeStroke
            }
          }
        }

        edges.push({
          id: `${parentComboId}_edge_${cell.id || idx}_${Date.now()}`,
          source: sourceId,
          target: targetId,
          label: edgeLabel,
          style: {
            stroke: edgeStroke,
            lineWidth: edgeLineWidth,
            lineDash: edgeLineDash,
            endArrow: endArrow
          },
          labelCfg: {
            style: {
              fill: labelFill,
              fontSize: labelFontSize,
              fontWeight: 'normal',
              background: {
                fill: 'rgba(255,255,255,0.8)',
                radius: 2,
                padding: [2, 4, 2, 4]
              }
            },
            position: 'center',
            autoRotate: true,
            offset: 0
          },
          originalData: cell
        })
      }
    }
  })

  console.log('转换后的任务网络数据:', { nodes, edges })
  return { nodes, edges }
}

// 转换 OODA 网络 X6 数据为 G6 内部节点和边（保持连接关系）
const convertOODAToInnerData = (x6Data, parentComboId, baseX, baseY) => {
  if (!x6Data?.cells) return { nodes: [], edges: [] }

  const nodeMap = new Map()
  const nodes = []
  const edges = []

  let minX = Infinity, minY = Infinity
  x6Data.cells.forEach(cell => {
    if (cell.shape === 'rect' || cell.shape === 'circle') {
      if (cell.position) {
        minX = Math.min(minX, cell.position.x || 0)
        minY = Math.min(minY, cell.position.y || 0)
      }
    }
  })

  minX = minX === Infinity ? 0 : minX
  minY = minY === Infinity ? 0 : minY

  x6Data.cells.forEach((cell, idx) => {
    if (cell.shape === 'rect' || cell.shape === 'circle') {
      const originalId = cell.id
      const nodeId = originalId || `${parentComboId}_inner_${idx}_${Date.now()}_${Math.random()}`
      let category = 'ooda'
      let color = '#1783FF'

      if (cell.taskId) {
        category = 'task'
        color = '#F08F56'
      } else if (cell.equipmentId) {
        category = 'equipment'
        color = '#00C9C9'
      }

      const offsetX = (cell.position?.x || 0) - minX + 60
      const offsetY = (cell.position?.y || 0) - minY + 60

      let nodeStyle = {}
      let nodeFill = color
      let nodeStroke = color
      let nodeRadius = 6

      if (cell.attrs?.rect) {
        nodeFill = cell.attrs.rect.fill || color
        nodeStroke = cell.attrs.rect.stroke || color
        nodeRadius = cell.attrs.rect.rx || 6
      }

      nodeStyle = {
        fill: nodeFill,
        stroke: nodeStroke,
        lineWidth: cell.attrs?.rect?.['stroke-width'] || 2,
        radius: nodeRadius,
        cursor: 'pointer'
      }

      let nodeLabel = ''
      if (cell.attrs?.label?.text) {
        nodeLabel = cell.attrs.label.text
      } else if (cell.attrs?.text?.text) {
        nodeLabel = cell.attrs.text.text
      } else {
        nodeLabel = cell.taskName || cell.name || `节点${idx + 1}`
      }

      const nodeWidth = cell.size?.width || 90
      const nodeHeight = cell.size?.height || 45

      nodes.push({
        id: nodeId,
        originalId: originalId,
        label: nodeLabel,
        type: 'node',
        category: category,
        comboId: parentComboId,
        x: baseX + offsetX,
        y: baseY + offsetY,
        size: [nodeWidth, nodeHeight],
        shape: cell.shape === 'circle' ? 'circle' : 'rect',
        style: nodeStyle,
        labelCfg: {
          style: {
            fill: '#e66465',
            fontSize: 11,
            fontStyle: 'italic'
          },
          position: 'bottom',
          offset: 5
        },
        originalData: cell
      })

      nodeMap.set(originalId, nodeId)
    }
  })

  x6Data.cells.forEach((cell, idx) => {
    if (cell.shape === 'edge' && cell.source?.cell && cell.target?.cell) {
      const sourceOriginalId = cell.source.cell
      const targetOriginalId = cell.target.cell
      const sourceId = nodeMap.get(sourceOriginalId)
      const targetId = nodeMap.get(targetOriginalId)

      if (sourceId && targetId) {
        let edgeStroke = '#5F95FF'
        let edgeLineWidth = 2
        let edgeLabel = cell.attrs?.label?.text || ''
        let labelFill = '#1890ff'

        if (cell.attrs?.line) {
          edgeStroke = cell.attrs.line.stroke || '#5F95FF'
          edgeLineWidth = cell.attrs.line['stroke-width'] || 2
        }

        if (cell.attrs?.label) {
          edgeLabel = cell.attrs.label.text || ''
          labelFill = cell.attrs.label.fill || '#1890ff'
        }

        edges.push({
          id: `${parentComboId}_edge_${cell.id || idx}_${Date.now()}`,
          source: sourceId,
          target: targetId,
          label: edgeLabel,
          style: {
            stroke: edgeStroke,
            lineWidth: edgeLineWidth,
            endArrow: {
              path: 'M 0,0 L 8,4 L 8,-4 Z',
              fill: edgeStroke
            }
          },
          labelCfg: {
            style: {
              fill: labelFill,
              fontSize: 10,
              background: {
                fill: 'rgba(24,144,255,0.15)',
                radius: 3,
                padding: [2, 4, 2, 4]
              }
            },
            position: 'center',
            autoRotate: true
          },
          originalData: cell
        })
      }
    }
  })

  console.log('转换后的OODA网络数据:', { nodes, edges })
  return { nodes, edges }
}

// 转换 G6 数据为内部数据结构
const convertG6ToInnerData = (g6Data, parentComboId, baseX, baseY) => {
  if (!g6Data) return { nodes: [], edges: [], combos: [] }

  const nodes = []
  const edges = []
  const subCombos = []

  let minX = Infinity, minY = Infinity
  if (g6Data.nodes) {
    g6Data.nodes.forEach(node => {
      if (node.x !== undefined) minX = Math.min(minX, node.x)
      if (node.y !== undefined) minY = Math.min(minY, node.y)
    })
  }
  if (g6Data.combos) {
    g6Data.combos.forEach(combo => {
      if (combo.x !== undefined) minX = Math.min(minX, combo.x)
      if (combo.y !== undefined) minY = Math.min(minY, combo.y)
    })
  }

  if (minX === Infinity) minX = 0
  if (minY === Infinity) minY = 0

  const offsetX = baseX - minX
  const offsetY = baseY - minY

  if (g6Data.combos && Array.isArray(g6Data.combos)) {
    g6Data.combos.forEach(combo => {
      const comboId = combo.id
      const existingCombo = graphData.value.combos.find(c => c.id === comboId)
      if (existingCombo) return

      const newX = combo.x + offsetX
      const newY = combo.y + offsetY

      let subCategory = 'equipment'
      if (parentComboId) {
        const parentCombo = graphData.value.combos.find(c => c.id === parentComboId)
        if (parentCombo) subCategory = parentCombo.category
      }

      const color = subCategory === 'task' ? '#F08F56' :
        (subCategory === 'ooda' ? '#1783FF' : '#52c41a')

      if (combo.children && combo.children.length > 0) {
        combo.children.forEach(child => {
          if (child.itemType === 'node') {
            const childNode = g6Data.nodes?.find(n => n.id === child.id)
            if (childNode) {
              const nodeId = childNode.id
              const nodeX = childNode.x + offsetX
              const nodeY = childNode.y + offsetY

              nodes.push({
                id: nodeId,
                label: childNode.label || childNode.name || '节点',
                type: 'node',
                category: subCategory,
                comboId: comboId,
                x: nodeX,
                y: nodeY,
                size: childNode.size || [90, 45],
                shape: childNode.type || 'rect',
                style: childNode.style || {
                  fill: color,
                  stroke: '#fff',
                  lineWidth: 2,
                  radius: 6
                },
                labelCfg: childNode.labelCfg || {
                  style: { fill: '#fff', fontSize: 11 },
                  position: 'bottom',
                  offset: 5
                },
                originalData: childNode
              })
            }
          }
        })
      }

      subCombos.push({
        id: comboId,
        label: combo.label || '组合',
        type: 'combo',
        category: subCategory,
        collapsed: true,
        hasGraphData: true,
        innerNodeCount: combo.children?.filter(c => c.itemType === 'node').length || 0,
        x: newX,
        y: newY,
        size: combo.style?.width ? [combo.style.width, combo.style.height] : [160, 120],
        style: {
          fill: `${color}20`,
          stroke: color,
          lineWidth: 2.5,
          radius: 10,
          fillOpacity: 0.35,
          shadowColor: color,
          shadowBlur: 8
        },
        labelCfg: {
          style: {
            fill: color,
            fontSize: 13,
            fontWeight: 'bold',
            background: {
              fill: 'rgba(0,0,0,0.7)',
              radius: 4,
              padding: [4, 8, 4, 8]
            }
          },
          position: 'top',
          offset: 0,
          refY: -5
        },
        originalData: combo
      })

      const subInnerNodes = nodes.filter(n => n.comboId === comboId)
      comboInnerData.set(comboId, {
        nodes: subInnerNodes,
        edges: [],
        combos: []
      })
    })
  }

  if (g6Data.nodes && Array.isArray(g6Data.nodes)) {
    g6Data.nodes.forEach(node => {
      const belongsToSubCombo = g6Data.combos?.some(combo =>
        combo.children?.some(child => child.id === node.id && child.itemType === 'node')
      )

      if (belongsToSubCombo) return

      const nodeId = node.id
      const existingNode = graphData.value.nodes.find(n => n.id === nodeId)
      if (existingNode) return

      const newX = node.x + offsetX
      const newY = node.y + offsetY

      let category = 'equipment'
      if (parentComboId) {
        const parentCombo = graphData.value.combos.find(c => c.id === parentComboId)
        if (parentCombo) category = parentCombo.category
      }

      let nodeColor = '#67C23A'
      if (node.type === 'circle') nodeColor = '#409EFF'
      else if (node.type === 'diamond') nodeColor = '#E6A23C'
      else if (node.type === 'triangle') nodeColor = '#F56C6C'

      let nodeStyle = node.style || {}
      nodeStyle = {
        fill: nodeStyle.fill || nodeColor,
        stroke: nodeStyle.stroke || '#fff',
        lineWidth: nodeStyle.lineWidth || 2,
        cursor: 'pointer',
        ...nodeStyle
      }

      nodes.push({
        id: nodeId,
        label: node.label || node.name || '节点',
        type: 'node',
        category: category,
        comboId: parentComboId || node.comboId,
        x: newX,
        y: newY,
        size: node.size || (node.type === 'circle' ? [40, 40] : [90, 45]),
        shape: node.type || 'rect',
        style: nodeStyle,
        labelCfg: node.labelCfg || {
          position: 'bottom',
          offset: 0,
          style: {
            fill: '#fff',
            fontSize: 10,
            fontStyle: 'italic',
            background: {
              fill: 'linear-gradient(#e66465, #9198e5)',
              radius: 2,
              padding: [2, 4, 2, 4]
            }
          }
        },
        originalData: node
      })
    })
  }

  if (g6Data.edges && Array.isArray(g6Data.edges)) {
    g6Data.edges.forEach(edge => {
      const edgeId = edge.id
      const existingEdge = graphData.value.edges.find(e => e.id === edgeId)
      if (existingEdge) return

      const sourceId = edge.source
      const targetId = edge.target

      const sourceExists = nodes.some(n => n.id === sourceId) ||
        graphData.value.nodes.some(n => n.id === sourceId) ||
        g6Data.combos?.some(c => c.id === sourceId)
      const targetExists = nodes.some(n => n.id === targetId) ||
        graphData.value.nodes.some(n => n.id === targetId) ||
        g6Data.combos?.some(c => c.id === targetId)

      if (sourceExists && targetExists) {
        let edgeStyle = edge.style || {}
        edgeStyle = {
          stroke: edgeStyle.stroke || '#94a3b8',
          lineWidth: edgeStyle.lineWidth || 2,
          endArrow: edgeStyle.endArrow || {
            path: 'M 0,0 L 8,4 L 8,-4 Z',
            fill: '#94a3b8'
          },
          ...edgeStyle
        }

        edges.push({
          id: edgeId,
          source: sourceId,
          target: targetId,
          label: edge.label || '',
          type: edge.type || 'line',
          style: edgeStyle,
          labelCfg: edge.labelCfg || {
            autoRotate: true,
            style: {
              fill: '#1890ff',
              fontSize: 12,
              fontWeight: 'bold',
              background: {
                fill: 'linear-gradient(336deg, rgba(0,0,255,.8), rgba(0,0,255,0) 70.71%)',
                radius: 2,
                padding: [2, 4, 2, 4]
              }
            }
          },
          isComboEdge: edge.isComboEdge || false,
          originalData: edge
        })
      }
    })
  }

  return { nodes, edges, combos: subCombos }
}

// 查找画布上的空白区域
const findEmptyPosition = () => {
  const existingPositions = []

  graphData.value.combos.forEach(combo => {
    existingPositions.push({ x: combo.x, y: combo.y, width: 160, height: 100 })
  })

  graphData.value.nodes.forEach(node => {
    existingPositions.push({ x: node.x, y: node.y, width: 80, height: 40 })
  })

  const centerX = 500, centerY = 300
  const spacing = 120

  for (let radius = 0; radius < 1000; radius += spacing) {
    for (let angle = 0; angle < 360; angle += 30) {
      const x = centerX + radius * Math.cos(angle * Math.PI / 180)
      const y = centerY + radius * Math.sin(angle * Math.PI / 180)

      const isOverlapping = existingPositions.some(pos => {
        return Math.abs(x - pos.x) < (160 + pos.width) / 2 &&
          Math.abs(y - pos.y) < (100 + pos.height) / 2
      })

      if (!isOverlapping) {
        return { x, y }
      }
    }
  }

  return { x: 800, y: 100 }
}

// 创建 Combo 节点
const createCombo = (nodeData, category, hasGraphData = false, graphDataInput = null, customPosition = null) => {
  const color = category === 'task' ? '#F08F56' : (category === 'equipment' ? '#52c41a' : '#1783FF')

  let baseX = 200, baseY = 150
  if (category === 'task') {
    baseX = 150
    baseY = 100
  } else if (category === 'ooda') {
    baseX = 500
    baseY = 100
  } else if (category === 'equipment') {
    baseX = 850
    baseY = 100
  }

  const existingCount = getCombosByCategory(category).length
  const x = customPosition ? customPosition.x : (baseX + (existingCount % 3) * 160)
  const y = customPosition ? customPosition.y : (baseY + Math.floor(existingCount / 3) * 130)

  const tempComboId = nodeData.id

  let innerNodeCount = 0
  let innerComboCount = 0

  if (hasGraphData && graphDataInput) {
    let nodes = []
    let edges = []

    if (category === 'equipment') {
      const { nodes: g6Nodes, edges: g6Edges, combos: g6Combos } = convertG6ToInnerData(graphDataInput, tempComboId, x, y)
      nodes = g6Nodes
      edges = g6Edges
      innerNodeCount = nodes.length
      innerComboCount = g6Combos.length
      comboInnerData.set(tempComboId, { nodes, edges, combos: g6Combos })
    } else if (category === 'task') {
      const { nodes: taskNodes, edges: taskEdges } = convertTaskX6ToInnerData(graphDataInput, tempComboId, x, y)
      nodes = taskNodes
      edges = taskEdges
      innerNodeCount = nodes.length
      comboInnerData.set(tempComboId, { nodes, edges })
    } else {
      const { nodes: oodaNodes, edges: oodaEdges } = convertOODAToInnerData(graphDataInput, tempComboId, x, y)
      nodes = oodaNodes
      edges = oodaEdges
      innerNodeCount = nodes.length
      comboInnerData.set(tempComboId, { nodes, edges })
    }
  } else if (nodeData.children && nodeData.children.length > 0) {
    const innerNodes = []
    const innerEdges = []
    const innerCombos = []

    const nodeShape = getNodeShapeForCombo(nodeData)
    const nodeColor = getNodeColorForCategory(category)

    const childNodes = []
    const childCombos = []

    nodeData.children.forEach((child) => {
      if (child.isCombo || child.children || child.graphData) {
        childCombos.push(child)
      } else {
        childNodes.push(child)
      }
    })

    const nodeWidth = 80
    const nodeHeight = 40
    const horizontalSpacing = nodeWidth + 20
    const verticalSpacing = nodeHeight + 20
    const columns = childNodes.length <= 2 ? childNodes.length : (childNodes.length <= 4 ? 2 : 3)
    const rows = Math.ceil(childNodes.length / columns)
    const totalWidth = (columns - 1) * horizontalSpacing
    const totalHeight = (rows - 1) * verticalSpacing
    const startX = x - totalWidth / 2
    const startY = y - totalHeight / 2

    childNodes.forEach((child, index) => {
      const col = index % columns
      const row = Math.floor(index / columns)
      const relativeX = startX + col * horizontalSpacing - x
      const relativeY = startY + row * verticalSpacing - y

      const childShape = child.type || child.originalData?.type || nodeShape

      innerNodes.push({
        id: child.id,
        label: child.name,
        type: 'node',
        category: category,
        comboId: tempComboId,
        x: x + relativeX,
        y: y + relativeY,
        relativeX: relativeX,
        relativeY: relativeY,
        size: [nodeWidth, nodeHeight],
        shape: childShape,
        nodeType: childShape,
        style: {
          fill: nodeColor,
          stroke: '#fff',
          lineWidth: 2,
          radius: childShape === 'circle' ? 20 : 6,
          cursor: 'pointer'
        },
        labelCfg: {
          position: 'bottom',
          offset: 0,
          style: {
            fill: '#fff',
            fontSize: 10,
            fontStyle: 'italic',
            background: {
              fill: 'rgba(0,0,0,0.6)',
              radius: 2,
              padding: [2, 4, 2, 4]
            }
          }
        },
        originalData: child.originalData || child
      })
    })

    if (childCombos.length > 0) {
      let maxNodeY = y
      innerNodes.forEach(node => {
        if (node.y + nodeHeight / 2 > maxNodeY) {
          maxNodeY = node.y + nodeHeight / 2
        }
      })

      childCombos.forEach((subChild, idx) => {
        const subComboWidth = 140
        const subComboHeight = 100
        const subX = x - subComboWidth / 2 + (idx % 2) * (subComboWidth + 20)
        const subY = maxNodeY + 40 + Math.floor(idx / 2) * (subComboHeight + 20)

        const subCategory = subChild.type || subChild.category || 'equipment'

        const subCombo = {
          id: subChild.id,
          label: subChild.name,
          type: 'combo',
          category: subCategory,
          collapsed: true,
          hasGraphData: !!(subChild.graphData),
          innerNodeCount: subChild.children?.length || 0,
          x: subX,
          y: subY,
          size: [subComboWidth, subComboHeight],
          style: {
            fill: `${getNodeColorForCategory(subCategory)}20`,
            stroke: getNodeColorForCategory(subCategory),
            lineWidth: 2,
            radius: 8,
            fillOpacity: 0.25
          },
          labelCfg: {
            style: {
              fill: getNodeColorForCategory(subCategory),
              fontSize: 12,
              fontWeight: 'bold',
              background: {
                fill: 'rgba(0,0,0,0.6)',
                radius: 4,
                padding: [2, 6, 2, 6]
              }
            },
            position: 'top',
            offset: 0,
            refY: -5
          },
          parentId: tempComboId,
          originalData: subChild,
          children: subChild.children || []
        }

        innerCombos.push(subCombo)

        if (subChild.children && subChild.children.length > 0) {
          const subInnerNodes = []
          const subNodeShape = getNodeShapeForCombo(subChild)
          const subNodeColor = getNodeColorForCategory(subCategory)

          const subNodeWidth = 70
          const subNodeHeight = 35
          const subColumns = subChild.children.length <= 2 ? subChild.children.length : 2

          subChild.children.forEach((grandChild, grandIdx) => {
            if (!grandChild.isCombo && !grandChild.graphData) {
              const col = grandIdx % subColumns
              const row = Math.floor(grandIdx / subColumns)
              const subRelativeX = -subNodeWidth / 2 + col * (subNodeWidth + 15)
              const subRelativeY = -subNodeHeight / 2 + row * (subNodeHeight + 15)

              const grandChildShape = grandChild.type || grandChild.originalData?.type || subNodeShape

              subInnerNodes.push({
                id: grandChild.id,
                label: grandChild.name,
                type: 'node',
                category: subCategory,
                comboId: subChild.id,
                relativeX: subRelativeX,
                relativeY: subRelativeY,
                size: [subNodeWidth, subNodeHeight],
                shape: grandChildShape,
                style: {
                  fill: subNodeColor,
                  stroke: '#fff',
                  lineWidth: 2,
                  radius: grandChildShape === 'circle' ? 18 : 4,
                  cursor: 'pointer'
                },
                labelCfg: {
                  position: 'bottom',
                  offset: 0,
                  style: {
                    fill: '#fff',
                    fontSize: 9,
                    fontStyle: 'italic',
                    background: {
                      fill: 'rgba(0,0,0,0.6)',
                      radius: 2,
                      padding: [1, 2, 1, 2]
                    }
                  }
                },
                originalData: grandChild.originalData || grandChild
              })
            }
          })

          comboInnerData.set(subChild.id, { nodes: subInnerNodes, edges: [], combos: [] })
        }
      })
    }

    innerNodeCount = innerNodes.length
    innerComboCount = innerCombos.length
    comboInnerData.set(tempComboId, { nodes: innerNodes, edges: innerEdges, combos: innerCombos })
  } else {
    comboInnerData.set(tempComboId, { nodes: [], edges: [] })
  }

  const totalInnerCount = innerNodeCount + innerComboCount
  const displayLabel = totalInnerCount > 0 ? `${nodeData.name}\n📦 ${totalInnerCount}` : nodeData.name

  let labelColor = '#409EFF'
  if (category === 'task') labelColor = '#F08F56'
  else if (category === 'ooda') labelColor = '#1783FF'
  else if (category === 'equipment') labelColor = '#52c41a'

  return {
    id: tempComboId,
    label: displayLabel,
    type: 'combo',
    category: category,
    collapsed: true,
    hasGraphData: hasGraphData || (nodeData.children && nodeData.children.length > 0),
    innerNodeCount: innerNodeCount,
    innerComboCount: innerComboCount,
    x: x,
    y: y,
    size: [160, 100],
    style: {
      fill: `${color}20`,
      stroke: color,
      lineWidth: 2.5,
      radius: 10,
      fillOpacity: 0.35,
      shadowColor: color,
      shadowBlur: 8
    },
    labelCfg: {
      style: {
        fill: labelColor,
        fontSize: 13,
        fontWeight: 'bold',
        background: {
          fill: 'rgba(0,0,0,0.7)',
          radius: 4,
          padding: [4, 8, 4, 8]
        }
      },
      position: 'top',
      offset: 0,
      refY: -5
    },
    originalData: nodeData.originalData || nodeData,
    children: nodeData.children || []
  }
}

// 添加节点到画布
const addComboToCanvas = (nodeData, category, hasGraphData = false, graphDataInput = null, customPosition = null) => {
  if (graphData.value.combos.find(c => c.id === nodeData.id)) {
    ElMessage.warning(`${nodeData.name} 已存在`)
    return
  }

  const position = customPosition || findEmptyPosition()
  const combo = createCombo(nodeData, category, hasGraphData, graphDataInput, position)
  graphData.value.combos.push(combo)
  comboCollapsedState.set(combo.id, true)

  const innerData = comboInnerData.get(combo.id)
  if (innerData && innerData.combos && innerData.combos.length > 0) {
    innerData.combos.forEach(subCombo => {
      if (!graphData.value.combos.find(c => c.id === subCombo.id)) {
        subCombo.parentId = combo.id
        graphData.value.combos.push(subCombo)
        comboCollapsedState.set(subCombo.id, true)
      }
    })
  }

  renderGraph()
  setTimeout(() => updateHulls(), 200)

  const innerCount = comboInnerData.get(combo.id)?.nodes?.length || 0
  const innerComboCount = comboInnerData.get(combo.id)?.combos?.length || 0
  ElMessage.success((hasGraphData && (innerCount > 0 || innerComboCount > 0)) || (nodeData.children && nodeData.children.length > 0) ?
    `已添加: ${nodeData.name} 📦 (包含 ${innerCount + innerComboCount} 个内部元素，双击可展开)` :
    `已添加: ${nodeData.name}`)
}

// 处理连线创建（允许任意连接）
const handleEdgeCreation = (targetItem, type) => {
  if (!edgeSourceItem) {
    edgeSourceItem = targetItem
    edgeSourceType = type
    const model = targetItem.getModel()
    edgeSourceName.value = model.label || model.id
    targetItem.setState('selected', true)

    const typeName = type === 'node' ? '节点' : 'Combo'
    ElMessage.info(`已选择源${typeName}: ${edgeSourceName.value}，请点击目标${typeName}`)
  } else {
    const sourceId = edgeSourceItem.getModel().id
    const targetId = targetItem.getModel().id
    const sourceName = edgeSourceItem.getModel().label || sourceId
    const targetName = targetItem.getModel().label || targetId

    if (sourceId !== targetId) {
      const edgeExists = graphData.value.edges.some(
        e => (e.source === sourceId && e.target === targetId) ||
          (e.source === targetId && e.target === sourceId)
      )

      if (!edgeExists) {
        const edgeId = `${sourceId}_to_${targetId}_${Date.now()}`

        let edgeLabel = ''
        if (edgeSourceType === 'node' && type === 'node') edgeLabel = '节点连接'
        else if (edgeSourceType === 'combo' && type === 'node') edgeLabel = '组合包含节点'
        else if (edgeSourceType === 'node' && type === 'combo') edgeLabel = '节点属于组合'
        else edgeLabel = '组合关联'

        const newEdge = {
          id: edgeId,
          source: sourceId,
          target: targetId,
          label: edgeLabel,
          style: {
            stroke: '#5F95FF',
            lineWidth: 2,
            endArrow: {
              path: 'M 0,0 L 8,4 L 8,-4 Z',
              fill: '#5F95FF'
            }
          },
          labelCfg: {
            style: {
              fill: '#1890ff',
              fontSize: 12,
              background: {
                fill: 'rgba(255,255,255,0.8)',
                radius: 2,
                padding: [2, 4, 2, 4]
              }
            },
            position: 'center',
            autoRotate: true
          }
        }

        graphData.value.edges.push(newEdge)
        renderGraph()
        ElMessage.success(`连线创建成功: ${sourceName} → ${targetName}`)
      } else {
        ElMessage.warning('连线已存在')
      }
    } else {
      ElMessage.warning('不能连接同一个节点/Combo')
    }

    edgeSourceItem.setState('selected', false)
    edgeSourceItem = null
    edgeSourceType = null
    edgeSourceName.value = ''
    currentEdgeTool.value = 'none'
    clearAllSelected()
    ElMessage.info('连线完成，已退出连线模式')
  }
}

// 编辑边信息
const openEdgeEditDialog = (edge) => {
  editingEdge.value = edge
  edgeEditForm.value = {
    label: edge.label || '',
    stroke: edge.style?.stroke || '#5F95FF',
    lineWidth: edge.style?.lineWidth || 2,
    lineDash: edge.style?.lineDash || []
  }
  edgeEditDialogVisible.value = true
  closeContextMenu()
}

// 保存边编辑
const saveEdgeEdit = () => {
  if (!editingEdge.value) return

  const edgeIndex = graphData.value.edges.findIndex(e => e.id === editingEdge.value.id)
  if (edgeIndex !== -1) {
    graphData.value.edges[edgeIndex] = {
      ...graphData.value.edges[edgeIndex],
      label: edgeEditForm.value.label,
      style: {
        ...graphData.value.edges[edgeIndex].style,
        stroke: edgeEditForm.value.stroke,
        lineWidth: edgeEditForm.value.lineWidth,
        lineDash: edgeEditForm.value.lineDash
      },
      labelCfg: {
        ...graphData.value.edges[edgeIndex].labelCfg,
        style: {
          ...graphData.value.edges[edgeIndex].labelCfg?.style,
          fill: edgeEditForm.value.stroke
        }
      }
    }
  }

  renderGraph()
  edgeEditDialogVisible.value = false
  ElMessage.success('连线信息已更新')
}

// 删除边
const deleteEdge = () => {
  if (!contextMenuEdge) return

  ElMessageBox.confirm('确定删除这条连线吗？', '提示', { type: 'warning' }).then(() => {
    const edgeIndex = graphData.value.edges.findIndex(e => e.id === contextMenuEdge.id)
    if (edgeIndex !== -1) {
      graphData.value.edges.splice(edgeIndex, 1)
      renderGraph()
      ElMessage.success('连线已删除')
    }
    closeContextMenu()
  }).catch(() => { })
}

// 编辑节点信息
const openNodeEditDialog = (node) => {
  editingNode.value = node
  nodeEditForm.value = {
    label: node.label || '',
    fill: node.style?.fill || '#409EFF',
    stroke: node.style?.stroke || '#337ecc',
    size: node.size || [100, 50],
    shape: node.shape || 'rect'
  }
  nodeEditDialogVisible.value = true
  closeContextMenu()
}

// 保存节点编辑
const saveNodeEdit = () => {
  if (!editingNode.value) return

  const nodeIndex = graphData.value.nodes.findIndex(n => n.id === editingNode.value.id)
  if (nodeIndex !== -1) {
    let radius = 6
    if (nodeEditForm.value.shape === 'circle') {
      radius = Math.min(nodeEditForm.value.size[0], nodeEditForm.value.size[1]) / 2
    }

    graphData.value.nodes[nodeIndex] = {
      ...graphData.value.nodes[nodeIndex],
      label: nodeEditForm.value.label,
      shape: nodeEditForm.value.shape,
      size: nodeEditForm.value.size,
      style: {
        ...graphData.value.nodes[nodeIndex].style,
        fill: nodeEditForm.value.fill,
        stroke: nodeEditForm.value.stroke,
        radius: radius
      },
      labelCfg: {
        ...graphData.value.nodes[nodeIndex].labelCfg,
        style: {
          ...graphData.value.nodes[nodeIndex].labelCfg?.style,
          fill: nodeEditForm.value.fill
        }
      }
    }
  }

  renderGraph()
  nodeEditDialogVisible.value = false
  ElMessage.success('节点信息已更新')
}

// 删除节点
const deleteNode = () => {
  if (!contextMenuTarget) return

  ElMessageBox.confirm('确定删除这个节点吗？相关的连线也会被删除！', '提示', { type: 'warning' }).then(() => {
    const nodeIndex = graphData.value.nodes.findIndex(n => n.id === contextMenuTarget.id)
    if (nodeIndex !== -1) {
      graphData.value.nodes.splice(nodeIndex, 1)
    }

    graphData.value.edges = graphData.value.edges.filter(e =>
      e.source !== contextMenuTarget.id && e.target !== contextMenuTarget.id
    )

    renderGraph()
    ElMessage.success('节点已删除')
    closeContextMenu()
  }).catch(() => { })
}

// 删除 Combo
const deleteCombo = () => {
  if (!contextMenuTarget) return

  ElMessageBox.confirm('确定删除这个组合吗？组合内的所有节点和连线也会被删除！', '提示', { type: 'warning' }).then(() => {
    const comboId = contextMenuTarget.id

    const deleteComboRecursive = (id) => {
      const innerData = comboInnerData.get(id)
      if (innerData) {
        if (innerData.nodes) {
          innerData.nodes.forEach(node => {
            const nodeIndex = graphData.value.nodes.findIndex(n => n.id === node.id)
            if (nodeIndex !== -1) graphData.value.nodes.splice(nodeIndex, 1)
          })
        }
        if (innerData.combos) {
          innerData.combos.forEach(subCombo => {
            deleteComboRecursive(subCombo.id)
          })
        }
        comboInnerData.delete(id)
      }
    }

    deleteComboRecursive(comboId)

    const comboIndex = graphData.value.combos.findIndex(c => c.id === comboId)
    if (comboIndex !== -1) graphData.value.combos.splice(comboIndex, 1)

    graphData.value.edges = graphData.value.edges.filter(e =>
      e.source !== comboId && e.target !== comboId
    )

    comboCollapsedState.delete(comboId)
    renderGraph()
    ElMessage.success('组合已删除')
    closeContextMenu()
  }).catch(() => { })
}

// 恢复所有combo的折叠状态
const restoreCollapsedStates = () => {
  if (!graph) return
  comboCollapsedState.forEach((isCollapsed, comboId) => {
    try {
      const combo = graph.findById(comboId)
      if (combo) {
        const currentCollapsed = combo.getModel().collapsed
        if (currentCollapsed !== isCollapsed) {
          graph.collapseExpandCombo(comboId, isCollapsed)
        }
      }
    } catch (e) {
      console.warn(`恢复combo ${comboId} 状态失败:`, e)
    }
  })
}

// 绘制自定义Hull
let hullUpdateTimer = null
const drawCustomHull = () => {
  if (!graph || !canvasContainer.value) return

  const containerRect = canvasContainer.value.getBoundingClientRect()
  const getScreenPos = (modelX, modelY) => {
    const point = graph.getClientByPoint(modelX, modelY)
    if (!point) return null
    return {
      x: point.x - containerRect.left,
      y: point.y - containerRect.top
    }
  }

  const taskPositions = []
  const oodaPositions = []
  const equipmentPositions = []

  graphData.value.combos.forEach(combo => {
    if (combo.parentId) return

    const pos = getScreenPos(combo.x, combo.y)
    if (pos) {
      if (combo.category === 'task') taskPositions.push(pos)
      else if (combo.category === 'ooda') oodaPositions.push(pos)
      else if (combo.category === 'equipment') equipmentPositions.push(pos)
    }
  })

  const getConvexHull = (points) => {
    if (points.length < 3) return points
    const cross = (o, a, b) => (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)
    const sorted = points.slice().sort((a, b) => a.x !== b.x ? a.x - b.x : a.y - b.y)
    const lower = []
    for (let i = 0; i < sorted.length; i++) {
      while (lower.length >= 2 && cross(lower[lower.length - 2], lower[lower.length - 1], sorted[i]) <= 0) lower.pop()
      lower.push(sorted[i])
    }
    const upper = []
    for (let i = sorted.length - 1; i >= 0; i--) {
      while (upper.length >= 2 && cross(upper[upper.length - 2], upper[upper.length - 1], sorted[i]) <= 0) upper.pop()
      upper.push(sorted[i])
    }
    lower.pop()
    upper.pop()
    return lower.concat(upper)
  }

  const existingHulls = document.querySelectorAll('.g6-hull-svg-layer')
  existingHulls.forEach(hull => hull.remove())

  const hullContainer = document.createElement('div')
  hullContainer.className = 'g6-hull-svg-layer'
  hullContainer.style.position = 'absolute'
  hullContainer.style.top = '0'
  hullContainer.style.left = '0'
  hullContainer.style.width = '100%'
  hullContainer.style.height = '100%'
  hullContainer.style.pointerEvents = 'none'
  hullContainer.style.zIndex = '0'

  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
  svg.style.width = '100%'
  svg.style.height = '100%'
  svg.style.position = 'absolute'
  svg.style.top = '0'
  svg.style.left = '0'
  svg.style.pointerEvents = 'none'
  hullContainer.appendChild(svg)
  canvasContainer.value.appendChild(hullContainer)

  const hulls = [
    { points: taskPositions, color: '#F08F56', label: '任务网络', icon: '📋' },
    { points: oodaPositions, color: '#1783FF', label: 'OODA网络', icon: '🔄' },
    { points: equipmentPositions, color: '#00C9C9', label: '装备网络', icon: '🔧' }
  ]

  hulls.forEach(hull => {
    if (hull.points.length === 0) return
    const convexHull = getConvexHull(hull.points)
    if (convexHull.length < 3) return

    const padding = 60
    const pointsForPath = convexHull.map(p => ({ x: p.x - padding, y: p.y - padding }))
    const pathData = pointsForPath.map((p, i) => (i === 0 ? `M ${p.x} ${p.y}` : `L ${p.x} ${p.y}`)).join(' ') + ' Z'

    const polygon = document.createElementNS('http://www.w3.org/2000/svg', 'path')
    polygon.setAttribute('d', pathData)
    polygon.setAttribute('fill', hull.color)
    polygon.setAttribute('fill-opacity', '0.15')
    polygon.setAttribute('stroke', hull.color)
    polygon.setAttribute('stroke-width', '2')
    polygon.setAttribute('stroke-dasharray', '6 4')
    svg.appendChild(polygon)

    let minX = Infinity, minY = Infinity
    pointsForPath.forEach(p => {
      minX = Math.min(minX, p.x)
      minY = Math.min(minY, p.y)
    })
    const labelX = minX
    const labelY = minY - 20

    const labelBg = document.createElementNS('http://www.w3.org/2000/svg', 'rect')
    labelBg.setAttribute('x', labelX - 8)
    labelBg.setAttribute('y', labelY - 12)
    labelBg.setAttribute('width', '140')
    labelBg.setAttribute('height', '26')
    labelBg.setAttribute('rx', '6')
    labelBg.setAttribute('fill', 'rgba(0,0,0,0.8)')
    labelBg.setAttribute('stroke', hull.color)
    labelBg.setAttribute('stroke-width', '1')
    svg.appendChild(labelBg)

    const labelText = document.createElementNS('http://www.w3.org/2000/svg', 'text')
    labelText.setAttribute('x', labelX)
    labelText.setAttribute('y', labelY)
    labelText.setAttribute('fill', hull.color)
    labelText.setAttribute('font-size', '13')
    labelText.setAttribute('font-weight', 'bold')
    labelText.setAttribute('dominant-baseline', 'middle')
    labelText.textContent = `${hull.icon} ${hull.label} (${hull.points.length})`
    svg.appendChild(labelText)
  })
}

const updateHulls = () => {
  if (!graph) return
  if (hullUpdateTimer) cancelAnimationFrame(hullUpdateTimer)
  hullUpdateTimer = requestAnimationFrame(() => {
    try {
      drawCustomHull()
    } catch (error) {
      console.warn('绘制Hull失败:', error)
    }
    hullUpdateTimer = null
  })
}

// 渲染图形
const renderGraph = () => {
  if (!graph) return

  // 保存当前视图状态
  saveCurrentView()

  const data = {
    nodes: graphData.value.nodes,
    edges: graphData.value.edges,
    combos: graphData.value.combos
  }

  graph.data(data)
  graph.render()

  setTimeout(() => {
    graphData.value.combos.forEach(combo => {
      const shouldBeCollapsed = comboCollapsedState.get(combo.id) ?? combo.collapsed
      if (shouldBeCollapsed) {
        try {
          graph.collapseExpandCombo(combo.id, true)
        } catch (e) {
          console.warn(`折叠 combo ${combo.id} 失败:`, e)
        }
      }
    })

    // 恢复视图状态
    restoreCurrentView()

    setTimeout(() => updateHulls(), 300)
  }, 150)
}

const handleResize = () => {
  if (graph && canvasContainer.value) {
    graph.changeSize(canvasContainer.value.clientWidth, canvasContainer.value.clientHeight)
    setTimeout(() => updateHulls(), 200)
  }
}

const enableLassoSelect = () => {
  lassoEnabled.value = !lassoEnabled.value
  if (graph) {
    if (lassoEnabled.value) {
      // 移除冲突行为
      graph.removeBehaviors(['drag-canvas', 'drag-node', 'drag-combo'], 'default')
      
      // 添加刷选行为（矩形框选）
      graph.addBehaviors([{
        type: 'brush-select',
        key: 'brush-select',
        brushStyle: {
          fill: 'rgba(64, 158, 255, 0.2)',
          stroke: '#409EFF',
          lineWidth: 2,
          lineDash: [4, 4]
        },
        selectedState: 'selected',
        trigger: 'drag',
        onSelect: (nodes, edges) => {
          console.log('框选选中节点:', nodes?.length || 0)
        }
      }], 'default')
      
      ElMessage.success('框选模式已开启，按住鼠标拖拽框选节点/Combo')
    } else {
      graph.removeBehaviors(['brush-select'], 'default')
      graph.addBehaviors(['drag-canvas', 'zoom-canvas', 'drag-node', 'drag-combo', 'click-select'], 'default')
      clearAllSelected()
      ElMessage.info('框选模式已关闭')
    }
  }
}

// 设置工具
const setTool = (tool) => { currentTool.value = tool }

// 从左侧树点击添加节点
const handleNodeClick = (data) => {
  if (!isBuildMode.value) return

  const allowedTypes = ['equipment', 'equipmentGroup', 'oodaNode', 'network', 'task']
  if (!allowedTypes.includes(data.type)) return

  let category = 'ooda'
  console.log("加载到画布节点》》", data)

  if (data.type === 'task' || data.type === 'task-network') {
    category = 'task'
  } else if (data.type === 'equipment' || data.type === 'equipmentGroup') {
    category = 'equipment'
  }

  let hasGraphData = false
  let graphDataInput = null

  if (category === 'task' && data.originalData?.subtaskDesc) {
    try {
      graphDataInput = typeof data.originalData.subtaskDesc === 'string'
        ? JSON.parse(data.originalData.subtaskDesc)
        : data.originalData.subtaskDesc
      hasGraphData = !!(graphDataInput && graphDataInput.cells && graphDataInput.cells.length > 0)
      console.log('解析任务网络数据成功:', graphDataInput)
    } catch (error) {
      console.error('解析subtaskDesc失败:', error)
    }
  }
  else if (category === 'equipment' && data.originalData?.graphJSONEquipmentNetwork) {
    try {
      graphDataInput = typeof data.originalData.graphJSONEquipmentNetwork === 'string'
        ? JSON.parse(data.originalData.graphJSONEquipmentNetwork)
        : data.originalData.graphJSONEquipmentNetwork
      hasGraphData = !!(graphDataInput && (graphDataInput.nodes?.length > 0 || graphDataInput.combos?.length > 0))
    } catch (error) {
      console.error('解析graphJSONEquipmentNetwork失败:', error)
    }
  }
  else if (category === 'ooda' && data.originalData?.graphJSONOODANetwork) {
    try {
      graphDataInput = typeof data.originalData.graphJSONOODANetwork === 'string'
        ? JSON.parse(data.originalData.graphJSONOODANetwork)
        : data.originalData.graphJSONOODANetwork
      hasGraphData = !!(graphDataInput && (graphDataInput.cells?.length > 0 || graphDataInput.nodes?.length > 0))
      console.log('解析OODA网络数据成功（保持连接关系）:', graphDataInput)
    } catch (error) {
      console.error('解析graphJSONOODANetwork失败:', error)
    }
  }

  const comboData = {
    id: data.id,
    name: data.name,
    type: data.type,
    children: data.children || [],
    originalData: data.originalData
  }

  addComboToCanvas(comboData, category, hasGraphData, graphDataInput)
}

// 保存
const handleSave = async () => {
  try {
    const networkIds = [...new Set([
      ...graphData.value.nodes.map(node => node.networkId).filter(Boolean),
      ...graphData.value.combos.map(combo => combo.networkId).filter(Boolean)
    ])]

    const connections = graphData.value.edges.map(edge => {
      const sourceNode = graphData.value.nodes.find(n => n.id === edge.source) ||
        graphData.value.combos.find(c => c.id === edge.source)
      const targetNode = graphData.value.nodes.find(n => n.id === edge.target) ||
        graphData.value.combos.find(c => c.id === edge.target)

      return {
        name: `REL_${Date.now()}_${edge.id}`,
        networkIdList: networkIds,
        originalNodeId: edge.source,
        originalNetworkId: sourceNode?.networkId || 'NET_1',
        targetNodeId: edge.target,
        targetNetworkId: targetNode?.networkId || 'NET_2',
        pgTaskId: 'TASK_1'
      }
    })

    if (connections.length === 0) {
      const saveData = {
        name: `REL_${Date.now()}`,
        networkIdList: networkIds.length > 0 ? networkIds : ['NET_1', 'NET_2'],
        originalNodeId: 'ON_1',
        originalNetworkId: 'NET_1',
        targetNodeId: 'TN_1',
        targetNetworkId: 'NET_2',
        pgTaskId: 'TASK_1'
      }

      console.log('保存数据:', saveData)
      const result = await insertMappingRelation(saveData)
      console.log('API响应:', result)
      ElMessage.success('保存成功')
    } else {
      const savePromises = connections.map(connection => insertMappingRelation(connection))
      const results = await Promise.all(savePromises)
      console.log('所有连接保存完成:', results)
      ElMessage.success(`成功保存 ${connections.length} 个连接关系`)
    }

  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}

// 清空
const handleClear = async () => {
  try {
    await ElMessageBox.confirm('确定清空画布？', '提示', { type: 'warning' })
    graphData.value = { nodes: [], edges: [], combos: [] }
    comboInnerData.clear()
    comboCollapsedState.clear()
    renderGraph()
    setTimeout(() => updateHulls(), 100)
    ElMessage.success('已清空')
  } catch { }
}

// 构建模式
const handleBuildMode = () => {
  isBuildMode.value = true
  if (lassoEnabled.value) enableLassoSelect()
  if (currentEdgeTool.value !== 'none') setEdgeMode()
}

// 预览模式
const handlePreviewMode = () => {
  isBuildMode.value = false
  if (lassoEnabled.value) enableLassoSelect()
  if (currentEdgeTool.value !== 'none') setEdgeMode()
}

// 加载树形数据
const loadTreeData = async () => {
  console.log('树形数据加载由NetworkTree组件统一处理')
}

// 初始化 G6 画布
const initG6 = () => {
  nextTick(() => {
    if (!canvasContainer.value) return

    const container = canvasContainer.value
    const width = container.clientWidth
    const height = container.clientHeight

    graph = new Graph({
      container: container,
      width: width,
      height: height,
      modes: {
        default: ['drag-canvas', 'zoom-canvas', 'drag-node', 'drag-combo', 'click-select'],
      },
      fitView: false,
      fitCenter: false,
      // 设置初始缩放比例
      zoom: 0.8,
      defaultNode: {
        size: [100, 50],
        style: {
          fill: '#409EFF',
          stroke: '#337ecc',
          lineWidth: 2,
          radius: 6,
          cursor: 'pointer'
        },
        labelCfg: {
          style: {
            fill: '#e66465',
            fontSize: 12,
            fontStyle: 'italic',
            fontWeight: 'normal',
            textAlign: 'center',
            textBaseline: 'top',
            background: {
              fill: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
              radius: 4,
              padding: [4, 8, 4, 8]
            }
          },
          position: 'bottom',
          offset: 10
        },
        stateStyles: {
          selected: {
            stroke: '#FFD700',
            lineWidth: 3,
            shadowBlur: 10,
            shadowColor: '#FFD700'
          },
          hover: {
            stroke: '#FFD700',
            lineWidth: 2,
            shadowBlur: 5,
            shadowColor: '#FFD700'
          }
        }
      },
      defaultEdge: {
        type: 'line',
        style: {
          stroke: '#5F95FF',
          lineWidth: 2,
          endArrow: {
            path: 'M 0,0 L 8,4 L 8,-4 Z',
            fill: '#5F95FF'
          }
        },
        labelCfg: {
          style: {
            fill: '#1890ff',
            fontSize: 12,
            fontWeight: 'bold',
            textAlign: 'center',
            textBaseline: 'top',
            background: {
              fill: 'linear-gradient(120deg, rgba(24,144,255,0.2), rgba(24,144,255,0.05))',
              radius: 4,
              padding: [4, 8, 4, 8]
            }
          },
          position: 'center',
          offset: 5,
          autoRotate: true
        }
      },
      defaultCombo: {
        type: 'rect',
        draggable: true,
        collapseExpand: true,
        style: {
          lineWidth: 2,
          radius: 8,
          cursor: 'move'
        },
        labelCfg: {
          style: (model) => {
            let color = '#409EFF'
            if (model.category === 'task') color = '#F08F56'
            else if (model.category === 'ooda') color = '#1783FF'
            else if (model.category === 'equipment') color = '#00C9C9'

            return {
              fill: color,
              fontSize: 13,
              fontWeight: 'bold',
              textAlign: 'center',
              background: {
                fill: 'rgba(0,0,0,0.7)',
                radius: 4,
                padding: [4, 8, 4, 8]
              }
            }
          },
          position: 'top',
          offset: -5
        },
        collapsible: true,
        stateStyles: {
          selected: {
            stroke: '#FFD700',
            lineWidth: 3,
            shadowBlur: 10,
            shadowColor: '#FFD700'
          },
          hover: {
            stroke: '#FFD700',
            lineWidth: 2,
            shadowBlur: 5,
            shadowColor: '#FFD700'
          }
        }
      }
    })

    window.addEventListener('resize', handleResize)

    graph.on('afterlayout', () => {
      restoreCollapsedStates()
      updateHulls()
    })

    graph.on('afterupdateitem', () => {
      restoreCollapsedStates()
    })

    graph.on('viewportchange', () => {
      updateHulls()
    })

    graph.on('node:dragend', () => {
      setTimeout(() => updateHulls(), 50)
    })

    graph.on('combo:dragend', () => {
      setTimeout(() => updateHulls(), 50)
    })

    graph.on('edge:contextmenu', (e) => {
      e.preventDefault()
      const edge = e.item.getModel()
      contextMenuEdge = edge
      edgeContextMenuX.value = e.clientX
      edgeContextMenuY.value = e.clientY
      edgeContextMenuVisible.value = true
    })

    graph.on('node:contextmenu', (e) => {
      e.preventDefault()
      const model = e.item.getModel()
      contextMenuTarget = model
      contextMenuX.value = e.clientX
      contextMenuY.value = e.clientY
      contextMenuVisible.value = true
    })

    graph.on('combo:contextmenu', (e) => {
      e.preventDefault()
      const model = e.item.getModel()
      contextMenuTarget = model
      contextMenuX.value = e.clientX
      contextMenuY.value = e.clientY
      contextMenuVisible.value = true
    })

    graph.on('combo:dblclick', (e) => {
      if (currentEdgeTool.value !== 'none') return

      const comboItem = e.item
      const comboId = comboItem.getModel().id
      const comboModel = graphData.value.combos.find(c => c.id === comboId)
      if (!comboModel) return

      const isSubCombo = !!comboModel.parentId
      const currentCollapsed = comboCollapsedState.has(comboId) ? comboCollapsedState.get(comboId) : comboItem.getModel().collapsed

      if (currentCollapsed) {
        let innerData = comboInnerData.get(comboId)

        if (isSubCombo) {
          if (!innerData && comboModel.children && comboModel.children.length > 0) {
            const innerNodes = []
            const innerEdges = []
            const nodeShape = getNodeShapeForCombo(comboModel)
            const nodeColor = getNodeColorForCategory(comboModel.category)

            const nodeCount = comboModel.children.length
            let columns = nodeCount <= 3 ? nodeCount : 3

            comboModel.children.forEach((child, index) => {
              const relativeX = -60 + (index % columns) * 60
              const relativeY = -30 + Math.floor(index / columns) * 50

              innerNodes.push({
                id: child.id,
                label: child.name,
                type: 'node',
                category: comboModel.category,
                comboId: comboId,
                x: comboModel.x + relativeX,
                y: comboModel.y + relativeY,
                size: [60, 30],
                shape: nodeShape,
                nodeType: nodeShape,
                style: {
                  fill: nodeColor,
                  stroke: '#fff',
                  lineWidth: 2,
                  radius: nodeShape === 'circle' ? 15 : 4,
                  cursor: 'pointer'
                },
                labelCfg: {
                  position: 'bottom',
                  offset: 0,
                  style: {
                    fill: '#fff',
                    fontSize: 8,
                    fontStyle: 'italic',
                    background: {
                      fill: 'rgba(0,0,0,0.6)',
                      radius: 2,
                      padding: [1, 2, 1, 2]
                    }
                  }
                },
                originalData: child.originalData || child
              })
            })

            innerData = { nodes: innerNodes, edges: innerEdges }
            comboInnerData.set(comboId, innerData)
          }
        }

        if (!isSubCombo && !innerData && comboModel.children && comboModel.children.length > 0) {
          const innerNodes = []
          const innerEdges = []
          const nodeShape = getNodeShapeForCombo(comboModel)
          const nodeColor = getNodeColorForCategory(comboModel.category)

          const nodeCount = comboModel.children.length
          let columns = 3

          if (nodeCount <= 2) {
            columns = nodeCount
          } else if (nodeCount <= 6) {
            columns = 3
          } else {
            columns = 4
          }

          comboModel.children.forEach((child, index) => {
            const relativeX = -80 + (index % columns) * 80
            const relativeY = -40 + Math.floor(index / columns) * 60

            innerNodes.push({
              id: child.id,
              label: child.name,
              type: 'node',
              category: comboModel.category,
              comboId: comboId,
              x: comboModel.x + relativeX,
              y: comboModel.y + relativeY,
              size: [80, 40],
              shape: nodeShape,
              nodeType: nodeShape,
              style: {
                fill: nodeColor,
                stroke: '#fff',
                lineWidth: 2,
                radius: nodeShape === 'circle' ? 20 : 6,
                cursor: 'pointer'
              },
              labelCfg: {
                position: 'bottom',
                offset: 0,
                style: {
                  fill: '#fff',
                  fontSize: 10,
                  fontStyle: 'italic',
                  background: {
                    fill: 'rgba(0,0,0,0.6)',
                    radius: 2,
                    padding: [2, 4, 2, 4]
                  }
                }
              },
              originalData: child.originalData || child
            })
          })

          innerData = { nodes: innerNodes, edges: innerEdges }
          comboInnerData.set(comboId, innerData)
        }

        if (innerData && (innerData.nodes?.length > 0 || innerData.combos?.length > 0)) {
          if (innerData.nodes) {
            innerData.nodes.forEach(node => {
              if (!graphData.value.nodes.find(n => n.id === node.id)) {
                const nodeShape = node.shape || node.originalData?.type || 'rect'
                let radius = 6
                if (nodeShape === 'circle') radius = 20
                else if (nodeShape === 'diamond') radius = 6
                else if (nodeShape === 'triangle') radius = 6
                else if (nodeShape === 'rect') radius = 6

                const nodeWithCorrectShape = {
                  ...node,
                  shape: nodeShape,
                  type: nodeShape,
                  style: {
                    ...node.style,
                    radius: radius
                  }
                }

                graphData.value.nodes.push(nodeWithCorrectShape)
              }
            })
          }

          if (innerData.edges) {
            innerData.edges.forEach(edge => {
              if (!graphData.value.edges.find(e => e.id === edge.id)) {
                graphData.value.edges.push(edge)
              }
            })
          }

          if (innerData.combos && innerData.combos.length > 0) {
            innerData.combos.forEach(subCombo => {
              if (!graphData.value.combos.find(c => c.id === subCombo.id)) {
                graphData.value.combos.push(subCombo)
                comboCollapsedState.set(subCombo.id, true)
              }
            })
          }

          // 更新 combo 大小
          if (innerData.nodes && innerData.nodes.length > 0) {
            let minX = Infinity, minY = Infinity
            let maxX = -Infinity, maxY = -Infinity

            innerData.nodes.forEach(node => {
              minX = Math.min(minX, node.x)
              minY = Math.min(minY, node.y)
              maxX = Math.max(maxX, node.x + (node.size?.[0] || 80))
              maxY = Math.max(maxY, node.y + (node.size?.[1] || 40))
            })

            if (minX !== Infinity) {
              const padding = 50
              const newWidth = maxX - minX + padding * 2
              const newHeight = maxY - minY + padding * 2

              const comboIndex = graphData.value.combos.findIndex(c => c.id === comboId)
              if (comboIndex !== -1) {
                graphData.value.combos[comboIndex].size = [Math.max(newWidth, 160), Math.max(newHeight, 120)]
              }
            }
          }

          renderGraph()

          setTimeout(() => {
            comboCollapsedState.set(comboId, false)
            try {
              graph.collapseExpandCombo(comboId, false)
            } catch (err) {
              console.warn('展开 Combo 失败:', err)
            }
            const totalCount = (innerData.nodes?.length || 0) + (innerData.combos?.length || 0)
            ElMessage.success(`已展开: ${comboModel.label?.split('\n')[0] || comboModel.label}，包含 ${totalCount} 个内部元素`)
            setTimeout(() => updateHulls(), 200)
          }, 100)
        } else {
          comboCollapsedState.set(comboId, false)
          try {
            graph.collapseExpandCombo(comboId, false)
          } catch (err) {
            console.warn('展开 Combo 失败:', err)
          }
          ElMessage.info(`已展开: ${comboModel.label?.split('\n')[0] || comboModel.label}（无内部元素）`)
          setTimeout(() => updateHulls(), 200)
        }
      } else {
        const innerData = comboInnerData.get(comboId)
        if (innerData) {
          if (innerData.nodes) {
            innerData.nodes.forEach(node => {
              const index = graphData.value.nodes.findIndex(n => n.id === node.id)
              if (index !== -1) {
                graphData.value.nodes.splice(index, 1)
              }
            })
          }

          if (innerData.edges) {
            innerData.edges.forEach(edge => {
              const index = graphData.value.edges.findIndex(e => e.id === edge.id)
              if (index !== -1) {
                graphData.value.edges.splice(index, 1)
              }
            })
          }

          if (innerData.combos) {
            innerData.combos.forEach(subCombo => {
              const index = graphData.value.combos.findIndex(c => c.id === subCombo.id)
              if (index !== -1) {
                graphData.value.combos.splice(index, 1)
                comboCollapsedState.delete(subCombo.id)
              }
            })
          }

          renderGraph()
        }

        comboCollapsedState.set(comboId, true)
        try {
          graph.collapseExpandCombo(comboId, true)
        } catch (err) {
          console.warn('折叠 Combo 失败:', err)
        }
        ElMessage.info(`已折叠: ${comboModel.label?.split('\n')[0] || comboModel.label}`)
        setTimeout(() => updateHulls(), 200)
      }

      setTimeout(() => {
        if (graph) {
          graph.refresh()
        }
      }, 300)
    })

    graph.on('node:click', (e) => {
      if (currentEdgeTool.value !== 'none') {
        e.item.setState('selected', true)
        handleEdgeCreation(e.item, 'node')
      }
    })

    graph.on('combo:click', (e) => {
      if (currentEdgeTool.value !== 'none') {
        e.item.setState('selected', true)
        handleEdgeCreation(e.item, 'combo')
      }
    })

    graph.on('canvas:click', () => {
      closeContextMenu()
      clearAllSelected()
    })

    renderGraph()
  })
}


/**
 * 防抖
 */


// 保存当前视图状态
const saveCurrentView = () => {
  if (!graph) return
  try {
    currentZoom = graph.getZoom()
    currentMatrix = graph.getGroup().getMatrix()
  } catch (e) {
    console.warn('保存视图状态失败:', e)
  }
}

// 恢复视图状态
const restoreCurrentView = () => {
  if (!graph) return
  try {
    if (currentMatrix) {
      graph.getGroup().setMatrix(currentMatrix)
    }
    graph.setZoom(currentZoom)
  } catch (e) {
    console.warn('恢复视图状态失败:', e)
  }
}

// 生命周期
onMounted(() => {
  loadTreeData()
  initG6()
  document.addEventListener('click', onDocumentClick)
})

onUnmounted(() => {
  if (graph) {
    graph.destroy()
    graph = null
  }
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('click', onDocumentClick)
})
</script>

<style lang="scss" scoped>
.super-network-build {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #0f0f12;
  color: #e1e1e6;

  // 顶部操作栏
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 24px;
    background: rgba(20, 20, 25, 0.95);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    z-index: 10;

    .header-left {
      display: flex;
      align-items: center;
      gap: 16px;

      .mode-group,
      .action-group,
      .tool-group {
        .el-button {
          background: transparent;
          border-color: rgba(255, 255, 255, 0.15);
          color: #a1a1aa;
          transition: all 0.2s ease;

          &:hover {
            background: rgba(64, 158, 255, 0.1);
            border-color: #409eff;
            color: #409eff;
          }

          &.is-primary {
            background: #409eff;
            border-color: #409eff;
            color: #fff;
          }
        }
      }

      .el-divider {
        height: 24px;
        margin: 0;
        background: rgba(255, 255, 255, 0.15);
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 16px;

      .mode-badge {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 6px 12px;
        background: rgba(255, 255, 255, 0.05);
        border-radius: 20px;
        font-size: 13px;

        .dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #909399;
          transition: all 0.2s ease;

          &.active {
            background: #67c23a;
            box-shadow: 0 0 6px #67c23a;
          }
        }
      }

      .edge-badge,
      .lasso-badge {
        :deep(.el-badge__content) {
          background: #409eff;
          border: none;
          font-size: 11px;
          padding: 0 6px;
        }
      }
    }
  }

  // 主内容区
  .main-content {
    flex: 1;
    display: flex;
    overflow: hidden;

    .left-panel {
      width: 320px;
      background: rgba(30, 30, 35, 0.6);
      border-right: 1px solid rgba(255, 255, 255, 0.06);
      overflow-y: auto;
      backdrop-filter: blur(5px);

      &::-webkit-scrollbar {
        width: 6px;
      }

      &::-webkit-scrollbar-track {
        background: transparent;
      }

      &::-webkit-scrollbar-thumb {
        background: rgba(255, 255, 255, 0.2);
        border-radius: 3px;
      }
    }

    .right-panel {
      flex: 1;
      display: flex;
      flex-direction: column;
      position: relative;

      .canvas-container {
        flex: 1;
        background: #0a0a0f;
        margin: 0;
        overflow: hidden;
        position: relative;

        :deep(canvas) {
          display: block;
          cursor: default;
        }

        // 框选时的光标样式
        &:active {
          cursor: crosshair;
        }

        // 禁用滚动条导致的抖动
        overflow: hidden !important;
      }

      :deep(.g6-container) {
        overflow: visible !important;
      }

      :deep(.g6-graph-container) {
        overflow: visible !important;
      }

      .debug-panel {
        position: absolute;
        bottom: 16px;
        left: 16px;
        background: rgba(0, 0, 0, 0.8);
        backdrop-filter: blur(8px);
        border-radius: 12px;
        padding: 12px 16px;
        min-width: 240px;
        z-index: 20;
        font-size: 12px;
        border: 1px solid rgba(255, 255, 255, 0.1);

        .debug-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;
          padding-bottom: 8px;
          border-bottom: 1px solid rgba(255, 255, 255, 0.1);
          font-weight: 500;
        }

        .debug-content {
          line-height: 1.8;
          color: #a1a1aa;
        }
      }

      .debug-btn {
        position: absolute;
        bottom: 16px;
        right: 16px;
        z-index: 20;
        background: rgba(0, 0, 0, 0.6);
        backdrop-filter: blur(8px);
        border-radius: 8px;
        color: #a1a1aa;
        width: 32px;
        height: 32px;
        padding: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        border: 1px solid rgba(255, 255, 255, 0.1);

        &:hover {
          background: rgba(64, 158, 255, 0.2);
          color: #409eff;
        }
      }
    }
  }

  .property-panel {
    padding: 16px;
  }
}

// 右键菜单样式
.context-menu {
  position: fixed;
  background: #1e1e24;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 4px 0;
  min-width: 140px;
  z-index: 1000;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(10px);

  .context-menu-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 16px;
    cursor: pointer;
    color: #e1e1e6;
    font-size: 13px;
    transition: all 0.2s ease;

    &:hover {
      background: rgba(64, 158, 255, 0.15);
      color: #409eff;
    }

    .el-icon {
      font-size: 14px;
    }
  }
}

// 颜色选择器
.color-selector {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;

  .color-option {
    width: 28px;
    height: 28px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s ease;
    border: 2px solid transparent;

    &:hover {
      transform: scale(1.1);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
    }

    &.active {
      border-color: #fff;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.5);
    }
  }
}

// 形状选择器
.shape-selector {
  display: flex;
  gap: 16px;

  .shape-option {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 8px;
    cursor: pointer;
    border-radius: 8px;
    transition: all 0.2s ease;
    background: rgba(255, 255, 255, 0.05);

    &:hover {
      background: rgba(64, 158, 255, 0.1);
    }

    &.active {
      background: rgba(64, 158, 255, 0.2);
      border: 1px solid #409eff;
    }

    .shape-preview {
      width: 40px;
      height: 40px;
      background: #409eff;

      &.rect {
        border-radius: 4px;
      }

      &.circle {
        border-radius: 50%;
      }

      &.diamond {
        transform: rotate(45deg);
        width: 30px;
        height: 30px;
        margin: 5px;
      }

      &.triangle {
        width: 0;
        height: 0;
        background: transparent;
        border-left: 20px solid transparent;
        border-right: 20px solid transparent;
        border-bottom: 34px solid #409eff;
      }
    }

    span {
      font-size: 12px;
      color: #a1a1aa;
    }
  }
}

// 玻璃面板效果
.glass-panel {
  background: rgba(0, 0, 0, 0.6) !important;
  backdrop-filter: blur(12px) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
}

// 响应式调整
@media (max-width: 768px) {
  .header {
    padding: 8px 12px;

    .header-left {
      gap: 8px;

      .mode-group,
      .action-group,
      .tool-group {
        .el-button {
          padding: 6px 10px;
          font-size: 12px;
        }
      }
    }
  }

  .left-panel {
    width: 260px !important;
  }
}
</style>