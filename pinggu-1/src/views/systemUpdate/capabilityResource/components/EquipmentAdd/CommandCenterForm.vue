<template>
  <div class="command-center-form">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="经度" prop="longitude">
          <el-input-number
            v-model="formData.basicInfo.longitude"
            :precision="3"
            :step="0.01"
            :min="-180"
            :max="180"
            placeholder="请输入经度"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="纬度" prop="latitude">
          <el-input-number
            v-model="formData.basicInfo.latitude"
            :precision="3"
            :step="0.01"
            :min="-90"
            :max="90"
            placeholder="请输入纬度"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="海拔高度(m)" prop="altitude">
          <el-input-number
            v-model="formData.basicInfo.altitude"
            :precision="2"
            :step="0.01"
            placeholder="请输入海拔高度"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({
      basicInfo: {
        longitude: null,
        latitude: null,
        altitude: null,
      }
    })
  }
})

const emit = defineEmits(['update:modelValue'])

// 使用计算属性简化数据流
const formData = ref({
  basicInfo: {
    longitude: null,
    latitude: null,
    altitude: null,
 
  }
})

// 初始化时从props获取数据
if (props.modelValue?.basicInfo) {
  formData.value.basicInfo = { ...props.modelValue.basicInfo }
}

// 监听formData变化并emit
watch(formData, (newVal) => {
  emit('update:modelValue', {
    basicInfo: { ...newVal.basicInfo }
  })
}, { deep: true })

// 表单验证
defineExpose({
  validate: () => {
    const { longitude, latitude, altitude, commandCenterName } = formData.value.basicInfo
    
    // 检查必填字段
    if (longitude === null || longitude === '') {
      return Promise.reject(new Error('请填写经度'))
    }
    if (latitude === null || latitude === '') {
      return Promise.reject(new Error('请填写纬度'))
    }
    if (altitude === null || altitude === '') {
      return Promise.reject(new Error('请填写海拔高度'))
    }
    
    return Promise.resolve()
  }
})
</script>

<style scoped>
.command-center-form {
  padding: 20px 0;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-input-number) {
  width: 100%;
}
</style>