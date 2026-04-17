<template>
  <div class="app-container">
    <div class="page-header mb12">
      <h2 class="page-title">评估数据源管理</h2>
      <p class="page-desc">统一管理评估任务需要的数据库、REST 接口和文件类数据源，文件类数据统一通过文件服务接入。</p>
    </div>

    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="数据源名称" prop="sourceName">
        <el-input
          v-model="queryParams.sourceName"
          placeholder="请输入数据源名称"
          clearable
          style="width: 220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="数据源类型" prop="sourceType">
        <el-select v-model="queryParams.sourceType" placeholder="全部类型" clearable style="width: 180px">
          <el-option v-for="item in sourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar-row mb8">
      <div class="toolbar-btns">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleBatchDelete">删除</el-button>
      </div>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </div>

    <el-table
      v-loading="loading"
      :data="dataSourceList"
      row-key="id"
      table-layout="fixed"
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" align="center" />
      <el-table-column label="数据源名称" prop="sourceName" min-width="180" show-overflow-tooltip />
      <el-table-column label="类型" prop="sourceType" min-width="120" show-overflow-tooltip>
        <template #default="scope">{{ getTypeLabel(scope.row.sourceType) }}</template>
      </el-table-column>
      <el-table-column label="连接目标" min-width="280" show-overflow-tooltip>
        <template #default="scope">{{ buildTarget(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="账号" prop="username" min-width="110" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.username || '-' }}</template>
      </el-table-column>
      <el-table-column label="关联字段" prop="fieldNames" min-width="180" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.fieldNames || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'info'" size="small">
            {{ getStatusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近测试" min-width="170" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="isFileType(scope.row.sourceType)" class="text-muted">文件服务接入</span>
          <span v-else-if="scope.row.lastTestStatus">
            {{ scope.row.lastTestStatus === 'SUCCESS' ? '成功' : '失败' }}
            <span class="text-muted">({{ parseTime(scope.row.lastTestTime) }})</span>
          </span>
          <span v-else class="text-muted">未测试</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="scope">
          <div class="table-ops">
            <el-button link type="primary" size="small" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
            <el-button
              v-if="!isFileType(scope.row.sourceType)"
              link
              type="success"
              size="small"
              icon="Connection"
              @click="handleRowTest(scope.row)"
            >
              测试
            </el-button>
            <el-button link type="danger" size="small" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </div>
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

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="860px" append-to-body :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-divider content-position="left">基础信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="数据源名称" prop="sourceName">
              <el-input v-model="form.sourceName" placeholder="请输入数据源名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据源类型" prop="sourceType">
              <el-select v-model="form.sourceType" placeholder="请选择数据源类型" style="width: 100%" @change="handleTypeChange">
                <el-option v-for="item in sourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio-button label="ENABLED">启用</el-radio-button>
                <el-radio-button label="DISABLED">停用</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联字段">
              <el-input v-model="form.fieldNames" placeholder="多个字段用逗号分隔，如：id,name,score" />
            </el-form-item>
          </el-col>
        </el-row>

        <template v-if="isDatabaseType(form.sourceType)">
          <el-divider content-position="left">数据库连接</el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="主机地址" prop="host">
                <el-input v-model="form.host" placeholder="如：192.168.1.100" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="端口">
                <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="数据库/服务名" prop="databaseName">
                <el-input v-model="form.databaseName" placeholder="数据库名或 Oracle 服务名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="Schema">
                <el-input v-model="form.schemaName" placeholder="可选" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="表/视图">
                <el-input v-model="form.tableName" placeholder="可选" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="用户名">
                <el-input v-model="form.username" placeholder="可选" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="密码">
                <el-input v-model="form.password" type="password" show-password placeholder="可选" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <template v-else-if="isApiType(form.sourceType)">
          <el-divider content-position="left">接口连接</el-divider>
          <el-row :gutter="20">
            <el-col :span="16">
              <el-form-item label="接口地址" prop="apiUrl">
                <el-input v-model="form.apiUrl" placeholder="请输入 REST 接口地址" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="请求方法">
                <el-select v-model="form.requestMethod" style="width: 100%">
                  <el-option label="GET" value="GET" />
                  <el-option label="POST" value="POST" />
                  <el-option label="PUT" value="PUT" />
                  <el-option label="DELETE" value="DELETE" />
                  <el-option label="HEAD" value="HEAD" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="请求参数">
            <el-input
              v-model="form.requestParams"
              type="textarea"
              :rows="4"
              placeholder='支持录入 JSON 文本，例如：{"taskId":"TASK-001"}'
            />
          </el-form-item>
        </template>

        <template v-else-if="isFileType(form.sourceType)">
          <el-divider content-position="left">文件接入</el-divider>
          <el-form-item label="上传文件">
            <div class="file-upload-row">
              <el-upload
                :show-file-list="false"
                :limit="1"
                :accept="currentFileAccept"
                :before-upload="beforeFileUpload"
                :http-request="handleFileUpload"
                :on-exceed="handleUploadExceed"
                :disabled="fileUploading"
              >
                <el-button type="primary" :loading="fileUploading">上传到文件服务</el-button>
              </el-upload>
              <el-button v-if="form.filePath" link type="danger" @click="clearUploadedFile">清空</el-button>
            </div>
            <div class="form-tip">
              文件统一托管到 <code>{{ currentFileDirPrefix }}</code> 目录，不支持手填本地路径。
            </div>
          </el-form-item>
          <el-form-item label="文件服务路径" prop="filePath">
            <el-input v-model="form.filePath" readonly placeholder="请先上传文件，系统将自动回填文件服务路径" />
            <div v-if="form.filePath" class="form-tip">当前文件：{{ extractFileName(form.filePath) }}</div>
          </el-form-item>
        </template>

        <el-divider content-position="left">补充说明</el-divider>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入数据源说明" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button v-if="!isFileType(form.sourceType)" type="success" :loading="testLoading" @click="handleTest">测试连接</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onMounted, reactive, ref } from 'vue'
import {
  addDataSource,
  delDataSource,
  getDataSource,
  listDataSource,
  testDataSource,
  updateDataSource,
  uploadDataSourceFile
} from '@/api/zhpg/dataSource'

const { proxy } = getCurrentInstance()

const showSearch = ref(true)
const loading = ref(false)
const submitLoading = ref(false)
const testLoading = ref(false)
const fileUploading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const queryRef = ref()
const formRef = ref()
const dataSourceList = ref([])
const total = ref(0)
const ids = ref([])
const multiple = ref(true)

const sourceTypeOptions = [
  { label: 'MySQL', value: 'MYSQL' },
  { label: 'PostgreSQL', value: 'POSTGRESQL' },
  { label: 'Oracle', value: 'ORACLE' },
  { label: 'Kingbase', value: 'KINGBASE' },
  { label: 'Hive', value: 'HIVE' },
  { label: 'REST API', value: 'REST_API' },
  { label: 'CSV', value: 'CSV' },
  { label: 'Excel', value: 'EXCEL' },
  { label: 'TXT', value: 'TXT' },
  { label: 'Word', value: 'WORD' },
  { label: 'WPS', value: 'WPS' },
  { label: 'PDF', value: 'PDF' },
  { label: 'DAT', value: 'DAT' },
  { label: 'JSON', value: 'JSON' }
]

const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' }
]

const dbTypes = ['MYSQL', 'POSTGRESQL', 'ORACLE', 'KINGBASE', 'HIVE']
const fileTypes = ['CSV', 'EXCEL', 'TXT', 'WORD', 'WPS', 'PDF', 'DAT', 'JSON']
const fileExtensionMap = {
  CSV: ['csv'],
  EXCEL: ['xls', 'xlsx'],
  TXT: ['txt'],
  WORD: ['doc', 'docx'],
  WPS: ['wps', 'et', 'dps'],
  PDF: ['pdf'],
  DAT: ['dat'],
  JSON: ['json']
}
const maxFileSizeMb = 50

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sourceName: undefined,
  sourceType: undefined,
  status: undefined
})

const form = reactive({
  id: undefined,
  sourceName: '',
  sourceType: 'MYSQL',
  host: '',
  port: 3306,
  databaseName: '',
  schemaName: '',
  tableName: '',
  apiUrl: '',
  requestMethod: 'GET',
  filePath: '',
  username: '',
  password: '',
  fieldNames: '',
  requestParams: '',
  description: '',
  status: 'ENABLED'
})

const rules = {
  sourceName: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
  sourceType: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
  host: [{ validator: validateByType, trigger: 'blur' }],
  databaseName: [{ validator: validateByType, trigger: 'blur' }],
  apiUrl: [{ validator: validateByType, trigger: 'blur' }],
  filePath: [{ validator: validateByType, trigger: 'change' }]
}

const currentFileExtensions = computed(() => fileExtensionMap[form.sourceType] || [])
const currentFileAccept = computed(() => currentFileExtensions.value.map(item => `.${item}`).join(','))
const currentFileDirPrefix = computed(() => `zhpg/evalDataSource/${String(form.sourceType || '').toLowerCase()}/yyyyMM/`)

function getList() {
  loading.value = true
  listDataSource(queryParams).then(res => {
    dataSourceList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryRef.value?.resetFields()
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  multiple.value = !selection.length
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增评估数据源'
  dialogVisible.value = true
}

async function handleUpdate(row) {
  resetForm()
  const res = await getDataSource(row.id)
  Object.assign(form, normalizeForm(res.data || {}))
  dialogTitle.value = '编辑评估数据源'
  dialogVisible.value = true
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除数据源“${row.sourceName}”吗？`).then(() => {
    return delDataSource(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleBatchDelete() {
  proxy.$modal.confirm('确认删除选中的评估数据源吗？').then(() => {
    return delDataSource(ids.value.join(','))
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

async function handleRowTest(row) {
  testLoading.value = true
  try {
    const res = await testDataSource({ id: row.id })
    proxy.$modal.msgSuccess(res.data?.message || '测试完成')
    getList()
  } finally {
    testLoading.value = false
  }
}

function handleTypeChange(type) {
  form.filePath = ''
  if (isDatabaseType(type)) {
    form.apiUrl = ''
    form.requestMethod = 'GET'
    form.port = defaultPort(type)
    return
  }
  if (isApiType(type)) {
    form.host = ''
    form.port = undefined
    form.databaseName = ''
    form.schemaName = ''
    form.tableName = ''
    return
  }
  if (isFileType(type)) {
    form.host = ''
    form.port = undefined
    form.databaseName = ''
    form.schemaName = ''
    form.tableName = ''
    form.apiUrl = ''
    form.requestMethod = 'GET'
    form.username = ''
    form.password = ''
  }
}

function handleTest() {
  formRef.value?.validate(async valid => {
    if (!valid) return
    testLoading.value = true
    try {
      const res = await testDataSource(buildSubmitData())
      if (res.data?.status === 'SUCCESS') {
        proxy.$modal.msgSuccess(res.data.message || '连接测试成功')
      } else {
        proxy.$modal.msgError(res.data?.message || '连接测试失败')
      }
      if (form.id) {
        getList()
      }
    } finally {
      testLoading.value = false
    }
  })
}

function submitForm() {
  formRef.value?.validate(async valid => {
    if (!valid) return
    submitLoading.value = true
    try {
      const data = buildSubmitData()
      if (data.id) {
        await updateDataSource(data)
      } else {
        await addDataSource(data)
      }
      proxy.$modal.msgSuccess('保存成功')
      dialogVisible.value = false
      getList()
    } finally {
      submitLoading.value = false
    }
  })
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    sourceName: '',
    sourceType: 'MYSQL',
    host: '',
    port: 3306,
    databaseName: '',
    schemaName: '',
    tableName: '',
    apiUrl: '',
    requestMethod: 'GET',
    filePath: '',
    username: '',
    password: '',
    fieldNames: '',
    requestParams: '',
    description: '',
    status: 'ENABLED'
  })
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

function buildSubmitData() {
  return {
    id: form.id,
    sourceName: form.sourceName,
    sourceType: form.sourceType,
    host: form.host,
    port: form.port,
    databaseName: form.databaseName,
    schemaName: form.schemaName,
    tableName: form.tableName,
    apiUrl: form.apiUrl,
    requestMethod: form.requestMethod,
    filePath: form.filePath,
    username: form.username,
    password: form.password,
    fieldNames: form.fieldNames,
    requestParams: form.requestParams,
    description: form.description,
    status: form.status
  }
}

function normalizeForm(data) {
  return {
    id: data.id,
    sourceName: data.sourceName || '',
    sourceType: data.sourceType || 'MYSQL',
    host: data.host || '',
    port: data.port || defaultPort(data.sourceType || 'MYSQL'),
    databaseName: data.databaseName || '',
    schemaName: data.schemaName || '',
    tableName: data.tableName || '',
    apiUrl: data.apiUrl || '',
    requestMethod: data.requestMethod || 'GET',
    filePath: data.filePath || '',
    username: data.username || '',
    password: data.password || '',
    fieldNames: data.fieldNames || '',
    requestParams: data.requestParams || '',
    description: data.description || '',
    status: data.status || 'ENABLED'
  }
}

function validateByType(rule, value, callback) {
  if (rule.field === 'host' && isDatabaseType(form.sourceType) && !form.host) {
    callback(new Error('数据库类数据源必须填写主机地址'))
    return
  }
  if (rule.field === 'databaseName' && isDatabaseType(form.sourceType) && !form.databaseName) {
    callback(new Error('数据库类数据源必须填写数据库/服务名称'))
    return
  }
  if (rule.field === 'apiUrl' && isApiType(form.sourceType) && !form.apiUrl) {
    callback(new Error('REST 接口类数据源必须填写接口地址'))
    return
  }
  if (rule.field === 'filePath' && isFileType(form.sourceType) && !form.filePath) {
    callback(new Error('文件类数据源必须先上传文件并生成文件服务路径'))
    return
  }
  callback()
}

function beforeFileUpload(file) {
  if (!isFileType(form.sourceType)) {
    proxy.$modal.msgError('当前类型不支持上传文件')
    return false
  }
  const ext = getFileExtension(file.name)
  if (!currentFileExtensions.value.includes(ext)) {
    proxy.$modal.msgError(`当前类型仅支持 ${currentFileExtensions.value.join('/')} 格式文件`)
    return false
  }
  if (file.size / 1024 / 1024 > maxFileSizeMb) {
    proxy.$modal.msgError(`上传文件大小不能超过 ${maxFileSizeMb}MB`)
    return false
  }
  return true
}

async function handleFileUpload(option) {
  fileUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', option.file)
    formData.append('sourceType', form.sourceType)
    formData.append('sourceName', form.sourceName || '')
    const res = await uploadDataSourceFile(formData)
    const filePath = res.fileName || res.data
    if (!filePath) {
      throw new Error('上传成功但未返回文件服务路径')
    }
    form.filePath = filePath
    formRef.value?.validateField('filePath')
    option.onSuccess?.(res)
    proxy.$modal.msgSuccess(res.msg || '上传成功')
  } catch (error) {
    option.onError?.(error)
  } finally {
    fileUploading.value = false
  }
}

function handleUploadExceed() {
  proxy.$modal.msgError('每个文件类数据源仅允许上传 1 个文件')
}

function clearUploadedFile() {
  form.filePath = ''
  formRef.value?.validateField('filePath')
}

function isDatabaseType(type) {
  return dbTypes.includes(type)
}

function isApiType(type) {
  return type === 'REST_API'
}

function isFileType(type) {
  return fileTypes.includes(type)
}

function defaultPort(type) {
  const portMap = {
    MYSQL: 3306,
    POSTGRESQL: 5432,
    ORACLE: 1521,
    KINGBASE: 54321,
    HIVE: 10000
  }
  return portMap[type] || undefined
}

function getTypeLabel(value) {
  return sourceTypeOptions.find(item => item.value === value)?.label || value || ''
}

function getStatusLabel(value) {
  return statusOptions.find(item => item.value === value)?.label || value || ''
}

function buildTarget(row) {
  if (isDatabaseType(row.sourceType)) {
    const parts = []
    if (row.host) parts.push(row.host + (row.port ? `:${row.port}` : ''))
    if (row.databaseName) parts.push(row.databaseName)
    if (row.tableName) parts.push(row.tableName)
    return parts.join(' / ') || '-'
  }
  if (isApiType(row.sourceType)) {
    return row.apiUrl || '-'
  }
  if (isFileType(row.sourceType)) {
    return row.filePath || '-'
  }
  return '-'
}

function getFileExtension(fileName) {
  return fileName?.includes('.') ? fileName.split('.').pop().toLowerCase() : ''
}

function extractFileName(filePath) {
  if (!filePath) return ''
  const index = filePath.lastIndexOf('/')
  return index > -1 ? filePath.slice(index + 1) : filePath
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
}

.page-title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.page-desc {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.table-ops {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 4px;
}

.text-muted {
  color: #909399;
}

.file-upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.form-tip {
  margin-top: 8px;
  line-height: 1.5;
  color: #909399;
  font-size: 12px;
}
</style>
