<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" class="search-form">
      <el-form-item label="评估报告名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入评估报告名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="评估报告类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择评估报告类型" clearable style="width: 200px">
          <el-option
            v-for="item in reportTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 动态表格 -->
    <DynamicTable
      :table-data="tableData"
      :field-config="fieldConfig"
      :show-checkbox="true"
      :show-index="true"
      @selection-change="handleSelectionChange"
    >
      <!-- 操作列自定义插槽 -->
      <template #actionSlot="{ row }">
        <el-button link type="primary" icon="View" @click="handlePreview(row)">预览</el-button>
        <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
        <el-button link type="success" icon="Download" @click="handleExport(row)">导出</el-button>
      </template>
    </DynamicTable>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DynamicTable from '@/components/DynamicTable/index.vue'

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  type: ''
})

// 表格数据
const tableData = ref([])
const total = ref(0)
const loading = ref(false)

// 评估报告类型选项
const reportTypeOptions = ref([
  { value: '1', label: '航天侦察装备子体系' },
  { value: '2', label: '太空态势感知装备子体系' },
  { value: '3', label: '太空攻防装备子体系' },
  { value: '4', label: '航天发射装备子体系' },
  { value: '5', label: '航天测运控装备子体系' },
  { value: '6', label: '航天装备联合作战体系' }
])

// 字段配置
const fieldConfig = ref([
  { key: 'reportName', label: '评估报告名称', width: 200, showOverflowTooltip: true },
  { 
    key: 'reportType', 
    label: '评估报告类型', 
    width: 180,
    component: 'select',
    options: reportTypeOptions.value
  },
  { key: 'templateName', label: '评估报告模板', width: 150, showOverflowTooltip: true },
  { key: 'reportUrl', label: '评估报告地址', width: 200, showOverflowTooltip: true },
  { key: 'relatedProject', label: '关联评估工程', width: 180, showOverflowTooltip: true },
  { 
    key: 'action', 
    label: '操作', 
    width: 200, 
    customColumn: true, 
    slotName: 'actionSlot',
    fixed: 'right'
  }
])

// 模拟数据
const mockData = [
  {
    id: 1,
    reportName: '天链一号通信效能评估报告',
    reportType: '1',
    templateName: '通信效能评估模板',
    reportUrl: '/reports/tianlian-2026-001.pdf',
    relatedProject: 'PRJ-2026-90'
  },
  {
    id: 2,
    reportName: '星网态势感知能力分析报告',
    reportType: '2',
    templateName: '态势感知分析模板',
    reportUrl: '/reports/xingwang-2026-002.pdf',
    relatedProject: 'PRJ-2026-91'
  },
  {
    id: 3,
    reportName: '太空攻防装备效能评估报告',
    reportType: '3',
    templateName: '攻防效能评估模板',
    reportUrl: '/reports/taikong-2026-003.pdf',
    relatedProject: 'PRJ-2026-92'
  },
  {
    id: 4,
    reportName: '航天发射任务综合评估报告',
    reportType: '4',
    templateName: '发射任务评估模板',
    reportUrl: '/reports/fashe-2026-004.pdf',
    relatedProject: 'PRJ-2026-93'
  },
  {
    id: 5,
    reportName: '测运控系统效能分析报告',
    reportType: '5',
    templateName: '测运控分析模板',
    reportUrl: '/reports/ceyunkong-2026-005.pdf',
    relatedProject: 'PRJ-2026-94'
  },
  {
    id: 6,
    reportName: '联合作战体系协同评估报告',
    reportType: '6',
    templateName: '协同评估模板',
    reportUrl: '/reports/lianhe-2026-006.pdf',
    relatedProject: 'PRJ-2026-95'
  }
]

// 获取列表数据
function getList() {
  loading.value = true
  
  // 模拟API调用
  setTimeout(() => {
    let filteredData = [...mockData]
    
    // 根据查询条件过滤数据
    if (queryParams.name) {
      filteredData = filteredData.filter(item => 
        item.reportName.includes(queryParams.name)
      )
    }
    
    if (queryParams.type) {
      filteredData = filteredData.filter(item => 
        item.reportType === queryParams.type
      )
    }
    
    // 分页处理
    const startIndex = (queryParams.pageNum - 1) * queryParams.pageSize
    const endIndex = startIndex + queryParams.pageSize
    
    tableData.value = filteredData.slice(startIndex, endIndex)
    total.value = filteredData.length
    loading.value = false
  }, 500)
}

// 查询操作
function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

// 重置查询
function resetQuery() {
  queryParams.name = ''
  queryParams.type = ''
  queryParams.pageNum = 1
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  console.log('选中数据:', selection)
}

// 预览报告
function handlePreview(row) {
  ElMessage.info(`预览报告: ${row.reportName}`)
  // 这里可以打开预览弹窗或跳转到预览页面
}

// 删除报告
function handleDelete(row) {
  ElMessageBox.confirm(
    `确定删除评估报告 "${row.reportName}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    // 模拟删除操作
    const index = mockData.findIndex(item => item.id === row.id)
    if (index !== -1) {
      mockData.splice(index, 1)
      getList()
      ElMessage.success('删除成功')
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 导出报告
function handleExport(row) {
  ElMessage.success(`正在导出报告: ${row.reportName}`)
  // 这里可以实现导出功能
}

// 初始化
onMounted(() => {
  getList()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.search-form {
  background: var(--el-bg-color);
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  border: 1px solid var(--el-border-color-light);
}

:deep(.el-table) {
  margin-top: 20px;
}

:deep(.el-table .cell) {
  padding: 8px 12px;
}

:deep(.el-button--small) {
  margin: 0 2px;
}
</style>