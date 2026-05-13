<template>
  <div class="app-container">
    <!-- Tab：指标 / 指标模板（共用同一份后端表，is_template 区分） -->
    <el-radio-group v-model="viewMode" class="view-mode-switch" @change="handleViewModeChange">
      <el-radio-button value="instance">指标</el-radio-button>
      <el-radio-button value="template">指标模板</el-radio-button>
    </el-radio-group>

    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item :label="isTemplateMode ? '模板名称' : '指标名称'" prop="indicatorName">
        <el-input
          v-model="queryParams.indicatorName"
          :placeholder="isTemplateMode ? '请输入模板名称' : '请输入指标名称'"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="装备类型" prop="indicatorType">
        <el-select v-model="queryParams.indicatorType" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="o in equipmentTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否启用" prop="isApplied">
        <el-select v-model="queryParams.isApplied" placeholder="全部" clearable style="width: 130px">
          <el-option label="启用" :value="1" />
          <el-option label="未启用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否底层" prop="isBottomNode">
        <el-select v-model="queryParams.isBottomNode" placeholder="全部" clearable style="width: 130px">
          <el-option label="底层指标" :value="true" />
          <el-option label="非底层" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item label="关联指标体系" prop="sourceSystemName" v-if="!isTemplateMode">
        <el-input
          v-model="queryParams.sourceSystemName"
          placeholder="请输入关联指标体系名称"
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

    <!-- 操作按钮 -->
    <div class="toolbar-row mb12">
      <div class="toolbar-btns">
        <el-button type="primary" icon="Plus" @click="handleAdd">{{ isTemplateMode ? '新增模板' : '新增指标' }}</el-button>
        <el-button v-if="!isTemplateMode" type="success" plain icon="DocumentCopy" @click="handleCreateFromTemplate">从模板创建</el-button>
        <el-button type="danger" plain icon="Delete" :disabled="!ids.length" @click="handleBatchDelete">批量删除</el-button>
      </div>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </div>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="tableData" table-layout="fixed" style="width: 100%" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="46" align="center" />
      <el-table-column label="ID" prop="id" width="80" align="center" />
      <el-table-column
        :label="isTemplateMode ? '模板名称' : '指标名称'"
        prop="indicatorName"
        min-width="200"
        show-overflow-tooltip
      />
      <el-table-column label="装备类型" prop="indicatorType" min-width="140" show-overflow-tooltip>
        <template #default="scope">{{ getZhpgEquipmentTypeLabel(scope.row.indicatorType) }}</template>
      </el-table-column>
      <el-table-column label="关联指标体系" prop="sourceSystemName" min-width="180" show-overflow-tooltip v-if="!isTemplateMode">
        <template #default="scope">
          <el-tag v-if="scope.row.sourceSystemName" size="small" effect="plain">{{ scope.row.sourceSystemName }}</el-tag>
          <span v-else class="text-secondary" style="font-size: 12px; color: #999;">—</span>
        </template>
      </el-table-column>
      <el-table-column label="是否底层" width="100" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.isBottomNode ? 'success' : 'info'" size="small">
            {{ scope.row.isBottomNode ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否启用" width="120" align="center">
        <template #default="scope">
          <el-switch
            :model-value="scope.row.isApplied === 1"
            @change="val => quickToggleApplied(scope.row, val)"
            inline-prompt
            active-text="启用"
            inactive-text="未启用"
          />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" min-width="160" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.updateTime || scope.row.createTime || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
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

    <!-- 新增 / 修改：单一表单（指标 + 模板共用） -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="min(720px, 95vw)"
      append-to-body
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="isTemplateMode ? '模板名称' : '指标名称'" prop="indicatorName">
              <el-input v-model="form.indicatorName" :placeholder="isTemplateMode ? '请输入模板名称' : '请输入指标名称'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="装备类型" prop="indicatorType">
              <el-select
                v-model="form.indicatorType"
                placeholder="请选择"
                style="width: 100%"
                @change="onIndicatorTypeChange"
              >
                <el-option v-for="o in equipmentTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="父级指标">
              <el-select
                v-model="form.parentId"
                filterable
                clearable
                placeholder="无（作为根节点）"
                :loading="parentLoading"
                style="width: 100%"
              >
                <el-option :value="0" label="无（根节点）" />
                <el-option
                  v-for="item in parentOptions"
                  :key="item.id"
                  :label="`${item.indicatorName}（L${item.level || 1}）`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否底层" prop="isBottomNode">
              <el-radio-group v-model="form.isBottomNode">
                <el-radio :value="true">底层指标</el-radio>
                <el-radio :value="false">非底层</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="是否启用">
              <el-switch
                v-model="form.isApplied"
                :active-value="1"
                :inactive-value="0"
                inline-prompt
                active-text="启用"
                inactive-text="未启用"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.orderNum" :min="0" :max="9999" :step="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <template v-if="form.isBottomNode">
          <el-divider content-position="left">底层指标 · 度量与算法</el-divider>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="计量单位">
                <el-input v-model="form.unit" placeholder="如 %、m、s" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="指标值类型">
                <el-select v-model="form.valueCategory" clearable placeholder="请选择" style="width: 100%">
                  <el-option v-for="o in valueCategoryOptions" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="取值下限">
                <el-input-number v-model="form.valueMin" :precision="4" :step="1" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="取值上限">
                <el-input-number v-model="form.valueMax" :precision="4" :step="1" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="计算方法">
            <ZhpgCalcMethodFields
              v-model="form.calcMethod"
              :leaf-uid="form.idCode || ('ind_' + (form.id || 'new'))"
              :show-work-mode-selector="false"
              :default-source-center="form.indicatorType !== '无' ? form.indicatorType : ''"
            />
          </el-form-item>
        </template>

        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 从模板创建 -->
    <el-dialog title="从模板创建指标" v-model="fromTemplateVisible" width="min(560px, 95vw)" append-to-body destroy-on-close>
      <el-form ref="fromTemplateRef" :model="fromTemplateForm" :rules="fromTemplateRules" label-width="100px">
        <el-form-item label="选择模板" prop="templateId">
          <el-select
            v-model="fromTemplateForm.templateId"
            filterable
            remote
            reserve-keyword
            :remote-method="searchTemplatesForCreate"
            :loading="fromTemplateSearchLoading"
            placeholder="输入模板名称检索"
            style="width: 100%"
            @change="onPickTemplateForCreate"
          >
            <el-option
              v-for="item in fromTemplateOptions"
              :key="item.id"
              :label="item.indicatorName"
              :value="item.id"
            >
              <span>{{ item.indicatorName }}</span>
              <span class="from-tpl-tag">{{ getZhpgEquipmentTypeLabel(item.indicatorType) }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="父级指标">
          <el-select
            v-model="fromTemplateForm.parentId"
            filterable
            clearable
            placeholder="无（作为根节点）"
            style="width: 100%"
            :disabled="!fromTemplateForm.templateId"
          >
            <el-option :value="0" label="无（根节点）" />
            <el-option
              v-for="item in fromTemplateParentOptions"
              :key="item.id"
              :label="`${item.indicatorName}（L${item.level || 1}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fromTemplateVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitFromTemplate">创 建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listIndicator,
  listParentIndicatorCandidates,
  getIndicator,
  addIndicator,
  updateIndicator,
  delIndicator,
  instantiateIndicatorFromTemplate
} from '@/api/zhpg/indicator'
import {
  ZHPG_EQUIPMENT_TYPE_OPTIONS,
  getZhpgEquipmentTypeLabel
} from '@/constants/zhpgIndicatorSystem'
import { ZHPG_VALUE_CATEGORY_OPTIONS } from '@/constants/zhpgIndicatorValueCategory'
import ZhpgCalcMethodFields from '@/views/zhpg/components/ZhpgCalcMethodFields.vue'

const equipmentTypeOptions = ZHPG_EQUIPMENT_TYPE_OPTIONS
const valueCategoryOptions = ZHPG_VALUE_CATEGORY_OPTIONS

const viewMode = ref('instance')
const isTemplateMode = computed(() => viewMode.value === 'template')

const loading = ref(false)
const showSearch = ref(true)
const tableData = ref([])
const total = ref(0)
const ids = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const queryRef = ref(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  indicatorName: undefined,
  indicatorType: undefined,
  isApplied: undefined,
  isBottomNode: undefined,
  sourceSystemName: undefined
})

const form = reactive({
  id: undefined,
  idCode: undefined,
  parentId: 0,
  indicatorName: undefined,
  indicatorType: undefined,
  unit: undefined,
  valueMin: undefined,
  valueMax: undefined,
  valueCategory: undefined,
  calcMethod: '',
  isBottomNode: true,
  isTemplate: 0,
  isApplied: 0,
  orderNum: 0,
  description: undefined,
  sourceSystemId: undefined,
  sourceSystemName: undefined
})

const rules = reactive({
  indicatorName: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
  indicatorType: [{ required: true, message: '请选择装备类型', trigger: 'change' }],
  isBottomNode: [{ required: true, message: '请选择是否为底层指标', trigger: 'change' }]
})

const parentOptions = ref([])
const parentLoading = ref(false)

const fromTemplateVisible = ref(false)
const fromTemplateRef = ref(null)
const fromTemplateOptions = ref([])
const fromTemplateSearchLoading = ref(false)
const fromTemplateParentOptions = ref([])
const fromTemplateForm = reactive({
  templateId: undefined,
  parentId: 0
})
const fromTemplateRules = reactive({
  templateId: [{ required: true, message: '请选择模板', trigger: 'change' }]
})

// ============== 列表加载 ==============
function getList() {
  loading.value = true
  listIndicator({
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
    indicatorName: queryParams.indicatorName || undefined,
    indicatorType: queryParams.indicatorType || undefined,
    isApplied: queryParams.isApplied,
    isBottomNode: queryParams.isBottomNode,
    sourceSystemName: queryParams.sourceSystemName || undefined,
    isTemplate: isTemplateMode.value ? 1 : 0
  })
    .then(res => {
      tableData.value = res.rows || []
      total.value = res.total || 0
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.indicatorName = undefined
  queryParams.indicatorType = undefined
  queryParams.isApplied = undefined
  queryParams.isBottomNode = undefined
  queryParams.sourceSystemName = undefined
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = (selection || []).map(it => it.id)
}

function handleViewModeChange() {
  resetQuery()
}

// ============== 父节点候选 ==============
async function loadParentCandidates(indicatorType, excludeId, isTemplate) {
  if (!indicatorType) {
    parentOptions.value = []
    return
  }
  parentLoading.value = true
  try {
    const res = await listParentIndicatorCandidates({
      indicatorType,
      excludeId,
      isTemplate,
      limit: 100
    })
    parentOptions.value = res.data || []
  } finally {
    parentLoading.value = false
  }
}

function onIndicatorTypeChange() {
  if (form.parentId && form.parentId !== 0) {
    const stillValid = parentOptions.value.find(p => p.id === form.parentId)
    if (!stillValid) form.parentId = 0
  }
  loadParentCandidates(form.indicatorType, form.id, isTemplateMode.value)
}

// ============== 新增 / 修改 ==============
function resetForm() {
  form.id = undefined
  form.idCode = undefined
  form.parentId = 0
  form.indicatorName = undefined
  form.indicatorType = undefined
  form.unit = undefined
  form.valueMin = undefined
  form.valueMax = undefined
  form.valueCategory = undefined
  form.calcMethod = ''
  form.isBottomNode = true
  form.isTemplate = isTemplateMode.value ? 1 : 0
  form.isApplied = 0
  form.orderNum = 0
  form.description = undefined
  form.sourceSystemId = undefined
  form.sourceSystemName = undefined
  parentOptions.value = []
}

function handleAdd() {
  resetForm()
  dialogTitle.value = isTemplateMode.value ? '新增指标模板' : '新增指标'
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate?.())
}

function handleUpdate(row) {
  resetForm()
  getIndicator(row.id).then(async res => {
    const d = res.data || {}
    Object.assign(form, {
      id: d.id,
      idCode: d.idCode,
      parentId: d.parentId == null ? 0 : d.parentId,
      indicatorName: d.indicatorName,
      indicatorType: d.indicatorType,
      unit: d.unit,
      valueMin: d.valueMin,
      valueMax: d.valueMax,
      valueCategory: d.valueCategory,
      calcMethod: d.calcMethod || '',
      isBottomNode: d.isBottomNode == null ? true : !!d.isBottomNode,
      isTemplate: d.isTemplate == null ? (isTemplateMode.value ? 1 : 0) : d.isTemplate,
      isApplied: d.isApplied == null ? 0 : d.isApplied,
      orderNum: d.orderNum == null ? 0 : d.orderNum,
      description: d.description,
      sourceSystemId: d.sourceSystemId,
      sourceSystemName: d.sourceSystemName
    })
    dialogTitle.value = isTemplateMode.value ? '修改指标模板' : '修改指标'
    dialogVisible.value = true
    await loadParentCandidates(form.indicatorType, form.id, isTemplateMode.value)
  })
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    const payload = { ...form }
    if (!payload.isBottomNode) {
      payload.unit = undefined
      payload.valueMin = undefined
      payload.valueMax = undefined
      payload.valueCategory = undefined
      payload.calcMethod = undefined
    }
    if (payload.parentId == null) payload.parentId = 0
    const promise = payload.id ? updateIndicator(payload) : addIndicator(payload)
    promise.then(() => {
      ElMessage.success(payload.id ? '修改成功' : '新增成功')
      dialogVisible.value = false
      getList()
    })
  })
}

// ============== 行内启用 / 停用 ==============
function quickToggleApplied(row, val) {
  const next = val ? 1 : 0
  updateIndicator({ ...row, isApplied: next }).then(() => {
    row.isApplied = next
    ElMessage.success(next ? '已启用' : '已停用')
  })
}

// ============== 删除 ==============
function handleDelete(row) {
  ElMessageBox.confirm(`确认删除「${row.indicatorName}」？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => delIndicator(row.id))
    .then(() => {
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

function handleBatchDelete() {
  if (!ids.value.length) return
  ElMessageBox.confirm(`确认删除选中的 ${ids.value.length} 条记录？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => delIndicator(ids.value.join(',')))
    .then(() => {
      ElMessage.success('删除成功')
      ids.value = []
      getList()
    })
    .catch(() => {})
}

// ============== 从模板创建 ==============
function handleCreateFromTemplate() {
  fromTemplateForm.templateId = undefined
  fromTemplateForm.parentId = 0
  fromTemplateOptions.value = []
  fromTemplateParentOptions.value = []
  fromTemplateVisible.value = true
  searchTemplatesForCreate('')
}

function searchTemplatesForCreate(keyword) {
  fromTemplateSearchLoading.value = true
  listIndicator({
    pageNum: 1,
    pageSize: 50,
    isTemplate: 1,
    indicatorName: keyword || undefined
  })
    .then(res => {
      fromTemplateOptions.value = res.rows || []
    })
    .finally(() => {
      fromTemplateSearchLoading.value = false
    })
}

function onPickTemplateForCreate(templateId) {
  fromTemplateForm.parentId = 0
  fromTemplateParentOptions.value = []
  const tpl = fromTemplateOptions.value.find(t => t.id === templateId)
  if (!tpl || !tpl.indicatorType) return
  // 父级候选限定为「指标实例」（is_template=0）
  listParentIndicatorCandidates({
    indicatorType: tpl.indicatorType,
    isTemplate: false,
    limit: 100
  }).then(res => {
    fromTemplateParentOptions.value = res.data || []
  })
}

function submitFromTemplate() {
  fromTemplateRef.value.validate(valid => {
    if (!valid) return
    instantiateIndicatorFromTemplate({
      templateId: fromTemplateForm.templateId,
      parentId: fromTemplateForm.parentId || 0
    }).then(res => {
      ElMessage.success('已基于模板生成指标')
      fromTemplateVisible.value = false
      // 切回指标 Tab 并刷新
      if (isTemplateMode.value) {
        viewMode.value = 'instance'
        resetQuery()
      } else {
        getList()
      }
      if (res?.data?.id) handleUpdate(res.data)
    })
  })
}

getList()
</script>

<style scoped>
.view-mode-switch {
  margin-bottom: 16px;
}
.toolbar-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.toolbar-btns {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.from-tpl-tag {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
:deep(.el-form--inline .el-form-item) {
  margin-right: 20px;
  margin-bottom: 16px;
}
</style>
