<template>
    <div class="subpage-container">
        <div class="page-header">
            <div class="left">
                <h3 class="page-title">采集表单列表</h3>
            </div>
            <div class="right">
                <el-radio-group v-model="filterStatus" class="tech-radio" style="margin-right: 20px;"
                    @change="fetchList">
                    <el-radio-button label="">全部</el-radio-button>
                    <el-radio-button label="NOT_ISSUED">未下发</el-radio-button>
                    <el-radio-button label="COLLECTING">采集中</el-radio-button>
                    <el-radio-button label="COMPLETED">已完成</el-radio-button>
                </el-radio-group>
                <el-button type="success" icon="Position" :disabled="selectedIds.length === 0" @click="handleBatchIssue"
                    style="margin-right: 12px;">批量下发</el-button>

                <el-button type="primary" icon="Plus" @click="handleCreateCommand">
                    创建表单
                </el-button>

            </div>
        </div>

        <div class="filter-bar tech-card">
            <div class="filter-item">
                <span class="label">表单名称/标识</span>
                <el-input v-model="searchFormName" placeholder="表单名称/识别码" clearable prefix-icon="Search"
                    class="tech-input" @change="fetchList" />
            </div>
            <div class="filter-item">
                <span class="label">评估工程</span>
                <el-input v-model="searchProjectName" placeholder="工程名称" clearable prefix-icon="Search"
                    class="tech-input" @change="fetchList" />
            </div>
            <div class="filter-item">
                <span class="label">需求标识</span>
                <el-input v-model="searchRequireCode" placeholder="搜索需求标识" clearable prefix-icon="Search"
                    class="tech-input" @change="fetchList" />
            </div>
            <div class="filter-item">
                <span class="label">采集表类型</span>
                <el-select v-model="filterFormType" placeholder="全部类型" clearable class="tech-select"
                    @change="fetchList">
                    <el-option label="航天侦察装备" value="technical" />
                    <el-option label="航天测控装备" value="resource" />
                </el-select>
            </div>
            <div class="filter-item">
                <span class="label">引接类型</span>
                <el-select v-model="filterImportType" placeholder="全部引接类型" clearable class="tech-select"
                    @change="fetchList">
                    <el-option label="离线填报" value="manual" />
                    <el-option label="在线引接" value="api" />
                    <el-option label="离线汇集" value="db" />
                </el-select>
            </div>
        </div>

        <el-card class="tech-card">
            <el-table :data="tableData" v-loading="loading" class="tech-table"
                @selection-change="handleSelectionChange">
                <el-table-column type="selection" width="55" />
                <el-table-column type="expand">
                    <template #default="{ row }">
                        <div class="expand-content">
                            <p><strong>采集要求:</strong> {{ row.collectRequirement || '无' }}</p>
                            <p><strong>备注信息:</strong> {{ row.remark || '无' }}</p>
                            <p v-if="row.issuingUnit"><strong>下发系统/单位:</strong> {{ row.issuingUnit }}</p>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="formName" label="表单名称" min-width="180" show-overflow-tooltip />
                <el-table-column prop="formCode" label="表单标识" width="120" show-overflow-tooltip />
                <el-table-column prop="evaluationProjectName" label="所属工程" min-width="150" show-overflow-tooltip />
                <el-table-column prop="requireName" label="需求名称" min-width="150" show-overflow-tooltip />
                <el-table-column prop="requireCode" label="需求标识" width="120" />
                <el-table-column label="下发单位" min-width="150">
                    <template #default="{ row }">
                        <div v-if="row.issuingUnit" class="unit-tags" @click="openSourceSelector(row)">
                            <el-tag size="small">{{ row.issuingUnit }}</el-tag>
                            <el-button link type="primary" size="small" icon="Edit"></el-button>
                        </div>
                        <el-button v-else link type="primary" icon="Plus" @click="openSourceSelector(row)">
                            绑定数据源
                        </el-button>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="创建时间" width="180" />
                <el-table-column prop="collectStatus" label="采集状态" width="120">
                    <template #default="{ row }">
                        <el-tag size="small" :type="getCollectStatusType(row.collectStatus)">
                            {{ getCollectStatusText(row.collectStatus) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="invokeType" label="引接类型" width="130" />
                <el-table-column label="操作" width="220" fixed="right">
                    <template #default="{ row }">
                        <div class="action-buttons">
                            <el-button link type="primary" @click="handleView(row)">查看</el-button>
                            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
                            <el-button v-if="row.collectStatus === 'NOT_ISSUED'" link type="primary"
                                @click="handleIssue(row)">下发</el-button>
                            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
                        </div>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-container" style="display: flex; justify-content: flex-end; margin-top: 20px;">
                <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize"
                    :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" :total="total"
                    @current-change="fetchList" @size-change="fetchList" />
            </div>
        </el-card>

        <!-- Data Source Selector Dialog -->
        <el-dialog v-model="sourceSelectorVisible" title="选择下发数据源/接口" width="560px" custom-class="tech-dialog">
            <div class="source-selector-container">
                <el-alert title="请选择要绑定此采集表单的数据源接口或数据库" type="info" :closable="false" show-icon class="mb-4" />
                <el-input v-model="sourceSearch" placeholder="搜索接口名或数据库名" prefix-icon="Search"
                    class="mb-4 tech-input" />
                <div class="source-list">
                    <el-checkbox-group v-model="tempSelectedSources" class="tech-checkbox-group">
                        <div v-for="item in filteredSourceList" :key="item.id" class="source-item">
                            <el-checkbox :label="item.name">
                                <template #default>
                                    <div class="source-info">
                                        <span class="source-name">{{ item.name }}</span>
                                        <el-tag size="mini" :type="item.type === 'API' ? 'warning' : 'success'"
                                            effect="dark" class="source-type">
                                            {{ item.type }}
                                        </el-tag>
                                    </div>
                                </template>
                            </el-checkbox>
                        </div>
                    </el-checkbox-group>
                </div>
            </div>
            <template #footer>
                <div class="dialog-action">
                    <el-button @click="sourceSelectorVisible = false">取消</el-button>
                    <el-button type="primary" @click="confirmSourceSelection">确认选择</el-button>
                </div>
            </template>
        </el-dialog>

        <!-- Form Fill Dialog -->
        <el-dialog v-model="fillVisible" :title="`填写表单 - ${activeFormTitle}`" width="800px" custom-class="fill-dialog">
            <div class="fill-form-container">
                <el-form label-position="top">
                    <div class="form-grid">
                        <div v-for="(field, index) in fillItems" :key="index"
                            :style="{ width: `calc(${field.width}% - 15px)` }" class="grid-item">
                            <el-form-item :label="field.label" :required="field.required" style="margin-bottom: 0;">
                                <el-input v-if="field.type === 'input'" :placeholder="field.placeholder" />
                                <el-select v-else-if="field.type === 'select'" style="width: 100%"
                                    :placeholder="field.placeholder">
                                    <el-option label="选项 A" value="A" />
                                    <el-option label="选项 B" value="B" />
                                </el-select>
                                <el-date-picker v-else-if="field.type === 'date'" style="width: 100%" />
                                <el-rate v-else-if="field.type === 'rate'" />
                            </el-form-item>
                        </div>
                    </div>
                </el-form>
            </div>
            <template #footer>
                <div class="dialog-action">
                    <el-button @click="fillVisible = false">取消</el-button>
                    <el-button type="primary" @click="submitForm">提交数据</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Plus, ArrowDown, Search, Edit } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listCollectForms, generateFormItems, changeFormStatus, batchIssueForms, issueForm, delCollectForms, updateCollectForm, listInterfaceInfosPage } from "@/api/dataCollection/index"

const router = useRouter()
const loading = ref(false)
const filterStatus = ref('')
const searchFormName = ref('')
const searchProjectName = ref('')
const searchRequireCode = ref('')
const filterFormType = ref('')
const filterImportType = ref('')

// Pagination
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const selectedIds = ref([])

const fillVisible = ref(false)
const activeFormTitle = ref('')
const fillItems = ref([])

// Data Source Selector State
const sourceSelectorVisible = ref(false)
const sourceSearch = ref('')
const tempSelectedSources = ref([])
const activeRow = ref(null)

const dataSourceList = ref([])

const fetchList = () => {
    loading.value = true
    const params = {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        formName: searchFormName.value || undefined,
        formCode: searchFormName.value || undefined, // Dual search for simplicity or add separate field
        evaluationProjectName: searchProjectName.value || undefined,
        requireCode: searchRequireCode.value || undefined,
        collectStatus: filterStatus.value !== 'all' && filterStatus.value ? filterStatus.value : undefined,
        invokeType: filterImportType.value || undefined
    }
    listCollectForms(params).then(res => {
        console.log("res ", res)
        tableData.value = res.data?.records || []
        total.value = res.data?.total || 0
        loading.value = false
    }).catch(() => {
        loading.value = false
    })
}

const fetchInterfaceInfos = () => {
    listInterfaceInfosPage({ pageNum: 1, pageSize: 100 }).then(res => {
        dataSourceList.value = (res.data?.records || []).map(item => ({
            id: item.id,
            name: item.interfaceName || item.name,
            type: item.interfaceType || 'API'
        }))
    })
}

onMounted(() => {
    if (route.query.id) {
        searchRequireCode.value = route.query.id
    }
    fetchList()
    // fetchInterfaceInfos()
})

const filteredSourceList = computed(() => {
    if (!sourceSearch.value) return dataSourceList.value
    return dataSourceList.value.filter(s => s.name.toLowerCase().includes(sourceSearch.value.toLowerCase()))
})

const handleSelectionChange = (val) => {
    selectedIds.value = val.map(item => item.id)
}

const getCollectStatusType = (status) => {
    const map = { 'COLLECTING': 'primary', 'COMPLETED': 'success', 'NOT_ISSUED': 'info', 'ABORTED': 'warning' }
    return map[status] || 'info'
}

const getCollectStatusText = (status) => {
    const map = { 'COLLECTING': '采集中', 'COMPLETED': '已完成', 'NOT_ISSUED': '未下发', 'ABORTED': '已中止' }
    return map[status] || status
}

const handleCreateCommand = () => {
    router.push('/process/reception-sys/designer')
}

const handleView = (row) => {
    ElMessage.info(`查看表单: ${row.name}`)
}

const handleEdit = (row) => {
    router.push({ path: '/process/reception-sys/designer', query: { id: row.id } })
}

const handleIssue = (row) => {
    ElMessageBox.confirm(`确定要下发表单「${row.formName || row.name}」吗？`, '提示', {
        type: 'warning'
    }).then(() => {
        issueForm(row.id).then(() => {
            ElMessage.success('表单下发成功')
            fetchList()
        })
    })
}

const handleBatchIssue = () => {
    if (selectedIds.value.length === 0) {
        ElMessage.warning('请先选择要下发的表单')
        return
    }
    ElMessageBox.confirm(`确定要下发选中的 ${selectedIds.value.length} 个表单吗？`, '提示', {
        type: 'warning'
    }).then(() => {
        batchIssueForms({ ids: selectedIds.value }).then(() => {
            ElMessage.success('批量下发任务已提交')
            fetchList()
        })
    })
}

const handleDelete = (row) => {
    ElMessageBox.confirm('确定要删除该采集表单吗？', '警告', {
        type: 'error'
    }).then(() => {
        delCollectForms(row.id).then(() => {
            ElMessage.success('删除成功')
            fetchList()
        })
    })
}

// Source Selection Methods
const openSourceSelector = (row) => {
    activeRow.value = row
    tempSelectedSources.value = [...(row.issueUnits || [])]
    sourceSelectorVisible.value = true
}

const confirmSourceSelection = () => {
    if (activeRow.value && tempSelectedSources.value.length > 0) {
        // Find the selected source ID
        const selectedSource = dataSourceList.value.find(s => s.name === tempSelectedSources.value[0])
        if (selectedSource) {
            const updateData = {
                ...activeRow.value,
                interfaceId: selectedSource.id,
                issuingUnit: selectedSource.name // Assuming issuingUnit corresponds to source name
            }
            updateCollectForm(updateData).then(() => {
                ElMessage.success('数据源绑定成功')
                sourceSelectorVisible.value = false
                fetchList()
            })
        }
    } else {
        sourceSelectorVisible.value = false
    }
}



// Mock form items for filling based on the selected row
fillItems.value = [
    { type: 'input', label: '记录主题', required: true, width: 100, placeholder: '请输入记录的主题内容' },
    { type: 'date', label: '记录时间', required: true, width: 50, placeholder: '请选择日期' },
    { type: 'select', label: '数据等级', required: true, width: 50, placeholder: '请选择...' },
    { type: 'rate', label: '重要程度评估', required: false, width: 100 },
    { type: 'input', label: '备注详情', required: false, width: 100, placeholder: '如有补充请记录在此' }
]

const submitForm = () => {
    ElMessage.success('数据提交成功！')
    fillVisible.value = false
}
</script>

<style scoped lang="scss">
.subpage-container {
    padding: 24px;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .page-title {
        margin: 0;
        font-size: 24px;
        font-weight: bold;
        color: #fff;
        border-left: 4px solid #00f2ff;
        padding-left: 12px;
    }
}

.filter-bar {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
    padding: 20px;
    margin-bottom: 20px;
    align-items: center;

    .filter-item {
        display: flex;
        align-items: center;
        gap: 12px;

        .label {
            font-size: 14px;
            color: #8fa3b8;
            white-space: nowrap;
        }

        .tech-input,
        .tech-select {
            width: 200px;
        }
    }
}

.tech-card {
    background: rgba(16, 32, 53, 0.6) !important;
    border: 1px solid rgba(0, 242, 255, 0.1) !important;
}

:deep(.tech-table) {
    background: transparent !important;
    color: #fff !important;

    th.el-table__cell {
        // background: rgba(0, 242, 255, 0.05) !important;
        color: #8fa3b8 !important;
        border-bottom: 1px solid rgba(0, 242, 255, 0.1) !important;
    }

    .el-table__row:hover {
        background: rgba(0, 242, 255, 0.02) !important;
    }
}

.unit-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    align-items: center;
    cursor: pointer;

    .el-tag {
        background: rgba(0, 242, 255, 0.1);
        border: 1px solid rgba(0, 242, 255, 0.3);
        color: #00f2ff;
    }
}

.status-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .status-indicator {
        width: 8px;
        height: 8px;
        border-radius: 50%;

        &.published {
            background: #67c23a;
            box-shadow: 0 0 8px #67c23a;
        }

        &.draft {
            background: #909399;
        }
    }
}

:deep(.tech-radio) {
    .el-radio-button__inner {
        background: rgba(10, 21, 37, 0.8) !important;
        border: 1px solid rgba(0, 242, 255, 0.2) !important;
        color: #8fa3b8;
    }

    .el-radio-button__original-radio:checked+.el-radio-button__inner {
        background: #00f2ff !important;
        color: #000;
        box-shadow: 0 0 10px rgba(0, 242, 255, 0.5);
    }
}

.expand-content {
    padding: 15px 50px;
    background: rgba(0, 0, 0, 0.2);
    border-radius: 4px;
    color: #8fa3b8;
    font-size: 14px;

    p {
        margin: 8px 0;

        strong {
            color: #00f2ff;
            margin-right: 10px;
        }
    }
}

.action-buttons {
    display: flex;
    align-items: center;
    gap: 8px;
}

// Dialogs
:deep(.tech-dialog),
:deep(.fill-dialog) {
    background: #0d1a2b !important;
    border: 1px solid #00f2ff !important;

    .el-dialog__title {
        color: #00f2ff !important;
    }

    .source-selector-container,
    .fill-form-container {
        padding: 10px;
    }

    .source-list {
        max-height: 300px;
        overflow-y: auto;
        padding: 10px;
        background: rgba(0, 0, 0, 0.2);
        border-radius: 4px;
    }

    .source-item {
        margin-bottom: 12px;

        &:last-child {
            margin-bottom: 0;
        }
    }

    .source-info {
        display: flex;
        align-items: center;
        gap: 12px;

        .source-name {
            color: #fff;
            font-size: 14px;
        }
    }

    .el-checkbox__label {
        flex: 1;
    }
}

:deep(.tech-checkbox-group) {
    .el-checkbox {
        display: flex;
        width: 100%;
        margin-right: 0;
        padding: 8px;
        border-radius: 4px;
        transition: background 0.3s;

        &:hover {
            background: rgba(0, 242, 255, 0.05);
        }

        &.is-checked {
            background: rgba(0, 242, 255, 0.1);
        }
    }

    .el-checkbox__inner {
        background: transparent;
        border-color: rgba(0, 242, 255, 0.5);
    }

    .el-checkbox__input.is-checked .el-checkbox__inner {
        background: #00f2ff;
        border-color: #00f2ff;
    }
}

.tech-input,
.tech-select {
    :deep(.el-input__wrapper) {
        background: rgba(0, 0, 0, 0.3) !important;
        border: 1px solid rgba(0, 242, 255, 0.2) !important;
        box-shadow: none !important;

        .el-input__inner {
            color: #fff !important;

            &::placeholder {
                color: rgba(255, 255, 255, 0.3);
            }
        }
    }
}

:deep(.el-select-dropdown) {
    background: #0d1a2b !important;
    border: 1px solid #00f2ff !important;

    .el-select-dropdown__item {
        color: #fff !important;

        &.hover,
        &:hover {
            background: rgba(0, 242, 255, 0.1) !important;
        }

        &.selected {
            color: #00f2ff !important;
            font-weight: bold;
        }
    }
}

.mb-4 {
    margin-bottom: 16px;
}
</style>
