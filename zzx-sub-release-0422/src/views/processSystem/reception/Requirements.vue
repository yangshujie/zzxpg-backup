<template>
    <div class="subpage-container">
        <div class="page-header">
            <div class="left">
                <h3 class="page-title">评估需求列表</h3>
            </div>
            <div class="right">
                <el-input v-model="searchQuery" placeholder="搜索需求名称/编号" clearable
                    style="width: 240px; margin-right: 12px;" prefix-icon="Search" />
                <el-button type="primary" icon="Plus" @click="handleAdd">新增需求</el-button>
            </div>
        </div>

        <el-card class="tech-card table-section">
            <el-table :data="tableData" style="width: 100%" class="tech-table">
                <el-table-column type="selection" width="55" />
                <el-table-column prop="id" label="编号" width="100" />
                <el-table-column prop="name" label="需求名称" min-width="180" show-overflow-tooltip />
                <el-table-column prop="project" label="评估工程" min-width="150" show-overflow-tooltip />
                <el-table-column prop="indicatorSystemCount" label="指标体系" width="120">
                    <template #default="{ row }">
                        <el-link type="primary" @click="viewIndicators(row)" :underline="false">
                            {{ row.indicatorSystemCount }} 项
                        </el-link>
                    </template>
                </el-table-column>
                <el-table-column prop="requirementDetails" label="需求要求" min-width="150" show-overflow-tooltip />
                <el-table-column prop="type" label="需求类型" width="120">
                    <template #default="{ row }">
                        <span class="type-tag">{{ row.type }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="creator" label="提出人" width="120" />
                <el-table-column prop="createTime" label="提出日期" width="160" />
                <el-table-column prop="status" label="状态" width="120">
                    <template #default="{ row }">
                        <el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="280" fixed="right">
                    <template #default="scope">
                        <el-button v-if="scope.row.status === '待设计表单'" link type="primary"
                            @click="handleManualDesign(scope.row)">手动设计</el-button>
                        <el-button v-if="scope.row.status === '已生成表单'" link type="primary"
                            @click="handleIssue(scope.row)">下发</el-button>
                        <el-button link type="primary" @click="handleView(scope.row)">采集表单</el-button>
                        <!-- <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button> -->
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-container">
                <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize"
                    :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" :total="total"
                    @size-change="handleSizeChange" @current-change="handleCurrentChange" />
            </div>
        </el-card>

        <!-- Add/Edit Dialog -->
        <el-dialog v-model="dialogVisible" :title="dialogType === 'add' ? '新增评估需求' : '编辑评估需求'" width="600px"
            class="tech-dialog">
            <el-form :model="formData" label-width="100px">
                <el-form-item label="需求名称">
                    <el-input v-model="formData.name" placeholder="请输入需求名称" />
                </el-form-item>
                <el-form-item label="需求类型">
                    <el-select v-model="formData.type" placeholder="请选择需求类型" style="width: 100%">
                        <el-option label="专项评估" value="专项评估" />
                        <el-option label="定期评估" value="定期评估" />
                        <el-option label="随机评估" value="随机评估" />
                    </el-select>
                </el-form-item>
                <el-form-item label="需求描述">
                    <el-input v-model="formData.desc" type="textarea" :rows="4" placeholder="请输入详情描述" />
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="dialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="submitForm">确认</el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'

const router = useRouter()

const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(100)

const tableData = ref([
    { id: 'REQ-01', name: '卫星通信载荷效能评估', project: '天基网络质量工程', indicatorSystemCount: 3, requirementDetails: '需要覆盖关键频段的误码率和星地链路延迟测试', type: '专项评估', creator: '李处长', createTime: '2024-03-09', status: '待设计表单' },
    { id: 'REQ-02', name: '网络安全渗透测试需求', project: '某型指控系统工程', indicatorSystemCount: 1, requirementDetails: '内外网攻防及系统访问控制验证', type: '定期评估', creator: '王主任', createTime: '2024-03-08', status: '已生成表单' },
    { id: 'REQ-03', name: '全链路并发测试评估', project: '低轨星座测控工程', indicatorSystemCount: 5, requirementDetails: '多星并发场景下的上下行指令丢包率分析', type: '专项评估', creator: '张工', createTime: '2024-03-07', status: '下发中' },
    { id: 'REQ-04', name: '终端天线增益评估', project: '车载终端升级工程', indicatorSystemCount: 2, requirementDetails: '评估各种极端仰角下的天线收发增益', type: '随机评估', creator: '刘组长', createTime: '2024-03-06', status: '已完成' },
])

const dialogVisible = ref(false)
const dialogType = ref('add')
const formData = ref({
    name: '',
    type: '',
    desc: ''
})

const getStatusType = (status) => {
    const map = {
        '待设计表单': 'warning',
        '已生成表单': 'primary',
        '下发中': 'info',
        '已完成': 'success'
    }
    return map[status] || 'info'
}

const handleManualDesign = (row) => {
    ElMessage.info(`进入手动设计表单页面: ${row.name}`)
}

const handleIssue = (row) => {
    ElMessage.success(`需求 ${row.name} 下发成功`)
}

const viewIndicators = (row) => {
    ElMessage.info(`查看需求 ${row.name} 的 ${row.indicatorSystemCount} 个关联指标体系详情`)
}

const handleAdd = () => {
    dialogType.value = 'add'
    formData.value = { name: '', type: '', desc: '' }
    dialogVisible.value = true
}

const handleEdit = (row) => {
    dialogType.value = 'edit'
    formData.value = { ...row }
    dialogVisible.value = true
}

const handleView = (row) => {
    ElMessage.success(`正在根据需求 "${row.name}" 智能生成采集表单...`)
    router.push({
        path: '/process/reception-sys/designer',
        query: { 
            reqId: row.id, 
            reqName: row.name,
            source: 'requirement'
        }
    })
}

const handleDelete = (row) => {
    ElMessageBox.confirm(`确定删除需求 ${row.id} 吗?`, '警告', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
    }).then(() => {
        ElMessage.success('删除成功')
    })
}

const submitForm = () => {
    ElMessage.success(dialogType.value === 'add' ? '新增成功' : '编辑成功')
    dialogVisible.value = false
}

const handleSizeChange = (val) => { pageSize.value = val }
const handleCurrentChange = (val) => { currentPage.value = val }
</script>

<style scoped lang="scss">
.subpage-container {
    padding: 24px;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    .page-title {
        margin: 0;
        font-size: 24px;
        font-weight: bold;
        color: #fff;
        border-left: 4px solid #00f2ff;
        padding-left: 12px;
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
        background: rgba(0, 242, 255, 0.05) !important;
        color: #8fa3b8 !important;
        border-bottom: 1px solid rgba(0, 242, 255, 0.1) !important;
    }

    /* Fixed column backgrounds to prevent overlap transparency */
    .el-table-fixed-column--right,
    .el-table-fixed-column--left {
        background-color: #0c1a2c !important;
    }

    th.el-table-fixed-column--right,
    th.el-table-fixed-column--left {
        background-color: #10273a !important;
    }
}

.pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
}

.type-tag {
    color: #00f2ff;
    font-size: 13px;
}
</style>
