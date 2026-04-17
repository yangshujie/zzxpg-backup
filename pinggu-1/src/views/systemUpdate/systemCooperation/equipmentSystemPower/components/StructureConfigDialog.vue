<template>
  <el-dialog :model-value="open" width="1200px" append-to-body @update:model-value="$emit('update:open', $event)">
    <template #header>
      <div class="dialog-header">
        <span class="dialog-title">{{ props.title }}</span>
        <div class="editable-title-wrapper" @dblclick="handleTitleEdit">
          <template v-if="isEditingTitle">
            <el-input
              ref="titleInputRef"
              v-model="tempTitle"
              size="small"
              placeholder="请输入装备编组名称"
              @blur="handleTitleSave"
              @keyup.enter="handleTitleSave"
              @keyup.esc="handleTitleCancel"
              class="title-input-inline"
            />
          </template>
          <template v-else>
            <span class="editable-title-text">{{ form.name || '未命名编组' }}</span>
            <el-icon class="edit-icon"><Edit /></el-icon>
          </template>
        </div>
      </div>
    </template>

    <el-form ref="configRef" :model="form" :rules="rules" label-width="120px">
      <!-- 第一行：两个选择器放一行 -->
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="装备体系类型" prop="subSystemType">
            <el-select v-model="form.subSystemType" placeholder="请选择装备体系类型" style="width: 100%">
              <el-option 
                v-for="item in equipmentTypeOptions" 
                :key="item.value" 
                :label="item.label" 
                :value="item.value" 
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="阵营" prop="side">
            <el-select v-model="form.side" placeholder="请选择阵营" style="width: 100%">
              <el-option 
                v-for="item in campOptions" 
                :key="item.value" 
                :label="item.label" 
                :value="item.value" 
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 树形结构和三维地球 -->
      <el-row :gutter="20" class="structure-row">
        <el-col :span="6">
          <div class="tree-container">
            <div class="tree-header">
              <span class="tree-title">兵力编成</span>
              <div class="equipment-select-container">
                <el-button 
                  type="primary" 
                  :icon="Plus" 
                  size="small"
                  class="transparent-blue-btn"
                  @click="handleEquipmentSelectClick"
                >
                  选择装备
                </el-button>
                <el-button 
                  type="primary" 
                  :icon="Upload" 
                  size="small"
                  class="transparent-blue-btn"
                  @click="handleEquipmentImport"
                >
                  装备导入
                </el-button>
              </div>
            </div>
            <el-tree
              :data="forceTreeData"
              :props="treeProps"
              node-key="id"
              default-expand-all
              class="structure-tree"
            />
          </div>
        </el-col>
        <el-col :span="18">
          <div class="cesium-container" style="height: 600px;">
            <SimpleCesiumViewer ref="simpleCesiumViewer" />
          </div>
        </el-col>
      </el-row>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 选择装备对话框 -->
  <el-dialog v-model="selectDialogVisible" title="选择装备" width="800px" append-to-body>
    <el-select
      v-model="tempSelectedEquipmentIds"
      multiple
      filterable
      placeholder="请选择装备"
      style="width: 100%"
    >
      <el-option
        v-for="item in equipmentOptions"
        :key="item.id"
        :label="item.name"
        :value="item.id"
      />
    </el-select>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="selectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSelectConfirm">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="StructureConfigDialog">
import { ref, reactive, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'
import { useDict } from '@/utils/dict'
import SimpleCesiumViewer from '@/components/SimpleCesiumViewer.vue'
import { listEquipment, listEquipmentGroup } from '@/api/systemPlus/systemCooperation/equipment'

// 定义props
const props = defineProps({
  open: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '新增装备编组'
  },
  formData: {
    type: Object,
    default: () => ({})
  }
})

// 定义emits
const emit = defineEmits(['update:open', 'update:formData', 'submit', 'cancel'])

// 使用字典获取选项
const { subsystem_type: equipmentTypeOptions } = useDict('subsystem_type')

// 响应式数据
const configRef = ref()
const simpleCesiumViewer = ref(null)
const titleInputRef = ref(null)
const isEditingTitle = ref(false)
const tempTitle = ref('')
const selectDialogVisible = ref(false)
const equipmentOptions = ref([])
const tempSelectedEquipmentIds = ref([]) // 临时存储选择的装备ID列表
const equipmentMap = ref(new Map()) // 装备ID到完整对象的映射

// 阵营选项
const campOptions = ref([
  { value: 0, label: '红方' },
  { value: 1, label: '蓝方' }
])

// 表单数据
const form = reactive({
  id: null,
  name: '',
  subSystemType: null,
  side: null,
  formationDetail: []
})

// 表单验证规则
const rules = reactive({
  name: [{ required: true, message: "装备编组名称不能为空", trigger: "blur" }],
  subSystemType: [{ required: true, message: "装备体系类型不能为空", trigger: "change" }],
  side: [{ required: true, message: "阵营不能为空", trigger: "change" }]
})

// 树形结构配置
const treeProps = {
  children: 'children',
  label: 'name'
}

// 兵力编成树形数据
const forceTreeData = ref([])

// 监听props变化
watch(() => props.open, (newVal) => {
  if (newVal) {
    // 对话框打开时初始化数据
    Object.assign(form, props.formData)
    tempTitle.value = form.name || ''
    
    // 加载装备列表
    loadEquipmentList()
    
    // 构建树形数据
    buildTreeData()
  }
})

// 加载装备编组列表
const loadEquipmentList = async () => {
  try {
    const response = await listEquipment({ pageNum: 1, pageSize: 1000 })
    const rows = response.rows || response.data || []
    
    // 清空并重建映射
    equipmentMap.value.clear()
    
    equipmentOptions.value = rows.map(item => {
      const groupItem = {
        id: item.id,
        name: item.name,
        formationDetail: item.formationDetail || []
      }
      equipmentMap.value.set(item.id, groupItem)
      return groupItem
    })
    
    console.log('装备编组列表加载成功:', equipmentOptions.value.length)
  } catch (error) {
    console.error('装备编组列表加载失败:', error)
    ElMessage.error('装备编组列表加载失败')
  }
}

// 装备选择按钮点击事件
const handleEquipmentSelectClick = () => {
  // 打开对话框前，将已选择的装备ID同步到临时变量
  tempSelectedEquipmentIds.value = form.formationDetail.map(item => item.id)
  selectDialogVisible.value = true
}

// 确定按钮点击事件
const handleSelectConfirm = () => {
  if (tempSelectedEquipmentIds.value.length === 0) {
    ElMessage.warning('请选择至少一个装备编组')
    return
  }
  
  // 根据ID获取完整的装备编组对象
  const selectedGroups = tempSelectedEquipmentIds.value
    .map(id => equipmentMap.value.get(id))
    .filter(item => item !== undefined)
  
  if (selectedGroups.length !== tempSelectedEquipmentIds.value.length) {
    ElMessage.warning('部分装备编组数据加载失败，请重新选择')
    return
  }
  
  // 更新表单数据 - 存储编组信息
  form.formationDetail = selectedGroups.map(item => ({
    id: item.id,
    name: item.name,
    equipmentType: item.equipmentType
  }))
  
  // 更新树形数据
  buildTreeData()
  
  selectDialogVisible.value = false
  ElMessage.success(`已选择 ${form.formationDetail.length} 个装备编组`)
}

// 构建树形数据
const buildTreeData = () => {
  if (form.formationDetail && form.formationDetail.length > 0) {
    forceTreeData.value = [
      {
        id: 'main',
        name: '主力装备',
        children: form.formationDetail.map(item => ({
          id: item.id,
          name: item.name,
          equipmentType: item.equipmentType
        }))
      }
    ]
  } else {
    forceTreeData.value = []
  }
}

// 双击编辑标题
const handleTitleEdit = () => {
  isEditingTitle.value = true
  tempTitle.value = form.name || ''
  nextTick(() => {
    titleInputRef.value?.focus()
    titleInputRef.value?.select()
  })
}

// 保存标题编辑
const handleTitleSave = () => {
  const newTitle = tempTitle.value.trim()
  if (newTitle) {
    form.name = newTitle
  } else if (!newTitle && form.name) {
    tempTitle.value = form.name
  } else if (!newTitle && !form.name) {
    tempTitle.value = ''
  }
  isEditingTitle.value = false
}

// 取消标题编辑
const handleTitleCancel = () => {
  isEditingTitle.value = false
  tempTitle.value = form.name || ''
}

// 装备导入操作
const handleEquipmentImport = () => {
  ElMessage.info('装备导入功能开发中')
}

// 提交表单
const submitForm = () => {
  // 验证标题不能为空
  if (!form.name || !form.name.trim()) {
    ElMessage.warning('请输入装备编组名称')
    return
  }
  
  configRef.value?.validate(valid => {
    if (valid) {
      // 构建提交数据
      const submitData = {
        name: form.name,
        subSystemType: form.subSystemType,
        side: form.side,
        formationDetail: form.formationDetail
      }
      
      // 如果有id，说明是修改
      if (form.id) {
        submitData.id = form.id
      }
      
      emit('submit', submitData)
      emit('update:open', false)
    }
  })
}

// 取消操作
const cancel = () => {
  emit('cancel')
  emit('update:open', false)
}

// 暴露方法给父组件
defineExpose({
  resetForm: () => {
    form.id = null
    form.name = ''
    form.subSystemType = null
    form.side = null
    form.formationDetail = []
    tempSelectedEquipmentIds.value = []
    forceTreeData.value = []
    isEditingTitle.value = false
    tempTitle.value = ''
  }
})
</script>

<style scoped>
/* 样式保持不变 */
.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 20px;
}

.dialog-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.editable-title-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  background-color: #fafafa;
}

.editable-title-wrapper:hover {
  border-color: #409eff;
  background-color: #f5f7fa;
}

.editable-title-text {
  font-size: 14px;
  font-weight: 500;
  color: #409eff;
}

.edit-icon {
  color: #909399;
  font-size: 14px;
}

.title-input-inline {
  width: 200px;
}

.title-input-inline :deep(.el-input__wrapper) {
  padding: 0 8px;
}

.title-input-inline :deep(.el-input__inner) {
  height: 28px;
  line-height: 28px;
  font-size: 14px;
}

.structure-row {
  margin-top: 20px;
}

.tree-container {
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  padding: 15px;
  height: 600px;
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

.structure-tree {
  height: 320px;
  overflow-y: auto;
}

.cesium-container {
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  padding: 0;
  height: 600px;
  min-height: 400px;
}

.dialog-footer {
  text-align: center;
}

.dialog-footer .el-button {
  margin: 0 10px;
}

.equipment-select-container {
  display: flex;
  gap: 12px;
}

.transparent-blue-btn {
  background: rgba(64, 158, 255, 0.1) !important;
  border: 1px solid rgba(64, 158, 255, 0.3) !important;
  color: #409EFF !important;
  transition: all 0.3s ease;
}

.transparent-blue-btn:hover {
  background: rgba(64, 158, 255, 0.2) !important;
  border-color: rgba(64, 158, 255, 0.5) !important;
  transform: translateY(-1px);
}

.transparent-blue-btn:active {
  background: rgba(64, 158, 255, 0.3) !important;
  transform: translateY(0);
}
</style>