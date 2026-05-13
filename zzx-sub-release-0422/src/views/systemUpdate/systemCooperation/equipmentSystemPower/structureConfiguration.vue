<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="120px">
      <el-form-item label="结构构型名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入结构构型名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="装备体系类型" prop="subSystemType">
        <el-select v-model="queryParams.subSystemType" placeholder="请选择装备体系类型" clearable style="width: 200px">
          <el-option
            v-for="item in equipmentTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
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
      :table-data="configList"
      :field-config="fieldConfig"
      :show-checkbox="true"
      :show-index="true"
      :loading="loading"
      empty-text="暂无结构构型数据"
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

    <!-- 新增或修改结构构型对话框 -->
    <StructureConfigDialog
      v-model:open="open"
      :title="title"
      :form-data="form"
      @submit="handleDialogSubmit"
      @cancel="cancel"
      ref="structureDialogRef"
    />
  </div>
</template>

<script setup name="StructureConfiguration">
import { listEquipmentGroup, getEquipmentGroup, deleteEquipmentGroup, insertEquipmentGroup, updateEquipmentGroup } from '@/api/systemPlus/systemCooperation/equipment'
import DynamicTable from '@/components/DynamicTable/index.vue'
import StructureConfigDialog from './components/StructureConfigDialog.vue'
import { useDict } from '@/utils/dict'

const { proxy } = getCurrentInstance()

const configList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const structureDialogRef = ref(null)

// 动态表格字段配置
const fieldConfig = ref([
  { key: 'name', label: '装备编组名称', width: 180, showOverflowTooltip: true },
  { 
    key: 'subSystemType', 
    label: '装备体系类型', 
    width: 180,
    formatter: (value) => {
      return value !== undefined && value !== null ? value : ''
    }
  },
  { 
    key: 'equipmentLevel', 
    label: '装备等级', 
    width: 100,
    formatter: (value) => {
      return value !== undefined && value !== null ? value : ''
    }
  },
  { 
    key: 'side', 
    label: '阵营', 
    width: 100,
    formatter: (value) => {
      return value !== undefined && value !== null ? value : ''
    }
  },
  { 
    key: 'formationDetail', 
    label: '兵力编成', 
    width: 200, 
    showOverflowTooltip: true,
    formatter: (value) => {
      if (Array.isArray(value)) {
        return value.map(item => item.name || item.id).join(', ')
      }
      return value ? value : ''
    }
  },
  // { key: 'creator', label: '创建人', width: 100 },
  { key: 'createTime', label: '创建时间', width: 180 },
  {
    key: 'operation',
    label: '操作',
    width: 200,
    customColumn: true,
    slotName: 'operationSlot'
  }
])

// 树形结构配置
const treeProps = {
  children: 'children',
  label: 'label'
}

// 兵力编成树形数据
const forceTreeData = ref([
  {
    id: 1,
    label: '主力装备',
    children: [
      { id: 2, label: '侦察卫星群' },
      { id: 3, label: '通信卫星' },
      { id: 4, label: '导航卫星' }
    ]
  },
  {
    id: 5,
    label: '支援装备',
    children: [
      { id: 6, label: '地面控制站' },
      { id: 7, label: '数据处理中心' }
    ]
  },
  {
    id: 8,
    label: '保障装备',
    children: [
      { id: 9, label: '发射系统' },
      { id: 10, label: '测控系统' }
    ]
  }
])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    subSystemType: undefined
  }
})

const { queryParams, form } = toRefs(data)

/** 查询装备编组列表 */
function getList() {
  loading.value = true
  listEquipmentGroup(queryParams.value).then(response => {
    configList.value = response.rows
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
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "新增结构构型"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const id = row.id || ids.value
  getEquipmentGroup(id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改装备编组"
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除装备编组编号为"' + ids + '"的数据项？').then(function() {
    return deleteEquipmentGroup(ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 对话框提交处理 */
function handleDialogSubmit(submitData) {
  if (submitData.id != null) {
    updateEquipmentGroup(submitData).then(response => {
      proxy.$modal.msgSuccess("修改成功")
      getList()
    })
  } else {
    insertEquipmentGroup(submitData).then(response => {
      proxy.$modal.msgSuccess("新增成功")
      getList()
    })
  }
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
    subSystemType: null,
    equipmentLevel: null,
    side: null,
    formationDetail: []
  }
  // 重置子组件表单
  structureDialogRef.value?.resetForm()
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

.structure-row {
  margin-top: 20px;
}

.tree-container {
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  padding: 15px;
  height: 600px;
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.tree-title {
  font-weight: bold;
  color: var(--el-text-color-primary);
  font-size: 16px;
}

.structure-tree {
  height: 320px;
  overflow-y: auto;
}

.cesium-container {
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  padding: 0;
  height: 600px;
  min-height: 400px;
}

.cesium-container :deep(.el-card__body) {
  padding: 0;
  height: 600px;
}

.cesium-header {
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.cesium-title {
  font-weight: bold;
  color: var(--el-text-color-primary);
  font-size: 16px;
}

.cesium-placeholder {
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 242, 255, 0.02);
  border: 2px dashed var(--el-border-color-light);
  border-radius: 8px;
}

.cesium-content {
  text-align: center;
  color: var(--el-text-color-secondary);
}

.cesium-content h3 {
  margin: 15px 0 10px 0;
  color: var(--el-text-color-primary);
}

.cesium-content p {
  margin: 5px 0;
  font-size: 14px;
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