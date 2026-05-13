<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="140px">
      <el-form-item label="装备网络名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入装备网络名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="任务ID" prop="pgTaskId">
        <el-input
          v-model="queryParams.pgTaskId"
          placeholder="请输入任务ID"
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
        <el-button type="primary" plain icon="Plus" @click="handleAdvancedAdd">新增</el-button>
      </el-col>
    </el-row>

    <!-- 动态表格 -->
    <DynamicTable
      :table-data="networkList"
      :field-config="fieldConfig"
      :show-checkbox="true"
      :show-index="true"
      @selection-change="handleSelectionChange"
    >
      <template #operationSlot="{ row }">
        <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">修改</el-button>
        <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
        <!-- <el-button link type="info" icon="View" @click="handleDetail(row)">详情</el-button> -->
      </template>
    </DynamicTable>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新增/编辑装备网络弹窗 -->
    <PowerStructureNetworkDialog
      v-model:open="networkDialogOpen"
      :title="networkDialogTitle"
      :form-data="form"
      @submit="handleNetworkSubmit"
      @cancel="handleNetworkCancel"
    />
  </div>
</template>

<script setup name="PowerStructureNetwork">
import { listEquipmentNetwork, getEquipmentNetwork, deleteEquipmentNetwork, insertEquipmentNetwork, updateEquipmentNetwork } from '@/api/systemPlus/systemCooperation/equipment'
import DynamicTable from '@/components/DynamicTable/index.vue'
import PowerStructureNetworkDialog from './components/PowerStructureNetworkDialog.vue'
import { useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()
const router = useRouter()

const networkList = ref([])
const networkDialogOpen = ref(false)
const networkDialogTitle = ref("新增装备网络")
const loading = ref(true)
const total = ref(0)

// 动态表格字段配置
const fieldConfig = ref([
  { key: 'id', label: 'ID', width: 280, showOverflowTooltip: true },
  { key: 'name', label: '装备网络名称', width: 200, showOverflowTooltip: true },
  { key: 'pgTaskId', label: '任务ID', width: 150, showOverflowTooltip: true },
  { key: 'description', label: '描述', width: 200, showOverflowTooltip: true },
  { 
    key: 'createTime', 
    label: '创建时间', 
    width: 180,
    formatter: (value) => {
      return value ? new Date(value).toLocaleString() : '-';
    }
  },
  {
    key: 'operation',
    label: '操作',
    width: 240,
    customColumn: true,
    slotName: 'operationSlot'
  }
])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    pgTaskId: undefined
  },
  rules: {
    name: [{ required: true, message: "装备网络名称不能为空", trigger: "blur" }],
    pgTaskId: [{ required: true, message: "任务ID不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询列表 */
function getList() {
  loading.value = true
  listEquipmentNetwork(queryParams.value).then(response => {
    networkList.value = response.rows || response.data
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




/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除装备网络编号为"' + row.id + '"的数据项？').then(function() {
    return deleteEquipmentNetwork(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 新增按钮操作 */
function handleAdvancedAdd() {
  reset()
  networkDialogOpen.value = true
  networkDialogTitle.value = "新增装备网络"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  getEquipmentNetwork(row.id).then(response => {
    form.value = response.data
    networkDialogOpen.value = true
    networkDialogTitle.value = "修改装备网络"
  })
}

/** 网络提交处理 */
function handleNetworkSubmit(networkData) {
  console.log('网络提交数据:', networkData)
  // 这里可以处理从高级新增弹窗返回的数据
  getList()
}

/** 网络取消处理 */
function handleNetworkCancel() {
  console.log('网络操作取消')
}

/** 详情按钮操作 */
function handleDetail(row) {
  router.push({
    path: '/power-structure-network/detail/index',
    query: { networkId: row.id, networkName: row.name }
  })
}

/** 表单重置 */
function reset() {
  form.value = {
    id: null,
    name: null,
    pgTaskId: null,
    description: null,
    networkStructure: null
  }
  proxy.resetForm("networkRef")
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
</style>