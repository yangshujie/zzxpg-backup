<template>
<div class="template-mgr-container">
    <div class="page-header">
        <h2 class="title">需求报告模板管理</h2>
        <div class="subtitle">管理最终向其他中心下发及归档流转时使用的试验任务书、技术协议等报告排版方案。支持Word及内嵌图表动态插值配置。</div>
    </div>

    <!-- 顶部工具栏 -->
    <div class="filter-bar">
        <el-input v-model="searchQuery" placeholder="搜索报告模板" class="cyber-input search-box" clearable>
            <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" class="cyber-btn" @click="addTemplate">
            <el-icon><DocumentAdd /></el-icon> 导入新模板 (*.docx)
        </el-button>
    </div>

    <!-- 列表数据 -->
    <div class="table-container">
        <el-table :data="filteredTemplates" class="cyber-table" border>
            <el-table-column prop="name" label="报告/文档类型" min-width="200">
                <template #default="scope">
                    <span class="doc-icon"><el-icon><Document /></el-icon></span>
                    {{ scope.row.name }}
                </template>
            </el-table-column>
            <el-table-column prop="type" label="主要受众中心" min-width="150" />
            <el-table-column prop="tags" label="提取要素插值" min-width="250">
                <template #default="scope">
                    <el-tag v-for="tag in scope.row.tags" :key="tag" type="info" size="small" effect="plain" class="mr-1">
                        {{ tag }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="date" label="最后修改日期" width="150" align="center" />
            <el-table-column label="操作" width="200" align="center" fixed="right">
                <template #default="scope">
                    <el-button type="primary" link @click="editTemplate(scope.row)"><el-icon><EditPen /></el-icon> 映射设置</el-button>
                    <el-button type="danger" link @click="deleteTemplate(scope.row)"><el-icon><Delete /></el-icon> 移除</el-button>
                </template>
            </el-table-column>
        </el-table>
    </div>
</div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search, DocumentAdd, Document, EditPen, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const searchQuery = ref('')

const templates = ref([
    { id: 1, name: '标准效能试验任务大纲模板(通稿)', type: '指挥中心, 主建单位', tags: ['${目标背景}', '${待评装备列表}', '${核心体系指标}', '${完备度结论}'], date: '2026-03-08' },
    { id: 2, name: '细化指标采集分发单(外场专用)', type: '电磁中心, 网络中心', tags: ['${中心编号}', '${算子采集树}', '${入站上报协议}'], date: '2026-02-14' },
    { id: 3, name: '算法验证模型白皮书草案', type: '效能分析中心', tags: ['${算子清单}', '${算子有向无环图}', '${参数设定表}'], date: '2025-11-30' }
])

const filteredTemplates = computed(() => {
    if (!searchQuery.value) return templates.value
    const q = searchQuery.value.toLowerCase()
    return templates.value.filter(t => t.name.toLowerCase().includes(q) || t.type.toLowerCase().includes(q))
})

const addTemplate = () => ElMessage.info('触发上传本地 Word 模板文件进行自动插值变量识别(演示)')
const editTemplate = (item) => ElMessage.info(`正在配置 ${item.name} 的插值变量映射规则`)
const deleteTemplate = (item) => {
    ElMessageBox.confirm(`确认要移除文档映射模板 "${item.name}" 吗？该操作不会删除本地原文件。`, '警告', {
        confirmButtonText: '移除', cancelButtonText: '取消', type: 'error'
    }).then(() => {
        templates.value = templates.value.filter(t => t.id !== item.id)
        ElMessage.success('模板移除成功')
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

.doc-icon {
    color: #bc8cff;
    margin-right: 8px;
    font-size: 16px;
    vertical-align: middle;
}

.mr-1 { margin-right: 6px; margin-bottom: 4px; }

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
