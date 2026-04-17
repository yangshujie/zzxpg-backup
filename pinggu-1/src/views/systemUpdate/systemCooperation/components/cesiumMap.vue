<template>
  <div
    class="cesium-content"
    id="cesiumContainer"
    @contextmenu.prevent="onContextMenu"
  >
    <div class="clear-button" @click="handleClear" v-if="canDraw && !disabled">
      清除{{ props.type === 'Point' ? '所有点' : '区域' }}
    </div>
    <div class="mouse-position">
      {{
        mousePosition
          ? `${mousePosition.longitude}，${mousePosition.latitude}，${mousePosition.height}`
          : ""
      }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from "vue";
// import jsonData from '/public/data/mcc_satellite_orbit.json';
// import { orbitData } from "@/api/mission/request";
import * as Cesium from "cesium";
import { ElMessage } from "element-plus";
import { 
  initCesium, 
  CESIUM_ION_TOKEN, 
  TDT_TOKEN,
  getImageryProvider,
  getImageryProviderWithFallback,
  addChineseLabelsSafely,
  configureViewer
} from '@/utils/cesiumConfig';
const props = defineProps({
  type: {
    type: String,
    default: "Area"
  },
  disabled: {
    type: Boolean,
    default: false
  }
});
const emit = defineEmits(["clear", "selectPoints"]);
let viewer = {
  clockViewModel: {
    multiplier: 2,
  },
  clock: {
    shouldAnimate: "true",
  },
};

// 多边形全部的点
const polygonPoints = ref([]);
const tempPolygonEntity = ref(null);
const handler = ref(null);
let previousPoint = null;

const canDraw = ref(true);
const mousePosition = ref(null);
const isInitialized = ref(false);

// 多个点的坐标列表
const pointsList = ref([]);

// 涟漪效果的样式
const rippleStyle = {
  position: "absolute",
  border: "2px solid #fff",
  background: "red",
  opacity: 0.6,
  borderRadius: "50%",
  animation: "ripple-animation 1s forwards",
};

// 创建涟漪效果的函数
function createRipple(event) {
  const cesiumContainer = document.getElementById("cesiumContainer");
  const ripple = document.createElement("div");
  Object.assign(ripple.style, rippleStyle);

  // 设置涟漪的大小和位置
  const size = 60; // 涟漪的最大尺寸
  ripple.style.width = `${size}px`;
  ripple.style.height = `${size}px`;
  // ripple.style.left = `${event.clientX - cesiumContainer.offsetLeft - size / 2}px`;
  // ripple.style.top = `${event.clientY - cesiumContainer.offsetTop - size / 2}px`;
  ripple.style.left = `${event.offsetX - 30}px`;
  ripple.style.top = `${event.offsetY - 30}px`;

  // 将涟漪元素添加到容器中
  cesiumContainer.appendChild(ripple);

  // 一秒后移除涟漪元素
  setTimeout(() => {
    cesiumContainer.removeChild(ripple);
  }, 1000);
}

// CSS 动画，定义涟漪扩散效果
const styleElement = document.createElement("style");
styleElement.textContent = `
@keyframes ripple-animation {
  from {
    transform: scale(0);
    opacity: 0.6;
  }
  to {
    transform: scale(1);
    opacity: 0;
  }
}
`;

// 在这里定义 onContextMenu 方法，尽管我们不需要做任何事情
function onContextMenu(event) {
  // 阻止默认行为，右键菜单不会显示
  event.preventDefault();
}

onMounted(async () => {
  document.head.appendChild(styleElement);
  nextTick(async () => {
    await initScene();

    var firstThreeImageryViewModels =
      viewer.baseLayerPicker.viewModel.imageryProviderViewModels.slice(
        1,
        3
      );
    viewer.baseLayerPicker.viewModel.imageryProviderViewModels =
      firstThreeImageryViewModels;
    // drawOrbit();
    cursorEvent();
  });
});

// 监听绘制模式的变化
watch(
  () => props.type,
  (newMode) => {
    if (newMode === "Point") {
      // 绘制点
      // viewer.entities.removeById('area');
      clickDrawPoint();
    } else if (newMode === "Area") {
      // 绘制多边形
      // viewer.entities.removeById('point');
      clickDrawPolygon();
    }
  },
  { immediate: true }
);

// 监听 disabled 属性变化
watch(() => props.disabled, (newDisabled) => {
  canDraw.value = !newDisabled;
  if (newDisabled) {
    // 禁用绘制时，移除所有事件监听器
    if (handler.value) {
      handler.value.removeInputAction(Cesium.ScreenSpaceEventType.LEFT_CLICK);
      handler.value.removeInputAction(Cesium.ScreenSpaceEventType.MOUSE_MOVE);
      handler.value.removeInputAction(Cesium.ScreenSpaceEventType.RIGHT_CLICK);
    }
  } else {
    // 启用绘制时，重新设置事件监听器
    if (props.type === "Area") {
      clickDrawPolygon();
    } else {
      clickDrawPoint();
    }
  }
}, { immediate: true });

async function initScene() {
  // 初始化Cesium配置（包括Ion Token）
  try {
    initCesium();
    console.log('✅ Cesium Ion Token 初始化成功');
  } catch (error) {
    console.error('❌ Cesium Ion Token 初始化失败:', error);
    ElMessage.error('地图服务初始化失败，将使用离线地图');
  }

  // 使用cesiumConfig.js中的地图源配置
  let imageryProvider;
  try {
    // 优先尝试在线地图源，失败时自动切换到离线地图
    imageryProvider = await getImageryProviderWithFallback('gaode_vec');
    console.log('✅ 地图源初始化成功');
  } catch (error) {
    console.warn('⚠️ 在线地图源不可用，使用离线地图:', error);
    imageryProvider = getImageryProvider('offline_map');
  }

  viewer = new Cesium.Viewer("cesiumContainer", {
    homeButton: false,
    sceneModePicker: false,
    geocoder: false, // 搜索框
    baseLayerPicker: true,
    terrainProviderViewModels: [],
    sceneMode: Cesium.SceneMode.SCENE2D, //初始场景模式 为二维
    selectionIndicator: false, // 点击entity后出现的绿色框
    navigationHelpButton: false,
    animation: false,
    timeline: false,
    fullscreenButton: false,
    vrButton: false,
    scene3DOnly: false,
    infoBox: false,
    imageryProvider: imageryProvider, // 使用配置的地图源
  });

  // 配置Viewer的标准设置
  configureViewer(viewer, {
    enableLighting: true,
    shadows: true,
    showSun: false,
    showMoon: false,
    chineseLabels: false // 稍后单独添加，以便错误处理
  });

  // 添加星空背景
  viewer.scene.skyBox = new Cesium.SkyBox({
    sources: {
      positiveX: "/data/images/SkyBox/00h+00.jpg",
      negativeX: "/data/images/SkyBox/12h+00.jpg",
      positiveY: "/data/images/SkyBox/06h+00.jpg",
      negativeY: "/data/images/SkyBox/18h+00.jpg",
      positiveZ: "/data/images/SkyBox/06h+90.jpg",
      negativeZ: "/data/images/SkyBox/06h-90.jpg",
    },
  });

  // 初始视角
  viewer.camera.setView({
    destination: Cesium.Cartesian3.fromDegrees(116.39, 39.91, 6000000.0), // 修改为中国北京的坐标
    orientation: {
      pitch: Cesium.Math.toRadians(-90),
      roll: 0.0,
    },
  });

  if (props.type === "Area") {
    clickDrawPolygon();
  } else {
    clickDrawPoint();
  }
  getMousePoint();

  // 安全添加天地图中文标注层（带错误处理）
  addChineseLabelsSafely(viewer, {
    showError: true,
    fallbackToOffline: true
  });
  
  // 标记地图初始化完成
  isInitialized.value = true;
  console.log('地图初始化完成');
}

function handleClear() {
  var getByIdBox = viewer.entities.getById("area");
  viewer.entities.remove(getByIdBox);
  
  if (props.type === "Point") {
    // 清除所有点
    pointsList.value.forEach((_, index) => {
      viewer.entities.removeById(`point_${index}`);
    });
    // 清空点列表
    pointsList.value = [];
    
    // 重新启动点绘制
    clickDrawPoint();
  } else if (props.type === "Area") {
    clickDrawPolygon();
  }
  
  emit("clear");
}

function getMousePoint() {
  // 监听鼠标移动事件
  const handler = new Cesium.ScreenSpaceEventHandler(viewer.canvas);
  handler.setInputAction((movement) => {
    const cartesian = viewer.camera.pickEllipsoid(
      movement.endPosition,
      viewer.scene.globe.ellipsoid
    );
    if (cartesian) {
      const cartographic = Cesium.Cartographic.fromCartesian(cartesian);
      const longitude = Cesium.Math.toDegrees(cartographic.longitude);
      const latitude = Cesium.Math.toDegrees(cartographic.latitude);
      const height = cartographic.height; // 获取高度信息
      mousePosition.value = { longitude, latitude, height };
    } else {
      mousePosition.value = null;
    }
  }, Cesium.ScreenSpaceEventType.MOUSE_MOVE);
}

function clickDrawPoint() {
  if (canDraw.value) {
    // 清空之前的点数据
    pointsList.value = [];
    
    if (handler.value) {
      handler.value.removeInputAction(Cesium.ScreenSpaceEventType.LEFT_CLICK);
      handler.value.removeInputAction(Cesium.ScreenSpaceEventType.RIGHT_CLICK);
    }
    handler.value = new Cesium.ScreenSpaceEventHandler(viewer.canvas);
    
    // 监听左键点击事件 - 添加点
    handler.value.setInputAction((click) => {
      const cartesian = viewer.camera.pickEllipsoid(
        click.position,
        viewer.scene.globe.ellipsoid
      );
      if (cartesian) {
        // 转换笛卡尔坐标为经纬度坐标
        const cartographicPosition = Cesium.Cartographic.fromCartesian(cartesian);
        const longitude = Cesium.Math.toDegrees(cartographicPosition.longitude);
        const latitude = Cesium.Math.toDegrees(cartographicPosition.latitude);
        const height = cartographicPosition.height; // 获取高度信息
        
        // 添加到点列表
        pointsList.value.push([longitude, latitude, height]);
        
        // 在地图上添加点实体
        const pointIndex = pointsList.value.length - 1;
        const point = viewer.entities.add({
          id: `point_${pointIndex}`, // 每个点使用唯一ID
          position: cartesian,
          point: {
            pixelSize: 12,
            color: Cesium.Color.YELLOW,
            outlineColor: Cesium.Color.BLACK,
            outlineWidth: 2,
          },
          label: {
            text: `${pointIndex + 1}`,
            font: '12pt monospace',
            style: Cesium.LabelStyle.FILL_AND_OUTLINE,
            outlineWidth: 2,
            verticalOrigin: Cesium.VerticalOrigin.CENTER,
            horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
            pixelOffset: new Cesium.Cartesian2(0, 0),
            fillColor: Cesium.Color.WHITE,
            outlineColor: Cesium.Color.BLACK,
          },
        });

        // 发送所有点的坐标给父组件
        emit("selectPoints", [...pointsList.value]);
        
        console.log(`添加点 ${pointIndex + 1}: longitude: ${longitude}, latitude: ${latitude}, height: ${height}`);
        console.log('当前所有点:', pointsList.value);
      }
    }, Cesium.ScreenSpaceEventType.LEFT_CLICK);
    
    // 监听右键点击事件 - 结束添加点
    handler.value.setInputAction((click) => {
      console.log('结束添加点，共添加了', pointsList.value.length, '个点');
      // 移除事件监听器
      if (handler.value) {
        handler.value.removeInputAction(Cesium.ScreenSpaceEventType.LEFT_CLICK);
        handler.value.removeInputAction(Cesium.ScreenSpaceEventType.RIGHT_CLICK);
      }
    }, Cesium.ScreenSpaceEventType.RIGHT_CLICK);
    
  } else {
    if (handler.value) {
      handler.value.removeInputAction(Cesium.ScreenSpaceEventType.LEFT_CLICK);
      handler.value.removeInputAction(Cesium.ScreenSpaceEventType.RIGHT_CLICK);
    }
  }
}

const drawOrbit = async () => {
  const params = {
    starttime: new Date().getTime(),
    step: 60,
  };
  // const { data } = await orbitData(params);
  const jsonData = data;
  console.log("json data ", jsonData);
  const sampledPostion1 = new Cesium.SampledPositionProperty();
  const sampledPostion2 = new Cesium.SampledPositionProperty();
  const sampledPostion3 = new Cesium.SampledPositionProperty();
  jsonData.forEach((item) => {
    const position1 = new Cesium.Cartesian3(
      ...item.slice(1, 4).map((i) => i * 1000)
    );
    const position2 = new Cesium.Cartesian3(
      ...item.slice(4, 7).map((i) => i * 1000)
    );
    const position3 = new Cesium.Cartesian3(
      ...item.slice(7).map((i) => i * 1000)
    );
    const timeAvalible = Cesium.JulianDate.fromDate(new Date(item[0] * 1000));
    sampledPostion1.addSample(timeAvalible, position1);
    sampledPostion2.addSample(timeAvalible, position2);
    sampledPostion3.addSample(timeAvalible, position3);
  });

  const start = Cesium.JulianDate.fromDate(new Date(jsonData[0][0] * 1000));
  const end = Cesium.JulianDate.fromDate(
    new Date(jsonData[jsonData.length - 1][0] * 1000)
  );
  viewer.clock.startTime = start.clone();
  viewer.clock.currentTime = start.clone();
  // 设置始终停止时间
  viewer.clock.stopTime = end.clone();

  viewer.entities.add({
    id: "123",
    position: sampledPostion1,
    availability: new Cesium.TimeIntervalCollection([
      new Cesium.TimeInterval({
        start: start,
        stop: end,
      }),
    ]),
    point: {
      pixelSize: 4,
      color: Cesium.Color.YELLOW,
    },

    path: {
      show: true,
      material: Cesium.Color.YELLOW,
      width: 2,
    },
  });
};

function cursorEvent() {
  /* 获取元素 */
  var card = document.getElementById("cesiumContainer");
  /* 绑定点击事件 */
  card.addEventListener("click", function (e) {
    createRipple(e);
  });
}

function clickDrawPolygon() {
  console.log(canDraw.value);
  if (canDraw.value) {
    // 清空之前的多边形点数据
    polygonPoints.value = [];
    tempPolygonEntity.value = null;
    
    if (handler.value) {
      handler.value.removeInputAction(Cesium.ScreenSpaceEventType.LEFT_CLICK);
      handler.value.removeInputAction(Cesium.ScreenSpaceEventType.MOUSE_MOVE);
      handler.value.removeInputAction(Cesium.ScreenSpaceEventType.RIGHT_CLICK);
    }
    handler.value = new Cesium.ScreenSpaceEventHandler(viewer.canvas);

    // 鼠标左键点击选中点
    handler.value.setInputAction((event) => {
      const cartesian = viewer.camera.pickEllipsoid(event.position, viewer.scene.globe.ellipsoid);
      if (Cesium.defined(cartesian)) {
        polygonPoints.value.push(cartesian);
        if (tempPolygonEntity.value == null) {
          drawDynamicPolygon();
        }
        // createRippleEffect(viewer, cartesian);
      }
    }, Cesium.ScreenSpaceEventType.LEFT_CLICK);

    // 鼠标移动实时绘制多边形
    handler.value.setInputAction((event) => {
      const cartesian = viewer.camera.pickEllipsoid(event.endPosition, viewer.scene.globe.ellipsoid);
      if (Cesium.defined(cartesian)) {
        if (tempPolygonEntity.value) {
          if (polygonPoints.value.length > 1) {
            // 删除数组最后一个鼠标移进去的点
            polygonPoints.value.pop();
          }
          polygonPoints.value.push(cartesian);
        }
      }
    }, Cesium.ScreenSpaceEventType.MOUSE_MOVE);

    // 鼠标右键结束绘制
    handler.value.setInputAction((event) => {
      handler.value.removeInputAction(Cesium.ScreenSpaceEventType.MOUSE_MOVE);
      viewer.entities.remove(tempPolygonEntity.value);
      tempPolygonEntity.value = null;
      drawPolygon();
      polygonPoints.value = [];
      console.log("polygonPoints", polygonPoints.value);

      if (handler.value) {
        handler.value.removeInputAction(Cesium.ScreenSpaceEventType.LEFT_CLICK);
        handler.value.removeInputAction(Cesium.ScreenSpaceEventType.MOUSE_MOVE);
        handler.value.removeInputAction(
          Cesium.ScreenSpaceEventType.RIGHT_CLICK
        );
      }
    }, Cesium.ScreenSpaceEventType.RIGHT_CLICK);
  } else {
    handler.value.removeInputAction(Cesium.ScreenSpaceEventType.LEFT_CLICK);
    handler.value.removeInputAction(Cesium.ScreenSpaceEventType.MOUSE_MOVE);
    handler.value.removeInputAction(Cesium.ScreenSpaceEventType.RIGHT_CLICK);
  }
}

function drawDynamicPolygon() {
  tempPolygonEntity.value = viewer.entities.add({
    polygon: {
      hierarchy: new Cesium.CallbackProperty(() => {
        return new Cesium.PolygonHierarchy(polygonPoints.value);
      }, false),
      extrudedHeight: 0, // 高度
      height: 10, // 离地高度
      material: Cesium.Color.RED.withAlpha(0.2),
    },
  });
}

function drawPolygon() {
  polygonPoints.value.pop();
  console.log(polygonPoints.value, ".value");
  if (polygonPoints.value.length >= 3) {
    let polygonPointEntity = viewer.entities.add({
      id: "area",
      polygon: {
        hierarchy: polygonPoints.value,
        extrudedHeight: 0, // 高度
        height: 12, // 离地高度
        material: Cesium.Color.RED.withAlpha(0.15),
        outline: true,
        outlineColor: Cesium.Color.RED,
        outlineWidth: 20,
      },
    });

    let points = [];
    polygonPoints.value.forEach((el) => {
      let polyObj = {};
      let cartographic =
        viewer.scene.globe.ellipsoid.cartesianToCartographic(el);
      polyObj["lon"] = Cesium.Math.toDegrees(cartographic.longitude);
      polyObj["lat"] = Cesium.Math.toDegrees(cartographic.latitude);
      polyObj["height"] = cartographic.height; // 添加高度信息
      points.push([polyObj.lon, polyObj.lat, polyObj.height]);
    });
    console.log(points, "points");
    emit("selectPoints", points);
  } else {
    return;
  }
}

// 创建涟漪效果
function createRippleEffect(viewer, position) {
  const entity = viewer.entities.add({
    position: position,
    ellipse: {
      semiMinorAxis: new Cesium.CallbackProperty(
        () => rippleSemiMinorAxis,
        false
      ),
      semiMajorAxis: new Cesium.CallbackProperty(
        () => rippleSemiMajorAxis,
        false
      ),
      material: Cesium.Color.WHITE.withAlpha(rippleAlpha),
      height: 0,
      outline: false,
    },
  });

  let rippleSemiMinorAxis = 0;
  let rippleSemiMajorAxis = 0;
  let rippleAlpha = 0.5;
  const maxRadius = 1000; // 最大涟漪半径
  const growthSpeed = 1000 / 60; // 增长速度，假设每帧增长

  function updateRipple() {
    rippleSemiMinorAxis += growthSpeed;
    rippleSemiMajorAxis += growthSpeed;
    rippleAlpha -= 0.01; // 逐渐减少透明度

    if (rippleAlpha < 0) {
      rippleAlpha = 0;
    }

    if (rippleSemiMinorAxis > maxRadius) {
      rippleSemiMinorAxis = maxRadius;
      rippleSemiMajorAxis = maxRadius;
    }

    // 更新涟漪实体的透明度
    entity.ellipse.material = Cesium.Color.WHITE.withAlpha(rippleAlpha);
  }

  // 使用 requestAnimationFrame 来更新涟漪效果
  const animationFrameId = requestAnimationFrame(function frame() {
    updateRipple();
    if (rippleAlpha <= 0) {
      cancelAnimationFrame(animationFrameId);
      viewer.entities.remove(entity); // 移除涟漪实体
    } else {
      requestAnimationFrame(frame);
    }
  });

  // 设置定时器，一秒后停止动画并移除涟漪实体
  setTimeout(() => {
    cancelAnimationFrame(animationFrameId);
    viewer.entities.remove(entity);
  }, 1000);
}

function sceneControl(e, type) {
  if (type == "control") {
    viewer.clock.shouldAnimate = e;
  } else {
    viewer.clockViewModel.multiplier = e;
  }
}

function updateCzml(e) {
  console.log(e, "czml scene");
  var dataSourceTemp = Cesium.CzmlDataSource.load(e);
  viewer.dataSources.add(dataSourceTemp);
}

function chartLet(data) {
  viewer.imageryLayers.addImageryProvider(
    new Cesium.SingleTileImageryProvider({
      url: "imgApi/blackearth/" + data.minioUrlPng,
      rectangle: Cesium.Rectangle.fromDegrees(
        data.leftDownLon,
        data.leftDownLat,
        data.rightUpLon,
        data.rightUpLat
      ),
    })
  );
  viewer.camera.setView({
    destination: Cesium.Cartesian3.fromDegrees(
      Number(data.leftDownLon),
      Number(data.leftDownLat),
      1000000.0
    ), // Cartesian3.fromDegrees从以度为单位的经度和纬度值返回 Cartesian3 位置。初始位置定位在中国上空
    orientation: {
      // 指向
      // heading: Cesium.Math.toRadians(-10),
      // 视角
      pitch: Cesium.Math.toRadians(-90),
      roll: 0.0,
    },
  });
}

function addPoint(pointList, id) {
  // 清除现有的点
  pointsList.value.forEach((_, index) => {
    viewer.entities.removeById(`point_${index}`);
  });
  viewer.entities.removeById("point"); // 兼容旧的单点ID
  
  if (!pointList || pointList.length === 0) {
    pointsList.value = [];
    return;
  }
  
  console.log('添加点到地图:', pointList, 'id:', id);
  
  // 更新点列表
  pointsList.value = pointList.map(point => [
    parseFloat(point.longitude), 
    parseFloat(point.latitude), 
    parseFloat(point.height || 0)
  ]);
  
  // 为每个点创建实体
  pointList.forEach((point, index) => {
    if (!point.longitude || !point.latitude) return;
    
    const cartesian = Cesium.Cartesian3.fromDegrees(
      parseFloat(point.longitude), 
      parseFloat(point.latitude), 
      parseFloat(point.height || 0)
    );
    
    const entityId = `point_${index}`;
    
    viewer.entities.add({
      id: entityId,
      position: cartesian,
      point: {
        pixelSize: 12,
        color: Cesium.Color.YELLOW,
        outlineColor: Cesium.Color.BLACK,
        outlineWidth: 2,
      },
      label: pointList.length > 1 ? {
        text: `${index + 1}`,
        font: '12pt monospace',
        style: Cesium.LabelStyle.FILL_AND_OUTLINE,
        outlineWidth: 2,
        verticalOrigin: Cesium.VerticalOrigin.CENTER,
        horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
        pixelOffset: new Cesium.Cartesian2(0, 0),
        fillColor: Cesium.Color.WHITE,
        outlineColor: Cesium.Color.BLACK,
      } : undefined,
    });
  });
}

function addPolygon(pointList, id) {
  if (id && viewer.entities.getById(id)) {
    viewer.entities.removeById(id);
  }
  if (!pointList || pointList.length === 0) return;
  
  const formatList = pointList.map((item) => {
    return Cesium.Cartesian3.fromDegrees(
      item.longitude, 
      item.latitude, 
      item.height || 0
    );
  });
  
  console.log('添加多边形到地图:', pointList, 'formatList:', formatList, 'id:', id);
  
  const polygonEntity = viewer.entities.add({
    id: id || "area",
    polygon: {
      hierarchy: new Cesium.PolygonHierarchy(formatList),
      extrudedHeight: 0,
      height: 12,
      material: Cesium.Color.YELLOW.withAlpha(0.15),
      outline: true,
      outlineColor: Cesium.Color.YELLOW,
      outlineWidth: 20,
    },
  });
  
  flyToCenter(pointList);
}

function repaintPolygon(id, isRed) {
  console.log("id ", id);
  if (!id) return;
  const entity = viewer.entities.getById(id);
  if (!entity) return;
  console.log("entity ", entity);
  entity.polygon.material = isRed
    ? Cesium.Color.RED.withAlpha(0.15)
    : Cesium.Color.YELLOW.withAlpha(0.15);
  entity.polygon.outlineColor = isRed ? Cesium.Color.RED : Cesium.Color.YELLOW;
}

function removePolygon(id) {
  // 移除主实体
  viewer.entities.removeById(id);
  
  // 移除临时多边形实体
  if (tempPolygonEntity.value) {
    viewer.entities.remove(tempPolygonEntity.value);
    tempPolygonEntity.value = null;
  }
  
  // 移除所有点实体
  pointsList.value.forEach((_, index) => {
    viewer.entities.removeById(`point_${index}`);
  });
  
  // 移除可能的旧格式点实体
  viewer.entities.removeById("point");
  let index = 0;
  while (viewer.entities.getById(`${id}_${index}`)) {
    viewer.entities.removeById(`${id}_${index}`);
    index++;
  }
  
  // 移除突出显示的点
  viewer.entities.removeById('highlight-point');
  
  // 清空点列表和多边形点列表
  if (id === "point" || id === "area") {
    pointsList.value = [];
    polygonPoints.value = [];
  }
}

function flyToCenter(pointList) {
  const formatList = pointList.map((item) => {
    return Cesium.Cartesian3.fromDegrees(item.longitude, item.latitude, item.height || 0);
  });
  const boundingSphere = Cesium.BoundingSphere.fromPoints(formatList);
  console.log("boundingSphere ", boundingSphere);
  viewer.camera.flyToBoundingSphere(boundingSphere, {
    offset: {
      range: boundingSphere.radius * 10,
    },
  });
}

// 飞行到单个点
function flyToSinglePoint(point) {
  console.log('飞行到单个点:', point);
  const destination = Cesium.Cartesian3.fromDegrees(
    parseFloat(point.longitude),
    parseFloat(point.latitude),
    parseFloat(point.height || 0) + 50000 // 在点上方50km的高度
  );
  
  viewer.camera.flyTo({
    destination: destination,
    orientation: {
      pitch: Cesium.Math.toRadians(-90), // 俯视角度
      roll: 0.0,
    },
    duration: 2.0 // 飞行时间2秒
  });
}

// 飞行到所有点并突出显示指定点
function flyToAllPoints(pointList, highlightIndex = -1) {
  console.log('飞行到所有点:', pointList, '突出显示索引:', highlightIndex);
  
  if (!pointList || pointList.length === 0) return;
  
  // 如果只有一个点，直接飞到该点
  if (pointList.length === 1) {
    flyToSinglePoint(pointList[0]);
    return;
  }
  
  // 计算所有点的边界
  const cartesianPoints = pointList.map((point) => {
    return Cesium.Cartesian3.fromDegrees(
      parseFloat(point.longitude),
      parseFloat(point.latitude),
      parseFloat(point.height || 0)
    );
  });
  
  const boundingSphere = Cesium.BoundingSphere.fromPoints(cartesianPoints);
  
  // 计算合适的相机高度
  const radius = boundingSphere.radius;
  const cameraHeight = Math.max(radius * 3, 10000); // 至少10km高度
  
  viewer.camera.flyToBoundingSphere(boundingSphere, {
    offset: new Cesium.HeadingPitchRange(
      0.0, // heading
      Cesium.Math.toRadians(-90), // pitch (俯视)
      cameraHeight // range
    ),
    duration: 2.0
  });
  
  // 如果指定了要突出显示的点，在飞行完成后添加标记
  if (highlightIndex >= 0 && highlightIndex < pointList.length) {
    setTimeout(() => {
      highlightPoint(pointList[highlightIndex], highlightIndex);
    }, 2500); // 等待飞行完成
  }
}

// 突出显示指定点
function highlightPoint(point, index) {
  console.log('突出显示点:', point, '索引:', index);
  
  // 移除之前的突出显示
  viewer.entities.removeById('highlight-point');
  
  // 添加突出显示的点
  const cartesian = Cesium.Cartesian3.fromDegrees(
    parseFloat(point.longitude),
    parseFloat(point.latitude),
    parseFloat(point.height || 0)
  );
  
  viewer.entities.add({
    id: 'highlight-point',
    position: cartesian,
    point: {
      pixelSize: 20,
      color: Cesium.Color.RED,
      outlineColor: Cesium.Color.WHITE,
      outlineWidth: 3,
      scaleByDistance: new Cesium.NearFarScalar(1.5e2, 2.0, 1.5e7, 0.5),
    },
    label: {
      text: `第${index + 1}个坐标点`,
      font: '14pt monospace',
      style: Cesium.LabelStyle.FILL_AND_OUTLINE,
      outlineWidth: 2,
      verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
      pixelOffset: new Cesium.Cartesian2(0, -9),
      fillColor: Cesium.Color.YELLOW,
      outlineColor: Cesium.Color.BLACK,
    }
  });
  
  // 3秒后移除突出显示
  setTimeout(() => {
    viewer.entities.removeById('highlight-point');
  }, 3000);
}

function changeDrawState(e) {
  canDraw.value = e;
  nextTick(() => {
    if (props.type === "Area") {
      clickDrawPolygon();
    } else {
      viewer.entities.removeById("point");
      clickDrawPoint();
    }
  });
}

defineExpose({
  sceneControl,
  updateCzml,
  chartLet,
  addPolygon,
  addPoint,
  removePolygon,
  repaintPolygon,
  flyToCenter,
  changeDrawState,
  handleClear,
  flyToSinglePoint,
  flyToAllPoints,
  highlightPoint,
  isInitialized,
});
</script>

<style lang="scss" scoped>
.cesium-content {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

#cesiumContainer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.clear-button {
  position: absolute;
  width: fit-content;
  height: 25px;
  bottom: 5px;
  right: 5px;
  padding: 0 4px;
  text-align: center;
  line-height: 25px;
  background: rgba(0, 183, 255, 0.2);
  border: solid 1px rgb(0, 217, 255);
  border-radius: 4px;
  color: aliceblue;
  cursor: pointer;
  z-index: 999;

  &:hover {
    background: rgba(0, 183, 255, 0.4);
  }
}

.mouse-position {
  position: absolute;
  width: fit-content;
  height: 30px;
  bottom: 0px;
  left: 5px;
  z-index: 999;
  line-height: 30px;
  color: #000000;
}

/* 确保Cesium Viewer正确显示 */
:deep(.cesium-viewer) {
  width: 100% !important;
  height: 100% !important;
  position: absolute !important;
  top: 0 !important;
  left: 0 !important;
}

:deep(.cesium-viewer-cesiumWidgetContainer) {
  width: 100% !important;
  height: 100% !important;
}

:deep(.cesium-widget) {
  width: 100% !important;
  height: 100% !important;
}

:deep(.cesium-widget canvas) {
  width: 100% !important;
  height: 100% !important;
}
</style>