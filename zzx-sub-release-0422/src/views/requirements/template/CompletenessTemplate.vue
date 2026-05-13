<template>
<div class="template-mgr-container">
    <div class="page-header">
        <h2 class="title">完备度分析模板管理</h2>
        <div class="subtitle">维护不同的完备度评分模板，配置四大维度（数据、指标、方法、模型）的评分权重和自动体检规则。</div>
    </div>

    <!-- 顶部工具栏 -->
    <div class="filter-bar">
        <el-input v-model="searchQuery" placeholder="搜索体检模板" class="cyber-input search-box" clearable>
            <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" class="cyber-btn" @click="addTemplate">
            <el-icon><Plus /></el-icon> 新建完备度模板
        </el-button>
    </div>

    <!-- 列表数据 -->
    <div class="table-container">
        <el-table :data="filteredTemplates" class="cyber-table" border>
            <el-table-column prop="name" label="模板名称" min-width="200" />
            <el-table-column label="权重占比 (数据:指标:方法:模型)" min-width="250" align="center">
                <template #default="scope">
                    <div class="weights">
                        <el-tag type="info" size="small">{{ scope.row.w1 }}%</el-tag>:
                        <el-tag type="primary" size="small">{{ scope.row.w2 }}%</el-tag>:
                        <el-tag type="warning" size="small">{{ scope.row.w3 }}%</el-tag>:
                        <el-tag type="danger" size="small">{{ scope.row.w4 }}%</el-tag>
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
                <template #default="scope">
                    <span :class="scope.row.status === 'active' ? 'text-success' : 'text-info'">
                        {{ scope.row.status === 'active' ? '生效中' : '已停用' }}
                    </span>
                </template>
            </el-table-column>
            <el-table-column prop="date" label="最后修改日期" width="150" align="center" />
            <el-table-column label="操作" width="200" align="center" fixed="right">
                <template #default="scope">
                    <el-button type="primary" link @click="editTemplate(scope.row)"><el-icon><Setting /></el-icon> 规则配置</el-button>
                    <el-button type="danger" link @click="deleteTemplate(scope.row)"><el-icon><Delete /></el-icon> 删除</el-button>
                </template>
            </el-table-column>
        </el-table>
    </div>
</div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search, Plus, Setting, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const searchQuery = ref('')

const templates = ref([
    { id: 1, name: '通用装备效能评估完备度检查模板', w1: 25, w2: 25, w3: 25, w4: 25, status: 'active', date: '2026-03-01' },
    { id: 2, name: '重数据驱动类任务完备度检查', w1: 40, w2: 20, w3: 20, w4: 20, status: 'inactive', date: '2025-12-15' },
    { id: 3, name: '算子机理验证类专项检查', w1: 15, w2: 20, w3: 25, w4: 40, status: 'active', date: '2026-02-10' }
])

const filteredTemplates = computed(() => {
    if (!searchQuery.value) return templates.value
    const q = searchQuery.value.toLowerCase()
    return templates.value.filter(t => t.name.toLowerCase().includes(q))
})

const addTemplate = () => ElMessage.info('触发新建完备度分析模板(演示功能)')
const editTemplate = (item) => ElMessage.info(`正在配置模板：${item.name}`)
const deleteTemplate = (item) => {
    ElMessageBox.confirm(`确认要删除模板 "${item.name}" 吗？该操作不可逆。`, '警告', {
        confirmButtonText: '删除', cancelButtonText: '取消', type: 'error'
    }).then(() => {
        templates.value = templates.value.filter(t => t.id !== item.id)
        ElMessage.success('模板已删除')
    }).catch(() => {})
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.template-mgr-container {
    padding: 24px;
    color: #e6edf3;
    animation: fadeIn 0.4s;
}

.page-header {
    margin-bottom: 24px;
    .title {
        font-size: 24px;
        color: #fff;
        text-shadow: 0 0 10px rgba(0, 242, 255, 0.4);
        margin-bottom: 8px;
    }
    .subtitle { color: #8fa3b8; font-size: 14px; line-height: 1.5; }
}

.filter-bar {
    display: flex;
    justify-content: space-between;
    margin-bottom: 24px;
    .search-box { width: 300px; }
}

.table-container {
    background: rgba(13, 20, 31, 0.6);
    padding: 2px;
    border-radius: 4px;
    border: 1px solid rgba(0, 242, 255, 0.2);
}

.weights {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 5px;
    font-family: monospace;
}

.text-success { color: #3fb950; font-weight: bold; }
.text-info { color: #5c7080; font-style: italic; }

:deep(.cyber-input) {
    .el-input__wrapper {
        background-color: rgba(6, 13, 23, 0.6);
        border-color: rgba(0, 242, 255, 0.3);
        box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.3) inset;
    }
    .el-input__inner { color: #00f2ff; }
}

.cyber-btn {
    background: rgba(0, 242, 255, 0.1);
    border: 1px solid #00f2ff;
    color: #00f2ff;
    &:hover { background: rgba(0, 242, 255, 0.2); box-shadow: 0 0 15px rgba(0, 242, 255, 0.4); }
}

:deep(.cyber-table) {
    background-color: transparent !important;
    --el-table-border-color: rgba(0, 242, 255, 0.1);
    --el-table-header-bg-color: rgba(6, 13, 23, 0.9);
    --el-table-header-text-color: #00f2ff;
    --el-table-row-hover-bg-color: rgba(0, 242, 255, 0.05);

    tr { background-color: transparent !important; }
    th.el-table__cell { font-weight: bold; }
    .el-table__inner-wrapper::before, .el-table__border-left-patch, .el-table__border-bottom-patch {
        background-color: transparent;
    }
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}
</style>
