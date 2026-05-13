<template>
  <div class="sub-app-container">
    <micro-app v-if="url" :name="appName" :url="url" :baseroute="route.path" iframe keep-alive inline></micro-app>
    <div v-else class="empty-state">
      <el-empty :description="`未配置子应用 [${appName}] 的访问地址`">
        <template #extra>
          <p class="hint">请在 <code>src/views/SubAppHost.vue</code> 中维护 <code>appConfig</code> 映射表</p>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const appName = computed(() => route.params.appName)

/**
 * 子应用地址映射表
 * 建议后续迁移至环境变量或后端配置
 */
const appConfig = {
  'system-analysis': 'http://localhost:4000/monitor',
}

const url = computed(() => appConfig[appName.value] || '')
</script>

<style scoped>
.sub-app-container {
  width: 100%;
  height: calc(100vh - 50px);
  background: #0d1117;
  overflow: hidden;
}

.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hint {
  color: #7d8590;
  font-size: 13px;
  margin-top: 10px;
}

code {
  background: rgba(110, 118, 129, 0.4);
  padding: 2px 4px;
  border-radius: 4px;
  color: #e6edf3;
}
</style>
