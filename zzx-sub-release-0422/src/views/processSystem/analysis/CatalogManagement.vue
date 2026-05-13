<template>
    <div class="subpage-container">
        <div class="page-header">
            <div class="left">
                <h3 class="page-title">数据目录管理</h3>
            </div>
            <div class="right">
                <el-input v-model="filterText" placeholder="搜索目录/资源" style="width: 250px" prefix-icon="Search" />
            </div>
        </div>

        <div class="catalog-layout">
            <!-- Left: Unit Tree -->
            <aside class="unit-sidebar">
                <div class="sidebar-title">试验单位层级</div>
                <el-tree :data="unitTree" :props="defaultProps" default-expand-all highlight-current class="tech-tree"
                    @node-click="handleNodeClick" />
            </aside>

            <!-- Right: Data Items -->
            <main class="catalog-content">
                <div class="breadcrumb-nav">
                    <el-breadcrumb separator="/">
                        <el-breadcrumb-item>全域目录</el-breadcrumb-item>
                        <el-breadcrumb-item v-if="activeUnit">{{ activeUnit }}</el-breadcrumb-item>
                    </el-breadcrumb>
                </div>

                <el-card class="tech-card">
                    <el-table :data="catalogItems" class="tech-table">
                        <el-table-column prop="name" label="资源名称" min-width="200">
                            <template #default="{ row }">
                                <div class="resource-name">
                                    <el-icon :size="16" style="margin-right: 8px">
                                        <Folder v-if="row.type === 'dir'" />
                                        <Document v-else />
                                    </el-icon>
                                    {{ row.name }}
                                </div>
                            </template>
                        </el-table-column>
                        <el-table-column prop="format" label="存储格式" width="120" />
                        <el-table-column prop="size" label="数据量" width="120" />
                        <el-table-column prop="syncTime" label="同步于" width="160" />
                        <el-table-column label="动作" width="120" fixed="right">
                            <template #default="{ row }">
                                <el-button link type="primary" v-if="row.type === 'file'">查看详情</el-button>
                                <el-button link type="primary" v-else>进入</el-button>
                            </template>
                        </el-table-column>
                    </el-table>
                </el-card>
            </main>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { Search, Folder, Document } from '@element-plus/icons-vue'

const filterText = ref('')
const activeUnit = ref('酒泉卫星发射中心')

const unitTree = ref([
    {
        label: '发射与测控基地',
        children: [
            { label: '酒泉卫星发射中心' },
            { label: '西安卫星测控中心' },
            { label: '远望号测量船编队' }
        ]
    },
    {
        label: '航天研制单位',
        children: [{ label: '空间技术研究院' }, { label: '航天动力技术研究院' }]
    }
])

const defaultProps = { children: 'children', label: 'label' }

const catalogItems = ref([
    { name: '预警卫星星载核心构件运行基线数据', type: 'dir', format: '-', size: '1.2TB', syncTime: '2024-03-09 10:00' },
    { name: '星地/星间激光通信链路原始波形文件', type: 'file', format: 'H5 / Bin', size: '240GB', syncTime: '2024-03-09 11:30' },
    { name: '遥测信号异常断链事件结构化快照分析', type: 'file', format: 'Parquet', size: '15MB', syncTime: '2024-03-09 09:20' },
    { name: '历代长征系列火箭飞行评估静态文档库', type: 'dir', format: '-', size: '4.5GB', syncTime: '2024-03-08 17:45' }
])

const handleNodeClick = (data) => {
    if (!data.children) { activeUnit.value = data.label }
}
</script>

<style scoped lang="scss">
.subpage-container {
    padding: 24px;
    height: 100%;
    overflow: hidden;
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

.catalog-layout {
    flex: 1;
    display: flex;
    gap: 20px;
    overflow: hidden;
}

.unit-sidebar {
    width: 280px;
    background: rgba(16, 32, 53, 0.4);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 8px;

    .sidebar-title {
        padding: 15px;
        font-size: 14px;
        color: #00f2ff;
        border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    }
}

.catalog-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 15px;
    overflow-y: auto;
}

.tech-tree {
    background: transparent !important;
    color: #8fa3b8 !important;
    padding: 10px;

    :deep(.el-tree-node__content:hover) {
        background: rgba(0, 242, 255, 0.05) !important;
        color: #fff;
    }

    :deep(.is-current > .el-tree-node__content) {
        background: rgba(0, 242, 255, 0.1) !important;
        color: #00f2ff;
        font-weight: bold;
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

.resource-name {
    display: flex;
    align-items: center;
}

.breadcrumb-nav {
    :deep(.el-breadcrumb__inner) {
        color: #8fa3b8 !important;

        &.is-link:hover {
            color: #00f2ff !important;
        }
    }
}
</style>
