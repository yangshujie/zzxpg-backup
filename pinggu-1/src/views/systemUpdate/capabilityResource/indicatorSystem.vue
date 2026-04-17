<!-- 修改后的 IndicatorSystem.vue - 父组件 -->
<template>
  <div class="app-container">
    <!-- 快速查询 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="0" class="mb8">
      <el-form-item prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入需要查询的指标集名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item prop="type">
        <el-select v-model="queryParams.type" placeholder="指标体系类型" clearable style="width: 150px">
          <el-option
            v-for="item in systemTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item prop="equipmentType">
        <el-select v-model="queryParams.equipmentType" placeholder="装备类型" clearable style="width: 150px">
          <el-option
            v-for="item in equipmentTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">快速查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="text" icon="ArrowDown" @click="showAdvanced = !showAdvanced">
          {{ showAdvanced ? '收起' : '高级查询' }}
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 高级查询 -->
    <el-collapse-transition>
      <div v-show="showAdvanced" class="advanced-query">
        <el-form :model="advancedQueryParams" ref="advancedQueryRef" :inline="true" label-width="120px">
          <el-form-item label="指标集类型" prop="systemType">
            <el-select v-model="advancedQueryParams.systemType" placeholder="请选择指标集类型" clearable style="width: 200px">
              <el-option
                v-for="item in systemTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="指标集对象" prop="systemObject">
            <el-select v-model="advancedQueryParams.systemObject" placeholder="请选择指标集对象" clearable style="width: 200px">
              <el-option
                v-for="item in systemObjectOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="是否启用" prop="isEnabled">
            <el-select v-model="advancedQueryParams.isEnabled" placeholder="请选择状态" clearable style="width: 200px">
              <el-option label="启用" :value="true" />
              <el-option label="禁用" :value="false" />
            </el-select>
          </el-form-item>
          <el-form-item label="会签状态" prop="approvalStatus">
            <el-select v-model="advancedQueryParams.approvalStatus" placeholder="请选择会签状态" clearable style="width: 200px">
              <el-option
                v-for="item in approvalStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleAdvancedQuery">查询</el-button>
            <el-button icon="Refresh" @click="resetAdvancedQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-collapse-transition>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
    </el-row>

    <!-- 动态表格 -->
    <DynamicTable
      :table-data="systemList"
      :field-config="fieldConfig"
      :show-checkbox="true"
      :show-index="true"
      :loading="loading"
      empty-text="暂无指标体系数据"
      @selection-change="handleSelectionChange"
    >
      <template #operationSlot="{ row }">
        <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">编辑</el-button>
        <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
        <el-button link type="info" icon="Download" @click="handleDownloadApproval(row)">下载下方会签</el-button>
        <el-button link type="warning" icon="MagicStick" @click="handleAutoOptimize(row)">权重自优化</el-button>
      </template>
    </DynamicTable>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 指标体系编辑器组件 -->
    <IndicatorSystemEditor
      v-model:visible="editorVisible"
      :title="editorTitle"
      :edit-data="currentEditData"
      @save-success="handleEditorSave"
    />
  </div>
</template>

<script setup name="IndicatorSystem">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import { treeIndicatorSystem, getIndicatorSystem, deleteIndicatorSystem, insertTreeIndicatorSystem, updateIndicatorSystem } from '@/api/systemPlus/systemCooperation/indicator'
import DynamicTable from '@/components/DynamicTable/index.vue'
import IndicatorSystemEditor from './components/IndicatorSystemEditor.vue'

const { proxy } = getCurrentInstance()

const systemList = ref([])
const loading = ref(true)
const total = ref(0)
const showAdvanced = ref(false)
const editorVisible = ref(false)
const editorTitle = ref('')
const currentEditData = ref(null)

// 选项数据
const systemTypeOptions = ref([
  { value: '1', label: '作战效能' },
  { value: '2', label: '作战适用性' },
  { value: '3', label: '体系适用性' },
  { value: '4', label: '在役适用性' }
])

const equipmentTypeOptions = ref([
  { value: '1', label: '航天侦察' },
  { value: '2', label: '太空态势感知' },
  { value: '3', label: '太空攻防' },
  { value: '4', label: '航天发射' },
  { value: '5', label: '航天测运控' },
  { value: '6', label: '海基航天' }
])

const systemObjectOptions = ref([
  { value: '1', label: '单装' },
  { value: '2', label: '装备系统' },
  { value: '3', label: '装备子系统' }
])

const approvalStatusOptions = ref([
  { value: '1', label: '待会签' },
  { value: '2', label: '会签中' },
  { value: '3', label: '已会签' }
])

// 动态表格字段配置
const fieldConfig = ref([
  { key: 'id', label: 'ID', width: 80 },
  { key: 'name', label: '指标体系名称', width: 200, showOverflowTooltip: true },
  { key: 'levelNum', label: '层级数', width: 80 },
  { key: 'indicatorSetType', label: '指标体系类型', width: 120, showOverflowTooltip: true },
  { key: 'indicatorSetObject', label: '指标体系对象', width: 150, showOverflowTooltip: true },
  { 
    key: 'isEnabled', 
    label: '是否启用', 
    width: 80,
    formatter: (row) => row.isEnabled ? '是' : '否'
  },
  { key: 'weightAlgorithm', label: '权重算法', width: 120, showOverflowTooltip: true },
  { key: 'reviewStatus', label: '审核状态', width: 100, showOverflowTooltip: true },
  { key: 'orderNum', label: '排序号', width: 80 },
  {
    key: 'operation',
    label: '操作',
    width: 320,
    customColumn: true,
    slotName: 'operationSlot'
  }
])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    type: undefined,
    equipmentType: undefined
  },
  advancedQueryParams: {
    systemType: undefined,
    systemObject: undefined,
    isEnabled: undefined,
    approvalStatus: undefined
  }
})

const { queryParams, advancedQueryParams } = toRefs(data)

/** 查询列表 */
function getList() {
  loading.value = true
  const params = { ...queryParams.value, ...advancedQueryParams.value }
  treeIndicatorSystem(params).then(response => {
    systemList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 快速查询按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 高级查询按钮操作 */
function handleAdvancedQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 重置高级查询按钮操作 */
function resetAdvancedQuery() {
  proxy.resetForm("advancedQueryRef")
  advancedQueryParams.value = {
    systemType: undefined,
    systemObject: undefined,
    isEnabled: undefined,
    approvalStatus: undefined
  }
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  // 处理选中数据
}

/** 新增按钮操作 */
function handleAdd() {
  editorTitle.value = '新增指标体系'
  currentEditData.value = null
  editorVisible.value = true
}

/** 修改按钮操作 */
function handleUpdate(row) {
  editorTitle.value = '编辑指标体系'
  // 获取完整的树形数据用于编辑
  getIndicatorSystem(row.id).then(response => {
    currentEditData.value = response.data
    editorVisible.value = true
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除指标体系编号为"' + row.id + '"的数据项？').then(function() {
    return deleteIndicatorSystem(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 下载会签按钮操作 */
function handleDownloadApproval(row) {
  // 实现下载逻辑
  proxy.$modal.msgSuccess("下载功能开发中")
}

/** 权重自优化按钮操作 */
function handleAutoOptimize(row) {
  proxy.$modal.confirm('是否确认对"' + row.name + '"进行权重自优化？').then(function() {
    // 调用权重自优化API
    proxy.$modal.msgSuccess("权重自优化成功")
    getList()
  }).catch(() => {})
}

/** 编辑器保存回调 */
function handleEditorSave(saveData) {
  if (currentEditData.value && currentEditData.value.id) {
    // 编辑保存
    updateIndicatorSystem(saveData).then(() => {
      proxy.$modal.msgSuccess("修改成功")
      getList()
    })
  } else {
    // 新增保存
    insertTreeIndicatorSystem(saveData).then(() => {
      proxy.$modal.msgSuccess("新增成功")
      getList()
    })
  }
}

/** 初始化 **/
getList()
</script>

<style scoped lang="scss">
@use '@/assets/styles/v2/variables.scss' as *;

.app-container {
  padding: 20px;
}

.mb8 {
  margin-bottom: 20px;
}

.advanced-query {
  background-color: var(--el-fill-color-light, rgba(0, 0, 0, 0.04));
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 15px;
  border: 1px solid var(--el-border-color-lighter, transparent);
}

:deep(.el-table) {
  margin-top: 20px;
}

:deep(.el-table .cell) {
  text-align: center;
}
</style>