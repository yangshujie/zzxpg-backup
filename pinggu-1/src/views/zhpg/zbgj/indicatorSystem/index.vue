<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="110px">
      <el-form-item label="指标体系名称" prop="systemName">
        <el-input
          v-model="queryParams.systemName"
          placeholder="请输入指标体系名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="需求ID" prop="requirementId">
        <el-input-number
          v-model="queryParams.requirementId"
          :min="1"
          :controls="false"
          placeholder="精确"
          clearable
          style="width: 140px"
        />
      </el-form-item>
      <el-form-item label="工作模式" prop="workMode">
        <el-select v-model="queryParams.workMode" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="item in workModeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="queryParams.workMode === ZHPG_WORK_MODE.MAIN_BRANCH_COLLAB" label="构建阶段" prop="buildPhase">
        <el-select v-model="queryParams.buildPhase" placeholder="全部" clearable style="width: 180px">
          <el-option v-for="item in buildPhaseQueryOptions" :key="item.value" :label="item.label" :value="item.value" />
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
        <el-button type="success" plain icon="DocumentCopy" @click="handleCreateFromTemplate">从模板创建</el-button>
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
      </div>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </div>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="systemList" table-layout="fixed" style="width: 100%" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="46" align="center" />
      <el-table-column label="ID" prop="id" width="80" align="center" />
      <el-table-column label="指标体系名称" prop="systemName" min-width="200" show-overflow-tooltip />
      <el-table-column label="需求ID" prop="requirementId" width="100" align="center" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.requirementId != null ? scope.row.requirementId : '—' }}</template>
      </el-table-column>
      <el-table-column label="工作模式" min-width="120" show-overflow-tooltip>
        <template #default="scope">{{ getWorkModeLabel(resolveSystemWorkMode(scope.row)) }}</template>
      </el-table-column>
      <el-table-column label="权重分配算法" prop="weightAssignAlgorithm" min-width="120" show-overflow-tooltip>
        <template #default="scope">{{ getWeightAlgLabel(scope.row.weightAssignAlgorithm) }}</template>
      </el-table-column>
      <el-table-column label="构建阶段" min-width="110" align="center">
        <template #default="scope">
          <template v-if="isMainBranchRow(scope.row)">
            <el-tag :type="getZhpgBuildPhaseTagType(scope.row.buildPhase || 'INITIAL_DRAFT')" size="small">
              {{ getZhpgBuildPhaseLabel(scope.row.buildPhase || 'INITIAL_DRAFT') }}
            </el-tag>
          </template>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="88" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.isApplied === 1 ? 'success' : 'info'" size="small">
            {{ scope.row.isApplied === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" min-width="160" show-overflow-tooltip>
        <template #default="scope">{{ formatLatestTime(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="340" align="center" fixed="right">
        <template #default="scope">
          <div class="table-ops">
            <el-button link type="primary" size="small" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
            <el-button link type="primary" size="small" icon="Download" @click="handleExportJson(scope.row)">下载</el-button>
            <el-button
              v-if="scope.row.isApplied !== 1"
              link
              type="success"
              size="small"
              @click="handleEnable(scope.row)"
            >启用</el-button>
            <el-button
              v-else
              link
              type="warning"
              size="small"
              @click="handleDisable(scope.row)"
            >停用</el-button>
            <!-- 主分协同操作按钮 -->
            <el-button
              v-if="isMainBranchRow(scope.row) && isMainBranchRefined(resolveSystemWorkMode(scope.row), scope.row.buildPhase)"
              link type="primary" size="small"
              @click="handleCompareTree(scope.row)"
            >对比查看</el-button>
            <el-button link type="danger" size="small" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改：全屏 + 左树 / 中图 / 右属性（对齐原项目指标集预览修改交互） -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      fullscreen
      append-to-body
      destroy-on-close
      class="zhpg-indicator-tree-dialog dark-theme"
    >
      <div class="zhpg-indicator-dialog-layout">
        <aside class="zhpg-indicator-meta-sidebar">
          <el-scrollbar class="zhpg-indicator-meta-scrollbar" height="100%">
            <div class="zhpg-indicator-meta-panel zhpg-indicator-meta-panel--stretch">
              <div class="zhpg-indicator-meta-panel-header">体系属性</div>
              <div class="zhpg-indicator-meta-panel-body">
                <el-form
                  ref="formRef"
                  :model="form"
                  :rules="rules"
                  label-position="top"
                  class="zhpg-indicator-meta-form zhpg-indicator-meta-form--sidebar"
                >
                  <div class="zhpg-sidebar-section-head">基本信息</div>
                  <el-form-item label="指标体系名称" prop="systemName" class="zhpg-sidebar-form-item">
                    <el-input v-model="form.systemName" placeholder="请输入指标体系名称" clearable />
                  </el-form-item>
                  <el-form-item v-if="form.requirementId != null" label="需求ID" class="zhpg-sidebar-form-item">
                    <el-input :model-value="String(form.requirementId)" readonly />
                  </el-form-item>
                  <div class="zhpg-sidebar-section-head">全局默认</div>
                  <el-form-item label="工作模式" class="zhpg-sidebar-form-item zhpg-sidebar-form-item--tight">
                    <el-radio-group v-model="form.workMode" class="zhpg-workmode-radio-group">
                      <el-radio-button v-for="item in workModeOptions" :key="item.value" :value="item.value">
                        {{ item.label }}
                      </el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item label="赋权与传导" class="zhpg-sidebar-form-item zhpg-sidebar-form-item--tight">
                    <div class="zhpg-global-algo-row zhpg-global-algo-row--sidebar">
                      <el-button type="primary" plain @click="globalAlgoDialogVisible = true">配置体系全局</el-button>
                      <div class="zhpg-global-algo-tags">
                        <el-tag type="info" effect="plain" class="zhpg-algo-tag">{{ globalWeightSummaryTag }}</el-tag>
                        <el-tag type="info" effect="plain" class="zhpg-algo-tag">{{ globalConductionSummaryTag }}</el-tag>
                      </div>
                    </div>
                  </el-form-item>
                  <!-- 主分协同阶段提示 -->
                  <template v-if="form.workMode === ZHPG_WORK_MODE.MAIN_BRANCH_COLLAB">
                    <el-form-item label="构建阶段" class="zhpg-sidebar-form-item">
                      <div class="zhpg-build-phase-wrapper" style="display: flex; align-items: center; gap: 6px;">
                        <el-tag
                          v-if="!isMainBranchRefined(form.workMode, form.buildPhase)"
                          :type="getZhpgBuildPhaseTagType(form.buildPhase || 'INITIAL_DRAFT')"
                        >
                          {{ getZhpgBuildPhaseLabel(form.buildPhase || 'INITIAL_DRAFT') }}
                        </el-tag>
                        <el-popover
                          v-if="isCurrentInitialDraft"
                          placement="right"
                          width="240"
                          trigger="hover"
                          effect="dark"
                          popper-class="zhpg-stage-popover"
                        >
                          <template #reference>
                            <el-icon color="var(--el-color-primary)" style="cursor: pointer;"><InfoFilled /></el-icon>
                          </template>
                          <div style="font-size: 12px; line-height: 1.5;">主分协同 · 初始粗建：仅需构建上层结构，底层节点可不配算法。分系统通过接口拉取当前指标树；回传接口写入后将进入「已回传细化」，即可直接编辑、计算权重与启用。</div>
                        </el-popover>
                      </div>
                      <div
                        v-if="isMainBranchRefined(form.workMode, form.buildPhase)"
                        class="zhpg-refined-info"
                      >
                        <div class="zhpg-refined-header">
                          <el-tag type="success" size="small">已回传细化</el-tag>
                          <span v-if="form.sourceSubsystem" class="zhpg-refined-source">来源：{{ form.sourceSubsystem }}</span>
                        </div>
                        <div class="zhpg-refined-desc">可直接编辑、计算权重与启用管理。</div>
                      </div>
                    </el-form-item>
                  </template>
                  <el-form-item label="描述" prop="description" class="zhpg-sidebar-form-item zhpg-sidebar-form-item--desc">
                    <el-input
                      v-model="form.description"
                      type="textarea"
                      :autosize="{ minRows: 5, maxRows: 16 }"
                      placeholder="请输入描述"
                    />
                  </el-form-item>
                </el-form>
              </div>
            </div>
          </el-scrollbar>
        </aside>
        <div class="zhpg-indicator-workbench-region">
          <div class="zhpg-indicator-workbench-region-title">指标树构建</div>
          <IndicatorTreeWorkbench
            ref="workbenchRef"
            v-model:tree-data="treeData"
            v-model:selected-node="selectedNode"
            variant="system"
            :show-template-import="true"
            fill-parent-height
            :preferred-root-label="form.systemName"
            weight-tuning-mode="full"
            :weight-assign-algorithm="form.weightAssignAlgorithm"
            :conduction-algorithm="form.conductionAlgorithm"
            :global-work-mode="form.workMode"
            :hide-leaf-calc-method="false"
            @import-indicators="importTreeFromIndicators"
            @import-template="importTreeFromTemplate"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button
          type="primary"
          plain
          :disabled="!form.id || !treeData.length || objectiveWeightLoading"
          :loading="objectiveWeightLoading"
          @click="runObjectiveWeight"
        >客观赋权计算</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 客观赋权：分步进度说明（计算过程） -->
    <el-dialog
      v-model="objectiveWeightProgressVisible"
      title="客观赋权计算"
      width="520px"
      append-to-body
      align-center
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="objectiveWeightDone"
      destroy-on-close
      class="zhpg-objective-weight-progress-dialog"
      @closed="onObjectiveWeightDialogClosed"
    >
      <div class="zhpg-owp-body">
        <el-steps direction="vertical" :active="objectiveWeightStepsActive" finish-status="success">
          <el-step title="校验与准备" description="确认指标体系已保存，并解析当前指标树结构" />
          <el-step
            title="组装层级数据"
            description="按各父节点下的子指标生成用于赋权的样本矩阵"
          />
          <el-step
            title="客观赋权计算"
            description="根据体系或节点上选择的赋权方法，提交至算法服务逐层计算子节点权重；指标层级多、调用次数多时，等待时间会相应变长"
          />
          <el-step title="回写与刷新" description="对权重做层内归一化，保存带权重的指标树快照，并刷新左侧树与中间图形" />
        </el-steps>
        <div v-if="objectiveWeightLoading" class="zhpg-owp-status">
          <el-icon class="zhpg-owp-spin is-loading"><Loading /></el-icon>
          <span>正在计算，请稍候，勿关闭本窗口</span>
        </div>
      </div>
      <template v-if="objectiveWeightDone" #footer>
        <el-button type="primary" @click="closeObjectiveWeightProgressDialog">知道了</el-button>
      </template>
    </el-dialog>

    <!-- 从模板创建对话框 -->
    <el-dialog title="从模板创建指标体系" v-model="templateDialogVisible" width="min(600px, 95vw)" append-to-body>
      <el-form ref="templateFormRef" :model="templateForm" :rules="templateRules" label-width="120px">
        <el-form-item label="选择模板" prop="templateId">
          <el-select
            v-model="templateForm.templateId"
            filterable
            remote
            reserve-keyword
            :remote-method="searchTemplates"
            :loading="templateSearchLoading"
            style="width: 100%"
            placeholder="请输入模板名称检索"
          >
            <el-option v-for="item in templateSearchOptions" :key="item.id" :label="item.templateName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="指标体系名称" prop="systemName">
          <el-input v-model="templateForm.systemName" placeholder="不填则使用模板名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitCreateFromTemplate">创 建</el-button>
      </template>
    </el-dialog>

    <!-- 从指标库导入对话框 -->
    <el-dialog title="从指标库导入" v-model="importIndicatorVisible" width="min(600px, 95vw)" append-to-body>
      <el-alert type="info" :closable="false" show-icon title="选择根指标后，将自动导入该指标及其子指标形成指标树" style="margin-bottom: 12px" />
      <el-form label-width="100px">
        <el-form-item label="指标类型">
          <el-select v-model="importIndicatorType" placeholder="请先选择指标类型" clearable style="width: 100%" @change="loadIndicatorRoots">
            <el-option v-for="item in equipmentTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="根指标">
          <el-select v-model="importRootId" filterable placeholder="请选择根指标" :loading="importIndicatorLoading" style="width: 100%">
            <el-option v-for="item in importRootOptions" :key="item.id" :label="item.indicatorName" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importIndicatorVisible = false">取 消</el-button>
        <el-button type="primary" @click="doImportFromIndicators" :disabled="!importRootId">导 入</el-button>
      </template>
    </el-dialog>

    <!-- 从体系模板导入对话框 -->
    <el-dialog title="从体系模板导入指标树" v-model="importTemplateVisible" width="min(600px, 95vw)" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="选择模板">
          <el-select
            v-model="importTemplateId"
            filterable
            remote
            reserve-keyword
            :remote-method="searchImportTemplates"
            :loading="importTemplateLoading"
            style="width: 100%"
            placeholder="请输入模板名称检索"
          >
            <el-option v-for="item in importTemplateOptions" :key="item.id" :label="item.templateName" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importTemplateVisible = false">取 消</el-button>
        <el-button type="primary" @click="doImportFromTemplate" :disabled="!importTemplateId">导 入</el-button>
      </template>
    </el-dialog>

    <!-- 主分协同：原始指标树 vs 已回传细化 对比查看 -->
    <el-dialog title="指标树对比：原始指标树（indicator_tree）↔ 已回传细化" v-model="compareDialogVisible" width="90%" top="5vh" append-to-body destroy-on-close>
      <div class="compare-container">
        <div class="compare-pane">
          <div class="compare-pane-title">原始指标树（indicator_tree）</div>
          <el-tree
            :data="compareRawTree"
            :props="{ label: 'label', children: 'children' }"
            default-expand-all
            class="compare-tree"
          />
        </div>
        <div class="compare-pane">
          <div class="compare-pane-title">已回传细化（refined_indicator_tree）</div>
          <el-tree
            :data="compareRefinedTree"
            :props="{ label: 'label', children: 'children' }"
            default-expand-all
            class="compare-tree"
          />
        </div>
      </div>
    </el-dialog>

    <el-dialog title="体系全局 · 赋权与传导" v-model="globalAlgoDialogVisible" width="min(520px, 95vw)" append-to-body>
      <el-form label-width="120px">
        <el-form-item label="权重分配算法">
          <el-select v-model="form.weightAssignAlgorithm" placeholder="未选" clearable style="width: 100%">
            <el-option v-for="item in weightAlgorithmOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="传导方法">
          <el-select v-model="form.conductionAlgorithm" placeholder="未选" clearable style="width: 100%">
            <el-option v-for="item in conductionMethodOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="saveGlobalAlgoAndSyncToNodes">完 成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, watch, computed, onMounted, nextTick } from 'vue'
import {
  listIndicatorSystem,
  getIndicatorSystem,
  addIndicatorSystem,
  updateIndicatorSystem,
  delIndicatorSystem,
  publishIndicatorSystem,
  distributeIndicatorSystem,
  disableIndicatorSystem,
  exportIndicatorSystemJson,
  createFromTemplate,
  objectiveWeightIndicatorSystem
} from '@/api/zhpg/indicatorSystem'
import {
  listIndicatorSystemTemplate,
  getIndicatorSystemTemplate
} from '@/api/zhpg/indicatorSystemTemplate'
import { listAllIndicator, getIndicatorTree } from '@/api/zhpg/indicator'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import {
  ZHPG_EQUIPMENT_TYPE_OPTIONS,
  getZhpgEquipmentTypeLabel
} from '@/constants/zhpgIndicatorSystem'
import {
  ZHPG_WEIGHT_ASSIGN_OPTIONS,
  getZhpgWeightAssignLabel,
  getZhpgConductionMethodLabel,
  serializeConductionConfig,
  parseConductionConfig
} from '@/constants/zhpgIndicatorSystemAlgorithms'
import { fetchZhpgConductionMethodSelectOptions } from '@/utils/zhpgConductionAlgorithms'
import IndicatorTreeWorkbench from '@/views/zhpg/components/IndicatorTreeWorkbench.vue'
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
  normalizeZhpgWorkMode,
  ZHPG_BUILD_PHASE,
  ZHPG_BUILD_PHASE_QUERY_OPTIONS,
  getZhpgBuildPhaseLabel,
  getZhpgBuildPhaseTagType,
  isMainBranchInitialDraft,
  isMainBranchRefined
} from '@/constants/zhpgWorkMode'

const equipmentTypeOptions = ZHPG_EQUIPMENT_TYPE_OPTIONS

const weightAlgorithmOptions = ZHPG_WEIGHT_ASSIGN_OPTIONS
const conductionMethodOptions = ref([])
const workModeOptions = ZHPG_WORK_MODE_OPTIONS
const buildPhaseQueryOptions = ZHPG_BUILD_PHASE_QUERY_OPTIONS

onMounted(() => {
  fetchZhpgConductionMethodSelectOptions()
    .then(opts => {
      conductionMethodOptions.value = opts
    })
    .catch(() => {})
})

function getEquipmentTypeLabel(val) {
  return getZhpgEquipmentTypeLabel(val)
}

function getWeightAlgLabel(val) {
  return getZhpgWeightAssignLabel(val)
}

/** 列表/解析用：主分协同且已有回传树时优先用回传（与权重计算一致） */
function treeJsonForRowWorkMode(row) {
  if (!row) return null
  const wm = row.workMode ? normalizeZhpgWorkMode(row.workMode) : null
  if (wm === ZHPG_WORK_MODE.MAIN_BRANCH_COLLAB && row.refinedIndicatorTree && String(row.refinedIndicatorTree).trim()) {
    return row.refinedIndicatorTree
  }
  return row.indicatorTree
}

function resolveSystemWorkMode(row) {
  if (row?.workMode) return normalizeZhpgWorkMode(row.workMode)
  try {
    const tree = parseTreeJson(treeJsonForRowWorkMode(row))
    return normalizeZhpgWorkMode(tree?.[0]?.workMode)
  } catch {
    return ZHPG_WORK_MODE.INTERNAL_CIRCULATION
  }
}

function getWorkModeLabel(value) {
  return getZhpgWorkModeLabel(value)
}

function formatLatestTime(row) {
  const c = row.createTime
  const u = row.updateTime
  if (!c && !u) return '—'
  if (!u) return c
  if (!c) return u
  return String(u) >= String(c) ? u : c
}

let uidCounter = 0
function genUid() {
  return 'node_' + (++uidCounter) + '_' + Date.now()
}

// 状态
const loading = ref(false)
const showSearch = ref(true)
const systemList = ref([])
const total = ref(0)
const multiple = ref(true)
const ids = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const templateDialogVisible = ref(false)
const templateSearchLoading = ref(false)
const templateSearchOptions = ref([])
const globalAlgoDialogVisible = ref(false)
const importIndicatorVisible = ref(false)
const importIndicatorLoading = ref(false)
const importIndicatorType = ref('')
const importRootId = ref(null)
const importRootOptions = ref([])
const importTemplateVisible = ref(false)
const importTemplateLoading = ref(false)
const importTemplateId = ref(null)
const importTemplateOptions = ref([])
/** 为 true 时工作台编辑的是 refined_indicator_tree，保存时不覆盖 indicator_tree */
const workbenchUsesRefinedTree = ref(false)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  systemName: undefined,
  requirementId: undefined,
  workMode: undefined,
  buildPhase: undefined
})

const form = reactive({
  id: undefined,
  idCode: undefined,
  systemName: undefined,
  requirementId: undefined,
  indicatorTree: undefined,
  refinedIndicatorTree: undefined,
  workMode: ZHPG_WORK_MODE.INTERNAL_CIRCULATION,
  weightAssignAlgorithm: undefined,
  conductionAlgorithm: undefined,
  description: undefined,
  status: 'DRAFT',
  buildPhase: undefined,
  sourceSubsystem: undefined,
  indicatorTreeWeight: undefined
})

watch(
  () => queryParams.workMode,
  mode => {
    if (mode !== ZHPG_WORK_MODE.MAIN_BRANCH_COLLAB) {
      queryParams.buildPhase = undefined
    }
  }
)

/** 切换工作模式时勿清空 buildPhase：否则从内部流转切回主分协同会丢失「已回传细化」等阶段（仅内存展示与再次切回主分协同时需要）。内部流转下侧栏不展示构建阶段；提交时仍按工作模式决定是否传 build_phase。 */

/** 主分协同：当前是否处于初始粗建阶段 */
const isCurrentInitialDraft = computed(() =>
  isMainBranchInitialDraft(form.workMode, form.buildPhase)
)

/** 对比查看弹窗 */
const compareDialogVisible = ref(false)
const compareRawTree = ref([])
const compareRefinedTree = ref([])

const globalWeightSummaryTag = computed(() => getZhpgWeightAssignLabel(form.weightAssignAlgorithm))
const globalConductionSummaryTag = computed(() =>
  getZhpgConductionMethodLabel(form.conductionAlgorithm, conductionMethodOptions.value)
)

const templateForm = reactive({
  templateId: undefined,
  systemName: undefined
})

const treeData = ref([])

/** 指标树根节点（多根时取第一个）与指标体系名称一致 */
function syncTreeRootToSystemName() {
  const name = (form.systemName || '').trim()
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
    } else if (node.children?.length) {
      applyWorkModeToLeafNodes(node.children, workMode)
    }
  }
}

/** 将全局赋权/传导配置同步到所有非叶子节点 */
function syncGlobalAlgoToAllNodes(nodes) {
  for (const node of nodes || []) {
    const isLeaf = !node.children || node.children.length === 0
    if (!isLeaf) {
      // 非叶子节点：同步全局赋权算法（如果全局已配置）
      if (form.weightAssignAlgorithm) {
        node.weightAssignAlgorithm = form.weightAssignAlgorithm
      }
      // 非叶子节点：同步全局传导算法（如果全局已配置）
      if (form.conductionAlgorithm) {
        node.conductionAlgorithm = form.conductionAlgorithm
      }
      // 递归处理子节点
      if (node.children?.length) {
        syncGlobalAlgoToAllNodes(node.children)
      }
    }
  }
}

/** 保存全局配置并同步到所有节点 */
function saveGlobalAlgoAndSyncToNodes() {
  // 同步全局配置到所有节点
  syncGlobalAlgoToAllNodes(treeData.value)
  // 关闭对话框
  globalAlgoDialogVisible.value = false
  ElMessage.success("全局配置已同步到所有节点")
}
watch(
  () => [dialogVisible.value, (form.systemName || '').trim()],
  ([open, name]) => {
    if (!open || !name || !treeData.value?.length) return
    const root = treeData.value[0]
    if (root) root.label = name
  }
)
const selectedNode = ref(null)
const workbenchRef = ref(null)
const objectiveWeightLoading = ref(false)
const objectiveWeightProgressVisible = ref(false)
const objectiveWeightStep = ref(0)
const objectiveWeightDone = ref(false)
let objectiveWeightStepTimers = []

/** 完成后 el-steps 置为全部完成态（4 步对应 active=4） */
const objectiveWeightStepsActive = computed(() => {
  if (objectiveWeightDone.value) {
    return 4
  }
  return objectiveWeightStep.value
})

function clearObjectiveWeightStepTimers() {
  objectiveWeightStepTimers.forEach(id => clearTimeout(id))
  objectiveWeightStepTimers = []
}

function closeObjectiveWeightProgressDialog() {
  objectiveWeightProgressVisible.value = false
}

function onObjectiveWeightDialogClosed() {
  clearObjectiveWeightStepTimers()
  objectiveWeightLoading.value = false
  objectiveWeightDone.value = false
  objectiveWeightStep.value = 0
}

/** 全局传导里「主机/表决」等下拉：默认取第一个根节点的直接子节点 */
const rules = reactive({
  systemName: [{ required: true, message: '指标体系名称不能为空', trigger: 'blur' }]
})

const templateRules = reactive({
  templateId: [{ required: true, message: '请选择模板', trigger: 'change' }]
})

const formRef = ref(null)
const queryRef = ref(null)
const templateFormRef = ref(null)

function parseTreeJson(json) {
  return parseIndicatorTreeToForest(json)
}

const SYSTEM_AGGREGATION = '无'

/**
 * 检查树中每个节点的装备类型设置：
 * 1. 底层节点（叶节点）必须设置 indicatorType 且不能为无
 * 2. 非叶节点若设置了具体装备类型（非无），其直接子节点中若有类型，则必须与父节点一致
 */
function validateTreeIndicatorTypes(nodes) {
  for (const n of nodes || []) {
    const isLeaf = !n.children || n.children.length === 0
    const nodeName = n.label || '未命名节点'
    if (isLeaf) {
      if (!n.indicatorType) {
        return `底层指标「${nodeName}」未设置适用装备，请在节点属性中选择装备类型`
      }
      if (n.indicatorType === SYSTEM_AGGREGATION) {
        return `底层指标「${nodeName}」不能设为无，请选择具体装备类型`
      }
    } else if (n.indicatorType && n.indicatorType !== SYSTEM_AGGREGATION) {
      // 非叶节点为具体装备类型时，子节点若已设类型则必须一致
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

/** 叶节点且填写了计算方法时，校验图形化 JSON（与模板构建一致） */
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

function getList() {
  loading.value = true
  listIndicatorSystem(queryParams).then(res => {
    systemList.value = res.rows
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
  form.idCode = undefined
  form.systemName = undefined
  form.requirementId = undefined
  form.indicatorTree = undefined
  form.refinedIndicatorTree = undefined
  form.workMode = ZHPG_WORK_MODE.INTERNAL_CIRCULATION
  form.weightAssignAlgorithm = undefined
  form.conductionAlgorithm = undefined
  form.description = undefined
  form.status = 'DRAFT'
  form.buildPhase = undefined
  form.sourceSubsystem = undefined
  form.indicatorTreeWeight = undefined
  treeData.value = []
  selectedNode.value = null
  workbenchUsesRefinedTree.value = false
  if (formRef.value) formRef.value.resetFields()
}

// ==================== 主分协同操作 ====================

function isMainBranchRow(row) {
  return resolveSystemWorkMode(row) === ZHPG_WORK_MODE.MAIN_BRANCH_COLLAB
}

function zhpgNonEmptyTreeJson(s) {
  return s != null && String(s).trim() !== ''
}

function handleCompareTree(row) {
  getIndicatorSystem(row.id).then(res => {
    const data = res.data
    if (zhpgNonEmptyTreeJson(data.indicatorTree)) {
      compareRawTree.value = parseTreeJson(data.indicatorTree)
    } else {
      compareRawTree.value = [{ label: '（原始指标树为空：indicator_tree 无数据）', children: [] }]
    }
    // 右侧：仅 refined_indicator_tree；为空则不再回退到 indicator_tree，避免「左空右满」的错觉
    if (zhpgNonEmptyTreeJson(data.refinedIndicatorTree)) {
      compareRefinedTree.value = parseTreeJson(data.refinedIndicatorTree)
    } else {
      compareRefinedTree.value = [
        { label: '（暂无回传细化数据：refined_indicator_tree 为空）', children: [] }
      ]
      if (isMainBranchRefined(normalizeZhpgWorkMode(data.workMode), data.buildPhase)) {
        ElMessage.warning(
          '构建阶段为「已回传细化」，但库中回传字段为空，与阶段标记不一致。请通过回传接口写入 refined_indicator_tree，或修正构建阶段。'
        )
      }
    }
    compareDialogVisible.value = true
  })
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增指标体系'
  dialogVisible.value = true
}

function handleUpdate(row) {
  resetForm()
  getIndicatorSystem(row.id).then(res => {
    const data = res.data
    form.id = data.id
    form.idCode = data.idCode
    form.systemName = data.systemName
    form.requirementId = data.requirementId
    form.indicatorTree = data.indicatorTree
    form.refinedIndicatorTree = data.refinedIndicatorTree
    form.weightAssignAlgorithm = data.weightAssignAlgorithm
    form.conductionAlgorithm = parseConductionConfig(data.conductionConfig) || undefined
    form.description = data.description
    form.status = data.status || 'DRAFT'
    form.buildPhase = data.buildPhase || undefined
    form.sourceSubsystem = data.sourceSubsystem || undefined
    form.indicatorTreeWeight = data.indicatorTreeWeight || undefined
    const refinedStr = data.refinedIndicatorTree && String(data.refinedIndicatorTree).trim()
    workbenchUsesRefinedTree.value = !!(
      refinedStr &&
      normalizeZhpgWorkMode(data.workMode) === ZHPG_WORK_MODE.MAIN_BRANCH_COLLAB &&
      isMainBranchRefined(data.workMode, data.buildPhase)
    )
    const treeJson = workbenchUsesRefinedTree.value ? data.refinedIndicatorTree : data.indicatorTree
    treeData.value = parseTreeJson(treeJson)
    if (treeJson && !treeData.value.length) {
      ElMessage.warning(
        '指标树数据解析失败，请检查库内 ' +
          (workbenchUsesRefinedTree.value ? 'refined_indicator_tree' : 'indicator_tree') +
          ' 是否为合法 JSON'
      )
    }
    form.workMode = normalizeZhpgWorkMode(data.workMode || treeData.value?.[0]?.workMode)
    syncTreeRootToSystemName()
    dialogTitle.value = '修改指标体系'
    dialogVisible.value = true
    nextTick(() => workbenchRef.value?.renderGraph())
  })
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    // 主分协同初始粗建阶段：跳过底层节点计算方法和装备类型校验
    if (!isCurrentInitialDraft.value) {
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
    }
    applyWorkModeToLeafNodes(treeData.value, form.workMode)
    syncTreeRootToSystemName()
    syncTreeRootWorkMode()
    // 主分协同：无阶段时默认初始粗建。内部流转：仍回传已有 buildPhase（若有），避免仅切换工作模式保存后把库里的「已回传细化」清空。
    const buildPhase =
      form.workMode === ZHPG_WORK_MODE.MAIN_BRANCH_COLLAB
        ? form.buildPhase || ZHPG_BUILD_PHASE.INITIAL_DRAFT
        : form.buildPhase
    const serialized = serializeForestToIndicatorTree(treeData.value, {
      systemName: form.systemName,
      workMode: form.workMode
    })
    const payload = {
      systemName: form.systemName,
      workMode: form.workMode,
      weightAssignAlgorithm: form.weightAssignAlgorithm,
      conductionConfig: serializeConductionConfig(form.conductionAlgorithm) || undefined,
      description: form.description,
      status: form.status,
      buildPhase
    }
    if (workbenchUsesRefinedTree.value) {
      payload.refinedIndicatorTree = serialized
      payload.indicatorTree = form.indicatorTree
    } else {
      payload.indicatorTree = serialized
      payload.refinedIndicatorTree = form.refinedIndicatorTree
    }
    if (form.indicatorTreeWeight != null && String(form.indicatorTreeWeight).trim() !== '') {
      payload.indicatorTreeWeight = form.indicatorTreeWeight
    }
    if (form.id != null) {
      payload.id = form.id
      updateIndicatorSystem(payload).then(() => {
        ElMessage.success('修改成功')
        dialogVisible.value = false
        getList()
      })
    } else {
      addIndicatorSystem(payload).then(() => {
        ElMessage.success('新增成功')
        dialogVisible.value = false
        getList()
      })
    }
  })
}

/** 客观赋权：后端模拟样本矩阵 → 算法服务，持久化 indicator_tree_weight 并刷新工作台 */
async function runObjectiveWeight() {
  if (!form.id) {
    ElMessage.warning('请先保存指标体系后再进行客观赋权')
    return
  }
  if (!treeData.value?.length) {
    ElMessage.warning('指标树为空')
    return
  }

  clearObjectiveWeightStepTimers()
  objectiveWeightStep.value = 0
  objectiveWeightDone.value = false
  objectiveWeightProgressVisible.value = true
  objectiveWeightLoading.value = true

  objectiveWeightStepTimers.push(setTimeout(() => {
    objectiveWeightStep.value = 1
  }, 320))
  objectiveWeightStepTimers.push(setTimeout(() => {
    objectiveWeightStep.value = 2
  }, 880))

  try {
    const res = await objectiveWeightIndicatorSystem(form.id, { persist: true, mockSampleRows: 8 })
    clearObjectiveWeightStepTimers()

    objectiveWeightStep.value = 3
    const payload = res.data || {}
    const tw = payload.indicatorTreeWeight
    if (tw) {
      form.indicatorTreeWeight = tw
      treeData.value = parseTreeJson(tw)
      await nextTick()
      workbenchRef.value?.renderGraph?.()
    }

    objectiveWeightLoading.value = false
    objectiveWeightDone.value = true

    ElMessage.success(res.msg || '客观赋权完成')
    if (payload.hint) {
      ElMessage.info(String(payload.hint))
    }
  } catch {
    clearObjectiveWeightStepTimers()
    objectiveWeightProgressVisible.value = false
    objectiveWeightLoading.value = false
    objectiveWeightStep.value = 0
    objectiveWeightDone.value = false
  }
}

function handleDelete(row) {
  const delIds = row.id ? [row.id] : ids.value
  ElMessageBox.confirm('是否确认删除所选的指标体系？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    return delIndicatorSystem(delIds.join(','))
  }).then(() => {
    getList()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

/** 启用：未发布则先发布再下发（isApplied=1），已发布则直接下发 */
function handleEnable(row) {
  ElMessageBox.confirm(
    '确认启用该指标体系？启用后将标记为已发布并下发生效。',
    '提示',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
  )
    .then(() => {
      if (row.status === 'PUBLISHED') {
        return distributeIndicatorSystem(row.id)
      }
      return publishIndicatorSystem(row.id).then(() => distributeIndicatorSystem(row.id))
    })
    .then(() => {
      getList()
      ElMessage.success('已启用')
    })
    .catch(() => {})
}

function handleDisable(row) {
  ElMessageBox.confirm('确认停用该指标体系？停用后不再作为当前启用体系使用。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => disableIndicatorSystem(row.id))
    .then(() => {
      getList()
      ElMessage.success('已停用')
    })
    .catch(() => {})
}

function handleExportJson(row) {
  exportIndicatorSystemJson(row.id).then(res => {
    const data = res.data || res
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = (data.systemName || 'indicator_system') + '.json'
    link.click()
    URL.revokeObjectURL(url)
  })
}

// 从模板创建
function handleCreateFromTemplate() {
  templateForm.templateId = undefined
  templateForm.systemName = undefined
  templateSearchOptions.value = []
  templateDialogVisible.value = true
  searchTemplates('')
}

function searchTemplates(keyword) {
  templateSearchLoading.value = true
  listIndicatorSystemTemplate({ pageNum: 1, pageSize: 50, templateName: keyword || undefined }).then(res => {
    templateSearchOptions.value = res.rows || []
  }).finally(() => {
    templateSearchLoading.value = false
  })
}

function submitCreateFromTemplate() {
  templateFormRef.value.validate(valid => {
    if (!valid) return
    createFromTemplate({
      templateId: templateForm.templateId,
      systemName: templateForm.systemName || undefined
    }).then(res => {
      ElMessage.success('创建成功')
      templateDialogVisible.value = false
      getList()
      if (res && res.data && res.data.id) {
        handleUpdate({ id: res.data.id })
      }
    })
  })
}

// 从指标库导入
function importTreeFromIndicators() {
  importIndicatorType.value = treeData.value?.[0]?.indicatorType || ''
  importRootId.value = null
  importRootOptions.value = []
  importIndicatorVisible.value = true
  if (importIndicatorType.value) loadIndicatorRoots()
}

function loadIndicatorRoots() {
  if (!importIndicatorType.value) {
    importRootOptions.value = []
    return
  }
  importIndicatorLoading.value = true
  listAllIndicator({ indicatorType: importIndicatorType.value, parentId: 0 }).then(res => {
    importRootOptions.value = res.data || []
  }).finally(() => {
    importIndicatorLoading.value = false
  })
}

function doImportFromIndicators() {
  if (!importRootId.value) return
  getIndicatorTree({ rootId: importRootId.value }).then(res => {
    const tree = res.data
    if (tree) {
      treeData.value = [convertIndicatorToTreeNode(tree)]
      syncTreeRootToSystemName()
      syncTreeRootWorkMode()
      ElMessage.success('导入成功')
      importIndicatorVisible.value = false
    }
  }).catch(() => {
    ElMessage.warning('导入失败，请检查指标数据')
  })
}

function convertIndicatorToTreeNode(indicator) {
  const node = {
    uid: indicator.idCode ? String(indicator.idCode) : genUid(),
    label: indicator.indicatorName || indicator.label || '未命名',
    indicatorType: indicator.indicatorType || undefined,
    systemType: indicator.systemType || undefined,
    unit: indicator.unit || '',
    description: indicator.description || '',
    valueCategory: indicator.valueCategory || undefined,
    valueMin: indicator.valueMin != null ? indicator.valueMin : undefined,
    valueMax: indicator.valueMax != null ? indicator.valueMax : undefined,
    calcMethod: setCalcMethodWorkMode(indicator.calcMethod != null ? String(indicator.calcMethod) : '', form.workMode) || undefined,
    children: []
  }
  if (indicator.children && indicator.children.length > 0) {
    node.children = indicator.children.map(c => convertIndicatorToTreeNode(c))
  }
  return node
}

// 从模板导入指标树
function importTreeFromTemplate() {
  importTemplateId.value = null
  importTemplateOptions.value = []
  importTemplateVisible.value = true
  searchImportTemplates('')
}

function searchImportTemplates(keyword) {
  importTemplateLoading.value = true
  listIndicatorSystemTemplate({ pageNum: 1, pageSize: 50, templateName: keyword || undefined }).then(res => {
    importTemplateOptions.value = res.rows || []
  }).finally(() => {
    importTemplateLoading.value = false
  })
}

function doImportFromTemplate() {
  if (!importTemplateId.value) return
  getIndicatorSystemTemplate(importTemplateId.value).then(res => {
    const data = res.data
    if (data && data.indicatorTree) {
      treeData.value = parseTreeJson(data.indicatorTree)
      applyWorkModeToLeafNodes(treeData.value, form.workMode)
      syncTreeRootToSystemName()
      syncTreeRootWorkMode()
      ElMessage.success('已导入模板指标树')
      importTemplateVisible.value = false
    } else {
      ElMessage.warning('该模板暂无指标树结构')
    }
  })
}

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
  gap: 4px 8px;
  padding: 4px;
}
.table-ops :deep(.el-button) {
  margin: 0;
}
.zhpg-indicator-meta-form {
  padding-right: 8px;
}
.zhpg-indicator-dialog-layout {
  display: flex;
  flex: 1;
  min-height: 0;
  gap: 10px;
  align-items: stretch;
}
.zhpg-indicator-meta-sidebar {
  width: min(304px, 28vw);
  min-width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding-right: 8px;
}
.zhpg-indicator-meta-scrollbar {
  flex: 1;
  min-height: 0;
}
.zhpg-indicator-meta-scrollbar :deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}
.zhpg-indicator-meta-scrollbar :deep(.el-scrollbar__view) {
  min-height: 100%;
}
.zhpg-indicator-meta-panel--stretch {
  min-height: 100%;
  box-sizing: border-box;
}
.zhpg-indicator-meta-panel {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: transparent;
  overflow: hidden;
}
.zhpg-indicator-meta-panel-header {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
  padding: 8px 12px;
  background: transparent;
  border-bottom: 1px solid var(--el-border-color);
  color: var(--el-text-color-primary);
}
.zhpg-indicator-meta-panel-body {
  padding: 8px 12px 14px;
}
.zhpg-sidebar-section-head {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  margin: 8px 0 6px;
  padding-bottom: 4px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
}
.zhpg-sidebar-section-head:first-child {
  margin-top: 2px;
}
.zhpg-indicator-meta-form--sidebar :deep(.el-form-item__label) {
  font-size: 13px;
  line-height: 1.35;
  color: var(--el-text-color-regular);
  margin-bottom: 4px !important;
  padding: 0;
}
.zhpg-indicator-meta-form--sidebar :deep(.el-input__inner),
.zhpg-indicator-meta-form--sidebar :deep(.el-textarea__inner) {
  font-size: 14px;
}
.zhpg-indicator-meta-form--sidebar :deep(.el-form-item.zhpg-sidebar-form-item) {
  margin-bottom: 11px;
}
.zhpg-indicator-meta-form--sidebar :deep(.el-form-item.zhpg-sidebar-form-item--tight) {
  margin-bottom: 8px;
}
.zhpg-indicator-meta-form--sidebar :deep(.el-form-item.zhpg-sidebar-form-item--desc) {
  margin-bottom: 0;
}
.zhpg-workmode-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  width: 100%;
}
.zhpg-workmode-radio-group :deep(.el-radio-button__inner) {
  padding: 6px 12px;
  font-size: 13px;
}
.zhpg-global-algo-row--sidebar {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
}
.zhpg-global-algo-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}
.zhpg-sidebar-alert {
  margin-top: 8px;
}
.zhpg-sidebar-alert :deep(.el-alert) {
  padding: 6px 8px;
}
.zhpg-sidebar-alert :deep(.el-alert__title) {
  line-height: 1.45;
  font-size: 13px;
}

/* 已回传细化紧凑信息展示 */
.zhpg-refined-info {
  width: 100%;
  flex: 0 0 100%;
  box-sizing: border-box;
  margin-top: 0;
  padding: 10px 12px;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.25);
  border-radius: 6px;
}
.zhpg-refined-header {
  flex-wrap: wrap;
}
.zhpg-refined-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.zhpg-refined-source {
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.zhpg-refined-desc {
  font-size: 13px;
  color: var(--el-text-color-primary);
  line-height: 1.5;
}
.zhpg-indicator-workbench-region {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.zhpg-indicator-workbench-region-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  padding: 0 0 6px;
  flex-shrink: 0;
}
.zhpg-global-algo-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.zhpg-algo-tag {
  max-width: 100%;
}
.compare-container {
  display: flex;
  gap: 16px;
  min-height: 400px;
}
.compare-pane {
  flex: 1;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 12px;
  overflow: auto;
  max-height: 70vh;
}
.compare-pane-title {
  font-weight: bold;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.compare-tree {
  background: transparent;
}

.zhpg-indicator-tree-dialog.dark-theme .zhpg-indicator-meta-panel {
  background: #111d35;
  border-color: rgba(0, 242, 255, 0.15);
}
.zhpg-indicator-tree-dialog.dark-theme .zhpg-indicator-meta-panel-header {
  background: rgba(34, 211, 238, 0.05);
  border-bottom-color: rgba(0, 242, 255, 0.15);
  color: #22d3ee;
}
.zhpg-indicator-tree-dialog.dark-theme .zhpg-sidebar-section-head { color: #9fb6cb; border-bottom-color: rgba(0, 242, 255, 0.1); }
.zhpg-indicator-tree-dialog.dark-theme :deep(.el-form-item__label) { color: #d8e7f5 !important; }
.zhpg-indicator-tree-dialog.dark-theme :deep(.el-input__inner), .zhpg-indicator-tree-dialog.dark-theme :deep(.el-textarea__inner) { background-color: rgba(5, 13, 24, 0.6); border-color: rgba(0, 242, 255, 0.2); color: #d8e7f5; }
.zhpg-indicator-tree-dialog.dark-theme :deep(.el-input-number__increase), .zhpg-indicator-tree-dialog.dark-theme :deep(.el-input-number__decrease) { background-color: rgba(5, 13, 24, 0.8); border-color: rgba(0, 242, 255, 0.2); color: #9fb6cb; }
</style>

<style>
.zhpg-indicator-tree-dialog.is-fullscreen {
  display: flex;
  flex-direction: column;
  margin: 0;
  max-height: 100vh;
  overflow: hidden;
}
.zhpg-indicator-tree-dialog.is-fullscreen .el-dialog__header {
  flex-shrink: 0;
}
.zhpg-indicator-tree-dialog.is-fullscreen .el-dialog__body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding-top: 8px;
  padding-bottom: 8px;
}
.zhpg-indicator-tree-dialog.dark-theme.is-fullscreen {
  background: var(--el-bg-color);
}
.zhpg-indicator-tree-dialog.dark-theme.is-fullscreen .el-dialog {
  background: var(--el-bg-color);
}
.zhpg-indicator-tree-dialog.dark-theme .el-dialog__header,
.zhpg-indicator-tree-dialog.dark-theme .el-dialog__body,
.zhpg-indicator-tree-dialog.dark-theme .el-dialog__footer {
  background: var(--el-bg-color);
}
.zhpg-indicator-tree-dialog.is-fullscreen .el-dialog__footer {
  flex-shrink: 0;
}
/* append-to-body：与 scoped 分离，保证进度弹窗样式生效 */
.zhpg-objective-weight-progress-dialog .el-dialog__body {
  padding-top: 8px;
}
.zhpg-objective-weight-progress-dialog .zhpg-owp-body {
  min-height: 0;
}
.zhpg-objective-weight-progress-dialog .zhpg-owp-status {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
  padding: 12px 14px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  font-size: 14px;
}
.zhpg-objective-weight-progress-dialog .zhpg-owp-spin {
  font-size: 20px;
  color: var(--el-color-primary);
}
</style>
