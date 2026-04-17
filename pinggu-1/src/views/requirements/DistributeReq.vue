<template>
    <div class="distribute-container">
        <div class="page-header">
            <h2 class="title">需求协同设计集汇与监控</h2>
            <!-- 结束协同操作 -->
            <div class="batch-actions mt-4 text-right">
                <el-button type="success" :disabled="!allCentersDone" class="cyber-btn-gold"
                    @click="proceedToCompleteness">
                    <el-icon>
                        <DataLine />
                    </el-icon>需求汇集 →
                </el-button>
            </div>
        </div>

        <!-- 协同流程步进条 -->
        <div class="workflow-stepper">
            <div class="ws-header">
                <el-icon class="ws-icon">
                    <Operation />
                </el-icon>
                <span class="ws-title">协同流程</span>
            </div>
            <div class="ws-content">
                <div v-for="(step, index) in workflowSteps" :key="index" class="ws-step" :class="{
                    'is-done': index < currentWorkflowStep,
                    'is-active': index === currentWorkflowStep,
                    'is-pending': index > currentWorkflowStep
                }">
                    <div class="ws-node">
                        <div class="ws-node-circle">
                            <el-icon v-if="index < currentWorkflowStep">
                                <Check />
                            </el-icon>
                            <el-icon v-else-if="index === currentWorkflowStep">
                                <Edit />
                            </el-icon>
                            <el-icon v-else-if="index === 2">
                                <Upload />
                            </el-icon>
                            <el-icon v-else>
                                <Box />
                            </el-icon>
                        </div>
                        <div class="ws-node-label">{{ step }}</div>
                    </div>
                    <div v-if="index < workflowSteps.length - 1" class="ws-connector">
                    </div>
                </div>
            </div>
        </div>

        <el-tabs v-model="activeTab" class="cyber-tabs">
            <!-- 面板1: 协同进度监控 -->
            <el-tab-pane label="分中心协同进度" name="progress">
                <div class="center-stats-grid">
                    <div v-for="center in subCenters" :key="center.id" class="stats-card" :class="center.status">
                        <div class="card-title">
                            <el-icon class="status-icon">
                                <OfficeBuilding v-if="center.status !== 'done'" />
                                <CircleCheck v-else />
                            </el-icon>
                            {{ center.subCenter }}
                        </div>
                        <div class="card-meta">
                            <div class="meta-item">
                                <span class="label">下发时间：</span>
                                <span class="val">{{ center.releaseTime }}</span>
                            </div>
                            <div class="meta-item">
                                <span class="label">当前状态：</span>
                                <span class="val status-tag">{{ center.statusLabel }}</span>
                            </div>
                            <div v-if="center.status === 'done'" class="meta-item">
                                <span class="label">上报时间：</span>
                                <span class="val">{{ center.finishTime }}</span>
                            </div>
                            <div v-if="center.status === 'rejected'" class="meta-item">
                                <span class="label">拒绝时间：</span>
                                <span class="val">{{ center.rejectTime }}</span>
                            </div>
                        </div>

                        <!-- 拒绝理由 (仅拒绝状态显示) -->
                        <div v-if="center.status === 'rejected'" class="reject-reason-box">
                            <div class="reason-title"><el-icon>
                                    <Warning />
                                </el-icon> 拒绝理由：</div>
                            <div class="reason-text">{{ center.refuseReason }}</div>
                        </div>

                        <!-- 拒绝操作 (仅拒绝状态显示) -->
                        <div v-if="center.status === 'rejected'" class="reject-actions">
                            <el-button type="warning" size="small" link @click="reEdit(center)">返回编辑</el-button>
                        </div>
                    </div>
                </div>
            </el-tab-pane>
        </el-tabs>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
    OfficeBuilding, CircleCheck, Check, DataLine, Warning,
    Operation, Edit, Upload, Box, Right
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRequirementCountersign, gatherCountersignData } from '@/api/requirements'

const router = useRouter()
const route = useRoute()
const activeTab = ref('progress')
const requirementId = ref(route.query.id)
onMounted(() => {
    getRequirementCountersign(requirementId.value).then(res => {
        console.log("res ", res)
        // subCenters.value = res.data
    })

})

const workflowSteps = ['设计下发', '分中心填写', '汇集整合']
const currentWorkflowStep = ref(1) // 0-indexed, 1 is '分中心填写'

/* 面板1: 分中心进度数据 */
const subCenters = ref([
    { id: 1, subCenter: '测控分中心', releaseTime: '2026-03-16 14:20', status: 'done', statusLabel: '已上报', percentage: 100, finishTime: '2026-03-16 16:45' },
    { id: 2, subCenter: '通信分中心', releaseTime: '2026-03-16 14:22', status: 'rejected', statusLabel: '已拒绝', percentage: 0, refuseReason: '缺少核心频率段指标信息，无法进行测控需求细化。', rejectTime: '2026-03-16 15:30' },
    { id: 3, subCenter: '装备分中心', releaseTime: '2026-03-16 14:25', status: 'processing', statusLabel: '正在填报', percentage: 75 }
])

const allCentersDone = computed(() => {
    return subCenters.value.every(c => c.status === 'done')
})

const reEdit = (center) => {
    ElMessage.info(`正在跳转至 ${center.name} 的编辑页面...`)
    router.push('/major/requirement-sys/generate')
}

const continueDistribute = (center) => {
    center.status = 'processing'
    center.statusLabel = '正在填报'
    center.percentage = 0
    delete center.refuseReason
    ElMessage.success(`已重新下发至 ${center.name}`)
}

const proceedToCompleteness = async () => {
    try {
        const { code, msg } = await gatherCountersignData(requirementId.value)
        if (code === 200) {
            ElMessage.success(msg)
            router.push('/major/requirement-sys/completeness')
        } else {
            ElMessage.error(msg)
        }
    } catch (error) {
        ElMessage.error('汇集失败，请稍后重试！')
    }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.distribute-container {
    padding: 24px;
    color: #e6edf3;
    animation: fadeIn 0.4s;
}

.center-stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 20px;
    padding: 10px 0;
}

.stats-card {
    background: rgba(13, 20, 31, 0.6);
    border: 1px solid rgba(0, 242, 255, 0.2);
    border-radius: 8px;
    padding: 20px;
    position: relative;
    overflow: hidden;
    transition: all 0.3s;

    &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        width: 4px;
        height: 100%;
        background: #2f81f7;
    }

    &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
        border-color: rgba(0, 242, 255, 0.4);
    }

    &.done {
        border-color: rgba(63, 185, 80, 0.3);

        &::before {
            background: #3fb950;
        }

        .status-tag {
            color: #3fb950;
            background: rgba(63, 185, 80, 0.1);
        }
    }

    &.processing {
        border-color: rgba(47, 129, 247, 0.3);

        &::before {
            background: #2f81f7;
        }

        .status-tag {
            color: #2f81f7;
            background: rgba(47, 129, 247, 0.1);
        }
    }

    &.rejected {
        border-color: rgba(248, 81, 73, 0.3);

        &::before {
            background: #f85149;
        }

        .status-tag {
            color: #f85149;
            background: rgba(248, 81, 73, 0.1);
        }

        .card-title {
            color: #f85149;
        }
    }

    .card-title {
        font-size: 16px;
        font-weight: bold;
        color: #fff;
        margin-bottom: 16px;
        display: flex;
        align-items: center;
        gap: 10px;

        .status-icon {
            font-size: 20px;
            color: inherit;
        }
    }

    .card-meta {
        margin-bottom: 20px;

        .meta-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 8px;
            font-size: 13px;

            .label {
                color: #8fa3b8;
            }

            .val {
                color: #e6edf3;
            }

            .status-tag {
                padding: 2px 8px;
                border-radius: 4px;
                font-size: 11px;
                font-weight: bold;
            }
        }
    }

    .reject-reason-box {
        background: rgba(248, 81, 73, 0.05);
        border: 1px dashed rgba(248, 81, 73, 0.2);
        border-radius: 4px;
        padding: 10px;
        margin-bottom: 15px;

        .reason-title {
            color: #f85149;
            font-size: 12px;
            font-weight: bold;
            display: flex;
            align-items: center;
            gap: 4px;
            margin-bottom: 4px;
        }

        .reason-text {
            color: #cbcaca;
            font-size: 12px;
            line-height: 1.4;
        }
    }

    .reject-actions {
        margin-top: 15px;
        display: flex;
        justify-content: flex-end;
        gap: 12px;
        border-top: 1px solid rgba(48, 54, 61, 0.4);
        padding-top: 12px;
    }
}

.cyber-btn-gold {
    background: rgba(210, 153, 34, 0.1);
    border: 1px solid #d29922;
    color: #d29922;
    font-weight: bold;
    height: 60px;
    position: absolute;
    right: 24px;
    top: 24px;
    box-shadow: 0 0 10px rgba(210, 153, 34, 0.2);

    &:hover:not(:disabled) {
        background: rgba(210, 153, 34, 0.2);
        box-shadow: 0 0 20px rgba(210, 153, 34, 0.4);
    }

    &:disabled {
        border-color: #30363d;
        color: #3b464e;
        background: rgb(198, 198, 198);
    }
}

.page-header {
    margin-bottom: 24px;

    .title {
        font-size: 24px;
        color: #fff;
        text-shadow: 0 0 10px rgba(0, 242, 255, 0.4);
        margin-bottom: 8px;
    }

    .subtitle {
        color: #8fa3b8;
        font-size: 14px;
        line-height: 1.5;
    }
}

/* ── Workflow Stepper ── */
.workflow-stepper {
    background: rgba(22, 27, 34, 0.4);
    border: 1px solid rgba(48, 54, 61, 0.5);
    border-radius: 12px;
    padding: 16px 24px;
    margin-bottom: 0;

    .ws-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 20px;
        color: #fff;

        .ws-icon {
            color: #58a6ff;
            font-size: 18px;
        }

        .ws-title {
            font-weight: 700;
            font-size: 15px;
            letter-spacing: 0.5px;
        }
    }

    .ws-content {
        display: flex;
        align-items: center;
        justify-content: space-around;
        padding: 10px 0;
    }

    .ws-step {
        display: flex;
        align-items: center;
        flex: 1;

        &:last-child {
            flex: none;
        }

        .ws-node {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 12px;
            z-index: 1;

            .ws-node-circle {
                width: 48px;
                height: 48px;
                border-radius: 50%;
                background: #0d1117;
                border: 2px solid #30363d;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 20px;
                color: #7d8590;
                transition: all 0.4s ease;
            }

            .ws-node-label {
                font-size: 13px;
                font-weight: 500;
                color: #7d8590;
                transition: color 0.4s;
            }
        }

        .ws-connector {
            flex: 1;
            height: 2px;
            background: #30363d;
            // margin: 0 -10px;
            transform: translateY(-18px);
            /* 居中对齐圆心 */
            display: flex;
            align-items: center;
            justify-content: center;
            position: relative;

            .el-icon {
                font-size: 12px;
                background: #0d1117;
                padding: 0 4px;
                color: inherit;
            }
        }

        // Status Modifiers
        &.is-done {
            .ws-node-circle {
                border-color: #3fb950;
                color: #3fb950;
                box-shadow: 0 0 10px rgba(63, 185, 80, 0.2);
            }

            .ws-node-label {
                color: #e6edf3;
            }

            .ws-connector {
                background: #3fb950;
                color: #3fb950;
                box-shadow: 0 0 5px rgba(63, 185, 80, 0.3);
            }
        }

        &.is-active {
            .ws-node-circle {
                width: 54px;
                height: 54px;
                border-color: #2f81f7;
                color: #fff;
                background: rgba(47, 129, 247, 0.1);
                box-shadow: 0 0 20px rgba(47, 129, 247, 0.5);
                font-size: 24px;
                // Animation for active step
                animation: pulse-glow 2s infinite;
            }

            .ws-node-label {
                color: #fff;
                font-weight: 700;
                text-shadow: 0 0 8px rgba(47, 129, 247, 0.6);
            }

            .ws-connector {
                background: linear-gradient(90deg, #2f81f7 0%, #30363d 100%);
            }
        }
    }
}

@keyframes pulse-glow {
    0% {
        box-shadow: 0 0 10px rgba(47, 129, 247, 0.4);
    }

    50% {
        box-shadow: 0 0 20px rgba(47, 129, 247, 0.7);
    }

    100% {
        box-shadow: 0 0 10px rgba(47, 129, 247, 0.4);
    }
}

.cyber-card {
    background: rgba(13, 20, 31, 0.6);
    border: 1px solid rgba(0, 242, 255, 0.2);
    border-radius: 6px;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .card-header {
        padding: 15px 20px;
        background: rgba(0, 242, 255, 0.05);
        border-bottom: 1px solid rgba(0, 242, 255, 0.1);
        color: #00f2ff;
        font-weight: bold;
        letter-spacing: 1px;

        &.flex-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 10px 20px;
        }
    }

    .card-body {
        padding: 15px;
        flex-grow: 1;
        overflow-y: auto;
    }
}

/* Tabs overriding */
:deep(.cyber-tabs) {
    .el-tabs__item {
        color: #8fa3b8;
        font-size: 16px;
        padding: 0 30px;
        height: 50px;
        line-height: 50px;

        &.is-active,
        &:hover {
            color: #00f2ff;
            text-shadow: 0 0 10px rgba(0, 242, 255, 0.4);
        }
    }

    .el-tabs__active-bar {
        background-color: #00f2ff;
        box-shadow: 0 0 10px #00f2ff;
    }

    .el-tabs__nav-wrap::after {
        background-color: rgba(48, 54, 61, 0.6);
    }
}

// Select overrides
:deep(.cyber-select) {
    width: 100%;

    .el-input__wrapper {
        background-color: rgba(6, 13, 23, 0.6);
        box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.3) inset;
    }

    .el-input__inner {
        color: #00f2ff;
    }
}

// Table overrides
:deep(.cyber-table) {
    background-color: transparent !important;
    --el-table-border-color: rgba(0, 242, 255, 0.1);
    --el-table-header-bg-color: rgba(6, 13, 23, 0.9);
    --el-table-header-text-color: #00f2ff;
    --el-table-row-hover-bg-color: rgba(0, 242, 255, 0.05);

    tr {
        background-color: transparent !important;
    }

    th.el-table__cell {
        font-weight: bold;
    }

    .el-table__inner-wrapper::before,
    .el-table__border-left-patch,
    .el-table__border-bottom-patch {
        background-color: transparent;
    }
}

// Buttons
.cyber-btn {
    background: rgba(0, 242, 255, 0.1);
    border: 1px solid #00f2ff;
    color: #00f2ff;
    box-shadow: 0 0 10px rgba(0, 242, 255, 0.2);

    &:hover,
    &:not(:disabled):focus {
        background: rgba(0, 242, 255, 0.2);
        box-shadow: 0 0 20px rgba(0, 242, 255, 0.4);
    }

    &.is-disabled {
        border-color: #30363d;
        color: #5c7080;
        box-shadow: none;
    }
}

.text-warning {
    color: #d29922;
    font-weight: bold;
    display: flex;
    align-items: center;
    gap: 5px;
}

.text-success {
    color: #3fb950;
    font-weight: bold;
    display: flex;
    align-items: center;
    gap: 5px;
    justify-content: center;
}

.text-right {
    text-align: right;
}

.mt-4 {
    margin-top: 20px;
}

@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

// Radio Button Overrides for Gather Section
:deep(.cyber-radio-small) {
    .el-radio-button__inner {
        background: rgba(13, 20, 31, 0.8);
        border-color: rgba(0, 242, 255, 0.3);
        color: #8fa3b8;
        padding: 6px 15px;
        font-size: 12px;

        &:hover {
            color: #00f2ff;
        }
    }

    .el-radio-button.is-active .el-radio-button__inner {
        background: rgba(0, 242, 255, 0.15);
        border-color: #00f2ff;
        color: #00f2ff;
        box-shadow: -1px 0 0 0 #00f2ff;
    }
}
</style>
