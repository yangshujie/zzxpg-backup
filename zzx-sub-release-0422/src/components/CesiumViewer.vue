<template>
  <div id="cesium-container" class="cesium-viewer-container"></div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, nextTick } from 'vue'
import * as Cesium from 'cesium'
import { Warning } from '@element-plus/icons-vue'

// 设置Cesium静态资源路径
window.CESIUM_BASE_URL = '/node_modules/cesium/Build/Cesium/'

const viewer = ref(null)

onMounted(async () => {
  try {
    // 等待DOM完全更新
    await nextTick()
    
    // 确保容器有高度
    const container = document.getElementById('cesium-container')
    if (!container) return
    
    // 检查容器高度
    const containerHeight = container.clientHeight
    const containerWidth = container.clientWidth
    
    console.log('Cesium容器尺寸:', containerWidth, containerHeight)
    
    // 如果容器高度为0，设置一个默认高度
    if (containerHeight === 0) {
      container.style.height = '400px'
    }
    
    // 初始化Cesium Viewer - 使用本地静态资源
    viewer.value = new Cesium.Viewer('cesium-container', {
      // 基础配置
      animation: false,
      baseLayerPicker: false,
      fullscreenButton: false,
      geocoder: false,
      homeButton: false,
      infoBox: false,
      sceneModePicker: false,
      selectionIndicator: false,
      timeline: false,
      navigationHelpButton: false,
      // 使用Cesium内置的默认影像图层（本地静态资源）
      // 不使用地形数据，避免加载问题
      terrainProvider: undefined,
      // 场景配置
      scene3DOnly: true
    })

    // 设置场景选项
    const scene = viewer.value.scene
    scene.globe.show = true
    scene.skyAtmosphere.show = true
    scene.globe.baseColor = Cesium.Color.WHITE

    // 设置相机位置
    viewer.value.camera.setView({
      destination: Cesium.Cartesian3.fromDegrees(116.3974, 39.9093, 10000000)
    })

    // 监听影像图层加载错误（使用更安全的方式）
    const imageryLayer = viewer.value.scene.imageryLayers.get(0)
    if (imageryLayer && imageryLayer.imageryProvider && imageryLayer.imageryProvider.errorEvent) {
      imageryLayer.imageryProvider.errorEvent.addEventListener(() => {
        console.warn('Cesium默认图层加载失败，尝试使用简单颜色填充')
        // 移除当前图层
        viewer.value.scene.imageryLayers.removeAll()
        // 设置简单的地球颜色
        scene.globe.baseColor = Cesium.Color.BLUE.withAlpha(0.8)
      })
    }

    // 强制重新计算尺寸
    viewer.value.resize()
    
    // 添加一些示例数据
    addSampleData()
  } catch (error) {
    console.error('Cesium初始化失败:', error)
    // 如果Cesium初始化失败，显示错误信息
    const container = document.getElementById('cesium-container')
    if (container) {
      container.innerHTML = `
        <div style="display: flex; align-items: center; justify-content: center; height: 100%; color: #666; flex-direction: column;">
          <el-icon size="48" color="var(--el-color-warning)">
            <Warning />
          </el-icon>
          <h3>Cesium加载失败</h3>
          <p>${error.message}</p>
          <p style="font-size: 12px; margin-top: 10px;">请检查Cesium静态资源路径配置</p>
        </div>
      `
    }
  }
})

// 监听窗口大小变化
const handleResize = () => {
  if (viewer.value && !viewer.value.isDestroyed()) {
    viewer.value.resize()
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  // 移除事件监听
  window.removeEventListener('resize', handleResize)
  
  // 销毁Viewer
  if (viewer.value && !viewer.value.isDestroyed()) {
    viewer.value.destroy()
  }
})

// 添加示例数据
function addSampleData() {
  if (!viewer.value) return

  // 添加一个示例点
  const redPin = viewer.value.entities.add({
    name: '红方作战基地',
    position: Cesium.Cartesian3.fromDegrees(116.3974, 39.9093, 0),
    billboard: {
      image: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjI0IiBoZWlnaHQ9IjI0IiBmaWxsPSJyZWQiLz4KPHRleHQgeD0iMTIiIHk9IjEyIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSJ3aGl0ZSIgZm9udC1zaXplPSIxMiI+5pawPC90ZXh0Pgo8L3N2Zz4K',
      verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
      scale: 1.5
    },
    label: {
      text: '红方基地',
      font: '14pt sans-serif',
      fillColor: Cesium.Color.RED,
      outlineColor: Cesium.Color.WHITE,
      outlineWidth: 2,
      pixelOffset: new Cesium.Cartesian2(0, -40)
    }
  })

  // 添加另一个示例点
  const bluePin = viewer.value.entities.add({
    name: '蓝方作战基地',
    position: Cesium.Cartesian3.fromDegrees(121.4737, 31.2304, 0), // 上海
    billboard: {
      image: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjI0IiBoZWlnaHQ9IjI0IiBmaWxsPSIjMDAwMGZmIi8+Cjx0ZXh0IHg9IjEyIiB5PSIxMiIgZG9taW5hbnQtYmFzZWxpbmU9Im1pZGRsZSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0id2hpdGUiIGZvbnQtc2l6ZT0iMTIiPuW3tDwvdGV4dD4KPC9zdmc+Cg==',
      verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
      scale: 1.5
    },
    label: {
      text: '蓝方基地',
      font: '14pt sans-serif',
      fillColor: Cesium.Color.BLUE,
      outlineColor: Cesium.Color.WHITE,
      outlineWidth: 2,
      pixelOffset: new Cesium.Cartesian2(0, -40)
    }
  })

  // 添加一条示例连线
  const redToBlueLine = viewer.value.entities.add({
    name: '作战连线',
    polyline: {
      positions: [
        Cesium.Cartesian3.fromDegrees(116.3974, 39.9093, 0),
        Cesium.Cartesian3.fromDegrees(121.4737, 31.2304, 0)
      ],
      width: 3,
      material: new Cesium.PolylineArrowMaterialProperty(Cesium.Color.YELLOW)
    }
  })
}

// 暴露方法给父组件
const flyTo = (longitude, latitude, height = 10000000) => {
  if (viewer.value) {
    viewer.value.camera.flyTo({
      destination: Cesium.Cartesian3.fromDegrees(longitude, latitude, height),
      duration: 2.0
    })
  }
}

// 暴露给父组件
defineExpose({
  flyTo,
  viewer
})
</script>

<style scoped>
.cesium-viewer-container {
  width: 100%;
  height: 100%;
  min-height: 400px; /* 添加最小高度作为后备 */
  position: relative;
}

/* 确保Cesium Viewer正确显示 */
:deep(.cesium-widget-credits) {
  display: none !important;
}

:deep(.cesium-viewer) {
  width: 100%;
  height: 100%;
}

:deep(.cesium-viewer-cesiumWidgetContainer) {
  width: 100%;
  height: 100%;
}

:deep(.cesium-widget) {
  width: 100%;
  height: 100%;
}

:deep(.cesium-widget canvas) {
  width: 100% !important;
  height: 100% !important;
}
</style>