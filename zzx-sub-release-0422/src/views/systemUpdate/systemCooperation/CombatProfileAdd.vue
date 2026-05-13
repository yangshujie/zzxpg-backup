<template>
  <div class="subpage-container combat-profile-add-page">
    <!-- 任务阶段配置弹窗 -->
    <TaskStageConfig v-model="taskStageDialogVisible" ref="taskStageConfigRef" :external-task-data="externalTaskData"
      @save="handleTaskStageSave" @close="handleTaskStageClose" />

    <!-- 顶部返回和基础信息 -->
    <div class="detail-header">
      <div class="header-content">
        <el-button class="back-btn" @click="goBack" icon="ArrowLeft">返回</el-button>
        <el-button type="primary" size="medium" @click="submitForm">保存</el-button>
        <div class="title-section">
          <h2 class="project-title">{{ pageMode === 'add' ? '新增体系作战剖面' : '编辑体系作战剖面' + (form.id ? ' - ID: ' + form.id :
            '') }}</h2>
          <el-tag type="primary" effect="dark" class="status-badge">编辑中</el-tag>
        </div>
        <!-- <div class="action-buttons">
          <el-button type="primary" size="large" @click="submitForm">保存作战剖面</el-button>
          <el-button size="large" @click="goBack">取消</el-button>
        </div> -->
      </div>
    </div>

    <!-- 主要表单区域 -->
    <el-card class="tech-card form-container">
      <template #header>
        <div class="card-title">
          <el-icon>
            <Document />
          </el-icon> 作战剖面基本信息
        </div>
      </template>

      <!-- 第一行：名称、类型、任务 -->
      <el-row :gutter="20" class="form-row">
        <el-col :span="8">
          <el-form-item label="体系作战剖面名称" prop="profileName">
            <el-input v-model="form.profileName" placeholder="请输入体系作战剖面名称" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="作战剖面类型" prop="profileTypeId">
            <el-select v-model="form.profileTypeId" placeholder="请选择作战剖面类型" style="width: 100%">
              <el-option v-for="item in profileTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="体系作战任务" prop="operationTask">
            <el-button type="primary" icon="Edit" @click="handleTaskPhaseConfig">任务阶段配置</el-button>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 第二行：作战对象、作战烈度、作战要求 -->
      <el-row :gutter="20" class="form-row">
        <el-col :span="8">
          <el-form-item label="体系作战对象" prop="operationTarget">
            <el-input v-model="form.operationTarget" placeholder="请输入体系作战对象" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="体系作战烈度" prop="operationIntensity">
            <div class="intensity-slider">
              <el-slider v-model="form.operationIntensity" :min="0" :max="1" :step="0.1" show-stops show-input />
              <div class="slider-labels">
                <span>0</span>
                <span>0.5</span>
                <span>1</span>
              </div>
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="体系作战要求" prop="operationRequirement">
            <el-input v-model="form.operationRequirement" placeholder="请输入体系作战要求" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 第三行：环境工况、蓝方作战能力、对抗策略 -->
      <el-row :gutter="20" class="form-row large-row">
        <el-col :span="8">
          <el-form-item label="体系作战环境工况" prop="environmentCondition">
            <el-button type="primary" icon="Setting" @click="handleEnvironmentConfig">工况设置</el-button>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="蓝方体系作战能力">
            <div class="blue-capability-panel">
              <el-form-item label="最大作战半径" prop="blueMaxRadius" label-width="120px">
                <el-input-number v-model="form.blueMaxRadius" :min="0" :max="10000" :step="100"
                  controls-position="right" style="width: 80%" />
                <span class="unit">公里</span>
              </el-form-item>
              <el-form-item label="反应时间" prop="blueReactionTime" label-width="120px">
                <el-input-number v-model="form.blueReactionTime" :min="0" :max="120" :step="5" controls-position="right"
                  style="width: 80%" />
                <span class="unit">分钟</span>
              </el-form-item>
              <el-form-item label="持续作战能力" prop="blueSustainTime" label-width="120px">
                <el-input-number v-model="form.blueSustainTime" :min="0" :max="72" :step="1" controls-position="right"
                  style="width: 80%" />
                <span class="unit">小时</span>
              </el-form-item>
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="蓝方对抗策略" prop="blueStrategy">
            <el-select v-model="form.blueStrategy" multiple placeholder="请选择蓝方对抗策略" style="width: 100%">
              <el-option v-for="item in blueStrategyOptions" :key="item.value" :label="item.label"
                :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <!-- 作战剖面属性结构 -->
    <el-row :gutter="20" class="structure-row" style="height: 500px;">
      <el-col :span="6" style="height: 100%;">
        <el-card class="tech-card structure-container" style="height: 100%;">
          <template #header>
            <div class="card-title">
              <el-icon>
                <Connection />
              </el-icon> 作战剖面属性结构
            </div>
          </template>

          <div class="tree-container" style="height: calc(100% - 60px);">
            <div class="tree-header">
              <span class="tree-title">红方</span>
              <div class="import-buttons">
                <el-button type="primary" size="small" icon="Upload" @click="handleRedEquipmentImport">装备导入</el-button>
                <el-button type="success" size="small" icon="FolderOpened"
                  @click="handleRedEquipmentGroupImport">装备构型导入</el-button>
              </div>
            </div>
            <div class="tree-scroll-container">
              <el-tree :data="redTreeData" :props="treeProps" node-key="id" default-expand-all class="combat-tree" />
            </div>

            <div class="tree-header">
              <span class="tree-title">蓝方</span>
              <div class="import-buttons">
                <el-button type="primary" size="small" icon="Upload" @click="handleBlueEquipmentImport">装备导入</el-button>
                <el-button type="success" size="small" icon="FolderOpened"
                  @click="handleBlueEquipmentGroupImport">装备构型导入</el-button>
              </div>
            </div>
            <div class="tree-scroll-container">
              <el-tree :data="blueTreeData" :props="treeProps" node-key="id" default-expand-all class="combat-tree" />
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="18" style="height: 100%;">

        <div class="cesium-wrapper" style="height: 100%; width: 100%;">
          <CesiumViewer ref="cesiumViewer" />
        </div>

      </el-col>
    </el-row>
    <!-- 工况设置对话框 -->
    <OperatingConfiguration v-model="envConfigDialogVisible" :env-data="envConditionData" @save="handleEnvConfigSave"
      @subSave="handleEnvConfigSubSave" @loadFromParent="handleEnvConfigLoadFromParent" />

    <!-- 装备导入对话框 -->
    <el-dialog v-model="equipmentDialogVisible" :title="currentSide === 0 ? '红方装备导入' : '蓝方装备导入'" width="1100px"
      append-to-body>
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input v-model="equipmentSearchName" placeholder="请输入装备名称搜索" style="width: 300px; margin-bottom: 15px;"
          clearable @input="handleEquipmentSearch">
          <template #append>
            <el-button icon="Search" @click="handleEquipmentSearch" />
          </template>
        </el-input>
      </div>

      <el-table :data="filteredEquipmentList" stripe border height="400"
        @selection-change="(selection) => selectedEquipment = selection">
        <el-table-column type="selection" width="55" fixed="left" />
        <el-table-column prop="name" label="装备名称" width="150" fixed="left" />
        <el-table-column prop="equipmentType" label="装备类型" width="120"
          :formatter="(row) => getEquipmentTypeLabel(row.equipmentType)" />
        <el-table-column prop="side" label="阵营" width="100" :formatter="(row) => row.side == 0 ? '红方' : '蓝方'" />
        <el-table-column prop="status" label="状态" width="100" :formatter="(row) => row.status === 0 ? '正常' : '禁用'" />
        <el-table-column prop="createTime" label="创建时间" width="180" :formatter="(row) => formatDate(row.createTime)" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="parameters" label="参数" min-width="200" :formatter="(row) => row.parameters || '无'" />
        <template #empty>
          <div class="empty-text">暂无装备数据</div>
        </template>
      </el-table>

      <!-- 装备列表分页 -->
      <div class="pagination-container">
        <el-pagination v-model:current-page="equipmentPagination.pageNum"
          v-model:page-size="equipmentPagination.pageSize" :page-sizes="[10, 20, 50, 100]"
          :total="equipmentPagination.total" layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleEquipmentSizeChange" @current-change="handleEquipmentCurrentChange" />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelEquipmentImport">取消</el-button>
          <el-button type="primary" @click="confirmEquipmentImport">确认导入</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 装备构型导入对话框 -->
    <el-dialog v-model="equipmentGroupDialogVisible" :title="currentSide === 0 ? '红方装备构型导入' : '蓝方装备构型导入'" width="1100"
      append-to-body>
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input v-model="equipmentGroupSearchName" placeholder="请输入编组名称搜索" style="width: 300px; margin-bottom: 15px;"
          clearable @input="handleEquipmentGroupSearch">
          <template #append>
            <el-button icon="Search" @click="handleEquipmentGroupSearch" />
          </template>
        </el-input>
      </div>

      <el-table :data="filteredEquipmentGroupList" stripe border height="400" width="1100"
        @selection-change="(selection) => selectedEquipmentGroup = selection">
        <el-table-column type="selection" width="55" fixed="left" />
        <el-table-column prop="name" label="编组名称" width="150" fixed="left" />
        <el-table-column prop="equipmentType" label="装备类型" width="120"
          :formatter="(row) => row.equipmentType ? row.equipmentType.join(', ') : '无'" />
        <el-table-column prop="equipmentLevel" label="装备等级" width="120"
          :formatter="(row) => getEquipmentLevelLabel(row.equipmentLevel)" />
        <el-table-column prop="side" label="阵营" width="100" :formatter="(row) => row.side == 0 ? '红方' : '蓝方'" />
        <el-table-column prop="formationDetail" label="包含装备" width="200"
          :formatter="(row) => row.formationDetail ? row.formationDetail.map(item => item.name).join(', ') : '无'" />
        <el-table-column prop="createTime" label="创建时间" width="180" :formatter="(row) => formatDate(row.createTime)" />
        <el-table-column prop="description" label="描述" min-width="200" :formatter="(row) => row.description || '无'" />
        <template #empty>
          <div class="empty-text">暂无装备编组数据</div>
        </template>
      </el-table>

      <!-- 装备构型列表分页 -->
      <div class="pagination-container">
        <el-pagination v-model:current-page="equipmentGroupPagination.pageNum"
          v-model:page-size="equipmentGroupPagination.pageSize" :page-sizes="[10, 20, 50, 100]"
          :total="equipmentGroupPagination.total" layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleEquipmentGroupSizeChange" @current-change="handleEquipmentGroupCurrentChange" />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelEquipmentGroupImport">取消</el-button>
          <el-button type="primary" @click="confirmEquipmentGroupImport">确认导入</el-button>
        </div>
      </template>
    </el-dialog>

  </div>

</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import CesiumViewer from '@/components/CesiumViewer.vue'
import OperatingConfiguration from './components/OperatingConfiguration.vue'
import TaskStageConfig from './components/TaskStageConfig.vue'
import { saveOrUpdateCombatProfile, updateProfile, profileDetail } from "@/api/systemPlus/systemCooperation/combatProfile"
import { listEquipmentBySide, listEquipmentGroupBySide, getEquipmentGroup } from "@/api/systemPlus/systemCooperation/equipment"
import { queryTask, getTaskList } from "@/api/systemPlus/systemCooperation/task"

const router = useRouter()
const route = useRoute()

// 页面模式：add 或 edit
const pageMode = ref('add')
// 编辑模式下的ID
const editId = ref(null)

// 表单数据
const form = reactive({
  profileName: '',
  profileTypeId: '',
  operationTask: [],
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


// 工况设置对话框显示状态
const envConfigDialogVisible = ref(false)

// 任务阶段配置相关
const taskStageConfigRef = ref(null)
const taskStageDialogVisible = ref(false)
const taskStageData = ref({
  taskStages: [
    {
      taskName: '侦察阶段',
      taskStage: '1',
      relationCriterion: [],
      order: 1
    },
    {
      taskName: '指挥决策',
      taskStage: '2',
      relationCriterion: [],
      order: 2
    },
    {
      taskName: '火力打击',
      taskStage: '3',
      relationCriterion: [],
      order: 3
    },
    {
      taskName: '效果评估',
      taskStage: '4',
      relationCriterion: [],
      order: 4
    }
  ]
})
// 从API获取的任务数据
const externalTaskData = ref([])
// 装备编组的原始信息（包含formationDetail）
const equipmentGroupDetails = ref({
  red: {},
  blue: {}
})

// 表单验证规则
const rules = reactive({
  profileName: [{ required: true, message: "体系作战剖面名称不能为空", trigger: "blur" }],
  profileTypeId: [{ required: true, message: "作战剖面类型不能为空", trigger: "change" }],
  operationTask: [{ required: true, message: "体系作战任务不能为空", trigger: "blur" }],
  operationTarget: [{ required: true, message: "体系作战对象不能为空", trigger: "blur" }],
  operationIntensity: [{ required: true, message: "体系作战烈度不能为空", trigger: "blur" }],
  operationRequirement: [{ required: true, message: "体系作战要求不能为空", trigger: "blur" }],
  blueMaxRadius: [{ required: true, message: "蓝方最大作战半径不能为空", trigger: "blur" }],
  blueReactionTime: [{ required: true, message: "蓝方反应时间不能为空", trigger: "blur" }],
  blueSustainTime: [{ required: true, message: "蓝方持续作战时间不能为空", trigger: "blur" }],
  blueStrategy: [{ required: true, message: "蓝方对抗策略不能为空", trigger: "change" }]
})

// 作战剖面类型选项
const profileTypeOptions = ref([
  { value: '1', label: '航天侦察装备子体系' },
  { value: '2', label: '太空态势感知装备子体系' },
  { value: '3', label: '太空攻防装备子体系' },
  { value: '4', label: '航天发射装备子体系' },
  { value: '5', label: '航天测运控装备子体系' },
  { value: '6', label: '航天装备联合作战体系' }
])

// 作战任务选项
const operationTaskOptions = ref([
  { value: '战场侦察', label: '战场侦察' },
  { value: '指挥决策', label: '指挥决策' },
  { value: '火力打击', label: '火力打击' },
  { value: '电子对抗', label: '电子对抗' },
  { value: '区域搜索发现', label: '区域搜索发现' },
  { value: '目标识别确认', label: '目标识别确认' },
  { value: '跟踪监视', label: '跟踪监视' },
  { value: '通信保障', label: '通信保障' },
  { value: '效果评估', label: '效果评估' }
])

// 蓝方对抗策略选项
const blueStrategyOptions = ref([
  { value: '卫星干扰', label: '卫星干扰' },
  { value: '电磁静默', label: '电磁静默' },
  { value: '雷达反制', label: '雷达反制' },
  { value: '通信阻断', label: '通信阻断' },
  { value: '光电干扰', label: '光电干扰' },
  { value: '角反射器干扰', label: '角反射器干扰' }
])

// 树形结构配置
const treeProps = {
  children: 'children',
  label: 'label'
}

// 红方树形数据
const redTreeData = ref([])

// 蓝方树形数据
const blueTreeData = ref([])

// 装备列表数据（用于编辑页面数据填充）
const blueEquipmentList = ref([])
const redEquipmentList = ref([])

// 装备导入相关状态
const equipmentDialogVisible = ref(false)
const equipmentGroupDialogVisible = ref(false)
const currentSide = ref(0) // 0:红方, 1:蓝方
const equipmentList = ref([])
const equipmentGroupList = ref([])
const selectedEquipment = ref([])
const selectedEquipmentGroup = ref([])

// 搜索相关状态
const equipmentSearchName = ref('')
const equipmentGroupSearchName = ref('')
const filteredEquipmentList = ref([])
const filteredEquipmentGroupList = ref([])

// 分页相关状态
const equipmentPagination = ref({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const equipmentGroupPagination = ref({
  pageNum: 1,
  pageSize: 10,
  total: 0
})


// 方法
const handleTaskPhaseConfig = async () => {
  // 如果是编辑页面且存在剖面ID，则调用API获取任务数据
  if (pageMode.value === 'edit' && form.id) {
    try {
      const response = await getTaskList({ profileId: form.id })
      if (response.code === 200 && response.data) {
        externalTaskData.value = response.data
      }
    } catch (error) {
      console.error('获取任务数据失败:', error)
      ElMessage.error('获取任务数据失败')
    }
  }

  taskStageDialogVisible.value = true
}

// 任务阶段配置保存回调
const handleTaskStageSave = (taskStageConfig) => {
  taskStageData.value = { ...taskStageConfig }
  console.log('任务阶段配置已保存:', JSON.stringify(taskStageData.value, null, 2))
  ElMessage.success('任务阶段配置已保存')
}

// 任务阶段配置关闭回调
const handleTaskStageClose = () => {
  console.log('任务阶段配置弹窗已关闭')
}

const handleEnvironmentConfig = () => {
  envConfigDialogVisible.value = true
}

// 工况配置数据状态
const envConditionData = ref({
  geoConditionList: [],
  weatherConditionList: [],
  electromagneticConditionList: []
})

// 工况设置保存回调
const handleEnvConfigSave = (envData) => {
  // 更新环境工况数据
  Object.keys(envData).forEach(key => {
    if (envConditionData.value[key]) {
      envConditionData.value[key] = [...envData[key]]
    }
  })

  // 确保所有环境类型都有数据（即使为空数组）
  envConditionData.value = {
    geoConditionList: envData.geoConditionList || [],
    weatherConditionList: envData.weatherConditionList || [],
    electromagneticConditionList: envData.electromagneticConditionList || []
  }

  console.log('保存的环境工况数据:', JSON.stringify(envConditionData.value, null, 2))
  ElMessage.success('环境工况设置已保存')
}

// 子保存项回调
const handleEnvConfigSubSave = (subSaveData) => {
  const { envType, data } = subSaveData

  // 更新对应环境类型的数据
  switch (envType) {
    case 'geoCondition':
      envConditionData.value.geoConditionList = data
      break
    case 'weatherCondition':
      envConditionData.value.weatherConditionList = data
      break
    case 'electromagneticCondition':
      envConditionData.value.electromagneticConditionList = data
      break
  }

  console.log(`子保存${envType}数据:`, JSON.stringify(data, null, 2))
  console.log('当前所有环境工况数据:', JSON.stringify(envConditionData.value, null, 2))
}

// 加载父组件数据回调
const handleEnvConfigLoadFromParent = () => {
  console.log('子组件请求加载父组件数据')
  // 这里可以触发父组件的数据加载逻辑
}

// 红方装备导入
const handleRedEquipmentImport = () => {
  currentSide.value = 0
  equipmentDialogVisible.value = true
  loadEquipmentList()
}

// 蓝方装备导入
const handleBlueEquipmentImport = () => {
  currentSide.value = 1
  equipmentDialogVisible.value = true
  loadEquipmentList()
}

// 红方装备构型导入
const handleRedEquipmentGroupImport = () => {
  currentSide.value = 0
  equipmentGroupDialogVisible.value = true
  loadEquipmentGroupList()
}

// 蓝方装备构型导入
const handleBlueEquipmentGroupImport = () => {
  currentSide.value = 1
  equipmentGroupDialogVisible.value = true
  loadEquipmentGroupList()
}

// 加载装备列表
const loadEquipmentList = async () => {
  try {
    const response = await listEquipmentBySide({
      pageNum: equipmentPagination.value.pageNum,
      pageSize: equipmentPagination.value.pageSize,
      side: currentSide.value // 根据点击的红方(0)或蓝方(1)传入对应side参数
    })
    equipmentList.value = response.rows || []
    equipmentPagination.value.total = response.total || 0
    filterEquipmentList()
    selectedEquipment.value = []
  } catch (error) {
    console.error('加载装备列表失败:', error)
    equipmentList.value = []
    filteredEquipmentList.value = []
    equipmentPagination.value.total = 0
  }
}

// 过滤装备列表
const filterEquipmentList = () => {
  if (!equipmentSearchName.value) {
    filteredEquipmentList.value = equipmentList.value
  } else {
    filteredEquipmentList.value = equipmentList.value.filter(item =>
      item.name && item.name.toLowerCase().includes(equipmentSearchName.value.toLowerCase())
    )
  }
}

// 装备搜索
const handleEquipmentSearch = () => {
  filterEquipmentList()
}

// 加载装备编组列表
const loadEquipmentGroupList = async () => {
  try {
    const response = await listEquipmentGroupBySide({
      pageNum: equipmentGroupPagination.value.pageNum,
      pageSize: equipmentGroupPagination.value.pageSize,
      side: currentSide.value // 根据点击的红方(0)或蓝方(1)传入对应side参数
    })
    equipmentGroupList.value = response.rows || []
    equipmentGroupPagination.value.total = response.total || 0
    filterEquipmentGroupList()
    selectedEquipmentGroup.value = []
  } catch (error) {
    console.error('加载装备编组列表失败:', error)
    equipmentGroupList.value = []
    filteredEquipmentGroupList.value = []
    equipmentGroupPagination.value.total = 0
  }
}

// 过滤装备编组列表
const filterEquipmentGroupList = () => {
  if (!equipmentGroupSearchName.value) {
    filteredEquipmentGroupList.value = equipmentGroupList.value
  } else {
    filteredEquipmentGroupList.value = equipmentGroupList.value.filter(item =>
      item.name && item.name.toLowerCase().includes(equipmentGroupSearchName.value.toLowerCase())
    )
  }
}

// 装备构型搜索
const handleEquipmentGroupSearch = () => {
  filterEquipmentGroupList()
}

// 装备列表分页事件
const handleEquipmentSizeChange = (newSize) => {
  equipmentPagination.value.pageSize = newSize
  equipmentPagination.value.pageNum = 1
  loadEquipmentList()
}

const handleEquipmentCurrentChange = (newPage) => {
  equipmentPagination.value.pageNum = newPage
  loadEquipmentList()
}

// 装备构型列表分页事件
const handleEquipmentGroupSizeChange = (newSize) => {
  equipmentGroupPagination.value.pageSize = newSize
  equipmentGroupPagination.value.pageNum = 1
  loadEquipmentGroupList()
}

const handleEquipmentGroupCurrentChange = (newPage) => {
  equipmentGroupPagination.value.pageNum = newPage
  loadEquipmentGroupList()
}

// 装备类型映射
const equipmentTypeMap = {
  '0': '卫星',
  '1': '地面站',
  '2': '雷达',
  '3': '通信设备',
  '4': '指挥系统',
  '5': '侦察设备',
  '6': '打击武器',
  '7': '防御系统',
  '8': '支援装备',
  '9': '其他'
}

// 装备等级映射
const equipmentLevelMap = {
  '0': '单装',
  '1': '装备系统',
  '2': '装备子系统'
}

// 获取装备类型标签
const getEquipmentTypeLabel = (type) => {
  return equipmentTypeMap[type] || type || '未知'
}

// 获取装备等级标签
const getEquipmentLevelLabel = (level) => {
  return equipmentLevelMap[level] || level || '未知'
}

// 格式化日期
const formatDate = (timestamp) => {
  if (!timestamp) return '无'
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 确认装备导入
const confirmEquipmentImport = () => {
  if (selectedEquipment.value.length === 0) {
    ElMessage.warning('请选择要导入的装备')
    return
  }

  const treeData = currentSide.value === 0 ? redTreeData.value : blueTreeData.value

  selectedEquipment.value.forEach(equipment => {
    const existingNode = treeData.find(node => node.id === equipment.id)
    if (!existingNode) {
      treeData.push({
        id: equipment.id,
        label: equipment.name,
        type: 'equipment',
        equipmentType: equipment.equipmentType,
        side: currentSide.value
      })
    }
  })

  equipmentDialogVisible.value = false
  ElMessage.success(`成功导入${selectedEquipment.value.length}个装备`)
}

// 确认装备构型导入
const confirmEquipmentGroupImport = () => {
  if (selectedEquipmentGroup.value.length === 0) {
    ElMessage.warning('请选择要导入的装备编组')
    return
  }

  const treeData = currentSide.value === 0 ? redTreeData.value : blueTreeData.value

  selectedEquipmentGroup.value.forEach(group => {
    const existingNode = treeData.find(node => node.id === group.id)
    if (!existingNode) {
      const children = group.formationDetail ? group.formationDetail.map(item => ({
        id: item.id,
        label: item.name,
        type: 'equipment',
        equipmentType: item.equipmentType,
        parentId: group.id
      })) : []

      treeData.push({
        id: group.id,
        label: group.name,
        type: 'group',
        equipmentType: 0,
        children: children
      })

      // 保存编组的详细信息（包含formationDetail）
      if (currentSide.value === 0) {
        equipmentGroupDetails.value.red[group.id] = { ...group }
      } else {
        equipmentGroupDetails.value.blue[group.id] = { ...group }
      }

      console.log(`导入${side}方装备编组:`, group.id)
    }
  })

  equipmentGroupDialogVisible.value = false
  ElMessage.success(`成功导入${selectedEquipmentGroup.value.length}个装备编组`)
}

// 取消装备导入
const cancelEquipmentImport = () => {
  equipmentDialogVisible.value = false
  selectedEquipment.value = []
}

// 取消装备构型导入
const cancelEquipmentGroupImport = () => {
  equipmentGroupDialogVisible.value = false
  selectedEquipmentGroup.value = []
}

const submitForm = async () => {
  try {
    // 构建提交数据 - 完全按照 combatData.js 格式
    console.log("form=======", form);

    const submitData = {
      profileId: form.id || null,
      basicInfo: {
        profileName: form.profileName,
        profileTypeId: form.profileTypeId,
        operationTarget: form.operationTarget,
        operationIntensity: form.operationIntensity,
        operationRequirement: form.operationRequirement,
        operationTask: Array.isArray(form.operationTask) ? form.operationTask : [],
        blueMaxRadius: form.blueMaxRadius,
        blueRadiusUnit: form.blueRadiusUnit,
        blueReactionTime: form.blueReactionTime,
        blueReactionUnit: form.blueReactionUnit,
        blueSustainTime: form.blueSustainTime,
        blueSustainUnit: form.blueSustainUnit,
        blueStrategy: Array.isArray(form.blueStrategy) ? form.blueStrategy : [],
        equipmentDesc: JSON.stringify({
          redTreeData: redTreeData.value,
          blueTreeData: blueTreeData.value,
          equipmentGroupDetails: equipmentGroupDetails.value
        })
      },
      taskList: taskStageData.value.taskList || [],
      geoConditionList: envConditionData.value.geoConditionList.map(item => ({
        envType: '地理环境',
        geoSubType: Array.isArray(item.geoSubType) ? item.geoSubType : [item.geoSubType].filter(Boolean),
        polygonCoords: item.polygonCoords || []
      })),
      weatherConditionList: envConditionData.value.weatherConditionList.map(item => ({
        envType: '气象海洋',
        weatherType: Array.isArray(item.weatherType) ? item.weatherType : [item.weatherType].filter(Boolean),
        weatherLevel: Array.isArray(item.weatherLevel) ? item.weatherLevel : [item.weatherLevel].filter(Boolean),
        validStartTime: item.validStartTime || '',
        validEndTime: item.validEndTime || '',
        polygonCoords: item.polygonCoords || []
      })),
      electromagneticConditionList: envConditionData.value.electromagneticConditionList.map(item => ({
        envType: '空间电磁',
        geoSubType: Array.isArray(item.geoSubType) ? item.geoSubType : [item.geoSubType].filter(Boolean),
        solar_flare_level: Array.isArray(item.solar_flare_level) ? item.solar_flare_level : [item.solar_flare_level].filter(Boolean),
        validStartTime: item.validStartTime || '',
        validEndTime: item.validEndTime || '',
        polygonCoords: item.polygonCoords || []
      })),
      blueEquipmentList: [],
      redEquipmentList: [],

    }

    // 转换装备树数据为combatData.js格式
    const convertEquipmentTreeData = (treeData, side) => {
      const equipmentList = []
      console.log('原始树数据:', treeData)
      treeData.forEach(node => {
        if (node.type === 'equipment') {
          // 单个装备 - 使用真实的装备ID
          equipmentList.push({
            equipmentId: node.id || '',
            equipmentName: node.label,
            subSystemType: 0,
            equipmentLevel: node.type === 'equipment' ? 1 : 0, // type为equipment时equipmentLevel为1
            equipmentType: parseInt(node.equipmentType) || 0
          })
        } else if (node.type === 'group') {
          // 装备编组 - 只保存编组本身，不保存编组中的单个装备
          equipmentList.push({
            equipmentId: node.id || '',
            equipmentName: node.label,
            subSystemType: 1, // 编组类型
            equipmentLevel: 0, // 编组级别为0
            equipmentType: 0 // 编组类型为0
          })
        }
      })

      console.log('转换后的装备列表:', equipmentList)
      return equipmentList
    }

    // 添加装备数据到提交数据
    submitData.blueEquipmentList = convertEquipmentTreeData(blueTreeData.value, 'blue')
    submitData.redEquipmentList = convertEquipmentTreeData(redTreeData.value, 'red')

    console.log('提交数据:', JSON.stringify(submitData, null, 2))

    // 根据页面模式调用不同的API接口
    if (pageMode.value === 'add') {
      await saveOrUpdateCombatProfile(submitData)
    } else {
      await updateProfile(submitData)
    }

    ElMessage.success(pageMode.value === 'add' ? '作战剖面新增成功！' : '作战剖面修改成功！')
    goBack()
  } catch (error) {
    ElMessage.error('保存失败，请重试')
  }
}

const goBack = () => {
  router.push('/major/system-cooperation/combat-profile')
}

// 初始化
onMounted(() => {
  // 检查路由参数和查询参数，判断是新增还是编辑模式
  const mode = route.query.mode
  const id = route.query.id

  if (mode === 'edit' && id) {
    pageMode.value = 'edit'
    editId.value = parseInt(id)
    loadEditData()
  } else {
    pageMode.value = 'add'
  }
})

// 加载编辑数据
const loadEditData = async () => {
  try {
    const response = await profileDetail(editId.value)
    const profileData = response.data

    console.log('加载的剖面数据:', profileData)

    // 填充表单数据
    form.id = profileData.id || editId.value
    form.profileName = profileData.profileName || ''
    form.profileTypeId = profileData.profileTypeId || ''
    form.operationTask = profileData.operationTask || ''
    form.operationTarget = profileData.operationTarget || ''
    form.operationIntensity = profileData.operationIntensity || 0.5
    form.operationRequirement = profileData.operationRequirement || ''
    form.blueMaxRadius = profileData.blueMaxRadius || 1000
    form.blueRadiusUnit = profileData.blueRadiusUnit || '公里'
    form.blueReactionTime = profileData.blueReactionTime || 30
    form.blueReactionUnit = profileData.blueReactionUnit || '分钟'
    form.blueSustainTime = profileData.blueSustainTime || 24
    form.blueSustainUnit = profileData.blueSustainUnit || '小时'
    form.blueStrategy = Array.isArray(profileData.blueStrategy) ? profileData.blueStrategy : []

    // 填充装备列表数据 - 直接使用equipmentDesc字段
    if (profileData.equipmentDesc) {
      try {
        const equipmentDesc = JSON.parse(profileData.equipmentDesc)
        console.log('解析后的equipmentDesc数据:', equipmentDesc)

        // 加载红方树结构数据
        if (equipmentDesc.redTreeData) {
          redTreeData.value = equipmentDesc.redTreeData
          console.log('红方el-tree数据:', redTreeData.value)
        }

        // 加载蓝方树结构数据
        if (equipmentDesc.blueTreeData) {
          blueTreeData.value = equipmentDesc.blueTreeData
          console.log('蓝方el-tree数据:', blueTreeData.value)
        }

        // 加载装备编组详细信息
        if (equipmentDesc.equipmentGroupDetails) {
          equipmentGroupDetails.value = equipmentDesc.equipmentGroupDetails
          console.log('装备编组详细信息:', equipmentGroupDetails.value)
        }

        // 保留原有的装备列表数据用于兼容
        if (profileData.blueEquipmentList) {
          blueEquipmentList.value = profileData.blueEquipmentList
        }
        if (profileData.redEquipmentList) {
          redEquipmentList.value = profileData.redEquipmentList
        }

      } catch (error) {
        console.error('解析equipmentDesc字段失败:', error)
        // 如果解析失败，回退到原有的加载方式
        if (profileData.blueEquipmentList) {
          blueEquipmentList.value = profileData.blueEquipmentList
          if (pageMode.value === 'edit') {
            await loadEquipmentDataToTree(profileData.blueEquipmentList, 'blue')
          }
        }
        if (profileData.redEquipmentList) {
          redEquipmentList.value = profileData.redEquipmentList
          if (pageMode.value === 'edit') {
            await loadEquipmentDataToTree(profileData.redEquipmentList, 'red')
          }
        }
      }
    } else {
      // 如果没有equipmentDesc字段，使用原有的加载方式
      if (profileData.blueEquipmentList) {
        blueEquipmentList.value = profileData.blueEquipmentList
        if (pageMode.value === 'edit') {
          await loadEquipmentDataToTree(profileData.blueEquipmentList, 'blue')
        }
      }
      if (profileData.redEquipmentList) {
        redEquipmentList.value = profileData.redEquipmentList
        if (pageMode.value === 'edit') {
          await loadEquipmentDataToTree(profileData.redEquipmentList, 'red')
        }
      }
    }

    // 填充任务阶段数据
    if (profileData.taskList && profileData.taskList.length > 0) {
      taskStageData.value.taskList = profileData.taskList
    } else {
      // 如果taskList为空，调用API获取任务数据
      await loadTaskListData(profileData.id)
    }

    // 填充环境工况数据 - 使用对象解构简化赋值
    Object.assign(envConditionData.value, {
      geoConditionList: profileData.geoConditionList || [],
      weatherConditionList: profileData.weatherConditionList || [],
      electromagneticConditionList: profileData.electromagneticConditionList || []
    })

    console.log('填充后的表单数据:', form)
    console.log('填充后的任务阶段数据:', taskStageData.value)

  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
    goBack()
  }
}

// 加载任务列表数据
const loadTaskListData = async (profileId) => {
  try {
    console.log('开始加载任务列表数据，profileId:', profileId)
    const response = await queryTask({ profileId: profileId })
    console.log('任务列表API响应:', response)

    if (response.code === 200 && response.data) {
      taskStageData.value.taskList = response.data
      console.log('任务列表数据加载成功:', taskStageData.value.taskList)
    } else {
      console.log('任务列表数据为空或加载失败')
      taskStageData.value.taskList = []
    }
  } catch (error) {
    console.error('加载任务列表数据失败:', error)
    taskStageData.value.taskList = []
  }
}

// 加载装备数据到el-tree中（包括单个装备和装备编组）
const loadEquipmentDataToTree = async (equipmentList, side) => {
  try {
    console.log(`开始加载${side}方装备数据到el-tree`)

    if (!equipmentList || equipmentList.length === 0) {
      console.log(`${side}方没有装备数据`)
      return
    }

    // 创建新的树数据数组，避免直接修改响应式数据
    const newTreeData = side === 'blue' ? [...blueTreeData.value] : [...redTreeData.value]

    // 处理所有装备数据
    for (const equipment of equipmentList) {
      const existingNode = newTreeData.find(node => node.id === equipment.equipmentId)
      if (!existingNode) {
        if (equipment.subSystemType === 1) {
          // 装备编组 - 创建编组节点（简化版，不再查询详细信息）
          newTreeData.push({
            id: equipment.equipmentId,
            label: equipment.equipmentName,
            type: 'group',
            equipmentType: 0,
            children: [] // 添加空的children数组，保持树结构格式
          })
          console.log(`创建${side}方装备编组节点:`, equipment.equipmentId)
        } else {
          // 单个装备 - 直接创建装备节点
          newTreeData.push({
            id: equipment.equipmentId,
            label: equipment.equipmentName,
            type: 'equipment',
            equipmentType: equipment.equipmentType || 0
          })
          console.log(`创建${side}方单个装备节点:`, equipment.equipmentId)
        }
      }
    }

    // 更新响应式数据
    if (side === 'blue') {
      blueTreeData.value = newTreeData
    } else {
      redTreeData.value = newTreeData
    }

    console.log(`${side}方装备数据加载到el-tree完成，树数据:`, newTreeData)
  } catch (error) {
    console.error(`加载${side}方装备数据到el-tree失败:`, error)
  }
}

// 加载装备编组详细信息并更新el-tree显示
const loadEquipmentGroupDetails = async (groupId, side) => {
  try {
    console.log(`开始加载${side}方装备编组详细信息，编组ID: ${groupId}`)

    // 查询装备编组的详细信息
    const response = await getEquipmentGroup(groupId)

    if (response.code === 200 && response.data) {
      const groupDetail = response.data
      console.log(`装备编组${groupId}详细信息:`, groupDetail)

      // 更新el-tree中的装备编组显示
      await updateEquipmentTreeWithGroupDetail(groupDetail, side)
    } else {
      console.log(`装备编组${groupId}查询失败`)
    }

    console.log(`${side}方装备编组${groupId}详细信息加载完成`)
  } catch (error) {
    console.error(`加载${side}方装备编组${groupId}详细信息失败:`, error)
  }
}

// 更新el-tree中的装备编组显示
const updateEquipmentTreeWithGroupDetail = async (groupDetail, side) => {
  const treeData = side === 'blue' ? blueTreeData.value : redTreeData.value

  // 查找对应的编组节点
  const groupNode = treeData.find(node => node.id === groupDetail.id)

  if (groupNode) {
    // 更新编组节点的子节点（装备列表）
    if (groupDetail.formationDetail && groupDetail.formationDetail.length > 0) {
      groupNode.children = groupDetail.formationDetail.map(equipment => ({
        id: equipment.id,
        label: equipment.name,
        type: 'equipment',
        equipmentType: equipment.equipmentType || 0
      }))

      console.log(`更新${side}方装备编组${groupDetail.id}的子节点:`, groupNode.children)
    }

    // 强制更新树结构显示
    if (side === 'blue') {
      blueTreeData.value = [...blueTreeData.value]
    } else {
      redTreeData.value = [...redTreeData.value]
    }
  } else {
    console.log(`未找到${side}方装备编组节点:`, groupDetail.id)
  }
}

</script>

<style scoped>
.combat-profile-add-page {
  padding: 20px;
}

.detail-header {
  background: var(--el-bg-color);
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 15px;
  flex: 1;
  justify-content: flex-start;
}

.project-title {
  margin: 0;
  font-size: 24px;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.status-badge {
  font-size: 14px;
  padding: 4px 12px;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.form-container {
  margin-bottom: 20px;
}

.form-row {
  margin-bottom: 20px;
}

.large-row {
  min-height: 200px;
}

.intensity-slider {
  width: 100%;
}

.slider-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.blue-capability-panel {
  background: rgba(0, 242, 255, 0.05);
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  padding: 15px;
  height: 180px;
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.blue-capability-panel .el-form-item {
  margin-bottom: 10px;
}

.blue-capability-panel .unit {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.structure-row {
  margin-bottom: 20px;
}

.tree-container {
  min-height: 680px;
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.tree-title {
  font-weight: bold;
  color: var(--el-text-color-primary);
  font-size: 16px;
}

.combat-tree {
  margin-bottom: 20px;
  background: transparent;
}

.cesium-container {
  height: 100%;
  min-height: 400px;
}

.cesium-container :deep(.el-card__body) {
  padding: 0;
  height: 100%;
}

.cesium-content {
  text-align: center;
  color: var(--el-text-color-secondary);
}

.cesium-content h3 {
  margin: 15px 0 10px 0;
  color: var(--el-text-color-primary);
}

.cesium-content p {
  margin: 5px 0;
  font-size: 14px;
}

.action-buttons .el-button {
  margin: 0 15px;
  min-width: 120px;
}

.import-buttons {
  display: flex;
  gap: 8px;
}

.empty-text {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 20px;
}

.search-bar {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 15px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .form-row .el-col {
    margin-bottom: 15px;
  }

  .large-row {
    min-height: auto;
  }

  .blue-capability-panel {
    height: auto;
    min-height: 180px;
  }
}

/* 树结构滚动条样式 */
.tree-scroll-container {
  height: calc(50% - 30px);
  overflow-y: auto;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  margin-top: 8px;
}

.tree-scroll-container::-webkit-scrollbar {
  width: 6px;
}

.tree-scroll-container::-webkit-scrollbar-track {
  background: var(--el-bg-color-page);
  border-radius: 3px;
}

.tree-scroll-container::-webkit-scrollbar-thumb {
  background: var(--el-border-color);
  border-radius: 3px;
}

.tree-scroll-container::-webkit-scrollbar-thumb:hover {
  background: var(--el-border-color-dark);
}

.tree-scroll-container .combat-tree {
  min-height: 100%;
  padding: 8px;
}
</style>