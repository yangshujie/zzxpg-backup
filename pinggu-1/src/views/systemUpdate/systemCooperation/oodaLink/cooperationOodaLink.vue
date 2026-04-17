<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="120px">
      <el-form-item label="OODA链路名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入OODA链路名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="OODA链路类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择OODA链路类型" clearable style="width: 200px">
          <el-option label="类型0" value="0" />
          <el-option label="类型1" value="1" />
          <el-option label="类型2" value="2" />
        </el-select>
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
      :table-data="linkList"
      :field-config="fieldConfig"
      :show-checkbox="true"
      :show-index="true"
      :loading="loading"
      empty-text="暂无协同作战OODA链路数据"
      @selection-change="handleSelectionChange"
    >
      <template #operationSlot="{ row }">
        <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">修改</el-button>
        <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
      </template>
    </DynamicTable>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新增或修改对话框 -->
    <OodaLinkDialog
      v-model:open="open"
      :title="title"
      :form-data="form"
      @submit="handleLinkSubmit"
      @cancel="cancel"
    />
  </div>
</template>

<script setup name="CooperationOodaLink">
import { listOodaPath, getOodaPath, deleteOodaPath, insertOodaPath, updateOodaPath } from '@/api/systemPlus/systemCooperation/ooda'
import DynamicTable from '@/components/DynamicTable/index.vue'
import OodaLinkDialog from './components/OodaLinkDialog.vue'
import { useDict } from '@/utils/dict'

const { proxy } = getCurrentInstance()

const linkList = ref([])
const open = ref(false)
const loading = ref(true)
const total = ref(0)
const title = ref("")

// 使用字典获取装备等级选项
const { equipment_level: equipmentLevelOptions } = useDict('equipment_level')

/** 处理OODA链路提交 */
function handleLinkSubmit(formData) {
  // if (formData.id != null) {
  //   updateOodaPath(formData).then(response => {
  //     proxy.$modal.msgSuccess("修改成功")
  //     open.value = false
  //     getList()
  //   })
  // } else {
  //   insertOodaPath(formData).then(response => {
  //     proxy.$modal.msgSuccess("新增成功")
  //     open.value = false
  //     getList()
  //   })
  // }
  getList()
}

// 动态表格字段配置
const fieldConfig = ref([
  { key: 'id', label: 'ID', width: 120, showOverflowTooltip: true },
  { key: 'name', label: 'OODA链路名称', width: 180, showOverflowTooltip: true },
  { 
    key: 'type', 
    label: 'OODA链路类型', 
    width: 120,
    formatter: (row) => {
      if (!row) return ''
      return row.type !== undefined ? row.type : ''
    }
  },
  { 
    key: 'nodeIds', 
    label: '节点ID', 
    width: 200,
    showOverflowTooltip: true,
    formatter: (row) => {
      if (!row) return '无'
      return row.nodeIds ? row.nodeIds : '无'
    }
  },
  { key: 'createTime', label: '创建时间', width: 120 },
  {
    key: 'operation',
    label: '操作',
    width: 160,
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
    type: undefined
  },
  rules: {
    name: [{ required: true, message: "OODA链路名称不能为空", trigger: "blur" }],
    type: [{ required: true, message: "OODA链路类型不能为空", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询列表 */
function getList() {
  loading.value = true
  listOodaPath(queryParams.value).then(response => {
    linkList.value = response.rows
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
  open.value = true
  title.value = "新增协同作战OODA链路"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  // 直接使用行数据，避免不必要的接口调用
  form.value = { ...row }
  open.value = true
  title.value = "修改协同作战OODA链路"
}

/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除OODA链路编号为"' + row.id + '"的数据项？').then(function() {
    return deleteOodaPath(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}



/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    id: null,
    name: null,
    type: null,
    nodeIds: []
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
</style>