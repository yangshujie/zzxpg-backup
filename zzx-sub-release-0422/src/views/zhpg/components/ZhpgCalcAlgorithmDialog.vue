<template>
  <el-dialog
    :title="title"
    :model-value="visible"
    class="zhpg-calc-algorithm-dialog"
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
    @update:model-value="emit('update:visible', $event)"
    @open="onDialogOpen"
    @closed="onDialogClosed"
  >
    <div v-if="showWorkModeSelector" class="dialog-topbar">
      <span class="topbar-label">工作模式</span>
      <el-radio-group :model-value="localWorkMode" @update:model-value="handleWorkModeChange">
        <el-radio-button v-for="item in workModeOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio-button>
      </el-radio-group>
      <span class="topbar-tip">拖拽控件到画布后自由连线，点击节点在右侧配置详情。</span>
    </div>

    <div class="algorithm-config">
      <aside class="flow-palette">
        <div class="panel-title">控件库</div>
        <div
          v-for="item in paletteItems"
          :key="item.kind"
          class="palette-card"
          :class="`palette-card--${item.kind}`"
          draggable="true"
          @dragstart="event => onPaletteDragStart(event, item.kind)"
        >
          <span class="palette-icon">{{ item.icon }}</span>
          <div>
            <div class="palette-name">{{ item.name }}</div>
            <div class="palette-desc">{{ item.desc }}</div>
          </div>
        </div>
        <div class="palette-hint">连线规则：数据源 → 算法盒 → 结果；算法盒之间可继续串联或分支。</div>
        <el-button class="palette-action" @click="autoLayout">整理画布</el-button>
      </aside>

      <section class="canvas-shell">
        <div class="canvas-toolbar">
          <span>算法画布</span>
          <div>
            <el-button link type="primary" @click="fitCanvas">适配视图</el-button>
            <el-button link type="danger" :disabled="!selectedCellId" @click="removeSelectedCell">删除选中</el-button>
          </div>
        </div>
        <div ref="canvasRef" class="flow-canvas" @dragover.prevent @drop="onCanvasDrop" />
      </section>

      <aside class="flow-inspector">
        <template v-if="selectedNodeData">
          <div class="panel-title">
            <span>{{ selectedNodeTitle }}</span>
            <el-tag size="small" effect="plain">{{ selectedNodeTypeLabel }}</el-tag>
          </div>
          <el-form label-position="top" class="inspector-form">
            <template v-if="selectedNodeData.kind === 'data'">
              <el-form-item label="名称"><el-input v-model="selectedNodeData.name" placeholder="如：数据源名称" clearable /></el-form-item>
              
              <div class="data-source-config-header">
                <span>数据配置摘要</span>
                <el-button type="primary" link @click="externalDataVisible = true">配置外部数据...</el-button>
              </div>

              <el-form-item label="分中心">
                <el-select 
                  v-model="selectedNodeData.source" 
                  disabled
                  placeholder="从弹窗选择后显示" 
                  style="width: 100%"
                >
                  <el-option v-for="o in sourceCenterOptions" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="目录">
                <el-input :model-value="selectedNodeData.directory" readonly placeholder="从弹窗选择后显示" />
              </el-form-item>
              
              <el-form-item label="字段">
                <div class="fields-tag-container is-readonly">
                  <el-tooltip
                    v-for="(f, idx) in parseFields(selectedNodeData.fieldsText)"
                    :key="idx"
                    :content="findTechnicalNameByLabel(f)"
                    placement="top"
                  >
                    <el-tag size="small" class="field-tag">
                      {{ f }}
                    </el-tag>
                  </el-tooltip>
                  <span v-if="!selectedNodeData.fieldsText" class="fields-placeholder">尚未选择字段</span>
                </div>
              </el-form-item>
              <el-form-item label="试验阶段"><el-input v-model="selectedNodeData.taskStage" placeholder="请输入或选择试验阶段" clearable /></el-form-item>
              <el-form-item label="任务时间">
                <el-date-picker
                  :model-value="taskTimeRangeModel(selectedNodeData)"
                  type="datetimerange"
                  range-separator="至"
                  start-placeholder="开始时间"
                  end-placeholder="结束时间"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  class="inspector-daterange"
                  clearable
                  @update:model-value="v => applyTaskTimeRange(selectedNodeData, v)"
                />
              </el-form-item>
              <el-form-item label="所属试验任务"><el-input v-model="selectedNodeData.experimentalTask" placeholder="请输入所属试验任务" clearable /></el-form-item>
              <el-form-item label="描述"><el-input v-model="selectedNodeData.description" type="textarea" :rows="3" placeholder="请输入数据源描述" clearable /></el-form-item>
            </template>

            <template v-else-if="selectedNodeData.kind === 'algorithm'">
              <el-form-item label="算法类型">
                <el-select v-model="selectedNodeData.algorithmType" clearable placeholder="先选择算法类型" style="width: 100%" @change="onSelectedAlgorithmTypeChange">
                  <el-option v-for="opt in algorithmTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="具体算法">
                <el-select
                  v-model="selectedNodeData.algorithmId"
                  filterable
                  clearable
                  :disabled="!selectedNodeData.algorithmType"
                  :loading="algorithmLoading"
                  placeholder="再选择具体算法"
                  no-data-text="当前类型下暂无可选算法"
                  style="width: 100%"
                  @change="onSelectedAlgorithmChange"
                >
                  <el-option v-for="opt in getAlgorithmOptionsByType(selectedNodeData.algorithmType)" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
              <div class="algo-param-row">
                <el-button type="primary" plain :disabled="!selectedNodeData.algorithmId" @click="openParamDrawerForSelected">配置参数</el-button>
                <el-tag v-if="paramFieldCount(selectedNodeData) > 0" type="info">{{ paramFieldCount(selectedNodeData) }} 项参数</el-tag>
              </div>
            </template>

            <el-alert v-else title="结果节点无需额外配置" type="warning" :closable="false" show-icon />
          </el-form>
        </template>
        <el-empty v-else description="点击画布上的控件后在这里配置详情" />
      </aside>
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="save">{{ saveButtonText }}</el-button>
    </template>

    <el-drawer v-model="paramDrawerVisible" title="算法参数" size="440px" append-to-body destroy-on-close @closed="onParamDrawerClosed">
      <div v-loading="paramLoading" class="param-drawer-body">
        <p v-if="paramEditorMeta" class="param-drawer-meta">{{ paramEditorMeta }}</p>
        <p v-if="onlyConfigParamsInDrawer && paramEditorMeta" class="param-drawer-hint">此处仅填写配置参数，输入数据由画布中的数据源连线在运行时提供。</p>
        <el-form v-if="paramFormFields.length" label-position="top" class="param-form">
          <el-form-item v-for="p in paramFormFields" :key="p.paramField" :required="p.requiredFlag === 1">
            <template #label><span>{{ p.paramName || p.paramField }}</span><span v-if="p.paramDesc" class="param-desc">{{ p.paramDesc }}</span></template>
            <el-input v-if="!p.paramType || p.paramType === 'string' || p.paramType === 'tel' || p.paramType === 'template'" v-model="paramEditValues[p.paramField]" :placeholder="defaultPlaceholder(p)" clearable />
            <el-input-number v-else-if="p.paramType === 'number'" v-model="paramNumberModels[p.paramField]" controls-position="right" style="width: 100%" @change="v => syncNumberParam(p.paramField, v)" />
            <el-switch v-else-if="p.paramType === 'boolean'" v-model="paramEditValues[p.paramField]" active-value="true" inactive-value="false" />
            <el-input v-else-if="p.paramType === 'date'" v-model="paramEditValues[p.paramField]" placeholder="YYYY-MM-DD HH:mm:ss" clearable />
            <el-input v-else v-model="paramEditValues[p.paramField]" type="textarea" :rows="2" :placeholder="defaultPlaceholder(p)" />
          </el-form-item>
        </el-form>
        <el-empty v-else-if="!paramLoading && paramDrawerVisible" description="该算法未配置可调参数" />
      </div>
      <template #footer>
        <el-button @click="paramDrawerVisible = false">取消</el-button>
        <el-button type="primary" @click="applyParamDrawer">确定</el-button>
      </template>
    </el-drawer>

    <ZhpgExternalDataDialog
      v-model:visible="externalDataVisible"
      @selected="handleExternalDataSelected"
    />
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { Graph } from '@antv/x6'
import { ElMessage } from 'element-plus'
import { listAllAlgorithm, getAlgorithm } from '@/api/zhpg/algorithm'
import {
  allocFlowNodeIdForNewNode,
  parseLeafCalcToWorkspace,
  serializeCalcWorkspace,
  serializeWorkspaceToComputeRule,
  validateCalcWorkspace
} from '@/utils/zhpg/calcMethodAlgorithm'
import {
  INPUT_PORT_COUNT,
  anchorArgsForSide,
  assignCalcAlgorithmLayout,
  buildOrthogonalEdgeVertices,
  chooseConnectionSides,
  distributeIncomingPorts,
} from '@/utils/zhpg/calcAlgorithmLayout'
import { ZHPG_WORK_MODE, ZHPG_WORK_MODE_OPTIONS } from '@/constants/zhpgWorkMode'
import { ZHPG_DATA_SOURCE_CENTER_OPTIONS } from '@/constants/zhpgIndicatorSystem'
import ZhpgExternalDataDialog from './ZhpgExternalDataDialog.vue'

const props = defineProps({
  modelValue: { type: [String, Object], default: '' },
  leafUid: { type: String, default: '' },
  visible: { type: Boolean, default: false },
  workMode: { type: String, default: ZHPG_WORK_MODE.INTERNAL_CIRCULATION },
  showWorkModeSelector: { type: Boolean, default: true },
  title: { type: String, default: '配置算法' },
  saveButtonText: { type: String, default: '保存配置' },
  onlyPublished: { type: Boolean, default: true },
  onlyConfigParamsInDrawer: { type: Boolean, default: false },
  defaultSourceCenter: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'update:visible', 'update:workMode', 'saved'])

const sourceCenterOptions = ZHPG_DATA_SOURCE_CENTER_OPTIONS
const workModeOptions = ZHPG_WORK_MODE_OPTIONS
const localWorkMode = ref(props.workMode)
const algorithmLoading = ref(false)
const algorithmCatalog = ref([])
const canvasRef = ref(null)
const graphRef = ref(null)
const selectedCellId = ref('')
const selectedNodeData = ref(null)
const suppressSelectedWatch = ref(false)
const externalDataVisible = ref(false)
const openFieldInput = ref(false)
const newFieldInput = ref(false)
const fieldInputRef = ref(null)

const paletteItems = [
  { kind: 'data', name: '数据源', desc: '输入目录、字段、筛选条件', icon: 'D' },
  { kind: 'algorithm', name: '算法盒', desc: '选择算法并配置参数', icon: 'A' },
  { kind: 'result', name: '结果', desc: '计算链路的输出终点', icon: 'R' }
]

const UNCLASSIFIED_ALGO_TYPE = '__UNCLASSIFIED__'

function normalizeAlgorithmType(type) {
  const value = String(type || '').trim()
  return value || UNCLASSIFIED_ALGO_TYPE
}

const algorithmTypeOptions = computed(() => {
  const map = new Map()
  for (const a of algorithmCatalog.value || []) {
    const typeValue = normalizeAlgorithmType(a.algorithmType)
    const typeLabel = typeValue === UNCLASSIFIED_ALGO_TYPE ? '未分类' : typeValue
    if (!map.has(typeValue)) map.set(typeValue, typeLabel)
  }
  return Array.from(map.entries()).map(([value, label]) => ({ value, label }))
})

const algorithmSelectOptions = computed(() =>
  (algorithmCatalog.value || []).map(a => ({
    value: String(a.id),
    label: a.algorithmName || `算法 #${a.id}`,
    algorithmType: normalizeAlgorithmType(a.algorithmType)
  }))
)

const selectedNodeTitle = computed(() => {
  const data = selectedNodeData.value
  if (!data) return ''
  if (data.kind === 'data') return data.name || '数据源'
  if (data.kind === 'algorithm') return algorithmLabel(data.algorithmId) || '算法盒'
  return '结果'
})

const selectedNodeTypeLabel = computed(() => {
  const data = selectedNodeData.value
  if (!data) return ''
  if (data.kind === 'data') return '数据源'
  if (data.kind === 'algorithm') return '算法盒'
  return '结果'
})

watch(() => props.workMode, v => {
  if (v) localWorkMode.value = v
})

watch(selectedNodeData, data => {
  if (suppressSelectedWatch.value || !data || !selectedCellId.value) return
  const node = graphRef.value?.getCellById(selectedCellId.value)
  if (!node || !node.isNode?.()) return
  node.setData({ ...data }, { deep: false })
  updateNodeVisual(node)
}, { deep: true })

function getAlgorithmOptionsByType(type) {
  if (!type) return []
  return algorithmSelectOptions.value.filter(opt => opt.algorithmType === type)
}

function algorithmLabel(id) {
  if (!id) return ''
  return algorithmSelectOptions.value.find(opt => opt.value === String(id))?.label || String(id)
}

function syncStepAlgorithmTypeFromId(step) {
  if (!step || !step.algorithmId) return
  const hit = algorithmSelectOptions.value.find(opt => opt.value === String(step.algorithmId))
  if (hit) step.algorithmType = hit.algorithmType
}

function extractListPayload(res) {
  if (res == null) return []
  if (Array.isArray(res)) return res
  if (Array.isArray(res.data)) return res.data
  if (Array.isArray(res.rows)) return res.rows
  return []
}

async function loadAlgorithms() {
  algorithmLoading.value = true
  try {
    const q = {}
    if (props.onlyPublished) q.publishStatus = 'PUBLISHED'
    const res = await listAllAlgorithm(q)
    algorithmCatalog.value = extractListPayload(res)
    graphRef.value?.getNodes().forEach(node => {
      const data = node.getData()
      if (data?.kind === 'algorithm') {
        syncStepAlgorithmTypeFromId(data)
        node.setData(data, { deep: false })
        updateNodeVisual(node)
      }
    })
  } catch (e) {
    algorithmCatalog.value = []
    ElMessage.error(e?.message || '加载算法列表失败')
  } finally {
    algorithmLoading.value = false
  }
}

function createDefaultData(kind) {
  if (kind === 'data') {
    const def = String(props.defaultSourceCenter || '').trim()
    const source = (def === '无' || def === '') ? '' : def
    return { kind, name: '', directory: '', source, fieldsText: '', taskStage: '', taskTimeStart: '', taskTimeEnd: '', experimentalTask: '', description: '' }
  }
  if (kind === 'algorithm') return { kind, algorithmType: '', algorithmId: '', params: {} }
  return { kind }
}

function createPorts(kind) {
  const common = {
    attrs: {
      circle: {
        r: 14,
        magnet: true,
        stroke: 'transparent',
        strokeWidth: 0,
        fill: 'transparent'
      }
    }
  }
  const groups = {
    left: { ...common, position: 'left' },
    right: { ...common, position: 'right' },
    top: { ...common, position: 'top' },
    bottom: { ...common, position: 'bottom' }
  }
  return {
    groups,
    items: ['left', 'right', 'top', 'bottom'].map(side => ({ id: `side_${side}`, group: side }))
  }
}

function nodeStyle(kind) {
  const theme = graphTheme()
  if (kind === 'data') return { fill: theme.dataFill, stroke: theme.dataStroke }
  if (kind === 'algorithm') return { fill: theme.algorithmFill, stroke: theme.algorithmStroke }
  return { fill: theme.resultFill, stroke: theme.resultStroke }
}

function nodeLabel(data) {
  if (data.kind === 'data') return data.name || '数据源'
  if (data.kind === 'algorithm') return algorithmLabel(data.algorithmId) || '算法盒'
  return '结果'
}

function fitNodeLabel(text) {
  const value = String(text || '').trim()
  if (value.length <= 8) return value
  if (value.length <= 16) return `${value.slice(0, 8)}\n${value.slice(8)}`
  return `${value.slice(0, 8)}\n${value.slice(8, 15)}...`
}

function isDarkMode() {
  if (typeof document === 'undefined') return false
  if (document.documentElement.classList.contains('dark')) return true
  if (document.documentElement.getAttribute('data-theme') === 'dark-theme') return true
  if (document.body.getAttribute('data-theme') === 'dark') return true
  return false
}

function graphTheme() {
  if (isDarkMode()) {
    return {
      canvasBg: '#07111f',
      grid: 'rgba(103, 232, 249, 0.08)',
      label: '#d8e7f5',
      edge: '#67e8f9',
      dataFill: 'rgba(16, 185, 129, 0.18)',
      dataStroke: '#34d399',
      algorithmFill: 'rgba(14, 165, 233, 0.18)',
      algorithmStroke: '#38bdf8',
      resultFill: 'rgba(245, 158, 11, 0.18)',
      resultStroke: '#f59e0b'
    }
  }
  return {
    canvasBg: '#f8fbff',
    grid: '#e8edf5',
    label: '#303133',
    edge: '#292929',
    dataFill: '#f0f9eb',
    dataStroke: '#67c23a',
    algorithmFill: '#ecf5ff',
    algorithmStroke: '#409eff',
    resultFill: '#fdf6ec',
    resultStroke: '#e6a23c'
  }
}

function addFlowNode(kind, x, y, data = {}, id = undefined) {
  if (kind === 'result' && hasResultNode() && !id) {
    ElMessage.info('结果节点只需要一个，已选中现有结果节点')
    const existing = graphRef.value.getNodes().find(node => node.getData()?.kind === 'result')
    if (existing) selectNode(existing)
    return existing
  }
  const mergedData = { ...createDefaultData(kind), ...data, kind }
  const style = nodeStyle(kind)
  return graphRef.value.addNode({
    id: id || allocFlowNodeIdForNewNode(),
    shape: 'rect',
    x,
    y,
      width: 240,
      height: 88,
    attrs: {
      body: { rx: 12, ry: 12, fill: style.fill, stroke: style.stroke, strokeWidth: 2 },
      label: {
        text: fitNodeLabel(nodeLabel(mergedData)),
        fill: graphTheme().label,
        fontSize: 14,
        fontWeight: 700,
        lineHeight: 18,
        textAnchor: 'middle',
        textVerticalAnchor: 'middle'
      }
    },
    ports: createPorts(kind),
    data: mergedData
  })
}

function updateNodeVisual(node) {
  const data = node.getData()
  if (data) node.attr('label/text', fitNodeLabel(nodeLabel(data)))
}

function edgeAttrs() {
  const theme = graphTheme()
  return {
    line: {
      stroke: theme.edge,
      strokeWidth: 2,
      targetMarker: {
        name: 'classic',
        size: 10,
        fill: theme.edge,
        stroke: theme.edge
      }
    }
  }
}

function edgeShape() {
  return {
    router: { name: 'manhattan', args: { padding: 24 } },
    connector: { name: 'rounded', args: { radius: 10 } },
    attrs: edgeAttrs(),
    zIndex: 1
  }
}

function sideFromPortId(portId) {
  const side = String(portId || '').replace(/^side_/, '')
  return ['left', 'right', 'top', 'bottom'].includes(side) ? side : ''
}

function initGraph() {
  if (!canvasRef.value) return
  graphRef.value?.dispose()
  graphRef.value = new Graph({
    container: canvasRef.value,
    autoResize: true,
    background: { color: graphTheme().canvasBg },
    grid: { size: 12, visible: true, type: 'mesh', args: { color: graphTheme().grid, thickness: 1 } },
    panning: true,
    mousewheel: { enabled: true, minScale: 0.45, maxScale: 2.2, factor: 1.12 },
    selecting: {
      enabled: true,
      multiple: false,
      rubberband: false,
      showNodeSelectionBox: true,
      showEdgeSelectionBox: true
    },
    connecting: {
      snap: true,
      allowBlank: false,
      allowLoop: false,
      allowNode: false,
      allowEdge: false,
      highlight: true,
      router: { name: 'manhattan', args: { padding: 24 } },
      connector: { name: 'rounded', args: { radius: 10 } },
      createEdge() {
        return this.createEdge(edgeShape())
      },
      validateConnection({ sourceCell, targetCell, sourceMagnet, targetMagnet }) {
        if (!sourceCell || !targetCell || sourceCell === targetCell) return false
        if (!sideFromPortId(sourceMagnet?.getAttribute('port'))) return false
        if (!sideFromPortId(targetMagnet?.getAttribute('port'))) return false
        const sourceKind = sourceCell.getData()?.kind
        const targetKind = targetCell.getData()?.kind
        return sourceKind !== 'result' && targetKind !== 'data'
      }
    }
  })
  graphRef.value.on('node:click', ({ node }) => selectNode(node))
  graphRef.value.on('blank:click', clearSelected)
  graphRef.value.on('edge:click', ({ edge }) => selectEdge(edge))

  graphRef.value.on('edge:connected', ({ edge }) => {
    applyCellVisual(edge, edge.id === selectedCellId.value)
    edge.setData({
      ...(edge.getData() || {}),
      targetPortId: edge.getData()?.targetPortId || 'in_3',
      sourcePortId: edge.getData()?.sourcePortId || 'out_3'
    }, { deep: false })
    applyDistributedEdgePorts()
    tidyCanvasEdges()
  })
  graphRef.value.on('node:moved', () => tidyCanvasEdges())
}

function applyCellVisual(cell, isSelected) {
  if (!cell || !cell.attr) return
  if (cell.isNode && cell.isNode()) {
    if (isSelected) {
      cell.attr('body/stroke', '#ffb022')
      cell.attr('body/strokeWidth', 4)
      cell.attr('body/style', 'filter: drop-shadow(0 0 6px rgba(255,176,34,0.4))')
      cell.setZIndex(10)
    } else {
      const style = nodeStyle(cell.getData()?.kind)
      cell.attr('body/stroke', style.stroke)
      cell.attr('body/fill', style.fill)
      cell.attr('label/fill', graphTheme().label)
      cell.attr('body/strokeWidth', 2)
      cell.attr('body/style', 'filter: none')
      cell.setZIndex(2)
    }
  } else if (cell.isEdge && cell.isEdge()) {
    if (isSelected) {
      cell.attr({
        line: {
          stroke: '#ffb022',
          strokeWidth: 4,
          targetMarker: {
            fill: '#ffb022',
            stroke: '#ffb022'
          }
        }
      })
      cell.setZIndex(10)
    } else {
      cell.attr({
        line: {
          stroke: graphTheme().edge,
          strokeWidth: 2,
          targetMarker: {
            fill: graphTheme().edge,
            stroke: graphTheme().edge
          }
        }
      })
      cell.setZIndex(1)
    }
  }
}

function hasResultNode() {
  return !!graphRef.value?.getNodes().some(node => node.getData()?.kind === 'result')
}

function selectNode(node) {
  if (selectedCellId.value) {
    const prev = graphRef.value?.getCellById(selectedCellId.value)
    if (prev) applyCellVisual(prev, false)
  }
  selectedCellId.value = node.id
  suppressSelectedWatch.value = true
  selectedNodeData.value = { ...node.getData() }
  suppressSelectedWatch.value = false
  applyCellVisual(node, true)
}

function selectEdge(edge) {
  if (selectedCellId.value) {
    const prev = graphRef.value?.getCellById(selectedCellId.value)
    if (prev) applyCellVisual(prev, false)
  }
  selectedCellId.value = edge.id
  selectedNodeData.value = null
  applyCellVisual(edge, true)
}

function clearSelected() {
  if (selectedCellId.value) {
    const prev = graphRef.value?.getCellById(selectedCellId.value)
    if (prev) applyCellVisual(prev, false)
  }
  selectedCellId.value = ''
  selectedNodeData.value = null
}

function onPaletteDragStart(event, kind) {
  event.dataTransfer?.setData('application/x-zhpg-flow-kind', kind)
  event.dataTransfer?.setData('text/plain', kind)
}

function onCanvasDrop(event) {
  const kind = event.dataTransfer?.getData('application/x-zhpg-flow-kind') || event.dataTransfer?.getData('text/plain')
  if (!['data', 'algorithm', 'result'].includes(kind) || !graphRef.value) return
  const point = graphRef.value.clientToLocal(event.clientX, event.clientY)
  const node = addFlowNode(kind, point.x - 80, point.y - 36)
  if (node) selectNode(node)
}

function removeSelectedCell() {
  if (!selectedCellId.value || !graphRef.value) return
  const cell = graphRef.value.getCellById(selectedCellId.value)
  if (cell) graphRef.value.removeCell(cell)
  clearSelected()
}

function fitCanvas() {
  graphRef.value?.zoomToFit({ padding: 28, maxScale: 1.22 })
}

function autoLayout() {
  const graph = graphRef.value
  if (!graph) return
  const nodes = graph.getNodes().map((node, order) => ({
    id: node.id,
    kind: node.getData()?.kind,
    order
  }))
  const edges = readGraphEdges()
  const layout = assignCalcAlgorithmLayout(nodes, edges)
  graph.getNodes().forEach(node => {
    const pos = layout.get(node.id)
    if (pos) node.position(pos.x, pos.y)
  })
  applyDistributedEdgePorts()
  tidyCanvasEdges()
  fitCanvas()
}

function readGraphEdges() {
  return graphRef.value?.getEdges()
    .map(edge => {
      const source = edge.getSource()
      const target = edge.getTarget()
      return {
        edge,
        sourceId: source?.cell ? String(source.cell) : '',
        targetId: target?.cell ? String(target.cell) : '',
        sourceSide: edge.getData()?.sourceSide || sideFromPortId(source?.port),
        targetSide: edge.getData()?.targetSide || sideFromPortId(target?.port),
        targetPortId: edge.getData()?.targetPortId || '',
        sourcePortId: edge.getData()?.sourcePortId || ''
      }
    })
    .filter(item => item.sourceId && item.targetId) || []
}

function applyDistributedEdgePorts() {
  const edges = readGraphEdges()
  const distributed = distributeIncomingPorts(edges)
  distributed.forEach(item => {
    item.edge.setData({ ...(item.edge.getData() || {}), targetPortId: item.targetPortId, sourcePortId: item.sourcePortId }, { deep: false })
  })
}

function nodeBox(node) {
  const pos = node.position()
  const size = node.size()
  return { x: pos.x, y: pos.y, width: size.width, height: size.height }
}

function tidyCanvasEdges() {
  const graph = graphRef.value
  if (!graph) return
  readGraphEdges().forEach((item, index) => {
    const sourceNode = graph.getCellById(item.sourceId)
    const targetNode = graph.getCellById(item.targetId)
    if (!sourceNode?.isNode?.() || !targetNode?.isNode?.()) return
    const source = item.edge.getSource()
    const target = item.edge.getTarget()
    const sourceBox = nodeBox(sourceNode)
    const targetBox = nodeBox(targetNode)
    const targetPortId = item.targetPortId || item.edge.getData()?.targetPortId || 'in_3'
    const sourcePortId = item.sourcePortId || item.edge.getData()?.sourcePortId || 'out_3'
    const sides = chooseConnectionSides(sourceBox, targetBox)
    item.edge.setRouter('manhattan', { padding: 24 })
    item.edge.setConnector('rounded', { radius: 10 })
    applyCellVisual(item.edge, item.edge.id === selectedCellId.value)
    item.edge.setSource({
      cell: source?.cell || item.sourceId,
      anchor: { name: sides.sourceSide, args: anchorArgsForSide(sourceBox, sides.sourceSide, sourcePortId) }
    })
    item.edge.setTarget({
      cell: target?.cell || item.targetId,
      anchor: { name: sides.targetSide, args: anchorArgsForSide(targetBox, sides.targetSide, targetPortId) }
    })
    item.edge.setData({ ...(item.edge.getData() || {}), ...sides, targetPortId, sourcePortId }, { deep: false })
    // Removed manual setVertices to allow manhattan router to handle paths correctly without diagonal lines during drag
  })
}

function mapDsToLocal(d) {
  const fields = Array.isArray(d.fields) ? d.fields : []
  // 提取中文描述作为显示文本（如：[{"timestamp": "时间戳"}] -> "时间戳"）
  const ft = fields.map(f => {
    if (typeof f === 'object' && f !== null) {
      const values = Object.values(f)
      return values.length > 0 ? String(values[0]).trim() : ''
    }
    return String(f || '').trim()
  }).filter(Boolean).join(',')

  return {
    name: d.name || '',
    directory: d.directory || '',
    directoryName: d.directoryName || '',
    source: String(d.source || '').trim() || '',
    sourceName: d.sourceName || '',
    fieldsText: ft,
    _rawFields: fields, // 确保原始字段对象被暂存，用于后续 Hover 显示技术名和反向映射
    _fieldTypes: d.fieldTypes || {},
    taskStage: d.taskStage || '',
    taskTimeStart: d.taskTimeStart || '',
    taskTimeEnd: d.taskTimeEnd || '',
    experimentalTask: d.experimentalTask || '',
    description: d.description || ''
  }
}

function hydrateGraphFromModel() {
  const graph = graphRef.value
  if (!graph) return
  graph.clearCells()
  clearSelected()
  const ws = parseLeafCalcToWorkspace(props.modelValue || '', {
    defaultWorkMode: props.workMode || ZHPG_WORK_MODE.INTERNAL_CIRCULATION
  })
  localWorkMode.value = props.workMode || ws.workMode || ZHPG_WORK_MODE.INTERNAL_CIRCULATION

  const dataSources = (ws.dataSources || []).map((d, i) => ({
    source: d,
    id: d.flowNodeId || allocFlowNodeIdForNewNode(),
    x: Number.isFinite(Number(d.x)) ? Number(d.x) : 80,
    y: Number.isFinite(Number(d.y)) ? Number(d.y) : 90 + i * 110
  }))
  const steps = (ws.algorithmSteps || []).map((s, i) => {
    const row = { ...s, algorithmType: '', algorithmId: s.algorithmId || '', params: s.params && typeof s.params === 'object' ? { ...s.params } : {} }
    syncStepAlgorithmTypeFromId(row)
    return {
      source: row,
      id: s.flowNodeId || allocFlowNodeIdForNewNode(),
      x: Number.isFinite(Number(s.x)) ? Number(s.x) : 360,
      y: Number.isFinite(Number(s.y)) ? Number(s.y) : 90 + i * 110
    }
  })
  const resultId = ws.resultFlowNodeId || allocFlowNodeIdForNewNode()
  const resultPos = ws.resultPosition || {}

  dataSources.forEach(item => addFlowNode('data', item.x, item.y, mapDsToLocal(item.source), item.id))
  steps.forEach(item => addFlowNode('algorithm', item.x, item.y, item.source, item.id))
  if (dataSources.length || steps.length || ws.flowEdges?.length) {
    addFlowNode(
      'result',
      Number.isFinite(Number(resultPos.x)) ? Number(resultPos.x) : 680,
      Number.isFinite(Number(resultPos.y)) ? Number(resultPos.y) : 160,
      {},
      resultId
    )
  }

  const edges = ws.flowEdges?.length ? ws.flowEdges : buildDefaultEdges(dataSources, steps, resultId)
  const targetCounter = new Map()
  edges.forEach(edge => {
    const tid = edge.targetId
    const count = targetCounter.get(tid) || 0
    const portId = edge.targetPortId || `in_${count % INPUT_PORT_COUNT}`
    addGraphEdge(edge.sourceId, tid, portId, edge.sourceSide || '', edge.targetSide || '')
    targetCounter.set(tid, count + 1)
  })
  nextTick(() => {
    autoLayout()
  })
}

function buildDefaultEdges(dataSources, steps, resultId) {
  const edges = []
  if (steps.length) {
    dataSources.forEach((ds, i) => edges.push({ sourceId: ds.id, targetId: steps[0].id, targetPortId: `in_${i % INPUT_PORT_COUNT}` }))
    for (let i = 0; i < steps.length - 1; i++) {
        edges.push({ sourceId: steps[i].id, targetId: steps[i + 1].id, targetPortId: 'in_0' })
    }
    edges.push({ sourceId: steps[steps.length - 1].id, targetId: resultId, targetPortId: 'in_0' })
  } else {
    dataSources.forEach((ds, i) => edges.push({ sourceId: ds.id, targetId: resultId, targetPortId: `in_${i % INPUT_PORT_COUNT}` }))
  }
  return edges
}

function addGraphEdge(sourceId, targetId, targetPortId = 'in_0', sourceSide = '', targetSide = '') {
  const graph = graphRef.value
  if (!graph || !graph.getCellById(sourceId) || !graph.getCellById(targetId)) return
  graph.addEdge({
    source: { cell: sourceId },
    target: { cell: targetId },
    data: { targetPortId, sourceSide, targetSide },
    ...edgeShape()
  })
}


function handleWorkModeChange(value) {
  localWorkMode.value = value
  emit('update:workMode', value)
}

function taskTimeRangeModel(d) {
  if (!d?.taskTimeStart && !d?.taskTimeEnd) return null
  return [d.taskTimeStart || null, d.taskTimeEnd || null]
}

function applyTaskTimeRange(d, v) {
  if (!d) return
  if (!v || !Array.isArray(v) || v.length < 2) {
    d.taskTimeStart = ''
    d.taskTimeEnd = ''
    return
  }
  d.taskTimeStart = v[0] ? String(v[0]) : ''
  d.taskTimeEnd = v[1] ? String(v[1]) : ''
}

function onSelectedAlgorithmChange(newId) {
  if (!selectedNodeData.value) return
  selectedNodeData.value.algorithmId = newId || ''
  selectedNodeData.value.params = {}
  if (newId) syncStepAlgorithmTypeFromId(selectedNodeData.value)
}

function onSelectedAlgorithmTypeChange(newType) {
  if (!selectedNodeData.value) return
  selectedNodeData.value.algorithmType = newType || ''
  selectedNodeData.value.algorithmId = ''
  selectedNodeData.value.params = {}
}

function paramFieldCount(step) {
  if (!step?.params || typeof step.params !== 'object') return 0
  return Object.keys(step.params).filter(k => step.params[k] != null && String(step.params[k]).trim() !== '').length
}

const paramDrawerVisible = ref(false)
const paramLoading = ref(false)
const paramEditorNodeId = ref('')
const paramEditorMeta = ref('')
const paramFormFields = ref([])
const paramEditValues = ref({})
const paramNumberModels = ref({})

function onParamDrawerClosed() {
  paramEditorNodeId.value = ''
  paramFormFields.value = []
  paramEditValues.value = {}
  paramNumberModels.value = {}
  paramEditorMeta.value = ''
}

function defaultPlaceholder(p) {
  return p.defaultValue != null && String(p.defaultValue) !== '' ? `默认：${p.defaultValue}` : '请输入'
}

function syncNumberParam(field, value) {
  paramEditValues.value[field] = value != null && value !== '' ? String(value) : ''
}

async function openParamDrawerForSelected() {
  const data = selectedNodeData.value
  if (!data || data.kind !== 'algorithm' || !data.algorithmId || !selectedCellId.value) return
  paramEditorNodeId.value = selectedCellId.value
  paramLoading.value = true
  paramDrawerVisible.value = true
  paramFormFields.value = []
  paramEditorMeta.value = ''
  try {
    const res = await getAlgorithm(data.algorithmId)
    const detail = res.data
    if (!detail) {
      ElMessage.warning('未获取到算法详情')
      return
    }
    paramEditorMeta.value = [detail.algorithmName, detail.algorithmType].filter(Boolean).join(' / ')
    const all = (detail.params || []).filter(p => {
      if (!p) return false
      if (props.onlyConfigParamsInDrawer) return p.paramCategory === 'CONFIG'
      return p.paramCategory === 'INPUT' || p.paramCategory === 'CONFIG'
    })
    all.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    paramFormFields.value = all
    const vals = {}
    const numModels = {}
    for (const p of all) {
      const field = p.paramField
      const defStr = p.defaultValue != null ? String(p.defaultValue) : ''
      const existing = data.params && data.params[field] != null ? String(data.params[field]) : ''
      const use = existing !== '' ? existing : defStr
      vals[field] = use
      if (p.paramType === 'number') {
        const n = use === '' ? undefined : Number(use)
        numModels[field] = Number.isFinite(n) ? n : undefined
      }
    }
    paramEditValues.value = vals
    paramNumberModels.value = numModels
  } catch {
    ElMessage.error('加载算法参数失败')
  } finally {
    paramLoading.value = false
  }
}

function applyParamDrawer() {
  const node = graphRef.value?.getCellById(paramEditorNodeId.value)
  if (!node || !node.isNode?.()) {
    paramDrawerVisible.value = false
    return
  }
  const data = { ...node.getData() }
  const out = props.onlyConfigParamsInDrawer && data.params && typeof data.params === 'object' ? { ...data.params } : {}
  for (const p of paramFormFields.value) {
    const field = p.paramField
    let value = paramEditValues.value[field]
    if (p.paramType === 'number') {
      const num = paramNumberModels.value[field]
      value = num != null && num !== '' ? String(num) : ''
    }
    const defStr = p.defaultValue != null ? String(p.defaultValue) : ''
    const sv = value != null ? String(value).trim() : ''
    if (sv !== '' && sv !== defStr) out[field] = sv
    else delete out[field]
  }
  data.params = out
  node.setData(data, { deep: false })
  if (selectedCellId.value === node.id) {
    suppressSelectedWatch.value = true
    selectedNodeData.value = { ...data }
    suppressSelectedWatch.value = false
  }
  paramDrawerVisible.value = false
}

function findTechnicalNameByLabel(label) {
  const raw = selectedNodeData.value?._rawFields
  if (!raw || !Array.isArray(raw)) return label
  // raw 结构：[{ techName: comment }, ...]
  const found = raw.find(f => {
    const keys = Object.keys(f)
    return keys.length > 0 && (f[keys[0]] === label || keys[0] === label)
  })
  if (found) return Object.keys(found)[0]
  return label
}

function parseFields(text) {
  if (!text) return []
  return String(text).split(/[,，]/).map(field => field.trim()).filter(Boolean)
}

function removeFieldTag(idx) {
  if (!selectedNodeData.value) return
  const fields = parseFields(selectedNodeData.value.fieldsText)
  fields.splice(idx, 1)
  selectedNodeData.value.fieldsText = fields.join(',')
}

function addFieldFromInput() {
  if (newFieldInput.value && selectedNodeData.value) {
    const fields = parseFields(selectedNodeData.value.fieldsText)
    if (!fields.includes(newFieldInput.value)) {
      fields.push(newFieldInput.value)
      selectedNodeData.value.fieldsText = fields.join(',')
    }
  }
  newFieldInput.value = ''
  openFieldInput.value = false
}

function handleExternalDataSelected(rows) {
  if (!rows || rows.length === 0 || !selectedNodeData.value) return
  
  // 简化逻辑：一个数据源仅对应一个分中心和目录
  const selection = rows[0]
  selectedNodeData.value.source = selection.source
  selectedNodeData.value.directory = selection.directory
  selectedNodeData.value.directoryName = selection.directoryName || selection.directory
  selectedNodeData.value.sourceName = selection.sourceName || selection.source
  // 存储原始字段用于 Hover 显示技术名称（映射至：[{ "timestamp": "时间戳" }, ...]）
  selectedNodeData.value._rawFields = selection.fields
  selectedNodeData.value._fieldTypes = selection.fieldTypes || {}
  // 提取中文描述用于 fieldsText 显示
  selectedNodeData.value.fieldsText = selection.fields.map(f => Object.values(f)[0]).join(',')
  
  // 移除多余的 sourceEntries 暂存
  if (selectedNodeData.value.sourceEntries) {
    delete selectedNodeData.value.sourceEntries
  }
  
  ElMessage.success(`已配置数据源：${selection.directory}，选择了 ${selection.fields.length} 个字段`)
}

function collectWorkspaceFromGraph() {
  const graph = graphRef.value
  const defCenter = String(props.defaultSourceCenter || '').trim()
  const dataSources = []
  const algorithmSteps = []
  let resultFlowNodeId
  let resultPosition

  graph.getNodes().forEach(node => {
    const data = node.getData() || {}
    const pos = node.position()
    if (data.kind === 'data') {
      const currentLabels = parseFields(data.fieldsText)
      const raw = data._rawFields || []
      const fieldsToSave = currentLabels.map(label => {
        // 优先从暂存的 _rawFields 中找回包含技术名的原始对象格式
        const found = raw.find(rf => {
          if (typeof rf === 'object' && rf !== null) {
            const values = Object.values(rf)
            return values.length > 0 && String(values[0]).trim() === label
          }
          return String(rf).trim() === label
        })
        return found || label
      })

      dataSources.push({
        flowNodeId: node.id,
        name: String(data.name || '').trim(),
        directory: data.directory || '',
        directoryName: String(data.directoryName || '').trim(),
        source: String(data.source || '').trim() || defCenter,
        sourceName: String(data.sourceName || '').trim(),
        fields: fieldsToSave,
        fieldTypes: data._fieldTypes || data.fieldTypes || {},
        taskStage: String(data.taskStage || '').trim(),
        taskTimeStart: String(data.taskTimeStart || '').trim(),
        taskTimeEnd: String(data.taskTimeEnd || '').trim(),
        experimentalTask: String(data.experimentalTask || '').trim(),
        description: String(data.description || '').trim(),
        x: pos.x,
        y: pos.y
      })
    } else if (data.kind === 'algorithm') {
      algorithmSteps.push({
        flowNodeId: node.id,
        algorithmId: data.algorithmId || '',
        params: data.params && typeof data.params === 'object' ? { ...data.params } : {},
        x: pos.x,
        y: pos.y
      })
    } else if (data.kind === 'result') {
      resultFlowNodeId = node.id
      resultPosition = { x: pos.x, y: pos.y }
    }
  })

  const flowEdges = graph.getEdges()
    .map(edge => {
      const source = edge.getSource()
      const target = edge.getTarget()
      return {
        sourceId: source?.cell ? String(source.cell) : '',
        targetId: target?.cell ? String(target.cell) : '',
        targetPortId: edge.getData()?.targetPortId || (target?.port ? String(target.port) : 'in_0'),
        sourceSide: edge.getData()?.sourceSide || '',
        targetSide: edge.getData()?.targetSide || ''
      }
    })
    .filter(edge => edge.sourceId && edge.targetId)

  return { workMode: localWorkMode.value, dataSources, algorithmSteps, resultFlowNodeId, resultPosition, flowEdges }
}

function graphValidationError(workspace) {
  const nodeCount = workspace.dataSources.length + workspace.algorithmSteps.length + (workspace.resultFlowNodeId ? 1 : 0)
  if (nodeCount === 0) return null
  if (!workspace.resultFlowNodeId) return '请在画布中添加结果节点'
  if (workspace.flowEdges.length === 0) return '请在画布中完成至少一条连线'
  const nodeIds = new Set([
    ...workspace.dataSources.map(d => d.flowNodeId),
    ...workspace.algorithmSteps.map(s => s.flowNodeId),
    workspace.resultFlowNodeId
  ].filter(Boolean))
  for (const edge of workspace.flowEdges) {
    if (!nodeIds.has(edge.sourceId) || !nodeIds.has(edge.targetId)) return '画布连线存在无效节点，请删除后重新连接'
  }
  return null
}

function save() {
  const workspace = collectWorkspaceFromGraph()
  const hasAnyNode = workspace.dataSources.length > 0 || workspace.algorithmSteps.length > 0 || !!workspace.resultFlowNodeId
  if (!hasAnyNode) {
    emit('update:workMode', localWorkMode.value)
    emit('update:modelValue', null)
    emit('saved', null)
    ElMessage.success('已保存工作模式配置')
    emit('update:visible', false)
    return
  }
  const graphErr = graphValidationError(workspace)
  if (graphErr) {
    ElMessage.warning(graphErr)
    return
  }
  const wsErr = validateCalcWorkspace(workspace)
  if (wsErr) {
    ElMessage.warning(wsErr)
    return
  }
  const leafUid = String(props.leafUid || '').trim() || 'leaf'
  const payload = serializeWorkspaceToComputeRule(workspace, leafUid)
  const legacyStr = JSON.stringify(serializeCalcWorkspace(workspace))
  emit('update:workMode', localWorkMode.value)
  emit('update:modelValue', payload)
  emit('saved', payload != null ? payload : legacyStr)
  ElMessage.success('已保存算法画布配置')
  emit('update:visible', false)
}

async function onDialogOpen() {
  await nextTick()
  initGraph()
  hydrateGraphFromModel()
  loadAlgorithms()
}

function onDialogClosed() {
  graphRef.value?.dispose()
  graphRef.value = null
  clearSelected()
}
</script>

<style scoped>
.dialog-topbar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 12px; }
.topbar-label { font-size: 15px; font-weight: 600; color: var(--el-text-color-regular); }
.topbar-tip { font-size: 13px; color: var(--el-text-color-secondary); }
.algorithm-config {
  display: grid;
  grid-template-columns: 220px minmax(520px, 1fr) 320px;
  height: calc(100vh - 240px);
  min-height: 620px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
  background: #f8fbff;
}
.flow-palette,
.flow-inspector {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 14px;
  background: #fff;
  overflow-y: auto;
}
.flow-palette { border-right: 1px solid var(--el-border-color-lighter); }
.flow-inspector { border-left: 1px solid var(--el-border-color-lighter); }
.panel-title { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 12px; font-size: 16px; font-weight: 700; color: var(--el-text-color-primary); }
.palette-card { display: flex; align-items: center; gap: 12px; padding: 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 8px; background: #fff; cursor: grab; user-select: none; transition: transform .18s ease, box-shadow .18s ease, border-color .18s ease; }
.palette-card + .palette-card { margin-top: 10px; }
.palette-card:hover { transform: translateY(-1px); box-shadow: var(--el-box-shadow-light); border-color: var(--el-color-primary-light-5); }
.palette-icon { display: inline-flex; align-items: center; justify-content: center; width: 34px; height: 34px; border-radius: 10px; font-weight: 800; color: #fff; }
.palette-card--data .palette-icon { background: #67c23a; }
.palette-card--algorithm .palette-icon { background: #409eff; }
.palette-card--result .palette-icon { background: #e6a23c; }
.palette-name { font-weight: 700; color: var(--el-text-color-primary); }
.palette-desc, .palette-hint { margin-top: 3px; font-size: 12px; line-height: 1.45; color: var(--el-text-color-secondary); }
.palette-hint { margin-top: 14px; padding: 10px; border-radius: 8px; background: var(--el-fill-color-light); border: 1px solid var(--el-border-color-lighter); }
.palette-action { width: 100%; margin-top: 12px; }
.canvas-shell { display: flex; flex-direction: column; min-width: 0; }
.canvas-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 14px; border-bottom: 1px solid var(--el-border-color-lighter); background: rgba(255,255,255,.86); font-weight: 700; color: var(--el-text-color-primary); }
.flow-canvas { flex: 1; min-height: 560px; }
.inspector-form :deep(.el-form-item) { margin-bottom: 14px; }
.inspector-form :deep(.el-form-item__label) { color: var(--el-text-color-secondary); }
.inspector-form :deep(.el-input__wrapper),
.inspector-form :deep(.el-select__wrapper),
.param-form :deep(.el-input__wrapper),
.param-form :deep(.el-input-number__decrease),
.param-form :deep(.el-input-number__increase) {
  background: var(--el-fill-color-blank);
  box-shadow: 0 0 0 1px var(--el-border-color) inset;
}
.inspector-form :deep(.el-input__inner),
.param-form :deep(.el-input__inner),
.param-form :deep(.el-textarea__inner) {
  color: var(--el-text-color-primary);
}
.inspector-form :deep(.el-textarea__inner),
.param-form :deep(.el-textarea__inner) {
  background: var(--el-fill-color-blank);
  border-color: var(--el-border-color);
}
.inspector-daterange { width: 100%; }
.inspector-daterange :deep(.el-input__wrapper) { width: 100%; }
.algo-param-row { display: flex; align-items: center; gap: 10px; }
.param-drawer-body { min-height: 180px; }
.param-drawer-meta { margin: 0 0 8px; color: var(--el-text-color-primary); font-weight: 600; }
.param-drawer-hint { margin: 0 0 12px; color: var(--el-text-color-secondary); font-size: 13px; }
.param-desc { margin-left: 8px; font-size: 12px; color: var(--el-text-color-secondary); }
.flow-canvas :deep(.x6-node rect) {
  transition: stroke 0.2s ease, stroke-width 0.2s ease, filter 0.2s ease;
}
.data-source-config-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
  font-weight: bold;
}
.fields-tag-container {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  min-height: 40px;
  width: 100%;
  background-color: var(--el-fill-color-light);
}
.fields-tag-container.is-readonly {
  cursor: not-allowed;
}
.fields-placeholder {
  font-size: 13px;
  color: var(--el-text-color-placeholder);
}

:global(.dark-theme) .algorithm-config {
  border-color: rgba(0, 242, 255, 0.14);
  background: #07111f;
}
:global(.dark-theme) .flow-palette,
:global(.dark-theme) .flow-inspector {
  background: rgba(5, 13, 24, 0.96);
}
:global(.dark-theme) .flow-palette { border-right-color: rgba(0, 242, 255, 0.14); }
:global(.dark-theme) .flow-inspector { border-left-color: rgba(0, 242, 255, 0.14); }
:global(.dark-theme) .panel-title,
:global(.dark-theme) .palette-name,
:global(.dark-theme) .param-drawer-meta {
  color: #d8e7f5;
}
:global(.dark-theme) .palette-card {
  border-color: rgba(0, 242, 255, 0.14);
  background: rgba(10, 21, 37, 0.82);
}
:global(.dark-theme) .palette-card:hover {
  box-shadow: 0 12px 28px rgba(0, 0, 0, .24);
  border-color: rgba(34, 211, 238, 0.46);
}
:global(.dark-theme) .palette-desc,
:global(.dark-theme) .palette-hint,
:global(.dark-theme) .inspector-form :deep(.el-form-item__label),
:global(.dark-theme) .param-drawer-hint,
:global(.dark-theme) .param-desc,
:global(.dark-theme) .data-source-config-header {
  color: #9fb6cb;
}
:global(.dark-theme) .palette-hint {
  background: rgba(0, 242, 255, 0.06);
  border-color: rgba(0, 242, 255, 0.12);
}
:global(.dark-theme) .canvas-toolbar {
  border-bottom-color: rgba(0, 242, 255, 0.14);
  background: rgba(5, 13, 24, 0.96);
  color: #d8e7f5;
}
:global(.dark-theme) .inspector-form :deep(.el-input__wrapper),
:global(.dark-theme) .inspector-form :deep(.el-select__wrapper),
:global(.dark-theme) .param-form :deep(.el-input__wrapper),
:global(.dark-theme) .param-form :deep(.el-input-number__decrease),
:global(.dark-theme) .param-form :deep(.el-input-number__increase) {
  background: rgba(5, 13, 24, 0.74);
  box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.16) inset;
}
:global(.dark-theme) .inspector-form :deep(.el-input__inner),
:global(.dark-theme) .param-form :deep(.el-input__inner),
:global(.dark-theme) .param-form :deep(.el-textarea__inner) {
  color: #d8e7f5;
}
:global(.dark-theme) .inspector-form :deep(.el-textarea__inner),
:global(.dark-theme) .param-form :deep(.el-textarea__inner) {
  background: rgba(5, 13, 24, 0.74);
  border-color: rgba(0, 242, 255, 0.16);
}
:global(.dark-theme) .fields-tag-container {
  border-color: rgba(0, 242, 255, 0.14);
  background-color: rgba(0, 242, 255, 0.06);
}
:global(.dark-theme) .fields-placeholder {
  color: #6f879d;
}
.field-tag {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>

<style>
.zhpg-calc-algorithm-dialog.el-dialog {
  width: min(96vw, 1480px) !important;
  max-width: 1480px;
  margin-top: 4vh !important;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: var(--el-box-shadow-light);
}
.zhpg-calc-algorithm-dialog .el-dialog__header,
.zhpg-calc-algorithm-dialog .el-dialog__body,
.zhpg-calc-algorithm-dialog .el-dialog__footer {
  background: var(--el-bg-color);
}
.zhpg-calc-algorithm-dialog .el-dialog__title {
  color: var(--el-text-color-primary);
}
.zhpg-calc-algorithm-dialog .el-dialog__headerbtn .el-dialog__close {
  color: var(--el-text-color-secondary);
}
.zhpg-calc-algorithm-dialog .el-dialog__body {
  padding: 12px 16px 8px;
  max-height: calc(100vh - 140px);
  overflow: auto;
}
.dark-theme .zhpg-calc-algorithm-dialog.el-dialog {
  background: #050d18;
  border-color: rgba(0, 242, 255, 0.16);
  box-shadow: 0 24px 72px rgba(0, 0, 0, 0.52);
}
.dark-theme .zhpg-calc-algorithm-dialog .el-dialog__header,
.dark-theme .zhpg-calc-algorithm-dialog .el-dialog__body,
.dark-theme .zhpg-calc-algorithm-dialog .el-dialog__footer {
  background: #050d18;
}
.dark-theme .zhpg-calc-algorithm-dialog .el-dialog__title {
  color: #d8e7f5;
}
.dark-theme .zhpg-calc-algorithm-dialog .el-dialog__headerbtn .el-dialog__close {
  color: #38bdf8;
}
.dark-theme .zhpg-calc-algorithm-dialog .algorithm-config {
  border-color: rgba(0, 242, 255, 0.14);
  background: #07111f;
}
.dark-theme .zhpg-calc-algorithm-dialog .flow-palette,
.dark-theme .zhpg-calc-algorithm-dialog .flow-inspector {
  background: rgba(5, 13, 24, 0.96);
}
.dark-theme .zhpg-calc-algorithm-dialog .flow-palette {
  border-right-color: rgba(0, 242, 255, 0.14);
}
.dark-theme .zhpg-calc-algorithm-dialog .flow-inspector {
  border-left-color: rgba(0, 242, 255, 0.14);
}
.dark-theme .zhpg-calc-algorithm-dialog .canvas-toolbar {
  border-bottom-color: rgba(0, 242, 255, 0.14);
  background: rgba(5, 13, 24, 0.96);
  color: #d8e7f5;
}
.dark-theme .zhpg-calc-algorithm-dialog .panel-title,
.dark-theme .zhpg-calc-algorithm-dialog .palette-name,
.dark-theme .zhpg-calc-algorithm-dialog .param-drawer-meta {
  color: #d8e7f5;
}
.dark-theme .zhpg-calc-algorithm-dialog .palette-card {
  border-color: rgba(0, 242, 255, 0.14);
  background: rgba(10, 21, 37, 0.82);
}
.dark-theme .zhpg-calc-algorithm-dialog .palette-card:hover {
  box-shadow: 0 12px 28px rgba(0, 0, 0, .24);
  border-color: rgba(34, 211, 238, 0.46);
}
.dark-theme .zhpg-calc-algorithm-dialog .palette-desc,
.dark-theme .zhpg-calc-algorithm-dialog .palette-hint,
.dark-theme .zhpg-calc-algorithm-dialog .data-source-config-header,
.dark-theme .zhpg-calc-algorithm-dialog .param-drawer-hint,
.dark-theme .zhpg-calc-algorithm-dialog .param-desc {
  color: #9fb6cb;
}
.dark-theme .zhpg-calc-algorithm-dialog .palette-hint {
  background: rgba(0, 242, 255, 0.06);
  border-color: rgba(0, 242, 255, 0.12);
}
.dark-theme .zhpg-calc-algorithm-dialog .fields-tag-container {
  border-color: rgba(0, 242, 255, 0.14);
  background-color: rgba(0, 242, 255, 0.06);
}
.dark-theme .zhpg-calc-algorithm-dialog .fields-placeholder {
  color: #6f879d;
}
</style>
