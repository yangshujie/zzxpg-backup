<template>
  <div class="generate-req-page">
    <!-- Topbar removed from here as per request (moved to footer in Stage 2) -->

    <!-- Stage 1: Skeleton Matching -->
    <div v-if="currentStage === 1" class="stage-one-layout">
      <div class="page-header">
        <h2 class="title" style="color: #fff;">{{ isEdit ? '修改评估需求' : '生成评估需求' }}</h2>
        <div class="subtitle" style="color: #7d8590;">
          {{ isEdit ? '正在编辑现有需求要素，完成后点击发布同步更新。' : '基于智能算法，从知识库中匹配最接近当前任务的需求骨架。' }}
        </div>
      </div>
      <div class="skeleton-layout">
        <!-- Left: Top 3 List -->
        <div class="skeleton-list">
          <div class="panel-label">推荐任务骨架 (Top 3)</div>
          <div v-for="(item, index) in skeletonTop3" :key="item.id" class="skeleton-card"
            :class="{ active: selectedSkeletonId === item.id }" @click="selectedSkeletonId = item.id">
            <div class="rank-badge">Top {{ index + 1 }}</div>
            <div class="card-content">
              <div class="skeleton-name">{{ item.name }}</div>
              <div class="skeleton-desc">{{ item.desc }}</div>
            </div>
            <div class="selection-indicator">
              <el-icon v-if="selectedSkeletonId === item.id">
                <CircleCheckFilled />
              </el-icon>
            </div>
          </div>
          <!-- 手动创建按钮 -->
          <div class="manual-create-btn" @click="createManually">
            <el-icon>
              <Edit />
            </el-icon>
            手动创建需求
          </div>
        </div>


        <!-- Right: Detail Panel -->
        <div class="skeleton-detail-panel">
          <div class="panel-header">
            <div class="panel-title-wrap">
              <el-icon class="panel-icon">
                <MagicStick />
              </el-icon>
              <span class="panel-title">骨架综合评估详情</span>
            </div>
            <el-button link type="primary" @click="goToManagement" class="manage-btn">
              进入骨架管理库 <el-icon>
                <Right />
              </el-icon>
            </el-button>
          </div>

          <div class="detail-body" v-if="selectedSkeleton">
            <!-- Part 1: High-level Metrics -->
            <div class="metrics-section">
              <div class="section-label">核心评估指标</div>
              <div class="metrics-inner">
                <div class="match-score-wrap">
                  <el-progress type="dashboard" :percentage="selectedSkeleton.matchScore" :color="matchColor"
                    :width="130">
                    <template #default="{ percentage }">
                      <div class="ring-center">
                        <div class="ring-val">{{ percentage }}%</div>
                        <div class="ring-txt">匹配度</div>
                      </div>
                    </template>
                  </el-progress>
                </div>
                <div class="stats-grid">
                  <div class="stat-item">
                    <div class="si-icon"><el-icon>
                        <DataLine />
                      </el-icon></div>
                    <div class="si-content">
                      <div class="si-val">{{ selectedSkeleton.usageCount }} <small>次</small></div>
                      <div class="si-label">历史引用次数</div>
                    </div>
                  </div>
                  <div class="stat-item">
                    <div class="si-icon"><el-icon>
                        <CircleCheck />
                      </el-icon></div>
                    <div class="si-content">
                      <div class="si-val">{{ selectedSkeleton.successRate }} <small>%</small></div>
                      <div class="si-label">评估成功概率</div>
                    </div>
                  </div>
                  <div class="stat-item">
                    <div class="si-icon"><el-icon>
                        <Files />
                      </el-icon></div>
                    <div class="si-content">
                      <div class="si-val">{{ selectedSkeleton.indicatorCount }} <small>项</small></div>
                      <div class="si-label">覆盖指标总数</div>
                    </div>
                  </div>
                  <div class="stat-item">
                    <div class="si-icon"><el-icon>
                        <Cpu />
                      </el-icon></div>
                    <div class="si-content">
                      <div class="si-val">{{ selectedSkeleton.operatorCount }} <small>个</small></div>
                      <div class="si-label">预设评估算子</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Part 2: Feature Tags -->
            <div class="features-section">
              <div class="section-label">适用技术特征</div>
              <div class="tech-tags">
                <el-tag v-for="tag in selectedSkeleton.tags" :key="tag" effect="dark" size="small" class="cyber-tag">{{
                  tag
                }}</el-tag>
              </div>
              <div class="kb-info">
                <span class="kb-label">关联知识包：</span>
                <span class="kb-val">STANDARD_SPACE_COMMS_V2</span>
              </div>
            </div>

            <div class="detail-actions">
              <el-button class="view-structure-btn" @click="showSkeletonDetails = true">查看指标层级</el-button>
              <el-button type="primary" class="gen-req-btn" @click="startGeneration">
                <el-icon>
                  <MagicStick />
                </el-icon> 生成各项需求要素
              </el-button>
            </div>
          </div>
          <div class="empty-detail" v-else>
            <el-empty description="请选择骨架查看详情" :image-size="80" />
          </div>
        </div>
      </div>
    </div>

    <!-- Stage 2: Redesigned Requirement Editing -->
    <div v-else class="stage-two-container">
      <!-- AI Generation Overlay -->
      <div v-if="isGenerating" class="generating-overlay">
        <RobotAnimation />
        <div class="generating-text">正在基于《{{ selectedSkeleton.name }}》骨架生成需求表单...</div>
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
            <div class="fg-row">
              <div class="fg half">
                <div class="fg-label">评估类型</div>
                <div class="pill-row">
                  <div v-for="opt in ['效能', '贡献率', '成熟度', '适用性']" :key="opt" class="pill"
                    :class="{ on: formData.evalType === opt }" @click="formData.evalType = opt">{{ opt }}</div>
                </div>
              </div>
              <div class="fg half">
                <div class="fg-label">评估级别</div>
                <div class="pill-row">
                  <div v-for="opt in ['装备级', '系统级', '体系级', '任务级']" :key="opt" class="pill"
                    :class="{ 'on-b': formData.evalLevel === opt }" @click="formData.evalLevel = opt">{{ opt }}</div>
                </div>
              </div>
            </div>
            <!-- 评估要求 -->
            <div class="fg">
              <div class="fg-label"><span class="dot" style="background:var(--accent3);"></span>评估要求</div>
              <textarea rows="3" v-model="formData.evalReq" placeholder="请输入评估要求..."></textarea>
            </div>
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
          <IndicatorSystemEditor v-model:tree-data="indicatorTree" v-model:lib-collapsed="isIndicatorLibCollapsed" />
        </div>
      </div>

      <!-- Stage 2 Footer Actions -->
      <div v-if="!isGenerating" class="stage-footer" :style="{ left: isLeftPaneCollapsed ? '40px' : '480px' }">
        <div class="footer-left">
          <div class="footer-button-group">
            <el-button class="tb-btn tb-outline" @click="handleAction('data')">数据要求</el-button>
            <el-button class="tb-btn tb-outline" @click="handleAction('criteria')">评估准则</el-button>
            <el-button class="tb-btn tb-outline" @click="handleAction('algorithm')">算法列表</el-button>
          </div>
        </div>
        <div class="tb-actions">
          <el-button class="tb-btn tb-ghost" @click="saveDraft">💾 存为草稿</el-button>
          <el-button class="tb-btn tb-blue" @click="currentStage = 1">返回</el-button>
          <el-button class="tb-btn tb-save" @click="confirmSave">确认保存</el-button>
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
          <el-table-column type="index" label="序号" width="60" align="center" header-align="center" :index="algorithmIndexMethod" />
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
        <el-pagination
          v-model:current-page="algorithmCurrentPage"
          v-model:page-size="algorithmPageSize"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="algorithmList.length"
        />
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="algorithmDialogVisible = false" class="tb-btn tb-outline">关闭</el-button>
        </span>
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

const router = useRouter()
const route = useRoute()

// ─── Constants & UI State ───
const currentStage = ref(1)
const isLeftPaneCollapsed = ref(false)
const isIndicatorLibCollapsed = ref(false)
const isGenerating = ref(false)
const selectedSkeletonId = ref('s1')
let nextId = Date.now()
const generateUniqueId = () => nextId++
const rightTab = ref('tree')
const showSkeletonDetails = ref(false)
const completionRate = ref(0)
const indicatorTree = ref([])
const indicatorLabels = ref([])
const indicatorTreeList = ref([])
const selectedIndicator = ref(null)
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
const skeletonTop3 = ref([
  {
    id: 's1',
    name: '卫星通信干扰装备综合效能评估骨架',
    desc: '适用于宽带卫星通信、干扰装备在复杂电磁环境下的体系效能评估。',
    matchScore: 98,
    usageCount: 142,
    successRate: 95.8,
    indicatorCount: 24,
    tags: ['卫星通信', '干扰装备', '高轨卫星'],
    operatorCount: 12,
    details: {
      indicators: [
        { label: '目指精度', children: [{ label: '方位角误差' }, { label: '频率误差' }] },
        { label: '干扰响应', children: [{ label: '时延迟' }, { label: '功率密度' }] }
      ],
      models: ['链路损耗模型', '电磁干扰模型']
    }
  },
  {
    id: 's2',
    name: '复杂电磁环境通信对抗评估骨架',
    desc: '针对宽带自组网系统的效能评估及抗干扰能力分析。',
    matchScore: 85,
    usageCount: 86,
    successRate: 91.2,
    indicatorCount: 18,
    tags: ['通信对抗', '自组网', '复杂环境']
  }
])

const selectedSkeleton = computed(() => {
  const sk = skeletonTop3.value.find(s => s.id === selectedSkeletonId.value)
  return sk || skeletonTop3.value[0]
})

// ─── Form Data ───
const formData = ref({
  equips: [],
  targets: [],
  purpose: '',
  evalType: '效能',
  evalLevel: '系统级',
  testReq: '',
  supportEquip: [],
  testType: [],
  dataRequirements: []
})

// ─── Methods ───
const loadRequirement = async (id) => {
  isGenerating.value = true
  try {
    const res = await getEvaluationRequirement(id)
    if (res.code === 200 && res.data) {
      const data = res.data
      requirementId.value = data.requirementId || data.id

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
        indicatorTree.value = [data.treeData]
      } else if (data.indicatorSystem) {
        try {
          indicatorTree.value = JSON.parse(data.indicatorSystem)
        } catch (e) {
          console.error('Parse tree error:', e)
        }
      }

      currentStage.value = 2
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
    equips: [],
    targets: [],
    purpose: '',
    evalType: '效能',
    evalLevel: '系统',
    evalReq: '',
    dataRequirements: []
  }
  indicatorTree.value = [
    { id: generateUniqueId(), label: '新建评估需求', children: [] }
  ]
  completionRate.value = 0
  currentStage.value = 2
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

const startGeneration = () => {
  isGenerating.value = true
  currentStage.value = 2
  completionRate.value = 0

  if (selectedSkeleton.value?.details?.indicators) {
    indicatorTree.value = [{
      id: generateUniqueId(),
      label: selectedSkeleton.value.name,
      children: injectSkeletonIds(selectedSkeleton.value.details.indicators)
    }]
  }

  setTimeout(() => {
    isGenerating.value = false
    completionRate.value = 85
    ElMessage.success('需求要素及指标体系已基于骨架自动匹配填充')
  }, 2000)
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
    projectId: 54, // 默认关联测试工程，后续可优化为动态选择
    designType: 1, // 独立设计
    status: status === 'published' ? 2 : 1, // 1草稿 2发布
    equipList: formData.value.equips,
    supportEquip: formData.value.supportEquip,
    evaluationGoal: formData.value.targets,
    evaluationType: typeMap[formData.value.evalType] || 1,
    evaluationLevel: levelMap[formData.value.evalLevel] || 2,
    evaluationPurpose: formData.value.purpose,
    testType: formData.value.testType,
    treeData: indicatorTree.value[0] || {},
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
      ElMessage.success(status === 'published' ? '需求已正式保存并发布' : '草稿暂存成功')
      if (status === 'published') {
        router.push('/major/requirement-sys/management')
      } else if (!requirementId.value) {
        requirementId.value = res.data?.requirementId || res.data?.id
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

const handleIndicatorChange = (value) => {
  console.log(value)
  indicatorTree.value = [indicatorTreeList.value.find(item => item.indicatorSystemId === value).treeData]
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
  initFromQuery()
  getIndicatorSystemList()
  getAlgorithms()
})

watch(() => route.query.id, (newId) => {
  if (newId) {
    loadRequirement(newId)
  } else {
    requirementId.value = null
    currentStage.value = 1
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
      dataRequirements: []
    }
  }
})

const initFromQuery = () => {
  const id = route.query.id
  const mode = route.query.mode
  if (id) {
    loadRequirement(id)
  } else if (mode === 'blank') {
    createManually()
  } else {
    selectedSkeletonId.value = 's1'
  }
}
</script>

<style scoped lang="scss">
// Cyber Color Palette
:root {
  --bg: #0d1117;
  --panel: #161b22;
  --card: #1c2330;
  --border: #30363d;
  --accent: #2f81f7;
  --accent2: #3fb950;
  --accent3: #d29922;
  --jam: #ff6b35;
  --jam-bg: rgba(255, 107, 53, 0.1);
  --jam-border: rgba(255, 107, 53, 0.28);
  --sat: #bc8cff;
}

.generate-req-page {
  background: #0d1117;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: #e6edf3;
}

/* ── Stage Footer (Relocated Actions) ── */
.stage-footer {
  background: #161b22;
  border-top: 1px solid #30363d;
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  z-index: 100;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.2);
  position: fixed;
  bottom: 0;
  left: 480px;
  right: 0;

  .footer-button-group {
    display: flex;
    gap: 10px;
  }

  .tb-actions {
    display: flex;
    gap: 12px;
  }

  .tb-btn {
    height: 36px;
    padding: 0 16px;
    font-size: 13px;
    border: 1px solid #30363d;
    background: #21262d;
    color: #c9d1d9;
    transition: all 0.2s;

    &:hover {
      border-color: #8b949e;
      background: #30363d;
    }

    &.tb-blue {
      background: rgba(47, 129, 247, 0.1);
      border-color: rgba(47, 129, 247, 0.4);
      color: #58a6ff;

      &:hover {
        background: rgba(47, 129, 247, 0.2);
        border-color: #58a6ff;
      }
    }

    &.tb-save {
      background: #238636;
      border: 1px solid rgba(240, 246, 252, 0.1);
      color: #ffffff;

      &:hover {
        background: #2ea043;
      }
    }

    &.tb-send {
      background: rgba(255, 107, 53, 0.1);
      color: #ff6b35;
      border-color: rgba(255, 107, 53, 0.4);
      font-weight: bold;

      &:hover {
        background: rgba(255, 107, 53, 0.2);
        border-color: #ff6b35;
      }
    }

    &.tb-outline {
      background: transparent;
      border-color: rgba(0, 242, 255, 0.3);
      color: #00f2ff;

      &:hover {
        background: rgba(0, 242, 255, 0.1);
        border-color: #00f2ff;
      }
    }
  }
}

/* ── Stage One Layout ── */
.stage-one-layout {
  padding: 40px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;

  .page-header {
    margin-bottom: 40px;

    .title {
      font-size: 28px;
      margin-bottom: 12px;
    }
  }
}

.skeleton-layout {
  display: flex;
  gap: 30px;
}

.skeleton-list {
  width: 400px;
  display: flex;
  flex-direction: column;
  gap: 20px;

  .panel-label {
    font-size: 14px;
    color: #00f2ff;
    font-weight: 600;
  }

  .skeleton-card {
    background: rgba(22, 27, 34, 0.8);
    border: 1px solid #30363d;
    border-radius: 8px;
    padding: 24px;
    cursor: pointer;
    position: relative;

    &.active {
      border-color: #00f2ff;
      background: rgba(0, 242, 255, 0.05);
      box-shadow: 0 0 20px rgba(0, 242, 255, 0.1);
    }

    .rank-badge {
      position: absolute;
      top: 0;
      right: 0;
      background: #00f2ff;
      color: #0d1117;
      font-size: 11px;
      padding: 2px 10px;
      border-radius: 0 8px 0 8px;
      font-weight: 800;
    }

    .skeleton-name {
      font-size: 16px;
      font-weight: 700;
      color: #fff;
      margin-bottom: 10px;
    }

    .skeleton-desc {
      font-size: 13px;
      color: #7d8590;
      line-height: 1.6;
    }
  }
}

.skeleton-detail-panel {
  flex: 1;
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 12px;
  padding: 30px;
}

/* ── Stage Two Layout ── */
.stage-two-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

.requirement-layout {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* ── Left Pane ── */
.left-pane {
  width: 480px;
  background: #161b22;
  border-right: 1px solid #30363d;
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;

  &.collapsed {
    width: 40px;
    overflow: hidden;
  }
}

.lp-collapse-btn {
  margin-right: 10px;
  font-size: 16px;
  cursor: pointer;
  color: #7d8590;
  transition: color 0.2s;

  &:hover {
    color: #58a6ff;
  }
}

.lp-head {
  padding: 16px 20px;
  border-bottom: 1px solid #30363d;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.lp-head-title {
  font-size: 12px;
  font-weight: 700;
  color: #7d8590;
  letter-spacing: 1px;
}

.lp-head {
  height: 40px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #30363d;
  background: #161b22;
  flex-shrink: 0;

  .lp-head-title {
    font-size: 13px;
    font-weight: 700;
  }

  .lib-toggle-btn {
    margin-left: 12px;
    font-size: 12px;
    color: #58a6ff;
  }
}

.lp-progress {
  display: flex;
  align-items: center;
  gap: 10px;

  .lp-prog-bar {
    width: 120px;
    height: 4px;
    background: #30363d;
    border-radius: 2px;
  }

  .lp-prog-fill {
    height: 100%;
    background: linear-gradient(90deg, #ff6b35, #f85149);
    border-radius: 2px;
    transition: width 0.5s;
  }

  .lp-prog-val {
    font-family: monospace;
    font-size: 12px;
    color: #ff6b35;
  }
}

.lp-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

/* Field Groups & Layout */
.fg {
  margin-bottom: 24px;
}

.req {
  border: 1px solid #990000;
  border-radius: 6px;
  line-height: 1;
  padding: 2px;
  background: #ff00003e;
}

.fg-row {
  display: flex;
  gap: 20px;

  .half {
    flex: 1;
  }
}

.fg-label {
  font-size: 11px;
  font-weight: 700;
  color: #7d8590;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;

  .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
  }
}

textarea {
  width: 100%;
  background: #1c2330;
  border: 1px solid #30363d;
  border-radius: 6px;
  color: #e6edf3;
  padding: 12px;
  font-size: 13px;
  resize: none;
}

/* Data Requirement Table */
.data-req-table-wrapper {
  border: 1px solid #30363d;
  border-radius: 4px;
  background: #0d1117;
  overflow: hidden;
}

.data-req-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;

  th {
    background: #1c2330;
    color: #7d8590;
    text-align: left;
    padding: 8px;
    border-bottom: 1px solid #30363d;
    font-weight: normal;
  }

  td {
    padding: 6px 8px;
    border-bottom: 1px solid #30363d;
  }

  input {
    background: transparent;
    border: none;
    color: #fff;
    width: 100%;
    font-size: 12px;

    &:focus {
      outline: none;
    }
  }

  .del-row {
    color: #f85149;
    cursor: pointer;
    font-size: 16px;
    display: block;
    text-align: center;
  }
}

.tag-editor {
  background: #1c2330;
  border: 1px solid #30363d;
  border-radius: 6px;
  padding: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;

  .tag {
    padding: 4px 10px;
    border-radius: 4px;
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .tag-input {
    border: none;
    background: transparent;
    color: #fff;
    font-size: 13px;
    flex: 1;
    min-width: 80px;

    &:focus {
      outline: none;
    }
  }
}

.pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .pill {
    padding: 6px 14px;
    border: 1px solid #30363d;
    border-radius: 20px;
    font-size: 11px;
    color: #7d8590;
    cursor: pointer;
    transition: 0.2s;

    &.on {
      background: rgba(255, 107, 53, 0.1);
      border-color: #ff6b35;
      color: #ff6b35;
    }

    &.on-b {
      background: rgba(47, 129, 247, 0.1);
      border-color: #2f81f7;
      color: #2f81f7;
    }
  }
}

.lp-foot-actions {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #30363d;
  display: flex;
  gap: 12px;

  .el-button {
    flex: 1;
  }
}

/* ── Right Pane ── */
.right-pane {
  flex: 1;
  background: #0d1117;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.rp-head {
  background: #161b22;
  border-bottom: 1px solid #30363d;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.rp-head-title {
  font-size: 12px;
  font-weight: 700;
  color: #7d8590;
  letter-spacing: 1px;
}

.rp-tabs {
  display: flex;
  border: 1px solid #30363d;
  border-radius: 4px;
  overflow: hidden;

  .rp-tab {
    padding: 4px 16px;
    font-size: 12px;
    color: #7d8590;
    cursor: pointer;
    transition: 0.2s;

    &.active {
      background: #21262d;
      color: #fff;
    }

    &+.rp-tab {
      border-left: 1px solid #30363d;
    }
  }
}

.rp-toolbar {
  margin-left: auto;
  display: flex;
  gap: 12px;
}

.tree-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-image: radial-gradient(circle at 1px 1px, #1c2330 1px, transparent 0);
  background-size: 40px 40px;
}

.tree-container {
  flex: 1;
  min-height: 500px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.x6-wrapper {
  width: 100%;
  height: 100%;
}

.table-container {
  flex: 1;
  overflow: auto;
  padding: 20px;
}

/* Overlays & Animations */
.generating-overlay {
  position: absolute;
  inset: 0;
  background: rgba(13, 17, 23, 0.9);
  z-index: 200;
  backdrop-filter: blur(10px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  .generating-text {
    margin-top: 30px;
    font-size: 18px;
    color: #ff6b35;
    font-family: monospace;
  }
}

:deep(.cyber-dialog) {
  background: #161b22;

  .el-dialog__title {
    color: #fff;
  }
}

/* ── Stage One Detail Panel Refinement ── */
.skeleton-detail-panel {
  flex: 1;
  background: rgba(13, 20, 31, 0.4);
  border: 1px solid rgba(0, 242, 255, 0.2);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .panel-header {
    height: 54px;
    padding: 0 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    background: rgba(255, 255, 255, 0.02);

    .panel-title-wrap {
      display: flex;
      align-items: center;
      gap: 10px;

      .panel-icon {
        color: #00f2ff;
        font-size: 16px;
      }
    }

    .panel-title {
      font-size: 15px;
      font-weight: 700;
      color: #fff;
    }

    .manage-btn {
      font-size: 13px;
    }
  }
}

.detail-body {
  flex: 1;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;

  .section-label {
    font-size: 11px;
    font-weight: 800;
    color: #00f2ff;
    text-transform: uppercase;
    letter-spacing: 1px;
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    gap: 10px;

    &::after {
      content: '';
      flex: 1;
      height: 1px;
      background: linear-gradient(90deg, rgba(0, 242, 255, 0.2), transparent);
    }
  }
}

.metrics-inner {
  display: flex;
  align-items: center;
  gap: 40px;
}

.ring-center {
  display: flex;
  flex-direction: column;
  align-items: center;

  .ring-val {
    font-size: 26px;
    font-weight: 800;
    color: #fff;
    line-height: 1;
  }

  .ring-txt {
    font-size: 11px;
    color: #7d8590;
    margin-top: 4px;
  }
}

.stats-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-item {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.3s;

  &:hover {
    background: rgba(0, 242, 255, 0.05);
    border-color: rgba(0, 242, 255, 0.3);
    transform: translateY(-2px);
  }

  .si-icon {
    width: 32px;
    height: 32px;
    border-radius: 4px;
    background: rgba(0, 242, 255, 0.08);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #00f2ff;
    font-size: 16px;
  }

  .si-val {
    font-size: 16px;
    font-weight: 700;
    color: #fff;
    margin-bottom: 2px;

    small {
      font-size: 11px;
      opacity: 0.5;
      margin-left: 2px;
    }
  }

  .si-label {
    font-size: 11px;
    color: #7d8590;
  }
}

.tech-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.kb-info {
  font-size: 12px;
  color: #7d8590;

  .kb-val {
    color: #fff;
    opacity: 0.8;
    font-family: monospace;
  }
}

.detail-actions {
  margin-top: auto;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  gap: 16px;

  .el-button {
    height: 40px;
    font-weight: 600;
  }

  .view-structure-btn {
    flex: 1;
    background: transparent;
    border-color: #30363d;
    color: #7d8590;
  }

  .gen-req-btn {
    flex: 2;
    background: linear-gradient(135deg, #00f2ff, #0072ff);
    border: none;
    box-shadow: 0 4px 15px rgba(0, 114, 255, 0.2);
  }
}

.empty-detail {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mr-1 {
  margin-right: 8px;
}

.mb-1 {
  margin-bottom: 8px;
}

.manual-create-btn {
  margin-top: 16px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px dashed #30363d;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
  color: #7d8590;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #58a6ff;
    color: #58a6ff;
    background: rgba(88, 166, 255, 0.05);
  }
}
</style>
