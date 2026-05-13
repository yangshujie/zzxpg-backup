<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="140px">
      <el-form-item label="OODA网名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入OODA网名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="评估任务ID" prop="pgTaskId">
        <el-input
          v-model="queryParams.pgTaskId"
          placeholder="请输入评估任务ID"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
    </el-row>

    <!-- 动态表格 -->
    <DynamicTable
      :table-data="networkList"
      :field-config="fieldConfig"
      :show-checkbox="true"
      :show-index="true"
      :loading="loading"
      empty-text="暂无OODA网络数据"
      @selection-change="handleSelectionChange"
    >
      <template #operationSlot="{ row }">
        <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">修改</el-button>
        <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
        <el-button link type="info" icon="View" @click="handleDetail(row)">详情</el-button>
      </template>
    </DynamicTable>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- OODA网络详情/新增/修改弹窗 -->
    <OodaNetworkDetail
      v-model:open="detailOpen"
      :title="detailTitle"
      :form-data="form"
      :is-view-only="isViewOnly"
      @submit="handleNetworkSubmit"
      @cancel="closeDetail"
    />
  </div>
</template>

<script setup name="OodaNetwork">
import { listOodaNetwork, getOodaNetwork, deleteOodaNetwork, insertOodaNetwork, updateOodaNetwork } from '@/api/systemPlus/systemCooperation/ooda'
import DynamicTable from '@/components/DynamicTable/index.vue'
import OodaNetworkDetail from './components/OodaNetworkDetail.vue'

const { proxy } = getCurrentInstance()

const networkList = ref([])
const loading = ref(true)
const total = ref(0)

// 详情弹窗相关变量
const detailOpen = ref(false)
const detailTitle = ref("")
const isViewOnly = ref(false)
const form = ref({})

// 动态表格字段配置
const fieldConfig = ref([
  { key: 'id', label: 'ID', width: 120, showOverflowTooltip: true },
  { key: 'name', label: 'OODA网名称', width: 200, showOverflowTooltip: true },
  { key: 'description', label: '描述', width: 200, showOverflowTooltip: true },
  { key: 'pgTaskId', label: '评估任务ID', width: 150, showOverflowTooltip: true },
  { 
    key: 'networkStructure', 
    label: '网络结构', 
    width: 180, 
    showOverflowTooltip: true,
    formatter: (row) => {
      if (!row) return '无'
      return row.networkStructure && Array.isArray(row.networkStructure) ? 
        `${row.networkStructure.length} 个节点` : '无'
    }
  },
  { key: 'createTime', label: '创建时间', width: 120 },
  {
    key: 'operation',
    label: '操作',
    width: 240,
    customColumn: true,
    slotName: 'operationSlot'
  }
])

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  name: undefined,
  pgTaskId: undefined
})

const rules = {
  name: [{ required: true, message: "OODA网名称不能为空", trigger: "blur" }],
  pgTaskId: [{ required: true, message: "评估任务ID不能为空", trigger: "blur" }]
}

/** 查询列表 */
function getList() {
  loading.value = true
  listOodaNetwork(queryParams.value).then(response => {
    networkList.value = response.rows
    total.value = response.total
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
  // 处理选中数据
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  detailTitle.value = "新增OODA网"
  isViewOnly.value = false
  detailOpen.value = true
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  // 直接传递当前行数据，无需请求接口
  form.value = { ...row }  // 使用浅拷贝或扩展运算符，避免直接引用
  detailTitle.value = "修改OODA网"
  isViewOnly.value = false
  detailOpen.value = true
}

/** 详情按钮操作 */
function handleDetail(row) {
  reset()
  // 直接传递当前行数据
  form.value = { ...row }
  detailTitle.value = "OODA网详情 - " + row.name
  isViewOnly.value = true
  detailOpen.value = true
}

/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除OODA网编号为"' + row.id + '"的数据项？').then(function() {
    return deleteOodaNetwork(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 处理OODA网络提交 */
function handleNetworkSubmit(formData) {
  console.log("formData=======", formData);
  if (formData.id != null) {
    // 修改操作
    updateOodaNetwork(formData).then(response => {
      proxy.$modal.msgSuccess("修改成功")
      closeDetail()
      getList()
    })
  } else {
    // 新增操作
    insertOodaNetwork(formData).then(response => {
      proxy.$modal.msgSuccess("新增成功")
      closeDetail()
      getList()
    })
  }
}

/** 关闭详情弹窗 */
function closeDetail() {
  detailOpen.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    id: null,
    name: null,
    description: null,
    pgTaskId: null,
    networkStructure: []
  }
}

/** 初始化 **/
getList()
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.mb8 {
  margin-bottom: 20px;
}

.dialog-footer {
  text-align: center;
}

.dialog-footer .el-button {
  margin: 0 10px;
}

/* 动态表格样式 */
:deep(.el-table) {
  margin-top: 20px;
}

:deep(.el-table .cell) {
  text-align: center;
}

/* OODA网详情弹窗样式 */
.ooda-detail-container {
  height: 600px;
}

.detail-layout {
  display: flex;
  height: 100%;
  gap: 15px;
}

.detail-left {
  width: 250px;
  background: #f8f9fa;
  border-radius: 4px;
  padding: 15px;
  border: 1px solid #e4e7ed;
}

.tree-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.detail-tree {
  background: transparent;
}

.detail-tree :deep(.el-tree-node__content) {
  height: 32px;
}

.detail-tree :deep(.el-tree-node__label) {
  font-size: 13px;
}

.detail-center {
  flex: 1;
  background: white;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.detail-right {
  width: 300px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.canvas-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  padding: 15px;
  border-bottom: 1px solid #e4e7ed;
  background: #f8f9fa;
}

.ooda-canvas-container {
  flex: 1;
  padding: 10px;
  overflow: hidden;
}

.ooda-canvas {
  width: 100%;
  height: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.param-canvas-container {
  flex: 1;
  padding: 10px;
  overflow: hidden;
}

.param-canvas {
  width: 100%;
  height: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  width: 100%;
}

/* 弹窗宽度调整 */
:deep(.el-dialog) {
  max-width: 90vw;
}

:deep(.el-dialog__body) {
  padding: 20px;
}
</style>