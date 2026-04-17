<template>
    <div class="subpage-container">
        <div class="page-header">
            <h3 class="page-title">装备评估全寿期数据资源态势一览</h3>
            <div class="right-actions">
                <el-button type="primary" icon="Download">生成综合态势报告</el-button>
            </div>
        </div>

        <div class="dashboard-grid">
            <!-- Row 1: Overview & Push -->
            <el-row :gutter="20" class="mb-20">
                <el-col :span="16">
                    <el-card class="tech-card module-card">
                        <template #header>
                            <div class="card-header">态势概览模块</div>
                        </template>
                        <el-row>
                            <el-col :span="6" v-for="(stat, i) in coreStats" :key="i">
                                <div class="stat-box">
                                    <div class="stat-value">{{ stat.val }}</div>
                                    <div class="stat-label">{{ stat.label }}</div>
                                </div>
                            </el-col>
                        </el-row>
                        <div ref="trendChartRef" class="mini-chart"></div>
                    </el-card>
                </el-col>
                <el-col :span="8">
                    <el-card class="tech-card module-card highlight">
                        <template #header>
                            <div class="card-header">知识精准推送</div>
                        </template>
                        <div class="push-list">
                            <div v-for="(push, i) in pushItems" :key="i" class="push-item">
                                <span class="dot"></span>
                                <span class="text">{{ push }}</span>
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>

            <!-- Row 2: Spectrums -->
            <el-row :gutter="20" class="mb-20">
                <el-col :span="8">
                    <el-card class="tech-card module-card">
                        <template #header>
                            <div class="card-header">装备型谱分布</div>
                        </template>
                        <div ref="equipPieRef" class="chart-container"></div>
                    </el-card>
                </el-col>
                <el-col :span="8">
                    <el-card class="tech-card module-card">
                        <template #header>
                            <div class="card-header">任务型谱图</div>
                        </template>
                        <div ref="taskRadarRef" class="chart-container"></div>
                    </el-card>
                </el-col>
                <el-col :span="8">
                    <el-card class="tech-card module-card">
                        <template #header>
                            <div class="card-header">指标型谱分布</div>
                        </template>
                        <div ref="indicatorBarRef" class="chart-container"></div>
                    </el-card>
                </el-col>
            </el-row>

            <!-- Row 3: Data, Process, Result -->
            <el-row :gutter="20">
                <el-col :span="8">
                    <el-card class="tech-card module-card">
                        <template #header>
                            <div class="card-header">评估数据累积分布</div>
                        </template>
                        <div ref="dataAreaRef" class="chart-container"></div>
                    </el-card>
                </el-col>
                <el-col :span="8">
                    <el-card class="tech-card module-card process-module">
                        <template #header>
                            <div class="card-header">评估流程动态进度</div>
                        </template>
                        <el-steps direction="vertical" :active="2" class="tech-steps">
                            <el-step title="需求与本体映射" description="完成 100%" />
                            <el-step title="模型组装与训练" description="构建中 45%" />
                            <el-step title="数据推理与预测" description="计算池等待" />
                            <el-step title="链路归档输出" description="未开始" />
                        </el-steps>
                    </el-card>
                </el-col>
                <el-col :span="8">
                    <el-card class="tech-card module-card result-module">
                        <template #header>
                            <div class="card-header">最新评估结果池</div>
                        </template>
                        <div class="result-list">
                            <div class="result-item" v-for="(res, i) in latestResults" :key="i">
                                <span class="target">{{ res.target }}</span>
                                <span class="score" :class="{ 'high': res.score > 90 }">{{ res.score }} 分</span>
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

// DOM Refs
const trendChartRef = ref(null)
const equipPieRef = ref(null)
const taskRadarRef = ref(null)
const indicatorBarRef = ref(null)
const dataAreaRef = ref(null)

const charts = []

// Mock Data
const coreStats = [
    { label: '纳管型号总数', val: '124' },
    { label: '实测数据总量(TB)', val: '805.6' },
    { label: '评估模型数量', val: '56' },
    { label: '累计生成报告', val: '2,890' }
]

const pushItems = [
    '【高价值】检测到新型某雷达参数，已关联至目标型号。',
    '【图谱预警】平台控制节点置信度随时间衰减，请复核。',
    '【策略】自动匹配 14 条装备能力短板分析规则。',
    '【协同】态势感知组发起了新的联合评估链路。'
]

const latestResults = [
    { target: '通信子系统抗干扰效能', score: 92 },
    { target: '全寿命周期成本健康度', score: 78 },
    { target: '高轨变轨机动可靠性', score: 95 },
    { target: '太阳阵列输出退化预测', score: 85 }
]

const initCharts = () => {
    // 1. Overview Trend
    const trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
        grid: { top: 10, right: 10, bottom: 20, left: 30 },
        xAxis: { type: 'category', data: ['W1', 'W2', 'W3', 'W4', 'W5'], axisLabel: { color: '#8fa3b8' } },
        yAxis: { type: 'value', splitLine: { show: false }, axisLabel: { color: '#8fa3b8' } },
        series: [{ data: [120, 132, 101, 134, 190], type: 'line', smooth: true, itemStyle: { color: '#00f2ff' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(0,242,255,0.5)' }, { offset: 1, color: 'transparent' }]) } }]
    })
    charts.push(trendChart)

    // 2. Equipment Pie
    const equipPie = echarts.init(equipPieRef.value)
    equipPie.setOption({
        tooltip: { trigger: 'item' },
        series: [{
            type: 'pie', radius: ['40%', '70%'],
            data: [
                { value: 1048, name: '平台', itemStyle: { color: '#00f2ff' } },
                { value: 735, name: '有效载荷', itemStyle: { color: '#67c23a' } },
                { value: 580, name: '测控', itemStyle: { color: '#e6a23c' } },
                { value: 484, name: '运载', itemStyle: { color: '#f56c6c' } }
            ],
            label: { color: '#8fa3b8' }
        }]
    })
    charts.push(equipPie)

    // 3. Task Radar
    const taskRadar = echarts.init(taskRadarRef.value)
    taskRadar.setOption({
        radar: { indicator: [{ name: '环境试验', max: 100 }, { name: '系统联调', max: 100 }, { name: '发射靶试', max: 100 }, { name: '在轨测试', max: 100 }, { name: '延寿评估', max: 100 }], splitLine: { lineStyle: { color: 'rgba(0,242,255,0.2)' } }, axisName: { color: '#8fa3b8' } },
        series: [{ type: 'radar', data: [{ value: [90, 80, 85, 60, 40], name: '任务分布', itemStyle: { color: '#00f2ff' }, areaStyle: { color: 'rgba(0,242,255,0.3)' } }] }]
    })
    charts.push(taskRadar)

    // 4. Indicator Bar
    const indBar = echarts.init(indicatorBarRef.value)
    indBar.setOption({
        grid: { top: 20, right: 10, bottom: 20, left: 40 },
        xAxis: { type: 'value', splitLine: { show: false }, axisLabel: { color: '#8fa3b8' } },
        yAxis: { type: 'category', data: ['性能', '设计', '工艺', '服役'], axisLabel: { color: '#8fa3b8' } },
        series: [{ type: 'bar', data: [320, 210, 150, 430], itemStyle: { color: '#00f2ff' } }]
    })
    charts.push(indBar)

    // 5. Data Area
    const dataArea = echarts.init(dataAreaRef.value)
    dataArea.setOption({
        grid: { top: 20, right: 10, bottom: 20, left: 30 },
        xAxis: { type: 'category', boundaryGap: false, data: ['2019', '2020', '2021', '2022', '2023'], axisLabel: { color: '#8fa3b8' } },
        yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(0,242,255,0.1)' } }, axisLabel: { color: '#8fa3b8' } },
        series: [{ type: 'line', data: [100, 230, 450, 890, 1500], areaStyle: { color: '#67c23a' }, itemStyle: { color: '#67c23a' } }]
    })
    charts.push(dataArea)
}

const resizeHandler = () => charts.forEach(c => c.resize())

onMounted(() => {
    setTimeout(initCharts, 100)
    window.addEventListener('resize', resizeHandler)
})

onUnmounted(() => {
    charts.forEach(c => c.dispose())
    window.removeEventListener('resize', resizeHandler)
})
</script>

<style scoped lang="scss">
.subpage-container {
    padding: 24px;
    height: 100%;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
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

.mb-20 {
    margin-bottom: 20px;
}

.dashboard-grid {
    flex: 1;
    overflow-y: auto;
    overflow-x: hidden;
}

.module-card {
    background: rgba(16, 32, 53, 0.6) !important;
    border: 1px solid rgba(0, 242, 255, 0.1) !important;
    height: 100%;

    :deep(.el-card__header) {
        border-bottom: 1px solid rgba(0, 242, 255, 0.1);
        padding: 12px 20px;

        .card-header {
            color: #00f2ff;
            font-weight: bold;
            font-size: 15px;
            letter-spacing: 1px;
        }
    }

    &.highlight {
        border-color: rgba(0, 242, 255, 0.3) !important;
        background: linear-gradient(180deg, rgba(0, 242, 255, 0.05) 0%, rgba(16, 32, 53, 0.6) 100%) !important;
        box-shadow: 0 0 15px rgba(0, 242, 255, 0.1) inset;
    }
}

.stat-box {
    text-align: center;
    padding: 15px 0;

    .stat-value {
        color: #fff;
        font-size: 28px;
        font-weight: bold;
        font-family: 'Courier New', Courier, monospace;
        margin-bottom: 5px;
        text-shadow: 0 0 10px rgba(0, 242, 255, 0.5);
    }

    .stat-label {
        color: #8fa3b8;
        font-size: 12px;
    }
}

.mini-chart {
    height: 80px;
    width: 100%;
    margin-top: 10px;
}

.chart-container {
    height: 220px;
    width: 100%;
}

.push-list {
    height: 180px;
    overflow-y: auto;

    .push-item {
        margin-bottom: 12px;
        display: flex;
        align-items: flex-start;
        gap: 8px;

        .dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: #e6a23c;
            margin-top: 6px;
            box-shadow: 0 0 5px #e6a23c;
        }

        .text {
            flex: 1;
            color: #8fa3b8;
            font-size: 13px;
            line-height: 1.5;
        }
    }
}

.process-module {
    :deep(.tech-steps) {
        padding: 10px;
        height: 220px;

        .el-step__title {
            color: #fff;
            font-size: 14px;
        }

        .el-step__description {
            color: #8fa3b8;
            font-size: 12px;
        }

        .el-step__head.is-wait {
            color: #606266;
            border-color: #606266;
        }

        .el-step__title.is-wait {
            color: #606266;
        }

        .el-step__head.is-process {
            color: #00f2ff;
            border-color: #00f2ff;
        }

        .el-step__title.is-process {
            color: #00f2ff;
            font-weight: bold;
        }
    }
}

.result-module {
    .result-list {
        height: 220px;
        display: flex;
        flex-direction: column;
        gap: 12px;

        .result-item {
            background: rgba(0, 242, 255, 0.05);
            border: 1px solid rgba(0, 242, 255, 0.1);
            padding: 10px 15px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-radius: 4px;

            .target {
                color: #fff;
                font-size: 13px;
            }

            .score {
                font-family: monospace;
                font-size: 16px;
                color: #e6a23c;
                font-weight: bold;

                &.high {
                    color: #67c23a;
                }
            }
        }
    }
}

// Scrollbar formatting for dashboard
.dashboard-grid::-webkit-scrollbar {
    width: 8px;
}

.dashboard-grid::-webkit-scrollbar-thumb {
    background: rgba(0, 242, 255, 0.2);
    border-radius: 4px;
}
</style>
