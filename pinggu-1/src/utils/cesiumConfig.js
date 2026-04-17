import * as Cesium from 'cesium'

// 天地图Token - 统一管理
export const TDT_TOKEN = '17446a709cb87853d6acbb6d85d330b2'

// Cesium Ion Token - 统一管理
// 注意：这个token可能已过期，请访问 https://ion.cesium.com/tokens 获取你自己的token
export const CESIUM_ION_TOKEN = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJjOTcyYmY5Yi0yM2YxLTQ4OTItOGM4MS1lZmQxNWE2NGE3MDQiLCJpZCI6MjI1ODc0LCJpYXQiOjE3NTA4MTc1MDl9.wdacvKxlEStqH2NO3ykhTf5vEzb4ck6QYfGswaqou98' // 请替换为你的实际token

// 地图源配置 - 优先使用国内地图源
export const IMAGERY_PROVIDERS = {
  // 离线地图源（备用）
  offline_map: {
    name: '离线地图',
    type: 'template',
    provider: () => new Cesium.UrlTemplateImageryProvider({
      url: "/map/{z}/{x}/{y}.jpg",
      credit: new Cesium.Credit('离线地图'),
      maximumLevel: 18
    })
  },
  // 天地图影像（默认）
  tianditu_img: {
    name: '天地图影像',
    type: 'wmts',
    provider: () => new Cesium.WebMapTileServiceImageryProvider({
      url: `https://t{s}.tianditu.gov.cn/img_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=img&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={TileMatrix}&TILEROW={TileRow}&TILECOL={TileCol}&tk=${TDT_TOKEN}`,
      layer: 'img',
      style: 'default',
      format: 'tiles',
      tileMatrixSetID: 'w',
      subdomains: ['0', '1', '2', '3', '4', '5', '6', '7'],
      credit: new Cesium.Credit('天地图'),
      maximumLevel: 18
    })
  },
  // 天地图矢量
  tianditu_vec: {
    name: '天地图矢量',
    type: 'wmts',
    provider: () => new Cesium.WebMapTileServiceImageryProvider({
      url: `https://t{s}.tianditu.gov.cn/vec_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=vec&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={TileMatrix}&TILEROW={TileRow}&TILECOL={TileCol}&tk=${TDT_TOKEN}`,
      layer: 'vec',
      style: 'default',
      format: 'tiles',
      tileMatrixSetID: 'w',
      subdomains: ['0', '1', '2', '3', '4', '5', '6', '7'],
      credit: new Cesium.Credit('天地图'),
      maximumLevel: 18
    })
  },
  // 高德影像
  gaode_img: {
    name: '高德影像',
    type: 'template',
    provider: () => new Cesium.UrlTemplateImageryProvider({
      url: 'https://webst{s}.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}',
      subdomains: ['01', '02', '03', '04'],
      credit: new Cesium.Credit('高德地图'),
      maximumLevel: 18
    })
  },
  // 高德矢量
  gaode_vec: {
    name: '高德矢量',
    type: 'template',
    provider: () => new Cesium.UrlTemplateImageryProvider({
      url: 'https://webrd{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
      subdomains: ['01', '02', '03', '04'],
      credit: new Cesium.Credit('高德地图'),
      maximumLevel: 18
    })
  },
  // 腾讯影像
  tencent_img: {
    name: '腾讯影像',
    type: 'template',
    provider: () => new Cesium.UrlTemplateImageryProvider({
      url: 'https://p{s}.map.gtimg.com/sateTiles/{z}/{sx}/{sy}/{x}_{y}.jpg',
      subdomains: ['0', '1', '2', '3'],
      credit: new Cesium.Credit('腾讯地图'),
      maximumLevel: 18,
      customTags: {
        sx: function (imageryProvider, x, y, level) {
          return Math.floor(x / 16);
        },
        sy: function (imageryProvider, x, y, level) {
          return Math.floor(y / 16);
        }
      }
    })
  },
  // 百度影像（注意：百度地图使用BD09坐标系，可能需要坐标转换）
  baidu_img: {
    name: '百度影像',
    type: 'template',
    provider: () => new Cesium.UrlTemplateImageryProvider({
      url: 'https://maponline{s}.bdimg.com/tile/?qt=vtile&x={x}&y={y}&z={z}&styles=sl&v=020',
      subdomains: ['0', '1', '2', '3'],
      credit: new Cesium.Credit('百度地图'),
      maximumLevel: 18
    })
  },
  // ArcGIS World Imagery（备用国外地图源）
  arcgis_world: {
    name: 'ArcGIS World Imagery',
    type: 'arcgis',
    provider: () => new Cesium.ArcGisMapServerImageryProvider({
      url: 'https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer'
    })
  }
}

// 地形数据源配置
// export const TERRAIN_PROVIDERS = {
//   // 国内地形数据（默认）
//   china_terrain: {
//     name: '国内地形',
//     provider: () => new Cesium.CesiumTerrainProvider({
//       url: 'http://www.freexgis.com/web-data/terrain'
//     })
//   },
//   // ArcGIS地形
//   arcgis_terrain: {
//     name: 'ArcGIS地形',
//     provider: () => new Cesium.ArcGISTiledElevationTerrainProvider({
//       url: 'https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer'
//     })
//   },
//   // Cesium World Terrain（需要token）
//   cesium_world: {
//     name: 'Cesium World Terrain',
//     provider: () => Cesium.createWorldTerrain()
//   }
// }

// 默认配置
export const DEFAULT_CONFIG = {
  // imagery: 'tianditu_img',  // 默认使用天地图影像
  imagery: 'offline_map',  // 默认使用离线地图，确保在线地图无法访问时也能正常显示
  terrain: 'china_terrain'  // 默认使用国内地形
}

// 星空背景图片路径 - 包含太阳和月球位置的时间相关背景【影响了月球和太阳位置的识别，注释掉了25.07.28】
// export const SKYBOX_SOURCES = {
//   positiveX: "/data/images/SkyBox/00h+00.jpg",
//   negativeX: "/data/images/SkyBox/12h+00.jpg",
//   positiveY: "/data/images/SkyBox/06h+00.jpg",
//   negativeY: "/data/images/SkyBox/18h+00.jpg",
//   positiveZ: "/data/images/SkyBox/06h+90.jpg",
//   negativeZ: "/data/images/SkyBox/06h-90.jpg"
// }

// 初始化Cesium基础配置
export function initCesium() {
  // 设置Cesium Ion Token（如果token有效的话）
  if (CESIUM_ION_TOKEN && CESIUM_ION_TOKEN !== 'YOUR_CESIUM_ION_TOKEN_HERE') {
    try {
      Cesium.Ion.defaultAccessToken = CESIUM_ION_TOKEN
      console.log('✅ Cesium Ion Token 设置成功')
    } catch (error) {
      console.warn('⚠️ Cesium Ion Token 设置失败，将使用离线地图源:', error)
    }
  } else {
    console.warn('⚠️ 未配置有效的 Cesium Ion Token，将使用离线地图源')
  }
}

// 检测地图源是否可用
export async function checkImageryProviderAvailability(providerKey) {
  const config = IMAGERY_PROVIDERS[providerKey]
  if (!config) return false
  
  try {
    const provider = config.provider()
    
    // 对于在线地图源，尝试加载一个测试瓦片
    if (providerKey !== 'offline_map') {
      // 创建一个测试的瓦片请求
      const testUrl = provider.url || provider._url
      if (testUrl) {
        const response = await fetch(testUrl.replace('{z}', '1').replace('{x}', '0').replace('{y}', '0'))
        return response.ok
      }
    }
    
    return true
  } catch (error) {
    console.warn(`地图源 ${providerKey} 检测失败:`, error)
    return false
  }
}

// 获取地图源提供者（带可用性检测）
export async function getImageryProviderWithFallback(preferredKey = 'tianditu_img') {
  // 首先尝试首选地图源
  if (preferredKey !== 'offline_map') {
    const isAvailable = await checkImageryProviderAvailability(preferredKey)
    if (isAvailable) {
      console.log(`✅ 使用在线地图源: ${preferredKey}`)
      return IMAGERY_PROVIDERS[preferredKey].provider()
    } else {
      console.warn(`⚠️ 在线地图源 ${preferredKey} 不可用，切换到离线地图`)
    }
  }
  
  // 如果首选地图源不可用或就是离线地图，使用离线地图
  console.log('✅ 使用离线地图源')
  return IMAGERY_PROVIDERS['offline_map'].provider()
}

// 获取地图源提供者（同步版本，保持向后兼容）
export function getImageryProvider(key = DEFAULT_CONFIG.imagery) {
  const config = IMAGERY_PROVIDERS[key]
  return config ? config.provider() : IMAGERY_PROVIDERS[DEFAULT_CONFIG.imagery].provider()
}

// 获取地形提供者
// export function getTerrainProvider(key = DEFAULT_CONFIG.terrain) {
//   const config = TERRAIN_PROVIDERS[key]
//   return config ? config.provider() : TERRAIN_PROVIDERS[DEFAULT_CONFIG.terrain].provider()
// }

// 获取天地图中文标注层
export function getChineseLabelsProvider() {
  return new Cesium.WebMapTileServiceImageryProvider({
    url: `https://t{s}.tianditu.gov.cn/cia_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cia&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={TileMatrix}&TILEROW={TileRow}&TILECOL={TileCol}&tk=${TDT_TOKEN}`,
    layer: "cia",
    style: "default",
    format: "tiles",
    tileMatrixSetID: "w",
    subdomains: ['0', '1', '2', '3', '4', '5', '6', '7'],
    credit: new Cesium.Credit("天地图全球中文注记服务"),
    maximumLevel: 18
  })
}

// 安全添加中文标注层（带错误处理）
export function addChineseLabelsSafely(viewer, options = {}) {
  if (!viewer) {
    console.warn('⚠️ Viewer 不存在，无法添加中文标注层');
    return false;
  }

  const defaultOptions = {
    showError: true, // 是否显示错误信息
    fallbackToOffline: true, // 是否在失败时使用离线标注
    maxRetries: 3, // 最大重试次数
    retryDelay: 1000 // 重试延迟（毫秒）
  };

  const config = { ...defaultOptions, ...options };

  try {
    // 检查网络连接
    const checkNetwork = async () => {
      try {
        // 使用更简单的网络检测方法
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 3000);
        
        const response = await fetch('https://t0.tianditu.gov.cn/cia_w/wmts?SERVICE=WMTS&REQUEST=GetCapabilities&VERSION=1.0.0&LAYER=cia&tk=' + TDT_TOKEN, {
          method: 'HEAD',
          signal: controller.signal
        });
        
        clearTimeout(timeoutId);
        return response.ok;
      } catch (error) {
        console.warn('网络检测失败:', error.message);
        return false;
      }
    };

    // 异步检查并添加标注层
    checkNetwork().then(isOnline => {
      if (isOnline) {
        try {
          const ciaProvider = getChineseLabelsProvider();
          viewer.imageryLayers.addImageryProvider(ciaProvider);
          console.log('✅ 天地图中文标注层添加成功');
        } catch (error) {
          console.warn('⚠️ 天地图中文标注层添加失败:', error);
          if (config.showError) {
            console.warn('⚠️ 标注服务不可用，将使用无标注的地图');
          }
        }
      } else {
        console.warn('⚠️ 网络连接不可用，跳过中文标注层');
        if (config.showError) {
          console.warn('⚠️ 离线模式下无法加载中文标注，地图将显示无标注版本');
        }
      }
    }).catch(error => {
      console.warn('⚠️ 网络检测失败，跳过中文标注层:', error);
    });

    return true;
  } catch (error) {
    console.error('❌ 添加中文标注层时发生错误:', error);
    return false;
  }
}

// 创建标准的Viewer配置
export function createViewerConfig(options = {}) {
  const defaultConfig = {
    animation: true,
    clock: true,
    fullscreenButton: true,
    vrButton: false,
    scene3DOnly: false,
    shouldAnimate: true,
    sceneMode: Cesium.SceneMode.SCENE3D,
    selectionIndicator: false,
    baseLayerPicker: false,
    timeline: true,
    sceneModePicker: true,
    homeButton: true,
    geocoder: false,
    infoBox: false,
    navigationHelpButton: false,
    // 使用默认地图源（离线地图作为默认）
    imageryProvider: getImageryProvider(options.imagery || 'offline_map'),
    // 使用默认地形数据
    // terrainProvider: getTerrainProvider(options.terrain)
  }

  return {
    ...defaultConfig,
    ...options
  }
}

// 创建带在线地图检测的Viewer配置（异步版本）
export async function createViewerConfigWithOnlineCheck(options = {}) {
  const defaultConfig = {
    animation: true,
    clock: true,
    fullscreenButton: true,
    vrButton: false,
    scene3DOnly: false,
    shouldAnimate: true,
    sceneMode: Cesium.SceneMode.SCENE3D,
    selectionIndicator: false,
    baseLayerPicker: false,
    timeline: true,
    sceneModePicker: true,
    homeButton: true,
    geocoder: false,
    infoBox: false,
    navigationHelpButton: false,
    // 使用带回退的地图源
    imageryProvider: await getImageryProviderWithFallback(options.imagery || 'tianditu_img'),
    // 使用默认地形数据
    // terrainProvider: getTerrainProvider(options.terrain)
  }

  return {
    ...defaultConfig,
    ...options
  }
}

// 配置Viewer的标准设置
export function configureViewer(viewer, options = {}) {
  // 去掉水印
  viewer.cesiumWidget.creditContainer.style.display = "none"

  // 添加太阳光照阴影
  viewer.scene.globe.enableLighting = options.enableLighting !== false
  viewer.shadows = options.shadows !== false
  viewer.terrainShadows = Cesium.ShadowMode.RECEIVE_ONLY

  // 添加星空背景
  // if (options.skyBox !== false) {
  //   const skyboxSources = options.skyBoxSources || SKYBOX_SOURCES
  //   viewer.scene.skyBox = new Cesium.SkyBox({
  //     sources: skyboxSources
  //   })
  // }
  
  // 启用太阳显示
  if (viewer.scene.sun) {
    viewer.scene.sun.show = options.showSun !== false;
    
    // 配置太阳显示选项
    if (options.showSun !== false) {
      // 设置太阳大小（可选）
      if (options.sunSize) {
        viewer.scene.sun.size = options.sunSize;
      }
      
      // 设置太阳亮度（可选）
      if (options.sunBrightness) {
        viewer.scene.sun.brightness = options.sunBrightness;
      }
      
      // 启用太阳光照效果
      viewer.scene.globe.enableLighting = options.enableLighting !== false;
      
      console.log('✅ 太阳显示已启用');
    }
  }

  // 启用月球显示（Cesium内置功能）
  if (viewer.scene.moon) {
    viewer.scene.moon.show = options.showMoon !== false;
    
    // 配置月球显示选项
    if (options.showMoon !== false) {
      // 设置月球大小（如果支持）
      if (options.moonSize) {
        try {
          viewer.scene.moon.size = options.moonSize * 1000; // 转换为米
        } catch (error) {
          console.log('⚠️ 月球大小设置失败，使用默认大小');
        }
      }
      
      // 设置月球亮度（如果支持）
      if (options.moonBrightness) {
        try {
          viewer.scene.moon.brightness = options.moonBrightness;
        } catch (error) {
          console.log('⚠️ 月球亮度设置失败，使用默认亮度');
        }
      }
      
      // 可选：自定义月球纹理
      if (options.moonTextureUrl) {
        try {
          viewer.scene.moon.textureUrl = options.moonTextureUrl;
        } catch (error) {
          console.log('⚠️ 月球纹理设置失败，使用默认纹理');
        }
      }
      
      console.log('✅ 月球显示已启用');
    }
  }

  // // 启用天空大气层
  // if (viewer.scene.skyAtmosphere) {
  //   viewer.scene.skyAtmosphere.show = true;
  // }

  // 设置初始视角（中国北京）
  const initialView = options.initialView || {
    longitude: 116.39,
    latitude: 39.91,
    height: 26000000.0
  }

  viewer.camera.setView({
    destination: Cesium.Cartesian3.fromDegrees(
      initialView.longitude,
      initialView.latitude,
      initialView.height
    ),
    orientation: {
      pitch: Cesium.Math.toRadians(-90),
      roll: 0.0
    }
  })

  // 去除二三维动画效果
  // viewer.sceneModePicker.viewModel.duration = 0.0

  // 添加天地图中文标注层
  if (options.chineseLabels !== false) {
    viewer.imageryLayers.addImageryProvider(getChineseLabelsProvider())
  }

  return viewer
}

// 一键创建配置好的Viewer
export function createConfiguredViewer(containerId, options = {}) {
  // 初始化Cesium
  initCesium()

  // 创建Viewer配置
  const config = createViewerConfig(options)

  // 创建Viewer
  const viewer = new Cesium.Viewer(containerId, config)

  // 配置Viewer - 确保传递所有选项，包括太阳和月球配置
  configureViewer(viewer, options)

  return viewer
}

// 一键创建配置好的Viewer（带在线地图检测）
export async function createConfiguredViewerWithOnlineCheck(containerId, options = {}) {
  // 初始化Cesium
  initCesium()

  // 创建带在线检测的Viewer配置
  const config = await createViewerConfigWithOnlineCheck(options)

  // 创建Viewer
  const viewer = new Cesium.Viewer(containerId, config)

  // 配置Viewer
  configureViewer(viewer, options)

  return viewer
}

// 获取所有可用的地图源列表
export function getImageryProvidersList() {
  return Object.keys(IMAGERY_PROVIDERS).map(key => ({
    key,
    name: IMAGERY_PROVIDERS[key].name,
    type: IMAGERY_PROVIDERS[key].type
  }))
}

// 切换地图源
export function switchImageryProvider(viewer, providerKey) {
  if (viewer && IMAGERY_PROVIDERS[providerKey]) {
    const newProvider = IMAGERY_PROVIDERS[providerKey].provider()

    // 移除现有的底图图层，保留标注层
    const imageryLayers = viewer.imageryLayers
    const baseLayer = imageryLayers.get(0)
    if (baseLayer) {
      imageryLayers.remove(baseLayer)
    }

    // 添加新的底图图层
    imageryLayers.addImageryProvider(newProvider, 0)
    
    console.log(`✅ 已切换到地图源: ${IMAGERY_PROVIDERS[providerKey].name}`)
  } else {
    console.warn(`⚠️ 地图源 ${providerKey} 不存在，切换到离线地图`)
    switchImageryProvider(viewer, 'offline_map')
  }
}

// 切换地图源（带可用性检测）
export async function switchImageryProviderWithFallback(viewer, preferredKey) {
  if (!viewer) {
    console.error('❌ Viewer 不存在')
    return false
  }
  
  try {
    // 检查首选地图源是否可用
    const isAvailable = await checkImageryProviderAvailability(preferredKey)
    
    if (isAvailable) {
      switchImageryProvider(viewer, preferredKey)
      return true
    } else {
      console.warn(`⚠️ 地图源 ${preferredKey} 不可用，切换到离线地图`)
      switchImageryProvider(viewer, 'offline_map')
      return false
    }
  } catch (error) {
    console.error('切换地图源失败:', error)
    // 出错时切换到离线地图
    switchImageryProvider(viewer, 'offline_map')
    return false
  }
}

// 创建自定义月球实体
export function createCustomMoon(viewer, options = {}) {
  const defaultOptions = {
    show: true,
    scale: 1737.1, // 月球半径（公里）
    textureUrl: '/data/images/moon_texture.jpg', // 月球纹理图片
    enableLighting: true,
    onlySunLighting: true
  }
  
  const config = { ...defaultOptions, ...options }
  
  // 创建月球椭球体
  const moonEllipsoid = new Cesium.Ellipsoid(
    config.scale * 1000, // 转换为米
    config.scale * 1000,
    config.scale * 1000
  )
  
  // 创建月球实体
  const moonEntity = viewer.entities.add({
    name: 'Moon',
    position: new Cesium.SampledPositionProperty(),
    ellipsoid: {
      radii: moonEllipsoid.radii,
      material: config.textureUrl,
      outline: true,
      outlineColor: Cesium.Color.WHITE.withAlpha(0.3),
      outlineWidth: 1,
      heightReference: Cesium.HeightReference.NONE
    }
  })
  
  // 更新月球位置（基于时间）
  function updateMoonPosition(time) {
    if (!moonEntity || !config.show) return
    
    try {
      // 使用Cesium内置的月球位置计算
      const moonPosition = Cesium.Simon1994PlanetaryPositions.computeMoonPositionInEarthInertialFrame(time)
      moonEntity.position.setValue(time, moonPosition)
    } catch (error) {
      console.warn('月球位置计算失败:', error)
    }
  }
  
  // 监听时钟变化
  viewer.clock.onTick.addEventListener(updateMoonPosition)
  
  return {
    entity: moonEntity,
    updatePosition: updateMoonPosition,
    remove: () => {
      viewer.clock.onTick.removeEventListener(updateMoonPosition)
      viewer.entities.remove(moonEntity)
    }
  }
}

// 获取月球当前位置
export function getMoonPosition(viewer, time = Cesium.JulianDate.now()) {
  try {
    return Cesium.Simon1994PlanetaryPositions.computeMoonPositionInEarthInertialFrame(time)
  } catch (error) {
    console.error('获取月球位置失败:', error)
    return null
  }
}

// 计算月球到地球的距离
export function getMoonDistance(viewer, time = Cesium.JulianDate.now()) {
  const moonPosition = getMoonPosition(viewer, time)
  if (!moonPosition) return null
  
  // 地球中心位置
  const earthCenter = Cesium.Cartesian3.ZERO
  
  // 计算距离
  const distance = Cesium.Cartesian3.distance(moonPosition, earthCenter)
  
  return {
    distance: distance, // 米
    distanceKm: distance / 1000, // 公里
    position: moonPosition
  }
}

// 太阳控制函数
export function enableSun(viewer, options = {}) {
  if (!viewer || !viewer.scene.sun) {
    console.warn('⚠️ 无法启用太阳显示：viewer 或 sun 对象不存在');
    return false;
  }
  
  viewer.scene.sun.show = true;
  
  // 设置太阳大小
  if (options.size) {
    viewer.scene.sun.size = options.size;
  }
  
  // 设置太阳亮度
  if (options.brightness) {
    viewer.scene.sun.brightness = options.brightness;
  }
  
  // 启用地球光照
  if (options.enableLighting !== false) {
    viewer.scene.globe.enableLighting = true;
  }
  
  console.log('✅ 太阳显示已启用');
  return true;
}

export function disableSun(viewer) {
  if (!viewer || !viewer.scene.sun) {
    console.warn('⚠️ 无法禁用太阳显示：viewer 或 sun 对象不存在');
    return false;
  }
  
  viewer.scene.sun.show = false;
  console.log('✅ 太阳显示已禁用');
  return true;
}

export function toggleSun(viewer) {
  if (!viewer || !viewer.scene.sun) {
    console.warn('⚠️ 无法切换太阳显示：viewer 或 sun 对象不存在');
    return false;
  }
  
  const isVisible = viewer.scene.sun.show;
  viewer.scene.sun.show = !isVisible;
  
  console.log(`✅ 太阳显示已${isVisible ? '禁用' : '启用'}`);
  return !isVisible;
}

// 获取太阳当前位置
export function getSunPosition(viewer, time = Cesium.JulianDate.now()) {
  if (!viewer || !viewer.scene.sun) {
    console.warn('⚠️ 无法获取太阳位置：viewer 或 sun 对象不存在');
    return null;
  }
  
  try {
    // 使用 Cesium 内置的太阳位置计算
    return Cesium.SunLight.computeSunPosition(time, viewer.scene.globe.ellipsoid);
  } catch (error) {
    console.error('获取太阳位置失败:', error);
    return null;
  }
}

// 设置太阳时间（影响太阳位置）
export function setSunTime(viewer, date) {
  if (!viewer || !viewer.clock) {
    console.warn('⚠️ 无法设置太阳时间：viewer 或 clock 对象不存在');
    return false;
  }
  
  try {
    const julianDate = Cesium.JulianDate.fromDate(date);
    viewer.clock.currentTime = julianDate;
    console.log('✅ 太阳时间已设置为:', date);
    return true;
  } catch (error) {
    console.error('设置太阳时间失败:', error);
    return false;
  }
}

// 调试函数：检查太阳和月球显示状态
export function debugCelestialBodies(viewer) {
  if (!viewer) {
    console.error('❌ Viewer 不存在');
    return;
  }
  
  console.log('🔍 天体显示状态检查:');
  console.log('📊 Viewer 对象:', viewer);
  console.log('🌍 Scene 对象:', viewer.scene);
  
  // 检查太阳
  if (viewer.scene.sun) {
    console.log('☀️ 太阳对象存在');
    console.log('   - 显示状态:', viewer.scene.sun.show);
    console.log('   - 大小:', viewer.scene.sun.size);
    console.log('   - 亮度:', viewer.scene.sun.brightness);
  } else {
    console.log('❌ 太阳对象不存在');
  }
  
  // 检查月球
  if (viewer.scene.moon) {
    console.log('🌙 月球对象存在');
    console.log('   - 显示状态:', viewer.scene.moon.show);
    console.log('   - 纹理URL:', viewer.scene.moon.textureUrl);
  } else {
    console.log('❌ 月球对象不存在');
  }
  
  // 检查地球光照
  console.log('🌍 地球光照状态:', viewer.scene.globe.enableLighting);
  console.log('🌍 动态大气光照:', viewer.scene.globe.dynamicAtmosphereLighting);
  
  // 检查时钟
  console.log('⏰ 时钟状态:');
  console.log('   - 当前时间:', viewer.clock.currentTime);
  console.log('   - 是否动画:', viewer.clock.shouldAnimate);
  console.log('   - 时间倍数:', viewer.clock.multiplier);
  
  // 检查天空盒
  console.log('🌌 天空盒状态:', viewer.scene.skyBox ? '已启用' : '未启用');
  console.log('🌫️ 天空大气层状态:', viewer.scene.skyAtmosphere?.show);
}

// 强制启用太阳和月球显示
export function forceEnableCelestialBodies(viewer) {
  if (!viewer) {
    console.error('❌ Viewer 不存在');
    return false;
  }
  
  console.log('🔧 强制启用天体显示...');
  
  // 强制启用太阳
  if (viewer.scene.sun) {
    viewer.scene.sun.show = true;
    viewer.scene.sun.size = 1000000;
    viewer.scene.sun.brightness = 1.5;
    console.log('✅ 太阳已强制启用');
  }
  
  // 强制启用月球
  if (viewer.scene.moon) {
    viewer.scene.moon.show = true;
    console.log('✅ 月球已强制启用');
  }
  
  // 强制启用地球光照
  viewer.scene.globe.enableLighting = true;
  console.log('✅ 地球光照已强制启用');
  
  // 设置时钟动画
  viewer.clock.shouldAnimate = true;
  viewer.clock.multiplier = 1.0;
  console.log('✅ 时钟动画已启用');
  
  return true;
}

// 增强月球可见性
export function enhanceMoonVisibility(viewer, options = {}) {
  if (!viewer) {
    console.error('❌ Viewer 不存在');
    return false;
  }
  
  console.log('🌙 增强月球可见性...');
  
  const defaultOptions = {
    size: 1737100, // 月球半径（米）
    brightness: 2.0, // 亮度倍数
    contrast: 1.5, // 对比度
    textureUrl: '/data/images/moon_texture.jpg', // 月球纹理
    enableLighting: true, // 启用月球光照
    showGlow: true, // 显示月球光晕
    createCustomMoon: false // 是否创建自定义月球实体
  };
  
  const config = { ...defaultOptions, ...options };
  
  try {
    // 方法1: 尝试增强默认月球
    if (viewer.scene.moon) {
      // 确保月球显示
      viewer.scene.moon.show = true;
      
      // 设置月球大小
      if (viewer.scene.moon.size !== undefined) {
        viewer.scene.moon.size = config.size;
        console.log('✅ 月球大小已设置为:', config.size);
      }
      
      // 设置月球亮度
      if (viewer.scene.moon.brightness !== undefined) {
        viewer.scene.moon.brightness = config.brightness;
        console.log('✅ 月球亮度已设置为:', config.brightness);
      }
      
      // 设置月球对比度
      if (viewer.scene.moon.contrast !== undefined) {
        viewer.scene.moon.contrast = config.contrast;
        console.log('✅ 月球对比度已设置为:', config.contrast);
      }
      
      // 设置月球纹理
      if (config.textureUrl && viewer.scene.moon.textureUrl !== undefined) {
        viewer.scene.moon.textureUrl = config.textureUrl;
        console.log('✅ 月球纹理已设置为:', config.textureUrl);
      }
      
      // 启用月球光照
      if (config.enableLighting && viewer.scene.moon.enableLighting !== undefined) {
        viewer.scene.moon.enableLighting = true;
        console.log('✅ 月球光照已启用');
      }
      
      // 显示月球光晕（如果支持）
      if (config.showGlow && viewer.scene.moon.showGlow !== undefined) {
        viewer.scene.moon.showGlow = true;
        console.log('✅ 月球光晕已启用');
      }
    }
    
    // 方法2: 创建自定义月球实体（如果默认月球不够明显）
    if (config.createCustomMoon) {
      createCustomMoonEntity(viewer, config);
    }
    
    // 方法3: 调整场景设置以增强月球可见性
    // 降低地球大气层强度，让月球更明显
    if (viewer.scene.skyAtmosphere) {
      viewer.scene.skyAtmosphere.show = false; // 临时关闭大气层
      console.log('✅ 临时关闭大气层以增强月球可见性');
    }
    
    // 调整背景色以突出月球
    viewer.scene.backgroundColor = Cesium.Color.BLACK;
    
    console.log('✅ 月球可见性增强完成');
    return true;
    
  } catch (error) {
    console.error('❌ 月球增强失败:', error);
    return false;
  }
}

// 创建自定义月球实体
function createCustomMoonEntity(viewer, config) {
  try {
    // 移除现有的自定义月球实体
    const existingMoon = viewer.entities.getById('custom_moon');
    if (existingMoon) {
      viewer.entities.remove(existingMoon);
    }
    
    // 创建月球椭球体
    const moonEllipsoid = new Cesium.Ellipsoid(
      config.size, // 半径（米）
      config.size,
      config.size
    );
    
    // 创建自定义月球实体
    const moonEntity = viewer.entities.add({
      id: 'custom_moon',
      name: 'Custom Moon',
      position: new Cesium.SampledPositionProperty(),
      ellipsoid: {
        radii: moonEllipsoid.radii,
        material: config.textureUrl || Cesium.Color.WHITE,
        outline: true,
        outlineColor: Cesium.Color.WHITE.withAlpha(0.5),
        outlineWidth: 2,
        heightReference: Cesium.HeightReference.NONE
      }
    });
    
    // 更新月球位置
    function updateMoonPosition(time) {
      try {
        const moonPosition = Cesium.Simon1994PlanetaryPositions.computeMoonPositionInEarthInertialFrame(time);
        moonEntity.position.setValue(time, moonPosition);
      } catch (error) {
        console.warn('自定义月球位置计算失败:', error);
      }
    }
    
    // 监听时钟变化
    viewer.clock.onTick.addEventListener(updateMoonPosition);
    
    console.log('✅ 自定义月球实体已创建');
    
  } catch (error) {
    console.error('❌ 创建自定义月球实体失败:', error);
  }
}

/*
使用示例：

// 1. 创建带太阳显示的 Viewer（使用离线地图作为默认）
const viewer = createConfiguredViewer('cesiumContainer', {
  showSun: true,           // 启用太阳显示
  showMoon: true,          // 启用月球显示
  enableLighting: true,    // 启用地球光照
  sunSize: 1000000,        // 设置太阳大小
  sunBrightness: 1.5       // 设置太阳亮度
});

// 2. 创建带在线地图检测的 Viewer（优先使用在线地图，失败时自动切换到离线地图）
const viewer = await createConfiguredViewerWithOnlineCheck('cesiumContainer', {
  imagery: 'tianditu_img', // 首选天地图影像
  showSun: true,
  showMoon: true
});

// 3. 动态切换地图源
import { switchImageryProvider, switchImageryProviderWithFallback } from './cesiumConfig.js';

// 直接切换到指定地图源
switchImageryProvider(viewer, 'tianditu_img');

// 带可用性检测的切换（推荐）
await switchImageryProviderWithFallback(viewer, 'tianditu_img');

// 4. 动态控制太阳显示
import { enableSun, disableSun, toggleSun, setSunTime } from './cesiumConfig.js';

// 启用太阳显示
enableSun(viewer, {
  size: 1000000,
  brightness: 1.5,
  enableLighting: true
});

// 禁用太阳显示
disableSun(viewer);

// 切换太阳显示
toggleSun(viewer);

// 设置太阳时间（影响太阳位置）
setSunTime(viewer, new Date('2024-01-01T12:00:00Z'));

// 5. 获取太阳位置
import { getSunPosition } from './cesiumConfig.js';
const sunPosition = getSunPosition(viewer);
console.log('太阳位置:', sunPosition);

// 6. 检查地图源可用性
import { checkImageryProviderAvailability } from './cesiumConfig.js';
const isAvailable = await checkImageryProviderAvailability('tianditu_img');
console.log('天地图是否可用:', isAvailable);

离线地图使用说明：
1. 离线地图文件应放置在 public/map/ 目录下
2. 文件结构应为：public/map/{z}/{x}/{y}.jpg
3. 其中 {z} 为缩放级别，{x} 和 {y} 为瓦片坐标
4. 支持的缩放级别：0-18
5. 当在线地图无法访问时，系统会自动切换到离线地图
*/ 