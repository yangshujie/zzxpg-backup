<template>
    <div class="subpage-container">
        <div class="page-header">
            <div class="left">
                <h3 class="page-title">本体知识构建</h3>
            </div>
            <div class="right">
                <el-input v-model="searchQuery" placeholder="搜索本体名称或领域" clearable
                    style="width: 260px; margin-right: 12px;" prefix-icon="Search" />
                <el-button type="primary" icon="Plus" @click="handleCreate">自构建本体</el-button>
                <el-button type="success" icon="UploadFilled" @click="handleBatchCreate">批量导入构建</el-button>
                <el-button type="warning" icon="DocumentCopy" @click="handleReuseChecklist">知识清单复用</el-button>
            </div>
        </div>

        <el-card class="tech-card table-card">
            <el-table :data="tableData" style="width: 100%" class="tech-table">
                <el-table-column prop="name" label="本体知识库名称" min-width="200" />
                <el-table-column prop="domain" label="所属专业领域" width="160">
                    <template #default="{ row }">
                        <el-tag size="small" effect="dark" type="info">{{ row.domain }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="entityCount" label="实体数量" width="120" align="center" />
                <el-table-column prop="relationCount" label="关系数量" width="120" align="center" />
                <el-table-column prop="updateTime" label="更新时间" width="180" />
                <el-table-column prop="status" label="状态" width="120" align="center">
                    <template #default="{ row }">
                        <div class="status-cell">
                            <span class="status-indicator" :class="row.status"></span>
                            {{ row.status === 'active' ? '已发布' : '构建中' }}
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="220" fixed="right">
                    <template #default="{ row }">
                        <div class="action-buttons">
                            <el-button link type="primary" icon="Edit">编辑</el-button>
                            <el-button link type="primary" icon="View">概览</el-button>
                        </div>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- Reuse Checklist Dialog -->
        <el-dialog v-model="reuseVisible" title="基于知识清单快速构建" width="600px" custom-class="tech-dialog">
            <el-form label-width="120px" label-position="left">
                <el-form-item label="选择基础清单">
                    <el-select v-model="selectedChecklist" style="width: 100%" placeholder="请选择现有装备技术特征核对清单...">
                        <el-option label="某型固体火箭发动机故障树清单" value="1" />
                        <el-option label="卫星姿轨控系统遥测参数映射清单" value="2" />
                        <el-option label="电磁环境干扰因素分级评估清单" value="3" />
                    </el-select>
                </el-form-item>
                <el-form-item label="目标本体名称">
                    <el-input v-model="newOntologyName" placeholder="为新生成的本体命名" />
                </el-form-item>
                <el-form-item label="领域映射策略">
                    <el-radio-group v-model="mappingStrategy" class="tech-radio">
                        <el-radio-button label="strict">严格保持同构</el-radio-button>
                        <el-radio-button label="fuzzy">模糊语义泛化</el-radio-button>
                    </el-radio-group>
                </el-form-item>
                <el-alert title="复用清单将自动把清单中的条目和嵌套层级转化为本体的实体(Entity)与包含关系(is-a/part-of)。" type="info" show-icon
                    :closable="false"
                    style="background: rgba(0,242,255,0.05); color: #8fa3b8; border: 1px solid rgba(0,242,255,0.2);" />
            </el-form>
            <template #footer>
                <div class="dialog-action">
                    <el-button @click="reuseVisible = false">取消</el-button>
                    <el-button type="primary" @click="confirmReuse">开始自动化构建</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { Plus, Search, View, Edit, ArrowDown, UploadFilled, DocumentCopy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const searchQuery = ref('')
const reuseVisible = ref(false)
const selectedChecklist = ref('')
const newOntologyName = ref('')
const mappingStrategy = ref('strict')

const tableData = ref([
    { name: '航天器平台通用组件本体', domain: '平台系统', entityCount: 1245, relationCount: 3890, updateTime: '2024-03-11 09:30', status: 'active' },
    { name: '空间环境效应对抗基础本体', domain: '环境与对抗', entityCount: 450, relationCount: 1120, updateTime: '2024-03-10 14:15', status: 'active' },
    { name: '新型多模复合导引头特征本体', domain: '有效载荷', entityCount: 0, relationCount: 0, updateTime: '2024-03-11 11:00', status: 'building' },
    { name: '运载火箭测发控评估规则字典', domain: '测发控体系', entityCount: 890, relationCount: 2200, updateTime: '2024-03-05 16:40', status: 'active' },
    { name: '体系对抗想定兵力推演逻辑库', domain: '战术战役', entityCount: 3400, relationCount: 12050, updateTime: '2024-02-28 10:10', status: 'active' }
])

const handleCreate = () => {
    ElMessage.success('打开自建本体图谱画布')
}

const handleBatchCreate = () => {
    ElMessage.info('触发批量导入（支持 CSV/RDF/OWL 格式）')
}

const handleReuseChecklist = () => {
    reuseVisible.value = true
}

const confirmReuse = () => {
    if (!selectedChecklist.value || !newOntologyName.value) {
        ElMessage.warning('请填写完整复用配置')
        return
    }
    ElMessage.success(`已发起从清单自动化萃取知识构建本体：${newOntologyName.value}`)
    reuseVisible.value = false
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

.status-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;

    .status-indicator {
        width: 8px;
        height: 8px;
        border-radius: 50%;

        &.active {
            background: #67c23a;
            box-shadow: 0 0 8px #67c23a;
        }

        &.building {
            background: #e6a23c;
            animation: pulse 1.5s infinite;
        }
    }
}

.action-buttons {
    display: flex;
    align-items: center;
    gap: 8px;
}

@keyframes pulse {
    0% {
        opacity: 0.5;
        transform: scale(0.8);
    }

    50% {
        opacity: 1;
        transform: scale(1.2);
        box-shadow: 0 0 10px #e6a23c;
    }

    100% {
        opacity: 0.5;
        transform: scale(0.8);
    }
}

:deep(.tech-dialog) {
    background: #0d1a2b !important;
    border: 1px solid #00f2ff !important;

    .el-dialog__title {
        color: #00f2ff !important;
    }

    .el-form-item__label {
        color: #8fa3b8 !important;
    }
}

:deep(.tech-radio) {
    .el-radio-button__inner {
        background: rgba(10, 21, 37, 0.8) !important;
        border: 1px solid rgba(0, 242, 255, 0.2) !important;
        color: #8fa3b8;
    }

    .el-radio-button__original-radio:checked+.el-radio-button__inner {
        background: #00f2ff !important;
        color: #000;
        box-shadow: 0 0 10px rgba(0, 242, 255, 0.5);
    }
}
</style>
