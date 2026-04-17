<template>
  <div class="global-app-layout dark-theme">
    <SideLayout>
      <!-- Middle: Dynamic Router Content -->
      <template #middle>
        <div class="middle-wrapper">
          <router-view v-slot="{ Component }">
            <transition name="fade-transform" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </template>

      <!-- Right: Notifications (Only for UnifiedHome) -->
      <template #right v-if="isHome">
        <div class="side-content">
          <div class="side-header">
            <el-icon>
              <ChatDotRound />
            </el-icon>
            <span>实时消息通知</span>
          </div>
          <div class="notification-list">
            <div v-for="note in notifications" :key="note.id" class="note-item">
              <div class="note-dot" :class="note.level"></div>
              <div class="note-body">
                <div class="note-text">{{ note.content }}</div>
                <div class="note-time">{{ note.time }}</div>
              </div>
            </div>
          </div>
          <div class="side-footer">
            <el-button link type="primary">查看全部消息</el-button>
          </div>
        </div>
      </template>
    </SideLayout>

    <!-- Floating Left Sidebar -->
    <div class="floating-sidebar" :class="{ 'is-expanded': isSidebarExpanded }">
      <div class="side-content">
        <div class="side-header">
          <el-icon>
            <Fold />
          </el-icon>
          <span>系统功能目录</span>
        </div>
        <div class="directory-container">
          <el-tree :data="menuData" :props="defaultProps" @node-click="handleNodeClick" class="cyber-tree"
            highlight-current default-expand-all>
            <template #default="{ node, data }">
              <span class="custom-tree-node"
                :class="{ 'is-disabled': !data.path && (!data.children || data.children.length === 0) }">
                <el-icon v-if="data.icon">
                  <component :is="data.icon" />
                </el-icon>
                <span class="node-label">{{ node.label }}</span>
              </span>
            </template>
          </el-tree>
        </div>
      </div>
    </div>

    <!-- Sidebar Toggle Background Overlay (Optional) -->
    <div v-if="isSidebarExpanded" class="sidebar-overlay" @click="isSidebarExpanded = false"></div>

    <!-- Toggle Button -->
    <div class="sidebar-toggle" :class="{ 'is-expanded': isSidebarExpanded }"
      @click="isSidebarExpanded = !isSidebarExpanded">
      <el-icon>
        <Fold v-if="isSidebarExpanded" />
        <Expand v-else />
      </el-icon>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import SideLayout from '@/layout/SideLayout.vue'
import {
  Fold, Expand, ChatDotRound, Management, Connection, Document, Coin, Search, Setting
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const isHome = computed(() => route.path === '/unified-home' || route.name === 'UnifiedHome')
const isSidebarExpanded = ref(false)

// Left: Directory Data
const defaultProps = {
  children: 'children',
  label: 'label',
}
const menuData = [
  {
    label: '任务管理',
    icon: Management,
    children: [
      { label: '评估工程管理', path: '/major/program-mgmt/list' },
      { label: '超网络构建', path: '/major/program-mgmt/super-network' },
      { label: '需求设计管理', path: '/major/requirement-sys/portal' },
      { label: '辅助计算', path: '' },
      { label: '任务骨架构建', path: '/process/skeleton-sys/builder' },
      { label: '预处理任务管理', path: '/process/process-sys/tasks' },
      { label: '汇总整编任务管理', path: '/process/analysis-sys/tasks' },
      { label: '数据分析挖掘任务管理', path: '/process/mining-sys/tasks' },
      { label: '评估计算及结果管理', path: '/major/program-mgmt/eval-result' },
    ],
  },
  {
    label: '模板库管理',
    icon: Document,
    children: [
      { label: '预处理流程模板管理', path: '/process/process-sys/templates' },
      { label: '任务模板管理', path: '' },
      { label: '综合评估流程模板管理', path: '/major/template-library/calc-flow' },
      { label: '需求设计流程模板管理', path: '/major/requirement-sys/template/flow' },
      { label: '需求分析结果模板管理', path: '' },
      { label: '需求方案模板管理', path: '' },
      { label: '评估报告模板管理', path: '/major/template-library/report' },
    ],
  },
  {
    label: '能力资源底座',
    icon: Coin,
    children: [
      { label: '装备管理', path: '/major/program-mgmt/equipment' },
      { label: '装备考核履历管理', path: '' },
      { label: '指标管理', path: '/major/capability-resource/indicator' },
      { label: '指标体系管理', path: '/major/capability-resource/indicator-system' },
      {
        label: '算法管理', path: '/major/capability-resource/algorithm',
        // children: [{ label: '算子新建', path: '/process/process-sys/operators' }]
      },
      { label: '模型管理', path: '/process/mining-sys/models' },
      { label: '评估报告管理', path: '/major/capability-resource/report' },
      { label: '数据源接口管理', path: '/process/analysis-sys/interfaces' },
      { label: '采集表管理', path: '/process/reception-sys/forms' },
      { label: '骨架管理', path: '/process/skeleton-sys/overview' },
      { label: '综合评估模型应用案例管理', path: '' },
      { label: '知识管理', path: '/process/knowledge-sys/management' },
      { label: '资源管理', path: '/process/resource-sys' },
    ],
  },
  {
    label: '知识库构建与管理分系统',
    icon: Search,
    children: [
      { label: '本体知识构建', path: '/process/knowledge-sys/ontology' },
      { label: '知识关联分析', path: '/process/knowledge-sys/association' },
      { label: '体系评估态势构建与管理', path: '/process/knowledge-sys/situation' },
      { label: '评估全链路知识关联追溯管理', path: '/process/knowledge-sys/traceability' },
      { label: '装备评估全寿期数据资源态势一览', path: '/process/knowledge-sys/lifecycle' },
    ],
  },
  {
    label: '体系协同',
    icon: Connection,
    children: [
      { label: '体系协同评估工程管理（不计入页面）', path: '' },
      { label: '体系作战剖面', path: '/major/system-cooperation/combat-profile' },
      {
        label: '装备体系力量',
        children: [
          { label: '结构构型管理', path: '/major/system-cooperation/structure-config' },
          { label: '力量结构网管理', path: '/major/system-cooperation/power-network' },
        ]
      },
      {
        label: '体系作战任务',
        children: [
          { label: '任务判据管理', path: '/major/system-cooperation/task-criterion' },
          { label: '任务管理', path: '/major/system-cooperation/task-mgmt' },
          { label: '任务网管理', path: '/major/system-cooperation/task-network' },
        ]
      },
      {
        label: 'OODA链路',
        children: [
          { label: '作战活动管理', path: '/major/system-cooperation/combat-activity' },
          { label: '装备协同组合运用策略管理', path: '/major/system-cooperation/coordination-strategy' },
          { label: '协同作战OODA链路管理', path: '/major/system-cooperation/ooda-link' },
          { label: 'OODA网管理', path: '/major/system-cooperation/ooda-network' },
        ]
      },
      { label: '指标网管理', path: '/major/system-cooperation/indicator-network' },
      { label: '算法网管理', path: '/major/system-cooperation/algorithm-network' },
      { label: '评估方案管理', path: '/major/system-cooperation/evaluation-plan' },
      { label: '效能射表管理', path: '/major/system-cooperation/effectiveness-table' },
      { label: '装备体系作战运用策略管理', path: '/major/system-cooperation/equipment-strategy' },
    ],
  },
  { label: '装备型谱', icon: Document, path: '' },
  { label: '数字展览馆', icon: Document, path: '' },
  {
    label: '系统管理',
    icon: Setting,
    children: [
      { label: '用户管理', path: '' },
      { label: '角色管理', path: '' },
      { label: '权限管理', path: '' },
      { label: '菜单管理', path: '' },
      { label: '布局管理', path: '' },
      { label: '日志管理', path: '' },
      { label: '公共信息管理', path: '' },
    ],
  },
]

const handleNodeClick = (data) => {
  if (data.path) {
    router.push(data.path)
  } else if (!data.children || data.children.length === 0) {
    ElMessage.info('暂未开放，敬请期待')
  }
}

// Right: Notifications
const notifications = ref([
  { id: 1, content: '系统自动拦截了一次针对 API 接口的异常访问请求。', time: '10:24', level: 'danger' },
  { id: 2, content: '您的“任务书导入”操作已通过后台校验，请继续生成。', time: '09:45', level: 'success' },
  { id: 3, content: '管理员更新了“评估准则模板-3号”，请注意查看。', time: '昨天', level: 'info' },
  { id: 4, content: '分中心上报了新的采集数据，待汇总核验。', time: '昨天', level: 'warning' },
  { id: 5, content: '知识库实体关系自动推演完成，新增链路158条。', time: '前天', level: 'info' },
])
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.global-app-layout {
  position: relative;
  height: 100vh;
  width: 100%;
  background-color: #02060c;
  overflow: hidden;
}

.floating-sidebar {
  position: absolute;
  top: 0;
  left: -280px;
  width: 280px;
  height: 100%;
  background: rgba(13, 20, 31, 0.95);
  backdrop-filter: blur(20px);
  border-right: 1px solid rgba(0, 242, 255, 0.3);
  box-shadow: 10px 0 40px rgba(0, 0, 0, 0.6);
  transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  z-index: 2000;

  &.is-expanded {
    transform: translateX(280px);
  }
}

.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.3);
  z-index: 1999;
}

.sidebar-toggle {
  position: absolute;
  top: 15px;
  left: 20px;
  width: 40px;
  height: 40px;
  background: rgba(13, 20, 31, 0.8);
  border: 1px solid rgba(0, 242, 255, 0.3);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #00f2ff;
  font-size: 20px;
  cursor: pointer;
  z-index: 2001;
  /* Above sidebar */
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.3);

  &:hover {
    background: #00f2ff;
    color: #000;
    box-shadow: 0 0 15px rgba(0, 242, 255, 0.5);
  }

  &.is-expanded {
    left: 295px;
  }
}

.middle-wrapper {
  height: 100%;
  width: 100%;
  position: relative;
  overflow: hidden;
  border-radius: 8px;
  background: rgba(10, 21, 37, 0.4);
  box-shadow: inset 0 0 20px rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(0, 242, 255, 0.1);
}

.side-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.side-header {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #00f2ff;
  font-weight: bold;
  font-size: 16px;
  border-bottom: 1px solid rgba(0, 242, 255, 0.1);
  background: rgba(0, 242, 255, 0.03);
  flex-shrink: 0;
}

.directory-container {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.cyber-tree {
  background: transparent;
  color: #cdd9e5;

  :deep(.el-tree-node__content) {
    height: 40px;
    margin-bottom: 4px;
    border-radius: 4px;
    transition: all 0.3s;

    &:hover {
      background: rgba(0, 242, 255, 0.1);
      color: #00f2ff;
    }
  }

  :deep(.el-tree-node.is-current > .el-tree-node__content) {
    background: rgba(0, 242, 255, 0.15) !important;
    color: #00f2ff !important;
    border-left: 3px solid #00f2ff;
  }
}

.custom-tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  width: 100%;

  &.is-disabled {
    color: #4e5d6c !important;
    cursor: not-allowed;
    filter: grayscale(1);
    opacity: 0.6;
  }
}

.notification-list {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
}

.note-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);

  &:last-child {
    border-bottom: none;
  }

  .note-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    margin-top: 5px;
    flex-shrink: 0;

    &.info {
      background: #5d6d7e;
    }

    &.success {
      background: #3fb950;
      box-shadow: 0 0 5px #3fb950;
    }

    &.warning {
      background: #e3b341;
      box-shadow: 0 0 5px #e3b341;
    }

    &.danger {
      background: #f85149;
      box-shadow: 0 0 10px #f85149;
    }
  }

  .note-body {
    flex: 1;

    .note-text {
      font-size: 13px;
      color: #cdd9e5;
      line-height: 1.5;
      margin-bottom: 4px;
    }

    .note-time {
      font-size: 11px;
      color: #5d6d7e;
    }
  }
}

.side-footer {
  padding: 15px;
  text-align: center;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  flex-shrink: 0;
}

/* Transition */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s cubic-bezier(0.55, 0, 0.1, 1);
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(10px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

// Custom Scrollbar
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-thumb {
  background: rgba(0, 242, 255, 0.15);
  border-radius: 3px;
}

::-webkit-scrollbar-track {
  background: transparent;
}
</style>
