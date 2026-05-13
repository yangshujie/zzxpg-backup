<template>
    <!-- MicroApp 子应用环境：跳过顶导航布局，直接渲染内容 -->
    <router-view v-if="isMicroApp" />

    <div v-else class="top-nav-layout">

        <header class="subsystem-header">
            <div class="header-left">
                <div class="back-home" @click="goToPortal">
                    <el-icon>
                        <HomeFilled />
                    </el-icon>
                </div>
                <div class="divider"></div>
                <h2 class="subsystem-title">{{ subsystemTitle }}</h2>
            </div>

            <!-- <nav class="header-nav">
                <el-menu :default-active="activeMenu" mode="horizontal" background-color="transparent"
                    text-color="#8fa3b8" active-text-color="#00f2ff" router class="top-menu">
                    <el-menu-item v-for="item in currentMenuItems" :key="item.index" :index="item.index">
                        {{ item.label }}
                    </el-menu-item>
                </el-menu>
            </nav> -->

            <div class="header-right">
                <AuxiliaryCalculation />

                <div class="status-indicator">
                    <span class="dot"></span>
                    LINK SECURE
                </div>
                <div class="theme-switch-container">
                  <ThemeSwitcher />
                </div>
                <el-avatar :size="32" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
            </div>
        </header>

        <main class="subsystem-main">
            <router-view v-slot="{ Component, route }">
                <component :is="Component" :key="route.fullPath" />
            </router-view>
        </main>
    </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled } from '@element-plus/icons-vue'
import ThemeSwitcher from '@/components/ThemeSwitcher'
import { useThemeStore } from '@/store/theme'
import AuxiliaryCalculation from '@/components/AuxiliaryCalculation/index.vue'

// 检测是否作为 MicroApp 子应用运行
const isMicroApp = computed(() => !!window.__MICRO_APP_ENVIRONMENT__)


const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)

// Subsystem Configurations
const subsystemConfig = {
    '/major/program-mgmt': {
        title: '评估工程管理',
        menus: [
        ]
    },
    '/process/reception-sys': {
        title: '数据采集分发分系统',
        menus: [
            { label: '状态概览', index: '/process/reception-sys/overview' },
            { label: '评估需求列表', index: '/process/reception-sys/requirements' },
            { label: '表单模板库', index: '/process/reception-sys/templates' },
            { label: '采集表单列表', index: '/process/reception-sys/forms' },
            { label: '表单设计器', index: '/process/reception-sys/designer' }
        ]
    },
    '/process/analysis-sys': {
        title: '汇总整理分系统',
        menus: [
            { label: '概览', index: '/process/analysis-sys/overview' },
            { label: '汇总整编任务管理', index: '/process/analysis-sys/tasks' },
            { label: '数据源接口管理', index: '/process/analysis-sys/interfaces' },
            { label: '数据目录管理', index: '/process/analysis-sys/catalogs' },
            { label: '数据集管理', index: '/process/analysis-sys/datasets' }
        ]
    },
    '/process/process-sys': {
        title: '数据预处理分系统',
        menus: [
            { label: '预处理任务管理', index: '/process/process-sys/tasks' },
            { label: '算子管理', index: '/process/process-sys/operators' },
            { label: '预处理流程模板', index: '/process/process-sys/templates' }
        ]
    },
    '/process/mining-sys': {
        title: '数据分析挖掘分系统',
        menus: [
            { label: '数据分析挖掘任务管理', index: '/process/mining-sys/tasks' },
            { label: '评估数据分析挖掘算法模型管理', index: '/process/mining-sys/models' }
        ]
    },
    '/process/resource-sys': {
        title: '资源管理',
        menus: [
            { label: '资源管理', index: '/process/resource-sys/overview' },
        ]
    },
    '/process/knowledge-sys': {
        title: '知识库构建与管理分系统',
        menus: [
            { label: '本体知识构建', index: '/process/knowledge-sys/ontology' },
            { label: '知识管理', index: '/process/knowledge-sys/management' },
            { label: '知识关联分析', index: '/process/knowledge-sys/association' },
            { label: '体系评估态势构建与管理', index: '/process/knowledge-sys/situation' },
            { label: '评估全链路知识关联追溯管理', index: '/process/knowledge-sys/traceability' },
            { label: '装备评估全寿期数据资源态势', index: '/process/knowledge-sys/lifecycle' }
        ]
    },
    '/major/requirement-sys': {
        title: '评估需求设计分系统',
        menus: [
            { label: '流程导航', index: '/major/requirement-sys/portal' },
            { label: '需求管理', index: '/major/requirement-sys/import' },
            { label: '支持工具', index: '/major/requirement-sys/template/flow' }
        ]
    }
}

// Compute current subsystem based on route path
const currentSubsystem = computed(() => {
    const path = route.path
    if (path.startsWith('/major/program-mgmt')) return '/major/program-mgmt'
    if (path.startsWith('/process/reception-sys')) return '/process/reception-sys'
    if (path.startsWith('/process/analysis-sys')) return '/process/analysis-sys'
    if (path.startsWith('/process/process-sys')) return '/process/process-sys'
    if (path.startsWith('/process/mining-sys')) return '/process/mining-sys'
    if (path.startsWith('/process/resource-sys')) return '/process/resource-sys'
    if (path.startsWith('/process/knowledge-sys')) return '/process/knowledge-sys'
    if (path.startsWith('/major/requirement-sys')) return '/major/requirement-sys'
    return ''
})

const subsystemTitle = computed(() => {
    return route.meta?.title || '分系统'
})

const currentMenuItems = computed(() => {
    return subsystemConfig[currentSubsystem.value]?.menus || []
})

const goToPortal = () => {
    // 基于新前缀结构简化判断逻辑
    router.push('/unified-home')
    // if (route.path.startsWith('/major')) {
    //     router.push('/major/new-portal')
    // } else {
    //     router.push('/process/portal')
    // }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.top-nav-layout {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    background-color: #060B13;
    color: #fff;
}

.subsystem-header {
    height: 70px;
    background: rgba(11, 26, 48, 0.95);
    border-bottom: 1px solid rgba(0, 242, 255, 0.2);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 40px;
    z-index: 100;
    backdrop-filter: blur(10px);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
}

.right-actions {
    display: flex;
    align-items: center;
    gap: 20px;
}

.header-left {
    display: flex;
    align-items: center;
    gap: 20px;

    .back-home {
        width: 36px;
        height: 36px;
        background: rgba(0, 242, 255, 0.1);
        border: 1px solid rgba(0, 242, 255, 0.3);
        border-radius: 50%;
        display: flex;
        justify-content: center;
        align-items: center;
        cursor: pointer;
        color: #00f2ff;
        transition: all 0.3s;

        &:hover {
            background: #00f2ff;
            color: #000;
            box-shadow: 0 0 15px rgba(0, 242, 255, 0.5);
        }
    }

    .divider {
        width: 1px;
        height: 24px;
        background: rgba(255, 255, 255, 0.1);
    }

    .subsystem-title {
        margin: 0;
        font-size: 20px;
        font-weight: 800;
        letter-spacing: 2px;
        background: linear-gradient(to right, #fff, #00f2ff);
        -webkit-background-clip: text;
        background-clip: text;
        -webkit-text-fill-color: transparent;
    }
}

.header-nav {
    flex: 1;
    display: flex;
    justify-content: center;
}

.top-menu {
    border-bottom: none !important;
    height: 70px;

    :deep(.el-menu-item) {
        height: 70px;
        line-height: 70px;
        font-size: 15px;
        letter-spacing: 1px;
        border-bottom: 2px solid transparent !important;

        &:hover {
            background: transparent !important;
            color: #00f2ff !important;
        }

        &.is-active {
            background: transparent !important;
            border-bottom-color: #00f2ff !important;
            text-shadow: 0 0 10px rgba(0, 242, 255, 0.5);
        }
    }
}

.header-right {
    display: flex;
    align-items: center;
    gap: 25px;

    .status-indicator {
        font-size: 10px;
        color: #00f2ff;
        letter-spacing: 2px;
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 4px 12px;
        border: 1px solid rgba(0, 242, 255, 0.2);
        border-radius: 4px;

        .dot {
            width: 6px;
            height: 6px;
            background: #00f2ff;
            border-radius: 50%;
            box-shadow: 0 0 8px #00f2ff;
            animation: blink 2s infinite;
        }
    }
}

.subsystem-main {
    flex: 1;
    overflow: hidden;
    position: relative;
    background: var(--bg-color);
    padding: 20px;
}

@keyframes blink {

    0%,
    100% {
        opacity: 1;
    }

    50% {
        opacity: 0.4;
    }
}

/* Transition */
.fade-transform-enter-active,
.fade-transform-leave-active {
    transition: all 0.3s cubic-bezier(0.55, 0, 0.1, 1);
}

.fade-transform-enter-from {
    opacity: 0;
    transform: translateY(10px);
}

.fade-transform-leave-to {
    opacity: 0;
    transform: translateY(-10px);
}
</style>
