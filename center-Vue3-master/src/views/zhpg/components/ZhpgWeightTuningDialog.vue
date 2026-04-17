<template>
  <el-dialog
    v-model="open"
    title="权重分配调优"
    width="min(960px, 94vw)"
    append-to-body
    class="zhpg-weight-tuning-dialog"
    @opened="onOpened"
    @closed="onClosed"
  >
    <div v-if="!treeData?.length" class="empty-tree">暂无指标树</div>
    <div v-else class="weight-tuning-layout">
      <div class="wtl-left">
        <div class="wtl-side-title">结构树（点选父节点）</div>
        <el-scrollbar max-height="440px">
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
              />
            </div>
          </el-col>
        </el-row>
      </div>
    </div>
    <template #footer>
      <div class="wtl-footer">
        <div class="wtl-footer-left">
          <el-button :disabled="!childrenList.length" @click="averageAmongSiblings">一键平均</el-button>
          <el-button @click="validateCurrent">校验（当前父节点）</el-button>
        </div>
        <div class="wtl-footer-right">
          <el-button @click="onCancel">取 消</el-button>
          <el-button type="primary" @click="onFinish">完 成</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

const SUM_EPS = 1e-5

const open = defineModel('modelValue', { type: Boolean, default: false })

const props = defineProps({
  /** 与工作台共用同一棵树的根数组（就地修改） */
  treeData: {
    type: Array,
    required: true
  }
})

const treeRef = ref(null)
const snapshotJson = ref('')
const suppressRestore = ref(false)
const selectedParent = ref(null)
const selectedParentUid = ref('')
const lastValidateOk = ref(null)

const parentTitle = computed(() => selectedParent.value?.label || '—')

const childrenList = computed(() => {
  const p = selectedParent.value
  if (!p?.children?.length) return []
  return p.children
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

/** 广度优先：第一个有子节点的节点 */
function firstParentInTree(nodes) {
  if (!nodes?.length) return null
  const q = [...nodes]
  while (q.length) {
    const n = q.shift()
    const ch = n.children
    if (ch?.length) return n
    if (ch) {
      for (const c of ch) q.push(c)
    }
  }
  return nodes[0]
}

function onOpened() {
  snapshotJson.value = JSON.stringify(props.treeData)
  suppressRestore.value = false
  lastValidateOk.value = null
  const pick = firstParentInTree(props.treeData)
  selectedParent.value = pick
  selectedParentUid.value = pick?.uid || ''
  nextTick(() => {
    if (pick?.uid && treeRef.value?.setCurrentKey) {
      treeRef.value.setCurrentKey(pick.uid)
    }
  })
}

function onClosed() {
  if (!suppressRestore.value) {
    restoreSnapshot()
  }
  suppressRestore.value = false
  lastValidateOk.value = null
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
  lastValidateOk.value = null
}

/** 未输入（未绑定数值）：参与「一键平均」的待填槽位 */
function isUnsetWeight(w) {
  if (w == null || w === '') return true
  if (typeof w === 'number' && !Number.isFinite(w)) return true
  return false
}

/**
 * 一键平均：全部未填则各 1/n；部分已填则 (1 - 已填之和) 均分到未填项；若均已填且全为 0（常见于导入）则按全未填处理。
 */
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
    for (let i = 0; i < n - 1; i++) {
      list[i].weight = base
      acc += base
    }
    list[n - 1].weight = Math.round((1 - acc) * PREC) / PREC
    lastValidateOk.value = Math.abs(sumChildWeights(list) - 1) <= SUM_EPS
    ElMessage.success('已均分权重')
    return
  }

  if (unsetIdx.length > 0) {
    const remainder = 1 - sumFixed
    if (remainder < -SUM_EPS) {
      ElMessage.warning(`已填权重之和为 ${sumFixed.toFixed(8)}，超过 1，无法向未填项分配剩余权重`)
      return
    }
    const m = unsetIdx.length
    const base = Math.round((remainder / m) * PREC) / PREC
    let acc = 0
    for (let k = 0; k < m - 1; k++) {
      list[unsetIdx[k]].weight = base
      acc += base
    }
    list[unsetIdx[m - 1]].weight = Math.round((remainder - acc) * PREC) / PREC
    lastValidateOk.value = Math.abs(sumChildWeights(list) - 1) <= SUM_EPS
    ElMessage.success('已将剩余权重均分到未填子节点')
    return
  }

  if (Math.abs(sumFixed) < SUM_EPS) {
    const base = Math.round((1 / n) * PREC) / PREC
    let acc = 0
    for (let i = 0; i < n - 1; i++) {
      list[i].weight = base
      acc += base
    }
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

  ElMessage.warning(
    `子节点均已填写权重，但之和为 ${sumFixed.toFixed(8)}。请清空需要自动分配的项后再试，或手工调整`
  )
}

function validateCurrent() {
  const list = childrenList.value
  if (!list.length) {
    ElMessage.warning('当前父节点无子节点')
    lastValidateOk.value = null
    return false
  }
  const sum = sumChildWeights(list)
  const ok = Math.abs(sum - 1) <= SUM_EPS
  lastValidateOk.value = ok
  if (ok) ElMessage.success('校验通过：当前父节点下子权重之和为 1')
  else ElMessage.warning(`校验未通过：和为 ${sum.toFixed(8)}，请调整至 1`)
  return ok
}

/** 树上每一处有 children 的节点，其直接子节点 weight 之和应为 1 */
function validateWholeTree(nodes) {
  if (!nodes?.length) return { ok: true, msg: '' }
  for (const n of nodes) {
    const ch = n.children
    if (ch?.length) {
      const sum = sumChildWeights(ch)
      if (Math.abs(sum - 1) > SUM_EPS) {
        return {
          ok: false,
          msg: `节点「${n.label || n.uid || '?'}」下子权重之和为 ${sum.toFixed(6)}，应为 1`
        }
      }
      const inner = validateWholeTree(ch)
      if (!inner.ok) return inner
    }
  }
  return { ok: true, msg: '' }
}

function onCancel() {
  suppressRestore.value = false
  open.value = false
}

function onFinish() {
  const check = validateWholeTree(props.treeData)
  if (!check.ok) {
    ElMessage.warning(check.msg)
    return
  }
  suppressRestore.value = true
  open.value = false
}

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
.wtl-cards {
  margin-top: 4px;
}
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
.wtl-num {
  width: 100%;
}
.wtl-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}
.wtl-footer-left,
.wtl-footer-right {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
</style>
