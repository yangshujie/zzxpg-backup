<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="rules"
    label-width="120px"
    class="special-form"
  >
    <div class="sub-section">
      <div class="sub-title">
        <el-icon><Promotion /></el-icon>
        <span>轨道信息</span>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="轨道类型" prop="basicInfo.orbitType">
            <el-select v-model="formData.basicInfo.orbitType" style="width: 100%">
              <el-option value="0" label="低轨(LEO)" />
              <el-option value="1" label="中轨(MEO)" />
              <el-option value="2" label="地球同步轨道(GEO)" />
              <el-option value="3" label="大椭圆轨道" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="轨道高度" prop="basicInfo.orbitAltitude">
            <el-input-number
              v-model="formData.basicInfo.orbitAltitude"
              :precision="1"
              :step="50"
              placeholder="km"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="半长轴" prop="basicInfo.semiMajorAxis">
            <el-input-number
              v-model="formData.basicInfo.semiMajorAxis"
              :precision="3"
              :step="10"
              placeholder="km"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="偏心率" prop="basicInfo.eccentricity">
            <el-input-number
              v-model="formData.basicInfo.eccentricity"
              :precision="7"
              :step="0.000001"
              :min="0"
              placeholder="km"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="轨道倾角" prop="basicInfo.inclination">
            <el-input-number
              v-model="formData.basicInfo.inclination"
              :precision="4"
              :step="0.5"
              :min="0"
              :max="180"
              placeholder="度"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="升交点赤经" prop="basicInfo.raan">
            <el-input-number
              v-model="formData.basicInfo.raan"
              :precision="4"
              :step="0.5"
              :min="0"
              :max="360"
              placeholder="度"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="近地点幅角" prop="basicInfo.argPerigee">
            <el-input-number
              v-model="formData.basicInfo.argPerigee"
              :precision="4"
              :step="0.5"
              :min="0"
              :max="360"
              placeholder="度"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="平近点角" prop="basicInfo.meanAnomaly">
            <el-input-number
              v-model="formData.basicInfo.meanAnomaly"
              :precision="4"
              :step="0.5"
              :min="0"
              :max="360"
              placeholder="度"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </div>
    
    <div class="sub-section">
      <div class="sub-title">
        <el-icon><InfoFilled /></el-icon>
        <span>基本信息</span>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="卫星名称" prop="basicInfo.satName">
            <el-input v-model="formData.basicInfo.satName" placeholder="请输入卫星名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="所属国家" prop="basicInfo.country">
            <el-input v-model="formData.basicInfo.country" placeholder="请输入所属国家" />
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="运营机构" prop="basicInfo.operator">
            <el-input v-model="formData.basicInfo.operator" placeholder="请输入运营机构" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="NORAD编号" prop="basicInfo.norad">
            <el-input v-model="formData.basicInfo.norad" placeholder="请输入NORAD编号" />
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="卫星质量" prop="basicInfo.mass">
            <el-input-number
              v-model="formData.basicInfo.mass"
              :precision="1"
              :step="100"
              placeholder="kg"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="设计寿命" prop="basicInfo.designLife">
            <el-input-number
              v-model="formData.basicInfo.designLife"
              :precision="1"
              :step="1"
              placeholder="年"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-form-item label="TLE Line1" prop="basicInfo.tleLine1">
        <el-input v-model="formData.basicInfo.tleLine1" placeholder="请输入TLE第一行" />
      </el-form-item>
      
      <el-form-item label="TLE Line2" prop="basicInfo.tleLine2">
        <el-input v-model="formData.basicInfo.tleLine2" placeholder="请输入TLE第二行" />
      </el-form-item>
    </div>
    
    <div class="sub-section">
      <div class="sub-title">
        <el-icon><Grid /></el-icon>
        <span>载荷配置</span>
      </div>
      
      <div class="payload-section">
        <div class="payload-info">
          <span>已选择载荷: {{ formData.parameters.length }} 个</span>
          <el-button type="primary" size="small" @click="handleSelectPayload">
            <el-icon><Select /></el-icon>选择载荷
          </el-button>
        </div>
        
        <div v-if="formData.parameters.length > 0" class="selected-payloads">
          <el-tag
            v-for="(payloadId, idx) in formData.parameters"
            :key="idx"
            closable
            @close="removePayload(idx)"
            class="payload-tag"
          >
            载荷 ID: {{ payloadId }}
          </el-tag>
        </div>
        
        <div v-else class="empty-payload">
          <el-empty description="暂无载荷配置" :image-size="60" />
        </div>
      </div>
    </div>
  </el-form>

  <!-- 载荷选择弹窗 -->
  <PayloadSelector
    v-model="payloadDialogVisible"
    @confirm="handlePayloadConfirm"
  />
</template>

<script setup>
import { ref, watch } from 'vue'
import { Promotion, InfoFilled, Grid, Select } from '@element-plus/icons-vue'
import PayloadSelector from './PayloadSelector.vue'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['update:modelValue', 'select-payload'])

const formRef = ref()
const formData = ref(props.modelValue)
const payloadDialogVisible = ref(false)

watch(formData, (val) => {
  emit('update:modelValue', val)
}, { deep: true })

const rules = {
  'basicInfo.satName': [
    { required: true, message: '请输入卫星名称', trigger: 'blur' }
  ],
  'basicInfo.country': [
    { required: true, message: '请输入所属国家', trigger: 'blur' }
  ],
  'basicInfo.orbitAltitude': [
    { required: true, message: '请输入轨道高度', trigger: 'blur' }
  ]
}

const handleSelectPayload = () => {
  payloadDialogVisible.value = true
}

const handlePayloadConfirm = (selectedPayloads) => {
  // 将选中的载荷ID保存到表单数据中
  formData.value.parameters = []
  
  // 遍历所有类型的载荷，将选中的载荷ID添加到参数中
  Object.values(selectedPayloads).forEach(payloads => {
    payloads.forEach(payload => {
      formData.value.parameters.push(payload.payloadId)
    })
  })
  
  payloadDialogVisible.value = false
}

const removePayload = (index) => {
  formData.value.parameters.splice(index, 1)
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
  
  .payload-section {
    .payload-info {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      
      span {
        color: #a0a0a0;
        font-size: 13px;
      }
    }
    
    .selected-payloads {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      min-height: 60px;
      padding: 12px;
      background: rgba(0, 0, 0, 0.2);
      border-radius: 8px;
      
      .payload-tag {
        background: rgba(64, 158, 255, 0.2);
        border-color: #409eff;
        color: #409eff;
      }
    }
    
    .empty-payload {
      min-height: 100px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.2);
      border-radius: 8px;
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