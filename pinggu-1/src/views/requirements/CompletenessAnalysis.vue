<template>
    <div class="completeness-analysis-container">
        <div class="page-header">
            <h2 class="title">需求完备度分析</h2>
            <div class="subtitle">通过对指标、数据、方法及体系模型的四个维度进行雷达和细项检查，得出最终0~100分的科学评估得分与弱项优化建议。</div>
        </div>

        <!-- 顶部状态栏 -->
        <div class="analysis-status-bar">
            <div class="status-item">
                <span class="label">分析对象：</span>
                <span class="value">2026年通信对抗协同效能评估任务大纲</span>
            </div>
            <div class="status-item">
                <span class="label">当前评估版本：</span>
                <span class="value">v1.2_RELEASE</span>
            </div>
            <el-button type="primary" class="cyber-btn" @click="startAnalysis" :loading="isAnalyzing">
                <el-icon>
                    <DataAnalysis />
                </el-icon> {{ isAnalyzing ? '全维解析中...' : '重新执行分析' }}
            </el-button>
            <el-button type="success" plain @click="exportReport" :disabled="!hasAnalyzed">
                <el-icon>
                    <List />
                </el-icon> 数据采集及汇总
            </el-button>
        </div>

        <div class="dashboard-grid">
            <!-- 总览雷达图面板 -->
            <div class="cyber-card radar-panel">
                <div class="card-header">完备度综合评分模型</div>
                <div class="card-body radar-body">
                    <div class="center-score" :class="{ 'score-anim': isAnalyzing }">
                        <div class="score-value">{{ currentScore }}<span>分</span></div>
                        <div class="score-label">总完备度</div>
                    </div>

                    <div class="dimension top left"
                        :class="{ 'active': hasAnalyzed, 'highlight': dimensions.data < 80 }">
                        <div class="d-val">{{ isAnalyzing ? '--' : dimensions.data }}%</div>
                        <div class="d-name">数据支撑完备度</div>
                    </div>
                    <div class="dimension top right"
                        :class="{ 'active': hasAnalyzed, 'highlight': dimensions.indicator < 80 }">
                        <div class="d-val">{{ isAnalyzing ? '--' : dimensions.indicator }}%</div>
                        <div class="d-name">指标体系约束度</div>
                    </div>
                    <div class="dimension bottom left"
                        :class="{ 'active': hasAnalyzed, 'highlight': dimensions.method < 80 }">
                        <div class="d-val">{{ isAnalyzing ? '--' : dimensions.method }}%</div>
                        <div class="d-name">评估方法可用度</div>
                    </div>
                    <div class="dimension bottom right"
                        :class="{ 'active': hasAnalyzed, 'highlight': dimensions.model < 80 }">
                        <div class="d-val">{{ isAnalyzing ? '--' : dimensions.model }}%</div>
                        <div class="d-name">算子模型映射度</div>
                    </div>

                    <!-- Radar lines visual hack -->
                    <div class="radar-bg">
                        <div class="circle c1"></div>
                        <div class="circle c2"></div>
                        <div class="cross-line h"></div>
                        <div class="cross-line v"></div>
                        <div class="radar-scan" :class="{ 'active': isAnalyzing }"></div>
                    </div>
                </div>
            </div>

            <!-- 详细分析与建议建议 -->
            <div class="cyber-card details-panel">
                <div class="card-header">
                    智能推演与优化建议清单
                    <el-tag v-if="hasAnalyzed" type="warning" size="small" effect="dark" class="ml-auto">检测到 2
                        项需关注项</el-tag>
                </div>
                <div class="card-body p-0">
                    <div v-if="!hasAnalyzed && !isAnalyzing" class="empty-state">
                        <el-icon>
                            <Monitor />
                        </el-icon>
                        <p>等待初始化分析系统以载入详表...</p>
                    </div>

                    <div v-if="isAnalyzing" class="analyzing-state">
                        <div class="spinner"></div>
                        <p>数据交叉验证与溯源匹配中...</p>
                    </div>

                    <template v-if="hasAnalyzed">
                        <el-collapse v-model="activeCollapse" class="cyber-collapse">
                            <el-collapse-item name="1">
                                <template #title>
                                    <div class="collapse-title">
                                        <span class="icon-box danger"><el-icon>
                                                <Warning />
                                            </el-icon></span>
                                        <span>数据支撑盲区预警 (弱项)</span>
                                    </div>
                                </template>
                                <div class="suggestion-content">
                                    <strong>发现问题:</strong> <br>
                                    指标 '复杂电磁环境抗干扰度' 的计算所需字段 <span class="code">emc_background_noise_level</span>
                                    在各分中心上报的试验数据模板中均未被声明。
                                    <br><br>
                                    <strong>修复建议:</strong><br>
                                    需要补充下发采集任务单，要求【电磁环境测试分中心】在采集客户端预留相应数据采集通道或接口。
                                    <div class="mt-3">
                                        <el-button type="primary" size="small" plain><el-icon>
                                                <EditPen />
                                            </el-icon> 快捷发起任务补充单</el-button>
                                    </div>
                                </div>
                            </el-collapse-item>

                            <el-collapse-item name="2">
                                <template #title>
                                    <div class="collapse-title">
                                        <span class="icon-box warning"><el-icon>
                                                <Opportunity />
                                            </el-icon></span>
                                        <span>算子模型映射度不足 (次弱项)</span>
                                    </div>
                                </template>
                                <div class="suggestion-content">
                                    <strong>发现问题:</strong> <br>
                                    预定使用 '信噪比衰减估算模型' (M-2015)，该模型要求的输入数据采样率不低于 1MHz，但当前登记的硬件采集卡极限参数为
                                    500KHz，可能无法保证模型推演误差上限。
                                    <br><br>
                                    <strong>修复建议:</strong><br>
                                    1. 尝试在【模型仓】内降级选取容差更大的计算模型。<br>
                                    2. 更换前端硬件资源台账挂载项。
                                </div>
                            </el-collapse-item>

                            <el-collapse-item name="3">
                                <template #title>
                                    <div class="collapse-title">
                                        <span class="icon-box success"><el-icon>
                                                <CircleCheck />
                                            </el-icon></span>
                                        <span>指标与方法匹配验证通过</span>
                                    </div>
                                </template>
                                <div class="suggestion-content">
                                    检测通过，无逻辑断层与环形矛盾。方法计算有向无环图(DAG)解析完毕。
                                </div>
                            </el-collapse-item>
                        </el-collapse>
                    </template>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { DataAnalysis, List, Monitor, Warning, Opportunity, CircleCheck, EditPen } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

const isAnalyzing = ref(false)
const hasAnalyzed = ref(true) // 为了首屏展示效果，默认设为已经分析完一次
const activeCollapse = ref(['1', '2'])

const currentScore = ref(84)
const dimensions = reactive({
    data: 68,
    indicator: 95,
    method: 92,
    model: 81
})

const startAnalysis = () => {
    isAnalyzing.value = true
    hasAnalyzed.value = false
    currentScore.value = '--'

    setTimeout(() => {
        isAnalyzing.value = false
        hasAnalyzed.value = true
        currentScore.value = 84
        ElMessage.warning('分析完毕：存在阻碍实盘推演的逻辑缺失，请查阅修复建议！')
    }, 2500)
}

const exportReport = () => {
    ElMessage.success('数据采集及汇总流程已启动，正在跳转...')
    router.push('/process/reception-sys/overview')
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.completeness-analysis-container {
    padding: 24px;
    color: #e6edf3;
    animation: fadeIn 0.4s;
    height: 85vh;
    display: flex;
    flex-direction: column;
}

.page-header {
    margin-bottom: 20px;
    flex-shrink: 0;

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

.analysis-status-bar {
    display: flex;
    align-items: center;
    gap: 20px;
    background: rgba(13, 20, 31, 0.6);
    border: 1px solid rgba(0, 242, 255, 0.2);
    border-radius: 8px;
    padding: 15px 24px;
    margin-bottom: 24px;

    .status-item {
        margin-right: 20px;

        .label {
            color: #8fa3b8;
            font-size: 14px;
        }

        .value {
            color: #00f2ff;
            font-weight: bold;
        }
    }

    .cyber-btn {
        margin-left: auto;
    }
}

.dashboard-grid {
    display: flex;
    gap: 24px;
    flex-grow: 1;
    min-height: 0;
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
        display: flex;
        align-items: center;

        .ml-auto {
            margin-left: auto;
        }
    }

    .card-body {
        padding: 20px;
        flex-grow: 1;
        overflow-y: auto;

        &.p-0 {
            padding: 0;
        }
    }
}

/* 面板: 雷达图样式（CSS模拟） */
.radar-panel {
    width: 380px;
    flex-shrink: 0;

    .radar-body {
        position: relative;
        display: flex;
        justify-content: center;
        align-items: center;
        background: radial-gradient(circle at center, rgba(0, 242, 255, 0.05) 0%, transparent 60%);
    }

    .center-score {
        position: relative;
        z-index: 10;
        text-align: center;
        background: rgba(6, 13, 23, 0.9);
        border: 2px solid #00f2ff;
        border-radius: 50%;
        width: 120px;
        height: 120px;
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        box-shadow: 0 0 20px rgba(0, 242, 255, 0.3), inset 0 0 15px rgba(0, 242, 255, 0.2);

        &.score-anim {
            border-color: #bc8cff;
            box-shadow: 0 0 20px rgba(188, 140, 255, 0.5);
            animation: pulseBg 1s infinite alternate;
        }

        .score-value {
            font-size: 38px;
            font-weight: 900;
            color: #fff;
            text-shadow: 0 0 10px #00f2ff;
            line-height: 1;

            span {
                font-size: 16px;
                font-weight: normal;
                margin-left: 2px;
            }
        }

        .score-label {
            font-size: 12px;
            color: #00f2ff;
            margin-top: 5px;
            letter-spacing: 1px;
        }
    }

    .dimension {
        position: absolute;
        z-index: 5;
        text-align: center;
        transition: all 0.5s ease;
        opacity: 0.2;

        &.active {
            opacity: 1;
        }

        &.highlight .d-val {
            color: #f85149;
            text-shadow: 0 0 10px rgba(248, 81, 73, 0.6);
        }

        .d-val {
            font-size: 24px;
            font-weight: bold;
            color: #00f2ff;
        }

        .d-name {
            font-size: 12px;
            color: #8fa3b8;
            margin-top: 4px;
        }

        &.top {
            top: 40px;
        }

        &.bottom {
            bottom: 40px;
        }

        &.left {
            left: 30px;
        }

        &.right {
            right: 30px;
        }
    }

    .radar-bg {
        position: absolute;
        inset: 0;
        display: flex;
        justify-content: center;
        align-items: center;
        pointer-events: none;

        .circle {
            position: absolute;
            border-radius: 50%;
            border: 1px dashed rgba(0, 242, 255, 0.2);

            &.c1 {
                width: 220px;
                height: 220px;
            }

            &.c2 {
                width: 340px;
                height: 340px;
            }
        }

        .cross-line {
            position: absolute;
            background: rgba(0, 242, 255, 0.1);

            &.h {
                width: 100%;
                height: 1px;
            }

            &.v {
                height: 100%;
                width: 1px;
            }
        }

        .radar-scan {
            position: absolute;
            width: 170px;
            height: 170px;
            transform-origin: bottom right;
            top: 50%;
            left: 50%;
            margin-top: -170px;
            margin-left: -170px;
            background: conic-gradient(from 0deg, transparent 45deg, rgba(0, 242, 255, 0.05) 80deg, rgba(0, 242, 255, 0.2) 90deg, transparent 90deg);
            opacity: 0;

            &.active {
                opacity: 1;
                animation: scanRotate 2s linear infinite;
            }
        }
    }
}

@keyframes scanRotate {
    0% {
        transform: rotate(0deg);
    }

    100% {
        transform: rotate(360deg);
    }
}

@keyframes pulseBg {
    0% {
        background: rgba(6, 13, 23, 0.9);
    }

    100% {
        background: rgba(188, 140, 255, 0.15);
    }
}

/* 面板 右侧折叠区细节 */
.details-panel {
    flex-grow: 1;
}

.empty-state,
.analyzing-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: #5c7080;

    .el-icon {
        font-size: 64px;
        margin-bottom: 20px;
        opacity: 0.5;
    }
}

.spinner {
    width: 50px;
    height: 50px;
    border: 3px solid rgba(0, 242, 255, 0.1);
    border-top-color: #00f2ff;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin-bottom: 20px;
}

@keyframes spin {
    100% {
        transform: rotate(360deg);
    }
}


:deep(.cyber-collapse) {
    border-top: none;
    border-bottom: none;

    .el-collapse-item__header {
        background-color: transparent;
        color: #e6edf3;
        border-bottom: 1px solid rgba(0, 242, 255, 0.1);
        font-size: 15px;
        height: 60px;

        &.is-active {
            color: #fff;
            background-color: rgba(0, 242, 255, 0.03);
            border-bottom-color: transparent;
        }
    }

    .el-collapse-item__wrap {
        background-color: rgba(6, 13, 23, 0.4);
        border-bottom: 1px solid rgba(0, 242, 255, 0.1);
    }

    .el-collapse-item__content {
        color: #8fa3b8;
        padding: 20px;
        font-size: 14px;
        line-height: 1.6;
    }
}

.collapse-title {
    display: flex;
    align-items: center;
    padding-left: 20px;

    span:not(.icon-box) {
        font-weight: bold;
        letter-spacing: 0.5px;
    }
}

.icon-box {
    display: inline-flex;
    justify-content: center;
    align-items: center;
    width: 30px;
    height: 30px;
    border-radius: 4px;
    margin-right: 15px;
    color: #fff;
    font-size: 16px;

    &.danger {
        background: rgba(248, 81, 73, 0.2);
        border: 1px solid #f85149;
        color: #f85149;
        box-shadow: 0 0 10px rgba(248, 81, 73, 0.3);
    }

    &.warning {
        background: rgba(210, 153, 34, 0.2);
        border: 1px solid #d29922;
        color: #d29922;
        box-shadow: 0 0 10px rgba(210, 153, 34, 0.3);
    }

    &.success {
        background: rgba(63, 185, 80, 0.2);
        border: 1px solid #3fb950;
        color: #3fb950;
        box-shadow: 0 0 10px rgba(63, 185, 80, 0.3);
    }
}

.suggestion-content {
    .code {
        font-family: monospace;
        background: rgba(0, 0, 0, 0.5);
        padding: 2px 6px;
        border-radius: 4px;
        color: #d2aaff;
    }

    strong {
        color: #e6edf3;
    }

    .mt-3 {
        margin-top: 15px;
    }
}

.cyber-btn {
    background: rgba(0, 242, 255, 0.1);
    border: 1px solid #00f2ff;
    color: #00f2ff;

    &:hover,
    &:focus {
        background: rgba(0, 242, 255, 0.2);
        box-shadow: 0 0 15px rgba(0, 242, 255, 0.4);
    }
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
</style>
