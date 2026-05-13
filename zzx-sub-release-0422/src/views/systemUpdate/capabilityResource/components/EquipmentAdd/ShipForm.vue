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
        <el-icon><Ship /></el-icon>
        <span>船只信息</span>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="船只编号" prop="basicInfo.code">
            <el-input v-model="formData.basicInfo.code" placeholder="请输入船只编号" />
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
          <el-form-item label="船只类型" prop="basicInfo.shipType">
            <el-select v-model="formData.basicInfo.shipType" style="width: 100%">
              <el-option :value="0" label="驱逐舰" />
              <el-option :value="1" label="护卫舰" />
              <el-option :value="2" label="巡洋舰" />
              <el-option :value="3" label="航空母舰" />
              <el-option :value="4" label="补给舰" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="排水量" prop="basicInfo.displacement">
            <el-input-number
              v-model="formData.basicInfo.displacement"
              :precision="2"
              :step="100"
              placeholder="吨"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </div>
    
    <div class="sub-section">
      <div class="sub-title">
        <el-icon><Compass /></el-icon>
        <span>航行参数</span>
      </div>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="航向" prop="parameters.heading">
            <el-input-number
              v-model="formData.parameters.heading"
              :precision="2"
              :step="0.5"
              :min="0"
              :max="360"
              placeholder="度"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="航速" prop="parameters.speed">
            <el-input-number
              v-model="formData.parameters.speed"
              :precision="2"
              :step="0.5"
              :min="0"
              placeholder="节"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </div>
  </el-form>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Ship, Compass } from '@element-plus/icons-vue'

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
  'basicInfo.code': [
    { required: true, message: '请输入船只编号', trigger: 'blur' }
  ],
  'basicInfo.country': [
    { required: true, message: '请输入所属国家', trigger: 'blur' }
  ],
  'basicInfo.displacement': [
    { required: true, message: '请输入排水量', trigger: 'blur' },
    { type: 'number', min: 0, message: '排水量必须大于0', trigger: 'blur' }
  ]
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
  
  :deep(.el-form-item__label) {
    color: #a0a0a0;
    font-size: 13px;
  }
  
  :deep(.el-input-number) {
    width: 100%;
  }
}
</style>