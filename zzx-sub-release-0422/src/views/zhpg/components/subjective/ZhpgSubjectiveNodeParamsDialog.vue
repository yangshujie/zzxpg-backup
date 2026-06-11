<template>
  <el-dialog
    v-model="open"
    :title="dialogTitle"
    width="min(960px, 94vw)"
    append-to-body
    :close-on-click-modal="false"
    class="zhpg-subj-node-dialog"
    @opened="onOpened"
    @closed="onClosed"
  >
    <div v-if="!parentNode" class="empty-node">未指定父节点</div>
    <div v-else-if="!hasChildren" class="empty-node">当前节点没有子节点，无需配置主观赋权参数</div>
    <div v-else-if="!effectiveSubtype" class="empty-node">
      未识别到主观赋权算法（{{ getSubjectiveAlgLabel('') }}的 6 种之一）
    </div>
    <template v-else>
      <div class="alg-bar">
        <span class="alg-label">主观赋权方法</span>
        <el-tag size="large" class="alg-tag">{{ getSubjectiveAlgLabel(effectiveSubtype) }}</el-tag>
        <span class="alg-hint">当前父节点：<strong>{{ parentNode.label || '—' }}</strong>，共 {{ children.length }} 个直接子节点</span>
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
      <el-button type="primary" :disabled="!effectiveSubtype || !hasChildren" @click="onConfirm">确 认</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
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
  /** 当前父节点对象（引用，会就地被子组件修改） */
  parentNode: { type: Object, default: null },
  /** 主观赋权 subtype（6 种中文之一） */
  subtype: { type: String, default: '' }
})

const emit = defineEmits(['done'])

const subjRef = ref(null)
const renderKey = ref(0)
const snapshotJson = ref('')
const confirmed = ref(false)

const hasChildren = computed(() =>
  Array.isArray(props.parentNode?.children) && props.parentNode.children.length > 0
)
const children = computed(() => props.parentNode?.children || [])
const effectiveSubtype = computed(() => SUBJ_COMPONENT_MAP[props.subtype] ? props.subtype : '')
const currentComp = computed(() => SUBJ_COMPONENT_MAP[effectiveSubtype.value] || null)
const dialogTitle = computed(() =>
  effectiveSubtype.value
    ? `本节点主观赋权配置 — ${getSubjectiveAlgLabel(effectiveSubtype.value)}`
    : '本节点主观赋权配置'
)

function onOpened() {
  if (!props.parentNode || !hasChildren.value || !effectiveSubtype.value) return
  snapshotJson.value = safeSnapshot(props.parentNode)
  confirmed.value = false
  renderKey.value++
  nextTick(() => subjRef.value?.init?.([props.parentNode]))
}

function onClosed() {
  if (!confirmed.value) restoreSnapshot()
  snapshotJson.value = ''
}

// subtype 变化（外层下拉切换算法）→ 重置子组件
watch(() => effectiveSubtype.value, () => {
  if (!open.value) return
  renderKey.value++
  nextTick(() => subjRef.value?.init?.([props.parentNode]))
})

function onConfigured() {
  ElMessage.success('已记录本节点主观赋权配置，点「确认」保留修改')
}

function onConfirm() {
  if (!props.parentNode || !hasChildren.value || !effectiveSubtype.value) {
    open.value = false
    return
  }
  confirmed.value = true
  emit('done', { parentNode: props.parentNode })
  open.value = false
}

function onCancel() {
  open.value = false
}

function safeSnapshot(node) {
  try { return JSON.stringify(node) } catch { return '' }
}

function restoreSnapshot() {
  if (!snapshotJson.value || !props.parentNode) return
  try {
    const snap = JSON.parse(snapshotJson.value)
    for (const k of Object.keys(props.parentNode)) {
      if (!(k in snap)) delete props.parentNode[k]
    }
    Object.assign(props.parentNode, snap)
    const snapChildren = Array.isArray(snap.children) ? snap.children : []
    const curChildren = Array.isArray(props.parentNode.children) ? props.parentNode.children : []
    if (snapChildren.length === curChildren.length) {
      for (let i = 0; i < curChildren.length; i++) {
        const snapC = snapChildren[i] || {}
        const curC = curChildren[i]
        for (const k of Object.keys(curC)) {
          if (!(k in snapC)) delete curC[k]
        }
        Object.assign(curC, snapC)
      }
    } else if (Array.isArray(props.parentNode.children)) {
      props.parentNode.children.splice(0, props.parentNode.children.length, ...snapChildren)
    }
  } catch {
    // ignore
  }
}
</script>

<style scoped>
.zhpg-subj-node-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}
.empty-node {
  padding: 32px;
  color: var(--el-text-color-secondary);
  text-align: center;
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
</style>
