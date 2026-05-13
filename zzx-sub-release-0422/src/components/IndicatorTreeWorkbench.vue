<template>
  <div class="indicator-tree-workbench" :class="{ 'indicator-tree-workbench--fill': fillParentHeight }">
    <div class="workbench-toolbar">
      <el-button-group class="toolbar-grp">
        <el-button type="primary" icon="Plus" @click="onAddRoot">添加根节点</el-button>
        <el-button icon="Download" @click="$emit('import-indicators')">从指标导入</el-button>
        <el-button v-if="showTemplateImport" icon="Document" @click="$emit('import-template')">从模板导入</el-button>
        <el-button v-if="weightTuningMode !== 'none'" type="primary" plain :disabled="!treeData.length"
          @click="weightTuningVisible = true">
          权重分配调优
        </el-button>
      </el-button-group>
      <el-button-group class="toolbar-grp graph-tools">
        <el-tooltip content="适应画布" placement="top">
          <el-button :icon="FullScreen" @click="graphFit" />
        </el-tooltip>
      </el-button-group>
      <span class="graph-roam-hint">
        图形区：拖动画布平移、滚轮缩放；<strong>拖动节点到另一节点上释放</strong>可合并（改挂接为子节点）；<strong>右键</strong>打开编辑菜单
      </span>
      <span v-if="selectedUid" class="selection-hint">
        已选：<strong>{{ selectedLabel }}</strong>
      </span>
    </div>

    <splitpanes class="workbench-split default-theme" :style="splitPaneStyle">
      <pane :size="variant === 'system' ? 24 : 32" :min-size="16">
        <div class="pane-inner tree-pane">
          <div class="pane-title"><span class="pane-title-icon">&#9670;</span> 结构树（可拖拽排序）</div>
          <el-scrollbar class="tree-scroll">
            <el-tree ref="innerTreeRef" :data="treeData" node-key="uid"
              :props="{ label: 'label', children: 'children' }" :default-expand-all="true" :expand-on-click-node="false"
              :draggable="draggable" :allow-drop="allowTreeDrop" highlight-current class="indicator-tree"
              @node-click="onTreeNodeClick" @node-drop="onNodeDrop">
              <template #default="{ node, data }">
                <div class="tree-node-row">
                  <el-tooltip :content="data.label || ''" placement="top" :show-after="400"
                    :disabled="!(data.label && data.label.length > 20)">
                    <span class="tree-node-label">{{ data.label }}</span>
                  </el-tooltip>
                  <span class="tree-node-ops">
                    <template v-if="variant === 'template'">
                      <el-button link type="primary" @click.stop="$emit('edit-node', data)">编辑</el-button>
                      <el-button link type="primary" @click.stop="addChild(data)">子节点</el-button>
                      <el-button link type="danger" @click.stop="confirmRemove(node, data)">删除</el-button>
                    </template>
                    <template v-else>
                      <el-button link type="primary" @click.stop="addChild(data)">+</el-button>
                      <el-button link type="danger" @click.stop="confirmRemove(node, data)">×</el-button>
                    </template>
                  </span>
                </div>
              </template>
            </el-tree>
            <div v-if="!treeData.length" class="tree-empty-hint">暂无节点，请添加根节点或从指标库导入</div>
          </el-scrollbar>
        </div>
      </pane>

      <pane :size="variant === 'system' ? 58 : 68" :min-size="28">
        <div class="pane-inner graph-pane">
          <div class="pane-title"><span class="pane-title-icon">&#9670;</span> 图形化视图（可拖拽合并 · 右键菜单）</div>
          <div ref="g6Ref" class="g6-graph-host" />
          <div class="graph-actions" v-if="activeNode">
            <span class="graph-actions-label">节点操作</span>
            <el-button @click="emitEditIfTemplate">编辑</el-button>
            <el-button type="primary" plain @click="addChild(activeNode)">添加子节点</el-button>
            <el-button type="danger" plain @click="confirmRemoveByUid(activeNode.uid)">删除（裁剪子树）</el-button>
          </div>
        </div>
      </pane>

      <pane v-if="variant === 'system'" :size="18" :min-size="14">
        <div class="pane-inner props-pane">
          <div class="pane-title"><span class="pane-title-icon">&#9670;</span> 节点属性</div>
          <el-scrollbar class="props-scroll">
            <template v-if="selectedNode">
              <el-form label-width="92px" class="props-form">
                <el-form-item label="节点名称">
                  <el-input v-model="selectedNode.label" @input="selectedNode.indicatorName = selectedNode.label" />
                </el-form-item>
                <el-form-item label="适用装备">
                  <el-select v-model="selectedNode.indicatorType" clearable placeholder="底层节点须选，非叶可选填"
                    style="width: 100%">
                    <el-option v-for="dict in zhpg_equipment_type" :key="dict.value" :label="dict.label"
                      :value="dict.value" />
                  </el-select>
                </el-form-item>
                <el-divider content-position="left" class="props-divider">度量与算法</el-divider>
                <el-form-item label="计量单位">
                  <el-input v-model="selectedNode.unit" placeholder="如 %、m、s" />
                </el-form-item>
                <!-- 主分协同初始粗建阶段：隐藏底层节点的算法规则配置 -->
                <template v-if="selectedNodeIsLeaf && hideLeafCalcMethod">
                  <el-form-item label="算法规则">
                    <el-tag type="info" size="default">初始粗建阶段 — 由需求分析分系统补充</el-tag>
                  </el-form-item>
                </template>
                <template v-else>
                  <el-form-item v-if="selectedNodeIsLeaf" label="指标值类型">
                    <el-select v-model="selectedNode.valueCategory" clearable placeholder="请选择" style="width: 100%">
                      <el-option v-for="o in valueCategoryOptions" :key="o.value" :label="o.label" :value="o.value" />
                    </el-select>
                  </el-form-item>
                  <el-form-item v-if="selectedNodeIsLeaf" label="门限范围">
                    <div class="threshold-range-row">
                      <el-input-number v-model="selectedNode.valueMin" controls-position="right"
                        class="threshold-num" />
                      <span class="threshold-sep">至</span>
                      <el-input-number v-model="selectedNode.valueMax" controls-position="right"
                        class="threshold-num" />
                    </div>
                  </el-form-item>
                </template>
                <!-- 指标体系：权重由工具栏「权重分配调优」统一管理，不在此逐节点编辑 -->
                <el-form-item v-if="selectedNodeIsLeaf && variant !== 'system'" label="指标权重">
                  <el-input-number v-model="selectedNode.weight" :min="0" :max="1" :step="0.05" :precision="4"
                    controls-position="right" placeholder="可选，0~1" style="width: 100%" />
                </el-form-item>
                <template v-if="variant === 'system' && !selectedNodeIsLeaf">
                  <el-form-item label="赋权与传导">
                    <div class="node-algo-trigger-row">
                      <el-button type="primary" plain @click="openNodeLocalAlgoDialog">配置本节点</el-button>
                      <el-tag type="info" effect="plain" class="node-algo-tag">{{ workbenchNodeWeightSummary }}</el-tag>
                      <el-tag type="info" effect="plain" class="node-algo-tag">{{ workbenchNodeConductionSummary
                      }}</el-tag>
                    </div>
                  </el-form-item>
                </template>
                <el-form-item label="描述">
                  <el-input v-model="selectedNode.description" type="textarea" :rows="4" />
                </el-form-item>
                <el-form-item v-if="selectedNodeIsLeaf && !hideLeafCalcMethod" label-width="0">
                  <ZhpgCalcMethodFields :model-value="selectedLeafCalcPayload" :leaf-uid="selectedNode?.uid || ''"
                    :work-mode="selectedNodeWorkMode" :show-work-mode-selector="!globalWorkMode"
                    :default-source-center="defaultDataSourceCenter" @update:model-value="onSelectedCalcMethodUpdate"
                    @update:work-mode="onSelectedWorkModeUpdate" />
                </el-form-item>
              </el-form>
            </template>
            <div v-else class="props-empty-wrap">
              <div class="props-empty-card">
                <div class="props-empty-title">未选择节点</div>
                <p class="props-empty-lead">在左侧<strong>结构树</strong>或中间<strong>图形化视图</strong>中点击节点，即可在此编辑名称、装备域与算法等属性。
                </p>
                <ul class="props-empty-tips">
                  <li>图形区支持右键菜单：新增、剪切、粘贴、删除等</li>
                  <li>结构树支持拖拽调整顺序与层级</li>
                </ul>
              </div>
            </div>
          </el-scrollbar>
        </div>
      </pane>
    </splitpanes>

    <el-dialog title="本节点 · 赋权与传导" v-model="nodeLocalAlgoDialog" width="480px" append-to-body
      class="indicator-workbench-dialog">
      <template v-if="selectedNode && !selectedNodeIsLeaf">
        <el-form label-width="120px" size="small">
          <!-- 指标体系：节点权重由「权重分配调优」统一维护 -->
          <el-form-item v-if="variant !== 'system'" label="节点权重">
            <el-input-number v-model="selectedNode.weight" :min="0" :max="1" :step="0.05" :precision="4"
              controls-position="right" placeholder="可选" style="width: 100%" />
          </el-form-item>
          <el-form-item label="权重分配算法">
            <el-select v-model="localWeightAssign" clearable placeholder="未选则体系全局" style="width: 100%">
              <el-option v-for="o in weightAssignOptions" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="传导方法">
            <el-select v-model="localConduction" clearable placeholder="未选则体系全局" style="width: 100%">
              <el-option v-for="o in conductionMethodOptions" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="clearWorkbenchNodeAlgoOverride">清除并采用全局默认</el-button>
        <el-button type="primary" @click="closeNodeLocalAlgoDialog">完 成</el-button>
      </template>
    </el-dialog>

    <ZhpgWeightTuningDialog v-model="weightTuningVisible" :tree-data="treeData"
      :show-objective-weight="showObjectiveWeight" :objective-weight-loading="objectiveWeightLoading"
      :objective-weight-disabled="objectiveWeightDisabled" @run-objective-weight="$emit('run-objective-weight')" />
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useDict } from '@/utils/dict'
import { Splitpanes, Pane } from 'splitpanes'
import 'splitpanes/dist/splitpanes.css'
import { Graph as G6Graph } from '@antv/g6'
import { FullScreen } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import ZhpgCalcMethodFields from '@/views/zhpg/components/ZhpgCalcMethodFields.vue'
import ZhpgWeightTuningDialog from '@/views/zhpg/components/ZhpgWeightTuningDialog.vue'
import { ZHPG_VALUE_CATEGORY_OPTIONS, getZhpgValueCategoryLabel } from '@/constants/zhpgIndicatorValueCategory'
import {
  ZHPG_EQUIPMENT_TYPE_OPTIONS
} from '@/constants/zhpgIndicatorSystem'
import {
  ZHPG_WEIGHT_ASSIGN_OPTIONS,
  getZhpgWeightAssignLabel,
  getZhpgConductionMethodLabel,
  coerceConductionMethod
} from '@/constants/zhpgIndicatorSystemAlgorithms'
import { fetchZhpgConductionMethodSelectOptions } from '@/utils/zhpg/zhpgConductionAlgorithms'
import { ZHPG_WORK_MODE } from '@/constants/zhpgWorkMode'
import { getCalcMethodWorkMode, setCalcMethodWorkMode } from '@/utils/zhpg/calcMethodAlgorithm'
import { buildNodeAlgoDialogState, applyNodeAlgoDialogState } from '@/utils/zhpg/zhpgNodeAlgoOverride'
import { useThemeStore } from '@/store/theme';
const emit = defineEmits(['import-indicators', 'import-template', 'edit-node', 'run-objective-weight'])
const NodeEvent = {
  DRAG: 'node:drag',
  CLICK: 'node:click'
}
const themeStore = useThemeStore();
function treeToGraphData(root, options = {}) {
  const nodes = []
  const edges = []
  const walk = (datum, depth = 0, parent = null) => {
    if (!datum) return
    const node = options.getNodeData ? options.getNodeData(datum, depth) : { id: datum.id }
    nodes.push(node)
    if (parent && options.getEdgeData) edges.push(options.getEdgeData(parent, datum))
    const children = options.getChildren ? options.getChildren(datum) : (datum.children || [])
    children.forEach(child => walk(child, depth + 1, datum))
  }
  walk(root)
  return { nodes, edges }
}

function applyStableTreeLayout(nodes, edges) {
  const nodeMap = new Map(nodes.map(node => [node.id, node]))
  const childrenMap = new Map(nodes.map(node => [node.id, []]))
  const childIds = new Set()
  edges.forEach(edge => {
    const source = edge.source || edge.sourceNode || edge.from
    const target = edge.target || edge.targetNode || edge.to
    if (!source || !target || !nodeMap.has(source) || !nodeMap.has(target)) return
    childrenMap.get(source)?.push(target)
    childIds.add(target)
  })

  const roots = nodes.filter(node => !childIds.has(node.id))
  const layoutRoots = roots.length ? roots : nodes.slice(0, 1)
  let cursorY = 0
  const columnGap = 100
  const rowGap = 14

  const visit = (node, depth) => {
    const children = (childrenMap.get(node.id) || []).map(id => nodeMap.get(id)).filter(Boolean)
    const nodeWidth = node.data?.layoutWidth || node.size?.[0] || GRAPH_NODE_FIXED_W
    const nodeHeight = node.data?.layoutHeight || node.size?.[1] || GRAPH_NODE_FIXED_H

    if (!children.length) {
      node.__layoutY = cursorY
      cursorY += nodeHeight + rowGap
    } else {
      children.forEach(child => visit(child, depth + 1))
      node.__layoutY = (children[0].__layoutY + children[children.length - 1].__layoutY) / 2
    }

    node.x = 60 + depth * (nodeWidth + columnGap)
    node.y = 60 + node.__layoutY
  }

  layoutRoots.forEach(root => visit(root, 0))
  nodes.forEach(node => delete node.__layoutY)
}

function toV4GraphData(data) {
  const edges = (data?.edges || []).map(edge => ({
    ...edge,
    type: 'cubic-horizontal',
    style: edge.style || {}
  }))
  const nodes = (data?.nodes || []).map(node => {
    const style = node.style || {}
    return {
      ...node,
      label: style.labelText || node.label || node.id,
      size: style.size || [GRAPH_NODE_FIXED_W, GRAPH_NODE_FIXED_H],
      style: {
        fill: style.fill,
        stroke: style.stroke,
        lineWidth: style.lineWidth,
        radius: style.radius,
        opacity: style.opacity,
        shadowColor: style.shadowColor,
        shadowBlur: style.shadowBlur,
        shadowOffsetX: style.shadowOffsetX,
        shadowOffsetY: style.shadowOffsetY
      },
      labelCfg: {
        position: 'center',
        style: {
          fill: style.labelFill || (isDarkMode.value ? '#e8eaf6' : '#1e293b'),
          fontSize: style.labelFontSize || 13,
          fontWeight: style.labelFontWeight || 500,
          textAlign: 'center',
          wordWrap: true,
          wordWrapWidth: style.labelMaxWidth || (GRAPH_NODE_FIXED_W - 28)
        }
      }
    }
  })
  applyStableTreeLayout(nodes, edges)
  return { nodes, edges }
}

function normalizeG6NodeEvent(event) {
  const item = event?.item
  return {
    ...event,
    target: { id: item?.getID?.() || item?.getModel?.()?.id || event?.target?.id },
    canvas: {
      x: event?.canvasX ?? event?.canvas?.x ?? event?.viewport?.x ?? 0,
      y: event?.canvasY ?? event?.canvas?.y ?? event?.viewport?.y ?? 0
    }
  }
}

class Graph extends G6Graph {
  constructor(config = {}) {
    super({
      container: config.container,
      width: config.width,
      height: config.height,
      renderer: 'canvas',
      fitView: false,
      modes: { default: ['zoom-canvas', 'drag-canvas', 'drag-node'] },
      defaultNode: {
        type: 'rect',
        style: { radius: 6, lineWidth: 1.5 },
        labelCfg: { position: 'center' }
      },
      defaultEdge: {
        type: 'cubic-horizontal',
        style: { lineWidth: 1.5, opacity: 0.7 }
      },
      nodeStateStyles: config.node?.state || {}
    })
    this.__rawData = config.data || { nodes: [], edges: [] }
    this.data(toV4GraphData(this.__rawData))
    // 设置画布背景色（G6 v4 canvas 默认透明，需显式设置以确保文字对比度）
    if (config.background) {
      this.__bgColor = config.background
    }
  }

  setData(data, options = {}) {
    this.__rawData = data || { nodes: [], edges: [] }
    const v4Data = toV4GraphData(this.__rawData)
    const hasExistingNodes = typeof this.getNodes === 'function' && this.getNodes().length > 0
    if (options.preservePositions && hasExistingNodes) {
      const savedPositions = {}
      this.getNodes().forEach(n => {
        const id = n.getID()
        const model = n.getModel()
        if (id && Number.isFinite(model?.x) && Number.isFinite(model?.y)) {
          savedPositions[id] = { x: model.x, y: model.y }
        }
      })
      v4Data.nodes.forEach(n => {
        const saved = savedPositions[n.id]
        if (saved) {
          n.x = saved.x
          n.y = saved.y
        }
      })
    }
    if (hasExistingNodes) {
      this.changeData(v4Data)
    } else {
      this.data(v4Data)
    }
  }

  getNodeData() {
    return this.__rawData?.nodes || []
  }

  async setElementState(stateMap) {
    Object.entries(stateMap || {}).forEach(([id, state]) => {
      const item = this.findById(id)
      if (!item) return
      const selected = Array.isArray(state) ? state.includes('selected') : state === 'selected'
      item.setState('selected', selected)
    })
  }

  getElementRenderBounds(id) {
    const item = this.findById(id)
    if (!item) return null
    // In G6 v4, getBBox() returns model coordinates which matches event.x/y
    const box = item.getBBox()
    return { min: [box.minX, box.minY], max: [box.maxX, box.maxY] }
  }

  setSize(width, height) {
    this.changeSize(width, height)
  }

  resize() { }

  render() {
    super.render()
    // 在渲染后将背景色应用到 canvas 容器，确保文字对比度
    if (this.__bgColor) {
      try {
        const canvasEl = this.get('canvas')?.get('el')
        if (canvasEl?.parentElement) {
          canvasEl.parentElement.style.background = this.__bgColor
        }
      } catch (_) { }
    }
    return Promise.resolve()
  }

  layout() {
    return Promise.resolve()
  }

  fitView(padding = 40) {
    try {
      super.fitView(padding)
    } catch (_) { }
    return Promise.resolve()
  }
}

// const emit = defineEmits(['import-indicators', 'import-template', 'edit-node'])

const props = defineProps({
  variant: {
    type: String,
    default: 'template',
    validator: v => ['template', 'system'].includes(v)
  },
  splitHeight: { type: String, default: 'min(72vh, 780px)' },
  /** 为 true 时在父级 flex 布局下占满剩余高度（侧栏 + 全高工作台场景） */
  fillParentHeight: { type: Boolean, default: false },
  showTemplateImport: { type: Boolean, default: false },
  draggable: { type: Boolean, default: true },
  /** 首个根节点默认名称：与模板名称 / 指标体系名称对齐，由父级传入 */
  preferredRootLabel: { type: String, default: '' },
  /**
   * 权重调优：none 关闭；manual / full 均只打开「权重分配调优」弹窗（原 full 与 manual 行为一致）
   */
  weightTuningMode: {
    type: String,
    default: 'none',
    validator: v => ['none', 'manual', 'full'].includes(v)
  },
  /** 指标体系全局赋权算法（节点级覆写展示等，不再参与弹窗内后端计算） */
  weightAssignAlgorithm: { type: String, default: undefined },
  /** 指标体系全局传导算法 */
  conductionAlgorithm: { type: [String, Object], default: undefined },
  globalWorkMode: { type: String, default: '' },
  /** 主分协同初始粗建阶段：隐藏叶节点的计算方法配置区域 */
  hideLeafCalcMethod: { type: Boolean, default: false },
  /** 透传：是否显示权重赋权计算按钮 */
  showObjectiveWeight: { type: Boolean, default: false },
  /** 透传：权重赋权计算加载状态 */
  objectiveWeightLoading: { type: Boolean, default: false },
  /** 透传：权重赋权计算禁用状态 */
  objectiveWeightDisabled: { type: Boolean, default: false }
})

const treeData = defineModel('treeData', { type: Array, default: () => [] })
const selectedNode = defineModel('selectedNode', { type: Object, default: null })

const splitPaneStyle = computed(() => {
  if (props.fillParentHeight) {
    return { flex: '1 1 0', minHeight: 0, overflow: 'hidden' }
  }
  return { height: props.splitHeight }
})

let uidSeq = 0
function genUid() {
  // 生成持久化风格的 ID：ind_ + 随机串 + 时间戳(36进制)
  const randomStr = Math.random().toString(36).substring(2, 8)
  const timestamp = Date.now().toString(36)
  return `ind_${randomStr}${timestamp}`
}

/** 节点有了子节点后，清除仅叶节点可配的字段 */
function stripLeafOnlyFields(node) {
  if (!node) return
  delete node.calcMethod
  delete node.computeRule
  delete node.valueCategory
  delete node.valueMin
  delete node.valueMax
}

/** 非叶节点：将历史 object 形态传导转为下拉字符串 */
function normalizeNodeConductionString(node) {
  if (!node?.children?.length) return
  if (node.conductionAlgorithm != null && typeof node.conductionAlgorithm === 'object') {
    const s = coerceConductionMethod(node.conductionAlgorithm)
    if (s) node.conductionAlgorithm = s
    else delete node.conductionAlgorithm
  }
}

const valueCategoryOptions = ZHPG_VALUE_CATEGORY_OPTIONS
const weightAssignOptions = ZHPG_WEIGHT_ASSIGN_OPTIONS
const conductionMethodOptions = ref([])
const { zhpg_equipment_type } = useDict('zhpg_equipment_type')

/** 指标体系根节点装备类型 → 配置算法里数据源默认分中心 */
const defaultDataSourceCenter = computed(() => {
  const roots = treeData.value
  if (!Array.isArray(roots) || !roots.length) return ''
  const t = roots[0]?.indicatorType
  if (!t || t === '无') return ''
  return String(t)
})

const weightTuningVisible = ref(false)

const innerTreeRef = ref(null)
const g6Ref = ref(null)
let graph = null
let resizeObs = null
let resizeFitTimer = null
const selectedUid = ref(null)
const lastDragCanvas = ref([0, 0])
/** 拖拽开始时节点的原始坐标，用于未发生合并时的还原 */
const dragStartPos = ref(null)
/** 拖拽合并时由 onDragElementFinish 接管图刷新，避免 treeData 监听重复执行重新布局 */
let suppressTreeSync = false
const contextMenuTargetUid = ref(null)
let hasAutoFittedGraph = false
/** @type {import('vue').Ref<{ type: 'cut'|'copy'; payload: object; removeUid?: string } | null>} */
const clipboard = ref(null)

const activeNode = computed(() => {
  if (!selectedUid.value || selectedUid.value === '__virtual_root__') return null
  return findNodeByUid(treeData.value, selectedUid.value)
})

const selectedLabel = computed(() => activeNode.value?.label || '—')

const selectedNodeIsLeaf = computed(() => {
  const n = selectedNode.value
  return !!n && (!n.children || n.children.length === 0)
})

const nodeLocalAlgoDialog = ref(false)

// 弹窗本地表单状态
const localWeightAssign = ref('')
const localConduction = ref('')

// 打开弹窗时复制当前节点值到本地状态
function openNodeLocalAlgoDialog() {
  const state = buildNodeAlgoDialogState(selectedNode.value, {
    weightAssignAlgorithm: props.weightAssignAlgorithm,
    conductionAlgorithm: props.conductionAlgorithm
  })
  localWeightAssign.value = state.weightAssignAlgorithm
  localConduction.value = state.conductionAlgorithm
  nodeLocalAlgoDialog.value = true
}

// 关闭弹窗时将本地状态写回节点
function closeNodeLocalAlgoDialog() {
  applyNodeAlgoDialogState(selectedNode.value, {
    weightAssignAlgorithm: localWeightAssign.value,
    conductionAlgorithm: localConduction.value
  }, {
    weightAssignAlgorithm: props.weightAssignAlgorithm,
    conductionAlgorithm: props.conductionAlgorithm
  })
  nodeLocalAlgoDialog.value = false
}

const workbenchNodeWeightSummary = computed(() => {
  const n = selectedNode.value
  if (!n || selectedNodeIsLeaf.value) return '—'
  if (n.weightAssignAlgorithm) {
    return getZhpgWeightAssignLabel(n.weightAssignAlgorithm)
  }
  if (props.weightAssignAlgorithm) {
    return '权重·' + getZhpgWeightAssignLabel(props.weightAssignAlgorithm)
  }
  return '权重·未配置'
})

const workbenchNodeConductionSummary = computed(() => {
  const n = selectedNode.value
  if (!n || selectedNodeIsLeaf.value) return '—'
  const c = coerceConductionMethod(n.conductionAlgorithm)
  if (c) {
    return getZhpgConductionMethodLabel(c, conductionMethodOptions.value)
  }
  const globalC = coerceConductionMethod(props.conductionAlgorithm)
  if (globalC) {
    return '传导·' + getZhpgConductionMethodLabel(globalC, conductionMethodOptions.value)
  }
  return '传导·未配置'
})

function clearWorkbenchNodeAlgoOverride() {
  const n = selectedNode.value
  if (!n) return
  delete n.weightAssignAlgorithm
  delete n.conductionAlgorithm
  // 同时清空本地状态
  localWeightAssign.value = ''
  localConduction.value = ''
  // 指标体系：权重由「权重分配调优」维护，清除覆盖时不删节点 weight
  if (props.variant !== 'system') {
    delete n.weight
  }
  ElMessage.success('已清除本节点覆盖')
}

const selectedLeafCalcPayload = computed(() => {
  const n = selectedNode.value
  if (!n) return ''
  if (n.computeRule != null && typeof n.computeRule === 'object') return n.computeRule
  if (n.calcMethod != null) return n.calcMethod
  return ''
})

const treeRootWorkMode = computed(() => treeData.value?.[0]?.workMode || '')

const selectedNodeWorkMode = computed(() => {
  if (props.globalWorkMode) return props.globalWorkMode
  return getCalcMethodWorkMode(
    selectedLeafCalcPayload.value,
    treeRootWorkMode.value || ZHPG_WORK_MODE.INTERNAL_CIRCULATION
  )
})

function onSelectedCalcMethodUpdate(v) {
  const n = selectedNode.value
  if (!n) return
  if (v == null) {
    delete n.computeRule
    delete n.calcMethod
    return
  }
  if (typeof v === 'object') {
    n.computeRule = v
    delete n.calcMethod
    return
  }
  const s = String(v)
  if (s.trim()) {
    n.calcMethod = s
    delete n.computeRule
  } else if (selectedNodeWorkMode.value !== ZHPG_WORK_MODE.INTERNAL_CIRCULATION) {
    n.calcMethod = setCalcMethodWorkMode('', selectedNodeWorkMode.value)
    delete n.computeRule
  } else {
    delete n.calcMethod
    delete n.computeRule
  }
}

function onSelectedWorkModeUpdate(workMode) {
  const n = selectedNode.value
  if (!n) return
  if (props.globalWorkMode) return
  if (n.computeRule && typeof n.computeRule === 'object') {
    n.computeRule = setCalcMethodWorkMode(n.computeRule, workMode)
    return
  }
  const cur = n.calcMethod != null ? String(n.calcMethod) : ''
  const next = setCalcMethodWorkMode(cur, workMode)
  if (next && typeof next === 'object') {
    n.computeRule = next
    delete n.calcMethod
  } else if (next && String(next).trim()) n.calcMethod = next
  else if (workMode === ZHPG_WORK_MODE.INTERNAL_CIRCULATION) {
    delete n.calcMethod
    delete n.computeRule
  }
}

watch(
  selectedNode,
  n => {
    if (props.variant !== 'system' || !n?.children?.length) return
    if (n.conductionAlgorithm != null && typeof n.conductionAlgorithm === 'object') {
      const s = coerceConductionMethod(n.conductionAlgorithm)
      n.conductionAlgorithm = s || undefined
    }
  },
  { immediate: true }
)

watch(
  () => treeData.value,
  () => {
    if (suppressTreeSync) return
    const fitView = shouldAutoFitInitialGraph()
    nextTick(() => syncGraphFromTreeData({ fitView }))
  },
  { deep: true }
)

/** 监听对话框打开：当 variant="system" 且父组件通过 ref 调用时，确保 graph 初始化后同步一次数据 */
watch(
  () => props.variant,
  () => {
    // variant 变化时说明组件可能被重新挂载，等待 nextTick 后确保 graph 已初始化
    nextTick(() => {
      if (graph && treeData.value?.length) {
        syncGraphFromTreeData({ fitView: shouldAutoFitInitialGraph() })
      }
    })
  }
)

/** 监听 html.dark 类变化，主题切换时重建图 */
let darkModeObs = null
function onThemeChange() {
  if (!graph || !g6Ref.value) return
  graph.destroy()
  graph = null
  nextTick(() => initGraph())
}
watch(() => themeStore.currentThemeName, () => {
  console.log("222 ", themeStore.currentThemeName)
  onThemeChange()
})
onMounted(() => {
  fetchZhpgConductionMethodSelectOptions()
    .then(opts => {
      conductionMethodOptions.value = opts
    })
    .catch(() => { })
  nextTick(() => initGraph())
  
})

onBeforeUnmount(() => {
  if (resizeFitTimer) clearTimeout(resizeFitTimer)
  resizeFitTimer = null
  resizeObs?.disconnect()
  resizeObs = null
  darkModeObs?.disconnect()
  darkModeObs = null
  graph?.destroy()
  graph = null
  window.removeEventListener('resize', onWinResize)
})

function allowTreeDrop(_draggingNode, _dropNode, type) {
  return type !== 'inner' || true
}

function onNodeDrop() {
  syncGraphFromTreeData()
}

function findNodeByUid(list, uid) {
  if (!list) return null
  for (const n of list) {
    if (n.uid === uid) return n
    const c = findNodeByUid(n.children || [], uid)
    if (c) return c
  }
  return null
}

function collectLabelsFromForest(list, acc = []) {
  for (const n of list || []) {
    if (n?.label != null && n.label !== '') acc.push(String(n.label))
    collectLabelsFromForest(n.children, acc)
  }
  return acc
}

/** 在整棵树中生成不重复的默认名称，如「新子节点」「新子节点-2」… */
function nextDistinctName(base) {
  const used = new Set(collectLabelsFromForest(treeData.value))
  if (!used.has(base)) return base
  let i = 2
  while (used.has(`${base}-${i}`)) i++
  return `${base}-${i}`
}

function findParentListAndIndex(nodes, uid, parentList = null) {
  for (let i = 0; i < nodes.length; i++) {
    if (nodes[i].uid === uid) return { list: nodes, index: i, parentList }
    const ch = nodes[i].children
    if (ch && ch.length) {
      const r = findParentListAndIndex(ch, uid, nodes)
      if (r) return r
    }
  }
  return null
}

/** candidateId 是否位于 ancestorUid 的子树中（不含自身） */
function isUnderAncestor(ancestorUid, candidateUid) {
  const anc = findNodeByUid(treeData.value, ancestorUid)
  if (!anc || !candidateUid) return false
  function walk(n) {
    if (n.uid === candidateUid) return true
    return (n.children || []).some(walk)
  }
  return (anc.children || []).some(walk)
}

/** 亮色分支配色——高饱和鲜明色彩，确保节点和连线清晰可见 */
const BRANCH_PALETTE_LIGHT = ['#4f46e5', '#059669', '#d97706', '#dc2626', '#7c3aed', '#0284c7', '#db2777', '#0d9488']
/** 暗色分支配色——高亮霓虹色系 */
const BRANCH_PALETTE_DARK = ['#22d3ee', '#34d399', '#a78bfa', '#fb923c', '#f472b6', '#38bdf8', '#facc15', '#c084fc']

const isDarkMode = computed(() => {
  // 通过 src/store/theme.js 的 useThemeStore 判断
  
  
  console.log('isDarkMode', themeStore.currentThemeName)
  return themeStore.currentThemeName === 'dark-theme';
})
const getBranchPalette = computed(() => isDarkMode.value ? BRANCH_PALETTE_DARK : BRANCH_PALETTE_LIGHT)

/** 普通指标节点固定占位：宽度适配约8个中文字符，高度适配单/双行名称 */
const GRAPH_NODE_FIXED_W = 200
const GRAPH_NODE_FIXED_H = 46
/** 图形区标题最大字符，超出省略号 */
const GRAPH_TITLE_MAX_CHARS = 20

function hexToRgba(hex, alpha) {
  const m = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex || '')
  if (!m) return `rgba(255,255,255,${alpha})`
  return `rgba(${parseInt(m[1], 16)},${parseInt(m[2], 16)},${parseInt(m[3], 16)},${alpha})`
}

function truncateGraphTitle(title) {
  if (!title) return ''
  const t = String(title)
  const maxW = GRAPH_NODE_FIXED_W - 28
  let res = ''
  let lineW = 0
  let lineCount = 1
  for (let i = 0; i < t.length; i++) {
    const ch = t[i]
    const w = ch.charCodeAt(0) > 255 ? 13 : 13 * 0.58
    if (lineW + w > maxW) {
      if (lineCount >= 2) {
        return res.slice(0, res.length - 1) + '…'
      }
      res += '\n'
      lineW = 0
      lineCount++
    }
    res += ch
    lineW += w
  }
  return res
}

/** 将业务森林转为 G6 TreeData（多根时加虚拟根 __vroot__） */
function forestToTreeDatum(roots) {
  const palette = getBranchPalette.value
  function conv(n, branchColor = null) {
    const raw = n.children || []
    const children = raw.map((ch, idx) => {
      const nextColor = branchColor === null ? palette[idx % palette.length] : branchColor
      return conv(ch, nextColor)
    })
    return {
      id: n.uid,
      label: n.label || '未命名',
      valueCategory: n.valueCategory,
      valueMin: n.valueMin,
      valueMax: n.valueMax,
      weight: n.weight,
      branchColor,
      children
    }
  }
  if (!roots?.length) {
    return { id: '__empty__', label: '（空）', branchColor: null, children: [] }
  }
  if (roots.length === 1) return conv(roots[0], null)
  return {
    id: '__vroot__',
    label: '指标体系',
    branchColor: null,
    children: roots.map((r, i) => conv(r, palette[i % palette.length]))
  }
}

function graphNodeSubtitle(datum) {
  if (!datum || typeof datum !== 'object') return ''
  const parts = []
  if (datum.valueCategory) {
    const lb = getZhpgValueCategoryLabel(datum.valueCategory)
    if (lb && lb !== '—') parts.push(lb)
  }
  if ((datum.valueMin != null && datum.valueMin !== '') || (datum.valueMax != null && datum.valueMax !== '')) {
    parts.push(`${datum.valueMin ?? '—'} 至 ${datum.valueMax ?? '—'}`)
  }
  if (parts.length) return parts.join('\n')
  const w = datum.weight
  if (w == null || w === '') return ''
  const num = typeof w === 'number' ? w : Number(w)
  const ws = Number.isFinite(num) ? String(num) : String(w)
  return `w=${ws}`
}

/** 左侧树等：完整名称；图形：截断后展示 */
function graphNodeLabelText(title, datum) {
  const head = truncateGraphTitle(title || '')
  const sub = graphNodeSubtitle(datum)
  if (!sub) return head
  return `${head}\n${sub}`
}

/** 估算文本占位宽度（13px 字号，中英混排）——仅用于虚拟根/空节点 */
function estimateTextWidthPx(str, fontSize = 13) {
  if (!str) return 0
  let w = 0
  for (const ch of str) {
    w += ch.charCodeAt(0) > 255 ? fontSize : fontSize * 0.58
  }
  return w
}

/**
 * 与 compact-box 的 getWidth/getHeight 一致；普通节点固定宽高，避免长名称撑破布局
 */
function computeNodeLayoutBox(title, datum, id) {
  const isVroot = id === '__vroot__'
  const isEmpty = id === '__empty__'
  const kind = isVroot ? 'vroot' : isEmpty ? 'empty' : 'normal'

  if (kind === 'normal') {
    const padX = 44
    return {
      w: GRAPH_NODE_FIXED_W,
      h: GRAPH_NODE_FIXED_H,
      labelMaxWidth: GRAPH_NODE_FIXED_W - padX,
      kind
    }
  }

  const text = graphNodeLabelText(title || id, datum)
  const lines = text.split('\n')
  const maxContentW = kind === 'empty' ? 88 : 120
  const lineHeight = 18
  const padX = 28
  const padY = 22

  let contentW = 0
  let lineCount = 0
  for (const line of lines) {
    const lw = estimateTextWidthPx(line, 13)
    if (lw <= maxContentW) {
      contentW = Math.max(contentW, lw)
      lineCount += 1
    } else {
      const approxCharsPerLine = Math.max(6, Math.floor(maxContentW / 13))
      lineCount += Math.max(1, Math.ceil(line.length / approxCharsPerLine))
      contentW = maxContentW
    }
  }

  const minW = kind === 'empty' ? 100 : 124
  const maxW = kind === 'empty' ? 120 : 168
  const w = Math.round(Math.min(Math.max(contentW + padX, minW), maxW))
  const h = Math.round(Math.max(lineCount * lineHeight + padY, kind === 'vroot' ? 46 : 40))

  return {
    w,
    h,
    labelMaxWidth: Math.max(56, w - padX),
    kind
  }
}

function toGraphData(roots) {
  const td = forestToTreeDatum(roots)
  return treeToGraphData(td, {
    getNodeData: (datum, depth) => {
      const id = datum.id
      const isVroot = id === '__vroot__'
      const isEmpty = id === '__empty__'
      const title = datum.label || id
      const dark = isDarkMode.value
      const palette = getBranchPalette.value
      const accent = datum.branchColor || palette[0]
      const { w, h, labelMaxWidth, kind } = computeNodeLayoutBox(title, datum, id)
      const isLeaf = !datum.children || datum.children.length === 0

      /* ---- 亮色 / 暗色双主题节点样式 ---- */
      let nodeFill, nodeStroke, nodeLW, labelColor, shadowC, shadowB, shadowOY
      if (dark) {
        /* 暗色：带色底 + 霓虹描边 + 发光 */
        labelColor = isEmpty ? '#6e85a5' : isVroot ? '#e2e8f0' : '#f0f4ff'
        nodeFill = isEmpty
          ? 'rgba(15,23,42,0.6)'
          : isVroot ? 'rgba(15,30,55,0.95)'
          : hexToRgba(accent, depth === 0 ? 0.32 : isLeaf ? 0.22 : 0.27)
        nodeStroke = isEmpty ? 'rgba(100,120,150,0.3)' : isVroot ? '#38bdf8' : accent
        nodeLW = isVroot ? 2 : isEmpty ? 1 : depth === 0 ? 2 : 1.5
        shadowC = isEmpty ? 'transparent' : hexToRgba(accent, 0.35)
        shadowB = isEmpty ? 0 : isVroot ? 20 : depth === 0 ? 16 : 10
        shadowOY = 0
      } else {
        /* 亮色：纯色填充 + 彩色描边 */
        labelColor = isEmpty ? '#94a3b8' : isVroot ? '#ffffff' : '#1e293b'
        nodeFill = isEmpty
          ? '#f1f5f9'
          : isVroot ? accent
          : hexToRgba(accent, 0.12)
        nodeStroke = isEmpty ? '#e2e8f0' : isVroot ? accent : accent
        nodeLW = isVroot ? 0 : isEmpty ? 1 : 2
        shadowC = isEmpty ? 'transparent' : hexToRgba(accent, 0.25)
        shadowB = isEmpty ? 0 : isVroot ? 20 : 14
        shadowOY = isVroot ? 3 : 4
      }

      return {
        id,
        depth,
        data: {
          kind,
          accent,
          layoutWidth: w,
          layoutHeight: h
        },
        style: {
          labelText: graphNodeLabelText(title, datum),
          labelFill: labelColor,
          labelFontSize: 13,
          labelFontWeight: isVroot ? 700 : depth === 0 ? 600 : 500,
          labelLineHeight: 18,
          labelPlacement: 'center',
          labelMaxWidth,
          labelWordWrap: true,
          fill: nodeFill,
          stroke: nodeStroke,
          lineWidth: nodeLW,
          radius: isVroot ? 24 : 8,
          size: [w, h],
          opacity: isEmpty ? 0.55 : 1,
          shadowColor: shadowC,
          shadowBlur: shadowB,
          shadowOffsetX: 0,
          shadowOffsetY: shadowOY,
          icon: false,
          port: true,
          halo: false,
          badge: false,
          /* 亮色模式：普通节点左侧彩色指示条（通过 border 模拟） */
          badgePalette: !dark && !isVroot && !isEmpty ? [accent] : undefined
        },
        children: (datum.children || []).map(c => c.id)
      }
    },
    getEdgeData: (s, t) => {
      const dark = isDarkMode.value
      const palette = getBranchPalette.value
      const accent = t.branchColor || palette[0]
      return {
        id: `e-${s.id}-${t.id}`,
        source: s.id,
        target: t.id,
        style: {
          stroke: accent,
          lineWidth: dark ? 2 : 2.5,
          opacity: dark ? 0.7 : 0.65,
          shadowColor: hexToRgba(accent, dark ? 0.3 : 0.18),
          shadowBlur: dark ? 6 : 4
        }
      }
    },
    getChildren: d => d.children || []
  })
}

async function applyGraphNodeSelection() {
  if (!graph) return
  const nodes = graph.getNodeData()
  const su = selectedUid.value
  const map = {}
  for (const n of nodes) {
    const id = n.id
    map[id] = su && id === su && su !== '__empty__' && su !== '__vroot__' ? 'selected' : []
  }
  await graph.setElementState(map, false)
}

watch(selectedUid, () => {
  nextTick(() => applyGraphNodeSelection().catch(() => { }))
})

function initGraph() {
  const el = g6Ref.value
  if (!el) return

  const contextmenuItems = e => {
    const id = e.target?.id
    contextMenuTargetUid.value = id || null
    if (!id || id === '__empty__') return []
    const items = []
    if (id !== '__vroot__' && id !== '__empty__' && props.variant === 'template') {
      items.push({ name: '编辑', value: 'edit-node' })
    }
    if (id !== '__vroot__') {
      items.push({ name: '新建子节点', value: 'add-child' })
      items.push({ name: '新建兄弟节点（后）', value: 'add-sibling-after' })
      items.push({ name: '新建兄弟节点（前）', value: 'add-sibling-before' })
      items.push({ name: '新建父节点（包一层）', value: 'wrap-parent' })
    }
    items.push({ name: '剪切', value: 'cut' })
    items.push({ name: '拷贝', value: 'copy' })
    if (clipboard.value) {
      items.push({ name: '粘贴为子节点', value: 'paste-child' })
      if (id === '__vroot__' || id !== '__empty__') {
        items.push({ name: '粘贴为根（与多根并列）', value: 'paste-root' })
      }
    }
    if (id !== '__vroot__' && id !== '__empty__') {
      items.push({ name: '删除子树', value: 'del-subtree' })
      items.push({ name: '删除本节点（子节点上移）', value: 'del-promote' })
    }
    return items
  }

  const onContextClick = value => {
    const tid = contextMenuTargetUid.value
    if (!tid || !value) return
    switch (value) {
      case 'edit-node': {
        const n = findNodeByUid(treeData.value, tid)
        if (n) emit('edit-node', n)
        break
      }
      case 'add-child':
        if (tid === '__vroot__') {
          onAddRoot()
        } else {
          const p = findNodeByUid(treeData.value, tid)
          if (p) addChild(p)
        }
        break
      case 'add-sibling-after':
        addSibling(tid, 1)
        break
      case 'add-sibling-before':
        addSibling(tid, 0)
        break
      case 'wrap-parent':
        wrapWithNewParent(tid)
        break
      case 'copy':
        copyNode(tid)
        break
      case 'cut':
        cutNode(tid)
        break
      case 'paste-child':
        pasteUnder(tid)
        break
      case 'paste-root':
        pasteAsRoot()
        break
      case 'del-subtree':
        confirmRemoveByUid(tid)
        break
      case 'del-promote':
        deleteNodePromoteChildren(tid)
        break
      default:
        break
    }
    syncGraphFromTreeData()
  }

  const dark = isDarkMode.value
  graph = new Graph({
    container: el,
    width: el.clientWidth || 400,
    height: el.clientHeight || 320,
    background: dark ? '#0a1628' : '#f8fafc',
    animation: false,
    data: toGraphData(treeData.value),
    node: {
      type: 'rect',
      style: {
        labelText: d => d.style?.labelText || d.id,
        labelFill: d => d.style?.labelFill,
        labelFontSize: d => d.style?.labelFontSize,
        labelFontWeight: d => d.style?.labelFontWeight,
        labelLineHeight: d => d.style?.labelLineHeight,
        labelMaxWidth: d => d.style?.labelMaxWidth,
        labelWordWrap: d => d.style?.labelWordWrap,
        labelPlacement: d => d.style?.labelPlacement ?? 'center',
        fill: d => d.style?.fill ?? (isDarkMode.value ? 'rgba(15,23,42,0.85)' : '#f8fafc'),
        stroke: d => d.style?.stroke ?? (isDarkMode.value ? '#38bdf8' : '#6366f1'),
        lineWidth: d => d.style?.lineWidth,
        radius: d => d.style?.radius ?? 8,
        size: d => d.style?.size,
        opacity: d => d.style?.opacity,
        shadowColor: d => d.style?.shadowColor,
        shadowBlur: d => d.style?.shadowBlur,
        shadowOffsetX: d => d.style?.shadowOffsetX,
        shadowOffsetY: d => d.style?.shadowOffsetY,
        icon: d => d.style?.icon ?? false,
        port: d => d.style?.port ?? true,
        halo: d => d.style?.halo ?? false,
        badge: d => d.style?.badge ?? false
      },
      state: {
        selected: dark ? {
          lineWidth: 2.5,
          stroke: '#22d3ee',
          fill: 'rgba(15,23,42,0.85)',
          shadowColor: 'rgba(34, 211, 238, 0.45)',
          shadowBlur: 24,
          shadowOffsetY: 0
        } : {
          lineWidth: 2,
          stroke: '#6366f1',
          fill: '#f8fafc',
          shadowColor: 'rgba(99, 102, 241, 0.25)',
          shadowBlur: 16,
          shadowOffsetY: 2
        }
      }
    },
    edge: {
      type: 'cubic-horizontal',
      style: {
        stroke: d => d.style?.stroke ?? (dark ? '#38bdf8' : '#6366f1'),
        lineWidth: d => d.style?.lineWidth ?? 2.5,
        opacity: d => d.style?.opacity ?? 0.65,
        shadowColor: d => d.style?.shadowColor ?? 'transparent',
        shadowBlur: d => d.style?.shadowBlur ?? 0
      }
    },
    layout: {
      type: 'compact-box',
      direction: 'LR',
      getWidth: d => {
        const w = d?.data?.layoutWidth
        return typeof w === 'number' && w > 0 ? w : 140
      },
      getHeight: d => {
        const h = d?.data?.layoutHeight
        return typeof h === 'number' && h > 0 ? h : 60
      },
      getHGap: () => 36,
      getVGap: () => 26
    },
    behaviors: [
      'zoom-canvas',
      'drag-canvas',
      {
        type: 'drag-element',
        enable: ev => {
          const id = ev.target?.id
          return !!id && id !== '__empty__' && id !== '__vroot__'
        },
        shadow: false,
        onFinish: async ids => {
          await onDragElementFinish(ids)
        }
      }
    ],
    plugins: [
      {
        type: 'contextmenu',
        trigger: 'contextmenu',
        enable: ev => {
          const id = ev.target?.id
          return !!id && id !== '__empty__'
        },
        getItems: contextmenuItems,
        onClick: value => onContextClick(value)
      }
    ]
  })

  graph.on('node:dragstart', e => {
    const event = normalizeG6NodeEvent(e)
    const id = event.target?.id
    if (!id) return
    const item = graph.findById(id)
    const model = item?.getModel?.()
    if (model && Number.isFinite(model.x) && Number.isFinite(model.y)) {
      dragStartPos.value = { id, x: model.x, y: model.y }
    } else {
      dragStartPos.value = null
    }
  })

  graph.on(NodeEvent.DRAG, e => {
    const event = normalizeG6NodeEvent(e)
    // Use model coordinates (x, y) for reliable hit testing in G6 v4
    const x = event.x ?? event.canvas?.x ?? 0
    const y = event.y ?? event.canvas?.y ?? 0
    lastDragCanvas.value = [x, y]
  })

  graph.on('node:dragend', e => {
    const event = normalizeG6NodeEvent(e)
    const id = event.target?.id
    if (!id) return
    const x = event.x ?? event.canvas?.x ?? 0
    const y = event.y ?? event.canvas?.y ?? 0
    if (Number.isFinite(x) && Number.isFinite(y)) {
      lastDragCanvas.value = [x, y]
    }
    onDragElementFinish([id])
  })

  graph.on(NodeEvent.CLICK, e => {
    const event = normalizeG6NodeEvent(e)
    const id = event.target?.id
    if (!id || id === '__empty__') return
    if (id === '__vroot__') return
    selectedUid.value = id
    if (props.variant === 'system') {
      selectedNode.value = findNodeByUid(treeData.value, id)
    }
    innerTreeRef.value?.setCurrentKey(id)
  })

  graph.render().then(async () => {
    // 延迟一两帧执行适配，确保容器尺寸和渲染队列已同步完成
    if (shouldAutoFitInitialGraph()) {
      await fitGraphAfterStableLayout()
    }
    await applyGraphNodeSelection().catch(() => { })
  })
  window.addEventListener('resize', onWinResize)
  resizeObs = new ResizeObserver(() => {
    if (!graph || !g6Ref.value) return
    graph.setSize(g6Ref.value.clientWidth, g6Ref.value.clientHeight)
    graph.resize()
    // 仅调整图表尺寸，不执行 fitView，确保用户当前的缩放/平移状态在容器大小变化（如面板切换）时得以维持
  })
  resizeObs.observe(el)
}

function onWinResize() {
  if (!graph || !g6Ref.value) return
  graph.setSize(g6Ref.value.clientWidth, g6Ref.value.clientHeight)
  graph.resize()
}

async function syncGraphFromTreeData(arg) {
  // 如果 graph 还未初始化（组件刚挂载），等待下一轮再同步
  if (!graph) {
    await nextTick()
    if (!graph) return
  }
  const doFit = arg && typeof arg === 'object' && arg.fitView === true
  graph.setData(toGraphData(treeData.value))
  await graph.layout()
  await graph.render()
  if (doFit) {
    await fitGraphAfterStableLayout()
  }
  await applyGraphNodeSelection().catch(() => { })
}

async function onDragElementFinish(ids) {
  if (!graph || !ids?.length) return
  const dragId = ids[0]
  if (dragId === '__empty__' || dragId === '__vroot__') {
    await syncGraphFromTreeData()
    return
  }
  const [cx, cy] = lastDragCanvas.value
  const hitId = findDropTargetNodeId(cx, cy, dragId)
  const original = dragStartPos.value

  // 未发生合并：将节点平滑弹回拖拽前的位置
  if (!hitId || hitId === dragId) {
    if (original && original.id === dragId) {
      const item = graph.findById(dragId)
      if (item) {
        graph.updateItem(item, { x: original.x, y: original.y })
      }
    }
    dragStartPos.value = null
    await applyGraphNodeSelection().catch(() => { })
    return
  }

  // 在调用 tryReparentById 修改数据之前，先记录一下拖入目标节点（hit）当前在模型坐标系中的位置，
  // 它将作为我们重排版后还原视野时使用的锚点。
  const oldHitModel = graph.findById(hitId)?.getModel?.()
  const oldHitX = oldHitModel?.x
  const oldHitY = oldHitModel?.y

  if (!tryReparentById(dragId, hitId)) {
    if (original && original.id === dragId) {
      const item = graph.findById(dragId)
      if (item) {
        graph.updateItem(item, { x: original.x, y: original.y })
      }
    }
    dragStartPos.value = null
    await applyGraphNodeSelection().catch(() => { })
    return
  }

  // 发生合并：
  ElMessage.success('已合并到目标节点下')
  suppressTreeSync = true
  try {
    // 重新进行 layout + render（结构变更后必须，否则新增节点会重叠）
    graph.setData(toGraphData(treeData.value))
    await graph.layout()
    await graph.render()

    // 重排版后，再次取一下目标节点的模型坐标。为了让用户感觉不到画板抖动，
    // 我们用 (oldX - newX)*zoom 的距离平移整个画布，使得目标节点在屏幕上的位置保持不变。
    const newHitModel = graph.findById(hitId)?.getModel?.()
    if (
      Number.isFinite(oldHitX) &&
      Number.isFinite(oldHitY) &&
      Number.isFinite(newHitModel?.x) &&
      Number.isFinite(newHitModel?.y)
    ) {
      const zoom = graph.getZoom() ?? 1
      const dx = (oldHitX - newHitModel.x) * zoom
      const dy = (oldHitY - newHitModel.y) * zoom
      if (Number.isFinite(dx) && Number.isFinite(dy) && (dx !== 0 || dy !== 0)) {
        graph.translate(dx, dy)
      }
    }
  } finally {
    dragStartPos.value = null
    nextTick(() => { suppressTreeSync = false })
  }
  await applyGraphNodeSelection().catch(() => { })
}

function findDropTargetNodeId(canvasX, canvasY, dragId) {
  if (!graph) return null
  const nodes = graph.getNodeData()
  let best = null
  let bestScore = Infinity
  for (const nd of nodes) {
    const id = nd.id
    if (!id || id === dragId || id === '__empty__') continue
    if (isUnderAncestor(dragId, id)) continue
    let bounds
    try {
      bounds = graph.getElementRenderBounds(id)
    } catch {
      continue
    }
    if (!bounds) continue
    const min = bounds.min
    const max = bounds.max
    const rcx = (min[0] + max[0]) / 2
    const rcy = (min[1] + max[1]) / 2
    const rw = Math.max(max[0] - min[0], 24) / 2 + 28
    const rh = Math.max(max[1] - min[1], 24) / 2 + 28
    const dx = Math.abs(canvasX - rcx)
    const dy = Math.abs(canvasY - rcy)
    if (dx <= rw && dy <= rh) {
      const score = dx + dy
      if (score < bestScore) {
        bestScore = score
        best = id
      }
    }
  }
  return best
}

function tryReparentById(dragUid, newParentId) {
  if (dragUid === newParentId) return false
  if (isUnderAncestor(dragUid, newParentId)) return false
  const dragNode = findNodeByUid(treeData.value, dragUid)
  if (!dragNode) return false

  if (newParentId === '__vroot__') {
    const pos = findParentListAndIndex(treeData.value, dragUid)
    if (!pos) return false
    const n = pos.list[pos.index]
    pos.list.splice(pos.index, 1)
    if (!treeData.value.some(r => r.uid === n.uid)) treeData.value.push(n)
    return true
  }

  const newParent = findNodeByUid(treeData.value, newParentId)
  if (!newParent) return false
  const pos = findParentListAndIndex(treeData.value, dragUid)
  if (!pos) return false
  if (newParent.children?.some(c => c.uid === dragUid)) return true

  const n = pos.list[pos.index]
  pos.list.splice(pos.index, 1)
  if (!newParent.children) newParent.children = []
  stripLeafOnlyFields(newParent)
  normalizeNodeConductionString(newParent)
  newParent.children.push(n)
  return true
}

function shouldAutoFitInitialGraph() {
  if (hasAutoFittedGraph) return false
  if (!treeData.value?.length) return false
  hasAutoFittedGraph = true
  return true
}

function waitStableGraphFrame() {
  return new Promise(resolve => {
    if (typeof requestAnimationFrame !== 'function') {
      setTimeout(resolve, 80)
      return
    }
    requestAnimationFrame(() => requestAnimationFrame(resolve))
  })
}

async function fitGraphAfterStableLayout() {
  await nextTick()
  await waitStableGraphFrame()
  graphFit()
}

function graphFit() {
  if (!graph || !g6Ref.value) return
  graph.setSize(g6Ref.value.clientWidth, g6Ref.value.clientHeight)
  graph.resize()
  graph.fitView({}, false).catch(() => { })
}

function onTreeNodeClick(data) {
  if (!data?.uid) return
  selectedUid.value = data.uid
  if (props.variant === 'system') {
    selectedNode.value = data
  }
}

function onAddRoot() {
  const isFirstRoot = treeData.value.length === 0
  const hint = (props.preferredRootLabel && String(props.preferredRootLabel).trim()) || ''
  const label = isFirstRoot && hint ? hint : nextDistinctName('新指标节点')
  treeData.value.push({
    uid: genUid(),
    label,
    unit: '',
    description: '',
    children: []
  })
  nextTick(() => syncGraphFromTreeData())
}

function addChild(data) {
  if (!data.children) data.children = []
  stripLeafOnlyFields(data)
  normalizeNodeConductionString(data)
  data.children.push({
    uid: genUid(),
    label: nextDistinctName('新子节点'),
    unit: '',
    description: '',
    children: []
  })
  syncGraphFromTreeData()
}

function addSibling(targetUid, offset) {
  const pos = findParentListAndIndex(treeData.value, targetUid)
  if (!pos) return
  const insertAt = pos.index + offset
  const newbie = {
    uid: genUid(),
    label: nextDistinctName('新兄弟节点'),
    unit: '',
    description: '',
    children: []
  }
  pos.list.splice(Math.max(0, insertAt), 0, newbie)
}

function wrapWithNewParent(targetUid) {
  const pos = findParentListAndIndex(treeData.value, targetUid)
  if (!pos) return
  const node = pos.list[pos.index]
  const wrapper = {
    uid: genUid(),
    label: nextDistinctName('新父节点'),
    unit: '',
    description: '',
    children: [node]
  }
  pos.list.splice(pos.index, 1, wrapper)
}

function cloneNodeNewIds(n) {
  let cond = n.conductionAlgorithm
  if (cond != null && typeof cond === 'object') cond = coerceConductionMethod(cond) || undefined
  return {
    uid: genUid(),
    label: n.label,
    unit: n.unit || '',
    description: n.description || '',
    calcMethod: n.calcMethod,
    computeRule: n.computeRule,
    valueCategory: n.valueCategory,
    valueMin: n.valueMin,
    valueMax: n.valueMax,
    weight: n.weight,
    weightAssignAlgorithm: n.weightAssignAlgorithm,
    conductionAlgorithm: cond,
    children: (n.children || []).map(cloneNodeNewIds)
  }
}

function copyNode(uid) {
  const n = findNodeByUid(treeData.value, uid)
  if (!n) return
  clipboard.value = { type: 'copy', payload: cloneNodeNewIds(n) }
  ElMessage.success('已拷贝子树')
}

function cutNode(uid) {
  const n = findNodeByUid(treeData.value, uid)
  if (!n) return
  clipboard.value = { type: 'cut', payload: cloneNodeNewIds(n), removeUid: uid }
  ElMessage.success('已剪切（粘贴后从原位置移除）')
}

function pasteUnder(parentUid) {
  const c = clipboard.value
  if (!c) return
  if (parentUid === '__vroot__') {
    treeData.value.push(c.payload)
  } else {
    const p = findNodeByUid(treeData.value, parentUid)
    if (!p) return
    if (!p.children) p.children = []
    stripLeafOnlyFields(p)
    normalizeNodeConductionString(p)
    p.children.push(c.payload)
  }
  if (c.type === 'cut' && c.removeUid) {
    removeUidFromForest(c.removeUid)
  }
  clipboard.value = null
  ElMessage.success('已粘贴')
}

function pasteAsRoot() {
  const c = clipboard.value
  if (!c) return
  treeData.value.push(c.payload)
  if (c.type === 'cut' && c.removeUid) {
    removeUidFromForest(c.removeUid)
  }
  clipboard.value = null
  ElMessage.success('已粘贴为根节点')
}

function removeUidFromForest(uid) {
  const pos = findParentListAndIndex(treeData.value, uid)
  if (pos) pos.list.splice(pos.index, 1)
}

function deleteNodePromoteChildren(uid) {
  const pos = findParentListAndIndex(treeData.value, uid)
  if (!pos) return
  const node = pos.list[pos.index]
  const ch = node.children || []
  pos.list.splice(pos.index, 1, ...ch)
  if (selectedUid.value === uid) {
    selectedUid.value = null
    if (props.variant === 'system') selectedNode.value = null
  }
}

function confirmRemove(node, data) {
  ElMessageBox.confirm('确定删除该节点及其全部子节点？', '裁剪确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
    .then(() => {
      removeTreeNode(node, data)
      if (selectedUid.value === data.uid) {
        selectedUid.value = null
        if (props.variant === 'system') selectedNode.value = null
      }
      syncGraphFromTreeData()
    })
    .catch(() => { })
}

function confirmRemoveByUid(uid) {
  const nodeObj = findNodeByUid(treeData.value, uid)
  if (!nodeObj) return
  ElMessageBox.confirm('确定删除该节点及其全部子节点？', '裁剪确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
    .then(() => {
      const pos = findParentListAndIndex(treeData.value, uid)
      if (pos) {
        pos.list.splice(pos.index, 1)
      }
      if (selectedUid.value === uid) {
        selectedUid.value = null
        if (props.variant === 'system') selectedNode.value = null
      }
      innerTreeRef.value?.setCurrentKey(null)
      syncGraphFromTreeData()
    })
    .catch(() => { })
}

function removeTreeNode(node, data) {
  const parent = node.parent
  const children = parent.data.children || parent.data
  const index = children.indexOf(data)
  if (index > -1) children.splice(index, 1)
}

function emitEditIfTemplate() {
  if (props.variant === 'template' && activeNode.value) {
    emit('edit-node', activeNode.value)
  }
}

watch(selectedNode, n => {
  if (props.variant !== 'system' || !n?.uid) return
  selectedUid.value = n.uid
  nextTick(() => innerTreeRef.value?.setCurrentKey(n.uid))
})

defineExpose({
  renderGraph: syncGraphFromTreeData,
  genUid
})
</script>

<style scoped lang="scss">
/* ========== 主题变量 ========== */


/* ========== 布局 ========== */
.indicator-tree-workbench {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.indicator-tree-workbench--fill {
  flex: 1;
  min-height: 80vh;
  height: 100%;
}

.indicator-tree-workbench--fill .workbench-split {
  min-height: 0;
}

.workbench-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
}

.toolbar-grp {
  margin-right: 4px;
}

.toolbar-grp :deep(.el-button),
.graph-actions :deep(.el-button),
.node-algo-trigger-row :deep(.el-button) {
  border-radius: 10px;
}

.toolbar-grp :deep(.el-button--primary),
.graph-actions :deep(.el-button--primary),
.node-algo-trigger-row :deep(.el-button--primary) {
  background: var(--itw-btn-bg);
  border-color: var(--itw-btn-border);
  color: var(--itw-btn-text);
}

.toolbar-grp :deep(.el-button--primary:hover),
.toolbar-grp :deep(.el-button--primary:focus-visible),
.graph-actions :deep(.el-button--primary:hover),
.graph-actions :deep(.el-button--primary:focus-visible),
.node-algo-trigger-row :deep(.el-button--primary:hover),
.node-algo-trigger-row :deep(.el-button--primary:focus-visible) {
  background: var(--itw-btn-bg-hover);
  border-color: var(--itw-btn-border-hover);
  color: var(--itw-btn-text);
}

.toolbar-grp :deep(.el-button:not(.el-button--primary):not(.is-link)),
.graph-actions :deep(.el-button:not(.el-button--primary):not(.el-button--danger):not(.is-link)),
.node-algo-trigger-row :deep(.el-button:not(.el-button--primary):not(.is-link)) {
  background: var(--itw-btn-secondary-bg);
  border-color: var(--itw-btn-secondary-border);
  color: var(--itw-btn-secondary-text);
}

.toolbar-grp :deep(.el-button:not(.el-button--primary):not(.is-link):hover),
.toolbar-grp :deep(.el-button:not(.el-button--primary):not(.is-link):focus-visible),
.graph-actions :deep(.el-button:not(.el-button--primary):not(.el-button--danger):not(.is-link):hover),
.graph-actions :deep(.el-button:not(.el-button--primary):not(.el-button--danger):not(.is-link):focus-visible),
.node-algo-trigger-row :deep(.el-button:not(.el-button--primary):not(.is-link):hover),
.node-algo-trigger-row :deep(.el-button:not(.el-button--primary):not(.is-link):focus-visible) {
  background: var(--itw-btn-secondary-bg-hover);
  border-color: var(--itw-btn-secondary-border-hover);
  color: var(--itw-btn-secondary-text);
}

.graph-actions :deep(.el-button--danger),
.graph-actions :deep(.el-button--danger.is-plain) {
  background: var(--itw-danger-bg);
  border-color: var(--itw-danger-border);
  color: var(--itw-danger-text);
}

.graph-actions :deep(.el-button--danger:hover),
.graph-actions :deep(.el-button--danger:focus-visible),
.graph-actions :deep(.el-button--danger.is-plain:hover),
.graph-actions :deep(.el-button--danger.is-plain:focus-visible) {
  background: var(--itw-danger-bg-hover);
  border-color: var(--itw-danger-border-hover);
  color: #ffe4e6;
}

.graph-tools {
  margin-left: auto;
}

.graph-roam-hint {
  font-size: 13px;
  line-height: 1.45;
  color: var(--itw-text-muted);
  flex: 1 1 200px;
}

.graph-roam-hint strong {
  color: var(--itw-text-secondary);
}

.selection-hint {
  font-size: 14px;
  color: var(--itw-text-secondary);
}

.selection-hint strong {
  color: var(--itw-accent);
  margin: 0 4px;
}

/* ========== 分栏容器 ========== */
.workbench-split {
  min-height: 360px;
  border: 1px solid var(--itw-border-accent);
  border-radius: 10px;
  overflow: hidden;
  background: var(--itw-bg);
  box-shadow: var(--itw-shadow);
}

.pane-inner {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--itw-pane-bg);
}

:deep(.splitpanes__pane) {
  background: var(--itw-pane-bg) !important;
}

.tree-pane,
.props-pane {
  padding: 8px 10px;
}

.props-pane {
  overflow: hidden;
}

.graph-pane {
  padding: 8px 10px;
  background: var(--itw-graph-bg);
}

/* ========== 面板标题 ========== */
.pane-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--itw-text);
  letter-spacing: 0.2px;
  margin-bottom: 6px;
  padding-bottom: 5px;
  border-bottom: 1px solid var(--itw-border);
  position: relative;
}

.pane-title::before {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 48px;
  height: 2px;
  background: linear-gradient(90deg, var(--itw-accent), transparent);
  border-radius: 1px;
}

.pane-title-icon {
  color: var(--itw-accent);
  font-size: 11px;
  margin-right: 4px;
  vertical-align: middle;
}

/* ========== 树 ========== */
.tree-scroll {
  flex: 1;
  min-height: 0;
}

.tree-scroll :deep(.el-scrollbar__view),
.tree-scroll :deep(.el-scrollbar__wrap),
.tree-scroll :deep(.el-tree) {
  background: transparent !important;
}

.indicator-tree {
  background: transparent;
  padding-right: 4px;
}

.indicator-tree :deep(.el-tree-node__content) {
  background: transparent;
  border-radius: 4px;
  transition: background 0.2s;
  min-height: 32px;
  padding: 4px 2px;
}

.indicator-tree :deep(.el-tree-node__content:hover) {
  background: var(--itw-tree-hover);
}

.indicator-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: var(--itw-tree-active);
}

.indicator-tree :deep(.el-tree-node__expand-icon) {
  color: var(--itw-text-muted);
}

.tree-node-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  padding-right: 4px;
}

.tree-node-label {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  line-height: 1.4;
  color: var(--itw-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-node-ops {
  display: flex;
  flex-shrink: 0;
  gap: 4px;
}

.tree-node-ops :deep(.el-button.is-link) {
  min-height: 26px;
  margin-left: 0;
  padding: 0 9px;
  font-size: 14px;
  border-radius: 999px;
  border: 1px solid var(--itw-btn-secondary-border);
  background: var(--itw-btn-secondary-bg);
  color: var(--itw-btn-secondary-text);
  text-decoration: none;
}

.tree-node-ops :deep(.el-button.is-link:hover),
.tree-node-ops :deep(.el-button.is-link:focus-visible) {
  background: var(--itw-btn-secondary-bg-hover);
  border-color: var(--itw-btn-secondary-border-hover);
}

.tree-node-ops :deep(.el-button.is-link.el-button--danger) {
  border-color: var(--itw-danger-border);
  background: var(--itw-danger-bg);
  color: var(--itw-danger-text);
}

.tree-node-ops :deep(.el-button.is-link.el-button--danger:hover),
.tree-node-ops :deep(.el-button.is-link.el-button--danger:focus-visible) {
  border-color: var(--itw-danger-border-hover);
  background: var(--itw-danger-bg-hover);
  color: #ffe4e6;
}

.tree-empty-hint {
  text-align: center;
  color: var(--itw-text-muted);
  padding: 28px 12px;
  font-size: 14px;
  line-height: 1.5;
}

/* ========== 图形区 ========== */
.g6-graph-host {
  flex: 1;
  min-height: 280px;
  width: 100%;
  position: relative;
  overflow: hidden;
  background: var(--itw-graph-bg);
  border-radius: 8px;
  border: 1px solid var(--itw-border);
  background-image:
    radial-gradient(circle at 20% 30%, var(--itw-glow1) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, var(--itw-glow2) 0%, transparent 50%),
    linear-gradient(var(--itw-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--itw-grid-line) 1px, transparent 1px);
  background-size: 100% 100%, 100% 100%, 32px 32px, 32px 32px;
}

.graph-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 10px 0 4px;
  border-top: 1px dashed var(--itw-border);
  margin-top: 6px;
}

.graph-actions-label {
  font-size: 13px;
  color: var(--itw-text-muted);
  margin-right: 4px;
}

/* ========== 属性面板 ========== */
.props-form {
  padding-top: 2px;
}

.props-form :deep(.el-form-item__label) {
  font-size: 13px;
  color: var(--itw-text-secondary);
}

.props-form :deep(.el-form-item) {
  margin-bottom: 14px;
}
.props-form :deep(.el-form-item__content) {
  width: calc(100% - 92px);
  flex: 1;
  min-width: 0;
}
.props-form :deep(.el-input),
.props-form :deep(.el-select),
.props-form :deep(.el-input-number) {
  width: 100% !important;
}
.props-form :deep(.el-input__wrapper),
.props-form :deep(.el-textarea__inner) {
  background: var(--itw-input-bg);
  box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.15) inset;
}
.props-form :deep(.el-input__inner) {
  background: transparent;
  color: var(--itw-input-text);
}
.props-form :deep(.el-textarea__inner) {
  background: var(--itw-input-bg);
  color: var(--itw-input-text);
  min-height: 80px;
}
.props-form :deep(.el-input-number__decrease),
.props-form :deep(.el-input-number__increase) {
  background: var(--itw-input-bg);
  border-color: var(--itw-input-border);
  color: var(--itw-text-secondary);
}
.props-form :deep(.el-input-number__decrease:hover),
.props-form :deep(.el-input-number__increase:hover) {
  color: var(--itw-accent);
}
/* 点击/聚焦时的样式 */
.props-form :deep(.el-input__wrapper.is-focus),
.props-form :deep(.el-textarea__inner:focus) {
  background: var(--itw-input-bg-focus);
  box-shadow: 0 0 0 1px var(--itw-input-border-focus) inset;
}
/* 选择器下拉菜单暗色主题 */
.props-form :deep(.el-select .el-input__wrapper) {
  background: var(--itw-input-bg);
}
/* 确保 ZhpgCalcMethodFields 内容不溢出 */
.props-form :deep(.zhpg-calc-method-fields) {
  width: 100%;
  min-width: 0;
  max-width: 100%;
}
.props-form :deep(.zhpg-calc-method-fields .calc-preview-wrap) {
  max-width: 100%;
  box-sizing: border-box;
}
.props-form :deep(.zhpg-calc-method-fields .calc-flow-preview) {
  max-width: 100%;
}
.props-form :deep(.zhpg-calc-method-fields .calc-flow-column) {
  max-width: 100%;
}
.props-scroll {
  flex: 1;
  min-height: 0;
}

.props-scroll :deep(.el-scrollbar__view) {
  min-height: 100%;
  padding-right: 4px;
}

.props-divider {
  margin: 10px 0 6px;
}
.props-divider :deep(.el-divider__text) {
  color: var(--itw-text-secondary);
  background: var(--itw-divider-bg);
}
.props-divider :deep(.el-divider) {
  border-color: var(--itw-divider-border);
}
.node-algo-trigger-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.node-algo-tag {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.node-algo-tag :deep(.el-tag__content) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
  display: inline-block;
}

/* el-tag 暗色主题 */
.props-form :deep(.el-tag) {
  background: var(--itw-tag-bg);
  border-color: var(--itw-tag-border);
  color: var(--itw-tag-text);
}
.props-form :deep(.el-tag--info) {
  background: var(--itw-tag-info-bg);
  border-color: var(--itw-tag-info-border);
  color: var(--itw-tag-info-text);
}
.props-empty-wrap {
  padding: 4px 2px 8px 0;
}

.props-empty-card {
  background: linear-gradient(165deg, rgba(14, 165, 233, 0.06), rgba(99, 102, 241, 0.04));
  border: 1px dashed var(--itw-border);
  border-radius: 8px;
  padding: 12px 12px 14px;
}

.props-empty-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--itw-text);
  margin-bottom: 8px;
}

.props-empty-lead {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
  color: var(--itw-text-secondary);
}

.props-empty-lead strong {
  color: var(--itw-text);
  font-weight: 600;
}

.props-empty-tips {
  margin: 10px 0 0;
  padding-left: 1.15em;
  font-size: 13px;
  line-height: 1.55;
  color: var(--itw-text-muted);
}

.props-empty-tips li+li {
  margin-top: 4px;
}

.threshold-range-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.threshold-range-row .threshold-num {
  flex: 1;
  min-width: 0;
}

.threshold-sep {
  flex-shrink: 0;
  color: var(--itw-text-muted);
  font-size: 14px;
}

/* ========== splitpanes 分割线 ========== */
:deep(.splitpanes__splitter) {
  background: var(--itw-splitter) !important;
  border-color: var(--itw-border) !important;
}

:deep(.splitpanes__splitter:hover) {
  background: var(--itw-splitter-hover) !important;
}

.indicator-workbench-dialog :deep(.el-dialog__body) {
  background: transparent;
}
</style>
