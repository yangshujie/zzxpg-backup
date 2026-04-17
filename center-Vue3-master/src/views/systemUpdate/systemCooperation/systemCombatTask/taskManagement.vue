<template>
  <div class="app-container mission-page task-management-page">
    <section class="page-hero">
      <div class="page-hero__body">
        <div>
          <span class="page-hero__eyebrow">Task Orchestration</span>
          <h1 class="page-hero__title">任务编排与协同控制台</h1>
          <p class="page-hero__description">
            把任务定义、判据关联和流程编排放在同一工作台中管理，让画布、表格和流程状态保持统一语义。
          </p>
          <div class="page-hero__actions">
            <el-button type="primary" icon="Plus" @click="handleAdd">新增任务</el-button>
            <el-button icon="Refresh" @click="getList">刷新数据</el-button>
          </div>
        </div>

        <div class="page-hero__panel">
          <div class="glass-card">
            <div class="panel-heading">
              <div>
                <h3 class="panel-title">编排总览</h3>
                <p class="panel-subtitle">当前分页任务、类型覆盖和流程节点状态</p>
              </div>
              <span class="status-chip status-chip--success">画布在线</span>
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
                <h3 class="panel-title">编排关注点</h3>
                <p class="panel-subtitle">交互与数据结构同步优化</p>
              </div>
            </div>
            <div class="score-bars">
              <div v-for="item in orchestrationPriority" :key="item.label" class="score-bar">
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
          <h3 class="panel-title">任务筛选</h3>
          <p class="panel-subtitle">按任务名称和类型筛选编排对象</p>
        </div>
      </div>
      <el-form ref="queryRef" :model="queryParams" label-width="110px">
        <div class="form-grid form-grid--2">
          <div class="field-card">
            <el-form-item label="任务名称" prop="name">
              <el-input v-model="queryParams.name" placeholder="请输入任务名称" clearable @keyup.enter="handleQuery" />
            </el-form-item>
          </div>
          <div class="field-card">
            <el-form-item label="任务类型" prop="type">
              <el-select v-model="queryParams.type" placeholder="请选择任务类型" clearable>
                <el-option v-for="item in taskTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
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
          <h3 class="panel-title">任务列表</h3>
          <p class="panel-subtitle">列表与编排工作台配合使用，便于快速进入编辑</p>
        </div>
        <span class="status-chip status-chip--warning">共 {{ total }} 条</span>
      </div>
      <el-table v-loading="loading" :data="taskList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="任务名称" prop="taskName" min-width="180" show-overflow-tooltip />
        <el-table-column label="任务类型" prop="taskType" min-width="180">
          <template #default="{ row }">
            <span class="status-chip status-chip--info">{{ getTaskTypeLabel(row.taskType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="任务描述" prop="taskDescription" min-width="220" show-overflow-tooltip />
        <el-table-column label="关联判据" prop="relatedCriterion" min-width="180" show-overflow-tooltip />
        <el-table-column label="创建时间" prop="createTime" width="180" />
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">编辑</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="open" :title="title" width="1380px" append-to-body class="task-management-dialog" @closed="disposeGraph">
      <div class="task-dialog-shell">
        <div class="panel-card">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">任务基础信息</h3>
              <p class="panel-subtitle">描述任务名称、类型、说明和判据关联</p>
            </div>
            <span class="status-chip status-chip--success">配置中</span>
          </div>
          <el-form ref="taskRef" :model="form" :rules="rules" label-position="top">
            <div class="form-grid form-grid--2">
              <div class="field-card">
                <el-form-item label="任务名称" prop="taskName">
                  <el-input v-model="form.taskName" placeholder="请输入任务名称" />
                </el-form-item>
              </div>
              <div class="field-card">
                <el-form-item label="任务类型" prop="taskType">
                  <el-select v-model="form.taskType" placeholder="请选择任务类型">
                    <el-option v-for="item in taskTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
              </div>
              <div class="field-card">
                <el-form-item label="任务描述" prop="taskDescription">
                  <el-input v-model="form.taskDescription" type="textarea" :rows="4" placeholder="请输入任务描述" />
                </el-form-item>
              </div>
              <div class="field-card">
                <el-form-item label="关联任务判据" prop="relatedCriterion">
                  <el-input v-model="form.relatedCriterion" placeholder="请输入关联任务判据" />
                </el-form-item>
                <div class="tag-cloud">
                  <span class="tag-cloud__item">判据联动</span>
                  <span class="tag-cloud__item">任务阶段</span>
                  <span class="tag-cloud__item">流程控制</span>
                </div>
              </div>
            </div>
          </el-form>
        </div>

        <div class="panel-card">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">任务编排工作台</h3>
              <p class="panel-subtitle">左侧逻辑组件，右侧流程画布，对齐指标分析视图的深色工作台风格</p>
            </div>
            <div class="legend-row">
              <span v-for="item in legendItems" :key="item.label" class="legend-chip">
                <span class="legend-chip__dot" :style="{ background: item.color }"></span>
                {{ item.label }}
              </span>
            </div>
          </div>

          <div class="orchestration-layout">
            <aside class="orchestration-sidebar">
              <div class="field-card">
                <div class="constraint-card__title">导入与操作</div>
                <p class="side-note">将子任务、逻辑节点与条件判断拖拽到右侧画布完成流程组织。</p>
                <div class="compact-actions">
                  <el-button type="primary" plain icon="Upload" @click="handleSubtaskImport">子任务导入</el-button>
                  <el-button icon="Refresh" @click="resetCanvas">重置画布</el-button>
                </div>
              </div>

              <div class="field-card">
                <div class="constraint-card__title">逻辑组件</div>
                <div class="palette-list">
                  <div
                    v-for="item in paletteItems"
                    :key="item.type"
                    class="palette-item"
                    draggable="true"
                    @dragstart="onDragStart($event, item.type)"
                  >
                    <span class="palette-item__icon" :style="{ background: item.color }">{{ item.short }}</span>
                    <div>
                      <div class="palette-item__title">{{ item.label }}</div>
                      <div class="palette-item__desc">{{ item.desc }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </aside>

            <div class="canvas-shell">
              <div class="canvas-shell__head">
                <div>
                  <div class="constraint-card__title">任务流程画布</div>
                  <div class="constraint-card__meta">支持拖拽节点、连线、缩放和复制</div>
                </div>
                <span class="status-chip status-chip--info">Ctrl + 滚轮缩放</span>
              </div>
              <div ref="taskFlowCanvas" class="task-flow-canvas"></div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="TaskManagement">
import { computed, nextTick, onBeforeUnmount, reactive, ref, toRefs, watch } from 'vue'
import { Graph, Shape } from '@antv/x6'
import useSettingsStore from '@/store/modules/settings'
import {
  listTaskManagement,
  getTaskManagement,
  delTaskManagement,
  addTaskManagement,
  updateTaskManagement
} from '@/api/systemPlus/systemCooperation/taskManagement'

const { proxy } = getCurrentInstance()
const settingsStore = useSettingsStore()

const open = ref(false)
const loading = ref(true)
const total = ref(0)
const title = ref('')
const taskList = ref([])
const selectedIds = ref([])
const taskRef = ref()
const queryRef = ref()
const taskFlowCanvas = ref()
const graph = ref(null)

const taskTypeOptions = ref([
  { value: '1', label: '航天侦察装备子体系' },
  { value: '2', label: '太空态势感知装备子体系' },
  { value: '3', label: '太空攻防装备子体系' },
  { value: '4', label: '航天发射装备子体系' },
  { value: '5', label: '航天测运控装备子体系' },
  { value: '6', label: '航天装备联合任务体系' }
])

const data = reactive({
  form: {
    id: null,
    taskName: '',
    taskType: '',
    taskDescription: '',
    relatedCriterion: ''
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    type: undefined
  },
  rules: {
    taskName: [{ required: true, message: '任务名称不能为空', trigger: 'blur' }],
    taskType: [{ required: true, message: '任务类型不能为空', trigger: 'change' }]
  }
})

const { form, queryParams, rules } = toRefs(data)

const paletteItems = [
  { type: 'subtask', label: '子任务', short: 'T', desc: '承载具体业务动作', color: 'linear-gradient(135deg, #0ea5e9, #38bdf8)' },
  { type: 'and', label: '并行汇聚', short: 'AND', desc: '多任务同步满足', color: 'linear-gradient(135deg, #10b981, #34d399)' },
  { type: 'or', label: '分支选择', short: 'OR', desc: '满足任一条件继续', color: 'linear-gradient(135deg, #f59e0b, #fbbf24)' },
  { type: 'if', label: '条件判断', short: 'IF', desc: '通过条件切换路径', color: 'linear-gradient(135deg, #8b5cf6, #a78bfa)' }
]

const legendItems = [
  { label: '主任务节点', color: '#38bdf8' },
  { label: '并行逻辑', color: '#34d399' },
  { label: '分支逻辑', color: '#fbbf24' },
  { label: '条件判断', color: '#a78bfa' }
]

const overviewStats = computed(() => {
  const rows = taskList.value || []
  const typeCount = new Set(rows.map(item => item.taskType).filter(Boolean)).size
  const criterionCount = rows.filter(item => item.relatedCriterion).length
  return [
    { label: '当前分页任务', value: rows.length },
    { label: '全量记录', value: total.value },
    { label: '任务类型覆盖', value: `${typeCount} 类` },
    { label: '已绑定判据', value: `${criterionCount} 项` }
  ]
})

const graphPalette = computed(() => settingsStore.isDark ? {
  canvas: '#07111f',
  grid: 'rgba(110, 132, 166, 0.12)',
  subtask: { fill: '#102845', stroke: '#38bdf8', text: '#7dd3fc' },
  and: { fill: '#0f2c27', stroke: '#34d399', text: '#86efac' },
  or: { fill: '#3a2410', stroke: '#fbbf24', text: '#fde68a' },
  ifNode: { fill: '#22173e', stroke: '#a78bfa', text: '#ddd6fe' }
} : {
  canvas: '#f6fbff',
  grid: 'rgba(120, 154, 200, 0.14)',
  subtask: { fill: '#edf7ff', stroke: '#0ea5e9', text: '#0369a1' },
  and: { fill: '#ecfdf5', stroke: '#10b981', text: '#047857' },
  or: { fill: '#fff7ed', stroke: '#f59e0b', text: '#b45309' },
  ifNode: { fill: '#f5f3ff', stroke: '#8b5cf6', text: '#6d28d9' }
})

const orchestrationPriority = [
  { label: '任务节点结构清晰度', value: 84 },
  { label: '拖拽体验与提示', value: 76, tone: 'warning' },
  { label: '流程数据持久化', value: 68, tone: 'danger' }
]

function getTaskTypeLabel(value) {
  return taskTypeOptions.value.find(item => item.value === value)?.label || '未分类'
}

function getList() {
  loading.value = true
  listTaskManagement(queryParams.value).then(response => {
    taskList.value = response.rows || []
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

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(item => item.id)
}

function handleSubtaskImport() {
  proxy.$modal.msgInfo('子任务导入功能开发中')
}

function onDragStart(event, type) {
  event.dataTransfer.setData('application/x-component-type', type)
}

function nodeStyleByType(type) {
  const palette = graphPalette.value
  const styleMap = {
    subtask: { ...palette.subtask, width: 140, height: 58, shape: 'rect' },
    and: { ...palette.and, width: 76, height: 76, shape: 'circle' },
    or: { ...palette.or, width: 76, height: 76, shape: 'circle' },
    if: { ...palette.ifNode, width: 92, height: 72, shape: 'polygon' }
  }
  return styleMap[type]
}

function createNode(type, x, y, label) {
  const style = nodeStyleByType(type)
  if (!style || !graph.value) return null

  const attrs = style.shape === 'polygon'
    ? {
        body: { fill: style.fill, stroke: style.stroke, strokeWidth: 2 },
        label: { text: label, fill: style.text, fontSize: 12, fontWeight: 600 }
      }
    : {
        body: { fill: style.fill, stroke: style.stroke, strokeWidth: 2, rx: 14, ry: 14 },
        label: { text: label, fill: style.text, fontSize: 12, fontWeight: 600 }
      }

  return graph.value.addNode({
    shape: style.shape,
    x,
    y,
    width: style.width,
    height: style.height,
    attrs,
    ...(style.shape === 'polygon' ? { attrs: { body: { ...attrs.body, refPoints: '0,10 10,0 20,10 10,20' }, label: attrs.label } } : {})
  })
}

function createEdge(source, target, color = '#4cc9f0') {
  if (!graph.value || !source || !target) return
  graph.value.addEdge({
    source,
    target,
    attrs: {
      line: {
        stroke: color,
        strokeWidth: 2,
        targetMarker: {
          name: 'block',
          width: 10,
          height: 6
        }
      }
    }
  })
}

function createSampleTaskFlow() {
  const start = createNode('subtask', 110, 110, '通信干扰准备')
  const branch = createNode('or', 320, 102, 'OR')
  const attack = createNode('subtask', 470, 40, '初始干扰发起')
  const sustain = createNode('subtask', 470, 182, '持续压制保持')
  const judge = createNode('if', 720, 105, 'IF')
  const finish = createNode('subtask', 900, 110, '任务完成上报')

  createEdge(start, branch, '#38bdf8')
  createEdge(branch, attack, '#fbbf24')
  createEdge(branch, sustain, '#fbbf24')
  createEdge(attack, judge, '#a78bfa')
  createEdge(sustain, judge, '#34d399')
  createEdge(judge, finish, '#38bdf8')
}

function initTaskFlowCanvas() {
  if (!taskFlowCanvas.value) return
  disposeGraph()

  graph.value = new Graph({
    container: taskFlowCanvas.value,
    width: taskFlowCanvas.value.clientWidth,
    height: taskFlowCanvas.value.clientHeight,
    background: { color: graphPalette.value.canvas },
    grid: {
      size: 20,
      visible: true,
      type: 'mesh',
      args: {
        color: graphPalette.value.grid,
        thickness: 1
      }
    },
    mousewheel: {
      enabled: true,
      modifiers: 'ctrl',
      zoomAtMousePosition: true,
      minScale: 0.4,
      maxScale: 2.2
    },
    selecting: {
      enabled: true,
      rubberband: true,
      showNodeSelectionBox: true
    },
    connecting: {
      router: 'manhattan',
      connector: {
        name: 'rounded',
        args: { radius: 8 }
      },
      allowBlank: false,
      createEdge() {
        return new Shape.Edge({
          attrs: {
            line: {
              stroke: '#4cc9f0',
              strokeWidth: 2,
              targetMarker: { name: 'block', width: 10, height: 6 }
            }
          }
        })
      }
    },
    snapline: true,
    clipboard: true,
    keyboard: true
  })

  taskFlowCanvas.value.addEventListener('dragover', handleCanvasDragOver)
  taskFlowCanvas.value.addEventListener('drop', handleCanvasDrop)
  createSampleTaskFlow()
  graph.value.centerContent()
}

function handleCanvasDragOver(event) {
  event.preventDefault()
}

function handleCanvasDrop(event) {
  event.preventDefault()
  const type = event.dataTransfer.getData('application/x-component-type')
  if (!type || !graph.value) return
  const point = graph.value.clientToLocal(event.clientX, event.clientY)
  const labelMap = { subtask: '子任务', and: 'AND', or: 'OR', if: 'IF' }
  createNode(type, point.x - 60, point.y - 30, labelMap[type])
}

function resetCanvas() {
  if (!graph.value) return
  graph.value.clearCells()
  createSampleTaskFlow()
  graph.value.centerContent()
}

function disposeGraph() {
  if (taskFlowCanvas.value) {
    taskFlowCanvas.value.removeEventListener('dragover', handleCanvasDragOver)
    taskFlowCanvas.value.removeEventListener('drop', handleCanvasDrop)
  }
  if (graph.value) {
    graph.value.dispose()
    graph.value = null
  }
}

watch(open, value => {
  if (value) {
    nextTick(() => {
      initTaskFlowCanvas()
    })
  }
})

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增任务'
}

function handleUpdate(row) {
  reset()
  getTaskManagement(row.id).then(response => {
    Object.assign(form.value, response.data || {})
    open.value = true
    title.value = '修改任务'
  })
}

function handleDelete(row) {
  proxy.$modal.confirm(`是否确认删除任务编号为 "${row.id}" 的数据项？`).then(() => {
    return delTaskManagement(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function submitForm() {
  taskRef.value.validate(valid => {
    if (!valid) return
    const request = form.value.id != null ? updateTaskManagement(form.value) : addTaskManagement(form.value)
    request.then(() => {
      proxy.$modal.msgSuccess(form.value.id != null ? '修改成功' : '新增成功')
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
    taskName: '',
    taskType: '',
    taskDescription: '',
    relatedCriterion: ''
  }
  nextTick(() => {
    taskRef.value?.clearValidate()
  })
}

onBeforeUnmount(() => {
  disposeGraph()
})

getList()
</script>

<style scoped lang="scss">
.task-management-page {
  .task-dialog-shell {
    display: grid;
    gap: 18px;
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

  .orchestration-layout {
    display: grid;
    grid-template-columns: 320px minmax(0, 1fr);
    gap: 18px;
    min-height: 560px;
  }

  .orchestration-sidebar {
    display: grid;
    gap: 16px;
    align-content: start;
  }

  .palette-list {
    display: grid;
    gap: 12px;
    margin-top: 14px;
  }

  .palette-item {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 14px;
    border-radius: 16px;
    border: 1px solid rgba(86, 122, 173, 0.22);
    background: var(--mc-soft-block-bg);
    cursor: grab;
  }

  .palette-item__icon {
    min-width: 44px;
    height: 44px;
    padding: 0 10px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 12px;
    color: #eff6ff;
    font-size: 12px;
    font-weight: 700;
  }

  .palette-item__title {
    color: var(--mc-text-primary);
    font-weight: 600;
  }

  .palette-item__desc {
    margin-top: 6px;
    color: var(--mc-text-tertiary);
    font-size: 12px;
    line-height: 1.6;
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
    gap: 16px;
    padding: 18px 20px;
    border-bottom: 1px solid rgba(86, 122, 173, 0.14);
  }

  .task-flow-canvas {
    min-height: 490px;
    height: 100%;
  }
}

@media (max-width: 1280px) {
  .task-management-page .orchestration-layout {
    grid-template-columns: 1fr;
  }
}
</style>
