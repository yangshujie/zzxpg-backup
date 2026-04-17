<template>
  <div class="zhpg-calc-method-fields">
    <div v-if="showWorkModeSelector" class="calc-mode-row">
      <span class="calc-mode-label">工作模式</span>
      <el-radio-group :model-value="workMode" @update:model-value="v => emit('update:workMode', v)">
        <el-radio-button v-for="item in workModeOptions" :key="item.value" :value="item.value">
          {{ item.label }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <div class="calc-row calc-row--button-only">
      <el-button type="primary" class="calc-config-btn" @click="algorithmVisible = true">配置算法</el-button>
    </div>

    <ZhpgCalcAlgorithmDialog
      v-model:visible="algorithmVisible"
      :model-value="modelValue"
      :leaf-uid="leafUid"
      :work-mode="workMode"
      :show-work-mode-selector="showWorkModeSelector"
      :default-source-center="defaultSourceCenter"
      only-config-params-in-drawer
      @update:model-value="v => emit('update:modelValue', v)"
      @update:work-mode="v => emit('update:workMode', v)"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import ZhpgCalcAlgorithmDialog from '@/views/zhpg/components/ZhpgCalcAlgorithmDialog.vue'
import { parseLeafCalcToWorkspace, calcMethodIsNonJsonText } from '@/utils/zhpg/calcMethodAlgorithm'
import { ZHPG_WORK_MODE, ZHPG_WORK_MODE_OPTIONS } from '@/constants/zhpgWorkMode'

const props = defineProps({
  modelValue: { type: [String, Object], default: '' },
  leafUid: { type: String, default: '' },
  workMode: { type: String, default: ZHPG_WORK_MODE.INTERNAL_CIRCULATION },
  showWorkModeSelector: { type: Boolean, default: true },
  /** 新建数据源时默认分中心（与装备类型枚举一致，不含「无」） */
  defaultSourceCenter: { type: String, default: '' },
  placeholderText: {
    type: String,
    default: '点击“配置算法”进入画布搭建算法链'
  }
})

const emit = defineEmits(['update:modelValue', 'update:workMode'])

const algorithmVisible = ref(false)
const workModeOptions = ZHPG_WORK_MODE_OPTIONS

const parsedWorkspace = computed(() =>
  parseLeafCalcToWorkspace(props.modelValue || '', {
    defaultWorkMode: props.workMode || ZHPG_WORK_MODE.INTERNAL_CIRCULATION
  })
)

const calcConfigured = computed(() => {
  const ws = parsedWorkspace.value
  return ws.dataSources.length > 0 || ws.algorithmSteps.length > 0 || calcMethodIsNonJsonText(props.modelValue)
})

const calcStatusText = computed(() => {
  if (calcMethodIsNonJsonText(props.modelValue)) return '已存在文本说明，点击按钮可改为图形化配置。'
  if (!calcConfigured.value) return props.placeholderText
  const ws = parsedWorkspace.value
  const edgeCount = Array.isArray(ws.flowEdges) ? ws.flowEdges.length : 0
  return `已配置 ${ws.dataSources.length} 个数据源、${ws.algorithmSteps.length} 个算法盒、${edgeCount} 条连线。`
})
</script>

<style scoped>
.zhpg-calc-method-fields {
  width: 100%;
  min-width: 0;
}
.calc-mode-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.calc-mode-label {
  font-size: 15px;
  font-weight: 600;
  color: #606266;
}
.calc-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.calc-row--button-only {
  justify-content: center;
  flex-wrap: wrap;
  padding: 10px 12px;
}
.calc-config-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 240px;
}
.calc-config-btn {
  min-width: 96px;
}
.calc-mode-tip {
  font-size: 14px;
  color: #909399;
  line-height: 1.5;
}

@media (max-width: 560px) {
  .calc-config-summary {
    min-width: 100%;
  }
}
</style>
