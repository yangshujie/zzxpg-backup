<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="100px">
      <el-form-item label="指标名称" prop="indicatorName">
        <el-input
          v-model="queryParams.indicatorName"
          placeholder="请输入指标名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="primary" icon="Plus" @click="handleAdd">新增</el-button>
      </el-form-item>
    </el-form>

    <!-- 动态表格 -->
    <DynamicTable
      :table-data="indicatorList"
      :field-config="fieldConfig"
      :show-checkbox="true"
      :show-index="true"
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

    <!-- 新增/修改对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="indicatorRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="指标名称" prop="indicatorName">
              <el-input v-model="form.indicatorName" placeholder="请输入指标名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="指标类型" prop="indicatorType">
              <el-input v-model="form.indicatorType" placeholder="请输入指标类型" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="是否基础指标" prop="isBaseIndicator">
              <el-radio-group v-model="form.isBaseIndicator">
                <el-radio :label="true">是</el-radio>
                <el-radio :label="false">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="算法ID" prop="algorithmId">
              <el-select v-model="form.algorithmId" placeholder="请选择算法" style="width: 100%">
                <el-option
                  v-for="algorithm in algorithmList"
                  :key="algorithm.id"
                  :label="algorithm.algorithmName"
                  :value="algorithm.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="IndicatorManagement">
import { listIndicator, getIndicator, deleteIndicator, insertIndicator, updateIndicator } from '@/api/systemPlus/systemCooperation/indicator'
import { listAlgorithm } from '@/api/systemPlus/systemCooperation/algorithm'
import DynamicTable from '@/components/DynamicTable/index.vue'

const { proxy } = getCurrentInstance()

const indicatorList = ref([])
const algorithmList = ref([])
const open = ref(false)
const loading = ref(true)
const total = ref(0)
const title = ref("")

// 动态表格字段配置
const fieldConfig = ref([
  { key: 'id', label: 'ID', width: 240, showOverflowTooltip: true },
  { key: 'indicatorName', label: '指标名称', width: 150, showOverflowTooltip: true },
  { key: 'indicatorType', label: '指标类型', width: 120, showOverflowTooltip: true },
  { key: 'isBaseIndicator', label: '是否基础指标', width: 120, 
    formatter: (row) => row.isBaseIndicator ? '是' : '否' },
  { key: 'algorithmId', label: '算法ID', width: 120, showOverflowTooltip: true },
  { key: 'creator', label: '创建人', width: 100, showOverflowTooltip: true },
  { key: 'createTime', label: '创建时间', width: 160 },
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
    indicatorName: undefined
  },
  rules: {
    indicatorName: [{ required: true, message: "指标名称不能为空", trigger: "blur" }],
    indicatorType: [{ required: true, message: "指标类型不能为空", trigger: "blur" }],
    isBaseIndicator: [{ required: true, message: "请选择是否基础指标", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 获取算法列表 */
function getAlgorithmList() {
  listAlgorithm({ pageNum: 1, pageSize: 1000 }).then(response => {
    algorithmList.value = response.rows
  })
}

/** 查询列表 */
function getList() {
  loading.value = true
  listIndicator(queryParams.value).then(response => {
    indicatorList.value = response.rows
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
  title.value = "新增指标"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  getIndicator(row.id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改指标"
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除指标编号为"' + row.id + '"的数据项？').then(function() {
    return deleteIndicator(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["indicatorRef"].validate(valid => {
    if (valid) {
      if (form.value.id) {
        updateIndicator(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        insertIndicator(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
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
    indicatorName: null,
    indicatorType: null,
    isBaseIndicator: false,
    algorithmId: null,
    creator: 'admin' // 默认创建人
  }
  proxy.resetForm("indicatorRef")
}

/** 初始化 **/
getList()
getAlgorithmList()
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