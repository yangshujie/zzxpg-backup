<template>
  <div class="app-container">
    <div class="page-header mb12">
      <h2 class="page-title">流程模板管理</h2>
      <p class="page-desc">维护模板名称、任务类型与四阶段编排；指标体系在计算执行时传入，不再在模板中绑定。</p>
    </div>

    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="110px">
      <el-form-item label="模板名称" prop="templateName">
        <el-input
          v-model="queryParams.templateName"
          placeholder="请输入模板名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="评估任务类型" prop="taskType">
        <el-select v-model="queryParams.taskType" placeholder="全部评估任务类型" clearable style="width: 180px">
          <el-option v-for="item in taskTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 130px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar-row mb8">
      <div class="toolbar-btns">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新建流程模板</el-button>
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleBatchDelete">删除</el-button>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="templateList"
      row-key="id"
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="48" align="center" />
      <el-table-column label="模板名称" prop="templateName" min-width="220" show-overflow-tooltip />
      <el-table-column label="评估任务类型" prop="taskType" min-width="200" show-overflow-tooltip>
        <template #default="scope">{{ getDictLabel(taskTypeOptions, scope.row.taskType) }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" min-width="110" align="center">
        <template #default="scope">
          <el-tag :type="statusTagTypeSimple(scope.row.status)" size="small">
            {{ statusDisplayLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" min-width="180" align="center" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime || scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="240" align="center">
        <template #default="scope">
          <div class="table-ops">
            <el-button link type="primary" size="small" icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button
              v-if="scope.row.status === 'DRAFT'"
              link
              type="danger"
              size="small"
              icon="Delete"
              @click="handleDelete(scope.row)"
            >删除</el-button>
            <el-button
              :type="scope.row.status === 'PUBLISHED' ? 'warning' : 'success'"
              link
              size="small"
              :icon="scope.row.status === 'PUBLISHED' ? 'SwitchButton' : 'Check'"
              @click="handleTogglePublished(scope.row)"
            >{{ scope.row.status === 'PUBLISHED' ? '停用' : '启用' }}</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <calc-flow-edit
      v-model:visible="editVisible"
      :template-id="editId"
      @saved="getList"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import {
  listCalcFlow,
  delCalcFlow,
  disableCalcFlow,
  enableCalcFlow,
  submitTestCalcFlow,
  publishCalcFlow
} from '@/api/zhpg/calcFlow'
import CalcFlowEdit from './edit.vue'

const { proxy } = getCurrentInstance()

// ==================== 字典选项 ====================
const taskTypeOptions = [
  { label: '性能指标评估', value: 'PERFORMANCE' },
  { label: '装备作战效能评估', value: 'EQUIP_EFFECTIVENESS' },
  { label: '体系贡献率评估', value: 'SYSTEM_CONTRIBUTION' },
  { label: '作战任务满足度评估', value: 'TASK_SATISFACTION' },
  { label: '体系级任务评估', value: 'SYSTEM_TASK' },
  { label: '演习演训任务评估', value: 'EXERCISE_TRAINING' }
]
/** 查询：启用 = 已发布；不启用 = 非发布（草稿/测试中/已停用） */
const statusOptions = [
  { label: '启用', value: 'PUBLISHED' },
  { label: '不启用', value: 'NOT_PUBLISHED' }
]

// ==================== 表格数据 ====================
const loading = ref(false)
const templateList = ref([])
const total = ref(0)
const ids = ref([])
const multiple = ref(true)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  templateName: undefined,
  taskType: undefined,
  status: undefined
})

// 编辑弹窗
const editVisible = ref(false)
const editId = ref(null)

function getList() {
  loading.value = true
  listCalcFlow(queryParams).then(res => {
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
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  multiple.value = !selection.length
}

// ==================== 操作 ====================
function handleAdd() {
  editId.value = null
  editVisible.value = true
}

function handleEdit(row) {
  editId.value = row.id
  editVisible.value = true
}

function handleDelete(row) {
  proxy.$modal.confirm('确认删除流程模板"' + row.templateName + '"？').then(() => {
    return delCalcFlow(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleBatchDelete() {
  proxy.$modal.confirm('确认删除选中的流程模板？').then(() => {
    return delCalcFlow(ids.value.join(','))
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function statusDisplayLabel(status) {
  return status === 'PUBLISHED' ? '启用' : '不启用'
}

function statusTagTypeSimple(status) {
  return status === 'PUBLISHED' ? 'success' : 'info'
}

/** 启用 / 停用切换（草稿会先提交测试再发布） */
function handleTogglePublished(row) {
  if (row.status === 'PUBLISHED') {
    proxy.$modal
      .confirm('确认停用流程模板「' + row.templateName + '」？停用后新任务将不能再选用。')
      .then(() => disableCalcFlow(row.id))
      .then(() => {
        getList()
        proxy.$modal.msgSuccess('已停用')
      })
      .catch(() => {})
    return
  }
  proxy.$modal
    .confirm('确认启用流程模板「' + row.templateName + '」？')
    .then(async () => {
      if (row.status === 'DRAFT') {
        await submitTestCalcFlow(row.id)
        await publishCalcFlow(row.id)
      } else {
        await enableCalcFlow(row.id)
      }
      getList()
      proxy.$modal.msgSuccess('已启用')
    })
    .catch(() => {})
}

// ==================== 工具方法 ====================
function getDictLabel(options, value) {
  const item = options.find(o => o.value === value)
  return item ? item.label : value || ''
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}
.page-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
}
.text-muted {
  color: #c0c4cc;
  font-size: 13px;
}
.table-ops {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;
}
</style>
