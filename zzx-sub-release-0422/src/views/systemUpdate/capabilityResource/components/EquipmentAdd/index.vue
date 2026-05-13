<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="800px"
    :close-on-click-modal="false"
    @close="handleClose"
    append-to-body
    class="equipment-dialog"
  >
    <div class="equipment-add-container">
      <!-- 基础信息区域 -->
      <div class="form-section">
        <div class="section-title">
          <el-icon><InfoFilled /></el-icon>
          <span>基础信息</span>
        </div>
        
        <el-form
          ref="baseFormRef"
          :model="baseForm"
          :rules="baseRules"
          label-width="120px"
          class="equipment-form"
        >
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="装备名称" prop="name">
                <el-input 
                  v-model="baseForm.name" 
                  placeholder="请输入装备名称"
                  class="dark-input"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="装备类型" prop="equipmentType">
                <el-select 
                  v-model="baseForm.equipmentType" 
                  placeholder="请选择装备类型"
                  class="dark-select"
                  @change="handleTypeChange"
                >
                  <el-option label="测站" :value="9">
                    <div class="option-content">
                      <el-icon><Monitor /></el-icon>
                      <span>测站</span>
                    </div>
                  </el-option>
                  <el-option label="船只" :value="12">
                    <div class="option-content">
                      <el-icon><Ship /></el-icon>
                      <span>船只</span>
                    </div>
                  </el-option>
                  <el-option label="卫星" :value="5">
                    <div class="option-content">
                      <el-icon><Promotion /></el-icon>
                      <span>卫星</span>
                    </div>
                  </el-option>
                  <el-option label="指挥中心" :value="13">
                    <div class="option-content">
                      <el-icon><Setting /></el-icon>
                      <span>指挥中心</span>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="子系统类型" prop="subSystemType">
                <el-select v-model="baseForm.subSystemType" placeholder="请选择" class="dark-select">
                  <el-option
                    v-for="item in subSystemTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="阵营" prop="side">
                <el-select v-model="baseForm.side" placeholder="请选择" class="dark-select">
                  <el-option label="红方" :value="1" />
                  <el-option label="蓝方" :value="2" />
                
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="baseForm.status">
                  <el-radio :value="0">正常</el-radio>
                  <el-radio :value="1">异常</el-radio>
            
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="描述" prop="description">
            <el-input
              v-model="baseForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入装备描述"
              class="dark-textarea"
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- 动态表单区域 -->
      <div class="form-section" v-if="currentType">
        <div class="section-title">
          <el-icon><Setting /></el-icon>
          <span>{{ typeName }}信息</span>
        </div>
        
        <!-- 测站表单 -->
        <StationForm
          v-if="currentType === 9"
          ref="stationFormRef"
          v-model="stationData"
        />
        
        <!-- 船只表单 -->
        <ShipForm
          v-else-if="currentType === 12"
          ref="shipFormRef"
          v-model="shipData"
        />
        
        <!-- 卫星表单 -->
        <SatelliteForm
          v-else-if="currentType === 5"
          ref="satelliteFormRef"
          v-model="satelliteData"
          @select-payload="handleSelectPayload"
        />
        
        <!-- 指挥中心表单 -->
        <CommandCenterForm
          v-else-if="currentType === 13"
          ref="commandCenterFormRef"
          v-model="commandCenterData"
        />
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          提交
        </el-button>
      </div>
    </template>

    <!-- 载荷选择弹窗 -->
    <PayloadSelector
      v-model="payloadVisible"
      @confirm="handlePayloadConfirm"
    />
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled, Setting, Monitor, Ship, Promotion } from '@element-plus/icons-vue'
import { useDict } from '@/utils/dict'
import StationForm from './StationForm.vue'
import ShipForm from './ShipForm.vue'
import SatelliteForm from './SatelliteForm.vue'
import CommandCenterForm from './CommandCenterForm.vue'
import PayloadSelector from './PayloadSelector.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'submit'])

// 对话框显示
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 提交状态
const submitting = ref(false)
const payloadVisible = ref(false)

// 使用字典获取子系统类型选项
const { subsystem_type: subSystemTypeOptions } = useDict('subsystem_type')

// 基础表单
const baseFormRef = ref()
const baseForm = ref({
  name: '',
  subSystemType: '',
  status: 0,
  description: '',
  side: 1,
  equipmentType: 9
})

// 基础表单验证规则
const baseRules = {
  name: [
    { required: true, message: '请输入装备名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  equipmentType: [
    { required: true, message: '请选择装备类型', trigger: 'change' }
  ],
  subSystemType: [
    { required: true, message: '请选择子系统类型', trigger: 'change' }
  ],
  side: [
    { required: true, message: '请选择阵营', trigger: 'change' }
  ]
}

// 当前选中的类型
const currentType = computed(() => baseForm.value.equipmentType)
const typeName = computed(() => {
  const map = { 9: '测站', 12: '船只', 5: '卫星', 13: '指挥中心' }
  return map[currentType.value] || ''
})

// 各类型数据
const stationData = ref({
  basicInfo: {
    stationName: '',
    longitude: null,
    latitude: null,
    altitude: null,
    stationCode: ''
  },
  parameters: {
    workMode: 1,
    maxTrackingRadius: null,
    maxLineSightRate: null,
    maxDistance: null,
    antennaInfos: []
  }
})

const shipData = ref({
  basicInfo: {
    code: '',
    country: '',
    shipType: 0,
    displacement: null
  },
  parameters: {
    heading: null,
    speed: null
  }
})

// 指挥中心数据
const commandCenterData = ref({
  basicInfo: {
    longitude: null,
    latitude: null,
    altitude: null,
  }
})

const satelliteData = ref({
  basicInfo: {
    country: '',
    operator: '',
    orbitType: '0',
    orbitAltitude: null,
    mass: null,
      designLife: null,
    tleLine1: '',
    tleLine2: '',
    satName: '',
    semiMajorAxis: null,
    eccentricity: null,
    inclination: null,
    raan: null,
    argPerigee: null,
    meanAnomaly: null,
    norad: '',
    orbitEpoch: null
  },
  parameters: []  // 载荷ID列表
})

// 各类型表单引用
const stationFormRef = ref()
const shipFormRef = ref()
const satelliteFormRef = ref()
const commandCenterFormRef = ref()

// 类型切换
const handleTypeChange = () => {
  // 重置表单数据
  stationData.value = {
    basicInfo: { stationName: '', longitude: null, latitude: null, altitude: null, stationCode: '' },
    parameters: { workMode: 1, maxTrackingRadius: null, maxLineSightRate: null, maxDistance: null, antennaInfos: [] }
  }
  shipData.value = {
    basicInfo: { code: '', country: '', shipType: 0, displacement: null },
    parameters: { heading: null, speed: null }
  }
  satelliteData.value = {
    basicInfo: {
      country: '', operator: '', orbitType: '0', orbitAltitude: null,
      mass: null, designLife: null, tleLine1: '', tleLine2: '',
      satName: '', semiMajorAxis: null, eccentricity: null,
      inclination: null, raan: null, argPerigee: null,
      meanAnomaly: null, norad: '', orbitEpoch: null
    },
    parameters: []
  }
}

// 选择载荷
const handleSelectPayload = () => {
  payloadVisible.value = true
}

const handlePayloadConfirm = (selectedPayloads) => {
  satelliteData.value.parameters = selectedPayloads.map(p => p.id)
  ElMessage.success(`已选择 ${selectedPayloads.length} 个载荷`)
}

// 提交表单
const handleSubmit = async () => {
  try {
    // 验证基础表单
    await baseFormRef.value.validate()
    
    // 验证表单
    let specialValid = false
    if (currentType.value === 9) {
      specialValid = await stationFormRef.value?.validate()
    } else if (currentType.value === 12) {
      specialValid = await shipFormRef.value?.validate()
    } else if (currentType.value === 5) {
      specialValid = await satelliteFormRef.value?.validate()
    } else if (currentType.value === 13) {
      specialValid = await commandCenterFormRef.value?.validate()
    }
    
    // if (!specialValid) {
    //   ElMessage.error('请完善表单信息')
    //   return
    // }
    
    submitting.value = true
    
    // 构建提交数据
    const submitData = {
      ...baseForm.value,
      basicInfo: {},
      parameters: {}
    }
    
    if (currentType.value === 9) {
      submitData.basicInfo = stationData.value.basicInfo
      submitData.parameters = stationData.value.parameters
    } else if (currentType.value === 12) {
      submitData.basicInfo = shipData.value.basicInfo
      submitData.parameters = shipData.value.parameters
    } else if (currentType.value === 5) {
      submitData.basicInfo = satelliteData.value.basicInfo
      submitData.parameters = satelliteData.value.parameters
    } else if (currentType.value === 13) {
      submitData.basicInfo = commandCenterData.value.basicInfo
      submitData.parameters = undefined
    }
    
    emit('submit', submitData)
    ElMessage.success('提交成功')
    handleClose()
  } catch (error) {
    console.error('表单验证失败:', error)
    ElMessage.error('请完善表单信息')
  } finally {
    submitting.value = false
  }
}

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false
  // 重置表单
  baseForm.value = {
    name: '', subSystemType: 1, status: 0,
    description: '', side: 1, equipmentType: 9
  }
  handleTypeChange()
}
</script>

<style lang="scss" scoped>
.equipment-dialog {
  :deep(.el-dialog) {
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
    border: 1px solid rgba(64, 158, 255, 0.3);
    border-radius: 12px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  }

  :deep(.el-dialog__header) {
    border-bottom: 1px solid rgba(64, 158, 255, 0.2);
    padding: 20px 24px;
    
    .el-dialog__title {
      color: #e4e7ed;
      font-size: 18px;
      font-weight: 600;
    }
  }

  :deep(.el-dialog__body) {
    padding: 24px;
    max-height: 70vh;
    overflow-y: auto;
    
    &::-webkit-scrollbar {
      width: 6px;
    }
    
    &::-webkit-scrollbar-track {
      background: rgba(0, 0, 0, 0.2);
      border-radius: 3px;
    }
    
    &::-webkit-scrollbar-thumb {
      background: rgba(64, 158, 255, 0.3);
      border-radius: 3px;
      
      &:hover {
        background: rgba(64, 158, 255, 0.5);
      }
    }
  }

  :deep(.el-dialog__footer) {
    border-top: 1px solid rgba(64, 158, 255, 0.2);
    padding: 16px 24px;
  }
}

.equipment-add-container {
  .form-section {
    margin-bottom: 32px;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    .section-title {
      display: flex;
      align-items: center;
      gap: 8px;
      padding-bottom: 12px;
      margin-bottom: 20px;
      border-bottom: 2px solid rgba(64, 158, 255, 0.3);
      
      .el-icon {
        font-size: 18px;
        color: #409eff;
      }
      
      span {
        font-size: 16px;
        font-weight: 600;
        color: #e4e7ed;
        letter-spacing: 1px;
      }
    }
  }
  
  .equipment-form {
    :deep(.el-form-item__label) {
      color: #a0a0a0;
      font-weight: 500;
    }
    
    :deep(.el-input__wrapper) {
      background: rgba(0, 0, 0, 0.3);
      border: 1px solid rgba(64, 158, 255, 0.2);
      box-shadow: none;
      
      &:hover {
        border-color: rgba(64, 158, 255, 0.5);
      }
      
      &.is-focus {
        border-color: #409eff;
        box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
      }
    }
    
    :deep(.el-input__inner) {
      color: #e4e7ed;
      background: transparent;
    }
    
    :deep(.el-textarea__inner) {
      background: rgba(0, 0, 0, 0.3);
      border: 1px solid rgba(64, 158, 255, 0.2);
      color: #e4e7ed;
      
      &:hover {
        border-color: rgba(64, 158, 255, 0.5);
      }
      
      &:focus {
        border-color: #409eff;
        box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
      }
    }
    
    :deep(.el-select) {
      .el-input__wrapper {
        background: rgba(0, 0, 0, 0.3);
      }
    }
    
    :deep(.el-radio) {
      .el-radio__label {
        color: #e4e7ed;
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  
  .el-button {
    padding: 8px 24px;
    
    &:not(.el-button--primary) {
      background: transparent;
      border-color: rgba(64, 158, 255, 0.3);
      color: #e4e7ed;
      
      &:hover {
        border-color: #409eff;
        color: #409eff;
      }
    }
  }
}

.option-content {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .el-icon {
    font-size: 16px;
  }
}
</style>