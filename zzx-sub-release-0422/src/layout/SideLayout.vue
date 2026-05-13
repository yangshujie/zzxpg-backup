<template>
  <div class="side-layout-container">
    <!-- Left Sidebar: Directory/Navigation -->
    <aside class="layout-side left-side" v-if="$slots.left">
      <slot name="left"></slot>
    </aside>

    <!-- Main Content: Carousel and Central Info -->
    <main class="layout-main">
      <slot name="middle"></slot>
    </main>

    <!-- Right Sidebar: Notices/Messages -->
    <aside class="layout-side right-side" v-if="$slots.right">
      <slot name="right"></slot>
    </aside>
  </div>
</template>

<script setup>
// SideLayout - A generic 3-column layout for dashboards
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.side-layout-container {
  display: flex;
  height: 100%;
  width: 100%;
  gap: 20px;
  // padding: 20px;
  box-sizing: border-box;
  background: radial-gradient(circle at 50% 50%, rgba(10, 25, 47, 0.4) 0%, rgba(1, 4, 9, 1) 100%);
  overflow: hidden;
}

.layout-side {
  height: 100%;
  background: rgba(13, 20, 31, 0.7);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(0, 242, 255, 0.1);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
}

.left-side {
  width: 280px;
  flex-shrink: 0;
}

.right-side {
  width: 320px;
  flex-shrink: 0;
}

.layout-main {
  flex: 1;
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

// Media queries for responsiveness if needed
@media (max-width: 1400px) {
  .left-side {
    width: 240px;
  }

  .right-side {
    width: 280px;
  }
}
</style>
