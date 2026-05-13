<template>
    <div class="subpage-container">
        <div class="page-header">
            <h3 class="page-title">知识管理与检索</h3>
        </div>

        <!-- Search Panel -->
        <el-card class="tech-card search-panel">
            <el-form :inline="true" :model="searchForm" class="tech-form">
                <el-form-item label="关键词检索">
                    <el-input v-model="searchForm.keyword" placeholder="实体名称、别名或描述..." style="width: 240px" clearable
                        prefix-icon="Search" />
                </el-form-item>
                <el-form-item label="实体类别">
                    <el-select v-model="searchForm.category" placeholder="全部分类" style="width: 160px" clearable>
                        <el-option label="武器装备" value="weapon" />
                        <el-option label="组织机构" value="org" />
                        <el-option label="空间环境" value="env" />
                        <el-option label="技术指标" value="metric" />
                    </el-select>
                </el-form-item>
                <el-form-item label="关联关系">
                    <el-select v-model="searchForm.relation" placeholder="全部分类" style="width: 160px" clearable>
                        <el-option label="包含 (part-of)" value="part-of" />
                        <el-option label="属于 (is-a)" value="is-a" />
                        <el-option label="对抗 (counter)" value="counter" />
                        <el-option label="依赖 (depend-on)" value="depend" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" icon="Search" @click="handleSearch">检索图谱</el-button>
                    <el-button icon="Refresh" @click="resetSearch">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- Tool Bar -->
        <div class="action-bar">
            <div class="left">
                <span class="result-text">共检索到 <span class="highlight">{{ totalResults }}</span> 条知识实体/关系</span>
            </div>
            <div class="right">
                <el-button type="primary" plain icon="Upload">批量导入 (CSV/RDF)</el-button>
                <el-button type="success" plain icon="Download">导出检索结果</el-button>
                <el-button type="danger" plain icon="Delete">批量删除</el-button>
            </div>
        </div>

        <!-- Data Grid -->
        <el-card class="tech-card table-card">
            <el-table :data="tableData" style="width: 100%" class="tech-table">
                <el-table-column type="selection" width="55" />
                <el-table-column prop="entityStart" label="核⼼实体 (Head)" min-width="180" />
                <el-table-column prop="category" label="实体类别" width="120">
                    <template #default="{ row }">
                        <el-tag size="small" :type="getCategoryType(row.category)" effect="dark">{{ row.category
                            }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="relation" label="关联关系 (Edge)" width="150">
                    <template #default="{ row }">
                        <span class="relation-badge">{{ row.relation }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="entityEnd" label="目标实体 (Tail)" min-width="180" />
                <el-table-column prop="source" label="知识来源" width="160" />
                <el-table-column prop="confidence" label="置信度" width="100" align="center">
                    <template #default="{ row }">
                        <span :class="{ 'high-conf': row.confidence > 0.8, 'med-conf': row.confidence <= 0.8 }">
                            {{ (row.confidence * 100).toFixed(0) }}%
                        </span>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="150" fixed="right">
                    <template #default="scope">
                        <el-button link type="primary" icon="Edit">编辑</el-button>
                        <el-button link type="primary" icon="Connection">查看</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-container">
                <el-pagination background layout="prev, pager, next, sizes, jumper" :total="totalResults"
                    :page-sizes="[10, 20, 50]" class="tech-pagination" />
            </div>
        </el-card>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { Search, Refresh, Upload, Download, Delete, Edit, Connection } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const searchForm = ref({
    keyword: '',
    category: '',
    relation: ''
})

const totalResults = ref(12845)

const tableData = ref([
    { entityStart: '长征五号B运载火箭', category: '武器装备', relation: '包含 (part-of)', entityEnd: 'YF-77 氢氧发动机', source: '《运载火箭系统论》', confidence: 0.98 },
    { entityStart: '太阳耀斑爆发', category: '空间环境', relation: '对抗/干扰 (counter)', entityEnd: 'GPS L1 频段信号', source: '空间天气监测网数据', confidence: 0.92 },
    { entityStart: '返回舱防热大底', category: '武器装备', relation: '属性约束 (has-attr)', entityEnd: '耐温 > 2500℃', source: '热真空试验报告_V2', confidence: 1.0 },
    { entityStart: '某型星载合成孔径雷达', category: '武器装备', relation: '依赖 (depend-on)', entityEnd: '双轴高精度太阳翼', source: '总体架构设计说明', confidence: 0.85 },
    { entityStart: '轨道衰减补偿计算', category: '技术指标', relation: '属于 (is-a)', entityEnd: '天体力学基础算法', source: '算法模型库清单', confidence: 0.95 }
])

const handleSearch = () => {
    ElMessage.success('基于图数据库 (Neo4j/Nebula) 检索完成')
}

const resetSearch = () => {
    searchForm.value = { keyword: '', category: '', relation: '' }
}

const getCategoryType = (cat) => {
    const map = {
        '武器装备': 'danger',
        '空间环境': 'warning',
        '技术指标': 'success',
        '组织机构': 'info'
    }
    return map[cat] || 'primary'
}
</script>

<style scoped lang="scss">
.subpage-container {
    padding: 24px;
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

.tech-card {
    background: rgba(16, 32, 53, 0.6) !important;
    border: 1px solid rgba(0, 242, 255, 0.1) !important;
}

.search-panel {
    margin-bottom: 20px;
    padding-bottom: 0px;

    .el-form-item {
        margin-bottom: 16px;
    }
}

.action-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .result-text {
        font-size: 14px;
        color: #8fa3b8;

        .highlight {
            color: #00f2ff;
            font-size: 16px;
            font-weight: bold;
            margin: 0 4px;
        }
    }
}

.relation-badge {
    display: inline-block;
    padding: 2px 8px;
    background: rgba(16, 32, 53, 0.8);
    border: 1px dashed rgba(0, 242, 255, 0.4);
    border-radius: 12px;
    color: #409eff;
    font-size: 12px;
}

.high-conf {
    color: #67c23a;
    font-weight: bold;
}

.med-conf {
    color: #e6a23c;
}

:deep(.tech-form) {
    .el-form-item__label {
        color: #8fa3b8;
    }

    .el-input__wrapper,
    .el-select__wrapper {
        background-color: rgba(10, 21, 37, 0.8) !important;
        box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.2) inset !important;

        &.is-focus,
        &:hover {
            box-shadow: 0 0 0 1px #00f2ff inset !important;
        }
    }

    .el-input__inner {
        color: #fff !important;
    }
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

.pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
}

:deep(.tech-pagination) {

    .el-pagination__total,
    .el-pagination__jump {
        color: #8fa3b8;
    }

    .btn-prev,
    .btn-next,
    .el-pager li {
        background: rgba(10, 21, 37, 0.8) !important;
        border: 1px solid rgba(0, 242, 255, 0.2) !important;
        color: #8fa3b8;

        &.is-active {
            background: #00f2ff !important;
            color: #000;
            border-color: #00f2ff !important;
        }

        &:hover {
            color: #00f2ff;
            border-color: #00f2ff !important;
        }
    }
}
</style>
