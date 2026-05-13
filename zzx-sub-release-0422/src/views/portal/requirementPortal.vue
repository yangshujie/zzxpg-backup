<template>
    <div class="requirement-portal-container" :class="currentThemeName">
        <div class="page-header">
            <div class="page-title">
                <el-icon class="mr-2">
                    <MapLocation />
                </el-icon>
                评估需求设计流程导航
            </div>
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
                <!-- 背景颜色通过 CSS 变量控制 -->
                <Background :pattern-color="bgPatternColor" gap="25" />
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
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
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

import { useThemeStore } from '@/store/theme'

const router = useRouter();
const route = useRoute();
const themeStore = useThemeStore();

// 获取当前主题
const currentThemeName = computed(() => themeStore.currentThemeName);

// 根据主题动态计算背景图案颜色
const bgPatternColor = computed(() => {
    return currentThemeName.value === 'light-clean' ? 'rgba(0, 123, 255, 0.1)' : 'rgba(0, 242, 255, 0.1)';
});

// =======================
// 定义图元素 Elements
// =======================
const makeNode = (id, x, y, title, icon, theme, type, path = '', desc = '') => ({
    id,
    type: 'custom',
    position: { x, y },
    data: { title, icon, theme, type, path, desc }
});

const makeEdge = (source, target, theme, dashed = false, isStraight = false, sourceHandle = 'right-source', targetHandle = 'left-target') => {
    let colorMap = {
        cyan: currentThemeName.value === 'light-clean' ? '#007bff' : '#00f2ff',
        orange: '#e3b341',
        purple: '#bc8cff',
        red: '#f85149',
        gray: currentThemeName.value === 'light-clean' ? '#6c757d' : '#5c7080',
        green: '#3fb950'
    }
    // 注意：makeEdge 在初始化时只运行一次，如果需要动态变色，需监听主题变化更新 elements
    // 这里为了简单，先使用默认深色值，后续可通过 watch 更新
    let defaultColorMap = {
        cyan: '#00f2ff',
        orange: '#e3b341',
        purple: '#bc8cff',
        red: '#f85149',
        gray: '#5c7080',
        green: '#3fb950'
    }
    let color = defaultColorMap[theme] || '#00f2ff';
    
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
    makeNode('list-main', 0, -200, '需求列表', Tickets, 'cyan', 'action', '/major/requirement-sys/management', '快速访问所有评估需求，支持全局查询、详情查看及状态跟踪。'),
    makeNode('a1', 0, 0, '导入一案三纲', Document, 'cyan', 'action', '/major/requirement-sys/import', '导入任务书、试验大纲等，系统智能提取目标、背景。'),
    makeNode('a2', 300, 0, '匹配任务骨架', Aim, 'cyan', 'action', '/major/requirement-sys/skeleton-match', '引擎比对知识库本体，调配顶层模型结构特征。'),
    makeNode('a3', 600, 0, '生成评估需求', MagicStick, 'green', 'action', '/major/requirement-sys/generate', '基于骨架全自动生成涵盖对象、指标等 7 类内容。'),
    makeNode('m1', 600, -120, '手动建设需求', EditPen, 'cyan', 'action', '/major/requirement-sys/generate', '通过配置项定制搭建评估需求结构，构建核心业务属性。'),
    makeNode('a4', 900, 0, '需求调整优化', Operation, 'cyan', 'action', '/major/requirement-sys/adjust', '修正低置信度指标项，二次知识挂载纠偏。'),
    makeNode('a5', 1200, 0, '完整评估需求', DocumentChecked, 'cyan', 'core', '/major/requirement-sys/complete', '全生命周期确权与协同高置信度完备版本。'),
    makeNode('a6', 1500, -150, '综合数据汇集系统', DataLine, 'gray', 'backend', '', '需求核心对象拓扑关系映射与持久化落库。'),
    makeNode('a8', 1500, 0, '完备度分析', DataAnalysis, 'red', 'action', '/major/requirement-sys/completeness', '扫描量化四项评估逻辑依赖，发现异常算子孤岛或重要环节数据盲区。'),
    makeNode('a7', 1500, 150, '评估需求报告方案', Files, 'gray', 'backend', '', '各阶段成果与技术规范的标准化排版归档编印。'),
    makeNode('b1', 600, 250, '分中心协同设计', Connection, 'gray', 'backend', '', '派发子任务。'),
    makeNode('b2', 900, 250, '分中心细化会签', EditPen, 'orange', 'action', '/major/requirement-sys/distribute', '将复杂项派单至靶场、分中心并行细化。'),
    makeNode('b3', 1200, 250, '需求汇集', Finished, 'orange', 'action', '/major/requirement-sys/distribute', '比对聚类多方上报，处理异常冲突，形成基准。'),
    makeNode('t1', 300, 500, '需求设计流程管理', CopyDocument, 'purple', 'tool', '/major/requirement-sys/template/flow', '定制生命周期并行流转与审批策略。'),
    makeNode('t2', 900, 500, '完备度量化准则', SetUp, 'purple', 'tool', '/major/requirement-sys/template/completeness', '配平各项雷达评分阈值模型与惩罚机制。'),
    makeNode('t3', 1500, 500, '需求报告模板管理', DocumentCopy, 'purple', 'tool', '/major/requirement-sys/template/report', '映射骨架属性对插 Word 生成标准化公文。'),

    // Edges
    makeEdge('a1', 'a2', 'cyan', false, true),
    makeEdge('a2', 'a3', 'cyan', false, true),
    makeEdge('a3', 'a4', 'cyan', false, true),
    makeEdge('a4', 'a5', 'cyan', false, true),
    makeEdge('a1', 'm1', 'cyan', false, false, 'top-source', 'left-target'),
    makeEdge('m1', 'a5', 'cyan', false, false, 'right-source', 'top-target'),
    makeEdge('a3', 'b1', 'orange', true, false, 'bottom-source', 'top-target'),
    makeEdge('b1', 'b2', 'orange', false, true),
    makeEdge('b2', 'b3', 'orange', false, true),
    makeEdge('b3', 'a5', 'cyan', false, false, 'top-source', 'bottom-target'),
    makeEdge('t2', 'a8', 'purple', true, false, 'right-source', 'right-target'),
    makeEdge('t3', 'a8', 'purple', true, false, 'top-source', 'right-target'),
    makeEdge('a5', 'a6', 'gray', true, false),
    makeEdge('a5', 'a8', 'red', false, true),
    makeEdge('a5', 'a7', 'gray', true, false),
]);

// 监听主题变化，更新连线和背景颜色（因为 makeEdge 只在初始化运行）
// 这里简单处理：主题变化时重新生成 edges 或者强制刷新
// 更优雅的方式是将 edge 的 style 绑定为响应式，但 VueFlow 对响应式 style 支持有限
// 这里我们主要依赖 CSS 变量控制节点，连线颜色暂不动态刷新，或者手动触发更新
// 若需严格同步，可在此处 watch themeStore.currentThemeName 并重新赋值 elements.value

const modalVisible = ref(false);
const modalData = ref({ title: '', icon: null, desc: '', path: '', theme: '' });

const handleNodeClick = (nodeData) => {
    if (nodeData.type === 'backend') return;
    modalData.value = { ...nodeData };
    modalVisible.value = true;
};

const goToPage = (path) => {
    if (!path) return;
    modalVisible.value = false;
    router.push({
        path: path,
        query: {
            projectId: route.query.projectId,
        }
    });
};
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.requirement-portal-container {
    height: 100%;
    display: flex;
    flex-direction: column;
    padding: 20px 30px;
    background-color: var(--bg-color); // 使用全局背景色
    transition: background-color 0.3s;
}

.page-header {
    text-align: center;
    margin-bottom: 20px;

    .page-title {
        font-size: 24px;
        font-weight: 800;
        color: var(--text-color-primary); // 使用全局文字色
        text-shadow: 0 0 15px rgba(0, 242, 255, 0.2); // 减弱光效以适配浅色
        margin-bottom: 8px;
        letter-spacing: 2px;
        display: flex;
        justify-content: center;
        align-items: center;
        transition: color 0.3s;
    }

    .page-sub {
        font-size: 14px;
        color: var(--text-color-secondary);
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
        color: var(--text-color-secondary);
        transition: color 0.3s;

        .ld {
            width: 16px;
            height: 16px;
            border-radius: 4px;
            border: 1px solid rgba(128, 128, 128, 0.3);
        }

        .theme-cyan {
            background: rgba(0, 242, 255, 0.2);
            border-color: var(--primary-color);
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
            background: rgba(128, 128, 128, 0.1);
            border-style: dashed;
            border-color: var(--text-color-placeholder);
        }
    }
}

.flow-wrapper {
    flex: 1;
    // ... existing code ...
    background: var(--card-bg-color);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    overflow: hidden;
    position: relative;
    box-shadow: var(--box-shadow-base);
    transition: background-color 0.3s, border-color 0.3s;

    .cyber-vue-flow {
        width: 100%;
        height: 100%;
        background: transparent;
    }
}

:deep(.vue-flow__edge-path) {
    // 连线颜色动态调整较复杂，这里使用默认值，或通过 JS 监听主题更新 elements
    filter: drop-shadow(0 0 2px currentColor);
}

.cyber-node {
    display: flex;
    flex-direction: column;
    text-align: center;
    padding: 10px;
    border-radius: 8px;
    box-sizing: border-box;
    width: 170px;
    height: 100px;
    // 默认深色背景，浅色主题下通过 CSS 变量覆盖
    background: var(--card-bg-color); 
    border: 2px solid var(--border-color);
    box-shadow: var(--box-shadow-base);
    color: var(--text-color-primary);
    cursor: pointer;
    transition: all 0.2s ease;

    .node-icon-box {
        font-size: 20px;
        margin: 0 auto 4px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: var(--primary-color); // 默认使用主色
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
        color: var(--text-color-secondary);
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
        color: var(--text-color-secondary);
        background: rgba(128, 128, 128, 0.05);

        .node-icon-box {
            color: var(--text-color-placeholder);
        }

        &:hover {
            transform: none;
            box-shadow: var(--box-shadow-base);
        }
    }

    &.type-action,
    &.type-tool,
    &.type-core {
        &:hover {
            transform: translateY(-2px);
            z-index: 100;
            border-color: var(--primary-color);
            box-shadow: 0 0 15px rgba(0, 179, 219, 0.2);
        }
    }

    &.type-core {
        border-width: 3px;
    }
}

// 主题色修饰符 (保持特定业务颜色，但调整边框和光效以适应背景)
@mixin cyber-node-theme($name, $color) {
    .cyber-node.theme-#{$name} {
        border-color: rgba($color, 0.4);

        .node-icon-box {
            color: $color;
            // 浅色模式下减弱光效
            text-shadow: 0 0 5px rgba($color, 0.3);
        }

        &:hover {
            border-color: $color;
            // 动态光效
            box-shadow: 0 0 15px rgba($color, 0.2); 
        }

        &.type-core {
            border-width: 2px;
            border-color: $color;
            box-shadow: 0 0 10px rgba($color, 0.1);
        }
    }

    .text-#{$name} {
        color: $color;
        font-size: 48px;
        // 浅色模式下减弱光效
        filter: drop-shadow(0 0 5px rgba($color, 0.3));
        margin-bottom: 15px;
    }
}

@include cyber-node-theme('cyan', #00b3db); // 使用主题色变量中的主色近似值
@include cyber-node-theme('green', #3fb950);
@include cyber-node-theme('orange', #e3b341);
@include cyber-node-theme('red', #f56c6c);
@include cyber-node-theme('purple', #bc8cff);
@include cyber-node-theme('gray', #909399);

.modal-content {
    text-align: center;
    padding: 10px 0 20px;

    .modal-title {
        font-size: 20px;
        font-weight: 800;
        color: var(--text-color-primary);
        margin-bottom: 15px;
        letter-spacing: 1px;
        transition: color 0.3s;
    }

    .modal-desc {
        font-size: 14px;
        color: var(--text-color-secondary);
        line-height: 1.6;
        text-align: left;
        background: rgba(128, 128, 128, 0.05);
        padding: 15px;
        border-radius: 6px;
        border: 1px solid var(--border-color);
        margin-bottom: 20px;
        transition: color 0.3s, background-color 0.3s, border-color 0.3s;
    }

    .modal-meta {
        font-size: 12px;
        text-align: left;
        display: flex;
        align-items: center;
        background: rgba(128, 128, 128, 0.05);
        padding: 8px 12px;
        border-radius: 4px;
        border-left: 3px solid var(--primary-color);
        transition: background-color 0.3s, border-color 0.3s;

        .meta-label {
            color: var(--text-color-secondary);
            margin-right: 10px;
        }

        .meta-value {
            color: var(--primary-color);
            font-family: monospace;
        }
    }
}

:deep(.portal-modal) {
    background-color: var(--card-bg-color) !important;
    border: 1px solid var(--border-color) !important;
    box-shadow: var(--box-shadow-base) !important;
    border-radius: 12px;
    transition: background-color 0.3s, border-color 0.3s;

    .el-dialog__header {
        border-bottom: 1px solid var(--border-color);
        margin-right: 0;
        padding-bottom: 15px;
    }

    .el-dialog__title {
        color: var(--text-color-primary);
        font-weight: bold;
        font-size: 14px;
    }
    
    .el-dialog__headerbtn .el-dialog__close {
        color: var(--text-color-secondary);
        &:hover {
            color: var(--primary-color);
        }
    }

    .el-dialog__footer {
        border-top: 1px solid var(--border-color);
        padding-top: 15px;
    }
}

.cyber-btn-cancel {
    background: transparent;
    border: 1px solid var(--border-color);
    color: var(--text-color-secondary);

    &:hover {
        background: rgba(128, 128, 128, 0.1);
        color: var(--text-color-primary);
        border-color: var(--primary-color);
    }
}

.cyber-btn-confirm {
    background: var(--primary-color);
    border: 1px solid var(--primary-color);
    color: #fff;
    font-weight: bold;
    letter-spacing: 1px;

    &:hover {
        filter: brightness(1.1);
        box-shadow: 0 0 15px rgba(0, 179, 219, 0.4);
    }
}
</style>