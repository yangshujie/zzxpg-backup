<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="120px">
      <el-form-item label="装备名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入装备名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="装备类型" prop="equipmentType">
        <el-select v-model="queryParams.equipmentType" placeholder="请选择装备类型" clearable style="width: 200px">
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
      <el-col :span="1.5">
        <el-button type="success" plain icon="Upload" @click="handleImport">导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport">导出</el-button>
      </el-col>
    </el-row>

    <!-- 装备列表 -->
    <el-table v-loading="loading" :data="equipmentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="序号" align="center" type="index" width="60" />
      <el-table-column label="装备名称" align="center" prop="name" width="150" show-overflow-tooltip />
      <el-table-column label="所属子体系" align="center" prop="subSystemType" width="120">
        <template #default="scope">
          {{ getSubSystemTypeText(scope.row.subSystemType) }}
        </template>
      </el-table-column>
      <el-table-column label="装备类型" align="center" prop="equipmentType" width="120">
        <template #default="scope">
          {{ getEquipmentTypeText(scope.row.equipmentType) }}
        </template>
      </el-table-column>
      <el-table-column label="装备参数详情" align="center" prop="basicInfo" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          {{ formatBasicInfo(scope.row.basicInfo) }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
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

    <!-- 新增或修改对话框 -->
    <EquipmentAddDialog
      v-model="open"
      :title="title"
      :form-data="form"
      :mode="pageMode"
      @submit="handleSubmit"
    />
  </div>
</template>

<script setup name="EquipmentManagement">
import { listEquipment, insertEquipment, updateEquipment, deleteEquipment } from '@/api/systemPlus/systemCooperation/equipment'
import { ref, reactive, toRefs, onMounted, getCurrentInstance } from 'vue'
import { useDict } from '@/utils/dict'
import EquipmentAddDialog from './components/EquipmentAdd/index.vue'

const { proxy } = getCurrentInstance()

// 使用字典获取装备类型选项
const { equipment_type: equipmentTypeOptions } = useDict('equipment_type')

const equipmentList = ref([])
const loading = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')
const pageMode = ref('add') // 'add' or 'edit'

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: undefined,
  equipmentType: undefined
})

const form = reactive({
  id: null,
  name: null,
  subSystemType: null,
  status: 0,
  description: null,
  equipmentType: null,
  side: 1,
  basicInfo: {},
  parameters: {}
})

/** 查询装备列表 */
function getList() {
  loading.value = true
  listEquipment(queryParams).then(response => {
    equipmentList.value = response.data || response.rows || []
    total.value = response.total || equipmentList.value.length
    loading.value = false
  }).catch(error => {
    console.error('查询装备列表失败:', error)
    equipmentList.value = []
    total.value = 0
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.pageNum = 1
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
  title.value = "新增装备"
  pageMode.value = 'add'
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  Object.assign(form, row)
  open.value = true
  title.value = "修改装备"
  pageMode.value = 'edit'
}

/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除装备"' + row.name + '"？').then(function() {
    return deleteEquipment(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch((error) => {
    proxy.$modal.msgError(error || "删除失败")
  })
}

/** 导入按钮操作 */
function handleImport() {
  proxy.$modal.msgInfo("导入功能暂未实现")
}

/** 导出按钮操作 */
function handleExport() {
  proxy.$modal.msgInfo("导出功能暂未实现")
}

/** 处理新增/修改提交 */
function handleSubmit(formData) {
  if (pageMode.value === 'edit') {
    updateEquipment(formData).then(response => {
      proxy.$modal.msgSuccess("修改成功")
      open.value = false
      getList()
    }).catch(error => {
      proxy.$modal.msgError("修改失败")
      console.error('修改装备失败:', error)
    })
  } else {
    insertEquipment(formData).then(response => {
      proxy.$modal.msgSuccess("新增成功")
      open.value = false
      getList()
    }).catch(error => {
      proxy.$modal.msgError("新增失败")
      console.error('新增装备失败:', error)
    })
  }
}

/** 表单重置 */
function reset() {
  Object.assign(form, {
    id: null,
    name: null,
    subSystemType: null,
    status: 0,
    description: null,
    equipmentType: null,
    side: 1,
    basicInfo: {},
    parameters: {}
  })
}

/** 获取子体系类型文本 */
function getSubSystemTypeText(type) {
  const typeMap = {
    0: '体系',
    1: '子系统'
  }
  return typeMap[type] || type
}

/** 获取装备类型文本 */
function getEquipmentTypeText(type) {
  if (!equipmentTypeOptions.value || !Array.isArray(equipmentTypeOptions.value)) {
    return type !== undefined ? type : ''
  }
  const typeItem = equipmentTypeOptions.value.find(item => item.value == type)
  return typeItem ? typeItem.label : (type !== undefined ? type : '')
}

/** 格式化基本信息显示 */
function formatBasicInfo(basicInfo) {
  if (!basicInfo) return ''
  
  const info = []
  if (basicInfo.stationName) info.push(`测站: ${basicInfo.stationName}`)
  if (basicInfo.code) info.push(`代码: ${basicInfo.code}`)
  if (basicInfo.satName) info.push(`卫星: ${basicInfo.satName}`)
  if (basicInfo.country) info.push(`国家: ${basicInfo.country}`)
  
  return info.join(', ')
}

/** 初始化 **/
onMounted(() => {
  getList()
})
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

/* 表格样式 */
:deep(.el-table) {
  margin-top: 20px;
}

:deep(.el-table .cell) {
  text-align: center;
}

/* 分割线样式 */
:deep(.el-divider__text) {
  background-color: var(--el-bg-color);
  padding: 0 10px;
  font-size: 14px;
  font-weight: bold;
}
</style>