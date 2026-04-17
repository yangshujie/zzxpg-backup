<template>
  <el-dialog 
    :title="formMode === 'add' ? '新增载荷' : '修改载荷'" 
    v-model="dialogVisible" 
    width="800px" 
    append-to-body
    :before-close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <!-- 公共字段 -->
      <el-form-item label="载荷名称" prop="payloadName">
        <el-input v-model="form.payloadName" placeholder="请输入载荷名称" />
      </el-form-item>
      
      <el-form-item label="载荷类型" prop="payloadType">
        <el-select v-model="form.payloadType" placeholder="请选择载荷类型" style="width: 100%" disabled>
          <el-option :label="getPayloadTypeName(payloadType)" :value="payloadType" />
        </el-select>
      </el-form-item>

      <!-- 光学和SAR载荷的公共字段 -->
      <template v-if="payloadType === 0 || payloadType === 1">
        <el-form-item label="侧摆角方向" prop="rollAngleDirection">
          <el-select v-model="form.rollAngleDirection" placeholder="请选择侧摆角方向" style="width: 100%">
            <el-option label="正向" :value="0" />
            <el-option label="反向" :value="1" />
          </el-select>
        </el-form-item>

        <!-- 新增字段 - 两行两列布局 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最大开机时间" prop="maxPowerOnTime">
              <el-input-number 
                v-model="form.maxPowerOnTime" 
                placeholder="请输入最大开机时间" 
                :min="0" 
                :precision="1"
                style="width: 100%"
              />
              <span style="margin-left: 8px; color: #a0a0a0;">秒</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最小观测时长" prop="minObservationDuration">
              <el-input-number 
                v-model="form.minObservationDuration" 
                placeholder="请输入最小观测时长" 
                :min="0" 
                :precision="1"
                style="width: 100%"
              />
              <span style="margin-left: 8px; color: #a0a0a0;">秒</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最小任务间隔" prop="minTaskInterval">
              <el-input-number 
                v-model="form.minTaskInterval" 
                placeholder="请输入最小任务间隔" 
                :min="0" 
                :precision="1"
                style="width: 100%"
              />
              <span style="margin-left: 8px; color: #a0a0a0;">秒</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <!-- 光学载荷特有字段 -->
            <el-form-item v-if="payloadType === 0" label="最小太阳高度角" prop="minSolarElevation">
              <el-input-number 
                v-model="form.minSolarElevation" 
                placeholder="请输入最小太阳高度角" 
                :min="0" 
                :precision="1"
                style="width: 100%"
              />
              <span style="margin-left: 8px; color: #a0a0a0;">°</span>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 侧摆角范围 -->
        <el-form-item label="侧摆角范围" prop="roll">
          <div class="array-container">
            <table class="array-table roll-range-table">
              <thead>
                <tr>
                  <th>范围</th>
                  <th>左范围</th>
                  <th></th>
                  <th>右范围</th>
                  <th class="action-cell">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in form.roll" :key="index">
                  <td>范围 {{ index + 1 }}</td>
                  <td>
                    <el-form-item :prop="`roll[${index}].lrange`" :rules="rollRangeRules">
                      <el-input-number 
                        v-model="item.lrange" 
                        placeholder="左范围" 
                        :min="-180" 
                        :max="180" 
                        :precision="1"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </td>
                  <td class="range-separator">~</td>
                  <td>
                    <el-form-item :prop="`roll[${index}].rrange`" :rules="rollRangeRules">
                      <el-input-number 
                        v-model="item.rrange" 
                        placeholder="右范围" 
                        :min="-180" 
                        :max="180" 
                        :precision="1"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </td>
                  <td class="action-cell">
                    <el-button 
                      v-if="form.roll.length > 1" 
                      type="danger" 
                      size="small" 
                      icon="Delete" 
                      @click="removeRollItem(index)"
                    >删除</el-button>
                  </td>
                </tr>
              </tbody>
            </table>
            <el-button type="primary" icon="Plus" @click="addRollItem" class="add-button">添加范围</el-button>
          </div>
        </el-form-item>

        <!-- 几何参数 -->
        <el-form-item label="几何参数" prop="geometry">
          <div class="array-container">
            <div class="geometry-table">
              <!-- 表头 -->
              <div class="geometry-row geometry-header">
                <div class="geometry-field">
                  <label>分辨率</label>
                </div>
                <div class="geometry-field">
                  <label>宽度</label>
                </div>
                <div class="geometry-field">
                  <label>半视场角</label>
                </div>
                <div class="geometry-field">
                  <label>工作模式</label>
                </div>
                <div class="geometry-field">
                  <label>操作</label>
                </div>
              </div>
              
              <!-- 数据行 -->
              <div v-for="(item, index) in form.geometry" :key="index" class="geometry-row">
                <div class="geometry-field">
                  <el-form-item :prop="`geometry[${index}].resolution`" :rules="geometryRules.resolution">
                    <el-input-number 
                      v-model="item.resolution" 
                      placeholder="分辨率" 
                      :min="0" 
                      :precision="1"
                      style="width: 100%"
                    />
                  </el-form-item>
                </div>
                <div class="geometry-field">
                  <el-form-item :prop="`geometry[${index}].width`" :rules="geometryRules.width">
                    <el-input-number 
                      v-model="item.width" 
                      placeholder="宽度" 
                      :min="0" 
                      :precision="1"
                      style="width: 100%"
                    />
                  </el-form-item>
                </div>
                <div class="geometry-field">
                  <el-form-item :prop="`geometry[${index}].halfFov`" :rules="geometryRules.halfFov">
                    <el-input-number 
                      v-model="item.halfFov" 
                      placeholder="半视场角" 
                      :min="0" 
                      :precision="1"
                      style="width: 100%"
                    />
                  </el-form-item>
                </div>
                <div class="geometry-field">
                  <el-form-item :prop="`geometry[${index}].workMode`" :rules="geometryRules.workMode">
                    <el-select v-model="item.workMode" placeholder="工作模式" style="width: 100%">
                      <el-option label="模式1" :value="1" />
                      <el-option label="模式2" :value="2" />
                    </el-select>
                  </el-form-item>
                </div>
                <div class="geometry-field">
                  <el-button 
                    v-if="form.geometry.length > 1" 
                    type="danger" 
                    size="small" 
                    icon="Delete" 
                    @click="removeGeometryItem(index)"
                  >删除</el-button>
                </div>
              </div>
            </div>
            <el-button type="primary" icon="Plus" @click="addGeometryItem" class="add-button">添加参数组</el-button>
          </div>
        </el-form-item>
      </template>

      <!-- 电子侦察载荷特有字段 -->
      <template v-if="payloadType === 2">
        <el-form-item label="半视场角" prop="capabilityInfos.halfFov">
          <el-input-number 
            v-model="form.capabilityInfos.halfFov" 
            placeholder="请输入半视场角" 
            :min="0" 
            :precision="1"
            style="width: 100%"
          />
        </el-form-item>

        <!-- 频率检测范围 -->
        <el-form-item label="频率检测范围" prop="capabilityInfos.frequencyDetected">
          <div class="array-container">
            <div v-for="(item, index) in form.capabilityInfos.frequencyDetected" :key="index" class="array-item horizontal-item">
              <div class="item-header">
                <span>频率范围 {{ index + 1 }}</span>
                <el-button type="danger" size="small" icon="Delete" @click="removeFrequencyItem(index)" v-if="form.capabilityInfos.frequencyDetected.length > 1">删除</el-button>
              </div>
              <div class="item-content horizontal-content">
                <el-form-item :prop="`capabilityInfos.frequencyDetected[${index}].frequencyMin`" :rules="frequencyRules" style="display: inline-block; width: 45%; margin-right: 10px;">
                  <el-input-number 
                    v-model="item.frequencyMin" 
                    placeholder="最小频率" 
                    :min="0" 
                    :precision="1"
                    style="width: 100%"
                  />
                </el-form-item>
                <span class="range-separator" style="display: inline-block; margin: 0 5px;">~</span>
                <el-form-item :prop="`capabilityInfos.frequencyDetected[${index}].frequencyMax`" :rules="frequencyRules" style="display: inline-block; width: 45%;">
                  <el-input-number 
                    v-model="item.frequencyMax" 
                    placeholder="最大频率" 
                    :min="0" 
                    :precision="1"
                    style="width: 100%"
                  />
                </el-form-item>
              </div>
            </div>
            <el-button type="primary" icon="Plus" @click="addFrequencyItem">添加频率范围</el-button>
          </div>
        </el-form-item>
      </template>

      <!-- 卫星中继载荷特有字段 -->
      <template v-if="payloadType === 3">
        <!-- 频率检测范围 -->
        <el-form-item label="频率检测范围" prop="capabilityInfos.frequencyDetected">
          <div class="array-container">
            <div v-for="(item, index) in form.capabilityInfos.frequencyDetected" :key="index" class="array-item horizontal-item">
              <div class="item-header">
                <span>频率范围 {{ index + 1 }}</span>
                <el-button type="danger" size="small" icon="Delete" @click="removeFrequencyItem(index)" v-if="form.capabilityInfos.frequencyDetected.length > 1">删除</el-button>
              </div>
              <div class="item-content horizontal-content">
                <el-form-item :prop="`capabilityInfos.frequencyDetected[${index}].frequencyMin`" :rules="frequencyRules" style="display: inline-block; width: 45%; margin-right: 10px;">
                  <el-input-number 
                    v-model="item.frequencyMin" 
                    placeholder="最小频率" 
                    :min="0" 
                    :precision="1"
                    style="width: 100%"
                  />
                </el-form-item>
                <span class="range-separator" style="display: inline-block; margin: 0 5px;">~</span>
                <el-form-item :prop="`capabilityInfos.frequencyDetected[${index}].frequencyMax`" :rules="frequencyRules" style="display: inline-block; width: 45%;">
                  <el-input-number 
                    v-model="item.frequencyMax" 
                    placeholder="最大频率" 
                    :min="0" 
                    :precision="1"
                    style="width: 100%"
                  />
                </el-form-item>
              </div>
            </div>
            <el-button type="primary" icon="Plus" @click="addFrequencyItem">添加频率范围</el-button>
          </div>
        </el-form-item>

        <!-- 传输速率 -->
        <el-form-item label="传输速率" prop="capabilityInfos.rate">
          <el-input-number 
            v-model="form.capabilityInfos.rate" 
            placeholder="请输入传输速率" 
            :min="0" 
            :precision="1"
            style="width: 100%"
          />
          <span style="margin-left: 8px; color: #a0a0a0;">Mbps</span>
        </el-form-item>
      </template>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="PayloadFormDialog">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  insertSatPayloadOptical, updateSatPayloadOptical,
  insertSatPayloadSAR, updateSatPayloadSAR,
  insertSatPayloadElectronic, updateSatPayloadElectronic,
  insertSatPayloadRelay, updateSatPayloadRelay
} from '@/api/systemPlus/systemCooperation/payload'

// 组件属性
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  payloadType: {
    type: Number,
    default: 0
  },
  formData: {
    type: Object,
    default: () => ({})
  },
  mode: {
    type: String,
    default: 'add'
  }
})

// 组件事件
const emit = defineEmits(['update:modelValue', 'success'])

// 响应式数据
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  payloadName: '',
  payloadType: 0,
  rollAngleDirection: 0,
  maxPowerOnTime: 0,
  minObservationDuration: 0,
  minTaskInterval: 0,
  minSolarElevation: 0,
  roll: [{ lrange: 0, rrange: 0 }],
  geometry: [{ resolution: 0, width: 0, halfFov: 0, workMode: 1 }],
  capabilityInfos: {
    halfFov: 0,
    rate: 0,
    frequencyDetected: [{ frequencyMin: 0, frequencyMax: 0 }]
  }
})

// 验证规则
const rules = {
  payloadName: [{ required: true, message: '请输入载荷名称', trigger: 'blur' }],
  maxPowerOnTime: [{ required: true, message: '请输入最大开机时间', trigger: 'blur' }],
  minObservationDuration: [{ required: true, message: '请输入最小观测时长', trigger: 'blur' }],
  minTaskInterval: [{ required: true, message: '请输入最小任务间隔', trigger: 'blur' }],
  minSolarElevation: [{ required: true, message: '请输入最小太阳高度角', trigger: 'blur' }]
}

const rollRangeRules = [
  { required: true, message: '请输入范围值', trigger: 'blur' }
]

const geometryRules = {
  resolution: [{ required: true, message: '请输入分辨率', trigger: 'blur' }],
  width: [{ required: true, message: '请输入宽度', trigger: 'blur' }],
  halfFov: [{ required: true, message: '请输入半视场角', trigger: 'blur' }],
  workMode: [{ required: true, message: '请选择工作模式', trigger: 'change' }]
}

const frequencyRules = [
  { required: true, message: '请输入频率值', trigger: 'blur' }
]

// 载荷类型映射
const payloadTypeMap = {
  0: '卫星光学载荷',
  1: '卫星SAR载荷', 
  2: '卫星电子侦察载荷',
  3: '卫星中继载荷'
}

// 监听器
watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
  if (val) {
    // 初始化表单数据
    initFormData()
  }
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

// 方法
const getPayloadTypeName = (type) => payloadTypeMap[type] || '未知类型'

const initFormData = () => {
  // 重置表单
  Object.assign(form, {
    payloadName: '',
    payloadType: props.payloadType,
    rollAngleDirection: 0,
    maxPowerOnTime: 0,
    minObservationDuration: 0,
    minTaskInterval: 0,
    minSolarElevation: 0,
    roll: [{ lrange: 0, rrange: 0 }],
    geometry: [{ resolution: 0, width: 0, halfFov: 0, workMode: 1 }],
    capabilityInfos: {
      halfFov: 0,
      rate: 0,
      frequencyDetected: [{ frequencyMin: 0, frequencyMax: 0 }]
    }
  })

  // 如果有传入的数据，则填充表单
  if (props.formData && Object.keys(props.formData).length > 0) {
    Object.assign(form, JSON.parse(JSON.stringify(props.formData)))
  }
}

// 侧摆角范围操作
const addRollItem = () => {
  form.roll.push({ lrange: 0, rrange: 0 })
}

const removeRollItem = (index) => {
  form.roll.splice(index, 1)
}

// 几何参数操作
const addGeometryItem = () => {
  form.geometry.push({ resolution: 0, width: 0, halfFov: 0, workMode: 1 })
}

const removeGeometryItem = (index) => {
  form.geometry.splice(index, 1)
}

// 频率检测范围操作
const addFrequencyItem = () => {
  form.capabilityInfos.frequencyDetected.push({ frequencyMin: 0, frequencyMax: 0 })
}

const removeFrequencyItem = (index) => {
  form.capabilityInfos.frequencyDetected.splice(index, 1)
}

const handleClose = () => {
  dialogVisible.value = false
}

const submitForm = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    
    // 根据载荷类型调用不同的API
    let result
    const submitData = JSON.parse(JSON.stringify(form))
    
    switch (props.payloadType) {
      case 0: // 光学载荷
        if (props.mode === 'add') {
          result = await insertSatPayloadOptical(submitData)
        } else {
          result = await updateSatPayloadOptical(submitData)
        }
        break
      case 1: // SAR载荷
        if (props.mode === 'add') {
          result = await insertSatPayloadSAR(submitData)
        } else {
          result = await updateSatPayloadSAR(submitData)
        }
        break
      case 2: // 电子侦察载荷
        if (props.mode === 'add') {
          result = await insertSatPayloadElectronic(submitData)
        } else {
          result = await updateSatPayloadElectronic(submitData)
        }
        break
      case 3: // 中继载荷
        if (props.mode === 'add') {
          result = await insertSatPayloadRelay(submitData)
        } else {
          result = await updateSatPayloadRelay(submitData)
        }
        break
    }

    ElMessage.success(props.mode === 'add' ? '新增成功' : '修改成功')
    emit('success')
    handleClose()
  } catch (error) {
    console.error('提交表单失败:', error)
    ElMessage.error('操作失败，请重试')
  }
}
</script>

<style lang="scss" scoped>
.payload-form-dialog {
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

  :deep(.el-form-item__label) {
    color: #a0a0a0;
    font-weight: 500;
  }

  :deep(.el-input__wrapper) {
    background: rgba(0, 0, 0, 0.3);
    border: 1px solid rgba(64, 158, 255, 0.2);
    border-radius: 6px;
    box-shadow: none;
    transition: all 0.3s ease;
    
    &:hover {
      border-color: rgba(64, 158, 255, 0.5);
    }
    
    &.is-focus {
      border-color: #409eff;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
    }
    
    .el-input__inner {
      color: #e4e7ed;
      background: transparent;
      
      &::placeholder {
        color: rgba(255, 255, 255, 0.4);
      }
    }
  }

  :deep(.el-select) {
    .el-input__wrapper {
      background: rgba(0, 0, 0, 0.3);
    }
    
    .el-select__caret {
      color: rgba(255, 255, 255, 0.6);
    }
  }

  :deep(.el-input-number) {
    width: 100%;
    
    .el-input-number__decrease,
    .el-input-number__increase {
      background: rgba(64, 158, 255, 0.1);
      border-color: rgba(64, 158, 255, 0.3);
      color: #e4e7ed;
      
      &:hover {
        background: rgba(64, 158, 255, 0.2);
        color: #409eff;
      }
      
      &.is-disabled {
        background: rgba(255, 255, 255, 0.1);
        color: rgba(255, 255, 255, 0.3);
      }
    }
  }

  :deep(.el-option) {
    background: rgba(0, 0, 0, 0.8);
    color: #e4e7ed;
    
    &:hover {
      background: rgba(64, 158, 255, 0.2);
    }
    
    &.selected {
      background: rgba(64, 158, 255, 0.3);
      color: #409eff;
    }
  }
}

.array-container {
  margin-top: 16px;
  
  .array-table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 16px;
    
    th, td {
      padding: 12px 8px;
      text-align: center;
      border: 1px solid rgba(64, 158, 255, 0.2);
    }
    
    th {
      background: rgba(64, 158, 255, 0.1);
      color: #e4e7ed;
      font-weight: 500;
    }
    
    td {
      background: rgba(0, 0, 0, 0.2);
      color: #e4e7ed;
    }
    
    .action-cell {
      width: 100px;
    }
  }
  
  .add-button {
    margin-top: 8px;
  }
}

.roll-range-table {
  .range-cell {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .range-separator {
      color: rgba(255, 255, 255, 0.6);
      font-weight: bold;
    }
  }
}

.geometry-table {
  .geometry-row {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr 1fr 80px;
    gap: 8px;
    align-items: center;
    padding: 8px;
    background: rgba(0, 0, 0, 0.2);
    border: 1px solid rgba(64, 158, 255, 0.2);
    border-radius: 4px;
    margin-bottom: 8px;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
  
  .geometry-header {
    background: rgba(64, 158, 255, 0.1);
    font-weight: 500;
    color: #e4e7ed;
  }
  
  .geometry-field {
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    label {
      font-size: 12px;
      color: rgba(255, 255, 255, 0.6);
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

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-form-item__content) {
  display: flex;
  align-items: center;
}
</style>