<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="110px">
      <el-form-item label="模板名称" prop="templateName">
        <el-input
          v-model="queryParams.templateName"
          placeholder="请输入模板名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="指标集类型" prop="systemType">
        <el-select v-model="queryParams.systemType" placeholder="一能三性" clearable style="width: 200px">
          <el-option v-for="item in systemTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="装备类型" prop="equipmentType">
        <el-select v-model="queryParams.equipmentType" placeholder="6类装备域" clearable style="width: 200px">
          <el-option v-for="item in equipmentTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
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

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="templateList" table-layout="fixed" style="width: 100%" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="46" align="center" />
      <el-table-column label="ID" prop="id" width="80" align="center" />
      <el-table-column label="模板编号" prop="templateCode" min-width="100" show-overflow-tooltip />
      <el-table-column label="模板名称" prop="templateName" min-width="180" show-overflow-tooltip />
      <el-table-column label="指标集类型" prop="systemType" min-width="130" show-overflow-tooltip>
        <template #default="scope">{{ getSystemTypeLabel(scope.row.systemType) }}</template>
      </el-table-column>
      <el-table-column label="装备类型" prop="equipmentType" min-width="120" show-overflow-tooltip>
        <template #default="scope">{{ getEquipmentTypeLabel(scope.row.equipmentType) }}</template>
      </el-table-column>
      <el-table-column label="工作模式" min-width="120" show-overflow-tooltip>
        <template #default="scope">{{ getWorkModeLabel(resolveTemplateWorkMode(scope.row)) }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" min-width="80" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
            {{ scope.row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建人" prop="createBy" min-width="90" show-overflow-tooltip />
      <el-table-column label="更新时间" min-width="160" show-overflow-tooltip>
        <template #default="scope">{{ formatLatestTime(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="scope">
          <div class="table-ops">
            <el-button link type="primary" size="small" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
            <el-button link type="primary" size="small" icon="View" @click="handlePreview(scope.row)">预览</el-button>
            <el-button link type="danger" size="small" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改对话框：全屏 + 指标树工作台（左树右图，图形化裁剪） -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      fullscreen
      append-to-body
      destroy-on-close
      class="zhpg-indicator-tree-dialog"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="zhpg-indicator-meta-form">
        <el-divider content-position="left">基本信息</el-divider>
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
            <el-form-item label="指标集类型" prop="systemType">
              <el-select v-model="form.systemType" placeholder="一能三性四类" style="width: 100%">
                <el-option v-for="item in systemTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="装备类型" prop="equipmentType">
              <el-select v-model="form.equipmentType" placeholder="6类装备之一" style="width: 100%">
                <el-option v-for="item in equipmentTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入模板描述" />
        </el-form-item>

        <el-divider content-position="left">全局默认</el-divider>
        <el-form-item label="工作模式">
          <el-radio-group v-model="form.workMode">
            <el-radio-button v-for="item in workModeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="赋权与传导">
          <div class="zhpg-global-algo-row">
            <el-button type="primary" plain @click="templateGlobalAlgoDialogVisible = true">配置模板全局</el-button>
            <el-tag size="small" type="info" effect="plain" class="zhpg-algo-tag">{{ templateGlobalWeightTag }}</el-tag>
            <el-tag size="small" type="info" effect="plain" class="zhpg-algo-tag">{{ templateGlobalConductionTag }}</el-tag>
          </div>
        </el-form-item>

        <el-divider content-position="left">指标树结构</el-divider>
      </el-form>
      <IndicatorTreeWorkbench
        v-model:tree-data="treeData"
        variant="template"
        split-height="calc(100vh - 280px)"
        :preferred-root-label="form.templateName"
        weight-tuning-mode="manual"
        :global-work-mode="form.workMode"
        @import-indicators="importTreeFromIndicators"
        @edit-node="editTreeNode"
      />
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 树节点编辑：叶节点配计算方法；非叶节点可单独覆盖全局默认的赋权与传导 -->
    <el-dialog title="编辑节点" v-model="nodeDialogVisible" class="zhpg-node-edit-dialog" append-to-body @closed="onNodeDialogClosed">
      <el-form ref="nodeFormRef" :model="nodeForm" :rules="nodeRules" label-width="96px" class="node-edit-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="节点名称" prop="label">
              <el-input v-model="nodeForm.label" placeholder="请输入节点名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="适用装备" prop="indicatorType">
              <el-select
                v-model="nodeForm.indicatorType"
                clearable
                style="width: 100%"
                :placeholder="nodeEditIsLeaf ? '底层节点必须选择具体装备' : '非叶节点可选填'"
              >
                <el-option
                  v-for="o in nodeEditIsLeaf ? equipmentTypeOptions.filter(o => o.value !== '无') : equipmentTypeOptions"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">度量与算法</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="计量单位">
              <el-input v-model="nodeForm.unit" placeholder="如 %、m、s" />
            </el-form-item>
          </el-col>
          <el-col v-if="nodeEditIsLeaf" :span="12">
            <el-form-item label="指标值类型">
              <el-select v-model="nodeForm.valueCategory" clearable placeholder="请选择" style="width: 100%">
                <el-option v-for="o in valueCategoryOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="nodeEditIsLeaf" :gutter="16">
          <el-col :span="12">
            <el-form-item label="门限范围">
              <div class="zhpg-threshold-range">
                <el-input-number v-model="nodeForm.valueMin" controls-position="right" class="zhpg-threshold-num" />
                <span class="zhpg-threshold-sep">至</span>
                <el-input-number v-model="nodeForm.valueMax" controls-position="right" class="zhpg-threshold-num" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="指标权重">
              <el-input-number
                v-model="nodeForm.weight"
                :min="0"
                :max="1"
                :step="0.05"
                :precision="4"
                controls-position="right"
                placeholder="可选 0~1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="nodeEditIsLeaf" label="计算方法">
          <ZhpgCalcMethodFields
            v-model="nodeCalcPayload"
            :leaf-uid="nodeForm.uid"
            v-model:work-mode="nodeForm.workMode"
          />
        </el-form-item>
        <template v-if="!nodeEditIsLeaf">
          <el-form-item label="赋权与传导">
            <div class="zhpg-global-algo-row">
              <el-button type="primary" plain @click="nodeOverrideDialogVisible = true">配置本节点</el-button>
              <el-tag size="small" type="info" effect="plain" class="zhpg-algo-tag">{{ nodeEditWeightTag }}</el-tag>
              <el-tag size="small" type="info" effect="plain" class="zhpg-algo-tag">{{ nodeEditConductionTag }}</el-tag>
            </div>
          </el-form-item>
        </template>
        <el-form-item label="描述">
          <el-input v-model="nodeForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nodeDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveNodeEdit">确 定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="模板全局 · 赋权与传导" v-model="templateGlobalAlgoDialogVisible" width="520px" append-to-body>
      <el-form label-width="120px">
        <el-form-item label="权重分配算法">
          <el-select v-model="templateAlgo.weightAssignAlgorithm" placeholder="未选" clearable style="width: 100%">
            <el-option v-for="item in weightAlgorithmOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="传导方法">
          <el-select v-model="templateAlgo.conductionAlgorithm" placeholder="未选" clearable style="width: 100%">
            <el-option v-for="item in conductionMethodOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="templateGlobalAlgoDialogVisible = false">完 成</el-button>
      </template>
    </el-dialog>

    <el-dialog title="本节点 · 赋权与传导" v-model="nodeOverrideDialogVisible" width="480px" append-to-body>
      <el-form label-width="120px">
        <el-form-item label="节点权重">
          <el-input-number
            v-model="nodeForm.weight"
            :min="0"
            :max="1"
            :step="0.05"
            :precision="4"
            controls-position="right"
            placeholder="可选"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="权重分配算法">
          <el-select v-model="nodeForm.weightAssignAlgorithm" clearable placeholder="未选则模板全局" style="width: 100%">
            <el-option v-for="item in weightAlgorithmOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="传导方法">
          <el-select v-model="nodeForm.conductionAlgorithm" clearable placeholder="未选则模板全局" style="width: 100%">
            <el-option v-for="item in conductionMethodOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clearNodeAlgoOverride">清除并采用全局默认</el-button>
        <el-button type="primary" @click="nodeOverrideDialogVisible = false">完 成</el-button>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog title="指标体系模板预览" v-model="previewVisible" width="800px" append-to-body>
      <div class="preview-info">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="模板名称">{{ previewData.templateName }}</el-descriptions-item>
          <el-descriptions-item label="指标集类型">{{ getSystemTypeLabel(previewData.systemType) }}</el-descriptions-item>
          <el-descriptions-item label="装备类型">{{ getEquipmentTypeLabel(previewData.equipmentType) }}</el-descriptions-item>
          <el-descriptions-item label="工作模式">{{ getWorkModeLabel(resolveTemplateWorkMode(previewData)) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ previewData.status === 'PUBLISHED' ? '已发布' : '草稿' }}</el-descriptions-item>
          <el-descriptions-item label="全局默认·权重分配">{{ previewWeightAssignLabel }}</el-descriptions-item>
          <el-descriptions-item label="全局默认·传导方法">{{ previewConductionLabel }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ previewData.description || '—' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div class="preview-tree-wrap" style="margin-top: 16px">
        <div class="preview-tree-title">指标树结构</div>
        <el-tree
          :data="previewTreeData"
          :props="{ label: 'label', children: 'children' }"
          default-expand-all
          :expand-on-click-node="false"
          class="indicator-tree"
        >
          <template #default="{ data }">
            <span class="preview-tree-node">
              <span>{{ data.label }}</span>
              <span v-if="data.unit" class="preview-unit">{{ data.unit }}</span>
              <span v-if="templateNodeIsLeaf(data) && data.valueCategory" class="preview-vcat">{{
                getZhpgValueCategoryLabel(data.valueCategory)
              }}</span>
              <span
                v-if="templateNodeIsLeaf(data) && leafHasCalcConfig(data)"
                class="preview-calc"
              >已配算法</span>
            </span>
          </template>
        </el-tree>
        <div v-if="previewTreeData.length === 0" class="tree-empty-hint">该模板暂无指标树结构</div>
      </div>
    </el-dialog>

    <!-- 从指标库导入对话框 -->
    <el-dialog title="从指标库导入" v-model="importVisible" width="600px" append-to-body>
      <el-alert type="info" :closable="false" show-icon title="选择根指标后，将自动导入该指标及其所有子指标形成指标树" style="margin-bottom: 12px" />
      <el-form label-width="100px">
        <el-form-item label="指标类型">
          <el-select v-model="importIndicatorType" placeholder="请先选择指标类型" clearable style="width: 100%" @change="loadIndicatorRoots">
            <el-option v-for="item in equipmentTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="根指标">
          <el-select v-model="importRootId" filterable placeholder="请选择根指标" :loading="importLoading" style="width: 100%">
            <el-option v-for="item in importRootOptions" :key="item.id" :label="item.indicatorName" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importVisible = false">取 消</el-button>
        <el-button type="primary" @click="doImportFromIndicators" :disabled="!importRootId">导 入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import {
  listIndicatorSystemTemplate,
  getIndicatorSystemTemplate,
  addIndicatorSystemTemplate,
  updateIndicatorSystemTemplate,
  delIndicatorSystemTemplate
} from '@/api/zhpg/indicatorSystemTemplate'
import { listAllIndicator, getIndicatorTree } from '@/api/zhpg/indicator'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ZHPG_SYSTEM_TYPE_OPTIONS,
  ZHPG_EQUIPMENT_TYPE_OPTIONS,
  getZhpgSystemTypeLabel,
  getZhpgEquipmentTypeLabel
} from '@/constants/zhpgIndicatorSystem'
import { ZHPG_VALUE_CATEGORY_OPTIONS, getZhpgValueCategoryLabel } from '@/constants/zhpgIndicatorValueCategory'
import {
  ZHPG_WEIGHT_ASSIGN_OPTIONS,
  getZhpgWeightAssignLabel,
  getZhpgConductionMethodLabel,
  coerceConductionMethod
} from '@/constants/zhpgIndicatorSystemAlgorithms'
import { fetchZhpgConductionMethodSelectOptions } from '@/utils/zhpgConductionAlgorithms'
import IndicatorTreeWorkbench from '@/views/zhpg/components/IndicatorTreeWorkbench.vue'
import ZhpgCalcMethodFields from '@/views/zhpg/components/ZhpgCalcMethodFields.vue'
import {
  getCalcMethodWorkMode,
  setCalcMethodWorkMode,
  validateCalcMethodString,
  leafHasCalcConfig
} from '@/utils/zhpg/calcMethodAlgorithm'
import { parseIndicatorTreeToForest, serializeForestToIndicatorTree } from '@/utils/zhpgIndicatorTreeJson'
import {
  ZHPG_WORK_MODE,
  ZHPG_WORK_MODE_OPTIONS,
  getZhpgWorkModeLabel,
  normalizeZhpgWorkMode
} from '@/constants/zhpgWorkMode'

const systemTypeOptions = ZHPG_SYSTEM_TYPE_OPTIONS
const equipmentTypeOptions = ZHPG_EQUIPMENT_TYPE_OPTIONS
const valueCategoryOptions = ZHPG_VALUE_CATEGORY_OPTIONS
const weightAlgorithmOptions = ZHPG_WEIGHT_ASSIGN_OPTIONS
const conductionMethodOptions = ref([])
const workModeOptions = ZHPG_WORK_MODE_OPTIONS

function getSystemTypeLabel(val) {
  return getZhpgSystemTypeLabel(val)
}

function getEquipmentTypeLabel(val) {
  return getZhpgEquipmentTypeLabel(val)
}

function formatLatestTime(row) {
  const c = row.createTime
  const u = row.updateTime
  if (!c && !u) return '—'
  if (!u) return c
  if (!c) return u
  return String(u) >= String(c) ? u : c
}

function resolveTemplateWorkMode(row) {
  if (row?.workMode) return normalizeZhpgWorkMode(row.workMode)
  try {
    const tree = parseTreeJson(row?.indicatorTree)
    return normalizeZhpgWorkMode(tree?.[0]?.workMode)
  } catch {
    return ZHPG_WORK_MODE.INTERNAL_CIRCULATION
  }
}

function getWorkModeLabel(value) {
  return getZhpgWorkModeLabel(value)
}

let uidCounter = 0
function genUid() {
  return 'node_' + (++uidCounter) + '_' + Date.now()
}

const loading = ref(false)
const showSearch = ref(true)
const templateList = ref([])
const total = ref(0)
const multiple = ref(true)
const ids = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const previewVisible = ref(false)
const previewData = ref({})
const previewTreeData = ref([])

const previewWeightAssignLabel = computed(() => {
  const j = previewData.value?.configJson
  if (!j || !String(j).trim()) return '—'
  try {
    const o = JSON.parse(j)
    return getZhpgWeightAssignLabel(o.weightAssignAlgorithm)
  } catch {
    return '—'
  }
})

const previewConductionLabel = computed(() => {
  const j = previewData.value?.configJson
  if (!j || !String(j).trim()) return '—'
  try {
    const o = JSON.parse(j)
    const code = o.conductionAlgorithm || coerceConductionMethod(o.globalConduction)
    return getZhpgConductionMethodLabel(code, conductionMethodOptions.value)
  } catch {
    return '—'
  }
})

const nodeDialogVisible = ref(false)
const templateGlobalAlgoDialogVisible = ref(false)
const nodeOverrideDialogVisible = ref(false)
const importVisible = ref(false)
const importLoading = ref(false)
const importIndicatorType = ref('')
const importRootId = ref(null)
const importRootOptions = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  templateName: undefined,
  systemType: undefined,
  equipmentType: undefined
})

const form = reactive({
  id: undefined,
  templateCode: undefined,
  templateName: undefined,
  systemType: undefined,
  equipmentType: undefined,
  workMode: ZHPG_WORK_MODE.INTERNAL_CIRCULATION,
  description: undefined,
  indicatorTree: undefined,
  configJson: undefined,
  status: 'DRAFT'
})

/** 写入 configJson：与从模板生成指标体系时后端解析字段一致 */
const templateAlgo = reactive({
  weightAssignAlgorithm: undefined,
  conductionAlgorithm: undefined
})

function resetTemplateAlgoFields() {
  templateAlgo.weightAssignAlgorithm = undefined
  templateAlgo.conductionAlgorithm = undefined
}

function parseTemplateAlgoFromConfigJson() {
  resetTemplateAlgoFields()
  if (!form.configJson || !String(form.configJson).trim()) return
  try {
    const o = JSON.parse(form.configJson)
    if (o.weightAssignAlgorithm) templateAlgo.weightAssignAlgorithm = o.weightAssignAlgorithm
    templateAlgo.conductionAlgorithm =
      o.conductionAlgorithm || coerceConductionMethod(o.globalConduction) || undefined
  } catch {
    // 保留默认
  }
}

function buildTemplateAlgoConfigJson() {
  const o = {
    schemaVersion: 1,
    weightAssignAlgorithm: templateAlgo.weightAssignAlgorithm || undefined,
    conductionAlgorithm: templateAlgo.conductionAlgorithm || undefined
  }
  form.configJson = JSON.stringify(o)
}

const treeData = ref([])

/** 指标树根节点（多根时取第一个）与模板名称一致 */
function syncTreeRootToTemplateName() {
  const name = (form.templateName || '').trim()
  if (!name || !treeData.value?.length) return
  const root = treeData.value[0]
  if (root) root.label = name
}

function syncTreeRootWorkMode() {
  if (!treeData.value?.length) return
  const root = treeData.value[0]
  if (root) root.workMode = form.workMode
}

function applyWorkModeToLeafNodes(nodes, workMode) {
  for (const node of nodes || []) {
    const isLeaf = !node.children || node.children.length === 0
    if (isLeaf) {
      if (node.computeRule && typeof node.computeRule === 'object') {
        node.computeRule = setCalcMethodWorkMode(node.computeRule, workMode)
      } else {
        const next = setCalcMethodWorkMode(node.calcMethod != null ? String(node.calcMethod) : '', workMode)
        if (next && typeof next === 'object') {
          node.computeRule = next
          delete node.calcMethod
        } else {
          node.calcMethod = next || undefined
          delete node.computeRule
        }
      }
      node.workMode = workMode
    } else if (node.children?.length) {
      applyWorkModeToLeafNodes(node.children, workMode)
    }
  }
}

watch(
  () => [dialogVisible.value, (form.templateName || '').trim()],
  ([open, name]) => {
    if (!open || !name || !treeData.value?.length) return
    const root = treeData.value[0]
    if (root) root.label = name
  }
)

const nodeForm = reactive({
  uid: '',
  label: '',
  indicatorType: undefined,
  unit: '',
  description: '',
  valueCategory: undefined,
  valueMin: undefined,
  valueMax: undefined,
  workMode: ZHPG_WORK_MODE.INTERNAL_CIRCULATION,
  calcMethod: '',
  computeRule: undefined,
  weight: undefined,
  weightAssignAlgorithm: undefined,
  conductionAlgorithm: undefined
})
const nodeRules = { label: [{ required: true, message: '节点名称不能为空', trigger: 'blur' }] }
const editingNodeRef = ref(null)

/** 模板树中叶节点 = 无子节点，对应指标侧的底层指标，需可配置计算方法 */
function templateNodeIsLeaf(node) {
  return node && (!node.children || node.children.length === 0)
}

const nodeEditIsLeaf = computed(() => templateNodeIsLeaf(editingNodeRef.value))

const nodeCalcPayload = computed({
  get() {
    if (nodeForm.computeRule && typeof nodeForm.computeRule === 'object') return nodeForm.computeRule
    return nodeForm.calcMethod || ''
  },
  set(v) {
    if (v == null) {
      nodeForm.computeRule = undefined
      nodeForm.calcMethod = ''
      return
    }
    if (typeof v === 'object') {
      nodeForm.computeRule = v
      nodeForm.calcMethod = ''
    } else {
      nodeForm.calcMethod = String(v)
      nodeForm.computeRule = undefined
    }
  }
})

const templateGlobalWeightTag = computed(() => getZhpgWeightAssignLabel(templateAlgo.weightAssignAlgorithm))
const templateGlobalConductionTag = computed(() =>
  getZhpgConductionMethodLabel(templateAlgo.conductionAlgorithm, conductionMethodOptions.value)
)

const nodeEditWeightTag = computed(() =>
  nodeForm.weightAssignAlgorithm ? getZhpgWeightAssignLabel(nodeForm.weightAssignAlgorithm) : '权重·模板全局'
)
const nodeEditConductionTag = computed(() =>
  nodeForm.conductionAlgorithm
    ? getZhpgConductionMethodLabel(nodeForm.conductionAlgorithm, conductionMethodOptions.value)
    : '传导·模板全局'
)

const rules = reactive({
  templateName: [{ required: true, message: '模板名称不能为空', trigger: 'blur' }],
  systemType: [{ required: true, message: '请选择指标集类型（一能三性）', trigger: 'change' }]
})

const formRef = ref(null)
const queryRef = ref(null)
const nodeFormRef = ref(null)

function getList() {
  loading.value = true
  listIndicatorSystemTemplate(queryParams).then(res => {
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
  form.systemType = undefined
  form.equipmentType = undefined
  form.workMode = ZHPG_WORK_MODE.INTERNAL_CIRCULATION
  form.description = undefined
  form.indicatorTree = undefined
  form.configJson = undefined
  form.status = 'DRAFT'
  resetTemplateAlgoFields()
  treeData.value = []
  if (formRef.value) formRef.value.resetFields()
}

function parseTreeJson(json) {
  return parseIndicatorTreeToForest(json)
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增指标体系模板'
  dialogVisible.value = true
}

function handleUpdate(row) {
  resetForm()
  getIndicatorSystemTemplate(row.id).then(res => {
    const data = res.data
    form.id = data.id
    form.templateCode = data.templateCode
    form.templateName = data.templateName
    form.systemType = data.systemType
    form.equipmentType = data.equipmentType
    form.workMode = normalizeZhpgWorkMode(data.workMode)
    form.description = data.description
    form.indicatorTree = data.indicatorTree
    form.configJson = data.configJson
    form.status = data.status || 'DRAFT'
    parseTemplateAlgoFromConfigJson()
    treeData.value = parseTreeJson(data.indicatorTree)
    if (data.indicatorTree && !treeData.value.length) {
      ElMessage.warning('指标树数据解析失败，当前库内 indicator_tree 不是合法 JSON，请检查导入数据')
    }
    form.workMode = normalizeZhpgWorkMode(data.workMode || treeData.value?.[0]?.workMode)
    syncTreeRootToTemplateName()
    syncTreeRootWorkMode()
    dialogTitle.value = '修改指标体系模板'
    dialogVisible.value = true
  })
}

function handlePreview(row) {
  getIndicatorSystemTemplate(row.id).then(res => {
    previewData.value = res.data
    previewTreeData.value = parseTreeJson(res.data.indicatorTree)
    previewVisible.value = true
  })
}

const SYSTEM_AGGREGATION = '无'

function validateTreeCalcMethods(nodes) {
  for (const n of nodes || []) {
    const isLeaf = !n.children || n.children.length === 0
    if (isLeaf && leafHasCalcConfig(n)) {
      const err = validateCalcMethodString(n.computeRule || n.calcMethod)
      if (err) return err
    }
    if (n.children?.length) {
      const nested = validateTreeCalcMethods(n.children)
      if (nested) return nested
    }
  }
  return null
}

function validateTreeIndicatorTypes(nodes) {
  for (const n of nodes || []) {
    const isLeaf = !n.children || n.children.length === 0
    const nodeName = n.label || '未命名节点'
    if (isLeaf) {
      if (!n.indicatorType) {
        return `底层指标「${nodeName}」未设置适用装备，请在节点编辑中选择装备类型`
      }
      if (n.indicatorType === SYSTEM_AGGREGATION) {
        return `底层指标「${nodeName}」不能设为无，请选择具体装备类型`
      }
    } else if (n.indicatorType && n.indicatorType !== SYSTEM_AGGREGATION) {
      for (const child of n.children) {
        if (child.indicatorType && child.indicatorType !== n.indicatorType) {
          return `节点「${nodeName}」（${getEquipmentTypeLabel(n.indicatorType)}）的子节点「${child.label || '未命名'}」装备类型（${getEquipmentTypeLabel(child.indicatorType)}）与父节点不一致`
        }
      }
    }
    if (n.children?.length) {
      const nested = validateTreeIndicatorTypes(n.children)
      if (nested) return nested
    }
  }
  return null
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    const calcErr = validateTreeCalcMethods(treeData.value)
    if (calcErr) {
      ElMessage.warning(calcErr)
      return
    }
    const typeErr = validateTreeIndicatorTypes(treeData.value)
    if (typeErr) {
      ElMessage.warning(typeErr)
      return
    }
    buildTemplateAlgoConfigJson()
    applyWorkModeToLeafNodes(treeData.value, form.workMode)
    syncTreeRootToTemplateName()
    syncTreeRootWorkMode()
    form.indicatorTree = serializeForestToIndicatorTree(treeData.value, {
      systemName: form.templateName,
      workMode: form.workMode
    })
    const payload = {
      templateName: form.templateName,
      systemType: form.systemType,
      equipmentType: form.equipmentType,
      workMode: form.workMode,
      description: form.description,
      indicatorTree: form.indicatorTree,
      configJson: form.configJson,
      status: form.status
    }
    if (form.id != null) {
      payload.id = form.id
      payload.templateCode = form.templateCode
      updateIndicatorSystemTemplate(payload).then(() => {
        ElMessage.success('修改成功')
        dialogVisible.value = false
        getList()
      })
    } else {
      addIndicatorSystemTemplate(payload).then(() => {
        ElMessage.success('新增成功')
        dialogVisible.value = false
        getList()
      })
    }
  })
}

function handleDelete(row) {
  const delIds = row.id ? [row.id] : ids.value
  ElMessageBox.confirm('是否确认删除所选的指标体系模板？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    return delIndicatorSystemTemplate(delIds.join(','))
  }).then(() => {
    getList()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

function editTreeNode(data) {
  editingNodeRef.value = data
  nodeForm.uid = data.uid
  nodeForm.label = data.label
  nodeForm.indicatorType = data.indicatorType || undefined
  nodeForm.unit = data.unit || ''
  nodeForm.description = data.description || ''
  nodeForm.valueCategory = data.valueCategory || undefined
  nodeForm.valueMin = data.valueMin != null ? data.valueMin : undefined
  nodeForm.valueMax = data.valueMax != null ? data.valueMax : undefined
  if (data.computeRule && typeof data.computeRule === 'object') {
    nodeForm.computeRule = data.computeRule
    nodeForm.calcMethod = ''
  } else {
    nodeForm.calcMethod = data.calcMethod != null ? String(data.calcMethod) : ''
    nodeForm.computeRule = undefined
  }
  nodeForm.workMode = normalizeZhpgWorkMode(
    data.workMode || getCalcMethodWorkMode(nodeForm.calcMethod || nodeForm.computeRule, form.workMode),
    form.workMode
  )
  nodeForm.weight = data.weight
  nodeForm.weightAssignAlgorithm = data.weightAssignAlgorithm
  nodeForm.conductionAlgorithm = coerceConductionMethod(data.conductionAlgorithm) || undefined
  nodeDialogVisible.value = true
}

function onNodeDialogClosed() {
  editingNodeRef.value = null
  nodeOverrideDialogVisible.value = false
}

function clearNodeAlgoOverride() {
  nodeForm.weightAssignAlgorithm = undefined
  nodeForm.conductionAlgorithm = undefined
  nodeForm.weight = undefined
  ElMessage.success('已清除本节点覆盖')
}

function saveNodeEdit() {
  nodeFormRef.value.validate(valid => {
    if (!valid) return
    const target = editingNodeRef.value
    if (!target) return
    const isLeaf = templateNodeIsLeaf(target)
    if (isLeaf && leafHasCalcConfig({ calcMethod: nodeForm.calcMethod, computeRule: nodeForm.computeRule })) {
      const calcErr = validateCalcMethodString(nodeForm.computeRule || nodeForm.calcMethod)
      if (calcErr) {
        ElMessage.warning(calcErr)
        return
      }
    }
    target.label = nodeForm.label
    target.indicatorType = nodeForm.indicatorType || undefined
    target.unit = nodeForm.unit
    target.description = nodeForm.description
    target.weight = nodeForm.weight
    if (isLeaf) {
      if (nodeForm.computeRule && typeof nodeForm.computeRule === 'object') {
        target.computeRule = setCalcMethodWorkMode(nodeForm.computeRule, nodeForm.workMode)
        delete target.calcMethod
      } else {
        const next = setCalcMethodWorkMode(nodeForm.calcMethod || '', nodeForm.workMode)
        if (next && typeof next === 'object') {
          target.computeRule = next
          delete target.calcMethod
        } else {
          target.calcMethod = next || undefined
          delete target.computeRule
        }
      }
      target.workMode = nodeForm.workMode
      target.valueCategory = nodeForm.valueCategory || undefined
      target.valueMin = nodeForm.valueMin != null ? nodeForm.valueMin : undefined
      target.valueMax = nodeForm.valueMax != null ? nodeForm.valueMax : undefined
      delete target.weightAssignAlgorithm
      delete target.conductionAlgorithm
    } else {
      delete target.calcMethod
      delete target.computeRule
      delete target.workMode
      delete target.valueCategory
      delete target.valueMin
      delete target.valueMax
      nodeForm.workMode = ZHPG_WORK_MODE.INTERNAL_CIRCULATION
      target.weightAssignAlgorithm = nodeForm.weightAssignAlgorithm || undefined
      target.conductionAlgorithm = nodeForm.conductionAlgorithm || undefined
    }
    nodeDialogVisible.value = false
  })
}

// 从指标库导入
function importTreeFromIndicators() {
  importIndicatorType.value = form.equipmentType || ''
  importRootId.value = null
  importRootOptions.value = []
  importVisible.value = true
  if (importIndicatorType.value) {
    loadIndicatorRoots()
  }
}

function loadIndicatorRoots() {
  if (!importIndicatorType.value) {
    importRootOptions.value = []
    return
  }
  importLoading.value = true
  listAllIndicator({ indicatorType: importIndicatorType.value, parentId: 0 }).then(res => {
    importRootOptions.value = res.data || []
  }).finally(() => {
    importLoading.value = false
  })
}

function doImportFromIndicators() {
  if (!importRootId.value) return
  getIndicatorTree({ rootId: importRootId.value }).then(res => {
    const tree = res.data
    if (tree) {
      const converted = convertIndicatorToTreeNode(tree)
      treeData.value = [converted]
      applyWorkModeToLeafNodes(treeData.value, form.workMode)
      syncTreeRootToTemplateName()
      syncTreeRootWorkMode()
      ElMessage.success('导入成功')
      importVisible.value = false
    }
  }).catch(() => {
    ElMessage.warning('导入失败，请检查指标数据')
  })
}

function convertIndicatorToTreeNode(indicator) {
  const node = {
    uid: genUid(),
    label: indicator.indicatorName || indicator.label || '未命名',
    unit: indicator.unit || '',
    description: indicator.description || '',
    valueCategory: indicator.valueCategory || undefined,
    valueMin: indicator.valueMin != null ? indicator.valueMin : undefined,
    valueMax: indicator.valueMax != null ? indicator.valueMax : undefined,
    calcMethod: setCalcMethodWorkMode(indicator.calcMethod != null ? String(indicator.calcMethod) : '', form.workMode) || undefined,
    workMode: form.workMode,
    children: []
  }
  if (indicator.children && indicator.children.length > 0) {
    node.children = indicator.children.map(c => convertIndicatorToTreeNode(c))
  }
  return node
}

onMounted(() => {
  fetchZhpgConductionMethodSelectOptions()
    .then(opts => {
      conductionMethodOptions.value = opts
    })
    .catch(() => {})
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
.table-ops {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 6px 12px;
  padding: 4px;
}
.table-ops :deep(.el-button) {
  margin: 0;
}
.zhpg-indicator-meta-form {
  padding-right: 8px;
}
.preview-tree-wrap {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px;
  min-height: 120px;
}
.preview-tree-title {
  font-weight: bold;
  font-size: 14px;
  margin-bottom: 10px;
  color: #303133;
}
.preview-tree-node {
  display: flex;
  align-items: center;
  gap: 10px;
}
.preview-unit {
  font-size: 12px;
  color: #909399;
}
.preview-calc {
  font-size: 12px;
  color: #67c23a;
}
.preview-vcat {
  font-size: 12px;
  color: #409eff;
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
.node-edit-form {
  padding-top: 4px;
}
.node-edit-form :deep(.el-form-item) {
  margin-bottom: 16px;
}
.zhpg-global-algo-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.zhpg-global-algo-row :deep(.el-button--primary) {
  background: #eef5ff;
  border-color: #bfd4fb;
  color: #2f5ea8;
}
.zhpg-global-algo-row :deep(.el-button--primary:hover),
.zhpg-global-algo-row :deep(.el-button--primary:focus-visible) {
  background: #e2efff;
  border-color: #a8c5f6;
  color: #264f91;
}
.zhpg-algo-tag {
  max-width: 100%;
}
</style>

<style>
.zhpg-indicator-tree-dialog .el-dialog__body {
  padding-top: 8px;
  padding-bottom: 16px;
}

.zhpg-node-edit-dialog.el-dialog {
  width: min(94vw, 1080px) !important;
  max-width: 1080px;
}

.zhpg-node-edit-dialog .el-dialog__body {
  max-height: calc(100vh - 170px);
  overflow: auto;
  padding: 12px 16px 10px;
}
</style>

