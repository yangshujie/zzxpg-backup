<template>
    <div class="distribute-container">
        <div class="page-header">
            <h2 class="title">需求协同设计集汇与监控</h2>
            <!-- 结束协同操作 -->
            <div class="batch-actions mt-4 text-right">
                <el-button type="success" class="cyber-btn-gold" @click="proceedToCompleteness">
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
    { id: 1, subCenter: 'XX分中心', releaseTime: '2026-03-16 14:20', status: 'done', statusLabel: '已上报', percentage: 100, finishTime: '2026-03-16 16:45' },
    { id: 2, subCenter: 'YY分中心', releaseTime: '2026-03-16 14:25', status: 'processing', statusLabel: '正在填报', percentage: 75 }
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
            router.push({
                path: '/major/requirement-sys/complete',
                query: {
                    id: requirementId.value
                }
            })
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
    color: var(--text-color-primary);
    animation: fadeIn 0.4s;
    background: var(--bg-color); 
}

.center-stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 20px;
    padding: 10px 0;
}

.stats-card {
    background: var(--card-bg-color);
    border: 1px solid var(--border-color)
;
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
        background: var(--info-color);
    }

    &:hover {
        transform: translateY(-4px);
        box-shadow: var(--box-shadow-base);
        border-color: var(--border-color-hover);
    }

    &.done {
        border-color: color-mix(in srgb, var(--success-color) 30%, transparent);

        &::before {
            background: var(--success-color);
        }

        .status-tag {
            color: var(--success-color);
            background: color-mix(in srgb, var(--success-color) 10%, transparent);
        }
    }

    &.processing {
        border-color: color-mix(in srgb, var(--info-color) 30%, transparent);

        &::before {
            background: var(--info-color);
        }

        .status-tag {
            color: var(--info-color);
            background: color-mix(in srgb, var(--info-color) 10%, transparent);
        }
    }

    &.rejected {
        border-color: color-mix(in srgb, var(--danger-color) 30%, transparent);

        &::before {
            background: var(--danger-color);
        }

        .status-tag {
            color: var(--danger-color);
            background: color-mix(in srgb, var(--danger-color) 10%, transparent);
        }

        .card-title {
            color: var(--danger-color);
        }
    }

    .card-title {
        font-size: 16px;
        font-weight: bold;
        color: var(--text-color-primary);
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
                // [MODIFIED] Use secondary text
                color: var(--text-color-secondary);
            }

            .val {
                color: var(--text-color-primary);
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
        // [MODIFIED] Use danger color mix
        background: color-mix(in srgb, var(--danger-color) 5%, transparent);
        border: 1px dashed color-mix(in srgb, var(--danger-color) 20%, transparent);
        border-radius: 4px;
        padding: 10px;
        margin-bottom: 15px;

        .reason-title {
            color: var(--danger-color);
            font-size: 12px;
            font-weight: bold;
            display: flex;
            align-items: center;
            gap: 4px;
            margin-bottom: 4px;
        }

        .reason-text {
            // [MODIFIED] Use secondary text
            color: var(--text-color-secondary);
            font-size: 12px;
            line-height: 1.4;
        }
    }

    .reject-actions {
        margin-top: 15px;
        display: flex;
        justify-content: flex-end;
        gap: 12px;
        // [MODIFIED] Use border color
        border-top: 1px solid var(--border-color);
        padding-top: 12px;
    }
}
.cyber-btn-gold {
    background: color-mix(in srgb, var(--warning-color) 10%, transparent);
    border: 1px solid var(--warning-color);
    color: var(--warning-color);
    font-weight: bold;
    height: 60px;
    position: absolute;
    right: 24px;
    top: 24px;
    box-shadow: 0 0 10px color-mix(in srgb, var(--warning-color) 20%, transparent);

    &:hover:not(:disabled) {
        background: color-mix(in srgb, var(--warning-color) 20%, transparent);
        box-shadow: 0 0 20px color-mix(in srgb, var(--warning-color) 40%, transparent);
    }

    &:disabled {
        border-color: var(--border-color);
        color: var(--text-color-secondary);
        opacity: 0.5;
        background: var(--card-bg-color);
    }
}

.page-header {
    margin-bottom: 24px;

    .title {
        font-size: 24px;
        color: var(--text-color-primary);
        // [MODIFIED] Use primary color for glow
        text-shadow: 0 0 10px color-mix(in srgb, var(--primary-color) 40%, transparent);
        margin-bottom: 8px;
    }

    .subtitle {
        color: var(--text-color-secondary);
        font-size: 14px;
        line-height: 1.5;
    }
}

/* ── Workflow Stepper ── */
.workflow-stepper {
    background: var(--card-bg-color);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    padding: 16px 24px;
    margin-bottom: 0;

    .ws-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 20px;
        color: var(--text-color-primary);

        .ws-icon {
            color: var(--info-color);
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
                // [MODIFIED] Use bg color
                background: var(--bg-color);
                border: 2px solid var(--border-color);
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 20px;
                color: var(--text-color-secondary);
                transition: all 0.4s ease;
            }

            .ws-node-label {
                font-size: 13px;
                font-weight: 500;
                color: var(--text-color-secondary);
                transition: color 0.4s;
            }
        }

        .ws-connector {
            flex: 1;
            height: 2px;
            background: var(--border-color);
            transform: translateY(-18px);
            display: flex;
            align-items: center;
            justify-content: center;
            position: relative;

            .el-icon {
                font-size: 12px;
                background: var(--bg-color);
                padding: 0 4px;
                color: inherit;
            }
        }

        // Status Modifiers
        &.is-done {
            .ws-node-circle {
                border-color: var(--success-color);
                color: var(--success-color);
                box-shadow: 0 0 10px color-mix(in srgb, var(--success-color) 20%, transparent);
            }

            .ws-node-label {
                color: var(--text-color-primary);
            }

            .ws-connector {
                background: var(--success-color);
                color: var(--success-color);
                box-shadow: 0 0 5px color-mix(in srgb, var(--success-color) 30%, transparent);
            }
        }

        &.is-active {
            .ws-node-circle {
                width: 54px;
                height: 54px;
                border-color: var(--info-color);
                color: var(--text-color-primary);
                background: color-mix(in srgb, var(--info-color) 10%, transparent);
                box-shadow: 0 0 20px color-mix(in srgb, var(--info-color) 50%, transparent);
                font-size: 24px;
                animation: pulse-glow 2s infinite;
            }

            .ws-node-label {
                color: var(--text-color-primary);
                font-weight: 700;
                text-shadow: 0 0 8px color-mix(in srgb, var(--info-color) 60%, transparent);
            }

            .ws-connector {
                background: linear-gradient(90deg, var(--info-color) 0%, var(--border-color) 100%);
            }
        }
    }
}

@keyframes pulse-glow {
    0% { box-shadow: 0 0 10px color-mix(in srgb, var(--info-color) 40%, transparent); }
    50% { box-shadow: 0 0 20px color-mix(in srgb, var(--info-color) 70%, transparent); }
    100% { box-shadow: 0 0 10px color-mix(in srgb, var(--info-color) 40%, transparent); }
}

.cyber-card {
    background: var(--card-bg-color);
    border: 1px solid color-mix(in srgb, var(--primary-color) 20%, transparent);
    border-radius: 6px;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .card-header {
        padding: 15px 20px;
        background: color-mix(in srgb, var(--primary-color) 5%, transparent);
        border-bottom: 1px solid color-mix(in srgb, var(--primary-color) 10%, transparent);
        color: var(--primary-color);
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
        color: var(--text-color-secondary);
        font-size: 16px;
        padding: 0 30px;
        height: 50px;
        line-height: 50px;

        &.is-active,
        &:hover {
            color: var(--primary-color);
            text-shadow: 0 0 10px color-mix(in srgb, var(--primary-color) 40%, transparent);
        }
    }

    .el-tabs__active-bar {
        background-color: var(--primary-color);
        box-shadow: 0 0 10px var(--primary-color);
    }

    .el-tabs__nav-wrap::after {
        background-color: var(--border-color);
    }
}

// Select overrides
:deep(.cyber-select) {
    width: 100%;

    .el-input__wrapper {
        background-color: var(--card-bg-color);
        box-shadow: 0 0 0 1px color-mix(in srgb, var(--primary-color) 30%, transparent) inset;
    }

    .el-input__inner {
        color: var(--primary-color);
    }
}

// Table overrides
:deep(.cyber-table) {
    background-color: transparent !important;
    --el-table-border-color: color-mix(in srgb, var(--primary-color) 10%, transparent);
    --el-table-header-bg-color: var(--card-bg-color);
    --el-table-header-text-color: var(--primary-color);
    --el-table-row-hover-bg-color: color-mix(in srgb, var(--primary-color) 5%, transparent);
    --el-table-text-color: var(--text-color-primary);

    tr {
        background-color: transparent !important;
    }

    th.el-table__cell {
        font-weight: bold;
        border-bottom: 2px solid var(--border-color) !important;
    }

    td.el-table__cell {
        border-bottom: 1px solid var(--border-color) !important;
    }

    .el-table__inner-wrapper::before,
    .el-table__border-left-patch,
    .el-table__border-bottom-patch {
        background-color: transparent;
    }
}

// Buttons
.cyber-btn {
    background: color-mix(in srgb, var(--primary-color) 10%, transparent);
    border: 1px solid var(--primary-color);
    color: var(--primary-color);
    box-shadow: 0 0 10px color-mix(in srgb, var(--primary-color) 20%, transparent);

    &:hover,
    &:not(:disabled):focus {
        background: color-mix(in srgb, var(--primary-color) 20%, transparent);
        box-shadow: 0 0 20px color-mix(in srgb, var(--primary-color) 40%, transparent);
    }

    &.is-disabled {
        border-color: var(--border-color);
        color: var(--text-color-secondary);
        opacity: 0.5;
        box-shadow: none;
    }
}

.text-warning {
    color: var(--warning-color);
    font-weight: bold;
    display: flex;
    align-items: center;
    gap: 5px;
}

.text-success {
    color: var(--success-color);
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

:deep(.cyber-radio-small) {
    .el-radio-button__inner {
        background: var(--card-bg-color);
        border-color: color-mix(in srgb, var(--primary-color) 30%, transparent);
        color: var(--text-color-secondary);
        padding: 6px 15px;
        font-size: 12px;

        &:hover {
            color: var(--primary-color);
        }
    }

    .el-radio-button.is-active .el-radio-button__inner {
        background: color-mix(in srgb, var(--primary-color) 15%, transparent);
        border-color: var(--primary-color);
        color: var(--primary-color);
        box-shadow: -1px 0 0 0 var(--primary-color);
    }
}
</style>
