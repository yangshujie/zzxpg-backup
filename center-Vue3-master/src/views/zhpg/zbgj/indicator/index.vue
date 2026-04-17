<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="指标名称" prop="indicatorName">
        <el-input
          v-model="queryParams.indicatorName"
          placeholder="请输入指标名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="适用装备" prop="indicatorType">
        <el-select v-model="queryParams.indicatorType" placeholder="请选择适用装备" clearable style="width: 200px">
          <el-option
            v-for="item in indicatorTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮区域（与指标模板管理：toolbar-row + 右侧折叠查询） -->
    <div class="toolbar-row mb8">
      <div class="toolbar-btns">
        <el-button type="primary" plain icon="Plus" @click="handleOpenInstantiate">从模板新增</el-button>
        <el-button type="primary" plain icon="EditPen" @click="handleAdd">手工新增</el-button>
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
      </div>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </div>

    <!-- 数据表格：与模板页相同的 table-layout / 列宽策略 -->
    <el-table
      v-loading="loading"
      class="template-table"
      :data="indicatorList"
      row-key="id"
      table-layout="fixed"
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" align="center" />
      <el-table-column label="主键ID" prop="id" width="88" align="center" />
      <el-table-column label="指标名称" prop="indicatorName" min-width="200" show-overflow-tooltip />
      <el-table-column label="适用装备" prop="indicatorType" min-width="152" show-overflow-tooltip>
        <template #default="scope">
          {{ getIndicatorTypeLabel(scope.row.indicatorType) }}
        </template>
      </el-table-column>
      <el-table-column label="节点类型" min-width="120" show-overflow-tooltip>
        <template #default="scope">
          {{ getNodeTypeLabel(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column label="工作模式" min-width="120" show-overflow-tooltip>
        <template #default="scope">
          {{ getWorkModeLabel(resolveIndicatorWorkMode(scope.row)) }}
        </template>
      </el-table-column>
      <el-table-column label="创建人" prop="createBy" min-width="104" show-overflow-tooltip />
      <el-table-column label="最近更新" min-width="178" show-overflow-tooltip>
        <template #default="scope">
          {{ formatLatestTime(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="148" align="center" fixed="right">
        <template #default="scope">
          <div class="table-ops">
            <el-button link type="primary" size="small" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
            <el-button link type="danger" size="small" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新增/修改对话框（分区与宽度对齐指标模板管理） -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="720px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-divider content-position="left">指标信息</el-divider>
        <el-row :gutter="20">
          <el-col v-if="form.id" :span="12">
            <el-form-item label="主键ID">
              <el-input :model-value="String(form.id)" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="form.id ? 12 : 24">
            <el-form-item label="指标名称" prop="indicatorName">
              <el-input v-model="form.indicatorName" placeholder="请输入指标名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="适用装备" prop="indicatorType">
              <el-select
                v-model="form.indicatorType"
                :disabled="selectedParentInfo && selectedParentInfo.indicatorType && selectedParentInfo.indicatorType !== SYSTEM_AGGREGATION"
                :placeholder="form.nodeRole === NODE_ROLE.LEAF ? '底层指标必须选择具体装备' : '非叶节点可选填'"
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="item in availableIndicatorTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <div
                v-if="selectedParentInfo && selectedParentInfo.indicatorType && selectedParentInfo.indicatorType !== SYSTEM_AGGREGATION"
                class="relation-tip"
              >
                已由父节点锁定类型，如需更改请先调整父节点
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入指标描述" />
        </el-form-item>

        <el-divider content-position="left">层级与组合</el-divider>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="节点角色" prop="nodeRole">
              <el-radio-group v-model="form.nodeRole">
                <el-radio-button :value="NODE_ROLE.ROOT">根节点</el-radio-button>
                <el-radio-button :value="NODE_ROLE.MIDDLE">中间节点</el-radio-button>
                <el-radio-button :value="NODE_ROLE.LEAF">底层指标</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="工作模式">
              <el-radio-group v-model="form.workMode">
                <el-radio-button v-for="item in workModeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="组合关系" prop="parentId">
              <div style="width: 100%">
                <el-alert
                  v-if="form.nodeRole === NODE_ROLE.ROOT"
                  type="info"
                  :closable="false"
                  show-icon
                  title="根节点不挂载父级，作为组合关系起点。"
                />
                <template v-else>
                  <el-select
                    v-model="form.parentId"
                    :loading="parentLoading"
                    remote
                    filterable
                    reserve-keyword
                    :remote-method="handleParentSearch"
                    clearable
                    style="width: 100%"
                    placeholder="请选择同类型父节点（根节点/中间节点）"
                  >
                    <el-option
                      v-for="item in parentOptions"
                      :key="item.id"
                      :label="item.label"
                      :value="item.id"
                    />
                  </el-select>
                  <div class="relation-tip">
                    {{ form.nodeRole === NODE_ROLE.LEAF
                      ? '底层指标可挂到同类型父节点或无父节点下，选择后类型自动对齐'
                      : '中间节点可挂到同类型父节点或无父节点下，无节点下可汇聚不同装备类型的分支' }}
                  </div>
                </template>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">度量与算法</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="计量单位" prop="unit">
              <el-input v-model="form.unit" placeholder="如 %、m、s" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.isBottomNode" :span="12">
            <el-form-item label="指标值类型" prop="valueCategory">
              <el-select v-model="form.valueCategory" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in valueCategoryOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.isBottomNode" :gutter="20">
          <el-col :span="24">
            <el-form-item label="门限范围">
              <div class="zhpg-threshold-range">
                <el-input-number v-model="form.valueMin" controls-position="right" class="zhpg-threshold-num" />
                <span class="zhpg-threshold-sep">至</span>
                <el-input-number v-model="form.valueMax" controls-position="right" class="zhpg-threshold-num" />
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="计算方法" prop="calcMethod">
          <div v-if="!form.isBottomNode" class="calc-non-leaf-hint">非底层指标无需配置计算方法</div>
          <ZhpgCalcMethodFields
            v-else
            v-model="form.calcMethod"
            v-model:work-mode="form.workMode"
            :show-work-mode-selector="false"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 从模板新增 -->
    <el-dialog title="从模板新增指标" v-model="instantiateVisible" width="720px" append-to-body>
      <el-form ref="instantiateRef" :model="instantiateForm" :rules="instantiateRules" label-width="110px">
        <el-form-item label="选择模板" prop="templateId">
          <el-select
            v-model="instantiateForm.templateId"
            filterable
            remote
            reserve-keyword
            :remote-method="searchTemplates"
            :loading="templateLoading"
            style="width: 100%"
            placeholder="请输入模板名称检索"
          >
            <el-option
              v-for="item in templateOptions"
              :key="item.id"
              :label="item.templateName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="挂载父节点">
          <el-select
            v-model="instantiateForm.parentId"
            :loading="parentLoading"
            remote
            filterable
            reserve-keyword
            :remote-method="handleInstantiateParentSearch"
            clearable
            style="width: 100%"
            placeholder="不选则为根节点，可输入关键字搜索"
          >
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :label="item.label"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="instantiateVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitInstantiate">生 成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { listIndicator, listParentIndicatorCandidates, getIndicator, addIndicator, updateIndicator, delIndicator, instantiateIndicatorFromTemplate } from '@/api/zhpg/indicator'
import { listIndicatorTemplate } from '@/api/zhpg/indicatorTemplate'
import ZhpgCalcMethodFields from '@/views/zhpg/components/ZhpgCalcMethodFields.vue'
import {
  getCalcMethodWorkMode,
  setCalcMethodWorkMode,
  validateCalcMethodString
} from '@/utils/zhpg/calcMethodAlgorithm'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ZHPG_VALUE_CATEGORY_OPTIONS } from '@/constants/zhpgIndicatorValueCategory'
import { ZHPG_WORK_MODE, ZHPG_WORK_MODE_OPTIONS, getZhpgWorkModeLabel } from '@/constants/zhpgWorkMode'
import { useDict } from '@/utils/dict'

const { zhpg_equipment_type } = useDict('zhpg_equipment_type')

const NODE_ROLE = {
  ROOT: 'ROOT',
  MIDDLE: 'MIDDLE',
  LEAF: 'LEAF'
}

/** 无类型标识，父节点为此类型时子节点可跨装备类型 */
const SYSTEM_AGGREGATION = '无'

const indicatorTypeOptions = computed(() => {
  const options = zhpg_equipment_type.value || []
  return options.map(item => (
    item.value === SYSTEM_AGGREGATION
      ? { ...item, label: '无' }
      : item
  ))
})

const valueCategoryOptions = ZHPG_VALUE_CATEGORY_OPTIONS
const workModeOptions = ZHPG_WORK_MODE_OPTIONS

/**
 * 底层指标可选的装备类型：排除无（底层必须是具体装备，聚合是中间/根节点的概念）
 * 非底层节点保留全部选项
 */
const availableIndicatorTypeOptions = computed(() => {
  const all = indicatorTypeOptions.value
  if (form.nodeRole === NODE_ROLE.LEAF) {
    return all.filter(o => o.value !== SYSTEM_AGGREGATION)
  }
  return all
})

const loading = ref(false)
const showSearch = ref(true)
const indicatorList = ref([])
const parentOptions = ref([])
const total = ref(0)
const multiple = ref(true)
const ids = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const parentLoading = ref(false)
const instantiateVisible = ref(false)
const templateLoading = ref(false)
const templateOptions = ref([])
/** 当前已选父节点的信息（用于类型锁定约束） */
const selectedParentInfo = ref(null) // { indicatorType: string } | null

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  indicatorName: undefined,
  indicatorType: undefined
})

const form = reactive({
  id: undefined,
  idCode: undefined,
  indicatorName: undefined,
  indicatorType: undefined,
  nodeRole: NODE_ROLE.ROOT,
  parentId: 0,
  isBottomNode: false,
  unit: undefined,
  valueCategory: undefined,
  valueMin: undefined,
  valueMax: undefined,
  workMode: ZHPG_WORK_MODE.INTERNAL_CIRCULATION,
  calcMethod: undefined,
  description: undefined
})
const instantiateForm = reactive({
  templateId: undefined,
  parentId: undefined
})

const rules = reactive({
  indicatorName: [{ required: true, message: '指标名称不能为空', trigger: 'blur' }],
  indicatorType: [{
    validator: (_rule, value, callback) => {
      if (form.nodeRole === NODE_ROLE.LEAF && !value) {
        callback(new Error('底层指标必须选择适用装备'))
        return
      }
      if (form.nodeRole === NODE_ROLE.LEAF && value === SYSTEM_AGGREGATION) {
        callback(new Error('底层指标不能选无，请选择具体装备类型'))
        return
      }
      callback()
    },
    trigger: 'change'
  }],
  nodeRole: [{ required: true, message: '请选择节点角色', trigger: 'change' }],
  parentId: [{
    validator: (_rule, value, callback) => {
      if (form.nodeRole !== NODE_ROLE.ROOT && (value == null || value === 0)) {
        callback(new Error('请选择挂载父节点'))
        return
      }
      callback()
    },
    trigger: 'change'
  }]
})
const instantiateRules = reactive({
  templateId: [{ required: true, message: '请选择模板', trigger: 'change' }]
})

const formRef = ref(null)
const queryRef = ref(null)
const instantiateRef = ref(null)

function formatLatestTime(row) {
  const c = row.createTime
  const u = row.updateTime
  if (!c && !u) return '—'
  if (!u) return c
  if (!c) return u
  return String(u) >= String(c) ? u : c
}

function getList() {
  loading.value = true
  listIndicator(queryParams).then(res => {
    indicatorList.value = res.rows
    total.value = res.total
  }).finally(() => {
    loading.value = false
  })
}

function buildParentLabel(item) {
  const role = item.parentId === 0 ? '根节点' : '中间节点'
  const typeLabel = getIndicatorTypeLabel(item.indicatorType)
  return `${item.indicatorName}（${role}${typeLabel ? '·' + typeLabel : ''}）`
}

function loadParentCandidatesByType(indicatorType, keyword = '', excludeId) {
  if (!indicatorType) {
    parentOptions.value = []
    return Promise.resolve()
  }
  parentLoading.value = true
  const baseQuery = { keyword, excludeId, limit: 60 }
  // 具体装备类型：同时拉取同类型父节点 + 无父节点（聚合可作任意类型的父节点）
  const promises = [listParentIndicatorCandidates({ ...baseQuery, indicatorType })]
  if (indicatorType !== SYSTEM_AGGREGATION) {
    promises.push(listParentIndicatorCandidates({ ...baseQuery, indicatorType: SYSTEM_AGGREGATION }))
  }
  return Promise.all(promises).then(async ([res1, res2]) => {
    const toOption = item => ({
      id: item.id,
      label: buildParentLabel(item),
      indicatorType: item.indicatorType
    })
    const seen = new Set()
    const merged = []
    ;[...(res1.data || []), ...((res2 && res2.data) || [])].forEach(item => {
      if (!seen.has(item.id)) {
        seen.add(item.id)
        merged.push(toOption(item))
      }
    })
    parentOptions.value = merged
    const selectedParentId = instantiateVisible.value ? instantiateForm.parentId : form.parentId
    if (selectedParentId && selectedParentId !== 0 && !parentOptions.value.some(o => o.id === selectedParentId)) {
      try {
        const parentRes = await getIndicator(selectedParentId)
        const parent = parentRes.data
        if (parent && parent.isBottomNode !== true) {
          const isCompatible = parent.indicatorType === indicatorType || parent.indicatorType === SYSTEM_AGGREGATION
          if (isCompatible) {
            parentOptions.value.unshift(toOption(parent))
          }
        }
      } catch (e) {
        // no-op
      }
    }
  }).finally(() => {
    parentLoading.value = false
  })
}

function loadParentCandidates(keyword = '') {
  return loadParentCandidatesByType(form.indicatorType, keyword, form.id)
}

function handleParentSearch(keyword) {
  loadParentCandidates(keyword)
}

function handleInstantiateParentSearch(keyword) {
  const currentTemplate = templateOptions.value.find(item => item.id === instantiateForm.templateId)
  const indicatorType = resolveIndicatorTypeFromTemplate(currentTemplate)
  return loadParentCandidatesByType(indicatorType, keyword, undefined)
}

function resolveIndicatorTypeFromTemplate(template) {
  if (!template) return undefined
  const eq = template.equipmentType
  const list = indicatorTypeOptions.value || []
  if (eq && list.some(i => i.value === eq)) {
    return eq
  }
  const fallbackEquipment = {
    SPACE_RECON: 'space_recon',
    SPACE_SITUATIONAL_AWARENESS: 'space_domain_awareness',
    SPACE_OFFENSE_DEFENSE: 'space_defense',
    SPACE_TTC: 'space_track_control',
    SPACE_LAUNCH: 'space_launch',
    SEA_BASED_SPACE: 'sea_based_space',
    SYSTEM_AGGREGATION: '无',
    SPACE_AWARENESS: 'space_domain_awareness',
    SPACE_AD: 'space_defense',
    SEA_BASED: 'sea_based_space',
    SPACE_SA: 'space_domain_awareness',
    '航天侦察': 'space_recon',
    '太空态势感知': 'space_domain_awareness',
    '太空攻防': 'space_defense',
    '航天测运控': 'space_track_control',
    '航天发射': 'space_launch',
    '海基航天': 'sea_based_space',
    '无': '无'
  }
  if (eq && fallbackEquipment[eq]) {
    return fallbackEquipment[eq]
  }
  const sceneMap = {
    SPACE_RECON: 'space_recon',
    SPACE_SA: 'space_domain_awareness',
    SPACE_AD: 'space_defense',
    SPACE_TTC: 'space_track_control',
    SPACE_LAUNCH: 'space_launch',
    SEA_BASED: 'sea_based_space'
  }
  return sceneMap[template.sceneType]
}

function searchTemplates(keyword = '') {
  templateLoading.value = true
  return listIndicatorTemplate({
    pageNum: 1,
    pageSize: 50,
    templateName: keyword || undefined
  }).then(res => {
    templateOptions.value = res.rows || []
  }).finally(() => {
    templateLoading.value = false
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  if (queryRef.value) queryRef.value.resetFields()
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  multiple.value = !selection.length
}

function resetForm() {
  form.id = undefined
  form.idCode = undefined
  form.indicatorName = undefined
  form.indicatorType = undefined
  form.nodeRole = NODE_ROLE.ROOT
  form.parentId = 0
  form.isBottomNode = false
  form.unit = undefined
  form.valueCategory = undefined
  form.valueMin = undefined
  form.valueMax = undefined
  form.workMode = ZHPG_WORK_MODE.INTERNAL_CIRCULATION
  form.calcMethod = undefined
  form.description = undefined
  selectedParentInfo.value = null
  if (formRef.value) formRef.value.resetFields()
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增指标'
  dialogVisible.value = true
}

function handleOpenInstantiate() {
  instantiateForm.templateId = undefined
  instantiateForm.parentId = undefined
  templateOptions.value = []
  parentOptions.value = []
  instantiateVisible.value = true
  searchTemplates('')
}

function handleUpdate(row) {
  resetForm()
  const id = row.id || ids.value[0]
  getIndicator(id).then(res => {
    Object.assign(form, res.data)
    form.nodeRole = resolveNodeRole(form)
    form.workMode = form.workMode || getCalcMethodWorkMode(form.calcMethod || '')
    loadParentCandidates()
    dialogTitle.value = '修改指标'
    dialogVisible.value = true
  })
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    // 父子类型一致性：若父节点是具体装备类型，当前指标类型必须与之相同
    if (form.parentId && form.parentId !== 0 && selectedParentInfo.value) {
      const parentType = selectedParentInfo.value.indicatorType
      if (parentType && parentType !== SYSTEM_AGGREGATION && form.indicatorType !== parentType) {
        ElMessage.warning(`当前指标的适用装备须与父节点（${getIndicatorTypeLabel(parentType)}）保持一致`)
        return
      }
    }
    if (form.nodeRole === NODE_ROLE.ROOT) {
      form.parentId = 0
      form.isBottomNode = false
    } else if (form.nodeRole === NODE_ROLE.MIDDLE) {
      form.isBottomNode = false
    } else {
      form.isBottomNode = true
    }
    if (form.nodeRole !== NODE_ROLE.LEAF) {
      form.calcMethod = undefined
      form.valueCategory = undefined
      form.valueMin = undefined
      form.valueMax = undefined
    } else {
      form.calcMethod = setCalcMethodWorkMode(form.calcMethod || '', form.workMode)
    }
    if (form.nodeRole === NODE_ROLE.LEAF && String(form.calcMethod || '').trim()) {
      const calcErr = validateCalcMethodString(form.calcMethod)
      if (calcErr) {
        ElMessage.warning(calcErr)
        return
      }
    }
    delete form.weight
    if (form.id != null) {
      updateIndicator(form).then(() => {
        ElMessage.success('修改成功')
        dialogVisible.value = false
        getList()
      })
    } else {
      addIndicator(form).then(() => {
        ElMessage.success('新增成功')
        dialogVisible.value = false
        getList()
      })
    }
  })
}

function submitInstantiate() {
  instantiateRef.value.validate(valid => {
    if (!valid) return
    instantiateIndicatorFromTemplate({
      templateId: instantiateForm.templateId,
      parentId: instantiateForm.parentId || 0
    }).then(res => {
      ElMessage.success('已基于模板生成指标，进入二次编辑')
      instantiateVisible.value = false
      getList()
      if (res && res.data && res.data.id) {
        handleUpdate({ id: res.data.id })
      }
    })
  })
}

function handleDelete(row) {
  const delIds = row.id ? [row.id] : ids.value
  ElMessageBox.confirm('是否确认删除所选的评估指标数据？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    return delIndicator(delIds.join(','))
  }).then(() => {
    getList()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

function getIndicatorTypeLabel(value) {
  if (value == null || value === '') return ''
  const matched = indicatorTypeOptions.value.find(item => item.value === value)
  return matched?.label || value
}

function getNodeTypeLabel(row) {
  if (row?.isBottomNode === true) return '底层指标'
  return row?.parentId === 0 ? '根节点' : '中间节点'
}

function resolveIndicatorWorkMode(row) {
  if (row?.workMode) return row.workMode
  return getCalcMethodWorkMode(row?.calcMethod || '')
}

function getWorkModeLabel(value) {
  return getZhpgWorkModeLabel(value)
}

function resolveNodeRole(data) {
  if ((data.parentId == null || data.parentId === 0) && data.isBottomNode !== true) {
    return NODE_ROLE.ROOT
  }
  return data.isBottomNode === true ? NODE_ROLE.LEAF : NODE_ROLE.MIDDLE
}

watch(() => form.nodeRole, (role) => {
  if (role === NODE_ROLE.ROOT) {
    form.parentId = 0
    form.isBottomNode = false
    form.calcMethod = undefined
    parentOptions.value = []
    selectedParentInfo.value = null
    return
  }
  if (role === NODE_ROLE.MIDDLE) {
    form.isBottomNode = false
    form.calcMethod = undefined
  } else {
    // 切换到底层指标：若当前类型是无则清除（底层不能用聚合）
    form.isBottomNode = true
    if (form.indicatorType === SYSTEM_AGGREGATION) {
      form.indicatorType = undefined
    }
  }
  if (form.parentId === 0) {
    form.parentId = undefined
  }
  loadParentCandidates()
})

watch(() => form.parentId, (pid) => {
  if (!pid || pid === 0) {
    selectedParentInfo.value = null
    return
  }
  const found = parentOptions.value.find(o => o.id === pid)
  if (found) {
    selectedParentInfo.value = { indicatorType: found.indicatorType }
    // 若父节点是具体装备类型（非无），自动锁定当前指标类型与父节点一致
    if (found.indicatorType && found.indicatorType !== SYSTEM_AGGREGATION) {
      form.indicatorType = found.indicatorType
    }
  }
})

watch(() => form.indicatorType, () => {
  if (!dialogVisible.value) return
  if (form.nodeRole === NODE_ROLE.ROOT) return
  form.parentId = undefined
  loadParentCandidates()
})

watch(dialogVisible, (visible) => {
  if (!visible) return
  if (form.nodeRole !== NODE_ROLE.ROOT) {
    loadParentCandidates()
  }
})

watch(() => instantiateForm.templateId, (templateId) => {
  if (!instantiateVisible.value) return
  instantiateForm.parentId = undefined
  if (!templateId) {
    parentOptions.value = []
    return
  }
  const currentTemplate = templateOptions.value.find(item => item.id === templateId)
  const indicatorType = resolveIndicatorTypeFromTemplate(currentTemplate)
  loadParentCandidatesByType(indicatorType, '', undefined)
})

watch(() => form.isBottomNode, (val) => {
  if (!val) {
    form.calcMethod = undefined
    form.workMode = ZHPG_WORK_MODE.INTERNAL_CIRCULATION
  }
})

onMounted(() => {
  getList()
})
</script>

<style scoped>
.toolbar-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.toolbar-btns {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.template-table :deep(.el-table__cell) {
  vertical-align: middle;
}
.table-ops {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 6px 16px;
  padding: 6px 4px;
  line-height: 1.4;
}
.table-ops :deep(.el-button) {
  margin: 0;
}
.calc-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.calc-row--block {
  flex-direction: column;
  align-items: stretch;
}
.calc-row--block > .el-button {
  align-self: flex-start;
  margin-top: 8px;
}
.calc-preview-wrap {
  width: 100%;
  min-height: 48px;
  padding: 10px 12px;
  background: #f0f9eb;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}
.calc-placeholder,
.calc-fallback-text,
.calc-non-leaf-hint {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}
.calc-fallback-text {
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
}
.calc-flow-preview {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
}
.calc-flow-section {
  width: 100%;
}
.calc-flow-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.calc-flow-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.calc-flow-merge {
  text-align: center;
  font-size: 18px;
  color: #409eff;
  font-weight: bold;
  padding: 2px 0;
}
.calc-flow-merge .merge-text {
  font-size: 12px;
  font-weight: normal;
  color: #606266;
  margin-left: 4px;
}
.node-content--readonly {
  padding: 10px 14px;
}
.node-detail {
  margin-top: 4px;
  font-size: 12px;
  font-weight: normal;
  color: #606266;
  text-align: center;
  max-width: 160px;
  word-break: break-word;
}
.node-result .node-detail {
  display: none;
}

.canvas-node {
  display: flex;
  align-items: center;
}

.node-content {
  position: relative;
  padding: 10px 14px;
  border-radius: 6px;
  border: 2px solid;
  background: #fff;
  min-width: 100px;
  text-align: center;
}

.node-data .node-content {
  border-color: #67c23a;
  background: #f0f9eb;
}

.node-algorithm .node-content {
  border-color: #409eff;
  background: #ecf5ff;
}

.node-result .node-content {
  border-color: #e6a23c;
  background: #fdf6ec;
}

.node-label {
  font-weight: bold;
  font-size: 13px;
  display: block;
}

.node-arrow {
  font-size: 24px;
  color: #409eff;
  margin: 0 6px;
  font-weight: bold;
}

.relation-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.zhpg-threshold-range {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.zhpg-threshold-range .zhpg-threshold-num {
  flex: 1;
  min-width: 0;
}
.zhpg-threshold-sep {
  flex-shrink: 0;
  color: #909399;
  font-size: 13px;
}
</style>
