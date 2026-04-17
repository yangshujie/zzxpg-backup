<template>
  <div class="cesium-placeholder">
    <div class="cesium-placeholder__grid"></div>
    <div class="cesium-placeholder__glow"></div>
    <div class="cesium-placeholder__hud">
      <div class="hud-title">态势地图占位视图</div>
      <div class="hud-subtitle">当前项目未包含 Cesium 组件实现，先以可编译的可视占位层承接 UI 结构。</div>
      <div class="hud-tags">
        <span>空间范围</span>
        <span>任务边界</span>
        <span>风险区标识</span>
      </div>
    </div>
    <div class="orbit orbit--1"></div>
    <div class="orbit orbit--2"></div>
    <div class="marker marker--a">A</div>
    <div class="marker marker--b">B</div>
    <div class="marker marker--c">C</div>
  </div>
</template>

<script setup>
</script>

<style scoped lang="scss">
.cesium-placeholder {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 420px;
  overflow: hidden;
  background:
    radial-gradient(circle at 20% 20%, rgba(34, 211, 238, 0.16), transparent 26%),
    radial-gradient(circle at 80% 30%, rgba(59, 130, 246, 0.18), transparent 30%),
    linear-gradient(180deg, color-mix(in srgb, var(--mc-bg) 92%, #07111f), color-mix(in srgb, var(--mc-bg) 86%, #040913));
}

.cesium-placeholder__grid,
.cesium-placeholder__glow {
  position: absolute;
  inset: 0;
}

.cesium-placeholder__grid {
  background-image:
    linear-gradient(rgba(127, 156, 201, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(127, 156, 201, 0.08) 1px, transparent 1px);
  background-size: 48px 48px;
  opacity: 0.45;
}

.cesium-placeholder__glow {
  background:
    radial-gradient(circle at center, rgba(34, 211, 238, 0.18), transparent 34%),
    radial-gradient(circle at center, rgba(34, 211, 238, 0.08), transparent 52%);
}

.cesium-placeholder__hud {
  position: absolute;
  left: 24px;
  bottom: 24px;
  width: min(420px, calc(100% - 48px));
  padding: 18px;
  border-radius: 18px;
  border: 1px solid rgba(86, 122, 173, 0.2);
  background: color-mix(in srgb, var(--mc-bg-elevated) 74%, transparent);
  backdrop-filter: blur(14px);
}

.hud-title {
  color: #edf4ff;
  font-size: 18px;
  font-weight: 700;
}

.hud-subtitle {
  margin-top: 8px;
  color: #8ea7c7;
  font-size: 13px;
  line-height: 1.7;
}

.hud-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;

  span {
    padding: 8px 12px;
    border-radius: 999px;
    border: 1px solid rgba(86, 122, 173, 0.18);
    background: color-mix(in srgb, var(--mc-bg-elevated) 82%, transparent);
    color: #8ea7c7;
    font-size: 12px;
  }
}

.orbit {
  position: absolute;
  border: 1px solid rgba(34, 211, 238, 0.18);
  border-radius: 50%;
  box-shadow: 0 0 32px rgba(34, 211, 238, 0.08);
}

.orbit--1 {
  width: 320px;
  height: 320px;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

.orbit--2 {
  width: 460px;
  height: 460px;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

.marker {
  position: absolute;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  color: #edf4ff;
  background: linear-gradient(135deg, #0891b2, #22d3ee);
  box-shadow: 0 0 18px rgba(34, 211, 238, 0.28);
}

.marker--a {
  top: 25%;
  left: 38%;
}

.marker--b {
  top: 48%;
  left: 62%;
}

.marker--c {
  top: 68%;
  left: 46%;
}
</style>
