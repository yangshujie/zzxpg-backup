<template>
  <div class="app-container" :class="themeStore.currentThemeName">
    <div class="page-header mb12">
      <h2 class="page-title">评估准则集管理</h2>
      <p class="page-desc">维护评估准则集，关联指标体系及装备类型，设计指标准则（极值注入、一票否决、定级区间、短板补偿等）。</p>
    </div>

    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="90px">
      <el-form-item label="准则集名称" prop="setName">
        <el-input
          v-model="queryParams.setName"
          placeholder="请输入准则集名称"
          clearable
          style="width: 220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="指标体系" prop="systemName">
        <el-input
          v-model="queryParams.params.systemName"
          placeholder="请输入指标体系名称"
          clearable
          style="width: 220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="装备类型" prop="equipmentType">
        <el-select v-model="queryParams.equipmentType" placeholder="全部" clearable style="width: 180px">
          <el-option
            v-for="item in ZHPG_EQUIPMENT_TYPE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已禁用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <div class="toolbar-row mb12">
      <el-button type="primary" plain icon="Plus" @click="handleAdd">新建准则集</el-button>
      <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete(null)">批量删除</el-button>
    </div>

    <!-- 数据表格 -->
    <el-table
      v-loading="loading"
      :data="criterionSetList"
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" align="center" />
      <el-table-column label="ID" prop="id" width="80" align="center" />
      <el-table-column label="准则集编号" prop="setCode" width="160" show-overflow-tooltip />
      <el-table-column label="准则集名称" prop="setName" min-width="180" show-overflow-tooltip />
      <el-table-column label="关联指标体系" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ getSystemName(scope.row.indicatorSystemId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="装备类型" prop="equipmentType" width="140" align="center">
        <template #default="scope">
          <el-tag type="info" effect="plain">{{ getEquipmentLabel(scope.row.equipmentType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="scope">
          <el-tag :type="getStatusTagType(scope.row.status)" size="small">
            {{ getStatusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" prop="updateTime" width="160" align="center">
        <template #default="scope">
          <span>{{ scope.row.updateTime || scope.row.createTime || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" align="center" fixed="right">
        <template #default="scope">
          <div class="table-ops">
            <el-button link type="primary" size="small" icon="Edit" @click="handleUpdate(scope.row)">编辑</el-button>
            <el-button link type="success" size="small" icon="Setting" @click="handleDesign(scope.row)">设计准则</el-button>
            <el-button link type="danger" size="small" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </div>
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

    <!-- 新增/修改准则集弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="550px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="准则集编号" prop="setCode">
          <el-input v-model="form.setCode" placeholder="留空则由系统自动生成" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="准则集名称" prop="setName">
          <el-input v-model="form.setName" placeholder="请输入准则集名称" />
        </el-form-item>
        <el-form-item label="关联指标体系" prop="indicatorSystemId">
          <el-select
            v-model="form.indicatorSystemId"
            placeholder="请选择绑定的指标体系"
            filterable
            clearable
            remote
            :remote-method="val => formSystemQuery = val"
            @visible-change="visible => { if (!visible) formSystemQuery = '' }"
            style="width: 100%"
            @change="handleSystemChange"
          >
            <el-option
              v-for="item in formSystemOptions"
              :key="item.id"
              :label="item.systemName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="装备类型" prop="equipmentType">
          <el-select v-model="form.equipmentType" placeholder="请选择装备类型" clearable style="width: 100%">
            <el-option
              v-for="item in ZHPG_EQUIPMENT_TYPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="DRAFT">草稿</el-radio>
            <el-radio value="PUBLISHED">发布</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入准则集说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useThemeStore } from '@/store/theme'
import {
  listSets,
  getSetInfo,
  addSet,
  editSet,
  removeSets
} from '@/api/zhpg/evalCriterion'
import { listIndicatorSystem } from '@/api/zhpg/indicatorSystem'
import {
  ZHPG_EQUIPMENT_TYPE_OPTIONS,
  getZhpgEquipmentTypeLabel
} from '@/constants/zhpgIndicatorSystem'

const router = useRouter()
const route = useRoute()
const themeStore = useThemeStore()

const formSystemQuery = ref('')

const formSystemOptions = computed(() => {
  const query = formSystemQuery.value.trim().toLowerCase()
  let list = systemOptions.value || []
  if (query) {
    list = list.filter(item => 
      (item.systemName && item.systemName.toLowerCase().includes(query)) ||
      (String(item.id).includes(query))
    )
  }
  const result = list.slice(0, 50)
  const selectedId = form.indicatorSystemId
  if (selectedId && !result.some(item => item.id === selectedId)) {
    const selectedItem = systemOptions.value.find(item => item.id === selectedId)
    if (selectedItem) {
      result.push(selectedItem)
    }
  }
  return result
})

// 状态定义
const loading = ref(false)
const submitLoading = ref(false)
const total = ref(0)
const criterionSetList = ref([])
const systemOptions = ref([])
const systemMap = ref({})
const ids = ref([])
const multiple = ref(true)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  setName: undefined,
  equipmentType: undefined,
  status: undefined,
  indicatorSystemId: undefined,
  params: {
    systemName: undefined
  }
})

const form = reactive({
  id: undefined,
  setCode: undefined,
  setName: undefined,
  indicatorSystemId: undefined,
  equipmentType: undefined,
  status: 'DRAFT',
  description: undefined
})

const rules = {
  setName: [{ required: true, message: '准则集名称不能为空', trigger: 'blur' }],
  indicatorSystemId: [{ required: true, message: '必须关联一个指标体系', trigger: 'change' }],
  equipmentType: [{ required: true, message: '请选择装备类型', trigger: 'change' }]
}

onMounted(() => {
  if (route.query.indicatorSystemId) {
    queryParams.indicatorSystemId = Number(route.query.indicatorSystemId)
  }
  getList()
  getSystemList()
})

// 加载列表数据
function getList() {
  loading.value = true
  listSets(queryParams)
    .then((res) => {
      criterionSetList.value = res.rows || []
      total.value = res.total || 0
    })
    .finally(() => {
      loading.value = false
    })
}

// 加载全部指标体系，用来做下拉选择
function getSystemList() {
  listIndicatorSystem({ pageNum: 1, pageSize: 1000 })
    .then((res) => {
      systemOptions.value = res.rows || []
      const map = {}
      systemOptions.value.forEach((item) => {
        map[item.id] = item.systemName
      })
      systemMap.value = map

      // 如果有路由传入的 id，反显指标体系名称到输入框
      if (queryParams.indicatorSystemId) {
        const sys = systemOptions.value.find(item => item.id === queryParams.indicatorSystemId)
        if (sys) {
          if (!queryParams.params) queryParams.params = {}
          queryParams.params.systemName = sys.systemName
        }
      }
    })
}

// 翻译指标体系名称
function getSystemName(id) {
  return systemMap.value[id] || 'ID: ' + id
}

// 翻译装备类型
function getEquipmentLabel(val) {
  return getZhpgEquipmentTypeLabel(val)
}

// 获取状态文案
function getStatusLabel(status) {
  const map = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    DISABLED: '已停用'
  }
  return map[status] || status
}

// 获取状态标签样式
function getStatusTagType(status) {
  const map = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    DISABLED: 'danger'
  }
  return map[status] || 'info'
}

// 检索/过滤
function handleQuery() {
  queryParams.pageNum = 1
  if (!queryParams.params?.systemName) {
    queryParams.indicatorSystemId = undefined
  }
  getList()
}

// 重置搜索
function resetQuery() {
  queryParams.setName = undefined
  queryParams.equipmentType = undefined
  queryParams.status = undefined
  queryParams.indicatorSystemId = undefined
  if (queryParams.params) {
    queryParams.params.systemName = undefined
  }
  handleQuery()
}

// 多选发生改变
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  multiple.value = !selection.length
}

// 指标体系改变时，自动将装备类型同步为其默认装备类型
function handleSystemChange(systemId) {
  if (!systemId) return
  const sys = systemOptions.value.find(item => item.id === systemId)
  if (sys && sys.equipmentType) {
    form.equipmentType = sys.equipmentType
  }
}

// 重置表单
function reset() {
  form.id = undefined
  form.setCode = undefined
  form.setName = undefined
  form.indicatorSystemId = undefined
  form.equipmentType = undefined
  form.status = 'DRAFT'
  form.description = undefined
}

// 新增按钮点击
function handleAdd() {
  reset()
  dialogTitle.value = '新建准则集'
  if (queryParams.indicatorSystemId) {
    form.indicatorSystemId = queryParams.indicatorSystemId
    handleSystemChange(queryParams.indicatorSystemId)
  }
  dialogVisible.value = true
}

// 修改按钮点击
function handleUpdate(row) {
  reset()
  getSetInfo(row.id).then((res) => {
    Object.assign(form, res.data)
    dialogTitle.value = '修改准则集'
    dialogVisible.value = true
  })
}

// 确定提交
function submitForm() {
  formRef.value.validate((valid) => {
    if (valid) {
      submitLoading.value = true
      // 如果 code 为空，自动生成一个唯一 code
      if (!form.setCode) {
        form.setCode = 'CRIT_' + Date.now()
      }
      
      const operation = form.id ? editSet(form) : addSet(form)
      operation
        .then(() => {
          ElMessage.success(form.id ? '修改成功' : '新增成功')
          dialogVisible.value = false
          getList()
        })
        .finally(() => {
          submitLoading.value = false
        })
    }
  })
}

// 删除或批量删除
function handleDelete(row) {
  const targetIds = row ? [row.id] : ids.value
  if (!targetIds.length) return

  ElMessageBox.confirm(
    `是否确认删除ID为 "${targetIds.join(',')}" 的评估准则集数据？删除后，该准则集配下的所有规则明细也将一并清空！`,
    '系统提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    loading.value = true
    removeSets(targetIds.join(','))
      .then(() => {
        ElMessage.success('删除成功')
        getList()
      })
      .finally(() => {
        loading.value = false
      })
  }).catch(() => {})
}

// 跳转到准则设计师
function handleDesign(row) {
  router.push({
    name: 'TemplateCriterionDesigner',
    query: { setId: row.id }
  })
}
</script>

<style scoped>
.page-header {
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 12px;
}
.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.page-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.toolbar-row {
  display: flex;
  justify-content: flex-start;
  gap: 10px;
}
.table-ops {
  display: flex;
  justify-content: center;
  gap: 8px;
}
</style>
