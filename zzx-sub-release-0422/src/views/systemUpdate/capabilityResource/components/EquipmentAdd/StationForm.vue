<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="rules"
    label-width="120px"
    class="special-form"
  >
    <!-- 基本信息 -->
    <div class="sub-section">
      <div class="sub-title">
        <el-icon><Location /></el-icon>
        <span>基本信息</span>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="测站名称" prop="basicInfo.stationName">
            <el-input v-model="formData.basicInfo.stationName" placeholder="请输入测站名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="测站编码" prop="basicInfo.stationCode">
            <el-input v-model="formData.basicInfo.stationCode" placeholder="请输入测站编码" />
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="经度" prop="basicInfo.longitude">
            <el-input-number
              v-model="formData.basicInfo.longitude"
              :precision="6"
              :step="0.000001"
              placeholder="经度"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="纬度" prop="basicInfo.latitude">
            <el-input-number
              v-model="formData.basicInfo.latitude"
              :precision="6"
              :step="0.000001"
              placeholder="纬度"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="海拔高度" prop="basicInfo.altitude">
            <el-input-number
              v-model="formData.basicInfo.altitude"
              :precision="2"
              :step="10"
              placeholder="海拔(m)"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </div>
    
    <!-- 参数配置 -->
    <div class="sub-section">
      <div class="sub-title">
        <el-icon><Setting /></el-icon>
        <span>参数配置</span>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="工作模式" prop="parameters.workMode">
            <el-select v-model="formData.parameters.workMode" style="width: 100%">
              <el-option :value="1" label="跟踪模式" />
              <el-option :value="2" label="扫描模式" />
              <el-option :value="3" label="待机模式" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="最大跟踪半径" prop="parameters.maxTrackingRadius">
            <el-input-number
              v-model="formData.parameters.maxTrackingRadius"
              :precision="3"
              :step="100"
              placeholder="km"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="最大视线速率" prop="parameters.maxLineSightRate">
            <el-input-number
              v-model="formData.parameters.maxLineSightRate"
              :precision="4"
              :step="0.1"
              placeholder="°/s"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="最大作用距离" prop="parameters.maxDistance">
            <el-input-number
              v-model="formData.parameters.maxDistance"
              :precision="3"
              :step="100"
              placeholder="km"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      
      <!-- 天线信息 -->
      <div class="antenna-section">
        <div class="antenna-header">
          <span>天线配置</span>
          <el-button type="primary" size="small" @click="addAntenna">
            <el-icon><Plus /></el-icon>添加天线
          </el-button>
        </div>
        
        <div v-for="(antenna, idx) in formData.parameters.antennaInfos" :key="idx" class="antenna-item">
          <div class="antenna-title">
            <span>天线 {{ idx + 1 }}</span>
            <el-button type="danger" size="small" link @click="removeAntenna(idx)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="最小仰角" :prop="`parameters.antennaInfos.${idx}.minElevationAngle`">
                <el-input-number
                  v-model="antenna.minElevationAngle"
                  :precision="1"
                  :step="0.5"
                  placeholder="deg"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="最小接收角" :prop="`parameters.antennaInfos.${idx}.minReceiveAngle`">
                <el-input-number
                  v-model="antenna.minReceiveAngle"
                  :precision="1"
                  :step="0.5"
                  placeholder="deg"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="频率范围">
                <div class="frequency-range">
                  <el-input-number
                    v-model="antenna.frequencyDetected[0].frequencyMin"
                    :precision="1"
                    :step="0.5"
                    placeholder="最小(GHz)"
                    style="width: 45%"
                  />
                  <span>~</span>
                  <el-input-number
                    v-model="antenna.frequencyDetected[0].frequencyMax"
                    :precision="1"
                    :step="0.5"
                    placeholder="最大(GHz)"
                    style="width: 45%"
                  />
                </div>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </div>
    </div>
  </el-form>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Location, Setting, Plus, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['update:modelValue'])

const formRef = ref()
const formData = ref(props.modelValue)

watch(formData, (val) => {
  emit('update:modelValue', val)
}, { deep: true })

const rules = {
  'basicInfo.stationName': [
    { required: true, message: '请输入测站名称', trigger: 'blur' }
  ],
  'basicInfo.longitude': [
    { required: true, message: '请输入经度', trigger: 'blur' },
    { type: 'number', min: -180, max: 180, message: '经度范围 -180 ~ 180', trigger: 'blur' }
  ],
  'basicInfo.latitude': [
    { required: true, message: '请输入纬度', trigger: 'blur' },
    { type: 'number', min: -90, max: 90, message: '纬度范围 -90 ~ 90', trigger: 'blur' }
  ]
}

const addAntenna = () => {
  formData.value.parameters.antennaInfos.push({
    frequencyDetected: [{ frequencyMin: null, frequencyMax: null }],
    minElevationAngle: null,
    minReceiveAngle: null
  })
}

const removeAntenna = (index) => {
  formData.value.parameters.antennaInfos.splice(index, 1)
}

const validate = () => {
  return formRef.value?.validate()
}

defineExpose({ validate })
</script>

<style lang="scss" scoped>
.special-form {
  .sub-section {
    margin-bottom: 24px;
    
    .sub-title {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-bottom: 16px;
      padding-left: 8px;
      border-left: 3px solid #409eff;
      
      .el-icon {
        font-size: 14px;
        color: #409eff;
      }
      
      span {
        font-size: 14px;
        font-weight: 500;
        color: #c0c4cc;
      }
    }
  }
  
  .antenna-section {
    margin-top: 16px;
    
    .antenna-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      padding: 8px 0;
      border-bottom: 1px dashed rgba(64, 158, 255, 0.2);
      
      span {
        color: #a0a0a0;
        font-size: 13px;
      }
    }
    
    .antenna-item {
      background: rgba(0, 0, 0, 0.2);
      border-radius: 8px;
      padding: 12px;
      margin-bottom: 12px;
      
      .antenna-title {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
        padding-bottom: 8px;
        border-bottom: 1px solid rgba(64, 158, 255, 0.1);
        
        span {
          color: #409eff;
          font-size: 13px;
          font-weight: 500;
        }
      }
    }
  }
  
  .frequency-range {
    display: flex;
    align-items: center;
    gap: 8px;
    
    span {
      color: #a0a0a0;
    }
  }
  
  :deep(.el-form-item__label) {
    color: #a0a0a0;
    font-size: 13px;
  }
  
  :deep(.el-input-number) {
    width: 100%;
  }
}
</style>