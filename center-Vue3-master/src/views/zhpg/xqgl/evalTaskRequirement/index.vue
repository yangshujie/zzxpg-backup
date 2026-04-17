<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="需求编号" prop="code">
        <el-input
          v-model="queryParams.code"
          placeholder="请输入需求编号"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="需求名称" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入需求名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 140px">
          <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-select v-model="queryParams.priority" placeholder="请选择优先级" clearable style="width: 120px">
          <el-option v-for="item in priorityOptions" :key="item" :label="item" :value="item" />
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
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleBatchDelete">删除</el-button>
        <el-button type="warning" plain icon="Download" @click="handleExport">导出</el-button>
      </div>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </div>

    <!-- 数据表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      table-layout="fixed"
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" align="center" />
      <el-table-column label="需求编号" prop="code" width="140" show-overflow-tooltip />
      <el-table-column label="需求名称" prop="title" min-width="220" show-overflow-tooltip />
      <el-table-column label="需求来源" prop="source" min-width="150" show-overflow-tooltip />
      <el-table-column label="归属模板" prop="template" min-width="180" show-overflow-tooltip />
      <el-table-column label="优先级" prop="priority" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getPriorityTagType(row.priority)" size="small">{{ row.priority }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="责任人" prop="owner" width="100" align="center" />
      <el-table-column label="期望完成" prop="deadline" width="120" align="center" />
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" icon="View" @click="handleView(row)">详情</el-button>
          <el-button link type="primary" size="small" icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" icon="Delete" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="750px" append-to-body :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="需求编号" prop="code">
              <el-input v-model="form.code" placeholder="自动生成或手动输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="需求来源" prop="source">
              <el-input v-model="form.source" placeholder="请输入需求来源" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="需求名称" prop="title">
          <el-input v-model="form.title" placeholder="请输入需求名称" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="归属模板" prop="template">
              <el-select v-model="form.template" placeholder="请选择归属模板" style="width: 100%" clearable>
                <el-option v-for="item in templateOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="form.priority" placeholder="请选择优先级" style="width: 100%">
                <el-option v-for="item in priorityOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="责任人" prop="owner">
              <el-input v-model="form.owner" placeholder="请输入责任人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="期望完成" prop="deadline">
              <el-date-picker v-model="form.deadline" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="需求说明" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="请输入需求说明" />
        </el-form-item>
        <el-form-item label="依赖数据源" prop="dataNeed">
          <el-input v-model="form.dataNeed" type="textarea" :rows="2" placeholder="请输入依赖数据源，多个用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="需求详情" width="750px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="需求编号">{{ currentRow.code }}</el-descriptions-item>
        <el-descriptions-item label="需求来源">{{ currentRow.source }}</el-descriptions-item>
        <el-descriptions-item label="归属模板">{{ currentRow.template }}</el-descriptions-item>
        <el-descriptions-item label="优先级">
          <el-tag :type="getPriorityTagType(currentRow.priority)" size="small">{{ currentRow.priority }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="责任人">{{ currentRow.owner }}</el-descriptions-item>
        <el-descriptions-item label="期望完成">{{ currentRow.deadline }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(currentRow.status)" size="small">{{ currentRow.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="需求说明" :span="2">{{ currentRow.summary }}</el-descriptions-item>
        <el-descriptions-item label="依赖数据源" :span="2">{{ currentRow.dataNeed }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const { proxy } = getCurrentInstance()

// 演示数据
const demoRows = ref([
  {
    id: 1,
    code: 'REQ-2026-001',
    title: '补充联合侦察打击任务效能评估需求',
    source: '总体设计会审',
    template: '作战效能评估任务模板',
    priority: '高',
    status: '待分派',
    owner: '李工',
    deadline: '2026-04-10',
    summary: '新增联合侦察打击场景，要求评估任务链完成质量与目标达成效率。',
    dataNeed: '任务执行日志、链路状态数据、态势回放数据',
  },
  {
    id: 2,
    code: 'REQ-2026-002',
    title: '增加体系贡献率专题评估需求',
    source: '分系统对接',
    template: '体系贡献率评估任务模板',
    priority: '中',
    status: '已分派',
    owner: '王工',
    deadline: '2026-04-15',
    summary: '补充体系节点的直接贡献与协同贡献评估要求。',
    dataNeed: '体系关系网、任务网络、资源保障数据',
  },
  {
    id: 3,
    code: 'REQ-2026-003',
    title: '补齐任务满足度缺口诊断需求',
    source: '试验阶段复盘',
    template: '作战任务满足度评估任务模板',
    priority: '高',
    status: '研判中',
    owner: '周工',
    deadline: '2026-04-12',
    summary: '要求形成需求缺口分级和整改建议联动。',
    dataNeed: '需求清单、缺口诊断结果、整改建议单',
  },
  {
    id: 4,
    code: 'REQ-2026-004',
    title: '新增专项能力评估需求',
    source: '用户反馈',
    template: '专项能力评估任务模板',
    priority: '低',
    status: '已闭环',
    owner: '吴工',
    deadline: '2026-04-20',
    summary: '新增通用质量特性评估场景。',
    dataNeed: '质量特性测试数据',
  },
  {
    id: 5,
    code: 'REQ-2026-005',
    title: '关键因素敏感性分析评估',
    source: '技术评审',
    template: '关键因素影响分析任务模板',
    priority: '中',
    status: '待分派',
    owner: '郑工',
    deadline: '2026-04-18',
    summary: '需要对影响作战效能的关键因素进行敏感性分析。',
    dataNeed: '关键因素列表、扰动范围数据',
  },
  {
    id: 6,
    code: 'REQ-2026-006',
    title: '增强数据预处理能力',
    source: '分系统对接',
    template: '作战效能评估任务模板',
    priority: '高',
    status: '已分派',
    owner: '李工',
    deadline: '2026-04-08',
    summary: '增强多源异构数据的清洗和预处理能力。',
    dataNeed: '原始数据样本、数据格式说明',
  },
  {
    id: 7,
    code: 'REQ-2026-007',
    title: '完善评估报告模板',
    source: '总体设计会审',
    template: '作战效能评估任务模板',
    priority: '中',
    status: '研判中',
    owner: '陈工',
    deadline: '2026-04-25',
    summary: '完善评估报告的标准化模板和自动生成能力。',
    dataNeed: '历史报告样本、报告模板需求文档',
  },
  {
    id: 8,
    code: 'REQ-2026-008',
    title: '优化算法权重分配策略',
    source: '试验阶段复盘',
    template: '体系贡献率评估任务模板',
    priority: '高',
    status: '待分派',
    owner: '刘工',
    deadline: '2026-04-14',
    summary: '根据专家意见优化权重分配算法。',
    dataNeed: '历史权重数据、专家评估意见',
  },
  {
    id: 9,
    code: 'REQ-2026-009',
    title: '扩展指标体系覆盖范围',
    source: '用户反馈',
    template: '作战效能评估任务模板',
    priority: '低',
    status: '已闭环',
    owner: '赵工',
    deadline: '2026-04-22',
    summary: '扩展现有指标体系，覆盖更多评估维度。',
    dataNeed: '现有指标体系文档、扩展需求说明',
  },
])

const loading = ref(false)
const showSearch = ref(true)
const tableData = ref([])
const total = ref(0)
const ids = ref([])
const multiple = ref(true)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  code: '',
  title: '',
  status: '',
  priority: '',
})

const statusOptions = ['待分派', '研判中', '已分派', '已闭环']
const priorityOptions = ['高', '中', '低']
const templateOptions = [
  '作战效能评估任务模板',
  '体系贡献率评估任务模板',
  '作战任务满足度评估任务模板',
  '专项能力评估任务模板',
  '关键因素影响分析任务模板',
]

const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const currentRow = ref({})

const initFormData = () => ({
  id: undefined,
  code: '',
  title: '',
  source: '',
  template: '',
  priority: '中',
  status: '待分派',
  owner: '',
  deadline: '',
  summary: '',
  dataNeed: '',
})

const form = ref(initFormData())

const rules = {
  title: [{ required: true, message: '请输入需求名称', trigger: 'blur' }],
  source: [{ required: true, message: '请输入需求来源', trigger: 'blur' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

function getPriorityTagType(priority) {
  const map = { '高': 'danger', '中': 'warning', '低': 'info' }
  return map[priority] || 'info'
}

function getStatusTagType(status) {
  const map = { '待分派': 'warning', '研判中': 'info', '已分派': 'primary', '已闭环': 'success' }
  return map[status] || 'info'
}

/** 查询列表 */
function getList() {
  loading.value = true
  setTimeout(() => {
    let result = [...demoRows.value]
    const { code, title, status, priority } = queryParams.value

    if (code) {
      result = result.filter(r => r.code.includes(code))
    }
    if (title) {
      result = result.filter(r => r.title.includes(title))
    }
    if (status) {
      result = result.filter(r => r.status === status)
    }
    if (priority) {
      result = result.filter(r => r.priority === priority)
    }

    tableData.value = result
    total.value = result.length
    if (result.length > 0 && !currentRow.value.code) {
      currentRow.value = result[0]
    }
    loading.value = false
  }, 300)
}

/** 搜索 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置 */
function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    code: '',
    title: '',
    status: '',
    priority: '',
  }
  handleQuery()
}

/** 多选 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  multiple.value = !selection.length
}

/** 新增 */
function handleAdd() {
  reset()
  dialogTitle.value = '新增评估任务需求'
  dialogVisible.value = true
  // 自动生成编号
  const maxCode = demoRows.value.reduce((max, r) => {
    const num = parseInt(r.code.replace('REQ-2026-', ''))
    return num > max ? num : max
  }, 0)
  form.value.code = `REQ-2026-${String(maxCode + 1).padStart(3, '0')}`
}

/** 编辑 */
function handleEdit(row) {
  reset()
  dialogTitle.value = '编辑评估任务需求'
  form.value = { ...row }
  dialogVisible.value = true
}

/** 查看 */
function handleView(row) {
  currentRow.value = row
  viewDialogVisible.value = true
}

/** 提交 */
function submitForm() {
  proxy.$refs.formRef.validate(valid => {
    if (!valid) return
    submitLoading.value = true
    setTimeout(() => {
      if (form.value.id) {
        const index = demoRows.value.findIndex(r => r.id === form.value.id)
        if (index !== -1) {
          demoRows.value[index] = { ...form.value }
        }
        ElMessage.success('修改成功')
      } else {
        const newId = Math.max(...demoRows.value.map(r => r.id)) + 1
        demoRows.value.unshift({ ...form.value, id: newId })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      getList()
      submitLoading.value = false
    }, 500)
  })
}

/** 重置表单 */
function reset() {
  form.value = initFormData()
  proxy.resetForm('formRef')
}

/** 删除 */
function handleDelete(row) {
  const delIds = row.id ? [row.id] : ids.value
  proxy.$modal.confirm(`确认删除选中的 ${delIds.length} 条需求？`).then(() => {
    delIds.forEach(id => {
      const index = demoRows.value.findIndex(r => r.id === id)
      if (index !== -1) demoRows.value.splice(index, 1)
    })
    ElMessage.success('删除成功')
    getList()
  }).catch(() => {})
}

function handleBatchDelete() {
  if (ids.value.length === 0) {
    ElMessage.warning('请先选择要删除的需求')
    return
  }
  handleDelete({})
}

/** 导出 */
function handleExport() {
  proxy.download('/zhpg/evalTaskRequirement/export', { ...queryParams.value }, `评估任务需求_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped>
.toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>
