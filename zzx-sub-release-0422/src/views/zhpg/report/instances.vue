<template>
  <div class="report-instance-page">
    <el-form ref="queryRef" :model="queryParams" :inline="true" class="search-panel">
      <el-form-item label="报告编号" prop="reportCode">
        <el-input
          v-model="queryParams.reportCode"
          placeholder="请输入报告编号"
          clearable
          style="width: 220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="评估任务" prop="taskName">
        <el-input
          v-model="queryParams.taskName"
          placeholder="请输入任务名称"
          clearable
          style="width: 240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="结果编号" prop="evalResultCode">
        <el-input
          v-model="queryParams.evalResultCode"
          placeholder="请输入结果编号"
          clearable
          style="width: 220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="生成状态" prop="renderStatus">
        <el-select v-model="queryParams.renderStatus" placeholder="请选择状态" clearable style="width: 150px">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="生成中" value="PENDING" />
          <el-option label="失败" value="FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar-panel">
      <div class="summary">
        <span class="summary-main">评估报告管理</span>
        <span class="summary-sub">承接评估计算流程“生成报告”产物，可预览、下载与清理历史版本</span>
      </div>
      <div class="toolbar-actions">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
        <el-button icon="Refresh" @click="getList">刷新</el-button>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="reportList"
      table-layout="fixed"
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" align="center" />
      <el-table-column prop="reportCode" label="报告编号" min-width="170" show-overflow-tooltip />
      <el-table-column prop="generationNo" label="版本" width="80" align="center">
        <template #default="{ row }">第 {{ row.generationNo || '-' }} 版</template>
      </el-table-column>
      <el-table-column label="评估任务" min-width="230" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="task-cell">
            <span class="task-name">{{ row.taskName || '未关联任务名称' }}</span>
            <span class="task-meta">{{ row.evalResultCode || row.reportCode || '-' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="indicatorSystemName" label="指标体系" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.indicatorSystemName || '-' }}</template>
      </el-table-column>
      <el-table-column label="评估结论" width="150" align="center">
        <template #default="{ row }">
          <div class="score-cell">
            <span class="score-value">{{ formatScore(row.score) }}</span>
            <el-tag v-if="row.grade" size="small" type="info">{{ row.grade }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="reportTemplateName" label="报告模板" min-width="190" show-overflow-tooltip />
      <el-table-column prop="renderStatus" label="状态" width="92" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.renderStatus)" size="small">{{ statusLabel(row.renderStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createBy" label="生成人" width="110" show-overflow-tooltip />
      <el-table-column prop="createTime" label="生成时间" width="170" />
      <el-table-column label="文件" width="112" align="center">
        <template #default="{ row }">
          <div class="file-tags">
            <el-tag v-if="row.wordUrl" type="info" size="small">Word</el-tag>
            <el-tag v-if="row.pdfUrl" type="success" size="small">PDF</el-tag>
            <span v-if="!row.wordUrl && !row.pdfUrl" class="empty-text">无</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <div class="row-actions">
            <el-button link type="primary" icon="View" @click="handlePreview(row)">预览</el-button>
            <el-dropdown trigger="click" @command="(format) => handleDownload(row, format)">
              <el-button link type="primary" icon="Download">
                下载<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="word">Word 报告</el-dropdown-item>
                  <el-dropdown-item command="pdf">PDF 报告</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog
      v-model="previewVisible"
      title="报告预览"
      width="80%"
      append-to-body
      destroy-on-close
      class="report-preview-dialog"
      @closed="closePreview"
    >
      <div class="report-preview-body" v-loading="previewLoading">
        <iframe v-if="previewUrl" :src="previewUrl" class="report-preview-frame" frameborder="0" />
        <el-empty v-else description="暂无可预览地址，可下载报告文件查看" />
      </div>
      <template #footer>
        <el-dropdown v-if="previewRow" trigger="click" @command="(format) => handleDownload(previewRow, format)">
          <el-button type="primary" plain icon="Download">
            下载报告<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="word">Word 报告</el-dropdown-item>
              <el-dropdown-item command="pdf">PDF 报告</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { getCurrentInstance, onMounted, reactive, ref } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { delEvalReport, getEvalReportLinks, listEvalReports } from '@/api/zhpg/evalReport'

const { proxy } = getCurrentInstance()

const queryRef = ref(null)
const loading = ref(false)
const reportList = ref([])
const total = ref(0)
const ids = ref([])
const multiple = ref(true)
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewUrl = ref('')
const previewRow = ref(null)
const linkCache = new Map()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  reportCode: '',
  taskName: '',
  evalResultCode: '',
  renderStatus: ''
})

function unwrapRows(res) {
  if (Array.isArray(res?.rows)) return res.rows
  if (Array.isArray(res?.data?.rows)) return res.data.rows
  if (Array.isArray(res?.data)) return res.data
  return []
}

function unwrapTotal(res) {
  return Number(res?.total ?? res?.data?.total ?? unwrapRows(res).length)
}

function buildQuery() {
  const query = { ...queryParams }
  Object.keys(query).forEach(key => {
    if (query[key] === '') delete query[key]
  })
  return query
}

async function getList() {
  loading.value = true
  try {
    const res = await listEvalReports(buildQuery())
    reportList.value = unwrapRows(res)
    total.value = unwrapTotal(res)
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryRef.value?.resetFields()
  queryParams.pageNum = 1
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id).filter(Boolean)
  multiple.value = ids.value.length === 0
}

function statusLabel(status) {
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'PENDING') return '生成中'
  return status || '未知'
}

function statusTagType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'PENDING') return 'warning'
  return 'info'
}

function formatScore(score) {
  if (score === null || score === undefined || score === '') return '-'
  const value = Number(score)
  return Number.isFinite(value) ? value.toFixed(1) : score
}

async function fetchLinks(row) {
  if (!row?.id) return {}
  if (linkCache.has(row.id)) return linkCache.get(row.id)
  const res = await getEvalReportLinks(row.id)
  const links = res?.data || {}
  linkCache.set(row.id, links)
  return links
}

async function handlePreview(row) {
  if (!row?.id) return
  previewVisible.value = true
  previewLoading.value = true
  previewUrl.value = ''
  previewRow.value = row
  try {
    const links = await fetchLinks(row)
    previewUrl.value = links.previewUrl || ''
    if (!previewUrl.value) {
      ElMessage.warning('暂无可预览地址，可下载 Word 或 PDF 报告查看')
    }
  } catch (e) {
    previewVisible.value = false
    ElMessage.error(e?.message || '获取报告预览地址失败')
  } finally {
    previewLoading.value = false
  }
}

function closePreview() {
  previewUrl.value = ''
  previewRow.value = null
  previewLoading.value = false
}

async function handleDownload(row, format = 'word') {
  if (!row?.id) return
  try {
    const links = await fetchLinks(row)
    const isPdf = format === 'pdf'
    const url = isPdf ? (links.pdfUrl || links.reportUrl) : (links.wordUrl || links.reportUrl)
    if (!url) {
      ElMessage.warning(`暂无${isPdf ? 'PDF' : 'Word'}报告可下载`)
      return
    }
    const fileName = `${row.reportCode || `eval_report_${row.id}`}.${isPdf ? 'pdf' : 'docx'}`
    proxy.download('file/downloadReport', { url }, fileName)
  } catch (e) {
    ElMessage.error(e?.message || '下载报告失败')
  }
}

async function handleDelete(row) {
  const targetIds = row?.id ? [row.id] : ids.value
  if (!targetIds.length) return
  const label = row?.reportCode ? `“${row.reportCode}”` : `选中的 ${targetIds.length} 个报告`
  try {
    await ElMessageBox.confirm(`确定删除${label}吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await delEvalReport(targetIds.join(','))
    targetIds.forEach(id => linkCache.delete(id))
    ElMessage.success('删除成功')
    getList()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.report-instance-page {
  height: 100%;
  padding: 16px 20px;
  overflow: auto;
  background: var(--el-bg-color-page);
}

.search-panel,
.toolbar-panel {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.search-panel {
  padding: 18px 18px 0;
  margin-bottom: 12px;
}

.toolbar-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  margin-bottom: 12px;
}

.summary {
  display: flex;
  align-items: baseline;
  gap: 12px;
  min-width: 0;
}

.summary-main {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.summary-sub {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.toolbar-actions,
.file-tags,
.row-actions,
.score-cell {
  display: flex;
  align-items: center;
}

.toolbar-actions,
.file-tags,
.score-cell {
  gap: 8px;
}

.row-actions {
  gap: 4px;
}

.task-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.task-name,
.task-meta {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-name {
  color: var(--el-text-color-primary);
}

.task-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.score-value {
  min-width: 42px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.empty-text {
  color: var(--el-text-color-placeholder);
}

.report-preview-body {
  height: 70vh;
  min-height: 520px;
}

.report-preview-frame {
  width: 100%;
  height: 100%;
  border: 0;
  background: var(--el-fill-color-light);
}

@media (max-width: 900px) {
  .report-instance-page {
    padding: 12px;
  }

  .toolbar-panel,
  .summary {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
