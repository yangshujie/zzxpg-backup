<template>
  <div class="app-wrapper">
    <div class="sidebar-container">
      <div class="logo">
        <span class="logo-text">需求设计系统</span>
      </div>
      <el-menu :default-active="activeMenu" class="el-menu-vertical" background-color="transparent" text-color="#8fa3b8"
        active-text-color="#00f2ff" router>
        <el-sub-menu index="/acceptance">
          <template #title>
            <el-icon>
              <Connection />
            </el-icon>
            <span>需求受理与反馈</span>
          </template>
          <el-menu-item index="/reception">需求接收</el-menu-item>
          <el-menu-item index="/analysis">需求解析</el-menu-item>
          <el-menu-item index="/template">模板设计</el-menu-item>
          <el-menu-item index="/feedback">受理反馈</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/design">
          <template #title>
            <el-icon>
              <Edit />
            </el-icon>
            <span>总体设计</span>
          </template>
          <el-menu-item index="/design/task">评估任务信息设计</el-menu-item>
          <el-menu-item index="/design/indicator">评估指标体系设计</el-menu-item>
          <el-menu-item index="/design/model">评估模型设计</el-menu-item>
          <el-menu-item index="/design/criteria">评估准则设计</el-menu-item>
          <el-menu-item index="/design/algorithm">评估算法设计</el-menu-item>
          <el-menu-item index="/design/collection">评估数据采集设计</el-menu-item>
          <el-menu-item index="/design/scheme">评估方案生成</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/monitor">
          <template #title>
            <el-icon>
              <Monitor />
            </el-icon>
            <span>任务监控</span>
          </template>
          <el-menu-item index="/monitor/status">评估任务状态监控</el-menu-item>
          <el-menu-item index="/monitor/process">评估流程配置执行</el-menu-item>
          <el-menu-item index="/monitor/warning">异常检测预警</el-menu-item>
          <el-menu-item index="/monitor/manual">人工干预控制</el-menu-item>
          <el-menu-item index="/monitor/sharing">评估任务状态数据共享</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/resource">
          <template #title>
            <el-icon>
              <Files />
            </el-icon>
            <span>资源管理</span>
          </template>
          <el-menu-item index="/resource/data">数据管理</el-menu-item>
          <el-menu-item index="/resource/model">模型算法库</el-menu-item>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/role">角色管理</el-menu-item>
          <el-menu-item index="/system/log">日志管理</el-menu-item>
          <el-menu-item index="/system/dict">数据字典</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </div>
    <div class="main-container">
      <div class="header">
        <div class="breadcrumb">
          <!-- Optional generic breadcrumb or title -->
          <h2>{{ currentRouteName }}</h2>
        </div>
        <div class="header-right">
          <el-badge is-dot class="item">
            <el-icon>
              <Bell />
            </el-icon>
          </el-badge>
          <div class="user-info">
            <el-avatar :size="30" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
            <span>Admin</span>
          </div>
        </div>
      </div>
      <div class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Document, DataAnalysis, SetUp, Connection, Bell, Edit, Monitor, Files } from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
const currentRouteName = computed(() => route.meta.title || 'Dashboard')

</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.app-wrapper {
  display: flex;
  height: 100vh;
  width: 100%;
  background-color: $bg-color;
}

.sidebar-container {
  width: $sider-width;
  background-color: $sider-bg-color;
  border-right: 1px solid $border-color;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;

  .logo {
    height: $header-height;
    display: flex;
    align-items: center;
    justify-content: center;
    border-bottom: 1px solid $border-color;

    .logo-text {
      font-size: 20px;
      font-weight: bold;
      color: $text-color-primary;
      letter-spacing: 2px;

      .highlight {
        color: $primary-color;
      }
    }
  }
}

.el-menu-vertical {
  border-right: none;
  margin-top: 20px;

  :deep(.el-menu-item) {
    &:hover {
      background-color: rgba(0, 242, 255, 0.05);
    }

    &.is-active {
      background-color: rgba(0, 242, 255, 0.1);
      border-right: 2px solid $primary-color;
    }
  }
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  height: $header-height;
  background-color: $header-bg-color;
  border-bottom: 1px solid $border-color;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;

  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 500;
    color: $text-color-primary;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 20px;

    .el-icon {
      font-size: 20px;
      color: $text-color-primary;
      cursor: pointer;

      &:hover {
        color: $primary-color;
      }
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 10px;
      color: $text-color-primary;
      cursor: pointer;
    }
  }
}

.app-main {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  position: relative;
}

/* Transition */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(10px);
}
</style>
