<template>
  <el-form ref="formRef" :model="formModel" :label-width="labelWidth" :class="formClass" :disabled="disabled">
    <!-- 动态生成表单字段 - label和输入框在同一行 -->
    <el-form-item v-for="field in filteredFields" :key="field.key" :label="field.label" :prop="field.key"
      class="inline-form-item">
      <!-- <component :is="getFieldComponent(field)" v-model="formModel[field.key]"
       :placeholder="getPlaceholder(field)"
        :style="{ width: fieldWidth }" v-bind="getFieldProps(field)" :disabled="isFieldDisabled(field)"
        class="form-field-input">
       
        <template v-if="field.component === 'select'">
          <el-option v-for="option in getFieldOptions(field)" :key="option.value" :label="option.label"
            :value="option.value" />
        </template>

        <template v-if="field.component === 'textarea'">
          <el-input v-model="formModel[field.key]" type="textarea" :rows="field.rows || 3"
            :placeholder="getPlaceholder(field)" :style="{ width: fieldWidth }" class="form-field-input" />
        </template>
      </component> -->
      <el-input 
        v-if="field.component === 'input' || !field.component"
        :model-value="formModel[field.key]"
        @update:model-value="handleInput(field.key, $event)"
        :placeholder="getPlaceholder(field)"
        :style="{ width: fieldWidth }"
        :disabled="isFieldDisabled(field)"
        class="form-field-input"
      />
      
      <!-- 数字输入框 -->
      <el-input-number 
        v-else-if="field.component === 'input-number'"
        :model-value="formModel[field.key]"
        @update:model-value="handleInput(field.key, $event)"
        :placeholder="getPlaceholder(field)"
        :style="{ width: fieldWidth }"
        v-bind="getFieldProps(field)"
        :disabled="isFieldDisabled(field)"
        class="form-field-input"
      />
      
      <!-- 选择框 -->
      <el-select 
        v-else-if="field.component === 'select'"
        :model-value="formModel[field.key]"
        @update:model-value="handleInput(field.key, $event)"
        :placeholder="getPlaceholder(field)"
        :style="{ width: fieldWidth }"
        :disabled="isFieldDisabled(field)"
        class="form-field-input"
      >
        <el-option v-for="option in getFieldOptions(field)" :key="option.value" :label="option.label"
          :value="option.value" />
      </el-select>
      
      <!-- 文本域 -->
      <el-input 
        v-else-if="field.component === 'textarea'"
        :model-value="formModel[field.key]"
        @update:model-value="handleInput(field.key, $event)"
        type="textarea" 
        :rows="field.rows || 3"
        :placeholder="getPlaceholder(field)" 
        :style="{ width: fieldWidth }" 
        class="form-field-input" 
      />
      
      <!-- 默认输入框 -->
      <el-input 
        v-else
        :model-value="formModel[field.key]"
        @update:model-value="handleInput(field.key, $event)"
        :placeholder="getPlaceholder(field)"
        :style="{ width: fieldWidth }"
        :disabled="isFieldDisabled(field)"
        class="form-field-input"
      />
    </el-form-item>

    <!-- 自定义插槽 -->
    <slot name="custom-fields"></slot>
  </el-form>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  // 表单配置
  formConfig: {
    type: Object,
    required: true,
    default: () => ({})
  },
  // 表单数据
  modelValue: {
    type: Object,
    default: () => ({})
  },
  // 标签宽度
  labelWidth: {
    type: String,
    default: '150px'
  },
  // 字段宽度
  fieldWidth: {
    type: String,
    default: '300px'  // 增加输入框宽度
  },
  // 表单类名
  formClass: {
    type: String,
    default: 'inline-form'
  },
  // 是否禁用整个表单
  disabled: {
    type: Boolean,
    default: false
  },
  //新增或编辑
  mode: {
    type: String,
    default: 'add'
  },
})

const emit = defineEmits(['update:modelValue', 'change'])

const formRef = ref()
const formModel = ref({})

// 修改JSON格式，添加组件类型信息
const enhancedTableConfig = computed(() => {
  return props.formConfig.table.map(field => {
    //   let componentType = 'input'
    //   let options = []

    //   // 状态类字段使用选择框
    //   if (field.key.includes('Status') || field.key.includes('Mode')) {
    //     componentType = 'select'

    //     if (field.key === 'equStatus') {
    //       options = [
    //         { label: '在线', value: 1 },
    //         { label: '离线', value: 0 },
    //         { label: '维护', value: 2 }
    //       ]
    //     } else if (field.key === 'trackingMode') {
    //       options = [
    //         { label: '自动', value: 0 },
    //         { label: '手动', value: 1 },
    //         { label: '程序', value: 2 }
    //       ]
    //     } else if (field.key === 'lockStatus') {
    //       options = [
    //         { label: '锁定', value: 1 },
    //         { label: '未锁定', value: 0 }
    //       ]
    //     }
    //   }

    //   // 数值类字段使用数字输入框
    //   if (field.label.includes('(°)') || field.label.includes('精度')) {
    //     componentType = 'input-number'
    //   }

    return {
      ...field
    }

  })
})

// 过滤掉操作列和隐藏字段
// const filteredFields = computed(() => {
//   return enhancedTableConfig.value.filter(field => 
//     !field.customColumn && !field.hidden 
//   )
// })
// 过滤掉操作列、隐藏字段，并根据模式处理disabled字段
const filteredFields = computed(() => {
  return enhancedTableConfig.value.filter(field => {
    if (field.customColumn) return false;

    if (field.hidden) return false;
    console.log("field.disabled", props.mode)
    // 处理disabled字段
    // if (props.mode === 'add' && field.disabled === true) {
    //   return false; 
    // }

    return true;
  });
});

// 监听表单数据变化
watch(() => props.modelValue, (newVal) => {
  formModel.value = { ...newVal }
}, { immediate: true, deep: true })


const handleInput = (key, value) => {
  formModel.value = { ...formModel.value, [key]: value }
  emit('update:modelValue', formModel.value)
  emit('change', formModel.value)
}
// 监听内部表单数据变化
// watch(formModel, (newVal) => {
//   emit('update:modelValue', newVal)
//   emit('change', newVal)
// }, { deep: true })

// 获取字段对应的组件
const getFieldComponent = (field) => {
  if (field.component === 'select') return 'el-select'
  if (field.component === 'input-number') return 'el-input-number'
  if (field.component === 'textarea') return 'el-input'
  return 'el-input'
}

// 获取字段属性
const getFieldProps = (field) => {
  const props = {}

  if (field.component === 'input-number') {
    props.min = field.min ?? 0
    props.max = field.max
    props.precision = field.precision ?? 2
    props.step = field.step ?? 0.1
    props.controls = field.controls !== false
  }

  if (field.component === 'select' && field.multiple) {
    props.multiple = true
  }

  return props
}

// 获取字段选项
const getFieldOptions = (field) => {
  return field.options || []
}

// 获取占位符
const getPlaceholder = (field) => {
  if (field.placeholder) return field.placeholder

  if (field.component === 'select') {
    return `请选择${field.label}`
  }
  return `请输入${field.label}`
}

// 判断字段是否禁用
const isFieldDisabled = (field) => {
  if (props.mode === 'edit' && field.disabled === true) {
    return true;
  }
  return props.disabled;
};
// 暴露方法
defineExpose({
  getFormData: () => formModel.value,
  setFormData: (data) => {
    formModel.value = { ...data }
  },
  resetFields: () => {
    formModel.value = { ...props.formConfig.initData }
  }
})
</script>

<style scoped>
.inline-form :deep(.el-form-item) {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}

.inline-form :deep(.el-form-item__label) {
  text-align: right;
  padding-right: 12px;
  flex-shrink: 0;
}

.inline-form :deep(.el-form-item__content) {
  flex: 1;
  margin-left: 0 !important;
}

.form-field-input {
  width: v-bind(fieldWidth);
}
</style>