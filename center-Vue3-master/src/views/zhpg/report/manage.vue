<script setup name="ReportManage">
import { reactive, ref, computed, nextTick, watch } from 'vue'
import {
  createTemplate,
  deleteTemplate,
  getTemplate,
  listTemplates,
  updateTemplate,
  updateTemplateContent,
} from '@/api/zhpg/report'

const { proxy } = getCurrentInstance()

const VARIABLE_PATTERN = /\{\{\s*([a-zA-Z0-9_.]+)\s*}}/g

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
  { type: 'table', label: '表格', hint: '指标对比表', html: '<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;"><tr><th>指标</th><th>值</th></tr><tr><td>{{MetricName}}</td><td>{{MetricValue}}</td></tr></table>' },
  { type: 'image', label: '图片', hint: '图片展示', html: '<p style="text-align:center;"><img src="{{ImageUrl}}" style="max-width:80%;" alt="img" /></p>' },
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
const selectedBlock = ref(null)
const styleForm = reactive({
  textAlign: 'left',
  fontSize: 16,
  color: '#111827',
})

// 监听对话框打开，加载画布内容
watch(dialogVisible, async (val) => {
  if (val && form.htmlContent) {
    await nextTick()
    if (canvasRef.value) {
      canvasRef.value.innerHTML = extractBodyContent(form.htmlContent || '')
      wireCanvasBlocks()
      clearSelection()
    }
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
  if (canvasRef.value) {
    canvasRef.value.innerHTML = ''
    clearSelection()
  }
}

// ========== 画布相关函数 ==========
function nextBlockId() {
  return `block-${Date.now()}-${Math.floor(Math.random() * 10000)}`
}

function ensureBlockId(el) {
  if (!el.id) el.id = nextBlockId()
  return el.id
}

function buildTableHtml() {
  const rows = 4, cols = 3
  const tableRows = []
  const headCells = []
  for (let col = 1; col <= cols; col += 1) headCells.push(`<th>表头${col}</th>`)
  tableRows.push(`<tr>${headCells.join('')}</tr>`)
  for (let row = 2; row <= rows; row += 1) {
    const bodyCells = []
    for (let col = 1; col <= cols; col += 1) bodyCells.push(`<td>内容${row - 1}-${col}</td>`)
    tableRows.push(`<tr>${bodyCells.join('')}</tr>`)
  }
  return `<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">${tableRows.join('')}</table>`
}

function extractBodyContent(html) {
  const match = (html || '').match(/<body[^>]*>([\s\S]*)<\/body>/i)
  return match ? match[1] : html
}

function toHtmlContent() {
  const clone = canvasRef.value?.cloneNode(true)
  if (!clone) return '<html><body></body></html>'
  clone.querySelectorAll('.canvas-block').forEach((node) => node.classList.remove('selected'))
  return `<html><body>${clone.innerHTML}</body></html>`
}

function collectCurrentVariableSet() {
  const html = toHtmlContent()
  const set = new Set()
  let matched
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
  return fragmentHtml.replace(VARIABLE_PATTERN, (full, key) => {
    if (!key || key.startsWith('item.')) return full
    if (!localMap[key]) localMap[key] = nextVariableName(key, usedSet)
    return `{{${localMap[key]}}}`
  })
}

function normalizeDuplicateVariables(html) {
  const countMap = {}
  return html.replace(VARIABLE_PATTERN, (full, key) => {
    if (!key || key.startsWith('item.')) return full
    countMap[key] = (countMap[key] || 0) + 1
    if (countMap[key] === 1) return `{{${key}}}`
    return `{{${key}_${countMap[key]}}}`
  })
}

function normalizeCanvasPlaceholders() {
  if (!canvasRef.value) return false
  const before = toHtmlContent()
  const after = normalizeDuplicateVariables(before)
  if (before === after) return false
  canvasRef.value.innerHTML = extractBodyContent(after)
  wireCanvasBlocks()
  clearSelection()
  return true
}

function makeEditable(root) {
  root.querySelectorAll('h1,h2,h3,h4,p,td,th,li,span,div,blockquote').forEach((node) => {
    node.setAttribute('contenteditable', 'true')
  })
  if (root.dataset.editableBound === '1') return
  root.addEventListener('focusin', (event) => {
    if (event.target instanceof HTMLElement && event.target.closest('[contenteditable="true"]')) root.draggable = false
  })
  root.addEventListener('focusout', (event) => {
    const next = event.relatedTarget
    if (next instanceof Node && root.contains(next)) return
    root.draggable = true
  })
  root.dataset.editableBound = '1'
}

function markBlock(el) {
  if (!el.classList.contains('canvas-block')) el.classList.add('canvas-block')
  ensureBlockId(el)
  makeEditable(el)
  el.draggable = true
  el.onclick = () => selectBlock(el)
}

function wireCanvasBlocks() {
  canvasRef.value?.querySelectorAll(':scope > *').forEach((node) => markBlock(node))
}

function selectBlock(el) {
  canvasRef.value?.querySelectorAll('.canvas-block').forEach((item) => item.classList.remove('selected'))
  el.classList.add('selected')
  selectedBlock.value = el
  styleForm.textAlign = el.style.textAlign || 'left'
  styleForm.fontSize = parseInt(el.style.fontSize || '16', 10)
  styleForm.color = el.style.color || '#111827'
}

function clearSelection() {
  canvasRef.value?.querySelectorAll('.canvas-block').forEach((item) => item.classList.remove('selected'))
  selectedBlock.value = null
}

function applyStyle() {
  if (!selectedBlock.value) return
  selectedBlock.value.style.textAlign = styleForm.textAlign
  selectedBlock.value.style.fontSize = `${styleForm.fontSize}px`
  selectedBlock.value.style.color = styleForm.color
}

function addComponent(type) {
  const def = componentDefs.find((item) => item.type === type)
  if (!def || !canvasRef.value) return
  let html = def.html
  if (type === 'table') html = buildTableHtml()
  const normalizedFragment = withUniquePlaceholders(html)
  const wrapper = document.createElement('div')
  wrapper.innerHTML = normalizedFragment
  const node = document.createElement('div')
  node.dataset.componentType = type
  node.innerHTML = wrapper.innerHTML
  markBlock(node)
  canvasRef.value.appendChild(node)
  selectBlock(node)
}

function deleteSelectedBlock() {
  if (!selectedBlock.value) return
  selectedBlock.value.remove()
  clearSelection()
}

function clearCanvasContent() {
  if (!canvasRef.value) return
  proxy.$modal.confirm('确认清空画布内容吗？').then(() => {
    canvasRef.value.innerHTML = ''
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
            <div class="toolbar-actions">
              <el-button size="small" :disabled="!selectedBlock" @click="deleteSelectedBlock">删除区块</el-button>
              <el-button size="small" type="danger" @click="clearCanvasContent">清空画布</el-button>
            </div>
          </div>
          <div ref="canvasRef" class="canvas-area" @dragover.prevent>
            <div v-if="!canvasRef || !canvasRef.childElementCount" class="canvas-empty">
              点击左侧组件开始构建模板
            </div>
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
</style>
