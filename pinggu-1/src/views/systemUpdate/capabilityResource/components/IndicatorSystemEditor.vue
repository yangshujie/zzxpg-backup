<template>
  <el-dialog
    :title="dialogTitle"
    v-model="dialogVisible"
    width="min(1400px, 96vw)"
    :close-on-click-modal="false"
    :before-close="handleClose"
    append-to-body
    class="indicator-dialog"
    :class="{ 'dark-theme': isDarkMode }"
  >
    <!-- 背景画布区域 -->
    <div class="canvas-background">
      <div class="mindmap-container">
        <mindmap
          ref="mindmapRef"
          v-model="mindmapData"
          :timetravel="true"
          :drag="true"
          :edit="true"
          :center-btn="false"
          :add-node-btn="false"
          :ctm="true"
          :zoom="true"
          @node-click="onMindmapNodeClick"
          @node-update="onMindmapNodeUpdate"
          @node-add="onMindmapNodeAdd"
        />
      </div>
    </div>

    <!-- 悬浮的左侧树结构区域 -->
    <div class="floating-tree-panel" :class="{ collapsed: treeCollapsed }">
      <div class="tree-toggle-btn" @click="toggleTreePanel">
        <el-icon><DArrowRight v-if="treeCollapsed" /><DArrowLeft v-else /></el-icon>
      </div>
      <div class="tree-content-wrapper">
        <div class="panel-header">
          <div class="header-title">
            <el-icon><FolderOpened /></el-icon>
            <span>指标体系结构</span>
          </div>
          <el-button type="primary" plain size="small" :icon="Plus" @click="addRootChildNode">添加子节点</el-button>
        </div>
        <div class="tree-content">
          <el-tree
            ref="treeRef"
            :data="treeData"
            :props="treeProps"
            :default-expand-all="true"
            :highlight-current="true"
            node-key="id"
            :expand-on-click-node="false"
            :current-node-key="currentNodeId"
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="custom-tree-node" :class="{ 'is-root': isRootNode(data) }">
                <div class="node-label">
                  <el-icon v-if="isRootNode(data)"><Monitor /></el-icon>
                  <el-icon v-else><Grid /></el-icon>
                  <span class="label-text">{{ node.label }}</span>
                  <el-tag v-if="data.levelNum" size="small" type="info" class="level-tag">L{{ data.levelNum }}</el-tag>
                </div>
                <div class="node-actions">
                  <el-tooltip content="添加子节点" placement="top">
                    <el-button text :icon="CirclePlus" size="small" @click.stop="addChildNode(data)" />
                  </el-tooltip>
                  <el-tooltip content="编辑" placement="top">
                    <el-button text :icon="Edit" size="small" @click.stop="editNode(data)" />
                  </el-tooltip>
                  <el-popconfirm
                    v-if="!isRootNode(data)"
                    title="是否确定删除此指标及其所有子指标?"
                    @confirm="deleteNode(data)"
                  >
                    <template #reference>
                      <el-tooltip content="删除" placement="top">
                        <el-button text :icon="Delete" size="small" style="color: #f56c6c;" />
                      </el-tooltip>
                    </template>
                  </el-popconfirm>
                </div>
              </div>
            </template>
          </el-tree>
        </div>
      </div>
    </div>

    <!-- 思维导图工具栏 -->
    <div class="mindmap-toolbar">
      <el-tooltip content="适应画布" placement="top">
        <el-button circle :icon="RefreshRight" @click="fitCanvas" />
      </el-tooltip>
      <el-tooltip content="放大" placement="top">
        <el-button circle :icon="ZoomIn" @click="zoomIn" />
      </el-tooltip>
      <el-tooltip content="缩小" placement="top">
        <el-button circle :icon="ZoomOut" @click="zoomOut" />
      </el-tooltip>
      <el-tooltip content="折叠所有节点" placement="top">
        <el-button circle :icon="Fold" @click="collapseAllMindmapNodes" />
      </el-tooltip>
      <el-tooltip content="展开所有节点" placement="top">
        <el-button circle :icon="Expand" @click="expandAllMindmapNodes" />
      </el-tooltip>
    </div>

    <!-- 节点编辑对话框 -->
    <el-dialog
      :title="nodeDialogTitle"
      v-model="nodeDialogVisible"
      width="560px"
      append-to-body
      :close-on-click-modal="false"
      class="node-edit-dialog"
      :class="{ 'dark-theme': isDarkMode }"
    >
      <el-form :model="currentNode" :rules="nodeRules" ref="nodeFormRef" label-width="110px" label-position="right">
        <el-divider content-position="left" v-if="isEditingRoot">
          <el-icon><Setting /></el-icon> 基础信息
        </el-divider>
        <el-form-item label="指标名称" prop="name">
          <el-input v-model="currentNode.name" placeholder="请输入指标名称" />
        </el-form-item>
        <el-form-item label="排序号" prop="orderNum">
          <el-input-number v-model="currentNode.orderNum" :min="1" :max="999" :step="1" style="width: 100%" />
        </el-form-item>
        
        <!-- 根节点额外字段 -->
        <template v-if="isEditingRoot">
          <el-divider content-position="left">
            <el-icon><Collection /></el-icon> 指标体系配置
          </el-divider>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="指标体系类型" prop="indicatorSetType">
                <el-select v-model="currentNode.indicatorSetType" placeholder="请选择" style="width: 100%">
                  <el-option label="作战效能" value="作战效能" />
                  <el-option label="作战适用性" value="作战适用性" />
                  <el-option label="体系适用性" value="体系适用性" />
                  <el-option label="在役适用性" value="在役适用性" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="子体系类型" prop="subSystemType">
                <el-select v-model="currentNode.subSystemType" placeholder="请选择" style="width: 100%">
                  <el-option label="卫星效能评估" value="1" />
                  <el-option label="地面系统" value="2" />
                  <el-option label="通信系统" value="3" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="指标体系对象" prop="indicatorSetObject">
                <el-input v-model="currentNode.indicatorSetObject" placeholder="请输入" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="权重算法" prop="weightAlgorithm">
                <el-input v-model="currentNode.weightAlgorithm" placeholder="请输入" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="审核状态" prop="reviewStatus">
                <el-select v-model="currentNode.reviewStatus" placeholder="请选择" style="width: 100%">
                  <el-option label="待审核" value="待审核" />
                  <el-option label="已通过" value="已通过" />
                  <el-option label="未通过" value="未通过" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="是否启用" prop="isEnabled">
                <el-switch v-model="currentNode.isEnabled" active-text="启用" inactive-text="禁用" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="nodeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveNode">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saveLoading">保存指标体系</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import mindmap from 'vue3-mindmap'
import 'vue3-mindmap/dist/style.css'
import { 
  CirclePlus, Edit, Delete, Plus, FolderOpened, Monitor, Grid, 
  Connection, RefreshRight, Setting, Collection, ZoomIn, ZoomOut,
  DArrowRight, DArrowLeft, Fold, Expand
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  editData: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:visible', 'save-success'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const dialogTitle = computed(() => props.title || (props.editData ? '编辑指标体系' : '新增指标体系'))

// 检测主题模式
const isDarkMode = ref(false)

const checkTheme = () => {
  const isDark = document.documentElement.classList.contains('dark') || 
                 document.body.getAttribute('data-theme') === 'dark' ||
                 (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches)
  isDarkMode.value = isDark
}

// 树折叠状态
const treeCollapsed = ref(false)

// 树数据
const treeData = ref([])
const treeRef = ref()
const treeProps = {
  children: 'children',
  label: 'name'
}
const currentNodeId = ref(null)

// 思维导图数据
const mindmapData = ref([])
const mindmapRef = ref()
const currentScale = ref(1)

// 节点编辑相关
const nodeDialogVisible = ref(false)
const nodeDialogTitle = ref('')
const currentNode = ref({})
const nodeFormRef = ref()
const isEditingRoot = ref(false)
const saveLoading = ref(false)

// 节点表单验证规则
const nodeRules = {
  name: [{ required: true, message: '指标名称不能为空', trigger: 'blur' }],
  orderNum: [{ required: true, message: '排序号不能为空', trigger: 'blur' }],
  indicatorSetType: [{ required: true, message: '请选择指标体系类型', trigger: 'change' }],
  indicatorSetObject: [{ required: true, message: '请输入指标体系对象', trigger: 'blur' }]
}

// 生成唯一ID
const generateId = () => {
  return Date.now() + '-' + Math.random().toString(36).substr(2, 9)
}

// 判断是否为根节点
const isRootNode = (data) => {
  return data.id === 'root' || data.parentId === undefined
}

// 初始化默认根节点
const initDefaultRoot = () => {
  return {
    id: 'root',
    name: '指标体系根节点',
    levelNum: 1,
    orderNum: 1,
    indicatorSetType: '',
    subSystemType: '',
    indicatorSetObject: '',
    isEnabled: true,
    weightAlgorithm: '层次分析法',
    reviewStatus: '待审核',
    children: []
  }
}

// 递归计算节点层级
const calculateLevelNum = (node, currentLevel = 1) => {
  node.levelNum = currentLevel
  if (node.children && node.children.length) {
    node.children.forEach(child => {
      calculateLevelNum(child, currentLevel + 1)
    })
  }
}

// 递归排序子节点
const sortChildrenByOrderNum = (node) => {
  if (node.children && node.children.length) {
    node.children.sort((a, b) => (a.orderNum || 0) - (b.orderNum || 0))
    node.children.forEach(child => sortChildrenByOrderNum(child))
  }
}

// 将后端数据转换为内部树结构
const convertToTreeData = (data) => {
  if (!data) return null
  
  const convert = (node) => {
    const newNode = {
      ...node,
      id: node.id || generateId(),
      children: node.children ? node.children.map(child => convert(child)) : []
    }
    return newNode
  }
  
  const tree = convert(data)
  sortChildrenByOrderNum(tree)
  return tree
}

// 将内部树结构转换为保存格式
const convertToSaveData = (node) => {
  const saveNode = {
    id: node.id === 'root' ? null : node.id,
    name: node.name,
    levelNum: node.levelNum,
    orderNum: node.orderNum,
    children: node.children ? node.children.map(child => convertToSaveData(child)) : []
  }
  
  if (node.id === 'root') {
    saveNode.indicatorSetType = node.indicatorSetType || ''
    saveNode.subSystemType = node.subSystemType || ''
    saveNode.indicatorSetObject = node.indicatorSetObject || ''
    saveNode.isEnabled = node.isEnabled !== undefined ? node.isEnabled : true
    saveNode.weightAlgorithm = node.weightAlgorithm || ''
    saveNode.reviewStatus = node.reviewStatus || ''
  }
  
  return saveNode
}

// 更新思维导图数据
const updateMindmapData = () => {
  if (!treeData.value || !treeData.value.length) {
    mindmapData.value = []
    return
  }
  
  const convertToMindmap = (node) => {
    const mindNode = {
      name: node.name,
      id: node.id
    }
    if (node.children && node.children.length) {
      mindNode.children = node.children.map(child => convertToMindmap(child))
    }
    return mindNode
  }
  
  mindmapData.value = treeData.value.map(node => convertToMindmap(node))
}

// 适应画布
const fitCanvas = () => {
  nextTick(() => {
    if (mindmapRef.value && mindmapRef.value.fitView) {
      mindmapRef.value.fitView()
    }
  })
}

// 放大
const zoomIn = () => {
  if (mindmapRef.value && mindmapRef.value.zoomIn) {
    mindmapRef.value.zoomIn()
    currentScale.value += 0.1
  }
}

// 缩小
const zoomOut = () => {
  if (mindmapRef.value && mindmapRef.value.zoomOut) {
    mindmapRef.value.zoomOut()
    currentScale.value -= 0.1
  }
}

// 展开所有思维导图节点
const expandAllMindmapNodes = () => {
  nextTick(() => {
    if (mindmapRef.value) {
      // 尝试多种可能的展开方法
      if (typeof mindmapRef.value.expandAll === 'function') {
        mindmapRef.value.expandAll()
      } else if (typeof mindmapRef.value.expandAllNodes === 'function') {
        mindmapRef.value.expandAllNodes()
      } else if (typeof mindmapRef.value.expandAllNode === 'function') {
        mindmapRef.value.expandAllNode()
      } else if (mindmapRef.value.$children && mindmapRef.value.$children[0]) {
        // 尝试访问子组件
        const child = mindmapRef.value.$children[0]
        if (child && typeof child.expandAll === 'function') {
          child.expandAll()
        }
      }
      
      // 备用方案：通过DOM操作尝试展开
      setTimeout(() => {
        const svg = document.querySelector('.mindmap-container svg')
        if (svg) {
          // 查找所有可折叠的节点并展开
          const collapsibleNodes = svg.querySelectorAll('.node-collapsible, .collapse-icon')
          collapsibleNodes.forEach(node => {
            if (node.getAttribute('data-expanded') === 'false' || 
                node.classList.contains('collapsed')) {
              node.click()
            }
          })
        }
      }, 200)
      
      ElMessage.success('已展开所有节点')
    }
  })
}

// 折叠所有思维导图节点
const collapseAllMindmapNodes = () => {
  nextTick(() => {
    if (mindmapRef.value) {
      // 尝试多种可能的折叠方法
      if (typeof mindmapRef.value.collapseAll === 'function') {
        mindmapRef.value.collapseAll()
      } else if (typeof mindmapRef.value.collapseAllNodes === 'function') {
        mindmapRef.value.collapseAllNodes()
      }
      
      ElMessage.success('已折叠所有节点')
    }
  })
}

// 展开左侧树所有节点
const expandAllTreeNodes = () => {
  if (treeRef.value) {
    const expandNode = (node) => {
      if (node.children && node.children.length) {
        treeRef.value.expandNode(node, true)
        node.children.forEach(child => expandNode(child))
      }
    }
    treeData.value.forEach(node => expandNode(node))
    ElMessage.success('已展开所有节点')
  }
}

// 折叠左侧树所有节点
const collapseAllTreeNodes = () => {
  if (treeRef.value) {
    const collapseNode = (node) => {
      if (node.children && node.children.length) {
        treeRef.value.expandNode(node, false)
        node.children.forEach(child => collapseNode(child))
      }
    }
    treeData.value.forEach(node => collapseNode(node))
    ElMessage.success('已折叠所有节点')
  }
}

// 切换树面板
const toggleTreePanel = () => {
  treeCollapsed.value = !treeCollapsed.value
}

// 监听树数据变化，同步到思维导图
watch(treeData, () => {
  updateMindmapData()
  setTimeout(() => {
    fitCanvas()
    expandAllMindmapNodes() // 数据更新后自动展开所有节点
  }, 100)
}, { deep: true })

// 添加子节点
const addChildNode = (parentNode) => {
  const maxOrder = parentNode.children?.length 
    ? Math.max(...parentNode.children.map(c => c.orderNum || 0), 0) 
    : 0
  
  const newNode = {
    id: generateId(),
    name: '新指标',
    levelNum: parentNode.levelNum + 1,
    orderNum: maxOrder + 1,
    children: []
  }
  
  if (!parentNode.children) {
    parentNode.children = []
  }
  parentNode.children.push(newNode)
  sortChildrenByOrderNum(parentNode)
  
  nextTick(() => {
    if (treeRef.value) {
      treeRef.value.expandNode(parentNode, true)
    }
  })
  
  setTimeout(() => {
    expandAllMindmapNodes() // 添加节点后展开
  }, 100)
  
  editNode(newNode)
}

// 添加根子节点
const addRootChildNode = () => {
  if (treeData.value.length) {
    addChildNode(treeData.value[0])
  } else {
    treeData.value = [initDefaultRoot()]
    addChildNode(treeData.value[0])
  }
}

// 编辑节点
const editNode = (node) => {
  currentNode.value = { ...node }
  isEditingRoot.value = isRootNode(node)
  nodeDialogTitle.value = isEditingRoot.value ? '编辑指标体系根节点' : '编辑指标'
  nodeDialogVisible.value = true
}

// 保存节点
const saveNode = async () => {
  try {
    await nodeFormRef.value?.validate()
    
    const targetNode = currentNode.value
    
    if (isEditingRoot.value && treeData.value.length) {
      Object.assign(treeData.value[0], targetNode)
    } else {
      const findAndUpdate = (nodes) => {
        for (let i = 0; i < nodes.length; i++) {
          if (nodes[i].id === targetNode.id) {
            Object.assign(nodes[i], targetNode)
            return true
          }
          if (nodes[i].children && findAndUpdate(nodes[i].children)) {
            return true
          }
        }
        return false
      }
      findAndUpdate(treeData.value)
    }
    
    if (treeData.value.length) {
      calculateLevelNum(treeData.value[0])
      sortChildrenByOrderNum(treeData.value[0])
    }
    
    nodeDialogVisible.value = false
    ElMessage.success('保存成功')
  } catch (error) {
    console.error('保存节点失败:', error)
    ElMessage.error('请填写完整的表单信息')
  }
}

// 删除节点
const deleteNode = (node) => {
  const deleteFromTree = (nodes) => {
    for (let i = 0; i < nodes.length; i++) {
      if (nodes[i].id === node.id) {
        nodes.splice(i, 1)
        return true
      }
      if (nodes[i].children && deleteFromTree(nodes[i].children)) {
        return true
      }
    }
    return false
  }
  deleteFromTree(treeData.value)
  
  setTimeout(() => {
    expandAllMindmapNodes() // 删除节点后重新展开
  }, 100)
  
  ElMessage.success('删除成功')
}

// 思维导图节点点击
const onMindmapNodeClick = (node, event) => {
  const findNodeById = (nodes, id) => {
    for (const item of nodes) {
      if (item.id === id) return item
      if (item.children) {
        const found = findNodeById(item.children, id)
        if (found) return found
      }
    }
    return null
  }
  
  const targetNode = findNodeById(treeData.value, node.id)
  if (targetNode) {
    currentNodeId.value = targetNode.id
    editNode(targetNode)
  }
}

// 思维导图节点更新
const onMindmapNodeUpdate = (node, oldNode) => {
  const findAndUpdate = (nodes) => {
    for (const item of nodes) {
      if (item.id === node.id) {
        item.name = node.name
        return true
      }
      if (item.children && findAndUpdate(item.children)) {
        return true
      }
    }
    return false
  }
  
  if (node.name && node.id) {
    findAndUpdate(treeData.value)
  }
}

// 思维导图添加节点
const onMindmapNodeAdd = (parentNode, newNode) => {
  const findParentAndAdd = (nodes, parentId) => {
    for (const item of nodes) {
      if (item.id === parentId) {
        const childNode = {
          id: generateId(),
          name: newNode.name || '新节点',
          levelNum: item.levelNum + 1,
          orderNum: (item.children?.length || 0) + 1,
          children: []
        }
        if (!item.children) item.children = []
        item.children.push(childNode)
        sortChildrenByOrderNum(item)
        return true
      }
      if (item.children && findParentAndAdd(item.children, parentId)) {
        return true
      }
    }
    return false
  }
  findParentAndAdd(treeData.value, parentNode.id)
  
  setTimeout(() => {
    expandAllMindmapNodes() // 添加节点后展开
  }, 100)
}

// 树节点点击
const handleNodeClick = (data) => {
  currentNodeId.value = data.id
}

// 保存整体数据
const handleSave = async () => {
  if (!treeData.value || !treeData.value.length) {
    ElMessage.warning('请至少添加一个指标体系')
    return
  }
  
  const rootNode = treeData.value[0]
  if (!rootNode.name) {
    ElMessage.warning('请填写指标体系名称')
    return
  }
  
  if (isRootNode(rootNode)) {
    if (!rootNode.indicatorSetType) {
      ElMessage.warning('请选择指标体系类型')
      return
    }
    if (!rootNode.indicatorSetObject) {
      ElMessage.warning('请填写指标体系对象')
      return
    }
  }
  
  saveLoading.value = true
  try {
    if (treeData.value.length) {
      calculateLevelNum(treeData.value[0])
      sortChildrenByOrderNum(treeData.value[0])
    }
    
    const saveData = convertToSaveData(treeData.value[0])
    emit('save-success', saveData)
    ElMessage.success('保存成功')
    handleClose()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saveLoading.value = false
  }
}

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false
  nodeDialogVisible.value = false
}

// 加载编辑数据
const loadEditData = () => {
  if (props.editData && Object.keys(props.editData).length > 0) {
    const tree = convertToTreeData(props.editData)
    treeData.value = tree ? [tree] : [initDefaultRoot()]
  } else {
    treeData.value = [initDefaultRoot()]
  }
  
  if (treeData.value.length) {
    calculateLevelNum(treeData.value[0])
    sortChildrenByOrderNum(treeData.value[0])
  }
  
  nextTick(() => {
    if (treeRef.value) {
      const expandAllNodes = (nodes) => {
        if (!nodes) return
        nodes.forEach(node => {
          if (node.children && node.children.length) {
            treeRef.value.expandNode(node, true)
            expandAllNodes(node.children)
          }
        })
      }
      expandAllNodes(treeData.value)
    }
  })
  
  setTimeout(() => {
    fitCanvas()
    expandAllMindmapNodes() // 加载完成后展开所有节点
  }, 200)
}

// 监听对话框打开
watch(() => props.visible, (newVal) => {
  if (newVal) {
    checkTheme()
    loadEditData()
  }
}, { immediate: true })

// 窗口大小变化时重新适应画布
const handleResize = () => {
  setTimeout(() => {
    fitCanvas()
  }, 100)
}

onMounted(() => {
  checkTheme()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped lang="scss">
.indicator-dialog {
  :deep(.el-dialog) {
    height: min(900px, 90vh);
    max-height: 90vh;
  }
  
  :deep(.el-dialog__header) {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    z-index: 100;
    background: rgba(0, 0, 0, 0.6);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid rgba(24, 144, 255, 0.3);
    padding: 16px 20px;
    margin-right: 0;
    
    .el-dialog__title {
      color: #fff;
      font-weight: 500;
    }
  }
  
  :deep(.el-dialog__body) {
    padding: 0;
    height: calc(100% - 120px);
    overflow: hidden;
    position: relative;
    display: flex;
    flex-direction: column;
  }
  
  :deep(.el-dialog__footer) {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 100;
    background: rgba(0, 0, 0, 0.6);
    backdrop-filter: blur(10px);
    border-top: 1px solid rgba(24, 144, 255, 0.3);
    padding: 12px 20px;
    
    .el-button {
      background: rgba(255, 255, 255, 0.1);
      border-color: rgba(24, 144, 255, 0.5);
      color: #fff;
      
      &:hover {
        background: rgba(24, 144, 255, 0.3);
        border-color: #1890ff;
      }
    }
    
    .el-button--primary {
      background: #1890ff;
      border-color: #1890ff;
      
      &:hover {
        background: #40a9ff;
      }
    }
  }
}

// 背景画布区域
.canvas-background {
  position: relative;
  flex: 1;
  min-height: 320px;
  max-height: calc(90vh - 200px);
  background: #1a1a2e;
  overflow: hidden;
  
  .mindmap-container {
    width: 100%;
    height: 100%;
    
    :deep(svg) {
      width: 100%;
      height: 100%;
      background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
    }
    
    // 思维导图节点样式
    :deep(.node) {
      circle, rect {
        stroke: #1890ff;
        stroke-width: 2;
        fill: rgba(24, 144, 255, 0.1);
      }
      
      text {
        fill: #e8eaf6;
        font-weight: 500;
        font-size: 14px;
      }
      
      &:hover {
        circle, rect {
          stroke: #40a9ff;
          fill: rgba(24, 144, 255, 0.2);
        }
      }
    }
    
    :deep(.link) {
      stroke: rgba(24, 144, 255, 0.5);
      stroke-width: 2;
    }
    
    :deep(.mindmap-editor) {
      background: transparent;
    }
  }
}

// 悬浮树面板
.floating-tree-panel {
  position: absolute;
  top: 70px;
  left: 20px;
  bottom: 20px;
  width: min(320px, 40%);
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  border: 1px solid rgba(24, 144, 255, 0.3);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
  z-index: 10;
  overflow: hidden;
  
  &.collapsed {
    width: 48px;
    
    .tree-content-wrapper {
      opacity: 0;
      visibility: hidden;
    }
  }
  
  .tree-toggle-btn {
    position: absolute;
    right: -12px;
    top: 50%;
    transform: translateY(-50%);
    width: 24px;
    height: 48px;
    background: rgba(24, 144, 255, 0.8);
    border-radius: 0 12px 12px 0;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s ease;
    z-index: 11;
    
    &:hover {
      background: #1890ff;
      width: 28px;
    }
    
    .el-icon {
      color: #fff;
      font-size: 16px;
    }
  }
  
  .tree-content-wrapper {
    height: 100%;
    display: flex;
    flex-direction: column;
    transition: all 0.3s ease;
    
    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid rgba(24, 144, 255, 0.3);
      background: rgba(0, 0, 0, 0.3);
      
      .header-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 500;
        color: #e8eaf6;
        
        .el-icon {
          color: #1890ff;
          font-size: 18px;
        }
      }
    }
    
    .tree-content {
      flex: 1;
      overflow-y: auto;
      padding: 12px;
      
      :deep(.el-tree) {
        background: transparent;
        color: #e8eaf6;
        
        .el-tree-node__content:hover {
          background: rgba(24, 144, 255, 0.1);
        }
        
        .el-tree-node.is-current > .el-tree-node__content {
          background: rgba(24, 144, 255, 0.2);
        }
        
        .el-tree-node__expand-icon {
          color: #1890ff;
          
          &.expanded {
            color: #40a9ff;
          }
        }
      }
    }
  }
}

// 思维导图工具栏
.mindmap-toolbar {
  position: absolute;
  bottom: 20px;
  right: 20px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 12px;
  
  .el-button {
    background: rgba(0, 0, 0, 0.7);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(24, 144, 255, 0.3);
    color: #1890ff;
    
    &:hover {
      background: rgba(24, 144, 255, 0.3);
      border-color: #1890ff;
      color: #fff;
    }
  }
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding: 4px 0;
  
  &.is-root {
    .node-label {
      font-weight: 600;
      color: #1890ff;
    }
  }
  
  .node-label {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    
    .el-icon {
      color: #909399;
      font-size: 14px;
    }
    
    .label-text {
      font-size: 14px;
      color: #e8eaf6;
    }
    
    .level-tag {
      margin-left: 8px;
      transform: scale(0.85);
      background: rgba(24, 144, 255, 0.2);
      border-color: rgba(24, 144, 255, 0.5);
      color: #1890ff;
    }
  }
  
  .node-actions {
    display: none;
    gap: 4px;
    
    .el-button {
      padding: 4px;
      color: #e8eaf6;
      
      &:hover {
        color: #1890ff;
      }
    }
  }
  
  &:hover .node-actions {
    display: flex;
  }
}

// 节点编辑对话框样式
.node-edit-dialog {
  :deep(.el-dialog__header) {
    border-bottom: 1px solid rgba(24, 144, 255, 0.3);
    padding: 16px 20px;
    background: rgba(0, 0, 0, 0.8);
    backdrop-filter: blur(10px);
    
    .el-dialog__title {
      color: #e8eaf6;
    }
  }
  
  :deep(.el-dialog__body) {
    padding: 20px;
    max-height: 60vh;
    overflow-y: auto;
    background: rgba(0, 0, 0, 0.8);
    backdrop-filter: blur(10px);
  }
  
  :deep(.el-divider) {
    margin: 16px 0;
    
    .el-divider__text {
      background-color: transparent;
      font-weight: 500;
      color: #1890ff;
      
      .el-icon {
        margin-right: 6px;
      }
    }
  }
  
  :deep(.el-form-item__label) {
    color: #e8eaf6;
  }
  
  :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.1);
    box-shadow: 0 0 0 1px rgba(24, 144, 255, 0.3);
    
    .el-input__inner {
      color: #e8eaf6;
    }
  }
  
  :deep(.el-select .el-input__wrapper) {
    background: rgba(255, 255, 255, 0.1);
  }
  
  :deep(.el-input-number__wrapper) {
    background: rgba(255, 255, 255, 0.1);
    box-shadow: 0 0 0 1px rgba(24, 144, 255, 0.3);
  }
}

.dialog-footer {
  text-align: center;
}

// 滚动条美化
.tree-content::-webkit-scrollbar,
:deep(.el-dialog__body)::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.tree-content::-webkit-scrollbar-track,
:deep(.el-dialog__body)::-webkit-scrollbar-track {
  background: rgba(24, 144, 255, 0.1);
  border-radius: 3px;
}

.tree-content::-webkit-scrollbar-thumb,
:deep(.el-dialog__body)::-webkit-scrollbar-thumb {
  background: rgba(24, 144, 255, 0.3);
  border-radius: 3px;
  
  &:hover {
    background: rgba(24, 144, 255, 0.5);
  }
}
</style>