<template>
  <div class="app-container flow-runner-test">
    <!-- 页面说明 -->
    <el-alert type="info" :closable="false" show-icon style="margin-bottom: 18px;">
      <template #title>
        <strong>测试页面</strong>：模拟「评估工程管理」在完成需求分析 / 指标体系构建 / 数据采集后进入"评估计算"步骤的交互。用于验证流程向导的步骤交互，也方便外部前端系统集成后续步骤页面。
      </template>
    </el-alert>

    <!-- 上游流程摘要（模拟） -->
    <el-card class="pipeline-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">评估工程进度（模拟）</span>
          <el-tag size="small" type="success">前序流程已完成</el-tag>
        </div>
      </template>
      <div class="pipeline">
        <div class="pipe-item done">
          <div class="pipe-badge">
            <el-icon><Check /></el-icon>
          </div>
          <div class="pipe-label">需求分析</div>
          <div class="pipe-desc">demo-req-001</div>
        </div>
        <div class="pipe-arrow"><el-icon><Right /></el-icon></div>
        <div class="pipe-item done">
          <div class="pipe-badge">
            <el-icon><Check /></el-icon>
          </div>
          <div class="pipe-label">指标体系构建</div>
          <div class="pipe-desc">{{ selectedIndicatorLabel || '待选择' }}</div>
        </div>
        <div class="pipe-arrow"><el-icon><Right /></el-icon></div>
        <div class="pipe-item done">
          <div class="pipe-badge">
            <el-icon><Check /></el-icon>
          </div>
          <div class="pipe-label">数据采集</div>
          <div class="pipe-desc">已对接</div>
        </div>
        <div class="pipe-arrow"><el-icon><Right /></el-icon></div>
        <div class="pipe-item active">
          <div class="pipe-badge">
            <el-icon><VideoPlay /></el-icon>
          </div>
          <div class="pipe-label">评估计算</div>
          <div class="pipe-desc">当前步骤</div>
        </div>
        <div class="pipe-arrow"><el-icon><Right /></el-icon></div>
        <div class="pipe-item todo">
          <div class="pipe-badge">
            <el-icon><DocumentRemove /></el-icon>
          </div>
          <div class="pipe-label">结果归档</div>
          <div class="pipe-desc">—</div>
        </div>
      </div>
    </el-card>

    <!-- 选择模板 + 指标体系 -->
    <el-card class="select-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">选择计算流程模板</span>
          <el-switch
            v-model="useInlineEmbed"
            active-text="内嵌"
            inactive-text="跳转"
            inline-prompt
            style="margin-left: auto;"
          />
        </div>
      </template>
      <el-form :model="form" label-width="110px" :rules="rules" ref="formRef">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="需求ID">
              <el-input v-model="form.requirementId" placeholder="可选，输入需求ID自动关联指标体系" clearable @change="onRequirementIdChange" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="流程模板" prop="templateId">
              <el-select
                v-model="form.templateId"
                placeholder="请选择已发布的流程模板"
                filterable
                style="width: 100%;"
                @change="onTemplateChange"
              >
                <el-option
                  v-for="t in templateOptions"
                  :key="t.id"
                  :label="t.label"
                  :value="t.id"
                >
                  <span>{{ t.templateName }}</span>
                  <span class="opt-sub">{{ taskTypeLabelMap[t.taskType] || t.taskType || '' }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="!form.requirementId">
            <el-form-item label="指标体系" prop="indicatorSystemId">
              <el-select
                v-model="form.indicatorSystemId"
                placeholder="请选择本次计算使用的指标体系"
                filterable
                remote
                :remote-method="searchIndicatorSystem"
                :loading="indicatorLoading"
                style="width: 100%;"
                @change="onIndicatorChange"
              >
                <el-option
                  v-for="s in indicatorOptions"
                  :key="s.indicatorSystemId || s.id"
                  :label="s.indicatorSystemName || s.systemName"
                  :value="s.indicatorSystemId || s.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务名称">
              <el-input v-model="form.taskName" placeholder="可选，不填则自动生成" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据采集方案">
              <el-input v-model="form.dataPlanName" placeholder="(模拟) 已自动带入上游方案" disabled />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div class="action-bar">
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" icon="VideoPlay" :disabled="!form.templateId" @click="handleEnterRunner">
          进入计算向导
        </el-button>
      </div>
    </el-card>

    <!-- 集成提示 -->
    <el-card class="integration-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">外部系统集成说明</span>
        </div>
      </template>
      <div class="integration-tip">
        <p><strong>方式 A · 路由跳转：</strong>外部系统通过路由跳转进入向导页。</p>
        <pre class="code-block">router.push({
  path: '/zhpg/zhfx/flowRunner',
  query: {
    templateId: {{ form.templateId || 'xxx' }},
    indicatorSystemId: {{ form.indicatorSystemId || 'xxx' }},
    taskName: '{{ form.taskName || '可选' }}'
  }
})</pre>
        <p><strong>方式 B · 组件内嵌：</strong>将 <code>FlowRunner</code> 作为子组件挂载，由外层传入 props。</p>
        <pre class="code-block">&lt;FlowRunner
  :template-id="templateId"
  :indicator-system-id="indicatorSystemId"
  :task-name-prop="taskName"
  @finished="onFlowFinished"
  @exit="onFlowExit"
/&gt;</pre>
        <p class="tip-small">
          · 传入 <code>indicatorSystemId</code> → 执行模式（含权重计算步骤、真实发起计算与报告生成）<br />
          · 不传 <code>indicatorSystemId</code> → 设计模式（仅 3 步骤，不含权重计算，不触发计算）
        </p>
      </div>
    </el-card>

    <!-- 内嵌模式下：在同页面渲染向导 -->
    <el-dialog
      v-model="embedVisible"
      fullscreen
      destroy-on-close
      append-to-body
      :show-close="false"
      class="flow-runner-embed-dialog"
    >
      <FlowRunner
        :template-id="form.templateId"
        :indicator-system-id="form.indicatorSystemId"
        :requirement-id-prop="form.requirementId"
        :task-name-prop="form.taskName"
        @exit="embedVisible = false"
        @finished="onEmbedFinished"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Right, VideoPlay, DocumentRemove } from '@element-plus/icons-vue'
import { listCalcFlow } from '@/api/zhpg/calcFlow'
import { selectIndicatorSystem } from '@/api/zhpg/indicatorSystem'
import FlowRunner from '@/views/zhpg/zhfx/flowRunner/index.vue'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance()

const taskTypeLabelMap = {
  PERFORMANCE: '性能指标评估',
  EQUIP_EFFECTIVENESS: '装备作战效能评估',
  SYSTEM_CONTRIBUTION: '体系贡献率评估',
  TASK_SATISFACTION: '作战任务满足度评估',
  SYSTEM_TASK: '体系级任务评估',
  EXERCISE_TRAINING: '演习演训任务评估'
}

const useInlineEmbed = ref(false)
const embedVisible = ref(false)

const form = reactive({
  requirementId: route.query.requirementId || '',
  templateId: null,
  indicatorSystemId: null,
  taskName: '',
  dataPlanName: 'demo-data-plan-001'
})
const rules = {
  templateId: [{ required: true, message: '请选择流程模板', trigger: 'change' }]
}

const templateOptions = ref([])
const indicatorOptions = ref([])
const indicatorLoading = ref(false)


const selectedIndicatorLabel = computed(() => {
  const hit = indicatorOptions.value.find(s => (s.indicatorSystemId || s.id) === form.indicatorSystemId)
  return hit ? (hit.indicatorSystemName || hit.systemName) : ''
})

onMounted(() => {
  loadTemplates()
  searchIndicatorSystem('')
})

async function loadTemplates() {
  try {
    const res = await listCalcFlow({ pageNum: 1, pageSize: 100, status: 'PUBLISHED' })
    const list = res.rows || res.data || []
    templateOptions.value = list.length
      ? list.map(t => ({ ...t, label: `${t.templateName} · ${taskTypeLabelMap[t.taskType] || ''}` }))
      : mockTemplates()
  } catch {
    templateOptions.value = mockTemplates()
  }
}

async function searchIndicatorSystem(keyword) {
  indicatorLoading.value = true
  try {
    const res = await selectIndicatorSystem({
      keyword,
      requirementId: form.requirementId || undefined
    })
    const list = res.data || res.rows || res || []
    indicatorOptions.value = Array.isArray(list) && list.length ? list : mockIndicatorSystems()
  } catch {
    indicatorOptions.value = mockIndicatorSystems()
  } finally {
    indicatorLoading.value = false
  }
}

function mockTemplates() {
  return [
    { id: 'mock-tpl-1', templateName: '(模拟)装备作战效能评估-标准流程', taskType: 'EQUIP_EFFECTIVENESS', label: '(模拟)装备作战效能评估-标准流程 · 装备作战效能评估' },
    { id: 'mock-tpl-2', templateName: '(模拟)体系贡献率评估-三级指标', taskType: 'SYSTEM_CONTRIBUTION', label: '(模拟)体系贡献率评估-三级指标 · 体系贡献率评估' },
    { id: 'mock-tpl-3', templateName: '(模拟)演习演训任务评估流程', taskType: 'EXERCISE_TRAINING', label: '(模拟)演习演训任务评估流程 · 演习演训任务评估' }
  ]
}
function mockIndicatorSystems() {
  return [
    { indicatorSystemId: 'mock-is-1', indicatorSystemName: '(模拟)空间态势感知装备指标体系 v2.1' },
    { indicatorSystemId: 'mock-is-2', indicatorSystemName: '(模拟)通信对抗装备指标体系 v1.3' },
    { indicatorSystemId: 'mock-is-3', indicatorSystemName: '(模拟)海基航天装备指标体系 v1.0' }
  ]
}

function onTemplateChange() { /* 可按模板类型过滤指标体系 */ }
function onIndicatorChange() { /* 可触发联动 */ }
function onRequirementIdChange() {
  form.indicatorSystemId = null
  searchIndicatorSystem('')
}

function handleReset() {
  form.templateId = null
  form.indicatorSystemId = null
  form.taskName = ''
}

function handleEnterRunner() {
  if (!form.templateId) {
    ElMessage.warning('请选择流程模板')
    return
  }
  if (useInlineEmbed.value) {
    embedVisible.value = true
    return
  }
  router.push({
    path: '/zhpg/zhfx/flowRunner',
    query: {
      templateId: form.templateId,
      indicatorSystemId: form.requirementId ? undefined : (form.indicatorSystemId || undefined),
      requirementId: form.requirementId || undefined,
      taskName: form.taskName || undefined
    }
  })
}

function onEmbedFinished(payload) {
  ElMessage.success('流程已完成')
  embedVisible.value = false
  console.log('[FlowRunnerTest] finished payload:', payload)
}
</script>

<style scoped>
.flow-runner-test { padding: 16px 20px; }
.pipeline-card,
.select-card,
.integration-card { margin-bottom: 18px; }
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.card-title { font-size: 15px; font-weight: 600; color: #303133; }

.pipeline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 4px;
}
.pipe-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 110px;
  gap: 6px;
}
.pipe-badge {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
}
.pipe-item.done .pipe-badge { background: #67c23a; }
.pipe-item.active .pipe-badge { background: #409eff; box-shadow: 0 0 0 4px rgba(64,158,255,0.2); }
.pipe-item.todo .pipe-badge { background: #c0c4cc; }
.pipe-label { font-size: 13px; font-weight: 500; color: #303133; }
.pipe-desc { font-size: 12px; color: #909399; }
.pipe-arrow { color: #c0c4cc; font-size: 18px; }

.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 4px;
}
.opt-sub { margin-left: 10px; color: #909399; font-size: 12px; }

.integration-tip { font-size: 13px; color: #606266; line-height: 1.8; }
.code-block {
  background: #f5f7fa;
  border-left: 3px solid #409eff;
  padding: 10px 12px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  line-height: 1.6;
  margin: 8px 0;
  overflow-x: auto;
}
.tip-small { font-size: 12px; color: #909399; }
code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
}

:deep(.flow-runner-embed-dialog .el-dialog__header) { display: none; }
:deep(.flow-runner-embed-dialog .el-dialog__body) { padding: 0; }
</style>
