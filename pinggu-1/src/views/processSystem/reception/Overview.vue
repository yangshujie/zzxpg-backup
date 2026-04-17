<template>
    <div class="subpage-container">
        <div class="page-header">
            <h3 class="page-title">状态概览</h3>
        </div>

        <div class="overview-stats">
            <el-row :gutter="20">
                <el-col :span="6" v-for="item in stats" :key="item.label">
                    <div class="stat-card">
                        <div class="stat-label">{{ item.label }}</div>
                        <div class="stat-value" :style="{ color: item.color }">{{ item.value }}</div>
                        <div class="stat-trend">
                            <span class="trend-icon" :class="item.trend"></span>
                            {{ item.desc }}
                        </div>
                    </div>
                </el-col>
            </el-row>
        </div>

        <div class="content-grid">
            <div class="main-content">
                <el-card class="tech-card">
                    <template #header>
                        <div class="card-header">
                            <span>最新评估需求列表</span>
                        </div>
                    </template>
                    <el-table :data="latestRequirements" style="width: 100%" class="tech-table">
                        <el-table-column prop="id" label="需求编号" width="120" />
                        <el-table-column prop="name" label="需求名称" min-width="200" />
                        <el-table-column prop="type" label="需求类型" width="120" />
                        <el-table-column prop="status" label="状态" width="120">
                            <template #default="scope">
                                <el-tag :type="scope.row.statusType" size="small">{{ scope.row.status }}</el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="time" label="发布时间" width="180" />
                        <el-table-column label="操作" width="100">
                            <template #default="scope">
                                <el-button link type="primary" size="small"
                                    @click="handleView(scope.row)">采集表单</el-button>
                            </template>
                        </el-table-column>
                    </el-table>
                </el-card>
            </div>

            <div class="side-content">
                <el-card class="tech-card">
                    <template #header>
                        <div class="card-header">
                            <span>采集节点状态</span>
                        </div>
                    </template>
                    <div class="node-status-list">
                        <div v-for="i in 5" :key="i" class="node-item">
                            <div class="node-info">
                                <span class="node-name">XS-GS-STATION-0{{ i }}</span>
                                <span class="node-ip">北斗/高分基站编组 {{ i }}</span>
                            </div>
                            <el-tag type="success" size="small" effect="dark">Active</el-tag>
                        </div>
                    </div>
                </el-card>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

const stats = ref([
    { label: '总需求数', value: '128', color: '#00f2ff', trend: 'up', desc: '较上周 +12%' },
    { label: '进行中采集', value: '45', color: '#67c23a', trend: 'stable', desc: '运行平稳' },
    { label: '待处理异常', value: '03', color: '#f56c6c', trend: 'down', desc: '持续优化中' },
    { label: '已完成表单', value: '1,024', color: '#e6a23c', trend: 'up', desc: '采集效率 98%' }
])

const latestRequirements = ref([
    { id: 'REQ-2024-001', name: '预警卫星星载软件可靠性专项评估', type: '专项评估', status: '进行中', statusType: 'primary', time: '2024-03-08 14:00' },
    { id: 'REQ-2024-002', name: '海外测控站星地链路抗截获能力测试', type: '性能评估', status: '待审核', statusType: 'warning', time: '2024-03-09 09:30' },
    { id: 'REQ-2024-003', name: '天基红蓝双方体系对抗演练数据采集', type: '演练评估', status: '已完成', statusType: 'success', time: '2024-03-07 16:45' },
    { id: 'REQ-2024-004', name: '运载火箭飞控系统网络安全脆弱性扫描', type: '安全评估', status: '进行中', statusType: 'primary', time: '2024-03-09 11:20' }
])
const handleView = (row) => {
    ElMessage.success(`正在根据需求 "${row.name}" 智能生成采集表单...`)
    router.push({
        path: '/process/reception-sys/designer',
        query: {
            reqId: row.id,
            reqName: row.name,
            source: 'requirement'
        }
    })
}   
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

.stat-card {
    background: rgba(16, 32, 53, 0.6);
    border: 1px solid rgba(0, 242, 255, 0.1);
    padding: 20px;
    border-radius: 8px;
    transition: all 0.3s;

    &:hover {
        border-color: rgba(0, 242, 255, 0.3);
        background: rgba(16, 32, 53, 0.8);
        transform: translateY(-5px);
    }

    .stat-label {
        font-size: 14px;
        color: #8fa3b8;
        margin-bottom: 12px;
    }

    .stat-value {
        font-size: 32px;
        font-weight: 800;
        font-family: 'Inter', sans-serif;
        margin-bottom: 8px;
    }

    .stat-trend {
        font-size: 12px;
        color: rgba(255, 255, 255, 0.5);
    }
}

.content-grid {
    display: grid;
    grid-template-columns: 1fr 300px;
    gap: 20px;
}

/* Custom Element Plus styles for tech theme */
:deep(.tech-card) {
    background: rgba(16, 32, 53, 0.6) !important;
    border: 1px solid rgba(0, 242, 255, 0.1) !important;

    .el-card__header {
        border-bottom: 1px solid rgba(0, 242, 255, 0.1);

        span {
            color: #00f2ff;
            font-weight: bold;
            letter-spacing: 1px;
        }
    }

    .el-card__body {
        padding: 15px;
    }
}

:deep(.tech-table) {
    background: transparent !important;
    color: #fff !important;

    tr {
        background: transparent !important;
    }

    th.el-table__cell {
        background: rgba(0, 242, 255, 0.05) !important;
        color: #8fa3b8 !important;
        border-bottom: 1px solid rgba(0, 242, 255, 0.1) !important;
    }

    td.el-table__cell {
        border-bottom: 1px solid rgba(0, 242, 255, 0.05) !important;
    }
}

.node-status-list {
    .node-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 0;
        border-bottom: 1px solid rgba(255, 255, 255, 0.05);

        &:last-child {
            border-bottom: none;
        }

        .node-info {
            display: flex;
            flex-direction: column;

            .node-name {
                font-size: 13px;
                color: #fff;
            }

            .node-ip {
                font-size: 11px;
                color: #5b6f82;
            }
        }
    }
}
</style>
