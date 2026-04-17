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
      <el-form-item label="适用装备" prop="equipmentType">
        <el-select v-model="queryParams.equipmentType" placeholder="请选择适用装备" clearable style="width: 200px">
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

    <!-- 操作按钮区域 -->
    <div class="toolbar-row mb8">
      <div class="toolbar-btns">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
      </div>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </div>

    <!-- 数据表格：各列 min-width 接近，剩余宽度由多列分摊，避免单侧大块留白 -->
    <el-table
      v-loading="loading"
      class="template-table"
      :data="templateList"
      table-layout="fixed"
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" align="center" />
      <el-table-column label="主键ID" prop="id" width="86" align="center" />
      <el-table-column label="模板编号" prop="templateCode" min-width="108" show-overflow-tooltip />
      <el-table-column label="模板名称" prop="templateName" min-width="132" show-overflow-tooltip />
      <el-table-column label="适用装备" prop="equipmentType" min-width="128" show-overflow-tooltip>
        <template #default="scope">
          {{ getBizTypeDisplayLabel(scope.row.equipmentType, scope.row.sceneType) }}
        </template>
      </el-table-column>
      <el-table-column label="节点角色" min-width="118" show-overflow-tooltip>
        <template #default="scope">
          {{ getNodeRoleLabelFromConfig(scope.row.configJson) }}
        </template>
      </el-table-column>
      <el-table-column label="工作模式" min-width="132" show-overflow-tooltip>
        <template #default="scope">
          {{ getWorkModeLabel(resolveTemplateWorkMode(scope.row)) }}
        </template>
      </el-table-column>
      <el-table-column label="创建人" prop="createBy" min-width="96" show-overflow-tooltip />
      <el-table-column label="最近更新" min-width="158" show-overflow-tooltip>
        <template #default="scope">
          {{ formatLatestTime(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="168" align="center" fixed="right">
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

    <!-- 新增/修改对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" class="indicator-template-dialog" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="indicator-template-form">
        <el-divider content-position="left">模板信息</el-divider>
        <el-row :gutter="20">
          <el-col v-if="form.id" :span="12">
            <el-form-item label="模板编号">
              <el-input v-model="form.templateCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="form.id ? 12 : 24">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="form.templateName" placeholder="请输入模板名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="适用装备" prop="equipmentType">
              <el-select v-model="form.equipmentType" placeholder="请选择适用装备" style="width: 100%">
                <el-option
                  v-for="item in indicatorTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板描述" prop="description">
              <el-input v-model="form.description" placeholder="请输入模板描述" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">指标默认配置</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="指标名称" prop="indicatorDraft.indicatorName">
              <el-input v-model="form.indicatorDraft.indicatorName" placeholder="留空则实例化时使用模板名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="节点角色" prop="indicatorDraft.nodeRole">
              <el-radio-group v-model="form.indicatorDraft.nodeRole">
                <el-radio-button :value="NODE_ROLE.NON_LEAF">非底层节点</el-radio-button>
                <el-radio-button :value="NODE_ROLE.LEAF">底层指标</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="工作模式">
              <el-radio-group v-model="form.indicatorDraft.workMode">
                <el-radio-button v-for="item in workModeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计量单位" prop="indicatorDraft.unit">
              <el-input v-model="form.indicatorDraft.unit" placeholder="如 %、m、s" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.indicatorDraft.nodeRole === NODE_ROLE.LEAF" :gutter="20">
          <el-col :span="12">
            <el-form-item label="指标值类型" prop="indicatorDraft.valueCategory">
              <el-select v-model="form.indicatorDraft.valueCategory" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in valueCategoryOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="门限范围">
              <div class="zhpg-threshold-range">
                <el-input-number v-model="form.indicatorDraft.valueMin" controls-position="right" class="zhpg-threshold-num" />
                <span class="zhpg-threshold-sep">至</span>
                <el-input-number v-model="form.indicatorDraft.valueMax" controls-position="right" class="zhpg-threshold-num" />
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="计算方法" prop="indicatorDraft.calcMethod">
          <div v-if="form.indicatorDraft.nodeRole !== NODE_ROLE.LEAF" class="calc-non-leaf-hint">非底层指标无需配置计算方法</div>
          <ZhpgCalcMethodFields
            v-else
            v-model="form.indicatorDraft.calcMethod"
            v-model:work-mode="form.indicatorDraft.workMode"
          />
        </el-form-item>
        <el-form-item label="指标说明" prop="indicatorDraft.indicatorDescription">
          <el-input v-model="form.indicatorDraft.indicatorDescription" type="textarea" :rows="2" placeholder="写入实例化后指标的描述（可与模板描述不同）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { listIndicatorTemplate, getIndicatorTemplate, addIndicatorTemplate, updateIndicatorTemplate, delIndicatorTemplate } from '@/api/zhpg/indicatorTemplate'
import ZhpgCalcMethodFields from '@/views/zhpg/components/ZhpgCalcMethodFields.vue'
import {
  getCalcMethodWorkMode,
  setCalcMethodWorkMode,
  validateCalcMethodString
} from '@/utils/zhpg/calcMethodAlgorithm'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ZHPG_VALUE_CATEGORY_OPTIONS } from '@/constants/zhpgIndicatorValueCategory'
import {
  ZHPG_EQUIPMENT_TYPE_OPTIONS
} from '@/constants/zhpgIndicatorSystem'
import { useDict } from '@/utils/dict'
import { ZHPG_WORK_MODE, ZHPG_WORK_MODE_OPTIONS, getZhpgWorkModeLabel } from '@/constants/zhpgWorkMode'

const NODE_ROLE = {
  NON_LEAF: 'NON_LEAF',
  LEAF: 'LEAF'
}

const valueCategoryOptions = ZHPG_VALUE_CATEGORY_OPTIONS
const workModeOptions = ZHPG_WORK_MODE_OPTIONS

/** 列表：configJson.indicator.nodeRole → 文案（兼容 ROOT / MIDDLE） */
function formatNodeRoleLabel(role) {
  if (role === 'NON_LEAF' || role === 'ROOT' || role === 'MIDDLE') return '非底层节点'
  if (role === 'LEAF') return '底层指标'
  return role || '—'
}

function getNodeRoleLabelFromConfig(configJson) {
  if (!configJson || !String(configJson).trim()) return '—'
  try {
    const o = JSON.parse(configJson)
    const role = o?.indicator?.nodeRole
    if (!role) return '—'
    return formatNodeRoleLabel(role)
  } catch {
    return '—'
  }
}

/** 展示创建时间、更新时间中较新的一个（后端 BaseEntity 通常带 updateTime） */
function formatLatestTime(row) {
  const c = row.createTime
  const u = row.updateTime
  if (!c && !u) return '—'
  if (!u) return c
  if (!c) return u
  return String(u) >= String(c) ? u : c
}

// 指标类型与体系类型选项
const { zhpg_equipment_type } = useDict('zhpg_equipment_type')
const indicatorTypeOptions = zhpg_equipment_type

/** 历史数据：旧版装备编码 → 6 类指标类型 */
function equipmentTypeToIndicatorType(eq) {
  const map = {
    SPACE_RECON: 'space_recon',
    SPACE_AWARENESS: 'space_domain_awareness',
    SPACE_AD: 'space_defense',
    SPACE_TTC: 'space_track_control',
    SPACE_LAUNCH: 'space_launch',
    SEA_BASED: 'sea_based_space',
    航天侦察: 'space_recon',
    太空态势感知: 'space_domain_awareness',
    太空攻防: 'space_defense',
    航天测运控: 'space_track_control',
    航天发射: 'space_launch',
    海基航天: 'sea_based_space',
    无: "无"
  }
  return map[eq]
}

function sceneTypeToIndicatorType(scene) {
  const map = {
    SPACE_RECON: 'space_recon',
    SPACE_SA: 'space_domain_awareness',
    SPACE_AD: 'space_defense',
    SPACE_TTC: 'space_track_control',
    SPACE_LAUNCH: 'space_launch',
    SEA_BASED: 'sea_based_space'
  }
  return map[scene]
}

function createEmptyIndicatorDraft() {
  return {
    indicatorName: '',
    nodeRole: NODE_ROLE.NON_LEAF,
    unit: '',
    valueCategory: undefined,
    valueMin: undefined,
    valueMax: undefined,
    workMode: ZHPG_WORK_MODE.INTERNAL_CIRCULATION,
    calcMethod: '',
    indicatorDescription: ''
  }
}

const loading = ref(false)
const showSearch = ref(true)
const templateList = ref([])
const total = ref(0)
const multiple = ref(true)
const ids = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  templateName: undefined,
  equipmentType: undefined
})

const form = reactive({
  id: undefined,
  templateCode: undefined,
  templateName: undefined,
  equipmentType: undefined,
  status: 'DRAFT',
  configJson: undefined,
  description: undefined,
  indicatorDraft: createEmptyIndicatorDraft()
})

const rules = reactive({
  templateName: [{ required: true, message: '模板名称不能为空', trigger: 'blur' }],
  equipmentType: [{ required: true, message: '请选择适用装备', trigger: 'change' }],
  'indicatorDraft.nodeRole': [{ required: true, message: '请选择节点角色', trigger: 'change' }]
})

const formRef = ref(null)
const queryRef = ref(null)

function getIndicatorTypeLabel(value) {
  const item = indicatorTypeOptions.find(i => i.value === value)
  return item ? item.label : value || '—'
}

/** 列表/详情：兼容旧库的 equipment_type / scene_type */
/** 列表/编辑回填：equipment_type 已为 6 类时直接用；否则按旧编码映射（历史数据） */
function normalizeRowBizType(equipmentType, legacySceneType) {
  if (equipmentType && indicatorTypeOptions.some(o => o.value === equipmentType)) {
    return equipmentType
  }
  const fromEq = equipmentTypeToIndicatorType(equipmentType)
  if (fromEq) return fromEq
  if (legacySceneType) {
    const fromScene = sceneTypeToIndicatorType(legacySceneType)
    if (fromScene) return fromScene
  }
  return equipmentType || ''
}

function getBizTypeDisplayLabel(equipmentType, legacySceneType) {
  const code = normalizeRowBizType(equipmentType, legacySceneType)
  return getIndicatorTypeLabel(code)
}

function resolveTemplateWorkMode(row) {
  if (row?.workMode) return row.workMode
  if (!row?.configJson || !String(row.configJson).trim()) {
    return ZHPG_WORK_MODE.INTERNAL_CIRCULATION
  }
  try {
    const o = JSON.parse(row.configJson)
    const indicator = o?.indicator || {}
    return indicator.workMode || getCalcMethodWorkMode(indicator.calcMethod || '')
  } catch {
    return ZHPG_WORK_MODE.INTERNAL_CIRCULATION
  }
}

function getWorkModeLabel(value) {
  return getZhpgWorkModeLabel(value)
}

function getList() {
  loading.value = true
  listIndicatorTemplate(queryParams).then(res => {
    templateList.value = res.rows
    total.value = res.total
  }).finally(() => {
    loading.value = false
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
  form.templateCode = undefined
  form.templateName = undefined
  form.equipmentType = undefined
  form.status = 'DRAFT'
  form.configJson = undefined
  form.description = undefined
  Object.assign(form.indicatorDraft, createEmptyIndicatorDraft())
  if (formRef.value) formRef.value.resetFields()
}

function parseConfigToDraft() {
  Object.assign(form.indicatorDraft, createEmptyIndicatorDraft())
  if (!form.configJson || !String(form.configJson).trim()) {
    return
  }
  try {
    const o = JSON.parse(form.configJson)
    if (o && o.indicator) {
      const ind = o.indicator
      form.indicatorDraft.indicatorName = ind.indicatorName || ''
      let draftRole = ind.nodeRole || NODE_ROLE.NON_LEAF
      if (draftRole === 'ROOT' || draftRole === 'MIDDLE') draftRole = NODE_ROLE.NON_LEAF
      form.indicatorDraft.nodeRole = draftRole
      form.indicatorDraft.unit = ind.unit || ''
      form.indicatorDraft.valueCategory = ind.valueCategory || undefined
      form.indicatorDraft.valueMin = ind.valueMin != null ? ind.valueMin : undefined
      form.indicatorDraft.valueMax = ind.valueMax != null ? ind.valueMax : undefined
      form.indicatorDraft.calcMethod = ind.calcMethod || ''
      form.indicatorDraft.workMode = ind.workMode || getCalcMethodWorkMode(ind.calcMethod || '')
      form.indicatorDraft.indicatorDescription = ind.description || ''
      if (ind.indicatorType) {
        form.equipmentType = ind.indicatorType
      }
    }
  } catch {
    // ignore
  }
}

function syncDraftToConfigJson() {
  const name = (form.indicatorDraft.indicatorName || '').trim()
  const isLeaf = form.indicatorDraft.nodeRole === NODE_ROLE.LEAF
  const ind = {
    indicatorName: name || undefined,
    indicatorType: form.equipmentType,
    nodeRole: form.indicatorDraft.nodeRole,
    workMode: form.indicatorDraft.workMode || ZHPG_WORK_MODE.INTERNAL_CIRCULATION,
    unit: form.indicatorDraft.unit || undefined,
    valueCategory: isLeaf ? (form.indicatorDraft.valueCategory || undefined) : undefined,
    valueMin: isLeaf && form.indicatorDraft.valueMin != null ? form.indicatorDraft.valueMin : undefined,
    valueMax: isLeaf && form.indicatorDraft.valueMax != null ? form.indicatorDraft.valueMax : undefined,
    calcMethod: isLeaf ? (setCalcMethodWorkMode(form.indicatorDraft.calcMethod || '', form.indicatorDraft.workMode) || undefined) : undefined,
    description: form.indicatorDraft.indicatorDescription || undefined
  }
  form.configJson = JSON.stringify({ schemaVersion: 1, indicator: ind })
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增指标模板'
  dialogVisible.value = true
}

function handleUpdate(row) {
  resetForm()
  const id = row.id || ids.value[0]
  getIndicatorTemplate(id).then(res => {
    const data = res.data
    form.id = data.id
    form.templateCode = data.templateCode
    form.templateName = data.templateName
    form.equipmentType = normalizeRowBizType(data.equipmentType, data.sceneType)
    form.status = data.status || 'DRAFT'
    form.description = data.description
    form.configJson = data.configJson
    parseConfigToDraft()
    form.indicatorDraft.workMode = data.workMode || form.indicatorDraft.workMode
    if (!form.equipmentType && form.configJson) {
      try {
        const o = JSON.parse(form.configJson)
        if (o?.indicator?.indicatorType) {
          form.equipmentType = o.indicator.indicatorType
        }
      } catch {
        // ignore
      }
    }
    dialogTitle.value = '修改指标模板'
    dialogVisible.value = true
  })
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    if (form.indicatorDraft.nodeRole === NODE_ROLE.LEAF && String(form.indicatorDraft.calcMethod || '').trim()) {
      const calcErr = validateCalcMethodString(form.indicatorDraft.calcMethod)
      if (calcErr) {
        ElMessage.warning(calcErr)
        return
      }
    }
    if (form.indicatorDraft.nodeRole !== NODE_ROLE.LEAF) {
      form.indicatorDraft.calcMethod = ''
      form.indicatorDraft.valueCategory = undefined
      form.indicatorDraft.valueMin = undefined
      form.indicatorDraft.valueMax = undefined
    } else {
      form.indicatorDraft.calcMethod = setCalcMethodWorkMode(form.indicatorDraft.calcMethod || '', form.indicatorDraft.workMode)
    }
    syncDraftToConfigJson()
    const payload = {
      templateName: form.templateName,
      equipmentType: form.equipmentType,
      workMode: form.indicatorDraft.workMode,
      status: form.status,
      description: form.description,
      configJson: form.configJson
    }
    if (form.id != null) {
      payload.id = form.id
      payload.templateCode = form.templateCode
      updateIndicatorTemplate(payload).then(() => {
        ElMessage.success('修改成功')
        dialogVisible.value = false
        getList()
      })
    } else {
      addIndicatorTemplate(payload).then(() => {
        ElMessage.success('新增成功')
        dialogVisible.value = false
        getList()
      })
    }
  })
}

function handleDelete(row) {
  const delIds = row.id ? [row.id] : ids.value
  ElMessageBox.confirm('是否确认删除所选的指标模板数据？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    return delIndicatorTemplate(delIds.join(','))
  }).then(() => {
    getList()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

watch(() => form.indicatorDraft.nodeRole, (role) => {
  if (role !== NODE_ROLE.LEAF) {
    form.indicatorDraft.calcMethod = ''
  }
})

getList()
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
.canvas-nodes--readonly {
  flex-wrap: wrap;
}
.canvas-nodes--compact .node-content--readonly {
  padding: 8px 12px;
  min-width: 88px;
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

.indicator-template-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.indicator-template-form :deep(.el-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>

<style>
.indicator-template-dialog.el-dialog {
  width: min(94vw, 1180px) !important;
  max-width: 1180px;
}

.indicator-template-dialog .el-dialog__body {
  max-height: calc(100vh - 170px);
  overflow: auto;
  padding: 14px 18px 10px;
}
</style>
