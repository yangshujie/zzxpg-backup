<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="需求编号" prop="code">
        <el-input
          v-model="queryParams.code"
          placeholder="请输入需求编号"
          clearable
          style="width: 140px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="需求名称" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入需求名称"
          clearable
          style="width: 160px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="需求类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择" clearable style="width: 120px">
          <el-option v-for="item in typeOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 120px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 右侧工具栏 -->
    <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />

    <!-- 数据表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      table-layout="fixed"
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" align="center" />
      <el-table-column label="需求编号" prop="code" width="130" show-overflow-tooltip />
      <el-table-column label="需求名称" prop="title" min-width="180" show-overflow-tooltip />
      <el-table-column label="需求类型" prop="type" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getTypeTagType(row.type)" size="small">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="关键输入" prop="input" min-width="140" show-overflow-tooltip />
      <el-table-column label="目标输出" prop="output" min-width="140" show-overflow-tooltip />
      <el-table-column label="关联算法" prop="algorithmName" width="130" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.algorithmName" class="link-text" @click="handleViewAlgo(row)">{{ row.algorithmName }}</span>
          <span v-else class="text-muted">待构建</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="通知" prop="notifyStatus" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.notifyStatus === '已通知'" type="success" size="small">已通知</el-tag>
          <el-tag v-else-if="row.notifyStatus === '通知失败'" type="danger" size="small">失败</el-tag>
          <span v-else class="text-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
          <el-button link type="success" size="small" @click="handleBuild(row)" :disabled="row.status === '已构建'">构建</el-button>
          <el-button link type="warning" size="small" @click="handleNotify(row)" :disabled="row.status !== '已构建' || row.notifyStatus === '已通知'">通知</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 详情对话框 - 算法构建要求 -->
    <el-dialog v-model="detailDialogVisible" title="算法构建要求" width="800px" append-to-body destroy-on-close>
      <div class="detail-header">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="需求编号">{{ currentRow.code }}</el-descriptions-item>
          <el-descriptions-item label="需求类型">
            <el-tag :type="getTypeTagType(currentRow.type)" size="small">{{ currentRow.type }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(currentRow.status)" size="small">{{ currentRow.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="关联算法" :span="2">
            <span v-if="currentRow.algorithmName" class="link-text" @click="handleViewAlgo(currentRow)">{{ currentRow.algorithmName }}</span>
            <span v-else class="text-muted">待构建</span>
          </el-descriptions-item>
          <el-descriptions-item label="通知状态">
            <el-tag v-if="currentRow.notifyStatus === '已通知'" type="success" size="small">已通知</el-tag>
            <span v-else class="text-muted">—</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <el-divider content-position="left">基本信息</el-divider>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="需求名称" :span="2">{{ currentRow.title }}</el-descriptions-item>
        <el-descriptions-item label="需求说明" :span="2">{{ currentRow.summary || '—' }}</el-descriptions-item>
        <el-descriptions-item label="验收方式" :span="2">{{ currentRow.acceptance || '—' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">接口定义</el-divider>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="关键输入">
          <div class="requirement-content">{{ currentRow.input || '—' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="目标输出">
          <div class="requirement-content">{{ currentRow.output || '—' }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">算法参数要求</el-divider>
      <el-table :data="currentRow.requirementParams || []" border size="small" max-height="200">
        <el-table-column prop="paramField" label="参数名称" min-width="100" />
        <el-table-column prop="paramType" label="参数类型" width="90" align="center" />
        <el-table-column prop="defaultValue" label="默认值" width="80" align="center" />
        <el-table-column prop="requiredFlag" label="必填" width="60" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.requiredFlag === 1" type="danger" size="small">是</el-tag>
            <el-tag v-else type="info" size="small">否</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paramDesc" label="参数说明" min-width="140" />
      </el-table>
      <div v-if="!currentRow.requirementParams || currentRow.requirementParams.length === 0" class="empty-tip">暂无参数要求</div>

      <el-divider content-position="left">算法配置要求</el-divider>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="算法类型">{{ currentRow.algorithmType || '待指定' }}</el-descriptions-item>
        <el-descriptions-item label="代码包要求">
          <div class="requirement-content">上传 .zip 格式算法包，路径为 algs/分类目录/算法名称.zip</div>
        </el-descriptions-item>
        <el-descriptions-item label="特殊配置">
          <div class="requirement-content">{{ currentRow.specialConfig || '无特殊配置要求' }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关 闭</el-button>
        <el-button type="primary" @click="handleBuild(currentRow)" :disabled="currentRow.status === '已构建'">前往构建</el-button>
      </template>
    </el-dialog>

    <!-- 构建算法对话框 - 复用算法管理新增弹窗 -->
    <el-dialog v-model="buildDialogVisible" :title="buildDialogTitle" width="900px" append-to-body :close-on-click-modal="false">
      <el-form ref="buildFormRef" :model="buildForm" :rules="buildRules" label-width="110px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="算法名称" prop="algorithmName">
              <el-input v-model="buildForm.algorithmName" placeholder="请输入算法名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="算法类型" prop="algorithmType">
              <el-select v-model="buildForm.algorithmType" placeholder="请选择算法类型" style="width: 100%">
                <el-option v-for="item in algorithmTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="装备类型" prop="equipmentType">
              <el-select v-model="buildForm.equipmentType" placeholder="6类装备之一" style="width: 100%">
                <el-option v-for="item in equipmentTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="基准数据">
              <el-radio-group v-model="buildForm.baseFlag">
                <el-radio-button :label="1">是</el-radio-button>
                <el-radio-button :label="0">否</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="算法代码包">
              <div class="code-upload-row">
                <el-upload accept=".zip" :auto-upload="false" :limit="1" :on-change="onZipChange" :on-remove="onZipRemove">
                  <el-button type="primary" plain size="small" icon="Upload">选择 zip</el-button>
                </el-upload>
                <span class="upload-tip">上传后路径为 algs/分类目录/算法名称.zip；请避免压缩包内主程序与算法名称不一致</span>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="存储路径">
              <el-input v-model="buildForm.algorithmCodeUrl" placeholder="上传后自动回填，也可手填 MinIO 相对路径" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="算法描述">
          <el-input v-model="buildForm.algorithmDesc" type="textarea" :rows="2" placeholder="请输入算法描述" />
        </el-form-item>

        <!-- 输入参数配置 -->
        <el-divider content-position="left">输入参数配置</el-divider>
        <div class="param-section">
          <el-table :data="buildForm.inputParams" border size="small" style="width: 100%">
            <el-table-column label="参数名称" min-width="120">
              <template #default="scope">
                <el-input v-model="scope.row.paramField" placeholder="参数名" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="参数类型" width="120">
              <template #default="scope">
                <el-select v-model="scope.row.paramType" placeholder="类型" size="small" style="width: 100%">
                  <el-option label="字符串" value="string" />
                  <el-option label="数值" value="number" />
                  <el-option label="数组" value="array" />
                  <el-option label="字典" value="dictionary" />
                  <el-option label="时间" value="date" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="默认值" min-width="100">
              <template #default="scope">
                <el-input v-model="scope.row.defaultValue" placeholder="默认值" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="必填" width="60" align="center">
              <template #default="scope">
                <el-checkbox v-model="scope.row.requiredFlag" :true-label="1" :false-label="0" />
              </template>
            </el-table-column>
            <el-table-column label="描述" min-width="140">
              <template #default="scope">
                <el-input v-model="scope.row.paramDesc" placeholder="参数描述" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="" width="60" align="center">
              <template #default="scope">
                <el-button link type="danger" size="small" icon="Delete" @click="removeBuildParam('inputParams', scope.$index)" />
              </template>
            </el-table-column>
          </el-table>
          <el-button class="mt8" type="primary" plain size="small" icon="Plus" @click="addBuildParam('inputParams')">添加输入参数</el-button>
        </div>

        <!-- 配置参数 -->
        <el-divider content-position="left">配置参数</el-divider>
        <div class="param-section">
          <el-table :data="buildForm.configParams" border size="small" style="width: 100%">
            <el-table-column label="参数名称" min-width="120">
              <template #default="scope">
                <el-input v-model="scope.row.paramField" placeholder="参数名" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="参数类型" width="120">
              <template #default="scope">
                <el-select v-model="scope.row.paramType" placeholder="类型" size="small" style="width: 100%">
                  <el-option label="字符串" value="string" />
                  <el-option label="数值" value="number" />
                  <el-option label="数组" value="array" />
                  <el-option label="字典" value="dictionary" />
                  <el-option label="数据模板" value="template" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="默认值" min-width="100">
              <template #default="scope">
                <el-input v-model="scope.row.defaultValue" placeholder="默认值" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="必填" width="60" align="center">
              <template #default="scope">
                <el-checkbox v-model="scope.row.requiredFlag" :true-label="1" :false-label="0" />
              </template>
            </el-table-column>
            <el-table-column label="描述" min-width="140">
              <template #default="scope">
                <el-input v-model="scope.row.paramDesc" placeholder="参数描述" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="" width="60" align="center">
              <template #default="scope">
                <el-button link type="danger" size="small" icon="Delete" @click="removeBuildParam('configParams', scope.$index)" />
              </template>
            </el-table-column>
          </el-table>
          <el-button class="mt8" type="primary" plain size="small" icon="Plus" @click="addBuildParam('configParams')">添加配置参数</el-button>
        </div>

        <!-- 输出参数 -->
        <el-divider content-position="left">输出参数配置</el-divider>
        <div class="param-section">
          <el-table :data="buildForm.outputParams" border size="small" style="width: 100%">
            <el-table-column label="参数名称" min-width="120">
              <template #default="scope">
                <el-input v-model="scope.row.paramField" placeholder="参数名" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="参数类型" width="120">
              <template #default="scope">
                <el-select v-model="scope.row.paramType" placeholder="类型" size="small" style="width: 100%">
                  <el-option label="字符串" value="string" />
                  <el-option label="数值" value="number" />
                  <el-option label="数组" value="array" />
                  <el-option label="字典" value="dictionary" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="描述" min-width="180">
              <template #default="scope">
                <el-input v-model="scope.row.paramDesc" placeholder="参数描述" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="" width="60" align="center">
              <template #default="scope">
                <el-button link type="danger" size="small" icon="Delete" @click="removeBuildParam('outputParams', scope.$index)" />
              </template>
            </el-table-column>
          </el-table>
          <el-button class="mt8" type="primary" plain size="small" icon="Plus" @click="addBuildParam('outputParams')">添加输出参数</el-button>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="buildDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="buildLoading" @click="submitBuild">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 通知外部系统对话框 -->
    <el-dialog v-model="notifyDialogVisible" title="通知外部系统" width="500px" append-to-body>
      <el-form :model="notifyForm" label-width="100px">
        <el-form-item label="需求编号">{{ currentRow.code }}</el-form-item>
        <el-form-item label="算法名称">{{ currentRow.algorithmName }}</el-form-item>
        <el-form-item label="通知方式">
          <el-radio-group v-model="notifyForm.notifyType">
            <el-radio label="HTTP">HTTP回调</el-radio>
            <el-radio label="MQ">消息队列</el-radio>
            <el-radio label="WEBHOOK">Webhook</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="目标地址">
          <el-input v-model="notifyForm.targetUrl" placeholder="请输入通知目标地址" />
        </el-form-item>
        <el-form-item label="通知内容">
          <el-input v-model="notifyForm.content" type="textarea" :rows="3" placeholder="可自定义通知内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="notifyDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="notifyLoading" @click="submitNotify">发送通知</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getZhpgEquipmentTypeLabel } from "@/constants/zhpgIndicatorSystem"
import {
  listAlgorithmRequirement,
  getAlgorithmRequirement,
  addAlgorithmRequirement,
  updateAlgorithmRequirement,
  delAlgorithmRequirement,
  updateBuildStatus,
  updateNotifyStatus
} from "@/api/zhpg/algorithmRequirement"
import { addAlgorithm } from "@/api/zhpg/algorithm"

const { proxy } = getCurrentInstance()

const getEquipmentTypeLabel = getZhpgEquipmentTypeLabel

const loading = ref(false)
const showSearch = ref(true)
const tableData = ref([])
const total = ref(0)
const ids = ref([])
const multiple = ref(true)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  code: '',
  title: '',
  type: '',
  status: '',
})

const typeOptions = ['新增算法', '算法改造', '参数调优']
const statusOptions = [
  { label: '待构建', value: '待构建' },
  { label: '构建中', value: '构建中' },
  { label: '已构建', value: '已构建' },
]

const algorithmTypeOptions = [
  { label: '预处理算法', value: '预处理算法' },
  { label: '指标量化算法', value: '指标量化算法' },
  { label: '属性值计算方法', value: '属性值计算方法' },
  { label: '权重分配', value: '权重分配' },
  { label: '聚合传导', value: '聚合传导' },
  { label: '方案评价', value: '方案评价' },
  { label: '其它算法', value: '其它算法' },
]

const equipmentTypeOptions = [
  { label: '航天侦察', value: 'TZJC' },
  { label: '太空态势感知', value: 'TKTS' },
  { label: '太空攻防', value: 'TKGF' },
  { label: '航天测运控', value: 'HZCY' },
  { label: '航天发射', value: 'HTFS' },
  { label: '海基航天', value: 'HJHT' },
  { label: '一能三性', value: 'YNSX' },
]

const detailDialogVisible = ref(false)
const buildDialogVisible = ref(false)
const notifyDialogVisible = ref(false)
const buildDialogTitle = ref('构建算法')
const buildLoading = ref(false)
const notifyLoading = ref(false)
const currentRow = ref({})
const pendingZipFile = ref(null)

const buildFormRef = ref()

const buildForm = ref({
  algorithmName: '',
  algorithmType: '',
  equipmentType: '',
  algorithmDesc: '',
  algorithmCodeUrl: '',
  baseFlag: 0,
  inputParams: [],
  configParams: [],
  outputParams: [],
})

const buildRules = {
  algorithmName: [{ required: true, message: '请输入算法名称', trigger: 'blur' }],
  algorithmType: [{ required: true, message: '请选择算法类型', trigger: 'change' }],
}

const notifyForm = ref({
  notifyType: 'HTTP',
  targetUrl: '',
  content: '',
})

function getTypeTagType(type) {
  const map = { '新增算法': 'danger', '算法改造': 'primary', '参数调优': 'success' }
  return map[type] || 'info'
}

function getStatusTagType(status) {
  const map = { '待构建': 'info', '构建中': 'warning', '已构建': 'success' }
  return map[status] || 'info'
}

/** 查询列表 */
function getList() {
  loading.value = true
  listAlgorithmRequirement(queryParams.value).then(res => {
    tableData.value = res.rows
    total.value = res.total
    if (res.rows.length > 0 && !currentRow.value.code) {
      currentRow.value = res.rows[0]
    }
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

/** 搜索 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置 */
function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    code: '',
    title: '',
    type: '',
    status: '',
  }
  handleQuery()
}

/** 多选 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  multiple.value = !selection.length
}

/** 查看详情 */
function handleDetail(row) {
  getAlgorithmRequirement(row.id).then(res => {
    currentRow.value = res.data || row
    detailDialogVisible.value = true
  })
}

/** 查看算法 */
function handleViewAlgo(row) {
  ElMessage.info('跳转算法管理查看: ' + row.algorithmName)
}

/** 构建算法 */
function handleBuild(row) {
  currentRow.value = row
  detailDialogVisible.value = false

  // 重置构建表单
  buildForm.value = {
    algorithmName: row.title,
    algorithmType: row.algorithmType || '',
    equipmentType: '',
    algorithmDesc: row.summary || '',
    algorithmCodeUrl: '',
    baseFlag: 0,
    inputParams: [],
    configParams: [],
    outputParams: [],
  }

  // 如果需求有参数要求，预填输入参数
  if (row.requirementParams && row.requirementParams.length > 0) {
    buildForm.value.inputParams = row.requirementParams.map(p => ({
      paramField: p.paramField || '',
      paramType: p.paramType === '字典' ? 'dictionary' : (p.paramType === '数组' ? 'array' : (p.paramType === '数值' ? 'number' : 'string')),
      defaultValue: p.defaultValue || '',
      paramDesc: p.paramDesc || '',
      requiredFlag: p.requiredFlag === 1 || p.requiredFlag === true ? 1 : 0,
    }))
  }

  buildDialogTitle.value = '构建算法（需求：' + row.code + '）'
  buildDialogVisible.value = true
}

/** 添加参数 */
function addBuildParam(paramType) {
  buildForm.value[paramType].push({
    paramField: '',
    paramType: 'string',
    defaultValue: '',
    paramDesc: '',
    requiredFlag: 0,
  })
}

/** 移除参数 */
function removeBuildParam(paramType, index) {
  buildForm.value[paramType].splice(index, 1)
}

/** 文件变化 */
function onZipChange(uploadFile) {
  const raw = uploadFile.raw
  if (!raw) return
  const name = (raw.name || '').toLowerCase()
  if (!name.endsWith('.zip')) {
    ElMessage.warning('请上传 .zip 格式的算法包')
    pendingZipFile.value = null
    return
  }
  pendingZipFile.value = raw
}

function onZipRemove() {
  pendingZipFile.value = null
}

/** 确认构建 */
function submitBuild() {
  buildFormRef.value.validate(valid => {
    if (!valid) return
    buildLoading.value = true

    // 构建算法数据
    const algorithmData = {
      algorithmName: buildForm.value.algorithmName,
      algorithmType: buildForm.value.algorithmType,
      equipmentType: buildForm.value.equipmentType,
      algorithmDesc: buildForm.value.algorithmDesc,
      algorithmCodeUrl: buildForm.value.algorithmCodeUrl,
      baseFlag: buildForm.value.baseFlag,
      params: [
        ...buildForm.value.inputParams.map((p, i) => ({ ...p, paramCategory: 'INPUT', sortOrder: i + 1 })),
        ...buildForm.value.configParams.map((p, i) => ({ ...p, paramCategory: 'CONFIG', sortOrder: i + 1 })),
        ...buildForm.value.outputParams.map((p, i) => ({ ...p, paramCategory: 'OUTPUT', sortOrder: i + 1 })),
      ],
    }

    // 调用算法管理接口新增算法
    addAlgorithm(algorithmData).then(res => {
      // 更新需求的构建状态
      return updateBuildStatus(currentRow.value.id, buildForm.value.algorithmName, res.data)
    }).then(res => {
      ElMessage.success('算法构建成功')
      buildDialogVisible.value = false
      getList()
    }).catch(err => {
      ElMessage.error(err.msg || '构建失败')
    }).finally(() => {
      buildLoading.value = false
    })
  })
}

/** 通知外部 */
function handleNotify(row) {
  currentRow.value = row
  notifyForm.value = {
    notifyType: 'HTTP',
    targetUrl: '',
    content: `算法 "${row.algorithmName}" 已构建完成（需求编号：${row.code}），请注意查收。`,
  }
  notifyDialogVisible.value = true
}

/** 确认通知 */
function submitNotify() {
  if (!notifyForm.value.targetUrl) {
    ElMessage.warning('请输入通知目标地址')
    return
  }
  notifyLoading.value = true

  // 实际项目中应该调用通知接口，这里模拟
  setTimeout(() => {
    updateNotifyStatus(currentRow.value.id, '已通知').then(() => {
      ElMessage.success('通知已发送')
      notifyDialogVisible.value = false
      getList()
    }).catch(() => {
      ElMessage.error('通知发送失败')
    }).finally(() => {
      notifyLoading.value = false
    })
  }, 500)
}

getList()
</script>

<style scoped>
.text-muted {
  color: #999;
  font-size: 13px;
}

.link-text {
  color: #409eff;
  cursor: pointer;
}

.link-text:hover {
  text-decoration: underline;
}

.detail-header {
  margin-bottom: 16px;
}

.requirement-content {
  white-space: pre-wrap;
  word-break: break-all;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 20px 0;
}

.code-upload-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.upload-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.param-section {
  padding: 0 20px 10px 20px;
}

.mt8 {
  margin-top: 8px;
}
</style>
