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
      <div class="subj-hint">请为除最后一项外的每个节点选择与其后一项的重要性比值</div>
      <div v-if="curParent" class="subj-parent-tag">{{ curParent.label || '-' }}</div>
      <div class="subj-cards">
        <div v-for="(child, idx) in curChildren" :key="child.uid || idx" class="subj-card">
          <div class="subj-card-title">{{ child.label || '未命名' }}</div>
          <el-select
            v-if="idx !== curChildren.length - 1"
            v-model="child.bz"
            size="small"
            placeholder="与后一节点之比"
            class="subj-num"
            @change="onChildChange"
          >
            <el-option v-for="opt in RATIO_OPTIONS" :key="opt" :label="opt" :value="opt" />
          </el-select>
          <el-select v-else disabled size="small" class="subj-num" placeholder="末位节点无需选择" />
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
import { firstParentInTree, refreshParentOkFlags, RATIO_OPTIONS, stampWeightAssignAlgorithmOnParents } from './subjectiveTreeShared'

const emit = defineEmits(['indicator-tree'])

const treeData = ref([])
const curParent = ref(null)
const defaultProps = { children: 'children', label: 'label' }

const curChildren = computed(() => curParent.value?.children || [])

defineExpose({ init })

function init(data) {
  treeData.value = data || []
  ensureDefaults(treeData.value)
  refreshOk()
  nextTick(() => {
    curParent.value = firstParentInTree(treeData.value)
  })
}

function onNodeClick(node) {
  if (node && Array.isArray(node.children) && node.children.length) {
    curParent.value = node
  }
}

function ensureDefaults(roots) {
  const walk = (list) => {
    for (const n of list || []) {
      if (Array.isArray(n.children) && n.children.length) {
        for (let i = 0; i < n.children.length - 1; i++) {
          if (!n.children[i].bz) n.children[i].bz = '1'
        }
        walk(n.children)
      }
    }
  }
  walk(roots)
}

function isParentConfigured(parent) {
  if (!parent || !Array.isArray(parent.children) || !parent.children.length) return false
  // 鏈€鍚庝竴椤逛笉瑕佹眰
  for (let i = 0; i < parent.children.length - 1; i++) {
    const v = parent.children[i].bz
    if (!RATIO_OPTIONS.includes(String(v))) return false
  }
  return true
}

function refreshOk() {
  refreshParentOkFlags(treeData.value, isParentConfigured)
}

function onChildChange() {
  if (curParent.value) curParent.value.isOk = isParentConfigured(curParent.value)
}

function handleOk() {
  refreshOk()
  if (!isWholeTreeOk(treeData.value)) {
    ElMessage.warning('仍有节点未完成比值配置')
    return
  }
  stampWeightAssignAlgorithmOnParents(treeData.value, '连环比率法')
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
