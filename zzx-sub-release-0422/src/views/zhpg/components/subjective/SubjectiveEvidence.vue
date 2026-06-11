<template>
  <div class="subjective-main">
    <div class="subj-left-tree">
      <el-tree
        :data="treeData"
        :props="defaultProps"
        node-key="uid"
        default-expand-all
        :expand-on-click-node="false"
        @node-click="onNodeClick"
      >
        <template #default="{ node, data }">
          <span v-if="!data.children || !data.children.length">{{ node.label }}</span>
          <span v-else :class="data.isOk ? 'subj-node-ok' : 'subj-node-bad'">{{ node.label }}</span>
        </template>
      </el-tree>
    </div>
    <div class="subj-right">
      <div class="subj-hint">请分两步填写证据分值，系统将综合两组证据计算最终权重</div>
      <div class="subj-step-bar">
        <el-radio-group v-model="activeStep" size="small" @change="onStepChange">
          <el-radio-button :label="0">证据 1</el-radio-button>
          <el-radio-button :label="1">证据 2</el-radio-button>
        </el-radio-group>
        <el-button size="small" @click="handleAverage">一键平均</el-button>
        <el-button size="small" type="success" plain @click="handleCheckSum">校验和=1</el-button>
      </div>
      <div v-if="curParent" class="subj-parent-tag">{{ curParent.label || '-' }}</div>
      <div class="subj-cards">
        <div v-for="(child, ci) in curChildren" :key="(child.uid || ci) + '-' + activeStep" class="subj-card">
          <div class="subj-card-title">{{ child.label || '未命名' }}</div>
          <el-input-number
            v-if="activeStep === 0"
            size="small"
            v-model="child.weightTemp1"
            :precision="8"
            :step="0.0001"
            :min="0"
            :max="1"
            controls-position="right"
            class="subj-num"
            @change="onChildChange(ci, 0)"
          />
          <el-input-number
            v-else
            size="small"
            v-model="child.weightTemp"
            :precision="8"
            :step="0.0001"
            :min="0"
            :max="1"
            controls-position="right"
            class="subj-num"
            @change="onChildChange(ci, 1)"
          />
        </div>
      </div>
    </div>
    <div class="subj-footer">
      <el-button type="primary" @click="handleOk">确认</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { firstParentInTree, refreshParentOkFlags, sumIsOne, stampWeightAssignAlgorithmOnParents } from './subjectiveTreeShared'

const emit = defineEmits(['indicator-tree'])

const treeData = ref([])
const curParent = ref(null)
const activeStep = ref(0)
const editedByStep = ref({ 0: new Set(), 1: new Set() })
const defaultProps = { children: 'children', label: 'label' }

const curChildren = computed(() => curParent.value?.children || [])

defineExpose({ init })

function init(data) {
  treeData.value = data || []
  ensureDefaults(treeData.value)
  refreshOk()
  nextTick(() => {
    curParent.value = firstParentInTree(treeData.value)
    editedByStep.value = { 0: new Set(), 1: new Set() }
  })
}

function onNodeClick(node) {
  if (node && Array.isArray(node.children) && node.children.length) {
    curParent.value = node
    editedByStep.value = { 0: new Set(), 1: new Set() }
  }
}

function onStepChange() {
  editedByStep.value[activeStep.value] = new Set()
}

function ensureDefaults(roots) {
  const walk = (list) => {
    for (const n of list || []) {
      if (Array.isArray(n.children) && n.children.length) {
        const cnt = n.children.length
        for (const c of n.children) {
          if (c.weightTemp1 == null) c.weightTemp1 = 1 / cnt
          if (c.weightTemp == null) c.weightTemp = 1 / cnt
        }
        walk(n.children)
      }
    }
  }
  walk(roots)
}

function fieldOfStep(step) {
  return step === 0 ? 'weightTemp1' : 'weightTemp'
}

function isParentConfigured(parent) {
  if (!parent || !Array.isArray(parent.children) || !parent.children.length) return false
  return sumIsOne(parent.children, 'weightTemp1') && sumIsOne(parent.children, 'weightTemp')
}

function refreshOk() {
  refreshParentOkFlags(treeData.value, isParentConfigured)
}

function onChildChange(idx, step) {
  if (typeof idx === 'number' && curChildren.value[idx]?.uid) {
    editedByStep.value[step].add(curChildren.value[idx].uid)
  }
  if (curParent.value) curParent.value.isOk = isParentConfigured(curParent.value)
}

function handleAverage() {
  const list = curChildren.value
  if (!list.length) return
  const field = fieldOfStep(activeStep.value)
  const edited = editedByStep.value[activeStep.value]
  let editedSum = 0
  let editCount = 0
  for (const c of list) {
    if (edited.has(c.uid)) {
      editCount++
      editedSum += Number(c[field]) || 0
    }
  }
  const remaining = 1 - editedSum
  const restCount = list.length - editCount
  if (restCount <= 0) return
  if (remaining < -1e-6) {
    ElMessage.warning('当前已填项总和超过1，无法分配')
    return
  }
  const each = remaining / restCount
  for (const c of list) {
    if (!edited.has(c.uid)) c[field] = Math.max(0, each)
  }
  if (curParent.value) {
    curParent.value.isOk = isParentConfigured(curParent.value)
  }
  ElMessage.success('计算成功，已更新证据权重')
}

function handleCheckSum() {
  const list = curChildren.value
  if (!list.length) return
  const field = fieldOfStep(activeStep.value)
  const sum = list.reduce((s, c) => s + (Number(c[field]) || 0), 0)
  if (Math.abs(sum - 1) < 1e-4) {
    ElMessage.success('鏍￠獙閫氳繃锛氬綋鍓嶄笓瀹跺湪璇ョ埗鑺傜偣涓嬪垎閰嶇殑瀛愭潈閲嶅拰=1')
    if (curParent.value) curParent.value.isOk = isParentConfigured(curParent.value)
  } else {
    ElMessage.warning(`鍜屼负 ${sum.toFixed(6)}锛岄渶璋冩暣涓?1`)
  }
}

function handleOk() {
  refreshOk()
  if (!isWholeTreeOk(treeData.value)) {
    ElMessage.warning('??????????')
    return
  }
  stampWeightAssignAlgorithmOnParents(treeData.value, '?????')
  emit('indicator-tree', treeData.value)
}

function isWholeTreeOk(roots) {
  let ok = true
  const walk = (list) => {
    for (const n of list || []) {
      if (Array.isArray(n.children) && n.children.length) {
        if (!n.isOk) ok = false
        walk(n.children)
      }
    }
  }
  walk(roots)
  return ok
}

</script>

<style scoped src="./subjectiveCommon.css"></style>
<style scoped>
.subj-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
  flex-wrap: wrap;
}
</style>
