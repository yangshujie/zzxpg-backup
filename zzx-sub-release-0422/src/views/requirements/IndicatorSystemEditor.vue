<template>
  <div class="indicator-system-editor">
    <!-- Left: Tree Management -->
    <div class="tree-sidebar">
      <div class="sidebar-head">
        <span class="title">体系层级</span>
        <el-icon v-if="!readonly" class="op-icon" title="添加根节点" @click="addRootChild">
          <CirclePlus />
        </el-icon>
      </div>
      <div class="sidebar-content">
        <el-tree ref="treeRef" :data="treeData" node-key="id" default-expand-all :expand-on-click-node="false" :draggable="!readonly"
          @node-drop="onTreeDrop" class="indicator-tree">
          <template #default="{ node, data }">
            <span class="custom-tree-node">
              <span class="node-label" :title="data.label">{{ data.label }}</span>
              <span class="node-ops" v-if="!readonly">
                <el-icon class="op-btn add-child" title="添加子指标" @click.stop="addChildNode(data)">
                  <CirclePlus />
                </el-icon>
                <el-icon class="op-btn edit" title="编辑" @click.stop="editNode(data)">
                  <Edit />
                </el-icon>
                <el-icon class="op-btn delete" title="删除" @click.stop="removeNode(data)">
                  <Delete />
                </el-icon>
              </span>
            </span>
          </template>
        </el-tree>
      </div>
    </div>

    <!-- Right: SVG Canvas -->
    <div class="visual-canvas">
      <div class="canvas-head">
        <div class="canvas-title">可视化拓扑</div>
        <div class="canvas-tips">拖动画布平移 · Ctrl+滚轮缩放 · 拖动节点调整层级</div>
        <div class="canvas-toolbar">
          <el-button size="small" type="primary" link @click="resetView">
            <el-icon>
              <FullScreen />
            </el-icon> 适应画布
          </el-button>
        </div>
      </div>

      <div ref="svgWrapRef" class="svg-wrap" @wheel.prevent="onWheel" @mousemove="onMouseMove" @mouseup="onMouseUp"
        @mouseleave="onMouseUp" @dragover.prevent @drop="onCanvasDrop">
        <svg ref="svgRef" :width="svgW" :height="svgH" class="tree-svg" @mousedown="onPanStart">
          <defs>
            <marker id="arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
              <path d="M0,0 L0,6 L8,3 z" fill="#3d4f6b" />
            </marker>
          </defs>
          <g :transform="`translate(${pan.x}, ${pan.y}) scale(${zoom})`">
            <!-- Edges -->
            <path v-for="edge in edges" :key="edge.id" :d="edge.d" class="tree-edge" marker-end="url(#arrow)" />
            <!-- Nodes -->
            <g v-for="node in layoutNodes" :key="node.uid" :transform="`translate(${node.x}, ${node.y})`"
              class="tree-node-group"
              :class="{ 'is-root': node.depth === 0, 'is-dragging': dragNodeId === node.id, 'is-drop-target': dropTargetId === node.id }"
              @mousedown.stop="onNodeDragStart($event, node)">
              <!-- Node body -->
              <rect :x="-nodeW / 2" :y="-nodeH / 2" :width="nodeW" :height="nodeH" :rx="6"
                :class="['node-rect', `depth-${Math.min(node.depth, 3)}`]" />
              <!-- Label -->
              <foreignObject :x="-nodeW / 2 + 10" :y="-nodeH / 2" :width="nodeW - 20" :height="nodeH">
                <div class="node-label-wrap">
                  <el-tooltip :content="node.label" placement="top" :show-after="500">
                    <div class="node-label-text">{{ node.label }}</div>
                  </el-tooltip>
                </div>
              </foreignObject>
              <!-- Threshold badge -->
              <text v-if="node.threshold" class="node-threshold" :y="nodeH / 2 - 6" text-anchor="middle">{{
                node.threshold
              }}</text>
              <!-- Action buttons (show on hover) -->
              <g class="node-actions" v-if="!readonly">
                <g class="node-action-btn add-btn" :transform="`translate(${nodeW / 2 - 10}, ${-nodeH / 2 - 8})`"
                  @click.stop="addChildNode(findNodeById(treeData, node.id))" title="添加子指标">
                  <circle r="8" class="action-circle add-circle" />
                  <text dy="0.35em" text-anchor="middle" class="action-icon">+</text>
                </g>
                <g class="node-action-btn edit-btn" :transform="`translate(${nodeW / 2 + 10}, ${-nodeH / 2 - 8})`"
                  @click.stop="editNode(findNodeById(treeData, node.id))" title="编辑">
                  <circle r="8" class="action-circle edit-circle" />
                  <text dy="0.35em" text-anchor="middle" class="action-icon">✎</text>
                </g>
                <g v-if="node.depth > 0" class="node-action-btn del-btn"
                  :transform="`translate(${nodeW / 2 + 29}, ${-nodeH / 2 - 8})`"
                  @click.stop="removeNode(findNodeById(treeData, node.id))" title="删除">
                  <circle r="8" class="action-circle del-circle" />
                  <text dy="0.35em" text-anchor="middle" class="action-icon">✕</text>
                </g>
              </g>
            </g>
            <!-- Ghost node during drag -->
            <g v-if="dragNodeId && ghostPos" :transform="`translate(${ghostPos.x}, ${ghostPos.y})`"
              style="pointer-events:none">
              <rect :x="-nodeW / 2" :y="-nodeH / 2" :width="nodeW" :height="nodeH" :rx="6" fill="rgba(88,166,255,0.15)"
                stroke="#58a6ff" stroke-width="2" stroke-dasharray="5 3" />
              <text fill="#58a6ff" font-size="11" dy="0.35em" text-anchor="middle">{{ dragNodeLabel }}</text>
            </g>
          </g>
        </svg>

        <!-- Drop hint -->
        <div v-if="isDraggingLib" class="drop-hint">
          <el-icon>
            <Download />
          </el-icon>
          释放以添加指标到体系根节点
        </div>
      </div>
    </div>

    <!-- Indicator Selection Dialog -->
    <el-dialog v-model="selectionVisible" title="添加指标项" width="600px" append-to-body class="cyber-dialog">
      <div class="selection-tools">
        <el-input v-model="selectSearch" placeholder="搜索指标库..." clearable size="small" style="width: 240px">
          <template #prefix><el-icon>
              <Search />
            </el-icon></template>
        </el-input>
        <el-button type="primary" size="small" @click="createBlankNode">
          <el-icon>
            <EditPen />
          </el-icon> 创建空白项
        </el-button>
      </div>
      <div class="selection-list-box">
        <el-table :data="filteredSelectLib" size="small" style="width: 100%" height="400px">
          <el-table-column label="指标名称" prop="indicatorName" min-width="150" />
          <el-table-column label="隶属分系统" prop="systemName" width="120" />
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="confirmSelect(row)">选择</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <el-dialog v-model="editorVisible" :title="editorTitle" width="400px" append-to-body class="cyber-dialog">
      <el-form :model="editingNode" label-width="80px" size="small">
        <el-form-item label="指标名称">
          <el-input v-model="editingNode.label" />
        </el-form-item>
        <el-form-item label="阈值要求">
          <el-input v-model="editingNode.threshold" placeholder="例：≤ 0.5°" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="editorVisible = false">取消</el-button>
        <el-button size="small" type="primary" @click="saveNodeEdit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Right: Indicator Library (Now on the far right) -->
    <div class="library-sidebar" v-if="!readonly" :class="{ collapsed: libCollapsed }">
      <div class="sidebar-head">
        <span class="title" v-if="!libCollapsed">指标库</span>
        <el-icon class="collapse-btn" @click="$emit('update:libCollapsed', !libCollapsed)">
          <Fold v-if="libCollapsed" />
          <Expand v-else />
        </el-icon>
      </div>
      <div class="sidebar-content" v-if="!libCollapsed">
        <el-input v-model="libSearch" placeholder="搜索指标库..." size="small" class="sidebar-search" clearable>
          <template #prefix><el-icon>
              <Search />
            </el-icon></template>
        </el-input>
        <div class="lib-list">
          <div v-for="item in filteredLib" :key="item.indicatorId" class="lib-item" draggable="true"
            @dragstart="handleLibDragStart($event, item)">
            <div class="lib-item-icon"><el-icon>
                <Files />
              </el-icon></div>
            <div class="lib-item-info">
              <div class="name">{{ item.indicatorName }}</div>
              <div class="system">{{ item.systemName }}</div>
            </div>
            <div class="lib-item-add" title="添加到体系" @click="addFromLib(item)">
              <el-icon>
                <Plus />
              </el-icon>
            </div>
          </div>
        </div>
        <div class="custom-add-btn" @click="addCustomIndicator">
          <el-icon>
            <EditPen />
          </el-icon> 自定义指标
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import {
  Expand, Fold, Search, Files, Plus, EditPen,
  Delete, Edit, FullScreen, CirclePlus, Download
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import indicatorLibData from '@/mock/indicator.json'

// ─── Props ───
const props = defineProps({
  treeData: { type: Array, default: () => [] },
  libCollapsed: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false }
})

let nextId = Date.now()
const generateUniqueId = () => nextId++

const emit = defineEmits(['update:libCollapsed', 'update:tree-data'])

// ─── State: UI Control ───
const selectionVisible = ref(false)
const selectSearch = ref('')
let parentForNewChild = null

// ─── Constants ───
const nodeW = 140
const nodeH = 44
const H_GAP = 60   // horizontal gap between levels
const V_GAP = 30   // vertical gap between siblings

// ─── State: Library ───
const libSearch = ref('')
const isDraggingLib = ref(false)

const filteredLib = computed(() => {
  if (!libSearch.value) return indicatorLibData
  return indicatorLibData.filter(i =>
    i.indicatorName.includes(libSearch.value) ||
    i.systemName?.includes(libSearch.value)
  )
})

const filteredSelectLib = computed(() => {
  if (!selectSearch.value) return indicatorLibData
  return indicatorLibData.filter(i =>
    i.indicatorName.includes(selectSearch.value) ||
    i.systemName?.includes(selectSearch.value)
  )
})

// ─── State: Tree data ───
const treeData = ref([])
const treeRef = ref(null)

// ─── State: Canvas ───
const svgWrapRef = ref(null)
const svgRef = ref(null)
const svgW = ref(800)
const svgH = ref(600)
const zoom = ref(1)
const pan = ref({ x: 60, y: 60 })

// ─── State: Panning ───
let panning = false
let panStart = { x: 0, y: 0 }
let panOrigin = { x: 0, y: 0 }

// ─── State: Node dragging ───
const dragNodeId = ref(null)
const dropTargetId = ref(null)
const ghostPos = ref(null)   // SVG-space position of ghost
const dragNodeLabel = ref('')
let draggingInSvg = false

// ─── State: Editor ───
const editorVisible = ref(false)
const editorTitle = ref('编辑指标')
const editingNode = ref({ id: '', label: '', threshold: '' })
let editingTarget = null

// ═══════════════════════════════════════════
//  LAYOUT: Reingold-Tilford style tree layout
// ═══════════════════════════════════════════

const layoutNodes = ref([])
const edges = ref([])

// 1. Compute subtree height for each node
const getHeight = (node) => {
  if (!node.children || node.children.length === 0) return 1
  return node.children.reduce((s, c) => s + getHeight(c), 0)
}

const buildLayout = () => {
  console.log('Building layout with treeData:', treeData.value)
  if (!treeData.value || !treeData.value.length) {
    layoutNodes.value = []
    edges.value = []
    return
  }
  const root = treeData.value[0]
  if (!root) return

  const nodes = []
  const edgeList = []
  let internalUid = 0

  const traverse = (node, depth, startY, parentLayoutNode = null) => {
    if (!node) return null

    const hCount = getHeight(node)
    const totalSubtreeH = hCount * nodeH + (hCount - 1) * V_GAP

    const x = depth * (nodeW + H_GAP) + nodeW / 2
    const y = startY + totalSubtreeH / 2 - nodeH / 2

    const currentLayoutNode = {
      uid: internalUid++,
      id: node.id,
      label: node.label || '未命名',
      threshold: node.threshold || '',
      depth,
      x,
      y
    }
    nodes.push(currentLayoutNode)

    if (parentLayoutNode) {
      const x1 = parentLayoutNode.x + nodeW / 2
      const y1 = parentLayoutNode.y
      const x2 = currentLayoutNode.x - nodeW / 2
      const y2 = currentLayoutNode.y
      const mx = (x1 + x2) / 2
      edgeList.push({
        id: `edge-${parentLayoutNode.uid}-${currentLayoutNode.uid}`,
        d: `M ${x1} ${y1} C ${mx} ${y1}, ${mx} ${y2}, ${x2} ${y2}`
      })
    }

    if (node.children && Array.isArray(node.children)) {
      let childY = startY
      for (const child of node.children) {
        if (!child) continue
        traverse(child, depth + 1, childY, currentLayoutNode)
        childY += getHeight(child) * (nodeH + V_GAP)
      }
    }
    return currentLayoutNode
  }

  try {
    traverse(root, 0, 0)
    layoutNodes.value = nodes
    edges.value = edgeList
    console.log('Layout generated:', nodes.length, 'nodes', edgeList.length, 'edges')
  } catch (err) {
    console.error('Layout algorithm error:', err)
  }

  nextTick(() => {
    if (svgWrapRef.value && nodes.length > 0) {
      const maxX = nodes.reduce((max, n) => Math.max(max, n.x + nodeW), 0)
      const maxY = nodes.reduce((max, n) => Math.max(max, n.y + nodeH), 0)
      svgW.value = Math.max(svgWrapRef.value.clientWidth, maxX + 200)
      svgH.value = Math.max(svgWrapRef.value.clientHeight, maxY + 200)
    }
  })
}

// ─── Watch tree data ───
watch(treeData, buildLayout, { deep: true })

// ─── Helper ───
const findNodeById = (arr, id) => {
  for (const item of arr) {
    if (item.id === id) return item
    if (item.children) {
      const found = findNodeById(item.children, id)
      if (found) return found
    }
  }
  return null
}

const findParent = (arr, id, parent = null) => {
  for (const item of arr) {
    if (item.id === id) return parent
    if (item.children) {
      const found = findParent(item.children, id, item)
      if (found !== undefined) return found
    }
  }
  return undefined
}

const removeNodeById = (arr, id) => {
  for (let i = 0; i < arr.length; i++) {
    if (arr[i].id === id) { arr.splice(i, 1); return true }
    if (arr[i].children && removeNodeById(arr[i].children, id)) return true
  }
  return false
}

// ─── Lifecycle & Sync ───
onMounted(() => {
  if (props.treeData?.length) {
    treeData.value = JSON.parse(JSON.stringify(props.treeData))
  } else {
    treeData.value = [{
      id: generateUniqueId(),
      label: '指标体系根节点',
      children: []
    }]
  }
  nextTick(buildLayout)
  window.addEventListener('resize', onResize)
})

watch(() => props.treeData, (newVal) => {
  // Sync from parent if changed externally (e.g. "Create Manually" reset)
  if (JSON.stringify(newVal) !== JSON.stringify(treeData.value)) {
    treeData.value = JSON.parse(JSON.stringify(newVal))
    nextTick(buildLayout)
  }
}, { deep: true })

watch(treeData, (newVal) => {
  // Sync to parent
  emit('update:tree-data', JSON.parse(JSON.stringify(newVal)))
}, { deep: true })

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
})

const onResize = () => {
  if (svgWrapRef.value) {
    svgW.value = svgWrapRef.value.clientWidth
    svgH.value = svgWrapRef.value.clientHeight
  }
}

// ─── View controls ───
const onWheel = (e) => {
  const delta = e.deltaY > 0 ? 0.9 : 1.1
  if (!e.ctrlKey && !e.metaKey) {
    // Pan vertically without Ctrl
    pan.value = { x: pan.value.x, y: pan.value.y - e.deltaY }
    return
  }
  const newZoom = Math.min(2, Math.max(0.2, zoom.value * delta))
  // Zoom toward cursor
  const rect = svgWrapRef.value.getBoundingClientRect()
  const cx = e.clientX - rect.left
  const cy = e.clientY - rect.top
  pan.value = {
    x: cx - (cx - pan.value.x) * (newZoom / zoom.value),
    y: cy - (cy - pan.value.y) * (newZoom / zoom.value)
  }
  zoom.value = newZoom
}

const onPanStart = (e) => {
  panning = true
  panStart = { x: e.clientX, y: e.clientY }
  panOrigin = { ...pan.value }
}

const onMouseMove = (e) => {
  if (panning) {
    pan.value = {
      x: panOrigin.x + (e.clientX - panStart.x),
      y: panOrigin.y + (e.clientY - panStart.y)
    }
    return
  }

  if (draggingInSvg && dragNodeId.value) {
    const rect = svgWrapRef.value.getBoundingClientRect()
    // Convert mouse to SVG space
    const svgX = (e.clientX - rect.left - pan.value.x) / zoom.value
    const svgY = (e.clientY - rect.top - pan.value.y) / zoom.value
    ghostPos.value = { x: svgX, y: svgY }

    // Find closest node to snap/highlight as drop target
    const hit = layoutNodes.value.find(n =>
      n.id !== dragNodeId.value &&
      Math.abs(n.x - svgX) < nodeW / 2 + 16 &&
      Math.abs(n.y - svgY) < nodeH / 2 + 16
    )
    dropTargetId.value = hit ? hit.id : null
  }
}

const onMouseUp = (e) => {
  if (panning) { panning = false; return }

  if (draggingInSvg && dragNodeId.value) {
    if (dropTargetId.value) {
      const dragId = dragNodeId.value
      const dropId = dropTargetId.value
      const isDescendant = (parentId, seekId) => {
        const n = findNodeById(treeData.value, parentId)
        if (!n || !n.children) return false
        return n.children.some(c => c.id === seekId || isDescendant(c.id, seekId))
      }
      if (!isDescendant(dragId, dropId) && dragId !== dropId) {
        const nodeToMove = JSON.parse(JSON.stringify(findNodeById(treeData.value, dragId)))
        removeNodeById(treeData.value, dragId)
        const newParent = findNodeById(treeData.value, dropId)
        if (newParent) {
          if (!newParent.children) newParent.children = []
          newParent.children.push(nodeToMove)
          ElMessage.success(`已将 "${nodeToMove.label}" 追加到 "${newParent.label}"`)
        }
      }
    }
    dragNodeId.value = null
    dropTargetId.value = null
    ghostPos.value = null
    draggingInSvg = false
  }
}

const onNodeDragStart = (e, node) => {
  if (node.depth === 0) return // root 不可拖
  dragNodeId.value = node.id
  dragNodeLabel.value = node.label
  draggingInSvg = true
  // Set initial ghost position to node's current SVG position
  ghostPos.value = { x: node.x, y: node.y }
  e.preventDefault()
}

const resetView = () => {
  if (!layoutNodes.value.length) return
  const xs = layoutNodes.value.map(n => n.x)
  const ys = layoutNodes.value.map(n => n.y)
  const minX = Math.min(...xs) - nodeW / 2
  const maxX = Math.max(...xs) + nodeW / 2
  const minY = Math.min(...ys) - nodeH
  const maxY = Math.max(...ys) + nodeH
  const treeW = maxX - minX
  const treeH = maxY - minY
  const wrapW = svgWrapRef.value?.clientWidth || 800
  const wrapH = svgWrapRef.value?.clientHeight || 600
  const z = Math.min(0.98, Math.min(wrapW / (treeW + 80), wrapH / (treeH + 80)))
  zoom.value = z
  pan.value = {
    x: (wrapW - treeW * z) / 2 - minX * z,
    y: (wrapH - treeH * z) / 2 - minY * z
  }
}

// ─── Tree CRUD ───
const addChildNode = (parentData) => {
  parentForNewChild = parentData
  selectionVisible.value = true
}

const createBlankNode = () => {
  const newNode = {
    id: generateUniqueId(),
    label: '新指标',
    threshold: '',
    children: []
  }
  if (!parentForNewChild.children) parentForNewChild.children = []
  parentForNewChild.children.push(newNode)
  selectionVisible.value = false
  editNode(newNode)
}

const confirmSelect = (libItem) => {
  let parsed = { label: libItem.indicatorName, children: [] }
  try {
    if (libItem.nodeTree) {
      const tree = JSON.parse(libItem.nodeTree)
      parsed = injectNodeIds(tree)
    }
  } catch (e) { console.error('Parse lib error', e) }

  if (!parentForNewChild.children) parentForNewChild.children = []
  parentForNewChild.children.push(parsed)
  selectionVisible.value = false
  nextTick(buildLayout)
}

const injectNodeIds = (node) => {
  node.id = generateUniqueId()
  if (node.children) {
    node.children.forEach(c => injectNodeIds(c))
  }
  return node
}

const addRootChild = () => {
  addChildNode(treeData.value[0])
}

const removeNode = (data) => {
  if (!data) return
  if (data.id === treeData.value[0]?.id) {
    ElMessage.warning('根节点不可删除')
    return
  }
  ElMessageBox.confirm(`确定要移除 "${data.label}" 及其所有子节点吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(() => {
    removeNodeById(treeData.value, data.id)
  }).catch(() => { })
}

const editNode = (data) => {
  if (!data) return
  editingTarget = data
  editingNode.value = { id: data.id, label: data.label || '', threshold: data.threshold || '' }
  editorTitle.value = data.label ? `编辑：${data.label}` : '新增指标'
  editorVisible.value = true
}

const saveNodeEdit = () => {
  if (editingTarget) {
    editingTarget.label = editingNode.value.label
    editingTarget.threshold = editingNode.value.threshold
    // trigger watch
    treeData.value = [...treeData.value]
    editorVisible.value = false
  }
}

const onTreeDrop = () => {
  // tree drag-drop already updates treeData via el-tree, just re-layout
  nextTick(buildLayout)
}

// ─── Library ───
const handleLibDragStart = (e, item) => {
  e.dataTransfer.setData('text/indicator', JSON.stringify(item))
  isDraggingLib.value = true
}

const onCanvasDrop = (e) => {
  isDraggingLib.value = false
  const dataStr = e.dataTransfer.getData('text/indicator')
  if (dataStr) {
    const libItem = JSON.parse(dataStr)
    addFromLib(libItem)
  }
}

const addFromLib = (libItem) => {
  let parsed = {}
  try { parsed = JSON.parse(libItem.nodeTree) } catch { }

  const newNode = {
    id: generateUniqueId(),
    label: libItem.indicatorName,
    threshold: parsed.threshold || '',
    children: (parsed.children || []).map(c => mapLibTree(c))
  }
  if (!treeData.value[0].children) treeData.value[0].children = []
  treeData.value[0].children.push(newNode)
  ElMessage.success(`已添加指标：${libItem.indicatorName}`)
}

const mapLibTree = (node) => ({
  id: generateUniqueId(),
  label: node.label || '',
  threshold: node.threshold || '',
  children: (node.children || []).map(c => mapLibTree(c))
})

const addCustomIndicator = () => {
  addChildNode(treeData.value[0])
}
</script>

<style scoped lang="scss">
.indicator-system-editor {
  display: flex;
  height: 100%;
  width: 100%;
  background: var(--bg-color);
  overflow: hidden;

  .sidebar-head {
    height: 40px;
    padding: 0 12px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid var(--border-color);
    background: var(--sider-bg-color);
    flex-shrink: 0;

    .title {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-color-secondary);
      letter-spacing: 1px;
    }

    .collapse-btn,
    .op-icon {
      cursor: pointer;
      color: var(--text-color-secondary);

      &:hover {
        color: var(--primary-color);
      }
    }
  }

  .sidebar-content {
    flex: 1;
    overflow-y: auto;
    padding: 10px;

    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: var(--border-color);
      border-radius: 2px;
    }
  }

  .selection-tools {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    gap: 12px;
  }

  .selection-list-box {
    border: 1px solid var(--border-color);
    border-radius: 4px;
    background: var(--bg-color);
    overflow: hidden;

    :deep(.el-table) {
      background: transparent;
      --el-table-bg-color: transparent;
      --el-table-tr-bg-color: transparent;
      --el-table-header-bg-color: var(--sider-bg-color);
      --el-table-border-color: var(--border-color);
      color: var(--text-color-primary);

      th.el-table__cell {
        background: var(--sider-bg-color) !important;
        color: var(--text-color-secondary);
        font-weight: 600;
        border-bottom: 1px solid var(--border-color) !important;
      }

      td.el-table__cell {
        border-bottom: 1px solid var(--border-color) !important;
      }
    }
  }

  /* Library */
  .library-sidebar {
    width: 230px;
    border-left: 1px solid var(--border-color);
    display: flex;
    flex-direction: column;
    flex-shrink: 0;
    background: var(--bg-color);
    transition: width 0.25s;

    &.collapsed {
      width: 40px;
    }

    .sidebar-search {
      margin-bottom: 10px;
    }

    .lib-list {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .lib-item {
      padding: 7px 8px;
      background: var(--card-bg-color);
      border: 1px solid var(--border-color);
      border-radius: 4px;
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: grab;

      &:hover {
        border-color: var(--primary-color);
        // background: rgba(var(--primary-color) r g b / 0.05); // Fallback or use specific var if available
        background: rgba(var(--primary-color-rgb), 0.08); // Fallback or use specific var if available
        

        .lib-item-add {
          opacity: 1;
        }
      }
      .lib-item-icon {
        color: var(--primary-color);
        flex-shrink: 0;
      }

      .lib-item-info {
        flex: 1;
        overflow: hidden;

        .name {
          font-size: 12px;
          color: var(--text-color-primary);
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .system {
          font-size: 10px;
          color: var(--text-color-secondary);
        }
      }

      .lib-item-add {
        opacity: 0;
        cursor: pointer;
        color: var(--success-color);
        transition: opacity 0.2s;

        &:hover {
          transform: scale(1.2);
        }
      }
    }

    .custom-add-btn {
      margin-top: 12px;
      padding: 7px;
      border: 1px dashed var(--border-color);
      border-radius: 4px;
      text-align: center;
      font-size: 12px;
      color: var(--text-color-secondary);
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 5px;

      &:hover {
        color: var(--primary-color);
        border-color: var(--primary-color);
        background:rgba(var(--primary-color-rgb), 0.05);
        // background: rgba(var(--primary-color) r g b / 0.05);
      }
    }
  }

  /* Tree sidebar */
  .tree-sidebar {
    width: 200px;
    border-right: 1px solid var(--border-color);
    display: flex;
    flex-direction: column;
    background: var(--sider-bg-color);
    flex-shrink: 0;
  }

  .indicator-tree {
    background: transparent;
    color: var(--text-color-primary);

    :deep(.el-tree-node__content) {
      height: 30px;

      &:hover {
        // background: rgba(var(--primary-color) r g b / 0.1);
        background: rgba(var(--primary-color-rgb), 0.1);
      }
    }

    :deep(.el-tree-node.is-current > .el-tree-node__content) {
      background: rgba(var(--primary-color), transparent 85%);
      // background: rgba(var(--primary-color) r g b / 0.15);
      border-right: 2px solid var(--primary-color);
    }
  }

  .custom-tree-node {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12px;
    padding-right: 6px;
    overflow: hidden;
    min-width: 0;

    .node-label {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      min-width: 0;
    }

    .node-ops {
      display: none;
      align-items: center;
      gap: 4px;
      flex-shrink: 0;

      .op-btn {
        font-size: 13px;
        cursor: pointer;

        &.add-child { color: var(--primary-color); }
        &.edit { color: var(--success-color); }
        &.delete { color: var(--danger-color); }

        &:hover {
          transform: scale(1.2);
        }
      }
    }

    &:hover .node-ops {
      display: flex;
    }
  }

  /* Canvas */
  .visual-canvas {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .canvas-head {
      height: 40px;
      padding: 0 16px;
      display: flex;
      align-items: center;
      gap: 12px;
      background: var(--bg-color);
      border-bottom: 1px solid var(--border-color);
      flex-shrink: 0;

      .canvas-title {
        color: var(--text-color-secondary);
      }

      .canvas-tips {
        color: var(--text-color-placeholder);
      }
    }

    .svg-wrap {
      flex: 1;
      overflow: hidden;
      position: relative;
      cursor: v-bind("dragNodeId ? 'grabbing' : 'default'");
      background-image: radial-gradient(var(--border-color) 1px, transparent 1px);
      background-size: 22px 22px;

      .tree-svg {
        display: block;
        width: 100%;
        height: 100%;
      }

      .drop-hint {
        position: absolute;
        bottom: 20px;
        left: 50%;
        transform: translateX(-50%);
        background: rgba(var(--primary-color-rgb), 0.15);
        // background: rgba(var(--primary-color) r g b / 0.15);
        border: 1px dashed var(--primary-color);
        color: var(--primary-color);
        padding: 8px 20px;
        border-radius: 20px;
        font-size: 13px;
        display: flex;
        align-items: center;
        gap: 8px;
        pointer-events: none;
      }
    }
  }
}

/* SVG node styles */
:deep(svg) {
  .tree-edge {
    fill: none;
    stroke: var(--border-color-hover);
    stroke-width: 1.5px;
  }

  .tree-node-group {
    cursor: pointer;

    &.is-root .node-rect {
      stroke: var(--primary-color);
    }

    &.is-dragging {
      opacity: 0.5;
    }

    &.is-drop-target .node-rect {
      stroke: var(--success-color);
      stroke-width: 2px;
      filter: drop-shadow(0 0 6px var(--success-color));
    }

    .node-rect {
      fill: var(--card-bg-color);
      stroke: var(--border-color);
      stroke-width: 1.5px;
      transition: stroke 0.15s;

      &.depth-0 {
        fill: rgba(var(--primary-color-rgb),0.12);
        // fill: rgba(var(--primary-color) r g b / 0.12);
        stroke: var(--primary-color);
      }

      &.depth-1 {
        fill: rgba(var(--info-color-rgb), 0.08);
        // fill: rgba(var(--info-color) r g b / 0.08);
        stroke: var(--info-color);
      }

      &.depth-2 {
        fill: rgba(var(--success-color-rgb), 0.08);
        // fill: rgba(var(--success-color) r g b / 0.08);
        stroke: var(--success-color);
      }

      &.depth-3 {
        fill: rgba(var(--warning-color-rgb), 0.08);
        stroke: var(--warning-color);
      }
    }

    .node-label-wrap {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      box-sizing: border-box;
      pointer-events: none;
    }

    .node-label-text {
      width: 100%;
      color: var(--text-color-primary);
      font-size: 11px;
      font-weight: 500;
      text-align: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      pointer-events: auto;
      user-select: none;
      line-height: normal;
    }

    .node-text {
      display: none;
    }

    .node-threshold {
      fill: var(--text-color-secondary);
      font-size: 9px;
      font-family: monospace;
      pointer-events: none;
      user-select: none;
    }

    .node-actions {
      opacity: 0;
      transition: opacity 0.15s;
      pointer-events: all;
    }

    &:hover .node-actions {
      opacity: 1;
    }

    .action-circle {
      stroke-width: 1px;
      cursor: pointer;

      &.add-circle {
        // fill: rgba(var(--primary-color) r g b / 0.2);
        fill: rgba(var(--primary-color-rgb), 0.2);
        stroke: var(--primary-color);
      }

      &.edit-circle {
        fill: rgba(var(--success-color-rgb), 0.2);
        // fill: rgba(var(--success-color) r g b / 0.2);
        stroke: var(--success-color);
      }

      &.del-circle {
        fill: rgba(var(--danger-color-rgb), 0.2);
        // fill: rgba(var(--danger-color) r g b / 0.2);
        stroke: var(--danger-color);
      }

      &:hover {
        opacity: 0.8;
      }
    }

    .action-icon {
      fill: var(--text-color-primary);
      font-size: 9px;
      pointer-events: none;
      user-select: none;
    }
  }
}

// Dialog
.cyber-dialog {
  :deep(.el-dialog) {
    background: var(--bg-color-overlay);
    backdrop-filter: var(--backdrop-filter);
    border: 1px solid var(--border-color);

    .el-dialog__title {
      color: var(--text-color-primary);
    }

    .el-form-item__label {
      color: var(--text-color-secondary);
    }

    .el-input__wrapper {
      background: var(--bg-color);
      box-shadow: 0 0 0 1px var(--border-color);
    }

    .el-input__inner {
      color: var(--text-color-primary);
    }
  }
}
</style>
