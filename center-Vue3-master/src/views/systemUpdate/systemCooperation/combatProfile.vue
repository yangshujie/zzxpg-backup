<template>
  <div class="app-container mission-page combat-profile-page">
    <section class="page-hero">
      <div class="page-hero__body">
        <div>
          <span class="page-hero__eyebrow">System Cooperation</span>
          <h1 class="page-hero__title">体系作战剖面协同设计</h1>
          <p class="page-hero__description">
            将作战剖面、环境约束、蓝方能力和体系结构统一到一个深色任务控制视图中，支持从列表态快速进入设计与编辑。
          </p>
          <div class="page-hero__actions">
            <el-button type="primary" icon="Plus" @click="handleAdd">新建剖面</el-button>
            <el-button icon="Refresh" @click="getList">刷新列表</el-button>
          </div>
        </div>

        <div class="page-hero__panel">
          <div class="glass-card">
            <div class="panel-heading">
              <div>
                <h3 class="panel-title">任务概览</h3>
                <p class="panel-subtitle">列表数据与协同流程状态的摘要视图</p>
              </div>
              <span class="status-chip status-chip--success">在线协同</span>
            </div>
            <div class="hero-kpi-grid">
              <div v-for="item in overviewStats" :key="item.label" class="hero-kpi">
                <div class="hero-kpi__value">{{ item.value }}</div>
                <div class="hero-kpi__label">{{ item.label }}</div>
              </div>
            </div>
          </div>

          <div class="glass-card">
            <div class="panel-heading">
              <div>
                <h3 class="panel-title">剖面分布</h3>
                <p class="panel-subtitle">当前分页数据中的类型与强度概况</p>
              </div>
            </div>
            <div class="score-bars">
              <div v-for="item in profileDistribution" :key="item.label" class="score-bar">
                <div class="score-bar__head">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
                <div class="score-bar__track">
                  <div :class="['score-bar__fill', item.tone && `score-bar__fill--${item.tone}`]" :style="{ width: `${item.percent}%` }"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="panel-card">
      <div class="panel-heading">
        <div>
          <h3 class="panel-title">检索条件</h3>
          <p class="panel-subtitle">按剖面名称和类型筛选协同设计数据</p>
        </div>
        <span class="status-chip status-chip--info">实时筛选</span>
      </div>
      <el-form ref="queryRef" :model="queryParams" label-width="110px">
        <div class="form-grid form-grid--2">
          <div class="field-card">
            <el-form-item label="剖面名称" prop="name">
              <el-input v-model="queryParams.name" placeholder="请输入体系作战剖面名称" clearable @keyup.enter="handleQuery" />
            </el-form-item>
          </div>
          <div class="field-card">
            <el-form-item label="剖面类型" prop="type">
              <el-select v-model="queryParams.type" placeholder="请选择作战剖面类型" clearable>
                <el-option v-for="item in profileTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </div>
        </div>
        <div class="compact-actions">
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </div>
      </el-form>
    </section>

    <section class="panel-card table-card">
      <div class="panel-heading">
        <div>
          <h3 class="panel-title">剖面列表</h3>
          <p class="panel-subtitle">以卡片化列表容器承载作战设计数据</p>
        </div>
        <span class="status-chip status-chip--warning">共 {{ total }} 条</span>
      </div>

      <el-table v-loading="loading" :data="profileList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="剖面名称" min-width="180" prop="profileName" show-overflow-tooltip />
        <el-table-column label="类型" min-width="160" prop="profileTypeId">
          <template #default="{ row }">
            <span class="status-chip status-chip--info">{{ getProfileTypeLabel(row.profileTypeId) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="作战任务" min-width="160" prop="operationTask" show-overflow-tooltip />
        <el-table-column label="作战对象" min-width="140" prop="operationTarget" show-overflow-tooltip />
        <el-table-column label="对抗强度" width="170" prop="operationIntensity">
          <template #default="{ row }">
            <div class="intensity-cell">
              <span>{{ Math.round((Number(row.operationIntensity || 0) || 0) * 100) }}%</span>
              <el-progress :percentage="Math.round((Number(row.operationIntensity || 0) || 0) * 100)" :stroke-width="8" :show-text="false" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="蓝方能力摘要" min-width="240">
          <template #default="{ row }">
            <div class="capability-summary">
              <span>半径 {{ row.blueMaxRadius || 0 }} 公里</span>
              <span>响应 {{ row.blueReactionTime || 0 }} 分钟</span>
              <span>持续 {{ row.blueSustainTime || 0 }} 小时</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180" prop="createTime">
          <template #default="{ row }">
            <span>{{ parseTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="180" align="center">
          <template #default="{ row }">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">编辑</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </section>
  </div>
</template>

<script setup name="CombatProfile">
import { computed, reactive, ref, toRefs } from 'vue'
import { useRouter } from 'vue-router'
import { listCombatProfile, delCombatProfile } from '@/api/systemPlus/systemCooperation/combatProfile'

const { proxy } = getCurrentInstance()
const router = useRouter()

const profileList = ref([])
const loading = ref(true)
const ids = ref([])
const total = ref(0)

const profileTypeOptions = ref([
  { value: '1', label: '航天侦察装备子体系' },
  { value: '2', label: '太空态势感知装备子体系' },
  { value: '3', label: '太空攻防装备子体系' },
  { value: '4', label: '航天发射装备子体系' },
  { value: '5', label: '航天测运控装备子体系' },
  { value: '6', label: '航天装备联合任务体系' }
])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    type: undefined
  }
})

const { queryParams } = toRefs(data)

const overviewStats = computed(() => {
  const rows = profileList.value || []
  const avgIntensity = rows.length
    ? Math.round(rows.reduce((sum, item) => sum + (Number(item.operationIntensity || 0) || 0), 0) / rows.length * 100)
    : 0
  const avgReaction = rows.length
    ? Math.round(rows.reduce((sum, item) => sum + (Number(item.blueReactionTime || 0) || 0), 0) / rows.length)
    : 0
  return [
    { label: '当前分页剖面', value: rows.length },
    { label: '全量记录', value: total.value },
    { label: '平均对抗强度', value: `${avgIntensity}%` },
    { label: '平均响应时长', value: `${avgReaction} 分钟` }
  ]
})

const profileDistribution = computed(() => {
  const rows = profileList.value || []
  const totalRows = rows.length || 1
  const highIntensity = rows.filter(item => (Number(item.operationIntensity || 0) || 0) >= 0.7).length
  const hasBlueStrategy = rows.filter(item => {
    const strategy = item.blueStrategy
    return Array.isArray(strategy) ? strategy.length > 0 : !!strategy
  }).length
  const multiTypeCount = new Set(rows.map(item => item.profileTypeId).filter(Boolean)).size
  return [
    { label: '高强度剖面', value: `${highIntensity} 项`, percent: Math.round(highIntensity / totalRows * 100), tone: 'danger' },
    { label: '已配置蓝方策略', value: `${hasBlueStrategy} 项`, percent: Math.round(hasBlueStrategy / totalRows * 100) },
    { label: '覆盖类型数量', value: `${multiTypeCount} 类`, percent: Math.min(multiTypeCount * 16, 100), tone: 'warning' }
  ]
})

function getProfileTypeLabel(value) {
  return profileTypeOptions.value.find(item => item.value === value)?.label || '未分类'
}

function getList() {
  loading.value = true
  listCombatProfile(queryParams.value).then(response => {
    profileList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
}

function handleAdd() {
  router.push('/system-cooperation/combat-profile/add')
}

function handleUpdate(row) {
  const id = row.id || ids.value[0]
  router.push(`/system-cooperation/combat-profile/edit/${id}`)
}

function handleDelete(row) {
  const targetId = row.id || ids.value
  proxy.$modal.confirm(`是否确认删除体系作战剖面编号为 "${targetId}" 的数据项？`).then(() => {
    return delCombatProfile(targetId)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

getList()
</script>

<style scoped lang="scss">
.combat-profile-page {
  .intensity-cell {
    display: grid;
    gap: 8px;
  }

  .capability-summary {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    span {
      padding: 4px 10px;
      border-radius: 999px;
      background: rgba(34, 211, 238, 0.08);
      border: 1px solid rgba(34, 211, 238, 0.14);
      color: var(--mc-text-secondary);
      font-size: 12px;
    }
  }
}
</style>
