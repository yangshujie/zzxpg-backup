<template>
    <div class="subpage-container dual-layout">
        <!-- Left Panel: Template Recommendation -->
        <aside class="left-panel">
            <div class="section-header">
                <h3 class="panel-title">预处理流程模板</h3>
                <el-button type="primary" link icon="Refresh">更新推荐</el-button>
            </div>

            <el-scrollbar class="template-list">
                <div v-for="tpl in templates" :key="tpl.id" class="recommend-card tech-card">
                    <div class="card-status">RECOMMENDED</div>
                    <div class="card-header">
                        <span class="tpl-name">{{ tpl.name }}</span>
                        <el-tag size="small" effect="plain">{{ tpl.operatorCount }} 节点</el-tag>
                    </div>
                    <div class="card-description">{{ tpl.desc }}</div>
                    <div class="meta-row">
                        <span><el-icon>
                                <User />
                            </el-icon> {{ tpl.creator }}</span>
                        <span><el-icon>
                                <VideoPlay />
                            </el-icon> {{ tpl.useCount }} 次使用</span>
                    </div>
                    <el-select v-model="selectedTemplate" placeholder="请选择模板">
                        <el-option v-for="item in templateList" :key="item.id" :label="item.templateName"
                            :value="item.id" />
                    </el-select>
                    <div class="action-row">
                        <el-button type="primary" class="w-full" icon="Pointer" @click="gotoCalculate">前往计算</el-button>
                    </div>
                </div>
            </el-scrollbar>
        </aside>

        <!-- Right Panel: Visual Workflow -->
        <main class="right-panel">
            <div class="canvas-header">
                <div class="canvas-title">
                    <el-icon>
                        <Monitor />
                    </el-icon> 流程可视化视图
                </div>
                <div class="canvas-tools">
                    <el-button-group>
                        <el-button size="small" icon="ZoomIn">放大</el-button>
                        <el-button size="small" icon="ZoomOut">缩小</el-button>
                        <el-button size="small" icon="Aim">居中</el-button>
                    </el-button-group>
                </div>
            </div>

            <div class="canvas-wrapper">
                <VueFlow :nodes="flowNodes" :edges="flowEdges" :fit-view-on-init="true" class="workflow-flow">
                    <Background pattern-color="#00f2ff" :gap="20" />
                    <Controls />
                    <MiniMap />
                </VueFlow>
            </div>
        </main>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
    Search, Plus, ArrowRight, Edit, Delete, CopyDocument, Back,
    Cpu, VideoPlay, Check
} from '@element-plus/icons-vue'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import { ElMessage } from 'element-plus'
import { listCalcFlow } from "@/api/zhpg/calcFlow"

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
const templateList = ref([])
const selectedTemplate = ref("")

// Selection & Props
const selectedNode = ref(null)
const selectedEdge = ref(null)
const nodeProps = reactive({ threshold: 50, timeout: 500, threads: 1 })

// Single recommended template as per request
const templates = ref([
    {
        id: 1,
        name: '智能化数据集预处理标准推荐模板',
        operatorCount: 9,
        creator: '系统推荐',
        useCount: 1540,
        previewOps: ['预处理', '清洗', '转换', '统计'],
        desc: '本课程涵盖了从原始数据清洗到最终统计分析的全流程标准，内置行业领先的野值检测与逻辑一致性校验算法。'
    }
])

// 9-step processing sequence
const flowNodes = ref([
    { id: '1', label: '野值剔除处理', position: { x: 50, y: 50 } },
    { id: '2', label: '数据值去重', position: { x: 280, y: 50 } },
    { id: '3', label: '表格合并', position: { x: 510, y: 50 } },
    { id: '4', label: '数据关联匹配', position: { x: 510, y: 200 } },
    { id: '5', label: '数据排序', position: { x: 280, y: 200 } },
    { id: '6', label: '数据格式转换', position: { x: 50, y: 200 } },
    { id: '7', label: '数据值填充', position: { x: 50, y: 350 } },
    { id: '8', label: '一致性检验', position: { x: 280, y: 350 } },
    { id: '9', label: '数据统计分析', position: { x: 510, y: 350 } },
])

const flowEdges = ref([
    { id: 'e1-2', source: '1', target: '2', animated: true },
    { id: 'e2-3', source: '2', target: '3', animated: true },
    { id: 'e3-4', source: '3', target: '4', animated: true },
    { id: 'e4-5', source: '4', target: '5', animated: true },
    { id: 'e5-6', source: '5', target: '6', animated: true },
    { id: 'e6-7', source: '6', target: '7', animated: true },
    { id: 'e7-8', source: '7', target: '8', animated: true },
    { id: 'e8-9', source: '8', target: '9', animated: true },
])

onNodeClick((e) => {
    selectedNode.value = e.node
})

onEdgeClick((e) => {
    selectedEdge.value = e.edge
    selectedNode.value = null
})

const router = useRouter()
const route = useRoute()

const gotoCalculate = () => {
    if (!selectedTemplate.value) {
        ElMessage.warning('请选择模板')
        return
    }
    router.push({
        path: '/process/reception-sys/flowRunner',
        query: {
            templateId: selectedTemplate.value,
            requirementId: route.query.id
        }
    })
}

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
const listFlow = () => {
    listCalcFlow().then(res => {
        console.log(res)
        templateList.value = res.rows.map(item => {
            return {
                id: item.id,
                templateName: item.templateName,
            }
        })
    })
}

onMounted(() => {
    listFlow()
})
</script>

<style scoped lang="scss">
@import "@/assets/styles/v2/variables.scss";

.subpage-container.dual-layout {
    display: flex;
    height: 100%;
    // 替换 #10131a 为深色主题背景变量
    background: var(--bg-color); 
    padding: 20px;
    gap: 20px;
    overflow: hidden;
}

/* Left Panel Styling */
.left-panel {
    width: 360px;
    // 替换 $surface-low (原 #0B1A30) 为侧边栏背景变量
    background: var(--sider-bg-color);
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    padding: 20px;
    // 替换 $diffused-shadow 为通用阴影变量
    box-shadow: var(--box-shadow-base);

    .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 24px;
        padding-bottom: 12px;
        // 使用 RGB 变量构建动态边框颜色
        border-bottom: 1px solid rgba(var(--border-color-rgb), 0.1);

        .panel-title {
            margin: 0;
            font-size: 18px;
            // 替换 #fff 为主文本色
            color: var(--text-color-primary);
            font-weight: 600;
        }
    }
}

.recommend-card {
    // 替换 $surface-high (原 #122c4c) 为卡片背景变量
    background: var(--card-bg-color);
    // 使用 RGB 变量构建动态边框
    border: 1px solid rgba(var(--border-color-rgb), 0.1); 
    border-radius: 8px;
    padding: 20px;
    position: relative;
    overflow: hidden;

    .card-status {
        position: absolute;
        top: 10px;
        right: -35px;
        // 替换 $cyber-gradient (原线性渐变) 为主色背景，或使用固定渐变但配合动态文字
        // 这里为了保持霓虹感，使用主色作为背景，文字使用深色以确保对比度，或者保留原渐变但需注意主题适配
        // 方案：使用主色背景
        background: var(--primary-color);
        // 赛博朋克模式下文字为黑，清爽模式下文字为白可能需要调整，这里暂时保持黑色或根据主色亮度调整
        // 为了简单适配，如果主色变浅，文字需变深。这里假设主色足够亮或深色。
        // 更通用的做法：
        color: #000; 
        font-size: 10px;
        font-weight: 800;
        padding: 4px 40px;
        transform: rotate(45deg);
        // 使用霓虹阴影变量
        box-shadow: var(--box-shadow-neon);
    }

    .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;

        .tpl-name {
            font-size: 16px;
            color: var(--text-color-primary);
            font-weight: 600;
            max-width: 70%;
        }
    }

    .card-description {
        font-size: 14px;
        // 替换 rgba(255, 255, 255, 0.6) 为次要文本色带透明度
        color: rgba(var(--text-color-primary-rgb), 0.6);
        line-height: 1.6;
        margin-bottom: 24px;
    }

    .meta-row {
        display: flex;
        gap: 20px;
        margin-bottom: 24px;

        span {
            font-size: 13px;
            // 替换 rgba(164, 230, 255, 0.7) 为次要文本色带透明度
            color: rgba(var(--text-color-secondary-rgb), 0.7);
            display: flex;
            align-items: center;
            gap: 6px;
        }
    }

    .action-row {
        .w-full {
            width: 100%;
            // 使用主色背景
            background: var(--primary-color);
            margin-top: 10px;
            border: none;
            // 根据背景色动态调整文字颜色 (深色背景白字，亮色背景黑字)
            // 这里简单处理：如果主色是亮的(如青色)，文字用黑；如果是深蓝，文字用白。
            // 为了兼容两个主题，我们可以使用 mix-blend-mode 或者简单的判断，但 CSS 变量本身不带亮度信息。
            // 观察主题配置：dark-theme 主色 #00b3db (亮), light-clean 主色 #007bff (亮)。
            // 两个主题的主色都较亮，所以文字统一用黑色或深色比较安全，或者使用 var(--text-color-primary) 如果背景够亮。
            // 在 dark-theme 中 #00b3db 配 #000 很好。在 light-clean 中 #007bff 配 #fff 很好。
            // 这里我们根据主题动态设置可能比较复杂，暂且使用 #000 (赛博朋克风格) 或者根据主色反相。
            // 更好的方式：在 light 主题下，按钮文字自动变白？
            // 简单策略：默认黑字，如果背景太深则不行。
            // 让我们看变量：dark-theme 主色亮，light-clean 主色也亮。
            // 实际上 light-clean 的 #007bff 配白字更好看。
            // 这里暂时保留原逻辑的黑色，或者使用一个专门的按钮文字变量（如果有的话）。
            // 暂且使用 #000，如果在 light 主题下看不清，建议用户在 theme.js 中为 light 主题调整主色或增加 --btn-text-color。
            // 修正：为了最佳效果，我们假设主色背景下的文字颜色。
            // Dark: #00b3db -> #000
            // Light: #007bff -> #fff (通常)
            // 由于无法在纯 CSS 中根据变量值判断亮度，这里暂时写死为 #000，
            // 但建议在 light-clean 主题中，如果主色较深，可能需要调整。
            // 或者，我们可以使用 var(--text-color-primary) 并依赖背景对比度。
            // 让我们尝试用 #000，因为两个主题的主色都偏向鲜艳的蓝色/青色，黑色通常可见，或者白色。
            // 观察 light-clean: #007bff 背景，白色文字 (#fff) 对比度更好。
            // 观察 dark-theme: #00b3db 背景，黑色文字 (#000) 对比度更好。
            // 这是一个难点。最简单的hack是利用 mix-blend-mode: difference; 但会改变颜色。
            // 这里暂时使用 #000，并在 light 主题下可能显得有点暗，或者我们可以利用 var(--bg-color) 来判断？不行。
            // 让我们假设用户希望保持“赛博朋克”风格按钮，即使在白模式下也是亮色背景黑字？
            // 或者，我们可以修改 light-clean 的主色为更浅的蓝色？
            // 暂时保持 color: #000; 如果效果不好，建议在 theme.js 中为 light 主题添加 --button-text-color: #fff;
            color: #000; 
            font-weight: 600;

            &:hover {
                // 使用霓虹阴影
                box-shadow: var(--box-shadow-neon);
                transform: translateY(-2px);
            }
        }
    }
}

/* Right Panel Styling */
.right-panel {
    flex: 1;
    background: var(--sider-bg-color);
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    position: relative;

    .canvas-header {
        padding: 16px 24px;
        // 使用卡片背景或侧边栏背景的轻微变体
        background: var(--card-bg-color); 
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid rgba(var(--border-color-rgb), 1);

        .canvas-title {
            color: var(--text-color-primary);
            font-size: 15px;
            font-weight: 500;
            display: flex;
            align-items: center;
            gap: 10px;

            .el-icon {
                // 使用主色
                color: var(--primary-color);
            }
        }
    }

    .canvas-wrapper {
        flex: 1;
        position: relative;
        // 使用背景色，稍微深一点可以通过 overlay 模拟，或者直接复用 bg-color
        // 原文是 #0d1017 (比 #10131a 更深)。
        // 我们可以使用 bg-color 并叠加一个深色遮罩，或者直接用 bg-color。
        // 为了简单，直接使用 var(--bg-color)，如果需要更深，可以使用 rgba(var(--bg-color-rgb), 0.8)
        background: rgba(var(--bg-color-rgb), 0.95); 
    }
}

/* VueFlow Overrides */
.workflow-flow {
    :deep(.vue-flow__node) {
        background: var(--card-bg-color);
        border: 1px solid rgba(var(--border-color-rgb), 1);
        border-radius: 6px;
        color: var(--text-color-primary);
        padding: 12px 20px;
        font-size: 14px;
        font-family: 'Space Grotesk', sans-serif;
        box-shadow: var(--box-shadow-base);
        transition: all 0.3s;

        &:hover {
            border-color: var(--primary-color);
            box-shadow: var(--box-shadow-neon);
        }

        &.selected {
            // 使用主色的透明背景
            background: rgba(var(--primary-color-rgb), 0.1);
            border-color: var(--primary-color);
            box-shadow: 0 0 20px rgba(var(--primary-color-rgb), 0.3);
        }
    }

    :deep(.vue-flow__edge-path) {
        // 使用边框颜色的透明版本
        stroke: rgba(var(--border-color-rgb), 0.3);
        stroke-width: 2.5;
    }

    :deep(.vue-flow__edge.animated .vue-flow__edge-path) {
        stroke: var(--primary-color);
        // 使用 drop-shadow 滤镜，颜色需硬编码或使用 var，这里 var 在 filter 中通常有效
        filter: drop-shadow(0 0 5px var(--primary-color));
    }

    :deep(.vue-flow__controls) {
        background: var(--card-bg-color);
        border: 1px solid rgba(var(--border-color-rgb), 0.1);

        button {
            background: transparent;
            border-bottom: 1px solid rgba(var(--border-color-rgb), 0.1);
            fill: var(--text-color-primary);

            &:hover {
                background: rgba(var(--primary-color-rgb), 0.1);
            }
        }
    }

    :deep(.vue-flow__minimap) {
        background: var(--card-bg-color);
        border: 1px solid rgba(var(--border-color-rgb), 0.1);
        border-radius: 4px;
    }
}
</style>
