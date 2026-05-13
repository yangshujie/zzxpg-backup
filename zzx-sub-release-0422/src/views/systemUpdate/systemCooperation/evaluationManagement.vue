<template>
  <div class="evaluation-management">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :model="queryParams" inline>
        <el-form-item label="方案名称">
          <el-input
            v-model="queryParams.schemeName"
            placeholder="请输入方案名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
          <el-button type="primary" @click="showAddDialog = true">
            <el-icon><Plus /></el-icon>
            新增评估方案
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 评估方案列表 -->
    <div class="scheme-list">
      <el-table v-loading="loading" :data="schemeList" style="width: 100%">
        <el-table-column label="方案名称" align="center" prop="schemeName" min-width="180" />
        <el-table-column label="方案类型" align="center" prop="schemeType" min-width="180">
          <template #default="{ row }">
            {{ getSchemeTypeLabel(row.schemeType) }}
          </template>
        </el-table-column>
        <el-table-column label="关联剖面" align="center" prop="profileName" min-width="180" />
        <el-table-column label="创建时间" align="center" prop="createTime" min-width="180">
          <template #default="{ row }">
            <span>{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建人" align="center" prop="creator" min-width="120" />
        <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" icon="Edit" @click="handleEdit(row)">修改</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <EvaluationSchemeDialog
      v-model:open="showAddDialog"
      :edit-data="currentEditData"
      @submit="handleDialogSubmit"
      @cancel="handleDialogCancel"
    />


  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import EvaluationSchemeDialog from './components/EvaluationSchemeDialog.vue'
import { 
  getEvaluationSchemeList, 
  addEvaluationScheme, 
  updateEvaluationScheme, 
  deleteEvaluationScheme
} from '@/api/systemPlus/systemCooperation/evaluation'

// 响应式数据
const showAddDialog = ref(false)
const currentEditData = ref(null)
const loading = ref(false)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  schemeName: '',
  schemeType: ''
})

const schemeList = ref([])
const total = ref(0)

// 方案类型字典
const schemeTypeOptions = [
  { label: '性能评估', value: 'performance' },
  { label: '效能评估', value: 'efficiency' },
  { label: '可靠性评估', value: 'reliability' }
]

// 获取方案类型标签
const getSchemeTypeLabel = (value) => {
  const option = schemeTypeOptions.find(item => item.value === value)
  return option ? option.label : value
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

// 加载评估方案列表
const loadSchemeList = async () => {
  try {
    loading.value = true
    const response = await getEvaluationSchemeList(queryParams)
    if (response.code === 200) {
      schemeList.value = response.data.list || []
      total.value = response.data.total || 0
    }
  } catch (error) {
    console.error('加载评估方案列表失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 分页处理
const handleSizeChange = (size) => {
  queryParams.pageSize = size
  loadSchemeList()
}

const handleCurrentChange = (page) => {
  queryParams.pageNum = page
  loadSchemeList()
}

// 编辑评估方案
const handleEdit = (row) => {
  currentEditData.value = { ...row }
  showAddDialog.value = true
}

// 删除评估方案
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该评估方案吗？', '提示', {
      type: 'warning'
    })
    
    await deleteEvaluationScheme({ schemeId: row.schemeId })
    ElMessage.success('删除成功')
    loadSchemeList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 搜索
const handleSearch = () => {
  queryParams.pageNum = 1
  loadSchemeList()
}

// 重置
const handleReset = () => {
  queryParams.schemeName = ''
  queryParams.schemeType = ''
  queryParams.pageNum = 1
  loadSchemeList()
}



// 弹窗提交处理
const handleDialogSubmit = async (formData) => {
  try {
    if (currentEditData.value) {
      // 编辑
      await updateEvaluationScheme({
        ...formData,
        schemeId: currentEditData.value.schemeId
      })
      ElMessage.success('更新成功')
    } else {
      // 新增
      await addEvaluationScheme(formData)
      ElMessage.success('新增成功')
    }
    
    showAddDialog.value = false
    currentEditData.value = null
    loadSchemeList()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}

// 弹窗取消处理
const handleDialogCancel = () => {
  showAddDialog.value = false
  currentEditData.value = null
}

// 初始化加载
onMounted(() => {
  loadSchemeList()
})
</script>

<style scoped>
.evaluation-management {
  padding: 20px;
  min-height: 100%;
}

.search-bar {
  margin-bottom: 20px;
  padding: 16px;
  background: var(--el-bg-color);
  border-radius: 4px;
}

.search-bar .el-form {
  margin-bottom: 0;
}

.scheme-list {
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
}

/* 适配暗色主题 */
:deep(.el-table) {
  background: var(--el-bg-color);
}

:deep(.el-table th) {
  background: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
}

:deep(.el-table td) {
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
}

:deep(.el-table--border) {
  border: 1px solid var(--el-border-color);
}

:deep(.el-table--border th) {
  border-right: 1px solid var(--el-border-color);
}

:deep(.el-table--border td) {
  border-right: 1px solid var(--el-border-color);
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: var(--el-fill-color-lighter);
}

/* 搜索栏样式适配 */
:deep(.el-form-item__label) {
  color: var(--el-text-color-regular);
}

:deep(.el-input__inner) {
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  border-color: var(--el-border-color);
}

:deep(.el-input__inner:focus) {
  border-color: var(--el-color-primary);
}
</style>