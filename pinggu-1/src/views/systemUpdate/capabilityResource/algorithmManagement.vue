<!-- eslint-disable vue/multi-word-component-names -->
<template>
    <div class="app-container">
        <!-- 查询条件 -->
        <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="100px">
            <el-form-item label="算法名称" prop="algorithmName">
                <el-input v-model="queryParams.algorithmName" placeholder="请输入算法名称" clearable style="width: 200px"
                    @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item>
                <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
                <el-button icon="Refresh" @click="resetQuery">重置</el-button>
                <el-button type="primary" icon="Plus" @click="handleAdd">新增</el-button>
            </el-form-item>
        </el-form>

        <!-- 动态表格 -->
        <DynamicTable :table-data="algorithmList" :field-config="fieldConfig" :show-checkbox="true" :show-index="true"
            @selection-change="handleSelectionChange">
            <template #operationSlot="{ row }">
                <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">修改</el-button>
                <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
            </template>
        </DynamicTable>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize" @pagination="getList" />

        <!-- 新增/修改对话框 -->
        <el-dialog :title="title" v-model="open" width="800px" append-to-body>
            <el-form ref="algorithmRef" :model="form" :rules="rules" label-width="140px">
                <el-row>
                    <el-col :span="12">
                        <el-form-item label="算法编码" prop="algorithmCode">
                            <el-input v-model="form.algorithmCode" placeholder="请输入算法编码" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="算法名称" prop="algorithmName">
                            <el-input v-model="form.algorithmName" placeholder="请输入算法名称" />
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row>
                    <el-col :span="12">
                        <el-form-item label="算法类型" prop="algorithmType">
                            <el-select v-model="form.algorithmType" placeholder="请选择算法类型" style="width: 100%">
                                <el-option v-for="item in algorithmTypeOptions" :key="item.value" :label="item.label"
                                    :value="item.value" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="装备类型" prop="equipmentType">
                            <el-select v-model="form.equipmentType" placeholder="请选择装备类型" style="width: 100%">
                                <el-option v-for="item in equipmentTypeOptions" :key="item.value" :label="item.label"
                                    :value="item.value" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row>
                    <el-col :span="12">
                        <el-form-item label="算法输入" prop="algorithmInput">
                            <el-select v-model="form.algorithmInput.dataType" placeholder="数据类型" style="width: 100%">
                                <el-option label="数值型" value="数值型" />
                                <el-option label="字符型" value="字符型" />
                                <el-option label="布尔型" value="布尔型" />
                                <el-option label="数组型" value="数组型" />
                                <el-option label="对象型" value="对象型" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">

                    </el-col>
                </el-row>

                <el-form-item label="算法描述" prop="algorithmDescription">
                    <el-input v-model="form.algorithmDescription" type="textarea" :rows="3" placeholder="请输入算法描述" />
                </el-form-item>
                <el-row>
                    <el-col :span="12">
                        <el-form-item label="是否基础数据" prop="isBaseData">
                            <el-radio-group v-model="form.isBaseData">
                                <el-radio label="1">是</el-radio>
                                <el-radio label="0">否</el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="发布状态" prop="publishStatus">
                            <el-select v-model="form.publishStatus" placeholder="请选择" style="width: 100%">
                                <el-option label="已发布" value="1" />
                                <el-option label="未发布" value="0" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-form-item label="算法参数" prop="algorithmParams">
                    <div class="algorithm-params-container">
                        <div v-for="(param, index) in form.algorithmParams" :key="index" class="param-row">
                            <el-row :gutter="10">
                                <el-col :span="6">
                                    <el-input v-model="param.name" placeholder="参数名" />
                                </el-col>
                                <el-col :span="6">
                                    <el-input v-model="param.label" placeholder="参数标签" />
                                </el-col>
                                <el-col :span="6">
                                    <el-select v-model="param.type" placeholder="参数类型" style="width: 100%">
                                        <el-option v-for="item in paramCategoryOptions" :key="item.value"
                                            :label="item.label" :value="item.value" />
                                    </el-select>
                                </el-col>
                                <el-col :span="5">
                                    <el-input v-model="param.value" placeholder="参数值" />
                                </el-col>
                                <el-col :span="1">
                                    <el-button type="danger" icon="Delete" @click="removeParam(index)" size="small" />
                                </el-col>
                            </el-row>
                        </div>
                        <el-button type="primary" icon="Plus" @click="addParam" size="small">添加参数</el-button>
                    </div>
                </el-form-item>
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

<script setup name="AlgorithmManagement">
import { listAlgorithm, getAlgorithm, deleteAlgorithm, insertAlgorithm, updateAlgorithm } from '@/api/systemPlus/systemCooperation/algorithm'
import { ref, reactive, toRefs, onMounted, getCurrentInstance } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDict } from '@/utils/dict'
import DynamicTable from '@/components/DynamicTable/index.vue'

const { proxy } = getCurrentInstance()

// 获取字典数据
const {
    pgzc_algorithm_type: algorithmTypeOptions,
    equipment_type: equipmentTypeOptions,
    yes_no: yesNoOptions,
    publish_status: publishStatusOptions,
    pgzc_param_category: paramCategoryOptions
} = useDict(
    'pgzc_algorithm_type',
    'equipment_type',
    'yes_no',
    'publish_status',
    'pgzc_param_category'
)

const algorithmList = ref([])
const open = ref(false)
const loading = ref(true)
const total = ref(0)
const title = ref("")

// 动态表格字段配置
const fieldConfig = ref([
    { key: 'id', label: 'ID', width: 240, showOverflowTooltip: true },
    { key: 'algorithmCode', label: '算法编码', width: 120, showOverflowTooltip: true },
    { key: 'algorithmName', label: '算法名称', width: 150, showOverflowTooltip: true },
    { key: 'algorithmType', label: '算法类型', width: 120, showOverflowTooltip: true },
    { key: 'equipmentType', label: '装备类型', width: 120, showOverflowTooltip: true },
    { key: 'algorithmDescription', label: '算法描述', width: 200, showOverflowTooltip: true },
    {
        key: 'isBaseData', label: '是否基础数据', width: 120,
        formatter: (row) => row.isBaseData === '1' ? '是' : '否'
    },
    {
        key: 'publishStatus', label: '发布状态', width: 100,
        formatter: (row) => row.publishStatus === '1' ? '已发布' : '未发布'
    },
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
        algorithmName: undefined
    },
    rules: {
        algorithmCode: [{ required: true, message: "算法编码不能为空", trigger: "blur" }],
        algorithmName: [{ required: true, message: "算法名称不能为空", trigger: "blur" }],
        algorithmType: [{ required: true, message: "算法类型不能为空", trigger: "change" }],
        equipmentType: [{ required: true, message: "装备类型不能为空", trigger: "change" }],
        // algorithmInput: {
        //     dataType: [{ required: true, message: "数据类型不能为空", trigger: "change" }],
        //     description: [{ required: true, message: "输入描述不能为空", trigger: "blur" }]
        // },
        algorithmDescription: [{ required: true, message: "算法描述不能为空", trigger: "blur" }],
        isBaseData: [{ required: true, message: "请选择是否基础数据", trigger: "change" }],
        publishStatus: [{ required: true, message: "请选择发布状态", trigger: "change" }]
    }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询列表 */
function getList() {
    loading.value = true
    listAlgorithm(queryParams.value).then(response => {
        algorithmList.value = response.rows
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
    title.value = "新增算法"
}

/** 修改按钮操作 */
function handleUpdate(row) {
    reset()
    getAlgorithm(row.id).then(response => {
        form.value = response.data
        open.value = true
        title.value = "修改算法"
    })
}

/** 删除按钮操作 */
function handleDelete(row) {
    proxy.$modal.confirm('是否确认删除算法编号为"' + row.id + '"的数据项？').then(function () {
        return deleteAlgorithm(row.id)
    }).then(() => {
        getList()
        proxy.$modal.msgSuccess("删除成功")
    }).catch(() => { })
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs["algorithmRef"].validate(valid => {
        if (valid) {
            if (form.value.id) {
                updateAlgorithm(form.value).then(response => {
                    proxy.$modal.msgSuccess("修改成功")
                    open.value = false
                    getList()
                })
            } else {
                insertAlgorithm(form.value).then(response => {
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

/** 添加算法参数 */
function addParam() {
    form.value.algorithmParams.push({
        name: '',
        label: '',
        type: '',
        value: ''
    })
}

/** 删除算法参数 */
function removeParam(index) {
    form.value.algorithmParams.splice(index, 1)
}

/** 表单重置 */
function reset() {
    form.value = {
        id: null,
        algorithmCode: '',
        algorithmName: '',
        algorithmType: '',
        equipmentType: '',
        algorithmDescription: '',
        isBaseData: '0',
        algorithmInput: {
            dataType: '',
            description: ''
        },
        algorithmParams: [{
            name: '',
            label: '',
            type: '',
            value: ''
        }],
        publishStatus: '0',
        creator: 'admin'
    }
    proxy.resetForm("algorithmRef")
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