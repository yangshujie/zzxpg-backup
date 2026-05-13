<template>
  <div class="subpage-container">
    <div class="page-header">
      <div class="left">
        <h3 class="page-title">汇总整编任务管理</h3>
      </div>
      <div class="right">
        <el-select v-model="statusFilter" placeholder="任务状态" clearable style="width: 160px; margin-right: 12px;">
          <el-option label="待配置数据源" :value="0" />
          <el-option label="进行中" :value="1" />
          <el-option label="已完成" :value="2" />
        </el-select>
        <el-input v-model="searchQuery" placeholder="搜索任务名称" clearable style="width: 240px; margin-right: 12px;"
          prefix-icon="Search" @keyup.enter="getTasks" />
        <el-button type="primary" icon="Search" @click="getTasks">查询</el-button>
        <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">批量删除</el-button>
        <el-button type="primary" icon="Plus" @click="handleAdd">新建任务</el-button>
      </div>
    </div>

    <el-card class="tech-card">
      <el-table :data="tableData" class="tech-table" style="width: 100%" v-loading="loading"
        @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <!-- <el-table-column prop="taskId" label="任务ID" width="80" /> -->
        <el-table-column prop="taskName" label="任务名称" min-width="280" show-overflow-tooltip />
        <el-table-column prop="projectId" label="工程ID" width="100" />
        <el-table-column prop="projectName" label="工程名称" width="100" />
        <el-table-column prop="requirementCode" label="需求编号" width="100" />
        <el-table-column prop="requirementName" label="需求名称" width="280" />
        <el-table-column label="进度" width="120">
          <template #default="{ row }">
            <span class="progress-text">
              <span class="progress-done">{{ row.completedCollectFormCount || 0 }}</span> / {{ row.totalCollectFormCount
                || 0
              }}
            </span>
          </template>
        </el-table-column>
        <!-- <el-table-column prop="createTime" label="创建时间" width="180" /> -->
        <el-table-column label="关联采集表" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="router.push('/process/reception-sys/forms?id=' + row.requirementCode)">
              查看采集表单
            </el-button>
          </template>
        </el-table-column>
        <!-- <el-table-column prop="status" label="状态" width="140">
                    <template #default="{ row }">
                        <el-tag size="small" :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
                    </template>
                </el-table-column> -->
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <!-- <el-button v-if="row.status === 0" link type="primary" @click="handleEdit(row)">编辑</el-button> -->
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">

        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @current-change="getTasks"
          @size-change="getTasks" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑任务' : '新建任务'" width="700px" destroy-on-close>
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="formData.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="所属工程" prop="projectId">
          <el-select v-model="formData.projectId" placeholder="请选择评估工程" style="width: 100%;">
            <el-option v-for="item in projectList" :key="item.id" :label="item.projectName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联表单" prop="formId">
          <el-select v-model="formData.formId" placeholder="请选择采集表单" style="width: 100%;">
            <el-option v-for="item in collectFormList" :key="item.id" :label="item.formName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据集ID" prop="datasetId">
          <el-input-number v-model="formData.datasetId" :min="0" placeholder="请输入数据集ID" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="数据源类型" prop="sourceType">
          <el-select v-model="formData.sourceType" placeholder="请选择数据源类型" style="width: 100%;">
            <el-option label="数据库" value="DATABASE" />
            <el-option label="文件" value="FILE" />
            <el-option label="API接口" value="API" />
          </el-select>
        </el-form-item>
        <el-form-item label="源格式" prop="sourceFormat">
          <el-select v-model="formData.sourceFormat" placeholder="请选择源格式" style="width: 100%;">
            <el-option label="CSV" :value="1" />
            <el-option label="Excel" :value="2" />
            <el-option label="JSON" :value="3" />
            <el-option label="XML" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="Cron表达式" prop="cronExpression">
          <el-input v-model="formData.cronExpression" placeholder="请输入调度Cron表达式" />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入任务描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="采集数据记录详情" width="1100px" custom-class="tech-dialog">

    </el-dialog>

    <!-- 指标数据内容详情表格弹窗 -->
    <el-dialog v-model="dataJsonDetailVisible" title="原始指标数据详情" width="900px" append-to-body class="tech-dialog">
      <el-table :data="dataJsonTableData" class="tech-table" border stripe>
        <el-table-column v-for="col in dataJsonTableColumns" :key="col" :prop="col" :label="col"
          show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button @click="dataJsonDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  listIntegrationTasks,
  addIntegrationTask, updateIntegrationTask,
  delIntegrationTasks, triggerIntegrationTask,
  listInterfaceInfosPage,
} from "@/api/dataCollection/index"

const router = useRouter()

const loading = ref(false)
const searchQuery = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const selectedRows = ref([])

const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const formData = reactive({
  id: null,
  taskName: '',
  projectId: null,
  formId: null,
  datasetId: null,
  sourceType: '',
  sourceFormat: null,
  cronExpression: '',
  description: ''
})

// 指标数据内容详情相关
const dataJsonDetailVisible = ref(false)
const dataJsonTableData = ref([])
const dataJsonTableColumns = ref([])

const projectList = ref([])
const collectFormList = ref([])

const rules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }]
}



const getTasks = () => {
  loading.value = true
  const params = {
    pageNum: currentPage.value,
    pageSize: pageSize.value,
    taskName: searchQuery.value || undefined,
    status: statusFilter.value !== '' ? statusFilter.value : undefined
  }
  listIntegrationTasks(params).then(res => {
    tableData.value = res.data?.records || res.data?.list || []
    total.value = res.data?.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleAdd = () => {
  isEdit.value = false
  Object.keys(formData).forEach(key => {
    if (Array.isArray(formData[key])) {
      formData[key] = []
    } else if (typeof formData[key] === 'number') {
      formData[key] = null
    } else {
      formData[key] = ''
    }
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  formData.id = row.id
  formData.taskName = row.taskName
  formData.projectId = row.projectId
  formData.formId = row.formId
  formData.datasetId = row.datasetId
  formData.sourceType = row.sourceType
  formData.sourceFormat = row.sourceFormat
  formData.cronExpression = row.cronExpression
  formData.description = row.description
  dialogVisible.value = true
}

const submitForm = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      const api = isEdit.value ? updateIntegrationTask : addIntegrationTask
      api(formData).then(() => {
        ElMessage.success(isEdit.value ? '编辑成功' : '新建成功')
        dialogVisible.value = false
        getTasks()
      })
    }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该任务吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    delIntegrationTasks(row.id).then(() => {
      ElMessage.success('删除成功')
      getTasks()
    })
  })
}

const handleBatchDelete = () => {
  const ids = selectedRows.value.map(row => row.id).join(',')
  ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 个任务吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    delIntegrationTasks(ids).then(() => {
      ElMessage.success('批量删除成功')
      getTasks()
    })
  })
}

const handleDetail = (row) => {
  router.push('/process/analysis-sys/task-detail?id=' + row.requirementCode)
}


onMounted(() => {
  getTasks()
})
</script>

<style scoped lang="scss">
.subpage-container {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .page-title {
    margin: 0;
    font-size: 24px;
    font-weight: bold;
    color: var(--text-color-primary);
    border-left: 4px solid var(--border-color-hover);
    padding-left: 12px;
  }
}

.tech-card {
  background: var(--card-bg-color) !important;
  border: 1px solid rgba(var(--border-color-hover-rgb), 0.1) !important;
}

:deep(.tech-table) {
  background: transparent !important;
  color: var(--text-color-primary) !important;

  th.el-table__cell {
    background: rgba(var(--border-color-hover-rgb), 0.05) !important;
    color: var(--text-color-secondary) !important;
    border-bottom: 1px solid rgba(var(--border-color-hover-rgb), 0.1) !important;
  }

  td.el-table__cell {
    border-bottom: 1px solid rgba(var(--text-color-primary-rgb), 0.05) !important;
  }

  .el-table-fixed-column--right,
  .el-table-fixed-column--left {
    background-color: var(--sider-bg-color) !important;
  }

  th.el-table-fixed-column--right,
  th.el-table-fixed-column--left {
    background-color: var(--header-bg-color) !important;
  }
}

.progress-text {
  font-size: 13px;
  color: var(--text-color-secondary);

  .progress-done {
    color: var(--border-color-hover);
    font-weight: bold;
    font-size: 15px;
  }
}

.count-link {
  color: var(--border-color-hover);
  cursor: pointer;
  text-decoration: underline;
  text-decoration-style: dashed;
  text-underline-offset: 4px;
  font-weight: bold;
  font-size: 14px;

  &:hover {
    color: var(--text-color-primary);
  }
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}
</style>