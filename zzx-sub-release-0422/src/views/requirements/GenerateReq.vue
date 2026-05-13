<template>
  <div class="generate-req-page">
    <!-- Topbar removed from here as per request (moved to footer in Stage 2) -->

    <!-- Requirement Editing Container -->
    <div class="stage-two-container">
      <!-- AI Generation Overlay -->
      <div v-if="isGenerating" class="generating-overlay">
        <RobotAnimation />
        <div class="generating-text">正在分析骨架并生成需求表单...</div>
      </div>

      <!-- Main Two-Pane Layout -->
      <div v-else class="requirement-layout">
        <!-- ════ Left Pane: Requirement Elements ════ -->
        <div class="left-pane" :class="{ collapsed: isLeftPaneCollapsed }">
          <div class="lp-head">
            <el-icon class="lp-collapse-btn" @click="isLeftPaneCollapsed = !isLeftPaneCollapsed">
              <Expand v-if="isLeftPaneCollapsed" />
              <Fold v-else />
            </el-icon>
            <span class="lp-head-title" v-if="!isLeftPaneCollapsed">需求要素编辑</span>
            <div class="lp-progress" v-if="!isLeftPaneCollapsed">
              <div class="lp-prog-bar">
                <div class="lp-prog-fill" :style="{ width: completionRate + '%' }"></div>
              </div>
              <span class="lp-prog-val">{{ completionRate }}%</span>
            </div>
          </div>
          <div class="lp-scroll" v-if="!isLeftPaneCollapsed">
            <!-- 关联工程 -->
            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:#00f2ff;"></span>关联工程<span class="req">必填</span>
              </div>
              <el-select v-model="formData.projectId" placeholder="请选择关联工程" style="width: 100%" class="cyber-select">
                <el-option v-for="item in projectOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>

            <!-- 待评装备 -->
            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:var(--jam);"></span>待评装备<span
                  class="req">必填</span>
              </div>
              <div class="tag-editor">
                <div class="tag" v-for="(tag, idx) in formData.equips" :key="idx"
                  style="background:var(--jam-bg);color:var(--jam);border:1px solid var(--jam-border);">
                  {{ tag }}<span class="x" @click="formData.equips.splice(idx, 1)">×</span>
                </div>
                <input class="tag-input" placeholder="回车添加…" @keydown.enter="addTag($event, 'equips')">
              </div>
            </div>

            <!-- 评估目标 -->
            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:var(--accent);"></span>评估目标<span
                  class="req">必填</span></div>
              <div class="tag-editor">
                <div class="tag" v-for="(tag, idx) in formData.targets" :key="idx"
                  style="background:rgba(47,129,247,.1);color:var(--accent);border:1px solid rgba(47,129,247,.28);">
                  {{ tag }}<span class="x" @click="formData.targets.splice(idx, 1)">×</span>
                </div>
                <input class="tag-input" placeholder="回车添加…" @keydown.enter="addTag($event, 'targets')">
              </div>
            </div>

            <!-- 陪试装备 -->
            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:#58a6ff;"></span>陪试装备</div>
              <div class="tag-editor">
                <div class="tag" v-for="(tag, idx) in formData.supportEquip" :key="idx"
                  style="background:rgba(88,166,255,.1);color:#58a6ff;border:1px solid rgba(88,166,255,.28);">
                  {{ tag }}<span class="x" @click="formData.supportEquip.splice(idx, 1)">×</span>
                </div>
                <input class="tag-input" placeholder="回车添加…" @keydown.enter="addTag($event, 'supportEquip')">
              </div>
            </div>

            <!-- 试验类型 -->
            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:#bc8cff;"></span>试验类型</div>
              <div class="tag-editor">
                <div class="tag" v-for="(tag, idx) in formData.testType" :key="idx"
                  style="background:rgba(188,140,255,.1);color:#bc8cff;border:1px solid rgba(188,140,255,.28);">
                  {{ tag }}<span class="x" @click="formData.testType.splice(idx, 1)">×</span>
                </div>
                <input class="tag-input" placeholder="按回车添加（如：联合测试）…" @keydown.enter="addTag($event, 'testType')">
              </div>
            </div>

            <!-- 评估目的 -->
            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:var(--accent2);"></span>评估目的</div>
              <textarea rows="3" v-model="formData.purpose" placeholder="请输入评估目的..."></textarea>
            </div>

            <!-- 数据要求 Table -->
            <!-- <div class="fg data-req-section">
              <div class="fg-label">
                <span class="dot" style="background:var(--sat);"></span>数据要求
                <el-button link type="primary" size="small" @click="addDataReqRow" style="margin-left: auto;">+
                  添加</el-button>
              </div>
              <div class="data-req-table-wrapper">
                <el-table :data="formData.dataRequirements" border size="small" class="cyber-table data-req-el-table">
                  <el-table-column label="数据项名称" min-width="120">
                    <template #default="{ row }">
                      <el-input v-model="row.name" placeholder="如：目标方位角" size="small" />
                    </template>
</el-table-column>
<el-table-column label="量纲" width="90">
  <template #default="{ row }">
                      <el-input v-model="row.unit" placeholder="如：°" size="small" />
                    </template>
</el-table-column>
<el-table-column label="时间范围" width="200">
  <template #default="{ row }">
                      <el-date-picker v-model="row.timeRange" type="daterange" size="small" range-separator="~"
                        start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width:100%"
                        :shortcuts="timeShortcuts" />
                    </template>
</el-table-column>
<el-table-column label="数据类型" width="120">
  <template #default="{ row }">
                      <el-select v-model="row.dataType" size="small" placeholder="选择类型" style="width:100%">
                        <el-option label="遥测" value="遥测" />
                        <el-option label="遥控" value="遥控" />
                        <el-option label="测试记录" value="测试记录" />
                        <el-option label="图像视频" value="图像视频" />
                        <el-option label="日志文件" value="日志文件" />
                        <el-option label="其他" value="其他" />
                      </el-select>
                    </template>
</el-table-column>
<el-table-column label="采集要求" width="110">
  <template #default="{ row }">
                      <el-input v-model="row.freq" placeholder="如：10Hz" size="small" />
                    </template>
</el-table-column>
<el-table-column label="数据来源" width="130">
  <template #default="{ row }">
                      <el-select v-model="row.source" size="small" placeholder="选择来源" style="width:100%">
                        <el-option label="xx分中心" value="xx分中心" />
                        <el-option label="感知节点" value="感知节点" />
                        <el-option label="干扰装备" value="干扰装备" />
                        <el-option label="卫星地面站" value="卫星地面站" />
                        <el-option label="测控系统" value="测控系统" />
                        <el-option label="第三方设备" value="第三方设备" />
                      </el-select>
                    </template>
</el-table-column>
<el-table-column width="36" fixed="right">
  <template #default="{ $index }">
                      <span class="del-row" @click="formData.dataRequirements.splice($index, 1)">×</span>
                    </template>
</el-table-column>
</el-table>
</div>
</div> -->

            <div class="fg-divider"></div>

            <!-- 评估类型 & 级别 -->
            <div class="fg">
              <div class="fg-label">评估类型</div>
              <div class="pill-row">
                <div v-for="opt in ['效能', '贡献率', '成熟度', '适用性']" :key="opt" class="pill"
                  :class="{ on: formData.evalType === opt }" @click="formData.evalType = opt">{{ opt }}</div>
              </div>
            </div>
            <div class="fg">
              <div class="fg-label">评估级别</div>
              <div class="pill-row">
                <div v-for="opt in ['装备级', '系统级', '体系级', '任务级']" :key="opt" class="pill"
                  :class="{ 'on-b': formData.evalLevel === opt }" @click="formData.evalLevel = opt">{{ opt }}</div>
              </div>
            </div>
            <!-- 评估要求 -->
            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:var(--accent3);"></span>评估要求</div>
              <textarea rows="3" v-model="formData.evalReq" placeholder="请输入评估要求..."></textarea>
            </div>

            <div class="fg-divider"></div>

            <div class="fg" style="margin-top: 16px;">
              <div class="fg-label"><span class="dot" style="background:var(--sat);"></span>工作模式</div>
              <el-radio-group v-model="formData.workMode" class="cyber-radio-group" size="small">
                <el-radio-button v-for="item in workModeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </el-radio-button>
              </el-radio-group>
            </div>

            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:var(--sat);"></span>权重分配算法</div>
              <el-select v-model="formData.weightAssignAlgorithm" placeholder="体系全局未选" clearable class="cyber-select"
                style="width: 100%">
                <el-option v-for="item in weightAlgorithmOptions" :key="item.value" :label="item.label"
                  :value="item.value" />
              </el-select>
            </div>

            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:var(--sat);"></span>传导方法</div>
              <el-select v-model="formData.conductionAlgorithm" placeholder="体系全局未选" clearable class="cyber-select"
                style="width: 100%">
                <el-option v-for="item in conductionMethodOptions" :key="item.value" :label="item.label"
                  :value="item.value" />
              </el-select>
            </div>
            <div class="fg-divider"></div>
            <!-- select indicator -->
            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:var(--accent3);"></span>选择指标体系</div>
              <el-select v-model="selectedIndicator" @change="handleIndicatorChange" placeholder="请选择指标体系"
                style="width: 100%">
                <el-option v-for="item in indicatorLabels" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>
          </div>
        </div>

        <!-- ════ Right Pane: Indicator Visualization ════ -->
        <div class="right-pane">
          <IndicatorTreeWorkbench ref="workbenchRef" v-model:tree-data="indicatorTree"
            v-model:selected-node="selectedNode" variant="system" :show-template-import="true" fill-parent-height
            :preferred-root-label="formData.equips[0] || '评估需求体系'" weight-tuning-mode="full"
            :weight-assign-algorithm="formData.weightAssignAlgorithm"
            :conduction-algorithm="formData.conductionAlgorithm" :global-work-mode="formData.workMode"
            :hide-leaf-calc-method="false" @import-indicators="openImportIndicators"
            @import-template="openImportTemplate" />
        </div>
      </div>

      <!-- Stage 2 Footer Actions -->
      <div v-if="!isGenerating" class="stage-footer" :style="{ left: isLeftPaneCollapsed ? '40px' : '300px' }">
        <!-- <div class="footer-left">
          <div class="footer-button-group">
            <el-button class="tb-btn tb-outline" @click="handleAction('data')">数据要求</el-button>
            <el-button class="tb-btn tb-outline" @click="handleAction('criteria')">评估准则</el-button>
            <el-button class="tb-btn tb-outline" @click="handleAction('algorithm')">算法列表</el-button>
          </div>
        </div> -->
        <div class="tb-actions">
          <el-button class="tb-btn tb-outline" :disabled="!indicatorTree.length || objectiveWeightLoading"
            :loading="objectiveWeightLoading" @click="runObjectiveWeight">客观赋权计算</el-button>
          <el-button class="tb-btn tb-ghost" @click="confirmSave">预览</el-button>
          <el-button class="tb-btn tb-blue" @click="router.push('/major/requirement-sys/management')">返回列表</el-button>
          <el-button class="tb-btn tb-save" @click="saveDraft">确认保存</el-button>
          <el-button class="tb-btn tb-send" @click="distribute">下发会签 →</el-button>
        </div>
      </div>
    </div>

    <!-- Skeleton Detail Dialog -->
    <el-dialog v-model="showSkeletonDetails" title="任务骨架详细结构" width="800px" class="cyber-dialog">
      <div class="skeleton-tree-view" v-if="selectedSkeleton">
        <div class="tree-section">
          <div class="section-title">指标体系大纲</div>
          <el-tree :data="selectedSkeleton.details.indicators" default-expand-all />
        </div>
        <div class="tree-section">
          <div class="section-title">关联算子及模型</div>
          <div class="tag-group">
            <el-tag v-for="m in selectedSkeleton.details.models" :key="m" class="mr-1 mb-1">{{ m }}</el-tag>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 算法列表弹窗 -->
    <el-dialog v-model="algorithmDialogVisible" title="算法预置库 (仅供查看)" width="1000px" class="cyber-dialog"
      destroy-on-close>
      <div class="table-container dialog-table-wrapper">
        <el-table :data="pagedAlgorithmList" border size="small" class="cyber-table">
          <el-table-column type="index" label="序号" width="60" align="center" header-align="center"
            :index="algorithmIndexMethod" />
          <el-table-column prop="algorithmName" label="算法名称" min-width="180" header-align="center" />
          <el-table-column prop="algorithmType" label="算法类型" min-width="120" header-align="center" />
          <el-table-column prop="algorithmVersion" label="版本" width="80" align="center" header-align="center" />
          <el-table-column prop="algorithmDesc" label="算法描述" min-width="220" show-overflow-tooltip
            header-align="center" />
          <el-table-column prop="algorithmCodeUrl" label="算法路径" min-width="220" show-overflow-tooltip
            header-align="center" />
        </el-table>
      </div>

      <div class="pagination-wrapper" style="margin-top: 16px; display: flex; justify-content: flex-end;">
        <el-pagination v-model:current-page="algorithmCurrentPage" v-model:page-size="algorithmPageSize"
          :page-sizes="[10, 20, 50]" background layout="total, sizes, prev, pager, next, jumper"
          :total="algorithmList.length" />
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="algorithmDialogVisible = false" class="tb-btn tb-outline">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 客观赋权：分步进度说明（计算过程） -->
    <el-dialog v-model="objectiveWeightProgressVisible" title="客观赋权计算" width="520px" append-to-body align-center
      :close-on-click-modal="false" :close-on-press-escape="false" :show-close="objectiveWeightDone" destroy-on-close
      class="cyber-dialog" @closed="onObjectiveWeightDialogClosed">
      <div class="zhpg-owp-body">
        <el-steps direction="vertical" :active="objectiveWeightStepsActive" finish-status="success">
          <el-step title="校验与准备" description="确认指标体系已保存，并解析当前指标树结构" />
          <el-step title="组装层级数据" description="按各父节点下的子指标生成用于赋权的样本矩阵" />
          <el-step title="客观赋权计算" description="根据体系或节点上选择的赋权方法，提交至算法服务逐层计算子节点权重；指标层级多、调用次数多时，等待时间会相应变长" />
          <el-step title="回写与刷新" description="对权重做层内归一化，保存带权重的指标树快照，并刷新左侧树与中间图形" />
        </el-steps>
        <div v-if="objectiveWeightLoading" class="zhpg-owp-status"
          style="display:flex;align-items:center;gap:10px;margin-top:16px;">
          <el-icon class="is-loading" style="color:var(--accent);">
            <Loading />
          </el-icon>
          <span style="color:#7d8590;">正在计算，请稍候，勿关闭本窗口</span>
        </div>
      </div>
      <template v-if="objectiveWeightDone" #footer>
        <el-button class="tb-btn tb-outline" @click="closeObjectiveWeightProgressDialog">知道了</el-button>
      </template>
    </el-dialog>

    <!-- 从指标库导入对话框 -->
    <el-dialog title="从指标库导入" v-model="importIndicatorVisible" width="min(600px, 95vw)" append-to-body
      class="cyber-dialog">
      <el-alert type="info" :closable="false" show-icon title="选择根指标后，将自动导入该指标及其子指标形成指标树"
        style="margin-bottom: 12px;background:rgba(47,129,247,0.1);color:#58a6ff;border:1px solid rgba(47,129,247,0.28);" />
      <el-form label-width="100px">
        <el-form-item label="指标类型">
          <el-select v-model="importIndicatorType" placeholder="请先选择指标类型" clearable style="width: 100%"
            class="cyber-select" @change="loadIndicatorRoots">
            <el-option v-for="item in equipmentTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="根指标">
          <el-select v-model="importRootId" filterable placeholder="请选择根指标" :loading="importIndicatorLoading"
            style="width: 100%" class="cyber-select">
            <el-option v-for="item in importRootOptions" :key="item.id" :label="item.indicatorName" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="tb-btn tb-ghost" @click="importIndicatorVisible = false">取 消</el-button>
        <el-button class="tb-btn tb-save" @click="doImportFromIndicators" :disabled="!importRootId">导 入</el-button>
      </template>
    </el-dialog>

    <!-- 从体系模板导入对话框 -->
    <el-dialog title="从体系模板导入指标树" v-model="importTemplateVisible" width="min(600px, 95vw)" append-to-body
      class="cyber-dialog">
      <el-form label-width="100px">
        <el-form-item label="选择模板">
          <el-select v-model="importTemplateId" filterable remote reserve-keyword :remote-method="searchImportTemplates"
            :loading="importTemplateLoading" style="width: 100%" class="cyber-select" placeholder="请输入模板名称检索">
            <el-option v-for="item in importTemplateOptions" :key="item.id" :label="item.systemName"
              :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="tb-btn tb-ghost" @click="importTemplateVisible = false">取 消</el-button>
        <el-button class="tb-btn tb-save" @click="doImportFromTemplate" :disabled="!importTemplateId">导 入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  CircleCheckFilled, MagicStick, Right, ArrowRight,
  DataLine, CircleCheck, Files, Cpu, InfoFilled, Edit
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import RobotAnimation from '@/components/RobotAnimation.vue'
import { getIndicatorSystem, getAlgorithmList, getAlgorithmDetail } from '@/api/xiaoyang'
import IndicatorSystemEditor from './IndicatorSystemEditor.vue'
import {
  getEvaluationRequirement,
  addEvaluationRequirement,
  updateEvaluationRequirement,
  analysisSendTree,
} from '@/api/requirements'
import { listProjectEvaluation } from '@/api/requirements/evaluationProject'
import IndicatorTreeWorkbench from '@/views/zhpg/components/IndicatorTreeWorkbench.vue'
import { Loading } from '@element-plus/icons-vue'
import {
  objectiveWeightIndicatorSystem,
  listIndicatorSystem,
  getIndicatorSystem as getZhpgIndicatorSystem
} from '@/api/zhpg/indicatorSystem'
import { listAllIndicator, getIndicatorTree } from '@/api/zhpg/indicator'
import { ZHPG_WORK_MODE, ZHPG_WORK_MODE_OPTIONS, ZHPG_BUILD_PHASE } from '@/constants/zhpgWorkMode'
import { ZHPG_WEIGHT_ASSIGN_OPTIONS } from '@/constants/zhpgIndicatorSystemAlgorithms'
import { fetchZhpgConductionMethodSelectOptions } from '@/utils/zhpg/zhpgConductionAlgorithms'
import { parseIndicatorTreeToForest } from '@/utils/zhpg/zhpgIndicatorTreeJson'
import { setCalcMethodWorkMode } from '@/utils/zhpg/calcMethodAlgorithm'

const router = useRouter()
const route = useRoute()

// ─── Constants & UI State ───
const isLeftPaneCollapsed = ref(false)
const isIndicatorLibCollapsed = ref(false)
const isGenerating = ref(false)
let nextId = Date.now()
const generateUniqueId = () => nextId++
const rightTab = ref('tree')
const completionRate = ref(0)
const indicatorTree = ref([])
const indicatorLabels = ref([])
const indicatorTreeList = ref([])
const selectedIndicator = ref(null)
const selectedNode = ref(null)
const workbenchRef = ref(null)

const projectOptions = ref([])

const workModeOptions = ZHPG_WORK_MODE_OPTIONS
const weightAlgorithmOptions = ZHPG_WEIGHT_ASSIGN_OPTIONS
const conductionMethodOptions = ref([])

const objectiveWeightLoading = ref(false)
const objectiveWeightProgressVisible = ref(false)
const objectiveWeightStep = ref(0)
const objectiveWeightDone = ref(false)
let objectiveWeightStepTimers = []

const importIndicatorVisible = ref(false)
const importIndicatorLoading = ref(false)
const importIndicatorType = ref('')
const importRootId = ref(null)
const importRootOptions = ref([])

const importTemplateVisible = ref(false)
const importTemplateLoading = ref(false)
const importTemplateId = ref(null)
const importTemplateOptions = ref([])

const equipmentTypeOptions = [
  { label: '太空攻防', value: '太空攻防' },
  { label: '反导拦截', value: '反导拦截' },
  { label: '电子对抗', value: '电子对抗' },
  { label: '无', value: '无' }
]
const algorithmList = ref([])
const algorithmCurrentPage = ref(1)
const algorithmPageSize = ref(10)
const pagedAlgorithmList = computed(() => {
  const start = (algorithmCurrentPage.value - 1) * algorithmPageSize.value
  const end = start + algorithmPageSize.value
  return algorithmList.value.slice(start, end)
})

const algorithmIndexMethod = (index) => {
  return (algorithmCurrentPage.value - 1) * algorithmPageSize.value + index + 1
}

const algorithmDetail = ref(null)
const algorithmDialogVisible = ref(false)
const requirementId = ref(null)
const isEdit = computed(() => !!requirementId.value)

// ─── Mock Data ───


// ─── Form Data ───
const formData = ref({
  projectId: null,
  equips: [],
  targets: [],
  purpose: '',
  evalType: '效能',
  evalLevel: '系统级',
  testReq: '',
  supportEquip: [],
  testType: [],
  dataRequirements: [],
  workMode: ZHPG_WORK_MODE.INTERNAL_CIRCULATION,
  weightAssignAlgorithm: undefined,
  conductionAlgorithm: undefined
})

// ─── Methods ───
const loadRequirement = async (id) => {
  isGenerating.value = true
  try {
    const res = await getEvaluationRequirement(id)
    if (res.code === 200 && res.data) {
      const data = res.data
      requirementId.value = data.requirementId || data.id
      formData.value.projectId = data.projectId

      // 解析要素映射
      formData.value.equips = data.equipList || []
      formData.value.supportEquip = data.supportEquip || []
      formData.value.targets = data.evaluationGoal || []
      formData.value.purpose = data.evaluationPurpose || ''
      formData.value.testType = data.testType || []

      // 级别映射
      const levelMap = { '1': '装备级', '2': '系统级', '3': '体系级', '4': '任务级' }
      formData.value.evalLevel = levelMap[data.evaluationLevel] || '系统级'

      // 类型映射
      const typeMap = { '1': '效能', '2': '贡献率', '3': '成熟度', '4': '适用性' }
      formData.value.evalType = typeMap[data.evaluationType] || '效能'

      // 解析指标树
      if (data.treeData) {
        indicatorTree.value = parseIndicatorTreeToForest(data.treeData)
      } else if (data.indicatorSystem) {
        indicatorTree.value = parseIndicatorTreeToForest(data.indicatorSystem)
      }

      formData.value.workMode = data.workMode || ZHPG_WORK_MODE.INTERNAL_CIRCULATION
      formData.value.weightAssignAlgorithm = data.weightAssignAlgorithm
      formData.value.conductionAlgorithm = data.conductionAlgorithm


      completionRate.value = data.progress || 85
    }
  } catch (err) {
    ElMessage.error('加载需求详情失败')
  } finally {
    isGenerating.value = false
  }
}

const createManually = () => {
  formData.value = {
    projectId: formData.value.projectId,
    equips: [],
    targets: [],
    purpose: '',
    evalType: '效能',
    evalLevel: '系统级',
    evalReq: '',
    dataRequirements: [],
    workMode: ZHPG_WORK_MODE.INTERNAL_CIRCULATION,
    weightAssignAlgorithm: undefined,
    conductionAlgorithm: undefined
  }
  indicatorTree.value = [
  ]
  completionRate.value = 0
}

const timeShortcuts = [
  {
    text: '全程',
    value: () => [new Date(2025, 0, 1), new Date(2025, 11, 31)]
  },
  {
    text: '试验阶段',
    value: () => {
      const e = new Date()
      const s = new Date()
      s.setDate(s.getDate() - 30)
      return [s, e]
    }
  }
]

const matchColor = (percentage) => {
  if (percentage > 90) return '#00f2ff'
  if (percentage > 70) return '#e6a23c'
  return '#f56c6c'
}


const injectSkeletonIds = (list) => {
  return list.map(item => ({
    id: generateUniqueId(),
    label: item.label,
    threshold: item.threshold || '',
    children: item.children ? injectSkeletonIds(item.children) : []
  }))
}

const addTag = (event, field) => {
  const val = event.target.value.trim()
  if (val) {
    formData.value[field].push(val)
    event.target.value = ''
  }
}

const addDataReqRow = () => {
  formData.value.dataRequirements.push({ name: '', unit: '', timeRange: null, dataType: '', freq: '', source: '' })
}

const saveDraft = async () => {
  await performSave('draft')
}

const confirmSave = async () => {
  await performSave('published')
}

const performSave = async (status) => {
  // 映射评估类型
  const typeMap = { '效能': 1, '贡献率': 2, '成熟度': 3, '适用性': 4 }
  // 映射评估级别
  const levelMap = { '装备级': 1, '系统级': 2, '体系级': 3, '任务级': 4 }

  const payload = {
    requirementId: requirementId.value || undefined,
    projectId: formData.value.projectId || undefined,
    designType: 1, // 独立设计
    status: status === 'published' ? 2 : 1, // 1草稿 2发布
    equipList: formData.value.equips,
    supportEquip: formData.value.supportEquip,
    evaluationGoal: formData.value.targets,
    evaluationType: typeMap[formData.value.evalType] || 1,
    evaluationLevel: levelMap[formData.value.evalLevel] || 2,
    evaluationPurpose: formData.value.purpose,
    testType: formData.value.testType,
    workMode: formData.value.workMode,
    weightAssignAlgorithm: formData.value.weightAssignAlgorithm,
    conductionAlgorithm: formData.value.conductionAlgorithm,
    treeData: indicatorTree.value.length ? indicatorTree.value[0] : null,
    // 保留冗余字段以防旧接口需要
    requirementName: formData.value.equips[0] ? `${formData.value.equips[0]}评估需求` : '未命名评估需求',
    // requirementElements: JSON.stringify(formData.value)
  }

  try {
    let res
    if (requirementId.value) {
      res = await updateEvaluationRequirement(payload)
    } else {
      res = await addEvaluationRequirement(payload)
    }

    if (res.code === 200) {
      ElMessage.success(status === 'published' ? '需求已正式保存并发布' : '需求已保存')
      if (status === 'published') {
        router.push({
          path: '/major/requirement-sys/complete',
          query: { id: requirementId.value }
        })
      } else if (!requirementId.value) {
        requirementId.value = res.data
      }
    }
  } catch (err) {
    ElMessage.error('保存失败')
  }
}

const distribute = async () => {
  // await performSave('published')
  await analysisSendTree(requirementId.value)
  ElMessage.success('已下发至相关分中心会签')
  router.push({
    path: '/major/requirement-sys/distribute',
    query: { id: requirementId.value }
  })
}

const goToManagement = () => router.push('/process/knowledge-sys/ontology')

const handleAction = (type) => {
  const map = {
    data: '数据要求',
    criteria: '评估准则',
    algorithm: '算法列表'
  }
  // ElMessage.info(`打开${map[type]}配置`)
  // 点击算法列表 弹窗显示
  if (type === 'algorithm') {
    algorithmDialogVisible.value = true
  }
}

const getIndicatorSystemList = async () => {
  const res = await getIndicatorSystem()
  if (res.code === 200) {
    indicatorTreeList.value = res.data
    indicatorLabels.value = res.data.map(item => {
      return {
        label: item.indicatorSystemName,
        value: item.indicatorSystemId
      }
    })
  }
}

const fetchProjects = async () => {
  try {
    const res = await listProjectEvaluation({ pageNum: 1, pageSize: 100 })
    if (res.code === 200) {
      projectOptions.value = (res.data.records || []).map(item => ({
        label: item.projectName,
        value: item.projectId
      }))
      initFromQuery()
      // formData.value.projectId = projectOptions.value[0].value
    }
  } catch (err) {
    console.error('Fetch projects error:', err)
  }
}

const handleIndicatorChange = (value) => {
  console.log(value)
  const selected = indicatorTreeList.value.find(item => item.indicatorSystemId === value)
  indicatorTree.value = parseIndicatorTreeToForest(selected?.treeData || selected?.indicatorTree)
}

const getAlgorithms = async () => {
  const res = await getAlgorithmList()
  if (res.code === 200) {
    algorithmList.value = res.data
  }
}

const getAlgorithmById = async (id) => {
  const res = await getAlgorithmDetail(id)
  if (res.code === 200) {
    algorithmDetail.value = res.data
  }
}

// Lifecycle
onMounted(() => {
  fetchZhpgConductionMethodSelectOptions().then(opts => {
    conductionMethodOptions.value = opts
  }).catch(() => { })

  initFromQuery()
  fetchProjects()
  getIndicatorSystemList()
  getAlgorithms()
})

const objectiveWeightStepsActive = computed(() => {
  if (objectiveWeightDone.value) return 4
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

async function runObjectiveWeight() {
  if (!requirementId.value) {
    ElMessage.warning('请先保存需求后再进行客观赋权')
    return
  }
  if (!indicatorTree.value?.length) {
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
    const res = await objectiveWeightIndicatorSystem(requirementId.value, { persist: true, mockSampleRows: 8 })
    clearObjectiveWeightStepTimers()

    objectiveWeightStep.value = 3
    const payload = res.data || {}
    const tw = payload.indicatorTreeWeight
    if (tw) {
      indicatorTree.value = parseIndicatorTreeToForest(tw)
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

function openImportIndicators() {
  importIndicatorType.value = indicatorTree.value?.[0]?.indicatorType || ''
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
  listAllIndicator({ indicatorType: importIndicatorType.value, parentId: 0, isTemplate: 0 }).then(res => {
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
      indicatorTree.value = [convertIndicatorToTreeNode(tree)]
      ElMessage.success('导入成功')
      importIndicatorVisible.value = false
    }
  }).catch(() => {
    ElMessage.warning('导入失败，请检查指标数据')
  })
}

let uidSequence = Date.now()
function generateNodeUid() {
  return 'node_' + (++uidSequence)
}

function convertIndicatorToTreeNode(indicator) {
  const node = {
    id: indicator.idCode ? String(indicator.idCode) : generateNodeUid(),
    label: indicator.indicatorName || indicator.label || '未命名',
    indicatorType: indicator.indicatorType || undefined,
    unit: indicator.unit || '',
    description: indicator.description || '',
    valueCategory: indicator.valueCategory || undefined,
    valueMin: indicator.valueMin != null ? indicator.valueMin : undefined,
    valueMax: indicator.valueMax != null ? indicator.valueMax : undefined,
    calcMethod: setCalcMethodWorkMode(indicator.calcMethod != null ? String(indicator.calcMethod) : '', formData.value.workMode) || undefined,
    children: []
  }
  if (indicator.children && indicator.children.length > 0) {
    node.children = indicator.children.map(c => convertIndicatorToTreeNode(c))
  }
  return node
}

function openImportTemplate() {
  importTemplateId.value = null
  importTemplateOptions.value = []
  importTemplateVisible.value = true
  searchImportTemplates('')
}

function searchImportTemplates(keyword) {
  importTemplateLoading.value = true
  listIndicatorSystem({ pageNum: 1, pageSize: 50, isTemplate: 1, systemName: keyword || undefined }).then(res => {
    importTemplateOptions.value = res.rows || []
  }).finally(() => {
    importTemplateLoading.value = false
  })
}

function doImportFromTemplate() {
  if (!importTemplateId.value) return
  getZhpgIndicatorSystem(importTemplateId.value).then(res => {
    const data = res.data
    if (data && data.indicatorTree) {
      indicatorTree.value = parseIndicatorTreeToForest(data.indicatorTree)
      ElMessage.success('已导入模板指标树')
      importTemplateVisible.value = false
    } else {
      ElMessage.warning('该模板暂无指标树结构')
    }
  })
}

watch(() => route.query.id, (newId) => {
  if (newId) {
    loadRequirement(newId)
  } else {
    requirementId.value = null

    // Reset form for fresh creation
    formData.value = {
      equips: [],
      targets: [],
      purpose: '',
      evalType: '效能',
      evalLevel: '系统级',
      testReq: '',
      supportEquip: [],
      testType: [],
      dataRequirements: [],
      workMode: ZHPG_WORK_MODE.INTERNAL_CIRCULATION,
      weightAssignAlgorithm: undefined,
      conductionAlgorithm: undefined
    }
  }
})

const initFromQuery = () => {
  const requirementId = route.query.id
  const mode = route.query.mode
  const routeProjectId = route.query.projectId

  if (routeProjectId !== undefined && routeProjectId !== null && routeProjectId !== '') {
    formData.value.projectId = Number(routeProjectId)
  }

  if (requirementId) {
    loadRequirement(requirementId)
  } else if (mode === 'blank') {
    createManually()
  } else {
    // 默认执行手动创建或加载默认骨架
    createManually()
  }
}
</script>

<style scoped lang="scss">
@import './_requirement-styles.scss';
</style>
