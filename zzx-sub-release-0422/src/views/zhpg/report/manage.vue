<script setup name="ReportManage">
import { reactive, ref, computed, nextTick, watch } from 'vue'
import Draggable from 'vuedraggable'
import {
  createTemplate,
  deleteTemplate,
  getTemplate,
  listTemplates,
  updateTemplate,
  updateTemplateContent,
} from '@/api/zhpg/report'
import {
  FIXED_REPORT_PLACEHOLDERS,
  VARIABLE_PATTERN,
  extractPlaceholdersFromHtml,
  getPlaceholderMeta,
} from '@/utils/zhpg/reportPlaceholders'

const { proxy } = getCurrentInstance()

// 评估任务类型选项
const taskTypeOptions = [
  { label: '性能指标评估', value: 'PERFORMANCE' },
  { label: '装备作战效能评估', value: 'EQUIP_EFFECTIVENESS' },
  { label: '体系贡献率评估', value: 'SYSTEM_CONTRIBUTION' },
  { label: '作战任务满足度评估', value: 'TASK_SATISFACTION' },
  { label: '体系级任务评估', value: 'SYSTEM_TASK' },
  { label: '演习演训任务评估', value: 'EXERCISE_TRAINING' },
]

// 获取任务类型标签
function getTaskTypeLabel(value) {
  const item = taskTypeOptions.find(o => o.value === value)
  return item ? item.label : value || '—'
}

const componentDefs = [
  { type: 'title', label: '主标题', hint: '封面/章节标题', html: '<h1 style="text-align:center;">{{ReportTitle}}</h1>' },
  { type: 'subtitle', label: '副标题', hint: '标题补充信息', html: '<h2 style="text-align:center;">{{SubTitle}}</h2>' },
  { type: 'h1', label: '一级标题', hint: '章节标题', html: '<h2>{{SectionOne}}</h2>' },
  { type: 'h2', label: '二级标题', hint: '小节标题', html: '<h3>{{SectionTwo}}</h3>' },
  { type: 'h3', label: '三级标题', hint: '细分标题', html: '<h4>{{SectionThree}}</h4>' },
  { type: 'paragraph', label: '正文', hint: '普通文本段落', html: '<p>{{Content}}</p>' },
  { type: 'quote', label: '引用块', hint: '结论/引用', html: '<blockquote style="padding:8px 12px;border-left:4px solid #5b8def;background:#f8fbff;">{{QuoteText}}</blockquote>' },
  { type: 'dataTable', label: '数据表格', hint: '匹配指标汇总等结果表', tableType: 'indicator_summary_table' },
  { type: 'imageRadar', label: '能力雷达图', hint: '匹配计算生成图', imageType: 'capability_radar_chart', imageWidth: 80, imageHeight: 160, imageAlign: 'center' },
  { type: 'imageCustom', label: '自定义图片', hint: '匹配图片地址', imageType: 'custom_image', imageWidth: 80, imageHeight: 160, imageAlign: 'center' },
  { type: 'indicatorSections', label: '指标章节循环块', hint: '指标层级章节骨架', atomic: true, html: '{{#each IndicatorSections}}<section><h{{=item.level}}>{{item.numbering}} {{item.title}}</h{{=item.level}}>{{#if item.summary}}<p>{{item.summary}}</p>{{/if}}{{#if item.evalTable}}<div>{{item.evalTable}}</div>{{/if}}{{#if item.chartImg}}<p style="text-align:center;"><img src="{{=item.chartImg}}" style="max-width:80%;" alt="{{item.title}}图表" /></p>{{/if}}</section>{{/each}}' },
]

const dataTableTypeOptions = [
  { label: '指标汇总表', value: 'indicator_summary_table', placeholder: 'CapabilityScoreTable' },
  { label: '指标树层级表', value: 'indicator_tree', placeholder: 'IndicatorTree' },
]

const imageTypeOptions = [
  { label: '能力雷达图', value: 'capability_radar_chart', placeholder: 'CapabilityRadarChart' },
  { label: '自定义图片地址', value: 'custom_image', placeholder: 'ImageUrl' },
]

const imageAlignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '居中', value: 'center' },
  { label: '右对齐', value: 'right' },
]

// ========== 状态 ==========
const loading = ref(false)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)

const templateList = ref([])
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  templateName: '',
})

const form = reactive({
  id: undefined,
  templateName: '',
  templateDescription: '',
  evaluationType: '',
  htmlContent: '',
})

// 对话框状态
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)

// 构建器状态
const canvasRef = ref(null)
const blocks = ref([])
const selectedBlock = ref(null)
const styleForm = reactive({
  textAlign: 'left',
  fontSize: 16,
  color: '#d8e7f5',
})
const selectedBlockIndex = computed(() => blocks.value.findIndex(item => item.id === selectedBlock.value?.id))
const hasSelectedBlock = computed(() => selectedBlockIndex.value > -1)
const isSelectedDataTable = computed(() => selectedBlock.value?.type === 'dataTable')
const isSelectedImage = computed(() => ['imageRadar', 'imageCustom'].includes(selectedBlock.value?.type))

// 监听对话框打开，加载画布内容
watch(dialogVisible, async (val) => {
  if (val) {
    await nextTick()
    loadBlocksFromHtml(form.htmlContent || '')
    clearSelection()
  }
})

// ========== 列表相关 ==========
async function getList() {
  loading.value = true
  try {
    const res = await listTemplates()
    templateList.value = res.data || []
    total.value = res.data?.length || 0
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.templateName = ''
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

// ========== CRUD ==========
function handleAdd() {
  resetForm()
  dialogTitle.value = '新增报告模板'
  isEdit.value = false
  dialogVisible.value = true
}

async function handleUpdate(row) {
  loading.value = true
  try {
    const res = await getTemplate(row.id)
    const data = res.data
    Object.assign(form, data)
    dialogTitle.value = '编辑报告模板'
    isEdit.value = true
    dialogVisible.value = true
  } finally {
    loading.value = false
  }
}

async function handleDelete(row) {
  const message = row.id ? `确认删除模板"${row.templateName}"吗？` : `确认删除选中的 ${ids.value.length} 个模板吗？`
  proxy.$modal.confirm(message).then(async () => {
    await deleteTemplate(row.id || ids.value.join(','))
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

async function submitForm() {
  if (!form.templateName?.trim()) {
    proxy.$modal.msgError('模板名称不能为空')
    return
  }
  if (!form.evaluationType) {
    proxy.$modal.msgError('请选择评估任务类型')
    return
  }
  loading.value = true
  try {
    normalizeCanvasPlaceholders()
    form.htmlContent = toHtmlContent()
    if (isEdit.value) {
      await updateTemplate(form.id, {
        templateName: form.templateName,
        templateDescription: form.templateDescription,
        evaluationType: form.evaluationType,
      })
      // 更新模板内容
      await updateTemplateContent(form.id, form.htmlContent)
      proxy.$modal.msgSuccess('修改成功')
    } else {
      await createTemplate({
        templateName: form.templateName,
        templateDescription: form.templateDescription,
        evaluationType: form.evaluationType,
        htmlContent: form.htmlContent,
        changeLog: '创建模板',
      })
      proxy.$modal.msgSuccess('新增成功')
    }
    dialogVisible.value = false
    getList()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    templateName: '',
    templateDescription: '',
    evaluationType: '',
    htmlContent: '',
  })
  blocks.value = []
  clearSelection()
}

// ========== 画布相关函数 ==========
function nextBlockId() {
  return `block-${Date.now()}-${Math.floor(Math.random() * 10000)}`
}

function buildDataTableHtml(tableType = 'indicator_summary_table') {
  const option = dataTableTypeOptions.find(item => item.value === tableType) || dataTableTypeOptions[0]
  return `<div class="report-data-table-placeholder">{{${option.placeholder}}}</div>`
}

function buildImageHtml(imageType = 'capability_radar_chart', imageWidth = 80, imageHeight = 120, imageAlign = 'center') {
  const option = imageTypeOptions.find(item => item.value === imageType) || imageTypeOptions[0]
  const alt = option.label.replace('地址', '')
  const width = Math.max(10, Math.min(Number(imageWidth) || 80, 100))
  const height = Math.max(60, Math.min(Number(imageHeight) || 120, 800))
  return `<p style="text-align:${imageAlign};"><img src="{{${option.placeholder}}}" style="width:${width}%;max-width:100%;height:${height}px;object-fit:contain;" alt="${alt}" /></p>`
}

function extractBodyContent(html) {
  const match = (html || '').match(/<body[^>]*>([\s\S]*)<\/body>/i)
  return match ? match[1] : html
}

function toHtmlContent() {
  return `<html><body>${blocks.value.map(block => block.html || '').join('')}</body></html>`
}

function createBlock({ type, label, html, atomic = false, locked = false, props = {} }) {
  return {
    id: nextBlockId(),
    type,
    label,
    html,
    atomic,
    locked,
    props,
  }
}

function wrapComponentBlock(def) {
  const attrs = [`data-component-type="${def.type}"`, `data-component-label="${def.label}"`]
  if (def.tableType) attrs.push(`data-table-type="${def.tableType}"`)
  if (def.imageType) attrs.push(`data-image-type="${def.imageType}"`)
  if (def.imageWidth) attrs.push(`data-image-width="${def.imageWidth}"`)
  if (def.imageHeight) attrs.push(`data-image-height="${def.imageHeight}"`)
  if (def.imageAlign) attrs.push(`data-image-align="${def.imageAlign}"`)
  return `<div ${attrs.join(' ')}>${def.html || ''}</div>`
}

function createComponentBlock(type) {
  const def = componentDefs.find(item => item.type === type)
  if (!def) return null
  const props = {
    tableType: def.tableType,
    imageType: def.imageType,
    imageWidth: def.imageWidth,
    imageHeight: def.imageHeight,
    imageAlign: def.imageAlign,
  }
  let html = def.html || ''
  if (def.type === 'dataTable') html = buildDataTableHtml(def.tableType)
  if (def.type === 'imageRadar' || def.type === 'imageCustom') html = buildImageHtml(def.imageType, def.imageWidth, def.imageHeight, def.imageAlign)
  return createBlock({
    type: def.type,
    label: def.label,
    html: wrapComponentBlock({ ...def, html }),
    atomic: !!def.atomic,
    locked: ['dataTable', 'imageRadar', 'imageCustom'].includes(def.type),
    props,
  })
}

function loadBlocksFromHtml(html) {
  const body = extractBodyContent(html || '')
  if (!body?.trim()) {
    blocks.value = []
    return
  }
  const wrapper = document.createElement('div')
  wrapper.innerHTML = body
  const children = Array.from(wrapper.children)
  blocks.value = children.length
    ? children.map((node) => {
      const type = node.dataset?.componentType || 'legacy'
      const def = componentDefs.find(item => item.type === type)
      const props = {
        tableType: node.dataset?.tableType || def?.tableType,
        imageType: node.dataset?.imageType || def?.imageType,
        imageWidth: Number(node.dataset?.imageWidth || def?.imageWidth || 80),
        imageHeight: Number(node.dataset?.imageHeight || def?.imageHeight || 160),
        imageAlign: node.dataset?.imageAlign || def?.imageAlign || 'center',
      }
      return createBlock({
        type,
        label: node.dataset?.componentLabel || def?.label || '模板区块',
        html: node.outerHTML,
        atomic: !!def?.atomic,
        locked: ['dataTable', 'imageRadar', 'imageCustom'].includes(type),
        props,
      })
    })
    : [createBlock({ type: 'legacy', label: '模板内容', html: body, atomic: true })]
}

function collectCurrentVariableSet() {
  const html = toHtmlContent()
  const set = new Set()
  let matched
  VARIABLE_PATTERN.lastIndex = 0
  while ((matched = VARIABLE_PATTERN.exec(html)) !== null) {
    const key = matched[1]
    if (!key || key.startsWith('item.')) continue
    set.add(key)
  }
  return set
}

function nextVariableName(baseName, usedSet) {
  if (!usedSet.has(baseName)) { usedSet.add(baseName); return baseName }
  let index = 2
  let candidate = `${baseName}_${index}`
  while (usedSet.has(candidate)) { index += 1; candidate = `${baseName}_${index}` }
  usedSet.add(candidate)
  return candidate
}

function withUniquePlaceholders(fragmentHtml) {
  const usedSet = collectCurrentVariableSet()
  const localMap = {}
  VARIABLE_PATTERN.lastIndex = 0
  return fragmentHtml.replace(VARIABLE_PATTERN, (full, key) => {
    if (!key || key.startsWith('item.')) return full
    if (FIXED_REPORT_PLACEHOLDERS.has(key)) return `{{${key}}}`
    if (!localMap[key]) localMap[key] = nextVariableName(key, usedSet)
    return `{{${localMap[key]}}}`
  })
}

function normalizeDuplicateVariables(html) {
  const countMap = {}
  VARIABLE_PATTERN.lastIndex = 0
  return html.replace(VARIABLE_PATTERN, (full, key) => {
    if (!key || key.startsWith('item.')) return full
    if (FIXED_REPORT_PLACEHOLDERS.has(key)) return `{{${key}}}`
    countMap[key] = (countMap[key] || 0) + 1
    if (countMap[key] === 1) return `{{${key}}}`
    return `{{${key}_${countMap[key]}}}`
  })
}

function normalizeCanvasPlaceholders() {
  const before = toHtmlContent()
  const after = normalizeDuplicateVariables(before)
  if (before === after) return false
  loadBlocksFromHtml(after)
  clearSelection()
  return true
}

function extractRootStyle(html) {
  const wrapper = document.createElement('div')
  wrapper.innerHTML = html || ''
  const first = wrapper.firstElementChild
  return first?.style || {}
}

function updateRootStyle(block, updater) {
  if (!block || block.atomic) return
  const wrapper = document.createElement('div')
  wrapper.innerHTML = block.html || ''
  const first = wrapper.firstElementChild
  if (!first) return
  updater(first.style)
  block.html = first.outerHTML
}

function selectBlock(block) {
  ensureBlockProps(block)
  selectedBlock.value = block
  const style = extractRootStyle(block.html)
  styleForm.textAlign = style.textAlign || 'left'
  styleForm.fontSize = parseInt(style.fontSize || '16', 10)
  styleForm.color = style.color || '#d8e7f5'
}

function clearSelection() {
  selectedBlock.value = null
}

function ensureBlockProps(block) {
  if (!block) return
  if (!block.props) block.props = {}
  if (block.type === 'dataTable') {
    block.props.tableType = block.props.tableType || 'indicator_summary_table'
  }
  if (block.type === 'imageRadar') {
    block.props.imageType = 'capability_radar_chart'
  }
  if (block.type === 'imageCustom') {
    block.props.imageType = 'custom_image'
  }
  if (block.type === 'imageRadar' || block.type === 'imageCustom') {
    block.props.imageWidth = Number(block.props.imageWidth || 80)
    block.props.imageHeight = Number(block.props.imageHeight || 160)
    block.props.imageAlign = block.props.imageAlign || 'center'
  }
}

function setWrappedBlockHtml(block, innerHtml, extraAttrs = {}) {
  if (!block) return
  const wrapper = document.createElement('div')
  wrapper.innerHTML = block.html || ''
  const first = wrapper.firstElementChild || document.createElement('div')
  first.dataset.componentType = block.type
  first.dataset.componentLabel = block.label
  Object.entries(extraAttrs).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      delete first.dataset[key]
    } else {
      first.dataset[key] = String(value)
    }
  })
  first.innerHTML = innerHtml
  block.html = first.outerHTML
}

function updateSelectedTableBlock() {
  const block = selectedBlock.value
  if (!block) return
  ensureBlockProps(block)
  if (block.type === 'dataTable') {
    setWrappedBlockHtml(block, buildDataTableHtml(block.props.tableType), {
      tableType: block.props.tableType,
    })
  }
}

function updateSelectedImageBlock() {
  const block = selectedBlock.value
  if (!block) return
  ensureBlockProps(block)
  const option = imageTypeOptions.find(item => item.value === block.props.imageType)
  if (option) block.label = option.label
  setWrappedBlockHtml(block, buildImageHtml(block.props.imageType, block.props.imageWidth, block.props.imageHeight, block.props.imageAlign), {
    imageType: block.props.imageType,
    imageWidth: block.props.imageWidth,
    imageHeight: block.props.imageHeight,
    imageAlign: block.props.imageAlign,
  })
}

function applyStyle() {
  if (!selectedBlock.value) return
  updateRootStyle(selectedBlock.value, (style) => {
    style.textAlign = styleForm.textAlign
    style.fontSize = `${styleForm.fontSize}px`
    style.color = styleForm.color
  })
}

const SINGLETON_BLOCK_TYPES = new Set([
  'indicatorSections',
])

function addComponent(type) {
  const def = componentDefs.find((item) => item.type === type)
  if (!def) return
  if (SINGLETON_BLOCK_TYPES.has(type) && blocks.value.some(b => b.type === type)) {
    const existing = blocks.value.find(b => b.type === type)
    proxy.$modal.msgWarning(`「${def.label}」全文唯一，无法重复添加，已为你定位到现有块`)
    if (existing) selectBlock(existing)
    return
  }
  let html = def.html
  const props = {
    tableType: def.tableType,
    imageType: def.imageType,
    imageWidth: def.imageWidth,
    imageHeight: def.imageHeight,
    imageAlign: def.imageAlign,
  }
  if (type === 'dataTable') html = buildDataTableHtml(def.tableType)
  if (type === 'imageRadar' || type === 'imageCustom') html = buildImageHtml(def.imageType, def.imageWidth, def.imageHeight, def.imageAlign)
  const normalizedFragment = withUniquePlaceholders(html)
  const block = createBlock({
    type,
    label: def.label,
    html: `<div data-component-type="${type}" data-component-label="${def.label}">${normalizedFragment}</div>`,
    atomic: !!def.atomic,
    locked: ['dataTable', 'imageRadar', 'imageCustom'].includes(type),
    props,
  })
  if (type === 'dataTable') {
    block.html = `<div data-component-type="${type}" data-component-label="${def.label}" data-table-type="${def.tableType}">${normalizedFragment}</div>`
  } else if (type === 'imageRadar' || type === 'imageCustom') {
    block.html = `<div data-component-type="${type}" data-component-label="${def.label}" data-image-type="${def.imageType}" data-image-width="${def.imageWidth}" data-image-height="${def.imageHeight}" data-image-align="${def.imageAlign}">${normalizedFragment}</div>`
  }
  blocks.value.push(block)
  selectBlock(block)
}

function deleteSelectedBlock() {
  if (!hasSelectedBlock.value) return
  blocks.value.splice(selectedBlockIndex.value, 1)
  clearSelection()
}

function moveSelectedBlock(offset) {
  const index = selectedBlockIndex.value
  const nextIndex = index + offset
  if (index < 0 || nextIndex < 0 || nextIndex >= blocks.value.length) return
  const [block] = blocks.value.splice(index, 1)
  blocks.value.splice(nextIndex, 0, block)
}

function copySelectedBlock() {
  if (!hasSelectedBlock.value) return
  const source = selectedBlock.value
  if (SINGLETON_BLOCK_TYPES.has(source.type)) {
    proxy.$modal.msgWarning(`「${source.label}」全文唯一，无法复制`)
    return
  }
  const copy = {
    ...source,
    id: nextBlockId(),
    html: withUniquePlaceholders(source.html),
  }
  blocks.value.splice(selectedBlockIndex.value + 1, 0, copy)
  selectBlock(copy)
}

function onBlockInput(block, event) {
  if (block.atomic || block.locked) return
  block.html = event.currentTarget?.innerHTML || ''
}

function renderBlockPreview(block) {
  if (block?.type === 'indicatorSections') {
    return `<div class="indicator-section-preview"><div class="indicator-preview-head"><strong>指标章节将按评估结果自动生成</strong><span>章节编号、层级与内容由后端按指标树递归渲染</span></div><div class="indicator-preview-sample"><div class="indicator-preview-row"><span class="indicator-preview-level">N.x</span><div class="indicator-preview-body"><div class="indicator-preview-title">能力域 / 子项 / 叶子指标</div><div class="indicator-preview-desc">按 h2~h5 自动展开，叶子节点自动插入总结句、评价表、指标图表。</div></div></div></div><div class="indicator-preview-fields"><span>章节编号</span><span>指标标题</span><span>总结句</span><span>评价表</span><span>指标图表</span></div></div>`
  }
  if (block?.type === 'dataTable') {
    const option = dataTableTypeOptions.find(item => item.value === block.props?.tableType) || dataTableTypeOptions[0]
    return `<div class="data-table-preview"><div class="data-table-title">${option.label}</div><table><tr><th>指标</th><th>权重</th><th>得分</th><th>等级</th></tr><tr><td>自动匹配</td><td>自动匹配</td><td>自动匹配</td><td>自动匹配</td></tr></table><div class="data-table-token">{{${option.placeholder}}}</div></div>`
  }
  if (block?.type === 'imageRadar' || block?.type === 'imageCustom') {
    const option = imageTypeOptions.find(item => item.value === block.props?.imageType) || imageTypeOptions[0]
    const width = Math.max(10, Math.min(Number(block.props?.imageWidth) || 80, 100))
    const height = Math.max(60, Math.min(Number(block.props?.imageHeight) || 160, 800))
    const align = block.props?.imageAlign || 'center'
    return `<div class="image-type-preview image-align-${align}"><div class="image-preview-frame" style="width:${width}%;height:${height}px;">${option.label}</div><div class="image-preview-token">{{${option.placeholder}}}</div></div>`
  }
  if (!block.atomic) return block.html
  return block.html
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function blockPlaceholderDetails(block) {
  return extractPlaceholdersFromHtml(block?.html || '').map((key) => {
    const meta = getPlaceholderMeta(key)
    return {
      key,
      token: `{{${key}}}`,
      label: meta.label,
      description: meta.description,
    }
  })
}

function clearCanvasContent() {
  proxy.$modal.confirm('确认清空画布内容吗？').then(() => {
    blocks.value = []
    clearSelection()
  })
}

// ========== 初始化 ==========
getList()
</script>

<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="模板名称" prop="templateName">
        <el-input
          v-model="queryParams.templateName"
          placeholder="请输入模板名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮区域 -->
    <div class="toolbar-row mb8">
      <div class="toolbar-btns">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
      </div>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </div>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="templateList" table-layout="fixed" style="width: 100%" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="46" align="center" />
      <el-table-column label="ID" prop="id" width="80" align="center" />
      <el-table-column label="模板名称" prop="templateName" min-width="180" show-overflow-tooltip />
      <el-table-column label="评估任务类型" prop="evaluationType" min-width="140" show-overflow-tooltip>
        <template #default="scope">
          {{ getTaskTypeLabel(scope.row.evaluationType) }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="160" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.createTime || '—' }}</template>
      </el-table-column>
      <el-table-column label="更新时间" min-width="160" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.updateTime || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="scope">
          <div class="table-ops">
            <el-button link type="primary" size="small" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
            <el-button link type="danger" size="small" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" fullscreen append-to-body destroy-on-close class="report-template-dialog">
      <div class="dialog-content">
        <!-- 左侧：表单区域 -->
        <div class="form-panel">
          <el-divider content-position="left">基本信息</el-divider>
          <el-form :model="form" label-width="100px" class="template-form">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="form.templateName" placeholder="请输入模板名称" />
            </el-form-item>
            <el-form-item label="评估任务类型" prop="evaluationType">
              <el-select v-model="form.evaluationType" placeholder="请选择评估任务类型" style="width: 100%">
                <el-option v-for="item in taskTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="模板描述" prop="templateDescription">
              <el-input v-model="form.templateDescription" type="textarea" :rows="3" placeholder="请输入模板描述" />
            </el-form-item>
          </el-form>

          <el-divider content-position="left">模板组件</el-divider>
          <div class="component-palette">
            <div
              v-for="comp in componentDefs"
              :key="comp.type"
              class="comp-item"
              @click="addComponent(comp.type)"
            >
              <span class="comp-label">{{ comp.label }}</span>
              <span class="comp-hint">{{ comp.hint }}</span>
            </div>
          </div>
        </div>

        <!-- 右侧：画布区域 -->
        <div class="canvas-panel">
          <div class="canvas-toolbar">
            <div class="toolbar-item">
              <span>对齐：</span>
              <el-select v-model="styleForm.textAlign" size="small" @change="applyStyle" style="width: 90px">
                <el-option label="左对齐" value="left" />
                <el-option label="居中" value="center" />
                <el-option label="右对齐" value="right" />
              </el-select>
            </div>
            <div class="toolbar-item">
              <span>字号：</span>
              <el-slider v-model="styleForm.fontSize" :min="12" :max="48" size="small" @input="applyStyle" style="width: 100px" />
            </div>
            <div class="toolbar-item">
              <span>颜色：</span>
              <el-color-picker v-model="styleForm.color" size="small" @change="applyStyle" />
            </div>
            <div v-if="isSelectedDataTable" class="toolbar-item toolbar-config">
              <span>表格数据：</span>
              <el-select v-model="selectedBlock.props.tableType" size="small" @change="updateSelectedTableBlock" style="width: 130px">
                <el-option v-for="item in dataTableTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>
            <div v-if="isSelectedImage" class="toolbar-item toolbar-config">
              <span>图片类型：</span>
              <el-select v-model="selectedBlock.props.imageType" size="small" @change="updateSelectedImageBlock" style="width: 140px">
                <el-option v-for="item in imageTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>
            <div v-if="isSelectedImage" class="toolbar-item toolbar-config">
              <span>尺寸：</span>
              <el-input-number v-model="selectedBlock.props.imageWidth" :min="10" :max="100" size="small" controls-position="right" @change="updateSelectedImageBlock" style="width: 92px" />
              <span>%</span>
              <el-input-number v-model="selectedBlock.props.imageHeight" :min="60" :max="800" size="small" controls-position="right" @change="updateSelectedImageBlock" style="width: 96px" />
              <span>px</span>
            </div>
            <div v-if="isSelectedImage" class="toolbar-item toolbar-config">
              <span>图片对齐：</span>
              <el-select v-model="selectedBlock.props.imageAlign" size="small" @change="updateSelectedImageBlock" style="width: 100px">
                <el-option v-for="item in imageAlignOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>
            <div class="toolbar-actions">
              <el-button size="small" :disabled="!hasSelectedBlock || selectedBlockIndex === 0" @click="moveSelectedBlock(-1)">上移</el-button>
              <el-button size="small" :disabled="!hasSelectedBlock || selectedBlockIndex === blocks.length - 1" @click="moveSelectedBlock(1)">下移</el-button>
              <el-button size="small" :disabled="!hasSelectedBlock" @click="copySelectedBlock">复制</el-button>
              <el-button size="small" :disabled="!hasSelectedBlock" @click="deleteSelectedBlock">删除区块</el-button>
              <el-button size="small" type="danger" @click="clearCanvasContent">清空画布</el-button>
            </div>
          </div>
          <div ref="canvasRef" class="canvas-area">
            <div v-if="!blocks.length" class="canvas-empty">
              点击左侧组件开始构建模板
            </div>
            <Draggable
              v-else
              v-model="blocks"
              item-key="id"
              handle=".drag-handle"
              ghost-class="drag-ghost"
              chosen-class="drag-chosen"
              class="canvas-block-list"
            >
              <template #item="{ element }">
                <div
                  class="canvas-block"
                  :class="{ selected: selectedBlock?.id === element.id, atomic: element.atomic }"
                  @click.stop="selectBlock(element)"
                >
                  <div class="block-action-bar">
                    <span class="drag-handle" title="拖拽排序">⋮⋮</span>
                    <span class="block-title">{{ element.label }}</span>
                    <span v-if="element.atomic" class="block-badge">{{ element.type === 'indicatorSections' ? '自动生成' : '不可拆分' }}</span>
                  </div>
                  <div v-if="blockPlaceholderDetails(element).length" class="block-placeholder-list">
                    <span
                      v-for="item in blockPlaceholderDetails(element)"
                      :key="item.key"
                      class="placeholder-chip"
                      :title="item.description"
                    >
                      <span>{{ item.label }}</span>
                      <code>{{ item.token }}</code>
                    </span>
                  </div>
                  <div
                    class="block-body"
                    :contenteditable="!element.atomic && !element.locked"
                    v-html="renderBlockPreview(element)"
                    @blur="onBlockInput(element, $event)"
                  />
                </div>
              </template>
            </Draggable>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm" :loading="loading">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;

  .toolbar-btns {
    display: flex;
    gap: 10px;
  }
}

.mb8 {
  margin-bottom: 8px;
}

.table-ops {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.report-template-dialog {
  .dialog-content {
    display: flex;
    height: calc(100vh - 200px);
    gap: 20px;
  }

  .form-panel {
    width: 380px;
    flex-shrink: 0;
    overflow-y: auto;
    padding-right: 10px;
    border-right: 1px solid #ebeef5;

    .template-form {
      :deep(.el-form-item) {
        margin-bottom: 18px;
      }
    }

    .component-palette {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 10px;

      .comp-item {
        border: 1px solid #e8e8e8;
        border-radius: 6px;
        padding: 12px 10px;
        cursor: pointer;
        transition: all 0.2s;
        background: #fafafa;

        &:hover {
          border-color: #409eff;
          background: #f0f7ff;
          transform: translateY(-1px);
        }

        .comp-label {
          display: block;
          font-weight: 600;
          font-size: 13px;
          color: #303133;
        }

        .comp-hint {
          display: block;
          font-size: 11px;
          color: #999;
          margin-top: 4px;
        }
      }
    }
  }

  .canvas-panel {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .canvas-toolbar {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 12px 16px;
      background: #f5f7fa;
      border-radius: 6px;
      margin-bottom: 12px;
      flex-wrap: wrap;

      .toolbar-item {
        display: flex;
        align-items: center;
        gap: 6px;

        span {
          font-size: 13px;
          color: #666;
        }
      }

      .toolbar-actions {
        margin-left: auto;
        display: flex;
        gap: 8px;
      }
    }

    .canvas-area {
      flex: 1;
      padding: 20px;
      overflow-y: auto;
      background: #f9f9f9;
      border: 1px dashed #dcdfe6;
      border-radius: 6px;
      min-height: 300px;

      .canvas-empty {
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #c0c4cc;
        font-size: 14px;
      }

      .canvas-block-list {
        display: flex;
        flex-direction: column;
        gap: 10px;
      }

      .canvas-block {
        position: relative;
        border: 1px solid transparent;
        padding: 0;
        border-radius: 6px;
        transition: all 0.2s;
        cursor: pointer;
        overflow: hidden;
        background: var(--el-bg-color);

        &:hover {
          border-color: var(--el-color-primary-light-5);
          background: var(--el-fill-color-light);
        }

        &.selected {
          border-color: var(--el-color-primary);
          background: var(--el-color-primary-light-9);
          box-shadow: 0 0 0 1px var(--el-color-primary-light-7) inset;
        }

        &.atomic .block-body {
          white-space: pre-wrap;
          font-family: "Cascadia Code", Consolas, monospace;
          font-size: 12px;
          line-height: 1.7;
          color: #4b5563;
          background: #f4f7fb;
        }

        &.atomic .block-body :deep(.indicator-section-preview) {
          white-space: normal;
          font-family: inherit;
          color: #1f2937;
        }
      }

      .block-action-bar {
        display: flex;
        align-items: center;
        gap: 8px;
        min-height: 32px;
        padding: 6px 10px;
        background: rgba(64, 158, 255, 0.08);
        border-bottom: 1px solid var(--el-border-color-lighter);
      }

      .drag-handle {
        cursor: grab;
        color: var(--el-color-primary);
        font-weight: 700;
        letter-spacing: -2px;
        user-select: none;
      }

      .block-title {
        font-size: 12px;
        font-weight: 700;
        color: var(--el-text-color-secondary);
      }

      .block-badge {
        margin-left: auto;
        font-size: 11px;
        color: var(--el-color-warning);
      }

      .block-placeholder-list {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        padding: 8px 10px;
        background: #f8fbff;
        border-bottom: 1px solid var(--el-border-color-lighter);
      }

      .placeholder-chip {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        max-width: 100%;
        padding: 3px 7px;
        border-radius: 4px;
        border: 1px solid #d7e6ff;
        background: #fff;
        color: #1f4f82;
        font-size: 11px;
        line-height: 1.4;

        code {
          color: #6b7280;
          font-family: "Cascadia Code", Consolas, monospace;
          font-size: 10px;
          white-space: nowrap;
        }
      }

      .block-body {
        padding: 12px 14px;
        min-height: 44px;
        outline: none;
      }

      .block-body :deep(.data-table-preview) {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }

      .block-body :deep(.data-table-title) {
        font-weight: 700;
        color: #1f2937;
      }

      .block-body :deep(.data-table-preview table) {
        width: 100%;
        border-collapse: collapse;
        background: #fff;
      }

      .block-body :deep(.data-table-preview th),
      .block-body :deep(.data-table-preview td) {
        border: 1px solid #d7dde7;
        padding: 7px 10px;
        text-align: center;
      }

      .block-body :deep(.data-table-token),
      .block-body :deep(.image-preview-token) {
        display: inline-flex;
        width: fit-content;
        padding: 3px 7px;
        border: 1px solid #d7e6ff;
        border-radius: 4px;
        color: #4b5563;
        background: #f8fbff;
        font-family: "Cascadia Code", Consolas, monospace;
        font-size: 11px;
      }

      .block-body :deep(.image-type-preview) {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }

      .block-body :deep(.image-align-left) {
        align-items: flex-start;
      }

      .block-body :deep(.image-align-center) {
        align-items: center;
      }

      .block-body :deep(.image-align-right) {
        align-items: flex-end;
      }

      .block-body :deep(.image-preview-frame) {
        display: flex;
        align-items: center;
        justify-content: center;
        max-width: 100%;
        border: 1px dashed #8fb8ff;
        border-radius: 6px;
        background: linear-gradient(135deg, #f4f8ff, #eef6ff);
        color: #1f4f82;
        font-weight: 700;
      }

      .block-body:focus {
        background: var(--el-color-primary-light-9);
        box-shadow: 0 0 0 1px var(--el-color-primary-light-7) inset;
      }

      :deep(.indicator-section-preview) {
        display: grid;
        gap: 10px;
        white-space: normal;
      }

      :deep(.indicator-preview-head) {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        padding-bottom: 8px;
        border-bottom: 1px solid #e5edf7;
      }

      :deep(.indicator-preview-head strong) {
        font-size: 14px;
        color: #111827;
      }

      :deep(.indicator-preview-head span) {
        flex: none;
        font-size: 12px;
        color: #64748b;
      }

      :deep(.indicator-preview-sample) {
        padding: 10px 12px;
        border: 1px solid #dbe7f3;
        border-radius: 6px;
        background: #fbfdff;
      }

      :deep(.indicator-preview-row) {
        display: grid;
        grid-template-columns: 56px 1fr;
        gap: 12px;
        align-items: center;
      }

      :deep(.indicator-preview-level) {
        align-self: center;
        padding: 3px 8px;
        border-radius: 4px;
        background: #e8f2ff;
        color: #1d4ed8;
        font-size: 12px;
        font-weight: 600;
        text-align: center;
      }

      :deep(.indicator-preview-body) {
        display: flex;
        flex-direction: column;
        gap: 2px;
      }

      :deep(.indicator-preview-title) {
        font-size: 14px;
        font-weight: 600;
        color: #1f2937;
      }

      :deep(.indicator-preview-desc) {
        font-size: 12px;
        line-height: 1.6;
        color: #64748b;
      }

      :deep(.indicator-preview-fields) {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
      }

      :deep(.indicator-preview-fields span) {
        padding: 3px 8px;
        border: 1px solid #dbe7f3;
        border-radius: 4px;
        background: #ffffff;
        color: #475569;
        font-size: 12px;
      }

      .drag-ghost {
        border: 2px dashed var(--el-color-primary);
        background: var(--el-color-primary-light-9);
      }

      .drag-chosen {
        box-shadow: 0 12px 28px rgba(64, 158, 255, 0.18);
      }
    }
  }
}

:deep(.canvas-block) {
  border: 1px solid transparent;
  padding: 5px;
  margin-bottom: 5px;
  border-radius: 4px;
  transition: all 0.2s;
  cursor: pointer;

  &:hover {
    border: 1px dashed #409eff;
  }

  &.selected {
    border: 1px solid #409eff;
    background: #ecf5ff;
  }
}

:deep([contenteditable="true"]) {
  &:focus {
    outline: none;
    background: #fff;
  }
}

:global(.report-template-dialog.el-dialog) {
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
}
:global(.report-template-dialog .el-dialog__header),
:global(.report-template-dialog .el-dialog__footer) {
  background: var(--el-bg-color);
  border-color: var(--el-border-color-lighter);
}
:global(.report-template-dialog .el-dialog__title) {
  color: var(--el-text-color-primary);
}
:global(.report-template-dialog .el-dialog__body) {
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
}
:global(.report-template-dialog .el-divider__text) {
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
}
:global(.report-template-dialog .el-form-item__label),
:global(.report-template-dialog .toolbar-item span) {
  color: var(--el-text-color-regular);
}
:global(.report-template-dialog .el-input__wrapper),
:global(.report-template-dialog .el-select__wrapper),
:global(.report-template-dialog .el-textarea__inner) {
  background: var(--el-fill-color-blank);
  box-shadow: 0 0 0 1px var(--el-border-color) inset;
  color: var(--el-text-color-primary);
}
:global(.report-template-dialog .el-input__inner),
:global(.report-template-dialog .el-select__placeholder),
:global(.report-template-dialog .el-textarea__inner) {
  color: var(--el-text-color-primary);
}
.report-template-dialog {
  .dialog-content {
    background: transparent;
  }

  .form-panel {
    border-right-color: var(--el-border-color-lighter);
  }

  .component-palette .comp-item {
    background: var(--el-bg-color);
    border-color: var(--el-border-color-lighter);
    box-shadow: none;

    &:hover {
      background: var(--el-fill-color-light);
      border-color: var(--el-color-primary-light-5);
      box-shadow: var(--el-box-shadow-light);
    }

    .comp-label {
      color: var(--el-text-color-primary);
    }

    .comp-hint {
      color: var(--el-text-color-secondary);
    }
  }

  .canvas-panel .canvas-toolbar {
    background: var(--el-fill-color-light);
    border: 1px solid var(--el-border-color-lighter);
  }

  .canvas-panel .canvas-area {
    background: #f9f9f9;
    border-color: var(--el-border-color);
    color: var(--el-text-color-primary);
  }

  .canvas-panel .canvas-empty {
    color: var(--el-text-color-placeholder);
  }
}

:deep(.canvas-block) {
  color: var(--el-text-color-primary);

  &:hover {
    border-color: var(--el-color-primary-light-5);
    background: var(--el-fill-color-light);
  }

  &.selected {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    box-shadow: 0 0 0 1px var(--el-color-primary-light-7) inset;
  }
}

:deep(.canvas-block h1),
:deep(.canvas-block h2),
:deep(.canvas-block h3),
:deep(.canvas-block h4),
:deep(.canvas-block p),
:deep(.canvas-block div),
:deep(.canvas-block span),
:deep(.canvas-block table),
:deep(.canvas-block td),
:deep(.canvas-block th) {
  color: inherit;
}

:deep([contenteditable="true"]:focus) {
  background: var(--el-color-primary-light-9);
  box-shadow: 0 0 0 1px var(--el-color-primary-light-7) inset;
}

:global(.dark-theme .report-template-dialog.el-dialog) {
  background: #07111f;
  color: #d8e7f5;
}
:global(.dark-theme .report-template-dialog .el-dialog__header),
:global(.dark-theme .report-template-dialog .el-dialog__footer) {
  background: #07111f;
  border-color: rgba(0, 242, 255, 0.14);
}
:global(.dark-theme .report-template-dialog .el-dialog__title) {
  color: #d8e7f5;
}
:global(.dark-theme .report-template-dialog .el-dialog__body) {
  background: linear-gradient(180deg, #07111f 0%, #050b14 100%);
  color: #d8e7f5;
}
:global(.dark-theme .report-template-dialog .el-divider__text) {
  background: #07111f;
  color: #d8e7f5;
}
:global(.dark-theme .report-template-dialog .el-form-item__label),
:global(.dark-theme .report-template-dialog .toolbar-item span) {
  color: #9fb6cb;
}
:global(.dark-theme .report-template-dialog .el-input__wrapper),
:global(.dark-theme .report-template-dialog .el-select__wrapper),
:global(.dark-theme .report-template-dialog .el-textarea__inner) {
  background: rgba(3, 12, 22, 0.82);
  box-shadow: 0 0 0 1px rgba(71, 153, 191, 0.26) inset;
  color: #d8e7f5;
}
:global(.dark-theme .report-template-dialog .el-input__inner),
:global(.dark-theme .report-template-dialog .el-select__placeholder),
:global(.dark-theme .report-template-dialog .el-textarea__inner) {
  color: #d8e7f5;
}
:global(.dark-theme) .report-template-dialog {
  .form-panel {
    border-right-color: rgba(0, 242, 255, 0.16);
  }

  .component-palette .comp-item {
    background: rgba(5, 13, 24, 0.9);
    border-color: rgba(71, 153, 191, 0.24);
    box-shadow: inset 0 0 0 1px rgba(0, 242, 255, 0.03);

    &:hover {
      background: rgba(0, 242, 255, 0.08);
      border-color: rgba(0, 242, 255, 0.42);
      box-shadow: 0 10px 24px rgba(0, 0, 0, 0.24);
    }

    .comp-label {
      color: #d8e7f5;
    }

    .comp-hint {
      color: #7f98ae;
    }
  }

  .canvas-panel .canvas-toolbar {
    background: rgba(5, 13, 24, 0.92);
    border-color: rgba(0, 242, 255, 0.16);
  }

  .canvas-panel .canvas-area {
    background:
      radial-gradient(circle at 16% 18%, rgba(0, 242, 255, 0.08), transparent 34%),
      linear-gradient(180deg, rgba(6, 17, 31, 0.98), rgba(3, 9, 18, 0.98));
    border-color: rgba(0, 242, 255, 0.18);
    color: #d8e7f5;
  }

  .canvas-panel .canvas-empty {
    color: #6f879d;
  }
}
:global(.dark-theme) :deep(.canvas-block) {
  color: #d8e7f5;

  &:hover {
    border-color: rgba(0, 242, 255, 0.6);
    background: rgba(0, 242, 255, 0.04);
  }

  &.selected {
    border-color: rgba(0, 242, 255, 0.9);
    background: rgba(0, 242, 255, 0.08);
    box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.16) inset;
  }
}
:global(.dark-theme) :deep([contenteditable="true"]:focus) {
  background: rgba(0, 242, 255, 0.08);
  box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.24) inset;
}
</style>

<style>
.dark-theme .report-template-dialog .form-panel {
  border-right-color: rgba(0, 242, 255, 0.16);
}
.dark-theme .report-template-dialog .component-palette .comp-item {
  background: rgba(5, 13, 24, 0.9);
  border-color: rgba(71, 153, 191, 0.24);
  box-shadow: inset 0 0 0 1px rgba(0, 242, 255, 0.03);
}
.dark-theme .report-template-dialog .component-palette .comp-item:hover {
  background: rgba(0, 242, 255, 0.08);
  border-color: rgba(0, 242, 255, 0.42);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.24);
}
.dark-theme .report-template-dialog .component-palette .comp-label {
  color: #d8e7f5;
}
.dark-theme .report-template-dialog .component-palette .comp-hint {
  color: #7f98ae;
}
.dark-theme .report-template-dialog .canvas-panel .canvas-toolbar {
  background: rgba(5, 13, 24, 0.92);
  border-color: rgba(0, 242, 255, 0.16);
}
.dark-theme .report-template-dialog .canvas-panel .canvas-area {
  background:
    radial-gradient(circle at 16% 18%, rgba(0, 242, 255, 0.08), transparent 34%),
    linear-gradient(180deg, rgba(6, 17, 31, 0.98), rgba(3, 9, 18, 0.98));
  border-color: rgba(0, 242, 255, 0.18);
  color: #d8e7f5;
}
.dark-theme .report-template-dialog .canvas-panel .canvas-empty {
  color: #6f879d;
}
.dark-theme .report-template-dialog .canvas-block {
  color: #d8e7f5;
}
.dark-theme .report-template-dialog .canvas-block:hover {
  border-color: rgba(0, 242, 255, 0.6);
  background: rgba(0, 242, 255, 0.04);
}
.dark-theme .report-template-dialog .canvas-block.selected {
  border-color: rgba(0, 242, 255, 0.9);
  background: rgba(0, 242, 255, 0.08);
  box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.16) inset;
}
.dark-theme .report-template-dialog .canvas-block h1,
.dark-theme .report-template-dialog .canvas-block h2,
.dark-theme .report-template-dialog .canvas-block h3,
.dark-theme .report-template-dialog .canvas-block h4,
.dark-theme .report-template-dialog .canvas-block p,
.dark-theme .report-template-dialog .canvas-block div,
.dark-theme .report-template-dialog .canvas-block span,
.dark-theme .report-template-dialog .canvas-block table,
.dark-theme .report-template-dialog .canvas-block td,
.dark-theme .report-template-dialog .canvas-block th {
  color: inherit;
}
.dark-theme .report-template-dialog [contenteditable="true"]:focus {
  background: rgba(0, 242, 255, 0.08);
  box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.24) inset;
}
</style>
