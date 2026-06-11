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
      <div class="subj-hint">请为各子节点填写重要性分值（如 1-9 分），系统将自动按分值比例计算权重</div>
      <div class="subj-cards">
        <div v-for="(child, idx) in curChildren" :key="child.uid || idx" class="subj-card">
          <div class="subj-card-title">{{ child.label || '未命名' }}</div>
          <el-input-number
            size="small"
            v-model="child.importance"
            :precision="6"
            :step="0.001"
            :min="0"
            controls-position="right"
            class="subj-num"
            @change="onChildChange"
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
import { firstParentInTree, refreshParentOkFlags, stampWeightAssignAlgorithmOnParents } from './subjectiveTreeShared'

const emit = defineEmits(['indicator-tree'])

const treeData = ref([])
const curParent = ref(null)
const defaultProps = { children: 'children', label: 'label' }

const curChildren = computed(() => curParent.value?.children || [])

defineExpose({ init })

function init(data) {
  treeData.value = data || []
  ensureImportanceDefaults(treeData.value)
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

function ensureImportanceDefaults(roots) {
  const walk = (list) => {
    for (const n of list || []) {
      if (Array.isArray(n.children) && n.children.length) {
        for (const c of n.children) {
          if (c.importance == null || !Number.isFinite(Number(c.importance))) c.importance = 1
        }
        walk(n.children)
      }
    }
  }
  walk(roots)
}

function isParentConfigured(parent) {
  if (!parent || !Array.isArray(parent.children)) return false
  return parent.children.every(c => Number.isFinite(Number(c.importance)) && Number(c.importance) > 0)
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
    ElMessage.warning('仍有节点未完成分值配置')
    return
  }
  stampWeightAssignAlgorithmOnParents(treeData.value, '不校验')
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
