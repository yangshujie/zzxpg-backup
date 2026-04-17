<template>
<div class="template-mgr-container">
    <div class="page-header">
        <h2 class="title">需求设计流程模板管理</h2>
        <div class="subtitle">维护“独立生成”与“协同细化”等各类业务导向的流程节点模板，支持定义环节、干预阈值及流转规则。</div>
    </div>

    <!-- 顶部工具栏 -->
    <div class="filter-bar">
        <el-input v-model="searchQuery" placeholder="搜索模板名称" class="cyber-input search-box" clearable>
            <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" class="cyber-btn" @click="addTemplate">
            <el-icon><Plus /></el-icon> 新建流程模板
        </el-button>
    </div>

    <!-- 卡片列表显示模板 -->
    <div class="card-grid">
        <div class="cyber-card template-card" v-for="item in filteredTemplates" :key="item.id">
            <div class="card-header">
                <span class="tpl-name">{{ item.name }}</span>
                <el-tag :type="item.status === 'active' ? 'success' : 'info'" size="small" effect="dark">
                    {{ item.status === 'active' ? '启用中' : '未启用' }}
                </el-tag>
            </div>
            <div class="card-body">
                <div class="tpl-desc">{{ item.desc }}</div>
                <div class="tpl-meta">
                    <span><el-icon><Timer /></el-icon> 更新于: {{ item.date }}</span>
                    <span><el-icon><CopyDocument /></el-icon> 调用次数: {{ item.uses }}</span>
                </div>
                <div class="tpl-actions">
                    <el-button type="primary" link @click="editTemplate(item)"><el-icon><Edit /></el-icon> 编辑</el-button>
                    <el-button type="danger" link @click="deleteTemplate(item)"><el-icon><Delete /></el-icon> 删除</el-button>
                </div>
            </div>
        </div>
        
        <!-- 新建占位卡片 -->
        <div class="cyber-card template-card add-card" @click="addTemplate">
            <el-icon class="add-icon"><Plus /></el-icon>
            <div class="add-text">创建一个新流程模板</div>
        </div>
    </div>
</div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search, Plus, Edit, Delete, CopyDocument, Timer } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const searchQuery = ref('')

const templates = ref([
    { id: 1, name: '标准双路并行流程模板', desc: '包含 AI 快速生成与人工协同审批双路径机制的标准体系评估需求生成流程。', date: '2026-03-12', uses: 89, status: 'active' },
    { id: 2, name: '快速应急评估直通模板', desc: '跳过协同审签与汇集阶段，直接采用通用算子库生成报告用于应急推演。', date: '2026-01-05', uses: 24, status: 'inactive' },
    { id: 3, name: '长周期装备摸底全链路流程', desc: '串行所有环节，要求最高指标的完备度审查，分中心多级意见征询。', date: '2025-11-20', uses: 12, status: 'active' },
])

const filteredTemplates = computed(() => {
    if (!searchQuery.value) return templates.value
    const q = searchQuery.value.toLowerCase()
    return templates.value.filter(t => t.name.toLowerCase().includes(q) || t.desc.toLowerCase().includes(q))
})

const addTemplate = () => {
    ElMessage.info('触发新建流程模板(演示功能)')
}

const editTemplate = (item) => {
    ElMessage.info(`正在编辑模板：${item.name}`)
}

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

.card-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 20px;
}

.cyber-card {
    background: rgba(13, 20, 31, 0.6);
    border: 1px solid rgba(0, 242, 255, 0.2);
    border-radius: 6px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    transition: all 0.3s ease;
    
    &:hover:not(.add-card) {
        transform: translateY(-3px);
        box-shadow: 0 5px 15px rgba(0, 242, 255, 0.1);
        border-color: rgba(0, 242, 255, 0.5);
    }

    .card-header {
        padding: 15px;
        background: rgba(0, 242, 255, 0.05);
        border-bottom: 1px solid rgba(0, 242, 255, 0.1);
        color: #00f2ff;
        font-weight: bold;
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .tpl-name {
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            margin-right: 10px;
        }
    }

    .card-body {
        padding: 15px;
        display: flex;
        flex-direction: column;
        flex-grow: 1;

        .tpl-desc {
            color: #8fa3b8;
            font-size: 13px;
            line-height: 1.5;
            margin-bottom: 15px;
            flex-grow: 1;
        }

        .tpl-meta {
            display: flex;
            justify-content: space-between;
            color: #5c7080;
            font-size: 12px;
            margin-bottom: 15px;
            
            span {
                display: flex;
                align-items: center;
                gap: 4px;
            }
        }

        .tpl-actions {
            border-top: 1px dashed rgba(48, 54, 61, 0.8);
            padding-top: 10px;
            display: flex;
            justify-content: flex-end;
        }
    }
    
    &.add-card {
        justify-content: center;
        align-items: center;
        border: 1px dashed rgba(0, 242, 255, 0.3);
        background: transparent;
        cursor: pointer;
        min-height: 200px;
        
        &:hover {
            border-color: #00f2ff;
            background: rgba(0, 242, 255, 0.05);
            
            .add-icon { color: #00f2ff; transform: scale(1.1); }
            .add-text { color: #00f2ff; }
        }
        
        .add-icon {
            font-size: 48px;
            color: #5c7080;
            margin-bottom: 15px;
            transition: all 0.3s;
        }
        
        .add-text {
            color: #8fa3b8;
            font-weight: bold;
            transition: all 0.3s;
        }
    }
}

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

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}
</style>
