<template>
    <div class="subpage-container">
        <div class="page-header">
            <h3 class="page-title">体系评估态势构建与管理</h3>
            <div class="right-actions">
                <el-button type="primary" icon="Plus">新建评估态势</el-button>
            </div>
        </div>

        <el-row :gutter="20" class="situation-layout">
            <!-- Left: Situation List -->
            <el-col :span="6">
                <el-card class="tech-card list-card">
                    <template #header>
                        <div class="card-header">
                            <span>评估态势列表</span>
                            <el-button link type="primary" icon="Filter">过滤</el-button>
                        </div>
                    </template>

                    <div class="situation-list">
                        <div v-for="item in situations" :key="item.id" class="situation-item"
                            :class="{ 'is-active': activeSituation === item.id }" @click="selectSituation(item.id)">
                            <div class="item-header">
                                <span class="item-name">{{ item.name }}</span>
                                <el-tag size="small" :type="item.status === 'live' ? 'success' : 'info'" effect="plain">
                                    {{ item.status === 'live' ? '实时监控' : '静态归档' }}
                                </el-tag>
                            </div>
                            <div class="item-desc">{{ item.desc }}</div>
                            <div class="item-meta">
                                <span><el-icon>
                                        <Share />
                                    </el-icon> 节点: {{ item.nodes }}</span>
                                <span><el-icon>
                                        <Connection />
                                    </el-icon> 边: {{ item.edges }}</span>
                            </div>
                        </div>
                    </div>
                </el-card>
            </el-col>

            <!-- Right: Knowledge Graph -->
            <el-col :span="18">
                <el-card class="tech-card graph-card">
                    <template #header>
                        <div class="card-header">
                            <span>体系态势关联图谱展示</span>
                            <div class="graph-tools">
                                <el-radio-group v-model="layoutMode" size="small" class="tech-radio">
                                    <el-radio-button label="force">力导向</el-radio-button>
                                    <el-radio-button label="circular">环形</el-radio-button>
                                </el-radio-group>
                                <el-divider direction="vertical" />
                                <el-button size="small" icon="FullScreen">全屏</el-button>
                            </div>
                        </div>
                    </template>

                    <div ref="chartRef" class="echarts-container"></div>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Plus, Filter, Share, Connection, FullScreen } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const activeSituation = ref(1)
const layoutMode = ref('force')
const chartRef = ref(null)
let myChart = null

const situations = ref([
    { id: 1, name: '区域防空压制效能态势', status: 'live', desc: '红蓝双方体系防空与突防节点实时能力映射', nodes: 42, edges: 156 },
    { id: 2, name: '卫星通信受扰链路传导', status: 'live', desc: '受干扰节点级联失效影响分析', nodes: 18, edges: 32 },
    { id: 3, name: '2023 演习整体效能回放', status: 'archived', desc: '归档演习期间体系结构动态演变情况', nodes: 120, edges: 450 }
])

const generateGraphData = () => {
    // Generate mock graph data matching a military aerospace evaluation ontology
    const categories = [
        { name: '平台节点', itemStyle: { color: '#00f2ff' } },
        { name: '探测载荷', itemStyle: { color: '#67c23a' } },
        { name: '通信链路', itemStyle: { color: '#e6a23c' } },
        { name: '威胁源', itemStyle: { color: '#f56c6c' } }
    ]

    const nodes = []
    const links = []

    // Core nodes
    nodes.push({ id: '0', name: '预警卫星星座', category: 0, symbolSize: 40 })
    nodes.push({ id: '1', name: '地面指控中心', category: 0, symbolSize: 50 })
    nodes.push({ id: '2', name: '敌方干扰阵地', category: 3, symbolSize: 35 })

    // Generate peripheral nodes and links
    for (let i = 3; i < 30; i++) {
        const cat = i < 10 ? 1 : (i < 20 ? 2 : 0)
        nodes.push({
            id: i.toString(),
            name: `节点-${i}`,
            category: cat,
            symbolSize: Math.random() * 20 + 10
        })

        links.push({
            source: i.toString(),
            target: Math.floor(Math.random() * 3).toString(),
            value: Math.random().toFixed(2)
        })

        // Random interconnects
        if (Math.random() > 0.7) {
            links.push({
                source: i.toString(),
                target: Math.floor(Math.random() * i).toString()
            })
        }
    }

    return { nodes, links, categories }
}

const renderChart = () => {
    if (!chartRef.value) return
    if (!myChart) {
        myChart = echarts.init(chartRef.value)
    }

    const { nodes, links, categories } = generateGraphData()

    const option = {
        backgroundColor: 'transparent',
        tooltip: {
            formatter: '{b}'
        },
        legend: {
            data: categories.map(a => a.name),
            textStyle: { color: '#8fa3b8' },
            bottom: 10
        },
        series: [{
            type: 'graph',
            layout: layoutMode.value === 'force' ? 'force' : 'circular',
            data: nodes,
            links: links,
            categories: categories,
            roam: true,
            label: {
                show: true,
                position: 'right',
                color: '#fff',
                fontSize: 10
            },
            force: {
                repulsion: 200,
                edgeLength: 80,
                layoutAnimation: true
            },
            circular: {
                rotateLabel: true
            },
            lineStyle: {
                color: 'source',
                curveness: 0.2,
                opacity: 0.6
            },
            emphasis: {
                focus: 'adjacency',
                lineStyle: {
                    width: 3
                }
            }
        }]
    }

    myChart.setOption(option, true)
}

const selectSituation = (id) => {
    activeSituation.value = id
    // Re-render chart with new mock data to simulate switching
    renderChart()
}

watch(layoutMode, () => {
    renderChart()
})

const resizeHandler = () => {
    myChart && myChart.resize()
}

onMounted(() => {
    setTimeout(() => {
        renderChart()
    }, 100)
    window.addEventListener('resize', resizeHandler)
})

onUnmounted(() => {
    if (myChart) {
        myChart.dispose()
    }
    window.removeEventListener('resize', resizeHandler)
})
</script>

<style scoped lang="scss">
.subpage-container {
    padding: 24px;
}

.page-header {
    margin-bottom: 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;

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

.situation-layout {
    height: 100%;

    .el-col {
        height: 100%;
        display: flex;
        flex-direction: column;
    }
}

.list-card,
.graph-card {
    flex: 1;
    display: flex;
    flex-direction: column;

    :deep(.el-card__body) {
        flex: 1;
        padding: 0;
        display: flex;
        flex-direction: column;
        overflow: hidden;
    }
}

:deep(.tech-card .el-card__header) {
    border-bottom: 1px solid rgba(0, 242, 255, 0.1);

    .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        color: #fff;
        font-weight: bold;
    }
}

.graph-tools {
    display: flex;
    align-items: center;
    gap: 8px;
}

.situation-list {
    flex: 1;
    overflow-y: auto;
    padding: 10px;

    .situation-item {
        background: rgba(10, 21, 37, 0.8);
        border: 1px solid rgba(0, 242, 255, 0.1);
        border-radius: 4px;
        padding: 15px;
        margin-bottom: 10px;
        cursor: pointer;
        transition: all 0.3s;

        &:hover {
            border-color: rgba(0, 242, 255, 0.5);
        }

        &.is-active {
            border-color: #00f2ff;
            background: linear-gradient(135deg, rgba(0, 242, 255, 0.1) 0%, rgba(10, 21, 37, 0.8) 100%);
            box-shadow: 0 0 10px rgba(0, 242, 255, 0.1);
        }

        .item-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 8px;

            .item-name {
                color: #fff;
                font-weight: bold;
                font-size: 15px;
            }
        }

        .item-desc {
            color: #8fa3b8;
            font-size: 12px;
            margin-bottom: 12px;
            line-height: 1.4;
        }

        .item-meta {
            display: flex;
            gap: 15px;
            color: #606266;
            font-size: 12px;

            span {
                display: flex;
                align-items: center;
                gap: 4px;
            }
        }
    }
}

.echarts-container {
    flex: 1;
    width: 100%;
    height: 100%;
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

// Scrollbar
.situation-list::-webkit-scrollbar {
    width: 6px;
}

.situation-list::-webkit-scrollbar-thumb {
    background: rgba(0, 242, 255, 0.2);
    border-radius: 3px;
}
</style>
