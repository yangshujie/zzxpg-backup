<template>
  <div class="prototype-canvas">
    <div class="system-container">
      <!-- 顶部栏：中心系统标识+用户 -->
      <div class="top-bar">
        <div class="logo-area">
          <div class="logo-box"><i class="fas fa-satellite"></i></div>
          <span class="system-title">中心系统</span>
        </div>
        <div class="user-area">
          <el-dropdown>
            <span class="user-info">
              <el-avatar size="small" :src="userAvatar" />
              <span class="username">管理员</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人信息</el-dropdown-item>
                <el-dropdown-item>修改密码</el-dropdown-item>
                <el-dropdown-item divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 主体左右布局 -->
      <div class="main-layout">
        <!-- 左侧一级导航：六个子系统 -->
        <div class="sidebar">
          <div class="nav-header"></div>
          <el-menu
            :default-active="$route.path"
            router
            class="sidebar-menu"
            background-color="#f9fbfd"
            text-color="#2a4053"
            active-text-color="#0e4770"
          >
            <el-menu-item index="/overview">
              <el-icon><Monitor /></el-icon>
              <span>系统概览</span>
            </el-menu-item>
            
            <el-sub-menu index="overall">
              <template #title>
                <el-icon><Setting /></el-icon>
                <span>总体子系统</span>
              </template>
              <el-menu-item index="/overall/user">用户管理</el-menu-item>
              <el-menu-item index="/overall/department">部门管理</el-menu-item>
              <el-menu-item index="/overall/project">评估工程管理</el-menu-item>
              <el-menu-item index="/overall/analysis">辅助计算分析</el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="data">
              <template #title>
                <el-icon><DataBoard /></el-icon>
                <span>数据子系统</span>
              </template>
              <el-menu-item index="/data/collection">评估数据采集配置</el-menu-item>
              <el-menu-item index="/data/knowledge">知识构建</el-menu-item>
              <el-menu-item index="/data/template">模版模型资源库</el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="task">
              <template #title>
                <el-icon><List /></el-icon>
                <span>任务子系统</span>
              </template>
              <el-menu-item index="/task/build">评估任务构建</el-menu-item>
              <el-menu-item index="/task/template">任务模板管理</el-menu-item>
              <el-menu-item index="/task/indicator">指标体系构建配置</el-menu-item>
              <el-menu-item index="/task/compute">综合评估计算</el-menu-item>
              <el-menu-item index="/task/analysis">评估结果智能分析系统</el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="visualization">
              <template #title>
                <el-icon><DataAnalysis /></el-icon>
                <span>可视化展示子系统</span>
              </template>
              <el-menu-item index="/visualization/scene">场景库列表</el-menu-item>
              <el-menu-item index="/visualization/equipment">zb数字化展览馆</el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="model">
              <template #title>
                <el-icon><TrendCharts /></el-icon>
                <span>评估模型构建子系统</span>
              </template>
              <el-menu-item index="/model/management">综合评估模型管理</el-menu-item>
              <el-menu-item index="/model/examples">模型典型应用样例管理</el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="algorithm">
              <template #title>
                <el-icon><Cpu /></el-icon>
                <span>体系协同指标算法构建子系统</span>
              </template>
              <el-menu-item index="/algorithm/scenes">体系协同指标算法构建子系统</el-menu-item>
            </el-sub-menu>
          </el-menu>
        </div>

        <!-- 右侧主内容区域 -->
        <div class="content-area">
          <!-- 面包屑导航 -->
          <div class="breadcrumb">
            <i class="fas fa-home"></i>
            <span>{{ currentSubsystem }}</span>
            <i v-if="currentPage" class="fas fa-chevron-right"></i>
            <span v-if="currentPage" class="current-page">{{ currentPage }}</span>
          </div>

          <!-- 路由内容 -->
          <router-view />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const userAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const currentSubsystem = computed(() => {
  const matched = route.matched.find(item => item.meta?.title)
  return matched?.meta.title || '中心系统'
})

const currentPage = computed(() => {
  if (route.path.includes('/overall/')) {
    const pageMap = {
      'user': '用户管理',
      'department': '部门管理',
      'project': '评估工程管理',
      'analysis': '辅助计算分析'
    }
    const pageKey = route.path.split('/').pop()
    return pageMap[pageKey] || ''
  }
  
  if (route.path.includes('/data/')) {
    const pageMap = {
      'collection': '评估数据采集配置',
      'knowledge': '知识构建',
      'template': '模版模型资源库'
    }
    const pageKey = route.path.split('/').pop()
    return pageMap[pageKey] || ''
  }
  
  if (route.path.includes('/task/')) {
    const pageMap = {
      'build': '评估任务构建',
      'template': '任务模板管理',
      'indicator': '指标体系构建配置',
      'compute': '综合评估计算',
      'analysis': '评估结果智能分析系统'
    }
    const pageKey = route.path.split('/').pop()
    return pageMap[pageKey] || ''
  }
  
  if (route.path.includes('/model/')) {
    const pageMap = {
      'management': '综合评估模型管理',
      'examples': '模型典型应用样例管理'
    }
    const pageKey = route.path.split('/').pop()
    return pageMap[pageKey] || ''
  }
  
  return ''
})
</script>

<style scoped>
/* ----- 全局原型风格：灰白背景+细边框+淡蓝点缀 ----- */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
}

.prototype-canvas {
  width: 100vw;
  height: 100vh;
  background-color: #2c3e50;
  display: flex;
  flex-direction: column;
  padding: 0;
}

.system-container {
  width: 100%;
  height: 100%;
  background-color: #f2f4f8;
  display: flex;
  flex-direction: column;
  border-radius: 0;
  box-shadow: none;
  padding: 0;
}

/* ----- 顶部栏 ----- */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 24px;
  background: white;
  border-bottom: 1px solid #e9ecf0;
  border-radius: 0;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-box {
  width: 36px;
  height: 36px;
  background: #e1e9f0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3a6ea5;
  font-size: 20px;
  border: 1px solid #bdcbd8;
}

.system-title {
  font-weight: 600;
  font-size: 18px;
  color: #1a2b3c;
  letter-spacing: 0.5px;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #4b5e6b;
  font-size: 15px;
}

.user-area i {
  cursor: pointer;
  transition: color 0.2s;
}

.user-area i:hover {
  color: #3a6ea5;
}

/* ----- 主体布局 ----- */
.main-layout {
  display: flex;
  flex: 1;
  min-height: 0;
  background: white;
  border-radius: 0;
  overflow: hidden;
}

/* ----- 左侧导航菜单 ----- */
.sidebar {
  width: 240px;
  background: #f9fbfd;
  border-right: 1px solid #e2e8f0;
  padding: 24px 0;
  overflow-y: auto;
}

.nav-header {
  padding: 0 20px 12px 20px;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: #6a7e8c;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 16px;
}

.sidebar-menu {
  border-right: none;
}

/* ----- 右侧主内容区域 ----- */
.content-area {
  flex: 1;
  background: white;
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

/* 面包屑导航 */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #5e6f7e;
  font-size: 14px;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf0f3;
}

.breadcrumb i {
  color: #7f99b0;
  font-size: 12px;
}

.current-page {
  background: #e2ecf5;
  padding: 5px 12px;
  border-radius: 30px;
  font-weight: 500;
  color: #1e5b8a;
}
</style>