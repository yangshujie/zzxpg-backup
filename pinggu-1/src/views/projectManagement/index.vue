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
            <dict-tag :options="projectTypeOptions" :value="scope.row.projectType" />
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
            <el-button type="primary" size="small" @click="handleEditProject(project)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDeleteProject(project)">删除</el-button>
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
              <el-tree-select v-model="form.projectType" :data="projectTypeTreeData" :props="{
                label: 'name',
                children: 'children',
                disabled: 'disabled'
              }" node-key="projectId" :disable-branch-nodes="true" placeholder="请选择工程类型" clearable filterable
                check-strictly style="width: 100%" />
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
  </div>
</template>

<script setup name="ProjectManagement">
import { listProjectEvaluation, getProjectEvaluation, addProjectEvaluation, updateProjectEvaluation, delProjectEvaluation } from '@/api/requirements/evaluationProject'
import { useRouter } from 'vue-router'
import { Document, CircleCheck, Clock, Timer } from '@element-plus/icons-vue'
import { ElTreeSelect } from 'element-plus'
import { computed } from 'vue'

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
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const currentStep = ref(0)
const showSystemCooperationTable = ref(false)
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
  ElMessage.info(`编辑工程: ${project.name}`)
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
  { title: '作战剖面构建', desc: '定义体系作战剖面和评估类型' },
  { title: '超网络模型构建', desc: '构建评估所需的超网络模型' },
  { title: '评估方案设计', desc: '设计具体的评估方案和参数' },
  { title: '数据汇总', desc: '收集和整理评估所需数据' },
  { title: '网络计算', desc: '执行网络计算和分析' },
  { title: '结果分析', desc: '分析评估结果并生成报告' }
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
  // 跳转到评估工程详情页面
  router.push(`/project/detail/${row.projectId}`)
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
  }).catch(() => { })
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
</script>

<style scoped lang="scss">
@use '@/assets/styles/v2/variables.scss' as *;

.app-container {
  padding: 20px;
}

// 数据概览卡片样式
.overview-cards {
  margin-bottom: 30px;

  .overview-card {
    background: $card-bg-color;
    border: 1px solid $border-color;
    border-radius: 8px;
    padding: 20px;
    display: flex;
    align-items: center;
    transition: all 0.3s ease;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
    }

    .card-icon {
      width: 60px;
      height: 60px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 15px;
      font-size: 24px;

      .el-icon {
        font-size: 24px;
      }
    }

    .card-content {
      flex: 1;

      .card-value {
        font-size: 28px;
        font-weight: bold;
        line-height: 1;
        margin-bottom: 5px;
      }

      .card-label {
        font-size: 14px;
        color: $text-color-secondary;
        margin-bottom: 3px;
      }

      .card-subtitle {
        font-size: 12px;
        color: $text-color-placeholder;
      }
    }

    // 不同类型卡片的颜色
    &.total {
      .card-icon {
        background: linear-gradient(135deg, #409EFF 0%, #337ecc 100%);
        color: #fff;
      }

      .card-value {
        color: #409EFF;
      }
    }

    &.completed {
      .card-icon {
        background: linear-gradient(135deg, #67C23A 0%, #529b2e 100%);
        color: #fff;
      }

      .card-value {
        color: #67C23A;
      }
    }

    &.in-progress {
      .card-icon {
        background: linear-gradient(135deg, #E6A23C 0%, #c0852e 100%);
        color: #fff;
      }

      .card-value {
        color: #E6A23C;
      }
    }

    &.pending {
      .card-icon {
        background: linear-gradient(135deg, #909399 0%, #75787c 100%);
        color: #fff;
      }

      .card-value {
        color: #909399;
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
  background: $card-bg-color;
  border: 1px solid $border-color;
  border-radius: 8px;
  padding: 15px;

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
  background: $card-bg-color;
  border: 1px solid $border-color;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 20px;

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

    &.active {
      .step-number {
        background: linear-gradient(90deg, $primary-color 0%, darken($primary-color, 15%) 100%);
        color: #ffffff;
        box-shadow: $box-shadow-neon;
      }

      .step-title {
        color: $primary-color;
        font-weight: bold;
      }
    }

    &.completed {
      .step-number {
        background: linear-gradient(90deg, $success-color 0%, darken($success-color, 15%) 100%);
        color: #ffffff;
      }

      .step-title {
        color: $success-color;
      }
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

// 其他体系工程列表样式
.other-system-projects {
  .system-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding: 15px 20px;
    background: $card-bg-color;
    border-radius: 8px;
    border: 1px solid $border-color;

    h3 {
      margin: 0;
      color: $text-color-primary;
      font-size: 18px;
    }

    .project-count {
      color: $text-color-secondary;
      font-size: 14px;
    }
  }

  .project-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 16px;

    .project-card {
      border: 1px solid $border-color;
      border-radius: 8px;
      transition: all 0.3s ease;

      &:hover {
        border-color: $primary-color;
        box-shadow: $box-shadow-neon;
        transform: translateY(-2px);
      }

      .card-header {
        padding: 16px 20px;
        border-bottom: 1px solid $border-color;

        .project-id {
          font-size: 12px;
          color: $text-color-secondary;
          margin-bottom: 4px;
        }

        .project-name {
          font-size: 16px;
          color: $text-color-primary;
          font-weight: 600;
        }
      }

      .card-content {
        padding: 16px 20px;

        .project-info {
          .info-item {
            display: flex;
            align-items: center;
            margin-bottom: 12px;

            &:last-child {
              margin-bottom: 0;
            }

            .label {
              color: $text-color-secondary;
              font-size: 14px;
              width: 80px;
              flex-shrink: 0;
            }

            .value {
              color: $text-color-primary;
              font-size: 14px;
              flex: 1;
            }

            .progress-area {
              display: flex;
              align-items: center;
              flex: 1;

              .el-progress {
                flex: 1;
                margin-right: 8px;
              }

              .progress-text {
                color: $text-color-secondary;
                font-size: 12px;
                min-width: 40px;
              }
            }
          }
        }
      }

      .card-actions {
        padding: 12px 20px;
        border-top: 1px solid $border-color;
        text-align: right;

        .el-button {
          margin-left: 8px;
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

// 表格样式优化
.el-table {
  background: transparent;

  :deep(.el-table__header-wrapper) {
    th {
      background: rgba(0, 242, 255, 0.1);
      color: $primary-color;
      font-weight: bold;
      border-bottom: 1px solid $primary-color;
    }
  }

  :deep(.el-table__body-wrapper) {
    tr:hover>td {
      background: rgba(0, 242, 255, 0.05);
    }

    td {
      border-bottom: 1px solid $border-color;
      color: $text-color-primary;
    }
  }
}

// 按钮样式优化
.el-button {
  transition: all 0.3s;

  &.el-button--primary {
    background: linear-gradient(90deg, $primary-color 0%, darken($primary-color, 15%) 100%);
    border: none;

    &:hover {
      background: linear-gradient(90deg, lighten($primary-color, 10%) 0%, $primary-color 100%);
      box-shadow: 0 0 15px rgba(0, 242, 255, 0.4);
    }
  }

  &.el-button--default {
    background: rgba(0, 242, 255, 0.1);
    border: 1px solid $primary-color;
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
</style>