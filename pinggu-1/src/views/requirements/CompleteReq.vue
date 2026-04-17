<template>
<div class="complete-req-container">
    <div class="page-header">
        <h2 class="title">完整评估需求文档</h2>
        <div class="subtitle">经过人工判定、协同汇集的最终评估需求基线。内容可导出并对接后续环节进行完备度分析或转入实验流转。</div>
    </div>

    <!-- 顶部操作区 -->
    <div class="action-bar flex-between">
        <div class="left-actions">
            <span class="status-label">当前版本基线: <span class="cyber-highlight">v1.2_RELEASE (已签名发布)</span></span>
        </div>
        <div class="right-actions">
            <el-button type="primary" class="cyber-btn" @click="goToAnalysis">
                <el-icon><DataAnalysis /></el-icon> 进行完备度分析
            </el-button>
            <el-button type="success" plain @click="exportDoc">
                <el-icon><Download /></el-icon> 导出需求大纲 (Word)
            </el-button>
        </div>
    </div>

    <!-- 面板区 -->
    <div class="dashboard-panels">
        <!-- 侧边导航 -->
        <div class="panel-left">
            <div class="cyber-card fill-height">
                <div class="card-header">需求文档目录</div>
                <div class="card-body p-0">
                    <el-menu default-active="1" class="cyber-menu" @select="handleSelect">
                        <el-menu-item index="1"><el-icon><Monitor /></el-icon><span>1. 待评装备清单</span></el-menu-item>
                        <el-menu-item index="2"><el-icon><Aim /></el-icon><span>2. 评估指标体系</span></el-menu-item>
                        <el-menu-item index="3"><el-icon><Opportunity /></el-icon><span>3. 评估方法准则</span></el-menu-item>
                        <el-menu-item index="4"><el-icon><Setting /></el-icon><span>4. 算子与评估模型</span></el-menu-item>
                        <el-menu-item index="5"><el-icon><Grid /></el-icon><span>5. 数据资源及结构</span></el-menu-item>
                    </el-menu>
                </div>
            </div>
        </div>

        <!-- 详细内容区 -->
        <div class="panel-right">
            <div class="cyber-card fill-height">
                <div class="card-header flex-header">
                    <span>{{ currentSectionTitle }}</span>
                    <el-tag type="success" effect="dark" size="small">置信度: 100% (已通过审查)</el-tag>
                </div>
                <div class="card-body">
                    <!-- 模拟文档内容呈现 -->
                    <div class="mock-doc-content" v-if="currentSection === '1'">
                        <h4>表 1.1 - 组网装备参试节点说明</h4>
                        <el-table :data="mockEquips" class="cyber-table" border size="small">
                            <el-table-column prop="id" label="节点代号" width="100"/>
                            <el-table-column prop="type" label="装备类型" width="120"/>
                            <el-table-column prop="name" label="具体型号及说明" />
                            <el-table-column prop="qty" label="测试预置数量" width="120" align="center"/>
                        </el-table>
                    </div>

                    <div class="mock-doc-content" v-if="currentSection === '2'">
                        <h4>图 2.1 - 通信对抗指标体系树状图</h4>
                        <div class="tree-container">
                            <el-tree
                                :data="mockIndicators"
                                :props="defaultProps"
                                default-expand-all
                                class="cyber-tree"
                            />
                        </div>
                    </div>

                    <div class="mock-doc-content" v-if="['3','4','5'].includes(currentSection)">
                        <el-empty :image-size="120" description="基线结构化详要展开中..." class="cyber-empty" />
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { DataAnalysis, Download, Monitor, Aim, Opportunity, Setting, Grid } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const currentSection = ref('1')

const titles = {
    '1': '待评装备清单',
    '2': '评估指标体系',
    '3': '评估方法准则',
    '4': '算子与评估模型',
    '5': '数据资源及结构'
}

const currentSectionTitle = computed(() => titles[currentSection.value])

const handleSelect = (index) => {
    currentSection.value = index
}

const setNode = (id, type, name, qty) => ({ id, type, name, qty })
const mockEquips = ref([
    setNode('N-01', '指挥平台', '某型综合协同指挥车(带数据链模组)', 1),
    setNode('N-02', '通信终端', '某便携宽带自组网终端', 15),
    setNode('N-03', '干扰吊舱', '远端随动注入式干扰源', 2),
    setNode('N-04', '侦察雷达', 'X波段地面机动雷达', 1)
])

const mockIndicators = ref([
    {
        label: '综合抗干扰效能指标',
        children: [
            {
                label: '网络连通性',
                children: [
                    { label: '自组网入网时间 (要求 < 3s)' },
                    { label: '链路保持率 (要求 > 99%)' }
                ]
            },
            {
                label: '抗欺骗能力',
                children: [
                    { label: '目标虚警排解率' },
                    { label: '正确指令传导率' }
                ]
            }
        ]
    }
])
const defaultProps = { children: 'children', label: 'label' }

const goToAnalysis = () => {
    router.push('/requirement-sys/completeness')
}

const exportDoc = () => {
    ElMessage.success('正在生成Word基线报告，请稍候...')
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.complete-req-container {
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

.action-bar {
    margin-bottom: 20px;
    padding: 15px 20px;
    background: rgba(13, 20, 31, 0.6);
    border: 1px solid rgba(0, 242, 255, 0.2);
    border-radius: 8px;
    flex-shrink: 0;
    
    &.flex-between {
        display: flex;
        justify-content: space-between;
        align-items: center;
    }

    .status-label {
        font-size: 14px;
        color: #8fa3b8;
    }
    
    .cyber-highlight {
        color: #00f2ff;
        font-weight: bold;
        text-shadow: 0 0 8px rgba(0, 242, 255, 0.5);
        margin-left: 10px;
        background: rgba(0, 242, 255, 0.1);
        padding: 4px 10px;
        border-radius: 4px;
        border: 1px solid rgba(0, 242, 255, 0.3);
    }
}

.dashboard-panels {
    display: flex;
    gap: 20px;
    flex-grow: 1;
    min-height: 0;
}

.panel-left {
    width: 250px;
    flex-shrink: 0;
}

.panel-right {
    flex-grow: 1;
    min-width: 0;
}

.cyber-card {
    background: rgba(13, 20, 31, 0.6);
    border: 1px solid rgba(0, 242, 255, 0.2);
    border-radius: 6px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    
    &.fill-height {
        height: 100%;
    }

    .card-header {
        padding: 15px 20px;
        background: rgba(0, 242, 255, 0.05);
        border-bottom: 1px solid rgba(0, 242, 255, 0.1);
        color: #00f2ff;
        font-weight: bold;
        letter-spacing: 1px;
        
        &.flex-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
    }

    .card-body {
        padding: 20px;
        flex-grow: 1;
        overflow-y: auto;
        
        &.p-0 { padding: 0; }
    }
}

/* Base Cyper UI Overrides */
.cyber-btn {
    background: rgba(0, 242, 255, 0.1);
    border: 1px solid #00f2ff;
    color: #00f2ff;
    box-shadow: 0 0 10px rgba(0, 242, 255, 0.2);
    
    &:hover {
        background: rgba(0, 242, 255, 0.2);
        box-shadow: 0 0 20px rgba(0, 242, 255, 0.4);
    }
}

:deep(.cyber-menu) {
    border-right: none;
    background: transparent;

    .el-menu-item {
        color: #8fa3b8;
        &:hover, &.is-active {
            background: rgba(0, 242, 255, 0.08);
            color: #00f2ff;
        }
        &.is-active {
            border-left: 3px solid #00f2ff;
            font-weight: bold;
        }
        .el-icon { margin-right: 15px; }
    }
}

:deep(.cyber-table) {
    background-color: transparent !important;
    --el-table-border-color: rgba(0, 242, 255, 0.1);
    --el-table-header-bg-color: rgba(6, 13, 23, 0.8);
    --el-table-header-text-color: #00f2ff;
    --el-table-row-hover-bg-color: rgba(0, 242, 255, 0.05);

    tr { background-color: transparent !important; }
    th.el-table__cell { font-weight: bold; }
    
    .el-table__inner-wrapper::before, .el-table__border-left-patch, .el-table__border-bottom-patch {
        background-color: transparent;
    }
}

:deep(.cyber-tree) {
    background: transparent;
    color: #e6edf3;
    
    .el-tree-node__content {
        height: 38px;
        &:hover {
            background-color: rgba(0, 242, 255, 0.1);
        }
    }
    
    .el-tree-node:focus > .el-tree-node__content {
        background-color: rgba(0, 242, 255, 0.15);
    }
}
.tree-container {
    padding: 15px;
    border: 1px solid rgba(0, 242, 255, 0.15);
    background: rgba(6, 13, 23, 0.5);
    border-radius: 4px;
}

:deep(.cyber-empty) {
    .el-empty__description p {
        color: #5c7080;
    }
}

h4 {
    color: #fff;
    font-size: 15px;
    margin-bottom: 15px;
    font-weight: 500;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}
</style>
