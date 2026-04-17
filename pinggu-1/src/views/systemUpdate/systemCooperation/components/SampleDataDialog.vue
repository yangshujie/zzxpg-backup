<template>
  <el-dialog
    title="评估子样数据"
    :model-value="open"
    width="1400px"
    append-to-body
    @update:model-value="(val) => $emit('update:open', val)"
    @close="handleClose"
  >
    <!-- 方案信息 -->
    <div class="scheme-info">
      <el-descriptions title="方案信息" :column="3" border>
        <el-descriptions-item label="方案名称">{{ schemeInfo?.schemeName }}</el-descriptions-item>
        <el-descriptions-item label="方案类型">{{ getSchemeTypeLabel(schemeInfo?.schemeType) }}</el-descriptions-item>
        <el-descriptions-item label="样本总数">{{ sampleData?.total || 0 }}</el-descriptions-item>
        <el-descriptions-item label="生成时间">{{ formatTime(schemeInfo?.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ schemeInfo?.creator }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 子样数据表格 -->
    <div class="sample-table" v-if="sampleData?.data?.length > 0">
      <h3>子样数据详情</h3>
      
      <!-- 使用Element Plus表格组件 -->
      <div class="table-container">
        <el-table 
          :data="sampleData.data" 
          border 
          stripe 
          style="width: 100%"
          :header-cell-style="{ background: '#f5f7fa', color: '#303133' }"
        >
          <!-- 样本编号列 -->
          <el-table-column 
            prop="schemeNo" 
            label="样本编号" 
            width="120" 
            fixed="left"
          />
          
          <!-- 蓝方装备列 -->
          <el-table-column 
            v-for="equip in blueEquipmentList" 
            :key="equip.equipmentId"
            :label="equip.equipmentName"
            width="120"
          >
            <template #header>
              <div class="column-header">
                <div class="section-title">蓝方装备</div>
                <div class="equipment-name">{{ equip.equipmentName }}</div>
              </div>
            </template>
            <template #default="{ row }">
              <el-checkbox 
                :model-value="isEquipmentSelected(row, 'blue', equip.equipmentId)"
                disabled
              />
            </template>
          </el-table-column>
          
          <!-- 红方装备列 -->
          <el-table-column 
            v-for="equip in redEquipmentList" 
            :key="equip.equipmentId"
            :label="equip.equipmentName"
            width="120"
          >
            <template #header>
              <div class="column-header">
                <div class="section-title">红方装备</div>
                <div class="equipment-name">{{ equip.equipmentName }}</div>
              </div>
            </template>
            <template #default="{ row }">
              <el-checkbox 
                :model-value="isEquipmentSelected(row, 'red', equip.equipmentId)"
                disabled
              />
            </template>
          </el-table-column>
          
          <!-- 环境条件列 -->
          <template v-for="envSection in environmentSections" :key="envSection.key">
            <el-table-column 
              v-for="envItem in envSection.items" 
              :key="envItem.key"
              :label="envItem.label"
              width="120"
            >
              <template #header>
                <div class="column-header">
                  <div class="section-title">{{ envSection.label }}</div>
                  <div class="environment-name">{{ envItem.label }}</div>
                </div>
              </template>
              <template #default="{ row }">
                <div v-if="envSection.type === 'geo'">
                  <el-checkbox 
                    :model-value="isGeoSelected(row, envItem.index, envItem.subType)"
                    disabled
                  />
                </div>
                <div v-else-if="envSection.type === 'weather'">
                  <el-checkbox 
                    :model-value="isWeatherSelected(row, envItem.index, envItem.weatherType)"
                    disabled
                  />
                  <div v-if="row.dimensionData[envItem.key]?.Level" class="level-badge">
                    L{{ row.dimensionData[envItem.key]?.Level }}
                  </div>
                </div>
                <div v-else-if="envSection.type === 'electromagnetic'">
                  <el-checkbox 
                    :model-value="isElectromagneticSelected(row, envItem.index, envItem.subType)"
                    disabled
                  />
                </div>
              </template>
            </el-table-column>
          </template>
        </el-table>
      </div>
    </div>

    <div v-else class="no-data">
      <el-empty description="暂无子样数据" />
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
        <el-button type="primary" @click="handleExport">导出数据</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  open: Boolean,
  sampleData: Object,
  schemeInfo: Object
})

const emit = defineEmits(['update:open'])

// 方案类型选项
const schemeTypeOptions = [
  { label: '性能评估', value: 'performance' },
  { label: '效能评估', value: 'efficiency' },
  { label: '可靠性评估', value: 'reliability' }
]

// 计算属性
const blueEquipmentList = computed(() => {
  return props.schemeInfo?.blueEquipmentList || []
})

const redEquipmentList = computed(() => {
  return props.schemeInfo?.redEquipmentList || []
})

const blueEquipmentCount = computed(() => blueEquipmentList.value.length)
const redEquipmentCount = computed(() => redEquipmentList.value.length)

// 环境条件分段配置
const environmentSections = computed(() => {
  const sections = []
  
  // 地理环境
  const geoConditions = props.schemeInfo?.geoConditionList || []
  if (geoConditions.length > 0) {
    sections.push({
      key: 'geo',
      type: 'geo',
      label: '地理环境',
      colspan: geoConditions.reduce((total, geo, index) => {
        return total + (geo.geoSubType?.length || 1)
      }, 0),
      items: geoConditions.flatMap((geo, index) => {
        const subTypes = geo.geoSubType || []
        return subTypes.map(subType => ({
          key: `geo-${index}-${subType}`,
          label: subType,
          index: index,
          subType: subType
        }))
      })
    })
  }
  
  // 气象海洋环境
  const weatherConditions = props.schemeInfo?.weatherConditionList || []
  if (weatherConditions.length > 0) {
    sections.push({
      key: 'weather',
      type: 'weather',
      label: '气象海洋环境',
      colspan: weatherConditions.reduce((total, weather, index) => {
        return total + (weather.weatherType?.length || 1)
      }, 0),
      items: weatherConditions.flatMap((weather, index) => {
        const weatherTypes = weather.weatherType || []
        return weatherTypes.map(weatherType => ({
          key: `weather-${index}-${weatherType}`,
          label: weatherType,
          index: index,
          weatherType: weatherType
        }))
      })
    })
  }
  
  // 空间电磁环境
  const emConditions = props.schemeInfo?.electromagneticConditionList || []
  if (emConditions.length > 0) {
    sections.push({
      key: 'electromagnetic',
      type: 'electromagnetic',
      label: '空间电磁环境',
      colspan: emConditions.reduce((total, em, index) => {
        return total + (em.geoSubType?.length || 1)
      }, 0),
      items: emConditions.flatMap((em, index) => {
        const subTypes = em.geoSubType || []
        return subTypes.map(subType => ({
          key: `em-${index}-${subType}`,
          label: subType,
          index: index,
          subType: subType
        }))
      })
    })
  }
  
  return sections
})

// 获取方案类型标签
const getSchemeTypeLabel = (value) => {
  const option = schemeTypeOptions.find(item => item.value === value)
  return option ? option.label : value
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

// 检查装备是否被选中
const isEquipmentSelected = (sample, side, equipmentId) => {
  const equipmentList = sample.dimensionData?.[`${side}EquipmentList`] || []
  return equipmentList.some(equip => equip.equipmentId === equipmentId)
}

// 检查地理环境是否被选中
const isGeoSelected = (sample, index, subType) => {
  const geoData = sample.dimensionData?.[`地理环境${index + 1}`]
  if (!geoData) return false
  
  return geoData.geoSubType?.includes(subType) && geoData.value === subType
}

// 检查气象环境是否被选中
const isWeatherSelected = (sample, index, weatherType) => {
  const weatherData = sample.dimensionData?.[`气象环境${index + 1}`]
  if (!weatherData) return false
  
  return weatherData.weatherType?.includes(weatherType) && weatherData.value === weatherType
}

// 检查电磁环境是否被选中
const isElectromagneticSelected = (sample, index, subType) => {
  const emData = sample.dimensionData?.[`电磁环境${index + 1}`]
  if (!emData) return false
  
  return emData.geoSubType?.includes(subType) && emData.value === subType
}

// 关闭弹窗
const handleClose = () => {
  emit('update:open', false)
}

// 导出数据
const handleExport = () => {
  // 这里实现数据导出逻辑
  ElMessage.success('导出功能开发中')
}

// 监听数据变化
watch(() => props.sampleData, (newData) => {
  if (newData) {
    console.log('子样数据:', newData)
  }
})
</script>

<style scoped>
.scheme-info {
  margin-bottom: 20px;
}

.sample-table {
  margin-top: 20px;
}

.sample-table h3 {
  margin-bottom: 15px;
  color: #303133;
}

.table-container {
  width: 100%;
  overflow-x: auto;
}

.column-header {
  text-align: center;
  line-height: 1.2;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 2px;
}

.equipment-name {
  font-size: 12px;
  color: #606266;
}

.environment-name {
  font-size: 12px;
  color: #606266;
}

.level-badge {
  display: inline-block;
  background: #ff4d4f;
  color: white;
  border-radius: 2px;
  padding: 1px 4px;
  font-size: 12px;
  margin-left: 4px;
}

.no-data {
  text-align: center;
  padding: 40px 0;
  color: #909399;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>