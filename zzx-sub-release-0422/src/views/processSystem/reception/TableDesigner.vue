<template>
  <div class="designer-container">
    <div class="designer-header">
      <div class="title">数据项建模设计器 <span class="v-tag">v3.0</span></div>
      <div class="actions">
        <el-button-group>
          <el-button icon="back" circle style="margin-left: 12px;" @click="router.back()"></el-button>
          <el-button icon="DocumentAdd" plain @click="saveDraft">保存</el-button>
          <!-- <el-button icon="Check" type="primary" @click="handleGenerate">生成并发布</el-button> -->
        </el-button-group>

      </div>
    </div>

    <div class="designer-body">
      <!-- Center: Editable Table -->
      <main class="designer-main">
        <div class="toolbar-header">
          <div class="header-left">
            <el-input v-model="formTitle" size="large" class="title-input-edit" placeholder="点击编辑表单标题" />
          </div>
          <div class="header-right">
            <el-button type="primary" @click="addRow" plain icon="Plus">新增数据项</el-button>
          </div>
        </div>

        <div class="table-scroller">
          <el-table :data="tableData" class="cyber-editable-table" border height="100%" style="min-width: 1200px;">
            <el-table-column label="数据项名称" prop="name">
              <template #default="{ row }">
                <el-input v-model="row.name" placeholder="请输入数据项名称" />
              </template>
            </el-table-column>
            <el-table-column label="关联指标" prop="indicator">
              <template #default="{ row }">
                <div class="indicator-bind-cell">
                  <el-input v-model="row.indicator" readonly placeholder="点击绑定" @click="openIndicatorLib(row)" />
                  <el-icon class="bind-icon" @click="openIndicatorLib(row)">
                    <Aim />
                  </el-icon>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="关联源表" prop="tableCode" width="150">
              <template #default="{ row }">
                <el-input v-model="row.tableCode" placeholder="表编码" />
              </template>
            </el-table-column>
            <el-table-column label="关联源字段" prop="fieldCode" width="150">
              <template #default="{ row }">
                <el-input v-model="row.fieldCode" placeholder="字段编码" />
              </template>
            </el-table-column>
            <el-table-column label="所属分中心" prop="dataSourceId">
              <template #default="{ row }">
                <el-input v-model="row.dataSourceId" placeholder="ID" />
              </template>
            </el-table-column>

            <el-table-column label="时间范围" prop="timeRange">
              <template #default="{ row }">
                <el-date-picker v-model="row.timeRange" type="datetimerange" range-separator="至" start-placeholder="开始"
                  end-placeholder="结束" size="small" />
              </template>
            </el-table-column>

            <el-table-column label="试验阶段" prop="taskStage">
            </el-table-column>
            <el-table-column label="所属试验" prop="experimentalTask">
              <template #default="{ row }">
                <el-input v-model="row.experimentalTask" placeholder="请输入所属试验" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center" fixed="right">
              <template #default="{ row }">

                <el-button icon="Delete" size="small" type="danger" link @click="deleteRow(row)" title="删除" />
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- AI Generation Overlay -->
        <div v-if="isGenerating" class="ai-generating-overlay">
          <div class="ai-content">
            <div class="ai-robot-wrapper">
              <MagicStick class="magic-icon-anim" />
            </div>
            <div class="ai-text">智能解析需求要素...</div>
            <div class="ai-subtext">正在从《{{ route.query.reqName }}》中智能提取数据项定义</div>
            <div class="ai-bar-loader"></div>
          </div>
        </div>
      </main>

      <!-- Right Panel: Info -->
      <aside class="side-panel right-panel">
        <el-tabs v-model="rightTab" class="designer-tabs">
          <el-tab-pane label="表单信息" name="info">
            <div class="info-form">
              <el-form :model="formInfo" label-position="top" size="small">
                <el-form-item label="采集模板标识">
                  <el-input v-model="formInfo.templateCode" placeholder="请输入模板编号" />
                </el-form-item>
                <el-form-item label="采集模板名称">
                  <el-input v-model="formInfo.templateName" placeholder="请输入模板名称" />
                </el-form-item>
                <el-form-item label="评估任务标识">
                  <el-input v-model="formInfo.taskCode" placeholder="请输入任务编号" />
                </el-form-item>
                <el-form-item label="评估任务名称">
                  <el-input v-model="formInfo.taskName" placeholder="请输入任务名称" />
                </el-form-item>
                <el-form-item label="装备代号">
                  <el-input v-model="formInfo.equipCode" placeholder="请输入装备代号" />
                </el-form-item>
                <el-form-item label="装备名称">
                  <el-input v-model="formInfo.equipName" placeholder="请输入装备名称" />
                </el-form-item>
                <el-form-item label="开始时间">
                  <el-date-picker v-model="formInfo.startTime" type="datetime" placeholder="请选择" style="width: 100%" />
                </el-form-item>
                <el-form-item label="结束时间">
                  <el-date-picker v-model="formInfo.endTime" type="datetime" placeholder="请选择" style="width: 100%" />
                </el-form-item>
                <el-form-item label="试验阶段">
                  <el-select v-model="formInfo.testPhase" placeholder="请选择" style="width: 100%">
                    <el-option label="方案论证阶段" value="phase1" />
                    <el-option label="初步设计阶段" value="phase2" />
                    <el-option label="详细设计阶段" value="phase3" />
                    <el-option label="定型试验阶段" value="phase4" />
                  </el-select>
                </el-form-item>
                <el-form-item label="试验任务">
                  <el-input v-model="formInfo.testTask" placeholder="请输入试验任务" />
                </el-form-item>
                <el-form-item label="所属系统">
                  <el-input v-model="formInfo.systemName" placeholder="请输入所属系统名称" />
                </el-form-item>
                <el-form-item label="上报截止时间">
                  <el-date-picker v-model="formInfo.deadline" type="datetime" placeholder="请选择" style="width: 100%" />
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </aside>
    </div>

    <!-- Indicator Node Library Dialog -->
    <el-dialog v-model="indicatorDialogVisible" title="指标节点库" width="700px" class="indicator-dialog">
      <div class="indicator-search">
        <el-input v-model="indicatorSearch" placeholder="搜索指标节点名称/编号..." prefix-icon="Search" clearable />
      </div>
      <el-table :data="filteredIndicators" highlight-current-row @current-change="onIndicatorSelect" class="tech-table"
        max-height="400">
        <el-table-column prop="code" label="节点编号" width="130" />
        <el-table-column prop="name" label="指标名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="category" label="所属类别" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="level" label="层级" width="80" />
      </el-table>
      <template #footer>
        <el-button @click="indicatorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmIndicator">确认绑定</el-button>
      </template>
    </el-dialog>

    <!-- Dynamic Records Viewer Dialog -->
    <el-dialog v-model="recordsDialogVisible" title="采录数据详情" width="800px" class="tech-dialog">
      <el-table :data="recordsData" class="cyber-editable-table" max-height="400">
        <el-table-column v-if="recordsColumns.length === 0" label="暂无列数据定义 / 无返回" />
        <el-table-column v-for="col in recordsColumns" :key="col.prop" :prop="col.prop" :label="col.label"
          show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Aim, Plus, Delete, Check, Close, MagicStick,
  DocumentAdd, Search, OfficeBuilding, DataLine
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getCollectForm, queryTableDataPage } from '@/api/dataCollection'
import { getZhpgEquipmentTypeLabel } from '@/constants/zhpgIndicatorSystem'

const route = useRoute()
const router = useRouter()
const isGenerating = ref(false)
const rightTab = ref('info')
const formTitle = ref('未命名数据采集模板')
const collectStatus = ref('')
const statusOptions = ref([
  { label: '未下发', value: 'NOT_ISSUED' },
  { label: '采集中', value: 'COLLECTING' },
  { label: '已完成', value: 'COMPLETED' }
])

const recordsDialogVisible = ref(false)
const recordsData = ref([])
const recordsColumns = ref([])

const formInfo = ref({
  templateCode: '',
  templateName: '',
  taskCode: '',
  taskName: '',
  equipCode: '',
  equipName: '',
  startTime: '',
  endTime: '',
  testPhase: '',
  testTask: '',
  systemName: '',
  deadline: ''
})

const tableData = ref([
])

const loadFormData = async (id) => {
  try {
    const res = await getCollectForm(id)
    if (res.code === 200 || res.data) {
      const data = res.data
      formTitle.value = data.formName || '未命名数据采集模板'

      // Map formInfo
      formInfo.value = {
        templateCode: data.formCode || '',
        templateName: data.formName || '',
        taskCode: data.requireCode || '',
        taskName: data.requireName || '',
        equipCode: '',
        equipName: data.issuingUnit || '',
        startTime: '',
        endTime: '',
        testPhase: '',
        testTask: '',
        systemName: data.issuingUnit || '',
        deadline: data.collectTime || ''
      }

      // Maps form status if accessible
      collectStatus.value = data.collectStatus || data.status || route.query.collectStatus || ''

      // Map tableData
      if (data.items && Array.isArray(data.items)) {
        tableData.value = data.items.map(item => {
          const constraint = JSON.parse(item.dataConstraint)
          return {
            name: item.dataItemName || '未标定字段',
            dataSourceId: item.dataSourceId ? getZhpgEquipmentTypeLabel(item.dataSourceId) : '',
            relatedIndicator: item.relatedIndicator || '',
            tableCode: item.tableCode || '',
            fieldCode: item.fieldCode || '',
            timeRange: (constraint.taskTimeStart && constraint.taskTimeEnd) ? [constraint.taskTimeStart, constraint.taskTimeEnd] : [],
            indicator: `${item.relatedIndicator || ''}`,
            experimentalTask: item.experimentalTask || '',
            taskStage: item.taskStage || '',
            status: data.collectStatus || 'NOT_ISSUED'
          }
        })
      }

      ElMessage.success('表单模型数据加载成功')
    }
  } catch (error) {
    console.error('Failed to load form data:', error)
    ElMessage.error('加载模型数据失败')
  }
}

onMounted(() => {
  const { id } = route.query
  if (id) {
    loadFormData(id)
  }
})

// const generateFromRequirement = (name, id) => {
//     isGenerating.value = true
//     formTitle.value = `采集表单 - ${name}`

//     setTimeout(() => {
//         formInfo.value.taskName = name
//         formInfo.value.taskCode = id || `TASK-${Date.now().toString().slice(-4)}`
//         formInfo.value.templateName = `${name}采集模板`

//         // Mock some AI generated data rows
//         tableData.value = [
//             { name: '信号占用带宽', dataSourceId: 'AI-GEN-01', timeRange: null, indicator: 'IND-BW-01', dataConstraint: ['required'] },
//             { name: '干扰功率谱密度', dataSourceId: 'AI-GEN-02', timeRange: null, indicator: 'IND-PWR-02', dataConstraint: ['range'] },
//             { name: '目标捕获时长', dataSourceId: 'AI-GEN-03', timeRange: null, indicator: 'IND-TIME-01', dataConstraint: ['required'] },
//             { name: '节点连接数', dataSourceId: 'AI-GEN-04', timeRange: null, indicator: 'IND-CON-05', dataConstraint: ['enum'] },
//             { name: '系统吞吐量', dataSourceId: 'AI-GEN-05', timeRange: null, indicator: 'IND-TP-08', dataConstraint: ['required', 'format'] }
//         ]

//         isGenerating.value = false
//         ElMessage.success('已基于需求文档自动解析并填充 5 项关键数据定义')
//     }, 2500)
// }

const addRow = () => {
  tableData.value.push({
    name: '',
    dataSourceId: '',
    tableCode: '',
    fieldCode: '',
    timeRange: [],
    indicator: '',
    constraintTaskStage: '',
    constraintTimeRange: []
  })
}

const deleteRow = (row) => {
  tableData.value.splice(tableData.value.indexOf(row), 1)
}

const handleView = async (row) => {
  recordsDialogVisible.value = true
  try {
    const res = await queryTableDataPage({
      fields: row.fieldCode ? [row.fieldCode] : ['*'],
      tableCode: row.tableCode || formInfo.value.templateCode
    })
    if (res.code === 200) {
      const list = res.data?.records || res.data?.list || res.data || []
      recordsData.value = Array.isArray(list) ? list : []
      if (recordsData.value.length > 0) {
        recordsColumns.value = Object.keys(recordsData.value[0]).filter(k => !k.startsWith('_')).map(k => ({
          label: k.toUpperCase(),
          prop: k
        }))
      } else {
        recordsColumns.value = []
      }
    }
  } catch (e) {
    recordsData.value = []
  }
}

const saveDraft = () => ElMessage.info('草案已保存')
const handleGenerate = () => {
  ElMessage.success('表单生成并发布成功')
  // router.push('/process/process-sys/templates')
}

// Indicator Selection
const indicatorDialogVisible = ref(false)
const indicatorSearch = ref('')
const currentRow = ref(null)
const selectedIndicatorRow = ref(null)

const indicatorNodes = ref([
  { code: 'IND-001', name: '星地链路丢包率', category: '通信', level: 'L3' },
  { code: 'IND-003', name: '姿态控制精度', category: '控制', level: 'L3' },
  { code: 'IND-005', name: '天线增益', category: '通信', level: 'L3' },
  { code: 'IND-006', name: '太阳能帆板输出', category: '电源', level: 'L2' },
  { code: 'IND-008', name: '遥测数据率', category: '测控', level: 'L3' }
])

const filteredIndicators = computed(() => {
  if (!indicatorSearch.value) return indicatorNodes.value
  return indicatorNodes.value.filter(n => n.name.includes(indicatorSearch.value) || n.code.includes(indicatorSearch.value))
})

const openIndicatorLib = (row) => {
  currentRow.value = row
  indicatorDialogVisible.value = true
}

const onIndicatorSelect = (val) => {
  selectedIndicatorRow.value = val
}

const confirmIndicator = () => {
  if (selectedIndicatorRow.value && currentRow.value) {
    currentRow.value.indicator = `${selectedIndicatorRow.value.code} - ${selectedIndicatorRow.value.name}`
  }
  indicatorDialogVisible.value = false
}
</script>

<style scoped lang="scss">
.designer-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-color);
  color: var(--text-color-primary);
}

.designer-header {
  height: 54px;
  padding: 0 20px;
  background: var(--header-bg-color);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;

  .title {
    font-weight: bold;
    color: var(--primary-color);
    font-size: 15px;

    .v-tag {
      font-size: 10px;
      padding: 2px 6px;
      background: rgba(var(--primary-color-rgb), 0.15);
      border-radius: 4px;
      margin-left: 8px;
    }
  }
}

.designer-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.designer-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 24px;
  position: relative;
  overflow: hidden;

  .toolbar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .title-input-edit :deep(.el-input__wrapper) {
      background: transparent !important;
      box-shadow: none !important;
      border-bottom: 1px solid var(--border-color);
      padding: 0;
      border-radius: 0;

      input {
        color: var(--text-color-primary);
        font-size: 18px;
        font-weight: bold;
      }
    }
  }
}

.table-scroller {
  flex: 1;
  background: var(--card-bg-color);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: auto;
}

.side-panel {
  width: 280px;
  padding-left: 10px;
  background: var(--sider-bg-color);
  border-left: 1px solid var(--border-color);
}

/* Cyber Table Styling */
:deep(.cyber-editable-table) {
  background-color: transparent !important;
  --el-table-border-color: var(--border-color);
  --el-table-header-bg-color: rgba(var(--primary-color-rgb), 0.1);
  --el-table-header-text-color: var(--primary-color);
  --el-table-row-hover-bg-color: rgba(var(--primary-color-rgb), 0.08);
  --el-table-tr-bg-color: transparent;

  &.el-table--striped .el-table__row--striped td.el-table__cell {
    background: rgba(var(--primary-color-rgb), 0.03) !important;
  }

  .el-table__header-wrapper th {
    border-bottom: 2px solid var(--border-color-hover) !important;
    // text-shadow: 0 0 2px rgba(var(--primary-color-rgb), 0.5);
  }

  .el-input__wrapper {
    background-color: var(--card-bg-color) !important;
    box-shadow: 0 0 0 1px var(--border-color) inset !important;
    border: none;
    transition: all 0.3s;

    &:hover,
    &.is-focus {
      box-shadow: 0 0 0 1px var(--primary-color) inset, 0 0 10px rgba(var(--primary-color-rgb), 0.2) !important;
    }
  }

  .el-input__inner {
    color: var(--text-color-primary);
    font-size: 13px;
  }

  .el-range-input {
    background: transparent;
    color: var(--text-color-primary);
  }

  .el-range-separator {
    color: var(--text-color-secondary);
  }
}

.table-scroller {
  // ... existing properties ...
  background: var(--card-bg-color);
  border: 1px solid var(--border-color);
  
  // ... scrollbar styles ...
  &::-webkit-scrollbar-thumb {
    background: rgba(var(--primary-color-rgb), 0.2);
    border-radius: 3px;

    &:hover {
      background: rgba(var(--primary-color-rgb), 0.4);
    }
  }

  .indicator-bind-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .bind-icon {
      cursor: pointer;
      color: var(--primary-color);

      &:hover {
        color: var(--text-color-primary);
        transform: scale(1.1);
      }
    }
  }

  .constraint-edit-group {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;

    :deep(.el-range-editor.el-input__inner) {
      background: var(--card-bg-color) !important;
    }
  }
}

.ai-generating-overlay {
  position: absolute;
  inset: 0;
  background: var(--bg-color-overlay);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  backdrop-filter: var(--backdrop-filter);

  .ai-text {
    color: var(--primary-color);
    font-size: 20px;
    margin-top: 40px;
  }

  .ai-subtext {
    color: var(--text-color-secondary);
    font-size: 14px;
    margin-top: 10px;
  }
}

.magic-icon-anim {
  width: 60px;
  height: 60px;
  color: var(--primary-color);
  animation: rotate-magic 2s linear infinite;
}

@keyframes rotate-magic {
  0% {
    transform: rotate(0) scale(1);
    filter: drop-shadow(0 0 5px var(--primary-color));
  }

  50% {
    transform: rotate(180deg) scale(1.2);
    filter: drop-shadow(0 0 20px var(--primary-color));
  }

  100% {
    transform: rotate(360deg) scale(1);
    filter: drop-shadow(0 0 5px var(--primary-color));
  }
}

.mb-4 {
  margin-bottom: 16px;
}

:deep(.info-form) {
  padding: 20px;

  .el-form-item__label {
    color: var(--text-color-secondary) !important;
    font-size: 12px;
  }
}
</style>
