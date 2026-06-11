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
      <div class="subj-hint">请为各子节点填写相似度分值，系统将自动按占比进行权重归一化</div>
      <div class="subj-step-bar">
        <el-radio-group v-model="activeStep" size="small" @change="onStepChange">
          <el-radio-button :label="0">第一阶段</el-radio-button>
          <el-radio-button :label="1">第二阶段</el-radio-button>
        </el-radio-group>
        <el-button v-if="activeStep === 1" size="small" @click="handleAverage">一键平均</el-button>
        <el-button v-if="activeStep === 1" size="small" type="success" plain @click="handleCheckSum">校验和=1</el-button>
      </div>
      <div v-if="curParent" class="subj-parent-tag">{{ curParent.label || '-' }}</div>
      <div class="subj-cards">
        <div v-for="(child, ci) in curChildren" :key="(child.uid || ci) + '-' + activeStep" class="subj-card">
          <div class="subj-card-title">{{ child.label || '未命名' }}</div>
          <el-input-number
            v-if="activeStep === 0"
            size="small"
            v-model="child.importance"
            :precision="6"
            :step="0.001"
            :min="0"
            controls-position="right"
            class="subj-num"
            @change="onChildChange"
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
            @change="onChildChange(idx)"
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
const editedUids = ref(new Set())
const defaultProps = { children: 'children', label: 'label' }

const curChildren = computed(() => curParent.value?.children || [])

defineExpose({ init })

function init(data) {
  treeData.value = data || []
  ensureDefaults(treeData.value)
  refreshOk()
  nextTick(() => {
    curParent.value = firstParentInTree(treeData.value)
    editedUids.value = new Set()
  })
}

function onNodeClick(node) {
  if (node && Array.isArray(node.children) && node.children.length) {
    curParent.value = node
    editedUids.value = new Set()
  }
}

function onStepChange() {
  editedUids.value = new Set()
}

function ensureDefaults(roots) {
  const walk = (list) => {
    for (const n of list || []) {
      if (Array.isArray(n.children) && n.children.length) {
        const cnt = n.children.length
        for (const c of n.children) {
          if (c.importance == null) c.importance = 1
          if (c.weightTemp == null) c.weightTemp = 1 / cnt
        }
        walk(n.children)
      }
    }
  }
  walk(roots)
}

function isParentConfigured(parent) {
  if (!parent || !Array.isArray(parent.children)) return false
  const allImp = parent.children.every(c => Number(c.importance) > 0)
  const sumOk = sumIsOne(parent.children, 'weightTemp')
  return allImp && sumOk
}

function refreshOk() {
  refreshParentOkFlags(treeData.value, isParentConfigured)
}

function onChildChange(idx) {
  if (typeof idx === 'number' && curChildren.value[idx]?.uid) {
    editedUids.value.add(curChildren.value[idx].uid)
  }
  if (curParent.value) curParent.value.isOk = isParentConfigured(curParent.value)
}

function handleAverage() {
  const list = curChildren.value
  if (!list.length) return
  let edited = 0
  let editedSum = 0
  for (const c of list) {
    if (editedUids.value.has(c.uid)) {
      edited++
      editedSum += Number(c.weightTemp) || 0
    }
  }
  const remaining = 1 - editedSum
  const restCount = list.length - edited
  if (restCount <= 0) return
  if (remaining < -1e-6) {
    ElMessage.warning('固定值总和超过了 1，无法平摊')
    return
  }
  const each = remaining / restCount
  for (const c of list) {
    if (!editedUids.value.has(c.uid)) c.weightTemp = Math.max(0, each)
  }
  curParent.value.isOk = true
  ElMessage.success('已按分值占比完成权重归一化')
}

function handleCheckSum() {
  if (sumIsOne(curChildren.value, 'weightTemp')) {
    ElMessage.success('校验通过：和已等于 1')
    if (curParent.value) curParent.value.isOk = isParentConfigured(curParent.value)
  } else {
    const sum = curChildren.value.reduce((s, c) => s + (Number(c.weightTemp) || 0), 0)
    ElMessage.warning(`鍜屼负 ${sum.toFixed(6)}锛岄渶璋冩暣涓?1`)
  }
}

function handleOk() {
  refreshOk()
  if (!isWholeTreeOk(treeData.value)) {
    ElMessage.warning('仍有父节点未完成配置')
    return
  }
  stampWeightAssignAlgorithmOnParents(treeData.value, '相似度法')
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
