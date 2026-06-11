<template>
  <div class="app-container" :class="themeStore.currentThemeName">
    <div class="page-header mb12">
      <h2 class="page-title">任务模板管理</h2>
      <p class="page-desc">维护评估任务模板（作战效能 / 体系贡献率 / 作战任务满足度），引用指标体系、计算流程模板与评估准则，供评估任务构建时快速调用。</p>
    </div>

    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="100px">
      <el-form-item label="模板名称" prop="templateName">
        <el-input v-model="queryParams.templateName" placeholder="请输入模板名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="评估方法" prop="templateType">
        <el-select v-model="queryParams.templateType" placeholder="全部" clearable style="width: 180px">
          <el-option v-for="item in TEMPLATE_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="分类" prop="classification">
        <el-select v-model="queryParams.classification" placeholder="全部" clearable style="width: 130px">
          <el-option v-for="item in CLASSIFICATION_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 130px">
          <el-option v-for="item in TEMPLATE_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar-row mb8">
      <el-button type="primary" plain icon="Plus" @click="handleAdd">新建任务模板</el-button>
      <el-button type="danger" plain icon="Delete" :disabled="ids.length === 0" @click="handleBatchDelete">删除</el-button>
    </div>

    <el-table v-loading="loading" :data="templateList" row-key="id" style="width: 100%" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="48" align="center" />
      <el-table-column label="模板名称" prop="templateName" min-width="200" show-overflow-tooltip />
      <el-table-column label="评估方法" prop="templateType" min-width="150">
        <template #default="scope">{{ getEnumLabel(TEMPLATE_TYPE_OPTIONS, scope.row.templateType) }}</template>
      </el-table-column>
      <el-table-column label="分类" prop="classification" min-width="100" align="center">
        <template #default="scope">{{ getEnumLabel(CLASSIFICATION_OPTIONS, scope.row.classification) }}</template>
      </el-table-column>
      <el-table-column label="装备类型" prop="equipmentType" min-width="130">
        <template #default="scope">{{ getEnumLabel(EQUIPMENT_TYPE_OPTIONS, scope.row.equipmentType) }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" min-width="100" align="center">
        <template #default="scope">
          <el-tag :type="templateStatusTagType(scope.row.status)" size="small">
            {{ scope.row.status === 'PUBLISHED' ? '已启用' : '已停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" min-width="170" align="center" show-overflow-tooltip>
        <template #default="scope">{{ parseTime(scope.row.updateTime || scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" min-width="180" align="center" fixed="right">
        <template #default="scope">
          <el-button plain type="primary" size="small" icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button
            plain
            :type="scope.row.status === 'PUBLISHED' ? 'warning' : 'success'"
            :icon="scope.row.status === 'PUBLISHED' ? 'Close' : 'Check'"
            size="small"
            @click="handleStatus(scope.row, scope.row.status === 'PUBLISHED' ? 'disable' : 'enable')"
          >{{ scope.row.status === 'PUBLISHED' ? '停用' : '启用' }}</el-button>
          <el-button
            plain type="danger" size="small" icon="Delete"
            @click="handleDelete(scope.row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" maxlength="200" />
        </el-form-item>
        <el-form-item label="评估方法" prop="templateType">
          <el-select v-model="form.templateType" placeholder="请选择评估方法类型" style="width: 100%">
            <el-option v-for="item in TEMPLATE_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板分类" prop="classification">
          <el-radio-group v-model="form.classification">
            <el-radio v-for="item in CLASSIFICATION_OPTIONS" :key="item.value" :label="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="装备类型" prop="equipmentType">
          <el-select v-model="form.equipmentType" placeholder="请选择装备类型" clearable style="width: 100%">
            <el-option v-for="item in EQUIPMENT_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估粒度" prop="calcGranularity">
          <el-select v-model="form.calcGranularity" placeholder="请选择评估粒度" clearable style="width: 100%">
            <el-option v-for="item in CALC_GRANULARITY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="指标体系" prop="indicatorSystemId">
          <el-select
            v-model="form.indicatorSystemId"
            placeholder="请选择引用的指标体系（可选）"
            filterable
            clearable
            remote
            :remote-method="remoteIndicator"
            :loading="indicatorLoading"
            style="width: 100%"
          >
            <el-option
              v-for="s in indicatorOptions"
              :key="s.indicatorSystemId"
              :label="s.indicatorSystemName"
              :value="s.indicatorSystemId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="流程模板" prop="calcFlowTemplateId">
          <el-select
            v-model="form.calcFlowTemplateId"
            placeholder="请选择引用的计算流程模板（可选）"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="t in calcFlowOptions"
              :key="t.id"
              :label="`${t.templateName}（${t.versionNo || 'V1.0'}）`"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="模板说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" placeholder="请输入模板说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useThemeStore } from '@/store/theme'
import {
  listEvalTaskTemplate, getEvalTaskTemplate, addEvalTaskTemplate, updateEvalTaskTemplate,
  delEvalTaskTemplate, disableEvalTaskTemplate, enableEvalTaskTemplate
} from '@/api/zhpg/evalTaskTemplate'
import {
  TEMPLATE_TYPE_OPTIONS, CLASSIFICATION_OPTIONS, TEMPLATE_STATUS_OPTIONS,
  EQUIPMENT_TYPE_OPTIONS, CALC_GRANULARITY_OPTIONS, getEnumLabel, templateStatusTagType
} from '@/api/zhpg/constants'
import { selectIndicatorSystem } from '@/api/zhpg/indicatorSystem'
import { listCalcFlow } from '@/api/zhpg/calcFlow'

const themeStore = useThemeStore()
const loading = ref(false)
const templateList = ref([])
const total = ref(0)
const ids = ref([])
const queryRef = ref()
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  templateName: undefined,
  templateType: undefined,
  classification: undefined,
  status: undefined
})

function getList() {
  loading.value = true
  listEvalTaskTemplate(queryParams).then(res => {
    templateList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}
function resetQuery() {
  queryRef.value && queryRef.value.resetFields()
  queryParams.templateName = undefined
  queryParams.templateType = undefined
  queryParams.classification = undefined
  queryParams.status = undefined
  handleQuery()
}
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
}

// ==================== 新建/编辑 ====================
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const defaultForm = () => ({
  id: undefined,
  templateName: '',
  templateType: 'EQUIP_EFFECTIVENESS',
  classification: 'SPECIFIC',
  equipmentType: undefined,
  calcGranularity: undefined,
  indicatorSystemId: undefined,
  calcFlowTemplateId: undefined,
  description: ''
})
const form = reactive(defaultForm())

// 指标体系 / 流程模板 下拉选项
const indicatorOptions = ref([])
const indicatorLoading = ref(false)
const calcFlowOptions = ref([])

function remoteIndicator(keyword) {
  indicatorLoading.value = true
  selectIndicatorSystem(keyword).then(res => {
    indicatorOptions.value = Array.isArray(res.data) ? res.data : []
  }).finally(() => { indicatorLoading.value = false })
}
function loadCalcFlowOptions() {
  listCalcFlow({ pageNum: 1, pageSize: 500 }).then(res => {
    calcFlowOptions.value = res.rows || []
  })
}

const rules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  templateType: [{ required: true, message: '请选择评估方法类型', trigger: 'change' }],
  classification: [{ required: true, message: '请选择模板分类', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, defaultForm())
  formRef.value && formRef.value.clearValidate()
}
function handleAdd() {
  resetForm()
  remoteIndicator('')
  loadCalcFlowOptions()
  dialogTitle.value = '新建任务模板'
  dialogVisible.value = true
}
function handleEdit(row) {
  resetForm()
  remoteIndicator('')
  loadCalcFlowOptions()
  getEvalTaskTemplate(row.id).then(res => {
    Object.assign(form, res.data || row)
    dialogTitle.value = '编辑任务模板'
    dialogVisible.value = true
  })
}
function handleSubmit() {
  formRef.value.validate(valid => {
    if (!valid) return
    submitting.value = true
    const api = form.id ? updateEvalTaskTemplate : addEvalTaskTemplate
    api(form).then(() => {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      getList()
    }).finally(() => { submitting.value = false })
  })
}

// ==================== 启用/停用 / 删除 ====================
const statusApiMap = {
  disable: disableEvalTaskTemplate,
  enable: enableEvalTaskTemplate
}
function handleStatus(row, action) {
  statusApiMap[action](row.id).then(() => {
    ElMessage.success('操作成功')
    getList()
  })
}
function handleDelete(row) {
  ElMessageBox.confirm(`确认删除模板「${row.templateName}」？`, '提示', { type: 'warning' }).then(() => {
    return delEvalTaskTemplate(row.id)
  }).then(() => {
    ElMessage.success('删除成功')
    getList()
  }).catch(() => {})
}
function handleBatchDelete() {
  if (ids.value.length === 0) return
  ElMessageBox.confirm(`确认删除选中的 ${ids.value.length} 个模板？`, '提示', { type: 'warning' }).then(() => {
    return delEvalTaskTemplate(ids.value.join(','))
  }).then(() => {
    ElMessage.success('删除成功')
    getList()
  }).catch(() => {})
}

getList()
</script>

<style scoped>
.page-header { padding-bottom: 8px; }
.page-title { margin: 0; font-size: 18px; }
.page-desc { margin: 4px 0 0; color: #909399; font-size: 13px; }
.toolbar-row { display: flex; gap: 8px; }
.mb8 { margin-bottom: 8px; }
.mb12 { margin-bottom: 12px; }

/* 容器底色与文字颜色优化：防微前端白色背景穿透 */
.app-container {
  background-color: var(--bg-color) !important;
  color: var(--text-color-primary) !important;
  min-height: 100vh;
  box-sizing: border-box;
  transition: background-color 0.3s ease, color 0.3s ease;
}

/* ==================== 按钮通用基础样式 ==================== */
.app-container :deep(.el-button.is-plain) {
  font-weight: 600 !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1) !important;
  border-radius: 4px !important;
}

/* ==================== 1. 亮色主题下极致清晰的按钮定制 ==================== */
/* (同时兼容本地 themeClass 与微前端全局 .light-clean 挂载) */

/* 编辑按钮 (Primary) */
.app-container.light-clean :deep(.el-button.is-plain.el-button--primary),
:global(.light-clean) .app-container :deep(.el-button.is-plain.el-button--primary) {
  background: rgba(0, 102, 204, 0.08) !important;
  border: 1px solid rgba(0, 102, 204, 0.45) !important;
  color: #0056b3 !important;
}
.app-container.light-clean :deep(.el-button.is-plain.el-button--primary:hover),
.app-container.light-clean :deep(.el-button.is-plain.el-button--primary:focus),
:global(.light-clean) .app-container :deep(.el-button.is-plain.el-button--primary:hover),
:global(.light-clean) .app-container :deep(.el-button.is-plain.el-button--primary:focus) {
  background: #0056b3 !important;
  border-color: #0056b3 !important;
  color: #ffffff !important;
  box-shadow: 0 2px 6px rgba(0, 86, 179, 0.25) !important;
}

/* 启用按钮 (Success) */
.app-container.light-clean :deep(.el-button.is-plain.el-button--success),
:global(.light-clean) .app-container :deep(.el-button.is-plain.el-button--success) {
  background: rgba(40, 122, 29, 0.08) !important;
  border: 1px solid rgba(40, 122, 29, 0.45) !important;
  color: #287a1d !important;
}
.app-container.light-clean :deep(.el-button.is-plain.el-button--success:hover),
.app-container.light-clean :deep(.el-button.is-plain.el-button--success:focus),
:global(.light-clean) .app-container :deep(.el-button.is-plain.el-button--success:hover),
:global(.light-clean) .app-container :deep(.el-button.is-plain.el-button--success:focus) {
  background: #287a1d !important;
  border-color: #287a1d !important;
  color: #ffffff !important;
  box-shadow: 0 2px 6px rgba(40, 122, 29, 0.25) !important;
}

/* 停用按钮 (Warning) */
.app-container.light-clean :deep(.el-button.is-plain.el-button--warning),
:global(.light-clean) .app-container :deep(.el-button.is-plain.el-button--warning) {
  /* 警告按钮：亮色下不能用黄色，必须用高对比度的深橙色，以防白底刺眼看不清 */
  background: rgba(186, 74, 0, 0.08) !important;
  border: 1px solid rgba(186, 74, 0, 0.45) !important;
  color: #ba4a00 !important;
}
.app-container.light-clean :deep(.el-button.is-plain.el-button--warning:hover),
.app-container.light-clean :deep(.el-button.is-plain.el-button--warning:focus),
:global(.light-clean) .app-container :deep(.el-button.is-plain.el-button--warning:hover),
:global(.light-clean) .app-container :deep(.el-button.is-plain.el-button--warning:focus) {
  background: #ba4a00 !important;
  border-color: #ba4a00 !important;
  color: #ffffff !important;
  box-shadow: 0 2px 6px rgba(186, 74, 0, 0.25) !important;
}

/* 删除按钮 (Danger) */
.app-container.light-clean :deep(.el-button.is-plain.el-button--danger),
:global(.light-clean) .app-container :deep(.el-button.is-plain.el-button--danger) {
  background: rgba(197, 34, 34, 0.08) !important;
  border: 1px solid rgba(197, 34, 34, 0.45) !important;
  color: #c52222 !important;
}
.app-container.light-clean :deep(.el-button.is-plain.el-button--danger:hover),
.app-container.light-clean :deep(.el-button.is-plain.el-button--danger:focus),
:global(.light-clean) .app-container :deep(.el-button.is-plain.el-button--danger:hover),
:global(.light-clean) .app-container :deep(.el-button.is-plain.el-button--danger:focus) {
  background: #c52222 !important;
  border-color: #c52222 !important;
  color: #ffffff !important;
  box-shadow: 0 2px 6px rgba(197, 34, 34, 0.25) !important;
}

/* 亮色下禁用状态 (Disabled) 优化：提高对比度，使其呈高辨识度柔和灰色而绝不隐形 */
.app-container.light-clean :deep(.el-button.is-plain.is-disabled),
:global(.light-clean) .app-container :deep(.el-button.is-plain.is-disabled) {
  background: #f5f5f5 !important;
  border-color: #d9d9d9 !important;
  color: #8c8c8c !important;
  cursor: not-allowed !important;
  box-shadow: none !important;
}


/* ==================== 2. 暗色/默认主题下的样式 ==================== */

/* 编辑按钮 (Primary) */
.app-container :deep(.el-button.is-plain.el-button--primary) {
  background: rgba(0, 179, 219, 0.08) !important;
  border: 1px solid rgba(0, 179, 219, 0.4) !important;
  color: #00b3db !important;
}
.app-container :deep(.el-button.is-plain.el-button--primary:hover),
.app-container :deep(.el-button.is-plain.el-button--primary:focus) {
  background: #00b3db !important;
  border-color: #00b3db !important;
  color: #ffffff !important;
  box-shadow: 0 0 10px rgba(0, 179, 219, 0.4) !important;
}

/* 启用按钮 (Success) */
.app-container :deep(.el-button.is-plain.el-button--success) {
  background: rgba(0, 255, 157, 0.08) !important;
  border: 1px solid rgba(0, 255, 157, 0.4) !important;
  color: #00ff9d !important;
}
.app-container :deep(.el-button.is-plain.el-button--success:hover),
.app-container :deep(.el-button.is-plain.el-button--success:focus) {
  background: #00ff9d !important;
  border-color: #00ff9d !important;
  color: #ffffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 157, 0.4) !important;
}

/* 停用按钮 (Warning) */
.app-container :deep(.el-button.is-plain.el-button--warning) {
  background: rgba(255, 189, 46, 0.08) !important;
  border: 1px solid rgba(255, 189, 46, 0.4) !important;
  color: #ffbd2e !important;
}
.app-container :deep(.el-button.is-plain.el-button--warning:hover),
.app-container :deep(.el-button.is-plain.el-button--warning:focus) {
  background: #ffbd2e !important;
  border-color: #ffbd2e !important;
  color: #ffffff !important;
  box-shadow: 0 0 10px rgba(255, 189, 46, 0.4) !important;
}

/* 删除按钮 (Danger) */
.app-container :deep(.el-button.is-plain.el-button--danger) {
  background: rgba(255, 60, 91, 0.08) !important;
  border: 1px solid rgba(255, 60, 91, 0.4) !important;
  color: #ff3c5b !important;
}
.app-container :deep(.el-button.is-plain.el-button--danger:hover),
.app-container :deep(.el-button.is-plain.el-button--danger:focus) {
  background: #ff3c5b !important;
  border-color: #ff3c5b !important;
  color: #ffffff !important;
  box-shadow: 0 0 10px rgba(255, 60, 91, 0.4) !important;
}

/* 默认禁用状态 (Disabled) 优化：具有良好辨识度但暗淡的灰色 */
.app-container :deep(.el-button.is-plain.is-disabled) {
  background: rgba(255, 255, 255, 0.03) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
  color: rgba(255, 255, 255, 0.28) !important;
  cursor: not-allowed !important;
  box-shadow: none !important;
}
</style>
