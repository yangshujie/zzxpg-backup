<script setup name="ReportGenerate">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  getTemplate,
  listTemplates,
  previewTemplate,
  downloadTemplateFile,
} from '@/api/zhpg/report'

const { proxy } = getCurrentInstance()

const previewRef = ref(null)

const state = reactive({
  templates: [],
  placeholders: [],
  fields: {},
  previewHtml: '',
  editedPreviewHtml: '',
  loading: false,
  currentTemplateId: null,
})

const canEdit = computed(() => state.currentTemplateId !== null)

async function refreshTemplates() {
  const res = await listTemplates()
  state.templates = res.data || []
}

const VARIABLE_PATTERN = /\{\{\s*([a-zA-Z0-9_.]+)\s*}}/g
function extractPlaceholdersFromHtml(html) {
  const source = html || ''
  const set = new Set()
  let matched
  while ((matched = VARIABLE_PATTERN.exec(source)) !== null) {
    const key = matched[1]
    if (!key || key.startsWith('item.')) continue
    set.add(key)
  }
  return Array.from(set)
}

async function selectTemplate(id) {
  try {
    state.loading = true
    state.currentTemplateId = id
    const res = await getTemplate(id)
    const data = res.data
    state.placeholders = data.placeholders || extractPlaceholdersFromHtml(data.htmlContent)
    // 重置字段
    const nextFields = {}
    state.placeholders.forEach((key) => {
      nextFields[key] = ''
    })
    state.fields = nextFields
    state.previewHtml = ''
    state.editedPreviewHtml = ''
  } finally {
    state.loading = false
  }
}

async function runPreview() {
  if (!canEdit.value) return
  try {
    state.loading = true
    const res = await previewTemplate(state.currentTemplateId, { fields: state.fields })
    state.previewHtml = res.data.html || ''
    state.editedPreviewHtml = state.previewHtml
    proxy.$modal.msgSuccess('预览已更新')
  } finally {
    state.loading = false
  }
}

async function downloadFile(type) {
  if (!canEdit.value) return
  try {
    state.loading = true
    const payload = { fields: state.fields }
    const editedHtml = previewRef.value?.innerHTML || state.editedPreviewHtml
    if (editedHtml && editedHtml.trim()) payload.editedHtml = editedHtml
    
    const res = await downloadTemplateFile(state.currentTemplateId, type, payload)
    const blob = new Blob([res], { type: type === 'docx' ? 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' : 'text/html' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `report_${Date.now()}.${type}`
    link.click()
    URL.revokeObjectURL(link.href)
    proxy.$modal.msgSuccess(`下载成功`)
  } finally {
    state.loading = false
  }
}

function onPreviewEditorInput(event) {
  const root = previewRef.value
  if (!root) {
    state.editedPreviewHtml = event.currentTarget?.innerHTML || ''
    return
  }
  const target = event.target instanceof HTMLElement ? event.target.closest('[data-field-key]') : null
  if (target instanceof HTMLElement) {
    const key = target.dataset.fieldKey || ''
    if (key) {
      const value = target.textContent || ''
      state.fields[key] = value
      const safeKey = key.replace(/"/g, '\\"')
      root.querySelectorAll(`[data-field-key="${safeKey}"]`).forEach((node) => {
        if (node !== target) node.textContent = value
      })
    }
  }
  state.editedPreviewHtml = root.innerHTML
}

function onFieldInput(key) {
  const root = previewRef.value
  if (!root || !key) return
  const value = state.fields[key] || ''
  const safeKey = key.replace(/"/g, '\\"')
  root.querySelectorAll(`[data-field-key="${safeKey}"]`).forEach((node) => {
    node.textContent = value
  })
  state.editedPreviewHtml = root.innerHTML
}

onMounted(() => {
  refreshTemplates()
})
</script>

<template>
  <div class="app-container">
    <div class="generate-container">
      <div class="gen-left">
        <el-card class="h100">
          <template #header>
            <div class="card-header">
              <span>选择模板</span>
              <el-button size="small" @click="refreshTemplates">刷新</el-button>
            </div>
          </template>
          <div class="template-list compact">
            <div
              v-for="tpl in state.templates"
              :key="tpl.id"
              :class="['template-item', { active: tpl.id === state.currentTemplateId }]"
              @click="selectTemplate(tpl.id)"
            >
              <div class="tpl-name">{{ tpl.templateName }}</div>
              <div class="tpl-code">{{ tpl.templateCode }}</div>
            </div>
          </div>
        </el-card>
      </div>

      <div class="gen-middle">
        <el-card class="h100">
          <template #header>
            <span>字段填充</span>
          </template>
          <div v-if="state.placeholders.length" class="field-list">
            <el-form label-position="top">
              <el-form-item v-for="key in state.placeholders" :key="key" :label="key">
                <el-input v-model="state.fields[key]" :placeholder="`请输入 ${key}`" @input="onFieldInput(key)" />
              </el-form-item>
            </el-form>
          </div>
          <div v-else class="empty-tip">选择模板后显示可填充字段</div>
          <div class="gen-actions mt10">
            <el-button type="primary" style="width: 100%" @click="runPreview" :disabled="!canEdit" :loading="state.loading">在线预览</el-button>
            <div class="mt10 flex">
              <el-button type="success" plain style="flex: 1" @click="downloadFile('html')" :disabled="!canEdit">下载 HTML</el-button>
              <el-button type="primary" plain style="flex: 1" @click="downloadFile('docx')" :disabled="!canEdit">下载 DOCX</el-button>
            </div>
          </div>
        </el-card>
      </div>

      <div class="gen-right">
        <el-card class="h100 preview-card">
          <template #header>
            <div class="card-header">
              <span>预览窗口</span>
              <small>预览区可直接编辑，内容同步至左侧表单</small>
            </div>
          </template>
          <div
            v-if="state.previewHtml"
            ref="previewRef"
            class="preview-box"
            contenteditable="true"
            @input="onPreviewEditorInput"
            v-html="state.previewHtml"
          ></div>
          <div v-else class="empty-preview">点击“在线预览”查看生成效果</div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.generate-container {
  display: flex;
  gap: 15px;
  height: calc(100vh - 120px);
  .gen-left { 
    width: 260px; 
    .template-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
      .template-item {
        border: 1px solid #dcdfe6;
        padding: 10px;
        border-radius: 4px;
        cursor: pointer;
        &:hover { border-color: #1890ff; background: #f0f7ff; }
        &.active { border-color: #1890ff; background: #e6f7ff; }
        .tpl-name { font-weight: bold; font-size: 13px; }
        .tpl-code { font-size: 11px; color: #999; margin-top: 2px; }
      }
    }
  }
  .gen-middle {
    width: 340px;
    display: flex;
    flex-direction: column;
    .field-list { flex: 1; overflow-y: auto; padding-right: 5px; }
  }
  .gen-right {
    flex: 1;
    .preview-card {
      display: flex;
      flex-direction: column;
      :deep(.el-card__body) { flex: 1; overflow: hidden; padding: 10px; }
      .preview-box {
        height: 100%;
        border: 1px solid #dcdfe6;
        padding: 20px;
        background: #fff;
        overflow-y: auto;
        &:focus { outline: 2px solid #1890ff; }
      }
    }
  }
}

.empty-tip, .empty-preview { color: #999; text-align: center; padding: 40px 0; font-size: 14px; }
.card-header { display: flex; justify-content: space-between; align-items: center; small { color: #999; font-size: 12px; } }
.mt10 { margin-top: 10px; }
.flex { display: flex; gap: 10px; }
.h100 { height: 100%; }
</style>
