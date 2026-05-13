<template>
  <el-dialog
    :title="dialogTitle"
    :open="open"
    width="1200px"
    append-to-body
    @close="handleClose"
    @update:open="(value) => emit('update:open', value)"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
      <!-- 基础信息 -->
      <el-form-item label="方案名称" prop="schemeName">
        <el-input v-model="formData.schemeName" placeholder="请输入方案名称" />
      </el-form-item>
      
      <el-form-item label="方案类型" prop="schemeType">
        <el-select v-model="formData.schemeType" placeholder="请选择方案类型">
          <el-option
            v-for="item in schemeTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <!-- 剖面选择 -->
      <el-form-item label="关联剖面" prop="profileId">
        <el-select 
          v-model="formData.profileId" 
          placeholder="请选择作战剖面"
          @change="handleProfileChange"
          style="width: 100%"
        >
          <el-option
            v-for="profile in profileList"
            :key="profile.profileId"
            :label="profile.profileName"
            :value="profile.profileId"
          />
        </el-select>
      </el-form-item>

      <!-- 抽样配置 -->
      <div v-if="formData.profileId" class="sampling-config">
        <h3>抽样配置</h3>
        
        <!-- 总体抽样配置 -->
        <el-card class="sampling-card">
          <template #header>
            <span>总体抽样配置</span>
          </template>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="样本总数" prop="sampleCount">
                <el-input-number 
                  v-model="formData.sampleCount" 
                  :min="1" 
                  :max="1000" 
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="蓝方抽样类型" prop="blueSamplingType">
                <el-select v-model="formData.blueSamplingType" style="width: 100%">
                  <el-option label="简单随机" value="简单随机" />
                  <el-option label="均匀分层" value="均匀分层" />
                  <el-option label="拉丁超立方" value="拉丁超立方" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="蓝方样本数" prop="blueSampleCount">
                <el-input-number 
                  v-model="formData.blueSampleCount" 
                  :min="1" 
                  :max="formData.sampleCount" 
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="红方抽样类型" prop="redSamplingType">
                <el-select v-model="formData.redSamplingType" style="width: 100%">
                  <el-option label="简单随机" value="简单随机" />
                  <el-option label="均匀分层" value="均匀分层" />
                  <el-option label="拉丁超立方" value="拉丁超立方" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="红方样本数" prop="redSampleCount">
                <el-input-number 
                  v-model="formData.redSampleCount" 
                  :min="1" 
                  :max="formData.sampleCount" 
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-card>

        <!-- 环境条件抽样配置 -->
        <div v-if="profileDetailData" class="environment-config">
          <!-- 地理环境配置 -->
          <div v-if="profileDetailData.geoConditionList?.length > 0" class="env-section">
            <h4>地理环境配置</h4>
            <div
              v-for="(geo, index) in profileDetailData.geoConditionList" 
              :key="index"
              class="env-item"
            >
              <div class="env-label">{{ geo.geoName }}</div>
              <el-checkbox-group v-model="formData.geoConditionList[index].geoSubType">
                <el-checkbox 
                  v-for="subType in geo.geoSubType" 
                  :key="subType"
                  :label="subType"
                >
                  {{ subType }}
                </el-checkbox>
              </el-checkbox-group>
            </div>
          </div>

          <!-- 气象海洋环境配置 -->
          <div v-if="profileDetailData.weatherConditionList?.length > 0" class="env-section">
            <h4>气象海洋环境配置</h4>
            <div
              v-for="(weather, index) in profileDetailData.weatherConditionList" 
              :key="index"
              class="env-item"
            >
              <div class="env-label">{{ weather.weatherName }}</div>
              <el-checkbox-group v-model="formData.weatherConditionList[index].weatherType">
                <el-checkbox 
                  v-for="weatherType in weather.weatherType" 
                  :key="weatherType"
                  :label="weatherType"
                >
                  {{ weatherType }}
                </el-checkbox>
              </el-checkbox-group>
            </div>
          </div>

          <!-- 空间电磁环境配置 -->
          <div v-if="profileDetailData.electromagneticConditionList?.length > 0" class="env-section">
            <h4>空间电磁环境配置</h4>
            <div
              v-for="(em, index) in profileDetailData.electromagneticConditionList" 
              :key="index"
              class="env-item"
            >
              <div class="env-label">{{ em.geoName }}</div>
              <el-checkbox-group v-model="formData.electromagneticConditionList[index].geoSubType">
                <el-checkbox 
                  v-for="subType in em.geoSubType" 
                  :key="subType"
                  :label="subType"
                >
                  {{ subType }}
                </el-checkbox>
              </el-checkbox-group>
            </div>
          </div>
        </div>
      </div>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存方案</el-button>
        <el-button type="success" @click="handleGenerateSample" :disabled="!formData.profileId">生成子样</el-button>
      </span>
    </template>

    <!-- 子样数据展示 -->
    <div v-if="sampleData?.data?.length > 0" class="sample-data-section">
      <h3>子样数据</h3>
      
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
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { addEvaluationScheme, updateEvaluationScheme, generateEvaluationScheme } from '@/api/systemPlus/systemCooperation/evaluation'
import { selectList, profileDetail as getProfileDetail } from '@/api/systemPlus/systemCooperation/combatProfile'

const props = defineProps({
  open: Boolean,
  editData: Object
})

const emit = defineEmits(['update:open', 'submit', 'cancel'])

// 响应式数据
const formRef = ref()
const profileList = ref([])
const profileDetailData = ref(null)
const sampleData = ref(null)

const formData = reactive({
  schemeName: '',
  schemeType: '',
  profileId: '',
  sampleCount: 20,
  blueSamplingType: '简单随机',
  blueSampleCount: 2,
  redSamplingType: '均匀分层',
  redSampleCount: 10,
  geoConditionList: [],
  weatherConditionList: [],
  electromagneticConditionList: [],
  blueEquipmentList: [],
  redEquipmentList: []
})

// 方案类型选项
const schemeTypeOptions = [
  { label: '性能评估', value: 'performance' },
  { label: '效能评估', value: 'efficiency' },
  { label: '可靠性评估', value: 'reliability' }
]

// 表单验证规则
const rules = {
  schemeName: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
  schemeType: [{ required: true, message: '请选择方案类型', trigger: 'change' }],
  profileId: [{ required: true, message: '请选择作战剖面', trigger: 'change' }],
  sampleCount: [{ required: true, message: '请输入样本总数', trigger: 'blur' }]
}

// 计算属性
const dialogTitle = computed(() => {
  return props.editData ? '编辑评估方案' : '新增评估方案'
})

const blueEquipmentList = computed(() => {
  return formData.blueEquipmentList || []
})

const redEquipmentList = computed(() => {
  return formData.redEquipmentList || []
})

// 环境条件分段配置
const environmentSections = computed(() => {
  const sections = []
  
  // 地理环境
  const geoConditions = formData.geoConditionList || []
  if (geoConditions.length > 0) {
    sections.push({
      key: 'geo',
      type: 'geo',
      label: '地理环境',
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
  const weatherConditions = formData.weatherConditionList || []
  if (weatherConditions.length > 0) {
    sections.push({
      key: 'weather',
      type: 'weather',
      label: '气象海洋环境',
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
  const emConditions = formData.electromagneticConditionList || []
  if (emConditions.length > 0) {
    sections.push({
      key: 'electromagnetic',
      type: 'electromagnetic',
      label: '空间电磁环境',
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

// 监听弹窗打开状态
watch(() => props.open, (newVal) => {
  if (newVal && props.editData) {
    // 编辑模式，填充数据
    Object.assign(formData, props.editData)
    if (props.editData.profileId) {
      loadProfileDetail(props.editData.profileId)
    }
  } else if (newVal) {
    // 新增模式，重置数据
    resetForm()
  }
})

// 加载剖面列表
const loadProfileList = async () => {
  try {
    const response = await selectList()
    if (response.code === 200) {
      profileList.value = response.data || []
    }
  } catch (error) {
    console.error('加载剖面列表失败:', error)
    ElMessage.error('加载剖面列表失败')
  }
}

// 加载剖面详情
const loadProfileDetail = async (profileId) => {
  try {
    const response = await getProfileDetail({ profileId })
    if (response.code === 200) {
      profileDetailData.value = response.data
      
      // 初始化抽样配置
      initSamplingConfig(response.data)
    }
  } catch (error) {
    console.error('加载剖面详情失败:', error)
    ElMessage.error('加载剖面详情失败')
  }
}

// 初始化抽样配置
const initSamplingConfig = (profileData) => {
  // 地理环境
  if (profileData.geoConditionList) {
    formData.geoConditionList = profileData.geoConditionList.map(geo => ({
      ...geo,
      samplingType: geo.samplingType || '简单随机'
    }))
  }
  
  // 气象海洋环境
  if (profileData.weatherConditionList) {
    formData.weatherConditionList = profileData.weatherConditionList.map(weather => ({
      ...weather,
      samplingType: weather.samplingType || '简单随机',
      subSamplingType: weather.subSamplingType || {}
    }))
  }
  
  // 空间电磁环境
  if (profileData.electromagneticConditionList) {
    formData.electromagneticConditionList = profileData.electromagneticConditionList.map(em => ({
      ...em,
      samplingType: em.samplingType || '简单随机'
    }))
  }
  
  // 装备列表
  formData.blueEquipmentList = profileData.blueEquipmentList || []
  formData.redEquipmentList = profileData.redEquipmentList || []
}

// 剖面变更处理
const handleProfileChange = (profileId) => {
  if (profileId) {
    loadProfileDetail(profileId)
  } else {
    profileDetailData.value = null
    resetSamplingConfig()
  }
}

// 重置抽样配置
const resetSamplingConfig = () => {
  formData.geoConditionList = []
  formData.weatherConditionList = []
  formData.electromagneticConditionList = []
  formData.blueEquipmentList = []
  formData.redEquipmentList = []
}

// 重置表单
const resetForm = () => {
  Object.assign(formData, {
    schemeName: '',
    schemeType: '',
    profileId: '',
    sampleCount: 20,
    blueSamplingType: '简单随机',
    blueSampleCount: 2,
    redSamplingType: '均匀分层',
    redSampleCount: 10,
    geoConditionList: [],
    weatherConditionList: [],
    electromagneticConditionList: [],
    blueEquipmentList: [],
    redEquipmentList: []
  })
  profileDetailData.value = null
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

// 生成子样数据
const handleGenerateSample = async () => {
  try {
    // 调用生成接口
    const response = await generateEvaluationScheme({
      ...formData,
      sampleCount: formData.sampleCount || 20
    })
    
    if (response.code === 200) {
      sampleData.value = response.data || []
      ElMessage.success('子样数据生成成功')
    }
  } catch (error) {
    console.error('生成子样数据失败:', error)
    ElMessage.error('生成失败')
  }
}

// 关闭弹窗
const handleClose = () => {
  emit('update:open', false)
  emit('cancel')
  sampleData.value = null // 清空子样数据
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    // 准备提交数据
    const submitData = {
      ...formData,
      createTime: new Date().getTime(),
      creator: '当前用户' // 实际项目中应从用户信息获取
    }
    
    emit('submit', submitData)
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

// 初始化加载
onMounted(() => {
  loadProfileList()
})
</script>

<style scoped>
.sampling-config {
  margin-top: 20px;
}

.sampling-config h3 {
  margin-bottom: 15px;
  color: var(--el-text-color-primary);
  border-left: 4px solid var(--el-color-primary);
  padding-left: 10px;
}

.sampling-card {
  margin-bottom: 20px;
}

.env-section {
  margin-bottom: 20px;
}

.env-section h4 {
  margin-bottom: 15px;
  color: var(--el-text-color-regular);
  font-weight: normal;
}

.env-card {
  margin-bottom: 15px;
}

.sub-sampling {
  margin-top: 15px;
  padding: 15px;
  background: var(--el-fill-color-lighter);
  border-radius: 4px;
}

.sub-sampling h5 {
  margin-bottom: 10px;
  color: var(--el-text-color-secondary);
}

.sub-sampling-item {
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--el-border-color);
}

.sub-sampling-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

/* 适配暗色主题 */
:deep(.el-dialog) {
  background: var(--el-bg-color);
}

:deep(.el-dialog__header) {
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color);
}

:deep(.el-dialog__title) {
  color: var(--el-text-color-primary);
}

:deep(.el-form-item__label) {
  color: var(--el-text-color-regular);
}

:deep(.el-input__inner) {
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  border-color: var(--el-border-color);
}

:deep(.el-input__inner:focus) {
  border-color: var(--el-color-primary);
}

:deep(.el-select .el-input__inner) {
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
}

:deep(.el-card) {
  background: var(--el-bg-color);
  border-color: var(--el-border-color);
}

:deep(.el-card__header) {
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color);
}

:deep(.el-checkbox__label) {
  color: var(--el-text-color-regular);
}

/* 子样数据表格适配 */
.sample-data-section {
  margin-top: 20px;
}

.sample-data-section h3 {
  color: var(--el-text-color-primary);
  margin-bottom: 15px;
}

:deep(.sample-data-section .el-table) {
  background: var(--el-bg-color);
}

:deep(.sample-data-section .el-table th) {
  background: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
}

:deep(.sample-data-section .el-table td) {
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
}

:deep(.sample-data-section .el-table--border) {
  border: 1px solid var(--el-border-color);
}

:deep(.sample-data-section .el-table--border th) {
  border-right: 1px solid var(--el-border-color);
}

:deep(.sample-data-section .el-table--border td) {
  border-right: 1px solid var(--el-border-color);
}

.column-header {
  text-align: center;
}

.section-title {
  font-weight: bold;
  color: var(--el-text-color-primary);
}

.equipment-name {
  color: var(--el-text-color-regular);
  font-size: 12px;
}
</style>