<template>
    <div class="subpage-container">
        <div class="page-header" v-if="!isEditing">
            <div class="left">
                <h3 class="page-title">预处理流程模板</h3>
            </div>
            <div class="right">
                <el-input v-model="searchQuery" placeholder="搜索模板名称" clearable style="width: 240px; margin-right: 12px;"
                    prefix-icon="Search" />
                <el-button type="primary" icon="Plus" @click="startNewTemplate">创建新模板</el-button>
            </div>
        </div>

        <!-- Template List View -->
        <div class="template-grid" v-if="!isEditing">
            <el-row :gutter="20">
                <el-col :span="8" v-for="tpl in templates" :key="tpl.id">
                    <el-card class="tech-card dark-card">
                        <template #header>
                            <div class="card-header">
                                <span class="tpl-name">{{ tpl.name }}</span>
                                <el-tag size="small" effect="dark" type="info">{{ tpl.operatorCount }} 算子</el-tag>
                            </div>
                        </template>
                        <div class="card-body">
                            <div class="sequence-preview">
                                <div v-for="(op, idx) in tpl.previewOps" :key="idx" class="preview-node">
                                    <span class="dot"></span>
                                    <span class="label">{{ op }}</span>
                                    <el-icon v-if="idx < tpl.previewOps.length - 1">
                                        <ArrowRight />
                                    </el-icon>
                                </div>
                            </div>
                            <div class="meta-info">
                                <span>创建者: {{ tpl.creator }}</span>
                                <span>使用次数: {{ tpl.useCount }}</span>
                            </div>
                        </div>
                        <div class="card-footer">
                            <el-button link type="primary" icon="Edit" @click="editTemplate(tpl)">设计流程</el-button>
                            <el-divider direction="vertical" />
                            <el-button link type="primary" icon="CopyDocument">复制</el-button>
                            <el-button link type="danger" icon="Delete">删除</el-button>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </div>

        <!-- Visual Workflow Editor Overlay -->
        <div v-if="isEditing" class="workflow-editor-overlay">
            <div class="editor-header">
                <el-button icon="Back" circle @click="isEditing = false" />
                <div class="header-info">
                    <span class="title">流程模板设计 - {{ activeTemplate.name || '未命名模板' }}</span>
                    <el-tag size="small" type="success" effect="plain" v-if="isAutoSaving">自动保存中...</el-tag>
                </div>
                <div class="spacer"></div>
                <el-button type="primary" plain icon="VideoPlay" @click="runSimulation">模拟运行</el-button>
                <el-button type="danger" plain icon="Delete" @click="deleteSelected" :disabled="!hasSelection">
                    删除选中项
                </el-button>
                <el-button type="primary" icon="Check" @click="saveAndExit">完成设计并同步</el-button>
            </div>

            <div class="editor-main">
                <!-- Operator Library Sidebar -->
                <aside class="op-library">
                    <div class="library-header">算子仓库</div>
                    <div class="library-search">
                        <el-input size="small" v-model="libSearch" placeholder="搜索算子..." prefix-icon="Search" />
                    </div>
                    <el-scrollbar class="lib-items">
                        <div v-for="op in operatorLibrary" :key="op.name" class="lib-item" draggable="true"
                            @dragstart="onDragStart($event, op)">
                            <div class="op-icon" :style="{ color: op.color }">
                                <el-icon>
                                    <Cpu />
                                </el-icon>
                            </div>
                            <div class="op-labels">
                                <span class="op-n">{{ op.name }}</span>
                                <span class="op-t">{{ op.type }}</span>
                            </div>
                        </div>
                    </el-scrollbar>
                </aside>

                <!-- Canvas -->
                <div class="canvas-container" @drop="onDrop" @dragover="onDragOver">
                    <VueFlow :fit-view-on-init="true" class="workflow-flow">
                        <Background pattern-color="#00f2ff" :gap="20" />
                        <Controls />
                        <MiniMap />
                    </VueFlow>
                </div>

                <!-- Properties Panel -->
                <aside class="prop-panel">
                    <div class="panel-header">算子实例配置</div>
                    <div v-if="selectedNode" class="panel-body">
                        <el-form label-position="top">
                            <el-form-item label="实例名称">
                                <el-input v-model="selectedNode.label" />
                            </el-form-item>
                            <el-divider>算法参数</el-divider>
                            <el-form-item label="阈值设定">
                                <el-slider v-model="nodeProps.threshold" :max="100" />
                            </el-form-item>
                            <el-form-item label="执行超时 (ms)">
                                <el-input-number v-model="nodeProps.timeout" :step="100" />
                            </el-form-item>
                            <el-form-item label="并行线程数">
                                <el-radio-group v-model="nodeProps.threads">
                                    <el-radio :label="1">1</el-radio>
                                    <el-radio :label="4">4</el-radio>
                                    <el-radio :label="8">8</el-radio>
                                </el-radio-group>
                            </el-form-item>
                        </el-form>
                    </div>
                    <el-empty v-else description="选中画布算子进行配置" />
                </aside>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import {
    Search, Plus, ArrowRight, Edit, Delete, CopyDocument, Back,
    Cpu, VideoPlay, Check
} from '@element-plus/icons-vue'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import { ElMessage } from 'element-plus'

// Styles for Flow
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'

// Use composable for better control and reactivity
const {
    addNodes, addEdges, nodes, edges, onConnect, project,
    onNodeClick, onPaneClick, removeNodes, removeEdges,
    onEdgeClick, getSelectedNodes, getSelectedEdges
} = useVueFlow()

const searchQuery = ref('')
const isEditing = ref(false)
const isAutoSaving = ref(false)
const libSearch = ref('')
const activeTemplate = ref({})

// Selection & Props
const selectedNode = ref(null)
const selectedEdge = ref(null)
const nodeProps = reactive({ threshold: 50, timeout: 500, threads: 1 })

const hasSelection = computed(() => getSelectedNodes.value.length > 0 || getSelectedEdges.value.length > 0)

const templates = ref([
    { id: 1, name: '星地测控链路信号抗干扰清洗流', operatorCount: 4, creator: 'admin', useCount: 245, previewOps: ['信号抽取', '带通滤波', '信噪比分析', '纯净存底'] },
    { id: 2, name: '空间站运行遥测包智能解译模板', operatorCount: 3, creator: 'researcher_1', useCount: 120, previewOps: ['帧同步与包解析', '协议字典匹配', '物理量转换入库'] },
    { id: 3, name: '高分卫星多光谱遥感影像预处理', operatorCount: 5, creator: 'admin', useCount: 68, previewOps: ['辐射定标', '大气校正', '几何精纠正', '波段融合', '金字塔瓦片化'] },
])

const operatorLibrary = [
    { name: '遥测断点插值补全', type: '数据清洗', color: '#00f2ff' },
    { name: '星体姿态震荡检测', type: '异常检测', color: '#f56c6c' },
    { id: 3, name: '空间坐标系转换矩阵', type: '特征工程', color: '#67c23a' },
    { name: '多载荷遥测通道合并', type: '数据整合', color: '#e6a23c' },
    { name: '轨道平滑滤波', type: '平滑加工', color: '#00f2ff' },
]

onNodeClick((e) => {
    selectedNode.value = e.node
})

onEdgeClick((e) => {
    selectedEdge.value = e.edge
    selectedNode.value = null
})

onConnect((params) => {
    addEdges([{
        ...params,
        updatable: true,
        selectable: true,
        animated: true,
        style: { stroke: '#00f2ff', strokeWidth: 2 }
    }])
})

const deleteSelected = () => {
    removeNodes(getSelectedNodes.value)
    removeEdges(getSelectedEdges.value)
    selectedNode.value = null
    selectedEdge.value = null
}

const startNewTemplate = () => {
    activeTemplate.value = { name: '新建预处理流程' }
    isEditing.value = true
    // Properly clear existing nodes/edges
    nodes.value = []
    edges.value = []
}

const editTemplate = (tpl) => {
    activeTemplate.value = { ...tpl }
    isEditing.value = true

    // Clear and then add initial mock data to ensure internal store sync
    nodes.value = []
    edges.value = []

    setTimeout(() => {
        addNodes([
            { id: '1', type: 'input', label: '全周期遥测包输入', position: { x: 50, y: 150 }, style: { border: '1px solid #00f2ff', background: '#1a2635', color: '#fff' } },
            { id: '2', label: '空间单粒子翻转剔除', position: { x: 300, y: 150 }, style: { border: '1px solid #00f2ff', background: '#1a2635', color: '#fff' } },
            { id: '3', type: 'output', label: '飞行参数时间序列', position: { x: 550, y: 150 }, style: { border: '1px solid #00f2ff', background: '#1a2635', color: '#fff' } },
        ])
        addEdges([
            { id: 'e1-2', source: '1', target: '2', animated: true },
            { id: 'e2-3', source: '2', target: '3' },
        ])
    }, 50)
}

const onDragStart = (event, op) => {
    if (event.dataTransfer) {
        event.dataTransfer.setData('application/vueflow', JSON.stringify(op))
        event.dataTransfer.effectAllowed = 'move'
    }
}

const onDragOver = (event) => {
    event.preventDefault()
}

const onDrop = (event) => {
    const data = event.dataTransfer?.getData('application/vueflow')
    if (!data) return

    const opData = JSON.parse(data)

    // Use project to transform client coordinates to flow coordinates correctly
    const position = project({
        x: event.clientX - 60, // Offset for node center roughly
        y: event.clientY - 20
    })

    const newNode = {
        id: `op_${Date.now()}`,
        label: opData.name,
        type: opData.type === '数据校验' ? 'input' : (opData.type === '数据整合' ? 'output' : 'default'),
        position,
        class: `node-${opData.type === '数据校验' ? 'input' : (opData.type === '数据整合' ? 'output' : 'process')}`,
        data: { type: opData.type }
    }
    addNodes([newNode])
}

const saveAndExit = () => {
    ElMessage.success('模板配置已同步至算子集')
    isEditing.value = false
}

const runSimulation = () => {
    ElMessage({
        message: '正在进行流程逻辑校验与模拟运行...',
        type: 'info',
        duration: 3000
    })
}
</script>

<style scoped lang="scss">
.subpage-container {
    padding: 24px;
    position: relative;
    min-height: calc(100vh - 70px);
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
    margin-bottom: 20px;
    transition: all 0.3s;

    &:hover {
        border-color: #00f2ff;
        box-shadow: 0 0 15px rgba(0, 242, 255, 0.1);
    }

    .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .tpl-name {
            color: #fff;
            font-weight: bold;
        }
    }

    .sequence-preview {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 15px 0;
        overflow-x: auto;
        padding-bottom: 8px;

        .preview-node {
            display: flex;
            align-items: center;
            gap: 6px;
            white-space: nowrap;

            .dot {
                width: 6px;
                height: 6px;
                border-radius: 50%;
                background: #00f2ff;
            }

            .label {
                font-size: 12px;
                color: #8fa3b8;
            }

            .el-icon {
                color: #404b5a;
                font-size: 14px;
            }
        }
    }

    .meta-info {
        display: flex;
        justify-content: space-between;
        font-size: 12px;
        color: #404b5a;
        margin-top: 20px;
    }

    .card-footer {
        border-top: 1px solid rgba(255, 255, 255, 0.05);
        padding: 12px 0 0 0;
        display: flex;
        justify-content: space-around;
    }
}

.workflow-editor-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: #02060c;
    z-index: 1000;
    display: flex;
    flex-direction: column;

    .editor-header {
        height: 70px;
        background: rgba(10, 21, 37, 0.95);
        border-bottom: 1px solid rgba(0, 242, 255, 0.2);
        display: flex;
        align-items: center;
        padding: 0 30px;
        gap: 20px;

        .header-info {
            display: flex;
            align-items: center;
            gap: 15px;

            .title {
                font-size: 18px;
                color: #fff;
                font-weight: bold;
            }
        }

        .spacer {
            flex: 1;
        }
    }

    .editor-main {
        flex: 1;
        display: flex;
        overflow: hidden;
    }
}

.op-library {
    width: 260px;
    border-right: 1px solid rgba(255, 255, 255, 0.05);
    display: flex;
    flex-direction: column;

    .library-header {
        padding: 20px;
        font-size: 14px;
        color: #00f2ff;
        font-weight: bold;
        border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    }

    .library-search {
        padding: 15px;
    }

    .lib-items {
        flex: 1;
        padding: 0 15px;
    }

    .lib-item {
        padding: 12px;
        background: rgba(255, 255, 255, 0.03);
        border: 1px solid rgba(0, 242, 255, 0.1);
        border-radius: 4px;
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 10px;
        cursor: move;

        &:hover {
            border-color: #00f2ff;
            background: rgba(0, 242, 255, 0.05);
        }

        .op-icon {
            background: rgba(0, 0, 0, 0.2);
            width: 32px;
            height: 32px;
            border-radius: 4px;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .op-labels {
            display: flex;
            flex-direction: column;

            .op-n {
                font-size: 13px;
                color: #fff;
            }

            .op-t {
                font-size: 11px;
                color: #8fa3b8;
            }
        }
    }
}

.canvas-container {
    flex: 1;
    height: 100%;
    position: relative;
    background: #050a14;
}

.workflow-flow {
    :deep(.vue-flow__node) {
        background: #1a2635;
        border: 1px solid #00f2ff;
        border-radius: 4px;
        color: #fff;
        padding: 12px;
        font-size: 13px;
        min-width: 120px;
        text-align: center;
        transition: all 0.2s;

        &.selected {
            border-width: 2px !important;
            box-shadow: 0 0 15px rgba(0, 242, 255, 0.5);
            background: rgba(0, 242, 255, 0.1);
        }

        &.node-input {
            border-color: #67c23a;
            border-radius: 20px;

            &.selected {
                box-shadow: 0 0 15px rgba(103, 194, 58, 0.5);
            }
        }

        &.node-output {
            border-color: #e6a23c;
            clip-path: polygon(10% 0, 100% 0, 90% 100%, 0 100%);

            &.selected {
                box-shadow: 0 0 15px rgba(230, 162, 60, 0.5);
            }
        }

        &.node-process {
            border-color: #00f2ff;
            border-style: ridge;
        }
    }

    :deep(.vue-flow__edge) {
        &.selected .vue-flow__edge-path {
            stroke: #fff !important;
            stroke-width: 3;
            filter: drop-shadow(0 0 5px #00f2ff);
        }
    }

    :deep(.vue-flow__handle) {
        background: #00f2ff;
        width: 8px;
        height: 8px;
    }

    :deep(.vue-flow__edge-path) {
        stroke: rgba(0, 242, 255, 0.4);
        stroke-width: 2;
    }
}

.prop-panel {
    width: 320px;
    border-left: 1px solid rgba(255, 255, 255, 0.05);
    display: flex;
    flex-direction: column;

    .panel-header {
        padding: 20px;
        font-size: 14px;
        color: #00f2ff;
        font-weight: bold;
        border-bottom: 1px solid rgba(255, 255, 255, 0.05);
        text-align: center;
    }

    .panel-body {
        padding: 20px;
    }

    :deep(.el-form-item__label) {
        color: #8fa3b8;
        font-size: 13px;
    }

    :deep(.el-divider__text) {
        background: #02060c;
        color: #404b5a;
        font-size: 12px;
    }

    :deep(.el-input-number.is-controls-right .el-input-number__decrease) {
        background: transparent;
        border: none;
    }

    :deep(.el-input-number.is-controls-right .el-input-number__increase) {
        background: transparent;
        border: none;
    }
}
</style>
