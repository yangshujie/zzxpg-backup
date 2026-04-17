<template>
  <div class="app-container mission-page task-network-page">
    <section class="page-hero">
      <div class="page-hero__body">
        <div>
          <span class="page-hero__eyebrow">Task Network Viewer</span>
          <h1 class="page-hero__title">任务网络结构总览</h1>
          <p class="page-hero__description">
            通过任务节点、子任务节点和关联边的分层展示，统一表达任务链路、从属关系和联动方式，方便在主中心进行结构复核。
          </p>
          <div class="page-hero__actions">
            <el-button type="primary" icon="Refresh" @click="getList">刷新网络</el-button>
          </div>
        </div>

        <div class="page-hero__panel">
          <div class="glass-card">
            <div class="panel-heading">
              <div>
                <h3 class="panel-title">网络概览</h3>
                <p class="panel-subtitle">当前数据页中的网络规模与联通情况</p>
              </div>
              <span class="status-chip status-chip--success">结构可视</span>
            </div>
            <div class="hero-kpi-grid">
              <div v-for="item in overviewStats" :key="item.label" class="hero-kpi">
                <div class="hero-kpi__value">{{ item.value }}</div>
                <div class="hero-kpi__label">{{ item.label }}</div>
              </div>
            </div>
          </div>

          <div class="glass-card">
            <div class="panel-heading">
              <div>
                <h3 class="panel-title">优化优先级</h3>
                <p class="panel-subtitle">树结构和画布结构应保持一一对应</p>
              </div>
            </div>
            <div class="score-bars">
              <div v-for="item in networkPriority" :key="item.label" class="score-bar">
                <div class="score-bar__head">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}%</strong>
                </div>
                <div class="score-bar__track">
                  <div :class="['score-bar__fill', item.tone && `score-bar__fill--${item.tone}`]" :style="{ width: `${item.value}%` }"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="panel-card">
      <div class="panel-heading">
        <div>
          <h3 class="panel-title">网络筛选</h3>
          <p class="panel-subtitle">按网络名称和评估任务进行筛选</p>
        </div>
      </div>
      <el-form ref="queryRef" :model="queryParams" label-width="110px">
        <div class="form-grid form-grid--2">
          <div class="field-card">
            <el-form-item label="任务网名称" prop="name">
              <el-input v-model="queryParams.name" placeholder="请输入任务网名称" clearable @keyup.enter="handleQuery" />
            </el-form-item>
          </div>
          <div class="field-card">
            <el-form-item label="评估任务" prop="task">
              <el-input v-model="queryParams.task" placeholder="请输入评估任务名称" clearable @keyup.enter="handleQuery" />
            </el-form-item>
          </div>
        </div>
        <div class="compact-actions">
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </div>
      </el-form>
    </section>

    <section class="panel-card table-card">
      <div class="panel-heading">
        <div>
          <h3 class="panel-title">任务网络列表</h3>
          <p class="panel-subtitle">支持编辑、删除和网络结构详情查看</p>
        </div>
        <span class="status-chip status-chip--warning">共 {{ total }} 条</span>
      </div>
      <el-table v-loading="loading" :data="networkList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="任务网名称" prop="networkName" min-width="200" show-overflow-tooltip />
        <el-table-column label="所属评估任务" prop="evaluationTask" min-width="180" show-overflow-tooltip />
        <el-table-column label="节点信息" prop="nodeInfo" min-width="180" show-overflow-tooltip />
        <el-table-column label="边信息" prop="edgeInfo" min-width="180" show-overflow-tooltip />
        <el-table-column label="创建时间" prop="createTime" width="180" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">编辑</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
            <el-button link type="info" icon="View" @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </section>

    <el-dialog v-model="open" :title="title" width="720px" append-to-body>
      <el-form ref="networkRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid form-grid--2">
          <div class="field-card">
            <el-form-item label="任务网名称" prop="networkName">
              <el-input v-model="form.networkName" placeholder="请输入任务网名称" />
            </el-form-item>
          </div>
          <div class="field-card">
            <el-form-item label="所属评估任务" prop="evaluationTask">
              <el-input v-model="form.evaluationTask" placeholder="请输入所属评估任务" />
            </el-form-item>
          </div>
          <div class="field-card">
            <el-form-item label="任务节点信息" prop="nodeInfo">
              <el-input v-model="form.nodeInfo" type="textarea" :rows="4" placeholder="请输入任务节点信息" />
            </el-form-item>
          </div>
          <div class="field-card">
            <el-form-item label="任务边信息" prop="edgeInfo">
              <el-input v-model="form.edgeInfo" type="textarea" :rows="4" placeholder="请输入任务边信息" />
            </el-form-item>
          </div>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailOpen"
      :title="detailTitle"
      width="1460px"
      append-to-body
      class="task-network-detail-dialog"
      @closed="disposeGraph"
    >
      <div class="network-detail-shell">
        <aside class="network-tree-panel">
          <div class="panel-heading network-panel-heading">
            <div>
              <h3 class="panel-title">结构树</h3>
              <p class="panel-subtitle">点击左侧节点后高亮右侧网络对象</p>
            </div>
            <span class="status-chip status-chip--info">Tree</span>
          </div>
          <el-tree
            ref="detailTreeRef"
            :data="detailTreeData"
            :props="detailTreeProps"
            node-key="id"
            :expand-on-click-node="false"
            :highlight-current="true"
            @node-click="handleTreeNodeClick"
            class="detail-tree"
          >
            <template #default="{ node, data }">
              <span class="tree-node-content">
                <span class="node-label">{{ node.label }}</span>
                <span v-if="data.type === 'task'" class="node-badge">T</span>
                <span v-if="data.type === 'subtask'" class="node-badge node-badge--success">S</span>
                <span v-if="data.type === 'edge'" class="node-badge node-badge--warning">E</span>
              </span>
            </template>
          </el-tree>
        </aside>

        <div class="network-main-panel">
          <div class="panel-card network-summary-panel">
            <div class="mini-stat-grid">
              <div v-for="item in detailStats" :key="item.label" class="mini-stat">
                <div class="mini-stat__value">{{ item.value }}</div>
                <div class="mini-stat__label">{{ item.label }}</div>
              </div>
            </div>
          </div>

          <div class="canvas-shell">
            <div class="canvas-shell__head">
              <div>
                <div class="constraint-card__title">任务网络画布</div>
                <div class="constraint-card__meta">主任务区、子任务区和关联边在同一画布中呈现</div>
              </div>
              <div class="legend-row">
                <span v-for="item in detailLegend" :key="item.label" class="legend-chip">
                  <span class="legend-chip__dot" :style="{ background: item.color }"></span>
                  {{ item.label }}
                </span>
              </div>
            </div>
            <div ref="taskNetworkCanvas" class="task-network-canvas"></div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeDetail">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="TaskNetwork">
import { computed, nextTick, onBeforeUnmount, reactive, ref, toRefs } from 'vue'
import { Graph } from '@antv/x6'
import { listTaskNetwork, getTaskNetwork, delTaskNetwork, updateTaskNetwork } from '@/api/systemPlus/systemCooperation/taskNetwork'
import useSettingsStore from '@/store/modules/settings'

const { proxy } = getCurrentInstance()
const settingsStore = useSettingsStore()

const networkList = ref([])
const loading = ref(true)
const total = ref(0)
const open = ref(false)
const detailOpen = ref(false)
const title = ref('')
const detailTitle = ref('')
const networkRef = ref()
const taskNetworkCanvas = ref()
const detailTreeRef = ref()
const taskNetworkGraph = ref(null)
const graphNodes = ref({})

const detailTreeData = ref([])
const detailTreeProps = {
  children: 'children',
  label: 'label'
}

const detailLegend = [
  { label: '主任务节点', color: '#38bdf8' },
  { label: '子任务节点', color: '#34d399' },
  { label: '任务边', color: '#a78bfa' }
]

const data = reactive({
  form: {
    id: null,
    networkName: '',
    evaluationTask: '',
    nodeInfo: '',
    edgeInfo: ''
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    task: undefined
  },
  rules: {
    networkName: [{ required: true, message: '任务网名称不能为空', trigger: 'blur' }],
    evaluationTask: [{ required: true, message: '所属评估任务不能为空', trigger: 'blur' }]
  }
})

const { form, queryParams, rules } = toRefs(data)

const overviewStats = computed(() => {
  const rows = networkList.value || []
  const nodeCount = rows.filter(item => item.nodeInfo).length
  const edgeCount = rows.filter(item => item.edgeInfo).length
  return [
    { label: '当前分页网络', value: rows.length },
    { label: '全量记录', value: total.value },
    { label: '含节点信息', value: `${nodeCount} 项` },
    { label: '含边信息', value: `${edgeCount} 项` }
  ]
})

const networkPriority = [
  { label: '结构树与画布一致性', value: 88 },
  { label: '节点信息可读性', value: 73, tone: 'warning' },
  { label: '边关系标注能力', value: 67, tone: 'danger' }
]

const detailStats = computed(() => [
  { label: '主任务节点', value: 4 },
  { label: '子任务节点', value: 6 },
  { label: '关系边', value: 8 },
  { label: '结构层级', value: 3 }
])

const networkPalette = computed(() => settingsStore.isDark ? {
  canvas: '#07111f',
  grid: 'rgba(110, 132, 166, 0.12)',
  zoneFill: 'rgba(10, 20, 38, 0.58)',
  zoneStroke: '#243b63',
  taskFill: '#102845',
  taskStroke: '#38bdf8',
  taskText: '#7dd3fc',
  subtaskFill: '#0f2c27',
  subtaskStroke: '#34d399',
  subtaskText: '#86efac'
} : {
  canvas: '#f6fbff',
  grid: 'rgba(120, 154, 200, 0.14)',
  zoneFill: 'rgba(255, 255, 255, 0.86)',
  zoneStroke: '#bdd0ea',
  taskFill: '#edf7ff',
  taskStroke: '#0ea5e9',
  taskText: '#0369a1',
  subtaskFill: '#ecfdf5',
  subtaskStroke: '#10b981',
  subtaskText: '#047857'
})

function getList() {
  loading.value = true
  listTaskNetwork(queryParams.value).then(response => {
    networkList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange() {}

function handleUpdate(row) {
  reset()
  getTaskNetwork(row.id).then(response => {
    Object.assign(form.value, response.data || {})
    open.value = true
    title.value = '修改任务网'
  })
}

function handleDelete(row) {
  proxy.$modal.confirm(`是否确认删除任务网编号为 "${row.id}" 的数据项？`).then(() => {
    return delTaskNetwork(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function handleDetail(row) {
  detailTitle.value = `${row.networkName} - 任务网络详情`
  detailOpen.value = true
  initDetailTreeData()
  nextTick(() => {
    initTaskNetworkGraph()
  })
}

function initDetailTreeData() {
  detailTreeData.value = [
    {
      id: 'root',
      label: '任务网络根节点',
      type: 'root',
      children: [
        {
          id: 'tasks',
          label: '主任务节点',
          type: 'group',
          children: [
            { id: 'T1', label: '任务 T1', type: 'task' },
            { id: 'T2', label: '任务 T2', type: 'task' },
            { id: 'T3', label: '任务 T3', type: 'task' },
            { id: 'T4', label: '任务 T4', type: 'task' }
          ]
        },
        {
          id: 'subtasks',
          label: '子任务节点',
          type: 'group',
          children: [
            { id: 'S1', label: '子任务 S1', type: 'subtask' },
            { id: 'S2', label: '子任务 S2', type: 'subtask' },
            { id: 'S3', label: '子任务 S3', type: 'subtask' },
            { id: 'S4', label: '子任务 S4', type: 'subtask' },
            { id: 'S5', label: '子任务 S5', type: 'subtask' },
            { id: 'S6', label: '子任务 S6', type: 'subtask' }
          ]
        },
        {
          id: 'edges',
          label: '关联边',
          type: 'group',
          children: [
            { id: 'E1', label: 'T1 -> T2', type: 'edge' },
            { id: 'E2', label: 'T2 -> T3', type: 'edge' },
            { id: 'E3', label: 'T3 -> T4', type: 'edge' },
            { id: 'E4', label: 'T1 -> S1', type: 'edge' },
            { id: 'E5', label: 'T2 -> S2', type: 'edge' }
          ]
        }
      ]
    }
  ]
}

function createNode(shape, id, x, y, width, height, fill, stroke, label, color = '#fff') {
  const node = taskNetworkGraph.value.addNode({
    shape,
    id,
    x,
    y,
    width,
    height,
    attrs: {
      body: {
        fill,
        stroke,
        strokeWidth: 2,
        rx: shape === 'rect' ? 14 : undefined,
        ry: shape === 'rect' ? 14 : undefined
      },
      label: {
        text: label,
        fill: color,
        fontSize: 13,
        fontWeight: 600
      }
    }
  })
  graphNodes.value[id] = node
  return node
}

function createEdge(id, source, target, color, dashed = false) {
  const edge = taskNetworkGraph.value.addEdge({
    id,
    source,
    target,
    attrs: {
      line: {
        stroke: color,
        strokeWidth: 2,
        strokeDasharray: dashed ? '6,4' : undefined,
        targetMarker: {
          name: 'block',
          width: 10,
          height: 6
        }
      }
    }
  })
  graphNodes.value[id] = edge
}

function initTaskNetworkGraph() {
  if (!taskNetworkCanvas.value) return
  disposeGraph()
  graphNodes.value = {}

  taskNetworkGraph.value = new Graph({
    container: taskNetworkCanvas.value,
    width: taskNetworkCanvas.value.clientWidth,
    height: taskNetworkCanvas.value.clientHeight,
    background: { color: networkPalette.value.canvas },
    grid: {
      visible: true,
      size: 20,
      type: 'mesh',
      args: {
        color: networkPalette.value.grid,
        thickness: 1
      }
    },
    panning: true,
    mousewheel: {
      enabled: true,
      modifiers: 'ctrl',
      minScale: 0.4,
      maxScale: 2
    }
  })

  taskNetworkGraph.value.addNode({
    shape: 'rect',
    x: 40,
    y: 40,
    width: 720,
    height: 150,
    attrs: {
      body: {
        fill: networkPalette.value.zoneFill,
        stroke: networkPalette.value.zoneStroke,
        strokeWidth: 2,
        rx: 18,
        ry: 18
      },
      label: {
        text: '主任务区域',
        fill: '#8ea7c7',
        fontSize: 14,
        fontWeight: 600
      }
    }
  })

  taskNetworkGraph.value.addNode({
    shape: 'rect',
    x: 40,
    y: 240,
    width: 720,
    height: 230,
    attrs: {
      body: {
        fill: networkPalette.value.zoneFill,
        stroke: networkPalette.value.zoneStroke,
        strokeWidth: 2,
        rx: 18,
        ry: 18
      },
      label: {
        text: '子任务区域',
        fill: '#8ea7c7',
        fontSize: 14,
        fontWeight: 600
      }
    }
  })

  const task1 = createNode('circle', 'T1', 100, 95, 56, 56, networkPalette.value.taskFill, networkPalette.value.taskStroke, 'T1', networkPalette.value.taskText)
  const task2 = createNode('circle', 'T2', 240, 95, 56, 56, networkPalette.value.taskFill, networkPalette.value.taskStroke, 'T2', networkPalette.value.taskText)
  const task3 = createNode('circle', 'T3', 380, 95, 56, 56, networkPalette.value.taskFill, networkPalette.value.taskStroke, 'T3', networkPalette.value.taskText)
  const task4 = createNode('circle', 'T4', 520, 95, 56, 56, networkPalette.value.taskFill, networkPalette.value.taskStroke, 'T4', networkPalette.value.taskText)

  const subtask1 = createNode('rect', 'S1', 90, 300, 92, 48, networkPalette.value.subtaskFill, networkPalette.value.subtaskStroke, 'S1', networkPalette.value.subtaskText)
  const subtask2 = createNode('rect', 'S2', 220, 300, 92, 48, networkPalette.value.subtaskFill, networkPalette.value.subtaskStroke, 'S2', networkPalette.value.subtaskText)
  const subtask3 = createNode('rect', 'S3', 350, 300, 92, 48, networkPalette.value.subtaskFill, networkPalette.value.subtaskStroke, 'S3', networkPalette.value.subtaskText)
  const subtask4 = createNode('rect', 'S4', 480, 300, 92, 48, networkPalette.value.subtaskFill, networkPalette.value.subtaskStroke, 'S4', networkPalette.value.subtaskText)
  const subtask5 = createNode('rect', 'S5', 155, 390, 92, 48, networkPalette.value.subtaskFill, networkPalette.value.subtaskStroke, 'S5', networkPalette.value.subtaskText)
  const subtask6 = createNode('rect', 'S6', 415, 390, 92, 48, networkPalette.value.subtaskFill, networkPalette.value.subtaskStroke, 'S6', networkPalette.value.subtaskText)

  createEdge('E1', task1, task2, '#38bdf8')
  createEdge('E2', task2, task3, '#38bdf8')
  createEdge('E3', task3, task4, '#38bdf8')
  createEdge('E4', task1, subtask1, '#a78bfa', true)
  createEdge('E5', task2, subtask2, '#a78bfa', true)
  createEdge('E6', task3, subtask3, '#a78bfa', true)
  createEdge('E7', task4, subtask4, '#a78bfa', true)
  createEdge('E8', subtask2, subtask5, '#34d399')
  createEdge('E9', subtask4, subtask6, '#34d399')

  taskNetworkGraph.value.centerContent()
}

function handleTreeNodeClick(data) {
  if (!taskNetworkGraph.value || !graphNodes.value[data.id]) return
  const cell = graphNodes.value[data.id]
  taskNetworkGraph.value.cleanSelection()
  if (cell.isNode?.()) {
    taskNetworkGraph.value.select(cell)
    taskNetworkGraph.value.centerCell(cell)
  } else if (cell.isEdge?.()) {
    taskNetworkGraph.value.select(cell)
    taskNetworkGraph.value.centerCell(cell)
  }
}

function closeDetail() {
  detailOpen.value = false
}

function submitForm() {
  networkRef.value.validate(valid => {
    if (!valid) return
    updateTaskNetwork(form.value).then(() => {
      proxy.$modal.msgSuccess('修改成功')
      open.value = false
      getList()
    })
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    id: null,
    networkName: '',
    evaluationTask: '',
    nodeInfo: '',
    edgeInfo: ''
  }
  nextTick(() => {
    networkRef.value?.clearValidate()
  })
}

function disposeGraph() {
  if (taskNetworkGraph.value) {
    taskNetworkGraph.value.dispose()
    taskNetworkGraph.value = null
  }
  graphNodes.value = {}
}

onBeforeUnmount(() => {
  disposeGraph()
})

getList()
</script>

<style scoped lang="scss">
.task-network-page {
  .network-detail-shell {
    display: grid;
    grid-template-columns: 320px minmax(0, 1fr);
    gap: 18px;
    min-height: 720px;
  }

  .network-tree-panel {
    border-radius: 20px;
    border: 1px solid rgba(86, 122, 173, 0.22);
    background: color-mix(in srgb, var(--mc-bg-elevated) 88%, transparent);
    padding: 20px;
  }

  .network-panel-heading {
    margin-bottom: 12px;
  }

  .detail-tree {
    max-height: 620px;
    overflow: auto;
  }

  .tree-node-content {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    width: 100%;
  }

  .node-label {
    flex: 1;
    min-width: 0;
    color: var(--mc-text-secondary);
  }

  .node-badge {
    width: 24px;
    height: 24px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: rgba(56, 189, 248, 0.16);
    color: #7dd3fc;
    font-size: 12px;
    font-weight: 700;
  }

  .node-badge--success {
    background: rgba(52, 211, 153, 0.16);
    color: #86efac;
  }

  .node-badge--warning {
    background: rgba(251, 191, 36, 0.16);
    color: #fde68a;
  }

  .network-main-panel {
    display: grid;
    grid-template-rows: auto minmax(0, 1fr);
    gap: 18px;
  }

  .network-summary-panel {
    padding-bottom: 18px;
  }

  .canvas-shell {
    display: flex;
    flex-direction: column;
    border-radius: 20px;
    border: 1px solid rgba(86, 122, 173, 0.22);
    background: color-mix(in srgb, var(--mc-bg-elevated) 88%, transparent);
    overflow: hidden;
  }

  .canvas-shell__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
    padding: 18px 20px;
    border-bottom: 1px solid rgba(86, 122, 173, 0.14);
  }

  .constraint-card__title {
    color: var(--mc-text-primary);
    font-weight: 600;
  }

  .constraint-card__meta {
    margin-top: 6px;
    color: var(--mc-text-tertiary);
    font-size: 12px;
  }

  .task-network-canvas {
    min-height: 560px;
    height: 100%;
  }
}

@media (max-width: 1280px) {
  .task-network-page .network-detail-shell {
    grid-template-columns: 1fr;
  }
}
</style>
