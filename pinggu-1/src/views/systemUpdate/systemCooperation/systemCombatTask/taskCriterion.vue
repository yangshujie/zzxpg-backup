<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="120px">
      <el-form-item label="判据名称" prop="criterionName">
        <el-input
          v-model="queryParams.criterionName"
          placeholder="请输入判据名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="判据类型" prop="criterionType">
        <el-select v-model="queryParams.criterionType" placeholder="请选择判据类型" clearable style="width: 200px">
          <el-option label="单装" value="单装" />
          <el-option label="装备系统" value="装备系统" />
          <el-option label="装备子系统" value="装备子系统" />
          <el-option label="系统" value="系统" />
        </el-select>
      </el-form-item>
      <el-form-item label="应用类型" prop="applyType">
        <el-select v-model="queryParams.applyType" placeholder="请选择应用类型" clearable style="width: 200px">
          <el-option label="光学侦察卫星" value="光学侦察卫星" />
          <el-option label="雷达侦察卫星" value="雷达侦察卫星" />
          <el-option label="电子侦察卫星" value="电子侦察卫星" />
          <el-option label="通信卫星" value="通信卫星" />
          <el-option label="导航卫星" value="导航卫星" />
          <el-option label="气象卫星" value="气象卫星" />
          <el-option label="其他" value="其他" />
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
      :table-data="criterionList"
      :field-config="fieldConfig"
      :show-checkbox="true"
      :show-index="true"
      :loading="loading"
      empty-text="暂无任务判据数据"
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
    <el-dialog :title="title" v-model="open" width="900px" class="criterion-dialog" append-to-body>
      <el-form ref="criterionRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="判据名称" prop="criterionName">
              <el-input v-model="form.criterionName" placeholder="请输入判据名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="判据类型" prop="criterionType">
              <el-select v-model="form.criterionType" placeholder="请选择判据类型" style="width: 100%">
                <el-option label="单装" value="单装" />
                <el-option label="装备系统" value="装备系统" />
                <el-option label="装备子系统" value="装备子系统" />
                <el-option label="系统" value="系统" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="应用类型" prop="applyType">
              <el-select v-model="form.applyType" placeholder="请选择应用类型" style="width: 100%">
                <el-option label="光学侦察卫星" value="光学侦察卫星" />
                <el-option label="雷达侦察卫星" value="雷达侦察卫星" />
                <el-option label="电子侦察卫星" value="电子侦察卫星" />
                <el-option label="通信卫星" value="通信卫星" />
                <el-option label="导航卫星" value="导航卫星" />
                <el-option label="气象卫星" value="气象卫星" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="条件运算符" prop="conditionOperator">
              <el-select v-model="form.conditionOperator" placeholder="请选择条件运算符" style="width: 100%">
                <el-option label="=" value="=" />
                <el-option label="＜" value="＜" />
                <el-option label="＞" value="＞" />
                <el-option label="≤" value="≤" />
                <el-option label="≥" value="≥" />
                <el-option label="≠" value="≠" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="阈值数值" prop="thresholdValue">
              <el-input v-model="form.thresholdValue" placeholder="请输入阈值数值" type="number" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-select v-model="form.unit" placeholder="请选择单位" style="width: 100%">
                <el-option label="无单位" value="" />
                <el-option label="秒" value="秒" />
                <el-option label="分钟" value="分钟" />
                <el-option label="小时" value="小时" />
                <el-option label="米" value="米" />
                <el-option label="千米" value="千米" />
                <el-option label="百分比" value="%" />
                <el-option label="度" value="度" />
                <el-option label="分贝" value="分贝" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="输入条件" prop="input">
              <el-input v-model="form.input" placeholder="请输入输入条件" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="输出结果" prop="output">
              <el-input v-model="form.output" placeholder="请输入输出结果" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      
      <!-- 任务判据流程图 -->
      <LogicSection :criterion-flow="defaultCriterionFlow" />
      
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="TaskCriterion">
import { queryCriterionList, addCriterion } from '@/api/systemPlus/systemCooperation/task'
import DynamicTable from '@/components/DynamicTable/index.vue'
import LogicSection from './components/LogicSection.vue'
import { ref, reactive, toRefs, watch, nextTick, onMounted } from 'vue'

const { proxy } = getCurrentInstance()

const criterionList = ref([])
const open = ref(false)
const loading = ref(true)
const total = ref(0)
const title = ref("")
const formulaResult = ref(null)

// 动态表格字段配置
const fieldConfig = ref([
  { key: 'criterionName', label: '判据名称', width: 150, showOverflowTooltip: true },
  { key: 'criterionType', label: '判据类型', width: 120 },
  { key: 'applyType', label: '应用类型', width: 150, showOverflowTooltip: true },
  { key: 'conditionOperator', label: '条件运算符', width: 100 },
  { key: 'thresholdValue', label: '阈值数值', width: 100 },
  { key: 'unit', label: '单位', width: 80 },
  { key: 'input', label: '输入条件', width: 120, showOverflowTooltip: true },
  { key: 'output', label: '输出结果', width: 120, showOverflowTooltip: true },
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
    criterionName: undefined,
    criterionType: undefined,
    applyType: undefined
  },
  rules: {
    criterionName: [{ required: true, message: "判据名称不能为空", trigger: "blur" }],
    criterionType: [{ required: true, message: "判据类型不能为空", trigger: "change" }],
    applyType: [{ required: true, message: "应用类型不能为空", trigger: "change" }],
    conditionOperator: [{ required: true, message: "条件运算符不能为空", trigger: "change" }],
    thresholdValue: [{ required: true, message: "阈值数值不能为空", trigger: "blur" }],
    input: [{ required: true, message: "输入条件不能为空", trigger: "blur" }],
    output: [{ required: true, message: "输出结果不能为空", trigger: "blur" }]
  }
})

// 默认任务判据流程配置
const defaultCriterionFlow = {
  criterionName: "侦察识别确认",
  criterionType: "装备子体系",
  applyType: "航天侦察装备子体系",
  remark: "根据昼夜、云层、波束范围计算权重",
  formula: [
    {
      condition: "x==1 && z==1 && y>3",
      result: "0.8",
      isDefault: false
    },
    {
      condition: "x==1 && z==1 && y<=3",
      result: "0.5",
      isDefault: false
    },
    {
      condition: "",
      result: "0",
      isDefault: true
    }
  ],
  conditionList: [
    {
      conditionName: "昼夜情况",
      conditionCode: "x",
      conditionRule: [{"value":"1","text":"白天"},{"value":"0","text":"黑夜"}]
    },
    {
      conditionName: "云层等级",
      conditionCode: "y",
      conditionRule: [{"value":"1","text":"等级1"},{"value":"2","text":"等级2"},{"value":"3","text":"等级3"}]
    },
    {
      conditionName: "波束范围",
      conditionCode: "z",
      conditionRule: [{"value":"1","text":"在范围内"},{"value":"0","text":"不在范围"}]
    }
  ]
}

const { queryParams, form, rules } = toRefs(data)

// 监听条件变化，实时计算权重
watch(() => [form.value.x, form.value.y, form.value.z], () => {
  calculateFormulaResult()
}, { deep: true })

// 计算公式结果
function calculateFormulaResult() {
  const x = form.value.x
  const y = form.value.y
  const z = form.value.z
  
  // 检查是否所有条件都已选择
  if (x === undefined || y === undefined || z === undefined) {
    formulaResult.value = null
    return
  }
  
  // 根据默认公式计算权重
  for (const formula of defaultCriterionFlow.formula) {
    if (formula.isDefault && formula.condition === '') {
      formulaResult.value = formula.result
      return
    }
    
    try {
      // 简单的条件判断逻辑
      const condition = formula.condition
        .replace(/x/g, x)
        .replace(/y/g, y)
        .replace(/z/g, z)
      
      if (eval(condition)) {
        formulaResult.value = formula.result
        return
      }
    } catch (error) {
      console.error('公式计算错误:', error)
    }
  }
  
  formulaResult.value = '0'
}

/** 查询列表 */
function getList() {
  loading.value = true
  queryCriterionList(queryParams.value).then(response => {
    criterionList.value = response.data || []
    total.value = criterionList.value.length
    loading.value = false
  }).catch(error => {
    console.error('查询判据列表失败:', error)
    criterionList.value = []
    total.value = 0
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
  // 设置默认的任务判据流程
  Object.assign(form.value, {
    criterionName: defaultCriterionFlow.criterionName,
    criterionType: defaultCriterionFlow.criterionType,
    applyType: defaultCriterionFlow.applyType,
    input: defaultCriterionFlow.remark,
    output: "权重计算结果"
  })
  open.value = true
  title.value = "新增任务判据"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  // getTaskCriterion(row.id).then(response => {
  //   form.value = response.data
  //   open.value = true
  //   title.value = "修改任务判据"
  // })
  
  // 暂时使用行数据填充表单
  form.value = { ...row }
  open.value = true
  title.value = "修改任务判据"
}

/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除任务判据编号为"' + row.id + '"的数据项？').then(function() {
    // return delTaskCriterion(row.id)
    return Promise.reject('删除功能暂未实现')
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch((error) => {
    proxy.$modal.msgError(error || "删除失败")
  })
}



/** 提交按钮 */
function submitForm() {
  proxy.$refs["criterionRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        // updateTaskCriterion(form.value).then(response => {
        //   proxy.$modal.msgSuccess("修改成功")
        //   open.value = false
        //   getList()
        // })
        proxy.$modal.msgError("修改功能暂未实现")
      } else {
        addCriterion(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        }).catch(error => {
          proxy.$modal.msgError("新增失败")
          console.error('新增判据失败:', error)
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
    criterionName: null,
    criterionType: null,
    applyType: null,
    conditionOperator: null,
    thresholdValue: null,
    unit: null,
    input: null,
    output: null,
    x: undefined,
    y: undefined,
    z: undefined
  }
  formulaResult.value = null
  proxy.resetForm("criterionRef")
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

/* 动态表格样式 */
:deep(.el-table) {
  margin-top: 20px;
}

:deep(.el-table .cell) {
  text-align: center;
}

/* 对话框样式 */
:deep(.criterion-dialog .el-dialog) {
  max-height: 80vh;
  min-height: 600px;
}

:deep(.criterion-dialog .el-dialog__body) {
  max-height: calc(80vh - 140px);
  overflow-y: auto;
}

/* 流程图样式 */
.flow-chart-section {
  margin-top: 20px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  padding: 15px;
  min-height: 350px;
}

.flow-chart-section h4 {
  margin: 0 0 15px 0;
  font-size: 14px;
  color: var(--el-text-color-primary);
  text-align: center;
}

.flow-chart {
  width: 100%;
  height: 300px;
  min-height: 300px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background-color: var(--el-bg-color);
}

/* 确保AntV X6画布正确显示 */
:deep(.x6-graph) {
  width: 100% !important;
  height: 100% !important;
}

:deep(.x6-graph-svg) {
  width: 100% !important;
  height: 100% !important;
}
</style>