<template>
<div class="adjust-req-container">
    <div class="page-header">
        <h2 class="title">需求调整优化</h2>
        <div class="subtitle">针对自动生成的初步需求，利用内部数据清单、算子库和模型库进行人为补充与优化，消除低置信度项目的歧义，确保整体需求完备可用。</div>
    </div>

    <div class="main-content">
        <!-- 过滤器和搜索 -->
        <div class="filter-bar">
            <el-radio-group v-model="filterStatus" class="cyber-radio">
                <el-radio-button label="all">全部需求项</el-radio-button>
                <el-radio-button label="low_confidence">需人工复核 (置信度 < 80%)</el-radio-button>
                <el-radio-button label="missing">已标注缺失</el-radio-button>
            </el-radio-group>
            
            <el-input
                v-model="searchQuery"
                placeholder="搜索要素名称或内容"
                class="cyber-input search-box"
                clearable
            >
                <template #prefix>
                    <el-icon><Search /></el-icon>
                </template>
            </el-input>
        </div>

        <!-- 可编辑表格 -->
        <div class="edit-table-container">
            <el-table :data="filteredData" class="cyber-table" height="100%" border>
                <el-table-column prop="category" label="要素分类" width="150">
                    <template #default="scope">
                        <el-tag :type="getCategoryPreset(scope.row.category)" effect="dark">{{ scope.row.category }}</el-tag>
                    </template>
                </el-table-column>
                
                <el-table-column prop="name" label="要素名称/描述" min-width="250">
                    <template #default="scope">
                        <div v-if="!scope.row.isEditing">
                            <span :class="{'missing-text': scope.row.isMissing}">
                                <el-icon v-if="scope.row.isMissing"><Warning /></el-icon>
                                {{ scope.row.name }}
                            </span>
                        </div>
                        <el-input v-else v-model="scope.row.editName" size="small" class="cyber-input-small"/>
                    </template>
                </el-table-column>
                
                <el-table-column prop="source" label="匹配库/来源" width="200">
                    <template #default="scope">
                        <div v-if="!scope.row.isEditing">
                            <el-button type="primary" link size="small" v-if="scope.row.source" @click="openLibModal(scope.row)">
                                <el-icon><Link /></el-icon> {{ scope.row.source }}
                            </el-button>
                            <span v-else class="empty-source">- 暂无关联库 -</span>
                        </div>
                        <el-select v-else v-model="scope.row.editSource" size="small" class="cyber-select-small" placeholder="请选择资源库">
                            <el-option label="通用评估指标库" value="通用评估指标库" />
                            <el-option label="算子模型仓" value="算子模型仓" />
                            <el-option label="装备实体信息库" value="装备实体信息库" />
                        </el-select>
                    </template>
                </el-table-column>

                <el-table-column prop="confidence" label="AI 置信度" width="120" align="center">
                    <template #default="scope">
                        <span :class="getConfidenceClass(scope.row.confidence)">
                            {{ scope.row.confidence }}%
                        </span>
                    </template>
                </el-table-column>

                <el-table-column label="操作" width="180" align="center" fixed="right">
                    <template #default="scope">
                        <div v-if="!scope.row.isEditing">
                            <el-button type="primary" link @click="startEdit(scope.row)">
                                <el-icon><Edit /></el-icon> 编修
                            </el-button>
                            <el-button type="success" link v-if="scope.row.confidence < 90 || scope.row.isMissing" @click="markResolved(scope.row)">
                                <el-icon><Check /></el-icon> 确认
                            </el-button>
                        </div>
                        <div v-else>
                            <el-button type="success" link @click="saveEdit(scope.row)">保存</el-button>
                            <el-button type="info" link @click="cancelEdit(scope.row)">取消</el-button>
                        </div>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <div class="bottom-actions">
            <el-button type="primary" class="cyber-btn" @click="finishAdjustment">
                <el-icon><CircleCheckFilled /></el-icon> 完成本地优化配置
            </el-button>
        </div>
    </div>
</div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Warning, Link, Edit, Check, CircleCheckFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const filterStatus = ref('low_confidence')
const searchQuery = ref('')

const reqData = ref([
    { id: 1, category: '指标体系', name: '复杂电磁环境抗干扰度', source: 'AI推理匹配', confidence: 65, isMissing: false, isEditing: false },
    { id: 2, category: '评估方法', name: '干扰环境下目标捕获概率评估说明 (缺失判定条件)', source: '', confidence: 50, isMissing: true, isEditing: false },
    { id: 3, category: '待评装备', name: '单兵便携测向终端', source: '装备实体信息库', confidence: 92, isMissing: false, isEditing: false },
    { id: 4, category: '算子模型', name: '环境白噪声模拟算子 (参数未定)', source: '', confidence: 40, isMissing: true, isEditing: false },
    { id: 5, category: '指标体系', name: '指令下发时延', source: '通用评估指标库', confidence: 95, isMissing: false, isEditing: false }
])

const filteredData = computed(() => {
    let result = reqData.value

    if (filterStatus.value === 'low_confidence') {
        result = result.filter(item => item.confidence < 80)
    } else if (filterStatus.value === 'missing') {
        result = result.filter(item => item.isMissing)
    }

    if (searchQuery.value) {
        const q = searchQuery.value.toLowerCase()
        result = result.filter(item => item.name.toLowerCase().includes(q) || item.category.toLowerCase().includes(q))
    }

    return result
})

const getCategoryPreset = (cat) => {
    const map = { '指标体系': 'primary', '评估方法': 'warning', '待评装备': 'success', '算子模型': 'danger' }
    return map[cat] || 'info'
}

const getConfidenceClass = (conf) => {
    if (conf >= 90) return 'text-success'
    if (conf >= 70) return 'text-warning'
    return 'text-danger'
}

const startEdit = (row) => {
    row.editName = row.name
    row.editSource = row.source
    row.isEditing = true
}

const saveEdit = (row) => {
    row.name = row.editName
    row.source = row.editSource
    row.isMissing = false
    row.confidence = 99 // Artificial bump after human review
    row.isEditing = false
    ElMessage.success('条目已更新')
}

const cancelEdit = (row) => {
    row.isEditing = false
}

const markResolved = (row) => {
    row.isMissing = false
    row.confidence = 100
    ElMessage.success('已标记为人工确义完成')
}

const openLibModal = (row) => {
    ElMessage.info(`正在连接 ${row.source} 资源库视图... (演示功能)`)
}

const finishAdjustment = () => {
    ElMessage.success('局部调整已保存，将前往完整评估需求页面。')
    router.push('/requirement-sys/complete')
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.adjust-req-container {
    padding: 24px;
    color: #e6edf3;
    animation: fadeIn 0.4s;
    height: 100vh;
    display: flex;
    flex-direction: column;
}

.page-header {
    margin-bottom: 20px;
    flex-shrink: 0;
    .title {
        font-size: 24px;
        color: #fff;
        text-shadow: 0 0 10px rgba(0, 242, 255, 0.4);
        margin-bottom: 8px;
    }
    .subtitle { color: #8fa3b8; font-size: 14px; line-height: 1.5; }
}

.main-content {
    background: rgba(13, 20, 31, 0.6);
    border: 1px solid rgba(0, 242, 255, 0.2);
    border-radius: 8px;
    padding: 20px;
    flex-grow: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
}

.filter-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    flex-wrap: wrap;
    gap: 15px;

    .search-box {
        width: 300px;
    }
}

.edit-table-container {
    flex-grow: 1;
    min-height: 0;
    margin-bottom: 20px;
    border: 1px solid rgba(0, 242, 255, 0.15);
    border-radius: 4px;
}

.bottom-actions {
    display: flex;
    justify-content: flex-end;
    flex-shrink: 0;
}

/* Cyber Inputs & Radios overrides */
:deep(.cyber-radio) {
    .el-radio-button__inner {
        background: rgba(13, 20, 31, 0.8);
        border-color: rgba(0, 242, 255, 0.3);
        color: #8fa3b8;
        &:hover { color: #00f2ff; }
    }
    .el-radio-button.is-active .el-radio-button__inner {
        background: rgba(0, 242, 255, 0.15);
        border-color: #00f2ff;
        color: #00f2ff;
        box-shadow: -1px 0 0 0 #00f2ff;
    }
}

:deep(.cyber-input), :deep(.cyber-input-small) {
    .el-input__wrapper {
        background-color: rgba(6, 13, 23, 0.6);
        border-color: rgba(0, 242, 255, 0.3);
        box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.3) inset;
        
        &.is-focus {
            box-shadow: 0 0 0 1px #00f2ff inset, 0 0 8px rgba(0, 242, 255, 0.4);
        }
    }
    .el-input__inner { color: #00f2ff; }
}

:deep(.cyber-select-small) {
    width: 100%;
    .el-input__wrapper {
        background-color: rgba(6, 13, 23, 0.6);
        box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.3) inset;
    }
    .el-input__inner { color: #fff; }
}

/* Table styling */
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

.text-success { color: #3fb950; font-weight: bold; text-shadow: 0 0 5px rgba(63, 185, 80, 0.4); }
.text-warning { color: #d29922; font-weight: bold; text-shadow: 0 0 5px rgba(210, 153, 34, 0.4); }
.text-danger { color: #f85149; font-weight: bold; text-shadow: 0 0 5px rgba(248, 81, 73, 0.4); }

.missing-text {
    color: #f85149;
    display: flex;
    align-items: center;
    gap: 6px;
}

.empty-source {
    color: #5c7080;
    font-style: italic;
    font-size: 12px;
}

.cyber-btn {
    background: rgba(0, 242, 255, 0.1);
    border: 1px solid #00f2ff;
    color: #00f2ff;
    box-shadow: 0 0 10px rgba(0, 242, 255, 0.2);
    padding: 8px 24px;
    
    &:hover, &:focus {
        background: rgba(0, 242, 255, 0.2);
        box-shadow: 0 0 20px rgba(0, 242, 255, 0.4);
    }
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}
</style>
