<template>
    <div class="requirement-management">
        <!-- Left Sidebar -->
        <aside class="left-sidebar">
            <div class="sb-lbl">状态筛选</div>
            <div v-for="status in statusOptions" :key="status.value" class="sb-item"
                :class="{ active: activeStatus === status.value }" @click="activeStatus = status.value">
                <span>{{ status.icon }}</span>{{ status.label }}
                <span class="sb-count">{{ getStatusCount(status.value) }}</span>
            </div>
        </aside>

        <!-- Main Content -->
        <main class="main-content">
            <!-- Filter Bar -->
            <div class="filter-bar">
                <div class="search-box">
                    <el-icon>
                        <Search />
                    </el-icon>
                    <input type="text" v-model="searchQuery" placeholder="搜索需求名称、装备名称..." @keyup.enter="handleQuery">
                </div>

                <div class="filter-tags">
                    <span v-for="level in levelOptions" :key="level" class="filter-tag"
                        :class="{ on: activeLevel === level }" @click="activeLevel = level">
                        {{ level }}
                    </span>
                </div>

                <div class="actions">
                    <el-button class="tb-btn tb-ghost" @click="importDialog = true">📥 导入评估方案</el-button>
                    <el-button class="tb-btn tb-ghost" @click="templateDialog = true">📋 加载模板</el-button>
                    <el-button type="primary" class="tb-btn tb-jam" @click="newReqDialog = true">＋ 新增需求</el-button>
                </div>
            </div>

            <!-- Requirement Table -->
            <div class="req-table-container" v-loading="loading" element-loading-background="rgba(13, 17, 23, 0.8)">
                <el-table v-if="filteredRequirements.length > 0" :data="filteredRequirements" style="width: 100%"
                    class="tech-table" @row-click="openRequirement">

                    <el-table-column label="工程ID" width="100" align="center">
                        <template #default="{ row }">
                            <span class="id-badge">{{ row.projectId || '-' }}</span>
                        </template>
                    </el-table-column>

                    <el-table-column label="工程名称" min-width="200">
                        <template #default="{ row }">
                            <span class="table-title-text">{{ row.projectName || '未命名需求' }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="需求ID" width="100" align="center">
                        <template #default="{ row }">
                            <span class="id-badge">{{ row.requirementId || '-' }}</span>
                        </template>
                    </el-table-column>

                    <el-table-column label="状态" width="100" align="center">
                        <template #default="{ row }">
                            <el-tag :type="getStatusTagType(row.status)" size="small" effect="dark">{{
                                getStatusLabel(row.status) }}</el-tag>
                        </template>
                    </el-table-column>

                    <el-table-column label="评估级别" width="120" align="center">
                        <template #default="{ row }">
                            <el-tag type="success" size="small" effect="plain">{{ getLevelLabel(row.evaluationLevel)
                            }}</el-tag>
                        </template>
                    </el-table-column>

                    <el-table-column label="待评装备" min-width="180">
                        <template #default="{ row }">
                            <div class="table-equip-list">
                                <div v-for="e in (row.equipList || [])" :key="e" class="equip-dot">🛰️ {{ e }}</div>
                                <span v-if="!row.equipList || row.equipList.length === 0">-</span>
                            </div>
                        </template>
                    </el-table-column>

                    <el-table-column label="陪试装备" min-width="180">
                        <template #default="{ row }">
                            <div class="table-equip-list support">
                                <div v-for="e in (row.supportEquip || [])" :key="e" class="equip-dot">🤝 {{ e }}</div>
                                <span v-if="!row.supportEquip || row.supportEquip.length === 0">-</span>
                            </div>
                        </template>
                    </el-table-column>

                    <el-table-column label="评估类型" width="130" align="center">
                        <template #default="{ row }">
                            <span class="type-main-label">{{ getEvalTypeLabel(row.evaluationType) }}</span>
                        </template>
                    </el-table-column>

                    <el-table-column label="评估目标" min-width="180">
                        <template #default="{ row }">
                            <div class="goal-tags-vertical">
                                <span v-for="g in row.evaluationGoal" :key="g" class="mini-tag-blue">{{ g }}</span>
                                <span v-if="!row.evaluationGoal || row.evaluationGoal.length === 0">-</span>
                            </div>
                        </template>
                    </el-table-column>

                    <el-table-column label="评估目的" min-width="220">
                        <template #default="{ row }">
                            <div class="table-purpose-cell" :title="row.evaluationPurpose">
                                {{ row.evaluationPurpose || '-' }}
                            </div>
                        </template>
                    </el-table-column>

                    <el-table-column label="设计类型" width="120" align="center">
                        <template #default="{ row }">
                            <el-tag size="small" effect="plain" type="info">{{ getDesignTypeLabel(row.designType)
                            }}</el-tag>
                        </template>
                    </el-table-column>

                    <el-table-column label="试验类型" min-width="150">
                        <template #default="{ row }">
                            <div class="test-type-list">
                                <el-tag v-for="t in row.testType" :key="t" size="small" style="margin: 2px"
                                    effect="dark">{{ t }}</el-tag>
                                <span v-if="!row.testType || row.testType.length === 0">-</span>
                            </div>
                        </template>
                    </el-table-column>

                    <el-table-column label="指标体系" width="110" align="center">
                        <template #default="{ row }">
                            <span v-if="row.indicatorSystem || row.treeData" class="status-ok">● 已配置</span>
                            <span v-else class="status-none">○ 未配置</span>
                        </template>
                    </el-table-column>

                    <el-table-column label="完成进度" width="140">
                        <template #default="{ row }">
                            <div class="table-progress-cell slim">
                                <div class="rcp-bar">
                                    <div class="rcp-fill"
                                        :style="{ width: (row.completeness || 0) + '%', background: getStatusColor(row.status) }">
                                    </div>
                                </div>
                                <div class="rcp-text">
                                    {{ row.completeness || 0 }}%
                                </div>
                            </div>
                        </template>
                    </el-table-column>

                    <el-table-column label="更新时间" width="160" align="center">
                        <template #default="{ row }">
                            <span class="table-date">{{ row.reqUpdateTime || row.reqCreateTime || '-' }}</span>
                        </template>
                    </el-table-column>

                    <el-table-column label="操作" width="180" fixed="right" align="center">
                        <template #default="{ row }">
                            <div class="table-actions">
                                <el-link type="primary" :underline="false"
                                    @click.stop="openRequirement(row)">编辑</el-link>
                                <el-divider direction="vertical" />
                                <el-link type="danger" :underline="false" @click.stop="handleDelete(row)">删除</el-link>
                                <el-divider direction="vertical" />
                                <el-link type="success" :underline="false"
                                    @click.stop="issueRequirement(row)">下发</el-link>
                            </div>
                        </template>
                    </el-table-column>
                </el-table>

                <!-- Empty State -->
                <div class="empty-state" v-else-if="!loading">
                    <el-icon class="empty-icon">
                        <Document />
                    </el-icon>
                    <div class="empty-title">暂无匹配的评估需求</div>
                    <p class="empty-sub">尝试调整搜索词或筛选条件</p>
                </div>
            </div>

            <div class="pagination-container" v-if="total > 0">
                <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize"
                    :total="total" layout="total, prev, pager, next" @current-change="getList" background />
            </div>
        </main>

        <!-- Modals -->
        <!-- New Requirement Modal -->
        <el-dialog v-model="newReqDialog" title="新增评估需求" width="640px" custom-class="tech-dialog">
            <div class="create-opts">
                <div v-for="opt in createOptions" :key="opt.value" class="create-opt"
                    :class="{ selected: selectedCreateOpt === opt.value }" @click="selectedCreateOpt = opt.value">
                    <div class="co-icon" :style="{ background: opt.iconBg }">{{ opt.icon }}</div>
                    <div class="co-content">
                        <div class="co-title">{{ opt.title }}</div>
                        <div class="co-desc">{{ opt.description }}</div>
                    </div>
                </div>
            </div>
            <template #footer>
                <el-button @click="newReqDialog = false">取消</el-button>
                <el-button type="primary" @click="handleCreate">确定并进入编辑器 →</el-button>
            </template>
        </el-dialog>

        <!-- Import Modal -->
        <el-dialog v-model="importDialog" title="导入评估方案" width="680px" custom-class="tech-dialog">
            <div class="file-drop" @click="triggerUpload">
                <div class="file-drop-icon">📤</div>
                <div class="file-drop-title">点击上传 或 拖拽文件到此处</div>
                <div class="file-drop-sub">支持 Word (.docx) · PDF · Excel (.xlsx)<br>系统将自动解析并填充评估需求要素</div>
            </div>
            <div class="divider-text">或从方案库选择</div>
            <div class="import-plan-list">
                <div v-for="plan in importPlans" :key="plan.id" class="import-plan-item"
                    :class="{ sel: selectedPlanId === plan.id }" @click="selectedPlanId = plan.id">
                    <div class="ipi-icon">📄</div>
                    <div class="ipi-content">
                        <div class="ipi-name">{{ plan.name }}</div>
                        <div class="ipi-meta">来源：{{ plan.source }} · {{ plan.date }} · 关联骨架：{{ plan.skeleton }}</div>
                    </div>
                    <div class="ipi-check" v-if="selectedPlanId === plan.id">✓</div>
                </div>
            </div>
            <template #footer>
                <el-button @click="importDialog = false">取消</el-button>
                <el-button type="primary" @click="handleImport">解析并填充 →</el-button>
            </template>
        </el-dialog>

        <!-- Template Modal -->
        <el-dialog v-model="templateDialog" title="选择骨架模板" width="750px" custom-class="tech-dialog">
            <div class="tmpl-grid">
                <div v-for="tmpl in templates" :key="tmpl.id" class="tmpl-card"
                    :class="{ sel: selectedTmplId === tmpl.id }" @click="selectedTmplId = tmpl.id">
                    <div class="tc-icon">{{ tmpl.icon }}</div>
                    <div class="tc-name">{{ tmpl.name }}</div>
                    <div class="tc-desc">{{ tmpl.description }}</div>
                    <div class="tc-tags">
                        <el-tag size="small" type="info">{{ tmpl.tag1 }}</el-tag>
                        <el-tag size="small" type="success">{{ tmpl.tag2 }}</el-tag>
                    </div>
                </div>
            </div>
            <template #footer>
                <el-button @click="templateDialog = false">取消</el-button>
                <el-button type="primary" @click="handleLoadTemplate">加载模板并生成需求 →</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { Search, Document, Loading } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { listEvaluationRequirement, delEvaluationRequirement } from '@/api/requirements'
// import { useThemeStore } from '@/store/theme'

// const themeStore = useThemeStore()
const router = useRouter()

// --- State ---
const searchQuery = ref('')
const activeStatus = ref('all')
const activeLevel = ref('全部')
const loading = ref(false)

const newReqDialog = ref(false)
const importDialog = ref(false)
const templateDialog = ref(false)

const selectedCreateOpt = ref('blank')
const selectedPlanId = ref(null)
const selectedTmplId = ref(1)

const requirements = ref([])
const total = ref(0)
const queryParams = ref({
    pageNum: 1,
    pageSize: 10
})

// --- Options ---
const statusOptions = [
    { label: '全部需求', value: 'all', icon: '📄' },
    { label: '草稿', value: 'draft', icon: '✏️' },
    { label: '已发布', value: 'published', icon: '✅' }
]

const levelOptions = ['全部', '装备级', '系统级', '体系级']

const createOptions = [
    {
        title: '从空白页面新建',
        value: 'blank',
        icon: '📝',
        iconBg: 'linear-gradient(135deg,rgba(255,107,53,.2),rgba(255,107,53,.05))',
        description: '不依赖任何模板，从零开始填写所有评估需求要素。适合全新类型的评估任务。'
    },
    {
        title: '导入评估方案填充生成',
        value: 'import',
        icon: '📥',
        iconBg: 'linear-gradient(135deg,rgba(47,129,247,.25),rgba(47,129,247,.1))',
        description: '上传或选择已有的评估方案文档（Word/PDF），系统自动解析并预填充评估目标、指标等要素。'
    },
    {
        title: '加载骨架模板',
        value: 'template',
        icon: '📋',
        iconBg: 'linear-gradient(135deg,rgba(188,140,255,.2),rgba(188,140,255,.05))',
        description: '从骨架库中选择一个任务骨架模板，系统自动预生成结构化需求框架。'
    }
]

const importPlans = [
    { id: 'ipi-1', name: '卫星通信干扰装备外场效能试验方案', source: '任务书', date: '2026-01', skeleton: '干扰效能评估骨架 v1.0' },
    { id: 'ipi-2', name: '某型跳频电台抗干扰性能试验方案', source: '研制总要求', date: '2025-12', skeleton: '抗干扰骨架 v1.0' },
    { id: 'ipi-3', name: '通信对抗体系联合试验评估方案', source: '体系规划文件', date: '2025-11', skeleton: '体系贡献率骨架 v1.3' },
    { id: 'ipi-4', name: '雷达对抗侦察装备定型试验方案', source: '定型任务书', date: '2025-10', skeleton: '无' }
]

const templates = [
    { id: 1, name: '卫星通信干扰装备效能评估骨架', icon: '🔫', description: '4个能力域 · 26项指标 · 含跳频对抗专项。支持度 73%。', tag1: '通信干扰', tag2: '装备级' },
    { id: 2, name: '通信侦察装备效能评估骨架', icon: ' telescope', description: '3个能力域 · 18项指标。支持度 81%。', tag1: '侦察能力', tag2: '装备级' },
    { id: 3, name: '通信装备抗干扰能力评估骨架', icon: '🛡️', description: '3个能力域 · 22项指标 · 含跳频/扩频抗干扰。支持度 65%。', tag1: '抗干扰', tag2: '系统级' },
    { id: 4, name: '通信对抗装备体系贡献率评估骨架', icon: '📊', description: '5个贡献域 · 31项指标 · 仿真与外场联合。支持度 58%。', tag1: '体系贡献率', tag2: '体系级' }
]

// --- Methods ---
const getStatusLabel = (status) => {
    const map = { '1': '草稿', '2': '已发布' }
    return map[status] || '未知'
}

const getDesignTypeLabel = (type) => {
    const map = { '1': '独立设计', '2': '协同设计' }
    return map[type] || '未知'
}

const getEvalTypeLabel = (type) => {
    const map = { '1': '效能评估', '2': '体系贡献率', '3': '能力成熟度', '4': '作战适用性' }
    return map[String(type)] || '未知'
}

const getLevelLabel = (level) => {
    const map = { '1': '装备级', '2': '系统级', '3': '体系级', '4': '任务级' }
    return map[level] || '未知'
}

const getStatusColor = (status) => {
    switch (status) {
        case '1': return '#d29922' // Draft - Orange
        case '2': return '#3fb950' // Published - Green
        default: return '#7d8590'
    }
}

const getStatusTagType = (status) => {
    switch (status) {
        case '1': return 'warning'
        case '2': return 'success'
        default: return 'info'
    }
}

const getList = async () => {
    loading.value = true
    try {
        const statusMap = { 'all': undefined, 'draft': '1', 'published': '2' }
        const params = {
            pageNum: queryParams.value.pageNum,
            pageSize: queryParams.value.pageSize,
            requirementName: searchQuery.value || undefined,
            status: statusMap[activeStatus.value]
        }
        const res = await listEvaluationRequirement(params)
        if (res.code === 200 && res.data) {
            requirements.value = res.data.records || []
            total.value = res.data.total || 0
        }
    } catch (err) {
        console.error('Fetch error:', err)
    } finally {
        loading.value = false
    }
}

const openRequirement = (row) => {
    router.push({
        path: '/major/requirement-sys/generate',
        query: { id: row.requirementId }
    })
}

const handleDelete = (row) => {
    ElMessageBox.confirm(`确定要彻底删除需求「${row.projectName}」吗？一旦删除不可恢复。`, '危险操作', {
        type: 'error',
        confirmButtonClass: 'el-button--danger',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
    }).then(async () => {
        try {
            const res = await delEvaluationRequirement(row.requirementId)
            if (res.code === 200) {
                ElMessage.success('需求已删除')
                getList()
            }
        } catch (err) {
            ElMessage.error('删除操作失败')
        }
    }).catch(() => { })
}

const issueRequirement = (row) => {
    ElMessage.success(`需求「${row.requirementName}」已下发至分中心进行协同会签`)
    router.push('/major/requirement-sys/distribute')
}

const handleCreate = () => {
    newReqDialog.value = false
    if (selectedCreateOpt.value === 'blank') {
        router.push({
            path: '/major/requirement-sys/generate',
            query: { mode: 'blank' }
        })
    } else if (selectedCreateOpt.value === 'import') {
        importDialog.value = true
    } else if (selectedCreateOpt.value === 'template') {
        templateDialog.value = true
    }
}

const triggerUpload = () => {
    ElMessage.info('触发文件选择器... (Mock)')
}

const handleImport = () => {
    if (!selectedPlanId.value) {
        ElMessage.warning('请选择一个要导入的评估方案')
        return
    }
    ElMessage.info('AI 引擎正在解析方案并填充需求要素...')
    setTimeout(() => {
        router.push('/major/requirement-sys/generate')
    }, 1500)
}

const handleLoadTemplate = () => {
    ElMessage.info('正在加载骨架模板并生成需求初始框架...')
    setTimeout(() => {
        router.push('/major/requirement-sys/generate')
    }, 1200)
}

const filteredRequirements = computed(() => {
    // 依然保留前端对 Level 的筛选，因为后端接口可能没提供这个过滤参数
    return requirements.value.filter(req => {
        const matchLevel = activeLevel.value === '全部' || req.level === activeLevel.value
        return matchLevel
    })
})

const getStatusCount = (status) => {
    if (status === 'all') return total.value
    return requirements.value.filter(req => req.status === status).length
}

// Watchers
watch([activeStatus, activeLevel, searchQuery], () => {
    handleQuery()
})

onMounted(() => {
    getList()
})
</script>

<style scoped lang="scss">
.requirement-management {
    display: flex;
    height: 100%;
    background: var(--bg-color); 
    color: var(--text-color-primary);
    overflow: hidden;
    position: relative;
}

// Side Sidebar
.left-sidebar {
    width: 220px;
    background: var(--sider-bg-color);
    // [MODIFIED] Use CSS variable
    border-right: 1px solid var(--border-color);
    padding: 20px 15px;
    display: flex;
    flex-direction: column;
    gap: 5px;

    .sb-lbl {
        font-size: 11px;
        font-weight: 700;
        color: var(--text-color-secondary);
        text-transform: uppercase;
        letter-spacing: 1px;
        padding: 10px 10px 5px;
    }

    .sb-item {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 10px 12px;
        border-radius: 6px;
        cursor: pointer;
        font-size: 13px;
        color: var(--text-color-secondary);
        transition: all 0.2s;
        border: 1px solid transparent;

        &:hover {
            background: var(--card-bg-color);
            color: var(--text-color-primary);
        }

        &.active {
            background: color-mix(in srgb, var(--primary-color) 10%, transparent);
            color: var(--primary-color);
            border-color: color-mix(in srgb, var(--primary-color) 20%, transparent);
        }

        .sb-count {
            margin-left: auto;
            font-size: 11px;
            background: var(--card-bg-color);
            padding: 1px 7px;
            border-radius: 10px;
        }
    }
}

// Main Content
.main-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

// Filter Bar
.filter-bar {
    background: var(--sider-bg-color);
    border-bottom: 1px solid var(--border-color);
    padding: 12px 25px;
    display: flex;
    align-items: center;
    gap: 15px;
    flex-shrink: 0;

    .search-box {
        background: var(--card-bg-color);
        // [MODIFIED] Use border color
        border: 1px solid var(--border-color);
        border-radius: 6px;
        padding: 6px 12px;
        display: flex;
        align-items: center;
        gap: 8px;
        flex: 1;
        max-width: 350px;

        .el-icon {
            color: var(--text-color-secondary);
        }

        input {
            background: transparent;
            border: none;
            outline: none;
            color: var(--text-color-primary);
            width: 100%;
            font-size: 13px;
        }
    }

    .filter-tags {
        display: flex;
        gap: 8px;

        .filter-tag {
            font-size: 12px;
            padding: 4px 12px;
            border-radius: 4px;
            border: 1px solid var(--border-color);
            color: var(--text-color-secondary);
            cursor: pointer;
            transition: all 0.2s;

            &:hover {
                border-color: var(--text-color-secondary);
                color: var(--text-color-primary);
            }

            &.on {
                background: color-mix(in srgb, var(--primary-color) 15%, transparent);
                border-color: color-mix(in srgb, var(--primary-color) 30%, transparent);
                color: var(--primary-color);
            }
        }
    }

    .actions {
        margin-left: auto;
        display: flex;
        gap: 10px;
    }
}

// Requirement Table
.req-table-container {
    flex: 1;
    padding: 20px 25px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
}

.tech-table {
    background: transparent !important;
    --el-table-bg-color: transparent;
    --el-table-tr-bg-color: transparent;
    --el-table-header-bg-color: var(--sider-bg-color);
    --el-table-header-text-color: #7d8590;
    --el-table-row-hover-bg-color: var(--primary-color);
    --el-table-border-color: var(--border-color);
    --el-table-text-color: var(--text-color-primary);

    &::before {
        display: none;
    }

    :deep(.el-table__inner-wrapper) {
        &::after {
            display: none;
        }
    }

    :deep(.el-table__header-wrapper) {
        th {
            font-size: 13px;
            font-weight: 700;
            border-bottom: 2px solid var(--border-color);
        }
    }

    :deep(.el-table__row) {
        cursor: pointer;
        transition: all 0.2s;

        td {
            border-bottom: 1px solid var(--card-bg-color);
            padding: 12px 0;
        }

        &:hover {
            transform: scale(1.002);
            box-shadow: 0 0 15px rgba(0, 0, 0, 0.2);
            // background-color: var(--primary-color);
        }
    }

    .id-badge {
        font-family: 'Outfit', sans-serif;
        background: #00f2ff23;
        color: var(--text-color-primary);
        padding: 2px 8px;
        border-radius: 4px;
        border: 1px solid rgba(0, 243, 255, 0.15);
        font-size: 11px;
        font-weight: 600;
    }

    .table-title-text {
        font-size: 14px;
        font-weight: 700;
        color: var(--text-color-primary);
        letter-spacing: 0.3px;
    }

    .table-purpose-cell {
        font-size: 12px;
        color: #8b949e;
        line-height: 1.6;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        text-align: left;
    }

    .table-equip-list {
        display: flex;
        flex-direction: column;
        gap: 4px;
        font-size: 12px;
        color: var(--text-color-primary);

        .equip-dot {
            display: flex;
            align-items: center;
            gap: 6px;
        }

        &.support {
            color: #8b949e;
        }
    }

    .type-main-label {
        font-size: 13px;
        font-weight: 700;
        color: #bc8cff;
    }

    .goal-tags-vertical {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;

        .mini-tag-blue {
            font-size: 10px;
            padding: 1px 8px;
            border-radius: 4px;
            background: rgba(47, 129, 247, 0.1);
            color: #2f81f7;
            border: 1px solid rgba(47, 129, 247, 0.2);
        }
    }

    .test-type-list {
        display: flex;
        flex-wrap: wrap;
        gap: 4px;
    }

    .status-ok {
        color: #3fb950;
        font-size: 12px;
        font-weight: 600;
    }

    .status-none {
        color: #7d8590;
        font-size: 12px;
    }

    .table-progress-cell {
        &.slim {
            .rcp-text {
                font-size: 11px;
                text-align: right;
                margin-top: 2px;
            }
        }

        .rcp-bar {
            width: 100%;
            height: 4px;
            background: var(--border-color);
            border-radius: 2px;
            overflow: hidden;
            margin-bottom: 6px;

            .rcp-fill {
                height: 100%;
                border-radius: 2px;
                transition: width 0.5s ease;
            }
        }

        .rcp-text {
            font-size: 11px;
            font-weight: 600;
        }
    }

    .table-date {
        font-size: 12px;
        color: #586069;
        font-family: 'JetBrains Mono', monospace;
    }

    .table-actions {
        display: flex;
        align-items: center;
        justify-content: center;

    }
}

.pagination-container {
    padding: 15px 25px;
    display: flex;
    justify-content: flex-end;
    border-top: 1px solid var(--border-color);
    background: var(--sider-bg-color);
}

// Empty State
.empty-state {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #7d8590;

    .empty-icon {
        font-size: 60px;
        margin-bottom: 15px;
        opacity: 0.3;
    }

    .empty-title {
        font-size: 18px;
        font-weight: 600;
        margin-bottom: 8px;
    }

    .empty-sub {
        font-size: 14px;
    }
}

// Modal Styles
.create-opts {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-bottom: 20px;

    .create-opt {
        display: flex;
        align-items: flex-start;
        gap: 15px;
        padding: 16px;
        background: #1c2330;
        border: 2px solid var(--border-color);
        border-radius: 10px;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
            border-color: #7d8590;
            background: var(--card-bg-color);
        }

        &.selected {
            border-color: var(--primary-color);
            background: rgba(255, 107, 53, 0.05);
        }

        .co-icon {
            width: 44px;
            height: 44px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 24px;
            flex-shrink: 0;
        }

        .co-content {
            flex: 1;

            .co-title {
                font-size: 15px;
                font-weight: 700;
                margin-bottom: 4px;
                color: var(--text-color-primary);
            }

            .co-desc {
                font-size: 12px;
                color: #7d8590;
                line-height: 1.5;
            }
        }
    }
}

.import-plan-list {
    max-height: 300px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin: 15px 0;

    .import-plan-item {
        display: flex;
        align-items: center;
        padding: 12px;
        background: #1c2330;
        border: 1px solid var(--border-color);
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
            border-color: #7d8590;
        }

        &.sel {
            border-color: #2f81f7;
            background: rgba(47, 129, 247, 0.1);
        }

        .ipi-icon {
            font-size: 20px;
            margin-right: 12px;
        }

        .ipi-content {
            flex: 1;

            .ipi-name {
                font-size: 13px;
                font-weight: 600;
                margin-bottom: 2px;
            }

            .ipi-meta {
                font-size: 11px;
                color: #7d8590;
            }
        }

        .ipi-check {
            color: #2f81f7;
            font-weight: bold;
        }
    }
}

.file-drop {
    border: 2px dashed var(--border-color);
    border-radius: 10px;
    padding: 30px;
    text-align: center;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
        border-color: #2f81f7;
        background: rgba(47, 129, 247, 0.05);
    }

    .file-drop-icon {
        font-size: 40px;
        margin-bottom: 10px;
    }

    .file-drop-title {
        font-size: 14px;
        font-weight: 600;
        margin-bottom: 5px;
    }

    .file-drop-sub {
        font-size: 12px;
        color: #7d8590;
        line-height: 1.5;
    }
}

.divider-text {
    display: flex;
    align-items: center;
    color: #586069;
    font-size: 11px;
    text-transform: uppercase;
    font-weight: bold;
    margin: 20px 0;

    &::before,
    &::after {
        content: "";
        flex: 1;
        height: 1px;
        background: var(--border-color);
        margin: 0 15px;
    }
}

.tmpl-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 15px;
    padding: 5px;
}

.tmpl-card {
    background: #1c2330;
    border: 2px solid var(--border-color);
    border-radius: 10px;
    padding: 16px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
        border-color: #7d8590;
        transform: translateY(-2px);
    }

    &.sel {
        border-color: #bc8cff;
        background: rgba(188, 140, 255, 0.05);
    }

    .tc-icon {
        font-size: 24px;
        margin-bottom: 10px;
    }

    .tc-name {
        font-size: 14px;
        font-weight: 700;
        margin-bottom: 6px;
        color: var(--text-color-primary);
    }

    .tc-desc {
        font-size: 12px;
        color: #7d8590;
        line-height: 1.4;
        margin-bottom: 10px;
        height: 34px;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }

    .tc-tags {
        display: flex;
        gap: 6px;
    }
}
</style>
