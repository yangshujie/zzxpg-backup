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
      <div class="subj-hint">请填写 AHP 互反矩阵并进行 CR 一致性校验</div>
      <div v-if="curParent" class="subj-parent-tag">{{ curParent.label || '-' }}</div>
      <div v-if="curChildren.length > 11" class="ccfx-warn">当前子节点数 {{ curChildren.length }} > 11，AHP RI 表可能无法支持此规模的校验</div>
      <div v-if="curChildren.length > 0" class="subj-ccfx-content">
        <div class="subj-ccfx-left-col">
          <div class="subj-ccfx-cell head">指标名称</div>
          <div v-for="(c, i) in curChildren" :key="'l' + (c.uid || i)" class="subj-ccfx-cell head">
            {{ c.label || '未命名' }}
          </div>
        </div>
        <div class="subj-ccfx-right">
          <div class="subj-ccfx-row">
            <div v-for="(c, j) in curChildren" :key="'h' + (c.uid || j)" class="subj-ccfx-cell head">
              {{ c.label || '未命名' }}
            </div>
          </div>
          <div v-for="(rowI, i) in curChildren" :key="'r' + (rowI.uid || i)" class="subj-ccfx-row">
            <div v-for="(colJ, j) in curChildren" :key="'c' + (colJ.uid || j) + '_' + (rowI.uid || i)" class="subj-ccfx-cell">
              <span v-if="i === j">1</span>
              <el-select
                v-else
                :model-value="matrix[i] && matrix[i][j]"
                size="small"
                style="width: 100%"
                @change="val => onCellChange(i, j, val)"
              >
                <el-option v-for="opt in RATIO_OPTIONS" :key="opt" :label="opt" :value="opt" />
              </el-select>
            </div>
          </div>
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
import { checkAhpConsistency } from '@/api/zhpg/indicatorSystem'

const emit = defineEmits(['indicator-tree'])

const treeData = ref([])
const curParent = ref(null)
const matrix = ref([])
const defaultProps = { children: 'children', label: 'label' }

const curChildren = computed(() => curParent.value?.children || [])

defineExpose({ init })

function init(data) {
  treeData.value = data || []
  refreshOk()
  nextTick(() => {
    curParent.value = firstParentInTree(treeData.value)
    syncMatrixFromCurrent()
  })
}

function onNodeClick(node) {
  if (node && Array.isArray(node.children) && node.children.length) {
    curParent.value = node
    syncMatrixFromCurrent()
  }
}

function syncMatrixFromCurrent() {
  const n = curChildren.value.length
  const saved = curParent.value?.jz
  if (Array.isArray(saved) && saved.length === n && saved.every(r => Array.isArray(r) && r.length === n)) {
    matrix.value = saved.map(r => r.map(v => String(v)))
  } else {
    const m = []
    for (let i = 0; i < n; i++) {
      const row = []
      for (let j = 0; j < n; j++) row.push('1')
      m.push(row)
    }
    matrix.value = m
  }
}

function onCellChange(i, j, val) {
  if (!matrix.value[i]) matrix.value[i] = []
  matrix.value[i][j] = val
  // 自动填倒数
  if (val === '1') {
    if (!matrix.value[j]) matrix.value[j] = []
    matrix.value[j][i] = '1'
  } else if (typeof val === 'string') {
    if (!matrix.value[j]) matrix.value[j] = []
    const parts = val.split('/')
    if (parts.length === 1) matrix.value[j][i] = '1/' + val
    else if (parts[0] === '1') matrix.value[j][i] = parts[1]
    else matrix.value[j][i] = '1/' + val
  }
}

function isParentConfigured(parent) {
  if (!parent || !Array.isArray(parent.children) || !parent.children.length) return false
  if (!Array.isArray(parent.jz) || parent.jz.length !== parent.children.length) return false
  return parent.isOk === true
}

function refreshOk() {
  refreshParentOkFlags(treeData.value, isParentConfigured)
}

async function handleOk() {
  if (!curParent.value) {
    ElMessage.warning('请先选择父节点')
    return
  }
  const n = curChildren.value.length
  if (n < 2) {
    ElMessage.warning('子节点数量必须大于等于 2 才能进行 AHP 赋权')
    return
  }
  try {
    const res = await checkAhpConsistency(matrix.value)
    const data = res?.data || {}
    if (data.ok) {
      curParent.value.jz = matrix.value.map(r => r.slice())
      curParent.value.isOk = true
      ElMessage.success('校验通过！CR=' + Number(data.cr).toFixed(4))
      refreshOk()
      if (allAhpParentsConfigured(treeData.value)) {
        stampWeightAssignAlgorithmOnParents(treeData.value, '层次分析')
        emit('indicator-tree', treeData.value)
      } else {
        ElMessage.info('当前节点已配置 AHP 参数')
      }
    } else {
      curParent.value.isOk = false
      ElMessage.warning('一致性校验不通过，CR=' + Number(data.cr || 0).toFixed(4) + ' > 0.1，请重新调整矩阵')
    }
  } catch (e) {
    ElMessage.error('AHP 一致性校验接口调用失败：' + (e?.message || e))
  }
}

function allAhpParentsConfigured(roots) {
  let ok = true
  const walk = (list) => {
    for (const n of list || []) {
      if (Array.isArray(n.children) && n.children.length) {
        if (!Array.isArray(n.jz) || n.jz.length !== n.children.length || n.isOk !== true) ok = false
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
.ccfx-warn {
  margin: 6px 0 10px;
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid var(--el-color-warning-light-5);
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
  font-size: 12px;
}
</style>
