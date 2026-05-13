<template>
    <div class="subpage-container project-detail-page">
        <!-- 顶部返回和基础信息 -->
        <div class="detail-header">
            <el-button class="back-btn" @click="goBack" icon="ArrowLeft">返回列表</el-button>
            <div class="project-title-area">
                <h2 class="project-title">{{ projectInfo.name }}</h2>
                <el-tag :type="getStatusType(projectInfo.status)" effect="dark" class="status-badge">{{
                    projectInfo.status }}</el-tag>
                <el-tag effect="plain" class="type-badge">{{ projectInfo.type }}</el-tag>
            </div>

            <el-row class="basic-info-row">
                <el-col :span="4">
                    <div class="info-item">
                        <span class="label">工程编号:</span>
                        <span class="value sys-font">{{ projectInfo.id }}</span>
                    </div>
                </el-col>
                <el-col :span="6">
                    <div class="info-item">
                        <span class="label">关联需求:</span>
                        <span class="value">{{ projectInfo.requirement }}</span>
                    </div>
                </el-col>
                <el-col :span="6">
                    <div class="info-item">
                        <span class="label">分配任务:</span>
                        <el-tooltip :content="projectInfo.tasks.join(', ')" placement="bottom">
                            <span class="value">{{ projectInfo.tasks.length }} 项评估任务</span>
                        </el-tooltip>
                    </div>
                </el-col>
                <el-col :span="8">
                    <div class="info-item">
                        <span class="label">整体进度:</span>
                        <div class="progress-wrap">
                            <el-progress :percentage="65" :color="customColors" class="tech-progress-mini" />
                        </div>
                    </div>
                </el-col>
            </el-row>
        </div>

        <!-- 链式评估流程节点 -->
        <el-card class="tech-card process-container">
            <template #header>
                <div class="card-title">
                    <el-icon>
                        <Connection />
                    </el-icon> 链式评估标准流程
                </div>
            </template>

            <div class="steps-wrapper">
                <el-steps :active="activeStep" finish-status="success" class="cyber-steps">
                    <el-step title="需求设计" icon="Document">
                        <template #description>已完成关联评估需求导入与解析</template>
                    </el-step>
                    <el-step title="数据采集汇总" icon="DataBoard">
                        <template #description>当前进行中，数据源对接接入</template>
                    </el-step>
                    <el-step title="综合分析计算" icon="Cpu">
                        <template #description>等待数据采集完成，启动模型</template>
                    </el-step>
                    <el-step title="结果分析与溯源" icon="DataAnalysis">
                        <template #description>生成测试报告，知识图谱关联</template>
                    </el-step>
                </el-steps>
            </div>

            <el-divider class="cyber-divider" />

            <!-- 动态面板：不同阶段显示不同的子任务或图表 -->
            <div class="step-content-panel">
                <!-- 第一步：需求设计详情 -->
                <div v-if="activeStep === 0" class="step-content step1">
                    <h3 class="panel-header">需求解析详情区</h3>
                    <el-descriptions column="2" border class="tech-descriptions">
                        <el-descriptions-item label="需求背景">该工程旨在... </el-descriptions-item>
                        <el-descriptions-item label="指标体系">已关联 1 个指标体系模型</el-descriptions-item>
                    </el-descriptions>
                </div>

                <!-- 第二步：数据采集汇总 (复杂步骤) -->
                <div v-if="activeStep === 1" class="step-content step2">
                    <h3 class="panel-header">数据采集与整编任务</h3>
                    <div class="sub-steps-container">
                        <el-steps :active="subStepIndex" finish-status="success" simple class="cyber-sub-steps">
                            <el-step title="表单生成与下发" icon="Edit" />
                            <el-step title="数据汇总整编" icon="FolderOpened" />
                            <el-step title="数据预处理" icon="Filter" />
                        </el-steps>
                        
                        <div class="sub-step-content-area">
                            <div v-if="subStepIndex === 0" class="sub-step-panel">
                                <el-table :data="formTasks" class="tech-table mini-table" size="small">
                                    <el-table-column prop="name" label="表单名称" />
                                    <el-table-column prop="target" label="下发对象" />
                                    <el-table-column prop="status" label="状态">
                                        <template #default="{ row }">
                                            <el-tag :type="row.status === '已填报' ? 'success' : 'warning'" size="small">{{
                                                row.status }}</el-tag>
                                        </template>
                                    </el-table-column>
                                </el-table>
                            </div>

                            <div v-if="subStepIndex === 1" class="sub-step-panel">
                                <div class="data-streams">
                                    模拟数据源接入展示，Kafka流处理状态：<span style="color:#00ff9d">Running (45.2 MB/s)</span>
                                </div>
                            </div>

                            <div v-if="subStepIndex === 2" class="sub-step-panel">
                                <div class="operator-flow">算子执行流节点图（集成已有算子管理组件）</div>
                            </div>
                        </div>

                        <div class="sub-step-actions">
                            <el-button size="small" @click="prevSubStep" :disabled="subStepIndex === 0">子环节上一步</el-button>
                            <el-button size="small" type="primary" @click="nextSubStep" v-if="subStepIndex < 2">子环节下一步</el-button>
                            <span v-if="subStepIndex === 2" style="font-size: 12px; color: #00ff9d; margin-left: 10px; line-height: 24px;">可进行主流程下一步</span>
                        </div>
                    </div>
                </div>

                <!-- 第三步：综合分析计算 -->
                <div v-if="activeStep === 2" class="step-content step3">
                    <h3 class="panel-header">算法计算调度中心</h3>
                    <el-button type="primary" class="glow-btn">立即触发计算实例</el-button>
                </div>

                <!-- 第四步：结果分析与溯源 -->
                <div v-if="activeStep === 3" class="step-content step4">
                    <h3 class="panel-header">评估结论图谱与报告</h3>
                    <div class="placeholder-report">自动生成的评估报告展示区</div>
                </div>

                <!-- 流程控制按钮 -->
                <div class="step-actions">
                    <el-button @click="prevStep" :disabled="activeStep === 0">上一步</el-button>
                    <el-button type="primary" @click="nextStep" v-if="activeStep < 3">完成当前阶段并继续</el-button>
                    <el-button type="success" @click="finishProject" v-if="activeStep === 3"
                        class="glow-btn-success">结束工程闭环</el-button>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Document, DataBoard, Cpu, DataAnalysis, Connection, ArrowLeft, Edit, FolderOpened, Filter } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

// 模拟获取工程详情
const projectId = route.params.id || 'PROJ-2401'
const projectInfo = ref({
    id: projectId,
    name: '2024天相一号评估工程',
    type: '链式评估',
    requirement: '卫星通信载荷效能评估 (REQ-01)',
    status: '进行中',
    tasks: ['卫星通信网络效能评估任务', '星间链路抗毁性评估任务']
})

// 环节步骤
const activeStep = ref(1) // 假设当前进行到第二步数据采集
const subStepIndex = ref(0) // 数据采集的子步骤索引 (0:表单, 1:汇总, 2:预处理)

// 进度条颜色
const customColors = [
    { color: '#ff3c5b', percentage: 20 },
    { color: '#ffbd2e', percentage: 60 },
    { color: '#00f2ff', percentage: 80 },
    { color: '#00ff9d', percentage: 100 },
]

// 模拟表单下发任务数据
const formTasks = ref([
    { name: '载荷基带参数采集表', target: '测控站A', status: '已填报' },
    { name: '天线增益实测数据录入', target: '测控站B', status: '填报中' },
])

const goBack = () => {
    router.push('/project/index')
}

const getStatusType = (status) => {
    if (status === '已完成') return 'success'
    if (status === '进行中') return 'primary'
    return 'info'
}

const nextStep = () => {
    if (activeStep.value === 1 && subStepIndex.value < 2) {
        ElMessage.warning('请先完成所有数据采集与整编的子环节再进入下一主阶段')
        return
    }
    if (activeStep.value < 3) {
        activeStep.value++
        ElMessage.success(`已进入下个阶段，调度节点资源`)
    }
}

const prevStep = () => {
    if (activeStep.value > 0) activeStep.value--
}

const nextSubStep = () => {
    if (subStepIndex.value < 2) subStepIndex.value++
}

const prevSubStep = () => {
    if (subStepIndex.value > 0) subStepIndex.value--
}

const finishProject = () => {
    projectInfo.value.status = '已完成'
    ElMessage.success('工程圆满结束，评估数据已固化至知识库。')
}
</script>

<style scoped lang="scss">
@use '@/assets/styles/v2/variables.scss' as *;

.project-detail-page {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

/* 顶部信息区 */
.detail-header {
    background: rgba(16, 33, 56, 0.7);
    border: 1px solid rgba(0, 242, 255, 0.2);
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.4), inset 0 0 20px rgba(0, 242, 255, 0.05);
    border-radius: 6px;
    padding: 20px 30px;
    position: relative;

    .back-btn {
        margin-bottom: 15px;
        background: transparent;
        border-color: rgba(255, 255, 255, 0.2);
        color: #e0eafc;

        &:hover {
            color: $primary-color;
            border-color: $primary-color;
            background: rgba(0, 242, 255, 0.1);
        }
    }
}

.project-title-area {
    display: flex;
    align-items: center;
    gap: 15px;
    margin-bottom: 20px;

    .project-title {
        margin: 0;
        font-size: 26px;
        color: #fff;
        text-shadow: 0 0 10px rgba(0, 242, 255, 0.4);
    }
}

.basic-info-row {
    background: rgba(0, 0, 0, 0.3);
    padding: 15px;
    border-radius: 4px;

    .info-item {
        display: flex;
        align-items: center;
        font-size: 14px;

        .label {
            color: $text-color-secondary;
            margin-right: 10px;
            white-space: nowrap;
        }

        .value {
            color: #fff;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .sys-font {
            font-family: 'Roboto Mono', monospace;
            color: $primary-color;
        }
    }
}

.progress-wrap {
    flex: 1;
    min-width: 120px;

    .tech-progress-mini {
        :deep(.el-progress-bar__outer) {
            background-color: rgba(255, 255, 255, 0.1);
        }

        :deep(.el-progress__text) {
            color: #fff;
            font-family: 'Roboto Mono', monospace;
        }
    }
}

/* 流程区 */
.process-container {
    flex: 1;
    display: flex;
    flex-direction: column;

    .card-title {
        color: $primary-color;
        font-weight: bold;
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 16px;
        letter-spacing: 1px;
    }
}

.steps-wrapper {
    padding: 20px 40px;

    .cyber-steps {
        :deep(.el-step__title) {
            color: $text-color-secondary;
            font-weight: bold;

            &.is-process {
                color: $primary-color;
                text-shadow: 0 0 8px rgba(0, 242, 255, 0.6);
            }

            &.is-success {
                color: $success-color;
            }
        }

        :deep(.el-step__description) {
            color: rgba(255, 255, 255, 0.4);

            &.is-process {
                color: #fff;
            }
        }

        :deep(.el-step__head) {
            &.is-process {
                color: $primary-color;
                border-color: $primary-color;

                .el-step__icon {
                    background: rgba(0, 242, 255, 0.1);
                    box-shadow: 0 0 10px rgba(0, 242, 255, 0.5);
                }
            }

            &.is-success {
                color: $success-color;
                border-color: $success-color;
            }
        }

        :deep(.el-step__line) {
            background-color: rgba(0, 242, 255, 0.2);
        }
    }
}

.cyber-divider {
    border-color: rgba(0, 242, 255, 0.1);
    margin: 10px 0 20px 0;
}

.step-content-panel {
    background: rgba(10, 20, 35, 0.5);
    border: 1px dashed rgba(0, 242, 255, 0.2);
    border-radius: 4px;
    padding: 20px;
    min-height: 250px;
    display: flex;
    flex-direction: column;

    .panel-header {
        margin-top: 0;
        margin-bottom: 20px;
        color: #fff;
        border-left: 3px solid $primary-color;
        padding-left: 10px;
        font-size: 16px;
    }

    .step-content {
        flex: 1;
    }
}

/* 第三方组件覆盖 */
.tech-descriptions {
    :deep(.el-descriptions__body) {
        background-color: transparent;
    }

    :deep(.el-descriptions__label) {
        background-color: rgba(0, 242, 255, 0.05) !important;
        color: $text-color-secondary;
        border-color: rgba(0, 242, 255, 0.1) !important;
    }

    :deep(.el-descriptions__content) {
        background-color: transparent !important;
        color: #fff;
        border-color: rgba(0, 242, 255, 0.1) !important;
    }
}

.cyber-sub-steps {
    background: transparent !important;
    padding: 10px 0 !important;
    border-bottom: 1px dashed rgba(255, 255, 255, 0.1);
    
    :deep(.el-step__title) {
        font-size: 13px;
        color: $text-color-secondary;
        &.is-process { color: $primary-color; }
        &.is-success { color: $success-color; }
    }
    :deep(.el-step__head) {
        &.is-process { color: $primary-color; }
        &.is-success { color: $success-color; }
    }
}

.sub-step-content-area {
    padding: 15px 0;
    min-height: 120px;
}

.sub-step-actions {
    display: flex;
    justify-content: flex-start;
    gap: 10px;
    margin-top: 10px;
}

.mini-table {
    margin-top: 5px;
    border: 1px solid rgba(0, 242, 255, 0.1);
}

.data-streams,
.operator-flow,
.placeholder-report {
    padding: 30px;
    background: rgba(0, 0, 0, 0.2);
    border-radius: 4px;
    text-align: center;
    color: rgba(255, 255, 255, 0.5);
    font-family: 'Roboto Mono', monospace;
    margin-top: 15px;
}

.glow-btn {
    box-shadow: 0 0 15px rgba(0, 242, 255, 0.4);
    animation: btnPulse 2s infinite;
}

.glow-btn-success {
    box-shadow: 0 0 15px rgba(0, 255, 157, 0.4);
}

@keyframes btnPulse {

    0%,
    100% {
        box-shadow: 0 0 15px rgba(0, 242, 255, 0.4);
    }

    50% {
        box-shadow: 0 0 25px rgba(0, 242, 255, 0.7);
    }
}

.step-actions {
    margin-top: 30px;
    display: flex;
    justify-content: flex-end;
    gap: 15px;
    padding-top: 20px;
    border-top: 1px solid rgba(255, 255, 255, 0.05);
}
</style>
