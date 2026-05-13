<template>
  <div class="subpage-container">
    <div class="page-header">
      <div class="left">
        <h3 class="page-title">数据汇总整编明细</h3>
      </div>
      <div class="right">
        <el-button type="warning" plain @click="handlePrevStep">上一步 (采集表单下发)</el-button>
        <el-button type="primary" @click="handleNextStep">下一步 (数据预处理)</el-button>
      </div>
    </div>

    <!-- Stepper -->
    <el-card class="tech-card step-card" shadow="never">
      <el-steps :active="1" align-center finish-status="success">
        <el-step title="采集表单生成及下发" />
        <el-step title="数据汇总整编" />
        <el-step title="数据预处理" />
      </el-steps>
    </el-card>

    <div class="main-content" v-loading="loading">
      <!-- Task Information -->
      <el-card class="tech-card info-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>任务基础信息</span>
          </div>
        </template>
        <el-descriptions size="large" :column="3" border class="tech-descriptions">
          <el-descriptions-item label="任务名称">{{ taskInfo.taskName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="任务ID">{{ taskInfo.taskId || '--' }}</el-descriptions-item>
          <el-descriptions-item label="所属工程">{{ taskInfo.projectName || '--' }}</el-descriptions-item>

          <el-descriptions-item label="需求编号">{{ taskInfo.requirementCode || '--' }}</el-descriptions-item>
          <el-descriptions-item label="需求名称">{{ taskInfo.requirementName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="采集表进度">
            <span class="progress-link" @click="goToForms">
              已完成 {{ taskInfo.completedCollectFormCount || 0 }} / 共 {{ taskInfo.totalCollectFormCount || 0
              }}
              <el-icon style="margin-left: 4px; vertical-align: middle;">
                <Link />
              </el-icon>
            </span>
          </el-descriptions-item>

          <el-descriptions-item label="任务状态">
            <el-tag size="small" :type="getStatusType(taskInfo.status)">{{ getStatusText(taskInfo.status)
              }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="数据记录数">{{ taskInfo.recordCount !== undefined ? taskInfo.recordCount :
            '--'
            }}</el-descriptions-item>
          <el-descriptions-item label="要求截止时间">{{ taskInfo.deadlineTime || '--' }}</el-descriptions-item>

          <el-descriptions-item label="实际结束时间">{{ taskInfo.endTime || '--' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Data Table -->
      <el-card class="tech-card table-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>数据集</span>
            <div class="header-filters" style="margin-left: auto; display: flex; align-items: center; gap: 12px;">
              <span style="font-size: 13px; color: #8fa3b8;">所属表单:</span>
              <el-select v-model="filterFormId" placeholder="全部表单" clearable size="small" style="width: 200px;"
                class="tech-select">
                <el-option v-for="item in formOptions" :key="item.id" :label="item.formName" :value="item.id" />
              </el-select>
            </div>
          </div>
        </template>
        <el-table :data="detailData" class="tech-table" border stripe>
          <el-table-column prop="id" label="记录ID" width="90" />
          <el-table-column prop="indicatorCode" label="评估指标标识" show-overflow-tooltip />
          <el-table-column prop="evaluationTaskName" label="评估任务" show-overflow-tooltip />
          <el-table-column prop="datasetName" label="数据集名称" />
          <el-table-column prop="createTime" label="生成时间" />
          <el-table-column label="操作" width="100" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleShowDataDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-container">
          <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper" :total="total" @current-change="loadTableData"
            @size-change="loadTableData" />
        </div>
      </el-card>
    </div>
  </div>
  <el-dialog v-model="dataJsonDetailVisible" title="原始指标数据详情" width="900px" append-to-body class="tech-dialog">
    <el-table :data="dataJsonTableData" class="tech-table" border stripe>
      <el-table-column v-for="col in dataJsonTableColumns" :key="col" :prop="col" :label="col" show-overflow-tooltip />
    </el-table>
    <template #footer>
      <el-button @click="dataJsonDetailVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getIntegrationTaskByRequirementCode, listInterfaceInfosPage, listCollectForms } from '@/api/dataCollection/index'
import { ElMessage } from 'element-plus'
import { Link } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detailData = ref([])

const requireCode = route.query.id
const dataJsonDetailVisible = ref(false)
const dataJsonTableData = ref([])
const dataJsonTableColumns = ref([])
const taskInfo = ref({})
const filterFormId = ref(null)
const formOptions = ref([])

const loadFormOptions = () => {
  if (!requireCode) return
  listCollectForms({ requireCode: requireCode }).then(res => {
    formOptions.value = res.data.records.map(item => {
      return {
        id: item.id,
        formName: item.formName
      }
    })
  }).catch(err => {
    console.error('加载项目表单失败', err)
  })
}

watch(filterFormId, () => {
  currentPage.value = 1
  getDetail()
})

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const getStatusType = (status) => {
  const map = { 0: 'warning', 1: 'primary', 2: 'success' }
  return map[status] || 'info'
}
const getDetail = () => {
  loading.value = true
  const params = {
    evaluationTaskId: requireCode,
    pageSize: pageSize.value,
    current: currentPage.value,
    formId: filterFormId.value || undefined
  }
  listInterfaceInfosPage(params).then(res => {
    // 确保数据是数组，如果后端返回单个对象则包装成数组
    const data = res.data.records
    total.value = res.data.total
    const list = Array.isArray(data) ? data : (data ? [data] : [])
    detailData.value = list
    detailVisible.value = true
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

const getStatusText = (status) => {
  const map = { 0: '待配置数据源', 1: '进行中', 2: '已完成' }
  return map[status] || '未知'
}

const loadTaskInfo = async () => {
  if (!requireCode) return
  loading.value = true
  try {
    const res = await getIntegrationTaskByRequirementCode(requireCode)
    if (res.code === 200 && res.data) {
      taskInfo.value = res.data

    }
  } catch (err) {
    ElMessage.error('加载任务详情失败')
  } finally {
    loading.value = false
  }
}


const handlePrevStep = () => {
  const reqCode = taskInfo.value?.requirementCode
  if (reqCode) {
    router.push('/process/reception-sys/forms?id=' + reqCode)
  } else {
    router.push('/process/reception-sys/forms')
  }
}

const goToForms = () => {
  const reqCode = taskInfo.value?.requirementCode
  if (reqCode) {
    router.push('/process/reception-sys/forms?id=' + reqCode)
  } else {
    router.push('/process/reception-sys/forms')
  }
}

const handleNextStep = () => {
  if (requireCode) {
    router.push('/process/process-sys/templates?id=' + requireCode)
  } else {
    router.push('/process/process-sys/templates')
  }
}

// 处理显示 DataJson 详情（表格式展示）
const handleShowDataDetail = (row) => {
  console.log("row ", row)
  if (!row || !row.dataJson) {
    ElMessage.warning('无可用数据内容')
    return
  }
  try {
    const parsed = JSON.parse(row.dataJson)
    dataJsonTableData.value = Array.isArray(parsed) ? parsed : [parsed]
    if (dataJsonTableData.value.length > 0) {
      dataJsonTableColumns.value = Object.keys(dataJsonTableData.value[0])
    } else {
      dataJsonTableColumns.value = []
    }
    dataJsonDetailVisible.value = true
  } catch (e) {
    console.error('DataJson parse error:', e)
    ElMessage.error('解析数据内容失败')
  }
}

onMounted(() => {
  loadTaskInfo();
  loadFormOptions();
  getDetail();
})
</script>

<style scoped lang="scss">
.subpage-container {
  padding: 24px;
  height: 100%;
  overflow-y: auto;
  box-sizing: border-box;
}

.progress-link {
  color: var(--border-color-hover);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.75;
    text-decoration: underline;
  }
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

.tech-select {
  :deep(.el-input__wrapper) {
    background-color: rgba(var(--border-color-hover-rgb), 0.05) !important;
    box-shadow: 0 0 0 1px rgba(var(--border-color-hover-rgb), 0.2) inset !important;

    .el-input__inner {
      color: var(--border-color-hover) !important;
    }
  }
}

.tech-card {
  background: var(--card-bg-color) !important;
  border: 1px solid rgba(var(--border-color-hover-rgb), 0.15) !important;
  margin-bottom: 20px;
  backdrop-filter: var(--backdrop-filter);

  .card-header {
    color: var(--border-color-hover);
    font-weight: bold;
    font-size: 16px;
    display: flex;
    align-items: center;

    &::before {
      content: '';
      display: inline-block;
      width: 4px;
      height: 16px;
      background: var(--border-color-hover);
      margin-right: 8px;
    }
  }
}

.step-card {
  padding: 18px 0;

  :deep(.el-step__title.is-process) {
    color: var(--border-color-hover);
    font-weight: bold;
  }

  :deep(.el-step__head.is-process) {
    color: var(--border-color-hover);
    border-color: var(--border-color-hover);
  }

  :deep(.el-step__title.is-success) {
    color: var(--success-color);
  }

  :deep(.el-step__head.is-success) {
    color: var(--success-color);
    border-color: var(--success-color);
  }
}

/* 战术风 Descriptions */
:deep(.tech-descriptions) {
  .el-descriptions__body {
    background: transparent !important;
  }

  .el-descriptions__table {
    border-color: rgba(var(--border-color-hover-rgb), 0.1) !important;
  }

  .el-descriptions__row {
    border-color: rgba(var(--border-color-hover-rgb), 0.1) !important;
  }

  .el-descriptions__cell {
    border-color: rgba(var(--border-color-hover-rgb), 0.1) !important;
  }

  .el-descriptions__label {
    background: rgba(var(--border-color-hover-rgb), 0.05) !important;
    color: var(--text-color-secondary) !important;
    font-weight: bold;
    min-width: 120px;
  }

  .el-descriptions__content {
    background: rgba(var(--bg-color-rgb), 0.4) !important;
    color: var(--text-color-primary) !important;
  }
}

:deep(.tech-table) {
  background: transparent !important;
  color: var(--text-color-primary) !important;
  --el-table-border-color: rgba(var(--border-color-hover-rgb), 0.1) !important;
  --el-table-header-bg-color: transparent;

  th.el-table__cell {
    background: rgba(var(--border-color-hover-rgb), 0.05) !important;
    color: var(--text-color-secondary) !important;
    border-bottom: 1px solid rgba(var(--border-color-hover-rgb), 0.1) !important;
  }

  td.el-table__cell {
    border-bottom: 1px solid rgba(255, 255, 255, 0.05) !important;
    border-right: none !important;
  }

  tr {
    background: transparent !important;
  }

  tr:hover>td {
    background: rgba(var(--border-color-hover-rgb), 0.1) !important;
  }

  .el-table__empty-block {
    background: transparent !important;
  }

  .el-table__empty-text {
    color: var(--text-color-secondary) !important;
  }
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: var(--text-color-secondary);
  --el-pagination-hover-color: var(--border-color-hover);
  --el-pagination-button-color: var(--text-color-secondary);
  --el-pagination-button-disabled-bg-color: transparent;

  .el-pager li {
    background: transparent;
    border: 1px solid rgba(var(--border-color-hover-rgb), 0.2);
    margin: 0 4px;
    border-radius: 4px;

    &.is-active {
      background: rgba(var(--border-color-hover-rgb), 0.1);
      color: var(--border-color-hover);
      border-color: var(--border-color-hover);
    }
  }

  button {
    background: transparent !important;
    border: 1px solid rgba(var(--border-color-hover-rgb), 0.2) !important;
    border-radius: 4px;
    margin: 0 4px;

    &:disabled {
      border-color: rgba(255, 255, 255, 0.1) !important;
    }
  }
}
</style>
