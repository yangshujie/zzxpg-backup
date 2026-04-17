<template>
    <div class="subpage-container">
        <div class="page-header">
            <div class="left">
                <h3 class="page-title">预处理任务管理</h3>
            </div>
            <div class="right">
                <el-input v-model="searchQuery" placeholder="搜索任务名称/ID" clearable
                    style="width: 240px; margin-right: 12px;" prefix-icon="Search" />
                <el-button type="primary" icon="Plus" @click="handleAdd">新建任务</el-button>
            </div>
        </div>

        <el-card class="tech-card">
            <el-table :data="tableData" class="tech-table">
                <el-table-column prop="id" label="任务ID" width="120" />
                <el-table-column prop="name" label="任务名称" min-width="200">
                    <template #default="{ row }">
                        <el-link type="primary" :underline="false" @click="handleViewDetail(row)">{{ row.name
                        }}</el-link>
                    </template>
                </el-table-column>
                <el-table-column prop="type" label="预处理流程模板" width="150" />
                <el-table-column prop="sourceType" label="数据源类型" width="150" />
                <el-table-column prop="sourceUrl" label="数据源地址" width="150" />
                <el-table-column prop="status" label="状态" width="120">
                    <template #default="{ row }">
                        <div class="status-tag" :class="row.status">
                            <span class="dot"></span>
                            {{ row.statusText }}
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="updateTime" label="更新时间" width="180" />
                <el-table-column label="操作" width="220" fixed="right">
                    <template #default="scope">
                        <el-button link type="primary" @click="handleViewDetail(scope.row)">编辑</el-button>
                        <el-button link type="primary" @click="handleConfig(scope.row)">启动</el-button>
                        <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>


        <!-- Task Detail & Run Dialog -->
        <el-dialog v-model="detailVisible" title="预处理任务详情" width="900px" class="tech-dialog detail-dialog">
            <div class="detail-layout">
                <div class="detail-form-section">
                    <div class="section-title">基础配置</div>
                    <el-form :model="formData" label-width="120px" size="small">
                        <el-form-item label="任务名称">
                            <el-input v-model="formData.name" />
                        </el-form-item>
                        <el-form-item label="工程名称">
                            <el-input v-model="formData.projectName" />
                        </el-form-item>
                        <el-form-item label="数据源地址">
                            <el-input v-model="formData.sourceUrl" />
                        </el-form-item>
                        <el-form-item label="数据源类型">
                            <el-select v-model="formData.sourceType" style="width: 100%">
                                <el-option label="结构化数据" value="结构化数据" />
                                <el-option label="非结构化数据" value="非结构化数据" />
                                <el-option label="时序数据" value="时序数据" />
                            </el-select>
                        </el-form-item>
                        <el-form-item label="流程模板">
                            <el-input v-model="formData.type" readonly />
                        </el-form-item>
                        <el-form-item label="任务描述">
                            <el-input v-model="formData.desc" type="textarea" :rows="3" />
                        </el-form-item>
                    </el-form>
                </div>
                <div class="detail-workflow-section">
                    <div class="section-title">算子流水线</div>
                    <div class="config-workflow mini">
                        <div class="workflow-steps">
                            <div v-for="(op, index) in activeOperators" :key="index" class="step-item">
                                <div class="step-icon"><el-icon>
                                        <Cpu />
                                    </el-icon></div>
                                <div class="step-content">
                                    <div class="op-name">{{ op.name }}</div>
                                </div>
                                <div class="step-actions">
                                    <el-button link type="danger" icon="Delete" @click="removeOp(index)"></el-button>
                                </div>
                                <div v-if="index < activeOperators.length - 1" class="step-arrow">
                                    <el-icon>
                                        <ArrowDown />
                                    </el-icon>
                                </div>
                            </div>
                            <div class="add-step" @click="addOpVisible = true">
                                <el-icon>
                                    <Plus />
                                </el-icon>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <template #footer>
                <el-button @click="detailVisible = false">取消</el-button>
                <el-button type="success" icon="VideoPlay" @click="handleStartProcessing">开始预处理</el-button>
            </template>
        </el-dialog>

        <!-- Select Operator Dialog -->
        <el-dialog v-model="addOpVisible" title="选择算子" width="500px">
            <el-radio-group v-model="selectedOpTemplate" class="op-grid">
                <el-radio v-for="op in operatorTemplates" :key="op.name" :label="op" border>
                    {{ op.name }}
                </el-radio>
            </el-radio-group>
            <template #footer>
                <el-button @click="addOpVisible = false">取消</el-button>
                <el-button type="primary" @click="confirmAddOp">添加</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Search, Plus, Cpu, ArrowDown, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const searchQuery = ref('')
const dialogType = ref('add')
const detailVisible = ref(false)
const addOpVisible = ref(false)

const formData = ref({ name: '', type: '', desc: '', projectName: '', sourceUrl: '', sourceType: '' })
const activeOperators = ref([])
const selectedOpTemplate = ref(null)

const tableData = ref([
    { id: 'PROC-001', name: '高轨通信卫星宽带信号滤波任务', type: '时序数据平滑', projectName: '电磁评估工程A', sourceUrl: 'mysql://192.168.1.10', sourceType: '时序数据', desc: '处理卫星下行的原始滤波数据', status: 'running', statusText: '运行中', updateTime: '2024-03-10 10:00' },
    { id: 'PROC-002', name: '飞控中心遥测日志结构化抽取', type: '结构化数据清洗', projectName: '飞控保障项目', sourceUrl: 'ftp://172.16.0.5/logs', sourceType: '结构化数据', desc: '提取历史遥测日志中的关键状态', status: 'stopped', statusText: '已停止', updateTime: '2024-03-09 15:30' },
    { id: 'PROC-003', name: '光学遥感卫星影像降噪预处理', type: '非结构化数据转化', projectName: '遥感影像分析', sourceUrl: 's3://rs-bucket/raw', sourceType: '非结构化数据', desc: '卫星原始影像的去噪与格式转换', status: 'running', statusText: '运行中', updateTime: '2024-03-10 11:45' },
])

const operatorTemplates = [
    { name: '遥测断点插值补全', desc: '支持基于轨道模型的动力学平滑插值' },
    { name: '传感器噪声剔除', desc: '基于卡尔曼滤波自动提取高频干扰' },
    { name: '姿态角时序滑动平均', desc: '滚动平滑航天器三轴姿态测量值' },
    { name: '星历坐标系转换', desc: 'J2000与WGS84坐标系互换' }
]

const handleAdd = () => {
    dialogType.value = 'add'
    formData.value = { name: '', type: '', desc: '', projectName: '', sourceUrl: '', sourceType: '' }
    activeOperators.value = []
    detailVisible.value = true
}

const handleViewDetail = (row) => {
    formData.value = { ...row }
    activeOperators.value = [
        { name: '传感器噪声剔除', desc: '基于卡尔曼滤波自动提取高频干扰' },
        { name: '姿态角时序滑动平均', desc: '滚动平滑航天器三轴姿态测量值' }
    ]
    detailVisible.value = true
}

const handleConfig = (row) => {
    ElMessage.success('任务已启动')
}

const handleDelete = (row) => {
    ElMessageBox.confirm(`确定删除任务 ${row.name} 吗？`, '警告', {
        type: 'warning'
    }).then(() => {
        ElMessage.success('任务已删除')
    })
}

const submitForm = () => {
    ElMessage.success(dialogType.value === 'add' ? '创建成功' : '修改成功')
    dialogVisible.value = false
}

const handleStartProcessing = () => {
    ElMessage.success('预处理任务已启动')
    detailVisible.value = false
}

const removeOp = (index) => {
    activeOperators.value.splice(index, 1)
}

const confirmAddOp = () => {
    if (selectedOpTemplate.value) {
        activeOperators.value.push({ ...selectedOpTemplate.value })
        addOpVisible.value = false
        selectedOpTemplate.value = null
    }
}

onMounted(() => {
    const id = route.query.id
    if (id) {
        const task = tableData.value.find(t => t.id === id)
        if (task) {
            handleViewDetail(task)
        }
    }
})
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

.tech-table {
    background: transparent !important;

    :deep(tr) {
        background: transparent !important;
        color: #fff;
    }

    :deep(th.el-table__cell) {
        background: rgba(0, 242, 255, 0.05) !important;
        color: #8fa3b8;
        border-bottom: 1px solid rgba(0, 242, 255, 0.1) !important;
    }
}

.status-tag {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;

    .dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
    }

    &.running {
        color: #67c23a;

        .dot {
            background: #67c23a;
            box-shadow: 0 0 8px #67c23a;
        }
    }

    &.stopped {
        color: #909399;

        .dot {
            background: #909399;
        }
    }
}

.config-workflow {
    background: rgba(10, 21, 37, 0.8);
    padding: 30px;
    border-radius: 8px;

    .workflow-steps {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 20px;

        .step-item {
            width: 100%;
            max-width: 500px;
            background: rgba(255, 255, 255, 0.03);
            border: 1px solid rgba(0, 242, 255, 0.2);
            padding: 15px;
            border-radius: 4px;
            position: relative;
            display: flex;
            align-items: center;
            gap: 15px;

            .step-icon {
                width: 40px;
                height: 40px;
                background: rgba(0, 242, 255, 0.1);
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                color: #00f2ff;
            }

            .step-content {
                flex: 1;

                .op-name {
                    color: #fff;
                    font-weight: bold;
                    margin-bottom: 4px;
                }

                .op-desc {
                    font-size: 12px;
                    color: #8fa3b8;
                }
            }

            .step-arrow {
                position: absolute;
                bottom: -25px;
                left: 50%;
                transform: translateX(-50%);
                color: rgba(0, 242, 255, 0.5);
                font-size: 20px;
            }
        }
    }
}

.add-step {
    width: 100%;
    max-width: 500px;
    height: 50px;
    border: 1px dashed rgba(0, 242, 255, 0.3);
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    color: #00f2ff;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
        background: rgba(0, 242, 255, 0.05);
        border-color: #00f2ff;
    }
}

.op-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;

    :deep(.el-radio) {
        margin: 0;
        width: 100%;
    }
}

.detail-dialog {
    :deep(.el-dialog__body) {
        padding: 20px 30px;
    }
}

.detail-layout {
    display: flex;
    gap: 30px;
}

.detail-form-section {
    flex: 1.2;
}

.detail-workflow-section {
    flex: 0.8;
    background: rgba(0, 0, 0, 0.2);
    padding: 20px;
    border-radius: 8px;
    border: 1px solid rgba(0, 242, 255, 0.1);
}

.section-title {
    color: #00f2ff;
    font-weight: bold;
    margin-bottom: 20px;
    font-size: 16px;
    padding-left: 10px;
    border-left: 3px solid #00f2ff;
}

.config-workflow.mini {
    padding: 0;
    background: transparent;

    .workflow-steps {
        gap: 15px;

        .step-item {
            padding: 10px;
            gap: 10px;

            .step-icon {
                width: 32px;
                height: 32px;
            }

            .op-name {
                font-size: 13px;
            }

            .step-arrow {
                bottom: -20px;
                font-size: 16px;
            }
        }
    }
}
</style>
