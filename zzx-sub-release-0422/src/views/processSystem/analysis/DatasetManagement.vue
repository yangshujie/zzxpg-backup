<template>
    <div class="subpage-container">
        <div class="page-header">
            <div class="left">
                <h3 class="page-title">数据集管理</h3>
            </div>
            <div class="right">
                <el-button type="primary" icon="Plus">构造训练数据集</el-button>
                <el-button plain icon="Download">导出选定集</el-button>
            </div>
        </div>

        <el-card class="tech-card">
            <el-table :data="datasets" class="tech-table">
                <el-table-column type="selection" width="55" />
                <el-table-column prop="name" label="数据集名称" min-width="200" />
                <el-table-column prop="items" label="数据条数" width="120" />
                <el-table-column prop="source" label="来源" width="150" />
                <el-table-column prop="creator" label="创建者" width="120" />
                <el-table-column prop="createTime" label="创建日期" width="160" />
                <el-table-column label="操作" width="180" fixed="right">
                    <template #default="scope">
                        <el-button link type="primary">探索</el-button>
                        <el-button link type="primary">加工</el-button>
                        <el-button link type="danger">销毁</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { Plus, Download } from '@element-plus/icons-vue'

const datasets = ref([
    { name: '2024Q1_天绘卫星性能全量离线数据集', items: '245,600', source: '汇总整编', creator: 'admin', createTime: '2024-03-01' },
    { name: '姿轨控系统异常特征提取训练集_v2', items: '12,000', source: '挖掘中心', creator: 'researcher_1', createTime: '2024-03-05' },
    { name: '深空测控长基线干涉信号杂波历史库', items: '890', source: '填报系统', creator: 'operator_A', createTime: '2024-02-28' },
    { name: '低轨星座组网星间链路综合抗毁数据集', items: '1,500,000', source: '异步引接', creator: 'system', createTime: '2024-03-09' },
])
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
}
</style>
