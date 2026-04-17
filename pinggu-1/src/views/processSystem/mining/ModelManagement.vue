<template>
    <div class="subpage-container">
        <div class="page-header">
            <div class="left">
                <h3 class="page-title">评估数据分析挖掘算法模型管理</h3>
            </div>
            <div class="right">
                <el-input v-model="search" placeholder="模型名称/算法编码" style="width: 240px; margin-right: 12px;"
                    prefix-icon="Search" />
                <el-button type="primary" icon="Plus">新建模型</el-button>
            </div>
        </div>

        <!-- Stats Overview -->
        <el-row :gutter="20" class="mt-4 mb-6">
            <el-col :span="6" v-for="stat in stats" :key="stat.label">
                <div class="stat-card">
                    <div class="label">{{ stat.label }}</div>
                    <div class="value" :style="{ color: stat.color }">{{ stat.value }}</div>
                    <div class="trend">
                        <el-icon>
                            <CaretTop />
                        </el-icon> {{ stat.trend }}% 环比
                    </div>
                </div>
            </el-col>
        </el-row>

        <!-- Model Table -->
        <el-tabs v-model="activeTab" class="mining-tabs">
            <el-tab-pane label="全量模型" name="all" />
            <el-tab-pane label="机器学习" name="ml" />
            <el-tab-pane label="深度学习" name="dl" />
            <el-tab-pane label="统计分析" name="stat" />
        </el-tabs>

        <el-table :data="paginatedModels" class="tech-table">
            <el-table-column prop="name" label="模型名称" width="220" />
            <el-table-column prop="algorithm" label="核心算法">
                <template #default="{ row }">
                    <code>{{ row.algorithm }}</code>
                </template>
            </el-table-column>
            <el-table-column prop="category" label="分类">
                <template #default="{ row }">
                    <el-tag size="small" :type="row.category === 'DL' ? 'warning' : 'info'">{{ row.category }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="accuracy" label="验证准确率">
                <template #default="{ row }">
                    <span class="accuracy-val">{{ row.accuracy }}%</span>
                </template>
            </el-table-column>
            <el-table-column prop="updateTime" label="更新时间" width="180" />
            <el-table-column label="操作" width="180" fixed="right">
                <template #default>
                    <el-button link type="primary">查看</el-button>
                    <el-button link type="danger">移除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <div class="pagination-container mt-4">
            <el-pagination background layout="prev, pager, next" :total="filteredModels.length" />
        </div>
    </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search, Plus, CaretTop } from '@element-plus/icons-vue'

const search = ref('')
const activeTab = ref('all')

const stats = [
    { label: '库内模型总数', value: 128, trend: 12, color: '#00f2ff' },
    { label: '平均推理时延', value: '45ms', trend: -5, color: '#67c23a' },
    { label: '最高准确率', value: '99.2%', trend: 2, color: '#e6a23c' },
    { label: '本月调用量', value: '1.2k', trend: 25, color: '#00f2ff' }
]

const models = ref([
    { id: 1, name: '多维卫星星座抗毁能力评估模型', algorithm: 'XGBoost', category: 'ML', accuracy: 94.5, updateTime: '2026-03-01 10:20' },
    { id: 2, name: '星载合成孔径雷达目标识别网络', algorithm: 'ResNet-50', category: 'DL', accuracy: 98.1, updateTime: '2026-02-28 14:45' },
    { id: 3, name: '太阳能帆板衰减损耗预测模拟器', algorithm: 'LSTM', category: 'DL', accuracy: 89.4, updateTime: '2026-03-05 09:12' },
    { id: 4, name: '天基红蓝对抗战术协同频次关联', algorithm: 'Apriori', category: 'STAT', accuracy: 92.0, updateTime: '2026-02-15 11:30' },
    { id: 5, name: '高动态空间目标轨道轨迹平滑算法', algorithm: 'Kalman Filter', category: 'STAT', accuracy: 95.7, updateTime: '2026-03-08 16:00' },
])

const filteredModels = computed(() => {
    return models.value.filter(m => {
        const matchesSearch = m.name.toLowerCase().includes(search.value.toLowerCase()) ||
            m.algorithm.toLowerCase().includes(search.value.toLowerCase())
        const matchesTab = activeTab.value === 'all' || m.category.toLowerCase() === activeTab.value
        return matchesSearch && matchesTab
    })
})

const paginatedModels = computed(() => filteredModels.value)
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

.stat-card {
    background: rgba(16, 32, 53, 0.4);
    border: 1px solid rgba(0, 242, 255, 0.1);
    padding: 20px;
    border-radius: 8px;

    .label {
        font-size: 13px;
        color: #8fa3b8;
        margin-bottom: 10px;
    }

    .value {
        font-size: 28px;
        font-weight: bold;
        font-family: 'Roboto Mono', monospace;
    }

    .trend {
        font-size: 12px;
        color: #67c23a;
        margin-top: 5px;
        display: flex;
        align-items: center;
        gap: 4px;
    }
}

.mining-tabs {
    :deep(.el-tabs__item) {
        color: #8fa3b8;
        font-size: 15px;
    }

    :deep(.el-tabs__item.is-active) {
        color: #00f2ff;
        font-weight: bold;
    }

    :deep(.el-tabs__nav-wrap::after) {
        background-color: rgba(255, 255, 255, 0.05);
    }
}

.tech-table {
    background: rgba(16, 32, 53, 0.4) !important;
    border: 1px solid rgba(0, 242, 255, 0.1) !important;
    border-radius: 8px;

    :deep(tr) {
        background: transparent !important;
        color: #fff;
    }

    :deep(th) {
        background: rgba(0, 242, 255, 0.05) !important;
        color: #00f2ff !important;
        border-bottom: 1px solid rgba(0, 242, 255, 0.1) !important;
    }

    :deep(td) {
        border-bottom: 1px solid rgba(255, 255, 255, 0.05) !important;
    }

    .accuracy-val {
        color: #00f2ff;
        font-weight: bold;
        font-family: 'Roboto Mono', monospace;
    }

    code {
        background: rgba(255, 255, 255, 0.1);
        padding: 2px 6px;
        border-radius: 4px;
        color: #e6a23c;
    }
}

.pagination-container {
    display: flex;
    justify-content: flex-end;
}
</style>
