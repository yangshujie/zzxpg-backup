<template>
    <div class="subpage-container">
        <div class="page-header">
            <h3 class="page-title">知识关联分析</h3>
            <div class="right-actions">
                <el-button type="primary" icon="Plus">新建应用空间</el-button>
            </div>
        </div>

        <el-row :gutter="20">
            <!-- Left: Space Configuration -->
            <el-col :span="8">
                <el-card class="tech-card config-card">
                    <template #header>
                        <div class="card-header">
                            <span>我的应用空间</span>
                            <el-button link type="primary" icon="Setting">空间管理</el-button>
                        </div>
                    </template>

                    <el-menu default-active="1" class="space-menu" background-color="transparent" text-color="#8fa3b8"
                        active-text-color="#00f2ff">
                        <el-menu-item index="1">
                            <el-icon>
                                <Monitor />
                            </el-icon>
                            <span>装备抗干扰能力弱点分析</span>
                        </el-menu-item>
                        <el-menu-item index="2">
                            <el-icon>
                                <TrendCharts />
                            </el-icon>
                            <span>效能指标传导影响网络</span>
                        </el-menu-item>
                        <el-menu-item index="3">
                            <el-icon>
                                <Location />
                            </el-icon>
                            <span>战场地理气象多维约束图</span>
                        </el-menu-item>
                    </el-menu>

                    <el-divider border-style="dashed" />

                    <div class="space-engine-config">
                        <h4 class="engine-title">分析驱动引擎配置</h4>
                        <el-form label-position="top">
                            <el-form-item label="加载规则集 (Rule-Driven)">
                                <el-select v-model="activeRule" placeholder="请选择规则..." style="width: 100%">
                                    <el-option label="强关联漏洞穿透规则" value="r1" />
                                    <el-option label="多跳链路衰减规则" value="r2" />
                                </el-select>
                            </el-form-item>
                            <el-form-item label="聚合算子 (Operator-Driven)">
                                <el-select v-model="activeOp" placeholder="请选择算子..." style="width: 100%">
                                    <el-option label="PageRank 核心节点发现" value="op1" />
                                    <el-option label="最短博弈对抗路径" value="op2" />
                                </el-select>
                            </el-form-item>
                        </el-form>
                    </div>
                </el-card>
            </el-col>

            <!-- Right: View & Canvas -->
            <el-col :span="16">
                <el-card class="tech-card canvas-card">
                    <template #header>
                        <div class="card-header">
                            <span>多维交叉视图展示</span>
                            <el-button-group>
                                <el-button type="primary" size="small" icon="Connection">力导向图</el-button>
                                <el-button size="small" icon="Help">辐射图</el-button>
                            </el-button-group>
                        </div>
                    </template>

                    <!-- Placeholder for Echart -->
                    <div class="graph-placeholder">
                        <div class="pulse-ring"></div>
                        <el-icon class="center-icon" :size="48">
                            <DataLine />
                        </el-icon>
                        <p class="desc">分析引擎运行中... 正在计算节点权重与连边衰减</p>
                    </div>

                    <div class="analytics-metrics">
                        <div class="metric-item">
                            <div class="label">知识切片涉及节点</div>
                            <div class="value">1,452</div>
                        </div>
                        <div class="metric-item">
                            <div class="label">规则触发次数</div>
                            <div class="value highlight">89</div>
                        </div>
                        <div class="metric-item">
                            <div class="label">核心风险簇汇聚点</div>
                            <div class="value danger">3</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { Plus, Setting, Monitor, TrendCharts, Location, Connection, Help, DataLine } from '@element-plus/icons-vue'

const activeRule = ref('r1')
const activeOp = ref('op1')
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

    .right-actions {
        display: flex;
        align-items: center;
        gap: 20px;
    }
}

.tech-card {
    background: rgba(16, 32, 53, 0.6) !important;
    border: 1px solid rgba(0, 242, 255, 0.1) !important;
    height: calc(100vh - 120px);
}

.config-card {
    .el-menu {
        border-right: none;
    }

    .el-menu-item {
        margin-bottom: 8px;
        border-radius: 4px;

        &:hover {
            background-color: rgba(0, 242, 255, 0.1) !important;
            color: #fff !important;
        }

        &.is-active {
            background: linear-gradient(90deg, rgba(0, 242, 255, 0.2) 0%, transparent 100%) !important;
            border-left: 3px solid #00f2ff;
            font-weight: bold;
        }
    }
}

.space-engine-config {
    padding: 10px;

    .engine-title {
        color: #00f2ff;
        margin-top: 0;
        margin-bottom: 16px;
        font-size: 14px;
        letter-spacing: 1px;
    }
}

.canvas-card {
    display: flex;
    flex-direction: column;

    :deep(.el-card__body) {
        flex: 1;
        display: flex;
        flex-direction: column;
        padding: 0;
    }

    .graph-placeholder {
        flex: 1;
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        background: radial-gradient(circle at center, rgba(0, 242, 255, 0.05) 0%, transparent 70%);
        position: relative;
        overflow: hidden;

        .pulse-ring {
            position: absolute;
            width: 150px;
            height: 150px;
            border: 2px solid rgba(0, 242, 255, 0.5);
            border-radius: 50%;
            animation: radarPulse 2s infinite ease-out;
        }

        .center-icon {
            color: #00f2ff;
            z-index: 2;
        }

        .desc {
            margin-top: 24px;
            color: #8fa3b8;
            font-size: 14px;
            letter-spacing: 2px;
            z-index: 2;
        }
    }
}

.analytics-metrics {
    display: flex;
    background: rgba(10, 21, 37, 0.9);
    border-top: 1px solid rgba(0, 242, 255, 0.1);

    .metric-item {
        flex: 1;
        padding: 20px;
        text-align: center;
        border-right: 1px solid rgba(0, 242, 255, 0.1);

        &:last-child {
            border-right: none;
        }

        .label {
            color: #8fa3b8;
            font-size: 13px;
            margin-bottom: 8px;
        }

        .value {
            color: #fff;
            font-size: 24px;
            font-weight: bold;
            font-family: 'Courier New', Courier, monospace;

            &.highlight {
                color: #e6a23c;
            }

            &.danger {
                color: #f56c6c;
                text-shadow: 0 0 10px rgba(245, 108, 108, 0.5);
            }
        }
    }
}

@keyframes radarPulse {
    0% {
        transform: scale(0.5);
        opacity: 1;
    }

    100% {
        transform: scale(2.5);
        opacity: 0;
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
</style>
