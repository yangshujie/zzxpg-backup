<template>
  <div class="app-container mission-page combat-profile-editor">
    <section class="page-hero">
      <div class="page-hero__body">
        <div>
          <span class="page-hero__eyebrow">Profile Design Studio</span>
          <h1 class="page-hero__title">{{ pageMode === 'add' ? '新建体系作战剖面' : '编辑体系作战剖面' }}</h1>
          <p class="page-hero__description">
            以“基础信息、能力约束、体系结构”三段式组织作战剖面，让分中心填写与主中心汇聚保持同一视觉语义。
          </p>
          <div class="page-hero__actions">
            <el-button icon="ArrowLeft" @click="goBack">返回列表</el-button>
            <el-button type="primary" @click="submitForm">保存剖面</el-button>
          </div>
        </div>

        <div class="page-hero__panel">
          <div class="glass-card">
            <div class="panel-heading">
              <div>
                <h3 class="panel-title">编辑状态</h3>
                <p class="panel-subtitle">当前页面用于剖面设计、约束配置和结构总览</p>
              </div>
              <span class="status-chip" :class="pageMode === 'add' ? 'status-chip--info' : 'status-chip--warning'">
                {{ pageMode === 'add' ? '新建模式' : '编辑模式' }}
              </span>
            </div>
            <div class="hero-kpi-grid">
              <div v-for="item in editorStats" :key="item.label" class="hero-kpi">
                <div class="hero-kpi__value">{{ item.value }}</div>
                <div class="hero-kpi__label">{{ item.label }}</div>
              </div>
            </div>
          </div>

          <div class="glass-card">
            <div class="panel-heading">
              <div>
                <h3 class="panel-title">协同流程</h3>
                <p class="panel-subtitle">与参考流程页保持一致的节点表达</p>
              </div>
            </div>
            <div class="workflow-strip workflow-strip--compact">
              <div
                v-for="item in workflow"
                :key="item.title"
                :class="['workflow-node', item.state && `workflow-node--${item.state}`]"
              >
                <div class="workflow-node__index">{{ item.index }}</div>
                <div class="workflow-node__title">{{ item.title }}</div>
                <div class="workflow-node__desc">{{ item.desc }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="stack-grid">
      <section class="page-grid-2">
        <div class="panel-card">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">基础信息</h3>
              <p class="panel-subtitle">描述剖面的名称、类型、任务对象和需求约束</p>
            </div>
            <span class="status-chip status-chip--success">步骤 1</span>
          </div>
          <div class="form-grid">
            <div class="field-card">
              <el-form-item label="体系作战剖面名称" prop="profileName">
                <el-input v-model="form.profileName" placeholder="请输入体系作战剖面名称" />
              </el-form-item>
            </div>
            <div class="field-card">
              <el-form-item label="作战剖面类型" prop="profileTypeId">
                <el-select v-model="form.profileTypeId" placeholder="请选择作战剖面类型">
                  <el-option v-for="item in profileTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </div>
            <div class="field-card">
              <el-form-item label="体系作战任务" prop="operationTask">
                <el-input v-model="form.operationTask" placeholder="请输入作战任务或通过阶段配置生成" />
              </el-form-item>
              <el-button type="primary" plain icon="Setting" @click="handleTaskPhaseConfig">任务阶段配置</el-button>
            </div>
            <div class="field-card">
              <el-form-item label="体系作战对象" prop="operationTarget">
                <el-input v-model="form.operationTarget" placeholder="请输入体系作战对象" />
              </el-form-item>
            </div>
            <div class="field-card">
              <el-form-item label="体系作战强度" prop="operationIntensity">
                <el-slider v-model="form.operationIntensity" :min="0" :max="1" :step="0.1" show-stops show-input />
              </el-form-item>
            </div>
            <div class="field-card">
              <el-form-item label="体系作战要求" prop="operationRequirement">
                <el-input v-model="form.operationRequirement" type="textarea" :rows="4" placeholder="请输入体系作战要求" />
              </el-form-item>
            </div>
          </div>
        </div>

        <div class="stack-grid">
          <div class="panel-card">
            <div class="panel-heading">
              <div>
                <h3 class="panel-title">能力摘要</h3>
                <p class="panel-subtitle">保存前快速确认约束与蓝方能力是否完整</p>
              </div>
              <span class="status-chip status-chip--info">步骤 2</span>
            </div>
            <div class="mini-stat-grid">
              <div class="mini-stat">
                <div class="mini-stat__value">{{ Math.round((Number(form.operationIntensity || 0) || 0) * 100) }}%</div>
                <div class="mini-stat__label">对抗强度</div>
              </div>
              <div class="mini-stat">
                <div class="mini-stat__value">{{ form.blueMaxRadius || 0 }}</div>
                <div class="mini-stat__label">蓝方半径 / 公里</div>
              </div>
              <div class="mini-stat">
                <div class="mini-stat__value">{{ form.blueReactionTime || 0 }}</div>
                <div class="mini-stat__label">响应时长 / 分钟</div>
              </div>
              <div class="mini-stat">
                <div class="mini-stat__value">{{ Array.isArray(form.blueStrategy) ? form.blueStrategy.length : 0 }}</div>
                <div class="mini-stat__label">已选策略</div>
              </div>
            </div>
          </div>

          <div class="panel-card">
            <div class="panel-heading">
              <div>
                <h3 class="panel-title">蓝方对抗策略</h3>
                <p class="panel-subtitle">策略选择结果将同步到任务设计视图</p>
              </div>
            </div>
            <el-form-item label="蓝方对抗策略" prop="blueStrategy">
              <el-select v-model="form.blueStrategy" multiple placeholder="请选择蓝方对抗策略">
                <el-option v-for="item in blueStrategyOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <div class="tag-cloud">
              <span v-for="item in selectedStrategyLabels" :key="item" class="tag-cloud__item">{{ item }}</span>
              <span v-if="!selectedStrategyLabels.length" class="tag-cloud__item">尚未选择策略</span>
            </div>
          </div>
        </div>
      </section>

      <section class="panel-card">
        <div class="panel-heading">
          <div>
            <h3 class="panel-title">环境与蓝方约束</h3>
            <p class="panel-subtitle">通过结构化卡片描述环境工况和蓝方作战能力</p>
          </div>
          <span class="status-chip status-chip--warning">步骤 3</span>
        </div>
        <div class="form-grid">
          <div class="field-card">
            <div class="constraint-card__head">
              <div>
                <div class="constraint-card__title">作战环境工况</div>
                <div class="constraint-card__meta">用于约束蓝方能力计算与地图展示</div>
              </div>
              <el-button type="primary" plain icon="Setting" @click="handleEnvironmentConfig">工况设置</el-button>
            </div>
            <p class="side-note">
              建议在此页完成时间窗、空间范围、气象条件和电磁环境的录入，以便后续任务网络和判据页引用统一数据。
            </p>
          </div>

          <div class="field-card">
            <el-form-item label="蓝方最大作战半径" prop="blueMaxRadius">
              <el-input-number v-model="form.blueMaxRadius" :min="0" :max="10000" :step="100" controls-position="right" />
            </el-form-item>
            <div class="metric-helper">单位：公里</div>
          </div>

          <div class="field-card">
            <el-form-item label="蓝方响应时间" prop="blueReactionTime">
              <el-input-number v-model="form.blueReactionTime" :min="0" :max="120" :step="5" controls-position="right" />
            </el-form-item>
            <div class="metric-helper">单位：分钟</div>
          </div>

          <div class="field-card">
            <el-form-item label="蓝方持续作战能力" prop="blueSustainTime">
              <el-input-number v-model="form.blueSustainTime" :min="0" :max="72" :step="1" controls-position="right" />
            </el-form-item>
            <div class="metric-helper">单位：小时</div>
          </div>
        </div>
      </section>

      <section class="page-grid-2 structure-layout">
        <div class="panel-card">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">体系结构树</h3>
              <p class="panel-subtitle">红蓝双方结构数据在同一栏中对照展示</p>
            </div>
            <span class="status-chip status-chip--success">步骤 4</span>
          </div>

          <div class="stack-grid">
            <div class="field-card tree-card">
              <div class="constraint-card__head">
                <div>
                  <div class="constraint-card__title">红方体系</div>
                  <div class="constraint-card__meta">导入装备结构后自动展开</div>
                </div>
                <el-button type="primary" plain icon="Upload" @click="handleRedEquipmentImport">装备构型导入</el-button>
              </div>
              <el-tree :data="redTreeData" :props="treeProps" node-key="id" default-expand-all class="combat-tree" />
            </div>

            <div class="field-card tree-card">
              <div class="constraint-card__head">
                <div>
                  <div class="constraint-card__title">蓝方体系</div>
                  <div class="constraint-card__meta">与红方结构并行编辑</div>
                </div>
                <el-button type="primary" plain icon="Upload" @click="handleBlueEquipmentImport">装备构型导入</el-button>
              </div>
              <el-tree :data="blueTreeData" :props="treeProps" node-key="id" default-expand-all class="combat-tree" />
            </div>
          </div>
        </div>

        <div class="panel-card map-card">
          <div class="panel-heading">
            <div>
              <h3 class="panel-title">态势地图</h3>
              <p class="panel-subtitle">为后续指标画布和网络图提供统一空间背景</p>
            </div>
            <div class="legend-row">
              <span class="legend-chip"><span class="legend-chip__dot" style="background:#22d3ee"></span>覆盖范围</span>
              <span class="legend-chip"><span class="legend-chip__dot" style="background:#fbbf24"></span>任务边界</span>
              <span class="legend-chip"><span class="legend-chip__dot" style="background:#fb7185"></span>风险区</span>
            </div>
          </div>
          <div class="map-stage">
            <CesiumViewer ref="cesiumViewer" />
            <div class="map-overlay">
              <div class="mini-stat-grid">
                <div class="mini-stat">
                  <div class="mini-stat__value">{{ form.blueMaxRadius || 0 }}</div>
                  <div class="mini-stat__label">覆盖半径</div>
                </div>
                <div class="mini-stat">
                  <div class="mini-stat__value">{{ form.blueSustainTime || 0 }}</div>
                  <div class="mini-stat__label">持续时间</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </el-form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import CesiumViewer from '@/components/CesiumViewer.vue'
import { getCombatProfile, saveOrUpdateCombatProfile } from '@/api/systemPlus/systemCooperation/combatProfile'

const router = useRouter()
const route = useRoute()
const formRef = ref()

const pageMode = ref('add')
const editId = ref(null)

const form = reactive({
  profileName: '',
  profileTypeId: '',
  operationTask: '',
  operationTarget: '',
  operationIntensity: 0.5,
  operationRequirement: '',
  blueMaxRadius: 1000,
  blueRadiusUnit: '公里',
  blueReactionTime: 30,
  blueReactionUnit: '分钟',
  blueSustainTime: 24,
  blueSustainUnit: '小时',
  blueStrategy: []
})

const rules = reactive({
  profileName: [{ required: true, message: '体系作战剖面名称不能为空', trigger: 'blur' }],
  profileTypeId: [{ required: true, message: '作战剖面类型不能为空', trigger: 'change' }],
  operationTask: [{ required: true, message: '体系作战任务不能为空', trigger: 'blur' }],
  operationTarget: [{ required: true, message: '体系作战对象不能为空', trigger: 'blur' }],
  operationRequirement: [{ required: true, message: '体系作战要求不能为空', trigger: 'blur' }],
  blueMaxRadius: [{ required: true, message: '蓝方最大作战半径不能为空', trigger: 'blur' }],
  blueReactionTime: [{ required: true, message: '蓝方响应时间不能为空', trigger: 'blur' }],
  blueSustainTime: [{ required: true, message: '蓝方持续作战能力不能为空', trigger: 'blur' }],
  blueStrategy: [{ required: true, message: '蓝方对抗策略不能为空', trigger: 'change' }]
})

const profileTypeOptions = ref([
  { value: '1', label: '航天侦察装备子体系' },
  { value: '2', label: '太空态势感知装备子体系' },
  { value: '3', label: '太空攻防装备子体系' },
  { value: '4', label: '航天发射装备子体系' },
  { value: '5', label: '航天测运控装备子体系' },
  { value: '6', label: '航天装备联合任务体系' }
])

const blueStrategyOptions = ref([
  { value: 'mobility', label: '机动规避' },
  { value: 'relay', label: '中继链路重构' },
  { value: 'backup', label: '备份链路切换' },
  { value: 'jam-resistance', label: '抗干扰模式' },
  { value: 'dynamic-scheduling', label: '动态调度' }
])

const treeProps = {
  children: 'children',
  label: 'label'
}

const redTreeData = ref([
  {
    id: 1,
    label: '红方作战体系',
    children: [
      {
        id: 2,
        label: '侦察预警系统',
        children: [
          { id: 3, label: '卫星侦察系统' },
          { id: 4, label: '地面雷达系统' },
          { id: 5, label: '预警机系统' }
        ]
      },
      {
        id: 6,
        label: '指挥控制系统',
        children: [
          { id: 7, label: '作战指挥中心' },
          { id: 8, label: '通信网络系统' }
        ]
      }
    ]
  }
])

const blueTreeData = ref([
  {
    id: 1,
    label: '蓝方作战体系',
    children: [
      {
        id: 2,
        label: '防御系统',
        children: [
          { id: 3, label: '反导系统' },
          { id: 4, label: '电子对抗系统' },
          { id: 5, label: '太空防御系统' }
        ]
      },
      {
        id: 6,
        label: '攻击系统',
        children: [
          { id: 7, label: '导弹系统' },
          { id: 8, label: '太空攻击系统' }
        ]
      }
    ]
  }
])

const workflow = [
  { index: '01', title: '设计下发', desc: '创建基础剖面', state: 'done' },
  { index: '02', title: '分中心填写', desc: '完善约束与结构', state: 'active' },
  { index: '03', title: '上报主中心', desc: '提交汇总信息' },
  { index: '04', title: '汇集整合', desc: '进入综合评估' }
]

const editorStats = computed(() => [
  { label: '当前强度', value: `${Math.round((Number(form.operationIntensity || 0) || 0) * 100)}%` },
  { label: '蓝方响应', value: `${form.blueReactionTime || 0} 分钟` },
  { label: '蓝方持续', value: `${form.blueSustainTime || 0} 小时` },
  { label: '策略数量', value: Array.isArray(form.blueStrategy) ? form.blueStrategy.length : 0 }
])

const selectedStrategyLabels = computed(() => {
  const selected = Array.isArray(form.blueStrategy) ? form.blueStrategy : []
  return blueStrategyOptions.value.filter(item => selected.includes(item.value)).map(item => item.label)
})

function handleTaskPhaseConfig() {
  ElMessage.info('任务阶段配置功能待实现')
}

function handleEnvironmentConfig() {
  ElMessage.info('工况设置功能待实现')
}

function handleRedEquipmentImport() {
  ElMessage.info('红方装备构型导入功能待实现')
}

function handleBlueEquipmentImport() {
  ElMessage.info('蓝方装备构型导入功能待实现')
}

async function submitForm() {
  try {
    await formRef.value.validate()
    const submitData = {
      profileId: pageMode.value === 'edit' ? editId.value : null,
      basicInfo: {
        profileName: form.profileName,
        profileTypeId: form.profileTypeId,
        operationTask: form.operationTask,
        operationTarget: form.operationTarget,
        operationIntensity: form.operationIntensity,
        operationRequirement: form.operationRequirement,
        blueMaxRadius: form.blueMaxRadius,
        blueRadiusUnit: form.blueRadiusUnit,
        blueReactionTime: form.blueReactionTime,
        blueReactionUnit: form.blueReactionUnit,
        blueSustainTime: form.blueSustainTime,
        blueSustainUnit: form.blueSustainUnit,
        blueStrategy: form.blueStrategy
      }
    }
    await saveOrUpdateCombatProfile(submitData)
    ElMessage.success(pageMode.value === 'add' ? '作战剖面新增成功' : '作战剖面修改成功')
    goBack()
  } catch (error) {
    if (error) {
      ElMessage.error('保存失败，请检查表单后重试')
    }
  }
}

function goBack() {
  router.push('/system-cooperation/combat-profile')
}

async function loadEditData() {
  try {
    const response = await getCombatProfile(editId.value)
    const profileData = response.data || {}
    form.profileName = profileData.profileName || ''
    form.profileTypeId = profileData.profileTypeId || ''
    form.operationTask = profileData.operationTask || ''
    form.operationTarget = profileData.operationTarget || ''
    form.operationIntensity = profileData.operationIntensity ?? 0.5
    form.operationRequirement = profileData.operationRequirement || ''
    form.blueMaxRadius = profileData.blueMaxRadius || 1000
    form.blueRadiusUnit = profileData.blueRadiusUnit || '公里'
    form.blueReactionTime = profileData.blueReactionTime || 30
    form.blueReactionUnit = profileData.blueReactionUnit || '分钟'
    form.blueSustainTime = profileData.blueSustainTime || 24
    form.blueSustainUnit = profileData.blueSustainUnit || '小时'
    form.blueStrategy = Array.isArray(profileData.blueStrategy) ? profileData.blueStrategy : (profileData.blueStrategy ? [profileData.blueStrategy] : [])
  } catch (error) {
    ElMessage.error('加载数据失败')
    goBack()
  }
}

onMounted(() => {
  if (route.params.id) {
    pageMode.value = 'edit'
    editId.value = Number(route.params.id)
    loadEditData()
  }
})
</script>

<style scoped lang="scss">
.combat-profile-editor {
  .workflow-strip--compact {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .constraint-card__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 14px;
  }

  .constraint-card__title {
    color: var(--mc-text-primary);
    font-weight: 600;
  }

  .constraint-card__meta,
  .metric-helper {
    margin-top: 6px;
    color: var(--mc-text-tertiary);
    font-size: 12px;
  }

  .structure-layout {
    align-items: stretch;
  }

  .tree-card {
    min-height: 320px;
  }

  .combat-tree {
    min-height: 220px;
  }

  .map-card {
    min-height: 100%;
  }

  .map-stage {
    position: relative;
    overflow: hidden;
    min-height: 660px;
    border-radius: 18px;
    border: 1px solid rgba(86, 122, 173, 0.18);
    background: rgba(6, 12, 24, 0.76);
  }

  .map-stage :deep(.cesium-viewer),
  .map-stage :deep(.cesium-container),
  .map-stage :deep(canvas) {
    width: 100%;
    height: 100%;
    min-height: 660px;
  }

  .map-overlay {
    position: absolute;
    top: 16px;
    right: 16px;
    width: min(280px, calc(100% - 32px));
    padding: 16px;
    border-radius: 18px;
    background: color-mix(in srgb, var(--mc-bg-elevated) 72%, transparent);
    border: 1px solid rgba(86, 122, 173, 0.2);
    backdrop-filter: blur(14px);
  }
}

@media (max-width: 1280px) {
  .combat-profile-editor .workflow-strip--compact {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .combat-profile-editor .workflow-strip--compact {
    grid-template-columns: 1fr;
  }
}
</style>
