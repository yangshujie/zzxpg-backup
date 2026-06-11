<template>
  <el-dialog
    title="选择外部数据源"
    :model-value="visible"
    width="900px"
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
                {{ getSourceLabel(scope.row.sourceSystem) }}
              </template>
            </el-table-column>
            <el-table-column label="目录" align="center" prop="tableName" show-overflow-tooltip />
            <el-table-column label="字段" align="center" prop="fieldName" show-overflow-tooltip />
            <el-table-column label="中文描述" align="center" prop="fieldComment" show-overflow-tooltip />
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
                  :content="tempStorage[table].fields.get(field) || '无描述'"
                  placement="top"
                  :disabled="!tempStorage[table].fields.get(field)"
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
import { ZHPG_DATA_SOURCE_CENTER_OPTIONS, getZhpgEquipmentTypeLabel } from '@/constants/zhpgIndicatorSystem'

const zhpg_equipment_type = ZHPG_DATA_SOURCE_CENTER_OPTIONS

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

// 暂存所有选中的字段，结构：{ tableName: { source: '', fields: Map<fieldName, fieldComment> } }
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
      dataList.value = res.rows || []
      total.value = res.total || 0
    } else if (res.data && res.data.records !== undefined) {
      dataList.value = res.data.records || []
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
      // 确定切换：清空旧的
      for (const t in tempStorage) delete tempStorage[t]
      // 手动同步表格勾选状态（除了当前点击的这一行，其他全部取消勾选）
      dataList.value.forEach(item => {
        if (item.id !== row.id) {
          tableRef.value?.toggleRowSelection(item, false)
        }
      })
      // 选中新的
      tempStorage[row.tableName] = { source: row.sourceSystem, fields: new Map() }
      tempStorage[row.tableName].fields.set(row.fieldName, row.fieldComment || '')
    }).catch(() => {
      // 取消切换：把刚才勾选的那一行取消掉
      tableRef.value?.toggleRowSelection(row, false)
    })
    return
  }

  // 同表或首选，正常添加
  if (!tempStorage[row.tableName]) {
    tempStorage[row.tableName] = { source: row.sourceSystem, fields: new Map() }
  }
  tempStorage[row.tableName].fields.set(row.fieldName, row.fieldComment || '')
}

function handleSelectAll(selection) {
  if (selection.length === 0) {
    // 全取消逻辑：清空当前页对应的字段（简单处理：清空全部，因为暂存区也是基于单表）
    clearAllSelected()
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
        if (!tempStorage[row.tableName]) {
          tempStorage[row.tableName] = { source: row.sourceSystem, fields: new Map() }
        }
        tempStorage[row.tableName].fields.set(row.fieldName, row.fieldComment || '')
      })
    }).catch(() => {
      tableRef.value?.clearSelection()
    })
  } else {
    // 正常批量同步
    selection.forEach(row => {
      if (!tempStorage[row.tableName]) {
        tempStorage[row.tableName] = { source: row.sourceSystem, fields: new Map() }
      }
      tempStorage[row.tableName].fields.set(row.fieldName, row.fieldComment || '')
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
  return getZhpgEquipmentTypeLabel(val)
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
  for (const tableName in tempStorage) {
    const fieldsArr = Array.from(tempStorage[tableName].fields.keys())
    if (fieldsArr.length > 0) {
      result[tableName] = fieldsArr
    }
  }
  return result
})
function removeField(tableName, fieldName) {
  if (tempStorage[tableName]) {
    tempStorage[tableName].fields.delete(fieldName)
    if (tempStorage[tableName].fields.size === 0) {
      delete tempStorage[tableName]
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
    fieldsMap.forEach((comment, name) => {
      const obj = {}
      obj[name] = comment || name
      fieldsList.push(obj)
    })
    
    result.push({
      directory: table,
      source: tempStorage[table].source,
      fields: fieldsList
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
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    display: flex;
    flex-direction: column;
    background: #fcfdfe;
    
    .summary-header {
      padding: 10px 12px;
      border-bottom: 1px solid #e4e7ed;
      background: #f5f7fa;
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
        color: #606266;
        font-weight: 600;
        margin-bottom: 8px;
        padding-bottom: 4px;
        border-bottom: 1px dashed #dcdfe6;
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
