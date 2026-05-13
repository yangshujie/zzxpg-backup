<template>
    <div class="subpage-container">
        <div class="page-header">
            <h3 class="page-title">评估全链路知识关联追溯管理</h3>
            <div class="right-actions">
                <el-button type="primary" icon="Plus" @click="handleCreate">新建追溯链路</el-button>
            </div>
        </div>

        <el-card class="tech-card table-card">
            <el-table :data="tableData" style="width: 100%" class="tech-table">
                <el-table-column prop="id" label="链路编号" width="180" />
                <el-table-column prop="name" label="追溯链路名称" min-width="200" />
                <el-table-column prop="target" label="评估目标" width="180">
                    <template #default="{ row }">
                        <span style="color: #00f2ff">{{ row.target }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="stage" label="当前阶段" width="150" align="center">
                    <template #default="{ row }">
                        <el-tag size="small" :type="getStageType(row.stage)" effect="dark">{{ row.stage }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="creator" label="创建人" width="120" />
                <el-table-column prop="date" label="创建时间" width="160" />
                <el-table-column label="操作" width="120" fixed="right">
                    <template #default="scope">
                        <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看链路</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- View Traceability Drawer -->
        <el-drawer v-model="drawerVisible" :title="`${activeRow?.name || ''} - 全链路追溯概览`" size="60%"
            custom-class="tech-drawer">
            <div class="trace-container" v-if="activeRow">
                <div class="trace-header">
                    <div class="trace-stat">
                        <span class="label">链路置信度评估</span>
                        <span class="value success">98.5%</span>
                    </div>
                    <div class="trace-stat">
                        <span class="label">知识节点跨度</span>
                        <span class="value">4 领域</span>
                    </div>
                </div>

                <h4 class="section-title">评估知识传导链</h4>
                <el-timeline class="tech-timeline">
                    <el-timeline-item v-for="(activity, index) in traceActivities" :key="index" :icon="activity.icon"
                        :type="activity.type" :color="activity.color" :size="activity.size" :hollow="activity.hollow"
                        :timestamp="activity.timestamp" placement="top">
                        <el-card class="timeline-box">
                            <h4>{{ activity.title }}</h4>
                            <p class="content">{{ activity.content }}</p>
                            <div class="meta-tags" v-if="activity.tags">
                                <el-tag v-for="tag in activity.tags" :key="tag" size="small" effect="plain"
                                    class="cyber-tag">{{
                                        tag }}</el-tag>
                            </div>
                        </el-card>
                    </el-timeline-item>
                </el-timeline>
            </div>
        </el-drawer>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { Plus, View, Connection, DataLine, DocumentChecked, Aim, WarnTriangleFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const tableData = ref([
    { id: 'TR-20240311-01', name: '远轨机动目标天基跟瞄能力评估', target: '光学跟瞄载荷', stage: '评估结论输出', creator: '系统管理员', date: '2024-03-11 10:05' },
    { id: 'TR-20240310-44', name: '深空网络测控中断影响链路', target: '测发控体系', stage: '数据模型切片', creator: '网络保障组', date: '2024-03-10 15:30' },
    { id: 'TR-20240308-12', name: '星载计算机抗单粒子翻转效能', target: '平台控制计算机', stage: '知识本体映射', creator: '可靠性工程组', date: '2024-03-08 11:20' }
])

const drawerVisible = ref(false)
const activeRow = ref(null)

const traceActivities = ref([
    {
        title: '初始目标指派',
        content: '接收演训导调下发的评估需求，确定核心评估实体为【光学跟瞄载荷】。',
        timestamp: '2024-03-11 10:05',
        type: 'primary',
        size: 'large',
        icon: Aim,
        tags: ['需求接入', '目标锚定']
    },
    {
        title: '本体库知识链接',
        content: '链接到【有效载荷本体库】，提取跟瞄精度、捕获概率等7项特征属性。',
        timestamp: '2024-03-11 10:20',
        color: '#00f2ff',
        icon: Connection,
        tags: ['图谱检索', '关系扩展']
    },
    {
        title: '评估模型适配',
        content: '根据本体特征，自动匹配【卡尔曼滤波改进跟踪评估模型(V2)】算法算子。',
        timestamp: '2024-03-11 14:00',
        color: '#e6a23c',
        icon: DataLine,
        tags: ['算子编排', '逻辑驱动']
    },
    {
        title: '异常阻断',
        content: '预处理阶段发现高维噪声干扰，模型置信度下降，记录为潜在风险节点。',
        timestamp: '2024-03-11 14:35',
        color: '#f56c6c',
        icon: WarnTriangleFilled,
        hollow: true,
        tags: ['数据告警', '噪声阻断']
    },
    {
        title: '结论汇聚输出',
        content: '克服干扰后生成评估散点图，证明跟瞄稳定性下降12%，生成最终可解释性报告。',
        timestamp: 'TBD',
        color: '#67c23a',
        size: 'large',
        icon: DocumentChecked,
        tags: ['报告生成', '归档']
    }
])

const handleCreate = () => {
    ElMessage.success('打开追溯链路配置向导')
}

const handleView = (row) => {
    activeRow.value = row
    drawerVisible.value = true
}

const getStageType = (stage) => {
    if (stage.includes('输出') || stage.includes('完成')) return 'success'
    if (stage.includes('映射') || stage.includes('切片')) return 'warning'
    return 'primary'
}
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

:deep(.tech-table) {
    background: transparent !important;
    color: #fff !important;

    th.el-table__cell {
        background: rgba(0, 242, 255, 0.05) !important;
        color: #8fa3b8 !important;
        border-bottom: 1px solid rgba(0, 242, 255, 0.1) !important;
    }
}

:deep(.tech-drawer) {
    background: #0d1a2b !important;
    border-left: 1px solid #00f2ff !important;

    .el-drawer__header {
        color: #00f2ff;
        font-weight: bold;
        border-bottom: 1px solid rgba(0, 242, 255, 0.1);
        margin-bottom: 0;
        padding-bottom: 20px;
    }
}

.trace-container {
    padding: 20px;
}

.trace-header {
    display: flex;
    gap: 20px;
    margin-bottom: 30px;

    .trace-stat {
        background: rgba(0, 242, 255, 0.05);
        border: 1px solid rgba(0, 242, 255, 0.2);
        padding: 15px 20px;
        border-radius: 4px;
        display: flex;
        flex-direction: column;
        min-width: 150px;

        .label {
            color: #8fa3b8;
            font-size: 13px;
            margin-bottom: 5px;
        }

        .value {
            color: #fff;
            font-size: 20px;
            font-weight: bold;
            font-family: monospace;
        }

        .value.success {
            color: #67c23a;
            text-shadow: 0 0 5px rgba(103, 194, 58, 0.5);
        }
    }
}

.section-title {
    color: #00f2ff;
    border-left: 3px solid #00f2ff;
    padding-left: 10px;
    margin-bottom: 20px;
}

.timeline-box {
    background: rgba(16, 32, 53, 0.6) !important;
    border: 1px solid rgba(0, 242, 255, 0.1) !important;

    h4 {
        margin-top: 0;
        margin-bottom: 10px;
        color: #fff;
    }

    .content {
        color: #8fa3b8;
        font-size: 13px;
        line-height: 1.5;
        margin-bottom: 15px;
    }
}

.meta-tags {
    display: flex;
    gap: 8px;

    .cyber-tag {
        background: rgba(0, 242, 255, 0.05) !important;
        border: 1px solid rgba(0, 242, 255, 0.3) !important;
        color: #00f2ff !important;
    }
}

:deep(.tech-timeline) {
    .el-timeline-item__timestamp {
        color: #8fa3b8;
    }

    .el-timeline-item__tail {
        border-left-color: rgba(0, 242, 255, 0.2);
    }
}
</style>
