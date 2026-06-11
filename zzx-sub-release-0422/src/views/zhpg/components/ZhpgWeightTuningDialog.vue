<template>
  <el-dialog
    v-model="open"
    title="权重分配调优"
    width="min(1080px, 96vw)"
    append-to-body
    class="zhpg-weight-tuning-dialog"
    :close-on-click-modal="false"
    @opened="onOpened"
    @closed="onClosed"
  >
    <div v-if="!treeData?.length" class="empty-tree">暂无指标树</div>
    <el-tabs v-else v-model="activeTab" class="wtd-tabs">
      <!-- ============ 客观赋权 Tab ============ -->
      <el-tab-pane label="客观赋权" name="objective">
        <div class="tab-inner">
          <div class="tab-hint">
            按所选客观算法（熵权 / AHP 等）对整棵指标树自上而下逐层计算权重；可在每个父节点上通过「本节点·赋权与传导」单独覆盖算法。
          </div>
          <el-form label-width="120px" class="tab-form">
            <el-form-item label="客观算法">
              <el-select
                v-model="objectiveAlgKey"
                clearable
                placeholder="不选则按各父节点上保存的算法（缺省即熵权）计算"
                style="width: 100%"
              >
                <el-option
                  v-for="o in objectiveAlgOptions"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="模拟样本行数">
              <el-input-number
                v-model="objectiveSampleRows"
                :min="2"
                :max="64"
                controls-position="right"
                style="width: 200px"
              />
              <span class="form-extra-hint">仅熵权类用；指标体系下尚无真实样本时按模拟样本估计权重。</span>
            </el-form-item>
          </el-form>
          <el-alert
            v-if="!systemId"
            type="warning"
            :closable="false"
            show-icon
            title="请先保存指标体系后再运行客观赋权"
            class="tab-alert"
          />
        </div>
      </el-tab-pane>

      <!-- ============ 主观赋权 Tab ============ -->
      <el-tab-pane label="主观赋权" name="subjective">
        <div class="tab-inner">
          <div class="tab-hint">
            在 6 种主观赋权方法中任选一种，为各父节点的子项填写所需参数；点「主观赋权计算」立即按本地 Java 实现计算并写回指标树。
          </div>
          <div class="subj-alg-bar">
            <span class="subj-alg-label">主观算法</span>
            <el-select v-model="subjAlgKey" class="subj-alg-select" @change="onSubjAlgChange">
              <el-option
                v-for="opt in SUBJECTIVE_ALG_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <span class="subj-alg-hint">同一算法可点下方完成按钮反复重配，每次都会重置交互区状态。</span>
          </div>
          <component
            v-if="currentSubjComp"
            :is="currentSubjComp"
            :key="subjRenderKey"
            ref="subjRef"
            @indicator-tree="onSubjectiveConfigured"
          />
          <el-alert
            v-if="!systemId"
            type="warning"
            :closable="false"
            show-icon
            title="请先保存指标体系后再运行主观赋权"
            class="tab-alert"
          />
        </div>
      </el-tab-pane>

      <!-- ============ 手动微调 Tab ============ -->
      <el-tab-pane label="手动微调" name="manual">
        <div class="tab-inner">
          <div class="tab-hint">
            适用于已有专家直接给定权重数值的场景。点选左侧父节点 → 为各子节点填写权重 → 校验和=1。
          </div>
          <div class="weight-tuning-layout">
            <div class="wtl-left">
              <div class="wtl-side-title">结构树（点选父节点）</div>
              <el-scrollbar max-height="420px">
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
                />
              </el-scrollbar>
            </div>
            <div class="wtl-right">
              <div class="wtl-right-head">
                <span class="wtl-parent-label">当前父节点</span>
                <span class="wtl-parent-name">{{ parentTitle }}</span>
              </div>
              <div v-if="currentSumText" class="wtl-sum-line" :class="{ 'is-ok': lastValidateOk === true, 'is-bad': lastValidateOk === false }">
                {{ currentSumText }}
              </div>
              <div v-if="!childrenList.length" class="wtl-no-children">该节点无子节点，请在左侧选择有子级的节点</div>
              <el-row v-else :gutter="12" class="wtl-cards">
                <el-col v-for="c in childrenList" :key="c.uid" :xs="24" :sm="12" :md="8">
                  <div class="wtl-card">
                    <div class="wtl-card-title">{{ c.label || '未命名' }}</div>
                    <el-input-number
                      v-model="c.weight"
                      :min="0"
                      :max="1"
                      :step="0.0001"
                      :precision="8"
                      controls-position="right"
                      class="wtl-num"
                      @change="lastEditedUid = c.uid"
                    />
                  </div>
                </el-col>
              </el-row>
              <div class="wtl-manual-tools">
                <el-button :disabled="!childrenList.length" @click="averageAmongSiblings">一键平均</el-button>
                <el-button @click="validateCurrent">校验（当前父节点）</el-button>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <div class="wtd-footer">
        <div class="wtd-footer-left">
          <el-button
            v-if="activeTab === 'objective'"
            type="warning"
            icon="DataAnalysis"
            :loading="objectiveWeightLoading"
            :disabled="!systemId || objectiveWeightDisabled"
            @click="runObjectiveWeight"
          >客观赋权计算</el-button>
          <el-button
            v-if="activeTab === 'subjective'"
            type="primary"
            icon="DataAnalysis"
            :loading="subjectiveRunning"
            :disabled="!systemId || subjectiveRunning"
            @click="runSubjectiveWeight"
          >主观赋权计算</el-button>
        </div>
        <div class="wtd-footer-right">
          <el-button @click="onCancel">取 消</el-button>
          <el-button type="primary" :loading="saving" @click="onFinish">完 成 / 保存</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  applyIndicatorTreeWeights,
  subjectiveWeightIndicatorSystem
} from '@/api/zhpg/indicatorSystem'
import SubjectiveAhpScore from './subjective/SubjectiveAhpScore.vue'
import SubjectiveSimilarity from './subjective/SubjectiveSimilarity.vue'
import SubjectiveExpert from './subjective/SubjectiveExpert.vue'
import SubjectiveEvidence from './subjective/SubjectiveEvidence.vue'
import SubjectiveChainRatio from './subjective/SubjectiveChainRatio.vue'
import SubjectiveAhpMatrix from './subjective/SubjectiveAhpMatrix.vue'
import { SUBJECTIVE_ALG_OPTIONS } from './subjective/subjectiveTreeShared'

const SUM_EPS = 1e-5

const open = defineModel('modelValue', { type: Boolean, default: false })

const props = defineProps({
  /** 与工作台共用同一棵树的根数组（就地修改） */
  treeData: { type: Array, required: true },
  /** 当前指标体系 id，用于保存 / 运行赋权 */
  systemId: { type: [Number, String], default: null },
  /** 是否显示客观赋权 Tab（模板模式下可隐藏） */
  showObjectiveWeight: { type: Boolean, default: false },
  /** 透传：外部触发客观赋权的 loading（与外部进度 dialog 联动） */
  objectiveWeightLoading: { type: Boolean, default: false },
  objectiveWeightDisabled: { type: Boolean, default: false }
})

const emit = defineEmits(['run-objective-weight', 'tree-saved', 'subjective-computed'])

const treeRef = ref(null)
const subjRef = ref(null)
const snapshotJson = ref('')
const suppressRestore = ref(false)
const saving = ref(false)
const subjectiveRunning = ref(false)

// Tab
const activeTab = ref('subjective')

// 客观赋权
const objectiveAlgOptions = [
  { value: '', label: '按各父节点保存的算法（默认）' },
  { value: 'ENTROPY', label: '熵权法' },
  { value: 'AHP', label: '层次分析法（AHP，模拟矩阵）' }
]
const objectiveAlgKey = ref('')
const objectiveSampleRows = ref(8)

// 主观赋权
const SUBJ_COMPONENT_MAP = {
  '不校验': SubjectiveAhpScore,
  '相似度法': SubjectiveSimilarity,
  '校验': SubjectiveExpert,
  '理论证据法': SubjectiveEvidence,
  '连环比率法': SubjectiveChainRatio,
  '层次分析': SubjectiveAhpMatrix
}
const subjAlgKey = ref('不校验')
const subjRenderKey = ref(0)
const currentSubjComp = computed(() => SUBJ_COMPONENT_MAP[subjAlgKey.value] || null)

// 手动模式
const selectedParent = ref(null)
const selectedParentUid = ref('')
const lastValidateOk = ref(null)
const lastEditedUid = ref('')

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

// ================= 对话框生命周期 =================
function onOpened() {
  snapshotJson.value = JSON.stringify(props.treeData)
  suppressRestore.value = false
  lastValidateOk.value = null
  pickInitialTab()
  // 手动 Tab 初始化
  const pick = firstParentInTree(props.treeData)
  selectedParent.value = pick
  selectedParentUid.value = pick?.uid || ''
  lastEditedUid.value = ''
  nextTick(() => {
    if (pick?.uid && treeRef.value?.setCurrentKey) treeRef.value.setCurrentKey(pick.uid)
    initSubjIfNeeded()
  })
}

function onClosed() {
  if (!suppressRestore.value) restoreSnapshot()
  suppressRestore.value = false
  lastValidateOk.value = null
}

function pickInitialTab() {
  // 优先取根节点上的主观算法；没有则保留上次 Tab；初始默认主观
  const root = firstParentInTree(props.treeData)
  const raw = root?.weightAssignAlgorithm
  if (typeof raw === 'string' && SUBJ_COMPONENT_MAP[raw]) {
    activeTab.value = 'subjective'
    subjAlgKey.value = raw
  } else if (typeof raw === 'number' || (typeof raw === 'string' && /^\d+$/.test(raw))) {
    activeTab.value = props.showObjectiveWeight ? 'objective' : 'subjective'
  }
  // 模板模式（无客观赋权按钮）默认主观
  if (!props.showObjectiveWeight && activeTab.value === 'objective') {
    activeTab.value = 'subjective'
  }
}

function restoreSnapshot() {
  if (!snapshotJson.value) return
  try {
    const arr = JSON.parse(snapshotJson.value)
    if (!Array.isArray(arr)) return
    props.treeData.splice(0, props.treeData.length, ...arr)
  } catch {
    // ignore
  }
}

function onTreePick(data) {
  selectedParent.value = data
  selectedParentUid.value = data?.uid || ''
  lastEditedUid.value = ''
  lastValidateOk.value = null
}

function initSubjIfNeeded() {
  if (activeTab.value !== 'subjective') return
  subjRenderKey.value++
  nextTick(() => {
    subjRef.value?.init?.(props.treeData)
  })
}

function onSubjAlgChange() {
  subjRenderKey.value++
  nextTick(() => {
    subjRef.value?.init?.(props.treeData)
  })
}

function onSubjectiveConfigured() {
  ElMessage.success('已记录主观赋权配置，可点「主观赋权计算」立即计算权重')
}

// ================= 手动模式 =================
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
    const w = list[i].weight
    if (isUnsetWeight(w)) unsetIdx.push(i)
    else sumFixed += Number(w)
  }
  if (unsetIdx.length === n) {
    const base = Math.round((1 / n) * PREC) / PREC
    let acc = 0
    for (let i = 0; i < n - 1; i++) { list[i].weight = base; acc += base }
    list[n - 1].weight = Math.round((1 - acc) * PREC) / PREC
    lastValidateOk.value = Math.abs(sumChildWeights(list) - 1) <= SUM_EPS
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
    lastValidateOk.value = Math.abs(sumChildWeights(list) - 1) <= SUM_EPS
    ElMessage.success('已将剩余权重均分到未填子节点')
    return
  }
  if (Math.abs(sumFixed) < SUM_EPS) {
    const base = Math.round((1 / n) * PREC) / PREC
    let acc = 0
    for (let i = 0; i < n - 1; i++) { list[i].weight = base; acc += base }
    list[n - 1].weight = Math.round((1 - acc) * PREC) / PREC
    lastValidateOk.value = Math.abs(sumChildWeights(list) - 1) <= SUM_EPS
    ElMessage.success('子节点均为 0，已按个数均分')
    return
  }
  if (Math.abs(sumFixed - 1) <= SUM_EPS) {
    lastValidateOk.value = true
    ElMessage.info('当前子节点权重之和已为 1，无需平均')
    return
  }
  const editUid = lastEditedUid.value
  if (editUid && list.some(item => item.uid === editUid)) {
    const fixedNode = list.find(item => item.uid === editUid)
    const fixedVal = numWeight(fixedNode.weight)
    const others = list.filter(item => item.uid !== editUid)
    if (others.length > 0) {
      const remainder = 1 - fixedVal
      if (remainder < -SUM_EPS) {
        ElMessage.warning(`手动修改项权重为 ${fixedVal.toFixed(4)}，已超过 1`)
        return
      }
      const m = others.length
      const base = Math.round((remainder / m) * PREC) / PREC
      let acc = 0
      for (let k = 0; k < m - 1; k++) { others[k].weight = base; acc += base }
      others[m - 1].weight = Math.round((remainder - acc) * PREC) / PREC
      lastValidateOk.value = Math.abs(sumChildWeights(list) - 1) <= SUM_EPS
      ElMessage.success(`已固定「${fixedNode.label || '未命名'}」，平衡其余项`)
      return
    }
  }
  ElMessage.warning(`子节点均已填写权重，但之和为 ${sumFixed.toFixed(8)}`)
}

function validateCurrent() {
  const list = childrenList.value
  if (!list.length) { ElMessage.warning('当前父节点无子节点'); lastValidateOk.value = null; return false }
  const sum = sumChildWeights(list)
  const ok = Math.abs(sum - 1) <= SUM_EPS
  lastValidateOk.value = ok
  if (ok) ElMessage.success('校验通过：当前父节点下子权重之和为 1')
  else ElMessage.warning(`校验未通过：和为 ${sum.toFixed(8)}`)
  return ok
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

// ================= 保存 / 计算 =================
function buildTreePayload() {
  const data = props.treeData
  if (!Array.isArray(data) || data.length === 0) return { treeData: [] }
  if (data.length === 1) return { treeData: data[0] }
  return { treeData: data }
}

async function onFinish() {
  if (activeTab.value === 'manual') {
    const check = validateWholeTree(props.treeData)
    if (!check.ok) { ElMessage.warning(check.msg); return }
  }
  if (!props.systemId) {
    suppressRestore.value = true
    open.value = false
    return
  }
  saving.value = true
  try {
    const res = await applyIndicatorTreeWeights({
      systemId: props.systemId,
      indicatorTree: buildTreePayload()
    })
    ElMessage.success(res?.msg || '已保存')
    emit('tree-saved', res?.data?.indicatorTreeWeight)
    suppressRestore.value = true
    open.value = false
  } catch (e) {
    ElMessage.error('保存失败：' + (e?.message || e))
  } finally {
    saving.value = false
  }
}

function onCancel() {
  suppressRestore.value = false
  open.value = false
}

async function runSubjectiveWeight() {
  if (!props.systemId) {
    ElMessage.warning('请先保存指标体系后再运行主观赋权')
    return
  }
  subjectiveRunning.value = true
  try {
    // 先保存当前树（含每父节点选定的主观算法 + 参数），再触发计算
    await applyIndicatorTreeWeights({
      systemId: props.systemId,
      indicatorTree: buildTreePayload()
    })
    const res = await subjectiveWeightIndicatorSystem(props.systemId, { persist: true })
    const payload = res?.data || {}
    ElMessage.success(res?.msg || '主观赋权完成')
    if (payload.hint) ElMessage.info(String(payload.hint))
    emit('subjective-computed', payload.indicatorTreeWeight)
    suppressRestore.value = true
    open.value = false
  } catch (e) {
    ElMessage.error('主观赋权失败：' + (e?.message || e))
  } finally {
    subjectiveRunning.value = false
  }
}

async function runObjectiveWeight() {
  if (!props.systemId) {
    ElMessage.warning('请先保存指标体系后再运行客观赋权')
    return
  }
  // 先把当前 Tab 选定的客观算法 / 模拟样本行数写回根节点上，方便外部进度 dialog 透传
  if (Array.isArray(props.treeData) && props.treeData.length) {
    const root = firstParentInTree(props.treeData)
    if (root && objectiveAlgKey.value) {
      // 节点上保存字符串 code（"AHP" / "ENTROPY"）—— ObjectiveWeightServiceImpl.mapToPythonModule 已兼容
      root.weightAssignAlgorithm = objectiveAlgKey.value
    }
  }
  // 委派给父级：父级的 runObjectiveWeight 已经接管进度 dialog 与样本行数等参数
  emit('run-objective-weight')
  suppressRestore.value = true
  open.value = false
}

watch(() => activeTab.value, (v) => {
  if (v === 'subjective') initSubjIfNeeded()
})

watch(() => props.treeData, () => {
  if (open.value && activeTab.value === 'subjective') initSubjIfNeeded()
})
</script>

<style scoped>
.zhpg-weight-tuning-dialog :deep(.el-dialog__body) {
  background: transparent;
}
.zhpg-weight-tuning-dialog :deep(.el-tree),
.zhpg-weight-tuning-dialog :deep(.el-scrollbar__view),
.zhpg-weight-tuning-dialog :deep(.el-scrollbar__wrap) {
  background: transparent !important;
}
.empty-tree {
  padding: 24px;
  text-align: center;
  color: var(--el-text-color-secondary);
}
.wtd-tabs :deep(.el-tabs__content) {
  padding-top: 4px;
}
.tab-inner {
  min-height: 360px;
}
.tab-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
  padding: 8px 10px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
}
.tab-form {
  max-width: 720px;
}
.form-extra-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-left: 8px;
}
.tab-alert {
  margin-top: 12px;
}
.subj-alg-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  padding: 8px 10px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}
.subj-alg-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.subj-alg-select { width: 320px; }
.subj-alg-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.weight-tuning-layout {
  display: flex;
  gap: 16px;
  min-height: 320px;
}
.wtl-left {
  flex: 0 0 280px;
  max-width: 36%;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 10px;
  background: var(--el-fill-color-blank);
}
.wtl-side-title {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}
.wtl-right {
  flex: 1;
  min-width: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px;
  background: var(--el-fill-color-blank);
}
.wtl-right-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}
.wtl-parent-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.wtl-parent-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.wtl-sum-line {
  font-size: 13px;
  margin-bottom: 12px;
  padding: 6px 10px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
}
.wtl-sum-line.is-ok {
  color: var(--el-color-success);
  background: var(--el-color-success-light-9);
}
.wtl-sum-line.is-bad {
  color: var(--el-color-warning);
  background: var(--el-color-warning-light-9);
}
.wtl-no-children {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  padding: 24px 8px;
}
.wtl-cards { margin-top: 4px; }
.wtl-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 12px;
  background: var(--el-fill-color-blank);
}
.wtl-card-title {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.wtl-num { width: 100%; }
.wtl-manual-tools {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed var(--el-border-color-lighter);
}
.wtd-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}
.wtd-footer-left,
.wtd-footer-right {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
</style>

<style>
.zhpg-weight-tuning-dialog.el-dialog {
  --wtl-dialog-bg: var(--el-bg-color);
  --wtl-dialog-border: var(--el-border-color-light);
  --wtl-dialog-shadow: var(--el-box-shadow-light);
  --wtl-dialog-title: var(--el-text-color-primary);
  --wtl-close-color: var(--el-text-color-secondary);
  --wtl-tree-text: var(--el-text-color-regular);
  --wtl-tree-hover-bg: var(--el-fill-color-light);
  --wtl-tree-hover-text: var(--el-text-color-primary);
  --wtl-tree-current-bg: var(--el-color-primary-light-9);
  --wtl-tree-current-text: var(--el-color-primary);
  background: var(--wtl-dialog-bg);
  border: 1px solid var(--wtl-dialog-border);
  box-shadow: var(--wtl-dialog-shadow);
}

.zhpg-weight-tuning-dialog .el-dialog__header,
.zhpg-weight-tuning-dialog .el-dialog__body,
.zhpg-weight-tuning-dialog .el-dialog__footer {
  background: var(--wtl-dialog-bg);
}

.zhpg-weight-tuning-dialog .el-dialog__title {
  color: var(--wtl-dialog-title);
}

.zhpg-weight-tuning-dialog .el-dialog__headerbtn .el-dialog__close {
  color: var(--wtl-close-color);
}

.zhpg-weight-tuning-dialog .el-tree-node__content {
  color: var(--wtl-tree-text);
}

.zhpg-weight-tuning-dialog .el-tree-node__content:hover {
  background: var(--wtl-tree-hover-bg);
  color: var(--wtl-tree-hover-text);
}

.zhpg-weight-tuning-dialog .el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content {
  background: var(--wtl-tree-current-bg);
  color: var(--wtl-tree-current-text);
}

html.dark .zhpg-weight-tuning-dialog,
body[data-theme="dark"] .zhpg-weight-tuning-dialog,
.dark-theme .zhpg-weight-tuning-dialog {
  --wtl-dialog-bg: #050d18;
  --wtl-dialog-border: rgba(0, 242, 255, 0.16);
  --wtl-dialog-shadow: 0 24px 72px rgba(0, 0, 0, 0.52);
  --wtl-dialog-title: #d8e7f5;
  --wtl-close-color: #38bdf8;
  --wtl-tree-text: #9fb6cb;
  --wtl-tree-hover-bg: rgba(0, 242, 255, 0.08);
  --wtl-tree-hover-text: #d8e7f5;
  --wtl-tree-current-bg: rgba(216, 231, 245, 0.1);
  --wtl-tree-current-text: #d8e7f5;
  color: #d8e7f5;
}
</style>
