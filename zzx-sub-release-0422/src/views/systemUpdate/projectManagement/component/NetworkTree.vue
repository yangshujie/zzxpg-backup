<template>
  <div class="network-tree">
    <div class="panel-header">
      <h3>{{ title }}</h3>
      <el-input
        v-model="searchText"
        :placeholder="searchPlaceholder"
        clearable
        size="small"
        style="width: 200px"
      />
    </div>
    
    <!-- 装备网络树 -->
    <div class="network-section">
      <div class="section-header" @click="toggleCollapse('equipment')">
        <span class="section-title">装备网络</span>
        <el-icon :class="{ 'rotate-180': collapsedState.equipment }">
          <ArrowDown />
        </el-icon>
      </div>
      <div v-show="!collapsedState.equipment" class="section-content">
        <el-tree
          ref="equipmentTreeRef"
          :data="equipmentTreeData"
          :props="treeProps"
          node-key="id"
          :filter-node-method="filterNode"
          :expand-on-click-node="false"
          @node-click="handleNodeClick"
          @node-dblclick="handleNodeDoubleClick"
          class="custom-tree"
        >
          <template #default="{ node, data }">
            <el-tooltip :content="getNodeTooltip(data)" placement="top-start">
              <span class="custom-tree-node">
                <el-icon class="node-icon" :data-type="data.type">
                  <component :is="getNodeIcon(data.type)" />
                </el-icon>
                <span class="node-label">{{ node.label }}</span>
              </span>
            </el-tooltip>
          </template>
        </el-tree>
      </div>
    </div>

    <!-- OODA网络树 -->
    <div class="network-section">
      <div class="section-header" @click="toggleCollapse('ooda')">
        <span class="section-title">OODA网络</span>
        <el-icon :class="{ 'rotate-180': collapsedState.ooda }">
          <ArrowDown />
        </el-icon>
      </div>
      <div v-show="!collapsedState.ooda" class="section-content">
        <el-tree
          ref="oodaTreeRef"
          :data="localOodaTreeData"
          :props="treeProps"
          node-key="id"
          :filter-node-method="filterNode"
          :expand-on-click-node="false"
          @node-click="handleNodeClick"
          @node-dblclick="handleNodeDoubleClick"
          class="custom-tree"
        >
          <template #default="{ node, data }">
            <el-tooltip :content="getNodeTooltip(data)" placement="top-start">
              <span class="custom-tree-node">
                <el-icon class="node-icon" :data-type="data.type">
                  <component :is="getNodeIcon(data.type)" />
                </el-icon>
                <span class="node-label">{{ node.label }}</span>
              </span>
            </el-tooltip>
          </template>
        </el-tree>
      </div>
    </div>

    <!-- 任务网络树 -->
    <div class="network-section">
      <div class="section-header" @click="toggleCollapse('task')">
        <span class="section-title">任务网络</span>
        <el-icon :class="{ 'rotate-180': collapsedState.task }">
          <ArrowDown />
        </el-icon>
      </div>
      <div v-show="!collapsedState.task" class="section-content">
        <el-tree
          ref="taskTreeRef"
          :data="taskTreeData"
          :props="treeProps"
          node-key="id"
          :filter-node-method="filterNode"
          :expand-on-click-node="false"
          @node-click="handleNodeClick"
          @node-dblclick="handleNodeDoubleClick"
          class="custom-tree"
        >
          <template #default="{ node, data }">
            <el-tooltip :content="getNodeTooltip(data)" placement="top-start">
              <span class="custom-tree-node">
                <el-icon class="node-icon" :data-type="data.type">
                  <component :is="getNodeIcon(data.type)" />
                </el-icon>
                <span class="node-label">{{ node.label }}</span>
              </span>
            </el-tooltip>
          </template>
        </el-tree>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, defineProps, defineEmits, onMounted } from 'vue'
import { ElTooltip } from 'element-plus'
import { ArrowDown, Monitor, Cpu, Setting, Document, Connection } from '@element-plus/icons-vue'
import { queryTask } from '@/api/systemPlus/systemCooperation/task'
import { listEquipmentNetwork, listEquipment, listEquipmentGroup } from '@/api/systemPlus/systemCooperation/equipment'
import { listOodaPathNode, listOodaNetwork } from '@/api/systemPlus/systemCooperation/ooda'

const props = defineProps({
  title: {
    type: String,
    default: '网络资源'
  },
  searchPlaceholder: {
    type: String,
    default: '搜索资源'
  },
  equipmentTreeData: {
    type: Array,
    default: () => []
  },
  oodaTreeData: {
    type: Array,
    default: () => []
  },
  taskTreeData: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['node-click'])

// 搜索文本
const searchText = ref('')

// 折叠状态
const collapsedState = reactive({
  equipment: false,
  ooda: false,
  task: false
})

// 网络数据
const taskData = ref([])
const equipmentNetworkData = ref([])
const equipmentData = ref([])
const equipmentGroupData = ref([])
const oodaPathNodeData = ref([])
const oodaNetworkData = ref([])
const loading = ref(false)

// 树形配置
const treeProps = {
  label: 'name',
  children: 'children'
}

// 节点图标映射
const nodeIcons = {
  equipment: Monitor,
  equipmentGroup: Cpu,
  oodaNode: Setting,
  task: Document,
  network: Connection,
  category: Document,  // 分类节点使用文档图标
  g6Data: Setting,     // G6数据节点使用设置图标
  connection: Connection // 连线节点使用连接图标
}

// 树形组件引用
const equipmentTreeRef = ref(null)
const oodaTreeRef = ref(null)
const taskTreeRef = ref(null)

// 切换折叠状态
const toggleCollapse = (type) => {
  collapsedState[type] = !collapsedState[type]
}

// 节点点击事件
const handleNodeClick = (data) => {
  emit('node-click', data)
}

// 节点双击事件
const handleNodeDoubleClick = (data, node) => {
  // 处理展开G6数据
  if (data.graphData) {
    expandG6Data(data, node)
  }
}

// 展开G6数据
const expandG6Data = (data, node) => {
  if (!data.graphData || !data.isCombo) return
  
  console.log('展开G6数据:', data.name, data.graphData)
  
  // 这里可以触发画布加载G6数据的事件
  // 例如：emit('load-g6-data', data.graphData)
  
  // 或者直接在当前树中显示G6数据
  if (data.children && data.children.length > 0) {
    // 如果已经有子节点，直接展开
    const treeRef = equipmentTreeRef.value
    if (treeRef) {
      treeRef.setCurrentKey(data.id)
    }
  } else {
    // 如果没有子节点，动态加载G6数据
    loadG6DataToNode(data, node)
  }
}

// 动态加载G6数据到节点
const loadG6DataToNode = (data, node) => {
  if (!data.graphData) return
  
  const g6Data = data.graphData
  
  // 创建节点映射表
  const nodeMap = new Map()
  g6Data.nodes.forEach(node => {
    nodeMap.set(node.id, node)
  })
  
  // 获取当前combo的节点
  const comboNodes = []
  if (data.originalData && data.originalData.children && Array.isArray(data.originalData.children)) {
    data.originalData.children.forEach(child => {
      let childId = child
      let childNode = null
      
      if (typeof child === 'string') {
        childNode = nodeMap.get(child)
      } else if (child && child.id) {
        childId = child.id
        childNode = nodeMap.get(child.id)
        if (!childNode) {
          childNode = child
        }
      }
      
      if (childNode) {
        const equipmentNode = {
          id: `node-${childNode.id}`,
          name: childNode.label || childNode.id,
          type: 'equipment',
          originalData: childNode
        }
        
        // 处理连线关系
        if (g6Data.edges && Array.isArray(g6Data.edges) && g6Data.edges.length > 0) {
          const nodeEdges = g6Data.edges.filter(edge => 
            edge.source === childNode.id || edge.target === childNode.id
          )
          
          if (nodeEdges.length > 0) {
            equipmentNode.children = nodeEdges.map(edge => ({
              id: `edge-${edge.id}`,
              name: `连线到 ${edge.target === childNode.id ? edge.source : edge.target}`,
              type: 'connection',
              originalData: edge
            }))
          }
        }
        
        comboNodes.push(equipmentNode)
      }
    })
  }
  
  // 更新节点的children
  data.children = comboNodes
  
  // 强制更新树视图
  const treeRef = equipmentTreeRef.value
  if (treeRef) {
    treeRef.updateKeyChildren(data.id, comboNodes)
  }
}

// 节点图标获取
const getNodeIcon = (type) => {
  return nodeIcons[type] || Monitor
}

// 生成节点tooltip内容
const getNodeTooltip = (data) => {
  if (!data) return '未知节点'
  
  const { type, originalData, name } = data
  
  // 如果originalData不存在，使用基本数据
  if (!originalData) {
    return name || '未知节点'
  }
  
  try {
    switch (type) {
      case 'network':
        return `装备网络: ${originalData.name || '未知'}\n描述: ${originalData.description || '无描述'}`
      case 'equipment':
        return `装备: ${originalData.label || originalData.name || originalData.id || '未知'}\n类型: ${originalData.type || '未知'}`
      case 'equipmentGroup':
        return `装备编组: ${originalData.name || originalData.groupName || '未知'}\n创建时间: ${originalData.createTime ? new Date(originalData.createTime).toLocaleString() : '未知'}`
      case 'oodaNode':
        return `OODA节点: ${originalData.name || originalData.nodeName || '未知'}\n类型: ${originalData.nodeType || '未知'}`
      case 'task':
        return `任务: ${originalData.taskName || '未知'}\n类型: ${originalData.taskType || '未知'}\n阶段: ${originalData.taskStage || '未知'}`
      case 'subtask':
        return `子任务: ${originalData.subTaskName || '未知'}\n执行顺序: ${originalData.execSort || '未知'}`
      case 'connection':
        return `连线关系: ${originalData.source || '未知'} → ${originalData.target || '未知'}`
      case 'category':
        return `分类: ${name || '未知'}`
      default:
        return name || '未知节点'
    }
  } catch (error) {
    console.warn('生成tooltip时出错:', error)
    return name || '未知节点'
  }
}

// 搜索过滤
const filterNode = (value, data) => {
  if (!value) return true
  return data.name?.includes(value) || false
}

// 监听搜索文本变化
watch(searchText, (val) => {
  equipmentTreeRef.value?.filter(val)
  oodaTreeRef.value?.filter(val)
  taskTreeRef.value?.filter(val)
})

// 加载任务数据
const loadTaskData = async () => {
  try {
    loading.value = true
    const response = await queryTask({ profileId: 91 })
    if (response.code === 200) {
      taskData.value = response.data || []
    }
  } catch (error) {
    console.error('加载任务数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 将任务数据转换为树形结构
const convertTaskDataToTree = (tasks) => {
  return tasks.map(task => {
    const taskNode = {
      id: `task-${task.taskId}`,
      name: task.taskName,
      type: 'task',
      originalData: task
    }
    
    // 添加子任务节点
        if (task.subTaskList && task.subTaskList.length > 0) {
          taskNode.children = task.subTaskList.map(subTask => {
            const subTaskNode = {
              id: `subtask-${subTask.subTaskId}`,
              name: subTask.subTaskName,
              type: 'subtask',
              originalData: subTask
            }
        
        // 处理子任务之间的连线关系
          if (subTask.relationList && subTask.relationList.length > 0) {
            subTaskNode.children = subTask.relationList.map(relation => ({
              id: `relation-${subTask.subTaskId}-${relation.subTaskName}`,
              name: `→ ${relation.subTaskName}`,
              type: 'relation',
              originalData: relation
            }))
          }
        
        return subTaskNode
      })
    }
    
    return taskNode
  })
}

// 计算属性：转换后的任务树数据
const taskTreeData = ref([])

// 监听任务数据变化，自动转换为树形结构
watch(() => taskData.value, (newTasks) => {
  taskTreeData.value = convertTaskDataToTree(newTasks)
}, { immediate: true })



// 解析G6画布数据并转换为树形结构
const parseG6GraphDataToTree = (equipmentNetworks) => {
  if (!equipmentNetworks || !Array.isArray(equipmentNetworks)) {
    return []
  }
  
  return equipmentNetworks.map(network => {
    const networkNode = {
      id: `network-${network.id}`,
      name: network.name,
      type: 'equipment', // 装备网络节点归属于装备类别，显示为绿色
      originalData: network
    }
    
    // 解析graphJSONEquipmentNetwork字段中的G6数据
    if (network.graphJSONEquipmentNetwork && typeof network.graphJSONEquipmentNetwork === 'string') {
      try {
        // 先检查是否是有效的JSON字符串
        const trimmedJson = network.graphJSONEquipmentNetwork.trim()
        if (trimmedJson && trimmedJson !== 'XXX' && trimmedJson !== 'null') {
          const g6Data = JSON.parse(trimmedJson)
          
          // 直接解析G6数据中的群组和节点关系
          if (g6Data && g6Data.nodes && Array.isArray(g6Data.nodes) && g6Data.nodes.length > 0) {
            // 创建节点映射表，便于查找
            const nodeMap = new Map()
            g6Data.nodes.forEach(node => {
              nodeMap.set(node.id, node)
            })
            
            console.log('节点映射表:', nodeMap)
            console.log('G6数据节点列表:', g6Data.nodes)
            
            // 如果有combos群组数据，按群组分类
            if (g6Data.combos && Array.isArray(g6Data.combos) && g6Data.combos.length > 0) {
              console.log('发现combos群组数据:', g6Data.combos)
              
              // 创建群组节点
              networkNode.children = g6Data.combos.map(combo => {
                // 获取群组中的节点
                const comboNodes = []
                
                if (combo.children && Array.isArray(combo.children)) {
                  console.log(`处理群组 ${combo.label || combo.id} 的children:`, combo.children)
                  
                  combo.children.forEach(child => {
                    // 检查child是ID字符串还是节点对象
                    let childId = child
                    let childNode = null
                    
                    if (typeof child === 'string') {
                      // child是节点ID
                      childNode = nodeMap.get(child)
                    } else if (child && child.id) {
                      // child是节点对象
                      childId = child.id
                      childNode = nodeMap.get(child.id)
                      if (!childNode) {
                        // 如果节点对象不在映射表中，直接使用该对象
                        childNode = child
                      }
                    }
                    
                    if (childNode) {
                      console.log(`找到节点: ${childNode.label || childNode.id}`)
                      // 创建装备节点（绿色）
                      const equipmentNode = {
                        id: `node-${childNode.id}`,
                        name: childNode.label || childNode.id,
                        type: 'equipment',
                        originalData: childNode
                      }
                      
                      // 处理连线关系
                      if (g6Data.edges && Array.isArray(g6Data.edges) && g6Data.edges.length > 0) {
                        const nodeEdges = g6Data.edges.filter(edge => 
                          edge.source === childNode.id || edge.target === childNode.id
                        )
                        
                        if (nodeEdges.length > 0) {
                          equipmentNode.children = nodeEdges.map(edge => ({
                            id: `edge-${edge.id}`,
                            name: `连线到 ${edge.target === childNode.id ? edge.source : edge.target}`,
                            type: 'connection',
                            originalData: edge
                          }))
                        }
                      }
                      
                      comboNodes.push(equipmentNode)
                    } else {
                      console.warn(`未找到节点: ${childId}`)
                    }
                  })
                }
                
                console.log(`群组 ${combo.label || combo.id} 包含 ${comboNodes.length} 个节点`)
                
                return {
                  id: `combo-${combo.id}`,
                  name: combo.label || combo.id,
                  type: 'equipment', // 一级节点显示为绿色装备节点
                  originalData: combo,
                  children: comboNodes,
                  isCombo: true, // 标记这是一个combo节点
                  graphData: g6Data // 保存完整的G6数据用于双击展开
                }
              })
            } else {
              // 如果没有combos，按默认方式处理
              const defaultGroupNodes = g6Data.nodes.map(node => {
                // 创建装备节点（绿色）
                const equipmentNode = {
                  id: `node-${node.id}`,
                  name: node.label || node.id,
                  type: 'equipment',
                  originalData: node
                }
                
                // 处理连线关系
                if (g6Data.edges && Array.isArray(g6Data.edges) && g6Data.edges.length > 0) {
                  const nodeEdges = g6Data.edges.filter(edge => 
                    edge.source === node.id || edge.target === node.id
                  )
                  
                  if (nodeEdges.length > 0) {
                    equipmentNode.children = nodeEdges.map(edge => ({
                      id: `edge-${edge.id}`,
                      name: `连线到 ${edge.target === node.id ? edge.source : edge.target}`,
                      type: 'connection',
                      originalData: edge
                    }))
                  }
                }
                
                return equipmentNode
              })
              
              // 创建默认群组
              networkNode.children = [{
                id: `group-default-${network.id}`,
                name: '默认群组',
                type: 'equipment', // 默认群组也显示为绿色装备节点
                originalData: network,
                children: defaultGroupNodes,
                isCombo: false,
                graphData: g6Data
              }]
            }
          }
        }
      } catch (error) {
        console.warn('解析G6数据失败，跳过该网络:', error.message)
        // 即使解析失败，也返回基本的网络节点
      }
    }
    
    return networkNode
  })
}


// 将装备数据转换为树形结构
const convertEquipmentDataToTree = (equipments) => {
  return equipments.map(equipment => ({
    id: `equipment-${equipment.id}`,
    name: equipment.name || equipment.equipmentName,
    type: 'equipment',
    originalData: equipment
  }))
}

// 将装备组数据转换为树形结构
const convertEquipmentGroupDataToTree = (equipmentGroups) => {
  return equipmentGroups.map(group => {
    const groupNode = {
      id: `equipmentGroup-${group.id}`,
      name: group.name || group.groupName,
      type: 'equipmentGroup',
      originalData: group
    }
    
    // 如果有formationDetail数据，显示具体装备节点
    if (group.formationDetail && Array.isArray(group.formationDetail) && group.formationDetail.length > 0) {
      groupNode.children = group.formationDetail.map(equipment => ({
        id: `equipment-${equipment.id}`,
        name: equipment.name || equipment.id,
        type: 'equipment',
        originalData: equipment
      }))
    }
    
    return groupNode
  })
}

// 计算属性：转换后的装备网络树数据（整合装备网络、装备数据、装备组数据）
const equipmentTreeData = ref([])

// 监听所有装备相关数据变化，自动整合为分层树形结构
watch([() => equipmentNetworkData.value, () => equipmentData.value, () => equipmentGroupData.value], 
  ([newNetworks, newEquipments, newGroups]) => {
    const networkTree = parseG6GraphDataToTree(newNetworks)
    const equipmentTree = convertEquipmentDataToTree(newEquipments)
    const groupTree = convertEquipmentGroupDataToTree(newGroups)
    
    // 创建分层结构：装备网络、装备、结构编组、力量编组
    equipmentTreeData.value = [
      // 装备网络分类节点
      {
        id: 'category-networks',
        name: '装备网络',
        type: 'category',
        children: networkTree
      },
      // 装备分类节点
      {
        id: 'category-equipments',
        name: '装备',
        type: 'category',
        children: equipmentTree
      },
      // 装备编组分类节点
      {
        id: 'category-groups',
        name: '装备编组',
        type: 'category',
        children: groupTree
      }
    ]
  }, { immediate: true })

// 加载装备数据
const loadEquipmentData = async () => {
  try {
    loading.value = true
    const response = await listEquipment({
      pageNum: 1,
      pageSize: 100
    })
    if (response.code === 200) {
      equipmentData.value = response.rows || response.data || []
    }
  } catch (error) {
    console.error('加载装备数据失败:', error)
  } finally {
    loading.value = false
  }
}
// 加载装备网络数据
const loadEquipmentNetworkData = async () => {
  try {
    loading.value = true
    const response = await listEquipmentNetwork({
      pageNum: 1,
      pageSize: 10
    })
    if (response.code === 200) {
      equipmentNetworkData.value = response.rows || response.data || []
    }
  } catch (error) {
    console.error('加载装备网络数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载装备组数据
const loadEquipmentGroupData = async () => {
  try {
    loading.value = true
    const response = await listEquipmentGroup({
      pageNum: 1,
      pageSize: 100
    })
    if (response.code === 200) {
      equipmentGroupData.value = response.rows || response.data || []
    }
  } catch (error) {
    console.error('加载装备组数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载OODA路径节点数据
const loadOodaPathNodeData = async () => {
  try {
    loading.value = true
    const response = await listOodaPathNode({
      pageNum: 1,
      pageSize: 100
    })
    if (response.code === 200) {
      oodaPathNodeData.value = response.rows || response.data || []
    }
  } catch (error) {
    console.error('加载OODA路径节点数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载OODA网络数据
const loadOodaNetworkData = async () => {
  try {
    loading.value = true
    const response = await listOodaNetwork({
      pageNum: 1,
      pageSize: 100
    })
    if (response.code === 200) {
      oodaNetworkData.value = response.rows || response.data || []
    }
  } catch (error) {
    console.error('加载OODA网络数据失败:', error)
  } finally {
    loading.value = false
  }
}



// 计算属性：转换后的装备数据树
const equipmentDataTree = ref([])

// 监听装备数据变化，自动转换为树形结构
watch(() => equipmentData.value, (newEquipments) => {
  equipmentDataTree.value = convertEquipmentDataToTree(newEquipments)
}, { immediate: true })

// 将OODA路径节点数据转换为树形结构
const convertOodaPathNodeDataToTree = (oodaPathNodes) => {
  return oodaPathNodes.map(node => ({
    id: `oodaNode-${node.id}`,
    name: node.name || node.nodeName,
    type: 'oodaNode',
    originalData: node
  }))
}

// 将OODA网络数据转换为树形结构
const convertOodaNetworkDataToTree = (oodaNetworks) => {
  return oodaNetworks.map(network => ({
    id: `oodaNetwork-${network.id}`,
    name: network.name || network.networkName,
    type: 'network',
    originalData: network
  }))
}

// 计算属性：转换后的OODA树数据（整合OODA网络、OODA路径节点数据）
const localOodaTreeData = ref([])

// 监听所有OODA相关数据变化，自动整合为分层树形结构
watch([() => oodaNetworkData.value, () => oodaPathNodeData.value], 
  ([newOodaNetworks, newOodaPathNodes]) => {
    const networkTree = convertOodaNetworkDataToTree(newOodaNetworks)
    const pathNodeTree = convertOodaPathNodeDataToTree(newOodaPathNodes)
    
    // 创建分层结构：OODA网络、OODA路径节点
    localOodaTreeData.value = [
      // OODA网络分类节点
      {
        id: 'category-ooda-networks',
        name: 'OODA网络',
        type: 'category',
        children: networkTree
      },
      // OODA路径节点分类节点
      // {
      //   id: 'category-ooda-nodes',
      //   name: 'OODA路径节点',
      //   type: 'category',
      //   children: pathNodeTree
      // }
    ]
  }, { immediate: true })

// 初始化加载所有网络数据
onMounted(() => {
  loadTaskData()
  loadEquipmentNetworkData()
  loadEquipmentData()
  loadEquipmentGroupData()
  loadOodaPathNodeData()
  loadOodaNetworkData()
})
</script>

<style lang="scss" scoped>
.network-tree {
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.02);
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  overflow-y: auto;

  .panel-header {
    padding: 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    h3 {
      margin: 0 0 12px 0;
      color: #1890ff;
      font-size: 16px;
    }
  }

  .network-section {
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      cursor: pointer;
      transition: background-color 0.3s;

      &:hover {
        background: rgba(255, 255, 255, 0.05);
      }

      .section-title {
        font-weight: bold;
        color: #fff;
      }

      .el-icon {
        transition: transform 0.3s;
        color: #1890ff;

        &.rotate-180 {
          transform: rotate(180deg);
        }
      }
    }

    .section-content {
      padding: 0 8px 8px 8px;
    }
  }

  // 自定义树形样式
  .custom-tree {
    background: transparent;
    color: #fff;

    :deep(.el-tree-node) {
      .el-tree-node__content {
        height: 36px;
        color: #fff;

        &:hover {
          background: rgba(24, 144, 255, 0.2);
        }
      }

      .el-tree-node__expand-icon {
        color: #1890ff;
      }

      &.is-current {
        > .el-tree-node__content {
          background: rgba(24, 144, 255, 0.3);
        }
      }
    }

    .custom-tree-node {
      display: flex;
      align-items: center;
      width: 100%;

      .node-icon {
        margin-right: 8px;
        font-size: 16px;
      }

      // 装备节点和装备网络节点图标颜色为绿色
      .node-icon[data-type="equipment"] {
        color: #52c41a !important;
      }
      
      .node-icon[data-type="network"] {
        color: #52c41a !important;
      }

      // 其他节点保持蓝色
      .node-icon {
        color: #1890ff;
      }

      .node-label {
        flex: 1;
        font-size: 14px;
      }
    }
  }
}
</style>