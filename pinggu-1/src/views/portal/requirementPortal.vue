<template>
    <div class="requirement-portal-container">
        <div class="page-header">
            <div class="page-title">
                <el-icon class="mr-2">
                    <MapLocation />
                </el-icon>
                评估需求设计流程导航
            </div>
            <div class="page-sub">点击蓝色及彩色功能节点进入操作界面 · 暗灰节点为后端自动化作业流程节点</div>
        </div>

        <!-- 图例 -->
        <div class="legend">
            <div class="legend-item"><span class="ld theme-cyan"></span>主线基准流程 (独立)</div>
            <div class="legend-item"><span class="ld theme-orange"></span>分中心协同细化会签</div>
            <div class="legend-item"><span class="ld theme-purple"></span>预置工具与规范配置区</div>
            <div class="legend-item"><span class="ld type-backend"></span>系统自动化无感流转</div>
        </div>

        <div class="flow-wrapper">
            <VueFlow v-model="elements" :min-zoom="0.2" :max-zoom="1.5" :nodes-draggable="false" :pan-on-drag="true"
                :zoom-on-scroll="true" fit-view-on-init class="cyber-vue-flow">
                <Background pattern-color="rgba(0, 242, 255, 0.1)" gap="25" />
                <Controls position="bottom-right" :show-interactive="false" />

                <!-- 自定义节点渲染 -->
                <template #node-custom="props">
                    <div class="cyber-node" :class="[`theme-${props.data.theme}`, `type-${props.data.type}`]"
                        @click="handleNodeClick(props.data)">

                        <Handle type="target" position="left" id="left-target" :style="{ opacity: 0 }" />
                        <Handle type="target" position="top" id="top-target" :style="{ opacity: 0 }" />
                        <Handle type="target" position="right" id="right-target" :style="{ opacity: 0 }" />
                        <Handle type="target" position="bottom" id="bottom-target" :style="{ opacity: 0 }" />

                        <div class="node-icon-box" v-if="props.data.icon">
                            <el-icon>
                                <component :is="props.data.icon" />
                            </el-icon>
                        </div>
                        <div class="node-content">
                            <div class="node-title">{{ props.data.title }}</div>
                            <div class="node-desc" v-if="props.data.desc">{{ props.data.desc }}</div>
                        </div>

                        <Handle type="source" position="left" id="left-source" :style="{ opacity: 0 }" />
                        <Handle type="source" position="right" id="right-source" :style="{ opacity: 0 }" />
                        <Handle type="source" position="top" id="top-source" :style="{ opacity: 0 }" />
                        <Handle type="source" position="bottom" id="bottom-source" :style="{ opacity: 0 }" />
                    </div>
                </template>
            </VueFlow>
        </div>

        <!-- 交互弹窗 -->
        <el-dialog v-model="modalVisible" :title="'节点任务调度系统'" width="480px" custom-class="portal-modal">
            <div class="modal-content">
                <div class="modal-icon" :class="`text-${modalData.theme}`">
                    <el-icon>
                        <component :is="modalData.icon" />
                    </el-icon>
                </div>
                <h3 class="modal-title">{{ modalData.title }}</h3>
                <div class="modal-desc">{{ modalData.desc }}</div>

                <div class="modal-meta" v-if="modalData.path">
                    <span class="meta-label">路径:</span>
                    <span class="meta-value">{{ modalData.path }}</span>
                </div>
            </div>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="modalVisible = false" class="cyber-btn-cancel">暂不进入</el-button>
                    <el-button type="primary" class="cyber-btn-confirm" @click="goToPage(modalData.path)">
                        进入页面 <el-icon class="ml-1">
                            <Right />
                        </el-icon>
                    </el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
// Vue Flow 依赖
import { VueFlow, MarkerType, Position, Handle } from '@vue-flow/core';
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';
import '@vue-flow/core/dist/style.css';
import '@vue-flow/core/dist/theme-default.css';
import '@vue-flow/controls/dist/style.css';

import {
    Document, Aim, MagicStick, Operation, DocumentChecked, DataLine, Files,
    DataAnalysis, Connection, EditPen, Finished, CopyDocument, SetUp, DocumentCopy,
    MapLocation, Right, Tickets
} from '@element-plus/icons-vue'

const router = useRouter();

// =======================
// 定义图元素 Elements
// =======================
// 通用的样式工厂函数
const makeNode = (id, x, y, title, icon, theme, type, path = '', desc = '') => ({
    id,
    type: 'custom',
    position: { x, y },
    data: { title, icon, theme, type, path, desc }
});

const makeEdge = (source, target, theme, dashed = false, isStraight = false, sourceHandle = 'right-source', targetHandle = 'left-target') => {
    let colorMap = {
        cyan: '#00f2ff',
        orange: '#e3b341',
        purple: '#bc8cff',
        red: '#f85149',
        gray: '#5c7080'
    }
    let color = colorMap[theme] || '#00f2ff';
    return {
        id: `e-${source}-${sourceHandle}-${target}-${targetHandle}`,
        source,
        target,
        sourceHandle,
        targetHandle,
        type: isStraight ? 'straight' : 'default',
        animated: dashed,
        style: { stroke: color, strokeWidth: 2, strokeDasharray: dashed ? '5 5' : 'none' },
        markerEnd: MarkerType.ArrowClosed
    }
};

const elements = ref([
    // Nodes
    // 独立功能
    makeNode('list-main', 0, -200, '需求列表', Tickets, 'cyan', 'action', '/major/requirement-sys/management', '快速访问所有评估需求，支持全局查询、详情查看及状态跟踪。'),

    // A线 (y: 0)
    makeNode('a1', 0, 0, '导入一案三纲', Document, 'cyan', 'action', '/major/requirement-sys/import', '导入任务书、试验大纲等，系统智能提取目标、背景。'),
    makeNode('a2', 300, 0, '匹配任务骨架', Aim, 'gray', 'backend', '', '引擎比对知识库本体，调配顶层模型结构特征。'),
    makeNode('a3', 600, 0, '生成评估需求', MagicStick, 'green', 'action', '/major/requirement-sys/generate', '基于骨架全自动生成涵盖对象、指标等7类内容。'),
    makeNode('a4', 900, 0, '需求调整优化', Operation, 'cyan', 'action', '/major/requirement-sys/adjust', '修正低置信度指标项，二次知识挂载纠偏。'),
    makeNode('a5', 1200, 0, '完整评估需求', DocumentChecked, 'cyan', 'core', '/major/requirement-sys/complete', '全生命周期确权与协同高置信度完备版本。'),
    // 尾部线 (1500列)
    makeNode('a6', 1500, -150, '综合数据汇集系统', DataLine, 'gray', 'backend', '', '需求核心对象拓扑关系映射与持久化落库。'),
    makeNode('a8', 1500, 0, '完备度分析', DataAnalysis, 'red', 'action', '/major/requirement-sys/completeness', '扫描量化四项评估逻辑依赖，发现异常算子孤岛或重要环节数据盲区。'),
    makeNode('a7', 1500, 150, '评估需求报告方案', Files, 'gray', 'backend', '', '各阶段成果与技术规范的标准化排版归档编印。'),

    // B协同线 (上下间距增大，y: 250)
    makeNode('b1', 600, 250, '分中心协同设计', Connection, 'gray', 'backend', '', '派发子任务。'),
    makeNode('b2', 900, 250, '分中心细化会签', EditPen, 'orange', 'action', '/major/requirement-sys/distribute', '将复杂项派单至靶场、分中心并行细化。'),
    makeNode('b3', 1200, 250, '需求汇集', Finished, 'orange', 'action', '/major/requirement-sys/distribute', '比对聚类多方上报，处理异常冲突，形成基准。'),

    // 工具模板 (上下间距增大，y: 500)
    makeNode('t1', 300, 500, '需求设计流程管理', CopyDocument, 'purple', 'tool', '/major/requirement-sys/template/flow', '定制生命周期并行流转与审批策略。'),
    makeNode('t2', 900, 500, '完备度量化准则', SetUp, 'purple', 'tool', '/major/requirement-sys/template/completeness', '配平各项雷达评分阈值模型与惩罚机制。'),
    makeNode('t3', 1500, 500, '需求报告模板管理', DocumentCopy, 'purple', 'tool', '/major/requirement-sys/template/report', '映射骨架属性对插 Word 生成标准化公文。'),

    // Edges
    // Main row (straight)
    // 默认按照右侧出发（sourceHandle='right-source'）
    makeEdge('a1', 'a2', 'cyan', false, true),
    makeEdge('a2', 'a3', 'cyan', false, true),
    makeEdge('a3', 'a4', 'cyan', false, true),
    makeEdge('a4', 'a5', 'cyan', false, true),

    // Cross B path (step)
    // 根据要求，生成评估需求(a3) 到分中心协同设计(b1) 的连线例外：由下方出发
    makeEdge('a3', 'b1', 'orange', true, false, 'bottom-source', 'top-target'),
    makeEdge('b1', 'b2', 'orange', false, true),
    makeEdge('b2', 'b3', 'orange', false, true),
    makeEdge('b3', 'a5', 'cyan', false, false, 'top-source', 'bottom-target'),

    // T tool paths (step)
    // source必须由上方或者右侧发出
    // makeEdge('t1', 'a3', 'purple', true, false, 'top-source', 'bottom-target'),
    makeEdge('t2', 'a8', 'purple', true, false, 'right-source', 'right-target'),
    makeEdge('t3', 'a8', 'purple', true, false, 'top-source', 'right-target'),

    // Tail paths (step)
    makeEdge('a5', 'a6', 'gray', true, false),
    makeEdge('a5', 'a8', 'red', false, true),
    makeEdge('a5', 'a7', 'gray', true, false),
]);


// =======================
// Modal 交互逻辑
// =======================
const modalVisible = ref(false);
const modalData = ref({ title: '', icon: null, desc: '', path: '', theme: '' });

const handleNodeClick = (nodeData) => {
    if (nodeData.type === 'backend') return; // backend nodes non-clickable
    modalData.value = { ...nodeData };
    modalVisible.value = true;
};

const goToPage = (path) => {
    if (!path) return;
    modalVisible.value = false;
    router.push(path);
};
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.requirement-portal-container {
    height: calc(100vh - 70px);
    display: flex;
    flex-direction: column;
    padding: 20px 30px;
}

.page-header {
    text-align: center;
    margin-bottom: 20px;

    .page-title {
        font-size: 24px;
        font-weight: 800;
        color: #fff;
        text-shadow: 0 0 15px rgba(0, 242, 255, 0.4);
        margin-bottom: 8px;
        letter-spacing: 2px;
        display: flex;
        justify-content: center;
        align-items: center;
    }

    .page-sub {
        font-size: 14px;
        color: #8fa3b8;
    }
}

.legend {
    display: flex;
    justify-content: center;
    gap: 30px;
    margin-bottom: 20px;
    z-index: 10;

    .legend-item {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 13px;
        color: #a0aab8;

        .ld {
            width: 16px;
            height: 16px;
            border-radius: 4px;
            border: 1px solid rgba(255, 255, 255, 0.2);
        }

        // Match node themes below
        .theme-cyan {
            background: rgba(0, 242, 255, 0.2);
            border-color: #00f2ff;
        }

        .theme-orange {
            background: rgba(227, 179, 65, 0.2);
            border-color: #e3b341;
        }

        .theme-purple {
            background: rgba(188, 140, 255, 0.2);
            border-color: #bc8cff;
        }

        .type-backend {
            background: rgba(30, 35, 40, 0.8);
            border-style: dashed;
            border-color: #5c7080;
        }
    }
}

.flow-wrapper {
    flex: 1;
    background: rgba(10, 15, 24, 0.4);
    border: 1px solid rgba(0, 242, 255, 0.15);
    border-radius: 12px;
    overflow: hidden;
    position: relative;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5), inset 0 0 30px rgba(0, 242, 255, 0.05);

    /* 让 vue-flow 原本的边框消失 */
    .cyber-vue-flow {
        width: 100%;
        height: 100%;
    }
}

// 覆写 vue-flow 连线自带阴影，让发光更赛博
:deep(.vue-flow__edge-path) {
    filter: drop-shadow(0 0 4px currentColor);
}

/* ==================
   Custom Node styling
   ================== */
.cyber-node {
    display: flex;
    flex-direction: column;
    text-align: center;
    padding: 10px 10px;
    border-radius: 8px;
    width: 170px;
    height: 80px;
    background: linear-gradient(135deg, rgba(16, 23, 35, 0.95), rgba(8, 13, 20, 0.9));
    border: 3px solid #30363d;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.5);
    color: #fff;
    cursor: pointer;
    transition: all 0.2s ease;

    .node-icon-box {
        font-size: 20px;
        margin: 0 auto 4px;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .node-content {
        flex: 1;
        display: flex;
        flex-direction: column;
        justify-content: flex-start;
        align-items: center;
    }

    .node-title {
        font-size: 13px;
        font-weight: 800;
        letter-spacing: 0.5px;
        line-height: 1.2;
        margin-bottom: 8px;
        word-break: break-word;
    }

    .node-desc {
        font-size: 11px;
        color: #8fa3b8;
        line-height: 1.2;
        font-weight: normal;
        margin-top: 2px;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }

    &.type-backend {
        cursor: default;
        border-style: dashed;
        color: #8fa3b8;
        background: rgba(20, 25, 30, 0.85);

        .node-icon-box {
            color: #5c7080;
        }

        &:hover {
            transform: none;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.5);
        }
    }

    &.type-action,
    &.type-tool,
    &.type-core {
        &:hover {
            transform: translateY(-2px);
            z-index: 100;
        }
    }

    &.type-core {
        border-width: 4px;
    }
}

/* Themes applying colors */
@mixin cyber-node-theme($name, $color, $bg-glow) {
    .cyber-node.theme-#{$name} {
        border-color: rgba($color, 0.4);

        .node-icon-box {
            color: $color;
            text-shadow: 0 0 10px rgba($color, 0.6);
        }

        &:hover {
            border-color: $color;
            box-shadow: 0 0 15px rgba($color, 0.3), inset 0 0 15px $bg-glow;
            text-shadow: 0 0 5px rgba($color, 0.3);
        }

        &.type-core {
            border-width: 2px;
            border-color: $color;
            box-shadow: 0 0 20px rgba($color, 0.15), inset 0 0 20px $bg-glow;
        }

        &.type-backend {
            border-color: #5c7080;

            &:hover {
                border-color: #5c7080;
                text-shadow: none;
                box-shadow: none;
            }
        }
    }

    // modal icon styling
    .text-#{$name} {
        color: $color;
        font-size: 48px;
        filter: drop-shadow(0 0 8px $color);
        margin-bottom: 15px;
    }
}

@include cyber-node-theme('cyan', #00f2ff, rgba(0, 242, 255, 0.15));
@include cyber-node-theme('green', #3fb950, rgba(63, 185, 80, 0.15));
@include cyber-node-theme('orange', #e3b341, rgba(227, 179, 65, 0.15));
@include cyber-node-theme('red', #f85149, rgba(248, 81, 73, 0.15));
@include cyber-node-theme('purple', #bc8cff, rgba(188, 140, 255, 0.15));
@include cyber-node-theme('gray', #8fa3b8, transparent);


/* ================= 
   弹窗交互重置 
   ================= */
.modal-content {
    text-align: center;
    padding: 10px 0 20px;

    .modal-title {
        font-size: 20px;
        font-weight: 800;
        color: #fff;
        margin-bottom: 15px;
        letter-spacing: 1px;
    }

    .modal-desc {
        font-size: 14px;
        color: #8fa3b8;
        line-height: 1.6;
        text-align: left;
        background: rgba(6, 13, 23, 0.5);
        padding: 15px;
        border-radius: 6px;
        border: 1px solid rgba(255, 255, 255, 0.05);
        margin-bottom: 20px;
    }

    .modal-meta {
        font-size: 12px;
        text-align: left;
        display: flex;
        align-items: center;
        background: rgba(0, 242, 255, 0.05);
        padding: 8px 12px;
        border-radius: 4px;
        border-left: 3px solid #00f2ff;

        .meta-label {
            color: #5c7080;
            margin-right: 10px;
        }

        .meta-value {
            color: #00f2ff;
            font-family: monospace;
        }
    }
}

:deep(.portal-modal) {
    background-color: #121822 !important;
    border: 1px solid rgba(0, 242, 255, 0.3) !important;
    box-shadow: 0 0 40px rgba(0, 0, 0, 0.8), 0 0 20px rgba(0, 242, 255, 0.1) !important;
    border-radius: 12px;

    .el-dialog__header {
        border-bottom: 1px solid rgba(255, 255, 255, 0.05);
        margin-right: 0;
        padding-bottom: 15px;
    }

    .el-dialog__title {
        color: #e6edf3;
        font-weight: bold;
        font-size: 14px;
    }

    .el-dialog__footer {
        border-top: 1px solid rgba(255, 255, 255, 0.05);
        padding-top: 15px;
    }
}

.cyber-btn-cancel {
    background: transparent;
    border: 1px solid #30363d;
    color: #8fa3b8;

    &:hover {
        background: rgba(255, 255, 255, 0.05);
        color: #fff;
    }
}

.cyber-btn-confirm {
    background: rgba(0, 242, 255, 0.1);
    border: 1px solid #00f2ff;
    color: #00f2ff;
    font-weight: bold;
    letter-spacing: 1px;

    &:hover {
        background: rgba(0, 242, 255, 0.2);
        box-shadow: 0 0 15px rgba(0, 242, 255, 0.4);
        color: #fff;
    }
}
</style>
