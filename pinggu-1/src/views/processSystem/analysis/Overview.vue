<template>
    <div class="subpage-container">
        <div class="page-header">
            <h3 class="page-title">概览</h3>
        </div>

        <!-- Stats Section -->
        <div class="overview-stats">
            <el-row :gutter="20">
                <el-col :span="8" v-for="item in summaryStats" :key="item.label">
                    <div class="summary-card">
                        <div class="card-left">
                            <el-icon :size="40" :style="{ color: item.color }">
                                <component :is="item.icon" />
                            </el-icon>
                        </div>
                        <div class="card-right">
                            <div class="label">{{ item.label }}</div>
                            <div class="value">{{ item.value }}</div>
                        </div>
                    </div>
                </el-col>
            </el-row>
        </div>

        <div class="content-grid">
            <!-- Left: Running Tasks -->
            <div class="main-column">
                <el-card class="tech-card">
                    <template #header>
                        <div class="card-header">
                            <span>运行中任务</span>
                            <el-tag type="success" effect="plain" size="small">实时刷新</el-tag>
                        </div>
                    </template>
                    <el-table :data="runningTasks" style="width: 100%" class="tech-table">
                        <el-table-column prop="name" label="任务名称" min-width="180" />
                        <el-table-column prop="source" label="来源单位" width="150" />
                        <el-table-column prop="progress" label="进度" width="180">
                            <template #default="{ row }">
                                <el-progress :percentage="row.progress" :color="progressColors" />
                            </template>
                        </el-table-column>
                        <el-table-column prop="status" label="状态" width="120">
                            <template #default="{ row }">
                                <span class="status-pulse" :class="row.status"></span>
                                {{ row.statusText }}
                            </template>
                        </el-table-column>
                    </el-table>
                </el-card>
            </div>

            <!-- Right: Data Access Status -->
            <div class="side-column">
                <el-card class="tech-card">
                    <template #header>
                        <div class="card-header">
                            <span>数据接入状态</span>
                        </div>
                    </template>
                    <div class="access-chart-mock">
                        <div class="chart-bar-container">
                            <div v-for="i in 12" :key="i" class="bar"
                                :style="{ height: Math.random() * 80 + 20 + '%', animationDelay: i * 0.1 + 's' }"></div>
                        </div>
                        <div class="chart-labels">
                            <span>接入速率: 1.2 GB/s</span>
                            <span>同步频率: 5ms</span>
                        </div>
                    </div>
                    <el-divider border-style="dashed" />
                    <div class="interface-status">
                        <div v-for="inf in interfaces" :key="inf.name" class="inf-item">
                            <span class="inf-name">{{ inf.name }}</span>
                            <el-tag :type="inf.active ? 'success' : 'danger'" size="small">{{ inf.active ? 'Connected' :
                                'Offline'
                                }}</el-tag>
                        </div>
                    </div>
                </el-card>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { Finished, Timer, Cpu } from '@element-plus/icons-vue'

const summaryStats = ref([
    { label: '总任务概览', value: '524', icon: Finished, color: '#00f2ff' },
    { label: '正在运行任务', value: '18', icon: Timer, color: '#67c23a' },
    { label: '数据接入节点', value: '42', icon: Cpu, color: '#e6a23c' }
])

const runningTasks = ref([
    { name: '跨中心遥测数据异步引接', source: '天通一号地面站', progress: 75, status: 'active', statusText: '引接中' },
    { name: '星载载荷离线数据集群汇集', source: '高分定标试验场', progress: 42, status: 'active', statusText: '计算中' },
    { name: '作战效能评估结果自动共享', source: '联合作战指挥部', progress: 98, status: 'warning', statusText: '校验中' },
    { name: '星地链路协议自动化映射', source: '航天飞行控制中心', progress: 15, status: 'active', statusText: '准备中' }
])

const interfaces = ref([
    { name: '海外测控站实时数据流', active: true },
    { name: '卫星运管中心集群接口', active: true },
    { name: '近地轨道空间环境仿真平台', active: false },
])

const progressColors = [
    { color: '#f56c6c', percentage: 20 },
    { color: '#e6a23c', percentage: 40 },
    { color: '#5cb87a', percentage: 60 },
    { color: '#1989fa', percentage: 80 },
    { color: '#6f7ad3', percentage: 100 },
]
</script>

<style scoped lang="scss">
.subpage-container {
    padding: 24px;
    height: calc(100vh - 70px);
    overflow-y: auto;
}

.page-header {
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

.overview-stats {
    margin-bottom: 24px;
}

.summary-card {
    background: rgba(16, 32, 53, 0.6);
    border: 1px solid rgba(0, 242, 255, 0.1);
    padding: 24px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    gap: 20px;
    transition: all 0.3s;

    &:hover {
        background: rgba(16, 32, 53, 0.9);
        border-color: #00f2ff;
        transform: translateY(-5px);
    }

    .label {
        font-size: 14px;
        color: #8fa3b8;
        margin-bottom: 8px;
    }

    .value {
        font-size: 28px;
        font-weight: 800;
        color: #fff;
        font-family: 'Inter', sans-serif;
    }
}

.content-grid {
    display: grid;
    grid-template-columns: 1fr 340px;
    gap: 24px;
}

.tech-card {
    background: rgba(16, 32, 53, 0.6) !important;
    border: 1px solid rgba(0, 242, 255, 0.1) !important;

    :deep(.el-card__header) {
        border-bottom: 1px solid rgba(0, 242, 255, 0.1);
        color: #00f2ff;
        font-weight: bold;
    }
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
    }

    :deep(.el-progress-bar__outer) {
        background-color: rgba(255, 255, 255, 0.1) !important;
    }
}

.status-pulse {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    margin-right: 8px;

    &.active {
        background: #67c23a;
        box-shadow: 0 0 8px #67c23a;
        animation: pulse 2s infinite;
    }

    &.warning {
        background: #e6a23c;
        box-shadow: 0 0 8px #e6a23c;
    }
}

.access-chart-mock {
    height: 120px;
    display: flex;
    flex-direction: column;
    justify-content: flex-end;

    .chart-bar-container {
        display: flex;
        align-items: flex-end;
        gap: 4px;
        height: 80px;

        .bar {
            flex: 1;
            background: linear-gradient(to top, rgba(0, 242, 255, 0.1), #00f2ff);
            border-radius: 2px 2px 0 0;
            animation: grow 1s ease-out forwards;
        }
    }

    .chart-labels {
        margin-top: 15px;
        display: flex;
        justify-content: space-between;
        font-size: 11px;
        color: #8fa3b8;
    }
}

.interface-status {
    .inf-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 10px 0;
        border-bottom: 1px solid rgba(255, 255, 255, 0.05);

        &:last-child {
            border-bottom: none;
        }

        .inf-name {
            font-size: 13px;
            color: #fff;
        }
    }
}

@keyframes pulse {
    0% {
        opacity: 1;
        transform: scale(1);
    }

    50% {
        opacity: 0.5;
        transform: scale(1.2);
    }

    100% {
        opacity: 1;
        transform: scale(1);
    }
}

@keyframes grow {
    from {
        height: 0;
    }
}
</style>
