<template>
  <el-dialog
    title="选择外部数据源"
    :model-value="visible"
    width="900px"
    class="zhpg-external-data-dialog"
    append-to-body
    destroy-on-close
    @update:model-value="v => emit('update:visible', v)"
  >
    <div class="external-data-selector">
      <el-form :inline="true" :model="queryParams" class="demo-form-inline">
        <el-form-item label="分中心">
          <el-select v-model="queryParams.sourceSystem" placeholder="选择分中心" clearable filterable @change="handleSourceChange">
            <el-option v-for="dict in zhpg_equipment_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="目录/表">
          <el-input v-model="queryParams.tableName" placeholder="输入目录/表名关键词" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="selector-content">
        <div class="data-table-section">
          <el-table
            ref="tableRef"
            v-loading="loading"
            :data="dataList"
            height="400px"
            row-key="id"
            @selection-change="handleSelectionChange"
            @select="handleSelect"
            @select-all="handleSelectAll"
          >
            <el-table-column type="selection" width="55" align="center" />
            <el-table-column label="分中心" align="center" prop="sourceSystem" show-overflow-tooltip>
              <template #default="scope">
                {{ scope.row.sourceSystemName || getSourceLabel(scope.row.sourceSystem) }}
              </template>
            </el-table-column>
            <el-table-column label="目录" align="center" prop="tableName" show-overflow-tooltip />
            <el-table-column label="字段" align="center" prop="fieldName" show-overflow-tooltip />
            <el-table-column label="类型" align="center" prop="fieldType" width="100" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.fieldType || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="中文描述" align="center" prop="fieldComment" show-overflow-tooltip>
              <template #default="{ row }">
                <el-link type="primary" :underline="false">{{ row.fieldComment || '—' }}</el-link>
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
        </div>

        <div class="selected-summary">
          <div class="summary-header">
            <span>已选摘要</span>
            <el-button link type="danger" @click="clearAllSelected">清空全部</el-button>
          </div>
          <div class="summary-body">
            <div v-for="(fields, table) in groupedSelections" :key="table" class="summary-group">
              <div class="group-title">{{ table }}</div>
              <div class="group-tags">
                <el-tooltip
                  v-for="field in fields"
                  :key="field"
                  :content="fieldTooltip(tempStorage[table].fields.get(field))"
                  placement="top"
                >
                  <el-tag
                    closable
                    size="small"
                    class="field-tag"
                    @close="removeField(table, field)"
                  >
                    {{ field }}
                  </el-tag>
                </el-tooltip>
              </div>
            </div>
            <el-empty v-if="Object.keys(groupedSelections).length === 0" description="暂未选择任何字段" :image-size="60" />
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :disabled="Object.keys(groupedSelections).length === 0" @click="submitSelection">确定选择</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listExternalData } from '@/api/zhpg/externalData'
import { useDict } from '@/utils/dict'
import { normalizeTableCreationRecords } from '@/utils/zhpg/externalDataRecord'

const { zhpg_equipment_type } = useDict('zhpg_equipment_type')

const props = defineProps({
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'selected'])

const loading = ref(false)
const total = ref(0)
const dataList = ref([])

watch(() => props.visible, (newVal) => {
  if (newVal) {
    getList()
  }
})

const selections = ref([])
// const sourceOptions = ref(['太空侦察', '太空态势感知', '太空攻防', '航天测运控', '航天发射', '海基航天'])

// 暂存所有选中的字段，结构：{ tableName: { source: '', fields: Map<fieldName, fieldMeta> } }
const tempStorage = reactive({})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sourceSystem: '',
  tableName: ''
})

/** 查询外部数据列表 */
async function getList() {
  loading.value = true
  try {
    const res = await listExternalData(queryParams)
    // 兼容 RuoYi 标准格式 和 MyBatis-Plus IPage 格式 (data.records)
    if (res.rows !== undefined) {
      dataList.value = normalizeTableCreationRecords(res.rows)
      total.value = res.total || 0
    } else if (res.data && res.data.records !== undefined) {
      dataList.value = normalizeTableCreationRecords(res.data.records)
      total.value = res.data.total || 0
    } else {
      dataList.value = []
      total.value = 0
    }
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.sourceSystem = ''
  queryParams.tableName = ''
  handleQuery()
}

const tableRef = ref(null)

function createStorageEntry(row) {
  return {
    source: row.sourceSystem,
    sourceName: row.sourceSystemName || '',
    tableDisplayName: row.tableDisplayName || row.tableName,
    fields: new Map()
  }
}

function createFieldMeta(row) {
  return {
    comment: row.fieldComment || row.fieldDisplayName || row.fieldName || '',
    displayName: row.fieldDisplayName || row.fieldComment || row.fieldName || '',
    type: row.fieldType || ''
  }
}

function addSelectedField(row) {
  if (!tempStorage[row.tableName]) {
    tempStorage[row.tableName] = createStorageEntry(row)
  }
  tempStorage[row.tableName].fields.set(row.fieldName, createFieldMeta(row))
}

function fieldTooltip(fieldMeta) {
  if (!fieldMeta) return '无描述'
  if (typeof fieldMeta === 'string') return fieldMeta || '无描述'
  const parts = []
  if (fieldMeta.comment) parts.push(fieldMeta.comment)
  if (fieldMeta.type) parts.push(`类型：${fieldMeta.type}`)
  return parts.join(' / ') || '无描述'
}

/** 勾选变化处理（主控逻辑） */
function handleSelect(selection, row) {
  const isSelected = selection.includes(row)
  if (!isSelected) {
    // 只是取消勾选，直接移除
    removeField(row.tableName, row.fieldName)
    return
  }

  // 检查是否切换表
  const existingTables = Object.keys(tempStorage)
  if (existingTables.length > 0 && existingTables[0] !== row.tableName) {
    ElMessageBox.confirm(
      `您已选择了目录【${existingTables[0]}】的字段，切换到目录【${row.tableName}】将清空之前的选择，是否继续？`,
      '切换目录提醒',
      { confirmButtonText: '确定切换', cancelButtonText: '取消', type: 'warning' }
    ).then(() => {
      // 确定切换：清空旧的，选中新的
      for (const t in tempStorage) delete tempStorage[t]
      // 手动同步表格勾选状态（除了当前点击的这一行，其他全部取消勾选）
      dataList.value.forEach(item => {
        if (item.id !== row.id) {
          tableRef.value?.toggleRowSelection(item, false)
        }
      })
      // 加入新表
      addSelectedField(row)
    }).catch(() => {
      // 取消切换：把刚才勾选的那一行取消掉
      tableRef.value?.toggleRowSelection(row, false)
    })
    return
  }

  // 同表或首选，正常添加
  addSelectedField(row)
}

function handleSelectAll(selection) {
  if (selection.length === 0) {
    // 全取消逻辑（简化处理，提示用户手动清空可能更稳）
    return
  }
  
  // 检查当前页是否包含多张表
  const tablesInBatch = new Set(selection.map(r => r.tableName))
  if (tablesInBatch.size > 1) {
    ElMessage.warning('不支持跨目录全选，请单选字段或先通过搜索缩小范围')
    tableRef.value?.clearSelection()
    return
  }
  
  const targetTable = Array.from(tablesInBatch)[0]
  const existingTables = Object.keys(tempStorage)
  
  if (existingTables.length > 0 && existingTables[0] !== targetTable) {
    ElMessageBox.confirm(
      `切换到目录【${targetTable}】将清空之前的选择，是否继续？`,
      '全选切换提醒',
      { confirmButtonText: '确定切换', cancelButtonText: '取消', type: 'warning' }
    ).then(() => {
      for (const t in tempStorage) delete tempStorage[t]
      selection.forEach(row => {
        addSelectedField(row)
      })
    }).catch(() => {
      tableRef.value?.clearSelection()
    })
  } else {
    // 正常批量同步
    selection.forEach(row => {
      addSelectedField(row)
    })
  }
}

/** 勾选变化处理：同步暂存区（兜底逻辑） */
function handleSelectionChange(newSelections) {
  selections.value = newSelections
}

/** 检查分中心显示标签 */
function getSourceLabel(val) {
  if (!val) return ''
  const dict = zhpg_equipment_type.value.find(item => item.value === val)
  return dict ? dict.label : val
}

/** 监听分中心变化，如果已有选择则提醒 */
function handleSourceChange(val) {
  const existingTables = Object.keys(tempStorage)
  if (existingTables.length > 0) {
    const firstTable = existingTables[0]
    if (tempStorage[firstTable].source !== val && val !== '') {
      ElMessageBox.confirm(
        '切换分中心将清空当前已选择的所有字段，是否继续？',
        '切换提醒',
        { confirmButtonText: '确定切换', cancelButtonText: '取消', type: 'warning' }
      ).then(() => {
        clearAllSelected()
        handleQuery()
      }).catch(() => {
        queryParams.sourceSystem = tempStorage[firstTable].source
      })
      return
    }
  }
  handleQuery()
}

/** 计算分组摘要用于显示 */
const groupedSelections = computed(() => {
  const result = {}
  for (const table in tempStorage) {
    const fieldsArr = Array.from(tempStorage[table].fields.keys())
    if (fieldsArr.length > 0) {
      result[table] = fieldsArr
    }
  }
  return result
})

function removeField(table, field) {
  if (tempStorage[table]) {
    tempStorage[table].fields.delete(field)
    if (tempStorage[table].fields.size === 0) {
      delete tempStorage[table]
    }
  }
}

function clearAllSelected() {
  for (const key in tempStorage) {
    delete tempStorage[key]
  }
  selections.value = []
}

function submitSelection() {
  const result = []
  for (const table in tempStorage) {
    const fieldsMap = tempStorage[table].fields
    const fieldsList = []
    const fieldTypes = {}
    fieldsMap.forEach((meta, name) => {
      const obj = {}
      const comment = typeof meta === 'string' ? meta : meta?.comment
      const type = typeof meta === 'object' && meta ? meta.type : ''
      obj[name] = comment || name
      fieldsList.push(obj)
      if (type) fieldTypes[name] = type
    })
    result.push({
      directory: table,
      directoryName: tempStorage[table].tableDisplayName || table,
      source: tempStorage[table].source,
      sourceName: tempStorage[table].sourceName || '',
      fields: fieldsList,
      fieldTypes
    })
  }
  emit('selected', result)
  emit('update:visible', false)
}
</script>

<style scoped lang="scss">
.external-data-selector {
  .selector-content {
    display: flex;
    gap: 20px;
    margin-top: 10px;
  }
  
  .data-table-section {
    flex: 1;
    min-width: 0;
  }
  
  .selected-summary {
    width: 260px;
    border: 1px solid rgba(0, 242, 255, 0.16);
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    background: rgba(5, 13, 24, 0.92);
    
    .summary-header {
      padding: 10px 12px;
      border-bottom: 1px solid rgba(0, 242, 255, 0.16);
      background: rgba(0, 242, 255, 0.06);
      color: #d8e7f5;
      font-weight: bold;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 14px;
    }
    
    .summary-body {
      padding: 12px;
      flex: 1;
      overflow-y: auto;
      max-height: 440px;
    }
    
    .summary-group {
      margin-bottom: 15px;
      
      .group-title {
        font-size: 13px;
        color: #d8e7f5;
        font-weight: 600;
        margin-bottom: 8px;
        padding-bottom: 4px;
        border-bottom: 1px dashed rgba(0, 242, 255, 0.18);
      }
      
      .group-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
      }
      
      .field-tag {
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }
}
</style>

<style>
.zhpg-external-data-dialog.el-dialog {
  background: #050d18;
  border: 1px solid rgba(0, 242, 255, 0.16);
  box-shadow: 0 24px 72px rgba(0, 0, 0, 0.52);
}
.zhpg-external-data-dialog .el-dialog__header,
.zhpg-external-data-dialog .el-dialog__body,
.zhpg-external-data-dialog .el-dialog__footer {
  background: #050d18;
}
.zhpg-external-data-dialog .el-dialog__title {
  color: #d8e7f5;
}
.zhpg-external-data-dialog .el-dialog__headerbtn .el-dialog__close {
  color: #38bdf8;
}
.zhpg-external-data-dialog .el-form-item__label {
  color: #9fb6cb;
}
.zhpg-external-data-dialog .el-input__wrapper,
.zhpg-external-data-dialog .el-select__wrapper {
  background: rgba(5, 13, 24, 0.74);
  box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.16) inset;
}
.zhpg-external-data-dialog .el-input__inner,
.zhpg-external-data-dialog .el-select__placeholder,
.zhpg-external-data-dialog .el-select__selected-item {
  color: #d8e7f5;
}
.zhpg-external-data-dialog .el-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(0, 242, 255, 0.08);
  --el-table-header-text-color: #22d3ee;
  --el-table-text-color: #d8e7f5;
  --el-table-row-hover-bg-color: rgba(0, 242, 255, 0.08);
  --el-table-border-color: rgba(0, 242, 255, 0.12);
}
.zhpg-external-data-dialog .el-empty__description p {
  color: #9fb6cb;
}
</style>
