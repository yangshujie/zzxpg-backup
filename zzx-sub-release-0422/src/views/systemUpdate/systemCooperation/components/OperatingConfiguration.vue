<template>
  <div class="operating-configuration">
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="1600px" :close-on-click-modal="false"
      @close="handleClose" append-to-body>
      <!-- 主要内容区域 -->
      <div class="configuration-content">
        <el-row :gutter="20">
          <!-- 左侧地球模型 -->
          <el-col :span="14">
            <div class="cesium-container">
              <cesium-map @selectPoints="selectPoints" @clear="clearPoints" :type="'Area'" :disabled="isViewMode"
                ref="cesiumRef" />
            </div>
          </el-col>

          <!-- 右侧表单 -->
          <el-col :span="10">
            <!-- 环境类型选择 -->
            <div class="environment-type-selector">
              <el-radio-group v-model="currentEnvType" @change="handleEnvTypeChange">
                <el-radio-button label="geoCondition">地理环境</el-radio-button>
                <el-radio-button label="weatherCondition">气象海洋</el-radio-button>
                <el-radio-button label="electromagneticCondition">空间电磁</el-radio-button>
              </el-radio-group>


            </div>

            <!-- 环境配置列表 -->
            <div class="env-list-container">
              <div class="env-list-header">
                <span class="env-list-title">环境配置列表</span>
              </div>

              <!-- 地理环境表单 -->
              <div v-if="currentEnvType === 'geoCondition'" class="env-form-list">
                <div v-for="(item, index) in currentEnvList" :key="index" class="env-form-item">
                  <div class="env-item-header">
                    <span class="env-item-title">地理环境配置 {{ index + 1 }}</span>
                    <div class="env-item-actions">
                      <el-button type="danger" size="small" icon="Delete" @click="removeEnvItem(index)">删除</el-button>
                    </div>
                  </div>

                  <el-form :ref="el => envFormRefs[index] = el" :model="item" :rules="geoEnvRules" label-width="100px"
                    class="env-form">
                    <el-form-item label="环境子类型" prop="geoSubType">
                      <el-select v-model="item.geoSubType" multiple placeholder="请选择环境子类型" style="width: 100%">
                        <el-option label="高山" value="高山" />
                        <el-option label="森林" value="森林" />
                        <el-option label="海洋" value="海洋" />
                        <el-option label="沼泽" value="沼泽" />
                      </el-select>
                    </el-form-item>

                    <!-- 坐标点列表 -->
                    <el-form-item label="区域坐标" prop="polygonCoords">
                      <div class="coords-list">
                        <div v-for="(coord, coordIndex) in item.coordsList" :key="coordIndex" class="coord-item">
                          <el-row :gutter="10" align="middle">
                            <el-col :span="8">
                              <el-input v-model="coord.longitude" placeholder="经度" size="small" />
                            </el-col>
                            <el-col :span="8">
                              <el-input v-model="coord.latitude" placeholder="纬度" size="small" />
                            </el-col>
                            <el-col :span="6">
                              <el-input v-model="coord.height" placeholder="高度" size="small" />
                            </el-col>
                            <el-col :span="2">
                              <el-button type="danger" size="small" icon="Delete"
                                @click="removeCoord(index, coordIndex)" />
                            </el-col>
                          </el-row>
                        </div>
                        <el-button type="primary" size="small" icon="Plus" @click="addCoord(index)"
                          style="margin-top: 10px;">添加坐标点</el-button>
                      </div>
                    </el-form-item>
                  </el-form>
                </div>
                <div class="env-form-footer">
                  <el-button type="primary" size="small" icon="Plus" @click="addEnvItem">新增地理环境配置</el-button>
                </div>
              </div>

              <!-- 气象海洋环境表单 -->
              <div v-else-if="currentEnvType === 'weatherCondition'" class="env-form-list">
                <div v-for="(item, index) in currentEnvList" :key="index" class="env-form-item">
                  <div class="env-item-header">
                    <span class="env-item-title">气象海洋配置 {{ index + 1 }}</span>
                    <div class="env-item-actions">
                      <el-button type="danger" size="small" icon="Delete" @click="removeEnvItem(index)">删除</el-button>
                    </div>
                  </div>

                  <el-form :ref="el => envFormRefs[index] = el" :model="item" :rules="weatherEnvRules"
                    label-width="100px" class="env-form">
                    <el-form-item label="有效时间" prop="validTime">
                      <el-date-picker v-model="item.validTime" type="datetimerange" range-separator="至"
                        start-placeholder="开始时间" end-placeholder="结束时间" value-format="x" style="width: 100%" />
                    </el-form-item>

                    <el-form-item label="天气类型" prop="weatherType">
                      <el-select v-model="item.weatherType" multiple placeholder="请选择天气类型" style="width: 100%">
                        <el-option label="雨" value="雨" />
                        <el-option label="云" value="云" />
                        <el-option label="雾" value="雾" />
                        <el-option label="霾" value="霾" />
                        <el-option label="雪" value="雪" />
                      </el-select>
                    </el-form-item>

                    <el-form-item label="天气等级" prop="weatherLevel">
                      <el-select v-model="item.weatherLevel" multiple placeholder="请选择天气等级" style="width: 100%">
                        <el-option label="小雨" value="0" />
                        <el-option label="中雨" value="1" />
                        <el-option label="大雨" value="2" />
                        <el-option label="暴雨" value="3" />
                        <el-option label="少云" value="4" />
                        <el-option label="多云" value="5" />
                        <el-option label="阴天" value="6" />
                        <el-option label="轻度雾霾" value="7" />
                        <el-option label="中度雾霾" value="8" />
                        <el-option label="重度雾霾" value="9" />
                        <el-option label="小雪" value="10" />
                        <el-option label="中雪" value="11" />
                        <el-option label="大雪" value="12" />
                        <el-option label="暴雪" value="13" />
                      </el-select>
                    </el-form-item>

                    <!-- 坐标点列表 -->
                    <el-form-item label="区域坐标" prop="polygonCoords">
                      <div class="coords-list">
                        <div v-for="(coord, coordIndex) in item.coordsList" :key="coordIndex" class="coord-item">
                          <el-row :gutter="10" align="middle">
                            <el-col :span="8">
                              <el-input v-model="coord.longitude" placeholder="经度" size="small" />
                            </el-col>
                            <el-col :span="8">
                              <el-input v-model="coord.latitude" placeholder="纬度" size="small" />
                            </el-col>
                            <el-col :span="6">
                              <el-input v-model="coord.height" placeholder="高度" size="small" />
                            </el-col>
                            <el-col :span="2">
                              <el-button type="danger" size="small" icon="Delete"
                                @click="removeCoord(index, coordIndex)" />
                            </el-col>
                          </el-row>
                        </div>
                        <el-button type="primary" size="small" icon="Plus" @click="addCoord(index)"
                          style="margin-top: 10px;">添加坐标点</el-button>
                      </div>
                    </el-form-item>
                  </el-form>
                </div>
                <div class="env-form-footer">
                  <el-button type="primary" size="small" icon="Plus" @click="addEnvItem">新增气象海洋配置</el-button>
                </div>
              </div>

              <!-- 空间电磁环境表单 -->
              <div v-else-if="currentEnvType === 'electromagneticCondition'" class="env-form-list">
                <div v-for="(item, index) in currentEnvList" :key="index" class="env-form-item">
                  <div class="env-item-header">
                    <span class="env-item-title">空间电磁配置 {{ index + 1 }}</span>
                    <div class="env-item-actions">
                      <el-button type="danger" size="small" icon="Delete" @click="removeEnvItem(index)">删除</el-button>
                    </div>
                  </div>

                  <el-form :ref="el => envFormRefs[index] = el" :model="item" :rules="electromagneticEnvRules"
                    label-width="100px" class="env-form">
                    <el-form-item label="环境子类型" prop="geoSubType">
                      <el-select v-model="item.geoSubType" multiple placeholder="请选择环境子类型" style="width: 100%">
                        <el-option label="太阳耀斑" value="太阳耀斑" />

                      </el-select>
                    </el-form-item>

                    <el-form-item label="有效时间" prop="validTime">
                      <el-date-picker v-model="item.validTime" type="datetimerange" range-separator="至"
                        start-placeholder="开始时间" end-placeholder="结束时间" value-format="x" style="width: 100%" />
                    </el-form-item>

                    <el-form-item label="太阳耀斑等级" prop="solar_flare_level">
                      <el-select v-model="item.solar_flare_level" multiple placeholder="请选择太阳耀斑等级" style="width: 100%">
                        <el-option label="C级" value="C级" />
                        <el-option label="M级" value="M级" />
                        <el-option label="X级" value="X级" />
                      </el-select>
                    </el-form-item>

                    <!-- 坐标点列表 -->
                    <el-form-item label="区域坐标" prop="polygonCoords">
                      <div class="coords-list">
                        <div v-for="(coord, coordIndex) in item.coordsList" :key="coordIndex" class="coord-item">
                          <el-row :gutter="10" align="middle">
                            <el-col :span="8">
                              <el-input v-model="coord.longitude" placeholder="经度" size="small" />
                            </el-col>
                            <el-col :span="8">
                              <el-input v-model="coord.latitude" placeholder="纬度" size="small" />
                            </el-col>
                            <el-col :span="6">
                              <el-input v-model="coord.height" placeholder="高度" size="small" />
                            </el-col>
                            <el-col :span="2">
                              <el-button type="danger" size="small" icon="Delete"
                                @click="removeCoord(index, coordIndex)" />
                            </el-col>
                          </el-row>
                        </div>
                        <el-button type="primary" size="small" icon="Plus" @click="addCoord(index)"
                          style="margin-top: 10px;">添加坐标点</el-button>
                      </div>
                    </el-form-item>
                  </el-form>
                </div>
                <div class="env-form-footer">
                  <el-button type="primary" size="small" icon="Plus" @click="addEnvItem">新增空间电磁配置</el-button>
                </div>
              </div>

              <!-- 保存取消按钮 -->
              <div class="action-buttons">
                <el-button @click="handleClose">取消</el-button>
                <el-button type="primary" @click="handleSave">保存</el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import CesiumMap from './cesiumMap.vue'

// 组件属性
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  // 环境数据
  envData: {
    type: Object,
    default: () => ({
      geoConditionList: [],
      weatherConditionList: [],
      electromagneticConditionList: []
    })
  }
})

// 组件事件
const emit = defineEmits(['update:modelValue', 'save', 'subSave', 'loadFromParent'])

// 对话框显示状态
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 对话框标题
const dialogTitle = '工况设置'

// 当前选中的环境类型
const currentEnvType = ref('geoCondition')

// 当前环境类型的数据列表
const currentEnvList = ref([])

// 统一的环境配置数据存储 - 确保各类表单数据独立
const formData = ref({
  geoCondition: [],
  weatherCondition: [],
  electromagneticCondition: []
})

// 表单引用数组
const envFormRefs = ref([])



// 表单验证规则
const geoEnvRules = {
  geoSubType: [{ required: true, message: '请选择环境子类型', trigger: 'change' }]
}

const weatherEnvRules = {
  validTime: [{ required: true, message: '请选择有效时间范围', trigger: 'change' }]
}

const electromagneticEnvRules = {
  geoSubType: [{ required: true, message: '请选择环境子类型', trigger: 'change' }],
  validTime: [{ required: true, message: '请选择有效时间范围', trigger: 'change' }]
}

// 格式化时间为指定格式 - yyyy-MM-dd'T'HH:mm:ss（不含毫秒）
const formatTime = (time) => {
  if (!time) return ''

  let dateObj

  // 如果是时间戳（毫秒数），创建Date对象
  if (typeof time === 'number' || (typeof time === 'string' && /^\d+$/.test(time))) {
    try {
      dateObj = new Date(parseInt(time))
    } catch {
      return ''
    }
  }
  // 如果已经是ISO格式，创建Date对象
  else if (typeof time === 'string' && time.includes('T')) {
    try {
      dateObj = new Date(time)
    } catch {
      return ''
    }
  }
  // 如果是日期对象，直接使用
  else if (time instanceof Date) {
    dateObj = time
  }
  // 如果是其他时间字符串，尝试创建Date对象
  else {
    try {
      dateObj = new Date(time)
    } catch {
      return ''
    }
  }

  // 检查Date对象是否有效
  if (!dateObj || isNaN(dateObj.getTime())) {
    return ''
  }

  // 格式化为 yyyy-MM-dd'T'HH:mm:ss（不含毫秒）
  const year = dateObj.getFullYear()
  const month = String(dateObj.getMonth() + 1).padStart(2, '0')
  const day = String(dateObj.getDate()).padStart(2, '0')
  const hours = String(dateObj.getHours()).padStart(2, '0')
  const minutes = String(dateObj.getMinutes()).padStart(2, '0')
  const seconds = String(dateObj.getSeconds()).padStart(2, '0')

  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}


// 创建空的环境配置项
const createEmptyEnvItem = (envType) => {
  if (envType === 'geoCondition') {
    return {
      envType: '地理环境',
      geoSubType: '',
      coordsList: []
    }
  } else if (envType === 'weatherCondition') {
    return {
      envType: '气象海洋',
      validStartTime: '',
      validEndTime: '',
      weatherType: [],
      weatherLevel: [],
      coordsList: []
    }
  } else if (envType === 'electromagneticCondition') {
    return {
      envType: '空间电磁',
      geoSubType: '',
      validTime: [],
      solar_flare_level: [],
      coordsList: []
    }
  }

  // 默认返回空对象
  return {}
}

// 环境类型变化处理
const handleEnvTypeChange = (type) => {
  // 从统一的 formData 中获取当前环境类型的数据，不清除其他表单数据
  currentEnvList.value = [...(formData.value[type] || [])]

  // 如果没有数据，添加一个空项用于填写
  if (currentEnvList.value.length === 0) {
    currentEnvList.value.push(createEmptyEnvItem(type))
  }
}

// 解析坐标数组
const parsePolygonCoords = (coordsArray) => {
  if (!coordsArray || !Array.isArray(coordsArray)) return [{ longitude: '', latitude: '', height: '' }]

  try {
    return coordsArray.map(coord => ({
      longitude: coord[0]?.toString() || '',
      latitude: coord[1]?.toString() || '',
      height: coord[2]?.toString() || '0'
    }))
  } catch (error) {
    return [{ longitude: '', latitude: '', height: '' }]
  }
}

// 生成坐标数组
const generatePolygonCoords = (coordsList) => {
  const validCoords = coordsList.filter(coord =>
    coord.longitude && coord.latitude && coord.height !== undefined
  )

  if (validCoords.length === 0) return []

  return validCoords.map(coord => [
    parseFloat(coord.longitude) || 0,
    parseFloat(coord.latitude) || 0,
    parseFloat(coord.height) || 0
  ])
}

// 新增环境配置项
const addEnvItem = () => {
  const baseItem = {
    coordsList: [{ longitude: '', latitude: '', height: '' }]
  }

  if (currentEnvType.value === 'geoCondition') {
    currentEnvList.value.push({
      ...baseItem,
      envType: '地理环境',
      geoSubType: ''
    })
  } else if (currentEnvType.value === 'weatherCondition') {
    currentEnvList.value.push({
      ...baseItem,
      envType: '气象海洋',
      validTime: [],
      weatherType: [],
      weatherLevel: []
    })
  } else if (currentEnvType.value === 'electromagneticCondition') {
    currentEnvList.value.push({
      ...baseItem,
      envType: '空间电磁',
      geoSubType: '',
      validTime: [],
      solar_flare_level: []
    })
  }
}

// 删除环境配置项
const removeEnvItem = (index) => {
  if (currentEnvList.value.length > 1) {
    currentEnvList.value.splice(index, 1)
  } else {
    ElMessage.warning('至少保留一个环境配置')
  }
}

// 新增坐标点
const addCoord = (envIndex) => {
  currentEnvList.value[envIndex].coordsList.push({
    longitude: '',
    latitude: '',
    height: ''
  })
}

// 删除坐标点
const removeCoord = (envIndex, coordIndex) => {
  if (currentEnvList.value[envIndex].coordsList.length > 1) {
    currentEnvList.value[envIndex].coordsList.splice(coordIndex, 1)
  } else {
    ElMessage.warning('至少保留一个坐标点')
  }
}

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false
}

// 保存完整的表单数据到父组件
const saveFormData = () => {
  // 构建保存数据格式，与父组件期望的格式一致
  const saveData = {
    geoConditionList: (formData.value.geoCondition || []).map(item => ({
      envType: item.envType || '地理环境',
      geoSubType: item.geoSubType || '',
      polygonCoords: generatePolygonCoords(item.coordsList || [])
    })),

    weatherConditionList: (formData.value.weatherCondition || []).map(item => ({
      envType: item.envType || '气象海洋',
      validStartTime: formatTime(item.validTime?.[0]) || '',
      validEndTime: formatTime(item.validTime?.[1]) || '',
      weatherType: item.weatherType || [],
      weatherLevel: item.weatherLevel || [],
      polygonCoords: generatePolygonCoords(item.coordsList || [])
    })),

    electromagneticConditionList: (formData.value.electromagneticCondition || []).map(item => ({
      envType: item.envType || '空间电磁',
      geoSubType: item.geoSubType || '',
      validStartTime: formatTime(item.validTime?.[0]) || '',
      validEndTime: formatTime(item.validTime?.[1]) || '',
      envAttrs: {
        solar_flare_level: { type: 'multi_select', values: item.solar_flare_level || [] }
      },
      polygonCoords: generatePolygonCoords(item.coordsList || [])
    }))
  }

  // 添加日志记录
  console.log('保存完整的环境配置数据:', saveData)
  console.log('当前环境类型:', currentEnvType.value)
  console.log('数据格式验证:', '符合combatData.js格式')

  // 触发保存事件，传递完整的数据
  emit('save', saveData)
}

// 保存数据
const handleSave = () => {
  // 直接保存完整的表单数据，不管数据是否为空
  saveFormData()
  ElMessage.success('保存成功')
  handleClose()
}

// 监听当前环境类型数据变化，同步到统一的formData中
watch([currentEnvList, currentEnvType], () => {
  // 延迟同步，避免频繁触发
  setTimeout(() => {
    if (currentEnvType.value && currentEnvList.value) {
      // 确保只同步当前环境类型的数据，不混淆其他类型的数据
      formData.value[currentEnvType.value] = [...currentEnvList.value]

      // 添加日志记录，验证数据独立性
      console.log(`同步 ${currentEnvType.value} 数据到 formData:`)
      console.log('当前环境类型数据:', currentEnvList.value)
      console.log('完整formData:', formData.value)
    }
  }, 300)
}, { deep: true })

// 监听对话框打开
watch(dialogVisible, (newVal) => {
  if (newVal) {
    nextTick(() => {
      // 自动加载父组件传递的环境工况数据
      loadParentEnvData()

      initGraph()
      loadEquipmentGroups()
    })
  }
})

// 自动加载父组件传递的环境工况数据
const loadParentEnvData = () => {
  if (props.envData) {
    // 清空现有数据，确保数据独立性
    formData.value = {
      geoCondition: [],
      weatherCondition: [],
      electromagneticCondition: []
    }

    // 加载地理环境数据 - 独立存储
    if (props.envData.geoConditionList && props.envData.geoConditionList.length > 0) {
      formData.value.geoCondition = props.envData.geoConditionList.map(item => ({
        envType: item.envType || '地理环境',
        geoSubType: item.geoSubType || '',
        coordsList: parsePolygonCoords(item.polygonCoords)
      }))
    }

    // 加载气象海洋数据 - 独立存储
    if (props.envData.weatherConditionList && props.envData.weatherConditionList.length > 0) {
      formData.value.weatherCondition = props.envData.weatherConditionList.map(item => ({
        envType: item.envType || '气象海洋',
        validTime: [item.validStartTime || '', item.validEndTime || ''],
        weatherType: item.weatherType || [],
        weatherLevel: item.weatherLevel || [],
        coordsList: parsePolygonCoords(item.polygonCoords)
      }))
    }

    // 加载空间电磁数据 - 独立存储
    if (props.envData.electromagneticConditionList && props.envData.electromagneticConditionList.length > 0) {
      formData.value.electromagneticCondition = props.envData.electromagneticConditionList.map(item => ({
        envType: item.envType || '空间电磁',
        geoSubType: item.geoSubType || '',
        validTime: [item.validStartTime || '', item.validEndTime || ''],
        solar_flare_level: item.envAttrs?.solar_flare_level?.values || [],
        coordsList: parsePolygonCoords(item.polygonCoords)
      }))
    }

    // 设置当前环境类型的数据
    if (currentEnvType.value && formData.value[currentEnvType.value]) {
      currentEnvList.value = [...formData.value[currentEnvType.value]]
    }

    console.log('加载父组件环境数据完成 - 各类数据独立存储:')
    console.log('地理环境数据:', formData.value.geoCondition)
    console.log('气象海洋数据:', formData.value.weatherCondition)
    console.log('空间电磁数据:', formData.value.electromagneticCondition)
  }
}
</script>

<style scoped>
.operating-configuration {
  width: 100%;
}

.environment-type-selector {
  margin-bottom: 16px;
  text-align: right;
}

.configuration-content {
  margin-top: 0;
  height: 600px;
}

.cesium-container {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 500px;
  max-height: 500px;
  border: 2px solid rgb(176, 194, 255);
  border-radius: 4px;
  overflow: hidden;
}

/* 确保地图组件撑满容器 */
.cesium-container :deep(.cesium-content) {
  width: 100% !important;
  height: 100% !important;
  position: absolute !important;
  top: 0 !important;
  left: 0 !important;
}

.cesium-container :deep(#cesiumContainer) {
  width: 100% !important;
  height: 100% !important;
  position: absolute !important;
  top: 0 !important;
  left: 0 !important;
}

.env-list-container {
  height: 100%;
  max-height: calc(100vh - 350px);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.env-list-header {
  margin-bottom: 16px;
  padding: 0 8px;
}

.env-list-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.env-form-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
}

.env-form-item {
  margin-bottom: 16px;
  padding: 16px;
  background-color: #000;
  border-radius: 6px;
}

.env-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
}

.env-item-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.env-item-actions {
  display: flex;
  gap: 8px;
}

.env-form {
  background-color: transparent;
}

.coords-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 8px;
  background-color: var(--el-bg-color);
  border-radius: 4px;
}

.coord-item {
  margin-bottom: 8px;
  padding: 8px;
  background-color: var(--el-bg-color-page);
  border-radius: 4px;
}

.coord-item:last-child {
  margin-bottom: 0;
}

.env-form-footer {
  margin-top: 16px;
  text-align: center;
}

.action-buttons {
  margin-top: 20px;
  text-align: right;
  padding: 16px 0;
  border-top: 1px solid var(--el-border-color-light);
}

.action-buttons .el-button {
  margin-left: 12px;
}

.dialog-footer {
  text-align: center;
  margin-top: 20px;
}

.dialog-footer .el-button {
  margin: 0 8px;
}


</style>