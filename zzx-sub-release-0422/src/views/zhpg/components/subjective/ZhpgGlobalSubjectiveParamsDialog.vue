<template>
  <el-dialog
    v-model="open"
    :title="dialogTitle"
    width="min(1080px, 96vw)"
    append-to-body
    :close-on-click-modal="false"
    class="zhpg-global-subj-dialog"
    modal-class="zhpg-global-subj-modal"
    @opened="onOpened"
    @closed="onClosed"
  >
    <div v-if="!innerTree.length" class="empty-tree">指标树为空</div>
    <div v-else-if="!effectiveSubtype" class="empty-tree">
      未识别到主观赋权算法（仅 6 种主观方法支持参数配置：层次分析 / 相似度法 / 校验 / 理论证据法 / 连环比率法 / 不校验）
    </div>
    <template v-else>
      <div class="alg-bar">
        <span class="alg-label">主观赋权方法</span>
        <el-tag size="large" class="alg-tag">{{ getSubjectiveAlgLabel(effectiveSubtype) }}</el-tag>
        <span class="alg-hint">对整棵树所有父节点统一应用此算法；点击「确认」时记录输入参数，算法 ID 由全局配置统一写入。</span>
      </div>
      <component
        v-if="currentComp"
        :is="currentComp"
        :key="renderKey"
        ref="subjRef"
        @indicator-tree="onConfigured"
      />
    </template>
    <template #footer>
      <el-button @click="onCancel">取 消</el-button>
      <el-button type="primary" :disabled="!effectiveSubtype" @click="onConfirm">确 认（覆盖所有父节点）</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SubjectiveAhpScore from './SubjectiveAhpScore.vue'
import SubjectiveSimilarity from './SubjectiveSimilarity.vue'
import SubjectiveExpert from './SubjectiveExpert.vue'
import SubjectiveEvidence from './SubjectiveEvidence.vue'
import SubjectiveChainRatio from './SubjectiveChainRatio.vue'
import SubjectiveAhpMatrix from './SubjectiveAhpMatrix.vue'
import { getSubjectiveAlgLabel } from './subjectiveTreeShared'

const SUBJ_COMPONENT_MAP = {
  '不校验': SubjectiveAhpScore,
  '相似度法': SubjectiveSimilarity,
  '校验': SubjectiveExpert,
  '理论证据法': SubjectiveEvidence,
  '连环比率法': SubjectiveChainRatio,
  '层次分析': SubjectiveAhpMatrix
}

const open = defineModel('modelValue', { type: Boolean, default: false })

const props = defineProps({
  /** 指标树根数组（引用，会就地被子组件修改） */
  treeData: { type: Array, default: () => [] },
  /** 主观赋权 subtype（6 种中文之一）—— 由调用方根据所选算法管理表算法推断后传入 */
  subtype: { type: String, default: '' }
})

const emit = defineEmits(['done'])

const subjRef = ref(null)
const renderKey = ref(0)
const snapshotJson = ref('')
const confirmed = ref(false)

const innerTree = computed(() => props.treeData || [])
const effectiveSubtype = computed(() => SUBJ_COMPONENT_MAP[props.subtype] ? props.subtype : '')
const currentComp = computed(() => SUBJ_COMPONENT_MAP[effectiveSubtype.value] || null)
const dialogTitle = computed(() =>
  effectiveSubtype.value
    ? `全局主观赋权配置 — ${getSubjectiveAlgLabel(effectiveSubtype.value)}`
    : '全局主观赋权配置'
)

function onOpened() {
  if (!innerTree.value.length || !effectiveSubtype.value) return
  snapshotJson.value = safeSnapshot(innerTree.value)
  confirmed.value = false
  renderKey.value++
  nextTick(() => subjRef.value?.init?.(innerTree.value))
}

function onClosed() {
  if (!confirmed.value) restoreSnapshot()
  snapshotJson.value = ''
}

// subtype 变化（用户在外层切换了算法）也强制重置子组件
watch(() => effectiveSubtype.value, () => {
  if (!open.value) return
  renderKey.value++
  nextTick(() => subjRef.value?.init?.(innerTree.value))
})

function onConfigured() {
  ElMessage.success('已记录全局主观赋权配置，点「确认」覆盖所有父节点')
}

async function onConfirm() {
  if (!effectiveSubtype.value) return
  try {
    await ElMessageBox.confirm(
      `将统一所有父节点的权重分配参数为「${getSubjectiveAlgLabel(effectiveSubtype.value)}」，已有的节点级覆盖会被替换。是否继续？`,
      '覆盖确认',
      { type: 'warning', confirmButtonText: '继续', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  confirmed.value = true
  emit('done', {})
  open.value = false
}

function onCancel() {
  open.value = false
}

function safeSnapshot(roots) {
  try { return JSON.stringify(roots) } catch { return '' }
}

function restoreSnapshot() {
  if (!snapshotJson.value) return
  try {
    const arr = JSON.parse(snapshotJson.value)
    if (!Array.isArray(arr) || !Array.isArray(props.treeData)) return
    props.treeData.splice(0, props.treeData.length, ...arr)
  } catch {
    // ignore
  }
}
</script>

<style scoped>
.zhpg-global-subj-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}
.empty-tree {
  padding: 32px;
  text-align: center;
  color: var(--el-text-color-secondary);
}
.alg-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 8px 10px;
  margin-bottom: 10px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}
.alg-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.alg-tag {
  font-weight: 600;
}
.alg-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

:global(html.dark .zhpg-global-subj-modal),
:global(body[data-theme="dark"] .zhpg-global-subj-modal),
:global(.dark-theme .zhpg-global-subj-modal) {
  background-color: rgba(0, 5, 12, 0.82);
  backdrop-filter: blur(1px);
}

:global(html.dark .zhpg-global-subj-dialog),
:global(body[data-theme="dark"] .zhpg-global-subj-dialog),
:global(.dark-theme .zhpg-global-subj-dialog) {
  --subj-dialog-bg: #071426;
  --subj-dialog-panel: #08192c;
  --subj-dialog-card: #0b2038;
  --subj-dialog-soft: #0a1a30;
  --subj-dialog-border: rgba(0, 194, 255, 0.24);
  --subj-dialog-border-strong: rgba(0, 224, 255, 0.36);
  --subj-dialog-text: #dbeafe;
  --subj-dialog-muted: #8fb0c8;
  --subj-dialog-accent: #00d9ff;
  color: var(--subj-dialog-text);
  background: var(--subj-dialog-bg);
  border: 1px solid var(--subj-dialog-border);
  box-shadow: 0 30px 96px rgba(0, 0, 0, 0.72);
}

:global(html.dark .zhpg-global-subj-dialog .el-dialog__header),
:global(html.dark .zhpg-global-subj-dialog .el-dialog__body),
:global(html.dark .zhpg-global-subj-dialog .el-dialog__footer),
:global(body[data-theme="dark"] .zhpg-global-subj-dialog .el-dialog__header),
:global(body[data-theme="dark"] .zhpg-global-subj-dialog .el-dialog__body),
:global(body[data-theme="dark"] .zhpg-global-subj-dialog .el-dialog__footer),
:global(.dark-theme .zhpg-global-subj-dialog .el-dialog__header),
:global(.dark-theme .zhpg-global-subj-dialog .el-dialog__body),
:global(.dark-theme .zhpg-global-subj-dialog .el-dialog__footer) {
  background: var(--subj-dialog-bg);
}

:global(html.dark .zhpg-global-subj-dialog .el-dialog__title),
:global(body[data-theme="dark"] .zhpg-global-subj-dialog .el-dialog__title),
:global(.dark-theme .zhpg-global-subj-dialog .el-dialog__title) {
  color: var(--subj-dialog-text);
}

:global(html.dark .zhpg-global-subj-dialog .el-dialog__headerbtn .el-dialog__close),
:global(body[data-theme="dark"] .zhpg-global-subj-dialog .el-dialog__headerbtn .el-dialog__close),
:global(.dark-theme .zhpg-global-subj-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: #6bdcff;
}

:global(html.dark .zhpg-global-subj-dialog .alg-bar),
:global(body[data-theme="dark"] .zhpg-global-subj-dialog .alg-bar),
:global(.dark-theme .zhpg-global-subj-dialog .alg-bar) {
  background: linear-gradient(180deg, rgba(10, 26, 48, 0.98), rgba(8, 25, 44, 0.98));
  border: 1px solid var(--subj-dialog-border);
}

:global(html.dark .zhpg-global-subj-dialog .alg-label),
:global(html.dark .zhpg-global-subj-dialog .alg-hint),
:global(html.dark .zhpg-global-subj-dialog .empty-tree),
:global(body[data-theme="dark"] .zhpg-global-subj-dialog .alg-label),
:global(body[data-theme="dark"] .zhpg-global-subj-dialog .alg-hint),
:global(body[data-theme="dark"] .zhpg-global-subj-dialog .empty-tree),
:global(.dark-theme .zhpg-global-subj-dialog .alg-label),
:global(.dark-theme .zhpg-global-subj-dialog .alg-hint),
:global(.dark-theme .zhpg-global-subj-dialog .empty-tree) {
  color: var(--subj-dialog-muted);
}
</style>
