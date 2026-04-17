<template>
  <div class="app-container">
    <el-result icon="info" title="功能已合并" sub-title="评估任务管理与评估结果管理已合并，请前往「评估结果管理」页面使用。">
      <template #extra>
        <el-button type="primary" @click="$router.push('/zhpg/pgjg')">前往评估结果管理</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
</script>
