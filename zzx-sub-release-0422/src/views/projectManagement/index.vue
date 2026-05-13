<template>
  <div class="app-container">
    <!-- 数据概览 -->
    <div class="overview-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="overview-card total">
            <div class="card-icon">
              <el-icon>
                <Document />
              </el-icon>
            </div>
            <div class="card-content">
              <div class="card-value">{{ overviewData.total }}</div>
              <div class="card-label">总工程数</div>
              <div class="card-subtitle">本月新增 {{ overviewData.monthlyNew }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card completed">
            <div class="card-icon">
              <el-icon>
                <CircleCheck />
              </el-icon>
            </div>
            <div class="card-content">
              <div class="card-value">{{ overviewData.completed }}</div>
              <div class="card-label">已完成</div>
              <div class="card-subtitle">完成率 {{ overviewData.completionRate }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card in-progress">
            <div class="card-icon">
              <el-icon>
                <Clock />
              </el-icon>
            </div>
            <div class="card-content">
              <div class="card-value">{{ overviewData.inProgress }}</div>
              <div class="card-label">进行中</div>
              <div class="card-subtitle">执行中 {{ overviewData.executionRate }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card pending">
            <div class="card-icon">
              <el-icon>
                <Timer />
              </el-icon>
            </div>
            <div class="card-content">
              <div class="card-value">{{ overviewData.pending }}</div>
              <div class="card-label">待启动</div>
              <div class="card-subtitle">待处理 {{ overviewData.pendingCount }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="100px">
      <el-form-item label="评估工程名称" prop="projectName">
        <el-input v-model="queryParams.projectName" placeholder="请输入评估工程名称" clearable style="width: 200px"
          @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 120px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item prop="projectType">
        <el-select v-model="queryParams.projectType" placeholder="全部体系" clearable style="width: 200px">
          <el-option v-for="item in systemOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">批量删除</el-button>
      </el-form-item>
    </el-form>



    <!-- 体系协同工程表格 -->
    <div v-if="!queryParams.projectType || queryParams.projectType === 'all'">
      <el-table v-loading="loading" :data="projectList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" fixed="left" />
        <el-table-column label="序号" type="index" width="70" fixed="left" align="center">
        </el-table-column>
        <el-table-column label="评估工程名称" align="center" prop="projectName" />
        <el-table-column label="评估工程类型" align="center" prop="projectType">
          <template #default="scope">
            <el-tag class="tactical-tag">
              {{projectTypeOptions.find(item => item.value == scope.row.projectType)?.label || scope.row.projectType}}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="任务分配" align="center" prop="taskAssignment" width="300">
        </el-table-column>
        <el-table-column label="优先级" align="center" prop="priority">
          <template #default="scope">
            <dict-tag :options="priorityOptions" :value="scope.row.priority" />
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" width="300" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看</el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
            <!-- <el-button link type="success" icon="Cpu" @click="handleCalculate(scope.row)">计算</el-button>
            <el-button link type="warning" icon="TrendCharts" @click="handleAnalyze(scope.row)">分析</el-button> -->
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />
    </div>

    <!-- 其他体系工程列表 -->
    <div v-else class="other-system-projects">
      <div class="system-header">
        <h3>{{ getSystemLabel(queryParams.projectType) }} 工程列表</h3>
        <span class="project-count">共 {{ getCurrentSystemProjects().length }} 个工程</span>
      </div>

      <div class="project-cards">
        <el-card v-for="project in getCurrentSystemProjects()" :key="project.projectId" class="project-card"
          shadow="hover">
          <div class="card-header">
            <div class="project-id">{{ project.projectId }}</div>
            <div class="project-name">{{ project.name }}</div>
          </div>

          <div class="card-content">
            <div class="project-info">
              <div class="info-item">
                <span class="label">关联需求:</span>
                <span class="value">{{ project.requirement }}</span>
              </div>
              <div class="info-item">
                <span class="label">任务进度:</span>
                <span class="value">{{ project.tasks }}</span>
              </div>
              <div class="info-item">
                <span class="label">整体进度:</span>
                <div class="progress-area">
                  <el-progress :percentage="project.progress" :color="customColors" :show-text="false" />
                  <span class="progress-text">{{ project.progress }}%</span>
                </div>
              </div>
            </div>
          </div>

          <div class="card-actions">
            <el-button type="primary" size="small" icon="View" @click="handleView(project)">查看</el-button>
            <el-button type="primary" size="small" icon="Edit" @click="handleEditProject(project)">编辑</el-button>
            <el-button type="danger" size="small" icon="Delete" @click="handleDeleteProject(project)">删除</el-button>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 新增评估工程对话框 -->
    <el-dialog :title="title" v-model="open" width="900px" append-to-body>
      <el-form ref="projectRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="工程名称" prop="projectName">
              <el-input v-model="form.projectName" placeholder="请输入工程名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工程类型" prop="projectType">
              <el-select v-model="form.projectType" placeholder="请选择工程类型" clearable filterable style="width: 100%">
                <el-option v-for="item in projectTypeOptions" :key="item.value" :label="item.label"
                  :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="工程描述" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入工程描述" maxlength="500"
            show-word-limit />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务分配" prop="tasks">
              <el-select v-model="form.tasks" multiple placeholder="请选择任务" style="width: 100%">
                <el-option v-for="item in taskOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="截止日期" prop="deadline">
              <el-date-picker v-model="form.deadline" type="date" placeholder="请选择截止日期" style="width: 100%"
                value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
        </el-row>

        <div v-if="showPriorityAndReminder">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="优先级" prop="priority">
                <el-select v-model="form.priority" placeholder="请选择优先级" style="width: 100%">
                  <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label"
                    :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="提醒信息设置" prop="reminder">
                <div style="display: flex; align-items: center;">
                  <el-button @click="openReminderDialog" size="small">设置</el-button>
                  <span v-if="form.reminder" style="margin-left: 10px; color: #909399; font-size: 12px;">
                    {{ getReminderDisplayText() }}
                  </span>
                </div>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" @click="submitForm">保 存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 提醒信息设置对话框 -->
    <el-dialog title="提醒信息设置" v-model="reminderDialogVisible" width="800px" append-to-body>
      <!-- 步骤条（基于ProjectBuild.vue的设计） -->
      <div class="process-steps-wrapper">
        <div class="process-steps">
          <div v-for="(step, index) in processSteps" :key="index" :class="['step-item']">
            <div class="step-number">{{ index + 1 }}</div>
            <div class="step-content">
              <div class="step-title">{{ step.title }}</div>
              <div class="step-desc">{{ step.desc }}</div>
              <!-- 提醒设置区域 -->
              <div class="step-reminder">
                <div class="reminder-switch">
                  <el-switch v-model="stepReminders[index].enabled" size="small" @change="updateStepReminder(index)" />
                  <span class="reminder-label">{{ stepReminders[index].enabled ? '启用提醒' : '不提醒' }}</span>
                </div>
                <el-date-picker v-if="stepReminders[index].enabled" v-model="stepReminders[index].time" type="datetime"
                  placeholder="提醒时间" size="small" style="width: 140px; margin-left: 8px;"
                  @change="updateStepReminder(index)" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="reminderDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="saveReminder">确 定</el-button>
        </div>
      </template>
    </el-dialog>
    <!-- 工程详情与流程监控对话框 -->
    <el-dialog v-model="projectDetailOpen" title="工程详情与流程监控" width="1200px" custom-class="project-detail-dialog"
      append-to-body>
      <div class="tech-detail-container">
        <!-- 基础信息头 -->
        <div class="detail-header-panel">
          <div class="header-left">
            <h2 class="project-name-title">{{ selectedProject.projectName || selectedProject.name }}</h2>
            <div class="project-tags">
              <el-tag effect="dark" size="small">{{ getSystemLabel(selectedProject.projectType) ||
                selectedProject.projectType }}</el-tag>
              <el-tag type="info" size="small" style="margin-left: 8px;">ID: {{ selectedProject.projectId ||
                selectedProject.id }}</el-tag>
            </div>
          </div>
          <div class="header-right">
            <div class="progress-box">
              <span class="label">当前进度</span>
              <div class="progress-inner">
                <el-progress :percentage="selectedProject.progress || 65" :color="customColors" />
              </div>
            </div>
          </div>
        </div>

        <!-- 核心流程区域 (SVG Redesigned) -->
        <div class="process-monitor-section">
          <div class="section-title">
            <el-icon>
              <Monitor />
            </el-icon> 核心流程
          </div>

          <div class="tactical-flow-board-vueflow" v-if="projectDetailOpen">
            <VueFlow :nodes="flowElements" :edges="flowEdges" :nodes-draggable="false" :pan-on-drag="false"
              :zoom-on-scroll="false" :zoom-on-pinch="false" :zoom-on-double-click="false" :pan-on-scroll="false"
              fit-view-on-init @pane-ready="onPaneReady" class="cyber-vue-flow">
              <Background pattern-color="var(--el-color-primary-light-5)" gap="25" />
              <Controls position="bottom-right" :show-interactive="false" />

              <!-- 自定义节点渲染 -->
              <template #node-custom="props">
                <div class="cyber-node"
                  :class="[`theme-${props.data.theme}`, `type-${props.data.type}`, { 'node-active': props.data.active }]"
                  @click="gotoStep(props.data.path)">

                  <Handle type="target" position="left" id="left-target" :style="{ opacity: 0 }" />
                  <Handle type="target" position="top" id="top-target" :style="{ opacity: 0 }" />
                  <Handle type="target" position="right" id="right-target" :style="{ opacity: 0 }" />
                  <Handle type="target" position="bottom" id="bottom-target" :style="{ opacity: 0 }" />

                  <div class="node-icon-box" v-if="props.data.icon">
                    <el-icon>
                      <component :is="props.data.icon" />
                    </el-icon>
                  </div>
                  <div class="node-content">
                    <div class="node-title">
                      <span class="node-id" v-if="props.data.idStr">{{ props.data.idStr }}</span>
                      {{ props.data.title }}
                    </div>
                    <div class="node-desc" v-if="props.data.desc">{{ props.data.desc }}</div>
                  </div>

                  <Handle type="source" position="left" id="left-source" :style="{ opacity: 0 }" />
                  <Handle type="source" position="right" id="right-source" :style="{ opacity: 0 }" />
                  <Handle type="source" position="top" id="top-source" :style="{ opacity: 0 }" />
                  <Handle type="source" position="bottom" id="bottom-source" :style="{ opacity: 0 }" />
                </div>
              </template>
            </VueFlow>
          </div>
        </div>

        <!-- 详细概览 -->
        <div class="detail-info-grid">
          <div class="info-card">
            <div class="icon"><el-icon>
                <User />
              </el-icon></div>
            <div class="content">
              <div class="label">负责人</div>
              <div class="val">{{ selectedProject.creator || '系统管理员' }}</div>
            </div>
          </div>
          <div class="info-card">
            <div class="icon"><el-icon>
                <Timer />
              </el-icon></div>
            <div class="content">
              <div class="label">截止日期</div>
              <div class="val">{{ selectedProject.deadline || '2026-12-31' }}</div>
            </div>
          </div>
          <div class="info-card wide">
            <div class="icon"><el-icon>
                <ChatDotRound />
              </el-icon></div>
            <div class="content">
              <div class="label">工程描述</div>
              <div class="val">{{ selectedProject.remark || '暂无详细描述内容' }}</div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="projectDetailOpen = false">关 闭</el-button>
        </span>
      </template>
    </el-dialog>
    <el-dialog v-model="resultRequirementDialogOpen" title="选择评估需求结果" width="560px" append-to-body>
      <el-table
        v-loading="resultRequirementLoading"
        :data="resultRequirementRows"
        class="result-requirement-table"
        max-height="420"
      >
        <el-table-column label="需求ID" prop="requirementId" width="100" align="center" />
        <el-table-column label="结果状态" min-width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="row.hasResult ? 'success' : 'info'" effect="dark">
              {{ row.hasResult ? '已有结果' : '暂无结果' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!row.hasResult" @click="openFormalResult(row)">
              查看结果
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="resultRequirementDialogOpen = false">关 闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ProjectManagement">
import { listProjectEvaluation, getProjectEvaluation, getRequirementIdsByProjectId, addProjectEvaluation, updateProjectEvaluation, delProjectEvaluation } from '@/api/requirements/evaluationProject'
import { selectIndicatorSystem } from '@/api/zhpg/indicatorSystem'
import { listEvalResult } from '@/api/zhpg/evalResult'
import { useRouter } from 'vue-router'
import { Document, CircleCheck, Clock, Timer, Monitor, Lightning, Cpu, DataAnalysis, DataBoard, User, ChatDotRound, Connection } from '@element-plus/icons-vue'
import { ElTreeSelect } from 'element-plus'
import { computed, nextTick, onMounted, reactive, ref, toRefs, getCurrentInstance } from 'vue'
import {
  EMPTY_RESULT_MESSAGE,
  RESULT_ANALYSIS_ENTRY_PATH,
  isResultAnalysisPath,
  normalizeRequirementIds,
  resolveFormalResultDecision
} from '@/utils/zhpg/projectResultAnalysisEntry'

// Vue Flow 依赖
import { VueFlow, MarkerType, Position, Handle } from '@vue-flow/core';
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';
import '@vue-flow/core/dist/style.css';
import '@vue-flow/core/dist/theme-default.css';
import '@vue-flow/controls/dist/style.css';

// ==========================================
// Mock Data (GEMINI.md Rules Applied)
// ==========================================
const MOCK_PROJECTS = [
  { projectId: 101, projectName: '深空一号效能评估', projectType: 1, remark: '针对深空探测器的作战效能进行全维度分析', tasks: '1,2', deadline: '2026-10-24', priority: 3, progress: 85, creator: '管理员' },
  { projectId: 102, projectName: '星间链路抗毁性测试', projectType: 8, remark: '评估中轨道卫星网络在强干扰环境下的连接稳定性', tasks: '3,4,5', deadline: '2026-11-15', priority: 4, progress: 45, creator: '张工' },
  { projectId: 103, projectName: '量子加密通信兼容性', projectType: 2, remark: '新一代量子密钥分发系统与现有装备的集成验证', tasks: '1,3', deadline: '2026-12-01', priority: 2, progress: 12, creator: '李工' },
  { projectId: 104, projectName: '多模态载荷融合分析', projectType: 5, remark: 'SAR、红外与光学载荷数据在联合计算中心的处理效率评估', tasks: '2,4', deadline: '2026-09-30', priority: 3, progress: 100, creator: '王工' },
  { projectId: 105, projectName: '自主避障导航系统验证', projectType: 7, remark: '基于强化学习算法的自主避障模块在复杂地形下的表现', tasks: '1,5', deadline: '2027-01-20', priority: 2, progress: 30, creator: '赵工' }
]

const { proxy } = getCurrentInstance()
const router = useRouter()

const projectList = ref([])

// 数据概览
const overviewData = ref({
  total: 8,
  monthlyNew: 2,
  completed: 4,
  completionRate: 50.0,
  inProgress: 2,
  executionRate: 25,
  pending: 2,
  pendingCount: 2
})
const open = ref(false)
const projectDetailOpen = ref(false)
const selectedProject = ref({})
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const currentStep = ref(0)
const showSystemCooperationTable = ref(false)
const resultRequirementDialogOpen = ref(false)
const resultRequirementLoading = ref(false)
const resultRequirementRows = ref([])
const priorityOptions = ref([
  { label: "低", value: 0, elTagClass: 'tag-low' },
  { label: "中", value: 1, elTagClass: 'tag-middle' },
  { label: "高", value: 2, elTagClass: 'tag-high' },
  { label: "紧急", value: 3, elTagClass: 'tag-urgent' },
])

/** 获取当前体系标签 */
const getSystemLabel = (systemValue) => {
  const system = projectTypeOptions.value.find(item => item.value === systemValue)
  return system ? system.label : '未知体系'
}

/** 获取当前体系工程列表 */
const getCurrentSystemProjects = () => {
  if (!queryParams.value.projectType || queryParams.value.projectType === 'all') {
    return []
  }
  return otherSystemProjects.value[queryParams.value.projectType] || []
}

/** 编辑其他体系工程 */
const handleEditProject = (project) => {
  reset()
  // 模拟从 project 对象获取数据
  form.value = {
    projectId: project.id || project.projectId,
    projectName: project.name || project.projectName,
    projectType: queryParams.value.projectType,
    remark: `源自 ${getSystemLabel(queryParams.value.projectType)} 的工程`,
    progress: project.progress
  }
  open.value = true
  title.value = "修改工程"
}

/** 删除其他体系工程 */
const handleDeleteProject = (project) => {
  ElMessageBox.confirm(
    `确定删除工程 "${project.name}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    ElMessage.success('删除成功')
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}
// 步骤数据（基于ProjectBuild.vue）
const processSteps = ref([
  { title: '作战剖面构建', desc: '定义体系作战剖面和评估类型', path: '/systemCooperation/combatProfile/detail' },
  { title: '超网络模型构建', desc: '构建评估所需的超网络模型', path: '/projectManagement/SuperNetworkBuild' },
  { title: '评估方案设计', desc: '设计具体的评估方案和参数', path: '/major/system-cooperation/evaluation-plan' },
  { title: '数据汇总', desc: '收集和整理评估所需数据', path: '/process/analysis-sys/tasks' },
  { title: '网络计算', desc: '执行网络计算和分析', path: '/process/mining-sys/tasks' },
  { title: '结果分析', desc: '查看已生成的评估结果', path: RESULT_ANALYSIS_ENTRY_PATH }
])

// 步骤提醒数据
const stepReminders = ref([
  { enabled: false, time: '' },
  { enabled: false, time: '' },
  { enabled: false, time: '' },
  { enabled: false, time: '' },
  { enabled: false, time: '' },
  { enabled: false, time: '' }
])

// 提醒信息设置相关数据
const reminderDialogVisible = ref(false)

/** 更新步骤提醒设置 */
const updateStepReminder = (index) => {
  const reminder = stepReminders.value[index]
  if (reminder.enabled && !reminder.time) {
    ElMessage.warning(`请为"${processSteps.value[index].title}"步骤设置提醒时间`)
    return
  }

  if (reminder.enabled) {
    ElMessage.success(`已为"${processSteps.value[index].title}"步骤设置提醒`)
  } else {
    ElMessage.info(`已关闭"${processSteps.value[index].title}"步骤提醒`)
  }
}

/** 保存提醒设置 */
const saveReminder = () => {
  // 验证所有启用的提醒是否设置了时间
  const hasError = stepReminders.value.some((reminder, index) => {
    if (reminder.enabled && !reminder.time) {
      ElMessage.warning(`请为"${processSteps.value[index].title}"步骤设置提醒时间`)
      return true
    }
    return false
  })

  if (!hasError) {
    ElMessage.success('提醒设置已保存')
    reminderDialogVisible.value = false
  }
}


// 状态选项
const statusOptions = ref([
  { value: '1', label: '待启动' },
  { value: '2', label: '进行中' },
  { value: '3', label: '已完成' },
  { value: '4', label: '已暂停' }
])

// 体系选项 (对应 API projectType)
const systemOptions = ref([
  { value: undefined, label: '全体系' },
  { value: 1, label: "链式评估" },
  { value: 2, label: "zb试验任务分析" },
  { value: 3, label: "演戏训练任务分析" },
  { value: 4, label: "考核履历表分析" },
  { value: 5, label: "qd能力对比分析" },
  { value: 6, label: "专项能力分析" },
  { value: 7, label: "辅助计算" },
  { value: 8, label: "zb体系协同指标对比分析" }
])

// 任务选项
const taskOptions = ref([
  { value: '1', label: '卫星通信网络效能评估任务' },
  { value: '2', label: '星间链路抗毁性评估任务' },
  { value: '3', label: '载荷性能测试任务' },
  { value: '4', label: '系统稳定性评估任务' },
  { value: '5', label: '作战响应时间测试任务' }
])

// 其他体系工程数据
const otherSystemProjects = ref({
  '1': [ // 测控
    { id: 'PRJ-2026-90', name: '测控效能评估', requirement: 'REQ-001', tasks: '3/5完成', progress: 60 }
  ],
  '2': [ // 通信
    { id: 'PRJ-2026-93', name: '通信网络可靠性', requirement: 'REQ-004', tasks: '5/8完成', progress: 63 }
  ],
  '3': [ // 装备
    { id: 'PRJ-2026-96', name: '装备综合效能', requirement: 'REQ-007', tasks: '1/2完成', progress: 50 }
  ]
})

// 评估工程类型选项 (API 定义)
const projectTypeOptions = ref([
  { value: 1, label: "链式评估" },
  { value: 2, label: "zb试验任务分析" },
  { value: 3, label: "yx训练任务分析" },
  { value: 4, label: "考核履历表分析" },
  { value: 5, label: "qd能力对比分析" },
  { value: 6, label: "专项能力分析" },
  { value: 7, label: "辅助计算" },
  { value: 8, label: "zb体系协同指标对比分析" }
])

// 评估超网络选项
const superNetworkOptions = ref([
  { value: '1', label: '体系作战剖面' },
  { value: '2', label: '装备体系力量结构网' },
  { value: '3', label: '体系作战任务网' },
  { value: '4', label: 'OODA网' },
  { value: '5', label: '指标网' },
  { value: '6', label: '算法网' }
])

// 进度条颜色
const customColors = ref([
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#5cb87a', percentage: 60 },
  { color: '#1989fa', percentage: 80 },
  { color: '#6f7ad3', percentage: 100 }
])

// 工程类型树形数据
const projectTypeTreeData = computed(() => {
  return projectTypeOptions.value.map(item => {
    return {
      id: item.value,
      name: item.label,
      children: []
    }
  })
})

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectName: undefined,
    projectType: undefined,
    status: undefined
  },
  rules: {
    projectName: [{ required: true, message: "工程名称不能为空", trigger: "blur" }],
    projectType: [{ required: true, message: "工程类型不能为空", trigger: "change" }],
    remark: [{ required: true, message: "工程描述不能为空", trigger: "blur" }],
    tasks: [{ required: true, message: "任务分配不能为空", trigger: "change" }],
    deadline: [{ required: true, message: "截止日期不能为空", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

// 计算属性：是否显示优先级和提醒信息设置
const showPriorityAndReminder = computed(() => {
  // 综合分析类和辅助计算不需要优先级和提醒信息设置
  const hideTypes = ['装备试验任务综合分析', '演习训练任务综合分析', '装备考核履历分析', '强敌能力对比分析', '专项能力分析', '辅助计算']
  return !hideTypes.includes(form.value.type)
})

/** 查询工程列表 */
function getList() {
  loading.value = true
  listProjectEvaluation(queryParams.value).then(response => {
    console.log(response, "===")
    projectList.value = response.data?.records || []
    total.value = response.data?.total || 0
    loading.value = false
  }).catch(() => {
    // 接口报错时返回 Mock 数据
    projectList.value = [...MOCK_PROJECTS]
    total.value = MOCK_PROJECTS.length
    loading.value = false
    console.warn('API error encountered. Falling back to MOCK_PROJECTS.')
  })
}


/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.projectId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "新增评估工程"
  currentStep.value = 0
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const projectId = row.projectId || ids.value[0]
  getProjectEvaluation(projectId).then(response => {
    form.value = response.data
    // 处理任务字符串转数组
    if (form.value.tasks && typeof form.value.tasks === 'string') {
      form.value.tasks = form.value.tasks.split(',')
    }
    open.value = true
    title.value = "修改评估工程"
  }).catch(() => {
    // 接口报错时返回模拟数据
    const mockDetail = MOCK_PROJECTS.find(p => p.projectId === projectId) || MOCK_PROJECTS[0]
    form.value = { ...mockDetail }
    // 处理任务字符串转数组
    if (form.value.tasks && typeof form.value.tasks === 'string') {
      form.value.tasks = form.value.tasks.split(',')
    }
    open.value = true
    title.value = "修改评估工程 (Mock Data)"
    console.warn(`API error for project ${projectId}. Using mock detail.`)
  })
}

/** 查看按钮操作 */
function handleView(row) {
  selectedProject.value = { ...row }
  // 如果是从卡片列表查看，可能没有 projectType，从查询参数中补全
  if (!selectedProject.value.projectType && queryParams.value.projectType) {
    selectedProject.value.projectType = queryParams.value.projectType
  }
  projectDetailOpen.value = true
}

function getSelectedProjectId() {
  return selectedProject.value.projectId || selectedProject.value.id
}

function normalizeListResponse(response) {
  const data = response?.data ?? response?.rows ?? response
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.rows)) return data.rows
  if (Array.isArray(data?.records)) return data.records
  return []
}

function pickLatestResult(results) {
  return [...results]
    .filter(item => item?.id)
    .sort((a, b) => {
      const aTime = new Date(a.updateTime || a.createTime || 0).getTime()
      const bTime = new Date(b.updateTime || b.createTime || 0).getTime()
      return bTime - aTime
    })[0] || null
}

async function loadLatestFormalResultForRequirement(requirementId) {
  const selectRes = await selectIndicatorSystem({ requirementId })
  const indicatorSystems = normalizeListResponse(selectRes)
  const candidates = []

  for (const system of indicatorSystems) {
    const indicatorSystemId = system.indicatorSystemId || system.id
    if (!indicatorSystemId) continue

    try {
      const resultRes = await listEvalResult({
        pageNum: 1,
        pageSize: 1,
        indicatorSystemId
      })
      const result = normalizeListResponse(resultRes)[0]
      if (result?.id) {
        candidates.push({
          ...result,
          indicatorSystemId,
          indicatorSystemName: result.indicatorSystemName || system.indicatorSystemName || system.systemName || system.name,
          templateId: result.templateId,
          templateName: result.templateName
        })
      }
    } catch {
      // 单个指标体系查询失败不阻断其他候选结果。
    }
  }

  return pickLatestResult(candidates)
}

async function resolveRequirementResultRow(requirementId) {
  try {
    const result = await loadLatestFormalResultForRequirement(requirementId)
    return {
      requirementId,
      hasResult: !!result?.id,
      evalResultId: result?.id || null,
      resultCode: result?.resultCode || null,
      resultName: result?.resultName || result?.taskName || null,
      templateId: result?.templateId || null,
      templateName: result?.templateName || null,
      indicatorSystemId: result?.indicatorSystemId || null,
      indicatorSystemName: result?.indicatorSystemName || '--',
      lastRunTime: result?.updateTime || result?.createTime || null
    }
  } catch {
    return {
      requirementId,
      hasResult: false,
      indicatorSystemName: '--'
    }
  }
}

async function handleResultAnalysisEntry() {
  const projectId = getSelectedProjectId()
  if (!projectId) {
    ElMessage.warning('未找到当前工程ID')
    return
  }

  resultRequirementLoading.value = true
  try {
    const res = await getRequirementIdsByProjectId(projectId)
    const requirementIds = normalizeRequirementIds(res)
    if (!requirementIds.length) {
      ElMessage.warning('当前工程暂无关联评估需求')
      return
    }

    const rows = await Promise.all(requirementIds.map(resolveRequirementResultRow))
    resultRequirementRows.value = rows
    resultRequirementDialogOpen.value = true
  } catch (e) {
    ElMessage.error(e?.message || '获取工程关联需求失败')
  } finally {
    resultRequirementLoading.value = false
  }
}

function openFormalResult(row) {
  const decision = resolveFormalResultDecision({
    projectId: getSelectedProjectId(),
    requirementId: row.requirementId,
    context: row
  })

  if (decision.type !== 'route') {
    ElMessage.warning(decision.message || EMPTY_RESULT_MESSAGE)
    return
  }

  resultRequirementDialogOpen.value = false
  projectDetailOpen.value = false
  router.push(decision.route)
}

/** 流程步骤跳转 */
function gotoStep(path) {
  if (!path) return;
  if (isResultAnalysisPath(path)) {
    handleResultAnalysisEntry()
    return
  }
  const isMain = selectedProject.value.projectType === 'zb体系协同指标对比分析' || selectedProject.value.projectType == 8
  const projectId = getSelectedProjectId();
  if (isMain) {
    const baseRouter = window.microApp?.router?.getBaseAppRouter()
    if (baseRouter) {
      baseRouter.push({
        path: path,
        query: {
          projectId: selectedProject.value.projectId,
          projectName: selectedProject.value.projectName
        }
      });
    } else {
      window.location.href = path
    }
    return;
  } else {
    router.push({
      path: path,
      query: { projectId: projectId }
    });
  }


  projectDetailOpen.value = false;
}

/** 删除按钮操作 */
function handleDelete(row) {
  const deleteIds = row?.projectId || ids.value
  const msg = typeof deleteIds === 'object' ? `编号为 [${deleteIds.join(', ')}] 的项目` : `该条数据`
  proxy.$modal.confirm(`是否确认删除${msg}吗？`).then(function () {
    return delProjectEvaluation(deleteIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch((err) => {
    // 如果是确认框取消，不提示错误
    if (err === 'cancel') return;

    // 模拟删除成功
    proxy.$modal.msgSuccess("删除成功 (Mock)")
    // 定向从本地列表中移除 (可选，这里简单调用 getList 触发逻辑)
    getList()
    console.warn('Delete API failed. Mocking success.')
  })
}

/** 计算按钮操作 */
function handleCalculate(row) {
  proxy.$modal.msgSuccess("开始计算：" + row.profileName)
}

/** 分析按钮操作 */
function handleAnalyze(row) {
  proxy.$modal.msgSuccess("开始分析：" + row.profileName)
}

/** 查看报告操作 */
function handleViewReport(row) {
  proxy.$modal.msgSuccess("查看报告：" + row.profileName)
}

/** 下一步 */
function nextStep() {
  if (currentStep.value < processSteps.value.length - 1) {
    currentStep.value++
  }
}

/** 上一步 */
function prevStep() {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["projectRef"].validate(valid => {
    if (valid) {
      // 将数组转换为字符串 (如果 API 需要)
      const submitData = { ...form.value }
      if (Array.isArray(submitData.tasks)) {
        submitData.tasks = submitData.tasks.join(',')
      }

      // 优先级映射
      const priorityMap = { low: 1, medium: 2, high: 3, urgent: 4 }
      if (submitData.priority) {
        submitData.priority = priorityMap[submitData.priority] || submitData.priority
      }

      if (submitData.projectId != null) {
        updateProjectEvaluation(submitData).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        }).catch(() => {
          // 模拟成功
          proxy.$modal.msgSuccess("修改成功 (Mock)")
          open.value = false
          console.warn('Update API failed. Mocking success.')
        })
      } else {
        addProjectEvaluation(submitData).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
          // 如果返回了 id，跳转
          if (response.data?.projectId) {
            router.push(`/project/detail/${response.data.projectId}`)
          }
        }).catch(() => {
          // 模拟成功
          proxy.$modal.msgSuccess("新增成功 (Mock)")
          open.value = false
          console.warn('Add API failed. Mocking success.')
        })
      }
    }
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
  currentStep.value = 0
}

/** 打开提醒信息设置对话框 */
function openReminderDialog() {
  reminderDialogVisible.value = true
  reminderCurrentStep.value = 0

  // 如果已有提醒信息，则填充表单
  if (form.value.reminder) {
    try {
      const reminderData = JSON.parse(form.value.reminder)
      reminderForm.value = { ...reminderData }
    } catch {
      reminderForm.value = {
        enabled: true,
        time: ''
      }
    }
  } else {
    reminderForm.value = {
      enabled: true,
      time: ''
    }
  }
}

/** 下一步提醒设置 */
function nextReminderStep() {
  if (reminderCurrentStep.value < reminderSteps.value.length - 1) {
    reminderCurrentStep.value++
  }
}

/** 上一步提醒设置 */
function prevReminderStep() {
  if (reminderCurrentStep.value > 0) {
    reminderCurrentStep.value--
  }
}

/** 获取提醒信息显示文本 */
function getReminderDisplayText() {
  if (!form.value.reminder) return ''

  try {
    const reminderData = JSON.parse(form.value.reminder)
    if (!reminderData.enabled) {
      return '提醒已关闭'
    }
    if (reminderData.time) {
      return `提醒时间: ${reminderData.time}`
    }
    return '提醒已启用'
  } catch {
    return form.value.reminder
  }
}

/** 表单重置 */
function reset() {
  form.value = {
    projectId: undefined,
    projectName: undefined,
    projectType: undefined,
    remark: undefined,
    tasks: [],
    deadline: undefined,
    priority: undefined,
    reminder: undefined,
    progress: 0,
    creator: undefined
  }
  proxy.resetForm("projectRef")
}

/** 初始化 **/
onMounted(() => {
  // 默认显示所有工程
  queryParams.value.projectType = undefined
  showSystemCooperationTable.value = true
  getList()
})
// ====== VueFlow Nodes Logic ======
const makeNode = (id, x, y, title, icon, theme, type, path = '', desc = '', idStr = '01', active = false) => ({
  id,
  type: 'custom',
  position: { x, y },
  data: { title, icon, theme, type, path, desc, idStr, active }
});

const makeEdge = (source, target, theme, dashed = false, edgeType = 'straight', sourceHandle = 'right-source', targetHandle = 'left-target') => {
  let colorMap = {
    cyan: '#00f2ff',
    orange: '#e3b341',
    purple: '#bc8cff',
    red: '#f85149',
    gray: '#5c7080'
  }
  let color = colorMap[theme] || '#00f2ff';
  return {
    id: `e-${source}-${sourceHandle}-${target}-${targetHandle}`,
    source,
    target,
    sourceHandle,
    targetHandle,
    type: edgeType,
    animated: dashed,
    style: { stroke: color, strokeWidth: 2, strokeDasharray: dashed ? '5 5' : 'none' },
    markerEnd: MarkerType.ArrowClosed
  }
};

const flowElements = computed(() => {
  const pType = selectedProject.value.projectType;
  let elements = [];

  if (pType === 'zb体系协同指标对比分析' || pType == 8) {
    // 第一排从左到右 (x: 0, 380, 760)
    // 第二排蛇形走位从右到左 (x: 760, 380, 0)
    processSteps.value.forEach((step, index) => {
      let x, y;
      if (index < 3) {
        x = index * 380;
        y = 0;
      } else {
        x = (5 - index) * 380; // 3->760, 4->380, 5->0
        y = 140; // 缩减到140，适应降低的画布高度
      }
      const icon = index < 1 ? 'CircleCheck' : 'Clock';
      const theme = index === 1 ? 'cyan' : 'gray';
      const active = index === 1;
      elements.push(makeNode(`n${index}`, x, y, step.title, icon, theme, 'action', step.path, step.desc, `0${index + 1}`, active));
    });
  } else if (pType === '链式评估' || pType == 1 || selectedProject.value.id === 101) {
    elements.push(makeNode('m1', 0, 0, '需求设计', 'Document', 'cyan', 'action', '/major/requirement-sys/portal', '', '01', false));
    elements.push(makeNode('m2', 360, 0, '数据采集及汇总', 'DataBoard', 'cyan', 'action', '/process/reception-sys/forms', '', '02', true));
    elements.push(makeNode('m3', 720, 0, '综合分析计算', 'Cpu', 'gray', 'action', '/process/mining-sys/tasks', '', '03', false));
    elements.push(makeNode('m4', 1080, 0, '结果分析', 'DataAnalysis', 'gray', 'action', RESULT_ANALYSIS_ENTRY_PATH, '', '04', false));
  } else {
    elements.push(makeNode('o1', 0, 0, '参数配置', 'Connection', 'gray', 'action', '/major/program-mgmt/build', '工程环境与模型初始化', '01', false));
    elements.push(makeNode('o2', 380, 0, '数据采集及汇总', 'DataBoard', 'cyan', 'action', '/process/reception-sys/forms', '多源同步与自动化入库', '02', true));
    elements.push(makeNode('o3', 760, 0, '结果分析', 'DataAnalysis', 'gray', 'action', RESULT_ANALYSIS_ENTRY_PATH, '评估报告与知识固化', '03', false));
  }
  return elements;
});

const flowEdges = computed(() => {
  const pType = selectedProject.value.projectType;
  let edges = [];

  if (pType === 'zb体系协同指标对比分析' || pType == 8) {
    edges.push(makeEdge('n0', 'n1', 'cyan', false, 'straight', 'right-source', 'left-target'));
    edges.push(makeEdge('n1', 'n2', 'cyan', false, 'straight', 'right-source', 'left-target'));

    // 蛇形绕线，从右上节点底部连接到右下节点顶部，完美直线
    edges.push(makeEdge('n2', 'n3', 'cyan', false, 'straight', 'bottom-source', 'top-target'));

    // 右到左直连
    edges.push(makeEdge('n3', 'n4', 'cyan', false, 'straight', 'left-source', 'right-target'));
    edges.push(makeEdge('n4', 'n5', 'cyan', false, 'straight', 'left-source', 'right-target'));
  } else if (pType === '链式评估' || pType == 1 || selectedProject.value.id === 101) {
    edges.push(makeEdge('m1', 'm2', 'cyan', false, 'straight'));
    edges.push(makeEdge('m2', 'm3', 'cyan', false, 'straight'));
    edges.push(makeEdge('m3', 'm4', 'cyan', false, 'straight'));
  } else {
    edges.push(makeEdge('o1', 'o2', 'cyan', false, 'straight'));
    edges.push(makeEdge('o2', 'o3', 'cyan', false, 'straight'));
  }
  return edges;
});

const onPaneReady = (vueFlowInstance) => {
  // 等待弹窗展开动画完成，再执行自适应居中缩放
  setTimeout(() => {
    vueFlowInstance.fitView({ padding: 0.1, duration: 600 });
  }, 150);
};

</script>

<style scoped lang="scss">
@use '@/assets/styles/v2/variables.scss' as *;

.app-container {
  padding: 20px;
}

.overview-cards {
  margin-top: 24px;
  margin-bottom: 24px;

  .overview-card {
    background: $surface-low; // surface_container_low
    backdrop-filter: $backdrop-filter;
    border: none; // No-Line rule
    border-radius: $border-radius-base;
    padding: 24px;
    display: flex;
    align-items: center;
    gap: 20px;
    transition: all 0.4s cubic-bezier(0.165, 0.84, 0.44, 1);
    position: relative;
    overflow: hidden;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 4px;
      height: 100%;
      background: $cyber-gradient;
      opacity: 0.8;
    }

    &:hover {
      background: lighten($surface-low, 2%);
      box-shadow: $diffused-shadow;
      transform: translateY(-4px);
    }

    .card-icon {
      width: 60px;
      height: 60px;
      background: rgba($primary-container, 0.08);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28px;
      color: $primary-container;
      border: 1px solid rgba($primary-container, 0.15); // Ghost Border
    }

    .card-content {
      flex: 1;

      .card-value {
        font-size: 32px;
        font-weight: 700;
        color: #fff;
        line-height: 1;
        margin-bottom: 8px;
        font-family: 'Space Grotesk', sans-serif;
        letter-spacing: -0.02em;
      }

      .card-label {
        font-size: 14px;
        font-weight: 500;
        color: $text-color-secondary;
        margin-bottom: 4px;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .card-subtitle {
        font-size: 14px; // Min 14px
        color: rgba($text-color-placeholder, 0.8);
      }
    }

    /* Variant Colors */
    &.total {
      &::before {
        background: $primary-color;
      }

      .card-icon {
        color: $primary-color;
        background: rgba($primary-color, 0.1);
        border-color: rgba($primary-color, 0.2);
      }
    }

    &.completed {
      &::before {
        background: $success-color;
      }

      .card-icon {
        color: $success-color;
        background: rgba($success-color, 0.1);
        border: 1px solid rgba($success-color, 0.2);
      }
    }

    &.in-progress {
      &::before {
        background: $warning-color;
      }

      .card-icon {
        color: $warning-color;
        background: rgba($warning-color, 0.1);
        border: 1px solid rgba($warning-color, 0.2);
      }
    }

    &.pending {
      &::before {
        background: $danger-color;
      }

      .card-icon {
        color: $danger-color;
        background: rgba($danger-color, 0.1);
        border: 1px solid rgba($danger-color, 0.2);
      }
    }
  }
}

.tech-detail-container {
  padding: 30px;
  max-height: 80vh;
  overflow-y: auto;

  /* Header Panel */
  .detail-header-panel {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding-bottom: 25px;
    margin-bottom: 30px;
    border-bottom: 1px dashed rgba(164, 230, 255, 0.2);

    .project-name-title {
      margin: 0 0 10px 0;
      font-size: 28px;
      color: #fff;
      text-shadow: 0 0 15px rgba(0, 209, 255, 0.5);
    }

    .project-tags {
      display: flex;
      align-items: center;
    }

    .progress-box {
      text-align: right;
      width: 240px;

      .label {
        font-size: 12px;
        color: rgba(164, 230, 255, 0.6);
        text-transform: uppercase;
        letter-spacing: 1px;
        margin-bottom: 8px;
        display: block;
      }
    }
  }

  /* Section Title */
  .section-title {
    font-size: 18px;
    color: #a4e6ff;
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 30px;
    font-weight: 600;

    .el-icon {
      font-size: 20px;
      filter: drop-shadow(0 0 5px rgba(0, 209, 255, 0.8));
    }
  }

  /* Tactical Flow Board (Vue Flow) */
  .tactical-flow-board-vueflow {
    background: rgba(20, 28, 40, 0.6); // Slightly brighter than surface-low
    border-radius: 8px;
    padding: 10px;
    margin-bottom: 30px;
    border: 1px solid rgba(164, 230, 255, 0.15);
    position: relative;
    height: 260px;
    /* Reduced from 380px to fix canvas being too tall */
    box-shadow: 0 5px 20px rgba(0, 0, 0, 0.3), inset 0 0 20px rgba(0, 242, 255, 0.05);

    .cyber-vue-flow {
      width: 100%;
      height: 100%;
    }
  }

  :deep(.vue-flow__edge-path) {
    filter: drop-shadow(0 0 3px currentColor);
  }

  /* Custom Node styling */
  .cyber-node {
    display: flex;
    flex-direction: column;
    padding: 12px 16px;
    border-radius: 6px;
    /* Smooth corners for glass effect */
    width: 240px;
    height: 100px;
    /* Frosted Glass Base */
    background: linear-gradient(135deg, rgba(24, 73, 117, 0.25), rgba(11, 28, 46, 0.4));
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    border: 2px solid rgba(0, 209, 255, 0.25);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3), inset 0 0 15px rgba(0, 209, 255, 0.1);
    color: #fff;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.165, 0.84, 0.44, 1);
    position: relative;
    overflow: hidden;
    box-sizing: border-box;

    .node-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .node-id {
        font-family: 'Space Grotesk', sans-serif;
        font-size: 13px;
        font-weight: 700;
        color: rgba(164, 230, 255, 0.85);
        text-transform: uppercase;
      }

      .node-icon-box {
        font-size: 24px;
        color: rgba(164, 230, 255, 0.9);
      }
    }

    .node-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: center;
      text-align: left;

      .node-title {
        font-size: 16px;
        font-weight: 600;
        margin-bottom: 4px;
        letter-spacing: 0.5px;
        word-break: break-word;
        text-shadow: 0 0 5px rgba(0, 209, 255, 0.3);
      }

      .node-desc {
        font-size: 13px;
        color: rgba(255, 255, 255, 0.75);
        display: -webkit-box;
        -webkit-line-clamp: 1;
        line-clamp: 1;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }
    }

    &:hover {
      transform: translateY(-4px);
      z-index: 100;
      /* Hover glow */
      background: linear-gradient(135deg, rgba(31, 102, 160, 0.35), rgba(15, 40, 65, 0.5));
      border-color: rgba(0, 209, 255, 0.5);
    }

    /* Active State */
    &.node-active {
      /* Crystal glass bright active */
      background: linear-gradient(135deg, rgba(0, 242, 255, 0.2), rgba(0, 162, 255, 0.1)) !important;
      border: 2px solid rgba(0, 242, 255, 0.8) !important;
      box-shadow: 0 0 25px rgba(0, 242, 255, 0.3), inset 0 0 30px rgba(0, 242, 255, 0.2);

      .node-title,
      .node-id,
      .node-icon-box {
        color: #00f2ff !important;
        text-shadow: 0 0 10px rgba(0, 242, 255, 0.8);
        font-weight: 700;
      }

      .node-desc {
        color: rgba(255, 255, 255, 0.9) !important;
      }
    }
  }

  /* Themes applying colors */
  .theme-cyan {
    border-color: rgba(0, 242, 255, 0.35);

    .node-icon-box {
      color: #00f2ff;
      filter: drop-shadow(0 0 5px rgba(0, 242, 255, 0.8));
    }

    &:hover {
      border-color: #00f2ff;
      box-shadow: 0 0 20px rgba(0, 242, 255, 0.2), inset 0 0 20px rgba(0, 242, 255, 0.1);
    }
  }

  .theme-gray {
    border-color: rgba(164, 230, 255, 0.2);
    background: linear-gradient(135deg, rgba(35, 45, 60, 0.25), rgba(15, 22, 32, 0.35));

    .node-title {
      color: rgba(255, 255, 255, 0.9);
    }

    &:hover {
      border-color: rgba(164, 230, 255, 0.5);
      box-shadow: 0 0 15px rgba(164, 230, 255, 0.15);
    }
  }

  /* 详细概览底栏 (Detail Info Grid) */
  .detail-info-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
    margin-top: 10px;

    .info-card {
      display: flex;
      align-items: center;
      background: rgba(20, 28, 40, 0.6);
      border: 1px solid rgba(164, 230, 255, 0.15);
      border-radius: 8px;
      padding: 16px 20px;
      transition: all 0.3s ease;

      &:hover {
        background: rgba(20, 28, 40, 0.9);
        border-color: rgba(0, 242, 255, 0.4);
        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3), inset 0 0 10px rgba(0, 242, 255, 0.1);
        transform: translateY(-2px);
      }

      .icon {
        width: 48px;
        height: 48px;
        background: rgba(0, 242, 255, 0.1);
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 16px;
        color: #00f2ff;
        font-size: 24px;
        border: 1px solid rgba(0, 242, 255, 0.25);
        box-shadow: inset 0 0 10px rgba(0, 242, 255, 0.1);
      }

      .content {
        flex: 1;

        .label {
          font-size: 13px;
          color: rgba(164, 230, 255, 0.6);
          margin-bottom: 6px;
          text-transform: uppercase;
          letter-spacing: 1px;
        }

        .val {
          font-size: 15px;
          color: #fff;
          font-weight: 600;
          line-height: 1.4;
        }
      }

      &.wide {
        grid-column: span 2;
      }
    }
  }
}





.el-form-item {
  margin-bottom: 15px;
}

.mb8 {
  margin-bottom: 8px;
}

.empty-table {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.network-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;

  .network-tag {
    background: rgba(0, 242, 255, 0.1);
    border: 1px solid $primary-color;
    color: $primary-color;
    font-size: 12px;
    padding: 2px 6px;
    border-radius: 3px;
  }
}

// 步骤条样式
.process-steps-wrapper {
  margin-bottom: 20px;
}

.process-steps {
  display: flex;
  background: $surface-low;
  border: none;
  border-radius: 8px;
  padding: 15px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);

  .step-item {
    display: flex;
    align-items: center;
    flex: 1;
    padding: 0 10px;
    position: relative;

    &:not(:last-child)::after {
      content: '';
      position: absolute;
      right: 0;
      top: 50%;
      width: 1px;
      height: 60%;
      background: $border-color;
      transform: translateY(-50%);
    }

    .step-number {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: $border-color;
      color: $text-color-secondary;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      margin-right: 10px;
      transition: all 0.3s;
    }

    .step-content {
      .step-title {
        font-size: 14px;
        color: $text-color-primary;
        margin-bottom: 2px;
      }

      .step-desc {
        font-size: 12px;
        color: $text-color-secondary;
      }

      /* 步骤提醒设置样式 */
      .step-reminder {
        margin-top: 10px;
        padding-top: 10px;
        border-top: 1px dashed $border-color;

        .reminder-switch {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .reminder-label {
          font-size: 12px;
          color: $text-color-secondary;
        }

        .el-date-picker {
          margin-top: 8px;
        }
      }
    }
  }
}

// 提醒信息设置弹窗样式
.reminder-process-steps {
  display: flex;
  background: $surface-low;
  border: none;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);

  .step-item {
    display: flex;
    align-items: center;
    flex: 1;
    padding: 0 10px;
    position: relative;

    &:not(:last-child)::after {
      content: '';
      position: absolute;
      right: 0;
      top: 50%;
      width: 1px;
      height: 60%;
      background: rgba(164, 230, 255, 0.1);
      transform: translateY(-50%);
    }

    &.active {
      .step-number {
        background: $cyber-gradient;
        color: #000;
        box-shadow: $box-shadow-neon;
      }

      .step-title {
        color: $primary-container;
        font-weight: bold;
      }
    }

    &.completed {
      .step-number {
        background: linear-gradient(90deg, $success-color 0%, darken($success-color, 15%) 100%);
        color: #000;
      }

      .step-title {
        color: $success-color;
      }
    }

    .step-number {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: $surface-high;
      color: $text-color-secondary;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      margin-right: 12px;
      transition: all 0.3s;
      font-family: 'Space Grotesk', sans-serif;
    }

    .step-content {
      .step-title {
        font-size: 14px;
        color: $text-color-primary;
        margin-bottom: 2px;
      }

      .step-desc {
        font-size: 14px;
        color: rgba($text-color-secondary, 0.7);
      }
    }
  }
}

.reminder-step-content {
  min-height: 150px;

  .step-panel {
    padding: 20px;

    .step-panel-header {
      font-size: 16px;
      color: $text-color-primary;
      margin-bottom: 20px;
      font-weight: bold;
      border-left: 3px solid $primary-color;
      padding-left: 10px;
    }

    .reminder-switch {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 80px;
    }
  }
}

.other-system-projects {
  .system-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding: 20px 24px;
    background: $surface-low;
    border-radius: 8px;
    border: none;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);

    h3 {
      margin: 0;
      color: #fff;
      font-size: 20px;
      font-family: 'Space Grotesk', sans-serif;
      letter-spacing: 0.02em;
    }

    .project-count {
      color: $primary-color;
      font-size: 14px;
      background: rgba(164, 230, 255, 0.1);
      padding: 4px 12px;
      border-radius: 12px;
    }
  }

  .project-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 16px;

    .project-card {
      background: $surface-low;
      border: none;
      border-radius: 8px;
      transition: all 0.4s cubic-bezier(0.165, 0.84, 0.44, 1);
      box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
      margin-bottom: 8px;

      &:hover {
        background: lighten($surface-low, 2%);
        box-shadow: $diffused-shadow;
        transform: translateY(-4px);
      }

      .card-header {
        padding: 20px 24px;
        border-bottom: 1px solid rgba(164, 230, 255, 0.05);

        .project-id {
          font-size: 14px;
          color: $primary-color;
          font-family: 'Space Grotesk', sans-serif;
          margin-bottom: 4px;
          text-transform: uppercase;
          letter-spacing: 0.05em;
        }

        .project-title {
          font-size: 18px;
          color: #fff;
          font-weight: 600;
        }
      }

      .card-content {
        padding: 20px 24px;

        .project-info {
          .info-item {
            display: flex;
            align-items: center;
            margin-bottom: 14px;

            &:last-child {
              margin-bottom: 0;
            }

            .label {
              color: rgba($text-color-secondary, 0.8);
              font-size: 14px;
              width: 90px;
              flex-shrink: 0;
            }

            .value {
              color: $text-color-primary;
              font-size: 14px;
              flex: 1;
              font-family: 'Inter', sans-serif;
            }
          }
        }
      }

      .card-actions {
        padding: 16px 24px;
        border-top: 1px solid rgba(164, 230, 255, 0.05);
        text-align: right;
        background: rgba(255, 255, 255, 0.02);

        .el-button {
          margin-left: 12px;
        }
      }
    }
  }
}

// 分页样式优化
:deep(.el-pagination) {
  margin-top: 20px;
  text-align: right;

  .btn-prev,
  .btn-next,
  .number {
    background: $card-bg-color;
    border: 1px solid $border-color;
    color: $text-color-primary;
    margin: 0 4px;
    border-radius: 4px;

    &:hover {
      border-color: $primary-color;
      color: $primary-color;
      box-shadow: $box-shadow-neon;
    }

    &.active {
      background: linear-gradient(90deg, $primary-color 0%, darken($primary-color, 15%) 100%);
      border-color: $primary-color;
      color: #ffffff;
      box-shadow: $box-shadow-neon;
    }

    &.disabled {
      background: rgba(255, 255, 255, 0.05);
      border-color: $border-color;
      color: $text-color-placeholder;
      cursor: not-allowed;
    }
  }

  .el-pagination__jump {
    color: $text-color-primary;

    .el-input__wrapper {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid $border-color;
      color: $text-color-primary;

      &:hover {
        border-color: $primary-color;
      }
    }
  }

  .el-pagination__total {
    color: $text-color-secondary;
    margin-right: 10px;
  }
}

// 表格样式优化 - No-Line style
.el-table {
  background: transparent !important;
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: #{$surface-low};
  --el-table-row-hover-bg-color: rgba(164, 230, 255, 0.05);

  :deep(.el-table__header-wrapper) {
    th {
      background: $surface-low !important;
      color: $primary-color;
      font-family: 'Space Grotesk', sans-serif;
      text-transform: uppercase;
      letter-spacing: 0.1em;
      border-bottom: none !important;
      font-size: 14px;
    }
  }

  :deep(.el-table__body-wrapper) {
    tr:hover>td {
      background: rgba(164, 230, 255, 0.08) !important;
    }

    td {
      border-bottom: 1px solid rgba(164, 230, 255, 0.05) !important; // Ghost line
      color: $text-color-primary;
      font-size: 14px;
      padding: 12px 0;
    }
  }

  &::before {
    display: none !important;
  }
}

// 按钮样式优化
.el-button {
  transition: all 0.3s cubic-bezier(0.165, 0.84, 0.44, 1);
  border-radius: $border-radius-base;

  &.el-button--primary {
    background: $cyber-gradient !important;
    border: none !important;
    color: #000 !important;
    font-weight: 700;

    &:hover {
      box-shadow: $box-shadow-neon;
      transform: translateY(-2px);
    }
  }

  &.el-button--default {
    background: rgba(164, 230, 255, 0.1);
    border: 1px solid rgba(164, 230, 255, 0.3);
    color: $primary-color;

    &:hover {
      background: rgba(0, 242, 255, 0.2);
      box-shadow: 0 0 10px rgba(0, 242, 255, 0.3);
    }
  }
}

// 弹窗样式优化
.el-dialog {
  background: $card-bg-color;
  border: 1px solid $border-color;
  border-radius: 8px;

  .el-dialog__header {
    border-bottom: 1px solid $border-color;
    color: $text-color-primary;
    font-weight: bold;
  }

  .el-dialog__body {
    color: $text-color-primary;
  }

  .el-dialog__footer {
    border-top: 1px solid $border-color;
  }
}

// 战术标签样式 - Tactical Tag
.tactical-tag {
  background: rgba(164, 230, 255, 0.05) !important;
  border: 1px solid rgba(164, 230, 255, 0.2) !important; // Ghost Border
  color: $primary-color !important;
  font-family: 'Space Grotesk', sans-serif;
  font-weight: 500;
  font-size: 13px;
  padding: 4px 10px;
  height: auto;
  line-height: 1.2;
  border-radius: 4px;
  transition: all 0.3s ease;
  backdrop-filter: blur(4px);

  &:hover {
    background: rgba(164, 230, 255, 0.15) !important;
    border-color: $primary-color !important;
    box-shadow: 0 0 10px rgba(0, 242, 255, 0.2);
    transform: scale(1.02);
  }
}
</style>
