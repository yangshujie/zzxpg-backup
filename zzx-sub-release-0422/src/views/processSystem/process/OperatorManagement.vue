<template>
    <div class="subpage-container">
        <div class="page-header">
            <div class="left">
                <h3 class="page-title">算子管理</h3>
            </div>
            <div class="right">
                <el-button type="primary" icon="Plus" @click="showCreationSelect = true">构建新算子</el-button>
            </div>
        </div>

        <!-- Main List View -->
        <div class="operator-grid" v-if="!activeMode">
            <el-row :gutter="20">
                <el-col :span="6" v-for="op in operators" :key="op.id">
                    <el-card class="operator-card" :body-style="{ padding: '0px' }">
                        <div class="op-icon-header" :style="{ background: op.color + '1a' }">
                            <el-icon :size="40" :style="{ color: op.color }">
                                <component :is="op.icon" />
                            </el-icon>
                        </div>
                        <div class="op-info">
                            <div class="op-name">{{ op.name }}</div>
                            <div class="op-meta">
                                <span class="type">{{ op.type }}</span>
                                <span class="mode-tag">{{ op.buildMode === 'visual' ? '可视化' : '引导' }}</span>
                            </div>
                            <div class="op-desc">{{ op.desc }}</div>
                        </div>
                        <div class="op-footer">
                            <el-button link type="primary">编辑</el-button>
                            <el-divider direction="vertical" />
                            <el-button link type="danger">移除</el-button>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </div>

        <!-- Creation Mode Selector -->
        <el-dialog v-model="showCreationSelect" title="选择算子构建方式" width="450px" class="tech-dialog">
            <div class="mode-selector">
                <div class="mode-option" @click="startGuided">
                    <el-icon :size="32">
                        <MagicStick />
                    </el-icon>
                    <div class="mode-text">
                        <h4>引导式创建</h4>
                        <p>通过向导一步步配置算子属性与逻辑</p>
                    </div>
                </div>
                <div class="mode-option" @click="startVisual">
                    <el-icon :size="32">
                        <Share />
                    </el-icon>
                    <div class="mode-text">
                        <h4>可视化建模</h4>
                        <p>使用节点画布自由编排算子内部逻辑</p>
                    </div>
                </div>
            </div>
        </el-dialog>

        <!-- Guided Mode Overlay -->
        <div v-if="activeMode === 'guided'" class="mode-overlay">
            <div class="overlay-header">
                <el-button icon="Back" circle @click="activeMode = null" />
                <span class="title">算子构建向导</span>
                <div class="spacer"></div>
                <el-button type="primary" @click="activeMode = null">完成构建</el-button>
            </div>
            <div class="wizard-container">
                <el-steps :active="step" finish-status="success" align-center>
                    <el-step title="基础信息" />
                    <el-step title="接口定义" />
                    <el-step title="逻辑配置" />
                    <el-step title="发布验证" />
                </el-steps>
                <div class="step-content">
                    <div v-if="step === 0" class="form-wrapper">
                        <el-form label-position="top">
                            <el-form-item label="算子名称"><el-input v-model="wizardForm.name" /></el-form-item>
                            <el-form-item label="分类"><el-select v-model="wizardForm.type" style="width: 100%"><el-option
                                        label="清洗" value="清洗" /></el-select></el-form-item>
                        </el-form>
                    </div>
                    <div v-else class="placeholder-content">步骤 {{ step + 1 }} 正在完善...</div>
                </div>
                <div class="wizard-actions">
                    <el-button @click="step--" v-if="step > 0">上一步</el-button>
                    <el-button type="primary" @click="step++" v-if="step < 3">下一步</el-button>
                </div>
            </div>
        </div>

        <!-- Visual Mode Overlay -->
        <div v-if="activeMode === 'visual'" class="mode-overlay visual-mode">
            <div class="overlay-header">
                <el-button icon="Back" circle @click="activeMode = null" />
                <span class="title">算子可视化建模 - {{ wizardForm.name || '未命名算子' }}</span>
                <div class="spacer"></div>
                <el-button type="primary" plain icon="Document">保存草稿</el-button>
                <el-button type="danger" plain icon="Delete" @click="deleteSelected"
                    :disabled="!hasSelection">删除选中项</el-button>
                <el-button type="primary" icon="Check" @click="activeMode = null">发布并退出</el-button>
            </div>
            <div class="visual-editor-body">
                <aside class="editor-sidebar">
                    <div class="sidebar-section">
                        <div class="section-title">逻辑块</div>
                        <div class="node-items">
                            <div v-for="node in nodeLibrary" :key="node.type" class="node-item" draggable="true"
                                @dragstart="onDragStart($event, node)">
                                <el-icon>
                                    <component :is="node.icon" />
                                </el-icon>
                                <span>{{ node.label }}</span>
                            </div>
                        </div>
                    </div>
                </aside>
                <div class="flow-container" @drop="onDrop" @dragover="onDragOver">
                    <!-- Flow Canvas -->
                    <VueFlow :fit-view-on-init="true" class="flow-canvas">
                        <Background pattern-color="#00f2ff" :gap="20" />
                        <Controls />
                        <MiniMap />
                    </VueFlow>
                </div>
                <aside class="editor-props">
                    <div class="props-header">节点属性</div>
                    <div v-if="selectedNode" class="props-body">
                        <el-form label-position="top">
                            <el-form-item label="节点名称">
                                <el-input v-model="selectedNode.label" @input="updateNodeLabel" />
                            </el-form-item>
                            <el-form-item label="标签颜色">
                                <el-color-picker v-model="nodeColor" @change="updateNodeColor" />
                            </el-form-item>
                            <el-form-item label="执行参数 (JSON)">
                                <el-input type="textarea" :rows="4" v-model="nodeDataJson" placeholder="{}" />
                            </el-form-item>
                        </el-form>
                    </div>
                    <el-empty v-else description="选中画布节点进行配置" />
                </aside>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import {
    Plus, MagicStick, Share, Back, Filter, Cpu,
    Connection, Setting, Histogram, Aim, Document, Check,
    Switch, Operation, TrendCharts
} from '@element-plus/icons-vue'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'

// Styles for Flow
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'

// Use composable for better control and reactivity
// Use composable for better control and reactivity
const {
    addNodes, addEdges, nodes, edges, onConnect, project,
    onNodeClick, onPaneClick, removeNodes, removeEdges,
    onEdgeClick, getSelectedNodes, getSelectedEdges
} = useVueFlow()

const showCreationSelect = ref(false)
const activeMode = ref(null) // 'guided' or 'visual'
const step = ref(0)
const wizardForm = ref({ name: '', type: '' })

// Property Editing
const selectedNode = ref(null)
const selectedEdge = ref(null)
const nodeColor = ref('#00f2ff')
const nodeDataJson = ref('{}')

const hasSelection = computed(() => getSelectedNodes.value.length > 0 || getSelectedEdges.value.length > 0)

onNodeClick((e) => {
    selectedNode.value = e.node
    selectedEdge.value = null
    nodeColor.value = e.node.style?.background || '#1a2635'
    nodeDataJson.value = JSON.stringify(e.node.data || {}, null, 2)
})

onEdgeClick((e) => {
    selectedEdge.value = e.edge
    selectedNode.value = null
})

onPaneClick(() => {
    selectedNode.value = null
    selectedEdge.value = null
})

const deleteSelected = () => {
    removeNodes(getSelectedNodes.value)
    removeEdges(getSelectedEdges.value)
    selectedNode.value = null
    selectedEdge.value = null
}

const updateNodeLabel = (val) => {
    if (selectedNode.value) {
        selectedNode.value.label = val
    }
}

const updateNodeColor = (color) => {
    if (selectedNode.value) {
        selectedNode.value.style = { ...selectedNode.value.style, border: `1px solid ${color}`, boxShadow: `0 0 10px ${color}33` }
    }
}

// Initial Data
const initialNodes = [
    { id: '1', type: 'input', label: '原始遥测包', position: { x: 250, y: 50 }, data: {}, style: { border: '1px solid #00f2ff' } },
    { id: '2', label: '帧同步与解密', position: { x: 250, y: 150 }, data: {}, style: { border: '1px solid #00f2ff' } },
    { id: '3', type: 'output', label: '物理量参数集', position: { x: 250, y: 250 }, data: {}, style: { border: '1px solid #00f2ff' } },
]

const initialEdges = [
    { id: 'e1-2', source: '1', target: '2', animated: true },
    { id: 'e2-3', source: '2', target: '3' },
]

// Handle Connections
onConnect((params) => {
    addEdges([{
        ...params,
        updatable: true,
        selectable: true,
        animated: true,
        style: { stroke: '#00f2ff', strokeWidth: 2 }
    }])
})

const nodeLibrary = [
    { label: '逻辑判断', type: 'logic', icon: Switch },
    { label: '数学运算', type: 'calc', icon: TrendCharts },
    { label: '接口协议', type: 'io', icon: Operation },
    { label: '转换映射', type: 'map', icon: Setting },
]

// Sidebar Drag Start
const onDragStart = (event, node) => {
    if (event.dataTransfer) {
        event.dataTransfer.setData('application/vueflow', JSON.stringify(node))
        event.dataTransfer.effectAllowed = 'move'
    }
}

// Container Drag Over
const onDragOver = (event) => {
    event.preventDefault()
    if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'move'
    }
}

// Container Drop
const onDrop = (event) => {
    const data = event.dataTransfer?.getData('application/vueflow')
    if (!data) return

    const nodeData = JSON.parse(data)

    // Project client coordinates to flow coordinates
    const position = project({
        x: event.clientX - 40, // Offset for node width center roughly
        y: event.clientY - 20,
    })

    const newNode = {
        id: `node_${Date.now()}`,
        label: nodeData.label,
        type: 'default',
        position,
        data: { icon: nodeData.icon }
    }

    addNodes([newNode])
}

const operators = [
    { id: 1, name: '遥测断点插值补全', type: '数据清洗', buildMode: 'wizard', desc: '处理星地链路信号遮挡导致的数据异常丢失。', icon: Filter, color: '#00f2ff' },
    { id: 2, name: '传感器噪声剔除', type: '异常检测', buildMode: 'visual', desc: '基于动力学模型自动识别姿控离群特征点。', icon: Aim, color: '#f56c6c' },
    { id: 3, name: '多源参数标准化', type: '特征工程', buildMode: 'visual', desc: '将异构载荷量测数据缩放至统一量纲与区间。', icon: Cpu, color: '#67c23a' },
]

const startGuided = () => {
    showCreationSelect.value = false
    activeMode.value = 'guided'
    step.value = 0
}

const startVisual = () => {
    showCreationSelect.value = false
    activeMode.value = 'visual'
    // Initialize nodes if empty with a small delay for stable store sync
    if (nodes.value.length === 0) {
        setTimeout(() => {
            addNodes(initialNodes)
            addEdges(initialEdges)
        }, 50)
    }
}
</script>

<style scoped lang="scss">
.subpage-container {
    padding: 24px;
    position: relative;
    height: 100%;
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

.operator-card {
    background: rgba(16, 32, 53, 0.6) !important;
    border: 1px solid rgba(0, 242, 255, 0.1) !important;
    margin-bottom: 20px;

    .op-icon-header {
        height: 80px;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .op-info {
        padding: 15px;

        .op-name {
            color: #fff;
            font-weight: bold;
            margin-bottom: 4px;
        }

        .op-meta {
            display: flex;
            gap: 10px;
            font-size: 11px;
            margin-bottom: 10px;

            .type {
                color: #00f2ff;
            }

            .mode-tag {
                color: #8fa3b8;
                border: 1px solid rgba(143, 163, 184, 0.3);
                padding: 0 4px;
                border-radius: 2px;
            }
        }

        .op-desc {
            font-size: 12px;
            color: #8fa3b8;
            height: 32px;
            overflow: hidden;
        }
    }

    .op-footer {
        padding: 10px;
        border-top: 1px solid rgba(255, 255, 255, 0.05);
        display: flex;
        justify-content: center;
    }
}

.mode-selector {
    display: flex;
    flex-direction: column;
    gap: 15px;
    padding: 10px;

    .mode-option {
        background: rgba(0, 242, 255, 0.05);
        border: 1px solid rgba(0, 242, 255, 0.1);
        border-radius: 8px;
        padding: 20px;
        display: flex;
        align-items: center;
        gap: 20px;
        cursor: pointer;
        transition: all 0.3s;

        &:hover {
            border-color: #00f2ff;
            background: rgba(0, 242, 255, 0.1);
            transform: translateX(5px);
        }

        h4 {
            color: #fff;
            margin: 0 0 4px 0;
        }

        p {
            color: #8fa3b8;
            margin: 0;
            font-size: 12px;
        }

        .el-icon {
            color: #00f2ff;
        }
    }
}

.mode-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: #02060c;
    z-index: 1000;
    display: flex;
    flex-direction: column;

    .overlay-header {
        height: 70px;
        background: rgba(10, 21, 37, 0.95);
        border-bottom: 1px solid rgba(0, 242, 255, 0.2);
        display: flex;
        align-items: center;
        padding: 0 30px;
        gap: 20px;

        .title {
            font-size: 18px;
            color: #fff;
            font-weight: bold;
        }

        .spacer {
            flex: 1;
        }
    }
}

.wizard-container {
    flex: 1;
    padding: 60px 0;
    display: flex;
    flex-direction: column;
    align-items: center;

    .el-steps {
        width: 800px;
        margin-bottom: 60px;
    }

    .step-content {
        width: 600px;
        min-height: 400px;
        background: rgba(16, 32, 53, 0.5);
        padding: 40px;
        border-radius: 8px;
    }

    .wizard-actions {
        margin-top: 40px;
    }
}

.visual-mode {
    .visual-editor-body {
        flex: 1;
        display: flex;
        overflow: hidden;
    }

    .editor-sidebar {
        width: 240px;
        border-right: 1px solid rgba(255, 255, 255, 0.05);
    }

    .editor-props {
        width: 300px;
        border-left: 1px solid rgba(255, 255, 255, 0.05);
    }

    .flow-container {
        flex: 1;
        height: 100%;
        position: relative;
    }
}

.sidebar-section {
    padding: 20px;

    .section-title {
        font-size: 12px;
        color: #00f2ff;
        text-transform: uppercase;
        letter-spacing: 1px;
        margin-bottom: 15px;
    }

    .node-items {
        display: flex;
        flex-direction: column;
        gap: 10px;

        .node-item {
            padding: 12px;
            background: rgba(255, 255, 255, 0.03);
            border: 1px solid rgba(255, 255, 255, 0.1);
            display: flex;
            align-items: center;
            gap: 12px;
            font-size: 13px;
            color: #fff;
            cursor: move;

            &:hover {
                border-color: #00f2ff;
                background: rgba(0, 242, 255, 0.05);
            }
        }
    }
}

.props-header {
    padding: 15px;
    font-size: 14px;
    color: #00f2ff;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    text-align: center;
}

.flow-canvas {
    background: #050a14;

    :deep(.vue-flow__node) {
        background: #1a2635;
        border: 1px solid #00f2ff;
        color: #fff;
        border-radius: 4px;
        padding: 10px;
        min-width: 100px;
        text-align: center;
        cursor: grab;
        transition: all 0.2s;

        &:active {
            cursor: grabbing;
        }

        &.selected {
            border-width: 2px;
            box-shadow: 0 0 15px rgba(0, 242, 255, 0.5);
            background: rgba(0, 242, 255, 0.1);
        }

        /* Input specific style */
        &.vue-flow__node-input {
            border-color: #67c23a;
            border-radius: 20px;
        }

        /* Output specific style */
        &.vue-flow__node-output {
            border-color: #e6a23c;
            clip-path: polygon(10% 0, 100% 0, 90% 100%, 0 100%);
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
    }

    :deep(.vue-flow__edge-path) {
        stroke: rgba(0, 242, 255, 0.4);
        stroke-width: 2;
    }
}

.props-body {
    padding: 20px;

    :deep(.el-form-item__label) {
        color: #8fa3b8;
        font-size: 13px;
    }

    :deep(.el-input__inner) {
        background: rgba(255, 255, 255, 0.05) !important;
        color: #fff;
    }

    :deep(.el-input__wrapper) {
        background: rgba(255, 255, 255, 0.05) !important;
        box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.2) inset !important;
    }
}
</style>
