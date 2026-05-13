<template>
  <el-dialog :title="title" v-model="dialogVisible" width="1400px" append-to-body :before-close="handleClose">
    <div class="payload-selector-container">
      <!-- 载荷类型选择 -->
      <div class="payload-type-selector">
        <el-radio-group v-model="currentPayloadType" @change="handlePayloadTypeChange">
          <el-radio :label="0">卫星光学载荷</el-radio>
          <el-radio :label="1">卫星SAR载荷</el-radio>
          <el-radio :label="2">卫星电子侦察载荷</el-radio>
          <el-radio :label="3">卫星中继载荷</el-radio>
        </el-radio-group>
      </div>

      <!-- 载荷管理区域 -->
      <div class="payload-management">
        <!-- 查询条件 -->
        <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="80px">
          <el-form-item label="载荷名称" prop="payloadName">
            <el-input v-model="queryParams.payloadName" placeholder="请输入载荷名称" clearable style="width: 200px"
              @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            <el-button type="primary" icon="Plus" @click="handleAdd">新增</el-button>
          </el-form-item>
        </el-form>

        <!-- 载荷表格 -->
        <DynamicTable :table-data="payloadList" :field-config="currentFieldConfig" :show-checkbox="true"
          :show-index="true" :loading="loading" @selection-change="handleSelectionChange">
          <template #operationSlot="{ row }">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">修改</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </DynamicTable>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize" @pagination="getList" />
      </div>

      <!-- 已选载荷展示区域 -->
      <div class="selected-payloads">
        <div class="selected-header">
          <span class="selected-title">已选载荷</span>
          <el-button type="danger" size="small" @click="clearSelected">清空</el-button>
        </div>
        <div class="selected-content">
          <div v-for="(payloads, type) in selectedPayloads" :key="type" class="payload-type-group">
            <div class="type-title">{{ getPayloadTypeName(parseInt(type)) }}</div>
            <div class="payload-items">
              <el-tag v-for="payload in payloads" :key="payload.payloadId" closable
                @close="removeSelected(payload.payloadId, parseInt(type))" class="payload-tag">
                {{ payload.payloadName }}
              </el-tag>
            </div>
          </div>
          <div v-if="Object.keys(selectedPayloads).length === 0" class="empty-tip">
            暂无选中载荷
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确认选择</el-button>
      </div>
    </template>

    <!-- 新增/修改载荷对话框 -->
    <PayloadFormDialog v-model="formDialogVisible" :payload-type="currentPayloadType" :form-data="formData"
      :mode="formMode" @success="handleFormSuccess" />
  </el-dialog>
</template>

<script setup name="PayloadSelector">
import { ref, reactive, computed, watch } from 'vue'
import DynamicTable from '@/components/DynamicTable/index.vue'
import Pagination from '@/components/Pagination/index.vue'
import PayloadFormDialog from './PayloadFormDialog.vue'
import {
  listSatPayloadOptical, getSatPayloadOptical, deleteSatPayloadOptical,
  listSatPayloadSAR, getSatPayloadSAR, deleteSatPayloadSAR,
  listSatPayloadElectronic, getSatPayloadElectronic, deleteSatPayloadElectronic,
  listSatPayloadRelay, getSatPayloadRelay, deleteSatPayloadRelay
} from '@/api/systemPlus/systemCooperation/payload'

// 组件属性
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '载荷选择'
  },
  selectedPayloads: {
    type: Object,
    default: () => ({})
  }
})

// 组件事件
const emit = defineEmits(['update:modelValue', 'confirm'])

// 响应式数据
const dialogVisible = ref(false)
const currentPayloadType = ref(0) // 0:光学, 1:SAR, 2:电子侦察
const loading = ref(true)
const total = ref(0)
const payloadList = ref([])
const selectedIds = ref([])
const formDialogVisible = ref(false)
const formData = ref({})
const formMode = ref('add')

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  payloadName: undefined
})

// 已选载荷数据
const selectedPayloads = ref({})

// 载荷类型映射
const payloadTypeMap = {
  0: '卫星光学载荷',
  1: '卫星SAR载荷',
  2: '卫星电子侦察载荷',
  3: '卫星中继载荷'
}

// 字段配置
const fieldConfigs = {
  0: [
    { key: 'payloadName', label: '载荷名称', width: 180, showOverflowTooltip: true },
    { key: 'payloadType', label: '载荷类型', width: 120, formatter: () => '光学载荷' },
    { key: 'rollAngleDirection', label: '侧摆角方向', width: 120, formatter: (row) => row.rollAngleDirection === 0 ? '正向' : '反向' },
    {
      key: 'maxPowerOnTime',
      label: '最大开机时间',
      width: 120,
      formatter: (value) => {
        if (value !== undefined && value !== null && value !== '' || value === 0) {
          return `${value}秒`;
        }
        return '-';
      }
    },
    {
      key: 'minObservationDuration',
      label: '最小观测时长',
      width: 120,
      formatter: (value) => {
        if (value !== undefined && value !== null && value !== '' || value === 0) {
          return `${value}秒`;
        }
        return '-';
      }
    },
    {
      key: 'minTaskInterval',
      label: '最小任务间隔',
      width: 120,
      formatter: (value) => {
        if (value !== undefined && value !== null && value !== '' || value === 0) {
          return `${value}秒`;
        }
        return '-';
      }
    },
    {
      key: 'minSolarElevation',
      label: '最小太阳高度角',
      width: 120,
      formatter: (value) => {
        if (value !== undefined && value !== null && value !== '' || value === 0) {
          return `${value}°`;
        }
        return '-';
      }
    },
    { key: 'roll', label: '侧摆角范围', width: 200, showOverflowTooltip: true },
    { key: 'geometry', label: '几何参数', width: 200, showOverflowTooltip: true },
    {
      key: 'operation',
      label: '操作',
      width: 200,
      customColumn: true,
      slotName: 'operationSlot'
    }
  ],
  1: [
    { key: 'payloadName', label: '载荷名称', width: 180, showOverflowTooltip: true },
    { key: 'payloadType', label: '载荷类型', width: 120, formatter: () => 'SAR载荷' },
    { key: 'rollAngleDirection', label: '侧摆角方向', width: 120, formatter: (row) => row && row.rollAngleDirection === 0 ? '正向' : '反向' },
    {
      key: 'maxPowerOnTime',
      label: '最大开机时间',
      width: 120,
      formatter: (value) => {
        if (value !== undefined && value !== null && value !== '' || value === 0) {
          return `${value}秒`;
        }
        return '-';
      }
    },
    {
      key: 'minObservationDuration',
      label: '最小观测时长',
      width: 120,
      formatter: (value) => {
        if (value !== undefined && value !== null && value !== '' || value === 0) {
          return `${value}秒`;
        }
        return '-';
      }
    },
    {
      key: 'minTaskInterval',
      label: '最小任务间隔',
      width: 120,
      formatter: (value) => {
        if (value !== undefined && value !== null && value !== '' || value === 0) {
          return `${value}秒`;
        }
        return '-';
      }
    },
    { key: 'roll', label: '侧摆角范围', width: 200, showOverflowTooltip: true },
    { key: 'geometry', label: '几何参数', width: 200, showOverflowTooltip: true },
    {
      key: 'operation',
      label: '操作',
      width: 200,
      customColumn: true,
      slotName: 'operationSlot'
    }
  ],
  2: [
    { key: 'payloadName', label: '载荷名称', width: 180, showOverflowTooltip: true },
    { key: 'payloadType', label: '载荷类型', width: 120, formatter: () => '电子侦察载荷' },
    {
      key: 'capabilityInfos.halfFov',
      label: '半视场角',
      width: 120,
      formatter: (row) => row?.capabilityInfos?.halfFov ? `${row.capabilityInfos.halfFov}°` : '-'
    },
    { key: 'capabilityInfos.frequencyDetected', label: '频率检测范围', width: 200, showOverflowTooltip: true, formatter: (row) => { const freqRanges = row?.capabilityInfos?.frequencyDetected || []; if (freqRanges.length === 0) return '-'; return freqRanges.map(freq => `${freq.frequencyMin}-${freq.frequencyMax}GHz`).join('; '); } },
    {
      key: 'operation',
      label: '操作',
      width: 200,
      customColumn: true,
      slotName: 'operationSlot'
    }
  ],
  3: [
    { key: 'payloadName', label: '载荷名称', width: 180, showOverflowTooltip: true },
    { key: 'payloadType', label: '载荷类型', width: 120, formatter: () => '中继载荷' },
    {
      key: 'capabilityInfos.rate',
      label: '传输速率',
      width: 120,
      formatter: (row) => row?.capabilityInfos?.rate ? `${row.capabilityInfos.rate}Mbps` : '-'
    },
    { key: 'capabilityInfos.frequencyDetected', label: '频率检测范围', width: 200, showOverflowTooltip: true, formatter: (row) => { const freqRanges = row?.capabilityInfos?.frequencyDetected || []; if (freqRanges.length === 0) return '-'; return freqRanges.map(freq => `${freq.frequencyMin}-${freq.frequencyMax}GHz`).join('; '); } },
    {
      key: 'operation',
      label: '操作',
      width: 200,
      customColumn: true,
      slotName: 'operationSlot'
    }
  ]
}

// 计算属性
const currentFieldConfig = computed(() => fieldConfigs[currentPayloadType.value])

// 监听器
watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
  if (val) {
    // 初始化已选载荷
    selectedPayloads.value = { ...props.selectedPayloads }
    getList()
  }
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

// 方法
const getPayloadTypeName = (type) => payloadTypeMap[type] || '未知类型'

const handlePayloadTypeChange = () => {
  queryParams.pageNum = 1
  getList()
}

const getList = async () => {
  loading.value = true
  try {
    let response
    switch (currentPayloadType.value) {
      case 0:
        response = await listSatPayloadOptical(queryParams)
        break
      case 1:
        response = await listSatPayloadSAR(queryParams)
        break
      case 2:
        response = await listSatPayloadElectronic(queryParams)
        break
      case 3:
        response = await listSatPayloadRelay(queryParams)
        break
    }

    // 检查组件是否已卸载
    if (!dialogVisible.value) {
      return
    }

    payloadList.value = response.rows || []
    console.log("payloadList.value", payloadList.value)
    total.value = response.total || 0

    // 设置选中状态
    updateSelection()
  } catch (error) {
    // 检查组件是否已卸载
    if (!dialogVisible.value) {
      return
    }
    console.error('获取载荷列表失败:', error)
    payloadList.value = []
    total.value = 0
  } finally {
    // 检查组件是否已卸载
    if (dialogVisible.value) {
      loading.value = false
    }
  }
}

const updateSelection = () => {
  const currentTypeSelected = selectedPayloads.value[currentPayloadType.value] || []
  const selectedIds = currentTypeSelected.map(item => item.payloadId)

  // 这里需要根据实际表格组件的API来设置选中状态
  // 假设DynamicTable有setSelection方法
  if (window.$tableRef && window.$tableRef.setSelection) {
    window.$tableRef.setSelection(selectedIds)
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const resetQuery = () => {
  queryParams.payloadName = undefined
  handleQuery()
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.payloadId)

  // 更新已选载荷
  const currentType = currentPayloadType.value
  if (!selectedPayloads.value[currentType]) {
    selectedPayloads.value[currentType] = []
  }

  // 移除当前类型的所有选中，然后添加新的选中
  selectedPayloads.value[currentType] = selection.map(item => ({
    payloadId: item.payloadId,
    payloadName: item.payloadName,
    payloadType: currentType
  }))
}

const handleAdd = () => {
  formData.value = {}
  formMode.value = 'add'
  formDialogVisible.value = true
}

const handleUpdate = async (row) => {
  try {
    let response
    switch (currentPayloadType.value) {
      case 0:
        response = await getSatPayloadOptical(row.payloadId)
        break
      case 1:
        response = await getSatPayloadSAR(row.payloadId)
        break
      case 2:
        response = await getSatPayloadElectronic(row.payloadId)
        break
      case 3:
        response = await getSatPayloadRelay(row.payloadId)
        break
    }

    // 检查组件是否已卸载
    if (!dialogVisible.value) {
      return
    }

    formData.value = response.data
    formMode.value = 'edit'
    formDialogVisible.value = true
  } catch (error) {
    // 检查组件是否已卸载
    if (!dialogVisible.value) {
      return
    }
    console.error('获取载荷详情失败:', error)
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('是否确认删除载荷"' + row.payloadName + '"?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      switch (currentPayloadType.value) {
        case 0:
          await deleteSatPayloadOptical(row.payloadId)
          break
        case 1:
          await deleteSatPayloadSAR(row.payloadId)
          break
        case 2:
          await deleteSatPayloadElectronic(row.payloadId)
          break
        case 3:
          await deleteSatPayloadRelay(row.payloadId)
          break
      }
      ElMessage.success('删除成功')
      getList()
    } catch (error) {
      console.error('删除载荷失败:', error)
    }
  })
}

const removeSelected = (payloadId, type) => {
  if (selectedPayloads.value[type]) {
    selectedPayloads.value[type] = selectedPayloads.value[type].filter(
      item => item.payloadId !== payloadId
    )
    if (selectedPayloads.value[type].length === 0) {
      delete selectedPayloads.value[type]
    }
  }
}

const clearSelected = () => {
  selectedPayloads.value = {}
}

const handleFormSuccess = () => {
  formDialogVisible.value = false
  getList()
}

const handleClose = () => {
  dialogVisible.value = false
}

const handleConfirm = () => {
  emit('confirm', selectedPayloads.value)
  dialogVisible.value = false
}

// 暴露方法给父组件
const open = () => {
  dialogVisible.value = true
}

defineExpose({
  open
})
</script>

<style lang="scss" scoped>
.payload-selector-container {
  display: flex;
  flex-direction: column;
  height: 600px;
}

.payload-type-selector {
  margin-bottom: 20px;
  padding: 16px;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(64, 158, 255, 0.2);
  border-radius: 8px;
  backdrop-filter: blur(10px);

  :deep(.el-radio-group) {
    .el-radio {
      margin-right: 20px;

      .el-radio__label {
        color: #e4e7ed;
        font-weight: 500;
      }

      .el-radio__inner {
        border-color: rgba(64, 158, 255, 0.5);
        background: rgba(0, 0, 0, 0.3);

        &:hover {
          border-color: #409eff;
        }
      }

      &.is-checked {
        .el-radio__inner {
          background-color: #409eff;
          border-color: #409eff;
        }

        .el-radio__label {
          color: #409eff;
        }
      }
    }
  }
}

.payload-management {
  flex: 1;
  display: flex;
  flex-direction: column;

  :deep(.el-form-item__label) {
    color: #a0a0a0;
    font-weight: 500;
  }

  :deep(.el-input__wrapper) {
    background: rgba(0, 0, 0, 0.3);
    border: 1px solid rgba(64, 158, 255, 0.2);
    border-radius: 6px;
    box-shadow: none;

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
    }
  }
}

.selected-payloads {
  margin-top: 20px;
  border: 1px solid rgba(64, 158, 255, 0.2);
  border-radius: 8px;
  max-height: 200px;
  overflow-y: auto;
  background: rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
}

.selected-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(64, 158, 255, 0.1);
  border-bottom: 1px solid rgba(64, 158, 255, 0.2);
}

.selected-title {
  font-weight: bold;
  color: #e4e7ed;
  font-size: 14px;
}

.selected-content {
  padding: 16px;
}

.payload-type-group {
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.type-title {
  font-weight: bold;
  margin-bottom: 8px;
  color: #409eff;
  font-size: 13px;
}

.payload-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.payload-tag {
  margin: 0;
  background: rgba(64, 158, 255, 0.1);
  border-color: rgba(64, 158, 255, 0.3);
  color: #e4e7ed;

  &:hover {
    background: rgba(64, 158, 255, 0.2);
    border-color: #409eff;
  }

  .el-icon-close {
    color: rgba(255, 255, 255, 0.6);

    &:hover {
      color: #ff4d4f;
    }
  }
}

.empty-tip {
  text-align: center;
  color: rgba(255, 255, 255, 0.4);
  font-style: italic;
  padding: 20px;
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

/* 对话框样式 */
.payload-selector-dialog {
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
</style>