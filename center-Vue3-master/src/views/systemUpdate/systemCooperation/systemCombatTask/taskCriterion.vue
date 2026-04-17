<template>
  <div class="app-container mission-page task-criterion-page">
    <section class="page-hero">
      <div class="page-hero__body">
        <div>
          <span class="page-hero__eyebrow">Criterion Workbench</span>
          <h1 class="page-hero__title">任务判据设计与数据处理流</h1>
          <p class="page-hero__description">
            将任务判据的类型、适用范围、判断条件和阈值统一组织，并用数据处理流图说明输入、处理、输出与完成终止条件。
          </p>
          <div class="page-hero__actions">
            <el-button type="primary" icon="Plus" @click="handleAdd">新增判据</el-button>
            <el-button icon="Refresh" @click="getList">刷新数据</el-button>
          </div>
        </div>

        <div class="page-hero__panel">
          <div class="glass-card">
            <div class="panel-heading">
              <div>
                <h3 class="panel-title">判据概览</h3>
                <p class="panel-subtitle">当前分页判据规模、类型和适用范围</p>
              </div>
              <span class="status-chip status-chip--success">规则在线</span>
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
                <h3 class="panel-title">设计关注点</h3>
                <p class="panel-subtitle">判据和数据处理流图需要一体化表达</p>
              </div>
            </div>
            <div class="score-bars">
              <div v-for="item in criterionPriority" :key="item.label" class="score-bar">
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
          <h3 class="panel-title">判据筛选</h3>
          <p class="panel-subtitle">按名称和类型筛选任务判据</p>
        </div>
      </div>
      <el-form ref="queryRef" :model="queryParams" label-width="110px">
        <div class="form-grid form-grid--2">
          <div class="field-card">
            <el-form-item label="判据名称" prop="name">
              <el-input v-model="queryParams.name" placeholder="请输入任务判据名称" clearable @keyup.enter="handleQuery" />
            </el-form-item>
          </div>
          <div class="field-card">
            <el-form-item label="判据类型" prop="type">
              <el-select v-model="queryParams.type" placeholder="请选择任务判据类型" clearable>
                <el-option v-for="item in criterionTypeOptions" :key="item" :label="item" :value="item" />
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
          <h3 class="panel-title">任务判据列表</h3>
          <p class="panel-subtitle">判据字段直接映射到右侧数据流图说明中</p>
        </div>
        <span class="status-chip status-chip--warning">共 {{ total }} 条</span>
      </div>
      <el-table v-loading="loading" :data="criterionList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="判据名称" prop="criterionName" min-width="180" show-overflow-tooltip />
        <el-table-column label="判据类型" prop="criterionType" min-width="140" />
        <el-table-column label="适用类型" prop="applicableType" min-width="160" show-overflow-tooltip />
        <el-table-column label="判断条件" prop="judgmentCondition" width="120" />
        <el-table-column label="阈值" prop="criterionThreshold" width="120" />
        <el-table-column label="单位" prop="criterionUnit" width="120" />
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

    <el-dialog v-model="open" :title="title" width="1320px" append-to-body class="criterion-dialog" @closed="disposeGraph">
      <div class="criterion-dialog-shell">
        <div class="panel-card">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">判据信息</h3>
              <p class="panel-subtitle">配置判据名称、类型、适用范围、阈值与单位</p>
            </div>
            <span class="status-chip status-chip--info">规则编辑</span>
          </div>

          <el-form ref="criterionRef" :model="form" :rules="rules" label-position="top">
            <div class="form-grid form-grid--2">
              <div class="field-card">
                <el-form-item label="任务判据名称" prop="criterionName">
                  <el-input v-model="form.criterionName" placeholder="请输入任务判据名称" />
                </el-form-item>
              </div>
              <div class="field-card">
                <el-form-item label="任务判据类型" prop="criterionType">
                  <el-select v-model="form.criterionType" placeholder="请选择任务判据类型">
                    <el-option v-for="item in criterionTypeOptions" :key="item" :label="item" :value="item" />
                  </el-select>
                </el-form-item>
              </div>
              <div class="field-card">
                <el-form-item label="适用类型" prop="applicableType">
                  <el-select v-model="form.applicableType" placeholder="请选择适用类型">
                    <el-option v-for="item in applicableTypeOptions" :key="item" :label="item" :value="item" />
                  </el-select>
                </el-form-item>
              </div>
              <div class="field-card">
                <el-form-item label="判断条件" prop="judgmentCondition">
                  <el-select v-model="form.judgmentCondition" placeholder="请选择判断条件">
                    <el-option v-for="item in judgmentConditionOptions" :key="item" :label="item" :value="item" />
                  </el-select>
                </el-form-item>
              </div>
              <div class="field-card">
                <el-form-item label="判据阈值" prop="criterionThreshold">
                  <el-input v-model="form.criterionThreshold" placeholder="请输入判据阈值" />
                </el-form-item>
              </div>
              <div class="field-card">
                <el-form-item label="判据单位" prop="criterionUnit">
                  <el-select v-model="form.criterionUnit" placeholder="请选择判据单位">
                    <el-option v-for="item in criterionUnitOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
              </div>
            </div>
          </el-form>
        </div>

        <div class="panel-card">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">数据处理流图</h3>
              <p class="panel-subtitle">从输入、处理、输出到完成/终止条件的深色流程视图</p>
            </div>
            <div class="legend-row">
              <span v-for="item in flowLegend" :key="item.label" class="legend-chip">
                <span class="legend-chip__dot" :style="{ background: item.color }"></span>
                {{ item.label }}
              </span>
            </div>
          </div>
          <div class="flow-chart-shell">
            <div ref="flowChartRef" class="flow-chart"></div>
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

<script setup name="TaskCriterion">
import { computed, nextTick, onBeforeUnmount, reactive, ref, toRefs, watch } from 'vue'
import { Graph } from '@antv/x6'
import useSettingsStore from '@/store/modules/settings'
import {
  listTaskCriterion,
  getTaskCriterion,
  delTaskCriterion,
  addTaskCriterion,
  updateTaskCriterion
} from '@/api/systemPlus/systemCooperation/taskCriterion'

const { proxy } = getCurrentInstance()
const settingsStore = useSettingsStore()

const criterionList = ref([])
const loading = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')
const criterionRef = ref()
const flowChartRef = ref()
const flowGraph = ref(null)

const criterionTypeOptions = ['单装', '装备系统', '装备子系统']
const applicableTypeOptions = ['作战任务', '装备性能', '系统效能', '环境条件']
const judgmentConditionOptions = ['=', '>', '<', '>=', '<=', '!=']
const criterionUnitOptions = [
  { label: '无单位', value: '' },
  { label: '秒', value: '秒' },
  { label: '分钟', value: '分钟' },
  { label: '小时', value: '小时' },
  { label: '米', value: '米' },
  { label: '千米', value: '千米' },
  { label: '百分比', value: '%' }
]

const flowLegend = [
  { label: '输入节点', color: '#34d399' },
  { label: '处理节点', color: '#38bdf8' },
  { label: '输出节点', color: '#fbbf24' },
  { label: '完成 / 终止', color: '#a78bfa' }
]

const data = reactive({
  form: {
    id: null,
    criterionName: '',
    criterionType: '',
    applicableType: '',
    judgmentCondition: '',
    criterionThreshold: '',
    criterionUnit: ''
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    type: undefined
  },
  rules: {
    criterionName: [{ required: true, message: '任务判据名称不能为空', trigger: 'blur' }],
    criterionType: [{ required: true, message: '任务判据类型不能为空', trigger: 'change' }]
  }
})

const { form, queryParams, rules } = toRefs(data)

const overviewStats = computed(() => {
  const rows = criterionList.value || []
  const typeCount = new Set(rows.map(item => item.criterionType).filter(Boolean)).size
  const conditionCount = new Set(rows.map(item => item.judgmentCondition).filter(Boolean)).size
  return [
    { label: '当前分页判据', value: rows.length },
    { label: '全量记录', value: total.value },
    { label: '判据类型', value: `${typeCount} 类` },
    { label: '判断条件', value: `${conditionCount} 类` }
  ]
})

const criterionPriority = [
  { label: '判据字段完整性', value: 86 },
  { label: '输入输出语义映射', value: 74, tone: 'warning' },
  { label: '终止条件展示能力', value: 66, tone: 'danger' }
]

const flowPalette = computed(() => settingsStore.isDark ? {
  canvas: '#07111f',
  grid: 'rgba(110, 132, 166, 0.12)',
  inputFill: '#0f2c27',
  inputStroke: '#34d399',
  inputText: '#86efac',
  processFill: '#102845',
  processStroke: '#38bdf8',
  processText: '#7dd3fc',
  outputFill: '#3a2410',
  outputStroke: '#fbbf24',
  outputText: '#fde68a',
  completeFill: '#22173e',
  completeStroke: '#a78bfa',
  completeText: '#ddd6fe',
  terminateFill: '#341723',
  terminateStroke: '#fb7185',
  terminateText: '#fecdd3'
} : {
  canvas: '#f6fbff',
  grid: 'rgba(120, 154, 200, 0.14)',
  inputFill: '#ecfdf5',
  inputStroke: '#10b981',
  inputText: '#047857',
  processFill: '#edf7ff',
  processStroke: '#0ea5e9',
  processText: '#0369a1',
  outputFill: '#fff7ed',
  outputStroke: '#f59e0b',
  outputText: '#b45309',
  completeFill: '#f5f3ff',
  completeStroke: '#8b5cf6',
  completeText: '#6d28d9',
  terminateFill: '#fff1f2',
  terminateStroke: '#e11d48',
  terminateText: '#be123c'
})

function getList() {
  loading.value = true
  listTaskCriterion(queryParams.value).then(response => {
    criterionList.value = response.rows || []
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

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增任务判据'
}

function handleUpdate(row) {
  reset()
  getTaskCriterion(row.id).then(response => {
    Object.assign(form.value, response.data || {})
    open.value = true
    title.value = '修改任务判据'
  })
}

function handleDelete(row) {
  proxy.$modal.confirm(`是否确认删除任务判据编号为 "${row.id}" 的数据项？`).then(() => {
    return delTaskCriterion(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function buildFlowChart() {
  if (!flowChartRef.value) return
  disposeGraph()

  flowGraph.value = new Graph({
    container: flowChartRef.value,
    width: flowChartRef.value.clientWidth,
    height: flowChartRef.value.clientHeight,
    background: { color: flowPalette.value.canvas },
    grid: {
      visible: true,
      size: 20,
      type: 'mesh',
      args: {
        color: flowPalette.value.grid,
        thickness: 1
      }
    },
    interacting: false
  })

  const input1 = flowGraph.value.addNode({
    shape: 'circle',
    x: 80,
    y: 120,
    width: 64,
    height: 64,
    attrs: {
      body: { fill: flowPalette.value.inputFill, stroke: flowPalette.value.inputStroke, strokeWidth: 2 },
      label: { text: '输入A', fill: flowPalette.value.inputText, fontSize: 12, fontWeight: 600 }
    }
  })
  const input2 = flowGraph.value.addNode({
    shape: 'circle',
    x: 80,
    y: 250,
    width: 64,
    height: 64,
    attrs: {
      body: { fill: flowPalette.value.inputFill, stroke: flowPalette.value.inputStroke, strokeWidth: 2 },
      label: { text: '输入B', fill: flowPalette.value.inputText, fontSize: 12, fontWeight: 600 }
    }
  })
  const process = flowGraph.value.addNode({
    shape: 'rect',
    x: 300,
    y: 150,
    width: 180,
    height: 112,
    attrs: {
      body: { fill: flowPalette.value.processFill, stroke: flowPalette.value.processStroke, strokeWidth: 2, rx: 16, ry: 16 },
      label: { text: '任务判据处理', fill: flowPalette.value.processText, fontSize: 16, fontWeight: 700 }
    }
  })
  const output1 = flowGraph.value.addNode({
    shape: 'circle',
    x: 640,
    y: 120,
    width: 64,
    height: 64,
    attrs: {
      body: { fill: flowPalette.value.outputFill, stroke: flowPalette.value.outputStroke, strokeWidth: 2 },
      label: { text: '输出A', fill: flowPalette.value.outputText, fontSize: 12, fontWeight: 600 }
    }
  })
  const output2 = flowGraph.value.addNode({
    shape: 'circle',
    x: 640,
    y: 250,
    width: 64,
    height: 64,
    attrs: {
      body: { fill: flowPalette.value.outputFill, stroke: flowPalette.value.outputStroke, strokeWidth: 2 },
      label: { text: '输出B', fill: flowPalette.value.outputText, fontSize: 12, fontWeight: 600 }
    }
  })
  const complete = flowGraph.value.addNode({
    shape: 'rect',
    x: 300,
    y: 28,
    width: 160,
    height: 54,
    attrs: {
      body: { fill: flowPalette.value.completeFill, stroke: flowPalette.value.completeStroke, strokeWidth: 2, rx: 14, ry: 14 },
      label: { text: '完成条件', fill: flowPalette.value.completeText, fontSize: 13, fontWeight: 600 }
    }
  })
  const terminate = flowGraph.value.addNode({
    shape: 'rect',
    x: 300,
    y: 346,
    width: 160,
    height: 54,
    attrs: {
      body: { fill: flowPalette.value.terminateFill, stroke: flowPalette.value.terminateStroke, strokeWidth: 2, rx: 14, ry: 14 },
      label: { text: '终止条件', fill: flowPalette.value.terminateText, fontSize: 13, fontWeight: 600 }
    }
  })

  const edges = [
    [input1, process, '#34d399'],
    [input2, process, '#34d399'],
    [process, output1, '#fbbf24'],
    [process, output2, '#fbbf24'],
    [complete, process, '#a78bfa', '6,4'],
    [process, terminate, '#fb7185', '6,4']
  ]

  edges.forEach(([source, target, color, dash]) => {
    flowGraph.value.addEdge({
      source,
      target,
      attrs: {
        line: {
          stroke: color,
          strokeWidth: 2,
          strokeDasharray: dash,
          targetMarker: {
            name: 'block',
            width: 10,
            height: 6
          }
        }
      }
    })
  })

  flowGraph.value.centerContent()
}

watch(open, value => {
  if (value) {
    nextTick(() => {
      buildFlowChart()
    })
  }
})

function submitForm() {
  criterionRef.value.validate(valid => {
    if (!valid) return
    const request = form.value.id != null ? updateTaskCriterion(form.value) : addTaskCriterion(form.value)
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
    criterionName: '',
    criterionType: '',
    applicableType: '',
    judgmentCondition: '',
    criterionThreshold: '',
    criterionUnit: ''
  }
  nextTick(() => {
    criterionRef.value?.clearValidate()
  })
}

function disposeGraph() {
  if (flowGraph.value) {
    flowGraph.value.dispose()
    flowGraph.value = null
  }
}

onBeforeUnmount(() => {
  disposeGraph()
})

getList()
</script>

<style scoped lang="scss">
.task-criterion-page {
  .criterion-dialog-shell {
    display: grid;
    gap: 18px;
  }

  .flow-chart-shell {
    min-height: 460px;
    border-radius: 20px;
    border: 1px solid rgba(86, 122, 173, 0.2);
    background: color-mix(in srgb, var(--mc-bg-elevated) 88%, transparent);
    overflow: hidden;
  }

  .flow-chart {
    min-height: 460px;
    height: 100%;
  }
}
</style>
