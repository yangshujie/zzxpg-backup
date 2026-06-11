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
      <div class="subj-hint">请添加专家并为每个父节点下的子指标分配权重，确保和=1</div>
      <div class="expert-toolbar">
        <el-input
          v-model="newExpertName"
          size="small"
          placeholder="请输入专家姓名"
          class="expert-input"
          clearable
          @keyup.enter="addExpert"
        />
        <el-button size="small" type="primary" @click="addExpert">添加专家</el-button>
        <el-button size="small" type="danger" plain :disabled="!expertList.length" @click="removeExpert">删除当前专家</el-button>
      </div>
      <div v-if="curParent" class="subj-parent-tag">{{ curParent.label || '-' }}</div>
      <el-empty v-if="!expertList.length" description="暂无专家数据" />
      <el-tabs v-else v-model="activeExpertIdx" class="subj-expert-tabs">
        <el-tab-pane
          v-for="(name, idx) in expertList"
          :key="idx"
          :label="'专家' + (idx + 1) + '：' + name"
          :name="idx"
        >
          <div class="subj-cards">
            <div v-for="(child, ci) in curChildren" :key="(child.uid || ci) + '-' + idx" class="subj-card">
              <div class="subj-card-title">{{ child.label || '未命名' }}</div>
              <el-input-number
                size="small"
                :model-value="getExpertWeight(child, idx)"
                @update:model-value="val => setExpertWeight(child, idx, val)"
                :precision="8"
                :step="0.0001"
                :min="0"
                :max="1"
                controls-position="right"
                class="subj-num"
                @change="onChildChange"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
    <div class="subj-footer">
      <el-button size="small" @click="handleCheckSum" :disabled="!expertList.length">校验当前和=1</el-button>
      <el-button type="primary" @click="handleOk" :disabled="!expertList.length">确认</el-button>
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
const expertList = ref([])
const activeExpertIdx = ref(0)
const newExpertName = ref('')
const defaultProps = { children: 'children', label: 'label' }

const curChildren = computed(() => curParent.value?.children || [])

defineExpose({ init })

function init(data) {
  treeData.value = data || []
  const root = (data && data[0]) || {}
  if (!Array.isArray(root.expertList)) root.expertList = []
  expertList.value = root.expertList
  ensureExpertWeightArrays(treeData.value, expertList.value.length)
  refreshOk()
  nextTick(() => {
    curParent.value = firstParentInTree(treeData.value)
    activeExpertIdx.value = 0
  })
}

function onNodeClick(node) {
  if (node && Array.isArray(node.children) && node.children.length) {
    curParent.value = node
  }
}

function addExpert() {
  const raw = (newExpertName.value || '').trim()
  if (!raw) {
    ElMessage.warning('专家姓名不能为空')
    return
  }
  if (expertList.value.includes(raw)) {
    ElMessage.warning('该专家已存在')
    return
  }
  expertList.value.push(raw)
  newExpertName.value = ''
  if (treeData.value[0]) {
    treeData.value[0].expertList = expertList.value
  }
  ensureExpertWeightArrays(treeData.value, expertList.value.length)
  activeExpertIdx.value = expertList.value.length - 1
}

function removeExpert() {
  const idx = Number(activeExpertIdx.value)
  if (idx < 0 || idx >= expertList.value.length) return
  expertList.value.splice(idx, 1)
  if (treeData.value[0]) treeData.value[0].expertList = expertList.value
  const walk = (list) => {
    for (const n of list || []) {
      if (Array.isArray(n.children) && n.children.length) {
        for (const c of n.children) {
          if (Array.isArray(c.expertWeight)) c.expertWeight.splice(idx, 1)
        }
        walk(n.children)
      }
    }
  }
  walk(treeData.value)
  activeExpertIdx.value = Math.max(0, idx - 1)
  refreshOk()
}

function ensureExpertWeightArrays(roots, expertCount) {
  const walk = (list) => {
    for (const n of list || []) {
      if (Array.isArray(n.children) && n.children.length) {
        const equal = 1 / n.children.length
        for (const c of n.children) {
          if (!Array.isArray(c.expertWeight)) c.expertWeight = []
          while (c.expertWeight.length < expertCount) c.expertWeight.push(equal)
        }
        walk(n.children)
      }
    }
  }
  walk(roots)
}

function getExpertWeight(child, idx) {
  if (!Array.isArray(child.expertWeight)) child.expertWeight = []
  return child.expertWeight[idx]
}
function setExpertWeight(child, idx, val) {
  if (!Array.isArray(child.expertWeight)) child.expertWeight = []
  child.expertWeight[idx] = val
}

function isParentConfigured(parent) {
  if (!parent || !Array.isArray(parent.children) || !parent.children.length) return false
  const expertCount = expertList.value.length || 0
  if (expertCount === 0) return false
  for (let e = 0; e < expertCount; e++) {
    let sum = 0
    for (const c of parent.children) {
      const v = Number(Array.isArray(c.expertWeight) ? c.expertWeight[e] : 0)
      if (!Number.isFinite(v) || v < 0) return false
      sum += v
    }
    if (Math.abs(sum - 1) > 1e-4) return false
  }
  return true
}

function refreshOk() {
  refreshParentOkFlags(treeData.value, isParentConfigured)
}

function onChildChange() {
  if (curParent.value) curParent.value.isOk = isParentConfigured(curParent.value)
}

function handleCheckSum() {
  if (!curParent.value) return
  const e = Number(activeExpertIdx.value)
  const sum = (curChildren.value || []).reduce((s, c) => {
    const v = Number(Array.isArray(c.expertWeight) ? c.expertWeight[e] : 0)
    return s + (Number.isFinite(v) ? v : 0)
  }, 0)
  if (Math.abs(sum - 1) < 1e-4) {
    ElMessage.success('鏍￠獙閫氳繃锛氬綋鍓嶄笓瀹跺湪璇ョ埗鑺傜偣涓嬪垎閰嶇殑瀛愭潈閲嶅拰=1')
  } else {
    ElMessage.warning(`褰撳墠涓撳缁欏嚭鐨勫瓙鏉冮噸鍜?${sum.toFixed(6)}锛岄渶璋冩暣涓?1`)
  }
}

function handleOk() {
  refreshOk()
  if (!isWholeTreeOk(treeData.value)) {
    ElMessage.warning('仍有节点未完成专家打分或和不为 1')
    return
  }
  if (treeData.value[0]) treeData.value[0].expertList = expertList.value
  stampWeightAssignAlgorithmOnParents(treeData.value, '校验')
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
.expert-toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.expert-input {
  width: 160px;
}
</style>
