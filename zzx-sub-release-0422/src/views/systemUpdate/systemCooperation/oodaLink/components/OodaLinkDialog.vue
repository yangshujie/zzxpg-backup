<template>
  <el-dialog :title="title" v-model="dialogVisible" width="1200px" append-to-body class="ooda-link-dialog">
    <el-form ref="linkRef" :model="form" :rules="rules" label-width="120px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="OODA链路名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入OODA链路名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="OODA链路类型" prop="type">
            <el-select v-model="form.type" placeholder="请选择OODA链路类型" style="width: 100%">
              <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <!-- 动作约束设置对话框 -->
    <el-dialog title="设置动作约束" v-model="actionConstraintDialog" width="400px" append-to-body>
      <el-form label-width="120px">
        <el-form-item label="动作约束">
          <el-checkbox-group v-model="selectedConstraints">
            <el-checkbox v-for="option in actionConstraintOptions" :key="option.value" :label="option.value">
              {{ option.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="actionConstraintDialog = false">取消</el-button>
          <el-button type="primary" @click="saveActionConstraints">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 作战活动编排区域 -->
    <div class="combat-activity-orchestration-section">
      <h4>作战活动编排</h4>
      <div class="orchestration-container">
        <!-- 左侧：装备编组组件（带滚动） -->
        <div class="left-panel">
          <div class="combat-activity-section">
            <h5>作战活动编组</h5>
            <div class="combat-activities">
              <div v-for="activity in combatActivities" :key="activity.id" class="combat-activity-component"
                draggable="true" @dragstart="onDragStart($event, 'combatActivity', activity)">
                <div class="group-icon">⚙️</div>
                <span>{{ activity.name }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：画布 + 图例 -->
        <div class="right-panel">
          <div id="combat-activity-canvas" class="combat-activity-canvas"></div>
          <!-- 连线图例 -->
          <div class="edge-legend">
            <div class="legend-item">
              <svg width="30" height="20" viewBox="0 0 30 20">
                <line x1="2" y1="10" x2="28" y2="10" stroke="#52C41A" stroke-width="2" stroke-dasharray="0" />
                <text x="15" y="8" font-size="8" fill="#52C41A" text-anchor="middle">与</text>
              </svg>
              <span>逻辑与 (实线)</span>
            </div>
            <div class="legend-item">
              <svg width="30" height="20" viewBox="0 0 30 20">
                <line x1="2" y1="10" x2="28" y2="10" stroke="#FA8C16" stroke-width="2" stroke-dasharray="0" />
                <text x="15" y="8" font-size="8" fill="#FA8C16" text-anchor="middle">或</text>
              </svg>
              <span>逻辑或 (实线)</span>
            </div>
            <div class="legend-item">
              <svg width="30" height="20" viewBox="0 0 30 20">
                <line x1="2" y1="10" x2="28" y2="10" stroke="#1890FF" stroke-width="2" stroke-dasharray="4,4" />
                <text x="15" y="8" font-size="8" fill="#1890FF" text-anchor="middle">连接</text>
              </svg>
              <span>普通连接 (虚线)</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 作战活动详情弹窗（保持不变） -->
    <el-dialog title="作战活动详情" v-model="combatActivityDialogVisible" width="1200px" append-to-body destroy-on-close>
      <el-form :model="combatActivityFormData" label-width="120px" :disabled="true">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="作战活动名称">
              <el-input v-model="combatActivityFormData.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作战活动类型">
              <el-select v-model="combatActivityFormData.type" style="width: 100%">
                <el-option v-for="item in activityTypeOptions" :key="item.value" :label="item.label"
                  :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="活动开始时间">
              <el-date-picker v-model="combatActivityFormData.activityStartTime" type="datetime" value-format="x"
                style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="活动结束时间">
              <el-date-picker v-model="combatActivityFormData.activityEndTime" type="datetime" value-format="x"
                style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关联任务判据">
              <el-input v-model="combatActivityFormData.taskCriterionId" />
            </el-form-item>
          </el-col>
        </el-row>

        <div v-if="combatActivityFormData.frequencyDetected && combatActivityFormData.frequencyDetected.length > 0">
          <el-divider content-position="left">频率探测范围</el-divider>
          <el-form-item v-for="(freq, idx) in combatActivityFormData.frequencyDetected" :key="idx"
            :label="`频率范围 ${idx + 1}`">
            <el-row :gutter="10">
              <el-col :span="10">
                <el-input-number v-model="freq.frequencyMin" :precision="1" style="width: 100%" />
              </el-col>
              <el-col :span="10">
                <el-input-number v-model="freq.frequencyMax" :precision="1" style="width: 100%" />
              </el-col>
              <el-col :span="4">
                <span style="line-height: 32px;">MHz</span>
              </el-col>
            </el-row>
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="combatActivityDialogVisible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="OodaLinkDialog">
import { ref, reactive, nextTick, watch, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Graph } from '@antv/x6'
import { listEquipmentActivity, getEquipmentActivity } from '@/api/systemPlus/systemCooperation/act'
import { insertOodaPath, updateOodaPath } from '@/api/systemPlus/systemCooperation/ooda'
import { useDict } from '@/utils/dict'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '新增OODA链路' },
  formData: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:open', 'submit', 'cancel'])

const { subsystem_type: typeOptions } = useDict('subsystem_type')
const { subsystem_type: activityTypeOptions } = useDict('subsystem_type')

const linkRef = ref()
const dialogVisible = ref(false)
const combatActivities = ref([])
const actionConstraintDialog = ref(false)
const selectedNode = ref(null)
const actionConstraintOptions = ref([
  { label: '有可用窗口', value: 1 },
  { label: '有可用装备', value: 2 }
])
const selectedConstraints = ref([])
const addedActivityIds = ref(new Set())
const combatActivityDialogVisible = ref(false)
const combatActivityFormData = ref({})
let combatActivityGraph = null

const form = reactive({
  name: '',
  type: '',
  oodaNodes: [],
  graphJSON: ''
})

const rules = reactive({
  name: [{ required: true, message: 'OODA链路名称不能为空', trigger: 'blur' }],
  type: [{ required: true, message: 'OODA链路类型不能为空', trigger: 'change' }]
})

// ========== 连线样式 ==========
const getEdgeAttrsByType = (type) => {
  switch (type) {
    case 0:
      return {
        line: { stroke: '#52C41A', strokeWidth: 2, targetMarker: { name: 'block', width: 12, height: 8, fill: '#52C41A' } },
        label: { text: '与', fill: '#52C41A', fontSize: 10, fontWeight: 'bold' }
      }
    case 1:
      return {
        line: { stroke: '#FA8C16', strokeWidth: 2, targetMarker: { name: 'block', width: 12, height: 8, fill: '#FA8C16' } },
        label: { text: '或', fill: '#FA8C16', fontSize: 10, fontWeight: 'bold' }
      }
    default:
      return {
        line: { stroke: '#1890FF', strokeWidth: 2, strokeDasharray: '5,5', targetMarker: { name: 'block', width: 12, height: 8, fill: '#1890FF' } },
        label: { text: '连接', fill: '#1890FF', fontSize: 10 }
      }
  }
}

const updateEdgeStyle = (edge) => {
  const type = edge.getData()?.timingConstraintType ?? 2
  const attrs = getEdgeAttrsByType(type)
  edge.setAttrs({ line: attrs.line, label: attrs.label })
}

// ========== 分组计算：仅基于连接边（timingConstraintType === 2） ==========
// ========== 分组计算：连接边(type=2)定义层级，与/或边(type=0/1)强制同组 ==========
const computeNodeGroups = () => {
  if (!combatActivityGraph) return new Map()
  const nodes = combatActivityGraph.getNodes().filter(n => n.getData()?.type === 1)
  const edges = combatActivityGraph.getEdges()

  // 1. 仅使用连接边 (type===2) 构建 DAG，计算初始分组（最长路径）
  const adj = new Map()
  const inDegree = new Map()
  nodes.forEach(node => {
    adj.set(node.id, [])
    inDegree.set(node.id, 0)
  })

  edges.forEach(edge => {
    const type = edge.getData()?.timingConstraintType ?? 2
    if (type !== 2) return
    const src = edge.getSourceCellId()
    const tgt = edge.getTargetCellId()
    const srcNode = combatActivityGraph.getCellById(src)
    const tgtNode = combatActivityGraph.getCellById(tgt)
    if (srcNode?.getData()?.type === 1 && tgtNode?.getData()?.type === 1) {
      adj.get(src).push(tgt)
      inDegree.set(tgt, inDegree.get(tgt) + 1)
    }
  })

  // 拓扑排序求最长路径深度（分组从 1 开始）
  const groupMap = new Map()
  const queue = []
  nodes.forEach(node => {
    if (inDegree.get(node.id) === 0) {
      queue.push(node.id)
      groupMap.set(node.id, 1)
    }
  })

  while (queue.length) {
    const u = queue.shift()
    const curGroup = groupMap.get(u)
    for (const v of adj.get(u)) {
      const newGroup = curGroup + 1
      if (!groupMap.has(v) || groupMap.get(v) < newGroup) {
        groupMap.set(v, newGroup)
      }
      inDegree.set(v, inDegree.get(v) - 1)
      if (inDegree.get(v) === 0) queue.push(v)
    }
  }

  // 孤立节点默认分组 1
  nodes.forEach(node => {
    if (!groupMap.has(node.id)) groupMap.set(node.id, 1)
  })

  // 2. 使用与/或边 (type===0 或 1) 进行分组合并（强制同组）
  // 收集所有需要合并的节点对
  const mergePairs = []
  edges.forEach(edge => {
    const type = edge.getData()?.timingConstraintType ?? 2
    if (type === 0 || type === 1) {
      const src = edge.getSourceCellId()
      const tgt = edge.getTargetCellId()
      const srcNode = combatActivityGraph.getCellById(src)
      const tgtNode = combatActivityGraph.getCellById(tgt)
      if (srcNode?.getData()?.type === 1 && tgtNode?.getData()?.type === 1) {
        mergePairs.push([src, tgt])
      }
    }
  })

  // 使用并查集合并分组
  const parent = new Map()
  const find = (x) => {
    if (!parent.has(x)) parent.set(x, x)
    if (parent.get(x) !== x) parent.set(x, find(parent.get(x)))
    return parent.get(x)
  }
  const union = (a, b) => {
    const ra = find(a), rb = find(b)
    if (ra !== rb) parent.set(ra, rb)
  }

  // 初始化每个节点的 parent 为自己
  nodes.forEach(node => union(node.id, node.id))

  // 合并所有需要同组的节点
  mergePairs.forEach(([a, b]) => {
    union(a, b)
  })

  // 计算每个连通分量的最大分组值（取该分量中所有节点的最大分组）
  const groupMax = new Map()
  nodes.forEach(node => {
    const root = find(node.id)
    const curGroup = groupMap.get(node.id) || 1
    if (!groupMax.has(root) || groupMax.get(root) < curGroup) {
      groupMax.set(root, curGroup)
    }
  })

  // 将同一连通分量的所有节点分组设为该分量的最大值
  const finalGroupMap = new Map()
  nodes.forEach(node => {
    const root = find(node.id)
    finalGroupMap.set(node.id, groupMax.get(root) || 1)
  })

  return finalGroupMap
}

const updateAllNodeGroups = () => {
  if (!combatActivityGraph) return
  const groupMap = computeNodeGroups()
  combatActivityGraph.getNodes().forEach(node => {
    const data = node.getData()
    if (data?.type === 1) {
      const newGroup = groupMap.get(node.id) ?? 1
      if (data.oodaNodeGroup !== newGroup) {
        data.oodaNodeGroup = newGroup
        node.setData(data)
        node.attr('label/text', `${data.name}\n分组:${newGroup}`)
      }
    }
  })
}
// ========== 连线交互 ==========
const showEdgeContextMenu = (edge) => {
  const currentType = edge.getData()?.timingConstraintType ?? 2
  ElMessageBox.confirm('请选择连线逻辑关系', '设置连线类型', {
    confirmButtonText: '与', cancelButtonText: '或', distinguishCancelAndClose: true, type: 'info',
    callback: (action) => {
      let newType
      if (action === 'confirm') newType = 0
      else if (action === 'cancel') newType = 1
      else newType = 2
      if (action !== 'close') {
        edge.setData({ ...edge.getData(), timingConstraintType: newType })
        updateEdgeStyle(edge)
        updateAllNodeGroups()  // 类型改变后重新计算分组
      }
    }
  }).catch(() => {})
}

const cycleEdgeRelation = (edge) => {
  const currentType = edge.getData()?.timingConstraintType ?? 2
  let newType = (currentType + 1) % 3
  edge.setData({ ...edge.getData(), timingConstraintType: newType })
  updateEdgeStyle(edge)
  updateAllNodeGroups()
  const typeName = newType === 0 ? '与' : (newType === 1 ? '或' : '连接')
  ElMessage.success(`连线类型已切换为：${typeName}`)
}

// ========== 节点交互 ==========
const editNodeDetails = (node) => {
  const data = node.getData()
  ElMessageBox.prompt('请输入节点名称', '编辑节点', {
    confirmButtonText: '确定', cancelButtonText: '取消',
    inputValue: data.name,
    inputValidator: (v) => v ? true : '名称不能为空'
  }).then(({ value }) => {
    data.name = value
    node.setData(data)
    node.attr('label/text', `${data.name}\n分组:${data.oodaNodeGroup || 0}`)
    ElMessage.success('节点名称已更新')
  }).catch(() => {})
}

const showActionConstraintDialog = (node) => {
  selectedNode.value = node
  const data = node.getData()
  selectedConstraints.value = data.actionConstraints || [1, 2]
  actionConstraintDialog.value = true
}

const saveActionConstraints = () => {
  if (selectedNode.value) {
    const data = selectedNode.value.getData()
    data.actionConstraints = [...selectedConstraints.value]
    selectedNode.value.setData(data)
    ElMessage.success('动作约束设置成功')
  }
  actionConstraintDialog.value = false
}

const handleNodeClick = async (node) => {
  const data = node.getData()
  if (data.type === 1 && data.equipmentActivityId) {
    try {
      const response = await getEquipmentActivity(data.equipmentActivityId)
      if (response?.data) {
        combatActivityFormData.value = response.data
        combatActivityDialogVisible.value = true
      } else ElMessage.error('获取装备活动详情失败')
    } catch (error) {
      console.error(error)
      ElMessage.error('获取装备活动详情失败')
    }
  }
}

// ========== 画布拖拽 ==========
const onDragStart = (event, type, data = null) => {
  if (type === 'combatActivity' && addedActivityIds.value.has(data.id)) {
    event.preventDefault()
    ElMessage.warning(`作战活动 "${data.name}" 已添加到画布，不能重复添加`)
    return
  }
  event.dataTransfer.setData('application/json', JSON.stringify({ type, data }))
}

const onCanvasDragOver = (event) => event.preventDefault()

const onCanvasDrop = (event) => {
  event.preventDefault()
  const raw = event.dataTransfer.getData('application/json')
  if (!raw) return
  const data = JSON.parse(raw)
  if (!combatActivityGraph) return
  const rect = event.currentTarget.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  if (data.type === 'combatActivity') addCombatActivityNode(x, y, data.data)
}

const addCombatActivityNode = (x, y, activity) => {
  const nodeId = activity.id || `act_${Date.now()}_${Math.random()}`
  const node = combatActivityGraph.addNode({
    id: nodeId, x, y, width: 120, height: 60,
    label: `${activity.name}\n分组:0`,
    data: {
      type: 1,
      equipmentActivityId: activity.id,
      name: activity.name,
      actionConstraints: [1, 2],
      timingConstraints: [],
      oodaNodeGroup: 0
    },
    ports: {
      groups: {
        top: { position: 'top', attrs: { circle: { r: 4, magnet: true, stroke: '#409EFF', strokeWidth: 2, fill: '#fff' } } },
        bottom: { position: 'bottom', attrs: { circle: { r: 4, magnet: true, stroke: '#409EFF', strokeWidth: 2, fill: '#fff' } } }
      },
      items: [{ id: 'top', group: 'top' }, { id: 'bottom', group: 'bottom' }]
    },
    attrs: {
      body: { stroke: '#409EFF', fill: '#ecf5ff', rx: 6, ry: 6 },
      label: { text: `${activity.name}\n分组:0`, fontSize: 12 }
    }
  })
  addedActivityIds.value.add(activity.id)
  return node
}

// ========== 初始化画布 ==========
const initCombatActivityGraph = () => {
  const container = document.getElementById('combat-activity-canvas')
  if (!container) return
  if (combatActivityGraph) combatActivityGraph.dispose()

  combatActivityGraph = new Graph({
    container, width: '100%', height: '100%', grid: true,
    background: { color: '#fafafa' },
    mousewheel: { enabled: true, zoomAtMousePosition: true, minScale: 0.5, maxScale: 3 },
    panning: { enabled: true },
    connecting: {
      router: 'manhattan', connector: { name: 'rounded', args: { radius: 8 } },
      createEdge() {
        return combatActivityGraph.createEdge({
          attrs: getEdgeAttrsByType(2),
          data: { timingConstraintType: 2 },
          zIndex: 0,
        })
      },
      validateMagnet: () => true,
      validateConnection: ({ sourceView, targetView }) => {
        if (sourceView === targetView) return false
        const sd = sourceView.cell.getData()
        const td = targetView.cell.getData()
        if (!sd || !td) return false
        if (sd.type !== 1 || td.type !== 1) return false
        // 禁止重复连线
        const edges = combatActivityGraph.getEdges()
        return !edges.some(e => e.getSourceCellId() === sourceView.cell.id && e.getTargetCellId() === targetView.cell.id)
      }
    }
  })

  combatActivityGraph.enableRubberband()
  combatActivityGraph.enableKeyboard()

  container.addEventListener('dragover', onCanvasDragOver)
  container.addEventListener('drop', onCanvasDrop)

  combatActivityGraph.on('edge:contextmenu', ({ edge, e }) => {
    e.preventDefault(); e.stopPropagation(); showEdgeContextMenu(edge)
  })
  combatActivityGraph.on('edge:dblclick', ({ edge, e }) => {
    e.preventDefault(); e.stopPropagation(); cycleEdgeRelation(edge)
  })
  combatActivityGraph.on('edge:connected', ({ edge }) => {
    updateEdgeStyle(edge)
    updateAllNodeGroups()
  })
  combatActivityGraph.on('edge:removed', () => {
    updateAllNodeGroups()
  })
  combatActivityGraph.on('node:contextmenu', ({ e, node }) => {
    e.preventDefault(); showActionConstraintDialog(node)
  })
  combatActivityGraph.on('node:dblclick', ({ node }) => editNodeDetails(node))
  combatActivityGraph.on('node:click', ({ node }) => handleNodeClick(node))
}

// ========== 加载数据 ==========
const loadCombatActivities = async () => {
  try {
    const res = await listEquipmentActivity({ pageNum: 1, pageSize: 1000 })
    combatActivities.value = res.rows || res.data || []
  } catch (error) {
    console.error(error)
    ElMessage.error('加载作战活动失败')
  }
}

const loadEditData = () => {
  if (!props.formData || !combatActivityGraph) return
  Object.assign(form, props.formData)
  combatActivityGraph.clearCells()
  addedActivityIds.value.clear()

  // 优先使用 graphJSON 恢复
  if (props.formData.graphJSON) {
    try {
      const graphData = JSON.parse(props.formData.graphJSON)
      combatActivityGraph.fromJSON(graphData)
      // 确保边的 timingConstraintType 与标签一致
      combatActivityGraph.getEdges().forEach(edge => {
        const labelText = edge.getAttrs()?.label?.text
        let type = 2
        if (labelText === '与') type = 0
        else if (labelText === '或') type = 1
        edge.setData({ ...edge.getData(), timingConstraintType: type })
        updateEdgeStyle(edge)
      })
      combatActivityGraph.getNodes().forEach(node => {
        const d = node.getData()
        if (d?.equipmentActivityId) addedActivityIds.value.add(d.equipmentActivityId)
        if (d?.type === 1) {
          node.attr('label/text', `${d.name}\n分组:${d.oodaNodeGroup ?? 0}`)
        }
      })
      updateAllNodeGroups()
      return
    } catch (e) {
      console.error('graphJSON 解析失败', e)
    }
  }

  // 兼容旧的 oodaNodes 结构
  if (props.formData.oodaNodes?.length) {
    const nodeMap = new Map()
    let x = 100, y = 100, spacing = 200
    props.formData.oodaNodes.forEach(nodeData => {
      const activity = { id: nodeData.equipmentActivityId, name: nodeData.name }
      const node = addCombatActivityNode(x, y, activity)
      const model = node.getData()
      model.oodaNodeGroup = nodeData.oodaNodeGroup || 0
      model.actionConstraints = nodeData.actionConstraints || [1, 2]
      node.setData(model)
      node.attr('label/text', `${model.name}\n分组:${model.oodaNodeGroup}`)
      nodeMap.set(nodeData.equipmentActivityId, node)
      x += spacing
      if (x > 600) { x = 100; y += spacing }
    })
    props.formData.oodaNodes.forEach(nodeData => {
      const srcNode = nodeMap.get(nodeData.equipmentActivityId)
      nodeData.timingConstraints?.forEach(tc => {
        const tgtNode = nodeMap.get(tc.relativeDriverOODAPathNodeId)
        if (tgtNode) {
          const edge = combatActivityGraph.addEdge({
            source: { cell: srcNode.id },
            target: { cell: tgtNode.id },
            data: { timingConstraintType: tc.timingConstraintType ?? 2 }
          })
          updateEdgeStyle(edge)
        }
      })
    })
    updateAllNodeGroups()
  }
}

// ========== 提交 ==========
const submitForm = async () => {
  try {
    await linkRef.value.validate()
    const oodaNodes = []
    if (combatActivityGraph) {
      const nodes = combatActivityGraph.getNodes()
      const edges = combatActivityGraph.getEdges()
      nodes.forEach(node => {
        const data = node.getData()
        if (data?.type !== 1) return
        const timingConstraints = []
        edges.forEach(edge => {
          if (edge.getTargetCellId() === node.id) {
            const src = combatActivityGraph.getCellById(edge.getSourceCellId())
            if (src?.getData()?.type === 1) {
              timingConstraints.push({
                relativeDriverOODAPathNodeId: src.id,
                timingConstraintType: edge.getData()?.timingConstraintType ?? 2,
                oodaNodeGroup: src.getData()?.oodaNodeGroup || 0
              })
            }
          }
        })
        oodaNodes.push({
          name: data.name,
          oodaNodeGroup: data.oodaNodeGroup ?? 0,
          equipmentActivityId: data.equipmentActivityId,
          actionConstraints: data.actionConstraints || [1, 2],
          timingConstraints
        })
      })
    }
    const submitData = {
      name: form.name,
      type: form.type,
      oodaNodes,
      graphJSON: combatActivityGraph ? JSON.stringify(combatActivityGraph.toJSON()) : ''
    }
    if (props.formData?.id) {
      submitData.id = props.formData.id
      await updateOodaPath(submitData)
      ElMessage.success('OODA链路修改成功')
    } else {
      await insertOodaPath(submitData)
      ElMessage.success('OODA链路新增成功')
    }
    emit('submit', submitData)
    emit('update:open', false)
  } catch (error) {
    if (error instanceof Error) ElMessage.error('请完善表单信息')
    else { console.error(error); ElMessage.error('保存失败，请重试') }
  }
}

const cancel = () => {
  emit('cancel')
  emit('update:open', false)
}

// ========== 生命周期 ==========
watch(() => props.open, (newVal) => {
  dialogVisible.value = newVal
  if (newVal) {
    addedActivityIds.value.clear()
    nextTick(() => {
      initCombatActivityGraph()
      loadCombatActivities()
      if (props.formData && props.formData.id) loadEditData()
    })
  }
})

watch(dialogVisible, (newVal) => {
  emit('update:open', newVal)
  if (!newVal) emit('cancel')
})

onUnmounted(() => {
  if (combatActivityGraph) combatActivityGraph.dispose()
})
</script>

<style scoped>
.ooda-link-dialog {
  max-height: 90vh;
}

.combat-activity-orchestration-section {
  margin-top: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 20px;
}

.orchestration-container {
  display: flex;
  height: 500px;
  gap: 20px;
}

.left-panel {
  width: 300px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.right-panel {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.combat-activities {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  max-height: 400px;
}

.combat-activity-component {
  display: flex;
  align-items: center;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: grab;
  background: #f5f7fa;
  transition: all 0.3s;
}

.combat-activity-component:hover {
  background: #e4e7ed;
  border-color: #409EFF;
}

.combat-activity-component:active {
  cursor: grabbing;
}

.combat-activity-component .group-icon {
  margin-right: 10px;
  font-size: 18px;
}

.combat-activity-canvas {
  width: 100%;
  height: 100%;
}

:deep(.x6-port-body) {
  stroke: #409EFF;
  stroke-width: 2;
  fill: #fff;
  r: 4;
}

:deep(.x6-port-body:hover) {
  stroke: #67C23A;
  fill: #67C23A;
}

:deep(.x6-edge path) {
  stroke-width: 2;
}

:deep(.x6-edge:hover path) {
  stroke-width: 3;
}

.dialog-footer {
  text-align: center;
  margin-top: 20px;
}

.dialog-footer .el-button {
  margin: 0 10px;
}

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
  border: 1px solid #e4e7ed;
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