<template>
  <el-dialog
    v-model="open"
    :title="dialogTitle"
    width="min(1280px, 96vw)"
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
    class="zhpg-weight-workbench-dialog"
    modal-class="zhpg-weight-workbench-modal"
    @opened="onOpened"
  >
    <div v-if="loading" class="state-block">
      <el-icon class="is-loading state-icon"><Loading /></el-icon>
      <span>正在加载指标体系...</span>
    </div>
    <div v-else-if="loadError" class="state-block error">
      <el-icon class="state-icon"><WarningFilled /></el-icon>
      <span>{{ loadError }}</span>
    </div>
    <div v-else-if="!treeData.length" class="state-block">
      <el-empty description="指标树为空，请先在「编辑」中构建指标树" />
    </div>
    <template v-else>
      <div class="header-bar">
        <div class="header-info">
          <div class="header-title">{{ systemName || '指标体系' }}</div>
          <div class="header-sub">
            算法依据来自指标体系上每个父节点的 <code>weightAssignAlgorithm</code> 字段；可在「编辑 → 配置体系全局 / 配置本节点算法」中事先配好后再来调优。
          </div>
        </div>
        <div class="header-actions">
          <el-button
            type="primary"
            icon="DataAnalysis"
            :loading="computeRunning"
            :disabled="!systemId || computeRunning || saving"
            @click="runSmartCompute"
          >一键计算权重</el-button>
        </div>
      </div>

      <div class="workbench-body">
        <div class="wb-left">
          <div class="wb-section-title">结构树（点选父节点进行微调）</div>
          <el-scrollbar max-height="520px">
            <el-tree
              ref="treeRef"
              :data="treeData"
              node-key="uid"
              :props="{ label: 'label', children: 'children' }"
              default-expand-all
              highlight-current
              :expand-on-click-node="false"
              :current-node-key="selectedParentUid"
              @node-click="onTreePick"
            >
              <template #default="{ node, data }">
                <div class="tree-node-row">
                  <span class="tree-node-label">{{ data.label }}</span>
                  <span v-if="data.children?.length" class="tree-node-w">
                    {{ formatWeight(data.weight) }}
                  </span>
                </div>
              </template>
            </el-tree>
          </el-scrollbar>
        </div>
        <div class="wb-right">
          <div class="wb-right-head">
            <div>
              <span class="wb-right-label">当前父节点</span>
              <span class="wb-right-name">{{ parentTitle }}</span>
            </div>
            <div v-if="parentAlgInfo" class="wb-right-alg">
              <el-tag type="info" effect="plain">本节点算法：{{ parentAlgInfo }}</el-tag>
            </div>
          </div>
          <div v-if="currentSumText" class="wb-sum-line" :class="{ 'is-ok': lastValidateOk === true, 'is-bad': lastValidateOk === false }">
            {{ currentSumText }}
          </div>
          <div v-if="!childrenList.length" class="wb-no-children">该节点无子节点，请在左侧选择有子级的父节点</div>
          <el-row v-else :gutter="12" class="wb-cards">
            <el-col v-for="c in childrenList" :key="c.uid" :xs="24" :sm="12" :md="8">
              <div class="wb-card">
                <div class="wb-card-title">{{ c.label || '未命名' }}</div>
                <el-input-number
                  v-model="c.weight"
                  :min="0"
                  :max="1"
                  :step="0.0001"
                  :precision="8"
                  controls-position="right"
                  class="wb-num"
                  @change="lastEditedUid = c.uid"
                />
              </div>
            </el-col>
          </el-row>
          <div class="wb-tools">
            <el-button :disabled="!childrenList.length" @click="averageAmongSiblings">一键平均</el-button>
            <el-button :disabled="!childrenList.length" @click="validateCurrent">校验（当前父节点）</el-button>
          </div>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="wb-footer">
        <el-button @click="open = false">关 闭</el-button>
        <el-button
          type="primary"
          :loading="saving"
          :disabled="!systemId || !treeData.length || computeRunning"
          @click="saveManualTune"
        >保存手动微调</el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 权重计算进度（独立子 dialog） -->
  <el-dialog
    v-model="computeProgressVisible"
    title="综合权重计算"
    width="min(520px, 90vw)"
    append-to-body
    align-center
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="computeProgressDone"
    destroy-on-close
    class="zhpg-compute-weight-progress-dialog"
    modal-class="zhpg-weight-workbench-modal"
    @closed="onComputeProgressClosed"
  >
    <div class="zhpg-owp-body">
      <el-steps direction="vertical" :active="computeStepActive" finish-status="success">
        <el-step title="校验与准备" description="确认指标体系状态，并解析当前指标树结构" />
        <el-step title="组装层级数据" description="识别各节点赋权模式（主观/客观）并准备参数" />
        <el-step title="智能分发计算" description="根据配置自动分派主客观算法；客观部分涉及样本模拟计算" />
        <el-step title="回写与刷新" description="执行层内归一化，写回指标体系，并刷新本工作台" />
      </el-steps>
      <div v-if="computeRunning" class="zhpg-owp-status">
        <el-icon class="zhpg-owp-spin is-loading"><Loading /></el-icon>
        <span>正在计算，请稍候...</span>
      </div>
    </div>
    <template v-if="computeProgressDone" #footer>
      <el-button type="primary" @click="computeProgressVisible = false">知道了</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, WarningFilled } from '@element-plus/icons-vue'
import {
  getIndicatorSystem,
  applyIndicatorTreeWeights,
  computeWeightsSmart
} from '@/api/zhpg/indicatorSystem'
import { parseIndicatorTreeToForest } from '@/utils/zhpg/zhpgIndicatorTreeJson'
import { getSubjectiveAlgLabel } from './subjective/subjectiveTreeShared'
import { fetchZhpgWeightAssignSelectOptions } from '@/utils/zhpg/zhpgWeightAssignAlgorithms'

const SUM_EPS = 1e-5

const open = defineModel('modelValue', { type: Boolean, default: false })

const props = defineProps({
  systemId: { type: [Number, String], default: null },
  systemName: { type: String, default: '' }
})

const emit = defineEmits(['updated'])

const loading = ref(false)
const loadError = ref('')
const treeData = ref([])
const selectedParent = ref(null)
const selectedParentUid = ref('')
const lastValidateOk = ref(null)
const lastEditedUid = ref('')
const computeRunning = ref(false)
const saving = ref(false)

const weightAssignOptions = ref([])

onMounted(() => {
  fetchZhpgWeightAssignSelectOptions().then(opts => {
    weightAssignOptions.value = opts
  })
})

// 计算进度
const computeProgressVisible = ref(false)
const computeStepActive = ref(0)
const computeProgressDone = ref(false)
const computeStepTimers = []

const dialogTitle = computed(() =>
  props.systemName ? `权重调优 — ${props.systemName}` : '权重调优'
)

const parentTitle = computed(() => selectedParent.value?.label || '—')
const childrenList = computed(() => {
  const p = selectedParent.value
  return p?.children?.length ? p.children : []
})
const currentSumText = computed(() => {
  if (!childrenList.value.length) return ''
  const sum = sumChildWeights(childrenList.value)
  return `当前子节点权重之和：${sum.toFixed(8)}（期望 1）`
})

const parentAlgInfo = computed(() => {
  const v = selectedParent.value?.weightAssignAlgorithm
  if (!v) return ''
  const s = String(v)
  const hit = weightAssignOptions.value.find(o => String(o.value) === s)
  if (hit) return hit.label
  if (typeof v === 'string' && !/^\d+$/.test(v)) {
    return getSubjectiveAlgLabel(v)
  }
  return `客观算法 #${v}`
})

function numWeight(w) {
  if (w == null || w === '') return 0
  const n = Number(w)
  return Number.isFinite(n) ? n : 0
}
function sumChildWeights(children) {
  return (children || []).reduce((s, c) => s + numWeight(c.weight), 0)
}
function firstParentInTree(nodes) {
  if (!nodes?.length) return null
  const q = [...nodes]
  while (q.length) {
    const n = q.shift()
    const ch = n.children
    if (ch?.length) return n
    if (ch) for (const c of ch) q.push(c)
  }
  return nodes[0]
}
function formatWeight(w) {
  const n = numWeight(w)
  return n === 0 ? '—' : n.toFixed(4)
}

// ================ 加载 ================
async function onOpened() {
  await reloadTree()
}

async function reloadTree() {
  if (!props.systemId) {
    loadError.value = '未提供指标体系 ID'
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const res = await getIndicatorSystem(props.systemId)
    const sys = res?.data || {}
    const treeJson = sys.indicatorTreeWeight || sys.indicatorTree || ''
    treeData.value = parseIndicatorTreeToForest(treeJson)
    selectedParent.value = firstParentInTree(treeData.value)
    selectedParentUid.value = selectedParent.value?.uid || ''
    lastEditedUid.value = ''
    lastValidateOk.value = null
  } catch (e) {
    loadError.value = '加载指标体系失败：' + (e?.message || e)
  } finally {
    loading.value = false
  }
}

function onTreePick(data) {
  selectedParent.value = data
  selectedParentUid.value = data?.uid || ''
  lastEditedUid.value = ''
  lastValidateOk.value = null
}

// ================ 手动微调 ================
function isUnsetWeight(w) {
  if (w == null || w === '') return true
  if (typeof w === 'number' && !Number.isFinite(w)) return true
  return false
}

function averageAmongSiblings() {
  const list = childrenList.value
  if (!list.length) return
  const PREC = 1e8
  const n = list.length
  const unsetIdx = []
  let sumFixed = 0
  for (let i = 0; i < n; i++) {
    if (isUnsetWeight(list[i].weight)) unsetIdx.push(i)
    else sumFixed += Number(list[i].weight)
  }
  if (unsetIdx.length === n) {
    const base = Math.round((1 / n) * PREC) / PREC
    let acc = 0
    for (let i = 0; i < n - 1; i++) { list[i].weight = base; acc += base }
    list[n - 1].weight = Math.round((1 - acc) * PREC) / PREC
    lastValidateOk.value = true
    ElMessage.success('已均分权重')
    return
  }
  if (unsetIdx.length > 0) {
    const remainder = 1 - sumFixed
    if (remainder < -SUM_EPS) {
      ElMessage.warning(`已填权重之和为 ${sumFixed.toFixed(8)}，超过 1`)
      return
    }
    const m = unsetIdx.length
    const base = Math.round((remainder / m) * PREC) / PREC
    let acc = 0
    for (let k = 0; k < m - 1; k++) { list[unsetIdx[k]].weight = base; acc += base }
    list[unsetIdx[m - 1]].weight = Math.round((remainder - acc) * PREC) / PREC
    lastValidateOk.value = true
    ElMessage.success('已将剩余权重均分到未填子节点')
    return
  }
  if (Math.abs(sumFixed - 1) <= SUM_EPS) {
    lastValidateOk.value = true
    ElMessage.info('当前子节点权重之和已为 1')
    return
  }
  // 子项全部已填但和非 1：按等比缩放到 1
  if (sumFixed > 0) {
    const k = 1 / sumFixed
    for (const c of list) c.weight = Math.round(Number(c.weight) * k * PREC) / PREC
    lastValidateOk.value = true
    ElMessage.success(`原和 ${sumFixed.toFixed(8)} 已等比缩放至 1`)
  }
}

function validateCurrent() {
  const list = childrenList.value
  if (!list.length) return
  const sum = sumChildWeights(list)
  const ok = Math.abs(sum - 1) <= SUM_EPS
  lastValidateOk.value = ok
  if (ok) ElMessage.success('校验通过：当前父节点子权重之和为 1')
  else ElMessage.warning(`校验未通过：和为 ${sum.toFixed(8)}`)
}

function validateWholeTree(nodes) {
  if (!nodes?.length) return { ok: true, msg: '' }
  for (const n of nodes) {
    const ch = n.children
    if (ch?.length) {
      const sum = sumChildWeights(ch)
      if (Math.abs(sum - 1) > SUM_EPS) {
        return { ok: false, msg: `节点「${n.label || n.uid || '?'}」下子权重之和为 ${sum.toFixed(6)}，应为 1` }
      }
      const inner = validateWholeTree(ch)
      if (!inner.ok) return inner
    }
  }
  return { ok: true, msg: '' }
}

function buildTreePayload() {
  const data = treeData.value
  if (!Array.isArray(data) || data.length === 0) return { treeData: [] }
  if (data.length === 1) return { treeData: data[0] }
  return { treeData: data }
}

// ================ 三个动作 ================
async function saveManualTune() {
  const check = validateWholeTree(treeData.value)
  if (!check.ok) { ElMessage.warning(check.msg); return }
  saving.value = true
  try {
    const res = await applyIndicatorTreeWeights({
      systemId: props.systemId,
      indicatorTree: buildTreePayload()
    })
    ElMessage.success(res?.msg || '已保存手动微调')
    // 用后端归一化后的树刷新本地视图
    const tw = res?.data?.indicatorTreeWeight
    if (tw) {
      treeData.value = parseIndicatorTreeToForest(tw)
      pickFirstParentAgain()
    }
    emit('updated', tw)
  } catch (e) {
    ElMessage.error('保存失败：' + (e?.message || e))
  } finally {
    saving.value = false
  }
}

async function runSmartCompute() {
  if (!props.systemId) return
  clearComputeStepTimers()
  computeStepActive.value = 0
  computeProgressDone.value = false
  computeProgressVisible.value = true
  computeRunning.value = true

  computeStepTimers.push(setTimeout(() => (computeStepActive.value = 1), 320))
  computeStepTimers.push(setTimeout(() => (computeStepActive.value = 2), 880))

  try {
    const res = await computeWeightsSmart(props.systemId, { persist: true, mockSampleRows: 8 })
    clearComputeStepTimers()
    computeStepActive.value = 3
    const payload = res?.data || {}
    if (payload.indicatorTreeWeight) {
      treeData.value = parseIndicatorTreeToForest(payload.indicatorTreeWeight)
      pickFirstParentAgain()
    }
    computeRunning.value = false
    computeProgressDone.value = true
    ElMessage.success(res?.msg || '智能赋权完成')
    if (payload.hint) ElMessage.info(String(payload.hint))
    emit('updated', payload.indicatorTreeWeight)
  } catch (e) {
    clearComputeStepTimers()
    computeProgressVisible.value = false
    computeRunning.value = false
    computeStepActive.value = 0
    ElMessage.error('权重计算失败：' + (e?.message || e))
  }
}

function pickFirstParentAgain() {
  nextTick(() => {
    selectedParent.value = firstParentInTree(treeData.value)
    selectedParentUid.value = selectedParent.value?.uid || ''
  })
}

function clearComputeStepTimers() {
  while (computeStepTimers.length) {
    clearTimeout(computeStepTimers.pop())
  }
}

function onComputeProgressClosed() {
  clearComputeStepTimers()
  computeRunning.value = false
  computeProgressDone.value = false
  computeStepActive.value = 0
}
</script>

<style scoped>
.zhpg-weight-workbench-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}
.state-block {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 280px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
.state-block.error {
  color: var(--el-color-warning);
}
.state-icon {
  font-size: 24px;
}
.header-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 8px 12px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 12px;
}
.header-info {
  flex: 1;
  min-width: 0;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}
.header-sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
.header-sub code {
  background: var(--el-fill-color-light);
  padding: 1px 4px;
  border-radius: 4px;
  font-size: 11px;
}
.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.workbench-body {
  display: flex;
  gap: 16px;
  min-height: 360px;
}
.wb-left {
  flex: 0 0 320px;
  max-width: 38%;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 10px;
  background: var(--el-fill-color-blank);
}
.wb-section-title {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}
.tree-node-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex: 1;
  font-size: 13px;
  padding-right: 8px;
}
.tree-node-w {
  color: var(--el-color-primary);
  font-weight: 600;
  font-size: 12px;
}
.wb-right {
  flex: 1;
  min-width: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px;
  background: var(--el-fill-color-blank);
}
.wb-right-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}
.wb-right-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-right: 6px;
}
.wb-right-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.wb-right-alg :deep(.el-tag) {
  font-size: 12px;
}
.wb-sum-line {
  font-size: 13px;
  margin-bottom: 10px;
  padding: 6px 10px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
}
.wb-sum-line.is-ok {
  color: var(--el-color-success);
  background: var(--el-color-success-light-9);
}
.wb-sum-line.is-bad {
  color: var(--el-color-warning);
  background: var(--el-color-warning-light-9);
}
.wb-no-children {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  padding: 24px 8px;
}
.wb-cards { margin-top: 4px; }
.wb-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 12px;
  background: var(--el-fill-color-blank);
}
.wb-card-title {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.wb-num { width: 100%; }
.wb-tools {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed var(--el-border-color-lighter);
}
.wb-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  width: 100%;
}
.zhpg-owp-body {
  padding: 10px 0;
}
.zhpg-owp-status {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.zhpg-owp-spin {
  font-size: 16px;
}

:global(html.dark .zhpg-weight-workbench-modal),
:global(body[data-theme="dark"] .zhpg-weight-workbench-modal),
:global(.dark-theme .zhpg-weight-workbench-modal) {
  background-color: rgba(0, 5, 12, 0.78);
  backdrop-filter: blur(1px);
}

:global(html.dark .zhpg-weight-workbench-dialog),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog),
:global(.dark-theme .zhpg-weight-workbench-dialog),
:global(html.dark .zhpg-objective-weight-progress-dialog),
:global(body[data-theme="dark"] .zhpg-objective-weight-progress-dialog),
:global(.dark-theme .zhpg-objective-weight-progress-dialog) {
  --wb-dialog-bg: #071426;
  --wb-dialog-bg-soft: #0a1a30;
  --wb-dialog-bg-panel: #08192c;
  --wb-dialog-bg-card: #0b2038;
  --wb-dialog-border: rgba(0, 194, 255, 0.22);
  --wb-dialog-border-strong: rgba(0, 224, 255, 0.34);
  --wb-dialog-text: #dbeafe;
  --wb-dialog-text-muted: #8fb0c8;
  --wb-dialog-accent: #00d9ff;
  --wb-dialog-shadow: 0 28px 88px rgba(0, 0, 0, 0.68);
  color: var(--wb-dialog-text);
  background: var(--wb-dialog-bg);
  border: 1px solid var(--wb-dialog-border);
  box-shadow: var(--wb-dialog-shadow);
}

:global(html.dark .zhpg-weight-workbench-dialog .el-dialog__header),
:global(html.dark .zhpg-weight-workbench-dialog .el-dialog__body),
:global(html.dark .zhpg-weight-workbench-dialog .el-dialog__footer),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .el-dialog__header),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .el-dialog__body),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .el-dialog__footer),
:global(.dark-theme .zhpg-weight-workbench-dialog .el-dialog__header),
:global(.dark-theme .zhpg-weight-workbench-dialog .el-dialog__body),
:global(.dark-theme .zhpg-weight-workbench-dialog .el-dialog__footer),
:global(html.dark .zhpg-objective-weight-progress-dialog .el-dialog__header),
:global(html.dark .zhpg-objective-weight-progress-dialog .el-dialog__body),
:global(html.dark .zhpg-objective-weight-progress-dialog .el-dialog__footer),
:global(body[data-theme="dark"] .zhpg-objective-weight-progress-dialog .el-dialog__header),
:global(body[data-theme="dark"] .zhpg-objective-weight-progress-dialog .el-dialog__body),
:global(body[data-theme="dark"] .zhpg-objective-weight-progress-dialog .el-dialog__footer),
:global(.dark-theme .zhpg-objective-weight-progress-dialog .el-dialog__header),
:global(.dark-theme .zhpg-objective-weight-progress-dialog .el-dialog__body),
:global(.dark-theme .zhpg-objective-weight-progress-dialog .el-dialog__footer) {
  background: var(--wb-dialog-bg);
}

:global(html.dark .zhpg-weight-workbench-dialog .el-dialog__title),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .el-dialog__title),
:global(.dark-theme .zhpg-weight-workbench-dialog .el-dialog__title),
:global(html.dark .zhpg-objective-weight-progress-dialog .el-dialog__title),
:global(body[data-theme="dark"] .zhpg-objective-weight-progress-dialog .el-dialog__title),
:global(.dark-theme .zhpg-objective-weight-progress-dialog .el-dialog__title) {
  color: var(--wb-dialog-text);
}

:global(html.dark .zhpg-weight-workbench-dialog .el-dialog__headerbtn .el-dialog__close),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .el-dialog__headerbtn .el-dialog__close),
:global(.dark-theme .zhpg-weight-workbench-dialog .el-dialog__headerbtn .el-dialog__close),
:global(html.dark .zhpg-objective-weight-progress-dialog .el-dialog__headerbtn .el-dialog__close),
:global(body[data-theme="dark"] .zhpg-objective-weight-progress-dialog .el-dialog__headerbtn .el-dialog__close),
:global(.dark-theme .zhpg-objective-weight-progress-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: #6bdcff;
}

:global(html.dark .zhpg-weight-workbench-dialog .header-bar),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .header-bar),
:global(.dark-theme .zhpg-weight-workbench-dialog .header-bar) {
  border-bottom-color: var(--wb-dialog-border);
  background: linear-gradient(180deg, rgba(10, 26, 48, 0.96), rgba(7, 20, 38, 0.98));
}

:global(html.dark .zhpg-weight-workbench-dialog .header-title),
:global(html.dark .zhpg-weight-workbench-dialog .wb-right-name),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .header-title),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .wb-right-name),
:global(.dark-theme .zhpg-weight-workbench-dialog .header-title),
:global(.dark-theme .zhpg-weight-workbench-dialog .wb-right-name) {
  color: var(--wb-dialog-text);
}

:global(html.dark .zhpg-weight-workbench-dialog .header-sub),
:global(html.dark .zhpg-weight-workbench-dialog .wb-section-title),
:global(html.dark .zhpg-weight-workbench-dialog .wb-right-label),
:global(html.dark .zhpg-weight-workbench-dialog .state-block),
:global(html.dark .zhpg-objective-weight-progress-dialog .zhpg-owp-status),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .header-sub),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .wb-section-title),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .wb-right-label),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .state-block),
:global(body[data-theme="dark"] .zhpg-objective-weight-progress-dialog .zhpg-owp-status),
:global(.dark-theme .zhpg-weight-workbench-dialog .header-sub),
:global(.dark-theme .zhpg-weight-workbench-dialog .wb-section-title),
:global(.dark-theme .zhpg-weight-workbench-dialog .wb-right-label),
:global(.dark-theme .zhpg-weight-workbench-dialog .state-block),
:global(.dark-theme .zhpg-objective-weight-progress-dialog .zhpg-owp-status) {
  color: var(--wb-dialog-text-muted);
}

:global(html.dark .zhpg-weight-workbench-dialog .header-sub code),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .header-sub code),
:global(.dark-theme .zhpg-weight-workbench-dialog .header-sub code) {
  color: #7ee7ff;
  background: rgba(0, 217, 255, 0.12);
  border: 1px solid rgba(0, 217, 255, 0.22);
}

:global(html.dark .zhpg-weight-workbench-dialog .wb-left),
:global(html.dark .zhpg-weight-workbench-dialog .wb-right),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .wb-left),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .wb-right),
:global(.dark-theme .zhpg-weight-workbench-dialog .wb-left),
:global(.dark-theme .zhpg-weight-workbench-dialog .wb-right) {
  background: var(--wb-dialog-bg-panel);
  border-color: var(--wb-dialog-border);
}

:global(html.dark .zhpg-weight-workbench-dialog .wb-card),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .wb-card),
:global(.dark-theme .zhpg-weight-workbench-dialog .wb-card) {
  background: var(--wb-dialog-bg-card);
  border-color: rgba(0, 194, 255, 0.2);
}

:global(html.dark .zhpg-weight-workbench-dialog .wb-card-title),
:global(html.dark .zhpg-weight-workbench-dialog .tree-node-row),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .wb-card-title),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .tree-node-row),
:global(.dark-theme .zhpg-weight-workbench-dialog .wb-card-title),
:global(.dark-theme .zhpg-weight-workbench-dialog .tree-node-row) {
  color: var(--wb-dialog-text);
}

:global(html.dark .zhpg-weight-workbench-dialog .tree-node-w),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .tree-node-w),
:global(.dark-theme .zhpg-weight-workbench-dialog .tree-node-w) {
  color: var(--wb-dialog-accent);
}

:global(html.dark .zhpg-weight-workbench-dialog .el-tree),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .el-tree),
:global(.dark-theme .zhpg-weight-workbench-dialog .el-tree) {
  --el-tree-bg-color: var(--wb-dialog-bg-panel);
  --el-tree-node-hover-bg-color: rgba(0, 217, 255, 0.1);
  background: var(--wb-dialog-bg-panel);
  color: var(--wb-dialog-text);
}

:global(html.dark .zhpg-weight-workbench-dialog .el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content),
:global(.dark-theme .zhpg-weight-workbench-dialog .el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
  background: rgba(219, 234, 254, 0.13);
}

:global(html.dark .zhpg-weight-workbench-dialog .wb-sum-line),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .wb-sum-line),
:global(.dark-theme .zhpg-weight-workbench-dialog .wb-sum-line) {
  background: var(--wb-dialog-bg-soft);
  border: 1px solid rgba(0, 194, 255, 0.16);
  color: var(--wb-dialog-text);
}

:global(html.dark .zhpg-weight-workbench-dialog .wb-sum-line.is-ok),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .wb-sum-line.is-ok),
:global(.dark-theme .zhpg-weight-workbench-dialog .wb-sum-line.is-ok) {
  color: #7dd3fc;
  background: rgba(14, 116, 144, 0.24);
}

:global(html.dark .zhpg-weight-workbench-dialog .wb-sum-line.is-bad),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .wb-sum-line.is-bad),
:global(.dark-theme .zhpg-weight-workbench-dialog .wb-sum-line.is-bad) {
  color: #fbbf24;
  background: rgba(146, 64, 14, 0.22);
}

:global(html.dark .zhpg-weight-workbench-dialog .wb-tools),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .wb-tools),
:global(.dark-theme .zhpg-weight-workbench-dialog .wb-tools) {
  border-top-color: var(--wb-dialog-border);
}

:global(html.dark .zhpg-weight-workbench-dialog .el-input-number .el-input__wrapper),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .el-input-number .el-input__wrapper),
:global(.dark-theme .zhpg-weight-workbench-dialog .el-input-number .el-input__wrapper) {
  background: #071426;
  border: 1px solid var(--wb-dialog-border-strong);
  box-shadow: none;
}

:global(html.dark .zhpg-weight-workbench-dialog .el-input-number__decrease),
:global(html.dark .zhpg-weight-workbench-dialog .el-input-number__increase),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .el-input-number__decrease),
:global(body[data-theme="dark"] .zhpg-weight-workbench-dialog .el-input-number__increase),
:global(.dark-theme .zhpg-weight-workbench-dialog .el-input-number__decrease),
:global(.dark-theme .zhpg-weight-workbench-dialog .el-input-number__increase) {
  color: #8fb0c8;
  background: #0a1a30;
  border-color: var(--wb-dialog-border);
}
</style>
