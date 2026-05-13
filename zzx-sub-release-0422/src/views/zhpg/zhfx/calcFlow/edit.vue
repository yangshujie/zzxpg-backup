<template>
  <el-dialog
    :title="templateId ? '编辑流程模板' : '新建流程模板'"
    :modelValue="visible"
    @update:modelValue="$emit('update:visible', $event)"
    width="1100px"
    class="calc-flow-edit-dialog"
    append-to-body
    :close-on-click-modal="false"
    @open="onDialogOpen"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <!-- ==================== 基本信息 ==================== -->
      <el-divider content-position="left">基本信息</el-divider>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="模板名称" prop="templateName">
            <el-input v-model="form.templateName" placeholder="请输入模板名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="任务类型" prop="taskType">
            <el-select v-model="form.taskType" placeholder="请选择任务类型" style="width: 100%" @change="onTaskTypeChange">
              <el-option v-for="item in taskTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="模板说明">
            <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- ==================== 流程阶段可视化 ==================== -->
      <el-divider content-position="left">流程阶段编排</el-divider>
      <div class="flow-pipeline">
        <div
          v-for="(stage, idx) in stageList"
          :key="stage.key"
          class="flow-stage-wrapper"
        >
          <div
            class="flow-stage"
            :class="{ active: activeStage === stage.key, disabled: !stageConfig[stage.key].enabled }"
            @click="activeStage = stage.key"
          >
            <div class="flow-stage-name">{{ stage.label }}</div>
          </div>
          <div v-if="idx < stageList.length - 1" class="flow-arrow">
            <el-icon><Right /></el-icon>
          </div>
        </div>
      </div>
      <!-- ==================== 阶段配置表单 ==================== -->
      <div class="stage-config-panel">
        <!-- 阶段一：调度策略选配 -->
        <div v-show="activeStage === 'scheduleConfig'" class="stage-form">
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="路由策略">
                <el-select v-model="stageConfig.scheduleConfig.config.routeStrategy" style="width: 100%">
                  <el-option v-for="item in routeStrategyOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="阻塞处理策略">
                <el-select v-model="runtimePolicy.blockStrategy" style="width: 100%">
                  <el-option label="单机串行（SERIAL_EXECUTION）" value="SERIAL_EXECUTION" />
                  <el-option label="丢弃后续调度（DISCARD_LATER）" value="DISCARD_LATER" />
                  <el-option label="覆盖之前调度（COVER_EARLY）" value="COVER_EARLY" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="错过调度策略">
                <el-select v-model="runtimePolicy.misfireStrategy" style="width: 100%">
                  <el-option label="忽略（DO_NOTHING）" value="DO_NOTHING" />
                  <el-option label="立即补触发一次（FIRE_ONCE_NOW）" value="FIRE_ONCE_NOW" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="超时时间(秒)">
                <el-input-number v-model="stageConfig.scheduleConfig.config.timeoutSeconds" :min="0" :max="3600" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="失败重试次数">
                <el-input-number v-model="stageConfig.scheduleConfig.config.retryTimes" :min="0" :max="10" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 阶段二：权重计算 -->
        <div v-show="activeStage === 'weightCalc'" class="stage-form">
          <div class="stage-hint">
            <el-alert type="info" :closable="false" show-icon>
              <template #title>执行计算时将使用请求中指定的指标体系；权重算法与权重值来自该体系，此处仅配置运行时执行策略</template>
            </el-alert>
          </div>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="权重来源">
                <el-select v-model="stageConfig.weightCalc.config.weightSource" style="width: 100%">
                  <el-option label="沿用运行时指标体系权重" value="INDICATOR_SYSTEM" />
                  <el-option label="流程覆盖（重新计算）" value="TEMPLATE_OVERRIDE" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8" v-show="stageConfig.weightCalc.config.weightSource === 'TEMPLATE_OVERRIDE'">
              <el-form-item label="覆盖算法">
                <el-select v-model="stageConfig.weightCalc.config.overrideAlgorithmId" placeholder="请选择权重分配算法" filterable clearable style="width: 100%">
                  <el-option v-for="alg in weightAlgorithmList" :key="alg.id" :label="alg.algorithmName" :value="alg.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="权重缺失处理">
                <el-select v-model="stageConfig.weightCalc.config.missingWeightPolicy" style="width: 100%">
                  <el-option label="同层均分补齐" value="EQUAL_DISTRIBUTE" />
                  <el-option label="终止计算" value="TERMINATE" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 阶段三：综合分析计算（运行时计算编排，算法定义在SFMX中维护） -->
        <div v-show="activeStage === 'comprehensiveCalc'" class="stage-form">
          <div class="stage-hint">
            <el-alert type="info" :closable="false" show-icon>
              <template #title>算法模型及其参数由算法模型配置(SFMX)统一管理，此处配置计算编排、聚合与结果处理策略</template>
            </el-alert>
          </div>
          <!-- 第一行：执行方式 + 空数据处理 + 中间结果 -->
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="执行方式">
                <el-select v-model="stageConfig.comprehensiveCalc.config.algorithmChainMode" style="width: 100%">
                  <el-option label="串行执行" value="SERIAL" />
                  <el-option label="并行后聚合" value="PARALLEL" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8" v-show="stageConfig.comprehensiveCalc.config.algorithmChainMode === 'PARALLEL'">
              <el-form-item label="并行批次大小">
                <el-input-number v-model="stageConfig.comprehensiveCalc.config.parallelBatchSize" :min="1" :max="20" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="空数据处理">
                <el-select v-model="stageConfig.comprehensiveCalc.config.nullDataPolicy" style="width: 100%">
                  <el-option label="补零参与计算" value="ZERO_FILL" />
                  <el-option label="终止计算" value="TERMINATE" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <!-- 第二行：聚合策略 + 中间结果 -->
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="聚合策略来源">
                <el-select v-model="stageConfig.comprehensiveCalc.config.aggregationSource" style="width: 100%">
                  <el-option label="沿用运行时指标体系配置" value="INDICATOR_SYSTEM" />
                  <el-option label="流程覆盖指定" value="TEMPLATE_OVERRIDE" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8" v-show="stageConfig.comprehensiveCalc.config.aggregationSource === 'TEMPLATE_OVERRIDE'">
              <el-form-item label="覆盖聚合算法">
                <el-select v-model="stageConfig.comprehensiveCalc.config.overrideAggregationAlg" placeholder="请选择聚合算法" filterable clearable style="width: 100%">
                  <el-option v-for="alg in aggregationAlgorithmList" :key="alg.id" :label="alg.algorithmName" :value="alg.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="输出中间结果">
                <el-switch v-model="stageConfig.comprehensiveCalc.config.intermediateResultOutput" />
              </el-form-item>
            </el-col>
          </el-row>
          <!-- 动态评分等级配置 -->
          <div class="score-level-config">
            <div class="section-title" style="font-size: 14px; margin-bottom: 12px; color: #606266; font-weight: 500;">
              评分等级配置
              <el-button type="primary" link @click="addScoreLevel" style="margin-left: 10px;">+ 添加等级</el-button>
            </div>
            <el-row v-for="(level, index) in stageConfig.comprehensiveCalc.config.scoreLevels" :key="index" :gutter="20" style="margin-bottom: 10px;">
              <el-col :span="10">
                <el-form-item :label="index === 0 ? '等级名称' : ''" style="margin-bottom: 0;">
                  <el-input v-model="level.name" placeholder="如：优秀" />
                </el-form-item>
              </el-col>
              <el-col :span="10">
                <el-form-item :label="index === 0 ? '阈值(≥)' : ''" style="margin-bottom: 0;">
                  <el-input-number v-model="level.threshold" :min="0" :max="100" style="width: 100%;" />
                </el-form-item>
              </el-col>
              <el-col :span="4" style="display: flex; align-items: center; padding-top: 28px;">
                <el-button v-if="stageConfig.comprehensiveCalc.config.scoreLevels.length > 1" type="danger" link @click="removeScoreLevel(index)">删除</el-button>
              </el-col>
            </el-row>
          </div>
        </div>

        <!-- 阶段四：评估报告模板选配 -->
        <div v-show="activeStage === 'reportOutput'" class="stage-form">
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="报告模板" prop="reportTemplateId">
                <el-select
                  v-model="stageConfig.reportOutput.config.reportTemplateId"
                  placeholder="请选择报告模板"
                  style="width: 100%"
                  clearable
                  @change="handleReportTemplateChange"
                >
                  <el-option
                    v-for="item in reportTemplateList"
                    :key="item.id"
                    :label="item.templateName"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <div v-if="stageConfig.reportOutput.config.reportTemplateId" class="section-title" style="font-size: 13px; margin-top: 5px;">报告占位符匹配</div>
          <el-alert
            v-if="stageConfig.reportOutput.config.reportTemplateId"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 10px;"
          >
            <template #title>
              <span style="font-size: 12px;">仅配置模板占位符如何取值：静态文本、评估数据、任务属性。评估数据已收敛为常用业务项，旧配置会自动兼容到新选项。</span>
            </template>
          </el-alert>
          <el-table
            v-if="stageConfig.reportOutput.config.reportTemplateId"
            :data="stageConfig.reportOutput.config.placeholderMappings || []"
            border
            size="small"
            class="report-mapping-table"
            max-height="400"
            style="width: 100%; margin-bottom: 15px;"
          >
            <el-table-column label="占位符" min-width="320">
              <template #default="scope">
                <div class="placeholder-cell">
                  <div class="placeholder-name">{{ scope.row.label || getPlaceholderMeta(scope.row.key).label }}</div>
                  <el-tag size="small" effect="plain">{{ scope.row.key }}</el-tag>
                  <div class="placeholder-desc">{{ scope.row.description || getPlaceholderMeta(scope.row.key).description }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="映射类型" width="160">
              <template #default="scope">
                <el-select v-model="scope.row.mappingType" size="small" style="width: 100%" @change="onMappingTypeChange(scope.row)">
                  <el-option label="静态文本" value="STATIC_TEXT" />
                  <el-option label="评估数据" value="AUTO_INDICATOR" />
                  <el-option label="任务属性" value="TASK_PROPERTY" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="取值" min-width="260">
              <template #default="scope">
                <!-- 静态文本 -->
                <template v-if="scope.row.mappingType === 'STATIC_TEXT'">
                  <div v-if="getPlaceholderUsage(scope.row.key) === 'image'" class="image-upload-wrapper">
                    <ImageUpload
                      v-model="scope.row.mappingValue"
                      action="/zhpg-api/zhpg/report/templates/uploadImage"
                      preview-prefix="/zhpg-api"
                      :limit="1"
                      :is-show-tip="false"
                    />
                  </div>
                  <el-input
                    v-else
                    v-model="scope.row.mappingValue"
                    size="small"
                    :placeholder="getStaticTextPlaceholder(scope.row.key)"
                  />
                </template>
                <el-select
                  v-else-if="scope.row.mappingType === 'AUTO_INDICATOR'"
                  v-model="scope.row.mappingValue"
                  size="small"
                  placeholder="选择评估数据项"
                  popper-class="report-mapping-popper"
                  style="width: 100%"
                >
                  <el-option-group v-for="grp in getAutoIndicatorGroups(scope.row)" :key="grp.label" :label="grp.label">
                    <el-option
                      v-for="opt in grp.options"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                    >
                      <div class="report-mapping-option">{{ opt.label }}</div>
                    </el-option>
                  </el-option-group>
                </el-select>
                <!-- 评估任务属性 -->
                <el-select
                  v-else-if="scope.row.mappingType === 'TASK_PROPERTY'"
                  v-model="scope.row.mappingValue"
                  size="small"
                  placeholder="请选择任务属性"
                  style="width: 100%"
                >
                  <el-option v-for="opt in taskPropertyOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="$emit('update:visible', false)">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance, watch, nextTick } from 'vue'
import { Right } from '@element-plus/icons-vue'
import { getCalcFlow, addCalcFlow, updateCalcFlow } from '@/api/zhpg/calcFlow'
import { listAllAlgorithm } from '@/api/zhpg/algorithm'
import { listTemplates, getTemplate } from '@/api/zhpg/report'
import {
  TASK_PROPERTY_OPTIONS,
  createReportMapping,
  extractPlaceholdersFromHtml,
  getAutoIndicatorOptionGroups,
  getPlaceholderMeta,
  getPlaceholderUsage,
  getStaticTextPlaceholder,
  inferReportMapping,
  normalizeReportMappings,
} from '@/utils/zhpg/reportPlaceholders'
import ImageUpload from '@/components/ImageUpload'

const props = defineProps({
  visible: Boolean,
  /** 支持 number / string（表格行 id 可能为字符串） */
  templateId: { type: [Number, String], default: null }
})
const emit = defineEmits(['update:visible', 'saved'])

const { proxy } = getCurrentInstance()

/** 与列表页「评估任务类型」一致 */
const taskTypeOptions = [
  { label: '性能指标评估', value: 'PERFORMANCE' },
  { label: '装备作战效能评估', value: 'EQUIP_EFFECTIVENESS' },
  { label: '体系贡献率评估', value: 'SYSTEM_CONTRIBUTION' },
  { label: '作战任务满足度评估', value: 'TASK_SATISFACTION' },
  { label: '体系级任务评估', value: 'SYSTEM_TASK' },
  { label: '演习演训任务评估', value: 'EXERCISE_TRAINING' }
]

// ==================== 基础数据 ====================
const algorithmList = ref([])
const reportTemplateList = ref([])

const taskPropertyOptions = TASK_PROPERTY_OPTIONS

// 权重分配算法列表（类型为"权重分配"的算法）
const weightAlgorithmList = computed(() => {
  return algorithmList.value.filter(alg => alg.algorithmType === '权重分配')
})

// 聚合算法列表（类型为"聚合传导"的算法）
const aggregationAlgorithmList = computed(() => {
  return algorithmList.value.filter(alg => alg.algorithmType === '聚合传导')
})

const routeStrategyOptions = [
  { label: '第一个', value: 'FIRST' },
  { label: '最后一个', value: 'LAST' },
  { label: '轮询', value: 'ROUND' },
  { label: '随机', value: 'RANDOM' },
  { label: '一致性哈希', value: 'CONSISTENT_HASH' },
  { label: '最不经常使用', value: 'LEAST_FREQUENTLY_USED' },
  { label: '最近最久未使用', value: 'LEAST_RECENTLY_USED' },
  { label: '故障转移', value: 'FAILOVER' },
  { label: '忙碌转移', value: 'BUSYOVER' },
  { label: '分片广播', value: 'SHARDING_BROADCAST' }
]
// ==================== 流程阶段定义 ====================
const stageList = [
  { key: 'scheduleConfig', label: '调度策略选配' },
  { key: 'weightCalc', label: '权重计算' },
  { key: 'comprehensiveCalc', label: '综合分析计算' },
  { key: 'reportOutput', label: '评估报告模板选配' }
]
const activeStage = ref('scheduleConfig')

// ==================== 表单数据 ====================
const form = reactive({
  id: null,
  templateCode: '',
  templateName: '',
  taskType: 'EQUIP_EFFECTIVENESS',
  equipmentType: undefined,
  calcGranularity: 'EQUIP_EFFECTIVENESS',
  /** 仅兼容历史数据回传，界面已取消「关联指标体系」编辑 */
  indicatorSystemId: undefined,
  indicatorSystemName: '',
  dataPlanId: undefined,
  dataPlanName: '',
  description: '',
  status: 'DRAFT',
  versionNo: 'V1.0'
})

const stageConfig = reactive({
  scheduleConfig: {
    stageCode: 'SCHEDULE_CONFIG',
    stageName: '调度策略选配',
    stageOrder: 1,
    enabled: true,
    config: {
      routeStrategy: 'FIRST',
      timeoutSeconds: 300,
      retryTimes: 0
    }
  },
  weightCalc: {
    stageCode: 'WEIGHT_CALC',
    stageName: '权重计算',
    stageOrder: 2,
    enabled: true,
    config: {
      weightSource: 'INDICATOR_SYSTEM',       // 权重来源：沿用指标体系 / 流程覆盖
      overrideAlgorithmId: null,              // 仅 TEMPLATE_OVERRIDE 时生效，覆盖算法ID
      missingWeightPolicy: 'EQUAL_DISTRIBUTE' // 权重缺失处理：同层均分补齐 / 终止计算
    }
  },
  comprehensiveCalc: {
    stageCode: 'COMPREHENSIVE_CALC',
    stageName: '综合分析计算',
    stageOrder: 3,
    enabled: true,
    config: {
      algorithmChainMode: 'SERIAL',            // 执行方式：串行 / 并行
      parallelBatchSize: 5,                    // 并行批次大小（并行模式生效）
      aggregationSource: 'INDICATOR_SYSTEM',   // 聚合策略来源：沿用指标体系 / 流程覆盖
      overrideAggregationAlg: null,            // 覆盖的聚合算法ID
      nullDataPolicy: 'ZERO_FILL',             // 空数据处理策略
      scoreLevels: [                           // 动态评分等级配置
        { name: '优秀', threshold: 90 },
        { name: '良好', threshold: 75 },
        { name: '及格', threshold: 60 }
      ],
      intermediateResultOutput: false          // 是否输出中间计算结果
    }
  },
  reportOutput: {
    stageCode: 'REPORT_OUTPUT',
    stageName: '评估报告模板选配',
    stageOrder: 4,
    enabled: true,
    config: {
      reportTemplateId: null,
      placeholderMappings: []
    }
  }
})

const runtimePolicy = reactive({
  blockStrategy: 'SERIAL_EXECUTION',
  misfireStrategy: 'DO_NOTHING'
})

const rules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }]
}

/** 从 RuoYi 接口响应中取出实体（兼容仅返回 data 或整包 AjaxResult） */
function unwrapCalcFlowDetail(res) {
  if (res == null) return null
  const inner = res.data
  if (inner && typeof inner === 'object' && inner.id != null) return inner
  if (res.id != null) return res
  if (inner && typeof inner === 'object' && (inner.templateName != null || inner.templateCode != null)) return inner
  return null
}

function unwrapReportTemplateDetail(res) {
  if (res == null) return null
  const inner = res.data
  if (inner && typeof inner === 'object' && (inner.id != null || inner.templateName != null)) return inner
  if (res.id != null) return res
  return inner || null
}

function granularityForTaskType(taskType) {
  if (taskType === 'SYSTEM_TASK') return 'SYSTEM_TASK'
  if (taskType === 'PERFORMANCE') return 'PERFORMANCE'
  return 'EQUIP_EFFECTIVENESS'
}

/** 库表 equipment_type 非空时，按任务类型给默认装备类型（界面不再单独维护） */
function defaultEquipmentForTaskType(taskType) {
  const map = {
    PERFORMANCE: 'SPACE_TTC',
    EQUIP_EFFECTIVENESS: 'SPACE_TTC',
    SYSTEM_CONTRIBUTION: 'SPACE_TTC',
    TASK_SATISFACTION: 'SPACE_TTC',
    SYSTEM_TASK: 'SPACE_TTC',
    EXERCISE_TRAINING: 'SPACE_TTC'
  }
  return map[taskType] || 'SPACE_TTC'
}

function onTaskTypeChange(val) {
  form.calcGranularity = granularityForTaskType(val)
  if (!form.equipmentType) {
    form.equipmentType = defaultEquipmentForTaskType(val)
  }
}

function onDialogOpen() {
  activeStage.value = 'scheduleConfig'
  loadBaseData()
}

watch(
  () => [props.visible, props.templateId],
  ([vis]) => {
    if (!vis) return
    nextTick(() => {
      const rawId = props.templateId
      const id = rawId != null && rawId !== '' ? Number(rawId) : NaN
      if (Number.isFinite(id) && id > 0) {
        loadTemplate(id)
      } else {
        resetForm()
        resetStageConfig()
      }
    })
  }
)

// ==================== 初始化 ====================

function resetStageConfig() {
  // 重置调度策略配置
  stageConfig.scheduleConfig.config = {
    routeStrategy: 'FIRST',
    timeoutSeconds: 300,
    retryTimes: 0
  }
  // 重置权重计算配置
  stageConfig.weightCalc.config = {
    weightSource: 'INDICATOR_SYSTEM',
    overrideAlgorithmId: null,
    missingWeightPolicy: 'EQUAL_DISTRIBUTE'
  }
  // 重置综合分析计算配置
  stageConfig.comprehensiveCalc.config = {
    algorithmChainMode: 'SERIAL',
    parallelBatchSize: 5,
    aggregationSource: 'INDICATOR_SYSTEM',
    overrideAggregationAlg: null,
    nullDataPolicy: 'ZERO_FILL',
    scoreLevels: [
      { name: '优秀', threshold: 90 },
      { name: '良好', threshold: 75 },
      { name: '及格', threshold: 60 }
    ],
    intermediateResultOutput: false
  }
  // 重置报告输出配置
  stageConfig.reportOutput.config = {
    reportTemplateId: null,
    placeholderMappings: []
  }
  // 重置运行时策略
  runtimePolicy.blockStrategy = 'SERIAL_EXECUTION'
  runtimePolicy.misfireStrategy = 'DO_NOTHING'
}

onMounted(() => {
  loadBaseData()
})

async function loadBaseData() {
  console.log('[CalcFlowEdit] Loading base data...')
  try {
    const algRes = await listAllAlgorithm({ publishStatus: 'PUBLISHED' })
    console.log('[CalcFlowEdit] Algorithms loaded:', algRes)
    algorithmList.value = algRes.data || algRes.rows || []
  } catch (e) {
    console.error('[CalcFlowEdit] 加载算法数据失败', e)
  }
  
  try {
    const tplRes = await listTemplates()
    console.log('[CalcFlowEdit] Report templates raw response:', tplRes)
    if (Array.isArray(tplRes)) {
      reportTemplateList.value = tplRes
    } else {
      reportTemplateList.value = tplRes.data || tplRes.rows || []
    }
    console.log('[CalcFlowEdit] Report templates assigned:', reportTemplateList.value)
  } catch (e) {
    console.error('[CalcFlowEdit] 加载报告模板失败', e)
  }
}

async function handleReportTemplateChange(id) {
  if (!id) {
    stageConfig.reportOutput.config.placeholderMappings = []
    return
  }
  try {
    const res = await getTemplate(id)
    const data = unwrapReportTemplateDetail(res)
    if (!data) {
      console.error('获取报告模板详情失败', res)
      return
    }
    const placeholders = data.placeholders || extractPlaceholdersFromHtml(data.htmlContent)

    const existingMappings = stageConfig.reportOutput.config.placeholderMappings || []
    stageConfig.reportOutput.config.placeholderMappings = placeholders.map(key => createReportMapping(key, existingMappings))
    normalizePlaceholderMappings()
  } catch(e) {
    console.error('获取报告模板详情失败', e)
  }
}

function normalizePlaceholderMappings() {
  stageConfig.reportOutput.config.placeholderMappings = normalizeReportMappings(stageConfig.reportOutput.config.placeholderMappings)
}

function normalizeReportOutputConfig() {
  stageConfig.reportOutput.config = {
    reportTemplateId: stageConfig.reportOutput.config?.reportTemplateId || null,
    placeholderMappings: normalizeReportMappings(stageConfig.reportOutput.config?.placeholderMappings || [])
  }
}

// 根据占位符名称智能推断映射类型（与精简后的评估数据项对齐）
function inferMappingType(key) {
  return inferReportMapping(key)
}

function getAutoIndicatorGroups(row) {
  return getAutoIndicatorOptionGroups(row?.key)
}

function getDefaultAutoIndicatorValue(row, inferredValue = '') {
  const options = getAutoIndicatorGroups(row).flatMap(group => group.options)
  return options.some(option => option.value === inferredValue) ? inferredValue : options[0]?.value || ''
}

function onMappingTypeChange(row) {
  // 切换映射类型时，根据类型设置默认值
  row.mappingValue = ''
  if (row.mappingType === 'AUTO_INDICATOR') {
    // 尝试根据占位符名称推断默认值
    const inferred = inferMappingType(row.key)
    row.mappingValue = getDefaultAutoIndicatorValue(row, inferred.type === 'AUTO_INDICATOR' ? inferred.value : '')
  } else if (row.mappingType === 'TASK_PROPERTY') {
    const inferred = inferMappingType(row.key)
    row.mappingValue = inferred.type === 'TASK_PROPERTY' ? inferred.value : 'taskName'
  }
}

// ==================== 评分等级动态配置 ====================
function addScoreLevel() {
  stageConfig.comprehensiveCalc.config.scoreLevels.push({
    name: '',
    threshold: 0
  })
}

function removeScoreLevel(index) {
  stageConfig.comprehensiveCalc.config.scoreLevels.splice(index, 1)
}

async function loadTemplate(id) {
  const res = await getCalcFlow(id)
  const data = unwrapCalcFlowDetail(res)
  if (!data) {
    console.warn('[CalcFlowEdit] 流程模板详情为空', res)
    proxy.$modal.msgError('加载流程模板失败，请检查接口返回')
    return
  }
  // 填充基本信息（含 null，避免切换记录时残留旧值）
  Object.keys(form).forEach(key => {
    if (Object.prototype.hasOwnProperty.call(data, key)) {
      form[key] = data[key]
    }
  })
  form.equipmentType = normalizeEquipmentTypeValue(form.equipmentType)
  // 解析 configJson
  if (data.configJson) {
    try {
      const cfg = typeof data.configJson === 'string' ? JSON.parse(data.configJson) : data.configJson
      if (cfg.stages) {
        Object.keys(cfg.stages).forEach(key => {
          if (stageConfig[key]) {
            deepAssign(stageConfig[key], cfg.stages[key])
          }
        })
        // 兼容性处理：旧数据使用 scoreLevelConfig 转换为 scoreLevels
        const calcCfg = cfg.stages.comprehensiveCalc?.config
        if (calcCfg?.scoreLevelConfig && !calcCfg.scoreLevels) {
          const oldCfg = calcCfg.scoreLevelConfig
          stageConfig.comprehensiveCalc.config.scoreLevels = [
            { name: '优秀', threshold: oldCfg.EXCELLENT || 90 },
            { name: '良好', threshold: oldCfg.GOOD || 75 },
            { name: '及格', threshold: oldCfg.PASS || 60 }
          ].filter(l => l.threshold > 0)
        }
      }
      if (cfg.runtimePolicy) {
        Object.assign(runtimePolicy, cfg.runtimePolicy)
      }
    } catch (e) {
      console.warn('解析 configJson 失败', e)
    }
  }
  normalizeReportOutputConfig()
  const reportTemplateId = stageConfig.reportOutput.config.reportTemplateId
  if (reportTemplateId) {
    handleReportTemplateChange(reportTemplateId)
  }
}

function resetForm() {
  form.id = null
  form.templateCode = ''
  form.templateName = ''
  form.taskType = 'EQUIP_EFFECTIVENESS'
  form.equipmentType = undefined
  form.calcGranularity = 'EQUIP_EFFECTIVENESS'
  form.versionNo = 'V1.0'
  form.indicatorSystemId = undefined
  form.indicatorSystemName = ''
  form.dataPlanId = undefined
  form.dataPlanName = ''
  form.description = ''
  form.status = 'DRAFT'
}

// ==================== 保存 ====================
async function handleSave() {
  try {
    await proxy.$refs.formRef.validate()
  } catch {
    return
  }

  normalizeReportOutputConfig()

  // 组装 configJson
  const configJson = JSON.stringify({
    stages: JSON.parse(JSON.stringify(stageConfig)),
    runtimePolicy: JSON.parse(JSON.stringify(runtimePolicy))
  })

  const submitData = { ...form, configJson }
  // 与指标体系解耦：保存时不再提交关联（避免无效 ID 触发后端校验）
  submitData.indicatorSystemId = null
  submitData.indicatorSystemName = null
  if (!submitData.equipmentType) {
    submitData.equipmentType = defaultEquipmentForTaskType(submitData.taskType)
  }
  if (!submitData.calcGranularity) {
    submitData.calcGranularity = granularityForTaskType(submitData.taskType)
  }

  if (form.id) {
    // 更新
    await updateCalcFlow(submitData)
    proxy.$modal.msgSuccess('保存成功')
  } else {
    // 新增
    await addCalcFlow(submitData)
    proxy.$modal.msgSuccess('创建成功')
  }

  emit('saved')
  emit('update:visible', false)
}

// ==================== 工具方法 ====================
function deepAssign(target, source) {
  Object.keys(source).forEach(key => {
    if (source[key] !== null && typeof source[key] === 'object' && !Array.isArray(source[key]) && target[key]) {
      deepAssign(target[key], source[key])
    } else {
      target[key] = source[key]
    }
  })
}

function normalizeEquipmentTypeValue(value) {
  const legacyMap = {
    SPACE_AWARENESS: 'SPACE_SITUATIONAL_AWARENESS',
    SPACE_SA: 'SPACE_SITUATIONAL_AWARENESS',
    SPACE_COMBAT: 'SPACE_OFFENSE_DEFENSE',
    SPACE_AD: 'SPACE_OFFENSE_DEFENSE',
    COMM_COUNTER: 'SPACE_TTC',
    NAV_POSITION: 'SEA_BASED_SPACE',
    SEA_BASED: 'SEA_BASED_SPACE'
  }
  return legacyMap[value] || value
}
</script>

<style scoped>
/* ==================== 流程管线样式 ==================== */
.flow-pipeline {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
  gap: 0;
}
.flow-stage-wrapper {
  display: flex;
  align-items: center;
}
.flow-stage {
  width: 150px;
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: #409eff;
  color: #fff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  padding: 8px 12px;
  transition: all 0.2s;
  user-select: none;
}
.flow-stage:hover {
  background: #337ecc;
}
.flow-stage.active {
  background: #337ecc;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.3);
}
.flow-stage.disabled {
  background: #c0c4cc;
}
.flow-arrow {
  display: flex;
  align-items: center;
  padding: 0 8px;
  color: #909399;
  font-size: 20px;
}
/* ==================== 阶段配置面板 ==================== */
.stage-config-panel {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 20px;
  background: #fafafa;
  min-height: 120px;
}
.stage-form {
  animation: fadeIn 0.2s ease;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.stage-hint {
  margin-bottom: 16px;
}
.score-level-config {
  padding: 12px 0 0;
  border-top: 1px dashed #dcdfe6;
  margin-top: 12px;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

:global(.calc-flow-edit-dialog.el-dialog) {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: var(--el-box-shadow-light);
}
:global(.calc-flow-edit-dialog .el-dialog__header),
:global(.calc-flow-edit-dialog .el-dialog__footer) {
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
}
:global(.calc-flow-edit-dialog .el-dialog__title) {
  color: var(--el-text-color-primary);
}
:global(.calc-flow-edit-dialog .el-dialog__body) {
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
}
:global(.calc-flow-edit-dialog .el-divider__text) {
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
}
:global(.calc-flow-edit-dialog .el-form-item__label) {
  color: var(--el-text-color-regular);
}
:global(.calc-flow-edit-dialog .el-input__wrapper),
:global(.calc-flow-edit-dialog .el-select__wrapper),
:global(.calc-flow-edit-dialog .el-textarea__inner) {
  background: var(--el-fill-color-blank);
  border-color: var(--el-border-color);
  box-shadow: 0 0 0 1px var(--el-border-color) inset;
  color: var(--el-text-color-primary);
}
:global(.calc-flow-edit-dialog .el-input__inner),
:global(.calc-flow-edit-dialog .el-select__placeholder),
:global(.calc-flow-edit-dialog .el-textarea__inner) {
  color: var(--el-text-color-primary);
}
:global(.calc-flow-edit-dialog .el-input-number__decrease),
:global(.calc-flow-edit-dialog .el-input-number__increase) {
  background: var(--el-fill-color-light);
  border-color: var(--el-border-color);
  color: var(--el-text-color-secondary);
}
.stage-config-panel {
  background: var(--el-fill-color-light);
  border-color: var(--el-border-color-lighter);
  box-shadow: var(--el-box-shadow-lighter);
}
.score-level-config {
  border-top-color: var(--el-border-color);
}
.section-title {
  color: var(--el-text-color-primary) !important;
}
.placeholder-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
  line-height: 1.35;
  min-height: 54px;
  justify-content: center;
}
.placeholder-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.placeholder-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.report-mapping-table :deep(.el-table__cell) {
  vertical-align: middle;
}
.report-mapping-table :deep(.el-select),
.report-mapping-table :deep(.el-input) {
  width: 100%;
}
:global(.report-mapping-popper .el-select-group__title) {
  height: 28px;
  line-height: 28px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
:global(.report-mapping-popper .el-select-dropdown__item) {
  height: auto;
  min-height: 38px;
  padding: 0 12px;
  font-size: 13px;
  line-height: 20px;
}
:global(.report-mapping-popper .report-mapping-option) {
  display: flex;
  align-items: center;
  min-height: 38px;
  width: 100%;
  font-size: 13px;
}
:global(.dark-theme .calc-flow-edit-dialog.el-dialog) {
  background: #07111f;
  border-color: rgba(0, 242, 255, 0.16);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.45), inset 0 0 0 1px rgba(24, 190, 255, 0.06);
}
:global(.dark-theme .calc-flow-edit-dialog .el-dialog__header),
:global(.dark-theme .calc-flow-edit-dialog .el-dialog__footer) {
  background: #07111f;
  color: #d8e7f5;
}
:global(.dark-theme .calc-flow-edit-dialog .el-dialog__title) {
  color: #d8e7f5;
}
:global(.dark-theme .calc-flow-edit-dialog .el-dialog__body) {
  background: linear-gradient(180deg, #07111f 0%, #050b14 100%);
  color: #d8e7f5;
}
:global(.dark-theme .calc-flow-edit-dialog .el-divider__text) {
  background: #07111f;
  color: #d8e7f5;
}
:global(.dark-theme .calc-flow-edit-dialog .el-form-item__label) {
  color: #9fb6cb;
}
:global(.dark-theme .calc-flow-edit-dialog .el-input__wrapper),
:global(.dark-theme .calc-flow-edit-dialog .el-select__wrapper),
:global(.dark-theme .calc-flow-edit-dialog .el-textarea__inner) {
  background: rgba(3, 12, 22, 0.82);
  border-color: rgba(71, 153, 191, 0.26);
  box-shadow: 0 0 0 1px rgba(71, 153, 191, 0.26) inset;
  color: #d8e7f5;
}
:global(.dark-theme .calc-flow-edit-dialog .el-input__inner),
:global(.dark-theme .calc-flow-edit-dialog .el-select__placeholder),
:global(.dark-theme .calc-flow-edit-dialog .el-textarea__inner) {
  color: #d8e7f5;
}
:global(.dark-theme .calc-flow-edit-dialog .el-input-number__decrease),
:global(.dark-theme .calc-flow-edit-dialog .el-input-number__increase) {
  background: rgba(8, 23, 39, 0.96);
  border-color: rgba(71, 153, 191, 0.26);
  color: #9fb6cb;
}
:global(.dark-theme) .stage-config-panel {
  background: rgba(5, 13, 24, 0.88);
  border-color: rgba(0, 242, 255, 0.18);
  box-shadow: inset 0 0 0 1px rgba(0, 242, 255, 0.05), 0 12px 32px rgba(0, 0, 0, 0.22);
}
:global(.dark-theme) .score-level-config {
  border-top-color: rgba(91, 141, 180, 0.34);
}
:global(.dark-theme) .section-title {
  color: #d8e7f5 !important;
}
.image-upload-wrapper {
  padding: 5px 0;
  :deep(.el-upload--picture-card) {
    width: 80px;
    height: 80px;
    line-height: 80px;
  }
  :deep(.el-upload-list--picture-card .el-upload-list__item) {
    width: 80px;
    height: 80px;
  }
  :deep(.el-upload-list--picture-card .el-upload-list__item-actions) {
    font-size: 16px;
  }
}
</style>
