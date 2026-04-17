<template>
    <div class="subpage-container">
        <!-- 概览统计区 -->
        <el-row :gutter="20" class="stat-cards-container">
            <el-col :span="8">
                <el-card class="tech-card stat-card" shadow="hover">
                    <div class="stat-content">
                        <div class="stat-icon-wrapper" style="color: #00f2ff;">
                            <el-icon :size="30">
                                <Monitor />
                            </el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="stat-label">总工程数</div>
                            <div class="stat-value">24</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="8">
                <el-card class="tech-card stat-card" shadow="hover">
                    <div class="stat-content">
                        <div class="stat-icon-wrapper" style="color: #ffbd2e;">
                            <el-icon :size="30">
                                <Loading />
                            </el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="stat-label">进行中</div>
                            <div class="stat-value">5</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="8">
                <el-card class="tech-card stat-card" shadow="hover">
                    <div class="stat-content">
                        <div class="stat-icon-wrapper" style="color: #00ff9d;">
                            <el-icon :size="30">
                                <CircleCheck />
                            </el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="stat-label">已完成</div>
                            <div class="stat-value">16</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <!-- 工具栏 -->
        <div class="page-header">
            <div class="left">
                <h3 class="page-title">评估工程列表</h3>
            </div>
            <div class="right">
                <el-select v-model="filterType" placeholder="工程类型" clearable class="filter-select">
                    <el-option label="链式评估" value="链式评估" />
                    <el-option label="辅助计算" value="辅助计算" />
                    <el-option label="综合分析" value="综合分析" />
                    <el-option label="体系协同" value="体系协同" />
                </el-select>
                <el-input v-model="searchQuery" placeholder="搜索工程名称/编号" clearable
                    style="width: 240px; margin-right: 12px; margin-left: 12px;" prefix-icon="Search" />
                <el-button type="primary" icon="Plus" @click="handleAdd">新建工程</el-button>
            </div>
        </div>

        <!-- 工程列表（卡片式） -->
        <el-row :gutter="20" class="project-list">
            <el-col :span="8" v-for="item in filteredProjects" :key="item.id" style="margin-bottom: 20px;">
                <el-card class="tech-card project-card">
                    <div class="card-header">
                        <div class="title-area">
                            <span class="project-id">{{ item.id }}</span>
                            <h4 class="project-name" :title="item.name">{{ item.name }}</h4>
                        </div>
                        <el-tag :type="getStatusType(item.status)" size="small" effect="dark" class="status-tag">
                            {{ item.status }}
                        </el-tag>
                    </div>

                    <div class="card-body">
                        <div class="info-row">
                            <span class="label">工程类型:</span>
                            <span class="value type-value">{{ item.type }}</span>
                        </div>
                        <div class="info-row">
                            <span class="label">关联需求:</span>
                            <span class="value">{{ item.requirement }}</span>
                        </div>

                        <div class="progress-section">
                            <div class="progress-header">
                                <span class="label">任务进度</span>
                                <span class="progress-text">{{ item.completedTasks }}/{{ item.totalTasks }}</span>
                            </div>
                            <el-progress
                                :percentage="item.totalTasks === 0 ? 0 : Math.round((item.completedTasks / item.totalTasks) * 100)"
                                :color="getCustomColor" :show-text="false" class="tech-progress" />
                        </div>
                    </div>

                    <div class="card-footer">
                        <el-button type="primary" link @click="handleViewDetail(item)">查看详情</el-button>
                        <div class="right-actions">
                            <el-button type="info" link @click="handleEdit(item)" icon="Edit">编辑</el-button>
                            <el-button type="danger" link @click="handleDelete(item)" icon="Delete">删除</el-button>
                        </div>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <div class="pagination-container">
            <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[9, 18, 27, 36]"
                layout="total, sizes, prev, pager, next" :total="total" @size-change="handleSizeChange"
                @current-change="handleCurrentChange" />
        </div>

        <!-- 新建工程表单对话框 -->
        <el-dialog v-model="dialogVisible" :title="dialogType === 'add' ? '新建评估工程' : '编辑评估工程'" width="850px"
            class="tech-dialog" :close-on-click-modal="false">
            <el-form :model="formData" :rules="formRules" ref="formRef" label-width="110px" class="project-form">
                <el-form-item label="工程名称" prop="name">
                    <el-input v-model="formData.name" placeholder="请输入工程名称，例如: 2024 天相验证 评估工程" />
                </el-form-item>

                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="工程类型" prop="type">
                            <el-select v-model="formData.type" placeholder="请选择类别" style="width: 100%">
                                <el-option label="链式评估" value="链式评估" />
                                <el-option label="辅助计算" value="辅助计算" />
                                <el-option label="综合分析" value="综合分析" />
                                <el-option label="体系协同" value="体系协同" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="优先级" prop="priority">
                            <el-select v-model="formData.priority" placeholder="紧急程度" style="width: 100%">
                                <el-option label="高" value="high" />
                                <el-option label="中" value="medium" />
                                <el-option label="低" value="low" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                </el-row>

                <el-form-item label="关联需求" prop="requirement">
                    <el-select v-model="formData.requirement" placeholder="选择接收的评估需求" style="width: 100%" clearable>
                        <el-option label="卫星通信载荷效能评估" value="REQ-01" />
                        <el-option label="网络安全渗透测试需求" value="REQ-02" />
                        <el-option label="全链路并发测试评估" value="REQ-03" />
                    </el-select>
                </el-form-item>

                <el-form-item label="任务分配" prop="tasks">
                    <el-select v-model="formData.tasks" multiple placeholder="请分配工程评估任务" style="width: 100%"
                        allow-create filterable>
                        <el-option label="卫星通信网络效能评估任务" value="task1" />
                        <el-option label="星间链路抗毁性评估任务" value="task2" />
                        <el-option label="机动平台天线增益评估任务" value="task3" />
                        <el-option label="全链路测控并发压力测试任务" value="task4" />
                    </el-select>
                </el-form-item>

                <el-form-item label="提醒信息设置" v-if="formData.type === '链式评估'" class="reminder-flow-item">
                    <div class="reminder-flow-container">
                        <div class="reminder-node" v-for="(node, index) in formData.reminders" :key="index">
                            <div class="node-circle">{{ index + 1 }}</div>
                            <div class="node-title">{{ node.name }}</div>
                            <div class="node-input">
                                <el-input v-model="node.message" placeholder="输入提醒信息" size="small" />
                            </div>
                            <!-- 连接线 -->
                            <div class="node-line" v-if="index < formData.reminders.length - 1"></div>
                        </div>
                    </div>
                </el-form-item>

                <el-form-item label="工程描述" prop="desc">
                    <el-input v-model="formData.desc" type="textarea" :rows="3" placeholder="简要描述工程建设目标与背景" />
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="dialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="submitForm">确认创设</el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Monitor, Loading, CircleCheck, Edit, Delete } from '@element-plus/icons-vue'

const router = useRouter()

// 筛选与分页
const searchQuery = ref('')
const filterType = ref('')
const currentPage = ref(1)
const pageSize = ref(9)
const total = ref(100)

// 模拟工程数据
const allProjects = ref([
    { id: 'PROJ-2401', name: '2024天相一号评估工程', type: '链式评估', requirement: '卫星通信载荷效能评估 (REQ-01)', status: '进行中', completedTasks: 8, totalTasks: 12, deadline: '2026-06-30' },
    { id: 'PROJ-2402', name: '2024区域拒止环境评估工程', type: '综合分析', requirement: '网络安全渗透测试需求 (REQ-02)', status: '待启动', completedTasks: 0, totalTasks: 5, deadline: '2026-07-15' },
    { id: 'PROJ-2403', name: '2024低轨测控链路评估工程', type: '链式评估', requirement: '全链路并发测试评估 (REQ-03)', status: '进行中', completedTasks: 6, totalTasks: 15, deadline: '2026-05-20' },
    { id: 'PROJ-2315', name: '2023机动天线增益评估工程', type: '辅助计算', requirement: '终端天线增益评估 (REQ-04)', status: '已完成', completedTasks: 8, totalTasks: 8, deadline: '2025-12-10' },
    { id: 'PROJ-2405', name: '2024组网抗毁性分析评估工程', type: '体系协同', requirement: '组网协议抗毁性评估 (REQ-05)', status: '进行中', completedTasks: 2, totalTasks: 10, deadline: '2026-08-01' },
    { id: 'PROJ-2310', name: '2023天基红外校准评估工程', type: '链式评估', requirement: '红外预警模型标校 (REQ-06)', status: '已完成', completedTasks: 20, totalTasks: 20, deadline: '2025-11-30' }
])

// 过滤后的列表
const filteredProjects = computed(() => {
    return allProjects.value.filter(proj => {
        const matchName = proj.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
            proj.id.toLowerCase().includes(searchQuery.value.toLowerCase())
        const matchType = filterType.value ? proj.type === filterType.value : true
        return matchName && matchType
    })
})

// 表单相关
const dialogVisible = ref(false)
const dialogType = ref('add')
const formRef = ref(null)
const formData = ref({
    name: '',
    type: '链式评估',
    requirement: '',
    tasks: [],
    priority: 'medium',
    desc: '',
    reminders: [
        { name: '需求设计', message: '' },
        { name: '数据采集汇总', message: '' },
        { name: '综合分析计算', message: '' },
        { name: '结果分析与溯源', message: '' }
    ]
})

const formRules = {
    name: [{ required: true, message: '请输入工程名称', trigger: 'blur' }],
    type: [{ required: true, message: '请选择工程类型', trigger: 'change' }]
}

// 辅助函数
const getStatusType = (status) => {
    const map = {
        '待启动': 'info',
        '进行中': 'primary',
        '已完成': 'success'
    }
    return map[status] || 'info'
}

const getCustomColor = (percentage) => {
    if (percentage < 30) return '#ff3c5b'
    if (percentage < 70) return '#ffbd2e'
    if (percentage < 100) return '#00f2ff'
    return '#00ff9d'
}

// 路由与操作
const handleViewDetail = (row) => {
    router.push(`/program-mgmt/detail/${row.id}`)
}

const handleAdd = () => {
    const currentYear = new Date().getFullYear();
    dialogType.value = 'add'
    formData.value = {
        name: `${currentYear} XXXX 评估工程`, type: '链式评估', requirement: '', tasks: [],
        priority: 'medium', desc: '',
        reminders: [
            { name: '需求设计', message: '' },
            { name: '数据采集汇总', message: '' },
            { name: '综合分析计算', message: '' },
            { name: '结果分析与溯源', message: '' }
        ]
    }
    dialogVisible.value = true
}

const handleEdit = (row) => {
    dialogType.value = 'edit'
    // 简单模拟数据回显
    formData.value = {
        name: row.name,
        type: row.type,
        requirement: row.requirement.split('(')[1]?.replace(')', '') || '',
        tasks: ['task1', 'task2'],
        priority: 'high',
        desc: '',
        reminders: [
            { name: '需求设计', message: '完成需求解析后触发通知' },
            { name: '数据采集汇总', message: '所有数据上传完成后触发通知' },
            { name: '综合分析计算', message: '模型运算结束后触发告警' },
            { name: '结果分析与溯源', message: '报告生成后抄送管理层' }
        ]
    }
    dialogVisible.value = true
}

const handleDelete = (row) => {
    ElMessageBox.confirm(`确定要删除工程 [${row.name}] 吗? 关联的数据也会一并归档。`, '系统警告', {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        customClass: 'cyber-message-box'
    }).then(() => {
        ElMessage.success(`工程 ${row.id} 已成功删除`)
    }).catch(() => { })
}

const submitForm = async () => {
    if (!formRef.value) return
    await formRef.value.validate((valid) => {
        if (valid) {
            ElMessage.success(dialogType.value === 'add' ? '工程创设成功，已分配专属资源池' : '工程信息更新完毕')
            dialogVisible.value = false
        }
    })
}

const handleSizeChange = (val) => { pageSize.value = val }
const handleCurrentChange = (val) => { currentPage.value = val }
</script>

<style scoped lang="scss">
@use '@/assets/styles/v2/variables.scss' as *;

.subpage-container {
    padding: 24px;
    height: 100%;
    overflow-y: auto;
    box-sizing: border-box;
}

/* 统计卡片 */
.stat-cards-container {
    margin-bottom: 24px;
}

.stat-card {
    height: 100px;
    background: linear-gradient(135deg, rgba(16, 33, 56, 0.8) 0%, rgba(10, 20, 35, 0.9) 100%) !important;
    border-top: 2px solid transparent !important;
    transition: all 0.3s ease;

    &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba(0, 0, 0, 0.6), inset 0 0 15px rgba(0, 242, 255, 0.1) !important;
    }

    /* 首张为总数，使用cyan色 */
    .el-col:nth-child(1) & {
        border-top-color: #00f2ff !important;
    }

    /* 第二张进行中，黄 */
    .el-col:nth-child(2) & {
        border-top-color: #ffbd2e !important;
    }

    /* 第三张完成，绿 */
    .el-col:nth-child(3) & {
        border-top-color: #00ff9d !important;
    }
}

.stat-content {
    display: flex;
    align-items: center;
    gap: 20px;
    height: 100%;

    .stat-icon-wrapper {
        width: 50px;
        height: 50px;
        border-radius: 12px;
        background: rgba(255, 255, 255, 0.05);
        display: flex;
        align-items: center;
        justify-content: center;
        border: 1px solid rgba(255, 255, 255, 0.1);
    }

    .stat-info {
        display: flex;
        flex-direction: column;

        .stat-label {
            font-size: 13px;
            color: $text-color-secondary;
            margin-bottom: 5px;
            letter-spacing: 1px;
        }

        .stat-value {
            font-size: 28px;
            font-weight: bold;
            color: #fff;
            font-family: 'Roboto Mono', monospace;
            line-height: 1;
        }
    }
}

/* 工具栏 */
.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .page-title {
        margin: 0;
        font-size: 20px;
        font-weight: bold;
        color: #fff;
        border-left: 4px solid $primary-color;
        padding-left: 12px;
        letter-spacing: 1px;
    }

    .right {
        display: flex;
        align-items: center;
    }
}

/* 过滤框特殊样式 */
.filter-select {
    width: 140px;

    :deep(.el-input__wrapper) {
        background-color: rgba(0, 242, 255, 0.05) !important;
        box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.3) inset !important;
    }
}

/* 工程卡片列表 */
.project-card {
    height: 220px;
    display: flex;
    flex-direction: column;
    background: rgba(16, 32, 53, 0.6) !important;
    border: 1px solid rgba(0, 242, 255, 0.1) !important;
    transition: all 0.3s;
    position: relative;
    overflow: hidden;

    &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 2px;
        background: linear-gradient(90deg, transparent, $primary-color, transparent);
        opacity: 0;
        transition: opacity 0.3s;
    }

    &:hover {
        transform: translateY(-4px);
        box-shadow: 0 10px 20px rgba(0, 0, 0, 0.5), 0 0 15px rgba(0, 242, 255, 0.1) !important;
        border-color: rgba(0, 242, 255, 0.3) !important;

        &::before {
            opacity: 1;
        }
    }

    :deep(.el-card__body) {
        padding: 0;
        height: 100%;
        display: flex;
        flex-direction: column;
    }
}

.card-header {
    padding: 15px 20px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    display: flex;
    justify-content: space-between;
    align-items: flex-start;

    .title-area {
        flex: 1;
        min-width: 0;
        padding-right: 15px;

        .project-id {
            font-size: 12px;
            color: $primary-color;
            font-family: 'Roboto Mono', monospace;
            display: block;
            margin-bottom: 4px;
            letter-spacing: 1px;
        }

        .project-name {
            margin: 0;
            font-size: 16px;
            color: #fff;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
    }

    .status-tag {
        flex-shrink: 0;
        border-radius: 4px;
        font-weight: bold;
    }
}

.card-body {
    padding: 15px 20px;
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;

    .info-row {
        display: flex;
        font-size: 13px;

        .label {
            color: $text-color-secondary;
            width: 70px;
            flex-shrink: 0;
        }

        .value {
            color: #dcdcdc;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;

            &.type-value {
                color: $primary-color;
            }
        }
    }
}

.progress-section {
    margin-top: auto;
    padding-top: 10px;

    .progress-header {
        display: flex;
        justify-content: space-between;
        font-size: 12px;
        margin-bottom: 5px;

        .label {
            color: $text-color-secondary;
        }

        .progress-text {
            color: #fff;
            font-family: 'Roboto Mono', monospace;
        }
    }
}

/* 进度条样式覆盖 */
.tech-progress {
    :deep(.el-progress-bar__outer) {
        background-color: rgba(255, 255, 255, 0.1);
        border-radius: 2px;
        height: 6px !important;
    }

    :deep(.el-progress-bar__inner) {
        border-radius: 2px;
        box-shadow: 0 0 8px currentColor;
    }
}

.card-footer {
    padding: 10px 20px;
    background: rgba(0, 0, 0, 0.2);
    border-top: 1px solid rgba(255, 255, 255, 0.05);
    display: flex;
    justify-content: space-between;
    align-items: center;

    .right-actions {
        display: flex;
        gap: 10px;
    }
}

.pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
}

/* 对话框表单样式 */
.project-form {
    :deep(.el-form-item__label) {
        color: #e0eafc;
    }

    :deep(.el-input__wrapper),
    :deep(.el-textarea__inner) {
        background-color: rgba(10, 20, 35, 0.8) !important;
    }
}

/* 横向流程图提醒设置 */
.reminder-flow-item {
    margin-top: 10px;
    margin-bottom: 25px;
}

.reminder-flow-container {
    display: flex;
    justify-content: space-between;
    width: 100%;
    position: relative;
    padding-top: 10px;
    align-items: flex-start;
}

.reminder-node {
    display: flex;
    flex-direction: column;
    align-items: center;
    position: relative;
    flex: 1;
    z-index: 2;

    .node-circle {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        background: rgba(0, 242, 255, 0.1);
        border: 1px solid $primary-color;
        color: $primary-color;
        display: flex;
        justify-content: center;
        align-items: center;
        font-weight: bold;
        font-size: 14px;
        margin-bottom: 8px;
        box-shadow: 0 0 10px rgba(0, 242, 255, 0.3);
        z-index: 3;
    }

    .node-title {
        font-size: 12px;
        color: #e0eafc;
        margin-bottom: 8px;
        text-align: center;
        white-space: nowrap;
    }

    .node-input {
        width: 90%;

        :deep(.el-input__wrapper) {
            background-color: rgba(0, 0, 0, 0.3) !important;
            padding: 0 8px;
        }

        :deep(.el-input__inner) {
            font-size: 11px;
            text-align: center;
        }
    }

    .node-line {
        position: absolute;
        top: 14px;
        left: calc(50% + 20px);
        width: calc(100% - 40px);
        height: 0;
        z-index: 1;
        border-top: 1px dashed rgba(0, 242, 255, 0.5);
    }
}
</style>
